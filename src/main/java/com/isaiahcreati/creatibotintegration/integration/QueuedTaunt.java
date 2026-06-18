package com.isaiahcreati.creatibotintegration.integration;

public class QueuedTaunt {
    private final String tauntId;
    private final String redeemerName;
    private final int durationSeconds;
    private final long queuedTick;

    public QueuedTaunt(String tauntId, String redeemerName, int durationSeconds, long queuedTick) {
        this.tauntId = tauntId;
        this.redeemerName = redeemerName;
        this.durationSeconds = durationSeconds;
        this.queuedTick = queuedTick;
    }

    public String getTauntId() { return tauntId; }
    public String getRedeemerName() { return redeemerName; }
    public int getDurationSeconds() { return durationSeconds; }
    public long getQueuedTick() { return queuedTick; }
}