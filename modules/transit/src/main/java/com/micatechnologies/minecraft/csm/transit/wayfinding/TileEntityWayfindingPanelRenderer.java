package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.transit.panel.PanelRods;
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
 * <p><b>Layout</b> ({@link WayfindingPanelLayout}). The pictogram is a square the panel's height
 * less its margins, at the end away from the arrow; the arrow, 0.8 of that, at the end it points
 * to (the left end for the three leftward arrows, else the right); the text left-aligned between
 * them, its capitals 0.45 of the panel's height on one line or 0.28 each on two, scaled down
 * together to fit the width. Worked out once per sign and panel size and kept on the tile
 * entity.</p>
 *
 * <p><b>Drawing.</b> The face is fullbright, as a backlit sign is: the baked slab behind it is
 * lit by the world and is what shows beyond the render distance. The legend's glyphs are a
 * display list per controller, keyed by the text (and any line's text scale), in the font's
 * units, and placed and scaled outside it; following "Display lists: one texture, no cached
 * state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the list holds geometry only, and the font atlas,
 * colour and lightmap are set outside it every frame. The background is one untextured quad, and the
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
  static final float FRAME = WayfindingPanelLayout.FRAME;
  /** How far in front of the slab's face the background and the art are drawn, in blocks. */
  private static final float BACKGROUND_LIFT = 0.1f / 16f;
  private static final float ART_LIFT = 0.2f / 16f;

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
    WayfindingPanelLayout layout = layout(te, fr);
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

    PanelRods.draw(te.getWorld(), te.getPos(), te.getLayout(), 0f);

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

  /** One face of the sign, in the reader's space with the slab's face at z = 0. */
  private void face(TileEntityWayfindingPanel te, WayfindingPanelLayout layout, Arrow arrow,
      int width, int height, CsmFontRenderer fr) {
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
  private static void text(TileEntityWayfindingPanel te, WayfindingPanelLayout layout,
      CsmFontRenderer fr) {
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

  /**
   * The lines in the font's units, the first at the origin, y down. Geometry only: a line with a
   * text scale of its own is placed by a raw GL matrix push, translate and scale, balanced inside
   * the list, which no {@code GlStateManager} cache knows about or needs to.
   */
  private static void glyphs(WayfindingPanelLayout layout, CsmFontRenderer fr) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    for (int i = 0; i < layout.lines.length; i++) {
      if (layout.lineK[i] == 1f) {
        fr.addString(buf, layout.lines[i], 0, i * layout.lineStep);
      }
    }
    tess.draw();
    if (layout.isUnscaled()) {
      return;
    }
    for (int i = 0; i < layout.lines.length; i++) {
      float k = layout.lineK[i];
      if (k != 1f) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, layout.lineY[i], 0);
        GL11.glScalef(k, k, 1f);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        fr.addString(buf, layout.lines[i], 0, 0);
        tess.draw();
        GL11.glPopMatrix();
      }
    }
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

  private static WayfindingPanelLayout layout(TileEntityWayfindingPanel te, CsmFontRenderer fr) {
    String line1 = te.getLine1();
    String line2 = te.getLine2();
    float scale1 = te.getLineTextScale(1);
    float scale2 = te.getLineTextScale(2);
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
    key = key * 1000003L ^ Float.floatToIntBits(scale1);
    key = key * 1000003L ^ Float.floatToIntBits(scale2);
    if (te.renderLayout instanceof WayfindingPanelLayout && te.renderLayoutKey == key) {
      return (WayfindingPanelLayout) te.renderLayout;
    }
    WayfindingPanelLayout l = WayfindingPanelLayout.of(width, height, pictogram, arrow, line1,
        line2, scale1, scale2, fr.FONT_HEIGHT, fr::getStringWidth, textKey);
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
