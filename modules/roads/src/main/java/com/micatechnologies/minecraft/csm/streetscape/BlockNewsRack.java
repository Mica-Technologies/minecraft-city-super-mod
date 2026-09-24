package com.micatechnologies.minecraft.csm.streetscape;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A newspaper rack. Racks set side by side facing the same way form one bank: the plinth and
 * top rail run on through the joint, and only the two racks at the ends of the bank carry an
 * end panel.
 *
 * <p>Which sides are joined is worked out from the neighbours each time the block is drawn
 * ({@link #getActualState}); nothing about it is stored, so a bank re-forms by itself when a
 * rack is added or taken away. {@link #LEFT} and {@link #RIGHT} are as the rack is seen from
 * the front.</p>
 *
 * @version 1.0
 */
public class BlockNewsRack extends BlockUtilityBox {

  /** Whether the rack to the left, seen from the front, is part of the same bank. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** Whether the rack to the right, seen from the front, is part of the same bank. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  public BlockNewsRack(String registryName, UtilityBoxSpec spec) {
    super(registryName, spec);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LEFT, false).withProperty(RIGHT, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    EnumFacing facing = state.getValue(FACING);
    // Facing north (towards a viewer standing north), the viewer's left is east.
    EnumFacing left = facing.rotateY();
    return actual.withProperty(LEFT, joins(world, pos.offset(left), facing))
        .withProperty(RIGHT, joins(world, pos.offset(left.getOpposite()), facing));
  }

  private static boolean joins(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() instanceof BlockNewsRack && other.getValue(FACING) == facing;
  }
}
