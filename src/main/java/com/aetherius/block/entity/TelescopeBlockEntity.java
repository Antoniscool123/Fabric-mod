package com.aetherius.block.entity;

import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.Rune;
import com.aetherius.registry.ModBlockEntities;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Telescope of the Ancients.
 *
 * <p>Broken on worldgen, repairable with 4 Aether Shards + 1 Aether Blood. Once repaired it
 * hands out one tier-III rune per in-game day, chosen by the real-time moon phase - the
 * classic "come back on a full moon" progression hook, but with actual mechanical teeth
 * because each phase teaches a different school's ultimate rune.</p>
 */
public class TelescopeBlockEntity extends BlockEntity {

    /** Moon phase (0-7) -> tier-III rune. Slot order matches {@code World.getMoonPhase()}. */
    private static final Rune[] PHASE_RUNES = {
            Rune.SOLARIS, Rune.CAELUS, Rune.LUNARIS, Rune.TERRANOX,
            Rune.ECLIPSE, Rune.SOLARIS, Rune.LUNARIS, Rune.CAELUS
    };

    private boolean repaired;
    private long lastOfferingDay = -1L;
    private int tickCounter;

    public TelescopeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELESCOPE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TelescopeBlockEntity telescope) {
        if (world == null || world.isClient()) {
            return;
        }
        telescope.tickCounter++;
        if (telescope.repaired && telescope.tickCounter % 120 == 0 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.STAR_DUST,
                    pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 8, 0.6, 0.6, 0.6, 0.01);
        }
    }

    public boolean isRepaired() {
        return repaired;
    }

    public ActionResult interact(PlayerEntity player) {
        if (getWorld() == null || getWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        if (!repaired) {
            return attemptRepair(serverPlayer);
        }
        return offerRune(serverPlayer);
    }

    private ActionResult attemptRepair(ServerPlayerEntity player) {
        boolean creative = player.getAbilities().creativeMode;
        int shards = player.getInventory().count(ModItems.AETHER_SHARD);
        int blood = player.getInventory().count(ModItems.AETHER_BLOOD);
        if (!creative && (shards < 4 || blood < 1)) {
            player.sendMessage(Text.translatable("aetherius.telescope.repair_requirements")
                    .formatted(Formatting.GRAY), false);
            return ActionResult.CONSUME;
        }
        if (!creative) {
            consume(player, ModItems.AETHER_SHARD, 4);
            consume(player, ModItems.AETHER_BLOOD, 1);
        }
        repaired = true;
        markDirty();
        ChronicleManager.get().onObservatoryRepaired(player);
        if (getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, ModSounds.OBSERVATORY_HUM, SoundCategory.BLOCKS, 1.2f, 1.0f);
            serverWorld.spawnParticles(ModParticles.ASTRAL_SPARK,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 90, 1.5, 1.5, 1.5, 0.2);
        }
        player.sendMessage(Text.translatable("aetherius.telescope.repaired").formatted(Formatting.AQUA), false);
        return ActionResult.CONSUME;
    }

    private ActionResult offerRune(ServerPlayerEntity player) {
        if (getWorld() == null) {
            return ActionResult.CONSUME;
        }
        long day = getWorld().getTimeOfDay() / 24000L;
        if (lastOfferingDay == day) {
            player.sendMessage(Text.translatable("aetherius.telescope.wait_for_moon")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }
        int phase = Math.floorMod(getWorld().getMoonPhase(), PHASE_RUNES.length);
        Rune rune = PHASE_RUNES[phase];
        var magic = MagicComponents.get(player);
        if (!magic.unlockRune(rune)) {
            player.sendMessage(Text.translatable("aetherius.telescope.already_known",
                    Text.translatable(rune.translationKey())).formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }
        lastOfferingDay = day;
        markDirty();
        player.sendMessage(Text.translatable("aetherius.telescope.rune_granted",
                        Text.translatable(rune.translationKey()).formatted(Formatting.LIGHT_PURPLE))
                .formatted(Formatting.WHITE), false);
        com.aetherius.network.AetherNetworking.syncMagic(player, magic);
        if (getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, ModSounds.CHRONICLE_UPDATE, SoundCategory.BLOCKS, 1.0f, 1.3f);
        }
        return ActionResult.CONSUME;
    }

    private static void consume(ServerPlayerEntity player, net.minecraft.item.Item item, int amount) {
        for (int slot = 0; slot < player.getInventory().size() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Repaired", repaired);
        nbt.putLong("LastOfferingDay", lastOfferingDay);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        repaired = nbt.getBoolean("Repaired");
        lastOfferingDay = nbt.getLong("LastOfferingDay");
    }
}
