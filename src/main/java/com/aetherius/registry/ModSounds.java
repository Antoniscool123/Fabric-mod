package com.aetherius.registry;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Sound palette of Aetherius.
 *
 * <p>We deliberately do <b>not</b> register custom {@code SoundEvent}s: shipping empty events
 * produces {@code Unable to play empty soundEvent} spam, and pointing {@code sounds.json} at
 * guessed vanilla {@code .ogg} paths produces {@code File minecraft:sounds/....ogg does not
 * exist} spam. Instead every entry here is an alias of a real vanilla {@link SoundEvent}
 * constant, so the compiler guarantees the file exists.</p>
 */
public final class ModSounds {

    public static final SoundEvent SPELL_SOL = SoundEvents.ITEM_FIRECHARGE_USE;
    public static final SoundEvent SPELL_LUNA = SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
    public static final SoundEvent SPELL_TERRA = SoundEvents.BLOCK_STONE_BREAK;
    public static final SoundEvent SPELL_CAELUM = SoundEvents.ENTITY_PHANTOM_FLAP;
    public static final SoundEvent SPELL_NOX = SoundEvents.ENTITY_WARDEN_SONIC_BOOM;
    public static final SoundEvent SPELL_RESONANT = SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
    public static final SoundEvent SPELL_NOVA_WARP = SoundEvents.ENTITY_ENDERMAN_TELEPORT;
    public static final SoundEvent ASTRAL_BACKFIRE = SoundEvents.ENTITY_BLAZE_HURT;
    public static final SoundEvent CHRONICLE_UPDATE = SoundEvents.ITEM_BOOK_PAGE_TURN;
    public static final SoundEvent STARFALL = SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
    public static final SoundEvent BOSS_NOX_ROAR = SoundEvents.ENTITY_ENDER_DRAGON_GROWL;
    public static final SoundEvent OBSERVATORY_HUM = SoundEvents.BLOCK_BEACON_AMBIENT;
    public static final SoundEvent ALTAR_PURIFY = SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE;
    public static final SoundEvent FORGE_STRIKE = SoundEvents.BLOCK_ANVIL_USE;

    private ModSounds() {
    }

    public static void register() {
        // Nothing to register; kept so the initialiser list stays stable.
    }
}
