package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.ItemEWSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalConfigurationTool;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPSPolara;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlockTrafficSignalAPSPolara extends
    AbstractBlockTrafficSignalAPS {

  public AbstractBlockTrafficSignalAPSPolara(Material p_i45394_1_) {
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
    return TileEntityTrafficSignalAPSPolara.class;
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
    return "tileentitytrafficsignalapspolara";
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
    return new TileEntityTrafficSignalAPSPolara();
  }
}
