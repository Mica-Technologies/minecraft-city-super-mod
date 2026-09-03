package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A span is stored redundantly on every block taking part in it, so the NBT round trip is what
 * keeps those copies identical across a save and reload. A packing bug here would not surface
 * until a world was reloaded, which is the worst time to find one.
 */
class SpanWireDefinitionTest {

  private static SpanWireDefinition sampleSpan() {
    return new SpanWireDefinition(
        new BlockPos(100, 70, 40),
        new BlockPos(120, 70, 40),
        Arrays.asList(new BlockPos(106, 69, 40), new BlockPos(110, 69, 40),
            new BlockPos(114, 69, 40)),
        SpanWireCatenary.DEFAULT_SLACK);
  }

  @Test
  void roundTripsThroughNbt() {
    final SpanWireDefinition original = sampleSpan();
    final SpanWireDefinition restored =
        SpanWireDefinition.readFromNBT(original.writeToNBT(new NBTTagCompound()));

    assertNotNull(restored);
    assertEquals(original, restored);
    assertEquals(original.getHangers(), restored.getHangers());
  }

  @Test
  void roundTripsWithNoHangers() {
    final SpanWireDefinition original = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(12, 70, 0), Collections.emptyList(), 1.01);
    final SpanWireDefinition restored =
        SpanWireDefinition.readFromNBT(original.writeToNBT(new NBTTagCompound()));

    assertNotNull(restored);
    assertEquals(original, restored);
    assertTrue(restored.getHangers().isEmpty());
  }

  /**
   * Hanger positions are packed into an int array as high/low word pairs, because 1.12 NBT has no
   * long array tag. Negative coordinates are exactly where that packing goes wrong if the low
   * word is sign extended, and half of any world has them.
   */
  @Test
  void roundTripsNegativeAndFarFlungCoordinates() {
    final List<BlockPos> hangers = Arrays.asList(
        new BlockPos(-1, -1, -1),
        new BlockPos(-2048, 5, -30000000),
        new BlockPos(29999999, 255, -7),
        new BlockPos(0, 0, 0));
    final SpanWireDefinition original = new SpanWireDefinition(
        new BlockPos(-5000, 70, -5000), new BlockPos(-4980, 70, -5000), hangers, 1.006);

    final SpanWireDefinition restored =
        SpanWireDefinition.readFromNBT(original.writeToNBT(new NBTTagCompound()));

    assertNotNull(restored);
    assertEquals(hangers, restored.getHangers());
    assertEquals(original.getAnchorA(), restored.getAnchorA());
    assertEquals(original.getAnchorB(), restored.getAnchorB());
  }

  @Test
  void emptyCompoundHoldsNoSpan() {
    assertNull(SpanWireDefinition.readFromNBT(new NBTTagCompound()));
  }

  @Test
  void clearingLeavesNothingBehind() {
    final NBTTagCompound compound = sampleSpan().writeToNBT(new NBTTagCompound());
    assertNotNull(SpanWireDefinition.readFromNBT(compound));

    SpanWireDefinition.clearNBT(compound);

    assertNull(SpanWireDefinition.readFromNBT(compound));
    assertTrue(compound.getKeySet().isEmpty(), "stale span keys left in NBT: " + compound);
  }

  @Test
  void attachmentsRunAnchorToAnchorThroughTheHangers() {
    final SpanWireDefinition span = sampleSpan();
    final List<BlockPos> attachments = span.getAttachments();

    assertEquals(5, attachments.size());
    assertEquals(span.getAnchorA(), attachments.get(0));
    assertEquals(span.getAnchorB(), attachments.get(attachments.size() - 1));
    assertEquals(span.getHangers(), attachments.subList(1, attachments.size() - 1));
  }

  @Test
  void everyAttachmentButTheLastOwnsASegment() {
    final SpanWireDefinition span = sampleSpan();
    final List<BlockPos> attachments = span.getAttachments();

    for (int i = 0; i < attachments.size() - 1; i++) {
      assertEquals(attachments.get(i + 1), span.nextAfter(attachments.get(i)));
    }
    assertNull(span.nextAfter(span.getAnchorB()), "the far end owns no segment");
    assertNull(span.nextAfter(new BlockPos(-999, -999, -999)), "a stranger owns no segment");
  }

  @Test
  void involvesFindsEveryParticipantAndNothingElse() {
    final SpanWireDefinition span = sampleSpan();

    assertTrue(span.involves(span.getAnchorA()));
    assertTrue(span.involves(span.getAnchorB()));
    assertTrue(span.involves(span.getHangers().get(1)));
    assertFalse(span.involves(new BlockPos(999, 70, 999)));

    assertTrue(span.isAnchor(span.getAnchorA()));
    assertFalse(span.isAnchor(span.getHangers().get(0)));
  }

  @Test
  void removingAHangerLeavesTheRestOfTheSpanIntact() {
    final SpanWireDefinition span = sampleSpan();
    final BlockPos removed = span.getHangers().get(1);
    final SpanWireDefinition reduced = span.withoutHanger(removed);

    assertFalse(reduced.involves(removed));
    assertEquals(2, reduced.getHangers().size());
    assertEquals(span.getAnchorA(), reduced.getAnchorA());
    assertEquals(span.getAnchorB(), reduced.getAnchorB());
    assertEquals(span.getSlack(), reduced.getSlack());
  }

  @Test
  void hangersListCannotBeMutatedThroughTheAccessor() {
    final SpanWireDefinition span = sampleSpan();
    try {
      span.getHangers().add(new BlockPos(1, 1, 1));
      throw new AssertionError("hanger list should not be modifiable");
    } catch (UnsupportedOperationException expected) {
      // A span is immutable; editing one means building a new definition.
    }
  }

  @Test
  void constructorCopiesTheHangerListItWasGiven() {
    final List<BlockPos> mutable = new ArrayList<>();
    mutable.add(new BlockPos(5, 70, 0));
    final SpanWireDefinition span =
        new SpanWireDefinition(new BlockPos(0, 70, 0), new BlockPos(10, 70, 0), mutable, 1.006);

    mutable.clear();

    assertEquals(1, span.getHangers().size(), "span should not share the caller's list");
  }

  @Test
  void cableMeetsItsAnchorsExactlyAndFallsAwayBetweenThem() {
    final SpanWireDefinition span = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0),
        Collections.singletonList(new BlockPos(10, 70, 0)), SpanWireCatenary.DEFAULT_SLACK);

    // The cable terminates on its anchors, so there is no hardware to draw at either end.
    assertEquals(0.0, span.cableDropAt(span.getAnchorA()), 1.0e-6);
    assertEquals(0.0, span.cableDropAt(span.getAnchorB()), 1.0e-6);

    // A mount left level with the anchors ends up ABOVE the cable, because the cable has sagged
    // away from the line joining them. Negative, and a placement error -- nothing hangs upward.
    final double midDrop = span.cableDropAt(new BlockPos(10, 70, 0));
    assertEquals(-span.solve().sag(), midDrop, 1.0e-6);
    assertTrue(midDrop < -0.9,
        "a mount level with its anchors should sit about a block above the cable, got " + midDrop);
  }

  @Test
  void steppingAMountDownIsWhatPutsItUnderTheCable() {
    final BlockPos anchorA = new BlockPos(0, 70, 0);
    final BlockPos anchorB = new BlockPos(20, 70, 0);
    final SpanWireDefinition level = new SpanWireDefinition(anchorA, anchorB,
        Collections.singletonList(new BlockPos(10, 70, 0)), SpanWireCatenary.DEFAULT_SLACK);
    final SpanWireDefinition stepped = new SpanWireDefinition(anchorA, anchorB,
        Collections.singletonList(new BlockPos(10, 68, 0)), SpanWireCatenary.DEFAULT_SLACK);

    final double levelDrop = level.cableDropAt(new BlockPos(10, 70, 0));
    final double steppedDrop = stepped.cableDropAt(new BlockPos(10, 68, 0));

    // Lowering a mount by two blocks buys exactly two blocks of clearance under the cable.
    assertEquals(levelDrop + 2.0, steppedDrop, 1.0e-9);
    // And it is the move that turns an unbuildable mount into a buildable one.
    assertTrue(levelDrop < 0.0, "level mount should be above the cable");
    assertTrue(steppedDrop > 0.0, "stepped-down mount should hang below the cable");
  }

  @Test
  void aMountUnderTheSaggingMiddleNeedsLessHardwareThanOneNearAnAnchor() {
    // Both mounts are a block below the anchors; the cable has fallen further at midspan, so the
    // midspan mount hangs closer to it. This is the whole reason the hardware length varies.
    final SpanWireDefinition span = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0),
        Arrays.asList(new BlockPos(2, 69, 0), new BlockPos(10, 69, 0)),
        SpanWireCatenary.DEFAULT_SLACK);

    final double nearAnchor = span.cableDropAt(new BlockPos(2, 69, 0));
    final double midspan = span.cableDropAt(new BlockPos(10, 69, 0));

    assertTrue(nearAnchor > 0.0, "mount near the anchor should still hang below the cable");
    assertTrue(midspan > 0.0, "midspan mount should hang below the cable");
    assertTrue(midspan < nearAnchor,
        "cable sags toward midspan, so the midspan drop should be shorter: " + midspan + " vs "
            + nearAnchor);
  }

  @Test
  void slackSurvivesTheRoundTripAndBadValuesFallBackToTheDefault() {
    final NBTTagCompound compound = sampleSpan().writeToNBT(new NBTTagCompound());
    assertEquals(SpanWireCatenary.DEFAULT_SLACK,
        SpanWireDefinition.readFromNBT(compound).getSlack(), 1.0e-9);

    // A cable shorter than the gap it crosses has no solution; fall back rather than persist it.
    compound.setDouble("swS", 0.5);
    assertEquals(SpanWireCatenary.DEFAULT_SLACK,
        SpanWireDefinition.readFromNBT(compound).getSlack(), 1.0e-9);
  }

  @Test
  void anOrdinarySpanHasNoTetherAndSaysSoInItsNbt() {
    final SpanWireDefinition span = sampleSpan();
    assertFalse(span.isBoxSpan());
    assertNull(span.solveTether());

    // The key is absent rather than false, so a span strung before tethers existed round-trips
    // byte for byte and a world that never builds one never grows the key.
    final NBTTagCompound compound = span.writeToNBT(new NBTTagCompound());
    assertFalse(compound.hasKey("swBox"), "an ordinary span should write no box span key");
    assertFalse(SpanWireDefinition.readFromNBT(compound).isBoxSpan());
  }

  @Test
  void boxSpanSurvivesTheRoundTrip() {
    final SpanWireDefinition original = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0),
        Collections.singletonList(new BlockPos(10, 69, 0)), SpanWireCatenary.DEFAULT_SLACK, true);

    final SpanWireDefinition restored =
        SpanWireDefinition.readFromNBT(original.writeToNBT(new NBTTagCompound()));

    assertNotNull(restored);
    assertTrue(restored.isBoxSpan());
    assertEquals(original, restored);
    // A box span and an otherwise identical plain span are different spans, so they must not
    // compare or hash equal -- the display list cache keys on the span's hash.
    final SpanWireDefinition plain = new SpanWireDefinition(original.getAnchorA(),
        original.getAnchorB(), original.getHangers(), original.getSlack(), false);
    assertNotEquals(original, plain);
  }

  @Test
  void removingAHangerKeepsTheSpanABoxSpan() {
    final BlockPos hanger = new BlockPos(10, 69, 0);
    final SpanWireDefinition span = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0),
        Arrays.asList(hanger, new BlockPos(14, 69, 0)), SpanWireCatenary.DEFAULT_SLACK, true);

    assertTrue(span.withoutHanger(hanger).isBoxSpan());
  }

  /**
   * The tether is what stops the heads turning, not what carries them, so it is strung hard --
   * a real one sags 75 to 85 percent less than the span wire above it. This pins that, because
   * the alternative is a tether that quietly drifts back toward the messenger's curve and turns
   * a box span into two parallel wires drawn twice.
   */
  @ParameterizedTest
  @ValueSource(ints = {8, 20, 40, 64})
  void tetherSagsFarLessThanTheMessengerAtEverySpanLength(int span) {
    final SpanWireDefinition boxSpan = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(span, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true);

    final double messengerSag = boxSpan.solve().sag();
    final SpanWireCatenary tether = boxSpan.solveTether();
    assertNotNull(tether);
    final double tetherSag = tether.sag();

    final double reduction = 1.0 - tetherSag / messengerSag;
    assertTrue(reduction > 0.75 && reduction < 0.85,
        "tether should sag 75-85% less than the messenger, got " + Math.round(reduction * 100)
            + "% at a " + span + " block span");
  }

  @Test
  void tetherHangsBelowTheMessengerForItsWholeLength() {
    final SpanWireDefinition boxSpan = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true);

    final SpanWireCatenary messenger = boxSpan.solve();
    final SpanWireCatenary tether = boxSpan.solveTether();

    // The tether is tighter, so the gap between the two is widest at the ends and closes toward
    // midspan. It must never close entirely, or the two would cross.
    for (int i = 0; i <= 20; i++) {
      final double t = i / 20.0;
      assertTrue(tether.heightAt(t) < messenger.heightAt(t),
          "tether crossed above the messenger at t=" + t);
    }
    assertTrue(tether.heightAt(0.5) < messenger.heightAt(0.5) - 1.0,
        "the two should still be well apart at midspan");
  }

  /**
   * The invariant a fixed drop got wrong. A hard-strung tether and a sagging messenger converge
   * toward midspan, so the gap at the anchors is not the gap that matters -- the smallest one is,
   * and it is what decides whether the tether passes under the signals or through their bottom
   * lamps. It has to hold at every span length, because the amount they converge by grows with
   * the span.
   */
  @ParameterizedTest
  @ValueSource(ints = {8, 20, 40, 64})
  void tetherKeepsItsMinimumClearanceUnderTheMessengerAtEverySpanLength(int span) {
    final SpanWireDefinition boxSpan = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(span, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true);

    final SpanWireCatenary messenger = boxSpan.solve();
    final SpanWireCatenary tether = boxSpan.solveTether();

    double smallest = Double.MAX_VALUE;
    for (int i = 0; i <= 200; i++) {
      final double t = i / 200.0;
      smallest = Math.min(smallest, messenger.heightAt(t) - tether.heightAt(t));
    }

    assertEquals(SpanWireDefinition.TETHER_MIN_CLEARANCE, smallest, 0.01,
        "closest approach over a " + span + " block span");
  }

  @Test
  void signalSideDefaultsToAutoAndCarriesNoOffsetUntilMeasured() {
    // Inverted deliberately. This used to pin CENTRED as the default, and centred was wrong: a
    // signal's body sits behind the middle of its block, so a span down the centre line puts the
    // messenger and the tether through the visors. The default is now AUTO, which runs the span
    // wherever its payloads actually are -- but the amount is measured when the span is strung,
    // so a span built in isolation like this one still has no displacement.
    final SpanWireDefinition span = sampleSpan();
    assertEquals(SpanWireSignalSide.AUTO, span.getSignalSide());
    assertEquals(0.0, span.getAutoOffset().x, 1.0e-12);
    assertEquals(0.0, span.getAutoOffset().z, 1.0e-12);
    assertEquals(0.0, span.spanOffset().x, 1.0e-12);
    assertEquals(0.0, span.spanOffset().z, 1.0e-12);
  }

  @Test
  void anAutomaticOffsetTranslatesTheWholeSpanAndSurvivesNbt() {
    // Inverted deliberately, and the inversion is the point. This used to assert that an offset
    // could only move a span *across* itself -- that it must never shift along its own run. That
    // held a span to its chord's normal, and a head is set back along the way it faces, which on
    // a diagonal is not the normal. The wire could then never sit over the housings and every tie
    // hung off it leaned to make up the difference.
    //
    // An automatic offset is now the payload's own displacement, applied whole. Moving the ends a
    // little along the span as well as across it is harmless: an anchor's shackle already takes up
    // the difference between its fixed eyebolt and where the wire lands.
    final Vec3d offset = new Vec3d(0.25, 0.0, -0.34375);
    final SpanWireDefinition span = sampleSpan().withAutoOffset(offset);
    assertEquals(0.25, span.spanOffset().x, 1.0e-12,
        "an automatic offset is applied whole, including along the span");
    assertEquals(-0.34375, span.spanOffset().z, 1.0e-12);

    final SpanWireDefinition read =
        SpanWireDefinition.readFromNBT(span.writeToNBT(new NBTTagCompound()));
    assertEquals(SpanWireSignalSide.AUTO, read.getSignalSide());
    assertEquals(0.25, read.getAutoOffset().x, 1.0e-12);
    assertEquals(-0.34375, read.getAutoOffset().z, 1.0e-12);
    assertEquals(-0.34375, read.spanOffset().z, 1.0e-12);
  }

  @Test
  void afixedSideStillOnlyMovesTheSpanAcrossItself() {
    // The manual sides are unchanged and still purely sideways: they are a builder saying "run the
    // wires over that way", with no payload geometry behind them to justify anything else.
    final SpanWireDefinition span = sampleSpan().withSignalSide(SpanWireSignalSide.LEFT);
    assertEquals(0.0, span.spanOffset().x, 1.0e-12, "a fixed side must not move a span along itself");
    assertEquals(-SpanWireSignalSide.OFFSET, span.spanOffset().z, 1.0e-12);
  }

  @Test
  void aTagWrittenBeforeAutomaticOffsetsExistedReadsBackAsCentred() {
    // Migration, and the reason the enum value was appended rather than inserted: a span saved
    // when centred was the default wrote no side key, and must not be silently re-interpreted as
    // following payloads it was never measured against.
    final NBTTagCompound compound = sampleSpan().writeToNBT(new NBTTagCompound());
    compound.removeTag("swSS");
    compound.removeTag("swAO");

    final SpanWireDefinition read = SpanWireDefinition.readFromNBT(compound);
    assertEquals(SpanWireSignalSide.CENTRED, read.getSignalSide());
    assertEquals(0.0, read.spanOffset().x, 1.0e-12);
    assertEquals(0.0, read.spanOffset().z, 1.0e-12);
  }

  @Test
  void signalSideRoundTripsAndOffsetsAcrossTheSpan() {
    // A span running due east: left of it is north, right of it is south.
    final SpanWireDefinition east = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true, SpanWireSignalSide.LEFT);

    final SpanWireDefinition restored =
        SpanWireDefinition.readFromNBT(east.writeToNBT(new NBTTagCompound()));
    assertNotNull(restored);
    assertEquals(SpanWireSignalSide.LEFT, restored.getSignalSide());
    assertEquals(east, restored);

    // The offset is across the span, never along it.
    assertEquals(0.0, east.spanOffset().x, 1.0e-9);
    assertEquals(-SpanWireSignalSide.OFFSET, east.spanOffset().z, 1.0e-9);
    assertEquals(EnumFacing.NORTH, east.getSignalSide().compassFor(east.horizontalDirection()));

    final SpanWireDefinition right = east.withSignalSide(SpanWireSignalSide.RIGHT);
    assertEquals(-east.spanOffset().z, right.spanOffset().z, 1.0e-9);
    assertEquals(EnumFacing.SOUTH, right.getSignalSide().compassFor(right.horizontalDirection()));
  }

  /**
   * The offset moves the <b>whole</b> span, messenger included.
   *
   * <p>This test used to assert the opposite — that the messenger stayed put because it is what
   * the heads hang from. That reasoning was wrong: leaving the messenger on the block centre line
   * while the tether moved just relocated the misalignment from the bottom of the assembly to the
   * top, and left the masts coming down off-centre into the housings.
   */
  @Test
  void signalSideShiftsTheWholeSpanTogether() {
    final SpanWireDefinition centred = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true);
    final SpanWireDefinition offset = centred.withSignalSide(SpanWireSignalSide.LEFT);

    final double shift = -SpanWireSignalSide.OFFSET;

    // Messenger and tether both move, by the same amount.
    assertEquals(centred.solve().pointAt(0.5).z + shift,
        offset.solve().pointAt(0.5).z, 1.0e-9);
    assertEquals(centred.solveTether().pointAt(0.5).z + shift,
        offset.solveTether().pointAt(0.5).z, 1.0e-9);

    // So they stay directly above one another -- shifting one and not the other is the bug this
    // replaced, and would show up here as the two ending on different z.
    assertEquals(offset.solve().pointAt(0.5).z, offset.solveTether().pointAt(0.5).z, 1.0e-9);

    // A sideways move must not disturb either curve's height or its clearance.
    assertEquals(centred.solve().pointAt(0.5).y, offset.solve().pointAt(0.5).y, 1.0e-9);
    assertEquals(centred.solveTether().pointAt(0.5).y, offset.solveTether().pointAt(0.5).y, 1.0e-9);
  }

  @Test
  void attachPointsFollowTheSpanOffsetAndTheStaticOneDoesNot() {
    final SpanWireDefinition offset = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, false, SpanWireSignalSide.RIGHT);
    final BlockPos mount = new BlockPos(10, 69, 0);

    // The static accessor is the raw block centre and stays that way -- geometry is defined
    // against it, so moving it would move everything twice.
    assertEquals(0.5, SpanWireDefinition.attachPoint(mount).z - mount.getZ(), 1.0e-9);
    // The span's own is where the hardware actually goes.
    assertEquals(SpanWireSignalSide.OFFSET,
        offset.attachPointOn(mount).z - SpanWireDefinition.attachPoint(mount).z, 1.0e-9);
    // Height is untouched, so nothing that depends on the drop has to know about any of this.
    assertEquals(SpanWireDefinition.attachPoint(mount).y, offset.attachPointOn(mount).y, 1.0e-9);
  }

  @Test
  void aSideOffsetDoesNotMoveAnythingAlongTheSpan() {
    // The offset is perpendicular, so where a mount sits along the span is unchanged -- which is
    // what lets the drop, the ordering and the segment ownership all ignore it.
    final SpanWireDefinition centred = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(20, 70, 0),
        Collections.singletonList(new BlockPos(10, 69, 0)), SpanWireCatenary.DEFAULT_SLACK, false);
    final SpanWireDefinition offset = centred.withSignalSide(SpanWireSignalSide.LEFT);

    assertEquals(centred.cableDropAt(new BlockPos(10, 69, 0)),
        offset.cableDropAt(new BlockPos(10, 69, 0)), 1.0e-9);
    assertEquals(centred.solve().parameterAt(10.5, 0.5),
        offset.solve().parameterAt(10.5, 0.5), 1.0e-9);
  }

  @Test
  void signalSideOffsetIsPerpendicularOnADiagonalSpanToo() {
    final SpanWireDefinition diagonal = new SpanWireDefinition(
        new BlockPos(0, 70, 0), new BlockPos(15, 70, 20), Collections.emptyList(),
        SpanWireCatenary.DEFAULT_SLACK, true, SpanWireSignalSide.RIGHT);

    final double dot = diagonal.spanOffset().x * diagonal.horizontalDirection().x
        + diagonal.spanOffset().z * diagonal.horizontalDirection().z;
    assertEquals(0.0, dot, 1.0e-9, "the offset must be square to the span, not along it");
    assertEquals(SpanWireSignalSide.OFFSET,
        Math.hypot(diagonal.spanOffset().x, diagonal.spanOffset().z), 1.0e-9);
  }

  @Test
  void theTetherHangsLowerAtTheAnchorsTheLongerTheSpan() {
    // Because a longer span sags more, the tether has to start further down to still clear the
    // signals in the middle. A drop that did not grow would be the bug this replaced.
    final double shortDrop = new SpanWireDefinition(new BlockPos(0, 70, 0),
        new BlockPos(10, 70, 0), Collections.emptyList(), SpanWireCatenary.DEFAULT_SLACK, true)
        .tetherDropAtAnchors();
    final double longDrop = new SpanWireDefinition(new BlockPos(0, 70, 0),
        new BlockPos(50, 70, 0), Collections.emptyList(), SpanWireCatenary.DEFAULT_SLACK, true)
        .tetherDropAtAnchors();

    assertTrue(longDrop > shortDrop + 1.0,
        "a 50-block span should dead end its tether well below a 10-block one, got "
            + shortDrop + " and " + longDrop);
    assertTrue(shortDrop >= SpanWireDefinition.TETHER_MIN_CLEARANCE,
        "even the shortest span never drops less than the minimum clearance");
  }
}
