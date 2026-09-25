package com.micatechnologies.minecraft.csm.transit.stop;

import javax.annotation.Nonnull;
import net.minecraft.block.state.BlockStateContainer;

/**
 * A length of bus stop pole, one block tall. Stacked, the lengths read as one pole: each wears the
 * cap only at the top of the stack and the base only at the bottom ({@link BusStopStack}), and a
 * fitting stacked on it draws its own length of pole in this one's {@link BusStopPoleStyle}.
 *
 * <p>The facing is kept (every block of the stack is placed the same way) but does not change
 * the pole's look.</p>
 *
 * @since 2026.9
 */
public class BlockBusStopPole extends AbstractBlockBusStopStack {

  private static final double[] BOX = {6.8, 0, 6.8, 9.2, 16, 9.2};

  private final BusStopPoleStyle style;

  /**
   * Constructs a pole.
   *
   * @param registryName its registry name
   * @param style        its shape and finish
   */
  public BlockBusStopPole(String registryName, BusStopPoleStyle style) {
    super(registryName, BOX, 0.0F);
    this.style = style;
  }

  /**
   * The pole's shape and finish.
   *
   * @return its style
   */
  public BusStopPoleStyle getStyle() {
    return style;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, CAP, BASE);
  }
}
