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

/**
 * Handles persistence of Camera Sequences. 
 * Saves data to config/camkey/<sequence_name>.json using Gson.
 */
public class SequenceStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CameraSequence> sequences = new HashMap<>();
    
    // Resolve the path to the standard Minecraft config folder
    private static final Path STORAGE_DIR = FMLPaths.CONFIGDIR.get().resolve("camkey");

    /**
     * Retrieves an existing sequence from memory, or initializes a new one.
     */
    public static CameraSequence getOrCreateSequence(String name) {
        return sequences.computeIfAbsent(name, CameraSequence::new);
    }

    /**
     * Retrieves a sequence, returning null if it doesn't exist.
     */
    public static CameraSequence getSequence(String name) {
        return sequences.get(name);
    }

    /**
     * Serializes a sequence to JSON and writes it to disk.
     * Called immediately after capturing a keyframe to prevent data loss on game crash.
     */
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

    /**
     * Scans the config/camkey directory and loads all .json files into memory.
     * Intended to be called once during Mod Initialization.
     */
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

    /**
     * Deletes a sequence from memory and removes its JSON file from disk.
     * Returns true if successful, false if the sequence didn't exist.
     */
    public static boolean deleteSequence(String name) {
        // Check if it exists in memory
        if (!sequences.containsKey(name)) {
            return false;
        }
        
        // Remove from active memory
        sequences.remove(name);
        
        // Delete the actual .json file from the hard drive
        File dir = STORAGE_DIR.toFile();
        File file = new File(dir, name + ".json");
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                CamKey.LOGGER.info("Deleted sequence file: {}", file.getName());
            } else {
                CamKey.LOGGER.error("Failed to delete sequence file: {}", file.getName());
            }
        }
        return true;
    }
}