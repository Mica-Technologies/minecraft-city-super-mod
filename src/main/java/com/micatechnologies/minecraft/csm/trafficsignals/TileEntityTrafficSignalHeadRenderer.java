package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBoundingBoxHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.SignalHeadMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;


/**
 * TESR (TileEntitySpecialRenderer) for configurable traffic signal head blocks. Renders signal
 * head geometry using cached OpenGL display lists built from OGL vertex data, with dynamic
 * texture atlas lookups based on the current signal configuration.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class TileEntityTrafficSignalHeadRenderer extends
    TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  private static final ResourceLocation ATLAS_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");

  // Per-tile-entity display list cache (keyed by BlockPos to avoid shared state bug)
  private final Map<BlockPos, Integer> displayListCache = new HashMap<>();
  private final Map<BlockPos, Integer> lastColorStateCache = new HashMap<>();
  private final Map<BlockPos, Integer> lastLitMaskCache = new HashMap<>();

  /**
   * Cleans up the cached display list for a signal head at the given position.
   * Called from AbstractBlockControllableSignalHead.breakBlock() to prevent
   * stale entries from accumulating during long play sessions.
   */
  public void cleanupDisplayList(BlockPos pos) {
    Integer displayList = displayListCache.remove(pos);
    if (displayList != null) {
      GL11.glDeleteLists(displayList, 1);
    }
    lastColorStateCache.remove(pos);
    lastLitMaskCache.remove(pos);
  }

  @Override
  public void render(TileEntityTrafficSignalHead te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    // Gather block state information
    IBlockState blockState = te.getWorld().getBlockState(te.getPos());
    EnumFacing facing = blockState.getValue(AbstractBlockControllableSignalHead.FACING);
    int signalColorState = blockState.getValue(AbstractBlockControllableSignalHead.COLOR);

    // Get per-block-type Y offset for signal positioning (world-aware for add-on detection)
    float signalYOffset = 0.0f;
    if (blockState.getBlock() instanceof AbstractBlockControllableSignalHead) {
      signalYOffset = ((AbstractBlockControllableSignalHead) blockState.getBlock())
          .getSignalYOffset(te.getWorld(), te.getPos());
    }

    // Gather tile entity information
    TrafficSignalSectionInfo[] sectionInfos = te.getSectionInfos(signalColorState);
    TrafficSignalBodyTilt bodyTilt = te.getBodyTilt();
    DirectionSixteen bodyDirection =
        AbstractBlockControllableSignalHead.getTiltedFacing(bodyTilt, facing);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;

    // Compute world lightmap for this block so housing/door/visor/mount use realistic
    // lighting while lit bulbs and strobe flashes stay fullbright.
    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int worldLightX = combinedLight % 65536;
    int worldLightY = combinedLight / 65536;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, worldLightX, worldLightY);

    // Get tilt pivot offset for add-on signals that need to rotate in sync with
    // their parent signal (offset is in block units from this block to the main signal)
    int[] tiltPivotOffset = new int[]{0, 0, 0};
    if (blockState.getBlock() instanceof AbstractBlockControllableSignalHead) {
      tiltPivotOffset = ((AbstractBlockControllableSignalHead) blockState.getBlock())
          .getTiltPivotOffset(te.getWorld(), te.getPos());
    }

    boolean hasTiltPivot = (tiltPivotOffset[0] != 0 || tiltPivotOffset[2] != 0)
        && bodyTilt != TrafficSignalBodyTilt.NONE;

    // Push matrix once
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);
    GL11.glScaled(0.0625, 0.0625, 0.0625);

    if (hasTiltPivot) {
      // For add-on signals with a tilt pivot offset, decompose into two rotations:
      // 1. Apply the tilt component around the MAIN signal's center
      // 2. Apply the base facing around this block's own center
      // (GL matrices apply in reverse order, so tilt goes first in code)
      float baseFacingAngle = getBaseFacingAngle(facing);
      float tiltAngle = bodyDirection.getRotation() - baseFacingAngle;

      // Main signal center in this block's model space
      float pivotX = 8 + tiltPivotOffset[0] * 16.0f;
      float pivotZ = 8 + tiltPivotOffset[2] * 16.0f;

      // Step 1: Tilt rotation around main signal center
      GL11.glTranslated(pivotX, 8, pivotZ);
      GL11.glRotatef(tiltAngle, 0, 1, 0);
      GL11.glTranslated(-pivotX, -8, -pivotZ);

      // Step 2: Base facing rotation around own block center
      GL11.glTranslated(8, 8, 8);
      GL11.glRotatef(baseFacingAngle, 0, 1, 0);
      GL11.glTranslated(-8, -8, -8);
    } else {
      // Standard rotation: single rotation around own block center
      GL11.glTranslated(8, 8, 8);
      float rotationAngle = bodyDirection.getRotation();
      GL11.glRotatef(rotationAngle, 0, 1, 0);
      GL11.glTranslated(-8, -8, -8);
    }

    // --- Compensation for tilt: shift slightly left/right for visual alignment ---
    // 1 model unit = 1/16 block, so shift by ±2 for tilt, ±4 for angle
    int tiltOffset = 0;
    if (bodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
      tiltOffset = -4;
    } else if (bodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
      tiltOffset = -2;
    } else if (bodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
      tiltOffset = 2;
    } else if (bodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
      tiltOffset = 4;
    }

    if (tiltOffset != 0) {
      GL11.glTranslated(tiltOffset, 0, 0);
    }

    // Apply per-block-type Y offset (e.g., single-section signals sit higher)
    if (signalYOffset != 0.0f) {
      GL11.glTranslated(0, signalYOffset, 0);
    }

    // Get per-section positions and sizes from the block
    int sectionCount = sectionInfos.length;
    float[] sectionYPositions;
    float[] sectionXPositions;
    int[] sectionSizes;
    boolean horizontal = false;
    if (blockState.getBlock() instanceof AbstractBlockControllableSignalHead) {
      AbstractBlockControllableSignalHead signalBlock =
          (AbstractBlockControllableSignalHead) blockState.getBlock();
      sectionYPositions = signalBlock.getSectionYPositions(sectionCount, te.getWorld(), te.getPos());
      sectionXPositions = signalBlock.getSectionXPositions(sectionCount, te.getWorld(), te.getPos());
      sectionSizes = signalBlock.getSectionSizes(sectionCount);
      horizontal = signalBlock.isHorizontal(te.getWorld(), te.getPos());
      // Safety: if TE has more sections than the block expects (e.g., world migration from
      // old 3-section defaults), pad the position arrays to avoid ArrayIndexOutOfBoundsException
      if (sectionYPositions.length < sectionCount) {
        float[] padded = new float[sectionCount];
        System.arraycopy(sectionYPositions, 0, padded, 0, sectionYPositions.length);
        for (int i = sectionYPositions.length; i < sectionCount; i++) {
          padded[i] = ((sectionCount - 1 - i) - (sectionCount - 1) / 2.0f) * 12.0f;
        }
        sectionYPositions = padded;
      }
      if (sectionXPositions.length < sectionCount) {
        float[] padded = new float[sectionCount];
        System.arraycopy(sectionXPositions, 0, padded, 0, sectionXPositions.length);
        sectionXPositions = padded;
      }
      // Safety: pad sectionSizes if needed
      if (sectionSizes.length < sectionCount) {
        int[] padded = new int[sectionCount];
        System.arraycopy(sectionSizes, 0, padded, 0, sectionSizes.length);
        for (int i = sectionSizes.length; i < sectionCount; i++) padded[i] = 12;
        sectionSizes = padded;
      }
    } else {
      // Fallback: standard vertical stack, no X offset, all 12-inch
      sectionYPositions = new float[sectionCount];
      sectionXPositions = new float[sectionCount];
      sectionSizes = new int[sectionCount];
      for (int i = 0; i < sectionCount; i++) {
        sectionYPositions[i] = ((sectionCount - 1 - i) - (sectionCount - 1) / 2.0f) * 12.0f;
        sectionSizes[i] = 12;
      }
    }

    // Compute Z push-back for uniform-size signals so the back stays flush with the
    // block face (Z=16) for mounting. Mixed-size signals (e.g., 8-8-12) keep fronts
    // aligned instead (no push-back).
    float zPushBack = TrafficSignalBoundingBoxHelper.computeZPushBack(sectionSizes);

    int litMask = 0;
    for (int i = 0; i < sectionInfos.length; i++) {
      if (sectionInfos[i].isBulbLit()) litMask |= (1 << i);
    }

    BlockPos pos = te.getPos();
    Integer displayList = displayListCache.get(pos);
    Integer lastColor = lastColorStateCache.get(pos);
    Integer lastLit = lastLitMaskCache.get(pos);
    if (displayList == null || te.isStateDirty()
        || lastColor == null || lastColor != signalColorState
        || lastLit == null || lastLit != litMask) {
      if (displayList != null) {
        GL11.glDeleteLists(displayList, 1);
      }
      displayList = GL11.glGenLists(1);
      displayListCache.put(pos, displayList);
      lastColorStateCache.put(pos, signalColorState);
      lastLitMaskCache.put(pos, litMask);
      GL11.glNewList(displayList, GL11.GL_COMPILE);
      renderStaticParts(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, horizontal, zPushBack);
      GL11.glEndList();
      te.clearDirtyFlag();
    }
    GL11.glCallList(displayList);

    // Fullbright for lit bulbs and strobe flashes — housing/door/visor already rendered
    // above with world lightmap via the display list.
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    // Overlay the inner-colored faces of every lit visor at fullbright. The display list
    // already drew them with the bulb tint but at world lightmap, so the reflected-light
    // effect was getting dimmed by ambient lighting (looking dull at night/in shadow).
    // Scale the overlay by daylight (sky exposure × sun brightness) so it eases off in
    // direct sunlight, mimicking how a real reflected-light effect washes out in daylight.
    int skyLightLevel = te.getWorld().getLightFor(EnumSkyBlock.SKY, te.getPos());
    float sunBrightness = te.getWorld().getSunBrightness(partialTicks);
    float daylightFactor = (skyLightLevel / 15.0f) * sunBrightness;
    float tintScale = 1.0f - VISOR_DAYLIGHT_DIM_AMOUNT * daylightFactor;
    renderLitVisorInteriors(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes,
        zPushBack, tintScale);

    // Pause-aware game clock — threaded into the bulb/Barlo paths so they can do 1300 ms /
    // 1000 ms modulo timing without each calling System.currentTimeMillis() (which is a JNI
    // call and adds up with many visible signals).
    long gameMillis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
    renderBulbs(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, zPushBack,
        gameMillis);

    // renderVisorGlow(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, zPushBack);

    // Restore world lightmap for mount hardware so brackets match world lighting.
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, worldLightX, worldLightY);

    // Mount hardware renders outside the cached display list: adjacency changes (add-on
    // placed/broken beside this signal) don't invalidate the TE's dirty flag, so rebuilding
    // from source every frame is the simplest way to keep suppression up to date. Geometry
    // is ~4-8 small boxes per frame — cheap compared to the main signal draw.
    //
    // mountTiltAngle is the net Y rotation the signal body is currently under, relative to
    // the base facing, because of bodyTilt. The mount itself is drawn inside the same GL
    // matrix (so it inherits the tilt visually on the signal side of the bracket), but the
    // arm's target is in world-neighbour coordinates that don't rotate with the tilt — so
    // BracketSpec uses mountTiltAngle to inverse-rotate the target before solving, cancelling
    // the GL tilt out and leaving the arm tip at the actual world pole position.
    float mountTiltAngle = bodyDirection.getRotation() - getBaseFacingAngle(facing);
    renderMount(te, blockState, sectionSizes, sectionYPositions, sectionXPositions, horizontal,
        zPushBack, mountTiltAngle);

    GL11.glPopMatrix();

    // Reset GL color to white and sync GlStateManager's cached color state.
    // The Barlo strobe code (and display list replay) sets GL color via GL11.glColor4f()
    // directly, bypassing GlStateManager's cache. If we don't reset here, the stale color
    // leaks into subsequent renderers (e.g., vanilla sign TESR) — GlStateManager thinks the
    // color is already white and skips the actual GL call, causing other blocks to inherit
    // whatever color the strobe left behind (strobing dark/white on the Barlo cycle).
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();

    // Restore previous lightmap brightness
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  private void renderStaticParts(TrafficSignalSectionInfo[] sectionInfos,
      float[] sectionYPositions, float[] sectionXPositions, int[] sectionSizes,
      boolean horizontal, float zPushBack) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();

    // Batch all body, door, and visor quads into a single draw call
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];
      TrafficSignalBodyColor bodyColor = sectionInfo.getBodyColor();
      TrafficSignalBodyColor doorColor = sectionInfo.getDoorColor();
      TrafficSignalBodyColor visorColor = sectionInfo.getVisorColor();
      TrafficSignalVisorType visorType = sectionInfo.getVisorType();

      float xOffset = sectionXPositions[i];
      float yOffset = sectionYPositions[i];
      boolean is8Inch = sectionSizes[i] == 8;
      boolean is4Inch = sectionSizes[i] == 4;

      List<RenderHelper.Box> bodyData;
      List<RenderHelper.Box> doorData;
      if (horizontal) {
        bodyData = TrafficSignalVertexData.SIGNAL_BODY_HORIZONTAL_VERTEX_DATA;
        doorData = TrafficSignalVertexData.SIGNAL_DOOR_HORIZONTAL_VERTEX_DATA;
      } else if (is4Inch) {
        bodyData = TrafficSignalVertexData.SIGNAL_BODY_4INCH_VERTEX_DATA;
        doorData = TrafficSignalVertexData.SIGNAL_DOOR_4INCH_VERTEX_DATA;
      } else if (is8Inch) {
        bodyData = TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA;
        doorData = TrafficSignalVertexData.SIGNAL_DOOR_8INCH_VERTEX_DATA;
      } else {
        bodyData = TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA;
        doorData = TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA;
      }

      RenderHelper.addBoxesToBuffer(bodyData, buffer,
          bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 1.0f, xOffset, yOffset, zPushBack);
      RenderHelper.addBoxesToBuffer(doorData, buffer,
          doorColor.getRed(), doorColor.getGreen(), doorColor.getBlue(), 1.0f, xOffset, yOffset, zPushBack);

      float[] inner = computeVisorInnerTint(sectionInfo);

      addVisorQuadsToBuffer(visorType, buffer,
          Math.min(1.0f, visorColor.getRed() * VISOR_TINT_SCALE + VISOR_TINT_BASE),
          Math.min(1.0f, visorColor.getGreen() * VISOR_TINT_SCALE + VISOR_TINT_BASE),
          Math.min(1.0f, visorColor.getBlue() * VISOR_TINT_SCALE + VISOR_TINT_BASE),
          inner[0], inner[1], inner[2],
          1.0f, xOffset, yOffset, sectionSizes[i], zPushBack);
    }
    tessellator.draw();

    GlStateManager.enableTexture2D();
  }

  /**
   * Computes the visor interior tint for a section. Unlit sections get the dark default;
   * lit sections get a color matching the bulb (red/yellow/green), with transit yellow/green
   * mapped to white because those bulb types render as white-LED textures.
   */
  private static float[] computeVisorInnerTint(TrafficSignalSectionInfo sectionInfo) {
    float[] out = new float[]{VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B};
    if (!sectionInfo.isBulbLit()) {
      return out;
    }
    TrafficSignalBulbColor bulbColor = sectionInfo.getBulbCustomColor();
    TrafficSignalBulbType bulbType = sectionInfo.getBulbType();
    boolean transitBulb = bulbType == TrafficSignalBulbType.TRANSIT
        || bulbType == TrafficSignalBulbType.TRANSIT_LEFT
        || bulbType == TrafficSignalBulbType.TRANSIT_RIGHT;
    if (transitBulb && bulbColor != TrafficSignalBulbColor.RED) {
      // Transit yellow/green render as white-LED textures; reflection follows.
      out[0] = 0.55f; out[1] = 0.55f; out[2] = 0.55f;
      return out;
    }
    switch (bulbColor) {
      case RED:    out[0] = 0.50f; out[1] = 0.08f; out[2] = 0.02f; break;
      case YELLOW: out[0] = 0.60f; out[1] = 0.40f; out[2] = 0.03f; break;
      case GREEN:  out[0] = 0.12f; out[1] = 0.70f; out[2] = 0.22f; break;
      default: break;
    }
    return out;
  }

  /**
   * Re-emits the inner-colored faces of every lit section's visor at the current (fullbright)
   * lightmap. The display-list pass already drew these faces with the bulb tint but at world
   * lightmap, so they look dim in shadow/at night even though the bulb itself is fullbright.
   * This second pass overdraws the inner faces with the same vertex positions and colors but
   * at fullbright, so the reflected-light effect actually looks lit. Outer body faces are
   * untouched and stay world-lit.
   */
  private void renderLitVisorInteriors(TrafficSignalSectionInfo[] sectionInfos,
      float[] sectionYPositions, float[] sectionXPositions, int[] sectionSizes, float zPushBack,
      float tintScale) {
    boolean anyLit = false;
    for (TrafficSignalSectionInfo info : sectionInfos) {
      if (info.isBulbLit()) { anyLit = true; break; }
    }
    if (!anyLit) return;

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];
      if (!sectionInfo.isBulbLit()) continue;

      TrafficSignalVisorType visorType = sectionInfo.getVisorType();
      List<RenderHelper.Box> visorData = resolveVisorData(visorType, sectionSizes[i]);
      if (visorData == null) continue;

      float[] inner = computeVisorInnerTint(sectionInfo);
      float r = inner[0] * tintScale;
      float g = inner[1] * tintScale;
      float b = inner[2] * tintScale;
      float xOffset = sectionXPositions[i];
      float yOffset = sectionYPositions[i];

      if (visorType != TrafficSignalVisorType.NONE) {
        float louverTiltAdjust = -yOffset * LOUVER_TILT_COMPENSATION_PER_UNIT;
        RenderHelper.addTiltedBoxesInnerFacesToBuffer(visorData, buffer,
            r, g, b, 1.0f,
            xOffset, yOffset, zPushBack, VISOR_PIVOT_Z + zPushBack, VISOR_TILT_DEGREES,
            VISOR_CENTER_X, VISOR_CENTER_Y, louverTiltAdjust);
      } else {
        RenderHelper.addBoxesInnerFacesToBuffer(visorData, buffer,
            r, g, b, 1.0f,
            xOffset, yOffset, zPushBack, VISOR_CENTER_X, VISOR_CENTER_Y);
      }
    }

    tessellator.draw();
    GlStateManager.enableTexture2D();
  }

  // Downward tilt angle for visors (degrees). Makes bulbs visible from below, as real
  // traffic signals are mounted high on mast arms. Pivot is at the body face (z=11).
  private static final float VISOR_TILT_DEGREES = 9.0f;
  private static final float VISOR_PIVOT_Z = 11.0f;

  // Visor center in model space (X=8, Y=6). All visor sizes scale relative to this point,
  // so it remains correct for 12-inch, 8-inch, and 4-inch sections.
  private static final float VISOR_CENTER_X = 8.0f;
  private static final float VISOR_CENTER_Y = 6.0f;

  // Default interior color for visors (unlit sections). Lit sections override with a
  // tinted color matching the bulb to simulate reflected light inside the visor.
  private static final float VISOR_INNER_R = 0.0f;
  private static final float VISOR_INNER_G = 0.0f;
  private static final float VISOR_INNER_B = 0.0f;

  // Visor tint parameters — proportional shift so dark colors get a gentler nudge while
  // lighter colors still have enough distinction.  Result: min(1, channel * SCALE + BASE).
  private static final float VISOR_TINT_SCALE = 1.04f;
  private static final float VISOR_TINT_BASE = 0.01f;

  // Maximum fraction by which the lit-visor inner reflection is dimmed at full daylight
  // (sky exposure × sun brightness = 1). At night/indoors the inner tint stays at full
  // strength so the bulb still appears to bounce light off the visor interior. At noon out
  // in the open the inner tint should be only barely visible — the sun washes most of it out.
  private static final float VISOR_DAYLIGHT_DIM_AMOUNT = 0.85f;

  // Per-section louver tilt compensation (degrees per model unit of Y offset).
  // In a multi-section signal, lower sections are closer to the viewer's eye level and
  // need steeper louver tilt to maintain the same visibility cutoff. Each 12-unit section
  // offset changes the viewing angle by ~1.8° at 20 blocks distance (10 blocks height).
  private static final float LOUVER_TILT_COMPENSATION_PER_UNIT = 0.05f;

  private void addVisorQuadsToBuffer(TrafficSignalVisorType visorType, BufferBuilder buffer,
      float red, float green, float blue,
      float innerR, float innerG, float innerB,
      float alpha, float xOffset, float yOffset,
      int sectionSize, float zPushBack) {
    List<RenderHelper.Box> visorData = resolveVisorData(visorType, sectionSize);
    if (visorData == null) return;
    boolean applyTilt = visorType != TrafficSignalVisorType.NONE;
    if (applyTilt) {
      float louverTiltAdjust = -yOffset * LOUVER_TILT_COMPENSATION_PER_UNIT;
      RenderHelper.addTiltedBoxesToBufferDualColor(visorData, buffer,
          red, green, blue, innerR, innerG, innerB, alpha,
          xOffset, yOffset, zPushBack, VISOR_PIVOT_Z + zPushBack, VISOR_TILT_DEGREES,
          VISOR_CENTER_X, VISOR_CENTER_Y, louverTiltAdjust);
    } else {
      RenderHelper.addBoxesToBufferDualColor(visorData, buffer,
          red, green, blue, innerR, innerG, innerB, alpha,
          xOffset, yOffset, zPushBack, VISOR_CENTER_X, VISOR_CENTER_Y);
    }
  }

  /**
   * Returns the base facing rotation angle (without tilt) for the given EnumFacing.
   */
  private static float getBaseFacingAngle(EnumFacing facing) {
    switch (facing) {
      case SOUTH: return 180.0f;
      case WEST:  return 90.0f;
      case NORTH: return 0.0f;
      case EAST:  return 270.0f;
      default:    return 0.0f;
    }
  }

  private static List<RenderHelper.Box> selectVisorData(List<RenderHelper.Box> data12,
      List<RenderHelper.Box> data8, List<RenderHelper.Box> data4, int sectionSize) {
    if (sectionSize <= 4) return data4;
    if (sectionSize <= 8) return data8;
    return data12;
  }

  private static List<RenderHelper.Box> resolveVisorData(TrafficSignalVisorType visorType,
      int sectionSize) {
    switch (visorType) {
      case CIRCLE:
        return selectVisorData(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case TUNNEL:
        return selectVisorData(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case CUTAWAY:
        return selectVisorData(TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CAP_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CAP_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BOTH_LOUVERED:
        return selectVisorData(TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.BOTH_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.BOTH_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case VERTICAL_LOUVERED:
        return selectVisorData(TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case HORIZONTAL_LOUVERED:
        return selectVisorData(TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BARLO:
        return selectVisorData(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BARLO_VERTICAL:
        return selectVisorData(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case NONE:
        return selectVisorData(TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.NONE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.NONE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      default:
        return null;
    }
  }

  /**
   * Renders all bulb face quads in a single batched draw call. Pre-computes rotated vertex
   * positions in Java to avoid per-section GL matrix push/pop and separate draw calls.
   */
  private void renderBulbs(TrafficSignalSectionInfo[] sectionInfos, float[] sectionYPositions,
      float[] sectionXPositions, int[] sectionSizes, float zPushBack, long gameMillis) {
    // Reset GL color to white so textures are not tinted by leftover static part colors.
    // Must use GL11.glColor4f directly because GlStateManager caches state and the display
    // list replay changes GL color behind GlStateManager's back, making it skip the reset.
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    Minecraft.getMinecraft().getTextureManager().bindTexture(ATLAS_TEXTURE);

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];

      // Skip unlit sections that share a position with a LIT section (overlapping add-ons).
      // This prevents the off-state texture from overdrawing a lit bulb at the same position.
      // If ALL sections at this position are unlit, allow the FIRST one to render so the
      // off-state texture is visible (important for bi-modal/hybrid signals).
      if (!sectionInfo.isBulbLit()) {
        boolean litSectionAtSamePos = false;
        boolean earlierUnlitAtSamePos = false;
        for (int j = 0; j < sectionInfos.length; j++) {
          if (j != i && sectionYPositions[j] == sectionYPositions[i]
              && sectionXPositions[j] == sectionXPositions[i]) {
            if (sectionInfos[j].isBulbLit()) {
              litSectionAtSamePos = true;
              break;
            } else if (j < i) {
              earlierUnlitAtSamePos = true;
            }
          }
        }
        // Skip if a lit section is at this position (it takes visual priority)
        // Also skip if an earlier unlit section already rendered the off texture here
        if (litSectionAtSamePos || earlierUnlitAtSamePos) continue;
      }

      TrafficSignalBulbStyle bulbStyle = sectionInfo.getBulbStyle();
      TrafficSignalBulbType bulbType = sectionInfo.getBulbType();
      TrafficSignalBulbColor bulbColor = sectionInfo.getBulbCustomColor();
      boolean isBulbLit = sectionInfo.isBulbLit();
      TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(bulbStyle, bulbType, bulbColor, isBulbLit);

      // Bulb quad parameters: sized to section (12, 8, or 4), slightly inset to avoid visor bleed
      float fullSize = sectionSizes[i];
      float sizeScale = fullSize / 12f;
      float inset = fullSize * 0.02f;
      float size = fullSize - inset * 2f;
      float sectionOffset = (12f - fullSize) / 2f; // center smaller sections within the 12-unit slot
      float baseX = 2f + inset + sectionXPositions[i] + sectionOffset;
      float baseY = sectionYPositions[i] + inset + sectionOffset;
      // Scale bulb Z to stay just in front of the (now depth-scaled) door, plus push-back
      float z = VISOR_PIVOT_Z + (10.4f - VISOR_PIVOT_Z) * sizeScale + zPushBack;

      float u1 = texInfo.getU1();
      float v1 = texInfo.getV1();
      float u2 = texInfo.getU2();
      float v2 = texInfo.getV2();

      float rotation = texInfo.getRotation();
      if (rotation == 0f) {
        // No rotation — emit quad directly (fast path, most common for BALL type)
        buffer.pos(baseX, baseY, z).tex(u2, v2).endVertex();
        buffer.pos(baseX + size, baseY, z).tex(u1, v2).endVertex();
        buffer.pos(baseX + size, baseY + size, z).tex(u1, v1).endVertex();
        buffer.pos(baseX, baseY + size, z).tex(u2, v1).endVertex();
      } else {
        // Pre-compute rotated corners in Java to avoid GL matrix push/pop per section
        float cx = baseX + size / 2f;
        float cy = baseY + size / 2f;
        float rad = (float) Math.toRadians(rotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float halfSize = size / 2f;

        // Unrotated corners relative to center: (-h,-h), (+h,-h), (+h,+h), (-h,+h)
        float x0 = cx + (-halfSize * cos - (-halfSize) * sin);
        float y0 = cy + (-halfSize * sin + (-halfSize) * cos);
        float x1 = cx + (halfSize * cos - (-halfSize) * sin);
        float y1 = cy + (halfSize * sin + (-halfSize) * cos);
        float x2 = cx + (halfSize * cos - halfSize * sin);
        float y2 = cy + (halfSize * sin + halfSize * cos);
        float x3 = cx + (-halfSize * cos - halfSize * sin);
        float y3 = cy + (-halfSize * sin + halfSize * cos);

        buffer.pos(x0, y0, z).tex(u2, v2).endVertex();
        buffer.pos(x1, y1, z).tex(u1, v2).endVertex();
        buffer.pos(x2, y2, z).tex(u1, v1).endVertex();
        buffer.pos(x3, y3, z).tex(u2, v1).endVertex();
      }
    }

    tessellator.draw();

    // Render Barlo strobe bars (dynamic, untextured white quads)
    renderBarloStrobeBars(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes,
        zPushBack, gameMillis);
  }

  private static final float VISOR_GLOW_CORE_ALPHA = 0.18f;
  private static final float VISOR_GLOW_HALO_ALPHA = 0.07f;
  private static final float GLOW_INNER_FRAC = 0.05f;

  private static final int GLOW_SEGMENTS = 16;
  private static final float[] GLOW_COS = new float[GLOW_SEGMENTS];
  private static final float[] GLOW_SIN = new float[GLOW_SEGMENTS];
  static {
    for (int i = 0; i < GLOW_SEGMENTS; i++) {
      float angle = (float) (2.0 * Math.PI * i / GLOW_SEGMENTS);
      GLOW_COS[i] = (float) Math.cos(angle);
      GLOW_SIN[i] = (float) Math.sin(angle);
    }
  }

  private void renderVisorGlow(TrafficSignalSectionInfo[] sectionInfos,
      float[] sectionYPositions, float[] sectionXPositions, int[] sectionSizes,
      float zPushBack) {
    boolean anyLit = false;
    for (TrafficSignalSectionInfo info : sectionInfos) {
      if (info.isBulbLit()) { anyLit = true; break; }
    }
    if (!anyLit) return;

    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(false);

    float tiltSlope = (float) Math.tan(Math.toRadians(VISOR_TILT_DEGREES));
    float pivotZ = VISOR_PIVOT_Z + zPushBack;

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo info = sectionInfos[i];
      if (!info.isBulbLit()) continue;

      TrafficSignalBulbColor bulbColor = info.getBulbCustomColor();
      float r, g, b;
      switch (bulbColor) {
        case RED:    r = 1.0f;  g = 0.2f;  b = 0.1f;  break;
        case YELLOW: r = 1.0f;  g = 0.8f;  b = 0.15f; break;
        case GREEN:  r = 0.15f; g = 1.0f;  b = 0.3f;  break;
        default:     continue;
      }

      float fullSize = sectionSizes[i];
      float sizeScale = fullSize / 12f;
      float sectionOffset = (12f - fullSize) / 2f;
      float cx = 2f + sectionXPositions[i] + sectionOffset + fullSize / 2f;
      float cy = sectionYPositions[i] + sectionOffset + fullSize / 2f;

      float coreRadius = fullSize * 0.42f;
      float coreZ = VISOR_PIVOT_Z + (1.0f - VISOR_PIVOT_Z) * sizeScale + zPushBack;
      float coreYShift = -(pivotZ - coreZ) * tiltSlope;
      emitGlowDisk(buffer, cx, cy + coreYShift, coreZ, coreRadius, r, g, b, VISOR_GLOW_CORE_ALPHA);

      float haloRadius = fullSize * 0.70f;
      float haloZ = VISOR_PIVOT_Z + (-1.0f - VISOR_PIVOT_Z) * sizeScale + zPushBack;
      float haloYShift = -(pivotZ - haloZ) * tiltSlope;
      emitGlowDisk(buffer, cx, cy + haloYShift, haloZ, haloRadius, r, g, b, VISOR_GLOW_HALO_ALPHA);
    }

    tessellator.draw();

    GlStateManager.depthMask(true);
    GlStateManager.enableTexture2D();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  private static void emitGlowDisk(BufferBuilder buffer, float cx, float cy, float z,
      float radius, float r, float g, float b, float centerAlpha) {
    float iR = radius * GLOW_INNER_FRAC;

    // Outer ring: quads from inner edge (bright) to outer edge (transparent)
    for (int i = 0; i < GLOW_SEGMENTS; i++) {
      int next = (i + 1) % GLOW_SEGMENTS;
      buffer.pos(cx + radius * GLOW_COS[i], cy + radius * GLOW_SIN[i], z)
          .color(r, g, b, 0f).endVertex();
      buffer.pos(cx + radius * GLOW_COS[next], cy + radius * GLOW_SIN[next], z)
          .color(r, g, b, 0f).endVertex();
      buffer.pos(cx + iR * GLOW_COS[next], cy + iR * GLOW_SIN[next], z)
          .color(r, g, b, centerAlpha).endVertex();
      buffer.pos(cx + iR * GLOW_COS[i], cy + iR * GLOW_SIN[i], z)
          .color(r, g, b, centerAlpha).endVertex();
    }

    // Solid inner disk
    buffer.pos(cx - iR, cy - iR, z).color(r, g, b, centerAlpha).endVertex();
    buffer.pos(cx + iR, cy - iR, z).color(r, g, b, centerAlpha).endVertex();
    buffer.pos(cx + iR, cy + iR, z).color(r, g, b, centerAlpha).endVertex();
    buffer.pos(cx - iR, cy + iR, z).color(r, g, b, centerAlpha).endVertex();
  }

  private static boolean isBarloVisor(TrafficSignalVisorType type) {
    return type == TrafficSignalVisorType.BARLO || type == TrafficSignalVisorType.BARLO_VERTICAL;
  }

  /**
   * Renders Barlo Safety Beam for RED sections with BARLO or BARLO_VERTICAL visor type. Draws a
   * permanent dark mounting bar (flat black) and a flashing white strobe on top. BARLO uses a
   * horizontal bar with tunnel visor; BARLO_VERTICAL uses a vertical bar with circle visor.
   * The strobe flashes with a rapid 5-pulse pattern (500ms) followed by 800ms dark (1.3s cycle).
   * Only renders on red sections.
   */
  private void renderBarloStrobeBars(TrafficSignalSectionInfo[] sectionInfos,
      float[] sectionYPositions, float[] sectionXPositions, int[] sectionSizes, float zPushBack,
      long gameMillis) {
    // Quick check: any Barlo red sections?
    boolean hasBarlo = false;
    for (int i = 0; i < sectionInfos.length; i++) {
      if (isBarloVisor(sectionInfos[i].getVisorType())
          && sectionInfos[i].getBulbColor() == TrafficSignalBulbColor.RED) {
        hasBarlo = true;
        break;
      }
    }
    if (!hasBarlo) return;

    GlStateManager.disableTexture2D();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Always render the dark mounting bar (flat black) for all Barlo red sections
    float fb = TrafficSignalBodyColor.FLAT_BLACK.getRed();
    GL11.glColor4f(fb, fb, fb, 1f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

    for (int i = 0; i < sectionInfos.length; i++) {
      if (!isBarloVisor(sectionInfos[i].getVisorType())
          || sectionInfos[i].getBulbColor() != TrafficSignalBulbColor.RED) {
        continue;
      }
      // Scale strobe Z position to match visor depth + push-back for flush mounting
      float barZ = VISOR_PIVOT_Z + (7.0f - VISOR_PIVOT_Z) * (sectionSizes[i] / 12f) + zPushBack;
      emitBarloQuad(buffer, sectionInfos[i].getVisorType(), sectionSizes[i],
          sectionXPositions[i], sectionYPositions[i], barZ);
    }
    tessellator.draw();

    // Conditionally render the white strobe flash on top of the mounting bar. gameMillis is
    // the pause-aware game clock (threaded from render() so we don't pay the JNI
    // System.currentTimeMillis() cost per-frame on every signal head with a Barlo visor).
    long t = gameMillis % 1300L;
    boolean strobeOn = t < 500L && (t / 50L) % 2L == 1L;

    if (strobeOn) {
      GL11.glColor4f(1f, 1f, 1f, 1f);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

      for (int i = 0; i < sectionInfos.length; i++) {
        if (!isBarloVisor(sectionInfos[i].getVisorType())
            || !sectionInfos[i].isBulbLit()
            || sectionInfos[i].getBulbColor() != TrafficSignalBulbColor.RED) {
          continue;
        }
        float strobeZ = VISOR_PIVOT_Z + (6.9f - VISOR_PIVOT_Z) * (sectionSizes[i] / 12f) + zPushBack;
        emitBarloQuad(buffer, sectionInfos[i].getVisorType(), sectionSizes[i],
            sectionXPositions[i], sectionYPositions[i], strobeZ);
      }
      tessellator.draw();
    }

    GlStateManager.enableTexture2D();
  }

  /**
   * Emits a single Barlo strobe quad into the buffer. Horizontal for BARLO, vertical for
   * BARLO_VERTICAL. Centered in the section.
   */
  private void emitBarloQuad(BufferBuilder buffer, TrafficSignalVisorType visorType,
      int fullSize, float xPos, float yPos, float z) {
    float sectionOffset = (12f - fullSize) / 2f;
    float scale = fullSize / 12f;
    float sectionCenterX = 2f + xPos + sectionOffset + fullSize / 2f;
    float sectionCenterY = yPos + sectionOffset + fullSize / 2f;

    float barLong = 10.4f * scale;   // length along the long axis
    float barShort = 1.0f * scale;   // thickness

    float x1, y1, x2, y2;
    if (visorType == TrafficSignalVisorType.BARLO_VERTICAL) {
      // Vertical bar: narrow in X, tall in Y. Shortened vs horizontal to account for
      // the 9° downward visor tilt shifting the top edge closer to the body.
      float vertLong = 9.0f * scale;
      x1 = sectionCenterX - barShort / 2f;
      y1 = sectionCenterY - vertLong / 2f;
      x2 = sectionCenterX + barShort / 2f;
      y2 = sectionCenterY + vertLong / 2f;
    } else {
      // Horizontal bar: wide in X, narrow in Y
      x1 = sectionCenterX - barLong / 2f;
      y1 = sectionCenterY - barShort / 2f;
      x2 = sectionCenterX + barLong / 2f;
      y2 = sectionCenterY + barShort / 2f;
    }

    buffer.pos(x1, y1, z).endVertex();
    buffer.pos(x2, y1, z).endVertex();
    buffer.pos(x2, y2, z).endVertex();
    buffer.pos(x1, y2, z).endVertex();
  }

  // ==================== Built-in signal mount hardware ====================
  //
  // Pelco-style bracket, matching the crosswalk signal mount aesthetic: a short stub
  // coming out of the signal housing at the attachment end (top and bottom in vertical,
  // left and right ends in horizontal) → a 90° elbow → a pole-direction arm/shaft
  // heading toward the pole.
  //
  // No mount plate — the stub reads as joining directly into the signal housing.
  //
  // Brackets are always paired — one at each attachment end — regardless of mount type.
  // The type (REAR / LEFT / RIGHT) only decides which direction the elbow bends toward
  // the pole:
  //   REAR   → pole behind the signal (+Z in model space)
  //   LEFT   → pole to the signal's left (vertical mode: -X; horizontal mode: -Y / below)
  //   RIGHT  → pole to the signal's right (vertical mode: +X; horizontal mode: +Y / above)
  //
  // Rendered per-frame rather than into the display list so adjacency changes (add-on
  // placed/broken next to the signal) show up immediately without needing a TE dirty flip.

  /** Direction the pole-side leg of the mount bracket extends from the elbow. */
  private enum PoleLeg {
    REAR_POS_Z, LEFT_NEG_X, RIGHT_POS_X, DOWN_NEG_Y, UP_POS_Y
  }

  // Square stub coming out of the signal housing toward the elbow.
  private static final float STUB_SIZE = 2.5f;        // cross-section side — thicker than crosswalk
  private static final float STUB_LENGTH = 5.5f;      // length from housing to elbow centre
  // Elbow: slightly fatter than stub/tube so the 90° turn reads as a cast fitting.
  private static final float ELBOW_SIZE = 3.0f;
  // Pole-direction arm cross-section. Length is computed per-bracket so the arm reaches
  // roughly the centre of the neighbouring block where the pole sits — see BracketSpec.
  private static final float TUBE_SIZE = 2.5f;
  // Signal body back-face anchor. The signal geometry's visible detail centres around
  // z≈6–7, but that's where the visors are. The housing body itself sits behind the
  // visors toward the block's back face; z=14 is about where a mount bracket would
  // realistically bolt onto the housing's rear shell.
  private static final float BODY_Z_CENTER = 14.0f;
  // Neighbouring block centre (beyond the current block's face) on the tube axis. Used to
  // aim each bracket's arm — the arm's rotation and length are both computed per bracket
  // so its tip lands at the centre of the pole's block, matching the crosswalk signal
  // mount convention of reaching into the pole's block.
  private static final float NEIGHBOUR_CENTRE_POS = 24.0f; // block face at 16 + half block (8)
  private static final float NEIGHBOUR_CENTRE_NEG = -8.0f; // mirror: 0 - 8
  // Block centre on any axis.
  private static final float BLOCK_CENTRE = 8.0f;

  /**
   * Returns true when a signal head sits along {@code dir} from {@code pos} — either
   * directly adjacent or two blocks out through a single air-block gap. The air-gap path
   * covers double-arrow add-on signals, which are placed one block away from their main
   * signal rather than touching it, so the shared mount edge is actually two blocks apart.
   */
  private static boolean hasPairedSignalAlong(net.minecraft.world.World world, BlockPos pos,
      EnumFacing dir) {
    BlockPos adjacent = pos.offset(dir);
    if (world.getTileEntity(adjacent) instanceof TileEntityTrafficSignalHead) {
      return true;
    }
    if (world.isAirBlock(adjacent)) {
      BlockPos farther = pos.offset(dir, 2);
      return world.getTileEntity(farther) instanceof TileEntityTrafficSignalHead;
    }
    return false;
  }

  private void renderMount(TileEntityTrafficSignalHead te, IBlockState blockState,
      int[] sectionSizes,
      float[] sectionYPositions, float[] sectionXPositions, boolean horizontal,
      float zPushBack, float mountTiltAngle) {
    SignalHeadMountType mountType = te.getMountType();
    if (mountType == SignalHeadMountType.NONE) return;

    // Signal body envelope from section placements.
    float topY = -Float.MAX_VALUE, bottomY = Float.MAX_VALUE;
    float leftX = Float.MAX_VALUE, rightX = -Float.MAX_VALUE;
    for (int i = 0; i < sectionSizes.length; i++) {
      float half = sectionSizes[i] / 2.0f;
      float yCenter = sectionYPositions[i] + 6.0f; // body center Y in model space
      float xCenter = sectionXPositions[i] + 8.0f; // body center X in model space
      topY = Math.max(topY, yCenter + half);
      bottomY = Math.min(bottomY, yCenter - half);
      leftX = Math.min(leftX, xCenter - half);
      rightX = Math.max(rightX, xCenter + half);
    }

    // Adjacent-signal detection for mount-edge suppression. If another signal head sits on
    // this signal's attachment axis (above/below for vertical, left/right for horizontal),
    // the pair shares a bracket at that joint and we hide this signal's bracket on that
    // edge so the hardware doesn't double up. Double-arrow add-ons sit one block away from
    // the main signal (air gap between), so the scan also peeks two blocks out through air.
    // Adjacent-signal detection — uses the blockState threaded from render() instead of
    // calling world.getBlockState(pos) again; the caller already has it in hand.
    boolean suppressHighEnd = false, suppressLowEnd = false;
    net.minecraft.world.World world = te.getWorld();
    if (world != null) {
      BlockPos pos = te.getPos();
      if (horizontal) {
        if (blockState.getProperties().containsKey(AbstractBlockControllableSignalHead.FACING)) {
          EnumFacing facing = blockState.getValue(AbstractBlockControllableSignalHead.FACING);
          suppressLowEnd = hasPairedSignalAlong(world, pos, facing.rotateYCCW());
          suppressHighEnd = hasPairedSignalAlong(world, pos, facing.rotateY());
        }
      } else {
        suppressHighEnd = hasPairedSignalAlong(world, pos, EnumFacing.UP);
        suppressLowEnd = hasPairedSignalAlong(world, pos, EnumFacing.DOWN);
      }
    }

    TrafficSignalBodyColor color = te.getMountColor();

    // Map mount type + orientation → pole-leg direction. Same pole direction for both
    // end brackets; user-facing type is a single choice, not per-end.
    //
    // For vertical mode the viewer-perspective left/right need to account for the facing
    // rotation the TESR already applies: after the blockstate's y-rotation, a bracket leg
    // extending in model +X always ends up on the viewer's LEFT regardless of which
    // cardinal direction the signal faces (checked via cross product for N/E/S/W). So the
    // user-facing "LEFT mount type" maps to model +X, and "RIGHT mount type" maps to -X.
    PoleLeg poleLeg;
    if (horizontal) {
      switch (mountType) {
        case REAR:  poleLeg = PoleLeg.REAR_POS_Z; break;
        case LEFT:  poleLeg = PoleLeg.DOWN_NEG_Y; break; // "left mount" in horizontal = pole below
        case RIGHT: poleLeg = PoleLeg.UP_POS_Y;   break; // "right mount" in horizontal = pole above
        default: return;
      }
    } else {
      switch (mountType) {
        case REAR:  poleLeg = PoleLeg.REAR_POS_Z;  break;
        case LEFT:  poleLeg = PoleLeg.RIGHT_POS_X; break; // model +X → viewer's LEFT after facing rotation
        case RIGHT: poleLeg = PoleLeg.LEFT_NEG_X;  break; // model -X → viewer's RIGHT
        default: return;
      }
    }

    // Collect bracket specs so we can (a) batch all stub + elbow boxes into one draw, and
    // (b) issue one additional draw per bracket with a glRotatef that tilts just the arm.
    List<BracketSpec> brackets = new ArrayList<>();
    if (horizontal) {
      if (!suppressHighEnd) brackets.add(new BracketSpec(rightX, 6.0f, true,  true,  poleLeg, mountTiltAngle));
      if (!suppressLowEnd)  brackets.add(new BracketSpec(leftX,  6.0f, true,  false, poleLeg, mountTiltAngle));
    } else {
      if (!suppressHighEnd) brackets.add(new BracketSpec(8.0f, topY,    false, true,  poleLeg, mountTiltAngle));
      if (!suppressLowEnd)  brackets.add(new BracketSpec(8.0f, bottomY, false, false, poleLeg, mountTiltAngle));
    }

    if (brackets.isEmpty()) return;

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    GlStateManager.disableTexture2D();

    // Pass 1: batched stubs + elbows (no tilt).
    List<RenderHelper.Box> stubElbowBoxes = new ArrayList<>();
    for (BracketSpec spec : brackets) {
      spec.addStubAndElbow(stubElbowBoxes);
    }
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(stubElbowBoxes, buffer,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, zPushBack);
    tessellator.draw();

    // Pass 2: per-bracket arm drawn with a glRotatef pivoted at the elbow. Each bracket
    // tilts around its own axis (cross product of stub direction and pole-leg direction)
    // so the arm angles toward the signal body's midpoint rather than sticking straight
    // out at a perfect 90°.
    for (BracketSpec spec : brackets) {
      GL11.glPushMatrix();
      // The zPushBack shift is applied via addBoxesToBuffer's offset parameter for the
      // unrotated boxes; for the rotation pivot we need the final world-space elbow Z,
      // which already includes zPushBack.
      GL11.glTranslatef(spec.elbowX, spec.elbowY, spec.elbowZ + zPushBack);
      GL11.glRotatef(spec.tiltAngleDegrees, spec.tiltAxisX, spec.tiltAxisY, spec.tiltAxisZ);
      GL11.glTranslatef(-spec.elbowX, -spec.elbowY, -(spec.elbowZ + zPushBack));

      List<RenderHelper.Box> armBoxes = new ArrayList<>();
      spec.addArm(armBoxes);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      RenderHelper.addBoxesToBuffer(armBoxes, buffer,
          color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, zPushBack);
      tessellator.draw();
      GL11.glPopMatrix();
    }

    GlStateManager.enableTexture2D();
  }

  /**
   * One built-in mount bracket. Precomputes the stub / elbow / arm geometry plus the pivot
   * point and tilt-axis needed to draw the arm with a slight angle toward the signal's
   * midpoint — letting {@link #renderMount} issue a batched stub+elbow pass followed by a
   * per-bracket rotated arm pass.
   *
   * <p>Pole-leg direction and stub direction are resolved to axis vectors at construction,
   * and the tilt axis is their cross product (so it's always perpendicular to both, with
   * the sign that rotates the arm toward -stubDir i.e. back toward the signal centre).
   */
  private static final class BracketSpec {
    // Geometry helpers for addStubAndElbow / addArm.
    private final int stubAxisIdx;    // axis the stub runs along (0=X, 1=Y, 2=Z)
    private final float stubSign;     // +1 if attaching at the high end, -1 at the low end
    private final int crossAxisIdx1;  // body-parallel axis 1
    private final int crossAxisIdx2;  // body-parallel axis 2
    private final float crossCenter1;
    private final float crossCenter2;
    private final float housingEdge;  // start of the stub on stubAxisIdx
    private final float stubEnd;      // end of the stub (= elbow centre on stubAxisIdx)

    private final int tubeAxisIdx;
    private final float tubeSign;
    // Length of the pole-direction arm from the elbow centre, chosen so the far end
    // lands near the centre of the neighbouring block in the tube direction.
    private final float tubeLength;

    // Elbow centre in model space — pivot for the arm's tilt rotation.
    final float elbowX, elbowY, elbowZ;

    // Tilt axis vector, normalised. Passed to glRotatef as the axis arguments. Computed
    // per bracket so the arm rotates from pure pole-axis direction onto the direction from
    // elbow → neighbour block centre (i.e., aims the arm at the pole's block).
    final float tiltAxisX, tiltAxisY, tiltAxisZ;
    // Angle in degrees for the tilt rotation.
    final float tiltAngleDegrees;

    BracketSpec(float bodyCenterX, float bodyCenterY, boolean horizontalSignal,
        boolean isHighEnd, PoleLeg poleLeg, float tiltAngleDeg) {
      this.stubSign = isHighEnd ? 1f : -1f;
      if (horizontalSignal) {
        this.crossAxisIdx1 = 1;  // Y
        this.crossAxisIdx2 = 2;  // Z
        this.stubAxisIdx = 0;    // X
      } else {
        this.crossAxisIdx1 = 0;  // X
        this.crossAxisIdx2 = 2;  // Z
        this.stubAxisIdx = 1;    // Y
      }
      this.housingEdge = horizontalSignal ? bodyCenterX : bodyCenterY;
      this.crossCenter1 = horizontalSignal ? 6.0f : 8.0f;
      this.crossCenter2 = BODY_Z_CENTER;
      this.stubEnd = housingEdge + stubSign * STUB_LENGTH;

      // Pole-leg direction → axis + sign.
      switch (poleLeg) {
        case REAR_POS_Z:  this.tubeAxisIdx = 2; this.tubeSign = +1f; break;
        case LEFT_NEG_X:  this.tubeAxisIdx = 0; this.tubeSign = -1f; break;
        case RIGHT_POS_X: this.tubeAxisIdx = 0; this.tubeSign = +1f; break;
        case UP_POS_Y:    this.tubeAxisIdx = 1; this.tubeSign = +1f; break;
        case DOWN_NEG_Y:  this.tubeAxisIdx = 1; this.tubeSign = -1f; break;
        default:          this.tubeAxisIdx = 2; this.tubeSign = +1f; break;
      }

      // Elbow centre in model coords. On the stub axis, the elbow sits at stubEnd; on
      // the two cross axes it sits at the centre of the body's cross-section.
      float[] elbowCoords = new float[3];
      elbowCoords[stubAxisIdx] = stubEnd;
      elbowCoords[crossAxisIdx1] = crossCenter1;
      elbowCoords[crossAxisIdx2] = crossCenter2;
      this.elbowX = elbowCoords[0];
      this.elbowY = elbowCoords[1];
      this.elbowZ = elbowCoords[2];

      // Target is chosen per axis on a uniform rule so the arm always angles in exactly
      // two axes (tube axis + the third "cross" axis), never three:
      //   - tube axis   → neighbour block centre in the pole direction
      //   - stub axis   → held at the elbow's coord (no angle along the attachment axis)
      //   - cross axis  → block centre (8), i.e. meet the pole at its block's centre
      //
      // For a vertical signal with REAR mount: tube=Z, stub=Y, cross=X → angle in X+Z.
      // For a vertical signal with LEFT/RIGHT: tube=X, stub=Y, cross=Z → angle in X+Z.
      // For a horizontal signal with REAR:     tube=Z, stub=X, cross=Y → angle in Y+Z.
      // For a horizontal signal with UP/DOWN:  tube=Y, stub=X, cross=Z → angle in Y+Z.
      //
      // In all four cases the stub axis is held constant so the bracket angles in the
      // bend plane and the perpendicular (cross) plane only. For vertical signals this
      // matches the typical layout of a vertical pole continuing past the signal's height;
      // for horizontal signals with UP/DOWN poles we assume a solid ceiling / mast-arm
      // surface the bracket can reach.
      float targetX = (stubAxisIdx == 0) ? elbowX
          : (tubeAxisIdx == 0) ? BLOCK_CENTRE + tubeSign * 16f
          : BLOCK_CENTRE;
      float targetY = (stubAxisIdx == 1) ? elbowY
          : (tubeAxisIdx == 1) ? BLOCK_CENTRE + tubeSign * 16f
          : BLOCK_CENTRE;
      float targetZ = (stubAxisIdx == 2) ? elbowZ
          : (tubeAxisIdx == 2) ? BLOCK_CENTRE + tubeSign * 16f
          : BLOCK_CENTRE;

      // Tilt compensation. The signal body tilt rotates the whole local frame around Y
      // at the body's pivot; rendering the arm straight at our un-tilted target would
      // then get rotated along with everything else and miss the world-fixed pole. To
      // keep the arm tip landing at the neighbour's world position regardless of tilt,
      // apply the INVERSE tilt rotation to the target: the rotation from the GL stack
      // then rotates that back, net-zero against the tilt, and the arm points at the
      // actual world-neighbour centre.
      if (Math.abs(tiltAngleDeg) > 0.0001f) {
        double rad = Math.toRadians(-tiltAngleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float relX = targetX - BLOCK_CENTRE;
        float relZ = targetZ - BLOCK_CENTRE;
        targetX = relX * cos + relZ * sin + BLOCK_CENTRE;
        targetZ = -relX * sin + relZ * cos + BLOCK_CENTRE;
      }

      float dx = targetX - elbowX;
      float dy = targetY - elbowY;
      float dz = targetZ - elbowZ;
      float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

      // Pre-tilt pole direction (unit vector along the pole axis).
      float preDirX = tubeAxisIdx == 0 ? tubeSign : 0f;
      float preDirY = tubeAxisIdx == 1 ? tubeSign : 0f;
      float preDirZ = tubeAxisIdx == 2 ? tubeSign : 0f;

      // Desired arm direction: unit vector from elbow toward neighbour-block centre.
      float desiredX = distance > 0 ? dx / distance : preDirX;
      float desiredY = distance > 0 ? dy / distance : preDirY;
      float desiredZ = distance > 0 ? dz / distance : preDirZ;

      // Arm length. Default is 3D distance from elbow to neighbour centre, which lands the
      // arm's far-face *centre* at the pole-block centre. That's fine at body-tilt=0 for a
      // horizontal-plane mount because the far face is axis-aligned with the pole — no
      // corners protrude past. At LEFT_ANGLE / RIGHT_ANGLE body tilt (±45°) the elbow
      // rotates out of the pole axis, so the arm's world direction is tilted relative to
      // the world pole direction: the far face is still perpendicular to the arm, but its
      // corners now stick out by halfSize * sin(worldTubeAngle) past the face centre in
      // the pole's world direction. For REAR mount that protrusion is ~0.42 units —
      // visible as the arm pushing out the back side of the pole block.
      //
      // Fix: shorten the tube by halfSize * tan(worldTubeAngle) so the far-face *corners*
      // (not centre) land exactly at the neighbour-block centre. Scoped to horizontal-
      // plane mounts (REAR/LEFT/RIGHT) because UP/DOWN mounts have a naturally diagonal
      // arm even at no tilt (elbow at Y=6, pole at Y=24/-8), and the user wants that case
      // to keep driving into the ceiling/floor block past its centre.
      //
      // World tube angle without needing baseFacing: world pole dir = preDir rotated by
      // +baseFacing; world tube dir = localDesiredDir rotated by +(baseFacing + tilt).
      // Relative angle between them = angle between localDesiredDir and preDir rotated by
      // -tilt (both rotations share a +baseFacing that cancels out).
      float unshrunkTubeLength = distance;
      if (tubeAxisIdx != 1) {
        double radInvTilt = Math.toRadians(-tiltAngleDeg);
        float cosInvTilt = (float) Math.cos(radInvTilt);
        float sinInvTilt = (float) Math.sin(radInvTilt);
        float rotPreDirX = preDirX * cosInvTilt + preDirZ * sinInvTilt;
        float rotPreDirZ = -preDirX * sinInvTilt + preDirZ * cosInvTilt;
        float cosWorldTubeAngle =
            desiredX * rotPreDirX + desiredY * preDirY + desiredZ * rotPreDirZ;
        if (cosWorldTubeAngle > 1f) cosWorldTubeAngle = 1f;
        if (cosWorldTubeAngle < -1f) cosWorldTubeAngle = -1f;
        float worldTubeAngleRad = (float) Math.acos(cosWorldTubeAngle);
        float tanWorld = (float) Math.tan(worldTubeAngleRad);
        if (tanWorld > 3f) tanWorld = 3f;  // cap to avoid degenerate shrinks near 90°
        unshrunkTubeLength = distance - (TUBE_SIZE / 2f) * tanWorld;
      }
      this.tubeLength = Math.max(0.5f, unshrunkTubeLength);

      // Rotation from pre-tilt onto desired: axis = pre × desired, angle = arccos(pre·desired).
      float axisX = preDirY * desiredZ - preDirZ * desiredY;
      float axisY = preDirZ * desiredX - preDirX * desiredZ;
      float axisZ = preDirX * desiredY - preDirY * desiredX;
      float axisMag = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
      float dot = preDirX * desiredX + preDirY * desiredY + preDirZ * desiredZ;
      // Clamp for numerical safety before acos.
      if (dot > 1f) dot = 1f;
      if (dot < -1f) dot = -1f;
      float angleRad = (float) Math.acos(dot);
      if (axisMag > 1e-4f) {
        this.tiltAxisX = axisX / axisMag;
        this.tiltAxisY = axisY / axisMag;
        this.tiltAxisZ = axisZ / axisMag;
        this.tiltAngleDegrees = (float) Math.toDegrees(angleRad);
      } else {
        // Pre and desired are parallel (or anti-parallel): no rotation needed (or would
        // be undefined). Default axis is harmless because angle is 0.
        this.tiltAxisX = 1f;
        this.tiltAxisY = 0f;
        this.tiltAxisZ = 0f;
        this.tiltAngleDegrees = 0f;
      }
    }

    /** Appends the (non-tilted) stub and elbow boxes. */
    void addStubAndElbow(List<RenderHelper.Box> boxes) {
      // Stub — square cross-section running from housingEdge to stubEnd.
      float[] stubFrom = new float[3];
      float[] stubTo = new float[3];
      stubFrom[crossAxisIdx1] = crossCenter1 - STUB_SIZE / 2f;
      stubTo[crossAxisIdx1]   = crossCenter1 + STUB_SIZE / 2f;
      stubFrom[crossAxisIdx2] = crossCenter2 - STUB_SIZE / 2f;
      stubTo[crossAxisIdx2]   = crossCenter2 + STUB_SIZE / 2f;
      stubFrom[stubAxisIdx]   = Math.min(housingEdge, stubEnd);
      stubTo[stubAxisIdx]     = Math.max(housingEdge, stubEnd);
      boxes.add(new RenderHelper.Box(stubFrom, stubTo));

      // Elbow — chunkier cube centred on the joint.
      float[] elbowFrom = new float[3];
      float[] elbowTo = new float[3];
      elbowFrom[crossAxisIdx1] = crossCenter1 - ELBOW_SIZE / 2f;
      elbowTo[crossAxisIdx1]   = crossCenter1 + ELBOW_SIZE / 2f;
      elbowFrom[crossAxisIdx2] = crossCenter2 - ELBOW_SIZE / 2f;
      elbowTo[crossAxisIdx2]   = crossCenter2 + ELBOW_SIZE / 2f;
      float elbowCoord = stubEnd;
      elbowFrom[stubAxisIdx] = elbowCoord - ELBOW_SIZE / 2f;
      elbowTo[stubAxisIdx]   = elbowCoord + ELBOW_SIZE / 2f;
      boxes.add(new RenderHelper.Box(elbowFrom, elbowTo));
    }

    /**
     * Appends the arm box. Rendered under a glPushMatrix/glRotatef(ARM_TILT_DEGREES,
     * tiltAxis*, ...)/glPopMatrix wrapper anchored at the elbow centre, so the box is
     * authored axis-aligned but ends up angled in world space.
     */
    void addArm(List<RenderHelper.Box> boxes) {
      float[] tubeFrom = new float[3];
      float[] tubeTo = new float[3];
      // Cross-section on the two non-tube axes. For the stub axis: centre on the elbow.
      // For whichever body-parallel axis the tube doesn't run along: centre on the body.
      for (int axis = 0; axis < 3; axis++) {
        if (axis == tubeAxisIdx) continue;
        float c;
        if (axis == stubAxisIdx) {
          c = stubEnd;
        } else if (axis == crossAxisIdx1) {
          c = crossCenter1;
        } else {
          c = crossCenter2;
        }
        tubeFrom[axis] = c - TUBE_SIZE / 2f;
        tubeTo[axis]   = c + TUBE_SIZE / 2f;
      }
      // Anchor on the tube axis at the elbow and extend in tubeSign direction.
      float tubeAnchor;
      if (tubeAxisIdx == stubAxisIdx) {
        tubeAnchor = stubEnd;
      } else if (tubeAxisIdx == crossAxisIdx1) {
        tubeAnchor = crossCenter1;
      } else {
        tubeAnchor = crossCenter2;
      }
      float tubeStart = tubeAnchor;
      float tubeEnd = tubeAnchor + tubeSign * tubeLength;
      tubeFrom[tubeAxisIdx] = Math.min(tubeStart, tubeEnd);
      tubeTo[tubeAxisIdx]   = Math.max(tubeStart, tubeEnd);
      boxes.add(new RenderHelper.Box(tubeFrom, tubeTo));
    }
  }
}
