package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingIcon;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingMount;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingScheme;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * The arena wayfinding blades' default signs and preset lists. Both blades are Core's wayfinding
 * panel, so every option (lines, scale, mount, colours) and the whole NBT are the panel's; only
 * the defaults and presets are the blades' own.
 *
 * @since 2026.10
 */
public final class ExteriorSignPresets {

  /** The gate blade's presets: the gate letters, and the entries a gate also marks. */
  public static final WayfindingPreset[] GATE = {
      gate("A"), gate("B"), gate("C"), gate("D"), gate("E"), gate("F"),
      new WayfindingPreset("BOX", "OFFICE", WayfindingIcon.TICKET, null,
          WayfindingScheme.METRO),
      new WayfindingPreset("WILL", "CALL", WayfindingIcon.TICKET, null, WayfindingScheme.METRO),
      new WayfindingPreset("TEAM", "STORE", WayfindingIcon.SHOP, null, WayfindingScheme.METRO),
      new WayfindingPreset("ENTRY", "", WayfindingIcon.NONE, null, WayfindingScheme.METRO),
  };

  /** The section blade's presets: the section ranges a portal or a gate leads to. */
  public static final WayfindingPreset[] SECTION = {
      new WayfindingPreset("SECTIONS", "101 - 112", WayfindingIcon.NONE, WayfindingArrow.UP,
          WayfindingScheme.METRO),
      new WayfindingPreset("SECTIONS", "113 - 124", WayfindingIcon.NONE, WayfindingArrow.RIGHT,
          WayfindingScheme.METRO),
      new WayfindingPreset("SECTIONS", "301 - 315", WayfindingIcon.STAIRS, WayfindingArrow.UP,
          WayfindingScheme.METRO),
      new WayfindingPreset("SUITES", "CLUB LEVEL", WayfindingIcon.ELEVATOR, null,
          WayfindingScheme.METRO),
      new WayfindingPreset("ALL GATES", "", WayfindingIcon.NONE, null, WayfindingScheme.METRO),
      new WayfindingPreset("ACCESSIBLE SEATING", "", WayfindingIcon.ACCESSIBLE, null,
          WayfindingScheme.METRO),
  };

  /**
   * The gate blade's default: GATE over a big A, centred, both sides, lit, hung (from a blade
   * bracket or a ceiling).
   */
  static final WayfindingSign GATE_DEFAULT = GATE[0].toSign(WayfindingArrow.NONE, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).lineScale(0, 0.5f)
      .mount(WayfindingMount.HUNG).doubleSided(true).build();

  /** The section blade's default: SECTIONS 101 - 112 with an arrow up, both sides, lit, hung. */
  static final WayfindingSign SECTION_DEFAULT = SECTION[0].toSign(WayfindingArrow.UP, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).mount(WayfindingMount.HUNG)
      .doubleSided(true).build();

  private static WayfindingPreset gate(String letter) {
    return new WayfindingPreset("GATE", letter, WayfindingIcon.NONE, WayfindingArrow.NONE,
        WayfindingScheme.METRO);
  }

  private ExteriorSignPresets() {
  }
}
