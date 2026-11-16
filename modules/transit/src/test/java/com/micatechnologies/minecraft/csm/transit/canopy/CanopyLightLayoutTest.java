package com.micatechnologies.minecraft.csm.transit.canopy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The light strip's seats and the Canopy Tool's strip rows.
 *
 * <p>The seat list is built twice, here in {@link CanopyStyle#SEATS} and in
 * {@code gen_transit_canopies.py} ({@code SEATS}), and a strip's {@code seat} indexes it to pick
 * a generated model: the two must agree entry for entry. The expected list is the generator's,
 * printed by {@code python -c "import gen_transit_canopies as g; print(g.SEATS)"}.</p>
 */
class CanopyLightLayoutTest {

  @Test
  void seatsMatchTheGenerator() {
    double[][] expected = {{0, 0}, {1, -22.5}, {1, 22.5}, {2, -45}, {2, 45}, {3, -22.5},
        {3, 22.5}, {4, 22.5}, {5, -22.5}, {5, 22.5}, {6, 0}};
    assertEquals(expected.length, CanopyStyle.SEATS.length);
    for (int i = 0; i < expected.length; i++) {
      assertArrayEquals(expected[i], CanopyStyle.SEATS[i], 1e-9, "seat " + i);
    }
  }

  @Test
  void everyShapedPhaseHasASeat() {
    for (CanopyStyle style : CanopyStyle.values()) {
      for (int p = 0; p < style.getModule(); p++) {
        boolean found = false;
        for (double[] seat : CanopyStyle.SEATS) {
          found |= seat[0] == style.reachAt(p) && seat[1] == style.slopeAt(p);
        }
        assertTrue(found, style + " phase " + p);
      }
    }
  }

  @Test
  void flatCentreAndEdges() {
    assertEquals(Arrays.asList(4), ItemCanopyTool.stripRows(CanopyStyle.FLAT, 0, 8,
        ItemCanopyTool.LIGHTS_STRIP_CENTRE));
    assertEquals(Arrays.asList(1, 7), ItemCanopyTool.stripRows(CanopyStyle.FLAT, 0, 8,
        ItemCanopyTool.LIGHTS_STRIP_EDGES));
    // two deep: the middle alone
    assertEquals(Arrays.asList(10), ItemCanopyTool.stripRows(CanopyStyle.GLASS, 10, 11,
        ItemCanopyTool.LIGHTS_STRIP_EDGES));
  }

  @Test
  void shapedRowsMoveToTheLowSoffit() {
    // one vault arch, six deep: the middle is the crown; the low rows are the springings, 0, 5
    List<Integer> centre = ItemCanopyTool.stripRows(CanopyStyle.VAULT, 0, 5,
        ItemCanopyTool.LIGHTS_STRIP_CENTRE);
    assertEquals(1, centre.size());
    assertTrue(Arrays.asList(0, 5).contains(centre.get(0)));
    assertEquals(Arrays.asList(0, 5), ItemCanopyTool.stripRows(CanopyStyle.VAULT, 0, 5,
        ItemCanopyTool.LIGHTS_STRIP_EDGES));
    // two arches: the middle is at the gutter between them (the first arch's far springing)
    assertEquals(Arrays.asList(5), ItemCanopyTool.stripRows(CanopyStyle.VAULT, 0, 11,
        ItemCanopyTool.LIGHTS_STRIP_CENTRE));
    // a butterfly's valley, phases 1 and 2
    assertEquals(Arrays.asList(1, 2), ItemCanopyTool.stripRows(CanopyStyle.BUTTERFLY, 0, 3,
        ItemCanopyTool.LIGHTS_STRIP_EDGES));
    // a sawtooth's low edge only, so the two edge rows of one module are one row
    assertEquals(Arrays.asList(0), ItemCanopyTool.stripRows(CanopyStyle.SAWTOOTH, 0, 2,
        ItemCanopyTool.LIGHTS_STRIP_EDGES));
    for (int mode : new int[]{ItemCanopyTool.LIGHTS_STRIP_CENTRE,
        ItemCanopyTool.LIGHTS_STRIP_EDGES}) {
      for (int row : ItemCanopyTool.stripRows(CanopyStyle.VAULT, 3, 26, mode)) {
        assertTrue(CanopyStyle.VAULT.lightAt((row - 3) % 6), "row " + row);
      }
    }
  }
}
