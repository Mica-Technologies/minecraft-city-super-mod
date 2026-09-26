package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A precast section stacked to a structure's height, choosing its ends from its neighbours like
 * the poles: the detention pond's outlet riser, and a manhole's riser rings. {@link #BASE} (no
 * section of the same family below: the riser's floor and orifice, the manhole's bench and
 * channel) and {@link #TOP} (none above: the riser's weir notch and trash rack, the manhole's
 * open rim) are actual state, so a stack re-forms as it is built. A manhole cone counts as a
 * manhole section, so a ring under one is not open-topped.
 *
 * <p>A manhole section is hollow: its walls collide and its middle is clear (0.75 block, room
 * for a player), and it climbs like a ladder on the steps cast into its back wall. The cutaway
 * sections are the back half, for a manhole seen from the side, and collide as that half.</p>
 *
 * @since 2026.9
 */
public class BlockStackedSection extends AbstractPrecastBlock {

  /** Nothing of this family is below: the section's floor. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** Nothing of this family is above: the section's top. */
  public static final PropertyBool TOP = PropertyBool.create("top");

  /** A manhole's walls, facing north: two sixteenths thick, the middle clear. */
  private static final AxisAlignedBB[] WALLS = {
      new AxisAlignedBB(0, 0, 0, 1, 1, 0.125), new AxisAlignedBB(0, 0, 0.875, 1, 1, 1),
      new AxisAlignedBB(0, 0, 0, 0.125, 1, 1), new AxisAlignedBB(0.875, 0, 0, 1, 1, 1)};
  /** The back half's walls, facing north. */
  private static final AxisAlignedBB[] CUT_WALLS = {
      new AxisAlignedBB(0, 0, 0.875, 1, 1, 1), new AxisAlignedBB(0, 0, 0.5, 0.125, 1, 1),
      new AxisAlignedBB(0.875, 0, 0.5, 1, 1, 1)};
  /** A manhole's bench, facing north. */
  private static final AxisAlignedBB BENCH = new AxisAlignedBB(0, 0, 0, 1, 0.25, 1);
  private static final AxisAlignedBB CUT_BENCH = new AxisAlignedBB(0, 0, 0.5, 1, 0.25, 1);
  /** The outlet riser's trash rack, over the top section. */
  private static final AxisAlignedBB RACK = new AxisAlignedBB(0, 1, 0, 1, 1.5, 1);

  private final String family;
  private final boolean manhole;
  private final boolean cutaway;

  /**
   * @param registryName its registry name
   * @param family       the stack it belongs to: sections of one family stack together
   * @param manhole      whether it is a hollow manhole section, climbed on its steps
   * @param cutaway      whether it is the back half only
   */
  public BlockStackedSection(String registryName, String family, boolean manhole,
      boolean cutaway) {
    super(registryName, new double[]{0, 0, cutaway ? 8 : 0, 16, 16, 16});
    this.family = family;
    this.manhole = manhole;
    this.cutaway = cutaway;
    if (getDefaultState().getPropertyKeys().contains(BASE)) {
      setDefaultState(getDefaultState().withProperty(BASE, true).withProperty(TOP, true));
    }
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BASE, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(BASE, !sameFamily(world, pos.down()))
        .withProperty(TOP, !sameFamily(world, pos.up()));
  }

  /** Whether a section of this one's family is at {@code at}. */
  protected boolean sameFamily(IBlockAccess world, BlockPos at) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() instanceof BlockStackedSection
        && ((BlockStackedSection) other.getBlock()).family.equals(family);
  }

  protected boolean isManhole() {
    return manhole;
  }

  protected boolean isCutaway() {
    return cutaway;
  }

  /** Whether this section has the bench under it; only a riser ring can. */
  protected boolean hasBench(IBlockState state, IBlockAccess world, BlockPos pos) {
    return !sameFamily(world, pos.down());
  }

  @Override
  public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos,
      EntityLivingBase entity) {
    return manhole;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    if (!manhole) {
      addCollisionBoxToList(pos, entityBox, boxes, FULL_BLOCK_AABB);
      if (!sameFamily(world, pos.up())) {
        addCollisionBoxToList(pos, entityBox, boxes, RACK);
      }
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB wall : cutaway ? CUT_WALLS : WALLS) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(wall, facing));
    }
    if (hasBench(state, world, pos)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(cutaway ? CUT_BENCH : BENCH, facing));
    }
  }
}
