package com.isaiahcreati.creatiintegration;

public class Taunt {
    private String id;
    private String displayName;

    public Taunt(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }


    public String getDisplayName() {
        return displayName;
    }
}
