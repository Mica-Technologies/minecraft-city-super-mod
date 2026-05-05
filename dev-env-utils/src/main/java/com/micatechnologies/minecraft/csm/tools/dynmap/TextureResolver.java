package com.micatechnologies.minecraft.csm.tools.dynmap;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates the model-side texture references (e.g. {@code csm:blocks/trafficsignals/shared_textures/foo})
 * used in blockstate/model JSONs into the on-disk filesystem path
 * ({@code assets/csm/textures/blocks/trafficsignals/shared_textures/foo.png}) and into a unique
 * short ID for use in Dynmap's {@code patch<i>=0:<id>} references.
 *
 * <p>IDs are derived from the path with {@code /} → {@code _} so they are unique by construction
 * across the whole texture tree.
 */
public final class TextureResolver {

    public static final String CSM_PREFIX = "csm:";
    public static final String TEXTURES_PREFIX = "assets/csm/textures/";

    private final File devEnvironmentPath;
    private final Map<String, String> idToFilename = new LinkedHashMap<>();

    public TextureResolver(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    /** Map of unique texture id → filename suitable for the {@code texture:} directive. */
    public Map<String, String> registeredTextures() {
        return idToFilename;
    }

    /**
     * Registers (if necessary) and returns the texture ID for the given model-side reference.
     *
     * @param ref e.g. {@code "csm:blocks/trafficsignals/shared_textures/metal_white"}; may also be
     *            a vanilla reference like {@code "minecraft:blocks/dirt"} (returned as-is in the
     *            filename).
     * @return unique short id, or null if the reference can't be resolved
     */
    public String register(String ref) {
        if (ref == null) return null;
        String id;
        String filename;
        if (ref.startsWith(CSM_PREFIX)) {
            String rel = ref.substring(CSM_PREFIX.length()); // e.g. "blocks/trafficsignals/shared_textures/metal_white"
            id = rel.replace('/', '_');
            filename = TEXTURES_PREFIX + rel + ".png";
        } else if (ref.startsWith("minecraft:")) {
            String rel = ref.substring("minecraft:".length());
            id = "mc_" + rel.replace('/', '_');
            filename = "assets/minecraft/textures/" + rel + ".png";
        } else if (ref.contains(":")) {
            int colon = ref.indexOf(':');
            String domain = ref.substring(0, colon);
            String rel = ref.substring(colon + 1);
            id = domain + "_" + rel.replace('/', '_');
            filename = "assets/" + domain + "/textures/" + rel + ".png";
        } else {
            id = ref.replace('/', '_');
            filename = TEXTURES_PREFIX + ref + ".png";
        }
        idToFilename.putIfAbsent(id, filename);
        return id;
    }

    /** True if the underlying PNG file exists for a CSM-domain reference. */
    public boolean csmTextureExists(String ref) {
        if (ref == null || !ref.startsWith(CSM_PREFIX)) return false;
        String rel = ref.substring(CSM_PREFIX.length());
        return new File(devEnvironmentPath, "src/main/resources/" + TEXTURES_PREFIX + rel + ".png").exists();
    }
}
