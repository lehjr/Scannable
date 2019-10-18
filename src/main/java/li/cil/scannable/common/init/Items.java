package li.cil.scannable.common.init;

import li.cil.scannable.api.API;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.item.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@ObjectHolder(API.MOD_ID)
public final class Items {
    @ObjectHolder(Constants.NAME_SCANNER)
    public static final Item scanner = null;
    @ObjectHolder(Constants.NAME_MODULE_BLANK)
    public static final Item moduleBlank = null;
    @ObjectHolder(Constants.NAME_MODULE_RANGE)
    public static final Item moduleRange = null;
    @ObjectHolder(Constants.NAME_MODULE_ANIMAL)
    public static final Item moduleAnimal = null;
    @ObjectHolder(Constants.NAME_MODULE_MONSTER)
    public static final Item moduleMonster = null;
    @ObjectHolder(Constants.NAME_MODULE_ORE_COMMON)
    public static final Item moduleOreCommon = null;
    @ObjectHolder(Constants.NAME_MODULE_ORE_RARE)
    public static final Item moduleOreRare = null;
    @ObjectHolder(Constants.NAME_MODULE_BLOCK)
    public static final Item moduleBlock = null;
    @ObjectHolder(Constants.NAME_MODULE_STRUCTURE)
    public static final Item moduleStructure = null;
    @ObjectHolder(Constants.NAME_MODULE_FLUID)
    public static final Item moduleFluid = null;
    @ObjectHolder(Constants.NAME_MODULE_ENTITY)
    public static final Item moduleEntity = null;

    public static List<Item> getAllItems() {
        return Arrays.asList(
                scanner,
                moduleBlank,
                moduleRange,
                moduleAnimal,
                moduleMonster,
                moduleOreCommon,
                moduleOreRare,
                moduleBlock,
                moduleStructure,
                moduleFluid,
                moduleEntity
        );
    }

    // --------------------------------------------------------------------- //

    public static boolean isScanner(final ItemStack stack) {
        return isItem(stack, scanner);
    }

    public static boolean isModuleRange(final ItemStack stack) {
        return isItem(stack, moduleRange);
    }

    public static boolean isModuleAnimal(final ItemStack stack) {
        return isItem(stack, moduleAnimal);
    }

    public static boolean isModuleMonster(final ItemStack stack) {
        return isItem(stack, moduleMonster);
    }

    public static boolean isModuleOreCommon(final ItemStack stack) {
        return isItem(stack, moduleOreCommon);
    }

    public static boolean isModuleOreRare(final ItemStack stack) {
        return isItem(stack, moduleOreRare);
    }

    public static boolean isModuleBlock(final ItemStack stack) {
        return isItem(stack, moduleBlock);
    }

    public static boolean isModuleStructure(final ItemStack stack) {
        return isItem(stack, moduleStructure);
    }

    public static boolean isModuleFluid(final ItemStack stack) {
        return isItem(stack, moduleFluid);
    }

    public static boolean isModuleEntity(final ItemStack stack) {
        return isItem(stack, moduleEntity);
    }

    // --------------------------------------------------------------------- //

    public static void register(final IForgeRegistry<Item> registry) {
        registry.registerAll(
                new ItemScanner(Constants.NAME_SCANNER),
                new ItemScannerModuleBlank(Constants.NAME_MODULE_BLANK),
                new ItemScannerModuleRange(Constants.NAME_MODULE_RANGE),
                new ItemScannerModuleAnimal(Constants.NAME_MODULE_ANIMAL),
                new ItemScannerModuleMonster(Constants.NAME_MODULE_MONSTER),
                new ItemScannerModuleBlockOreCommon(Constants.NAME_MODULE_ORE_COMMON),
                new ItemScannerModuleBlockOreRare(Constants.NAME_MODULE_ORE_RARE),
                new ItemScannerModuleBlockConfigurable(Constants.NAME_MODULE_BLOCK),
                new ItemScannerModuleStructure(Constants.NAME_MODULE_STRUCTURE),
                new ItemScannerModuleBlockFluid(Constants.NAME_MODULE_FLUID),
                new ItemScannerModuleEntity(Constants.NAME_MODULE_ENTITY));
    }

    // --------------------------------------------------------------------- //
    private static boolean isItem(final ItemStack stack, @Nullable final Item item) {
        return !stack.isEmpty() && stack.getItem() == item;
    }

    // --------------------------------------------------------------------- //

    private Items() {
    }
}
