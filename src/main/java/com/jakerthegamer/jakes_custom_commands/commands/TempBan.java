package com.jakerthegamer.jakes_custom_commands.commands;

import com.google.gson.reflect.TypeToken;
import com.jakerthegamer.jakes_custom_commands.DataManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.event.ServerChatEvent;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.jakerthegamer.jakes_custom_commands.DataTypeHelper.formatTimeLeft;
import static com.jakerthegamer.jakes_custom_commands.DataTypeHelper.parseDuration;

public class TempBan {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Path BAN_DATA_PATH = Paths.get("config/temp_bans.json");
    private static final Type BAN_DATA_TYPE = new TypeToken<Map<UUID, BanInfo>>() {}.getType();
    private static Map<UUID, BanInfo> banData = new HashMap<>();

    static {
        loadBanData();
    }

    // Register Command
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerTempBan(dispatcher);
        registerUnBan(dispatcher);
    }

    public static void registerUnBan(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unban")
                .requires(source -> source.hasPermission(3)) // Requires admin permission
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            return unBan(context.getSource(), playerName);
                        })));
    }

    public static void registerTempBan(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                .requires(source -> source.hasPermission(3)) // Admin level
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.string())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playerName = GameProfileArgument.getGameProfiles(context, "player").iterator().next().getName();
                                            String durationStr = StringArgumentType.getString(context, "duration");
                                            String reason = StringArgumentType.getString(context, "reason");
                                            CommandSourceStack source = context.getSource();

                                            long durationSeconds = parseDuration(durationStr);
                                            if (durationSeconds <= 0) {
                                                source.sendFailure(Component.literal("Invalid duration format. Use 1m, 2h, 3d, etc."));
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
                                            if (player == null) {
                                                source.sendFailure(Component.literal("Player not found."));
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            banPlayer(player, durationSeconds, reason, source.getServer());
                                            source.sendSuccess(() -> Component.literal(playerName + " has been temp-banned for " + durationStr + " Reason: " + reason), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))));
    }


    private static void banPlayer(ServerPlayer player, long durationSeconds, String reason, MinecraftServer server) {
        UserBanList banList = server.getPlayerList().getBans();
        UUID playerUUID = player.getUUID();

        // Set ban expiry time
        long expiryTimestamp = Instant.now().plusSeconds(durationSeconds).getEpochSecond();
        banData.put(playerUUID, new BanInfo(expiryTimestamp, reason)); // Ensure expiryTimestamp is long
        saveBanData();
        // Kick the player
        player.connection.disconnect(Component.literal("You have been temporarily banned for " + durationSeconds + " seconds. Reason: " + reason));

        // Schedule unban
        scheduler.schedule(() -> {
            server.execute(() -> {
                if (banData.containsKey(playerUUID) && banData.get(playerUUID).expiry <= Instant.now().getEpochSecond()) {
                    banList.remove(server.getProfileCache().get(playerUUID).orElse(null));
                    banData.remove(playerUUID);
                    saveBanData();
                }
            });
        }, durationSeconds, TimeUnit.SECONDS);

    }

    private static int unBan(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        PlayerList playerList = server.getPlayerList();
        GameProfileCache profileCache = server.getProfileCache();

        Optional<GameProfile> profileOpt = profileCache.get(playerName);
        if (profileOpt.isEmpty()) {
            source.sendFailure(Component.nullToEmpty("Player not found."));
            return 0;
        }
        UUID playerUUID = profileOpt.get().getId();
        if (isBanned(playerUUID)) {
            banData.remove(playerUUID);
            saveBanData();
            source.sendSuccess(() -> Component.nullToEmpty("Player " + playerName + " has been unbanned!"), true);
            return Command.SINGLE_SUCCESS;
        }
        GameProfile profile = profileOpt.get();
        source.sendFailure(Component.nullToEmpty("Player is not banned."));
        return 0;
    }


    private static void saveBanData() {
        DataManager.saveData(BAN_DATA_PATH, banData);
    }

    private static void loadBanData() {
        banData = DataManager.loadData(BAN_DATA_PATH, BAN_DATA_TYPE, new HashMap<>());
    }

    public static class BanInfo {
        public long expiry;
        public String reason;

        public BanInfo(long expiry, String reason) {
            this.expiry = expiry;
            this.reason = reason;
        }
    }

    public static boolean isBanned(UUID playerUUID) {
        if (banData.containsKey(playerUUID)) {
            BanInfo banInfo = banData.get(playerUUID);
            long timeLeft = banInfo.expiry - Instant.now().getEpochSecond();

            if (timeLeft > 0) {
                return true;
            }
        }
        return false;
    }

    public static void onPlayerJoin(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
            if (isBanned(playerUUID)) {
                BanInfo banInfo = banData.get(playerUUID);
                String updatedBanMessage = "You are still banned for " + formatTimeLeft(banInfo.expiry) + ". Reason: " + banInfo.reason;
                player.connection.disconnect(Component.literal(updatedBanMessage));
            } else {
                // Unban if the time has expired
                    banData.remove(playerUUID);
                    saveBanData();
            }

    }


}
