package com.isaiahcreati.creatibotintegration.integration;

import com.google.gson.*;

import java.lang.reflect.Type;

// Custom deserializer (simplified example)
public class PayloadDeserializer implements JsonDeserializer<Payload> {

    @Override
    public Payload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String action = jsonObject.get("action").getAsString();

        Payload payload = new Payload();
        payload.action = Action.fromString(action);
        payload.metadata = context.deserialize(jsonObject.get("metadata"), Metadata.class);

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