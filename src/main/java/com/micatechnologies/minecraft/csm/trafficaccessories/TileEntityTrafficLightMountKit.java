package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Minimal marker tile entity for the dynamic signal mount kit. Stores no data — exists solely
 * to allow a TESR to be bound to the block. Overrides the render bounding box so the TESR
 * is not prematurely frustum-culled when the bracket extends well beyond the block position
 * (e.g., downward for add-on signals).
 */
public class TileEntityTrafficLightMountKit extends AbstractTileEntity {

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
