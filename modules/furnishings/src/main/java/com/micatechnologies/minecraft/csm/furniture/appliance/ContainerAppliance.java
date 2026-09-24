package com.micatechnologies.minecraft.csm.furniture.appliance;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * An appliance's slots over the player's inventory, where a vanilla furnace has them: the input
 * over the flame and the fuel under it (or the input alone, level with the output, when it burns
 * nothing), the output to the right of the arrow. {@link GuiAppliance} draws it on the furnace's
 * own screen.
 *
 * <p>The cycle, the tank and the fuel go to the client as window properties, which the vanilla
 * container sync carries; nothing else is sent. On the client the slots are backed by a
 * stand-in of the same kind, so what a slot refuses it refuses on both sides.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class ContainerAppliance extends Container {

  private static final int PROP_PROGRESS = 0;
  private static final int PROP_TOTAL = 1;
  private static final int PROP_WATER = 2;
  private static final int PROP_BURN = 3;
  private static final int PROP_BURN_MAX = 4;
  private static final int PROPS = 5;

  private final TileEntityAppliance appliance;
  private final ApplianceSpec spec;
  private final int machineSlots;
  /** The last values sent (server) or received (client). */
  private final int[] values = new int[PROPS];

  /**
   * Constructs the container.
   *
   * @param playerInventory the player's inventory
   * @param appliance       the appliance's tile entity
   * @param spec            its spec
   * @param server          whether this is the server's container (the real slots)
   */
  public ContainerAppliance(InventoryPlayer playerInventory, TileEntityAppliance appliance,
      ApplianceSpec spec, boolean server) {
    this.appliance = appliance;
    this.spec = spec;
    ItemStackHandler handler = server ? appliance.getItems() : new ApplianceInventory(() -> spec);
    boolean fuel = spec.usesFuel() || spec.usesSupply();
    addSlotToContainer(new SlotItemHandler(handler, ApplianceInventory.INPUT, 56, fuel ? 17 : 35));
    addSlotToContainer(new SlotItemHandler(handler, ApplianceInventory.OUTPUT, 116, 35));
    if (fuel) {
      addSlotToContainer(new SlotItemHandler(handler, ApplianceInventory.FUEL, 56, 53));
    }
    machineSlots = inventorySlots.size();
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
      }
    }
    for (int c = 0; c < 9; c++) {
      addSlotToContainer(new Slot(playerInventory, c, 8 + c * 18, 142));
    }
    for (int i = 0; i < PROPS; i++) {
      values[i] = -1;
    }
  }

  /**
   * The appliance's spec.
   *
   * @return the spec
   */
  public ApplianceSpec getSpec() {
    return spec;
  }

  private int current(int id) {
    switch (id) {
      case PROP_PROGRESS:
        return appliance.getProgress();
      case PROP_TOTAL:
        return appliance.getTotal();
      case PROP_WATER:
        return appliance.getWater();
      case PROP_BURN:
        return appliance.getBurn();
      default:
        return appliance.getBurnMax();
    }
  }

  @Override
  public void addListener(@Nonnull IContainerListener listener) {
    super.addListener(listener);
    for (int i = 0; i < PROPS; i++) {
      listener.sendWindowProperty(this, i, current(i));
    }
  }

  @Override
  public void detectAndSendChanges() {
    super.detectAndSendChanges();
    for (int i = 0; i < PROPS; i++) {
      int v = current(i);
      if (v != values[i]) {
        values[i] = v;
        for (IContainerListener listener : listeners) {
          listener.sendWindowProperty(this, i, v);
        }
      }
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void updateProgressBar(int id, int data) {
    if (id >= 0 && id < PROPS) {
      values[id] = data;
    }
  }

  private int value(int id) {
    return Math.max(0, values[id]);
  }

  /**
   * How far through the cycle, 0 to {@code width}, for drawing the arrow (client side).
   *
   * @param width the arrow's width
   *
   * @return the filled width
   */
  public int progressScaled(int width) {
    int total = value(PROP_TOTAL);
    return total <= 0 ? 0 : Math.min(width, value(PROP_PROGRESS) * width / total);
  }

  /**
   * How much fuel is left of the piece burning, 0 to {@code height} (client side).
   *
   * @param height the flame's height
   *
   * @return the filled height
   */
  public int burnScaled(int height) {
    int max = value(PROP_BURN_MAX);
    return max <= 0 ? 0 : Math.min(height, value(PROP_BURN) * height / max);
  }

  /**
   * How many cycles' water is in the tank (client side).
   *
   * @return the water
   */
  public int getWater() {
    return value(PROP_WATER);
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    return !appliance.isInvalid() && player.getDistanceSq(appliance.getPos().getX() + 0.5,
        appliance.getPos().getY() + 0.5, appliance.getPos().getZ() + 0.5) <= 64.0;
  }

  /** Shift-click: out of the appliance into the inventory, or into the input or fuel slot. */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    if (index < machineSlots) {
      if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) {
        return ItemStack.EMPTY;
      }
      slot.onSlotChange(stack, copy);
    } else if (inventorySlots.get(0).isItemValid(stack)) {
      if (!mergeItemStack(stack, 0, 1, false)) {
        return ItemStack.EMPTY;
      }
    } else if (machineSlots > 2 && inventorySlots.get(2).isItemValid(stack)) {
      if (!mergeItemStack(stack, 2, 3, false)) {
        return ItemStack.EMPTY;
      }
    } else {
      return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    if (stack.getCount() == copy.getCount()) {
      return ItemStack.EMPTY;
    }
    slot.onTake(player, stack);
    return copy;
  }
}
