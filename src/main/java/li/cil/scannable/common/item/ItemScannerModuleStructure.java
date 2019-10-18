package li.cil.scannable.common.item;

import li.cil.scannable.common.capabilities.CapabilityProviderModuleStructure;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.init.Items;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

public final class ItemScannerModuleStructure extends AbstractItemScannerModule {
    private static final String TAG_HIDE_EXPLORED = "hideExplored";

    public ItemScannerModuleStructure(String registryName) {
        super(registryName);
    }

    public static boolean hideExplored(final ItemStack stack) {
        if (!Items.isModuleStructure(stack)) {
            return false;
        }


        final CompoundNBT nbt = stack.getTag();
        return nbt != null && nbt.getBoolean(TAG_HIDE_EXPLORED);
    }

    private static void setHideExplored(final ItemStack stack, final boolean hideExplored) {
        final CompoundNBT nbt;
        if (!stack.hasTag()) {
            stack.setTag(nbt = new CompoundNBT());
        } else {
            nbt = stack.getTag();
        }

        assert nbt != null;

        nbt.putBoolean(TAG_HIDE_EXPLORED, hideExplored);
    }

    // --------------------------------------------------------------------- //
    // Item

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundNBT nbt) {
        return CapabilityProviderModuleStructure.INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_STRUCTURE));
        if (hideExplored(stack)) {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_STRUCTURE_HIDE_EXPLORED));
        } else {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_STRUCTURE_SHOW_EXPLORED));
        }
        super.addInformation(stack, world, tooltip, flag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, final Hand hand) {
        final ItemStack stack = player.getHeldItem(hand);

        if (!player.isSneaking()) {
            return new ActionResult<>(ActionResultType.PASS, stack);
        }

        setHideExplored(stack, !hideExplored(stack));

        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }
}
