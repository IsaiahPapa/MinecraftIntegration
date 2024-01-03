package com.isaiahcreati.creatiintegration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class Utils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void playSoundByName(@NotNull final Player player, String soundName)
    {

        ResourceLocation resource = ResourceLocation.tryParse(soundName);
        if(resource == null) {
            LOGGER.info("Failed to find sound 'minecraft:" + soundName + "'");
            return;
        }

        if (player instanceof ServerPlayer)
        {
            ((ServerPlayer) player).connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(resource)),
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

    public static boolean isSafeLocation(Level world, BlockPos pos) {
        // Check if the block below the position is solid and the two blocks above are air
        return world.getBlockState(pos.below()).isSolidRender(world, pos.below()) &&
                world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
    }

    public static void givePlayerItem(ServerPlayer player, String itemName, int amount){
        itemName = itemName.toLowerCase();
        ResourceLocation resource = ResourceLocation.tryParse(itemName);
        Item item = ForgeRegistries.ITEMS.getValue(resource);
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
}
