package com.aetherius.item;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Starlight Elixir - the consumable that refills Mana.
 *
 * <p>The purified variant also vents Astral Tension (the only stackable way to do so),
 * which makes it the correct reward for Act II purification rites and the correct
 * consumable to hoard before Nox'Thal's third phase.</p>
 */
public class StarlightElixirItem extends Item {

    private final boolean purified;

    public StarlightElixirItem(boolean purified, Settings settings) {
        super(settings);
        this.purified = purified;
    }

    public boolean isPurified() {
        return purified;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity player) {
            PlayerMagicCapability magic = MagicComponents.get(player);
            float mana = purified ? magic.getMaxMana() : 40f;
            magic.addMana(mana);
            if (purified) {
                magic.setAstralTension(Math.max(0f, magic.getAstralTension() - 30f));
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        com.aetherius.registry.ModStatusEffects.STARCLEANSE, 20 * 60, 0));
            }
            com.aetherius.network.AetherNetworking.syncMagic(player, magic);
        }
        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            stack.decrement(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().insertStack(bottle)) {
                player.dropItem(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(purified
                        ? "item.aetherius.purified_starlight_elixir.desc"
                        : "item.aetherius.starlight_elixir.desc")
                .formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return purified;
    }
}
