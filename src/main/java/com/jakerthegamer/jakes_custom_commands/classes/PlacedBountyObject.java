package com.jakerthegamer.jakes_custom_commands.classes;

import java.util.UUID;

public class PlacedBountyObject {
    public UUID placer;
    public int amount;
    public long timestamp;
    public long expiry;

    public PlacedBountyObject() {}

    public PlacedBountyObject(UUID placer, int amount, long expiry) {
        this.placer = placer;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.expiry = expiry;
    }
}

