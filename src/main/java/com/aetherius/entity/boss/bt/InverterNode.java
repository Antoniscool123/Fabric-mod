package com.aetherius.entity.boss.bt;

/** Flips SUCCESS and FAILURE; RUNNING passes through. */
public final class InverterNode implements BehaviorNode {

    private final BehaviorNode child;

    public InverterNode(BehaviorNode child) {
        this.child = child;
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        BTStatus status = child.tick(blackboard);
        return switch (status) {
            case SUCCESS -> BTStatus.FAILURE;
            case FAILURE -> BTStatus.SUCCESS;
            case RUNNING -> BTStatus.RUNNING;
        };
    }

    @Override
    public void reset() {
        child.reset();
    }

    @Override
    public String label() {
        return "Not(" + child.label() + ")";
    }
}
