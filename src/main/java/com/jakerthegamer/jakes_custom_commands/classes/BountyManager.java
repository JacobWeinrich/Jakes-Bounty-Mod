package com.jakerthegamer.jakes_custom_commands.classes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import net.sixik.sdm_economy.api.CurrencyHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class BountyManager {
    private static final File FILE = new File("config/bounties.json");
    private static final Gson gson = new Gson();
    private static final Map<UUID, List<PlacedBountyObject>> bounties = new HashMap<>();
    public static final Logger LOGGER = LogManager.getLogger(); // [Guide: Logger for outputting debug/info messages.]

    public static void load() {
        try {
            if (FILE.exists()) {
                FileReader reader = new FileReader(FILE);
                Map<UUID, List<PlacedBountyObject>> loaded = gson.fromJson(reader,
                        new com.google.gson.reflect.TypeToken<Map<UUID, List<PlacedBountyObject>>>() {}.getType());
                if (loaded != null) {
                    bounties.clear();
                    bounties.putAll(loaded);
                }
                reader.close();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load bounty data", e);
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            gson.toJson(bounties, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save bounty data", e);
        }
    }

    public static void addBounty(UUID targetId, UUID placerId, int amount, long expiryTimeMillis) {
        bounties.computeIfAbsent(targetId, k -> new ArrayList<>())
                .add(new PlacedBountyObject(placerId, amount, expiryTimeMillis));
        save();
    }


    public static int claimBounty(UUID targetId) {
        List<PlacedBountyObject> bounties = BountyManager.bounties.get(targetId);
        if (bounties == null || bounties.isEmpty()) return 0;

        long now = System.currentTimeMillis();

        // Remove expired bounties (and do NOT pay for them)
        bounties.removeIf(b -> b.expiry <= now);

        if (bounties.isEmpty()) {
            BountyManager.bounties.remove(targetId);
            save();
            return 0;
        }

        int total = bounties.stream().mapToInt(b -> b.amount).sum();
        BountyManager.bounties.remove(targetId); // Claim clears it all
        save();
        return total;
    }


    public static boolean hasBounty(UUID uuid) {
        return bounties.containsKey(uuid);
    }

    public static int getBounty(UUID targetId) {
        return bounties.getOrDefault(targetId, Collections.emptyList())
                .stream().mapToInt(b -> b.amount).sum();
    }

    public static void processExpiredBounties(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, List<PlacedBountyObject>>> iterator = bounties.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, List<PlacedBountyObject>> entry = iterator.next();
            UUID targetUUID = entry.getKey();
            List<PlacedBountyObject> bountyList = entry.getValue();

            Iterator<PlacedBountyObject> bountyIter = bountyList.iterator();
            while (bountyIter.hasNext()) {
                PlacedBountyObject bounty = bountyIter.next();
                if (bounty.expiry <= now) {
                    int refund = (int) (bounty.amount * 0.75);
                    int reward = bounty.amount - refund;

                    ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                    ServerPlayer placer = server.getPlayerList().getPlayer(bounty.placer);

                    if (placer != null) {
                        CurrencyHelper.Basic.addMoney(placer, refund);
                    } else {
                        BountyPayoutQueueObject.queue(bounty.placer, refund);
                        LOGGER.info("Placer offline, queued refund: $" + refund);
                    }

                    if (target != null) {
                        CurrencyHelper.Basic.addMoney(target, reward);
                    } else {
                        BountyPayoutQueueObject.queue(targetUUID, reward);
                        LOGGER.info("Target offline, queued reward: $" + reward);
                    }

                    LOGGER.info("Expired bounty on " + targetUUID + ": $" + bounty.amount +
                            " → $" + refund + " to placer, $" + reward + " to target.");

                    bountyIter.remove();
                }
            }

            if (bountyList.isEmpty()) {
                iterator.remove();
            }
        }

        save();
    }


    public static List<PlacedBountyObject> getRawBounties(UUID targetId) {
        return bounties.getOrDefault(targetId, Collections.emptyList());
    }

    // Queue if player was ofline
    private static final List<QueuedPayout> payoutQueue = new ArrayList<>();

    public static void queuePayout(UUID playerId, long amount) {
        payoutQueue.add(new QueuedPayout(playerId, amount));
    }

    public static void processQueuedPayouts(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Iterator<QueuedPayout> iter = payoutQueue.iterator();
        while (iter.hasNext()) {
            QueuedPayout payout = iter.next();
            if (payout.playerId.equals(playerId)) {
                CurrencyHelper.Basic.addMoney(player, payout.amount);
                player.sendSystemMessage(Component.literal("You received a delayed bounty payout of $" + payout.amount));
                iter.remove();
            }
        }
    }




}
