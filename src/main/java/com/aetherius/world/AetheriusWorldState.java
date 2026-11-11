package com.aetherius.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Overworld-persistent story and corruption state.
 *
 * <p>Stored once on the overworld's {@code PersistentStateManager} so it survives restarts
 * and is available cross-dimension. Three datasets:</p>
 * <ul>
 *   <li><b>Blight</b>: chunk -> intensity (0-100). Never deleted on spread, only on purify,
 *       which is what lets the Astrolabe and Chronicle report "infected regions".</li>
 *   <li><b>Observatories</b>: positions recorded by worldgen so the Astrolabe can point at
 planetary-scale distances without a structure-locator round trip.</li>
 *   <li><b>Story flags</b>: world-scoped milestones such as the Shattered Sky being opened.</li>
 * </ul>
 */
public class AetheriusWorldState extends PersistentState {

    public static final String ID = "aetherius_world_state";
    public static final int MAX_BLIGHT = 100;

    // 1.20.1 has no PersistentState.Type: state is registered through
    // PersistentStateManager#getOrCreate(readFunction, factory, id).

    private final Map<Long, Integer> blight = new HashMap<>();
    private final List<BlockPos> observatories = new ArrayList<>();
    private boolean noxDefeated;
    private long lastDecayTick;

    public AetheriusWorldState() {
    }

    public static AetheriusWorldState get(ServerWorld world) {
        ServerWorld overworld = world.getServer().getOverworld();
        return overworld.getPersistentStateManager()
                .getOrCreate(AetheriusWorldState::fromNbt, AetheriusWorldState::new, ID);
    }

    // ---- Blight -----------------------------------------------------------

    public int blightAt(ChunkPos pos) {
        return blight.getOrDefault(pos.toLong(), 0);
    }

    public boolean isBlighted(ChunkPos pos) {
        return blightAt(pos) > 0;
    }

    public int addBlight(ChunkPos pos, int amount) {
        int next = Math.min(MAX_BLIGHT, blightAt(pos) + amount);
        blight.put(pos.toLong(), next);
        markDirty();
        return next;
    }

    public void setBlight(ChunkPos pos, int value) {
        if (value <= 0) {
            if (blight.remove(pos.toLong()) != null) {
                markDirty();
            }
            return;
        }
        blight.put(pos.toLong(), Math.min(MAX_BLIGHT, value));
        markDirty();
    }

    public int clearBlightAround(ChunkPos center, int chunkRadius) {
        int cleared = 0;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                long key = ChunkPos.toLong(center.x + dx, center.z + dz);
                if (blight.remove(key) != null) {
                    cleared++;
                }
            }
        }
        if (cleared > 0) {
            markDirty();
        }
        return cleared;
    }

    public Map<Long, Integer> blightSnapshot() {
        return Map.copyOf(blight);
    }

    // ---- Observatories ----------------------------------------------------

    public void addObservatory(BlockPos pos) {
        BlockPos immutable = pos.toImmutable();
        if (!observatories.contains(immutable)) {
            observatories.add(immutable);
            markDirty();
        }
    }

    public Optional<BlockPos> findNearestObservatory(BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : observatories) {
            double distance = candidate.getSquaredDistance(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    public List<BlockPos> observatories() {
        return List.copyOf(observatories);
    }

    // ---- Story ------------------------------------------------------------

    public boolean isNoxDefeated() {
        return noxDefeated;
    }

    public void setNoxDefeated(boolean value) {
        noxDefeated = value;
        markDirty();
    }

    public long lastDecayTick() {
        return lastDecayTick;
    }

    public void setLastDecayTick(long tick) {
        lastDecayTick = tick;
        markDirty();
    }

    // ---- Persistence ------------------------------------------------------

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound blightNbt = new NbtCompound();
        blight.forEach((key, value) -> blightNbt.putInt(Long.toString(key), value));
        nbt.put("Blight", blightNbt);

        NbtList observatoryList = new NbtList();
        for (BlockPos pos : observatories) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            observatoryList.add(entry);
        }
        nbt.put("Observatories", observatoryList);
        nbt.putBoolean("NoxDefeated", noxDefeated);
        nbt.putLong("LastDecayTick", lastDecayTick);
        return nbt;
    }

    public static AetheriusWorldState fromNbt(NbtCompound nbt) {
        AetheriusWorldState state = new AetheriusWorldState();
        NbtCompound blightNbt = nbt.getCompound("Blight");
        for (String key : blightNbt.getKeys()) {
            try {
                state.blight.put(Long.parseLong(key), blightNbt.getInt(key));
            } catch (NumberFormatException ignored) {
                // Skip corrupt entries rather than failing the whole world load.
            }
        }
        NbtList observatoryList = nbt.getList("Observatories", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < observatoryList.size(); i++) {
            NbtCompound entry = observatoryList.getCompound(i);
            state.observatories.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
        }
        state.noxDefeated = nbt.getBoolean("NoxDefeated");
        state.lastDecayTick = nbt.getLong("LastDecayTick");
        return state;
    }
}
