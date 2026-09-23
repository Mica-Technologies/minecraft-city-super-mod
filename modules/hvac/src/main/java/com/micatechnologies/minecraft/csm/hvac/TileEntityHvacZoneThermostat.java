package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * A zone thermostat: its own setpoints and its own vents, served by the equipment of the primary
 * thermostat it is linked to. The rooms its vents blow into are held at its setpoints; see
 * {@link HvacSystemControl}.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacZoneThermostat extends TileEntityHvacThermostatBase {

  static final String NBT_LINKED_PRIMARY = "lP";
  static final String LEGACY_NBT_LINKED_PRIMARY = "linkedPrimary";
  static final String NBT_HAS_PRIMARY = "hP";
  static final String LEGACY_NBT_HAS_PRIMARY = "hasPrimary";

  /** Position of the linked primary thermostat, or null. */
  private BlockPos linkedPrimaryPos;

  @Override
  protected int anchorKind() {
    return ThermalAnchor.ZONE;
  }

  public BlockPos getLinkedPrimaryPos() {
    return linkedPrimaryPos;
  }

  public void setLinkedPrimaryPos(BlockPos primaryPos) {
    this.linkedPrimaryPos = primaryPos != null ? primaryPos.toImmutable() : null;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  public boolean hasLinkedPrimary() {
    return linkedPrimaryPos != null;
  }

  private TileEntityHvacThermostat getPrimaryThermostat() {
    if (linkedPrimaryPos == null || world == null || !world.isBlockLoaded(linkedPrimaryPos)) {
      return null;
    }
    TileEntity te = world.getTileEntity(linkedPrimaryPos);
    return te instanceof TileEntityHvacThermostat ? (TileEntityHvacThermostat) te : null;
  }

  /** Farthest a vent may be linked: the primary's reach. */
  @Override
  public int getMaxVentLinkDistance() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    return primary != null ? primary.getMaxVentLinkDistance() : 30;
  }

  /** Units in the primary's system, as last reported. */
  public int getLinkedUnitCount() {
    return totalUnits;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    linkedPrimaryPos = null;
    if (readBool(compound, NBT_HAS_PRIMARY, LEGACY_NBT_HAS_PRIMARY)) {
      String key = compound.hasKey(NBT_LINKED_PRIMARY) ? NBT_LINKED_PRIMARY
          : LEGACY_NBT_LINKED_PRIMARY;
      NBTTagCompound tag = compound.getCompoundTag(key);
      linkedPrimaryPos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"),
          tag.getInteger("z"));
    }
    compound.removeTag(LEGACY_NBT_LINKED_PRIMARY);
    compound.removeTag(LEGACY_NBT_HAS_PRIMARY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (linkedPrimaryPos != null) {
      compound.setBoolean(NBT_HAS_PRIMARY, true);
      NBTTagCompound tag = new NBTTagCompound();
      tag.setInteger("x", linkedPrimaryPos.getX());
      tag.setInteger("y", linkedPrimaryPos.getY());
      tag.setInteger("z", linkedPrimaryPos.getZ());
      compound.setTag(NBT_LINKED_PRIMARY, tag);
    } else {
      compound.setBoolean(NBT_HAS_PRIMARY, false);
    }
    return compound;
  }
}
