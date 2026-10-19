package com.micatechnologies.minecraft.csm.healthcare.queue;

import com.micatechnologies.minecraft.csm.healthcare.nursecall.NurseCallNetwork;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * A dimension's patient queues, in its saved data ({@code data/csm_healthcare_queue.dat}): each
 * queue controller (a check-in kiosk or a desk's call console) with the number it is serving and
 * the now-serving displays linked to it.
 *
 * <p>Event-driven, as the nurse call network is: a click on a controller or
 * {@code /csmhealthcare queue next} advances its number and pushes it to its displays once, and
 * nothing polls. A display in a chunk that is not loaded is left as it is and catches up from
 * here when its chunk loads ({@link TileEntityNowServingDisplay#onLoad}).</p>
 *
 * @since 2026.10
 */
public class QueueNetwork extends WorldSavedData {

  /** The saved data's name. */
  public static final String NAME = "csm_healthcare_queue";

  /** The highest number before the queue wraps back to 1. */
  public static final int MAX_NUMBER = 999;

  /** controller -> the number it is serving (0: none yet) */
  private final Map<Long, Integer> numbers = new HashMap<>();
  /** controller -> its displays, in link order */
  private final Map<Long, List<Long>> displays = new LinkedHashMap<>();
  /** display -> its controller */
  private final Map<Long, Long> controllers = new HashMap<>();

  public QueueNetwork(String name) {
    super(name);
  }

  public QueueNetwork() {
    this(NAME);
  }

  /**
   * The queues of {@code world}'s dimension.
   *
   * @param world a server world
   *
   * @return its queues, made empty if it has none yet
   */
  public static QueueNetwork get(World world) {
    MapStorage storage = world.getPerWorldStorage();
    QueueNetwork net = (QueueNetwork) storage.getOrLoadData(QueueNetwork.class, NAME);
    if (net == null) {
      net = new QueueNetwork(NAME);
      storage.setData(NAME, net);
    }
    return net;
  }

  /** Whether the block at {@code pos} is a queue controller. */
  public static boolean isController(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockQueueController;
  }

  /** Whether the block at {@code pos} is a now-serving display. */
  public static boolean isDisplay(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockNowServingDisplay;
  }

  // --- links ---------------------------------------------------------------------------------

  /**
   * Links a display to a controller; a display linked elsewhere moves. The display shows the
   * controller's number at once.
   *
   * @param world      the world
   * @param controller the kiosk or console
   * @param display    the display
   *
   * @throws IllegalArgumentException with the reason, if the link cannot be made
   */
  public void link(World world, BlockPos controller, BlockPos display) {
    if (!isController(world, controller)) {
      throw new IllegalArgumentException("there is no check-in kiosk or queue console at "
          + NurseCallNetwork.describe(controller));
    }
    if (!isDisplay(world, display)) {
      throw new IllegalArgumentException("there is no now-serving display at "
          + NurseCallNetwork.describe(display));
    }
    long c = controller.toLong();
    long d = display.toLong();
    detach(d);
    controllers.put(d, c);
    displays.computeIfAbsent(c, k -> new ArrayList<>()).add(d);
    numbers.putIfAbsent(c, 0);
    markDirty();
    show(world, display, numberOf(controller));
  }

  /**
   * Unlinks a display from a controller.
   *
   * @return whether it was linked there
   */
  public boolean unlink(World world, BlockPos controller, BlockPos display) {
    long d = display.toLong();
    if (!Long.valueOf(controller.toLong()).equals(controllers.get(d))) {
      return false;
    }
    detach(d);
    markDirty();
    return true;
  }

  private void detach(long display) {
    Long c = controllers.remove(display);
    if (c != null) {
      List<Long> list = displays.get(c);
      if (list != null) {
        list.remove(Long.valueOf(display));
        if (list.isEmpty()) {
          displays.remove(c);
        }
      }
    }
  }

  /** Forgets a controller that is gone: its number, and its displays' links. */
  public void forgetController(long controller) {
    boolean changed = numbers.remove(controller) != null;
    List<Long> list = displays.remove(controller);
    if (list != null) {
      for (long d : list) {
        controllers.remove(d);
      }
      changed = true;
    }
    if (changed) {
      markDirty();
    }
  }

  /** Forgets a display that is gone. */
  public void forgetDisplay(long display) {
    if (controllers.containsKey(display)) {
      detach(display);
      markDirty();
    }
  }

  /**
   * Drops every controller and display that is no longer there (loaded chunks only).
   *
   * @return how many were dropped
   */
  public int prune(World world) {
    int dropped = 0;
    Set<Long> all = new HashSet<>(numbers.keySet());
    all.addAll(displays.keySet());
    for (long c : all) {
      BlockPos pos = BlockPos.fromLong(c);
      if (world.isBlockLoaded(pos) && !isController(world, pos)) {
        forgetController(c);
        dropped++;
      }
    }
    for (long d : new ArrayList<>(controllers.keySet())) {
      BlockPos pos = BlockPos.fromLong(d);
      if (world.isBlockLoaded(pos) && !isDisplay(world, pos)) {
        forgetDisplay(d);
        dropped++;
      }
    }
    return dropped;
  }

  /** The displays linked to a controller, in link order. */
  public List<Long> displaysOf(BlockPos controller) {
    List<Long> list = displays.get(controller.toLong());
    return list == null ? Collections.emptyList() : list;
  }

  /** The controller a display is linked to, or null. */
  @Nullable
  public BlockPos controllerOf(BlockPos display) {
    Long c = controllers.get(display.toLong());
    return c == null ? null : BlockPos.fromLong(c);
  }

  // --- numbers -------------------------------------------------------------------------------

  /** The number a controller is serving (0 before its first call). */
  public int numberOf(BlockPos controller) {
    Integer n = numbers.get(controller.toLong());
    return n == null ? 0 : n;
  }

  /**
   * Calls the next number at a controller and shows it on its displays.
   *
   * @return the number now served
   */
  public int advance(World world, BlockPos controller) {
    int n = numberOf(controller) % MAX_NUMBER + 1;
    set(world, controller, n);
    return n;
  }

  /** Sets the number a controller is serving and shows it on its displays. */
  public void set(World world, BlockPos controller, int number) {
    numbers.put(controller.toLong(), Math.max(0, Math.min(MAX_NUMBER, number)));
    markDirty();
    int n = numberOf(controller);
    for (long d : displaysOf(controller)) {
      show(world, BlockPos.fromLong(d), n);
    }
  }

  private static void show(World world, BlockPos display, int number) {
    if (!world.isBlockLoaded(display)) {
      return;
    }
    TileEntity te = world.getTileEntity(display);
    if (te instanceof TileEntityNowServingDisplay) {
      ((TileEntityNowServingDisplay) te).setNumber(number);
    }
  }

  // --- saving --------------------------------------------------------------------------------

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    numbers.clear();
    displays.clear();
    controllers.clear();
    NBTTagList queues = nbt.getTagList("queues", 10);
    for (int i = 0; i < queues.tagCount(); i++) {
      NBTTagCompound q = queues.getCompoundTagAt(i);
      long c = q.getLong("c");
      numbers.put(c, q.getInteger("n"));
      NBTTagList ds = q.getTagList("d", 4);
      List<Long> list = new ArrayList<>();
      for (int j = 0; j < ds.tagCount(); j++) {
        long d = ((NBTTagLong) ds.get(j)).getLong();
        list.add(d);
        controllers.put(d, c);
      }
      if (!list.isEmpty()) {
        displays.put(c, list);
      }
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
    NBTTagList queues = new NBTTagList();
    Set<Long> all = new HashSet<>(numbers.keySet());
    all.addAll(displays.keySet());
    for (long c : all) {
      NBTTagCompound q = new NBTTagCompound();
      q.setLong("c", c);
      q.setInteger("n", numbers.getOrDefault(c, 0));
      NBTTagList ds = new NBTTagList();
      for (long d : displays.getOrDefault(c, Collections.emptyList())) {
        ds.appendTag(new NBTTagLong(d));
      }
      q.setTag("d", ds);
      queues.appendTag(q);
    }
    nbt.setTag("queues", queues);
    return nbt;
  }
}
