package com.jakerthegamer.jakes_bounties_mod.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyTracker {
    private static final Map<UUID, UUID> lastDamager = new HashMap<>();

    public static void recordDamage(UUID victim, UUID attacker) {
        lastDamager.put(victim, attacker);
    }

    public static UUID getLastDamager(UUID victim) {
        return lastDamager.get(victim);
    }

    public static void clearDamage(UUID victim) {
        lastDamager.remove(victim);
    }

}
