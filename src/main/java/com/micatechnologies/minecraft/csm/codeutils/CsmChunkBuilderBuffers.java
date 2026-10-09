package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The direct memory held by vanilla's chunk builders, and a way to give back what they grew.
 *
 * <p>Vanilla builds chunk meshes with a pool of {@link RegionRenderCacheBuilder}s, one
 * {@link BufferBuilder} per render layer each, all in direct (off-heap) memory. The pool is
 * {@code min(cores x 10, 30% of the heap / 10 MB)} builders, about 10.5 MB each to begin with: 1.6
 * GB on a sixteen-core machine with a 16 GB heap. A builder that meets a chunk section heavier
 * than its buffer grows it in 2 MB steps and keeps the larger buffer for the rest of the session,
 * so the pool's direct memory only ever rises, a builder at a time, as the player meets heavy
 * sections. Direct memory is capped by default at the heap size, and when it runs out the client
 * dies with "OutOfMemoryError: Direct buffer memory" while building a chunk. That is the
 * crash a dense CSM test grid produced, and it comes late in a session because the growth is
 * gradual.</p>
 *
 * <p>{@link #describe()} reports the pool; {@link #trim(long)} swaps each idle builder's oversized
 * buffers for new ones at vanilla's starting size and frees the old buffers at once, rather than
 * leaving them to a garbage collection that a modpack's {@code -XX:+DisableExplicitGC} can keep
 * from ever coming in time. Only builders waiting in the dispatcher's free queue are touched, each
 * taken out of the queue while it is trimmed, so no worker can be using one; the replaced buffer
 * is referenced by nothing else once its slot is overwritten.</p>
 *
 * <p>The fields are private with no accessor. Each is found by its SRG name (the release jar runs
 * against obfuscated Minecraft), then by its MCP name (a dev workspace). If any cannot be found
 * the report says so and nothing is trimmed.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class CsmChunkBuilderBuffers {

  /** Vanilla's starting size of each layer's buffer, in ints, by {@link BlockRenderLayer}. */
  private static final int[] DEFAULT_INTS = defaultInts();

  @Nullable
  private static final Field RENDER_DISPATCHER =
      find(RenderGlobal.class, "field_174995_M", "renderDispatcher");
  @Nullable
  private static final Field FREE_BUILDERS =
      find(ChunkRenderDispatcher.class, "field_178520_e", "queueFreeRenderBuilders");
  @Nullable
  private static final Field COUNT_BUILDERS =
      find(ChunkRenderDispatcher.class, "field_188249_c", "countRenderBuilders");
  @Nullable
  private static final Field WORLD_RENDERERS =
      find(RegionRenderCacheBuilder.class, "field_179040_a", "worldRenderers");
  @Nullable
  private static final Field BYTE_BUFFER =
      find(BufferBuilder.class, "field_179001_a", "byteBuffer");

  private CsmChunkBuilderBuffers() {
  }

  private static int[] defaultInts() {
    int[] ints = new int[BlockRenderLayer.values().length];
    ints[BlockRenderLayer.SOLID.ordinal()] = 2097152;
    ints[BlockRenderLayer.CUTOUT.ordinal()] = 131072;
    ints[BlockRenderLayer.CUTOUT_MIPPED.ordinal()] = 131072;
    ints[BlockRenderLayer.TRANSLUCENT.ordinal()] = 262144;
    return ints;
  }

  @Nullable
  private static Field find(Class<?> owner, String srg, String mcp) {
    try {
      return ObfuscationReflectionHelper.findField(owner, srg);
    } catch (Throwable srgFailed) {
      try {
        Field f = owner.getDeclaredField(mcp);
        f.setAccessible(true);
        return f;
      } catch (Throwable mcpFailed) {
        Csm.getLogger().warn("Cannot read {}.{} ({}); chunk builder buffers cannot be measured "
            + "or trimmed.", owner.getSimpleName(), mcp, srgFailed.toString());
        return null;
      }
    }
  }

  /** Whether every field this needs was found. */
  public static boolean available() {
    return RENDER_DISPATCHER != null && FREE_BUILDERS != null && COUNT_BUILDERS != null
        && WORLD_RENDERERS != null && BYTE_BUFFER != null;
  }

  /** Vanilla's starting size of a layer's buffer, in bytes. */
  static long defaultBytes(int layer) {
    return DEFAULT_INTS[layer] * 4L;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private static BlockingQueue<RegionRenderCacheBuilder> freeBuilders() throws Exception {
    RenderGlobal rg = Minecraft.getMinecraft().renderGlobal;
    if (rg == null) {
      return null;
    }
    Object dispatcher = RENDER_DISPATCHER.get(rg);
    if (dispatcher == null) {
      return null;
    }
    return (BlockingQueue<RegionRenderCacheBuilder>) FREE_BUILDERS.get(dispatcher);
  }

  private static int builderCount() throws Exception {
    Object dispatcher = RENDER_DISPATCHER.get(Minecraft.getMinecraft().renderGlobal);
    return dispatcher == null ? 0 : COUNT_BUILDERS.getInt(dispatcher);
  }

  private static long capacity(BufferBuilder buffer) throws Exception {
    ByteBuffer bytes = (ByteBuffer) BYTE_BUFFER.get(buffer);
    return bytes == null ? 0 : bytes.capacity();
  }

  /**
   * Reports the builder pool: how many builders there are, how many are idle (only those are
   * measured), the direct memory their buffers hold, and per layer the largest buffer and how many
   * have grown past vanilla's starting size. Client thread.
   *
   * @return the chat lines
   */
  public static List<String> describe() {
    List<String> lines = new ArrayList<>();
    if (!available()) {
      lines.add("chunkbuffers: the chunk builder fields could not be found in this game");
      return lines;
    }
    try {
      BlockingQueue<RegionRenderCacheBuilder> queue = freeBuilders();
      if (queue == null) {
        lines.add("chunkbuffers: no world renderer yet");
        return lines;
      }
      int layers = BlockRenderLayer.values().length;
      long[] total = new long[layers];
      long[] largest = new long[layers];
      int[] grown = new int[layers];
      int idle = 0;
      for (RegionRenderCacheBuilder builder : queue) {
        idle++;
        BufferBuilder[] buffers = (BufferBuilder[]) WORLD_RENDERERS.get(builder);
        for (int i = 0; i < layers; i++) {
          long cap = capacity(buffers[i]);
          total[i] += cap;
          largest[i] = Math.max(largest[i], cap);
          if (cap > defaultBytes(i)) {
            grown[i]++;
          }
        }
      }
      long sum = 0;
      for (long t : total) {
        sum += t;
      }
      lines.add(String.format(Locale.ROOT,
          "chunkbuffers: %d builders, %d idle and measured, holding %.1f MB direct "
              + "(%.1f MB at vanilla's starting size)",
          builderCount(), idle, mb(sum), mb(idle * startingBytesPerBuilder())));
      for (BlockRenderLayer layer : BlockRenderLayer.values()) {
        int i = layer.ordinal();
        lines.add(String.format(Locale.ROOT,
            "  %s: %.1f MB in all, largest %.1f MB (starts %.1f MB), %d grown",
            layer.name(), mb(total[i]), mb(largest[i]), mb(defaultBytes(i)), grown[i]));
      }
      lines.add(String.format(Locale.ROOT, "  direct memory in use: %.1f MB",
          mb(CsmDirectMemory.used())));
    } catch (Exception e) {
      lines.add("chunkbuffers: could not measure (" + e + ")");
    }
    return lines;
  }

  /** What one builder holds at vanilla's starting size, in bytes. */
  private static long startingBytesPerBuilder() {
    long sum = 0;
    for (int i = 0; i < DEFAULT_INTS.length; i++) {
      sum += defaultBytes(i);
    }
    return sum;
  }

  /**
   * Replaces every idle builder's buffers larger than {@code keepBytes} (or than their starting
   * size, if that is larger) with new ones at vanilla's starting size, and frees the old ones now.
   * Client thread.
   *
   * @param keepBytes buffers up to this size are left alone
   *
   * @return the bytes given back
   */
  public static long trim(long keepBytes) {
    if (!available()) {
      return 0;
    }
    long freed = 0;
    try {
      BlockingQueue<RegionRenderCacheBuilder> queue = freeBuilders();
      if (queue == null) {
        return 0;
      }
      // Take each idle builder out while it is trimmed, so a worker cannot pick it up mid-swap,
      // and put it back after. A builder a worker returns meanwhile is simply not visited.
      int visits = queue.size();
      for (int n = 0; n < visits; n++) {
        RegionRenderCacheBuilder builder = queue.poll();
        if (builder == null) {
          break;
        }
        try {
          BufferBuilder[] buffers = (BufferBuilder[]) WORLD_RENDERERS.get(builder);
          for (int i = 0; i < buffers.length && i < DEFAULT_INTS.length; i++) {
            ByteBuffer old = (ByteBuffer) BYTE_BUFFER.get(buffers[i]);
            if (old == null || old.capacity() <= Math.max(keepBytes, defaultBytes(i))) {
              continue;
            }
            buffers[i] = new BufferBuilder(DEFAULT_INTS[i]);
            freed += old.capacity() - defaultBytes(i);
            CsmDirectMemory.free(old);
          }
        } finally {
          queue.add(builder);
        }
      }
    } catch (Exception e) {
      Csm.getLogger().warn("Could not trim the chunk builder buffers", e);
    }
    return freed;
  }

  static double mb(long bytes) {
    return bytes / (1024.0 * 1024.0);
  }
}
