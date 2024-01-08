package com.isaiahcreati.creatiintegration;

import com.isaiahcreati.creatiintegration.helpers.Utils;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Taunts {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Random rand = new Random();

    private Map<String, Taunt> taunts = new HashMap<>();

    public Taunts() {
        taunts.put("tnt", new Taunt("tnt", "Spawn Primed TNT"));
        taunts.put("shuffle", new Taunt("shuffle", "Shuffle Inventory"));
        taunts.put("punch", new Taunt("punch", "Smack with Fish"));
        taunts.put("noise", new Taunt("noise", "Random Noise"));
        taunts.put("strike", new Taunt("strike", "Lightning Strike"));
        taunts.put("break", new Taunt("break", "Break block under me"));
        taunts.put("wild", new Taunt("wild", "Teleport into the wild"));
        taunts.put("drop", new Taunt("drop", "Drop item in hand"));
        taunts.put("cobweb", new Taunt("cobweb", "Cobwebbed!"));
    }

    public Taunt getTauntById(String id) {
        return taunts.get(id);
    }

    public static void ShuffleInventory(ServerPlayer player){
        Inventory inventory = player.getInventory();
        Collections.shuffle(inventory.items);
        player.inventoryMenu.broadcastChanges();
    }

    public static void applyPotionEffect(ServerPlayer player, String effectName, int durationSeconds, int amplifier){
        MobEffect effect = Utils.getPotionEffect(effectName);
        if(effect == null){
            LOGGER.error("Failed to find effect: " + effectName);
            return;
        }
        LOGGER.info("Applying effect '" + effect.getDisplayName().getString() + "' for " + durationSeconds + " seconds!");
        MobEffectInstance instance = new MobEffectInstance(effect, durationSeconds * 20, amplifier);
        player.addEffect(instance);

        //Play splash noise
        Utils.playSoundByName(player, "minecraft:entity.splash_potion.break");
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
        player.level().setBlock(underPlayerPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

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

            if (Utils.isSafeLocation(world, newPos)) {
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

    public static void webBlockPlayer(ServerPlayer player){
        player.getOnPos();
        player.level().setBlock(player.getOnPos().above(), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static void spawnPrimedTntOnPlayer(Level world, ServerPlayer player) {
        // Create the primed TNT entity
        PrimedTnt tnt = new PrimedTnt(world, player.getX(), player.getY(), player.getZ(), null);

        int MIN = 2;
        int MAX = 6;
        int fuseTime = rand.nextInt(MAX - MIN) + MIN;

        //TODO: Randomize tick amount
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
    public static void givePlayerItem(ServerPlayer player, String itemName, int amount){
        Item item = Utils.getItemById(itemName);
        if (item == null) {
            LOGGER.info("Failed to find item 'minecraft:" + itemName + "'");
            return;
        }
        ItemStack itemStack = new ItemStack(item, amount); // You can change the quantity if needed
        boolean wasAdded = player.getInventory().add(itemStack);

        if (!wasAdded) {
            // If the player's inventory is full, drop the item in the world
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack);
            player.level().addFreshEntity(itemEntity);
        }
    }
    //Could be a cool idea
    public static void randomizeMovementTemporarily(ServerPlayer player){
        //Swap player movement temporarily.
        // Ex: Forward is back, or left is up.
    }



}
