package com.micatechnologies.minecraft.csm.venues.bowling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BowlingScoreTest {

  @Test
  void perfectGameIs300() {
    BowlingScore s = BowlingScore.parse("Ace: XXXXXXXXXXXX");
    assertEquals("ACE", s.getName());
    assertEquals(300, s.total(9));
    assertArrayEquals(new String[]{"X", "X", "X"}, s.marks(9));
    assertArrayEquals(new String[]{"", "X"}, s.marks(0));
    assertEquals(10, s.currentFrame());
  }

  @Test
  void allSparesOfFiveIs150() {
    BowlingScore s = BowlingScore.parse("Pat: 5/5/5/5/5/5/5/5/5/5/5");
    assertEquals(150, s.total(9));
    assertArrayEquals(new String[]{"5", "/"}, s.marks(3));
    assertArrayEquals(new String[]{"5", "/", "5"}, s.marks(9));
  }

  @Test
  void openGameAndMisses() {
    BowlingScore s = BowlingScore.parse("Sam: 9- 8- -7 F1 45 9- 9- 9- 9- 9-");
    assertEquals(9, s.total(0));
    assertArrayEquals(new String[]{"F", "1"}, s.marks(3));
    assertEquals(9 + 8 + 7 + 1 + 9 + 9 * 5, s.total(9));
  }

  @Test
  void strikeWaitsForTwoBalls() {
    BowlingScore s = BowlingScore.parse("Lee: X 7");
    assertEquals(-1, s.total(0));
    assertEquals(0, s.currentFrame());
    s = BowlingScore.parse("Lee: X 72");
    assertEquals(19, s.total(0));
    assertEquals(28, s.total(1));
  }

  @Test
  void tooManyPinsAreCutAndJunkSkipped() {
    BowlingScore s = BowlingScore.parse("Kim: 9 9 ? 3");
    // 9 then "9" with one pin standing is a spare; then 3
    assertArrayEquals(new String[]{"9", "/"}, s.marks(0));
    assertEquals(13, s.total(0));
  }

  @Test
  void blankLineIsEmpty() {
    assertTrue(BowlingScore.parse("").isEmpty());
    assertTrue(BowlingScore.parse(null).isEmpty());
    assertEquals("JO", BowlingScore.parse("jo").getName());
  }

  @Test
  void tenthFrameWithoutBonusStopsAtTwo() {
    BowlingScore s = BowlingScore.parse("Al: -- -- -- -- -- -- -- -- -- 72 9");
    assertEquals(9, s.total(9));
    assertEquals(20, s.balls());
  }
}
