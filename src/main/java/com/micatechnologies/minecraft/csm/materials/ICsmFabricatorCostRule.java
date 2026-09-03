package com.micatechnologies.minecraft.csm.materials;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * A subsystem's own pricing rule for the blocks in one creative tab, asked by
 * {@link CsmFabricatorCosts} once the tab-independent rules (structural members, road signs,
 * building materials, mounting hardware, optical devices) have all declined.
 *
 * <p>This is what keeps the equipment branches out of Core. Pricing fire alarm appliances means
 * knowing that a detector is not a pull station, and pricing signal equipment means knowing a
 * sensor from a controller cabinet from a signal head; both are facts about a subsystem's own
 * class hierarchy, so the rule lives in the subsystem and Core never imports it.
 *
 * <p>Rules are registered with
 * {@link CsmFabricatorCosts#registerRule(String, ICsmFabricatorCostRule)} from the owning
 * module's pre-initialization. That is early enough: costs are first read at
 * post-initialization, for the "Fabricator coverage" log line, and thereafter only when a
 * Fabricator GUI is opened — both long after every mod's pre-initialization has run.
 *
 * @see CsmFabricatorCosts
 * @since 2026.9
 */
@FunctionalInterface
public interface ICsmFabricatorCostRule {

  /**
   * Returns the ingredients needed to fabricate one of the given block, or {@code null} if this
   * rule has no specific price for it.
   *
   * <p>{@code null} means "no opinion", not "not fabricable": the block then falls through to the
   * generic equipment cost that {@link CsmFabricatorCosts} applies to any block in a tab whose
   * rule is absent or silent. A rule must therefore return {@code null} exactly where it wants
   * that generic cost, and never as a way of hiding a block from the Fabricator.
   *
   * @param block        the block to price
   * @param registryName the block's registry name, for display-name lookups through
   *                     {@link CsmBlockDisplayNames}. Nothing may be inferred from the id itself —
   *                     see the {@link CsmFabricatorCosts} class documentation.
   *
   * @return an immutable ingredient list, or {@code null} to take the generic equipment cost
   *
   * @since 2026.9
   */
  @Nullable
  List<FabricatorIngredient> price(Block block, String registryName);
}
