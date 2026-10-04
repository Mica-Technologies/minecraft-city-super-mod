package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Whether a position's chunk is really there, on either side.
 *
 * <p>{@link World#isBlockLoaded(BlockPos)} is the right test on the server and the wrong one on
 * the client: the client's chunk provider answers for a chunk it has never been sent with a blank,
 * empty chunk, which counts as loaded, and every block in it reads as air. Code that runs on the
 * client and asks "is the device still there?" then calls everything past the player's view
 * missing (#260). This treats that blank chunk as not loaded.</p>
 *
 * @since 2026.10
 */
public final class CsmChunks {

  private CsmChunks() {
  }

  /**
   * Whether the chunk holding {@code pos} is loaded and holds real data.
   *
   * @param world the world, either side
   * @param pos   the position
   *
   * @return {@code true} if the block there can be trusted
   */
  public static boolean isReallyLoaded(World world, BlockPos pos) {
    if (!world.isBlockLoaded(pos)) {
      return false;
    }
    if (!world.isRemote) {
      return true;
    }
    Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
    return chunk != null && !chunk.isEmpty();
  }
}
