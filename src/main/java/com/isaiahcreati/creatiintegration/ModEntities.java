package com.isaiahcreati.creatiintegration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.isaiahcreati.creatiintegration.CreatiIntegration.MOD_ID;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<FakeTnt>> FAKE_TNT = register("fake_tnt",
            EntityType.Builder.<FakeTnt>of(FakeTnt::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .setUpdateInterval(10));

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder)
    {
        return REGISTER.register(name, () -> builder.build(name));
    }
}
