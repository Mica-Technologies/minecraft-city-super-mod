package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * Drives the dynamic behavior of {@link BlockFareGate}:
 *
 * <ul>
 *   <li><b>Auto-close timer</b> — once the block flips to an open state (entry or exit),
 *   {@link #markOpened()} starts a countdown; when {@link #AUTO_CLOSE_TICKS} ticks have
 *   elapsed the TE flips state back to {@link GateState#CLOSED}.</li>
 *
 *   <li><b>Proximity-driven exit</b> — while the gate is closed, the TE polls the cell
 *   directly behind the gate (interior cell) for nearby players. Detection is
 *   <em>edge-triggered</em> on a per-player basis: a player has to enter the cell from
 *   outside it to trigger an exit-open. Players who are still standing in the cell when
 *   the gate auto-closes don't immediately re-open it — they have to step out and back in.
 *   This prevents a bystander loitering on the interior side from cycling the gate
 *   open-and-shut indefinitely.</li>
 * </ul>
 *
 * <p>The TE ticks at {@link #TICK_RATE} (5 ticks ≈ 4 Hz). It never pauses ticking — even
 * when closed it needs to poll for proximity. The work per tick is a single
 * {@code getEntitiesWithinAABB} on a 1-cell box, which is cheap.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class TileEntityFareGate extends AbstractTickableTileEntity {

  private static final String NBT_OPENED_AT = "openedAt";

  /**
   * How long an opened gate stays open before auto-closing, in game ticks. 60 ticks ≈
   * 3 seconds — long enough to walk through unhurried, short enough to discourage
   * tailgating.
   */
  private static final long AUTO_CLOSE_TICKS = 60L;

  /** Tick rate for both auto-close countdown and proximity polling. */
  private static final long TICK_RATE = 5L;

  /** World tick the gate was opened on, or {@link Long#MIN_VALUE} when closed. */
  private long openedAt = Long.MIN_VALUE;

  /**
   * UUIDs of players we believe are currently standing in the interior cell. We only fire
   * an exit-open for a player whose UUID is NOT already in this set; once it's been
   * detected, the player has to leave the cell (drop out of the set) before they can
   * trigger another exit-open. Drained back to the players actually present each tick.
   */
  private final Set<UUID> playersInInteriorCell = new HashSet<>();

  /** Called by the block class right after flipping STATE to an open value. */
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
    // Always tick — the TE needs to run when CLOSED so it can poll the interior cell for
    // newly-arrived players, AND when OPEN so it can fire the auto-close.
    return false;
  }

  @Override
  public long getTickRate() {
    return TICK_RATE;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockFareGate)) {
      // Block was replaced out from under us. Reset everything and let the TE be GC'd.
      openedAt = Long.MIN_VALUE;
      playersInInteriorCell.clear();
      return;
    }

    GateState gs = state.getValue(BlockFareGate.STATE);
    if (gs.isOpen()) {
      tickWhileOpen(state);
    } else {
      tickWhileClosed(state);
    }
  }

  /** Auto-close countdown while open. */
  private void tickWhileOpen(IBlockState state) {
    if (openedAt == Long.MIN_VALUE) {
      // Lost our timer (NBT corruption, hand-edit). Snap shut so the gate doesn't sit
      // open forever waiting for a tick that never came.
      world.setBlockState(pos, state.withProperty(BlockFareGate.STATE, GateState.CLOSED), 3);
      return;
    }
    long elapsed = world.getTotalWorldTime() - openedAt;
    if (elapsed >= AUTO_CLOSE_TICKS) {
      world.setBlockState(pos, state.withProperty(BlockFareGate.STATE, GateState.CLOSED), 3);
      openedAt = Long.MIN_VALUE;
      world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
          SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.4F, 1.6F);
    }
  }

  /** Proximity polling while closed. Edge-trigger on the interior cell. */
  private void tickWhileClosed(IBlockState state) {
    BlockPos interior = BlockFareGate.interiorCell(pos, state);
    AxisAlignedBB detectionBox = new AxisAlignedBB(
        interior.getX(),     interior.getY(),     interior.getZ(),
        interior.getX() + 1, interior.getY() + 2, interior.getZ() + 1);
    List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, detectionBox);

    Set<UUID> currentTickPresence = new HashSet<>();
    EntityPlayer firstNew = null;
    for (EntityPlayer p : players) {
      UUID id = p.getUniqueID();
      currentTickPresence.add(id);
      if (firstNew == null && !playersInInteriorCell.contains(id)) {
        firstNew = p;
      }
    }
    // Update the persistent presence set to *only* include players still in the cell, so
    // a player who left and comes back can re-trigger.
    playersInInteriorCell.retainAll(currentTickPresence);
    playersInInteriorCell.addAll(currentTickPresence);

    if (firstNew != null) {
      ((BlockFareGate) state.getBlock()).openGate(world, pos, state, GateState.OPEN_EXIT);
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
    // playersInInteriorCell is intentionally transient — players' presence is re-derived
    // from world state on the next tick.
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (openedAt != Long.MIN_VALUE) {
      compound.setLong(NBT_OPENED_AT, openedAt);
    }
    return compound;
  }
}
