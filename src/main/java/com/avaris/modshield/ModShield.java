package com.avaris.modshield;

import com.avaris.modshield.network.ClientModsC2S;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
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

    private static void receiveClientModsC2S(ClientModsC2S packet, ServerConfigurationNetworking.Context context) {
        UUID playerUuid = context.networkHandler().getDebugProfile().getId();

        allowedPlayers.remove(playerUuid);

        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            if(!packet.valid()){
                ModShield.getLogger().info("{} sent invalid packet!", playerUuid);
            }
            ModShield.getLogger().info("{} sent mods:", playerUuid);
            for(var i : packet.mods().entrySet()){
                ModShield.getLogger().info("{}",i);
            }
        }

        // Check this way to avoid a NullPointerException
        if(Boolean.FALSE.equals(validateMods(packet,playerUuid))){
            if(context.server() instanceof MinecraftDedicatedServer){
                context.networkHandler().disconnect(Text.literal(denialReasons.get(playerUuid)));
            }
        }else{
            allowedPlayers.add(playerUuid);
        }
    }

    private static boolean validateMods(ClientModsC2S packet,UUID playerUuid) {
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

    public static @Nullable Text canJoin(SocketAddress address, GameProfile profile) {
        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            ModShield.getLogger().info("ModShield.canJoin");
        }
        String denialReason = denialReasons.get(profile.getId());
        if(denialReason == null||denialReason.isBlank()){
            //if(address.toString().contains("192.168.")||
            //   address.toString().contains("local:")||address.toString().contains("127.0.0.1:")){
            //    return null;
            //}

            //if(!allowedPlayers.contains(profile.getId())){
            //    return NO_MOD_SHIELD_MESSAGE;
            //}
            // Return null for success
            return null;
        }
        return Text.literal(denialReason);
    }

    public static void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if(!connection.isOpen()){
            return;
        }
        ModShield.getLogger().info("ModShield.onPlayerConnect");
        if(denialReasons.get(player.getUuid()) != null){
            connection.disconnect(Text.of(denialReasons.get(player.getUuid())));
        }
        if(!allowedPlayers.contains(player.getUuid())){
            player.server.getPlayerManager().remove(player);
            connection.send(new DisconnectS2CPacket(NO_MOD_SHIELD_MESSAGE));
            connection.disconnect(NO_MOD_SHIELD_MESSAGE);
        }
        allowedPlayers.remove(player.getUuid());
    }

    public static void clearCache(){
        allowedModsCache.clear();
        denialReasons.clear();
        allowedPlayers.clear();
    }

    @Override
    public void onInitialize() {
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
    }
}
