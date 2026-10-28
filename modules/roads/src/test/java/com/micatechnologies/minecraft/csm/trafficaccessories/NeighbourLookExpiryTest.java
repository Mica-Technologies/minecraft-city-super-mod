package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

/**
 * A truss sign looks for the truss behind it at most once a second, and again at once after a
 * sync announcing a change behind it, a turn, or a clock that went backwards (a new world).
 */
class NeighbourLookExpiryTest {

  private static final long OLD_WORLD = 3_803_291_727L;

  @Test
  void aFreshSignLooks() {
    assertTrue(new NeighbourLookExpiry(20).isStale(OLD_WORLD, EnumFacing.NORTH));
  }

  @Test
  void aLookIsTrustedForTwentyTicks() {
    NeighbourLookExpiry look = new NeighbourLookExpiry(
        TileEntityDynamicGuideSign.TRUSS_RECHECK_TICKS);
    look.looked(OLD_WORLD, EnumFacing.NORTH);
    for (long t = 0; t < 20; t++) {
      assertFalse(look.isStale(OLD_WORLD + t, EnumFacing.NORTH), "tick " + t);
    }
    assertTrue(look.isStale(OLD_WORLD + 20, EnumFacing.NORTH));
  }

  @Test
  void aSyncDropsTheLook() {
    NeighbourLookExpiry look = new NeighbourLookExpiry(20);
    look.looked(OLD_WORLD, EnumFacing.EAST);
    look.invalidate();
    assertTrue(look.isStale(OLD_WORLD, EnumFacing.EAST));
  }

  @Test
  void aTurnOrABackwardClockLooksAgain() {
    NeighbourLookExpiry look = new NeighbourLookExpiry(20);
    look.looked(OLD_WORLD, EnumFacing.EAST);
    assertTrue(look.isStale(OLD_WORLD + 1, EnumFacing.WEST));
    assertTrue(look.isStale(OLD_WORLD - 1, EnumFacing.EAST));
  }

  @Test
  void theSignReadsTheWholeColumnItUsedTo() {
    // 8 above, its own level, 4 below: the 13 reads the renderer made every frame
    assertEquals(13, TileEntityDynamicGuideSign.TRUSS_LOOK_UP
        + TileEntityDynamicGuideSign.TRUSS_LOOK_DOWN + 1);
  }
}
