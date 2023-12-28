package com.isaiahcreati.creatiintegration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SpawnPayload {

    String mobId;
    int amount;

    SpawnPayload(String payload){
        JsonElement jsonElement = JsonParser.parseString(payload);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        this.mobId = jsonObject.get("mobId").getAsString();
        this.amount = jsonObject.get("amount").getAsInt();
    }

}
