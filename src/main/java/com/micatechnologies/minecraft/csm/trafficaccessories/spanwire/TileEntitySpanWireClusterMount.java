package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.List;
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

  private int clusterWidth = MIN_WIDTH;

  /**
   * The block columns this cluster's bracket reaches over, its own included. Derived from the
   * width and the cable's direction whenever either changes, and held so the signals underneath
   * can ask a cheap question instead of repeating the geometry.
   */
  private final List<BlockPos> coveredColumns = new ArrayList<>();

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
  public long getHardwareStateKey() {
    return super.getHardwareStateKey() * 31L + clusterWidth;
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
  }

  private static int clampWidth(int width) {
    return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
  }
}
