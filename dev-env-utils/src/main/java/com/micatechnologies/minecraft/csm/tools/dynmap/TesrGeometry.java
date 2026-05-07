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
 *   <li>Combines named arrays — or applies hard-coded box silhouettes — per the {@link #RECIPES}
 *       table to produce a canonical shape for each TESR-rendered block class.</li>
 * </ol>
 *
 * <p>Coverage:
 * <ul>
 *   <li>Vehicle / pedestrian signals + blankout boxes + lane control signal: vertex-data parsed.</li>
 *   <li>Portable / overhead message signs, portable / overhead speed limit signs, dynamic guide
 *       sign: hard-coded silhouettes (the rendered geometry extends well beyond Dynmap's [-1, 2]
 *       window so we silhouette the in-range portion only).</li>
 * </ul>
 *
 * <p>The remaining TESR-rendered blocks (fire alarm strobe, emergency light, HVAC thermostat,
 * traffic beacons, mount kit, tattle-tale beacon) already have correct static JSON geometry —
 * their TESRs only paint glow/text effects on top, which Dynmap can't represent regardless.
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

    /**
     * Hard-coded box silhouettes for blocks whose visible geometry is drawn inline in their
     * TESR rather than from a {@code *VertexData} class. Each entry is six floats:
     * {@code {fromX, fromY, fromZ, toX, toY, toZ}} in 0..16 model units. Boxes may extend
     * to {@code [-16, 32]} (one block past each face) — Dynmap's {@code [-1, 2]} unit-space
     * window — to silhouette TESR geometry that hangs outside the host block.
     *
     * <p>Coordinates are in the block's <b>default-facing</b> local frame (the renderer's
     * {@code rotY=0} branch). Variant rotation from the blockstate is applied later in the
     * {@code modellist:} pipeline via the trailing {@code R/x/y/z} token.
     */
    private static final List<double[]> PORTABLE_MESSAGE_SIGN_BOXES = Arrays.asList(
            // Trailer (TileEntityPortableMessageSignRenderer TRAILER_*: 19.5 × 6 × 36, offset +4 in z).
            new double[]{-1.75, 4.5, -6.0, 17.75, 10.5, 30.0},
            // Lower portion of the mast (MAST_SIZE=4.5, extends to y=40.5, clamped to y=32).
            new double[]{5.75, 10.5, 5.75, 10.25, 32.0, 10.25}
    );

    private static final List<double[]> PORTABLE_SPEED_LIMIT_SIGN_BOXES = Arrays.asList(
            // Same trailer as the message sign (TileEntityPortableSpeedLimitRenderer TRAILER_*).
            new double[]{-1.75, 4.5, -6.0, 17.75, 10.5, 30.0},
            // Lower portion of the mast (MAST_HEIGHT=24, extends to y=34.5, clamped to y=32).
            new double[]{5.75, 10.5, 5.75, 10.25, 32.0, 10.25}
    );

    private static final List<double[]> OVERHEAD_MESSAGE_SIGN_BOXES = Arrays.asList(
            // Sign panel: 144W × 64.02H × 20D centered on the host block (CX=8, CY=8) with
            // FACE_Z=-4 and BACK_Z=16 (TileEntityOverheadMessageSignRenderer SIGN_*). The
            // panel extends well beyond the host so we clamp to Dynmap's [-16, 32] window.
            new double[]{-16.0, -16.0, -4.0, 32.0, 32.0, 16.0}
    );

    private static final List<double[]> OVERHEAD_SPEED_LIMIT_SIGN_BOXES = Arrays.asList(
            // Sign panel: 48W × 64H × 12D centered on the host block (CX=8, CY=8) with
            // FACE_Z=4 and BACK_Z=16 (TileEntityOverheadSpeedLimitRenderer SIGN_*). Wider
            // and shorter than the message sign; clamp the same way.
            new double[]{-16.0, -16.0, 4.0, 32.0, 32.0, 16.0}
    );

    private static final List<double[]> DYNAMIC_GUIDE_SIGN_BOXES = Arrays.asList(
            // The panel is variable per-sign (computeTotalSignWidth/Height in the renderer);
            // the minimum height is 16 (one block) and typical exit signs are 2–3 blocks wide.
            // We silhouette a representative 3-wide × 2-tall back-of-block panel: (faceZ=14.5,
            // BACK_Z=16). Users with large signs see roughly the right footprint; users with
            // single-block signs see a slight overshoot, which is preferable to silhouetting
            // only the host block on a multi-block sign.
            new double[]{-16.0, -8.0, 13.0, 32.0, 24.0, 15.0}
    );

    private static final List<RecipeEntry> RECIPES = Arrays.asList(
            // Crosswalk button/tweeter blocks — keep these as JSON models (small accessories);
            // skip them with an empty recipe (matched only to suppress fallthrough).
            RecipeEntry.byRegistryContains(Arrays.asList("crosswalkbutton", "crosswalktweeter"),
                    Collections.emptyList()),
            // Crosswalk signal bodies (any "crosswalk" not caught above).
            RecipeEntry.byRegistryContains(Arrays.asList("crosswalk"), CROSSWALK_PARTS),
            // Blankout boxes.
            RecipeEntry.byRegistryContains(Arrays.asList("blankout"), BLANKOUT_PARTS),
            // Lane control signal — same body+visor shape as a blankout box.
            RecipeEntry.byRegistryContains(Arrays.asList("lane_control_signal"), BLANKOUT_PARTS),
            // Portable message sign: trailer + lower mast.
            RecipeEntry.hardCoded("portable_message_sign",
                    n -> "portable_message_sign".equals(n),
                    PORTABLE_MESSAGE_SIGN_BOXES),
            // Portable speed limit sign: same trailer footprint, shorter mast.
            RecipeEntry.hardCoded("portable_speed_limit_sign",
                    n -> "portable_speed_limit_sign".equals(n),
                    PORTABLE_SPEED_LIMIT_SIGN_BOXES),
            // Overhead message sign: full panel clamped to Dynmap's window.
            RecipeEntry.hardCoded("overhead_message_sign",
                    n -> "overhead_message_sign".equals(n),
                    OVERHEAD_MESSAGE_SIGN_BOXES),
            // Overhead speed limit sign: shorter, narrower panel.
            RecipeEntry.hardCoded("overhead_speed_limit_sign",
                    n -> "overhead_speed_limit_sign".equals(n),
                    OVERHEAD_SPEED_LIMIT_SIGN_BOXES),
            // Dynamic guide sign: representative multi-block exit sign panel.
            RecipeEntry.hardCoded("dynamic_guide_sign",
                    n -> "dynamic_guide_sign".equals(n),
                    DYNAMIC_GUIDE_SIGN_BOXES),
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

    /**
     * Pattern matching one {@code new Box(new float[]{x, y, z}, new float[]{x, y, z})} entry.
     * Each captured component may be a literal float, a named float constant, or a simple
     * {@code +}/{@code -} expression of either — resolved later via {@link #parseFloatConstants}.
     */
    private static final Pattern BOX_ENTRY_PATTERN = Pattern.compile(
            "new\\s+Box\\s*\\(\\s*new\\s+float\\s*\\[\\s*\\]\\s*\\{\\s*"
                    + "([^,}]+?)\\s*,\\s*([^,}]+?)\\s*,\\s*([^,}]+?)\\s*\\}\\s*,\\s*"
                    + "new\\s+float\\s*\\[\\s*\\]\\s*\\{\\s*"
                    + "([^,}]+?)\\s*,\\s*([^,}]+?)\\s*,\\s*([^,}]+?)\\s*\\}\\s*\\)");

    /** Pattern matching {@code static final float NAME = expr;} declarations. */
    private static final Pattern FLOAT_CONST_PATTERN = Pattern.compile(
            "(?:public|private|protected)?\\s*static\\s+final\\s+float\\s+(\\w+)\\s*=\\s*([^;]+);");

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
            Map<String, Double> floatConsts = parseFloatConstants(content);
            Matcher m = LIST_BOX_PATTERN.matcher(content);
            while (m.find()) {
                String name = m.group(1);
                String body = m.group(2);
                List<double[]> entries = new ArrayList<>();
                Matcher bm = BOX_ENTRY_PATTERN.matcher(body);
                while (bm.find()) {
                    double[] raw = new double[]{
                            evalExpr(bm.group(1), floatConsts), evalExpr(bm.group(2), floatConsts),
                            evalExpr(bm.group(3), floatConsts), evalExpr(bm.group(4), floatConsts),
                            evalExpr(bm.group(5), floatConsts), evalExpr(bm.group(6), floatConsts)
                    };
                    entries.add(raw);
                }
                vertexDataArrays.put(name, entries);
            }
        }
    }

    /**
     * Parses {@code static final float NAME = expr;} declarations in declaration order, evaluating
     * each {@code expr} as a sum/difference of literals and previously-declared constants. This
     * lets the box-array regex resolve named references like {@code BODY_X_MIN + BEZEL_INSET}.
     */
    private static Map<String, Double> parseFloatConstants(String content) {
        Map<String, Double> consts = new LinkedHashMap<>();
        Matcher m = FLOAT_CONST_PATTERN.matcher(content);
        while (m.find()) {
            String name = m.group(1);
            String expr = m.group(2);
            try {
                consts.put(name, evalExpr(expr, consts));
            } catch (RuntimeException ignored) {
                // Skip declarations we can't evaluate (e.g. function calls). They can't contribute
                // to a Box literal anyway.
            }
        }
        return consts;
    }

    /**
     * Evaluates a {@code +}/{@code -} expression of literal floats and named constants. Returns
     * 0 for unrecognised tokens (e.g. references to constants we haven't parsed) — the caller
     * should treat this as a soft failure.
     */
    private static double evalExpr(String expr, Map<String, Double> consts) {
        String s = expr.replaceAll("\\s+", "");
        if (s.isEmpty()) return 0;
        double sum = 0;
        char sign = '+';
        StringBuilder term = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '+' || c == '-')) {
                if (term.length() == 0 && i == 0) {
                    // Leading sign on the first term.
                    sign = c;
                    continue;
                }
                if (term.length() > 0) {
                    sum = applySign(sum, sign, evalTerm(term.toString(), consts));
                    sign = c;
                    term.setLength(0);
                    continue;
                }
            }
            term.append(c);
        }
        if (term.length() > 0) {
            sum = applySign(sum, sign, evalTerm(term.toString(), consts));
        }
        return sum;
    }

    private static double applySign(double sum, char sign, double v) {
        return sign == '-' ? sum - v : sum + v;
    }

    private static double evalTerm(String t, Map<String, Double> consts) {
        // Strip a trailing 'f' or 'F' suffix on float literals.
        if (t.endsWith("f") || t.endsWith("F")) t = t.substring(0, t.length() - 1);
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ignored) {}
        Double v = consts.get(t);
        return v != null ? v : 0;
    }

    /**
     * Looks up the canonical silhouette for a block. Returns null if no recipe applies — in which
     * case the caller should fall back to the JSON-model pipeline.
     *
     * @param registryName         the block's registry name (e.g. {@code "controllablesignal3"})
     * @param blockClassSimpleName the block's Java class simple name; may be null
     */
    public ResolvedModel forBlock(String registryName, String blockClassSimpleName) {
        RecipeEntry recipe = null;
        for (RecipeEntry e : RECIPES) {
            if (e.matches(registryName, blockClassSimpleName)) {
                recipe = e;
                break;
            }
        }
        if (recipe == null) return null;

        // Resolve box list: hard-coded boxes if provided, else the union of named vertex-data arrays.
        List<double[]> boxData = new ArrayList<>();
        if (recipe.hardCodedBoxes != null) {
            boxData.addAll(recipe.hardCodedBoxes);
        } else if (recipe.vertexDataNames != null) {
            for (String name : recipe.vertexDataNames) {
                List<double[]> entries = vertexDataArrays.get(name);
                if (entries != null) boxData.addAll(entries);
            }
        }

        List<Box> outBoxes = new ArrayList<>();
        for (double[] r : boxData) {
            double[] from = {r[0], r[1], r[2]};
            double[] to   = {r[3], r[4], r[5]};
            List<Face> faces = new ArrayList<>();
            for (Side s : Side.values()) {
                faces.add(new Face(s, 0, 0, deriveUv(s, from, to)));
            }
            outBoxes.add(new Box(from, to, true, null, null, faces));
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
        /** Names of {@code List<Box>} arrays in {@link #vertexDataArrays} to union. Mutually exclusive with {@link #hardCodedBoxes}. */
        final List<String> vertexDataNames;
        /** Hard-coded box silhouettes (six floats: fromX/Y/Z toX/Y/Z) when no VertexData class applies. */
        final List<double[]> hardCodedBoxes;

        private RecipeEntry(String matchKey,
                            java.util.function.Predicate<String> registryNameMatcher,
                            List<String> vertexDataNames,
                            List<double[]> hardCodedBoxes) {
            this.matchKey = matchKey;
            this.registryNameMatcher = registryNameMatcher;
            this.vertexDataNames = vertexDataNames;
            this.hardCodedBoxes = hardCodedBoxes;
        }

        static RecipeEntry byRegistryPrefix(String prefix, List<String> vertexDataNames) {
            return new RecipeEntry("prefix:" + prefix, n -> n != null && n.startsWith(prefix),
                    vertexDataNames, null);
        }

        static RecipeEntry byRegistryContains(List<String> substrings, List<String> vertexDataNames) {
            return new RecipeEntry("contains:" + String.join("|", substrings), n -> {
                if (n == null) return false;
                for (String s : substrings) if (n.contains(s)) return true;
                return false;
            }, vertexDataNames, null);
        }

        static RecipeEntry byPredicate(String label, java.util.function.Predicate<String> p,
                                       List<String> vertexDataNames) {
            return new RecipeEntry(label, p, vertexDataNames, null);
        }

        static RecipeEntry hardCoded(String label, java.util.function.Predicate<String> p,
                                     List<double[]> boxes) {
            return new RecipeEntry(label, p, null, boxes);
        }

        boolean matches(String registryName, String classSimpleName) {
            return registryNameMatcher.test(registryName);
        }
    }
}
