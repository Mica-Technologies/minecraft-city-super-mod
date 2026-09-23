package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * The station alerting controller's state: the zone it will dispatch, its linked devices, and
 * where a dispatch is in its sequence.
 *
 * <p>A dispatch runs the pre-alert warble from every speaker, then the zone's tones, and turns on
 * every light and relay at once, so the bay doors start opening while the tones are still going.
 * {@link #CLEARANCE_TICKS} in, the bay clearance lights go green. After {@link #HOLD_TICKS} (or a
 * reset) everything goes back off. Devices are set only when the sequence changes step, and
 * devices in unloaded chunks are skipped, so the controller costs nothing between calls.</p>
 *
 * @since 2026.9
 */
public class TileEntityStationAlertController extends AbstractTickableTileEntity
    implements ILinkedDeviceController {

  /** The zones, in the order a click cycles them. Their tones are the sounds named here. */
  public enum Zone {
    ENGINE(LifeSafetySounds.STATION_TONE_ENGINE),
    LADDER(LifeSafetySounds.STATION_TONE_LADDER),
    MEDIC(LifeSafetySounds.STATION_TONE_MEDIC),
    BATTALION(LifeSafetySounds.STATION_TONE_BATTALION),
    ALL_CALL(LifeSafetySounds.STATION_TONE_ALL_CALL);

    public final LifeSafetySounds tone;

    Zone(LifeSafetySounds tone) {
      this.tone = tone;
    }
  }

  /** When the zone's tones start, after the pre-alert warble. */
  public static final int TONE_TICKS = 40;
  /** When the bay clearance lights go green: the doors have had ten seconds. */
  public static final int CLEARANCE_TICKS = 200;
  /** How long an alert holds before it resets itself. */
  public static final int HOLD_TICKS = 1200;

  private static final String ZONE_KEY = "z";
  private static final String DEVICES_KEY = "d";
  private static final String ELAPSED_KEY = "t";

  private int zone;
  private final List<BlockPos> devices = new ArrayList<>();
  /** Ticks since the dispatch, or -1 when idle. */
  private int elapsed = -1;

  public Zone getZone() {
    return Zone.values()[Math.floorMod(zone, Zone.values().length)];
  }

  public Zone cycleZone() {
    zone = (zone + 1) % Zone.values().length;
    markDirty();
    return getZone();
  }

  public boolean isAlerting() {
    return elapsed >= 0;
  }

  /**
   * Adds a device.
   *
   * @return false if it was already linked
   */
  public boolean addDevice(BlockPos pos) {
    if (devices.contains(pos)) {
      return false;
    }
    devices.add(pos.toImmutable());
    markDirty();
    return true;
  }

  @Override
  public LinkResult link(Block block, BlockPos pos) {
    if (!(block instanceof BlockStationAlertDevice)) {
      return LinkResult.NOT_MINE;
    }
    return addDevice(pos) ? LinkResult.LINKED : LinkResult.ALREADY_LINKED;
  }

  @Override
  public String describe() {
    return "station alerting controller";
  }

  public int getDeviceCount() {
    return devices.size();
  }

  /** Starts a dispatch of the current zone, or restarts one in progress. */
  public void dispatch() {
    elapsed = 0;
    play(LifeSafetySounds.STATION_PREALERT);
    setDevices(true, false);
    markDirty();
  }

  /** Ends an alert now. */
  public void reset() {
    if (elapsed < 0) {
      return;
    }
    elapsed = -1;
    setDevices(false, true);
    markDirty();
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return elapsed < 0;
  }

  @Override
  public long getTickRate() {
    return 1;
  }

  @Override
  public void onTick() {
    if (elapsed < 0 || world.isRemote) {
      return;
    }
    elapsed++;
    if (elapsed == TONE_TICKS) {
      play(getZone().tone);
    } else if (elapsed == CLEARANCE_TICKS) {
      setDevices(true, true);
    } else if (elapsed >= HOLD_TICKS) {
      reset();
    }
  }

  private void play(LifeSafetySounds sound) {
    SoundEvent event = sound.getSoundEvent();
    if (event == null || world == null || world.isRemote) {
      return;
    }
    for (BlockPos pos : devices) {
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      IBlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof BlockStationAlertDevice
          && ((BlockStationAlertDevice) state.getBlock()).getKind()
          == BlockStationAlertDevice.Kind.SPEAKER) {
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 2.0F, 1.0F);
      }
    }
  }

  /**
   * Sets every loaded device's state: lights, relays and speakers to {@code on}, clearance lights
   * to {@code on} only if {@code clearance}.
   */
  private void setDevices(boolean on, boolean clearance) {
    if (world == null || world.isRemote) {
      return;
    }
    devices.removeIf(pos -> world.isBlockLoaded(pos)
        && !(world.getBlockState(pos).getBlock() instanceof BlockStationAlertDevice));
    for (BlockPos pos : devices) {
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      IBlockState state = world.getBlockState(pos);
      BlockStationAlertDevice device = (BlockStationAlertDevice) state.getBlock();
      boolean want = device.getKind() == BlockStationAlertDevice.Kind.CLEARANCE
          ? on && clearance : on;
      if (state.getValue(BlockStationAlertDevice.ACTIVE) != want) {
        world.setBlockState(pos, state.withProperty(BlockStationAlertDevice.ACTIVE, want), 3);
      }
    }
    IBlockState self = world.getBlockState(getPos());
    if (self.getBlock() instanceof BlockStationAlertController
        && self.getValue(BlockStationAlertController.ACTIVE) != on) {
      world.setBlockState(getPos(), self.withProperty(BlockStationAlertController.ACTIVE, on), 3);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    zone = compound.getInteger(ZONE_KEY);
    elapsed = compound.hasKey(ELAPSED_KEY) ? compound.getInteger(ELAPSED_KEY) : -1;
    devices.clear();
    for (long l : readLongs(compound)) {
      devices.add(BlockPos.fromLong(l));
    }
  }

  private static long[] readLongs(NBTTagCompound compound) {
    int[] packed = compound.getIntArray(DEVICES_KEY);
    long[] out = new long[packed.length / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = ((long) packed[2 * i] << 32) | (packed[2 * i + 1] & 0xFFFFFFFFL);
    }
    return out;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(ZONE_KEY, zone);
    compound.setInteger(ELAPSED_KEY, elapsed);
    int[] packed = new int[devices.size() * 2];
    for (int i = 0; i < devices.size(); i++) {
      long l = devices.get(i).toLong();
      packed[2 * i] = (int) (l >>> 32);
      packed[2 * i + 1] = (int) l;
    }
    compound.setIntArray(DEVICES_KEY, packed);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    return 0;
  }
}
