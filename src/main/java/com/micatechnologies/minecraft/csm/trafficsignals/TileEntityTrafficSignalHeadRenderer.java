package com.micatechnologies.minecraft.csm.trafficsignals;

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
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
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
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;


public class TileEntityTrafficSignalHeadRenderer extends
    TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  private static final ResourceLocation ATLAS_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");

  // Per-tile-entity display list cache (keyed by BlockPos to avoid shared state bug)
  private final Map<BlockPos, Integer> displayListCache = new HashMap<>();

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
  }

  @Override
  public void render(TileEntityTrafficSignalHead te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    // Gather block state information
    IBlockState blockState = te.getWorld().getBlockState(te.getPos());
    EnumFacing facing = blockState.getValue(AbstractBlockControllableSignalHead.FACING);
    int signalColorState = blockState.getValue(AbstractBlockControllableSignalHead.COLOR);

    // Get per-block-type Y offset for signal positioning
    float signalYOffset = 0.0f;
    if (blockState.getBlock() instanceof AbstractBlockControllableSignalHead) {
      signalYOffset = ((AbstractBlockControllableSignalHead) blockState.getBlock()).getSignalYOffset();
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

    // Set fullbright lightmap so signal textures aren't dimmed by world lighting
    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    // Push matrix once
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);
    GL11.glScaled(0.0625, 0.0625, 0.0625);
    GL11.glTranslated(8, 8, 8);

    // Apply rotation once
    float rotationAngle = bodyDirection.getRotation();
    GL11.glRotatef(rotationAngle, 0, 1, 0);
    GL11.glTranslated(-8, -8, -8);

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
      sectionYPositions = signalBlock.getSectionYPositions(sectionCount);
      sectionXPositions = signalBlock.getSectionXPositions(sectionCount);
      sectionSizes = signalBlock.getSectionSizes(sectionCount);
      horizontal = signalBlock.isHorizontal();
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

    BlockPos pos = te.getPos();
    Integer displayList = displayListCache.get(pos);
    if (displayList == null || te.isStateDirty()) {
      if (displayList != null) {
        GL11.glDeleteLists(displayList, 1);
      }
      displayList = GL11.glGenLists(1);
      displayListCache.put(pos, displayList);
      GL11.glNewList(displayList, GL11.GL_COMPILE);
      renderStaticParts(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, horizontal, zPushBack);
      GL11.glEndList();
      te.clearDirtyFlag();
    }
    GL11.glCallList(displayList);

    renderBulbs(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, zPushBack);

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

      addVisorQuadsToBuffer(visorType, buffer, visorColor.getRed(), visorColor.getGreen(),
          visorColor.getBlue(), 1.0f, xOffset, yOffset, sectionSizes[i], zPushBack);
    }
    tessellator.draw();

    GlStateManager.enableTexture2D();
  }

  // Downward tilt angle for visors (degrees). Makes bulbs visible from below, as real
  // traffic signals are mounted high on mast arms. Pivot is at the body face (z=11).
  private static final float VISOR_TILT_DEGREES = 6.5f;
  private static final float VISOR_PIVOT_Z = 11.0f;

  private void addVisorQuadsToBuffer(TrafficSignalVisorType visorType, BufferBuilder buffer,
      float red, float green, float blue, float alpha, float xOffset, float yOffset,
      int sectionSize, float zPushBack) {
    List<RenderHelper.Box> visorData;
    boolean applyTilt = true;
    switch (visorType) {
      case CIRCLE:
        visorData = selectVisorData(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case TUNNEL:
        visorData = selectVisorData(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case CUTAWAY:
        visorData = selectVisorData(TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CAP_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CAP_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case BOTH_LOUVERED:
        visorData = selectVisorData(TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.BOTH_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.BOTH_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case VERTICAL_LOUVERED:
        visorData = selectVisorData(TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case HORIZONTAL_LOUVERED:
        visorData = selectVisorData(TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case BARLO:
        visorData = selectVisorData(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case BARLO_VERTICAL:
        visorData = selectVisorData(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
        break;
      case NONE:
        visorData = selectVisorData(TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA,
            TrafficSignalVertexData.NONE_VISOR_8INCH_VERTEX_DATA,
            TrafficSignalVertexData.NONE_VISOR_4INCH_VERTEX_DATA, sectionSize);
        applyTilt = false;
        break;
      default:
        return;
    }
    if (applyTilt) {
      RenderHelper.addTiltedBoxesToBuffer(visorData, buffer, red, green, blue, alpha,
          xOffset, yOffset, zPushBack, VISOR_PIVOT_Z + zPushBack, VISOR_TILT_DEGREES);
    } else {
      RenderHelper.addBoxesToBuffer(visorData, buffer, red, green, blue, alpha,
          xOffset, yOffset, zPushBack);
    }
  }

  private static List<RenderHelper.Box> selectVisorData(List<RenderHelper.Box> data12,
      List<RenderHelper.Box> data8, List<RenderHelper.Box> data4, int sectionSize) {
    if (sectionSize <= 4) return data4;
    if (sectionSize <= 8) return data8;
    return data12;
  }

  /**
   * Renders all bulb face quads in a single batched draw call. Pre-computes rotated vertex
   * positions in Java to avoid per-section GL matrix push/pop and separate draw calls.
   */
  private void renderBulbs(TrafficSignalSectionInfo[] sectionInfos, float[] sectionYPositions,
      float[] sectionXPositions, int[] sectionSizes, float zPushBack) {
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
    renderBarloStrobeBars(sectionInfos, sectionYPositions, sectionXPositions, sectionSizes, zPushBack);
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
      float[] sectionYPositions, float[] sectionXPositions, int[] sectionSizes, float zPushBack) {
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

    // Conditionally render the white strobe flash on top of the mounting bar
    long t = System.currentTimeMillis() % 1300L;
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
      // Vertical bar: narrow in X, tall in Y
      x1 = sectionCenterX - barShort / 2f;
      y1 = sectionCenterY - barLong / 2f;
      x2 = sectionCenterX + barShort / 2f;
      y2 = sectionCenterY + barLong / 2f;
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
}
