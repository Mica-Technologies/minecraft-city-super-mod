package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.furniture.market.TileEntityDisplayCase;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A shop: the one item it sells (its <em>template</em>, a real stack that is shown and never
 * sold), 27 slots of stock, what it asks per sale and what it pays per buy-back, its till, its
 * owner, and three switches: a server shop (no owner, payments destroyed, buy-backs paid from
 * nothing), unlimited stock, decorative (shows its item, trades nothing). The trading itself is
 * {@link ShopTrade}'s.
 *
 * <p>Prices and tills are kept per currency, money and emeralds side by side, and the one in
 * force ({@link ShopEconomy#isMoneyMode()}) is used: a server that adds or drops the economy
 * mod finds every shop's prices as its owner left them, never reinterpreted.</p>
 *
 * <p>Stock counts only stacks that match the template exactly (item, metadata and NBT). The
 * owner's screen and hoppers may put only such stacks in; hoppers can never take anything out
 * ({@link #getCapability}).</p>
 *
 * <p>Clients are sent what the shelf and the shop's screen show -- the template, the prices in
 * force, roughly how much stock, the owner's name, the switches -- and never the stock itself.
 * A change of stock syncs only when it changes how full the shelf looks or whether a sale can be
 * made, so a hopper feeding a shop does not send a packet per item. No baked model reads any of
 * this ({@link #getBakedModelKey}), so a sync never rebuilds the chunk.</p>
 *
 * @since 2026.10
 */
public class TileEntityShop extends TileEntityDisplayCase {

  /** Stock slots. */
  public static final int STOCK_SLOTS = 27;
  /** The most items a single sale or buy-back moves. */
  public static final int MAX_AMOUNT = 64;

  private static final String KEY_TEMPLATE = "t";
  private static final String KEY_SELL_AMOUNT = "sa";
  private static final String KEY_SELL_MONEY = "sm";
  private static final String KEY_SELL_EMERALDS = "se";
  private static final String KEY_BUY_AMOUNT = "ba";
  private static final String KEY_BUY_MONEY = "bm";
  private static final String KEY_BUY_EMERALDS = "be";
  private static final String KEY_TILL_MONEY = "tm";
  private static final String KEY_TILL_EMERALDS = "tE";
  private static final String KEY_OWNER = "o";
  private static final String KEY_OWNER_NAME = "on";
  private static final String KEY_SERVER = "ss";
  private static final String KEY_INFINITE = "inf";
  private static final String KEY_DECORATIVE = "dec";
  // Sync only.
  private static final String KEY_STOCK = "sc";
  private static final String KEY_MONEY_MODE = "mm";
  private static final String KEY_SYMBOL = "cs";
  private static final String KEY_ALL_DECORATIVE = "ad";

  /** The item it sells, one slot, never sold. */
  private final ItemStackHandler template = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      templateChanged();
    }
  };

  /** What hoppers and pipes see: the stock, insert only. */
  private final IItemHandler insertOnly = new InsertOnly();

  private int sellAmount = 1;
  private double sellMoney = 1.0;
  private int sellEmeralds = 1;
  private int buyAmount = 1;
  /** Zero: the shop does not buy. */
  private double buyMoney;
  /** Zero: the shop does not buy. */
  private int buyEmeralds;
  private double tillMoney;
  private long tillEmeralds;
  @Nullable
  private UUID owner;
  private String ownerName = "";
  private boolean serverShop;
  private boolean infinite;
  private boolean decorative;

  // Client copies of what only the server knows.
  private int clientStock;
  private boolean clientMoneyMode;
  private String clientSymbol = "$";
  private boolean clientAllDecorative;

  /**
   * The renderer's cache: the template's item model (an {@code IBakedModel}, typed loosely so
   * the server never names a client class) and the stack it was looked up for.
   */
  @Nullable
  transient Object clientModel;
  @Nullable
  transient ItemStack clientModelFor;

  /** Set while loading, when a change of template is not news to anyone. */
  private transient boolean reading;
  /** Set while a trade moves stock, which syncs once when it is done ({@link ShopTrade}). */
  private transient boolean quiet;

  /** What the last sync said about the stock, so only a visible change syncs again. */
  private transient long syncedStockKey = Long.MIN_VALUE;

  /** Constructs an empty shop, as the game does before loading a saved one. */
  public TileEntityShop() {
    super(STOCK_SLOTS);
  }

  // --- the template ------------------------------------------------------------------------

  /** The template slot, for the owner's screen. */
  public ItemStackHandler getTemplateSlot() {
    return template;
  }

  /** The item it sells (count meaningless), or empty if not set up. */
  public ItemStack getTemplate() {
    return template.getStackInSlot(0);
  }

  /** Whether {@code stack} is the item this shop sells. */
  public boolean matches(ItemStack stack) {
    ItemStack t = getTemplate();
    return !t.isEmpty() && !stack.isEmpty() && ItemStack.areItemsEqual(t, stack)
        && ItemStack.areItemStackTagsEqual(t, stack);
  }

  @Override
  public boolean accepts(@Nonnull ItemStack stack) {
    return matches(stack);
  }

  private void templateChanged() {
    if (!reading && world != null && !world.isRemote) {
      syncedStockKey = Long.MIN_VALUE;
      sync();
    }
  }

  // --- stock ---------------------------------------------------------------------------

  /** Matching items held. Server side; clients see {@link #getShownStock()}. */
  public int countStock() {
    int count = 0;
    ItemStackHandler items = getItems();
    for (int s = 0; s < items.getSlots(); s++) {
      ItemStack st = items.getStackInSlot(s);
      if (matches(st)) {
        count += st.getCount();
      }
    }
    return count;
  }

  /** Whether {@code amount} can be sold now. */
  public boolean hasStock(int amount) {
    return infinite || countStock() >= amount;
  }

  /**
   * Takes {@code amount} matching items out of stock.
   *
   * @return how many were taken
   */
  int takeStock(int amount) {
    if (infinite) {
      return amount;
    }
    int left = amount;
    ItemStackHandler items = getItems();
    quiet = true;
    try {
      for (int s = 0; s < items.getSlots() && left > 0; s++) {
        if (matches(items.getStackInSlot(s))) {
          left -= items.extractItem(s, left, false).getCount();
        }
      }
    } finally {
      quiet = false;
    }
    return amount - left;
  }

  /**
   * Puts {@code stack} into stock.
   *
   * @return what did not fit
   */
  ItemStack putStock(ItemStack stack, boolean simulate) {
    ItemStack rest = stack.copy();
    ItemStackHandler items = getItems();
    quiet = true;
    try {
      for (int s = 0; s < items.getSlots() && !rest.isEmpty(); s++) {
        rest = items.insertItem(s, rest, simulate);
      }
    } finally {
      quiet = false;
    }
    return rest;
  }

  /** The stock as last sent to clients (unlimited shows as {@link Integer#MAX_VALUE}). */
  public int getShownStock() {
    return infinite ? Integer.MAX_VALUE : clientStock;
  }

  @Override
  public void markDirty() {
    super.markDirty();
    if (!quiet && world != null && !world.isRemote) {
      long key = stockKey();
      if (key != syncedStockKey) {
        sync();
      }
    }
  }

  /**
   * What the clients' view of the stock depends on: how many places the shelf fills (in
   * quarter stacks, up to a few dozen) and whether one sale can be made.
   */
  private long stockKey() {
    int stock = countStock();
    int perPlace = Math.max(1, getTemplate().getMaxStackSize() / 4);
    long places = Math.min(64, (stock + perPlace - 1) / perPlace);
    return places * 2 + (stock >= sellAmount ? 1 : 0);
  }

  /** Sends clients what they show, if anything changed. */
  public void sync() {
    if (world == null || world.isRemote) {
      return;
    }
    syncedStockKey = stockKey();
    super.markDirty();
    syncServerToClient(world);
  }

  // --- prices --------------------------------------------------------------------------

  public int getSellAmount() {
    return sellAmount;
  }

  public int getBuyAmount() {
    return buyAmount;
  }

  /** The sale price in the currency in force (emeralds as a whole number). */
  public double getSellPrice(boolean money) {
    return money ? sellMoney : sellEmeralds;
  }

  /** The buy-back price in the currency in force; zero when the shop does not buy. */
  public double getBuyPrice(boolean money) {
    return money ? buyMoney : buyEmeralds;
  }

  /** Whether the shop buys its item back in the currency in force. */
  public boolean buysBack(boolean money) {
    return getBuyPrice(money) > 0;
  }

  /** Sets what one sale moves and costs, clamped, in the currency in force. */
  void setSell(boolean money, int amount, double price) {
    sellAmount = clampAmount(amount);
    if (money) {
      sellMoney = clampMoney(price, false);
    } else {
      sellEmeralds = (int) clampEmeralds(price, false);
    }
  }

  /** Sets what one buy-back moves and pays (zero: none), clamped, in the currency in force. */
  void setBuy(boolean money, int amount, double price) {
    buyAmount = clampAmount(amount);
    if (money) {
      buyMoney = clampMoney(price, true);
    } else {
      buyEmeralds = (int) clampEmeralds(price, true);
    }
  }

  private static int clampAmount(int amount) {
    return Math.max(1, Math.min(MAX_AMOUNT, amount));
  }

  private static double clampMoney(double price, boolean zeroAllowed) {
    if (Double.isNaN(price) || Double.isInfinite(price)) {
      return zeroAllowed ? 0 : 0.01;
    }
    double cents = Math.round(price * 100.0) / 100.0;
    double min = zeroAllowed ? 0 : 0.01;
    return Math.max(min, Math.min(CsmConfig.getShopsMaxMoneyPrice(), cents));
  }

  private static long clampEmeralds(double price, boolean zeroAllowed) {
    if (Double.isNaN(price) || Double.isInfinite(price)) {
      return zeroAllowed ? 0 : 1;
    }
    long whole = Math.round(price);
    return Math.max(zeroAllowed ? 0 : 1, Math.min(CsmConfig.getShopsMaxEmeraldPrice(), whole));
  }

  // --- the till ------------------------------------------------------------------------

  public double getTillMoney() {
    return tillMoney;
  }

  public long getTillEmeralds() {
    return tillEmeralds;
  }

  /** The till in the currency in force. */
  public double getTill(boolean money) {
    return money ? tillMoney : tillEmeralds;
  }

  /** Adds to (or, negative, takes from) the till in the currency in force. */
  void addTill(boolean money, double amount) {
    if (money) {
      tillMoney = Math.max(0, Math.round((tillMoney + amount) * 100.0) / 100.0);
    } else {
      tillEmeralds = Math.max(0, tillEmeralds + Math.round(amount));
    }
    super.markDirty();
  }

  // --- owner and switches ----------------------------------------------------------------

  @Nullable
  public UUID getOwner() {
    return owner;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public boolean isOwned() {
    return owner != null;
  }

  public boolean isOwner(EntityPlayer player) {
    return owner != null && owner.equals(player.getUniqueID());
  }

  /** Makes {@code player} the owner. */
  void claim(EntityPlayer player) {
    owner = player.getUniqueID();
    ownerName = player.getName();
    sync();
  }

  public boolean isServerShop() {
    return serverShop;
  }

  /** Makes it a server shop (dropping its owner) or a player shop owned by {@code player}. */
  void setServerShop(boolean server, EntityPlayer player) {
    serverShop = server;
    if (server) {
      owner = null;
      ownerName = "";
    } else if (owner == null) {
      owner = player.getUniqueID();
      ownerName = player.getName();
    }
    sync();
  }

  public boolean isInfinite() {
    return infinite;
  }

  void setInfinite(boolean infinite) {
    this.infinite = infinite;
    sync();
  }

  /** Whether its owner made this shop decorative. */
  public boolean isDecorativeSelf() {
    return decorative;
  }

  void setDecorative(boolean decorative) {
    this.decorative = decorative;
    sync();
  }

  /**
   * Whether it only shows its item: its owner said so, or the server makes every shop
   * decorative. Right on both sides.
   */
  public boolean isDecorative() {
    boolean all = world != null && world.isRemote ? clientAllDecorative
        : CsmConfig.isShopsDecorativeOnly();
    return decorative || all;
  }

  /** Whether prices are money. Right on both sides (the server's view is sent). */
  public boolean isMoneyMode() {
    return world != null && world.isRemote ? clientMoneyMode : ShopEconomy.isMoneyMode();
  }

  /** The currency symbol for money prices. Right on both sides. */
  public String getSymbol() {
    return world != null && world.isRemote ? clientSymbol : ShopEconomy.SUM.currencySymbol();
  }

  /** Whether there is an item to sell. */
  public boolean isSetUp() {
    return !getTemplate().isEmpty();
  }

  // --- saving and syncing ----------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    reading = true;
    try {
      template.setStackInSlot(0, compound.hasKey(KEY_TEMPLATE)
          ? new ItemStack(compound.getCompoundTag(KEY_TEMPLATE)) : ItemStack.EMPTY);
    } finally {
      reading = false;
    }
    sellAmount = compound.hasKey(KEY_SELL_AMOUNT) ? clampAmount(
        compound.getInteger(KEY_SELL_AMOUNT)) : 1;
    sellMoney = compound.hasKey(KEY_SELL_MONEY) ? compound.getDouble(KEY_SELL_MONEY) : 1.0;
    sellEmeralds = compound.hasKey(KEY_SELL_EMERALDS) ? compound.getInteger(KEY_SELL_EMERALDS)
        : 1;
    buyAmount = compound.hasKey(KEY_BUY_AMOUNT) ? clampAmount(compound.getInteger(KEY_BUY_AMOUNT))
        : 1;
    buyMoney = compound.getDouble(KEY_BUY_MONEY);
    buyEmeralds = compound.getInteger(KEY_BUY_EMERALDS);
    tillMoney = compound.getDouble(KEY_TILL_MONEY);
    tillEmeralds = compound.getLong(KEY_TILL_EMERALDS);
    owner = compound.hasUniqueId(KEY_OWNER) ? compound.getUniqueId(KEY_OWNER) : null;
    ownerName = compound.getString(KEY_OWNER_NAME);
    serverShop = compound.getBoolean(KEY_SERVER);
    infinite = compound.getBoolean(KEY_INFINITE);
    decorative = compound.getBoolean(KEY_DECORATIVE);
    if (compound.hasKey(KEY_MONEY_MODE)) {
      clientStock = compound.getInteger(KEY_STOCK);
      clientMoneyMode = compound.getBoolean(KEY_MONEY_MODE);
      clientSymbol = compound.getString(KEY_SYMBOL);
      clientAllDecorative = compound.getBoolean(KEY_ALL_DECORATIVE);
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    writeShared(compound);
    compound.setDouble(KEY_TILL_MONEY, tillMoney);
    compound.setLong(KEY_TILL_EMERALDS, tillEmeralds);
    return compound;
  }

  /** What both the save and the sync carry. */
  private void writeShared(NBTTagCompound compound) {
    ItemStack t = getTemplate();
    if (!t.isEmpty()) {
      compound.setTag(KEY_TEMPLATE, t.writeToNBT(new NBTTagCompound()));
    }
    compound.setInteger(KEY_SELL_AMOUNT, sellAmount);
    compound.setDouble(KEY_SELL_MONEY, sellMoney);
    compound.setInteger(KEY_SELL_EMERALDS, sellEmeralds);
    compound.setInteger(KEY_BUY_AMOUNT, buyAmount);
    compound.setDouble(KEY_BUY_MONEY, buyMoney);
    compound.setInteger(KEY_BUY_EMERALDS, buyEmeralds);
    if (owner != null) {
      compound.setUniqueId(KEY_OWNER, owner);
    }
    compound.setString(KEY_OWNER_NAME, ownerName);
    compound.setBoolean(KEY_SERVER, serverShop);
    compound.setBoolean(KEY_INFINITE, infinite);
    compound.setBoolean(KEY_DECORATIVE, decorative);
  }

  /**
   * The clients' copy: the shared settings and how much stock, never the stock or the till.
   */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("x", pos.getX());
    tag.setInteger("y", pos.getY());
    tag.setInteger("z", pos.getZ());
    writeShared(tag);
    tag.setInteger(KEY_STOCK, Math.min(countStock(), 1 << 20));
    tag.setBoolean(KEY_MONEY_MODE, ShopEconomy.isMoneyMode());
    tag.setString(KEY_SYMBOL, ShopEconomy.SUM.currencySymbol());
    tag.setBoolean(KEY_ALL_DECORATIVE, CsmConfig.isShopsDecorativeOnly());
    return tag;
  }

  /** Nothing a baked model draws comes from here: the items are the renderer's. */
  @Override
  protected long getBakedModelKey() {
    return 0;
  }

  /** Everything it holds, for dropping when it is broken: the stock, the template, emeralds. */
  @Override
  public void forEachItem(Consumer<ItemStack> action) {
    super.forEachItem(action);
    if (!getTemplate().isEmpty()) {
      action.accept(getTemplate());
    }
    long left = tillEmeralds;
    while (left > 0) {
      int n = (int) Math.min(64, left);
      action.accept(new ItemStack(Items.EMERALD, n));
      left -= n;
    }
  }

  // --- automation ----------------------------------------------------------------------

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        || super.hasCapability(capability, facing);
  }

  @Override
  @Nullable
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(insertOnly);
    }
    return super.getCapability(capability, facing);
  }

  @Override
  @SideOnly(Side.CLIENT)
  @Nonnull
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p, p.add(1, 2, 1));
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    double d = CsmConfig.getShopsRenderDistance();
    return d * d;
  }

  /** The stock as hoppers and pipes see it: matching items in, nothing out. */
  private final class InsertOnly implements IItemHandler {

    @Override
    public int getSlots() {
      return getItems().getSlots();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
      return getItems().getStackInSlot(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
      return getItems().insertItem(slot, stack, simulate);
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return getItems().getSlotLimit(slot);
    }
  }
}
