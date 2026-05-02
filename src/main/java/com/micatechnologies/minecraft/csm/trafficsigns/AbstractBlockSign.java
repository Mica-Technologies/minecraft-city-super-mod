package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

@MethodsReturnNonnullByDefault
public abstract class AbstractBlockSign extends AbstractBlockRotatableHZEight
    implements ICsmNoSnowAccumulation {

  public static final PropertyBool DOWNWARD = PropertyBool.create("downward");
  public static final PropertyEnum<SignShift> SHIFT =
      PropertyEnum.create("shift", SignShift.class);

  private static final ConcurrentHashMap<Long, Boolean> SETBACK_CACHE = new ConcurrentHashMap<>();

  public AbstractBlockSign() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    SignShift shift = state.getValue(SHIFT);
    double minY = state.getValue(DOWNWARD) ? -0.5 : 0.0;

    switch (shift) {
      case SETBACK:
        // Sign panel at z~12.5 model units; 5-unit deep box centered on sign
        return new AxisAlignedBB(0, minY, 0.625, 1, 1, 0.9375);
      case BACKTOBACK:
        // Sign panel at z~28.5 model units; box extends past block edge to cover sign
        return new AxisAlignedBB(0, 0, 0.6875, 1, 1, 1.8125);
      default:
        return new AxisAlignedBB(0, minY, 0, 1, 1, 0.21875);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn,
      BlockPos pos) {
    IBlockState actualState = state.getActualState(worldIn, pos);
    SignShift shift = actualState.getValue(SHIFT);

    switch (shift) {
      case BACKTOBACK:
        return NULL_AABB;
      case SETBACK:
        double minY = actualState.getValue(DOWNWARD) ? -0.5 : 0.0;
        AxisAlignedBB bb = new AxisAlignedBB(0, minY, 0.75, 1, 1, 0.8125);
        return RotationUtils.rotateBoundingBoxByFacing(bb, actualState.getValue(FACING));
      default:
        return RotationUtils.rotateBoundingBoxByFacing(
            getBlockBoundingBox(actualState, worldIn, pos), actualState.getValue(FACING));
    }
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  public boolean getBlockBelowIsSlab(IBlockAccess source, BlockPos pos) {
    return source.getBlockState(pos.down()).getBlock() instanceof BlockSlab;
  }

  public boolean getBlockIsInFrontOfSignalArm(IBlockAccess source, BlockPos pos) {
    IBlockState sourceBlockState = source.getBlockState(pos);
    if (!(sourceBlockState.getBlock() instanceof AbstractBlockSign)) {
      return false;
    }
    DirectionEight facing8 = source.getBlockState(pos).getValue(FACING);
    if (facing8 == DirectionEight.N) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.S) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.E) {
      return source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.W) {
      return source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.NE) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.NW) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.SE) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.SW) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    }
    return false;
  }

  /**
   * Determines the shift mode for this sign. Back-to-back is detected when the block directly
   * behind this sign (opposite its facing direction) contains another {@link AbstractBlockSign}
   * facing the opposite direction, and this sign has no structural support below (slab, sign, or
   * traffic pole). Signs with support below never shift — only the unsupported partner shifts.
   *
   * <p>Back-to-back takes priority over signal-arm setback.
   *
   * @param source the block access
   * @param pos    the position of this sign
   *
   * @return {@code true} if this sign should render in back-to-back mode
   */
  public boolean getShouldBackToBack(IBlockAccess source, BlockPos pos) {
    IBlockState state = source.getBlockState(pos);
    if (!(state.getBlock() instanceof AbstractBlockSign)) {
      return false;
    }
    Block blockBelow = source.getBlockState(pos.down()).getBlock();
    if (blockBelow instanceof BlockSlab || blockBelow instanceof AbstractBlockSign
        || blockBelow instanceof AbstractBlockTrafficPole) {
      return false;
    }
    DirectionEight facing = state.getValue(FACING);
    if (facing.isDiagonal()) {
      return false;
    }
    DirectionEight opposite = facing.getOpposite();
    BlockPos behindPos = pos.add(opposite.getOffsetX(), 0, opposite.getOffsetZ());
    IBlockState behindState = source.getBlockState(behindPos);
    if (!(behindState.getBlock() instanceof AbstractBlockSign)) {
      return false;
    }
    return behindState.getValue(FACING) == opposite;
  }

  public boolean getShouldSetback(IBlockAccess source, BlockPos pos) {
    if (getBlockIsInFrontOfSignalArm(source, pos)) {
      return true;
    }

    if (source.getBlockState(pos.up()).getBlock() instanceof AbstractBlockSign) {
      if (getBlockIsInFrontOfSignalArm(source, pos.up())) {
        return true;
      }
    }

    if (source.getBlockState(pos.down()).getBlock() instanceof AbstractBlockSign) {
      if (getBlockIsInFrontOfSignalArm(source, pos.down())) {
        return true;
      }
    }

    return false;
  }


  /**
   * Creates a new {@link BlockStateContainer} for the block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DOWNWARD, SHIFT);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    SETBACK_CACHE.remove(pos.toLong());
    SETBACK_CACHE.remove(pos.up().toLong());
    SETBACK_CACHE.remove(pos.down().toLong());
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    SETBACK_CACHE.remove(pos.toLong());
    super.breakBlock(worldIn, pos, state);
  }

  private boolean getSetbackCached(IBlockAccess source, BlockPos pos) {
    Long key = pos.toLong();
    Boolean cached = SETBACK_CACHE.get(key);
    if (cached != null) return cached;
    boolean result = getShouldSetback(source, pos);
    SETBACK_CACHE.put(key, result);
    return result;
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    SignShift shift;
    if (getShouldBackToBack(worldIn, pos)) {
      shift = SignShift.BACKTOBACK;
    } else if (getSetbackCached(worldIn, pos)) {
      shift = SignShift.SETBACK;
    } else {
      shift = SignShift.NONE;
    }
    return state.withProperty(DOWNWARD,
            shift != SignShift.BACKTOBACK && getBlockBelowIsSlab(worldIn, pos))
        .withProperty(SHIFT, shift);
  }
}
