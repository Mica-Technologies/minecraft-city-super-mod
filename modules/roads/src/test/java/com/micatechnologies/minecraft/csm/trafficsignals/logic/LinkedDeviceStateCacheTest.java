package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rule {@link LinkedDeviceStateCache} reads linked devices by: fresh while their chunk is
 * loaded, remembered while it is not, and read once when never seen.
 */
class LinkedDeviceStateCacheTest {

  private static final BlockPos POS = new BlockPos(400, 5, 0);

  @Test
  @DisplayName("a loaded position is read every time, so a change shows at once")
  void loadedReadsFresh() {
    Map<BlockPos, Object> table = new HashMap<>();
    AtomicInteger reads = new AtomicInteger();
    assertEquals("north", LinkedDeviceStateCache.lookup(table, POS, true, () -> {
      reads.incrementAndGet();
      return "north";
    }));
    assertEquals("east", LinkedDeviceStateCache.lookup(table, POS, true, () -> {
      reads.incrementAndGet();
      return "east";
    }));
    assertEquals(2, reads.get());
  }

  @Test
  @DisplayName("an unloaded position answers from memory without reading (no chunk load)")
  void unloadedUsesMemory() {
    Map<BlockPos, Object> table = new HashMap<>();
    LinkedDeviceStateCache.lookup(table, POS, true, () -> "east");
    AtomicInteger reads = new AtomicInteger();
    for (int i = 0; i < 20; i++) {
      assertEquals("east", LinkedDeviceStateCache.lookup(table, POS, false, () -> {
        reads.incrementAndGet();
        return "loaded the chunk";
      }));
    }
    assertEquals(0, reads.get());
  }

  @Test
  @DisplayName("a position never seen is read once, then remembered")
  void neverSeenReadsOnce() {
    Map<BlockPos, Object> table = new HashMap<>();
    AtomicInteger reads = new AtomicInteger();
    for (int i = 0; i < 5; i++) {
      assertEquals("south", LinkedDeviceStateCache.lookup(table, POS, false, () -> {
        reads.incrementAndGet();
        return "south";
      }));
    }
    assertEquals(1, reads.get());
  }

  @Test
  @DisplayName("a null result is remembered too, not re-read")
  void rememberedNull() {
    Map<BlockPos, Object> table = new HashMap<>();
    AtomicInteger reads = new AtomicInteger();
    for (int i = 0; i < 3; i++) {
      assertNull(LinkedDeviceStateCache.lookup(table, POS, false, () -> {
        reads.incrementAndGet();
        return null;
      }));
    }
    assertEquals(1, reads.get());
  }

  @Test
  @DisplayName("a mutable position is stored as an immutable key")
  void mutableKey() {
    Map<BlockPos, Object> table = new HashMap<>();
    BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(1, 2, 3);
    LinkedDeviceStateCache.lookup(table, m, true, () -> "west");
    m.setPos(9, 9, 9);
    assertEquals("west", table.get(new BlockPos(1, 2, 3)));
  }
}
