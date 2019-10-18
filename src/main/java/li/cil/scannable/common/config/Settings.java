//package li.cil.scannable.common.config;
//
//import li.cil.scannable.client.scanning.ScanResultProviderBlock;
//import net.minecraft.block.Block;
//import net.minecraft.block.Blocks;
//import net.minecraft.util.ResourceLocation;
//import net.minecraftforge.common.ForgeConfigSpec;
//import net.minecraftforge.registries.ForgeRegistries;
//
//import javax.annotation.Nullable;
//import java.util.*;
//
//public final class Settings {
//
//
//
//
//
//
//    // --------------------------------------------------------------------- //
//
//    private static ServerSettings serverSettings;
//    private static final Set<Block> blockBlacklistSet = new HashSet<>();
//
//    public static void setServerSettings(@Nullable final ServerSettings serverSettings) {
//        Settings.serverSettings = serverSettings;
//
//        ScanResultProviderBlock.INSTANCE.rebuildOreCache();
//
//        blockBlacklistSet.clear();
//        for (final String blockName : getBlockBlacklist()) {
//            final Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
//            if (block != null && block != Blocks.AIR) {
//                blockBlacklistSet.add(block);
//            }
//        }
//    }
//
//    public static Set<Block> getBlockBlacklistSet() {
//        return blockBlacklistSet;
//    }
//
//    // --------------------------------------------------------------------- //
//
//    public static boolean useEnergy() {
//        return serverSettings != null ? serverSettings.useEnergy : useEnergy;
//    }
//
//    public static int getEnergyCapacityScanner() {
//        return serverSettings != null ? serverSettings.energyCapacityScanner : energyCapacityScanner;
//    }
//
//    public static int getEnergyCostModuleRange() {
//        return serverSettings != null ? serverSettings.energyCostModuleRange : energyCostModuleRange;
//    }
//
//    public static int getEnergyCostModuleAnimal() {
//        return serverSettings != null ? serverSettings.energyCostModuleAnimal : energyCostModuleAnimal;
//    }
//
//    public static int getEnergyCostModuleMonster() {
//        return serverSettings != null ? serverSettings.energyCostModuleMonster : energyCostModuleMonster;
//    }
//
//    public static int getEnergyCostModuleOreCommon() {
//        return serverSettings != null ? serverSettings.energyCostModuleOreCommon : energyCostModuleOreCommon;
//    }
//
//    public static int getEnergyCostModuleOreRare() {
//        return serverSettings != null ? serverSettings.energyCostModuleOreRare : energyCostModuleOreRare;
//    }
//
//    public static int getEnergyCostModuleBlock() {
//        return serverSettings != null ? serverSettings.energyCostModuleBlock : energyCostModuleBlock;
//    }
//
//    public static int getEnergyCostModuleStructure() {
//        return serverSettings != null ? serverSettings.energyCostModuleStructure : energyCostModuleStructure;
//    }
//
//    public static int getEnergyCostModuleFluid() {
//        return serverSettings != null ? serverSettings.energyCostModuleFluid : energyCostModuleFluid;
//    }
//
//    public static int getEnergyCostModuleEntity() {
//        return serverSettings != null ? serverSettings.energyCostModuleEntity : energyCostModuleEntity;
//    }
//
//    public static int getBaseScanRadius() {
//        return serverSettings != null ? serverSettings.baseScanRadius : baseScanRadius;
//    }
//
//    public static String[] getBlockBlacklist() {
//        return serverSettings != null ? serverSettings.blockBlacklist : blockBlacklist;
//    }
//
//    public static String[] getOreBlacklist() {
//        return serverSettings != null ? serverSettings.oresBlacklist : oreBlacklist;
//    }
//
//    public static String[] getCommonOres() {
//        return serverSettings != null ? serverSettings.oresCommon : oresCommon;
//    }
//
//    public static String[] getRareOres() {
//        return serverSettings != null ? serverSettings.oresRare : oresRare;
//    }
//
//    public static String[] getCommonStates() {
//        return serverSettings != null ? serverSettings.statesCommon : statesCommon;
//    }
//
//    public static String[] getRareStates() {
//        return serverSettings != null ? serverSettings.statesRare : statesRare;
//    }
//
//    public static String[] getStructures() {
//        return serverSettings != null ? serverSettings.structures : structures;
//    }
//
//    public static String[] getFluidBlacklist() {
//        return serverSettings != null ? serverSettings.fluidBlacklist : fluidBlacklist;
//    }
//
//    // --------------------------------------------------------------------- //
//
//    private Settings() {
//    }
//}
