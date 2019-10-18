package li.cil.scannable.api.prefab;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.GlStateManager;
import li.cil.scannable.api.scanning.ScanResult;
import li.cil.scannable.api.scanning.ScanResultProvider;
import li.cil.scannable.common.config.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Helper base class for scan result providers, providing some common
 * functionality for drawing result information.
 */
public abstract class AbstractScanResultProvider implements ScanResultProvider {
    protected PlayerEntity player;
    protected Vec3d center;
    protected float radius;

    // --------------------------------------------------------------------- //
    // ScanResultProvider

    @Override
    public int getEnergyCost(final PlayerEntity player, final ItemStack module) {
        return 50;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initialize(final PlayerEntity player, final Collection<ItemStack> modules, final Vec3d center, final float radius, final int scanTicks) {
        this.player = player;
        this.center = center;
        this.radius = radius;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isValid(final ScanResult result) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void reset() {
        player = null;
        center = null;
        radius = 0f;
    }

    // --------------------------------------------------------------------- //

    /**
     * Utility method for rendering a centered textured quad.
     *
     * @param width  the width of the quad.
     * @param height the height of the quad.
     */
    @OnlyIn(Dist.CLIENT)
    protected static void renderQuad(final float width, final float height) {
        final Tessellator t = Tessellator.getInstance();
        final BufferBuilder buffer = t.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        drawTexturedQuad(width, height, buffer);

        t.draw();
    }

    /**
     * Renders an icon with a label that is only shown when looked at. This is
     * what's used to render the entity labels for example.
     *
     * @param posX            the interpolated X position of the viewer.
     * @param posY            the interpolated Y position of the viewer.
     * @param posZ            the interpolated Z position of the viewer.
     * @param yaw             the interpolated yaw of the viewer.
     * @param pitch           the interpolated pitch of the viewer.
     * @param lookVec         the look vector of the viewer.
     * @param viewerEyes      the eye position of the viewer.
     * @param displayDistance the distance to show in the label. Zero or negative to hide.
     * @param resultPos       the interpolated position of the result.
     * @param icon            the icon to display.
     * @param label           the label text. May be null.
     */
    @OnlyIn(Dist.CLIENT)
    protected static void renderIconLabel(final double posX, final double posY, final double posZ, final float yaw, final float pitch, final Vec3d lookVec, final Vec3d viewerEyes, final float displayDistance, final Vec3d resultPos, final ResourceLocation icon, @Nullable final String label) {
        final Vec3d toResult = resultPos.subtract(viewerEyes);
        final float distance = (float) toResult.length();
        final float lookDirDot = (float) lookVec.dotProduct(toResult.normalize());
        final float sqLookDirDot = lookDirDot * lookDirDot;
        final float sq2LookDirDot = sqLookDirDot * sqLookDirDot;
        final float focusScale = MathHelper.clamp(sq2LookDirDot * sq2LookDirDot + 0.005f, 0.5f, 1f);
        final float scale = distance * focusScale * 0.005f;

        GlStateManager.pushMatrix();
        GlStateManager.translated(resultPos.x, resultPos.y, resultPos.z);
        GlStateManager.translated(-posX, -posY, -posZ);
        GlStateManager.rotatef(-yaw, 0, 1, 0);
        GlStateManager.rotatef(pitch, 1, 0, 0);
        GlStateManager.scalef(-scale, -scale, scale);

        if (lookDirDot > 0.999f && !Strings.isNullOrEmpty(label)) {
            final String text;
            if (displayDistance > 0) {
                text = I18n.format(Constants.GUI_OVERLAY_LABEL_DISTANCE, label, MathHelper.ceil(displayDistance));
            } else {
                text = label;
            }

            final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
            final int width = fontRenderer.getStringWidth(text) + 16;

            GlStateManager.disableTexture();
            GlStateManager.pushMatrix();
            GlStateManager.translated(width / 2, 0, 0);

            GlStateManager.color4f(0, 0, 0, 0.6f);
            renderQuad(width, fontRenderer.FONT_HEIGHT + 5);

            GlStateManager.popMatrix();
            GlStateManager.enableTexture();

            fontRenderer.drawStringWithShadow(text, 12, -4, 0xFFFFFFFF);
        }

        Minecraft.getInstance().getTextureManager().bindTexture(icon);

        GlStateManager.color4f(1, 1, 1, 1);
        renderQuad(16, 16);

        GlStateManager.popMatrix();
    }

    // --------------------------------------------------------------------- //
    // Drawing simple primitives in an existing buffer.

    @OnlyIn(Dist.CLIENT)
    protected static void drawTexturedQuad(final float width, final float height, final BufferBuilder buffer) {
        buffer.pos(-width / 2, height / 2, 0).tex(0, 1).endVertex();
        buffer.pos(width / 2, height / 2, 0).tex(1, 1).endVertex();
        buffer.pos(width / 2, -height / 2, 0).tex(1, 0).endVertex();
        buffer.pos(-width / 2, -height / 2, 0).tex(0, 0).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawCube(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final BufferBuilder buffer) {
        drawPlaneNegX(minX, minY, maxY, minZ, maxZ, buffer);
        drawPlanePosX(maxX, minY, maxY, minZ, maxZ, buffer);
        drawPlaneNegY(minY, minX, maxX, minZ, maxZ, buffer);
        drawPlanePosY(maxY, minX, maxX, minZ, maxZ, buffer);
        drawPlaneNegZ(minZ, minX, maxX, minY, maxY, buffer);
        drawPlanePosZ(maxZ, minX, maxX, minY, maxY, buffer);
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawCube(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        drawPlaneNegX(minX, minY, maxY, minZ, maxZ, r, g, b, a * 0.9f, buffer);
        drawPlanePosX(maxX, minY, maxY, minZ, maxZ, r, g, b, a * 0.9f, buffer);
        drawPlaneNegY(minY, minX, maxX, minZ, maxZ, r, g, b, a * 0.8f, buffer);
        drawPlanePosY(maxY, minX, maxX, minZ, maxZ, r, g, b, a * 1.1f, buffer);
        drawPlaneNegZ(minZ, minX, maxX, minY, maxY, r, g, b, a, buffer);
        drawPlanePosZ(maxZ, minX, maxX, minY, maxY, r, g, b, a, buffer);
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).endVertex();
        buffer.pos(x, minY, maxZ).endVertex();
        buffer.pos(x, maxY, maxZ).endVertex();
        buffer.pos(x, maxY, minZ).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).color(r, g, b, a).endVertex();
        buffer.pos(x, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(x, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(x, maxY, minZ).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).endVertex();
        buffer.pos(x, maxY, minZ).endVertex();
        buffer.pos(x, maxY, maxZ).endVertex();
        buffer.pos(x, minY, maxZ).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).color(r, g, b, a).endVertex();
        buffer.pos(x, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.pos(x, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(x, minY, maxZ).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).endVertex();
        buffer.pos(maxX, y, minZ).endVertex();
        buffer.pos(maxX, y, maxZ).endVertex();
        buffer.pos(minX, y, maxZ).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).color(r, g, b, a).endVertex();
        buffer.pos(maxX, y, minZ).color(r, g, b, a).endVertex();
        buffer.pos(maxX, y, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(minX, y, maxZ).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).endVertex();
        buffer.pos(minX, y, maxZ).endVertex();
        buffer.pos(maxX, y, maxZ).endVertex();
        buffer.pos(maxX, y, minZ).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).color(r, g, b, a).endVertex();
        buffer.pos(minX, y, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(maxX, y, maxZ).color(r, g, b, a).endVertex();
        buffer.pos(maxX, y, minZ).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).endVertex();
        buffer.pos(minX, maxY, z).endVertex();
        buffer.pos(maxX, maxY, z).endVertex();
        buffer.pos(maxX, minY, z).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlaneNegZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).color(r, g, b, a).endVertex();
        buffer.pos(minX, maxY, z).color(r, g, b, a).endVertex();
        buffer.pos(maxX, maxY, z).color(r, g, b, a).endVertex();
        buffer.pos(maxX, minY, z).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).endVertex();
        buffer.pos(maxX, minY, z).endVertex();
        buffer.pos(maxX, maxY, z).endVertex();
        buffer.pos(minX, maxY, z).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    protected static void drawPlanePosZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final float r, final float g, final float b, final float a, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).color(r, g, b, a).endVertex();
        buffer.pos(maxX, minY, z).color(r, g, b, a).endVertex();
        buffer.pos(maxX, maxY, z).color(r, g, b, a).endVertex();
        buffer.pos(minX, maxY, z).color(r, g, b, a).endVertex();
    }
}
