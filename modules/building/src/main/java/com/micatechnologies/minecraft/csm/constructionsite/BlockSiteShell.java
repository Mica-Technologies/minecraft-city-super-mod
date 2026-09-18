package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A block that joins its neighbours into one object built to any size: shipping containers,
 * roll-off dumpsters and the job trailer.
 *
 * <p>A container is several blocks long, wide and high, and a real one comes in more than one
 * size, so rather than a fixed multi-block prop it is built block by block: each block draws a
 * wall only on a side where its neighbour is not part of the same object, and a frame rail only
 * along an edge where both sides are outside. Six by two by two blocks is a twenty-foot container,
 * twelve long a forty-foot one. Blocks of the same colour always join, so two containers touching
 * side by side or stacked read as one big one unless they differ in colour, which is also how a
 * real container stack reads; that is the price of building to size.</p>
 *
 * <ul>
 *   <li>A <b>container</b> has doors on the end it faces -- the face toward the player who placed
 *       it -- and corrugated walls everywhere else.</li>
 *   <li>A <b>dumpster</b> is the same with the top left open: its walls are drawn inside and out,
 *       with a lip along the top, and only its walls and floor collide, so it can be filled.</li>
 *   <li>The <b>job trailer</b> is three blocks that join each other -- plain wall, window and door
 *       -- so a trailer is built with its windows and door where you want them. A door stacked on
 *       a door draws the door's upper half.</li>
 * </ul>
 *
 * <p>Stored: the facing, which only a container uses. The six neighbours and whether a door has a
 * door below it are actual state. The models come from
 * {@code dev-env-utils/scripts/gen_facilities.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSiteShell extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** A door block with another door block under it: it draws the door's upper half. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");

  /** Steel that comes down by hand, as the scaffold's does. */
  private static final Material SHELL_STEEL = new Material(MapColor.IRON);

  private static final double WALL = 1.5 / 16.0;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockSiteShell}.
   *
   * @param registryName the registry name; {@code container_*} and {@code dumpster_*} each join
   *                     only their own block, {@code job_trailer_*} join each other
   *
   * @since 1.0
   */
  public BlockSiteShell(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return SHELL_STEEL;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  private String family() {
    String name = getBlockRegistryName();
    return name.startsWith("job_trailer") ? "job_trailer" : name;
  }

  private boolean isDumpster() {
    return getBlockRegistryName().startsWith("dumpster");
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN, UPPER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  private boolean joins(IBlockAccess world, BlockPos pos) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() instanceof BlockSiteShell
        && ((BlockSiteShell) other.getBlock()).family().equals(family());
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(NORTH, joins(worldIn, pos.north()))
        .withProperty(EAST, joins(worldIn, pos.east()))
        .withProperty(SOUTH, joins(worldIn, pos.south()))
        .withProperty(WEST, joins(worldIn, pos.west()))
        .withProperty(UP, joins(worldIn, pos.up()))
        .withProperty(DOWN, joins(worldIn, pos.down()))
        .withProperty(UPPER, worldIn.getBlockState(pos.down()).getBlock() == this);
  }

  /**
   * A dumpster collides as its walls and floor on the outside of the whole dumpster, so it can be
   * stood in and filled; everything else is a solid block.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    if (!isDumpster()) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
      return;
    }
    IBlockState a = isActualState ? state : state.getActualState(worldIn, pos);
    if (!a.getValue(NORTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(0, 0, 0, 1, 1, WALL));
    }
    if (!a.getValue(SOUTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          new AxisAlignedBB(0, 0, 1 - WALL, 1, 1, 1));
    }
    if (!a.getValue(WEST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(0, 0, 0, WALL, 1, 1));
    }
    if (!a.getValue(EAST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          new AxisAlignedBB(1 - WALL, 0, 0, 1, 1, 1));
    }
    if (!a.getValue(DOWN)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          new AxisAlignedBB(0, 0, 0, 1, 1.0 / 16.0, 1));
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return !isDumpster() || face == EnumFacing.DOWN ? BlockFaceShape.SOLID
        : BlockFaceShape.UNDEFINED;
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
