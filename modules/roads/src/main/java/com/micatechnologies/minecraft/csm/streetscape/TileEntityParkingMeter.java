package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A parking meter's or pay station's state: who owns it, what it charges, what it has taken, and
 * when each of its spaces runs out.
 *
 * <p>Time is real time. Each space stores the wall-clock moment it expires, so paid time keeps
 * running while the chunk is unloaded or the server is down, as parking does. Nothing ticks: the
 * block schedules one update at the next expiry to flip its redstone, and every display computes
 * what is left from the expiry when it draws.</p>
 *
 * <p>The client's clock is not the server's. The sync carries the server's clock, and the client
 * shifts every expiry by the difference, so a display counts down to the moment the server
 * agrees the space has run out.</p>
 *
 * @version 1.0
 */
public class TileEntityParkingMeter extends AbstractTileEntity {

  /** The most spaces a pay station can sell. */
  public static final int MAX_SPACES = 50;
  /** A pay station's spaces when first placed. */
  public static final int DEFAULT_SPACES = 10;

  private static final String KEY_OWNER = "o";
  private static final String KEY_OWNER_NAME = "on";
  private static final String KEY_EXPIRY = "ex";
  private static final String KEY_EMERALDS = "re";
  private static final String KEY_MINUTES = "rm";
  private static final String KEY_MAX = "mx";
  private static final String KEY_MONEY = "rd";
  private static final String KEY_COLLECT = "c";
  private static final String KEY_STORED_EMERALDS = "se";
  private static final String KEY_STORED_MONEY = "sd";
  private static final String KEY_SERVER_NOW = "sn";
  private static final String KEY_MONEY_MODE = "sm";

  @Nullable
  private UUID owner;
  private String ownerName = "";
  /** Expiry per head or space, in wall-clock milliseconds; 0 for never paid. */
  private long[] expiry = new long[1];
  private int emeraldsPerBlock = CsmConfig.getParkingEmeraldsPerBlock();
  private int minutesPerBlock = CsmConfig.getParkingMinutesPerBlock();
  private int maxMinutes = CsmConfig.getParkingMaxMinutes();
  private double moneyPerBlock = CsmConfig.getParkingMoneyPerBlock();
  private boolean collect;
  private long storedEmeralds;
  private double storedMoney;
  /** Client only: whether the server charges money (SUM) rather than emeralds. */
  private boolean moneyMode;

  /**
   * Client render cache, render thread only: the digital reading each head last showed and the
   * whole seconds it shows, so the renderer formats and measures it once a second rather than
   * every frame. The text depends on nothing but the seconds.
   */
  private final long[] readingSeconds = {-1, -1};
  private final String[] readingText = new String[2];
  private final int[] readingWidth = new int[2];

  // ----------------------------------------------------------------------------------------
  // Spaces and time
  // ----------------------------------------------------------------------------------------

  public int getSpaces() {
    return expiry.length;
  }

  /** Sets how many spaces there are, keeping what the surviving ones have paid. */
  public void setSpaces(int spaces) {
    int n = Math.max(1, Math.min(MAX_SPACES, spaces));
    if (n != expiry.length) {
      expiry = Arrays.copyOf(expiry, n);
    }
  }

  /** Milliseconds left on a space at {@code now}; 0 once expired. */
  public long remaining(int space, long now) {
    if (space < 0 || space >= expiry.length) {
      return 0;
    }
    return Math.max(0, expiry[space] - now);
  }

  public boolean isExpired(int space, long now) {
    return remaining(space, now) <= 0;
  }

  /**
   * The reading a digital head shows, {@link ParkingPayments#remaining}, remembered per head for
   * the second it stands for. Render thread only.
   *
   * @param head    the head, 0 or 1
   * @param now     the client's wall clock
   * @param measure measures the text, called only when it changes
   *
   * @return the reading; its width is then {@link #readingWidth(int)}
   */
  String reading(int head, long now, ToIntFunction<String> measure) {
    long millis = remaining(head, now);
    long seconds = Math.max(0, (millis + 999) / 1000);
    if (readingSeconds[head] != seconds || readingText[head] == null) {
      readingText[head] = ParkingPayments.clock(millis);
      readingWidth[head] = measure.applyAsInt(readingText[head]);
      readingSeconds[head] = seconds;
    }
    return readingText[head];
  }

  /** The width {@link #reading} measured for a head's current reading. */
  int readingWidth(int head) {
    return readingWidth[head];
  }

  /** Whether any space is expired at {@code now}. */
  public boolean anyExpired(long now) {
    for (int i = 0; i < expiry.length; i++) {
      if (isExpired(i, now)) {
        return true;
      }
    }
    return false;
  }

  /** The earliest expiry still in the future, or -1 if none is. */
  public long nextExpiry(long now) {
    long next = -1;
    for (long e : expiry) {
      if (e > now && (next < 0 || e < next)) {
        next = e;
      }
    }
    return next;
  }

  /**
   * Buys up to {@code blocks} blocks of time on a space, no further ahead than the meter's
   * maximum, and returns how many blocks were actually bought (0 if the space is already at
   * its maximum).
   */
  public int extend(int space, int blocks, long now) {
    if (space < 0 || space >= expiry.length || blocks <= 0) {
      return 0;
    }
    long block = minutesPerBlock * 60_000L;
    long limit = now + maxMinutes * 60_000L;
    long from = Math.max(now, expiry[space]);
    int bought = 0;
    while (bought < blocks && from + block <= limit + block / 2) {
      from += block;
      bought++;
    }
    if (bought > 0) {
      expiry[space] = Math.min(from, limit);
    }
    return bought;
  }

  /** How many blocks could still be bought on a space before it reaches its maximum. */
  public int blocksToMax(int space, long now) {
    long block = minutesPerBlock * 60_000L;
    long left = now + maxMinutes * 60_000L - Math.max(now, expiry[space]);
    return (int) Math.max(0, (left + block / 2) / block);
  }

  // ----------------------------------------------------------------------------------------
  // Owner and settings
  // ----------------------------------------------------------------------------------------

  @Nullable
  public UUID getOwner() {
    return owner;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwner(UUID owner, String name) {
    this.owner = owner;
    this.ownerName = name == null ? "" : name;
  }

  public int getEmeraldsPerBlock() {
    return emeraldsPerBlock;
  }

  public int getMinutesPerBlock() {
    return minutesPerBlock;
  }

  public int getMaxMinutes() {
    return maxMinutes;
  }

  public double getMoneyPerBlock() {
    return moneyPerBlock;
  }

  public boolean isCollect() {
    return collect;
  }

  /** Applies an owner's settings, each clamped to the server's caps. */
  public void applySettings(int emeralds, int minutes, int max, double money, boolean collect,
      int spaces) {
    this.emeraldsPerBlock = Math.max(1, Math.min(CsmConfig.getParkingCapEmeralds(), emeralds));
    this.minutesPerBlock = Math.max(1, Math.min(1440, minutes));
    this.maxMinutes = Math.max(this.minutesPerBlock,
        Math.min(CsmConfig.getParkingCapMinutes(), max));
    double capped = Math.min(CsmConfig.getParkingCapMoney(), money);
    this.moneyPerBlock = Double.isNaN(capped) ? CsmConfig.getParkingMoneyPerBlock()
        : Math.max(0.01, Math.round(capped * 100.0) / 100.0);
    this.collect = collect;
    setSpaces(spaces);
  }

  // ----------------------------------------------------------------------------------------
  // Takings
  // ----------------------------------------------------------------------------------------

  public long getStoredEmeralds() {
    return storedEmeralds;
  }

  public double getStoredMoney() {
    return storedMoney;
  }

  public void addTakings(long emeralds, double money) {
    storedEmeralds += emeralds;
    storedMoney = Math.round((storedMoney + money) * 100.0) / 100.0;
  }

  public void clearEmeralds() {
    storedEmeralds = 0;
  }

  public void clearMoney() {
    storedMoney = 0;
  }

  /** Whether the server charges money through SUM rather than emeralds (client side). */
  public boolean isMoneyMode() {
    return moneyMode;
  }

  // ----------------------------------------------------------------------------------------
  // NBT
  // ----------------------------------------------------------------------------------------

  /** Nothing a baked model reads comes from here; only the special renderer does. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  /** The whole unit is drawn from here, and a pay station's screen sits a block up. */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(getPos()).expand(0, 1, 0);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    owner = compound.hasUniqueId(KEY_OWNER) ? compound.getUniqueId(KEY_OWNER) : null;
    ownerName = compound.getString(KEY_OWNER_NAME);
    NBTTagList list = compound.getTagList(KEY_EXPIRY, Constants.NBT.TAG_LONG);
    long[] read = new long[Math.max(1, Math.min(MAX_SPACES, list.tagCount()))];
    for (int i = 0; i < read.length && i < list.tagCount(); i++) {
      read[i] = ((NBTTagLong) list.get(i)).getLong();
    }
    if (compound.hasKey(KEY_SERVER_NOW)) {
      // A sync from the server: move every expiry onto this machine's clock.
      long skew = System.currentTimeMillis() - compound.getLong(KEY_SERVER_NOW);
      for (int i = 0; i < read.length; i++) {
        if (read[i] > 0) {
          read[i] += skew;
        }
      }
    }
    expiry = read;
    moneyMode = compound.getBoolean(KEY_MONEY_MODE);
    if (compound.hasKey(KEY_MINUTES)) {
      emeraldsPerBlock = compound.getInteger(KEY_EMERALDS);
      minutesPerBlock = Math.max(1, compound.getInteger(KEY_MINUTES));
      maxMinutes = Math.max(minutesPerBlock, compound.getInteger(KEY_MAX));
      moneyPerBlock = compound.getDouble(KEY_MONEY);
    }
    collect = compound.getBoolean(KEY_COLLECT);
    storedEmeralds = compound.getLong(KEY_STORED_EMERALDS);
    storedMoney = compound.getDouble(KEY_STORED_MONEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (owner != null) {
      compound.setUniqueId(KEY_OWNER, owner);
    }
    compound.setString(KEY_OWNER_NAME, ownerName);
    NBTTagList list = new NBTTagList();
    for (long e : expiry) {
      list.appendTag(new NBTTagLong(e));
    }
    compound.setTag(KEY_EXPIRY, list);
    compound.setInteger(KEY_EMERALDS, emeraldsPerBlock);
    compound.setInteger(KEY_MINUTES, minutesPerBlock);
    compound.setInteger(KEY_MAX, maxMinutes);
    compound.setDouble(KEY_MONEY, moneyPerBlock);
    compound.setBoolean(KEY_COLLECT, collect);
    compound.setLong(KEY_STORED_EMERALDS, storedEmeralds);
    compound.setDouble(KEY_STORED_MONEY, storedMoney);
    return compound;
  }

  /** The sync to clients carries the server's clock; see the class notes. */
  @Override
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = super.getUpdateTag();
    tag.setLong(KEY_SERVER_NOW, System.currentTimeMillis());
    tag.setBoolean(KEY_MONEY_MODE, ParkingPaymentSum.isAvailable());
    return tag;
  }
}
