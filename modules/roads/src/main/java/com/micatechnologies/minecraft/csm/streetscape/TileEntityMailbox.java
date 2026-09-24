package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A mailbox's compartments: who placed the box, who owns each compartment, and what each holds.
 *
 * <p>Anyone may post into an owned compartment; only its owner (or an operator) may take out.
 * The placer owns every compartment to begin with, and hands one to a named player or frees it
 * for the next player to claim ({@link BlockMailbox}).</p>
 *
 * <p>What a compartment holds never leaves the server except through the container of a player
 * allowed to see it: the sync to clients carries the owners and one bit per compartment saying
 * whether it holds anything (the curbside box raises its flag on it), and nothing else.</p>
 *
 * <p>A box with one compartment takes items from a hopper or pipe on any side, as a real drop
 * box takes mail, and gives nothing back to one: automation must not be a way round the lock.
 * A box with several compartments takes nothing from automation, since it could not say which
 * compartment an item was meant for.</p>
 *
 * @version 1.0
 */
public class TileEntityMailbox extends AbstractTileEntity {

  /** How long, in milliseconds, before a compartment's owner is told of new mail again. */
  private static final long NOTICE_INTERVAL_MS = 30_000L;

  private static final String KEY_PLACER = "p";
  private static final String KEY_PLACER_NAME = "pn";
  private static final String KEY_COMPARTMENTS = "c";
  private static final String KEY_OWNER = "o";
  private static final String KEY_OWNER_NAME = "on";
  private static final String KEY_ITEMS = "i";
  private static final String KEY_MAIL = "m";

  @Nullable
  private UUID placer;
  private String placerName = "";
  private UUID[] owners = new UUID[0];
  private String[] ownerNames = new String[0];
  private MailHandler[] compartments = new MailHandler[0];
  /** Client only: bit i set when compartment i holds anything. */
  private int clientMail;
  /** When each compartment's owner was last told of new mail; not saved. */
  private long[] lastNotice = new long[0];

  private final IItemHandler automationSlot = new AutomationSlot();

  // ----------------------------------------------------------------------------------------
  // Layout
  // ----------------------------------------------------------------------------------------

  /**
   * Makes sure there is one compartment per entry of {@code slots}, each that many slots. Called
   * when the box is placed; a saved box brings its own compartments.
   */
  public void init(int[] slots) {
    if (compartments.length == slots.length) {
      return;
    }
    UUID[] o = new UUID[slots.length];
    String[] n = new String[slots.length];
    MailHandler[] c = new MailHandler[slots.length];
    for (int i = 0; i < slots.length; i++) {
      o[i] = i < owners.length ? owners[i] : null;
      n[i] = i < ownerNames.length ? ownerNames[i] : "";
      c[i] = i < compartments.length ? compartments[i] : new MailHandler(slots[i]);
    }
    owners = o;
    ownerNames = n;
    compartments = c;
    lastNotice = new long[slots.length];
  }

  public int getCompartmentCount() {
    return compartments.length;
  }

  public ItemStackHandler getCompartment(int i) {
    return compartments[i];
  }

  // ----------------------------------------------------------------------------------------
  // Owners
  // ----------------------------------------------------------------------------------------

  @Nullable
  public UUID getPlacer() {
    return placer;
  }

  public String getPlacerName() {
    return placerName;
  }

  /** Sets who placed the box, and gives them every compartment. */
  public void setPlacer(UUID uuid, String name) {
    placer = uuid;
    placerName = name == null ? "" : name;
    for (int i = 0; i < owners.length; i++) {
      owners[i] = uuid;
      ownerNames[i] = placerName;
    }
  }

  @Nullable
  public UUID getOwner(int i) {
    return i >= 0 && i < owners.length ? owners[i] : null;
  }

  public String getOwnerName(int i) {
    return i >= 0 && i < ownerNames.length ? ownerNames[i] : "";
  }

  /** Gives compartment {@code i} to a player, or frees it when {@code uuid} is null. */
  public void setOwner(int i, @Nullable UUID uuid, String name) {
    if (i < 0 || i >= owners.length) {
      return;
    }
    owners[i] = uuid;
    ownerNames[i] = uuid == null || name == null ? "" : name;
  }

  public boolean isOwner(int i, EntityPlayer player) {
    UUID o = getOwner(i);
    return o != null && o.equals(player.getUniqueID());
  }

  public boolean isPlacer(EntityPlayer player) {
    return placer != null && placer.equals(player.getUniqueID());
  }

  // ----------------------------------------------------------------------------------------
  // Mail
  // ----------------------------------------------------------------------------------------

  /** Whether compartment {@code i} holds anything, on either side. */
  public boolean hasMail(int i) {
    if (world != null && world.isRemote) {
      return (clientMail & (1 << i)) != 0;
    }
    if (i < 0 || i >= compartments.length) {
      return false;
    }
    MailHandler h = compartments[i];
    for (int s = 0; s < h.getSlots(); s++) {
      if (!h.getStackInSlot(s).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private int mailMask() {
    int mask = 0;
    for (int i = 0; i < compartments.length && i < 31; i++) {
      if (hasMail(i)) {
        mask |= 1 << i;
      }
    }
    return mask;
  }

  /**
   * Posts {@code stack} into compartment {@code i} and returns what did not fit. A free
   * compartment takes nothing: mail must be for someone.
   *
   * @param poster who posted it, or {@code null} for a hopper or pipe
   */
  public ItemStack post(int i, ItemStack stack, @Nullable EntityPlayer poster) {
    if (i < 0 || i >= compartments.length || getOwner(i) == null || stack.isEmpty()) {
      return stack;
    }
    // A copy: an item handler keeps the very stack it is given, and this one is the caller's.
    ItemStack left = ItemHandlerHelper.insertItemStacked(compartments[i], stack.copy(), false);
    if (left.getCount() != stack.getCount()) {
      tellOwner(i, poster);
    }
    return left;
  }

  /** Tells an online owner that mail has come, at most once every {@link #NOTICE_INTERVAL_MS}. */
  private void tellOwner(int i, @Nullable EntityPlayer poster) {
    if (world == null || world.isRemote || world.getMinecraftServer() == null) {
      return;
    }
    UUID o = getOwner(i);
    if (o == null || (poster != null && o.equals(poster.getUniqueID()))) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastNotice[i] < NOTICE_INTERVAL_MS) {
      return;
    }
    EntityPlayerMP owner = world.getMinecraftServer().getPlayerList().getPlayerByUUID(o);
    if (owner == null) {
      return;
    }
    lastNotice[i] = now;
    Block block = getBlockType();
    String in = block instanceof BlockMailbox && ((BlockMailbox) block).getCompartmentCount() > 1
        ? " in " + ((BlockMailbox) block).getCompartmentName(i) : "";
    owner.sendMessage(new TextComponentString(TextFormatting.GOLD + String.format(Locale.ROOT,
        "You have mail%s%s at %d, %d, %d", poster != null ? " from " + poster.getName() : "", in,
        pos.getX(), pos.getY(), pos.getZ())));
  }

  /** Called whenever a compartment's contents change: resyncs only when its mail bit flips. */
  private void contentsChanged() {
    if (world == null || world.isRemote) {
      return;
    }
    int mask = mailMask();
    if (mask != clientMail) {
      clientMail = mask;
      markDirtySync(world, pos, true);
    } else {
      markDirty();
    }
  }

  /** Every item in every compartment, for dropping when the box is broken. */
  public void forEachItem(java.util.function.Consumer<ItemStack> action) {
    for (MailHandler h : compartments) {
      for (int s = 0; s < h.getSlots(); s++) {
        ItemStack st = h.getStackInSlot(s);
        if (!st.isEmpty()) {
          action.accept(st);
        }
      }
    }
  }

  // ----------------------------------------------------------------------------------------
  // Automation
  // ----------------------------------------------------------------------------------------

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return compartments.length == 1;
    }
    return super.hasCapability(capability, facing);
  }

  @Override
  @Nullable
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return compartments.length == 1
          ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automationSlot) : null;
    }
    return super.getCapability(capability, facing);
  }

  /**
   * What a hopper or pipe sees: one empty slot that posts whatever it is given into the box's
   * one compartment, and never has anything to take.
   */
  private final class AutomationSlot implements IItemHandler {

    @Override
    public int getSlots() {
      return 1;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
      if (compartments.length != 1 || getOwner(0) == null) {
        return stack;
      }
      if (simulate) {
        return ItemHandlerHelper.insertItemStacked(compartments[0], stack, true);
      }
      return post(0, stack, null);
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 64;
    }
  }

  /** A compartment: an item handler that reports its changes to the box. */
  private final class MailHandler extends ItemStackHandler {

    MailHandler(int size) {
      super(size);
    }

    @Override
    protected void onContentsChanged(int slot) {
      contentsChanged();
    }
  }

  // ----------------------------------------------------------------------------------------
  // NBT
  // ----------------------------------------------------------------------------------------

  /** The flag is the one thing a baked model reads, and it follows the first compartment. */
  @Override
  protected long getBakedModelKey() {
    return hasMail(0) ? 1L : 0L;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(getPos()).expand(0, 1, 0);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    placer = compound.hasUniqueId(KEY_PLACER) ? compound.getUniqueId(KEY_PLACER) : null;
    placerName = compound.getString(KEY_PLACER_NAME);
    NBTTagList list = compound.getTagList(KEY_COMPARTMENTS, Constants.NBT.TAG_COMPOUND);
    int n = list.tagCount();
    boolean withItems = n > 0 && list.getCompoundTagAt(0).hasKey(KEY_ITEMS);
    if (owners.length != n) {
      owners = new UUID[n];
      ownerNames = new String[n];
      lastNotice = new long[n];
    }
    if (withItems || compartments.length != n) {
      MailHandler[] c = new MailHandler[n];
      for (int i = 0; i < n; i++) {
        c[i] = i < compartments.length ? compartments[i] : new MailHandler(9);
      }
      compartments = c;
    }
    for (int i = 0; i < n; i++) {
      NBTTagCompound tag = list.getCompoundTagAt(i);
      owners[i] = tag.hasUniqueId(KEY_OWNER) ? tag.getUniqueId(KEY_OWNER) : null;
      ownerNames[i] = tag.getString(KEY_OWNER_NAME);
      if (tag.hasKey(KEY_ITEMS)) {
        compartments[i].deserializeNBT(tag.getCompoundTag(KEY_ITEMS));
      }
    }
    clientMail = compound.getInteger(KEY_MAIL);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return write(compound, true);
  }

  private NBTTagCompound write(NBTTagCompound compound, boolean withItems) {
    if (placer != null) {
      compound.setUniqueId(KEY_PLACER, placer);
    }
    compound.setString(KEY_PLACER_NAME, placerName);
    NBTTagList list = new NBTTagList();
    for (int i = 0; i < compartments.length; i++) {
      NBTTagCompound tag = new NBTTagCompound();
      if (owners[i] != null) {
        tag.setUniqueId(KEY_OWNER, owners[i]);
      }
      tag.setString(KEY_OWNER_NAME, ownerNames[i]);
      if (withItems) {
        tag.setTag(KEY_ITEMS, compartments[i].serializeNBT());
      }
      list.appendTag(tag);
    }
    compound.setTag(KEY_COMPARTMENTS, list);
    compound.setInteger(KEY_MAIL, mailMask());
    return compound;
  }

  /**
   * What clients are sent: owners and the mail bits, never the contents (see the class notes).
   */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("x", pos.getX());
    tag.setInteger("y", pos.getY());
    tag.setInteger("z", pos.getZ());
    return write(tag, false);
  }
}
