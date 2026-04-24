package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityOverheadSpeedLimit extends TileEntityVariableSpeedLimit {

  public TileEntityOverheadSpeedLimit() {
    super();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 2, pos.getY() - 3, pos.getZ() - 2,
        pos.getX() + 3, pos.getY() + 4, pos.getZ() + 3);
  }
}
