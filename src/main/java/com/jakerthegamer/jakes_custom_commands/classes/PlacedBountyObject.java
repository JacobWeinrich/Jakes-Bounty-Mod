package com.jakerthegamer.jakes_custom_commands.classes;

import java.util.UUID;

public class PlacedBountyObject {
    public UUID placer;
    public int amount;
    public long timestamp;
    public long expiry;
    public boolean isAdmin;

    public PlacedBountyObject() {}

    public PlacedBountyObject(UUID placer, int amount, long expiry, boolean isAdmin) {
        this.placer = placer;
        this.amount = amount;
        this.expiry = expiry;
        this.timestamp = System.currentTimeMillis();
        this.isAdmin = isAdmin;
    }

}

