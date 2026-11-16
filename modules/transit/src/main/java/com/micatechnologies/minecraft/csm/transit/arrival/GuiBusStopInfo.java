package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.transit.CsmTransit;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The editor an arrival board and the info totem share: the agency (a button stepping through
 * the four built-in agencies, METRO and a custom one, whose name and two colours then get fields),
 * the stop's name, the direction line, the board's own routes, and the destinations, closed and
 * two-sided switches. Save sends it all in one packet ({@link BusStopInfoPacket}); Cancel or
 * Escape leaves the board as it was.
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class GuiBusStopInfo extends GuiScreen {

  private static final int W = 300;
  private static final int ID_AGENCY = 0;
  private static final int ID_DESTINATIONS = 1;
  private static final int ID_CLOSED = 2;
  private static final int ID_TWO_SIDED = 3;
  private static final int ID_SAVE = 10;
  private static final int ID_CANCEL = 11;
  private static final int CAPTION = 0xFFD070;

  private final BlockPos pos;
  private final String title;
  private final boolean wall;
  /** The values being edited: a copy, sent on Save. */
  private final BusStopInfo edit = new BusStopInfo();
  private boolean twoSided;

  private GuiTextField customName;
  private GuiTextField customGround;
  private GuiTextField customLegend;
  private GuiTextField stopName;
  private GuiTextField direction;
  private GuiTextField routes;
  private GuiTextField[] fields;
  private GuiButton agencyButton;
  private GuiButton destinationsButton;
  private GuiButton closedButton;
  private GuiButton twoSidedButton;

  /**
   * An editor for a board.
   *
   * @param te the board's tile entity
   */
  public GuiBusStopInfo(TileEntityBusArrivalBoard te) {
    this.pos = te.getPos();
    IBlockState state = te.getWorld().getBlockState(pos);
    this.title = I18n.format(state.getBlock().getTranslationKey() + ".name");
    this.wall = state.getBlock() instanceof BlockBusArrivalBoard
        && state.getValue(BlockBusArrivalBoard.MOUNT) == BoardMount.WALL;
    edit.copyFrom(te.getInfo());
    this.twoSided = edit.isTwoSided(wall);
  }

  private int left() {
    return width / 2 - W / 2;
  }

  private int top() {
    return Math.max(24, height / 2 - 112);
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int x = left();
    int y = top();
    agencyButton = new GuiButton(ID_AGENCY, x, y, 120, 20, "");
    buttonList.add(agencyButton);
    customName = field(x + 124, y, 80, BusStopInfo.MAX_AGENCY_NAME, edit.getCustomName());
    customGround = field(x + 208, y, 44, 6, hex(edit.getCustomGround()));
    customLegend = field(x + 256, y, 44, 6, hex(edit.getCustomLegend()));
    y += 46;
    stopName = field(x, y, W, BusStopInfo.MAX_STOP_NAME, edit.getStopName());
    y += 34;
    direction = field(x, y, W, BusStopInfo.MAX_DIRECTION, edit.getDirection());
    y += 34;
    routes = field(x, y, W, BusStopInfo.MAX_ROUTES, edit.getRoutes());
    y += 26;
    int bw = (W - 8) / 3;
    destinationsButton = new GuiButton(ID_DESTINATIONS, x, y, bw, 20, "");
    closedButton = new GuiButton(ID_CLOSED, x + bw + 4, y, bw, 20, "");
    twoSidedButton = new GuiButton(ID_TWO_SIDED, x + 2 * (bw + 4), y, bw, 20, "");
    twoSidedButton.enabled = !wall;
    buttonList.add(destinationsButton);
    buttonList.add(closedButton);
    buttonList.add(twoSidedButton);
    y += 28;
    buttonList.add(new GuiButton(ID_SAVE, width / 2 - 104, y, 100, 20,
        I18n.format("csm.transit.arrival_board.save")));
    buttonList.add(new GuiButton(ID_CANCEL, width / 2 + 4, y, 100, 20,
        I18n.format("gui.cancel")));
    fields = new GuiTextField[]{stopName, direction, routes, customName, customGround,
        customLegend};
    stopName.setFocused(true);
    labels();
  }

  private GuiTextField field(int x, int y, int w, int max, String text) {
    GuiTextField f = new GuiTextField(0, fontRenderer, x, y, w, 20);
    f.setMaxStringLength(max);
    f.setText(text);
    return f;
  }

  private static String hex(int rgb) {
    return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
  }

  private boolean custom() {
    return edit.getAgency() == BoardAgency.CUSTOM;
  }

  private void labels() {
    BoardAgency a = edit.getAgency();
    String name = a == BoardAgency.CUSTOM ? I18n.format("csm.transit.arrival_board.custom")
        : a.title(edit);
    agencyButton.displayString = I18n.format("csm.transit.arrival_board.agency", name);
    destinationsButton.displayString = I18n.format("csm.transit.arrival_board.destinations",
        onOff(edit.isDestinations()));
    closedButton.displayString = I18n.format("csm.transit.arrival_board.closed",
        onOff(edit.isClosed()));
    twoSidedButton.displayString = I18n.format("csm.transit.arrival_board.two_sided",
        onOff(twoSided && !wall));
  }

  private static String onOff(boolean on) {
    return I18n.format(on ? "options.on" : "options.off");
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case ID_AGENCY: {
        int n = BoardAgency.values().length;
        int next = (edit.getAgency().ordinal() + (isShiftKeyDown() ? n - 1 : 1)) % n;
        edit.setAgency(BoardAgency.byOrdinal(next));
        break;
      }
      case ID_DESTINATIONS:
        edit.setDestinations(!edit.isDestinations());
        break;
      case ID_CLOSED:
        edit.setClosed(!edit.isClosed());
        break;
      case ID_TWO_SIDED:
        twoSided = !twoSided;
        break;
      case ID_SAVE:
        save();
        mc.displayGuiScreen(null);
        return;
      case ID_CANCEL:
        mc.displayGuiScreen(null);
        return;
      default:
        return;
    }
    labels();
  }

  private void save() {
    edit.setCustomName(customName.getText());
    edit.setCustomGround(parseHex(customGround.getText(), edit.getCustomGround()));
    edit.setCustomLegend(parseHex(customLegend.getText(), edit.getCustomLegend()));
    edit.setStopName(stopName.getText());
    edit.setDirection(direction.getText());
    edit.setRoutes(routes.getText());
    if (!wall) {
      edit.setTwoSided(twoSided);
    }
    CsmTransit.NETWORK.sendToServer(new BusStopInfoPacket(pos, edit));
  }

  private static int parseHex(String text, int fallback) {
    String s = text.trim();
    if (s.startsWith("#")) {
      s = s.substring(1);
    }
    try {
      return s.isEmpty() ? fallback : Integer.parseInt(s, 16) & 0xFFFFFF;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB) {
      int focused = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
      }
      int count = custom() ? fields.length : 3;
      int next = (focused + 1) % count;
      for (int i = 0; i < fields.length; i++) {
        fields[i].setFocused(i == next);
      }
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      save();
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : fields) {
      if ((custom() || !isCustomField(field)) && field.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  private boolean isCustomField(GuiTextField field) {
    return field == customName || field == customGround || field == customLegend;
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField field : fields) {
      if (custom() || !isCustomField(field)) {
        field.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField field : fields) {
      field.updateCursorCounter();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int x = left();
    int y = top();
    drawCenteredString(fontRenderer, title, width / 2, y - 16, 0xFFFFFF);
    if (custom()) {
      drawString(fontRenderer, I18n.format("csm.transit.arrival_board.custom_name"), x + 124,
          y + 22, CAPTION);
      drawString(fontRenderer, I18n.format("csm.transit.arrival_board.custom_colours"), x + 208,
          y + 22, CAPTION);
      customName.drawTextBox();
      customGround.drawTextBox();
      customLegend.drawTextBox();
    }
    y += 46;
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.stop_name"), x, y - 11,
        CAPTION);
    stopName.drawTextBox();
    y += 34;
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.direction"), x, y - 11,
        CAPTION);
    direction.drawTextBox();
    y += 34;
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.routes"), x, y - 11,
        CAPTION);
    routes.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
