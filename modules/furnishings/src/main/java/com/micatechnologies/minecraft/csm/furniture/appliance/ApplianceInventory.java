package com.micatechnologies.minecraft.csm.furniture.appliance;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.items.ItemStackHandler;

/**
 * An appliance's three slots: {@link #INPUT}, {@link #OUTPUT} and {@link #FUEL} (used only by
 * an appliance that burns fuel, or uses up a supply). The input takes only what the appliance
 * has a recipe for, up to its input limit; the fuel slot takes only furnace fuel, or for an
 * appliance with a supply only that supply; nothing is put into the output but by the
 * appliance itself.
 *
 * <p>The same class backs the server's slots and the client's stand-in, so a player's click is
 * refused on both sides alike.</p>
 *
 * @since 2026.9
 */
public class ApplianceInventory extends ItemStackHandler {

  /** What goes in. */
  public static final int INPUT = 0;
  /** What comes out. */
  public static final int OUTPUT = 1;
  /** Fuel, for an appliance that burns it; or its supply, for one that uses one up. */
  public static final int FUEL = 2;
  /** How many slots there are. */
  public static final int SLOTS = 3;

  private final Supplier<ApplianceSpec> spec;

  /**
   * Constructs the slots.
   *
   * @param spec the appliance's spec, asked each time (null while the block is not known)
   */
  public ApplianceInventory(Supplier<ApplianceSpec> spec) {
    super(SLOTS);
    this.spec = spec;
  }

  @Nullable
  private ApplianceSpec spec() {
    return spec.get();
  }

  @Override
  public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
    ApplianceSpec s = spec();
    if (s == null || stack.isEmpty()) {
      return false;
    }
    if (slot == INPUT) {
      return s.getBook().find(stack) != null;
    }
    if (slot == FUEL) {
      return s.usesFuel() ? TileEntityFurnace.isItemFuel(stack) : s.acceptsSupply(stack);
    }
    return false;
  }

  @Override
  public int getSlotLimit(int slot) {
    ApplianceSpec s = spec();
    return slot == INPUT && s != null ? s.getInputLimit() : 64;
  }

  /** Refuses what the slot does not take (this Forge's handler does not ask). */
  @Override
  @Nonnull
  public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
    if (!isItemValid(slot, stack)) {
      return stack;
    }
    return super.insertItem(slot, stack, simulate);
  }
}
