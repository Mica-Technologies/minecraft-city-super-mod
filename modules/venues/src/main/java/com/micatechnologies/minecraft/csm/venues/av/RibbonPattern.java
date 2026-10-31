package com.micatechnologies.minecraft.csm.venues.av;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * An LED ribbon board's pattern: a row of the one animated ribbon texture, which
 * {@code gen_venues_av.py} draws in this order. Stored in metadata bits 2-3, so only appended
 * to, never reordered.
 *
 * @since 2026.10
 */
public enum RibbonPattern implements IStringSerializable {
  /** Navy fading into ice blue, a white head sweeping along it. */
  TEAM_NAVY("team_navy"),
  /** Crimson and gold chevrons marching. */
  TEAM_CRIMSON("team_crimson"),
  /** Green with white pulses chasing through it. */
  TEAM_GREEN("team_green"),
  /** LET'S GO crawling in white on black. */
  WHITE_CRAWL("white_crawl");

  private final String name;

  RibbonPattern(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  /** The pattern at an index, wrapped into range. */
  public static RibbonPattern of(int index) {
    return values()[index & 3];
  }
}
