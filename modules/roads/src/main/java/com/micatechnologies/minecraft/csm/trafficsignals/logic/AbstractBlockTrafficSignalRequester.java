package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalLinkTool;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalRequester;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalTickableRequester;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractBlockTrafficSignalRequester
    extends AbstractBlockControllableCrosswalkAccessory
    implements ICsmTileEntityProvider {

  private static final Logger LOGGER =
      LogManager.getLogger(AbstractBlockTrafficSignalRequester.class);

  public AbstractBlockTrafficSignalRequester(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  public static void resetRequestCount(World world, BlockPos blockPos) {
    try {
      TileEntity rawTileEntity = world.getTileEntity(blockPos);
      if (rawTileEntity instanceof TileEntityTrafficSignalTickableRequester) {
        TileEntityTrafficSignalTickableRequester tileEntity
            = (TileEntityTrafficSignalTickableRequester) rawTileEntity;
        tileEntity.resetRequestCount();
      } else {
        LOGGER.error(
            "Unable to reset the traffic signal's request count due to tile entity missing error!");
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while resetting the traffic signal's request count!", e);
    }
  }

  @Override
  public boolean onBlockActivated(World p_180639_1_,
      BlockPos p_180639_2_,
      IBlockState p_180639_3_,
      EntityPlayer p_180639_4_,
      EnumHand p_180639_5_,
      EnumFacing p_180639_6_,
      float p_180639_7_,
      float p_180639_8_,
      float p_180639_9_) {
    if (p_180639_4_.inventory.getCurrentItem() != null &&
        p_180639_4_.inventory.getCurrentItem().getItem() instanceof ItemSignalLinkTool) {
      return super.onBlockActivated(p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_,
          p_180639_6_,
          p_180639_7_, p_180639_8_, p_180639_9_);
    }

    try {
      TileEntity rawTileEntity = p_180639_1_.getTileEntity(p_180639_2_);
      if (rawTileEntity instanceof TileEntityTrafficSignalTickableRequester) {
        TileEntityTrafficSignalTickableRequester tileEntity
            = (TileEntityTrafficSignalTickableRequester) rawTileEntity;
        tileEntity.incrementRequestCount();
      } else {
        LOGGER.error(
            "Unable to send a traffic signal request due to tile entity missing error!");
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while activating a traffic signal request!", e);
    }

    return true;
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
    return TileEntityTrafficSignalRequester.class;
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
    return "tileentitytrafficsignalrequester";
  }
}
