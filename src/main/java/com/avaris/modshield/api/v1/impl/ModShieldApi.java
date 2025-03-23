package com.avaris.modshield.api.v1.impl;

import com.avaris.modshield.ModShield;
import com.avaris.modshield.ShieldConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes functions and events that can be used by other mods.
 * It is not recommended to call functions from packages other than api.
 * @see EventApi
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

    /**
     * Retrieve mod version, should be called after{@link ModShield#onInitialize()}is called.
     * @return ModShield version as a string
     */
    public static String getVersion(){
        return ModShield.MOD_VERSION;
    }

    /**
     * Retrieve protocol version
     * @return protocol version as an integer
     */
    public static int getProtocolVersion(){
        return ModShield.PROTOCOL_VERSION;
    }
}
