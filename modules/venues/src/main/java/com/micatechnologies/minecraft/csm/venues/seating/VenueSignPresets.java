package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingIcon;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingMount;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingScheme;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * The section and portal signs' default signs and preset lists. Both blocks are Core's
 * wayfinding panel, so every option (lines, scale, mount, colours) and the whole NBT are the
 * panel's; only the defaults and presets are the bowl's own.
 *
 * @since 2026.10
 */
public final class VenueSignPresets {

  /** The section sign's presets: a painted section number, and the bowl's named levels. */
  public static final WayfindingPreset[] SECTION = {
      new WayfindingPreset("101", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("201", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("301", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("SECTION", "101", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("LOGE", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("CLUB", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("SUITES", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
  };

  /** The portal sign's presets: the sections a vomitory leads to, and the concourse signs. */
  public static final WayfindingPreset[] PORTAL = {
      new WayfindingPreset("SECTIONS", "101 - 104", WayfindingIcon.NONE, WayfindingArrow.UP,
          WayfindingScheme.TRANSIT),
      new WayfindingPreset("PORTAL 1", "SECTIONS 101 - 104", WayfindingIcon.NONE,
          WayfindingArrow.NONE, WayfindingScheme.TRANSIT),
      new WayfindingPreset("ACCESSIBLE SEATING", "", WayfindingIcon.ACCESSIBLE, null,
          WayfindingScheme.TRANSIT),
      new WayfindingPreset("CONCOURSE", "", WayfindingIcon.NONE, null, WayfindingScheme.TRANSIT),
      new WayfindingPreset("STAIRS", "UPPER LEVEL", WayfindingIcon.STAIRS, null,
          WayfindingScheme.TRANSIT),
      new WayfindingPreset("EXIT", "", WayfindingIcon.EXIT, null, WayfindingScheme.EXIT),
  };

  /** The section sign's default: 101 painted big, navy, flat on the wall over the portal. */
  static final WayfindingSign SECTION_DEFAULT = SECTION[0].toSign(WayfindingArrow.NONE, false)
      .toBuilder().align(WayfindingSign.Align.CENTRE).mount(WayfindingMount.WALL)
      .doubleSided(false).build();

  /** The portal sign's default: SECTIONS 101 - 104 with an arrow up, lit, hung or on the wall. */
  static final WayfindingSign PORTAL_DEFAULT = PORTAL[0].toSign(WayfindingArrow.UP, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).build();

  private VenueSignPresets() {
  }
}
