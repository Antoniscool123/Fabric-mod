package com.aetherius.entity.boss.phase;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.entity.boss.bt.BehaviorNode;
import com.aetherius.entity.boss.bt.Behaviors;
import com.aetherius.registry.ModSounds;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * <b>Phase 1 - The Starlit Herald</b>
 *
 * <p>Nox'Thal circles at altitude, dives through the player's position and breathes Cosmic
 * Fire. It is <b>immune to all ranged attacks</b> in this phase (arrows, tridents and thrown
 * potions bounce off its eclipse scales), which forces melee and spell combat and teaches
 * the player that the fight is about positioning, not DPS uptime.</p>
 */
public final class StarlitHeraldPhase implements BossPhase {

    private final BehaviorNode tree = Behaviors.selector(
            Behaviors.sequence(
                    Behaviors.condition("has target", BossBlackboardChecks.HAS_TARGET),
                    Behaviors.condition("swoop window", bb -> bb.distance() > 4 && bb.distance() < 34),
                    Behaviors.cooldown("swoop", 110,
                            Behaviors.action("swoop", bb -> ((NoxThalEntity) bb.boss()).actionSwoop()))
            ),
            Behaviors.sequence(
                    Behaviors.condition("breath window", bb -> bb.distance() > 5 && bb.distance() < 18),
                    Behaviors.cooldown("cosmic_fire", 190,
                            Behaviors.action("cosmic fire", bb -> ((NoxThalEntity) bb.boss()).actionCosmicBreath()))
            ),
            Behaviors.cooldown("reposition", 70,
                    Behaviors.action("circle", bb -> ((NoxThalEntity) bb.boss()).actionReposition(14.0)))
    );

    @Override
    public int index() {
        return 0;
    }

    @Override
    public String id() {
        return "starlit_herald";
    }

    @Override
    public String titleKey() {
        return "aetherius.boss.nox_thal.phase1";
    }

    @Override
    public int barColor() {
        return 0x9B4DFF;
    }

    @Override
    public BehaviorNode tree() {
        return tree;
    }

    @Override
    public void onEnter(NoxThalEntity boss) {
        tree.reset();
        boss.setRangedImmune(true);
        boss.announce(Text.translatable(titleKey()).formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        boss.playBossSound(ModSounds.BOSS_NOX_ROAR, 1.4f, 0.9f);
    }

    @Override
    public void onExit(NoxThalEntity boss) {
        boss.setRangedImmune(false);
        tree.reset();
    }

    /** Phase 1 aura: slowly drains mana from everyone nearby, feeding the Herald's flight. */
    @Override
    public void tick(NoxThalEntity boss) {
        if (boss.age % 60 == 0) {
            boss.drainNearbyMana(20, 2.0f);
        }
    }
}
