package com.aetherius.entity.boss.bt;

import java.util.function.Predicate;

/** Terse factory methods so phase trees stay readable in source. */
public final class Behaviors {

    private Behaviors() {
    }

    public static SequenceNode sequence(BehaviorNode... children) {
        return new SequenceNode(children);
    }

    public static SelectorNode selector(BehaviorNode... children) {
        return new SelectorNode(children);
    }

    public static ConditionNode condition(String name, Predicate<BossBlackboard> predicate) {
        return new ConditionNode(name, predicate);
    }

    public static ActionNode action(String name, ActionNode.BossAction action) {
        return new ActionNode(name, action);
    }

    public static ActionNode once(String name, Runnable runnable) {
        return new ActionNode(name, blackboard -> {
            runnable.run();
            return BTStatus.SUCCESS;
        });
    }

    public static InverterNode not(BehaviorNode node) {
        return new InverterNode(node);
    }

    public static CooldownNode cooldown(String key, int ticks, BehaviorNode node) {
        return new CooldownNode(key, ticks, node);
    }

    public static WaitNode wait(int ticks) {
        return new WaitNode(ticks);
    }

    public static WaitNode wait(int ticks, Runnable onStart, Runnable onFinish) {
        return new WaitNode(ticks, onStart, onFinish);
    }
}
