package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link BlockUtils} direction mapping methods. These test the pure
 * logic of getRelativeFacing() and getOppositeFacing() without needing a Minecraft world.
 */
class BlockUtilsTest {

  @Test
  void relativeFacingNorthBaseShouldReturnIdentity() {
    // When the block faces NORTH (default), relative facing is identity
    for (EnumFacing direction : EnumFacing.VALUES) {
      assertEquals(direction, BlockUtils.getRelativeFacing(EnumFacing.NORTH, direction),
          "NORTH base should return identity for " + direction);
    }
  }

  @Test
  void relativeFacingSouthShouldRotate180Horizontally() {
    // SOUTH is 180 degrees from NORTH: N->S, E->W, S->N, W->E, UP/DOWN unchanged
    assertEquals(EnumFacing.SOUTH, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.NORTH));
    assertEquals(EnumFacing.WEST, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.EAST));
    assertEquals(EnumFacing.NORTH, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH));
    assertEquals(EnumFacing.EAST, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.WEST));
    assertEquals(EnumFacing.UP, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.UP));
    assertEquals(EnumFacing.DOWN, BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.DOWN));
  }

  @Test
  void relativeFacingEastShouldRotate90CW() {
    // EAST is 90 degrees CW from NORTH: N->E, E->S, S->W, W->N
    assertEquals(EnumFacing.EAST, BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.NORTH));
    assertEquals(EnumFacing.SOUTH, BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.EAST));
    assertEquals(EnumFacing.WEST, BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH));
    assertEquals(EnumFacing.NORTH, BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.WEST));
  }

  @Test
  void relativeFacingWestShouldRotate90CCW() {
    // WEST is 90 degrees CCW from NORTH: N->W, E->N, S->E, W->S
    assertEquals(EnumFacing.WEST, BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.NORTH));
    assertEquals(EnumFacing.NORTH, BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.EAST));
    assertEquals(EnumFacing.EAST, BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH));
    assertEquals(EnumFacing.SOUTH, BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.WEST));
  }

  @ParameterizedTest
  @EnumSource(EnumFacing.class)
  void relativeFacingShouldNeverReturnNull(EnumFacing base) {
    for (EnumFacing direction : EnumFacing.VALUES) {
      assertNotNull(BlockUtils.getRelativeFacing(base, direction),
          "getRelativeFacing(" + base + ", " + direction + ") should not return null");
    }
  }

  @ParameterizedTest
  @EnumSource(EnumFacing.class)
  void oppositeFacingShouldNeverReturnNull(EnumFacing base) {
    for (EnumFacing direction : EnumFacing.VALUES) {
      assertNotNull(BlockUtils.getOppositeFacing(base, direction),
          "getOppositeFacing(" + base + ", " + direction + ") should not return null");
    }
  }

  @Test
  void oppositeFacingNorthBaseShouldReturnOpposite() {
    // When base is NORTH, getOppositeFacing should return direction.getOpposite()
    for (EnumFacing direction : EnumFacing.VALUES) {
      assertEquals(direction.getOpposite(),
          BlockUtils.getOppositeFacing(EnumFacing.NORTH, direction),
          "NORTH base opposite should match EnumFacing.getOpposite() for " + direction);
    }
  }

  @Test
  void relativeFacingUpShouldMapVertically() {
    // When block faces UP: N->UP, S->DOWN, UP->S, DOWN->N
    assertEquals(EnumFacing.UP, BlockUtils.getRelativeFacing(EnumFacing.UP, EnumFacing.NORTH));
    assertEquals(EnumFacing.DOWN, BlockUtils.getRelativeFacing(EnumFacing.UP, EnumFacing.SOUTH));
    assertEquals(EnumFacing.SOUTH, BlockUtils.getRelativeFacing(EnumFacing.UP, EnumFacing.UP));
    assertEquals(EnumFacing.NORTH, BlockUtils.getRelativeFacing(EnumFacing.UP, EnumFacing.DOWN));
  }

  @Test
  void relativeFacingDownShouldMapVertically() {
    // When block faces DOWN: N->DOWN, S->UP, UP->N, DOWN->S
    assertEquals(EnumFacing.DOWN, BlockUtils.getRelativeFacing(EnumFacing.DOWN, EnumFacing.NORTH));
    assertEquals(EnumFacing.UP, BlockUtils.getRelativeFacing(EnumFacing.DOWN, EnumFacing.SOUTH));
    assertEquals(EnumFacing.NORTH, BlockUtils.getRelativeFacing(EnumFacing.DOWN, EnumFacing.UP));
    assertEquals(EnumFacing.SOUTH, BlockUtils.getRelativeFacing(EnumFacing.DOWN, EnumFacing.DOWN));
  }
}
