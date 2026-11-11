package com.aetherius.registry;

import com.aetherius.block.entity.AetherAltarBlockEntity;
import com.aetherius.block.entity.CosmicForgeBlockEntity;
import com.aetherius.block.entity.SkyRiftAnchorBlockEntity;
import com.aetherius.block.entity.TelescopeBlockEntity;
import com.aetherius.util.Aeth;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Block entity types. Registered after blocks; block entities are tickable where needed. */
public final class ModBlockEntities {

    public static final BlockEntityType<CosmicForgeBlockEntity> COSMIC_FORGE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Aeth.id("cosmic_forge"),
            BlockEntityType.Builder.create(CosmicForgeBlockEntity::new, ModBlocks.AETHER_FORGE).build(null));

    public static final BlockEntityType<AetherAltarBlockEntity> AETHER_ALTAR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Aeth.id("aether_altar"),
            BlockEntityType.Builder.create(AetherAltarBlockEntity::new, ModBlocks.AETHER_ALTAR_CORE).build(null));

    public static final BlockEntityType<TelescopeBlockEntity> TELESCOPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Aeth.id("telescope"),
            BlockEntityType.Builder.create(TelescopeBlockEntity::new, ModBlocks.TELESCOPE).build(null));

    public static final BlockEntityType<SkyRiftAnchorBlockEntity> SKY_RIFT_ANCHOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Aeth.id("sky_rift_anchor"),
            BlockEntityType.Builder.create(SkyRiftAnchorBlockEntity::new, ModBlocks.SKY_RIFT_ANCHOR).build(null));

    private ModBlockEntities() {
    }

    public static void register() {
    }
}
