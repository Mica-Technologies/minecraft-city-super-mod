package com.micatechnologies.minecraft.csm.lifesafety;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * How many fire blocks each 16-block chunk section holds, so a detector skips every section with
 * none instead of reading it.
 *
 * <p><b>Why.</b> Every detector reads the space it covers, up to 31 x 31 columns down to the floor,
 * once every 25 seconds. A building has a detector in nearly every room, their areas overlap many
 * times over, and there is almost never a fire in them. On the Alto server that was 3% of the
 * server tick, all of it confirming that nothing was burning.</p>
 *
 * <p><b>How.</b> A section is counted once, when a detector first asks about it, by reading its
 * 4,096 blocks (an all-air section is 0 without reading). After that the count is kept by this
 * class's world listener, which hears every block change that is sent to clients: fire placed by
 * flint and steel, spreading, burning out, and anything a player or machine does. A detector reads
 * a section itself only when its count is above zero, so in a building that is not on fire a
 * detector's scan is a few map lookups.</p>
 *
 * <p>A change made without telling clients (a mod writing chunk storage directly, or
 * {@code setBlockState} without flag 2) is not heard. A count is therefore trusted for five
 * minutes at most and then taken again, which bounds how long such a fire could go unseen. Counts
 * are dropped with their chunk, and a chunk loaded again is counted again.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
final class FireCensus implements IWorldEventListener {

  /** How long a count is trusted before the section is read again, in ticks. */
  private static final long RECOUNT_TICKS = 6000L;

  private static final Map<World, FireCensus> WORLDS = new WeakHashMap<>();

  private final World world;

  /** Fire blocks per section, for the sections counted so far. */
  private final Long2IntOpenHashMap counts = new Long2IntOpenHashMap();

  /** The world time each section was last counted. */
  private final Long2LongOpenHashMap countedAt = new Long2LongOpenHashMap();

  private FireCensus(World world) {
    this.world = world;
    counts.defaultReturnValue(-1);
  }

  /** The census for a server world, created on first use. */
  static FireCensus get(World world) {
    synchronized (WORLDS) {
      FireCensus c = WORLDS.get(world);
      if (c == null) {
        c = new FireCensus(world);
        WORLDS.put(world, c);
        world.addEventListener(c);
      }
      return c;
    }
  }

  /** What a detector looks for. */
  static boolean isFire(IBlockState state) {
    return state.getBlock() == Blocks.FIRE || state.getMaterial() == Material.FIRE;
  }

  private static long key(int chunkX, int chunkZ, int sectionY) {
    return (((long) chunkX & 0xFFFFFFFL) << 32) | (((long) chunkZ & 0xFFFFFFFL) << 4)
        | (sectionY & 15);
  }

  /**
   * Whether any section of a chunk from {@code fromSection} to {@code toSection} may hold fire.
   * Counts the sections not counted yet, or counted too long ago.
   */
  boolean mayHoldFire(Chunk chunk, int fromSection, int toSection) {
    long now = world.getTotalWorldTime();
    boolean any = false;
    for (int sy = fromSection; sy <= toSection; sy++) {
      long k = key(chunk.x, chunk.z, sy);
      int n = counts.get(k);
      if (n < 0 || now - countedAt.get(k) >= RECOUNT_TICKS) {
        n = count(chunk, sy);
        counts.put(k, n);
        countedAt.put(k, now);
      }
      any |= n > 0;
    }
    return any;
  }

  private static int count(Chunk chunk, int sectionY) {
    ExtendedBlockStorage section = chunk.getBlockStorageArray()[sectionY];
    if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
      return 0;
    }
    int n = 0;
    for (int y = 0; y < 16; y++) {
      for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
          if (isFire(section.get(x, y, z))) {
            n++;
          }
        }
      }
    }
    return n;
  }

  // region Keeping the counts

  @Override
  public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState,
      IBlockState newState, int flags) {
    if (oldState == newState || pos.getY() < 0 || pos.getY() > 255) {
      return;
    }
    boolean was = isFire(oldState);
    boolean is = isFire(newState);
    if (was == is) {
      return;
    }
    long k = key(pos.getX() >> 4, pos.getZ() >> 4, pos.getY() >> 4);
    int n = counts.get(k);
    if (n >= 0) {
      counts.put(k, Math.max(0, n + (is ? 1 : -1)));
    }
  }

  private void forgetChunk(int chunkX, int chunkZ) {
    for (int sy = 0; sy < 16; sy++) {
      long k = key(chunkX, chunkZ, sy);
      counts.remove(k);
      countedAt.remove(k);
    }
  }

  /** Drops counts with their chunk and the census with its world. */
  static final class Events {

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
      World w = event.getWorld();
      if (w.isRemote) {
        return;
      }
      FireCensus c;
      synchronized (WORLDS) {
        c = WORLDS.get(w);
      }
      if (c != null) {
        c.forgetChunk(event.getChunk().x, event.getChunk().z);
      }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      World w = event.getWorld();
      synchronized (WORLDS) {
        FireCensus c = WORLDS.remove(w);
        if (c != null) {
          w.removeEventListener(c);
        }
      }
    }
  }

  @Override
  public void notifyLightSet(BlockPos pos) {
  }

  @Override
  public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
  }

  @Override
  public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent soundIn,
      SoundCategory category, double x, double y, double z, float volume, float pitch) {
  }

  @Override
  public void playRecord(SoundEvent soundIn, BlockPos pos) {
  }

  @Override
  public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord,
      double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
  }

  @Override
  public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x,
      double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
  }

  @Override
  public void onEntityAdded(Entity entityIn) {
  }

  @Override
  public void onEntityRemoved(Entity entityIn) {
  }

  @Override
  public void broadcastSound(int soundID, BlockPos pos, int data) {
  }

  @Override
  public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {
  }

  @Override
  public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
  }

  // endregion
}
