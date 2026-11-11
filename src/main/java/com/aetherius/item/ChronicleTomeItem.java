package com.aetherius.item;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.network.ChronicleSyncS2CPacket;
import com.aetherius.network.OpenChronicleS2CPacket;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Chronicle Tome: opens the quest/lore GUI. Awarded by the first Act I quest. */
public class ChronicleTomeItem extends Item {

    public ChronicleTomeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            // The server owns this: it answers with OPEN_CHRONICLE + a fresh snapshot. Sending
            // our own "open" request here only duplicated that round trip.
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity player) {
            OpenChronicleS2CPacket.send(player);
            ChronicleSyncS2CPacket.send(player, MagicComponents.get(player));
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.aetherius.chronicle_tome.desc").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.aetherius.chronicle_tome.hint").formatted(Formatting.DARK_AQUA));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
