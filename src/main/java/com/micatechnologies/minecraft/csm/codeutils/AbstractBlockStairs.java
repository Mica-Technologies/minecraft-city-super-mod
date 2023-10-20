package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Abstract stair block class which provides common methods and properties for all blocks in this
 * mod.
 *
 * @version 1.0
 * @see Block
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlockStairs extends BlockStairs implements IHasModel, ICsmBlock {

  /**
   * Constructs an {@link AbstractBlockStairs} instance.
   *
   * @param modelBlock       The model block of the stair block.
   * @param soundType        The sound type of the stair block.
   * @param harvestToolClass The harvest tool class of the stair block.
   * @param harvestLevel     The harvest level of the stair block.
   * @param hardness         The stair block's hardness.
   * @param resistance       The stair block's resistance to explosions.
   * @param lightLevel       The stair block's light level.
   * @param lightOpacity     The stair block's light opacity.
   *
   * @since 1.0
   */
  public AbstractBlockStairs(Block modelBlock,
      SoundType soundType,
      String harvestToolClass,
      int harvestLevel,
      float hardness,
      float resistance,
      float lightLevel,
      int lightOpacity) {
    super(modelBlock.getDefaultState());
    AbstractBlockStairs.this.setUnlocalizedName(AbstractBlockStairs.this.getBlockRegistryName());
    AbstractBlockStairs.this.setRegistryName(CsmConstants.MOD_NAMESPACE,
        AbstractBlockStairs.this.getBlockRegistryName());
    AbstractBlockStairs.this.setSoundType(soundType);
    AbstractBlockStairs.this.setHarvestLevel(harvestToolClass, harvestLevel);
    AbstractBlockStairs.this.setHardness(hardness);
    AbstractBlockStairs.this.setResistance(resistance);
    AbstractBlockStairs.this.setLightLevel(lightLevel);
    AbstractBlockStairs.this.setLightOpacity(lightOpacity);
    CsmRegistry.registerBlock(AbstractBlockStairs.this);
    CsmRegistry.registerItem(new ItemBlock(AbstractBlockStairs.this).setRegistryName(
        Objects.requireNonNull(AbstractBlockStairs.this.getRegistryName())));
  }

  /**
   * Registers the stair block's model.
   *
   * @see IHasModel#registerModels()
   * @since 1.0
   */
  @Override
  public void registerModels() {
    Csm.proxy.setCustomModelResourceLocation(Item.getItemFromBlock(AbstractBlockStairs.this), 0,
        "inventory");
  }

  /**
   * Implementation of the
   * {@link ICsmBlock#getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)} method which returns
   * {@code null}, as this class uses the standard/default {@link BlockStairs} bounding boxes. This
   * method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)} method in
   * subclasses.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return {@code null}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return null;
  }

  /**
   * Implementation of the {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method which returns
   * {@code false}, as this class uses the standard/default {@link BlockSlab} full cube value. This
   * method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method in subclasses.
   *
   * @param state the {@link IBlockState} to get the full cube value of
   *
   * @return {@code false}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Implementation of the
   * {@link ICsmBlock#getBlockConnectsRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)}
   * method which returns {@code false}, as this class uses the standard/default {@link BlockStairs}
   * redstone connection value. This method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockConnectsRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)}
   * method in subclasses.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code false}
   *
   * @implNote The value returned by this method implementation is ignored.
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
   * Implementation of the {@link ICsmBlock#getBlockRenderLayer()} method which returns
   * {@code null}, as this class uses the standard/default {@link BlockSlab} render layer value.
   * This method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockRenderLayer()} method in subclasses.
   *
   * @return {@code null}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return null;
  }
}
