package com.micatechnologies.minecraft.csm.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.lighting.BlockFloodlight.Surface;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The floodlights' aims (each surface's pitch, turned with the facing the way the blockstate turns
 * the model) and the RealGrid crossarm names a flood bolts to.
 */
class FloodlightTest {

  @Test
  void wallFloodAimsOutAndDown() {
    double[] a = BlockFloodlight.aim(Surface.WALL, EnumFacing.NORTH);
    assertEquals(0.0, a[0], 1e-9);
    assertEquals(-Math.sin(Math.toRadians(35)), a[1], 1e-9);
    assertEquals(-Math.cos(Math.toRadians(35)), a[2], 1e-9);
    // turned east, as the blockstate's y 90 turns the model
    double[] e = BlockFloodlight.aim(Surface.WALL, EnumFacing.EAST);
    assertEquals(Math.cos(Math.toRadians(35)), e[0], 1e-9);
    assertEquals(0.0, e[2], 1e-9);
  }

  @Test
  void groundUplightAimsUpAndCeilingDown() {
    assertTrue(BlockFloodlight.aim(Surface.GROUND, EnumFacing.SOUTH)[1] > 0.7);
    assertTrue(BlockFloodlight.aim(Surface.GROUND, EnumFacing.SOUTH)[2] > 0.5);
    assertTrue(BlockFloodlight.aim(Surface.CEILING, EnumFacing.WEST)[1] < -0.7);
    assertTrue(BlockFloodlight.aim(Surface.POLE_TOP, EnumFacing.WEST)[1] < 0);
    for (Surface s : Surface.values()) {
      double[] a = BlockFloodlight.aim(s, EnumFacing.NORTH);
      assertEquals(1.0, a[0] * a[0] + a[1] * a[1] + a[2] * a[2], 1e-9, s.name());
    }
  }

  @Test
  void companionSuffixes() {
    assertEquals("", Surface.WALL.suffix());
    assertEquals("_g", Surface.GROUND.suffix());
    assertEquals("_g", Surface.POLE_TOP.suffix());
    assertEquals("_c", Surface.CEILING.suffix());
    assertEquals("_x", Surface.CROSSARM.suffix());
  }

  @Test
  void realGridCrossarmNames() {
    assertTrue(RealGridCompat.isCrossarmName(new ResourceLocation("realgrid", "crossarm_brooks_brown")));
    assertTrue(RealGridCompat.isCrossarmName(new ResourceLocation("realgrid", "crossarm_pupi_white")));
    assertFalse(RealGridCompat.isCrossarmName(new ResourceLocation("realgrid", "crossarm_brace_wood")));
    assertFalse(RealGridCompat.isCrossarmName(
        new ResourceLocation("realgrid", "crossarm_alley_brace_metal_6")));
    assertFalse(RealGridCompat.isCrossarmName(new ResourceLocation("realgrid", "crossarm_spacer_wood")));
    assertFalse(RealGridCompat.isCrossarmName(new ResourceLocation("realgrid", "crossarm_guy_mount")));
    assertFalse(RealGridCompat.isCrossarmName(new ResourceLocation("csm", "crossarm_brooks_brown")));
    assertFalse(RealGridCompat.isCrossarmName(null));
  }
}
