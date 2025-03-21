package com.avaris.modshield.api.v1.impl;

import com.avaris.modshield.ModShield;
import com.avaris.modshield.ShieldConfig;
import com.avaris.modshield.network.ClientModsC2S;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes functions and events that can be used by other mods.
 * It is not recommended to call functions from packages other than api.
 */
public class ModShieldApi {
    /**
     * Retrieves disallowed mods
     * @return a Collection of allowed mods, can be cast to List/HashSet
     * @see ShieldConfig#getDisallowedMods()
     */
    public static Collection<String> getDisallowedMods() {
        return ShieldConfig.getDisallowedMods();
    }

    /**
     * Retrieves allowed mods
     * @return a Collection of allowed mods, can be cast to List/HashSet
     * @see ShieldConfig#getAllowedMods()
     */
    public static Collection<String> getAllowedMods() {
        return ShieldConfig.getAllowedMods();
    }

    /**
     * @param playerUuid player's UUID
     * @return whether the player can join, and passed all checks
     */
    public static boolean canPlayerJoin(UUID playerUuid){
        return ModShield.isPlayerAllowed(playerUuid);
    }

    /**
     * Retrieves player mod saving status.<br>
     * To enable this option set 'savePlayerMods' in the config file to 'true'.
     * @return whether player mods are saved and can be retrieved
     * @see ShieldConfig
     */
    public static boolean arePlayerModsSaved(){
        return ShieldConfig.shouldSavePlayerMods();
    }

    /**
     * Retrieves player mods sent by the client, only available when player mods are saved.<br>
     * To check if player mods are saved use{@link ModShieldApi#arePlayerModsSaved()}
     * @return A Map of mod id to mod version
     */
    public static @Nullable Map<String,String> getPlayerMods(UUID playerUuid){
        if(!arePlayerModsSaved()){
            return null;
        }
        return ModShield.getPlayerMods(playerUuid);
    }

    public static class Events{
        /**
         * Called when the config is reloaded.
         * @see ModShieldApi#getDisallowedMods()
         * @see ModShieldApi#getAllowedMods()
         * @see ShieldConfig#load()
         */
        public static final Event<ConfigReloaded> CONFIG_RELOADED_EVENT = EventFactory.createArrayBacked(ConfigReloaded.class,(callbacks) -> () -> {
            for(var callback : callbacks){
                callback.onConfigReloaded();
            }
        });

        /**
         * Called when a player is cleared to connect by ModShield.
         * playerUuid - the disconnected player's UUID
         * modMap - map from mod id to version
         */
        public static final Event<PlayerAllowed> PLAYER_ALLOWED_EVENT = EventFactory.createArrayBacked(PlayerAllowed.class,(callbacks) -> (playerUuid, modMap) -> {
            for(var callback : callbacks){
                callback.onPlayerAllowed(playerUuid, modMap);
            }
        });

        /**
         * Called when a player is disconnected by ModShield.
         * playerUuid - the disconnected player's UUID
         * reason - a String with the disconnection reason
         */
        public static final Event<PlayerDisallowed> PLAYER_DISALLOWED_EVENT = EventFactory.createArrayBacked(PlayerDisallowed.class,(callbacks) -> (playerUuid,reason) -> {
            for(var callback : callbacks){
                callback.onPlayerDisallow(playerUuid,reason);
            }
        });

        /**
         * Called when a player sends an invalid packet hash.<br>
         * Return{@code false} to disconnect that player.
         * @see ModShield#receiveClientModsC2S(ClientModsC2S, ServerConfigurationNetworking.Context)
         */
        public static final Event<SentInvalidPacket> SENT_INVALID_PACKET_EVENT = EventFactory.createArrayBacked(SentInvalidPacket.class,(callbacks) -> (playerUuid) -> {
            for(var callback : callbacks){
                boolean ret = callback.onSentInvalidPacket(playerUuid);
                if(!ret){
                    return false;
                }
            }
            return true;
        });

        @FunctionalInterface
        public interface ConfigReloaded{
            void onConfigReloaded();
        }

        @FunctionalInterface
        public interface PlayerAllowed{
            void onPlayerAllowed(UUID playerUuid,Map<String,String> modMap);
        }

        @FunctionalInterface
        public interface PlayerDisallowed{
            void onPlayerDisallow(UUID playerUuid,String reason);
        }

        @FunctionalInterface
        public interface SentInvalidPacket{
            boolean onSentInvalidPacket(UUID playerUuid);
        }
    }

}
