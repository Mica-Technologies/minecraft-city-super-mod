package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ControllerTickContext}, verifying that all constructor parameters are
 * correctly stored and returned by their corresponding getters.
 */
class ControllerTickContextTest {

  @Test
  void allGettersReturnConstructorValues() {
    TrafficSignalControllerMode configuredMode = TrafficSignalControllerMode.NORMAL;
    TrafficSignalControllerMode operatingMode = TrafficSignalControllerMode.FLASH;

    ControllerTickContext ctx = new ControllerTickContext(
        null, // world (cannot instantiate without running game)
        configuredMode,
        operatingMode,
        null, // circuits
        null, // overlaps
        null, // cachedPhases
        null, // originalPhase
        1000L, // timeSinceLastPhaseApplicabilityChange
        2000L, // timeSinceLastPhaseChange
        true,  // alternatingFlash
        false, // overlapPedestrianSignals
        40L,   // yellowTime
        80L,   // flashDontWalkTime
        20L,   // allRedTime
        100L,  // minRequestableServiceTime
        300L,  // maxRequestableServiceTime
        60L,   // minGreenTime
        120L,  // maxGreenTime
        30L,   // minGreenTimeSecondary
        90L,   // maxGreenTimeSecondary
        50L,   // dedicatedPedSignalTime
        10L,   // leadPedestrianIntervalTime
        true   // allRedFlash
    );

    assertNull(ctx.getWorld());
    assertEquals(configuredMode, ctx.getConfiguredMode());
    assertEquals(operatingMode, ctx.getOperatingMode());
    assertNull(ctx.getCircuits());
    assertNull(ctx.getOverlaps());
    assertNull(ctx.getCachedPhases());
    assertNull(ctx.getOriginalPhase());
    assertEquals(1000L, ctx.getTimeSinceLastPhaseApplicabilityChange());
    assertEquals(2000L, ctx.getTimeSinceLastPhaseChange());
    assertTrue(ctx.isAlternatingFlash());
    assertFalse(ctx.isOverlapPedestrianSignals());
    assertEquals(40L, ctx.getYellowTime());
    assertEquals(80L, ctx.getFlashDontWalkTime());
    assertEquals(20L, ctx.getAllRedTime());
    assertEquals(100L, ctx.getMinRequestableServiceTime());
    assertEquals(300L, ctx.getMaxRequestableServiceTime());
    assertEquals(60L, ctx.getMinGreenTime());
    assertEquals(120L, ctx.getMaxGreenTime());
    assertEquals(30L, ctx.getMinGreenTimeSecondary());
    assertEquals(90L, ctx.getMaxGreenTimeSecondary());
    assertEquals(50L, ctx.getDedicatedPedSignalTime());
    assertEquals(10L, ctx.getLeadPedestrianIntervalTime());
    assertTrue(ctx.isAllRedFlash());
  }

  @Test
  void nullObjectParametersAreStoredAsNull() {
    ControllerTickContext ctx = new ControllerTickContext(
        null, null, null, null, null, null, null,
        0L, 0L, false, false,
        0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false
    );

    assertNull(ctx.getWorld());
    assertNull(ctx.getConfiguredMode());
    assertNull(ctx.getOperatingMode());
    assertNull(ctx.getCircuits());
    assertNull(ctx.getOverlaps());
    assertNull(ctx.getCachedPhases());
    assertNull(ctx.getOriginalPhase());
  }

  @Test
  void extremeLongValuesArePreserved() {
    ControllerTickContext ctx = new ControllerTickContext(
        null,
        TrafficSignalControllerMode.REQUESTABLE,
        TrafficSignalControllerMode.MANUAL_OFF,
        null, null, null, null,
        Long.MAX_VALUE,
        Long.MIN_VALUE,
        false, true,
        Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
        Long.MIN_VALUE, Long.MIN_VALUE,
        Long.MAX_VALUE, Long.MAX_VALUE,
        Long.MIN_VALUE, Long.MIN_VALUE,
        Long.MAX_VALUE, Long.MIN_VALUE,
        false
    );

    assertEquals(Long.MAX_VALUE, ctx.getTimeSinceLastPhaseApplicabilityChange());
    assertEquals(Long.MIN_VALUE, ctx.getTimeSinceLastPhaseChange());
    assertEquals(Long.MAX_VALUE, ctx.getYellowTime());
    assertEquals(Long.MIN_VALUE, ctx.getMinRequestableServiceTime());
    assertEquals(Long.MAX_VALUE, ctx.getDedicatedPedSignalTime());
    assertEquals(Long.MIN_VALUE, ctx.getLeadPedestrianIntervalTime());
  }

  @Test
  void booleanFalseValuesArePreserved() {
    ControllerTickContext ctx = new ControllerTickContext(
        null, null, null, null, null, null, null,
        0L, 0L,
        false, false,
        0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
        false
    );

    assertFalse(ctx.isAlternatingFlash());
    assertFalse(ctx.isOverlapPedestrianSignals());
    assertFalse(ctx.isAllRedFlash());
  }

  @Test
  void allEnumModesAccepted() {
    for (TrafficSignalControllerMode mode : TrafficSignalControllerMode.values()) {
      ControllerTickContext ctx = new ControllerTickContext(
          null, mode, mode, null, null, null, null,
          0L, 0L, false, false,
          0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false
      );
      assertEquals(mode, ctx.getConfiguredMode());
      assertEquals(mode, ctx.getOperatingMode());
    }
  }
}
