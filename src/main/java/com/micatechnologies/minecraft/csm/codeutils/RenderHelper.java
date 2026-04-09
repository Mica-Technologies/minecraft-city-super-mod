package com.micatechnologies.minecraft.csm.codeutils;

import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Utility class for rendering cubes and other shapes in TESR-based rendering. Supports solid color
 * rendering, per-face textured rendering, and additional geometric rendering methods.
 */
public class RenderHelper {

  // New: Add a triangle fan to the buffer (for circles, efficient curves)
  public static void addTriangleFanToBuffer(BufferBuilder buffer, float centerX, float centerY, float centerZ,
      List<float[]> perimeterPoints, float red, float green, float blue, float alpha) {
    // Center vertex
    buffer.pos(centerX, centerY, centerZ).color(red, green, blue, alpha).endVertex();

    // Perimeter vertices
    for (float[] point : perimeterPoints) {
      buffer.pos(point[0], point[1], point[2]).color(red, green, blue, alpha).endVertex();
    }

    // Close the fan by repeating the first perimeter point
    if (!perimeterPoints.isEmpty()) {
      float[] first = perimeterPoints.get(0);
      buffer.pos(first[0], first[1], first[2]).color(red, green, blue, alpha).endVertex();
    }
  }

  public static void drawCuboidColoredbySize(double x, double y, double z, double width,
      double height, double depth, float red, float green, float blue, float alpha) {
    drawCuboidColored(x, y, z, x + width, y + height, z + depth, red, green, blue, alpha);
  }

  public static void drawCuboidColored(double x1, double y1, double z1, double x2, double y2,
      double z2, float red, float green, float blue, float alpha) {
    // Get tessellator and buffer
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Start a single buffer for the cuboid
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    addCuboidVerticesToBuffer(buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);
    tessellator.draw();
  }

  // New method: Add cuboid vertices to an existing buffer (for batching)
  private static void addCuboidVerticesToBuffer(BufferBuilder buffer, double x1, double y1, double z1,
      double x2, double y2, double z2, float red, float green, float blue, float alpha) {
    // Front
    buffer.pos(x1, y1, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y1, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y2, z2).color(red, green, blue, alpha).endVertex();

    // Back
    buffer.pos(x2, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(red, green, blue, alpha).endVertex();

    // Left
    buffer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y1, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y2, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(red, green, blue, alpha).endVertex();

    // Right
    buffer.pos(x2, y1, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();

    // Top
    buffer.pos(x1, y2, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(red, green, blue, alpha).endVertex();

    // Bottom
    buffer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y1, z1).color(red, green, blue, alpha).endVertex();
    buffer.pos(x2, y1, z2).color(red, green, blue, alpha).endVertex();
    buffer.pos(x1, y1, z2).color(red, green, blue, alpha).endVertex();
  }

  // Updated: Now adds to a buffer instead of drawing per box
  public static void addBoxesToBuffer(List<Box> boxes, BufferBuilder buffer, float red, float green, float blue, float alpha,
      float xOffset, float yOffset, float zOffset) {
    for (Box box : boxes) {
      float x1 = box.from[0] + xOffset, y1 = box.from[1] + yOffset, z1 = box.from[2] + zOffset;
      float x2 = box.to[0] + xOffset, y2 = box.to[1] + yOffset, z2 = box.to[2] + zOffset;
      addCuboidVerticesToBuffer(buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);
    }
  }

  /**
   * Adds boxes to the buffer with a downward tilt applied. Vertices are shifted downward
   * proportionally to their distance from the pivot Z (where the visor meets the body).
   * This creates a realistic visor angle so bulbs are visible from below.
   *
   * @param pivotZ     the Z coordinate where the visor attaches to the body (no shift here)
   * @param tiltAngleDeg downward tilt in degrees (positive = front edge tilts down)
   */
  public static void addTiltedBoxesToBuffer(List<Box> boxes, BufferBuilder buffer,
      float red, float green, float blue, float alpha,
      float xOffset, float yOffset, float zOffset,
      float pivotZ, float tiltAngleDeg) {
    float tiltSlope = (float) Math.tan(Math.toRadians(tiltAngleDeg));
    for (Box box : boxes) {
      float x1 = box.from[0] + xOffset, y1 = box.from[1] + yOffset, z1 = box.from[2] + zOffset;
      float x2 = box.to[0] + xOffset, y2 = box.to[1] + yOffset, z2 = box.to[2] + zOffset;

      // Y shift per vertex based on distance from pivot (positive distance = forward = shift down)
      float yShift1 = -(pivotZ - z1) * tiltSlope;
      float yShift2 = -(pivotZ - z2) * tiltSlope;

      // Front face (z2 side) — both y corners get yShift2
      buffer.pos(x1, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();

      // Back face (z1 side)
      buffer.pos(x2, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();

      // Left face (x1 side) — z1 edge uses yShift1, z2 edge uses yShift2
      buffer.pos(x1, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();

      // Right face (x2 side)
      buffer.pos(x2, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();

      // Top face — front edge (z2) shifted, back edge (z1) shifted differently
      buffer.pos(x1, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(red, green, blue, alpha).endVertex();

      // Bottom face
      buffer.pos(x1, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y1 + yShift1, z1).color(red, green, blue, alpha).endVertex();
      buffer.pos(x2, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
      buffer.pos(x1, y1 + yShift2, z2).color(red, green, blue, alpha).endVertex();
    }
  }

  /**
   * Adds boxes to the buffer with dual coloring: outside faces use the outer color, inside faces
   * (those facing toward the visor center) use the inner color. Front/back faces (Z axis) always
   * use the outer color since they are the visible rim and attachment edges.
   *
   * @param centerX X coordinate of the visor center (used to determine inside vs outside)
   * @param centerY Y coordinate of the visor center (used to determine inside vs outside)
   */
  public static void addBoxesToBufferDualColor(List<Box> boxes, BufferBuilder buffer,
      float outerR, float outerG, float outerB,
      float innerR, float innerG, float innerB,
      float alpha, float xOffset, float yOffset, float zOffset,
      float centerX, float centerY) {
    for (Box box : boxes) {
      float x1 = box.from[0] + xOffset, y1 = box.from[1] + yOffset, z1 = box.from[2] + zOffset;
      float x2 = box.to[0] + xOffset, y2 = box.to[1] + yOffset, z2 = box.to[2] + zOffset;
      float midX = (box.from[0] + box.to[0]) / 2f + xOffset;
      float midY = (box.from[1] + box.to[1]) / 2f + yOffset;

      addCuboidVerticesDualColor(buffer, x1, y1, z1, x2, y2, z2,
          outerR, outerG, outerB, innerR, innerG, innerB, alpha,
          midX, midY, centerX + xOffset, centerY + yOffset);
    }
  }

  /**
   * Adds tilted boxes to the buffer with dual coloring. Same inside/outside logic as
   * {@link #addBoxesToBufferDualColor} but with the visor downward tilt applied.
   */
  public static void addTiltedBoxesToBufferDualColor(List<Box> boxes, BufferBuilder buffer,
      float outerR, float outerG, float outerB,
      float innerR, float innerG, float innerB,
      float alpha, float xOffset, float yOffset, float zOffset,
      float pivotZ, float tiltAngleDeg,
      float centerX, float centerY) {
    float tiltSlope = (float) Math.tan(Math.toRadians(tiltAngleDeg));
    for (Box box : boxes) {
      float x1 = box.from[0] + xOffset, y1 = box.from[1] + yOffset, z1 = box.from[2] + zOffset;
      float x2 = box.to[0] + xOffset, y2 = box.to[1] + yOffset, z2 = box.to[2] + zOffset;
      float midX = (box.from[0] + box.to[0]) / 2f + xOffset;
      float midY = (box.from[1] + box.to[1]) / 2f + yOffset;

      float yShift1 = -(pivotZ - z1) * tiltSlope;
      float yShift2 = -(pivotZ - z2) * tiltSlope;

      // Determine inside/outside per face based on box center vs visor center.
      // Left/right faces always use dual coloring.
      // Top/bottom faces only use dual coloring when the box is clearly in the
      // top or bottom arc of the visor curve (1.5+ units from center Y). Near the
      // center, the visor's stepped-box geometry has partial-depth gaps where
      // inner-colored top/bottom faces would peek through the exterior.
      float cx = centerX + xOffset;
      float cy = centerY + yOffset;
      boolean leftInside = midX > cx;
      boolean rightInside = midX < cx;
      float tbMargin = 0.5f;
      boolean bottomInside = midY > cy + tbMargin;
      boolean topInside = midY < cy - tbMargin;

      float lr, lg, lb, rr, rg, rb, br, bg, bb, tr, tg, tb;
      lr = leftInside ? innerR : outerR; lg = leftInside ? innerG : outerG; lb = leftInside ? innerB : outerB;
      rr = rightInside ? innerR : outerR; rg = rightInside ? innerG : outerG; rb = rightInside ? innerB : outerB;
      br = bottomInside ? innerR : outerR; bg = bottomInside ? innerG : outerG; bb = bottomInside ? innerB : outerB;
      tr = topInside ? innerR : outerR; tg = topInside ? innerG : outerG; tb = topInside ? innerB : outerB;

      // Front face (z2) — outer color (visible rim)
      buffer.pos(x1, y1 + yShift2, z2).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x2, y1 + yShift2, z2).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x1, y2 + yShift2, z2).color(outerR, outerG, outerB, alpha).endVertex();

      // Back face (z1) — outer color (attachment edge)
      buffer.pos(x2, y1 + yShift1, z1).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x1, y1 + yShift1, z1).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(outerR, outerG, outerB, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(outerR, outerG, outerB, alpha).endVertex();

      // Left face (x1)
      buffer.pos(x1, y1 + yShift1, z1).color(lr, lg, lb, alpha).endVertex();
      buffer.pos(x1, y1 + yShift2, z2).color(lr, lg, lb, alpha).endVertex();
      buffer.pos(x1, y2 + yShift2, z2).color(lr, lg, lb, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(lr, lg, lb, alpha).endVertex();

      // Right face (x2)
      buffer.pos(x2, y1 + yShift2, z2).color(rr, rg, rb, alpha).endVertex();
      buffer.pos(x2, y1 + yShift1, z1).color(rr, rg, rb, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(rr, rg, rb, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(rr, rg, rb, alpha).endVertex();

      // Top face (y2)
      buffer.pos(x1, y2 + yShift2, z2).color(tr, tg, tb, alpha).endVertex();
      buffer.pos(x2, y2 + yShift2, z2).color(tr, tg, tb, alpha).endVertex();
      buffer.pos(x2, y2 + yShift1, z1).color(tr, tg, tb, alpha).endVertex();
      buffer.pos(x1, y2 + yShift1, z1).color(tr, tg, tb, alpha).endVertex();

      // Bottom face (y1)
      buffer.pos(x1, y1 + yShift1, z1).color(br, bg, bb, alpha).endVertex();
      buffer.pos(x2, y1 + yShift1, z1).color(br, bg, bb, alpha).endVertex();
      buffer.pos(x2, y1 + yShift2, z2).color(br, bg, bb, alpha).endVertex();
      buffer.pos(x1, y1 + yShift2, z2).color(br, bg, bb, alpha).endVertex();
    }
  }

  /**
   * Adds cuboid vertices with dual coloring based on face orientation relative to visor center.
   */
  private static void addCuboidVerticesDualColor(BufferBuilder buffer,
      double x1, double y1, double z1, double x2, double y2, double z2,
      float outerR, float outerG, float outerB,
      float innerR, float innerG, float innerB, float alpha,
      float midX, float midY, float centerX, float centerY) {
    boolean leftInside = midX > centerX;
    boolean rightInside = midX < centerX;
    boolean bottomInside = midY > centerY;
    boolean topInside = midY < centerY;

    float lr, lg, lb, rr, rg, rb, br, bg, bb, tr, tg, tb;
    lr = leftInside ? innerR : outerR; lg = leftInside ? innerG : outerG; lb = leftInside ? innerB : outerB;
    rr = rightInside ? innerR : outerR; rg = rightInside ? innerG : outerG; rb = rightInside ? innerB : outerB;
    br = bottomInside ? innerR : outerR; bg = bottomInside ? innerG : outerG; bb = bottomInside ? innerB : outerB;
    tr = topInside ? innerR : outerR; tg = topInside ? innerG : outerG; tb = topInside ? innerB : outerB;

    // Front — outer
    buffer.pos(x1, y1, z2).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x2, y1, z2).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x1, y2, z2).color(outerR, outerG, outerB, alpha).endVertex();

    // Back — outer
    buffer.pos(x2, y1, z1).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x1, y1, z1).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(outerR, outerG, outerB, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(outerR, outerG, outerB, alpha).endVertex();

    // Left
    buffer.pos(x1, y1, z1).color(lr, lg, lb, alpha).endVertex();
    buffer.pos(x1, y1, z2).color(lr, lg, lb, alpha).endVertex();
    buffer.pos(x1, y2, z2).color(lr, lg, lb, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(lr, lg, lb, alpha).endVertex();

    // Right
    buffer.pos(x2, y1, z2).color(rr, rg, rb, alpha).endVertex();
    buffer.pos(x2, y1, z1).color(rr, rg, rb, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(rr, rg, rb, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(rr, rg, rb, alpha).endVertex();

    // Top
    buffer.pos(x1, y2, z2).color(tr, tg, tb, alpha).endVertex();
    buffer.pos(x2, y2, z2).color(tr, tg, tb, alpha).endVertex();
    buffer.pos(x2, y2, z1).color(tr, tg, tb, alpha).endVertex();
    buffer.pos(x1, y2, z1).color(tr, tg, tb, alpha).endVertex();

    // Bottom
    buffer.pos(x1, y1, z1).color(br, bg, bb, alpha).endVertex();
    buffer.pos(x2, y1, z1).color(br, bg, bb, alpha).endVertex();
    buffer.pos(x2, y1, z2).color(br, bg, bb, alpha).endVertex();
    buffer.pos(x1, y1, z2).color(br, bg, bb, alpha).endVertex();
  }

  public static class Box {
    public final float[] from;
    public final float[] to;

    public Box(float[] from, float[] to) {
      this.from = from;
      this.to = to;
    }
  }

  public static class TextureInfo {

    private final ResourceLocation texture;
    private final float uStart, vStart, uEnd, vEnd;

    public TextureInfo(ResourceLocation texture, float uStart, float vStart, float uEnd,
        float vEnd) {
      this.texture = texture;
      this.uStart = uStart;
      this.vStart = vStart;
      this.uEnd = uEnd;
      this.vEnd = vEnd;
    }

    public float getTextureUStart() {
      return uStart / 16.0f;
    }

    public float getTextureVStart() {
      return vStart / 16.0f;
    }

    public float getTextureUEnd() {
      return uEnd / 16.0f;
    }

    public float getTextureVEnd() {
      return vEnd / 16.0f;
    }

    public ResourceLocation getTexture() {
      return texture;
    }
  }
}
