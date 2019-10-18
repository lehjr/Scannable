package li.cil.scannable.common;

import li.cil.scannable.api.API;
import li.cil.scannable.common.init.Items;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public final class CreativeTab extends ItemGroup {
    CreativeTab() {
        super(API.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(Items.scanner);
    }
}