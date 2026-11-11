package com.aetherius.block.entity;

import com.aetherius.api.chronicle.ChronicleCounters;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.registry.ModBlockEntities;
import com.aetherius.registry.ModBlocks;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sky-Rift anchor logic.
 *
 * <p>Each anchor holds exactly one Elemental Heart. When four anchors within 12 blocks hold
 * four <i>distinct</i> hearts, the portal ignites in the middle of the ring. The anchor that
 * completes the set is the one that performs the ignition, which means the last heart
 * socketed is always the dramatic beat.</p>
 */
public class SkyRiftAnchorBlockEntity extends BlockEntity {

    private static final int SEARCH_RADIUS = 12;
    private static final Map<Item, String> HEART_IDS = new HashMap<>();
    private static final Set<String> ALL_HEARTS = Set.of("sol", "luna", "terra", "caelum");

    static {
        HEART_IDS.put(ModItems.HEART_OF_SOL, "sol");
        HEART_IDS.put(ModItems.HEART_OF_LUNA, "luna");
        HEART_IDS.put(ModItems.HEART_OF_TERRA, "terra");
        HEART_IDS.put(ModItems.HEART_OF_CAELUM, "caelum");
    }

    private String heartId = "";

    public SkyRiftAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKY_RIFT_ANCHOR, pos, state);
    }

    public boolean hasHeart() {
        return !heartId.isEmpty();
    }

    public String heartId() {
        return heartId;
    }

    public boolean insertHeart(ItemStack stack, PlayerEntity player) {
        String id = HEART_IDS.get(stack.getItem());
        if (id == null || getWorld() == null || getWorld().isClient()) {
            return false;
        }
        heartId = id;
        markDirty();
        if (getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, ModSounds.SPELL_RESONANT, SoundCategory.BLOCKS, 1.0f, 1.2f);
            serverWorld.spawnParticles(ModParticles.ASTRAL_SPARK,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.8, 0.8, 0.8, 0.15);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable("aetherius.anchor.socketed",
                    Text.translatable("item.aetherius.heart_of_" + id).formatted(Formatting.LIGHT_PURPLE)),
                    false);
            tryOpenPortal(serverPlayer);
        }
        return true;
    }

    private void tryOpenPortal(ServerPlayerEntity player) {
        if (getWorld() == null) {
            return;
        }
        Map<BlockPos, String> anchors = new HashMap<>();
        for (BlockPos pos2 : BlockPos.iterateOutwards(pos, SEARCH_RADIUS, 8, SEARCH_RADIUS)) {
            if (!getWorld().getBlockState(pos2).isOf(ModBlocks.SKY_RIFT_ANCHOR)) {
                continue;
            }
            if (getWorld().getBlockEntity(pos2) instanceof SkyRiftAnchorBlockEntity other && other.hasHeart()) {
                anchors.put(pos2.toImmutable(), other.heartId);
            }
        }
        anchors.put(pos, heartId);
        if (!new HashSet<>(anchors.values()).containsAll(ALL_HEARTS)) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int sumY = 0;
        for (BlockPos anchorPos : anchors.keySet()) {
            minX = Math.min(minX, anchorPos.getX());
            maxX = Math.max(maxX, anchorPos.getX());
            minZ = Math.min(minZ, anchorPos.getZ());
            maxZ = Math.max(maxZ, anchorPos.getZ());
            sumY += anchorPos.getY();
        }
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int centerY = sumY / anchors.size();

        boolean alongX = (maxX - minX) >= (maxZ - minZ);
        int placed = 0;
        for (int offset = -1; offset <= 1; offset++) {
            for (int height = 0; height < 4; height++) {
                BlockPos portalPos = alongX
                        ? new BlockPos(centerX, centerY + height, centerZ + offset)
                        : new BlockPos(centerX + offset, centerY + height, centerZ);
                if (getWorld().getBlockState(portalPos).isAir()
                        || getWorld().getBlockState(portalPos).isOf(ModBlocks.SKY_RIFT_PORTAL)) {
                    getWorld().setBlockState(portalPos, ModBlocks.SKY_RIFT_PORTAL.getDefaultState());
                    placed++;
                }
            }
        }
        if (placed > 0 && getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, ModSounds.SPELL_NOVA_WARP, SoundCategory.BLOCKS, 2.0f, 0.7f);
            serverWorld.spawnParticles(ModParticles.VOID_MOTE,
                    centerX + 0.5, centerY + 2, centerZ + 0.5, 200, 3.0, 3.0, 3.0, 0.3);
        }
        ChronicleManager.get().onFlag(player, ChronicleCounters.FLAG_SKY_RIFT_OPEN);
        player.sendMessage(Text.translatable("aetherius.anchor.rift_open").formatted(Formatting.AQUA), false);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Heart", heartId);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        heartId = nbt.getString("Heart");
    }
}
