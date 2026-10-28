package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

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
    /** What to tell the player, worst first. */
    public final List<String> messages = new ArrayList<>();

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
      out.messages.add("Pawn shops aren't buying right now.");
      return out;
    }
    if (player.isCreative() && !CsmConfig.isPawnAllowCreative()) {
      out.messages.add("Pawn shops don't buy from players in creative mode.");
      return out;
    }

    PawnLedger ledger = PawnLedger.get(player.world);
    long today = today();
    double allowance = ledger.allowanceToday(player.getUniqueID(), today,
        CsmConfig.getPawnMaxPayoutPerDay());

    Set<String> refusals = new LinkedHashSet<>();
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
        refusals.add(stack.getDisplayName() + ": " + q.refusal);
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
      out.messages.add(allowance <= 0
          ? "You've pawned all a pawn shop will take from you today."
          : "That's more than a pawn shop will take from you today; the rest stays with you.");
    }
    out.messages.addAll(refusals);
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
      out.messages.add(0, "The pawn shop couldn't pay you: " + payment.refusal);
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
    out.messages.add(0, "Pawned " + out.taken + (out.taken == 1 ? " item" : " items") + " for "
        + PawnEconomy.describe(out.paid)
        + (PawnEconomy.isMoneyMode() ? "." : ": you got " + payment.paid + "."));
    return out;
  }
}
