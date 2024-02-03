package com.isaiahcreati.creatiintegration.integration;

public enum Action {
    GIVE, TAKE, SPAWN, TAUNT, EFFECT;

    // Optional: Method to convert string to enum
    public static Action fromString(String action) throws IllegalArgumentException {
        for (Action a : Action.values()) {
            if (a.name().equalsIgnoreCase(action)) {
                return a;
            }
        }
        throw new IllegalArgumentException("No constant with text " + action + " found");
    }
}