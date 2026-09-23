package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.CsmEnvironment;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;

/**
 * The thermal simulation of one world: its spaces, the anchors that keep them alive, and the
 * once-a-second step that runs the controllers and moves every temperature.
 *
 * <p>Everything that decides a temperature happens here, on the server, in one place. Tile
 * entities hold configuration (setpoints, links) and display what this class tells them; the HUD
 * is sent the value this class holds for the player's position. That is why a thermostat and the
 * HUD beside it can no longer disagree.</p>
 *
 * <p><b>Spaces come and go with their anchors.</b> A device registers as an anchor when its tile
 * entity loads and unregisters when it unloads. An anchor with no space scans for one (or finds
 * the space already covering its cell). A space with no anchors left is dropped; its temperature
 * survives in the anchors' saved data, so when the chunk loads again the space is found again at
 * the temperature it had. While a building's chunks are unloaded, its temperature is frozen.</p>
 *
 * <p><b>Spaces follow the blocks.</b> A block change on or beside a space's cells marks it dirty
 * and it is rescanned a second later; every space is also rescanned every five minutes in case
 * something the listener cannot see changed. A rescan keeps each cell's temperature: the new
 * space's regions start from the temperatures of the cells they took over, so opening a door
 * mixes two rooms and closing it leaves each as it was.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class HvacThermalWorld implements IWorldEventListener {

  /** Game ticks per simulation step. */
  static final int STEP_TICKS = 20;

  /** Seconds per simulation step. */
  static final float DT = STEP_TICKS / 20.0f;

  /** Minimum ticks between rescans of one space, so a flapping door costs one scan a second. */
  private static final long RESCAN_DEBOUNCE_TICKS = 20L;

  /**
   * Ticks after which every space is rescanned whether or not anything was seen to change: a
   * safety net for what the block listener cannot see (a wall thickened out beyond the envelope
   * probe). Five minutes, since rescanning the largest space allowed takes some 15-25 ms.
   */
  private static final long RESCAN_PERIOD_TICKS = 6000L;

  private static final long RETRY_UNLOADED_TICKS = 40L;
  private static final long RETRY_NOT_ENCLOSED_TICKS = 100L;
  private static final long RETRY_TOO_LARGE_TICKS = 600L;

  /** How long a too-large flood's cells are remembered, so neighbours do not repeat it. */
  private static final long TOO_LARGE_MEMORY_TICKS = 600L;

  private static final long COUPLING_REFRESH_TICKS = 200L;

  /** Distance to an HVAC device within which a player sees the HUD. */
  static final int HUD_RANGE = 24;

  private static final long PLAYER_SCAN_INTERVAL_TICKS = 100L;

  final World world;

  private final Long2ObjectOpenHashMap<ThermalSpace> cellToSpace = new Long2ObjectOpenHashMap<>();
  private final List<ThermalSpace> spaces = new ArrayList<>();
  private int nextSpaceId = 1;

  private final Map<BlockPos, ThermalAnchor> anchors = new HashMap<>();
  private final Long2ObjectOpenHashMap<List<ThermalAnchor>> anchorsByChunk =
      new Long2ObjectOpenHashMap<>();
  private final Map<UUID, ThermalAnchor> playerAnchors = new HashMap<>();
  private final Map<UUID, Long> playerNextScan = new HashMap<>();

  private final List<TooLarge> tooLarge = new ArrayList<>();

  /** Spaces taken apart this step for a rescan, kept until their cells are re-homed. */
  private final List<ThermalSpace> detached = new ArrayList<>();

  private boolean topologyChanged;
  private long lastCouplingTick = Long.MIN_VALUE;

  /** Counts steps; tile entities use it to tell whether a system claimed them this step. */
  long stepId;

  private static final class TooLarge {
    final LongOpenHashSet cells;
    final long expires;

    TooLarge(LongOpenHashSet cells, long expires) {
      this.cells = cells;
      this.expires = expires;
    }
  }

  HvacThermalWorld(World world) {
    this.world = world;
  }

  // region Anchors

  /**
   * Registers a device as an anchor, or refreshes its saved temperature if it already is one.
   *
   * @param savedTemp the temperature the device saved, or NaN
   */
  void registerAnchor(BlockPos pos, int kind, float savedTemp) {
    ThermalAnchor a = anchors.get(pos);
    if (a != null && a.kind == kind) {
      if (a.space == null && !Float.isNaN(savedTemp)) {
        a.savedTemp = savedTemp;
      }
      return;
    }
    if (a != null) {
      unregisterAnchor(pos);
    }
    a = new ThermalAnchor(pos, kind);
    a.savedTemp = savedTemp;
    anchors.put(a.pos, a);
    anchorsByChunk.computeIfAbsent(chunkKey(pos.getX() >> 4, pos.getZ() >> 4),
        k -> new ArrayList<>()).add(a);
  }

  /** Removes a device anchor. The space goes with it if it was the last one. */
  void unregisterAnchor(BlockPos pos) {
    ThermalAnchor a = anchors.remove(pos);
    if (a == null) {
      return;
    }
    List<ThermalAnchor> list = anchorsByChunk.get(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
    if (list != null) {
      list.remove(a);
      if (list.isEmpty()) {
        anchorsByChunk.remove(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
      }
    }
    detachAnchor(a);
  }

  private void detachAnchor(ThermalAnchor a) {
    ThermalSpace s = a.space;
    if (s == null) {
      return;
    }
    a.savedTemp = a.temperature();
    a.space = null;
    a.region = -1;
    s.anchors.remove(a);
    if (s.anchors.isEmpty()) {
      removeSpace(s);
    }
  }

  @Nullable
  ThermalAnchor anchor(BlockPos pos) {
    return anchors.get(pos);
  }

  Collection<ThermalAnchor> anchors() {
    return anchors.values();
  }

  /** The temperature a device should show: its region's, else what it saved, else the biome. */
  float displayTemperature(BlockPos pos) {
    ThermalAnchor a = anchors.get(pos);
    float t = a != null ? a.temperature() : Float.NaN;
    return Float.isNaN(t) ? CsmEnvironment.getBaselineTemperatureAt(world, pos) : t;
  }

  // endregion

  // region Space lifecycle

  /** Tries to attach every unattached anchor that is due. */
  private void attachPending(long now) {
    for (ThermalAnchor a : new ArrayList<>(anchors.values())) {
      if (a.space == null && now >= a.retryTick) {
        attach(a, now);
      }
    }
  }

  /**
   * Finds or builds the space for an anchor. The anchor's own cell is tried first -- thermostats,
   * vents and most units are not full blocks, so they sit in the room's air -- then its six
   * neighbours, for a unit that fills its cell.
   */
  private void attach(ThermalAnchor a, long now) {
    ThermalCellSource cellSource = HvacAirflow.worldSource(world);
    ThermalScanner.Status last = ThermalScanner.Status.NOT_ENCLOSED;
    BlockPos p = a.pos;
    for (int i = -1; i < 6; i++) {
      int x = p.getX();
      int y = p.getY();
      int z = p.getZ();
      if (i >= 0) {
        EnumFacing f = EnumFacing.byIndex(i);
        x += f.getXOffset();
        y += f.getYOffset();
        z += f.getZOffset();
      }
      byte kind = cellSource.classify(x, y, z);
      if (kind == ThermalCellSource.UNLOADED) {
        last = ThermalScanner.Status.UNLOADED;
        continue;
      }
      if (kind != ThermalCellSource.AIR) {
        continue;
      }
      long key = ThermalScanner.pack(x, y, z);
      ThermalSpace s = cellToSpace.get(key);
      if (s == null) {
        if (inTooLarge(key, now)) {
          last = ThermalScanner.Status.TOO_LARGE;
          continue;
        }
        ThermalScanner.Result r = ThermalScanner.scan(cellSource, x, y, z);
        if (r.status == ThermalScanner.Status.OK) {
          s = createSpace(r, key, now);
        } else {
          if (r.status == ThermalScanner.Status.TOO_LARGE) {
            tooLarge.add(new TooLarge(r.cells, now + TOO_LARGE_MEMORY_TICKS));
          }
          last = r.status;
          if (r.status == ThermalScanner.Status.UNLOADED) {
            break; // the other faces will reach the same unloaded chunk
          }
          continue;
        }
      }
      join(a, s, key);
      return;
    }
    a.status = last;
    if (last != ThermalScanner.Status.UNLOADED) {
      // Open to the sky or part of something too big: whatever it saved no longer describes a
      // room, and must not seed one if the room is closed up later.
      a.savedTemp = Float.NaN;
    }
    a.retryTick = now + (last == ThermalScanner.Status.UNLOADED ? RETRY_UNLOADED_TICKS
        : last == ThermalScanner.Status.TOO_LARGE ? RETRY_TOO_LARGE_TICKS
            : RETRY_NOT_ENCLOSED_TICKS);
  }

  private void join(ThermalAnchor a, ThermalSpace s, long cell) {
    a.space = s;
    a.cell = cell;
    a.region = s.regionOfCell(cell);
    a.status = ThermalScanner.Status.OK;
    s.anchors.add(a);
    // The saved value has been used (or the space was already live); a later detach refreshes it.
    a.savedTemp = Float.NaN;
  }

  private boolean inTooLarge(long key, long now) {
    tooLarge.removeIf(t -> t.expires <= now);
    for (TooLarge t : tooLarge) {
      if (t.cells.contains(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builds a space from a scan and gives each region its starting temperature: the temperature
   * of the cells it took over from spaces that existed a moment ago (live or detached for
   * rescan), else what the anchors inside it saved, else the equilibrium it would settle at.
   */
  private ThermalSpace createSpace(ThermalScanner.Result r, long sampleCell, long now) {
    ThermalSpace s = new ThermalSpace(nextSpaceId++, r, sampleCell, now);
    s.outdoor = outdoorAt(sampleCell);

    int n = s.regionCount;
    double[] sum = new double[n];
    int[] count = new int[n];
    IdentityHashMap<ThermalSpace, Boolean> merged = new IdentityHashMap<>();

    LongIterator it = s.cells.iterator();
    while (it.hasNext()) {
      long cell = it.nextLong();
      ThermalSpace owner = cellToSpace.get(cell);
      if (owner != null) {
        merged.put(owner, Boolean.TRUE);
      } else {
        for (ThermalSpace d : detached) {
          if (d.cells.contains(cell)) {
            owner = d;
            break;
          }
        }
      }
      if (owner != null) {
        int r0 = s.regionOfCell(cell);
        sum[r0] += owner.temperatureAt(cell);
        count[r0]++;
      }
    }

    // Anchors that saved a temperature and sit in this space seed their own regions.
    for (ThermalAnchor a : anchorsIn(s)) {
      if (!Float.isNaN(a.savedTemp)) {
        int r0 = regionOfAnchor(s, a);
        if (r0 >= 0 && count[r0] == 0) {
          sum[r0] += a.savedTemp;
          count[r0] = -1; // marker: seeded from a saved value, not inherited cells
          s.temperature[r0] = a.savedTemp;
        } else if (r0 >= 0 && count[r0] < 0) {
          s.temperature[r0] = (s.temperature[r0] + a.savedTemp) * 0.5f;
        }
      }
    }

    double knownSum = 0;
    int known = 0;
    for (int i = 0; i < n; i++) {
      if (count[i] > 0) {
        s.temperature[i] = (float) (sum[i] / count[i]);
      }
      if (count[i] != 0) {
        knownSum += s.temperature[i];
        known++;
      }
    }
    if (known > 0) {
      float mean = (float) (knownSum / known);
      for (int i = 0; i < n; i++) {
        if (count[i] == 0) {
          s.temperature[i] = mean;
        }
      }
      s.temperatureKnown = true;
    } else {
      s.needsEquilibrium = true;
    }

    // Spaces this one swallowed (a wall came down, a door opened) go; their anchors rejoin.
    for (ThermalSpace m : merged.keySet()) {
      List<ThermalAnchor> moved = new ArrayList<>(m.anchors);
      removeSpaceCells(m);
      spaces.remove(m);
      for (ThermalAnchor a : moved) {
        a.space = null;
        a.region = -1;
        ThermalSpace target = s;
        long cell = a.cell;
        if (s.cells.contains(cell)) {
          join(a, target, cell);
        } else {
          a.retryTick = 0;
        }
      }
      m.anchors.clear();
    }

    LongIterator it2 = s.cells.iterator();
    while (it2.hasNext()) {
      cellToSpace.put(it2.nextLong(), s);
    }
    spaces.add(s);
    topologyChanged = true;
    return s;
  }

  /** Anchors whose cell (their own or a neighbour) is inside the space. */
  private List<ThermalAnchor> anchorsIn(ThermalSpace s) {
    List<ThermalAnchor> out = new ArrayList<>();
    LongOpenHashSet chunks = new LongOpenHashSet();
    LongIterator it = s.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      chunks.add(chunkKey(ThermalScanner.unpackX(c) >> 4, ThermalScanner.unpackZ(c) >> 4));
    }
    LongIterator ci = chunks.iterator();
    while (ci.hasNext()) {
      List<ThermalAnchor> list = anchorsByChunk.get(ci.nextLong());
      if (list != null) {
        for (ThermalAnchor a : list) {
          if (regionOfAnchor(s, a) >= 0) {
            out.add(a);
          }
        }
      }
    }
    return out;
  }

  private static int regionOfAnchor(ThermalSpace s, ThermalAnchor a) {
    BlockPos p = a.pos;
    long own = ThermalScanner.pack(p.getX(), p.getY(), p.getZ());
    if (s.cells.contains(own)) {
      return s.regionOfCell(own);
    }
    for (EnumFacing f : EnumFacing.values()) {
      long k = ThermalScanner.pack(p.getX() + f.getXOffset(), p.getY() + f.getYOffset(),
          p.getZ() + f.getZOffset());
      if (s.cells.contains(k)) {
        return s.regionOfCell(k);
      }
    }
    return -1;
  }

  private void removeSpaceCells(ThermalSpace s) {
    LongIterator it = s.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      if (cellToSpace.get(c) == s) {
        cellToSpace.remove(c);
      }
    }
    topologyChanged = true;
  }

  private void removeSpace(ThermalSpace s) {
    removeSpaceCells(s);
    spaces.remove(s);
  }

  /**
   * Rescans every space that is due: dirty ones a second after the change, the rest every five
   * minutes.
   * Each is taken apart, its anchors (remembering their region's temperature) reattach, and the
   * spaces they build inherit the old cells' temperatures. A space whose rescan cannot complete
   * because part of it is unloaded is put back exactly as it was.
   */
  private void rescanDue(long now) {
    List<ThermalSpace> due = new ArrayList<>();
    for (ThermalSpace s : spaces) {
      long age = now - s.lastScanTick;
      if ((s.dirty && age >= RESCAN_DEBOUNCE_TICKS) || age >= RESCAN_PERIOD_TICKS) {
        due.add(s);
      }
    }
    if (due.isEmpty()) {
      return;
    }
    List<List<ThermalAnchor>> anchorsOf = new ArrayList<>();
    for (ThermalSpace s : due) {
      List<ThermalAnchor> list = new ArrayList<>(s.anchors);
      anchorsOf.add(list);
      for (ThermalAnchor a : list) {
        a.savedTemp = a.temperature();
        a.space = null;
        a.region = -1;
        a.retryTick = 0;
      }
      s.anchors.clear();
      removeSpaceCells(s);
      spaces.remove(s);
      detached.add(s);
    }
    for (List<ThermalAnchor> list : anchorsOf) {
      for (ThermalAnchor a : list) {
        boolean live = a.kind == ThermalAnchor.PLAYER || anchors.get(a.pos) == a;
        if (live && a.space == null) {
          attach(a, now);
        }
      }
    }
    // Put back any space none of whose cells found a new home because its rescan hit an
    // unloaded chunk: the building is only partly loaded, and a partial space would be wrong.
    for (int i = 0; i < due.size(); i++) {
      ThermalSpace s = due.get(i);
      boolean anyClaimed = false;
      boolean anyUnloaded = false;
      for (ThermalAnchor a : anchorsOf.get(i)) {
        if (a.space != null && a.space != s) {
          anyClaimed = true;
        }
        if (a.status == ThermalScanner.Status.UNLOADED) {
          anyUnloaded = true;
        }
      }
      if (!anyClaimed && anyUnloaded) {
        restore(s, anchorsOf.get(i), now);
      }
    }
    detached.clear();
  }

  private void restore(ThermalSpace s, List<ThermalAnchor> list, long now) {
    LongIterator it = s.cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      if (!cellToSpace.containsKey(c)) {
        cellToSpace.put(c, s);
      }
    }
    s.dirty = true;
    s.lastScanTick = now + RETRY_UNLOADED_TICKS; // try again shortly
    spaces.add(s);
    for (ThermalAnchor a : list) {
      if (a.space == null && (anchors.get(a.pos) == a || a.kind == ThermalAnchor.PLAYER)) {
        long cell = a.cell;
        if (s.cells.contains(cell)) {
          join(a, s, cell);
        }
      }
    }
    if (s.anchors.isEmpty()) {
      removeSpace(s);
    }
    topologyChanged = true;
  }

  /** Resolves which region of which space lies beyond each wall face, and refreshes outdoors. */
  private void resolveCouplings(long now) {
    for (ThermalSpace s : spaces) {
      s.outdoor = outdoorAt(s.sampleCell);
      java.util.Arrays.fill(s.unconditionedUA, 0f);
      Map<Long, Integer> index = new HashMap<>();
      IntList regions = new IntList();
      List<ThermalSpace> others = new ArrayList<>();
      IntList otherRegions = new IntList();
      FloatList uas = new FloatList();
      for (int i = 0; i < s.beyondCell.length; i++) {
        long cell = s.beyondCell[i];
        int r = s.beyondRegion[i];
        ThermalSpace other = cellToSpace.get(cell);
        if (other == null || other == s) {
          s.unconditionedUA[r] += s.beyondUA[i];
          continue;
        }
        int or = other.regionOfCell(cell);
        long key = ((long) r << 40) ^ ((long) other.id << 20) ^ or;
        Integer at = index.get(key);
        if (at == null) {
          index.put(key, regions.size());
          regions.add(r);
          others.add(other);
          otherRegions.add(or);
          uas.add(s.beyondUA[i]);
        } else {
          uas.set(at, uas.get(at) + s.beyondUA[i]);
        }
      }
      s.coupleRegion = regions.toArray();
      s.coupleSpace = others.toArray(new ThermalSpace[0]);
      s.coupleOtherRegion = otherRegions.toArray();
      s.coupleUA = uas.toArray();
    }
    for (ThermalSpace s : spaces) {
      if (s.needsEquilibrium) {
        s.settleToEquilibrium();
        s.needsEquilibrium = false;
        s.temperatureKnown = true;
      }
    }
    topologyChanged = false;
    lastCouplingTick = now;
  }

  private float outdoorAt(long cell) {
    return CsmEnvironment.getBaselineTemperatureAt(world, BlockPos.fromLong(cell));
  }

  // endregion

  // region Stepping

  /** Called every server tick for this world. */
  void tick() {
    long now = world.getTotalWorldTime();
    if (now % STEP_TICKS != 0) {
      return;
    }
    maintain(now);
    step();
    updatePlayers(now);
  }

  /** Rescans, attaches and couplings: everything that follows the blocks. */
  void maintain(long now) {
    rescanDue(now);
    attachPending(now);
    if (topologyChanged || now - lastCouplingTick >= COUPLING_REFRESH_TICKS) {
      resolveCouplings(now);
    }
  }

  /** One second of simulated time: controllers, then physics. */
  void step() {
    stepId++;
    for (ThermalSpace s : spaces) {
      s.frozen = !allLoaded(s);
      s.prepareLoss();
    }
    HvacSystemControl.run(this);
    for (ThermalSpace s : spaces) {
      s.integrate(DT);
    }
  }

  /**
   * Runs {@code seconds} of simulated time at once, for testing: the blocks are taken as they are
   * now, then the controllers and physics are stepped as fast as they will go.
   */
  void fastForward(int seconds) {
    long now = world.getTotalWorldTime();
    maintain(now);
    for (int i = 0; i < seconds; i++) {
      step();
    }
    updatePlayers(now);
  }

  // endregion

  /** Whether every chunk the space lies in is loaded. */
  private boolean allLoaded(ThermalSpace s) {
    for (long key : s.chunks) {
      int cx = (int) key;
      int cz = (int) (key >>> 32);
      if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) {
        return false;
      }
    }
    return true;
  }

  // region Queries

  /** The space owning a cell, or null. */
  @Nullable
  ThermalSpace spaceAt(BlockPos pos) {
    return cellToSpace.get(ThermalScanner.pack(pos.getX(), pos.getY(), pos.getZ()));
  }

  /** The temperature at a position: its region's if it is in a space, else the biome's. */
  float temperatureAt(BlockPos pos) {
    long key = ThermalScanner.pack(pos.getX(), pos.getY(), pos.getZ());
    ThermalSpace s = cellToSpace.get(key);
    if (s != null) {
      return s.temperatureAt(key);
    }
    return CsmEnvironment.getBaselineTemperatureAt(world, pos);
  }

  List<ThermalSpace> spaces() {
    return spaces;
  }

  /** Whether any device anchor lies within {@link #HUD_RANGE} blocks of a position. */
  boolean nearDevice(BlockPos pos) {
    int r = HUD_RANGE;
    double rSq = (double) r * r;
    for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
      for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
        List<ThermalAnchor> list = anchorsByChunk.get(chunkKey(cx, cz));
        if (list == null) {
          continue;
        }
        for (ThermalAnchor a : list) {
          if (a.pos.distanceSq(pos) <= rSq) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // endregion

  // region Players

  /**
   * Sends every player near HVAC the temperature where they stand. A player standing in enclosed
   * air that no device's space covers (a hallway, a storeroom) anchors a space of their own, so
   * the reading there comes from the same simulation -- warmed or chilled by the conditioned rooms
   * around it -- rather than jumping to the outdoor temperature.
   */
  private void updatePlayers(long now) {
    for (EntityPlayer player : new ArrayList<>(world.playerEntities)) {
      BlockPos pos = new BlockPos(MathHelper.floor(player.posX),
          MathHelper.floor(player.posY + 0.1), MathHelper.floor(player.posZ));
      UUID id = player.getUniqueID();
      boolean near = nearDevice(pos);
      long key = ThermalScanner.pack(pos.getX(), pos.getY(), pos.getZ());
      ThermalSpace here = cellToSpace.get(key);

      ThermalAnchor pa = playerAnchors.get(id);
      if (pa != null && (pa.space == null || !pa.space.cells.contains(key) || !near)) {
        detachAnchor(pa);
        playerAnchors.remove(id);
        pa = null;
      }
      if (near && here == null && pa == null) {
        ThermalCellSource cellSource = HvacAirflow.worldSource(world);
        Long next = playerNextScan.get(id);
        if ((next == null || now >= next)
            && cellSource.classify(pos.getX(), pos.getY(), pos.getZ()) == ThermalCellSource.AIR
            && !inTooLarge(key, now)) {
          playerNextScan.put(id, now + PLAYER_SCAN_INTERVAL_TICKS);
          ThermalScanner.Result r = ThermalScanner.scan(cellSource, pos.getX(), pos.getY(),
              pos.getZ());
          if (r.status == ThermalScanner.Status.OK) {
            ThermalSpace s = createSpace(r, key, now);
            pa = new ThermalAnchor(pos, ThermalAnchor.PLAYER);
            join(pa, s, key);
            playerAnchors.put(id, pa);
            resolveCouplings(now);
            here = s;
          } else if (r.status == ThermalScanner.Status.TOO_LARGE) {
            tooLarge.add(new TooLarge(r.cells, now + TOO_LARGE_MEMORY_TICKS));
          }
        }
      }
      if (near || here != null) {
        float t = here != null ? here.temperatureAt(key)
            : CsmEnvironment.getBaselineTemperatureAt(world, pos);
        HvacHudPacket.send(player, true, t, here != null);
      } else {
        HvacHudPacket.send(player, false, 0f, false);
      }
    }
    playerAnchors.keySet().removeIf(id -> {
      for (EntityPlayer p : world.playerEntities) {
        if (p.getUniqueID().equals(id)) {
          return false;
        }
      }
      ThermalAnchor a = playerAnchors.get(id);
      if (a != null) {
        detachAnchor(a);
      }
      return true;
    });
  }

  // endregion

  // region IWorldEventListener

  @Override
  public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState,
      IBlockState newState, int flags) {
    if (oldState == newState) {
      return; // a tile entity sync, not a change of shape
    }
    long now = world.getTotalWorldTime();
    markDirtyAround(pos.getX(), pos.getY(), pos.getZ());
    long key = ThermalScanner.pack(pos.getX(), pos.getY(), pos.getZ());
    tooLarge.removeIf(t -> t.cells.contains(key));
    // A roof going on may enclose an anchor that could not find a space: let it try again soon.
    List<ThermalAnchor> list = anchorsByChunk.get(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
    if (list != null) {
      for (ThermalAnchor a : list) {
        if (a.space == null && a.retryTick > now + RESCAN_DEBOUNCE_TICKS) {
          a.retryTick = now + RESCAN_DEBOUNCE_TICKS;
        }
      }
    }
  }

  private void markDirtyAround(int x, int y, int z) {
    markDirty(ThermalScanner.pack(x, y, z));
    for (EnumFacing f : EnumFacing.values()) {
      markDirty(ThermalScanner.pack(x + f.getXOffset(), y + f.getYOffset(), z + f.getZOffset()));
    }
  }

  private void markDirty(long cell) {
    ThermalSpace s = cellToSpace.get(cell);
    if (s != null) {
      s.dirty = true;
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

  static long chunkKey(int cx, int cz) {
    return (long) cx & 0xFFFFFFFFL | ((long) cz & 0xFFFFFFFFL) << 32;
  }

  /** A growable int list without boxing, for coupling resolution. */
  private static final class IntList {
    int[] data = new int[8];
    int size;

    void add(int v) {
      if (size == data.length) {
        data = java.util.Arrays.copyOf(data, size * 2);
      }
      data[size++] = v;
    }

    int size() {
      return size;
    }

    int[] toArray() {
      return java.util.Arrays.copyOf(data, size);
    }
  }

  private static final class FloatList {
    float[] data = new float[8];
    int size;

    void add(float v) {
      if (size == data.length) {
        data = java.util.Arrays.copyOf(data, size * 2);
      }
      data[size++] = v;
    }

    float get(int i) {
      return data[i];
    }

    void set(int i, float v) {
      data[i] = v;
    }

    float[] toArray() {
      return java.util.Arrays.copyOf(data, size);
    }
  }
}
