package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link ThermalScanner#assignRegions} keeps each merged group's volume at its root instead of
 * summing every bucket for every question. The regions it hands out must be exactly the ones the
 * old quadratic version gave, bucket for bucket and index for index, or rooms would come back from
 * a rescan with their temperatures in different regions.
 */
class ThermalRegionAssignmentTest {

  private static final int[][] DIRS = {
      {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

  @Test
  void sameRegionsAsTheQuadraticVersionOnRandomSpaces() {
    Random rnd = new Random(20261002L);
    for (int trial = 0; trial < 120; trial++) {
      LongArrayList cells = randomSpace(rnd);
      // Built by the same insertions, so both iterate their cells (and buckets) in one order.
      ThermalScanner.Result fast = result(cells);
      ThermalScanner.Result slow = result(cells);
      ThermalScanner.assignRegions(fast);
      referenceAssignRegions(slow);
      assertEquals(slow.regionCount, fast.regionCount, "trial " + trial + ": region count");
      assertEquals(slow.regionOf, fast.regionOf, "trial " + trial + ": bucket to region");
    }
  }

  /**
   * A blob of cells grown from a few random boxes and random walks, sized from a handful of cells
   * to tens of thousands, compact or sprawling, so both the single-region and the split branch and
   * plenty of slivers are exercised.
   */
  private static LongArrayList randomSpace(Random rnd) {
    LongArrayList r = new LongArrayList();
    int boxes = 1 + rnd.nextInt(6);
    int ox = rnd.nextInt(64) - 32;
    int oy = rnd.nextInt(32);
    int oz = rnd.nextInt(64) - 32;
    for (int b = 0; b < boxes; b++) {
      int x0 = ox + rnd.nextInt(40) - 20;
      int y0 = oy + rnd.nextInt(8);
      int z0 = oz + rnd.nextInt(40) - 20;
      int w = 1 + rnd.nextInt(rnd.nextBoolean() ? 12 : 45);
      int h = 1 + rnd.nextInt(rnd.nextBoolean() ? 4 : 14);
      int d = 1 + rnd.nextInt(rnd.nextBoolean() ? 12 : 45);
      for (int x = x0; x < x0 + w; x++) {
        for (int y = y0; y < y0 + h; y++) {
          for (int z = z0; z < z0 + d; z++) {
            r.add(ThermalScanner.pack(x, y, z));
          }
        }
      }
    }
    int walks = rnd.nextInt(4);
    for (int k = 0; k < walks; k++) {
      int x = ox;
      int y = oy;
      int z = oz;
      for (int step = 0; step < 400; step++) {
        int[] dir = DIRS[rnd.nextInt(6)];
        x += dir[0];
        y = Math.max(0, y + dir[1]);
        z += dir[2];
        r.add(ThermalScanner.pack(x, y, z));
      }
    }
    return r;
  }

  private static ThermalScanner.Result result(LongArrayList cells) {
    ThermalScanner.Result r = new ThermalScanner.Result();
    for (int i = 0; i < cells.size(); i++) {
      r.cells.add(cells.getLong(i));
    }
    r.regionOf.defaultReturnValue(-1);
    return r;
  }

  // region The version before 2026-10, kept as the reference

  private static void referenceAssignRegions(ThermalScanner.Result r) {
    Long2IntOpenHashMap bucketVolume = new Long2IntOpenHashMap();
    LongIterator it = r.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      bucketVolume.addTo(ThermalScanner.regionKey(ThermalScanner.unpackX(c),
          ThermalScanner.unpackY(c), ThermalScanner.unpackZ(c)), 1);
    }
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxZ = Integer.MIN_VALUE;
    it = r.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      minX = Math.min(minX, ThermalScanner.unpackX(c));
      maxX = Math.max(maxX, ThermalScanner.unpackX(c));
      minY = Math.min(minY, ThermalScanner.unpackY(c));
      maxY = Math.max(maxY, ThermalScanner.unpackY(c));
      minZ = Math.min(minZ, ThermalScanner.unpackZ(c));
      maxZ = Math.max(maxZ, ThermalScanner.unpackZ(c));
    }
    boolean compact = maxX - minX < ThermalScanner.SINGLE_REGION_SPAN
        && maxZ - minZ < ThermalScanner.SINGLE_REGION_SPAN
        && maxY - minY < ThermalScanner.SINGLE_REGION_HEIGHT;
    Long2LongOpenHashMap parent = new Long2LongOpenHashMap();
    if (r.cells.size() <= ThermalScanner.SINGLE_REGION_MAX && compact) {
      long root = bucketVolume.keySet().iterator().nextLong();
      for (long b : bucketVolume.keySet()) {
        parent.put(b, root);
      }
    } else {
      for (int pass = 0; pass < 2; pass++) {
        for (long b : bucketVolume.keySet()) {
          long rootB = find(parent, b);
          if (volumeOf(bucketVolume, parent, rootB) >= ThermalScanner.MIN_REGION_CELLS) {
            continue;
          }
          int bx = ThermalScanner.unpackX(b);
          int by = ThermalScanner.unpackY(b);
          int bz = ThermalScanner.unpackZ(b);
          long best = -1;
          int bestVolume = -1;
          boolean found = false;
          for (int[] d : DIRS) {
            long n = ThermalScanner.pack(bx + d[0], by + d[1], bz + d[2]);
            if (!bucketVolume.containsKey(n)) {
              continue;
            }
            long rootN = find(parent, n);
            if (rootN == rootB) {
              continue;
            }
            int v = volumeOf(bucketVolume, parent, rootN);
            if (v > bestVolume) {
              bestVolume = v;
              best = rootN;
              found = true;
            }
          }
          if (found) {
            parent.put(rootB, best);
          }
        }
      }
    }
    for (long b : bucketVolume.keySet()) {
      long root = find(parent, b);
      int idx = r.regionOf.get(root);
      if (idx < 0) {
        idx = r.regionCount++;
        r.regionOf.put(root, idx);
      }
      r.regionOf.put(b, idx);
    }
  }

  private static long find(Long2LongOpenHashMap parent, long b) {
    long cur = b;
    while (parent.containsKey(cur) && parent.get(cur) != cur) {
      cur = parent.get(cur);
    }
    return cur;
  }

  private static int volumeOf(Long2IntOpenHashMap bucketVolume, Long2LongOpenHashMap parent,
      long root) {
    int v = 0;
    for (Long2IntMap.Entry e : bucketVolume.long2IntEntrySet()) {
      if (find(parent, e.getLongKey()) == root) {
        v += e.getIntValue();
      }
    }
    return v;
  }

  // endregion
}
