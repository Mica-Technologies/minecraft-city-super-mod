package com.micatechnologies.minecraft.csm.novelties;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * The cabinet's picture tube. Games draw in their own fixed playfield coordinate space (for example
 * 224x288 for the upright cabinets) and this class maps that onto whatever rectangle of the real
 * screen the GUI gave it, so a game never has to know the player's resolution or GUI scale.
 *
 * <p>Everything a frame draws is accumulated into two batches — filled rectangles and vector lines
 * — and flushed in two draw calls. Both the sprite-style cabinets and the wireframe ones are cheap
 * to draw this way; the alternative of one {@code drawRect} per shape would issue thousands of
 * tessellator draws per frame.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeScreen {

  /**
   * Number of floats stored per queued rectangle: x, y, width, height.
   *
   * @since 1.0
   */
  private static final int FLOATS_PER_RECT = 4;

  /**
   * Number of floats stored per queued line: x1, y1, x2, y2.
   *
   * @since 1.0
   */
  private static final int FLOATS_PER_LINE = 4;

  /**
   * A string queued for drawing, already resolved to screen coordinates.
   *
   * @since 1.0
   */
  private static final class QueuedText {

    /**
     * The text to draw.
     */
    private final String text;

    /**
     * The screen X coordinate to draw at.
     */
    private final float x;

    /**
     * The screen Y coordinate to draw at.
     */
    private final float y;

    /**
     * The ARGB colour to draw in.
     */
    private final int color;

    /**
     * The scale factor to draw at.
     */
    private final float scale;

    /**
     * Constructs a queued text entry.
     *
     * @param text  the text
     * @param x     the screen X coordinate
     * @param y     the screen Y coordinate
     * @param color the ARGB colour
     * @param scale the scale factor
     */
    private QueuedText(String text, float x, float y, int color, float scale) {
      this.text = text;
      this.x = x;
      this.y = y;
      this.color = color;
      this.scale = scale;
    }
  }

  /**
   * The font renderer used for on-screen text.
   *
   * @since 1.0
   */
  private final FontRenderer fontRenderer;

  /**
   * Queued rectangle geometry, in playfield coordinates.
   *
   * @since 1.0
   */
  private float[] rectGeometry = new float[512];

  /**
   * Queued rectangle colours, one per rectangle.
   *
   * @since 1.0
   */
  private int[] rectColors = new int[128];

  /**
   * The number of rectangles queued.
   *
   * @since 1.0
   */
  private int rectCount;

  /**
   * Queued line geometry, in playfield coordinates.
   *
   * @since 1.0
   */
  private float[] lineGeometry = new float[512];

  /**
   * Queued line colours, one per line.
   *
   * @since 1.0
   */
  private int[] lineColors = new int[128];

  /**
   * The number of lines queued.
   *
   * @since 1.0
   */
  private int lineCount;

  /**
   * Queued text entries.
   *
   * @since 1.0
   */
  private final List<QueuedText> texts = new ArrayList<>();

  /**
   * The screen X coordinate of the playfield's origin.
   *
   * @since 1.0
   */
  private float originX;

  /**
   * The screen Y coordinate of the playfield's origin.
   *
   * @since 1.0
   */
  private float originY;

  /**
   * The playfield-to-screen scale factor.
   *
   * @since 1.0
   */
  private float scale = 1F;

  /**
   * The width of vector lines, in screen pixels.
   *
   * @since 1.0
   */
  private float lineWidth = 1F;

  /**
   * Constructs an {@link ArcadeScreen} drawing with the specified font renderer.
   *
   * @param fontRenderer the font renderer to use for text
   *
   * @since 1.0
   */
  public ArcadeScreen(FontRenderer fontRenderer) {
    this.fontRenderer = fontRenderer;
  }

  /**
   * Positions the playfield on the real screen. Called by the GUI every frame before the game
   * draws.
   *
   * @param originX the screen X coordinate of playfield (0, 0)
   * @param originY the screen Y coordinate of playfield (0, 0)
   * @param scale   the playfield-to-screen scale factor
   *
   * @since 1.0
   */
  public void setViewport(float originX, float originY, float scale) {
    this.originX = originX;
    this.originY = originY;
    this.scale = scale;
    this.lineWidth = Math.max(1F, scale * 0.9F);
  }

  /**
   * Retrieves the playfield-to-screen scale factor.
   *
   * @return the scale factor
   *
   * @since 1.0
   */
  public float getScale() {
    return scale;
  }

  /**
   * Converts a playfield X coordinate to a screen X coordinate.
   *
   * @param x the playfield X coordinate
   *
   * @return the screen X coordinate
   *
   * @since 1.0
   */
  public float toScreenX(float x) {
    return originX + x * scale;
  }

  /**
   * Converts a playfield Y coordinate to a screen Y coordinate.
   *
   * @param y the playfield Y coordinate
   *
   * @return the screen Y coordinate
   *
   * @since 1.0
   */
  public float toScreenY(float y) {
    return originY + y * scale;
  }

  /**
   * Converts a screen X coordinate back to a playfield X coordinate.
   *
   * @param screenX the screen X coordinate
   *
   * @return the playfield X coordinate
   *
   * @since 1.0
   */
  public float toPlayfieldX(float screenX) {
    return (screenX - originX) / scale;
  }

  /**
   * Converts a screen Y coordinate back to a playfield Y coordinate.
   *
   * @param screenY the screen Y coordinate
   *
   * @return the playfield Y coordinate
   *
   * @since 1.0
   */
  public float toPlayfieldY(float screenY) {
    return (screenY - originY) / scale;
  }

  /**
   * Discards everything queued, readying the screen for a new frame.
   *
   * @since 1.0
   */
  public void beginFrame() {
    rectCount = 0;
    lineCount = 0;
    texts.clear();
  }

  /**
   * Queues a filled rectangle.
   *
   * @param x      the playfield X coordinate of the left edge
   * @param y      the playfield Y coordinate of the top edge
   * @param width  the width, in playfield units
   * @param height the height, in playfield units
   * @param color  the ARGB colour
   *
   * @since 1.0
   */
  public void rect(float x, float y, float width, float height, int color) {
    if (width <= 0F || height <= 0F) {
      return;
    }
    if ((rectCount + 1) * FLOATS_PER_RECT > rectGeometry.length) {
      rectGeometry = java.util.Arrays.copyOf(rectGeometry, rectGeometry.length * 2);
      rectColors = java.util.Arrays.copyOf(rectColors, rectColors.length * 2);
    }
    int base = rectCount * FLOATS_PER_RECT;
    rectGeometry[base] = x;
    rectGeometry[base + 1] = y;
    rectGeometry[base + 2] = width;
    rectGeometry[base + 3] = height;
    rectColors[rectCount] = color;
    rectCount++;
  }

  /**
   * Queues a filled rectangle centred on the specified point.
   *
   * @param centerX the playfield X coordinate of the centre
   * @param centerY the playfield Y coordinate of the centre
   * @param radius  half the width and height, in playfield units
   * @param color   the ARGB colour
   *
   * @since 1.0
   */
  public void dot(float centerX, float centerY, float radius, int color) {
    rect(centerX - radius, centerY - radius, radius * 2F, radius * 2F, color);
  }

  /**
   * Queues a vector line.
   *
   * @param x1    the playfield X coordinate of the start point
   * @param y1    the playfield Y coordinate of the start point
   * @param x2    the playfield X coordinate of the end point
   * @param y2    the playfield Y coordinate of the end point
   * @param color the ARGB colour
   *
   * @since 1.0
   */
  public void line(float x1, float y1, float x2, float y2, int color) {
    if ((lineCount + 1) * FLOATS_PER_LINE > lineGeometry.length) {
      lineGeometry = java.util.Arrays.copyOf(lineGeometry, lineGeometry.length * 2);
      lineColors = java.util.Arrays.copyOf(lineColors, lineColors.length * 2);
    }
    int base = lineCount * FLOATS_PER_LINE;
    lineGeometry[base] = x1;
    lineGeometry[base + 1] = y1;
    lineGeometry[base + 2] = x2;
    lineGeometry[base + 3] = y2;
    lineColors[lineCount] = color;
    lineCount++;
  }

  /**
   * Queues a polygon outline built from the supplied vertices.
   *
   * @param xs     the vertex X coordinates
   * @param ys     the vertex Y coordinates
   * @param count  the number of vertices to use
   * @param closed whether to draw a closing edge from the last vertex back to the first
   * @param color  the ARGB colour
   *
   * @since 1.0
   */
  public void polyline(float[] xs, float[] ys, int count, boolean closed, int color) {
    for (int i = 0; i < count - 1; i++) {
      line(xs[i], ys[i], xs[i + 1], ys[i + 1], color);
    }
    if (closed && count > 2) {
      line(xs[count - 1], ys[count - 1], xs[0], ys[0], color);
    }
  }

  /**
   * Queues a circle outline approximated by line segments.
   *
   * @param centerX  the playfield X coordinate of the centre
   * @param centerY  the playfield Y coordinate of the centre
   * @param radius   the radius, in playfield units
   * @param segments the number of segments to approximate with
   * @param color    the ARGB colour
   *
   * @since 1.0
   */
  public void circle(float centerX, float centerY, float radius, int segments, int color) {
    float previousX = centerX + radius;
    float previousY = centerY;
    for (int i = 1; i <= segments; i++) {
      double angle = Math.PI * 2.0 * i / segments;
      float nextX = centerX + radius * (float) Math.cos(angle);
      float nextY = centerY + radius * (float) Math.sin(angle);
      line(previousX, previousY, nextX, nextY, color);
      previousX = nextX;
      previousY = nextY;
    }
  }

  /**
   * Queues a filled disc approximated by horizontal spans. Used for explosion fireballs, which want
   * to be solid rather than outlined.
   *
   * @param centerX the playfield X coordinate of the centre
   * @param centerY the playfield Y coordinate of the centre
   * @param radius  the radius, in playfield units
   * @param color   the ARGB colour
   *
   * @since 1.0
   */
  public void disc(float centerX, float centerY, float radius, int color) {
    int steps = Math.max(3, (int) radius);
    for (int i = -steps; i <= steps; i++) {
      float rowY = radius * i / steps;
      float halfWidth = (float) Math.sqrt(Math.max(0F, radius * radius - rowY * rowY));
      float rowHeight = radius / steps + 0.6F;
      rect(centerX - halfWidth, centerY + rowY - rowHeight * 0.5F, halfWidth * 2F, rowHeight,
          color);
    }
  }

  /**
   * Queues a string, left-aligned at the specified playfield position.
   *
   * @param text  the text to draw
   * @param x     the playfield X coordinate of the left edge
   * @param y     the playfield Y coordinate of the top edge
   * @param color the ARGB colour
   *
   * @since 1.0
   */
  public void text(String text, float x, float y, int color) {
    textScaled(text, x, y, color, 1F);
  }

  /**
   * Queues a string, centred horizontally on the specified playfield position.
   *
   * @param text  the text to draw
   * @param x     the playfield X coordinate of the centre
   * @param y     the playfield Y coordinate of the top edge
   * @param color the ARGB colour
   *
   * @since 1.0
   */
  public void textCentered(String text, float x, float y, int color) {
    textCenteredScaled(text, x, y, color, 1F);
  }

  /**
   * Queues a string at a custom scale, left-aligned at the specified playfield position.
   *
   * @param text      the text to draw
   * @param x         the playfield X coordinate of the left edge
   * @param y         the playfield Y coordinate of the top edge
   * @param color     the ARGB colour
   * @param textScale the additional scale factor to apply to the font
   *
   * @since 1.0
   */
  public void textScaled(String text, float x, float y, int color, float textScale) {
    texts.add(new QueuedText(text, toScreenX(x), toScreenY(y), color, textScale));
  }

  /**
   * Queues a string at a custom scale, centred horizontally on the specified playfield position.
   *
   * @param text      the text to draw
   * @param x         the playfield X coordinate of the centre
   * @param y         the playfield Y coordinate of the top edge
   * @param color     the ARGB colour
   * @param textScale the additional scale factor to apply to the font
   *
   * @since 1.0
   */
  public void textCenteredScaled(String text, float x, float y, int color, float textScale) {
    float halfWidth = fontRenderer.getStringWidth(text) * 0.5F * textScale;
    texts.add(new QueuedText(text, toScreenX(x) - halfWidth, toScreenY(y), color, textScale));
  }

  /**
   * Draws everything queued this frame: rectangles first, then vector lines over them, then text on
   * top. Leaves the GL state as it was found.
   *
   * @since 1.0
   */
  public void flush() {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
    GlStateManager.disableAlpha();

    if (rectCount > 0) {
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      for (int i = 0; i < rectCount; i++) {
        int base = i * FLOATS_PER_RECT;
        float left = toScreenX(rectGeometry[base]);
        float top = toScreenY(rectGeometry[base + 1]);
        float right = left + rectGeometry[base + 2] * scale;
        float bottom = top + rectGeometry[base + 3] * scale;
        int color = rectColors[i];
        int alpha = color >>> 24;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        buffer.pos(left, bottom, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, bottom, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, top, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(left, top, 0.0D).color(red, green, blue, alpha).endVertex();
      }
      tessellator.draw();
    }

    if (lineCount > 0) {
      GlStateManager.glLineWidth(lineWidth);
      buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
      for (int i = 0; i < lineCount; i++) {
        int base = i * FLOATS_PER_LINE;
        int color = lineColors[i];
        int alpha = color >>> 24;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        buffer.pos(toScreenX(lineGeometry[base]), toScreenY(lineGeometry[base + 1]), 0.0D)
            .color(red, green, blue, alpha).endVertex();
        buffer.pos(toScreenX(lineGeometry[base + 2]), toScreenY(lineGeometry[base + 3]), 0.0D)
            .color(red, green, blue, alpha).endVertex();
      }
      tessellator.draw();
      GlStateManager.glLineWidth(1.0F);
    }

    GlStateManager.enableAlpha();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    for (QueuedText queued : texts) {
      if (queued.scale == 1F) {
        fontRenderer.drawString(queued.text, (int) queued.x, (int) queued.y, queued.color);
      } else {
        GlStateManager.pushMatrix();
        GlStateManager.translate(queued.x, queued.y, 0F);
        GlStateManager.scale(queued.scale, queued.scale, 1F);
        fontRenderer.drawString(queued.text, 0, 0, queued.color);
        GlStateManager.popMatrix();
      }
    }

    GlStateManager.disableBlend();
  }
}
