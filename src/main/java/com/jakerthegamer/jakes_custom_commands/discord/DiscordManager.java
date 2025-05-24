package com.jakerthegamer.jakes_custom_commands.discord;

import com.jakerthegamer.jakes_custom_commands.config.DiscordConfig;
import com.jakerthegamer.jakes_custom_commands.classes.BountyManager;
import com.jakerthegamer.jakes_custom_commands.classes.PlacedBountyObject;
import net.minecraft.server.MinecraftServer;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DiscordManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static DiscordConfig config;

    public static void initialize() {
        config = DiscordConfig.load();
        
        if (config.getWebhookUrl().isEmpty()) {
            LOGGER.warn("Discord webhook not configured. Please set webhook URL in config/JakesCustomCommands/discord_config.json");
            return;
        }
        
        // Create initial message on startup
        updateBountyMessage();
    }

    private static void deleteMessage(String webhookUrl, String messageId) throws IOException {
        String[] parts = webhookUrl.split("/");
        String webhookId = parts[parts.length - 2];
        String webhookToken = parts[parts.length - 1];
        
        String deleteUrl = String.format("https://discord.com/api/v10/webhooks/%s/%s/messages/%s",
            webhookId, webhookToken, messageId);
        
        URL url = new URL(deleteUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        
        int responseCode = conn.getResponseCode();
        LOGGER.info("Discord API response code (delete): " + responseCode);
        
        if (responseCode != 204) {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream))) {
                    String error = br.readLine();
                    LOGGER.error("Failed to delete message. Error: " + error);
                } catch (Exception e) {
                    LOGGER.error("Failed to read error stream", e);
                }
            } else {
                LOGGER.error("Failed to delete message. Response code: " + responseCode);
            }
        }
    }

    private static String createMessage(String webhookUrl, String json) throws IOException {
        // Parse webhook URL to get ID and token
        String[] parts = webhookUrl.split("/");
        String webhookId = parts[parts.length - 2];
        String webhookToken = parts[parts.length - 1];
        
        // Use the webhook URL with wait=true to get the message response
        URL url = new URL(webhookUrl + "?wait=true");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        LOGGER.info("Discord API response code (create): " + responseCode);

        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = br.readLine();
                LOGGER.debug("Discord API response: " + response);
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                return jsonResponse.get("id").getAsString();
            }
        } else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream))) {
                    String error = br.readLine();
                    LOGGER.error("Failed to create message. Error: " + error);
                } catch (Exception e) {
                    LOGGER.error("Failed to read error stream", e);
                }
            } else {
                LOGGER.error("Failed to create message. Response code: " + responseCode);
            }
            return null;
        }
    }

    public static void updateBountyMessage() {
        if (config == null || config.getWebhookUrl().isEmpty()) return;

        Map<UUID, List<PlacedBountyObject>> allBounties = BountyManager.getAllBounties();
        
        // Create the JSON structure for a nice looking embed
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"embeds\": [{");
        jsonBuilder.append("\"title\": \"🎯 Bounty Board\",");
        // Use a nice dark red color for the embed
        jsonBuilder.append("\"color\": 15158332,"); // This is #E74C3C in decimal
        
        StringBuilder description = new StringBuilder();
        
        if (allBounties.isEmpty()) {
            description.append("*No active bounties at this time*");
        } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            
            for (Map.Entry<UUID, List<PlacedBountyObject>> entry : allBounties.entrySet()) {
                UUID targetId = entry.getKey();
                List<PlacedBountyObject> bounties = entry.getValue();
                
                if (bounties.isEmpty()) continue;

                int total = bounties.stream().mapToInt(b -> b.getAmount()).sum();
                
                GameProfile profile = server.getProfileCache().get(targetId).orElse(null);
                String playerName = profile != null ? profile.getName() : "Unknown Player";

                description.append("**").append(playerName).append("**")
                          .append("\n💰 Bounty: `$").append(String.format("%,d", total)).append("`\n\n");
            }
        }
        
        jsonBuilder.append("\"description\": ").append(escapeJson(description.toString()));
        jsonBuilder.append(",\"footer\": {\"text\": \"Last updated: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")) + "\"}");
        jsonBuilder.append("}]}");

        try {
            String webhookUrl = config.getWebhookUrl();
            String messageId = config.getMessageId();
            
            // If we have an existing message, try to delete it first
            if (!messageId.isEmpty()) {
                try {
                    deleteMessage(webhookUrl, messageId);
                    // Clear the message ID after successful deletion
                    config.setMessageId("");
                    config.save();
                } catch (Exception e) {
                    LOGGER.error("Failed to delete old message", e);
                    // Continue anyway to try creating a new message
                }
            }

            // Create new message
            String newMessageId = createMessage(webhookUrl, jsonBuilder.toString());
            if (newMessageId != null) {
                config.setMessageId(newMessageId);
                LOGGER.info("Created new Discord message with ID: " + newMessageId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update Discord message", e);
            e.printStackTrace();
        }
    }

    private static String escapeJson(String raw) {
        return "\"" + raw.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t") + "\"";
    }

    public static void shutdown() {
        // Nothing to do for webhook implementation
    }
} 