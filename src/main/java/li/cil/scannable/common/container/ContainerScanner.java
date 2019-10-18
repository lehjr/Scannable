package li.cil.scannable.common.container;

import li.cil.scannable.common.init.Items;
import li.cil.scannable.common.inventory.ItemHandlerScanner;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public final class ContainerScanner extends Container {
    PlayerInventory playerInventory;
    private final Hand hand;

    // --------------------------------------------------------------------- //

    public ContainerScanner(int windowId, final PlayerInventory playerInventory, final Hand hand) {
        super(ContainerTypes.SCANNER_CONTAINER_TYPE, windowId);
        this.playerInventory = playerInventory;
        this.hand = hand;

        playerInventory.player.getHeldItem(hand).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(itemHandler->{
            if (itemHandler instanceof ItemHandlerScanner) {
                final IItemHandler activeModules = ((ItemHandlerScanner) itemHandler).getActiveModules();
                for (int slot = 0; slot < activeModules.getSlots(); ++slot) {
                    addSlot(new SlotItemHandler(activeModules, slot, 62 + slot * 18, 20));
                }

                final IItemHandler storedModules = ((ItemHandlerScanner) itemHandler).getInactiveModules();
                for (int slot = 0; slot < storedModules.getSlots(); ++slot) {
                    addSlot(new SlotItemHandler(storedModules, slot, 62 + slot * 18, 46));
                }
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, row * 18 + 77));
            }
        }

        for (int slot = 0; slot < 9; ++slot) {
            addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 135));
        }
    }



    public PlayerEntity getPlayer() {
        return playerInventory.player;
    }

    public Hand getHand() {
        return hand;
    }

    // --------------------------------------------------------------------- //
    // Container

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        return player == this.playerInventory.player && Items.isScanner(player.getHeldItem(hand));
    }

    @Override
    public ItemStack transferStackInSlot(final PlayerEntity player, final int index) {
        final Slot from = inventorySlots.get(index);
        if (from == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = from.getStack().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = from.inventory != player.inventory;
        final ItemStack fromStack = from.getStack();

        final int step, begin;
        if (intoPlayerInventory) {
            step = -1;
            begin = inventorySlots.size() - 1;
        } else {
            step = 1;
            begin = 0;
        }

        if (fromStack.getMaxStackSize() > 1) {
            for (int i = begin; i >= 0 && i < inventorySlots.size(); i += step) {
                final Slot into = inventorySlots.get(i);
                if (into.inventory == from.inventory) {
                    continue;
                }

                final ItemStack intoStack = into.getStack();
                if (intoStack.isEmpty()) {
                    continue;
                }

                final boolean itemsAreEqual = fromStack.isItemEqual(intoStack) && ItemStack.areItemStackTagsEqual(fromStack, intoStack);
                if (!itemsAreEqual) {
                    continue;
                }

                final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getItemStackLimit(stack));
                final int spaceInSlot = maxSizeInSlot - intoStack.getCount();
                if (spaceInSlot <= 0) {
                    continue;
                }

                final int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
                if (itemsMoved <= 0) {
                    continue;
                }

                intoStack.grow(from.decrStackSize(itemsMoved).getCount());
                into.onSlotChanged();

                if (from.getStack().isEmpty()) {
                    break;
                }
            }
        }

        for (int i = begin; i >= 0 && i < inventorySlots.size(); i += step) {
            if (from.getStack().isEmpty()) {
                break;
            }

            final Slot into = inventorySlots.get(i);
            if (into.inventory == from.inventory) {
                continue;
            }

            if (into.getHasStack()) {
                continue;
            }

            if (!into.isItemValid(fromStack)) {
                continue;
            }

            final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getItemStackLimit(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.putStack(from.decrStackSize(itemsMoved));
        }

        return from.getStack().getCount() < stack.getCount() ? from.getStack() : ItemStack.EMPTY;
    }
}
