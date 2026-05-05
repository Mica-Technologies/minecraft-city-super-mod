package com.micatechnologies.minecraft.csm.tools.dynmap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers CSM blocks by walking the blockstates directory (registry name = filename) and
 * augments each entry with the corresponding Java source file (used in later phases for TESR
 * detection and property inference).
 */
public final class BlockDiscovery {

    /** Same regex pattern used by {@code BoundingBoxExtractionTool}. */
    private static final Pattern REGISTRY_NAME_PATTERN = Pattern.compile(
            "public\\s+String\\s+getBlockRegistryName\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+\"([a-z0-9_]+)\"\\s*;");

    private BlockDiscovery() {}

    public static final class BlockMetadata {
        public final String registryName;
        public final File blockstateFile;
        public final File javaFile; // null if not located

        BlockMetadata(String registryName, File blockstateFile, File javaFile) {
            this.registryName = registryName;
            this.blockstateFile = blockstateFile;
            this.javaFile = javaFile;
        }
    }

    /**
     * @param devEnvironmentPath repo root
     * @return map of registry name → metadata, sorted by name for deterministic output
     */
    public static Map<String, BlockMetadata> discover(File devEnvironmentPath) throws IOException {
        Map<String, File> registryToJava = buildRegistryToJavaFileMap(devEnvironmentPath);
        Map<String, BlockMetadata> out = new TreeMap<>();
        File blockstatesDir = new File(devEnvironmentPath,
                "src/main/resources/assets/csm/blockstates");
        File[] files = blockstatesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return out;
        }
        for (File bs : files) {
            String name = bs.getName().substring(0, bs.getName().length() - ".json".length());
            out.put(name, new BlockMetadata(name, bs, registryToJava.get(name)));
        }
        return out;
    }

    private static Map<String, File> buildRegistryToJavaFileMap(File devEnvironmentPath) {
        Map<String, File> map = new TreeMap<>();
        File sourceDir = new File(devEnvironmentPath,
                "src/main/java/com/micatechnologies/minecraft/csm");
        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    Matcher m = REGISTRY_NAME_PATTERN.matcher(content);
                    if (m.find()) {
                        map.put(m.group(1), p.toFile());
                    }
                } catch (IOException ignore) {}
            });
        } catch (IOException e) {
            System.err.println("Failed to walk source directory: " + e.getMessage());
        }
        return map;
    }
}
