package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the section blade ({@link BlockVenueBlade}): Core's wayfinding panel cell with the
 * blade's own default, SECTIONS 101 - 112 with an arrow up.
 *
 * @since 2026.10
 */
public class TileEntitySectionBlade extends AbstractTileEntityWayfindingPanel {

  @Override
  public WayfindingSign defaultSign() {
    return ExteriorSignPresets.SECTION_DEFAULT;
  }
}
