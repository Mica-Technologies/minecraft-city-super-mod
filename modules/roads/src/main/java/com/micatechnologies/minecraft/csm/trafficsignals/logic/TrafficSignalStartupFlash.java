package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * What a controller does when its power comes back, and how it leaves flash for steady operation
 * whenever it does: the decisions, kept apart from the tile entity so they can be tested without
 * a world.
 *
 * <p><b>Start-up flash.</b> A real cabinet does not come back from an outage showing whatever it
 * was showing when the lights went out. It flashes for a few seconds -- long enough for a driver
 * who was treating the dark intersection as an all-way stop to see that it is alive again -- and
 * then begins its cycle from the start. {@link #DURATION_TICKS} is that flash; NEMA controllers
 * call the setting start-up flash time. It runs in the cabinet's configured flash, yellow-red or
 * all-red, because that is how the cabinet is wired to flash whatever the reason.</p>
 *
 * <p><b>Leaving flash.</b> MUTCD Section 4D.31 gives the two ways a signal may go from flashing
 * to steady operation, by what it was flashing:</p>
 *
 * <ul>
 *   <li>from <em>yellow-red</em> flash, at the beginning of the major street green: the main
 *       street's flashing yellow becomes green and the side street's flashing red becomes steady
 *       red, with nothing in between. An all-red interval here would turn a flashing yellow --
 *       which drivers are rolling through -- into a red with no yellow change interval before
 *       it;</li>
 *   <li>from <em>all-red</em> flash, to steady red on every approach, then the greens. Everyone
 *       is already stopped, so the steady all-red costs nothing and marks the change.</li>
 * </ul>
 *
 * <p>Either way the main street is served first. In NORMAL mode that is circuit 1, the primary
 * circuit by convention everywhere in the controller; in ADVANCED it is the phases the ring
 * engine rests on, the coordinated ones.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class TrafficSignalStartupFlash {

  /**
   * How long a controller flashes after its power returns: five seconds, five flashes at the
   * flash mode's one-second cycle. Short beside a real cabinet's ten or so, because a player is
   * standing there watching it.
   */
  public static final long DURATION_TICKS = 100L;

  private TrafficSignalStartupFlash() {
  }

  /**
   * Whether a controller configured for {@code mode} flashes when its power returns. The modes
   * that run an intersection or a crossing do. A ramp meter, a detection-driven warning system
   * and a controller switched off do not: flashing a wrong-way or overheight warning at nobody
   * is a false alarm, and those modes are simply repainted.
   *
   * @param mode the configured mode
   *
   * @return whether it has a start-up flash
   */
  public static boolean appliesTo(TrafficSignalControllerMode mode) {
    return mode == TrafficSignalControllerMode.NORMAL
        || mode == TrafficSignalControllerMode.ADVANCED
        || mode == TrafficSignalControllerMode.REQUESTABLE;
  }

  /**
   * The world tick a start-up flash beginning now runs until, or 0 for a mode that has none.
   *
   * @param mode the configured mode
   * @param now  the world's total time
   *
   * @return the tick it ends on, 0 for none
   */
  public static long armedUntil(TrafficSignalControllerMode mode, long now) {
    return appliesTo(mode) ? now + DURATION_TICKS : 0L;
  }

  /**
   * Whether a start-up flash is still running.
   *
   * @param until what {@link #armedUntil} returned, 0 for none
   * @param now   the world's total time
   *
   * @return whether the controller should still be flashing
   */
  public static boolean isActive(long until, long now) {
    return until > 0L && now < until;
  }

  /**
   * Whether a change of operating mode is one that begins steady operation on the main street's
   * green, skipping NORMAL mode's opening all-red interval: leaving yellow-red flash, per MUTCD
   * 4D.31. ADVANCED has no opening all-red to skip, so there the question is only which phases
   * come up first, and the answer is the same for both kinds of flash.
   *
   * @param leaving     the operating mode being left
   * @param entering    the operating mode being entered
   * @param allRedFlash whether the cabinet flashes all-red rather than yellow-red
   *
   * @return whether steady operation begins on the main street's green
   */
  public static boolean resumesOnPrimaryGreen(TrafficSignalControllerMode leaving,
      TrafficSignalControllerMode entering, boolean allRedFlash) {
    if (leaving != TrafficSignalControllerMode.FLASH) {
      return false;
    }
    if (entering == TrafficSignalControllerMode.ADVANCED) {
      return true;
    }
    return entering == TrafficSignalControllerMode.NORMAL && !allRedFlash;
  }
}
