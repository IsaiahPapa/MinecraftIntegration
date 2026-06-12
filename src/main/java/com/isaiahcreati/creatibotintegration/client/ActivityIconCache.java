package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class ActivityIconCache {

    private static final Map<String, LivingEntity> entityCache = new HashMap<>();
    private static final Map<String, LivingEntity> itemEntityCache = new HashMap<>();
    private static Level cachedLevel = null;
    private static float spinAngle = 0f;

    public static LivingEntity getOrCreateEntity(String entityTypeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        if (cachedLevel != mc.level) {
            entityCache.clear();
            itemEntityCache.clear();
            cachedLevel = mc.level;
        }

        return entityCache.computeIfAbsent(entityTypeId, id -> {
            try {
                EntityType<?> type = EntityType.byString(id).orElse(null);
                if (type == null) return null;
                if (!(type.create(mc.level, EntitySpawnReason.EVENT) instanceof LivingEntity living)) return null;
                living.setPos(0, 0, 0);
                living.yBodyRotO = 0f;
                living.yRotO = 0f;
                living.xRotO = 0f;
                return living;
            } catch (Exception e) {
                CreatiIntegration.LOGGER.warn("Failed to create display entity for icon: {}", id, e);
                return null;
            }
        });
    }

    public static LivingEntity getOrCreateItemEntity(String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        if (cachedLevel != mc.level) {
            entityCache.clear();
            itemEntityCache.clear();
            cachedLevel = mc.level;
        }

        return itemEntityCache.computeIfAbsent(itemId, id -> {
            try {
                var itemOpt = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(id));
                if (itemOpt.isEmpty()) return null;

                ItemStack itemStack = new ItemStack(itemOpt.get());

                ArmorStand stand = new ArmorStand(mc.level, 0, 0, 0);
                stand.setInvisible(true);
                stand.setNoGravity(true);
                stand.setSilent(true);
                stand.setItemSlot(EquipmentSlot.HEAD, itemStack);
                stand.setPos(0, 0, 0);
                stand.yBodyRotO = 0f;
                stand.yRotO = 0f;
                stand.xRotO = 0f;

                return stand;
            } catch (Exception e) {
                CreatiIntegration.LOGGER.warn("Failed to create item entity for icon: {}", id, e);
                return null;
            }
        });
    }

    public static void tick() {
        spinAngle += 3f;
        if (spinAngle >= 360f) spinAngle -= 360f;
    }

    public static float getSpinAngle() {
        return spinAngle;
    }

    public static void clear() {
        entityCache.clear();
        itemEntityCache.clear();
        cachedLevel = null;
    }
}