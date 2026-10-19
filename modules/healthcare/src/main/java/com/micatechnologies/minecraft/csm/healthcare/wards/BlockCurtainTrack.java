package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The ceiling track a cubicle curtain hangs from ({@code cubicle_curtain_track}). It goes in the
 * ceiling-tile layer, in place of a tile: the cell draws a ceiling tile across its bottom and
 * the track hangs just below it, in the room. Tracks side by side join into one run by
 * themselves: straight, corners (inside or outside, an L either way), tees and crosses, with an
 * end cap where a run stops.
 *
 * <p>The curtain is {@link BlockCurtainPanel} blocks hung in a column under each track cell. A
 * track placed by hand fills its own column (matching the drop, pattern and state of the column
 * beside it, or down to one block above the floor); {@code /csmhealthcare curtain fill} fills a
 * whole run. Breaking a track takes its column with it. A click on the track opens or closes the
 * curtain below.</p>
 *
 * <p><b>Metadata</b>: bit 0 the axis a lone track runs along (0 east-west, 1 north-south). The
 * sides are worked out from the neighbours: {@code join} towards another track, {@code end} the
 * end of a run (the track runs to the cell's edge and is capped), {@code none}.</p>
 *
 * @since 2026.10
 */
public class BlockCurtainTrack extends AbstractBlock {

  /** What one side of a track cell does. */
  public enum Side implements IStringSerializable {
    NONE, JOIN, END;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  public static final PropertyEnum<Side> NORTH = PropertyEnum.create("north", Side.class);
  public static final PropertyEnum<Side> EAST = PropertyEnum.create("east", Side.class);
  public static final PropertyEnum<Side> SOUTH = PropertyEnum.create("south", Side.class);
  public static final PropertyEnum<Side> WEST = PropertyEnum.create("west", Side.class);

  /** The tile and the track below it. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, -1.5 / 16, 0, 1, 1 / 16.0, 1);
  /** Only the tile collides. */
  private static final AxisAlignedBB PLATE = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  public BlockCurtainTrack() {
    super(HealthcareMaterial.METAL.getMaterial(), HealthcareMaterial.METAL.getSound(), "pickaxe",
        0, 1.0F, 2.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X)
        .withProperty(NORTH, Side.NONE).withProperty(EAST, Side.END)
        .withProperty(SOUTH, Side.NONE).withProperty(WEST, Side.END));
  }

  @Override
  public String getBlockRegistryName() {
    return "cubicle_curtain_track";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS,
        (meta & 1) == 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  /**
   * Whether the track at {@code pos} joins towards {@code side}.
   *
   * @param world the world
   * @param pos   a track
   * @param side  a horizontal direction
   *
   * @return true if a track is there
   */
  public boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    return world.getBlockState(pos.offset(side)).getBlock() == this;
  }

  private static PropertyEnum<Side> property(EnumFacing side) {
    switch (side) {
      case NORTH:
        return NORTH;
      case EAST:
        return EAST;
      case SOUTH:
        return SOUTH;
      default:
        return WEST;
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = state;
    int count = 0;
    EnumFacing only = null;
    for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
      boolean j = joins(world, pos, d);
      s = s.withProperty(property(d), j ? Side.JOIN : Side.NONE);
      if (j) {
        count++;
        only = d;
      }
    }
    if (count == 0) {
      boolean x = state.getValue(AXIS) == EnumFacing.Axis.X;
      for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
        if ((d.getAxis() == EnumFacing.Axis.X) == x) {
          s = s.withProperty(property(d), Side.END);
        }
      }
    } else if (count == 1) {
      // the run's end cell carries the track to the far edge of its cell and caps it there
      s = s.withProperty(property(only.getOpposite()), Side.END);
    }
    return s;
  }

  /**
   * The axis a lone panel hung under this track runs along: the track's own straight line, or
   * its stored axis.
   *
   * @param world the world
   * @param pos   the track
   *
   * @return the axis
   */
  public EnumFacing.Axis lineAxis(IBlockAccess world, BlockPos pos) {
    boolean x = joins(world, pos, EnumFacing.EAST) || joins(world, pos, EnumFacing.WEST);
    boolean z = joins(world, pos, EnumFacing.NORTH) || joins(world, pos, EnumFacing.SOUTH);
    if (x != z) {
      return x ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
    }
    return world.getBlockState(pos).getValue(AXIS);
  }

  /** A track placed by hand fills its own column. */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (!world.isRemote) {
      CubicleCurtains.fillColumnLikeNeighbours(world, pos);
    }
  }

  /** The curtain below goes with its track. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    CubicleCurtains.clearColumn(world, pos);
    super.breakBlock(world, pos, state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    BlockPos below = pos.down();
    if (!(world.getBlockState(below).getBlock() instanceof BlockCurtainPanel)) {
      return false;
    }
    if (!world.isRemote) {
      CubicleCurtains.toggle(world, below);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return PLATE;
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
