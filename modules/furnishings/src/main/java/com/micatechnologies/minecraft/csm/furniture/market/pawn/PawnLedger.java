package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * What the pawn shops remember about each player, in the save's global data (one for every
 * dimension):
 *
 * <ul>
 *   <li>the <b>emerald remainder</b>: value paid for in emerald mode that did not make a whole
 *   emerald yet, paid once it does;</li>
 *   <li><b>today's payouts</b> and the day they were for, for {@code maxPayoutPerDay};</li>
 *   <li>what is <b>owed</b> to a player who was offline when something was pawned in their name
 *   (a drop-off bin fed by a hopper), paid when they are next online.</li>
 * </ul>
 *
 * <p>All amounts are money (value units), so a server that adds or drops the economy mod keeps
 * every remainder and debt as it was. Server side only.</p>
 *
 * @since 2026.10
 */
public class PawnLedger extends WorldSavedData {

  private static final String NAME = "csm_pawn";

  /** One player's record. */
  static final class Entry {

    double emeraldRemainder;
    long day;
    double paidToday;
    double owed;

    boolean isEmpty() {
      return emeraldRemainder <= 0 && paidToday <= 0 && owed <= 0;
    }
  }

  private final Map<UUID, Entry> entries = new HashMap<>();

  /** For the world's storage, which constructs it by name. */
  public PawnLedger(String name) {
    super(name);
  }

  /** The ledger, from the save's global storage. */
  public static PawnLedger get(World world) {
    MapStorage storage = world.getMapStorage();
    PawnLedger data = (PawnLedger) storage.getOrLoadData(PawnLedger.class, NAME);
    if (data == null) {
      data = new PawnLedger(NAME);
      storage.setData(NAME, data);
    }
    return data;
  }

  Entry entry(UUID player) {
    return entries.computeIfAbsent(player, k -> new Entry());
  }

  /** How much more the player may be paid today, or infinity with no cap. */
  double allowanceToday(UUID player, long today, double cap) {
    if (cap <= 0) {
      return Double.POSITIVE_INFINITY;
    }
    Entry e = entries.get(player);
    if (e == null || e.day != today) {
      return cap;
    }
    return Math.max(0, cap - e.paidToday);
  }

  void recordPaid(UUID player, long today, double amount) {
    Entry e = entry(player);
    if (e.day != today) {
      e.day = today;
      e.paidToday = 0;
    }
    e.paidToday += amount;
    markDirty();
  }

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    entries.clear();
    NBTTagList list = nbt.getTagList("p", Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < list.tagCount(); i++) {
      NBTTagCompound t = list.getCompoundTagAt(i);
      Entry e = new Entry();
      e.emeraldRemainder = t.getDouble("r");
      e.day = t.getLong("d");
      e.paidToday = t.getDouble("t");
      e.owed = t.getDouble("o");
      entries.put(t.getUniqueId("u"), e);
    }
  }

  @Nonnull
  @Override
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
    NBTTagList list = new NBTTagList();
    for (Map.Entry<UUID, Entry> me : entries.entrySet()) {
      Entry e = me.getValue();
      if (e.isEmpty()) {
        continue;
      }
      NBTTagCompound t = new NBTTagCompound();
      t.setUniqueId("u", me.getKey());
      t.setDouble("r", e.emeraldRemainder);
      t.setLong("d", e.day);
      t.setDouble("t", e.paidToday);
      t.setDouble("o", e.owed);
      list.appendTag(t);
    }
    nbt.setTag("p", list);
    return nbt;
  }
}
