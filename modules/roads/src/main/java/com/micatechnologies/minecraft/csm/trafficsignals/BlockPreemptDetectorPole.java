package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficaccessories.IPreemptPoleMounted;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The preempt detector clamped to the side of a post or pedestal pole (#338), for a junction
 * whose heads stand on poles with no mast arm or span wire: place it on the face of the pole that
 * looks up the approach it serves, and it stands in front of the pole on a bracket, facing that
 * approach. A pole-side confirmation light placed a cell above or below it on the same pole is
 * its lamp.
 * <p>
 * It is {@link BlockPreemptDetector} in everything but its model, box and placement: the same
 * tile entity, linked to a circuit with the Signal Link Tool, called by an emitter it can see
 * along its facing or by redstone, and its range and cone stepped by right-click and
 * sneak-right-click.
 *
 * @since 2026.11
 */
public class BlockPreemptDetectorPole extends BlockPreemptDetector implements IPreemptPoleMounted {

  /** The head and the bracket up to the cell's edge (from {@code gen_preempt_detector.py}). */
  private static final AxisAlignedBB POLE_BOUNDING_BOX =
      new AxisAlignedBB(6.4 / 16.0, 4.9 / 16.0, 4.4 / 16.0, 9.6 / 16.0, 11.2 / 16.0, 1.0);

  @Override
  public String getBlockRegistryName() {
    return "preempt_detector_pole";
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING,
        IPreemptPoleMounted.placementFacing(facing, placer));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return POLE_BOUNDING_BOX;
  }
}
