package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.venues.CsmVenues;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The centre-hung scoreboard's editor: the teams' names, scores, shots and colours, the period,
 * the clock and a period's length, what the screens show, and the clock's start/stop and reset.
 * Done (or Enter) sends the fields; Start/Stop and Reset act at once. A number field that does
 * not read leaves its value as it was.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiScoreboard extends GuiScreen {

  private static final int ID_DONE = 0;
  private static final int ID_TOGGLE = 1;
  private static final int ID_RESET = 2;
  private static final int ID_MODE = 3;
  private static final int ID_HOME_COLOUR = 4;
  private static final int ID_AWAY_COLOUR = 5;
  private static final int ID_HOME_GOAL = 6;
  private static final int ID_AWAY_GOAL = 7;
  private static final int ID_HOME_SHOT = 8;
  private static final int ID_AWAY_SHOT = 9;

  private static final int FIELD_H = 18;

  private final TileEntityScoreboard board;

  private GuiTextField home;
  private GuiTextField away;
  private GuiTextField homeScore;
  private GuiTextField awayScore;
  private GuiTextField homeShots;
  private GuiTextField awayShots;
  private GuiTextField period;
  private GuiTextField clock;
  private GuiTextField length;
  private GuiTextField[] fields;
  private String clockShown;

  private int mode;
  private int homeColour;
  private int awayColour;
  private GuiButton toggle;
  private GuiButton modeButton;
  private GuiButton homeColourButton;
  private GuiButton awayColourButton;

  /**
   * An editor for a board.
   *
   * @param board its tile entity (the master's)
   */
  public GuiScoreboard(TileEntityScoreboard board) {
    this.board = board;
    this.mode = board.getMode();
    this.homeColour = board.getHomeColour();
    this.awayColour = board.getAwayColour();
  }

  private int top() {
    return height / 2 - 96;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int cx = width / 2;
    int top = top();
    int left = cx - 170;
    int right = cx + 50;
    home = field(0, left, top + 24, 120, TileEntityScoreboard.MAX_NAME, board.getHome());
    away = field(1, right, top + 24, 120, TileEntityScoreboard.MAX_NAME, board.getAway());
    homeScore = field(2, left, top + 58, 34, 3, Integer.toString(board.getHomeScore()));
    awayScore = field(3, right, top + 58, 34, 3, Integer.toString(board.getAwayScore()));
    homeShots = field(4, left + 64, top + 58, 34, 3, Integer.toString(board.getHomeShots()));
    awayShots = field(5, right + 64, top + 58, 34, 3, Integer.toString(board.getAwayShots()));
    period = field(6, cx - 35, top + 24, 70, TileEntityScoreboard.MAX_PERIOD, board.getPeriod());
    long now = board.getWorld() == null ? 0 : board.getWorld().getTotalWorldTime();
    clockShown = TileEntityScoreboard.clockText(board.remaining(now, 0));
    clock = field(7, cx - 35, top + 58, 70, 8, clockShown);
    length = field(8, cx - 35, top + 92, 70, 8,
        TileEntityScoreboard.clockText(board.getLength()));
    fields = new GuiTextField[]{home, away, homeScore, awayScore, homeShots, awayShots, period,
        clock, length};
    home.setFocused(true);

    buttonList.add(new GuiButton(ID_HOME_GOAL, left + 36, top + 57, 22, 20, "+1"));
    buttonList.add(new GuiButton(ID_AWAY_GOAL, right + 36, top + 57, 22, 20, "+1"));
    buttonList.add(new GuiButton(ID_HOME_SHOT, left + 100, top + 57, 22, 20, "+1"));
    buttonList.add(new GuiButton(ID_AWAY_SHOT, right + 100, top + 57, 22, 20, "+1"));
    homeColourButton = new GuiButton(ID_HOME_COLOUR, left, top + 90, 120, 20, "");
    awayColourButton = new GuiButton(ID_AWAY_COLOUR, right, top + 90, 120, 20, "");
    modeButton = new GuiButton(ID_MODE, cx - 170, top + 120, 340, 20, "");
    toggle = new GuiButton(ID_TOGGLE, cx - 170, top + 146, 110, 20, "");
    buttonList.add(homeColourButton);
    buttonList.add(awayColourButton);
    buttonList.add(modeButton);
    buttonList.add(toggle);
    buttonList.add(new GuiButton(ID_RESET, cx - 55, top + 146, 110, 20,
        I18n.format("gui.csm.scoreboard.reset")));
    buttonList.add(new GuiButton(ID_DONE, cx + 60, top + 146, 110, 20, I18n.format("gui.done")));
    labels();
  }

  private GuiTextField field(int id, int x, int y, int w, int max, String text) {
    GuiTextField f = new GuiTextField(id, fontRenderer, x, y, w, FIELD_H);
    f.setMaxStringLength(max);
    f.setText(text);
    return f;
  }

  private void labels() {
    homeColourButton.displayString = I18n.format("gui.csm.scoreboard.colour",
        I18n.format(VenueColours.key(homeColour)));
    awayColourButton.displayString = I18n.format("gui.csm.scoreboard.colour",
        I18n.format(VenueColours.key(awayColour)));
    modeButton.displayString = I18n.format("gui.csm.scoreboard.mode",
        I18n.format("gui.csm.scoreboard.mode." + mode));
    toggle.displayString = I18n.format(board.isClockStarted() ? "gui.csm.scoreboard.stop"
        : "gui.csm.scoreboard.start");
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case ID_DONE:
        sendApply();
        mc.displayGuiScreen(null);
        return;
      case ID_TOGGLE:
        sendApply();
        CsmVenues.NETWORK.sendToServer(ScoreboardPacket.action(board.getPos(),
            ScoreboardPacket.TOGGLE));
        mc.displayGuiScreen(null);
        return;
      case ID_RESET:
        CsmVenues.NETWORK.sendToServer(ScoreboardPacket.action(board.getPos(),
            ScoreboardPacket.RESET));
        clock.setText(length.getText());
        clockShown = clock.getText();
        break;
      case ID_MODE:
        mode = (mode + 1) % 3;
        break;
      case ID_HOME_COLOUR:
        homeColour = (homeColour + (isShiftKeyDown() ? VenueColours.COUNT - 1 : 1))
            % VenueColours.COUNT;
        break;
      case ID_AWAY_COLOUR:
        awayColour = (awayColour + (isShiftKeyDown() ? VenueColours.COUNT - 1 : 1))
            % VenueColours.COUNT;
        break;
      case ID_HOME_GOAL:
        bump(homeScore);
        break;
      case ID_AWAY_GOAL:
        bump(awayScore);
        break;
      case ID_HOME_SHOT:
        bump(homeShots);
        break;
      case ID_AWAY_SHOT:
        bump(awayShots);
        break;
      default:
        break;
    }
    labels();
  }

  private static void bump(GuiTextField f) {
    int v = number(f, 0);
    f.setText(Integer.toString(Math.min(TileEntityScoreboard.MAX_COUNT, v + 1)));
  }

  private static int number(GuiTextField f, int fallback) {
    try {
      return Math.max(0, Math.min(TileEntityScoreboard.MAX_COUNT,
          Integer.parseInt(f.getText().trim())));
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private void sendApply() {
    // the clock is sent only if it was edited, so a running clock is not set back to the time
    // the screen was opened at
    int clk = clock.getText().trim().equals(clockShown) ? -1
        : TileEntityScoreboard.parseClock(clock.getText());
    int len = TileEntityScoreboard.parseClock(length.getText());
    CsmVenues.NETWORK.sendToServer(ScoreboardPacket.apply(board.getPos(), home.getText(),
        away.getText(), number(homeScore, board.getHomeScore()),
        number(awayScore, board.getAwayScore()), number(homeShots, board.getHomeShots()),
        number(awayShots, board.getAwayShots()), period.getText(), clk, len, mode, homeColour,
        awayColour));
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
      int next = (focused + (isShiftKeyDown() ? fields.length - 1 : 1)) % fields.length;
      for (int i = 0; i < fields.length; i++) {
        fields[i].setFocused(i == next);
      }
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      sendApply();
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField f : fields) {
      if (f.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField f : fields) {
      f.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField f : fields) {
      f.updateCursorCounter();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int cx = width / 2;
    int top = top();
    int left = cx - 170;
    int right = cx + 50;
    int gold = 0xFFD070;
    drawCenteredString(fontRenderer, I18n.format("gui.csm.scoreboard.title"), cx, top - 4,
        0xFFFFFF);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.home"), left, top + 13, gold);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.away"), right, top + 13, gold);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.scoreboard.period"), cx, top + 13,
        gold);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.score"), left, top + 47, gold);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.shots"), left + 64, top + 47, gold);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.score"), right, top + 47, gold);
    drawString(fontRenderer, I18n.format("gui.csm.scoreboard.shots"), right + 64, top + 47,
        gold);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.scoreboard.clock"), cx, top + 47,
        gold);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.scoreboard.length"), cx, top + 81,
        gold);
    for (GuiTextField f : fields) {
      f.drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.scoreboard.hint"), cx, top + 174,
        0xA0A0A0);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
