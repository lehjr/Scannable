package li.cil.scannable.common.item;

import li.cil.scannable.common.config.Constants;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public final class ItemScannerModuleAnimal extends AbstractItemScannerModuleEntity {
    public ItemScannerModuleAnimal(String registryName) {
        super(registryName);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_ANIMAL));
        super.addInformation(stack, world, tooltip, flag);
    }
}
