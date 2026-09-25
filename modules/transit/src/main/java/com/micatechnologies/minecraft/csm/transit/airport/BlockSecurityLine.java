package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * A piece of a security lane: the X-ray unit, the roller conveyor and the divesting table, whose
 * tops are all at the same height ({@code LANE_Y} in {@code gen_transit_airport.py}, 12
 * sixteenths) so a lane of them reads as one. Pieces side by side along the lane join whichever
 * way each faces, as long as the lane runs the same way: {@link #LEFT} and {@link #RIGHT} (actual
 * state, the model's west and east) say whether the lane carries on there, and the rollers and
 * table draw their end plates only where it stops. Security trays set on a lane piece drop onto
 * its top ({@link BlockSecurityTray}).
 *
 * @since 2026.9
 */
public class BlockSecurityLine extends BlockPlatformFixture {

  /** The lane carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The lane carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  private final AxisAlignedBB collision;

  /**
   * Constructs a lane piece.
   *
   * @param registryName its registry name
   * @param box          its selection box facing north, in sixteenths
   * @param height       how high it collides, in sixteenths, when that is above its box (the
   *                     X-ray unit's housing stands above its block); 0 to collide with its box
   */
  public BlockSecurityLine(String registryName, double[] box, double height) {
    super(registryName, box);
    this.collision = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0,
        box[3] / 16.0, Math.max(box[4], height) / 16.0, box[5] / 16.0);
    setDefaultState(getDefaultState().withProperty(LEFT, false).withProperty(RIGHT, false));
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
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(LEFT, continues(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(RIGHT, continues(world, pos.offset(facing.rotateY()), facing));
  }

  /** Whether a lane piece running the same way is at {@code at}. */
  private static boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() instanceof BlockSecurityLine
        && other.getValue(FACING).getAxis() == facing.getAxis();
  }

  /**
   * Whether a tray can be set on its top: the rollers and the table, not the X-ray unit, whose
   * housing stands over its belt.
   *
   * @return whether its top is open
   */
  public boolean hasOpenTop() {
    return collision.maxY <= 1.0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        RotationUtils.rotateBoundingBoxByFacing(collision, state.getValue(FACING)));
  }
}
