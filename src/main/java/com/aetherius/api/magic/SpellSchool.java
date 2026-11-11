package com.aetherius.api.magic;

import net.minecraft.util.Formatting;

/**
 * The five celestial schools of Aetherius. Each school maps to one of the shattered
 * sky-gods and drives rune cost curves, visual identity and Chronicle progression.
 *
 * <table>
 *   <caption>School identity</caption>
 *   <tr><th>School</th><th>Deity</th><th>Role</th><th>Primary resource pressure</th></tr>
 *   <tr><td>Sol</td><td>Solaeth</td><td>Damage / armor shred / light</td><td>Mana</td></tr>
 *   <tr><td>Luna</td><td>Lunara</td><td>Regeneration / freeze / fluids</td><td>Mana</td></tr>
 *   <tr><td>Terra</td><td>Terrath</td><td>Defense / walls / transmutation</td><td>Mana</td></tr>
 *   <tr><td>Caelum</td><td>Caelus</td><td>Flight / knockback / lightning</td><td>Mana</td></tr>
 *   <tr><td>Nox</td><td>Nox'Thal</td><td>Gravity / teleport / life-drain</td><td><b>Astral Tension</b></td></tr>
 * </table>
 */
public enum SpellSchool {
    SOL("sol", 0xFF8A3D, Formatting.GOLD),
    LUNA("luna", 0x8FD6FF, Formatting.AQUA),
    TERRA("terra", 0x9C7B4F, Formatting.DARK_GREEN),
    CAELUM("caelum", 0xD9E7FF, Formatting.WHITE),
    NOX("nox", 0x9B4DFF, Formatting.DARK_PURPLE);

    private final String id;
    private final int color;
    private final Formatting formatting;

    SpellSchool(String id, int color, Formatting formatting) {
        this.id = id;
        this.color = color;
        this.formatting = formatting;
    }

    public String id() {
        return id;
    }

    /** ARGB colour used by HUD, particles and spell trails. */
    public int color() {
        return color;
    }

    public Formatting formatting() {
        return formatting;
    }

    /** Voidmancy is the only school that feeds the Astral Tension meter. */
    public boolean feedsTension() {
        return this == NOX;
    }

    public static SpellSchool byId(String id) {
        for (SpellSchool school : values()) {
            if (school.id.equals(id)) {
                return school;
            }
        }
        return SOL;
    }
}
