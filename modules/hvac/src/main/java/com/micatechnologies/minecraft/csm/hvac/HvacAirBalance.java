package com.micatechnologies.minecraft.csm.hvac;

/**
 * Supply against exhaust in a room (#314): the airflow book-keeping behind exhaust grilles and
 * exhaust fans, and the one way it reaches the heat balance.
 *
 * <p>Air is counted in cubic feet a minute (CFM). While a system runs (it is not switched off
 * and at least one of its units has power), each of its vents supplies {@link #VENT_CFM}; each of
 * its exhaust grilles draws out an equal share of its powered exhaust fans' capacity, up to
 * {@link #GRILLE_CFM}. A room whose grilles draw out more than its vents bring in is under
 * negative pressure; one with more supply than exhaust (any room with vents and no grilles) is
 * positive, as offices are.</p>
 *
 * <p><b>The heat it costs.</b> The vents' air is conditioned, and the heat it carries is already
 * what the system delivers. The air a negative room draws in past its doors to make up the
 * difference is not: it is counted as outdoor air leaking in, a conductance to the outdoors of
 * the shortfall in blocks of air a second ({@link #infiltrationUA}), one air-filled cell holding
 * one unit of heat per degree. A grille's worth of imbalance (500 CFM) adds about a quarter to a
 * 10 x 4 x 10 room's loss. Positive pressure pushes conditioned air out, which costs nothing
 * here.</p>
 *
 * <p>Pure functions, so the rules are unit tested without a world.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
final class HvacAirBalance {

  /** Air a vent supplies while its system runs, CFM. */
  static final float VENT_CFM = 300.0f;

  /** The most air one exhaust grille draws out, CFM. */
  static final float GRILLE_CFM = 500.0f;

  /** CFM in one block of air a second: 35.3 cubic feet a block, sixty seconds a minute. */
  static final float CFM_PER_BLOCK_PER_SECOND = 35.3147f * 60.0f;

  /**
   * A room is neutral when supply and exhaust are within this many CFM of each other, or within
   * {@link #NEUTRAL_SHARE} of the larger of them.
   */
  static final float NEUTRAL_CFM = 25.0f;

  /** See {@link #NEUTRAL_CFM}. */
  static final float NEUTRAL_SHARE = 0.05f;

  static final int NEGATIVE = -1;
  static final int NEUTRAL = 0;
  static final int POSITIVE = 1;

  private HvacAirBalance() {
  }

  /**
   * Air each of a system's exhaust grilles draws out.
   *
   * @param fanCfm  the capacity of the system's powered exhaust fans, CFM
   * @param grilles the system's exhaust grilles in a room
   *
   * @return CFM per grille
   */
  static float grilleCfm(float fanCfm, int grilles) {
    if (grilles <= 0 || fanCfm <= 0) {
      return 0.0f;
    }
    return Math.min(GRILLE_CFM, fanCfm / grilles);
  }

  /**
   * The pressure of a room relative to what is around it.
   *
   * @return {@link #NEGATIVE}, {@link #NEUTRAL} or {@link #POSITIVE}
   */
  static int pressure(float supplyCfm, float exhaustCfm) {
    float d = supplyCfm - exhaustCfm;
    float band = Math.max(NEUTRAL_CFM, NEUTRAL_SHARE * Math.max(supplyCfm, exhaustCfm));
    if (Math.abs(d) <= band) {
      return NEUTRAL;
    }
    return d < 0 ? NEGATIVE : POSITIVE;
  }

  /**
   * The conductance to the outdoors of the air a room draws in to make up an exhaust its supply
   * does not: per degree, in the simulation's units.
   */
  static float infiltrationUA(float supplyCfm, float exhaustCfm) {
    float shortfall = exhaustCfm - supplyCfm;
    return shortfall > 0 ? shortfall / CFM_PER_BLOCK_PER_SECOND : 0.0f;
  }

  /** How {@code /csmhvac info} words a room's balance. */
  static String describe(float supplyCfm, float exhaustCfm) {
    int p = pressure(supplyCfm, exhaustCfm);
    int d = Math.round(supplyCfm - exhaustCfm);
    String what = p == NEGATIVE ? "negative pressure" : p == POSITIVE ? "positive pressure"
        : "neutral";
    return String.format("air: supply %d CFM, exhaust %d CFM: %s (%s%d CFM)",
        Math.round(supplyCfm), Math.round(exhaustCfm), what, d > 0 ? "+" : "", d);
  }
}
