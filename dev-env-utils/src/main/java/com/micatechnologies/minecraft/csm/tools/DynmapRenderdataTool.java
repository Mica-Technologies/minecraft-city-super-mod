package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.dynmap.BlockDiscovery;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockDiscovery.BlockMetadata;
import com.micatechnologies.minecraft.csm.tools.dynmap.BlockstateExpander;
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
import com.micatechnologies.minecraft.csm.tools.dynmap.PatchValidator;
import com.micatechnologies.minecraft.csm.tools.dynmap.TesrGeometry;
import com.micatechnologies.minecraft.csm.tools.dynmap.TextureResolver;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
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
        private int blocksFallbackMultipart;
        private int blocksFallbackObj;
        private int blocksFallbackEmpty;
        private int blocksFallbackParentOnly;
        private int blocksFailed;
        private int missingTextureFiles;
        private int blocksUsedTesrGeometry;

        Run(File devEnvironmentPath) {
            this.devEnvironmentPath = devEnvironmentPath;
            this.textureResolver = new TextureResolver(devEnvironmentPath);
            this.modelResolver = new ModelResolver(devEnvironmentPath);
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
            if (expanded.kind == Kind.MULTIPART) blocksFallbackMultipart++;
            if (expanded.kind == Kind.VANILLA) blocksFallbackVanilla++;
            if (expanded.kind == Kind.OBJ) blocksFallbackObj++;

            // TESR-rendered blocks: try the VertexData-derived silhouette first. If we get a
            // recipe match, use that for every variant of this block (geometry doesn't change
            // by colour/state on the map — the TESR draws the same body shape).
            ResolvedModel tesrSilhouette = tesrGeometry.forBlock(bm.registryName, bm.javaClassName);
            if (tesrSilhouette != null) blocksUsedTesrGeometry++;

            boolean any = false;
            for (ResolvedVariant rv : expanded.variants) {
                ResolvedModel resolved;
                if (tesrSilhouette != null) {
                    resolved = tesrSilhouette;
                } else {
                    if (rv.model == null) continue;
                    resolved = modelResolver.resolve(rv.model, rv.textures);
                    if (resolved.boxes.isEmpty()) continue;
                    if (resolved.isFallback) blocksFallbackParentOnly++;
                }

                // Apply degenerate-face filter; replace any out-of-range box with AABB cube.
                List<Box> sanitisedBoxes = new ArrayList<>();
                double[] modelRot = new double[]{rv.xRotation, rv.yRotation, 0};
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
            System.out.println();
            System.out.println("Blockstate fallbacks:");
            System.out.println("  vanilla format:           " + blocksFallbackVanilla);
            System.out.println("  multipart format:         " + blocksFallbackMultipart);
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
