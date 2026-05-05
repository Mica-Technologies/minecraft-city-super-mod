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

    /** Captures the base class from {@code public class Foo extends Bar [...] }. */
    private static final Pattern EXTENDS_PATTERN = Pattern.compile(
            "public\\s+(?:abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+([\\w.]+)");

    private BlockDiscovery() {}

    public static final class BlockMetadata {
        public final String registryName;
        public final File blockstateFile;
        public final File javaFile;            // null if not located
        public final String javaClassName;     // simple name; null if no Java file
        public final String javaBaseClassName; // simple name (last segment of qualified base); null if not detected

        BlockMetadata(String registryName, File blockstateFile, File javaFile,
                      String javaClassName, String javaBaseClassName) {
            this.registryName = registryName;
            this.blockstateFile = blockstateFile;
            this.javaFile = javaFile;
            this.javaClassName = javaClassName;
            this.javaBaseClassName = javaBaseClassName;
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
            File javaFile = registryToJava.get(name);
            String[] classInfo = readClassInfo(javaFile);
            out.put(name, new BlockMetadata(name, bs, javaFile, classInfo[0], classInfo[1]));
        }
        return out;
    }

    /**
     * @return [simpleClassName, simpleBaseClassName]; both null if javaFile is null or unreadable.
     */
    private static String[] readClassInfo(File javaFile) {
        if (javaFile == null) return new String[]{null, null};
        try {
            String content = Files.readString(javaFile.toPath());
            Matcher m = EXTENDS_PATTERN.matcher(content);
            if (m.find()) {
                String simple = m.group(1);
                String base = m.group(2);
                int dot = base.lastIndexOf('.');
                if (dot >= 0) base = base.substring(dot + 1);
                return new String[]{simple, base};
            }
        } catch (IOException ignore) {}
        return new String[]{null, null};
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
