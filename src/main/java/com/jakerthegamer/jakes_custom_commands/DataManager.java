package com.jakerthegamer.jakes_custom_commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Saves data to a specified JSON file.
     * @param path Path to the file
     * @param data Data to save
     */
    public static <T> void saveData(Path path, T data) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads data from a JSON file.
     * @param path Path to the file
     * @param type Type of data to load
     * @return Loaded data or a new instance if file doesn't exist
     */
    public static <T> T loadData(Path path, Type type, T defaultValue) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                T data = GSON.fromJson(reader, type);
                return (data != null) ? data : defaultValue;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }
}
