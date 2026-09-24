package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipe;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipeBook;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * The laundry's working appliances, on the kitchen's machine framework
 * ({@code furniture.appliance}), with their recipe books and specs.
 *
 * <ul>
 *   <li>The washing machine repairs armour, the way the dishwasher repairs tools: a piece of
 *   worn armour comes out of each 12 s wash a twenty-fifth less worn and stays in until it is
 *   whole, one wash of water each. The elytra is left out, as it is of the dishwasher: its
 *   repair is meant to cost leather or mending. Beside a sink, a vanity, a pedestal sink or the
 *   laundry tub ({@link IWaterTap}) it is plumbed in and never runs dry.</li>
 *   <li>The dryer dries a wet sponge in 10 s, sixteen at a time, with no water.</li>
 * </ul>
 *
 * @since 2026.9
 */
public final class LaundryAppliances {

  /** How much of a piece's durability a wash restores: one twenty-fifth. */
  private static final int REPAIR_PARTS = 25;
  /** The sponge's metadata when wet. */
  private static final int WET = 1;

  public static final ApplianceRecipeBook WASHING_MACHINE_BOOK =
      ApplianceRecipeBook.get("washing_machine").add(new ArmourWash());
  public static final ApplianceRecipeBook DRYER_BOOK = ApplianceRecipeBook.get("dryer")
      .add(ApplianceRecipe.of(stack -> stack.getItem() == Item.getItemFromBlock(Blocks.SPONGE)
          && stack.getMetadata() == WET, 1, stack -> new ItemStack(Blocks.SPONGE, 1, 0), 200));

  public static final ApplianceSpec WASHING_MACHINE = new ApplianceSpec(WASHING_MACHINE_BOOK)
      .inputLimit(1).water(4, KitchenAppliances::nextToSink)
      .runSound(FurnishingsSounds.WASHING_MACHINE_RUN, 60, 0.45F, 1.0F)
      .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 0.9F).light(1);
  public static final ApplianceSpec DRYER = new ApplianceSpec(DRYER_BOOK).inputLimit(16)
      .runSound(FurnishingsSounds.DRYER_TUMBLE, 60, 0.45F, 1.0F)
      .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 1.1F).light(3);

  private LaundryAppliances() {
  }

  /**
   * The washing machine's wash: a damaged piece of armour comes out of each wash a
   * twenty-fifth less worn, and stays in until it is whole.
   */
  private static final class ArmourWash implements ApplianceRecipe {

    @Override
    public boolean matches(@Nonnull ItemStack input) {
      return input.getCount() == 1 && input.getItem() instanceof ItemArmor
          && input.isItemStackDamageable() && input.isItemDamaged();
    }

    @Nonnull
    @Override
    public ItemStack getResult(@Nonnull ItemStack input) {
      ItemStack out = input.copy();
      int part = Math.max(1, (out.getMaxDamage() + REPAIR_PARTS - 1) / REPAIR_PARTS);
      out.setItemDamage(Math.max(0, out.getItemDamage() - part));
      return out;
    }

    @Override
    public int getTicks(@Nonnull ItemStack input) {
      return 240;
    }

    @Override
    public boolean staysInInput(@Nonnull ItemStack result) {
      return result.isItemDamaged();
    }
  }
}
