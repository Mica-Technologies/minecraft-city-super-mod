package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a model reference (with parent chain) into a list of {@link Box}es plus the merged
 * texture key map. Element-based models are converted directly; parent-only models that resolve
 * to a known vanilla terminal (cube_all, fence_post, half_slab, …) are converted via hard-coded
 * geometry.
 */
public final class ModelResolver {

    private final File devEnvironmentPath;
    private final Map<String, JsonObject> modelCache = new HashMap<>();

    public ModelResolver(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    /** Result of resolving a model reference. */
    public static final class ResolvedModel {
        public final List<Box> boxes;
        /** Ordered list of texture references appearing in the model, in patch-index order. */
        public final List<String> patchTextureRefs;
        /** True if this came from the vanilla-terminal fallback, not real model elements. */
        public final boolean isFallback;

        public ResolvedModel(List<Box> boxes, List<String> patchTextureRefs, boolean isFallback) {
            this.boxes = boxes;
            this.patchTextureRefs = patchTextureRefs;
            this.isFallback = isFallback;
        }
    }

    /**
     * @param modelRef        e.g. {@code "csm:trafficsignals/shared_models/foo"} or {@code "cube_all"}
     * @param variantTextures merged textures map from the blockstate variant; takes precedence over model textures
     */
    public ResolvedModel resolve(String modelRef, Map<String, String> variantTextures) {
        // Build merged texture map from parent chain (child wins).
        Map<String, String> mergedTextures = new LinkedHashMap<>();
        JsonObject elementsHolder = walkChainAndMerge(modelRef, mergedTextures, new HashSet<>());

        // Variant textures take final precedence over model textures.
        if (variantTextures != null) {
            mergedTextures.putAll(variantTextures);
        }

        // If we found elements, convert them.
        if (elementsHolder != null && elementsHolder.has("elements")) {
            return convertElements(elementsHolder.getAsJsonArray("elements"), mergedTextures);
        }

        // Otherwise, attempt vanilla terminal fallback.
        return fallbackVanillaTerminal(modelRef, mergedTextures);
    }

    /**
     * Walks the parent chain and merges textures from root → leaf (leaf wins). Returns the deepest
     * model JSON that contains an {@code elements} array, or null if none found.
     */
    private JsonObject walkChainAndMerge(String modelRef, Map<String, String> mergedTextures,
                                         Set<String> visited) {
        if (modelRef == null || !visited.add(modelRef)) return null;
        JsonObject obj = loadModel(modelRef);
        if (obj == null) return null;

        JsonObject parentTexturesHolder = null;
        if (obj.has("parent")) {
            parentTexturesHolder = walkChainAndMerge(obj.get("parent").getAsString(),
                    mergedTextures, visited);
        }

        // Merge this model's textures *over* the parent's.
        if (obj.has("textures")) {
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("textures").entrySet()) {
                mergedTextures.put(e.getKey(), e.getValue().getAsString());
            }
        }

        // Prefer this model's elements if it has them; else inherit parent's.
        if (obj.has("elements")) return obj;
        return parentTexturesHolder;
    }

    private JsonObject loadModel(String modelRef) {
        if (modelCache.containsKey(modelRef)) return modelCache.get(modelRef);
        String relPath;
        String domain = "csm";
        String name = modelRef;
        int colon = modelRef.indexOf(':');
        if (colon >= 0) {
            domain = modelRef.substring(0, colon);
            name = modelRef.substring(colon + 1);
        }
        if (!name.contains("/")) {
            // Bare names like "cube_all" are vanilla parents; treat as block/<name> in the minecraft domain.
            domain = "minecraft";
            name = "block/" + name;
        } else if ("csm".equals(domain) && !name.startsWith("block/")) {
            // CSM model refs like "trafficsignals/shared_models/foo" → assets/csm/models/block/...
            name = "block/" + name;
        }
        relPath = "src/main/resources/assets/" + domain + "/models/" + name + ".json";
        File f = new File(devEnvironmentPath, relPath);
        if (!f.exists()) {
            modelCache.put(modelRef, null);
            return null;
        }
        try {
            String json = Files.readString(f.toPath());
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            modelCache.put(modelRef, obj);
            return obj;
        } catch (IOException | RuntimeException e) {
            modelCache.put(modelRef, null);
            return null;
        }
    }

    private ResolvedModel convertElements(JsonArray elements, Map<String, String> mergedTextures) {
        List<Box> boxes = new ArrayList<>();
        List<String> patchTextureRefs = new ArrayList<>();
        Map<String, Integer> textureRefToIndex = new LinkedHashMap<>();

        for (JsonElement el : elements) {
            JsonObject e = el.getAsJsonObject();
            double[] from = readVec3(e.getAsJsonArray("from"));
            double[] to = readVec3(e.getAsJsonArray("to"));
            boolean shade = !e.has("shade") || e.get("shade").getAsBoolean();

            double[] rotation = null;
            double[] rotOrigin = null;
            if (e.has("rotation")) {
                JsonObject r = e.getAsJsonObject("rotation");
                double angle = r.get("angle").getAsDouble();
                String axis = r.get("axis").getAsString();
                rotation = new double[]{
                        "x".equals(axis) ? angle : 0,
                        "y".equals(axis) ? angle : 0,
                        "z".equals(axis) ? angle : 0
                };
                if (r.has("origin")) {
                    rotOrigin = readVec3(r.getAsJsonArray("origin"));
                }
            }

            List<Face> faces = new ArrayList<>();
            if (e.has("faces")) {
                JsonObject facesObj = e.getAsJsonObject("faces");
                for (Map.Entry<String, JsonElement> fe : facesObj.entrySet()) {
                    Side side;
                    try { side = Side.fromMinecraftFace(fe.getKey()); }
                    catch (IllegalArgumentException ex) { continue; }
                    JsonObject face = fe.getValue().getAsJsonObject();
                    String tex = face.has("texture") ? face.get("texture").getAsString() : null;
                    String resolved = resolveTextureRef(tex, mergedTextures);
                    if (resolved == null) continue;
                    int idx = textureRefToIndex.computeIfAbsent(resolved, k -> {
                        patchTextureRefs.add(k);
                        return patchTextureRefs.size() - 1;
                    });
                    int rot = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    double[] uv = face.has("uv") ? readUv(face.getAsJsonArray("uv")) : deriveUv(side, from, to);
                    faces.add(new Face(side, idx, rot, uv));
                }
            }
            boxes.add(new Box(from, to, shade, rotation, rotOrigin, faces));
        }
        return new ResolvedModel(boxes, patchTextureRefs, false);
    }

    /** Resolves a {@code "#key"} reference through the merged textures map until a real path is found. */
    static String resolveTextureRef(String ref, Map<String, String> textures) {
        if (ref == null) return null;
        Set<String> guard = new HashSet<>();
        String cur = ref;
        while (cur != null && cur.startsWith("#")) {
            String key = cur.substring(1);
            if (!guard.add(key)) return null;
            cur = textures.get(key);
        }
        return cur;
    }

    private static double[] readVec3(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static double[] readUv(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(),
                a.get(2).getAsDouble(), a.get(3).getAsDouble()};
    }

    /**
     * Derives default UVs for a face when none specified, per vanilla convention: project the
     * cuboid onto the face and use 0..16 → axis bounds.
     */
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

    /**
     * For models without an {@code elements} array (parent-only), produce hard-coded geometry for
     * recognised vanilla terminals. Everything else falls back to a full unit cube using the
     * "all"/"particle"/first-available texture.
     */
    private ResolvedModel fallbackVanillaTerminal(String modelRef, Map<String, String> textures) {
        String terminal = stripParentDomain(modelRef);
        if (terminal == null) terminal = "";

        switch (terminal) {
            case "cube":
                return cube6Sided(textures);
            case "cube_all":
            case "cube_mirrored_all":
                return cubeAll(textures);
            case "fence_post":
                return cuboid(6, 0, 6, 10, 16, 10, textures, "texture", "particle", "all");
            case "fence_inventory":
                return cuboid(6, 0, 6, 10, 16, 10, textures, "texture", "particle", "all");
            case "fence_side":
                // Skipped here; multipart-aware Phase 10 will emit proper connections. For now,
                // collapse to the post.
                return cuboid(6, 6, 0, 10, 15, 16, textures, "texture", "particle", "all");
            case "half_slab":
            case "slab":
                return cuboid(0, 0, 0, 16, 8, 16, textures, "side", "all", "particle");
            case "upper_slab":
            case "upper_slab_all":
                return cuboid(0, 8, 0, 16, 16, 16, textures, "side", "all", "particle");
            case "stairs":
            case "inner_stairs":
            case "outer_stairs":
                // Real stairs geometry varies per shape/half/facing — too complex for the fallback.
                // Use a half-block placeholder; Dynmap built-in stairs renderer (if invoked) would
                // give correct geometry, but we don't currently route stairs to it.
                return cuboid(0, 0, 0, 16, 8, 16, textures, "side", "all", "particle");
            case "":
            case "block":
            default:
                return cubeAll(textures);
        }
    }

    private static String stripParentDomain(String ref) {
        if (ref == null) return null;
        String s = ref;
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        if (s.startsWith("block/")) s = s.substring("block/".length());
        return s;
    }

    /** Build a single-cuboid resolved model with all 6 faces using the first available texture key. */
    private static ResolvedModel cubeAll(Map<String, String> textures) {
        String tex = pickTexture(textures, "all", "particle", "texture", "0");
        return cuboid(0, 0, 0, 16, 16, 16, textures, tex);
    }

    /** Build a single-cuboid resolved model with per-side textures (vanilla "cube" model). */
    private static ResolvedModel cube6Sided(Map<String, String> textures) {
        String down = pickTexture(textures, "down", "all", "particle");
        String up   = pickTexture(textures, "up",   "all", "particle");
        String north = pickTexture(textures, "north", "all", "particle");
        String south = pickTexture(textures, "south", "all", "particle");
        String west = pickTexture(textures, "west",  "all", "particle");
        String east = pickTexture(textures, "east",  "all", "particle");
        return cuboid6(0, 0, 0, 16, 16, 16, textures, down, up, north, south, west, east);
    }

    private static String pickTexture(Map<String, String> textures, String... candidates) {
        for (String k : candidates) {
            if (textures.containsKey(k)) return textures.get(k);
        }
        // Fallback: first non-#-prefixed value in the map.
        for (String v : textures.values()) {
            if (v != null && !v.startsWith("#")) return v;
        }
        return null;
    }

    /** Helper: full single-cuboid with one texture for all 6 faces. */
    private static ResolvedModel cuboid(double x1, double y1, double z1, double x2, double y2, double z2,
                                        Map<String, String> textures, String... textureKeys) {
        // First arg of textureKeys is the resolved texture path; if multiple given, treat as candidate keys.
        String resolved;
        if (textureKeys.length == 1) {
            resolved = textureKeys[0];
        } else {
            resolved = pickTexture(textures, textureKeys);
        }
        if (resolved != null && resolved.startsWith("#")) {
            resolved = ModelResolver.resolveTextureRef(resolved, textures);
        }
        if (resolved == null) {
            return new ResolvedModel(new ArrayList<>(), new ArrayList<>(), true);
        }
        List<String> patches = new ArrayList<>();
        patches.add(resolved);
        List<Face> faces = new ArrayList<>();
        double[] from = {x1, y1, z1};
        double[] to   = {x2, y2, z2};
        for (Side s : Side.values()) {
            faces.add(new Face(s, 0, 0, deriveUv(s, from, to)));
        }
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(from, to, true, null, null, faces));
        return new ResolvedModel(boxes, patches, true);
    }

    /** Helper: full single-cuboid with per-side textures. */
    private static ResolvedModel cuboid6(double x1, double y1, double z1, double x2, double y2, double z2,
                                         Map<String, String> textures,
                                         String down, String up, String north, String south,
                                         String west, String east) {
        List<String> patches = new ArrayList<>();
        Map<String, Integer> idx = new LinkedHashMap<>();
        String[] sides = {down, up, north, south, west, east};
        Side[] order = {Side.DOWN, Side.UP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST};
        List<Face> faces = new ArrayList<>();
        double[] from = {x1, y1, z1};
        double[] to   = {x2, y2, z2};
        for (int i = 0; i < 6; i++) {
            String t = sides[i];
            if (t == null) continue;
            if (t.startsWith("#")) {
                t = ModelResolver.resolveTextureRef(t, textures);
                if (t == null) continue;
            }
            Integer pi = idx.get(t);
            if (pi == null) {
                pi = patches.size();
                patches.add(t);
                idx.put(t, pi);
            }
            faces.add(new Face(order[i], pi, 0, deriveUv(order[i], from, to)));
        }
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(from, to, true, null, null, faces));
        return new ResolvedModel(boxes, patches, true);
    }
}
