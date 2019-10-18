package li.cil.scannable.client.scanning;

import com.mojang.blaze3d.platform.GlStateManager;
import li.cil.scannable.api.Icons;
import li.cil.scannable.api.prefab.AbstractScanResultProvider;
import li.cil.scannable.api.scanning.ScanResult;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.init.Items;
import li.cil.scannable.common.item.ItemScannerModuleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Consumer;

public final class ScanResultProviderEntity extends AbstractScanResultProvider {
    public static final ScanResultProviderEntity INSTANCE = new ScanResultProviderEntity();

    // --------------------------------------------------------------------- //

    private boolean scanAnimal;
    private boolean scanMonster;
    private String scanEntity;
    private AxisAlignedBB bounds;
    private int minX, maxX, minZ, maxZ;
    private int chunksPerTick;
    private int x, z;
    private final List<LivingEntity> entities = new ArrayList<>();

    // --------------------------------------------------------------------- //

    private ScanResultProviderEntity() {
    }

    // --------------------------------------------------------------------- //
    // ScanResultProvider

    @Override
    public int getEnergyCost(final PlayerEntity player, final ItemStack module) {
        if (Items.isModuleAnimal(module)) {
            return CommonConfig.energyCostModuleAnimal.get();
        }
        if (Items.isModuleMonster(module)) {
            return CommonConfig.energyCostModuleMonster.get();
        }
        if (Items.isModuleEntity(module)) {
            return CommonConfig.energyCostModuleEntity.get();
        }

        throw new IllegalArgumentException(String.format("Module not supported by this provider: %s", module));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initialize(final PlayerEntity player, final Collection<ItemStack> modules, final Vec3d center, final float radius, final int scanTicks) {
        super.initialize(player, modules, center, radius, scanTicks);

        scanAnimal = false;
        scanMonster = false;
        scanEntity = null;
        for (final ItemStack module : modules) {
            scanAnimal |= Items.isModuleAnimal(module);
            scanMonster |= Items.isModuleMonster(module);
            if (Items.isModuleEntity(module)) {
                scanEntity = ItemScannerModuleEntity.getEntity(module);
            }
        }

        bounds = new AxisAlignedBB(center.x - radius, center.y - radius, center.z - radius,
                                   center.x + radius, center.y + radius, center.z + radius);


        double maxRadius =  player.world.getMaxEntityRadius();

        minX = MathHelper.floor((bounds.minX - maxRadius) / 16f);
        maxX = MathHelper.ceil((bounds.maxX + maxRadius) / 16f);
        minZ = MathHelper.floor((bounds.minZ - maxRadius) / 16f);
        maxZ = MathHelper.ceil((bounds.maxZ + maxRadius) / 16f);
        x = minX - 1; // -1 for initial moveNext.
        z = minZ;

        final int count = (maxX - minX + 1) * (maxZ - minZ + 1);
        chunksPerTick = MathHelper.ceil(count / (float) scanTicks);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void computeScanResults(final Consumer<ScanResult> callback) {
        final World world = player.getEntityWorld();
        for (int i = 0; i < chunksPerTick; i++) {
            if (!moveNext()) {
                return;
            }

            world.getChunk(x, z).getEntitiesOfTypeWithinAABB(LivingEntity.class, bounds, entities, this::FilterEntities);
            for (final LivingEntity entity : entities) {
                if (!entity.isAlive()) {
                    continue;
                }

                final Vec3d position = entity.getPositionVector();
                if (center.distanceTo(position) < radius) {
                    callback.accept(new ScanResultEntity(entity));
                }
            }
            entities.clear();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(final Entity entity, final List<ScanResult> results, final float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.enableBlend();

        final double posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        final double posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        final double posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        final float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        final float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        final Vec3d lookVec = entity.getLook(partialTicks).normalize();
        final Vec3d viewerEyes = entity.getEyePosition(partialTicks);

        final boolean showDistance = entity.isSneaking();

        // Order results by distance to center of screen (deviation from look
        // vector) so that labels we're looking at are in front of others.
        results.sort(Comparator.comparing(result -> {
            final ScanResultEntity resultEntity = (ScanResultEntity) result;
            final Vec3d entityEyes = resultEntity.entity.getEyePosition(partialTicks);
            final Vec3d toResult = entityEyes.subtract(viewerEyes);
            return lookVec.dotProduct(toResult.normalize());
        }));

        for (final ScanResult result : results) {
            final ScanResultEntity resultEntity = (ScanResultEntity) result;
            final String name = resultEntity.entity.getName().getFormattedText();
            final ResourceLocation icon = isMonster(resultEntity.entity) ? Icons.WARNING : Icons.INFO;
            final Vec3d resultPos = resultEntity.entity.getEyePosition(partialTicks);
            final float distance = showDistance ? (float) resultPos.subtract(viewerEyes).length() : 0f;
            renderIconLabel(posX, posY, posZ, yaw, pitch, lookVec, viewerEyes, distance, resultPos, icon, name);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepthTest();
        GlStateManager.enableLighting();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void reset() {
        super.reset();
        scanAnimal = scanMonster = false;
        bounds = null;
        minX = maxX = minZ = maxZ = 0;
        chunksPerTick = 0;
        x = z = 0;
        entities.clear();
    }

    // --------------------------------------------------------------------- //

    @OnlyIn(Dist.CLIENT)
    private boolean moveNext() {
        x++;
        if (x > maxX) {
            x = minX;
            z++;
            if (z > maxZ) {
                chunksPerTick = 0;
                return false;
            }
        }
        return true;
    }

    private <T extends Entity> boolean FilterEntities(final T entity) {
        if (scanEntity != null && Objects.equals(new ResourceLocation(scanEntity), ForgeRegistries.ENTITIES.getKey(entity.getType()))) {
            return true;
        }

        if (scanAnimal && isAnimal(entity)) {
            return true;
        }

        if (scanMonster && isMonster(entity)) {
            return true;
        }

        return false;
    }

    private static boolean isAnimal(final Entity entity) {
        if (entity instanceof AnimalEntity) {
            return true;
        }
        if (entity instanceof BatEntity) {
            return true;
        }
        if (entity instanceof SquidEntity) {
            return true;
        }
        return false;
    }

    private static boolean isMonster(final Entity entity) {
        if (entity instanceof MobEntity) {
            return true;
        }
        if (entity instanceof SlimeEntity) {
            return true;
        }
        if (entity instanceof GhastEntity) {
            return true;
        }
        if (entity instanceof EnderDragonEntity) {
            return true;
        }
        if (entity instanceof GolemEntity) {
            return true;
        }
        return false;
    }

    // --------------------------------------------------------------------- //

    private static final class ScanResultEntity implements ScanResult {
        private final Entity entity;

        ScanResultEntity(final Entity entity) {
            this.entity = entity;
        }

        // --------------------------------------------------------------------- //
        // ScanResult

        @Override
        public Vec3d getPosition() {
            return entity.getPositionVector();
        }

        @Override
        public AxisAlignedBB getRenderBounds() {
            return entity.getRenderBoundingBox();
        }
    }
}
