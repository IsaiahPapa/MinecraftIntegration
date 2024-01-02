package com.isaiahcreati.creatiintegration;

import com.google.gson.*;

import java.lang.reflect.Type;

enum Action {
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


class Payload<T> {
    Action action;
    T details;
}

// Detail classes
class ItemDetails {
    String type;
    String itemId;
    int amount;
}

class EffectDetails {
    String effectId;
    int amplifier;
    int duration;
}

class SpawnDetails {
    String type;
    String mobId;
    int amount;
}

class TauntDetails {
    String tauntId;
}

// Custom deserializer (simplified example)
class PayloadDeserializer implements JsonDeserializer<Payload> {
    @Override
    public Payload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String action = jsonObject.get("action").getAsString();

        Payload payload = new Payload();
        payload.action = Action.fromString(action);

        switch (action) {
            case "give":
            case "take":
                payload.details = context.deserialize(jsonObject.get("details"), ItemDetails.class);
                break;
            case "spawn":
                payload.details = context.deserialize(jsonObject.get("details"), SpawnDetails.class);
                break;
            case "effect":
                payload.details = context.deserialize(jsonObject.get("details"), EffectDetails.class);
                break;
            case "taunt":
                payload.details = context.deserialize(jsonObject.get("details"), TauntDetails.class);
                break;
            // Add cases for other action types
        }

        return payload;
    }
}