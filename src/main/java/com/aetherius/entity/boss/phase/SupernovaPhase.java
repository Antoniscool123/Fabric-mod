package com.aetherius.entity.boss.phase;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.entity.boss.bt.BehaviorNode;
import com.aetherius.entity.boss.bt.Behaviors;
import com.aetherius.network.BossEventS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * <b>Phase 3 - Supernova</b>
 *
 * <p>The arena goes pitch black (client-side overlay keyed to a packet, not a potion effect,
 * so night vision cannot trivialise it). Players navigate by the glowing runes of Rune
 * Lanterns they placed beforehand while dodging telegraphed AOE star-strikes. Nox'Thal keeps
 * its ranged immunity off here - the fight becomes a damage race against visibility.</p>
 */
public final class SupernovaPhase implements BossPhase {

    public static final int DARKNESS_REFRESH = 20 * 12;

    private final BehaviorNode tree = Behaviors.selector(
            Behaviors.sequence(
                    Behaviors.condition("has target", BossBlackboardChecks.HAS_TARGET),
                    Behaviors.cooldown("star_strike", 45,
                            Behaviors.action("star strike", bb -> ((NoxThalEntity) bb.boss()).actionStarStrike()))
            ),
            Behaviors.sequence(
                    Behaviors.condition("close", bb -> bb.distance() < 12),
                    Behaviors.cooldown("eclipse_claw", 70,
                            Behaviors.action("eclipse claw", bb -> ((NoxThalEntity) bb.boss()).actionMeleeLunge()))
            ),
            Behaviors.cooldown("reposition", 100,
                    Behaviors.action("drift", bb -> ((NoxThalEntity) bb.boss()).actionReposition(10.0)))
    );

    @Override
    public int index() {
        return 2;
    }

    @Override
    public String id() {
        return "supernova";
    }

    @Override
    public String titleKey() {
        return "aetherius.boss.nox_thal.phase3";
    }

    @Override
    public int barColor() {
        return 0xFF8A3D;
    }

    @Override
    public BehaviorNode tree() {
        return tree;
    }

    @Override
    public void onEnter(NoxThalEntity boss) {
        tree.reset();
        boss.announce(Text.translatable(titleKey()).formatted(Formatting.GOLD, Formatting.BOLD));
        boss.playBossSound(net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        boss.beginSupernova();
    }

    @Override
    public void onExit(NoxThalEntity boss) {
        boss.endSupernova();
        tree.reset();
    }

    @Override
    public void tick(NoxThalEntity boss) {
        if (boss.age % DARKNESS_REFRESH == 0) {
            boss.refreshSupernovaDarkness();
        }
        if (boss.age % 80 == 0) {
            boss.applyElytraLockToArena(20 * 15);
        }
        if (boss.age % 200 == 0) {
            boss.forceTensionSurgeArena();
        }
    }

    /** Exposed for the client overlay contract documentation. */
    public static BossEventS2CPacket.Kind darknessEvent() {
        return BossEventS2CPacket.Kind.SUPERNOVA_DARKEN;
    }
}
