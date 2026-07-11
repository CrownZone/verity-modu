package com.example.verity.entity;

import com.example.verity.VerityMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final EntityType<WatcherEntity> WATCHER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(VerityMod.MOD_ID, "watcher"),
            FabricEntityTypeBuilder.create(MobCategory.MISC, WatcherEntity::new)
                    .dimensions(EntityDimensions.scalable(0.6F, 1.95F))
                    .build()
    );

    /** Call once from the mod's onInitialize(). */
    public static void register() {
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createAttributes());
    }
}
