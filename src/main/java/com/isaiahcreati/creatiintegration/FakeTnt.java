package com.isaiahcreati.creatiintegration;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FakeTnt extends PrimedTnt {


    public FakeTnt(EntityType<? extends PrimedTnt> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FakeTnt(Level pLevel, double pX, double pY, double pZ, @Nullable LivingEntity pOwner) {
        super(pLevel, pX, pY, pZ, pOwner);
        blocksBuilding = true;
    }

    @Override
    public EntityType<?> getType() {
        return ModEntities.FAKE_TNT.get();
    }
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    protected void explode() {


        // Create an explosion without damaging blocks
        this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 4.0F, Level.ExplosionInteraction.NONE);
    }
}
