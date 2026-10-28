package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * A pawn drop-off bin: who has claimed it, and the hopper slot it pays them through.
 *
 * <p>A hopper (or anything else that inserts) feeds one virtual slot that never holds anything:
 * each stack is quoted and, if a pawn shop would take it and the claimer's daily allowance has
 * room, destroyed and paid to the claimer ({@link PawnEconomy#payOrOwe}: at once if they are
 * online, at their next login if not). Anything refused stays in the hopper. An unclaimed bin, or
 * one with {@code binAcceptsHoppers} off, takes nothing this way. Nothing can be pulled out.</p>
 *
 * @since 2026.10
 */
public class TileEntityPawnDropBin extends AbstractTileEntity {

  private static final String KEY_PAYEE = "pu";
  private static final String KEY_NAME = "pn";
  private static final String KEY_PENDING = "pp";

  @Nullable
  private UUID payee;
  private String payeeName = "";
  /** What hopper input has earned the claimer short of a whole cent, in money. */
  private double pending;

  private final IItemHandler hopperSlot = new IItemHandler() {
    @Override
    public int getSlots() {
      return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
      return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
      return sink(stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 64;
    }
  };

  @Nullable
  public UUID getPayee() {
    return payee;
  }

  public String getPayeeName() {
    return payeeName;
  }

  /** Claims the bin for a player, or with null releases it. */
  public void setPayee(@Nullable UUID payee, String name) {
    this.payee = payee;
    this.payeeName = payee == null ? "" : name;
    // what the last claimer had saved up short of a cent is theirs, not the next one's: dropped
    this.pending = 0;
    markDirty();
  }

  /**
   * Takes a stack from a hopper and pays the claimer for it.
   *
   * @return what is not taken
   */
  ItemStack sink(ItemStack stack, boolean simulate) {
    if (stack.isEmpty() || payee == null || world == null || world.isRemote
        || !CsmConfig.isPawnEnabled() || !CsmConfig.isPawnBinAcceptsHoppers()) {
      return stack;
    }
    MinecraftServer server = world.getMinecraftServer();
    if (server == null) {
      return stack;
    }
    EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(payee);
    if (online != null && online.isCreative() && !CsmConfig.isPawnAllowCreative()) {
      return stack;
    }
    // A hopper brings one item at a time: its fraction of a cent is saved up, not rounded away.
    Quote q = PawnValuation.quote(stack, true);
    if (!q.isAccepted()) {
      return stack;
    }
    PawnLedger ledger = PawnLedger.get(world);
    long today = PawnTrade.today();
    if (q.payout > ledger.allowanceToday(payee, today, CsmConfig.getPawnMaxPayoutPerDay())
        + 1e-9) {
      return stack;
    }
    if (!simulate) {
      ledger.recordPaid(payee, today, q.payout);
      pending += q.payout;
      double due = PawnValuation.cents(pending);
      if (due > 0) {
        pending = Math.max(0, pending - due);
        PawnEconomy.payOrOwe(server, payee, due, "Pawn drop-off bin");
      }
      markDirty();
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    payee = compound.hasUniqueId(KEY_PAYEE) ? compound.getUniqueId(KEY_PAYEE) : null;
    payeeName = compound.getString(KEY_NAME);
    pending = compound.getDouble(KEY_PENDING);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (payee != null) {
      compound.setUniqueId(KEY_PAYEE, payee);
      compound.setString(KEY_NAME, payeeName);
    }
    if (pending > 0) {
      compound.setDouble(KEY_PENDING, pending);
    }
    return compound;
  }

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        || super.hasCapability(capability, facing);
  }

  @Nullable
  @Override
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(hopperSlot);
    }
    return super.getCapability(capability, facing);
  }
}
