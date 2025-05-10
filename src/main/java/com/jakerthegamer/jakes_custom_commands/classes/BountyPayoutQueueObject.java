package com.jakerthegamer.jakes_custom_commands.classes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakerthegamer.jakes_custom_commands.classes.QueuedPayout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.sixik.sdm_economy.api.CurrencyHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class BountyPayoutQueueObject {

    private static final File FILE = new File("config/bounty_payouts.json");
    private static final Gson gson = new Gson();

    private static final List<QueuedPayout> queue = new ArrayList<>();

    public static void queue(UUID playerId, long amount) {
        queue.add(new QueuedPayout(playerId, amount));
        save();
    }

    public static void process(ServerPlayer player) {
        UUID id = player.getUUID();
        Iterator<QueuedPayout> iter = queue.iterator();
        while (iter.hasNext()) {
            QueuedPayout payout = iter.next();
            if (payout.playerId.equals(id)) {
                CurrencyHelper.Basic.addMoney(player, payout.amount);
                player.sendSystemMessage(Component.literal("You received a delayed bounty payout of $" + payout.amount));
                iter.remove();
            }
        }
        save();
    }

    public static void load() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<List<QueuedPayout>>() {}.getType();
            List<QueuedPayout> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                queue.clear();
                queue.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            gson.toJson(queue, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
