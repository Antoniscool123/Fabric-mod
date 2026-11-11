package com.aetherius.item;

import com.aetherius.api.magic.CastResult;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.RuneCombination;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.network.CastSpellC2SPacket;
import com.aetherius.network.InscribeRuneC2SPacket;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Rune wand - the physical manifestation of the Rune-Combination System.
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li><b>Right click</b>: cast the inscribed sequence from this hand.</li>
 *   <li><b>Sneak + right click</b>: clear the sequence.</li>
 *   <li>Runes are appended through the Chronicle/rune UI ({@link InscribeRuneC2SPacket}).</li>
 * </ul>
 *
 * <h2>Data</h2>
 * The sequence is stored as an ordered {@code Runes} string list in the stack NBT, which
 * keeps wands copyable, anvil-safe and readable by the Astrolabe's preview logic.
 */
public class WandItem extends Item {

    public static final String RUNES_KEY = "Runes";

    /** Wand tiers differ in cast cadence and Tension forgiveness, not in rune count. */
    public enum Tier {
        ASTRAL("astral", 0, 1.0f, Formatting.AQUA),
        ECLIPSE("eclipse", 2, 0.85f, Formatting.DARK_PURPLE);

        private final String id;
        private final int cooldownReductionTicks;
        private final float tensionModifier;
        private final Formatting color;

        Tier(String id, int cooldownReductionTicks, float tensionModifier, Formatting color) {
            this.id = id;
            this.cooldownReductionTicks = cooldownReductionTicks;
            this.tensionModifier = tensionModifier;
            this.color = color;
        }
    }

    private final Tier tier;

    public WandItem(Tier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    // ---- Sequence storage -------------------------------------------------

    public RuneCombination getRunes(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? RuneCombination.EMPTY : RuneCombination.readNbt(nbt, RUNES_KEY);
    }

    public RuneCombination inscribe(ItemStack stack, Rune rune) {
        RuneCombination next = getRunes(stack).append(rune);
        NbtCompound nbt = stack.getOrCreateNbt();
        next.writeNbt(nbt, RUNES_KEY);
        return next;
    }

    public void clearRunes(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.remove(RUNES_KEY);
    }

    // ---- Interaction ------------------------------------------------------

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking()) {
            if (world.isClient()) {
                InscribeRuneC2SPacket.clear();
            }
            return TypedActionResult.success(stack, world.isClient());
        }
        RuneCombination combination = getRunes(stack);
        if (combination.isEmpty()) {
            if (world.isClient()) {
                user.sendMessage(Text.translatable("aetherius.wand.empty").formatted(Formatting.GRAY), true);
            }
            return TypedActionResult.fail(stack);
        }
        if (world.isClient()) {
            CastSpellC2SPacket.send(combination, hand);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    /** Server-side post-cast bookkeeping (durability, sounds, wand-tier bonuses). */
    public void onSuccessfulCast(ServerPlayerEntity player, ItemStack stack, CastResult result,
                                 RuneCombination combination) {
        if (tier == Tier.ECLIPSE && player.getRandom().nextFloat() < 0.08f && stack.isDamageable()) {
            // The Eclipse wand eats a little of itself on casts; the callback is a no-op because
            // the break animation is client-driven through the item cooldown/attribute pipeline.
            stack.damage(1, player, broken -> {
            });
            player.getWorld().playSound(null, player.getBlockPos(),
                    net.minecraft.sound.SoundEvents.ENTITY_ITEM_BREAK,
                    net.minecraft.sound.SoundCategory.PLAYERS, 0.4f,
                    0.8f + player.getRandom().nextFloat() * 0.4f);
        }
    }

    // ---- Tooltip ----------------------------------------------------------

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        RuneCombination combination = getRunes(stack);
        tooltip.add(Text.translatable("item.aetherius.wand.tier." + tier.id).formatted(tier.color));
        tooltip.add(Text.literal("" + Formatting.DARK_GRAY) .append(
                Text.translatable("item.aetherius.wand.inscription", combination.displayString())
                        .formatted(Formatting.GRAY)));
        if (!combination.isEmpty()) {
            AbstractSpell spell = SpellRegistry.resolve(combination);
            if (spell != null) {
                tooltip.add(Text.translatable("item.aetherius.wand.preview",
                        Text.translatable(spell.translationKey()).formatted(Formatting.AQUA))
                        .formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.translatable(spell.descriptionKey()).formatted(Formatting.DARK_GRAY));
            }
        }
        tooltip.add(Text.translatable("item.aetherius.wand.controls").formatted(Formatting.DARK_GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
