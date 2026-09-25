package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A queue stanchion: a weighted base, a chrome post and a retractable belt. Its belt reaches out
 * to every stanchion beside it -- {@link #NORTH} to {@link #WEST}, world sides, actual state -- so
 * a row of them is one barrier and a queue is laid out as rows of stanchions with a block of floor
 * between them, a row stopping a block short where the lane turns. The belts collide up to a
 * fence's height, so a queue cannot be stepped or jumped through. Any two stanchions join,
 * whatever the colour of their belts; the belt drawn in each block is that block's.
 *
 * @since 2026.9
 */
public class BlockQueueStanchion extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final double TOP = 1.5;
  private static final AxisAlignedBB POST = new AxisAlignedBB(6.5 / 16.0, 0, 6.5 / 16.0,
      9.5 / 16.0, 1, 9.5 / 16.0);
  private static final AxisAlignedBB POST_COLLISION = new AxisAlignedBB(7 / 16.0, 0, 7 / 16.0,
      9 / 16.0, TOP, 9 / 16.0);
  private static final AxisAlignedBB ARM_NORTH = new AxisAlignedBB(7.6 / 16.0, 0, 0, 8.4 / 16.0,
      TOP, 8 / 16.0);
  private static final AxisAlignedBB ARM_SOUTH = new AxisAlignedBB(7.6 / 16.0, 0, 8 / 16.0,
      8.4 / 16.0, TOP, 1);
  private static final AxisAlignedBB ARM_WEST = new AxisAlignedBB(0, 0, 7.6 / 16.0, 8 / 16.0,
      TOP, 8.4 / 16.0);
  private static final AxisAlignedBB ARM_EAST = new AxisAlignedBB(8 / 16.0, 0, 7.6 / 16.0, 1,
      TOP, 8.4 / 16.0);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a stanchion.
   *
   * @param registryName its registry name
   */
  public BlockQueueStanchion(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 6.0F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(NORTH, false)
        .withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos.north()))
        .withProperty(EAST, joins(world, pos.east()))
        .withProperty(SOUTH, joins(world, pos.south()))
        .withProperty(WEST, joins(world, pos.west()));
  }

  private static boolean joins(IBlockAccess world, BlockPos at) {
    return world.getBlockState(at).getBlock() instanceof BlockQueueStanchion;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return POST;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(world, pos);
    addCollisionBoxToList(pos, entityBox, collidingBoxes, POST_COLLISION);
    if (actual.getValue(NORTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_NORTH);
    }
    if (actual.getValue(SOUTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_SOUTH);
    }
    if (actual.getValue(WEST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_WEST);
    }
    if (actual.getValue(EAST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_EAST);
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
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
