package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The resale screen: a reselling counter's stock, a click buying one of a slot's items and a
 * shift-click the whole stack, each slot's price in its tooltip, and a button back to the pawn
 * screen.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiPawnResale extends GuiContainer {

  private static final ResourceLocation CHEST =
      new ResourceLocation("textures/gui/container/generic_54.png");

  private final ContainerPawnResale container;
  private final String title;

  public GuiPawnResale(ContainerPawnResale container, String title) {
    super(container);
    this.container = container;
    this.title = title;
    this.xSize = 176;
    this.ySize = 166;
  }

  @Override
  public void initGui() {
    super.initGui();
    buttonList.add(new GuiButton(0, guiLeft + xSize - 48, guiTop + 3, 42, 12,
        I18n.format("csm.pawn.gui.back")));
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    CsmFurnishings.NETWORK.sendToServer(new PawnActionPacket(container.getCounter().getPos(),
        PawnActionPacket.OPEN_PAWN, 0, false));
  }

  /** A left click on a stocked slot buys from it (shift: the whole stack); nothing is picked up. */
  @Override
  protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
    if (slot != null && slot.slotNumber < ContainerPawnResale.STOCK_SLOTS) {
      if (slot.getHasStack() && mouseButton == 0
          && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
        CsmFurnishings.NETWORK.sendToServer(new PawnActionPacket(
            container.getCounter().getPos(), PawnActionPacket.BUY, slot.getSlotIndex(),
            type == ClickType.QUICK_MOVE));
      }
      return;
    }
    super.handleMouseClick(slot, slotId, mouseButton, type);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  @Nonnull
  public List<String> getItemToolTip(@Nonnull ItemStack stack) {
    List<String> lines = super.getItemToolTip(stack);
    Slot slot = getSlotUnderMouse();
    PawnQuotePacket q = container.getQuote();
    if (slot != null && slot.slotNumber < ContainerPawnResale.STOCK_SLOTS && q != null
        && slot.slotNumber < q.lines.length && !q.lines[slot.slotNumber].isEmpty()) {
      lines.add(TextFormatting.GOLD + GuiPawnCounter.text(q.lines[slot.slotNumber]));
      lines.add(TextFormatting.GRAY + I18n.format("csm.pawn.gui.buy_hint"));
    }
    return lines;
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    fontRenderer.drawString(fontRenderer.trimStringToWidth(title, xSize - 60), 8, 6, 0x404040);
    fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(CHEST);
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 71);
    drawTexturedModalRect(guiLeft, guiTop + 71, 0, 126, xSize, 96);
  }
}
