package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RotationUtils} bounding box rotation methods.
 */
class RotationUtilsTest {

  /** A non-symmetric bounding box to test rotation behavior. */
  private static final AxisAlignedBB TEST_BB = new AxisAlignedBB(
      0.25, 0.0, 0.0,   // minX, minY, minZ
      0.75, 0.5, 0.25   // maxX, maxY, maxZ
  );

  @Test
  void rotateByNorthShouldReturnIdentity() {
    AxisAlignedBB result = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.NORTH);
    assertEquals(TEST_BB.minX, result.minX, 1e-9);
    assertEquals(TEST_BB.minY, result.minY, 1e-9);
    assertEquals(TEST_BB.minZ, result.minZ, 1e-9);
    assertEquals(TEST_BB.maxX, result.maxX, 1e-9);
    assertEquals(TEST_BB.maxY, result.maxY, 1e-9);
    assertEquals(TEST_BB.maxZ, result.maxZ, 1e-9);
  }

  @Test
  void rotateBySouthShouldMirrorXZ() {
    AxisAlignedBB result = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.SOUTH);
    // South rotation: (x,y,z) -> (1-x, y, 1-z), with min/max swapped appropriately
    assertEquals(1.0 - TEST_BB.maxX, result.minX, 1e-9);
    assertEquals(TEST_BB.minY, result.minY, 1e-9);
    assertEquals(1.0 - TEST_BB.maxZ, result.minZ, 1e-9);
    assertEquals(1.0 - TEST_BB.minX, result.maxX, 1e-9);
    assertEquals(TEST_BB.maxY, result.maxY, 1e-9);
    assertEquals(1.0 - TEST_BB.minZ, result.maxZ, 1e-9);
  }

  @Test
  void rotateByEnumFacingShouldNeverReturnNull() {
    for (EnumFacing facing : EnumFacing.VALUES) {
      AxisAlignedBB result = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, facing);
      assertNotNull(result, "Rotation by " + facing + " should not return null");
    }
  }

  @Test
  void rotateByDirectionEightCardinalShouldMatchEnumFacing() {
    // Cardinal DirectionEight rotations should produce the same result as EnumFacing rotations
    assertBBEquals(
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.NORTH),
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, DirectionEight.N));
    assertBBEquals(
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.SOUTH),
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, DirectionEight.S));
    assertBBEquals(
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.EAST),
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, DirectionEight.E));
    assertBBEquals(
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.WEST),
        RotationUtils.rotateBoundingBoxByFacing(TEST_BB, DirectionEight.W));
  }

  @Test
  void rotateByDirectionEightDiagonalShouldNotReturnNull() {
    DirectionEight[] diagonals = {DirectionEight.NE, DirectionEight.NW,
        DirectionEight.SE, DirectionEight.SW};
    for (DirectionEight dir : diagonals) {
      AxisAlignedBB result = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, dir);
      assertNotNull(result, "Diagonal rotation by " + dir + " should not return null");
    }
  }

  @Test
  void rotateByDirectionSixteenShouldNotReturnNull() {
    for (DirectionSixteen dir : DirectionSixteen.values()) {
      AxisAlignedBB result = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, dir);
      assertNotNull(result, "Rotation by " + dir + " should not return null");
    }
  }

  @Test
  void rotateNorthThenSouthShouldReturnOriginal() {
    // Rotating by SOUTH twice should return to original (SOUTH is 180-degree rotation)
    AxisAlignedBB first = RotationUtils.rotateBoundingBoxByFacing(TEST_BB, EnumFacing.SOUTH);
    AxisAlignedBB second = RotationUtils.rotateBoundingBoxByFacing(first, EnumFacing.SOUTH);
    assertBBEquals(TEST_BB, second);
  }

  private static void assertBBEquals(AxisAlignedBB expected, AxisAlignedBB actual) {
    assertEquals(expected.minX, actual.minX, 1e-9, "minX mismatch");
    assertEquals(expected.minY, actual.minY, 1e-9, "minY mismatch");
    assertEquals(expected.minZ, actual.minZ, 1e-9, "minZ mismatch");
    assertEquals(expected.maxX, actual.maxX, 1e-9, "maxX mismatch");
    assertEquals(expected.maxY, actual.maxY, 1e-9, "maxY mismatch");
    assertEquals(expected.maxZ, actual.maxZ, 1e-9, "maxZ mismatch");
  }
}
