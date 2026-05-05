package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;
import com.micatechnologies.minecraft.csm.tools.dynmap.ModelResolver.ResolvedModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Geometry derivation for blocks rendered by a TileEntitySpecialRenderer (TESR). Their visible
 * shape lives in Java code (in {@code *VertexData} classes) rather than in a JSON model, so the
 * normal model-resolution pipeline can't see it.
 *
 * <p>This class:
 * <ol>
 *   <li>Scans {@link #VERTEXDATA_FILES} (relative to repo root) and parses each Java source for
 *       {@code public static final List<Box> NAME = Arrays.asList(...)} constants, returning the
 *       AABB cuboids encoded as {@code new Box(new float[]{x,y,z}, new float[]{x,y,z})}.</li>
 *   <li>Combines named arrays per the {@link #BLOCK_CLASS_RECIPES} table to produce a canonical
 *       silhouette for each TESR-rendered block class.</li>
 * </ol>
 *
 * <p>Note: this only handles the TESRs that actually have a corresponding {@code *VertexData}
 * class — vehicle/pedestrian signals and the blankout box. Other TESR-rendered blocks (lane
 * control signal, fire alarm strobe, emergency light, HVAC thermostat, message signs, speed
 * limit signs, traffic beacons, dynamic guide sign, mount kit, tattle-tale beacon) have inline
 * geometry in the renderer itself; they continue to fall back to the static blockstate model
 * (typically an AABB cube) until per-renderer adapters are written.
 */
public final class TesrGeometry {

    /**
     * Java source files to parse for {@code List<Box>} constants. Relative to repo root.
     */
    private static final List<String> VERTEXDATA_FILES = Arrays.asList(
            "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic/TrafficSignalVertexData.java",
            "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic/CrosswalkSignalVertexData.java",
            "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic/BlankoutBoxVertexData.java"
    );

    /**
     * Recipes for blocks that should be silhouetted from {@code *VertexData} arrays. Two match
     * modes are supported (the matcher returns the first hit):
     *
     * <ol>
     *   <li><b>Registry-name prefix</b> (most signal heads use a factory class so their Java
     *       class doesn't match per-registry-name) — e.g. {@code "controllablesignal"} matches
     *       every {@code controllablesignal*} block.</li>
     *   <li><b>Java class name match</b> — checked when the block has its own Java class.</li>
     * </ol>
     */
    /**
     * Recipe order matters: more-specific patterns must come first. The matcher returns the first
     * hit. Boolean substring tests rather than prefix tests handle both
     * {@code controllablecrosswalk} and {@code controllablehorizontalanglecrosswalksingle}.
     */
    private static final List<String> SIGNAL_PARTS = Arrays.asList(
            "SIGNAL_BODY_VERTEX_DATA", "SIGNAL_DOOR_VERTEX_DATA", "NONE_VISOR_VERTEX_DATA");
    private static final List<String> CROSSWALK_PARTS = Arrays.asList(
            "SINGLE_BODY_VERTEX_DATA", "SINGLE_VISOR_HOOD_VERTEX_DATA");
    private static final List<String> BLANKOUT_PARTS = Arrays.asList(
            "BODY_VERTEX_DATA", "VISOR_HOOD_VERTEX_DATA");

    private static final List<RecipeEntry> RECIPES = Arrays.asList(
            // Crosswalk button/tweeter blocks — keep these as JSON models (small accessories);
            // skip them with an empty recipe (matched only to suppress fallthrough).
            RecipeEntry.byRegistryContains(Arrays.asList("crosswalkbutton", "crosswalktweeter"),
                    Collections.emptyList()),
            // Crosswalk signal bodies (any "crosswalk" not caught above).
            RecipeEntry.byRegistryContains(Arrays.asList("crosswalk"), CROSSWALK_PARTS),
            // Blankout boxes.
            RecipeEntry.byRegistryContains(Arrays.asList("blankout"), BLANKOUT_PARTS),
            // Everything else "controllable*signal*" gets the standard signal body. Includes:
            //   controllablesignal, controllablehawksignal, controllabledoghousesignal*,
            //   controllablehorizontal*signal, controllablevertical*signal, *bikesignal,
            //   *railsignal, etc.
            RecipeEntry.byPredicate("controllable*signal*",
                    n -> n.startsWith("controllable") && n.contains("signal"),
                    SIGNAL_PARTS)
    );

    /** Default texture used for the silhouette. Most signal bodies are black metal. */
    public static final String DEFAULT_BODY_TEXTURE_REF =
            "csm:blocks/trafficsignals/shared_textures/metal_black";

    /** Pattern matching one named {@code public static final List<Box> NAME = Arrays.asList(...)} block. */
    private static final Pattern LIST_BOX_PATTERN = Pattern.compile(
            "public\\s+static\\s+final\\s+List<Box>\\s+(\\w+)\\s*=\\s*Arrays\\.asList\\s*\\(([\\s\\S]*?)\\)\\s*;",
            Pattern.MULTILINE);

    /** Pattern matching one {@code new Box(new float[]{x, y, z}, new float[]{x, y, z})} entry. */
    private static final Pattern BOX_ENTRY_PATTERN = Pattern.compile(
            "new\\s+Box\\s*\\(\\s*new\\s+float\\s*\\[\\s*\\]\\s*\\{\\s*"
                    + "([-\\d.f]+)\\s*,\\s*([-\\d.f]+)\\s*,\\s*([-\\d.f]+)\\s*\\}\\s*,\\s*"
                    + "new\\s+float\\s*\\[\\s*\\]\\s*\\{\\s*"
                    + "([-\\d.f]+)\\s*,\\s*([-\\d.f]+)\\s*,\\s*([-\\d.f]+)\\s*\\}\\s*\\)");

    private final File devEnvironmentPath;
    private final Map<String, List<double[]>> vertexDataArrays = new HashMap<>();

    public TesrGeometry(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    /** Parses all known {@code *VertexData} Java sources and caches the named Box arrays. */
    public void load() throws IOException {
        for (String relPath : VERTEXDATA_FILES) {
            File f = new File(devEnvironmentPath, relPath);
            if (!f.exists()) {
                System.err.println("  TesrGeometry: missing source " + relPath);
                continue;
            }
            String content = Files.readString(f.toPath());
            Matcher m = LIST_BOX_PATTERN.matcher(content);
            while (m.find()) {
                String name = m.group(1);
                String body = m.group(2);
                List<double[]> entries = new ArrayList<>();
                Matcher bm = BOX_ENTRY_PATTERN.matcher(body);
                while (bm.find()) {
                    double[] raw = new double[]{
                            parseFloatLit(bm.group(1)), parseFloatLit(bm.group(2)), parseFloatLit(bm.group(3)),
                            parseFloatLit(bm.group(4)), parseFloatLit(bm.group(5)), parseFloatLit(bm.group(6))
                    };
                    entries.add(raw);
                }
                vertexDataArrays.put(name, entries);
            }
        }
    }

    /**
     * Looks up the canonical silhouette for a block. Returns null if no recipe applies — in which
     * case the caller should fall back to the JSON-model pipeline.
     *
     * @param registryName         the block's registry name (e.g. {@code "controllablesignal3"})
     * @param blockClassSimpleName the block's Java class simple name; may be null
     */
    public ResolvedModel forBlock(String registryName, String blockClassSimpleName) {
        List<String> recipe = null;
        for (RecipeEntry e : RECIPES) {
            if (e.matches(registryName, blockClassSimpleName)) {
                recipe = e.vertexDataNames;
                break;
            }
        }
        if (recipe == null) return null;

        // Union all referenced vertex-data arrays into a single Box list with the default texture.
        List<Box> outBoxes = new ArrayList<>();
        for (String name : recipe) {
            List<double[]> entries = vertexDataArrays.get(name);
            if (entries == null) continue;
            for (double[] r : entries) {
                double[] from = {r[0], r[1], r[2]};
                double[] to   = {r[3], r[4], r[5]};
                List<Face> faces = new ArrayList<>();
                for (Side s : Side.values()) {
                    faces.add(new Face(s, 0, 0, deriveUv(s, from, to)));
                }
                outBoxes.add(new Box(from, to, true, null, null, faces));
            }
        }
        if (outBoxes.isEmpty()) return null;
        List<String> patches = new ArrayList<>();
        patches.add(DEFAULT_BODY_TEXTURE_REF);
        return new ResolvedModel(outBoxes, patches, false);
    }

    /** Number of named Box arrays parsed (for the summary report). */
    public int parsedArrayCount() {
        return vertexDataArrays.size();
    }

    /** Sorted recipe identifiers for the summary. */
    public List<String> knownRecipes() {
        List<String> out = new ArrayList<>();
        for (RecipeEntry e : RECIPES) out.add(e.matchKey);
        Collections.sort(out);
        return out;
    }

    private static double parseFloatLit(String lit) {
        String s = lit.endsWith("f") || lit.endsWith("F") ? lit.substring(0, lit.length() - 1) : lit;
        return Double.parseDouble(s);
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

    private static final class RecipeEntry {
        final String matchKey;
        final java.util.function.Predicate<String> registryNameMatcher;
        final List<String> vertexDataNames;

        private RecipeEntry(String matchKey,
                            java.util.function.Predicate<String> registryNameMatcher,
                            List<String> vertexDataNames) {
            this.matchKey = matchKey;
            this.registryNameMatcher = registryNameMatcher;
            this.vertexDataNames = vertexDataNames;
        }

        static RecipeEntry byRegistryPrefix(String prefix, List<String> vertexDataNames) {
            return new RecipeEntry("prefix:" + prefix, n -> n != null && n.startsWith(prefix),
                    vertexDataNames);
        }

        static RecipeEntry byRegistryContains(List<String> substrings, List<String> vertexDataNames) {
            return new RecipeEntry("contains:" + String.join("|", substrings), n -> {
                if (n == null) return false;
                for (String s : substrings) if (n.contains(s)) return true;
                return false;
            }, vertexDataNames);
        }

        static RecipeEntry byPredicate(String label, java.util.function.Predicate<String> p,
                                       List<String> vertexDataNames) {
            return new RecipeEntry(label, p, vertexDataNames);
        }

        boolean matches(String registryName, String classSimpleName) {
            return registryNameMatcher.test(registryName);
        }
    }
}
