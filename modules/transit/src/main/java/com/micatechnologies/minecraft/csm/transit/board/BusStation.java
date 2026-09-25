package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopFlag;
import com.micatechnologies.minecraft.csm.transit.stop.BusAgency;
import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * The bus stops a departure board or a bay display can see: every bus stop flag within
 * {@link #CHUNK_REACH} chunks of the board's chunk and {@link #HEIGHT_REACH} blocks of its
 * sixteen-block band of height, each one a bay. Client side, and looked up rather than linked:
 * a board has nothing to configure but the agency it shows, and a stop is added to a station by
 * building it.
 *
 * <p><b>Bays.</b> The flags are numbered 1 up in world order -- west to east, then north to
 * south, then bottom to top -- so every board and bay display looking at the same stops numbers
 * them alike, whichever way it faces. At most {@link #MAX_BAYS} are kept.</p>
 *
 * <p><b>Cost.</b> One station is kept per chunk and height band and shared by every board in it,
 * so a bank of boards looks once. The lookup walks the loaded chunks' tile entity maps, never
 * block by block, and is repeated only every {@link #RESCAN_TICKS} ticks; the departures are
 * worked out once a minute of the countdown for each agency filter. Cleared on disconnect
 * ({@link #clear}).</p>
 *
 * @since 2026.9
 */
public final class BusStation {

  /** Chunks looked at each way from the board's own. */
  public static final int CHUNK_REACH = 2;
  /** Blocks looked at above and below the board's band of height. */
  public static final int HEIGHT_REACH = 12;
  /** The most bays a station has. */
  public static final int MAX_BAYS = 16;
  /** The most departures listed: two buses of every route of every bay. */
  public static final int MAX_DEPARTURES =
      MAX_BAYS * TileEntityBusStopFlag.PLATES * BusDepartures.PER_ROUTE;
  /** How often the stops are looked for again. */
  private static final long RESCAN_TICKS = 100;
  /** Filters: 0 every agency, then one per agency. */
  public static final int FILTERS = BusAgency.values().length + 1;

  private static final Map<Long, BusStation> STATIONS = new HashMap<>();

  private final World world;
  private final long scannedAt;
  private final int bays;
  private final BlockPos[] bayPos = new BlockPos[MAX_BAYS];
  private final BusAgency[] bayAgency = new BusAgency[MAX_BAYS];
  private final int[][] bayRoutes = new int[MAX_BAYS][TileEntityBusStopFlag.PLATES];
  private final int[] bayRouteCount = new int[MAX_BAYS];
  private final Departures[] departures = new Departures[FILTERS];

  /** A board's list of departures, soonest first. */
  public static final class Departures {

    private long minute = Long.MIN_VALUE;
    private int count;
    private final int[] route = new int[MAX_DEPARTURES];
    private final int[] bay = new int[MAX_DEPARTURES];
    private final int[] minutes = new int[MAX_DEPARTURES];
    private final int[] agency = new int[MAX_DEPARTURES];

    /** How many departures are listed. */
    public int count() {
      return count;
    }

    /** A departure's route number. */
    public int route(int i) {
      return route[i];
    }

    /** A departure's bay, 1 up. */
    public int bay(int i) {
      return bay[i];
    }

    /** A departure's minutes to go; 0 is due. */
    public int minutes(int i) {
      return minutes[i];
    }

    /** A departure's agency, an ordinal of {@link BusAgency}. */
    public int agency(int i) {
      return agency[i];
    }
  }

  private BusStation(World world, long scannedAt, List<TileEntityBusStopFlag> flags) {
    this.world = world;
    this.scannedAt = scannedAt;
    int n = 0;
    for (TileEntityBusStopFlag flag : flags) {
      if (n >= MAX_BAYS) {
        break;
      }
      Block block = world.getBlockState(flag.getPos()).getBlock();
      if (!(block instanceof BlockBusStopFlag)) {
        continue;
      }
      bayPos[n] = flag.getPos();
      bayAgency[n] = ((BlockBusStopFlag) block).getAgency();
      int routes = 0;
      for (int i = 0; i < TileEntityBusStopFlag.PLATES; i++) {
        int route = flag.getRoute(i);
        if (route != 0) {
          bayRoutes[n][routes++] = route;
        }
      }
      bayRouteCount[n] = routes;
      n++;
    }
    bays = n;
  }

  /**
   * The station a board at {@code pos} sees, looked up again if the last look is older than
   * {@link #RESCAN_TICKS}. Client side, render or client tick thread.
   *
   * @param world the client world
   * @param pos   the board
   *
   * @return the station, never null (it may have no bays)
   */
  public static BusStation at(World world, BlockPos pos) {
    int cx = pos.getX() >> 4;
    int cz = pos.getZ() >> 4;
    int band = pos.getY() >> 4;
    long key = ((long) (cx & 0x3FFFFF) << 42) | ((long) (cz & 0x3FFFFF) << 20)
        | (band & 0xFFFFF);
    long now = world.getTotalWorldTime();
    BusStation station = STATIONS.get(key);
    if (station == null || station.world != world || now - station.scannedAt >= RESCAN_TICKS
        || now < station.scannedAt) {
      station = scan(world, cx, cz, band, now);
      STATIONS.put(key, station);
    }
    return station;
  }

  /** Forgets every station: on disconnect, so a new world starts clean. */
  public static void clear() {
    STATIONS.clear();
  }

  private static BusStation scan(World world, int cx, int cz, int band, long now) {
    int y0 = band * 16 - HEIGHT_REACH;
    int y1 = band * 16 + 15 + HEIGHT_REACH;
    List<TileEntityBusStopFlag> flags = new ArrayList<>();
    for (int x = cx - CHUNK_REACH; x <= cx + CHUNK_REACH; x++) {
      for (int z = cz - CHUNK_REACH; z <= cz + CHUNK_REACH; z++) {
        Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
        if (chunk == null) {
          continue;
        }
        for (TileEntity te : chunk.getTileEntityMap().values()) {
          if (te instanceof TileEntityBusStopFlag && !te.isInvalid()
              && te.getPos().getY() >= y0 && te.getPos().getY() <= y1) {
            flags.add((TileEntityBusStopFlag) te);
          }
        }
      }
    }
    flags.sort(Comparator.comparingInt((TileEntityBusStopFlag f) -> f.getPos().getX())
        .thenComparingInt(f -> f.getPos().getZ()).thenComparingInt(f -> f.getPos().getY()));
    return new BusStation(world, now, flags);
  }

  /**
   * How many bays the station has.
   *
   * @return 0 to {@link #MAX_BAYS}
   */
  public int bays() {
    return bays;
  }

  /**
   * The bay nearest a position within a reach, for a bay display.
   *
   * @param pos        the display
   * @param horizontal how far away the flag may be across
   * @param vertical   how far above or below
   *
   * @return the bay, 0 up, or -1 if none is that close
   */
  public int nearestBay(BlockPos pos, int horizontal, int vertical) {
    int best = -1;
    double bestDist = Double.MAX_VALUE;
    for (int i = 0; i < bays; i++) {
      BlockPos p = bayPos[i];
      int dx = Math.abs(p.getX() - pos.getX());
      int dz = Math.abs(p.getZ() - pos.getZ());
      int dy = Math.abs(p.getY() - pos.getY());
      if (dx > horizontal || dz > horizontal || dy > vertical) {
        continue;
      }
      double dist = dx * dx + dz * dz + dy * dy * 0.25;
      if (dist < bestDist) {
        bestDist = dist;
        best = i;
      }
    }
    return best;
  }

  /** A bay's flag position. */
  public BlockPos bayPos(int bay) {
    return bayPos[bay];
  }

  /** A bay's routes; the first {@link #bayRouteCount} are used. Read, never change. */
  public int[] bayRoutes(int bay) {
    return bayRoutes[bay];
  }

  /** How many routes a bay's flag carries. */
  public int bayRouteCount(int bay) {
    return bayRouteCount[bay];
  }

  /**
   * The departures a board shows, soonest first (then by bay, then by route), worked out once
   * a minute for each filter.
   *
   * @param filter 0 for every agency, else 1 + the agency's ordinal
   * @param minute the countdown's minute, {@link BusDepartures#minuteOf}
   *
   * @return the list; shared, read it before the next call
   */
  public Departures departures(int filter, long minute) {
    int f = Math.max(0, Math.min(FILTERS - 1, filter));
    Departures d = departures[f];
    if (d == null) {
      d = new Departures();
      departures[f] = d;
    }
    if (d.minute == minute) {
      return d;
    }
    d.minute = minute;
    int n = 0;
    for (int b = 0; b < bays; b++) {
      if (f != 0 && bayAgency[b].ordinal() != f - 1) {
        continue;
      }
      BlockPos p = bayPos[b];
      for (int r = 0; r < bayRouteCount[b]; r++) {
        int route = bayRoutes[b][r];
        int next = BusDepartures.minutesToNext(route, p.getX(), p.getZ(), minute);
        for (int k = 0; k < BusDepartures.PER_ROUTE; k++) {
          n = insert(d, n, route, b + 1, next + k * BusDepartures.headwayOf(route),
              bayAgency[b].ordinal());
        }
      }
    }
    d.count = n;
    return d;
  }

  private static int insert(Departures d, int n, int route, int bay, int minutes, int agency) {
    int at = n;
    while (at > 0 && after(d, at - 1, minutes, bay, route)) {
      d.route[at] = d.route[at - 1];
      d.bay[at] = d.bay[at - 1];
      d.minutes[at] = d.minutes[at - 1];
      d.agency[at] = d.agency[at - 1];
      at--;
    }
    d.route[at] = route;
    d.bay[at] = bay;
    d.minutes[at] = minutes;
    d.agency[at] = agency;
    return n + 1;
  }

  /** Whether listed entry {@code i} goes after a departure with these values. */
  private static boolean after(Departures d, int i, int minutes, int bay, int route) {
    if (d.minutes[i] != minutes) {
      return d.minutes[i] > minutes;
    }
    if (d.bay[i] != bay) {
      return d.bay[i] > bay;
    }
    return d.route[i] > route;
  }
}
