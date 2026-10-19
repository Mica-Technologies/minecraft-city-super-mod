package com.micatechnologies.minecraft.csm.parks.planters;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;

/**
 * A block a plant can be planted in: the plantable planters, the raised beds and the troughs.
 * Each holds at most one plant in a {@link TileEntityPlanter} (data only, no renderer, no
 * ticking), and its baked model draws the plant's own baked model on its soil
 * ({@link PlanterBakedModel}). What a block does with a click, a break and its extended state
 * is the same for every planter and lives in {@link Planters}.
 *
 * @since 2026.10
 */
public interface ICsmPlanter {

  /**
   * Where this planter's soil is in the given state.
   *
   * @param state the planter's state
   * @return its soil, or null if nothing can be planted in it in this state (a planter lying on
   *     its side)
   */
  @Nullable
  PlanterSoil getSoil(IBlockState state);
}
