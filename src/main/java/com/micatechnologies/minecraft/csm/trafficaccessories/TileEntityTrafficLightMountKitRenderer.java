package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
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

  // Aluminum alloy gray — matches real Pelco Astro-brac cast aluminum finish
  private static final float COLOR_R = 0.62f;
  private static final float COLOR_G = 0.63f;
  private static final float COLOR_B = 0.64f;

  // Model-space constants (16 units = 1 block)
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;

  // Bar dimensions (from existing static mount models)
  private static final float BAR_THICKNESS_THIN = 1.0f;  // thin axis (1 unit)
  private static final float BAR_THICKNESS_WIDE = 2.0f;  // wide axis (2 units)
  private static final float BAR_FRONT_Z = -4.0f;        // front extent of bars
  private static final float BAR_BACK_Z = 16.0f;         // back extent of bars
  private static final float SPINE_FRONT_Z = 15.0f;      // spine sits at back face
  private static final float SPINE_BACK_Z = 16.0f;

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
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

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

  private void renderBracket(TileEntityTrafficLightMountKit te, EnumFacing facing) {
    SignalInfo info = detectSignals(te, facing);

    List<RenderHelper.Box> boxes = new ArrayList<>(3);

    if (info.horizontal) {
      // Horizontal signal: vertical end bars on left/right, horizontal rail at back
      float barMinY = CENTER_Y - BAR_THICKNESS_WIDE / 2f;
      float barMaxY = CENTER_Y + BAR_THICKNESS_WIDE / 2f;

      // Left bar — flush with signal left edge, extends outward
      float leftBarX = info.minX - BAR_THICKNESS_THIN;
      boxes.add(new RenderHelper.Box(
          new float[]{leftBarX, barMinY, BAR_FRONT_Z},
          new float[]{leftBarX + BAR_THICKNESS_THIN, barMaxY, BAR_BACK_Z}));

      // Right bar — flush with signal right edge, extends outward
      float rightBarX = info.maxX;
      boxes.add(new RenderHelper.Box(
          new float[]{rightBarX, barMinY, BAR_FRONT_Z},
          new float[]{rightBarX + BAR_THICKNESS_THIN, barMaxY, BAR_BACK_Z}));

      // Horizontal rail connecting the two bars at the back
      boxes.add(new RenderHelper.Box(
          new float[]{leftBarX + BAR_THICKNESS_THIN, barMinY, SPINE_FRONT_Z},
          new float[]{rightBarX, barMaxY, SPINE_BACK_Z}));
    } else {
      // Vertical signal: horizontal end bars on top/bottom, vertical spine at back
      float barMinX = CENTER_X - BAR_THICKNESS_WIDE / 2f;
      float barMaxX = CENTER_X + BAR_THICKNESS_WIDE / 2f;

      // Top bar — flush with signal top edge, extends upward
      float topBarY = info.maxY;
      boxes.add(new RenderHelper.Box(
          new float[]{barMinX, topBarY, BAR_FRONT_Z},
          new float[]{barMaxX, topBarY + BAR_THICKNESS_THIN, BAR_BACK_Z}));

      // Bottom bar — flush with signal bottom edge, extends downward
      float bottomBarY = info.minY - BAR_THICKNESS_THIN;
      boxes.add(new RenderHelper.Box(
          new float[]{barMinX, bottomBarY, BAR_FRONT_Z},
          new float[]{barMaxX, bottomBarY + BAR_THICKNESS_THIN, BAR_BACK_Z}));

      // Vertical spine connecting the two bars at the back
      boxes.add(new RenderHelper.Box(
          new float[]{barMinX, bottomBarY + BAR_THICKNESS_THIN, SPINE_FRONT_Z},
          new float[]{barMaxX, topBarY, SPINE_BACK_Z}));
    }

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(boxes, buffer, COLOR_R, COLOR_G, COLOR_B, 1.0f, 0, 0, 0);
    tessellator.draw();
    GlStateManager.enableTexture2D();
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
    return te.getWorld().getTileEntity(pos) instanceof TileEntityTrafficSignalHead;
  }

  /**
   * Reads signal info from a single block position. Returns null if the block is not a
   * valid signal head.
   */
  @Nullable
  private SignalInfo readSignalAt(TileEntityTrafficLightMountKit te, BlockPos pos) {
    TileEntity checkTe = te.getWorld().getTileEntity(pos);
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
        signalBlock.getSectionYPositions(sectionCount),
        signalBlock.getSectionXPositions(sectionCount),
        signalBlock.getSignalYOffset(),
        signalBlock.isHorizontal());
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
      case SOUTH: return 0f;
      case WEST:  return 90f;
      case NORTH: return 180f;
      case EAST:  return 270f;
      default:    return 0f;
    }
  }

  private static class SignalInfo {
    float minX, maxX, minY, maxY;
    boolean horizontal;
  }
}
