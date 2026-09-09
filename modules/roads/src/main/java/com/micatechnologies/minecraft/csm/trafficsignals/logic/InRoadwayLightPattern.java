package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * The flash sequences an in-roadway warning light can run.
 *
 * <p>Which one is right depends on what the fixture is paired with, and there is no single
 * answer: a row wired to an RRFB should match the beacon above it, a row at an ordinary
 * crosswalk is usually a plain flash, and a wig-wag reads better where the fixtures are far
 * enough apart to be seen as a pair. So it is a setting rather than a decision baked into the
 * block.</p>
 *
 * <p>Each pattern is an animated texture rather than render-time logic, which is what keeps
 * every fixture in a row in step — Minecraft advances texture animations from the global tick
 * counter. The strips are written by {@code dev-env-utils/scripts/gen_irwl_textures.py}, and the
 * RRFB sequence there is imported from the RRFB's own generator rather than restated, so the
 * beacon and the pavement cannot drift apart.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public enum InRoadwayLightPattern implements IStringSerializable {

  /** The RRFB's IA-21 sequence, for a row wired to a beacon on the post above. */
  RRFB("rrfb", "RRFB (IA-21)"),
  /** A slow alternation, so a pair of fixtures reads as a wig-wag. */
  WIG_WAG("wig_wag", "Wig-Wag"),
  /** A plain one-per-second flash, every fixture together. */
  FLASH("flash", "Flash");

  private final String name;
  private final String friendlyName;

  InRoadwayLightPattern(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  public static InRoadwayLightPattern fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return RRFB;
    }
    return values()[ordinal];
  }

  public int toNBT() {
    return ordinal();
  }

  public InRoadwayLightPattern getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  @Override
  public String getName() {
    return name;
  }
}
