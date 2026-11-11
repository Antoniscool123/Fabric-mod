package com.aetherius.entity.boss.phase;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.entity.boss.bt.BehaviorNode;

/**
 * A scripted act of the Nox'Thal encounter. Phases own their behaviour tree, their boss-bar
 * colour and their enter/exit choreography (sounds, packets, world state).
 */
public interface BossPhase {

    /** 0-based index, persisted with the boss. */
    int index();

    String id();

    String titleKey();

    /** RGB used by the custom client boss bar overlay (vanilla bar only supports enums). */
    int barColor();

    /** Phase-local behaviour tree; built once and reused. */
    BehaviorNode tree();

    default void onEnter(NoxThalEntity boss) {
    }

    default void onExit(NoxThalEntity boss) {
    }

    /** Per-tick phase mechanics that are not tree-driven (timers, auras, elytra lock). */
    default void tick(NoxThalEntity boss) {
    }
}
