package com.micatechnologies.minecraft.csm.codeutils.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

/**
 * A real {@link World} with nothing behind it but a box of chunks held in memory: a stone floor,
 * sky light, and vanilla's light engine. Enough to place light-up air and time the light updates
 * it causes on the server side, without a save, a server or a client.
 *
 * <p>What it does not cost: the packets a real server sends for each changed block, and the
 * client's re-mesh of each section a light change touches.</p>
 */
final class LightTestWorld extends World {

  private final Long2ObjectMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();

  /**
   * A world of the chunks from {@code (cx0, cz0)} to {@code (cx1, cz1)}, solid stone up to and
   * including {@code floorY}.
   */
  LightTestWorld(int cx0, int cz0, int cx1, int cz1, int floorY) {
    this(cx0, cz0, cx1, cz1, floorY, (x, y, z) -> false);
  }

  /** Stone also wherever {@code extra} says, built into the chunks before their light is made. */
  interface Solid {

    boolean at(int x, int y, int z);
  }

  /** As the plain world, with stone also wherever {@code extra} holds. */
  LightTestWorld(int cx0, int cz0, int cx1, int cz1, int floorY, Solid extra) {
    super(null, new WorldInfo(new WorldSettings(0L, GameType.CREATIVE, false, false,
            WorldType.FLAT), "csm-light-test"), new WorldProviderSurface(), new Profiler(),
        false);
    provider.setWorld(this);
    chunkProvider = createChunkProvider();
    IBlockState stone = Blocks.STONE.getDefaultState();
    for (int cx = cx0; cx <= cx1; cx++) {
      for (int cz = cz0; cz <= cz1; cz++) {
        ChunkPrimer primer = new ChunkPrimer();
        for (int x = 0; x < 16; x++) {
          for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 256; y++) {
              if (y <= floorY || extra.at((cx << 4) + x, y, (cz << 4) + z)) {
                primer.setBlockState(x, y, z, stone);
              }
            }
          }
        }
        Chunk chunk = new Chunk(this, primer, cx, cz);
        chunk.generateSkylightMap();
        chunk.setTerrainPopulated(true);
        chunk.setLightPopulated(true);
        chunks.put(ChunkPos.asLong(cx, cz), chunk);
      }
    }
  }

  @Override
  protected IChunkProvider createChunkProvider() {
    return new IChunkProvider() {
      @Nullable
      @Override
      public Chunk getLoadedChunk(int x, int z) {
        return chunks.get(ChunkPos.asLong(x, z));
      }

      @Override
      public Chunk provideChunk(int x, int z) {
        Chunk chunk = chunks.get(ChunkPos.asLong(x, z));
        if (chunk == null) {
          throw new IllegalStateException("chunk " + x + ", " + z + " is outside the test world");
        }
        return chunk;
      }

      @Override
      public boolean tick() {
        return false;
      }

      @Override
      public String makeString() {
        return "LightTestWorld";
      }

      @Override
      public boolean isChunkGeneratedAt(int x, int z) {
        return chunks.containsKey(ChunkPos.asLong(x, z));
      }
    };
  }

  @Override
  protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
    return chunks.containsKey(ChunkPos.asLong(x, z));
  }
}
