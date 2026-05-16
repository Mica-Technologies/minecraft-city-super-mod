package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import com.micatechnologies.minecraft.csm.hvac.HvacTemperatureManager;
import java.io.IOException;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Gag desktop GUI for the iMac, iMac Pro, and MacBook Pro. CSM-OS hosts three "apps":
 *
 * <ul>
 *   <li><b>Notepad</b> — persistent free-form text stored on {@link TileEntityComputer},
 *   plus a Fortune button that cycles a small palette of jokey one-liners.</li>
 *   <li><b>Calculator</b> — basic 4-function calculator (digits 0–9, +, −, ×, ÷, =, C).
 *   Mouse-only; calculator state is per-GUI-instance and resets on close.</li>
 *   <li><b>Weather</b> — read-only panel showing the player's biome, biome temperature,
 *   the conditioned indoor temperature from {@link HvacTemperatureManager}, the sky state
 *   (clear / rain / thunder), and the in-game day count.</li>
 * </ul>
 *
 * <p>Tabs along the top of the window switch apps. The bottom button row changes per app:
 * Notepad shows Save / Cancel / Fortune / Shutdown; Calculator and Weather show only
 * Cancel / Shutdown.</p>
 *
 * <p>The top menu bar's clock matches the HVAC thermostat's formatting (12-hour, +6 hour
 * Minecraft offset so 0 ticks reads as 6:00 AM).</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ComputerGui extends GuiScreen {

  // === Layout constants ===
  private static final int WINDOW_W_TARGET = 320;
  private static final int WINDOW_H_TARGET = 200;
  private static final int TITLE_BAR_H = 14;
  private static final int TAB_STRIP_H = 14;
  private static final int BUTTON_ROW_H = 22;
  private static final int CONTENT_PADDING = 8;

  // === Color palette ===
  private static final int COLOR_DESKTOP_BG = 0xFF1B2233;
  private static final int COLOR_MENU_BAR = 0xFFE8ECF2;
  private static final int COLOR_MENU_BAR_TEXT = 0xFF1A1A1A;
  private static final int COLOR_WINDOW_FRAME = 0xFFD8DCE6;
  private static final int COLOR_WINDOW_BG = 0xFFF5F6F8;
  private static final int COLOR_WINDOW_TITLE = 0xFF263043;
  private static final int COLOR_WINDOW_TITLE_TEXT = 0xFFFFFFFF;
  private static final int COLOR_TAB_INACTIVE = 0xFF1F2A3D;
  private static final int COLOR_TAB_INACTIVE_TEXT = 0xFFB8C0D0;
  private static final int COLOR_TAB_ACTIVE = COLOR_WINDOW_BG;
  private static final int COLOR_TAB_ACTIVE_TEXT = COLOR_WINDOW_TITLE;
  private static final int COLOR_INFO_TEXT = 0xFF2A2F3A;
  private static final int COLOR_INFO_HIGHLIGHT = 0xFF445888;
  private static final int COLOR_CALC_DISPLAY_BG = 0xFF101418;
  private static final int COLOR_CALC_DISPLAY_TEXT = 0xFFB8FFB8;

  // === Button IDs ===
  private static final int BUTTON_ID_NOTEPAD_SAVE = 0;
  private static final int BUTTON_ID_CANCEL = 1;
  private static final int BUTTON_ID_NOTEPAD_FORTUNE = 2;
  private static final int BUTTON_ID_SHUTDOWN = 3;
  private static final int BUTTON_ID_CALC_BASE = 100; // 100..115 for the 4×4 grid

  private static final String[] FORTUNES = new String[] {
      "Today is a good day to refactor.",
      "You will find a creeper in your basement.",
      "An emerald block is in your future.",
      "All your TPS are belong to us.",
      "Have you tried turning it off and on again?",
      "Beware of falling anvils on Tuesdays.",
      "Your build will be reviewed by a wandering trader.",
      "There is no spoon. There is, however, a sponge.",
      "Patch notes will be longer than expected.",
      "It is pitch dark. You are likely to be eaten by an enderman.",
  };

  private enum App {
    NOTEPAD("Notepad"),
    CALCULATOR("Calculator"),
    WEATHER("Weather");

    final String label;

    App(String label) {
      this.label = label;
    }
  }

  /**
   * Calculator labels arranged the way a player expects on a 4×4 button grid. Operators
   * occupy the right-hand column; the bottom row mirrors a real calculator with a wide
   * "0" replaced by a single cell to keep the grid uniform.
   */
  private static final String[][] CALC_LABELS = {
      {"7", "8", "9", "/"},
      {"4", "5", "6", "*"},
      {"1", "2", "3", "-"},
      {"0", "C", "=", "+"}
  };

  // === Persistent identity ===
  private final BlockPos tilePos;
  private final String computerName;
  private final String initialNotepadText;

  // === Tab state ===
  private App currentApp = App.NOTEPAD;

  // === Notepad state ===
  private GuiMultiLineTextField notepad;
  /**
   * Last known notepad contents. Tracked separately from the widget so that switching
   * away from the Notepad tab and back doesn't drop typed-but-unsaved text when the
   * widget is rebuilt. Seeded from {@link #initialNotepadText} and refreshed every
   * time we send the text to the server.
   */
  private String latestNotepadText;
  private String fortuneText = "Welcome to CSM-OS. Click 'Fortune' for a tip.";
  private final Random fortuneRandom = new Random();
  private int lastFortuneIndex = -1;

  // === Calculator state ===
  /** What's currently shown in the display, as a String (so we can show "0", "12.5", etc). */
  private String calcDisplay = "0";
  /** The accumulated value waiting for the pending operator to be applied. */
  private double calcAccumulator = 0.0;
  /** Pending operator: '+', '-', '*', '/', or 0 for none. */
  private char calcPendingOp = 0;
  /** When true, the next digit press replaces the display rather than appending to it. */
  private boolean calcAwaitingNewInput = true;
  /** Set when the last operation produced an error (divide by zero); cleared by C. */
  private boolean calcError = false;

  // === Cached layout (recomputed in initGui) ===
  private int windowX, windowY, windowW, windowH;
  private int contentY, contentH;
  private int[] tabXs = new int[App.values().length];
  private int tabWidth;

  public ComputerGui(TileEntityComputer tile) {
    this.tilePos = tile.getPos();
    this.initialNotepadText = tile.getNotepadText() == null ? "" : tile.getNotepadText();
    this.latestNotepadText = this.initialNotepadText;
    this.computerName = computerNameFromBlock(tile);
  }

  private static String computerNameFromBlock(TileEntityComputer tile) {
    if (tile.getWorld() == null) {
      return "CSM-OS";
    }
    String path = "";
    if (tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName() != null) {
      path = tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName().getPath();
    }
    switch (path) {
      case "imac":
        return "iMac — CSM-OS";
      case "imacpro":
        return "iMac Pro — CSM-OS";
      case "mbp":
        return "MacBook Pro — CSM-OS";
      default:
        return "CSM-OS";
    }
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    recomputeLayout();
    rebuildForApp();
  }

  private void recomputeLayout() {
    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();
    windowW = Math.min(WINDOW_W_TARGET, screenW - 40);
    windowH = Math.min(WINDOW_H_TARGET, screenH - 80);
    windowX = (screenW - windowW) / 2;
    windowY = (screenH - windowH) / 2;
    contentY = windowY + TITLE_BAR_H + TAB_STRIP_H;
    contentH = windowH - TITLE_BAR_H - TAB_STRIP_H - BUTTON_ROW_H;

    tabWidth = Math.min(72, windowW / App.values().length);
    int totalTabW = tabWidth * App.values().length;
    int tabStartX = windowX + (windowW - totalTabW) / 2;
    for (int i = 0; i < App.values().length; i++) {
      tabXs[i] = tabStartX + i * tabWidth;
    }
  }

  /** Tears down and rebuilds the button list whenever the active app changes. */
  private void rebuildForApp() {
    this.buttonList.clear();
    int btnY = windowY + windowH - 22;
    int btnW = 62;
    int btnGap = 4;

    if (currentApp == App.NOTEPAD) {
      initNotepadApp();
      // Save | Cancel | Fortune | Shutdown
      int totalW = btnW * 4 + btnGap * 3;
      int startX = windowX + (windowW - totalW) / 2;
      this.buttonList.add(new GuiButton(BUTTON_ID_NOTEPAD_SAVE,
          startX, btnY, btnW, 20, "Save"));
      this.buttonList.add(new GuiButton(BUTTON_ID_CANCEL,
          startX + (btnW + btnGap), btnY, btnW, 20, "Cancel"));
      this.buttonList.add(new GuiButton(BUTTON_ID_NOTEPAD_FORTUNE,
          startX + (btnW + btnGap) * 2, btnY, btnW, 20, "Fortune"));
      this.buttonList.add(new GuiButton(BUTTON_ID_SHUTDOWN,
          startX + (btnW + btnGap) * 3, btnY, btnW, 20, "Shutdown"));
    } else {
      // Calculator + Weather: just Cancel and Shutdown
      int totalW = btnW * 2 + btnGap;
      int startX = windowX + (windowW - totalW) / 2;
      this.buttonList.add(new GuiButton(BUTTON_ID_CANCEL,
          startX, btnY, btnW, 20, "Cancel"));
      this.buttonList.add(new GuiButton(BUTTON_ID_SHUTDOWN,
          startX + btnW + btnGap, btnY, btnW, 20, "Shutdown"));

      if (currentApp == App.CALCULATOR) {
        initCalculatorApp();
      }
    }
  }

  private void initNotepadApp() {
    int notepadX = windowX + CONTENT_PADDING;
    int notepadY = contentY + 6;
    int notepadW = windowW - CONTENT_PADDING * 2;
    // Reserve space for the fortune label below the text field, plus a gap.
    int reservedForFortune = 18;
    int notepadH = contentH - 6 - reservedForFortune;
    notepad = new GuiMultiLineTextField(0, fontRenderer, notepadX, notepadY, notepadW, notepadH);
    notepad.setText(latestNotepadText);
    notepad.setFocused(true);
  }

  private void initCalculatorApp() {
    int padding = 10;
    int displayY = contentY + 6;
    int displayH = 18;
    // GuiButton's textured-modal-rect renderer assumes a 20-pixel-tall widget; making
    // the buttons taller stretches the texture and exposes the unused/hovered band along
    // the bottom edge of every cell. Lock height at 20 and lay out with explicit gaps.
    int btnH = 20;
    int rowGap = 4;
    int gridY = displayY + displayH + 6;
    int btnW = (windowW - padding * 2) / 4;
    int gridX = windowX + padding;

    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        int id = BUTTON_ID_CALC_BASE + row * 4 + col;
        this.buttonList.add(new GuiButton(id,
            gridX + col * btnW,
            gridY + row * (btnH + rowGap),
            btnW - 2, btnH, CALC_LABELS[row][col]));
      }
    }
  }

  // === Drawing ===

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();
    drawRect(0, 0, screenW, screenH, COLOR_DESKTOP_BG);

    drawMenuBar(screenW);
    drawWindowChrome();
    drawTabStrip();
    drawAppContent();

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private void drawMenuBar(int screenW) {
    drawRect(0, 0, screenW, TITLE_BAR_H, COLOR_MENU_BAR);
    fontRenderer.drawString("❖ " + computerName, 6, 3, COLOR_MENU_BAR_TEXT);
    String right = formatRightMenu();
    int rightWidth = fontRenderer.getStringWidth(right);
    fontRenderer.drawString(right, screenW - rightWidth - 6, 3, COLOR_MENU_BAR_TEXT);
  }

  private void drawWindowChrome() {
    drawRect(windowX - 1, windowY - 1, windowX + windowW + 1, windowY + windowH + 1,
        COLOR_WINDOW_FRAME);
    drawRect(windowX, windowY, windowX + windowW, windowY + TITLE_BAR_H,
        COLOR_WINDOW_TITLE);
    drawRect(windowX, windowY + TITLE_BAR_H, windowX + windowW, windowY + windowH,
        COLOR_WINDOW_BG);
    fontRenderer.drawString(currentApp.label, windowX + 6, windowY + 3,
        COLOR_WINDOW_TITLE_TEXT);
  }

  private void drawTabStrip() {
    int tabY = windowY + TITLE_BAR_H;
    App[] apps = App.values();
    for (int i = 0; i < apps.length; i++) {
      boolean active = apps[i] == currentApp;
      int x0 = tabXs[i];
      int x1 = x0 + tabWidth - 1;
      drawRect(x0, tabY, x1, tabY + TAB_STRIP_H,
          active ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
      String label = apps[i].label;
      int tx = x0 + (tabWidth - fontRenderer.getStringWidth(label)) / 2;
      fontRenderer.drawString(label, tx, tabY + 3,
          active ? COLOR_TAB_ACTIVE_TEXT : COLOR_TAB_INACTIVE_TEXT);
    }
  }

  private void drawAppContent() {
    switch (currentApp) {
      case NOTEPAD:
        drawNotepadApp();
        break;
      case CALCULATOR:
        drawCalculatorApp();
        break;
      case WEATHER:
        drawWeatherApp();
        break;
    }
  }

  private void drawNotepadApp() {
    if (notepad != null) {
      notepad.drawTextBox();
    }
    // Fortune label sits below the notepad with margin built into initNotepadApp's
    // reservedForFortune value. Keep the y-coordinate in sync with that reservation.
    int labelY = contentY + contentH - 14;
    String fortuneShort = trimToWidth(fortuneText, windowW - CONTENT_PADDING * 2);
    fontRenderer.drawString(fortuneShort, windowX + CONTENT_PADDING, labelY, COLOR_INFO_TEXT);
  }

  private void drawCalculatorApp() {
    int padding = 10;
    int displayY = contentY + 6;
    int displayH = 18;
    int displayX0 = windowX + padding;
    int displayX1 = windowX + windowW - padding;
    drawRect(displayX0, displayY, displayX1, displayY + displayH, COLOR_CALC_DISPLAY_BG);
    String shown = calcError ? "ERR" : calcDisplay;
    String trimmed = trimToWidth(shown, displayX1 - displayX0 - 8);
    int textW = fontRenderer.getStringWidth(trimmed);
    fontRenderer.drawString(trimmed, displayX1 - 4 - textW, displayY + 5,
        COLOR_CALC_DISPLAY_TEXT);
  }

  private void drawWeatherApp() {
    int x = windowX + CONTENT_PADDING + 4;
    int y = contentY + 8;
    int lineH = 12;
    World world = this.mc.world;
    EntityPlayer player = this.mc.player;
    if (world == null || player == null) {
      fontRenderer.drawString("No world data available.", x, y, COLOR_INFO_TEXT);
      return;
    }

    BlockPos playerPos = player.getPosition();
    Biome biome = world.getBiome(playerPos);
    String biomeName = biome.getBiomeName();
    float biomeTempUnits = biome.getTemperature(playerPos);
    float ambientF = HvacTemperatureManager.getTemperatureAt(world, playerPos);
    float biomeRainfall = biome.getRainfall();

    String sky;
    if (world.isThundering()) {
      sky = "Thunderstorm";
    } else if (world.isRaining()) {
      // Some biomes can't have rain (deserts) — clarify.
      sky = biome.canRain() ? "Raining" : "Overcast";
    } else {
      sky = "Clear";
    }

    long worldTime = world.getWorldTime() % 24000L;
    long totalDays = world.getWorldTime() / 24000L + 1L;
    boolean daytime = worldTime < 12000L;

    // Use fontRenderer.drawString (no shadow) instead of Gui#drawString (with shadow);
    // shadowed text reads as muddy on the light window background.
    fontRenderer.drawString("☀ Biome:  " + biomeName,
        x, y, COLOR_INFO_HIGHLIGHT);
    y += lineH;
    fontRenderer.drawString("Climate:  " + describeClimate(biomeTempUnits, biomeRainfall),
        x, y, COLOR_INFO_TEXT);
    y += lineH;
    fontRenderer.drawString(String.format("Temperature:  %.1f°F", ambientF),
        x, y, COLOR_INFO_TEXT);
    y += lineH;
    fontRenderer.drawString("Sky:  " + sky,
        x, y, COLOR_INFO_TEXT);
    y += lineH;
    fontRenderer.drawString("Time:  Day " + totalDays + " — " + (daytime ? "Daytime" : "Night"),
        x, y, COLOR_INFO_TEXT);
    y += lineH + 4;
    fontRenderer.drawString("(Indoor temperature reflects nearby HVAC units.)",
        x, y, 0xFF7B82A0);
  }

  /** Friendly climate label derived from biome temperature and rainfall (no API needed). */
  private static String describeClimate(float tempUnits, float rainfall) {
    String temp;
    if (tempUnits < 0.15f) temp = "Frigid";
    else if (tempUnits < 0.5f) temp = "Cool";
    else if (tempUnits < 1.0f) temp = "Temperate";
    else if (tempUnits < 1.5f) temp = "Warm";
    else temp = "Hot";

    String moisture;
    if (rainfall < 0.1f) moisture = "arid";
    else if (rainfall < 0.4f) moisture = "dry";
    else if (rainfall < 0.7f) moisture = "moderate";
    else moisture = "humid";
    return temp + ", " + moisture;
  }

  private String trimToWidth(String s, int maxPx) {
    if (s == null) return "";
    if (fontRenderer.getStringWidth(s) <= maxPx) return s;
    String suffix = "...";
    int suffixW = fontRenderer.getStringWidth(suffix);
    while (s.length() > 0 && fontRenderer.getStringWidth(s) + suffixW > maxPx) {
      s = s.substring(0, s.length() - 1);
    }
    return s + suffix;
  }

  /** Builds the right-aligned menu label: "Day N  •  H:MM AM/PM". */
  private String formatRightMenu() {
    World world = this.mc.world;
    if (world == null) {
      return "";
    }
    long worldTime = world.getWorldTime() % 24000L;
    long totalDays = world.getWorldTime() / 24000L + 1L;
    int hours = (int) ((worldTime / 1000L + 6L) % 24L);
    int minutes = (int) ((worldTime % 1000L) * 60L / 1000L);
    boolean pm = hours >= 12;
    int displayHour = hours % 12;
    if (displayHour == 0) {
      displayHour = 12;
    }
    return String.format("Day %d  •  %d:%02d %s", totalDays, displayHour, minutes,
        pm ? "PM" : "AM");
  }

  // === Input handling ===

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    super.keyTyped(typedChar, keyCode);
    if (currentApp == App.NOTEPAD && notepad != null) {
      notepad.setFocused(true);
      notepad.textboxKeyTyped(typedChar, keyCode);
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    // Tab clicks first: if the click landed in the tab strip, switch apps and skip the
    // rest of the dispatch. (Otherwise a click on a tab also reaches GuiButtons under it
    // when initGui rebuilds.)
    if (mouseButton == 0 && handleTabStripClick(mouseX, mouseY)) {
      return;
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (currentApp == App.NOTEPAD && notepad != null) {
      notepad.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  /** Returns true if the click hit a tab and the app was switched. */
  private boolean handleTabStripClick(int mouseX, int mouseY) {
    int tabY = windowY + TITLE_BAR_H;
    if (mouseY < tabY || mouseY >= tabY + TAB_STRIP_H) {
      return false;
    }
    App[] apps = App.values();
    for (int i = 0; i < apps.length; i++) {
      int x0 = tabXs[i];
      int x1 = x0 + tabWidth - 1;
      if (mouseX >= x0 && mouseX < x1) {
        if (apps[i] != currentApp) {
          // Save notepad text before leaving the tab so unsaved input persists across
          // app switches (and across the GUI close that often follows).
          if (currentApp == App.NOTEPAD && notepad != null) {
            sendNotepad(false);
          }
          currentApp = apps[i];
          rebuildForApp();
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    if (currentApp == App.NOTEPAD && notepad != null) {
      int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
      int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
      int scrollDelta = Mouse.getEventDWheel();
      notepad.handleMouseInput(mouseX, mouseY, scrollDelta);
    }
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    int id = button.id;
    if (id == BUTTON_ID_CANCEL) {
      this.mc.displayGuiScreen(null);
      return;
    }
    if (id == BUTTON_ID_SHUTDOWN) {
      // Save any pending notepad changes, then ask the server to flip POWERED off.
      if (currentApp == App.NOTEPAD) {
        sendNotepad(true);
      } else {
        // No live widget to read from, but the cache holds whatever the user typed
        // before they switched tabs — send that so we don't clobber it server-side.
        CsmNetwork.sendToServer(new ComputerNotepadPacket(tilePos, latestNotepadText, true));
      }
      this.mc.displayGuiScreen(null);
      return;
    }
    if (id == BUTTON_ID_NOTEPAD_SAVE) {
      sendNotepad(false);
      this.mc.displayGuiScreen(null);
      return;
    }
    if (id == BUTTON_ID_NOTEPAD_FORTUNE) {
      cycleFortune();
      return;
    }
    if (id >= BUTTON_ID_CALC_BASE && id < BUTTON_ID_CALC_BASE + 16) {
      int idx = id - BUTTON_ID_CALC_BASE;
      int row = idx / 4;
      int col = idx % 4;
      handleCalcButton(CALC_LABELS[row][col]);
    }
  }

  private void cycleFortune() {
    int idx;
    do {
      idx = fortuneRandom.nextInt(FORTUNES.length);
    } while (FORTUNES.length > 1 && idx == lastFortuneIndex);
    lastFortuneIndex = idx;
    fortuneText = FORTUNES[idx];
  }

  private void sendNotepad(boolean shutdown) {
    String text = notepad == null ? latestNotepadText : notepad.getText();
    latestNotepadText = text;
    CsmNetwork.sendToServer(new ComputerNotepadPacket(tilePos, text, shutdown));
  }

  // === Calculator logic ===

  private void handleCalcButton(String label) {
    if (calcError && !"C".equals(label)) {
      return;
    }
    char c = label.charAt(0);
    if (c >= '0' && c <= '9') {
      onCalcDigit(c);
    } else if (c == 'C') {
      onCalcClear();
    } else if (c == '=') {
      onCalcEquals();
    } else {
      // +, -, *, /
      onCalcOperator(c);
    }
  }

  private void onCalcDigit(char digit) {
    if (calcAwaitingNewInput) {
      calcDisplay = String.valueOf(digit);
      calcAwaitingNewInput = false;
    } else {
      // Cap input length so we don't overflow the display.
      if (calcDisplay.length() >= 12) {
        return;
      }
      if ("0".equals(calcDisplay)) {
        calcDisplay = String.valueOf(digit);
      } else {
        calcDisplay = calcDisplay + digit;
      }
    }
  }

  private void onCalcClear() {
    calcAccumulator = 0.0;
    calcPendingOp = 0;
    calcDisplay = "0";
    calcAwaitingNewInput = true;
    calcError = false;
  }

  private void onCalcEquals() {
    if (calcPendingOp == 0) {
      return;
    }
    double current = parseCalcDisplay();
    double result = applyOp(calcAccumulator, current, calcPendingOp);
    if (Double.isNaN(result) || Double.isInfinite(result)) {
      calcError = true;
      calcDisplay = "ERR";
    } else {
      calcDisplay = formatCalcResult(result);
      calcAccumulator = result;
    }
    calcPendingOp = 0;
    calcAwaitingNewInput = true;
  }

  private void onCalcOperator(char op) {
    double current = parseCalcDisplay();
    if (calcPendingOp != 0 && !calcAwaitingNewInput) {
      // Chain: apply the previous operator first.
      double result = applyOp(calcAccumulator, current, calcPendingOp);
      if (Double.isNaN(result) || Double.isInfinite(result)) {
        calcError = true;
        calcDisplay = "ERR";
        calcPendingOp = 0;
        return;
      }
      calcAccumulator = result;
      calcDisplay = formatCalcResult(result);
    } else {
      calcAccumulator = current;
    }
    calcPendingOp = op;
    calcAwaitingNewInput = true;
  }

  private double parseCalcDisplay() {
    try {
      return Double.parseDouble(calcDisplay);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private static double applyOp(double a, double b, char op) {
    switch (op) {
      case '+': return a + b;
      case '-': return a - b;
      case '*': return a * b;
      case '/': return b == 0.0 ? Double.NaN : a / b;
      default: return b;
    }
  }

  /** Format a double for the calculator display: drop trailing ".0" for whole numbers. */
  private static String formatCalcResult(double v) {
    if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
      return String.format("%d", (long) v);
    }
    String s = String.format("%.6f", v);
    // Trim trailing zeros after the decimal point.
    int dot = s.indexOf('.');
    if (dot >= 0) {
      int end = s.length();
      while (end > dot + 2 && s.charAt(end - 1) == '0') end--;
      s = s.substring(0, end);
    }
    return s;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
    super.onGuiClosed();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
