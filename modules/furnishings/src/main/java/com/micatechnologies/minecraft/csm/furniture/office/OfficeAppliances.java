package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipe;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceRecipeBook;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The office's working appliance, on the kitchen's machine framework
 * ({@code furniture.appliance}): the copier, which copies written books.
 *
 * <p>A written book in its input and a book and quill in its supply slot make a copy in 5 s:
 * the original stays in the input, the copy goes to the output and the book and quill is used
 * up, as copying a book at a crafting table uses one. The rules are the crafting table's: an
 * original makes a "copy of original", a copy of an original makes a "copy of a copy", and a
 * copy of a copy cannot be copied. A written book does not stack, so the copier makes one copy
 * and waits for it to be taken (a hopper under it takes each as it comes).</p>
 *
 * @since 2026.9
 */
public final class OfficeAppliances {

  /** How long a copy takes, in ticks. */
  private static final int COPY_TICKS = 100;
  /** The book tag that says how far from the original a copy is. */
  private static final String GENERATION = "generation";

  public static final ApplianceRecipeBook COPIER_BOOK = ApplianceRecipeBook.get("copier")
      .add(new BookCopy());

  public static final ApplianceSpec COPIER = new ApplianceSpec(COPIER_BOOK).inputLimit(1)
      .supply(stack -> stack.getItem() == Items.WRITABLE_BOOK,
          "csm.furnishings.appliance.supply.copier")
      .runSound(FurnishingsSounds.PRINTER_RUN, 40, 0.5F, 1.0F)
      .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 1.2F).light(2);

  private OfficeAppliances() {
  }

  /** A copy of a written book, one generation further from the original; the original kept. */
  private static final class BookCopy implements ApplianceRecipe {

    @Override
    public boolean matches(@Nonnull ItemStack input) {
      return input.getItem() == Items.WRITTEN_BOOK && input.hasTagCompound()
          && ItemWrittenBook.getGeneration(input) < 2;
    }

    @Nonnull
    @Override
    public ItemStack getResult(@Nonnull ItemStack input) {
      ItemStack copy = new ItemStack(Items.WRITTEN_BOOK);
      NBTTagCompound tag = input.getTagCompound();
      if (tag == null) {
        return ItemStack.EMPTY;
      }
      NBTTagCompound out = tag.copy();
      out.setInteger(GENERATION, ItemWrittenBook.getGeneration(input) + 1);
      copy.setTagCompound(out);
      return copy;
    }

    @Override
    public int getTicks(@Nonnull ItemStack input) {
      return COPY_TICKS;
    }

    @Override
    public boolean consumesInput() {
      return false;
    }
  }
}
