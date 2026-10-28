package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * Pawning: a player hands over the stacks in some slots and is paid for the ones a pawn shop
 * takes.
 *
 * <ol>
 *   <li>Are pawn shops buying, and may this player pawn (creative players may not, unless
 *   {@code allowCreative})?</li>
 *   <li>Quote every stack. Refused stacks stay where they are.</li>
 *   <li>Take stacks, in slot order, while the total stays within the player's allowance for the
 *   day ({@code maxPayoutPerDay}). The rest stay.</li>
 *   <li>Take the stacks out of the slots.</li>
 *   <li>Pay. If the payment is refused, put every stack back.</li>
 *   <li>Record the payout against the day, and hand each stack to the keeper (a reselling
 *   counter's stock), or destroy it.</li>
 * </ol>
 *
 * <p>Server side only.</p>
 *
 * @since 2026.10
 */
public final class PawnTrade {

  /** What happened. */
  public static final class Outcome {

    /** How many items were taken. */
    public int taken;
    /** What was paid, in money. */
    public double paid;
    /** What to tell the player: the outcome first, then why anything stayed. */
    public final List<ITextComponent> messages = new ArrayList<>();

    boolean isEmpty() {
      return taken == 0;
    }
  }

  private PawnTrade() {
  }

  /** Today, for the daily allowance: the server's local date. */
  static long today() {
    return LocalDate.now().toEpochDay();
  }

  /**
   * Pawns the stacks in {@code slots[from, to)}.
   *
   * @param keeper given each taken stack, returns what it could not keep (which is destroyed);
   *               null destroys everything taken
   */
  public static Outcome pawn(EntityPlayerMP player, IInventory slots, int from, int to,
      @Nullable UnaryOperator<ItemStack> keeper) {
    Outcome out = new Outcome();
    if (!CsmConfig.isPawnEnabled()) {
      out.messages.add(new TextComponentTranslation("csm.pawn.msg.closed"));
      return out;
    }
    if (player.isCreative() && !CsmConfig.isPawnAllowCreative()) {
      out.messages.add(new TextComponentTranslation("csm.pawn.msg.creative"));
      return out;
    }

    PawnLedger ledger = PawnLedger.get(player.world);
    long today = today();
    double allowance = ledger.allowanceToday(player.getUniqueID(), today,
        CsmConfig.getPawnMaxPayoutPerDay());

    Map<String, ITextComponent> refusals = new LinkedHashMap<>();
    List<Integer> picked = new ArrayList<>();
    double total = 0;
    boolean capped = false;
    for (int i = from; i < to; i++) {
      ItemStack stack = slots.getStackInSlot(i);
      if (stack.isEmpty()) {
        continue;
      }
      Quote q = PawnValuation.quote(stack);
      if (!q.isAccepted()) {
        refusals.putIfAbsent(stack.getDisplayName() + "|" + q.refusal,
            new TextComponentTranslation("csm.pawn.msg.refused_item", stack.getTextComponent(),
                new TextComponentTranslation(q.refusal)));
        continue;
      }
      if (total + q.payout > allowance + 1e-9) {
        capped = true;
        continue;
      }
      total += q.payout;
      picked.add(i);
    }
    if (capped) {
      out.messages.add(new TextComponentTranslation(allowance <= 0
          ? "csm.pawn.msg.cap_reached" : "csm.pawn.msg.cap_partial"));
    }
    out.messages.addAll(refusals.values());
    if (picked.isEmpty()) {
      return out;
    }

    List<ItemStack> taken = new ArrayList<>();
    for (int i : picked) {
      taken.add(slots.removeStackFromSlot(i));
    }
    PawnEconomy.Payment payment =
        PawnEconomy.pay(player, PawnValuation.cents(total), "Pawn shop");
    if (!payment.isPaid()) {
      for (int k = 0; k < picked.size(); k++) {
        slots.setInventorySlotContents(picked.get(k), taken.get(k));
      }
      slots.markDirty();
      out.messages.add(0, new TextComponentTranslation("csm.pawn.msg.not_paid", payment.refusal));
      return out;
    }
    slots.markDirty();
    ledger.recordPaid(player.getUniqueID(), today, total);
    for (ItemStack stack : taken) {
      out.taken += stack.getCount();
      if (keeper != null) {
        keeper.apply(stack);
      }
    }
    out.paid = PawnValuation.cents(total);
    ITextComponent items = out.taken == 1 ? new TextComponentTranslation("csm.pawn.item_one")
        : new TextComponentTranslation("csm.pawn.items", out.taken);
    ITextComponent done = PawnEconomy.isMoneyMode()
        ? new TextComponentTranslation("csm.pawn.msg.pawned", items, PawnEconomy.describe(out.paid))
        : new TextComponentTranslation("csm.pawn.msg.pawned_got", items,
            PawnEconomy.describe(out.paid), payment.paid);
    done.getStyle().setColor(TextFormatting.GREEN);
    out.messages.add(0, done);
    return out;
  }
}
