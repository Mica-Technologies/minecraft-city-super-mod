package com.micatechnologies.minecraft.csm.codeutils.light;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block that projects one light-up air into the column below it (Lighting's bright lights), so
 * that other code removing light-up air can ask whether a cell is still wanted by a fixture.
 *
 * <p>{@link CsmLightupAir#claimedBySource} looks for sources in the cells up to
 * {@link #SEARCH_UP} above a light-up air and {@link #SEARCH_X} to either side of it along x (the
 * only axis a bright light offsets its column along, by at most one block).</p>
 */
public interface ICsmLightupAirSource {

  /** How far to either side along x a source may stand from the light-up air it claims. */
  int SEARCH_X = 1;

  /** How far above a light-up air a source may stand. */
  int SEARCH_UP = 16;

  /**
   * Whether this block, lit, relies on the light-up air at {@code air}.
   *
   * @param world the world
   * @param pos   this block's position
   * @param state this block's state
   * @param air   a light-up air's position
   *
   * @return {@code true} if removing it would leave this light without its light
   */
  boolean claimsLightupAir(World world, BlockPos pos, IBlockState state, BlockPos air);
}
