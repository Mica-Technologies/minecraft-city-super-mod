package com.micatechnologies.minecraft.csm.venues.seating;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A seat in a row of stadium seats: a {@link BlockStadiumSeat} or a
 * {@link BlockStadiumAisleEnd}. A row joins across seats facing the same way, so a seat draws an
 * end standard only where the cell beside it holds no seat of the row.
 *
 * @since 2026.10
 */
public interface IStadiumSeat {

  /** The colour every stadium seat piece falls back to: charcoal. */
  int DEFAULT_COLOUR = 0x3A3C40;

  /**
   * The way the seat at {@code pos} faces.
   *
   * @param world the world
   * @param pos   its position
   * @param state its stored state (never the actual state)
   *
   * @return the facing
   */
  EnumFacing seatFacing(IBlockAccess world, BlockPos pos, IBlockState state);

  /**
   * The seat's colour, as the cushion's tint.
   *
   * @param world the world
   * @param pos   its position
   * @param state its stored state
   *
   * @return the colour, 0xRRGGBB
   */
  int seatColour(IBlockAccess world, BlockPos pos, IBlockState state);
}
