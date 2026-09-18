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
 * A steel trench plate, the road plate laid over an open trench so traffic can drive across it.
 *
 * <p>A real plate is several blocks across, so plates laid side by side draw as one: the raised
 * edge bar runs only along a side with no plate beside it, and the plates between have no seam.
 * The sides are actual state; nothing is stored. The models come from
 * {@code dev-env-utils/scripts/gen_earthworks.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockTrenchPlate extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  /** The plate and its edge bar, which stands a quarter pixel above it. */
  private static final AxisAlignedBB BOX = BlockSiteProp.box16(0, 0, 0, 16, 1.25, 16);

  /**
   * Constructs a {@link BlockTrenchPlate}.
   *
   * @since 1.0
   */
  public BlockTrenchPlate() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 3F, 12F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "trench_plate";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
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

  private boolean plate(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() == this;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(NORTH, plate(worldIn, pos.north()))
        .withProperty(EAST, plate(worldIn, pos.east()))
        .withProperty(SOUTH, plate(worldIn, pos.south()))
        .withProperty(WEST, plate(worldIn, pos.west()));
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
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}
