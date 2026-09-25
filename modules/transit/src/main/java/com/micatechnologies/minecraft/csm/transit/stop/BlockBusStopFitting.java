package com.micatechnologies.minecraft.csm.transit.stop;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A length of bus stop pole with something clamped to it: a timetable or route map case, and the
 * base of the flag sign and the arrival display. It draws its length of pole in the style of the
 * pole it is stacked on ({@link #POLE}, from {@link BusStopStack#styleAt}), so a fitting needs no
 * variant per pole: the same timetable case sits on a galvanized round pole or a red square one.
 *
 * <p>Placed on the top of a pole it continues the pole; the fitting faces the player who placed
 * it, like a sign.</p>
 *
 * @since 2026.9
 */
public class BlockBusStopFitting extends AbstractBlockBusStopStack {

  /** The style of pole this fitting's length of pole is drawn in. Actual state only. */
  public static final PropertyEnum<BusStopPoleStyle> POLE =
      PropertyEnum.create("pole", BusStopPoleStyle.class);

  /**
   * Constructs a fitting.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths, pole and fitting together
   */
  public BlockBusStopFitting(String registryName, double[] box) {
    this(registryName, box, 0.0F);
  }

  /**
   * Constructs a fitting that gives light.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths, pole and fitting together
   * @param lightLevel   the light it gives, 0 to 1
   */
  protected BlockBusStopFitting(String registryName, double[] box, float lightLevel) {
    super(registryName, box, lightLevel);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POLE, CAP, BASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(POLE, BusStopStack.styleAt(world, pos));
  }
}
