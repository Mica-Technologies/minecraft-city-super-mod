package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;

/**
 * What an outdoor warning siren sounds, and for how long. The ordinal is saved on the sirens and
 * their controller: append, never reorder.
 *
 * @since 2026.9
 */
public enum SirenSignal {
  /** Silent. */
  NONE(null, 0, false),
  /** Alert: a steady tone. */
  ALERT(LifeSafetySounds.SIREN_STEADY, 1800, true),
  /** Attack: the rising and falling wail. */
  ATTACK(LifeSafetySounds.SIREN_WAIL, 1800, true),
  /** Fire: hi-lo, calling volunteers in. */
  FIRE(LifeSafetySounds.SIREN_HILO, 900, true),
  /** The weekly test: one short growl, up and down. */
  TEST(LifeSafetySounds.SIREN_GROWL, 260, false);

  /** The sound, or null for {@link #NONE}. */
  public final LifeSafetySounds sound;
  /** How long a signal sounds, in ticks, before the siren stops by itself. */
  public final int ticks;
  /** Whether the sound loops for as long as the signal lasts, or plays once. */
  public final boolean loops;

  SirenSignal(LifeSafetySounds sound, int ticks, boolean loops) {
    this.sound = sound;
    this.ticks = ticks;
    this.loops = loops;
  }

  public static SirenSignal of(int ordinal) {
    SirenSignal[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
  }
}
