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

            player.level().addFreshEntity(mobEntity);
            spawned.add(mobEntity);
        }
        return spawned;
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