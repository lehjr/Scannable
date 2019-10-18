package li.cil.scannable.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import li.cil.scannable.api.API;
import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.container.ContainerScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GuiScanner extends ContainerScreen<ContainerScanner> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/scanner.png");
    FontRenderer fontRenderer;

    // --------------------------------------------------------------------- //

    private final ContainerScanner container;

    // --------------------------------------------------------------------- //

    public GuiScanner(ContainerScanner container, PlayerInventory inv, ITextComponent titleIn) {
        super(container, inv, titleIn);
        this.container = container;
        ySize = 159;
//        allowUserInput = false; // fixme
        fontRenderer = Minecraft.getInstance().fontRenderer;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        if (isPointInRegion(8, 23, fontRenderer.getStringWidth(I18n.format(Constants.GUI_SCANNER_MODULES)), fontRenderer.FONT_HEIGHT, mouseX, mouseY)) {
            renderTooltip(I18n.format(Constants.GUI_SCANNER_MODULES_TOOLTIP), mouseX, mouseY);
        }
        if (isPointInRegion(8, 49, fontRenderer.getStringWidth(I18n.format(Constants.GUI_SCANNER_MODULES_INACTIVE)), fontRenderer.FONT_HEIGHT, mouseX, mouseY)) {
            renderTooltip(I18n.format(Constants.GUI_SCANNER_MODULES_INACTIVE_TOOLTIP), mouseX, mouseY);
        }

        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        fontRenderer.drawString(I18n.format(Constants.GUI_SCANNER_TITLE), 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format(Constants.GUI_SCANNER_MODULES), 8, 23, 0x404040);
        fontRenderer.drawString(I18n.format(Constants.GUI_SCANNER_MODULES_INACTIVE), 8, 49, 0x404040);
        fontRenderer.drawString(container.getPlayer().inventory.getDisplayName().getUnformattedComponentText(), 8, 65, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color4f(1, 1, 1, 1);
        minecraft.getTextureManager().bindTexture(BACKGROUND);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
//        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        blit(x, y, 0, 0, xSize, ySize);
    }

    @Override
    protected void handleMouseClick(final Slot slot, final int slotId, final int mouseButton, final ClickType type) {
        final PlayerInventory playerInventory = container.getPlayer().inventory;
        if (container.getHand() == Hand.MAIN_HAND && slot != null && slot.inventory == playerInventory) {
            final int currentItem = playerInventory.currentItem;
            if (slot.getSlotIndex() == currentItem) {
                return;
            }
            if (type == ClickType.SWAP && mouseButton == currentItem) {
                return;
            }
        }

        super.handleMouseClick(slot, slotId, mouseButton, type);
    }
}
