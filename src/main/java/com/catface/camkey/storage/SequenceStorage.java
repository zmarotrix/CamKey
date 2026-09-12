package com.catface.camkey.storage;

import com.catface.camkey.CamKey;
import com.catface.camkey.model.CameraSequence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SequenceStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CameraSequence> sequences = new HashMap<>();
    
    // Get the path to config/camkey/
    private static final Path STORAGE_DIR = FMLPaths.CONFIGDIR.get().resolve("camkey");

    // Get a sequence by name, or create a new one if it doesn't exist
    public static CameraSequence getOrCreateSequence(String name) {
        return sequences.computeIfAbsent(name, CameraSequence::new);
    }

    public static CameraSequence getSequence(String name) {
        return sequences.get(name);
    }

    // Save a specific sequence to config/camkey/<name>.json
    public static void saveSequence(String name) {
        CameraSequence seq = sequences.get(name);
        if (seq == null) return;

        File dir = STORAGE_DIR.toFile();
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, name + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(seq, writer);
            CamKey.LOGGER.info("Saved sequence: {}", name);
        } catch (IOException e) {
            CamKey.LOGGER.error("Failed to save sequence {}", name, e);
        }
    }

    // Load all .json files from config/camkey/ into memory
    public static void loadAllSequences() {
        sequences.clear();
        File dir = STORAGE_DIR.toFile();
        if (!dir.exists()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                CameraSequence seq = GSON.fromJson(reader, CameraSequence.class);
                if (seq != null) {
                    sequences.put(seq.getName(), seq);
                    CamKey.LOGGER.info("Loaded sequence: {}", seq.getName());
                }
            } catch (Exception e) {
                CamKey.LOGGER.error("Failed to load sequence file {}", file.getName(), e);
            }
        }
    }
}