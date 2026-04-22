package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tile entity for fire alarm activator blocks (pull stations, detectors) that stores the
 * linked fire alarm control panel position for alarm activation.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class TileEntityFireAlarmSensor extends AbstractTileEntity {

  // Short-form key holds the linked panel position as a single 3-int IntArray {x, y, z}.
  // The legacy triple of per-axis ints is still accepted on read for backwards compat.
  private static final String linkedPanelKey = "lp";
  private static final String legacyLinkedPanelPosXKey = "lpX";
  private static final String legacyLinkedPanelPosYKey = "lpY";
  private static final String legacyLinkedPanelPosZKey = "lpZ";
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
    boolean loaded = false;
    if (compound.hasKey(linkedPanelKey)) {
      int[] pos = compound.getIntArray(linkedPanelKey);
      if (pos.length == 3) {
        linkedPanelX = pos[0];
        linkedPanelY = pos[1];
        linkedPanelZ = pos[2];
        loaded = true;
      }
    } else if (compound.hasKey(legacyLinkedPanelPosXKey) &&
        compound.hasKey(legacyLinkedPanelPosYKey) &&
        compound.hasKey(legacyLinkedPanelPosZKey)) {
      linkedPanelX = compound.getInteger(legacyLinkedPanelPosXKey);
      linkedPanelY = compound.getInteger(legacyLinkedPanelPosYKey);
      linkedPanelZ = compound.getInteger(legacyLinkedPanelPosZKey);
      loaded = true;
    }
    if (!loaded) {
      linkedPanelY = -500;
    }

    // Strip legacy long-form keys so the next save produces only short-form output
    compound.removeTag(legacyLinkedPanelPosXKey);
    compound.removeTag(legacyLinkedPanelPosYKey);
    compound.removeTag(legacyLinkedPanelPosZKey);
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
    compound.setIntArray(linkedPanelKey,
        new int[] {linkedPanelX, linkedPanelY, linkedPanelZ});
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
