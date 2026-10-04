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
 * A preemption confirmation light that mounts on top of a mast arm, beside the preempt detector:
 * a PAR lamp facing the approach, or a 360 degree dome. It is a preemption beacon in every way
 * that matters (linked to a circuit with the Signal Link Tool, lit while a preempt triggered from
 * that circuit runs, and by redstone; the glow is the traffic beacon renderer's), with its own
 * model, lens and flash.
 *
 * <p>Its models are {@code gen_preempt_detector.py}'s and stand in the cell above a thin traffic
 * pole, a mount stub reaching down to it. The lens boxes here must match that script's.</p>
 *
 * @since 2026.10
 */
public abstract class BlockPreemptConfirmationLight extends BlockPreemptBeacon {

  /** The PAR lamp's box facing north: the yoke and can on their mount stub. */
  static final AxisAlignedBB PAR_BOX =
      new AxisAlignedBB(4.6 / 16, 0.0, 4.7 / 16, 11.4 / 16, 7.0 / 16, 11.0 / 16);

  /** The dome's box: its base, dome and cap. */
  static final AxisAlignedBB DOME_BOX =
      new AxisAlignedBB(5.0 / 16, 0.0, 5.0 / 16, 11.0 / 16, 5.6 / 16, 11.0 / 16);

  /** The PAR lamp's lens, facing north. */
  static final float[] PAR_LENS_FROM = {5.5f, 1.3f, 4.6f};
  static final float[] PAR_LENS_TO = {10.5f, 6.3f, 4.75f};

  /** The dome's lens, all round. */
  static final float[] DOME_LENS_FROM = {5.5f, -0.6f, 5.5f};
  static final float[] DOME_LENS_TO = {10.5f, 5.2f, 10.5f};

  /**
   * Faces the player placing it, never up or down: it is placed onto the top of an arm, where the
   * clicked face would point it at the sky.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PAR_BOX;
  }

  @Override
  public float[] getBeaconLensFrom() {
    return PAR_LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return PAR_LENS_TO;
  }

  /** A PAR lamp flashes once a second, longer than the beacon's double strobe. */
  @Override
  public long getBeaconCycleMillis() {
    return 1000L;
  }

  @Override
  public long getBeaconPulseMillis() {
    return 350L;
  }

  @Override
  public long getBeaconFadeMillis() {
    return 120L;
  }
}
