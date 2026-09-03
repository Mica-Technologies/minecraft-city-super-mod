package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrosswalkTextureMapTest {

  @Test
  void atlasTextureIsNonNull() {
    assertNotNull(CrosswalkTextureMap.ATLAS_TEXTURE);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
  void getAtlasUVProducesValidCoordinates(int index) {
    float[] uv = CrosswalkTextureMap.getAtlasUV(index);
    assertNotNull(uv);
    assertEquals(4, uv.length);
    float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];
    assertTrue(u1 >= 0.0f && u1 <= 1.0f, "u1 out of range: " + u1);
    assertTrue(v1 >= 0.0f && v1 <= 1.0f, "v1 out of range: " + v1);
    assertTrue(u2 >= 0.0f && u2 <= 1.0f, "u2 out of range: " + u2);
    assertTrue(v2 >= 0.0f && v2 <= 1.0f, "v2 out of range: " + v2);
    assertTrue(u2 > u1, "u2 should be > u1");
    assertTrue(v2 > v1, "v2 should be > v1");
  }

  @Test
  void index0IsTopLeftTile() {
    float[] uv = CrosswalkTextureMap.getAtlasUV(0);
    assertEquals(0.0f, uv[0], 0.001f); // u1 = 0
    assertEquals(0.0f, uv[1], 0.001f); // v1 = 0
    assertEquals(0.25f, uv[2], 0.001f); // u2 = 1/4
    assertEquals(0.25f, uv[3], 0.001f); // v2 = 1/4
  }

  @Test
  void index3IsTopRightTile() {
    float[] uv = CrosswalkTextureMap.getAtlasUV(3);
    assertEquals(0.75f, uv[0], 0.001f); // u1 = 3/4
    assertEquals(0.0f, uv[1], 0.001f);  // v1 = 0
    assertEquals(1.0f, uv[2], 0.001f);  // u2 = 1
    assertEquals(0.25f, uv[3], 0.001f); // v2 = 1/4
  }

  @Test
  void index4IsSecondRowFirstTile() {
    float[] uv = CrosswalkTextureMap.getAtlasUV(4);
    assertEquals(0.0f, uv[0], 0.001f);  // u1 = 0
    assertEquals(0.25f, uv[1], 0.001f); // v1 = 1/4
    assertEquals(0.25f, uv[2], 0.001f); // u2 = 1/4
    assertEquals(0.50f, uv[3], 0.001f); // v2 = 2/4
  }

  // --- Single face (16-inch) atlas index tests ---

  @Test
  void singleFaceDontWalkLitReturnsIndex0() {
    assertEquals(0, CrosswalkTextureMap.getSingleFaceAtlasIndex(0, true));
  }

  @Test
  void singleFaceFlashingDontWalkWithFlashOnReturnsHandLit() {
    assertEquals(0, CrosswalkTextureMap.getSingleFaceAtlasIndex(1, true));
  }

  @Test
  void singleFaceFlashingDontWalkWithFlashOffReturnsOff() {
    assertEquals(2, CrosswalkTextureMap.getSingleFaceAtlasIndex(1, false));
  }

  @Test
  void singleFaceWalkLitReturnsIndex1() {
    assertEquals(1, CrosswalkTextureMap.getSingleFaceAtlasIndex(2, true));
  }

  @Test
  void singleFaceOffReturnsIndex2() {
    assertEquals(2, CrosswalkTextureMap.getSingleFaceAtlasIndex(3, true));
  }

  // --- Hand/Man stacked (12-inch) upper atlas index tests ---

  @Test
  void handManUpperDontWalkReturnsIndex3() {
    assertEquals(3, CrosswalkTextureMap.getHandManUpperAtlasIndex(0, true));
  }

  @Test
  void handManUpperFlashOnReturnsHandLit12() {
    assertEquals(3, CrosswalkTextureMap.getHandManUpperAtlasIndex(1, true));
  }

  @Test
  void handManUpperFlashOffReturnsOff12() {
    assertEquals(5, CrosswalkTextureMap.getHandManUpperAtlasIndex(1, false));
  }

  @Test
  void handManUpperWalkReturnsManLit12() {
    assertEquals(4, CrosswalkTextureMap.getHandManUpperAtlasIndex(2, true));
  }

  @Test
  void handManLowerReturnsBase12() {
    assertEquals(6, CrosswalkTextureMap.getHandManLowerAtlasIndex());
  }

  // --- Worded (12-inch) atlas index tests ---

  @Test
  void wordedUpperDontWalkReturnsLit() {
    assertEquals(7, CrosswalkTextureMap.getWordedUpperAtlasIndex(0, true));
  }

  @Test
  void wordedUpperFlashOnReturnsDontWalkLit() {
    assertEquals(7, CrosswalkTextureMap.getWordedUpperAtlasIndex(1, true));
  }

  @Test
  void wordedUpperFlashOffReturnsDontWalkOff() {
    assertEquals(8, CrosswalkTextureMap.getWordedUpperAtlasIndex(1, false));
  }

  @Test
  void wordedUpperWalkReturnsDontWalkOff() {
    assertEquals(8, CrosswalkTextureMap.getWordedUpperAtlasIndex(2, true));
  }

  @Test
  void wordedLowerWalkReturnsWalkLit() {
    assertEquals(9, CrosswalkTextureMap.getWordedLowerAtlasIndex(2, true));
  }

  @Test
  void wordedLowerDontWalkReturnsWalkOff() {
    assertEquals(10, CrosswalkTextureMap.getWordedLowerAtlasIndex(0, true));
  }
}
