package li.cil.scannable.client.scanning;

import com.mojang.blaze3d.platform.GlStateManager;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import li.cil.scannable.api.prefab.AbstractScanResultProvider;
import li.cil.scannable.api.scanning.ScanResult;
import li.cil.scannable.common.Scannable;
import li.cil.scannable.common.config.ClientConfig;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.init.Items;
import li.cil.scannable.common.item.ItemScannerModuleBlockConfigurable;
import li.cil.scannable.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScanResultProviderBlock extends AbstractScanResultProvider {
    public static final ScanResultProviderBlock INSTANCE = new ScanResultProviderBlock();

    // --------------------------------------------------------------------- //

    private static final int DEFAULT_COLOR = 0x4466CC;
    private static final float BASE_ALPHA = 0.25f;
    private static final float MIN_ALPHA = 0.13f; // Slightly > 0.1f/0.8f
    private static final float STATE_SCANNED_ALPHA = 0.7f;
    private static final Pattern STATE_DESC_PATTERN = Pattern.compile("(?<name>[^\\[]+)(?:\\[(?<properties>(?:[^,=\\]]+)=(?:[^,=\\]]+)(?:,(?:[^,=\\]]+)=(?:[^,=\\]]+))*)])?");

    private final TIntIntMap blockColors = new TIntIntHashMap();
    private final BitSet oresCommon = new BitSet();
    private final BitSet oresRare = new BitSet();
    private final BitSet fluids = new BitSet();
    private boolean scanCommon, scanRare, scanFluids;
    private final List<ScanFilter> scanFilters = new ArrayList<>();
    private float sqRadius, sqOreRadius;
    private BlockPos min, max;
    private int blocksPerTick;
    private int x, y, z;
    private final Map<BlockPos, ScanResultOre> resultClusters = new HashMap<>();
    private final List<ScanResultOre> nonCulledResults = new ArrayList<>();

    // --------------------------------------------------------------------- //

    private ScanResultProviderBlock() {
    }

    @OnlyIn(Dist.CLIENT)
    public void rebuildOreCache() {
        blockColors.clear();
        oresCommon.clear();
        oresRare.clear();
        fluids.clear();

        buildOreCache();
    }

    // --------------------------------------------------------------------- //
    // ScanResultProvider

    @Override
    public int getEnergyCost(final PlayerEntity player, final ItemStack module) {
        if (Items.isModuleOreCommon(module)) {
            return CommonConfig.energyCostModuleOreCommon.get();
        }
        if (Items.isModuleOreRare(module)) {
            return CommonConfig.energyCostModuleOreRare.get();
        }
        if (Items.isModuleBlock(module)) {
            return CommonConfig.energyCostModuleBlock.get();
        }
        if (Items.isModuleFluid(module)) {
            return CommonConfig.energyCostModuleFluid.get();
        }

        throw new IllegalArgumentException(String.format("Module not supported by this provider: %s", module));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initialize(final PlayerEntity player, final Collection<ItemStack> modules, final Vec3d center, final float radius, final int scanTicks) {
        super.initialize(player, modules, center, computeRadius(modules, radius), scanTicks);

        scanCommon = false;
        scanRare = false;
        scanFluids = false;
        scanFilters.clear();
        for (final ItemStack module : modules) {
            scanCommon |= Items.isModuleOreCommon(module);
            scanRare |= Items.isModuleOreRare(module);
            scanFluids |= Items.isModuleFluid(module);
            if (Items.isModuleBlock(module)) {
                final BlockState state = ItemScannerModuleBlockConfigurable.getBlockState(module);
                if (state != null) {
                    scanFilters.add(new ScanFilter(state));
                }
            }
        }

        sqRadius = this.radius * this.radius;
        sqOreRadius = radius * Constants.MODULE_ORE_RADIUS_MULTIPLIER;
        sqOreRadius *= sqOreRadius;
        min = new BlockPos(center).add(-this.radius, -this.radius, -this.radius);
        max = new BlockPos(center).add(this.radius, this.radius, this.radius);
        x = min.getX();
        y = min.getY() - 1; // -1 for initial moveNext.
        z = min.getZ();
        final BlockPos size = max.subtract(min);
        final int count = (size.getX() + 1) * (size.getY() + 1) * (size.getZ() + 1);
        blocksPerTick = MathHelper.ceil(count / (float) scanTicks);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void computeScanResults(final Consumer<ScanResult> callback) {
        final World world = player.getEntityWorld();
        final List<String> blacklist = CommonConfig.blockBlacklist.get();
        for (int i = 0; i < blocksPerTick; i++) {
            if (!moveNext(world)) {
                return;
            }

            if (center.squareDistanceTo(x + 0.5, y + 0.5, z + 0.5) > sqRadius) {
                continue;
            }

            final BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);

            if (blacklist.contains(state.getBlock())) {
                continue;
            }

//            state = state.getActualState(world, pos);

            if (blacklist.contains(state.getBlock())) {
                continue;
            }

            final int stateId = Block.getStateId(state);
            if (anyFilterMatches(state) && !tryAddToCluster(pos, stateId)) {
                final ScanResultOre result = new ScanResultOre(stateId, pos, STATE_SCANNED_ALPHA);
                callback.accept(result);
                resultClusters.put(pos, result);
                continue;
            }

            if (!scanCommon && !scanRare && !scanFluids) {
                continue;
            }

            if (center.squareDistanceTo(x + 0.5, y + 0.5, z + 0.5) > sqOreRadius) {
                continue;
            }

            final boolean matches = (scanCommon && oresCommon.get(stateId)) || (scanRare && oresRare.get(stateId)) || (scanFluids && fluids.get(stateId));
            if (matches && !tryAddToCluster(pos, stateId)) {
                final ScanResultOre result = new ScanResultOre(stateId, pos);
                callback.accept(result);
                resultClusters.put(pos, result);
            }
        }
    }

    private boolean anyFilterMatches(final BlockState state) {
        for (final ScanFilter filter : scanFilters) {
            if (filter.matches(state)) {
                return true;
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isValid(final ScanResult result) {
        return ((ScanResultOre) result).isRoot();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(final Entity entity, final List<ScanResult> results, final float partialTicks) {
        final double posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        final double posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        final double posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        final Vec3d lookVec = entity.getLook(partialTicks).normalize();
        final Vec3d viewerEyes = entity.getEyePosition(partialTicks);

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GlStateManager.pushMatrix();
        GlStateManager.translated(-posX, -posY, -posZ);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        final float colorNormalizer = 1 / 255f;
        for (final ScanResult result : results) {
            final ScanResultOre resultOre = (ScanResultOre) result;

            if (resultOre.bounds.contains(viewerEyes)) {
                nonCulledResults.add(resultOre);
                continue;
            }

            final Vec3d toResult = resultOre.getPosition().subtract(viewerEyes);
            final float lookDirDot = (float) lookVec.dotProduct(toResult.normalize());
            final float sqLookDirDot = lookDirDot * lookDirDot;
            final float sq2LookDirDot = sqLookDirDot * sqLookDirDot;
            final float focusScale = MathHelper.clamp(sq2LookDirDot * sq2LookDirDot + 0.005f, 0.5f, 1f);

            final int color;
            if (blockColors.containsKey(resultOre.stateId)) {
                color = blockColors.get(resultOre.stateId);
            } else {
                color = DEFAULT_COLOR;
            }

            final float r = ((color >> 16) & 0xFF) * colorNormalizer;
            final float g = ((color >> 8) & 0xFF) * colorNormalizer;
            final float b = (color & 0xFF) * colorNormalizer;
            final float a = Math.max(MIN_ALPHA, Math.max(BASE_ALPHA, resultOre.getAlphaOverride()) * focusScale);

            drawCube(resultOre.bounds.minX, resultOre.bounds.minY, resultOre.bounds.minZ,
                     resultOre.bounds.maxX, resultOre.bounds.maxY, resultOre.bounds.maxZ,
                     r, g, b, a, buffer);
        }

        tessellator.draw();

        if (!nonCulledResults.isEmpty()) {
            GlStateManager.disableCull();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            for (final ScanResultOre resultOre : nonCulledResults) {
                final Vec3d toResult = resultOre.getPosition().subtract(viewerEyes);
                final float lookDirDot = (float) lookVec.dotProduct(toResult.normalize());
                final float sqLookDirDot = lookDirDot * lookDirDot;
                final float sq2LookDirDot = sqLookDirDot * sqLookDirDot;
                final float focusScale = MathHelper.clamp(sq2LookDirDot * sq2LookDirDot + 0.005f, 0.5f, 1f);

                final int color;
                if (blockColors.containsKey(resultOre.stateId)) {
                    color = blockColors.get(resultOre.stateId);
                } else {
                    color = DEFAULT_COLOR;
                }

                final float r = ((color >> 16) & 0xFF) * colorNormalizer;
                final float g = ((color >> 8) & 0xFF) * colorNormalizer;
                final float b = (color & 0xFF) * colorNormalizer;
                final float a = Math.max(MIN_ALPHA, Math.max(BASE_ALPHA, resultOre.getAlphaOverride()) * focusScale);

                drawCube(resultOre.bounds.minX, resultOre.bounds.minY, resultOre.bounds.minZ,
                         resultOre.bounds.maxX, resultOre.bounds.maxY, resultOre.bounds.maxZ,
                         r, g, b, a, buffer);
            }

            tessellator.draw();

            GlStateManager.enableCull();
        }

        nonCulledResults.clear();

        GlStateManager.popMatrix();

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.enableDepthTest();
        GlStateManager.enableLighting();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void reset() {
        super.reset();
        scanCommon = scanRare = scanFluids = false;
        scanFilters.clear();
        sqRadius = sqOreRadius = 0;
        min = max = null;
        blocksPerTick = 0;
        x = y = z = 0;
        resultClusters.clear();
    }

    // --------------------------------------------------------------------- //

    @OnlyIn(Dist.CLIENT)
    private static float computeRadius(final Collection<ItemStack> modules, final float radius) {
        boolean scanOres = false;
        boolean scanState = false;
        for (final ItemStack module : modules) {
            scanOres |= Items.isModuleOreCommon(module);
            scanOres |= Items.isModuleOreRare(module);
            scanState |= Items.isModuleBlock(module);
        }

        if (scanOres && scanState) {
            return radius * Math.max(Constants.MODULE_ORE_RADIUS_MULTIPLIER, Constants.MODULE_BLOCK_RADIUS_MULTIPLIER);
        } else if (scanOres) {
            return radius * Constants.MODULE_ORE_RADIUS_MULTIPLIER;
        } else {
            assert scanState;
            return radius * Constants.MODULE_BLOCK_RADIUS_MULTIPLIER;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private boolean tryAddToCluster(final BlockPos pos, final int stateId) {
        final BlockPos min = pos.add(-1, -1, -1);
        final BlockPos max = pos.add(1, 1, 1);

        ScanResultOre root = null;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    final BlockPos clusterPos = new BlockPos(x, y, z);
                    final ScanResultOre cluster = resultClusters.get(clusterPos);
                    if (cluster == null) {
                        continue;
                    }
                    if (stateId != cluster.stateId) {
                        continue;
                    }

                    if (root == null) {
                        root = cluster.getRoot();
                        root.add(pos);
                        resultClusters.put(pos, root);
                    } else {
                        cluster.setRoot(root);
                    }
                }
            }
        }

        return root != null;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean moveNext(final World world) {
        y++;
        if (y > max.getY() || y >= world.getHeight()) {
            y = min.getY();
            x++;
            if (x > max.getX()) {
                x = min.getX();
                z++;
                if (z > max.getZ()) {
                    blocksPerTick = 0;
                    return false;
                }
            }
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void buildOreCache() {
        Scannable.getLog().info("Building block state lookup table...");

        final long start = System.currentTimeMillis();

        final TObjectIntMap<String> oreColorsByOreName = buildColorTable(ClientConfig.oreColors.get());
        final TObjectIntMap<String> fluidColorsByFluidName = buildColorTable(ClientConfig.fluidColors.get());

        final Set<String> oreNamesBlacklist = new HashSet<>(CommonConfig.oreBlacklist.get());
        final Set<String> oreNamesCommon = new HashSet<>(CommonConfig.oresCommon.get());
        final Set<String> oreNamesRare = new HashSet<>(CommonConfig.oresRare.get());
        final Set<String> stateDescsCommon = new HashSet<>(CommonConfig.statesCommon.get());
        final Set<String> stateDescsRare = new HashSet<>(CommonConfig.statesRare.get());
        final Set<String> fluidBlacklist = new HashSet<>(CommonConfig.blockBlacklist.get());

        final Pattern pattern = Pattern.compile("^ore[A-Z].*$");
        for (final Block block : ForgeRegistries.BLOCKS.getValues()) {
            for (final BlockState state : block.getStateContainer().getValidStates()) {
                final int stateId = Block.getStateId(state);
                final ItemStack stack = BlockUtils.getItemStackFromState(state, null);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    final Collection<ResourceLocation> ids = ItemTags.getCollection().getOwningTags(item);
                    boolean isRare = false;
                    boolean isCommon = false;
                    for (final ResourceLocation id : ids) {
                        final String name = id.toString();
                        if (oreNamesBlacklist.contains(name)) {
                            isRare = false;
                            isCommon = false;
                            break;
                        }

                        if (oreNamesCommon.contains(name)) {
                            isCommon = true;

                            // Fixme: this is broken. We don't actually prefix with "Ore" anymore
                        } else if (oreNamesRare.contains(name) || pattern.matcher(name).matches()) {
                            isRare = true;
                        } else {
                            continue;
                        }

                        if (oreColorsByOreName.containsKey(name)) {
                            blockColors.put(stateId, oreColorsByOreName.get(name));
                        }
                    }

                    if (isCommon) {
                        oresCommon.set(stateId);
                    } else if (isRare) {
                        oresRare.set(stateId);
                    }
                }
            }
        }

        registerStates(stateDescsCommon, oresCommon);
        registerStates(stateDescsRare, oresRare);

        for (final Map.Entry<ResourceLocation, Fluid> entry : ForgeRegistries.FLUIDS.getEntries()) {
            final String fluidName = entry.getKey().toString();
            if (fluidBlacklist.contains(fluidName)) {
                continue;
            }

            final Fluid fluid = entry.getValue();
            final Block block = fluid.getDefaultState().getBlockState().getBlock();
            if (block == null) {
                continue;
            }

            final BlockState state = block.getDefaultState();
            final int stateId = Block.getStateId(state);

            if (fluidColorsByFluidName.containsKey(fluidName)) {
                blockColors.put(stateId, fluidColorsByFluidName.get(fluidName));
            } else {
                blockColors.put(stateId, fluid.getAttributes().getColor());
            }

            fluids.set(stateId);
        }

        Scannable.getLog().info("Built    block state lookup table in {} ms.", System.currentTimeMillis() - start);
    }

    @OnlyIn(Dist.CLIENT)
    private static TObjectIntMap<String> buildColorTable(final List<String> colorConfigs) {
        final TObjectIntMap<String> colors = new TObjectIntHashMap<>();

        final Pattern pattern = Pattern.compile("^(?<name>[^\\s=]+)\\s*=\\s*0x(?<color>[a-fA-F0-9]+)$");
        for (final String colorConfig : colorConfigs) {
            final Matcher matcher = pattern.matcher(colorConfig.trim());
            if (!matcher.matches()) {
                Scannable.getLog().warn("Illegal color entry in settings: '{}'", colorConfig.trim());
                continue;
            }

            final String name = matcher.group("name");
            final int color = Integer.parseInt(matcher.group("color"), 16);

            colors.put(name, color);
        }

        return colors;
    }

    private static void registerStates(final Set<String> stateDescs, final BitSet states) {
        for (final String stateDesc : stateDescs) {
            final BlockState state = parseStateDesc(stateDesc);
            if (state != null) {
                final int stateId = Block.getStateId(state);
                states.set(stateId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static BlockState parseStateDesc(final String stateDesc) {
        final Matcher matcher = STATE_DESC_PATTERN.matcher(stateDesc);
        if (!matcher.matches()) {
            Scannable.getLog().warn("Failed parsing block state: {}", stateDesc);
            return null;
        }

        final String name = matcher.group("name").trim();
        final Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        if (block == null || block == Blocks.AIR) {
            return null;
        }

        BlockState state = block.getDefaultState();

        final String serializedProperties = matcher.group("properties");
        if (serializedProperties != null) {
            final Collection<IProperty<?>> blockProperties = state.getProperties();
            outer:
            for (final String serializedProperty : serializedProperties.split(",")) {
                final String[] keyValuePair = serializedProperty.split("=");
                assert keyValuePair.length == 2;
                final String serializedKey = keyValuePair[0].trim();
                final String serializedValue = keyValuePair[1].trim();
                for (final IProperty property : blockProperties) {
                    if (Objects.equals(property.getName(), serializedKey)) {
                        final Comparable originalValue = state.get(property);
                        do {
                            if (Objects.equals(property.getName(state.get(property)), serializedValue)) {
                                continue outer;
                            }
                            state = state.cycle(property);
                        }
                        while (!Objects.equals(state.get(property), originalValue));
                        Scannable.getLog().warn("Cannot parse property value '{}' for property '{}' of block {}.", serializedValue, serializedKey, name);
                        continue outer;
                    }
                }
                Scannable.getLog().warn("Block {} has no property '{}'.", name, serializedKey);
            }
        }

        return state;
    }

    // --------------------------------------------------------------------- //

    private static final class ScanFilter {
        private final BlockState reference;
        private final List<IProperty> properties = new ArrayList<>();

        private ScanFilter(final BlockState state) {
            this.reference = state;
            // TODO Filter for configurable properties (configurable in the block module).
            for (final IProperty<?> property : state.getProperties()) {
                if (Objects.equals(property.getName(), "variant") || // Vanilla Minecraft.
                    Objects.equals(property.getName(), "type") || // E.g. ThermalFoundation, TiCon, IC2, Immersive Engineering.
                    Objects.equals(property.getName(), "ore") || // E.g. BigReactors.
                    Objects.equals(property.getName(), "oretype")) { // E.g. DeepResonance.
                    properties.add(property);
                }
            }
        }

        @SuppressWarnings("unchecked")
        boolean matches(final BlockState state) {
            if (reference.getBlock() != state.getBlock()) {
                return false;
            }

            if (properties.isEmpty()) {
                return true;
            }

            for (final IProperty property : properties) {
                if (!state.getProperties().contains(property)) {
                    continue;
                }
                if (!Objects.equals(state.get(property), reference.get(property))) {
                    return false;
                }
            }

            return true;
        }
    }

    private static final class ScanResultOre implements ScanResult {
        private final int stateId;
        private AxisAlignedBB bounds;
        @Nullable
        private ScanResultOre parent;
        private final float alphaOverride;

        ScanResultOre(final int stateId, final BlockPos pos, final float alphaOverride) {
            bounds = new AxisAlignedBB(pos);
            this.stateId = stateId;
            this.alphaOverride = alphaOverride;
        }

        ScanResultOre(final int stateId, final BlockPos pos) {
            this(stateId, pos, 0f);
        }

        float getAlphaOverride() {
            return alphaOverride;
        }

        boolean isRoot() {
            return parent == null;
        }

        ScanResultOre getRoot() {
            if (parent != null) {
                return parent.getRoot();
            }
            return this;
        }

        void setRoot(final ScanResultOre root) {
            if (parent != null) {
                parent.setRoot(root);
                return;
            }
            if (root == this) {
                return;
            }

            root.bounds = root.bounds.union(bounds);
            parent = root;
        }

        void add(final BlockPos pos) {
            assert parent == null : "Trying to add to non-root node.";
            bounds = bounds.union(new AxisAlignedBB(pos));
        }

        // --------------------------------------------------------------------- //
        // ScanResult

        @Nullable
        @Override
        public AxisAlignedBB getRenderBounds() {
            return bounds;
        }

        @Override
        public Vec3d getPosition() {
            return bounds.getCenter();
        }
    }
}
