package com.isaiahcreati.creatibotintegration.helpers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class Utils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void playSoundByName(@NotNull final Player player, String soundName) {
        Identifier resource = Identifier.parse(soundName);

        var holder = BuiltInRegistries.SOUND_EVENT.get(resource);
        if (holder.isEmpty()) {
            LOGGER.info("Sound not found in registry: " + soundName);
            return;
        }

        player.level().playSound(null, player.blockPosition(), holder.get().value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    public static boolean isSafeLocation(Level world, BlockPos pos) {
        return world.getBlockState(pos.below()).isSolid() &&
                world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
    }

    public static Holder<MobEffect> getPotionEffect(String effectName){
        Identifier resource = Identifier.parse(effectName);
        return BuiltInRegistries.MOB_EFFECT.get(resource).orElse(null);
    }

    public static Item getItemById(String itemName){
        itemName = itemName.toLowerCase();
        Identifier resource = Identifier.parse(itemName);
        return BuiltInRegistries.ITEM.getValue(resource);
    }

    public static EntityType<?> getEntityTypeByName(String entityId) {
        Identifier resource = Identifier.parse(entityId.toLowerCase());
        if (resource == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getValue(resource);
    }

}