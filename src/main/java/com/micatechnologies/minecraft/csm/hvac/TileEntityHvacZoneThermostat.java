package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * Zone thermostat for the HVAC system. A zone thermostat does not own any heater/cooler units
 * directly; instead it links to a primary {@link TileEntityHvacThermostat} and manages a
 * separate set of vent relays. It reads the temperature at its own position, determines
 * whether it needs heating or cooling, and pushes vent contributions to its own vents using
 * the primary thermostat's units and ramp state.
 *
 * <p>This allows a single set of heaters/coolers (controlled by the primary thermostat) to
 * serve multiple rooms, each with independent setpoints and vent networks.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacZoneThermostat extends AbstractTickableTileEntity
    implements IHvacThermostatDisplay {

  private static final String NBT_TARGET_TEMP_LOW = "targetTempLow";
  private static final String NBT_TARGET_TEMP_HIGH = "targetTempHigh";
  private static final String NBT_IS_CALLING = "isCalling";
  private static final String NBT_LINKED_VENTS = "linkedVents";
  private static final String NBT_LINKED_PRIMARY = "linkedPrimary";
  private static final String NBT_HAS_PRIMARY = "hasPrimary";

  /**
   * Blending factor per tick for thermal smoothing. Matches the primary thermostat.
   */
  private static final float THERMAL_BLEND_FACTOR = 0.06f;

  /**
   * Hysteresis deadband in degrees F. Matches the primary thermostat.
   */
  private static final float DEADBAND = 2.0f;

  /** Bonus temperature per additional vent beyond the first. */
  private static final float VENT_DENSITY_BONUS_PER_VENT = 2.0f;

  /** Maximum total vent density bonus in degrees F. */
  private static final float VENT_DENSITY_BONUS_CAP = 8.0f;

  /** Calling mode constants. */
  private static final int MODE_IDLE = 0;
  private static final int MODE_HEATING = 1;
  private static final int MODE_COOLING = 2;

  private int targetTempLow = 65;
  private int targetTempHigh = 80;
  private float currentTemperature = 72.0f;
  private boolean isCalling = false;
  private int callingMode = MODE_IDLE;
  private boolean temperatureInitialized = false;

  /** Positions of linked vent relays. Persisted in NBT. */
  private final List<BlockPos> linkedVents = new ArrayList<>();

  /** Position of the linked primary thermostat, or null if not linked. */
  private BlockPos linkedPrimaryPos = null;

  /**
   * Accumulated ramp ticks. Increases while calling, decreases while idle.
   * Persisted in NBT so the ramp survives chunk unload/reload.
   */
  private long accumulatedRampTicks = 0L;

  /** Cached efficiency percent for client sync. */
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

    // Self-heal: if we think we have a primary but the primary no longer lists us
    // (e.g. primary was replaced), clear our stale back-link so the display is accurate.
    if (linkedPrimaryPos != null && world.isBlockLoaded(linkedPrimaryPos)) {
      TileEntityHvacThermostat primary = getPrimaryThermostat();
      if (primary != null && !primary.getLinkedZones().contains(pos)) {
        linkedPrimaryPos = null;
        markDirtySync(world, pos, true);
      }
    }

    // Read the instantaneous temperature at this zone's position
    float rawTemp = HvacTemperatureManager.getRawTemperatureAt(world, pos);
    float previousTemp = currentTemperature;

    // Thermal smoothing — snap immediately when the saved temp is wildly off from reality
    // (e.g. moved to a different biome) to avoid a misleadingly stale display.
    if (!temperatureInitialized || Math.abs(currentTemperature - rawTemp) > 60.0f) {
      currentTemperature = rawTemp;
      temperatureInitialized = true;
    } else {
      currentTemperature += (rawTemp - currentTemperature) * THERMAL_BLEND_FACTOR;
    }

    boolean wasPreviouslyCalling = isCalling;

    // Hysteresis deadband logic (same as primary thermostat)
    switch (callingMode) {
      case MODE_HEATING:
        if (currentTemperature >= targetTempLow + DEADBAND) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      case MODE_COOLING:
        if (currentTemperature <= targetTempHigh - DEADBAND) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      default:
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

    // Debug: log zone thermostat state after mode evaluation
    if (HvacTemperatureManager.isDebugLogging()) {
      String modeStr = callingMode == MODE_HEATING ? "HEATING"
          : callingMode == MODE_COOLING ? "COOLING" : "IDLE";
      org.apache.logging.log4j.LogManager.getLogger("CSM-HVAC").info(
          String.format("[HVAC-ZONE-TICK] pos=%s raw=%.1f smoothed=%.1f mode=%s calling=%s low=%d high=%d ramp=%d%%",
              pos, rawTemp, currentTemperature, modeStr, isCalling,
              targetTempLow, targetTempHigh, Math.round(getSystemRampFactor() * 100)));
    }

    // Track ramp-up: accumulate ticks while calling, decay while idle
    if (isCalling) {
      accumulatedRampTicks += getTickRate();
    } else if (accumulatedRampTicks > 0) {
      accumulatedRampTicks = Math.max(0L, accumulatedRampTicks - getTickRate());
    }

    if (isCalling != wasPreviouslyCalling) {
      world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }

    // Update efficiency from zone's own ramp
    int newEfficiency = Math.round(getSystemRampFactor() * 100);
    boolean efficiencyChanged = newEfficiency != cachedEfficiencyPercent;
    cachedEfficiencyPercent = newEfficiency;

    // Update linked vents
    updateLinkedVents();

    // Sync to client when state changes
    boolean tempChanged = Math.abs(currentTemperature - previousTemp) >= 0.5f;
    if (isCalling != wasPreviouslyCalling || efficiencyChanged || tempChanged) {
      markDirtySync(world, pos, true);
    }
  }

  // endregion

  // region Primary Thermostat Delegation

  /**
   * Returns the primary thermostat this zone is linked to, or null if not linked or unloaded.
   */
  private TileEntityHvacThermostat getPrimaryThermostat() {
    if (linkedPrimaryPos == null || world == null || !world.isBlockLoaded(linkedPrimaryPos)) {
      return null;
    }
    TileEntity te = world.getTileEntity(linkedPrimaryPos);
    if (te instanceof TileEntityHvacThermostat) {
      return (TileEntityHvacThermostat) te;
    }
    return null;
  }

  /** Ramp constants — match primary thermostat. */
  private static final long PHASE1_RAMP_TICKS = 6000L;
  private static final long PHASE2_RAMP_TICKS = 12000L;
  private static final float RAMP_MIN_FACTOR = 0.2f;
  private static final float MAX_EXTENDED_RAMP = 1.6f;

  /**
   * Returns the zone's own ramp factor. Phase 1 (0–5 min): 0.2→1.0 sqrt curve.
   * Phase 2 (5–15 min): 1.0→1.6 linear. Persisted via accumulatedRampTicks.
   */
  public float getSystemRampFactor() {
    if (accumulatedRampTicks <= 0L) {
      return 0.0f;
    }
    if (accumulatedRampTicks <= PHASE1_RAMP_TICKS) {
      float progress = (float) accumulatedRampTicks / (float) PHASE1_RAMP_TICKS;
      float curved = (float) Math.sqrt(progress);
      return RAMP_MIN_FACTOR + (1.0f - RAMP_MIN_FACTOR) * curved;
    }
    long phase2Elapsed = accumulatedRampTicks - PHASE1_RAMP_TICKS;
    float phase2Progress = Math.min(1.0f, (float) phase2Elapsed / (float) PHASE2_RAMP_TICKS);
    return 1.0f + (MAX_EXTENDED_RAMP - 1.0f) * phase2Progress;
  }

  /**
   * Returns the system efficiency percentage for GUI display.
   */
  public int getSystemEfficiencyPercent() {
    return cachedEfficiencyPercent;
  }

  /**
   * Returns true if at least one unit on the primary thermostat has power.
   */
  public boolean hasSystemPower() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    return primary != null && primary.hasSystemPower();
  }

  /**
   * Returns the number of powered units on the primary thermostat.
   */
  public int getPoweredUnitCount() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    return primary != null ? primary.getPoweredUnitCount() : 0;
  }

  /**
   * Returns the total linked unit count from the primary thermostat.
   */
  public int getLinkedUnitCount() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    return primary != null ? primary.getLinkedUnitCount() : 0;
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
   * Returns the maximum vent link distance, delegating to the primary thermostat.
   */
  public int getMaxVentLinkDistance() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    return primary != null ? primary.getMaxVentLinkDistance() : 30;
  }

  /**
   * Returns the base vent contribution from the primary thermostat's strongest linked unit.
   */
  private float getBaseVentContribution() {
    TileEntityHvacThermostat primary = getPrimaryThermostat();
    if (primary == null || world == null) {
      return 0.0f;
    }
    float best = 0.0f;
    for (BlockPos unitPos : primary.getLinkedUnits()) {
      if (world.isBlockLoaded(unitPos)) {
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

  /**
   * Pushes contribution values to all linked vents.
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

  // endregion

  // region Linked Primary Management

  public BlockPos getLinkedPrimaryPos() {
    return linkedPrimaryPos;
  }

  public void setLinkedPrimaryPos(BlockPos primaryPos) {
    this.linkedPrimaryPos = primaryPos != null ? primaryPos.toImmutable() : null;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  public boolean hasLinkedPrimary() {
    return linkedPrimaryPos != null;
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    this.targetTempLow = compound.getInteger(NBT_TARGET_TEMP_LOW);
    this.targetTempHigh = compound.getInteger(NBT_TARGET_TEMP_HIGH);
    this.isCalling = compound.getBoolean(NBT_IS_CALLING);
    this.callingMode = compound.getInteger("callingMode");
    this.cachedEfficiencyPercent = compound.getInteger("efficiency");
    this.accumulatedRampTicks = compound.getLong("rampTicks");
    this.currentTemperature = compound.getFloat("currentTemp");
    if (compound.hasKey("currentTemp")) {
      this.temperatureInitialized = true;
    }
    if (targetTempLow == 0 && targetTempHigh == 0) {
      targetTempLow = 65;
      targetTempHigh = 80;
    }

    // Read linked primary
    if (compound.getBoolean(NBT_HAS_PRIMARY)) {
      NBTTagCompound primaryTag = compound.getCompoundTag(NBT_LINKED_PRIMARY);
      linkedPrimaryPos = new BlockPos(
          primaryTag.getInteger("x"),
          primaryTag.getInteger("y"),
          primaryTag.getInteger("z"));
    } else {
      linkedPrimaryPos = null;
    }

    // Read linked vents
    linkedVents.clear();
    if (compound.hasKey(NBT_LINKED_VENTS)) {
      NBTTagList list = compound.getTagList(NBT_LINKED_VENTS, Constants.NBT.TAG_COMPOUND);
      for (int i = 0; i < list.tagCount(); i++) {
        NBTTagCompound tag = list.getCompoundTagAt(i);
        linkedVents.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"),
            tag.getInteger("z")));
      }
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_TARGET_TEMP_LOW, targetTempLow);
    compound.setInteger(NBT_TARGET_TEMP_HIGH, targetTempHigh);
    compound.setBoolean(NBT_IS_CALLING, isCalling);
    compound.setInteger("callingMode", callingMode);
    compound.setInteger("efficiency", cachedEfficiencyPercent);
    compound.setLong("rampTicks", accumulatedRampTicks);
    compound.setFloat("currentTemp", currentTemperature);

    // Write linked primary
    if (linkedPrimaryPos != null) {
      compound.setBoolean(NBT_HAS_PRIMARY, true);
      NBTTagCompound primaryTag = new NBTTagCompound();
      primaryTag.setInteger("x", linkedPrimaryPos.getX());
      primaryTag.setInteger("y", linkedPrimaryPos.getY());
      primaryTag.setInteger("z", linkedPrimaryPos.getZ());
      compound.setTag(NBT_LINKED_PRIMARY, primaryTag);
    } else {
      compound.setBoolean(NBT_HAS_PRIMARY, false);
    }

    // Write linked vents
    NBTTagList ventList = new NBTTagList();
    for (BlockPos ventPos : linkedVents) {
      NBTTagCompound tag = new NBTTagCompound();
      tag.setInteger("x", ventPos.getX());
      tag.setInteger("y", ventPos.getY());
      tag.setInteger("z", ventPos.getZ());
      ventList.appendTag(tag);
    }
    compound.setTag(NBT_LINKED_VENTS, ventList);
    return compound;
  }

  // endregion

  // region Getters/Setters

  @Override
  public float getCurrentTemperature() { return currentTemperature; }

  @Override
  public int getTargetTempLow() { return targetTempLow; }

  public void setTargetTempLow(int targetTempLow) {
    this.targetTempLow = targetTempLow;
    if (world != null && !world.isRemote) { markDirtySync(world, pos, true); }
  }

  @Override
  public int getTargetTempHigh() { return targetTempHigh; }

  public void setTargetTempHigh(int targetTempHigh) {
    this.targetTempHigh = targetTempHigh;
    if (world != null && !world.isRemote) { markDirtySync(world, pos, true); }
  }

  @Override
  public boolean isCalling() { return isCalling; }

  public int getCallingMode() { return callingMode; }

  // endregion
}
