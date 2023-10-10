package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Utility class to provide methods and otherwise necessary functionality for the rotation of blocks and their bounding
 * boxes.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class RotationUtils
{
    /**
     * Rotates the specified bounding box (default/north) to align with the specified {@link EnumFacing} value.
     *
     * @param boundingBox the bounding box to rotate (default/north)
     * @param facing      the direction to rotate the bounding box to
     *
     * @return the rotated bounding box which aligns with the specified {@link EnumFacing} value
     *
     * @since 1.0
     */
    public static AxisAlignedBB rotateBoundingBoxByFacing( AxisAlignedBB boundingBox, EnumFacing facing ) {
        // Apply rotation to the bounding box if necessary (north is default)
        AxisAlignedBB defaultBoundingBox = boundingBox;
        if ( facing != EnumFacing.NORTH ) {
            switch ( facing ) {
                case SOUTH:
                    defaultBoundingBox = new AxisAlignedBB( 1.0D - defaultBoundingBox.maxX, defaultBoundingBox.minY,
                                                            1.0D - defaultBoundingBox.maxZ,
                                                            1.0D - defaultBoundingBox.minX, defaultBoundingBox.maxY,
                                                            1.0D - defaultBoundingBox.minZ );
                    break;
                case EAST:
                    defaultBoundingBox = new AxisAlignedBB( 1.0D - defaultBoundingBox.maxZ, defaultBoundingBox.minY,
                                                            defaultBoundingBox.minX, 1.0D - defaultBoundingBox.minZ,
                                                            defaultBoundingBox.maxY, defaultBoundingBox.maxX );
                    break;
                case WEST:
                    defaultBoundingBox = new AxisAlignedBB( defaultBoundingBox.minZ, defaultBoundingBox.minY,
                                                            1.0D - defaultBoundingBox.maxX, defaultBoundingBox.maxZ,
                                                            defaultBoundingBox.maxY, 1.0D - defaultBoundingBox.minX );
                    break;
                case UP:
                    defaultBoundingBox = new AxisAlignedBB( defaultBoundingBox.minX, 1.0D - defaultBoundingBox.maxZ,
                                                            defaultBoundingBox.minY, defaultBoundingBox.maxX,
                                                            1.0D - defaultBoundingBox.minZ, defaultBoundingBox.maxY );
                    break;
                case DOWN:
                    defaultBoundingBox = new AxisAlignedBB( 1.0D - defaultBoundingBox.maxX, defaultBoundingBox.minZ,
                                                            1.0D - defaultBoundingBox.maxY, 1.0D - defaultBoundingBox.minX,
                                                            defaultBoundingBox.maxZ, 1.0D - defaultBoundingBox.minY );
                    break;
            }
        }

        return defaultBoundingBox;
    }
}
