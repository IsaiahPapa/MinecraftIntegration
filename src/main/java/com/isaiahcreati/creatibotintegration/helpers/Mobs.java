package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.integration.MobModifier;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

public final class Mobs {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Random rand = new Random();

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId) {
        spawnMobNearPlayer(player, mobId, 1, "");
    }

    static public List<Entity> spawnMobNearPlayer(ServerPlayer player, String mobId, int amount, String mobName){
        return spawnMobNearPlayer(player, mobId, amount, mobName, null);
    }

    static public List<Entity> spawnMobNearPlayer(ServerPlayer player, String mobId, int amount, String mobName, List<MobModifier> modifiers){
        List<Entity> spawned = new ArrayList<>();

        if (isSpecialVariant(mobId)) {
            for (int i = 0; i < amount; i++) {
                Entity special = spawnSpecialVariant(player, mobId, mobName, modifiers);
                if (special != null) spawned.add(special);
            }
            return spawned;
        }

        EntityType<?> mob =  getMobByName(mobId);
        if(mob == null) return spawned;

        for (int i = 0; i < amount; i++) {
            Entity mobEntity  = mob.create(player.level(), EntitySpawnReason.EVENT);
            if(mobEntity == null){
                LOGGER.info("Cannot spawn mob. mobEntity is null");
            }
            Vec3 safePosition = getSafeMobPosition(player);
            if(safePosition == null) {
                LOGGER.info("Cannot spawn mob. No safe position");
                return spawned;
            };
            mobEntity.setCustomName(Component.literal(mobName));
            mobEntity.setPos(safePosition.x, safePosition.y, safePosition.z);

            // Apply modifiers BEFORE addFreshEntity — attributes like SCALE must be
            // set before the entity ticks/renders. Also call finalizeSpawn and
            // setPersistenceRequired so the mob has proper AI and doesn't despawn
            // (mirrors Taunts.spawnScaledMob at lines 515-560).
            if (modifiers != null && !modifiers.isEmpty() && mobEntity instanceof Mob modifiableMob) {
                ServerLevel level = (ServerLevel) player.level();
                modifiableMob.finalizeSpawn(level, level.getCurrentDifficultyAt(modifiableMob.blockPosition()), EntitySpawnReason.EVENT, null);
                modifiableMob.setPersistenceRequired();
                MobModifiers.apply(modifiableMob, modifiers);
            }

            if (mobEntity instanceof Mob buffableMob) {
                Buffs.applyBuffsToSpawnedMob(player, buffableMob);
            }

            player.level().addFreshEntity(mobEntity);
            spawned.add(mobEntity);
        }
        return spawned;
    }


    private static boolean isSpecialVariant(String mobId) {
        return mobId != null && switch (mobId) {
            case "minecraft:killer_bunny", "minecraft:charged_creeper", "minecraft:chicken_jockey" -> true;
            default -> false;
        };
    }

    private static Entity spawnSpecialVariant(ServerPlayer player, String mobId, String mobName, List<MobModifier> modifiers) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 pos = getSafeMobPosition(player);
        if (pos == null) return null;

        Entity entity = switch (mobId) {
            case "minecraft:killer_bunny" -> spawnKillerBunny(level, player, pos, mobName, modifiers);
            case "minecraft:charged_creeper" -> spawnChargedCreeper(level, player, pos, mobName, modifiers);
            case "minecraft:chicken_jockey" -> spawnChickenJockey(level, player, pos, mobName, modifiers);
            default -> null;
        };

        return entity;
    }

    private static Entity spawnKillerBunny(ServerLevel level, ServerPlayer player, Vec3 pos, String mobName, List<MobModifier> modifiers) {
        net.minecraft.world.entity.animal.rabbit.Rabbit rabbit = EntityType.RABBIT.create(level, EntitySpawnReason.EVENT);
        if (rabbit == null) return null;
        rabbit.setPos(pos.x, pos.y, pos.z);
        rabbit.finalizeSpawn(level, level.getCurrentDifficultyAt(rabbit.blockPosition()), EntitySpawnReason.EVENT, null);
        rabbit.setPersistenceRequired();
        try {
            java.lang.reflect.Method m = net.minecraft.world.entity.animal.rabbit.Rabbit.class.getDeclaredMethod("setVariant", net.minecraft.world.entity.animal.rabbit.Rabbit.Variant.class);
            m.setAccessible(true);
            m.invoke(rabbit, net.minecraft.world.entity.animal.rabbit.Rabbit.Variant.EVIL);
        } catch (Exception e) {
            LOGGER.warn("Failed to set killer bunny variant: {}", e.getMessage());
        }
        if (mobName != null && !mobName.isEmpty()) rabbit.setCustomName(Component.literal(mobName));
        if (modifiers != null && !modifiers.isEmpty()) MobModifiers.apply(rabbit, modifiers);
        Buffs.applyBuffsToSpawnedMob(player, rabbit);
        level.addFreshEntity(rabbit);
        return rabbit;
    }

    private static Entity spawnChargedCreeper(ServerLevel level, ServerPlayer player, Vec3 pos, String mobName, List<MobModifier> modifiers) {
        Creeper creeper = EntityType.CREEPER.create(level, EntitySpawnReason.EVENT);
        if (creeper == null) return null;
        creeper.setPos(pos.x, pos.y, pos.z);
        creeper.finalizeSpawn(level, level.getCurrentDifficultyAt(creeper.blockPosition()), EntitySpawnReason.EVENT, null);
        creeper.setPersistenceRequired();
        net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (lightning != null) {
            lightning.setVisualOnly(true);
            lightning.setPos(creeper.getX(), creeper.getY(), creeper.getZ());
            creeper.thunderHit(level, lightning);
        }
        if (mobName != null && !mobName.isEmpty()) creeper.setCustomName(Component.literal(mobName));
        if (modifiers != null && !modifiers.isEmpty()) MobModifiers.apply(creeper, modifiers);
        Buffs.applyBuffsToSpawnedMob(player, creeper);
        level.addFreshEntity(creeper);
        return creeper;
    }

    private static Entity spawnChickenJockey(ServerLevel level, ServerPlayer player, Vec3 pos, String mobName, List<MobModifier> modifiers) {
        Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.EVENT);
        if (chicken == null) return null;
        chicken.setPos(pos.x, pos.y, pos.z);
        chicken.finalizeSpawn(level, level.getCurrentDifficultyAt(chicken.blockPosition()), EntitySpawnReason.EVENT, null);
        chicken.setPersistenceRequired();
        level.addFreshEntity(chicken);

        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.EVENT);
        if (zombie == null) return chicken;
        zombie.setPos(pos.x, pos.y + 1.0, pos.z);
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.EVENT, null);
        zombie.setPersistenceRequired();
        zombie.setBaby(true);
        zombie.setTarget(player);
        if (mobName != null && !mobName.isEmpty()) zombie.setCustomName(Component.literal(mobName));
        if (modifiers != null && !modifiers.isEmpty()) MobModifiers.apply(zombie, modifiers);
        Buffs.applyBuffsToSpawnedMob(player, zombie);
        level.addFreshEntity(zombie);
        zombie.startRiding(chicken, true, true);

        return zombie;
    }

    public static EntityType<?> getMobByName(String mobId) {
        try{
            Optional<EntityType<?>> mobByString = EntityType.byString(mobId.toLowerCase());
            if(mobByString.isEmpty()){
                LOGGER.info("Mob is not present");
                return null;
            }
            return mobByString.get();
        }catch(NoSuchElementException error){
            LOGGER.info("failed to getMobByName.", error);
            return null;
        }

    }
    static public Vec3 getSafeMobPosition(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        for (int attempts = 0; attempts < 10; attempts++) {
            int x = playerPos.getX() + rand.nextInt(20) - 10;
            int z = playerPos.getZ() + rand.nextInt(20) - 10;

            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos spawnPos = new BlockPos(x, y, z);

            if (world.isEmptyBlock(spawnPos.above())) {
                return new Vec3(x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }
}