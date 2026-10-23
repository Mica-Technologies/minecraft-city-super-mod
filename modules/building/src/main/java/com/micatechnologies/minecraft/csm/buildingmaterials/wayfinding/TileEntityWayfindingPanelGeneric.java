package com.micatechnologies.minecraft.csm.buildingmaterials.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the generic wayfinding panel ({@link BlockWayfindingPanelGeneric}): Core's panel
 * cell, its default sign INFORMATION with the information pictogram, no arrow, the corporate
 * colours, unlit.
 *
 * @since 2026.10
 */
public class TileEntityWayfindingPanelGeneric extends AbstractTileEntityWayfindingPanel {

  private static final WayfindingSign DEFAULT =
      WayfindingPreset.BUILDING[0].toSign(WayfindingArrow.NONE, false);

  @Override
  public WayfindingSign defaultSign() {
    return DEFAULT;
  }
}
