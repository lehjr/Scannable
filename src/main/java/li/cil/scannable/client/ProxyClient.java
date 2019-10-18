//package li.cil.scannable.client;
//
//import li.cil.scannable.api.API;
//import li.cil.scannable.client.gui.GuiHandlerClient;
//import li.cil.scannable.client.renderer.OverlayRenderer;
//import li.cil.scannable.client.renderer.ScannerRenderer;
//import li.cil.scannable.common.Scannable;
//import li.cil.scannable.common.init.Items;
//import net.minecraft.client.renderer.model.ModelResourceLocation;
//import net.minecraft.item.Item;
//import net.minecraft.util.ResourceLocation;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.ModelRegistryEvent;
//import net.minecraftforge.client.model.ModelLoader;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.client.event.ConfigChangedEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.network.NetworkRegistry;
//import org.apache.commons.lang3.ObjectUtils;
//
///**
// * Takes care of client-side only setup.
// */
//@Mod.EventBusSubscriber(Dist.CLIENT)
//public final class ProxyClient extends ProxyCommon {
//    @Override
//    public void onInit(final FMLInitializationEvent event) {
//        super.onInit(event);
//
//        NetworkRegistry.INSTANCE.registerGuiHandler(Scannable.instance, new GuiHandlerClient());
//
//
//    }
//
//    @Override
//    public void onPostInit(final FMLPostInitializationEvent event) {
//        super.onPostInit(event);
//
//
//    }
//
//    // --------------------------------------------------------------------- //
//
//    @SubscribeEvent
//    public static void handleModelRegistryEvent(final ModelRegistryEvent event) {
//        for (final Item item : Items.getAllItems()) {
//            final ResourceLocation registryName = item.getRegistryName();
//            assert registryName != null;
//            final ModelResourceLocation location = new ModelResourceLocation(registryName, "inventory");
//            ModelLoader.setCustomModelResourceLocation(item, 0, location);
//        }
//    }
//
//    @SubscribeEvent
//    public static void handleConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
//        if (ObjectUtils.notEqual(API.MOD_ID, event.getModID())) {
//            return;
//        }
//
//        ConfigManager.sync(API.MOD_ID, Config.Type.INSTANCE);
//    }
//}
