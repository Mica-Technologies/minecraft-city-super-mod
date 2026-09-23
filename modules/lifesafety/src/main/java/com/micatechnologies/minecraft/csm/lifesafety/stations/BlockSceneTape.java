package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Police or fire line tape, laid a block at a time like a fence: each block draws a strip of tape
 * towards every neighbour it joins -- more tape of either colour, a tape stanchion, or the solid
 * side of any block, which it is tied to. It does not collide (tape is stepped over or ducked
 * under), and the connections are actual state, so nothing is stored but the block.
 *
 * <p>A side toward a stanchion is {@link Link#POST}: the tape runs on past its own block to the
 * post in the middle of the next, since the stanchion draws no tape and an arm that stopped at
 * the block edge left half a block of nothing beside every post.</p>
 *
 * @since 2026.9
 */
public class BlockSceneTape extends AbstractBlock {

  /** What a side of the tape runs to. */
  public enum Link implements IStringSerializable {
    NONE, TAPE, POST;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  public static final PropertyEnum<Link> NORTH = PropertyEnum.create("north", Link.class);
  public static final PropertyEnum<Link> EAST = PropertyEnum.create("east", Link.class);
  public static final PropertyEnum<Link> SOUTH = PropertyEnum.create("south", Link.class);
  public static final PropertyEnum<Link> WEST = PropertyEnum.create("west", Link.class);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 8 / 16.0, 0, 1, 12 / 16.0, 1);

  private final String registryName;

  public BlockSceneTape(String registryName) {
    super(stash(registryName), SoundType.CLOTH, "shears", 0, 0.2F, 0.2F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, Link.NONE)
        .withProperty(EAST, Link.NONE).withProperty(SOUTH, Link.NONE)
        .withProperty(WEST, Link.NONE));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.CLOTH;
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
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos, EnumFacing.NORTH))
        .withProperty(EAST, joins(world, pos, EnumFacing.EAST))
        .withProperty(SOUTH, joins(world, pos, EnumFacing.SOUTH))
        .withProperty(WEST, joins(world, pos, EnumFacing.WEST));
  }

  /** What the tape runs to on a side: tape or a solid face to tie onto, a stanchion, or nothing. */
  private static Link joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos next = pos.offset(side);
    IBlockState other = world.getBlockState(next);
    if (other.getBlock() instanceof BlockTapeStanchion) {
      return Link.POST;
    }
    if (other.getBlock() instanceof BlockSceneTape
        || other.getBlockFaceShape(world, next, side.getOpposite()) == BlockFaceShape.SOLID) {
      return Link.TAPE;
    }
    return Link.NONE;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
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
    return BlockRenderLayer.CUTOUT;
  }
}
