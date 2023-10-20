package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockForgeEnergyToRedstone extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyBool POWERED = PropertyBool.create("powered");

  public BlockForgeEnergyToRedstone() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(POWERED, meta == 1);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(POWERED) ? 1 : 0;
  }

  @Override
  @ParametersAreNonnullByDefault
  public int getWeakPower(IBlockState p_getWeakPower_1_,
      IBlockAccess p_getWeakPower_2_,
      BlockPos p_getWeakPower_3_,
      EnumFacing p_getWeakPower_4_) {
    return p_getWeakPower_1_.getValue(POWERED) ? 15 : 0;
  }

  @Override
  @ParametersAreNonnullByDefault
  public boolean canProvidePower(IBlockState p_canProvidePower_1_) {
    return true;
  }

  @Override
  @ParametersAreNonnullByDefault
  public int getStrongPower(IBlockState p_getStrongPower_1_,
      IBlockAccess p_getStrongPower_2_,
      BlockPos p_getStrongPower_3_,
      EnumFacing p_getStrongPower_4_) {
    return p_getStrongPower_1_.getValue(POWERED) ? 15 : 0;
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, POWERED);
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

  @Override
  @ParametersAreNonnullByDefault
  public boolean isOpaqueCube(IBlockState state) {
    return false;
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
    return "rftors";
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
    return new TileEntityForgeEnergyConsumer();
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
    return TileEntityForgeEnergyConsumer.class;
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
    return "tileentityforgeenergyconsumer";
  }
}

