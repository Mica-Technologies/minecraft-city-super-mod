package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The platform bench: perforated steel seats with armrests on a beam, two seats a block. Benches
 * side by side join into one (an armrest at every joint, the end armrests only at the ends; see
 * {@link BlockPlatformRun}). A click sits the player in the seat nearer where they clicked, as
 * a park bench does, through Core's {@link EntityCsmSeat}; each seat takes one person.
 *
 * <p>The seats' places are the generator's ({@code gen_transit_platforms.py}, {@code SEATS}):
 * their middles are {@link #SEAT_SIDE} either side of the block's middle and {@link #SEAT_FORWARD}
 * in front of it.</p>
 *
 * @since 2026.9
 */
public class BlockPlatformBench extends BlockPlatformRun {

  /** How far each seat's middle is from the block's middle, sideways, in blocks. */
  private static final double SEAT_SIDE = 3.65 / 16.0;
  /** How far the seats' middles are in front of the block's middle, in blocks. */
  private static final double SEAT_FORWARD = 1.5 / 16.0;
  /** The seat's height above the block, and how far above that the rider sits. */
  private static final double SEAT_Y = 0.1;
  private static final double RIDER_OFFSET = 0.3;
  /** Half the width of the space another seat must not be in for this one to be free. */
  private static final double SEAT_ROOM = 0.15;

  /**
   * Constructs a bench.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformBench(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking() || hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    EnumFacing front = state.getValue(FACING);
    EnumFacing right = front.rotateY();
    double across = (hitX - 0.5) * right.getXOffset() + (hitZ - 0.5) * right.getZOffset();
    double sideways = across < 0 ? -SEAT_SIDE : SEAT_SIDE;
    double sx = pos.getX() + 0.5 + right.getXOffset() * sideways
        + front.getXOffset() * SEAT_FORWARD;
    double sz = pos.getZ() + 0.5 + right.getZOffset() * sideways
        + front.getZOffset() * SEAT_FORWARD;
    BlockPos out = pos.offset(front);
    AxisAlignedBB room = new AxisAlignedBB(sx - SEAT_ROOM, pos.getY(), sz - SEAT_ROOM,
        sx + SEAT_ROOM, pos.getY() + 1, sz + SEAT_ROOM);
    EntityCsmSeat.sit(world, pos, room, "gui.csm.seat.taken", sx, pos.getY() + SEAT_Y, sz,
        RIDER_OFFSET, front, out.getX() + 0.5, out.getY(), out.getZ() + 0.5, player);
    return true;
  }
}
