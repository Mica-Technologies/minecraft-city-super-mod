package com.micatechnologies.minecraft.csm.novelties;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>High Rise</b> — an original CSM cabinet game. A tower crane tracks a steel beam back and forth
 * above a construction site; the player releases it and tries to land it square on the floor below.
 *
 * <p>Whatever hangs over the edge shears off and falls to the street, so every sloppy drop makes the
 * next floor narrower. Miss the stack completely and the shift ends. Landing a beam dead centre pays
 * a bonus and welds back a little of the width lost earlier, which is the only way to keep building
 * past the first dozen floors.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameHighRise extends ArcadeGame {

  private static final float FIELD_WIDTH = 224F;
  private static final float FIELD_HEIGHT = 288F;

  /**
   * The height of one floor, in playfield units.
   */
  private static final float FLOOR_HEIGHT = 14F;
  /**
   * The width the first beam starts at.
   */
  private static final float STARTING_WIDTH = 84F;
  /**
   * Below this width a beam has nothing left to land on.
   */
  private static final float MINIMUM_WIDTH = 5F;
  /**
   * Overhang under this counts as a clean drop and welds a little width back on.
   */
  private static final float PERFECT_TOLERANCE = 2.5F;
  private static final float PERFECT_REWARD = 3F;

  /**
   * The Y position the crane's beam rides at.
   */
  private static final float CRANE_Y = 46F;
  /**
   * How fast a released beam falls, in units per step.
   */
  private static final float DROP_SPEED = 3.2F;
  /**
   * How fast the trimmed offcuts tumble away.
   */
  private static final float SCRAP_FALL_SPEED = 2.2F;
  /**
   * How high the stack is allowed to grow on screen before the view scrolls to follow it.
   */
  private static final float SCROLL_MARGIN = 150F;

  private static final int READY_STEPS = (int) (0.9F * TICK_RATE);

  /**
   * A placed floor of the tower.
   *
   * @since 1.0
   */
  private static final class Floor {

    private float centerX;
    private float width;
    /**
     * The floor's index from the ground, used to pick its colour band.
     */
    private int index;
  }

  /**
   * A trimmed-off piece tumbling to the street.
   *
   * @since 1.0
   */
  private static final class Scrap {

    private float x;
    private float y;
    private float width;
    private float velocityX;
    private float velocityY;
  }

  private final List<Floor> tower = new ArrayList<>();
  private final List<Scrap> scrap = new ArrayList<>();

  /**
   * The beam currently on the crane hook, or falling.
   */
  private float beamX;
  private float beamY;
  private float beamWidth;
  private float beamSpeed;
  private boolean beamFalling;

  /**
   * How far the camera has panned up to follow the tower.
   */
  private float cameraOffset;
  private int readyTimer;
  private int perfectStreak;
  private int lastDropPerfect;

  /**
   * Constructs a new High Rise game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameHighRise() {
    reset();
  }

  /**
   * Retrieves the game's display title.
   *
   * @return the game title
   *
   * @since 1.0
   */
  @Override
  public String getTitle() {
    return "HIGH RISE";
  }

  /**
   * Retrieves the game's control summary.
   *
   * @return the control summary
   *
   * @since 1.0
   */
  @Override
  public String getControls() {
    return "Space to release the beam  -  line it up with the floor below";
  }

  /**
   * Retrieves the playfield width.
   *
   * @return the playfield width
   *
   * @since 1.0
   */
  @Override
  public float getPlayfieldWidth() {
    return FIELD_WIDTH;
  }

  /**
   * Retrieves the playfield height.
   *
   * @return the playfield height
   *
   * @since 1.0
   */
  @Override
  public float getPlayfieldHeight() {
    return FIELD_HEIGHT;
  }

  /**
   * Retrieves the number of floors standing, which is what this cabinet shows in place of a wave
   * counter.
   *
   * @return the tower's height in floors
   *
   * @since 1.0
   */
  @Override
  public int getLevel() {
    return tower.size();
  }

  /**
   * Clears the site and lays the ground floor.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    tower.clear();
    scrap.clear();
    cameraOffset = 0F;
    perfectStreak = 0;
    lastDropPerfect = 0;

    Floor ground = new Floor();
    ground.centerX = FIELD_WIDTH * 0.5F;
    ground.width = STARTING_WIDTH;
    ground.index = 0;
    tower.add(ground);

    loadCrane();
  }

  /**
   * Hangs the next beam on the crane, matching the width of the floor below.
   *
   * @since 1.0
   */
  private void loadCrane() {
    Floor top = tower.get(tower.size() - 1);
    beamWidth = top.width;
    beamFalling = false;
    beamY = CRANE_Y;
    // The crane starts from alternating sides and speeds up as the tower grows.
    beamSpeed = (0.9F + tower.size() * 0.075F) * (tower.size() % 2 == 0 ? 1F : -1F);
    beamSpeed = clamp(beamSpeed, -3.4F, 3.4F);
    beamX = beamSpeed > 0F ? beamWidth * 0.5F : FIELD_WIDTH - beamWidth * 0.5F;
    readyTimer = READY_STEPS;
  }

  /**
   * Advances the simulation by one step.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  @Override
  protected void update(ArcadeInput input) {
    updateScrap();

    if (readyTimer > 0) {
      readyTimer--;
      return;
    }

    if (!beamFalling) {
      beamX += beamSpeed;
      // Bounce off the edges of the site rather than running off screen.
      if (beamX - beamWidth * 0.5F <= 0F) {
        beamX = beamWidth * 0.5F;
        beamSpeed = Math.abs(beamSpeed);
      } else if (beamX + beamWidth * 0.5F >= FIELD_WIDTH) {
        beamX = FIELD_WIDTH - beamWidth * 0.5F;
        beamSpeed = -Math.abs(beamSpeed);
      }
      if (input.pressed(ArcadeInput.Button.FIRE) || input.pressed(ArcadeInput.Button.DOWN)) {
        beamFalling = true;
      }
      return;
    }

    beamY += DROP_SPEED;
    float landingY = towerTopY();
    if (beamY >= landingY) {
      beamY = landingY;
      land();
    }
  }

  /**
   * Retrieves the screen Y coordinate the next beam lands at, in camera space.
   *
   * @return the landing height
   *
   * @since 1.0
   */
  private float towerTopY() {
    return FIELD_HEIGHT - 24F - tower.size() * FLOOR_HEIGHT + cameraOffset;
  }

  /**
   * Resolves a landed beam: trims the overhang, awards points, and either raises the crane for the
   * next floor or ends the run.
   *
   * @since 1.0
   */
  private void land() {
    Floor below = tower.get(tower.size() - 1);
    float overhang = beamX - below.centerX;
    float absoluteOverhang = Math.abs(overhang);

    if (absoluteOverhang >= (beamWidth + below.width) * 0.5F) {
      // Nothing of the beam is over the floor below; the whole thing goes to the street.
      addScrap(beamX, beamY, beamWidth, overhang > 0F ? 1F : -1F);
      if (!loseLife()) {
        loadCrane();
      }
      return;
    }

    float newWidth = Math.min(beamWidth, below.width) - Math.max(0F,
        absoluteOverhang - Math.abs(beamWidth - below.width) * 0.5F);
    newWidth = Math.min(newWidth, below.width);

    if (absoluteOverhang <= PERFECT_TOLERANCE) {
      // A clean drop: keep the full width, weld a little back on, and pay a streak bonus.
      newWidth = Math.min(STARTING_WIDTH, below.width + PERFECT_REWARD);
      perfectStreak++;
      lastDropPerfect = TICK_RATE;
      addScore(100 + 50 * Math.min(6, perfectStreak));
    } else {
      perfectStreak = 0;
      float trimmed = beamWidth - newWidth;
      if (trimmed > 0.5F) {
        float trimX = overhang > 0F
            ? beamX + beamWidth * 0.5F - trimmed * 0.5F
            : beamX - beamWidth * 0.5F + trimmed * 0.5F;
        addScrap(trimX, beamY, trimmed, overhang > 0F ? 1F : -1F);
      }
      addScore(50);
    }

    if (newWidth < MINIMUM_WIDTH) {
      // Too narrow to build on any further.
      gameOver = true;
      return;
    }

    Floor placed = new Floor();
    // The new floor is centred on whatever actually overlapped, not on where the beam was let go.
    placed.centerX = absoluteOverhang <= PERFECT_TOLERANCE ? below.centerX
        : beamX - Math.signum(overhang) * (beamWidth - newWidth) * 0.5F;
    placed.width = newWidth;
    placed.index = tower.size();
    tower.add(placed);

    // Pan the view up once the tower reaches the upper part of the screen.
    float topY = towerTopY();
    if (topY < SCROLL_MARGIN) {
      cameraOffset += SCROLL_MARGIN - topY;
    }

    loadCrane();
  }

  /**
   * Sends a trimmed piece tumbling to the street.
   *
   * @param x         the piece's centre X coordinate
   * @param y         the piece's Y coordinate
   * @param width     the piece's width
   * @param direction which way it topples: 1 right, -1 left
   *
   * @since 1.0
   */
  private void addScrap(float x, float y, float width, float direction) {
    Scrap piece = new Scrap();
    piece.x = x;
    piece.y = y;
    piece.width = width;
    piece.velocityX = direction * 1.1F;
    piece.velocityY = 0F;
    scrap.add(piece);
  }

  /**
   * Tumbles the offcuts and drops the ones that leave the screen.
   *
   * @since 1.0
   */
  private void updateScrap() {
    for (int i = 0; i < scrap.size(); i++) {
      Scrap piece = scrap.get(i);
      piece.velocityY += 0.16F;
      piece.x += piece.velocityX;
      piece.y += Math.min(SCRAP_FALL_SPEED * 2F, piece.velocityY);
      if (piece.y > FIELD_HEIGHT + 20F) {
        scrap.remove(i--);
      }
    }
    if (lastDropPerfect > 0) {
      lastDropPerfect--;
    }
  }

  /**
   * Draws the skyline, the tower, the crane and the beam.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    // Night sky graduating to city glow at street level.
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF0B1024);
    screen.rect(0F, FIELD_HEIGHT * 0.7F, FIELD_WIDTH, FIELD_HEIGHT * 0.3F, 0xFF141C34);
    for (int i = 0; i < 30; i++) {
      float x = (i * 53 % 219) + 3F;
      float y = (i * 71 % 160) + 6F;
      screen.rect(x, y, 1F, 1F, (i % 4 == 0) ? 0xFFFFFFFF : 0xFF6070A0);
    }

    // Street and the neighbouring blocks, drawn at the bottom of the shot.
    float streetY = FIELD_HEIGHT - 16F + cameraOffset;
    if (streetY < FIELD_HEIGHT + 40F) {
      screen.rect(0F, streetY, FIELD_WIDTH, FIELD_HEIGHT - streetY + 40F, 0xFF24282E);
      screen.rect(0F, streetY, FIELD_WIDTH, 1.5F, 0xFF4A5058);
    }

    for (Floor floor : tower) {
      float y = FIELD_HEIGHT - 24F - floor.index * FLOOR_HEIGHT + cameraOffset;
      if (y < -FLOOR_HEIGHT || y > FIELD_HEIGHT + FLOOR_HEIGHT) {
        continue;
      }
      drawBeam(screen, floor.centerX, y, floor.width, floorColor(floor.index));
    }

    for (Scrap piece : scrap) {
      drawBeam(screen, piece.x, piece.y, piece.width, 0xFF807060);
    }

    // The crane jib and its hoist cable.
    if (!beamFalling && readyTimer == 0) {
      screen.rect(0F, CRANE_Y - 10F, FIELD_WIDTH, 2.5F, 0xFF505A6A);
      screen.rect(beamX - 1F, CRANE_Y - 8F, 2F, 8F, 0xFF8A94A4);
    }
    drawBeam(screen, beamX, beamY, beamWidth, 0xFFFFC84A);

    if (lastDropPerfect > 0) {
      screen.textCentered("CLEAN DROP x" + perfectStreak, FIELD_WIDTH * 0.5F, beamY - 18F,
          0xFF60FFA0);
    }
    screen.text("FLOOR " + tower.size(), 5F, 5F, 0xFF8090B0);
  }

  /**
   * Retrieves the colour band a floor is drawn in, cycling every few storeys so the tower reads as
   * having sections.
   *
   * @param index the floor's index from the ground
   *
   * @return the ARGB colour
   *
   * @since 1.0
   */
  private static int floorColor(int index) {
    switch ((index / 4) % 4) {
      case 0:
        return 0xFF4A90D0;
      case 1:
        return 0xFF50B080;
      case 2:
        return 0xFFC08050;
      default:
        return 0xFF9070C0;
    }
  }

  /**
   * Draws one beam or floor slab, with a highlight along its top edge.
   *
   * @param screen the screen to draw on
   * @param centerX the beam's centre X coordinate
   * @param y      the beam's top Y coordinate
   * @param width  the beam's width
   * @param color  the beam's colour
   *
   * @since 1.0
   */
  private void drawBeam(ArcadeScreen screen, float centerX, float y, float width, int color) {
    float left = centerX - width * 0.5F;
    screen.rect(left, y, width, FLOOR_HEIGHT - 2F, color);
    screen.rect(left, y, width, 1.5F, 0x60FFFFFF);
    // Window slots, so a wide floor does not read as a plain bar.
    int windows = Math.max(1, (int) (width / 9F));
    for (int i = 0; i < windows; i++) {
      float windowX = left + 3F + i * (width - 6F) / Math.max(1, windows);
      if (windowX + 3F < left + width) {
        screen.rect(windowX, y + 4F, 3F, FLOOR_HEIGHT - 9F, 0x50000000);
      }
    }
  }
}
