package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A nurses' station counter ({@code nurse_station_counter}, {@code nurse_station_counter_monitors},
 * {@code nurse_station_corner}): a transaction ledge over a front panel facing the corridor, a
 * work surface behind it at desk height. Counters side by side join into one counter, with an
 * end panel only where the run stops; the corner turns the run round an outside corner.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the front faces, towards
 * the corridor. The ends ({@link #EAST}, {@link #WEST}, actual state) are named in model space as
 * the corridor rail's are: {@code east} is the end at the left of someone facing the counter from
 * the corridor ({@code facing.rotateY()} in the world). An end is joined when the next cell that
 * way holds any station piece. The corner draws the front along its north and west sides (model
 * space), so a counter facing the same way joins it on its east side, and a counter facing the
 * corner's {@code facing.rotateYCCW()} (west, model space) joins it on its south side.</p>
 *
 * @since 2026.10
 */
public class BlockNurseStation extends BlockHealthcare {

  /** Joined at the model's east end. */
  public static final PropertyBool EAST = PropertyBool.create("east");
  /** Joined at the model's west end. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final IProperty<?>[] ACTUAL = {EAST, WEST};

  private final boolean corner;

  /**
   * Constructs a station piece.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param corner       whether it is the outside corner (which has no ends of its own)
   */
  public BlockNurseStation(String registryName, int[] box, boolean corner) {
    super(registryName, box, HealthcareMaterial.WOOD, Mount.FLOOR, true);
    this.corner = corner;
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    // read while constructing, before {@code corner} is set: every piece has the two ends, the
    // corner simply never draws them
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(EAST, false).withProperty(WEST, false);
  }

  private static boolean station(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockNurseStation;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    if (corner) {
      return s;
    }
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(EAST, station(world, pos.offset(facing.rotateY())))
        .withProperty(WEST, station(world, pos.offset(facing.rotateYCCW())));
  }
}
