package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPSCampbell;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPSPolara;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class AbstractBlockTrafficSignalAPSCampbell extends
    AbstractBlockTrafficSignalAPS {

  public AbstractBlockTrafficSignalAPSCampbell(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficSignalAPSCampbell.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  @Override
  public String getTileEntityName() {
    return "tileentitytrafficsignalapscampbell";
  }

  /**
   * Gets a new tile entity for the block.
   *
   * @param worldIn the world
   * @param meta    the block metadata
   *
   * @return the new tile entity for the block
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalAPSCampbell();
  }
}
