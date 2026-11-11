package com.aetherius.registry;

import com.aetherius.util.Aeth;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/** All mod sounds. Actual .ogg files live in {@code assets/aetherius/sounds/}. */
public final class ModSounds {

    public static final SoundEvent SPELL_SOL = register("spell_sol");
    public static final SoundEvent SPELL_LUNA = register("spell_luna");
    public static final SoundEvent SPELL_TERRA = register("spell_terra");
    public static final SoundEvent SPELL_CAELUM = register("spell_caelum");
    public static final SoundEvent SPELL_NOX = register("spell_nox");
    public static final SoundEvent SPELL_RESONANT = register("spell_resonant");
    public static final SoundEvent SPELL_NOVA_WARP = register("spell_nova_warp");
    public static final SoundEvent ASTRAL_BACKFIRE = register("astral_backfire");
    public static final SoundEvent CHRONICLE_UPDATE = register("chronicle_update");
    public static final SoundEvent STARFALL = register("starfall");
    public static final SoundEvent BOSS_NOX_ROAR = register("boss_nox_roar");
    public static final SoundEvent OBSERVATORY_HUM = register("observatory_hum");
    public static final SoundEvent ALTAR_PURIFY = register("altar_purify");
    public static final SoundEvent FORGE_STRIKE = register("forge_strike");

    private ModSounds() {
    }

    private static SoundEvent register(String path) {
        var id = Aeth.id(path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        // Force class initialisation.
    }
}
