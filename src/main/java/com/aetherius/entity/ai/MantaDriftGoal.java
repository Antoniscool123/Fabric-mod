package com.aetherius.entity.ai;

import com.aetherius.entity.AetherMantaEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Slow three-dimensional wander for the Aether-Manta.
 *
 * <p>Vanilla wander goals path along the ground, which is useless in an archipelago of floating
 * islands. This goal picks a point in a 24x8x24 box, steers with direct velocity (no navigation
 * graph) and idles for 1-3 minutes between drifts.</p>
 */
public class MantaDriftGoal extends Goal {

    private final AetherMantaEntity manta;
    private Vec3d target;
    private int cooldown;

    public MantaDriftGoal(AetherMantaEntity manta) {
        this.manta = manta;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (manta.hasPassengers()) {
            return false;
        }
        if (cooldown-- > 0) {
            return false;
        }
        Vec3d pos = manta.getPos();
        this.target = pos.add((manta.getRandom().nextDouble() - 0.5) * 24,
                (manta.getRandom().nextDouble() - 0.4) * 8,
                (manta.getRandom().nextDouble() - 0.5) * 24);
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return target != null && manta.getPos().squaredDistanceTo(target) > 4.0;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        Vec3d dir = target.subtract(manta.getPos()).normalize().multiply(0.08);
        manta.setVelocity(manta.getVelocity().add(dir).multiply(0.92));
        manta.setYaw((float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90f);
    }

    @Override
    public void stop() {
        this.cooldown = 60 + manta.getRandom().nextInt(120);
        this.target = null;
    }
}
