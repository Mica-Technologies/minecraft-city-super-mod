package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.transit.board.BusStation;
import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An arrival board's or info totem's tile entity, on its master ({@link BlockBusArrivalBoard}).
 * It saves the board's data ({@link BusStopInfo}, whose class javadoc lists the NBT keys); the
 * countdown is worked out on the client from the world's clock and is never saved or synced.
 *
 * <p><b>Agreeing with the stop.</b> On the client the board looks up the nearest bus stop
 * ({@code IBusStopSource}: a stop flag or a full-size pole's flag) through the departure boards'
 * own {@link BusStation} lookup, within {@value #STOP_REACH} blocks across and
 * {@value #STOP_REACH_VERTICAL} up or down, passing over a stop that lists no routes. Its column
 * seeds the countdown, so the pole, this board and a departure board count down the same buses;
 * with no stop near, the board's own column does. A board with no routes of its own lists that
 * stop's.</p>
 *
 * <p><b>Cost.</b> That lookup, the rods' and posts' lengths and the light the housing is lit by
 * are cached here and looked at again at most once a second ({@link #REFRESH_TICKS}): never per
 * frame. The expiry also catches a ceiling or a chunk that arrived since. A data packet rebuilds
 * no chunk section ({@link #getBakedModelKey}): the board's blocks bake nothing from it.</p>
 *
 * @since 2026.11
 */
public class TileEntityBusArrivalBoard extends AbstractTileEntity {

  /** How far across a stop may be from the board for the board to follow it, in blocks. */
  public static final int STOP_REACH = 32;
  /** How far above or below. */
  public static final int STOP_REACH_VERTICAL = 8;
  /** How often the cached neighbour answers are looked at again, in ticks. */
  public static final int REFRESH_TICKS = 20;
  /** The longest rod or post drawn, in blocks. */
  public static final int MAX_REACH = 16;

  private final BusStopInfo info = new BusStopInfo();

  // --- client only, never saved ---------------------------------------------------------------

  private transient long refreshedAt = Long.MIN_VALUE;
  /** The rods' length above the housing, the posts' below it, in sixteenths. */
  private transient int rodPx;
  private transient int postPx;
  /** The world's light at the board, packed as {@code getCombinedLight} packs it. */
  private transient int light;
  /** The routes listed and the column seeding their countdown. */
  private final transient int[] routes = new int[TransitRoute.MAX_PER_LINE];
  private transient int routeCount;
  private transient int seedX;
  private transient int seedZ;
  /** Moves whenever the routes or the seed change. */
  private transient int sourceVersion;
  private transient AxisAlignedBB renderBox;
  /** The board's block, facing and mount, as last looked at; null before the first look. */
  private transient BlockBusArrivalBoard board;
  private transient EnumFacing facing = EnumFacing.NORTH;
  private transient BoardMount mount = BoardMount.HUNG;
  private transient BoardShape shape;
  /** The data's version the view was last worked out for: an edit refreshes at once. */
  private transient int resolvedInfoVersion = -1;

  /** The renderer's: each page's compiled content key, and the frame's scratch. */
  final transient long[] compiledKey = new long[TileEntityBusArrivalBoardRenderer.MAX_PAGES];

  /**
   * The board's data. Change it only through its setters, on the server, then
   * {@link #markChanged}.
   *
   * @return the data
   */
  public BusStopInfo getInfo() {
    return info;
  }

  /** Saves the board and sends it to the players near it. Server side. */
  public void markChanged() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    info.readFrom(compound);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    info.writeTo(compound);
    return compound;
  }

  /** The board's blocks bake nothing from its data: a sync never rebuilds a chunk section. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  // --- client view ----------------------------------------------------------------------------

  /**
   * Looks at the board's surroundings again if the last look is a second old: the stop it
   * follows, the rods' and posts' lengths and its light. The renderer calls this every frame.
   */
  @SideOnly(Side.CLIENT)
  public void refreshView() {
    long now = world.getTotalWorldTime();
    if (now - refreshedAt < REFRESH_TICKS && now >= refreshedAt
        && info.getVersion() == resolvedInfoVersion) {
      return;
    }
    refreshedAt = now;
    resolvedInfoVersion = info.getVersion();
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockBusArrivalBoard)) {
      board = null;
      return;
    }
    board = (BlockBusArrivalBoard) state.getBlock();
    facing = state.getValue(BlockBusArrivalBoard.FACING);
    mount = state.getValue(BlockBusArrivalBoard.MOUNT);
    if (shape == null || !shape.isFor(board, mount)) {
      shape = new BoardShape(board, mount);
    }
    light = world.getCombinedLight(pos, 0);
    rodPx = 0;
    postPx = 0;
    if (mount == BoardMount.HUNG && !board.isTotem()) {
      int gap = MAX_REACH;
      for (int i = 0; i < MAX_REACH; i++) {
        if (!world.isAirBlock(pos.up(board.getHeight() + i))) {
          gap = i;
          break;
        }
      }
      rodPx = BoardShape.TOP_GAP_PX + 16 * gap;
    } else if (mount == BoardMount.POST && !board.isTotem()) {
      int gap = BlockBusArrivalBoard.POST_RISE;
      for (int i = 1; i <= MAX_REACH; i++) {
        BlockPos below = pos.down(i);
        IBlockState s = world.getBlockState(below);
        if (!(s.getBlock() instanceof BlockBusArrivalBoardPart) && !world.isAirBlock(below)) {
          gap = i - 1;
          break;
        }
      }
      postPx = BoardShape.BOTTOM_GAP_PX + 16 * gap;
    }
    renderBox = board.worldOutline(pos, state).grow(0.25)
        .expand(0, rodPx / 16.0, 0).expand(0, -postPx / 16.0, 0);
    resolveStop();
  }

  /** Finds the stop the board follows and the routes it lists. */
  @SideOnly(Side.CLIENT)
  private void resolveStop() {
    BusStation station = BusStation.at(world, pos);
    int best = -1;
    double bestDist = Double.MAX_VALUE;
    for (int i = 0; i < station.bays(); i++) {
      if (station.bayRouteCount(i) <= 0) {
        continue;   // a flag with no routes (a full-size flag's upper cell) is not a stop
      }
      BlockPos p = station.bayPos(i);
      int dx = Math.abs(p.getX() - pos.getX());
      int dz = Math.abs(p.getZ() - pos.getZ());
      int dy = Math.abs(p.getY() - pos.getY());
      if (dx > STOP_REACH || dz > STOP_REACH || dy > STOP_REACH_VERTICAL) {
        continue;
      }
      double dist = dx * dx + dz * dz + dy * dy * 0.25;
      if (dist < bestDist) {
        bestDist = dist;
        best = i;
      }
    }
    int[] own = info.getRouteKeys();
    int[] from;
    int count;
    if (own.length > 0) {
      from = own;
      count = own.length;
    } else if (best >= 0) {
      from = station.bayRoutes(best);
      count = station.bayRouteCount(best);
    } else {
      from = own;
      count = 0;
    }
    count = Math.min(count, routes.length);
    int x = best >= 0 ? station.bayPos(best).getX() : pos.getX();
    int z = best >= 0 ? station.bayPos(best).getZ() : pos.getZ();
    boolean changed = count != routeCount || x != seedX || z != seedZ;
    for (int i = 0; i < count; i++) {
      changed |= routes[i] != from[i];
      routes[i] = from[i];
    }
    routeCount = count;
    seedX = x;
    seedZ = z;
    if (changed) {
      sourceVersion++;
    }
  }

  /** The board's block as last looked at, or null if this is no longer a board. */
  BlockBusArrivalBoard getBoard() {
    return board;
  }

  /** The board's measurements, made again only when its block or mount changes. */
  BoardShape getShape() {
    return shape;
  }

  EnumFacing getFacing() {
    return facing;
  }

  BoardMount getMount() {
    return mount;
  }

  /** The routes listed, as last looked up; the first {@link #getRouteCount} are used. */
  int[] getRoutes() {
    return routes;
  }

  int getRouteCount() {
    return routeCount;
  }

  int getSeedX() {
    return seedX;
  }

  int getSeedZ() {
    return seedZ;
  }

  int getSourceVersion() {
    return sourceVersion;
  }

  int getRodPx() {
    return rodPx;
  }

  int getPostPx() {
    return postPx;
  }

  int getLight() {
    return light;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    if (renderBox == null) {
      return new AxisAlignedBB(pos).grow(3);
    }
    return renderBox;
  }

  /** A board meant to be read across a street. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(96.0 * 96.0);
  }

  /** Releases the board's lists when it is removed. */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityBusArrivalBoardRenderer.release(pos);
    }
  }

  /** And when its chunk unloads. */
  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityBusArrivalBoardRenderer.release(pos);
    }
  }
}
