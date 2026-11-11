package com.aetherius.util;

import com.aetherius.Aetherius;
import net.minecraft.util.Identifier;

/**
 * Short identifier factory used across the mod.
 *
 * <p>Every registry key in Aetherius is namespaced through this helper so a namespace
 * migration is a one-line change. Prefer {@code Aeth.id("star_vein_ore")} over string
 * concatenation anywhere a vanilla registry is touched.</p>
 */
public final class Aeth {
    private Aeth() {
    }

    /** @return {@code aetherius:<path>} */
    public static Identifier id(String path) {
        return new Identifier(Aetherius.MOD_ID, path);
    }

    /** @return {@code <namespace>:<path>} for keys owned by other mods/vanilla. */
    public static Identifier vanilla(String path) {
        return new Identifier("minecraft", path);
    }

    /** Translation key helper: {@code aetherius.spell.nova_warp}. */
    public static String lang(String category, String path) {
        return Aetherius.MOD_ID + "." + category + "." + path;
    }
}
