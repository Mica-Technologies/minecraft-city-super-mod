package com.micatechnologies.minecraft.csm.tools.dynmap;

import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.BlockRecord;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.ModelListRecord;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.TextureRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Emits Dynmap {@code <modid>-models.txt} and {@code <modid>-texture.txt} files. Number formatting
 * mirrors DynmapBlockScan exactly: 6-decimal {@link Locale#US}, no thousands separator.
 */
public final class DynmapEmitter {

    private DynmapEmitter() {}

    /** Six-decimal Locale.US format used everywhere in DynmapBlockScan output. */
    public static String num(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    public static void writeTextureFile(Path outFile, String modName,
                                        List<TextureRecord> textures,
                                        List<BlockRecord> blocks) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            w.write("modname:" + modName);
            w.newLine();
            w.newLine();
            for (TextureRecord t : textures) {
                w.write(t.emit());
                w.newLine();
            }
            w.newLine();
            for (BlockRecord b : blocks) {
                w.write(emitBlock(b));
                w.newLine();
            }
        }
    }

    public static void writeModelsFile(Path outFile, String modName,
                                       List<ModelListRecord> models) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            w.write("modname:" + modName);
            w.newLine();
            w.newLine();
            for (ModelListRecord m : models) {
                w.write(emitModelList(m));
                w.newLine();
            }
        }
    }

    static String emitBlock(BlockRecord b) {
        StringBuilder sb = new StringBuilder();
        sb.append("block:id=%").append(b.registryName);
        appendStateClause(sb, b.stateMap);
        for (int i = 0; i < b.patches.size(); i++) {
            sb.append(",patch").append(i).append("=0:").append(b.patches.get(i));
        }
        String t = b.transparency.keyword();
        if (t != null) {
            sb.append(",transparency=").append(t);
        }
        sb.append(",stdrot=true");
        return sb.toString();
    }

    static String emitModelList(ModelListRecord m) {
        StringBuilder sb = new StringBuilder();
        sb.append("modellist:id=%").append(m.registryName);
        appendStateClause(sb, m.stateMap);
        for (Box box : m.boxes) {
            sb.append(",box=");
            sb.append(emitBox(box, m.modelRotation));
        }
        return sb.toString();
    }

    static String emitBox(Box box, double[] modelRotation) {
        StringBuilder sb = new StringBuilder();

        // from — x/y/z [/false]
        sb.append(num(box.from[0])).append('/').append(num(box.from[1])).append('/').append(num(box.from[2]));
        if (!box.shade) {
            sb.append("/false");
        }

        // to — x/y/z [/xrot/yrot/zrot [/rorigx/rorigy/rorigz]]
        sb.append(':').append(num(box.to[0])).append('/').append(num(box.to[1])).append('/').append(num(box.to[2]));
        if (box.rotation != null) {
            sb.append('/').append(num(box.rotation[0])).append('/').append(num(box.rotation[1])).append('/').append(num(box.rotation[2]));
            if (box.rotOrigin != null) {
                sb.append('/').append(num(box.rotOrigin[0])).append('/').append(num(box.rotOrigin[1])).append('/').append(num(box.rotOrigin[2]));
            }
        }

        // sides
        for (Face face : box.faces) {
            sb.append(':').append(face.side.token);
            if (face.textureRotation != 0) {
                sb.append(face.textureRotation);
            }
            sb.append('/').append(face.textureIndex);
            if (face.uv != null) {
                sb.append('/').append(num(face.uv[0])).append('/').append(num(face.uv[1])).append('/').append(num(face.uv[2])).append('/').append(num(face.uv[3]));
            }
        }

        // optional model-level rotation
        if (modelRotation != null && (modelRotation[0] != 0 || modelRotation[1] != 0 || modelRotation[2] != 0)) {
            sb.append(":R/").append(formatRotInt(modelRotation[0])).append('/').append(formatRotInt(modelRotation[1])).append('/').append(formatRotInt(modelRotation[2]));
        }
        return sb.toString();
    }

    private static String formatRotInt(double v) {
        // Variant rotation is always integer degrees (90/180/270); emit without decimals to match the
        // DynmapBlockScan convention seen in real outputs (e.g. ":R/180/270/0").
        if (v == Math.floor(v)) {
            return Integer.toString((int) v);
        }
        return num(v);
    }

    private static void appendStateClause(StringBuilder sb, Map<String, String> stateMap) {
        if (stateMap == null || stateMap.isEmpty()) {
            return;
        }
        sb.append(",state=");
        boolean first = true;
        for (Map.Entry<String, String> e : stateMap.entrySet()) {
            if (!first) sb.append('/');
            sb.append(e.getKey()).append(':').append(e.getValue());
            first = false;
        }
    }
}
