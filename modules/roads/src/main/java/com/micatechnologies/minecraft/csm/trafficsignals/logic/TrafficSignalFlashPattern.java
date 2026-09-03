package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * The flash pattern a signal head's flashing bulbs follow.
 *
 * <p>This replaces the older boolean "alternate flash" setting, which could only express the
 * first two entries here. {@link #OFF} and {@link #B} are the two halves of a wig-wag pair: one
 * head is lit exactly while the other is dark, so a pair of beacons set to OFF and B alternate.
 * {@link #C} is not part of that pair at all -- it is the rapid multi-pulse strobe the Barlo
 * safety beam fires, applied to the bulb itself.</p>
 *
 * <p>The ordinal is the persisted form, so entries must only ever be appended.</p>
 */
public enum TrafficSignalFlashPattern implements IStringSerializable {
  /** Standard flash: lit for the second half of each second. */
  OFF("off", "OFF (normal)"),
  /** Wig-wag counterpart of {@link #OFF}: lit for the first half of each second. */
  B("b", "B (wig-wag)"),
  /** Rapid strobe: five quick pulses, then a long dark gap, matching the Barlo safety beam. */
  C("c", "C (rapid strobe)");

  /**
   * Length of one full rapid-strobe cycle, in milliseconds. The burst occupies
   * {@link #RAPID_BURST_MILLIS} of it and the remainder is dark.
   */
  private static final long RAPID_CYCLE_MILLIS = 1300L;

  /** Length of the pulse burst at the start of each rapid-strobe cycle, in milliseconds. */
  private static final long RAPID_BURST_MILLIS = 500L;

  /** Length of a single pulse (and of the gap between pulses) within the burst, in milliseconds. */
  private static final long RAPID_PULSE_MILLIS = 50L;

  /** Length of one full standard flash cycle, in milliseconds. */
  private static final long FLASH_CYCLE_MILLIS = 1000L;

  /** The identifier used for serialization. */
  private final String name;

  /** The friendly name for display purposes. */
  private final String friendlyName;

  TrafficSignalFlashPattern(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  /**
   * Gets the flash pattern stored as the given ordinal, falling back to {@link #OFF} for a value
   * written by a future version (or otherwise out of range).
   *
   * @param ordinal the persisted ordinal
   *
   * @return the corresponding flash pattern
   */
  public static TrafficSignalFlashPattern fromNBT(int ordinal) {
    int finalOrdinal = ordinal;
    if (ordinal < 0 || ordinal >= values().length) {
      finalOrdinal = 0;
    }
    return values()[finalOrdinal];
  }

  /**
   * Migrates the pre-pattern boolean "alternate flash" setting. The boolean only ever meant
   * "flash on the opposite half-second", which is exactly {@link #B}.
   *
   * @param alternateFlash the legacy boolean value
   *
   * @return the equivalent flash pattern
   */
  public static TrafficSignalFlashPattern fromLegacyAlternateFlash(boolean alternateFlash) {
    return alternateFlash ? B : OFF;
  }

  /**
   * Whether the Barlo-style rapid strobe is lit at the given wall-clock time. Shared by
   * {@link #C} and by the Barlo safety beam strobe bars the pattern is modelled on, so the two
   * cannot drift apart.
   *
   * @param millis the wall-clock flash timer, in milliseconds
   *
   * @return {@code true} if the strobe is lit at that instant
   */
  public static boolean isRapidStrobeLit(long millis) {
    long t = Math.floorMod(millis, RAPID_CYCLE_MILLIS);
    return t < RAPID_BURST_MILLIS && (t / RAPID_PULSE_MILLIS) % 2L == 1L;
  }

  /**
   * Whether a flashing bulb following this pattern is lit at the given wall-clock time.
   *
   * @param millis the wall-clock flash timer, in milliseconds
   *
   * @return {@code true} if a flashing bulb is lit at that instant
   */
  public boolean isFlashLit(long millis) {
    switch (this) {
      case B:
        return Math.floorMod(millis, FLASH_CYCLE_MILLIS) < FLASH_CYCLE_MILLIS / 2L;
      case C:
        return isRapidStrobeLit(millis);
      case OFF:
      default:
        return Math.floorMod(millis, FLASH_CYCLE_MILLIS) >= FLASH_CYCLE_MILLIS / 2L;
    }
  }

  /**
   * Gets the friendly name for display purposes.
   *
   * @return the friendly name
   */
  public String getFriendlyName() {
    return friendlyName;
  }

  /**
   * Gets the next flash pattern in the sequence, wrapping back to the first.
   *
   * @return the next flash pattern
   */
  public TrafficSignalFlashPattern getNextPattern() {
    int nextOrdinal = ordinal() + 1;
    if (nextOrdinal >= values().length) {
      nextOrdinal = 0;
    }
    return values()[nextOrdinal];
  }

  /**
   * Converts this flash pattern to its ordinal value for NBT storage.
   *
   * @return the ordinal value
   */
  public int toNBT() {
    return ordinal();
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }
}
