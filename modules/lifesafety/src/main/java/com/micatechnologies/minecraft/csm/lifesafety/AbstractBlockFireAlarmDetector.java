package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Abstract base class for fire alarm detector blocks that automatically scan a surrounding
 * area for fire and lava, triggering the linked control panel when a hazard is found.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public abstract class AbstractBlockFireAlarmDetector extends AbstractBlockFireAlarmActivator {

  public static final int VERT_BELOW_BLOCKS_CHECK = 30;
  public static final int RADIUS_AROUND_BLOCKS_CHECK = 15;

  @Override
  public int getBlockTickRate() {
    // Check for fires every 25 seconds. (25 secs x 20 ticks per second = 500 ticks)
    return 500;
  }

  @Override
  public void onTick(World world, BlockPos blockPos, IBlockState blockState) {
    BlockPos foundFirePos = findFire(world, blockPos);
    if (foundFirePos == null) {
      return;
    }

    MinecraftServer minecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();
    if (minecraftServer != null) {
      minecraftServer.sendMessage(new TextComponentString("A fire has been detected at [" +
          foundFirePos.getX() +
          "," +
          foundFirePos.getY() +
          "," +
          foundFirePos.getZ() +
          "]"));
    }

    activateLinkedPanel(world, blockPos, null);
    onFire(world, blockPos, blockState, foundFirePos);
  }

  /**
   * Searches the space this detector covers for fire and returns the first block found, or
   * {@code null} if there is none.
   *
   * <h3>What is covered</h3>
   * A column of blocks under each position within {@link #RADIUS_AROUND_BLOCKS_CHECK}, running
   * down from the detector until it reaches a floor -- any block that blocks movement -- or until
   * it has descended {@link #VERT_BELOW_BLOCKS_CHECK} blocks, whichever comes first. Liquids and
   * fire do not count as floors, so a sprinkler's own discharge cannot blind it.
   * <p>
   * The depth is therefore the height of the room rather than a fixed number, which is the point:
   * a fixed depth is simultaneously too deep for a five-block room and too shallow for an atrium.
   * A normal room now stops after a handful of layers, while an open shaft is still covered to the
   * full backstop depth. It also means a detector no longer alarms for a fire on the floor below,
   * through an intact floor, which it previously did and should not have.
   *
   * <h3>How it is walked</h3>
   * Layer by layer from the top down, carrying a per-column "has hit its floor" flag, rather than
   * column by column from the top down. Both cover the same space, but chunk storage is indexed
   * {@code y << 8 | z << 4 | x}, so descending a column strides 256 entries per step and misses
   * the cache on every read, while walking a layer reads contiguously. The flags let the walk keep
   * the fast order and still stop at the floor, and the whole scan ends early once every column
   * has found its floor.
   * <p>
   * Blocks in the detector's own layer are read for fire but never mark their column as floored.
   * Without that exception a detector mounted flush with the ceiling would mark every column
   * blocked on the very first layer and never see anything below it again.
   * <p>
   * Reads come straight from each chunk's {@link ExtendedBlockStorage} sections, and an all-air
   * section is skipped whole rather than a block at a time. Chunks are checked with
   * {@code isBlockLoaded} first: {@code World.getBlockState} resolves its chunk through
   * {@code provideChunk}, which loads from disk and generates terrain for an absent chunk, so
   * scanning used to pull in neighbouring chunks every 25 seconds. Nothing is lost by skipping
   * them, as fire does not burn in a chunk that is not ticking.
   *
   * @param world  the world to search
   * @param center this detector's position
   *
   * @return the position of a fire block within the covered space, or {@code null} if none
   */
  private static BlockPos findFire(World world, BlockPos center) {
    final int minX = center.getX() - RADIUS_AROUND_BLOCKS_CHECK;
    final int maxX = center.getX() + RADIUS_AROUND_BLOCKS_CHECK;
    final int minZ = center.getZ() - RADIUS_AROUND_BLOCKS_CHECK;
    final int maxZ = center.getZ() + RADIUS_AROUND_BLOCKS_CHECK;
    // Clamped to the world so the section index below is always in range. Out-of-bounds positions
    // read back as air anyway, so clamping loses nothing.
    final int minY = Math.max(0, center.getY() - VERT_BELOW_BLOCKS_CHECK);
    final int maxY = Math.min(255, center.getY());
    if (minY > maxY) {
      return null;
    }

    // One column flag per position in a chunk, reused for each chunk rather than reallocated.
    final boolean[] floored = new boolean[256];

    for (int chunkX = minX >> 4; chunkX <= (maxX >> 4); chunkX++) {
      for (int chunkZ = minZ >> 4; chunkZ <= (maxZ >> 4); chunkZ++) {
        // Ask whether the chunk is present before touching it, so resolving it cannot load it.
        if (!world.isBlockLoaded(new BlockPos(chunkX << 4, center.getY(), chunkZ << 4))) {
          continue;
        }
        final Chunk chunk = world.getChunk(chunkX, chunkZ);
        // Clip the covered area to this chunk's columns. Columns in one chunk never affect
        // another's, so each chunk carries its own flags and its own early exit.
        final int fromX = Math.max(minX, chunkX << 4);
        final int toX = Math.min(maxX, (chunkX << 4) + 15);
        final int fromZ = Math.max(minZ, chunkZ << 4);
        final int toZ = Math.min(maxZ, (chunkZ << 4) + 15);
        final int width = toX - fromX + 1;

        java.util.Arrays.fill(floored, false);
        int unflooredColumns = width * (toZ - fromZ + 1);

        final ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (int y = maxY; y >= minY && unflooredColumns > 0; y--) {
          final ExtendedBlockStorage section = sections[y >> 4];
          if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
            // Nothing but air in this section: no fire to find and no floor to hit, so drop
            // straight to the section below instead of reading 16 layers of it.
            y = (y >> 4) << 4;
            continue;
          }
          final boolean isDetectorLayer = y == center.getY();
          for (int z = fromZ; z <= toZ; z++) {
            for (int x = fromX; x <= toX; x++) {
              final int column = (z - fromZ) * width + (x - fromX);
              if (floored[column]) {
                continue;
              }
              final IBlockState state = section.get(x & 15, y & 15, z & 15);
              final Block block = state.getBlock();
              if (block == Blocks.FIRE || state.getMaterial() == Material.FIRE) {
                return new BlockPos(x, y, z);
              }
              if (!isDetectorLayer && state.getMaterial().blocksMovement()) {
                floored[column] = true;
                unflooredColumns--;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Called when this detector finds fire in its search area.
   *
   * @param world      the world
   * @param blockPos   this detector's position
   * @param blockState this detector's state
   * @param firePos    where the fire was found. Previously the detector located the fire and then
   *                   threw the position away, which is why sprinklers could only discharge
   *                   underneath themselves and never at what they had detected.
   */
  abstract public void onFire(World world, BlockPos blockPos, IBlockState blockState,
      BlockPos firePos);
}
