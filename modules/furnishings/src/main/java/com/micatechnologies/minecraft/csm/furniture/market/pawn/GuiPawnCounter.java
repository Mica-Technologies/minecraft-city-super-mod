package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The pawn screen: nine offer slots over the player's inventory, the total the server quotes for
 * them, a button to pawn them, and (when the counter resells) one to its resale screen. A refused
 * slot is tinted red; hovering any offer slot shows what it pays or why not. Those who manage
 * pawn shops also get the resale switch.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiPawnCounter extends GuiContainer {

  private static final ResourceLocation CHEST =
      new ResourceLocation("textures/gui/container/generic_54.png");
  /** The chest texture's background grey, to cover its second and third rows. */
  private static final int PANEL = 0xFFC6C6C6;

  private static final int BTN_PAWN = 0;
  private static final int BTN_RESALE_SCREEN = 1;
  private static final int BTN_RESALE_SWITCH = 2;

  private final ContainerPawnCounter container;
  private final String title;
  private GuiButton pawnButton;
  private GuiButton resaleScreenButton;
  private GuiButton resaleSwitchButton;

  public GuiPawnCounter(ContainerPawnCounter container, String title) {
    super(container);
    this.container = container;
    this.title = title;
    this.xSize = 176;
    this.ySize = 166;
  }

  @Override
  public void initGui() {
    super.initGui();
    int y = guiTop + 54;
    pawnButton = new GuiButton(BTN_PAWN, guiLeft + 8, y, 78, 14,
        I18n.format("csm.pawn.gui.accept"));
    resaleScreenButton = new GuiButton(BTN_RESALE_SCREEN, guiLeft + 90, y, 78, 14,
        I18n.format("csm.pawn.gui.buy_back"));
    // the managers' switch, in the title row's right end
    resaleSwitchButton = new GuiButton(BTN_RESALE_SWITCH, guiLeft + xSize - 76, guiTop + 3, 70,
        12, "");
    buttonList.add(pawnButton);
    buttonList.add(resaleScreenButton);
    buttonList.add(resaleSwitchButton);
    updateButtons();
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    updateButtons();
  }

  private void updateButtons() {
    PawnQuotePacket q = container.getQuote();
    int flags = q == null ? 0 : q.flags;
    pawnButton.enabled = (flags & PawnQuotePacket.FLAG_CAN_PAWN) != 0;
    resaleScreenButton.visible = (flags & PawnQuotePacket.FLAG_RESALE) != 0;
    resaleSwitchButton.visible = (flags & PawnQuotePacket.FLAG_ADMIN) != 0;
    resaleSwitchButton.displayString = I18n.format((flags & PawnQuotePacket.FLAG_RESALE) != 0
        ? "csm.pawn.gui.resale_on" : "csm.pawn.gui.resale_off");
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    int action;
    switch (button.id) {
      case BTN_PAWN:
        action = PawnActionPacket.ACCEPT;
        break;
      case BTN_RESALE_SCREEN:
        action = PawnActionPacket.OPEN_RESALE;
        break;
      case BTN_RESALE_SWITCH:
        action = PawnActionPacket.TOGGLE_RESALE;
        break;
      default:
        return;
    }
    CsmFurnishings.NETWORK.sendToServer(
        new PawnActionPacket(container.getCounter().getPos(), action, 0, false));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
  }

  /** An offer slot's tooltip ends with what it pays, or why it is refused. */
  @Override
  @Nonnull
  public List<String> getItemToolTip(@Nonnull ItemStack stack) {
    List<String> lines = super.getItemToolTip(stack);
    Slot slot = getSlotUnderMouse();
    PawnQuotePacket q = container.getQuote();
    if (slot != null && slot.slotNumber < ContainerPawnCounter.OFFER_SLOTS && q != null
        && slot.slotNumber < q.lines.length && !q.lines[slot.slotNumber].isEmpty()) {
      lines.add((q.bad[slot.slotNumber] ? TextFormatting.RED : TextFormatting.GREEN)
          + text(q.lines[slot.slotNumber]));
    }
    return lines;
  }

  static String text(String json) {
    try {
      ITextComponent c = ITextComponent.Serializer.jsonToComponent(json);
      return c == null ? "" : c.getFormattedText();
    } catch (RuntimeException e) {
      return "";
    }
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    PawnQuotePacket flags = container.getQuote();
    boolean admin = flags != null && (flags.flags & PawnQuotePacket.FLAG_ADMIN) != 0;
    fontRenderer.drawString(fontRenderer.trimStringToWidth(title, admin ? xSize - 86 : xSize - 16),
        8, 6, 0x404040);
    fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    PawnQuotePacket q = container.getQuote();
    String header = q == null ? I18n.format("csm.pawn.gui.quoting") : text(q.header);
    fontRenderer.drawString(fontRenderer.trimStringToWidth(header, xSize - 16), 8, 41,
        0x404040);
    if (q != null) {
      for (int i = 0; i < ContainerPawnCounter.OFFER_SLOTS && i < q.bad.length; i++) {
        if (q.bad[i]) {
          Slot s = container.inventorySlots.get(i);
          GlStateManager.disableDepth();
          drawRect(s.xPos, s.yPos, s.xPos + 16, s.yPos + 16, 0x60FF2020);
          GlStateManager.enableDepth();
        }
      }
    }
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(CHEST);
    // a three-row chest's top, its second and third rows painted over, and its inventory
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 71);
    drawTexturedModalRect(guiLeft, guiTop + 71, 0, 126, xSize, 96);
    drawRect(guiLeft + 7, guiTop + 35, guiLeft + 169, guiTop + 71, PANEL);
  }
}
