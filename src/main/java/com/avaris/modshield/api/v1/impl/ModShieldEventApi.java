package com.avaris.modshield.api.v1.impl;

import com.avaris.modshield.ModShield;
import com.avaris.modshield.ShieldConfig;
import com.avaris.modshield.network.ClientModsC2S;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;

import java.util.Map;
import java.util.UUID;

/**
 * A container for all the mod's events.<br>
 * For other API methods see{@link ModShieldApi}
 */
public class ModShieldEventApi {
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
        void onPlayerAllowed(UUID playerUuid, Map<String,String> modMap);
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
