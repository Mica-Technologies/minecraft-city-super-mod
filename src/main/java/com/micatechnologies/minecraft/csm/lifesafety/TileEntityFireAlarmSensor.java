package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TileEntityFireAlarmSensor extends AbstractTileEntity {

  private static final String linkedPanelPosXKey = "lpX";
  private static final String linkedPanelPosYKey = "lpY";
  private static final String linkedPanelPosZKey = "lpZ";
  private int linkedPanelX;
  private int linkedPanelY = -500;
  private int linkedPanelZ;

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(linkedPanelPosXKey) &&
        compound.hasKey(linkedPanelPosYKey) &&
        compound.hasKey(linkedPanelPosZKey)) {
      linkedPanelX = compound.getInteger(linkedPanelPosXKey);
      linkedPanelY = compound.getInteger(linkedPanelPosYKey);
      linkedPanelZ = compound.getInteger(linkedPanelPosZKey);
    } else {
      linkedPanelY = -500;
    }
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(linkedPanelPosXKey, linkedPanelX);
    compound.setInteger(linkedPanelPosYKey, linkedPanelY);
    compound.setInteger(linkedPanelPosZKey, linkedPanelZ);

    return compound;
  }

  public BlockPos getLinkedPanelPos(World world) {
    return new BlockPos(linkedPanelX, linkedPanelY, linkedPanelZ);
  }

  public boolean setLinkedPanelPos(BlockPos blockPos, EntityPlayer player) {
    if (linkedPanelY == -500) {
      linkedPanelX = blockPos.getX();
      linkedPanelY = blockPos.getY();
      linkedPanelZ = blockPos.getZ();
      markDirty();
      return true;
    } else {
      return false;
    }
  }
}
