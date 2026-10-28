package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Input;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Recipe;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Result;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Source;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

/**
 * What a pawn shop pays: every item's value, worked out once when the server starts from the
 * values file ({@link PawnValues}) and every crafting, smelting and Fabricator recipe, then a
 * quote for any stack from it. Server side only.
 *
 * @since 2026.10
 */
public final class PawnValuation {

  /** What a stack would fetch, or why it would not be taken. */
  public static final class Quote {

    /** Why the stack is refused (a lang key), or null if it is taken. */
    @Nullable
    public final String refusal;
    /** The value of one item, adjustments included. */
    public final double valueEach;
    /** What the whole stack pays: value x count x the payout fraction, in cents. */
    public final double payout;
    /** How the base value was found. */
    @Nullable
    public final Source source;

    Quote(@Nullable String refusal, double valueEach, double payout, @Nullable Source source) {
      this.refusal = refusal;
      this.valueEach = valueEach;
      this.payout = payout;
      this.source = source;
    }

    static Quote refused(String why) {
      return new Quote(why, 0, 0, null);
    }

    public boolean isAccepted() {
      return refusal == null;
    }
  }

  private static File file;
  private static PawnValues values = PawnValues.defaults();
  private static Result result;

  private PawnValuation() {
  }

  /** Where the values file lives: {@code config/csm/pawn_values.json}. */
  public static void setConfigDirectory(File configDirectory) {
    file = new File(new File(configDirectory, "csm"), PawnValues.FILE_NAME);
  }

  /**
   * Reads the values file and solves every value. Called when the server has started, by which
   * time every mod's recipes are registered, and by {@code /csmpawn reload}.
   *
   * @return how long it took, in milliseconds
   */
  public static synchronized long rebuild() {
    long t0 = System.nanoTime();
    if (file != null) {
      values = PawnValues.load(file);
    }
    List<Recipe> recipes = collectRecipes();
    result = PawnValueSolver.solve(values.fixed(), recipes,
        CsmConfig.getPawnDefaultValue());
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    CsmFurnishings.getLogger().info(
        "Pawn shop: valued {} items from {} recipes in {} passes, {} ms", result.keys().size(),
        recipes.size(), result.getPasses(), ms);
    return ms;
  }

  /** Drops the solved values (server stopping). */
  public static synchronized void clear() {
    result = null;
  }

  static synchronized Result result() {
    if (result == null) {
      rebuild();
    }
    return result;
  }

  static PawnValues values() {
    return values;
  }

  /** Sets (or with null clears) an override, saves the file and solves again. */
  static synchronized void setOverride(String key, @Nullable Double value) {
    values.setOverride(key, value);
    save();
    rebuild();
  }

  /** Blocks or unblocks an item and saves the file; returns whether it is now blocked. */
  static synchronized boolean toggleBlocked(String key) {
    boolean now = values.toggleBlocked(key);
    save();
    return now;
  }

  private static void save() {
    if (file != null) {
      values.save(file);
    }
  }

  /** The base value of one item of {@code key}, or NaN; the default for an unknown item. */
  static double baseValue(String key) {
    double v = result().value(key);
    if (Double.isNaN(v)) {
      double d = CsmConfig.getPawnDefaultValue();
      return d > 0 ? d : Double.NaN;
    }
    return v;
  }

  /** How {@code key}'s base value was found. */
  static Source baseSource(String key) {
    Source s = result().source(key);
    return s != null ? s : (CsmConfig.getPawnDefaultValue() > 0 ? Source.DEFAULT : null);
  }

  /**
   * What a stack fetches.
   *
   * @param stack the stack offered; its count is how many
   */
  public static Quote quote(ItemStack stack) {
    if (stack.isEmpty()) {
      return Quote.refused("csm.pawn.refuse.nothing");
    }
    String key = key(stack);
    if (values.isBlocked(key)) {
      return Quote.refused("csm.pawn.refuse.blocked");
    }
    if (holdsItems(stack)) {
      return Quote.refused("csm.pawn.refuse.full");
    }
    double base = baseValue(key);
    if (Double.isNaN(base) || base <= 0) {
      return Quote.refused("csm.pawn.refuse.worthless");
    }
    double each = base;
    if (stack.isItemStackDamageable() && stack.getMaxDamage() > 0) {
      each *= (double) (stack.getMaxDamage() - stack.getItemDamage()) / stack.getMaxDamage();
    }
    int levels = 0;
    for (int level : EnchantmentHelper.getEnchantments(stack).values()) {
      levels += level;
    }
    each += levels * CsmConfig.getPawnEnchantValuePerLevel();
    double payout = cents(each * stack.getCount() * CsmConfig.getPawnPayoutFraction());
    if (payout <= 0) {
      return Quote.refused("csm.pawn.refuse.worthless");
    }
    return new Quote(null, each, payout, baseSource(key));
  }

  /** Rounds down to a cent, so a quote never pays more than its value. */
  static double cents(double amount) {
    return Math.floor(amount * 100 + 1e-6) / 100.0;
  }

  /** Whether the stack carries items inside it (a filled shulker box, a backpack). */
  private static boolean holdsItems(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null && tag.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) {
      NBTTagCompound te = tag.getCompoundTag("BlockEntityTag");
      if (te.hasKey("Items", Constants.NBT.TAG_LIST)
          && !te.getTagList("Items", Constants.NBT.TAG_COMPOUND).isEmpty()) {
        return true;
      }
    }
    if (stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
      IItemHandler handler =
          stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
      if (handler != null) {
        for (int i = 0; i < handler.getSlots(); i++) {
          if (!handler.getStackInSlot(i).isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * A stack's key: {@code modid:item@meta}, with a damageable item's damage left out (it is
   * durability, not a subtype) and a wildcard subtype written as {@code modid:item}.
   */
  public static String key(ItemStack stack) {
    ResourceLocation id = stack.getItem().getRegistryName();
    String name = id == null ? "unknown" : id.toString();
    int meta = stack.getMetadata();
    if (meta == OreDictionary.WILDCARD_VALUE) {
      return name;
    }
    if (stack.getItem().isDamageable()) {
      meta = 0;
    }
    return name + "@" + meta;
  }

  private static List<Recipe> collectRecipes() {
    List<Recipe> recipes = new ArrayList<>();
    int skipped = 0;
    for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
      try {
        Recipe r = crafting(recipe);
        if (r != null) {
          recipes.add(r);
        }
      } catch (RuntimeException e) {
        // Another mod's recipe that cannot answer outside a crafting grid.
        skipped++;
      }
    }
    double fuel = CsmConfig.getPawnSmeltFuelCost();
    for (Map.Entry<ItemStack, ItemStack> e :
        FurnaceRecipes.instance().getSmeltingList().entrySet()) {
      ItemStack in = e.getKey();
      ItemStack out = e.getValue();
      if (in.isEmpty() || out.isEmpty()) {
        continue;
      }
      recipes.add(new Recipe(key(out), out.getCount(),
          new Input[]{new Input(key(in), 1)}, fuel, "smelting"));
    }
    // Made in the world rather than at a bench, or by a recipe with no fixed output.
    for (EnumDyeColor colour : EnumDyeColor.values()) {
      int m = colour.getMetadata();
      recipes.add(new Recipe("minecraft:concrete@" + m, 1,
          new Input[]{new Input("minecraft:concrete_powder@" + m, 1)}, 0, "in water"));
      Item box = Item.getItemFromBlock(BlockShulkerBox.getBlockByColor(colour));
      if (box != Items.AIR && colour != EnumDyeColor.PURPLE) {
        recipes.add(new Recipe(key(new ItemStack(box)), 1, new Input[]{
            new Input("minecraft:purple_shulker_box@0", 1),
            new Input("minecraft:dye@" + colour.getDyeDamage(), 1)}, 0, "dyeing"));
      }
    }
    for (Block block : CsmRegistry.getBlocks()) {
      Recipe r = fabricator(block);
      if (r != null) {
        recipes.add(r);
      }
    }
    if (skipped > 0) {
      CsmFurnishings.getLogger().debug("Pawn shop: {} recipes could not be read", skipped);
    }
    return recipes;
  }

  @Nullable
  private static Recipe crafting(IRecipe recipe) {
    ItemStack out = recipe.getRecipeOutput();
    if (out.isEmpty()) {
      return null;
    }
    List<Input> inputs = new ArrayList<>();
    for (Ingredient ingredient : recipe.getIngredients()) {
      if (ingredient == Ingredient.EMPTY) {
        continue;
      }
      ItemStack[] matching = ingredient.getMatchingStacks();
      if (matching.length == 0) {
        continue;
      }
      String[] keys = new String[matching.length];
      String[] refunds = new String[matching.length];
      for (int i = 0; i < matching.length; i++) {
        keys[i] = key(matching[i]);
        Item item = matching[i].getItem();
        if (item.hasContainerItem(matching[i])) {
          ItemStack back = item.getContainerItem(matching[i]);
          if (!back.isEmpty()) {
            refunds[i] = key(back);
          }
        }
      }
      inputs.add(new Input(keys, refunds, 1));
    }
    if (inputs.isEmpty()) {
      return null;
    }
    return new Recipe(key(out), out.getCount(), inputs.toArray(new Input[0]), 0, "crafting");
  }

  @Nullable
  private static Recipe fabricator(Block block) {
    List<FabricatorIngredient> cost = CsmFabricatorCosts.getCost(block);
    if (cost == null || cost.isEmpty()) {
      return null;
    }
    Item item = Item.getItemFromBlock(block);
    if (item == Items.AIR) {
      return null;
    }
    Input[] inputs = new Input[cost.size()];
    for (int i = 0; i < inputs.length; i++) {
      FabricatorIngredient f = cost.get(i);
      String k = f.getMetadata() == OreDictionary.WILDCARD_VALUE ? f.getItemId()
          : f.getItemId() + "@" + f.getMetadata();
      inputs[i] = new Input(k, f.getCount());
    }
    return new Recipe(key(new ItemStack(item)), 1, inputs, 0, "Fabricator");
  }
}
