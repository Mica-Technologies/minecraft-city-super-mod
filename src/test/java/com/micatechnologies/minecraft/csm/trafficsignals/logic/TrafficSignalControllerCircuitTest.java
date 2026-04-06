package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalControllerCircuit} NBT serialization, device linking,
 * forAllSignals iteration, and signal list getters.
 */
class TrafficSignalControllerCircuitTest {

  // region: NBT round-trip with populated lists

  @Test
  void populatedCircuitNbtRoundTrip() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

    BlockPos through1 = new BlockPos(1, 2, 3);
    BlockPos through2 = new BlockPos(4, 5, 6);
    BlockPos left1 = new BlockPos(10, 20, 30);
    BlockPos right1 = new BlockPos(11, 21, 31);
    BlockPos flashLeft1 = new BlockPos(12, 22, 32);
    BlockPos flashRight1 = new BlockPos(13, 23, 33);
    BlockPos ped1 = new BlockPos(14, 24, 34);
    BlockPos pedBeacon1 = new BlockPos(15, 25, 35);
    BlockPos pedAccessory1 = new BlockPos(16, 26, 36);
    BlockPos protected1 = new BlockPos(17, 27, 37);
    BlockPos beacon1 = new BlockPos(18, 28, 38);
    BlockPos sensor1 = new BlockPos(19, 29, 39);

    circuit.linkThroughSignal(through1);
    circuit.linkThroughSignal(through2);
    circuit.linkLeftSignal(left1);
    circuit.linkRightSignal(right1);
    circuit.linkFlashingLeftSignal(flashLeft1);
    circuit.linkFlashingRightSignal(flashRight1);
    circuit.linkPedestrianSignal(ped1);
    circuit.linkPedestrianBeaconSignal(pedBeacon1);
    circuit.linkPedestrianAccessorySignal(pedAccessory1);
    circuit.linkProtectedSignal(protected1);
    circuit.linkBeaconSignal(beacon1);
    circuit.linkSensor(sensor1);

    NBTTagCompound nbt = circuit.toNBT();
    TrafficSignalControllerCircuit deserialized = TrafficSignalControllerCircuit.fromNBT(nbt);

    assertEquals(circuit.getThroughSignals(), deserialized.getThroughSignals());
    assertEquals(circuit.getLeftSignals(), deserialized.getLeftSignals());
    assertEquals(circuit.getRightSignals(), deserialized.getRightSignals());
    assertEquals(circuit.getFlashingLeftSignals(), deserialized.getFlashingLeftSignals());
    assertEquals(circuit.getFlashingRightSignals(), deserialized.getFlashingRightSignals());
    assertEquals(circuit.getPedestrianSignals(), deserialized.getPedestrianSignals());
    assertEquals(circuit.getPedestrianBeaconSignals(), deserialized.getPedestrianBeaconSignals());
    assertEquals(circuit.getPedestrianAccessorySignals(),
        deserialized.getPedestrianAccessorySignals());
    assertEquals(circuit.getProtectedSignals(), deserialized.getProtectedSignals());
    assertEquals(circuit.getBeaconSignals(), deserialized.getBeaconSignals());
    assertEquals(circuit.getSensors(), deserialized.getSensors());
  }

  // endregion

  // region: NBT round-trip with empty lists

  @Test
  void emptyCircuitNbtRoundTrip() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

    NBTTagCompound nbt = circuit.toNBT();
    TrafficSignalControllerCircuit deserialized = TrafficSignalControllerCircuit.fromNBT(nbt);

    assertTrue(deserialized.getThroughSignals().isEmpty());
    assertTrue(deserialized.getLeftSignals().isEmpty());
    assertTrue(deserialized.getRightSignals().isEmpty());
    assertTrue(deserialized.getFlashingLeftSignals().isEmpty());
    assertTrue(deserialized.getFlashingRightSignals().isEmpty());
    assertTrue(deserialized.getPedestrianSignals().isEmpty());
    assertTrue(deserialized.getPedestrianBeaconSignals().isEmpty());
    assertTrue(deserialized.getPedestrianAccessorySignals().isEmpty());
    assertTrue(deserialized.getProtectedSignals().isEmpty());
    assertTrue(deserialized.getBeaconSignals().isEmpty());
    assertTrue(deserialized.getSensors().isEmpty());
    assertEquals(0, deserialized.getSize());
  }

  // endregion

  // region: fromNBT null throws

  @Test
  void fromNbtNullThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TrafficSignalControllerCircuit.fromNBT(null));
  }

  // endregion

  // region: isDeviceLinked

  @Test
  void isDeviceLinkedThroughSignal() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(5, 10, 15);
    circuit.linkThroughSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedLeftSignal() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(6, 11, 16);
    circuit.linkLeftSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedRightSignal() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(7, 12, 17);
    circuit.linkRightSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedFlashingLeft() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(8, 13, 18);
    circuit.linkFlashingLeftSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedFlashingRight() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(8, 13, 18);
    circuit.linkFlashingRightSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedPedestrian() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(9, 14, 19);
    circuit.linkPedestrianSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedPedestrianBeacon() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(9, 14, 19);
    circuit.linkPedestrianBeaconSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedPedestrianAccessory() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(9, 14, 19);
    circuit.linkPedestrianAccessorySignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedProtected() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(9, 14, 19);
    circuit.linkProtectedSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedBeacon() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(9, 14, 19);
    circuit.linkBeaconSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedSensor() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(20, 30, 40);
    circuit.linkSensor(pos);
    assertTrue(circuit.isDeviceLinked(pos));
  }

  @Test
  void isDeviceLinkedUnknownReturnsFalse() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    circuit.linkThroughSignal(new BlockPos(1, 2, 3));
    assertFalse(circuit.isDeviceLinked(new BlockPos(99, 99, 99)));
  }

  // endregion

  // region: forAllSignals

  @Test
  void forAllSignalsVisitsEveryPosition() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

    BlockPos p1 = new BlockPos(1, 1, 1);
    BlockPos p2 = new BlockPos(2, 2, 2);
    BlockPos p3 = new BlockPos(3, 3, 3);
    BlockPos p4 = new BlockPos(4, 4, 4);
    BlockPos p5 = new BlockPos(5, 5, 5);
    BlockPos p6 = new BlockPos(6, 6, 6);
    BlockPos p7 = new BlockPos(7, 7, 7);
    BlockPos p8 = new BlockPos(8, 8, 8);
    BlockPos p9 = new BlockPos(9, 9, 9);
    BlockPos p10 = new BlockPos(10, 10, 10);

    circuit.linkThroughSignal(p1);
    circuit.linkLeftSignal(p2);
    circuit.linkRightSignal(p3);
    circuit.linkFlashingLeftSignal(p4);
    circuit.linkFlashingRightSignal(p5);
    circuit.linkPedestrianSignal(p6);
    circuit.linkPedestrianBeaconSignal(p7);
    circuit.linkPedestrianAccessorySignal(p8);
    circuit.linkProtectedSignal(p9);
    circuit.linkBeaconSignal(p10);

    List<BlockPos> visited = new ArrayList<>();
    circuit.forAllSignals(visited::add);

    // forAllSignals should NOT include sensors
    assertEquals(10, visited.size());
    assertTrue(visited.contains(p1));
    assertTrue(visited.contains(p2));
    assertTrue(visited.contains(p3));
    assertTrue(visited.contains(p4));
    assertTrue(visited.contains(p5));
    assertTrue(visited.contains(p6));
    assertTrue(visited.contains(p7));
    assertTrue(visited.contains(p8));
    assertTrue(visited.contains(p9));
    assertTrue(visited.contains(p10));
  }

  @Test
  void forAllSignalsDoesNotIncludeSensors() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    circuit.linkSensor(new BlockPos(50, 50, 50));
    circuit.linkThroughSignal(new BlockPos(1, 1, 1));

    List<BlockPos> visited = new ArrayList<>();
    circuit.forAllSignals(visited::add);

    assertEquals(1, visited.size());
    assertEquals(new BlockPos(1, 1, 1), visited.get(0));
  }

  // endregion

  // region: Signal list getters

  @Test
  void signalListGettersReturnCorrectLists() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

    BlockPos thr = new BlockPos(1, 0, 0);
    BlockPos left = new BlockPos(2, 0, 0);
    BlockPos right = new BlockPos(3, 0, 0);
    BlockPos fLeft = new BlockPos(4, 0, 0);
    BlockPos fRight = new BlockPos(5, 0, 0);
    BlockPos ped = new BlockPos(6, 0, 0);
    BlockPos pedB = new BlockPos(7, 0, 0);
    BlockPos pedA = new BlockPos(8, 0, 0);
    BlockPos prot = new BlockPos(9, 0, 0);
    BlockPos beac = new BlockPos(10, 0, 0);
    BlockPos sens = new BlockPos(11, 0, 0);

    circuit.linkThroughSignal(thr);
    circuit.linkLeftSignal(left);
    circuit.linkRightSignal(right);
    circuit.linkFlashingLeftSignal(fLeft);
    circuit.linkFlashingRightSignal(fRight);
    circuit.linkPedestrianSignal(ped);
    circuit.linkPedestrianBeaconSignal(pedB);
    circuit.linkPedestrianAccessorySignal(pedA);
    circuit.linkProtectedSignal(prot);
    circuit.linkBeaconSignal(beac);
    circuit.linkSensor(sens);

    assertEquals(Collections.singletonList(thr), circuit.getThroughSignals());
    assertEquals(Collections.singletonList(left), circuit.getLeftSignals());
    assertEquals(Collections.singletonList(right), circuit.getRightSignals());
    assertEquals(Collections.singletonList(fLeft), circuit.getFlashingLeftSignals());
    assertEquals(Collections.singletonList(fRight), circuit.getFlashingRightSignals());
    assertEquals(Collections.singletonList(ped), circuit.getPedestrianSignals());
    assertEquals(Collections.singletonList(pedB), circuit.getPedestrianBeaconSignals());
    assertEquals(Collections.singletonList(pedA), circuit.getPedestrianAccessorySignals());
    assertEquals(Collections.singletonList(prot), circuit.getProtectedSignals());
    assertEquals(Collections.singletonList(beac), circuit.getBeaconSignals());
    assertEquals(Collections.singletonList(sens), circuit.getSensors());
  }

  // endregion

  // region: Link bulk lists

  @Test
  void linkBulkSignalLists() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

    List<BlockPos> positions = Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));

    circuit.linkThroughSignals(positions);
    circuit.linkLeftSignals(positions);
    circuit.linkRightSignals(positions);
    circuit.linkFlashingLeftSignals(positions);
    circuit.linkFlashingRightSignals(positions);
    circuit.linkPedestrianSignals(positions);
    circuit.linkPedestrianBeaconSignals(positions);
    circuit.linkPedestrianAccessorySignals(positions);
    circuit.linkProtectedSignals(positions);
    circuit.linkSensors(positions);

    assertEquals(2, circuit.getThroughSignals().size());
    assertEquals(2, circuit.getLeftSignals().size());
    assertEquals(2, circuit.getRightSignals().size());
    assertEquals(2, circuit.getFlashingLeftSignals().size());
    assertEquals(2, circuit.getFlashingRightSignals().size());
    assertEquals(2, circuit.getPedestrianSignals().size());
    assertEquals(2, circuit.getPedestrianBeaconSignals().size());
    assertEquals(2, circuit.getPedestrianAccessorySignals().size());
    assertEquals(2, circuit.getProtectedSignals().size());
    assertEquals(2, circuit.getSensors().size());
  }

  // endregion

  // region: unlinkDevice

  @Test
  void unlinkDeviceRemovesFromCorrectList() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    BlockPos pos = new BlockPos(42, 42, 42);
    circuit.linkThroughSignal(pos);
    assertTrue(circuit.isDeviceLinked(pos));

    assertTrue(circuit.unlinkDevice(pos));
    assertFalse(circuit.isDeviceLinked(pos));
  }

  @Test
  void unlinkDeviceReturnsFalseForUnknown() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    assertFalse(circuit.unlinkDevice(new BlockPos(99, 99, 99)));
  }

  @Test
  void unlinkDeviceNullReturnsFalse() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    assertFalse(circuit.unlinkDevice(null));
  }

  // endregion

  // region: getSize

  @Test
  void getSizeCountsAllDevices() {
    TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
    circuit.linkThroughSignal(new BlockPos(1, 0, 0));
    circuit.linkLeftSignal(new BlockPos(2, 0, 0));
    circuit.linkSensor(new BlockPos(3, 0, 0));
    assertEquals(3, circuit.getSize());
  }

  // endregion

  // region: equals and hashCode

  @Test
  void equalsAndHashCodeConsistent() {
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();

    BlockPos pos = new BlockPos(1, 2, 3);
    c1.linkThroughSignal(pos);
    c2.linkThroughSignal(pos);

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  void notEqualsDifferentSignals() {
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();

    c1.linkThroughSignal(new BlockPos(1, 2, 3));
    c2.linkThroughSignal(new BlockPos(4, 5, 6));

    assertNotEquals(c1, c2);
  }

  // endregion
}
