package com.isaiahcreati.creatiintegration.helpers;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

public final class Mobs {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Random rand = new Random();

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId) {
        spawnMobNearPlayer(player, mobId, 1, "");
    }

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId, int amount, String mobName){
        // Find the Entity given the mobId
        EntityType<?> mob =  getMobByName(mobId);
        if(mob == null) return;


        for (int i = 0; i < amount; i++) {
            Entity mobEntity  = mob.create(player.level());
            if(mobEntity == null){
                LOGGER.info("Cannot spawn mob. mobEntity is null");
            }
            Vec3 safePosition = getSafeMobPosition(player);
            if(safePosition == null) {
                LOGGER.info("Cannot spawn mob. No safe position");
                return;
            };
            mobEntity.setCustomName(Component.literal(mobName));
            mobEntity.setPos(safePosition.x, safePosition.y, safePosition.z); // Center the zombie in the block
            player.level().addFreshEntity(mobEntity);
        }
    }


    public static EntityType<?> getMobByName(String mobId) {
        try{
            Optional<EntityType<?>> mobByString = EntityType.byString(mobId.toLowerCase());
            if(!mobByString.isPresent()){
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
        ServerLevel world = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        for (int attempts = 0; attempts < 10; attempts++) {
            int x = playerPos.getX() + rand.nextInt(20) - 10;
            int z = playerPos.getZ() + rand.nextInt(20) - 10;

            // Find the highest non-air block at the x, z coordinates (ground level)
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos spawnPos = new BlockPos(x, y, z);

            // Check if the position above is air (safe for spawning)
            if (world.isEmptyBlock(spawnPos.above())) {
                return new Vec3(x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }
}