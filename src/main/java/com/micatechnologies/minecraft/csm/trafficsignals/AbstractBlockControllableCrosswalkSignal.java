package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractBlockControllableCrosswalkSignal extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  public AbstractBlockControllableCrosswalkSignal() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN;
  }

  @Override
  public boolean doesFlash() {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCrosswalkSignal.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycrosswalksignal";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityCrosswalkSignal();
  }

  /**
   * Returns the Z offset from block center to the display face for countdown rendering.
   * Positive values move toward the viewer (north face direction in model space).
   * Return -1 to disable countdown rendering for this variant.
   */
  public float getCountdownZOffset() {
    return 0.4375f; // base model: north face at Z=1/16
  }

  /**
   * Returns the Y center of the signal body element in block space (0-1).
   * Used to vertically center the countdown overlay on the signal face.
   */
  public float getCountdownYCenter() {
    return 0.5625f; // base model: element Y 1-17, center at 9/16
  }

  @Override
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
      net.minecraft.block.Block blockIn, BlockPos fromPos) {
    // Auto-create TE for pre-existing crosswalk signals (migration)
    if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
      worldIn.setTileEntity(pos, createNewTileEntity(worldIn, 0));
    }
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
  }
}
