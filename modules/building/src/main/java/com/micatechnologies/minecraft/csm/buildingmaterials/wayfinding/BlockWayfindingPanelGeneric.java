package com.micatechnologies.minecraft.csm.buildingmaterials.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The generic wayfinding panel ({@code wayfinding_panel}, GitHub #307): Core's wayfinding panel
 * with no airport or hospital theming, for buildings, offices, museums and stairwells. It starts
 * as an unlit aluminium sign that sits flat on the wall it is placed against; every option (the
 * mounts, scale, line icons, colours) is the airport and hospital panels' too.
 *
 * @since 2026.10
 */
public class BlockWayfindingPanelGeneric extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the panel.
   *
   * @param registryName its registry name
   */
  public BlockWayfindingPanelGeneric(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return WayfindingPreset.BUILDING;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityWayfindingPanelGeneric.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitywayfindingpanelgeneric";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityWayfindingPanelGeneric();
  }
}
