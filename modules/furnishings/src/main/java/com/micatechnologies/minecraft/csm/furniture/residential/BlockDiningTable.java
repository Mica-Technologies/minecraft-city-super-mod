package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
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
 * A dining table: one block of table, 0.75 m high, that joins on all four sides into a table of
 * any rectangle. {@link #NORTH}, {@link #EAST}, {@link #SOUTH} and {@link #WEST} say whether the
 * same block is on that side, in world directions; the multipart blockstate draws the top
 * always, an apron only on an open side, and a leg only at a corner whose two sides are both
 * open, so blocks placed 2 x 3 are one table with four legs. A table does not face anywhere,
 * and nothing is stored in metadata.
 *
 * @since 2026.9
 */
public class BlockDiningTable extends AbstractBlock {

  /** The same table continues to the north. */
  public static final PropertyBool NORTH = PropertyBool.create("north");
  /** The same table continues to the east. */
  public static final PropertyBool EAST = PropertyBool.create("east");
  /** The same table continues to the south. */
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  /** The same table continues to the west. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  /** The table's box: the whole block, up to the top of the table. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 12 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a dining table.
   *
   * @param registryName its registry name
   */
  public BlockDiningTable(String registryName) {
    super(stash(registryName), SoundType.WOOD, "axe", 0, 1.5F, 3.0F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos, EnumFacing.NORTH))
        .withProperty(EAST, joins(world, pos, EnumFacing.EAST))
        .withProperty(SOUTH, joins(world, pos, EnumFacing.SOUTH))
        .withProperty(WEST, joins(world, pos, EnumFacing.WEST));
  }

  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    return world.getBlockState(pos.offset(side)).getBlock() == this;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
