package li.cil.scannable.common;


import li.cil.scannable.api.API;
import li.cil.scannable.client.ScanManager;
import li.cil.scannable.client.gui.GuiScanner;
import li.cil.scannable.client.renderer.OverlayRenderer;
import li.cil.scannable.client.renderer.ScannerRenderer;
import li.cil.scannable.common.capabilities.CapabilityScanResultProvider;
import li.cil.scannable.common.config.ClientConfig;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.config.ConfigHelper;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.container.ContainerScanner;
import li.cil.scannable.common.container.ContainerTypes;
import li.cil.scannable.common.init.Items;
import li.cil.scannable.network.Network;
import li.cil.scannable.network.packets.ClientOnLoginPacket;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for FML.
 */
@Mod(API.MOD_ID)
public final class Scannable {
    // --------------------------------------------------------------------- //
    // FML / Forge

    public Scannable() {
        // Initialize API.
        API.creativeTab = new CreativeTab();

        log = LogManager.getLogger(API.MOD_ID);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_SPEC, ConfigHelper.setupConfigFile(API.MOD_ID+"-common.toml").getAbsolutePath());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC, ClientConfig.clientFile.getAbsolutePath());

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the setupClient method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);

        // Register ourselves for server, registry and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);
    }

    // common
    private void setup(final FMLCommonSetupEvent event) {
        Network.registerPackets();

        // Initialize capabilities.
        CapabilityScanResultProvider.register();
    }

    // client
    private void setupClient(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(ScannerRenderer.INSTANCE);
        MinecraftForge.EVENT_BUS.register(OverlayRenderer.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ScanManager.INSTANCE);
        ScreenManager.registerFactory(ContainerTypes.SCANNER_CONTAINER_TYPE, GuiScanner::new);
//
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player != null) {
            Network.CHANNEL_INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new ClientOnLoginPacket());
        }
    }


    @SubscribeEvent
    public void handleRegisterItemsEvent(final RegistryEvent.Register<Item> event) {
        Items.register(event.getRegistry());
    }

    @SubscribeEvent
    public void registerContainerTypes(final RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().register(
                // the IForgeContainerType only needed for extra data
                IForgeContainerType.create((windowId, playerInventory, data) -> {
                    Hand typeIndex = Hand.values()[data.readInt()];
                    return new ContainerScanner(windowId, playerInventory, typeIndex);
                }).setRegistryName(Constants.SCANNER_CONTAINER_TYPE__REG_NAME));
    }


    // --------------------------------------------------------------------- //

    /**
     * Logger the mod should use, filled in pre-init.
     */
    private static Logger log;

    /**
     * Get the logger to be used by the mod.
     *
     * @return the mod's logger.
     */
    public static Logger getLog() {
        return log;
    }
}
