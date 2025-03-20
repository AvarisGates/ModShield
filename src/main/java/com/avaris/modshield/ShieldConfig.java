package com.avaris.modshield;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ShieldConfig {
    private static final ArrayList<String> disallowedMods = new ArrayList<>();
    private static final ArrayList<String> allowedMods = new ArrayList<>();
    private static final String DISALLOWED_MODS_KEY = "disallowed";
    private static final String ALLOWED_MODS_KEY = "allowed";
    private static final String CONFIG_COMMENTS = """
            This is a template ModShield config file.
            If you want to allow only certain mods add them in the 'allowed = 'option, separated by commas.
            This way only mods in that option can be used by the client.
            
            If you want to disallow mods put them int the 'disallowed = ' option, separated by commas.""";

    public static Collection<String> getDisallowedMods() {
        return disallowedMods;
    }

    public static Collection<String> getAllowedMods() {
        return allowedMods;
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

    private static Path prepareConfigFile() throws IOException {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(ModShield.MOD_ID+".properties");
        if(Files.exists(configPath)){
           return configPath;
        }

        BufferedWriter writer = Files.newBufferedWriter(configPath);

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        Properties properties = new Properties();
        properties.put(DISALLOWED_MODS_KEY,"");
        properties.put(ALLOWED_MODS_KEY,"");

        properties.store(writer,CONFIG_COMMENTS);
        writer.close();

        return configPath;
    }

    public static void load() throws IOException {
        ModShield.clearCache();

        Path configPath = prepareConfigFile();
        Properties properties = new Properties();
        BufferedReader reader = Files.newBufferedReader(configPath);
        properties.load(reader);
        reader.close();

        String disallowedModsString = properties.getProperty(DISALLOWED_MODS_KEY);
        disallowedMods.clear();

        if(disallowedModsString != null){
            disallowedMods.addAll(
                    validateInputList(List.of(disallowedModsString.split(",")))
            );
        }

        String allowedModsString = properties.getProperty(ALLOWED_MODS_KEY);
        allowedMods.clear();

        if (allowedModsString != null){
            allowedMods.addAll(
                    validateInputList(List.of(allowedModsString.split(",")))
            );
        }

        ModShield.getLogger().info("Loaded ModShield config, disallowed mods: {}, allowed mods: {}",getDisallowedMods().size(),getAllowedMods().size());
        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            ModShield.getLogger().info("Disallowed Mods:");
            for(var i : getDisallowedMods()){
                ModShield.getLogger().info("\t'{}'",i);
            }

            ModShield.getLogger().info("Allowed Mods:");
            for(var i : getAllowedMods()){
                ModShield.getLogger().info("\t'{}'",i);
            }
        }
    }
}
