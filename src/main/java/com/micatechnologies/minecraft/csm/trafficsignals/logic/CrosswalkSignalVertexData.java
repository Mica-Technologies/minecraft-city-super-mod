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

    // Mount arm Y positions — extended vertical shafts for visible pole stubs.
    private static final float SINGLE_LOWER_ARM_Y = -3.0f;
    private static final float SINGLE_UPPER_ARM_Y = 18.0f;
    private static final float DOUBLE_LOWER_ARM_Y = -3.0f;
    private static final float DOUBLE_UPPER_ARM_Y = 27.0f;

    // ========================================================================================
    // VISORS — SINGLE FACE (16x16 signal)
    // Visor sits flush against body front face (Z=BODY_FRONT_Z) and protrudes forward.
    // ========================================================================================

    public static final List<Box> SINGLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood style visor: U-shaped hood wrapping top and sides, open at bottom with ~20% gap.
    // Thin panels inset within housing bounds, protruding ~5 units forward.
    // Bottom gap: sides stop at Y=3.2 (20% of 16 = 3.2 units up from bottom)
    public static final List<Box> SINGLE_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ 0.5f, 15.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 15.5f, 16.0f, BODY_FRONT_Z } ),
            // Left side panel (stops 20% from bottom)
            new Box( new float[]{ 0.0f, 3.2f, BODY_FRONT_Z - 5.0f }, new float[]{ 0.5f, 16.0f, BODY_FRONT_Z } ),
            // Right side panel (stops 20% from bottom)
            new Box( new float[]{ 15.5f, 3.2f, BODY_FRONT_Z - 5.0f }, new float[]{ 16.0f, 16.0f, BODY_FRONT_Z } )
    );

    // Crate style visor
    public static final List<Box> SINGLE_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            0.0f, 0.0f, 16.0f, 16.0f, 0.5f );

    // ========================================================================================
    // VISORS — DOUBLE SECTIONS
    // ========================================================================================

    public static final List<Box> DOUBLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood for upper double section — 20% bottom gap (2.4 units of 12), with gap above lower.
    public static final List<Box> DOUBLE_UPPER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ 2.5f, 23.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 13.5f, 24.0f, BODY_FRONT_Z } ),
            // Left side (stops 20% from section bottom = Y=14.4)
            new Box( new float[]{ 2.0f, 14.4f, BODY_FRONT_Z - 5.0f }, new float[]{ 2.5f, 24.0f, BODY_FRONT_Z } ),
            // Right side
            new Box( new float[]{ 13.5f, 14.4f, BODY_FRONT_Z - 5.0f }, new float[]{ 14.0f, 24.0f, BODY_FRONT_Z } )
    );

    // Hood for lower double section — 20% bottom gap (2.4 units of 12)
    public static final List<Box> DOUBLE_LOWER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ 2.5f, 11.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 13.5f, 12.0f, BODY_FRONT_Z } ),
            // Left side (stops at Y=2.4)
            new Box( new float[]{ 2.0f, 2.4f, BODY_FRONT_Z - 5.0f }, new float[]{ 2.5f, 12.0f, BODY_FRONT_Z } ),
            // Right side
            new Box( new float[]{ 13.5f, 2.4f, BODY_FRONT_Z - 5.0f }, new float[]{ 14.0f, 12.0f, BODY_FRONT_Z } )
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
     * Creates a diamond lattice crate visor pattern. Uses a staggered grid of small node
     * boxes at diamond intersection points, connected by thin horizontal and vertical bars
     * between them. This creates a diamond-like mesh pattern that approximates a real
     * crosswalk signal crate visor using only axis-aligned boxes.
     *
     * <p>The pattern alternates row offsets: even rows have nodes at 0, spacing, 2*spacing...
     * and odd rows have nodes at spacing/2, 3*spacing/2... creating the diamond layout.
     */
    private static List<Box> createCrateVisor( float x1, float y1, float x2, float y2,
            float barThickness ) {
        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();
        float faceZ = BODY_FRONT_Z;
        float frontZ = faceZ - 0.8f;

        // Diamond cell spacing — controls mesh density
        float spacing = 2.0f;
        float halfSpacing = spacing / 2.0f;
        float nodeSize = barThickness * 1.2f; // slightly larger than bar for visible nodes
        float halfNode = nodeSize / 2.0f;
        float halfBar = barThickness / 2.0f;

        // Generate diamond lattice nodes and connecting bars
        int row = 0;
        for ( float yy = y1; yy <= y2 + 0.01f; yy += halfSpacing ) {
            boolean offsetRow = ( row % 2 ) != 0;
            float startX = offsetRow ? x1 + halfSpacing : x1;

            for ( float xx = startX; xx <= x2 + 0.01f; xx += spacing ) {
                // Node box at intersection point
                float nx1 = Math.max( xx - halfNode, x1 );
                float ny1 = Math.max( yy - halfNode, y1 );
                float nx2 = Math.min( xx + halfNode, x2 );
                float ny2 = Math.min( yy + halfNode, y2 );
                if ( nx2 - nx1 > 0.1f && ny2 - ny1 > 0.1f ) {
                    boxes.add( new Box(
                            new float[]{ nx1, ny1, frontZ },
                            new float[]{ nx2, ny2, faceZ } ) );
                }

                // Horizontal bar to next node in this row
                float nextX = xx + spacing;
                if ( nextX <= x2 + 0.01f ) {
                    float bx1 = Math.min( xx + halfNode, x2 );
                    float bx2 = Math.max( Math.min( nextX - halfNode, x2 ), bx1 );
                    float by1 = Math.max( yy - halfBar, y1 );
                    float by2 = Math.min( yy + halfBar, y2 );
                    if ( bx2 - bx1 > 0.1f && by2 - by1 > 0.1f ) {
                        boxes.add( new Box(
                                new float[]{ bx1, by1, frontZ },
                                new float[]{ bx2, by2, faceZ } ) );
                    }
                }

                // Vertical bar down to next row's offset node (diagonal approximation)
                float nextY = yy + halfSpacing;
                if ( nextY <= y2 + 0.01f ) {
                    float vx1 = Math.max( xx - halfBar, x1 );
                    float vx2 = Math.min( xx + halfBar, x2 );
                    float vy1 = Math.min( yy + halfNode, y2 );
                    float vy2 = Math.max( Math.min( nextY - halfNode, y2 ), vy1 );
                    if ( vx2 - vx1 > 0.1f && vy2 - vy1 > 0.1f ) {
                        boxes.add( new Box(
                                new float[]{ vx1, vy1, frontZ },
                                new float[]{ vx2, vy2, faceZ } ) );
                    }
                }
            }
            row++;
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
     * Generates bracket vertex data dynamically to account for body tilt. The bracket has
     * three parts:
     * <ol>
     *   <li>Vertical stubs — straight pipes above/below the housing, positioned at the
     *       tilted housing location (shifted by tiltOffset in X)</li>
     *   <li>Horizontal arms — angled/extended from the stub ends to the stationary pole
     *       mount point. The pole end stays fixed; the housing end follows the tilt. For
     *       rear mounts this creates a diagonal arm in the X-Z plane. For left/right mounts
     *       the arm simply gets longer or shorter.</li>
     * </ol>
     *
     * @param mountType  the mount direction
     * @param isDouble   true for double-worded (taller) signal, false for single
     * @param tiltOffset the X offset applied by the tilt (0, ±2, ±4 model units)
     */
    public static List<Box> getBracketData( CrosswalkMountType mountType, boolean isDouble,
            int tiltOffset ) {
        if ( mountType == CrosswalkMountType.BASE ) {
            return Collections.emptyList();
        }

        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();

        float lowerArmY = isDouble ? DOUBLE_LOWER_ARM_Y : SINGLE_LOWER_ARM_Y;
        float upperArmY = isDouble ? DOUBLE_UPPER_ARM_Y : SINGLE_UPPER_ARM_Y;
        float bodyBottom = 0.0f;
        float bodyTop = isDouble ? 24.0f : 16.0f;

        // Housing-side X positions (shifted by tilt)
        float hx1 = MOUNT_X1 + tiltOffset;
        float hx2 = MOUNT_X2 + tiltOffset;

        // --- Vertical stubs: straight, at the housing position ---
        // Lower stub (below housing down to arm Y)
        boxes.add( new Box(
                new float[]{ hx1, lowerArmY, MOUNT_Z1 },
                new float[]{ hx2, bodyBottom, MOUNT_Z2 } ) );
        // Upper stub (above housing up to arm Y)
        boxes.add( new Box(
                new float[]{ hx1, bodyTop, MOUNT_Z1 },
                new float[]{ hx2, upperArmY, MOUNT_Z2 } ) );

        // --- Horizontal arms: pivot from pole end, angle to meet housing stubs ---
        // Pole-side X stays at MOUNT_X1/X2 (stationary). Housing-side X is hx1/hx2.
        switch ( mountType ) {
            case REAR:
                // Arm extends along Z from housing (Z=MOUNT_Z2) to pole (Z=22).
                // When tilted, arm diagonals in X from hx1..hx2 at Z=MOUNT_Z2 to
                // MOUNT_X1..X2 at Z=22. Build as stepped boxes.
                addAngledArm( boxes, hx1, hx2, MOUNT_X1, MOUNT_X2,
                        lowerArmY, lowerArmY + 1.0f, MOUNT_Z2, 22.0f );
                addAngledArm( boxes, hx1, hx2, MOUNT_X1, MOUNT_X2,
                        upperArmY - 1.0f, upperArmY, MOUNT_Z2, 22.0f );
                break;
            case LEFT:
                // Arm extends along X from housing (X=hx2) to pole (X=20).
                // Just a straight arm from the shifted position to the pole.
                boxes.add( new Box(
                        new float[]{ hx2, lowerArmY, MOUNT_Z1 },
                        new float[]{ 20.0f, lowerArmY + 1.0f, MOUNT_Z2 } ) );
                boxes.add( new Box(
                        new float[]{ hx2, upperArmY - 1.0f, MOUNT_Z1 },
                        new float[]{ 20.0f, upperArmY, MOUNT_Z2 } ) );
                break;
            case RIGHT:
                // Arm extends along X from pole (X=-4) to housing (X=hx1).
                boxes.add( new Box(
                        new float[]{ -4.0f, lowerArmY, MOUNT_Z1 },
                        new float[]{ hx1, lowerArmY + 1.0f, MOUNT_Z2 } ) );
                boxes.add( new Box(
                        new float[]{ -4.0f, upperArmY - 1.0f, MOUNT_Z1 },
                        new float[]{ hx1, upperArmY, MOUNT_Z2 } ) );
                break;
        }

        return boxes;
    }

    /**
     * Builds an angled horizontal arm as stepped boxes. The arm goes from
     * (startX1..startX2, startZ) to (endX1..endX2, endZ), stepping in X proportionally
     * across the Z distance. Used for rear mount arms that need to diagonal when tilted.
     */
    private static void addAngledArm( java.util.ArrayList<Box> boxes,
            float startX1, float startX2, float endX1, float endX2,
            float y1, float y2, float startZ, float endZ ) {
        float xShift = endX1 - startX1;

        if ( Math.abs( xShift ) < 0.1f ) {
            // No angle needed — straight arm
            boxes.add( new Box(
                    new float[]{ Math.min( startX1, endX1 ), y1, startZ },
                    new float[]{ Math.max( startX2, endX2 ), y2, endZ } ) );
            return;
        }

        // Step along Z, shifting X proportionally
        float zLength = endZ - startZ;
        int steps = Math.max( 2, Math.round( Math.abs( xShift ) / 1.0f ) );
        float stepZ = zLength / steps;
        float stepX = xShift / steps;

        for ( int i = 0; i < steps; i++ ) {
            float sz = startZ + i * stepZ;
            float ez = startZ + ( i + 1 ) * stepZ;
            float sx1 = startX1 + i * stepX;
            float sx2 = startX2 + i * stepX;
            float ex1 = startX1 + ( i + 1 ) * stepX;
            float ex2 = startX2 + ( i + 1 ) * stepX;

            boxes.add( new Box(
                    new float[]{ Math.min( sx1, ex1 ), y1, sz },
                    new float[]{ Math.max( sx2, ex2 ), y2, ez } ) );
        }
    }
}
