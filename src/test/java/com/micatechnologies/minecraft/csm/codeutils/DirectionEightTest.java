package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link DirectionEight} enum, verifying index uniqueness,
 * name formatting, and enum completeness.
 */
class DirectionEightTest {

  @Test
  void allDirectionsShouldHaveUniqueIndices() {
    Set<Integer> indices = new HashSet<>();
    for (DirectionEight dir : DirectionEight.values()) {
      assertTrue(indices.add(dir.getIndex()),
          "Duplicate index found: " + dir.getIndex() + " for " + dir.name());
    }
  }

  @Test
  void allDirectionsShouldHaveUniqueNames() {
    Set<String> names = new HashSet<>();
    for (DirectionEight dir : DirectionEight.values()) {
      assertTrue(names.add(dir.getName()),
          "Duplicate name found: " + dir.getName() + " for " + dir.name());
    }
  }

  @Test
  void namesShouldBeLowercase() {
    for (DirectionEight dir : DirectionEight.values()) {
      assertEquals(dir.getName(), dir.getName().toLowerCase(),
          "Name should be lowercase for " + dir.name());
    }
  }

  @Test
  void toStringShouldMatchGetName() {
    for (DirectionEight dir : DirectionEight.values()) {
      assertEquals(dir.getName(), dir.toString(),
          "toString() should match getName() for " + dir.name());
    }
  }

  @Test
  void shouldHaveExactlyEightDirections() {
    assertEquals(8, DirectionEight.values().length,
        "DirectionEight should have exactly 8 values");
  }

  @Test
  void indicesShouldBeContiguous() {
    for (int i = 0; i < DirectionEight.values().length; i++) {
      final int index = i;
      boolean found = false;
      for (DirectionEight dir : DirectionEight.values()) {
        if (dir.getIndex() == index) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Missing direction with index " + index);
    }
  }

  @Test
  void cardinalDirectionsShouldHaveLowerIndices() {
    // S=0, W=1, N=2, E=3 should all be < 4
    assertTrue(DirectionEight.S.getIndex() < 4);
    assertTrue(DirectionEight.W.getIndex() < 4);
    assertTrue(DirectionEight.N.getIndex() < 4);
    assertTrue(DirectionEight.E.getIndex() < 4);
    // Diagonals should be >= 4
    assertTrue(DirectionEight.SE.getIndex() >= 4);
    assertTrue(DirectionEight.SW.getIndex() >= 4);
    assertTrue(DirectionEight.NW.getIndex() >= 4);
    assertTrue(DirectionEight.NE.getIndex() >= 4);
  }
}
