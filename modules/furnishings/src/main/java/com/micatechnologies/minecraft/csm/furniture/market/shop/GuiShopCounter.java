package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A shop's counter, as a customer sees it: the item, what a sale is (count and price) with a Buy
 * button, what the shop pays to buy it back with a Sell button, how much is in stock, what the
 * customer has to spend, and who runs the shop. A decorative shop shows the same with the buttons
 * off. A shop's managers also get a button to its setup.
 *
 * <p>What the shop is comes from its sync; the customer's balance and the exact stock from
 * {@link ShopStatusPacket}, which the server sends as the screen opens and after every trade.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiShopCounter extends GuiContainer {

  private static final int W = 200;
  private static final int H = 132;
  private static final int BTN_BUY = 0;
  private static final int BTN_SELL = 1;
  private static final int BTN_SETUP = 2;

  private final TileEntityShop shop;
  private final String title;
  private int left;
  private int top;
  private GuiButton buy;
  private GuiButton sell;
  private GuiButton setup;

  /**
   * Constructs the screen.
   *
   * @param container the (empty) container
   * @param title     the block's name
   */
  public GuiShopCounter(ContainerShopCounter container, String title) {
    super(container);
    this.shop = container.getShop();
    this.title = title;
    this.xSize = W;
    this.ySize = H;
  }

  @Override
  public void initGui() {
    super.initGui();
    left = guiLeft;
    top = guiTop;
    buttonList.clear();
    buy = new GuiButton(BTN_BUY, left + W - 66, top + 52, 58, 16,
        I18n.format("csm.shop.gui.buy_button"));
    sell = new GuiButton(BTN_SELL, left + W - 66, top + 74, 58, 16,
        I18n.format("csm.shop.gui.sell_button"));
    setup = new GuiButton(BTN_SETUP, left + W - 66, top + H - 22, 58, 14,
        I18n.format("csm.shop.gui.setup_button"));
    buttonList.add(buy);
    buttonList.add(sell);
    buttonList.add(setup);
    updateScreen();
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    ShopStatusPacket status = ShopClientStatus.get(shop.getPos());
    boolean trades = !shop.isDecorative() && shop.isSetUp();
    boolean money = shop.isMoneyMode();
    buy.enabled = trades && shop.getShownStock() >= shop.getSellAmount();
    sell.visible = shop.buysBack(money);
    sell.enabled = trades;
    setup.visible = status != null && status.canManage();
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    int action;
    switch (button.id) {
      case BTN_BUY:
        action = ShopActionPacket.BUY;
        break;
      case BTN_SELL:
        action = ShopActionPacket.SELL;
        break;
      case BTN_SETUP:
        action = ShopActionPacket.OPEN_SETUP;
        break;
      default:
        return;
    }
    CsmFurnishings.NETWORK.sendToServer(new ShopActionPacket(shop.getPos(), action, 0, 0));
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    ShopClientStatus.clear();
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    // Everything is drawn with the background, in screen space.
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    // The vanilla window: black edge, white and grey bevel, light grey face.
    Gui.drawRect(left, top, left + W, top + H, 0xFF000000);
    Gui.drawRect(left + 1, top + 1, left + W - 1, top + H - 1, 0xFFFFFFFF);
    Gui.drawRect(left + 2, top + 2, left + W - 1, top + H - 1, 0xFF555555);
    Gui.drawRect(left + 2, top + 2, left + W - 2, top + H - 2, 0xFFC6C6C6);

    fontRenderer.drawString(fontRenderer.trimStringToWidth(title, W - 16), left + 8, top + 7,
        0x404040);
    String who = shop.isServerShop() ? I18n.format("csm.shop.gui.server_shop")
        : I18n.format("csm.shop.gui.owner", shop.getOwnerName());
    fontRenderer.drawString(fontRenderer.trimStringToWidth(who, W - 16), left + 8, top + 18,
        0x707070);

    ItemStack item = shop.getTemplate();
    boolean money = shop.isMoneyMode();
    String symbol = shop.getSymbol();
    if (item.isEmpty()) {
      fontRenderer.drawSplitString(I18n.format("csm.shop.gui.not_open"), left + 8, top + 40,
          W - 16, 0x404040);
    } else {
      GlStateManager.pushMatrix();
      GlStateManager.translate(left + 10, top + 34, 0);
      GlStateManager.scale(2.0F, 2.0F, 1.0F);
      RenderHelper.enableGUIStandardItemLighting();
      itemRender.renderItemAndEffectIntoGUI(item, 0, 0);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.popMatrix();
      fontRenderer.drawString(fontRenderer.trimStringToWidth(item.getDisplayName(), W - 56),
          left + 48, top + 38, 0x202020);
      String sale = I18n.format("csm.shop.gui.sale", shop.getSellAmount(),
          ShopEconomy.format(money, symbol, shop.getSellPrice(money)));
      fontRenderer.drawString(sale, left + 8, top + 56, 0x404040);
      if (shop.buysBack(money)) {
        String back = I18n.format("csm.shop.gui.buyback", shop.getBuyAmount(),
            ShopEconomy.format(money, symbol, shop.getBuyPrice(money)));
        fontRenderer.drawString(back, left + 8, top + 78, 0x404040);
      }
    }

    ShopStatusPacket status = ShopClientStatus.get(shop.getPos());
    String stock = shop.isInfinite() ? I18n.format("csm.shop.gui.always_in_stock")
        : I18n.format("csm.shop.gui.in_stock", status != null ? status.getStock()
            : shop.getShownStock());
    fontRenderer.drawString(stock, left + 8, top + 98, 0x404040);
    String balance;
    if (status == null) {
      balance = "";
    } else if (money) {
      balance = Double.isNaN(status.getBalance()) ? ""
          : I18n.format("csm.shop.gui.balance", ShopEconomy.format(true, symbol,
              status.getBalance()));
    } else {
      balance = I18n.format("csm.shop.gui.balance", ShopEconomy.format(false, "",
          status.getEmeralds()));
    }
    fontRenderer.drawString(balance, left + 8, top + 110, 0x404040);
    if (shop.isDecorative()) {
      fontRenderer.drawString(I18n.format("csm.shop.gui.decorative"), left + 8, top + H - 12,
          0xA04040);
    }
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
  }
}
