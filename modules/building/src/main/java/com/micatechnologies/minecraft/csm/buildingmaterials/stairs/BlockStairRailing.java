package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A stair railing, guard or wall handrail: one cell of it, in the air cell above a stair or a
 * floor, along one or more of the cell's edges.
 *
 * <p><b>Stored</b>: which edges the cell has a railing on ({@link #NORTH} to {@link #WEST}, a bit
 * each), so a landing's corner or U-turn is one cell. <b>Read from the world</b>: {@link #SLOPE},
 * for a cell with one edge -- whether the rail climbs a flight below it, bends level at its top
 * or bends up at its foot ({@link RailSlope}). The stairs are any {@link BlockStairs} the right
 * way up whose facing runs along the railing, vanilla's, CSM's or another mod's; their facing is
 * the way they climb. A cell whose railings turn a corner is level; both sides of a flight one
 * block wide slope. 112 states, no tile entity, nothing ticks: the model is baked.</p>
 *
 * <p><b>Placing</b>: against a wall, the edge is the wall's side; on a floor or a stair, the edge
 * nearest the point clicked. Clicking a railing with the same railing adds the nearest edge it
 * does not have yet.</p>
 *
 * <p><b>Collision</b>: a guard collides along each edge as tall as a fence, so it keeps players
 * from stepping off a landing; a wall handrail only where its rail is.</p>
 *
 * @since 2026.10
 */
public class BlockStairRailing extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyEnum<RailSlope> SLOPE = PropertyEnum.create("slope",
      RailSlope.class);

  /** What a railing is, for its collision and its render layer. */
  public enum Kind {
    /** Posts, rails and infill standing on the floor or the stairs. */
    GUARD,
    /** A guard with glass infill, drawn in the translucent layer. */
    GLASS,
    /** A handrail on brackets off the wall at the edge. */
    WALL
  }

  private static final double P = 1.0 / 16;
  /** A guard's collision on the cell's north edge, as tall as a fence. */
  private static final AxisAlignedBB GUARD_NORTH = new AxisAlignedBB(0, 0, 0, 1, 1.5, 3.5 * P);
  /** A wall handrail's collision on the north wall: where the rail runs, level or sloped. */
  private static final AxisAlignedBB WALL_NORTH = new AxisAlignedBB(0, 7 * P, 0, 1, 24 * P,
      4 * P);
  /** The outline on the north edge. */
  private static final AxisAlignedBB SELECT_NORTH = new AxisAlignedBB(0, 0, 0, 1, 1, 3.5 * P);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final Kind kind;

  /**
   * @param registryName its registry name
   * @param kind         what it is
   * @param wood         timber rather than metal (its material, sound and tool)
   */
  public BlockStairRailing(String registryName, Kind kind, boolean wood) {
    super(stash(registryName, wood), wood ? SoundType.WOOD : SoundType.METAL,
        wood ? "axe" : "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.kind = kind;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, true)
        .withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false)
        .withProperty(SLOPE, RailSlope.NONE));
  }

  private static Material stash(String registryName, boolean wood) {
    PENDING.set(registryName);
    return wood ? Material.WOOD : Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  /** What this railing is. */
  public Kind getKind() {
    return kind;
  }

  // region State

  private static PropertyBool edge(EnumFacing side) {
    switch (side) {
      case EAST:
        return EAST;
      case SOUTH:
        return SOUTH;
      case WEST:
        return WEST;
      default:
        return NORTH;
    }
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST, SLOPE);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
        | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    if ((meta & 15) == 0) {
      meta = 1;   // a cell with no edge is not a railing; show it on the north edge
    }
    return getDefaultState().withProperty(NORTH, (meta & 1) != 0)
        .withProperty(EAST, (meta & 2) != 0).withProperty(SOUTH, (meta & 4) != 0)
        .withProperty(WEST, (meta & 8) != 0);
  }

  /**
   * The axis the cell's railings run along, or null if it has none or they turn a corner (a
   * corner is level). Both sides of a flight one block wide share an axis.
   */
  @Nullable
  private static EnumFacing.Axis runAxis(IBlockState state) {
    EnumFacing.Axis axis = null;
    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
      if (state.getValue(edge(side))) {
        EnumFacing.Axis along = side.rotateY().getAxis();
        if (axis != null && axis != along) {
          return null;
        }
        axis = along;
      }
    }
    return axis;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(SLOPE, slope(state, world, pos));
  }

  /**
   * The way the rail runs at {@code pos}. {@code pos} climbs toward the positive end of the run's
   * axis, east or south; the blockstate mirrors the model for the south and west edges, whose
   * turn carries the model's east the other way.
   */
  private RailSlope slope(IBlockState state, IBlockAccess world, BlockPos pos) {
    EnumFacing.Axis axis = runAxis(state);
    if (axis == null) {
      return RailSlope.NONE;
    }
    EnumFacing ahead = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis);
    EnumFacing up = climbs(world.getBlockState(pos.down()), axis);
    if (up != null) {
      // The next stair up the flight sits beside this cell, at its height
      boolean top = climbs(world.getBlockState(pos.offset(up)), axis) != up;
      return RailSlope.over(up == ahead, top);
    }
    for (EnumFacing toward : new EnumFacing[]{ahead, ahead.getOpposite()}) {
      BlockPos first = pos.offset(toward);
      if (climbs(world.getBlockState(first), axis) == toward) {
        IBlockState over = world.getBlockState(first.up());
        if (over.getBlock() instanceof BlockStairRailing
            && (getMetaFromState(over) & getMetaFromState(state)) != 0) {
          return RailSlope.foot(toward == ahead);
        }
      }
    }
    return RailSlope.NONE;
  }

  /** The way a stair climbs, if it is the right way up and climbs along {@code axis}. */
  @Nullable
  private static EnumFacing climbs(IBlockState state, EnumFacing.Axis axis) {
    if (!(state.getBlock() instanceof BlockStairs)
        || state.getValue(BlockStairs.HALF) != BlockStairs.EnumHalf.BOTTOM) {
      return null;
    }
    EnumFacing facing = state.getValue(BlockStairs.FACING);
    return facing.getAxis() == axis ? facing : null;
  }

  // endregion

  // region Placing

  /** The edge of a cell nearest a point on it, {@code x} and {@code z} 0 to 1 across. */
  private static EnumFacing nearest(float x, float z, @Nullable IBlockState skip) {
    EnumFacing best = EnumFacing.NORTH;
    float bestDistance = Float.MAX_VALUE;
    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
      if (skip != null && skip.getValue(edge(side))) {
        continue;
      }
      float d;
      switch (side) {
        case NORTH:
          d = z;
          break;
        case SOUTH:
          d = 1 - z;
          break;
        case WEST:
          d = x;
          break;
        default:
          d = 1 - x;
          break;
      }
      if (d < bestDistance) {
        bestDistance = d;
        best = side;
      }
    }
    return best;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // Against a wall, the wall's side; on top of something, the edge nearest the point clicked
    EnumFacing side = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : nearest(hitX, hitZ, null);
    return getDefaultState().withProperty(NORTH, side == EnumFacing.NORTH).withProperty(EAST, side == EnumFacing.EAST)
        .withProperty(SOUTH, side == EnumFacing.SOUTH).withProperty(WEST, side == EnumFacing.WEST);
  }

  /** The same railing in hand adds the nearest edge the cell lacks. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (player.isSneaking() || held.isEmpty() || Block.getBlockFromItem(held.getItem()) != this
        || getMetaFromState(state) == 15) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing add = nearest(hitX, hitZ, state);
      world.setBlockState(pos, state.withProperty(edge(add), true), 3);
      SoundType sound = getSoundType(state, world, pos, player);
      world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
          (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
      if (!player.capabilities.isCreativeMode) {
        held.shrink(1);
      }
    }
    return true;
  }

  // endregion

  // region Shape

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB out = null;
    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
      if (state.getValue(edge(side))) {
        AxisAlignedBB b = RotationUtils.rotateBoundingBoxByFacing(SELECT_NORTH, side);
        out = out == null ? b : out.union(b);
      }
    }
    return out == null ? FULL_BLOCK_AABB : out;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    AxisAlignedBB north = kind == Kind.WALL ? WALL_NORTH : GUARD_NORTH;
    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
      if (state.getValue(edge(side))) {
        addCollisionBoxToList(pos, entityBox, boxes,
            RotationUtils.rotateBoundingBoxByFacing(north, side));
      }
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
    return kind == Kind.GLASS ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }

  // endregion
}
