package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPipe;
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
 * Decorative exposed ductwork and pipe for an open ceiling: round spiral duct, rectangular duct
 * and painted pipe, one class constructed by registry name ({@code duct_round_},
 * {@code duct_rect_}, {@code pipe_}). Decorative only: no air moves through it, and its shape
 * passes air for the thermal scanner (it spans its cell in one direction at most).
 *
 * <p>Stored: the {@code axis} the segment runs along (the face clicked when it is a wall's, or
 * the placer's look; straight up or down when the placer looks steeply). Actual state joins it
 * with the runs of the same kind (any colour) round it, by the cable trays' rule: along its own
 * axis it reaches to any neighbour of its kind, and out of an open end too unless it also turns;
 * sideways it reaches only to a neighbour whose own axis points at it, so two parallel runs do not
 * mat together. A rectangular duct joins only level. The blockstate draws the segment's middle
 * along the one axis its arms share, or as a fitting where they turn, from the arms alone. A
 * level segment under a solid ceiling, or under a {@link BlockExposedServiceHanger}, hangs on a
 * {@code rod}. 384 states. Models from
 * {@code dev-env-utils/scripts/gen_exposed_services.py}.</p>
 *
 * @since 2026.10
 */
public class BlockExposedDuct extends AbstractBlock implements ICsmPipe {

  public static final PropertyEnum<Axis> AXIS = PropertyEnum.create("axis", Axis.class);
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  public static final PropertyBool ROD = PropertyBool.create("rod");

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String kind;
  /** Half the cross-section across and up, in sixteenths. */
  private final double halfWide;
  private final double halfHigh;

  public BlockExposedDuct(String registryName) {
    super(pending(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 6F, 0F, 0);
    this.registryName = registryName;
    this.kind = kindOf(registryName);
    this.halfWide = "pipe".equals(kind) ? 2 : 6;
    this.halfHigh = "pipe".equals(kind) ? 2 : "duct_rect".equals(kind) ? 4 : 6;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, Axis.Z)
        .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
        .withProperty(WEST, false).withProperty(UP, false).withProperty(DOWN, false)
        .withProperty(ROD, false));
  }

  private static Material pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  private static String kindOf(String name) {
    return name.startsWith("duct_round") ? "duct_round"
        : name.startsWith("duct_rect") ? "duct_rect" : "pipe";
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, NORTH, EAST, SOUTH, WEST, UP, DOWN, ROD);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    Axis axis = Axis.values()[Math.min(meta & 3, 2)];
    return getDefaultState().withProperty(AXIS, axis);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS).ordinal();
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    Axis axis;
    if (facing.getAxis() != Axis.Y) {
      axis = facing.getAxis();
    } else if (Math.abs(placer.rotationPitch) > 60 && !"duct_rect".equals(kind)) {
      axis = Axis.Y;
    } else {
      axis = placer.getHorizontalFacing().getAxis();
    }
    return getDefaultState().withProperty(AXIS, axis);
  }

  private static PropertyBool arm(EnumFacing side) {
    switch (side) {
      case NORTH:
        return NORTH;
      case EAST:
        return EAST;
      case SOUTH:
        return SOUTH;
      case WEST:
        return WEST;
      case UP:
        return UP;
      default:
        return DOWN;
    }
  }

  /** The neighbour towards {@code side}, if it is a run of this kind. */
  @Nullable
  private IBlockState sameKind(IBlockAccess world, BlockPos pos, EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockExposedDuct
        && ((BlockExposedDuct) other.getBlock()).kind.equals(kind) ? other : null;
  }

  private boolean pointsHere(IBlockAccess world, BlockPos pos, EnumFacing side) {
    IBlockState other = sameKind(world, pos, side);
    return other != null && other.getValue(AXIS) == side.getAxis();
  }

  private boolean reaches(IBlockAccess world, BlockPos pos, Axis axis, EnumFacing side) {
    if ("duct_rect".equals(kind) && side.getAxis() == Axis.Y) {
      return false;
    }
    if (side.getAxis() != axis) {
      return pointsHere(world, pos, side);
    }
    if (sameKind(world, pos, side) != null) {
      return true;
    }
    for (EnumFacing across : EnumFacing.values()) {
      if (across.getAxis() != axis && reaches(world, pos, axis, across)) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    Axis axis = state.getValue(AXIS);
    IBlockState out = state;
    boolean vertical = false;
    for (EnumFacing side : EnumFacing.values()) {
      boolean r = reaches(world, pos, axis, side);
      out = out.withProperty(arm(side), r);
      vertical |= r && side.getAxis() == Axis.Y;
    }
    BlockPos up = pos.up();
    IBlockState above = world.getBlockState(up);
    boolean rod = !vertical && (above.getBlock() instanceof BlockExposedServiceHanger
        || above.getBlockFaceShape(world, up, EnumFacing.DOWN) == BlockFaceShape.SOLID);
    return out.withProperty(ROD, rod);
  }

  /** A sprinkler fitting joins the pipe; a duct is not a pipe to it (#304). */
  @Override
  @SuppressWarnings("deprecation")
  public int pipeBottom(IBlockAccess world, BlockPos pos, IBlockState state) {
    if (!"pipe".equals(kind)) {
      return -1;
    }
    return getActualState(state, world, pos).getValue(DOWN) ? 0 : (int) (8 - halfHigh);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int pipeTop(IBlockAccess world, BlockPos pos, IBlockState state) {
    if (!"pipe".equals(kind)) {
      return -1;
    }
    return getActualState(state, world, pos).getValue(UP) ? 16 : (int) (8 + halfHigh);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double w = halfWide / 16;
    double h = halfHigh / 16;
    switch (state.getValue(AXIS)) {
      case X:
        return new AxisAlignedBB(0, 0.5 - h, 0.5 - w, 1, 0.5 + h, 0.5 + w);
      case Y:
        return new AxisAlignedBB(0.5 - w, 0, 0.5 - w, 0.5 + w, 1, 0.5 + w);
      default:
        return new AxisAlignedBB(0.5 - w, 0.5 - h, 0, 0.5 + w, 0.5 + h, 1);
    }
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
