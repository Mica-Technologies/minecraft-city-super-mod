package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalControllerOverlaps} NBT serialization, overlap management,
 * and query methods.
 */
class TrafficSignalControllerOverlapsTest {

  // region: NBT round-trip

  @Test
  void populatedOverlapsNbtRoundTrip() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();

    BlockPos source1 = new BlockPos(10, 20, 30);
    BlockPos target1a = new BlockPos(40, 50, 60);
    BlockPos target1b = new BlockPos(70, 80, 90);
    BlockPos source2 = new BlockPos(100, 110, 120);
    BlockPos target2 = new BlockPos(130, 140, 150);

    overlaps.addOverlap(source1, target1a);
    overlaps.addOverlap(source1, target1b);
    overlaps.addOverlap(source2, target2);

    NBTTagCompound nbt = overlaps.toNBT();
    TrafficSignalControllerOverlaps deserialized = TrafficSignalControllerOverlaps.fromNBT(nbt);

    assertEquals(overlaps, deserialized);
    assertEquals(2, deserialized.getOverlapCount());
  }

  @Test
  void emptyOverlapsNbtRoundTrip() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();

    NBTTagCompound nbt = overlaps.toNBT();
    TrafficSignalControllerOverlaps deserialized = TrafficSignalControllerOverlaps.fromNBT(nbt);

    assertEquals(0, deserialized.getOverlapCount());
    assertEquals(overlaps, deserialized);
  }

  // endregion

  // region: fromNBT null throws

  @Test
  void fromNbtNullThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TrafficSignalControllerOverlaps.fromNBT(null));
  }

  // endregion

  // region: getOverlapsForSource

  @Test
  void getOverlapsForSourceKnown() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 2, 3);
    BlockPos target1 = new BlockPos(4, 5, 6);
    BlockPos target2 = new BlockPos(7, 8, 9);

    overlaps.addOverlap(source, target1);
    overlaps.addOverlap(source, target2);

    List<BlockPos> result = overlaps.getOverlapsForSource(source);
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(target1));
    assertTrue(result.contains(target2));
  }

  @Test
  void getOverlapsForSourceUnknownReturnsNull() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    overlaps.addOverlap(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));

    assertNull(overlaps.getOverlapsForSource(new BlockPos(99, 99, 99)));
  }

  // endregion

  // region: addOverlap

  @Test
  void addOverlapReturnsTrueForNew() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    assertTrue(overlaps.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
    assertEquals(1, overlaps.getOverlapCount());
  }

  @Test
  void addOverlapDuplicateReturnsFalse() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 0, 0);
    BlockPos target = new BlockPos(2, 0, 0);

    assertTrue(overlaps.addOverlap(source, target));
    assertFalse(overlaps.addOverlap(source, target));

    // Still only one target under that source
    assertEquals(1, overlaps.getOverlapsForSource(source).size());
  }

  @Test
  void addMultipleOverlapsPerSource() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 0, 0);

    overlaps.addOverlap(source, new BlockPos(2, 0, 0));
    overlaps.addOverlap(source, new BlockPos(3, 0, 0));
    overlaps.addOverlap(source, new BlockPos(4, 0, 0));

    assertEquals(3, overlaps.getOverlapsForSource(source).size());
    assertEquals(1, overlaps.getOverlapCount()); // 1 source entry
  }

  // endregion

  // region: removeOverlap

  @Test
  void removeOverlapExistingReturnsTrue() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 0, 0);
    BlockPos target = new BlockPos(2, 0, 0);
    overlaps.addOverlap(source, target);

    assertTrue(overlaps.removeOverlap(source, target));
    // Source entry should be removed since it has no more targets
    assertNull(overlaps.getOverlapsForSource(source));
    assertEquals(0, overlaps.getOverlapCount());
  }

  @Test
  void removeOverlapNonExistentReturnsFalse() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    assertFalse(overlaps.removeOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
  }

  @Test
  void removeOverlapLeavesOtherTargets() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 0, 0);
    BlockPos target1 = new BlockPos(2, 0, 0);
    BlockPos target2 = new BlockPos(3, 0, 0);

    overlaps.addOverlap(source, target1);
    overlaps.addOverlap(source, target2);

    assertTrue(overlaps.removeOverlap(source, target1));
    // Source entry still present with target2
    assertNotNull(overlaps.getOverlapsForSource(source));
    assertEquals(1, overlaps.getOverlapsForSource(source).size());
    assertTrue(overlaps.getOverlapsForSource(source).contains(target2));
  }

  // endregion

  // region: removeOverlaps (all for source)

  @Test
  void removeOverlapsForSource() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    BlockPos source = new BlockPos(1, 0, 0);
    overlaps.addOverlap(source, new BlockPos(2, 0, 0));
    overlaps.addOverlap(source, new BlockPos(3, 0, 0));

    assertTrue(overlaps.removeOverlaps(source));
    assertNull(overlaps.getOverlapsForSource(source));
    assertEquals(0, overlaps.getOverlapCount());
  }

  @Test
  void removeOverlapsForUnknownSourceReturnsFalse() {
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
    assertFalse(overlaps.removeOverlaps(new BlockPos(99, 99, 99)));
  }

  // endregion

  // region: equals and hashCode

  @Test
  void equalsAndHashCodeConsistent() {
    TrafficSignalControllerOverlaps o1 = new TrafficSignalControllerOverlaps();
    TrafficSignalControllerOverlaps o2 = new TrafficSignalControllerOverlaps();

    BlockPos s = new BlockPos(1, 2, 3);
    BlockPos t = new BlockPos(4, 5, 6);

    o1.addOverlap(s, t);
    o2.addOverlap(s, t);

    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());
  }

  // endregion
}
