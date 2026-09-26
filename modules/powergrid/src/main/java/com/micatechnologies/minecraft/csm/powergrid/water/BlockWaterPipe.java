package com.micatechnologies.minecraft.csm.powergrid.water;

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
 * A length of flanged ductile iron water pipe, centred in its block, that joins the pipe, the
 * inline fittings and the pumps' nozzles next to it on any of its six sides: runs, bends and
 * tees form by themselves, as the Life Safety standpipe's do.
 *
 * <p>Which sides are joined is actual state, from the neighbours, so nothing is stored and a
 * run re-forms as pieces are added or taken away. {@link #JOINT} is set wherever the pipe is not
 * a straight run and draws the cast fitting there. A pipe joined to nothing stands upright.</p>
 *
 * <p>A pipe carries one service, {@link #WATER} unless constructed with another: the gas yard's
 * welded steel pipe is this class carrying {@link #GAS}. A pipe joins only a pipe, and only the
 * fittings and nozzles, of its own service, so a gas run laid beside a water run stays apart.</p>
 *
 * @since 2026.9
 */
public class BlockWaterPipe extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** Set where the pipe is not a straight run, to draw the cast fitting. */
  public static final PropertyBool JOINT = PropertyBool.create("joint");

  /** The service of the water system's pipe, fittings and pumps. */
  public static final String WATER = "water";
  /** The service of the gas yard's pipe and fittings. */
  public static final String GAS = "gas";

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final String service;
  /** Half the width of the fitting and every arm, in blocks. */
  private final double r;

  public BlockWaterPipe(String registryName) {
    this(registryName, WATER, 5.0);
  }

  /**
   * @param registryName its registry name
   * @param service      the service it carries: it joins only pipe, fittings and nozzles of it
   * @param radius       the radius of its fitting, in sixteenths: its box and collision
   */
  public BlockWaterPipe(String registryName, String service, double radius) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 2F, 6F, 0F, 0);
    this.registryName = registryName;
    this.service = service;
    this.r = radius / 16.0;
    PENDING.remove();
  }

  /** The service this pipe carries. */
  public String getService() {
    return service;
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

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
  boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    IBlockState other = world.getBlockState(at);
    if (other.getBlock() instanceof BlockWaterPipe) {
      return service.equals(((BlockWaterPipe) other.getBlock()).service);
    }
    if (!(other.getBlock() instanceof IWaterPipeJoint)) {
      return false;
    }
    IWaterPipeJoint joint = (IWaterPipeJoint) other.getBlock();
    return service.equals(joint.pipeService())
        && joint.joinsWaterPipe(world, at, other, side.getOpposite());
  }

  /**
   * The six arms of the pipe at {@code pos}, in {@link EnumFacing} order; a pipe joined to
   * nothing stands upright.
   *
   * @param world the world
   * @param pos   the pipe's position
   *
   * @return which of its sides it joins
   */
  public boolean[] arms(IBlockAccess world, BlockPos pos) {
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

  public static boolean straight(boolean[] arms) {
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
    return new CsmBlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN, JOINT);
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

  /** The box round the fitting and every arm the pipe has. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    boolean[] a = arms(source, pos);
    double lo = 0.5 - r;
    double hi = 0.5 + r;
    return new AxisAlignedBB(a[EnumFacing.WEST.getIndex()] ? 0 : lo,
        a[EnumFacing.DOWN.getIndex()] ? 0 : lo, a[EnumFacing.NORTH.getIndex()] ? 0 : lo,
        a[EnumFacing.EAST.getIndex()] ? 1 : hi, a[EnumFacing.UP.getIndex()] ? 1 : hi,
        a[EnumFacing.SOUTH.getIndex()] ? 1 : hi);
  }

  /** Collides arm by arm, so a bend is not a solid block to walk into. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    boolean[] a = arms(world, pos);
    double lo = 0.5 - r;
    double hi = 0.5 + r;
    addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(lo, lo, lo, hi, hi, hi));
    if (a[EnumFacing.WEST.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(0, lo, lo, lo, hi, hi));
    }
    if (a[EnumFacing.EAST.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(hi, lo, lo, 1, hi, hi));
    }
    if (a[EnumFacing.DOWN.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(lo, 0, lo, hi, lo, hi));
    }
    if (a[EnumFacing.UP.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(lo, hi, lo, hi, 1, hi));
    }
    if (a[EnumFacing.NORTH.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(lo, lo, 0, hi, hi, lo));
    }
    if (a[EnumFacing.SOUTH.getIndex()]) {
      addCollisionBoxToList(pos, entityBox, boxes, new AxisAlignedBB(lo, lo, hi, hi, hi, 1));
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
