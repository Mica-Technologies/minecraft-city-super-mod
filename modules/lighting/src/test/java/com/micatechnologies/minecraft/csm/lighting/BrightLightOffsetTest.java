package com.micatechnologies.minecraft.csm.lighting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

/**
 * A facing-relative light column (the cantilevered post tops) falls ahead of the fixture, the way
 * its head points, and its side offset to the fixture's right, whichever way it is turned.
 */
class BrightLightOffsetTest {

  @Test
  void forwardFollowsTheFacing() {
    // The fixtures are modelled facing north, the head toward -z
    assertArrayEquals(new int[]{0, -1}, BrightLightOffset.worldOffset(1, 0, EnumFacing.NORTH));
    assertArrayEquals(new int[]{1, 0}, BrightLightOffset.worldOffset(1, 0, EnumFacing.EAST));
    assertArrayEquals(new int[]{0, 1}, BrightLightOffset.worldOffset(1, 0, EnumFacing.SOUTH));
    assertArrayEquals(new int[]{-1, 0}, BrightLightOffset.worldOffset(1, 0, EnumFacing.WEST));
  }

  @Test
  void rightIsClockwiseFromAbove() {
    assertArrayEquals(new int[]{1, 0}, BrightLightOffset.worldOffset(0, 1, EnumFacing.NORTH));
    assertArrayEquals(new int[]{0, 1}, BrightLightOffset.worldOffset(0, 1, EnumFacing.EAST));
    assertArrayEquals(new int[]{-1, 0}, BrightLightOffset.worldOffset(0, 1, EnumFacing.SOUTH));
    assertArrayEquals(new int[]{0, -1}, BrightLightOffset.worldOffset(0, 1, EnumFacing.WEST));
  }

  @Test
  void noOffsetStaysPut() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      assertArrayEquals(new int[]{0, 0}, BrightLightOffset.worldOffset(0, 0, f));
    }
  }
}
