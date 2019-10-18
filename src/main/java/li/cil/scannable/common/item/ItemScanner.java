package li.cil.scannable.common.item;

import li.cil.scannable.api.API;
import li.cil.scannable.client.ScanManager;
import li.cil.scannable.common.capabilities.CapabilityProviderItemScanner;
import li.cil.scannable.common.capabilities.CapabilityScanResultProvider;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.container.ScannerContainerProvider;
import li.cil.scannable.common.init.Items;
import li.cil.scannable.common.inventory.ItemHandlerScanner;
import li.cil.scannable.util.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public final class ItemScanner extends Item {
    public ItemScanner(String registryName) {
        super(new Properties().maxStackSize(1).group(API.creativeTab));
        setRegistryName(registryName);
    }

    // --------------------------------------------------------------------- //
    // Item

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new CapabilityProviderItemScanner(stack);
    }

//    @Override
//    public void getSubItems(final CreativeTabs tab, final NonNullList<ItemStack> items) {
//        super.getSubItems(tab, items);
//
//        if (!isInCreativeTab(tab)) {
//            return;
//        }
//
//        final ItemStack stack = new ItemStack(this);
//        final IEnergyStorage energyStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
//        if (energyStorage == null) {
//            return;
//        }
//
//        energyStorage.receiveEnergy(energyStorage.getMaxEnergyStored(), false);
//        items.add(stack);
//    }

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        // prevent crash during startup
        if (world == null)
            return;
        tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_SCANNER));

        if (!CommonConfig.useEnergy.get()) {
            return;
        }

        stack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyStorage->
                tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_SCANNER_ENERGY,
                        energyStorage.getEnergyStored(), energyStorage.getMaxEnergyStored())));
    }

    @Override
    public boolean showDurabilityBar(final ItemStack stack) {
        return CommonConfig.useEnergy.get();
    }

    @Override
    public double getDurabilityForDisplay(final ItemStack stack) {
        if (!CommonConfig.useEnergy.get()) {
            return 0;
        }

        return stack.getCapability(CapabilityEnergy.ENERGY).map(energyStorage->
                1 - energyStorage.getEnergyStored() / (double) energyStorage.getMaxEnergyStored()).orElse(1D);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, final Hand hand) {
        final ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) {
            final List<ItemStack> modules = new ArrayList<>();
            if (!collectModules(stack, modules)) {
                if (world.isRemote) {
                    Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TranslationTextComponent(Constants.MESSAGE_NO_SCAN_MODULES), Constants.CHAT_LINE_ID);
                }
                player.getCooldownTracker().setCooldown(this, 10);
                return new ActionResult<>(ActionResultType.FAIL, stack);
            }

            if (!tryConsumeEnergy(player, stack, modules, true)) {
                if (world.isRemote) {
                    Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TranslationTextComponent(Constants.MESSAGE_NOT_ENOUGH_ENERGY), Constants.CHAT_LINE_ID);
                }
                player.getCooldownTracker().setCooldown(this, 10);
                return new ActionResult<>(ActionResultType.FAIL, stack);
            }

            player.setActiveHand(hand);
            if (world.isRemote) {
                ScanManager.INSTANCE.beginScan(player, modules);
                SoundManager.INSTANCE.playChargingSound();
            }
        } else if (!world.isRemote()){
            NetworkHooks.openGui((ServerPlayerEntity) player, new ScannerContainerProvider(hand), (buffer) -> buffer.writeInt(hand.ordinal()));
        }
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(final ItemStack oldStack, final ItemStack newStack, final boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Constants.SCAN_COMPUTE_DURATION;
    }

    @Override
    public void onUsingTick(final ItemStack stack, final LivingEntity entity, final int count) {
        if (entity.getEntityWorld().isRemote) {
            ScanManager.INSTANCE.updateScan(entity, false);
        }
    }

    @Override
    public void onPlayerStoppedUsing(final ItemStack stack, final World world, final LivingEntity entity, final int timeLeft) {
        if (world.isRemote) {
            ScanManager.INSTANCE.cancelScan();
            SoundManager.INSTANCE.stopChargingSound();
        }
        super.onPlayerStoppedUsing(stack, world, entity, timeLeft);
    }

    @Override
    public ItemStack onItemUseFinish(final ItemStack stack, final World world, final LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return stack;
        }

        if (world.isRemote) {
            SoundCanceler.cancelEquipSound();
        }

        final List<ItemStack> modules = new ArrayList<>();
        if (!collectModules(stack, modules)) {
            return stack;
        }

        final boolean hasEnergy = tryConsumeEnergy((PlayerEntity) entity, stack, modules, false);
        if (world.isRemote) {
            SoundManager.INSTANCE.stopChargingSound();

            if (hasEnergy) {
                ScanManager.INSTANCE.updateScan(entity, true);
                SoundManager.INSTANCE.playActivateSound();
            } else {
                ScanManager.INSTANCE.cancelScan();
            }
        }

        final PlayerEntity player = (PlayerEntity) entity;
        player.getCooldownTracker().setCooldown(this, 40);

        return stack;
    }

    // --------------------------------------------------------------------- //

    static int getModuleEnergyCost(final PlayerEntity player, final ItemStack module) {
        if(module.getCapability(CapabilityScanResultProvider.SCAN_RESULT_PROVIDER_CAPABILITY).isPresent()) {
            return module.getCapability(CapabilityScanResultProvider.SCAN_RESULT_PROVIDER_CAPABILITY)
                    .map(provider->provider.getEnergyCost(player, module)).orElse(0);
        }
        if (Items.isModuleRange(module)) {
            return CommonConfig.energyCostModuleRange.get();
        }

        return 0;
    }

    private static boolean tryConsumeEnergy(final PlayerEntity player, final ItemStack stack, final List<ItemStack> modules, final boolean simulate) {
        if (!CommonConfig.useEnergy.get()) {
            return true;
        }

        if (player.isCreative()) {
            return true;
        }

        return stack.getCapability(CapabilityEnergy.ENERGY).map(energyStorage->{
            int totalCost = 0;
            for (final ItemStack module : modules) {
                totalCost += getModuleEnergyCost(player, module);
            }

            final int extracted = energyStorage.extractEnergy(totalCost, simulate);
            if (extracted < totalCost) {
                return false;
            }
            return true;
        }).orElse(false);
    }

    private static boolean collectModules(final ItemStack stack, final List<ItemStack> modules) {
        return stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).map(itemHandler->{
            boolean hasProvider = false;
            if (itemHandler instanceof ItemHandlerScanner) {
                final IItemHandler activeModules = ((ItemHandlerScanner) itemHandler).getActiveModules();
                for (int slot = 0; slot < activeModules.getSlots(); slot++) {
                    final ItemStack module = activeModules.getStackInSlot(slot);
                    if (module.isEmpty()) {
                        continue;
                    }

                    modules.add(module);
                    if (module.getCapability(CapabilityScanResultProvider.SCAN_RESULT_PROVIDER_CAPABILITY).isPresent()) {
                        hasProvider = true;
                    }
                }
            }
            return hasProvider;
        }).orElse(false);
    }

    // --------------------------------------------------------------------- //

    // Used to suppress the re-equip sound after finishing a scan (due to potential scanner item stack data change).
    private enum SoundCanceler {
        INSTANCE;

        public static void cancelEquipSound() {
            Minecraft.getInstance().enqueue(() -> MinecraftForge.EVENT_BUS.register(SoundCanceler.INSTANCE));
        }

        @SubscribeEvent
        public void onPlaySoundAtEntityEvent(final PlaySoundAtEntityEvent event) {
            if (event.getSound() == SoundEvents.ITEM_ARMOR_EQUIP_GENERIC) {
                event.setCanceled(true);
            }
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }
}
