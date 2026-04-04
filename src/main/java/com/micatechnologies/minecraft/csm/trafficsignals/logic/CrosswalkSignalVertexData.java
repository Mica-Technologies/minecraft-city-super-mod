package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pre-computed vertex data for crosswalk signal custom rendering. All geometry is defined in
 * model units (1 model unit = 1/16 of a block). The signal faces north (toward Z=0) by default;
 * the renderer rotates for other facings.
 *
 * <p>Coordinate system: X=0-16 (left-right), Y=0-16 (bottom-top), Z=0-16 (back-front).
 * The display face is on the front (low Z side), and the back is at high Z.
 *
 * <p>Body is positioned at Z=4-12 so that left/right mount brackets (at Z=7-9) are centered
 * within the housing depth, matching the original JSON crosswalk signal models.
 */
public class CrosswalkSignalVertexData {

    // Body Z range — pushed back so mounts are centered within housing depth
    private static final float BODY_FRONT_Z = 4.0f;
    private static final float BODY_BACK_Z = 12.0f;

    // ========================================================================================
    // SINGLE-FACE BODY (16-inch crosswalk signal)
    // Squared-off rectangular housing, Z=4-12 (8 units deep)
    // ========================================================================================

    public static final List<Box> SINGLE_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 0.0f, 0.0f, BODY_FRONT_Z }, new float[]{ 16.0f, 16.0f, BODY_BACK_Z } )
    );

    // Display face position (just in front of body face)
    public static final float SINGLE_DISPLAY_FACE_Z = BODY_FRONT_Z - 0.05f;
    public static final float SINGLE_DISPLAY_X1 = 0.5f;
    public static final float SINGLE_DISPLAY_Y1 = 0.5f;
    public static final float SINGLE_DISPLAY_X2 = 15.5f;
    public static final float SINGLE_DISPLAY_Y2 = 15.5f;

    // ========================================================================================
    // DOUBLE-WORDED BODY (two stacked 12-inch sections with tapered backs)
    // Upper section (DON'T WALK): Y=12-24, Lower section (WALK): Y=0-12
    // Narrower (X=2-14), tapered back for visual style
    // ========================================================================================

    public static final List<Box> DOUBLE_UPPER_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.0f, 12.0f, BODY_FRONT_Z }, new float[]{ 14.0f, 24.0f, BODY_FRONT_Z + 1.0f } ),
            new Box( new float[]{ 2.2f, 12.0f, BODY_FRONT_Z + 1.0f }, new float[]{ 13.8f, 24.0f, BODY_FRONT_Z + 2.0f } ),
            new Box( new float[]{ 2.4f, 12.0f, BODY_FRONT_Z + 2.0f }, new float[]{ 13.6f, 24.0f, BODY_FRONT_Z + 3.0f } ),
            new Box( new float[]{ 2.6f, 12.0f, BODY_FRONT_Z + 3.0f }, new float[]{ 13.4f, 24.0f, BODY_FRONT_Z + 4.0f } ),
            new Box( new float[]{ 2.8f, 12.0f, BODY_FRONT_Z + 4.0f }, new float[]{ 13.2f, 24.0f, BODY_FRONT_Z + 5.0f } ),
            new Box( new float[]{ 3.0f, 12.0f, BODY_FRONT_Z + 5.0f }, new float[]{ 13.0f, 24.0f, BODY_FRONT_Z + 6.0f } )
    );

    public static final List<Box> DOUBLE_LOWER_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.0f, 0.0f, BODY_FRONT_Z }, new float[]{ 14.0f, 12.0f, BODY_FRONT_Z + 1.0f } ),
            new Box( new float[]{ 2.2f, 0.0f, BODY_FRONT_Z + 1.0f }, new float[]{ 13.8f, 12.0f, BODY_FRONT_Z + 2.0f } ),
            new Box( new float[]{ 2.4f, 0.0f, BODY_FRONT_Z + 2.0f }, new float[]{ 13.6f, 12.0f, BODY_FRONT_Z + 3.0f } ),
            new Box( new float[]{ 2.6f, 0.0f, BODY_FRONT_Z + 3.0f }, new float[]{ 13.4f, 12.0f, BODY_FRONT_Z + 4.0f } ),
            new Box( new float[]{ 2.8f, 0.0f, BODY_FRONT_Z + 4.0f }, new float[]{ 13.2f, 12.0f, BODY_FRONT_Z + 5.0f } ),
            new Box( new float[]{ 3.0f, 0.0f, BODY_FRONT_Z + 5.0f }, new float[]{ 13.0f, 12.0f, BODY_FRONT_Z + 6.0f } )
    );

    // Display face positions for double sections
    public static final float DOUBLE_DISPLAY_FACE_Z = BODY_FRONT_Z - 0.05f;
    public static final float DOUBLE_DISPLAY_X1 = 2.5f;
    public static final float DOUBLE_DISPLAY_X2 = 13.5f;
    public static final float DOUBLE_UPPER_DISPLAY_Y1 = 12.5f;
    public static final float DOUBLE_UPPER_DISPLAY_Y2 = 23.5f;
    public static final float DOUBLE_LOWER_DISPLAY_Y1 = 0.5f;
    public static final float DOUBLE_LOWER_DISPLAY_Y2 = 11.5f;

    // ========================================================================================
    // MOUNT BRACKETS — SINGLE (16-inch, Y=0-16)
    // Pattern from original JSON models: vertical stub connectors above/below body,
    // then horizontal arm extending in mount direction.
    // Mount center at X=7-9, Z=7-9 (centered within body depth Z=4-12)
    // ========================================================================================

    private static final float MOUNT_Z1 = 7.0f;  // mount pipe Z range
    private static final float MOUNT_Z2 = 9.0f;
    private static final float MOUNT_X1 = 7.0f;  // mount pipe X range (centered)
    private static final float MOUNT_X2 = 9.0f;

    // Rear mount — single: stub connectors + arm extending along +Z
    public static final List<Box> BRACKET_SINGLE_REAR_VERTEX_DATA = Arrays.asList(
            // Lower vertical stub (below body)
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            // Upper vertical stub (above body)
            new Box( new float[]{ MOUNT_X1, 16.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 17.0f, MOUNT_Z2 } ),
            // Lower horizontal arm extending back
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z2 }, new float[]{ MOUNT_X2, 0.0f, 22.0f } ),
            // Upper horizontal arm extending back
            new Box( new float[]{ MOUNT_X1, 16.0f, MOUNT_Z2 }, new float[]{ MOUNT_X2, 17.0f, 22.0f } )
    );

    // Left mount — single: stub connectors + arm extending to +X
    public static final List<Box> BRACKET_SINGLE_LEFT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, 16.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 17.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X2, -1.0f, MOUNT_Z1 }, new float[]{ 20.0f, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X2, 16.0f, MOUNT_Z1 }, new float[]{ 20.0f, 17.0f, MOUNT_Z2 } )
    );

    // Right mount — single: stub connectors + arm extending to -X
    public static final List<Box> BRACKET_SINGLE_RIGHT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, 16.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 17.0f, MOUNT_Z2 } ),
            new Box( new float[]{ -4.0f, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X1, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ -4.0f, 16.0f, MOUNT_Z1 }, new float[]{ MOUNT_X1, 17.0f, MOUNT_Z2 } )
    );

    // Base mount: no bracket
    public static final List<Box> BRACKET_BASE_VERTEX_DATA = Collections.emptyList();

    // ========================================================================================
    // MOUNT BRACKETS — DOUBLE (Y=0-24, upper mount at top of upper section)
    // ========================================================================================

    // Rear mount — double
    public static final List<Box> BRACKET_DOUBLE_REAR_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, 24.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 25.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z2 }, new float[]{ MOUNT_X2, 0.0f, 22.0f } ),
            new Box( new float[]{ MOUNT_X1, 24.0f, MOUNT_Z2 }, new float[]{ MOUNT_X2, 25.0f, 22.0f } )
    );

    // Left mount — double
    public static final List<Box> BRACKET_DOUBLE_LEFT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, 24.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 25.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X2, -1.0f, MOUNT_Z1 }, new float[]{ 20.0f, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X2, 24.0f, MOUNT_Z1 }, new float[]{ 20.0f, 25.0f, MOUNT_Z2 } )
    );

    // Right mount — double
    public static final List<Box> BRACKET_DOUBLE_RIGHT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ MOUNT_X1, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ MOUNT_X1, 24.0f, MOUNT_Z1 }, new float[]{ MOUNT_X2, 25.0f, MOUNT_Z2 } ),
            new Box( new float[]{ -4.0f, -1.0f, MOUNT_Z1 }, new float[]{ MOUNT_X1, 0.0f, MOUNT_Z2 } ),
            new Box( new float[]{ -4.0f, 24.0f, MOUNT_Z1 }, new float[]{ MOUNT_X1, 25.0f, MOUNT_Z2 } )
    );

    // ========================================================================================
    // VISORS — SINGLE FACE (16x16 signal)
    // Visor sits flush against body front face (Z=BODY_FRONT_Z) and protrudes forward.
    // ========================================================================================

    public static final List<Box> SINGLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood style visor: U-shaped hood wrapping top and sides, open at bottom.
    // Thin panels inset within housing bounds, protruding ~5 units forward.
    public static final List<Box> SINGLE_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 0.5f, 15.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 15.5f, 16.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 0.0f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 0.5f, 16.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 15.5f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 16.0f, 16.0f, BODY_FRONT_Z } )
    );

    // Crate style visor
    public static final List<Box> SINGLE_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            0.0f, 0.0f, 16.0f, 16.0f, 0.5f );

    // ========================================================================================
    // VISORS — DOUBLE SECTIONS
    // ========================================================================================

    public static final List<Box> DOUBLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood for upper double section
    public static final List<Box> DOUBLE_UPPER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.5f, 23.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 13.5f, 24.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 2.0f, 12.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 2.5f, 24.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 13.5f, 12.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 14.0f, 24.0f, BODY_FRONT_Z } )
    );

    // Hood for lower double section
    public static final List<Box> DOUBLE_LOWER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.5f, 11.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 13.5f, 12.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 2.0f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 2.5f, 12.0f, BODY_FRONT_Z } ),
            new Box( new float[]{ 13.5f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 14.0f, 12.0f, BODY_FRONT_Z } )
    );

    // Crate for upper double section
    public static final List<Box> DOUBLE_UPPER_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            2.0f, 12.0f, 14.0f, 24.0f, 0.4f );

    // Crate for lower double section
    public static final List<Box> DOUBLE_LOWER_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            2.0f, 0.0f, 14.0f, 12.0f, 0.4f );

    // ========================================================================================
    // HELPER: Generate crate-style visor
    // ========================================================================================

    /**
     * Creates a crate visor pattern using a dense grid of horizontal and vertical bars.
     * All bars are strictly clamped within [x1,y1] to [x2,y2]. The visor sits flush against
     * the body front face and protrudes forward.
     */
    private static List<Box> createCrateVisor( float x1, float y1, float x2, float y2,
            float barThickness ) {
        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();
        float faceZ = BODY_FRONT_Z;
        float frontZ = faceZ - 0.8f;

        float spacing = 2.0f;
        float halfBar = barThickness / 2.0f;

        // Horizontal bars
        for ( float yy = y1; yy <= y2 + 0.01f; yy += spacing ) {
            float barY1 = Math.max( yy - halfBar, y1 );
            float barY2 = Math.min( yy + halfBar, y2 );
            if ( barY2 - barY1 < 0.1f ) continue;
            boxes.add( new Box(
                    new float[]{ x1, barY1, frontZ },
                    new float[]{ x2, barY2, faceZ } ) );
        }

        // Vertical bars
        for ( float xx = x1; xx <= x2 + 0.01f; xx += spacing ) {
            float barX1 = Math.max( xx - halfBar, x1 );
            float barX2 = Math.min( xx + halfBar, x2 );
            if ( barX2 - barX1 < 0.1f ) continue;
            boxes.add( new Box(
                    new float[]{ barX1, y1, frontZ },
                    new float[]{ barX2, y2, faceZ } ) );
        }

        // Outer frame (slightly thicker)
        float frameThick = barThickness * 1.5f;
        boxes.add( new Box( new float[]{ x1, y2 - frameThick, frontZ },
                new float[]{ x2, y2, faceZ } ) );
        boxes.add( new Box( new float[]{ x1, y1, frontZ },
                new float[]{ x2, y1 + frameThick, faceZ } ) );
        boxes.add( new Box( new float[]{ x1, y1, frontZ },
                new float[]{ x1 + frameThick, y2, faceZ } ) );
        boxes.add( new Box( new float[]{ x2 - frameThick, y1, frontZ },
                new float[]{ x2, y2, faceZ } ) );

        return boxes;
    }

    /**
     * Returns the bracket vertex data for the given mount type and signal type.
     *
     * @param mountType the mount direction
     * @param isDouble  true for double-worded (taller) signal, false for single
     */
    public static List<Box> getBracketData( CrosswalkMountType mountType, boolean isDouble ) {
        if ( isDouble ) {
            switch ( mountType ) {
                case REAR:
                    return BRACKET_DOUBLE_REAR_VERTEX_DATA;
                case LEFT:
                    return BRACKET_DOUBLE_LEFT_VERTEX_DATA;
                case RIGHT:
                    return BRACKET_DOUBLE_RIGHT_VERTEX_DATA;
                case BASE:
                default:
                    return BRACKET_BASE_VERTEX_DATA;
            }
        }
        else {
            switch ( mountType ) {
                case REAR:
                    return BRACKET_SINGLE_REAR_VERTEX_DATA;
                case LEFT:
                    return BRACKET_SINGLE_LEFT_VERTEX_DATA;
                case RIGHT:
                    return BRACKET_SINGLE_RIGHT_VERTEX_DATA;
                case BASE:
                default:
                    return BRACKET_BASE_VERTEX_DATA;
            }
        }
    }
}
