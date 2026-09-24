package com.micatechnologies.minecraft.csm.furniture.residential;

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
import net.minecraft.world.World;

/**
 * An area rug, a sixteenth thick on the floor, that joins on all four sides into a rug of any
 * size, as the dining table does: {@link #NORTH}, {@link #EAST}, {@link #SOUTH} and
 * {@link #WEST} say whether the same rug continues on that side, in world directions, and the
 * multipart blockstate draws its bound border only on an open side, and a corner of it only
 * where a side beside the corner is open. So blocks laid 3 x 4 are one rug with one border
 * round it. The field's pattern tiles across blocks, so the rug does not show where one block
 * ends. Nothing is stored in metadata.
 *
 * @since 2026.9
 */
public class BlockRug extends AbstractBlock {

  /** The same rug continues to the north. */
  public static final PropertyBool NORTH = PropertyBool.create("north");
  /** The same rug continues to the east. */
  public static final PropertyBool EAST = PropertyBool.create("east");
  /** The same rug continues to the south. */
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  /** The same rug continues to the west. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a rug.
   *
   * @param registryName its registry name
   */
  public BlockRug(String registryName) {
    super(stash(registryName), SoundType.CLOTH, "axe", 0, 0.2F, 0.4F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.CARPET;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
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

  /** A rug lies on something: it cannot be laid on air. */
  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(world, pos) && !world.isAirBlock(pos.down());
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
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
