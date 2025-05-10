package com.jakerthegamer.jakes_custom_commands.events;

import com.jakerthegamer.jakes_custom_commands.classes.BountyManager;
import com.jakerthegamer.jakes_custom_commands.classes.BountyPayoutQueueObject;
import com.jakerthegamer.jakes_custom_commands.classes.BountyTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.sixik.sdm_economy.api.CurrencyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountySystemEvents {

    private long lastCheckTime = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long now = System.currentTimeMillis();
        if (now - lastCheckTime >= 1_000L) {
            BountyManager.processExpiredBounties(server);
            lastCheckTime = now;
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BountyManager.processQueuedPayouts(player);
        }
    }

    @SubscribeEvent
    public void onPlayerKilled(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        UUID victimId = victim.getUUID();

        if (!BountyManager.hasBounty(victimId)) return;

        ServerPlayer killer = null;

        // Try direct sources
        var source = event.getSource().getEntity();
        var direct = event.getSource().getDirectEntity();

        if (source instanceof ServerPlayer sp) {
            killer = sp;
        } else if (direct instanceof ServerPlayer sp) {
            killer = sp;
        }

        // Fallback to last hurt player
        if (killer == null) {
            UUID lastAttackerId = BountyTracker.getLastDamager(victimId);
            if (lastAttackerId != null) {
                killer = victim.getServer().getPlayerList().getPlayer(lastAttackerId);
            }
        }

        // No killer or suicide
        if (killer == null || killer.getUUID().equals(victimId)) return;

        int reward = BountyManager.claimBounty(victimId);
        if (reward > 0) {
            CurrencyHelper.Basic.addMoney(killer, reward);

            killer.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(killer.getName().getString() + " claimed a $" +
                            reward + " bounty on " + victim.getName().getString()), false);
        }

        BountyTracker.clearDamage(victimId); // Clean up
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            BountyTracker.recordDamage(victim.getUUID(), attacker.getUUID());
        }
    }



}
