package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalControllerCircuits} NBT serialization, circuit management,
 * device linking, and forAllSignals iteration.
 */
class TrafficSignalControllerCircuitsTest {

  // region: NBT round-trip with multiple circuits

  @Test
  void multipleCircuitsNbtRoundTrip() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.linkThroughSignal(new BlockPos(1, 2, 3));
    c1.linkLeftSignal(new BlockPos(4, 5, 6));

    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.linkRightSignal(new BlockPos(10, 20, 30));
    c2.linkSensor(new BlockPos(40, 50, 60));

    circuits.addCircuit(c1);
    circuits.addCircuit(c2);

    NBTTagCompound nbt = circuits.toNBT();
    TrafficSignalControllerCircuits deserialized = TrafficSignalControllerCircuits.fromNBT(nbt);

    assertEquals(2, deserialized.getCircuitCount());
    assertEquals(circuits, deserialized);
  }

  // endregion

  // region: Empty round-trip

  @Test
  void emptyCircuitsNbtRoundTrip() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    NBTTagCompound nbt = circuits.toNBT();
    TrafficSignalControllerCircuits deserialized = TrafficSignalControllerCircuits.fromNBT(nbt);

    assertEquals(0, deserialized.getCircuitCount());
    assertTrue(deserialized.getCircuits().isEmpty());
  }

  // endregion

  // region: fromNBT null throws

  @Test
  void fromNbtNullThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TrafficSignalControllerCircuits.fromNBT(null));
  }

  // endregion

  // region: getCircuit valid index

  @Test
  void getCircuitValidIndex() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.linkThroughSignal(new BlockPos(1, 1, 1));
    circuits.addCircuit(c1);

    TrafficSignalControllerCircuit result = circuits.getCircuit(0);
    assertSame(c1, result);
  }

  // endregion

  // region: getCircuit out-of-bounds creates new circuit

  @Test
  void getCircuitOutOfBoundsCreatesNew() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    assertEquals(0, circuits.getCircuitCount());

    TrafficSignalControllerCircuit created = circuits.getCircuit(5);
    assertNotNull(created);
    // The new circuit gets added to the list
    assertEquals(1, circuits.getCircuitCount());
  }

  @Test
  void getCircuitNegativeIndexCreatesNew() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit created = circuits.getCircuit(-1);
    assertNotNull(created);
    assertEquals(1, circuits.getCircuitCount());
  }

  // endregion

  // region: getCircuits

  @Test
  void getCircuitsReturnsAllCircuits() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    TrafficSignalControllerCircuit c3 = new TrafficSignalControllerCircuit();
    circuits.addCircuit(c1);
    circuits.addCircuit(c2);
    circuits.addCircuit(c3);

    assertEquals(3, circuits.getCircuits().size());
    assertSame(c1, circuits.getCircuits().get(0));
    assertSame(c2, circuits.getCircuits().get(1));
    assertSame(c3, circuits.getCircuits().get(2));
  }

  // endregion

  // region: removeCircuit

  @Test
  void removeCircuit() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    circuits.addCircuit(c1);
    assertEquals(1, circuits.getCircuitCount());

    assertTrue(circuits.removeCircuit(c1));
    assertEquals(0, circuits.getCircuitCount());
  }

  @Test
  void removeCircuitNotPresent() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    assertFalse(circuits.removeCircuit(c1));
  }

  // endregion

  // region: isDeviceLinked across circuits

  @Test
  void isDeviceLinkedAcrossCircuits() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.linkThroughSignal(new BlockPos(1, 1, 1));
    circuits.addCircuit(c1);

    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.linkSensor(new BlockPos(2, 2, 2));
    circuits.addCircuit(c2);

    assertTrue(circuits.isDeviceLinked(new BlockPos(1, 1, 1)));
    assertTrue(circuits.isDeviceLinked(new BlockPos(2, 2, 2)));
    assertFalse(circuits.isDeviceLinked(new BlockPos(99, 99, 99)));
  }

  // endregion

  // region: unlinkDevice across circuits

  @Test
  void unlinkDeviceAcrossCircuits() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(5, 5, 5);
    c1.linkLeftSignal(pos);
    circuits.addCircuit(c1);

    assertTrue(circuits.unlinkDevice(pos));
    assertFalse(circuits.isDeviceLinked(pos));
  }

  @Test
  void unlinkDeviceNotLinked() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    assertFalse(circuits.unlinkDevice(new BlockPos(99, 99, 99)));
  }

  // endregion

  // region: forAllSignals across circuits

  @Test
  void forAllSignalsAcrossCircuits() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.linkThroughSignal(new BlockPos(1, 0, 0));

    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.linkLeftSignal(new BlockPos(2, 0, 0));

    circuits.addCircuit(c1);
    circuits.addCircuit(c2);

    List<BlockPos> visited = new ArrayList<>();
    circuits.forAllSignals(visited::add);

    assertEquals(2, visited.size());
    assertTrue(visited.contains(new BlockPos(1, 0, 0)));
    assertTrue(visited.contains(new BlockPos(2, 0, 0)));
  }

  // endregion

  // region: equals and hashCode

  @Test
  void equalsAndHashCodeConsistent() {
    TrafficSignalControllerCircuits c1 = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuits c2 = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    circuit.linkThroughSignal(new BlockPos(1, 2, 3));

    // Create identical circuit for c2
    TrafficSignalControllerCircuit circuit2 = new TrafficSignalControllerCircuit();
    circuit2.linkThroughSignal(new BlockPos(1, 2, 3));

    c1.addCircuit(circuit);
    c2.addCircuit(circuit2);

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  void notEqualsDifferentCircuits() {
    TrafficSignalControllerCircuits c1 = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuits c2 = new TrafficSignalControllerCircuits();

    TrafficSignalControllerCircuit circuit1 = new TrafficSignalControllerCircuit();
    circuit1.linkThroughSignal(new BlockPos(1, 2, 3));

    TrafficSignalControllerCircuit circuit2 = new TrafficSignalControllerCircuit();
    circuit2.linkThroughSignal(new BlockPos(4, 5, 6));

    c1.addCircuit(circuit1);
    c2.addCircuit(circuit2);

    assertNotEquals(c1, c2);
  }

  // endregion
}
