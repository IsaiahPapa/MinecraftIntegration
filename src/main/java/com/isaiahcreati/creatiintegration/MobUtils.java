package com.isaiahcreati.creatiintegration;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Random;

public final class MobUtils {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Random rand = new Random();

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId) {
        spawnMobNearPlayer(player, mobId, 1, "");
    }

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId, int amount, String mobName){
        // Find the Entity given the mobId
        Optional<EntityType<?>> mobByString = EntityType.byString(mobId.toLowerCase());
        if(!mobByString.isPresent()){
            LOGGER.info("Mob is not present");
            return;
        }

        for (int i = 0; i < amount; i++) {
            Entity mob  = mobByString.get().create(player.level());
            Vec3 safePosition = getSafeMobPosition(player);
            if(safePosition == null) {
                LOGGER.info("Cannot spawn mob, no safe position");
                return;
            };
            mob.setCustomName(Component.literal(mobName));
            mob.setPos(safePosition.x, safePosition.y, safePosition.z); // Center the zombie in the block
            player.level().addFreshEntity(mob);
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