package com.isaiahcreati.creatiintegration;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
        spawnMobNearPlayer(player, mobId, 1);
    }

    static public void spawnMobNearPlayer(ServerPlayer player, String mobId, int amount){
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

    public static void spawnPrimedTntOnPlayer(Level world, ServerPlayer player) {
        // Create the primed TNT entity
        PrimedTnt tnt = new PrimedTnt(world, player.getX(), player.getY(), player.getZ(), null);

        //TODO: Randomize tick amount
        tnt.setFuse(80);

        // Add the TNT to the world
        world.addFreshEntity(tnt);
    }

    public static void spawnFakePrimedTntOnPlayer(Level world, ServerPlayer player) {
        // Create the primed TNT entity
        FakeTnt tnt = new FakeTnt(world, player.getX(), player.getY(), player.getZ(), null);

        // Set the fuse time (optional, 80 ticks is the default)
        tnt.setFuse(80);

        // Add the TNT to the world
        world.addFreshEntity(tnt);

    }

    public static void smackPlayer(ServerPlayer player) {

        DamageSource source = player.level().damageSources().generic();


        // Generate random components for the force vector
        double x = rand.nextDouble() * 2.0 - 1.0; // random value between -1.0 and 1.0
        double y = rand.nextDouble() * 0.5 + 0.5; // random value between 0.5 and 1.5 to ensure upward motion
        double z = rand.nextDouble() * 2.0 - 1.0; // random value between -1.0 and 1.0

        // Normalize the vector (to give it a consistent magnitude)
        Vec3 force = new Vec3(x, y, z).normalize();

        // Apply a magnitude to the force (adjust 0.5 to change the strength of the force)
        force = force.scale(0.5);

        // Apply the force to the player
        player.setDeltaMovement(force);

        player.hurt(source, 0.5f);

    }

    public static void playSuccessSound(@NotNull final Player player)
    {
        if (player instanceof ServerPlayer)
        {
            ((ServerPlayer) player).connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvents.CREEPER_PRIMED),
                    SoundSource.NEUTRAL,
                    player.position().x,
                    player.position().y,
                    player.position().z,
                    (float) 0.5D * 2,
                    (float) 1.0,
                    player.level().random.nextLong()));
        }
        else
        {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }


}