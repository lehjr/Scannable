package li.cil.scannable.common.item;

import net.minecraft.item.Item;

public final class ItemScannerModuleBlank extends Item {
    public ItemScannerModuleBlank(String registryName) {
        super(new Properties());
        setRegistryName(registryName);
    }
}
