package com.aetherius.registry;

import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Custom particles. Each needs a sprite in {@code textures/particle/} and a JSON factory entry. */
public final class ModParticles {

    public static final DefaultParticleType ASTRAL_SPARK = register("astral_spark", false);
    public static final DefaultParticleType VOID_MOTE = register("void_mote", false);
    public static final DefaultParticleType SOL_EMBER = register("sol_ember", false);
    public static final DefaultParticleType STAR_DUST = register("star_dust", false);
    public static final DefaultParticleType BLIGHT_SPORE = register("blight_spore", true);

    private ModParticles() {
    }

    private static DefaultParticleType register(String path, boolean alwaysShowEntire) {
        return Registry.register(Registries.PARTICLE_TYPE, Aeth.id(path),
                FabricParticleTypes.simple(alwaysShowEntire));
    }

    public static void register() {
    }
}
