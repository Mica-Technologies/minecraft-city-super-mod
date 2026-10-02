package com.micatechnologies.minecraft.csm.hvac;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * One enclosed air space -- a room, a flat, a hallway, a whole open-plan floor -- and the heat it
 * holds, region by region.
 *
 * <p>This is the whole thermal model. Each region (see {@link ThermalScanner}) has a heat capacity
 * (its air plus a share of its walls' mass) and a temperature. It loses heat to what it touches in
 * proportion to the temperature difference: the outdoors through walls and openings, the ground,
 * regions of neighbouring spaces through shared walls, and -- far faster -- the neighbouring
 * regions of its own space through the open air between them. Equipment adds or removes heat.
 * Once a second:</p>
 *
 * <pre>
 *   C_r dT_r/dt = Q_r - SUM_i UA_ri (T_r - T_i)
 * </pre>
 *
 * <p>is stepped implicitly (Jacobi sweeps over the regions), which is stable for any step,
 * any room shape and any amount of equipment: nothing in here can oscillate on its own. Every
 * reader -- thermostat, HUD, computer screen -- reads a region's temperature; there is no second
 * number to drift away from it.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class ThermalSpace {

  /** How much of each envelope face's wall mass counts toward heat capacity. */
  static final float WALL_MASS_PER_FACE = 0.5f;

  /** Conductance factor, relative to a wall, toward enclosed air that belongs to no space. */
  static final float UNCONDITIONED_FACTOR = 0.5f;

  /**
   * Heat exchanged per second, per degree, through one open cell face between two regions of the
   * same space. Moving air carries heat hundreds of times faster than a wall conducts it (a light
   * draught through a square metre moves a few hundred watts per kelvin; a wall passes about one),
   * and a unit heater's fan throws its air across a room, so this is some 750 times a wall face:
   * two neighbouring regions of a room even out in seconds, a 40-block corridor heated from one
   * end is around ten degrees cooler at the far end, and a tall atrium stratifies by a few degrees
   * rather than by tens.
   */
  static final float MIX_PER_FACE = 2.0f;

  /** Extra mixing when the lower of two stacked regions is the warmer: warm air rises. */
  static final float BUOYANT_MIX_FACTOR = 3.0f;

  /** Jacobi sweeps per step. The system is strongly diagonal, so few are needed. */
  static final int SOLVER_SWEEPS = 20;

  final int id;
  final LongOpenHashSet cells;
  final Long2IntOpenHashMap regionOf;
  final int regionCount;
  final int volume;
  final int openingFaces;

  // Per region.
  final int[] regionVolume;
  final int[] regionMinY;
  final float[] capacity;
  final float[] exteriorUA;
  final float[] openingUA;
  final float[] groundUA;
  final float[] temperature;
  final float[] pendingHeat;
  final float[] lastHeat;
  final float[] sumUA;
  final float[] sumUAT;
  /** Conductance to enclosed air owned by no space, resolved with the couplings. */
  final float[] unconditionedUA;

  // Open interfaces between regions of this space.
  final int[] ifaceA;
  final int[] ifaceB;
  final float[] ifaceFaces;
  final int[] ifaceVertical;

  // Faces onto enclosed air beyond a wall (candidate couplings to other spaces).
  final long[] beyondCell;
  final int[] beyondRegion;
  final float[] beyondUA;

  // Resolved couplings to regions of other spaces: this region, other space, other region, UA.
  int[] coupleRegion = new int[0];
  ThermalSpace[] coupleSpace = new ThermalSpace[0];
  int[] coupleOtherRegion = new int[0];
  float[] coupleUA = new float[0];

  /** A cell of the space, for biome lookups. */
  final long sampleCell;

  /** The biome (outdoor) temperature at the space, refreshed periodically. */
  float outdoor;

  /** Anchors (devices, players) currently attached. The space is dropped when none remain. */
  final java.util.List<ThermalAnchor> anchors = new java.util.ArrayList<>();

  /** Set when a block changed on or next to the space; it is rescanned soon after. */
  boolean dirty;

  /** Chunks the space's cells lie in, as {@link HvacThermalWorld#chunkKey} values. */
  final long[] chunks;

  /** The 16-block cubes its cells lie in ({@link #sectionOf}). */
  final long[] sections;

  /** The 16-block cubes the cells beyond its walls lie in: where a change can alter couplings. */
  final long[] beyondSections;

  /**
   * The world's change stamp when the couplings were last resolved, or -1 before the first time;
   * see {@link HvacThermalWorld}'s coupling resolution.
   */
  long couplingStamp = -1;

  /**
   * Set for a step in which part of the space, or part of a system serving it, is not loaded.
   * A frozen space keeps its temperature exactly: a building never cools (or warms) because the
   * half of it holding its thermostat or its rooftop units unloaded before the rest did.
   */
  boolean frozen;

  /**
   * Set for a step in which every system touching the space is switched off, and nothing else
   * (another system, a space heater) is working in it: the space sleeps. It is frozen as an
   * unloaded one is, and not rescanned; a block change while it sleeps leaves it dirty, and it is
   * rescanned the step after something wakes it.
   */
  boolean idle;

  /** The last step in which a running system or unit worked in this space. */
  long activeStep = -1;

  /** World tick of the last scan. */
  long lastScanTick;

  /**
   * After a rescan that could not finish because part of the space was unloaded (the space was
   * put back as it was): the chunks it was stopped at. It is not rescanned again until one of them
   * loads or a block on or beside it changes. Null when not waiting.
   */
  long[] waitChunks;

  /** False until the temperatures have been set from saved, inherited or equilibrium data. */
  boolean temperatureKnown;

  /** Set when a new space must start at its equilibrium once couplings are resolved. */
  boolean needsEquilibrium;

  ThermalSpace(int id, ThermalScanner.Result scan, long sampleCell, long tick) {
    this.id = id;
    this.cells = scan.cells;
    this.regionOf = scan.regionOf;
    this.regionCount = scan.regionCount;
    this.volume = scan.cells.size();
    this.openingFaces = scan.openingFaces;
    this.sampleCell = sampleCell;
    this.lastScanTick = tick;

    int n = regionCount;
    regionVolume = scan.volume.toIntArray();
    regionMinY = scan.minY.toIntArray();
    exteriorUA = scan.exteriorUA.toFloatArray();
    openingUA = scan.openingUA.toFloatArray();
    groundUA = scan.groundUA.toFloatArray();
    capacity = new float[n];
    for (int r = 0; r < n; r++) {
      capacity[r] = regionVolume[r] + WALL_MASS_PER_FACE * scan.envelopeFaces.getInt(r);
    }
    temperature = new float[n];
    pendingHeat = new float[n];
    lastHeat = new float[n];
    sumUA = new float[n];
    sumUAT = new float[n];
    unconditionedUA = new float[n];

    ifaceA = scan.ifaceA.toIntArray();
    ifaceB = scan.ifaceB.toIntArray();
    ifaceVertical = scan.ifaceVertical.toIntArray();
    ifaceFaces = new float[ifaceA.length];
    for (int i = 0; i < ifaceFaces.length; i++) {
      ifaceFaces[i] = scan.ifaceFaces.getInt(i);
    }
    beyondCell = scan.beyondCell.toLongArray();
    beyondRegion = scan.beyondRegion.toIntArray();
    beyondUA = scan.beyondUA.toFloatArray();

    LongOpenHashSet chunkSet = new LongOpenHashSet();
    LongOpenHashSet sectionSet = new LongOpenHashSet();
    it.unimi.dsi.fastutil.longs.LongIterator it = cells.iterator();
    while (it.hasNext()) {
      long c = it.nextLong();
      chunkSet.add(HvacThermalWorld.chunkKey(ThermalScanner.unpackX(c) >> 4,
          ThermalScanner.unpackZ(c) >> 4));
      sectionSet.add(sectionOf(c));
    }
    chunks = chunkSet.toLongArray();
    sections = sectionSet.toLongArray();

    LongOpenHashSet beyondSet = new LongOpenHashSet();
    for (long c : beyondCell) {
      beyondSet.add(sectionOf(c));
    }
    beyondSections = beyondSet.toLongArray();
  }

  /** The 16-block cube a packed cell lies in, packed the same way. */
  static long sectionOf(long packedCell) {
    return ThermalScanner.pack(ThermalScanner.unpackX(packedCell) >> 4,
        ThermalScanner.unpackY(packedCell) >> 4, ThermalScanner.unpackZ(packedCell) >> 4);
  }

  /** Region of a cell of this space, or -1. */
  int regionOfCell(long packedCell) {
    return regionOf.getOrDefault(ThermalScanner.regionKey(ThermalScanner.unpackX(packedCell),
        ThermalScanner.unpackY(packedCell), ThermalScanner.unpackZ(packedCell)), -1);
  }

  /** The ground's temperature: halfway between the air above it and 50°F deep soil. */
  static float groundTemperature(float outdoor) {
    return (outdoor + 50.0f) * 0.5f;
  }

  /**
   * Fixes this step's linear loss terms for every region from the current temperatures of
   * everything it touches, own-space neighbours included: {@code loss_r(T) = sumUA_r * T -
   * sumUAT_r}. Done for every space before any is stepped, so controllers and the solver see one
   * consistent snapshot.
   */
  void prepareLoss() {
    float tg = groundTemperature(outdoor);
    for (int r = 0; r < regionCount; r++) {
      float outUA = exteriorUA[r] + openingUA[r] + unconditionedUA[r] * UNCONDITIONED_FACTOR;
      sumUA[r] = outUA + groundUA[r];
      sumUAT[r] = outUA * outdoor + groundUA[r] * tg;
    }
    for (int i = 0; i < coupleRegion.length; i++) {
      int r = coupleRegion[i];
      float t = coupleSpace[i].temperature[coupleOtherRegion[i]];
      sumUA[r] += coupleUA[i];
      sumUAT[r] += coupleUA[i] * t;
    }
    for (int i = 0; i < ifaceA.length; i++) {
      int a = ifaceA[i];
      int b = ifaceB[i];
      float k = mixConductance(i);
      sumUA[a] += k;
      sumUAT[a] += k * temperature[b];
      sumUA[b] += k;
      sumUAT[b] += k * temperature[a];
    }
  }

  /** Mixing conductance of an interface this step, faster when warm air is below cold. */
  private float mixConductance(int i) {
    float k = MIX_PER_FACE * ifaceFaces[i];
    int v = ifaceVertical[i];
    if (v != 0) {
      int lower = v > 0 ? ifaceA[i] : ifaceB[i];
      int upper = v > 0 ? ifaceB[i] : ifaceA[i];
      if (temperature[lower] > temperature[upper]) {
        k *= BUOYANT_MIX_FACTOR;
      }
    }
    return k;
  }

  /** Heat per second region {@code r} loses at temperature {@code t} (negative: it gains). */
  float lossAt(int r, float t) {
    return sumUA[r] * t - sumUAT[r];
  }

  /**
   * Heat per second needed to bring region {@code r} to {@code target} with a closed-loop time
   * constant of {@code tau} seconds: exactly the loss at the target (so the target, once reached,
   * is held with no error and no cycling) plus enough to close the gap.
   */
  float requiredHeat(int r, float target, float tau) {
    return lossAt(r, target) + capacity[r] * (target - temperature[r]) / tau;
  }

  /** Steps every region {@code dt} seconds with the heat delivered this step. */
  void integrate(float dt) {
    if (frozen) {
      java.util.Arrays.fill(pendingHeat, 0f);
      java.util.Arrays.fill(lastHeat, 0f);
      return;
    }
    int n = regionCount;
    float[] old = temperature.clone();
    // External-only terms (everything but own-space mixing) per region.
    float[] extUA = new float[n];
    float[] extUAT = new float[n];
    float tg = groundTemperature(outdoor);
    for (int r = 0; r < n; r++) {
      float outUA = exteriorUA[r] + openingUA[r] + unconditionedUA[r] * UNCONDITIONED_FACTOR;
      extUA[r] = outUA + groundUA[r];
      extUAT[r] = outUA * outdoor + groundUA[r] * tg;
    }
    for (int i = 0; i < coupleRegion.length; i++) {
      int r = coupleRegion[i];
      extUA[r] += coupleUA[i];
      extUAT[r] += coupleUA[i] * coupleSpace[i].temperature[coupleOtherRegion[i]];
    }
    float[] mix = new float[ifaceA.length];
    for (int i = 0; i < mix.length; i++) {
      mix[i] = mixConductance(i);
    }
    // Implicit: C (T' - T)/dt = Q + extUAT - extUA T' - SUM mix (T' - Tn').
    float[] diag = new float[n];
    for (int r = 0; r < n; r++) {
      diag[r] = capacity[r] / dt + extUA[r];
    }
    for (int i = 0; i < mix.length; i++) {
      diag[ifaceA[i]] += mix[i];
      diag[ifaceB[i]] += mix[i];
    }
    float[] rhs = new float[n];
    for (int r = 0; r < n; r++) {
      rhs[r] = capacity[r] / dt * old[r] + pendingHeat[r] + extUAT[r];
    }
    if (mix.length == 0) {
      for (int r = 0; r < n; r++) {
        temperature[r] = rhs[r] / diag[r];
      }
    } else {
      float[] acc = new float[n];
      for (int sweep = 0; sweep < SOLVER_SWEEPS; sweep++) {
        java.util.Arrays.fill(acc, 0f);
        for (int i = 0; i < mix.length; i++) {
          acc[ifaceA[i]] += mix[i] * temperature[ifaceB[i]];
          acc[ifaceB[i]] += mix[i] * temperature[ifaceA[i]];
        }
        for (int r = 0; r < n; r++) {
          temperature[r] = (rhs[r] + acc[r]) / diag[r];
        }
      }
    }
    for (int r = 0; r < n; r++) {
      lastHeat[r] = pendingHeat[r];
      pendingHeat[r] = 0.0f;
    }
  }

  /** Sets every region to the temperature it would settle at with no equipment running. */
  void settleToEquilibrium() {
    prepareLoss();
    // Iterate the steady state: each region at the weighted mean of what it touches.
    for (int iter = 0; iter < 50; iter++) {
      prepareLoss();
      for (int r = 0; r < regionCount; r++) {
        temperature[r] = sumUA[r] > 0 ? sumUAT[r] / sumUA[r] : outdoor;
      }
    }
  }

  /** Volume-weighted mean temperature of the whole space. */
  float meanTemperature() {
    double sum = 0;
    for (int r = 0; r < regionCount; r++) {
      sum += (double) temperature[r] * regionVolume[r];
    }
    return volume > 0 ? (float) (sum / volume) : outdoor;
  }

  /** Temperature at a cell of this space (its region's), or the mean if the cell is not ours. */
  float temperatureAt(long packedCell) {
    int r = regionOfCell(packedCell);
    return r >= 0 ? temperature[r] : meanTemperature();
  }

  /** Total conductance of the whole space to everything outside it, per degree. */
  float envelopeUA() {
    float ua = 0;
    for (int r = 0; r < regionCount; r++) {
      ua += exteriorUA[r] + openingUA[r] + groundUA[r]
          + unconditionedUA[r] * UNCONDITIONED_FACTOR;
    }
    for (float c : coupleUA) {
      ua += c;
    }
    return ua;
  }

  /**
   * Heat per second the whole space loses to everything outside it when held at {@code t}:
   * the load a system has to meet. Mixing inside the space is not a loss and is left out.
   */
  float envelopeLossAt(float t) {
    float tg = groundTemperature(outdoor);
    float loss = 0;
    for (int r = 0; r < regionCount; r++) {
      float outUA = exteriorUA[r] + openingUA[r] + unconditionedUA[r] * UNCONDITIONED_FACTOR;
      loss += outUA * (t - outdoor) + groundUA[r] * (t - tg);
    }
    for (int i = 0; i < coupleRegion.length; i++) {
      loss += coupleUA[i] * (t - coupleSpace[i].temperature[coupleOtherRegion[i]]);
    }
    return loss;
  }

  /** Total heat capacity. */
  float totalCapacity() {
    float c = 0;
    for (float v : capacity) {
      c += v;
    }
    return c;
  }
}
