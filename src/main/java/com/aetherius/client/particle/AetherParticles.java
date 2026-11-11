package com.aetherius.client.particle;

import com.aetherius.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/**
 * Binds the server-side particle types to client factories.
 *
 * <p>Without this registration the client receives particle packets for types it has no factory
 * for and silently drops them - a very common "my custom particles don't show" bug. Profiles:</p>
 * <ul>
 *   <li><b>astral_spark</b> - white-blue, floating upward, tiny.</li>
 *   <li><b>void_mote</b> - violet, drifting with a slight fall.</li>
 *   <li><b>sol_ember</b> - orange, rising and shrinking.</li>
 *   <li><b>star_dust</b> - warm white, very small and long-lived.</li>
 *   <li><b>blight_spore</b> - sickly purple, slow and dense.</li>
 * </ul>
 */
public final class AetherParticles {

    private AetherParticles() {
    }

    public static void register() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();

        registry.register(ModParticles.ASTRAL_SPARK,
                sprites -> new AetherParticle.Factory(sprites, 0.72f, 0.92f, 1.0f, 0.10f, 16, true));

        registry.register(ModParticles.VOID_MOTE,
                sprites -> new AetherParticle.Factory(sprites, 0.60f, 0.30f, 1.0f, 0.13f, 24, false));

        registry.register(ModParticles.SOL_EMBER,
                sprites -> new AetherParticle.Factory(sprites, 1.0f, 0.58f, 0.24f, 0.12f, 18, true));

        registry.register(ModParticles.STAR_DUST,
                sprites -> new AetherParticle.Factory(sprites, 1.0f, 0.98f, 0.86f, 0.07f, 30, true));

        registry.register(ModParticles.BLIGHT_SPORE,
                sprites -> new AetherParticle.Factory(sprites, 0.45f, 0.24f, 0.62f, 0.16f, 40, false));
    }
}
