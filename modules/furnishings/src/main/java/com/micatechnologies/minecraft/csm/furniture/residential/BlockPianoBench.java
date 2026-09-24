package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A piano bench, sat on as a chair is. Unlike a chair it faces the way its placer looks, not
 * back at them: a bench is set down by someone looking at the piano, and whoever sits on it
 * faces the keys. Getting up, the sitter steps back off it, away from the piano.
 *
 * @since 2026.9
 */
public class BlockPianoBench extends BlockResidentialFurniture {

  /** Where the seat entity sits, and how far below the seat top the rider is: a chair's. */
  private static final double SEAT_Y = 0.1;
  private static final double RIDER_BELOW_SEAT = 0.12;

  private final double seatTop;

  /**
   * Constructs a piano bench.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param seatTopPx    the top of the seat above the floor, in sixteenths
   */
  public BlockPianoBench(String registryName, int[] box, double seatTopPx) {
    super(registryName, box, false, seatTopPx, 0, 0);
    this.seatTop = seatTopPx / 16.0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  @Override
  protected boolean sit(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
    EnumFacing front = state.getValue(FACING);
    BlockPos out = pos.offset(front.getOpposite());
    EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), "csm.furnishings.seat.taken",
        pos.getX() + 0.5, pos.getY() + SEAT_Y, pos.getZ() + 0.5,
        seatTop - RIDER_BELOW_SEAT - SEAT_Y, front, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
    return true;
  }
}
