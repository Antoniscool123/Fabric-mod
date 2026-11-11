package com.aetherius.api.magic;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a spell cast attempt. Returned by
 * {@link PlayerMagicCapability#cast(com.aetherius.api.spell.RuneCombination)} so callers
 * (network handler, wand item, boss scripting) never need to guess why nothing happened.
 */
public record CastResult(boolean success, @Nullable Identifier spellId, Reason reason,
                         float manaSpent, float tensionAdded) {

    public enum Reason {
        OK,
        CLIENT_SIDE,
        UNKNOWN_COMBINATION,
        NOT_ENOUGH_MANA,
        ON_COOLDOWN,
        SILENCED,
        NO_WAND,
        BACKFIRING
    }

    public static CastResult ok(Identifier spellId, float manaSpent, float tensionAdded) {
        return new CastResult(true, spellId, Reason.OK, manaSpent, tensionAdded);
    }

    public static CastResult fail(Reason reason) {
        return new CastResult(false, null, reason, 0f, 0f);
    }

    public String messageKey() {
        return "aetherius.cast." + reason.name().toLowerCase(java.util.Locale.ROOT);
    }
}
