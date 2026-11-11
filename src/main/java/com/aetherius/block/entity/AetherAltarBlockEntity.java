package com.aetherius.block.entity;

import com.aetherius.api.chronicle.ChronicleCounters;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.multiblock.ModMultiblocks;
import com.aetherius.registry.ModBlockEntities;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import com.aetherius.world.blight.CelestialBlightManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
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
import net.minecraft.world.World;

/**
 * Aetherial Altar of Purification logic.
 *
 * <p>A rite consumes one Purified Starlight Elixir (or two Aether Blood) and purges Celestial
 * Blight from every chunk within 48 blocks, reverting Star-Ash to Starlit Grass. Rites are
 * limited by a 5-minute per-altar cooldown so purification is a progression beat rather than
 * a spamable eraser.</p>
 */
public class AetherAltarBlockEntity extends BlockEntity {

    private static final int PURIFY_RADIUS = 48;
    private static final int RITE_COOLDOWN_TICKS = 20 * 300;

    private boolean formed;
    private long lastRiteTime = -1L;
    private int tickCounter;
    private int totalPurifications;

    public AetherAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_ALTAR, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, AetherAltarBlockEntity altar) {
        if (world == null || world.isClient()) {
            return;
        }
        altar.tickCounter++;
        if (altar.tickCounter % 40 == 0) {
            altar.formed = altar.validateStructure();
        }
        if (altar.formed && altar.tickCounter % 8 == 0 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.STAR_DUST,
                    pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5,
                    6, 1.6, 1.6, 1.6, 0.01);
        }
    }

    public boolean isFormed() {
        return formed;
    }

    public boolean validateStructure() {
        if (getWorld() == null) {
            return false;
        }
        return ModMultiblocks.AETHER_ALTAR.match(getWorld(),
                pos.subtract(ModMultiblocks.ALTAR_CORE_OFFSET)).matched();
    }

    public ActionResult performRite(PlayerEntity player, Hand hand) {
        if (getWorld() == null || getWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        if (!validateStructure()) {
            player.sendMessage(Text.translatable("aetherius.altar.incomplete").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }
        ChronicleManager.get().onFlag(serverPlayer, ChronicleCounters.FLAG_ALTAR_BUILT);

        long now = getWorld().getTime();
        if (lastRiteTime > 0 && now - lastRiteTime < RITE_COOLDOWN_TICKS) {
            long remaining = (RITE_COOLDOWN_TICKS - (now - lastRiteTime)) / 20L;
            player.sendMessage(Text.translatable("aetherius.altar.cooldown",
                    Text.literal(remaining + "s").formatted(Formatting.GOLD)).formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        ItemStack held = player.getStackInHand(hand);
        boolean hasElixir = held.isOf(ModItems.PURIFIED_STARLIGHT_ELIXIR);
        int bloodNeeded = hasElixir ? 0 : 2;
        boolean hasBlood = player.getInventory().count(ModItems.AETHER_BLOOD) >= bloodNeeded;
        if (!hasElixir && !hasBlood && !player.getAbilities().creativeMode) {
            player.sendMessage(Text.translatable("aetherius.altar.needs_offering").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) {
            if (hasElixir) {
                held.decrement(1);
            } else {
                consumeBlood(player, bloodNeeded);
            }
        }

        ServerWorld world = serverPlayer.getServerWorld();
        int purged = CelestialBlightManager.get().purify(world, pos, PURIFY_RADIUS);
        lastRiteTime = now;
        totalPurifications++;
        ChronicleManager.get().onPurification(serverPlayer);

        world.playSound(null, pos, ModSounds.ALTAR_PURIFY, SoundCategory.BLOCKS, 1.4f, 1.0f);
        world.spawnParticles(ModParticles.STAR_DUST, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                260, 6.0, 4.0, 6.0, 0.25);
        serverPlayer.addStatusEffect(new StatusEffectInstance(
                com.aetherius.registry.ModStatusEffects.STARCLEANSE, 20 * 60 * 5, 0));
        player.sendMessage(Text.translatable("aetherius.altar.rite_complete",
                Text.literal(String.valueOf(purged)).formatted(Formatting.AQUA)).formatted(Formatting.WHITE), false);
        markDirty();
        return ActionResult.CONSUME;
    }

    private static void consumeBlood(PlayerEntity player, int amount) {
        for (int slot = 0; slot < player.getInventory().size() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.AETHER_BLOOD)) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
            }
        }
    }

    public void onControllerBroken() {
        if (getWorld() != null && !getWorld().isClient()) {
            getWorld().playSound(null, pos, ModSounds.SPELL_NOX, SoundCategory.BLOCKS, 0.8f, 0.6f);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Formed", formed);
        nbt.putLong("LastRite", lastRiteTime);
        nbt.putInt("Purifications", totalPurifications);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        formed = nbt.getBoolean("Formed");
        lastRiteTime = nbt.getLong("LastRite");
        totalPurifications = nbt.getInt("Purifications");
    }
}
