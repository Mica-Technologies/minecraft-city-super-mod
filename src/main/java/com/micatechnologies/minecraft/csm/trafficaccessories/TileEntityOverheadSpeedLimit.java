package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityOverheadSpeedLimit extends TileEntityVariableSpeedLimit {

  private boolean fullScreen = false;

  public TileEntityOverheadSpeedLimit() {
    super();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    fullScreen = compound.getBoolean("fScr");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setBoolean("fScr", fullScreen);
    return compound;
  }

  public boolean isFullScreen() {
    return fullScreen;
  }

  public void setFullScreen(boolean fullScreen) {
    this.fullScreen = fullScreen;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 2, pos.getY() - 3, pos.getZ() - 2,
        pos.getX() + 3, pos.getY() + 4, pos.getZ() + 3);
  }
}
