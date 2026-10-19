package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A corridor handrail or crash rail along a wall ({@code corridor_handrail},
 * {@code corridor_crash_rail}). Rails in a row join into one rail, and turn the corners of the
 * corridor: an inside corner where two walls meet, and an outside corner round the end of a
 * wall, the rail wrapping round it. Where a run stops, the rail returns to the wall.
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5). Each end is
 * worked out from the neighbours, and named in model space, as the model faces north: {@code east}
 * is the end to the left of someone facing the rail ({@code facing.rotateY()} in the world),
 * {@code west} the one to their right.</p>
 *
 * <ul>
 *   <li>{@code join}: the next cell holds a rail on the same wall, or the rail of an inside
 *   corner turning onto this wall;</li>
 *   <li>{@code inner}: an inside corner -- the cell in front of this one holds a rail on the
 *   other wall, facing back along this one; this cell carries the turn;</li>
 *   <li>{@code outer}: an outside corner -- the cell diagonally behind, round the end of the
 *   wall, holds a rail facing that way; both rails run past their cells to meet at the
 *   corner;</li>
 *   <li>{@code end}: none of these; the rail returns to the wall.</li>
 * </ul>
 *
 * @since 2026.10
 */
public class BlockCorridorRail extends BlockHealthcare {

  /** What one end of a rail does. */
  public enum End implements IStringSerializable {
    END, JOIN, INNER, OUTER;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  public static final PropertyEnum<End> EAST = PropertyEnum.create("east", End.class);
  public static final PropertyEnum<End> WEST = PropertyEnum.create("west", End.class);

  private static final IProperty<?>[] ACTUAL = {EAST, WEST};

  /**
   * Constructs a rail.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockCorridorRail(String registryName, int[] box) {
    super(registryName, box, HealthcareMaterial.PLASTIC, Mount.WALL, true);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(EAST, End.END).withProperty(WEST, End.END);
  }

  private boolean railFacing(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockCorridorRail && s.getValue(FACING) == facing;
  }

  /** What the end towards {@code side} of a rail facing {@code facing} at {@code pos} does. */
  private End end(IBlockAccess world, BlockPos pos, EnumFacing facing, EnumFacing side) {
    BlockPos next = pos.offset(side);
    if (railFacing(world, next, facing) || railFacing(world, next, side.getOpposite())) {
      return End.JOIN;
    }
    if (railFacing(world, pos.offset(facing), side.getOpposite())) {
      return End.INNER;
    }
    if (railFacing(world, next.offset(facing.getOpposite()), side)) {
      return End.OUTER;
    }
    return End.END;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(EAST, end(world, pos, facing, facing.rotateY()))
        .withProperty(WEST, end(world, pos, facing, facing.rotateYCCW()));
  }
}
