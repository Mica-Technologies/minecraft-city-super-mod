package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A wire mount carrying a cluster of signals from one point on the span.
 *
 * <p>Real span wire routinely hangs two to four heads from a single mast, on a horizontal bracket
 * across the bottom of it -- it is how a span covers several lanes without a mast per lane. This
 * is that: one attachment, one mast, one clamp, and a bracket the heads hang along.
 *
 * <p>The bracket runs <b>along the cable</b>, not along the block's facing. That is derived rather
 * than configured because it is not really a choice: a bracket square to anything but the span
 * would fight the wire it hangs from, and now that spans may run diagonally the block's own
 * facing would frequently be the wrong answer.
 */
public class TileEntitySpanWireClusterMount extends TileEntitySpanWireHanger {

  private static final String WIDTH_KEY = "swCW";

  /** Narrowest and widest cluster. One head is not a cluster; beyond four is not a span wire. */
  public static final int MIN_WIDTH = 2;
  public static final int MAX_WIDTH = 4;

  /**
   * How far below the attach point the bracket runs.
   *
   * <p>Has to clear the tops of the heads it carries, which is tighter than it looks: a head one
   * block down reaches 0.5 above its own block, so with the attach point 0.75 up there is only a
   * quarter of a block to work in. At 0.12 the bracket's underside clears a head top by about a
   * pixel. Heads on a cluster take no span rise ({@code SpanWireHangOffset}), without which this
   * clearance does not exist at all.
   *
   * <p>Lives here rather than in the renderer because the mast has to stand on the bar, so both
   * need the same number.
   */
  public static final double BRACKET_DROP = 0.12;

  private int clusterWidth = MIN_WIDTH;

  /**
   * The block columns this cluster's bracket reaches over, its own included. Derived from the
   * width and the cable's direction whenever either changes, and held so the signals underneath
   * can ask a cheap question instead of repeating the geometry.
   */
  private final List<BlockPos> coveredColumns = new ArrayList<>();

  /**
   * What is actually hanging under each covered column, resolved once rather than per frame.
   *
   * <p>A cluster's whole point is that the heads on it need not face the same way, and a head's
   * body is set back along the way it faces -- so every drop off this bracket lands somewhere
   * different, and each one has to ask its own head where. Empty columns are simply absent, which
   * is what stops the bracket growing arms into thin air.
   */
  private final List<ClusterPayload> payloads = new ArrayList<>();

  /** One head under the bracket: which column, where its hardware wants to be met, and its roof. */
  public static final class ClusterPayload {

    private final BlockPos column;
    private final Vec3d hardwareOffset;
    private final double topY;
    private final double tieY;

    ClusterPayload(BlockPos column, Vec3d hardwareOffset, double topY, double tieY) {
      this.column = column;
      this.hardwareOffset = hardwareOffset;
      this.topY = topY;
      this.tieY = tieY;
    }

    public BlockPos getColumn() {
      return column;
    }

    /** Horizontal shift from the column's centre line to where this head should be met. */
    public Vec3d getHardwareOffset() {
      return hardwareOffset;
    }

    /** World height of this head's roof, or {@code NaN} if it did not report one. */
    public double getTopY() {
      return topY;
    }

    /**
     * World height of this head's underside, or {@code NaN} if it did not report one.
     *
     * <p>The same height a box span's tether ties at, reused: both are asking where the bottom of
     * the housing is, and a cluster's lower tie bar meets it in exactly the same place.
     */
    public double getTieY() {
      return tieY;
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    if (compound.hasKey(WIDTH_KEY)) {
      clusterWidth = clampWidth(compound.getInteger(WIDTH_KEY));
    }
    recomputeCoverage();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setInteger(WIDTH_KEY, clusterWidth);
    return compound;
  }

  @Override
  protected void onSpanChanged() {
    super.onSpanChanged();
    recomputeCoverage();
  }

  @Override
  public void refreshPayload() {
    super.refreshPayload();
    refreshClusterPayloads();
  }

  /** The heads hanging under this bracket, in order along it. Never null, often empty. */
  public List<ClusterPayload> getPayloads() {
    return Collections.unmodifiableList(payloads);
  }

  /**
   * Re-reads what hangs under each covered column.
   *
   * <p>Run whenever coverage or the span changes, and from the mount's own payload refresh, which
   * is what catches a head being placed under an already-linked cluster.
   */
  private void refreshClusterPayloads() {
    payloads.clear();
    if (world == null) {
      return;
    }
    for (BlockPos column : coveredColumns) {
      final BlockPos payloadPos = new BlockPos(column.getX(), pos.getY() - 1, column.getZ());
      if (!world.isBlockLoaded(payloadPos)) {
        continue;
      }
      final IBlockState state = world.getBlockState(payloadPos);
      if (!(state.getBlock() instanceof ISpanWireHangable)) {
        continue;
      }
      final ISpanWireHangable payload = (ISpanWireHangable) state.getBlock();
      payloads.add(new ClusterPayload(column,
          payload.getSpanHardwareOffset(world, payloadPos, state),
          payload.getSpanHangerTopY(world, payloadPos, state),
          payload.getSpanTetherTieY(world, payloadPos, state)));
    }
  }

  @Override
  public long getHardwareStateKey() {
    long key = super.getHardwareStateKey() * 31L + clusterWidth;
    // Folded in so the bracket is rebuilt when a head is added, removed or turned -- each of those
    // moves a drop, and none of them changes anything else the key already covers.
    for (ClusterPayload payload : payloads) {
      key = key * 31L + payload.getColumn().hashCode();
      key = key * 31L + Math.round(payload.getHardwareOffset().x * 64.0);
      key = key * 31L + Math.round(payload.getHardwareOffset().z * 64.0);
      key = key * 31L + (Double.isNaN(payload.getTieY())
          ? 0L
          : Math.round(payload.getTieY() * 64.0));
    }
    return key;
  }

  public int getClusterWidth() {
    return clusterWidth;
  }

  /** Advances the cluster width, wrapping back to the narrowest. */
  public void cycleClusterWidth() {
    clusterWidth = clusterWidth >= MAX_WIDTH ? MIN_WIDTH : clusterWidth + 1;
    recomputeCoverage();
    if (world != null) {
      if (world.isRemote) {
        SpanWireCableRenderer.cleanupDisplayList(pos);
      }
      markDirtySync(world, pos, true);
    }
  }

  /** Whether a signal standing in this column hangs from this cluster's bracket. */
  public boolean covers(BlockPos column) {
    for (BlockPos covered : coveredColumns) {
      if (covered.getX() == column.getX() && covered.getZ() == column.getZ()) {
        return true;
      }
    }
    return false;
  }

  /** The columns the bracket reaches, in order along the cable. */
  public List<BlockPos> getCoveredColumns() {
    return coveredColumns;
  }

  /**
   * The columns the bracket actually spans: the occupied ones, or the covered ones when nothing
   * hangs yet.
   *
   * <p>A four-wide cluster carrying two heads should not reach two empty columns further along
   * holding nothing, but an empty cluster should still read as a cluster waiting for heads. Both
   * the bar and the mast that stands on it ask this, so the mast cannot end up centred on a
   * different bar than the one drawn.
   *
   * @return the columns to span, never empty for a linked cluster.
   */
  public List<BlockPos> getBracketColumns() {
    if (payloads.isEmpty()) {
      return coveredColumns;
    }
    final List<BlockPos> occupied = new ArrayList<>(payloads.size());
    for (ClusterPayload payload : payloads) {
      occupied.add(payload.getColumn());
    }
    return occupied;
  }

  /**
   * Where along the bracket a payload actually hangs, in blocks from the attach point.
   *
   * <p>Measured to the payload's <b>hardware point</b> -- the place on its housing the drop lands
   * -- rather than to the middle of its block. Those are not the same place: a head's body is set
   * back along the way it faces, so two heads on one bracket facing different ways sit at different
   * distances along it.
   *
   * @param payload the payload to measure.
   *
   * @return the signed distance along the bracket.
   */
  public double alongFor(ClusterPayload payload) {
    final Vec3d attach = getAttachPoint();
    final Vec3d along = getBracketDirection();
    return (hardwareX(payload) - attach.x) * along.x + (hardwareZ(payload) - attach.z) * along.z;
  }

  /**
   * The point on the bracket a payload hangs from, in world coordinates at the attach height.
   *
   * <p>This is the payload's hardware point projected onto the bracket, and it is the single
   * definition three things now share: the bar spans between these, each drop falls straight down
   * from one, and each head slides sideways onto one. Before they shared it, the bar ran down the
   * span while the drops reached out to the housings, so a drop passed under the bar and then bent
   * away to meet its head.
   *
   * @param payload the payload to place.
   *
   * @return the hanging point, with the attach point's own height.
   */
  public Vec3d bracketPointFor(ClusterPayload payload) {
    final Vec3d attach = getAttachPoint();
    final Vec3d along = getBracketDirection();
    final double distance = alongFor(payload);
    return new Vec3d(attach.x + along.x * distance, attach.y, attach.z + along.z * distance);
  }

  private static double hardwareX(ClusterPayload payload) {
    return payload.getColumn().getX() + 0.5 + payload.getHardwareOffset().x;
  }

  private static double hardwareZ(ClusterPayload payload) {
    return payload.getColumn().getZ() + 0.5 + payload.getHardwareOffset().z;
  }

  /**
   * A clustered head slides onto the bracket, and takes no rise.
   *
   * <p>The bracket is a straight bar and the heads bolt to it, so the heads belong on its line.
   * Sliding them there is what lets every drop fall straight down; the height is still the
   * bracket's own, which is why {@link #getPayloadRise()} stays zero here.
   */
  @Override
  public Vec3d getPayloadSlide(BlockPos payloadPos) {
    if (!payloadMoves() || payloadPos == null) {
      return Vec3d.ZERO;
    }
    for (ClusterPayload payload : payloads) {
      if (payload.getColumn().getX() == payloadPos.getX()
          && payload.getColumn().getZ() == payloadPos.getZ()) {
        final Vec3d hanging = bracketPointFor(payload);
        return new Vec3d(hanging.x - hardwareX(payload), 0.0, hanging.z - hardwareZ(payload));
      }
    }
    return Vec3d.ZERO;
  }

  /**
   * Clustered payloads move sideways even though they take no rise, so this cannot defer to the
   * rise the way the single mount does.
   */
  @Override
  protected boolean payloadMoves() {
    return true;
  }

  /**
   * How far along the bracket a column sits, in blocks from this mount.
   *
   * @param column the column to measure.
   * @param along  the bracket's unit direction.
   *
   * @return signed distance along the bracket.
   */
  public double offsetAlongBracket(BlockPos column, Vec3d along) {
    return (column.getX() - pos.getX()) * along.x + (column.getZ() - pos.getZ()) * along.z;
  }

  /**
   * The middle of the bracket, in blocks along it from this mount.
   *
   * <p>Not zero, and that is the point. A two-wide cluster covers its own column and the next one
   * along -- {@code -(2 - 1) / 2} truncates to zero in Java, and there is no half column to centre
   * on anyway -- so its bar runs from this block forward and its middle is half a block along.
   * Standing the mast on this rather than on the block is what stops the mast coming down at one
   * end of the bar.
   *
   * @return the offset of the bar's midpoint.
   */
  public double getBracketCentreOffset() {
    double lowest = Double.POSITIVE_INFINITY;
    double highest = Double.NEGATIVE_INFINITY;
    if (payloads.isEmpty()) {
      final Vec3d along = getBracketDirection();
      for (BlockPos column : coveredColumns) {
        final double offset = offsetAlongBracket(column, along);
        lowest = Math.min(lowest, offset);
        highest = Math.max(highest, offset);
      }
    } else {
      for (ClusterPayload payload : payloads) {
        final double offset = alongFor(payload);
        lowest = Math.min(lowest, offset);
        highest = Math.max(highest, offset);
      }
    }
    if (lowest > highest) {
      return 0.0;
    }
    return (lowest + highest) * 0.5;
  }

  /**
   * The mast stands on the middle of the bracket, not on this block.
   *
   * <p>A cluster hangs from one point and spreads its heads either side of it; a mast at one end
   * of the bar reads as a bar bolted to the side of a mount rather than as a cluster. The bar
   * itself cannot move -- its drops have to land on the heads that are actually there -- so it is
   * the mast that goes to the middle.
   *
   * <p>Stops at the bar, because from there it is the drops that carry the heads. The single
   * mount's version reaches all the way down to its payload's roof, which on a cluster would put
   * the mast through the bracket and down the back of whichever head it happened to land on.
   */
  @Override
  public Vec3d getHardwareFootPoint() {
    final Vec3d attach = getAttachPoint();
    final Vec3d along = getBracketDirection();
    final double centre = getBracketCentreOffset();
    return new Vec3d(attach.x + along.x * centre,
        attach.y - BRACKET_DROP,
        attach.z + along.z * centre);
  }

  /**
   * The direction the bracket runs, as a unit vector along the cable at this mount. Falls back to
   * east for an unlinked cluster, which is only ever used to draw a bracket nothing hangs from.
   */
  public Vec3d getBracketDirection() {
    final SpanWireCatenary cable = getCable();
    if (cable == null) {
      return new Vec3d(1.0, 0.0, 0.0);
    }
    final double t = getSpanParameter();
    final Vec3d along = cable.pointAt(Math.min(1.0, t + 0.01))
        .subtract(cable.pointAt(Math.max(0.0, t - 0.01)));
    if (along.x * along.x + along.z * along.z < 1.0e-12) {
      return new Vec3d(1.0, 0.0, 0.0);
    }
    // Flattened: the bracket is level even where the cable it hangs from is not.
    return new Vec3d(along.x, 0.0, along.z).normalize();
  }

  /**
   * Works out which columns the bracket reaches.
   *
   * <p>Centred on this mount, so a two-wide cluster reaches one column to one side and a
   * four-wide reaches two one way and one the other. Stepping along the cable's own direction
   * rather than a fixed axis is what makes this work on a diagonal span.
   */
  private void recomputeCoverage() {
    coveredColumns.clear();
    final Vec3d direction = getBracketDirection();
    final int firstOffset = -(clusterWidth - 1) / 2;

    for (int i = 0; i < clusterWidth; i++) {
      final int step = firstOffset + i;
      final int dx = (int) Math.round(direction.x * step);
      final int dz = (int) Math.round(direction.z * step);
      final BlockPos column = pos.add(dx, 0, dz);
      if (!coveredColumns.contains(column)) {
        coveredColumns.add(column);
      }
    }
    refreshClusterPayloads();
  }

  private static int clampWidth(int width) {
    return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
  }
}
