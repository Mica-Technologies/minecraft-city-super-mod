package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

/**
 * A railing cell's collision: both sides of a flight one block wide leave a player room to climb
 * it (#368), and each edge still stops a player walking through it.
 */
class RailingCollisionTest {

  /** A player's width, as the game holds it (a float). */
  private static final double PLAYER = 0.6f;
  /** The lane two opposite railings must leave: the player, with room to walk in off centre. */
  private static final double LANE = 0.62;
  private static final double EPS = 1e-9;

  @Test
  void bothSidesOfANorthSouthFlightLeaveALane() {
    for (BlockStairRailing.Kind kind : BlockStairRailing.Kind.values()) {
      List<AxisAlignedBB> boxes = RailingCollision.boxes(kind, true, false, true, false);
      assertEquals(2, boxes.size());
      AxisAlignedBB north = boxes.get(0);
      AxisAlignedBB south = boxes.get(1);
      double lane = south.minZ - north.maxZ;
      assertTrue(lane >= LANE && lane > PLAYER, kind + ": lane " + lane);
    }
  }

  @Test
  void bothSidesOfAnEastWestFlightLeaveALane() {
    for (BlockStairRailing.Kind kind : BlockStairRailing.Kind.values()) {
      List<AxisAlignedBB> boxes = RailingCollision.boxes(kind, false, true, false, true);
      assertEquals(2, boxes.size());
      AxisAlignedBB east = boxes.get(0);
      AxisAlignedBB west = boxes.get(1);
      double lane = east.minX - west.maxX;
      assertTrue(lane >= LANE && lane > PLAYER, kind + ": lane " + lane);
    }
  }

  @Test
  void eachEdgeHugsItsOwnSideAndSpansTheRun() {
    for (BlockStairRailing.Kind kind : BlockStairRailing.Kind.values()) {
      AxisAlignedBB n = RailingCollision.boxes(kind, true, false, false, false).get(0);
      AxisAlignedBB e = RailingCollision.boxes(kind, false, true, false, false).get(0);
      AxisAlignedBB s = RailingCollision.boxes(kind, false, false, true, false).get(0);
      AxisAlignedBB w = RailingCollision.boxes(kind, false, false, false, true).get(0);

      // across the run: against its own edge, and with some depth so it blocks
      assertEquals(0, n.minZ, EPS, kind + " north");
      assertEquals(1, s.maxZ, EPS, kind + " south");
      assertEquals(1, e.maxX, EPS, kind + " east");
      assertEquals(0, w.minX, EPS, kind + " west");
      for (AxisAlignedBB b : new AxisAlignedBB[]{n, s}) {
        assertTrue(b.maxZ - b.minZ > 0.1, kind + " depth " + b);
        assertEquals(0, b.minX, EPS);   // the whole length of the run
        assertEquals(1, b.maxX, EPS);
      }
      for (AxisAlignedBB b : new AxisAlignedBB[]{e, w}) {
        assertTrue(b.maxX - b.minX > 0.1, kind + " depth " + b);
        assertEquals(0, b.minZ, EPS);
        assertEquals(1, b.maxZ, EPS);
      }
    }
  }

  @Test
  void aGuardStaysAsTallAsAFence() {
    for (BlockStairRailing.Kind kind : new BlockStairRailing.Kind[]{BlockStairRailing.Kind.GUARD,
        BlockStairRailing.Kind.GLASS}) {
      for (AxisAlignedBB b : RailingCollision.boxes(kind, true, true, true, true)) {
        assertEquals(0, b.minY, EPS);
        assertEquals(1.5, b.maxY, EPS);
      }
    }
  }

  @Test
  void aCellWithNoEdgeHasNoCollision() {
    assertTrue(RailingCollision.boxes(BlockStairRailing.Kind.GUARD, false, false, false, false)
        .isEmpty());
  }
}
