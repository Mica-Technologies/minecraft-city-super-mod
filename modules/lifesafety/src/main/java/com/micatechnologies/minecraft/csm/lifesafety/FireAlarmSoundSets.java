package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Selectable alarm tone sets, shared by the appliances that offer more of them than a block's
 * metadata can hold and so keep the choice in a {@link TileEntityFireAlarmSoundIndex}.
 *
 * <p>A set belongs to a family of appliances rather than to any one of them: every Gentex GOS-based
 * unit offers the same five tones and every Simplex 4903 the same two, and a copy of the list per
 * block class is a copy that can drift out of step with the others. The Commander 3 and the
 * Commander 5 read one of these; the 4903s, horizontal and vertical, read the other.</p>
 */
public final class FireAlarmSoundSets {

  /** The Gentex GOS tones, in the order a block cycles through them. */
  public static final String[] GENTEX_GOS = {
      "csm:gentex_gos_code3",
      "csm:gentex_gos_code3_chime",
      "csm:gentex_gos_whoop",
      "csm:gentex_gos_continuous_chime",
      "csm:broken_gentex_gos"
  };

  /** Human-readable names for {@link #GENTEX_GOS}, in the same order. */
  public static final String[] GENTEX_GOS_NAMES = {
      "Code 3 Horn",
      "Code 3 Chime",
      "Whoop",
      "Continuous Chime",
      "Broken GOS"
  };

  /** The Simplex 4903's two tones, in the order a block cycles through them. */
  public static final String[] SIMPLEX_4903 = {
      "csm:4030code44",
      "csm:stahorn"
  };

  /** Human-readable names for {@link #SIMPLEX_4903}, in the same order. */
  public static final String[] SIMPLEX_4903_NAMES = {
      "Old",
      "New"
  };

  private FireAlarmSoundSets() {
  }
}
