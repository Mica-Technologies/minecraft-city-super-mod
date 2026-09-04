package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrafficSignalBoundingBoxHelperTest {

  @Test
  void computeZPushBackAllTwelve() {
    // All 12-inch sections: no push-back needed (already full size)
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{12, 12, 12});
    assertEquals(0f, result, 0.001f);
  }

  @Test
  void computeZPushBackAllEight() {
    // All 8-inch: scale = 8/12 = 0.667, pushBack = 5 * (1 - 0.667) = 1.667
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{8, 8, 8});
    float expected = 5f * (1f - 8f / 12f);
    assertEquals(expected, result, 0.001f);
  }

  @Test
  void computeZPushBackAllFour() {
    // All 4-inch: scale = 4/12 = 0.333, pushBack = 5 * (1 - 0.333) = 3.333
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{4, 4, 4});
    float expected = 5f * (1f - 4f / 12f);
    assertEquals(expected, result, 0.001f);
  }

  @Test
  void computeZPushBackMixedSizesReturnsZero() {
    // Mixed sizes: no push-back (fronts align with largest)
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{12, 8, 4});
    assertEquals(0f, result, 0.001f);
  }

  @Test
  void computeZPushBackEmptyArrayReturnsZero() {
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{});
    assertEquals(0f, result, 0.001f);
  }

  @Test
  void computeZPushBackSingleElementTwelve() {
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{12});
    assertEquals(0f, result, 0.001f);
  }

  @Test
  void computeZPushBackSingleElementEight() {
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{8});
    float expected = 5f * (1f - 8f / 12f);
    assertEquals(expected, result, 0.001f);
  }

  @Test
  void computeZPushBackSingleElementFour() {
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{4});
    float expected = 5f * (1f - 4f / 12f);
    assertEquals(expected, result, 0.001f);
  }

  @Test
  void computeZPushBackMixedTwoSizesReturnsZero() {
    float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{8, 12});
    assertEquals(0f, result, 0.001f);
  }

  @Test
  void computeZPushBackResultIsNonNegative() {
    // All uniform sizes should give non-negative push-back
    for (int size : new int[]{4, 8, 12}) {
      float result = TrafficSignalBoundingBoxHelper.computeZPushBack(new int[]{size, size});
      assertTrue(result >= 0f, "Push-back should be non-negative for size " + size);
    }
  }
}
