package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The resale screen's slots: a reselling counter's 27 slots of stock, to look at and buy from
 * (a click buys, through {@link PawnActionPacket#BUY}; nothing can be picked up out of them), and
 * the player's inventory below. The server sends each slot's price ({@link PawnQuotePacket}).
 *
 * @since 2026.10
 */
public class ContainerPawnResale extends Container implements IPawnQuoted {

  public static final int STOCK_SLOTS = 27;

  private final TileEntityPawnCounter counter;

  @Nullable
  private String quoted;
  @Nullable
  private PawnQuotePacket quote;

  /**
   * Constructs the container.
   *
   * @param server whether this is the server's (the real stock); the client's slots are stand-ins
   *               the server fills
   */
  public ContainerPawnResale(InventoryPlayer playerInventory, TileEntityPawnCounter counter,
      boolean server) {
    this.counter = counter;
    IItemHandler stock = server ? counter.getItems() : new ItemStackHandler(STOCK_SLOTS);
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new SlotItemHandler(stock, c + r * 9, 8 + c * 18, 18 + r * 18) {
          @Override
          public boolean isItemValid(@Nonnull ItemStack stack) {
            return false;
          }

          @Override
          public boolean canTakeStack(EntityPlayer player) {
            return false;
          }
        });
      }
    }
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
      }
    }
    for (int c = 0; c < 9; c++) {
      addSlotToContainer(new Slot(playerInventory, c, 8 + c * 18, 142));
    }
  }

  public TileEntityPawnCounter getCounter() {
    return counter;
  }

  /** Clicks on the stock do nothing here: buying is the screen's packet. */
  @Override
  @Nonnull
  public ItemStack slotClick(int slotId, int dragType, ClickType clickType,
      EntityPlayer player) {
    if (slotId >= 0 && slotId < STOCK_SLOTS) {
      return ItemStack.EMPTY;
    }
    return super.slotClick(slotId, dragType, clickType, player);
  }

  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    return !counter.isInvalid() && counter.isResale()
        && player.getDistanceSq(counter.getPos().getX() + 0.5, counter.getPos().getY() + 0.5,
        counter.getPos().getZ() + 0.5) <= 64.0;
  }

  @Override
  public void detectAndSendChanges() {
    super.detectAndSendChanges();
    if (counter.getWorld() == null || counter.getWorld().isRemote) {
      return;
    }
    StringBuilder key = new StringBuilder();
    IItemHandler stock = counter.getItems();
    for (int i = 0; i < STOCK_SLOTS; i++) {
      ItemStack s = stock.getStackInSlot(i);
      key.append(PawnValuation.key(s)).append('x').append(s.getCount()).append(';');
    }
    key.append(PawnEconomy.isMoneyMode());
    if (key.toString().equals(quoted)) {
      return;
    }
    quoted = key.toString();
    String[] lines = new String[STOCK_SLOTS];
    boolean[] bad = new boolean[STOCK_SLOTS];
    for (int i = 0; i < STOCK_SLOTS; i++) {
      ItemStack s = stock.getStackInSlot(i);
      if (s.isEmpty()) {
        lines[i] = "";
        continue;
      }
      double each = PawnResale.priceEach(s);
      lines[i] = ContainerPawnCounter.json(new TextComponentTranslation("csm.pawn.gui.price",
          PawnEconomy.describePrice(each), s.getCount(),
          PawnEconomy.describePrice(each * s.getCount())));
    }
    String header = ContainerPawnCounter.json(new TextComponentTranslation(
        PawnEconomy.isMoneyMode() ? "csm.pawn.gui.resale_money" : "csm.pawn.gui.resale_emeralds"));
    for (IContainerListener listener : listeners) {
      if (listener instanceof EntityPlayerMP) {
        EntityPlayerMP p = (EntityPlayerMP) listener;
        int flags = PawnQuotePacket.FLAG_RESALE
            | (PawnPermissions.isAdmin(p) ? PawnQuotePacket.FLAG_ADMIN : 0);
        CsmFurnishings.NETWORK.sendTo(new PawnQuotePacket(windowId, flags, header, lines, bad),
            p);
      }
    }
  }

  /** Server: prices again at the next change check. */
  void requote() {
    quoted = null;
  }

  @Override
  public void setQuote(PawnQuotePacket quote) {
    this.quote = quote;
  }

  @Nullable
  @Override
  public PawnQuotePacket getQuote() {
    return quote;
  }
}
