package com.jakerthegamer.jakes_custom_commands.discord;

import com.jakerthegamer.jakes_custom_commands.config.DiscordConfig;
import com.jakerthegamer.jakes_custom_commands.classes.BountyManager;
import com.jakerthegamer.jakes_custom_commands.classes.PlacedBountyObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.text.SimpleDateFormat;

public class DiscordManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static JDA jda;
    private static DiscordConfig config;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public static void initialize() {
        config = DiscordConfig.load();
        
        if (config.getBotToken().isEmpty() || config.getChannelId().isEmpty()) {
            LOGGER.warn("Discord bot not configured. Please set bot token and channel ID in config/JakesCustomCommands/discord_config.json");
            return;
        }

        try {
            jda = JDABuilder.createDefault(config.getBotToken()).build();
            jda.awaitReady();
            LOGGER.info("Discord bot initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Discord bot", e);
        }
    }

    public static void updateBountyMessage() {
        if (jda == null) return;

        TextChannel channel = jda.getTextChannelById(config.getChannelId());
        if (channel == null) {
            LOGGER.error("Could not find Discord channel");
            return;
        }

        StringBuilder content = new StringBuilder("**Active Bounties**\n\n");
        Map<UUID, List<PlacedBountyObject>> allBounties = BountyManager.getAllBounties();
        
        if (allBounties.isEmpty()) {
            content.append("No active bounties");
        } else {
            for (Map.Entry<UUID, List<PlacedBountyObject>> entry : allBounties.entrySet()) {
                UUID targetId = entry.getKey();
                List<PlacedBountyObject> bounties = entry.getValue();
                
                if (bounties.isEmpty()) continue;

                int total = bounties.stream().mapToInt(b -> b.getAmount()).sum();
                long nextExpiry = bounties.stream()
                    .mapToLong(b -> b.getExpiry())
                    .min()
                    .orElse(0);

                content.append(String.format("Player: `%s`\n", targetId))
                       .append(String.format("Total Bounty: $%d\n", total))
                       .append(String.format("Next Expiry: %s\n\n", dateFormat.format(new Date(nextExpiry))));
            }
        }

        try {
            if (config.getBountyMessageId().isEmpty()) {
                Message message = channel.sendMessage(content.toString()).complete();
                config.setBountyMessageId(message.getId());
            } else {
                channel.editMessageById(config.getBountyMessageId(), content.toString()).queue();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update Discord message", e);
        }
    }

    public static void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
} 