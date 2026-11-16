package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the stop-name blade ({@link BlockBusStopBlade}): Core's wayfinding panel cell with
 * the blade's own default, a stop name in CITYLINE's livery.
 *
 * @since 2026.11
 */
public class TileEntityBusStopBlade extends AbstractTileEntityWayfindingPanel {

  @Override
  public WayfindingSign defaultSign() {
    return BusStopSignPresets.BLADE_DEFAULT;
  }
}
