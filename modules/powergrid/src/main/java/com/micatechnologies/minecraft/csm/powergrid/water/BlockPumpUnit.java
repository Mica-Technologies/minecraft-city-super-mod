package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBox;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A pump or a pressure tank in a pump station: Roads' utility box multi-block (the root draws
 * the whole unit, invisible parts fill the rest, placing is all or nothing), with the nozzles a
 * {@link BlockWaterPipe} joins on the root block's sides.
 *
 * @since 2026.9
 */
public class BlockPumpUnit extends BlockUtilityBox implements IWaterPipeJoint {

  /** Which of the root block's sides, relative to the way it faces, carry a nozzle. */
  public enum Nozzles {
    /** Front and back: a split-case pump, suction and discharge either side of its case. */
    FRONT_BACK,
    /** Left and right: an inline pump, on the pipe's own line. */
    LEFT_RIGHT,
    /** The back only: a pressure tank's connection. */
    BACK
  }

  private final Nozzles nozzles;

  public BlockPumpUnit(String registryName, UtilityBoxSpec spec, Nozzles nozzles) {
    super(registryName, spec);
    this.nozzles = nozzles;
  }

  @Override
  public boolean joinsWaterPipe(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    EnumFacing facing = state.getValue(FACING);
    switch (nozzles) {
      case FRONT_BACK:
        return side.getAxis() == facing.getAxis();
      case LEFT_RIGHT:
        return side.getAxis() == facing.rotateY().getAxis();
      default:
        return side == facing.getOpposite();
    }
  }
}
