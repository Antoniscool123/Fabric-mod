package com.aetherius.registry;

import com.aetherius.entity.AetherMantaEntity;
import com.aetherius.entity.ElementalWardenEntity;
import com.aetherius.entity.GravityAnchorEntity;
import com.aetherius.entity.NoxThalEntity;
import com.aetherius.entity.VoidSentinelEntity;
import com.aetherius.entity.VoidSpecterEntity;
import com.aetherius.entity.VoidWarpedAbominationEntity;
import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Entity registry + attribute registration.
 *
 * <p>Dimensions are intentionally generous for the boss ({@code changing} so it can
 * shrink into its "grounded" state during Phase 2) and tight for the anchor so it can
 * hide inside a block-sized hitbox.</p>
 */
public final class ModEntities {

    public static final EntityType<AetherMantaEntity> AETHER_MANTA = register("aether_manta",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AetherMantaEntity::new)
                    .dimensions(EntityDimensions.changing(2.4f, 0.9f))
                    .trackRangeChunks(8).trackedUpdateRate(3).build());

    public static final EntityType<VoidSentinelEntity> VOID_SENTINEL = register("void_sentinel",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, VoidSentinelEntity::new)
                    .dimensions(EntityDimensions.fixed(1.2f, 2.6f))
                    .trackRangeChunks(8).build());

    public static final EntityType<VoidSpecterEntity> VOID_SPECTER = register("void_specter",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, VoidSpecterEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 1.9f))
                    .trackRangeChunks(8).build());

    public static final EntityType<VoidWarpedAbominationEntity> VOID_WARPED_ABOMINATION =
            register("void_warped_abomination",
                    FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, VoidWarpedAbominationEntity::new)
                            .dimensions(EntityDimensions.fixed(0.7f, 1.9f))
                            .trackRangeChunks(8).build());

    public static final EntityType<ElementalWardenEntity> EMBERS_WARDEN = warden("embers_warden",
            ElementalWardenEntity.Variant.SOL);
    public static final EntityType<ElementalWardenEntity> TIDES_WARDEN = warden("tides_warden",
            ElementalWardenEntity.Variant.LUNA);
    public static final EntityType<ElementalWardenEntity> STONES_WARDEN = warden("stones_warden",
            ElementalWardenEntity.Variant.TERRA);
    public static final EntityType<ElementalWardenEntity> STORMS_WARDEN = warden("storms_warden",
            ElementalWardenEntity.Variant.CAELUM);

    public static final EntityType<GravityAnchorEntity> GRAVITY_ANCHOR = register("gravity_anchor",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GravityAnchorEntity::new)
                    .dimensions(EntityDimensions.fixed(1.3f, 1.3f))
                    .trackRangeChunks(6).build());

    public static final EntityType<NoxThalEntity> NOX_THAL = register("nox_thal",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, NoxThalEntity::new)
                    .dimensions(EntityDimensions.changing(7.0f, 9.0f))
                    .trackRangeChunks(16).trackedUpdateRate(2).fireImmune().build());

    private ModEntities() {
    }

    private static EntityType<ElementalWardenEntity> warden(String path, ElementalWardenEntity.Variant variant) {
        EntityType.EntityFactory<ElementalWardenEntity> factory =
                (type, world) -> new ElementalWardenEntity(type, world, variant);
        FabricEntityTypeBuilder<ElementalWardenEntity> builder =
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory);
        return register(path, builder.dimensions(EntityDimensions.fixed(1.6f, 3.4f))
                .trackRangeChunks(10).fireImmune().build());
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String path,
                                                                                EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, Aeth.id(path), type);
    }

    public static void register() {
        // Registry entries are created at class-load; attributes must be bound explicitly.
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(AETHER_MANTA, AetherMantaEntity.createMantaAttributes());
        FabricDefaultAttributeRegistry.register(VOID_SENTINEL, VoidSentinelEntity.createSentinelAttributes());
        FabricDefaultAttributeRegistry.register(VOID_SPECTER, VoidSpecterEntity.createSpecterAttributes());
        FabricDefaultAttributeRegistry.register(VOID_WARPED_ABOMINATION,
                VoidWarpedAbominationEntity.createAbominationAttributes());
        FabricDefaultAttributeRegistry.register(EMBERS_WARDEN, ElementalWardenEntity.createWardenAttributes());
        FabricDefaultAttributeRegistry.register(TIDES_WARDEN, ElementalWardenEntity.createWardenAttributes());
        FabricDefaultAttributeRegistry.register(STONES_WARDEN, ElementalWardenEntity.createWardenAttributes());
        FabricDefaultAttributeRegistry.register(STORMS_WARDEN, ElementalWardenEntity.createWardenAttributes());
        FabricDefaultAttributeRegistry.register(GRAVITY_ANCHOR, GravityAnchorEntity.createAnchorAttributes());
        FabricDefaultAttributeRegistry.register(NOX_THAL, NoxThalEntity.createNoxThalAttributes());
    }
}
