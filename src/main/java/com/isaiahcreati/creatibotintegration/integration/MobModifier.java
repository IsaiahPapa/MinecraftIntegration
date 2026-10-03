package com.isaiahcreati.creatibotintegration.integration;

import java.util.List;

public class MobModifier {
    public String kind;
    public Float value;
    public Float multiplier;
    public String slot;
    public String itemId;
    public List<EnchantmentEntry> enchantments;

    public static class EnchantmentEntry {
        public String id;
        public int level;
    }
}