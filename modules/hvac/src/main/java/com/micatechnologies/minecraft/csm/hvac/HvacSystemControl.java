package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.CsmEnvironment;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The controllers: once a second, each primary thermostat's system decides whether it is heating
 * or cooling, how much heat each room it serves needs, and hands that heat out through its vents;
 * unlinked heaters and coolers run their own built-in thermostat.
 *
 * <p><b>Why model-based control.</b> The simulation knows exactly how fast every region is losing
 * heat, so a region's need is computed rather than guessed: the heat that holds it at the target
 * ({@link ThermalSpace#lossAt}) plus enough to close the remaining gap over {@link #TAU} seconds.
 * At the target the request equals the loss, so the room is held there with no error and nothing
 * to cycle; away from it the request closes the gap smoothly. Because the request is computed from
 * the room's own capacity and loss, the loop behaves the same in a closet and in a warehouse, with
 * one heater or thirty-two rooftop units -- where the old fixed gains made a small room ring.</p>
 *
 * <p><b>What is regulated.</b> Each region a zone's vents blow into is held at that zone's
 * setpoint -- the low setpoint plus a degree when heating, the high minus a degree when cooling --
 * so a zone serving five offices from a hallway thermostat still keeps all five offices right.
 * Where the zone's vents share a room with its thermostat, a slow trim raises (or lowers) the vent
 * regions' target until the thermostat itself reads the setpoint, so a thermostat low on a wall
 * under ceiling vents is satisfied too, not just the air up at the vents.</p>
 *
 * <p><b>One mode at a time.</b> A system with both heaters and coolers heats or cools, not both.
 * It changes over only after a minute with no demand for its current mode, or when the other mode
 * has out-demanded it for five minutes, so zones wanting opposite things cannot make it flap.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
final class HvacSystemControl {

  /** Closed-loop time constant, seconds: how quickly a request closes a temperature gap. */
  static final float TAU = 90.0f;

  /** Heat per second one vent can deliver. */
  static final float VENT_CAPACITY = 250.0f;

  /** Trim integration rate: degrees of trim per second per degree of thermostat error. */
  static final float TRIM_RATE = 0.01f;

  /** Largest trim, degrees. */
  static final float TRIM_MAX = 20.0f;

  /**
   * Trim only moves while every region the zone blows into (in the thermostat's room) is within
   * this many degrees of the target it is being given. Until then the room is still warming up or
   * cooling down, the thermostat's error is not the difference between it and the vents, and
   * integrating it would wind the trim up and overshoot.
   */
  static final float TRIM_SETTLED = 1.5f;

  /**
   * Share of a vent's air that stays in the vent's own region; the rest is thrown down the
   * column of air below it, as a ceiling diffuser's jet reaches the floor.
   */
  static final float THROW_OWN_SHARE = 0.4f;

  /** How many regions below a vent its throw reaches (four blocks each). */
  static final int THROW_REGIONS = 3;

  /** Where an unlinked heater holds its room. */
  static final float STANDALONE_HEAT_TARGET = 70.0f;

  /** Where an unlinked cooler holds its room. */
  static final float STANDALONE_COOL_TARGET = 74.0f;

  /** Demand below this (heat per second) is no demand. */
  private static final float DEMAND_EPS = 1.0f;

  /** A system reverses mode only after this long with nothing to do in its current one. */
  private static final long CHANGEOVER_TICKS = 1200L;

  /** ...or after the other mode has out-demanded the current one for this long. */
  private static final long FORCE_CHANGEOVER_TICKS = 6000L;

  private HvacSystemControl() {
  }

  /** A region a system delivers into. */
  private static final class Served {
    final ThermalSpace space;
    final int region;
    /** Governing zone (index into the system's zone list). */
    int zone;
    int vents;
    float heatCapHere;
    float coolCapHere;
    float needHeat;
    float needCool;
    float request;
    float targetHeat;
    float targetCool;
    /** Regions the delivery goes to (the vent's own first) and each one's share. */
    int[] group;
    float[] share;

    Served(ThermalSpace space, int region, int zone) {
      this.space = space;
      this.region = region;
      this.zone = zone;
      this.group = new int[]{region};
      this.share = new float[]{1.0f};
    }

    /** Heat per second the group needs to reach {@code target}, less what it already has. */
    float need(float target) {
      float q = 0;
      for (int r : group) {
        q += space.requiredHeat(r, target, TAU) - space.pendingHeat[r];
      }
      return q;
    }

    void deliver(float q) {
      for (int i = 0; i < group.length; i++) {
        space.pendingHeat[group[i]] += q * share[i];
      }
    }
  }

  /**
   * The regions below a vent that its air is thrown into: down its column through the space, one
   * region per four blocks, stopping at the floor (or anything else not part of the space).
   */
  private static void setThrow(Served e, BlockPos vent) {
    ThermalSpace s = e.space;
    int[] found = new int[THROW_REGIONS];
    int n = 0;
    for (int y = vent.getY() - 1; y >= vent.getY() - THROW_REGIONS * ThermalScanner.REGION_Y
        && n < THROW_REGIONS; y--) {
      long cell = ThermalScanner.pack(vent.getX(), y, vent.getZ());
      if (!s.cells.contains(cell)) {
        break;
      }
      int r = s.regionOfCell(cell);
      boolean seen = r == e.region;
      for (int i = 0; i < n; i++) {
        seen |= found[i] == r;
      }
      if (!seen && r >= 0) {
        found[n++] = r;
      }
    }
    if (n == 0) {
      return;
    }
    e.group = new int[n + 1];
    e.share = new float[n + 1];
    e.group[0] = e.region;
    e.share[0] = THROW_OWN_SHARE;
    for (int i = 0; i < n; i++) {
      e.group[i + 1] = found[i];
      e.share[i + 1] = (1.0f - THROW_OWN_SHARE) / n;
    }
  }

  /** One thermostat of a system: the primary (index 0) or one of its zones. */
  private static final class Zone {
    final TileEntityHvacThermostatBase tstat;
    final ThermalAnchor anchor;
    int vents;
    boolean servesOwnSpace;
    boolean wantsHeat;
    boolean wantsCool;
    float delivered;
    float maxDelivery;

    Zone(TileEntityHvacThermostatBase tstat, ThermalAnchor anchor) {
      this.tstat = tstat;
      this.anchor = anchor;
    }

    ThermalSpace space() {
      return anchor != null ? anchor.space : null;
    }
  }

  static void run(HvacThermalWorld w) {
    World world = w.world;
    List<ThermalAnchor> list = new ArrayList<>(w.anchors());
    for (ThermalAnchor a : list) {
      if (a.kind == ThermalAnchor.THERMOSTAT) {
        TileEntity te = world.getTileEntity(a.pos);
        if (te instanceof TileEntityHvacThermostat) {
          runSystem(w, (TileEntityHvacThermostat) te, a);
        }
      }
    }
    for (ThermalAnchor a : list) {
      if (a.kind == ThermalAnchor.UNIT) {
        TileEntity te = world.getTileEntity(a.pos);
        if (te instanceof TileEntityHvacHeater
            && ((TileEntityHvacHeater) te).claimStep != w.stepId) {
          runStandalone((TileEntityHvacHeater) te, a);
        }
      } else if (a.kind == ThermalAnchor.ZONE) {
        TileEntity te = world.getTileEntity(a.pos);
        if (te instanceof TileEntityHvacZoneThermostat
            && ((TileEntityHvacZoneThermostat) te).lastRunStep != w.stepId) {
          // A zone no loaded primary ran this step. If its primary is only unloaded, hold its
          // rooms as they are until it loads; if it has none, it only reports.
          TileEntityHvacZoneThermostat z = (TileEntityHvacZoneThermostat) te;
          BlockPos pp = z.getLinkedPrimaryPos();
          boolean primaryUnloaded = pp != null && !world.isBlockLoaded(pp);
          if (primaryUnloaded) {
            freeze(a);
            for (BlockPos vp : z.getLinkedVents()) {
              freeze(w.anchor(vp));
            }
          }
          z.applyControl(display(w, a), HvacStatus.MODE_IDLE, HvacStatus.MODE_IDLE, 0,
              spaceFlags(a) | (primaryUnloaded ? HvacStatus.FLAG_WAITING_FOR_CHUNKS
                  : HvacStatus.FLAG_NO_PRIMARY), -1, 0, 0);
        }
      }
    }
  }

  // region Standalone units

  /** An unlinked, powered unit holds its own room like a space heater or window unit. */
  private static void runStandalone(TileEntityHvacHeater unit, ThermalAnchor a) {
    if (a.space != null && a.space.frozen) {
      return; // part of its room is unloaded: leave it exactly as it is
    }
    if (!unit.hasPower() || a.space == null || a.region < 0 || unit.isDuctedOnly()) {
      unit.applyOutput(0.0f);
      return;
    }
    ThermalSpace s = a.space;
    int r = a.region;
    float cap = unit.getHeatCapacity();
    float q;
    if (unit.isCoolingUnit()) {
      float need = s.pendingHeat[r] - s.requiredHeat(r, STANDALONE_COOL_TARGET, TAU);
      q = -clamp(need, 0, cap);
    } else {
      float need = s.requiredHeat(r, STANDALONE_HEAT_TARGET, TAU) - s.pendingHeat[r];
      q = clamp(need, 0, cap);
    }
    s.pendingHeat[r] += q;
    unit.applyOutput(Math.abs(q) / cap);
  }

  // endregion

  // region Systems

  private static void runSystem(HvacThermalWorld w, TileEntityHvacThermostat primary,
      ThermalAnchor primaryAnchor) {
    World world = w.world;
    long now = world.getTotalWorldTime();
    primary.lastRunStep = w.stepId;

    // Thermostats of the system: the primary first, then its zones.
    List<Zone> zones = new ArrayList<>();
    zones.add(new Zone(primary, primaryAnchor));
    for (Iterator<BlockPos> it = primary.getLinkedZones().iterator(); it.hasNext(); ) {
      BlockPos zp = it.next();
      if (!world.isBlockLoaded(zp)) {
        continue;
      }
      TileEntity te = world.getTileEntity(zp);
      if (!(te instanceof TileEntityHvacZoneThermostat)) {
        it.remove();
        continue;
      }
      TileEntityHvacZoneThermostat zone = (TileEntityHvacZoneThermostat) te;
      zone.lastRunStep = w.stepId;
      if (!primary.getPos().equals(zone.getLinkedPrimaryPos())) {
        zone.setLinkedPrimaryPos(primary.getPos());
      }
      zones.add(new Zone(zone, w.anchor(zp)));
    }

    // Equipment.
    List<TileEntityHvacHeater> units = new ArrayList<>();
    for (Iterator<BlockPos> it = primary.getLinkedUnits().iterator(); it.hasNext(); ) {
      BlockPos up = it.next();
      if (!world.isBlockLoaded(up)) {
        continue;
      }
      TileEntity te = world.getTileEntity(up);
      if (!(te instanceof TileEntityHvacHeater)) {
        it.remove();
        continue;
      }
      TileEntityHvacHeater unit = (TileEntityHvacHeater) te;
      unit.claimStep = w.stepId;
      units.add(unit);
    }
    // A system with any member unloaded (a zone, a unit, a vent) cannot know what it would do,
    // so it does nothing and holds every room it touches exactly as it is until it is whole.
    boolean complete = zones.size() == 1 + primary.getLinkedZones().size()
        && units.size() == primary.getLinkedUnits().size();
    for (Zone z : zones) {
      for (BlockPos vp : z.tstat.getLinkedVents()) {
        complete &= world.isBlockLoaded(vp);
      }
    }
    if (!complete) {
      for (Zone z : zones) {
        freeze(z.anchor);
        for (BlockPos vp : z.tstat.getLinkedVents()) {
          freeze(w.anchor(vp));
        }
        z.tstat.applyControl(display(w, z.anchor, z.tstat.getPos()), HvacStatus.MODE_IDLE,
            HvacStatus.MODE_IDLE, 0, spaceFlags(z.anchor) | HvacStatus.FLAG_WAITING_FOR_CHUNKS,
            -1, 0, units.size());
      }
      for (TileEntityHvacHeater u : units) {
        freeze(w.anchor(u.getPos()));
      }
      return;
    }

    boolean hasHeater = false;
    boolean hasCooler = false;
    float heatCap = 0;
    float coolCap = 0;
    int powered = 0;
    for (TileEntityHvacHeater u : units) {
      boolean p = u.hasPower();
      if (p) {
        powered++;
      }
      if (u.isCoolingUnit()) {
        hasCooler = true;
        coolCap += p ? u.getHeatCapacity() : 0;
      } else {
        hasHeater = true;
        heatCap += p ? u.getHeatCapacity() : 0;
      }
    }

    // Where the system delivers: its vents' regions, or with no vents at all, the units' own.
    boolean hasDucts = false;
    for (Zone z : zones) {
      if (!z.tstat.getLinkedVents().isEmpty()) {
        hasDucts = true;
        break;
      }
    }
    Map<Long, Served> served = new LinkedHashMap<>();
    boolean unitsUnconnected = false;
    if (hasDucts) {
      for (int zi = 0; zi < zones.size(); zi++) {
        Zone z = zones.get(zi);
        for (Iterator<BlockPos> it = z.tstat.getLinkedVents().iterator(); it.hasNext(); ) {
          BlockPos vp = it.next();
          if (!world.isBlockLoaded(vp)) {
            continue;
          }
          if (!(world.getTileEntity(vp) instanceof TileEntityHvacVentRelay)) {
            it.remove();
            continue;
          }
          ThermalAnchor va = w.anchor(vp);
          if (va == null || va.space == null || va.region < 0) {
            continue; // a vent blowing into the open, or into a space still loading
          }
          z.vents++;
          if (va.space == z.space()) {
            z.servesOwnSpace = true;
          }
          Served e = served.get(key(va.space, va.region));
          if (e == null) {
            e = new Served(va.space, va.region, zi);
            setThrow(e, vp);
            served.put(key(va.space, va.region), e);
          }
          e.vents++;
          // A region two zones blow into follows the zone whose thermostat is in that room.
          if (e.zone != zi && zones.get(e.zone).space() != e.space && z.space() == e.space) {
            e.zone = zi;
          }
        }
      }
    } else {
      for (TileEntityHvacHeater u : units) {
        ThermalAnchor ua = w.anchor(u.getPos());
        if (ua == null || ua.space == null || ua.region < 0 || u.isDuctedOnly()) {
          unitsUnconnected = true;
          continue;
        }
        if (ua.space == zones.get(0).space()) {
          zones.get(0).servesOwnSpace = true;
        }
        Served e = served.computeIfAbsent(key(ua.space, ua.region),
            k -> new Served(ua.space, ua.region, 0));
        if (u.hasPower()) {
          if (u.isCoolingUnit()) {
            e.coolCapHere += u.getHeatCapacity();
          } else {
            e.heatCapHere += u.getHeatCapacity();
          }
        }
      }
    }

    // What each region needs, both ways.
    float heatWant = 0;
    float coolWant = 0;
    for (Served e : served.values()) {
      Zone g = zones.get(e.zone);
      boolean trimmed = e.space == g.space();
      float th = heatTarget(g.tstat) + (trimmed ? g.tstat.trimHeat : 0);
      float tc = coolTarget(g.tstat) - (trimmed ? g.tstat.trimCool : 0);
      e.targetHeat = th;
      e.targetCool = tc;
      float rh = e.need(th);
      float rc = -e.need(tc);
      float limH = hasDucts ? e.vents * VENT_CAPACITY : e.heatCapHere;
      float limC = hasDucts ? e.vents * VENT_CAPACITY : e.coolCapHere;
      e.needHeat = clamp(rh, 0, limH);
      e.needCool = clamp(rc, 0, limC);
      if (rh > DEMAND_EPS) {
        g.wantsHeat = true;
      }
      if (rc > DEMAND_EPS) {
        g.wantsCool = true;
      }
      heatWant += e.needHeat;
      coolWant += e.needCool;
    }
    if (!hasHeater) {
      heatWant = 0;
    }
    if (!hasCooler) {
      coolWant = 0;
    }

    int mode = chooseMode(primary, heatWant, coolWant, now);

    // Hand the heat out.
    float cap = mode == HvacStatus.MODE_HEATING ? heatCap
        : mode == HvacStatus.MODE_COOLING ? coolCap : 0;
    float total = 0;
    for (Served e : served.values()) {
      e.request = mode == HvacStatus.MODE_HEATING ? e.needHeat
          : mode == HvacStatus.MODE_COOLING ? e.needCool : 0;
      total += e.request;
    }
    float scale = hasDucts ? (total > 0 ? Math.min(1.0f, cap / total) : 0) : 1.0f;
    boolean capacityLimited = mode != HvacStatus.MODE_IDLE && hasDucts && total > 0
        && cap < total * 0.999f;
    float sign = mode == HvacStatus.MODE_COOLING ? -1.0f : 1.0f;
    float delivered = 0;
    for (Served e : served.values()) {
      Zone g = zones.get(e.zone);
      float q = e.request * scale;
      e.deliver(sign * q);
      delivered += q;
      g.delivered += q;
      g.maxDelivery += hasDucts ? e.vents * VENT_CAPACITY
          : (mode == HvacStatus.MODE_COOLING ? e.coolCapHere : e.heatCapHere);
    }
    float fraction = cap > 0 ? Math.min(1.0f, delivered / cap) : 0;
    for (TileEntityHvacHeater u : units) {
      boolean running = mode == HvacStatus.MODE_HEATING ? !u.isCoolingUnit()
          : mode == HvacStatus.MODE_COOLING && u.isCoolingUnit();
      u.applyOutput(running && u.hasPower() ? fraction : 0.0f);
    }

    // Trim: nudge each zone's vent-region target until its own thermostat reads the setpoint,
    // but only once those regions have reached the target they are being given.
    float[] gap = new float[zones.size()];
    for (Served e : served.values()) {
      Zone g = zones.get(e.zone);
      if (e.space == g.space()) {
        float target = mode == HvacStatus.MODE_COOLING ? e.targetCool : e.targetHeat;
        gap[e.zone] = Math.max(gap[e.zone], Math.abs(e.space.temperature[e.region] - target));
      }
    }
    for (int zi = 0; zi < zones.size(); zi++) {
      Zone z = zones.get(zi);
      if (gap[zi] > TRIM_SETTLED) {
        continue;
      }
      ThermalAnchor a = z.anchor;
      if (a == null || a.space == null || !z.servesOwnSpace || capacityLimited) {
        continue;
      }
      float t = a.temperature();
      TileEntityHvacThermostatBase ts = z.tstat;
      if (mode == HvacStatus.MODE_HEATING && z.delivered > 0) {
        ts.trimHeat = clamp(ts.trimHeat + TRIM_RATE * (heatTarget(ts) - t) * HvacThermalWorld.DT,
            0, TRIM_MAX);
      } else if (mode == HvacStatus.MODE_COOLING && z.delivered > 0) {
        ts.trimCool = clamp(ts.trimCool + TRIM_RATE * (t - coolTarget(ts)) * HvacThermalWorld.DT,
            0, TRIM_MAX);
      }
    }

    // Capacity against the load of every room the system serves, at the primary's setpoints.
    int capacityPercent = -1;
    Map<ThermalSpace, Boolean> spacesServed = new IdentityHashMap<>();
    for (Served e : served.values()) {
      spacesServed.put(e.space, Boolean.TRUE);
    }
    boolean coolingSeason = mode == HvacStatus.MODE_COOLING
        || (mode == HvacStatus.MODE_IDLE && coolWant > heatWant);
    float load = 0;
    for (ThermalSpace s : spacesServed.keySet()) {
      load += coolingSeason ? -s.envelopeLossAt(coolTarget(primary))
          : s.envelopeLossAt(heatTarget(primary));
    }
    float installed = coolingSeason ? coolCap : heatCap;
    if (hasDucts) {
      int vents = 0;
      for (Served e : served.values()) {
        vents += e.vents;
      }
      installed = Math.min(installed, vents * VENT_CAPACITY);
    }
    if (load > 1.0f) {
      capacityPercent = Math.min(9999, Math.round(100.0f * installed / load));
    }

    // Report. A primary with no vents of its own speaks for the whole system.
    boolean anyWantsHeat = false;
    boolean anyWantsCool = false;
    for (Zone z : zones) {
      anyWantsHeat |= z.wantsHeat;
      anyWantsCool |= z.wantsCool;
    }
    for (int zi = 0; zi < zones.size(); zi++) {
      Zone z = zones.get(zi);
      int flags = spaceFlags(z.anchor);
      if (hasDucts && z.vents == 0 && (zi > 0 || zones.size() == 1)) {
        flags |= HvacStatus.FLAG_NO_VENTS;
      }
      if (z.vents > 0 && !z.servesOwnSpace) {
        flags |= HvacStatus.FLAG_ROOM_NOT_SERVED;
      }
      if (!hasDucts && unitsUnconnected && zi == 0) {
        flags |= HvacStatus.FLAG_UNITS_UNCONNECTED;
      }
      boolean systemView = zi == 0 && z.vents == 0 && zones.size() > 1;
      boolean wantsHeat = systemView ? anyWantsHeat : z.wantsHeat;
      boolean wantsCool = systemView ? anyWantsCool : z.wantsCool;
      int blocked = HvacStatus.MODE_IDLE;
      if (wantsHeat && !hasHeater) {
        blocked = HvacStatus.MODE_HEATING;
      } else if (wantsCool && !hasCooler) {
        blocked = HvacStatus.MODE_COOLING;
      }
      if ((wantsHeat && hasHeater && heatCap <= 0) || (wantsCool && hasCooler
          && coolCap <= 0)) {
        flags |= HvacStatus.FLAG_NO_POWER;
      }
      if ((mode == HvacStatus.MODE_HEATING && z.wantsCool && !z.wantsHeat && hasCooler)
          || (mode == HvacStatus.MODE_COOLING && z.wantsHeat && !z.wantsCool && hasHeater)) {
        flags |= HvacStatus.FLAG_WAITING;
      }
      if (capacityLimited && (z.delivered > 0 || systemView)) {
        flags |= HvacStatus.FLAG_CAPACITY_LIMITED;
      }
      int calling = z.delivered > 0.5f ? mode : HvacStatus.MODE_IDLE;
      if (zi == 0 && z.vents == 0 && zones.size() > 1) {
        // A primary with no vents of its own runs the equipment for its zones.
        calling = delivered > 0.5f ? mode : HvacStatus.MODE_IDLE;
      }
      int outputPercent;
      if (zi == 0) {
        outputPercent = Math.round(100.0f * fraction);
      } else {
        outputPercent = z.maxDelivery > 0 ? Math.round(100.0f * z.delivered / z.maxDelivery) : 0;
      }
      z.tstat.applyControl(display(w, z.anchor, z.tstat.getPos()), calling, blocked,
          outputPercent, flags, zi == 0 ? capacityPercent : -1, powered, units.size());
    }
  }

  /**
   * Picks the system's mode for this step. Staying put is always preferred; reversing needs a
   * minute of nothing to do in the current mode, or five minutes of the other mode wanting more.
   */
  private static int chooseMode(TileEntityHvacThermostat primary, float heatWant, float coolWant,
      long now) {
    boolean wantH = heatWant > DEMAND_EPS;
    boolean wantC = coolWant > DEMAND_EPS;
    int mode = primary.systemMode;
    int next = mode;
    long dwell = now - primary.systemModeSince;
    switch (mode) {
      case HvacStatus.MODE_HEATING:
        if (!wantH) {
          next = HvacStatus.MODE_IDLE;
        } else if (wantC && coolWant > heatWant && dwell >= FORCE_CHANGEOVER_TICKS) {
          next = HvacStatus.MODE_COOLING;
        }
        break;
      case HvacStatus.MODE_COOLING:
        if (!wantC) {
          next = HvacStatus.MODE_IDLE;
        } else if (wantH && heatWant > coolWant && dwell >= FORCE_CHANGEOVER_TICKS) {
          next = HvacStatus.MODE_HEATING;
        }
        break;
      default:
        boolean reverseOk = dwell >= CHANGEOVER_TICKS;
        boolean heatOk = primary.lastActiveMode != HvacStatus.MODE_COOLING || reverseOk;
        boolean coolOk = primary.lastActiveMode != HvacStatus.MODE_HEATING || reverseOk;
        if (wantH && heatOk && (!wantC || !coolOk || heatWant >= coolWant)) {
          next = HvacStatus.MODE_HEATING;
        } else if (wantC && coolOk) {
          next = HvacStatus.MODE_COOLING;
        }
        break;
    }
    if (next != mode) {
      primary.systemMode = next;
      primary.systemModeSince = now;
      if (next != HvacStatus.MODE_IDLE) {
        primary.lastActiveMode = next;
      }
    }
    return next;
  }

  // endregion

  // region Helpers

  static float heatTarget(TileEntityHvacThermostatBase t) {
    return Math.min(t.getTargetTempLow() + 1.0f,
        (t.getTargetTempLow() + t.getTargetTempHigh()) * 0.5f);
  }

  static float coolTarget(TileEntityHvacThermostatBase t) {
    return Math.max(t.getTargetTempHigh() - 1.0f,
        (t.getTargetTempLow() + t.getTargetTempHigh()) * 0.5f);
  }

  /** Holds the anchor's space as it is for this step, if it has one. */
  private static void freeze(ThermalAnchor a) {
    if (a != null && a.space != null) {
      a.space.frozen = true;
    }
  }

  /** Flags describing why a thermostat has no room reading, if it has none. */
  static int spaceFlags(ThermalAnchor a) {
    if (a == null || a.space != null) {
      return 0;
    }
    switch (a.status) {
      case TOO_LARGE:
        return HvacStatus.FLAG_TOO_LARGE;
      case UNLOADED:
        return HvacStatus.FLAG_WAITING_FOR_CHUNKS;
      default:
        return HvacStatus.FLAG_NOT_ENCLOSED;
    }
  }

  private static float display(HvacThermalWorld w, ThermalAnchor a) {
    return display(w, a, a.pos);
  }

  /**
   * What a thermostat shows: its region's temperature; while its room is still loading (or has
   * not been looked for yet), the temperature it saved; outdoors or in a space too large to be a
   * room, the biome's.
   */
  private static float display(HvacThermalWorld w, ThermalAnchor a, BlockPos pos) {
    float t = Float.NaN;
    if (a != null && (a.space != null || a.status == ThermalScanner.Status.UNLOADED
        || a.status == ThermalScanner.Status.OK)) {
      t = a.temperature();
    }
    return Float.isNaN(t) ? CsmEnvironment.getBaselineTemperatureAt(w.world, pos) : t;
  }

  private static long key(ThermalSpace s, int region) {
    return ((long) s.id << 32) | (region & 0xFFFFFFFFL);
  }

  private static float clamp(float v, float lo, float hi) {
    return v < lo ? lo : (v > hi ? hi : v);
  }

  // endregion
}
