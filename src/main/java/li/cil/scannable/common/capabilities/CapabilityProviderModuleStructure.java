package li.cil.scannable.common.capabilities;

import li.cil.scannable.client.scanning.ScanResultProviderStructure;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum CapabilityProviderModuleStructure implements ICapabilityProvider {
    INSTANCE;

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CapabilityScanResultProvider.SCAN_RESULT_PROVIDER_CAPABILITY.orEmpty(cap, LazyOptional.of(()->ScanResultProviderStructure.INSTANCE));
    }
}