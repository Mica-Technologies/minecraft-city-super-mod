package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An adjustable steel post shore, the kind that holds a slab deck up while it cures.
 *
 * <p>A real shore is two or three blocks tall, so a stack of these is one shore and each block
 * works out its part from the stack: the outer tube the full height where the stack carries on,
 * the adjusting collar, inner tube and U-head where it ends, and a base plate at its foot. Nothing
 * is stored. The models come from {@code dev-env-utils/scripts/gen_formwork.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockPostShore extends AbstractBlock {

  /** Whether another shore is above or below. Actual state only. */
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The tube, and a little over for the collar and head. */
  private static final AxisAlignedBB BOX = BlockSiteProp.box16(6, 0, 6, 10, 16, 10);

  /**
   * Constructs a {@link BlockPostShore}.
   *
   * @since 1.0
   */
  public BlockPostShore() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1.5F, 6F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "shore_post";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, UP, DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state
        .withProperty(UP, worldIn.getBlockState(pos.up()).getBlock() == this)
        .withProperty(DOWN, worldIn.getBlockState(pos.down()).getBlock() == this);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
