package li.cil.scannable.common.item;

import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.init.Items;
import li.cil.scannable.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public final class ItemScannerModuleBlockConfigurable extends AbstractItemScannerModuleBlock {
    private static final String TAG_BLOCK = "block";
    private static final String TAG_METADATA = "meta";

    public ItemScannerModuleBlockConfigurable(String registryName) {
        super(registryName);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static BlockState getBlockState(final ItemStack stack) {
        if (!Items.isModuleBlock(stack)) {
            return null;
        }

        final CompoundNBT nbt = stack.getOrCreateTag();

        if (nbt == null || !nbt.contains(TAG_BLOCK, NBT.TAG_STRING)) {
            return null;
        }

        final ResourceLocation blockName = new ResourceLocation(nbt.getString(TAG_BLOCK));
        final Block block = ForgeRegistries.BLOCKS.getValue(blockName);
        if (block == null || block == Blocks.AIR) {
            return null;
        }

        return NBTUtil.readBlockState(nbt);
    }

    private static void setBlockState(final ItemStack stack, final BlockState state) {
        final CompoundNBT nbt;
        if (!stack.hasTag()) {
            stack.setTag(nbt = new CompoundNBT());
        } else {
            nbt = stack.getTag();
        }

        assert nbt != null;

        final ResourceLocation blockName = state.getBlock().getRegistryName();
        if (blockName == null) {
            return;
        }

        nbt.putString(TAG_BLOCK, blockName.toString());
        NBTUtil.writeBlockState(state);
    }

    // --------------------------------------------------------------------- //
    // Item

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World worldIn, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        final BlockState state = getBlockState(stack);
        if (state == null) {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_BLOCK));
        } else {
            final ItemStack blockStack = BlockUtils.getItemStackFromState(state, worldIn);
            if (!blockStack.isEmpty()) {
                tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_BLOCK_NAME, blockStack.getDisplayName()));
            } else {
                tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_BLOCK_NAME, state.getBlock().getNameTextComponent()));
            }
        }
        super.addInformation(stack, worldIn, tooltip, flag);
    }


    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IWorldReader world, BlockPos pos, PlayerEntity player) {
        return false;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();


        if (!world.isBlockLoaded(pos)) {
            return ActionResultType.PASS;
        }
        if (world.isAirBlock(pos)) {
            return ActionResultType.PASS;
        }

        final ItemStack stack = player.getHeldItem(hand);
        final BlockState state = world.getBlockState(pos);

        if (CommonConfig.blockBlacklist.get().contains(state.getBlock())) {
            if (world.isRemote) {
                Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TranslationTextComponent(Constants.MESSAGE_BLOCK_BLACKLISTED), Constants.CHAT_LINE_ID);
            }
            player.getCooldownTracker().setCooldown(this, 10);
            return ActionResultType.SUCCESS;
        }

        setBlockState(stack, state);

        return ActionResultType.SUCCESS;
    }
}
