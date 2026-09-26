package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A fixed steel ladder with a safety cage, hung on whatever the player was looking at (a tower
 * leg, a tank, a wall) and climbed like a vanilla ladder. Stacked, it picks its pieces from
 * its neighbours, all actual state: feet on the lowest block ({@link #BOTTOM}), the cage from
 * the third block up ({@link #CAGE}, as a real cage starts clear of the ground), and the rails
 * carried on up past the top as grab rails ({@link #TOP}).
 *
 * <p>Only the rails collide, so a player can step into the cage to climb. A balcony's handrail
 * leaves a gap where a caged ladder comes up beside it ({@link TankUnits#addCollision}).</p>
 *
 * @since 2026.9
 */
public class BlockCagedLadder extends BlockUtilityFixture {

  public static final PropertyBool BOTTOM = PropertyBool.create("bottom");
  public static final PropertyBool CAGE = PropertyBool.create("cage");
  public static final PropertyBool TOP = PropertyBool.create("top");

  /** The rails and rungs, facing north: what collides. */
  private static final AxisAlignedBB RAILS = new AxisAlignedBB(0.2, 0, 0.8, 0.8, 1, 1);

  public BlockCagedLadder(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(BOTTOM, true).withProperty(CAGE, false)
        .withProperty(TOP, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BOTTOM, CAGE, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    boolean below = same(world, pos.down(), state);
    boolean below2 = below && same(world, pos.down(2), state);
    return state.withProperty(BOTTOM, !below).withProperty(CAGE, below2)
        .withProperty(TOP, !same(world, pos.up(), state));
  }

  private boolean same(IBlockAccess world, BlockPos at, IBlockState state) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == state.getValue(FACING);
  }

  @Override
  public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos,
      EntityLivingBase entity) {
    return true;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return RotationUtils.rotateBoundingBoxByFacing(RAILS, state.getValue(FACING));
  }
}
