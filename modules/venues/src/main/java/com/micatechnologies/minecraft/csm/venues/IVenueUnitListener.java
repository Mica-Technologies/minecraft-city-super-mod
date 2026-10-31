package com.micatechnologies.minecraft.csm.venues;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A {@link BlockVenueUnit} master that wants to hear of a neighbour change at any of its cells,
 * not only its own (the scoreboard, whose clock a redstone pulse at any cell starts and stops).
 *
 * @since 2026.10
 */
public interface IVenueUnitListener {

  /**
   * A neighbour of one of the unit's cells changed. Server side.
   *
   * @param world  the world
   * @param master the master's position
   * @param state  the master's state
   * @param cell   the cell whose neighbour changed (the master or a part)
   */
  void onUnitNeighborChanged(World world, BlockPos master, IBlockState state, BlockPos cell);
}
