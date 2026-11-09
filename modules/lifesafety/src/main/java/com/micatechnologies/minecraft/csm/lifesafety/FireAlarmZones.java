package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Fire alarm zones (#335): which notification appliances sound for which alarm.
 *
 * <p>A panel has zones 1 to {@link #MAX_ZONE}, plus zone 0, the general zone. Every linked device
 * carries one: an initiating device (pull station, detector) on its own
 * {@link TileEntityFireAlarmSensor}, an appliance in its panel's link data, since most appliances
 * are not tile entities. A link made before zones existed reads as zone 0, so an unzoned system
 * behaves exactly as it always did.</p>
 *
 * <ul>
 *   <li>An initiating device in zone 1-8 puts its own zone in alarm; one in zone 0 puts every zone
 *   in alarm (an unzoned device is a general alarm).</li>
 *   <li>A drill, redstone, a command and the external API put every zone in alarm.</li>
 *   <li>An appliance in zone 0 sounds for any alarm; one in zone 1-8 sounds while its zone is in
 *   alarm.</li>
 * </ul>
 *
 * <p>The zones in alarm are a bit mask, bit {@code n} for zone {@code n}; bit 0 is unused, so
 * {@link #ALL} is bits 1 to 8.</p>
 *
 * @since 2026.11
 */
public final class FireAlarmZones {

  /** The general zone: an appliance in it sounds for every alarm, a device in it alarms all. */
  public static final int GENERAL = 0;

  /** The highest zone number. */
  public static final int MAX_ZONE = 8;

  /** Every zone in alarm. */
  public static final int ALL = ((1 << (MAX_ZONE + 1)) - 1) & ~1;

  private FireAlarmZones() {
  }

  /**
   * A zone number brought into range; anything outside 0-8 (a hand-edited NBT value) is general.
   *
   * @param zone a zone number
   *
   * @return the zone, or {@link #GENERAL} if it is out of range
   */
  public static int clamp(int zone) {
    return zone < 0 || zone > MAX_ZONE ? GENERAL : zone;
  }

  /**
   * The zones a device in this zone puts in alarm.
   *
   * @param zone the initiating device's zone
   *
   * @return its own zone's bit, or {@link #ALL} for the general zone
   */
  public static int alarmMaskFor(int zone) {
    int z = clamp(zone);
    return z == GENERAL ? ALL : 1 << z;
  }

  /**
   * Whether an appliance in a zone sounds (or flashes) while these zones are in alarm.
   *
   * @param applianceZone the appliance's zone
   * @param alarmZones    the zones in alarm, as a mask
   *
   * @return {@code true} if it should sound
   */
  public static boolean sounds(int applianceZone, int alarmZones) {
    if ((alarmZones & ALL) == 0) {
      return false;
    }
    int z = clamp(applianceZone);
    return z == GENERAL || (alarmZones & (1 << z)) != 0;
  }

  /**
   * The zone after this one, for the linker's cycle: 0, 1, ... 8, 0.
   *
   * @param zone the current zone
   *
   * @return the next zone
   */
  public static int next(int zone) {
    int z = clamp(zone);
    return z >= MAX_ZONE ? GENERAL : z + 1;
  }

  /**
   * The zones in alarm as the panel display writes them: {@code ALL ZONES}, or {@code ZONES 1,3}.
   *
   * @param alarmZones the zones in alarm, as a mask
   *
   * @return the text, empty when no zone is in alarm
   */
  public static String describeMask(int alarmZones) {
    int mask = alarmZones & ALL;
    if (mask == 0) {
      return "";
    }
    if (mask == ALL) {
      return "ALL ZONES";
    }
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (int z = 1; z <= MAX_ZONE; z++) {
      if ((mask & (1 << z)) != 0) {
        sb.append(count++ == 0 ? "" : ",").append(z);
      }
    }
    return (count == 1 ? "ZONE " : "ZONES ") + sb;
  }

  /**
   * One zone as a player reads it: {@code zone 3}, or {@code zone 0 (general)}.
   *
   * @param zone the zone
   *
   * @return the text
   */
  public static String describeZone(int zone) {
    int z = clamp(zone);
    return z == GENERAL ? "zone 0 (general)" : "zone " + z;
  }
}
