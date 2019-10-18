package li.cil.scannable.common.capabilities;

import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.energy.EnergyStorageScanner;
import li.cil.scannable.common.inventory.ItemHandlerScanner;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CapabilityProviderItemScanner implements ICapabilityProvider {
    private final ItemHandlerScanner itemHandler;
    private final EnergyStorageScanner energyStorage;

    // --------------------------------------------------------------------- //

    public CapabilityProviderItemScanner(final ItemStack container) {
        itemHandler = new ItemHandlerScanner(container);
        energyStorage = new EnergyStorageScanner(container);
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            itemHandler.updateFromNBT();
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(cap, LazyOptional.of(()->itemHandler));
        }

        if (CommonConfig.useEnergy.get() && cap == CapabilityEnergy.ENERGY) {
            energyStorage.updateFromNBT();
            return CapabilityEnergy.ENERGY.orEmpty(cap, LazyOptional.of(()->energyStorage));
        }

        return LazyOptional.empty();
    }
}
