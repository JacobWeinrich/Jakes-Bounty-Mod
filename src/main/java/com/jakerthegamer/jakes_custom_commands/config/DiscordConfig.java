package com.jakerthegamer.jakes_custom_commands.config;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class DiscordConfig {
    private static final File CONFIG_FILE = new File("config/JakesCustomCommands/discord_config.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new Gson();

    private String botToken = "";
    private String channelId = "";
    private String bountyMessageId = "";

    public static DiscordConfig load() {
        if (!CONFIG_FILE.exists()) {
            DiscordConfig config = new DiscordConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return gson.fromJson(reader, DiscordConfig.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load Discord config", e);
            return new DiscordConfig();
        }
    }

    public void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save Discord config", e);
        }
    }

    public String getBotToken() { return botToken; }
    public String getChannelId() { return channelId; }
    public String getBountyMessageId() { return bountyMessageId; }
    public void setBountyMessageId(String id) { 
        this.bountyMessageId = id;
        save();
    }
} 