package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipe;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipeBook;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.furniture.residential.ItemResidentialFood;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenAppliances;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.init.Items;

/**
 * The Market &amp; Store tab's working appliances, on the kitchen's machine framework
 * ({@code furniture.appliance}); the tab's lines hand each block its spec.
 *
 * <ul>
 *   <li>The rotisserie oven roasts raw meat and fish, eight at a time, a little slower than the
 *   air fryer it shares its recipes with (a spit turns slowly), and chimes the oven's timer when
 *   a piece is done. Its window is lit while it roasts.</li>
 *   <li>The commercial coffee brewer makes coffee from cocoa beans as the kitchen's coffee
 *   machine does, but takes 32 at a time and its tank lasts sixteen cups.</li>
 *   <li>The fountain drink machine pours a {@link #FOUNTAIN_DRINK} from sugar, with a cup of
 *   water each, in 2 s; its tank lasts 32 drinks.</li>
 * </ul>
 *
 * <p>The brewer and the fountain stand beside a sink to be plumbed in, as the kitchen's machines
 * do; otherwise a water bucket fills them.</p>
 *
 * @since 2026.9
 */
public final class StoreAppliances {

  /** What the fountain drink machine pours. */
  public static final String FOUNTAIN_DRINK = "fountain_drink";

  public static final ApplianceRecipeBook FOUNTAIN_BOOK = ApplianceRecipeBook.get("fountain")
      .add(ApplianceRecipe.of(stack -> stack.getItem() == Items.SUGAR, 1,
          stack -> ItemResidentialFood.stack(FOUNTAIN_DRINK, 1), 40));

  public static final ApplianceSpec ROTISSERIE = new ApplianceSpec(
      KitchenAppliances.AIR_FRYER_BOOK).timeFactor(1.5F).inputLimit(8)
      .doneSound(FurnishingsSounds.OVEN_TIMER, 1.1F).light(10);
  public static final ApplianceSpec COFFEE_BREWER = new ApplianceSpec(
      KitchenAppliances.COFFEE_BOOK).inputLimit(32).water(16, KitchenAppliances::nextToSink)
      .runSound(FurnishingsSounds.COFFEE_GURGLE, 80, 0.5F, 0.9F).light(2);
  public static final ApplianceSpec FOUNTAIN = new ApplianceSpec(FOUNTAIN_BOOK).inputLimit(64)
      .water(32, KitchenAppliances::nextToSink)
      .runSound(FurnishingsSounds.COFFEE_GURGLE, 40, 0.4F, 1.6F).light(6);

  private StoreAppliances() {
  }
}
