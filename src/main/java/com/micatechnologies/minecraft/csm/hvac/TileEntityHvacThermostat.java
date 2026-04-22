package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * Central controller for the HVAC system. Manages linked heaters/coolers and vent relays,
 * handles gradual temperature ramp-up, and calculates vent contributions with density bonuses.
 *
 * <p><b>System ramp-up:</b> When the thermostat starts calling, units begin at
 * {@link #RAMP_MIN_FACTOR} (20%) effectiveness and linearly ramp to 100% over
 * {@link #RAMP_DURATION_MS} (5 minutes). This makes the system gradually bring a space to
 * the target temperature rather than snapping instantly.</p>
 *
 * <p><b>Vent density bonus:</b> Each vent linked to this thermostat contributes a base amount
 * (from the strongest linked unit's vent relay contribution). Additional vents in range add
 * {@link #VENT_DENSITY_BONUS_PER_VENT} (2°F) each, capped at {@link #VENT_DENSITY_BONUS_CAP}
 * (8°F). This rewards realistic duct layouts with multiple vents per room.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacThermostat extends AbstractTickableTileEntity
    implements IHvacThermostatDisplay {

  // Short-form NBT keys. LEGACY_* counterparts are retained only so readNBT can still
  // load worlds saved before the short-key optimization; writeNBT only emits the short form.
  private static final String NBT_TARGET_TEMP_LOW = "tLo";
  private static final String LEGACY_NBT_TARGET_TEMP_LOW = "targetTempLow";
  private static final String NBT_TARGET_TEMP_HIGH = "tHi";
  private static final String LEGACY_NBT_TARGET_TEMP_HIGH = "targetTempHigh";
  private static final String NBT_IS_CALLING = "cL";
  private static final String LEGACY_NBT_IS_CALLING = "isCalling";
  private static final String NBT_CALLING_MODE = "cM";
  private static final String LEGACY_NBT_CALLING_MODE = "callingMode";
  private static final String NBT_EFFICIENCY = "eff";
  private static final String LEGACY_NBT_EFFICIENCY = "efficiency";
  private static final String NBT_RAMP_TICKS = "rT";
  private static final String LEGACY_NBT_RAMP_TICKS = "rampTicks";
  private static final String NBT_CURRENT_TEMP = "cT";
  private static final String LEGACY_NBT_CURRENT_TEMP = "currentTemp";
  private static final String NBT_LINKED_UNITS = "lU";
  private static final String LEGACY_NBT_LINKED_UNITS = "linkedUnits";
  private static final String NBT_LINKED_VENTS = "lV";
  private static final String LEGACY_NBT_LINKED_VENTS = "linkedVents";
  private static final String NBT_LINKED_ZONES = "lZ";
  private static final String LEGACY_NBT_LINKED_ZONES = "linkedZones";

  /** Phase 1 ramp duration in game ticks (5 minutes = 6000 ticks at 20 TPS). */
  private static final long PHASE1_RAMP_TICKS = 6000L;

  /** Phase 2 (extended) ramp duration in game ticks (10 more minutes = 12000 ticks). */
  private static final long PHASE2_RAMP_TICKS = 12000L;

  /** Minimum effectiveness factor when the system first starts calling (20%). */
  private static final float RAMP_MIN_FACTOR = 0.2f;

  /** Maximum extended ramp factor at the end of phase 2. Vent contributions are multiplied
   *  by this, so 1.6 × 15°F base = 24°F max vent output after 15 minutes of operation. */
  private static final float MAX_EXTENDED_RAMP = 1.6f;

  /** Bonus temperature per additional vent beyond the first in the same area. */
  private static final float VENT_DENSITY_BONUS_PER_VENT = 2.0f;

  /** Maximum total vent density bonus in °F. */
  private static final float VENT_DENSITY_BONUS_CAP = 8.0f;

  /**
   * Blending factor per tick for thermal smoothing. Each thermostat tick (40 game ticks = 2s),
   * the displayed temperature moves this fraction of the way toward the raw calculated value.
   * At 0.06 and a 40-tick (2s) interval, it takes ~38 ticks (~76s) to cover 90% of a
   * temperature change — realistic room thermal mass without feeling unresponsive.
   */
  private static final float THERMAL_BLEND_FACTOR = 0.06f;

  /**
   * Hysteresis deadband in °F. Once the system starts heating, it won't stop until the
   * temperature rises DEADBAND degrees above targetLow. Once cooling, it won't stop until
   * the temperature drops DEADBAND degrees below targetHigh. Prevents rapid cycling near
   * the setpoint boundaries.
   */
  private static final float DEADBAND = 2.0f;

  /** Calling mode: 0 = idle, 1 = heating, 2 = cooling. */
  private static final int MODE_IDLE = 0;
  private static final int MODE_HEATING = 1;
  private static final int MODE_COOLING = 2;

  private int targetTempLow = 65;
  private int targetTempHigh = 80;
  private float currentTemperature = 72.0F;
  private boolean isCalling = false;
  /** Tracks whether we're heating or cooling for deadband logic. Transient. */
  private int callingMode = MODE_IDLE;
  /** Whether the thermal smoothing has been initialized with a real reading. */
  private boolean temperatureInitialized = false;

  /** Positions of linked heaters/coolers. Persisted in NBT. */
  private final List<BlockPos> linkedUnits = new ArrayList<>();

  /** Positions of linked vent relays. Persisted in NBT. */
  private final List<BlockPos> linkedVents = new ArrayList<>();

  /** Positions of linked zone thermostats. Persisted in NBT. */
  private final List<BlockPos> linkedZones = new ArrayList<>();

  /**
   * Accumulated ramp ticks. Increases by {@link #getTickRate()} each tick while the system
   * is calling, decreases at the same rate when idle. Persisted in NBT so the ramp survives
   * chunk unload/reload. The ramp factor is derived from this value.
   */
  private long accumulatedRampTicks = 0L;

  /** Cached efficiency percent for client sync. Updated server-side, read by GUI. */
  private int cachedEfficiencyPercent = 0;

  // region AbstractTickableTileEntity

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return 40;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }

    // Read the instantaneous temperature at this position (includes active HVAC effects)
    float rawTemp = HvacTemperatureManager.getRawTemperatureAt(world, pos);
    float previousTemp = currentTemperature;

    // Thermal smoothing: blend toward raw temperature gradually to simulate thermal mass.
    // This prevents oscillation where the thermostat's own heater/cooler instantly changes
    // the reading, causing rapid on/off cycling.
    // Exception: if the saved temperature differs from reality by more than 60°F (e.g. the
    // thermostat was moved to a very different biome, or was saved in a warmer world), snap
    // immediately so the display is not misleadingly stale.
    if (!temperatureInitialized || Math.abs(currentTemperature - rawTemp) > 60.0f) {
      currentTemperature = rawTemp;
      temperatureInitialized = true;
    } else {
      currentTemperature += (rawTemp - currentTemperature) * THERMAL_BLEND_FACTOR;
    }

    boolean wasPreviouslyCalling = isCalling;

    // Hysteresis deadband: once heating/cooling starts, don't stop until the temperature
    // overshoots the setpoint by DEADBAND degrees. This creates natural HVAC cycles.
    switch (callingMode) {
      case MODE_HEATING:
        // Keep heating until we reach targetLow + deadband
        if (currentTemperature >= targetTempLow + DEADBAND) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      case MODE_COOLING:
        // Keep cooling until we reach targetHigh - deadband
        if (currentTemperature <= targetTempHigh - DEADBAND) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      default:
        // Idle — start calling if outside the setpoint range
        if (currentTemperature < targetTempLow) {
          callingMode = MODE_HEATING;
          isCalling = true;
        } else if (currentTemperature > targetTempHigh) {
          callingMode = MODE_COOLING;
          isCalling = true;
        } else {
          isCalling = false;
        }
        break;
    }

    // Debug: log thermostat state after mode evaluation
    if (HvacTemperatureManager.isDebugLogging()) {
      String modeStr = callingMode == MODE_HEATING ? "HEATING"
          : callingMode == MODE_COOLING ? "COOLING" : "IDLE";
      org.apache.logging.log4j.LogManager.getLogger("CSM-HVAC").info(
          String.format("[HVAC-TSTAT-TICK] pos=%s raw=%.1f smoothed=%.1f mode=%s calling=%s low=%d high=%d ramp=%d%%",
              pos, rawTemp, currentTemperature, modeStr, isCalling,
              targetTempLow, targetTempHigh, Math.round(getSystemRampFactor() * 100)));
    }

    // Track ramp-up: accumulate ticks while calling, decay while idle.
    // Decay at 1:1 rate preserves ramp progress across short thermostat cycles
    // so the system can gradually build to extended ramp levels over many cycles.
    if (isSystemCalling()) {
      accumulatedRampTicks += getTickRate();
    } else if (accumulatedRampTicks > 0) {
      accumulatedRampTicks = Math.max(0L, accumulatedRampTicks - getTickRate());
    }

    if (isCalling != wasPreviouslyCalling) {
      world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }

    // Update efficiency and sync to client
    int newEfficiency = Math.round(getSystemRampFactor() * 100);
    boolean efficiencyChanged = newEfficiency != cachedEfficiencyPercent;
    cachedEfficiencyPercent = newEfficiency;

    // Always update linked units and vents (handles ramp-up changes)
    updateLinkedUnits();
    updateLinkedVents();

    // Sync to client when calling/efficiency changes OR temperature shifts by >= 1°F
    boolean tempChanged = Math.abs(currentTemperature - previousTemp) >= 0.5f;
    if (isCalling != wasPreviouslyCalling || efficiencyChanged || tempChanged) {
      markDirtySync(world, pos, true);
    }
  }

  // endregion

  // region System Ramp-Up

  /**
   * Returns true if ANY part of the system is calling — the primary thermostat itself
   * OR any linked zone thermostat. Used for ramp factor and unit activation.
   */
  public boolean isSystemCalling() {
    if (isCalling) {
      return true;
    }
    for (BlockPos zonePos : linkedZones) {
      if (world != null && world.isBlockLoaded(zonePos)) {
        TileEntity te = world.getTileEntity(zonePos);
        if (te instanceof TileEntityHvacZoneThermostat
            && ((TileEntityHvacZoneThermostat) te).isCalling()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the current system effectiveness factor, in two phases:
   * <ul>
   *   <li><b>Phase 1</b> (0–5 min): 0.2 → 1.0 using a sqrt curve. The system ramps quickly
   *   in the early phase then tapers — like a real HVAC system that moves a room most of the
   *   way to temperature quickly, then fine-tunes.</li>
   *   <li><b>Phase 2</b> (5–15 min): 1.0 → {@link #MAX_EXTENDED_RAMP} linear. Extended
   *   operation pushes vent contributions beyond their base value, allowing the system to
   *   reach higher/lower temperatures over time.</li>
   * </ul>
   * <p>Persisted via {@link #accumulatedRampTicks} so it survives chunk unload/reload.</p>
   */
  public float getSystemRampFactor() {
    if (accumulatedRampTicks <= 0L) {
      return 0.0f;
    }

    // Phase 1: sqrt curve from RAMP_MIN_FACTOR to 1.0
    if (accumulatedRampTicks <= PHASE1_RAMP_TICKS) {
      float progress = (float) accumulatedRampTicks / (float) PHASE1_RAMP_TICKS;
      float curved = (float) Math.sqrt(progress);
      return RAMP_MIN_FACTOR + (1.0f - RAMP_MIN_FACTOR) * curved;
    }

    // Phase 2: linear from 1.0 to MAX_EXTENDED_RAMP
    long phase2Elapsed = accumulatedRampTicks - PHASE1_RAMP_TICKS;
    float phase2Progress = Math.min(1.0f, (float) phase2Elapsed / (float) PHASE2_RAMP_TICKS);
    return 1.0f + (MAX_EXTENDED_RAMP - 1.0f) * phase2Progress;
  }

  /**
   * Returns the system efficiency as a percentage (0-100) for GUI display.
   * Uses the cached value that is synced from server to client via NBT.
   */
  public int getSystemEfficiencyPercent() {
    return cachedEfficiencyPercent;
  }

  // endregion

  // region Linked Unit Management

  public boolean linkUnit(BlockPos unitPos) {
    if (linkedUnits.contains(unitPos)) {
      return false;
    }
    if (world != null) {
      TileEntity te = world.getTileEntity(unitPos);
      if (!(te instanceof TileEntityHvacHeater)) {
        return false;
      }
    }
    linkedUnits.add(unitPos.toImmutable());
    if (world != null && !world.isRemote) {
      updateSingleUnit(unitPos, isCalling);
      markDirtySync(world, pos, true);
    }
    return true;
  }

  public boolean unlinkUnit(BlockPos unitPos) {
    boolean removed = linkedUnits.remove(unitPos);
    if (removed && world != null && !world.isRemote) {
      updateSingleUnit(unitPos, false);
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedUnits() {
    return linkedUnits;
  }

  public int getLinkedUnitCount() {
    return linkedUnits.size();
  }

  private void updateLinkedUnits() {
    // Determine which direct units (heaters/coolers) should run based on the PRIMARY
    // thermostat's own calling mode only. Zone demand is NOT aggregated here — zones
    // handle heating/cooling through their own vent contributions independently. If zone
    // demand activated the cooler while the primary is heating (or vice versa), the heater
    // and cooler would cancel each other out at nearby positions, producing zero net offset.
    boolean needsHeating = callingMode == MODE_HEATING;
    boolean needsCooling = callingMode == MODE_COOLING;

    // Validate zone links (remove broken ones) but don't aggregate their demand for units
    Iterator<BlockPos> zoneIt = linkedZones.iterator();
    while (zoneIt.hasNext()) {
      BlockPos zonePos = zoneIt.next();
      if (!world.isBlockLoaded(zonePos)) {
        continue;
      }
      TileEntity zoneTe = world.getTileEntity(zonePos);
      if (!(zoneTe instanceof TileEntityHvacZoneThermostat)) {
        zoneIt.remove();
      } else {
        // Repair back-link: if the zone was replaced and the new TE doesn't know its
        // primary (or points to a different one), re-establish the link silently.
        TileEntityHvacZoneThermostat zone = (TileEntityHvacZoneThermostat) zoneTe;
        if (!pos.equals(zone.getLinkedPrimaryPos())) {
          zone.setLinkedPrimaryPos(pos);
        }
      }
    }

    Iterator<BlockPos> it = linkedUnits.iterator();
    while (it.hasNext()) {
      BlockPos unitPos = it.next();
      if (!world.isBlockLoaded(unitPos)) {
        continue;
      }
      TileEntity te = world.getTileEntity(unitPos);
      if (te instanceof TileEntityHvacHeater) {
        TileEntityHvacHeater unit = (TileEntityHvacHeater) te;
        unit.setLinkedToThermostat(true);
        // Only call heaters when heating is needed, coolers when cooling is needed
        boolean shouldCall;
        if (unit instanceof TileEntityHvacCooler) {
          shouldCall = needsCooling;
        } else {
          shouldCall = needsHeating;
        }
        unit.setThermostatCalling(shouldCall);
      } else {
        it.remove();
      }
    }
  }

  private void updateSingleUnit(BlockPos unitPos, boolean calling) {
    if (world.isBlockLoaded(unitPos)) {
      TileEntity te = world.getTileEntity(unitPos);
      if (te instanceof TileEntityHvacHeater) {
        TileEntityHvacHeater unit = (TileEntityHvacHeater) te;
        unit.setLinkedToThermostat(calling);
        unit.setThermostatCalling(calling);
      }
    }
  }

  // endregion

  // region Linked Vent Management

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

  /**
   * Returns the maximum vent link distance based on the strongest linked unit.
   * Standard units: 30 blocks. RTU units: 100 blocks.
   */
  public int getMaxVentLinkDistance() {
    int max = 30;
    for (BlockPos unitPos : linkedUnits) {
      if (world != null && world.isBlockLoaded(unitPos)) {
        TileEntity te = world.getTileEntity(unitPos);
        if (te instanceof IHvacUnit) {
          max = Math.max(max, ((IHvacUnit) te).getMaxVentLinkDistance());
        }
      }
    }
    return max;
  }

  /**
   * Pushes contribution values to all linked vents. The contribution accounts for:
   * - The strongest linked unit's vent relay base contribution
   * - The system ramp-up factor
   * - The sign (heating vs cooling) based on what the thermostat is calling for
   */
  private void updateLinkedVents() {
    float rampFactor = getSystemRampFactor();
    float baseContribution = getBaseVentContribution();
    float contribution = baseContribution * rampFactor;

    // Determine sign from callingMode (not temperature comparison, which breaks
    // during the deadband zone where temp has crossed the threshold but we're still calling)
    if (callingMode == MODE_COOLING) {
      contribution = -Math.abs(contribution);
    } else if (callingMode == MODE_HEATING) {
      contribution = Math.abs(contribution);
    } else {
      contribution = 0.0f;
    }

    // Single pass: validate links, count vents, and collect references
    List<TileEntityHvacVentRelay> validVents = new ArrayList<>();
    Iterator<BlockPos> it = linkedVents.iterator();
    while (it.hasNext()) {
      BlockPos ventPos = it.next();
      if (!world.isBlockLoaded(ventPos)) {
        continue;
      }
      TileEntity te = world.getTileEntity(ventPos);
      if (te instanceof TileEntityHvacVentRelay) {
        TileEntityHvacVentRelay vent = (TileEntityHvacVentRelay) te;
        // Repair back-link if the vent was replaced and no longer knows its thermostat.
        if (!pos.equals(vent.getLinkedThermostatPos())) {
          vent.setLinkedThermostat(pos, getMaxVentLinkDistance());
        }
        validVents.add(vent);
      } else {
        it.remove();
      }
    }

    // Calculate vent density bonus and push to all vents
    float densityBonus = 0.0f;
    if (validVents.size() > 1 && contribution != 0.0f) {
      densityBonus = Math.min((validVents.size() - 1) * VENT_DENSITY_BONUS_PER_VENT,
          VENT_DENSITY_BONUS_CAP);
      if (contribution < 0) {
        densityBonus = -densityBonus;
      }
    }

    float finalContribution = contribution + densityBonus;
    for (TileEntityHvacVentRelay vent : validVents) {
      vent.setContribution(finalContribution);
    }
  }

  /**
   * Returns the base vent relay contribution from the strongest linked unit.
   */
  private float getBaseVentContribution() {
    float best = 0.0f;
    for (BlockPos unitPos : linkedUnits) {
      if (world != null && world.isBlockLoaded(unitPos)) {
        TileEntity te = world.getTileEntity(unitPos);
        if (te instanceof IHvacUnit) {
          float abs = Math.abs(((IHvacUnit) te).getVentRelayContribution());
          if (abs > best) {
            best = abs;
          }
        }
      }
    }
    return best;
  }

  // endregion

  // region Linked Zone Management

  public boolean linkZone(BlockPos zonePos) {
    if (linkedZones.contains(zonePos)) {
      return false;
    }
    if (world != null) {
      TileEntity te = world.getTileEntity(zonePos);
      if (!(te instanceof TileEntityHvacZoneThermostat)) {
        return false;
      }
    }
    linkedZones.add(zonePos.toImmutable());
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  public boolean unlinkZone(BlockPos zonePos) {
    boolean removed = linkedZones.remove(zonePos);
    if (removed && world != null && !world.isRemote) {
      if (world.isBlockLoaded(zonePos)) {
        TileEntity te = world.getTileEntity(zonePos);
        if (te instanceof TileEntityHvacZoneThermostat) {
          ((TileEntityHvacZoneThermostat) te).setLinkedPrimaryPos(null);
        }
      }
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedZones() {
    return linkedZones;
  }

  public int getLinkedZoneCount() {
    return linkedZones.size();
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    this.targetTempLow = readInt(compound, NBT_TARGET_TEMP_LOW, LEGACY_NBT_TARGET_TEMP_LOW);
    this.targetTempHigh = readInt(compound, NBT_TARGET_TEMP_HIGH, LEGACY_NBT_TARGET_TEMP_HIGH);
    this.isCalling = readBool(compound, NBT_IS_CALLING, LEGACY_NBT_IS_CALLING);
    this.callingMode = readInt(compound, NBT_CALLING_MODE, LEGACY_NBT_CALLING_MODE);
    this.cachedEfficiencyPercent = readInt(compound, NBT_EFFICIENCY, LEGACY_NBT_EFFICIENCY);
    this.accumulatedRampTicks = readLong(compound, NBT_RAMP_TICKS, LEGACY_NBT_RAMP_TICKS);
    boolean hasCurrentTemp = compound.hasKey(NBT_CURRENT_TEMP)
        || compound.hasKey(LEGACY_NBT_CURRENT_TEMP);
    if (compound.hasKey(NBT_CURRENT_TEMP)) {
      this.currentTemperature = compound.getFloat(NBT_CURRENT_TEMP);
    } else if (compound.hasKey(LEGACY_NBT_CURRENT_TEMP)) {
      this.currentTemperature = compound.getFloat(LEGACY_NBT_CURRENT_TEMP);
    }
    // If we loaded a saved temperature, mark as initialized so thermal smoothing
    // blends from this value rather than snapping to the first raw reading
    if (hasCurrentTemp) {
      this.temperatureInitialized = true;
    }
    if (targetTempLow == 0 && targetTempHigh == 0) {
      targetTempLow = 65;
      targetTempHigh = 80;
    }
    readPosList(compound, NBT_LINKED_UNITS, LEGACY_NBT_LINKED_UNITS, linkedUnits);
    readPosList(compound, NBT_LINKED_VENTS, LEGACY_NBT_LINKED_VENTS, linkedVents);
    readPosList(compound, NBT_LINKED_ZONES, LEGACY_NBT_LINKED_ZONES, linkedZones);

    // Strip legacy long-form keys so the next save produces only short-form output
    compound.removeTag(LEGACY_NBT_TARGET_TEMP_LOW);
    compound.removeTag(LEGACY_NBT_TARGET_TEMP_HIGH);
    compound.removeTag(LEGACY_NBT_IS_CALLING);
    compound.removeTag(LEGACY_NBT_CALLING_MODE);
    compound.removeTag(LEGACY_NBT_EFFICIENCY);
    compound.removeTag(LEGACY_NBT_RAMP_TICKS);
    compound.removeTag(LEGACY_NBT_CURRENT_TEMP);
    compound.removeTag(LEGACY_NBT_LINKED_UNITS);
    compound.removeTag(LEGACY_NBT_LINKED_VENTS);
    compound.removeTag(LEGACY_NBT_LINKED_ZONES);
  }

  private static int readInt(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) return compound.getInteger(key);
    if (compound.hasKey(legacyKey)) return compound.getInteger(legacyKey);
    return 0;
  }

  private static boolean readBool(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) return compound.getBoolean(key);
    return compound.hasKey(legacyKey) && compound.getBoolean(legacyKey);
  }

  private static long readLong(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) return compound.getLong(key);
    if (compound.hasKey(legacyKey)) return compound.getLong(legacyKey);
    return 0L;
  }

  private static void readPosList(NBTTagCompound compound, String key, String legacyKey,
      List<BlockPos> out) {
    out.clear();
    String listKey = null;
    if (compound.hasKey(key)) {
      listKey = key;
    } else if (compound.hasKey(legacyKey)) {
      listKey = legacyKey;
    }
    if (listKey == null) return;
    NBTTagList list = compound.getTagList(listKey, Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < list.tagCount(); i++) {
      NBTTagCompound tag = list.getCompoundTagAt(i);
      out.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_TARGET_TEMP_LOW, targetTempLow);
    compound.setInteger(NBT_TARGET_TEMP_HIGH, targetTempHigh);
    compound.setBoolean(NBT_IS_CALLING, isCalling);
    compound.setInteger(NBT_CALLING_MODE, callingMode);
    compound.setInteger(NBT_EFFICIENCY, cachedEfficiencyPercent);
    compound.setLong(NBT_RAMP_TICKS, accumulatedRampTicks);
    compound.setFloat(NBT_CURRENT_TEMP, currentTemperature);
    compound.setTag(NBT_LINKED_UNITS, writePosList(linkedUnits));
    compound.setTag(NBT_LINKED_VENTS, writePosList(linkedVents));
    compound.setTag(NBT_LINKED_ZONES, writePosList(linkedZones));
    return compound;
  }

  private static NBTTagList writePosList(List<BlockPos> positions) {
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

  // region Getters/Setters

  public int getTargetTempLow() { return targetTempLow; }

  public void setTargetTempLow(int targetTempLow) {
    this.targetTempLow = targetTempLow;
    if (world != null && !world.isRemote) { markDirtySync(world, pos, true); }
  }

  public int getTargetTempHigh() { return targetTempHigh; }

  public void setTargetTempHigh(int targetTempHigh) {
    this.targetTempHigh = targetTempHigh;
    if (world != null && !world.isRemote) { markDirtySync(world, pos, true); }
  }

  public float getCurrentTemperature() { return currentTemperature; }

  public boolean isCalling() { return isCalling; }

  public int getCallingMode() { return callingMode; }

  /**
   * Returns true if at least one linked unit has power (redstone or FE). The thermostat
   * considers itself "powered" if any connected unit can actually run.
   */
  public boolean hasSystemPower() {
    for (BlockPos unitPos : linkedUnits) {
      if (world != null && world.isBlockLoaded(unitPos)) {
        TileEntity te = world.getTileEntity(unitPos);
        if (te instanceof TileEntityHvacHeater && ((TileEntityHvacHeater) te).hasPower()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the number of linked units that currently have power.
   */
  public int getPoweredUnitCount() {
    int count = 0;
    for (BlockPos unitPos : linkedUnits) {
      if (world != null && world.isBlockLoaded(unitPos)) {
        TileEntity te = world.getTileEntity(unitPos);
        if (te instanceof TileEntityHvacHeater && ((TileEntityHvacHeater) te).hasPower()) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Returns a render bounding box covering just the thermostat block itself. The TESR
   * draws the LCD panel on the block's face and does not project beyond the cell, so a
   * single-block AABB is enough for vanilla frustum culling to skip offscreen thermostats.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX(), pos.getY(), pos.getZ(),
        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
  }

  // endregion
}
