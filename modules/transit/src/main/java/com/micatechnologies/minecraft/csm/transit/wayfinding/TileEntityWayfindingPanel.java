package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the large hanging sign ({@link BlockWayfindingPanel}): Core's panel cell, with the
 * airport's default sign (GATES, departures, an arrow to the right, the airport colours, lit).
 * Kept as its own class so the tile entity keeps its registered name.
 *
 * @since 2026.10
 */
public class TileEntityWayfindingPanel extends AbstractTileEntityWayfindingPanel {

  private static final WayfindingSign DEFAULT =
      WayfindingPreset.AIRPORT[0].toSign(WayfindingArrow.RIGHT, true);

  @Override
  public WayfindingSign defaultSign() {
    return DEFAULT;
  }
}
