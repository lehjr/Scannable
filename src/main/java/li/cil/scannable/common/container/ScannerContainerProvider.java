package li.cil.scannable.common.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * @author lehjr
 */
public class ScannerContainerProvider implements INamedContainerProvider {
    Hand hand;
    public ScannerContainerProvider(Hand handIn) {
        hand = handIn;
    }

    @Override
    public ITextComponent getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public Container createMenu(int windowID, PlayerInventory playerInventory, PlayerEntity player) {
        return new ContainerScanner(windowID, playerInventory, hand);
    }
}