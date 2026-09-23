package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The scanner and the physics on hand-built grids: rooms are found and measured correctly, and a
 * room under model-based control settles at its target without overshoot or oscillation whatever
 * its size, the equipment's size or the climate. The controller law here is the one
 * {@link HvacSystemControl} applies to each served region.
 */
class ThermalModelTest {

  /** A world of stone walls on flat ground (y < 0 is earth), open sky above everything else. */
  static final class Grid implements ThermalCellSource {
    final Set<Long> walls = new HashSet<>();
    final Set<Long> glass = new HashSet<>();

    void wall(int x, int y, int z) {
      walls.add(ThermalScanner.pack(x, y, z));
    }

    void clear(int x, int y, int z) {
      walls.remove(ThermalScanner.pack(x, y, z));
    }

    /** A closed shell whose inside is x0+1..x1-1 etc. */
    void box(int x0, int y0, int z0, int x1, int y1, int z1) {
      for (int x = x0; x <= x1; x++) {
        for (int y = y0; y <= y1; y++) {
          for (int z = z0; z <= z1; z++) {
            if (x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1) {
              wall(x, y, z);
            }
          }
        }
      }
    }

    @Override
    public byte classify(int x, int y, int z) {
      if (y < 0) {
        return WALL;
      }
      if (walls.contains(ThermalScanner.pack(x, y, z))) {
        return WALL;
      }
      for (int yy = y + 1; yy < 64; yy++) {
        if (walls.contains(ThermalScanner.pack(x, yy, z))) {
          return AIR;
        }
      }
      return SKY;
    }

    @Override
    public float wallFactor(int x, int y, int z) {
      return glass.contains(ThermalScanner.pack(x, y, z)) ? 2.0f : 1.0f;
    }
  }

  private static ThermalSpace space(Grid g, int x, int y, int z, float outdoor) {
    ThermalScanner.Result r = ThermalScanner.scan(g, x, y, z);
    assertEquals(ThermalScanner.Status.OK, r.status);
    ThermalSpace s = new ThermalSpace(1, r, ThermalScanner.pack(x, y, z), 0);
    s.outdoor = outdoor;
    java.util.Arrays.fill(s.temperature, outdoor);
    return s;
  }

  // region Scanner

  @Test
  void sealedRoomIsMeasured() {
    Grid g = new Grid();
    g.box(0, 0, 0, 11, 5, 11); // inside 10 x 4 x 10
    ThermalScanner.Result r = ThermalScanner.scan(g, 5, 2, 5);
    assertEquals(ThermalScanner.Status.OK, r.status);
    assertEquals(400, r.cells.size());
    assertEquals(0, r.openingFaces);
    int volume = 0;
    float ext = 0;
    float ground = 0;
    for (int i = 0; i < r.regionCount; i++) {
      volume += r.volume.getInt(i);
      ext += r.exteriorUA.getFloat(i);
      ground += r.groundUA.getFloat(i);
    }
    assertEquals(400, volume);
    assertTrue(ext > 0, "walls and roof lose heat outdoors");
    assertTrue(ground > 0, "the floor on the earth loses heat to the ground");
    assertEquals(1, r.regionCount, "an ordinary room is one well-mixed region");
  }

  @Test
  void aBigHallIsSplitIntoRegionsWithoutSlivers() {
    Grid g = new Grid();
    g.box(0, 0, 0, 41, 7, 31); // 40 x 6 x 30, off the region grid on purpose
    ThermalScanner.Result r = ThermalScanner.scan(g, 5, 2, 5);
    assertEquals(ThermalScanner.Status.OK, r.status);
    assertTrue(r.regionCount > 4, "a hall has regions: " + r.regionCount);
    assertTrue(r.ifaceA.size() > 0, "its regions share open faces");
    for (int i = 0; i < r.regionCount; i++) {
      assertTrue(r.volume.getInt(i) >= ThermalScanner.MIN_REGION_CELLS,
          "region " + i + " is a sliver of " + r.volume.getInt(i));
    }
  }

  @Test
  void doorwayIsAnOpeningNotALeak() {
    Grid g = new Grid();
    g.box(0, 0, 0, 11, 5, 11);
    g.clear(0, 1, 5);
    g.clear(0, 2, 5); // a two-high doorway to the outside
    ThermalScanner.Result r = ThermalScanner.scan(g, 5, 2, 5);
    assertEquals(ThermalScanner.Status.OK, r.status);
    // The doorway cells have wall above them, so they are enclosed air joining the room; the
    // faces beyond them open onto the sky.
    assertTrue(r.openingFaces >= 2, "the doorway opens onto the sky");
  }

  @Test
  void roofWithNoWallsIsTooLarge() {
    Grid g = new Grid();
    for (int x = -120; x <= 120; x++) {
      for (int z = -120; z <= 120; z++) {
        g.wall(x, 4, z); // a vast roof on nothing: shade, not a room
      }
    }
    ThermalScanner.Result r = ThermalScanner.scan(g, 0, 1, 0);
    assertEquals(ThermalScanner.Status.TOO_LARGE, r.status);
  }

  @Test
  void skyAirIsNotEnclosed() {
    Grid g = new Grid();
    assertEquals(ThermalScanner.Status.NOT_ENCLOSED, ThermalScanner.scan(g, 0, 1, 0).status);
  }

  @Test
  void sharedWallRecordsTheRoomNextDoor() {
    Grid g = new Grid();
    g.box(0, 0, 0, 6, 4, 6);
    g.box(6, 0, 0, 12, 4, 6); // shares the x = 6 wall
    ThermalScanner.Result a = ThermalScanner.scan(g, 3, 2, 3);
    ThermalScanner.Result b = ThermalScanner.scan(g, 9, 2, 3);
    boolean found = false;
    for (int i = 0; i < a.beyondCell.size(); i++) {
      if (b.cells.contains(a.beyondCell.getLong(i))) {
        found = true;
        break;
      }
    }
    assertTrue(found, "room A's scan sees room B's air through the shared wall");
  }

  @Test
  void glassLosesMoreThanStone() {
    Grid stone = new Grid();
    stone.box(0, 0, 0, 7, 4, 7);
    Grid glazed = new Grid();
    glazed.box(0, 0, 0, 7, 4, 7);
    for (int z = 1; z <= 6; z++) {
      for (int y = 1; y <= 3; y++) {
        glazed.glass.add(ThermalScanner.pack(0, y, z));
      }
    }
    float s = space(stone, 3, 2, 3, 0).envelopeUA();
    float gl = space(glazed, 3, 2, 3, 0).envelopeUA();
    assertTrue(gl > s * 1.1f, "a window wall loses noticeably more heat");
  }

  // endregion

  // region Physics and control

  /**
   * Steps a space under the control law, delivering into {@code deliveryRegion} up to
   * {@code capacity} heat per second (negative for cooling) toward {@code target}, and returns
   * the trace of the region temperature, one sample per second.
   */
  private static float[] run(ThermalSpace s, int deliveryRegion, float capacity, float target,
      int seconds) {
    float[] trace = new float[seconds];
    boolean cooling = capacity < 0;
    float cap = Math.abs(capacity);
    for (int t = 0; t < seconds; t++) {
      s.prepareLoss();
      float need = s.requiredHeat(deliveryRegion, target, HvacSystemControl.TAU);
      float q = cooling ? -clamp(-need, 0, cap) : clamp(need, 0, cap);
      s.pendingHeat[deliveryRegion] += q;
      s.integrate(1.0f);
      trace[t] = s.temperature[deliveryRegion];
    }
    return trace;
  }

  private static float clamp(float v, float lo, float hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  private static void assertSettles(float[] trace, float target, float tolerance,
      float maxOvershoot, boolean heating) {
    float end = trace[trace.length - 1];
    assertEquals(target, end, tolerance, "settles at the target");
    for (float t : trace) {
      float over = heating ? t - target : target - t;
      assertTrue(over <= maxOvershoot, "overshoot " + over + " exceeds " + maxOvershoot);
    }
    // No oscillation: once within half a degree, it stays within a degree.
    boolean arrived = false;
    for (float t : trace) {
      if (Math.abs(t - target) < 0.5f) {
        arrived = true;
      } else if (arrived) {
        assertTrue(Math.abs(t - target) < 1.0f, "left the target after reaching it: " + t);
      }
    }
  }

  @Test
  void cabinetHeaterHoldsARoomAtMinusFifty() {
    Grid g = new Grid();
    g.box(0, 0, 0, 11, 5, 11); // 10 x 4 x 10
    ThermalSpace s = space(g, 5, 2, 5, -50);
    float[] trace = run(s, 0, TileEntityHvacHeater.CABINET_CAPACITY, 66, 1800);
    assertSettles(trace, 66, 0.5f, 1.0f, true);
    // Warm-up from -50 takes a few minutes, not seconds and not an hour.
    int reached = -1;
    for (int i = 0; i < trace.length; i++) {
      if (trace[i] >= 65) {
        reached = i;
        break;
      }
    }
    assertTrue(reached > 60 && reached < 900, "warm-up took " + reached + " s");
  }

  @Test
  void aRoomHoldsItsHeatWhenTheEquipmentStops() {
    Grid g = new Grid();
    g.box(0, 0, 0, 11, 5, 11);
    ThermalSpace s = space(g, 5, 2, 5, -50);
    java.util.Arrays.fill(s.temperature, 66);
    float[] trace = run(s, 0, 0.0f, 66, 60); // nothing delivered for a minute
    float after = trace[trace.length - 1];
    assertTrue(after > 55, "a warm room keeps most of its heat for a minute: " + after);
    assertTrue(after < 66, "but it does cool");
  }

  @Test
  void aFrozenSpaceKeepsItsTemperatureExactly() {
    Grid g = new Grid();
    g.box(0, 0, 0, 11, 5, 11);
    ThermalSpace s = space(g, 5, 2, 5, -50);
    java.util.Arrays.fill(s.temperature, 71);
    s.frozen = true;
    for (int i = 0; i < 3600; i++) {
      s.prepareLoss();
      s.pendingHeat[0] += 500; // whatever arrives while part of it is unloaded is not applied
      s.integrate(1.0f);
    }
    assertEquals(71.0f, s.temperature[0], 0.0f, "an unloaded room holds its temperature");
  }

  @Test
  void aClosetWithARooftopUnitDoesNotRing() {
    Grid g = new Grid();
    g.box(0, 0, 0, 3, 4, 3); // 2 x 3 x 2 closet
    ThermalSpace s = space(g, 1, 1, 1, -50);
    float[] trace = run(s, 0, TileEntityHvacRtuHeater.RTU_CAPACITY, 66, 900);
    assertSettles(trace, 66, 0.5f, 1.0f, true);
  }

  @Test
  void coolingHoldsADesertShopAt79() {
    Grid g = new Grid();
    g.box(0, 0, 0, 13, 6, 13);
    ThermalSpace s = space(g, 6, 2, 6, 176);
    float[] trace = run(s, 0, -2 * TileEntityHvacHeater.CABINET_CAPACITY, 79, 2400);
    assertSettles(trace, 79, 0.5f, 1.0f, false);
  }

  @Test
  void aTallAtriumWarmsItsFloorFromCeilingHeat() {
    Grid g = new Grid();
    g.box(0, 0, 0, 9, 21, 9); // 8 x 20 x 8
    ThermalSpace s = space(g, 4, 10, 4, 0);
    int top = s.regionOfCell(ThermalScanner.pack(4, 19, 4));
    int floor = s.regionOfCell(ThermalScanner.pack(4, 1, 4));
    run(s, top, 4 * TileEntityHvacHeater.CABINET_CAPACITY, 72, 3600);
    assertTrue(s.temperature[floor] > 58, "the floor warms: " + s.temperature[floor]);
    assertTrue(s.temperature[top] - s.temperature[floor] < 15,
        "stratified, but not absurdly: " + (s.temperature[top] - s.temperature[floor]));
    assertTrue(s.temperature[top] >= s.temperature[floor], "warm air stays up");
  }

  @Test
  void neighbouringSpacesExchangeHeatThroughTheirWall() {
    Grid g = new Grid();
    g.box(0, 0, 0, 6, 4, 6);
    g.box(6, 0, 0, 12, 4, 6);
    ThermalScanner.Result ra = ThermalScanner.scan(g, 3, 2, 3);
    ThermalScanner.Result rb = ThermalScanner.scan(g, 9, 2, 3);
    ThermalSpace a = new ThermalSpace(1, ra, 0, 0);
    ThermalSpace b = new ThermalSpace(2, rb, 0, 0);
    a.outdoor = -50;
    b.outdoor = -50;
    java.util.Arrays.fill(a.temperature, 70);
    java.util.Arrays.fill(b.temperature, -50);
    // Wire A -> B couplings the way HvacThermalWorld resolves them.
    int n = ra.beyondCell.size();
    a.coupleRegion = new int[n];
    a.coupleSpace = new ThermalSpace[n];
    a.coupleOtherRegion = new int[n];
    a.coupleUA = new float[n];
    int k = 0;
    for (int i = 0; i < n; i++) {
      long cell = ra.beyondCell.getLong(i);
      if (rb.cells.contains(cell)) {
        a.coupleRegion[k] = ra.beyondRegion.getInt(i);
        a.coupleSpace[k] = b;
        a.coupleOtherRegion[k] = b.regionOfCell(cell);
        a.coupleUA[k] = ra.beyondUA.getFloat(i);
        k++;
      }
    }
    assertTrue(k > 0);
    a.coupleRegion = java.util.Arrays.copyOf(a.coupleRegion, k);
    a.coupleSpace = java.util.Arrays.copyOf(a.coupleSpace, k);
    a.coupleOtherRegion = java.util.Arrays.copyOf(a.coupleOtherRegion, k);
    a.coupleUA = java.util.Arrays.copyOf(a.coupleUA, k);
    float before = a.envelopeLossAt(70);
    java.util.Arrays.fill(b.temperature, 70);
    float after = a.envelopeLossAt(70);
    assertTrue(after < before, "a warm neighbour cuts the loss through the shared wall");
  }

  // endregion
}
