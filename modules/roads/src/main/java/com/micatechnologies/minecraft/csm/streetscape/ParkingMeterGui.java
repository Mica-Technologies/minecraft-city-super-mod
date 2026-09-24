package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * A parking meter's or pay station's screen: the time left on a space, buttons to pay for more,
 * and for the owner or an operator the meter's settings and its takings.
 *
 * <p>Whether the settings show is decided here from the owner the meter synced and the player's
 * own permission level, and checked again by the server, which ignores a settings packet from
 * anyone else. Paying takes emeralds from the inventory or money through the economy, whichever
 * the server says the meter uses.</p>
 *
 * @version 1.0
 */
@SideOnly(Side.CLIENT)
public class ParkingMeterGui extends GuiScreen {

  private static final int ID_SPACE_PREV = 0;
  private static final int ID_SPACE_NEXT = 1;
  private static final int ID_PAY_ONE = 2;
  private static final int ID_PAY_HOUR = 3;
  private static final int ID_PAY_MAX = 4;
  private static final int ID_COLLECT_MODE = 5;
  private static final int ID_SAVE = 6;
  private static final int ID_TAKE = 7;
  private static final int ID_DONE = 8;

  private static final int W = 220;
  private static final int ROW = 22;
  private static final int FIELD_W = 44;

  private final TileEntityParkingMeter meter;
  private final BlockPos pos;
  private final boolean station;
  private final boolean owner;

  private int space;
  private boolean collect;
  private GuiButton collectButton;
  private GuiButton takeButton;
  private GuiTextField emeraldsField;
  private GuiTextField minutesField;
  private GuiTextField moneyField;
  private GuiTextField maxField;
  private GuiTextField spacesField;

  public ParkingMeterGui(TileEntityParkingMeter meter, boolean station) {
    this.meter = meter;
    this.pos = meter.getPos();
    this.station = station;
    net.minecraft.client.entity.EntityPlayerSP me = net.minecraft.client.Minecraft
        .getMinecraft().player;
    this.owner = me != null && ((meter.getOwner() != null
        && meter.getOwner().equals(me.getUniqueID())) || me.canUseCommand(2, ""));
    this.collect = meter.isCollect();
  }

  private int top() {
    return height / 2 - (owner ? 110 : 55);
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int left = width / 2 - W / 2;
    int y = top() + 30;
    if (meter.getSpaces() > 1) {
      buttonList.add(new GuiButton(ID_SPACE_PREV, left, y, 20, 20, "<"));
      buttonList.add(new GuiButton(ID_SPACE_NEXT, left + W - 20, y, 20, 20, ">"));
    }
    y += ROW + 12;
    int third = (W - 8) / 3;
    buttonList.add(new GuiButton(ID_PAY_ONE, left, y, third, 20,
        "+" + ParkingPayments.duration(meter.getMinutesPerBlock())));
    buttonList.add(new GuiButton(ID_PAY_HOUR, left + third + 4, y, third, 20, "+1 hour"));
    buttonList.add(new GuiButton(ID_PAY_MAX, left + 2 * (third + 4), y, third, 20, "To max"));
    y += ROW + 16;
    if (owner) {
      y += 12;
      emeraldsField = field(left + W / 2 - FIELD_W - 2, y,
          String.valueOf(meter.getEmeraldsPerBlock()));
      minutesField = field(left + W - FIELD_W, y, String.valueOf(meter.getMinutesPerBlock()));
      y += ROW;
      moneyField = field(left + W / 2 - FIELD_W - 2, y,
          String.format(Locale.ROOT, "%.2f", meter.getMoneyPerBlock()));
      maxField = field(left + W - FIELD_W, y, String.valueOf(meter.getMaxMinutes()));
      y += ROW;
      collectButton = new GuiButton(ID_COLLECT_MODE, left, y, W / 2 - 4, 20, "");
      buttonList.add(collectButton);
      if (station) {
        spacesField = field(left + W - FIELD_W, y, String.valueOf(meter.getSpaces()));
      }
      y += ROW + 4;
      buttonList.add(new GuiButton(ID_SAVE, left, y, W / 2 - 2, 20, "Save settings"));
      takeButton = new GuiButton(ID_TAKE, left + W / 2 + 2, y, W / 2 - 2, 20, "");
      buttonList.add(takeButton);
      y += ROW + 4;
    }
    buttonList.add(new GuiButton(ID_DONE, width / 2 - 50, y, 100, 20, "Done"));
  }

  private GuiTextField field(int x, int y, String text) {
    GuiTextField f = new GuiTextField(100 + buttonList.size(), fontRenderer, x, y, FIELD_W, 18);
    f.setMaxStringLength(8);
    f.setText(text);
    return f;
  }

  private GuiTextField[] fields() {
    return owner ? new GuiTextField[]{emeraldsField, minutesField, moneyField, maxField,
        spacesField} : new GuiTextField[0];
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case ID_SPACE_PREV:
        space = (space + meter.getSpaces() - 1) % meter.getSpaces();
        break;
      case ID_SPACE_NEXT:
        space = (space + 1) % meter.getSpaces();
        break;
      case ID_PAY_ONE:
        pay(1);
        break;
      case ID_PAY_HOUR:
        pay(Math.max(1, (60 + meter.getMinutesPerBlock() - 1) / meter.getMinutesPerBlock()));
        break;
      case ID_PAY_MAX:
        pay(Math.max(1, meter.blocksToMax(space, System.currentTimeMillis())));
        break;
      case ID_COLLECT_MODE:
        collect = !collect;
        break;
      case ID_SAVE:
        CsmRoads.NETWORK.sendToServer(new ParkingMeterSettingsPacket(pos,
            intOf(emeraldsField, meter.getEmeraldsPerBlock()),
            intOf(minutesField, meter.getMinutesPerBlock()),
            intOf(maxField, meter.getMaxMinutes()),
            doubleOf(moneyField, meter.getMoneyPerBlock()), collect,
            spacesField != null ? intOf(spacesField, meter.getSpaces()) : meter.getSpaces()));
        break;
      case ID_TAKE:
        CsmRoads.NETWORK.sendToServer(
            new ParkingMeterActionPacket(pos, ParkingMeterActionPacket.COLLECT, 0, 0));
        break;
      case ID_DONE:
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  private void pay(int blocks) {
    CsmRoads.NETWORK.sendToServer(
        new ParkingMeterActionPacket(pos, ParkingMeterActionPacket.PAY, space, blocks));
  }

  private static int intOf(GuiTextField field, int fallback) {
    try {
      return Integer.parseInt(field.getText().trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static double doubleOf(GuiTextField field, double fallback) {
    try {
      double v = Double.parseDouble(field.getText().trim().replace("$", ""));
      return Double.isNaN(v) || Double.isInfinite(v) ? fallback : v;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    for (GuiTextField f : fields()) {
      if (f != null && f.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField f : fields()) {
      if (f != null) {
        f.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField f : fields()) {
      if (f != null) {
        f.updateCursorCounter();
      }
    }
    if (space >= meter.getSpaces()) {
      space = 0;
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    if (collectButton != null) {
      collectButton.displayString = collect ? "Keeps takings" : "Takings vanish";
    }
    if (takeButton != null) {
      // The takings are on the button itself: a line under it would sit behind the Done button
      // on a small screen.
      String held = meter.getStoredMoney() > 0 ? ParkingPayments.money(meter.getStoredMoney())
          : String.valueOf(meter.getStoredEmeralds());
      takeButton.displayString = "Collect (" + held + ")";
    }
    int left = width / 2 - W / 2;
    int y = top();
    drawCenteredString(fontRenderer, station ? "Pay Station" : "Parking Meter", width / 2, y,
        0xFFFFFF);
    String rate = meter.isMoneyMode()
        ? ParkingPayments.money(meter.getMoneyPerBlock())
        : ParkingPayments.emeralds(meter.getEmeraldsPerBlock());
    drawCenteredString(fontRenderer, rate + " per "
            + ParkingPayments.duration(meter.getMinutesPerBlock()) + ", up to "
            + ParkingPayments.duration(meter.getMaxMinutes()), width / 2, y + 13, 0xA0A0A0);
    long now = System.currentTimeMillis();
    String spaceName = meter.getSpaces() > 1 ? "Space " + (space + 1) + ": " : "";
    String left1 = meter.isExpired(space, now) ? "EXPIRED"
        : ParkingPayments.remaining(meter, space, now) + " left";
    drawCenteredString(fontRenderer, spaceName + left1, width / 2, y + 36,
        meter.isExpired(space, now) ? 0xFF5050 : 0x80FF80);
    super.drawScreen(mouseX, mouseY, partialTicks);
    if (owner) {
      int oy = y + 30 + ROW + 12 + ROW + 16;
      drawString(fontRenderer, "Owner settings" + (meter.getOwnerName().isEmpty() ? ""
          : " (" + meter.getOwnerName() + ")"), left, oy, 0xFFD070);
      oy += 12;
      drawString(fontRenderer, "Emeralds", left, oy + 5, 0xE0E0E0);
      drawString(fontRenderer, "Minutes", left + W / 2 + 4, oy + 5, 0xE0E0E0);
      oy += ROW;
      drawString(fontRenderer, "Money $", left, oy + 5, 0xE0E0E0);
      drawString(fontRenderer, "Max min", left + W / 2 + 4, oy + 5, 0xE0E0E0);
      oy += ROW;
      if (station) {
        drawString(fontRenderer, "Spaces", left + W / 2 + 4, oy + 5, 0xE0E0E0);
      }
      for (GuiTextField f : fields()) {
        if (f != null) {
          f.drawTextBox();
        }
      }
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
