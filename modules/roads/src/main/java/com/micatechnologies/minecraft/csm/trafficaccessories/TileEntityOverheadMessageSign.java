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
    // The housing is 9.25 blocks wide along the sign's own X (144 px plus a 2 px frame each
    // side), centred on the block, so it reaches 4.625 blocks either side of the block centre;
    // the renderer turns that axis onto Z when the sign faces east or west. The box is square
    // about the centre, 5.5 blocks each way, so it covers the housing in every facing.
    return new AxisAlignedBB(
        pos.getX() - 5, pos.getY() - 2, pos.getZ() - 5,
        pos.getX() + 6, pos.getY() + 3, pos.getZ() + 6);
  }
}
