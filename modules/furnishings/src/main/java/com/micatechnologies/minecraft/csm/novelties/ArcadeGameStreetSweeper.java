package com.micatechnologies.minecraft.csm.novelties;

/**
 * <b>Street Sweeper</b> — an original CSM cabinet game. The player drives a municipal sweeper around
 * a city block grid clearing litter from every street tile while live traffic keeps circulating.
 *
 * <p>The traffic does not hunt the player: cars obey the road, preferring to carry straight on and
 * only turning where the street forces them to, which makes them a moving hazard to read and time
 * rather than a pursuer to outrun. The pickup scattered around the map is a <em>signal preempt</em>
 * — grabbing one throws every light red and freezes traffic for a few seconds, opening a window to
 * clear the awkward corners. Frozen cars are simply harmless; there is no bonus for touching them.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameStreetSweeper extends ArcadeGame {

  /**
   * The city block layout. {@code #} is a building, {@code .} is street, {@code o} is a signal
   * preempt pickup, and the open ends of row 11 form a through route across the map edge.
   *
   * @since 1.0
   */
  private static final String[] CITY = {
      "#####################",
      "#........#.#........#",
      "#o##.###.#.#.###.##o#",
      "#...................#",
      "#.##.#.#######.#.##.#",
      "#....#...#.#...#....#",
      "####.###.#.#.###.####",
      "#......#.....#......#",
      "#.####.#.###.#.####.#",
      "#.#......###......#.#",
      "#.#.####.###.####.#.#",
      "....#....###....#....",
      "#.#.#.##.###.##.#.#.#",
      "#.#......###......#.#",
      "#.#.####.###.####.#.#",
      "#o..#....###....#..o#",
      "##.##.###.#.###.##.##",
      "#.....#.......#.....#",
      "#.###.#.#####.#.###.#",
      "#...................#",
      "#####################"
  };

  private static final int COLUMNS = 21;
  private static final int ROWS = 21;
  private static final float CELL = 8F;

  private static final float SWEEPER_SPEED = 0.075F;
  private static final float TRAFFIC_SPEED = 0.062F;
  private static final int PREEMPT_STEPS = 5 * TICK_RATE;
  private static final int WRECK_STEPS = (int) (1.6F * TICK_RATE);
  private static final int READY_STEPS = (int) (1.4F * TICK_RATE);
  private static final int LITTER_POINTS = 10;
  private static final int PREEMPT_POINTS = 50;

  /**
   * Direction deltas along the X axis, indexed by direction: east, south, west, north.
   */
  private static final int[] DIRECTION_X = {1, 0, -1, 0};

  /**
   * Direction deltas along the Y axis, indexed by direction: east, south, west, north.
   */
  private static final int[] DIRECTION_Y = {0, 1, 0, -1};

  /**
   * Paint colours for the traffic, cycled by index.
   */
  private static final int[] CAR_COLORS = {0xFFE04040, 0xFF4090E0, 0xFFE0C040, 0xFF60C060,
      0xFFE07030, 0xFFB060E0};

  /**
   * The player's sweeper and each vehicle share this movement state.
   *
   * @since 1.0
   */
  private static final class Vehicle {

    private float x;
    private float y;
    private int direction;
    /**
     * The direction the driver wants next; taken at the following intersection if the street
     * allows it.
     */
    private int desiredDirection;
  }

  /**
   * The litter and pickups still on the streets, mirroring {@link #CITY} as it is swept.
   */
  private final char[][] litter = new char[ROWS][COLUMNS];

  private final Vehicle sweeper = new Vehicle();
  private Vehicle[] traffic = new Vehicle[0];
  private int litterRemaining;
  private int preemptTimer;
  private int wreckTimer;
  private int readyTimer;

  /**
   * Constructs a new Street Sweeper game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameStreetSweeper() {
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
    return "STREET SWEEPER";
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
    return "Arrows / WASD to drive  -  clear every street, mind the traffic";
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
    return COLUMNS * CELL;
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
    return ROWS * CELL;
  }

  /**
   * Litters every street again and starts a fresh shift.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    loadShift();
  }

  /**
   * Refills the litter and puts the traffic back on the road for the current level.
   *
   * @since 1.0
   */
  private void loadShift() {
    litterRemaining = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int column = 0; column < COLUMNS; column++) {
        char cell = CITY[row].charAt(column);
        litter[row][column] = (cell == '.' || cell == 'o') ? cell : ' ';
        if (cell == '.' || cell == 'o') {
          litterRemaining++;
        }
      }
    }

    // One more vehicle joins the shift each level, up to a full six.
    int cars = Math.min(CAR_COLORS.length, 3 + level);
    traffic = new Vehicle[cars];
    int[][] depots = {{3, 9}, {17, 9}, {3, 13}, {17, 13}, {10, 7}, {10, 17}};
    for (int i = 0; i < cars; i++) {
      Vehicle car = new Vehicle();
      int[] depot = depots[i % depots.length];
      car.x = depot[0] + 0.5F;
      car.y = depot[1] + 0.5F;
      car.direction = i % 4;
      car.desiredDirection = car.direction;
      traffic[i] = car;
    }

    resetPositions();
  }

  /**
   * Returns the sweeper to the depot without touching the litter, as after a wreck.
   *
   * @since 1.0
   */
  private void resetPositions() {
    sweeper.x = 10.5F;
    sweeper.y = 17.5F;
    sweeper.direction = 2;
    sweeper.desiredDirection = 2;
    preemptTimer = 0;
    wreckTimer = 0;
    readyTimer = READY_STEPS;
  }

  /**
   * Retrieves whether the specified cell is drivable street. Columns wrap, so the through route
   * across the map edge works; rows do not.
   *
   * @param column the cell column
   * @param row    the cell row
   *
   * @return {@code true} if the cell is street rather than building
   *
   * @since 1.0
   */
  private boolean isStreet(int column, int row) {
    if (row < 0 || row >= ROWS) {
      return false;
    }
    int wrapped = ((column % COLUMNS) + COLUMNS) % COLUMNS;
    return CITY[row].charAt(wrapped) != '#';
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
    if (readyTimer > 0) {
      readyTimer--;
      readSteering(input);
      return;
    }

    if (wreckTimer > 0) {
      wreckTimer--;
      if (wreckTimer == 0 && !loseLife()) {
        resetPositions();
      }
      return;
    }

    readSteering(input);
    advance(sweeper, SWEEPER_SPEED);
    sweepAt((int) sweeper.x, (int) sweeper.y);

    if (preemptTimer > 0) {
      preemptTimer--;
    } else {
      float speed = TRAFFIC_SPEED + Math.min(0.02F, (level - 1) * 0.003F);
      for (Vehicle car : traffic) {
        driveTraffic(car);
        advance(car, speed);
      }
    }

    for (Vehicle car : traffic) {
      if (touching(car, sweeper)) {
        // Frozen traffic is simply parked — you can drive straight past it.
        if (preemptTimer == 0) {
          wreckTimer = WRECK_STEPS;
        }
        return;
      }
    }

    if (litterRemaining <= 0) {
      level++;
      addScore(500);
      loadShift();
    }
  }

  /**
   * Translates the control panel into the sweeper's next turn.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void readSteering(ArcadeInput input) {
    if (input.held(ArcadeInput.Button.RIGHT)) {
      sweeper.desiredDirection = 0;
    } else if (input.held(ArcadeInput.Button.DOWN)) {
      sweeper.desiredDirection = 1;
    } else if (input.held(ArcadeInput.Button.LEFT)) {
      sweeper.desiredDirection = 2;
    } else if (input.held(ArcadeInput.Button.UP)) {
      sweeper.desiredDirection = 3;
    }
  }

  /**
   * Picks a car's next turn. Drivers carry straight on wherever the street allows and only turn
   * when it does not, so traffic follows plausible routes instead of chasing the player.
   *
   * @param car the car to steer
   *
   * @since 1.0
   */
  private void driveTraffic(Vehicle car) {
    int column = (int) Math.floor(car.x);
    int row = (int) Math.floor(car.y);
    float speed = TRAFFIC_SPEED;
    boolean atIntersection = Math.abs(car.x - (column + 0.5F)) <= speed
        && Math.abs(car.y - (row + 0.5F)) <= speed;
    if (!atIntersection) {
      return;
    }

    if (isStreet(column + DIRECTION_X[car.direction], row + DIRECTION_Y[car.direction])) {
      // Carry on, but occasionally take a legal turn so routes do not become fixed loops.
      if (random.nextInt(9) != 0) {
        car.desiredDirection = car.direction;
        return;
      }
    }

    int reverse = (car.direction + 2) % 4;
    int[] options = new int[4];
    int optionCount = 0;
    for (int direction = 0; direction < 4; direction++) {
      if (direction == reverse) {
        continue;
      }
      if (isStreet(column + DIRECTION_X[direction], row + DIRECTION_Y[direction])) {
        options[optionCount++] = direction;
      }
    }
    // A dead end is the only place a driver will back out of.
    car.desiredDirection = optionCount == 0 ? reverse : options[random.nextInt(optionCount)];
  }

  /**
   * Moves a vehicle one step, honouring its queued turn and the buildings.
   *
   * @param vehicle the vehicle to move
   * @param speed   the distance to travel, in cells
   *
   * @since 1.0
   */
  private void advance(Vehicle vehicle, float speed) {
    int column = (int) Math.floor(vehicle.x);
    int row = (int) Math.floor(vehicle.y);
    boolean atCenter = Math.abs(vehicle.x - (column + 0.5F)) <= speed
        && Math.abs(vehicle.y - (row + 0.5F)) <= speed;

    if (atCenter) {
      vehicle.x = column + 0.5F;
      vehicle.y = row + 0.5F;
      if (vehicle.desiredDirection != vehicle.direction
          && isStreet(column + DIRECTION_X[vehicle.desiredDirection],
          row + DIRECTION_Y[vehicle.desiredDirection])) {
        vehicle.direction = vehicle.desiredDirection;
      }
      if (!isStreet(column + DIRECTION_X[vehicle.direction],
          row + DIRECTION_Y[vehicle.direction])) {
        return;
      }
    }

    vehicle.x += DIRECTION_X[vehicle.direction] * speed;
    vehicle.y += DIRECTION_Y[vehicle.direction] * speed;
    vehicle.x = wrap(vehicle.x, COLUMNS);
  }

  /**
   * Sweeps whatever is on the specified street tile.
   *
   * @param column the cell column
   * @param row    the cell row
   *
   * @since 1.0
   */
  private void sweepAt(int column, int row) {
    if (row < 0 || row >= ROWS || column < 0 || column >= COLUMNS) {
      return;
    }
    char cell = litter[row][column];
    if (cell == ' ') {
      return;
    }
    litter[row][column] = ' ';
    litterRemaining--;
    if (cell == 'o') {
      addScore(PREEMPT_POINTS);
      // Later levels get a shorter preempt window.
      preemptTimer = Math.max(TICK_RATE * 2, PREEMPT_STEPS - (level - 1) * (TICK_RATE / 2));
    } else {
      addScore(LITTER_POINTS);
    }
  }

  /**
   * Retrieves whether two vehicles are in contact, measured the short way around the wrapping map.
   *
   * @param first  the first vehicle
   * @param second the second vehicle
   *
   * @return {@code true} if the two overlap
   *
   * @since 1.0
   */
  private static boolean touching(Vehicle first, Vehicle second) {
    float deltaX = Math.abs(first.x - second.x);
    deltaX = Math.min(deltaX, COLUMNS - deltaX);
    return deltaX < 0.7F && Math.abs(first.y - second.y) < 0.7F;
  }

  /**
   * Draws the city blocks, the streets, the litter and the vehicles.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, getPlayfieldWidth(), getPlayfieldHeight(), 0xFF101418);

    for (int row = 0; row < ROWS; row++) {
      for (int column = 0; column < COLUMNS; column++) {
        float x = column * CELL;
        float y = row * CELL;
        if (CITY[row].charAt(column) == '#') {
          screen.rect(x, y, CELL, CELL, 0xFF2C3440);
          screen.rect(x + 1F, y + 1F, CELL - 2F, CELL - 2F, 0xFF3A4658);
          // A lit window or two, on a pattern fixed per block so the skyline stays still.
          if ((column * 7 + row * 5) % 3 == 0) {
            screen.rect(x + 2F, y + 2F, 2F, 2F, 0xFFFFE080);
          }
          if ((column * 3 + row * 11) % 4 == 0) {
            screen.rect(x + CELL - 4F, y + CELL - 4F, 2F, 2F, 0xFFFFE080);
          }
          continue;
        }

        screen.rect(x, y, CELL, CELL, 0xFF181C22);
        char remaining = litter[row][column];
        if (remaining == '.') {
          screen.dot(x + CELL * 0.5F, y + CELL * 0.5F, 0.9F, 0xFFC8C0A0);
        } else if (remaining == 'o') {
          // The preempt pickup reads as a little signal head, cycling green while it waits.
          int color = (ticks / 10) % 2 == 0 ? 0xFF40FF60 : 0xFF208030;
          screen.rect(x + CELL * 0.32F, y + CELL * 0.18F, CELL * 0.36F, CELL * 0.64F, 0xFF303030);
          screen.dot(x + CELL * 0.5F, y + CELL * 0.5F, 1.5F, color);
        }
      }
    }

    if (preemptTimer > 0) {
      // Everything is held at red — tint the streets so the state is unmistakable.
      screen.rect(0F, 0F, getPlayfieldWidth(), getPlayfieldHeight(), 0x18FF3030);
    }

    for (int i = 0; i < traffic.length; i++) {
      drawCar(screen, traffic[i], CAR_COLORS[i % CAR_COLORS.length]);
    }

    drawSweeper(screen);

    if (readyTimer > 0) {
      screen.textCentered("SHIFT " + level, getPlayfieldWidth() * 0.5F,
          getPlayfieldHeight() * 0.56F, 0xFFFFD040);
    } else if (preemptTimer > 0) {
      screen.textCentered("SIGNAL PREEMPT", getPlayfieldWidth() * 0.5F, 2F, 0xFFFF6060);
    }
  }

  /**
   * Draws the player's sweeper, including its spinning brush and wreck animation.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawSweeper(ArcadeScreen screen) {
    float centerX = sweeper.x * CELL;
    float centerY = sweeper.y * CELL;

    if (wreckTimer > 0) {
      // Hazard flashers, then gone.
      if ((wreckTimer / 4) % 2 == 0) {
        screen.rect(centerX - 3F, centerY - 3F, 6F, 6F, 0xFFFF8020);
      }
      return;
    }

    boolean lengthwise = sweeper.direction == 0 || sweeper.direction == 2;
    float halfLong = CELL * 0.42F;
    float halfShort = CELL * 0.28F;
    screen.rect(centerX - (lengthwise ? halfLong : halfShort),
        centerY - (lengthwise ? halfShort : halfLong),
        (lengthwise ? halfLong : halfShort) * 2F, (lengthwise ? halfShort : halfLong) * 2F,
        0xFFFFC020);
    // Cab, at the leading end.
    screen.rect(centerX + DIRECTION_X[sweeper.direction] * halfLong * 0.45F - 1.4F,
        centerY + DIRECTION_Y[sweeper.direction] * halfLong * 0.45F - 1.4F, 2.8F, 2.8F,
        0xFF303840);
    // The brush sweeps at the trailing end.
    float brushX = centerX - DIRECTION_X[sweeper.direction] * halfLong;
    float brushY = centerY - DIRECTION_Y[sweeper.direction] * halfLong;
    float phase = (ticks % 8) / 8F * (float) Math.PI * 2F;
    for (int i = 0; i < 4; i++) {
      double angle = phase + Math.PI * 0.5 * i;
      screen.line(brushX, brushY, brushX + (float) Math.cos(angle) * 2.6F,
          brushY + (float) Math.sin(angle) * 2.6F, 0xFFE0E0E0);
    }
  }

  /**
   * Draws one car, with headlights pointing the way it is travelling.
   *
   * @param screen the screen to draw on
   * @param car    the car to draw
   * @param color  the car's paint colour
   *
   * @since 1.0
   */
  private void drawCar(ArcadeScreen screen, Vehicle car, int color) {
    float centerX = car.x * CELL;
    float centerY = car.y * CELL;
    boolean lengthwise = car.direction == 0 || car.direction == 2;
    float halfLong = CELL * 0.38F;
    float halfShort = CELL * 0.24F;

    if (preemptTimer > 0) {
      // Parked at a red light: brake lights on, colour dimmed.
      color = (color >>> 1) & 0x7F7F7F | 0xFF000000;
    }

    screen.rect(centerX - (lengthwise ? halfLong : halfShort),
        centerY - (lengthwise ? halfShort : halfLong),
        (lengthwise ? halfLong : halfShort) * 2F, (lengthwise ? halfShort : halfLong) * 2F, color);

    float lightX = centerX + DIRECTION_X[car.direction] * halfLong;
    float lightY = centerY + DIRECTION_Y[car.direction] * halfLong;
    int lightColor = preemptTimer > 0 ? 0xFFFF4040 : 0xFFFFF0C0;
    screen.dot(lightX, lightY, 0.9F, lightColor);
  }
}
