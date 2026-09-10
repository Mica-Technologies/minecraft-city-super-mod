package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

/**
 * A portable signal trailer's boom, found from a block sitting on it.
 *
 * <p>The boom is the one thing in this mod a mount can be fixed to that is not a block. It is
 * geometry belonging to a {@code portable_signal_trailer_arm} up to eight cells away, so a block
 * standing in one of the cells it passes through cannot find it by looking at its neighbours the
 * way everything else does. It has to walk back along the boom to the trailer that owns it.</p>
 *
 * <p>That walk is what this class is. Given a position and the way a mount kit faces, it looks
 * along both axes across that facing — a mount on a boom faces ACROSS it, because the boom
 * reaches over the road and the head looks back up it — and reports the bar's cross-section
 * where it passes through, or {@code null} if there is no boom there.</p>
 *
 * <p>All measurements are in 1/16 block units in the frame of the cell, and come from
 * {@link SignalTrailerGeometry}, which the model generator writes from the same numbers it
 * sweeps the boom with.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class SignalTrailerBoom {

  /** Half the bar's width, across the boom's run. The bar is centred on the cell on that axis. */
  public final float halfWidth;

  /** How far the bar stands above the cell's floor. */
  public final float thickness;

  /**
   * How far the trailer that owns this boom has settled onto the road under it, in blocks —
   * negative is down. The whole trailer model moves with it, boom included, so a clamp drawn at
   * the nominal height would float above the bar wherever the trailer is sitting on a slope.
   */
  public final double settleY;

  /** Which way the boom runs, away from its trailer. */
  public final EnumFacing runDirection;

  private SignalTrailerBoom(float halfWidth, float thickness, double settleY,
      EnumFacing runDirection) {
    this.halfWidth = halfWidth;
    this.thickness = thickness;
    this.settleY = settleY;
    this.runDirection = runDirection;
  }

  /**
   * Looks for a boom passing through {@code pos}.
   *
   * @param world     block access to probe
   * @param pos       the position to test — a mount kit's own block
   * @param kitFacing which way that mount faces; must be horizontal, and the boom is looked for
   *                  across it
   * @return the boom's cross-section there, or {@code null} if no trailer's boom reaches it
   * @since 1.0
   */
  @Nullable
  public static SignalTrailerBoom findAt(IBlockAccess world, BlockPos pos, EnumFacing kitFacing) {
    if (world == null || pos == null || kitFacing == null
        || kitFacing.getAxis() == EnumFacing.Axis.Y) {
      return null;
    }
    // A boom runs across the way its head faces, so only the two directions perpendicular to the
    // mount's facing can hold one. Checking all four would also match a mount facing straight
    // down the boom, which is a head nobody can read.
    SignalTrailerBoom found = search(world, pos, kitFacing.rotateY());
    return found != null ? found : search(world, pos, kitFacing.rotateYCCW());
  }

  @Nullable
  private static SignalTrailerBoom search(IBlockAccess world, BlockPos pos, EnumFacing run) {
    for (int cell = SignalTrailerGeometry.FIRST_CELL;
        cell <= SignalTrailerGeometry.LAST_CELL; cell++) {
      BlockPos trailerPos = pos.offset(run.getOpposite(), cell)
          .down(SignalTrailerGeometry.BOOM_HEIGHT);
      IBlockState state = world.getBlockState(trailerPos);
      if (!isArmTrailerFacing(state, run)) {
        continue;
      }
      double settle = 0.0;
      Vec3d offset = state.getOffset(world, trailerPos);
      if (offset != null) {
        settle = offset.y;
      }
      return new SignalTrailerBoom(SignalTrailerGeometry.halfWidthAt(cell),
          SignalTrailerGeometry.thicknessAt(cell), settle, run);
    }
    return null;
  }

  /** True if {@code state} is an arm-style signal trailer whose boom reaches out along {@code run}. */
  private static boolean isArmTrailerFacing(IBlockState state, EnumFacing run) {
    if (state == null) {
      return false;
    }
    Block block = state.getBlock();
    ResourceLocation name = block.getRegistryName();
    if (name == null || !SignalTrailerGeometry.ARM_TRAILER.equals(name.getPath())) {
      return false;
    }
    if (!state.getProperties().containsKey(AbstractBlockRotatableNSEW.FACING)) {
      return false;
    }
    return state.getValue(AbstractBlockRotatableNSEW.FACING) == run;
  }
}
