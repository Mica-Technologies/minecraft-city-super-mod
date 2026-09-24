package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The screen of a TV stand or sideboard: the vanilla chest's, cut to its rows (one for a TV
 * stand, two for a sideboard), titled with the block's name.
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class GuiResidentialStorage extends GuiContainer {

  private static final ResourceLocation CHEST =
      new ResourceLocation("textures/gui/container/generic_54.png");

  private final InventoryPlayer playerInventory;
  private final String title;
  private final int rows;

  /**
   * Constructs the screen.
   *
   * @param playerInventory the player's inventory
   * @param container       the container
   * @param title           the title, the block's name
   */
  public GuiResidentialStorage(InventoryPlayer playerInventory,
      ContainerResidentialStorage container, String title) {
    super(container);
    this.playerInventory = playerInventory;
    this.title = title;
    this.rows = container.getRows();
    this.allowUserInput = false;
    this.ySize = 114 + rows * 18;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    fontRenderer.drawString(title, 8, 6, 0x404040);
    fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8,
        ySize - 96 + 2, 0x404040);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(CHEST);
    int x = (width - xSize) / 2;
    int y = (height - ySize) / 2;
    // The chest's title bar and this many rows of slots, then its inventory panel.
    drawTexturedModalRect(x, y, 0, 0, xSize, rows * 18 + 17);
    drawTexturedModalRect(x, y + rows * 18 + 17, 0, 126, xSize, 96);
  }
}
