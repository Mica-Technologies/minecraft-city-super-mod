package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nullable;
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
   * <p>May return {@code null} to indicate "no retirement right now" — useful for factory block
   * classes that implement this interface unconditionally but only retire for a subset of their
   * instances (the rest pass {@code null} in their constructor). When this returns {@code null},
   * the retirement randomTick handler skips replacement entirely.
   *
   * @return The replacement block ID, or {@code null} if this block instance should not retire.
   *
   * @since 1.0
   */
  @Nullable
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
