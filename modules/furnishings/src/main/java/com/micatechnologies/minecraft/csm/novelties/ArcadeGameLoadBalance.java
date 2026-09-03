package com.micatechnologies.minecraft.csm.novelties;

/**
 * <b>Load Balance</b> — an original CSM cabinet game. The player works a substation dispatch board:
 * six feeders each carry a rising load, and the job is to keep every one of them out of the red.
 *
 * <p>Scoring rewards nerve. A feeder vented while nearly full pays several times what an early vent
 * does, so the safe play of dumping everything on sight will not keep pace with the demand curve.
 * The load-shed lever knocks every feeder down at once but costs banked points, making it a genuine
 * last resort rather than a free reset. Three tripped feeders ends the shift.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameLoadBalance extends ArcadeGame {

  private static final int FEEDERS = 6;
  private static final float FIELD_WIDTH = 240F;
  private static final float FIELD_HEIGHT = 200F;

  private static final float GAUGE_WIDTH = 24F;
  private static final float GAUGE_HEIGHT = 118F;
  private static final float GAUGE_SPACING = 36F;
  private static final float GAUGE_TOP = 34F;

  /**
   * The load level at which a feeder trips.
   */
  private static final float TRIP_LEVEL = 1F;
  /**
   * Load above this is "hot" — vents from here pay the premium.
   */
  private static final float HOT_LEVEL = 0.75F;
  /**
   * How much load one vent removes.
   */
  private static final float VENT_AMOUNT = 0.62F;
  /**
   * How much load the shed lever removes from every feeder.
   */
  private static final float SHED_AMOUNT = 0.45F;
  /**
   * The points surrendered for pulling the shed lever.
   */
  private static final int SHED_COST = 400;

  private static final int VENT_COOLDOWN = 10;
  private static final int SHED_COOLDOWN = 12 * TICK_RATE;
  private static final int MOVE_COOLDOWN = 7;
  private static final int TRIPS_ALLOWED = 3;
  private static final int SHIFT_INTRO_STEPS = (int) (1.2F * TICK_RATE);

  /**
   * Each feeder's current load, from 0 to {@link #TRIP_LEVEL}.
   */
  private final float[] load = new float[FEEDERS];

  /**
   * Each feeder's demand rate, in load per step.
   */
  private final float[] rate = new float[FEEDERS];

  /**
   * Steps remaining of each feeder's vent animation.
   */
  private final int[] ventFlash = new int[FEEDERS];

  /**
   * Whether each feeder has tripped and is out of service.
   */
  private final boolean[] tripped = new boolean[FEEDERS];

  private int cursor;
  private int moveCooldown;
  private int ventCooldown;
  private int shedCooldown;
  private int shedFlash;
  private int tripCount;
  private int shiftIntroTimer;
  /**
   * Accumulates the per-step uptime award so it can be paid in whole points.
   */
  private int uptimeAccumulator;

  /**
   * Constructs a new Load Balance game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameLoadBalance() {
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
    return "LOAD BALANCE";
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
    return "Left/Right pick a feeder  -  Space vents it  -  Shift sheds the whole board";
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
   * Retrieves the number of feeders still in service, shown in place of a life count.
   *
   * @return the feeders remaining before the shift ends
   *
   * @since 1.0
   */
  @Override
  public int getLives() {
    return Math.max(0, TRIPS_ALLOWED - tripCount);
  }

  /**
   * Brings every feeder back on line and starts the shift.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    tripCount = 0;
    cursor = 0;
    moveCooldown = 0;
    ventCooldown = 0;
    shedCooldown = 0;
    shedFlash = 0;
    uptimeAccumulator = 0;
    shiftIntroTimer = SHIFT_INTRO_STEPS;
    for (int i = 0; i < FEEDERS; i++) {
      load[i] = 0.1F + random.nextFloat() * 0.2F;
      tripped[i] = false;
      ventFlash[i] = 0;
      rollRate(i);
    }
  }

  /**
   * Assigns a feeder a fresh demand rate for the current level.
   *
   * @param index the feeder index
   *
   * @since 1.0
   */
  private void rollRate(int index) {
    float base = 0.0016F + random.nextFloat() * 0.0022F;
    rate[index] = base * (1F + (level - 1) * 0.16F);
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
    for (int i = 0; i < FEEDERS; i++) {
      if (ventFlash[i] > 0) {
        ventFlash[i]--;
      }
    }
    if (shedFlash > 0) {
      shedFlash--;
    }

    if (shiftIntroTimer > 0) {
      shiftIntroTimer--;
      return;
    }

    handleControls(input);
    raiseLoads();
    payUptime();

    // Demand steps up every half minute survived.
    if (ticks % (30L * TICK_RATE) == 0L) {
      level++;
      for (int i = 0; i < FEEDERS; i++) {
        rollRate(i);
      }
    }
  }

  /**
   * Applies the dispatch board's controls.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void handleControls(ArcadeInput input) {
    if (moveCooldown > 0) {
      moveCooldown--;
    } else if (input.held(ArcadeInput.Button.LEFT)) {
      cursor = (cursor + FEEDERS - 1) % FEEDERS;
      moveCooldown = MOVE_COOLDOWN;
    } else if (input.held(ArcadeInput.Button.RIGHT)) {
      cursor = (cursor + 1) % FEEDERS;
      moveCooldown = MOVE_COOLDOWN;
    }

    if (ventCooldown > 0) {
      ventCooldown--;
    } else if (input.held(ArcadeInput.Button.FIRE) && !tripped[cursor]) {
      vent(cursor);
      ventCooldown = VENT_COOLDOWN;
    }

    if (shedCooldown > 0) {
      shedCooldown--;
    } else if (input.pressed(ArcadeInput.Button.ALT)) {
      shedLoad();
    }
  }

  /**
   * Vents one feeder, paying out on a curve that rewards leaving it as late as possible.
   *
   * @param index the feeder index
   *
   * @since 1.0
   */
  private void vent(int index) {
    float before = load[index];
    if (before < 0.05F) {
      // Nothing worth venting; venting an idle feeder is a wasted press, not free points.
      return;
    }
    load[index] = Math.max(0F, before - VENT_AMOUNT);
    ventFlash[index] = 12;

    if (before >= HOT_LEVEL) {
      // The premium climbs steeply through the hot band.
      float intoRed = (before - HOT_LEVEL) / (TRIP_LEVEL - HOT_LEVEL);
      addScore(150 + (int) (intoRed * 350F));
    } else {
      addScore((int) (before * 60F));
    }
  }

  /**
   * Pulls the load-shed lever: every feeder drops, tripped ones come back on line, and the board
   * surrenders banked points for the privilege.
   *
   * @since 1.0
   */
  private void shedLoad() {
    shedCooldown = SHED_COOLDOWN;
    shedFlash = 16;
    score = Math.max(0, score - SHED_COST);
    for (int i = 0; i < FEEDERS; i++) {
      load[i] = Math.max(0F, load[i] - SHED_AMOUNT);
      if (tripped[i]) {
        // Restoring a tripped feeder does not give the trip back.
        tripped[i] = false;
        load[i] = 0.2F;
      }
    }
  }

  /**
   * Raises every in-service feeder's load and trips the ones that reach the red.
   *
   * @since 1.0
   */
  private void raiseLoads() {
    for (int i = 0; i < FEEDERS; i++) {
      if (tripped[i]) {
        continue;
      }
      load[i] += rate[i];
      if (load[i] < TRIP_LEVEL) {
        continue;
      }
      load[i] = TRIP_LEVEL;
      tripped[i] = true;
      tripCount++;
      if (tripCount >= TRIPS_ALLOWED) {
        gameOver = true;
        return;
      }
    }
  }

  /**
   * Pays the standing award for keeping the board up, scaled by how many feeders are in service.
   *
   * @since 1.0
   */
  private void payUptime() {
    int inService = 0;
    for (boolean out : tripped) {
      if (!out) {
        inService++;
      }
    }
    uptimeAccumulator += inService;
    while (uptimeAccumulator >= TICK_RATE) {
      uptimeAccumulator -= TICK_RATE;
      addScore(2);
    }
  }

  /**
   * Draws the dispatch board.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF0E1418);
    if (shedFlash > 0 && (shedFlash / 4) % 2 == 0) {
      screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0x30FFFFFF);
    }

    screen.text("SUBSTATION DISPATCH", 10F, 8F, 0xFF60A0C0);
    screen.text("DEMAND STEP " + level, 10F, 18F, 0xFF6E7C88);

    float boardLeft = (FIELD_WIDTH - (FEEDERS - 1) * GAUGE_SPACING - GAUGE_WIDTH) * 0.5F;

    for (int i = 0; i < FEEDERS; i++) {
      float x = boardLeft + i * GAUGE_SPACING;
      drawGauge(screen, i, x);
    }

    // Bus bar across the bottom, with the shed lever's readiness on it.
    float busY = GAUGE_TOP + GAUGE_HEIGHT + 12F;
    screen.rect(boardLeft - 6F, busY, (FEEDERS - 1) * GAUGE_SPACING + GAUGE_WIDTH + 12F, 3F,
        0xFF3A4650);

    boolean shedReady = shedCooldown == 0;
    String shedText = shedReady ? "LOAD SHED READY" : "LOAD SHED " + (shedCooldown / TICK_RATE + 1);
    screen.textCentered(shedText, FIELD_WIDTH * 0.5F, busY + 8F,
        shedReady ? 0xFF60FF80 : 0xFF6E7C88);

    if (shiftIntroTimer > 0) {
      screen.textCentered("SHIFT START", FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.45F, 0xFF60A0C0);
    }
  }

  /**
   * Draws one feeder gauge, its label and its state lamp.
   *
   * @param screen the screen to draw on
   * @param index  the feeder index
   * @param x      the gauge's left edge
   *
   * @since 1.0
   */
  private void drawGauge(ArcadeScreen screen, int index, float x) {
    screen.rect(x, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT, 0xFF1A2229);

    // The hot band, marked on the housing so the premium zone is visible before you enter it.
    float hotHeight = GAUGE_HEIGHT * (1F - HOT_LEVEL);
    screen.rect(x, GAUGE_TOP, GAUGE_WIDTH, hotHeight, 0xFF2A1C1C);

    float fill = clamp(load[index], 0F, 1F);
    float fillHeight = GAUGE_HEIGHT * fill;
    int fillColor;
    if (tripped[index]) {
      fillColor = (ticks / 8) % 2 == 0 ? 0xFFFF3030 : 0xFF802020;
    } else if (fill >= HOT_LEVEL) {
      fillColor = 0xFFFFA030;
    } else if (fill >= 0.45F) {
      fillColor = 0xFFFFE040;
    } else {
      fillColor = 0xFF40D060;
    }
    screen.rect(x + 2F, GAUGE_TOP + GAUGE_HEIGHT - fillHeight + 2F, GAUGE_WIDTH - 4F,
        Math.max(0F, fillHeight - 4F), fillColor);

    // Vent flash: a burst of exhaust off the top of the gauge.
    if (ventFlash[index] > 0) {
      float spread = (12 - ventFlash[index]) * 1.4F;
      screen.rect(x + GAUGE_WIDTH * 0.5F - spread * 0.5F, GAUGE_TOP - 6F - spread * 0.4F, spread,
          3F, 0xFF80E0FF);
    }

    // Scale ticks.
    for (int mark = 1; mark < 4; mark++) {
      float markY = GAUGE_TOP + GAUGE_HEIGHT * mark / 4F;
      screen.rect(x, markY, 3F, 1F, 0xFF3A4650);
      screen.rect(x + GAUGE_WIDTH - 3F, markY, 3F, 1F, 0xFF3A4650);
    }

    // Selection frame.
    if (index == cursor) {
      int color = (ticks / 6) % 2 == 0 ? 0xFFFFFFFF : 0xFF90A0B0;
      screen.rect(x - 2F, GAUGE_TOP - 2F, GAUGE_WIDTH + 4F, 1.5F, color);
      screen.rect(x - 2F, GAUGE_TOP + GAUGE_HEIGHT + 0.5F, GAUGE_WIDTH + 4F, 1.5F, color);
      screen.rect(x - 2F, GAUGE_TOP - 2F, 1.5F, GAUGE_HEIGHT + 4F, color);
      screen.rect(x + GAUGE_WIDTH + 0.5F, GAUGE_TOP - 2F, 1.5F, GAUGE_HEIGHT + 4F, color);
    }

    screen.textCentered("F" + (index + 1), x + GAUGE_WIDTH * 0.5F, GAUGE_TOP + GAUGE_HEIGHT + 2F,
        tripped[index] ? 0xFFFF6060 : 0xFF8898A6);
  }
}
