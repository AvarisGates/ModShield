package com.avaris.modshield;

import com.avaris.modshield.api.v1.impl.EventApi;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

public class ShieldConfig {
    private static final ArrayList<String> disallowedMods = new ArrayList<>();
    private static final ArrayList<String> allowedMods = new ArrayList<>();
    private static final ArrayList<String> alwaysAllowedPlayers = new ArrayList<>();
    private static boolean savePlayerMods = false;

    private static final String DISALLOWED_MODS_KEY = "disallowed";
    private static final String ALLOWED_MODS_KEY = "allowed";
    private static final String SAVE_PLAYER_MODS_KEY = "savePlayerMods";
    private static final String ONLY_SERVER_MODS_KEY = "onlyAllowServerMods";
    private static final String ALWAYS_ALLOWED_PLAYERS_KEY = "alwaysAllowedPlayers";

    private static final String CONFIG_COMMENTS = MessageFormat.format("""
            This is a template ModShield config file.
            If you want to allow only certain mods add them in the {0} option, separated by commas.
            This way only mods in that option can be used by the client.
            
            If you want to disallow mods put them in the {1} option, separated by commas.
            
            {2} - when set to 'true' ModShield will save mods used by players, that can be accessed through the API.
            {3} - when set to 'true' only mods found on the server will be allowed on the client.
            {4} - list of players that can run every mod, separated by commas, UUIDs or names""",
            DISALLOWED_MODS_KEY,
            ALLOWED_MODS_KEY,
            SAVE_PLAYER_MODS_KEY,
            ONLY_SERVER_MODS_KEY,
            ALWAYS_ALLOWED_PLAYERS_KEY
    );

    public static synchronized Collection<String> getDisallowedMods() {
        return disallowedMods;
    }

    public static synchronized Collection<String> getAllowedMods() {
        return allowedMods;
    }

    public static boolean shouldSavePlayerMods(){
        return savePlayerMods;
    }

    private static List<String> validateInputList(List<String> input){
        return input.stream().flatMap(s -> {
            s = s.toLowerCase(Locale.ROOT).strip();
            if(s.isBlank()){
                return Stream.empty();
            }
            return Stream.of(s);
        }
        ).distinct().sorted().toList();
    }

    private static synchronized Path prepareConfigFile() throws IOException {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(ModShield.MOD_ID+".properties");
        if(Files.exists(configPath)){
           return configPath;
        }

        BufferedWriter writer = Files.newBufferedWriter(configPath);

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        Properties properties = new Properties();
        properties.put(DISALLOWED_MODS_KEY,"");
        properties.put(ALLOWED_MODS_KEY,"");
        properties.put(SAVE_PLAYER_MODS_KEY,"false");
        properties.put(ONLY_SERVER_MODS_KEY,"false");

        properties.store(writer,CONFIG_COMMENTS);
        writer.close();

        return configPath;
    }

    public static synchronized void load() throws IOException {
        ModShield.clearCache();

        Path configPath = prepareConfigFile();
        Properties properties = new Properties();
        BufferedReader reader = Files.newBufferedReader(configPath);
        properties.load(reader);
        reader.close();

        disallowedMods.clear();
        allowedMods.clear();
        boolean onlyServerMods = false;

        String onlyServerModsString = properties.getProperty(ONLY_SERVER_MODS_KEY);

        if (onlyServerModsString != null){
            onlyServerMods = Boolean.parseBoolean(onlyServerModsString);
        }

        if(onlyServerMods){
            allowedMods.clear();
            disallowedMods.clear();
            FabricLoader.getInstance().getAllMods().forEach(mod -> allowedMods.add(mod.getMetadata().getId()));
        }else{
            String disallowedModsString = properties.getProperty(DISALLOWED_MODS_KEY);

            if(disallowedModsString != null){
                disallowedMods.addAll(
                        validateInputList(List.of(disallowedModsString.split(",")))
                );
            }

            String allowedModsString = properties.getProperty(ALLOWED_MODS_KEY);

            if (allowedModsString != null){
                allowedMods.addAll(
                        validateInputList(List.of(allowedModsString.split(",")))
                );
            }

        }

        String savePlayerModsString = properties.getProperty(SAVE_PLAYER_MODS_KEY);

        if (savePlayerModsString != null){
            savePlayerMods = Boolean.parseBoolean(savePlayerModsString);
        }

        String alwaysAllowedPlayersString = properties.getProperty(ALWAYS_ALLOWED_PLAYERS_KEY);

        if (alwaysAllowedPlayersString != null){
            alwaysAllowedPlayers.addAll(
                    validateInputList(List.of(alwaysAllowedPlayersString.split(",")))
            );
        }

        EventApi.CONFIG_RELOADED_EVENT.invoker().onConfigReloaded();
    }

    public static boolean isAlwaysAllowed(UUID playerUuid, String name) {
        return alwaysAllowedPlayers.contains(playerUuid.toString()) ||alwaysAllowedPlayers.contains(name);
    }
}
