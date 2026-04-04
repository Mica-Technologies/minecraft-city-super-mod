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
 */
public class CrosswalkSignalVertexData {

    // ========================================================================================
    // SINGLE-FACE BODY (16-inch crosswalk signal)
    // Based on crosswalklightsingle_base.json: single element [0,1,1] to [16,17,9]
    // In TESR model space (centered at 8,8,8): body from X=0-16, Y=1-17, Z=1-9
    // We use the same tapered-back pattern as signal heads for visual consistency.
    // Front face (display) is at Z=1, back tapers from Z=9.
    // ========================================================================================

    public static final List<Box> SINGLE_BODY_VERTEX_DATA = Arrays.asList(
            // Main body shell (front to back, slight taper)
            new Box( new float[]{ 0.0f, 0.0f, 1.0f }, new float[]{ 16.0f, 16.0f, 2.0f } ),
            new Box( new float[]{ 0.2f, 0.0f, 2.0f }, new float[]{ 15.8f, 16.0f, 3.0f } ),
            new Box( new float[]{ 0.4f, 0.0f, 3.0f }, new float[]{ 15.6f, 16.0f, 4.0f } ),
            new Box( new float[]{ 0.6f, 0.0f, 4.0f }, new float[]{ 15.4f, 16.0f, 5.0f } ),
            new Box( new float[]{ 0.8f, 0.0f, 5.0f }, new float[]{ 15.2f, 16.0f, 6.0f } ),
            new Box( new float[]{ 1.0f, 0.0f, 6.0f }, new float[]{ 15.0f, 16.0f, 7.0f } ),
            new Box( new float[]{ 1.2f, 0.0f, 7.0f }, new float[]{ 14.8f, 16.0f, 8.0f } ),
            new Box( new float[]{ 1.4f, 0.0f, 8.0f }, new float[]{ 14.6f, 16.0f, 9.0f } )
    );

    // Display face Z position for the single body (front face)
    public static final float SINGLE_DISPLAY_FACE_Z = 0.95f;
    // Display face extends from [0,0] to [16,16] in X,Y within the body
    public static final float SINGLE_DISPLAY_X1 = 0.5f;
    public static final float SINGLE_DISPLAY_Y1 = 0.5f;
    public static final float SINGLE_DISPLAY_X2 = 15.5f;
    public static final float SINGLE_DISPLAY_Y2 = 15.5f;

    // ========================================================================================
    // DOUBLE-WORDED BODY (two stacked 12-inch sections)
    // Based on crosswalklightdouble_base.json: two sections, each ~12 units tall
    // Upper section (DON'T WALK): Y=13-25, Lower section (WALK): Y=1-13
    // Both sections have tapered backs, narrower than single (X=2-14)
    // In TESR space we shift so the assembly is centered. Upper section body + lower section body.
    // ========================================================================================

    // Upper section body (DON'T WALK) — Y offset from 12 to 24 in model space
    public static final List<Box> DOUBLE_UPPER_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.0f, 12.0f, 1.0f }, new float[]{ 14.0f, 24.0f, 2.0f } ),
            new Box( new float[]{ 2.2f, 12.0f, 2.0f }, new float[]{ 13.8f, 24.0f, 3.0f } ),
            new Box( new float[]{ 2.4f, 12.0f, 3.0f }, new float[]{ 13.6f, 24.0f, 4.0f } ),
            new Box( new float[]{ 2.6f, 12.0f, 4.0f }, new float[]{ 13.4f, 24.0f, 5.0f } ),
            new Box( new float[]{ 2.8f, 12.0f, 5.0f }, new float[]{ 13.2f, 24.0f, 6.0f } ),
            new Box( new float[]{ 3.0f, 12.0f, 6.0f }, new float[]{ 13.0f, 24.0f, 7.0f } )
    );

    // Lower section body (WALK) — Y offset from 0 to 12 in model space
    public static final List<Box> DOUBLE_LOWER_BODY_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 2.0f, 0.0f, 1.0f }, new float[]{ 14.0f, 12.0f, 2.0f } ),
            new Box( new float[]{ 2.2f, 0.0f, 2.0f }, new float[]{ 13.8f, 12.0f, 3.0f } ),
            new Box( new float[]{ 2.4f, 0.0f, 3.0f }, new float[]{ 13.6f, 12.0f, 4.0f } ),
            new Box( new float[]{ 2.6f, 0.0f, 4.0f }, new float[]{ 13.4f, 12.0f, 5.0f } ),
            new Box( new float[]{ 2.8f, 0.0f, 5.0f }, new float[]{ 13.2f, 12.0f, 6.0f } ),
            new Box( new float[]{ 3.0f, 0.0f, 6.0f }, new float[]{ 13.0f, 12.0f, 7.0f } )
    );

    // Display face positions for double sections
    public static final float DOUBLE_DISPLAY_FACE_Z = 0.95f;
    public static final float DOUBLE_DISPLAY_X1 = 2.5f;
    public static final float DOUBLE_DISPLAY_X2 = 13.5f;
    // Upper section display Y range
    public static final float DOUBLE_UPPER_DISPLAY_Y1 = 12.5f;
    public static final float DOUBLE_UPPER_DISPLAY_Y2 = 23.5f;
    // Lower section display Y range
    public static final float DOUBLE_LOWER_DISPLAY_Y1 = 0.5f;
    public static final float DOUBLE_LOWER_DISPLAY_Y2 = 11.5f;

    // ========================================================================================
    // MOUNT BRACKETS
    // Extracted from the JSON mount models. Brackets are pairs of thin connector arms.
    // ========================================================================================

    // Rear mount: two arms extending backward along +Z axis
    public static final List<Box> BRACKET_REAR_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 7.0f, 0.0f, 9.0f }, new float[]{ 9.0f, 1.0f, 22.0f } ),
            new Box( new float[]{ 7.0f, 15.0f, 9.0f }, new float[]{ 9.0f, 16.0f, 22.0f } )
    );

    // Left mount: two arms extending to the left (+X direction past block)
    public static final List<Box> BRACKET_LEFT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 16.0f, 0.0f, 7.0f }, new float[]{ 20.0f, 1.0f, 9.0f } ),
            new Box( new float[]{ 16.0f, 15.0f, 7.0f }, new float[]{ 20.0f, 16.0f, 9.0f } )
    );

    // Right mount: two arms extending to the right (-X direction past block)
    public static final List<Box> BRACKET_RIGHT_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ -4.0f, 0.0f, 7.0f }, new float[]{ 0.0f, 1.0f, 9.0f } ),
            new Box( new float[]{ -4.0f, 15.0f, 7.0f }, new float[]{ 0.0f, 16.0f, 9.0f } )
    );

    // Base mount: no bracket (signal is flush-mounted)
    public static final List<Box> BRACKET_BASE_VERTEX_DATA = Collections.emptyList();

    // ========================================================================================
    // VISORS — SINGLE FACE (16x16 signal)
    // ========================================================================================

    // No visor
    public static final List<Box> SINGLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood style visor: U-shaped hood wrapping top and sides, open at bottom.
    // Protruding ~5 model units forward from the display face.
    public static final List<Box> SINGLE_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            // Top panel
            new Box( new float[]{ -0.5f, 16.0f, -4.0f }, new float[]{ 16.5f, 17.0f, 1.0f } ),
            // Left side panel
            new Box( new float[]{ -0.5f, -1.0f, -4.0f }, new float[]{ 0.0f, 17.0f, 1.0f } ),
            // Right side panel
            new Box( new float[]{ 16.0f, -1.0f, -4.0f }, new float[]{ 16.5f, 17.0f, 1.0f } )
    );

    // Crate style visor: diamond/cross-hatch grid covering the display face.
    // Thin diagonal bars forming a lattice pattern, protruding ~1 unit forward.
    // Modeled as a grid of thin vertical and horizontal bars creating the diamond pattern.
    public static final List<Box> SINGLE_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            0.0f, 0.0f, 16.0f, 16.0f, 0.5f );

    // ========================================================================================
    // VISORS — DOUBLE SECTIONS (12-unit wide, for each section independently)
    // These are rendered at each section's Y position.
    // ========================================================================================

    public static final List<Box> DOUBLE_VISOR_NONE_VERTEX_DATA = Collections.emptyList();

    // Hood for upper double section
    public static final List<Box> DOUBLE_UPPER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 1.5f, 24.0f, -4.0f }, new float[]{ 14.5f, 25.0f, 1.0f } ),
            new Box( new float[]{ 1.5f, 11.0f, -4.0f }, new float[]{ 2.0f, 25.0f, 1.0f } ),
            new Box( new float[]{ 14.0f, 11.0f, -4.0f }, new float[]{ 14.5f, 25.0f, 1.0f } )
    );

    // Hood for lower double section
    public static final List<Box> DOUBLE_LOWER_VISOR_HOOD_VERTEX_DATA = Arrays.asList(
            new Box( new float[]{ 1.5f, 12.0f, -4.0f }, new float[]{ 14.5f, 13.0f, 1.0f } ),
            new Box( new float[]{ 1.5f, -1.0f, -4.0f }, new float[]{ 2.0f, 13.0f, 1.0f } ),
            new Box( new float[]{ 14.0f, -1.0f, -4.0f }, new float[]{ 14.5f, 13.0f, 1.0f } )
    );

    // Crate for upper double section
    public static final List<Box> DOUBLE_UPPER_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            2.0f, 12.0f, 14.0f, 24.0f, 0.4f );

    // Crate for lower double section
    public static final List<Box> DOUBLE_LOWER_VISOR_CRATE_VERTEX_DATA = createCrateVisor(
            2.0f, 0.0f, 14.0f, 12.0f, 0.4f );

    // ========================================================================================
    // HELPER: Generate crate-style diamond grid visor
    // ========================================================================================

    /**
     * Creates a diamond/cross-hatch visor pattern as a grid of thin bars at 45-degree angles.
     * The real-world crate visor has diagonal bars forming a diamond mesh over the signal face.
     * We approximate this with horizontal and vertical bars forming a regular grid, plus
     * diagonal reinforcements at the edges.
     */
    private static List<Box> createCrateVisor( float x1, float y1, float x2, float y2,
            float barThickness ) {
        java.util.ArrayList<Box> boxes = new java.util.ArrayList<>();
        float faceZ = 0.5f; // just in front of the display face
        float depth = barThickness;

        float width = x2 - x1;
        float height = y2 - y1;

        // Horizontal bars
        int hBars = 5;
        for ( int i = 0; i <= hBars; i++ ) {
            float yPos = y1 + ( height * i / hBars ) - barThickness / 2;
            boxes.add( new Box(
                    new float[]{ x1, yPos, faceZ - depth },
                    new float[]{ x2, yPos + barThickness, faceZ } ) );
        }

        // Vertical bars
        int vBars = 5;
        for ( int i = 0; i <= vBars; i++ ) {
            float xPos = x1 + ( width * i / vBars ) - barThickness / 2;
            boxes.add( new Box(
                    new float[]{ xPos, y1, faceZ - depth },
                    new float[]{ xPos + barThickness, y2, faceZ } ) );
        }

        // Frame border (thicker edge)
        float frameThick = barThickness * 1.5f;
        // Top frame
        boxes.add( new Box( new float[]{ x1, y2 - frameThick, faceZ - depth },
                new float[]{ x2, y2, faceZ } ) );
        // Bottom frame
        boxes.add( new Box( new float[]{ x1, y1, faceZ - depth },
                new float[]{ x2, y1 + frameThick, faceZ } ) );
        // Left frame
        boxes.add( new Box( new float[]{ x1, y1, faceZ - depth },
                new float[]{ x1 + frameThick, y2, faceZ } ) );
        // Right frame
        boxes.add( new Box( new float[]{ x2 - frameThick, y1, faceZ - depth },
                new float[]{ x2, y2, faceZ } ) );

        return boxes;
    }

    /**
     * Returns the bracket vertex data for the given mount type.
     */
    public static List<Box> getBracketData( CrosswalkMountType mountType ) {
        switch ( mountType ) {
            case REAR:
                return BRACKET_REAR_VERTEX_DATA;
            case LEFT:
                return BRACKET_LEFT_VERTEX_DATA;
            case RIGHT:
                return BRACKET_RIGHT_VERTEX_DATA;
            case BASE:
            default:
                return BRACKET_BASE_VERTEX_DATA;
        }
    }
}
