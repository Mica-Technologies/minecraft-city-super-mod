package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * A flash-on-call beacon (an RRFB, an in-roadway warning light) flashes in its own animated
 * texture, so the controller's flash and fault flash must hold it at one colour through both of
 * their halves rather than switch it with the heads: a block update every half second for
 * nothing, and half of the beacon's own sequence cut away.
 */
class FlashOnCallBeaconFlashTest {

  private static final BlockPos RRFB = new BlockPos(10, 64, 10);
  private static final BlockPos HEAD = new BlockPos(12, 64, 10);

  private static TrafficSignalPhase phase() {
    return new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
        TrafficSignalPhaseApplicability.NONE);
  }

  @Test
  void litInOneHalfIsLitInBoth() {
    TrafficSignalPhase first = phase();
    TrafficSignalPhase second = phase();
    first.addYellowSignals(Collections.singletonList(RRFB));
    second.addOffSignals(Collections.singletonList(RRFB));
    // a head flashing alongside it keeps alternating
    first.addYellowSignals(Collections.singletonList(HEAD));
    second.addOffSignals(Collections.singletonList(HEAD));

    TrafficSignalPhases.holdLit(Collections.singletonList(RRFB), first, second);

    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_YELLOW),
        first.resolveVehicleSignalStates().get(RRFB));
    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_YELLOW),
        second.resolveVehicleSignalStates().get(RRFB));
    assertTrue(second.getOffSignals().contains(HEAD), "the head still flashes");
    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_OFF),
        second.resolveVehicleSignalStates().get(HEAD));
  }

  @Test
  void litInTheSecondHalfOnlyTakesThatColour() {
    TrafficSignalPhase first = phase();
    TrafficSignalPhase second = phase();
    first.addOffSignals(Collections.singletonList(RRFB));
    second.addRedSignals(Collections.singletonList(RRFB));

    TrafficSignalPhases.holdLit(Arrays.asList(RRFB), first, second);

    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_RED),
        first.resolveVehicleSignalStates().get(RRFB));
    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_RED),
        second.resolveVehicleSignalStates().get(RRFB));
  }

  @Test
  void darkInBothStaysDark() {
    TrafficSignalPhase first = phase();
    TrafficSignalPhase second = phase();
    first.addOffSignals(Collections.singletonList(RRFB));
    second.addOffSignals(Collections.singletonList(RRFB));

    TrafficSignalPhases.holdLit(Collections.singletonList(RRFB), first, second);

    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_OFF),
        first.resolveVehicleSignalStates().get(RRFB));
    assertEquals(Integer.valueOf(AbstractBlockControllableSignal.SIGNAL_OFF),
        second.resolveVehicleSignalStates().get(RRFB));
  }
}
