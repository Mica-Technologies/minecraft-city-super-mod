package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityOverheadMessageSign extends TileEntityPortableMessageSign {

  public TileEntityOverheadMessageSign() {
    super();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 4, pos.getY() - 4, pos.getZ() - 4,
        pos.getX() + 5, pos.getY() + 1, pos.getZ() + 5);
  }
}
