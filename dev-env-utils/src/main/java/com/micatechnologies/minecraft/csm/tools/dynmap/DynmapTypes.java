package com.micatechnologies.minecraft.csm.tools.dynmap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data types for Dynmap renderdata generation. Mirrors the on-disk format documented in
 * {@code assets/docs/agent_progress/DYNMAP_RENDERDATA_GENERATOR_PLAN.md}.
 */
public final class DynmapTypes {

    private DynmapTypes() {}

    public enum Side {
        DOWN("d"), UP("u"), NORTH("n"), SOUTH("s"), WEST("w"), EAST("e");

        public final String token;

        Side(String token) { this.token = token; }

        public static Side fromMinecraftFace(String face) {
            switch (face) {
                case "down":  return DOWN;
                case "up":    return UP;
                case "north": return NORTH;
                case "south": return SOUTH;
                case "west":  return WEST;
                case "east":  return EAST;
                default: throw new IllegalArgumentException("Unknown face: " + face);
            }
        }
    }

    /** A single texture file declaration for the {@code texture:} directive. */
    public static final class TextureRecord {
        public final String id;          // short ID used by patch references
        public final String filename;    // assets/csm/textures/blocks/.../foo.png
        public final int xcount;
        public final int ycount;

        public TextureRecord(String id, String filename, int xcount, int ycount) {
            this.id = id;
            this.filename = filename;
            this.xcount = xcount;
            this.ycount = ycount;
        }

        public String emit() {
            return String.format(Locale.US,
                    "texture:id=%s,filename=%s,xcount=%d,ycount=%d",
                    id, filename, xcount, ycount);
        }
    }

    /** A face on a {@link Box}: which side, which texture index (into the per-block patch list), UVs and rotation. */
    public static final class Face {
        public final Side side;
        public final int textureIndex;          // into BlockRecord.patches
        public final int textureRotation;       // 0/90/180/270
        public final double[] uv;               // {umin, vmin, umax, vmax} in 0..16

        public Face(Side side, int textureIndex, int textureRotation, double[] uv) {
            this.side = side;
            this.textureIndex = textureIndex;
            this.textureRotation = textureRotation;
            this.uv = uv;
        }
    }

    /**
     * One element in a model: a cuboid {@code from}/{@code to} (in 0..16 model space) plus per-face
     * texture mappings, optional element-level rotation, and a shade flag.
     */
    public static final class Box {
        public final double[] from;       // 0..16
        public final double[] to;         // 0..16
        public final boolean shade;
        public final double[] rotation;   // {rx, ry, rz} degrees; null if no rotation
        public final double[] rotOrigin;  // {ox, oy, oz} 0..16; null if rotation null
        public final List<Face> faces;

        public Box(double[] from, double[] to, boolean shade,
                   double[] rotation, double[] rotOrigin, List<Face> faces) {
            this.from = from;
            this.to = to;
            this.shade = shade;
            this.rotation = rotation;
            this.rotOrigin = rotOrigin;
            this.faces = faces;
        }
    }

    /**
     * One {@code modellist:} record — a block's geometry for a specific blockstate. The
     * {@code modelRotation} comes from the variant's {@code x}/{@code y} rotation in the blockstate
     * file and is emitted as the trailing {@code R/mrx/mry/mrz} token.
     */
    public static final class ModelListRecord {
        public final String registryName;          // without csm: prefix
        public final Map<String, String> stateMap; // ordered; empty for blocks with no properties
        public final List<Box> boxes;
        public final double[] modelRotation;       // {mrx, mry, mrz} degrees, 0 if no variant rotation

        public ModelListRecord(String registryName, Map<String, String> stateMap,
                               List<Box> boxes, double[] modelRotation) {
            this.registryName = registryName;
            this.stateMap = stateMap;
            this.boxes = boxes;
            this.modelRotation = modelRotation;
        }
    }

    /**
     * One {@code block:} record — texture mappings for a specific (block, state). The {@code patches}
     * list is the ordered list of texture IDs referenced by the {@link ModelListRecord}'s
     * {@link Face#textureIndex}.
     */
    public static final class BlockRecord {
        public final String registryName;
        public final Map<String, String> stateMap;
        public final List<String> patches;     // texture IDs, indexed by patch index
        public final Transparency transparency;

        public BlockRecord(String registryName, Map<String, String> stateMap,
                           List<String> patches, Transparency transparency) {
            this.registryName = registryName;
            this.stateMap = stateMap;
            this.patches = patches;
            this.transparency = transparency;
        }
    }

    public enum Transparency {
        OPAQUE, TRANSPARENT, SEMITRANSPARENT;

        /** Returns null for OPAQUE (omitted in output); the keyword otherwise. */
        public String keyword() {
            return this == OPAQUE ? null : name();
        }
    }

    /**
     * Empty-state map (Java 8 compatible). Used for blocks with no variant properties.
     */
    public static Map<String, String> emptyStateMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Defensive copy of an arbitrary state map into a new ordered map.
     */
    public static Map<String, String> copyStateMap(Map<String, String> src) {
        Map<String, String> out = new LinkedHashMap<>();
        if (src != null) {
            out.putAll(src);
        }
        return out;
    }

    /**
     * Empty list helper kept for readability at call sites.
     */
    public static <T> List<T> emptyMutableList() {
        return new ArrayList<>();
    }
}
