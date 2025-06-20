package com.jakerthegamer.jakes_bounties_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakerthegamer.jakes_bounties_mod.helpers.DebugLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class DiscordConfig {
    private static final File CONFIG_FILE = new File("config/JakesCustomCommands/discord_config.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String webhookUrl = "";
    private String messageId = "";

    public static DiscordConfig load() {
        if (!CONFIG_FILE.exists()) {
            DiscordConfig config = new DiscordConfig();
            config.webhookUrl = "REPLACE_WITH_YOUR_DISCORD_WEBHOOK_URL";
            LOGGER.info("Creating new Discord config file at: " + CONFIG_FILE.getAbsolutePath());
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            DiscordConfig config = gson.fromJson(reader, DiscordConfig.class);
            if (config == null) {
                config = new DiscordConfig();
            }
            // If the config exists but has default/empty values, log a warning
            if (config.webhookUrl.isEmpty() || config.webhookUrl.equals("REPLACE_WITH_YOUR_DISCORD_WEBHOOK_URL")) {
                LOGGER.warn("Discord webhook not configured. Please edit config/JakesCustomCommands/discord_config.json and add your webhook URL");
            }
            return config;
        } catch (Exception e) {
            DebugLogger.error("Failed to load Discord config", e,true);
            return new DiscordConfig();
        }
    }

    public void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(this, writer);
            DebugLogger.info("Saved Discord config to: " + CONFIG_FILE.getAbsolutePath());
        } catch (Exception e) {
            DebugLogger.error("Failed to save Discord config", e,true);
        }
    }

    public String getWebhookUrl() { 
        return webhookUrl.equals("REPLACE_WITH_YOUR_DISCORD_WEBHOOK_URL") ? "" : webhookUrl; 
    }
    
    public void setWebhookUrl(String url) {
        this.webhookUrl = url;
        save();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String id) {
        this.messageId = id;
        save();
    }
} 