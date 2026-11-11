package com.aetherius.entity.boss.bt;

/**
 * Tries children in priority order until one succeeds or is running. This is the boss's
 * decision layer: highest-priority behaviours (survival, phase mechanics) sit first,
 * ambient movement sits last.
 */
public final class SelectorNode extends CompositeNode {

    public SelectorNode(BehaviorNode... children) {
        super(children);
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        for (BehaviorNode child : children) {
            BTStatus status = child.tick(blackboard);
            if (status != BTStatus.FAILURE) {
                return status;
            }
        }
        return BTStatus.FAILURE;
    }

    @Override
    public String label() {
        return "Selector[" + children.size() + "]";
    }
}
