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
import java.util.Comparator;
import java.util.HashMap;
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
 * axis-aligned cuboids), so this parser computes one AABB per Wavefront {@code o}-group and emits
 * each as its own cuboid. That gives a much better silhouette than a single AABB for largely
 * box-shaped models (crates, anchors, barbed-wire fences) while still being lossy for curves
 * (barber-pole stripes become a stack of cuboids, fine but not exact).
 *
 * <p>Coordinate convention: Blockbench-exported {@code .obj} files in CSM follow several
 * conventions (corner-origin {@code [0,1]^3}, x/z-centered around 0, models taller than a block,
 * etc.). To produce a consistent result, this parser computes the model's global AABB and fits it
 * into Dynmap's {@code [0, 16]^3} model space by anchoring {@code y_min → 0} (sitting on the
 * ground) and centering the horizontal extents on {@code x = 8, z = 8} (block centre). The same
 * transform is applied to every per-group AABB so the parts retain their relative positions.
 *
 * <p>To prevent absurd box counts (some Blockbench exports have 90+ groups for individual
 * decorations like apples in a crate), the result is capped at {@link #MAX_BOXES}. When that cap
 * is exceeded, the smallest groups by volume are merged into a single residual AABB.
 */
public final class ObjModelParser {

    private static final int MAX_BOXES = 24;
    /** Minimum cuboid thickness on each axis (in 0..16 space). */
    private static final double MIN_THICKNESS = 0.5;

    private final File devEnvironmentPath;

    public ObjModelParser(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    public ResolvedModel resolve(String modelRef, Map<String, String> variantTextures) {
        File objFile = locateModelFile(modelRef);
        if (objFile == null || !objFile.exists()) return null;

        try {
            ParsedObj parsed = parseObj(objFile);
            if (parsed.vertexCount == 0) return null;

            String fallbackTexture = pickTexture(parsed, variantTextures);
            if (fallbackTexture == null) return null;

            // Per-group AABBs in obj space. If no groups were detected (or all are empty), fall
            // back to a single AABB containing every vertex.
            List<GroupBox> groups = collectGroupAabbs(parsed);
            if (groups.isEmpty()) {
                groups.add(new GroupBox("__all__", parsed.min.clone(), parsed.max.clone(),
                        materialOrAny(parsed, null)));
            }
            // Cap the count: merge the smallest groups into a single residual AABB.
            groups = capBoxCount(groups, MAX_BOXES);

            // Build a unique patch list, mapping material → texture or falling back.
            Map<String, Integer> textureToPatchIdx = new LinkedHashMap<>();
            List<String> patches = new ArrayList<>();

            // Compute global fit transform from the obj-space global AABB.
            // dx/dz: centre on (8, 8). dy: anchor min to ground.
            double[] gMin = parsed.min;
            double[] gMax = parsed.max;
            double midX = (gMin[0] + gMax[0]) * 0.5;
            double midZ = (gMin[2] + gMax[2]) * 0.5;
            double yBase = gMin[1];

            List<Box> boxes = new ArrayList<>();
            for (GroupBox g : groups) {
                double[] from = new double[3];
                double[] to = new double[3];
                from[0] = clamp((g.min[0] - midX) * 16 + 8);
                to[0]   = clamp((g.max[0] - midX) * 16 + 8);
                from[1] = clamp((g.min[1] - yBase) * 16);
                to[1]   = clamp((g.max[1] - yBase) * 16);
                from[2] = clamp((g.min[2] - midZ) * 16 + 8);
                to[2]   = clamp((g.max[2] - midZ) * 16 + 8);

                // Avoid zero-volume slivers — pad to MIN_THICKNESS while staying inside [0, 16].
                for (int i = 0; i < 3; i++) {
                    if (to[i] - from[i] < MIN_THICKNESS) {
                        double mid = (from[i] + to[i]) * 0.5;
                        from[i] = Math.max(0, mid - MIN_THICKNESS / 2);
                        to[i]   = Math.min(16, mid + MIN_THICKNESS / 2);
                        // If we hit the wall, push back so the box still has min thickness.
                        if (to[i] - from[i] < MIN_THICKNESS) {
                            if (from[i] == 0) to[i] = MIN_THICKNESS;
                            if (to[i] == 16)  from[i] = 16 - MIN_THICKNESS;
                        }
                    }
                }

                String texRef = parsed.materialToTexture.getOrDefault(g.material, fallbackTexture);
                if (texRef == null) texRef = fallbackTexture;
                Integer patchIdx = textureToPatchIdx.get(texRef);
                if (patchIdx == null) {
                    patchIdx = patches.size();
                    patches.add(texRef);
                    textureToPatchIdx.put(texRef, patchIdx);
                }

                List<Face> faces = new ArrayList<>();
                for (Side s : Side.values()) {
                    faces.add(new Face(s, patchIdx, 0, deriveUv(s, from, to)));
                }
                boxes.add(new Box(from, to, true, null, null, faces));
            }
            return new ResolvedModel(boxes, patches, false);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Walks {@code f} directives, mapping each to its active {@code o}/{@code g} group + active
     * {@code usemtl} material. Aggregates per-group vertex AABBs.
     */
    private static List<GroupBox> collectGroupAabbs(ParsedObj parsed) {
        List<GroupBox> out = new ArrayList<>();
        Map<String, GroupBox> byKey = new LinkedHashMap<>();
        for (FaceRef f : parsed.faces) {
            String key = f.group + "|" + f.material;
            GroupBox gb = byKey.get(key);
            if (gb == null) {
                gb = new GroupBox(f.group,
                        new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
                        new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
                        f.material);
                byKey.put(key, gb);
                out.add(gb);
            }
            for (int vIdx : f.vertices) {
                if (vIdx < 0 || vIdx >= parsed.vertexCount) continue;
                double[] v = parsed.verts.get(vIdx);
                for (int a = 0; a < 3; a++) {
                    gb.min[a] = Math.min(gb.min[a], v[a]);
                    gb.max[a] = Math.max(gb.max[a], v[a]);
                }
            }
        }
        // Drop groups with no vertices (shouldn't happen, but be safe).
        out.removeIf(g -> !Double.isFinite(g.min[0]));
        return out;
    }

    /**
     * Reduces {@code groups} to at most {@code max} boxes by merging the smallest-volume entries
     * into a single residual AABB.
     */
    private static List<GroupBox> capBoxCount(List<GroupBox> groups, int max) {
        if (groups.size() <= max) return groups;
        groups.sort(Comparator.comparingDouble(ObjModelParser::volume).reversed());
        List<GroupBox> kept = new ArrayList<>(groups.subList(0, max - 1));
        // Merge the tail into a residual AABB.
        GroupBox residual = null;
        for (int i = max - 1; i < groups.size(); i++) {
            GroupBox g = groups.get(i);
            if (residual == null) {
                residual = new GroupBox("__residual__", g.min.clone(), g.max.clone(), g.material);
            } else {
                for (int a = 0; a < 3; a++) {
                    residual.min[a] = Math.min(residual.min[a], g.min[a]);
                    residual.max[a] = Math.max(residual.max[a], g.max[a]);
                }
            }
        }
        if (residual != null) kept.add(residual);
        return kept;
    }

    private static double volume(GroupBox g) {
        double vx = Math.max(0, g.max[0] - g.min[0]);
        double vy = Math.max(0, g.max[1] - g.min[1]);
        double vz = Math.max(0, g.max[2] - g.min[2]);
        return vx * vy * vz;
    }

    private static String materialOrAny(ParsedObj parsed, String preferred) {
        if (preferred != null && parsed.materialToTexture.containsKey(preferred)) return preferred;
        for (String m : parsed.materialsUsedInOrder) {
            if (parsed.materialToTexture.containsKey(m)) return m;
        }
        return parsed.materialToTexture.keySet().stream().findFirst().orElse(null);
    }

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
        for (String tex : parsed.materialToTexture.values()) {
            if (tex != null) return tex;
        }
        return null;
    }

    private File locateModelFile(String modelRef) {
        String domain = "csm";
        String name = modelRef;
        int colon = modelRef.indexOf(':');
        if (colon >= 0) {
            domain = modelRef.substring(0, colon);
            name = modelRef.substring(colon + 1);
        }
        if (!name.contains("/")) {
            name = "block/" + name;
        }
        return new File(devEnvironmentPath,
                "src/main/resources/assets/" + domain + "/models/" + name);
    }

    /** A single {@code f} directive's vertex-index list with its active group/material. */
    static final class FaceRef {
        final int[] vertices;
        final String group;
        final String material;

        FaceRef(int[] vertices, String group, String material) {
            this.vertices = vertices;
            this.group = group;
            this.material = material;
        }
    }

    /** Per-group AABB result. */
    static final class GroupBox {
        final String groupName;
        final double[] min;
        final double[] max;
        final String material;

        GroupBox(String groupName, double[] min, double[] max, String material) {
            this.groupName = groupName;
            this.min = min;
            this.max = max;
            this.material = material;
        }
    }

    static final class ParsedObj {
        final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int vertexCount = 0;
        final List<double[]> verts = new ArrayList<>();
        final List<FaceRef> faces = new ArrayList<>();
        final Set<String> materialsUsedInOrder = new LinkedHashSet<>();
        final Map<String, String> materialToTexture = new HashMap<>();
    }

    private ParsedObj parseObj(File objFile) throws IOException {
        ParsedObj p = new ParsedObj();
        File mtlFile = null;
        String currentGroup = "default";
        String currentMaterial = "";
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
                            p.verts.add(new double[]{x, y, z});
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
                    case "o":
                    case "g":
                        currentGroup = rest;
                        break;
                    case "usemtl":
                        currentMaterial = rest;
                        p.materialsUsedInOrder.add(rest);
                        break;
                    case "mtllib":
                        mtlFile = new File(objFile.getParentFile(), rest);
                        break;
                    case "f": {
                        String[] tokens = rest.split("\\s+");
                        int[] indices = new int[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            String t = tokens[i];
                            int slash = t.indexOf('/');
                            if (slash >= 0) t = t.substring(0, slash);
                            try {
                                int idx = Integer.parseInt(t);
                                // Wavefront indices are 1-based; negatives count from the end.
                                if (idx > 0) indices[i] = idx - 1;
                                else if (idx < 0) indices[i] = p.verts.size() + idx;
                                else indices[i] = -1;
                            } catch (NumberFormatException e) {
                                indices[i] = -1;
                            }
                        }
                        p.faces.add(new FaceRef(indices, currentGroup, currentMaterial));
                        break;
                    }
                    default:
                        // Ignore vt, vn, s, etc.
                        break;
                }
            }
        }
        if (mtlFile != null && mtlFile.exists()) {
            parseMtl(mtlFile, p.materialToTexture);
        }
        return p;
    }

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
