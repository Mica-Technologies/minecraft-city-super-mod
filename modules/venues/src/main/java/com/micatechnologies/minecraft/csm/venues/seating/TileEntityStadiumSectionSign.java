package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the {@link BlockStadiumSectionSign}: Core's wayfinding panel cell with the sign's own default.
 *
 * @since 2026.10
 */
public class TileEntityStadiumSectionSign extends AbstractTileEntityWayfindingPanel {

  @Override
  public WayfindingSign defaultSign() {
    return VenueSignPresets.SECTION_DEFAULT;
  }
}
