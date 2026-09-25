package com.micatechnologies.minecraft.csm.transit.station;

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
 * The subway entrance kiosk's framed glass wall, built block by block. It joins as a pane does:
 * toward every side where more entrance glass, a booth counter running the same way, or a solid
 * face continues it, a glass arm runs out from the post that stands at every block
 * ({@link #NORTH} to {@link #WEST}, actual state). {@link #UP} and {@link #DOWN} say whether the
 * wall carries on above or below, so the head rail and kick plate are drawn only where it stops
 * and a wall two blocks tall is one pane. A block with nothing beside it is drawn as a panel
 * along x. Nothing is stored; the model comes from {@code gen_transit_stations.py}.
 *
 * @since 2026.9
 */
public class BlockStationGlass extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private static final double LO = 7 / 16.0;
  private static final double HI = 9 / 16.0;
  private static final AxisAlignedBB POST = new AxisAlignedBB(LO, 0, LO, HI, 1, HI);
  private static final AxisAlignedBB ARM_NORTH = new AxisAlignedBB(LO, 0, 0, HI, 1, HI);
  private static final AxisAlignedBB ARM_SOUTH = new AxisAlignedBB(LO, 0, LO, HI, 1, 1);
  private static final AxisAlignedBB ARM_WEST = new AxisAlignedBB(0, 0, LO, HI, 1, HI);
  private static final AxisAlignedBB ARM_EAST = new AxisAlignedBB(LO, 0, LO, 1, 1, HI);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a glass wall.
   *
   * @param registryName its registry name
   */
  public BlockStationGlass(String registryName) {
    super(stash(registryName), SoundType.GLASS, "pickaxe", 0, 1.5F, 6.0F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(NORTH, false)
        .withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false)
        .withProperty(UP, false).withProperty(DOWN, false));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.GLASS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN);
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
    IBlockState below = world.getBlockState(pos.down());
    return state.withProperty(NORTH, joins(world, pos, EnumFacing.NORTH))
        .withProperty(EAST, joins(world, pos, EnumFacing.EAST))
        .withProperty(SOUTH, joins(world, pos, EnumFacing.SOUTH))
        .withProperty(WEST, joins(world, pos, EnumFacing.WEST))
        .withProperty(UP, world.getBlockState(pos.up()).getBlock() instanceof BlockStationGlass)
        .withProperty(DOWN, below.getBlock() instanceof BlockStationGlass
            || below.getBlock() instanceof BlockStationBoothCounter);
  }

  /**
   * Whether the wall runs on toward {@code side}: more glass, a booth counter whose run lies
   * that way, or the solid face of a block (a wall the kiosk is built against).
   */
  private static boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    IBlockState other = world.getBlockState(at);
    if (other.getBlock() instanceof BlockStationGlass) {
      return true;
    }
    if (other.getBlock() instanceof BlockStationBoothCounter) {
      return other.getValue(BlockStationBoothCounter.FACING).getAxis() != side.getAxis();
    }
    return other.getBlockFaceShape(world, at, side.getOpposite()) == BlockFaceShape.SOLID;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState a = state.getActualState(source, pos);
    boolean lone = !a.getValue(NORTH) && !a.getValue(EAST) && !a.getValue(SOUTH)
        && !a.getValue(WEST);
    return new AxisAlignedBB(a.getValue(WEST) || lone ? 0 : LO, 0, a.getValue(NORTH) ? 0 : LO,
        a.getValue(EAST) || lone ? 1 : HI, 1, a.getValue(SOUTH) ? 1 : HI);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState a = isActualState ? state : state.getActualState(world, pos);
    boolean lone = !a.getValue(NORTH) && !a.getValue(EAST) && !a.getValue(SOUTH)
        && !a.getValue(WEST);
    addCollisionBoxToList(pos, entityBox, collidingBoxes, POST);
    if (a.getValue(NORTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_NORTH);
    }
    if (a.getValue(SOUTH)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_SOUTH);
    }
    if (a.getValue(WEST) || lone) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_WEST);
    }
    if (a.getValue(EAST) || lone) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_EAST);
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_SMALL
        : BlockFaceShape.MIDDLE_POLE_THIN;
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
    return BlockRenderLayer.TRANSLUCENT;
  }
}
