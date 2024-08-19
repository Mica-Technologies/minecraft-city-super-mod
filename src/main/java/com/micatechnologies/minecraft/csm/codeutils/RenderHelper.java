package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Utility class for rendering cubes and other shapes in TESR-based rendering. Supports solid color
 * rendering, per-face textured rendering, and additional geometric rendering methods.
 */
public class RenderHelper {

  public static void drawModelColored(IBakedModel model, float red, float green, float blue) {
    // Get tessellator and buffer
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Push matrix and attribs
    GL11.glPushMatrix();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

    // Disable textures and set baseline color
    GlStateManager.disableTexture2D();
    GlStateManager.color(red, green, blue, 1.0f);

    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    addModelToBuffer(buffer, model);
    tessellator.draw();

    // Restore textures and reset color
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableTexture2D();

    // Pop matrix and attribs
    GL11.glPopAttrib();
    GL11.glPopMatrix();
  }

  private static void addModelToBuffer(
      BufferBuilder builder, IBakedModel model) {
    for (BakedQuad quad : model.getQuads(null, EnumFacing.NORTH, 0)) {
      builder.addVertexData(quad.getVertexData());
    }
  }

  public static void drawCuboidColored(double x, double y, double z, double width, double height,
      double depth, float red, float green, float blue) {
    // Get tessellator and buffer
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Push matrix and attribs
    GL11.glPushMatrix();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

    // Disable textures and set baseline color
    GlStateManager.disableTexture2D();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    // Draw cuboid (up, down, north, south, east, west)
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

    // Up
    buffer.pos(x + width, y + height, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z).color(red, green, blue, 1.0f).endVertex();

    // Down
    buffer.pos(x, y, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y, z).color(red, green, blue, 1.0f).endVertex();

    // North
    buffer.pos(x + width, y, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y + height, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y, z).color(red, green, blue, 1.0f).endVertex();

    // South
    buffer.pos(x, y, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y, z + depth).color(red, green, blue, 1.0f).endVertex();

    // East
    buffer.pos(x + width, y, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y + height, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x + width, y, z).color(red, green, blue, 1.0f).endVertex();

    // West
    buffer.pos(x, y, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y + height, z + depth).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x, y, z + depth).color(red, green, blue, 1.0f).endVertex();

    tessellator.draw();

    // Restore textures and reset color
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableTexture2D();

    // Pop matrix and attribs
    GL11.glPopAttrib();
    GL11.glPopMatrix();
  }

  public static void drawLineColored(double x1, double y1, double x2, double y2, float red,
      float green, float blue, float lineWidth) {
    drawLineColored(x1, y1, 0, x2, y2, 0, red, green, blue, lineWidth);
  }

  public static void drawLineColored(double x1, double y1, double z1, double x2, double y2,
      double z2, float red,
      float green, float blue, float lineWidth) {
    // Get tessellator and buffer
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Push matrix and attribs
    GL11.glPushMatrix();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

    // Disable textures and set baseline color
    GlStateManager.disableTexture2D();
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

    // Set line width
    GL11.glLineWidth(lineWidth);

    // Draw line
    buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    buffer.pos(x1, y1, z1).color(red, green, blue, 1.0f).endVertex();
    buffer.pos(x2, y2, z2).color(red, green, blue, 1.0f).endVertex();
    tessellator.draw();

    // Restore textures and reset color
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableTexture2D();

    // Pop matrix and attribs
    GL11.glPopAttrib();
    GL11.glPopMatrix();
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
