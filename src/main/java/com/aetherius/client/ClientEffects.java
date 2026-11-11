package com.aetherius.client;

import com.aetherius.network.BossEventS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Purely visual client state driven by server packets: HUD shake, boss phase (for the custom
 * bar colour) and the Supernova darkness overlay.
 */
public final class ClientEffects {

    private static int shakeTicks;
    private static int shakeIntensity;
    private static int darknessTicks;
    private static int darknessDuration;
    private static final Map<Integer, Integer> BOSS_PHASE = new HashMap<>();
    private static int lastBossId = -1;

    private ClientEffects() {
    }

    // ---- Shake ------------------------------------------------------------

    public static void triggerShake(int intensity, int durationTicks) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeTicks = Math.max(shakeTicks, durationTicks);
    }

    public static void tickShake() {
        if (shakeTicks > 0) {
            if (--shakeTicks == 0) {
                shakeIntensity = 0;
            }
        }
    }

    public static boolean isShaking() {
        return shakeTicks > 0 && shakeIntensity > 0;
    }

    /** Offsets in screen pixels, sinusoidally driven so the shake reads as a rumble. */
    public static float shakeX() {
        if (!isShaking()) {
            return 0f;
        }
        return (float) Math.sin(shakeTicks * 1.7) * (shakeIntensity * 0.12f);
    }

    public static float shakeY() {
        if (!isShaking()) {
            return 0f;
        }
        return (float) Math.cos(shakeTicks * 2.3) * (shakeIntensity * 0.1f);
    }

    // ---- Boss -------------------------------------------------------------

    public static void onBossEvent(BossEventS2CPacket.BossEvent event) {
        switch (event.kind()) {
            case PHASE_CHANGE -> {
                BOSS_PHASE.put(event.entityId(), event.phase());
                lastBossId = event.entityId();
                triggerShake(10, 20);
            }
            case SUPERNOVA_DARKEN -> {
                darknessTicks = Math.max(20 * 30, event.durationTicks());
                darknessDuration = darknessTicks;
            }
            case SUPERNOVA_END -> darknessTicks = 0;
            case GRAVITY_PULSE -> triggerShake(6, event.durationTicks() > 0 ? 18 : 12);
            case ELYTHERA_LOCK -> triggerShake(4, 10);
            case BOSS_DEATH -> {
                darknessTicks = 0;
                BOSS_PHASE.remove(event.entityId());
            }
        }
    }

    public static int bossPhase(int entityId) {
        return BOSS_PHASE.getOrDefault(entityId, 0);
    }

    public static int lastBossId() {
        return lastBossId;
    }

    public static int phaseColor(int phase) {
        return switch (phase) {
            case 1 -> 0x4DD2FF;
            case 2 -> 0xFF8A3D;
            default -> 0x9B4DFF;
        };
    }

    public static void tickDarkness() {
        if (darknessTicks > 0) {
            darknessTicks--;
        }
    }

    public static boolean isDark() {
        return darknessTicks > 0;
    }

    /** 0 (transparent) .. 0.95 (near-total eclipse), easing out in the last 3 seconds. */
    public static float darknessAlpha() {
        if (darknessTicks <= 0 || darknessDuration <= 0) {
            return 0f;
        }
        float fade = Math.min(1f, darknessTicks / 60f);
        return 0.95f * fade;
    }

    public static void reset() {
        shakeTicks = 0;
        shakeIntensity = 0;
        darknessTicks = 0;
        darknessDuration = 0;
        BOSS_PHASE.clear();
        lastBossId = -1;
    }

    /** Clamp helper used by overlays. */
    public static float clamp01(float value) {
        return MathHelper.clamp(value, 0f, 1f);
    }
}
