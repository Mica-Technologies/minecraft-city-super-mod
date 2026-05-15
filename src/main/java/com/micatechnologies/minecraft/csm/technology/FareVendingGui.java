package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Player-facing GUI for the {@link BlockFareVendingMachine}. Lists every
 * {@link FareVendingPurchase} as a button with its label and emerald cost; reload buttons
 * are only enabled when the player is holding a {@link ItemTransitCard}; every button is
 * disabled if the player can't afford the option.
 *
 * <p>Header line shows the player's current emerald total and (when applicable) the held
 * card's balance, both refreshed every frame so a successful purchase visibly updates them
 * before the player does anything else.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareVendingGui extends GuiScreen {

  private static final int BUTTON_ID_BASE = 100;
  private static final int BUTTON_ID_CLOSE = 0;

  private static final int COLOR_HEADER_BG = 0xFF1F2A3D;
  private static final int COLOR_BG = 0xFF26354F;
  private static final int COLOR_TEXT = 0xFFFFFFFF;
  private static final int COLOR_HEADER_TEXT = 0xFFE2EBFF;
  private static final int COLOR_INFO_DIM = 0xFFB8C0D0;

  private final BlockPos vendingPos;
  private GuiButton closeButton;
  private GuiButton[] purchaseButtons;

  public FareVendingGui(BlockPos vendingPos) {
    this.vendingPos = vendingPos;
  }

  @Override
  public void initGui() {
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();

    int panelW = 260;
    int panelH = 230;
    int panelX = (screenW - panelW) / 2;
    int panelY = (screenH - panelH) / 2;

    this.buttonList.clear();

    FareVendingPurchase[] options = FareVendingPurchase.values();
    purchaseButtons = new GuiButton[options.length];
    int btnY = panelY + 36;
    int btnH = 18;
    int btnGap = 2;
    for (int i = 0; i < options.length; i++) {
      FareVendingPurchase p = options[i];
      String label = p.label + "  —  " + p.costEmeralds + (p.costEmeralds == 1 ? " emerald" : " emeralds");
      GuiButton b = new GuiButton(BUTTON_ID_BASE + i, panelX + 10, btnY, panelW - 20, btnH, label);
      this.buttonList.add(b);
      purchaseButtons[i] = b;
      btnY += btnH + btnGap;
    }

    closeButton = new GuiButton(BUTTON_ID_CLOSE,
        panelX + (panelW - 80) / 2, panelY + panelH - 24, 80, 20, "Close");
    this.buttonList.add(closeButton);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();

    int panelW = 260;
    int panelH = 230;
    int panelX = (screenW - panelW) / 2;
    int panelY = (screenH - panelH) / 2;

    drawRect(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_BG);
    drawRect(panelX, panelY, panelX + panelW, panelY + 32, COLOR_HEADER_BG);

    EntityPlayer player = Minecraft.getMinecraft().player;
    int emeralds = countEmeralds(player);
    ItemStack heldCard = findHeldCard(player);
    int heldBalance = ItemTransitCard.getBalance(heldCard);

    fontRenderer.drawString("Fare Vending Machine", panelX + 10, panelY + 6,
        COLOR_HEADER_TEXT);
    String wallet = "Wallet: " + emeralds + (emeralds == 1 ? " emerald" : " emeralds");
    fontRenderer.drawString(wallet, panelX + 10, panelY + 18, COLOR_INFO_DIM);
    if (!heldCard.isEmpty()) {
      String balLabel = "Held card: " + heldBalance + (heldBalance == 1 ? " trip" : " trips");
      int w = fontRenderer.getStringWidth(balLabel);
      fontRenderer.drawString(balLabel, panelX + panelW - w - 10, panelY + 18, COLOR_INFO_DIM);
    }

    // Update button enabled-state every frame so a successful purchase / picking a card up
    // mid-GUI immediately re-evaluates which options the player can afford.
    if (purchaseButtons != null) {
      FareVendingPurchase[] options = FareVendingPurchase.values();
      for (int i = 0; i < options.length; i++) {
        FareVendingPurchase p = options[i];
        boolean canAfford = emeralds >= p.costEmeralds;
        boolean needsCard = p.kind == FareVendingPurchase.Kind.RELOAD;
        boolean hasCard = !heldCard.isEmpty();
        purchaseButtons[i].enabled = canAfford && (!needsCard || hasCard);
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private static int countEmeralds(EntityPlayer player) {
    if (player == null) return 0;
    InventoryPlayer inv = player.inventory;
    int total = 0;
    for (int i = 0; i < inv.mainInventory.size(); i++) {
      ItemStack s = inv.mainInventory.get(i);
      if (s.getItem() == Items.EMERALD) {
        total += s.getCount();
      }
    }
    return total;
  }

  private static ItemStack findHeldCard(EntityPlayer player) {
    if (player == null) return ItemStack.EMPTY;
    ItemStack mainHand = player.getHeldItemMainhand();
    if (mainHand.getItem() instanceof ItemTransitCard) {
      return mainHand;
    }
    return ItemStack.EMPTY;
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == BUTTON_ID_CLOSE) {
      this.mc.displayGuiScreen(null);
      return;
    }
    int idx = button.id - BUTTON_ID_BASE;
    FareVendingPurchase p = FareVendingPurchase.fromOrdinal(idx);
    if (p == null) {
      return;
    }
    CsmNetwork.sendToServer(new FareVendingPurchasePacket(vendingPos, p));
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
