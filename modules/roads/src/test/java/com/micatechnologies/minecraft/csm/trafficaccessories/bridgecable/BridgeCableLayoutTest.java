package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The layout against a made-up world laid out like the two test bridges: the self-anchored
 * suspension's main span over a deck at y 84, and a cable-stayed pylon's stays.
 */
class BridgeCableLayoutTest {

  /** A world of anchors and solid blocks given by position; everything else is air. */
  private static final class FakeWorld implements BridgeCableLayout.WorldView {

    final Map<BlockPos, BridgeCableLayout.AnchorEnd> anchors = new HashMap<>();
    final Map<BlockPos, Double> solids = new HashMap<>();

    void anchor(BlockPos pos, BridgeCableAnchorKind kind, EnumFacing facing) {
      anchors.put(pos, new BridgeCableLayout.AnchorEnd(kind, facing));
    }

    /** A deck along z at one x, its top at {@code top}. */
    void deck(int x, int y, int z0, int z1) {
      for (int z = z0; z <= z1; z++) {
        solids.put(new BlockPos(x, y, z), y + 1.0);
      }
    }

    @Nullable
    @Override
    public BridgeCableLayout.AnchorEnd anchorAt(BlockPos pos) {
      return anchors.get(pos);
    }

    @Override
    public double solidTop(BlockPos pos) {
      final Double top = solids.get(pos);
      return top == null ? Double.NaN : top;
    }
  }

  private static final BlockPos SADDLE_A = new BlockPos(-1320, 111, -2380);
  private static final BlockPos SADDLE_B = new BlockPos(-1320, 111, -2300);

  private static FakeWorld suspension() {
    final FakeWorld world = new FakeWorld();
    world.anchor(SADDLE_A, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    world.anchor(SADDLE_B, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    world.deck(-1320, 83, -2400, -2270);
    return world;
  }

  private static BridgeCableLink mainWithHangers() {
    return new BridgeCableLink(0, 0, 80, BridgeCableStyle.MAIN, 8, "international_orange", 6.0,
        false, null, null, 5, 2, null);
  }

  @Test
  void aMainCableDropsAHangerEveryFiveBlocksToTheDeck() {
    final BridgeCableLayout layout = BridgeCableLayout.build(SADDLE_A,
        Collections.singletonList(mainWithHangers()), suspension());
    final List<BridgeCableLayout.Hanger> hangers = layout.getHangers();
    assertEquals(15, hangers.size()); // 5 .. 75 along an 80-block span
    for (BridgeCableLayout.Hanger hanger : hangers) {
      assertEquals(84.0, hanger.bottom.y, 1.0e-9);
      assertTrue(hanger.top.y > hanger.bottom.y);
    }
    // the middle hanger hangs from the cable's low point, six blocks under the saddles
    final BridgeCableLayout.Hanger middle = hangers.get(7);
    assertEquals(-2340.0 + 0.5, middle.top.z, 1.0e-9);
    assertEquals(111 + 13.0 / 16.0 - 6.0, middle.top.y, 1.0e-9);
  }

  @Test
  void aHangerEndsOnADeckAnchorWhereThereIsOne() {
    final FakeWorld world = suspension();
    final BlockPos deckAnchor = new BlockPos(-1320, 84, -2340);
    world.anchor(deckAnchor, BridgeCableAnchorKind.DECK, EnumFacing.NORTH);
    final BridgeCableLayout layout = BridgeCableLayout.build(SADDLE_A,
        Collections.singletonList(mainWithHangers()), world);
    final BridgeCableLayout.Hanger middle = layout.getHangers().get(7);
    assertTrue(middle.onDeckAnchor);
    assertEquals(84 + 6.0 / 16.0, middle.bottom.y, 1.0e-9);
  }

  @Test
  void noHangerWhereThereIsNothingBelow() {
    final FakeWorld world = new FakeWorld();
    world.anchor(SADDLE_A, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    world.anchor(SADDLE_B, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    world.deck(-1320, 83, -2350, -2330); // only the middle twenty blocks are built
    final BridgeCableLayout layout = BridgeCableLayout.build(SADDLE_A,
        Collections.singletonList(mainWithHangers()), world);
    assertEquals(5, layout.getHangers().size()); // -2350, -2345, -2340, -2335, -2330
  }

  @Test
  void theKeyChangesWhenTheDeckIsBuiltUnderTheCable() {
    final FakeWorld world = new FakeWorld();
    world.anchor(SADDLE_A, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    world.anchor(SADDLE_B, BridgeCableAnchorKind.SADDLE, EnumFacing.SOUTH);
    final List<BridgeCableLink> links = Collections.singletonList(mainWithHangers());
    final long before = BridgeCableLayout.build(SADDLE_A, links, world).getKey();
    assertEquals(before, BridgeCableLayout.build(SADDLE_A, links, world).getKey());
    world.deck(-1320, 83, -2400, -2270);
    assertNotEquals(before, BridgeCableLayout.build(SADDLE_A, links, world).getKey());
  }

  @Test
  void aStayWearsAGuidePipeAtTheSocketAndAtTheDeckAnchor() {
    final FakeWorld world = new FakeWorld();
    final BlockPos socket = new BlockPos(-2535, 117, -2340);
    final BlockPos deck = new BlockPos(-2532, 84, -2387);
    world.anchor(socket, BridgeCableAnchorKind.SOCKET, EnumFacing.NORTH);
    world.anchor(deck, BridgeCableAnchorKind.DECK, EnumFacing.NORTH);
    final BridgeCableLayout layout = BridgeCableLayout.build(socket,
        Collections.singletonList(BridgeCableLink.of(socket, deck, BridgeCableStyle.STAY)), world);
    // the stay, then a pipe at each end
    assertEquals(3, layout.getTubes().size());
    final BridgeCableLayout.Tube stay = layout.getTubes().get(0);
    assertEquals(BridgeCableAnchorKind.SOCKET.attachPoint(socket, EnumFacing.NORTH),
        stay.points.get(0));
    assertEquals(BridgeCableAnchorKind.DECK.attachPoint(deck, EnumFacing.NORTH),
        stay.points.get(stay.points.size() - 1));
    assertTrue(layout.getTubes().get(1).radius > stay.radius);
  }

  @Test
  void theBoundsCoverTheWholeCable() {
    final FakeWorld world = new FakeWorld();
    final BlockPos socket = new BlockPos(-2535, 117, -2340);
    final BlockPos far = new BlockPos(-2478, 84, -2290);
    world.anchor(socket, BridgeCableAnchorKind.SOCKET, EnumFacing.SOUTH);
    final BridgeCableLayout layout = BridgeCableLayout.build(socket,
        Collections.singletonList(BridgeCableLink.of(socket, far, BridgeCableStyle.STAY)), world);
    final AxisAlignedBB bounds = layout.getBounds();
    assertTrue(bounds.contains(new net.minecraft.util.math.Vec3d(-2534.5, 117.5, -2339.5)));
    assertTrue(bounds.contains(new net.minecraft.util.math.Vec3d(-2477.5, 84.5, -2289.5)));
  }

  @Test
  void anLedStripGlowsAndAStripeIsCarried() {
    final FakeWorld world = new FakeWorld();
    final BlockPos socket = new BlockPos(0, 100, 0);
    world.anchor(socket, BridgeCableAnchorKind.CLEVIS, EnumFacing.EAST);
    final BridgeCableLink link = new BridgeCableLink(30, -16, 0, BridgeCableStyle.STAY, 4,
        "white", Double.NaN, false, "blue", "blue", 0, 0, null);
    final BridgeCableLayout layout =
        BridgeCableLayout.build(socket, Collections.singletonList(link), world);
    final BridgeCableLayout.Tube cable = layout.getTubes().get(0);
    assertEquals(BridgeCableColour.rgb("blue"), cable.stripeRgb);
    assertTrue(layout.getTubes().stream().anyMatch(tube -> tube.glow));
  }

  @Test
  void prismSidesFollowThickness() {
    assertEquals(6, BridgeCableLayout.sidesFor(2 / 32.0));
    assertEquals(8, BridgeCableLayout.sidesFor(4 / 32.0));
    assertEquals(12, BridgeCableLayout.sidesFor(8 / 32.0));
  }
}
