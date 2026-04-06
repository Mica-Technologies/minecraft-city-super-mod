package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalTextureMapTest {

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbColor.class)
  void textureInfoHasValidUVForLedBallLit(TrafficSignalBulbColor color) {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL, color, true);
    assertNotNull(info);
    assertValidUV(info);
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbColor.class)
  void textureInfoHasValidUVForLedBallUnlit(TrafficSignalBulbColor color) {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL, color, false);
    assertNotNull(info);
    assertValidUV(info);
  }

  @Test
  void allStyleTypeColorCombinationsProduceValidUV() {
    for (TrafficSignalBulbStyle style : TrafficSignalBulbStyle.values()) {
      for (TrafficSignalBulbType type : TrafficSignalBulbType.values()) {
        for (TrafficSignalBulbColor color : TrafficSignalBulbColor.values()) {
          for (boolean lit : new boolean[]{true, false}) {
            var info = TrafficSignalTextureMap.getTextureInfoForBulb(style, type, color, lit);
            assertNotNull(info, "Null TextureInfo for " + style + "/" + type + "/" + color + "/" + lit);
            assertValidUV(info, style + "/" + type + "/" + color + "/" + lit);
          }
        }
      }
    }
  }

  @Test
  void leftArrowHasZeroRotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT, TrafficSignalBulbColor.GREEN, true);
    assertEquals(0f, info.getRotation(), 0.001f);
  }

  @Test
  void upArrowHas90Rotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP, TrafficSignalBulbColor.GREEN, true);
    assertEquals(90f, info.getRotation(), 0.001f);
  }

  @Test
  void rightArrowHas180Rotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT, TrafficSignalBulbColor.GREEN, true);
    assertEquals(180f, info.getRotation(), 0.001f);
  }

  @Test
  void upLeftArrowHas45Rotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT, TrafficSignalBulbColor.GREEN, true);
    assertEquals(45f, info.getRotation(), 0.001f);
  }

  @Test
  void upRightArrowHas135Rotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_RIGHT, TrafficSignalBulbColor.GREEN, true);
    assertEquals(135f, info.getRotation(), 0.001f);
  }

  @Test
  void ballHasZeroRotation() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL, TrafficSignalBulbColor.GREEN, true);
    assertEquals(0f, info.getRotation(), 0.001f);
  }

  @Test
  void bikeForcesLedStyle() {
    // BIKE with INCANDESCENT should still produce valid texture (forced to LED internally)
    var incandescentInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.INCANDESCENT, TrafficSignalBulbType.BIKE,
        TrafficSignalBulbColor.GREEN, true);
    var ledInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
        TrafficSignalBulbColor.GREEN, true);
    assertNotNull(incandescentInfo);
    assertNotNull(ledInfo);
    // Both should produce the same UV since BIKE forces LED
    assertEquals(ledInfo.getU1(), incandescentInfo.getU1(), 0.001f);
    assertEquals(ledInfo.getV1(), incandescentInfo.getV1(), 0.001f);
    assertEquals(ledInfo.getU2(), incandescentInfo.getU2(), 0.001f);
    assertEquals(ledInfo.getV2(), incandescentInfo.getV2(), 0.001f);
  }

  @Test
  void uturnForcesLedStyle() {
    var incandescentInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.INCANDESCENT, TrafficSignalBulbType.UTURN,
        TrafficSignalBulbColor.GREEN, true);
    var ledInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
        TrafficSignalBulbColor.GREEN, true);
    assertEquals(ledInfo.getU1(), incandescentInfo.getU1(), 0.001f);
    assertEquals(ledInfo.getV1(), incandescentInfo.getV1(), 0.001f);
  }

  @Test
  void textureInfoHasNonNullTextureName() {
    var info = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.GREEN, true);
    assertNotNull(info.getTexture());
    assertFalse(info.getTexture().isEmpty());
  }

  @Test
  void aheadGreenUsesUpArrow() {
    // AHEAD with GREEN should behave like UP arrow
    var aheadInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.AHEAD,
        TrafficSignalBulbColor.GREEN, true);
    var upInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
        TrafficSignalBulbColor.GREEN, true);
    assertEquals(upInfo.getU1(), aheadInfo.getU1(), 0.001f);
    assertEquals(upInfo.getV1(), aheadInfo.getV1(), 0.001f);
    assertEquals(upInfo.getRotation(), aheadInfo.getRotation(), 0.001f);
  }

  @Test
  void aheadRedUsesBall() {
    // AHEAD with RED should behave like BALL
    var aheadInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.AHEAD,
        TrafficSignalBulbColor.RED, true);
    var ballInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.RED, true);
    assertEquals(ballInfo.getU1(), aheadInfo.getU1(), 0.001f);
    assertEquals(ballInfo.getV1(), aheadInfo.getV1(), 0.001f);
  }

  private void assertValidUV(TrafficSignalTextureMap.TextureInfo info) {
    assertValidUV(info, "");
  }

  private void assertValidUV(TrafficSignalTextureMap.TextureInfo info, String context) {
    assertTrue(info.getU1() >= 0.0f && info.getU1() <= 1.0f,
        "U1 out of range: " + info.getU1() + " " + context);
    assertTrue(info.getV1() >= 0.0f && info.getV1() <= 1.0f,
        "V1 out of range: " + info.getV1() + " " + context);
    assertTrue(info.getU2() >= 0.0f && info.getU2() <= 1.0f,
        "U2 out of range: " + info.getU2() + " " + context);
    assertTrue(info.getV2() >= 0.0f && info.getV2() <= 1.0f,
        "V2 out of range: " + info.getV2() + " " + context);
    assertTrue(info.getU2() > info.getU1(), "U2 should be > U1 " + context);
  }
}
