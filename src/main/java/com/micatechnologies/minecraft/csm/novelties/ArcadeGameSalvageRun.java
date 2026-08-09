package com.micatechnologies.minecraft.csm.novelties;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Salvage Run</b> — an original CSM cabinet game. The player flies an orbital tug with no weapons
 * at all: the job is to hook drifting cargo pods on a tractor beam, tow them back to the depot ring
 * in the middle of the field, and do it before the shift clock runs out.
 *
 * <p>The tension comes from towing rather than shooting. A pod on the beam drags behind the tug and
 * swings wide through turns, so a full load makes the ship handle badly exactly when it most needs
 * to thread past the tumbling wrecks. Hitting a wreck costs a life and scatters whatever was in
 * tow.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameSalvageRun extends ArcadeGame {

  private static final float FIELD_WIDTH = 320F;
  private static final float FIELD_HEIGHT = 240F;

  private static final float TURN_RATE = 0.072F;
  private static final float THRUST = 0.1F;
  private static final float DRAG = 0.99F;
  private static final float MAX_SPEED = 3F;
  private static final float TUG_RADIUS = 7F;

  /**
   * How far ahead of the tug the beam reaches.
   */
  private static final float BEAM_RANGE = 52F;
  /**
   * How wide a cone the beam covers, in radians either side of the nose.
   */
  private static final float BEAM_HALF_ANGLE = 0.55F;
  /**
   * How strongly a towed pod is pulled toward its hold point behind the tug.
   */
  private static final float TOW_STIFFNESS = 0.055F;
  /**
   * How many pods the tug can have in tow at once.
   */
  private static final int MAX_TOW = 3;

  private static final float DEPOT_RADIUS = 20F;
  private static final int SHIFT_STEPS = 60 * TICK_RATE;
  private static final int RESPAWN_SHIELD_STEPS = 2 * TICK_RATE;

  private static final int HULL_COLOR = 0xFFD8ECFF;
  private static final int POD_COLOR = 0xFFFFC040;
  private static final int WRECK_COLOR = 0xFF7090A0;
  private static final int BEAM_COLOR = 0x9040FFC0;

  /**
   * A cargo pod: free-drifting until hooked, then towed, then delivered.
   *
   * @since 1.0
   */
  private static final class Pod {

    private float x;
    private float y;
    private float velocityX;
    private float velocityY;
    private float spin;
    private float angle;
    /**
     * Position in the tow chain, or -1 when drifting free.
     */
    private int towIndex = -1;
  }

  /**
   * A tumbling piece of scrap. Purely a hazard; it cannot be destroyed.
   *
   * @since 1.0
   */
  private static final class Wreck {

    private float x;
    private float y;
    private float velocityX;
    private float velocityY;
    private float radius;
    private float angle;
    private float spin;
    private float[] shape;
  }

  private final List<Pod> pods = new ArrayList<>();
  private final List<Wreck> wrecks = new ArrayList<>();
  private float tugX;
  private float tugY;
  private float tugVelocityX;
  private float tugVelocityY;
  private float tugAngle;
  private boolean thrusting;
  private boolean beamOn;
  private int towedCount;
  private int spawnShield;
  private int shiftClock;
  private int deliveredThisShift;
  private int explosionTimer;

  /**
   * Constructs a new Salvage Run game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameSalvageRun() {
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
    return "SALVAGE RUN";
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
    return "Left/Right turn  -  Up thrust  -  Hold Space for the tractor beam";
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
   * Clears the field and starts the first shift.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    wrecks.clear();
    respawnTug();
    startShift();
  }

  /**
   * Parks the tug at the depot with its beam empty.
   *
   * @since 1.0
   */
  private void respawnTug() {
    tugX = FIELD_WIDTH * 0.5F;
    tugY = FIELD_HEIGHT * 0.5F + DEPOT_RADIUS + 18F;
    tugVelocityX = 0F;
    tugVelocityY = 0F;
    tugAngle = -(float) Math.PI / 2F;
    towedCount = 0;
    spawnShield = RESPAWN_SHIELD_STEPS;
    explosionTimer = 0;
  }

  /**
   * Scatters a new consignment of pods and the scrap that comes with it.
   *
   * @since 1.0
   */
  private void startShift() {
    pods.clear();
    wrecks.clear();
    deliveredThisShift = 0;
    shiftClock = SHIFT_STEPS;

    int podCount = 4 + level;
    for (int i = 0; i < podCount; i++) {
      Pod pod = new Pod();
      float[] position = findClearSpot();
      pod.x = position[0];
      pod.y = position[1];
      double heading = random.nextFloat() * Math.PI * 2.0;
      float speed = 0.15F + random.nextFloat() * 0.3F;
      pod.velocityX = (float) Math.cos(heading) * speed;
      pod.velocityY = (float) Math.sin(heading) * speed;
      pod.spin = (random.nextFloat() - 0.5F) * 0.04F;
      pods.add(pod);
    }

    int wreckCount = Math.min(9, 2 + level);
    for (int i = 0; i < wreckCount; i++) {
      Wreck wreck = new Wreck();
      float[] position = findClearSpot();
      wreck.x = position[0];
      wreck.y = position[1];
      wreck.radius = 9F + random.nextFloat() * 11F;
      double heading = random.nextFloat() * Math.PI * 2.0;
      float speed = 0.3F + random.nextFloat() * 0.45F + Math.min(0.5F, level * 0.05F);
      wreck.velocityX = (float) Math.cos(heading) * speed;
      wreck.velocityY = (float) Math.sin(heading) * speed;
      wreck.angle = random.nextFloat() * (float) Math.PI * 2F;
      wreck.spin = (random.nextFloat() - 0.5F) * 0.05F;
      wreck.shape = new float[8];
      for (int vertex = 0; vertex < wreck.shape.length; vertex++) {
        wreck.shape[vertex] = 0.6F + random.nextFloat() * 0.5F;
      }
      wrecks.add(wreck);
    }
  }

  /**
   * Finds a spawn position clear of both the tug and the depot.
   *
   * @return the chosen position, as an {x, y} pair
   *
   * @since 1.0
   */
  private float[] findClearSpot() {
    float x;
    float y;
    int attempts = 0;
    do {
      x = random.nextFloat() * FIELD_WIDTH;
      y = random.nextFloat() * FIELD_HEIGHT;
      attempts++;
    } while (attempts < 40
        && (distanceSquared(x, y, tugX, tugY) < 70F * 70F
        || distanceSquared(x, y, FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.5F)
        < (DEPOT_RADIUS + 26F) * (DEPOT_RADIUS + 26F)));
    return new float[] {x, y};
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
    if (explosionTimer > 0) {
      explosionTimer--;
      driftEverything();
      if (explosionTimer == 0 && !loseLife()) {
        respawnTug();
      }
      return;
    }

    if (--shiftClock <= 0) {
      // The clock running out ends the run outright; there is no spare shift.
      gameOver = true;
      return;
    }

    updateTug(input);
    driftEverything();
    updateTow();
    deliverPods();
    checkWreckCollisions();

    if (pods.isEmpty()) {
      level++;
      // Time left on the clock pays out, then a fresh consignment arrives.
      addScore(300 + shiftClock / TICK_RATE * 10);
      startShift();
    }
  }

  /**
   * Applies the control panel to the tug and works the tractor beam.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void updateTug(ArcadeInput input) {
    if (spawnShield > 0) {
      spawnShield--;
    }

    if (input.held(ArcadeInput.Button.LEFT)) {
      tugAngle -= TURN_RATE;
    }
    if (input.held(ArcadeInput.Button.RIGHT)) {
      tugAngle += TURN_RATE;
    }

    thrusting = input.held(ArcadeInput.Button.UP) || input.held(ArcadeInput.Button.ALT);
    if (thrusting) {
      // A loaded tug is sluggish; each pod in tow costs acceleration.
      float loadFactor = 1F - towedCount * 0.18F;
      tugVelocityX += (float) Math.cos(tugAngle) * THRUST * loadFactor;
      tugVelocityY += (float) Math.sin(tugAngle) * THRUST * loadFactor;
      float speed = (float) Math.sqrt(tugVelocityX * tugVelocityX + tugVelocityY * tugVelocityY);
      if (speed > MAX_SPEED) {
        tugVelocityX = tugVelocityX / speed * MAX_SPEED;
        tugVelocityY = tugVelocityY / speed * MAX_SPEED;
      }
    }

    tugVelocityX *= DRAG;
    tugVelocityY *= DRAG;
    tugX = wrap(tugX + tugVelocityX, FIELD_WIDTH);
    tugY = wrap(tugY + tugVelocityY, FIELD_HEIGHT);

    beamOn = input.held(ArcadeInput.Button.FIRE);
    if (beamOn && towedCount < MAX_TOW) {
      hookNearestPod();
    }
  }

  /**
   * Hooks the closest free pod that lies inside the beam cone.
   *
   * @since 1.0
   */
  private void hookNearestPod() {
    Pod best = null;
    float bestDistance = BEAM_RANGE * BEAM_RANGE;
    for (Pod pod : pods) {
      if (pod.towIndex >= 0) {
        continue;
      }
      float distance = distanceSquared(pod.x, pod.y, tugX, tugY);
      if (distance > bestDistance) {
        continue;
      }
      float deltaX = shortestDelta(pod.x - tugX, FIELD_WIDTH);
      float deltaY = shortestDelta(pod.y - tugY, FIELD_HEIGHT);
      double bearing = Math.atan2(deltaY, deltaX);
      double offset = Math.abs(normalizeAngle((float) bearing - tugAngle));
      if (offset > BEAM_HALF_ANGLE) {
        continue;
      }
      best = pod;
      bestDistance = distance;
    }
    if (best != null) {
      best.towIndex = towedCount++;
    }
  }

  /**
   * Drifts every free pod and every wreck.
   *
   * @since 1.0
   */
  private void driftEverything() {
    for (Pod pod : pods) {
      pod.angle += pod.spin;
      if (pod.towIndex >= 0) {
        continue;
      }
      pod.x = wrap(pod.x + pod.velocityX, FIELD_WIDTH);
      pod.y = wrap(pod.y + pod.velocityY, FIELD_HEIGHT);
    }
    for (Wreck wreck : wrecks) {
      wreck.x = wrap(wreck.x + wreck.velocityX, FIELD_WIDTH);
      wreck.y = wrap(wreck.y + wreck.velocityY, FIELD_HEIGHT);
      wreck.angle += wreck.spin;
    }
  }

  /**
   * Drags every towed pod toward its hold point in the chain behind the tug. The lag in this pull is
   * what makes a loaded tug swing wide through corners.
   *
   * @since 1.0
   */
  private void updateTow() {
    for (Pod pod : pods) {
      if (pod.towIndex < 0) {
        continue;
      }
      float distanceBack = TUG_RADIUS + 12F + pod.towIndex * 13F;
      float holdX = wrap(tugX - (float) Math.cos(tugAngle) * distanceBack, FIELD_WIDTH);
      float holdY = wrap(tugY - (float) Math.sin(tugAngle) * distanceBack, FIELD_HEIGHT);
      pod.velocityX += shortestDelta(holdX - pod.x, FIELD_WIDTH) * TOW_STIFFNESS;
      pod.velocityY += shortestDelta(holdY - pod.y, FIELD_HEIGHT) * TOW_STIFFNESS;
      pod.velocityX *= 0.86F;
      pod.velocityY *= 0.86F;
      pod.x = wrap(pod.x + pod.velocityX, FIELD_WIDTH);
      pod.y = wrap(pod.y + pod.velocityY, FIELD_HEIGHT);
    }
  }

  /**
   * Books in any towed pod that has been dragged inside the depot ring.
   *
   * @since 1.0
   */
  private void deliverPods() {
    float depotX = FIELD_WIDTH * 0.5F;
    float depotY = FIELD_HEIGHT * 0.5F;
    boolean delivered = false;

    for (int i = 0; i < pods.size(); i++) {
      Pod pod = pods.get(i);
      if (pod.towIndex < 0) {
        continue;
      }
      if (distanceSquared(pod.x, pod.y, depotX, depotY) > DEPOT_RADIUS * DEPOT_RADIUS) {
        continue;
      }
      pods.remove(i--);
      deliveredThisShift++;
      // Delivering a full chain in one trip is worth more than three separate runs.
      addScore(100 * Math.min(MAX_TOW, towedCount));
      delivered = true;
    }

    if (delivered) {
      renumberTow();
    }
  }

  /**
   * Rebuilds the tow chain's indices after a delivery, so the remaining pods close up the gap.
   *
   * @since 1.0
   */
  private void renumberTow() {
    towedCount = 0;
    for (Pod pod : pods) {
      if (pod.towIndex >= 0) {
        pod.towIndex = towedCount++;
      }
    }
  }

  /**
   * Tests the tug and its load against the tumbling scrap.
   *
   * @since 1.0
   */
  private void checkWreckCollisions() {
    if (spawnShield > 0) {
      return;
    }
    for (Wreck wreck : wrecks) {
      float reach = wreck.radius + TUG_RADIUS * 0.7F;
      if (distanceSquared(tugX, tugY, wreck.x, wreck.y) < reach * reach) {
        explosionTimer = TICK_RATE;
        cutLoose();
        return;
      }
      for (Pod pod : pods) {
        if (pod.towIndex < 0) {
          continue;
        }
        float podReach = wreck.radius + 5F;
        if (distanceSquared(pod.x, pod.y, wreck.x, wreck.y) < podReach * podReach) {
          // The pod is knocked off the beam but survives; only the tug can be destroyed.
          pod.towIndex = -1;
          double kick = random.nextFloat() * Math.PI * 2.0;
          pod.velocityX = (float) Math.cos(kick) * 0.6F;
          pod.velocityY = (float) Math.sin(kick) * 0.6F;
          renumberTow();
          return;
        }
      }
    }
  }

  /**
   * Releases everything in tow, as happens when the tug is destroyed.
   *
   * @since 1.0
   */
  private void cutLoose() {
    for (Pod pod : pods) {
      pod.towIndex = -1;
    }
    towedCount = 0;
  }

  /**
   * Retrieves the squared distance between two points, measured the short way around the wrapping
   * field.
   *
   * @param x1 the first point's X coordinate
   * @param y1 the first point's Y coordinate
   * @param x2 the second point's X coordinate
   * @param y2 the second point's Y coordinate
   *
   * @return the squared distance
   *
   * @since 1.0
   */
  private static float distanceSquared(float x1, float y1, float x2, float y2) {
    float deltaX = shortestDelta(x1 - x2, FIELD_WIDTH);
    float deltaY = shortestDelta(y1 - y2, FIELD_HEIGHT);
    return deltaX * deltaX + deltaY * deltaY;
  }

  /**
   * Reduces a difference to the shortest way around a wrapping axis.
   *
   * @param delta the raw difference
   * @param limit the axis length
   *
   * @return the equivalent difference in the range {@code [-limit/2, limit/2]}
   *
   * @since 1.0
   */
  private static float shortestDelta(float delta, float limit) {
    if (delta > limit * 0.5F) {
      return delta - limit;
    }
    if (delta < -limit * 0.5F) {
      return delta + limit;
    }
    return delta;
  }

  /**
   * Reduces an angle to the range {@code [-pi, pi]}.
   *
   * @param angle the raw angle, in radians
   *
   * @return the normalized angle
   *
   * @since 1.0
   */
  private static float normalizeAngle(float angle) {
    final float fullTurn = (float) (Math.PI * 2.0);
    while (angle > (float) Math.PI) {
      angle -= fullTurn;
    }
    while (angle < -(float) Math.PI) {
      angle += fullTurn;
    }
    return angle;
  }

  /**
   * Draws the depot, the field and the tug.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF000008);

    float depotX = FIELD_WIDTH * 0.5F;
    float depotY = FIELD_HEIGHT * 0.5F;
    screen.circle(depotX, depotY, DEPOT_RADIUS, 24, 0xFF40FFC0);
    screen.circle(depotX, depotY, DEPOT_RADIUS * 0.55F, 16, 0xFF208060);
    for (int i = 0; i < 4; i++) {
      double angle = Math.PI * 0.5 * i + ticks * 0.01;
      screen.line(depotX + (float) Math.cos(angle) * DEPOT_RADIUS * 0.55F,
          depotY + (float) Math.sin(angle) * DEPOT_RADIUS * 0.55F,
          depotX + (float) Math.cos(angle) * DEPOT_RADIUS,
          depotY + (float) Math.sin(angle) * DEPOT_RADIUS, 0xFF208060);
    }

    float[] xs = new float[10];
    float[] ys = new float[10];
    for (Wreck wreck : wrecks) {
      int vertices = wreck.shape.length;
      for (int i = 0; i < vertices; i++) {
        double angle = wreck.angle + Math.PI * 2.0 * i / vertices;
        xs[i] = wreck.x + (float) Math.cos(angle) * wreck.radius * wreck.shape[i];
        ys[i] = wreck.y + (float) Math.sin(angle) * wreck.radius * wreck.shape[i];
      }
      screen.polyline(xs, ys, vertices, true, WRECK_COLOR);
    }

    for (Pod pod : pods) {
      drawPod(screen, pod);
    }

    if (beamOn && explosionTimer == 0) {
      float noseX = tugX + (float) Math.cos(tugAngle) * TUG_RADIUS;
      float noseY = tugY + (float) Math.sin(tugAngle) * TUG_RADIUS;
      for (int i = -1; i <= 1; i += 2) {
        double edge = tugAngle + i * BEAM_HALF_ANGLE;
        screen.line(noseX, noseY, noseX + (float) Math.cos(edge) * BEAM_RANGE,
            noseY + (float) Math.sin(edge) * BEAM_RANGE, BEAM_COLOR);
      }
    }

    if (explosionTimer > 0) {
      float progress = 1F - explosionTimer / (float) TICK_RATE;
      for (int i = 0; i < 10; i++) {
        double angle = Math.PI * 2.0 * i / 10.0;
        float distance = progress * 28F;
        float x = tugX + (float) Math.cos(angle) * distance;
        float y = tugY + (float) Math.sin(angle) * distance;
        screen.line(x, y, x + (float) Math.cos(angle) * 5F, y + (float) Math.sin(angle) * 5F,
            0xFFFFB040);
      }
    } else if (spawnShield == 0 || (spawnShield / 5) % 2 == 0) {
      drawTug(screen);
    }

    // Shift clock along the top, turning red as it runs down.
    float clockFraction = shiftClock / (float) SHIFT_STEPS;
    int clockColor = clockFraction < 0.2F ? 0xFFFF4040 : 0xFF40FFC0;
    screen.rect(4F, 4F, (FIELD_WIDTH - 8F) * clockFraction, 3F, clockColor);
    screen.text("PODS " + pods.size() + "   RUN " + deliveredThisShift, 4F, 9F, 0xFF80A0C0);
  }

  /**
   * Draws one cargo pod, brighter while it is under tow.
   *
   * @param screen the screen to draw on
   * @param pod    the pod to draw
   *
   * @since 1.0
   */
  private void drawPod(ArcadeScreen screen, Pod pod) {
    int color = pod.towIndex >= 0 ? 0xFF60FFC0 : POD_COLOR;
    float half = 4.5F;
    float cos = (float) Math.cos(pod.angle);
    float sin = (float) Math.sin(pod.angle);
    float[] localX = {-half, half, half, -half};
    float[] localY = {-half, -half, half, half};
    float[] xs = new float[4];
    float[] ys = new float[4];
    for (int i = 0; i < 4; i++) {
      xs[i] = pod.x + localX[i] * cos - localY[i] * sin;
      ys[i] = pod.y + localX[i] * sin + localY[i] * cos;
    }
    screen.polyline(xs, ys, 4, true, color);
    screen.line(xs[0], ys[0], xs[2], ys[2], color);

    if (pod.towIndex >= 0) {
      // The tether back toward the tug.
      screen.line(pod.x, pod.y, tugX, tugY, 0x5040FFC0);
    }
  }

  /**
   * Draws the tug: a blunt hull with a pair of outboard nacelles, plus its exhaust when thrusting.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawTug(ArcadeScreen screen) {
    float cos = (float) Math.cos(tugAngle);
    float sin = (float) Math.sin(tugAngle);
    float[] localX = {TUG_RADIUS * 1.3F, TUG_RADIUS * 0.3F, -TUG_RADIUS, -TUG_RADIUS,
        TUG_RADIUS * 0.3F};
    float[] localY = {0F, -TUG_RADIUS * 0.75F, -TUG_RADIUS * 0.6F, TUG_RADIUS * 0.6F,
        TUG_RADIUS * 0.75F};
    float[] xs = new float[5];
    float[] ys = new float[5];
    for (int i = 0; i < 5; i++) {
      xs[i] = tugX + localX[i] * cos - localY[i] * sin;
      ys[i] = tugY + localX[i] * sin + localY[i] * cos;
    }
    screen.polyline(xs, ys, 5, true, HULL_COLOR);
    screen.line(xs[1], ys[1], xs[4], ys[4], HULL_COLOR);

    if (thrusting && (ticks / 3) % 2 == 0) {
      float tailX = tugX - cos * TUG_RADIUS;
      float tailY = tugY - sin * TUG_RADIUS;
      screen.line(tailX, tailY, tailX - cos * 7F, tailY - sin * 7F, 0xFFFFB040);
    }
  }
}
