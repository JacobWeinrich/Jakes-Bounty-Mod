package com.jakerthegamer.jakes_custom_commands.commands;

import com.jakerthegamer.jakes_custom_commands.DataManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import java.util.HashSet;
import java.util.UUID;

import com.google.gson.reflect.TypeToken;

public class PvpKeepInventory {
    private static final HashSet<UUID> enabledPlayers = new HashSet<>();
    private static final Path DATA_FILE = Path.of("config/pvpkeepinv.json");
    private static final Type DATA_TYPE = new TypeToken<HashSet<UUID>>() {}.getType();


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("togglepvpkeepinv")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.literal("true")
                                .executes(ctx -> {
                                    // Get the target player from the argument
                                    ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    UUID playerUUID = targetPlayer.getUUID();

                                    // Set to true: enable keep inventory
                                    enabledPlayers.add(playerUUID);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            targetPlayer.getName().getString() + " will now keep inventory on PvP death."
                                    ), true);
                                    DataManager.saveData(DATA_FILE, enabledPlayers);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("false")
                                .executes(ctx -> {
                                    // Get the target player from the argument
                                    ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    UUID playerUUID = targetPlayer.getUUID();

                                    // Set to false: disable keep inventory
                                    enabledPlayers.remove(playerUUID);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            targetPlayer.getName().getString() + " will no longer keep inventory on PvP death."
                                    ), true);
                                    DataManager.saveData(DATA_FILE, enabledPlayers);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
        );
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