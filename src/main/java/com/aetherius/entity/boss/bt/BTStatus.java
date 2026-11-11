package com.aetherius.entity.boss.bt;

/** Result of ticking a behaviour tree node. */
public enum BTStatus {
    /** Node finished successfully this tick. */
    SUCCESS,
    /** Node could not do its job (precondition failed or blocked). */
    FAILURE,
    /** Node is mid-execution and must be ticked again. */
    RUNNING;

    public boolean isTerminal() {
        return this != RUNNING;
    }
}
