package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A mount hanging from a span of messenger cable.
 *
 * <p>This is the tile entity added to the existing wire mount blocks (the plan's D5), which
 * until now carried nothing. A mount that has never been linked into a span behaves exactly as
 * it always did -- decorative hardware -- so no existing build changes.
 *
 * <p>Beyond the span itself this holds the mount's own settings and one derived number: how far
 * below the cable it hangs. That is fixed for a given span, so it is computed when the span
 * changes rather than re-solved on demand. It sets the length of the mast drawn up to the cable,
 * and Phase 3 hands it to the signal below as its droop offset -- a path read every frame by the
 * signal head renderer.
 */
public class TileEntitySpanWireHanger extends AbstractTileEntitySpanWireAttachment {

  /** The furthest a drop will stretch to reach a payload below it. */
  private static final double MAX_HANGER_REACH = 2.0;

  private static final String MOUNT_STYLE_KEY = "swMS";
  private static final String COIL_STYLE_KEY = "swCS";

  /**
   * How far this mount hangs below the cable, in blocks -- the length of hardware between them.
   * Zero when unlinked. Negative would mean the mount is above the cable, which is a placement
   * error rather than a tight fit; see {@link SpanWireDefinition#cableDropAt}.
   */
  private double cableDrop = 0.0;

  /**
   * Whether the signal gives or the mast gives when the two do not meet. Defaults to
   * {@link SpanWireMountStyle#FLUSH}, which is what every mount did before the setting existed,
   * so an already-built span keeps the look it had.
   */
  private SpanWireMountStyle mountStyle = SpanWireMountStyle.FLUSH;

  /** How much coiled conductor slack hangs at this clamp. */
  private SpanWireCoilStyle coilStyle = SpanWireCoilStyle.ONE_SIDE;

  /**
   * Whether whatever hangs directly below wants a strap drawn onto it. Derived from the payload
   * rather than listed here, so a new kind of hangable block needs no edit to this class.
   */
  private transient boolean payloadTakesConductorFeed = true;

  /**
   * Whether the payload actually moves when this mount offers a rise. A sign does not, so its
   * reported geometry is already where it will be.
   */
  private transient boolean payloadTakesRise = false;

  /**
   * Where the hardware should come down to meet the payload, as an offset from the middle of
   * this block. Read from the payload rather than assumed, and cached with everything else that
   * is derived once rather than per frame.
   */
  private transient Vec3d payloadHardwareOffset = Vec3d.ZERO;

  /**
   * The height a box span's tether ties to on the payload below, or {@code NaN} for a payload
   * that takes no tie. Cached with everything else derived from the payload.
   */
  private transient double payloadTetherTieY = Double.NaN;

  /**
   * The height this mount's drop should reach down to on the payload, or {@code NaN} to stop at
   * this mount's own attach height.
   */
  private transient double payloadTopY = Double.NaN;

  /** How long a payload reading stays good for. One second; see {@link #refreshPayloadIfStale}. */
  private static final long PAYLOAD_REFRESH_TICKS = 20L;

  /** When the payload was last read, or {@link Long#MIN_VALUE} for never. */
  private transient long payloadReadTick = Long.MIN_VALUE;

  /**
   * Redraws the column this mount can carry, not just its own block.
   *
   * <p>A payload reads its mount by looking up to {@link SpanWireHangOffset#MAX_SEARCH_UP} blocks,
   * so a sign two blocks down, under a head, changes shape the moment this mount exists -- but
   * the render update a block placement gives off reaches one block around it, and a sign in the
   * render section below that goes on drawing as it was until something else rebuilds it. This
   * asks for that section too. Same on the way out, in {@link #invalidate()}.
   */
  @Override
  public void onLoad() {
    super.onLoad();
    markPayloadColumnForRender();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    markPayloadColumnForRender();
  }

  private void markPayloadColumnForRender() {
    if (world != null && world.isRemote) {
      world.markBlockRangeForRenderUpdate(pos.down(SpanWireHangOffset.MAX_SEARCH_UP), pos);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    // Absent keys leave the defaults in place, so a mount saved before these settings existed
    // reads back as the flush, single-coil mount it was drawn as.
    if (compound.hasKey(MOUNT_STYLE_KEY)) {
      mountStyle = SpanWireMountStyle.fromNBT(compound.getInteger(MOUNT_STYLE_KEY));
    }
    if (compound.hasKey(COIL_STYLE_KEY)) {
      coilStyle = SpanWireCoilStyle.fromNBT(compound.getInteger(COIL_STYLE_KEY));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setInteger(MOUNT_STYLE_KEY, mountStyle.toNBT());
    compound.setInteger(COIL_STYLE_KEY, coilStyle.toNBT());
    return compound;
  }

  @Override
  protected void onSpanChanged() {
    super.onSpanChanged();
    refreshPayload();
    final SpanWireCatenary cable = getCable();
    // Reuses the cable the base class already solved rather than calling span.cableDropAt,
    // which would solve it a second time for the same answer.
    cableDrop = cable == null
        ? 0.0
        : cable.heightAt(getSpanParameter())
            - (pos.getY() + SpanWireDefinition.CABLE_ATTACH_HEIGHT);
  }

  /**
   * How far this mount hangs below the cable, in blocks. Positive is the buildable case -- the
   * hardware reaches up from the mount to the cable. Zero when not part of a span.
   */
  public double getCableDrop() {
    return cableDrop;
  }

  @Override
  public long getHardwareStateKey() {
    // The one per-frame entry point this mount has, so it is where the stale check goes -- not in
    // the accessors below. Those are only reached while a display list is being compiled, and a
    // list that is already cached never compiles again, so a reading refreshed only from there
    // could never notice it had gone stale: cached list, no refresh, no key change, cached list.
    refreshPayloadIfStale();

    long key = (mountStyle.ordinal() * 31L + coilStyle.ordinal()) * 31L
        + (payloadTakesConductorFeed ? 1L : 0L);
    // Quantised rather than taken raw off the double: the offset only ever holds a handful of
    // values, and this keeps the key stable against the last bit of floating point noise.
    key = key * 31L + Math.round(payloadHardwareOffset.x * 64.0);
    key = key * 31L + Math.round(payloadHardwareOffset.z * 64.0);
    // How far the payload reaches, which is what the drop and the tether tie are drawn to. Left
    // out until now, so bolting an add-on onto a head redrew nothing unless the change happened
    // to move the span as well.
    key = key * 31L + quantise(payloadTetherTieY);
    key = key * 31L + quantise(payloadTopY);
    return key;
  }

  /**
   * A height as a stable key term, or a sentinel for "none".
   *
   * <p>{@code NaN} is a real answer here -- it is what a payload that takes no tie reports -- and
   * it has to be distinguishable from a height, which rounding it would not be.
   */
  private static long quantise(double height) {
    return Double.isNaN(height) ? Long.MIN_VALUE : Math.round(height * 64.0);
  }

  /** Whether to draw a strap from this mount down onto what it carries. */
  public boolean payloadTakesConductorFeed() {
    return payloadTakesConductorFeed;
  }

  /**
   * Re-reads what hangs below. Called when the span changes and from the block's neighbour
   * notification, which is what catches a sign being placed under an already-strung span.
   */
  public void refreshPayload() {
    readPayload();
  }

  /**
   * Reads what hangs below, without going through {@link #refreshPayload()}.
   *
   * <p>Private, and called directly by the stale-refresh path, so that a lazy refresh cannot reach
   * a subclass override. A cluster's override rebuilds the payload list its renderer iterates, and
   * a rebuild triggered from inside that iteration would be a concurrent modification. A cluster
   * keeps its own refresh for its own list; this one only covers the fields declared here.
   */
  private void readPayload() {
    payloadReadTick = world == null ? Long.MIN_VALUE : world.getTotalWorldTime();
    final boolean previousFeed = payloadTakesConductorFeed;
    final Vec3d previousOffset = payloadHardwareOffset;

    final IBlockState below = world == null ? null : world.getBlockState(pos.down());
    if (below != null && below.getBlock() instanceof ISpanWireHangable) {
      final ISpanWireHangable payload = (ISpanWireHangable) below.getBlock();
      payloadTakesConductorFeed = payload.needsSpanConductorFeed();
      payloadTakesRise = payload.takesSpanRise();
      payloadHardwareOffset = payload.getSpanHardwareOffset(world, pos.down(), below);
      payloadTetherTieY = payload.getSpanTetherTieY(world, pos.down(), below);
      payloadTopY = payload.getSpanHangerTopY(world, pos.down(), below);
    } else {
      payloadTakesConductorFeed = true;
      payloadTakesRise = false;
      payloadHardwareOffset = Vec3d.ZERO;
      payloadTetherTieY = Double.NaN;
      payloadTopY = Double.NaN;
    }

    if ((previousFeed != payloadTakesConductorFeed
        || !previousOffset.equals(payloadHardwareOffset))
        && world != null && world.isRemote) {
      SpanWireCableRenderer.cleanupDisplayList(pos);
    }
  }

  /**
   * Where this mount's hardware comes down to meet what it carries.
   *
   * <p>Measured from the block's own centre line, <b>not</b> from {@link #getAttachPoint()}.
   * That distinction is the whole of it: the attach point is already shifted sideways by the
   * span, and since the span now follows these same payload offsets by default, adding the
   * payload offset on top of it applied the setback twice and stood the mast off the back of the
   * housing.
   *
   * <p>Keeping the two separate is also what makes the geometry self-correcting. The cable sits
   * at the span's offset and the foot at the payload's; when they agree -- which is exactly what
   * the automatic side arranges -- the offset arm has zero length and the mast is plumb. When a
   * builder overrides the side, the arm grows by the difference and still reaches.
   */
  /**
   * The height a box span's tether ties to on what this mount carries, or {@code NaN} for none.
   *
   * <p>The payload reports its underside before any span rise; this adds the rise this mount is
   * giving it. See {@link #getPayloadRise()}.
   */
  public double getPayloadTetherTieY() {
    return Double.isNaN(payloadTetherTieY)
        ? Double.NaN
        : payloadTetherTieY + appliedRise();
  }

  /**
   * Re-reads the payload if the last reading has gone stale.
   *
   * <p>Exists because the notifications this mount does get are not enough to keep the reading
   * true. A mount hears about its own neighbours, so it learns when the head directly beneath it
   * is placed or broken -- but an add-on section going on the <em>bottom</em> of that head is two
   * blocks down and silent, and it changes how far the assembly reaches. The alternative is
   * re-stringing the span to force a refresh, which is a workaround rather than a fix.
   *
   * <p>Slow on purpose. One second is far below noticing and far above per-frame.
   */
  private void refreshPayloadIfStale() {
    if (world == null) {
      return;
    }
    final long now = world.getTotalWorldTime();
    if (payloadReadTick != Long.MIN_VALUE && now - payloadReadTick < PAYLOAD_REFRESH_TICKS) {
      return;
    }
    readPayload();
  }

  /**
   * The rise this mount's payload actually ends up with: the rise on offer, or nothing at all for
   * a payload that cannot move.
   */
  private double appliedRise() {
    return payloadTakesRise ? getPayloadRise() : 0.0;
  }

  /**
   * How far the payload should slide sideways so it hangs directly under the clamp, in blocks.
   *
   * <p>The horizontal companion to {@link #getPayloadRise()}, and the reason it is not always zero
   * is the same reason the clamp needed moving: a housing does not sit on its block's centre line,
   * so the mast reaches across to it and the head hangs beside the wire rather than under it.
   * Raising the head without also sliding it fixes the height and leaves the sideways gap, which is
   * what the arm was papering over.
   *
   * <p>This is the perpendicular from the mast foot to the span, because the clamp is that foot
   * projected onto the span. Following it puts the head under the wire whichever way the span has
   * been shifted -- a centred span pulls the head onto its own centre line, and a span already
   * lined up with the housings asks for nothing.
   *
   * <p>Only where the mount actually lowers its payload, so it follows the rise exactly: an
   * extending mast exists to keep the payload on its own block, and a cluster's heads are carried
   * by drops that already aim at each housing, so moving either would break what holds them.
   *
   * @param payloadPos the payload's own position. Ignored here, where a mount carries one thing;
   *     a cluster carries several and answers differently for each.
   *
   * @return the sideways shift in blocks, with a zero Y, never null.
   */
  public Vec3d getPayloadSlide(BlockPos payloadPos) {
    if (!payloadMoves() || getPayloadRise() <= 0.0) {
      return Vec3d.ZERO;
    }
    final SpanWireCatenary cable = getCable();
    if (cable == null) {
      return Vec3d.ZERO;
    }
    final Vec3d foot = getHardwareFootPoint();
    final Vec3d clamp = cable.pointAt(cable.parameterAt(foot.x, foot.z));
    return new Vec3d(clamp.x - foot.x, 0.0, clamp.z - foot.z);
  }

  /**
   * Whether what hangs here moves with the hardware at all.
   *
   * <p>A sign does not: its reported geometry is already where it will be, so shifting it would
   * take it away from itself.
   *
   * @return true when the payload follows this mount.
   */
  protected boolean payloadMoves() {
    return payloadTakesRise;
  }

  /**
   * How far this mount lifts what it carries, in blocks.
   *
   * <p>The single definition of the rise. {@code SpanWireHangOffset} converts this for the signal
   * head renderer and the hardware here adds it to the payload's reported geometry, so the two
   * cannot disagree about where a head has ended up.
   *
   * <p>Zero for an extending mast, which exists precisely so the payload stays on its own block,
   * and zero for a cluster, whose bracket is rigid and holds everything on it level.
   */
  public double getPayloadRise() {
    if (mountStyle == SpanWireMountStyle.MAST || this instanceof TileEntitySpanWireClusterMount) {
      return 0.0;
    }
    final double drop = getCableDrop();
    return drop <= 0.0 ? 0.0 : Math.min(drop, mountStyle.getMaximumDrop());
  }

  public Vec3d getHardwareFootPoint() {
    final Vec3d foot = SpanWireDefinition.attachPoint(pos).add(payloadHardwareOffset);
    if (Double.isNaN(payloadTopY)) {
      return foot;
    }
    final double payloadTop = payloadTopY + appliedRise();
    // Reaches down to the payload rather than stopping at this mount's own attach height. Only
    // lower, never higher: a payload that has risen to meet the hardware is already touching it,
    // and pulling the drop up to its top would bury the foot inside it. Bounded so a payload
    // reporting nonsense cannot stretch the drop to the ground.
    final double reach = Math.max(foot.y - MAX_HANGER_REACH, Math.min(foot.y, payloadTop));
    return new Vec3d(foot.x, reach, foot.z);
  }

  public SpanWireMountStyle getMountStyle() {
    return mountStyle;
  }

  public SpanWireCoilStyle getCoilStyle() {
    return coilStyle;
  }

  /** Advances the mount style and pushes the change to clients. */
  public void cycleMountStyle() {
    mountStyle = mountStyle.getNext();
    onSettingChanged();
  }

  /**
   * Advances the signal side for the <b>whole span</b>, not just this mount.
   *
   * <p>Handled by the manager rather than here because the change belongs to the span: every
   * attachment holds a copy of it, and all of them have to be rewritten together.
   */
  /** Adds or removes the span's lower tether. Span-wide, like the signal side. */
  public void toggleBoxSpan() {
    if (world != null && !world.isRemote) {
      SpanWireManager.toggleBoxSpan(world, pos);
    }
  }

  public void cycleSignalSide() {
    if (world != null && !world.isRemote) {
      SpanWireManager.cycleSignalSide(world, pos);
    }
  }

  /** Advances the coil style and pushes the change to clients. */
  public void cycleCoilStyle() {
    coilStyle = coilStyle.getNext();
    onSettingChanged();
  }

  /**
   * Both settings change what is drawn, so the cached geometry has to go with them -- the mount
   * style also moves the signal underneath, whose own cache picks the change up on its next
   * refresh.
   */
  private void onSettingChanged() {
    if (world != null) {
      if (world.isRemote) {
        SpanWireCableRenderer.cleanupDisplayList(pos);
      }
      markDirtySync(world, pos, true);
    }
  }
}
