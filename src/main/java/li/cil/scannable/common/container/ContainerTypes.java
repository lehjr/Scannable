package li.cil.scannable.common.container;

import li.cil.scannable.api.API;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.registries.ObjectHolder;

import static li.cil.scannable.common.config.Constants.SCANNER_CONTAINER_TYPE__REG_NAME;

@ObjectHolder(API.MOD_ID)
public class ContainerTypes {

    @ObjectHolder(SCANNER_CONTAINER_TYPE__REG_NAME)
    public static final ContainerType<ContainerScanner> SCANNER_CONTAINER_TYPE = null;
}
