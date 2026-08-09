package com.micatechnologies.minecraft.csm.novelties;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Site Survey</b> — an original CSM cabinet game. The player drives a survey van around a
 * wireframe city block, hunting down survey markers against the clock. There is no combat of any
 * kind: the whole game is navigation under time pressure.
 *
 * <p>Only one marker is live at a time and only its bearing is given, never its distance, so the
 * player has to read the street grid and commit to a route. Reaching a marker banks it and adds time
 * to the clock; the next one is placed further away than the last. Clipping a building costs speed
 * and a couple of seconds, which is punishment enough when the clock is the only opponent.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameSiteSurvey extends ArcadeGame {

  private static final float FIELD_WIDTH = 320F;
  private static final float FIELD_HEIGHT = 240F;
  private static final float HORIZON_Y = 112F;
  private static final float FOCAL_LENGTH = 145F;
  private static final float NEAR_PLANE = 3F;

  /**
   * The side length of the wrapping survey area.
   */
  private static final float WORLD_SIZE = 420F;
  /**
   * How far apart the street centrelines are.
   */
  private static final float BLOCK_PITCH = 70F;
  /**
   * How much of each block is building rather than street.
   */
  private static final float BUILDING_HALF = 22F;

  private static final float TURN_RATE = 0.03F;
  private static final float ACCELERATION = 0.05F;
  private static final float BRAKE = 0.09F;
  private static final float MAX_SPEED = 1.5F;
  private static final float ROLLING_DRAG = 0.985F;

  private static final float MARKER_RADIUS = 11F;
  private static final int STARTING_SECONDS = 45;
  private static final int SECONDS_PER_MARKER = 18;
  private static final int BUMP_PENALTY_STEPS = 2 * TICK_RATE;

  private static final int WIRE_COLOR = 0xFF50C8FF;
  private static final int BUILDING_COLOR = 0xFF2E7CA8;
  private static final int MARKER_COLOR = 0xFFFFD040;

  /**
   * One city block's tower.
   *
   * @since 1.0
   */
  private static final class Tower {

    private float x;
    private float z;
    private float height;
  }

  private final List<Tower> towers = new ArrayList<>();
  private float vanX;
  private float vanZ;
  private float vanHeading;
  private float vanSpeed;
  private float markerX;
  private float markerZ;
  private int clockSteps;
  private int markersFound;
  private int bumpFlash;
  private int arrivalFlash;

  /**
   * Constructs a new Site Survey game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameSiteSurvey() {
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
    return "SITE SURVEY";
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
    return "Left/Right steer  -  Up drive  -  Down brake  -  follow the bearing";
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
   * Retrieves the number of markers banked, shown in place of a wave counter.
   *
   * @return the markers found this run
   *
   * @since 1.0
   */
  @Override
  public int getLevel() {
    return markersFound + 1;
  }

  /**
   * Retrieves the seconds left on the clock, shown in place of a life count.
   *
   * @return the remaining seconds
   *
   * @since 1.0
   */
  @Override
  public int getLives() {
    return Math.max(0, clockSteps / TICK_RATE);
  }

  /**
   * Lays out the survey area and places the first marker.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    towers.clear();
    int blocks = (int) (WORLD_SIZE / BLOCK_PITCH);
    for (int gridX = 0; gridX < blocks; gridX++) {
      for (int gridZ = 0; gridZ < blocks; gridZ++) {
        Tower tower = new Tower();
        tower.x = gridX * BLOCK_PITCH + BLOCK_PITCH * 0.5F;
        tower.z = gridZ * BLOCK_PITCH + BLOCK_PITCH * 0.5F;
        tower.height = 18F + random.nextFloat() * 42F;
        towers.add(tower);
      }
    }

    // Start the van on a street centreline so it is not inside a block.
    vanX = BLOCK_PITCH;
    vanZ = BLOCK_PITCH;
    vanHeading = 0F;
    vanSpeed = 0F;
    markersFound = 0;
    bumpFlash = 0;
    arrivalFlash = 0;
    clockSteps = STARTING_SECONDS * TICK_RATE;
    placeMarker();
  }

  /**
   * Places the next survey marker on a street intersection, further out each time.
   *
   * @since 1.0
   */
  private void placeMarker() {
    // Markers always land on an intersection, so they are always reachable by road.
    int blocks = (int) (WORLD_SIZE / BLOCK_PITCH);
    float minimumDistance = 80F + Math.min(140F, markersFound * 18F);
    for (int attempt = 0; attempt < 60; attempt++) {
      float x = random.nextInt(blocks) * BLOCK_PITCH;
      float z = random.nextInt(blocks) * BLOCK_PITCH;
      float deltaX = shortestDelta(x - vanX);
      float deltaZ = shortestDelta(z - vanZ);
      if (deltaX * deltaX + deltaZ * deltaZ >= minimumDistance * minimumDistance) {
        markerX = x;
        markerZ = z;
        return;
      }
    }
    // Fallback if the area is too small for the requested spacing.
    markerX = wrap(vanX + WORLD_SIZE * 0.5F, WORLD_SIZE);
    markerZ = wrap(vanZ + WORLD_SIZE * 0.4F, WORLD_SIZE);
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
    if (bumpFlash > 0) {
      bumpFlash--;
    }
    if (arrivalFlash > 0) {
      arrivalFlash--;
    }

    if (--clockSteps <= 0) {
      clockSteps = 0;
      gameOver = true;
      return;
    }

    drive(input);
    checkMarker();
  }

  /**
   * Steers and drives the van, bouncing it off any building it clips.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void drive(ArcadeInput input) {
    if (input.held(ArcadeInput.Button.LEFT)) {
      vanHeading -= TURN_RATE;
    }
    if (input.held(ArcadeInput.Button.RIGHT)) {
      vanHeading += TURN_RATE;
    }

    if (input.held(ArcadeInput.Button.UP) || input.held(ArcadeInput.Button.FIRE)) {
      vanSpeed = Math.min(MAX_SPEED, vanSpeed + ACCELERATION);
    } else if (input.held(ArcadeInput.Button.DOWN)) {
      vanSpeed = Math.max(-MAX_SPEED * 0.45F, vanSpeed - BRAKE);
    } else {
      vanSpeed *= ROLLING_DRAG;
    }

    float nextX = wrap(vanX + (float) Math.sin(vanHeading) * vanSpeed, WORLD_SIZE);
    float nextZ = wrap(vanZ + (float) Math.cos(vanHeading) * vanSpeed, WORLD_SIZE);

    if (insideBuilding(nextX, nextZ)) {
      // Clipping a wall stops the van dead and costs two seconds off the clock.
      if (bumpFlash == 0) {
        clockSteps = Math.max(1, clockSteps - BUMP_PENALTY_STEPS);
      }
      bumpFlash = 12;
      vanSpeed = 0F;
      return;
    }

    vanX = nextX;
    vanZ = nextZ;
  }

  /**
   * Retrieves whether the specified position is inside a city block's footprint.
   *
   * @param x the X position
   * @param z the Z position
   *
   * @return {@code true} if a building occupies the position
   *
   * @since 1.0
   */
  private static boolean insideBuilding(float x, float z) {
    float offsetX = Math.abs(((x % BLOCK_PITCH) + BLOCK_PITCH) % BLOCK_PITCH - BLOCK_PITCH * 0.5F);
    float offsetZ = Math.abs(((z % BLOCK_PITCH) + BLOCK_PITCH) % BLOCK_PITCH - BLOCK_PITCH * 0.5F);
    return offsetX < BUILDING_HALF && offsetZ < BUILDING_HALF;
  }

  /**
   * Banks the marker if the van has reached it, and puts out the next one.
   *
   * @since 1.0
   */
  private void checkMarker() {
    float deltaX = shortestDelta(markerX - vanX);
    float deltaZ = shortestDelta(markerZ - vanZ);
    if (deltaX * deltaX + deltaZ * deltaZ > MARKER_RADIUS * MARKER_RADIUS) {
      return;
    }
    markersFound++;
    arrivalFlash = TICK_RATE;
    // Later markers are worth more, and each one buys back some clock.
    addScore(250 + markersFound * 75);
    clockSteps += Math.max(9, SECONDS_PER_MARKER - markersFound) * TICK_RATE;
    placeMarker();
  }

  /**
   * Reduces a world-space difference to the shortest way around the wrapping area.
   *
   * @param delta the raw difference
   *
   * @return the equivalent difference in the range {@code [-WORLD_SIZE/2, WORLD_SIZE/2]}
   *
   * @since 1.0
   */
  private static float shortestDelta(float delta) {
    if (delta > WORLD_SIZE * 0.5F) {
      return delta - WORLD_SIZE;
    }
    if (delta < -WORLD_SIZE * 0.5F) {
      return delta + WORLD_SIZE;
    }
    return delta;
  }

  /**
   * Projects a world-relative point into screen space.
   *
   * @param deltaX the point's X offset from the van
   * @param deltaZ the point's Z offset from the van
   * @param height the point's height above the road
   *
   * @return the screen coordinates, or {@code null} if the point is behind the near plane
   *
   * @since 1.0
   */
  private float[] project(float deltaX, float deltaZ, float height) {
    float sin = (float) Math.sin(vanHeading);
    float cos = (float) Math.cos(vanHeading);
    float forward = deltaX * sin + deltaZ * cos;
    if (forward < NEAR_PLANE) {
      return null;
    }
    float right = deltaX * cos - deltaZ * sin;
    return new float[] {
        FIELD_WIDTH * 0.5F + right / forward * FOCAL_LENGTH,
        HORIZON_Y - (height - 2.5F) / forward * FOCAL_LENGTH
    };
  }

  /**
   * Draws one line between two world-relative points, skipping it if either end is behind the
   * camera or both ends are far off screen.
   *
   * @param screen the screen to draw on
   * @param x1     the first point's X offset
   * @param z1     the first point's Z offset
   * @param h1     the first point's height
   * @param x2     the second point's X offset
   * @param z2     the second point's Z offset
   * @param h2     the second point's height
   * @param color  the ARGB colour
   *
   * @since 1.0
   */
  private void drawWorldLine(ArcadeScreen screen, float x1, float z1, float h1, float x2, float z2,
      float h2, int color) {
    float[] a = project(x1, z1, h1);
    float[] b = project(x2, z2, h2);
    if (a == null || b == null) {
      return;
    }
    if ((a[0] < -FIELD_WIDTH && b[0] < -FIELD_WIDTH)
        || (a[0] > FIELD_WIDTH * 2F && b[0] > FIELD_WIDTH * 2F)) {
      return;
    }
    screen.line(a[0], a[1], b[0], b[1], color);
  }

  /**
   * Draws the survey area and the van's instruments.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF03080E);
    screen.rect(0F, HORIZON_Y, FIELD_WIDTH, FIELD_HEIGHT - HORIZON_Y, 0xFF07121C);
    screen.line(0F, HORIZON_Y, FIELD_WIDTH, HORIZON_Y, 0xFF1E5070);

    for (Tower tower : towers) {
      drawTower(screen, tower);
    }
    drawMarker(screen);
    drawInstruments(screen);

    if (bumpFlash > 0) {
      screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0x28FF4040);
    }
    if (arrivalFlash > 0) {
      screen.textCentered("MARKER LOGGED  +" + Math.max(9, SECONDS_PER_MARKER - markersFound)
          + "s", FIELD_WIDTH * 0.5F, HORIZON_Y - 34F, 0xFF60FFA0);
    }
  }

  /**
   * Draws one city block as a wireframe box.
   *
   * @param screen the screen to draw on
   * @param tower  the block to draw
   *
   * @since 1.0
   */
  private void drawTower(ArcadeScreen screen, Tower tower) {
    float deltaX = shortestDelta(tower.x - vanX);
    float deltaZ = shortestDelta(tower.z - vanZ);
    // Skip anything well outside the draw distance; the grid wraps so there is always more.
    if (deltaX * deltaX + deltaZ * deltaZ > 200F * 200F) {
      return;
    }

    float[] cornerX = {deltaX - BUILDING_HALF, deltaX + BUILDING_HALF, deltaX + BUILDING_HALF,
        deltaX - BUILDING_HALF};
    float[] cornerZ = {deltaZ - BUILDING_HALF, deltaZ - BUILDING_HALF, deltaZ + BUILDING_HALF,
        deltaZ + BUILDING_HALF};

    for (int i = 0; i < 4; i++) {
      int next = (i + 1) % 4;
      drawWorldLine(screen, cornerX[i], cornerZ[i], 0F, cornerX[next], cornerZ[next], 0F,
          BUILDING_COLOR);
      drawWorldLine(screen, cornerX[i], cornerZ[i], tower.height, cornerX[next], cornerZ[next],
          tower.height, WIRE_COLOR);
      drawWorldLine(screen, cornerX[i], cornerZ[i], 0F, cornerX[i], cornerZ[i], tower.height,
          BUILDING_COLOR);
    }
    // A floor line partway up, so height reads at a glance.
    for (int i = 0; i < 4; i++) {
      int next = (i + 1) % 4;
      drawWorldLine(screen, cornerX[i], cornerZ[i], tower.height * 0.5F, cornerX[next],
          cornerZ[next], tower.height * 0.5F, 0xFF1E5070);
    }
  }

  /**
   * Draws the live survey marker as a beacon standing in the street.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawMarker(ArcadeScreen screen) {
    float deltaX = shortestDelta(markerX - vanX);
    float deltaZ = shortestDelta(markerZ - vanZ);

    float[] base = project(deltaX, deltaZ, 0F);
    float[] top = project(deltaX, deltaZ, 26F);
    if (base == null || top == null) {
      return;
    }
    int color = (ticks / 8) % 2 == 0 ? MARKER_COLOR : 0xFF806820;
    screen.line(base[0], base[1], top[0], top[1], color);
    // A ring of guy lines, so the beacon reads as a tripod rather than a stray line.
    for (int i = 0; i < 4; i++) {
      double angle = Math.PI * 0.5 * i;
      float[] foot = project(deltaX + (float) Math.cos(angle) * 7F,
          deltaZ + (float) Math.sin(angle) * 7F, 0F);
      if (foot != null) {
        screen.line(top[0], top[1], foot[0], foot[1], color);
      }
    }
  }

  /**
   * Draws the van's dashboard: the clock, the bearing needle and the marker count.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawInstruments(ArcadeScreen screen) {
    // Clock bar. It drains continuously and is topped up at each marker.
    int seconds = clockSteps / TICK_RATE;
    float fraction = clamp(clockSteps / (float) (STARTING_SECONDS * TICK_RATE), 0F, 1F);
    int clockColor = seconds <= 8 ? 0xFFFF4040 : (seconds <= 20 ? 0xFFFFC040 : 0xFF40D0FF);
    screen.rect(8F, 8F, FIELD_WIDTH - 16F, 5F, 0xFF10202C);
    screen.rect(8F, 8F, (FIELD_WIDTH - 16F) * fraction, 5F, clockColor);
    screen.text("CLOCK " + seconds + "s", 8F, 15F, clockColor);
    String found = "MARKERS " + markersFound;
    screen.text(found, FIELD_WIDTH - 8F - found.length() * 6F, 15F, 0xFF60748A);

    // Bearing dial: a needle pointing at the live marker, relative to the van's heading.
    float dialX = FIELD_WIDTH * 0.5F;
    float dialY = FIELD_HEIGHT - 34F;
    float dialRadius = 22F;
    screen.circle(dialX, dialY, dialRadius, 24, 0xFF1E5070);
    for (int i = 0; i < 4; i++) {
      double angle = Math.PI * 0.5 * i;
      screen.line(dialX + (float) Math.sin(angle) * dialRadius * 0.82F,
          dialY - (float) Math.cos(angle) * dialRadius * 0.82F,
          dialX + (float) Math.sin(angle) * dialRadius,
          dialY - (float) Math.cos(angle) * dialRadius, 0xFF1E5070);
    }
    float bearing = (float) Math.atan2(shortestDelta(markerX - vanX),
        shortestDelta(markerZ - vanZ)) - vanHeading;
    screen.line(dialX, dialY, dialX + (float) Math.sin(bearing) * dialRadius * 0.85F,
        dialY - (float) Math.cos(bearing) * dialRadius * 0.85F, MARKER_COLOR);
    screen.textCentered("BEARING", dialX, dialY + dialRadius + 2F, 0xFF3E6880);

    // Speed readout on the left of the dash.
    float speedFraction = Math.abs(vanSpeed) / MAX_SPEED;
    screen.rect(14F, FIELD_HEIGHT - 44F, 8F, 34F, 0xFF10202C);
    screen.rect(14F, FIELD_HEIGHT - 10F - 34F * speedFraction, 8F, 34F * speedFraction,
        0xFF40D0FF);
    screen.textCentered("SPD", 18F, FIELD_HEIGHT - 8F, 0xFF3E6880);
  }
}
