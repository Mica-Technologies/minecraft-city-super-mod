package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * The pawn screen's slots: nine offer slots, which belong to this screen alone (what is left in
 * them goes back to the player when it closes), and the player's inventory below them.
 *
 * <p>Quotes are the server's: whenever the offer changes it works each slot's out and sends them
 * with the total ({@link PawnQuotePacket}); the client only shows them.</p>
 *
 * @since 2026.10
 */
public class ContainerPawnCounter extends Container implements IPawnQuoted {

  /** How many offer slots. */
  public static final int OFFER_SLOTS = 9;
  /** Where the offer row sits. */
  public static final int OFFER_Y = 18;

  private final TileEntityPawnCounter counter;
  private final InventoryBasic offer = new InventoryBasic("pawn", false, OFFER_SLOTS);

  /** Server: the offer last quoted, so a quote goes out only when it changes. */
  @Nullable
  private String quoted;
  /** Client: the latest quote. */
  @Nullable
  private PawnQuotePacket quote;

  public ContainerPawnCounter(InventoryPlayer playerInventory, TileEntityPawnCounter counter) {
    this.counter = counter;
    for (int c = 0; c < OFFER_SLOTS; c++) {
      addSlotToContainer(new Slot(offer, c, 8 + c * 18, OFFER_Y));
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

  /** Server: pawns the offer and sends the player what happened. */
  void accept(EntityPlayerMP player) {
    PawnTrade.Outcome out = PawnTrade.pawn(player, offer, 0, OFFER_SLOTS,
        counter.isResale() ? counter::keep : null);
    if (out.messages.isEmpty()) {
      player.sendMessage(new TextComponentTranslation("csm.pawn.gui.empty"));
    }
    for (ITextComponent line : out.messages) {
      player.sendMessage(line);
    }
    requote();
    detectAndSendChanges();
  }

  /** Server: quotes again at the next change check, even if the offer is the same. */
  void requote() {
    quoted = null;
  }

  @Override
  public void detectAndSendChanges() {
    super.detectAndSendChanges();
    if (counter.getWorld() == null || counter.getWorld().isRemote) {
      return;
    }
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < OFFER_SLOTS; i++) {
      ItemStack s = offer.getStackInSlot(i);
      key.append(PawnValuation.key(s)).append('x').append(s.getCount())
          .append(s.hasTagCompound() ? s.getTagCompound().hashCode() : 0).append(';');
    }
    key.append(counter.isResale()).append(PawnEconomy.isMoneyMode());
    if (key.toString().equals(quoted)) {
      return;
    }
    quoted = key.toString();
    for (IContainerListener listener : listeners) {
      if (listener instanceof EntityPlayerMP) {
        CsmFurnishings.NETWORK.sendTo(quote((EntityPlayerMP) listener), (EntityPlayerMP) listener);
      }
    }
  }

  private PawnQuotePacket quote(EntityPlayerMP player) {
    String[] lines = new String[OFFER_SLOTS];
    boolean[] bad = new boolean[OFFER_SLOTS];
    double total = 0;
    for (int i = 0; i < OFFER_SLOTS; i++) {
      ItemStack s = offer.getStackInSlot(i);
      if (s.isEmpty()) {
        lines[i] = "";
        continue;
      }
      Quote q = PawnValuation.quote(s);
      if (q.isAccepted()) {
        total += q.payout;
        lines[i] = json(new TextComponentTranslation("csm.pawn.gui.pays",
            PawnEconomy.describe(q.payout)));
      } else {
        bad[i] = true;
        lines[i] = json(new TextComponentTranslation(q.refusal));
      }
    }
    int flags = 0;
    if (counter.isResale()) {
      flags |= PawnQuotePacket.FLAG_RESALE;
    }
    if (PawnPermissions.isAdmin(player)) {
      flags |= PawnQuotePacket.FLAG_ADMIN;
    }
    ITextComponent header;
    if (!CsmConfig.isPawnEnabled()) {
      header = new TextComponentTranslation("csm.pawn.msg.closed");
    } else if (player.isCreative() && !CsmConfig.isPawnAllowCreative()) {
      header = new TextComponentTranslation("csm.pawn.msg.creative");
    } else {
      flags |= PawnQuotePacket.FLAG_CAN_PAWN;
      boolean any = false;
      for (int i = 0; i < OFFER_SLOTS; i++) {
        any |= !offer.getStackInSlot(i).isEmpty();
      }
      header = any ? new TextComponentTranslation("csm.pawn.gui.total",
          PawnEconomy.describe(PawnValuation.cents(total)))
          : new TextComponentTranslation("csm.pawn.gui.quoting");
      double cap = CsmConfig.getPawnMaxPayoutPerDay();
      if (cap > 0) {
        double left = PawnLedger.get(player.world).allowanceToday(player.getUniqueID(),
            PawnTrade.today(), cap);
        header.appendText("  ").appendSibling(new TextComponentTranslation(
            "csm.pawn.gui.left_today", PawnEconomy.describe(PawnValuation.cents(left))));
      }
    }
    return new PawnQuotePacket(windowId, flags, json(header), lines, bad);
  }

  static String json(ITextComponent text) {
    return ITextComponent.Serializer.componentToJson(text);
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

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    return !counter.isInvalid() && player.getDistanceSq(counter.getPos().getX() + 0.5,
        counter.getPos().getY() + 0.5, counter.getPos().getZ() + 0.5) <= 64.0;
  }

  /** What is still on offer goes back to the player. */
  @Override
  public void onContainerClosed(@Nonnull EntityPlayer player) {
    super.onContainerClosed(player);
    if (!player.world.isRemote) {
      clearContainer(player, player.world, offer);
    }
  }

  /** Shift-click: between the offer and the inventory. */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    boolean moved = index < OFFER_SLOTS
        ? mergeItemStack(stack, OFFER_SLOTS, inventorySlots.size(), true)
        : mergeItemStack(stack, 0, OFFER_SLOTS, false);
    if (!moved) {
      return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    return copy;
  }
}
