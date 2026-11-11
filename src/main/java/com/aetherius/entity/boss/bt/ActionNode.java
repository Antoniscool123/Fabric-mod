package com.aetherius.entity.boss.bt;

/** Leaf: a single boss action. Implemented as a lambda by most phases. */
public final class ActionNode implements BehaviorNode {

    private final String name;
    private final BossAction action;

    public ActionNode(String name, BossAction action) {
        this.name = name;
        this.action = action;
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        return action.tick(blackboard);
    }

    @Override
    public void reset() {
        action.reset();
    }

    @Override
    public String label() {
        return "Do(" + name + ")";
    }

    /** Stateful action callback. Reset is invoked on phase transitions. */
    @FunctionalInterface
    public interface BossAction {
        BTStatus tick(BossBlackboard blackboard);

        default void reset() {
        }
    }
}
