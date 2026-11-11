package com.aetherius.entity.boss.phase;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.entity.boss.bt.BehaviorNode;
import com.aetherius.entity.boss.bt.Behaviors;
import com.aetherius.registry.ModSounds;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * <b>Phase 2 - Gravity Collapse</b>
 *
 * <p>Nox'Thal plants four <i>Gravity Anchors</i> and becomes untargetable while any survive.
 * Every 20 seconds it inverts gravity for everyone in the arena, turning the fight into a
 * three-dimensional scramble; destroying all four anchors "grounds" the boss and opens the
 * only damage window of the phase.</p>
 */
public final class GravityCollapsePhase implements BossPhase {

    public static final int GRAVITY_INVERSION_PERIOD = 20 * 20; // 20 seconds
    public static final int GRAVITY_INVERSION_DURATION = 70;

    private final BehaviorNode tree = Behaviors.selector(
            Behaviors.sequence(
                    Behaviors.condition("anchors alive", bb -> ((NoxThalEntity) bb.boss()).hasActiveAnchors()),
                    Behaviors.cooldown("reposition_high", 120,
                            Behaviors.action("hover", bb -> ((NoxThalEntity) bb.boss()).actionReposition(22.0)))
            ),
            Behaviors.sequence(
                    Behaviors.condition("grounded", bb -> ((NoxThalEntity) bb.boss()).isGrounded()),
                    Behaviors.cooldown("ground_slam", 90,
                            Behaviors.action("ground slam", bb -> ((NoxThalEntity) bb.boss()).actionGroundSlam()))
            ),
            Behaviors.cooldown("tail_sweep", 140,
                    Behaviors.action("tail sweep", bb -> ((NoxThalEntity) bb.boss()).actionTailSweep()))
    );

    @Override
    public int index() {
        return 1;
    }

    @Override
    public String id() {
        return "gravity_collapse";
    }

    @Override
    public String titleKey() {
        return "aetherius.boss.nox_thal.phase2";
    }

    @Override
    public int barColor() {
        return 0x4DD2FF;
    }

    @Override
    public BehaviorNode tree() {
        return tree;
    }

    @Override
    public void onEnter(NoxThalEntity boss) {
        tree.reset();
        boss.announce(Text.translatable(titleKey()).formatted(Formatting.AQUA, Formatting.BOLD));
        boss.playBossSound(ModSounds.BOSS_NOX_ROAR, 1.6f, 0.7f);
        boss.summonGravityAnchors(4);
        boss.setElytraLockDuration(20 * 30);
    }

    @Override
    public void tick(NoxThalEntity boss) {
        if (boss.age % GRAVITY_INVERSION_PERIOD == 0) {
            boss.invertGravityArena(GRAVITY_INVERSION_DURATION);
        }
        // Elytra stays suppressed for the whole phase - flight is the phase's whole point.
        if (boss.age % 40 == 0) {
            boss.applyElytraLockToArena(20 * 20);
        }
        if (boss.age % 100 == 0) {
            boss.pulseAnchorGuidance();
        }
    }
}
