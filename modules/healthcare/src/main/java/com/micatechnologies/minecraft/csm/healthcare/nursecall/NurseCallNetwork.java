package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareLight;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * A dimension's nurse call wiring, in its saved data ({@code data/csm_healthcare_nursecall.dat}):
 * which device calls through which dome light, which dome light (or lone device) is which room on
 * which master panel, and which devices are calling.
 *
 * <p>Two levels. A <b>room</b> is a {@link BlockNurseCallDomeLight dome light} linked to a
 * {@link BlockNurseCallMasterPanel master panel}, and the call devices in that room are linked to
 * the dome light; a device can also be linked straight to a panel, when it is a room of its own.
 * A panel shows {@link BlockNurseCallMasterPanel#ROOMS six rooms}, in the order they were
 * linked.</p>
 *
 * <p>A nurses' station's <b>monitors</b> ({@link BlockNurseStationMonitors}) are linked to one
 * panel each, any number to a panel, and show its rooms' calls: they take no room on it and are
 * kept apart from the rooms, and whenever the panel is brought up to date they are too.</p>
 *
 * <p>It is all event-driven: a device pressed, a call cancelled or a link made updates the dome
 * light and the panel it reaches, once, and nothing polls. A block in a chunk that is not loaded is
 * left as it is: its call is still recorded here, and it catches up the next time something in its
 * room changes.</p>
 *
 * @since 2026.10
 */
public class NurseCallNetwork extends WorldSavedData {

  /** The saved data's name. */
  public static final String NAME = "csm_healthcare_nursecall";

  /** What a linked position holds. */
  public enum Kind {
    PANEL, DOME, DEVICE, MONITOR
  }

  /** child -> parent */
  private final Map<Long, Long> parents = new HashMap<>();
  /** parent -> children, in link order */
  private final Map<Long, List<Long>> children = new LinkedHashMap<>();
  /** devices calling */
  private final Set<Long> calling = new HashSet<>();
  /** station monitor -> the panel it shows */
  private final Map<Long, Long> monitorPanel = new HashMap<>();
  /** panel -> the station monitors showing it, in link order */
  private final Map<Long, List<Long>> monitors = new LinkedHashMap<>();

  public NurseCallNetwork(String name) {
    super(name);
  }

  public NurseCallNetwork() {
    this(NAME);
  }

  /**
   * The network of {@code world}'s dimension.
   *
   * @param world a server world
   *
   * @return its network, made empty if it has none yet
   */
  public static NurseCallNetwork get(World world) {
    MapStorage storage = world.getPerWorldStorage();
    NurseCallNetwork net = (NurseCallNetwork) storage.getOrLoadData(NurseCallNetwork.class, NAME);
    if (net == null) {
      net = new NurseCallNetwork(NAME);
      storage.setData(NAME, net);
    }
    return net;
  }

  /**
   * What the block at {@code pos} is to the network.
   *
   * @param world the world
   * @param pos   a position
   *
   * @return its kind, or null if it is none of them
   */
  @Nullable
  public static Kind kindOf(World world, BlockPos pos) {
    Block b = world.getBlockState(pos).getBlock();
    if (b instanceof BlockNurseCallMasterPanel) {
      return Kind.PANEL;
    }
    if (b instanceof BlockNurseCallDomeLight) {
      return Kind.DOME;
    }
    if (b instanceof BlockNurseCallDevice) {
      return Kind.DEVICE;
    }
    if (b instanceof BlockNurseStationMonitors) {
      return Kind.MONITOR;
    }
    return null;
  }

  // --- links ---------------------------------------------------------------------------------

  /**
   * Links {@code child} to {@code parent}: a dome light or a device to a panel, a device to a
   * dome light, or a nurses' station monitor to a panel. A child already linked elsewhere moves.
   *
   * @param world  the world
   * @param parent the panel or dome light
   * @param child  the dome light or device
   *
   * @throws IllegalArgumentException with the reason, if the link cannot be made
   */
  public void link(World world, BlockPos parent, BlockPos child) {
    Kind pk = kindOf(world, parent);
    Kind ck = kindOf(world, child);
    if (pk == null || pk == Kind.DEVICE) {
      throw new IllegalArgumentException("there is no nurse call master panel or dome light at "
          + describe(parent));
    }
    if (ck == null || ck == Kind.PANEL) {
      throw new IllegalArgumentException("there is no nurse call device, dome light or nurses'"
          + " station monitor at " + describe(child));
    }
    if (ck == Kind.MONITOR) {
      if (pk != Kind.PANEL) {
        throw new IllegalArgumentException("a nurses' station monitor links to a master panel");
      }
      long p = parent.toLong();
      long c = child.toLong();
      if (!Long.valueOf(p).equals(monitorPanel.get(c))) {
        detachMonitor(c);
        monitorPanel.put(c, p);
        monitors.computeIfAbsent(p, k -> new ArrayList<>()).add(c);
        markDirty();
      }
      refresh(world, child);
      return;
    }
    if (pk == Kind.DOME && ck == Kind.DOME) {
      throw new IllegalArgumentException("a dome light links to a master panel, not to another"
          + " dome light");
    }
    long p = parent.toLong();
    long c = child.toLong();
    if (Long.valueOf(p).equals(parents.get(c))) {
      return;
    }
    if (pk == Kind.PANEL && childrenOf(p).size() >= BlockNurseCallMasterPanel.ROOMS) {
      throw new IllegalArgumentException("the panel at " + describe(parent) + " already shows "
          + BlockNurseCallMasterPanel.ROOMS + " rooms; use another panel");
    }
    Long old = parents.get(c);
    detach(c);
    parents.put(c, p);
    children.computeIfAbsent(p, k -> new ArrayList<>()).add(c);
    markDirty();
    if (old != null) {
      refresh(world, BlockPos.fromLong(old));
    }
    refresh(world, child);
    refresh(world, parent);
  }

  /**
   * Unlinks {@code child} from {@code parent}.
   *
   * @param world  the world
   * @param parent the panel or dome light
   * @param child  the dome light or device
   *
   * @return whether it was linked there
   */
  public boolean unlink(World world, BlockPos parent, BlockPos child) {
    long p = parent.toLong();
    long c = child.toLong();
    if (Long.valueOf(p).equals(monitorPanel.get(c))) {
      detachMonitor(c);
      markDirty();
      refresh(world, child);
      return true;
    }
    if (!Long.valueOf(p).equals(parents.get(c))) {
      return false;
    }
    detach(c);
    markDirty();
    refresh(world, parent);
    return true;
  }

  private void detachMonitor(long monitor) {
    Long p = monitorPanel.remove(monitor);
    if (p != null) {
      List<Long> list = monitors.get(p);
      if (list != null) {
        list.remove(Long.valueOf(monitor));
        if (list.isEmpty()) {
          monitors.remove(p);
        }
      }
    }
  }

  private void detach(long child) {
    Long p = parents.remove(child);
    if (p != null) {
      List<Long> kids = children.get(p);
      if (kids != null) {
        kids.remove(Long.valueOf(child));
        if (kids.isEmpty()) {
          children.remove(p);
        }
      }
    }
  }

  /**
   * Removes every link to or from a position that no longer holds what it was linked as.
   * Positions in chunks that are not loaded are kept.
   *
   * @param world the world
   *
   * @return how many positions were dropped
   */
  public int prune(World world) {
    Set<Long> all = new HashSet<>(parents.keySet());
    all.addAll(children.keySet());
    all.addAll(monitorPanel.keySet());
    all.addAll(monitors.keySet());
    int dropped = 0;
    for (long l : all) {
      BlockPos pos = BlockPos.fromLong(l);
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      Kind k = kindOf(world, pos);
      boolean asChild = parents.containsKey(l);
      boolean asParent = children.containsKey(l);
      boolean shown = monitors.containsKey(l);
      boolean asMonitor = monitorPanel.containsKey(l);
      boolean ok = k != null && (!asChild || k != Kind.PANEL && k != Kind.MONITOR)
          && (!asParent || k == Kind.PANEL || k == Kind.DOME) && (!shown || k == Kind.PANEL)
          && (!asMonitor || k == Kind.MONITOR);
      if (!ok) {
        forget(l);
        dropped++;
      }
    }
    if (dropped > 0) {
      markDirty();
    }
    return dropped;
  }

  private void forget(long l) {
    detachMonitor(l);
    List<Long> shown = monitors.remove(l);
    if (shown != null) {
      for (long m : shown) {
        monitorPanel.remove(m);
      }
    }
    detach(l);
    List<Long> kids = children.remove(l);
    if (kids != null) {
      for (long k : kids) {
        parents.remove(k);
      }
    }
    calling.remove(l);
  }

  /** The children of a parent, in link order (rooms of a panel, devices of a dome light). */
  public List<Long> childrenOf(long parent) {
    List<Long> kids = children.get(parent);
    return kids == null ? Collections.emptyList() : kids;
  }

  /**
   * The master panel a nurses' station monitor shows, or null.
   *
   * @param monitor the monitor
   *
   * @return its panel
   */
  @Nullable
  public BlockPos panelOfMonitor(BlockPos monitor) {
    Long p = monitorPanel.get(monitor.toLong());
    return p == null ? null : BlockPos.fromLong(p);
  }

  /**
   * The nurses' station monitors showing a panel, in link order.
   *
   * @param panel the panel
   *
   * @return its monitors
   */
  public List<Long> monitorsOf(long panel) {
    List<Long> list = monitors.get(panel);
    return list == null ? Collections.emptyList() : list;
  }

  /**
   * Which of a panel's six rooms are calling, one bit a room, from the network alone (the panel
   * need not be loaded).
   *
   * @param panel the panel
   *
   * @return the mask
   */
  public int panelMask(long panel) {
    int mask = 0;
    List<Long> rooms = childrenOf(panel);
    for (int i = 0; i < rooms.size() && i < BlockNurseCallMasterPanel.ROOMS; i++) {
      if (roomCalling(rooms.get(i))) {
        mask |= 1 << i;
      }
    }
    return mask;
  }

  /** The parent of a child, or null. */
  @Nullable
  public BlockPos parentOf(BlockPos child) {
    Long p = parents.get(child.toLong());
    return p == null ? null : BlockPos.fromLong(p);
  }

  // --- calls ---------------------------------------------------------------------------------

  /**
   * Whether the room at {@code room} (a dome light, or a device linked straight to a panel) has a
   * call: it is calling itself, or a device linked to it is.
   *
   * @param room the room's position
   *
   * @return true if it is calling
   */
  public boolean roomCalling(long room) {
    if (calling.contains(room)) {
      return true;
    }
    for (long k : childrenOf(room)) {
      if (calling.contains(k)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether the device at {@code pos} is recorded as calling.
   *
   * @param pos the device
   *
   * @return true if it is
   */
  public boolean isCalling(BlockPos pos) {
    return calling.contains(pos.toLong());
  }

  /**
   * Records a device's call starting or ending, and updates its room's dome light and panel.
   *
   * @param world   the world
   * @param device  the device
   * @param calling whether it is calling now
   */
  public void setCalling(World world, BlockPos device, boolean calling) {
    long d = device.toLong();
    boolean changed = calling ? this.calling.add(d) : this.calling.remove(d);
    if (!changed) {
      return;
    }
    markDirty();
    Long p = parents.get(d);
    if (p != null) {
      refresh(world, BlockPos.fromLong(p));
    }
  }

  /**
   * Cancels every call in a room: each calling device linked to the dome light at {@code dome},
   * as a nurse does at the room.
   *
   * @param world the world
   * @param dome  the dome light
   *
   * @return how many calls were cancelled
   */
  public int cancelRoom(World world, BlockPos dome) {
    int n = 0;
    for (long k : new ArrayList<>(childrenOf(dome.toLong()))) {
      if (calling.remove(k)) {
        n++;
        BlockPos pos = BlockPos.fromLong(k);
        if (world.isBlockLoaded(pos)) {
          IBlockState s = world.getBlockState(pos);
          if (s.getBlock() instanceof BlockNurseCallDevice && s.getValue(BlockNurseCallDevice.CALLING)) {
            world.setBlockState(pos, s.withProperty(BlockNurseCallDevice.CALLING, false), 3);
          }
        }
      }
    }
    if (n > 0) {
      markDirty();
      refresh(world, dome);
    }
    return n;
  }

  /**
   * Brings the block at {@code pos} up to date with the network -- a dome light's lamp, a panel's
   * room lamps and the station monitors showing them, a monitor's room lamps -- and then whatever
   * it reports to.
   *
   * @param world the world
   * @param pos   a dome light, panel or station monitor (anything else is left alone)
   */
  public void refresh(World world, BlockPos pos) {
    if (!world.isBlockLoaded(pos)) {
      // a panel out of loaded chunks still has its monitors brought up to date
      for (long m : monitorsOf(pos.toLong())) {
        refresh(world, BlockPos.fromLong(m));
      }
      return;
    }
    IBlockState s = world.getBlockState(pos);
    Block b = s.getBlock();
    if (b instanceof BlockNurseCallDomeLight) {
      int state = s.getValue(BlockHealthcareLight.STATE);
      if (state == BlockHealthcareLight.RS_OFF || state == BlockHealthcareLight.RS_ON) {
        boolean lit = roomCalling(pos.toLong()) || world.isBlockPowered(pos);
        int want = lit ? BlockHealthcareLight.RS_ON : BlockHealthcareLight.RS_OFF;
        if (want != state) {
          world.setBlockState(pos, s.withProperty(BlockHealthcareLight.STATE, want), 3);
        }
      }
      Long p = parents.get(pos.toLong());
      if (p != null) {
        refresh(world, BlockPos.fromLong(p));
      }
    } else if (b instanceof BlockNurseStationMonitors) {
      Long p = monitorPanel.get(pos.toLong());
      int mask = p == null ? 0 : panelMask(p);
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityNurseStationMonitor
          && ((TileEntityNurseStationMonitor) te).getRooms() != mask) {
        ((TileEntityNurseStationMonitor) te).setRooms(mask);
      }
    } else if (b instanceof BlockNurseCallMasterPanel) {
      int mask = panelMask(pos.toLong());
      for (long m : monitorsOf(pos.toLong())) {
        refresh(world, BlockPos.fromLong(m));
      }
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityNurseCallPanel) {
        int before = ((TileEntityNurseCallPanel) te).getRooms();
        if (before != mask) {
          ((TileEntityNurseCallPanel) te).setRooms(mask);
          if ((mask & ~before) != 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 1.0F,
                1.2F);
          }
          if ((before == 0) != (mask == 0)) {
            // its redstone signal follows "any room calling"
            world.notifyNeighborsOfStateChange(pos, b, false);
          }
        }
      }
    }
  }

  // --- saving --------------------------------------------------------------------------------

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    parents.clear();
    children.clear();
    calling.clear();
    NBTTagList links = nbt.getTagList("links", 10);
    for (int i = 0; i < links.tagCount(); i++) {
      NBTTagCompound l = links.getCompoundTagAt(i);
      long p = l.getLong("p");
      NBTTagList kids = l.getTagList("c", 4);
      List<Long> list = new ArrayList<>();
      for (int j = 0; j < kids.tagCount(); j++) {
        long c = ((NBTTagLong) kids.get(j)).getLong();
        list.add(c);
        parents.put(c, p);
      }
      if (!list.isEmpty()) {
        children.put(p, list);
      }
    }
    NBTTagList calls = nbt.getTagList("calling", 4);
    for (int i = 0; i < calls.tagCount(); i++) {
      calling.add(((NBTTagLong) calls.get(i)).getLong());
    }
    monitorPanel.clear();
    monitors.clear();
    NBTTagList shown = nbt.getTagList("monitors", 10);
    for (int i = 0; i < shown.tagCount(); i++) {
      NBTTagCompound l = shown.getCompoundTagAt(i);
      long p = l.getLong("p");
      NBTTagList list = l.getTagList("m", 4);
      List<Long> ms = new ArrayList<>();
      for (int j = 0; j < list.tagCount(); j++) {
        long m = ((NBTTagLong) list.get(j)).getLong();
        ms.add(m);
        monitorPanel.put(m, p);
      }
      if (!ms.isEmpty()) {
        monitors.put(p, ms);
      }
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
    NBTTagList links = new NBTTagList();
    for (Map.Entry<Long, List<Long>> e : children.entrySet()) {
      NBTTagCompound l = new NBTTagCompound();
      l.setLong("p", e.getKey());
      NBTTagList kids = new NBTTagList();
      for (long c : e.getValue()) {
        kids.appendTag(new NBTTagLong(c));
      }
      l.setTag("c", kids);
      links.appendTag(l);
    }
    nbt.setTag("links", links);
    NBTTagList calls = new NBTTagList();
    for (long c : calling) {
      calls.appendTag(new NBTTagLong(c));
    }
    nbt.setTag("calling", calls);
    NBTTagList shown = new NBTTagList();
    for (Map.Entry<Long, List<Long>> e : monitors.entrySet()) {
      NBTTagCompound l = new NBTTagCompound();
      l.setLong("p", e.getKey());
      NBTTagList list = new NBTTagList();
      for (long m : e.getValue()) {
        list.appendTag(new NBTTagLong(m));
      }
      l.setTag("m", list);
      shown.appendTag(l);
    }
    nbt.setTag("monitors", shown);
    return nbt;
  }

  /**
   * A position as {@code x y z}.
   *
   * @param pos the position
   *
   * @return its text
   */
  public static String describe(BlockPos pos) {
    return pos.getX() + " " + pos.getY() + " " + pos.getZ();
  }
}
