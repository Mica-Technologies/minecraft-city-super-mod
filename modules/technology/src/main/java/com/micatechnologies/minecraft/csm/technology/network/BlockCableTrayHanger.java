package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A block of threaded hanger rod, for hanging a {@link BlockCableTray} a block or more below a
 * ceiling: stacked under the ceiling, with a hung tray (one at the top of its block) beneath the
 * stack, the rods run from the ceiling down into the tray's own rods, which reach the top of the
 * tray's block. One block serves the basket and the ladder: both trays carry their rods at the
 * same places.
 *
 * <p>Stored: an axis (the placer's look), used only when there is no tray beneath. Everything
 * else is actual state: looking down through the stack to the first block that is not a hanger,
 * a rod is drawn on each side where that tray draws a rail (and so a rod) -- both sides of a
 * straight run, one of a tee, the outside two of a corner -- so the rods line up whatever the
 * tray does there. A crossing has no rails, so its hangers fall back on the tray's axis. With no
 * hung tray beneath, the rods stand on the two sides across the stored axis. No tile entity,
 * nothing ticks, and no collision: the rods are too thin to stand on.</p>
 *
 * @since 2026.10
 */
public class BlockCableTrayHanger extends AbstractBlock {

  public static final PropertyEnum<Axis> AXIS = BlockCableTray.AXIS;
  public static final PropertyBool NORTH = BlockCableTray.NORTH;
  public static final PropertyBool EAST = BlockCableTray.EAST;
  public static final PropertyBool SOUTH = BlockCableTray.SOUTH;
  public static final PropertyBool WEST = BlockCableTray.WEST;

  /** How far down through a stack of hangers to look for the tray. */
  private static final int MAX_STACK = 32;

  private static final AxisAlignedBB BOX = new AxisAlignedBB(1.5 / 16, 0, 1.5 / 16, 14.5 / 16, 1,
      14.5 / 16);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * @param registryName the block's registry name
   */
  public BlockCableTrayHanger(String registryName) {
    super(pending(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 10F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, Axis.Z)
        .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
        .withProperty(WEST, false));
  }

  private static Material pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, NORTH, EAST, SOUTH, WEST);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? Axis.Z : Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == Axis.X ? 1 : 0;
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState below = worldIn.getBlockState(pos.down());
    Axis axis = below.getBlock() instanceof BlockCableTray
        || below.getBlock() instanceof BlockCableTrayHanger ? below.getValue(AXIS)
        : placer.getHorizontalFacing().getAxis();
    return getDefaultState().withProperty(AXIS, axis);
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    BlockPos q = pos.down();
    for (int i = 0; i < MAX_STACK && worldIn.getBlockState(q).getBlock() == this; i++) {
      q = q.down();
    }
    IBlockState below = worldIn.getBlockState(q);
    if (below.getBlock() instanceof BlockCableTray && below.getValue(BlockCableTray.TOP)) {
      IBlockState tray = below.getActualState(worldIn, q);
      boolean north = !tray.getValue(BlockCableTray.NORTH);
      boolean east = !tray.getValue(BlockCableTray.EAST);
      boolean south = !tray.getValue(BlockCableTray.SOUTH);
      boolean west = !tray.getValue(BlockCableTray.WEST);
      if (north || east || south || west) {
        return state.withProperty(NORTH, north).withProperty(EAST, east)
            .withProperty(SOUTH, south).withProperty(WEST, west);
      }
      return across(state, tray.getValue(AXIS));
    }
    return across(state, state.getValue(AXIS));
  }

  /** Rods on the two sides across a run along {@code axis}, where a straight tray's rails are. */
  private static IBlockState across(IBlockState state, Axis axis) {
    boolean z = axis == Axis.Z;
    return state.withProperty(NORTH, !z).withProperty(SOUTH, !z).withProperty(EAST, z)
        .withProperty(WEST, z);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Nonnull
  @Override
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
