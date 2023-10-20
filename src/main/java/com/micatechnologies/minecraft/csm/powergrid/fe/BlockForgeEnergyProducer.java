package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockForgeEnergyProducer extends AbstractBlock implements ICsmTileEntityProvider {

  public BlockForgeEnergyProducer() {
    super(Material.ANVIL, SoundType.ANVIL, "pickaxe", 1, 2F, 10F, 0F, 0);
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
    // Increment tick rate if tile entity is present and valid
    if (p_onBlockActivated_4_.isSneaking()) {
      TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity(p_onBlockActivated_2_);
      if (tileEntity instanceof TileEntityForgeEnergyProducer) {
        TileEntityForgeEnergyProducer tileEntityForgeEnergyProducer
            = (TileEntityForgeEnergyProducer) tileEntity;
        int tickRate = tileEntityForgeEnergyProducer.incrementTickRate();
        if (!p_onBlockActivated_1_.isRemote) {
          double tickRateSeconds = (double) tickRate / 20.0;
          p_onBlockActivated_4_.sendMessage(new TextComponentString(
              "Producer infinite output rate set to " +
                  tickRate +
                  " ticks" +
                  " (" +
                  tickRateSeconds +
                  " seconds)."));
        }
      }

      return true;
    } else {
      return super.onBlockActivated(p_onBlockActivated_1_, p_onBlockActivated_2_,
          p_onBlockActivated_3_,
          p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
          p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_);
    }
  }

  @Override
  @ParametersAreNonnullByDefault
  public boolean canProvidePower(IBlockState p_canProvidePower_1_) {
    return true;
  }

  @Override
  @ParametersAreNonnullByDefault
  public void addInformation(ItemStack p_addInformation_1_,
      World p_addInformation_2_,
      List<String> p_addInformation_3_,
      ITooltipFlag p_addInformation_4_) {
    super.addInformation(p_addInformation_1_, p_addInformation_2_, p_addInformation_3_,
        p_addInformation_4_);
    p_addInformation_3_.add(I18n.format("csm.highvoltage"));
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
    return "rfprod";
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
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
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
    return new TileEntityForgeEnergyProducer();
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
    return TileEntityForgeEnergyProducer.class;
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
    return "tileentityforgeenergyproducer";
  }
}

