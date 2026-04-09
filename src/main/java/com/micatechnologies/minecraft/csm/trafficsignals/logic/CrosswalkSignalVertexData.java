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
    // SINGLE-FACE BODY (16x18 inch crosswalk signal — wider than tall)
    // Real crosswalk signals are 16" tall x 18" wide. In model units: 16 tall, 18 wide.
    // Centered in the block: X = -1 to 17 (18 units), Y = 0 to 16 (16 units)
    // ========================================================================================

    public static final float SINGLE_WIDTH = 18.0f;
    public static final float SINGLE_X_MIN = -1.0f;
    public static final float SINGLE_X_MAX = 17.0f;

    public static final List<Box> SINGLE_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ SINGLE_X_MIN, 0.0f, BODY_FRONT_Z },
                    new float[]{ SINGLE_X_MAX, 16.0f, BODY_BACK_Z } )
    );

    // Display face position (just in front of body face)
    public static final float SINGLE_DISPLAY_FACE_Z = BODY_FRONT_Z - 0.05f;
    public static final float SINGLE_DISPLAY_X1 = SINGLE_X_MIN + 0.5f;
    public static final float SINGLE_DISPLAY_Y1 = 0.5f;
    public static final float SINGLE_DISPLAY_X2 = SINGLE_X_MAX - 0.5f;
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
    // SINGLE 12-INCH BODY (single section, same 12-inch housing as double but at Y=0-12)
    // Uses the same tapered body style and narrower X range (2-14) as the double sections.
    // ========================================================================================

    public static final List<Box> SINGLE_12INCH_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.0f, 0.0f, BODY_FRONT_Z }, new float[]{ 14.0f, 12.0f, BODY_FRONT_Z + 1.0f } ),
            new Box( new float[]{ 2.2f, 0.0f, BODY_FRONT_Z + 1.0f }, new float[]{ 13.8f, 12.0f, BODY_FRONT_Z + 2.0f } ),
            new Box( new float[]{ 2.4f, 0.0f, BODY_FRONT_Z + 2.0f }, new float[]{ 13.6f, 12.0f, BODY_FRONT_Z + 3.0f } ),
            new Box( new float[]{ 2.6f, 0.0f, BODY_FRONT_Z + 3.0f }, new float[]{ 13.4f, 12.0f, BODY_FRONT_Z + 4.0f } ),
            new Box( new float[]{ 2.8f, 0.0f, BODY_FRONT_Z + 4.0f }, new float[]{ 13.2f, 12.0f, BODY_FRONT_Z + 5.0f } ),
            new Box( new float[]{ 3.0f, 0.0f, BODY_FRONT_Z + 5.0f }, new float[]{ 13.0f, 12.0f, BODY_FRONT_Z + 6.0f } )
    );

    // Display face position for single 12-inch
    public static final float SINGLE_12INCH_DISPLAY_FACE_Z = BODY_FRONT_Z - 0.05f;
    public static final float SINGLE_12INCH_DISPLAY_X1 = 2.5f;
    public static final float SINGLE_12INCH_DISPLAY_Y1 = 0.5f;
    public static final float SINGLE_12INCH_DISPLAY_X2 = 13.5f;
    public static final float SINGLE_12INCH_DISPLAY_Y2 = 11.5f;

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
    private static final float SINGLE_12INCH_LOWER_ARM_Y = -3.0f;
    private static final float SINGLE_12INCH_UPPER_ARM_Y = 15.0f;
    private static final float DOUBLE_LOWER_ARM_Y = -3.0f;
    private static final float DOUBLE_UPPER_ARM_Y = 27.0f;

    // ========================================================================================
    // VISORS — SINGLE FACE (16x16 signal)
    // Visor sits flush against body front face (Z=BODY_FRONT_Z) and protrudes forward.
    // ========================================================================================

    public static final List<Box> SINGLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood style visor: U-shaped hood wrapping top and sides, extending to bottom.
    // Matches wider 18-unit body. No bottom gap on the 16-inch single signal.
    public static final List<Box> SINGLE_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ SINGLE_X_MIN + 0.5f, 15.5f, BODY_FRONT_Z - 5.0f },
                    new float[]{ SINGLE_X_MAX - 0.5f, 16.0f, BODY_FRONT_Z } ),
            // Left side panel (extends to bottom)
            new Box( new float[]{ SINGLE_X_MIN, 0.0f, BODY_FRONT_Z - 5.0f },
                    new float[]{ SINGLE_X_MIN + 0.5f, 16.0f, BODY_FRONT_Z } ),
            // Right side panel (extends to bottom)
            new Box( new float[]{ SINGLE_X_MAX - 0.5f, 0.0f, BODY_FRONT_Z - 5.0f },
                    new float[]{ SINGLE_X_MAX, 16.0f, BODY_FRONT_Z } )
    );

    // Crate style visor (matches wider 18-unit body)
    public static final List<Box> SINGLE_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            SINGLE_X_MIN, 0.0f, SINGLE_X_MAX, 16.0f, 0.5f );

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
    // VISORS — SINGLE 12-INCH (same geometry as double lower section visors)
    // ========================================================================================

    public static final List<Box> SINGLE_12INCH_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood for single 12-inch — no bottom gap (standalone section, extends to bottom)
    public static final List<Box> SINGLE_12INCH_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ 2.5f, 11.5f, BODY_FRONT_Z - 5.0f }, new float[]{ 13.5f, 12.0f, BODY_FRONT_Z } ),
            // Left side (extends to bottom)
            new Box( new float[]{ 2.0f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 2.5f, 12.0f, BODY_FRONT_Z } ),
            // Right side (extends to bottom)
            new Box( new float[]{ 13.5f, 0.0f, BODY_FRONT_Z - 5.0f }, new float[]{ 14.0f, 12.0f, BODY_FRONT_Z } )
    );

    // Crate for single 12-inch
    public static final List<Box> SINGLE_12INCH_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            2.0f, 0.0f, 14.0f, 12.0f, 0.4f );

    // ========================================================================================
    // HELPER: Generate crate-style visor
    // ========================================================================================

    /**
     * Creates a diamond lattice crate visor using diagonal lines approximated by small
     * stepped boxes. Two sets of parallel diagonal lines (/ and \) cross to form a diamond
     * mesh, matching the real-world crate visor pattern visible in reference images.
     *
     * <p>Each diagonal "line" is a series of small square boxes placed along the diagonal
     * path in a staircase pattern. At Minecraft's scale, the steps blend into a convincing
     * diagonal line.
     */
    private static List<Box> createCrateVisor( float x1, float y1, float x2, float y2,
            float barThickness ) {
        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();
        float faceZ = BODY_FRONT_Z;
        float frontZ = faceZ - 0.8f;

        float width = x2 - x1;
        float height = y2 - y1;

        // Diagonal line spacing — tighter for denser diamond mesh
        float diagSpacing = 1.8f;
        // Step size for each box along the diagonal — smaller = smoother lines
        float step = 0.5f;
        float halfBar = barThickness / 2.0f;

        // Generate / diagonals (lower-left to upper-right)
        // Each line: y = x + c, where c is the y-intercept offset
        float maxOffset = width + height;
        for ( float c = -maxOffset; c <= maxOffset; c += diagSpacing ) {
            // Walk along this diagonal
            for ( float t = 0; t <= maxOffset; t += step ) {
                float px = x1 + t;
                float py = y1 + t + c;
                // Clamp to face bounds
                if ( px - halfBar >= x2 || px + halfBar <= x1 ) continue;
                if ( py - halfBar >= y2 || py + halfBar <= y1 ) continue;

                float bx1 = Math.max( px - halfBar, x1 );
                float by1 = Math.max( py - halfBar, y1 );
                float bx2 = Math.min( px + halfBar, x2 );
                float by2 = Math.min( py + halfBar, y2 );

                boxes.add( new Box(
                        new float[]{ bx1, by1, frontZ },
                        new float[]{ bx2, by2, faceZ } ) );
            }
        }

        // Generate \ diagonals (upper-left to lower-right)
        // Each line: y = -x + c
        for ( float c = -maxOffset; c <= maxOffset; c += diagSpacing ) {
            for ( float t = 0; t <= maxOffset; t += step ) {
                float px = x1 + t;
                float py = y2 - t + c;
                if ( px - halfBar >= x2 || px + halfBar <= x1 ) continue;
                if ( py - halfBar >= y2 || py + halfBar <= y1 ) continue;

                float bx1 = Math.max( px - halfBar, x1 );
                float by1 = Math.max( py - halfBar, y1 );
                float bx2 = Math.min( px + halfBar, x2 );
                float by2 = Math.min( py + halfBar, y2 );

                boxes.add( new Box(
                        new float[]{ bx1, by1, frontZ },
                        new float[]{ bx2, by2, faceZ } ) );
            }
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
     * Returns the vertical stub geometry. Stubs are straight vertical pipes above/below the
     * housing at the standard mount pipe position (X=7-9, Z=7-9). These are rendered in the
     * TILTED context so they rotate with the housing and always line up perfectly.
     */
    public static List<Box> getStubData( CrosswalkMountType mountType, boolean isDouble ) {
        return getStubData( mountType, isDouble ? CrosswalkDisplayType.TEXT
                : CrosswalkDisplayType.SYMBOL );
    }

    /**
     * Returns the vertical stub geometry for a given display type.
     */
    public static List<Box> getStubData( CrosswalkMountType mountType,
            CrosswalkDisplayType displayType ) {
        if ( mountType == CrosswalkMountType.BASE ) {
            return Collections.emptyList();
        }

        float lowerArmY, upperArmY, bodyTop;
        switch ( displayType ) {
            case TEXT:
                lowerArmY = DOUBLE_LOWER_ARM_Y;
                upperArmY = DOUBLE_UPPER_ARM_Y;
                bodyTop = 24.0f;
                break;
            case SYMBOL_12INCH:
                lowerArmY = SINGLE_12INCH_LOWER_ARM_Y;
                upperArmY = SINGLE_12INCH_UPPER_ARM_Y;
                bodyTop = 12.0f;
                break;
            case SYMBOL:
            default:
                lowerArmY = SINGLE_LOWER_ARM_Y;
                upperArmY = SINGLE_UPPER_ARM_Y;
                bodyTop = 16.0f;
                break;
        }
        float bodyBottom = 0.0f;

        return Arrays.asList(
                // Lower stub (below housing down to arm Y)
                new Box( new float[]{ MOUNT_X1, lowerArmY, MOUNT_Z1 },
                        new float[]{ MOUNT_X2, bodyBottom, MOUNT_Z2 } ),
                // Upper stub (above housing up to arm Y)
                new Box( new float[]{ MOUNT_X1, bodyTop, MOUNT_Z1 },
                        new float[]{ MOUNT_X2, upperArmY, MOUNT_Z2 } )
        );
    }

    /**
     * Returns the horizontal arm geometry. Arms extend from the stub ends to the pole mount
     * point. Rendered in the BASE facing context so the pole-side stays stationary.
     *
     * <p>The housing-side endpoint is computed by transforming the stub's model-space position
     * through the tilted context's rotation, then inverse-transforming through the base
     * context's rotation, to get the correct (X,Z) in base model space where the arm must
     * connect.
     *
     * @param mountType     the mount direction
     * @param isDouble      true for double-worded signal
     * @param tiltOffset    the X offset applied by the tilt (0, ±2, ±4)
     * @param tiltedAngleDeg the full rotation angle of the tilted context (degrees)
     * @param baseAngleDeg  the rotation angle of the base context (degrees)
     */
    public static List<Box> getArmData( CrosswalkMountType mountType, boolean isDouble,
            int tiltOffset, float tiltedAngleDeg, float baseAngleDeg ) {
        return getArmData( mountType,
                isDouble ? CrosswalkDisplayType.TEXT : CrosswalkDisplayType.SYMBOL,
                tiltOffset, tiltedAngleDeg, baseAngleDeg );
    }

    /**
     * Returns the horizontal arm geometry for a given display type.
     */
    public static List<Box> getArmData( CrosswalkMountType mountType,
            CrosswalkDisplayType displayType, int tiltOffset,
            float tiltedAngleDeg, float baseAngleDeg ) {
        if ( mountType == CrosswalkMountType.BASE ) {
            return Collections.emptyList();
        }

        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();

        float lowerArmY, upperArmY;
        switch ( displayType ) {
            case TEXT:
                lowerArmY = DOUBLE_LOWER_ARM_Y;
                upperArmY = DOUBLE_UPPER_ARM_Y;
                break;
            case SYMBOL_12INCH:
                lowerArmY = SINGLE_12INCH_LOWER_ARM_Y;
                upperArmY = SINGLE_12INCH_UPPER_ARM_Y;
                break;
            case SYMBOL:
            default:
                lowerArmY = SINGLE_LOWER_ARM_Y;
                upperArmY = SINGLE_UPPER_ARM_Y;
                break;
        }

        // Compute where the stub center (8, 8 in model space) ends up in base-context
        // model space after the tilted transform.
        float[] stubBasePos = transformTiltedToBase(
                MOUNT_X1 + 1.0f, MOUNT_Z1 + 1.0f, // stub center (8, 8) in model space
                tiltOffset, tiltedAngleDeg, baseAngleDeg );
        float hCenterX = stubBasePos[0];
        float hCenterZ = stubBasePos[1];
        float hx1 = hCenterX - 1.0f;
        float hx2 = hCenterX + 1.0f;
        float hz1 = hCenterZ - 1.0f;
        float hz2 = hCenterZ + 1.0f;

        switch ( mountType ) {
            case REAR:
                // Arm from stub (hx, hz) to pole (MOUNT_X, Z=22)
                addAngledArm2D( boxes, hx1, hx2, hz2, MOUNT_X1, MOUNT_X2, 22.0f,
                        lowerArmY, lowerArmY + 1.0f );
                addAngledArm2D( boxes, hx1, hx2, hz2, MOUNT_X1, MOUNT_X2, 22.0f,
                        upperArmY - 1.0f, upperArmY );
                break;
            case LEFT:
                // Arm from stub (hx2, hz) to pole (X=20, MOUNT_Z center)
                addAngledArm2D( boxes, hx2, hx2 + 0.01f, hz1,
                        19.0f, 20.0f, MOUNT_Z1 + 1.0f,
                        lowerArmY, lowerArmY + 1.0f );
                addAngledArm2D( boxes, hx2, hx2 + 0.01f, hz1,
                        19.0f, 20.0f, MOUNT_Z1 + 1.0f,
                        upperArmY - 1.0f, upperArmY );
                break;
            case RIGHT:
                // Arm from pole (X=-4, MOUNT_Z center) to stub (hx1, hz)
                addAngledArm2D( boxes, -4.0f, -3.0f, MOUNT_Z1 + 1.0f,
                        hx1 - 0.01f, hx1, hz1,
                        lowerArmY, lowerArmY + 1.0f );
                addAngledArm2D( boxes, -4.0f, -3.0f, MOUNT_Z1 + 1.0f,
                        hx1 - 0.01f, hx1, hz1,
                        upperArmY - 1.0f, upperArmY );
                break;
        }

        return boxes;
    }

    /**
     * Transforms a point from tilted-context model space to base-context model space.
     * Both contexts share the same world space but apply different rotations around (8,8).
     *
     * <p>Tilted context: translate(tiltOffset,0) → translate(-8,-8) → rotate(tiltAngle) → translate(8,8)
     * <p>Base context: translate(-8,-8) → rotate(baseAngle) → translate(8,8)
     *
     * <p>Returns the [x, z] position in base-context model space.
     */
    private static float[] transformTiltedToBase( float modelX, float modelZ,
            int tiltOffset, float tiltedAngleDeg, float baseAngleDeg ) {
        double tiltRad = Math.toRadians( tiltedAngleDeg );
        double baseRad = Math.toRadians( baseAngleDeg );

        // Apply tilted context transform to get world position
        // Step 1: apply tiltOffset
        float px = modelX + tiltOffset;
        float pz = modelZ;
        // Step 2: translate to pivot (-8, -8)
        px -= 8.0f;
        pz -= 8.0f;
        // Step 3: rotate by tiltedAngle
        float wx = (float) ( px * Math.cos( tiltRad ) + pz * Math.sin( tiltRad ) );
        float wz = (float) ( -px * Math.sin( tiltRad ) + pz * Math.cos( tiltRad ) );
        // Step 4: translate back (+8, +8)
        wx += 8.0f;
        wz += 8.0f;

        // Now inverse-transform from world to base-context model space
        // Inverse of base context: translate(-8,-8) → rotate(-baseAngle) → translate(8,8)
        wx -= 8.0f;
        wz -= 8.0f;
        float bx = (float) ( wx * Math.cos( -baseRad ) + wz * Math.sin( -baseRad ) );
        float bz = (float) ( -wx * Math.sin( -baseRad ) + wz * Math.cos( -baseRad ) );
        bx += 8.0f;
        bz += 8.0f;

        return new float[]{ bx, bz };
    }

    /**
     * Builds an arm as stepped boxes that goes from (startX1..startX2, startZ) to
     * (endX1..endX2, endZ) in the X-Z plane, stepping diagonally. Handles any combination
     * of X and Z shifts.
     */
    /**
     * Minimum arm cross-section half-width. Ensures arms always have visible thickness
     * in both X and Z, even when the arm runs primarily along one axis.
     */
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

            // Each step box has minimum ARM_HALF_THICKNESS in both X and Z
            boxes.add( new Box(
                    new float[]{ Math.min( cx0, cx1 ) - ARM_HALF_THICKNESS, y1,
                            Math.min( cz0, cz1 ) - ARM_HALF_THICKNESS },
                    new float[]{ Math.max( cx0, cx1 ) + ARM_HALF_THICKNESS, y2,
                            Math.max( cz0, cz1 ) + ARM_HALF_THICKNESS } ) );
        }
    }
}
