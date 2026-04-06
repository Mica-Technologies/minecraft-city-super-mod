package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link DirectionSixteen} enum, verifying index uniqueness,
 * rotation values, and lookup methods.
 */
class DirectionSixteenTest {

  @Test
  void allDirectionsShouldHaveUniqueIndices() {
    Set<Integer> indices = new HashSet<>();
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertTrue(indices.add(dir.getIndex()),
          "Duplicate index found: " + dir.getIndex() + " for " + dir.name());
    }
  }

  @Test
  void allDirectionsShouldHaveUniqueNames() {
    Set<String> names = new HashSet<>();
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertTrue(names.add(dir.getName()),
          "Duplicate name found: " + dir.getName() + " for " + dir.name());
    }
  }

  @Test
  void allRotationsShouldBeUniqueAndInRange() {
    Set<Float> rotations = new HashSet<>();
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertTrue(dir.getRotation() >= 0.0F && dir.getRotation() < 360.0F,
          "Rotation out of range for " + dir.name() + ": " + dir.getRotation());
      assertTrue(rotations.add(dir.getRotation()),
          "Duplicate rotation found: " + dir.getRotation() + " for " + dir.name());
    }
  }

  @Test
  void shouldHaveExactlySixteenDirections() {
    assertEquals(16, DirectionSixteen.values().length,
        "DirectionSixteen should have exactly 16 values");
  }

  @Test
  void fromNameShouldReturnCorrectDirection() {
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertEquals(dir, DirectionSixteen.fromName(dir.getName()),
          "fromName() should return correct direction for " + dir.getName());
    }
  }

  @Test
  void fromNameShouldReturnNullForUnknown() {
    assertNull(DirectionSixteen.fromName("xyz"),
        "fromName() should return null for unknown name");
  }

  @Test
  void fromIndexShouldReturnCorrectDirection() {
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertEquals(dir, DirectionSixteen.fromIndex(dir.getIndex()),
          "fromIndex() should return correct direction for index " + dir.getIndex());
    }
  }

  @Test
  void fromIndexShouldReturnNullForInvalidIndex() {
    assertNull(DirectionSixteen.fromIndex(-1),
        "fromIndex() should return null for negative index");
    assertNull(DirectionSixteen.fromIndex(999),
        "fromIndex() should return null for out-of-range index");
  }

  @Test
  void toStringShouldMatchGetName() {
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      assertEquals(dir.getName(), dir.toString(),
          "toString() should match getName() for " + dir.name());
    }
  }

  @Test
  void northShouldHaveZeroRotation() {
    assertEquals(0.0F, DirectionSixteen.N.getRotation(),
        "North should have 0 degree rotation");
  }

  @Test
  void southShouldHave180Rotation() {
    assertEquals(180.0F, DirectionSixteen.S.getRotation(),
        "South should have 180 degree rotation");
  }

  @Test
  void rotationsShouldBeMultiplesOf22Point5() {
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      float remainder = dir.getRotation() % 22.5F;
      assertEquals(0.0F, remainder, 0.001F,
          "Rotation for " + dir.name() + " should be a multiple of 22.5 degrees");
    }
  }
}
