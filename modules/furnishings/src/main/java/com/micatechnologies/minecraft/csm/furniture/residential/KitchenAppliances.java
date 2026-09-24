package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipe;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipeBook;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The kitchen's working appliances: their recipe books and their specs, which the tab's lines
 * hand to each block.
 *
 * <ul>
 *   <li>The oven (the range's and the wall oven's) cooks whatever a furnace would cook into
 *   food, a little faster than a furnace (8 s against 10). The microwave uses the same book,
 *   one item at a time, in 3 s, and beeps when it is done.</li>
 *   <li>The air fryer cooks raw food (meat, fish, potatoes) in 4 s.</li>
 *   <li>The toaster makes toast from bread in 5 s, and pops.</li>
 *   <li>The blender makes a smoothie from an apple or two melon slices, in 3 s.</li>
 *   <li>The coffee machine makes coffee from cocoa beans, with a cup of water each, in 4 s.</li>
 *   <li>The dishwasher repairs a tool or weapon (not armour: that is the washing machine's), a
 *   twenty-fifth of its durability a 12 s wash, one wash of water each, until it is whole.</li>
 * </ul>
 *
 * <p>The machines that use water hold a tank a water bucket fills (and a bottle adds a cup to);
 * standing beside a kitchen sink base they are plumbed in and never run dry.</p>
 *
 * @since 2026.9
 */
public final class KitchenAppliances {

  /** What the toaster makes. */
  public static final String TOAST = "toast";
  /** What the blender makes. */
  public static final String SMOOTHIE = "smoothie";
  /** What the coffee machine makes. */
  public static final String COFFEE = "coffee";

  /** How much of a tool's durability a wash restores: one twenty-fifth. */
  private static final int REPAIR_PARTS = 25;

  public static final ApplianceRecipeBook OVEN_BOOK = ApplianceRecipeBook.get("oven")
      .add(ApplianceRecipe.of(KitchenAppliances::cooksToFood, 1,
          stack -> FurnaceRecipes.instance().getSmeltingResult(stack).copy(), 160));
  public static final ApplianceRecipeBook AIR_FRYER_BOOK = ApplianceRecipeBook.get("air_fryer")
      .add(ApplianceRecipe.of(stack -> stack.getItem() instanceof ItemFood && cooksToFood(stack),
          1, stack -> FurnaceRecipes.instance().getSmeltingResult(stack).copy(), 80));
  public static final ApplianceRecipeBook TOASTER_BOOK = ApplianceRecipeBook.get("toaster")
      .add(ApplianceRecipe.of(stack -> stack.getItem() == Items.BREAD, 1,
          stack -> ItemResidentialFood.stack(TOAST, 1), 100));
  public static final ApplianceRecipeBook BLENDER_BOOK = ApplianceRecipeBook.get("blender")
      .add(ApplianceRecipe.of(stack -> stack.getItem() == Items.APPLE, 1,
          stack -> ItemResidentialFood.stack(SMOOTHIE, 1), 60))
      .add(ApplianceRecipe.of(stack -> stack.getItem() == Items.MELON, 2,
          stack -> ItemResidentialFood.stack(SMOOTHIE, 1), 60));
  public static final ApplianceRecipeBook COFFEE_BOOK = ApplianceRecipeBook.get("coffee_machine")
      .add(ApplianceRecipe.of(KitchenAppliances::isCocoa, 1,
          stack -> ItemResidentialFood.stack(COFFEE, 1), 80));
  public static final ApplianceRecipeBook DISHWASHER_BOOK = ApplianceRecipeBook.get("dishwasher")
      .add(new ToolWash());

  public static final ApplianceSpec OVEN = new ApplianceSpec(OVEN_BOOK)
      .doneSound(FurnishingsSounds.OVEN_TIMER, 1.0F).light(8);
  public static final ApplianceSpec MICROWAVE = new ApplianceSpec(OVEN_BOOK).timeFactor(0.375F)
      .inputLimit(1).doneSound(FurnishingsSounds.APPLIANCE_BEEP, 1.0F).light(6);
  public static final ApplianceSpec AIR_FRYER = new ApplianceSpec(AIR_FRYER_BOOK).inputLimit(8)
      .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 1.25F).light(3);
  public static final ApplianceSpec TOASTER = new ApplianceSpec(TOASTER_BOOK).inputLimit(2)
      .doneSound(FurnishingsSounds.TOASTER_POP, 1.0F).light(4);
  public static final ApplianceSpec BLENDER = new ApplianceSpec(BLENDER_BOOK).inputLimit(16)
      .runSound(FurnishingsSounds.BLENDER_WHIRR, 30, 0.5F, 1.0F);
  public static final ApplianceSpec COFFEE_MACHINE = new ApplianceSpec(COFFEE_BOOK)
      .inputLimit(16).water(8, KitchenAppliances::nextToSink)
      .runSound(FurnishingsSounds.COFFEE_GURGLE, 80, 0.5F, 1.0F).light(2);
  public static final ApplianceSpec DISHWASHER = new ApplianceSpec(DISHWASHER_BOOK)
      .inputLimit(1).water(4, KitchenAppliances::nextToSink)
      .runSound(FurnishingsSounds.DISHWASHER_HUM, 60, 0.35F, 1.0F)
      .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 0.8F).light(1);

  private KitchenAppliances() {
  }

  /** Whether a furnace cooks {@code stack} into food. */
  private static boolean cooksToFood(ItemStack stack) {
    return FurnaceRecipes.instance().getSmeltingResult(stack).getItem() instanceof ItemFood;
  }

  private static boolean isCocoa(ItemStack stack) {
    return stack.getItem() == Items.DYE
        && stack.getMetadata() == EnumDyeColor.BROWN.getDyeDamage();
  }

  /**
   * Whether the appliance at {@code pos} stands beside a block with a tap (a kitchen sink base,
   * a bathroom vanity, a pedestal sink, a laundry tub: an {@link IWaterTap}), and so is plumbed.
   *
   * @param world the world
   * @param pos   the appliance
   *
   * @return true if it is
   */
  public static boolean nextToSink(World world, BlockPos pos) {
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      if (world.getBlockState(pos.offset(side)).getBlock() instanceof IWaterTap) {
        return true;
      }
    }
    return false;
  }

  /**
   * The dishwasher's wash: a damaged tool or weapon (anything that wears but armour and the
   * elytra) comes out of each wash a twenty-fifth less worn, and stays in until it is whole.
   */
  private static final class ToolWash implements ApplianceRecipe {

    @Override
    public boolean matches(@Nonnull ItemStack input) {
      Item item = input.getItem();
      return input.getCount() == 1 && input.isItemStackDamageable() && input.isItemDamaged()
          && !(item instanceof ItemArmor) && !(item instanceof ItemElytra);
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
