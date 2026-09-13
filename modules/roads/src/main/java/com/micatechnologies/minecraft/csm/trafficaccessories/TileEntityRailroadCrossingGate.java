package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The gate arm's motion. The block's powered state says where the arm should be; this walks
 * the arm there at a real gate's pace -- about eight seconds down, ten up -- and keeps the
 * previous tick's angle so the renderer can interpolate between ticks.
 *
 * <p>The angle is visual state, so it is ticked on the client and never saved: a chunk that
 * loads with the crossing already active shows the gate already down rather than lowering it
 * again.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityRailroadCrossingGate extends AbstractTickableTileEntity {

  /** Arm angle from horizontal, in degrees: 0 is down across the road, 90 is up. */
  public static final float RAISED = 90F;
  public static final float LOWERED = 0F;
  private static final float LOWER_DEGREES_PER_TICK = RAISED / 160F;  // 8 s
  private static final float RAISE_DEGREES_PER_TICK = RAISED / 200F;  // 10 s

  private float angle = RAISED;
  private float prevAngle = RAISED;
  private boolean settled = false;

  @Override
  public boolean doClientTick() {
    return true;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return 1L;
  }

  @Override
  public void onTick() {
    float target = AbstractBlockRailroadCrossing.isActive(getWorld(), getPos()) ? LOWERED : RAISED;
    if (!settled) {
      // First tick after load: be where the state says, without a swing
      angle = prevAngle = target;
      settled = true;
      return;
    }
    prevAngle = angle;
    if (angle > target) {
      angle = Math.max(target, angle - LOWER_DEGREES_PER_TICK);
    } else if (angle < target) {
      angle = Math.min(target, angle + RAISE_DEGREES_PER_TICK);
    }
  }

  /**
   * The arm angle to draw this frame.
   *
   * @param partialTicks the fraction of the current tick elapsed
   *
   * @return degrees from horizontal
   */
  @SideOnly(Side.CLIENT)
  public float getRenderAngle(float partialTicks) {
    return prevAngle + (angle - prevAngle) * partialTicks;
  }

  /** @return whether the arm is moving or down: the lamps' condition */
  public boolean isArmActive() {
    return angle < RAISED - 0.5F
        || AbstractBlockRailroadCrossing.isActive(getWorld(), getPos());
  }

  /**
   * The arm reaches well outside the block in every direction it can point, so the renderer
   * must not be culled while the cabinet is off screen.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    Block block = getWorld() == null ? null : getWorld().getBlockState(getPos()).getBlock();
    double reach = block instanceof BlockRailroadCrossingGate
        ? ((BlockRailroadCrossingGate) block).getArmLength() + 1.0 : 12.0;
    return new AxisAlignedBB(getPos()).grow(reach, reach, reach);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    return LONG_RANGE_RENDER_DISTANCE_SQUARED;
  }
}
