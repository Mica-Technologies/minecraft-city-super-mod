package com.micatechnologies.minecraft.csm.furniture.appliance;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * One thing an appliance does with what is put in it: bread into toast, a raw porkchop into a
 * cooked one, a worn pickaxe into a less worn one. An {@link ApplianceRecipeBook} holds a
 * machine type's recipes; {@link TileEntityAppliance} asks it for the one that takes what is in
 * its input slot.
 *
 * <p>A recipe is asked about the input stack as it stands, so one recipe can cover a family of
 * inputs (every smelting recipe that ends in food) and work its result out from the input (a
 * tool with less damage). The static factories cover the plain cases.</p>
 *
 * @since 2026.9
 */
public interface ApplianceRecipe {

  /**
   * Whether this recipe takes {@code input}. The count is not considered here, only the kind.
   *
   * @param input the input stack, never empty
   *
   * @return true if it does
   */
  boolean matches(@Nonnull ItemStack input);

  /**
   * What one cycle makes from {@code input}: a new stack, never the input itself.
   *
   * @param input the input stack
   *
   * @return the result
   */
  @Nonnull
  ItemStack getResult(@Nonnull ItemStack input);

  /**
   * How long a cycle takes at this recipe's own pace, in ticks, before the appliance's
   * {@link ApplianceSpec#getTimeFactor() time factor} is applied.
   *
   * @param input the input stack
   *
   * @return the ticks, at least 1
   */
  int getTicks(@Nonnull ItemStack input);

  /**
   * How many of the input one cycle uses; a cycle waits until there are that many.
   *
   * @param input the input stack
   *
   * @return the count, at least 1
   */
  default int getInputCount(@Nonnull ItemStack input) {
    return 1;
  }

  /**
   * Whether a cycle uses up the input. A recipe that makes something from the input without
   * using it up (the copier copies a book and hands the original back) says no: the input stays
   * where it is and the appliance goes on while there is room for another result.
   *
   * @return false to keep the input
   */
  default boolean consumesInput() {
    return true;
  }

  /**
   * Whether the result goes back into the input slot for another cycle instead of into the
   * output: a tool being repaired stays in until it is whole. The input is then replaced by the
   * result rather than used up.
   *
   * @param result the result of the cycle just finished
   *
   * @return true to keep working on it
   */
  default boolean staysInInput(@Nonnull ItemStack result) {
    return false;
  }

  /**
   * A recipe taking {@code count} of one item and making {@code result}.
   *
   * @param input  the item taken
   * @param count  how many a cycle takes
   * @param result what a cycle makes (copied for each cycle)
   * @param ticks  how long a cycle takes
   *
   * @return the recipe
   */
  static ApplianceRecipe of(Item input, int count, ItemStack result, int ticks) {
    ItemStack made = result.copy();
    return of(stack -> stack.getItem() == input, count, stack -> made.copy(), ticks);
  }

  /**
   * A recipe taking {@code count} of whatever {@code input} accepts and making what
   * {@code result} works out from it.
   *
   * @param input  which stacks it takes
   * @param count  how many a cycle takes
   * @param result what a cycle makes from a stack; a new stack each call
   * @param ticks  how long a cycle takes
   *
   * @return the recipe
   */
  static ApplianceRecipe of(Predicate<ItemStack> input, int count,
      Function<ItemStack, ItemStack> result, int ticks) {
    return new ApplianceRecipe() {
      @Override
      public boolean matches(@Nonnull ItemStack stack) {
        return input.test(stack);
      }

      @Nonnull
      @Override
      public ItemStack getResult(@Nonnull ItemStack stack) {
        ItemStack out = result.apply(stack);
        return out == null ? ItemStack.EMPTY : out;
      }

      @Override
      public int getTicks(@Nonnull ItemStack stack) {
        return ticks;
      }

      @Override
      public int getInputCount(@Nonnull ItemStack stack) {
        return count;
      }
    };
  }
}
