package com.aetherius.entity.boss.bt;

/**
 * One node of a boss behaviour tree.
 *
 * <p>Trees are stateless across phases except for running indices, which means a phase can
 * be reset cleanly on transition. Every tick the root is ticked exactly once.</p>
 */
public interface BehaviorNode {

    BTStatus tick(BossBlackboard blackboard);

    /** Clears internal running state - called on phase transitions and boss despawn. */
    default void reset() {
    }

    /** Human-readable debug label, surfaced by {@code /aetherius boss debug}. */
    default String label() {
        return getClass().getSimpleName();
    }
}
