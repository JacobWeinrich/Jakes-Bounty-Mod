package com.jakerthegamer.jakes_bounties_mod.classes;

import java.util.UUID;

public class PlacedBountyObject {
    public final UUID placer;
    public final int amount;
    public final long expiry;
    public final boolean isAdmin;

    public PlacedBountyObject(UUID placer, int amount, long expiry, boolean isAdmin) {
        this.placer = placer;
        this.amount = amount;
        this.expiry = expiry;
        this.isAdmin = isAdmin;
    }

    public int getAmount() {
        return amount;
    }

    public long getExpiry() {
        return expiry;
    }
}

