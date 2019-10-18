package li.cil.scannable.common.item;

import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.init.Items;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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

public final class ItemScannerModuleEntity extends AbstractItemScannerModuleEntity {
    private static final String TAG_ENTITY = "entity";

    public ItemScannerModuleEntity(String registryName) {
        super(registryName);
    }

    @Nullable
    public static String getEntity(final ItemStack stack) {
        if (!Items.isModuleEntity(stack)) {
            return null;
        }

        final CompoundNBT nbt = stack.getTag();
        if (nbt == null || !nbt.contains(TAG_ENTITY, NBT.TAG_STRING)) {
            return null;
        }

        return nbt.getString(TAG_ENTITY);
    }

    private static void setEntity(final ItemStack stack, final String entity) {
        final CompoundNBT nbt;
        if (!stack.hasTag()) {
            stack.setTag(nbt = new CompoundNBT());
        } else {
            nbt = stack.getTag();
        }

        assert nbt != null;

        nbt.putString(TAG_ENTITY, entity);
    }

    // --------------------------------------------------------------------- //
    // Item

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        final String entity = getEntity(stack);
        if (entity == null) {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_ENTITY));
        } else {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_ENTITY_NAME, entity));
        }
        super.addInformation(stack, world, tooltip, flag);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IWorldReader world, BlockPos pos, PlayerEntity player) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(final ItemStack oldStack, final ItemStack newStack, final boolean slotChanged) {
        if (!slotChanged && Items.isModuleEntity(oldStack) && Items.isModuleEntity(newStack)) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }

        final ResourceLocation entity = ForgeRegistries.ENTITIES.getKey(target.getType());
        if (entity == null) {
            return false;
        }

        // NOT stack, because that's a copy in creative mode.
        setEntity(playerIn.getHeldItem(hand), entity.toString());
        playerIn.swingArm(hand);
        playerIn.inventory.markDirty();
        return true;
    }
}
