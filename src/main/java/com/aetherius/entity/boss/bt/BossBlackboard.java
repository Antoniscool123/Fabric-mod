package com.aetherius.entity.boss.bt;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared memory for a boss fight: the boss, its target, cached spatial facts and a small
 * string-keyed memory map used by cooldown/wait decorators.
 */
public final class BossBlackboard {

    private final MobEntity boss;
    private final Map<String, Object> memory = new HashMap<>();
    private LivingEntity target;
    private double squaredDistance = Double.MAX_VALUE;
    private boolean targetVisible;
    private long ticks;

    public BossBlackboard(MobEntity boss) {
        this.boss = boss;
    }

    /** Refreshes cached target facts once per boss tick. */
    public void refresh() {
        this.ticks++;
        this.target = boss.getTarget();
        if (target != null && target.isAlive()) {
            this.squaredDistance = boss.squaredDistanceTo(target);
            this.targetVisible = boss.canSee(target);
        } else {
            this.target = null;
            this.squaredDistance = Double.MAX_VALUE;
            this.targetVisible = false;
        }
    }

    public MobEntity boss() {
        return boss;
    }

    public LivingEntity target() {
        return target;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public double distance() {
        return Math.sqrt(squaredDistance);
    }

    public double squaredDistance() {
        return squaredDistance;
    }

    public boolean targetVisible() {
        return targetVisible;
    }

    public long ticks() {
        return ticks;
    }

    public Vec3d targetPos() {
        return target == null ? boss.getPos() : target.getPos();
    }

    // ---- Memory -----------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> T memory(String key, T fallback) {
        Object value = memory.get(key);
        return value == null ? fallback : (T) value;
    }

    public void remember(String key, Object value) {
        memory.put(key, value);
    }

    public long memoryLong(String key) {
        Object value = memory.get(key);
        return value instanceof Number number ? number.longValue() : Long.MIN_VALUE;
    }

    public void forget(String key) {
        memory.remove(key);
    }

    public void clearMemory() {
        memory.clear();
    }
}
