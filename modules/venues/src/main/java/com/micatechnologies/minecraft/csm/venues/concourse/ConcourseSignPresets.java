package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingIcon;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingScheme;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * The concession sign's default and presets: the lit header legends over a stand, one colour
 * scheme each so a concourse of stands reads at a glance, and the concourse's own names. The
 * sign is Core's wayfinding panel, so every option and the whole NBT are the panel's.
 *
 * @since 2026.10
 */
public final class ConcourseSignPresets {

  /** The presets, in the editor's order. */
  public static final WayfindingPreset[] CONCESSION = {
      new WayfindingPreset("HOT DOGS", "", WayfindingIcon.FOOD, WayfindingArrow.NONE,
          WayfindingScheme.EMERGENCY),
      new WayfindingPreset("BEER", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.AIRPORT),
      new WayfindingPreset("PIZZA", "", WayfindingIcon.FOOD, WayfindingArrow.NONE,
          WayfindingScheme.LABORATORY),
      new WayfindingPreset("SNACKS", "", WayfindingIcon.FOOD, WayfindingArrow.NONE,
          WayfindingScheme.INFO),
      new WayfindingPreset("GRILL", "", WayfindingIcon.FOOD, WayfindingArrow.NONE,
          WayfindingScheme.TRANSIT),
      new WayfindingPreset("SWEETS", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.MATERNITY),
      new WayfindingPreset("DRINKS", "", WayfindingIcon.CAFE, WayfindingArrow.NONE,
          WayfindingScheme.OUTPATIENT),
      new WayfindingPreset("CONCESSIONS", "", WayfindingIcon.FOOD, WayfindingArrow.NONE,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("TEAM STORE", "", WayfindingIcon.SHOP, WayfindingArrow.NONE,
          WayfindingScheme.INPATIENT),
  };

  /** A new sign: HOT DOGS, centred, lit, on the wall or hung as the panel decides. */
  static final WayfindingSign DEFAULT = CONCESSION[0].toSign(WayfindingArrow.NONE, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).build();

  private ConcourseSignPresets() {
  }
}
