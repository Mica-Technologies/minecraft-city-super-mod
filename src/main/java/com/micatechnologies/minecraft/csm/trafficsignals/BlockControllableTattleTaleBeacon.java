package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;

/**
 * Tattle-tale beacon block for traffic signal confirmation. Provides a small indicator light that
 * mirrors the state of a linked traffic signal phase, allowing officers to verify signal status.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableTattleTaleBeacon extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  public BlockControllableTattleTaleBeacon() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    // Get tile entity and cycle mode
    SIGNAL_SIDE side = SIGNAL_SIDE.THROUGH;
    if (world != null && blockPos != null) {
      TileEntity tileEntity = world.getTileEntity(blockPos);
      if (tileEntity instanceof TileEntityTattleTaleBeacon) {
        TileEntityTattleTaleBeacon tileEntityTattleTaleBeacon =
            (TileEntityTattleTaleBeacon) tileEntity;
        side = tileEntityTattleTaleBeacon.getSignalSide();
      }
    }

    return side;
  }

  @Override
  public boolean doesFlash() {
    return false;
  }

  @Override
  public boolean onBlockActivated(World p_onBlockActivated_1_,
      BlockPos p_onBlockActivated_2_,
      IBlockState p_onBlockActivated_3_,
      EntityPlayer p_onBlockActivated_4_,
      EnumHand p_onBlockActivated_5_,
      EnumFacing p_onBlockActivated_6_,
      float p_onBlockActivated_7_,
      float p_onBlockActivated_8_,
      float p_onBlockActivated_9_) {
    if (!(p_onBlockActivated_4_.inventory.getCurrentItem().getItem() instanceof ItemSensorZoneTool)
        &&
        !(p_onBlockActivated_4_.inventory.getCurrentItem()
            .getItem() instanceof ItemSignalLinkTool)) {
      // Get tile entity and cycle mode
      TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity(p_onBlockActivated_2_);
      if (tileEntity instanceof TileEntityTattleTaleBeacon) {
        TileEntityTattleTaleBeacon tileEntityTattleTaleBeacon =
            (TileEntityTattleTaleBeacon) tileEntity;
        tileEntityTattleTaleBeacon.cycleMode(p_onBlockActivated_4_);
      }
    } else {
      return super.onBlockActivated(p_onBlockActivated_1_, p_onBlockActivated_2_,
          p_onBlockActivated_3_,
          p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
          p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_);
    }

    return true;
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllabletattletalebeacon";
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
    return new TileEntityTattleTaleBeacon();
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
    return TileEntityTattleTaleBeacon.class;
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
    return "tileentitytattletalebeacon";
  }


    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0.000000, -0.375000, 0.400000, 0.562500, 1.000000, 0.587500);
    }
}
