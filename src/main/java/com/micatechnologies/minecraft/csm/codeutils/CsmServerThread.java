package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Runs server-side work on the server thread, whichever thread the event that asked for it fired
 * on.
 *
 * <p>Forge fires {@code ChunkEvent.Load} and {@code ChunkEvent.Unload} on whatever thread loads or
 * unloads the chunk. In vanilla that is always the server thread, but an asynchronous world editor
 * (FAWE and its forks) loads chunks on its own worker threads. A handler that then writes to plain
 * collections the world tick also reads races the tick: on the Alto server a FAWE
 * {@code //setbiome} corrupted the HVAC chunk-audit queue mid-drain and took the server down
 * (#375). Handlers that touch tick-owned state go through {@link #run} instead.</p>
 *
 * @since 2026.11
 */
public final class CsmServerThread {

  private CsmServerThread() {
  }

  /**
   * Whether the calling thread is the server thread of {@code world}'s server.
   *
   * @param world a server world
   *
   * @return {@code true} on the server thread, or when the world has no server to ask
   */
  public static boolean isServerThread(World world) {
    MinecraftServer server = world.getMinecraftServer();
    return server == null || server.isCallingFromMinecraftThread();
  }

  /**
   * Runs {@code task} now if the caller is on the server thread, otherwise queues it to run on the
   * server thread at the start of its next tick. A task queued off-thread while the server is
   * stopping is dropped, since nothing is left to tick the state it would touch.
   *
   * <p>On the server thread the task runs inline, exactly as if it had been called directly,
   * exceptions included. (Vanilla's {@code addScheduledTask} would also run it inline there, but
   * would swallow an exception into the returned future.) A queued task has to re-resolve
   * anything that may have gone by the time it runs, such as the world's per-world state or the
   * chunk, rather than capture it.</p>
   *
   * @param world the server world the work belongs to
   * @param task  the work
   */
  public static void run(World world, Runnable task) {
    MinecraftServer server = world.getMinecraftServer();
    if (server == null || server.isCallingFromMinecraftThread()) {
      task.run();
    } else if (!server.isServerStopped()) {
      server.addScheduledTask(task);
    }
  }

  /**
   * For a tile entity's {@code onLoad}, which Forge calls from {@code World.addTileEntity} on the
   * thread loading the chunk: runs {@code task} now on the server thread (or the client), else
   * queues it to the server thread, where it runs only if the tile entity is still the live one at
   * its position (not removed, replaced or unloaded meanwhile).
   *
   * @param tile the tile entity loading
   * @param task what its {@code onLoad} does with shared state
   */
  public static void runOnLoad(TileEntity tile, Runnable task) {
    World world = tile.getWorld();
    if (world == null || isServerThread(world)) {
      task.run();
      return;
    }
    run(world, () -> {
      if (isLive(world, tile)) {
        task.run();
      }
    });
  }

  private static boolean isLive(World world, TileEntity tile) {
    BlockPos pos = tile.getPos();
    return !tile.isInvalid() && world.isBlockLoaded(pos) && world.getTileEntity(pos) == tile;
  }

  /**
   * For a tile entity's {@code onChunkUnload} or {@code invalidate} that withdraws this instance
   * from shared state, a registry keyed by the instance or checked against its owner: runs
   * {@code task} now on the server thread (or the client), else queues it to the server thread.
   * For a registry keyed only by position use {@link #runOnUnloadByPos}.
   *
   * @param tile the tile entity going
   * @param task what it withdraws from shared state
   */
  public static void runOnUnload(TileEntity tile, Runnable task) {
    World world = tile.getWorld();
    if (world == null) {
      task.run();
    } else {
      run(world, task);
    }
  }

  /**
   * As {@link #runOnUnload}, for a registry keyed only by position. A queued task is skipped if,
   * by the time it runs, a live tile entity of the same class has loaded at the same position:
   * withdrawing the position then would undo the newcomer's registration rather than this one's.
   *
   * @param tile the tile entity going
   * @param task what it withdraws from shared state
   */
  public static void runOnUnloadByPos(TileEntity tile, Runnable task) {
    World world = tile.getWorld();
    if (world == null || isServerThread(world)) {
      task.run();
      return;
    }
    BlockPos pos = tile.getPos();
    run(world, () -> {
      if (world.isBlockLoaded(pos)) {
        TileEntity now = world.getTileEntity(pos);
        if (now != null && now != tile && !now.isInvalid() && now.getClass() == tile.getClass()) {
          return;
        }
      }
      task.run();
    });
  }
}
