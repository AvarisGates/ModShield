package com.avaris.modshield.client;

import com.avaris.modshield.network.ClientModsC2S;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;

public class ModShieldClient implements ClientModInitializer {

    private static final HashMap<String,String> modIds = new HashMap<>();

    public static void sendMods() {
        // Populate mod ids
        FabricLoader.getInstance().getAllMods().forEach(container -> {
                modIds.putIfAbsent(container.getMetadata().getId(),container.getMetadata().getVersion().getFriendlyString());
        });
        ClientConfigurationNetworking.send(new ClientModsC2S(modIds, modIds.hashCode(),true));
    }

    @Override
    public void onInitializeClient() {
    }
}
