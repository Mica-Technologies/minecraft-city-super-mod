package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the gate blade ({@link BlockVenueBlade}): Core's wayfinding panel cell with the
 * blade's own default, GATE over A.
 *
 * @since 2026.10
 */
public class TileEntityGateBlade extends AbstractTileEntityWayfindingPanel {

  @Override
  public WayfindingSign defaultSign() {
    return ExteriorSignPresets.GATE_DEFAULT;
  }
}
