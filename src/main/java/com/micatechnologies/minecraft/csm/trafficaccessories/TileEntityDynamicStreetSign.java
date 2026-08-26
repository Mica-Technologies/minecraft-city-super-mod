package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Holds one street blade's configuration as a JSON string in NBT, plus the synced redstone
 * power state its illumination can follow. Deserialization is lazy and cached, because the
 * renderer asks for the data every frame.
 */
public class TileEntityDynamicStreetSign extends AbstractTileEntity {

  private static final String NBT_KEY = "signData";
  private static final String NBT_KEY_POWERED = "lightPowered";

  private String signDataJson = "";
  // Redstone power at this block, kept server-authoritative and synced so the client-side
  // renderer can light the blade in SignLightMode.REDSTONE without polling neighbors every
  // frame. Only meaningful for lighting; the blade has no other redstone behavior.
  private boolean powered = false;
  private transient StreetSignData cachedData = null;

  public TileEntityDynamicStreetSign() {
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    signDataJson = compound.getString(NBT_KEY);
    powered = compound.getBoolean(NBT_KEY_POWERED);
    cachedData = null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(NBT_KEY, signDataJson);
    compound.setBoolean(NBT_KEY_POWERED, powered);
    return compound;
  }

  public StreetSignData getSignData() {
    if (cachedData == null) {
      cachedData = StreetSignData.fromJson(signDataJson);
    }
    return cachedData;
  }

  public void setSignData(StreetSignData data) {
    setSignDataJson(data == null ? new StreetSignData().toJson() : data.toJson());
  }

  public void setSignDataJson(String json) {
    this.signDataJson = json != null ? json : "";
    this.cachedData = null;
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
   * Records the redstone power state and, when it actually changed, pushes it to clients so
   * redstone-controlled illumination updates without a block update.
   */
  public void setPowered(boolean powered) {
    if (this.powered == powered) {
      return;
    }
    this.powered = powered;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // A blade may be forced out to 20 blocks wide and 4 tall (StreetSignData's min-size
    // ceilings), centered on the block, and a hanging one carries its hangers above. The box
    // must cover that or a wide blade culls the moment its own block leaves the frustum.
    return new AxisAlignedBB(
        pos.getX() - 10, pos.getY() - 3, pos.getZ() - 10,
        pos.getX() + 11, pos.getY() + 4, pos.getZ() + 11);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    // Blades carry small legend that is unreadable much past this; the renderer drops to a
    // body-only LOD at 48 blocks and stops entirely at 96.
    return 96.0 * 96.0;
  }
}
