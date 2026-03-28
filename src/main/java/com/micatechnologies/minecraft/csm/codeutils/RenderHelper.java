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
