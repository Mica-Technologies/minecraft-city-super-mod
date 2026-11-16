package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The pole cabinet's walk down its pole to the foot, where its power comes in (#360).
 */
class PoleControllerPowerTest {

  private static Set<BlockPos> column(int x, int z, int y0, int y1) {
    Set<BlockPos> poles = new HashSet<>();
    for (int y = y0; y <= y1; y++) {
      poles.add(new BlockPos(x, y, z));
    }
    return poles;
  }

  @Test
  void findsTheFootOfThePoleBehindTheCabinet() {
    Set<BlockPos> poles = column(10, 20, 4, 10);
    assertEquals(new BlockPos(10, 4, 20),
        BlockTrafficSignalControllerPole.poleFoot(new BlockPos(10, 5, 20), poles::contains, 32));
  }

  @Test
  void startingAtTheFootIsTheFoot() {
    Set<BlockPos> poles = column(0, 0, 4, 10);
    assertEquals(new BlockPos(0, 4, 0),
        BlockTrafficSignalControllerPole.poleFoot(new BlockPos(0, 4, 0), poles::contains, 32));
  }

  @Test
  void noPoleBehindMeansNoFoot() {
    assertNull(BlockTrafficSignalControllerPole.poleFoot(new BlockPos(0, 5, 0),
        p -> false, 32));
  }

  @Test
  void aGapEndsTheColumn() {
    Set<BlockPos> poles = column(0, 0, 8, 12);
    poles.addAll(column(0, 0, 2, 6));          // a second pole below a one-block gap
    assertEquals(new BlockPos(0, 8, 0),
        BlockTrafficSignalControllerPole.poleFoot(new BlockPos(0, 10, 0), poles::contains, 32));
  }

  @Test
  void theWalkIsBounded() {
    Set<BlockPos> poles = column(0, 0, 2, 200);
    assertEquals(new BlockPos(0, 168, 0),
        BlockTrafficSignalControllerPole.poleFoot(new BlockPos(0, 200, 0), poles::contains, 32));
  }

  @Test
  void theWalkStopsAboveTheWorldFloor() {
    Set<BlockPos> poles = column(0, 0, 0, 5);
    assertEquals(new BlockPos(0, 1, 0),
        BlockTrafficSignalControllerPole.poleFoot(new BlockPos(0, 5, 0), poles::contains, 32));
  }
}
