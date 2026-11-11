package com.aetherius.entity.boss.bt;

/**
 * Runs children in order, succeeding only if all succeed. Remembers the running child so
 * multi-tick actions (charges, breaths) resume correctly instead of restarting.
 */
public final class SequenceNode extends CompositeNode {

    private int runningIndex;

    public SequenceNode(BehaviorNode... children) {
        super(children);
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        while (runningIndex < children.size()) {
            BTStatus status = children.get(runningIndex).tick(blackboard);
            if (status == BTStatus.RUNNING) {
                return BTStatus.RUNNING;
            }
            if (status == BTStatus.FAILURE) {
                runningIndex = 0;
                return BTStatus.FAILURE;
            }
            runningIndex++;
        }
        runningIndex = 0;
        return BTStatus.SUCCESS;
    }

    @Override
    public void reset() {
        runningIndex = 0;
        super.reset();
    }

    @Override
    public String label() {
        return "Sequence[" + children.size() + "]";
    }
}
