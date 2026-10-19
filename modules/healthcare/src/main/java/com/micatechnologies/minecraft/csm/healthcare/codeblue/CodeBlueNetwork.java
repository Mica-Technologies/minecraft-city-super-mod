package com.micatechnologies.minecraft.csm.healthcare.codeblue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * A dimension's code blue wiring, in its saved data ({@code data/csm_healthcare_codeblue.dat}):
 * which code blue buttons call which annunciators, and which buttons are active.
 *
 * <p>Many to many: a button can call the annunciators of a whole floor, and an annunciator can
 * answer every button of its unit. It is event-driven, as the nurse call is: a button pressed or
 * reset, or a link made, updates the annunciators it reaches once, and nothing polls. An
 * annunciator in a chunk that is not loaded is left alone and catches up the next time it is
 * refreshed (its neighbour changing, or a button of its pressed or reset).</p>
 *
 * <p>Redstone works beside this, with no links at all: a button gives a signal while active, and
 * an annunciator is active while it is powered.</p>
 *
 * @since 2026.10
 */
public class CodeBlueNetwork extends WorldSavedData {

  /** The saved data's name. */
  public static final String NAME = "csm_healthcare_codeblue";

  /** button -> annunciators, in link order */
  private final Map<Long, Set<Long>> calls = new LinkedHashMap<>();
  /** buttons active */
  private final Set<Long> active = new HashSet<>();

  public CodeBlueNetwork(String name) {
    super(name);
  }

  public CodeBlueNetwork() {
    this(NAME);
  }

  /**
   * The network of {@code world}'s dimension.
   *
   * @param world a server world
   * @return its network, made empty if it has none yet
   */
  public static CodeBlueNetwork get(World world) {
    MapStorage storage = world.getPerWorldStorage();
    CodeBlueNetwork net = (CodeBlueNetwork) storage.getOrLoadData(CodeBlueNetwork.class, NAME);
    if (net == null) {
      net = new CodeBlueNetwork(NAME);
      storage.setData(NAME, net);
    }
    return net;
  }

  private static boolean isButton(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockCodeBlueButton;
  }

  private static boolean isAnnunciator(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockCodeBlueAnnunciator;
  }

  // --- links ---------------------------------------------------------------------------------

  /**
   * Links a button to an annunciator, so the button's code sounds there.
   *
   * @throws IllegalArgumentException with the reason, if the link cannot be made
   */
  public void link(World world, BlockPos button, BlockPos annunciator) {
    if (!isButton(world, button)) {
      throw new IllegalArgumentException("there is no code blue button at " + describe(button));
    }
    if (!isAnnunciator(world, annunciator)) {
      throw new IllegalArgumentException("there is no code blue annunciator at "
          + describe(annunciator));
    }
    if (calls.computeIfAbsent(button.toLong(), k -> new LinkedHashSet<>())
        .add(annunciator.toLong())) {
      markDirty();
      refresh(world, annunciator);
    }
  }

  /**
   * Unlinks a button from an annunciator.
   *
   * @return whether they were linked
   */
  public boolean unlink(World world, BlockPos button, BlockPos annunciator) {
    Set<Long> to = calls.get(button.toLong());
    if (to == null || !to.remove(annunciator.toLong())) {
      return false;
    }
    if (to.isEmpty()) {
      calls.remove(button.toLong());
    }
    markDirty();
    refresh(world, annunciator);
    return true;
  }

  /** The annunciators a button calls, in link order. */
  public List<BlockPos> annunciatorsOf(BlockPos button) {
    Set<Long> to = calls.get(button.toLong());
    if (to == null) {
      return Collections.emptyList();
    }
    List<BlockPos> out = new ArrayList<>();
    for (long l : to) {
      out.add(BlockPos.fromLong(l));
    }
    return out;
  }

  /** The buttons that call an annunciator. */
  public List<BlockPos> buttonsOf(BlockPos annunciator) {
    long a = annunciator.toLong();
    List<BlockPos> out = new ArrayList<>();
    for (Map.Entry<Long, Set<Long>> e : calls.entrySet()) {
      if (e.getValue().contains(a)) {
        out.add(BlockPos.fromLong(e.getKey()));
      }
    }
    return out;
  }

  /**
   * Drops every link to or from a position that no longer holds a button or an annunciator.
   * Positions in chunks that are not loaded are kept.
   *
   * @return how many positions were dropped
   */
  public int prune(World world) {
    int dropped = 0;
    Set<Long> gone = new HashSet<>();
    for (Map.Entry<Long, Set<Long>> e : calls.entrySet()) {
      BlockPos b = BlockPos.fromLong(e.getKey());
      if (world.isBlockLoaded(b) && !isButton(world, b)) {
        gone.add(e.getKey());
      }
      for (long a : e.getValue()) {
        BlockPos p = BlockPos.fromLong(a);
        if (world.isBlockLoaded(p) && !isAnnunciator(world, p)) {
          gone.add(a);
        }
      }
    }
    for (long l : gone) {
      if (calls.remove(l) != null) {
        dropped++;
      }
      active.remove(l);
      boolean any = false;
      for (Set<Long> to : calls.values()) {
        any |= to.remove(l);
      }
      if (any) {
        dropped++;
      }
    }
    calls.values().removeIf(Set::isEmpty);
    if (!gone.isEmpty()) {
      markDirty();
    }
    return dropped;
  }

  // --- the code ------------------------------------------------------------------------------

  /** Whether a button is recorded as active. */
  public boolean isActive(BlockPos button) {
    return active.contains(button.toLong());
  }

  /** Whether any button linked to an annunciator is active. */
  public boolean annunciatorCalled(BlockPos annunciator) {
    long a = annunciator.toLong();
    for (long b : active) {
      Set<Long> to = calls.get(b);
      if (to != null && to.contains(a)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Records a button going active or being reset, and updates the annunciators it calls.
   */
  public void setActive(World world, BlockPos button, boolean on) {
    long b = button.toLong();
    boolean changed = on ? active.add(b) : active.remove(b);
    if (!changed) {
      return;
    }
    markDirty();
    for (BlockPos a : annunciatorsOf(button)) {
      refresh(world, a);
    }
  }

  /**
   * Resets the code: every active button linked to the annunciator at {@code pos} (or, for a
   * button, the button itself and every active button sharing an annunciator with it) goes back
   * to ready, and their annunciators fall silent unless redstone still holds them.
   *
   * @return how many buttons were reset
   */
  public int reset(World world, BlockPos pos) {
    Set<Long> buttons = new LinkedHashSet<>();
    Set<Long> annunciators = new LinkedHashSet<>();
    if (isButton(world, pos)) {
      buttons.add(pos.toLong());
      for (BlockPos a : annunciatorsOf(pos)) {
        annunciators.add(a.toLong());
      }
    } else {
      annunciators.add(pos.toLong());
    }
    for (long a : annunciators) {
      for (BlockPos b : buttonsOf(BlockPos.fromLong(a))) {
        buttons.add(b.toLong());
      }
    }
    int n = 0;
    for (long l : buttons) {
      BlockPos b = BlockPos.fromLong(l);
      boolean was = active.remove(l);
      if (world.isBlockLoaded(b)) {
        IBlockState s = world.getBlockState(b);
        if (s.getBlock() instanceof BlockCodeBlueButton && s.getValue(BlockCodeBlueButton.ACTIVE)) {
          BlockCodeBlueButton.setActive(world, b, s, false);
          was = true;
        }
      }
      if (was) {
        n++;
      }
    }
    markDirty();
    // every annunciator any of those buttons calls, not only the one reset at
    for (long l : buttons) {
      for (BlockPos a : annunciatorsOf(BlockPos.fromLong(l))) {
        annunciators.add(a.toLong());
      }
    }
    for (long a : annunciators) {
      refresh(world, BlockPos.fromLong(a));
    }
    if (!annunciators.contains(pos.toLong()) && isAnnunciator(world, pos)) {
      refresh(world, pos);
    }
    return n;
  }

  /** Brings an annunciator up to date: active while called by a button or powered. */
  public void refresh(World world, BlockPos annunciator) {
    if (!world.isBlockLoaded(annunciator)) {
      return;
    }
    IBlockState s = world.getBlockState(annunciator);
    if (s.getBlock() instanceof BlockCodeBlueAnnunciator) {
      ((BlockCodeBlueAnnunciator) s.getBlock()).update(world, annunciator, s,
          annunciatorCalled(annunciator));
    }
  }

  // --- saving --------------------------------------------------------------------------------

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    calls.clear();
    active.clear();
    NBTTagList links = nbt.getTagList("links", 10);
    for (int i = 0; i < links.tagCount(); i++) {
      NBTTagCompound l = links.getCompoundTagAt(i);
      NBTTagList to = l.getTagList("a", 4);
      Set<Long> set = new LinkedHashSet<>();
      for (int j = 0; j < to.tagCount(); j++) {
        set.add(((NBTTagLong) to.get(j)).getLong());
      }
      if (!set.isEmpty()) {
        calls.put(l.getLong("b"), set);
      }
    }
    NBTTagList on = nbt.getTagList("active", 4);
    for (int i = 0; i < on.tagCount(); i++) {
      active.add(((NBTTagLong) on.get(i)).getLong());
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
    NBTTagList links = new NBTTagList();
    for (Map.Entry<Long, Set<Long>> e : calls.entrySet()) {
      NBTTagCompound l = new NBTTagCompound();
      l.setLong("b", e.getKey());
      NBTTagList to = new NBTTagList();
      for (long a : e.getValue()) {
        to.appendTag(new NBTTagLong(a));
      }
      l.setTag("a", to);
      links.appendTag(l);
    }
    nbt.setTag("links", links);
    NBTTagList on = new NBTTagList();
    for (long b : active) {
      on.appendTag(new NBTTagLong(b));
    }
    nbt.setTag("active", on);
    return nbt;
  }

  /** A position as {@code x y z}. */
  public static String describe(BlockPos pos) {
    return pos.getX() + " " + pos.getY() + " " + pos.getZ();
  }
}
