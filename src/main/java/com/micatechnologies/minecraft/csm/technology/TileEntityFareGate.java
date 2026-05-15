package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;

/**
 * Auto-close timer for {@link BlockFareGate}. Records the world tick the gate was opened on
 * and, when ticking, flips OPEN back to false once the configured delay elapses. Pauses
 * ticking entirely when the gate is closed so an unused gate costs nothing per tick.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class TileEntityFareGate extends AbstractTickableTileEntity {

  private static final String NBT_OPENED_AT = "openedAt";

  /**
   * How long an opened gate stays open before auto-closing, in game ticks. 60 ticks ≈
   * 3 seconds — long enough to walk through unhurried, short enough to discourage tailgating.
   */
  private static final long AUTO_CLOSE_TICKS = 60L;

  /** World tick the gate was opened on, or {@link Long#MIN_VALUE} when closed. */
  private long openedAt = Long.MIN_VALUE;

  /** Called by the block class right after flipping OPEN to true on the world state. */
  public void markOpened() {
    openedAt = world == null ? 0L : world.getTotalWorldTime();
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  public long getAutoCloseTicks() {
    return AUTO_CLOSE_TICKS;
  }

  // === Tickable plumbing ===

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    // Only run while the gate is actually open and waiting to close.
    return openedAt == Long.MIN_VALUE;
  }

  @Override
  public long getTickRate() {
    return 10;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockFareGate)) {
      // Block was replaced out from under us — drop the timer so the TE stops ticking.
      openedAt = Long.MIN_VALUE;
      return;
    }
    if (!state.getValue(BlockFareGate.OPEN)) {
      // Already closed (e.g. by a future ops override). Reset the timer.
      openedAt = Long.MIN_VALUE;
      return;
    }
    long elapsed = world.getTotalWorldTime() - openedAt;
    if (elapsed >= AUTO_CLOSE_TICKS) {
      world.setBlockState(pos, state.withProperty(BlockFareGate.OPEN, false), 3);
      openedAt = Long.MIN_VALUE;
      world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
          SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.4F, 1.6F);
    }
  }

  // === NBT ===

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(NBT_OPENED_AT)) {
      openedAt = compound.getLong(NBT_OPENED_AT);
    } else {
      openedAt = Long.MIN_VALUE;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (openedAt != Long.MIN_VALUE) {
      compound.setLong(NBT_OPENED_AT, openedAt);
    }
    return compound;
  }
}
