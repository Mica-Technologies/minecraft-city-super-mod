package com.micatechnologies.minecraft.csm.transit.stop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A route id's key must be the old route number for 1 to 99, so every stop flag saved before ids
 * had letters keeps its routes and countdowns; every other id must round-trip and never collide.
 */
class TransitRouteTest {

  @Test
  void numbersAreTheirOwnKeys() {
    for (int n = 1; n < TransitRoute.PACKED_BASE; n++) {
      assertEquals(n, TransitRoute.parse(Integer.toString(n)));
      assertEquals(Integer.toString(n), TransitRoute.label(n));
    }
    assertEquals(7, TransitRoute.parse("07"));
    assertEquals(7, TransitRoute.parse("007"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"B", "M15", "X", "ZZZ", "100", "999", "A1", "9B", "Q"})
  void otherIdsRoundTrip(String id) {
    int key = TransitRoute.parse(id);
    assertTrue(key >= TransitRoute.PACKED_BASE, id);
    assertEquals(id, TransitRoute.label(key));
    assertEquals(key, TransitRoute.parse(id.toLowerCase()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "0", "00", "ABCD", "1234", "B-1", "é", "#"})
  void junkIsNoRoute(String id) {
    assertEquals(0, TransitRoute.parse(id));
  }

  @Test
  void noTwoIdsShareAKey() {
    String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Set<Integer> keys = new HashSet<>();
    Set<String> labels = new HashSet<>();
    for (int len = 1; len <= TransitRoute.MAX_LENGTH; len++) {
      int[] idx = new int[len];
      while (true) {
        StringBuilder sb = new StringBuilder();
        for (int i : idx) {
          sb.append(alphabet.charAt(i));
        }
        int key = TransitRoute.parse(sb.toString());
        if (key != 0) {
          String label = TransitRoute.label(key);
          if (labels.add(label)) {
            assertTrue(keys.add(key), "two ids share key " + key + ": " + label);
          }
        }
        int k = len - 1;
        while (k >= 0 && ++idx[k] == alphabet.length()) {
          idx[k--] = 0;
        }
        if (k < 0) {
          break;
        }
      }
    }
  }

  @Test
  void aLineReadsItsRoutes() {
    assertArrayEquals(new int[]{TransitRoute.parse("B"), 30},
        TransitRoute.parseLine("Routes B, 30"));
    assertArrayEquals(new int[]{12, 40}, TransitRoute.parseLine("12 / 40 / 12"));
    assertArrayEquals(new int[0], TransitRoute.parseLine(null));
  }
}
