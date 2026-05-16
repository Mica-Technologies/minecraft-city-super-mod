package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tile entity for {@link BlockPoleMountSpeedLimitSign}. Inherits the same data shape
 * as {@link TileEntityOverheadSpeedLimit} (speedValue + housingColor + fullScreen) —
 * the trailer-color / sign-angle / flasher-mode fields from the further base class
 * are unused for pole-mount renderings. Kept as a distinct subclass solely so the
 * client proxy can bind a different TESR to it.
 */
public class TileEntityPoleMountSpeedLimit extends TileEntityOverheadSpeedLimit {

  public TileEntityPoleMountSpeedLimit() {
    super();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // Tighter than the overhead's render bbox since the panel and bracket fit inside
    // (and only barely extend past) the placed cell.
    return new AxisAlignedBB(
        pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
  }
}
