package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * The outdoor warning siren controller's state: the signal it is set to, its linked sirens, and
 * whether it runs the weekly test.
 *
 * <p>The weekly test sounds {@link SirenSignal#TEST} on every linked siren at noon on every
 * seventh in-game day, as towns test theirs at a set hour each week. The controller looks at the
 * clock once every {@link #CHECK_TICKS} and remembers the day it last tested, so a restart or a
 * time change does not test twice in a day.</p>
 *
 * @since 2026.9
 */
public class TileEntitySirenController extends AbstractTickableTileEntity implements
    ILinkedDeviceController {

  /** The choices a click cycles through: the four signals, cancel, and the weekly test toggle. */
  public enum Choice {
    ALERT, ATTACK, FIRE, TEST, CANCEL, WEEKLY_TEST
  }

  /** Ticks between looks at the clock for the weekly test. */
  public static final int CHECK_TICKS = 100;
  /** Noon, in the day's ticks. */
  private static final long NOON = 6000;

  private static final String CHOICE_KEY = "c";
  private static final String SIRENS_KEY = "s";
  private static final String WEEKLY_KEY = "w";
  private static final String LAST_TEST_KEY = "lt";

  private int choice;
  private final List<BlockPos> sirens = new ArrayList<>();
  private boolean weekly = true;
  private long lastTestDay = -1;

  public Choice getChoice() {
    return Choice.values()[Math.floorMod(choice, Choice.values().length)];
  }

  public Choice cycle() {
    choice = (choice + 1) % Choice.values().length;
    markDirty();
    return getChoice();
  }

  public boolean isWeekly() {
    return weekly;
  }

  public int getSirenCount() {
    return sirens.size();
  }

  /**
   * Carries out the current choice: sounds the signal on every linked siren, stops them, or turns
   * the weekly test on or off.
   */
  public void activate() {
    switch (getChoice()) {
      case ALERT:
        soundAll(SirenSignal.ALERT);
        break;
      case ATTACK:
        soundAll(SirenSignal.ATTACK);
        break;
      case FIRE:
        soundAll(SirenSignal.FIRE);
        break;
      case TEST:
        soundAll(SirenSignal.TEST);
        break;
      case CANCEL:
        soundAll(SirenSignal.NONE);
        break;
      default:
        weekly = !weekly;
        markDirty();
        break;
    }
  }

  /** Sounds a signal on every linked siren that is loaded, dropping any that is gone. */
  public void soundAll(SirenSignal signal) {
    sirens.removeIf(p -> world.isBlockLoaded(p)
        && !(world.getTileEntity(p) instanceof TileEntityWarningSiren));
    for (BlockPos p : sirens) {
      if (!world.isBlockLoaded(p)) {
        continue;
      }
      TileEntity te = world.getTileEntity(p);
      if (te instanceof TileEntityWarningSiren) {
        ((TileEntityWarningSiren) te).sound(signal);
      }
    }
    markDirty();
  }

  @Override
  public LinkResult link(Block block, BlockPos pos) {
    if (!(block instanceof BlockWarningSiren)) {
      return LinkResult.NOT_MINE;
    }
    if (sirens.contains(pos)) {
      return LinkResult.ALREADY_LINKED;
    }
    sirens.add(pos.toImmutable());
    markDirty();
    return LinkResult.LINKED;
  }

  @Override
  public String describe() {
    return "warning siren controller";
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return !weekly || sirens.isEmpty();
  }

  @Override
  public long getTickRate() {
    return CHECK_TICKS;
  }

  @Override
  public void onTick() {
    if (world.isRemote) {
      return;
    }
    long time = world.getWorldTime();
    long day = time / 24000;
    long ofDay = time % 24000;
    if (day % 7 == 0 && day != lastTestDay && ofDay >= NOON && ofDay < NOON + CHECK_TICKS * 2) {
      lastTestDay = day;
      soundAll(SirenSignal.TEST);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    choice = compound.getInteger(CHOICE_KEY);
    weekly = !compound.hasKey(WEEKLY_KEY) || compound.getBoolean(WEEKLY_KEY);
    lastTestDay = compound.hasKey(LAST_TEST_KEY) ? compound.getLong(LAST_TEST_KEY) : -1;
    sirens.clear();
    int[] packed = compound.getIntArray(SIRENS_KEY);
    for (int i = 0; i + 1 < packed.length; i += 2) {
      sirens.add(BlockPos.fromLong(((long) packed[i] << 32) | (packed[i + 1] & 0xFFFFFFFFL)));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(CHOICE_KEY, choice);
    compound.setBoolean(WEEKLY_KEY, weekly);
    compound.setLong(LAST_TEST_KEY, lastTestDay);
    int[] packed = new int[sirens.size() * 2];
    for (int i = 0; i < sirens.size(); i++) {
      long l = sirens.get(i).toLong();
      packed[2 * i] = (int) (l >>> 32);
      packed[2 * i + 1] = (int) l;
    }
    compound.setIntArray(SIRENS_KEY, packed);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    return 0;
  }
}
