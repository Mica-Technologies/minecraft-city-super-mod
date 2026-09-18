package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A garage door hanger: perforated steel angle hung from the ceiling, stacked a block at a time so
 * it reaches down from whatever height the ceiling is. It holds up a {@link BlockGarageDoorOpener}
 * hung under it, or the back end of a sectional door's ceiling track beside it.
 *
 * <p>Where it sits in its cell is chosen when it is placed: against the edge nearest where it was
 * clicked, to be bolted to the side of a track -- which runs along the edge of the door's end
 * column -- or in the middle, over an opener. A stack takes its position from the block above it.
 * The strap runs the whole height of the block; the lowest block of a stack, with no hanger or
 * opener under it, ends in a foot at the height of a sectional door's ceiling track, and the
 * highest, under the ceiling, gets a cleat along the ceiling. Both are actual state.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGarageDoorHanger extends AbstractBlock {

  /**
   * Where the strap is in its cell.
   *
   * @since 1.0
   */
  public enum Position implements IStringSerializable {
    CENTER, NORTH, EAST, SOUTH, WEST;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    static Position of(EnumFacing f) {
      switch (f) {
        case NORTH:
          return NORTH;
        case EAST:
          return EAST;
        case SOUTH:
          return SOUTH;
        case WEST:
          return WEST;
        default:
          return CENTER;
      }
    }
  }

  public static final PropertyEnum<Position> POSITION =
      PropertyEnum.create("position", Position.class);
  public static final PropertyBool TOP = PropertyBool.create("top");
  public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

  /** How near an edge a click must be to put the strap against it. */
  private static final float EDGE = 0.3F;

  /**
   * Constructs a {@link BlockGarageDoorHanger}.
   *
   * @since 1.0
   */
  public BlockGarageDoorHanger() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1F, 5F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(POSITION, Position.CENTER));
  }

  @Override
  public String getBlockRegistryName() {
    return "garage_door_hanger";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, POSITION, TOP, BOTTOM);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    Position[] all = Position.values();
    return getDefaultState().withProperty(POSITION, all[Math.min(meta, all.length - 1)]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(POSITION).ordinal();
  }

  /**
   * Under another hanger, the same position as it; placed against a side, that side; placed on a
   * ceiling or a floor, the edge nearest the click, or the middle.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState above = worldIn.getBlockState(pos.up());
    if (above.getBlock() == this) {
      return getDefaultState().withProperty(POSITION, above.getValue(POSITION));
    }
    if (facing.getAxis().isHorizontal()) {
      // Clicked on a wall: against that wall.
      return getDefaultState().withProperty(POSITION, Position.of(facing.getOpposite()));
    }
    Position p = Position.CENTER;
    float best = EDGE;
    float[] d = {hitZ, 1 - hitX, 1 - hitZ, hitX};
    Position[] edges = {Position.NORTH, Position.EAST, Position.SOUTH, Position.WEST};
    for (int i = 0; i < 4; i++) {
      if (d[i] < best) {
        best = d[i];
        p = edges[i];
      }
    }
    return getDefaultState().withProperty(POSITION, p);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    net.minecraft.block.Block below = worldIn.getBlockState(pos.down()).getBlock();
    return state.withProperty(TOP, worldIn.getBlockState(pos.up()).getBlock() != this)
        .withProperty(BOTTOM, below != this && !(below instanceof BlockGarageDoorOpener));
  }

  // --- shape ------------------------------------------------------------------------------------

  private static AxisAlignedBB box(Position p) {
    double a = 5 / 16.0;
    double b = 11 / 16.0;
    switch (p) {
      case NORTH:
        return new AxisAlignedBB(a, 0, 0, b, 1, 2 / 16.0);
      case SOUTH:
        return new AxisAlignedBB(a, 0, 14 / 16.0, b, 1, 1);
      case EAST:
        return new AxisAlignedBB(14 / 16.0, 0, a, 1, 1, b);
      case WEST:
        return new AxisAlignedBB(0, 0, a, 2 / 16.0, 1, b);
      default:
        return new AxisAlignedBB(a, 0, a, b, 1, b);
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box(state.getValue(POSITION));
  }

  /** Nothing to bump into: it hangs overhead. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
  }

  @Override
  @Nonnull
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
