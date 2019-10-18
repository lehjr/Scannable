package li.cil.scannable.common.item;

import li.cil.scannable.api.API;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.config.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractItemScannerModule extends Item {
    AbstractItemScannerModule(String registryName) {
        super(new Properties().maxStackSize(1).group(API.creativeTab));
        setRegistryName(registryName);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        if (!CommonConfig.useEnergy.get()) {
            return;
        }

        if (stack.isEmpty()) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }

        final int cost = ItemScanner.getModuleEnergyCost(mc.player, stack);
        if (cost <= 0) {
            return;
        }

        tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_MODULE_ENERGY_COST, cost));
    }
}
