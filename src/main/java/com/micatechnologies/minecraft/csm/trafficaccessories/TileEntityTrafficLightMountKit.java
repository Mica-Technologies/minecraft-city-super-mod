package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Tile entity for the dynamic signal mount kit. Caches the computed bounding box so the block
 * doesn't recompute it every frame, and holds the player-selected color scheme so the TESR
 * can paint the bracket in one of several finishes. The BB cache is invalidated when a
 * neighbor changes (via {@link #invalidateCachedBB()}).
 */
public class TileEntityTrafficLightMountKit extends AbstractTileEntity {

  private static final String NBT_KEY_COLOR_SCHEME = "colorScheme";

  @Nullable
  private AxisAlignedBB cachedBoundingBox;

  private MountKitColorScheme colorScheme = MountKitColorScheme.DEFAULT;

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

  /** Returns the current color scheme. Never null. */
  public MountKitColorScheme getColorScheme() {
    return colorScheme;
  }

  /**
   * Advances to the next color scheme in declaration order (wraps at the end) and returns the
   * new scheme. Called when a player sneak+right-clicks the block.
   */
  public MountKitColorScheme cycleColorScheme() {
    this.colorScheme = colorScheme.next();
    return this.colorScheme;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    if (compound.hasKey(NBT_KEY_COLOR_SCHEME)) {
      this.colorScheme = MountKitColorScheme.fromOrdinal(compound.getInteger(NBT_KEY_COLOR_SCHEME));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_KEY_COLOR_SCHEME, colorScheme.ordinal());
    return super.writeNBT(compound);
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
