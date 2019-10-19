package li.cil.scannable.common.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import li.cil.scannable.api.API;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ClientConfig {
    public static final ClientSettings CLIENT_CONFIG;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static File clientFile;

    static {
        final Pair<ClientSettings, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(ClientSettings::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT_CONFIG = clientSpecPair.getLeft();
        clientFile = ConfigHelper.setupConfigFile(API.MOD_ID + "-client-only.toml");
        CLIENT_SPEC.setConfig(CommentedFileConfig.of(clientFile));
    }

    public static ForgeConfigSpec.ConfigValue<List<String>>
            oreColors,
            fluidColors;

    public static ForgeConfigSpec.BooleanValue
            injectDepthTexture,
            logBlockDropLookupFailures;

    public static class ClientSettings {
        ClientSettings(ForgeConfigSpec.Builder builder) {
            oreColors = builder
                    .comment("The colors for ores used when rendering their result bounding box.\n" +
                            "Each entry must be a key-value pair separated by a `=`, with the.\n" +
                            "key being the ore dictionary name and the value being the hexadecimal\n" +
                            "RGB value of the color.")
                    .translation(Constants.CONFIG_ORE_COLORS)
                    .define("oreColors", Arrays.asList(
                            // Minecraft
                            "forge:ores/coal=0x433E3B",
                            "forge:ores/iron=0xA17951",
                            "forge:ores/gold=0xF4F71F",
                            "forge:ores/lapis=0x4863F0",
                            "forge:ores/diamond=0x48E2F0",
                            "forge:ores/redstone=0xE61E1E",
                            "forge:ores/emerald=0x12BA16",
                            "forge:ores/quartz=0xB3D9D2",
                            "minecraft:glowstone=0xE9E68E",

                            // Thermal Foundation
                            "forge:ores/copper=0xE4A020",
                            "forge:ores/lead=0x8187C3",
                            "forge:ores/mithril=0x97D5FE",
                            "forge:ores/nickel=0xD0D3AC",
                            "forge:ores/platinum=0x7AC0FD",
                            "forge:ores/silver=0xE8F2FB",
                            "forge:ores/tin=0xCCE4FE",

                            // Misc.
                            "forge:ores/aluminum=0xCBE4E2",
                            "forge:ores/aluminium=0xCBE4E2",
                            "forge:ores/plutonium=0x9DE054",
                            "forge:ores/uranium=0x9DE054",
                            "forge:ores/yellorium=0xD8E054",

                            // Tinker's Construct
                            "forge:ores/ardite=0xB77E11",
                            "forge:ores/cobalt=0x413BB8",

                            // Thaumcraft
                            "forge:ores/cinnabar=0xF5DA25",
                            "forge:ores/infusedair=0xF7E677",
                            "forge:ores/infusedfire=0xDC7248",
                            "forge:ores/infusedwater=0x9595D5",
                            "forge:ores/infusedearth=0x49B45A",
                            "forge:ores/infusedorder=0x9FF2DE",
                            "forge:ores/infusedentropy=0x545476"
                    ));

            fluidColors = builder
                    .comment("The colors for fluids used when rendering their result bounding box.\n" +
                            "See `oreColors` for format entries have to be in.")
                    .translation(Constants.CONFIG_FLUID_COLORS)
                    .define("fluidColors", Arrays.asList(
                            "water=0x4275DC",
                            "lava=0xE26723"));

            injectDepthTexture = builder
                    .comment("Whether to try to inject a depth texture into Minecraft's FBO when rendering the\n" +
                            "scan wave effect. This is much faster as it will not have to re-render the world\n" +
                            "geometry to retrieve the depth information required for the effect. However, it\n" +
                            "appears that on some systems this doesn't work. The mod tries to detect that and\n" +
                            "will fall back to re-rendering automatically, but you can force re-rendering by\n" +
                            "setting this to false, e.g. for debugging or just to avoid the one logged warning.")
                    .translation(Constants.CONFIG_INJECT_DEPTH_TEXTURE)
                    .define("injectDepthTexture", true);

            logBlockDropLookupFailures = builder
                    .comment("Whether to log out failure to determine the item stack dropped by a block.\n" +
                            "Scannable needs to find the item stack representation of a block to get the\n" +
                            "ore dictionary name(s) of blocks, as well as to show a more accurate tooltip\n" +
                            "of the currently bound block in the block module. Scannable attempts to find\n" +
                            "the item stack representation by calling Block.getPickBlock (which is allowed\n" +
                            "to fail, as some blocks require a valid world state) and alternatively by using\n " +
                            "Item.getItemFromBlock+Block.damageDropped, the latter being verified using the\n" +
                            "roundtrip through Block.damageDropped/Item.getMetadata/Block.getStateFromMeta.\n" +
                            "Sadly this fails for a lot of modded blocks because people rarely implement\n" +
                            "Block.damageDropped. As a workaround you can add blocks for which this fails to\n" +
                            "the `statesCommon` and `statesRare` lists.")
                    .translation(Constants.CONFIG_LOG_BLOCK_DROP_LOOKUP_FAILURES)
                    .define("logBlockDropLookupFailures", false);

        }
    }
}
