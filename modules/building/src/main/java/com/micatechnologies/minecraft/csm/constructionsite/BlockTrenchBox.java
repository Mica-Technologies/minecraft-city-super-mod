package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A trench box: two steel side panels held apart by spreader pipes, set into a trench so it can be
 * worked in without its walls caving in.
 *
 * <p>Runs along the placer's line of sight, the way you look down a trench. Boxes laid end to end
 * are one box, stacked they are one taller box -- the top rail and lifting lugs are drawn only on
 * the top course ({@link #UP}) -- and side by side they are one wider box: a block drops its panel
 * on a side where another box runs the same way ({@link #SIDE_A}, {@link #SIDE_B}), and its
 * spreader pipe runs on through to the far panel. So a box is built as wide as the trench, with no
 * limit. Only the panels collide, so a player can walk the trench between them; the spreaders are
 * overhead of anyone in a real one. All of this is actual state; only the axis is stored. The
 * models come from {@code dev-env-utils/scripts/gen_earthworks.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockTrenchBox extends AbstractBlockSiteAxial {

  /** Another box is stacked on this one. Actual state only. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /**
   * A panel on the model's north side (the world's north for a box along x, its east for one along
   * z) and on its south side: false where another box running the same way joins it there.
   * Actual state only.
   */
  public static final PropertyBool SIDE_A = PropertyBool.create("side_a");
  public static final PropertyBool SIDE_B = PropertyBool.create("side_b");

  /** The whole box, running along x. */
  private static final AxisAlignedBB RUN_BOX = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

  /** The two side panels, running along x. */
  private static final AxisAlignedBB[] PANELS_X = {
      BlockSiteProp.box16(0, 0, 0, 16, 16, 1.5),
      BlockSiteProp.box16(0, 0, 14.5, 16, 16, 16)};
  private static final AxisAlignedBB[] PANELS_Z = {
      BlockSiteProp.box16(0, 0, 0, 1.5, 16, 16),
      BlockSiteProp.box16(14.5, 0, 0, 16, 16, 16)};

  /**
   * Constructs a {@link BlockTrenchBox}.
   *
   * @since 1.0
   */
  public BlockTrenchBox() {
    super(Material.IRON, SoundType.METAL, "pickaxe");
  }

  @Override
  public String getBlockRegistryName() {
    return "trench_box";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS, UP, SIDE_A, SIDE_B);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    boolean alongX = state.getValue(AXIS) == EnumFacing.Axis.X;
    BlockPos a = alongX ? pos.north() : pos.east();
    BlockPos b = alongX ? pos.south() : pos.west();
    return state.withProperty(UP, worldIn.getBlockState(pos.up()).getBlock() == this)
        .withProperty(SIDE_A, !joins(worldIn, a, state))
        .withProperty(SIDE_B, !joins(worldIn, b, state));
  }

  private boolean joins(IBlockAccess world, BlockPos pos, IBlockState state) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(AXIS) == state.getValue(AXIS);
  }

  @Override
  protected AxisAlignedBB getRunBox() {
    return RUN_BOX;
  }

  @Override
  protected boolean runsAlongLook() {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    IBlockState a = isActualState ? state : state.getActualState(worldIn, pos);
    AxisAlignedBB[] panels = a.getValue(AXIS) == EnumFacing.Axis.X ? PANELS_X : PANELS_Z;
    // PANELS_Z lists the west panel first; side A of a box along z is its east.
    boolean alongX = a.getValue(AXIS) == EnumFacing.Axis.X;
    if (a.getValue(alongX ? SIDE_A : SIDE_B)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, panels[0]);
    }
    if (a.getValue(alongX ? SIDE_B : SIDE_A)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, panels[1]);
    }
  }
}
