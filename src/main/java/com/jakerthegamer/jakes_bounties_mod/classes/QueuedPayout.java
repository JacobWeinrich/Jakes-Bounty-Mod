package com.jakerthegamer.jakes_bounties_mod.classes;

import java.util.UUID;

public class QueuedPayout {
    public UUID playerId;
    public long amount;

    public QueuedPayout() {}

    public QueuedPayout(UUID playerId, long amount) {
        this.playerId = playerId;
        this.amount = amount;
    }
}
