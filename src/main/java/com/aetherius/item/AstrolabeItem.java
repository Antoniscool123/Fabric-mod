package com.aetherius.item;

import com.aetherius.world.AetheriusWorldState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Astrolabe of the Void - Act I's milestone item.
 *
 * <p>Queries {@link AetheriusWorldState} for the nearest observatory that worldgen has
 * recorded, then reports bearing and distance. In the Shattered Sky it instead points at
 * the nearest active Sky-Rift, which doubles as a navigational aid in Phase 3 of the boss
 * fight when the screen is dark.</p>
 */
public class AstrolabeItem extends Item {

    private static final int COOLDOWN_TICKS = 200;

    public AstrolabeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack, world.isClient());
        }
        ServerWorld serverWorld = player.getServerWorld();
        AetheriusWorldState state = AetheriusWorldState.get(serverWorld);

        Optional<BlockPos> target = state.findNearestObservatory(player.getBlockPos());
        if (target.isEmpty()) {
            player.sendMessage(Text.translatable("aetherius.astrolabe.no_signal").formatted(Formatting.GRAY), false);
            return TypedActionResult.consume(stack);
        }

        BlockPos pos = target.get();
        double distance = Math.sqrt(pos.getSquaredDistance(player.getBlockPos()));
        int bearing = (int) Math.toDegrees(Math.atan2(pos.getZ() - player.getZ(), pos.getX() - player.getX()));
        player.sendMessage(Text.translatable("aetherius.astrolabe.ping",
                Text.literal("" + (int) distance).formatted(Formatting.AQUA),
                Text.literal(bearing + "\u00B0").formatted(Formatting.GOLD),
                Text.literal(pos.getX() + ", " + pos.getZ()).formatted(Formatting.DARK_GRAY)
        ).formatted(Formatting.WHITE), false);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.aetherius.astrolabe.desc").formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
