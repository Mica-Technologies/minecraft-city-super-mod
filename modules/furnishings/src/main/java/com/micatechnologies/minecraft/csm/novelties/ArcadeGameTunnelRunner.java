package com.micatechnologies.minecraft.csm.novelties;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Tunnel Runner</b> — an original CSM cabinet game. The player drives a subway train down a
 * three-track tunnel that never ends, switching between tracks to pick up waiting passengers and to
 * miss whatever the maintenance crews have left on the rails.
 *
 * <p>There is nothing to shoot. The only controls are the track switch and the throttle, and the
 * throttle is the whole game: passengers and hazards both arrive faster the harder the train is
 * driven, and the score rate scales with speed, so the run is a standing bet on how fast the player
 * can still read the track ahead.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeGameTunnelRunner extends ArcadeGame {

  private static final float FIELD_WIDTH = 256F;
  private static final float FIELD_HEIGHT = 224F;

  private static final int TRACKS = 3;
  /**
   * The horizon's Y coordinate, where the tunnel converges.
   */
  private static final float VANISHING_Y = 74F;
  /**
   * How far apart the tracks are at the near end of the tunnel.
   */
  private static final float TRACK_SPREAD = 78F;
  /**
   * The depth an object spawns at; zero is under the train's nose.
   */
  private static final float SPAWN_DEPTH = 1F;

  private static final float MIN_SPEED = 0.006F;
  private static final float MAX_SPEED = 0.019F;
  private static final float THROTTLE_RATE = 0.00016F;
  /**
   * How far along a lane switch moves per step.
   */
  private static final float SWITCH_RATE = 0.09F;
  /**
   * Depth window around the train within which a collision or pickup registers.
   */
  private static final float CONTACT_DEPTH = 0.045F;

  private static final int RESPAWN_STEPS = (int) (1.5F * TICK_RATE);
  private static final int INTRO_STEPS = (int) (1.2F * TICK_RATE);

  /**
   * Something sitting on the track ahead: either a hazard or a waiting passenger.
   *
   * @since 1.0
   */
  private static final class Obstruction {

    private int track;
    /**
     * Distance ahead of the train: 1 at the horizon, 0 at the cab.
     */
    private float depth;
    private boolean passenger;
  }

  private final List<Obstruction> obstructions = new ArrayList<>();
  /**
   * The track the train is on, as a float so switching animates.
   */
  private float trainTrack;
  private int targetTrack;
  private float speed;
  private int spawnTimer;
  private int respawnTimer;
  private int introTimer;
  private int passengersAboard;
  private int pickupFlash;
  /**
   * Accumulates the distance award so it can be paid in whole points.
   */
  private float distanceAccumulator;

  /**
   * Constructs a new Tunnel Runner game in its starting state.
   *
   * @since 1.0
   */
  public ArcadeGameTunnelRunner() {
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
    return "TUNNEL RUNNER";
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
    return "Left/Right switch track  -  Up throttle  -  Down brake";
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
   * Puts the train back at the depot portal and clears the tunnel.
   *
   * @since 1.0
   */
  @Override
  protected void resetGame() {
    passengersAboard = 0;
    distanceAccumulator = 0F;
    respawnTrain();
    introTimer = INTRO_STEPS;
  }

  /**
   * Recentres the train and empties the track ahead.
   *
   * @since 1.0
   */
  private void respawnTrain() {
    obstructions.clear();
    trainTrack = 1F;
    targetTrack = 1;
    speed = MIN_SPEED;
    spawnTimer = TICK_RATE;
    pickupFlash = 0;
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
    if (pickupFlash > 0) {
      pickupFlash--;
    }

    if (introTimer > 0) {
      introTimer--;
      return;
    }

    if (respawnTimer > 0) {
      respawnTimer--;
      if (respawnTimer == 0) {
        respawnTrain();
      }
      return;
    }

    handleControls(input);
    advanceTunnel();
    spawnAhead();
    payDistance();
  }

  /**
   * Applies the throttle and the track switch.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  private void handleControls(ArcadeInput input) {
    if (input.held(ArcadeInput.Button.UP)) {
      speed = Math.min(MAX_SPEED, speed + THROTTLE_RATE);
    } else if (input.held(ArcadeInput.Button.DOWN)) {
      speed = Math.max(MIN_SPEED, speed - THROTTLE_RATE * 1.8F);
    } else {
      // The train coasts back toward a comfortable line speed when the driver lets go.
      float cruise = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * 0.35F;
      speed += Math.signum(cruise - speed) * THROTTLE_RATE * 0.25F;
    }

    if (input.pressed(ArcadeInput.Button.LEFT)) {
      targetTrack = Math.max(0, targetTrack - 1);
    }
    if (input.pressed(ArcadeInput.Button.RIGHT)) {
      targetTrack = Math.min(TRACKS - 1, targetTrack + 1);
    }

    float difference = targetTrack - trainTrack;
    if (Math.abs(difference) <= SWITCH_RATE) {
      trainTrack = targetTrack;
    } else {
      trainTrack += Math.signum(difference) * SWITCH_RATE;
    }
  }

  /**
   * Brings everything on the track closer and resolves what the train reaches.
   *
   * @since 1.0
   */
  private void advanceTunnel() {
    for (int i = 0; i < obstructions.size(); i++) {
      Obstruction obstruction = obstructions.get(i);
      obstruction.depth -= speed;

      if (obstruction.depth < -0.06F) {
        obstructions.remove(i--);
        continue;
      }

      if (Math.abs(obstruction.depth) > CONTACT_DEPTH) {
        continue;
      }
      // A switch only counts as complete when the train is genuinely on the track.
      if (Math.abs(trainTrack - obstruction.track) > 0.35F) {
        continue;
      }

      obstructions.remove(i--);
      if (obstruction.passenger) {
        passengersAboard++;
        pickupFlash = 14;
        // Passengers are worth more the faster they are collected.
        addScore(50 + (int) ((speed - MIN_SPEED) / (MAX_SPEED - MIN_SPEED) * 150F));
      } else {
        derail();
        return;
      }
    }
  }

  /**
   * Places new hazards and passengers at the horizon, keeping at least one track clear so the run is
   * always survivable.
   *
   * @since 1.0
   */
  private void spawnAhead() {
    if (--spawnTimer > 0) {
      return;
    }
    // Spacing shrinks with speed, so the throttle really is the difficulty dial.
    float speedFraction = (speed - MIN_SPEED) / (MAX_SPEED - MIN_SPEED);
    spawnTimer = (int) (TICK_RATE * (1.25F - speedFraction * 0.7F));
    spawnTimer = Math.max(TICK_RATE / 4, spawnTimer);

    // Never block more than one track at a time; hazards are to be dodged, not survived.
    int hazardTrack = random.nextInt(TRACKS);
    Obstruction hazard = new Obstruction();
    hazard.track = hazardTrack;
    hazard.depth = SPAWN_DEPTH;
    obstructions.add(hazard);

    if (random.nextInt(3) != 0) {
      int passengerTrack;
      do {
        passengerTrack = random.nextInt(TRACKS);
      } while (passengerTrack == hazardTrack);
      Obstruction passenger = new Obstruction();
      passenger.track = passengerTrack;
      passenger.depth = SPAWN_DEPTH;
      passenger.passenger = true;
      obstructions.add(passenger);
    }
  }

  /**
   * Pays the standing award for distance covered, scaled by speed.
   *
   * @since 1.0
   */
  private void payDistance() {
    distanceAccumulator += speed * 400F;
    while (distanceAccumulator >= 1F) {
      distanceAccumulator -= 1F;
      addScore(1);
    }
  }

  /**
   * Derails the train, ending the run if that was the last one.
   *
   * @since 1.0
   */
  private void derail() {
    // Everyone aboard is lost with the train; the count starts again.
    passengersAboard = 0;
    if (!loseLife()) {
      respawnTimer = RESPAWN_STEPS;
    }
  }

  /**
   * Retrieves the screen X coordinate of a track at the specified depth.
   *
   * @param track the track index, which may be fractional mid-switch
   * @param depth the depth: 1 at the horizon, 0 at the cab
   *
   * @return the X coordinate, in playfield units
   *
   * @since 1.0
   */
  private static float trackX(float track, float depth) {
    float centred = track - (TRACKS - 1) * 0.5F;
    // Perspective: everything converges on the vanishing point as depth approaches one.
    return FIELD_WIDTH * 0.5F + centred * TRACK_SPREAD * perspective(depth);
  }

  /**
   * Retrieves the screen Y coordinate of the track bed at the specified depth.
   *
   * @param depth the depth: 1 at the horizon, 0 at the cab
   *
   * @return the Y coordinate, in playfield units
   *
   * @since 1.0
   */
  private static float trackY(float depth) {
    return VANISHING_Y + (FIELD_HEIGHT - VANISHING_Y) * perspective(depth);
  }

  /**
   * Retrieves the perspective scale at the specified depth, falling off toward the horizon.
   *
   * @param depth the depth: 1 at the horizon, 0 at the cab
   *
   * @return the scale factor, 1 at the cab and near zero at the horizon
   *
   * @since 1.0
   */
  private static float perspective(float depth) {
    return 1F / (1F + Math.max(0F, depth) * 7F);
  }

  /**
   * Draws the tunnel, the track, everything on it, and the train.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  @Override
  public void render(ArcadeScreen screen) {
    screen.rect(0F, 0F, FIELD_WIDTH, FIELD_HEIGHT, 0xFF07090C);

    drawTunnelRings(screen);
    drawRails(screen);

    // Draw far to near so nearer objects overlap the ones behind them.
    List<Obstruction> sorted = new ArrayList<>(obstructions);
    sorted.sort((a, b) -> Float.compare(b.depth, a.depth));
    for (Obstruction obstruction : sorted) {
      drawObstruction(screen, obstruction);
    }

    if (respawnTimer == 0 && introTimer == 0) {
      drawTrain(screen);
    } else if (respawnTimer > 0) {
      screen.textCentered("DERAILED", FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.45F, 0xFFFF6060);
    } else {
      screen.textCentered("DEPART", FIELD_WIDTH * 0.5F, FIELD_HEIGHT * 0.45F, 0xFF60D0FF);
    }

    drawSpeedometer(screen);

    if (pickupFlash > 0) {
      screen.textCentered("PASSENGER ABOARD", FIELD_WIDTH * 0.5F, VANISHING_Y + 14F, 0xFF60FFA0);
    }
  }

  /**
   * Draws the tunnel lining as a run of rings receding to the vanishing point. The rings scroll with
   * the train, which is what sells the sense of speed.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawTunnelRings(ArcadeScreen screen) {
    // Rings sit at fixed intervals in depth and slide toward the camera over time.
    float phase = (ticks * speed) % 0.1F;
    for (int i = 0; i < 12; i++) {
      float depth = i * 0.1F - phase;
      if (depth < 0.001F) {
        continue;
      }
      float scale = perspective(depth);
      float halfWidth = FIELD_WIDTH * 0.62F * scale;
      float top = VANISHING_Y - 46F * scale * 4F;
      float bottom = trackY(depth);
      float centerX = FIELD_WIDTH * 0.5F;
      // Dim the rings toward the horizon so the tunnel has depth.
      int brightness = (int) (40 + 150 * scale);
      int color = 0xFF000000 | (brightness / 3 << 16) | (brightness / 2 << 8) | brightness;
      screen.line(centerX - halfWidth, bottom, centerX - halfWidth * 0.9F, top, color);
      screen.line(centerX + halfWidth, bottom, centerX + halfWidth * 0.9F, top, color);
      screen.line(centerX - halfWidth * 0.9F, top, centerX + halfWidth * 0.9F, top, color);
      screen.line(centerX - halfWidth, bottom, centerX + halfWidth, bottom, color);
    }
  }

  /**
   * Draws the three tracks and their sleepers.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawRails(ArcadeScreen screen) {
    for (int track = 0; track < TRACKS; track++) {
      float nearX = trackX(track, 0F);
      float farX = trackX(track, 1F);
      float nearY = trackY(0F);
      float farY = trackY(1F);
      float nearGauge = 11F;
      float farGauge = nearGauge * perspective(1F);
      int color = track == targetTrack ? 0xFF6098C0 : 0xFF3A4A58;
      screen.line(nearX - nearGauge, nearY, farX - farGauge, farY, color);
      screen.line(nearX + nearGauge, nearY, farX + farGauge, farY, color);
    }

    float phase = (ticks * speed) % 0.05F;
    for (int i = 0; i < 24; i++) {
      float depth = i * 0.05F - phase;
      if (depth < 0.001F) {
        continue;
      }
      float y = trackY(depth);
      float scale = perspective(depth);
      float halfWidth = (TRACK_SPREAD * (TRACKS - 1) * 0.5F + 14F) * scale;
      int brightness = (int) (30 + 70 * scale);
      screen.rect(FIELD_WIDTH * 0.5F - halfWidth, y, halfWidth * 2F, Math.max(0.5F, 2F * scale),
          0xFF000000 | (brightness << 16) | (brightness << 8) | (brightness / 2));
    }
  }

  /**
   * Draws one hazard or waiting passenger, sized by how far away it is.
   *
   * @param screen      the screen to draw on
   * @param obstruction the obstruction to draw
   *
   * @since 1.0
   */
  private void drawObstruction(ArcadeScreen screen, Obstruction obstruction) {
    float depth = obstruction.depth;
    if (depth < -0.05F || depth > SPAWN_DEPTH) {
      return;
    }
    float scale = perspective(depth);
    float x = trackX(obstruction.track, depth);
    float y = trackY(depth);

    if (obstruction.passenger) {
      // A figure on the platform edge, waving a lamp.
      float height = 26F * scale;
      float width = 9F * scale;
      screen.rect(x - width * 0.5F, y - height, width, height * 0.66F, 0xFF40E0A0);
      screen.dot(x, y - height - height * 0.16F, height * 0.17F, 0xFFD8F0E0);
      int lampColor = (ticks / 6) % 2 == 0 ? 0xFFFFE060 : 0xFFA08020;
      screen.dot(x + width * 0.8F, y - height * 0.55F, Math.max(0.6F, height * 0.1F), lampColor);
      return;
    }

    // A maintenance trolley left on the rails, with a hazard stripe.
    float height = 20F * scale;
    float width = 26F * scale;
    screen.rect(x - width * 0.5F, y - height, width, height, 0xFF803020);
    for (int stripe = 0; stripe < 3; stripe++) {
      screen.rect(x - width * 0.5F + stripe * width / 3F, y - height, width / 6F, height,
          0xFFFFC020);
    }
    screen.rect(x - width * 0.5F, y - height, width, Math.max(0.6F, 2F * scale), 0xFFE06040);
  }

  /**
   * Draws the train's cab in the foreground.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawTrain(ArcadeScreen screen) {
    float x = trackX(trainTrack, 0F);
    float y = trackY(0F);
    float halfWidth = 30F;
    float height = 44F;

    screen.rect(x - halfWidth, y - height, halfWidth * 2F, height, 0xFF2E4C6E);
    screen.rect(x - halfWidth + 3F, y - height + 3F, halfWidth * 2F - 6F, height * 0.42F,
        0xFF9CD8FF);
    screen.rect(x - halfWidth, y - height, halfWidth * 2F, 3F, 0xFF6A9CC8);
    // Headlights, throwing a little light down the track.
    screen.dot(x - halfWidth * 0.62F, y - height * 0.24F, 3.4F, 0xFFFFF4C0);
    screen.dot(x + halfWidth * 0.62F, y - height * 0.24F, 3.4F, 0xFFFFF4C0);
    // Route number board.
    screen.rect(x - 9F, y - height * 0.34F, 18F, 8F, 0xFF101418);
    screen.textCentered(String.valueOf(passengersAboard), x, y - height * 0.34F, 0xFFFFC040);
  }

  /**
   * Draws the throttle readout across the top of the screen.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  private void drawSpeedometer(ArcadeScreen screen) {
    float fraction = (speed - MIN_SPEED) / (MAX_SPEED - MIN_SPEED);
    screen.rect(8F, 8F, FIELD_WIDTH - 16F, 5F, 0xFF1C2630);
    int color = fraction > 0.8F ? 0xFFFF6040 : (fraction > 0.5F ? 0xFFFFC040 : 0xFF40C0FF);
    screen.rect(8F, 8F, (FIELD_WIDTH - 16F) * fraction, 5F, color);
    screen.text("THROTTLE", 8F, 15F, 0xFF60748A);
    String riders = "RIDERS " + passengersAboard;
    screen.text(riders, FIELD_WIDTH - 8F - riders.length() * 6F, 15F, 0xFF60748A);
  }
}
