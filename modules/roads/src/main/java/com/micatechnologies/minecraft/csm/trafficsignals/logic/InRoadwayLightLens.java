package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Which lens texture an in-roadway warning light is showing: dark, or one of the three flash
 * sequences on one of its two phases.
 *
 * <p>This exists as a single property rather than as "lit" plus "pattern" plus "phase" because
 * of how Forge's blockstate format resolves a property map: each property contributes its own
 * fragment and they are merged, so three independent properties cannot between them name one
 * texture that depends on all three. Folding the whole decision into one derived value keeps
 * the blockstate JSON a flat list of seven cases with nothing to get subtly wrong.</p>
 *
 * <p>Nothing configures this directly. It is computed in the block's {@code getActualState} from
 * the controller's colour, the tile entity's {@link InRoadwayLightPattern} and link mode, and
 * the block's own position.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public enum InRoadwayLightLens implements IStringSerializable {

  OFF("off"),
  RRFB_A("rrfb_a"),
  RRFB_B("rrfb_b"),
  WIG_WAG_A("wig_wag_a"),
  WIG_WAG_B("wig_wag_b"),
  FLASH_A("flash_a"),
  FLASH_B("flash_b");

  private final String name;

  InRoadwayLightLens(String name) {
    this.name = name;
  }

  /**
   * The lens for a pattern on a given phase.
   *
   * @param pattern the flash sequence
   * @param offbeat whether this fixture runs the second phase
   *
   * @return the lens to show
   */
  public static InRoadwayLightLens of(InRoadwayLightPattern pattern, boolean offbeat) {
    switch (pattern == null ? InRoadwayLightPattern.RRFB : pattern) {
      case WIG_WAG:
        return offbeat ? WIG_WAG_B : WIG_WAG_A;
      case FLASH:
        return offbeat ? FLASH_B : FLASH_A;
      case RRFB:
      default:
        return offbeat ? RRFB_B : RRFB_A;
    }
  }

  @Override
  public String getName() {
    return name;
  }
}
