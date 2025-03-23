package com.avaris.modshield;

import com.avaris.modshield.api.v1.impl.EventApi;
import com.avaris.modshield.api.v1.impl.ModShieldApi;
import com.avaris.modshield.network.ClientModsC2S;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Common entrypoint to ModShield.
 * @see ModShieldApi
 */
public class ModShield implements ModInitializer {

    public static final String MOD_ID_CAP = "ModShield";
    public static final String MOD_ID = MOD_ID_CAP.toLowerCase(Locale.ROOT);

    private static final Logger SERVER_LOGGER = LoggerFactory.getLogger(MOD_ID_CAP+"|Server");
    private static final Logger CLIENT_LOGGER = LoggerFactory.getLogger(MOD_ID_CAP+"|Client");

    private static final Text NO_MOD_SHIELD_MESSAGE =
            Text.literal("Please install ")
            .append(Text.literal(ModShield.MOD_ID_CAP)
                .formatted(Formatting.GOLD))
            .append(" to join the server");
    public static String MOD_VERSION;
    public static final int PROTOCOL_VERSION = 2;

    public static Identifier id(String id) {
        return Identifier.of(MOD_ID,id);
    }

    public static Logger getLogger(){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            return SERVER_LOGGER;
        }
        return CLIENT_LOGGER;
    }

    private static final HashMap<Integer,Boolean> allowedModsCache = new HashMap<>();
    private static final HashMap<UUID,String> denialReasons = new HashMap<>();
    private static final HashSet<UUID> allowedPlayers = new HashSet<>();
    private static final HashMap<UUID,Map<String,String>> playerMods = new HashMap<>();

    public static synchronized boolean isPlayerAllowed(UUID playerUuid){
        return allowedPlayers.contains(playerUuid);
    }

    public static synchronized Map<String,String> getPlayerMods(UUID playerUuid){
        return playerMods.get(playerUuid);
    }

    private static synchronized void receiveClientModsC2S(ClientModsC2S packet, ServerConfigurationNetworking.Context context) {
        UUID playerUuid = context.networkHandler().getDebugProfile().getId();
        if(ShieldConfig.isAlwaysAllowed(playerUuid,context.networkHandler().getDebugProfile().getName())){
            allowedPlayers.add(playerUuid);
            EventApi.PLAYER_ALLOWED_EVENT.invoker().onPlayerAllowed(playerUuid,packet.mods());
        }
        if(packet.protocolVersion() != PROTOCOL_VERSION){
            denialReasons.put(playerUuid,"Please update ModShield.");
            if(context.server() instanceof MinecraftDedicatedServer){
                if(context.networkHandler().isConnectionOpen()){
                    EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(playerUuid,denialReasons.get(playerUuid));
                    context.networkHandler().disconnect(Text.literal(denialReasons.get(playerUuid)));
                }
            }
            return;
        }

        allowedPlayers.remove(playerUuid);

        if(!packet.valid()){
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                ModShield.getLogger().info("{} sent invalid packet!", playerUuid);
            }
            if(!EventApi.SENT_INVALID_PACKET_EVENT.invoker().onSentInvalidPacket(playerUuid)){
                denialReasons.put(playerUuid,"Invalid mods hash.");
                EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(playerUuid,denialReasons.get(playerUuid));
                context.networkHandler().disconnect(Text.literal(denialReasons.get(playerUuid)));
            }
        }

        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            ModShield.getLogger().info("{} sent mods:", playerUuid);
            for(var i : packet.mods().entrySet()){
                ModShield.getLogger().info("{}",i);
            }
        }

        if(ShieldConfig.shouldSavePlayerMods()){
            playerMods.put(playerUuid,packet.mods());
        }

        // Check this way to avoid a NullPointerException
        if(Boolean.FALSE.equals(validateMods(packet,playerUuid))){
            if(context.server() instanceof MinecraftDedicatedServer){
                if(context.networkHandler().isConnectionOpen()){
                    EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(playerUuid,denialReasons.get(playerUuid));
                    context.networkHandler().disconnect(Text.literal(denialReasons.get(playerUuid)));
                }
            }
        }else{
            allowedPlayers.add(playerUuid);
            EventApi.PLAYER_ALLOWED_EVENT.invoker().onPlayerAllowed(playerUuid,packet.mods());
        }
    }

    private static synchronized boolean validateMods(ClientModsC2S packet,UUID playerUuid) {
        Boolean ret = allowedModsCache.get(packet.hash());
        if(ret != null){
            return ret;
        }
        ret = true;
        List<String> allowedMods = (List<String>) ShieldConfig.getAllowedMods();
        if(allowedMods != null&&!allowedMods.isEmpty()) {
            for (String name : packet.mods().keySet()) {
                if (!allowedMods.contains(name)) {
                    ret = false;
                    denialReasons.put(playerUuid,"Your mod list contains an invalid mod: "+name);
                    break;
                }
            }
        }
        if(ret){
            List<String> disallowedMods = (List<String>) ShieldConfig.getDisallowedMods();
            if(disallowedMods != null&&!disallowedMods.isEmpty()){
                for(String name : disallowedMods){
                    if(packet.mods().containsKey(name)){
                        ret = false;
                        denialReasons.put(playerUuid,"Your mod list contains an invalid mod: "+name);
                        break;
                    }
                }
            }
        }

        allowedModsCache.put(packet.hash(),ret);
        if(ret){
            denialReasons.remove(playerUuid);
        }
        return ret;
    }

    public static synchronized @Nullable Text canJoin(SocketAddress address, GameProfile profile) {
        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            ModShield.getLogger().info("ModShield.canJoin");
        }
        if(ShieldConfig.isAlwaysAllowed(profile.getId(),profile.getName())){
            return null;
        }
        String denialReason = denialReasons.get(profile.getId());
        if(denialReason == null||denialReason.isBlank()){
            // Return null for success
            return null;
        }
        EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(profile.getId(),denialReason);
        return Text.literal(denialReason);
    }

    public static synchronized void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if(!connection.isOpen()){
            return;
        }
        ModShield.getLogger().info("ModShield.onPlayerConnect");
        if(ShieldConfig.isAlwaysAllowed(player.getUuid(),player.getNameForScoreboard())){
            return;
        }
        if(denialReasons.get(player.getUuid()) != null){
            EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(player.getUuid(),denialReasons.get(player.getUuid()));
            connection.disconnect(Text.of(denialReasons.get(player.getUuid())));
        }
        if(!allowedPlayers.contains(player.getUuid())){
            player.server.getPlayerManager().remove(player);
            connection.send(new DisconnectS2CPacket(NO_MOD_SHIELD_MESSAGE));
            EventApi.PLAYER_DISALLOWED_EVENT.invoker().onPlayerDisallow(player.getUuid(),NO_MOD_SHIELD_MESSAGE.getLiteralString());
            //connection.disconnect(NO_MOD_SHIELD_MESSAGE);
        }
        allowedPlayers.remove(player.getUuid());
    }

    public static synchronized void clearCache(){
        allowedModsCache.clear();
        denialReasons.clear();
        allowedPlayers.clear();
        playerMods.clear();
    }

    private static synchronized int commandReload(CommandContext<ServerCommandSource> context) {
        try {
            ShieldConfig.load();
            context.getSource().sendMessage(Text.literal(ModShield.MOD_ID_CAP)
                    .formatted(Formatting.GOLD)
                    .append(" reloaded with disallowed: "+ShieldConfig.getDisallowedMods().size()+", allowed: "+ShieldConfig.getAllowedMods().size()));
        } catch (IOException e) {
            e.printStackTrace();
            context.getSource().sendError(Text.literal(e.getMessage()));
            return 1;
        }
        return 0;
    }

    @Override
    public void onInitialize() {

        //ServiceLoader<IAddServerMods> iAddServerMods = ServiceLoader.load(IAddServerMods.class);
        //iAddServerMods.forEach(x -> x.test());
        PayloadTypeRegistry.configurationC2S().register(ClientModsC2S.ID,ClientModsC2S.CODEC);
        ServerConfigurationNetworking.registerGlobalReceiver(ClientModsC2S.ID, ModShield::receiveClientModsC2S);
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER||
            // For easier development
            FabricLoader.getInstance().isDevelopmentEnvironment()){
            try {
                ShieldConfig.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
        container.ifPresent(mod ->
                MOD_VERSION = mod.getMetadata().getVersion().getFriendlyString());

        ModUpdater.downloadLatest();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("mod-shield-reload").requires(source -> source.hasPermissionLevel(4))
                        .executes((ModShield::commandReload)))
        );

        EventApi.CONFIG_RELOADED_EVENT.register(() -> {
            ModShield.getLogger().info("Loaded ModShield config, disallowed mods: {}, allowed mods: {}",ShieldConfig.getDisallowedMods().size(),ShieldConfig.getAllowedMods().size());
            if(FabricLoader.getInstance().isDevelopmentEnvironment()){
                ModShield.getLogger().info("Disallowed Mods:");
                for(var i : ShieldConfig.getDisallowedMods()){
                    ModShield.getLogger().info("\t'{}'",i);
                }

                ModShield.getLogger().info("Allowed Mods:");
                for(var i : ShieldConfig.getAllowedMods()){
                    ModShield.getLogger().info("\t'{}'",i);
                }
            }
        });
    }
}
