package com.jakerthegamer.jakes_custom_commands.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class LocalChat {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<ServerPlayer, Boolean> localChatEnabled = new HashMap<>();

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("local")
                .executes(context -> {
                    return toggleLocalChat(context.getSource(), true);
                })
        );
        dispatcher.register(Commands.literal("global")
                .executes(context -> {
                    return toggleLocalChat(context.getSource(), false);
                })
        );
    }

    private static int toggleLocalChat(CommandSourceStack source, boolean enableLocal) {
        if (source.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) source.getEntity();
            localChatEnabled.put(player, enableLocal);
            source.sendSuccess(() -> Component.literal("Local chat " + (enableLocal ? "enabled" : "disabled")), false);
            return 1;
        }else {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }
    }

    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        boolean local = localChatEnabled.getOrDefault(player, false);

        if (local) {
            event.setCanceled(true); // Prevent the normal chat message from being sent
            sendLocalMessage(player, event.getMessage());
        }
    }

    private static void sendLocalMessage(ServerPlayer player, Component message) {
        Vec3 playerPos = player.position();
        double range = 25.0;

        player.getServer().getPlayerList().getPlayers().forEach(targetPlayer -> {
            targetPlayer.sendSystemMessage(Component.literal("[Local] ").append(player.getDisplayName()).append(": ").append(message));
        });
    }
}