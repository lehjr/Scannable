package li.cil.scannable.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonConfig {
    public static final CommonSettings COMMON_CONFIG;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<CommonSettings, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(CommonSettings::new);
        COMMON_SPEC = serverSpecPair.getRight();
        COMMON_CONFIG = serverSpecPair.getLeft();
    }

    public static ForgeConfigSpec.BooleanValue useEnergy;

    public static ForgeConfigSpec.IntValue
            energyCapacityScanner,
            energyCostModuleRange,
            energyCostModuleAnimal,
            energyCostModuleMonster,
            energyCostModuleOreCommon,
            energyCostModuleOreRare,
            energyCostModuleBlock,
            energyCostModuleStructure,
            energyCostModuleFluid,
            energyCostModuleEntity,
            baseScanRadius;

    public static ForgeConfigSpec.ConfigValue<List<String>>
            oreBlacklist,
            blockBlacklist,
            oresCommon,
            oresRare,
            statesCommon,
            statesRare,
            structures,
            fluidBlacklist;

    public static class CommonSettings {
        CommonSettings(ForgeConfigSpec.Builder builder) {
            useEnergy = builder
                    .comment("Whether to consume energy when performing a scan.\n" +
                            "Will make the scanner a chargeable item.")
                    .translation(Constants.CONFIG_USE_ENERGY)
                    .worldRestart()
                    .define("useEnergy", true);

            energyCapacityScanner = builder
                    .comment("Amount of energy that can be stored in a scanner.")
                    .translation(Constants.CONFIG_ENERGY_CAPACITY_SCANNER)
                    .worldRestart()
                    .defineInRange("energyCapacityScanner", 5000, 0, Integer.MAX_VALUE);

            energyCostModuleRange = builder
                    .comment("Amount of energy used by the range module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_RANGE)
                    .worldRestart()
                    .defineInRange("energyCostModuleRange", 100, 0, Integer.MAX_VALUE);

            energyCostModuleAnimal = builder
                    .comment("Amount of energy used by the animal module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_ANIMAL)
                    .worldRestart()
                    .defineInRange("energyCostModuleAnimal", 25, 0, Integer.MAX_VALUE);

            energyCostModuleMonster = builder
                    .comment("Amount of energy used by the monster module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_MONSTER)
                    .worldRestart()
                    .defineInRange("energyCostModuleMonster", 50, 0, Integer.MAX_VALUE);

            energyCostModuleOreCommon = builder
                    .comment("Amount of energy used by the common ore module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_ORE_COMMON)
                    .worldRestart()
                    .defineInRange("energyCostModuleOreCommon", 75, 0, Integer.MAX_VALUE);

            energyCostModuleOreRare = builder
                    .comment("Amount of energy used by the rare ore module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_ORE_RARE)
                    .worldRestart()
                    .defineInRange("energyCostModuleOreRare", 100, 0, Integer.MAX_VALUE);

            energyCostModuleBlock = builder
                    .comment("Amount of energy used by the block module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_BLOCK)
                    .worldRestart()
                    .defineInRange("energyCostModuleBlock", 100, 0, Integer.MAX_VALUE);

            energyCostModuleStructure = builder
                    .comment("Amount of energy used by the structure module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_STRUCTURE)
                    .worldRestart()
                    .defineInRange("energyCostModuleStructure", 150, 0, Integer.MAX_VALUE);

            energyCostModuleFluid = builder
                    .comment("Amount of energy used by the fluid module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_FLUID)
                    .worldRestart()
                    .defineInRange("energyCostModuleFluid", 50, 0, Integer.MAX_VALUE);

            energyCostModuleEntity = builder
                    .comment("Amount of energy used by the entity module per scan.")
                    .translation(Constants.CONFIG_ENERGY_MODULE_ENTITY)
                    .worldRestart()
                    .defineInRange("energyCostModuleEntity", 75, 0, Integer.MAX_VALUE);

            baseScanRadius = builder
                    .comment("The basic scan radius without range modules.\n" +
                            "IMPORTANT: some modules such as the block and ore scanner modules will already use\n" +
                            "a reduced radius based on this value. Specifically, the ore scanners multiply this\n" +
                            "value by " + Constants.MODULE_ORE_RADIUS_MULTIPLIER + ", and the block scanner multiplies it by " + Constants.MODULE_BLOCK_RADIUS_MULTIPLIER + ".\n" +
                            "Range modules will boost the range by half this value.")
                    .translation(Constants.CONFIG_BASE_SCAN_RADIUS)
                    .worldRestart()
                    .defineInRange("baseScanRadius", 64, 16, 128);

            oreBlacklist = builder
                    .comment("Ore dictionary entries that match the common ore pattern but should be ignored.")
                    .translation(Constants.CONFIG_BLOCK_BLACKLIST)
                    .worldRestart()
                    .define("oreBlacklist", new ArrayList<>());

            blockBlacklist = builder
                    .comment("Registry names of blocks that will never be scanned.")
                    .translation(Constants.CONFIG_ORE_BLACKLIST)
                    .worldRestart()
                    .define("blockBlacklist", new ArrayList<String>() {{
                        add("minecraft:command_block");
                    }});

            oresCommon = builder
                    .comment("Ore dictionary entries considered common ores, requiring the common ore scanner module.\n" +
                            "Use this to mark ores as common, as opposed to rare (see oresRare).")
                    .translation(Constants.CONFIG_ORES_COMMON)
                    .worldRestart()
                    .define("oresCommon", Arrays.asList(
                            // Minecraft
                            "oreCoal",
                            "oreIron",
                            "oreRedstone",
                            "glowstone",

                            // Thermal Foundation
                            "oreCopper",
                            "oreTin",
                            "oreLead",

                            // Immersive Engineering
                            "oreAluminum",
                            "oreAluminium",

                            // Thaumcraft
                            "oreCinnabar"
                    ));

            oresRare = builder
                    .comment("Ore dictionary names of ores considered 'rare', requiring the rare ore scanner module.\n" +
                            "Anything matching /ore[A-Z].*/ that isn't in the common ore list is\n" +
                            "automatically considered a rare ore (as opposed to the other way around,\n" +
                            "to make missing entries less likely be a problem). Use this to add rare\n" +
                            "ores that do follow this pattern.")
                    .translation(Constants.CONFIG_ORES_RARE)
                    .worldRestart()
                    .define("oresRare", new ArrayList<>());

            statesCommon = builder
                    .            comment("Block states considered common ores, requiring the common ore scanner module.\n" +
                            "Use this to mark arbitrary block states as common ores. Format is as follows:\n" +
                            "  mod_id:block_name\n" +
                            "or with block properties:\n" +
                            "  mod_id:block_name[property1=value1,property2=value2]\n" +
                            "You can look up the properties (as well as name and mod id) in the F3 debug overlay\n" +
                            "in the bottom right.")
                    .translation(Constants.CONFIG_STATES_COMMON)
                    .worldRestart()
                    .define("statesCommon", new ArrayList<>());

            statesRare = builder
                    .            comment("Block states considered rare ores, requiring the rare ore scanner module.\n" +
                            "Use this to mark arbitrary block states as rare ores. Format is as follows:\n" +
                            "  mod_id:block_name\n" +
                            "or with block properties:\n" +
                            "  mod_id:block_name[property1=value1,property2=value2]\n" +
                            "You can look up the properties (as well as name and mod id) in the F3 debug overlay\n" +
                            "in the bottom right.")
                    .translation(Constants.CONFIG_STATES_RARE)
                    .worldRestart()
                    .define("statesRare", new ArrayList<>());

            structures = builder
                    .comment("The list of structures the structure module scans for.")
                    .translation(Constants.CONFIG_STRUCTURES)
                    .worldRestart()
                    .define("structures", Arrays.asList(
                            "EndCity",
                            "Fortress",
                            "Mansion",
                            "Mineshaft",
                            "Monument",
                            "Stronghold",
                            "Temple",
                            "Village"));

            fluidBlacklist = builder
                    .comment("Fluid names of fluids that should be ignored.")
                    .translation(Constants.CONFIG_FLUID_BLACKLIST)
                    .define("fluidBlacklist", new ArrayList<>());
        }
    }
}