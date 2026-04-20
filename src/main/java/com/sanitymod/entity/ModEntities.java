package com.sanitymod.entity;

import com.sanitymod.SanityMod;
import com.sanitymod.entity.custom.MimicEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<MimicEntity> MIMIC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(SanityMod.MOD_ID, "mimic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MimicEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build()
    );

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());
    }
}
