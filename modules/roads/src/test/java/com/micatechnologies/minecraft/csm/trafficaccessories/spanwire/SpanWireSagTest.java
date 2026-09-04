package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SpanWireSagTest {

  /** Each preset's slack, fed to the real solver, must hang by the sag the preset is named for. */
  @ParameterizedTest
  @EnumSource(SpanWireSag.class)
  void presetHangsByItsNamedSag(SpanWireSag preset) {
    final double length = 20.0;
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 10.0, 0.0), new Vec3d(length, 10.0, 0.0), preset.getSlack());
    final double measured = cable.sag() / length;
    // The parabolic relation is an approximation of the catenary; a few percent of the sag itself
    // is the agreement expected, and far tighter than the gap between neighbouring presets.
    assertEquals(preset.getSagRatio(), measured, preset.getSagRatio() * 0.05,
        preset + " should sag about " + preset.getSagRatio() + " of its length");
  }

  @Test
  void standardIsTheSolverDefaultExactly() {
    assertEquals(SpanWireCatenary.DEFAULT_SLACK, SpanWireSag.STANDARD.getSlack(), 0.0);
    assertSame(SpanWireSag.STANDARD, SpanWireSag.closestTo(SpanWireCatenary.DEFAULT_SLACK));
  }

  @Test
  void presetsAreStrictlyOrderedBySlack() {
    final SpanWireSag[] values = SpanWireSag.values();
    for (int i = 1; i < values.length; i++) {
      assertTrue(values[i].getSlack() > values[i - 1].getSlack(),
          values[i] + " should carry more slack than " + values[i - 1]);
    }
  }

  @Test
  void closestToRoundTripsEveryPresetAndWraps() {
    for (SpanWireSag preset : SpanWireSag.values()) {
      assertSame(preset, SpanWireSag.closestTo(preset.getSlack()));
    }
    assertSame(SpanWireSag.TAUT, SpanWireSag.LOOSE.getNext());
  }
}
