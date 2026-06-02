package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Shared, self-correcting orientation logic for traffic-signal mount kits.
 *
 * <p>A mount kit is a bracket that holds a signal head one block in front of it — i.e. in the
 * block's {@code FACING} direction. The {@code verticallightmount}/{@code horizontallightmount}
 * shared models make this concrete: the back plate sits on the pole side (opposite FACING) and the
 * arms cantilever toward FACING, where the signal hangs at their end.
 *
 * <p>When the kits were ported from NSEWUD to NSEW metadata (commit d93c3231), existing placed kits
 * had their stored meta re-decoded through the new bit layout, which shifted their facing. The
 * signal head blocks themselves never moved, so the correct facing can be recovered cheaply on a
 * random tick: if a signal head is already in front there is nothing to do; otherwise, if exactly
 * one horizontal neighbor holds a signal head, the kit is re-pointed at it.
 *
 * <p>TODO (remove after Dec 2026): this is a one-time migration aid for worlds placed before the
 * NSEWUD&rarr;NSEW port. Once existing worlds have had a chance to load and self-correct, delete
 * this class and the {@code setTickRandomly(true)} / {@code randomTick} hooks in
 * {@link BlockSignalMountKit} and {@link BlockTrafficLightMountKit} — the kits don't need to keep
 * ticking forever.
 */
public final class SignalMountKitOrientation {

  private SignalMountKitOrientation() {
  }

  /**
   * @return {@code true} if the block at {@code pos} is a traffic signal head that a mount kit would
   *     hold (a signal head, blankout box, or lane-control signal).
   */
  public static boolean isSignalHead(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityTrafficSignalHead
        || te instanceof TileEntityBlankoutBox
        || te instanceof TileEntityLaneControlSignal;
  }

  /**
   * Re-points the mount kit at {@code pos} toward an adjacent signal head if its current facing is
   * wrong. This is a no-op when a signal already sits in front, when no horizontal neighbor holds a
   * signal, or when more than one does (ambiguous — left for the player to resolve manually).
   *
   * @param world      the world (server side)
   * @param pos        the mount kit position
   * @param state      the mount kit's current block state
   * @param facingProp the kit's horizontal facing property
   *
   * @return {@code true} if the facing was changed.
   */
  public static boolean correctFacing(World world, BlockPos pos, IBlockState state,
      PropertyDirection facingProp) {
    EnumFacing current = state.getValue(facingProp);

    // Already oriented correctly: a signal head sits one block ahead.
    if (current.getAxis().isHorizontal() && isSignalHead(world, pos.offset(current))) {
      return false;
    }

    // Otherwise look for the signal head among the horizontal neighbors.
    EnumFacing target = null;
    for (EnumFacing dir : EnumFacing.HORIZONTALS) {
      if (isSignalHead(world, pos.offset(dir))) {
        if (target != null) {
          // More than one candidate — can't tell which side is the front, so leave it alone.
          return false;
        }
        target = dir;
      }
    }

    if (target == null || target == current) {
      return false;
    }
    world.setBlockState(pos, state.withProperty(facingProp, target), 3);
    return true;
  }
}
