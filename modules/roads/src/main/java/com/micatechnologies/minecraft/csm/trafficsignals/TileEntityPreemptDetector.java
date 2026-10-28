package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptEmitter;
import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptSources;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The preempt detector's state: how far and how wide it looks, and whether it has a call.
 * <p>
 * A detector is called by redstone, or by an emergency emitter it can see (from
 * {@link CsmPreemptSources}, which a module such as CSM: Vehicles feeds). The controller reads
 * {@link #isCalled()} through the circuit the detector is linked to, as it reads a sensor; a
 * preempt whose trigger is that circuit's detectors fires while any of them is called.
 * <p>
 * For testing a preempt without a vehicle, NBT {@code tst} is a timed call in ticks
 * ({@code /blockdata x y z {tst:200}} calls for 10 s, then ends by itself). Writing {@code pw}
 * no longer works for that: the redstone call is checked against the redstone actually there.
 * <p>
 * A transit emitter (a bus) is a separate call, {@link #isTransitCalled()}, read by transit
 * signal priority and never by a preempt, as a real detector tells the two apart by the
 * strobe's rate.
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
  private static final String K_TEST = "tst";

  /** How far the detector sees, in blocks. */
  private int range = DEFAULT_RANGE;

  /** How far either side of its facing the detector sees, in degrees. */
  private int halfAngle = DEFAULT_HALF_ANGLE;

  /**
   * Whether redstone was calling the detector at the last look: set by a neighbour change, and
   * checked against the redstone actually reaching the detector whenever the controller reads a
   * standing call ({@link #isCalled()}), so it cannot outlive its source.
   */
  private boolean powered = false;

  /**
   * A test call's length, in ticks, not yet started: set by NBT {@code tst} (for example
   * {@code /blockdata x y z {tst:200}}, a 10 s call), and started at the next read.
   */
  private int testTicksPending = 0;

  /** The world tick a started test call ends at, or -1 with no test call running. */
  private long testUntil = -1L;

  /** The world tick of the last read, to save what is left of a running test call. */
  private long lastReadTick = 0L;

  /** Whether an emitter was in view at the last look. Transient: re-read within a tick. */
  private transient boolean emitterInView = false;

  /** Whether a transit emitter was in view at the last look. Transient, like the above. */
  private transient boolean transitInView = false;

  @Override
  public void readNBT(NBTTagCompound compound) {
    range = compound.hasKey(K_RANGE) ? clampTo(RANGES, compound.getInteger(K_RANGE))
        : DEFAULT_RANGE;
    halfAngle = compound.hasKey(K_HALF_ANGLE)
        ? clampTo(HALF_ANGLES, compound.getInteger(K_HALF_ANGLE)) : DEFAULT_HALF_ANGLE;
    powered = compound.getBoolean(K_POWERED);
    testTicksPending = Math.max(0, compound.getInteger(K_TEST));
    testUntil = -1L;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(K_RANGE, range);
    compound.setInteger(K_HALF_ANGLE, halfAngle);
    compound.setBoolean(K_POWERED, powered);
    long testLeft = testUntil >= 0L ? testUntil - lastReadTick : testTicksPending;
    if (testLeft > 0L) {
      compound.setInteger(K_TEST, (int) Math.min(Integer.MAX_VALUE, testLeft));
    }
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
   * Whether the detector has a call: redstone, an emergency emitter in view, or a test call.
   * <p>
   * A redstone call is checked against the redstone reaching the detector now. The flag alone
   * was only ever cleared by a neighbour update, so a call written by {@code /blockdata}, saved
   * with the chunk, or left by a source removed without an update reaching the detector held the
   * controller's preempt dwell for good (issue #319). Only a standing call is checked, and only
   * while the detector and its neighbours are loaded, so an idle detector costs nothing and a
   * read never loads a chunk.
   *
   * @return whether the detector is called
   *
   * @since 1.0
   */
  public boolean isCalled() {
    World world = getWorld();
    if (world == null || world.isRemote) {
      return powered || emitterInView;
    }
    BlockPos pos = getPos();
    return isCalledAt(world.getTotalWorldTime(),
        () -> !world.isAreaLoaded(pos, 1) || world.isBlockPowered(pos));
  }

  /**
   * {@link #isCalled()} at world tick {@code now}, with {@code redstone} saying whether redstone
   * reaches the detector (asked only while a redstone call stands).
   */
  boolean isCalledAt(long now, BooleanSupplier redstone) {
    lastReadTick = now;
    if (powered && !redstone.getAsBoolean()) {
      setPowered(false);
    }
    if (testTicksPending > 0) {
      testUntil = now + testTicksPending;
      testTicksPending = 0;
    }
    boolean testCall = testUntil >= 0L && now < testUntil;
    if (testUntil >= 0L && !testCall) {
      testUntil = -1L;
      markDirty();
    }
    return powered || emitterInView || testCall;
  }

  /**
   * Whether a transit emitter is in view: a call for transit signal priority, not a preempt.
   *
   * @return whether the detector has a transit call
   *
   * @since 1.0
   */
  public boolean isTransitCalled() {
    return transitInView;
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
   * Records whether redstone is calling the detector.
   *
   * @param powered whether the detector is powered
   *
   * @since 1.0
   */
  public void setPowered(boolean powered) {
    if (this.powered != powered) {
      this.powered = powered;
      markDirty();
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
    lookForEmitters();
  }

  /** Records which kinds of emitter are in view now. */
  private void lookForEmitters() {
    boolean emergency = false;
    boolean transit = false;
    List<CsmPreemptEmitter> emitters = CsmPreemptSources.emitters(getWorld());
    IBlockState state = getWorld().getBlockState(getPos());
    if (!emitters.isEmpty() && state.getBlock() instanceof BlockPreemptDetector) {
      emergency = sees(emitters, state, CsmPreemptEmitter.Kind.EMERGENCY);
      transit = sees(emitters, state, CsmPreemptEmitter.Kind.TRANSIT);
    }
    transitInView = transit;
    emitterInView = emergency;
  }

  /** Whether an emitter of {@code kind} is in view. */
  private boolean sees(List<CsmPreemptEmitter> emitters, IBlockState state,
      CsmPreemptEmitter.Kind kind) {
    EnumFacing facing = state.getValue(BlockPreemptDetector.FACING);
    double lensX = getPos().getX() + 0.5;
    double lensY = getPos().getY() + 0.5;
    double lensZ = getPos().getZ() + 0.5;
    for (CsmPreemptEmitter emitter : emitters) {
      if (emitter.kind == kind
          && CsmPreemptSources.sees(emitter, lensX, lensY, lensZ, facing.getXOffset(),
          facing.getZOffset(), range, halfAngle)) {
        return true;
      }
    }
    return false;
  }
}
