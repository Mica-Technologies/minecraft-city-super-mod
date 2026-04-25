package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityDynamicGuideSign extends AbstractTileEntity {

  private static final String NBT_KEY = "signData";

  private String signDataJson = "";
  private transient GuideSignData cachedData = null;
  private transient boolean stateDirty = true;

  public TileEntityDynamicGuideSign() {
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    signDataJson = compound.getString(NBT_KEY);
    cachedData = null;
    stateDirty = true;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(NBT_KEY, signDataJson);
    return compound;
  }

  public GuideSignData getSignData() {
    if (cachedData == null) {
      cachedData = GuideSignData.fromJson(signDataJson);
    }
    return cachedData;
  }

  public void setSignData(GuideSignData data) {
    if (data == null) {
      data = new GuideSignData();
    }
    this.signDataJson = data.toJson();
    this.cachedData = null;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public void setSignDataJson(String json) {
    this.signDataJson = json != null ? json : "";
    this.cachedData = null;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public String getSignDataJson() {
    return signDataJson;
  }

  public boolean isStateDirty() {
    return stateDirty;
  }

  public void clearStateDirty() {
    stateDirty = false;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 8, pos.getY() - 4, pos.getZ() - 8,
        pos.getX() + 9, pos.getY() + 5, pos.getZ() + 9);
  }
}
