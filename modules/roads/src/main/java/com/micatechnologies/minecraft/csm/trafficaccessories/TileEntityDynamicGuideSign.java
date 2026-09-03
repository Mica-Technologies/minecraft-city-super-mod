package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityDynamicGuideSign extends AbstractTileEntity {

  private static final String NBT_KEY = "signData";
  private static final String NBT_KEY_POWERED = "lightPowered";

  private String signDataJson = "";
  // Redstone power at this block, kept server-authoritative and synced so the client-side
  // renderer can light the sign in SignLightMode.REDSTONE without polling neighbors every
  // frame. Only meaningful for lighting; the sign has no other redstone behavior.
  private boolean powered = false;
  private transient GuideSignData cachedData = null;
  private transient boolean stateDirty = true;

  public TileEntityDynamicGuideSign() {
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    signDataJson = compound.getString(NBT_KEY);
    powered = compound.getBoolean(NBT_KEY_POWERED);
    cachedData = null;
    stateDirty = true;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(NBT_KEY, signDataJson);
    compound.setBoolean(NBT_KEY_POWERED, powered);
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

  public boolean isPowered() {
    return powered;
  }

  /**
   * Records the redstone power state and, when it actually changed, pushes it to clients
   * so redstone-controlled sign lighting updates without a block update.
   */
  public void setPowered(boolean powered) {
    if (this.powered == powered) {
      return;
    }
    this.powered = powered;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public boolean isStateDirty() {
    return stateDirty;
  }

  public void clearStateDirty() {
    stateDirty = false;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityDynamicGuideSignRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityDynamicGuideSignRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // Signs may reach 30 blocks wide and 15 tall (centered on the block), plus posts
    // running 3 blocks below; the box must cover that or big signs cull when the
    // block itself leaves the frustum.
    return new AxisAlignedBB(
        pos.getX() - 15, pos.getY() - 11, pos.getZ() - 15,
        pos.getX() + 16, pos.getY() + 9, pos.getZ() + 16);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    // Matches the traffic signal heads' 128 blocks. The renderer draws full detail
    // (text, shields, arrows) only inside 64 blocks; between 64 and 128 it draws a
    // cheap LOD of just the sign body, back, and posts.
    return LONG_RANGE_RENDER_DISTANCE_SQUARED;
  }
}
