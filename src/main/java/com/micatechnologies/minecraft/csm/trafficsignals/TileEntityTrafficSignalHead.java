package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * The {@link TileEntityTrafficSignalHead} is a {@link TileEntity} which is paired with TBD.
 *
 * @version 1.0
 * @see TileEntity
 * @since 2024.1
 */
public class TileEntityTrafficSignalHead extends AbstractTileEntity {

  /**
   * Boolean value indicating whether the first indication is lit. From the perspective of the
   * player, this is the top indication.
   *
   * @since 1.0
   */
  private final boolean indication1Lit = false;

  /**
   * Boolean value indicating whether the second indication is lit. From the perspective of the
   * player, this is the middle indication.
   *
   * @since 1.0
   */
  private final boolean indication2Lit = false;

  /**
   * Boolean value indicating whether the third indication is lit. From the perspective of the
   * player, this is the bottom indication.
   *
   * @since 1.0
   */
  private final boolean indication3Lit = false;

  /**
   * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalHead}'s NBT
   * data.
   *
   * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalHead}'s NBT
   *                 data to
   * @return the NBT tag compound with the {@link TileEntityTrafficSignalHead}'s NBT data
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {

    // Write TBD to NBT

    // Return the NBT tag compound
    return compound;
  }

  /**
   * Processes the reading of the {@link TileEntityTrafficSignalHead}'s NBT data from the supplied
   * NBT tag compound.
   *
   * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalHead}'s NBT data
   *                 from
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {

    // Read TBD from NBT
  }
}
