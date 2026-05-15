package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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
  private static final String NBT_BLOCKED_MODE = "bM";
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
   * Restart hysteresis in °F. Heating stops AT the low setpoint (no overshoot), and
   * won't restart until the temperature drops more than this many degrees below the
   * setpoint. Cooling mirrors: stops AT the high setpoint, restarts when temperature
   * climbs more than this many degrees above. Putting the hysteresis on the restart
   * side — rather than as an overshoot deadband at the stop side — matches real
   * residential HVAC and avoids the previous bug where the proportional-control term
   * throttled output to near zero before the temperature could ever push past the
   * overshoot threshold, leaving the system stuck calling indefinitely at the setpoint.
   *
   * <p>1.5°F is on the tight end of typical real-world residential thermostats (1–3°F).
   * Tight enough that the user's commanded range still feels accurate, wide enough that
   * residual vent decay (~80s thermal coast after shutoff) doesn't immediately retrigger.</p>
   */
  private static final float CYCLE_HYSTERESIS = 1.5f;

  /** Calling mode: 0 = idle, 1 = heating, 2 = cooling. */
  private static final int MODE_IDLE = 0;
  private static final int MODE_HEATING = 1;
  private static final int MODE_COOLING = 2;

  /**
   * Temperature error (in °F) at which the proportional-control term saturates at full
   * output. The P-term targets the active setpoint directly (low for heating, high for
   * cooling) and ramps vent output down linearly as the room approaches it, clamped below
   * by {@link #P_TERM_MIN_OUTPUT} so the system can always finish closing the last degree
   * or two against ambient heat loss. The calling state exits the moment the setpoint is
   * reached, at which point output value is irrelevant. With this set to 5°F and a 0.4
   * floor:
   * <ul>
   *   <li>5°F+ off setpoint → full vent output (no throttling)</li>
   *   <li>2.5°F off → 50% output</li>
   *   <li>1°F off → 40% output (floor)</li>
   *   <li>at setpoint → calling state exits this tick</li>
   * </ul>
   * Previously this was 10°F, paired with an above-setpoint overshoot deadband. The
   * combination throttled output to ~20% by the time the room hit the user's setpoint,
   * never reached the overshoot threshold needed to stop calling, and the system stalled
   * indefinitely. Tightening the saturation alone (5°F, no floor) flipped the failure
   * mode the other way: the smoothed reading asymptotically approached the setpoint from
   * below — output dropped to near zero faster than the room could close the last degree,
   * and the system stalled 1–2°F short. The 5°F range plus the 0.4 floor keeps the
   * approach gentle while guaranteeing enough output near the setpoint to actually reach
   * it. See {@link #CYCLE_HYSTERESIS} for the restart side.
   */
  private static final float P_TERM_FULL_RANGE = 5.0f;

  /**
   * Minimum proportional-term output applied while a calling state is active. Prevents
   * the steady-state-error stall described in {@link #P_TERM_FULL_RANGE}: with no floor,
   * output decays to zero as the smoothed reading nears setpoint, room temperature
   * asymptotes 1–2°F short, and the smoothed value never crosses the exit threshold.
   * 0.4 (= 40% of base) is empirically enough to overcome ambient heat loss in
   * typical-size rooms against a 15–25°F gradient to outdoor temperature while still
   * leaving meaningful proportional behavior in the upper range.
   */
  private static final float P_TERM_MIN_OUTPUT = 0.4f;

  private int targetTempLow = 65;
  private int targetTempHigh = 80;
  private float currentTemperature = 72.0F;
  private boolean isCalling = false;
  /** Tracks whether we're heating or cooling for deadband logic. Transient. */
  private int callingMode = MODE_IDLE;

  /**
   * Records the mode the thermostat would have entered if the appropriate equipment was
   * linked. {@code MODE_HEATING} means "we're below setpoint but no heater is linked";
   * {@code MODE_COOLING} means "above setpoint but no cooler"; {@code MODE_IDLE} means
   * "no problem, equipment matches need". Used purely for GUI feedback so the user knows
   * why the thermostat is not actively conditioning the room. Transient — re-derived
   * every tick from current state.
   */
  private transient int blockedMode = MODE_IDLE;
  /** Whether the thermal smoothing has been initialized with a real reading. */
  private boolean temperatureInitialized = false;

  /**
   * Set after readNBT and cleared on the first tick. While set, the smoothing snaps to
   * the raw reading even if it differs wildly from the saved value — this handles the
   * "thermostat moved between biomes / extreme reload" case. After the first tick we never
   * snap again, so transient HVAC overshoot can't bypass thermal smoothing and oscillate.
   */
  private transient boolean firstTickAfterLoad = false;

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

    // Read the instantaneous ambient temperature for this thermostat. The thermostat
    // sits flush against the wall it's mounted on, so a single-point read at `pos`
    // accumulates wall-attenuation against every HVAC source — collapsing the per-direction
    // cap and freezing the reading well below what the room actually sits at. Sampling
    // a few blocks forward through the FACING direction lets the device pick up the room's
    // real temperature; see HvacTemperatureManager#getAmbientTemperatureAt for the full
    // rationale.
    EnumFacing facing = getRoomFacing();
    float rawTemp = HvacTemperatureManager.getAmbientTemperatureAt(world, pos, facing);
    float previousTemp = currentTemperature;

    // Thermal smoothing: blend toward raw temperature gradually to simulate thermal mass.
    // This prevents oscillation where the thermostat's own heater/cooler instantly changes
    // the reading, causing rapid on/off cycling.
    //
    // Snap-to-raw is restricted to the very first tick after world load (or first init).
    // A previous version snapped any time |current - raw| exceeded 60°F as a "moved to a
    // new biome" guard, but that path also fires whenever HVAC overshoot makes raw
    // temperature huge — bypassing hysteresis and causing the thermostat to oscillate
    // between heating and cooling modes once per tick.
    if (!temperatureInitialized || firstTickAfterLoad) {
      currentTemperature = rawTemp;
      temperatureInitialized = true;
      firstTickAfterLoad = false;
    } else {
      currentTemperature += (rawTemp - currentTemperature) * THERMAL_BLEND_FACTOR;
    }

    boolean wasPreviouslyCalling = isCalling;
    int previousBlockedMode = blockedMode;

    // Determine which equipment is actually linked. A heater-only system must never enter
    // cooling mode (and vice versa) — doing so flips vent contributions negative and creates
    // a runaway oscillation where the thermostat can't settle, since the very vents that
    // overheated the space then start "cooling" it without any actual coolers present.
    boolean hasHeater = false;
    boolean hasCooler = false;
    for (BlockPos unitPos : linkedUnits) {
      if (!world.isBlockLoaded(unitPos)) continue;
      TileEntity unitTe = world.getTileEntity(unitPos);
      if (unitTe instanceof TileEntityHvacCooler) {
        hasCooler = true;
      } else if (unitTe instanceof TileEntityHvacHeater) {
        hasHeater = true;
      }
      if (hasHeater && hasCooler) break;
    }

    // Reset blockedMode each tick — it's re-derived below if applicable.
    blockedMode = MODE_IDLE;

    // Hysteresis on the restart side, not the stop side: heating exits the moment the
    // temperature reaches the low setpoint, cooling the moment it reaches the high
    // setpoint. The CYCLE_HYSTERESIS gap is applied to the IDLE→calling transition
    // below, so the system has to drift past the setpoint by that margin before it
    // kicks back on. See CYCLE_HYSTERESIS Javadoc for why this beats the old
    // overshoot-deadband approach.
    switch (callingMode) {
      case MODE_HEATING:
        // Keep heating until we reach the low setpoint, OR drop heating immediately if
        // the linked heater was removed (avoids stuck-calling state).
        if (!hasHeater || currentTemperature >= targetTempLow) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      case MODE_COOLING:
        if (!hasCooler || currentTemperature <= targetTempHigh) {
          callingMode = MODE_IDLE;
          isCalling = false;
        }
        break;
      default:
        // Idle → calling: require the temperature to be more than CYCLE_HYSTERESIS past
        // the setpoint before re-engaging. Combined with stop-at-setpoint, this gives
        // each heating or cooling pass a real off-cycle instead of nibbling at the
        // setpoint forever. Heater/cooler-only-system guard: a heater-only system at
        // 100°F sits idle; it does NOT switch into cooling mode and start blowing
        // "negative" air.
        if (currentTemperature < targetTempLow - CYCLE_HYSTERESIS) {
          if (hasHeater) {
            callingMode = MODE_HEATING;
            isCalling = true;
          } else {
            // Want to heat but no heater available — flag for GUI feedback.
            blockedMode = MODE_HEATING;
            isCalling = false;
          }
        } else if (currentTemperature > targetTempHigh + CYCLE_HYSTERESIS) {
          if (hasCooler) {
            callingMode = MODE_COOLING;
            isCalling = true;
          } else {
            blockedMode = MODE_COOLING;
            isCalling = false;
          }
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

    if (isCalling != wasPreviouslyCalling) {
      world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }

    // Resolve all linked TEs once per tick, then pass to consumers.
    // Previously each method re-resolved the same positions — e.g. linkedUnits was iterated
    // in updateLinkedUnits, getBaseVentContribution, AND getMaxVentLinkDistance every tick.
    ResolvedLinks resolved = resolveAllLinks();

    // Track ramp-up: accumulate ticks while calling, decay while idle.
    // Decay at 1:1 rate preserves ramp progress across short thermostat cycles
    // so the system can gradually build to extended ramp levels over many cycles.
    if (isSystemCalling(resolved.zones)) {
      accumulatedRampTicks += getTickRate();
    } else if (accumulatedRampTicks > 0) {
      accumulatedRampTicks = Math.max(0L, accumulatedRampTicks - getTickRate());
    }

    // Update efficiency and sync to client
    int newEfficiency = Math.round(getSystemRampFactor() * 100);
    boolean efficiencyChanged = newEfficiency != cachedEfficiencyPercent;
    cachedEfficiencyPercent = newEfficiency;

    updateLinkedUnits(resolved);
    updateLinkedVents(resolved);

    // Sync to client when calling/efficiency changes OR temperature shifts by >= 1°F
    // OR blockedMode flipped (so the GUI can switch between "Comfortable" and the
    // "Need heater" / "Need cooler" warning).
    boolean tempChanged = Math.abs(currentTemperature - previousTemp) >= 0.5f;
    boolean blockedModeChanged = blockedMode != previousBlockedMode;
    if (isCalling != wasPreviouslyCalling || efficiencyChanged || tempChanged
        || blockedModeChanged) {
      markDirtySync(world, pos, true);
    }
  }

  /**
   * Returns the direction "into the room" from this thermostat, derived from its block's
   * {@code FACING} property. The thermostat model sits at the back of its block (+Z in the
   * unrotated state), so the FACING direction points into the room — exactly what
   * {@link HvacTemperatureManager#getAmbientTemperatureAt} wants for its forward-sample
   * walk. Returns {@code null} if the block has been replaced out from under the TE, in
   * which case the manager falls back to sampling only this exact position.
   */
  private EnumFacing getRoomFacing() {
    if (world == null) return null;
    IBlockState state = world.getBlockState(pos);
    if (state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) {
      return state.getValue(AbstractBlockRotatableNSEWUD.FACING);
    }
    return null;
  }

  // endregion

  // region Per-tick TE resolution

  private static class ResolvedLinks {
    final List<TileEntityHvacHeater> units;
    final List<TileEntityHvacZoneThermostat> zones;

    ResolvedLinks(List<TileEntityHvacHeater> units, List<TileEntityHvacZoneThermostat> zones) {
      this.units = units;
      this.zones = zones;
    }
  }

  private ResolvedLinks resolveAllLinks() {
    List<TileEntityHvacHeater> units = new ArrayList<>();
    Iterator<BlockPos> unitIt = linkedUnits.iterator();
    while (unitIt.hasNext()) {
      BlockPos unitPos = unitIt.next();
      if (!world.isBlockLoaded(unitPos)) continue;
      TileEntity te = world.getTileEntity(unitPos);
      if (te instanceof TileEntityHvacHeater) {
        units.add((TileEntityHvacHeater) te);
      } else {
        unitIt.remove();
      }
    }

    List<TileEntityHvacZoneThermostat> zones = new ArrayList<>();
    Iterator<BlockPos> zoneIt = linkedZones.iterator();
    while (zoneIt.hasNext()) {
      BlockPos zonePos = zoneIt.next();
      if (!world.isBlockLoaded(zonePos)) continue;
      TileEntity te = world.getTileEntity(zonePos);
      if (te instanceof TileEntityHvacZoneThermostat) {
        zones.add((TileEntityHvacZoneThermostat) te);
      } else {
        zoneIt.remove();
      }
    }

    return new ResolvedLinks(units, zones);
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

  private boolean isSystemCalling(List<TileEntityHvacZoneThermostat> resolvedZones) {
    if (isCalling) return true;
    for (TileEntityHvacZoneThermostat zone : resolvedZones) {
      if (zone.isCalling()) return true;
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

  private void updateLinkedUnits(ResolvedLinks resolved) {
    boolean needsHeating = callingMode == MODE_HEATING;
    boolean needsCooling = callingMode == MODE_COOLING;

    for (TileEntityHvacZoneThermostat zone : resolved.zones) {
      if (!pos.equals(zone.getLinkedPrimaryPos())) {
        zone.setLinkedPrimaryPos(pos);
      }
    }

    for (TileEntityHvacHeater unit : resolved.units) {
      unit.setLinkedToThermostat(true);
      boolean shouldCall;
      if (unit instanceof TileEntityHvacCooler) {
        shouldCall = needsCooling;
      } else {
        shouldCall = needsHeating;
      }
      unit.setThermostatCalling(shouldCall);
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

  private int getMaxVentLinkDistance(List<TileEntityHvacHeater> resolvedUnits) {
    int max = 30;
    for (TileEntityHvacHeater unit : resolvedUnits) {
      if (unit instanceof IHvacUnit) {
        max = Math.max(max, ((IHvacUnit) unit).getMaxVentLinkDistance());
      }
    }
    return max;
  }

  private void updateLinkedVents(ResolvedLinks resolved) {
    float rampFactor = getSystemRampFactor();
    float baseContribution = getBaseVentContribution(resolved.units);
    // Proportional-control term: scales output by the temperature error so the system
    // throttles down as it approaches setpoint instead of pumping at full power until
    // the deadband fires (which previously caused 25-30°F overshoots).
    float pTerm = computeProportionalTerm();
    float contribution = baseContribution * rampFactor * pTerm;

    if (callingMode == MODE_COOLING) {
      contribution = -Math.abs(contribution);
    } else if (callingMode == MODE_HEATING) {
      contribution = Math.abs(contribution);
    } else {
      contribution = 0.0f;
    }

    int maxVentDist = getMaxVentLinkDistance(resolved.units);

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
        if (!pos.equals(vent.getLinkedThermostatPos())) {
          vent.setLinkedThermostat(pos, maxVentDist);
        }
        validVents.add(vent);
      } else {
        it.remove();
      }
    }

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
   * Returns the proportional output factor (0..1) for the current heating/cooling mode.
   * Saturates at 1.0 when the thermostat reading is more than {@link #P_TERM_FULL_RANGE}
   * degrees from the deadband edge, ramps linearly down to 0 at setpoint. Returns 0 when
   * idle (no output called for).
   *
   * <p>This is the regulator's core: it lets a system at extreme error (-30°F outside,
   * 70°F target) pump at full power, while a system close to setpoint backs off gracefully
   * and avoids the 25-30°F overshoot that the old "full output until deadband fires"
   * approach produced.</p>
   */
  private float computeProportionalTerm() {
    float error;
    if (callingMode == MODE_HEATING) {
      error = targetTempLow - currentTemperature;
    } else if (callingMode == MODE_COOLING) {
      error = currentTemperature - targetTempHigh;
    } else {
      return 0.0f;
    }
    float pTerm = error / P_TERM_FULL_RANGE;
    if (pTerm < P_TERM_MIN_OUTPUT) return P_TERM_MIN_OUTPUT;
    if (pTerm > 1.0f) return 1.0f;
    return pTerm;
  }

  private float getBaseVentContribution(List<TileEntityHvacHeater> resolvedUnits) {
    float best = 0.0f;
    for (TileEntityHvacHeater unit : resolvedUnits) {
      if (unit instanceof IHvacUnit) {
        float abs = Math.abs(((IHvacUnit) unit).getVentRelayContribution());
        if (abs > best) {
          best = abs;
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
    this.blockedMode = compound.hasKey(NBT_BLOCKED_MODE) ? compound.getInteger(NBT_BLOCKED_MODE) : MODE_IDLE;
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
    // blends from this value rather than snapping to the first raw reading. But also set
    // firstTickAfterLoad so the very first tick can snap if the saved value is wildly
    // out of sync with reality (block was moved to a new biome between sessions).
    if (hasCurrentTemp) {
      this.temperatureInitialized = true;
      this.firstTickAfterLoad = true;
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
    compound.setInteger(NBT_BLOCKED_MODE, blockedMode);
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
   * Returns the mode the thermostat would have entered if the right equipment was linked
   * but couldn't (no heater while cold, no cooler while warm). Returns {@code MODE_IDLE}
   * when there's no blockage to report. Used by the GUI to show a "Need heater" /
   * "Need cooler" warning instead of "Comfortable".
   */
  public int getBlockedMode() { return blockedMode; }

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
