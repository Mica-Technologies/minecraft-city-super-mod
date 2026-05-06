package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.dynmap.BlockDiscovery;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockDiscovery.BlockMetadata;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander.Apply;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander.ExpandedBlockstate;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander.Kind;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander.ResolvedVariant;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapEmitter;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.BlockRecord;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Box;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Face;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.ModelListRecord;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Side;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.TextureRecord;
import com.micatechnologies.minecraft.csm.tools.dynmap.DynmapTypes.Transparency;
import com.micatechnologies.minecraft.csm.tools.dynmap.ModelResolver;
import com.micatechnologies.minecraft.csm.tools.dynmap.ModelResolver.ResolvedModel;
import com.micatechnologies.minecraft.csm.tools.dynmap.ObjModelParser;
import com.micatechnologies.minecraft.csm.tools.dynmap.PatchValidator;
import com.micatechnologies.minecraft.csm.tools.dynmap.TesrGeometry;
import com.micatechnologies.minecraft.csm.tools.dynmap.TextureResolver;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Dynmap renderdata files ({@code csm-models.txt}, {@code csm-texture.txt}) for the CSM
 * mod. Replaces the broken auto-generated output from DynmapBlockScan, which produced ~226k FATAL
 * warnings on server startup.
 *
 * <p>See {@code assets/docs/agent_progress/DYNMAP_RENDERDATA_GENERATOR_PLAN.md} for full design and
 * format reference.
 *
 * <p>Output is written to {@code dev-env-utils/dynmapRenderdataOutput/}; copy both files into your
 * server's {@code dynmap/renderdata/} folder and restart Dynmap.
 */
public class DynmapRenderdataTool {

    private static final String MOD_NAME = "csm";
    private static final String OUTPUT_DIR = "dev-env-utils/dynmapRenderdataOutput";

    public static void main(String[] args) {
        CsmToolUtility.doToolExecuteWrapped("Dynmap Renderdata Generator", args, devPath -> {
            new Run(devPath).execute();
        });
    }

    private static final class Run {
        private final File devEnvironmentPath;
        private final TextureResolver textureResolver;
        private final ModelResolver modelResolver;
        private final ObjModelParser objModelParser;
        private final TesrGeometry tesrGeometry;

        // Output records.
        private final List<ModelListRecord> modelLines = new ArrayList<>();
        private final List<BlockRecord> blockLines = new ArrayList<>();

        // Stats.
        private int blocksProcessed;
        private int variantsEmitted;
        private int boxesEmitted;
        private int facesSkippedDegenerate;
        private int boxesReplacedAabb;
        private int blocksFallbackVanilla;
        private int blocksHandledMultipart;
        private int blocksFallbackObj;
        private int blocksFallbackEmpty;
        private int blocksFallbackParentOnly;
        private int blocksFailed;
        private int missingTextureFiles;
        private int blocksUsedTesrGeometry;
        private int blocksUsedObjGeometry;

        Run(File devEnvironmentPath) {
            this.devEnvironmentPath = devEnvironmentPath;
            this.textureResolver = new TextureResolver(devEnvironmentPath);
            this.modelResolver = new ModelResolver(devEnvironmentPath);
            this.objModelParser = new ObjModelParser(devEnvironmentPath);
            this.tesrGeometry = new TesrGeometry(devEnvironmentPath);
        }

        void execute() throws Exception {
            tesrGeometry.load();
            System.out.println("Loaded " + tesrGeometry.parsedArrayCount()
                    + " VertexData arrays for TESR geometry");
            Map<String, BlockMetadata> blocks = BlockDiscovery.discover(devEnvironmentPath);
            System.out.println("Discovered " + blocks.size() + " blockstates");

            int i = 0;
            for (BlockMetadata bm : blocks.values()) {
                i++;
                if (i % 100 == 0) {
                    System.out.println("  ... " + i + "/" + blocks.size() + " (" + bm.registryName + ")");
                }
                try {
                    processBlock(bm);
                } catch (Exception e) {
                    blocksFailed++;
                    System.err.println("  FAIL: " + bm.registryName + " — " + e.getMessage());
                }
            }

            // Build texture records from the resolver's registered texture map.
            List<TextureRecord> textures = new ArrayList<>();
            for (Map.Entry<String, String> e : textureResolver.registeredTextures().entrySet()) {
                textures.add(new TextureRecord(e.getKey(), e.getValue(), 1, 1));
                File f = new File(devEnvironmentPath, "src/main/resources/" + e.getValue());
                if (!f.exists()) missingTextureFiles++;
            }

            File outDir = new File(devEnvironmentPath, OUTPUT_DIR);
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("Failed to create output dir: " + outDir);
                return;
            }
            Path modelsOut = new File(outDir, MOD_NAME + "-models.txt").toPath();
            Path textureOut = new File(outDir, MOD_NAME + "-texture.txt").toPath();
            DynmapEmitter.writeModelsFile(modelsOut, MOD_NAME, modelLines);
            DynmapEmitter.writeTextureFile(textureOut, MOD_NAME, textures, blockLines);

            printSummary(textures.size(), modelsOut, textureOut);
        }

        private void processBlock(BlockMetadata bm) throws Exception {
            ExpandedBlockstate expanded = BlockstateExpander.expand(bm.blockstateFile);
            if (expanded.kind == Kind.EMPTY) {
                blocksFallbackEmpty++;
                return;
            }
            if (expanded.kind == Kind.MULTIPART) blocksHandledMultipart++;
            if (expanded.kind == Kind.VANILLA) blocksFallbackVanilla++;
            if (expanded.kind == Kind.OBJ) blocksFallbackObj++;

            // TESR-rendered blocks: try the VertexData-derived silhouette first. If we get a
            // recipe match, use that for every variant of this block (geometry doesn't change
            // by colour/state on the map — the TESR draws the same body shape).
            ResolvedModel tesrSilhouette = tesrGeometry.forBlock(bm.registryName, bm.javaClassName);
            if (tesrSilhouette != null) blocksUsedTesrGeometry++;

            // .obj-referencing blocks: parse once per block since variants only differ by rotation.
            ResolvedModel objSilhouette = null;
            if (tesrSilhouette == null && expanded.kind == Kind.OBJ) {
                ResolvedVariant first = expanded.variants.isEmpty() ? null : expanded.variants.get(0);
                if (first != null && first.model != null && first.model.endsWith(".obj")) {
                    objSilhouette = objModelParser.resolve(first.model, first.textures);
                    if (objSilhouette != null) blocksUsedObjGeometry++;
                }
            }

            boolean any = false;
            for (ResolvedVariant rv : expanded.variants) {
                ResolvedModel resolved;
                double[] modelRot;
                if (tesrSilhouette != null) {
                    resolved = tesrSilhouette;
                    modelRot = new double[]{rv.xRotation, rv.yRotation, 0};
                } else if (rv.multipartApplies != null && !rv.multipartApplies.isEmpty()) {
                    resolved = resolveMultipart(rv.multipartApplies);
                    if (resolved.boxes.isEmpty()) continue;
                    // Per-apply rotation is folded into per-box rotation, so no model-level rotation.
                    modelRot = new double[]{0, 0, 0};
                } else if (objSilhouette != null) {
                    resolved = objSilhouette;
                    modelRot = new double[]{rv.xRotation, rv.yRotation, 0};
                } else {
                    if (rv.model == null) continue;
                    resolved = modelResolver.resolve(rv.model, rv.textures);
                    if (resolved.boxes.isEmpty()) continue;
                    if (resolved.isFallback) blocksFallbackParentOnly++;
                    modelRot = new double[]{rv.xRotation, rv.yRotation, 0};
                }

                // Apply degenerate-face filter; replace any out-of-range box with AABB cube.
                List<Box> sanitisedBoxes = new ArrayList<>();
                boolean anyOutOfRange = false;
                for (Box b : resolved.boxes) {
                    if (PatchValidator.isOutOfRange(b, modelRot)) {
                        anyOutOfRange = true;
                        break;
                    }
                }
                List<Box> source = resolved.boxes;
                if (anyOutOfRange) {
                    boxesReplacedAabb += resolved.boxes.size();
                    source = aabbCubeBoxes(resolved.boxes, resolved.patchTextureRefs);
                }
                for (Box b : source) {
                    Box filtered = PatchValidator.withoutDegenerateFaces(b);
                    if (filtered != null) {
                        sanitisedBoxes.add(filtered);
                        facesSkippedDegenerate += (b.faces.size() - filtered.faces.size());
                    } else {
                        facesSkippedDegenerate += b.faces.size();
                    }
                }
                if (sanitisedBoxes.isEmpty()) continue;

                // Register textures and build the per-block patch list.
                List<String> patchIds = new ArrayList<>();
                for (String texRef : resolved.patchTextureRefs) {
                    String id = textureResolver.register(texRef);
                    patchIds.add(id != null ? id : "missing");
                }

                Map<String, String> stateMap = new LinkedHashMap<>(rv.stateMap);
                modelLines.add(new ModelListRecord(bm.registryName, stateMap, sanitisedBoxes, modelRot));
                blockLines.add(new BlockRecord(bm.registryName, stateMap, patchIds, Transparency.TRANSPARENT));
                variantsEmitted++;
                boxesEmitted += sanitisedBoxes.size();
                any = true;
            }
            if (any) blocksProcessed++;
        }

        /**
         * Resolves a list of multipart {@link Apply} clauses into a combined {@link ResolvedModel}.
         * Each apply's model is resolved independently; per-apply rotation is folded into each box's
         * element rotation around (8, 8, 8). Patch lists are merged with deduplication by texture ref.
         */
        private ResolvedModel resolveMultipart(List<Apply> applies) {
            List<Box> combinedBoxes = new ArrayList<>();
            List<String> combinedPatches = new ArrayList<>();
            Map<String, Integer> patchIdx = new LinkedHashMap<>();

            for (Apply ap : applies) {
                if (ap.model == null) continue;
                ResolvedModel sub = modelResolver.resolve(ap.model, Collections.emptyMap());
                for (Box b : sub.boxes) {
                    // Remap each face's texture index into the combined patch list.
                    List<Face> remappedFaces = new ArrayList<>(b.faces.size());
                    for (Face f : b.faces) {
                        String texRef = sub.patchTextureRefs.get(f.textureIndex);
                        Integer idx = patchIdx.get(texRef);
                        if (idx == null) {
                            idx = combinedPatches.size();
                            combinedPatches.add(texRef);
                            patchIdx.put(texRef, idx);
                        }
                        remappedFaces.add(new Face(f.side, idx, f.textureRotation, f.uv));
                    }
                    // Fold apply rotation into the per-box rotation around (8, 8, 8).
                    double[] newRotation;
                    double[] newRotOrigin;
                    if (b.rotation == null) {
                        if (ap.xRotation == 0 && ap.yRotation == 0) {
                            newRotation = null;
                            newRotOrigin = null;
                        } else {
                            newRotation = new double[]{ap.xRotation, ap.yRotation, 0};
                            newRotOrigin = new double[]{8, 8, 8};
                        }
                    } else {
                        // Existing element rotation; combine additively (only safe when both are
                        // single-axis or zero — which is the case for the fence multiparts in CSM).
                        newRotation = new double[]{
                                b.rotation[0] + ap.xRotation,
                                b.rotation[1] + ap.yRotation,
                                b.rotation[2]
                        };
                        newRotOrigin = b.rotOrigin != null ? b.rotOrigin : new double[]{8, 8, 8};
                    }
                    combinedBoxes.add(new Box(b.from, b.to, b.shade, newRotation, newRotOrigin, remappedFaces));
                }
            }
            return new ResolvedModel(combinedBoxes, combinedPatches, false);
        }

        /** Build a single-cube replacement using the model's overall AABB and the same texture set. */
        private List<Box> aabbCubeBoxes(List<Box> originalBoxes, List<String> patchTextureRefs) {
            double[][] aabb = PatchValidator.aabb(originalBoxes);
            double[] from = aabb[0];
            double[] to = aabb[1];
            // Avoid zero-volume cubes from over-clamping.
            for (int i = 0; i < 3; i++) {
                if (to[i] - from[i] < 0.5) {
                    double mid = (to[i] + from[i]) * 0.5;
                    from[i] = Math.max(0, mid - 0.25);
                    to[i] = Math.min(16, mid + 0.25);
                }
            }
            List<Face> faces = new ArrayList<>();
            for (Side s : Side.values()) {
                faces.add(new Face(s, 0, 0, deriveUv(s, from, to)));
            }
            List<Box> out = new ArrayList<>();
            out.add(new Box(from, to, true, null, null, faces));
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

        private void printSummary(int textureCount, Path modelsOut, Path textureOut) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("Dynmap Renderdata Generator Report");
            System.out.println("========================================");
            System.out.println("Blocks processed:           " + blocksProcessed);
            System.out.println("Variants emitted:           " + variantsEmitted);
            System.out.println("Boxes emitted:              " + boxesEmitted);
            System.out.println("Textures registered:        " + textureCount);
            System.out.println();
            System.out.println("Faces skipped (degenerate): " + facesSkippedDegenerate);
            System.out.println("Boxes replaced (AABB):      " + boxesReplacedAabb);
            System.out.println("Blocks via TESR geometry:   " + blocksUsedTesrGeometry);
            System.out.println("Blocks via .obj geometry:   " + blocksUsedObjGeometry);
            System.out.println();
            System.out.println("Blockstate kinds:");
            System.out.println("  vanilla format (fallback):" + blocksFallbackVanilla);
            System.out.println("  multipart (handled):      " + blocksHandledMultipart);
            System.out.println("  .obj reference:           " + blocksFallbackObj);
            System.out.println("  empty/unparseable:        " + blocksFallbackEmpty);
            System.out.println("  parent-only model:        " + blocksFallbackParentOnly);
            System.out.println("  failed:                   " + blocksFailed);
            System.out.println();
            System.out.println("Missing texture files:      " + missingTextureFiles
                    + (missingTextureFiles == 0 ? " (good)" : " (CHECK PATHS)"));
            System.out.println();
            System.out.println("Output:");
            System.out.println("  " + modelsOut);
            System.out.println("  " + textureOut);
            System.out.println("========================================");
            System.out.println();
            System.out.println("To install: copy both files to <server>/dynmap/renderdata/");
            System.out.println("            and restart the Dynmap plugin/mod.");
        }
    }
}
