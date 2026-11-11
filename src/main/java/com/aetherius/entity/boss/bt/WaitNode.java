package com.aetherius.entity.boss.bt;

/**
 * Holds RUNNING for a fixed number of ticks, then succeeds. Used for telegraph windows
 * (the 1.5 seconds before a star-strike lands, for example).
 */
public final class WaitNode implements BehaviorNode {

    private final int waitTicks;
    private final Runnable onStart;
    private final Runnable onFinish;
    private int elapsed;
    private boolean started;

    public WaitNode(int waitTicks) {
        this(waitTicks, () -> {
        }, () -> {
        });
    }

    public WaitNode(int waitTicks, Runnable onStart, Runnable onFinish) {
        this.waitTicks = waitTicks;
        this.onStart = onStart;
        this.onFinish = onFinish;
    }

    @Override
    public BTStatus tick(BossBlackboard blackboard) {
        if (!started) {
            started = true;
            onStart.run();
        }
        if (++elapsed >= waitTicks) {
            onFinish.run();
            elapsed = 0;
            started = false;
            return BTStatus.SUCCESS;
        }
        return BTStatus.RUNNING;
    }

    @Override
    public void reset() {
        elapsed = 0;
        started = false;
    }

    @Override
    public String label() {
        return "Wait(" + waitTicks + "t)";
    }
}
