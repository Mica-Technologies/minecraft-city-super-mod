package com.micatechnologies.minecraft.csm.hvac;

/**
 * Calling modes and status flags a thermostat reports, shared by the controller that sets them,
 * the tile entities that sync them and the screens that explain them.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class HvacStatus {

  public static final int MODE_IDLE = 0;
  public static final int MODE_HEATING = 1;
  public static final int MODE_COOLING = 2;

  /**
   * A thermostat's switch, as on a real one. Auto heats or cools to hold the range (what every
   * thermostat did before the switch existed, and still the default); Heat and Cool do only that;
   * Off does nothing. A primary's switch governs its whole system, a zone's only its own rooms.
   */
  public static final int SWITCH_AUTO = 0;
  public static final int SWITCH_HEAT = 1;
  public static final int SWITCH_COOL = 2;
  public static final int SWITCH_OFF = 3;
  /** The switch positions' names, by value. */
  public static final String[] SWITCH_NAMES = {"Auto", "Heat", "Cool", "Off"};

  /** The thermostat is not in an enclosed room (it sees the sky, or the room has a hole). */
  public static final int FLAG_NOT_ENCLOSED = 1;
  /** The thermostat's room is too large to condition (a cave, a vast open hall). */
  public static final int FLAG_TOO_LARGE = 1 << 1;
  /** A ducted system, and this thermostat has no vents of its own. */
  public static final int FLAG_NO_VENTS = 1 << 2;
  /** This thermostat's vents are all in other rooms than the one it reads. */
  public static final int FLAG_ROOM_NOT_SERVED = 1 << 3;
  /** A zone thermostat not linked to a primary. */
  public static final int FLAG_NO_PRIMARY = 1 << 4;
  /** A zone that wants the opposite of what the system is doing, waiting its turn. */
  public static final int FLAG_WAITING = 1 << 5;
  /** Heating or cooling is wanted and linked, but no unit of that kind has power. */
  public static final int FLAG_NO_POWER = 1 << 6;
  /** The system is delivering all it can and it is not enough. */
  public static final int FLAG_CAPACITY_LIMITED = 1 << 7;
  /** No vents at all, and some units are not in any room (a rooftop unit needs vents). */
  public static final int FLAG_UNITS_UNCONNECTED = 1 << 8;
  /** Part of the room is in an unloaded chunk; the reading is the last one saved. */
  public static final int FLAG_WAITING_FOR_CHUNKS = 1 << 9;
  /** A zone whose primary thermostat is switched off, so its system is off. */
  public static final int FLAG_SYSTEM_OFF = 1 << 10;

  /** Whether a switch position lets its thermostat heat. */
  public static boolean switchHeats(int switchMode) {
    return switchMode == SWITCH_AUTO || switchMode == SWITCH_HEAT;
  }

  /** Whether a switch position lets its thermostat cool. */
  public static boolean switchCools(int switchMode) {
    return switchMode == SWITCH_AUTO || switchMode == SWITCH_COOL;
  }

  private HvacStatus() {
  }
}
