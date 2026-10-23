package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The large hanging sign ({@code airport_wayfinding_panel}): Core's wayfinding panel with the
 * airport's default sign and presets. Everything it does (cells joining into one panel up to
 * 16 x 6, the mounts, the scaling, the icons, the editor) is
 * {@link AbstractBlockWayfindingPanel}'s; the generic panel and the hospital's are the same block
 * with another default.
 *
 * @since 2026.10
 */
public class BlockWayfindingPanel extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the block.
   *
   * @param registryName its registry name
   */
  public BlockWayfindingPanel(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return WayfindingPreset.AIRPORT;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityWayfindingPanel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitywayfindingpanel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityWayfindingPanel();
  }
}
