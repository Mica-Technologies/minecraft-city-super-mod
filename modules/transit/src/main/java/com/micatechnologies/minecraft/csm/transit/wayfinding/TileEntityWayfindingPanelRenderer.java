package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Pictogram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a large hanging sign ({@link BlockWayfindingPanel}) from its controller, once across the
 * whole panel: the hanger rods up to the ceiling, then on the face the background in the scheme's
 * colour, the pictogram on its square, the arrow and the legend, and the same on the back with
 * the arrow mirrored when the sign is double-sided. Other cells draw nothing here.
 *
 * <p><b>Space.</b> Everything is laid out in the reader's space: x along the panel to the
 * reader's right from its left edge, y up from its bottom, in blocks, with the face at
 * {@link #FACE_Z} toward the reader. The back is the same layout turned half round about the
 * panel's middle, so its legend reads the right way and its arrow (mirrored) points the same way
 * in the world as the front's.</p>
 *
 * <p><b>Layout.</b> The pictogram is a square the panel's height less its margins, at the end
 * away from the arrow; the arrow, {@link #ARROW_SHARE} of that, at the end it points to (the left end for the three
 * leftward arrows, else the right); the text left-aligned between them, its capitals
 * {@link #CAPS_ONE_LINE} of the panel's height on one line or {@link #CAPS_TWO_LINES} each on two,
 * scaled down together to fit the width. Worked out once per
 * sign and panel size and kept on the tile entity.</p>
 *
 * <p><b>Drawing.</b> The face is fullbright, as a backlit sign is: the baked slab behind it is
 * lit by the world and is what shows beyond the render distance. The legend's glyphs are a
 * display list per controller, keyed by the text, in the font's units, and placed and scaled
 * outside it; following "Display lists: one texture, no cached state" in
 * {@code TRAFFIC_SIGNAL_SYSTEM.md}, the list holds geometry only, and the font atlas, colour and
 * lightmap are set outside it every frame. The background is one untextured quad, and the
 * pictogram and arrow one block-atlas quad each, drawn directly.</p>
 *
 * <p>The sprites are {@code gen_transit_airport.py}'s {@code wayfinding_panel()}; no model face
 * draws them, so {@link Sprites} puts them on the atlas.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityWayfindingPanelRenderer
    extends TileEntitySpecialRenderer<TileEntityWayfindingPanel> {

  /** The slab's face, from the block's middle plane (the model's z 6 and 10), in blocks. */
  static final float FACE_Z = 2f / 16f;
  /** The frame's width on the panel's outer edges (the generator's PANEL_FRAME), in blocks. */
  static final float FRAME = 0.8f / 16f;
  /** How far in front of the slab's face the background and the art are drawn, in blocks. */
  private static final float BACKGROUND_LIFT = 0.1f / 16f;
  private static final float ART_LIFT = 0.2f / 16f;
  /** Half a hanger rod's width, in blocks. */
  private static final float ROD = 0.55f / 16f;

  /**
   * The Highway Gothic capitals' height as a share of the font's line height, measured in game:
   * the line height leaves room for accents and descenders, so the legend is sized by its
   * capitals, which sit about centred in the line.
   */
  private static final float CAP_SHARE = 0.45f;
  /** The capitals' height on a one-line sign and on each line of a two-line one, of the panel. */
  private static final float CAPS_ONE_LINE = 0.45f;
  private static final float CAPS_TWO_LINES = 0.28f;
  /** The arrow's square as a share of the pictogram's: an arrow reads well smaller. */
  private static final float ARROW_SHARE = 0.8f;
  /**
   * How small the pictogram and arrow may go, as a share of the panel's height inside its
   * margins, to give a long legend room before the legend itself is shrunk to fit.
   */
  private static final float MIN_ART_SHARE = 0.55f;
  /** From one line to the next, in line heights. */
  private static final float LINE_STEP = 0.85f;

  private static final String ARROW_SPRITE = "csm:blocks/transit/airport/wayfinding_arrow";

  private static final CsmDisplayListCache TEXT_LISTS =
      new CsmDisplayListCache("wayfinding_panel_text");

  /** Releases a controller's text list; its tile entity calls this when unloaded or removed. */
  public static void release(BlockPos pos) {
    TEXT_LISTS.invalidate(pos);
  }

  /**
   * Drawn whatever chunk section the controller is in is doing: a panel sixteen blocks wide and
   * its rods reach well past the controller's own section, which may be culled while they show.
   */
  @Override
  public boolean isGlobalRenderer(TileEntityWayfindingPanel te) {
    return true;
  }

  @Override
  public void render(TileEntityWayfindingPanel te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshLayout();
    if (!te.isController()) {
      return;
    }
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    Layout layout = layout(te, fr);
    int width = te.getWidth();
    int height = te.getHeight();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so the model's front (north) faces +z: the model's point (mx, my, mz) is
    // (8 - mx, my, 8 - mz) / 16 here and the reader's right is +x; then moved to the panel's
    // left edge as read (the model's x = 16).
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, 0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

    rods(te, height);

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    GlStateManager.pushMatrix();
    GlStateManager.translate(0, 0, FACE_Z);
    face(te, layout, te.getArrow(), width, height, fr);
    GlStateManager.popMatrix();

    if (te.isDoubleSided()) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(width, 0, -FACE_Z);
      GlStateManager.rotate(180F, 0F, 1F, 0F);
      face(te, layout, te.getArrow().mirrored(), width, height, fr);
      GlStateManager.popMatrix();
    }

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** The hanger rods, steel, lit by the world at the panel's top. */
  private void rods(TileEntityWayfindingPanel te, int height) {
    float[] rodX = te.getRodX();
    int[] drop = te.getRodDrop();
    boolean any = false;
    for (int d : drop) {
      any |= d > 0;
    }
    if (!any) {
      return;
    }
    int light = te.getWorld().getCombinedLight(te.getPos().up(height), 0);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >>> 16);
    TextureAtlasSprite steel = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite("csm:blocks/transit/airport/steel");
    float u0 = steel.getMinU();
    float u1 = steel.getInterpolatedU(2);
    float v0 = steel.getMinV();
    float v1 = steel.getMaxV();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int i = 0; i < rodX.length && i < drop.length; i++) {
      if (drop[i] <= 0) {
        continue;
      }
      float x0 = rodX[i] - ROD;
      float x1 = rodX[i] + ROD;
      float y0 = height;
      float y1 = height + drop[i];
      // four sides, shaded by hand as the world's directional light would
      side(buf, x0, -ROD, x1, -ROD, y0, y1, u0, u1, v0, v1, 0.8f);
      side(buf, x1, ROD, x0, ROD, y0, y1, u0, u1, v0, v1, 0.8f);
      side(buf, x0, ROD, x0, -ROD, y0, y1, u0, u1, v0, v1, 0.6f);
      side(buf, x1, -ROD, x1, ROD, y0, y1, u0, u1, v0, v1, 0.6f);
    }
    tess.draw();
  }

  private static void side(BufferBuilder buf, float xa, float za, float xb, float zb, float y0,
      float y1, float u0, float u1, float v0, float v1, float shade) {
    buf.pos(xa, y0, za).tex(u0, v1).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xb, y0, zb).tex(u1, v1).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xb, y1, zb).tex(u1, v0).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xa, y1, za).tex(u0, v0).color(shade, shade, shade, 1f).endVertex();
  }

  /** One face of the sign, in the reader's space with the slab's face at z = 0. */
  private void face(TileEntityWayfindingPanel te, Layout layout, Arrow arrow, int width,
      int height, CsmFontRenderer fr) {
    WayfindingSign.Scheme scheme = te.getScheme();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // the background, inside the frame
    GlStateManager.disableTexture2D();
    colour(scheme.getBackground());
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    quad(buf, FRAME, FRAME, width - FRAME, height - FRAME, BACKGROUND_LIFT);
    tess.draw();
    GlStateManager.enableTexture2D();

    GlStateManager.depthMask(false);
    boolean left = arrow != Arrow.NONE && arrow.isLeftEnd();
    float pictX = left ? layout.pictXArrowLeft : layout.pictX;
    float arrowX = left ? layout.arrowXLeft : layout.arrowX;
    float textX = left ? layout.textXArrowLeft : layout.textX;

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    Pictogram pictogram = te.getPictogram();
    if (pictogram != Pictogram.NONE) {
      TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
          .getAtlasSprite(pictogram.getSprite());
      colour(scheme.getPictogram());
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
      float half = layout.square / 2f;
      spriteQuad(buf, sprite, pictX, layout.artMiddle - half, pictX + layout.square,
          layout.artMiddle + half, ART_LIFT);
      tess.draw();
    }
    if (arrow != Arrow.NONE) {
      TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
          .getAtlasSprite(ARROW_SPRITE);
      float half = layout.arrow / 2f;
      colour(scheme.getLegend());
      GlStateManager.pushMatrix();
      GlStateManager.translate(arrowX + half, layout.artMiddle, ART_LIFT);
      GlStateManager.rotate(arrow.getAngle(), 0F, 0F, 1F);
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
      spriteQuad(buf, sprite, -half, -half, half, half, 0);
      tess.draw();
      GlStateManager.popMatrix();
    }

    if (layout.textScale > 0) {
      fr.bindAtlas();
      colour(scheme.getLegend());
      GlStateManager.pushMatrix();
      GlStateManager.translate(textX, layout.textTop, ART_LIFT);
      GlStateManager.scale(layout.textScale, -layout.textScale, 1f);
      text(te, layout, fr);
      GlStateManager.popMatrix();
    }
    GlStateManager.depthMask(true);
  }

  /** The legend's glyphs, from the controller's list, compiling it the first time. */
  private static void text(TileEntityWayfindingPanel te, Layout layout, CsmFontRenderer fr) {
    BlockPos pos = te.getPos();
    int list = TEXT_LISTS.get(pos, layout.textKey);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = TEXT_LISTS.allocate(pos, layout.textKey);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(layout, fr);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      glyphs(layout, fr);
    }
  }

  /** The lines in the font's units, the first at the origin, y down. Geometry only. */
  private static void glyphs(Layout layout, CsmFontRenderer fr) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    for (int i = 0; i < layout.lines.length; i++) {
      fr.addString(buf, layout.lines[i], 0, i * layout.lineStep);
    }
    tess.draw();
  }

  private static void quad(BufferBuilder buf, float x0, float y0, float x1, float y1, float z) {
    buf.pos(x0, y0, z).endVertex();
    buf.pos(x1, y0, z).endVertex();
    buf.pos(x1, y1, z).endVertex();
    buf.pos(x0, y1, z).endVertex();
  }

  private static void spriteQuad(BufferBuilder buf, TextureAtlasSprite sprite, float x0, float y0,
      float x1, float y1, float z) {
    buf.pos(x0, y0, z).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
    buf.pos(x1, y0, z).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
    buf.pos(x1, y1, z).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
    buf.pos(x0, y1, z).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
  }

  private static void colour(int rgb) {
    GlStateManager.color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
        (rgb & 0xFF) / 255f, 1.0f);
  }

  // ---------------------------------------------------------------------------------------
  // Layout
  // ---------------------------------------------------------------------------------------

  /** Where everything goes on a face, in blocks; worked out once per sign and size. */
  static final class Layout {

    float margin;
    float square;
    /** The pictogram's and arrow's middle, up the face. */
    float artMiddle;
    float arrow;
    /** With the arrow at the right end, or none. */
    float pictX;
    float arrowX;
    float textX;
    /** With the arrow at the left end. */
    float pictXArrowLeft;
    float arrowXLeft;
    float textXArrowLeft;
    float textTop;
    float textScale;
    String[] lines;
    int lineStep;
    long textKey;
  }

  private static Layout layout(TileEntityWayfindingPanel te, CsmFontRenderer fr) {
    String line1 = te.getLine1();
    String line2 = te.getLine2();
    long textKey = hash(line1, line2);
    int width = te.getWidth();
    int height = te.getHeight();
    boolean pictogram = te.getPictogram() != Pictogram.NONE;
    boolean arrow = te.getArrow() != Arrow.NONE;
    long key = textKey;
    key = key * 1000003L ^ width;
    key = key * 1000003L ^ height;
    key = key * 1000003L ^ (pictogram ? 1 : 0);
    key = key * 1000003L ^ (arrow ? 1 : 0);
    if (te.renderLayout instanceof Layout && te.renderLayoutKey == key) {
      return (Layout) te.renderLayout;
    }
    Layout l = new Layout();
    l.textKey = textKey;
    l.margin = FRAME + 0.08f * height;
    float fullSquare = height - 2 * l.margin;
    float gap = l.margin;

    if (line1.isEmpty() && line2.isEmpty()) {
      l.lines = new String[0];
    } else if (line1.isEmpty() || line2.isEmpty()) {
      l.lines = new String[]{line1.isEmpty() ? line2 : line1};
    } else {
      l.lines = new String[]{line1, line2};
    }
    int n = l.lines.length;
    int fontHeight = fr.FONT_HEIGHT;
    l.lineStep = Math.round(fontHeight * LINE_STEP);
    int widest = 0;
    for (String line : l.lines) {
      widest = Math.max(widest, fr.getStringWidth(line));
    }
    float wantScale = n == 0 ? 0
        : (n == 1 ? CAPS_ONE_LINE : CAPS_TWO_LINES) * height / (CAP_SHARE * fontHeight);

    // A legend too long for the room beside a full-height pictogram and arrow takes room from
    // them first, down to MIN_ART_SHARE, and is shrunk only past that: the words are the sign.
    float perSquare = (pictogram ? 1f : 0f) + (arrow ? ARROW_SHARE : 0f);
    float room = width - 2 * l.margin - (pictogram ? fullSquare + gap : 0)
        - (arrow ? fullSquare * ARROW_SHARE + gap : 0);
    float short_ = widest * wantScale - room;
    l.square = fullSquare;
    if (short_ > 0 && perSquare > 0) {
      l.square = Math.max(fullSquare * MIN_ART_SHARE, fullSquare - short_ / perSquare);
    }
    l.arrow = l.square * ARROW_SHARE;
    l.artMiddle = height / 2f;

    // the arrow at the right end (or none): pictogram, text, arrow
    float xl = l.margin;
    float xr = width - l.margin;
    if (arrow) {
      l.arrowX = xr - l.arrow;
      xr -= l.arrow + gap;
    }
    if (pictogram) {
      l.pictX = xl;
      xl += l.square + gap;
    }
    l.textX = xl;
    float textWidth = xr - xl;
    // the arrow at the left end: arrow, text, pictogram
    xl = l.margin;
    xr = width - l.margin;
    l.arrowXLeft = xl;
    if (arrow) {
      xl += l.arrow + gap;
    }
    if (pictogram) {
      l.pictXArrowLeft = xr - l.square;
    }
    l.textXArrowLeft = xl;

    if (n == 0 || widest == 0 || textWidth <= 0) {
      l.textScale = 0;
    } else {
      float scale = wantScale;
      if (widest * scale > textWidth) {
        scale = textWidth / widest;
      }
      l.textScale = scale;
      float block = ((n - 1) * l.lineStep + fontHeight) * scale;
      l.textTop = height / 2f + block / 2f;
    }
    te.renderLayout = l;
    te.renderLayoutKey = key;
    return l;
  }

  /** A 64-bit hash of the two lines (FNV-1a), for the text list's key. */
  private static long hash(String line1, String line2) {
    long h = 0xcbf29ce484222325L;
    for (int i = 0; i < line1.length(); i++) {
      h = (h ^ line1.charAt(i)) * 0x100000001b3L;
    }
    h = (h ^ 0x0A) * 0x100000001b3L;
    for (int i = 0; i < line2.length(); i++) {
      h = (h ^ line2.charAt(i)) * 0x100000001b3L;
    }
    return h;
  }

  /**
   * Puts the renderer's sprites on the block atlas: no model face draws them, and the game
   * stitches only what a face draws.
   */
  public static final class Sprites {

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
      for (Pictogram p : Pictogram.values()) {
        if (p.getSprite() != null) {
          event.getMap().registerSprite(new ResourceLocation(p.getSprite()));
        }
      }
      event.getMap().registerSprite(new ResourceLocation(ARROW_SPRITE));
    }
  }
}
