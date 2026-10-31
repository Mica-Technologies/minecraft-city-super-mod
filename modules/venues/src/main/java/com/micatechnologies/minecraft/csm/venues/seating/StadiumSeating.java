package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Sitting in a stadium seat: Core's {@link EntityCsmSeat}, spawned only when a player clicks a
 * seat and gone when they get up, so a bowl of eighteen thousand seats holds no entity and no
 * tile entity until someone sits.
 *
 * @since 2026.10
 */
public final class StadiumSeating {

  /** The status message when a seat is taken. */
  public static final String SEAT_TAKEN = "csm.venues.seat.taken";

  /** Where the seat entity sits, in blocks above the block's floor. */
  private static final double SEAT_Y = 0.1;
  /** How far below the top of a seat the rider is placed (the park bench's fit). */
  private static final double RIDER_BELOW_SEAT = 0.12;

  private StadiumSeating() {
  }

  /**
   * Seats a player in the seat at {@code pos}, facing {@code front}, and lets them out into the
   * cell in front of it (a 2-deep row's legroom, or the air over the row below's seat backs in a
   * 1-deep row). One sitter a block.
   *
   * @param world   the world
   * @param pos     the seat
   * @param front   the way the sitter faces
   * @param top     the top of the seat pan above the block's floor, in blocks
   * @param forward how far towards {@code front} of the block's middle the pan's middle is
   * @param player  the player
   */
  public static void sit(World world, BlockPos pos, EnumFacing front, double top, double forward,
      EntityPlayer player) {
    double sx = pos.getX() + 0.5 + front.getXOffset() * forward;
    double sz = pos.getZ() + 0.5 + front.getZOffset() * forward;
    BlockPos out = pos.offset(front);
    EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), SEAT_TAKEN, sx, pos.getY() + SEAT_Y,
        sz, top - RIDER_BELOW_SEAT - SEAT_Y, front, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
  }
}
