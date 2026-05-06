package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses Forge-format ({@code "forge_marker": 1}) and vanilla-format blockstate JSONs and expands
 * them into a list of resolved variants (one per cartesian product point of the variant properties).
 *
 * <p>Multipart blockstates (as used by the 15 fence blocks) are properly expanded to one resolved
 * variant per applicable boolean-property combination, with each variant carrying the list of
 * matching {@link Apply} clauses (Phase 10).
 *
 * <p>Vanilla-format and {@code .obj}-referencing blockstates are detected and reported as a single
 * "fallback" resolved variant carrying just the inferred top-level model + textures, signalling the
 * caller to dispatch to the appropriate handler.
 */
public final class BlockstateExpander {

    private BlockstateExpander() {}

    public enum Kind { FORGE, VANILLA, MULTIPART, OBJ, EMPTY }

    /** One {@code apply} clause referenced by a multipart entry. */
    public static final class Apply {
        public final String model;
        public final int xRotation;
        public final int yRotation;
        public final boolean uvlock;

        public Apply(String model, int xRotation, int yRotation, boolean uvlock) {
            this.model = model;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
            this.uvlock = uvlock;
        }
    }

    /** One resolved (block, state) combo produced by expanding a blockstate JSON. */
    public static final class ResolvedVariant {
        public final Map<String, String> stateMap;     // state key → value (in declaration order)
        public final String model;                     // resolved model reference, e.g. "csm:trafficsignals/shared_models/foo"
        public final Map<String, String> textures;     // resolved texture key → reference (e.g. "csm:blocks/.../foo")
        public final int xRotation;
        public final int yRotation;
        public final boolean uvlock;
        /** Multipart-only: list of applies whose {@code when} clause matched this state. Null otherwise. */
        public final List<Apply> multipartApplies;

        public ResolvedVariant(Map<String, String> stateMap, String model,
                               Map<String, String> textures, int xRotation, int yRotation,
                               boolean uvlock) {
            this(stateMap, model, textures, xRotation, yRotation, uvlock, null);
        }

        public ResolvedVariant(Map<String, String> stateMap, String model,
                               Map<String, String> textures, int xRotation, int yRotation,
                               boolean uvlock, List<Apply> multipartApplies) {
            this.stateMap = stateMap;
            this.model = model;
            this.textures = textures;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
            this.uvlock = uvlock;
            this.multipartApplies = multipartApplies;
        }
    }

    public static final class ExpandedBlockstate {
        public final Kind kind;
        public final List<ResolvedVariant> variants;

        public ExpandedBlockstate(Kind kind, List<ResolvedVariant> variants) {
            this.kind = kind;
            this.variants = variants;
        }
    }

    public static ExpandedBlockstate expand(File blockstateFile) throws IOException {
        String json = Files.readString(blockstateFile.toPath());
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new ExpandedBlockstate(Kind.EMPTY, new ArrayList<>());
        }

        if (root.has("multipart")) {
            return expandMultipart(root);
        }

        boolean isForge = root.has("forge_marker") && root.get("forge_marker").getAsInt() == 1;
        if (!isForge) {
            return new ExpandedBlockstate(Kind.VANILLA, fallbackSingle(root));
        }

        JsonObject defaults = root.has("defaults") ? root.getAsJsonObject("defaults") : new JsonObject();
        if (!root.has("variants")) {
            return new ExpandedBlockstate(Kind.EMPTY, new ArrayList<>());
        }
        JsonObject variants = root.getAsJsonObject("variants");

        // Detect .obj reference in the defaults model.
        String defaultModel = defaults.has("model") ? defaults.get("model").getAsString() : null;
        boolean isObj = defaultModel != null && defaultModel.endsWith(".obj");

        // Collect variant property keys (excluding inventory/normal — those are render-context selectors,
        // not real block properties).
        Map<String, List<String>> propertyValues = new LinkedHashMap<>();
        JsonObject normalOverride = null;
        for (Map.Entry<String, JsonElement> e : variants.entrySet()) {
            String key = e.getKey();
            if ("inventory".equals(key)) continue;
            if ("normal".equals(key)) {
                normalOverride = unwrap(e.getValue());
                continue;
            }
            JsonObject inner = e.getValue().getAsJsonObject();
            List<String> values = new ArrayList<>(inner.keySet());
            propertyValues.put(key, values);
        }

        List<ResolvedVariant> resolved = new ArrayList<>();
        if (propertyValues.isEmpty()) {
            // Only normal/inventory present → single variant with no state.
            ResolvedVariant rv = mergeChain(defaults, normalOverride, new LinkedHashMap<>(),
                    new LinkedHashMap<>(), variants);
            resolved.add(rv);
            return new ExpandedBlockstate(isObj ? Kind.OBJ : Kind.FORGE, resolved);
        }

        // Cartesian product over propertyValues.
        List<Map<String, String>> combos = cartesian(propertyValues);
        for (Map<String, String> combo : combos) {
            ResolvedVariant rv = mergeChain(defaults, normalOverride, combo, propertyValues, variants);
            resolved.add(rv);
        }
        return new ExpandedBlockstate(isObj ? Kind.OBJ : Kind.FORGE, resolved);
    }

    /**
     * Expands a multipart blockstate. Discovers all property names that appear in {@code when}
     * clauses (assuming boolean-valued — the standard fence pattern), enumerates the
     * cartesian product of {@code {true, false}} for each, and emits one resolved variant per
     * combination with the matching applies attached.
     */
    private static ExpandedBlockstate expandMultipart(JsonObject root) {
        JsonArray multipart = root.getAsJsonArray("multipart");

        // Collect the set of property names referenced by any when clause.
        Set<String> propNames = new LinkedHashSet<>();
        for (JsonElement me : multipart) {
            JsonObject mp = me.getAsJsonObject();
            if (!mp.has("when")) continue;
            collectWhenProperties(mp.getAsJsonObject("when"), propNames);
        }

        // For each property name, enumerate {false, true}. (Fences are the only multipart case in
        // CSM and they use boolean connection properties.)
        Map<String, List<String>> propValues = new LinkedHashMap<>();
        for (String p : propNames) {
            propValues.put(p, Arrays.asList("false", "true"));
        }

        List<ResolvedVariant> resolved = new ArrayList<>();
        if (propValues.isEmpty()) {
            // No when clauses → single state with all unconditional applies.
            List<Apply> applies = new ArrayList<>();
            for (JsonElement me : multipart) {
                JsonObject mp = me.getAsJsonObject();
                applies.addAll(readApplies(mp.get("apply")));
            }
            resolved.add(new ResolvedVariant(new LinkedHashMap<>(), null, new LinkedHashMap<>(),
                    0, 0, false, applies));
            return new ExpandedBlockstate(Kind.MULTIPART, resolved);
        }

        List<Map<String, String>> combos = cartesian(propValues);
        for (Map<String, String> combo : combos) {
            List<Apply> applies = new ArrayList<>();
            for (JsonElement me : multipart) {
                JsonObject mp = me.getAsJsonObject();
                if (matchesWhen(mp, combo)) {
                    applies.addAll(readApplies(mp.get("apply")));
                }
            }
            resolved.add(new ResolvedVariant(combo, null, new LinkedHashMap<>(),
                    0, 0, false, applies));
        }
        return new ExpandedBlockstate(Kind.MULTIPART, resolved);
    }

    private static void collectWhenProperties(JsonObject when, Set<String> out) {
        for (Map.Entry<String, JsonElement> e : when.entrySet()) {
            if ("OR".equals(e.getKey())) {
                for (JsonElement orEl : e.getValue().getAsJsonArray()) {
                    collectWhenProperties(orEl.getAsJsonObject(), out);
                }
            } else {
                out.add(e.getKey());
            }
        }
    }

    private static boolean matchesWhen(JsonObject mp, Map<String, String> combo) {
        if (!mp.has("when")) return true;
        return matchesClause(mp.getAsJsonObject("when"), combo);
    }

    private static boolean matchesClause(JsonObject when, Map<String, String> combo) {
        for (Map.Entry<String, JsonElement> e : when.entrySet()) {
            if ("OR".equals(e.getKey())) {
                boolean any = false;
                for (JsonElement sub : e.getValue().getAsJsonArray()) {
                    if (matchesClause(sub.getAsJsonObject(), combo)) {
                        any = true;
                        break;
                    }
                }
                if (!any) return false;
            } else {
                String required = e.getValue().getAsString();
                String actual = combo.getOrDefault(e.getKey(), "");
                boolean ok = false;
                for (String alt : required.split("\\|")) {
                    if (alt.equals(actual)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) return false;
            }
        }
        return true;
    }

    private static List<Apply> readApplies(JsonElement applyEl) {
        if (applyEl == null) return Collections.emptyList();
        List<Apply> out = new ArrayList<>();
        if (applyEl.isJsonArray()) {
            // Random-selection list — for renderdata, the first variant is sufficient.
            JsonArray arr = applyEl.getAsJsonArray();
            if (arr.size() > 0) out.add(readApply(arr.get(0).getAsJsonObject()));
        } else if (applyEl.isJsonObject()) {
            out.add(readApply(applyEl.getAsJsonObject()));
        }
        return out;
    }

    private static Apply readApply(JsonObject ap) {
        String model = ap.has("model") ? ap.get("model").getAsString() : null;
        int x = ap.has("x") ? ap.get("x").getAsInt() : 0;
        int y = ap.has("y") ? ap.get("y").getAsInt() : 0;
        boolean uvlock = ap.has("uvlock") && ap.get("uvlock").getAsBoolean();
        return new Apply(model, x, y, uvlock);
    }

    /**
     * Builds a single fallback resolved variant from the top-level defaults — used for vanilla and
     * other formats we don't expand fully.
     */
    private static List<ResolvedVariant> fallbackSingle(JsonObject root) {
        List<ResolvedVariant> out = new ArrayList<>();
        JsonObject defaults = root.has("defaults") ? root.getAsJsonObject("defaults") : new JsonObject();
        String model = defaults.has("model") ? defaults.get("model").getAsString() : null;
        Map<String, String> textures = readTextures(defaults);
        int x = defaults.has("x") ? defaults.get("x").getAsInt() : 0;
        int y = defaults.has("y") ? defaults.get("y").getAsInt() : 0;
        boolean uvlock = defaults.has("uvlock") && defaults.get("uvlock").getAsBoolean();
        out.add(new ResolvedVariant(new LinkedHashMap<>(), model, textures, x, y, uvlock));
        return out;
    }

    /**
     * Merges {@code defaults → normal → each property override} for one cartesian combination.
     */
    private static ResolvedVariant mergeChain(JsonObject defaults, JsonObject normalOverride,
                                              Map<String, String> combo,
                                              Map<String, List<String>> propertyValues,
                                              JsonObject variants) {
        String model = defaults.has("model") ? defaults.get("model").getAsString() : null;
        Map<String, String> textures = readTextures(defaults);
        int x = defaults.has("x") ? defaults.get("x").getAsInt() : 0;
        int y = defaults.has("y") ? defaults.get("y").getAsInt() : 0;
        boolean uvlock = defaults.has("uvlock") && defaults.get("uvlock").getAsBoolean();

        // Apply normal override first (rare but legal).
        Override normal = readOverride(normalOverride);
        if (normal != null) {
            if (normal.model != null) model = normal.model;
            textures.putAll(normal.textures);
            x += normal.x; y += normal.y;
            if (normal.uvlock != null) uvlock = normal.uvlock;
        }

        // Apply each property's override in declaration order.
        for (Map.Entry<String, String> e : combo.entrySet()) {
            String prop = e.getKey();
            String val = e.getValue();
            JsonObject propValues = variants.getAsJsonObject(prop);
            JsonObject overrideObj = unwrap(propValues.get(val));
            Override ov = readOverride(overrideObj);
            if (ov == null) continue;
            if (ov.model != null) model = ov.model;
            textures.putAll(ov.textures);
            x += ov.x; y += ov.y;
            if (ov.uvlock != null) uvlock = ov.uvlock;
        }

        // Normalise rotation to [0, 360).
        x = ((x % 360) + 360) % 360;
        y = ((y % 360) + 360) % 360;

        return new ResolvedVariant(combo, model, textures, x, y, uvlock);
    }

    private static final class Override {
        String model;
        Map<String, String> textures = new LinkedHashMap<>();
        int x, y;
        Boolean uvlock;
    }

    private static Override readOverride(JsonObject obj) {
        if (obj == null || obj.entrySet().isEmpty()) return null;
        Override ov = new Override();
        if (obj.has("model")) ov.model = obj.get("model").getAsString();
        if (obj.has("textures")) {
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("textures").entrySet()) {
                ov.textures.put(e.getKey(), e.getValue().getAsString());
            }
        }
        if (obj.has("x")) ov.x = obj.get("x").getAsInt();
        if (obj.has("y")) ov.y = obj.get("y").getAsInt();
        if (obj.has("uvlock")) ov.uvlock = obj.get("uvlock").getAsBoolean();

        // Forge-format transform.rotation array: [{ "x": 0 }, { "y": 90 }, { "z": 0 }]
        if (obj.has("transform")) {
            JsonObject transform = obj.getAsJsonObject("transform");
            if (transform.has("rotation")) {
                JsonArray rot = transform.getAsJsonArray("rotation");
                for (JsonElement re : rot) {
                    JsonObject ro = re.getAsJsonObject();
                    if (ro.has("x")) ov.x += ro.get("x").getAsInt();
                    if (ro.has("y")) ov.y += ro.get("y").getAsInt();
                }
            }
        }
        return ov;
    }

    private static JsonObject unwrap(JsonElement e) {
        if (e == null) return null;
        if (e.isJsonArray()) {
            JsonArray arr = e.getAsJsonArray();
            return arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
        }
        return e.getAsJsonObject();
    }

    private static Map<String, String> readTextures(JsonObject obj) {
        Map<String, String> out = new LinkedHashMap<>();
        if (obj.has("textures")) {
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("textures").entrySet()) {
                out.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return out;
    }

    /** Maximum cartesian-product size we'll expand. Beyond this, we fall back to a single variant. */
    public static final int MAX_VARIANT_EXPANSION = 256;

    private static List<Map<String, String>> cartesian(Map<String, List<String>> propertyValues) {
        long expectedSize = 1;
        for (List<String> v : propertyValues.values()) {
            expectedSize *= Math.max(1, v.size());
            if (expectedSize > MAX_VARIANT_EXPANSION) {
                // Bail out: too many combinations. Return a single empty combo (caller will treat as
                // "no specific state" and emit one row, which Dynmap matches against all states).
                List<Map<String, String>> bail = new ArrayList<>();
                bail.add(new LinkedHashMap<>());
                return bail;
            }
        }
        List<Map<String, String>> out = new ArrayList<>();
        out.add(new LinkedHashMap<>());
        for (Map.Entry<String, List<String>> e : propertyValues.entrySet()) {
            List<Map<String, String>> next = new ArrayList<>();
            for (Map<String, String> partial : out) {
                for (String v : e.getValue()) {
                    Map<String, String> copy = new LinkedHashMap<>(partial);
                    copy.put(e.getKey(), v);
                    next.add(copy);
                }
            }
            out = next;
        }
        return out;
    }
}
