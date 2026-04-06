package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalPhase} NBT serialization round-trips and edge cases.
 */
class TrafficSignalPhaseTest {

  // region: Basic round-trip

  @Test
  void emptyPhaseRoundTrip() {
    TrafficSignalPhase phase = new TrafficSignalPhase(1, TrafficSignalPhaseApplicability.ALL_EAST);

    NBTTagCompound nbt = phase.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertEquals(1, deserialized.getCircuit());
    assertEquals(TrafficSignalPhaseApplicability.ALL_EAST, deserialized.getApplicability());
    assertNull(deserialized.getUpcomingPhase());
    assertTrue(deserialized.getOffSignals().isEmpty());
    assertTrue(deserialized.getGreenSignals().isEmpty());
    assertTrue(deserialized.getYellowSignals().isEmpty());
    assertTrue(deserialized.getRedSignals().isEmpty());
    assertTrue(deserialized.getWalkSignals().isEmpty());
    assertTrue(deserialized.getFlashDontWalkSignals().isEmpty());
    assertTrue(deserialized.getDontWalkSignals().isEmpty());
    assertTrue(deserialized.getFyaSignals().isEmpty());
  }

  // endregion

  // region: Populated signal lists

  @Test
  void populatedSignalListsRoundTrip() {
    TrafficSignalPhase phase = new TrafficSignalPhase(2, TrafficSignalPhaseApplicability.ALL_NORTH);

    phase.addOffSignal(new BlockPos(1, 2, 3));
    phase.addGreenSignal(new BlockPos(10, 20, 30));
    phase.addGreenSignal(new BlockPos(11, 21, 31));
    phase.addYellowSignal(new BlockPos(40, 50, 60));
    phase.addRedSignal(new BlockPos(70, 80, 90));
    phase.addWalkSignal(new BlockPos(100, 110, 120));
    phase.addFlashDontWalkSignal(new BlockPos(130, 140, 150));
    phase.addDontWalkSignal(new BlockPos(160, 170, 180));
    phase.addFyaSignal(new BlockPos(200, 210, 220));

    NBTTagCompound nbt = phase.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertEquals(1, deserialized.getOffSignals().size());
    assertEquals(new BlockPos(1, 2, 3), deserialized.getOffSignals().get(0));

    assertEquals(2, deserialized.getGreenSignals().size());
    assertEquals(new BlockPos(10, 20, 30), deserialized.getGreenSignals().get(0));
    assertEquals(new BlockPos(11, 21, 31), deserialized.getGreenSignals().get(1));

    assertEquals(1, deserialized.getYellowSignals().size());
    assertEquals(1, deserialized.getRedSignals().size());
    assertEquals(1, deserialized.getWalkSignals().size());
    assertEquals(1, deserialized.getFlashDontWalkSignals().size());
    assertEquals(1, deserialized.getDontWalkSignals().size());
    assertEquals(1, deserialized.getFyaSignals().size());
    assertEquals(new BlockPos(200, 210, 220), deserialized.getFyaSignals().get(0));
  }

  // endregion

  // region: Upcoming phase

  @Test
  void upcomingPhaseRoundTrip() {
    TrafficSignalPhase upcoming =
        new TrafficSignalPhase(3, TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
    upcoming.addYellowSignal(new BlockPos(5, 5, 5));

    TrafficSignalPhase main =
        new TrafficSignalPhase(2, upcoming, TrafficSignalPhaseApplicability.ALL_SOUTH);
    main.addGreenSignal(new BlockPos(10, 10, 10));

    NBTTagCompound nbt = main.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertEquals(2, deserialized.getCircuit());
    assertEquals(TrafficSignalPhaseApplicability.ALL_SOUTH, deserialized.getApplicability());
    assertEquals(1, deserialized.getGreenSignals().size());

    TrafficSignalPhase deserializedUpcoming = deserialized.getUpcomingPhase();
    assertNotNull(deserializedUpcoming);
    assertEquals(3, deserializedUpcoming.getCircuit());
    assertEquals(TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING,
        deserializedUpcoming.getApplicability());
    assertEquals(1, deserializedUpcoming.getYellowSignals().size());
    assertEquals(new BlockPos(5, 5, 5), deserializedUpcoming.getYellowSignals().get(0));
  }

  @Test
  void noUpcomingPhaseResultsInNull() {
    TrafficSignalPhase phase = new TrafficSignalPhase(0, TrafficSignalPhaseApplicability.NONE);

    NBTTagCompound nbt = phase.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertNull(deserialized.getUpcomingPhase());
  }

  // endregion

  // region: Circuit values

  @Test
  void circuitNotApplicableValueRoundTrip() {
    TrafficSignalPhase phase =
        new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
            TrafficSignalPhaseApplicability.ALL_RED);

    NBTTagCompound nbt = phase.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertEquals(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, deserialized.getCircuit());
  }

  // endregion

  // region: Applicability values

  @Test
  void allApplicabilityValuesRoundTrip() {
    for (TrafficSignalPhaseApplicability applicability :
        TrafficSignalPhaseApplicability.values()) {
      TrafficSignalPhase phase = new TrafficSignalPhase(0, applicability);

      NBTTagCompound nbt = phase.toNBT();
      TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

      assertEquals(applicability, deserialized.getApplicability(),
          "Failed round-trip for applicability: " + applicability.name());
    }
  }

  // endregion

  // region: Null NBT handling

  @Test
  void fromNBTWithNullThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> TrafficSignalPhase.fromNBT(null));
  }

  // endregion

  // region: Priority indicator

  @Test
  void priorityIndicatorMatchesCircuitAndApplicability() {
    TrafficSignalPhase phase =
        new TrafficSignalPhase(5, TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN);

    var indicator = phase.getPriorityIndicator();
    assertEquals(5, indicator.getFirst());
    assertEquals(TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN, indicator.getSecond());
  }

  // endregion

  // region: Batch add signals

  @Test
  void addMultipleSignalsAtOnce() {
    TrafficSignalPhase phase = new TrafficSignalPhase(1, TrafficSignalPhaseApplicability.NONE);
    phase.addOffSignals(
        Arrays.asList(new BlockPos(1, 1, 1), new BlockPos(2, 2, 2), new BlockPos(3, 3, 3)));

    NBTTagCompound nbt = phase.toNBT();
    TrafficSignalPhase deserialized = TrafficSignalPhase.fromNBT(nbt);

    assertEquals(3, deserialized.getOffSignals().size());
  }

  // endregion
}
