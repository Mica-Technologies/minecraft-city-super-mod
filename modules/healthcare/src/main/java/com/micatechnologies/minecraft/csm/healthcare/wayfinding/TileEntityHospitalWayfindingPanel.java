package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * A cell of the hospital wayfinding panel ({@link BlockHospitalWayfindingPanel}): Core's panel
 * cell, with the hospital's default sign (INFORMATION, the information pictogram, an arrow to the
 * right, the neutral navy, lit). Kept as its own class so the tile entity keeps its registered
 * name.
 *
 * @since 2026.10
 */
public class TileEntityHospitalWayfindingPanel extends AbstractTileEntityWayfindingPanel {

  private static final WayfindingSign DEFAULT =
      WayfindingPreset.HOSPITAL[0].toSign(WayfindingArrow.RIGHT, true);

  @Override
  public WayfindingSign defaultSign() {
    return DEFAULT;
  }
}
