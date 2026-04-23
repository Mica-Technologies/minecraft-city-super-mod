package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pre-computed vertex data for blankout box custom rendering. All geometry is defined in model
 * units (1 model unit = 1/16 of a block). The signal faces north (toward Z=0) by default; the
 * renderer rotates for other facings.
 *
 * <p>The blankout box is significantly larger than the crosswalk signal: 28 units wide x 28 units
 * tall (vs 18x16 for the single crosswalk), matching the height of an 8-8-12 signal head so it
 * can share mounting hardware. The bezel is thicker so the visor sits more inset.
 */
public class BlankoutBoxVertexData {

    // Body Z range — same as crosswalk so mounts center properly
    private static final float BODY_FRONT_Z = 4.0f;
    private static final float BODY_BACK_Z = 12.0f;

    // Body dimensions: X=-6 to 22 (28 wide), Y=-12 to 16 (28 tall)
    // Height matches 8-8-12 signal head; extra height added on top, extra width split evenly
    public static final float BODY_X_MIN = -6.0f;
    public static final float BODY_X_MAX = 22.0f;
    public static final float BODY_Y_MIN = -12.0f;
    public static final float BODY_Y_MAX = 16.0f;

    public static final List<Box> BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ BODY_X_MIN, BODY_Y_MIN, BODY_FRONT_Z },
                    new float[]{ BODY_X_MAX, BODY_Y_MAX, BODY_BACK_Z } )
    );

    // Display face — inset 1.0 unit from body edges (thicker bezel than crosswalk's 0.5)
    private static final float BEZEL_INSET = 1.0f;
    public static final float DISPLAY_FACE_Z = BODY_FRONT_Z - 0.05f;
    public static final float DISPLAY_X1 = BODY_X_MIN + BEZEL_INSET;
    public static final float DISPLAY_Y1 = BODY_Y_MIN + BEZEL_INSET;
    public static final float DISPLAY_X2 = BODY_X_MAX - BEZEL_INSET;
    public static final float DISPLAY_Y2 = BODY_Y_MAX - BEZEL_INSET;

    // ========================================================================================
    // MOUNT BRACKETS
    // ========================================================================================

    private static final float MOUNT_Z1 = 7.0f;
    private static final float MOUNT_Z2 = 9.0f;
    private static final float MOUNT_X1 = 7.0f;
    private static final float MOUNT_X2 = 9.0f;

    private static final float LOWER_ARM_Y = BODY_Y_MIN - 3.0f;  // -15
    private static final float UPPER_ARM_Y = BODY_Y_MAX + 3.0f;   // 15

    // ========================================================================================
    // VISORS
    // ========================================================================================

    public static final List<Box> VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Visor inset from body edge — thicker bezel means the visor is more inset
    private static final float VISOR_INSET = 1.0f;

    // Hood style: U-shaped hood wrapping top and sides, extending to bottom
    private static final float HOOD_DEPTH = 6.0f;

    public static final List<Box> VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ BODY_X_MIN + BEZEL_INSET + VISOR_INSET,
                    BODY_Y_MAX - BEZEL_INSET - VISOR_INSET, BODY_FRONT_Z - HOOD_DEPTH },
                    new float[]{ BODY_X_MAX - BEZEL_INSET - VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } ),
            // Left side panel
            new Box( new float[]{ BODY_X_MIN + VISOR_INSET,
                    BODY_Y_MIN + VISOR_INSET, BODY_FRONT_Z - HOOD_DEPTH },
                    new float[]{ BODY_X_MIN + BEZEL_INSET + VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } ),
            // Right side panel
            new Box( new float[]{ BODY_X_MAX - BEZEL_INSET - VISOR_INSET,
                    BODY_Y_MIN + VISOR_INSET, BODY_FRONT_Z - HOOD_DEPTH },
                    new float[]{ BODY_X_MAX - VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } )
    );

    // Deep hood: same shape, deeper protrusion
    private static final float DEEP_HOOD_DEPTH = 10.0f;

    public static final List<Box> VISOR_DEEP_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ BODY_X_MIN + BEZEL_INSET + VISOR_INSET,
                    BODY_Y_MAX - BEZEL_INSET - VISOR_INSET, BODY_FRONT_Z - DEEP_HOOD_DEPTH },
                    new float[]{ BODY_X_MAX - BEZEL_INSET - VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } ),
            // Left side panel
            new Box( new float[]{ BODY_X_MIN + VISOR_INSET,
                    BODY_Y_MIN + VISOR_INSET, BODY_FRONT_Z - DEEP_HOOD_DEPTH },
                    new float[]{ BODY_X_MIN + BEZEL_INSET + VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } ),
            // Right side panel
            new Box( new float[]{ BODY_X_MAX - BEZEL_INSET - VISOR_INSET,
                    BODY_Y_MIN + VISOR_INSET, BODY_FRONT_Z - DEEP_HOOD_DEPTH },
                    new float[]{ BODY_X_MAX - VISOR_INSET,
                            BODY_Y_MAX - VISOR_INSET, BODY_FRONT_Z } )
    );

    // Visor center for dual-color inside/outside rendering
    public static final float VISOR_CENTER_X = 8.0f;
    public static final float VISOR_CENTER_Y = 2.0f;

    // ========================================================================================
    // STUBS AND ARMS
    // ========================================================================================

    public static List<Box> getStubData( CrosswalkMountType mountType ) {
        if ( mountType == CrosswalkMountType.BASE ) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                new Box( new float[]{ MOUNT_X1, LOWER_ARM_Y, MOUNT_Z1 },
                        new float[]{ MOUNT_X2, BODY_Y_MIN, MOUNT_Z2 } ),
                new Box( new float[]{ MOUNT_X1, BODY_Y_MAX, MOUNT_Z1 },
                        new float[]{ MOUNT_X2, UPPER_ARM_Y, MOUNT_Z2 } )
        );
    }

    public static List<Box> getArmData( CrosswalkMountType mountType, int tiltOffset,
            float tiltedAngleDeg, float baseAngleDeg ) {
        if ( mountType == CrosswalkMountType.BASE ) {
            return Collections.emptyList();
        }

        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();

        float[] stubBasePos = transformTiltedToBase(
                MOUNT_X1 + 1.0f, MOUNT_Z1 + 1.0f,
                tiltOffset, tiltedAngleDeg, baseAngleDeg );
        float hCenterX = stubBasePos[0];
        float hCenterZ = stubBasePos[1];
        float hx1 = hCenterX - 1.0f;
        float hx2 = hCenterX + 1.0f;
        float hz1 = hCenterZ - 1.0f;
        float hz2 = hCenterZ + 1.0f;

        switch ( mountType ) {
            case REAR:
                addAngledArm2D( boxes, hx1, hx2, hz2, MOUNT_X1, MOUNT_X2, 22.0f,
                        LOWER_ARM_Y, LOWER_ARM_Y + 1.0f );
                addAngledArm2D( boxes, hx1, hx2, hz2, MOUNT_X1, MOUNT_X2, 22.0f,
                        UPPER_ARM_Y - 1.0f, UPPER_ARM_Y );
                break;
            case LEFT:
                addAngledArm2D( boxes, hx2, hx2 + 0.01f, hz1,
                        31.0f, 32.0f, MOUNT_Z1 + 1.0f,
                        LOWER_ARM_Y, LOWER_ARM_Y + 1.0f );
                addAngledArm2D( boxes, hx2, hx2 + 0.01f, hz1,
                        31.0f, 32.0f, MOUNT_Z1 + 1.0f,
                        UPPER_ARM_Y - 1.0f, UPPER_ARM_Y );
                break;
            case RIGHT:
                addAngledArm2D( boxes, -16.0f, -15.0f, MOUNT_Z1 + 1.0f,
                        hx1 - 0.01f, hx1, hz1,
                        LOWER_ARM_Y, LOWER_ARM_Y + 1.0f );
                addAngledArm2D( boxes, -16.0f, -15.0f, MOUNT_Z1 + 1.0f,
                        hx1 - 0.01f, hx1, hz1,
                        UPPER_ARM_Y - 1.0f, UPPER_ARM_Y );
                break;
        }

        return boxes;
    }

    private static float[] transformTiltedToBase( float modelX, float modelZ,
            int tiltOffset, float tiltedAngleDeg, float baseAngleDeg ) {
        double tiltRad = Math.toRadians( tiltedAngleDeg );
        double baseRad = Math.toRadians( baseAngleDeg );

        float px = modelX + tiltOffset;
        float pz = modelZ;
        px -= 8.0f;
        pz -= 8.0f;
        float wx = (float) ( px * Math.cos( tiltRad ) + pz * Math.sin( tiltRad ) );
        float wz = (float) ( -px * Math.sin( tiltRad ) + pz * Math.cos( tiltRad ) );
        wx += 8.0f;
        wz += 8.0f;

        wx -= 8.0f;
        wz -= 8.0f;
        float bx = (float) ( wx * Math.cos( -baseRad ) + wz * Math.sin( -baseRad ) );
        float bz = (float) ( -wx * Math.sin( -baseRad ) + wz * Math.cos( -baseRad ) );
        bx += 8.0f;
        bz += 8.0f;

        return new float[]{ bx, bz };
    }

    private static final float ARM_HALF_THICKNESS = 1.0f;

    private static void addAngledArm2D( java.util.ArrayList<Box> boxes,
            float startX1, float startX2, float startZ,
            float endX1, float endX2, float endZ,
            float y1, float y2 ) {
        float startCX = ( startX1 + startX2 ) / 2;
        float endCX = ( endX1 + endX2 ) / 2;
        float xShift = endCX - startCX;
        float zShift = endZ - startZ;
        float totalDist = (float) Math.sqrt( xShift * xShift + zShift * zShift );

        if ( totalDist < 0.5f ) {
            boxes.add( new Box(
                    new float[]{ Math.min( startX1, endX1 ) - ARM_HALF_THICKNESS, y1,
                            Math.min( startZ, endZ ) - ARM_HALF_THICKNESS },
                    new float[]{ Math.max( startX2, endX2 ) + ARM_HALF_THICKNESS, y2,
                            Math.max( startZ, endZ ) + ARM_HALF_THICKNESS } ) );
            return;
        }

        int steps = Math.max( 2, Math.round( totalDist / 2.0f ) );

        for ( int i = 0; i < steps; i++ ) {
            float t0 = (float) i / steps;
            float t1 = (float) ( i + 1 ) / steps;

            float cx0 = startCX + xShift * t0;
            float cz0 = startZ + zShift * t0;
            float cx1 = startCX + xShift * t1;
            float cz1 = startZ + zShift * t1;

            boxes.add( new Box(
                    new float[]{ Math.min( cx0, cx1 ) - ARM_HALF_THICKNESS, y1,
                            Math.min( cz0, cz1 ) - ARM_HALF_THICKNESS },
                    new float[]{ Math.max( cx0, cx1 ) + ARM_HALF_THICKNESS, y2,
                            Math.max( cz0, cz1 ) + ARM_HALF_THICKNESS } ) );
        }
    }
}
