package com.aetherius.entity.boss.bt;

import java.util.function.Predicate;

/** Leaf: pure gate, never RUNNING. */
public final class ConditionNode implements BehaviorNode {

    private final String name;
    private final Predicate<BossBlackboard> predicate;

    public ConditionNode(String name, Predicate<BossBlackboard> predicate) {
        this.name = name;
        this.predicate = predicate;
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        return predicate.test(blackboard) ? BTStatus.SUCCESS : BTStatus.FAILURE;
    }

    @Override
    public String label() {
        return "If(" + name + ")";
    }
}
