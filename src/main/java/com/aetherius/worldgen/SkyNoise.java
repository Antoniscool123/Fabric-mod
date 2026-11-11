package com.aetherius.worldgen;

/**
 * Deterministic value noise for the Shattered Sky.
 *
 * <p>Kept deliberately small and dependency-free: a hash on an integer lattice, smooth
 * interpolation, and an fbm sum. It is <b>not</b> tileable (world coordinates, not textures),
 * and every function takes an explicit seed so the terrain, the biome regions and the
 * decoration pass can share one field without ever touching Java's {@code Random}.</p>
 *
 * <p>All methods are pure and thread-safe: chunk generation runs on worker threads, so nothing
 * here may hold mutable state. The only mutable thing in the whole worldgen stack is
 * {@link com.aetherius.dimension.ShatteredSkyChunkGenerator#worldSeed()}, set once at server
 * start.</p>
 */
public final class SkyNoise {

    private SkyNoise() {
    }

    /** Stable 0..1 hash of a lattice point. */
    public static double hash(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /** Bilinear value noise in 0..1; {@code scale} is in "cells per block". */
    public static double value(double x, double z, long seed, double scale) {
        double fx = x * scale;
        double fz = z * scale;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = smooth(fx - x0);
        double tz = smooth(fz - z0);
        double a = hash(x0, z0, seed);
        double b = hash(x0 + 1, z0, seed);
        double c = hash(x0, z0 + 1, seed);
        double d = hash(x0 + 1, z0 + 1, seed);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    /** Fractal sum of value noise, normalised to 0..1. */
    public static double fbm(double x, double z, long seed, double scale, int octaves) {
        double total = 0.0;
        double norm = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        for (int i = 0; i < octaves; i++) {
            total += value(x, z, seed + i * 7919L, frequency) * amplitude;
            norm += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        return norm == 0.0 ? 0.5 : total / norm;
    }
}
