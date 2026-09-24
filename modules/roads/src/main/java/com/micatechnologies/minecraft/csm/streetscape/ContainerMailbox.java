package com.micatechnologies.minecraft.csm.streetscape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * One mailbox compartment's slots and the player's inventory.
 *
 * <p>What the container holds is set by the mode the server chose ({@link BlockMailbox#guiId}):
 * an owner sees the compartment's slots; anyone else gets one slot to post through, which empties
 * into the compartment the moment something is put in it, so nothing inside can be seen or
 * taken; a free compartment has no slots at all. The server checks the player again every tick
 * ({@link #canInteractWith}), so a compartment handed to someone else closes on its old owner.
 * </p>
 *
 * <p>On the client the compartment's slots are backed by an empty stand-in of the right size:
 * the client's tile entity is never sent the contents, and the container sync fills the slots
 * for the one player allowed to see them.</p>
 *
 * @version 1.0
 */
public class ContainerMailbox extends Container {

  /** Where the slots sit on the screen, in its coordinates. SHARED with the GUI. */
  static final int TOP_SLOTS_Y = 30;
  static final int INVENTORY_Y_GAP = 14;

  private final TileEntityMailbox box;
  private final int compartment;
  private final int mode;
  private final int boxSlots;
  @Nullable
  private final InventoryBasic postSlot;
  private final int inventoryY;
  /** Set when a post has emptied the posting slot on the server; see detectAndSendChanges. */
  private boolean resendPostSlot;

  public ContainerMailbox(InventoryPlayer playerInventory, TileEntityMailbox box, int compartment,
      int mode, int size, boolean server) {
    this.box = box;
    this.compartment = compartment;
    this.mode = mode;
    int rows = 0;
    if (mode == BlockMailbox.MODE_OPEN) {
      IItemHandler handler = server ? box.getCompartment(compartment) : new ItemStackHandler(size);
      for (int s = 0; s < size; s++) {
        addSlotToContainer(new SlotItemHandler(handler, s, 8 + (s % 9) * 18,
            TOP_SLOTS_Y + (s / 9) * 18));
      }
      boxSlots = size;
      rows = (size + 8) / 9;
      postSlot = null;
    } else if (mode == BlockMailbox.MODE_POST) {
      postSlot = new InventoryBasic("post", false, 1);
      addSlotToContainer(new PostSlot(postSlot, server));
      boxSlots = 1;
      rows = 1;
    } else {
      boxSlots = 0;
      postSlot = null;
    }
    inventoryY = inventoryY(rows);
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18,
            inventoryY + row * 18));
      }
    }
    for (int col = 0; col < 9; col++) {
      addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, inventoryY + 58));
    }
  }

  /** Where the player's inventory starts below {@code rows} rows of the box's slots. */
  static int inventoryY(int rows) {
    return TOP_SLOTS_Y + Math.max(rows, 1) * 18 + INVENTORY_Y_GAP;
  }

  public TileEntityMailbox getBox() {
    return box;
  }

  public int getCompartment() {
    return compartment;
  }

  public int getMode() {
    return mode;
  }

  public int getInventoryY() {
    return inventoryY;
  }

  /**
   * Still in reach, the box still there, and -- for an open compartment -- still the player's
   * (or the player still an operator).
   */
  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    if (box.isInvalid() || player.getDistanceSq(box.getPos().getX() + 0.5,
        box.getPos().getY() + 0.5, box.getPos().getZ() + 0.5) > 64.0) {
      return false;
    }
    return mode != BlockMailbox.MODE_OPEN
        || BlockMailbox.modeFor(player, box, compartment) == BlockMailbox.MODE_OPEN;
  }

  /** Shift-click: out of the compartment into the inventory, or from the inventory into the box. */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack() || boxSlots == 0) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    if (index < boxSlots) {
      if (!mergeItemStack(stack, boxSlots, inventorySlots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else if (!mergeItemStack(stack, 0, boxSlots, false)) {
      return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    return copy;
  }

  /**
   * Sends the posting slot once more after a post. The client put the stack in the slot itself
   * and still shows it there; the sync made while the server handles a click is sent with
   * slot updates held back (only the quantity is changing, it assumes), and by the next sync
   * the server's record already says empty. So it is sent here, outside a click.
   */
  @Override
  public void detectAndSendChanges() {
    super.detectAndSendChanges();
    if (!resendPostSlot) {
      return;
    }
    for (IContainerListener listener : listeners) {
      if (listener instanceof EntityPlayerMP
          && ((EntityPlayerMP) listener).isChangingQuantityOnly) {
        return;
      }
    }
    for (IContainerListener listener : listeners) {
      listener.sendSlotContents(this, 0, getSlot(0).getStack());
    }
    resendPostSlot = false;
  }

  /** Whatever could not be posted (the compartment was full) goes back to the player. */
  @Override
  public void onContainerClosed(@Nonnull EntityPlayer player) {
    super.onContainerClosed(player);
    if (postSlot != null && !player.world.isRemote) {
      clearContainer(player, player.world, postSlot);
    }
  }

  /**
   * The slot a letter is posted through. On the server whatever is put in goes straight into
   * the compartment; what does not fit stays in the slot, to be taken back.
   */
  private final class PostSlot extends Slot {

    private final boolean server;

    PostSlot(InventoryBasic inventory, boolean server) {
      super(inventory, 0, 80, TOP_SLOTS_Y);
      this.server = server;
    }

    @Override
    public void onSlotChanged() {
      super.onSlotChanged();
      if (!server) {
        return;
      }
      ItemStack in = getStack();
      if (!in.isEmpty()) {
        EntityPlayer poster = null;
        for (IContainerListener l : listeners) {
          if (l instanceof EntityPlayer) {
            poster = (EntityPlayer) l;
          }
        }
        ItemStack left = box.post(compartment, in, poster);
        inventory.setInventorySlotContents(0, left);
        resendPostSlot = true;
      }
    }
  }
}
