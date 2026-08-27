package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
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
  // Set whenever anything the renderer bakes into a display list changes, and cleared by the
  // renderer once it has recompiled. Light is NOT covered by this -- it changes without the tile
  // entity being touched, so the renderer keys its caches on the block light separately.
  private transient boolean stateDirty = true;

  public TileEntityDynamicStreetSign() {
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    StreetSignMount previousMount = signDataJson.isEmpty() ? null : getSignData().getMountType();
    signDataJson = compound.getString(NBT_KEY);
    powered = compound.getBoolean(NBT_KEY_POWERED);
    cachedData = null;
    stateDirty = true;
    // This is also the client's receive path for a sync, so it is where a mount change made on
    // the server has to reach the neighbouring poles.
    if (previousMount != null) {
      refreshNeighborsOnMountChange(previousMount);
    }
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
    StreetSignMount previousMount = getSignData().getMountType();
    this.signDataJson = json != null ? json : "";
    this.cachedData = null;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
      refreshNeighborsOnMountChange(previousMount);
    }
  }

  /**
   * Tells the neighbours when the mount changes, because a traffic pole decides whether to
   * sprout a mount stub toward this block from the mount type -- a hanging blade already has
   * its own hangers and is ignored, a flat one is bolted to something and is not.
   *
   * <p>Neither half of that is covered by the ordinary sync. {@code markDirtySync} explicitly
   * does not re-render, and would only cover this block if it did; the pole's decision is
   * evaluated in ITS {@code getActualState} during a chunk rebuild. Without this an adjacent
   * pole keeps the stub it had until something else happens to update it.
   *
   * <p>It fires only on an actual mount change: nothing else on this sign is any of a
   * neighbour's business, and a re-render per keystroke in the editing GUI would be waste.
   */
  private void refreshNeighborsOnMountChange(StreetSignMount previousMount) {
    if (getWorld() == null || getSignData().getMountType() == previousMount) {
      return;
    }
    if (getWorld().isRemote) {
      getWorld().markBlockRangeForRenderUpdate(getPos().add(-1, -1, -1), getPos().add(1, 1, 1));
    } else {
      getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType(), false);
    }
  }

  /**
   * Whether anything the renderer caches has changed since it last rebuilt.
   *
   * @return true if the renderer must discard what it compiled for this blade
   */
  public boolean isStateDirty() {
    return stateDirty;
  }

  /** Clears the dirty flag once the renderer has rebuilt from current data. */
  public void clearStateDirty() {
    stateDirty = false;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    releaseRenderCache();
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    releaseRenderCache();
  }

  /**
   * Hands the blade's compiled geometry back on the client. Without this the display lists for a
   * removed or unloaded blade stay allocated until the cache evicts them.
   */
  private void releaseRenderCache() {
    if (getWorld() != null && getWorld().isRemote) {
      TileEntityDynamicStreetSignRenderer.cleanupDisplayList(getPos());
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
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // A blade may be forced out to 20 blocks wide and 4 tall (StreetSignData's min-size
    // ceilings), centered on the block, and a hanging one carries its hangers half a block
    // above. The box must cover that or a wide blade culls the moment its own block leaves
    // the frustum.
    return new AxisAlignedBB(
        pos.getX() - 10, pos.getY() - 3, pos.getZ() - 10,
        pos.getX() + 11, pos.getY() + 4, pos.getZ() + 11);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    // Matched to the guide sign (and the traffic signal heads) at 128 blocks. Blades stopped
    // at 96 while every other dynamic sign around them kept drawing, which reads as the blade
    // popping out of a junction rather than as distance. Full detail holds to 64 blocks.
    return 128.0 * 128.0;
  }
}
