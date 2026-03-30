package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Mod block interface for blocks which are scheduled to be replaced.
 *
 * @version 1.0
 * @since 2024.8.7
 */
public interface ICsmRetiringBlock {

  /**
   * Retrieves the replacement block ID.
   *
   * @return The replacement block ID.
   *
   * @since 1.0
   */
  String getReplacementBlockId();

  /**
   * Configures the replacement block's tile entity after block replacement. Called by
   * {@link AbstractBlock#randomTick} after the retiring block has been replaced. The default
   * implementation transfers all custom TE data from the old block to the new one.
   *
   * @param world            the world
   * @param pos              the block position
   * @param oldTileEntityNBT the old tile entity's NBT data
   *
   * @since 2.0
   */
  default void configureReplacement(World world, BlockPos pos, NBTTagCompound oldTileEntityNBT) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof AbstractTileEntity) {
      ((AbstractTileEntity) te).readNBT(oldTileEntityNBT);
    }
  }
}
