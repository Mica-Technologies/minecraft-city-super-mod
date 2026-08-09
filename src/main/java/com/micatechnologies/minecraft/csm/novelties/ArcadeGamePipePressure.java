package com.micatechnologies.minecraft.csm.novelties;

/**
 * <b>Pipe Pressure</b> — an original CSM cabinet game. A water main has to be routed across a grid of
 * loose pipe fittings before the pressure in the header climbs into the red.
 *
 * <p>Every tile is a fitting that can be turned a quarter turn at a time; the player walks a cursor
 * over the grid and rotates fittings until an unbroken run of pipe connects the inlet on one side to
 * the outlet on the other. Water fills the connected run live as it is built, so a partial route is
 * visible feedback rather than guesswork. Completing a run vents the header, banks the remaining
 * pressure as points, and starts a harder board.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGamePipePressure extends ArcadeGame {

  private static final int GRID_SIZE = 7;
  private static final float CELL = 26F;
  private static final float MARGIN = 14F;
  private static final float FIELD_WIDTH = GRID_SIZE * CELL + MARGIN * 2F;
  private static final float FIELD_HEIGHT = GRID_SIZE * CELL + MARGIN * 2F + 22F;

  /**
   * Connection bits for each of the four sides of a fitting.
   */
  private static final int NORTH = 1;
  private static final int EAST = 2;
  private static final int SOUTH = 4;
  private static final int WEST = 8;

  /**
   * The fitting shapes available, as connection masks in their unrotated orientation: a straight, an
   * elbow, a tee, and a blank.
   */
  private static final int[] FITTING_SHAPES = {NORTH | SOUTH, NORTH | EAST, NORTH | EAST | SOUTH,
      0};

  /**
   * How long the header takes to redline at level one, in simulation steps.
   */
  private static final int BASE_PRESSURE_STEPS = 55 * TICK_RATE;
  private static final int BOARD_INTRO_STEPS = (int) (1.1F * TICK_RATE);
  private static final int ROTATE_COOLDOWN = 6;

  /**
   * Each tile's connection mask.
   */
  private final int[][] fittings = new int[GRID_SIZE][GRID_SIZE];

  /**
   * Whether each tile currently has water in it.
   */
  private final boolean[][] flooded = new boolean[GRID_SIZE][GRID_SIZE];

  private int cursorColumn;
  private int cursorRow;
  private int moveCooldown;
  private int rotateCooldown;
  /**
   * The grid row the inlet feeds into, on the left edge.
   */
  private int inletRow;
  /**
   * The grid row the outlet drains from, on the right edge.
   */
  private int outletRow;
  private int pressure;
  private int pressureLimit;
  private int boardIntroTimer;
  private int completionFlash;

  /**
   * Constructs a new Pipe Pressure game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGamePipePressure() {
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
    return "PIPE PRESSURE";
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
    return "Arrows to move the cursor  -  Space to turn a fitting";
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
   * Lays out the first board.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    newBoard();
  }

  /**
   * Scrambles a fresh board and resets the header.
   *
   * @since 1.0
   */
  private void newBoard() {
    inletRow = random.nextInt(GRID_SIZE);
    outletRow = random.nextInt(GRID_SIZE);
    // A blank is only worth including once the player has room to route around it.
    int shapeCount = level >= 3 ? FITTING_SHAPES.length : FITTING_SHAPES.length - 1;
    for (int row = 0; row < GRID_SIZE; row++) {
      for (int column = 0; column < GRID_SIZE; column++) {
        int shape = FITTING_SHAPES[random.nextInt(shapeCount)];
        fittings[row][column] = rotate(shape, random.nextInt(4));
      }
    }
    pressureLimit = Math.max(18 * TICK_RATE, BASE_PRESSURE_STEPS - (level - 1) * 5 * TICK_RATE);
    pressure = 0;
    cursorColumn = 0;
    cursorRow = inletRow;
    moveCooldown = 0;
    rotateCooldown = 0;
    boardIntroTimer = BOARD_INTRO_STEPS;
    completionFlash = 0;
    floodFill();
  }

  /**
   * Turns a connection mask clockwise the specified number of quarter turns.
   *
   * @param mask  the connection mask
   * @param turns the number of quarter turns
   *
   * @return the rotated mask
   *
   * @since 1.0
   */
  private static int rotate(int mask, int turns) {
    int result = mask;
    for (int i = 0; i < turns; i++) {
      // Shift every bit one side clockwise, wrapping west back round to north.
      result = ((result << 1) & 0x0F) | ((result & WEST) != 0 ? NORTH : 0);
    }
    return result;
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
    if (completionFlash > 0) {
      completionFlash--;
      if (completionFlash == 0) {
        level++;
        newBoard();
      }
      return;
    }

    if (boardIntroTimer > 0) {
      boardIntroTimer--;
      return;
    }

    moveCursor(input);

    if (rotateCooldown > 0) {
      rotateCooldown--;
    }
    if (input.held(ArcadeInput.Button.FIRE) && rotateCooldown == 0) {
      fittings[cursorRow][cursorColumn] = rotate(fittings[cursorRow][cursorColumn], 1);
      rotateCooldown = ROTATE_COOLDOWN;
      floodFill();
      if (isConnected()) {
        completeBoard();
        return;
      }
    }

    pressure++;
    if (pressure >= pressureLimit) {
      // The header blew. That costs a crew, and the board is rebuilt from scratch.
      if (!loseLife()) {
        newBoard();
      }
    }
  }

  /**
   * Moves the cursor around the grid, with a repeat delay so a held key steps rather than sprints.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void moveCursor(ArcadeInput input) {
    if (moveCooldown > 0) {
      moveCooldown--;
      return;
    }
    int deltaColumn = 0;
    int deltaRow = 0;
    if (input.held(ArcadeInput.Button.LEFT)) {
      deltaColumn = -1;
    } else if (input.held(ArcadeInput.Button.RIGHT)) {
      deltaColumn = 1;
    } else if (input.held(ArcadeInput.Button.UP)) {
      deltaRow = -1;
    } else if (input.held(ArcadeInput.Button.DOWN)) {
      deltaRow = 1;
    }
    if (deltaColumn == 0 && deltaRow == 0) {
      return;
    }
    cursorColumn = (int) clamp(cursorColumn + deltaColumn, 0, GRID_SIZE - 1);
    cursorRow = (int) clamp(cursorRow + deltaRow, 0, GRID_SIZE - 1);
    moveCooldown = 8;
  }

  /**
   * Recomputes which tiles have water in them by walking outward from the inlet through matching
   * connections.
   *
   * @since 1.0
   */
  private void floodFill() {
    for (int row = 0; row < GRID_SIZE; row++) {
      java.util.Arrays.fill(flooded[row], false);
    }
    // The inlet only feeds the first tile if that tile actually has a west-facing connection.
    if ((fittings[inletRow][0] & WEST) == 0) {
      return;
    }

    int[] stack = new int[GRID_SIZE * GRID_SIZE];
    int top = 0;
    flooded[inletRow][0] = true;
    stack[top++] = inletRow * GRID_SIZE;

    while (top > 0) {
      int packed = stack[--top];
      int row = packed / GRID_SIZE;
      int column = packed % GRID_SIZE;
      int mask = fittings[row][column];

      if ((mask & NORTH) != 0) {
        top = visit(stack, top, row - 1, column, SOUTH);
      }
      if ((mask & SOUTH) != 0) {
        top = visit(stack, top, row + 1, column, NORTH);
      }
      if ((mask & WEST) != 0) {
        top = visit(stack, top, row, column - 1, EAST);
      }
      if ((mask & EAST) != 0) {
        top = visit(stack, top, row, column + 1, WEST);
      }
    }
  }

  /**
   * Floods a neighbouring tile if it exists and faces back the way the water is coming from.
   *
   * @param stack        the flood-fill stack
   * @param top          the current stack depth
   * @param row          the neighbour's row
   * @param column       the neighbour's column
   * @param requiredSide the connection the neighbour must have facing the current tile
   *
   * @return the new stack depth
   *
   * @since 1.0
   */
  private int visit(int[] stack, int top, int row, int column, int requiredSide) {
    if (row < 0 || row >= GRID_SIZE || column < 0 || column >= GRID_SIZE) {
      return top;
    }
    if (flooded[row][column] || (fittings[row][column] & requiredSide) == 0) {
      return top;
    }
    flooded[row][column] = true;
    stack[top++] = row * GRID_SIZE + column;
    return top;
  }

  /**
   * Retrieves whether the water has reached the outlet.
   *
   * @return {@code true} if the run is complete
   *
   * @since 1.0
   */
  private boolean isConnected() {
    return flooded[outletRow][GRID_SIZE - 1]
        && (fittings[outletRow][GRID_SIZE - 1] & EAST) != 0;
  }

  /**
   * Banks the board and starts the celebration flash.
   *
   * @since 1.0
   */
  private void completeBoard() {
    // Whatever headroom is left in the header pays out, so a fast route is worth far more.
    int headroom = pressureLimit - pressure;
    addScore(400 + headroom / TICK_RATE * 25);
    completionFlash = (int) (1.2F * TICK_RATE);
  }

  /**
   * Draws the grid, the fittings, the water and the pressure gauge.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF10161C);

    for (int row = 0; row < GRID_SIZE; row++) {
      for (int column = 0; column < GRID_SIZE; column++) {
        float x = MARGIN + column * CELL;
        float y = MARGIN + row * CELL;
        screen.rect(x + 1F, y + 1F, CELL - 2F, CELL - 2F, 0xFF1C242E);
        drawFitting(screen, x, y, fittings[row][column], flooded[row][column]);
      }
    }

    // Inlet and outlet stubs, poking out of the frame.
    float inletY = MARGIN + inletRow * CELL + CELL * 0.5F;
    screen.rect(MARGIN - 10F, inletY - 3F, 10F, 6F, 0xFF40C0FF);
    screen.textCentered("IN", MARGIN - 5F, inletY + 5F, 0xFF40C0FF);
    float outletY = MARGIN + outletRow * CELL + CELL * 0.5F;
    int outletColor = isConnected() ? 0xFF60FF80 : 0xFF806040;
    screen.rect(MARGIN + GRID_SIZE * CELL, outletY - 3F, 10F, 6F, outletColor);
    screen.textCentered("OUT", MARGIN + GRID_SIZE * CELL + 5F, outletY + 5F, outletColor);

    // Cursor.
    float cursorX = MARGIN + cursorColumn * CELL;
    float cursorY = MARGIN + cursorRow * CELL;
    int cursorColor = (ticks / 8) % 2 == 0 ? 0xFFFFE040 : 0xFFB09020;
    screen.rect(cursorX, cursorY, CELL, 1.5F, cursorColor);
    screen.rect(cursorX, cursorY + CELL - 1.5F, CELL, 1.5F, cursorColor);
    screen.rect(cursorX, cursorY, 1.5F, CELL, cursorColor);
    screen.rect(cursorX + CELL - 1.5F, cursorY, 1.5F, CELL, cursorColor);

    // Pressure gauge along the bottom.
    float gaugeY = FIELD_HEIGHT - 16F;
    float fraction = clamp(pressure / (float) pressureLimit, 0F, 1F);
    screen.rect(MARGIN, gaugeY, GRID_SIZE * CELL, 8F, 0xFF202830);
    int gaugeColor = fraction > 0.8F ? 0xFFFF4040 : (fraction > 0.55F ? 0xFFFFC040 : 0xFF40C0FF);
    screen.rect(MARGIN, gaugeY, GRID_SIZE * CELL * fraction, 8F, gaugeColor);
    screen.text("HEADER PRESSURE", MARGIN, gaugeY - 10F, 0xFF7088A0);

    if (boardIntroTimer > 0) {
      screen.textCentered("SECTION " + level, FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.45F,
          0xFF40C0FF);
    } else if (completionFlash > 0 && (completionFlash / 5) % 2 == 0) {
      screen.textCentered("MAIN CHARGED", FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.45F, 0xFF60FF80);
    }
  }

  /**
   * Draws one fitting as stubs running from the centre of its tile out to each connected side.
   *
   * @param screen  the screen to draw on
   * @param x       the tile's left edge
   * @param y       the tile's top edge
   * @param mask    the fitting's connection mask
   * @param hasWater whether the fitting is flooded
   *
   * @since 1.0
   */
  private void drawFitting(ArcadeScreen screen, float x, float y, int mask, boolean hasWater) {
    if (mask == 0) {
      // A blank: a capped stub that nothing can route through.
      screen.rect(x + CELL * 0.4F, y + CELL * 0.4F, CELL * 0.2F, CELL * 0.2F, 0xFF404850);
      return;
    }

    int pipeColor = hasWater ? 0xFF40C0FF : 0xFF808C98;
    float centerX = x + CELL * 0.5F;
    float centerY = y + CELL * 0.5F;
    float thickness = CELL * 0.26F;
    float half = thickness * 0.5F;

    if ((mask & NORTH) != 0) {
      screen.rect(centerX - half, y, thickness, CELL * 0.5F + half, pipeColor);
    }
    if ((mask & SOUTH) != 0) {
      screen.rect(centerX - half, centerY - half, thickness, CELL * 0.5F + half, pipeColor);
    }
    if ((mask & WEST) != 0) {
      screen.rect(x, centerY - half, CELL * 0.5F + half, thickness, pipeColor);
    }
    if ((mask & EAST) != 0) {
      screen.rect(centerX - half, centerY - half, CELL * 0.5F + half, thickness, pipeColor);
    }

    // A collar at the junction, brighter when water is moving through it.
    screen.rect(centerX - half * 0.7F, centerY - half * 0.7F, thickness * 0.7F, thickness * 0.7F,
        hasWater ? 0xFFA0E8FF : 0xFF5A646E);
  }
}
