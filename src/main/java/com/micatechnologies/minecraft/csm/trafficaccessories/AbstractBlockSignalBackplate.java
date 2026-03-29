package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Abstract base class for traffic signal backplate blocks. Adds a computed TILT property
 * that mirrors the tilt setting of the signal head behind the backplate. The TILT property
 * is not stored in metadata — it is computed each render frame via {@link #getActualState}
 * by reading the {@link TileEntityTrafficSignalHead} in the block behind this backplate.
 * This requires no tile entity on the backplate itself.
 */
public abstract class AbstractBlockSignalBackplate extends AbstractBlockRotatableNSEWUD {

  /**
   * The tilt property, matching {@link TrafficSignalBodyTilt} values. Computed in
   * {@link #getActualState} — not stored in block metadata.
   */
  public static final PropertyEnum<TrafficSignalBodyTilt> TILT =
      PropertyEnum.create("tilt", TrafficSignalBodyTilt.class);

  public AbstractBlockSignalBackplate(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
        lightLevel, lightOpacity, false);
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(TILT, TrafficSignalBodyTilt.NONE));
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, TILT);
  }

  /**
   * Computes the actual tilt state by reading the signal head behind this backplate.
   * "Behind" is determined by the backplate's facing: the signal is one block in the
   * opposite direction of FACING (since the backplate faces the viewer, the signal is
   * behind it).
   */
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);

    // Search all 4 horizontal neighbors for an adjacent signal head.
    // Filter: only accept signals on the same facing axis (behind or in front).
    // This prevents picking up unrelated signals to the sides.
    EnumFacing[] horizontalDirs = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST};
    for (EnumFacing checkDir : horizontalDirs) {
      // Only check positions along the backplate's facing axis
      if (checkDir.getAxis() != facing.getAxis()) continue;

      BlockPos signalPos = pos.offset(checkDir);
      TileEntity te = worldIn.getTileEntity(signalPos);
      if (te instanceof TileEntityTrafficSignalHead) {
        TrafficSignalBodyTilt tilt = ((TileEntityTrafficSignalHead) te).getBodyTilt();
        // For SOUTH facing, the blockstate's 180° rotation flips left/right visually,
        // so we swap left↔right to match the signal renderer's SOUTH reversal
        if (facing == EnumFacing.SOUTH) {
          tilt = mirrorTilt(tilt);
        }
        return state.withProperty(TILT, tilt);
      }
    }

    return state.withProperty(TILT, TrafficSignalBodyTilt.NONE);
  }

  /**
   * Mirrors a tilt value (swaps left↔right). Used for SOUTH-facing backplates where the
   * 180° blockstate rotation flips the visual direction.
   */
  private static TrafficSignalBodyTilt mirrorTilt(TrafficSignalBodyTilt tilt) {
    switch (tilt) {
      case LEFT_TILT:   return TrafficSignalBodyTilt.RIGHT_TILT;
      case RIGHT_TILT:  return TrafficSignalBodyTilt.LEFT_TILT;
      case LEFT_ANGLE:  return TrafficSignalBodyTilt.RIGHT_ANGLE;
      case RIGHT_ANGLE: return TrafficSignalBodyTilt.LEFT_ANGLE;
      default:          return tilt;
    }
  }
}
