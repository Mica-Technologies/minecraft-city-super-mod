package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Shared behaviour for anything a span of messenger cable attaches to -- the anchors at either
 * end and the hanger mounts along it.
 *
 * <p>Both kinds hold the same thing (a copy of the whole span) and draw the same thing (the
 * piece of cable running from themselves to the next attachment), so the storage, the NBT and
 * everything derived from the span live here. The subclasses add only what is genuinely theirs.
 *
 * <p><b>Everything derived from the span is computed once, when the span changes.</b> The cable
 * is solved here rather than in the renderer for a specific reason: Minecraft asks every visible
 * tile entity for its render bounding box every frame, and the renderer needs the curve on top of
 * that. Solving on demand would mean a Newton iteration per attachment per frame for every span
 * in view — invisible until someone builds a corridor of them, and exactly the kind of per-frame
 * cost the mod's performance work has spent time removing. A span changes when a player edits it,
 * which is many orders of magnitude rarer than a frame.
 */
public abstract class AbstractTileEntitySpanWireAttachment extends AbstractTileEntity
    implements ISpanWireAttachment {

  @Nullable
  private SpanWireDefinition span = null;

  /** The solved messenger for the whole span. Null when unlinked. */
  @Nullable
  private SpanWireCatenary cachedCable = null;

  /** The solved lower tether, when this is a box span. Null otherwise. */
  @Nullable
  private SpanWireCatenary cachedTether = null;

  /** The attachment this one draws cable to, or null if it is the far end and draws none. */
  @Nullable
  private BlockPos cachedNext = null;

  /** Where this attachment and the next sit along the span, as fractions. */
  private double cachedFromT = 0.0;
  private double cachedToT = 0.0;

  /**
   * Where this attachment's hardware meets the cable, with the span's sideways offset already
   * applied. Cached alongside everything else derived from the span so renderers do not each
   * recompute it, and so they cannot accidentally use the un-offset block centre.
   */
  @Nullable
  private Vec3d cachedAttachPoint = null;

  /** Bounds covering this block plus the cable it draws. Null until a span is set. */
  @Nullable
  private AxisAlignedBB cachedRenderBounds = null;

  @Override
  public void readNBT(NBTTagCompound compound) {
    span = SpanWireDefinition.readFromNBT(compound);
    onSpanChanged();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (span != null) {
      span.writeToNBT(compound);
    } else {
      SpanWireDefinition.clearNBT(compound);
    }
    return compound;
  }

  @Nullable
  @Override
  public SpanWireDefinition getSpan() {
    return span;
  }

  @Override
  public void setSpan(@Nullable SpanWireDefinition span) {
    this.span = span;
    onSpanChanged();
    if (world != null) {
      if (world.isRemote) {
        SpanWireCableRenderer.cleanupDisplayList(pos);
      }
      markDirtySync(world, pos, true);
      // Whatever hangs below has state that depends on this span existing -- a sign's setback,
      // most of all -- and stringing a span fires no block change of its own at the payload's
      // position. A neighbour notification is what reaches it, and it is the vanilla path those
      // blocks already listen on, so nothing has to know this system exists.
      if (!world.isRemote) {
        world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
      }
    }
  }

  @Override
  public BlockPos getAttachmentPos() {
    return pos;
  }

  /**
   * Recomputes everything derived from the span. Subclasses caching their own derived values
   * override this and call up first.
   */
  protected void onSpanChanged() {
    cachedCable = null;
    cachedTether = null;
    cachedNext = null;
    cachedRenderBounds = null;
    cachedAttachPoint = null;
    cachedFromT = 0.0;
    cachedToT = 0.0;

    if (span == null) {
      return;
    }
    cachedCable = span.solve();
    cachedTether = span.solveTether();
    cachedAttachPoint = span.attachPointOn(pos);
    cachedNext = span.nextAfter(pos);
    cachedFromT = cachedCable.parameterAt(pos.getX() + 0.5, pos.getZ() + 0.5);
    cachedToT = cachedNext == null
        ? cachedFromT
        : cachedCable.parameterAt(cachedNext.getX() + 0.5, cachedNext.getZ() + 0.5);
    cachedRenderBounds = computeRenderBounds();
  }

  /** The solved cable for the whole span this attachment belongs to, or null when unlinked. */
  @Nullable
  public SpanWireCatenary getCable() {
    return cachedCable;
  }

  /**
   * Anything beyond the span itself that changes what this attachment bakes into its display
   * list. Folded into the renderer's cache key, so a subclass with its own settings does not have
   * to remember to invalidate anything -- a different key is a different list.
   */
  public long getHardwareStateKey() {
    return 0L;
  }

  /** The solved lower tether of a box span, or null when this span has none. */
  @Nullable
  public SpanWireCatenary getTether() {
    return cachedTether;
  }

  /**
   * Where this attachment's hardware meets the cable, in world coordinates, including the span's
   * sideways offset. Falls back to the raw block centre when unlinked.
   */
  public Vec3d getAttachPoint() {
    return cachedAttachPoint != null
        ? cachedAttachPoint
        : SpanWireDefinition.attachPoint(pos);
  }

  /** The attachment this one draws cable to, or null if it draws none. */
  @Nullable
  public BlockPos getNextAttachment() {
    return cachedNext;
  }

  /** Where this attachment sits along the span, as a fraction from the start anchor. */
  public double getSpanParameter() {
    return cachedFromT;
  }

  /** Where the next attachment sits along the span. */
  public double getNextSpanParameter() {
    return cachedToT;
  }

  /**
   * Stays in view as long as the signals on it do, and then for as far as its own cable reaches.
   *
   * <p>Two halves. The first is the shared long-range distance every signal head uses; without
   * it this fell through to vanilla's 64 blocks, and a span vanished with its heads still drawn
   * for the next 64. The second is the distance from this block to the far corner of what it
   * draws. Vanilla measures the cut-off to the tile entity's own block, and an anchor draws the
   * whole run out to the first mount -- twenty blocks or more on a wide intersection -- so an
   * anchor just past the limit would drop a segment whose far end, and the head hanging there,
   * were well inside it. Adding the reach means any part of the cable a signal could be drawn
   * beside is drawn too.
   */
  @Override
  public double getMaxRenderDistanceSquared() {
    final AxisAlignedBB bounds = getRenderBoundingBox();
    final double cx = pos.getX() + 0.5;
    final double cy = pos.getY() + 0.5;
    final double cz = pos.getZ() + 0.5;
    final double dx = Math.max(cx - bounds.minX, bounds.maxX - cx);
    final double dy = Math.max(cy - bounds.minY, bounds.maxY - cy);
    final double dz = Math.max(cz - bounds.minZ, bounds.maxZ - cz);
    final double reach = Math.sqrt(dx * dx + dy * dy + dz * dz);
    final double distance = LONG_RANGE_RENDER_DISTANCE + reach;
    return distance * distance;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    if (cachedRenderBounds != null) {
      return cachedRenderBounds;
    }
    // Never fall through to TileEntity's default, which is INFINITE_EXTENT_AABB and disables
    // frustum culling for this tile entity entirely.
    return new AxisAlignedBB(pos);
  }

  /**
   * The box covering this attachment's own block plus the piece of cable it draws.
   *
   * <p>Extended downward by the whole span's sag rather than by this piece's own dip. That is
   * generous by up to a block on a long span, and deliberately so: it costs nothing and it means
   * the cable cannot be culled away while part of it is still on screen, which is the failure
   * this box exists to prevent.
   */
  @Nullable
  private AxisAlignedBB computeRenderBounds() {
    if (span == null || cachedCable == null) {
      return null;
    }
    AxisAlignedBB box;
    if (cachedNext == null) {
      // The far anchor draws no cable, only its own hardware.
      box = new AxisAlignedBB(pos);
    } else {
      final double sag = cachedCable.sag();
      box = new AxisAlignedBB(
          Math.min(pos.getX(), cachedNext.getX()),
          Math.min(pos.getY(), cachedNext.getY()) - sag - 1.0,
          Math.min(pos.getZ(), cachedNext.getZ()),
          Math.max(pos.getX(), cachedNext.getX()) + 1.0,
          Math.max(pos.getY(), cachedNext.getY()) + 1.0,
          Math.max(pos.getZ(), cachedNext.getZ()) + 1.0);
    }

    // A box span's tether hangs below everything else, and once it is dead-ended on placed
    // anchors it sits at a height this attachment cannot work out from its own position -- so the
    // wire's own points are taken rather than a drop being assumed. Only the stretch this
    // attachment actually draws is unioned in, which is what keeps the boxes small enough for
    // culling to still mean something (the plan's D6).
    if (cachedTether != null) {
      final Vec3d from = cachedTether.pointAt(cachedFromT);
      final Vec3d to = cachedTether.pointAt(cachedToT);
      box = box.union(new AxisAlignedBB(from.x, from.y, from.z, to.x, to.y, to.z).grow(1.0));
    }
    return box;
  }

  /** Releases this position's cached cable geometry when the block goes away. */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      SpanWireCableRenderer.cleanupDisplayList(pos);
    }
  }

  /** The common case: a player walking away, rather than breaking anything. */
  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      SpanWireCableRenderer.cleanupDisplayList(pos);
    }
  }
}
