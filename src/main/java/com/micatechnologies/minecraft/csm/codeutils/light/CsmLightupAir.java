package com.micatechnologies.minecraft.csm.codeutils.light;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The light-up air helpers every light shares: Lighting's one-per-fixture column projection, moved
 * here unchanged from {@code AbstractBrightLight}, and the scans the stadium light footprints
 * ({@link CsmLightFootprints}) are built from. None of them loads a chunk.
 */
public final class CsmLightupAir {

  /** How far below a fixture its column projection looks. */
  public static final int COLUMN_DEPTH = 16;

  private CsmLightupAir() {
  }

  /**
   * The light-up air block.
   *
   * @return the block, or {@code null} before Core's hidden tab has built it
   */
  @Nullable
  public static Block block() {
    return BlockLightupAir.get();
  }

  /**
   * Whether a state is light-up air.
   *
   * @param state a block state
   *
   * @return {@code true} if it is light-up air
   */
  public static boolean is(IBlockState state) {
    Block air = block();
    return air != null && state.getBlock() == air;
  }

  /**
   * A fixture's column projection, Lighting's rule: turning on, it places one light-up air in the
   * lowest air cell of the column below the fixture (offset by {@code xOffset}, {@code zOffset}),
   * down to {@link #COLUMN_DEPTH}, unless that is the cell straight under it or the column already
   * holds one; turning off, it removes the first light-up air in the column. A column in an
   * unloaded chunk is left alone.
   *
   * @param on      {@code true} to light, {@code false} to remove
   * @param world   the world
   * @param pos     the fixture's position
   * @param xOffset the column's x offset from the fixture
   * @param zOffset the column's z offset from the fixture
   *
   * @return {@code true} if a light-up air was placed
   */
  public static boolean projectColumn(boolean on, World world, BlockPos pos, int xOffset,
      int zOffset) {
    Block lightupAir = block();
    if (lightupAir == null) {
      return false;
    }
    final int xWithOffset = pos.getX() + xOffset;
    final int zWithOffset = pos.getZ() + zOffset;
    // Add light
    if (on) {
      BlockPos doAddAt = null;
      for (int findy = -1; findy >= -COLUMN_DEPTH; findy--) {
        final int yWithOffset = pos.getY() + findy;
        BlockPos test = new BlockPos(xWithOffset, yWithOffset, zWithOffset);
        // An offset column can reach into a neighbouring chunk; never load one to light it
        if (!world.isBlockLoaded(test)) {
          return false;
        }
        // Already lit: a lightupair is not air, so without this the scan would stop on it and
        // place a second one in the cell above, and every later call another
        if (world.getBlockState(test).getBlock() == lightupAir) {
          return false;
        }
        // found block that is not air
        if (world.isAirBlock(test)) {
          // dont add light if block is right below street light
          if (findy < -1) {
            doAddAt = test;
          }
        } else {
          break;
        }
      }
      // add if marked for add
      if (doAddAt != null) {
        world.setBlockState(doAddAt, lightupAir.getDefaultState(), 3);
        return true;
      }
    }
    // Remove light
    else {
      for (int findy = -1; findy >= -COLUMN_DEPTH; findy--) {
        final int yWithOffset = pos.getY() + findy;
        BlockPos test = new BlockPos(xWithOffset, yWithOffset, zWithOffset);
        // Verify the position is in a loaded chunk before accessing block state
        if (!world.isBlockLoaded(test)) {
          continue;
        }
        IBlockState bs = world.getBlockState(test);
        // stop removing light once hit block
        if (bs.getBlock() == lightupAir) {
          world.setBlockToAir(test);
          break;
        }
      }
    }
    return false;
  }

  /**
   * The light-up air a scan down a column meets before anything else that is not air.
   *
   * @param world the world
   * @param x     the column's x
   * @param fromY the first cell scanned
   * @param z     the column's z
   * @param depth how many cells to scan
   *
   * @return its position, or {@code null} if the scan meets something else first, runs out, or
   *     the column is not loaded
   */
  @Nullable
  public static BlockPos firstBelow(World world, int x, int fromY, int z, int depth) {
    Block lightupAir = block();
    if (lightupAir == null) {
      return null;
    }
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos(x, fromY, z);
    if (!world.isBlockLoaded(test)) {
      return null;
    }
    int bottom = Math.max(0, fromY - depth + 1);
    for (int y = Math.min(255, fromY); y >= bottom; y--) {
      test.setY(y);
      IBlockState state = world.getBlockState(test);
      if (state.getBlock() == lightupAir) {
        return test.toImmutable();
      }
      if (!state.getBlock().isAir(state, world, test)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Whether a fixture ({@link ICsmLightupAirSource}) above a light-up air still relies on it.
   * Reads up to {@code (2 * SEARCH_X + 1) * SEARCH_UP} cells, loaded chunks only.
   *
   * @param world the world
   * @param air   the light-up air's position
   *
   * @return {@code true} if some lit fixture claims it
   */
  public static boolean claimedBySource(World world, BlockPos air) {
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
    for (int dx = -ICsmLightupAirSource.SEARCH_X; dx <= ICsmLightupAirSource.SEARCH_X; dx++) {
      test.setPos(air.getX() + dx, air.getY(), air.getZ());
      if (!world.isBlockLoaded(test)) {
        continue;
      }
      int top = Math.min(255, air.getY() + ICsmLightupAirSource.SEARCH_UP);
      for (int y = air.getY() + 1; y <= top; y++) {
        test.setY(y);
        IBlockState state = world.getBlockState(test);
        if (state.getBlock() instanceof ICsmLightupAirSource
            && ((ICsmLightupAirSource) state.getBlock()).claimsLightupAir(world,
            test.toImmutable(), state, air)) {
          return true;
        }
      }
    }
    return false;
  }
}
