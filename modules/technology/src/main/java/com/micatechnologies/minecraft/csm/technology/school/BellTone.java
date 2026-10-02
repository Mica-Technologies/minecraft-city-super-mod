package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.technology.TechnologySounds;

/**
 * What a scheduled bell sounds like. Linked speakers (and the controller itself) play the tone's
 * sound; linked hallway bells ring only for the two class-change signals, since a gong can make
 * no other sound. An announcement plays the chime and then speaks the bell's text through
 * Core's speech service to everyone in hearing range of a speaker.
 *
 * <p>The ordinal is saved and sent; only append.</p>
 *
 * @since 2026.10
 */
public enum BellTone {
  /** The bell's own ring, through the speakers and on the hallway bells. */
  BELL("Bell", TechnologySounds.SCHOOL_BELL_RING, true, false),
  /** An electronic class-change tone, and the hallway bells ring. */
  TONE("Tone", TechnologySounds.SCHOOL_TONE, true, false),
  /** A three-tone chime, speakers only. */
  CHIME("Chime", TechnologySounds.SCHOOL_CHIME, false, false),
  /** The chime, then the bell's text spoken. */
  ANNOUNCE("Announce", TechnologySounds.SCHOOL_CHIME, false, true);

  private final String label;
  private final TechnologySounds sound;
  private final boolean ringsBells;
  private final boolean speaks;

  BellTone(String label, TechnologySounds sound, boolean ringsBells, boolean speaks) {
    this.label = label;
    this.sound = sound;
    this.ringsBells = ringsBells;
    this.speaks = speaks;
  }

  /** The name the controller's screen shows. */
  public String getLabel() {
    return label;
  }

  /** The sound the speakers play. */
  public TechnologySounds getSound() {
    return sound;
  }

  /** Whether linked hallway bells ring for it. */
  public boolean ringsBells() {
    return ringsBells;
  }

  /** Whether it speaks the bell's text. */
  public boolean speaks() {
    return speaks;
  }

  /**
   * The tone with the given ordinal, or {@link #BELL} for one out of range (an old save, or a
   * hostile packet).
   *
   * @param ordinal the saved or sent ordinal
   *
   * @return the tone
   */
  public static BellTone byOrdinal(int ordinal) {
    BellTone[] all = values();
    return ordinal >= 0 && ordinal < all.length ? all[ordinal] : BELL;
  }

  /** The next tone round, for the screen's cycling button. */
  public BellTone next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
