package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;

/**
 * Which block columns a signal cluster's bracket reaches over, as a hint for the search that has
 * to find out.
 *
 * <p><b>What this is for.</b> A payload looking for the mount it hangs from checks straight up
 * first, which is where an ordinary mount is. A clustered payload sits <em>beside</em> its mast,
 * so when that fails the search sweeps the columns a bracket could reach from -- twenty-four
 * offsets at three heights, seventy-two tile entity lookups, every time. Almost every signal head
 * in a world hangs on a pole or a mast arm and pays all seventy-two to learn nothing. This turns
 * the overwhelmingly common negative answer into one hash lookup.
 *
 * <p><b>Every way this can be wrong is a slow answer, not a wrong one.</b> That is the property
 * the design is built around, because a registry that has to be exactly right about a thing the
 * game can invalidate from a dozen directions would be a worse bug than the cost it saves:
 *
 * <ul>
 *   <li>An entry that lingers after its cluster is gone makes the search run as it always did,
 *       and the search itself decides what is true. Leaks cost speed.</li>
 *   <li>Client and server both register their own copies of a cluster into this one table. The
 *       union over-reports, which is the harmless direction; nothing here hands a caller a tile
 *       entity, so a client cannot be given the server's.</li>
 *   <li>Dimension is not part of the key, so the same column in the Nether over-reports too.</li>
 * </ul>
 *
 * <p>The one direction that would be wrong -- a column reported as uncovered while a cluster does
 * cover it -- is prevented by counting rather than flagging. Two brackets that overlap a column
 * both hold it, and the first of them to be taken down cannot drop it out from under the second.
 */
public final class SpanWireClusterColumns {

  /**
   * Packed column to the number of brackets currently claiming it.
   *
   * <p>Concurrent because tile entities on the integrated server and the client both write here,
   * from their own threads.
   */
  private static final Map<Long, Integer> CLAIMS = new ConcurrentHashMap<>();

  private SpanWireClusterColumns() {
  }

  /**
   * A column's key. Y is deliberately absent: a bracket's reach is a horizontal thing, and the
   * payload asking is a block or three below the mount that covers it.
   */
  private static long key(int x, int z) {
    return ((long) x << 32) | (z & 0xFFFFFFFFL);
  }

  /** Claims every column in the collection on behalf of one bracket. */
  public static void publish(Collection<BlockPos> columns) {
    for (BlockPos column : columns) {
      CLAIMS.merge(key(column.getX(), column.getZ()), 1, Integer::sum);
    }
  }

  /**
   * Releases one bracket's claim on every column in the collection.
   *
   * <p>Callers pass back exactly what they last published, which is what keeps the counts honest
   * across a cluster that changes width.
   */
  public static void withdraw(Collection<BlockPos> columns) {
    for (BlockPos column : columns) {
      CLAIMS.computeIfPresent(key(column.getX(), column.getZ()),
          (ignored, count) -> count <= 1 ? null : count - 1);
    }
  }

  /**
   * Whether any bracket claims this block's column.
   *
   * <p>False is a promise worth acting on: no cluster anywhere has said it reaches here, so the
   * sweep would find nothing. True is only a hint -- the sweep still runs, and still decides.
   */
  public static boolean anyClusterCovers(BlockPos pos) {
    return !CLAIMS.isEmpty() && CLAIMS.containsKey(key(pos.getX(), pos.getZ()));
  }

  // There is deliberately no session-wide reset here, unlike the display list cache next door.
  // That cache holds driver and GPU memory and clearing it wholesale is free of consequence; this
  // holds two longs per column, and clearing it while a cluster is still loaded is the single
  // thing that could make a lookup answer "no cluster here" about a cluster that exists. Claims
  // are released precisely, by the bracket that made them, when it is invalidated or its chunk
  // unloads. A claim that outlives its bracket costs one wasted sweep and a few dozen bytes.
}
