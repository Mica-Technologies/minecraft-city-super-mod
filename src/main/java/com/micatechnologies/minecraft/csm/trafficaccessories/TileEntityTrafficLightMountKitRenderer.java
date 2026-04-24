package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * TESR for the dynamic signal mount kit (Pelco Astro-brac style). Detects adjacent
 * {@link TileEntityTrafficSignalHead} blocks and renders a bracket that adapts to the
 * signal's orientation, section count, and sizes. Scans up to {@link #MAX_SCAN_DISTANCE}
 * blocks in both directions along the facing axis to detect add-on signals.
 *
 * <p>The bracket consists of 3 elements:
 * <ul>
 *   <li>Two end bars at the top/bottom (vertical) or left/right (horizontal) edges</li>
 *   <li>A spine/rail connecting them at the back face</li>
 * </ul>
 *
 * <p>Color is aluminum alloy gray matching real-world Pelco Astro-brac cast aluminum finish.
 *
 * <p>Rendered fresh each frame (no display list caching) since the geometry is trivial
 * (3 boxes) and this ensures immediate response to adjacent signal changes.
 */
public class TileEntityTrafficLightMountKitRenderer
    extends TileEntitySpecialRenderer<TileEntityTrafficLightMountKit> {

  // Color palette lives on MountKitColorScheme — the scheme is read off the tile entity
  // each render so sneak+click re-coloring takes effect immediately.

  // --- Model-space constants (16 units = 1 block) ---
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;

  // --- C-channel arm cross-section ---
  // Real Astro-brac arms are aluminum C-channel (U-shape when viewed end-on).
  // We build this from 3 boxes: top flange, bottom flange, and web (back plate).
  private static final float CHANNEL_OUTER = 2.0f;   // total width/height of the C-channel
  private static final float FLANGE_THICK = 0.5f;    // thickness of each flange
  private static final float WEB_THICK = 0.5f;       // thickness of the web (back wall)
  private static final float CHANNEL_DEPTH = 2.0f;   // how deep the channel is (front to back of flanges)

  // Arm Z positioning — arms sit forward of the spine
  private static final float ARM_FRONT_Z = -2.0f;
  private static final float ARM_BACK_Z = ARM_FRONT_Z + CHANNEL_DEPTH;  // flanges only go this deep
  private static final float WEB_FRONT_Z = ARM_BACK_Z;                  // web starts where flanges end
  private static final float WEB_BACK_Z = 16.0f;                        // web runs to back of block

  // --- Spine (round tube, approximated as box) ---
  private static final float SPINE_SIZE = 1.4f;      // width=height of spine tube
  private static final float SPINE_FRONT_Z = 14.0f;  // spine sits at back, behind the arms
  private static final float SPINE_BACK_Z = 16.0f;

  // --- Pivot joint hubs (where arms meet spine) ---
  private static final float PIVOT_SIZE = 3.5f;      // larger than both arm and spine
  private static final float PIVOT_DEPTH = 2.5f;     // depth along Z

  // --- Knuckle clamps (signal housing grip at arm tips) ---
  // Multi-part: a main clamp body + a smaller bolt plate on top
  private static final float KNUCKLE_MAIN_WIDE = 4.0f;
  private static final float KNUCKLE_MAIN_THIN = 2.0f;
  private static final float KNUCKLE_MAIN_DEPTH = 3.5f;
  private static final float BOLT_PLATE_WIDE = 2.5f;
  private static final float BOLT_PLATE_THIN = 0.5f;
  private static final float BOLT_PLATE_DEPTH = 2.0f;

  // --- Mounting collar (top of spine, mast arm attachment) ---
  private static final float COLLAR_SIZE = 2.5f;
  private static final float COLLAR_HEIGHT = 1.5f;

  // How many blocks to scan in each direction for signal heads (handles add-on signals)
  private static final int MAX_SCAN_DISTANCE = 3;

  @Override
  public void render(TileEntityTrafficLightMountKit te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    IBlockState blockState = te.getWorld().getBlockState(te.getPos());
    if (!(blockState.getBlock() instanceof BlockTrafficLightMountKit)) return;

    EnumFacing facing = blockState.getValue(AbstractBlockRotatableNSEWUD.FACING);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int worldLightX = combinedLight % 65536;
    int worldLightY = combinedLight / 65536;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, worldLightX, worldLightY);

    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);
    GL11.glScaled(0.0625, 0.0625, 0.0625);
    GL11.glTranslated(8, 8, 8);

    float rotationAngle = getRotationAngle(facing);
    GL11.glRotatef(rotationAngle, 0, 1, 0);
    GL11.glTranslated(-8, -8, -8);

    renderBracket(te, facing);

    GL11.glPopMatrix();

    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  /**
   * Renders the Astro-brac bracket with detailed geometry:
   * <ul>
   *   <li>C-channel arms (top flange, bottom flange, web) at top/bottom or left/right</li>
   *   <li>Pivot joint hubs where each arm meets the spine</li>
   *   <li>Knuckle clamps with bolt plates at each arm tip</li>
   *   <li>Spine tube connecting the pivot joints at the back</li>
   *   <li>Mounting collar at the top/right end of the spine</li>
   * </ul>
   */
  private void renderBracket(TileEntityTrafficLightMountKit te, EnumFacing facing) {
    SignalInfo info = detectSignals(te, facing);
    MountKitColorScheme scheme = te.getColorScheme();

    List<RenderHelper.Box> aluBoxes = new ArrayList<>();
    List<RenderHelper.Box> aluDarkBoxes = new ArrayList<>();
    List<RenderHelper.Box> knuckleBoxes = new ArrayList<>();
    List<RenderHelper.Box> pivotBoxes = new ArrayList<>();

    if (info.horizontal) {
      buildHorizontalBracket(info, aluBoxes, aluDarkBoxes, knuckleBoxes, pivotBoxes);
    } else {
      buildVerticalBracket(info, aluBoxes, aluDarkBoxes, knuckleBoxes, pivotBoxes);
    }

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

    RenderHelper.addBoxesToBuffer(aluBoxes, buffer,
        scheme.aluR, scheme.aluG, scheme.aluB, 1.0f, 0, 0, 0);
    RenderHelper.addBoxesToBuffer(aluDarkBoxes, buffer,
        scheme.aluDarkR, scheme.aluDarkG, scheme.aluDarkB, 1.0f, 0, 0, 0);
    RenderHelper.addBoxesToBuffer(knuckleBoxes, buffer,
        scheme.knuckleR, scheme.knuckleG, scheme.knuckleB, 1.0f, 0, 0, 0);
    RenderHelper.addBoxesToBuffer(pivotBoxes, buffer,
        scheme.pivotR, scheme.pivotG, scheme.pivotB, 1.0f, 0, 0, 0);

    tessellator.draw();
    GlStateManager.enableTexture2D();
  }

  // ==================== Vertical bracket (for vertical signals) ====================

  private void buildVerticalBracket(SignalInfo info,
      List<RenderHelper.Box> alu, List<RenderHelper.Box> aluDark,
      List<RenderHelper.Box> knuckle, List<RenderHelper.Box> pivot) {

    float topEdge = info.maxY;
    float bottomEdge = info.minY;

    // --- Top C-channel arm (horizontal, extending outward from top of signal) ---
    buildVerticalArm(topEdge, true, alu, aluDark);
    // --- Bottom C-channel arm ---
    buildVerticalArm(bottomEdge, false, alu, aluDark);

    // --- Top knuckle clamp ---
    buildVerticalKnuckle(topEdge, true, knuckle);
    // --- Bottom knuckle clamp ---
    buildVerticalKnuckle(bottomEdge, false, knuckle);

    // --- Pivot joint hubs (where arms meet spine) ---
    float topPivotCenter = topEdge + CHANNEL_OUTER / 2f;
    float bottomPivotCenter = bottomEdge - CHANNEL_OUTER / 2f;
    buildPivotHub(CENTER_X, topPivotCenter, pivot);
    buildPivotHub(CENTER_X, bottomPivotCenter, pivot);

    // --- Vertical spine connecting the two pivots ---
    float spineMinX = CENTER_X - SPINE_SIZE / 2f;
    float spineMaxX = CENTER_X + SPINE_SIZE / 2f;
    alu.add(new RenderHelper.Box(
        new float[]{spineMinX, bottomPivotCenter + PIVOT_SIZE / 2f, SPINE_FRONT_Z},
        new float[]{spineMaxX, topPivotCenter - PIVOT_SIZE / 2f, SPINE_BACK_Z}));

    // --- Mounting collar at top of spine ---
    float collarMin = CENTER_X - COLLAR_SIZE / 2f;
    float collarMax = CENTER_X + COLLAR_SIZE / 2f;
    float collarY = topPivotCenter + PIVOT_SIZE / 2f;
    pivot.add(new RenderHelper.Box(
        new float[]{collarMin, collarY, SPINE_FRONT_Z},
        new float[]{collarMax, collarY + COLLAR_HEIGHT, SPINE_BACK_Z}));
  }

  /**
   * Builds one horizontal C-channel arm for a vertical bracket.
   * The C-channel has: top flange, bottom flange (outer surfaces), and a web (back wall).
   *
   * @param signalEdge the Y coordinate of the signal envelope edge
   * @param isTop      true for top arm (extends upward), false for bottom (extends downward)
   */
  private void buildVerticalArm(float signalEdge, boolean isTop,
      List<RenderHelper.Box> alu, List<RenderHelper.Box> aluDark) {
    float armMinX = CENTER_X - CHANNEL_OUTER / 2f;
    float armMaxX = CENTER_X + CHANNEL_OUTER / 2f;

    // Arm Y range: extends outward from signal edge by CHANNEL_OUTER
    float armOuterY, armInnerY;
    if (isTop) {
      armInnerY = signalEdge;
      armOuterY = signalEdge + CHANNEL_OUTER;
    } else {
      armOuterY = signalEdge - CHANNEL_OUTER;
      armInnerY = signalEdge;
    }
    float yMin = Math.min(armOuterY, armInnerY);
    float yMax = Math.max(armOuterY, armInnerY);

    // Top flange (outer edge of channel)
    alu.add(new RenderHelper.Box(
        new float[]{armMinX, yMax - FLANGE_THICK, ARM_FRONT_Z},
        new float[]{armMaxX, yMax, WEB_BACK_Z}));

    // Bottom flange (inner edge of channel, toward signal)
    alu.add(new RenderHelper.Box(
        new float[]{armMinX, yMin, ARM_FRONT_Z},
        new float[]{armMaxX, yMin + FLANGE_THICK, WEB_BACK_Z}));

    // Web (back wall of the C-channel, connecting flanges)
    aluDark.add(new RenderHelper.Box(
        new float[]{armMinX, yMin + FLANGE_THICK, WEB_FRONT_Z},
        new float[]{armMaxX, yMax - FLANGE_THICK, WEB_BACK_Z}));
  }

  /**
   * Builds knuckle clamp where the arm meets the signal housing.
   * The clamp grips the signal body at the inner end of the arm (near the signal edge),
   * not at the outer tip. Consists of a main clamp body and a smaller bolt plate.
   */
  private void buildVerticalKnuckle(float signalEdge, boolean isTop,
      List<RenderHelper.Box> knuckle) {
    float cx = CENTER_X;

    // Knuckle spans from 0.01 inside the signal housing to 0.01 past the outer
    // edge of the C-channel arm, so no face is coplanar with either the housing
    // or the arm flanges.
    float knuckleHeight = CHANNEL_OUTER + 0.02f; // full arm height + anti-flicker margin
    float knuckleY;
    if (isTop) {
      knuckleY = signalEdge - 0.01f;             // 0.01 into signal housing
    } else {
      knuckleY = signalEdge - knuckleHeight + 0.01f;
    }

    // Main clamp body
    knuckle.add(new RenderHelper.Box(
        new float[]{cx - KNUCKLE_MAIN_WIDE / 2f - 0.01f, knuckleY, ARM_FRONT_Z - 0.01f},
        new float[]{cx + KNUCKLE_MAIN_WIDE / 2f + 0.01f, knuckleY + knuckleHeight,
            ARM_FRONT_Z + KNUCKLE_MAIN_DEPTH}));

    // Bolt plate (smaller, on the outward-facing side of the clamp)
    float boltY = isTop ? knuckleY + knuckleHeight : knuckleY - BOLT_PLATE_THIN;
    knuckle.add(new RenderHelper.Box(
        new float[]{cx - BOLT_PLATE_WIDE / 2f, boltY, ARM_FRONT_Z + 0.5f},
        new float[]{cx + BOLT_PLATE_WIDE / 2f, boltY + BOLT_PLATE_THIN,
            ARM_FRONT_Z + 0.5f + BOLT_PLATE_DEPTH}));
  }

  // ==================== Horizontal bracket (for horizontal signals) ====================

  private void buildHorizontalBracket(SignalInfo info,
      List<RenderHelper.Box> alu, List<RenderHelper.Box> aluDark,
      List<RenderHelper.Box> knuckle, List<RenderHelper.Box> pivot) {

    float leftEdge = info.minX;
    float rightEdge = info.maxX;

    // --- Left C-channel arm (vertical, extending outward from left of signal) ---
    buildHorizontalArm(leftEdge, true, alu, aluDark);
    // --- Right C-channel arm ---
    buildHorizontalArm(rightEdge, false, alu, aluDark);

    // --- Left knuckle clamp ---
    buildHorizontalKnuckle(leftEdge, true, knuckle);
    // --- Right knuckle clamp ---
    buildHorizontalKnuckle(rightEdge, false, knuckle);

    // --- Pivot joint hubs ---
    float leftPivotCenter = leftEdge - CHANNEL_OUTER / 2f;
    float rightPivotCenter = rightEdge + CHANNEL_OUTER / 2f;
    buildPivotHub(leftPivotCenter, CENTER_Y, pivot);
    buildPivotHub(rightPivotCenter, CENTER_Y, pivot);

    // --- Horizontal spine connecting the two pivots ---
    float spineMinY = CENTER_Y - SPINE_SIZE / 2f;
    float spineMaxY = CENTER_Y + SPINE_SIZE / 2f;
    alu.add(new RenderHelper.Box(
        new float[]{leftPivotCenter + PIVOT_SIZE / 2f, spineMinY, SPINE_FRONT_Z},
        new float[]{rightPivotCenter - PIVOT_SIZE / 2f, spineMaxY, SPINE_BACK_Z}));

    // --- Mounting collar at right end of spine ---
    float collarMin = CENTER_Y - COLLAR_SIZE / 2f;
    float collarMax = CENTER_Y + COLLAR_SIZE / 2f;
    float collarX = rightPivotCenter + PIVOT_SIZE / 2f;
    pivot.add(new RenderHelper.Box(
        new float[]{collarX, collarMin, SPINE_FRONT_Z},
        new float[]{collarX + COLLAR_HEIGHT, collarMax, SPINE_BACK_Z}));
  }

  /**
   * Builds one vertical C-channel arm for a horizontal bracket.
   */
  private void buildHorizontalArm(float signalEdge, boolean isLeft,
      List<RenderHelper.Box> alu, List<RenderHelper.Box> aluDark) {
    float armMinY = CENTER_Y - CHANNEL_OUTER / 2f;
    float armMaxY = CENTER_Y + CHANNEL_OUTER / 2f;

    float armOuterX, armInnerX;
    if (isLeft) {
      armOuterX = signalEdge - CHANNEL_OUTER;
      armInnerX = signalEdge;
    } else {
      armInnerX = signalEdge;
      armOuterX = signalEdge + CHANNEL_OUTER;
    }
    float xMin = Math.min(armOuterX, armInnerX);
    float xMax = Math.max(armOuterX, armInnerX);

    // Left flange (outer edge)
    alu.add(new RenderHelper.Box(
        new float[]{xMin, armMinY, ARM_FRONT_Z},
        new float[]{xMin + FLANGE_THICK, armMaxY, WEB_BACK_Z}));

    // Right flange (inner edge, toward signal)
    alu.add(new RenderHelper.Box(
        new float[]{xMax - FLANGE_THICK, armMinY, ARM_FRONT_Z},
        new float[]{xMax, armMaxY, WEB_BACK_Z}));

    // Web (back wall)
    aluDark.add(new RenderHelper.Box(
        new float[]{xMin + FLANGE_THICK, armMinY, WEB_FRONT_Z},
        new float[]{xMax - FLANGE_THICK, armMaxY, WEB_BACK_Z}));
  }

  /**
   * Builds knuckle clamp where the arm meets the signal housing (horizontal variant).
   */
  private void buildHorizontalKnuckle(float signalEdge, boolean isLeft,
      List<RenderHelper.Box> knuckle) {
    float cy = CENTER_Y;

    // Knuckle spans from 0.01 inside signal housing to 0.01 past outer arm edge
    float knuckleWidth = CHANNEL_OUTER + 0.02f;
    float knuckleX;
    if (isLeft) {
      knuckleX = signalEdge - knuckleWidth + 0.01f;
    } else {
      knuckleX = signalEdge - 0.01f;
    }

    // Main clamp body
    knuckle.add(new RenderHelper.Box(
        new float[]{knuckleX, cy - KNUCKLE_MAIN_WIDE / 2f - 0.01f, ARM_FRONT_Z - 0.01f},
        new float[]{knuckleX + knuckleWidth, cy + KNUCKLE_MAIN_WIDE / 2f + 0.01f,
            ARM_FRONT_Z + KNUCKLE_MAIN_DEPTH}));

    // Bolt plate (on the outward-facing side)
    float boltX = isLeft ? knuckleX - BOLT_PLATE_THIN : knuckleX + knuckleWidth;
    knuckle.add(new RenderHelper.Box(
        new float[]{boltX, cy - BOLT_PLATE_WIDE / 2f, ARM_FRONT_Z + 0.5f},
        new float[]{boltX + BOLT_PLATE_THIN, cy + BOLT_PLATE_WIDE / 2f,
            ARM_FRONT_Z + 0.5f + BOLT_PLATE_DEPTH}));
  }

  // ==================== Shared geometry helpers ====================

  /**
   * Builds a pivot joint hub — a thick block at the point where an arm meets the spine.
   * Sits slightly forward and larger than the spine to fully enclose it without Z-fighting.
   */
  private void buildPivotHub(float centerX, float centerY, List<RenderHelper.Box> pivot) {
    pivot.add(new RenderHelper.Box(
        new float[]{centerX - PIVOT_SIZE / 2f, centerY - PIVOT_SIZE / 2f,
            SPINE_FRONT_Z - PIVOT_DEPTH / 2f},
        new float[]{centerX + PIVOT_SIZE / 2f, centerY + PIVOT_SIZE / 2f,
            SPINE_BACK_Z + 0.01f}));
  }

  /**
   * Detects adjacent signal heads and merges their bounding envelopes. First finds the
   * primary signal in front of or behind the mount (along the facing axis), then scans
   * vertically (up/down) from that column for add-on signals which are typically placed
   * 1-2 blocks below the main signal block.
   * Falls back to a default 3-section 12-inch vertical bracket if no signals are found.
   */
  private SignalInfo detectSignals(TileEntityTrafficLightMountKit te, EnumFacing facing) {
    BlockPos origin = te.getPos();

    // Find the primary signal: check forward first, then backward
    BlockPos signalColumn = null;
    if (isSignalHead(te, origin.offset(facing))) {
      signalColumn = origin.offset(facing);
    } else if (isSignalHead(te, origin.offset(facing.getOpposite()))) {
      signalColumn = origin.offset(facing.getOpposite());
    }

    if (signalColumn == null) {
      return getDefaultSignalInfo();
    }

    // Scan the signal column vertically: start at the found position, then scan
    // up and down for additional signal heads (add-on signals)
    SignalInfo merged = readSignalAt(te, signalColumn);

    // Scan downward from signal column (add-on signals are placed 1-2 blocks below).
    // Don't break on gaps — double add-on signals may be 2 blocks below with an empty
    // block in between (legacy placement for world compatibility).
    for (int dy = 1; dy <= MAX_SCAN_DISTANCE; dy++) {
      BlockPos below = signalColumn.down(dy);
      SignalInfo addon = readSignalAt(te, below);
      if (addon != null) {
        float yShift = -dy * 16.0f;
        merged = mergeInfo(merged, addon, yShift);
      }
    }

    // Scan upward as well (unusual but possible)
    for (int dy = 1; dy <= MAX_SCAN_DISTANCE; dy++) {
      BlockPos above = signalColumn.up(dy);
      SignalInfo addon = readSignalAt(te, above);
      if (addon != null) {
        float yShift = dy * 16.0f;
        merged = mergeInfo(merged, addon, yShift);
      }
    }

    return merged != null ? merged : getDefaultSignalInfo();
  }

  private boolean isSignalHead(TileEntityTrafficLightMountKit te, BlockPos pos) {
    TileEntity check = te.getWorld().getTileEntity(pos);
    return check instanceof TileEntityTrafficSignalHead
        || check instanceof TileEntityBlankoutBox;
  }

  /**
   * Reads signal info from a single block position. Returns null if the block is not a
   * valid signal head.
   */
  @Nullable
  private SignalInfo readSignalAt(TileEntityTrafficLightMountKit te, BlockPos pos) {
    TileEntity checkTe = te.getWorld().getTileEntity(pos);

    if (checkTe instanceof TileEntityBlankoutBox) {
      SignalInfo info = new SignalInfo();
      info.minX = BlankoutBoxVertexData.BODY_X_MIN;
      info.maxX = BlankoutBoxVertexData.BODY_X_MAX;
      info.minY = BlankoutBoxVertexData.BODY_Y_MIN;
      info.maxY = BlankoutBoxVertexData.BODY_Y_MAX;
      info.horizontal = false;
      return info;
    }

    if (!(checkTe instanceof TileEntityTrafficSignalHead)) return null;

    TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) checkTe;
    IBlockState signalState = te.getWorld().getBlockState(pos);
    if (!(signalState.getBlock() instanceof AbstractBlockControllableSignalHead)) return null;

    AbstractBlockControllableSignalHead signalBlock =
        (AbstractBlockControllableSignalHead) signalState.getBlock();

    int sectionCount = signalHead.getSectionCount();
    if (sectionCount == 0) return null;

    return computeEnvelope(sectionCount,
        signalBlock.getSectionSizes(sectionCount),
        signalBlock.getSectionYPositions(sectionCount, te.getWorld(), pos),
        signalBlock.getSectionXPositions(sectionCount, te.getWorld(), pos),
        signalBlock.getSignalYOffset(te.getWorld(), pos),
        signalBlock.isHorizontal(te.getWorld(), pos));
  }

  /**
   * Merges two signal infos, applying a Y shift to the second one (for signals at different
   * block positions vertically). Horizontal flag is inherited from the primary signal.
   */
  private SignalInfo mergeInfo(@Nullable SignalInfo primary, @Nullable SignalInfo addon,
      float yShift) {
    if (addon == null) return primary;
    if (primary == null) {
      addon.minY += yShift;
      addon.maxY += yShift;
      return addon;
    }
    primary.minX = Math.min(primary.minX, addon.minX);
    primary.maxX = Math.max(primary.maxX, addon.maxX);
    primary.minY = Math.min(primary.minY, addon.minY + yShift);
    primary.maxY = Math.max(primary.maxY, addon.maxY + yShift);
    return primary;
  }

  /**
   * Computes the min/max X and Y envelope across all signal sections, using the same
   * coordinate system as the signal head renderer.
   */
  private SignalInfo computeEnvelope(int sectionCount, int[] sectionSizes,
      float[] sectionYPositions, float[] sectionXPositions,
      float signalYOffset, boolean horizontal) {
    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

    for (int i = 0; i < sectionCount; i++) {
      float size = sectionSizes[i];
      float scale = size / 12.0f;

      float localMinX = CENTER_X + (2.0f - CENTER_X) * scale;
      float localMaxX = CENTER_X + (14.0f - CENTER_X) * scale;
      float localMinY = CENTER_Y + (0.0f - CENTER_Y) * scale;
      float localMaxY = CENTER_Y + (12.0f - CENTER_Y) * scale;

      float sMinX = localMinX + sectionXPositions[i];
      float sMaxX = localMaxX + sectionXPositions[i];
      float sMinY = localMinY + sectionYPositions[i];
      float sMaxY = localMaxY + sectionYPositions[i];

      minX = Math.min(minX, sMinX);
      maxX = Math.max(maxX, sMaxX);
      minY = Math.min(minY, sMinY);
      maxY = Math.max(maxY, sMaxY);
    }

    minY += signalYOffset;
    maxY += signalYOffset;

    SignalInfo info = new SignalInfo();
    info.minX = minX;
    info.maxX = maxX;
    info.minY = minY;
    info.maxY = maxY;
    info.horizontal = horizontal;
    return info;
  }

  private SignalInfo getDefaultSignalInfo() {
    return computeEnvelope(3,
        new int[]{12, 12, 12},
        new float[]{12.0f, 0.0f, -12.0f},
        new float[]{0, 0, 0},
        0.0f, false);
  }

  private static float getRotationAngle(EnumFacing facing) {
    switch (facing) {
      case NORTH: return 0f;
      case WEST:  return 90f;
      case SOUTH: return 180f;
      case EAST:  return 270f;
      default:    return 0f;
    }
  }

  private static class SignalInfo {
    float minX, maxX, minY, maxY;
    boolean horizontal;
  }
}
