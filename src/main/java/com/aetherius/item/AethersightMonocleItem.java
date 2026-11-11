package com.aetherius.item;

import com.aetherius.api.chronicle.ChronicleCounters;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Aethersight Monocle - Act II's key item.
 *
 * <p>While worn it reveals Aspect Corruption in the area (client overlay + blight spores),
 * and it is the hard requirement for the Act II trigger. Biome/dimension objectives check
 * for this exact class, so subclasses or addons can extend the check by extending this.</p>
 */
public class AethersightMonocleItem extends ArmorItem {

    public AethersightMonocleItem(ArmorMaterial material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.aetherius.aethersight_monocle.desc").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.aetherius.aethersight_monocle.hint")
                .formatted(Formatting.DARK_PURPLE));
    }

    /**
     * Called by the server when the player equips the monocle in a blighted chunk.
     * Kept as a static hook so the armour tick does not need to import world state.
     */
    public static void onWornInBlight(ServerPlayerEntity player) {
        com.aetherius.api.chronicle.ChronicleManager.get().onBlightWitnessed(player);
        if (!player.getWorld().isClient()) {
            player.sendMessage(Text.translatable("aetherius.monocle.blight_revealed")
                    .formatted(Formatting.LIGHT_PURPLE), true);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    /** Creative/utility helper so tooling can query the requirement string. */
    public static String requirementFlag() {
        return ChronicleCounters.FLAG_ACT2_ATTUNED;
    }

    /** Unused parameter guard for future enchantability hooks. */
    protected void noop(PlayerEntity player) {
        // intentionally empty
    }
}
