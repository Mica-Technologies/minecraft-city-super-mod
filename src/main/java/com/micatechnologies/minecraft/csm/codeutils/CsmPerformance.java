package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConfig;

/**
 * The performance mode: what CSM draws, how far, and how much memory it lets Minecraft keep, as
 * every renderer and the chunk builder upkeep read it. One place, so a mode means the same thing
 * everywhere and a new knob has one table to join.
 *
 * <p>{@code HIGH} is the default and is exactly how CSM behaved before the mode existed, so a
 * player who never touches it sees no change. {@code MEDIUM} and {@code LOW} trim the extras that
 * cost frame time or memory; {@code CUSTOM} takes every value from its own configuration entry.</p>
 *
 * <p>The switches in the configuration's general category ({@code enableStrobeEffect},
 * {@code animateDoors}, {@code enableThermostatDisplay}) and {@code trimChunkBuilders} combine
 * with the presets so that they can only ever turn something off: a player who turned strobes off
 * because flashing light bothers them keeps them off on {@code HIGH}, and no preset turns a switch
 * back on. On {@code CUSTOM} the switches decide on their own.</p>
 *
 * <p>Everything here is a client matter, read every frame or tick, so a change applies at once,
 * whether from {@code /csmclient performance} or {@code /csm reloadconfig}.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class CsmPerformance {

  /** The modes, in the order {@code /csmclient performance} lists them. */
  public enum Mode {
    HIGH, MEDIUM, LOW, CUSTOM
  }

  /** How much of a fire alarm strobe's flash is drawn. */
  public enum StrobeDetail {
    /** The lens, its halo, the beam, and the light it throws on the wall behind and on surfaces. */
    FULL,
    /** The lens, its halo and the beam; no light on surfaces (the raycasts are the costly part). */
    CONE,
    /** The lens and its halo alone. */
    LENS
  }

  private CsmPerformance() {
  }

  /**
   * The mode in force.
   *
   * @return the mode
   */
  public static Mode mode() {
    // Read per block per frame: parse the name only when the configuration has changed
    int version = CsmConfig.getConfigVersion();
    if (version != seenVersion) {
      cachedMode = Mode.valueOf(CsmConfig.getPerformanceModeName());
      seenVersion = version;
    }
    return cachedMode;
  }

  private static volatile Mode cachedMode = Mode.HIGH;
  private static volatile int seenVersion = Integer.MIN_VALUE;

  private static boolean custom() {
    return mode() == Mode.CUSTOM;
  }

  /**
   * Caps a block's own render distance by the mode's: unchanged on {@code HIGH}, at most 96
   * blocks on {@code MEDIUM} and 64 on {@code LOW}, which is Minecraft's own default for blocks
   * that draw themselves. For {@code getMaxRenderDistanceSquared}.
   *
   * @param ownSquared the block's own distance, squared
   *
   * @return the distance to use, squared
   */
  public static double capRenderDistanceSq(double ownSquared) {
    int cap;
    switch (mode()) {
      case MEDIUM:
        cap = 96;
        break;
      case LOW:
        cap = 64;
        break;
      case CUSTOM:
        cap = CsmConfig.getCustomMaxRenderDistance();
        break;
      default:
        cap = 0;
    }
    return cap <= 0 ? ownSquared : Math.min(ownSquared, (double) cap * cap);
  }

  /**
   * Within this distance, squared, guide and street signs draw their legends; farther, only the
   * blank sign. 64 blocks on {@code HIGH}.
   *
   * @return the distance, squared
   */
  public static double signDetailDistanceSq() {
    double d;
    switch (mode()) {
      case MEDIUM:
        d = 48;
        break;
      case LOW:
        d = 24;
        break;
      case CUSTOM:
        d = CsmConfig.getCustomSignDetailDistance();
        break;
      default:
        d = 64;
    }
    return d * d;
  }

  /**
   * Within this distance, squared, an arrow board's lit lamps glow; 0 for never. 48 blocks on
   * {@code HIGH}. The strobe switch can still turn the glow off.
   *
   * @return the distance, squared
   */
  public static double arrowBoardHaloDistanceSq() {
    if (!strobeEffect()) {
      return 0;
    }
    double d;
    switch (mode()) {
      case MEDIUM:
        d = 32;
        break;
      case LOW:
        d = 0;
        break;
      case CUSTOM:
        d = CsmConfig.getCustomArrowBoardHaloDistance();
        break;
      default:
        d = 48;
    }
    return d * d;
  }

  /**
   * Whether flashing effects are drawn at all: strobes, beacons, barricade lamps, emergency light
   * glow. The {@code enableStrobeEffect} switch.
   *
   * @return whether to draw them
   */
  public static boolean strobeEffect() {
    return CsmConfig.isStrobeEffectEnabled();
  }

  /**
   * How much of a fire alarm strobe's flash to draw.
   *
   * @return the detail
   */
  public static StrobeDetail strobeDetail() {
    switch (mode()) {
      case MEDIUM:
        return StrobeDetail.CONE;
      case LOW:
        return StrobeDetail.LENS;
      case CUSTOM:
        return StrobeDetail.valueOf(CsmConfig.getCustomStrobeDetail());
      default:
        return StrobeDetail.FULL;
    }
  }

  /**
   * Whether lit emergency lights cast their glow. Off on {@code LOW}; the strobe switch can also
   * turn it off.
   *
   * @return whether to draw the glow
   */
  public static boolean emergencyLightGlow() {
    if (!strobeEffect()) {
      return false;
    }
    switch (mode()) {
      case LOW:
        return false;
      case CUSTOM:
        return CsmConfig.isCustomEmergencyLightGlow();
      default:
        return true;
    }
  }

  /**
   * Within this distance, squared, thermostats show their live screen: any distance on
   * {@code HIGH}, 24 blocks on {@code MEDIUM}, none on {@code LOW}. The
   * {@code enableThermostatDisplay} switch can turn it off everywhere.
   *
   * @return the distance, squared; {@link Double#MAX_VALUE} for any, 0 for none
   */
  public static double thermostatDisplayDistanceSq() {
    if (!CsmConfig.isThermostatDisplayEnabled()) {
      return 0;
    }
    switch (mode()) {
      case MEDIUM:
        return 24.0 * 24.0;
      case LOW:
        return 0;
      case CUSTOM: {
        int d = CsmConfig.getCustomThermostatDisplayDistance();
        return d <= 0 ? Double.MAX_VALUE : (double) d * d;
      }
      default:
        return Double.MAX_VALUE;
    }
  }

  /**
   * Whether doors swing, rather than snap. Off on {@code LOW}; the {@code animateDoors} switch can
   * turn it off everywhere.
   *
   * @return whether to animate
   */
  public static boolean doorAnimation() {
    if (!CsmConfig.isDoorAnimationEnabled()) {
      return false;
    }
    return mode() != Mode.LOW;
  }

  /**
   * Whether incandescent signal lamps fade like a filament, rather than switch like an LED. Off
   * on {@code LOW}.
   *
   * @return whether to fade
   */
  public static boolean incandescentFade() {
    switch (mode()) {
      case LOW:
        return false;
      case CUSTOM:
        return CsmConfig.isCustomIncandescentFade();
      default:
        return true;
    }
  }

  /**
   * Whether advertising boards fade or scroll between ads, rather than cut. Off on {@code LOW}.
   *
   * @return whether to animate the change
   */
  public static boolean adBoardTransitions() {
    switch (mode()) {
      case LOW:
        return false;
      case CUSTOM:
        return CsmConfig.isCustomAdBoardTransitions();
      default:
        return true;
    }
  }

  /**
   * Whether the chunk builders' grown buffers are given back when over budget. On in every
   * preset; the {@code trimChunkBuilders} switch can turn it off, and decides alone on
   * {@code CUSTOM}.
   *
   * @return whether to trim
   */
  public static boolean trimChunkBuilders() {
    return CsmConfig.isChunkBuilderTrimEnabled();
  }

  /**
   * The share of the direct memory limit the chunk builders may hold before some is given back:
   * 40% on {@code HIGH}, 30% on {@code MEDIUM}, 25% on {@code LOW}.
   *
   * @return a percentage
   */
  public static int chunkBuilderBudgetPercent() {
    switch (mode()) {
      case MEDIUM:
        return 30;
      case LOW:
        return 25;
      case CUSTOM:
        return CsmConfig.getChunkBuilderBudgetPercent();
      default:
        return 40;
    }
  }

  /**
   * The most chunk builders to keep, or 0 for Minecraft's own number: Minecraft's on
   * {@code HIGH}, four per build thread on {@code MEDIUM}, two on {@code LOW}. The
   * {@code chunkBuilderLimit} entry caps it further on every level, as a switch would (a pack can
   * ship it to give back builders OptiFine never uses without changing anyone's visuals).
   *
   * @param buildThreads the chunk build threads
   *
   * @return the limit, 0 for none
   */
  public static int chunkBuilderLimit(int buildThreads) {
    int preset;
    switch (mode()) {
      case MEDIUM:
        preset = buildThreads * 4;
        break;
      case LOW:
        preset = buildThreads * 2;
        break;
      default:
        preset = 0;
    }
    int entry = CsmConfig.getChunkBuilderLimit();
    if (preset <= 0) {
      return Math.max(0, entry);
    }
    return entry <= 0 ? preset : Math.min(preset, entry);
  }
}
