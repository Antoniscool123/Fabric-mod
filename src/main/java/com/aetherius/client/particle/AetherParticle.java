package com.aetherius.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * One small, reusable billboard particle for every Aetherius effect.
 *
 * <p>Rather than five near-identical particle classes, each effect registers a factory with a
 * different colour/gravity/scale profile. Tinting is done in the particle itself so the same
 * sprite atlas entry can serve fire, void and starlight variants.</p>
 */
public class AetherParticle extends SpriteBillboardParticle {

    private final float red;
    private final float green;
    private final float blue;
    private final boolean rising;

    public AetherParticle(ClientWorld world, double x, double y, double z,
                          double velocityX, double velocityY, double velocityZ,
                          SpriteProvider spriteProvider, float red, float green, float blue,
                          float scale, int maxAge, boolean rising) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(spriteProvider.getSprite(this.random));
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.rising = rising;
        this.scale = scale;
        this.maxAge = maxAge;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.collidesWithWorld = false;
        this.gravityStrength = rising ? -0.02f : 0.04f;
        this.setColor(red, green, blue);
        this.setAlpha(0.95f);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Fade out over the back half of the lifetime and shrink slightly.
        float life = (float) this.age / (float) this.maxAge;
        if (life > 0.5f) {
            this.setAlpha(Math.max(0f, 1f - (life - 0.5f) * 2f));
            this.scale *= 0.985f;
        }
        this.setColor(red, green, blue);
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * (1f - 0.35f * ((float) this.age / (float) this.maxAge));
    }

    /**
     * Fabric's factory contract: the constructor receives the sprite provider for the particle's
     * sprite set (declared in {@code assets/aetherius/particles/<name>.json}), then produces one
     * particle per spawn packet.
     */
    public static final class Factory implements ParticleFactory<DefaultParticleType> {

        private final SpriteProvider spriteProvider;
        private final float red;
        private final float green;
        private final float blue;
        private final float scale;
        private final int maxAge;
        private final boolean rising;

        public Factory(SpriteProvider spriteProvider, float red, float green, float blue,
                       float scale, int maxAge, boolean rising) {
            this.spriteProvider = spriteProvider;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.scale = scale;
            this.maxAge = maxAge;
            this.rising = rising;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new AetherParticle(world, x, y, z, velocityX, velocityY, velocityZ,
                    spriteProvider, red, green, blue, scale, maxAge, rising);
        }
    }
}
