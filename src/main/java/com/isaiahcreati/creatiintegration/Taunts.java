package com.isaiahcreati.creatiintegration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import java.util.Collections;
import java.util.Random;

public class Taunts {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Random rand = new Random();
    public static void ShuffleInventory(ServerPlayer player){
        Inventory inventory = player.getInventory();
        Collections.shuffle(inventory.items);
        player.inventoryMenu.broadcastChanges();
    }

    public static void applyPotionEffect(ServerPlayer player, String effectName){
        ResourceLocation resource= new ResourceLocation(effectName);
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(resource);
        if(effect == null){
            LOGGER.error("Failed to find effect: " + effectName);
            return;
        }
        MobEffectInstance instance = new MobEffectInstance(effect, 15 * 20, 0);
        player.addEffect(instance);
    }
    public static void strikeDownPlayer(ServerPlayer player){
        BlockPos playerPosition =  player.getOnPos();
        Entity lightning = EntityType.LIGHTNING_BOLT.create(player.level());
        lightning.setPos(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ());
        player.level().addFreshEntity(lightning);
    }
    public static void breakBlockUnderPlayer(ServerPlayer player){
        BlockPos underPlayerPosition = player.getOnPos();

        BlockState blockState = player.level().getBlockState(underPlayerPosition);

        // Play the block break sound
        player.level().playSound(null, underPlayerPosition, blockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

        // Replace the block with air
        player.level().setBlock(underPlayerPosition, Blocks.AIR.defaultBlockState(), 3);

    }
    public static void teleportPlayerToRandomLocation(ServerPlayer player) {
        // Current player position
        BlockPos currentPos = player.blockPosition();
        Level world = player.level();

        for (int attempts = 0; attempts < 30; attempts++) {
            int x = currentPos.getX() + rand.nextInt(3000) - 1500;
            int z = currentPos.getZ() + rand.nextInt(3000) - 1500;

            BlockPos newPos = new BlockPos(x, world.getHeight(), z);
            while (newPos.getY() > 0 && world.isEmptyBlock(newPos.below())) {
                newPos = newPos.below();
            }

            if (isSafeLocation(world, newPos)) {
                // Teleport the player
                player.teleportTo(player.serverLevel(), newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                return;
            }
        }
    }

    public static void dropHand(ServerPlayer player){
        ItemStack itemstack = player.getMainHandItem();
        if(itemstack.isEmpty()) return;
        player.drop(itemstack.copyAndClear(), false);
    }

    private static boolean isSafeLocation(Level world, BlockPos pos) {
        // Check if the block below the position is solid and the two blocks above are air
        return world.getBlockState(pos.below()).isSolidRender(world, pos.below()) &&
                world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
    }

    //Could be a cool idea
    public static void randomizeMovementTemporarily(ServerPlayer player){
        //Swap player movement temporarily.
        // Ex: Forward is back, or left is up.
    }



}
