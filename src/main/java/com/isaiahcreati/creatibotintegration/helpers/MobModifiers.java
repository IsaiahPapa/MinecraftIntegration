package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.integration.MobModifier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class MobModifiers {

    public static void apply(Mob mob, List<MobModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) return;
        for (MobModifier m : modifiers) {
            applyOne(mob, m);
        }
    }

    private static void applyOne(Mob mob, MobModifier m) {
        if (m == null || m.kind == null) return;
        switch (m.kind) {
            case "scale" -> applyScale(mob, m.value != null ? m.value : 1.0f);
            case "speed" -> applySpeed(mob, m.multiplier != null ? m.multiplier : 1.0f);
            case "baby" -> applyBaby(mob);
            case "equipment" -> applyEquipment(mob, m);
            default -> {}
        }
    }

    private static void applyScale(Mob mob, float scale) {
        var attr = mob.getAttribute(Attributes.SCALE);
        if (attr != null) attr.setBaseValue(scale);
    }

    private static void applySpeed(Mob mob, float multiplier) {
        var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(attr.getBaseValue() * multiplier);
    }

    private static void applyBaby(Mob mob) {
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(true);
        }
    }

    private static void applyEquipment(Mob mob, MobModifier m) {
        if (m.itemId == null) return;
        Identifier itemId = Identifier.tryParse(m.itemId);
        if (itemId == null) return;
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack stack = new ItemStack(item);

        if (m.enchantments != null) {
            for (MobModifier.EnchantmentEntry ench : m.enchantments) {
                if (ench.id == null) continue;
                Identifier enchId = Identifier.tryParse(ench.id);
                if (enchId == null) continue;
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchId);
                mob.level().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .get(key)
                        .ifPresent(holder -> stack.enchant(holder, ench.level));
            }
        }

        EquipmentSlot slot = resolveSlot(m.slot);
        if (slot != null) {
            mob.setItemSlot(slot, stack);
        }
    }

    private static EquipmentSlot resolveSlot(String slotName) {
        if (slotName == null) return null;
        return switch (slotName) {
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "head" -> EquipmentSlot.HEAD;
            default -> null;
        };
    }
}