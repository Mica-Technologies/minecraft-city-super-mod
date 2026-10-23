package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareStateChanges;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The hospital wayfinding panel ({@code hospital_wayfinding_panel}): Core's wayfinding panel with
 * the hospital's default sign and department presets. Its metadata and NBT are the airport
 * panel's and the generic panel's, so a generator writes any of the three by changing the name.
 *
 * <p>A {@code /setblock} of a cell over itself facing another way is heard through
 * {@link HealthcareStateChanges}, and moves the cell from one panel to the other.</p>
 *
 * @since 2026.10
 */
public class BlockHospitalWayfindingPanel extends AbstractBlockWayfindingPanel implements
    HealthcareStateChanges.Listening {

  /**
   * Constructs the panel.
   *
   * @param registryName its registry name
   */
  public BlockHospitalWayfindingPanel(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return WayfindingPreset.HOSPITAL;
  }

  @Override
  public void onStateChangedInPlace(World world, BlockPos pos, IBlockState before,
      IBlockState after) {
    onFacingChangedInPlace(world, pos, before, after);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHospitalWayfindingPanel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhospitalwayfindingpanel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityHospitalWayfindingPanel();
  }
}
