package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenAppliances;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;

/**
 * The backyard's grills, on the kitchen's appliance framework: both cook from the oven's recipe
 * book ({@link KitchenAppliances#OVEN_BOOK}, whatever a furnace cooks into food), sizzling while
 * they cook.
 *
 * <ul>
 *   <li>The gas grill needs nothing but food, as the kitchen's electric appliances do (its
 *   bottle is assumed full), and cooks in 6 s.</li>
 *   <li>The charcoal kettle grill burns furnace fuel from its third slot, as a furnace does
 *   (charcoal, of course, or anything else that burns), and cooks in 8 s.</li>
 * </ul>
 *
 * @since 2026.9
 */
public final class OutdoorAppliances {

  public static final ApplianceSpec GAS_GRILL = new ApplianceSpec(KitchenAppliances.OVEN_BOOK)
      .timeFactor(0.75F).inputLimit(16)
      .runSound(FurnishingsSounds.GRILL_SIZZLE, 40, 0.35F, 1.0F).light(3);
  public static final ApplianceSpec CHARCOAL_GRILL = new ApplianceSpec(
      KitchenAppliances.OVEN_BOOK).inputLimit(16).fuel()
      .runSound(FurnishingsSounds.GRILL_SIZZLE, 40, 0.35F, 0.92F).light(6);

  private OutdoorAppliances() {
  }
}
