package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import org.junit.jupiter.api.Test;

class StreetSignDataTest {

  /**
   * A document exactly as the single-blade format wrote it, before the legend moved into
   * {@link StreetSignLegend}: every key at the top level, no {@code lowerBlade}.
   */
  private static final String OLD_FORMAT_JSON = "{\"version\":1,\"signColor\":1,\"borderWidth\":2,"
      + "\"cornerStyle\":0,\"mountType\":2,\"extrudedFrame\":true,\"doubleSided\":false,"
      + "\"internalLight\":true,\"lightMode\":1,\"prefix\":\"W\",\"streetName\":\"BAKER\","
      + "\"suffix\":\"RD\",\"cityText\":\"UPTOWN\",\"textScale\":1.2,\"affixVertical\":2,"
      + "\"blockNumber\":\"1200\",\"blockPosition\":1,\"blockVertical\":2,\"emblemKind\":2,"
      + "\"emblemPosition\":2,\"shieldType\":3,\"shieldRoute\":\"66\",\"logoType\":4,"
      + "\"arrowPosition\":1,\"arrowType\":2,\"minWidth\":48,\"minHeight\":16}";

  @Test
  void oldSingleBladeDocumentLoadsUnchanged() {
    StreetSignData data = StreetSignData.fromJson(OLD_FORMAT_JSON);

    assertFalse(data.hasLowerBlade());
    assertNull(data.getLowerBlade());
    assertEquals("W", data.getPrefix());
    assertEquals("BAKER", data.getStreetName());
    assertEquals("RD", data.getSuffix());
    assertEquals("UPTOWN", data.getCityText());
    assertEquals("1200", data.getBlockNumber());
    assertEquals(StreetSignSlotPosition.LEFT, data.getBlockPosition());
    assertEquals("66", data.getShieldRoute());
    assertEquals(StreetSignSlotPosition.LEFT, data.getArrowPosition());
    assertEquals(GuideSignColor.fromNBT(1), data.getSignColor());
    assertEquals(StreetSignMount.fromOrdinal(2), data.getMountType());
    assertEquals(48, data.getMinWidth());
    assertEquals(16, data.getMinHeight());
  }

  @Test
  void oldSingleBladeDocumentSavesBackTheSameDocument() {
    // Key order may differ -- Gson writes a subclass's fields before its superclass's -- but
    // the keys and values must not: no new key appears for a sign without a second blade.
    JsonObject before = new JsonParser().parse(OLD_FORMAT_JSON).getAsJsonObject();
    JsonObject after = new JsonParser()
        .parse(StreetSignData.fromJson(OLD_FORMAT_JSON).toJson()).getAsJsonObject();
    assertEquals(before, after);
  }

  @Test
  void freshSignHasNoSecondBladeAndWritesNoKeyForIt() {
    StreetSignData data = new StreetSignData();
    assertFalse(data.hasLowerBlade());
    assertFalse(data.toJson().contains("lowerBlade"));
  }

  @Test
  void secondBladeRoundTripsWithoutDisturbingTheFirst() {
    StreetSignData data = new StreetSignData();
    data.setStreetName("MAIN");
    StreetSignLegend lower = StreetSignData.newLowerBlade();
    lower.setPrefix("N");
    lower.setStreetName("OAK");
    lower.setSuffix("AVE");
    lower.setCityText("MIDTOWN");
    lower.setBlockNumber("400");
    lower.setBlockPosition(StreetSignSlotPosition.RIGHT);
    lower.setEmblemKind(StreetSignEmblemKind.LOGO);
    lower.setLogoType(StreetSignLogoType.fromOrdinal(5));
    lower.setArrowPosition(StreetSignSlotPosition.LEFT);
    lower.setArrowType(GuideSignArrowType.LEFT);
    data.setLowerBlade(lower);

    StreetSignData back = StreetSignData.fromJson(data.toJson());

    assertEquals("MAIN", back.getStreetName());
    assertEquals(StreetSignSlotPosition.NONE, back.getArrowPosition());
    assertTrue(back.hasLowerBlade());
    StreetSignLegend lowerBack = back.getLowerBlade();
    assertEquals("N", lowerBack.getPrefix());
    assertEquals("OAK", lowerBack.getStreetName());
    assertEquals("AVE", lowerBack.getSuffix());
    assertEquals("MIDTOWN", lowerBack.getCityText());
    assertEquals("400", lowerBack.getBlockNumber());
    assertEquals(StreetSignSlotPosition.RIGHT, lowerBack.getBlockPosition());
    assertEquals(StreetSignEmblemKind.LOGO, lowerBack.getEmblemKind());
    assertEquals(StreetSignLogoType.fromOrdinal(5), lowerBack.getLogoType());
    assertEquals(StreetSignSlotPosition.LEFT, lowerBack.getArrowPosition());
    assertEquals(GuideSignArrowType.LEFT, lowerBack.getArrowType());
  }

  @Test
  void removingTheSecondBladeRestoresTheSingleBladeDocument() {
    StreetSignData data = StreetSignData.fromJson(OLD_FORMAT_JSON);
    data.setLowerBlade(StreetSignData.newLowerBlade());
    StreetSignData stacked = StreetSignData.fromJson(data.toJson());
    stacked.setLowerBlade(null);
    assertEquals(new JsonParser().parse(OLD_FORMAT_JSON),
        new JsonParser().parse(stacked.toJson()));
  }

  @Test
  void copyDoesNotShareTheSecondBlade() {
    StreetSignData data = new StreetSignData();
    data.setLowerBlade(StreetSignData.newLowerBlade());
    StreetSignData copy = data.copy();
    copy.getLowerBlade().setStreetName("PINE");
    assertEquals("ELM", data.getLowerBlade().getStreetName());
  }

  @Test
  void secondBladeFromAClientIsRepairedOnTheWayIn() {
    // Gson writes fields directly, bypassing the setters' caps, and the document arrives in a
    // packet -- so nulls and over-long strings in the nested blade must be repaired on load.
    String json = "{\"streetName\":\"MAIN\",\"lowerBlade\":{\"prefix\":null,"
        + "\"streetName\":\"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\",\"blockNumber\":\"12345678\","
        + "\"emblemKind\":99,\"arrowPosition\":-4}}";
    StreetSignData data = StreetSignData.fromJson(json);
    StreetSignLegend lower = data.getLowerBlade();
    assertNotNull(lower);
    assertEquals("", lower.getPrefix());
    assertEquals(StreetSignLegend.MAX_NAME_LENGTH, lower.getStreetName().length());
    assertEquals(StreetSignLegend.MAX_BLOCK_LENGTH, lower.getBlockNumber().length());
    // Fields the nested blade leaves out take the legend defaults, and bad ordinals clamp.
    assertEquals("ST", lower.getSuffix());
    assertNotNull(lower.getEmblemKind());
    assertNotNull(lower.getArrowPosition());
  }

  @Test
  void malformedSecondBladeYieldsADefaultSignRatherThanThrowing() {
    StreetSignData data = StreetSignData.fromJson("{\"streetName\":\"MAIN\",\"lowerBlade\":7}");
    assertNotNull(data);
    assertFalse(data.hasLowerBlade());
  }
}
