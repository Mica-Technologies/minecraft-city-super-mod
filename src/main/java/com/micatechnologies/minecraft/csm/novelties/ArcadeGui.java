package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/**
 * The screen a player looks at while using the multi-game arcade cabinet. It opens on a select
 * screen listing every game in {@link ArcadeCatalog} with this cabinet's record beside it, runs the
 * chosen game at a fixed step rate independent of frame rate, and posts the score back to the
 * cabinet when the run ends.
 *
 * <p>The simulation is driven from {@link #drawScreen} rather than {@code updateScreen} so the games
 * run at their own 60 Hz rather than Minecraft's 20 Hz client tick — at 20 Hz the driving cabinets
 * in particular feel like they are running in treacle.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGui extends GuiScreen {

  // === Layout ===
  private static final int BEZEL_PADDING = 10;
  private static final int HUD_HEIGHT = 24;
  private static final int FOOTER_HEIGHT = 16;
  /**
   * The aspect ratio the cabinet uses while sitting on the select screen, before a game has
   * declared its own playfield shape.
   */
  private static final float MENU_ASPECT = 4F / 3F;

  // === Colours ===
  private static final int COLOR_BACKDROP = 0xE0080810;
  private static final int COLOR_CABINET = 0xFF1A1A24;
  private static final int COLOR_BEZEL = 0xFF3A3A4C;
  private static final int COLOR_MARQUEE = 0xFF40E0FF;
  private static final int COLOR_HUD = 0xFFE0E0E0;
  private static final int COLOR_DIM = 0xFF808898;
  private static final int COLOR_SELECTED = 0xFFFFD040;

  // === Simulation timing ===
  /**
   * Nanoseconds per fixed simulation step.
   */
  private static final long STEP_NANOS = 1_000_000_000L / ArcadeGame.TICK_RATE;
  /**
   * The most steps that may be run for a single frame, so a stall cannot spiral.
   */
  private static final int MAX_STEPS_PER_FRAME = 5;
  /**
   * Steps between repeats while a direction is held on the select screen.
   */
  private static final int MENU_REPEAT_FRAMES = 6;

  /**
   * The cabinet's position, used to address the high score packet.
   */
  private final BlockPos cabinetPos;

  /**
   * The cabinet's record for each catalogue entry, as known to the client.
   */
  private final int[] highScores = new int[ArcadeCatalog.GAME_IDS.length];

  /**
   * The name attached to each entry of {@link #highScores}.
   */
  private final String[] highScoreHolders = new String[ArcadeCatalog.GAME_IDS.length];

  /**
   * The control panel state handed to the game each step.
   */
  private final ArcadeInput input = new ArcadeInput();

  /**
   * The renderer the game draws through.
   */
  private ArcadeScreen arcadeScreen;

  /**
   * The catalogue entry highlighted on the select screen.
   */
  private int selection;

  /**
   * Frames remaining before a held direction repeats on the select screen.
   */
  private int menuRepeatDelay;

  /**
   * The game currently loaded, or {@code null} while on the select screen.
   */
  private ArcadeGame game;

  /**
   * The catalogue index of {@link #game}.
   */
  private int loadedIndex = -1;

  /**
   * Whether a run is in progress.
   */
  private boolean playing;

  /**
   * Whether the last run has ended and its results are still on screen.
   */
  private boolean showingGameOver;

  /**
   * Timestamp of the previous frame, or zero before the first one.
   */
  private long lastFrameNanos;

  /**
   * Leftover time carried between frames, in nanoseconds.
   */
  private long stepAccumulator;

  // === Cached layout, recomputed whenever the loaded game changes ===
  private int cabinetX;
  private int cabinetY;
  private int cabinetWidth;
  private int cabinetHeight;
  private int viewX;
  private int viewY;
  private int viewWidth;
  private int viewHeight;

  /**
   * Constructs an {@link ArcadeGui} for the specified cabinet.
   *
   * @param cabinet the cabinet being played
   *
   * @since 1.0
   */
  public ArcadeGui(TileEntityArcadeCabinet cabinet) {
    this.cabinetPos = cabinet.getPos();
    for (int i = 0; i < ArcadeCatalog.GAME_IDS.length; i++) {
      highScores[i] = cabinet.getHighScore(ArcadeCatalog.GAME_IDS[i]);
      highScoreHolders[i] = cabinet.getHighScoreHolder(ArcadeCatalog.GAME_IDS[i]);
    }
  }

  /**
   * Resolves a catalogue identifier to a fresh game instance. This is the only place the game
   * classes are named, and it lives on the client so a dedicated server never loads them.
   *
   * @param gameId the identifier from {@link ArcadeCatalog#GAME_IDS}
   *
   * @return a new game instance; Street Sweeper stands in for an unrecognised identifier
   *
   * @since 1.0
   */
  private static ArcadeGame createGame(String gameId) {
    switch (gameId) {
      case "salvagerun":
        return new ArcadeGameSalvageRun();
      case "sitesurvey":
        return new ArcadeGameSiteSurvey();
      case "pipepressure":
        return new ArcadeGamePipePressure();
      case "highrise":
        return new ArcadeGameHighRise();
      case "loadbalance":
        return new ArcadeGameLoadBalance();
      case "tunnelrunner":
        return new ArcadeGameTunnelRunner();
      default:
        return new ArcadeGameStreetSweeper();
    }
  }

  /**
   * Sets up the renderer and computes the cabinet layout for the current resolution.
   *
   * @since 1.0
   */
  @Override
  public void initGui() {
    super.initGui();
    arcadeScreen = new ArcadeScreen(this.fontRenderer);
    recomputeLayout();
    lastFrameNanos = 0L;
    stepAccumulator = 0L;
  }

  /**
   * Sizes the cabinet to the largest rectangle matching the loaded game's aspect ratio that fits in
   * the window.
   *
   * @since 1.0
   */
  private void recomputeLayout() {
    ScaledResolution resolution = new ScaledResolution(this.mc);
    int screenWidth = resolution.getScaledWidth();
    int screenHeight = resolution.getScaledHeight();

    int availableWidth = Math.max(80, screenWidth - 40);
    int availableHeight = Math.max(80, screenHeight - 40 - HUD_HEIGHT - FOOTER_HEIGHT);

    float aspect = game == null ? MENU_ASPECT
        : game.getPlayfieldWidth() / game.getPlayfieldHeight();
    viewWidth = availableWidth;
    viewHeight = (int) (viewWidth / aspect);
    if (viewHeight > availableHeight) {
      viewHeight = availableHeight;
      viewWidth = (int) (viewHeight * aspect);
    }

    cabinetWidth = viewWidth + BEZEL_PADDING * 2;
    cabinetHeight = viewHeight + BEZEL_PADDING * 2 + HUD_HEIGHT + FOOTER_HEIGHT;
    cabinetX = (screenWidth - cabinetWidth) / 2;
    cabinetY = (screenHeight - cabinetHeight) / 2;
    viewX = cabinetX + BEZEL_PADDING;
    viewY = cabinetY + BEZEL_PADDING + HUD_HEIGHT;

    if (game != null) {
      arcadeScreen.setViewport(viewX, viewY, viewWidth / game.getPlayfieldWidth());
    }
  }

  /**
   * Retrieves whether the screen pauses a singleplayer world. It does, so a player is not eaten
   * while looking at the cabinet.
   *
   * @return {@code true}
   *
   * @since 1.0
   */
  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  /**
   * Draws the cabinet and advances whatever is loaded.
   *
   * @param mouseX       the mouse X position
   * @param mouseY       the mouse Y position
   * @param partialTicks the frame's partial tick
   *
   * @since 1.0
   */
  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawRect(0, 0, this.width, this.height, COLOR_BACKDROP);

    pollControls(mouseX, mouseY);
    advanceSimulation();

    drawCabinet();
    drawHud();

    if (game != null) {
      arcadeScreen.beginFrame();
      game.render(arcadeScreen);
      arcadeScreen.flush();
    }

    if (!playing) {
      drawOverlayScreen();
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  /**
   * Reads the keyboard and mouse into the {@link ArcadeInput} handed to the game.
   *
   * @param mouseX the mouse X position
   * @param mouseY the mouse Y position
   *
   * @since 1.0
   */
  private void pollControls(int mouseX, int mouseY) {
    input.setHeld(ArcadeInput.Button.LEFT,
        Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A));
    input.setHeld(ArcadeInput.Button.RIGHT,
        Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D));
    input.setHeld(ArcadeInput.Button.UP,
        Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W));
    input.setHeld(ArcadeInput.Button.DOWN,
        Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S));
    input.setHeld(ArcadeInput.Button.FIRE,
        Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Keyboard.isKeyDown(Keyboard.KEY_Z));
    input.setHeld(ArcadeInput.Button.ALT,
        Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_X));
    if (game != null) {
      input.setPointer(arcadeScreen.toPlayfieldX(mouseX), arcadeScreen.toPlayfieldY(mouseY));
    }
  }

  /**
   * Runs however many fixed steps the elapsed wall-clock time calls for, or drives the select
   * screen when no game is running.
   *
   * @since 1.0
   */
  private void advanceSimulation() {
    long now = System.nanoTime();
    if (lastFrameNanos == 0L) {
      lastFrameNanos = now;
      return;
    }
    long elapsed = now - lastFrameNanos;
    lastFrameNanos = now;

    if (!playing) {
      updateMenu();
      input.clearEdges();
      return;
    }

    stepAccumulator += elapsed;
    int steps = 0;
    while (stepAccumulator >= STEP_NANOS && steps < MAX_STEPS_PER_FRAME) {
      game.step(input);
      // Edges belong to exactly one step, so a tap cannot fire twice in a catch-up frame.
      input.clearEdges();
      stepAccumulator -= STEP_NANOS;
      steps++;
      if (game.isGameOver()) {
        endRun();
        break;
      }
    }
    if (steps == MAX_STEPS_PER_FRAME) {
      // Fell too far behind to catch up; drop the backlog rather than fast-forwarding the game.
      stepAccumulator = 0L;
    }
  }

  /**
   * Moves the highlight on the select screen. Held keys repeat on a delay so the list steps rather
   * than sprinting past every title.
   *
   * @since 1.0
   */
  private void updateMenu() {
    if (menuRepeatDelay > 0) {
      menuRepeatDelay--;
      return;
    }
    if (input.held(ArcadeInput.Button.UP)) {
      selection = (selection + ArcadeCatalog.GAME_IDS.length - 1) % ArcadeCatalog.GAME_IDS.length;
      menuRepeatDelay = MENU_REPEAT_FRAMES;
      showingGameOver = false;
    } else if (input.held(ArcadeInput.Button.DOWN)) {
      selection = (selection + 1) % ArcadeCatalog.GAME_IDS.length;
      menuRepeatDelay = MENU_REPEAT_FRAMES;
      showingGameOver = false;
    }
  }

  /**
   * Ends the current run and posts the score if it beat this cabinet's record for the game.
   *
   * @since 1.0
   */
  private void endRun() {
    playing = false;
    showingGameOver = true;
    int finalScore = game.getScore();
    if (loadedIndex >= 0 && finalScore > highScores[loadedIndex]) {
      highScores[loadedIndex] = finalScore;
      highScoreHolders[loadedIndex] = this.mc.player == null ? "" : this.mc.player.getName();
      CsmNetwork.sendToServer(new ArcadeHighScorePacket(cabinetPos,
          ArcadeCatalog.GAME_IDS[loadedIndex], finalScore));
    }
  }

  /**
   * Loads and starts the highlighted game.
   *
   * @since 1.0
   */
  private void startRun() {
    // A fresh instance every time, so nothing carries over between runs.
    game = createGame(ArcadeCatalog.GAME_IDS[selection]);
    loadedIndex = selection;
    recomputeLayout();
    game.reset();
    input.clearAll();
    playing = true;
    showingGameOver = false;
    stepAccumulator = 0L;
    lastFrameNanos = System.nanoTime();
  }

  /**
   * Unloads the current game and returns to the select screen.
   *
   * @since 1.0
   */
  private void returnToMenu() {
    game = null;
    loadedIndex = -1;
    playing = false;
    showingGameOver = false;
    recomputeLayout();
  }

  /**
   * Draws the cabinet shell, its marquee and the bezel around the screen.
   *
   * @since 1.0
   */
  private void drawCabinet() {
    drawRect(cabinetX - 4, cabinetY - 4, cabinetX + cabinetWidth + 4,
        cabinetY + cabinetHeight + 4, COLOR_CABINET);
    drawRect(cabinetX, cabinetY, cabinetX + cabinetWidth, cabinetY + cabinetHeight, COLOR_BEZEL);
    drawRect(viewX - 1, viewY - 1, viewX + viewWidth + 1, viewY + viewHeight + 1, 0xFF000000);
  }

  /**
   * Draws the marquee title, the score line and the footer prompt.
   *
   * @since 1.0
   */
  private void drawHud() {
    int titleY = cabinetY + 3;
    String marquee = game == null ? "CSM MULTI-GAME" : game.getTitle();
    drawCenteredString(this.fontRenderer, marquee, cabinetX + cabinetWidth / 2, titleY,
        COLOR_MARQUEE);

    int scoreY = titleY + 12;
    if (game != null) {
      this.fontRenderer.drawString("SCORE " + game.getScore(), cabinetX + BEZEL_PADDING, scoreY,
          COLOR_HUD);

      String right = "LIVES " + game.getLives() + "   WAVE " + game.getLevel();
      this.fontRenderer.drawString(right,
          cabinetX + cabinetWidth - BEZEL_PADDING - this.fontRenderer.getStringWidth(right),
          scoreY, COLOR_HUD);

      drawCenteredString(this.fontRenderer, recordLabel(loadedIndex),
          cabinetX + cabinetWidth / 2, scoreY, COLOR_MARQUEE);
    } else {
      drawCenteredString(this.fontRenderer, "SELECT A GAME", cabinetX + cabinetWidth / 2, scoreY,
          COLOR_DIM);
    }

    String footer;
    if (playing) {
      footer = "ESC to walk away";
    } else if (game == null) {
      footer = "Up/Down to choose  -  SPACE to play  -  ESC to walk away";
    } else {
      footer = "SPACE to play again  -  BACKSPACE for the game list  -  ESC to walk away";
    }
    drawCenteredString(this.fontRenderer, footer, cabinetX + cabinetWidth / 2,
        viewY + viewHeight + BEZEL_PADDING - 2, COLOR_DIM);
  }

  /**
   * Builds the record line for a catalogue entry.
   *
   * @param index the catalogue index, or -1 for none
   *
   * @return the record text
   *
   * @since 1.0
   */
  private String recordLabel(int index) {
    if (index < 0 || highScores[index] <= 0) {
      return "NO RECORD YET";
    }
    String holder = highScoreHolders[index];
    return "BEST " + highScores[index] + (holder == null || holder.isEmpty() ? "" : " - " + holder);
  }

  /**
   * Draws the select screen, or the results panel after a run.
   *
   * @since 1.0
   */
  private void drawOverlayScreen() {
    Gui.drawRect(viewX, viewY, viewX + viewWidth, viewY + viewHeight,
        game == null ? 0xFF06080E : 0xC0000000);

    int centerX = viewX + viewWidth / 2;

    if (showingGameOver && game != null) {
      int centerY = viewY + viewHeight / 2;
      drawCenteredString(this.fontRenderer, "GAME OVER", centerX, centerY - 26, 0xFFFF6060);
      drawCenteredString(this.fontRenderer, "FINAL SCORE " + game.getScore(), centerX,
          centerY - 12, COLOR_HUD);
      if (loadedIndex >= 0 && game.getScore() >= highScores[loadedIndex] && game.getScore() > 0) {
        drawCenteredString(this.fontRenderer, "NEW CABINET RECORD", centerX, centerY + 2,
            COLOR_MARQUEE);
      }
      drawCenteredString(this.fontRenderer, "Press SPACE to play again", centerX, centerY + 20,
          COLOR_DIM);
      return;
    }

    drawGameList(centerX);
  }

  /**
   * Draws the list of installed games with this cabinet's record beside each.
   *
   * @param centerX the horizontal centre of the screen area
   *
   * @since 1.0
   */
  private void drawGameList(int centerX) {
    int rowHeight = 16;
    int listHeight = ArcadeCatalog.GAME_IDS.length * rowHeight;
    int headerY = viewY + Math.max(6, (viewHeight - listHeight) / 2 - 34);

    drawCenteredString(this.fontRenderer, "CSM MULTI-GAME BOARD", centerX, headerY,
        COLOR_MARQUEE);
    drawCenteredString(this.fontRenderer, "SEVEN TITLES INSTALLED", centerX, headerY + 11,
        COLOR_DIM);

    int listY = headerY + 30;
    int rowLeft = viewX + 12;
    int rowRight = viewX + viewWidth - 12;

    for (int i = 0; i < ArcadeCatalog.GAME_IDS.length; i++) {
      int y = listY + i * rowHeight;
      boolean selected = i == selection;
      if (selected) {
        Gui.drawRect(rowLeft - 4, y - 3, rowRight + 4, y + 11, 0x40FFD040);
      }
      int color = selected ? COLOR_SELECTED : COLOR_HUD;
      this.fontRenderer.drawString((selected ? "> " : "  ") + ArcadeCatalog.GAME_TITLES[i],
          rowLeft, y, color);

      String record = highScores[i] > 0 ? String.valueOf(highScores[i]) : "---";
      this.fontRenderer.drawString(record,
          rowRight - this.fontRenderer.getStringWidth(record), y, selected ? COLOR_SELECTED
              : COLOR_DIM);
    }

    // Blurb and record holder for whatever is highlighted.
    int blurbY = listY + listHeight + 8;
    drawCenteredString(this.fontRenderer, ArcadeCatalog.GAME_BLURBS[selection], centerX, blurbY,
        COLOR_DIM);
    drawCenteredString(this.fontRenderer, recordLabel(selection), centerX, blurbY + 11,
        COLOR_MARQUEE);
  }

  /**
   * Handles key presses. Space starts or restarts a run, backspace returns to the game list, and
   * escape falls through to the default handling that closes the screen.
   *
   * @param typedChar the character typed
   * @param keyCode   the LWJGL key code
   *
   * @throws IOException if the superclass handler throws
   * @since 1.0
   */
  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_ESCAPE) {
      super.keyTyped(typedChar, keyCode);
      return;
    }

    if (!playing) {
      if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN) {
        // After a run, space replays the same game; from the list it starts the highlighted one.
        if (showingGameOver && loadedIndex >= 0) {
          selection = loadedIndex;
        }
        startRun();
        return;
      }
      if (keyCode == Keyboard.KEY_BACK && game != null) {
        returnToMenu();
        return;
      }
      super.keyTyped(typedChar, keyCode);
    }

    // Anything else during play is swallowed, so the games' keys never leak into the world.
  }

  /**
   * Handles mouse clicks: picking a title on the select screen, and firing in the games that want a
   * pointer.
   *
   * @param mouseX      the mouse X position
   * @param mouseY      the mouse Y position
   * @param mouseButton the button clicked
   *
   * @throws IOException if the superclass handler throws
   * @since 1.0
   */
  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if (playing) {
      if (mouseButton == 0) {
        input.setPointer(arcadeScreen.toPlayfieldX(mouseX), arcadeScreen.toPlayfieldY(mouseY));
        input.setPointerClicked();
        return;
      }
      super.mouseClicked(mouseX, mouseY, mouseButton);
      return;
    }

    if (game == null && mouseButton == 0) {
      int clicked = gameListRowAt(mouseY);
      if (clicked >= 0) {
        // First click highlights a title, a second on the same one starts it.
        if (clicked == selection) {
          startRun();
        } else {
          selection = clicked;
        }
        return;
      }
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  /**
   * Retrieves which game list row, if any, sits at the specified screen position.
   *
   * @param mouseY the mouse Y position
   *
   * @return the catalogue index, or -1 if the position is not over a row
   *
   * @since 1.0
   */
  private int gameListRowAt(int mouseY) {
    int rowHeight = 16;
    int listHeight = ArcadeCatalog.GAME_IDS.length * rowHeight;
    int headerY = viewY + Math.max(6, (viewHeight - listHeight) / 2 - 34);
    int listY = headerY + 30;
    int index = (mouseY - listY + 3) / rowHeight;
    return (index >= 0 && index < ArcadeCatalog.GAME_IDS.length) ? index : -1;
  }
}
