package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;

/**
 * The ticket booth ({@code ticket_booth}): a small freestanding booth for a field's gate, two
 * blocks square and three tall. A window on the front with a speaking grille and a deal tray
 * over a counter ledge, windows on both sides, a door at the back, a lit TICKETS band under a
 * roof with an overhang. A {@link BlockVenueUnit} drawn whole (one OBJ) by its master, the
 * front cell on the customer's right; the booth reaches a block to the customer's left and a
 * block back from it.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way its window faces.</p>
 *
 * @since 2026.10
 */
public class BlockTicketBooth extends BlockVenueUnit {

  /**
   * Constructs the booth.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockTicketBooth(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return 7;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}
