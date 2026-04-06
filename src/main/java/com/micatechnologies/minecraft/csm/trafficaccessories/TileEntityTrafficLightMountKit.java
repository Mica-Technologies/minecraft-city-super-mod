package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Tile entity for the dynamic signal mount kit. Stores no persistent data — exists to
 * allow a TESR to be bound and to cache the computed bounding box for the block.
 * The BB cache is invalidated when a neighbor changes (via {@link #invalidateCachedBB()}).
 */
public class TileEntityTrafficLightMountKit extends AbstractTileEntity {

  @Nullable
  private AxisAlignedBB cachedBoundingBox;

  /**
   * Returns the cached bounding box, or null if it needs to be recomputed.
   */
  @Nullable
  public AxisAlignedBB getCachedBoundingBox() {
    return cachedBoundingBox;
  }

  /**
   * Stores a computed bounding box for reuse until invalidated.
   */
  public void setCachedBoundingBox(AxisAlignedBB bb) {
    this.cachedBoundingBox = bb;
  }

  /**
   * Clears the cached bounding box so it will be recomputed on next access.
   * Called from {@link BlockTrafficLightMountKit#neighborChanged} when adjacent blocks change.
   */
  public void invalidateCachedBB() {
    this.cachedBoundingBox = null;
  }

  /**
   * Returns an expanded render bounding box so Minecraft's frustum culling doesn't hide
   * the bracket when only the extended portion (below/above/beside the block) is on screen.
   * Covers up to 4 blocks in each direction from the block center, which is enough for
   * the maximum scan distance (3 blocks) plus knuckle/collar overshoot.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 4.0, pos.getY() - 4.0, pos.getZ() - 4.0,
        pos.getX() + 5.0, pos.getY() + 5.0, pos.getZ() + 5.0);
  }
}
