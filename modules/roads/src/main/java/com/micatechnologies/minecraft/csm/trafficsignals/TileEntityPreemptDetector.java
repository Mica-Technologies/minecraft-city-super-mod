package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptEmitter;
import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptSources;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * The preempt detector's state: how far and how wide it looks, and whether it has a call.
 * <p>
 * A detector is called by redstone, or by an emergency emitter it can see (from
 * {@link CsmPreemptSources}, which a module such as CSM: Vehicles feeds). The controller reads
 * {@link #isCalled()} through the circuit the detector is linked to, as it reads a sensor; a
 * preempt whose trigger is that circuit's detectors fires while any of them is called.
 * <p>
 * Looking for emitters is a few multiplications per emitter against a list collected once a
 * world tick for every detector, done four times a second. With no source registered the
 * detector does not tick at all, and only redstone can call it.
 *
 * @version 1.0
 * @see BlockPreemptDetector
 * @since 2026.10
 */
public class TileEntityPreemptDetector extends AbstractTickableTileEntity {

  /**
   * The ranges a detector can be set to, in blocks, in the order a click steps through them.
   *
   * @since 1.0
   */
  public static final int[] RANGES = {60, 120, 200, 300};

  /**
   * The cone half-angles a detector can be set to, in degrees, in the order a click steps through
   * them: narrow for a straight approach, wide for one that bends in.
   *
   * @since 1.0
   */
  public static final int[] HALF_ANGLES = {10, 20, 35};

  /**
   * The range a new detector has, in blocks.
   *
   * @since 1.0
   */
  public static final int DEFAULT_RANGE = 120;

  /**
   * The half-angle a new detector has, in degrees.
   *
   * @since 1.0
   */
  public static final int DEFAULT_HALF_ANGLE = 20;

  private static final String K_RANGE = "rg";
  private static final String K_HALF_ANGLE = "ha";
  private static final String K_POWERED = "pw";

  /** How far the detector sees, in blocks. */
  private int range = DEFAULT_RANGE;

  /** How far either side of its facing the detector sees, in degrees. */
  private int halfAngle = DEFAULT_HALF_ANGLE;

  /** Whether redstone is calling the detector. Kept from the last neighbour change. */
  private boolean powered = false;

  /** Whether an emitter was in view at the last look. Transient: re-read within a tick. */
  private transient boolean emitterInView = false;

  @Override
  public void readNBT(NBTTagCompound compound) {
    range = compound.hasKey(K_RANGE) ? clampTo(RANGES, compound.getInteger(K_RANGE))
        : DEFAULT_RANGE;
    halfAngle = compound.hasKey(K_HALF_ANGLE)
        ? clampTo(HALF_ANGLES, compound.getInteger(K_HALF_ANGLE)) : DEFAULT_HALF_ANGLE;
    powered = compound.getBoolean(K_POWERED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(K_RANGE, range);
    compound.setInteger(K_HALF_ANGLE, halfAngle);
    compound.setBoolean(K_POWERED, powered);
    return compound;
  }

  /** A saved value that is not one of the choices becomes the nearest choice. */
  private static int clampTo(int[] choices, int value) {
    int best = choices[0];
    for (int c : choices) {
      if (Math.abs(c - value) < Math.abs(best - value)) {
        best = c;
      }
    }
    return best;
  }

  /**
   * Whether the detector has a call: redstone, or an emergency emitter in view.
   *
   * @return whether the detector is called
   *
   * @since 1.0
   */
  public boolean isCalled() {
    return powered || emitterInView;
  }

  /** @return how far the detector sees, in blocks */
  public int getRange() {
    return range;
  }

  /** @return how far either side of its facing the detector sees, in degrees */
  public int getHalfAngle() {
    return halfAngle;
  }

  /**
   * Steps the range to the next choice, wrapping round.
   *
   * @return the new range
   *
   * @since 1.0
   */
  public int cycleRange() {
    range = next(RANGES, range);
    markDirtySync(getWorld(), getPos());
    return range;
  }

  /**
   * Steps the cone half-angle to the next choice, wrapping round.
   *
   * @return the new half-angle
   *
   * @since 1.0
   */
  public int cycleHalfAngle() {
    halfAngle = next(HALF_ANGLES, halfAngle);
    markDirtySync(getWorld(), getPos());
    return halfAngle;
  }

  private static int next(int[] choices, int current) {
    for (int i = 0; i < choices.length; i++) {
      if (choices[i] == current) {
        return choices[(i + 1) % choices.length];
      }
    }
    return choices[0];
  }

  /**
   * Records whether redstone is calling the detector, and shows it on the confirmation lamp.
   *
   * @param powered whether the detector is powered
   *
   * @since 1.0
   */
  public void setPowered(boolean powered) {
    if (this.powered != powered) {
      this.powered = powered;
      markDirty();
      showCall();
    }
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  /** Nothing to look for until some module registers a source of emitters. */
  @Override
  public boolean pauseTicking() {
    return !CsmPreemptSources.hasSources();
  }

  @Override
  public long getTickRate() {
    return 5L;
  }

  @Override
  public void onTick() {
    boolean inView = lookForEmitters();
    if (inView != emitterInView) {
      emitterInView = inView;
      showCall();
    }
  }

  /** Whether an emergency emitter is in view now. */
  private boolean lookForEmitters() {
    List<CsmPreemptEmitter> emitters = CsmPreemptSources.emitters(getWorld());
    if (emitters.isEmpty()) {
      return false;
    }
    IBlockState state = getWorld().getBlockState(getPos());
    if (!(state.getBlock() instanceof BlockPreemptDetector)) {
      return false;
    }
    EnumFacing facing = state.getValue(BlockPreemptDetector.FACING);
    double lensX = getPos().getX() + 0.5;
    double lensY = getPos().getY() + 0.5;
    double lensZ = getPos().getZ() + 0.5;
    for (CsmPreemptEmitter emitter : emitters) {
      if (emitter.kind == CsmPreemptEmitter.Kind.EMERGENCY
          && CsmPreemptSources.sees(emitter, lensX, lensY, lensZ, facing.getXOffset(),
          facing.getZOffset(), range, halfAngle)) {
        return true;
      }
    }
    return false;
  }

  /** Lights or darkens the confirmation lamp to match the call. Only writes on a change. */
  private void showCall() {
    if (getWorld() == null || getWorld().isRemote) {
      return;
    }
    IBlockState state = getWorld().getBlockState(getPos());
    if (state.getBlock() instanceof BlockPreemptDetector
        && state.getValue(BlockPreemptDetector.CALLED) != isCalled()) {
      getWorld().setBlockState(getPos(), state.withProperty(BlockPreemptDetector.CALLED,
          isCalled()), 2);
    }
  }
}
