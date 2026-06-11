package com.isaiahcreati.creatibotintegration.helpers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class Utils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void playSoundByName(@NotNull final Player player, String soundName) {
        ResourceLocation resource = ResourceLocation.tryParse(soundName);
        if (resource == null) {
            LOGGER.info("Failed to find sound '" + soundName + "'");
            return;
        }

        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(resource);
        if (soundEvent == null) {
            LOGGER.info("Sound not found in registry: " + soundName);
            return;
        }

        player.level().playSound(null, player.blockPosition(), soundEvent, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    public static boolean isSafeLocation(Level world, BlockPos pos) {
        return world.getBlockState(pos.below()).isSolidRender(world, pos.below()) &&
                world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
    }

    public static MobEffect getPotionEffect(String effectName){
        ResourceLocation resource = ResourceLocation.tryParse(effectName);
        return ForgeRegistries.MOB_EFFECTS.getValue(resource);
    }

    public static Item getItemById(String itemName){
        itemName = itemName.toLowerCase();
        ResourceLocation resource = ResourceLocation.tryParse(itemName);
        return ForgeRegistries.ITEMS.getValue(resource);
    }

    public static EntityType<?> getEntityTypeByName(String entityId) {
        ResourceLocation resource = ResourceLocation.tryParse(entityId.toLowerCase());
        if (resource == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(resource);
    }

}