package com.aetherius.item;

import com.aetherius.network.ChronicleActionC2SPacket;
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
            ChronicleActionC2SPacket.send(ChronicleActionC2SPacket.ACTION_OPEN, "");
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity player) {
            OpenChronicleS2CPacket.send(player);
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
