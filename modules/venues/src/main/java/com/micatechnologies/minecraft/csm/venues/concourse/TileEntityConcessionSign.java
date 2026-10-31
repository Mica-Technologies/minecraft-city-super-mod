package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the {@link BlockConcessionSign}: Core's wayfinding panel cell with the sign's own
 * default.
 *
 * @since 2026.10
 */
public class TileEntityConcessionSign extends AbstractTileEntityWayfindingPanel {

  @Override
  public WayfindingSign defaultSign() {
    return ConcourseSignPresets.DEFAULT;
  }
}
