package com.micatechnologies.minecraft.csm.trafficsigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import org.junit.jupiter.api.Test;

/**
 * Tests the rule that decides which sign of a facing-opposite pair moves into the other's block.
 *
 * <p>The back-to-back model draws the plate a block behind the sign, on the far side of the
 * partner's post, so the pair only looks right when exactly one of the two takes it. Anything
 * that lets both of them take it puts each sign behind the other with its art pointing inward,
 * which is what a pair hung in the air used to do.</p>
 */
class SignBackToBackRuleTest {

  /** The four facings a sign can be back-to-back in; the diagonals never are. */
  private static final DirectionEight[] SQUARE_ON =
      {DirectionEight.N, DirectionEight.E, DirectionEight.S, DirectionEight.W};

  /**
   * Asks the rule for both signs of a pair and returns how many of them move.
   *
   * @param facing          the facing of the first sign; the partner faces the opposite way
   * @param supportedHere   whether the first sign has support below
   * @param supportedBehind whether the partner has support below
   *
   * @return the number of signs of the pair that take the back-to-back shift
   */
  private static int shiftersInPair(DirectionEight facing, boolean supportedHere,
      boolean supportedBehind) {
    boolean here =
        AbstractBlockSign.shouldBackToBack(facing, true, supportedHere, supportedBehind);
    boolean there = AbstractBlockSign.shouldBackToBack(facing.getOpposite(), true, supportedBehind,
        supportedHere);
    return (here ? 1 : 0) + (there ? 1 : 0);
  }

  @Test
  void theUnsupportedSignOfAPairIsTheOneThatMoves() {
    for (DirectionEight facing : SQUARE_ON) {
      assertTrue(AbstractBlockSign.shouldBackToBack(facing, true, false, true),
          facing + " hanging in front of a posted sign should move");
      assertFalse(AbstractBlockSign.shouldBackToBack(facing, true, true, false),
          facing + " standing on its own post should stay put");
    }
  }

  @Test
  void exactlyOneOfAnUnsupportedPairMoves() {
    for (DirectionEight facing : SQUARE_ON) {
      assertEquals(1, shiftersInPair(facing, false, false),
          "a pair of " + facing + "/" + facing.getOpposite()
              + " signs with nothing under either of them");
    }
  }

  @Test
  void exactlyOneMovesWheneverThePairCanBeBackToBackAtAll() {
    for (DirectionEight facing : SQUARE_ON) {
      assertEquals(1, shiftersInPair(facing, false, true), facing + " unsupported, partner posted");
      assertEquals(1, shiftersInPair(facing, true, false), facing + " posted, partner unsupported");
      assertEquals(0, shiftersInPair(facing, true, true), facing + " both posted");
    }
  }

  @Test
  void theTieBreakGivesTheSameAnswerFromEitherSide() {
    // The two blocks never see each other's shift, so the rule has to be a property of the pair
    // and not of whichever one the renderer reaches first.
    for (DirectionEight facing : SQUARE_ON) {
      assertTrue(
          AbstractBlockSign.isDesignatedShifter(facing)
              != AbstractBlockSign.isDesignatedShifter(facing.getOpposite()),
          facing + " and its opposite must not both be the designated shifter");
    }
  }

  @Test
  void aSignWithNoPartnerBehindItNeverMoves() {
    for (DirectionEight facing : DirectionEight.values()) {
      assertFalse(AbstractBlockSign.shouldBackToBack(facing, false, false, false),
          facing + " alone");
      assertFalse(AbstractBlockSign.shouldBackToBack(facing, false, false, true),
          facing + " alone, over nothing");
    }
  }

  @Test
  void aDiagonalSignNeverMoves() {
    for (DirectionEight facing : DirectionEight.values()) {
      if (!facing.isDiagonal()) {
        continue;
      }
      assertFalse(AbstractBlockSign.shouldBackToBack(facing, true, false, false),
          facing + " is diagonal: its partner is not square behind it");
      assertFalse(AbstractBlockSign.shouldBackToBack(facing, true, false, true), facing.toString());
    }
  }
}
