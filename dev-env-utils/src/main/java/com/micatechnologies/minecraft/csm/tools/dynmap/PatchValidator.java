package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the filters that keep Dynmap from rejecting the output:
 *
 * <ol>
 *   <li><b>Dynmap's own face check:</b> {@link #passesDynmapFaceCheck} reproduces, quirks and all,
 *       the check Dynmap runs on every face. The two filters below did not, and the May 2026
 *       output still drew 73,107 "Invalid modellist patch" lines at startup.</li>
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
     * Returns a copy of the box without the faces Dynmap would reject (see
     * {@link #passesDynmapFaceCheck}), or null if it rejects every face. Dynmap drops a rejected face
     * from the render anyway, so leaving it out changes nothing on the map; it only spares the
     * server one FATAL log line per face per startup (73,107 of them from the May 2026 output).
     */
    public static Box withoutRejectedFaces(Box box) {
        List<Face> kept = new ArrayList<>(box.faces.size());
        for (Face f : box.faces) {
            if (passesDynmapFaceCheck(box, f)) kept.add(f);
        }
        if (kept.isEmpty()) return null;
        if (kept.size() == box.faces.size()) return box;
        return new Box(box.from, box.to, box.shade, box.rotation, box.rotOrigin, kept);
    }

    /**
     * Whether Dynmap accepts this face of a {@code modellist} box: a port of
     * {@code PatchDefinition.updateModelFace} followed by {@code PatchDefinition.validate}
     * (Dynmap 3.7-beta-6, unchanged on its main branch as of 2026-10). A face that fails is logged
     * as "Invalid modellist patch for box ..." and left out of the render.
     *
     * <p>Dynmap builds the face as a patch spanning the WHOLE texture (origin, U and V vectors),
     * scaled so the face's UV window lands on the face. {@code validate} then checks four points of
     * that patch against [-1, 2], but two of them take {@code vmin}/{@code vmax} as their U
     * coefficient where the face corner would use {@code umin}/{@code umax}. So the point tested
     * is the face corner moved along U by {@code (vmin - umin)} whole textures. A face whose UV
     * window is much narrower than the face (a half-pixel swatch on a 14 px plate edge) has a U
     * vector many blocks long, and fails unless its window happens to sit on the texture's
     * diagonal. This must reproduce that check exactly, not a correct one, or the output still
     * floods the log.
     *
     * <p>Only the face itself is checked. Element and model rotation are applied by Dynmap after
     * this and drop an out-of-range result silently, which {@link #isOutOfRange} already guards.
     */
    public static boolean passesDynmapFaceCheck(Box box, Face face) {
        // Dynmap reads the numbers back from the text we write, so check those, not the doubles.
        double[] from = new double[3];
        double[] to = new double[3];
        for (int i = 0; i < 3; i++) {
            from[i] = written(box.from[i]) / 16.0;
            to[i] = written(box.to[i]) / 16.0;
        }
        double[] puv = null;
        if (face.uv != null) {
            double u0 = written(face.uv[0]), v0 = written(face.uv[1]);
            double u1 = written(face.uv[2]), v1 = written(face.uv[3]);
            // Minecraft's V runs top down, Dynmap's bottom up
            puv = new double[]{u0 / 16.0, 1 - v1 / 16.0, u1 / 16.0, 1 - v0 / 16.0};
            if (puv[0] > puv[2]) { puv[0] = 1 - puv[0]; puv[2] = 1 - puv[2]; }
            if (puv[1] > puv[3]) { puv[1] = 1 - puv[1]; puv[3] = 1 - puv[3]; }
        }
        double[] ll, lr, ul, ur, def;
        switch (face.side) {
            case DOWN:
                ll = new double[]{from[0], from[1], from[2]}; lr = new double[]{to[0], from[1], from[2]};
                ul = new double[]{from[0], from[1], to[2]};   ur = new double[]{to[0], from[1], to[2]};
                def = new double[]{from[0], from[2], to[0], to[2]};
                break;
            case UP:
                ll = new double[]{from[0], to[1], to[2]};   lr = new double[]{to[0], to[1], to[2]};
                ul = new double[]{from[0], to[1], from[2]}; ur = new double[]{to[0], to[1], from[2]};
                def = new double[]{from[0], 1 - to[2], to[0], 1 - from[2]};
                break;
            case NORTH:
                ll = new double[]{to[0], from[1], from[2]}; lr = new double[]{from[0], from[1], from[2]};
                ul = new double[]{to[0], to[1], from[2]};   ur = new double[]{from[0], to[1], from[2]};
                def = new double[]{1 - to[0], from[1], 1 - from[0], to[1]};
                break;
            case SOUTH:
                ll = new double[]{from[0], from[1], to[2]}; lr = new double[]{to[0], from[1], to[2]};
                ul = new double[]{from[0], to[1], to[2]};   ur = new double[]{to[0], to[1], to[2]};
                def = new double[]{from[0], from[1], to[0], to[1]};
                break;
            case WEST:
                ll = new double[]{from[0], from[1], from[2]}; lr = new double[]{from[0], from[1], to[2]};
                ul = new double[]{from[0], to[1], from[2]};   ur = new double[]{from[0], to[1], to[2]};
                def = new double[]{from[2], from[1], to[2], to[1]};
                break;
            case EAST:
                ll = new double[]{to[0], from[1], to[2]}; lr = new double[]{to[0], from[1], from[2]};
                ul = new double[]{to[0], to[1], to[2]};   ur = new double[]{to[0], to[1], from[2]};
                def = new double[]{1 - to[2], from[1], 1 - from[2], to[1]};
                break;
            default:
                return false;
        }
        if (puv == null) puv = def;
        // Dynmap slides a window hanging off the texture back onto it, keeping its size
        if (puv[0] < 0) { puv[2] -= puv[0]; puv[0] = 0; }
        if (puv[1] < 0) { puv[3] -= puv[1]; puv[1] = 0; }
        if (puv[2] > 1) { puv[0] -= puv[2] - 1; puv[2] = 1; }
        if (puv[3] > 1) { puv[1] -= puv[3] - 1; puv[3] = 1; }
        // Only 90, 180 and 270 mean anything to Dynmap's parser; it ignores any other suffix
        double[] t;
        switch (face.textureRotation) {
            case 270: t = ll; ll = lr; lr = ur; ur = ul; ul = t; break;
            case 180: t = ll; ll = ur; ur = t; t = lr; lr = ul; ul = t; break;
            case 90:  t = lr; lr = ll; ll = ul; ul = ur; ur = t; break;
            default: break;
        }
        double[] o = new double[3], u = new double[3], v = new double[3];
        // A zero-width window leaves all three at zero, which Dynmap accepts
        if (puv[0] != puv[2] && puv[1] != puv[3]) {
            double du = puv[2] - puv[0];
            double dv = puv[3] - puv[1];
            for (int i = 0; i < 3; i++) {
                u[i] = (lr[i] - ll[i]) / du;
                v[i] = (ul[i] - ll[i]) / dv;
                o[i] = ll[i] - u[i] * puv[0] - v[i] * puv[1];
            }
        }
        double umin = puv[0], vmin = puv[1], umax = puv[2], vmax = puv[3];
        for (int i = 0; i < 3; i++) {
            // validate()'s four points, its swapped coefficients included
            if (outOfDynmapRange(o[i] + u[i] * umin + v[i] * vmin)) return false;
            if (outOfDynmapRange(o[i] + u[i] * vmin + v[i] * vmax)) return false;
            if (outOfDynmapRange(o[i] + u[i] * umax + v[i] * vmin)) return false;
            if (outOfDynmapRange(o[i] + u[i] * vmax + v[i] * vmax)) return false;
        }
        return true;
    }

    private static boolean outOfDynmapRange(double c) {
        return c < RANGE_MIN || c > RANGE_MAX;
    }

    /** The value Dynmap parses back from {@link DynmapEmitter#num}. */
    private static double written(double value) {
        return Double.parseDouble(DynmapEmitter.num(value));
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
