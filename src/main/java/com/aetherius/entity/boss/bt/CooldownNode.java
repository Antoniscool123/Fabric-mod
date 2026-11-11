package com.aetherius.entity.boss.bt;

/**
 * Decorator that rate-limits its child. Uses blackboard memory so the cooldown survives tree
 * rebuilds but not boss death (memory is cleared on reset).
 */
public final class CooldownNode implements BehaviorNode {

    private final String key;
    private final int cooldownTicks;
    private final BehaviorNode child;

    public CooldownNode(String key, int cooldownTicks, BehaviorNode child) {
        this.key = key;
        this.cooldownTicks = cooldownTicks;
        this.child = child;
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        long last = blackboard.memoryLong("cd:" + key);
        if (last != Long.MIN_VALUE && blackboard.ticks() - last < cooldownTicks) {
            return BTStatus.FAILURE;
        }
        BTStatus status = child.tick(blackboard);
        if (status == BTStatus.SUCCESS) {
            blackboard.remember("cd:" + key, blackboard.ticks());
        }
        return status;
    }

    @Override
    public void reset() {
        child.reset();
    }

    @Override
    public String label() {
        return "Every(" + cooldownTicks + "t, " + child.label() + ")";
    }
}
