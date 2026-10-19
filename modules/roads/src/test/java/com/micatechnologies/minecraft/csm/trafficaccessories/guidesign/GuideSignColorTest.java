package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import org.junit.jupiter.api.Test;

class GuideSignColorTest {

  /**
   * The stored ordinals. A colour may only be appended: moving one changes what every saved
   * sign is painted.
   */
  @Test
  void ordinalsAreAppendOnly() {
    GuideSignColor[] expected = {
        GuideSignColor.GREEN, GuideSignColor.BLUE, GuideSignColor.BROWN, GuideSignColor.YELLOW,
        GuideSignColor.WHITE, GuideSignColor.BLACK, GuideSignColor.PURPLE, GuideSignColor.RED,
        GuideSignColor.ORANGE, GuideSignColor.FLUORESCENT_YELLOW_GREEN, GuideSignColor.CORAL};
    for (int i = 0; i < expected.length; i++) {
      assertEquals(i, expected[i].ordinal(), expected[i].name());
      assertEquals(expected[i], GuideSignColor.fromNBT(i));
    }
  }

  @Test
  void outOfRangeFallsBackToDefault() {
    int count = GuideSignColor.values().length;
    assertEquals(GuideSignColor.DEFAULT, GuideSignColor.fromNBT(-1));
    assertEquals(GuideSignColor.DEFAULT, GuideSignColor.fromNBT(count));
    assertEquals(GuideSignColor.DEFAULT, GuideSignColor.fromNBT(Integer.MAX_VALUE));
    assertEquals(GuideSignColor.DEFAULT.ordinal(), GuideSignColor.sanitize(99));
    assertEquals(GuideSignColor.CORAL.ordinal(), GuideSignColor.sanitize(10));
  }

  @Test
  void legendContrastFollowsMutcd() {
    assertTrue(GuideSignColor.YELLOW.isLight());
    assertTrue(GuideSignColor.WHITE.isLight());
    assertTrue(GuideSignColor.ORANGE.isLight());
    assertTrue(GuideSignColor.FLUORESCENT_YELLOW_GREEN.isLight());
    assertTrue(GuideSignColor.CORAL.isLight());
    assertFalse(GuideSignColor.RED.isLight());
    assertFalse(GuideSignColor.GREEN.isLight());
  }

  @Test
  void cyclingReachesNewColoursAndWraps() {
    assertEquals(GuideSignColor.RED, GuideSignColor.PURPLE.next());
    assertEquals(GuideSignColor.GREEN, GuideSignColor.CORAL.next());
  }

  @Test
  void guideSignDocumentRepairsOutOfRangeColours() {
    GuideSignData data = GuideSignData.fromJson("{\"signColor\":42,\"panels\":[{\"rows\":[],"
        + "\"exitTab\":{\"position\":2,\"text\":\"EXIT\",\"color\":-3}}]}");
    assertEquals(GuideSignColor.DEFAULT.ordinal(), data.getSignColorOrdinal());
    assertEquals(GuideSignColor.DEFAULT.ordinal(), data.getPanels().get(0).getExitTab().getColor());

    GuideSignData kept = GuideSignData.fromJson("{\"signColor\":8}");
    assertEquals(GuideSignColor.ORANGE, kept.getSignColor());
  }

  @Test
  void streetSignDocumentRepairsOutOfRangeColour() {
    StreetSignData data = StreetSignData.fromJson("{\"signColor\":11,\"streetName\":\"LEVEL 9\"}");
    assertEquals(GuideSignColor.DEFAULT, data.getSignColor());
    assertTrue(data.toJson().contains("\"signColor\":0"));

    StreetSignData coral = StreetSignData.fromJson("{\"signColor\":10}");
    assertEquals(GuideSignColor.CORAL, coral.getSignColor());
  }
}
