package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A wide-flange steel beam: the same section laid down, flanges top and bottom.
 *
 * <p>Spans the block, so a row of them makes a girder line.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelBeam extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0D, 3 / 16.0D, 0 / 16.0D,
          12 / 16.0D, 13 / 16.0D, 16 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelBeam}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelBeam() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  /**
   * The beam stops over a column at its model's north end (east when it spans east-west): no beam
   * carries on that way and a column stands under it, so it ends at the column's far face with an
   * end plate rather than running on to the edge of its block, half a block past the column.
   */
  public static final PropertyBool CUT_NORTH = PropertyBool.create("cut_north");
  /** The same at the model's south end (west when it spans east-west). */
  public static final PropertyBool CUT_SOUTH = PropertyBool.create("cut_south");

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, CUT_NORTH, CUT_SOUTH);
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing.Axis axis = state.getValue(AXIS);
    // the blockstate turns the model a quarter (y 90) for an east-west beam: its north is east
    EnumFacing north = axis == EnumFacing.Axis.Z ? EnumFacing.NORTH : EnumFacing.EAST;
    boolean column = isColumn(world.getBlockState(pos.down()));
    return state
        .withProperty(CUT_NORTH, column && !continues(world, pos.offset(north), axis))
        .withProperty(CUT_SOUTH, column && !continues(world, pos.offset(north.getOpposite()),
            axis));
  }

  private static boolean isColumn(IBlockState state) {
    return state.getBlock() instanceof AbstractBlockSteelColumn
        || state.getBlock() instanceof AbstractBlockSteelConnection;
  }

  private static boolean continues(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis) {
    IBlockState next = world.getBlockState(pos);
    return next.getBlock() instanceof AbstractBlockSteelBeam && next.getValue(AXIS) == axis;
  }
}
