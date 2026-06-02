package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import com.micatechnologies.minecraft.csm.hvac.HvacTemperatureManager;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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
 * Gag desktop GUI for the iMac, iMac Pro, and MacBook Pro. CSM-OS hosts a handful of "apps":
 *
 * <ul>
 *   <li><b>Notepad</b> — persistent free-form text stored on {@link TileEntityComputer},
 *   plus a Fortune button that cycles a small palette of jokey one-liners.</li>
 *   <li><b>Calculator</b> — basic 4-function calculator (digits 0–9, +, −, ×, ÷, =, C).
 *   Mouse-only; calculator state is per-GUI-instance and resets on close.</li>
 *   <li><b>Weather</b> — read-only panel showing the player's biome, biome temperature,
 *   the conditioned indoor temperature from {@link HvacTemperatureManager}, the sky state
 *   (clear / rain / thunder), and the in-game day count.</li>
 *   <li><b>Minesweeper</b> — classic 9×9 / 10-mine game. Left-click reveals, right-click
 *   flags; the first reveal is always safe. Custom-drawn grid (not GuiButtons) so it can
 *   handle right-clicks and per-cell rendering.</li>
 *   <li><b>Terminal</b> — gag shell with a few commands (help, date, weather, fortune,
 *   echo, whoami, clear) that reuse the same live world data as the Weather app.</li>
 *   <li><b>Snake</b> — arcade Snake driven by {@link #updateScreen()} ticks and the arrow
 *   keys; walls and self-collision end the round.</li>
 *   <li><b>About This PC</b> — gag spec sheet per model plus a live "uptime" = in-game day.</li>
 * </ul>
 *
 * <p>Tabs along the top of the window switch apps. The bottom button row changes per app:
 * Notepad shows Save / Cancel / Fortune / Shutdown; Minesweeper adds New, Snake adds New,
 * Terminal adds Clear; the rest show only Cancel / Shutdown. Games and the terminal keep
 * all state per-GUI-instance (they reset when the window closes) — only the notepad persists.</p>
 *
 * <p>The top menu bar's clock matches the HVAC thermostat's formatting (12-hour, +6 hour
 * Minecraft offset so 0 ticks reads as 6:00 AM).</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ComputerGui extends GuiScreen {

  // === Layout constants ===
  // Widened from the original 320×200 to give the extra tabs room and the games a usable board.
  private static final int WINDOW_W_TARGET = 360;
  private static final int WINDOW_H_TARGET = 210;
  private static final int TITLE_BAR_H = 14;
  private static final int TAB_STRIP_H = 14;
  private static final int BUTTON_ROW_H = 22;
  private static final int CONTENT_PADDING = 8;

  // Minesweeper board: classic beginner dimensions.
  private static final int MINES_COLS = 9;
  private static final int MINES_ROWS = 9;
  private static final int MINES_COUNT = 10;
  private static final int MINES_STATUS_H = 14;

  // Snake board cell size (model units) and how many client ticks pass between moves.
  private static final int SNAKE_CELL = 10;
  private static final int SNAKE_STATUS_H = 14;
  private static final int SNAKE_TICKS_PER_MOVE = 3;

  // Terminal scrollback cap (oldest lines drop off the top).
  private static final int TERMINAL_MAX_LINES = 200;

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
  // Minesweeper palette
  private static final int COLOR_MINES_HIDDEN = 0xFFB9C0CC;
  private static final int COLOR_MINES_HIDDEN_HI = 0xFFD2D8E2;
  private static final int COLOR_MINES_REVEALED = 0xFFE7EAF0;
  private static final int COLOR_MINES_GRID_LINE = 0xFF8B93A2;
  private static final int COLOR_MINES_MINE = 0xFF252A33;
  private static final int COLOR_MINES_FLAG = 0xFFC8312B;
  // Per-count number colors (index 1..8), classic Minesweeper-ish.
  private static final int[] COLOR_MINES_NUMBERS = {
      0x00000000, 0xFF2A47D8, 0xFF1E7D34, 0xFFC42B2B, 0xFF1B2A6B,
      0xFF7A1E1E, 0xFF1E7A7A, 0xFF202020, 0xFF6B6B6B
  };
  // Snake / terminal palette
  private static final int COLOR_SNAKE_BG = 0xFF0E1622;
  private static final int COLOR_SNAKE_GRID = 0xFF16202F;
  private static final int COLOR_SNAKE_BODY = 0xFF66D17A;
  private static final int COLOR_SNAKE_HEAD = 0xFFB6F2C2;
  private static final int COLOR_SNAKE_FOOD = 0xFFE6543C;
  private static final int COLOR_TERM_BG = 0xFF0B0E12;
  private static final int COLOR_TERM_TEXT = 0xFF9BE6A0;
  private static final int COLOR_TERM_PROMPT = 0xFF6FE0D2;

  // === Button IDs ===
  private static final int BUTTON_ID_NOTEPAD_SAVE = 0;
  private static final int BUTTON_ID_CANCEL = 1;
  private static final int BUTTON_ID_NOTEPAD_FORTUNE = 2;
  private static final int BUTTON_ID_SHUTDOWN = 3;
  private static final int BUTTON_ID_MINES_NEW = 4;
  private static final int BUTTON_ID_SNAKE_NEW = 5;
  private static final int BUTTON_ID_TERMINAL_CLEAR = 6;
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
    NOTEPAD("Notepad", "Notepad"),
    CALCULATOR("Calculator", "Calc"),
    WEATHER("Weather", "Weather"),
    MINESWEEPER("Minesweeper", "Mines"),
    TERMINAL("Terminal", "Term"),
    SNAKE("Snake", "Snake"),
    ABOUT("About This PC", "About");

    /** Full name, shown in the window title bar. */
    final String label;
    /** Short name, shown in the (space-constrained) tab strip. */
    final String tab;

    App(String label, String tab) {
      this.label = label;
      this.tab = tab;
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
  /** Block registry path ("imac" / "imacpro" / "mbp"), used by the About app for fake specs. */
  private final String modelId;
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

  // === Minesweeper state ===
  /** true once the board has been generated (on the first reveal, so it's always safe). */
  private boolean minesGenerated = false;
  private boolean minesGameOver = false;
  private boolean minesWon = false;
  private final boolean[][] minesMine = new boolean[MINES_ROWS][MINES_COLS];
  private final boolean[][] minesRevealed = new boolean[MINES_ROWS][MINES_COLS];
  private final boolean[][] minesFlagged = new boolean[MINES_ROWS][MINES_COLS];
  /** Adjacent-mine counts, valid only where {@link #minesGenerated} is true. */
  private final int[][] minesAdjacent = new int[MINES_ROWS][MINES_COLS];
  private final Random minesRandom = new Random();
  // Cached board geometry (recomputed in recomputeLayout).
  private int minesCell, minesOriginX, minesOriginY;

  // === Snake state ===
  private final Deque<int[]> snakeBody = new ArrayDeque<>();
  private int snakeDirX = 1, snakeDirY = 0;
  /**
   * Set when the direction has already changed during the current move interval. Limiting input
   * to one turn per step stops a fast up-then-left (while moving right) from folding the snake
   * back into its own neck in a single tick.
   */
  private boolean snakeDirLocked = false;
  private int snakeFoodX, snakeFoodY;
  private int snakeScore = 0;
  private boolean snakeRunning = false;
  private boolean snakeGameOver = false;
  private int snakeTickCounter = 0;
  private final Random snakeRandom = new Random();
  // Cached board geometry (recomputed in recomputeLayout).
  private int snakeCols, snakeRows, snakeBoardX, snakeBoardY;

  // === Terminal state ===
  private final List<String> terminalLines = new ArrayList<>();
  private final StringBuilder terminalInput = new StringBuilder();
  private boolean terminalInitialized = false;

  // === Cached layout (recomputed in initGui) ===
  private int windowX, windowY, windowW, windowH;
  private int contentY, contentH;
  private int[] tabXs = new int[App.values().length];
  private int tabWidth;

  public ComputerGui(TileEntityComputer tile) {
    this.tilePos = tile.getPos();
    this.initialNotepadText = tile.getNotepadText() == null ? "" : tile.getNotepadText();
    this.latestNotepadText = this.initialNotepadText;
    this.modelId = modelIdFromBlock(tile);
    this.computerName = computerNameForModel(this.modelId);
  }

  private static String modelIdFromBlock(TileEntityComputer tile) {
    if (tile.getWorld() == null) {
      return "";
    }
    if (tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName() != null) {
      return tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName().getPath();
    }
    return "";
  }

  private static String computerNameForModel(String modelId) {
    switch (modelId) {
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

    // Minesweeper grid: square cells, centered in the content area below a status line.
    int minesAvailW = windowW - CONTENT_PADDING * 2;
    int minesAvailH = contentH - MINES_STATUS_H;
    minesCell = Math.max(8, Math.min(minesAvailW / MINES_COLS, minesAvailH / MINES_ROWS));
    int minesGridW = minesCell * MINES_COLS;
    int minesGridH = minesCell * MINES_ROWS;
    minesOriginX = windowX + (windowW - minesGridW) / 2;
    minesOriginY = contentY + MINES_STATUS_H + (minesAvailH - minesGridH) / 2;

    // Snake board: as many whole cells as fit below its status line.
    snakeBoardX = windowX + CONTENT_PADDING;
    snakeBoardY = contentY + SNAKE_STATUS_H;
    snakeCols = Math.max(8, (windowW - CONTENT_PADDING * 2) / SNAKE_CELL);
    snakeRows = Math.max(6, (contentH - SNAKE_STATUS_H) / SNAKE_CELL);
  }

  /** Tears down and rebuilds the button list whenever the active app changes. */
  private void rebuildForApp() {
    this.buttonList.clear();

    // Bottom button row: every app ends with Cancel | Shutdown; some prepend an app action.
    switch (currentApp) {
      case NOTEPAD:
        initNotepadApp();
        layoutButtonRow(
            new int[] {BUTTON_ID_NOTEPAD_SAVE, BUTTON_ID_CANCEL, BUTTON_ID_NOTEPAD_FORTUNE,
                BUTTON_ID_SHUTDOWN},
            new String[] {"Save", "Cancel", "Fortune", "Shutdown"});
        break;
      case MINESWEEPER:
        layoutButtonRow(
            new int[] {BUTTON_ID_MINES_NEW, BUTTON_ID_CANCEL, BUTTON_ID_SHUTDOWN},
            new String[] {"New", "Cancel", "Shutdown"});
        break;
      case SNAKE:
        layoutButtonRow(
            new int[] {BUTTON_ID_SNAKE_NEW, BUTTON_ID_CANCEL, BUTTON_ID_SHUTDOWN},
            new String[] {"New", "Cancel", "Shutdown"});
        break;
      case TERMINAL:
        initTerminalApp();
        layoutButtonRow(
            new int[] {BUTTON_ID_TERMINAL_CLEAR, BUTTON_ID_CANCEL, BUTTON_ID_SHUTDOWN},
            new String[] {"Clear", "Cancel", "Shutdown"});
        break;
      case CALCULATOR:
        layoutButtonRow(new int[] {BUTTON_ID_CANCEL, BUTTON_ID_SHUTDOWN},
            new String[] {"Cancel", "Shutdown"});
        initCalculatorApp();
        break;
      default: // WEATHER, ABOUT
        layoutButtonRow(new int[] {BUTTON_ID_CANCEL, BUTTON_ID_SHUTDOWN},
            new String[] {"Cancel", "Shutdown"});
        break;
    }
  }

  /** Lays out a horizontally-centered row of equal-width buttons along the window's bottom edge. */
  private void layoutButtonRow(int[] ids, String[] labels) {
    int btnY = windowY + windowH - 22;
    int btnW = 62;
    int btnGap = 4;
    // Shrink the buttons if the row would overflow the window (e.g. four buttons on a narrow GUI).
    int maxW = (windowW - btnGap * (ids.length - 1) - 8) / ids.length;
    btnW = Math.min(btnW, Math.max(40, maxW));
    int totalW = btnW * ids.length + btnGap * (ids.length - 1);
    int startX = windowX + (windowW - totalW) / 2;
    for (int i = 0; i < ids.length; i++) {
      this.buttonList.add(new GuiButton(ids[i], startX + i * (btnW + btnGap), btnY, btnW, 20,
          labels[i]));
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
      String label = apps[i].tab;
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
      case MINESWEEPER:
        drawMinesweeperApp();
        break;
      case TERMINAL:
        drawTerminalApp();
        break;
      case SNAKE:
        drawSnakeApp();
        break;
      case ABOUT:
        drawAboutApp();
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

  // === Minesweeper app ===

  private void drawMinesweeperApp() {
    String status;
    if (minesWon) {
      status = "You cleared it!  Click New to play again.";
    } else if (minesGameOver) {
      status = "Boom!  Click New to try again.";
    } else {
      status = "Mines left: " + (MINES_COUNT - countMineFlags())
          + "    (left-click reveal · right-click flag)";
    }
    fontRenderer.drawString(trimToWidth(status, windowW - CONTENT_PADDING * 2),
        windowX + CONTENT_PADDING, contentY + 3, COLOR_INFO_TEXT);

    int gridW = minesCell * MINES_COLS;
    int gridH = minesCell * MINES_ROWS;
    // Backdrop doubles as the grid lines: each cell is drawn one pixel short, exposing it.
    drawRect(minesOriginX, minesOriginY, minesOriginX + gridW + 1, minesOriginY + gridH + 1,
        COLOR_MINES_GRID_LINE);

    for (int r = 0; r < MINES_ROWS; r++) {
      for (int c = 0; c < MINES_COLS; c++) {
        int x0 = minesOriginX + c * minesCell + 1;
        int y0 = minesOriginY + r * minesCell + 1;
        int x1 = x0 + minesCell - 1;
        int y1 = y0 + minesCell - 1;
        boolean isMine = minesGenerated && minesMine[r][c];
        boolean showRevealed = minesRevealed[r][c] || (minesGameOver && isMine);

        if (!showRevealed) {
          drawRect(x0, y0, x1, y1, COLOR_MINES_HIDDEN);
          // Bevel highlight on the top/left edges for a raised-key look.
          drawRect(x0, y0, x1, y0 + 1, COLOR_MINES_HIDDEN_HI);
          drawRect(x0, y0, x0 + 1, y1, COLOR_MINES_HIDDEN_HI);
          if (minesFlagged[r][c]) {
            drawMineFlag(x0, y0, minesCell);
          }
        } else {
          drawRect(x0, y0, x1, y1, COLOR_MINES_REVEALED);
          if (isMine) {
            drawMine(x0, y0, minesCell);
          } else if (minesAdjacent[r][c] > 0) {
            drawCellCenteredString(String.valueOf(minesAdjacent[r][c]), x0, y0, minesCell,
                COLOR_MINES_NUMBERS[minesAdjacent[r][c]]);
          }
        }
      }
    }
  }

  private void drawMineFlag(int x0, int y0, int cell) {
    int cx = x0 + cell / 2;
    drawRect(cx, y0 + 2, cx + 1, y0 + cell - 2, COLOR_MINES_MINE);          // pole
    drawRect(cx - 3, y0 + 2, cx, y0 + 5, COLOR_MINES_FLAG);                 // banner
  }

  private void drawMine(int x0, int y0, int cell) {
    int cx = x0 + cell / 2;
    int cy = y0 + cell / 2;
    drawRect(cx - 2, cy - 2, cx + 2, cy + 2, COLOR_MINES_MINE);
  }

  private void drawCellCenteredString(String s, int x0, int y0, int cell, int color) {
    int tx = x0 + (cell - fontRenderer.getStringWidth(s)) / 2;
    int ty = y0 + (cell - fontRenderer.FONT_HEIGHT) / 2;
    fontRenderer.drawString(s, tx, ty, color);
  }

  private int countMineFlags() {
    int n = 0;
    for (int r = 0; r < MINES_ROWS; r++) {
      for (int c = 0; c < MINES_COLS; c++) {
        if (minesFlagged[r][c]) {
          n++;
        }
      }
    }
    return n;
  }

  private void minesReset() {
    minesGenerated = false;
    minesGameOver = false;
    minesWon = false;
    for (int r = 0; r < MINES_ROWS; r++) {
      for (int c = 0; c < MINES_COLS; c++) {
        minesMine[r][c] = false;
        minesRevealed[r][c] = false;
        minesFlagged[r][c] = false;
        minesAdjacent[r][c] = 0;
      }
    }
  }

  /** Lays mines avoiding the player's first-clicked cell, then fills adjacency counts. */
  private void minesGenerate(int safeR, int safeC) {
    int placed = 0;
    while (placed < MINES_COUNT) {
      int r = minesRandom.nextInt(MINES_ROWS);
      int c = minesRandom.nextInt(MINES_COLS);
      if (minesMine[r][c] || (r == safeR && c == safeC)) {
        continue;
      }
      minesMine[r][c] = true;
      placed++;
    }
    for (int r = 0; r < MINES_ROWS; r++) {
      for (int c = 0; c < MINES_COLS; c++) {
        if (minesMine[r][c]) {
          continue;
        }
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
          for (int dc = -1; dc <= 1; dc++) {
            int nr = r + dr;
            int nc = c + dc;
            if (nr >= 0 && nr < MINES_ROWS && nc >= 0 && nc < MINES_COLS && minesMine[nr][nc]) {
              count++;
            }
          }
        }
        minesAdjacent[r][c] = count;
      }
    }
    minesGenerated = true;
  }

  private void minesReveal(int r, int c) {
    if (minesGameOver || minesWon || r < 0 || r >= MINES_ROWS || c < 0 || c >= MINES_COLS) {
      return;
    }
    if (minesRevealed[r][c] || minesFlagged[r][c]) {
      return;
    }
    if (!minesGenerated) {
      minesGenerate(r, c);
    }
    minesRevealed[r][c] = true;
    if (minesMine[r][c]) {
      minesGameOver = true;
      return;
    }
    if (minesAdjacent[r][c] == 0) {
      // Flood-fill across the connected empty region (recursion depth bounded by the 9×9 grid).
      for (int dr = -1; dr <= 1; dr++) {
        for (int dc = -1; dc <= 1; dc++) {
          if (dr != 0 || dc != 0) {
            minesReveal(r + dr, c + dc);
          }
        }
      }
    }
    checkMinesWin();
  }

  private void minesToggleFlag(int r, int c) {
    if (minesGameOver || minesWon || minesRevealed[r][c]) {
      return;
    }
    minesFlagged[r][c] = !minesFlagged[r][c];
  }

  private void checkMinesWin() {
    for (int r = 0; r < MINES_ROWS; r++) {
      for (int c = 0; c < MINES_COLS; c++) {
        if (!minesMine[r][c] && !minesRevealed[r][c]) {
          return;
        }
      }
    }
    minesWon = true;
  }

  private void handleMinesClick(int mouseX, int mouseY, int mouseButton) {
    if (mouseX < minesOriginX || mouseY < minesOriginY) {
      return;
    }
    int c = (mouseX - minesOriginX) / minesCell;
    int r = (mouseY - minesOriginY) / minesCell;
    if (r < 0 || r >= MINES_ROWS || c < 0 || c >= MINES_COLS) {
      return;
    }
    if (mouseButton == 0) {
      minesReveal(r, c);
    } else if (mouseButton == 1) {
      minesToggleFlag(r, c);
    }
  }

  // === Snake app ===

  private void drawSnakeApp() {
    String status;
    if (snakeGameOver) {
      status = "Game over — score " + snakeScore + ".  Press Space or New to restart.";
    } else if (!snakeRunning) {
      status = "Snake — press an arrow key (or New) to start.";
    } else {
      status = "Score: " + snakeScore;
    }
    fontRenderer.drawString(trimToWidth(status, windowW - CONTENT_PADDING * 2),
        windowX + CONTENT_PADDING, contentY + 3, COLOR_INFO_TEXT);

    int boardW = snakeCols * SNAKE_CELL;
    int boardH = snakeRows * SNAKE_CELL;
    drawRect(snakeBoardX, snakeBoardY, snakeBoardX + boardW, snakeBoardY + boardH, COLOR_SNAKE_BG);

    if (snakeRunning || snakeGameOver) {
      drawSnakeCell(snakeFoodX, snakeFoodY, COLOR_SNAKE_FOOD);
      boolean head = true;
      for (int[] seg : snakeBody) {
        drawSnakeCell(seg[0], seg[1], head ? COLOR_SNAKE_HEAD : COLOR_SNAKE_BODY);
        head = false;
      }
    }
  }

  private void drawSnakeCell(int cx, int cy, int color) {
    int x0 = snakeBoardX + cx * SNAKE_CELL;
    int y0 = snakeBoardY + cy * SNAKE_CELL;
    drawRect(x0 + 1, y0 + 1, x0 + SNAKE_CELL - 1, y0 + SNAKE_CELL - 1, color);
  }

  private void snakeStart() {
    snakeBody.clear();
    int sx = snakeCols / 2;
    int sy = snakeRows / 2;
    snakeBody.addFirst(new int[] {sx, sy});
    snakeBody.addLast(new int[] {sx - 1, sy});
    snakeBody.addLast(new int[] {sx - 2, sy});
    snakeDirX = 1;
    snakeDirY = 0;
    snakeDirLocked = false;
    snakeScore = 0;
    snakeRunning = true;
    snakeGameOver = false;
    snakeTickCounter = 0;
    snakePlaceFood();
  }

  private void snakePlaceFood() {
    for (int tries = 0; tries < 500; tries++) {
      int fx = snakeRandom.nextInt(snakeCols);
      int fy = snakeRandom.nextInt(snakeRows);
      if (!snakeOccupies(fx, fy)) {
        snakeFoodX = fx;
        snakeFoodY = fy;
        return;
      }
    }
  }

  private boolean snakeOccupies(int x, int y) {
    for (int[] seg : snakeBody) {
      if (seg[0] == x && seg[1] == y) {
        return true;
      }
    }
    return false;
  }

  /** Advances the snake one cell; ends the round on a wall or self collision. */
  private void snakeStep() {
    snakeDirLocked = false; // a new turn may be queued for the next interval
    int[] head = snakeBody.peekFirst();
    int nx = head[0] + snakeDirX;
    int ny = head[1] + snakeDirY;
    if (nx < 0 || nx >= snakeCols || ny < 0 || ny >= snakeRows || snakeOccupies(nx, ny)) {
      snakeGameOver = true;
      snakeRunning = false;
      return;
    }
    snakeBody.addFirst(new int[] {nx, ny});
    if (nx == snakeFoodX && ny == snakeFoodY) {
      snakeScore++;
      snakePlaceFood();
    } else {
      snakeBody.removeLast();
    }
  }

  private void handleSnakeKey(int keyCode) {
    switch (keyCode) {
      case Keyboard.KEY_UP:
      case Keyboard.KEY_W:
        snakeSetDirection(0, -1);
        break;
      case Keyboard.KEY_DOWN:
      case Keyboard.KEY_S:
        snakeSetDirection(0, 1);
        break;
      case Keyboard.KEY_LEFT:
      case Keyboard.KEY_A:
        snakeSetDirection(-1, 0);
        break;
      case Keyboard.KEY_RIGHT:
      case Keyboard.KEY_D:
        snakeSetDirection(1, 0);
        break;
      case Keyboard.KEY_SPACE:
      case Keyboard.KEY_RETURN:
        if (!snakeRunning) {
          snakeStart();
        }
        break;
      default:
        break;
    }
  }

  private void snakeSetDirection(int dx, int dy) {
    if (snakeGameOver) {
      return; // must restart via Space/New first
    }
    if (!snakeRunning) {
      snakeStart();
      return; // the start already faces right; don't also turn on the same key press
    }
    if (snakeDirLocked) {
      return; // already turned this interval — ignore until the next step
    }
    // Reject a 180° reversal into the neck (and a no-op repeat of the current heading).
    if (dx == -snakeDirX && dy == -snakeDirY) {
      return;
    }
    if (dx == snakeDirX && dy == snakeDirY) {
      return;
    }
    snakeDirX = dx;
    snakeDirY = dy;
    snakeDirLocked = true;
  }

  // === Terminal app ===

  private void initTerminalApp() {
    if (terminalInitialized) {
      return;
    }
    terminalInitialized = true;
    terminalLines.add("CSM-OS Terminal v1.0");
    terminalLines.add("Type 'help' for a list of commands.");
    terminalLines.add("");
  }

  private void drawTerminalApp() {
    int x0 = windowX + CONTENT_PADDING;
    int areaTop = contentY + 3;
    int areaBottom = contentY + contentH - 3;
    drawRect(x0 - 2, areaTop - 1, windowX + windowW - CONTENT_PADDING + 2, areaBottom + 1,
        COLOR_TERM_BG);

    int lineH = fontRenderer.FONT_HEIGHT + 1;
    int areaW = windowW - CONTENT_PADDING * 2;
    int promptY = areaBottom - lineH;
    int maxLines = Math.max(0, (promptY - areaTop) / lineH);
    int start = Math.max(0, terminalLines.size() - maxLines);
    int ty = areaTop;
    for (int i = start; i < terminalLines.size(); i++) {
      fontRenderer.drawString(trimToWidth(terminalLines.get(i), areaW), x0, ty, COLOR_TERM_TEXT);
      ty += lineH;
    }
    String prompt = "csm> " + terminalInput + "_";
    fontRenderer.drawString(trimToWidth(prompt, areaW), x0, promptY, COLOR_TERM_PROMPT);
  }

  private void handleTerminalKey(char typedChar, int keyCode) {
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      terminalExecute();
      return;
    }
    if (keyCode == Keyboard.KEY_BACK) {
      if (terminalInput.length() > 0) {
        terminalInput.deleteCharAt(terminalInput.length() - 1);
      }
      return;
    }
    if (typedChar >= 32 && typedChar != 127 && terminalInput.length() < 120) {
      terminalInput.append(typedChar);
    }
  }

  private void terminalExecute() {
    String cmd = terminalInput.toString().trim();
    terminalInput.setLength(0);
    pushTerminal("csm> " + cmd);
    if (!cmd.isEmpty()) {
      runTerminalCommand(cmd);
    }
    while (terminalLines.size() > TERMINAL_MAX_LINES) {
      terminalLines.remove(0);
    }
  }

  private void runTerminalCommand(String cmd) {
    String[] parts = cmd.split("\\s+", 2);
    String name = parts[0].toLowerCase();
    String arg = parts.length > 1 ? parts[1] : "";
    switch (name) {
      case "help":
        pushTerminal("Commands: help, date, time, weather, fortune, echo, whoami, ver, clear, "
            + "exit");
        break;
      case "date":
      case "time":
        pushTerminal(formatRightMenu());
        break;
      case "weather":
        for (String line : terminalWeatherLines()) {
          pushTerminal(line);
        }
        break;
      case "fortune":
        cycleFortune();
        pushTerminal(fortuneText);
        break;
      case "echo":
        pushTerminal(arg);
        break;
      case "whoami":
        pushTerminal(this.mc.player != null ? this.mc.player.getName() : "player");
        break;
      case "ver":
      case "about":
        pushTerminal("CSM-OS 1.0 — " + computerName);
        break;
      case "clear":
        terminalLines.clear();
        break;
      case "exit":
      case "shutdown":
        this.mc.displayGuiScreen(null);
        break;
      default:
        pushTerminal("Unknown command: " + name + "  (try 'help')");
        break;
    }
  }

  private String[] terminalWeatherLines() {
    World world = this.mc.world;
    EntityPlayer player = this.mc.player;
    if (world == null || player == null) {
      return new String[] {"No world data available."};
    }
    BlockPos pos = player.getPosition();
    Biome biome = world.getBiome(pos);
    String sky = world.isThundering() ? "Thunderstorm"
        : world.isRaining() ? (biome.canRain() ? "Raining" : "Overcast") : "Clear";
    float ambientF = HvacTemperatureManager.getTemperatureAt(world, pos);
    return new String[] {
        "Biome: " + biome.getBiomeName(),
        "Climate: " + describeClimate(biome.getTemperature(pos), biome.getRainfall()),
        String.format("Temp: %.1f°F    Sky: %s", ambientF, sky),
    };
  }

  private void pushTerminal(String line) {
    terminalLines.add(line == null ? "" : line);
  }

  // === About app ===

  private void drawAboutApp() {
    int x = windowX + CONTENT_PADDING + 4;
    int y = contentY + 8;
    int lineH = 12;
    fontRenderer.drawString("About This PC", x, y, COLOR_INFO_HIGHLIGHT);
    y += lineH + 2;
    for (String line : aboutLines()) {
      fontRenderer.drawString(line, x, y, COLOR_INFO_TEXT);
      y += lineH;
    }
  }

  /** Gag spec sheet, varied by model, with a live "uptime" tied to the in-game day count. */
  private String[] aboutLines() {
    String model;
    String cpu;
    String gpu;
    String ram;
    String storage;
    switch (modelId) {
      case "imac":
        model = "iMac (CSM Edition)";
        cpu = "3.6 GHz 10-core Redstone i9";
        gpu = "Radeon Creeper Pro 5700";
        ram = "32 GB DDR-Emerald";
        storage = "2 TB Chiseled SSD";
        break;
      case "imacpro":
        model = "iMac Pro";
        cpu = "3.2 GHz 18-core Xeon Netherite";
        gpu = "Radeon Pro Vega II Duo";
        ram = "128 GB ECC";
        storage = "4 TB SSD";
        break;
      case "mbp":
        model = "MacBook Pro";
        cpu = "M-Mojang 16-core";
        gpu = "38-core integrated";
        ram = "64 GB unified";
        storage = "2 TB SSD";
        break;
      default:
        model = "CSM Workstation";
        cpu = "Redstone Core (unknown)";
        gpu = "Integrated";
        ram = "16 GB";
        storage = "512 GB SSD";
        break;
    }
    long days = this.mc.world != null ? this.mc.world.getWorldTime() / 24000L + 1L : 1L;
    return new String[] {
        "Model:      " + model,
        "Processor:  " + cpu,
        "Graphics:   " + gpu,
        "Memory:     " + ram,
        "Storage:    " + storage,
        "OS:         CSM-OS 1.0 (build 2026.5)",
        "Uptime:     Day " + days,
    };
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
    if (keyCode == Keyboard.KEY_ESCAPE) {
      // ESC is the natural close gesture; silently discarding typed notepad text
      // would be an easy footgun. Save before closing if we're on the Notepad tab.
      // Other tabs have already auto-flushed to the server on tab leave, so nothing
      // extra to do for them.
      if (currentApp == App.NOTEPAD && notepad != null) {
        sendNotepad(false);
      }
      this.mc.displayGuiScreen(null);
      return;
    }
    // Route typing to the active app. Terminal and Snake consume keys themselves; everything
    // else falls through to the default GuiScreen handling (a no-op for non-ESC keys).
    switch (currentApp) {
      case NOTEPAD:
        super.keyTyped(typedChar, keyCode);
        if (notepad != null) {
          notepad.setFocused(true);
          notepad.textboxKeyTyped(typedChar, keyCode);
        }
        break;
      case TERMINAL:
        handleTerminalKey(typedChar, keyCode);
        break;
      case SNAKE:
        handleSnakeKey(keyCode);
        break;
      default:
        super.keyTyped(typedChar, keyCode);
        break;
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
    } else if (currentApp == App.MINESWEEPER) {
      // super.mouseClicked only dispatches left-clicks to GuiButtons, so right-clicks on the
      // grid arrive here uncontested; the bottom button row sits outside the grid bounds.
      handleMinesClick(mouseX, mouseY, mouseButton);
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

  /** Drives Snake's movement: advance one cell every {@link #SNAKE_TICKS_PER_MOVE} client ticks. */
  @Override
  public void updateScreen() {
    super.updateScreen();
    if (currentApp == App.SNAKE && snakeRunning && !snakeGameOver) {
      if (++snakeTickCounter >= SNAKE_TICKS_PER_MOVE) {
        snakeTickCounter = 0;
        snakeStep();
      }
    }
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
    if (id == BUTTON_ID_MINES_NEW) {
      minesReset();
      return;
    }
    if (id == BUTTON_ID_SNAKE_NEW) {
      snakeStart();
      return;
    }
    if (id == BUTTON_ID_TERMINAL_CLEAR) {
      terminalLines.clear();
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
