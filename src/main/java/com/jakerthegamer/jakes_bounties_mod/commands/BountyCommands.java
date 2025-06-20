package com.jakerthegamer.jakes_bounties_mod.commands;

import com.jakerthegamer.jakes_bounties_mod.DataTypeHelper;
import com.jakerthegamer.jakes_bounties_mod.classes.BountyManager;
import com.jakerthegamer.jakes_bounties_mod.classes.BountyPayoutQueueObject;
import com.jakerthegamer.jakes_bounties_mod.classes.PlacedBountyObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sixik.sdm_economy.api.CurrencyHelper;
import com.jakerthegamer.jakes_bounties_mod.commands.PvpKeepInventory;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class BountyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bounty")
                .requires(source -> source.hasPermission(0)) // ✅ allow everyone
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(context -> {
                                            context.getSource().withSuppressedOutput(); // Don't broadcast to System
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            String durationStr = StringArgumentType.getString(context, "duration");

                                            long durationSeconds = com.jakerthegamer.jakes_bounties_mod.DataTypeHelper.parseDuration(durationStr);


                                            if (durationSeconds < 28800) { // Minimum 8 hours
                                                context.getSource().sendFailure(
                                                        net.minecraft.network.chat.Component.literal("Duration must be at least 8 hours (e.g., 8h, 1d).").withStyle(ChatFormatting.YELLOW));
                                                return 0;
                                            }

                                            Player player = context.getSource().getPlayerOrException();
                                            long balance = CurrencyHelper.Basic.getMoney(player);

                                            if (balance >= amount) {
                                                CurrencyHelper.Basic.setMoney(player, balance - amount);

                                                long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
                                                BountyManager.addBounty(target.getUUID(), player.getUUID(), amount, expiry);

                                                context.getSource().sendSuccess(() ->
                                                                Component.literal("Bounty set on ")
                                                                        .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.GOLD))
                                                                        .append(Component.literal(" for $").withStyle(ChatFormatting.GRAY))
                                                                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                                                                        .append(Component.literal(" (expires in ").withStyle(ChatFormatting.GRAY))
                                                                        .append(Component.literal(durationStr).withStyle(ChatFormatting.AQUA))
                                                                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY)),
                                                        false
                                                );

                                                Component broadcast = Component.literal(
                                                        player.getName().getString() + " has placed a $" + amount + " bounty on " +
                                                                target.getName().getString() + "!")
                                                        .withStyle(ChatFormatting.GOLD);

                                                player.getServer().getPlayerList().broadcastSystemMessage(broadcast, false);

                                            } else {
                                                context.getSource().sendFailure(Component.literal("You don't have enough money to place this bounty.").withStyle(ChatFormatting.YELLOW));
                                            }


                                            return 1;
                                        })))));
        dispatcher.register(Commands.literal("bounty-admin")
                .requires(source -> source.hasPermission(2)) // Level 2 = OP-only by default
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(context -> {
                                            context.getSource().withSuppressedOutput(); // Don't broadcast to System
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            String durationStr = StringArgumentType.getString(context, "duration");

                                            long durationSeconds = com.jakerthegamer.jakes_bounties_mod.DataTypeHelper.parseDuration(durationStr);

                                            if (durationSeconds < 10) {
                                                context.getSource().sendFailure(
                                                        net.minecraft.network.chat.Component.literal("Invalid duration format. Use something like 1d, 12h, 30m.").withStyle(ChatFormatting.YELLOW));
                                                return 0;
                                            }

                                            long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
                                            BountyManager.addBounty(target.getUUID(), source.getUUID(), amount, expiry, true);

                                            context.getSource().sendSuccess(() ->
                                                            Component.literal("Admin bounty set on ")
                                                                    .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.GOLD))
                                                                    .append(Component.literal(" for $").withStyle(ChatFormatting.GRAY))
                                                                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                                                                    .append(Component.literal(" (expires in ").withStyle(ChatFormatting.GRAY))
                                                                    .append(Component.literal(durationStr).withStyle(ChatFormatting.AQUA))
                                                                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY)),
                                                    false
                                            );

                                            ServerPlayer admin = context.getSource().getPlayerOrException();

                                            Component broadcast = Component.literal("[ADMIN] ")
                                                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                                                    .append(Component.literal("A $" + amount + " bounty was placed on " +
                                                            target.getName().getString() + "!").withStyle(ChatFormatting.RED));

                                            admin.getServer().getPlayerList().broadcastSystemMessage(broadcast, false);

                                            return 1;
                                        })))));
        dispatcher.register(Commands.literal("bounty-check")
                .requires(source -> source.hasPermission(0)) // ✅ allow everyone
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return showBountyInfo(context.getSource(), player);
                })
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                            return showBountyInfo(context.getSource(), target);
                        })));
        dispatcher.register(Commands.literal("bounty-payoff")
                .requires(source -> source.hasPermission(0)) // ✅ allow everyone
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            try {
                                ServerPlayer self = context.getSource().getPlayerOrException();
                                UUID selfId = self.getUUID();
                                int index = IntegerArgumentType.getInteger(context, "index");

                                List<PlacedBountyObject> bounties = new ArrayList<>(
                                        BountyManager.getRawBounties(selfId)
                                                .stream().filter(b -> b.expiry > System.currentTimeMillis()).toList()
                                );


                                if (bounties.isEmpty() || index <= 0 || index > bounties.size()) {
                                    context.getSource().sendFailure(Component.literal("Invalid bounty index."));
                                    return 0;
                                }

                                PlacedBountyObject bounty = bounties.get(index - 1);

                                long costToPayOff = (long) (bounty.amount * 1.25);
                                long bonus = (long) (bounty.amount * 0.05);
                                long payout = bounty.amount + bonus;

                                long balance = CurrencyHelper.Basic.getMoney(self);
                                if (balance < costToPayOff) {
                                    context.getSource().sendFailure(Component.literal("You need $" + costToPayOff + " to pay off this bounty."));
                                    return 0;
                                }

                                // Deduct from payer
                                CurrencyHelper.Basic.setMoney(self, balance - costToPayOff);

                                boolean isAdmin = bounty.isAdmin;

                                if (!isAdmin) {
                                    ServerPlayer placer = self.server.getPlayerList().getPlayer(bounty.placer);
                                    if (placer != null) {
                                        CurrencyHelper.Basic.addMoney(placer, payout);
                                        placer.sendSystemMessage(Component.literal(
                                                self.getName().getString() + " paid off their bounty. You received $" + payout));
                                    } else {
                                        BountyPayoutQueueObject.queue(bounty.placer, payout);
                                    }
                                }

                                bounties.remove(bounty);
                                BountyManager.setBountiesForPlayer(selfId, bounties);

                                boolean placerOnline = self.server.getPlayerList().getPlayer(bounty.placer) != null;

                                context.getSource().sendSuccess(() -> Component.literal(
                                        "You paid off bounty #" + index + " for $" + costToPayOff +
                                                (isAdmin
                                                        ? ". No refund was issued (admin bounty)."
                                                        : ". The placer " + (placerOnline ? "received" : "will receive") + " $" + payout + ".")),
                                        false);

                                return 1;

                            } catch (Exception e) {
                                e.printStackTrace();
                                context.getSource().sendFailure(Component.literal("An unexpected error occurred: " + e.getMessage()));
                                return 0;
                            }
                        })));




    }
    private static int showBountyInfo(CommandSourceStack source, ServerPlayer target) {
        List<PlacedBountyObject> bounties = BountyManager.getRawBounties(target.getUUID())
                .stream()
                .filter(b -> b.expiry > System.currentTimeMillis())
                .toList();

        if (bounties == null || bounties.isEmpty()) {
            source.sendSuccess(() -> Component.literal(target.getName().getString() + " has no active bounties."), false);
            return 1;
        }

        int totalAmount = bounties.stream().mapToInt(b -> b.amount).sum();
        source.sendSuccess(() -> Component.literal(
                target.getName().getString() + " has " + bounties.size() + " active bounties totaling $" + totalAmount), false);

        int index = 1;
        UUID requesterId;
        try {
            requesterId = source.getPlayerOrException().getUUID();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("You must be a player to run this command."));
            return 0;
        }

        UUID targetId = target.getUUID();

        for (PlacedBountyObject bounty : bounties) {
            long payoffCost = (long) (bounty.amount * 1.25);
            String timeLeft = DataTypeHelper.formatTimeLeft(bounty.expiry / 1000);
            String placerName = target.getServer().getProfileCache()
                    .get(bounty.placer)
                    .map(GameProfile::getName)
                    .orElse("Unknown");

            Component placerDisplay = bounty.isAdmin
                    ? Component.literal("[ADMIN]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.RED)
                            .withBold(true)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("This bounty was placed by an administrator and cannot be refunded."))))
                    : Component.literal(placerName);


            final int displayIndex = index;

            MutableComponent line = Component.literal("[" + displayIndex + "] $" + bounty.amount + " from ")
                    .append(placerDisplay)
                    .append(Component.literal(" (expires in " + timeLeft));

            if (requesterId.equals(targetId)) {
                line.append(Component.literal(", payoff: $" + payoffCost));
            }

            line.append(Component.literal(")"));

            source.sendSuccess(() -> line.withStyle(ChatFormatting.YELLOW), false);
            index++;
        }

        return 1;
    }



}
