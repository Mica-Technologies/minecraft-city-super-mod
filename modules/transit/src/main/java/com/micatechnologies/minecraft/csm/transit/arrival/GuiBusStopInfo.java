package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.transit.CsmTransit;
import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
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
 * the stop's name and direction line, the board's own routes, a destination for each route, and
 * the closed and two-sided switches. Save sends it all in one packet ({@link BusStopInfoPacket});
 * Cancel or Escape leaves the board as it was.
 *
 * <p><b>Destinations.</b> One field a route, labelled with the route as the routes field reads at
 * that moment ({@link TransitRoute#parseLine}), so typing "B 30" shows a B field and a 30 field.
 * With no routes typed (the board lists the nearest stop's), the fields are numbered #1 to #6, the
 * stop's routes in their order. A destination belongs to its place in the list, not to the route
 * id: reorder the routes and move the destinations with them.</p>
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class GuiBusStopInfo extends GuiScreen {

  private static final int W = 300;
  private static final int ID_AGENCY = 0;
  private static final int ID_CLOSED = 2;
  private static final int ID_TWO_SIDED = 3;
  private static final int ID_SAVE = 10;
  private static final int ID_CANCEL = 11;
  private static final int CAPTION = 0xFFD070;
  /** A destination field's label column, and its field's width. */
  private static final int DEST_LABEL = 22;
  private static final int DEST_W = W / 3 - DEST_LABEL - 4;

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
  private final GuiTextField[] destinations = new GuiTextField[TransitRoute.MAX_PER_LINE];
  private GuiButton agencyButton;
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
    return Math.max(14, height / 2 - 108);
  }

  // rows, from the top
  private int rowNames() {
    return top() + 44;
  }

  private int rowRoutes() {
    return rowNames() + 32;
  }

  private int rowDestinations() {
    return rowRoutes() + 34;
  }

  private int rowToggles() {
    return rowDestinations() + 48;
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
    int half = (W - 4) / 2;
    stopName = field(x, rowNames(), half, BusStopInfo.MAX_STOP_NAME, edit.getStopName());
    direction = field(x + half + 4, rowNames(), half, BusStopInfo.MAX_DIRECTION,
        edit.getDirection());
    routes = field(x, rowRoutes(), W, BusStopInfo.MAX_ROUTES, edit.getRoutes());
    for (int i = 0; i < destinations.length; i++) {
      destinations[i] = field(destX(i), destY(i), DEST_W, BusStopInfo.MAX_DESTINATION,
          edit.getDestination(i));
    }
    int bw = (W - 4) / 2;
    closedButton = new GuiButton(ID_CLOSED, x, rowToggles(), bw, 20, "");
    twoSidedButton = new GuiButton(ID_TWO_SIDED, x + bw + 4, rowToggles(), bw, 20, "");
    twoSidedButton.enabled = !wall;
    buttonList.add(closedButton);
    buttonList.add(twoSidedButton);
    int y2 = rowToggles() + 24;
    buttonList.add(new GuiButton(ID_SAVE, width / 2 - 104, y2, 100, 20,
        I18n.format("csm.transit.arrival_board.save")));
    buttonList.add(new GuiButton(ID_CANCEL, width / 2 + 4, y2, 100, 20,
        I18n.format("gui.cancel")));
    stopName.setFocused(true);
    labels();
  }

  private int destX(int i) {
    return left() + (i % 3) * (W / 3) + DEST_LABEL;
  }

  private int destY(int i) {
    return rowDestinations() + (i / 3) * 22;
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

  /** The routes the routes field reads now; empty means the nearest stop's. */
  private int[] typedRoutes() {
    return TransitRoute.parseLine(routes.getText());
  }

  /** How many destination fields show: one a typed route, or all six numbered. */
  private int destinationCount() {
    int n = typedRoutes().length;
    return n > 0 ? n : destinations.length;
  }

  /** The fields that take input now, in Tab order. */
  private GuiTextField[] activeFields() {
    int dests = destinationCount();
    GuiTextField[] out = new GuiTextField[3 + dests + (custom() ? 3 : 0)];
    int n = 0;
    out[n++] = stopName;
    out[n++] = direction;
    out[n++] = routes;
    for (int i = 0; i < dests; i++) {
      out[n++] = destinations[i];
    }
    if (custom()) {
      out[n++] = customName;
      out[n++] = customGround;
      out[n] = customLegend;
    }
    return out;
  }

  private void labels() {
    BoardAgency a = edit.getAgency();
    String name = a == BoardAgency.CUSTOM ? I18n.format("csm.transit.arrival_board.custom")
        : a.title(edit);
    agencyButton.displayString = I18n.format("csm.transit.arrival_board.agency", name);
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
    // a destination past the routes typed has no route to go with: cleared
    int dests = destinationCount();
    for (int i = 0; i < destinations.length; i++) {
      edit.setDestination(i, i < dests ? destinations[i].getText() : "");
    }
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
    GuiTextField[] fields = activeFields();
    if (keyCode == Keyboard.KEY_TAB) {
      int focused = -1;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
      }
      int next = (focused + (isShiftKeyDown() ? fields.length - 1 : 1)) % fields.length;
      for (GuiTextField f : allFields()) {
        f.setFocused(f == fields[next]);
      }
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      save();
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : fields) {
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  private GuiTextField[] allFields() {
    GuiTextField[] out = new GuiTextField[6 + destinations.length];
    out[0] = stopName;
    out[1] = direction;
    out[2] = routes;
    out[3] = customName;
    out[4] = customGround;
    out[5] = customLegend;
    System.arraycopy(destinations, 0, out, 6, destinations.length);
    return out;
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    GuiTextField[] active = activeFields();
    for (GuiTextField field : allFields()) {
      boolean on = false;
      for (GuiTextField a : active) {
        on |= a == field;
      }
      if (on) {
        field.mouseClicked(mouseX, mouseY, mouseButton);
      } else {
        field.setFocused(false);
      }
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField field : allFields()) {
      field.updateCursorCounter();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int x = left();
    int y = top();
    drawCenteredString(fontRenderer, title, width / 2, y - 12, 0xFFFFFF);
    if (custom()) {
      drawString(fontRenderer, I18n.format("csm.transit.arrival_board.custom_name"), x + 124,
          y + 22, CAPTION);
      drawString(fontRenderer, I18n.format("csm.transit.arrival_board.custom_colours"), x + 208,
          y + 22, CAPTION);
      customName.drawTextBox();
      customGround.drawTextBox();
      customLegend.drawTextBox();
    }
    int half = (W - 4) / 2;
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.stop_name"), x,
        rowNames() - 10, CAPTION);
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.direction"),
        x + half + 4, rowNames() - 10, CAPTION);
    stopName.drawTextBox();
    direction.drawTextBox();
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.routes"), x,
        rowRoutes() - 10, CAPTION);
    routes.drawTextBox();
    drawString(fontRenderer, I18n.format("csm.transit.arrival_board.destinations"), x,
        rowDestinations() - 10, CAPTION);
    int[] typed = typedRoutes();
    int dests = destinationCount();
    for (int i = 0; i < dests; i++) {
      String label = typed.length > 0 ? TransitRoute.label(typed[i]) : "#" + (i + 1);
      drawString(fontRenderer, label, destX(i) - DEST_LABEL + 2, destY(i) + 6, 0xFFFFFF);
      destinations[i].drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
