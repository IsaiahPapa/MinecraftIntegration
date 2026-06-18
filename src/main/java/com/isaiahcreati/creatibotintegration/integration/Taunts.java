package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.helpers.Utils;
import com.isaiahcreati.creatibotintegration.helpers.Mobs;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Relative;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

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
        taunts.put("parkour", new Taunt("parkour", "Parkour Course"));
        taunts.put("tntrun", new Taunt("tntrun", "TNT Run"));
        taunts.put("dropper", new Taunt("dropper", "Dropper"));
        taunts.put("drop_all", new Taunt("drop_all", "Drop Everything!"));
        taunts.put("half_heart", new Taunt("half_heart", "Half-Hearted"));
        taunts.put("hungry", new Taunt("hungry", "Starving!"));
        taunts.put("sky", new Taunt("sky", "To The Moon!"));
        taunts.put("fake_tp", new Taunt("fake_tp", "Fake Teleport"));
        taunts.put("jumpscare", new Taunt("jumpscare", "Jumpscare!"));
        taunts.put("chicken_rain", new Taunt("chicken_rain", "Chicken Rain"));
        taunts.put("meteor_rain", new Taunt("meteor_rain", "Meteor Rain"));
        taunts.put("raid", new Taunt("raid", "Prepare to Fight!"));
        taunts.put("fire_trail", new Taunt("fire_trail", "Fire Trail"));
        taunts.put("downgrade_gear", new Taunt("downgrade_gear", "Downgrade Gear"));
        taunts.put("anvil", new Taunt("anvil", "Anvil Drop"));
        taunts.put("bury", new Taunt("bury", "Buried Alive"));
        taunts.put("curse_gear", new Taunt("curse_gear", "Cursed!"));
        taunts.put("stack_one", new Taunt("stack_one", "Stack of One"));
        taunts.put("mob_army", new Taunt("mob_army", "Mob Army"));
        taunts.put("anvil_rain", new Taunt("anvil_rain", "Anvil Rain"));
        taunts.put("blind_noise", new Taunt("blind_noise", "Blind Panic"));
        taunts.put("rename_chat", new Taunt("rename_chat", "Rename the Streamer"));
        taunts.put("hot_potato", new Taunt("hot_potato", "Hot Potato"));
        taunts.put("lucky_block", new Taunt("lucky_block", "Lucky Block"));
        taunts.put("drunk", new Taunt("drunk", "Drunk Streamer"));
        taunts.put("vignette_heartbeat", new Taunt("vignette_heartbeat", "Heartbeat"));
        taunts.put("pixelate", new Taunt("pixelate", "PS1 Aesthetic"));
        taunts.put("mirror", new Taunt("mirror", "Mirror World"));
        taunts.put("fisheye", new Taunt("fisheye", "Fisheye"));
        taunts.put("fov_quake", new Taunt("fov_quake", "Quake FOV"));
        taunts.put("fov_zoom", new Taunt("fov_zoom", "Ultra Zoom"));
        taunts.put("upside_down", new Taunt("upside_down", "Upside Down"));
        taunts.put("rolling_camera", new Taunt("rolling_camera", "Rolling Camera"));
        taunts.put("camera_tilt", new Taunt("camera_tilt", "Tilted Camera"));
        taunts.put("pumpkin_view", new Taunt("pumpkin_view", "Pumpkin View"));
        taunts.put("dvd", new Taunt("dvd", "DVD Screensaver"));
        taunts.put("inverted_controls", new Taunt("inverted_controls", "Inverted Controls"));
        taunts.put("mouse_drifting", new Taunt("mouse_drifting", "Mouse Drifting"));
        taunts.put("blur", new Taunt("blur", "Blur"));
        taunts.put("inverted_colors", new Taunt("inverted_colors", "Inverted Colors"));
        taunts.put("black_and_white", new Taunt("black_and_white", "1950s"));
        taunts.put("lsd", new Taunt("lsd", "Lucy In The Sky"));
        taunts.put("crt", new Taunt("crt", "Monitor Downgrade"));
    }

    public Taunt getTauntById(String id) {
        return taunts.get(id);
    }

    public Map<String, Taunt> getAllTaunts() {
        return taunts;
    }

    public static void ShuffleInventory(ServerPlayer player){
        Inventory inventory = player.getInventory();
        Collections.shuffle(inventory.getNonEquipmentItems());
        player.inventoryMenu.broadcastChanges();
    }

    public static void applyPotionEffect(ServerPlayer player, String effectName, int durationSeconds, int amplifier){
        Holder<MobEffect> effect = Utils.getPotionEffect(effectName);
        if(effect == null){
            LOGGER.error("Failed to find effect: " + effectName);
            return;
        }
        LOGGER.info("Applying effect for {} seconds!", durationSeconds);
        MobEffectInstance instance = new MobEffectInstance(effect, durationSeconds * 20, amplifier);
        player.addEffect(instance);

        Utils.playSoundByName(player, "minecraft:entity.splash_potion.break");
    }

    public static void strikeDownPlayer(ServerPlayer player){
        BlockPos playerPosition =  player.getOnPos();
        Entity lightning = EntityType.LIGHTNING_BOLT.create(player.level(), EntitySpawnReason.EVENT);
        lightning.setPos(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ());
        player.level().addFreshEntity(lightning);
    }
    public static void breakBlockUnderPlayer(ServerPlayer player){
        BlockPos underPlayerPosition = player.getOnPos();

        BlockState blockState = player.level().getBlockState(underPlayerPosition);

        player.level().playSound(null, underPlayerPosition, blockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

        player.level().setBlock(underPlayerPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

    }
    public static void teleportPlayerToRandomLocation(ServerPlayer player) {
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
                player.teleportTo((ServerLevel) player.level(), newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
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
        PrimedTnt tnt = new PrimedTnt(world, player.getX(), player.getY(), player.getZ(), player);
        tnt.setFuse(80);
        world.addFreshEntity(tnt);
        world.playSound(null, player.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static void smackPlayer(ServerPlayer player) {
        DamageSource source = player.level().damageSources().generic();

        double x = rand.nextDouble() * 2.0 - 1.0;
        double y = rand.nextDouble() * 0.5 + 0.5;
        double z = rand.nextDouble() * 2.0 - 1.0;

        Vec3 force = new Vec3(x, y, z).normalize();
        force = force.scale(0.5);

        player.setDeltaMovement(force);
        player.hurtMarked = true;
        player.hurt(source, 0.5f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
    public static void givePlayerItem(ServerPlayer player, String itemName, int amount){
        Item item = Utils.getItemById(itemName);
        if (item == null) {
            LOGGER.info("Failed to find item 'minecraft:" + itemName + "'");
            return;
        }
        ItemStack itemStack = new ItemStack(item, amount);
        boolean wasAdded = player.getInventory().add(itemStack);

        if (!wasAdded) {
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack);
            player.level().addFreshEntity(itemEntity);
        }
    }

    public static void dropAllInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                player.drop(stack.copyAndClear(), false);
            }
        }
    }

    public static void setHalfHeart(ServerPlayer player) {
        player.setHealth(1.0f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.5F);
    }

    public static void drainHunger(ServerPlayer player) {
        FoodData food = player.getFoodData();
        food.setFoodLevel(0);
        food.setSaturation(0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static void launchToSky(ServerPlayer player) {
        player.setDeltaMovement(player.getDeltaMovement().add(0, 5.0, 0));
        player.hurtMarked = true;
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void fakeTeleport(ServerPlayer player) {
        BlockPos originalPos = player.blockPosition();
        float originalYRot = player.getYRot();
        float originalXRot = player.getXRot();

        teleportPlayerToRandomLocation(player);

        player.level().getServer().executeIfPossible(new net.minecraft.server.TickTask(player.level().getServer().getTickCount() + 100, () -> {
            player.teleportTo((ServerLevel) player.level(), originalPos.getX() + 0.5, originalPos.getY(), originalPos.getZ() + 0.5, Set.<Relative>of(), originalYRot, originalXRot, false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }));
    }

    public static void spawnMob(ServerPlayer player, String mobId) {
        EntityType<?> type = Utils.getEntityTypeByName(mobId);
        if (type == null) {
            LOGGER.error("Unknown mob type: {}", mobId);
            return;
        }

        double x = player.getX() + (rand.nextDouble() - 0.5) * 6;
        double z = player.getZ() + (rand.nextDouble() - 0.5) * 6;
        double y = player.getY();

        Entity entity = type.create(player.level(), EntitySpawnReason.EVENT);
        if (entity == null) return;

        entity.setPos(x, y, z);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn((ServerLevel) player.level(), player.level().getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.EVENT, null);
        }
        player.level().addFreshEntity(entity);
    }

    public static void chickenRain(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 20; i++) {
            double x = player.getX() + (rand.nextDouble() - 0.5) * 20;
            double z = player.getZ() + (rand.nextDouble() - 0.5) * 20;
            double y = player.getY() + 20 + rand.nextDouble() * 10;

            Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.EVENT);
            if (chicken == null) continue;
            chicken.setPos(x, y, z);
            chicken.setDeltaMovement((rand.nextDouble() - 0.5) * 0.5, -0.5, (rand.nextDouble() - 0.5) * 0.5);
            level.addFreshEntity(chicken);
        }
    }

    public static void meteorRain(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 8; i++) {
            double x = player.getX() + (rand.nextDouble() - 0.5) * 16;
            double z = player.getZ() + (rand.nextDouble() - 0.5) * 16;
            double y = player.getY() + 30 + rand.nextDouble() * 10;

            PrimedTnt tnt = new PrimedTnt(level, x, y, z, player);
            tnt.setFuse(40 + rand.nextInt(40));
            level.addFreshEntity(tnt);
        }
    }

    public static void triggerRaid(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Raid raid = level.getRaidAt(player.blockPosition());
        if (raid == null || raid.isStopped()) {
            level.getRaids().createOrExtendRaid(player, player.blockPosition());
        }
    }

    private static final Map<java.util.UUID, Integer> fireTrailPlayers = new HashMap<>();

    public static void startFireTrail(ServerPlayer player) {
        fireTrailPlayers.put(player.getUUID(), 200);
    }

    public static void tickFireTrails() {
        fireTrailPlayers.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    public static void updateFireTrail(ServerPlayer player) {
        Integer ticks = fireTrailPlayers.get(player.getUUID());
        if (ticks == null) return;

        BlockPos pos = player.getOnPos();
        if (player.level().isEmptyBlock(pos.above()) && !player.level().getFluidState(pos).is(Fluids.LAVA)) {
            player.level().setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void downgradeGear(ServerPlayer player) {
        Map<Item, Item> downgradeMap = new HashMap<>();
        downgradeMap.put(Items.NETHERITE_BOOTS, Items.DIAMOND_BOOTS);
        downgradeMap.put(Items.NETHERITE_LEGGINGS, Items.DIAMOND_LEGGINGS);
        downgradeMap.put(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE);
        downgradeMap.put(Items.NETHERITE_HELMET, Items.DIAMOND_HELMET);
        downgradeMap.put(Items.NETHERITE_SWORD, Items.DIAMOND_SWORD);
        downgradeMap.put(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        downgradeMap.put(Items.NETHERITE_AXE, Items.DIAMOND_AXE);
        downgradeMap.put(Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL);
        downgradeMap.put(Items.NETHERITE_HOE, Items.DIAMOND_HOE);
        downgradeMap.put(Items.DIAMOND_BOOTS, Items.IRON_BOOTS);
        downgradeMap.put(Items.DIAMOND_LEGGINGS, Items.IRON_LEGGINGS);
        downgradeMap.put(Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE);
        downgradeMap.put(Items.DIAMOND_HELMET, Items.IRON_HELMET);
        downgradeMap.put(Items.DIAMOND_SWORD, Items.IRON_SWORD);
        downgradeMap.put(Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE);
        downgradeMap.put(Items.DIAMOND_AXE, Items.IRON_AXE);
        downgradeMap.put(Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL);
        downgradeMap.put(Items.DIAMOND_HOE, Items.IRON_HOE);
        downgradeMap.put(Items.IRON_BOOTS, Items.LEATHER_BOOTS);
        downgradeMap.put(Items.IRON_LEGGINGS, Items.LEATHER_LEGGINGS);
        downgradeMap.put(Items.IRON_CHESTPLATE, Items.LEATHER_CHESTPLATE);
        downgradeMap.put(Items.IRON_HELMET, Items.LEATHER_HELMET);
        downgradeMap.put(Items.IRON_SWORD, Items.WOODEN_SWORD);
        downgradeMap.put(Items.IRON_PICKAXE, Items.WOODEN_PICKAXE);
        downgradeMap.put(Items.IRON_AXE, Items.WOODEN_AXE);
        downgradeMap.put(Items.IRON_SHOVEL, Items.WOODEN_SHOVEL);
        downgradeMap.put(Items.IRON_HOE, Items.WOODEN_HOE);

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            Item downgrade = downgradeMap.get(stack.getItem());
            if (downgrade != null) {
                ItemStack newStack = new ItemStack(downgrade, stack.getCount());
                inv.setItem(i, newStack);
                player.inventoryMenu.broadcastChanges();
                player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
        }
    }
    public static void dropAnvilOnPlayer(ServerPlayer player) {
        FallingBlockEntity anvil = FallingBlockEntity.fall(player.level(), new BlockPos(player.getBlockX(), player.getBlockY() + 8, player.getBlockZ()), Blocks.ANVIL.defaultBlockState());
        anvil.setHurtsEntities(2.0F, 40);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5F, 2.0F);
    }

    public static void buryPlayer(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos feetPos = player.blockPosition();
        boolean placedAny = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = feetPos.offset(dx, dy, dz);
                    if (level.isEmptyBlock(pos)) {
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                        placedAny = true;
                    }
                }
            }
        }
        if (placedAny) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.GRAVEL_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    public static void curseGear(ServerPlayer player) {
        Inventory inv = player.getInventory();
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) return;
        int slot = slots.get(rand.nextInt(slots.size()));
        ItemStack stack = inv.getItem(slot);
        Holder<net.minecraft.world.item.enchantment.Enchantment> curse = rand.nextBoolean()
                ? player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE)
                : player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE);
        stack.enchant(curse, 1);
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static void reduceStacksToOne(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getCount() > 1) {
                stack.setCount(1);
            }
        }
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static void randomizeMovementTemporarily(ServerPlayer player){
    }

    public static void spawnMobArmy(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        String[] hostileTypes = {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider",
                "minecraft:creeper", "minecraft:witch", "minecraft:enderman"};
        String chosen = hostileTypes[rand.nextInt(hostileTypes.length)];
        EntityType<?> type = Utils.getEntityTypeByName(chosen);
        if (type == null) return;

        int count = 8 + rand.nextInt(8);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double radius = 3.5 + rand.nextDouble() * 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY();

            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (entity == null) continue;
            entity.setPos(x, y, z);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.EVENT, null);
                if (mob instanceof net.minecraft.world.entity.monster.Monster monster) {
                    monster.setTarget(player);
                }
            }
            level.addFreshEntity(entity);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.6F, 0.8F);
    }

    public static void anvilRain(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int count = 6;
        for (int i = 0; i < count; i++) {
            int delay = i * 12;
            int dropIndex = i;
            level.getServer().executeIfPossible(new net.minecraft.server.TickTask(level.getServer().getTickCount() + delay, () -> {
                double ox = (rand.nextDouble() - 0.5) * 6;
                double oz = (rand.nextDouble() - 0.5) * 6;
                BlockPos pos = new BlockPos((int)(player.getBlockX() + ox), player.getBlockY() + 8, (int)(player.getBlockZ() + oz));
                FallingBlockEntity anvil = FallingBlockEntity.fall(level, pos, Blocks.ANVIL.defaultBlockState());
                anvil.setHurtsEntities(2.0F, 40);
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.4F, 1.5F);
            }));
        }
    }

    public static void blindNoise(ServerPlayer player) {
        applyPotionEffect(player, "minecraft:blindness", 3, 1);
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 6; i++) {
            int delay = 4 + rand.nextInt(7);
            level.getServer().executeIfPossible(new net.minecraft.server.TickTask(level.getServer().getTickCount() + delay, () -> {
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
            }));
        }
    }

    public static final record RenameState(String name, long expiryTick) {}
    private static final Map<java.util.UUID, RenameState> activeRenames = new HashMap<>();
    private static final String[] SILLY_NAMES = {
            "Suspicious Steve", "A Very Lost Pig", "Definitely Not The Streamer", "Greg",
            "The Imposter", "A Suspicious Llama", "Creeper In A Trenchcoat", "Villager #42"
    };

    public static void renameChat(ServerPlayer player) {
        String name = SILLY_NAMES[rand.nextInt(SILLY_NAMES.length)];
        long expiryTick = player.level().getServer().getTickCount() + (60 * 20L);
        activeRenames.put(player.getUUID(), new RenameState(name, expiryTick));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static RenameState getActiveRename(java.util.UUID uuid, long currentTick) {
        RenameState state = activeRenames.get(uuid);
        if (state == null) return null;
        if (currentTick >= state.expiryTick()) {
            activeRenames.remove(uuid);
            return null;
        }
        return state;
    }

    public static void tickRenames() {
        if (activeRenames.isEmpty()) return;
        long currentTick = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null
                ? net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getTickCount() : -1;
        if (currentTick < 0) return;
        activeRenames.entrySet().removeIf(e -> currentTick >= e.getValue().expiryTick());
    }

    private static final Map<java.util.UUID, Long> hotPotatoDeadlines = new HashMap<>();
    private static final Map<java.util.UUID, Integer> hotPotatoLastShownSecond = new HashMap<>();

    public static void hotPotato(ServerPlayer player) {
        ItemStack potato = new ItemStack(Items.BAKED_POTATO);
        potato.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("HOT POTATO")
                        .withStyle(net.minecraft.ChatFormatting.RED));
        potato.set(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        net.minecraft.network.chat.Component.literal("Drop me or you'll explode!").withStyle(net.minecraft.ChatFormatting.YELLOW),
                        net.minecraft.network.chat.Component.literal("Or eat me to defuse it...").withStyle(net.minecraft.ChatFormatting.GRAY)
                )));
        potato.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);

        var data = potato.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.world.item.component.CustomData customData;
        if (data != null) {
            customData = data;
        } else {
            customData = net.minecraft.world.item.component.CustomData.EMPTY;
        }
        final net.minecraft.world.item.component.CustomData finalData = customData.update(tag -> {
            tag.putBoolean("creatibotintegration.hot_potato", true);
        });
        potato.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, finalData);

        if (!player.getInventory().add(potato)) {
            player.drop(potato, false);
        }
        long deadline = player.level().getServer().getTickCount() + 100;
        hotPotatoDeadlines.put(player.getUUID(), deadline);
        hotPotatoLastShownSecond.remove(player.getUUID());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    public static void tickHotPotatoes() {
        if (hotPotatoDeadlines.isEmpty()) return;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long currentTick = server.getTickCount();
        var it = hotPotatoDeadlines.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            long deadline = entry.getValue();
            boolean stillHasPotato = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || stack.getItem() != Items.BAKED_POTATO) continue;
                var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (cd == null) continue;
                if (cd.contains("creatibotintegration.hot_potato")) {
                    stillHasPotato = true;
                    break;
                }
            }
            if (!stillHasPotato) {
                it.remove();
                hotPotatoLastShownSecond.remove(entry.getKey());
                continue;
            }
            long secondsLeft = (deadline - currentTick) / 20L;
            int lastShown = hotPotatoLastShownSecond.getOrDefault(entry.getKey(), -1);
            if (secondsLeft != lastShown && secondsLeft >= 0) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("HOT POTATO: " + secondsLeft + "s — DROP IT!")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        true);
                hotPotatoLastShownSecond.put(entry.getKey(), (int) secondsLeft);
            }
            if (currentTick >= deadline) {
                PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), player);
                tnt.setFuse(0);
                player.level().addFreshEntity(tnt);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
                it.remove();
                hotPotatoLastShownSecond.remove(entry.getKey());
            }
        }
    }

    public static void clearHotPotatoOnDeath(ServerPlayer player) {
        var deadline = hotPotatoDeadlines.remove(player.getUUID());
        hotPotatoLastShownSecond.remove(player.getUUID());
        if (deadline == null) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != Items.BAKED_POTATO) continue;
            var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (cd == null) continue;
            if (cd.contains("creatibotintegration.hot_potato")) {
                player.getInventory().removeItem(i, stack.getCount());
            }
        }
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static final java.util.Set<BlockPos> luckyBlocks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private static final Map<BlockPos, Long> luckyBlockPlacedTick = new java.util.concurrent.ConcurrentHashMap<>();

    public static void luckyBlock(ServerPlayer player) {
        ItemStack block = new ItemStack(Items.SPONGE);
        block.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Lucky Block")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
        block.set(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        net.minecraft.network.chat.Component.literal("Place and break for a surprise!").withStyle(net.minecraft.ChatFormatting.YELLOW)
                )));
        block.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);

        var data = block.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.world.item.component.CustomData customData = (data != null) ? data : net.minecraft.world.item.component.CustomData.EMPTY;
        final net.minecraft.world.item.component.CustomData finalData = customData.update(tag -> {
            tag.putBoolean("creatibotintegration.lucky_block", true);
        });
        block.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, finalData);

        if (!player.getInventory().add(block)) {
            player.drop(block, false);
        }
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static boolean isLuckyBlockItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.SPONGE) return false;
        var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return cd != null && cd.contains("creatibotintegration.lucky_block");
    }

    public static void registerPlacedLuckyBlock(ServerLevel level, BlockPos pos) {
        luckyBlocks.add(pos);
        luckyBlockPlacedTick.put(pos, (long) level.getServer().getTickCount());
        level.playSound(null, pos, Blocks.SPONGE.defaultBlockState().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static void tickLuckyBlocks() {
        if (luckyBlockPlacedTick.isEmpty()) return;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long currentTick = server.getTickCount();
        long expiryTicks = 10 * 60 * 20L;
        var it = luckyBlockPlacedTick.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (currentTick - entry.getValue() > expiryTicks) {
                luckyBlocks.remove(entry.getKey());
                it.remove();
            }
        }
    }

    public static boolean isLuckyBlock(BlockPos pos) {
        return luckyBlocks.contains(pos);
    }

    public static void onLuckyBlockBroken(ServerPlayer player, BlockPos pos) {
        luckyBlocks.remove(pos);
        luckyBlockPlacedTick.remove(pos);
        ServerLevel level = (ServerLevel) player.level();
        int roll = rand.nextInt(100);
        if (roll < 25) {
            ItemStack loot = new ItemStack(Items.DIAMOND, 3);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, loot);
            level.addFreshEntity(drop);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else if (roll < 45) {
            String[] hostile = {"minecraft:zombie", "minecraft:skeleton", "minecraft:creeper"};
            Mobs.spawnMobNearPlayer(player, hostile[rand.nextInt(hostile.length)], 1, "");
            level.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 1.0F);
        } else if (roll < 65) {
            String[] good = {"minecraft:regeneration", "minecraft:strength"};
            applyPotionEffect(player, good[rand.nextInt(good.length)], 15, 1);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (roll < 85) {
            String[] bad = {"minecraft:poison", "minecraft:weakness"};
            applyPotionEffect(player, bad[rand.nextInt(bad.length)], 10, 1);
            level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.0F);
        } else if (roll < 95) {
            PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, player);
            tnt.setFuse(40);
            level.addFreshEntity(tnt);
            level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        } else {
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }
}