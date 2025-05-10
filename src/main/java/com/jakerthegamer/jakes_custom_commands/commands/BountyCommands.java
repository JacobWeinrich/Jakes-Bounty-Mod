package com.jakerthegamer.jakes_custom_commands.commands;

import com.jakerthegamer.jakes_custom_commands.classes.BountyManager;
import com.jakerthegamer.jakes_custom_commands.classes.PlacedBountyObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sixik.sdm_economy.api.CurrencyHelper;
import net.sixik.sdm_economy.common.cap.MoneyData;


import java.util.List;

public class BountyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bounty")
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            String durationStr = StringArgumentType.getString(context, "duration");

                                            long durationSeconds = com.jakerthegamer.jakes_custom_commands.DataTypeHelper.parseDuration(durationStr);


                                            /*
                                                       if (durationSeconds < 86400) { // Minimum 1 day
                                                context.getSource().sendFailure(
                                                        net.minecraft.network.chat.Component.literal("Duration must be at least 1 day (e.g., 1d, 2d)."));
                                                return 0;
                                            }
                                             */
                                            Player player = context.getSource().getPlayerOrException();
                                            long balance = CurrencyHelper.Basic.getMoney(player);

                                            if (balance >= amount) {
                                                CurrencyHelper.Basic.setMoney(player, balance - amount);

                                                long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
                                                BountyManager.addBounty(target.getUUID(), player.getUUID(), amount, expiry);

                                                context.getSource().sendSuccess(() ->
                                                        Component.literal("Bounty set on " + target.getName().getString() +
                                                                " for $" + amount + " (expires in " + durationStr + ")"), true);
                                            } else {
                                                context.getSource().sendFailure(Component.literal("You don't have enough money to place this bounty."));
                                            }


                                            return 1;
                                        })))));
        dispatcher.register(Commands.literal("bounty-admin")
                .requires(cs -> cs.hasPermission(2)) // Admin only
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            String durationStr = StringArgumentType.getString(context, "duration");

                                            long durationSeconds = com.jakerthegamer.jakes_custom_commands.DataTypeHelper.parseDuration(durationStr);

                                            if (durationSeconds <= 0) {
                                                context.getSource().sendFailure(
                                                        net.minecraft.network.chat.Component.literal("Invalid duration format. Use something like 1d, 12h, 30m."));
                                                return 0;
                                            }

                                            long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
                                            BountyManager.addBounty(target.getUUID(), source.getUUID(), amount, expiry);

                                            context.getSource().sendSuccess(() ->
                                                    net.minecraft.network.chat.Component.literal("Admin bounty set on " + target.getName().getString() +
                                                            " for $" + amount + " (expires in " + durationStr + ")"), true);

                                            return 1;
                                        })))));
        dispatcher.register(Commands.literal("bounty-check")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return showBountyInfo(context.getSource(), player);
                })
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                            return showBountyInfo(context.getSource(), target);
                        })));



    }

    private static int showBountyInfo(CommandSourceStack source, ServerPlayer target) {
        List<PlacedBountyObject> bounties = BountyManager.getRawBounties(target.getUUID())
                .stream()
                .filter(b -> b.expiry > System.currentTimeMillis())
                .toList();

        if (bounties == null || bounties.isEmpty()) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(target.getName().getString() + " has no active bounties."), false);
            return 1;
        }

        int totalAmount = bounties.stream().mapToInt(b -> b.amount).sum();
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                target.getName().getString() + " has " + bounties.size() + " active bounties totaling $" + totalAmount), false);

        for (PlacedBountyObject bounty : bounties) {
            String timeLeft = com.jakerthegamer.jakes_custom_commands.DataTypeHelper.formatTimeLeft(bounty.expiry / 1000);
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "- $" + bounty.amount + " (expires in " + timeLeft + ")"), false);
        }

        return 1;
    }


}
