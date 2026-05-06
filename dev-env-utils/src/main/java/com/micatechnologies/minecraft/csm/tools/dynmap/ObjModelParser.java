package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;
import com.micatechnologies.minecraft.csm.tools.dynmap.ModelResolver.ResolvedModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal Wavefront {@code .obj} reader sufficient for Dynmap renderdata generation.
 *
 * <p>The CSM novelty blocks reference {@code .obj} models as a Forge custom loader. These models
 * are too detailed to round-trip into Dynmap modellist boxes faithfully (Dynmap supports only
 * axis-aligned cuboids), so this parser extracts the model's {@em silhouette} — the bounding box
 * of all vertices — plus the first material's texture, and emits a single cuboid. That gives a
 * recognisable colored block on the web map without rejecting the geometry.
 *
 * <p>Coordinate convention: Blockbench-exported {@code .obj} files in CSM follow several
 * conventions (corner-origin {@code [0,1]^3}, x/z-centered around 0, models taller than a block,
 * etc.). To produce a consistent result, this parser fits the obj-space AABB into Dynmap's
 * {@code [0, 16]^3} model space by anchoring the AABB to {@code y_min → 0} (sitting on the ground)
 * and centering the horizontal extents on {@code x = 8, z = 8} (block centre).
 */
public final class ObjModelParser {

    private final File devEnvironmentPath;

    public ObjModelParser(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    /**
     * Resolves a {@code csm:foo.obj} reference into a single-box {@link ResolvedModel}.
     *
     * @return a resolved model, or {@code null} if the .obj could not be read or has no vertices
     */
    public ResolvedModel resolve(String modelRef, Map<String, String> variantTextures) {
        File objFile = locateModelFile(modelRef);
        if (objFile == null || !objFile.exists()) return null;

        try {
            ParsedObj parsed = parseObj(objFile);
            if (parsed.vertexCount == 0) return null;

            String textureRef = pickTexture(parsed, variantTextures);
            if (textureRef == null) return null;

            // Fit obj AABB → Dynmap [0, 16] model space.
            double[] from = new double[3];
            double[] to = new double[3];
            // Horizontal axes: center on block centre (8, 8).
            for (int axis : new int[]{0, 2}) {
                double mid = (parsed.min[axis] + parsed.max[axis]) * 0.5;
                from[axis] = clamp((parsed.min[axis] - mid) * 16 + 8);
                to[axis]   = clamp((parsed.max[axis] - mid) * 16 + 8);
            }
            // Vertical axis: anchor min to ground (y=0).
            from[1] = clamp((parsed.min[1] - parsed.min[1]) * 16);
            to[1]   = clamp((parsed.max[1] - parsed.min[1]) * 16);

            // Avoid zero-volume boxes (some "decoration" models are very thin).
            for (int i = 0; i < 3; i++) {
                if (to[i] - from[i] < 0.5) {
                    double mid = (from[i] + to[i]) * 0.5;
                    from[i] = Math.max(0, mid - 0.25);
                    to[i] = Math.min(16, mid + 0.25);
                }
            }

            List<String> patches = new ArrayList<>();
            patches.add(textureRef);
            List<Face> faces = new ArrayList<>();
            for (Side s : Side.values()) {
                faces.add(new Face(s, 0, 0, deriveUv(s, from, to)));
            }
            List<Box> boxes = new ArrayList<>();
            boxes.add(new Box(from, to, true, null, null, faces));
            return new ResolvedModel(boxes, patches, true);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Picks the best texture reference for the obj model:
     * <ol>
     *   <li>If the variant blockstate has a single texture override, use that.</li>
     *   <li>Else if the .mtl file declared a {@code map_Kd} for any used material, use the first.</li>
     * </ol>
     */
    private static String pickTexture(ParsedObj parsed, Map<String, String> variantTextures) {
        if (variantTextures != null) {
            for (String v : variantTextures.values()) {
                if (v != null && !v.startsWith("#")) return v;
            }
        }
        for (String mat : parsed.materialsUsedInOrder) {
            String tex = parsed.materialToTexture.get(mat);
            if (tex != null) return tex;
        }
        // Fall back to the first material that has a texture, even if not "used" by f directives.
        for (String tex : parsed.materialToTexture.values()) {
            if (tex != null) return tex;
        }
        return null;
    }

    /** Resolves {@code csm:foo.obj} → {@code <devEnv>/src/main/resources/assets/csm/models/block/foo.obj}. */
    private File locateModelFile(String modelRef) {
        String domain = "csm";
        String name = modelRef;
        int colon = modelRef.indexOf(':');
        if (colon >= 0) {
            domain = modelRef.substring(0, colon);
            name = modelRef.substring(colon + 1);
        }
        // Bare names land in models/block/.
        if (!name.contains("/")) {
            name = "block/" + name;
        }
        return new File(devEnvironmentPath,
                "src/main/resources/assets/" + domain + "/models/" + name);
    }

    /** Parsed {@code .obj} contents — vertex AABB plus {@code material → texture} mapping. */
    static final class ParsedObj {
        final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int vertexCount = 0;
        /** Materials in the order they first appear via {@code usemtl}. */
        final Set<String> materialsUsedInOrder = new LinkedHashSet<>();
        /** Material name → texture ref (e.g. {@code csm:blocks/furniture/apple_crate}) from {@code map_Kd}. */
        final Map<String, String> materialToTexture = new LinkedHashMap<>();
    }

    private ParsedObj parseObj(File objFile) throws IOException {
        ParsedObj p = new ParsedObj();
        File mtlFile = null;
        try (BufferedReader r = new BufferedReader(new FileReader(objFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int sp = line.indexOf(' ');
                if (sp < 0) continue;
                String head = line.substring(0, sp);
                String rest = line.substring(sp + 1).trim();
                switch (head) {
                    case "v": {
                        String[] parts = rest.split("\\s+");
                        if (parts.length < 3) continue;
                        try {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);
                            p.min[0] = Math.min(p.min[0], x);
                            p.min[1] = Math.min(p.min[1], y);
                            p.min[2] = Math.min(p.min[2], z);
                            p.max[0] = Math.max(p.max[0], x);
                            p.max[1] = Math.max(p.max[1], y);
                            p.max[2] = Math.max(p.max[2], z);
                            p.vertexCount++;
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                    case "usemtl":
                        p.materialsUsedInOrder.add(rest);
                        break;
                    case "mtllib":
                        // Companion .mtl is sibling to the .obj.
                        mtlFile = new File(objFile.getParentFile(), rest);
                        break;
                    default:
                        // Ignore vt, vn, f, o, g, s, etc. — we only need vertex AABB and materials.
                        break;
                }
            }
        }
        if (mtlFile != null && mtlFile.exists()) {
            parseMtl(mtlFile, p.materialToTexture);
        }
        return p;
    }

    /** Reads {@code newmtl} / {@code map_Kd} pairs from a Wavefront .mtl. */
    private static void parseMtl(File mtlFile, Map<String, String> out) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(mtlFile))) {
            String line;
            String currentMaterial = null;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int sp = line.indexOf(' ');
                if (sp < 0) continue;
                String head = line.substring(0, sp);
                String rest = line.substring(sp + 1).trim();
                if ("newmtl".equals(head)) {
                    currentMaterial = rest;
                } else if ("map_Kd".equals(head) && currentMaterial != null) {
                    out.put(currentMaterial, rest);
                }
            }
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(16, v));
    }

    /** Project the cuboid onto the face for default UVs (matches {@link ModelResolver}). */
    private static double[] deriveUv(Side side, double[] from, double[] to) {
        switch (side) {
            case DOWN:  return new double[]{from[0], 16 - to[2],   to[0], 16 - from[2]};
            case UP:    return new double[]{from[0], from[2],      to[0], to[2]};
            case NORTH: return new double[]{16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]};
            case SOUTH: return new double[]{from[0], 16 - to[1],   to[0], 16 - from[1]};
            case WEST:  return new double[]{from[2], 16 - to[1],   to[2], 16 - from[1]};
            case EAST:  return new double[]{16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]};
            default: throw new IllegalStateException();
        }
    }
}
