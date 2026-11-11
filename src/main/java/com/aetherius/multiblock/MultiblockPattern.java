package com.aetherius.multiblock;

import com.aetherius.util.Aeth;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A rotation-aware 3D block pattern.
 *
 * <h2>Format</h2>
 * <pre>{@code
 * MultiblockPattern.builder("cosmic_forge", 3, 3, 3)
 *     .layer("QLQ", "LQL", "QLQ")   // y = 0, rows are z, chars are x
 *     .layer("PPP", "PFP", "PPP")   // y = 1
 *     .layer("###", "###", "###")   // y = 2
 *     .legend('Q', ModMultiblocks.QUARTZ)
 *     .legend('L', ModMultiblocks.LAPIS)
 *     .legend('P', state -> state.isOf(ModBlocks.CELESTIAL_PILLAR))
 *     .legend('F', state -> state.isOf(ModBlocks.AETHER_FORGE))
 *     .build();
 * }</pre>
 *
 * <p>Legend semantics: {@code '#'} matches anything, {@code '.'} matches air or any
 * non-solid block (so players can stand inside), everything else is a predicate. The
 * matcher tries all four horizontal rotations so a player can build the structure facing
 * any direction.</p>
 */
public final class MultiblockPattern {

    public static final char WILDCARD = '#';
    public static final char PASSABLE = '.';

    private final Identifier id;
    private final int width;
    private final int height;
    private final int depth;
    private final String[][] layers;
    private final Map<Character, Predicate<BlockState>> legend;

    private MultiblockPattern(Identifier id, int width, int height, int depth, String[][] layers,
                              Map<Character, Predicate<BlockState>> legend) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.layers = layers;
        this.legend = legend;
    }

    public Identifier id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    /** Attempts every rotation; returns the first match (or the best mismatch report). */
    public Result match(World world, BlockPos origin) {
        Result best = null;
        for (int rotation = 0; rotation < 4; rotation++) {
            Result result = matchRotated(world, origin, rotation);
            if (result.matched()) {
                return result;
            }
            if (best == null || result.mismatches().size() < best.mismatches().size()) {
                best = result;
            }
        }
        return best == null ? new Result(false, 0, List.of()) : best;
    }

    private Result matchRotated(World world, BlockPos origin, int rotation) {
        List<BlockPos> mismatches = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            String[] layer = layers[y];
            for (int z = 0; z < depth; z++) {
                String row = layer[z];
                for (int x = 0; x < width; x++) {
                    char symbol = row.charAt(x);
                    if (symbol == WILDCARD) {
                        continue;
                    }
                    int[] rotated = rotate(x, z, rotation);
                    BlockPos worldPos = origin.add(rotated[0], y, rotated[1]);
                    BlockState state = world.getBlockState(worldPos);
                    if (!matches(symbol, state, world, worldPos)) {
                        mismatches.add(worldPos);
                    }
                }
            }
        }
        return new Result(mismatches.isEmpty(), rotation, mismatches);
    }

    private int[] rotate(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> new int[]{depth - 1 - z, x};
            case 2 -> new int[]{width - 1 - x, depth - 1 - z};
            case 3 -> new int[]{z, width - 1 - x};
            default -> new int[]{x, z};
        };
    }

    private boolean matches(char symbol, BlockState state, World world, BlockPos pos) {
        if (symbol == PASSABLE) {
            return state.isAir() || !state.isSolidBlock(world, pos);
        }
        Predicate<BlockState> predicate = legend.get(symbol);
        return predicate != null && predicate.test(state);
    }

    public boolean isInsidePattern(BlockPos coreOffset, BlockPos relative) {
        return relative.getX() >= 0 && relative.getX() < width
                && relative.getY() >= 0 && relative.getY() < height
                && relative.getZ() >= 0 && relative.getZ() < depth;
    }

    public static Builder builder(String path, int width, int height, int depth) {
        return new Builder(Aeth.id(path), width, height, depth);
    }

    public record Result(boolean matched, int rotation, List<BlockPos> mismatches) {
        public boolean isComplete() {
            return matched;
        }

        public int progress() {
            return mismatches.isEmpty() ? 1 : 0;
        }

        public String summary() {
            if (matched) {
                return "formed (rotation " + rotation + ")";
            }
            return "incomplete: " + mismatches.size() + " block(s) off";
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final int width;
        private final int height;
        private final int depth;
        private final List<String[]> layerList = new ArrayList<>();
        private final Map<Character, Predicate<BlockState>> legend = new HashMap<>();

        private Builder(Identifier id, int width, int height, int depth) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public Builder layer(String... rows) {
            if (rows.length != depth) {
                throw new IllegalArgumentException("Layer must contain " + depth + " rows, got " + rows.length);
            }
            for (String row : rows) {
                if (row.length() != width) {
                    throw new IllegalArgumentException("Row '" + row + "' must be " + width + " wide");
                }
            }
            layerList.add(rows);
            return this;
        }

        public Builder legend(char symbol, Block block) {
            legend.put(symbol, state -> state.isOf(block));
            return this;
        }

        public Builder legend(char symbol, Predicate<BlockState> predicate) {
            legend.put(symbol, predicate);
            return this;
        }

        public MultiblockPattern build() {
            if (layerList.size() != height) {
                throw new IllegalStateException("Expected " + height + " layers, got " + layerList.size());
            }
            String[][] layers = new String[height][];
            for (int i = 0; i < height; i++) {
                layers[i] = layerList.get(i);
            }
            legend.putIfAbsent(WILDCARD, state -> true);
            return new MultiblockPattern(id, width, height, depth, layers, Map.copyOf(legend));
        }
    }
}
