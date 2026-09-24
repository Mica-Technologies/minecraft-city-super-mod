package com.micatechnologies.minecraft.csm.furniture.appliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;

/**
 * The recipes of one type of appliance ("oven", "toaster", "dishwasher", ...), found by name so
 * that a later phase, or another module's code, adds to a book without owning it. The first
 * recipe that {@link ApplianceRecipe#matches matches} an input wins, in the order they were
 * added.
 *
 * <p>Books are filled while the game starts (the block constructors and pre-initialisation);
 * reading one afterwards from the server thread needs no locking.</p>
 *
 * @since 2026.9
 */
public final class ApplianceRecipeBook {

  private static final Map<String, ApplianceRecipeBook> BOOKS = new LinkedHashMap<>();

  private final String id;
  private final List<ApplianceRecipe> recipes = new ArrayList<>();

  private ApplianceRecipeBook(String id) {
    this.id = id;
  }

  /**
   * The book named {@code id}, made empty the first time it is asked for.
   *
   * @param id the machine type, e.g. {@code "oven"}
   *
   * @return the book
   */
  public static synchronized ApplianceRecipeBook get(String id) {
    return BOOKS.computeIfAbsent(id, ApplianceRecipeBook::new);
  }

  /**
   * Every book, by name, for listing (a guide page, a JEI plugin).
   *
   * @return the books
   */
  public static synchronized Map<String, ApplianceRecipeBook> all() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(BOOKS));
  }

  /**
   * The book's name.
   *
   * @return its id
   */
  public String getId() {
    return id;
  }

  /**
   * Adds a recipe after those already in the book.
   *
   * @param recipe the recipe
   *
   * @return this book, to chain adds
   */
  public ApplianceRecipeBook add(ApplianceRecipe recipe) {
    recipes.add(recipe);
    return this;
  }

  /**
   * The recipes, in order.
   *
   * @return the recipes
   */
  public List<ApplianceRecipe> getRecipes() {
    return Collections.unmodifiableList(recipes);
  }

  /**
   * The recipe that takes {@code input}, or null if none does.
   *
   * @param input the input stack
   *
   * @return the recipe
   */
  @Nullable
  public ApplianceRecipe find(@Nonnull ItemStack input) {
    if (input.isEmpty()) {
      return null;
    }
    for (ApplianceRecipe recipe : recipes) {
      if (recipe.matches(input)) {
        return recipe;
      }
    }
    return null;
  }
}
