package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The job trailer's wall and window blocks: a site office built as a building is, of a floor,
 * walls and a roof, with air inside to walk about in and furnish, and a door from the doors family
 * set in an opening in the wall.
 *
 * <p>Every block draws a face only on a side where its neighbour is not trailer, and that face is
 * the outside (siding, the roof, the underside) or the inside (panelling, the floor, the ceiling).
 * Which, no block stores: a side is <b>in</b> when the space it faces has trailer both above and
 * below it within {@link #REACH} blocks -- the roof over a room and the floor under it -- and for
 * a top or bottom face, the one of those across the space. That is the whole rule, so it needs no
 * placing from a particular side, it holds for a doorway (the header is over it and the floor
 * under it, so its jambs are inside), and a trailer built as a solid box, as the first ones were,
 * has no space inside and still looks as it did. A trailer with no floor has nothing under its
 * rooms, so its inside is drawn as siding until a floor is laid.</p>
 *
 * <p>The window's glass can be seen through, from either side, and its reveals are drawn where the
 * wall carries on beside it, so looking through at an angle shows the depth of the wall. Windows
 * stacked on windows are one tall window ({@link #UPPER} and {@link #TOPPED}).</p>
 *
 * <p>Nothing is stored; the whole state is actual. The models come from
 * {@code dev-env-utils/scripts/gen_facilities.py}. A block's inside or outside can change when
 * trailer is placed or broken up to {@link #REACH} blocks away, further than the game rebuilds
 * around a changed block, so {@link JobTrailerRenderUpdater} rebuilds that far on the client.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockJobTrailer extends AbstractBlock {

  public static final PropertyEnum<TrailerSide> NORTH =
      PropertyEnum.create("north", TrailerSide.class);
  public static final PropertyEnum<TrailerSide> EAST =
      PropertyEnum.create("east", TrailerSide.class);
  public static final PropertyEnum<TrailerSide> SOUTH =
      PropertyEnum.create("south", TrailerSide.class);
  public static final PropertyEnum<TrailerSide> WEST =
      PropertyEnum.create("west", TrailerSide.class);
  public static final PropertyEnum<TrailerSide> UP = PropertyEnum.create("up", TrailerSide.class);
  public static final PropertyEnum<TrailerSide> DOWN =
      PropertyEnum.create("down", TrailerSide.class);
  /** The same block below: a window stacked on a window leaves out the frame between them. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");
  /** The same block above. */
  public static final PropertyBool TOPPED = PropertyBool.create("topped");

  /**
   * How far above and below a space the roof and the floor are looked for: a room up to seven
   * blocks high. Also how far a change reaches, for {@link JobTrailerRenderUpdater}.
   */
  public static final int REACH = 8;

  /** Steel that comes down by hand, as the scaffold's does. */
  private static final Material TRAILER_STEEL = new Material(MapColor.IRON);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockJobTrailer}.
   *
   * @param registryName {@code job_trailer_wall} or {@code job_trailer_window}
   *
   * @since 1.0
   */
  public BlockJobTrailer(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return TRAILER_STEEL;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  private boolean isWindow() {
    return "job_trailer_window".equals(getBlockRegistryName());
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN, UPPER, TOPPED);
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

  /**
   * Whether a block is part of a job trailer. The wall and window join each other; the door does
   * not, so the walls draw the opening round it.
   */
  static boolean isTrailer(IBlockState state) {
    return state.getBlock() instanceof BlockJobTrailer;
  }

  /** Whether there is trailer within {@link #REACH} blocks of {@code from}, going {@code dir}. */
  private static boolean trailerToward(IBlockAccess world, BlockPos from, EnumFacing dir) {
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(from);
    for (int i = 0; i < REACH; i++) {
      p.move(dir);
      if (p.getY() < 0 || p.getY() > 255) {
        return false;
      }
      if (isTrailer(world.getBlockState(p))) {
        return true;
      }
    }
    return false;
  }

  private static TrailerSide side(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos space = pos.offset(facing);
    if (isTrailer(world.getBlockState(space))) {
      return TrailerSide.JOINED;
    }
    boolean inside;
    if (facing.getAxis() == EnumFacing.Axis.Y) {
      inside = trailerToward(world, space, facing);
    } else {
      inside = trailerToward(world, space, EnumFacing.UP)
          && trailerToward(world, space, EnumFacing.DOWN);
    }
    return inside ? TrailerSide.IN : TrailerSide.OUT;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(NORTH, side(worldIn, pos, EnumFacing.NORTH))
        .withProperty(EAST, side(worldIn, pos, EnumFacing.EAST))
        .withProperty(SOUTH, side(worldIn, pos, EnumFacing.SOUTH))
        .withProperty(WEST, side(worldIn, pos, EnumFacing.WEST))
        .withProperty(UP, side(worldIn, pos, EnumFacing.UP))
        .withProperty(DOWN, side(worldIn, pos, EnumFacing.DOWN))
        .withProperty(UPPER, worldIn.getBlockState(pos.down()).getBlock() == this)
        .withProperty(TOPPED, worldIn.getBlockState(pos.up()).getBlock() == this);
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
    return BlockFaceShape.SOLID;
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

  /**
   * The window is drawn with the translucent blocks, for its tinted glass; the wall is cutout.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return isWindow() ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}
