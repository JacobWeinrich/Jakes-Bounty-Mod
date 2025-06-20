package com.jakerthegamer.jakes_bounties_mod;

import java.time.Duration;
import java.time.Instant;

public class DataTypeHelper {

    public static long parseDuration(String durationStr) {
        try {
            char unit = durationStr.charAt(durationStr.length() - 1);
            int value = Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
            return switch (unit) {
                case 's' -> value * 1L;
                case 'm' -> value * 60L;
                case 'h' -> value * 3600L;
                case 'd' -> value * 86400L;
                case 'w' -> value * 604800L;
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatTimeLeft(long expiryTimestamp) {
        long secondsLeft = expiryTimestamp - Instant.now().getEpochSecond();
        Duration duration = Duration.ofSeconds(secondsLeft);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m " : "") + (seconds > 0 ? seconds + "s" : "").trim();
    }

}
