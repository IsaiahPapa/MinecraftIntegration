package com.isaiahcreati.creatibotintegration.integration.arena;

import com.mojang.math.Transformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Floating arena text, drawn with text displays. Labels are respawned every
 * time their arena is built, so any copy saved to disk is stale: those are
 * rejected when their chunk loads instead of piling up as duplicates.
 */
public final class ArenaLabels {

    private static final String LABEL_TAG = "creatibotintegration_arena_label";
    private static final Set<UUID> LIVE_LABELS = ConcurrentHashMap.newKeySet();

    private ArenaLabels() {}

    /** Removes every label inside the area and spawns the given ones. */
    public static void replace(ServerLevel level, AABB area, List<ArenaLabel> labels) {
        for (Entity entity : level.getEntities((Entity) null, area, ArenaLabels::isLabel)) {
            LIVE_LABELS.remove(entity.getUUID());
            entity.discard();
        }
        for (ArenaLabel label : labels) {
            spawn(level, label);
        }
    }

    /** True for a label entity that was not spawned during this server session. */
    public static boolean isStale(Entity entity) {
        return isLabel(entity) && !LIVE_LABELS.contains(entity.getUUID());
    }

    private static boolean isLabel(Entity entity) {
        // Older versions used invisible, named armor stands for labels.
        return entity.entityTags().contains(LABEL_TAG)
                || (entity instanceof ArmorStand stand && stand.isInvisible() && stand.getCustomName() != null);
    }

    private static void spawn(ServerLevel level, ArenaLabel label) {
        Display.TextDisplay display = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;
        display.setPos(label.x(), label.y(), label.z());
        display.setText(label.text());
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setBackgroundColor(0);
        display.setFlags(Display.TextDisplay.FLAG_SHADOW);
        if (label.scale() != 1.0F) {
            display.setTransformation(new Transformation(null, null, new Vector3f(label.scale()), null));
        }
        display.addTag(LABEL_TAG);
        LIVE_LABELS.add(display.getUUID());
        level.addFreshEntity(display);
    }
}
