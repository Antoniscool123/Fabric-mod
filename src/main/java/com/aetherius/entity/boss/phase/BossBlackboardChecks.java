package com.aetherius.entity.boss.phase;

import com.aetherius.entity.boss.bt.BossBlackboard;

import java.util.function.Predicate;

/** Reusable predicates shared by Nox'Thal's phase trees. */
final class BossBlackboardChecks {

    static final Predicate<BossBlackboard> HAS_TARGET = BossBlackboard::hasTarget;
    static final Predicate<BossBlackboard> TARGET_VISIBLE = BossBlackboard::targetVisible;

    private BossBlackboardChecks() {
    }
}
