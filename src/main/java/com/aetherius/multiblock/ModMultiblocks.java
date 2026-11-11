package com.aetherius.multiblock;

import com.aetherius.registry.ModBlocks;
import net.minecraft.block.Blocks;

import java.util.function.Predicate;

/**
 * The two authored multiblocks.
 *
 * <h2>Cosmic Forge (3x3x3)</h2>
 * <pre>
 * y2  # # #      (open/anything)
 *     # # #
 *     # # #
 * y1  P P P      P = Celestial Pillar (8 of them)
 *     P F P      F = Aether Forge Block (centre)
 *     P P P
 * y0  Q L Q      Q = Quartz, L = Lapis (the inscribed base)
 *     L Q L
 *     Q L Q
 * </pre>
 *
 * <h2>Aetherial Altar of Purification (5x5x5)</h2>
 * A sealed quartz/lapis base and lid with a hollow pillar chamber; the
 * {@code Aether Altar Core} sits in the exact centre. Rites require the player to stand
 * inside the chamber, which is why {@code '.'} accepts air <i>or</i> any non-solid block.
 */
public final class ModMultiblocks {

    public static final Predicate<net.minecraft.block.BlockState> LAPIS = state -> state.isOf(Blocks.LAPIS_BLOCK);
    public static final Predicate<net.minecraft.block.BlockState> QUARTZ =
            state -> state.isOf(Blocks.QUARTZ_BLOCK) || state.isOf(Blocks.SMOOTH_QUARTZ)
                    || state.isOf(Blocks.CHISELED_QUARTZ_BLOCK);
    public static final Predicate<net.minecraft.block.BlockState> PILLAR =
            state -> state.isOf(ModBlocks.CELESTIAL_PILLAR);

    public static final MultiblockPattern COSMIC_FORGE = MultiblockPattern.builder("cosmic_forge", 3, 3, 3)
            .layer("QLQ", "LQL", "QLQ")
            .layer("PPP", "PFP", "PPP")
            .layer("###", "###", "###")
            .legend('Q', QUARTZ)
            .legend('L', LAPIS)
            .legend('P', PILLAR)
            .legend('F', ModBlocks.AETHER_FORGE)
            .build();

    /** Forge core sits at local (1, 1, 1). */
    public static final net.minecraft.util.math.BlockPos FORGE_CORE_OFFSET =
            new net.minecraft.util.math.BlockPos(1, 1, 1);

    public static final MultiblockPattern AETHER_ALTAR = MultiblockPattern.builder("aether_altar", 5, 5, 5)
            .layer("LQQQL", "QQQQQ", "QQQQQ", "QQQQQ", "LQQQL")
            .layer("PPPPP", "P...P", "P...P", "P...P", "PPPPP")
            .layer("PPPPP", "P...P", "P.A.P", "P...P", "PPPPP")
            .layer("PPPPP", "P...P", "P...P", "P...P", "PPPPP")
            .layer("QQQQQ", "QQQQQ", "QQQQQ", "QQQQQ", "QQQQQ")
            .legend('Q', QUARTZ)
            .legend('L', LAPIS)
            .legend('P', PILLAR)
            .legend('A', ModBlocks.AETHER_ALTAR_CORE)
            .build();

    /** Altar core sits at local (2, 2, 2) - the exact centre. */
    public static final net.minecraft.util.math.BlockPos ALTAR_CORE_OFFSET =
            new net.minecraft.util.math.BlockPos(2, 2, 2);

    private ModMultiblocks() {
    }
}
