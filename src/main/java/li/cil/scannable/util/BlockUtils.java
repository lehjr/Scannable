package li.cil.scannable.util;

import li.cil.scannable.common.Scannable;
import li.cil.scannable.common.config.ClientConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BlockUtils {
    @SuppressWarnings("deprecation")
    public static ItemStack getItemStackFromState(final BlockState state, @Nullable final World world) {
        final Block block = state.getBlock();
        try {
            ItemStack stack = block.getPickBlock(state, null, world, BlockPos.ZERO, null);
            if (stack == null) {
                Scannable.getLog().warn("Some mod returns null from Block.getPickBlock, should return ItemStack.EMPTY at this point. Block in question: {}", block.toString());
                return ItemStack.EMPTY;
            }
            return stack;
        } catch (final Throwable t) {
            try {
                final Item item = Item.getItemFromBlock(block);
                return new ItemStack(item, 1);
            } catch (final Throwable t2) {
                if (ClientConfig.logBlockDropLookupFailures.get() && loggedWarningFor.add(state)) {
                    // Log twice to get both stack traces. Don't log first trace if second lookup succeeds.
                    Scannable.getLog().debug("Failed determining dropped block for " + state.toString() + " via getPickBlock, trying to resolve via meta.", t);
                    Scannable.getLog().debug("Failed determining dropped block for " + state.toString() + " via meta.", t2);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // --------------------------------------------------------------------- //

    private static final Set<BlockState> loggedWarningFor = Collections.synchronizedSet(new HashSet<>());

    // --------------------------------------------------------------------- //

    private BlockUtils() {
    }
}
