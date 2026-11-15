package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A signal cluster hanger: one block above the middle of a cluster of heads, drawing the bracketing
 * that holds them -- the top frame, a drop to each head, the lower tie, and the clamp up to the
 * wire or arm above.
 *
 * <p>Built on the span wire cluster mount, and different from it in three ways:</p>
 * <ul>
 *   <li>it claims heads in the three-by-three columns under it ({@link SignalClusterFrame}), not
 *       a line along a cable, so it works unlinked under a mast arm as well as on a span;</li>
 *   <li>the heads stay exactly where they were placed. Nothing slides onto a bar; the frame is
 *       centred on the heads instead, and its arms reach out to each one;</li>
 *   <li>the hardware is painted in any signal body colour.</li>
 * </ul>
 */
public class TileEntitySignalClusterHanger extends TileEntitySpanWireClusterMount {

  private static final String COLOR_KEY = "scC";

  /** The retired span wire cluster mount's hand-picked width, which the hanger has no use for. */
  static final String RETIRED_WIDTH_KEY = "swCW";

  /**
   * The colour a retired span wire cluster mount becomes. Its hardware was a fixed steel grey
   * (0.30, 0.31, 0.33); Battleship Gray is the body colour that still reads as galvanised steel.
   */
  public static final TrafficSignalBodyColor RETIRED_CLUSTER_MOUNT_COLOR =
      TrafficSignalBodyColor.BATTLESHIP_GRAY;

  private TrafficSignalBodyColor color = TrafficSignalBodyColor.FLAT_BLACK;

  /**
   * A retired span wire cluster mount's tile entity data, made into a hanger's.
   *
   * <p>Everything the two share is kept as it is: the span (cable, sag, signal side, the box
   * span's tether and its anchors), the mount style and the coil style. The width is dropped,
   * since the hanger finds its heads itself, and the colour is set to
   * {@link #RETIRED_CLUSTER_MOUNT_COLOR}, the old steel. A colour already present is kept.
   *
   * @param old the old mount's NBT; not changed
   *
   * @return a new compound for the hanger to read
   */
  public static NBTTagCompound fromRetiredClusterMountNBT(NBTTagCompound old) {
    final NBTTagCompound converted = old.copy();
    converted.removeTag(RETIRED_WIDTH_KEY);
    if (!converted.hasKey(COLOR_KEY)) {
      converted.setInteger(COLOR_KEY, RETIRED_CLUSTER_MOUNT_COLOR.toNBT());
    }
    return converted;
  }

  /** How often, in ticks, the block above is read again: an arm placed later is picked up. */
  private static final long UPPER_MOUNT_REFRESH_TICKS = 20L;

  /** The render bounds, made once: the footprint plus the depth a hung assembly can reach. */
  private transient AxisAlignedBB frameBounds;

  /** What the hanger hangs from when unlinked; read from the block above, at most once a second. */
  private transient SignalClusterUpperMount upperMount = SignalClusterUpperMount.NONE;
  private transient long upperMountReadTick = Long.MIN_VALUE;

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    if (compound.hasKey(COLOR_KEY)) {
      color = TrafficSignalBodyColor.fromNBT(compound.getInteger(COLOR_KEY));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setInteger(COLOR_KEY, color.toNBT());
    return compound;
  }

  /**
   * Claims the footprint as soon as the hanger exists. A freshly placed hanger never reads NBT and
   * is never linked, and without a published claim the heads in its neighbouring columns would
   * never look sideways for it.
   */
  @Override
  public void onLoad() {
    super.onLoad();
    recomputeCoverage();
  }

  @Override
  protected void computeCoveredColumns(List<BlockPos> out) {
    SignalClusterFrame.footprint(pos, out);
  }

  /**
   * Heads only: a payload that takes a span's rise is one drawn by a renderer, which is what a
   * signal head is. A sign beside a cluster is not bolted to its frame.
   *
   * <p>The head's hand nudge is folded into where its drop lands, because the head is drawn where
   * the nudge put it and the drop has to meet it there.
   */
  @Override
  protected ClusterPayload readPayloadAt(BlockPos column) {
    final BlockPos payloadPos = new BlockPos(column.getX(), pos.getY() - 1, column.getZ());
    if (world == null || !world.isBlockLoaded(payloadPos)) {
      return null;
    }
    final IBlockState state = world.getBlockState(payloadPos);
    if (!(state.getBlock() instanceof ISpanWireHangable)) {
      return null;
    }
    final ISpanWireHangable payload = (ISpanWireHangable) state.getBlock();
    if (!payload.takesSpanRise()) {
      return null;
    }
    return new ClusterPayload(column,
        payload.getSpanHardwareOffset(world, payloadPos, state)
            .add(payload.getSpanHandOffset(world, payloadPos, state)),
        payload.getSpanHangerTopY(world, payloadPos, state),
        payload.getSpanTetherTieY(world, payloadPos, state));
  }

  /** The heads stay where they were placed; the frame reaches out to them instead. */
  @Override
  protected boolean payloadMoves() {
    return false;
  }

  /**
   * Whether this hanger's frame drops to a head at this position.
   *
   * @param headPos the head's position.
   *
   * @return true for a head in the footprint, one row down.
   */
  public boolean carries(BlockPos headPos) {
    return SignalClusterFrame.carries(pos, headPos);
  }

  /**
   * The middle of the frame, at bracket height: the centroid of the points the drops land on, or
   * the attach point when nothing hangs yet.
   *
   * @return the hub of the top frame, in world coordinates.
   */
  public Vec3d getFrameHub() {
    final Vec3d attach = getAttachPoint();
    final Vec3d fallback = new Vec3d(attach.x, attach.y - BRACKET_DROP, attach.z);
    final List<ClusterPayload> payloads = getPayloads();
    final List<Vec3d> points = new ArrayList<>(payloads.size());
    for (ClusterPayload payload : payloads) {
      points.add(hardwarePoint(payload));
    }
    return SignalClusterFrame.centroid(points, fallback);
  }

  /** Where a payload's drop lands, horizontally, in world coordinates. Y is zero. */
  public static Vec3d hardwarePoint(ClusterPayload payload) {
    return new Vec3d(hardwareX(payload), 0.0, hardwareZ(payload));
  }

  /**
   * The mast or clamp comes down onto the frame's hub, not onto the bracket line the span cluster
   * centres on: this frame is not a line.
   */
  @Override
  public Vec3d getHardwareFootPoint() {
    return getFrameHub();
  }

  /**
   * What the hanger hangs from when it is not on a span: an arm to clamp, something to bolt a
   * flange to, or nothing. Re-read about once a second, and at once after a neighbour changes.
   *
   * @return the mount above; never null
   */
  public SignalClusterUpperMount getUpperMount() {
    if (world == null) {
      return SignalClusterUpperMount.NONE;
    }
    final long now = world.getTotalWorldTime();
    if (upperMountReadTick == Long.MIN_VALUE
        || now - upperMountReadTick >= UPPER_MOUNT_REFRESH_TICKS
        || now < upperMountReadTick) {
      upperMountReadTick = now;
      upperMount = world.isBlockLoaded(pos.up())
          ? SignalClusterUpperMount.find(world, pos)
          : SignalClusterUpperMount.NONE;
    }
    return upperMount;
  }

  /** Reads the block above again on the next draw, as well as the heads below. */
  @Override
  public void refreshPayload() {
    super.refreshPayload();
    upperMountReadTick = Long.MIN_VALUE;
  }

  @Override
  public long getHardwareStateKey() {
    long key = super.getHardwareStateKey() * 31L + color.ordinal();
    // Only drawn unlinked, but cheap to fold in either way: an arm placed above, or turned,
    // changes the clamp.
    key = key * 31L + getUpperMount().stateKey();
    // The drops reach each head's top, which the span cluster's key leaves out because its drops
    // are a fixed stub. A head swapped for a shorter one moves a drop here.
    for (ClusterPayload payload : getPayloads()) {
      key = key * 31L + (Double.isNaN(payload.getTopY())
          ? 0L
          : Math.round(payload.getTopY() * 64.0));
    }
    return key;
  }

  public TrafficSignalBodyColor getColor() {
    return color;
  }

  /**
   * Advances the hardware colour and pushes the change to clients.
   *
   * @return the new colour.
   */
  public TrafficSignalBodyColor cycleColor() {
    setColor(color.getNextColor());
    return color;
  }

  /**
   * Sets the hardware colour, as a copied colour is pasted onto the hanger, and pushes the change
   * to clients.
   *
   * @param newColor the colour; not null
   */
  public void setColor(TrafficSignalBodyColor newColor) {
    color = newColor;
    if (world != null) {
      if (world.isRemote) {
        TileEntitySignalClusterHangerRenderer.cleanupUnlinkedList(pos);
      }
      markDirtySync(world, pos, true);
    }
  }

  /**
   * Covers the whole footprint and the depth a hung assembly reaches, so the frame is not culled
   * while its arms and drops are still on screen.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    if (frameBounds == null) {
      frameBounds = new AxisAlignedBB(
          pos.getX() - SignalClusterFrame.REACH, pos.getY() - SpanWireHangOffset.MAX_SEARCH_UP,
          pos.getZ() - SignalClusterFrame.REACH,
          // Up into the block above, where the clamp grips an arm's pipe.
          pos.getX() + 1 + SignalClusterFrame.REACH, pos.getY() + 2,
          pos.getZ() + 1 + SignalClusterFrame.REACH);
    }
    return super.getRenderBoundingBox().union(frameBounds);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntitySignalClusterHangerRenderer.cleanupUnlinkedList(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntitySignalClusterHangerRenderer.cleanupUnlinkedList(pos);
    }
  }
}
