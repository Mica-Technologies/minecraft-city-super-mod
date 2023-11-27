package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Utility class to provide methods and otherwise necessary functionality for the rotation of blocks
 * and their bounding boxes.
 *
 * @author Mica Technologies
 * @version 1.1
 * @since 2023.2.0
 */
public class RotationUtils {

  /**
   * Rotates the specified bounding box (default/north) to align with the specified
   * {@link DirectionEight} value.
   *
   * @param boundingBox the bounding box to rotate (default/north)
   * @param facing      the direction to rotate the bounding box to
   *
   * @return the rotated bounding box which aligns with the specified {@link DirectionEight} value
   *
   * @since 1.1
   */
  public static AxisAlignedBB rotateBoundingBoxByFacing(AxisAlignedBB boundingBox,
      DirectionEight facing) {
    // If it's a cardinal direction, just rotate normally
    if (facing == DirectionEight.N || facing == DirectionEight.S || facing == DirectionEight.E
        || facing == DirectionEight.W) {
      return rotateBoundingBoxByFacing(boundingBox, mapDirectionEightToEnumFacing(facing));
    } else {
      // For diagonal directions, get the two associated cardinal directions
      EnumFacing[] cardinalDirections = getAssociatedCardinalDirections(facing);

      // Rotate and combine the bounding boxes for each cardinal direction
      AxisAlignedBB firstBox = rotateBoundingBoxByFacing(boundingBox, cardinalDirections[0]);
      AxisAlignedBB secondBox = rotateBoundingBoxByFacing(boundingBox, cardinalDirections[1]);

      return firstBox.union(secondBox);
    }
  }

  /**
   * Rotates the specified bounding box (default/north) to align with the specified
   * {@link EnumFacing} value.
   *
   * @param boundingBox the bounding box to rotate (default/north)
   * @param facing      the direction to rotate the bounding box to
   *
   * @return the rotated bounding box which aligns with the specified {@link EnumFacing} value
   *
   * @since 1.0
   */
  public static AxisAlignedBB rotateBoundingBoxByFacing(AxisAlignedBB boundingBox,
      EnumFacing facing) {
    // Apply rotation to the bounding box if necessary (north is default)
    AxisAlignedBB defaultBoundingBox = boundingBox;
    if (facing != EnumFacing.NORTH) {
      switch (facing) {
        case SOUTH:
          defaultBoundingBox = new AxisAlignedBB(1.0D - defaultBoundingBox.maxX,
              defaultBoundingBox.minY,
              1.0D - defaultBoundingBox.maxZ,
              1.0D - defaultBoundingBox.minX, defaultBoundingBox.maxY,
              1.0D - defaultBoundingBox.minZ);
          break;
        case EAST:
          defaultBoundingBox = new AxisAlignedBB(1.0D - defaultBoundingBox.maxZ,
              defaultBoundingBox.minY,
              defaultBoundingBox.minX, 1.0D - defaultBoundingBox.minZ,
              defaultBoundingBox.maxY, defaultBoundingBox.maxX);
          break;
        case WEST:
          defaultBoundingBox = new AxisAlignedBB(defaultBoundingBox.minZ, defaultBoundingBox.minY,
              1.0D - defaultBoundingBox.maxX, defaultBoundingBox.maxZ,
              defaultBoundingBox.maxY, 1.0D - defaultBoundingBox.minX);
          break;
        case UP:
          defaultBoundingBox = new AxisAlignedBB(defaultBoundingBox.minX,
              1.0D - defaultBoundingBox.maxZ,
              defaultBoundingBox.minY, defaultBoundingBox.maxX,
              1.0D - defaultBoundingBox.minZ, defaultBoundingBox.maxY);
          break;
        case DOWN:
          defaultBoundingBox = new AxisAlignedBB(1.0D - defaultBoundingBox.maxX,
              defaultBoundingBox.minZ,
              1.0D - defaultBoundingBox.maxY, 1.0D - defaultBoundingBox.minX,
              defaultBoundingBox.maxZ, 1.0D - defaultBoundingBox.minY);
          break;
      }
    }

    return defaultBoundingBox;
  }

  /**
   * Maps the specified {@link DirectionEight} value to the associated {@link EnumFacing} value.
   *
   * @param direction the {@link DirectionEight} value to map to an {@link EnumFacing} value\
   *
   * @return the associated {@link EnumFacing} value
   *
   * @since 1.1
   */
  private static EnumFacing mapDirectionEightToEnumFacing(DirectionEight direction) {
    switch (direction) {
      case N:
      case NE:
      case NW:
        return EnumFacing.NORTH;
      case S:
      case SE:
      case SW:
        return EnumFacing.SOUTH;
      case E:
        return EnumFacing.EAST;
      case W:
        return EnumFacing.WEST;
      default:
        return EnumFacing.NORTH; // Default to North for any unexpected cases
    }
  }

  /**
   * Gets the associated {@link EnumFacing} cardinal directions for the specified
   * {@link DirectionEight} value.
   *
   * @param facing the {@link DirectionEight} value to get the associated cardinal directions for
   *
   * @return the associated {@link EnumFacing} cardinal directions for the specified
   *     {@link DirectionEight} value
   *
   * @since 1.1
   */
  private static EnumFacing[] getAssociatedCardinalDirections(DirectionEight facing) {
    switch (facing) {
      case NE:
        return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST};
      case SE:
        return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.EAST};
      case SW:
        return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST};
      case NW:
        return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.WEST};
      default:
        throw new IllegalArgumentException("Expected a diagonal direction.");
    }
  }
}
