package com.aetherius.block.entity;

import com.aetherius.api.chronicle.ChronicleCounters;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.block.AetherForgeBlock;
import com.aetherius.multiblock.ModMultiblocks;
import com.aetherius.registry.ModBlockEntities;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Cosmic Forge logic: 3x3x3 validation, wand ascension and weapon infusion.
 *
 * <p>Validation is re-run every 2 seconds from the ticker and, crucially, again at the
 * moment of use. Nothing is cached across chunk reloads, so a player who dismantles the
 * forge cannot keep using a stale "formed" flag.</p>
 */
public class CosmicForgeBlockEntity extends BlockEntity {

    private static final int VALIDATE_INTERVAL = 40;
    private static final String INFUSED_TAG = "AetheriusInfused";

    private boolean formed;
    private int tickCounter;
    private int infusions;

    public CosmicForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COSMIC_FORGE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CosmicForgeBlockEntity forge) {
        if (world == null || world.isClient()) {
            return;
        }
        forge.tickCounter++;
        if (forge.tickCounter % VALIDATE_INTERVAL == 0) {
            forge.refreshFormed();
        }
        if (forge.formed && forge.tickCounter % 10 == 0 && world instanceof ServerWorld serverWorld) {
            Vec3d center = Vec3d.ofCenter(pos).add(0, 0.6, 0);
            serverWorld.spawnParticles(ModParticles.SOL_EMBER, center.x, center.y, center.z,
                    4, 0.6, 0.2, 0.6, 0.02);
        }
    }

    public boolean isFormed() {
        return formed;
    }

    private boolean refreshFormed() {
        boolean now = validateStructure();
        if (now != formed) {
            formed = now;
            if (getWorld() != null && !getWorld().isClient()) {
                getWorld().setBlockState(pos, getCachedState().with(AetherForgeBlock.LIT, now));
            }
        }
        return now;
    }

    public boolean validateStructure() {
        if (getWorld() == null) {
            return false;
        }
        BlockPos origin = pos.subtract(ModMultiblocks.FORGE_CORE_OFFSET);
        return ModMultiblocks.COSMIC_FORGE.match(getWorld(), origin).matched();
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (getWorld() == null || getWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        if (!validateStructure()) {
            var result = ModMultiblocks.COSMIC_FORGE.match(getWorld(),
                    pos.subtract(ModMultiblocks.FORGE_CORE_OFFSET));
            player.sendMessage(Text.translatable("aetherius.forge.incomplete",
                    Text.literal(String.valueOf(result.mismatches().size())).formatted(Formatting.RED))
                    .formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        ChronicleManager.get().onFlag(serverPlayer, ChronicleCounters.FLAG_FORGE_BUILT);
        ItemStack held = player.getStackInHand(hand);
        if (held.isEmpty()) {
            player.sendMessage(Text.translatable("aetherius.forge.awaiting_item").formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        if (!hasAetherBlood(player, 1)) {
            player.sendMessage(Text.translatable("aetherius.forge.needs_blood").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }

        if (held.isOf(ModItems.ASTRAL_WAND)) {
            ItemStack ascended = new ItemStack(ModItems.ECLIPSE_WAND);
            if (held.getNbt() != null) {
                ascended.setNbt(held.getNbt().copy());
            }
            player.setStackInHand(hand, ascended);
            consumeAetherBlood(player, 1);
            player.sendMessage(Text.translatable("aetherius.forge.wand_ascended").formatted(Formatting.LIGHT_PURPLE),
                    false);
        } else if (held.isDamageable()) {
            if (held.getOrCreateNbt().getBoolean(INFUSED_TAG)) {
                player.sendMessage(Text.translatable("aetherius.forge.already_infused")
                        .formatted(Formatting.GRAY), true);
                return ActionResult.CONSUME;
            }
            held.addEnchantment(Enchantments.SHARPNESS, 1 + Math.min(2, infusions / 3));
            held.getOrCreateNbt().putBoolean(INFUSED_TAG, true);
            consumeAetherBlood(player, 1);
            infusions++;
            player.sendMessage(Text.translatable("aetherius.forge.infused").formatted(Formatting.GOLD), false);
        } else {
            player.sendMessage(Text.translatable("aetherius.forge.invalid_item").formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        if (getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, ModSounds.FORGE_STRIKE, SoundCategory.BLOCKS, 1.2f, 0.9f);
            serverWorld.spawnParticles(ModParticles.ASTRAL_SPARK, pos.getX() + 0.5, pos.getY() + 1.2,
                    pos.getZ() + 0.5, 80, 1.2, 1.2, 1.2, 0.2);
        }
        markDirty();
        return ActionResult.CONSUME;
    }

    private static boolean hasAetherBlood(PlayerEntity player, int amount) {
        return player.getInventory().count(ModItems.AETHER_BLOOD) >= amount || player.getAbilities().creativeMode;
    }

    private static void consumeAetherBlood(PlayerEntity player, int amount) {
        if (player.getAbilities().creativeMode) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().size() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.AETHER_BLOOD)) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Formed", formed);
        nbt.putInt("Infusions", infusions);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        formed = nbt.getBoolean("Formed");
        infusions = nbt.getInt("Infusions");
    }
}
