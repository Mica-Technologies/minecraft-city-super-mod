package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * What a primary and a zone thermostat share: setpoints, vents, and the reading and status the
 * simulation reports each second.
 *
 * <p>A thermostat no longer measures or decides anything itself. {@link HvacSystemControl} runs
 * the control for the whole system on the server and calls {@link #applyControl}, and this class
 * keeps the result and syncs it to clients. The temperature it shows is its region's temperature
 * in the simulation -- the same number the HUD is sent for the same spot.</p>
 *
 * <p>It is an anchor of the simulation: loading registers it, unloading unregisters it after
 * capturing the room's temperature, and that temperature is saved with it. When the chunk loads
 * again the room is found again at that temperature, so a room that was held at its setpoint does
 * not have to be reheated just because nobody was nearby.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public abstract class TileEntityHvacThermostatBase extends AbstractTileEntity
    implements IHvacThermostatDisplay {

  // Short NBT keys; LEGACY_* are read so worlds saved before the short keys still load.
  static final String NBT_TARGET_TEMP_LOW = "tLo";
  static final String LEGACY_NBT_TARGET_TEMP_LOW = "targetTempLow";
  static final String NBT_TARGET_TEMP_HIGH = "tHi";
  static final String LEGACY_NBT_TARGET_TEMP_HIGH = "targetTempHigh";
  static final String NBT_IS_CALLING = "cL";
  static final String LEGACY_NBT_IS_CALLING = "isCalling";
  static final String NBT_CALLING_MODE = "cM";
  static final String LEGACY_NBT_CALLING_MODE = "callingMode";
  static final String NBT_BLOCKED_MODE = "bM";
  static final String NBT_OUTPUT = "eff";
  static final String LEGACY_NBT_OUTPUT = "efficiency";
  static final String NBT_CURRENT_TEMP = "cT";
  static final String LEGACY_NBT_CURRENT_TEMP = "currentTemp";
  static final String NBT_LINKED_VENTS = "lV";
  static final String LEGACY_NBT_LINKED_VENTS = "linkedVents";
  static final String NBT_FLAGS = "sF";
  static final String NBT_CAPACITY = "cap";
  static final String NBT_POWERED_UNITS = "pU";
  static final String NBT_TOTAL_UNITS = "tU";
  static final String NBT_TRIM_HEAT = "trH";
  static final String NBT_TRIM_COOL = "trC";
  /** Retired keys (the ramp accumulator) removed on load. */
  static final String OLD_NBT_RAMP_TICKS = "rT";
  static final String LEGACY_OLD_NBT_RAMP_TICKS = "rampTicks";

  static final int DEFAULT_LOW = 65;
  static final int DEFAULT_HIGH = 80;

  protected int targetTempLow = DEFAULT_LOW;
  protected int targetTempHigh = DEFAULT_HIGH;
  protected float currentTemperature = 72.0f;
  /** True once {@link #currentTemperature} came from a save or the simulation. */
  protected boolean temperatureKnown;
  protected boolean isCalling;
  protected int callingMode = HvacStatus.MODE_IDLE;
  protected int blockedMode = HvacStatus.MODE_IDLE;
  protected int outputPercent;
  protected int statusFlags;
  /** Capacity as a percentage of load; -1 when there is no load to compare with. */
  protected int capacityPercent = -1;
  protected int poweredUnits;
  protected int totalUnits;

  /** Learned offset of the vent regions' heating target; see {@link HvacSystemControl}. */
  float trimHeat;
  /** Learned offset of the vent regions' cooling target. */
  float trimCool;

  /** Simulation step in which a system last ran this thermostat. */
  transient long lastRunStep = -1;

  /** Temperature last marked for saving, so the chunk is marked dirty as it drifts. */
  private transient float lastSavedTemperature = Float.NaN;

  protected final List<BlockPos> linkedVents = new ArrayList<>();

  /** The simulation's anchor kind for this thermostat. */
  protected abstract int anchorKind();

  // region Simulation anchor

  @Override
  public void onLoad() {
    super.onLoad();
    HvacThermalWorld w = HvacThermal.get(world);
    if (w != null) {
      w.registerAnchor(pos, anchorKind(), temperatureKnown ? currentTemperature : Float.NaN);
    }
  }

  @Override
  public void onChunkUnload() {
    detachFromSimulation();
    super.onChunkUnload();
  }

  @Override
  public void invalidate() {
    detachFromSimulation();
    super.invalidate();
  }

  private void detachFromSimulation() {
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      ThermalAnchor a = w.anchor(pos);
      if (a != null) {
        float t = a.temperature();
        if (!Float.isNaN(t)) {
          currentTemperature = t;
          temperatureKnown = true;
        }
      }
      w.unregisterAnchor(pos);
    }
  }

  /**
   * The temperature to save: the live region temperature when the thermostat is in a room, else
   * what it is showing (the biome's outdoors, or the saved value while its room loads).
   */
  private float temperatureForSave() {
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      ThermalAnchor a = w.anchor(pos);
      if (a != null && a.space != null) {
        return a.temperature();
      }
    }
    return currentTemperature;
  }

  /**
   * Takes the result of this step's control. Syncs to clients when anything they display changes
   * (the rounded temperature included), toggles the redstone output with the calling state, and
   * keeps the chunk marked for saving as the temperature drifts.
   */
  void applyControl(float temperature, int mode, int blocked, int output, int flags,
      int capacity, int powered, int total) {
    boolean wasCalling = isCalling;
    boolean displayChanged = Math.round(temperature) != Math.round(currentTemperature)
        || mode != callingMode || blocked != blockedMode || output != outputPercent
        || flags != statusFlags || capacity != capacityPercent || powered != poweredUnits
        || total != totalUnits || !temperatureKnown;
    currentTemperature = temperature;
    temperatureKnown = true;
    callingMode = mode;
    isCalling = mode != HvacStatus.MODE_IDLE;
    blockedMode = blocked;
    outputPercent = output;
    statusFlags = flags;
    capacityPercent = capacity;
    poweredUnits = powered;
    totalUnits = total;
    if (isCalling != wasCalling && world != null) {
      world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }
    if (displayChanged) {
      markDirtySync(world, pos, true);
      lastSavedTemperature = temperature;
    } else if (Float.isNaN(lastSavedTemperature)
        || Math.abs(temperature - lastSavedTemperature) >= 0.25f) {
      markDirty();
      lastSavedTemperature = temperature;
    }
  }

  // endregion

  // region Vents

  public boolean linkVent(BlockPos ventPos, int maxDistance) {
    if (linkedVents.contains(ventPos)) {
      return false;
    }
    if (world != null) {
      TileEntity te = world.getTileEntity(ventPos);
      if (!(te instanceof TileEntityHvacVentRelay)) {
        return false;
      }
      if (pos.getDistance(ventPos.getX(), ventPos.getY(), ventPos.getZ()) > maxDistance) {
        return false;
      }
      ((TileEntityHvacVentRelay) te).setLinkedThermostat(pos, maxDistance);
    }
    linkedVents.add(ventPos.toImmutable());
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  public boolean unlinkVent(BlockPos ventPos) {
    boolean removed = linkedVents.remove(ventPos);
    if (removed && world != null && !world.isRemote) {
      if (world.isBlockLoaded(ventPos)) {
        TileEntity te = world.getTileEntity(ventPos);
        if (te instanceof TileEntityHvacVentRelay) {
          ((TileEntityHvacVentRelay) te).clearLink();
        }
      }
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedVents() {
    return linkedVents;
  }

  public int getLinkedVentCount() {
    return linkedVents.size();
  }

  /** Farthest a vent may be linked from this thermostat. */
  public abstract int getMaxVentLinkDistance();

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    targetTempLow = readInt(compound, NBT_TARGET_TEMP_LOW, LEGACY_NBT_TARGET_TEMP_LOW);
    targetTempHigh = readInt(compound, NBT_TARGET_TEMP_HIGH, LEGACY_NBT_TARGET_TEMP_HIGH);
    if (targetTempLow == 0 && targetTempHigh == 0) {
      targetTempLow = DEFAULT_LOW;
      targetTempHigh = DEFAULT_HIGH;
    }
    isCalling = readBool(compound, NBT_IS_CALLING, LEGACY_NBT_IS_CALLING);
    callingMode = readInt(compound, NBT_CALLING_MODE, LEGACY_NBT_CALLING_MODE);
    blockedMode = compound.getInteger(NBT_BLOCKED_MODE);
    outputPercent = readInt(compound, NBT_OUTPUT, LEGACY_NBT_OUTPUT);
    statusFlags = compound.getInteger(NBT_FLAGS);
    capacityPercent = compound.hasKey(NBT_CAPACITY) ? compound.getInteger(NBT_CAPACITY) : -1;
    poweredUnits = compound.getInteger(NBT_POWERED_UNITS);
    totalUnits = compound.getInteger(NBT_TOTAL_UNITS);
    trimHeat = compound.getFloat(NBT_TRIM_HEAT);
    trimCool = compound.getFloat(NBT_TRIM_COOL);
    if (compound.hasKey(NBT_CURRENT_TEMP)) {
      currentTemperature = compound.getFloat(NBT_CURRENT_TEMP);
      temperatureKnown = true;
    } else if (compound.hasKey(LEGACY_NBT_CURRENT_TEMP)) {
      currentTemperature = compound.getFloat(LEGACY_NBT_CURRENT_TEMP);
      temperatureKnown = true;
    }
    readPosList(compound, NBT_LINKED_VENTS, LEGACY_NBT_LINKED_VENTS, linkedVents);

    // Strip long-form and retired keys so the next save writes only the current set.
    compound.removeTag(LEGACY_NBT_TARGET_TEMP_LOW);
    compound.removeTag(LEGACY_NBT_TARGET_TEMP_HIGH);
    compound.removeTag(LEGACY_NBT_IS_CALLING);
    compound.removeTag(LEGACY_NBT_CALLING_MODE);
    compound.removeTag(LEGACY_NBT_OUTPUT);
    compound.removeTag(LEGACY_NBT_CURRENT_TEMP);
    compound.removeTag(LEGACY_NBT_LINKED_VENTS);
    compound.removeTag(OLD_NBT_RAMP_TICKS);
    compound.removeTag(LEGACY_OLD_NBT_RAMP_TICKS);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_TARGET_TEMP_LOW, targetTempLow);
    compound.setInteger(NBT_TARGET_TEMP_HIGH, targetTempHigh);
    compound.setBoolean(NBT_IS_CALLING, isCalling);
    compound.setInteger(NBT_CALLING_MODE, callingMode);
    compound.setInteger(NBT_BLOCKED_MODE, blockedMode);
    compound.setInteger(NBT_OUTPUT, outputPercent);
    compound.setInteger(NBT_FLAGS, statusFlags);
    compound.setInteger(NBT_CAPACITY, capacityPercent);
    compound.setInteger(NBT_POWERED_UNITS, poweredUnits);
    compound.setInteger(NBT_TOTAL_UNITS, totalUnits);
    compound.setFloat(NBT_TRIM_HEAT, trimHeat);
    compound.setFloat(NBT_TRIM_COOL, trimCool);
    if (temperatureKnown || world != null) {
      compound.setFloat(NBT_CURRENT_TEMP, world != null && !world.isRemote
          ? temperatureForSave() : currentTemperature);
    }
    compound.setTag(NBT_LINKED_VENTS, writePosList(linkedVents));
    return compound;
  }

  static int readInt(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) {
      return compound.getInteger(key);
    }
    if (compound.hasKey(legacyKey)) {
      return compound.getInteger(legacyKey);
    }
    return 0;
  }

  static boolean readBool(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) {
      return compound.getBoolean(key);
    }
    return compound.hasKey(legacyKey) && compound.getBoolean(legacyKey);
  }

  static void readPosList(NBTTagCompound compound, String key, String legacyKey,
      List<BlockPos> out) {
    out.clear();
    String listKey = compound.hasKey(key) ? key : compound.hasKey(legacyKey) ? legacyKey : null;
    if (listKey == null) {
      return;
    }
    NBTTagList list = compound.getTagList(listKey, Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < list.tagCount(); i++) {
      NBTTagCompound tag = list.getCompoundTagAt(i);
      out.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
    }
  }

  static NBTTagList writePosList(List<BlockPos> positions) {
    NBTTagList list = new NBTTagList();
    for (BlockPos p : positions) {
      NBTTagCompound tag = new NBTTagCompound();
      tag.setInteger("x", p.getX());
      tag.setInteger("y", p.getY());
      tag.setInteger("z", p.getZ());
      list.appendTag(tag);
    }
    return list;
  }

  // endregion

  // region Getters/setters

  @Override
  public int getTargetTempLow() {
    return targetTempLow;
  }

  public void setTargetTempLow(int targetTempLow) {
    this.targetTempLow = targetTempLow;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public int getTargetTempHigh() {
    return targetTempHigh;
  }

  public void setTargetTempHigh(int targetTempHigh) {
    this.targetTempHigh = targetTempHigh;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public float getCurrentTemperature() {
    return currentTemperature;
  }

  @Override
  public boolean isCalling() {
    return isCalling;
  }

  @Override
  public int getCallingMode() {
    return callingMode;
  }

  /** The mode wanted but impossible for lack of equipment of that kind, else idle. */
  public int getBlockedMode() {
    return blockedMode;
  }

  /**
   * Output, percent: for a primary, how hard its equipment is running; for a zone, how much of
   * what its vents could deliver they are delivering.
   */
  public int getOutputPercent() {
    return outputPercent;
  }

  /** {@link HvacStatus} flags. */
  public int getStatusFlags() {
    return statusFlags;
  }

  /**
   * Installed capacity as a percentage of the heat the served rooms lose at the setpoint, or -1
   * when there is no load to compare with (or this is a zone). Under 100 cannot keep up.
   */
  public int getCapacityPercent() {
    return capacityPercent;
  }

  public int getPoweredUnitCount() {
    return poweredUnits;
  }

  public boolean hasSystemPower() {
    return poweredUnits > 0;
  }

  // endregion

  /**
   * The special renderer draws only on the block's face, so its own cell is enough to cull by.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(),
        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
  }

  /**
   * No baked model reads this tile entity -- only its special renderer, which reads it every
   * frame -- so a sync never needs the chunk section rebuilt.
   */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
