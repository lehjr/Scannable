package li.cil.scannable.common.item;

import li.cil.scannable.common.capabilities.CapabilityProviderModuleBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

abstract class AbstractItemScannerModuleBlock extends AbstractItemScannerModule {
    AbstractItemScannerModuleBlock(String registryName) {
        super(registryName);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundNBT nbt) {
        return CapabilityProviderModuleBlock.INSTANCE;
    }
}
