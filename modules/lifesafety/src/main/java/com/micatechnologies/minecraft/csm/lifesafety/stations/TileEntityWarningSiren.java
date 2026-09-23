package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * An outdoor warning siren's state: the signal it is sounding and how long is left, set by its
 * controller and synced to the client, which turns the horn ({@link TileEntityWarningSirenRenderer})
 * and plays the sound ({@link SirenClient}).
 *
 * <p>The server counts the signal down and stops it; nothing else ticks. The horn's angle is kept
 * on the client only, from the world clock, so every siren sounding turns in step and nothing
 * about it is sent.</p>
 *
 * @since 2026.9
 */
public class TileEntityWarningSiren extends AbstractTickableTileEntity {

  /** Degrees a rotating horn turns each tick: a turn every eight seconds. */
  public static final float DEGREES_PER_TICK = 360F / 160F;

  private static final String SIGNAL_KEY = "s";
  private static final String LEFT_KEY = "l";

  private SirenSignal signal = SirenSignal.NONE;
  private int ticksLeft;

  /** Client only: the playing sound, kept as an Object so this class names no client class. */
  public Object clientSound;

  public SirenSignal getSignal() {
    return signal;
  }

  /** Whether this siren's horn turns (the rotating siren) or stands still (the electronic array). */
  public boolean rotates() {
    return world != null && world.getBlockState(pos).getBlock() instanceof BlockWarningSiren
        && ((BlockWarningSiren) world.getBlockState(pos).getBlock()).rotates();
  }

  /**
   * Starts a signal, or stops the siren with {@link SirenSignal#NONE}.
   *
   * @param next the signal to sound
   */
  public void sound(SirenSignal next) {
    signal = next;
    ticksLeft = next.ticks;
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof BlockWarningSiren
        && state.getValue(BlockWarningSiren.ACTIVE) != (next != SirenSignal.NONE)) {
      world.setBlockState(pos, state.withProperty(BlockWarningSiren.ACTIVE,
          next != SirenSignal.NONE), 3);
    }
    markDirtySync(world, pos, true);
  }

  /** The horn's angle in degrees at a moment, from the world clock. */
  public float hornAngle(float partialTicks) {
    return ((world.getTotalWorldTime() % 160) + partialTicks) * DEGREES_PER_TICK;
  }

  @Override
  public boolean doClientTick() {
    return true;
  }

  @Override
  public boolean pauseTicking() {
    return signal == SirenSignal.NONE && (world == null || !world.isRemote
        || clientSound == null);
  }

  @Override
  public long getTickRate() {
    return 1;
  }

  @Override
  public void onTick() {
    if (world.isRemote) {
      SirenClient.tick(this);
      return;
    }
    if (signal != SirenSignal.NONE && --ticksLeft <= 0) {
      sound(SirenSignal.NONE);
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      SirenClient.stop(this);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      SirenClient.stop(this);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    signal = SirenSignal.of(compound.getInteger(SIGNAL_KEY));
    ticksLeft = compound.getInteger(LEFT_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(SIGNAL_KEY, signal.ordinal());
    compound.setInteger(LEFT_KEY, ticksLeft);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    // The baked model reads only the block's own ACTIVE state, which travels by block update.
    return 0;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2, 2));
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return 128 * 128;
  }
}
