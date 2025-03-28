package com.jakerthegamer.jakes_custom_commands.commands;

import com.jakerthegamer.jakes_custom_commands.DataManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.reflect.TypeToken;

public class PvpKeepInventory {
    private static final Path DATA_FILE = Path.of("config/pvpkeepinv.json");
    private static final Type DATA_TYPE = new TypeToken<HashSet<UUID>>() {}.getType();
    private static final Set<UUID> enabledPlayers = DataManager.loadData(DATA_FILE, DATA_TYPE, new HashSet<>());
    private static final Map<UUID, Integer> activeTimers = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("togglepvpkeepinv")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2+
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.literal("true")
                                .executes(ctx -> toggleKeepInventory(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player").iterator().next(), -1, true))) // Enable permanently
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                    return toggleKeepInventory(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player").iterator().next(), duration, true);
                                })
                        )
                )
                .then(Commands.literal("false")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> toggleKeepInventory(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player").iterator().next(), -1, false))) // Disable
                )
        );
    }


    private static int toggleKeepInventory(CommandSourceStack source, GameProfile targetProfile, int duration, boolean enable) {
        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(targetProfile.getId());

        if (targetPlayer != null) {
            UUID playerUUID = targetPlayer.getUUID();

            if (enable) {
                enabledPlayers.add(playerUUID);
                DataManager.saveData(DATA_FILE, enabledPlayers);
                source.sendSuccess(() -> Component.literal(targetPlayer.getName().getString() + " will now keep inventory on PvP death."), true);

                if (duration > 0) {
                    activeTimers.put(playerUUID, duration);
                    scheduler.schedule(() -> {
                        enabledPlayers.remove(playerUUID);
                        activeTimers.remove(playerUUID);
                        DataManager.saveData(DATA_FILE, enabledPlayers);
                        targetPlayer.sendSystemMessage(Component.literal("Your PvP keep inventory effect has expired."));
                    }, duration, TimeUnit.SECONDS);
                }
            } else {
                enabledPlayers.remove(playerUUID);
                activeTimers.remove(playerUUID);
                DataManager.saveData(DATA_FILE, enabledPlayers);
                source.sendSuccess(() -> Component.literal(targetPlayer.getName().getString() + " will no longer keep inventory on PvP death."), true);
            }

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Player not found!"));
            return 0;
        }
    }


    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if (event.getSource().getEntity() instanceof Player) {
                if (enabledPlayers.contains(player.getUUID())) {
                    player.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, player.getServer());
                    player.sendSystemMessage(Component.literal("Your inventory was kept due to PvP death."));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer) { // Check if entity is a ServerPlayer
            ServerPlayer player = (ServerPlayer) event.getEntity(); // Explicit casting

            player.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, player.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        enabledPlayers.clear();
        enabledPlayers.addAll(DataManager.loadData(DATA_FILE, DATA_TYPE, new HashSet<>()));
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppedEvent event) {
        DataManager.saveData(DATA_FILE, enabledPlayers);
    }
}