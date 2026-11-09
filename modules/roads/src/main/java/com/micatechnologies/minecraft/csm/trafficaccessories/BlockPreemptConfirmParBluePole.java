package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The blue PAR confirmation light, clamped to the side of a post or pedestal pole (#338): place it on the face of
 * the pole that looks up the approach. In every other way it is {@link BlockPreemptConfirmParBlue}: linked to a
 * circuit with the Signal Link Tool and lit by the controller or redstone the same way.
 *
 * @since 2026.11
 */
public class BlockPreemptConfirmParBluePole extends BlockPreemptConfirmParBlue
    implements IPreemptPoleMounted {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_par_blue_pole";
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
    return PAR_POLE_BOX;
  }

  @Override
  public float[] getBeaconLensFrom() {
    return PAR_POLE_LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return PAR_POLE_LENS_TO;
  }
}
