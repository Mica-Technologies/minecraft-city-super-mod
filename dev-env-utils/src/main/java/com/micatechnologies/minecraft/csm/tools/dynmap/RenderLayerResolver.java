package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.BlockDiscovery.BlockMetadata;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Transparency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Resolves a block's {@link Transparency} value for the Dynmap renderdata output by reading the
 * Forge {@code BlockRenderLayer} that the block returns from its {@code getBlockRenderLayer()}
 * method.
 *
 * <p>Mapping:
 * <ul>
 *   <li>{@code SOLID}, or {@code null} (vanilla default for fences/slabs/stairs) &rarr; {@link Transparency#OPAQUE}</li>
 *   <li>{@code CUTOUT}, {@code CUTOUT_MIPPED} &rarr; {@link Transparency#TRANSPARENT}</li>
 *   <li>{@code TRANSLUCENT} &rarr; {@link Transparency#SEMITRANSPARENT}</li>
 *   <li>otherwise &rarr; {@link Transparency#TRANSPARENT} (safe fallback that keeps cutout
 *       textures visible)</li>
 * </ul>
 *
 * <p>Two source locations are scanned:
 * <ul>
 *   <li>The block's own Java file (most blocks) — its {@code getBlockRenderLayer()} return.</li>
 *   <li>For blocks instantiated via {@code BlockRotatableNSEWUDFactory} in tab files (no
 *       per-block Java file), the layer is the 14th constructor argument; we extract it by
 *       matching {@code new BlockRotatableNSEWUDFactory("registry_name", ..., BlockRenderLayer.X, ...)}.</li>
 * </ul>
 */
public final class RenderLayerResolver {

    private static final Pattern GET_LAYER_RETURN = Pattern.compile(
            "public\\s+BlockRenderLayer\\s+getBlockRenderLayer\\s*\\(\\s*\\)\\s*\\{\\s*"
                    + "return\\s+(BlockRenderLayer\\.[A-Z_]+|null)\\s*;");

    /** Captures e.g. {@code new BlockRotatableNSEWUDFactory("foo_bar", ...)} up to the closing paren. */
    private static final Pattern FACTORY_CALL = Pattern.compile(
            "new\\s+BlockRotatableNSEWUDFactory\\s*\\(\\s*\"([a-z0-9_]+)\"([^;]*?)\\)\\s*\\)?\\s*;",
            Pattern.DOTALL);

    private static final Pattern LAYER_TOKEN = Pattern.compile(
            "BlockRenderLayer\\.([A-Z_]+)");

    private final File devEnvironmentPath;
    /** registry name &rarr; layer keyword (e.g. "SOLID"). Lazily populated. */
    private final Map<String, String> factoryLayers = new HashMap<>();
    /** java-file path &rarr; layer keyword or "null" or "" (no method match). */
    private final Map<String, String> fileCache = new HashMap<>();
    private boolean factoryScanned;

    public RenderLayerResolver(File devEnvironmentPath) {
        this.devEnvironmentPath = devEnvironmentPath;
    }

    /** Number of registry names successfully picked up from factory calls in tab files. */
    public int factoryEntryCount() {
        ensureFactoryScanned();
        return factoryLayers.size();
    }

    public Transparency forBlock(BlockMetadata bm) {
        String layer = lookupLayer(bm);
        return mapLayer(layer);
    }

    /**
     * @return the {@code BlockRenderLayer.X} suffix (e.g. "SOLID"), the literal "null" if the
     *         method returns {@code null}, or {@code null} if no source was found.
     */
    private String lookupLayer(BlockMetadata bm) {
        // Direct lookup in the block's Java file, walking the parent chain.
        if (bm.javaFile != null) {
            String layer = scanFileWithInheritance(bm.javaFile);
            if (layer != null) return layer;
        }
        // Factory-instantiated blocks have no Java file; consult the tab scan.
        ensureFactoryScanned();
        return factoryLayers.get(bm.registryName);
    }

    private static Transparency mapLayer(String layerKeyword) {
        if (layerKeyword == null) return Transparency.TRANSPARENT;
        switch (layerKeyword) {
            case "SOLID":
            case "null":            // vanilla-default fences/slabs/stairs render as solid on the map.
                return Transparency.OPAQUE;
            case "CUTOUT":
            case "CUTOUT_MIPPED":
                return Transparency.TRANSPARENT;
            case "TRANSLUCENT":
                return Transparency.SEMITRANSPARENT;
            default:
                return Transparency.TRANSPARENT;
        }
    }

    private String scanFileWithInheritance(File javaFile) {
        File current = javaFile;
        for (int hops = 0; hops < 4 && current != null; hops++) {
            String layer = scanFile(current);
            if (layer != null) return layer;
            current = parentFile(current);
        }
        return null;
    }

    private String scanFile(File file) {
        String key = file.getAbsolutePath();
        if (fileCache.containsKey(key)) {
            String v = fileCache.get(key);
            return v.isEmpty() ? null : v;
        }
        String layer = null;
        try {
            String content = Files.readString(file.toPath());
            Matcher m = GET_LAYER_RETURN.matcher(content);
            if (m.find()) {
                String token = m.group(1);
                if (token.equals("null")) {
                    layer = "null";
                } else {
                    int dot = token.indexOf('.');
                    layer = (dot >= 0) ? token.substring(dot + 1) : token;
                }
            }
        } catch (IOException ignore) {}
        fileCache.put(key, layer == null ? "" : layer);
        return layer;
    }

    /** Walks up one inheritance level by looking for the file declaring the parent class name. */
    private File parentFile(File child) {
        try {
            String content = Files.readString(child.toPath());
            Matcher m = Pattern.compile("class\\s+\\w+\\s+extends\\s+([\\w.]+)").matcher(content);
            if (!m.find()) return null;
            String parent = m.group(1);
            int dot = parent.lastIndexOf('.');
            if (dot >= 0) parent = parent.substring(dot + 1);
            // Stop walking at framework roots that do not override.
            if (parent.equals("Block") || parent.equals("Object")
                    || parent.equals("BlockSlab") || parent.equals("BlockFence")
                    || parent.equals("BlockStairs")) {
                return null;
            }
            File codeutils = new File(devEnvironmentPath,
                    "src/main/java/com/micatechnologies/minecraft/csm/codeutils/" + parent + ".java");
            if (codeutils.exists()) return codeutils;
            // Otherwise scan src/main/java for a class file with that simple name.
            return findClassFile(parent);
        } catch (IOException ignore) {
            return null;
        }
    }

    private File findClassFile(String simpleName) {
        File sourceDir = new File(devEnvironmentPath,
                "src/main/java/com/micatechnologies/minecraft/csm");
        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            return paths.filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .map(Path::toFile).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void ensureFactoryScanned() {
        if (factoryScanned) return;
        factoryScanned = true;
        File tabsDir = new File(devEnvironmentPath,
                "src/main/java/com/micatechnologies/minecraft/csm/tabs");
        if (!tabsDir.isDirectory()) return;
        File[] files = tabsDir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) return;
        for (File f : files) {
            try {
                String content = Files.readString(f.toPath());
                Matcher m = FACTORY_CALL.matcher(content);
                while (m.find()) {
                    String name = m.group(1);
                    String args = m.group(2);
                    Matcher lm = LAYER_TOKEN.matcher(args);
                    if (lm.find()) {
                        factoryLayers.put(name, lm.group(1));
                    }
                }
            } catch (IOException ignore) {}
        }
    }
}
