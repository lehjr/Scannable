package li.cil.scannable.client.renderer;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import li.cil.scannable.api.API;
import li.cil.scannable.client.ScanManager;
import li.cil.scannable.common.Scannable;
import li.cil.scannable.common.config.ClientConfig;
import li.cil.scannable.integration.optifine.ProxyOptiFine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public enum ScannerRenderer {
    INSTANCE;

    final boolean isStencilEnabled = false; // Fixme: experimental
    ShaderReloader shaderReloader = new ShaderReloader();


    // --------------------------------------------------------------------- //
    // Settings

    // Locations of the actual shaders used for rendering the scanner effect.
    private static final ResourceLocation SCANNER_VERTEX_SHADER_LOCATION = new ResourceLocation(API.MOD_ID, "shaders/scanner.vsh");
    private static final ResourceLocation SCANNER_FRAGMENT_SHADER_LOCATION = new ResourceLocation(API.MOD_ID, "shaders/scanner.fsh");
    private static final ResourceLocation COPY_VERTEX_SHADER_LOCATION = new ResourceLocation(API.MOD_ID, "shaders/copy.vsh");
    private static final ResourceLocation COPY_FRAGMENT_SHADER_LOCATION = new ResourceLocation(API.MOD_ID, "shaders/copy.fsh");

    // --------------------------------------------------------------------- //
    // Resolved locations of uniforms in the shader, cached for speed.

    private int vertexShader, fragmentShader, shaderProgram;
    private int camPosUniform, centerUniform, radiusUniform;
    private int zNearUniform, zFarUniform, aspectUniform;
    private int copyVertexShader, copyFragmentShader, copyShaderProgram;

    // --------------------------------------------------------------------- //
    // Framebuffer and depth texture IDs.

    private Mode mode;
    private int framebufferObject;
    private int framebufferDepthTexture;
    private int copyFramebufferObject;
    private int copyFramebufferDepthTexture;

    // --------------------------------------------------------------------- //
    // Direct memory float buffers for setting uniforms, cached for alloc-free.

    private final FloatBuffer float1Buffer = BufferUtils.createFloatBuffer(1);
    private final FloatBuffer float3Buffer = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer float16Buffer = BufferUtils.createFloatBuffer(16);

    // --------------------------------------------------------------------- //
    // Matrices and corner ray vertices for alloc-free.

    private final Matrix4f projectionMatrix = new Matrix4f(), modelViewMatrix = new Matrix4f(), mvpMatrix = new Matrix4f();
    private final Vector4f tempCorner = new Vector4f();
    private final Vector3f topLeft = new Vector3f(), topRight = new Vector3f(), bottomLeft = new Vector3f(), bottomRight = new Vector3f();

    // View space coordinates of screen corners.
    public static final Vector4f CORNER_TOP_LEFT = new Vector4f(-1f, 1f, 1f, 1f);
    public static final Vector4f CORNER_TOP_RIGHT = new Vector4f(1f, 1f, 1f, 1f);
    public static final Vector4f CORNER_BOTTOM_LEFT = new Vector4f(-1f, -1f, 1f, 1f);
    public static final Vector4f CORNER_BOTTOM_RIGHT = new Vector4f(1f, -1f, 1f, 1f);

    // --------------------------------------------------------------------- //
    // State of the scanner, set when triggering a ping.

    private long currentStart = -1;

    // --------------------------------------------------------------------- //

    /**
     * Initialize or re-initialize the shader used for the scanning effect.
     */
    public void init() {
        final IResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        shaderReloader.onResourceManagerReload(resourceManager);
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).addReloadListener(shaderReloader);
        }
    }

    class ShaderReloader implements ISelectiveResourceReloadListener {
        @Nullable
        @Override
        public IResourceType getResourceType() {
            return ShaderResourceType.CUSTOM_SHADERS;
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            try {
                deleteShader();
                vertexShader = loadShader(resourceManager, GLX.GL_VERTEX_SHADER, SCANNER_VERTEX_SHADER_LOCATION);
                fragmentShader = loadShader(resourceManager, GLX.GL_FRAGMENT_SHADER, SCANNER_FRAGMENT_SHADER_LOCATION);
                shaderProgram = linkProgram(vertexShader, fragmentShader);
                camPosUniform = GLX.glGetUniformLocation(shaderProgram, "camPos");
                centerUniform = GLX.glGetUniformLocation(shaderProgram, "center");
                radiusUniform = GLX.glGetUniformLocation(shaderProgram, "radius");
                zNearUniform = GLX.glGetUniformLocation(shaderProgram, "zNear");
                zFarUniform = GLX.glGetUniformLocation(shaderProgram, "zFar");
                aspectUniform = GLX.glGetUniformLocation(shaderProgram, "aspect");

                copyVertexShader = loadShader(resourceManager, GLX.GL_VERTEX_SHADER, COPY_VERTEX_SHADER_LOCATION);
                copyFragmentShader = loadShader(resourceManager, GLX.GL_FRAGMENT_SHADER, COPY_FRAGMENT_SHADER_LOCATION);
                copyShaderProgram = linkProgram(copyVertexShader, copyFragmentShader);
            } catch (final Exception e) {
                deleteShader();
                Scannable.getLog().error("Failed loading shader.", e);
            }
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
            if (resourcePredicate.test(getResourceType())) {
                onResourceManagerReload(resourceManager);
            }
        }
    }

    public enum ShaderResourceType implements IResourceType {
        CUSTOM_SHADERS;
    }

    public void ping(final Vec3d pos) {
        if (shaderProgram == 0) {
            return;
        }
        // framebuffer always enabled now
        if (!GLX.isUsingFBOs()) {
            return;
        }

        currentStart = System.currentTimeMillis();

        GLX.glUseProgram(shaderProgram);

        final Minecraft mc = Minecraft.getInstance();
        final Framebuffer framebuffer = mc.getFramebuffer();

        setUniform(aspectUniform, framebuffer.framebufferTextureWidth / (float) framebuffer.framebufferTextureHeight);
        setUniform(zNearUniform, 0.05f);
        setUniform(zFarUniform, mc.gameSettings.renderDistanceChunks * 16);
        setUniform(centerUniform, pos);

        GLX.glUseProgram(0);
    }

    // A note on the rendering: usually we just render when the world renders, as a simple additive overlay after
    // everything else has been rendered. This is fine for regular Minecraft. However, when people use shaders (via
    // OptiFine) those shaders may build up a GBuffer for deferred shading or other special stuff where this just
    // doesn't play nice; so in that case we instead render as a game overlay. However, for this to work we have to
    // copy the depth texture somewhere else for use in the overlay event because it gets cleared before the game
    // overlays start rendering. We'll also have to push the matrices used for world rendering again.

    @SubscribeEvent
    public void onPreWorldRender(final TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Prepare injected framebuffer if necessary in prerender so it is populated in this frame right away.
            preRender();
        }
    }

    @SubscribeEvent
    public void onWorldRender(final RenderWorldLastEvent event) {
        final boolean isRenderingEffect = framebufferDepthTexture != 0;
        if (isRenderingEffect) {
            final boolean isUsingShaders = ProxyOptiFine.INSTANCE.isShaderPackLoaded();
            if (isUsingShaders) {
                // Using shaders, need to copy depth and render as a game overlay.
                copyDepthTexture();
            } else {
                // Regular rendering, just render additively in world space.
                render(event.getPartialTicks());
            }
        }
    }

    @SubscribeEvent
    public void onPreRenderGameOverlay(final RenderGameOverlayEvent.Pre event) {
        final boolean isRenderingEffect = framebufferDepthTexture != 0;
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && isRenderingEffect) {
            final boolean isUsingShaders = ProxyOptiFine.INSTANCE.isShaderPackLoaded();
            if (isUsingShaders) {
                // Using shaders so we render as game overlay; restore matrices as used for world rendering.
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();

                Minecraft.getInstance().gameRenderer.setupCameraTransform(event.getPartialTicks());
                render(event.getPartialTicks());

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
        }
    }

    private void preRender() {
        if (currentStart < 0) {
            return;
        }

        if (shaderProgram == 0) {
            return;
        }

        if (!GLX.isUsingFBOs()) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();

        final World world = mc.world;
        if (world == null) {
            return;
        }

        if (ProxyOptiFine.INSTANCE.isShaderPackLoaded()) {
            mode = Mode.OPTIFINE;
        } else if (mode == Mode.OPTIFINE || mode == null) {
            mode = ClientConfig.injectDepthTexture.get() ? Mode.INJECT : Mode.RENDER;
        }

        final Framebuffer framebuffer = mc.getFramebuffer();
        final int adjustedDuration = ScanManager.computeScanGrowthDuration();

        if (framebufferDepthTexture == 0) {
            if (adjustedDuration > (int) (System.currentTimeMillis() - currentStart)) {
                installDepthTexture(framebuffer);
            }
        } else if (adjustedDuration < (int) (System.currentTimeMillis() - currentStart)) {
            uninstallDepthTexture(framebuffer);
            currentStart = -1; // for early exit
        } else if (mode == Mode.INJECT && GL11.glGetError() != 0) {
            Scannable.getLog().info("Huh, looks like our injected depth texture broke something maybe? Falling back to re-rendering.");
            uninstallDepthTexture(framebuffer);
            mode = Mode.RENDER;
            installDepthTexture(framebuffer);
        }
    }

    private void copyDepthTexture() {
        final Minecraft mc = Minecraft.getInstance();

        final Framebuffer framebuffer = mc.getFramebuffer();

        final int oldFramebuffer = GlStateManager.getInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, copyFramebufferObject);

        final int width = framebuffer.framebufferTextureWidth;
        final int height = framebuffer.framebufferTextureHeight;

        GlStateManager.bindTexture(framebufferDepthTexture);

        final int oldProgram = GlStateManager.getInteger(GL20.GL_CURRENT_PROGRAM);
        GLX.glUseProgram(copyShaderProgram);

        setupMatrices(width, height);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(0, height, 0).tex(0, 0).endVertex();
        buffer.pos(width, height, 0).tex(1, 0).endVertex();
        buffer.pos(width, 0, 0).tex(1, 1).endVertex();
        buffer.pos(0, 0, 0).tex(0, 1).endVertex();

        tessellator.draw();

        restoreMatrices();

        GLX.glUseProgram(oldProgram);

        GlStateManager.bindTexture(0);

        GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, oldFramebuffer);
    }

    private void render(final float partialTicks) {
        final Minecraft mc = Minecraft.getInstance();

        final World world = mc.world;
        if (world == null) {
            return;
        }

        final Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) {
            return;
        }

        final Framebuffer framebuffer = mc.getFramebuffer();
        final int adjustedDuration = ScanManager.computeScanGrowthDuration();

        if (mode == Mode.RENDER) {
            final int oldFramebuffer = GlStateManager.getInteger(GL30.GL_FRAMEBUFFER_BINDING);

            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);

            GlStateManager.clearDepth(GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.disableTexture();

            mc.worldRenderer.renderBlockLayer(BlockRenderLayer.SOLID, mc.gameRenderer.getActiveRenderInfo());
            mc.worldRenderer.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, mc.gameRenderer.getActiveRenderInfo());

            GlStateManager.enableTexture();

            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, oldFramebuffer);
        }

        setupCorners();

        GlStateManager.pushMatrix();
        GlStateManager.pushLightingAttributes();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        final int width = framebuffer.framebufferTextureWidth;
        final int height = framebuffer.framebufferTextureHeight;
        final float radius = ScanManager.computeRadius(currentStart, adjustedDuration);

        if (copyFramebufferDepthTexture != 0) {
            // Rendering as game overlay, take depth from our own texture into which we copied the depth.
            GlStateManager.bindTexture(copyFramebufferDepthTexture);
        } else {
            // Regular rendering (there's no depth copy), read from depth directly.
            if (mode == Mode.INJECT) {
                // Activate original depth render buffer while we use the depth texture.
                // Even though it's not written to typically drivers won't like reading
                // from a sampler of a texture that's part of the current render target.
                if (this.isStencilEnabled /*framebuffer.isStencilEnabled()*/) {
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                } else {
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                }
            }

            GlStateManager.bindTexture(framebufferDepthTexture);
        }

        final int oldProgram = GlStateManager.getInteger(GL20.GL_CURRENT_PROGRAM);
        GLX.glUseProgram(shaderProgram);

        setUniform(camPosUniform, viewer.getEyePosition(partialTicks));
        setUniform(radiusUniform, radius);

        setupMatrices(width, height);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();

        // Use the normal to pass along the ray direction for each corner.
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);

        buffer.pos(0, height, 0).tex(0, 0).normal(bottomLeft.getX(), bottomLeft.getY(), bottomLeft.getZ()).endVertex();
        buffer.pos(width, height, 0).tex(1, 0).normal(bottomRight.getX(), bottomRight.getY(), bottomRight.getZ()).endVertex();
        buffer.pos(width, 0, 0).tex(1, 1).normal(topRight.getX(), topRight.getY(), topRight.getZ()).endVertex();
        buffer.pos(0, 0, 0).tex(0, 1).normal(topLeft.getX(), topLeft.getY(), topLeft.getZ()).endVertex();

        tessellator.draw();

        restoreMatrices();

        GLX.glUseProgram(oldProgram);

        GlStateManager.bindTexture(0);

        if (mode == Mode.INJECT && copyFramebufferDepthTexture == 0) {
            // Swap back in our depth texture for that sweet, sweet depth info.
            if (this.isStencilEnabled /*framebuffer.isStencilEnabled()*/) {
                GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, framebufferDepthTexture, 0);
            } else {
                GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, framebufferDepthTexture, 0);
            }
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);

        GlStateManager.popAttributes();;
        GlStateManager.popMatrix();
    }

    // --------------------------------------------------------------------- //

    private static int loadShader(final IResourceManager resourceManager, final int type, final ResourceLocation location) throws Exception {
        final int shader = GLX.glCreateShader(type);
        compileShader(resourceManager, shader, location);
        return shader;
    }

    private static void compileShader(final IResourceManager resourceManager, final int shader, final ResourceLocation location) throws Exception {
        final IResource resource = resourceManager.getResource(location);

        try (final InputStream stream = resource.getInputStream()) {
            String s = TextureUtil.readResourceAsString(stream);
            GLX.glShaderSource(shader, s);
        }

        GLX.glCompileShader(shader);
        if (GLX.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new Exception(GLX.glGetShaderInfoLog(shader, 4096));
        }
    }

    private static int linkProgram(final int vertexShader, final int fragmentShader) throws Exception {
        final int program = GLX.glCreateProgram();
        if (vertexShader > 0) GLX.glAttachShader(program, vertexShader);
        GLX.glAttachShader(program, fragmentShader);
        GLX.glLinkProgram(program);
        if (GLX.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new Exception(GLX.glGetProgramInfoLog(program, 4096));
        }
        return program;
    }

    private void deleteShader() {
        if (shaderProgram != 0) {
            GLX.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (vertexShader != 0) {
            GLX.glDeleteShader(vertexShader);
            vertexShader = 0;
        }
        if (fragmentShader != 0) {
            GLX.glDeleteShader(fragmentShader);
            fragmentShader = 0;
        }
        if (copyShaderProgram != 0) {
            GLX.glDeleteProgram(copyShaderProgram);
            copyShaderProgram = 0;
        }
        if (copyVertexShader != 0) {
            GLX.glDeleteShader(copyVertexShader);
            copyVertexShader = 0;
        }
        if (copyFragmentShader != 0) {
            GLX.glDeleteShader(copyFragmentShader);
            copyFragmentShader = 0;
        }
    }

    private void installDepthTexture(final Framebuffer framebuffer) {
        final int oldFramebuffer = GlStateManager.getInteger(GL30.GL_FRAMEBUFFER_BINDING);

        switch (mode) {
            case INJECT:
                framebufferObject = framebuffer.framebufferObject;
                if (this.isStencilEnabled /*framebuffer.isStencilEnabled()*/) {
                    framebufferDepthTexture = createTexture(framebuffer.framebufferTextureWidth, framebuffer.framebufferTextureHeight, GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8);
                    GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);
                    GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, framebufferDepthTexture, 0);
                } else {
                    framebufferDepthTexture = createTexture(framebuffer.framebufferTextureWidth, framebuffer.framebufferTextureHeight, GL14.GL_DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT);
                    GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);
                    GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, framebufferDepthTexture, 0);
                }
                break;
            case RENDER:
                framebufferObject = GLX.glGenFramebuffers();
                framebufferDepthTexture = createTexture(framebuffer.framebufferTextureWidth, framebuffer.framebufferTextureHeight, GL14.GL_DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT);
                GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);
                GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, framebufferDepthTexture, 0);
                break;
            case OPTIFINE:
                framebufferDepthTexture = ProxyOptiFine.INSTANCE.getDepthTexture();

                copyFramebufferObject = GLX.glGenFramebuffers();
                copyFramebufferDepthTexture = createTexture(framebuffer.framebufferTextureWidth, framebuffer.framebufferTextureHeight, GL30.GL_R32F, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE);
                GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, copyFramebufferObject);
                GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GLX.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, copyFramebufferDepthTexture, 0);
                break;
        }

        GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, oldFramebuffer);
    }

    private void uninstallDepthTexture(final Framebuffer framebuffer) {
        switch (mode) {
            case INJECT:
                if (this.isStencilEnabled /*framebuffer.isStencilEnabled()*/) {
                    GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                } else {
                    GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, framebufferObject);
                    GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GLX.GL_RENDERBUFFER, framebuffer.depthBuffer);
                }
                TextureUtil.releaseTextureId(framebufferDepthTexture);
                break;
            case RENDER:
                GLX.glDeleteFramebuffers(framebufferObject);
                TextureUtil.releaseTextureId(framebufferDepthTexture);
                break;
            case OPTIFINE:
                GLX.glDeleteFramebuffers(copyFramebufferObject);
                TextureUtil.releaseTextureId(copyFramebufferDepthTexture);
                break;
        }

        framebufferObject = 0;
        framebufferDepthTexture = 0;
        copyFramebufferObject = 0;
        copyFramebufferDepthTexture = 0;
    }

    private int createTexture(final int width, final int height, final int internalFormat, final int format, final int type) {
        final int texture = TextureUtil.generateTextureId();
        GlStateManager.bindTexture(texture);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL14.GL_DEPTH_TEXTURE_MODE, GL11.GL_LUMINANCE);
//        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
        GlStateManager.texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null);
        GlStateManager.bindTexture(0);
        return texture;
    }

    private void setupCorners() {
        getMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        getMatrix(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        mvpMatrix.mul(projectionMatrix, modelViewMatrix);
        mvpMatrix.invert();
        setupCorner(CORNER_TOP_LEFT, topLeft);
        setupCorner(CORNER_TOP_RIGHT, topRight);
        setupCorner(CORNER_BOTTOM_LEFT, bottomLeft);
        setupCorner(CORNER_BOTTOM_RIGHT, bottomRight);
    }

    private void setupCorner(final Vector4f corner, final Vector3f into) {
        mvpMatrix.transform(corner, tempCorner);
        tempCorner.scale(1 / tempCorner.w);
        into.set(tempCorner.x, tempCorner.y, tempCorner.z);
        into.normalize();
    }

    private void setupMatrices(final int width, final int height) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width, height, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translated(0, 0, -2000);
        GlStateManager.viewport(0, 0, width, height);
    }

    private void restoreMatrices() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    private void getMatrix(final int matrix, final Matrix4f into) {
        float16Buffer.position(0);
        GlStateManager.getMatrix(matrix, float16Buffer);
        float16Buffer.position(0);
        // floatbuffer.Array() not working for whatever reason
        into.set(new Matrix4f(
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get(),
                float16Buffer.get()));
    }

    private void setUniform(final int uniform, final float value) {
        float1Buffer.clear();
        float1Buffer.put(value);
        float1Buffer.rewind();
        GLX.glUniform1(uniform, float1Buffer);
    }

    private void setUniform(final int uniform, final Vec3d value) {
        float3Buffer.clear();
        float3Buffer.put((float) value.x);
        float3Buffer.put((float) value.y);
        float3Buffer.put((float) value.z);
        float3Buffer.rewind();
        GLX.glUniform3(uniform, float3Buffer);
    }

    private enum Mode {
        INJECT,
        RENDER,
        OPTIFINE
    }
}
