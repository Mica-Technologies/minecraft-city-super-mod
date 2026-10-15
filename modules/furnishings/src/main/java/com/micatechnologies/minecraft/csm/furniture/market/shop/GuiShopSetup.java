package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * A shop's setup screen: its stock as a three-row chest on the left, and beside it a panel with
 * the item for sale, what a sale moves and costs, what a buy-back moves and pays, the till, and
 * the switches (decorative for everyone who manages it; unlimited stock and server shop for those
 * allowed, as {@link ShopStatusPacket} says).
 *
 * <p>Prices are typed in the currency in force: money with cents, or whole emeralds. Nothing is
 * changed until Apply; the server clamps what it is sent.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiShopSetup extends GuiContainer {

  private static final ResourceLocation CHEST =
      new ResourceLocation("textures/gui/container/generic_54.png");

  private static final int PANEL_W = 160;
  private static final int BTN_APPLY = 0;
  private static final int BTN_WITHDRAW = 1;
  private static final int BTN_DEPOSIT = 2;
  private static final int BTN_DECORATIVE = 3;
  private static final int BTN_INFINITE = 4;
  private static final int BTN_SERVER = 5;

  private final InventoryPlayer playerInventory;
  private final TileEntityShop shop;
  private final String title;

  private GuiTextField sellAmount;
  private GuiTextField sellPrice;
  private GuiTextField buyAmount;
  private GuiTextField buyPrice;
  private GuiTextField deposit;
  private GuiButton withdrawButton;
  private GuiButton depositButton;
  private GuiButton decorativeButton;
  private GuiButton infiniteButton;
  private GuiButton serverButton;
  private int ticksOpen;

  /**
   * Constructs the screen.
   *
   * @param playerInventory the player's inventory
   * @param container       the container
   * @param title           the block's name
   */
  public GuiShopSetup(InventoryPlayer playerInventory, ContainerShopSetup container,
      String title) {
    super(container);
    this.playerInventory = playerInventory;
    this.shop = container.getShop();
    this.title = title;
    this.xSize = ContainerShopSetup.PANEL_X + PANEL_W;
    this.ySize = 166;
  }

  @Override
  public void initGui() {
    super.initGui();
    Keyboard.enableRepeatEvents(true);
    int px = guiLeft + ContainerShopSetup.PANEL_X;
    boolean money = shop.isMoneyMode();
    sellAmount = field(0, px + 30, guiTop + 40, 22, String.valueOf(shop.getSellAmount()));
    sellPrice = field(1, px + 72, guiTop + 40, 80, price(shop.getSellPrice(money), money));
    buyAmount = field(2, px + 30, guiTop + 58, 22, String.valueOf(shop.getBuyAmount()));
    buyPrice = field(3, px + 72, guiTop + 58, 80, price(shop.getBuyPrice(money), money));
    deposit = field(4, px + 64, guiTop + 113, 62, "");
    buttonList.clear();
    buttonList.add(new GuiButton(BTN_APPLY, px + 6, guiTop + 74, PANEL_W - 12, 14,
        I18n.format("csm.shop.gui.apply")));
    withdrawButton = new GuiButton(BTN_WITHDRAW, px + 6, guiTop + 112, 56, 14,
        I18n.format("csm.shop.gui.withdraw"));
    depositButton = new GuiButton(BTN_DEPOSIT, px + 128, guiTop + 112, 26, 14,
        I18n.format("csm.shop.gui.deposit"));
    decorativeButton = new GuiButton(BTN_DECORATIVE, px + 6, guiTop + 130, PANEL_W - 12, 14, "");
    infiniteButton = new GuiButton(BTN_INFINITE, px + 6, guiTop + 147, 73, 14, "");
    serverButton = new GuiButton(BTN_SERVER, px + 81, guiTop + 147, 73, 14, "");
    buttonList.add(withdrawButton);
    buttonList.add(depositButton);
    buttonList.add(decorativeButton);
    buttonList.add(infiniteButton);
    buttonList.add(serverButton);
    send(ShopActionPacket.REFRESH, 0, 0);
  }

  private GuiTextField field(int id, int x, int y, int w, String text) {
    GuiTextField f = new GuiTextField(id, fontRenderer, x, y, w, 12);
    f.setMaxStringLength(12);
    f.setText(text);
    return f;
  }

  private static String price(double value, boolean money) {
    return money ? String.format(Locale.ROOT, "%.2f", value) : String.valueOf((long) value);
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Keyboard.enableRepeatEvents(false);
    ShopClientStatus.clear();
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    ShopStatusPacket status = ShopClientStatus.get(shop.getPos());
    boolean server = shop.isServerShop();
    withdrawButton.enabled = !server && shop.isOwner(mc.player);
    depositButton.enabled = !server;
    deposit.setEnabled(!server);
    decorativeButton.displayString = I18n.format(shop.isDecorativeSelf()
        ? "csm.shop.gui.decorative_on" : "csm.shop.gui.decorative_off");
    infiniteButton.displayString = I18n.format(shop.isInfinite()
        ? "csm.shop.gui.infinite_on" : "csm.shop.gui.infinite_off");
    serverButton.displayString = I18n.format(server
        ? "csm.shop.gui.server_on" : "csm.shop.gui.server_off");
    infiniteButton.visible = status != null && status.canInfinite();
    serverButton.visible = status != null && status.canServer();
    for (GuiTextField f : fields()) {
      f.updateCursorCounter();
    }
    // The stock changes as the owner moves items in and out; ask again once a second.
    if (++ticksOpen % 20 == 0) {
      send(ShopActionPacket.REFRESH, 0, 0);
    }
  }

  private GuiTextField[] fields() {
    return new GuiTextField[]{sellAmount, sellPrice, buyAmount, buyPrice, deposit};
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BTN_APPLY:
        send(ShopActionPacket.SET_SELL, parseInt(sellAmount, shop.getSellAmount()),
            parseDouble(sellPrice, shop.getSellPrice(shop.isMoneyMode())));
        send(ShopActionPacket.SET_BUY, parseInt(buyAmount, shop.getBuyAmount()),
            parseDouble(buyPrice, shop.getBuyPrice(shop.isMoneyMode())));
        break;
      case BTN_WITHDRAW:
        send(ShopActionPacket.WITHDRAW, 0, 0);
        break;
      case BTN_DEPOSIT:
        double amount = parseDouble(deposit, 0);
        if (amount > 0) {
          send(ShopActionPacket.DEPOSIT, 0, amount);
          deposit.setText("");
        }
        break;
      case BTN_DECORATIVE:
        send(ShopActionPacket.SET_DECORATIVE, shop.isDecorativeSelf() ? 0 : 1, 0);
        break;
      case BTN_INFINITE:
        send(ShopActionPacket.SET_INFINITE, shop.isInfinite() ? 0 : 1, 0);
        break;
      case BTN_SERVER:
        send(ShopActionPacket.SET_SERVER, shop.isServerShop() ? 0 : 1, 0);
        break;
      default:
        break;
    }
  }

  private void send(int action, int count, double value) {
    CsmFurnishings.NETWORK.sendToServer(new ShopActionPacket(shop.getPos(), action, count,
        value));
  }

  private static int parseInt(GuiTextField f, int fallback) {
    try {
      return Integer.parseInt(f.getText().trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static double parseDouble(GuiTextField f, double fallback) {
    try {
      double d = Double.parseDouble(f.getText().trim().replace(',', '.'));
      return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    for (GuiTextField f : fields()) {
      if (f.isFocused()) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
          f.setFocused(false);
          return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
          focusNext(f);
          return;
        }
        f.textboxKeyTyped(typedChar, keyCode);
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  private void focusNext(GuiTextField current) {
    GuiTextField[] all = fields();
    for (int i = 0; i < all.length; i++) {
      if (all[i] == current) {
        current.setFocused(false);
        all[(i + 1) % all.length].setFocused(true);
        return;
      }
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField f : fields()) {
      f.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    GlStateManager.disableLighting();
    for (GuiTextField f : fields()) {
      f.drawTextBox();
    }
    renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    int px = ContainerShopSetup.PANEL_X;
    ShopStatusPacket shown = ShopClientStatus.get(shop.getPos());
    int stockRoom = shown != null && !shop.isInfinite() ? fontRenderer.getStringWidth(
        I18n.format("csm.shop.gui.in_stock", shown.getStock())) + 6 : 0;
    fontRenderer.drawString(fit(title, 162 - stockRoom), 8, 6, 0x404040);
    fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(), 8,
        ySize - 96 + 2, 0x404040);
    boolean money = shop.isMoneyMode();
    fontRenderer.drawString(I18n.format("csm.shop.gui.for_sale"), px + 28, 7, 0x404040);
    String currency = money ? I18n.format("csm.shop.gui.in_money", shop.getSymbol())
        : I18n.format("csm.shop.gui.in_emeralds");
    fontRenderer.drawString(fit(currency, PANEL_W - 30), px + 28, 22, 0x707070);
    fontRenderer.drawString(I18n.format("csm.shop.gui.sell"), px + 6, 42, 0x404040);
    fontRenderer.drawString("×", px + 56, 42, 0x404040);
    fontRenderer.drawString(I18n.format("csm.shop.gui.buy"), px + 6, 60, 0x404040);
    fontRenderer.drawString("×", px + 56, 60, 0x404040);
    ShopStatusPacket status = ShopClientStatus.get(shop.getPos());
    String till;
    if (shop.isServerShop()) {
      till = I18n.format("csm.shop.gui.till_server");
    } else if (status == null) {
      till = I18n.format("csm.shop.gui.till", "...");
    } else {
      String amount = money ? ShopEconomy.format(true, shop.getSymbol(), status.getTillMoney())
          : ShopEconomy.format(false, "", status.getTillEmeralds());
      till = I18n.format("csm.shop.gui.till", amount);
    }
    fontRenderer.drawString(fit(till, PANEL_W - 10), px + 6, 98, 0x404040);
    if (status != null && !shop.isInfinite()) {
      String stock = I18n.format("csm.shop.gui.in_stock", status.getStock());
      fontRenderer.drawString(stock, 170 - fontRenderer.getStringWidth(stock), 6, 0x404040);
    }
  }

  private String fit(String text, int width) {
    return fontRenderer.trimStringToWidth(text, width);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(CHEST);
    // The three-row chest: its title bar and three rows of slots, then its inventory panel.
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, ContainerShopSetup.PANEL_X, 3 * 18 + 17);
    drawTexturedModalRect(guiLeft, guiTop + 3 * 18 + 17, 0, 126, ContainerShopSetup.PANEL_X, 96);
    // The panel: the chest's grey with its bevel, and one slot for the template.
    int px = guiLeft + ContainerShopSetup.PANEL_X - 3;
    int w = PANEL_W + 3;
    Gui.drawRect(px, guiTop, px + w, guiTop + ySize, 0xFF000000);
    Gui.drawRect(px + 1, guiTop + 1, px + w - 1, guiTop + ySize - 1, 0xFFFFFFFF);
    Gui.drawRect(px + 2, guiTop + 2, px + w - 1, guiTop + ySize - 1, 0xFF555555);
    Gui.drawRect(px + 2, guiTop + 2, px + w - 2, guiTop + ySize - 2, 0xFFC6C6C6);
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    mc.getTextureManager().bindTexture(CHEST);
    drawTexturedModalRect(guiLeft + ContainerShopSetup.TEMPLATE_X - 1,
        guiTop + ContainerShopSetup.TEMPLATE_Y - 1, 7, 17, 18, 18);
  }
}
