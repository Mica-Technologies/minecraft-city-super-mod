package com.micatechnologies.minecraft.csm.furniture.appliance;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An appliance's screen, drawn on the vanilla furnace's: the progress arrow fills through each
 * cycle, the flame burns down for an appliance with fuel. One with a supply (the copier's book
 * and quill) keeps the fuel slot for it, the flame painted out. One that burns nothing has the fuel
 * slot and the flame painted out with the screen's own blank panel and its input slot moved level
 * with the output. One that uses water has a tank gauge to the left, whose tooltip says how many
 * cycles are left and how to fill it.
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class GuiAppliance extends GuiContainer {

  private static final ResourceLocation FURNACE =
      new ResourceLocation("textures/gui/container/furnace.png");

  /** The tank gauge: left, top, width and height within the screen. */
  private static final int TANK_X = 30;
  private static final int TANK_Y = 17;
  private static final int TANK_W = 12;
  private static final int TANK_H = 52;

  private final InventoryPlayer playerInventory;
  private final ContainerAppliance container;
  private final String title;

  /**
   * Constructs the screen.
   *
   * @param playerInventory the player's inventory
   * @param container       the container
   * @param title           the title, the block's name
   */
  public GuiAppliance(InventoryPlayer playerInventory, ContainerAppliance container,
      String title) {
    super(container);
    this.playerInventory = playerInventory;
    this.container = container;
    this.title = title;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
    ApplianceSpec spec = container.getSpec();
    int x = mouseX - guiLeft;
    int y = mouseY - guiTop;
    if (spec.usesSupply() && spec.getSupplyHint() != null && x >= 55 && x < 73 && y >= 52
        && y < 70 && inventorySlots.getSlot(2).getStack().isEmpty()) {
      drawHoveringText(I18n.format(spec.getSupplyHint()), mouseX, mouseY);
    }
    if (spec.usesWater() && x >= TANK_X && x < TANK_X + TANK_W && y >= TANK_Y
        && y < TANK_Y + TANK_H) {
      List<String> lines = new ArrayList<>();
      lines.add(I18n.format("csm.furnishings.appliance.water", container.getWater(),
          spec.getWaterCapacity()));
      lines.add("§7" + I18n.format("csm.furnishings.appliance.water.hint"));
      drawHoveringText(lines, mouseX, mouseY);
    }
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    fontRenderer.drawString(title, xSize / 2 - fontRenderer.getStringWidth(title) / 2, 6,
        0x404040);
    fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8,
        ySize - 96 + 2, 0x404040);
    ApplianceSpec spec = container.getSpec();
    if (spec.usesWater() && container.getWater() <= 0) {
      String empty = I18n.format("csm.furnishings.appliance.water.empty");
      fontRenderer.drawString(empty, 116 + 8 - fontRenderer.getStringWidth(empty) / 2, 60,
          0xA02020);
    }
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(FURNACE);
    int x = guiLeft;
    int y = guiTop;
    drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    ApplianceSpec spec = container.getSpec();
    if (spec.usesFuel()) {
      int f = container.burnScaled(13);
      if (f > 0) {
        drawTexturedModalRect(x + 56, y + 36 + 12 - f, 176, 12 - f, 14, f + 1);
      }
    } else if (spec.usesSupply()) {
      // The supply slot stays where the fuel slot is; only the flame is painted out.
      drawTexturedModalRect(x + 56, y + 36, 8, 36, 14, 14);
    } else {
      // Paint out the input slot, the flame and the fuel slot with the blank panel to their
      // left, then draw the input slot again level with the output.
      drawTexturedModalRect(x + 55, y + 16, 8, 16, 18, 54);
      drawTexturedModalRect(x + 55, y + 34, 55, 16, 18, 18);
    }
    int k = container.progressScaled(24);
    if (k > 0) {
      drawTexturedModalRect(x + 79, y + 34, 176, 14, k + 1, 16);
    }
    if (spec.usesWater()) {
      drawTank(x + TANK_X, y + TANK_Y, spec);
    }
  }

  /** A sunken gauge in the slots' style, filled with water from the bottom. */
  private void drawTank(int x, int y, ApplianceSpec spec) {
    drawRect(x, y, x + TANK_W, y + TANK_H, 0xFF373737);
    drawRect(x + 1, y + 1, x + TANK_W, y + TANK_H, 0xFFFFFFFF);
    drawRect(x + 1, y + 1, x + TANK_W - 1, y + TANK_H - 1, 0xFF8B8B8B);
    int inner = TANK_H - 2;
    int fill = Math.min(inner, container.getWater() * inner / Math.max(1,
        spec.getWaterCapacity()));
    if (fill > 0) {
      drawRect(x + 1, y + 1 + inner - fill, x + TANK_W - 1, y + TANK_H - 1, 0xFF3F76E4);
      drawRect(x + 1, y + 1 + inner - fill, x + 3, y + TANK_H - 1, 0xFF6C9BF0);
    }
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
  }
}
