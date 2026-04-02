package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.math.AxisAlignedBB;

/**
 * Computes accurate bounding boxes for traffic signal head blocks based on the same
 * sizing/positioning data the TESR renderer uses. Returns NORTH-facing AABBs in
 * block-relative coordinates. The existing rotation infrastructure in
 * {@link com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW}
 * handles facing rotation via {@code RotationUtils.rotateBoundingBoxByFacing()}.
 *
 * @since 1.0
 */
public final class TrafficSignalBoundingBoxHelper {

  // Model-space constants (from TrafficSignalVertexData, 12-inch reference).
  // All coordinates are in model units where 16 units = 1 block.
  private static final float BODY_MIN_X = 2.0f;
  private static final float BODY_MAX_X = 14.0f;
  private static final float BODY_MIN_Y = 0.0f;
  private static final float BODY_MAX_Y = 12.0f;
  private static final float VISOR_MIN_Z = 2.0f;
  private static final float BODY_MAX_Z = 16.0f;
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;
  private static final float CENTER_Z_VISOR = 11.0f;

  private TrafficSignalBoundingBoxHelper() {}

  /**
   * Computes the Z push-back for uniform-size signals so that the body back stays flush
   * with the block face (Z=16). Mixed-size signals get no push-back so section fronts
   * align with the largest section. Shared by both the renderer and bounding box utility.
   *
   * @param sectionSizes per-section sizes (12, 8, or 4 inches)
   *
   * @return the Z push-back in model units
   */
  public static float computeZPushBack(int[] sectionSizes) {
    if (sectionSizes.length == 0) return 0f;
    int firstSize = sectionSizes[0];
    if (firstSize >= 12) return 0f;
    for (int size : sectionSizes) {
      if (size != firstSize) return 0f;
    }
    float scale = firstSize / 12f;
    return 5f * (1f - scale);
  }

  /**
   * Convenience method that pulls all parameters from a signal head block and computes
   * its NORTH-facing bounding box.
   *
   * @param signalBlock the signal head block to compute the bounding box for
   *
   * @return a NORTH-facing {@link AxisAlignedBB} in block-relative coordinates
   */
  public static AxisAlignedBB computeBoundingBox(AbstractBlockControllableSignalHead signalBlock) {
    int sectionCount = signalBlock.getDefaultTrafficSignalSectionInfo().length;
    return computeBoundingBox(
        sectionCount,
        signalBlock.getSectionYPositions(sectionCount),
        signalBlock.getSectionXPositions(sectionCount),
        signalBlock.getSectionSizes(sectionCount),
        signalBlock.getSignalYOffset());
  }

  /**
   * Computes a NORTH-facing bounding box in block coordinates for the given signal head
   * parameters. The bounding box encompasses all sections including body and visor geometry,
   * using the same sizing logic as the TESR renderer.
   *
   * @param sectionCount     number of signal sections
   * @param sectionYPositions per-section Y offsets in model units
   * @param sectionXPositions per-section X offsets in model units
   * @param sectionSizes     per-section sizes (12, 8, or 4 inches)
   * @param signalYOffset    overall Y shift in model units
   *
   * @return a NORTH-facing {@link AxisAlignedBB} in block-relative coordinates
   */
  public static AxisAlignedBB computeBoundingBox(
      int sectionCount,
      float[] sectionYPositions,
      float[] sectionXPositions,
      int[] sectionSizes,
      float signalYOffset) {

    float zPushBack = computeZPushBack(sectionSizes);

    float globalMinX = Float.MAX_VALUE;
    float globalMaxX = -Float.MAX_VALUE;
    float globalMinY = Float.MAX_VALUE;
    float globalMaxY = -Float.MAX_VALUE;
    float globalMinZ = Float.MAX_VALUE;
    float globalMaxZ = -Float.MAX_VALUE;

    for (int i = 0; i < sectionCount; i++) {
      float sectionSize = sectionSizes[i];
      float scale = sectionSize / 12.0f;

      // Section-local extents scaled from 12-inch reference around center
      float localMinX = CENTER_X + (BODY_MIN_X - CENTER_X) * scale;
      float localMaxX = CENTER_X + (BODY_MAX_X - CENTER_X) * scale;
      float localMinY = CENTER_Y + (BODY_MIN_Y - CENTER_Y) * scale;
      float localMaxY = CENTER_Y + (BODY_MAX_Y - CENTER_Y) * scale;
      float localMinZ = CENTER_Z_VISOR + (VISOR_MIN_Z - CENTER_Z_VISOR) * scale;
      float localMaxZ = BODY_MAX_Z;

      // Apply section position offsets
      float sMinX = localMinX + sectionXPositions[i];
      float sMaxX = localMaxX + sectionXPositions[i];
      float sMinY = localMinY + sectionYPositions[i];
      float sMaxY = localMaxY + sectionYPositions[i];
      float sMinZ = localMinZ + zPushBack;
      float sMaxZ = localMaxZ + zPushBack;

      // Accumulate global extents
      globalMinX = Math.min(globalMinX, sMinX);
      globalMaxX = Math.max(globalMaxX, sMaxX);
      globalMinY = Math.min(globalMinY, sMinY);
      globalMaxY = Math.max(globalMaxY, sMaxY);
      globalMinZ = Math.min(globalMinZ, sMinZ);
      globalMaxZ = Math.max(globalMaxZ, sMaxZ);
    }

    // Apply per-block Y offset
    globalMinY += signalYOffset;
    globalMaxY += signalYOffset;

    // Convert model units to block coordinates (16 model units = 1 block)
    return new AxisAlignedBB(
        globalMinX / 16.0, globalMinY / 16.0, globalMinZ / 16.0,
        globalMaxX / 16.0, globalMaxY / 16.0, globalMaxZ / 16.0);
  }
}
