package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the two filters that fix the bulk of the previous tool's broken output:
 *
 * <ol>
 *   <li><b>Degenerate-face filter:</b> skip emitting a side face when the box dimension on that
 *       face's normal axis is below {@link #DEGENERATE_THICKNESS_EPSILON_MODEL_UNITS}, or when the
 *       face's UV rectangle has zero area. These produce patches Dynmap rejects with "Invalid
 *       modellist patch" warnings.</li>
 *   <li><b>Range simulation:</b> mirrors Dynmap's {@code PatchDefinition.outOfRange} ([-1, 2] per
 *       axis after rotation, in unit space). Boxes whose corners exceed this range are flagged so
 *       the caller can substitute an AABB-cube fallback.</li>
 * </ol>
 */
public final class PatchValidator {

    /** Below this model-unit thickness on the face-normal axis, we suppress the face. */
    public static final double DEGENERATE_THICKNESS_EPSILON_MODEL_UNITS = 0.0001;

    /** UV area below this (in 16x16 units squared) is treated as a degenerate face. */
    public static final double DEGENERATE_UV_AREA_EPSILON = 0.0001;

    public static final double RANGE_MIN = -1.0;
    public static final double RANGE_MAX = 2.0;

    private PatchValidator() {}

    /** True when the side has effectively no surface area on its normal axis. */
    public static boolean isDegenerateFace(Box box, Face face) {
        double thickness;
        switch (face.side) {
            case UP:
            case DOWN:
                thickness = Math.abs(box.to[1] - box.from[1]);
                break;
            case NORTH:
            case SOUTH:
                thickness = Math.abs(box.to[2] - box.from[2]);
                break;
            case EAST:
            case WEST:
                thickness = Math.abs(box.to[0] - box.from[0]);
                break;
            default:
                return false;
        }
        if (thickness < DEGENERATE_THICKNESS_EPSILON_MODEL_UNITS) return true;
        if (face.uv != null) {
            double uvArea = Math.abs((face.uv[2] - face.uv[0]) * (face.uv[3] - face.uv[1]));
            if (uvArea < DEGENERATE_UV_AREA_EPSILON) return true;
        }
        return false;
    }

    /** Returns a copy of the box with degenerate faces removed. May return null if every face is dropped. */
    public static Box withoutDegenerateFaces(Box box) {
        List<Face> kept = new ArrayList<>(box.faces.size());
        for (Face f : box.faces) {
            if (!isDegenerateFace(box, f)) kept.add(f);
        }
        if (kept.isEmpty()) return null;
        if (kept.size() == box.faces.size()) return box;
        return new Box(box.from, box.to, box.shade, box.rotation, box.rotOrigin, kept);
    }

    /**
     * Returns true if any corner of the box, after applying both per-element rotation and the given
     * model-level (variant) rotation, falls outside Dynmap's [{@link #RANGE_MIN},
     * {@link #RANGE_MAX}] window in unit space.
     */
    public static boolean isOutOfRange(Box box, double[] modelRotationDegrees) {
        double[][] corners = corners(box.from, box.to);
        for (double[] c : corners) {
            // Convert to unit space.
            double x = c[0] / 16.0;
            double y = c[1] / 16.0;
            double z = c[2] / 16.0;
            // Apply per-element rotation around origin.
            if (box.rotation != null) {
                double[] origin = box.rotOrigin != null
                        ? new double[]{box.rotOrigin[0] / 16.0, box.rotOrigin[1] / 16.0, box.rotOrigin[2] / 16.0}
                        : new double[]{0.5, 0.5, 0.5};
                double[] rotated = rotate(new double[]{x, y, z}, origin,
                        Math.toRadians(box.rotation[0]),
                        Math.toRadians(box.rotation[1]),
                        Math.toRadians(box.rotation[2]));
                x = rotated[0]; y = rotated[1]; z = rotated[2];
            }
            // Apply model-level rotation around (0.5, 0.5, 0.5).
            if (modelRotationDegrees != null && (modelRotationDegrees[0] != 0
                    || modelRotationDegrees[1] != 0 || modelRotationDegrees[2] != 0)) {
                double[] rotated = rotate(new double[]{x, y, z}, new double[]{0.5, 0.5, 0.5},
                        Math.toRadians(modelRotationDegrees[0]),
                        Math.toRadians(modelRotationDegrees[1]),
                        Math.toRadians(modelRotationDegrees[2]));
                x = rotated[0]; y = rotated[1]; z = rotated[2];
            }
            if (x < RANGE_MIN || x > RANGE_MAX) return true;
            if (y < RANGE_MIN || y > RANGE_MAX) return true;
            if (z < RANGE_MIN || z > RANGE_MAX) return true;
        }
        return false;
    }

    private static double[][] corners(double[] from, double[] to) {
        return new double[][]{
                {from[0], from[1], from[2]}, {to[0], from[1], from[2]},
                {from[0], to[1],   from[2]}, {to[0], to[1],   from[2]},
                {from[0], from[1], to[2]},   {to[0], from[1], to[2]},
                {from[0], to[1],   to[2]},   {to[0], to[1],   to[2]},
        };
    }

    /** Rotate point around origin by Euler angles (rx then ry then rz) in radians. */
    private static double[] rotate(double[] p, double[] o, double rx, double ry, double rz) {
        double x = p[0] - o[0];
        double y = p[1] - o[1];
        double z = p[2] - o[2];

        // Around X
        if (rx != 0) {
            double c = Math.cos(rx), s = Math.sin(rx);
            double ny = y * c - z * s;
            double nz = y * s + z * c;
            y = ny; z = nz;
        }
        // Around Y
        if (ry != 0) {
            double c = Math.cos(ry), s = Math.sin(ry);
            double nx = x * c + z * s;
            double nz = -x * s + z * c;
            x = nx; z = nz;
        }
        // Around Z
        if (rz != 0) {
            double c = Math.cos(rz), s = Math.sin(rz);
            double nx = x * c - y * s;
            double ny = x * s + y * c;
            x = nx; y = ny;
        }
        return new double[]{x + o[0], y + o[1], z + o[2]};
    }

    /** Compute the bounding box (in 0..16 model space) of all the boxes' from/to corners. */
    public static double[][] aabb(List<Box> boxes) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Box b : boxes) {
            minX = Math.min(minX, Math.min(b.from[0], b.to[0]));
            minY = Math.min(minY, Math.min(b.from[1], b.to[1]));
            minZ = Math.min(minZ, Math.min(b.from[2], b.to[2]));
            maxX = Math.max(maxX, Math.max(b.from[0], b.to[0]));
            maxY = Math.max(maxY, Math.max(b.from[1], b.to[1]));
            maxZ = Math.max(maxZ, Math.max(b.from[2], b.to[2]));
        }
        // Clamp to the unit-cube extension Dynmap allows ([-16, 32] in 0..16 model space ≈ [-1, 2] in unit).
        minX = Math.max(0, Math.min(16, minX));
        minY = Math.max(0, Math.min(16, minY));
        minZ = Math.max(0, Math.min(16, minZ));
        maxX = Math.max(0, Math.min(16, maxX));
        maxY = Math.max(0, Math.min(16, maxY));
        maxZ = Math.max(0, Math.min(16, maxZ));
        return new double[][]{{minX, minY, minZ}, {maxX, maxY, maxZ}};
    }
}
