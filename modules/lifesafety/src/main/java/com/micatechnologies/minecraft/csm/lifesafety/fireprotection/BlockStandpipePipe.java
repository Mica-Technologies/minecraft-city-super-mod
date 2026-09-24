package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

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
 * A free-standing length of dry standpipe, centred in its block, that joins the standpipe next to
 * it on any of its six sides: runs, elbows and tees form by themselves.
 *
 * <p>It joins another free-standing pipe on any side, and a wall pipe
 * ({@link BlockStandpipeWallPipe}) only through that pipe's front, where the two pipes' axes
 * meet: a wall pipe's axis is set back toward its wall, so side by side the two would not line
 * up. A main and a branch pipe join; the step where the main's end meets the branch is the
 * reducer.</p>
 *
 * <p>Which sides are joined is actual state, from the neighbours, so nothing is stored and a run
 * re-forms as pieces are added or taken away. {@link #JOINT} is set wherever the pipe is not a
 * straight run (a bend, a tee or an end), and draws the cast fitting there. A pipe joined to
 * nothing is drawn standing upright.</p>
 *
 * @version 1.0
 */
public class BlockStandpipePipe extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** Set where the pipe is not a straight run, so the cast fitting is drawn. */
  public static final PropertyBool JOINT = PropertyBool.create("joint");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final float radius;

  /**
   * @param registryName its registry name
   * @param radius       the pipe's radius, in pixels (4 for a main, 3 for a branch)
   */
  public BlockStandpipePipe(String registryName, float radius) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 2.0F, 6.0F, 0.0F, 0);
    this.registryName = registryName;
    this.radius = radius;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, false)
        .withProperty(SOUTH, false).withProperty(EAST, false).withProperty(WEST, false)
        .withProperty(UP, false).withProperty(DOWN, false).withProperty(JOINT, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public float getRadius() {
    return radius;
  }

  /** The property for the arm toward {@code side}. */
  static PropertyBool arm(EnumFacing side) {
    switch (side) {
      case NORTH:
        return NORTH;
      case SOUTH:
        return SOUTH;
      case EAST:
        return EAST;
      case WEST:
        return WEST;
      case UP:
        return UP;
      default:
        return DOWN;
    }
  }

  /** Whether the pipe at {@code pos} joins whatever is on its {@code side}. */
  static boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    if (other.getBlock() instanceof BlockStandpipePipe) {
      return true;
    }
    if (other.getBlock() instanceof BlockStandpipeWallPipe) {
      // Only through the wall pipe's front: this pipe stands in front of it.
      BlockStandpipeWallPipe wall = (BlockStandpipeWallPipe) other.getBlock();
      EnumFacing front = other.getValue(BlockStandpipeWallPipe.FACING);
      return side == front.getOpposite() && wall.getFitting().takesFront();
    }
    return false;
  }

  /** The six arms, in {@link EnumFacing} order; a pipe joined to nothing stands upright. */
  static boolean[] arms(IBlockAccess world, BlockPos pos) {
    boolean[] arms = new boolean[6];
    boolean any = false;
    for (EnumFacing side : EnumFacing.values()) {
      arms[side.getIndex()] = joins(world, pos, side);
      any |= arms[side.getIndex()];
    }
    if (!any) {
      arms[EnumFacing.UP.getIndex()] = true;
      arms[EnumFacing.DOWN.getIndex()] = true;
    }
    return arms;
  }

  /** A straight run: exactly two arms, opposite each other. */
  static boolean straight(boolean[] arms) {
    int count = 0;
    for (boolean a : arms) {
      count += a ? 1 : 0;
    }
    if (count != 2) {
      return false;
    }
    for (EnumFacing side : EnumFacing.values()) {
      if (arms[side.getIndex()] && arms[side.getOpposite().getIndex()]) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN, JOINT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    boolean[] arms = arms(world, pos);
    IBlockState actual = state;
    for (EnumFacing side : EnumFacing.values()) {
      actual = actual.withProperty(arm(side), arms[side.getIndex()]);
    }
    return actual.withProperty(JOINT, !straight(arms));
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

  /** The core, grown out along each joined arm. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    boolean[] arms = arms(source, pos);
    double r = (radius + (straight(arms) ? 0.0 : 0.8)) / 16.0;
    return new AxisAlignedBB(
        arms[EnumFacing.WEST.getIndex()] ? 0 : 0.5 - r,
        arms[EnumFacing.DOWN.getIndex()] ? 0 : 0.5 - r,
        arms[EnumFacing.NORTH.getIndex()] ? 0 : 0.5 - r,
        arms[EnumFacing.EAST.getIndex()] ? 1 : 0.5 + r,
        arms[EnumFacing.UP.getIndex()] ? 1 : 0.5 + r,
        arms[EnumFacing.SOUTH.getIndex()] ? 1 : 0.5 + r);
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
