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
 * safety beam fires, applied to the bulb itself. {@link #D} is C's wig-wag counterpart: the same
 * strobe delayed by the length of one burst, so a pair set to C and D fire alternating bursts
 * that never overlap. {@link #E} and {@link #F} are a third wig-wag pair, shaped exactly like
 * OFF and B but run at {@value #FAST_FLASH_HZ} Hz rather than 1 Hz.</p>
 *
 * <p>The ordinal is the persisted form, so entries must only ever be appended.</p>
 */
public enum TrafficSignalFlashPattern implements IStringSerializable {
  /** Standard flash: lit for the second half of each second. */
  OFF("off", "OFF (normal)"),
  /** Wig-wag counterpart of {@link #OFF}: lit for the first half of each second. */
  B("b", "B (wig-wag)"),
  /** Rapid strobe: five quick pulses, then a long dark gap, matching the Barlo safety beam. */
  C("c", "C (rapid strobe)"),
  /**
   * Wig-wag counterpart of {@link #C}: the same rapid strobe, delayed by one burst length so its
   * five pulses fall in the gap C leaves. C's burst runs 0-500 ms of the cycle and D's 500-1000
   * ms, leaving both dark for the rest of it, so two heads set to C and D alternate cleanly
   * rather than overlapping.
   */
  D("d", "D (alt strobe)"),
  /**
   * The standard flash sped up to {@value #FAST_FLASH_HZ} Hz: same even on/off duty cycle as
   * {@link #OFF}, lit for the second half of each cycle, but that many cycles to the second.
   */
  E("e", "E (fast flash)"),
  /**
   * Wig-wag counterpart of {@link #E}: the same fast flash lit for the first half of each cycle,
   * so a pair of heads set to E and F alternate the way OFF and B do at the slow rate.
   */
  F("f", "F (fast alt)");

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

  /** Rate of the fast flash shared by {@link #E} and {@link #F}, in flashes per second. */
  private static final int FAST_FLASH_HZ = 5;

  /**
   * Length of one full fast flash cycle, in milliseconds. Kept as a divisor of
   * {@link #FLASH_CYCLE_MILLIS} so the fast pair stays in step with the slow one at the top of
   * each second however {@link #FAST_FLASH_HZ} is retuned.
   */
  private static final long FAST_FLASH_CYCLE_MILLIS = FLASH_CYCLE_MILLIS / FAST_FLASH_HZ;

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
   * Whether the wig-wag counterpart of the rapid strobe is lit at the given wall-clock time: the
   * same burst shifted later by its own length, so it starts exactly as {@link #C}'s ends. Both
   * bursts fit inside the cycle with room to spare, so the two never overlap.
   *
   * @param millis the wall-clock flash timer, in milliseconds
   *
   * @return {@code true} if the offset strobe is lit at that instant
   */
  public static boolean isAltRapidStrobeLit(long millis) {
    return isRapidStrobeLit(millis - RAPID_BURST_MILLIS);
  }

  /**
   * Whether an even on/off flash of the given cycle length is lit at the given wall-clock time.
   * Both halves of a wig-wag pair go through here with the same cycle and opposite {@code
   * firstHalf}, which is what guarantees one is lit exactly while the other is dark.
   *
   * @param millis      the wall-clock flash timer, in milliseconds
   * @param cycleMillis the length of one full on/off cycle, in milliseconds
   * @param firstHalf   {@code true} to be lit for the first half of the cycle, {@code false} for
   *                    the second
   *
   * @return {@code true} if that flash is lit at that instant
   */
  private static boolean isEvenFlashLit(long millis, long cycleMillis, boolean firstHalf) {
    return (Math.floorMod(millis, cycleMillis) < cycleMillis / 2L) == firstHalf;
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
        return isEvenFlashLit(millis, FLASH_CYCLE_MILLIS, true);
      case C:
        return isRapidStrobeLit(millis);
      case D:
        return isAltRapidStrobeLit(millis);
      case E:
        return isEvenFlashLit(millis, FAST_FLASH_CYCLE_MILLIS, false);
      case F:
        return isEvenFlashLit(millis, FAST_FLASH_CYCLE_MILLIS, true);
      case OFF:
      default:
        return isEvenFlashLit(millis, FLASH_CYCLE_MILLIS, false);
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
