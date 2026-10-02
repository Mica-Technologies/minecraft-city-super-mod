package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * The bell schedule controller's tile entity: its {@link BellSchedule}, the speakers and hallway
 * bells linked to it, and the clock that rings them.
 *
 * <p><b>Cost.</b> It runs only while it has a schedule (or an announcement waiting). It then looks
 * at the world's time every {@link #TICK_RATE} ticks, about twice a game minute: one division and
 * a compare. Only when the minute has changed does it walk its bells (at most
 * {@link BellSchedule#MAX_ENTRIES}), and only when one is due does it touch the world, through
 * {@link BellRinger}, which reads only linked positions whose chunks are loaded and never loads
 * one. A controller in a chunk nobody has loaded does not tick at all, so a school no one is near
 * stays silent, as its speakers would be out of hearing anyway.</p>
 *
 * <p>The first look after loading only notes the minute: a bell is rung by the clock passing its
 * time, never by the world loading after it.</p>
 *
 * @since 2026.10
 */
public class TileEntityBellController extends AbstractTickableTileEntity {

  /** Speakers and bells one controller may link. */
  public static final int MAX_LINKS = 64;

  /** About twice a game minute (16 2/3 ticks), so no minute is missed. */
  static final long TICK_RATE = 8L;

  /** Ticks between the chime and the announcement that follows it. */
  static final long ANNOUNCE_DELAY = 32L;

  private static final String KEY_SCHEDULE = "bs";
  private static final String KEY_LINKS = "ln";
  private static final String KEY_MINUTE = "m";
  private static final String KEY_TONE = "t";
  private static final String KEY_TEXT = "x";
  private static final String KEY_ENABLED = "e";

  private BellSchedule schedule = BellSchedule.EMPTY;
  private final List<BlockPos> links = new ArrayList<>();

  private transient int lastMinute = -1;
  private transient String pendingAnnouncement;
  private transient long announceAt;
  /** When the screen's ring-now last rang, for its cooldown (server only). */
  transient long lastRingNow = Long.MIN_VALUE / 2;

  // --- schedule ------------------------------------------------------------------------------

  public BellSchedule getSchedule() {
    return schedule;
  }

  /**
   * Replaces the schedule (from the controller's screen) and syncs it to the clients.
   *
   * @param newSchedule the new schedule
   */
  public void setSchedule(BellSchedule newSchedule) {
    this.schedule = newSchedule == null ? BellSchedule.EMPTY : newSchedule;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  // --- links ---------------------------------------------------------------------------------

  /** The linked speakers and bells, read only. */
  public List<BlockPos> getLinks() {
    return Collections.unmodifiableList(links);
  }

  public boolean isLinked(BlockPos p) {
    return links.contains(p);
  }

  /**
   * Links a speaker or bell.
   *
   * @param p its position
   *
   * @return false if it was linked already or the controller is full
   */
  public boolean link(BlockPos p) {
    if (p == null || links.contains(p) || links.size() >= MAX_LINKS) {
      return false;
    }
    links.add(p.toImmutable());
    changed();
    return true;
  }

  public boolean unlink(BlockPos p) {
    boolean removed = links.remove(p);
    if (removed) {
      changed();
    }
    return removed;
  }

  public int clearLinks() {
    int n = links.size();
    links.clear();
    if (n > 0) {
      changed();
    }
    return n;
  }

  /** Drops links {@link BellRinger} found pointing at neither a speaker nor a bell. */
  void pruneLinks(List<BlockPos> stale) {
    if (!stale.isEmpty() && links.removeAll(stale)) {
      changed();
    }
  }

  private void changed() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  // --- ringing -------------------------------------------------------------------------------

  /**
   * Rings a tone now: through the linked speakers (and the controller's own), on the linked
   * bells if the tone rings them, and as a one-second redstone pulse from the controller. An
   * announcement's text is spoken a moment later, after its chime.
   *
   * @param tone the tone
   * @param text the announcement text, for a tone that speaks
   */
  public void ring(BellTone tone, String text) {
    if (world == null || world.isRemote) {
      return;
    }
    BellRinger.ring(this, tone);
    if (tone.speaks() && text != null && !text.isEmpty()) {
      pendingAnnouncement = text;
      announceAt = world.getTotalWorldTime() + ANNOUNCE_DELAY;
    }
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return schedule.isEmpty() && pendingAnnouncement == null;
  }

  @Override
  public long getTickRate() {
    return TICK_RATE;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }
    if (pendingAnnouncement != null && world.getTotalWorldTime() >= announceAt) {
      String text = pendingAnnouncement;
      pendingAnnouncement = null;
      BellRinger.announce(this, text);
    }
    if (schedule.isEmpty()) {
      lastMinute = -1;
      return;
    }
    int now = ClockTime.minuteOfDay(world.getWorldTime());
    if (lastMinute < 0) {
      lastMinute = now;
      return;
    }
    if (now == lastMinute) {
      return;
    }
    BellSchedule.Entry due = schedule.due(lastMinute, now);
    lastMinute = now;
    if (due != null) {
      ring(due.getTone(), due.getText());
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound compound) {
    List<BellSchedule.Entry> entries = new ArrayList<>();
    NBTTagList list = compound.getTagList(KEY_SCHEDULE, Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < list.tagCount() && i < BellSchedule.MAX_ENTRIES; i++) {
      NBTTagCompound tag = list.getCompoundTagAt(i);
      entries.add(new BellSchedule.Entry(tag.getShort(KEY_MINUTE),
          BellTone.byOrdinal(tag.getByte(KEY_TONE)), tag.getString(KEY_TEXT),
          !tag.hasKey(KEY_ENABLED) || tag.getBoolean(KEY_ENABLED)));
    }
    schedule = entries.isEmpty() ? BellSchedule.EMPTY : new BellSchedule(entries);
    links.clear();
    int[] xyz = compound.getIntArray(KEY_LINKS);
    for (int i = 0; i + 2 < xyz.length && links.size() < MAX_LINKS; i += 3) {
      links.add(new BlockPos(xyz[i], xyz[i + 1], xyz[i + 2]));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (!schedule.isEmpty()) {
      NBTTagList list = new NBTTagList();
      for (BellSchedule.Entry e : schedule.getEntries()) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort(KEY_MINUTE, (short) e.getMinute());
        tag.setByte(KEY_TONE, (byte) e.getTone().ordinal());
        if (!e.getText().isEmpty()) {
          tag.setString(KEY_TEXT, e.getText());
        }
        if (!e.isEnabled()) {
          tag.setBoolean(KEY_ENABLED, false);
        }
        list.appendTag(tag);
      }
      compound.setTag(KEY_SCHEDULE, list);
    }
    if (!links.isEmpty()) {
      int[] xyz = new int[links.size() * 3];
      for (int i = 0; i < links.size(); i++) {
        BlockPos p = links.get(i);
        xyz[i * 3] = p.getX();
        xyz[i * 3 + 1] = p.getY();
        xyz[i * 3 + 2] = p.getZ();
      }
      compound.setIntArray(KEY_LINKS, xyz);
    }
    return compound;
  }

  /** No baked model reads this tile entity; a sync never needs the chunk section rebuilt. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
