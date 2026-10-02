package com.micatechnologies.minecraft.csm.hvac;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Finds the enclosed air space around a cell, splits it into regions and measures the envelope
 * of each region: how much heat it holds, how readily it loses that heat and to what, and how
 * much open face it shares with each neighbouring region.
 *
 * <p>The space is a six-connected flood fill through {@link ThermalCellSource#AIR} cells. Air
 * that can see the sky is not part of it: a face onto such a cell is an <em>opening</em> (a
 * doorway, a window hole) and loses heat ten times as fast as a wall.</p>
 *
 * <p><b>Regions.</b> A space's cells are grouped by a fixed world grid of
 * {@value #REGION_XZ} x {@value #REGION_Y} x {@value #REGION_XZ} blocks. Each region has its own
 * temperature in the simulation and mixes with its neighbours through the open faces they share.
 * That is what lets four zones of one open-plan store hold four different setpoints, lets a tall
 * atrium be warmer under its roof than on its floor, and lets the HUD change as a player walks
 * across a big room. A compact space -- up to {@value #SINGLE_REGION_MAX} cells, and under
 * {@value #SINGLE_REGION_SPAN} blocks across and {@value #SINGLE_REGION_HEIGHT} high -- is a single
 * region, well mixed as an ordinary room is; a long hallway or a tall stair hall is not. In a split
 * space, the slivers the grid leaves along walls and ceilings join their neighbours.</p>
 *
 * <p><b>Walls.</b> Each face onto a wall is followed through the wall, up to
 * {@link #THICK_PROBE} cells:</p>
 * <ul>
 *   <li>coming out in this same space, it is a partition inside the space and loses nothing;</li>
 *   <li>coming out in other enclosed air, the far cell is recorded, so the simulation can couple
 *   this region to whatever space owns that cell (the flat next door) once it is known;</li>
 *   <li>coming out in the open, it is an exterior wall conducting to the outdoors, weaker the
 *   thicker it is;</li>
 *   <li>never coming out, it is ground or a very thick wall, conducting weakly to the ground.</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class ThermalScanner {

  /**
   * Largest space, in air cells, that is treated as a room. Anything bigger -- a cave system, the
   * underside of a forest canopy -- is not conditionable and reads the outdoor temperature.
   * 40,000 cells is a 50 x 50 x 16 hall.
   */
  public static final int MAX_CELLS = 40_000;

  /** Horizontal size of a region, blocks. */
  public static final int REGION_XZ = 8;

  /** Vertical size of a region, blocks. */
  public static final int REGION_Y = 4;

  /** A space of at most this many cells, and no longer than the spans below, is one region. */
  public static final int SINGLE_REGION_MAX = 2_000;

  /** Longest horizontal span of a single-region space: a long hallway is not well mixed. */
  static final int SINGLE_REGION_SPAN = 20;

  /** Tallest single-region space: a tall room stratifies. */
  static final int SINGLE_REGION_HEIGHT = 8;

  /** A grid cell holding fewer of a space's cells than this joins a neighbouring region. */
  static final int MIN_REGION_CELLS = 64;

  /** How far a wall face is followed to find out what is on its other side. */
  static final int THICK_PROBE = 3;

  /** Heat conducted per second, per degree, through one face of a one-block stone wall. */
  public static final float U_FACE = 0.0027f;

  /**
   * How much faster an opening loses heat than a wall face: air pours through a doorway, so an
   * open door is worth dozens of square metres of wall, and in a hard winter the room behind it
   * needs roughly twice the heat.
   */
  public static final float OPENING_FACTOR = 40.0f;

  /** How much slower ground (or a wall thicker than the probe) conducts than a plain wall. */
  public static final float GROUND_FACTOR = 0.3f;

  /** What a scan found. */
  public enum Status {
    /** A space: the cells, regions and envelope are filled in. */
    OK,
    /** The start cell is not enclosed air (it sees the sky, or is inside a wall). */
    NOT_ENCLOSED,
    /** The flood passed {@link #MAX_CELLS}; {@link Result#cells} holds what it visited. */
    TOO_LARGE,
    /** The flood reached a chunk that is not loaded, so the space cannot be known yet. */
    UNLOADED
  }

  /** The result of one scan. */
  public static final class Result {

    public Status status;

    /** The space's cells, packed with {@link #pack}. */
    public final LongOpenHashSet cells = new LongOpenHashSet();

    /** Region grid key ({@link #regionKey}) to region index. */
    public final Long2IntOpenHashMap regionOf = new Long2IntOpenHashMap();

    public int regionCount;

    /** Per region: air cells. */
    public final IntArrayList volume = new IntArrayList();

    /** Per region: envelope faces (walls, openings, ground). */
    public final IntArrayList envelopeFaces = new IntArrayList();

    /** Per region: conductance to the outdoors through exterior walls, per second per degree. */
    public final FloatArrayList exteriorUA = new FloatArrayList();

    /** Per region: conductance to the outdoors through openings. */
    public final FloatArrayList openingUA = new FloatArrayList();

    /** Per region: conductance to the ground. */
    public final FloatArrayList groundUA = new FloatArrayList();

    /** Per region: lowest cell y, for reports. */
    public final IntArrayList minY = new IntArrayList();

    /** Faces onto enclosed air beyond a wall: the far cell, the region it touches, conductance. */
    public final LongArrayList beyondCell = new LongArrayList();
    public final IntArrayList beyondRegion = new IntArrayList();
    public final FloatArrayList beyondUA = new FloatArrayList();

    /**
     * Open interfaces between regions: region a, region b, the number of cell faces they share,
     * and whether b sits directly above a (so the simulation can let warm air rise).
     */
    public final IntArrayList ifaceA = new IntArrayList();
    public final IntArrayList ifaceB = new IntArrayList();
    public final IntArrayList ifaceFaces = new IntArrayList();
    public final IntArrayList ifaceVertical = new IntArrayList();

    /** Total openings, for reports. */
    public int openingFaces;

    /**
     * For {@link Status#UNLOADED}: the x and z of the first unloaded cell the flood reached, so
     * the caller can wait for that chunk rather than flood again while it is still unloaded.
     */
    public int unloadedX;
    public int unloadedZ;

    /** Region index of a cell of this space. */
    public int regionOfCell(int x, int y, int z) {
      return regionOf.getOrDefault(regionKey(x, y, z), -1);
    }
  }

  private static final int[][] DIRS = {
      {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

  private ThermalScanner() {
  }

  /**
   * Scans the space containing the given cell.
   *
   * @param src the cells to read
   * @param sx  start x
   * @param sy  start y
   * @param sz  start z
   *
   * @return what was found; never null
   */
  public static Result scan(ThermalCellSource src, int sx, int sy, int sz) {
    Result r = new Result();
    byte startKind = src.classify(sx, sy, sz);
    if (startKind == ThermalCellSource.UNLOADED) {
      r.status = Status.UNLOADED;
      r.unloadedX = sx;
      r.unloadedZ = sz;
      return r;
    }
    if (startKind != ThermalCellSource.AIR) {
      r.status = Status.NOT_ENCLOSED;
      return r;
    }

    LongOpenHashSet cells = r.cells;
    LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    long start = pack(sx, sy, sz);
    cells.add(start);
    queue.enqueue(start);

    while (!queue.isEmpty()) {
      long cur = queue.dequeueLong();
      int cx = unpackX(cur);
      int cy = unpackY(cur);
      int cz = unpackZ(cur);
      for (int[] d : DIRS) {
        int nx = cx + d[0];
        int ny = cy + d[1];
        int nz = cz + d[2];
        long key = pack(nx, ny, nz);
        if (cells.contains(key)) {
          continue;
        }
        byte kind = src.classify(nx, ny, nz);
        if (kind == ThermalCellSource.UNLOADED) {
          r.status = Status.UNLOADED;
          r.unloadedX = nx;
          r.unloadedZ = nz;
          return r;
        }
        if (kind != ThermalCellSource.AIR) {
          continue;
        }
        cells.add(key);
        if (cells.size() > MAX_CELLS) {
          r.status = Status.TOO_LARGE;
          return r;
        }
        queue.enqueue(key);
      }
    }

    measure(src, r);
    r.status = Status.OK;
    return r;
  }

  /** Assigns regions, then walks every face of the space and sorts it by what is beyond it. */
  private static void measure(ThermalCellSource src, Result r) {
    LongOpenHashSet cells = r.cells;
    r.regionOf.defaultReturnValue(-1);

    // Regions first, so an interface can name both of its sides.
    assignRegions(r);
    LongIterator it = cells.iterator();
    while (it.hasNext()) {
      long cur = it.nextLong();
      int y = unpackY(cur);
      int idx = r.regionOf.get(regionKey(unpackX(cur), y, unpackZ(cur)));
      r.volume.set(idx, r.volume.getInt(idx) + 1);
      if (y < r.minY.getInt(idx)) {
        r.minY.set(idx, y);
      }
    }

    Long2IntOpenHashMap ifaceIndex = new Long2IntOpenHashMap();
    ifaceIndex.defaultReturnValue(-1);

    it = cells.iterator();
    while (it.hasNext()) {
      long cur = it.nextLong();
      int cx = unpackX(cur);
      int cy = unpackY(cur);
      int cz = unpackZ(cur);
      int region = r.regionOf.get(regionKey(cx, cy, cz));
      for (int[] d : DIRS) {
        int nx = cx + d[0];
        int ny = cy + d[1];
        int nz = cz + d[2];
        if (cells.contains(pack(nx, ny, nz))) {
          int other = r.regionOf.get(regionKey(nx, ny, nz));
          // Count each shared face once, from the lower-indexed side.
          if (other != region && region < other) {
            long pairKey = ((long) region << 32) | other;
            int fi = ifaceIndex.get(pairKey);
            if (fi < 0) {
              fi = r.ifaceA.size();
              ifaceIndex.put(pairKey, fi);
              r.ifaceA.add(region);
              r.ifaceB.add(other);
              r.ifaceFaces.add(0);
              // +1: b above a, -1: b below a, 0: side by side.
              r.ifaceVertical.add(d[1]);
            }
            r.ifaceFaces.set(fi, r.ifaceFaces.getInt(fi) + 1);
          }
          continue;
        }
        byte kind = src.classify(nx, ny, nz);
        r.envelopeFaces.set(region, r.envelopeFaces.getInt(region) + 1);
        if (kind == ThermalCellSource.SKY) {
          r.openingFaces++;
          addTo(r.openingUA, region, U_FACE * OPENING_FACTOR);
        } else if (kind == ThermalCellSource.WALL) {
          classifyWallFace(src, r, region, nx, ny, nz, d, src.wallFactor(nx, ny, nz));
        } else {
          // A chunk that went away between the flood and this pass: an exterior wall.
          addTo(r.exteriorUA, region, U_FACE);
        }
      }
    }
  }

  /**
   * Maps every grid cell the space touches to a region index. A space of up to
   * {@link #SINGLE_REGION_MAX} cells is one region: an ordinary room is well mixed, and cutting it
   * by a world-aligned grid would only make up a temperature difference between arbitrary slivers
   * of it. In a bigger space, a grid cell holding fewer than {@link #MIN_REGION_CELLS} of the
   * space's cells (the sliver a wall or ceiling line leaves in the next grid cell) joins its
   * largest neighbour instead of standing alone.
   */
  static void assignRegions(Result r) {
    Long2IntOpenHashMap bucketVolume = new Long2IntOpenHashMap();
    LongIterator it = r.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      bucketVolume.addTo(regionKey(unpackX(c), unpackY(c), unpackZ(c)), 1);
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
      minX = Math.min(minX, unpackX(c));
      maxX = Math.max(maxX, unpackX(c));
      minY = Math.min(minY, unpackY(c));
      maxY = Math.max(maxY, unpackY(c));
      minZ = Math.min(minZ, unpackZ(c));
      maxZ = Math.max(maxZ, unpackZ(c));
    }
    boolean compact = maxX - minX < SINGLE_REGION_SPAN && maxZ - minZ < SINGLE_REGION_SPAN
        && maxY - minY < SINGLE_REGION_HEIGHT;
    Long2LongOpenHashMap parent = new Long2LongOpenHashMap();
    // Cells in each merged group, kept at its root, so a group's volume is one lookup. It was
    // summed over every bucket each time a sliver asked, which grows with the square of the
    // buckets in a space of many small ones (a comb of corridors).
    Long2IntOpenHashMap groupVolume = new Long2IntOpenHashMap(bucketVolume);
    if (r.cells.size() <= SINGLE_REGION_MAX && compact) {
      long root = bucketVolume.keySet().iterator().nextLong();
      for (long b : bucketVolume.keySet()) {
        parent.put(b, root);
      }
    } else {
      // Two passes, so a sliver whose best neighbour was itself a sliver still lands somewhere big.
      for (int pass = 0; pass < 2; pass++) {
        for (long b : bucketVolume.keySet()) {
          long rootB = find(parent, b);
          if (groupVolume.get(rootB) >= MIN_REGION_CELLS) {
            continue;
          }
          int bx = unpackX(b);
          int by = unpackY(b);
          int bz = unpackZ(b);
          long best = -1;
          int bestVolume = -1;
          boolean found = false;
          for (int[] d : DIRS) {
            long n = pack(bx + d[0], by + d[1], bz + d[2]);
            if (!bucketVolume.containsKey(n)) {
              continue;
            }
            long rootN = find(parent, n);
            if (rootN == rootB) {
              continue;
            }
            int v = groupVolume.get(rootN);
            if (v > bestVolume) {
              bestVolume = v;
              best = rootN;
              found = true;
            }
          }
          if (found) {
            parent.put(rootB, best);
            groupVolume.addTo(best, groupVolume.get(rootB));
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
        r.volume.add(0);
        r.envelopeFaces.add(0);
        r.exteriorUA.add(0f);
        r.openingUA.add(0f);
        r.groundUA.add(0f);
        r.minY.add(Integer.MAX_VALUE);
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

  private static void classifyWallFace(ThermalCellSource src, Result r, int region,
      int wx, int wy, int wz, int[] d, float factor) {
    for (int k = 1; k <= THICK_PROBE; k++) {
      int px = wx + d[0] * k;
      int py = wy + d[1] * k;
      int pz = wz + d[2] * k;
      byte kind = src.classify(px, py, pz);
      if (kind == ThermalCellSource.WALL) {
        continue;
      }
      // k wall cells were crossed before reaching open air.
      float ua = U_FACE * factor / k;
      if (kind == ThermalCellSource.AIR) {
        long key = pack(px, py, pz);
        if (r.cells.contains(key)) {
          // A partition inside this same space: the far side is the same air.
          r.envelopeFaces.set(region, r.envelopeFaces.getInt(region) - 1);
          return;
        }
        r.beyondCell.add(key);
        r.beyondRegion.add(region);
        r.beyondUA.add(ua);
        return;
      }
      addTo(r.exteriorUA, region, ua); // sky, or a chunk we cannot see: outdoors either way
      return;
    }
    addTo(r.groundUA, region, U_FACE * factor * GROUND_FACTOR);
  }

  private static void addTo(FloatArrayList list, int index, float value) {
    list.set(index, list.getFloat(index) + value);
  }

  /** The key of the region grid cell containing a block. */
  public static long regionKey(int x, int y, int z) {
    return pack(Math.floorDiv(x, REGION_XZ), Math.floorDiv(y, REGION_Y),
        Math.floorDiv(z, REGION_XZ));
  }

  // Packing identical to BlockPos#toLong, so a packed cell and a BlockPos key agree.

  private static final int NUM_X_BITS = 26;
  private static final int NUM_Z_BITS = 26;
  private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
  private static final int Y_SHIFT = NUM_Z_BITS;
  private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
  private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
  private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
  private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

  /** Packs a cell exactly as {@code BlockPos#toLong} does. */
  public static long pack(int x, int y, int z) {
    return ((long) x & X_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT | ((long) z & Z_MASK);
  }

  public static int unpackX(long packed) {
    return (int) (packed << (64 - X_SHIFT - NUM_X_BITS) >> (64 - NUM_X_BITS));
  }

  public static int unpackY(long packed) {
    return (int) (packed << (64 - Y_SHIFT - NUM_Y_BITS) >> (64 - NUM_Y_BITS));
  }

  public static int unpackZ(long packed) {
    return (int) (packed << (64 - NUM_Z_BITS) >> (64 - NUM_Z_BITS));
  }
}
