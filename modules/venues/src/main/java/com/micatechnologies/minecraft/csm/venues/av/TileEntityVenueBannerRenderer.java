package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a banner's three lines on both faces of its cloth: the top line, the big one (a year or
 * a number) and the bottom one, each shrunk to fit the cloth's width. One display list per
 * banner holds the glyphs (one {@code POSITION_TEX_COLOR} draw, compiled when the words or the
 * lettering's colour change) and is called once a face. The lettering is lit as the cloth is, at
 * the banner's own light, since a banner is printed, not lit.
 *
 * <p>The cloth's numbers are {@code gen_venues_av.py}'s ({@code banner_model}): change one,
 * change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityVenueBannerRenderer
    extends TileEntitySpecialRenderer<TileEntityVenueBanner> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_banner");

  /** Cloth: left and right edge, top, and its faces, pixels of the master's cell. */
  private static final float LEFT = 1f;
  private static final float RIGHT_LARGE = 31f;
  private static final float RIGHT_SMALL = 15f;
  private static final float TOP = 30f;
  private static final float FRONT_Z = 7.75f;
  private static final float BACK_Z = 8.25f;
  private static final float LIFT = 0.05f;

  /** Each line: top (pixels down from the cloth's top) and height; large, then small. */
  private static final float[][] LARGE = {{5f, 4.5f}, {12f, 15f}, {30.5f, 4f}};
  private static final float[][] SMALL = {{4.5f, 2.8f}, {8.5f, 9f}, {19.5f, 2.6f}};

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityVenueBanner te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueBanners) {
      return;
    }
    BlockVenueBanner block = te.block();
    if (block == null) {
      return;
    }
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    boolean large = block.isLarge();
    float right = large ? RIGHT_LARGE : RIGHT_SMALL;
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.getFacing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();

    // the north face: its left edge as seen is the cloth's east edge
    GlStateManager.pushMatrix();
    GlStateManager.translate(right / 16.0, TOP / 16.0, (FRONT_Z - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
    lines(te, fr, large, right - LEFT);
    GlStateManager.popMatrix();
    // the south face
    GlStateManager.pushMatrix();
    GlStateManager.translate(LEFT / 16.0, TOP / 16.0, (BACK_Z + LIFT) / 16.0);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
    lines(te, fr, large, right - LEFT);
    GlStateManager.popMatrix();

    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  private static void lines(TileEntityVenueBanner te, CsmFontRenderer fr, boolean large,
      float width) {
    int list = LISTS.get(te.getPos(), 0L);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(te.getPos(), 0L);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(te, fr, large, width);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, large, width);
    }
  }

  private static void glyphs(TileEntityVenueBanner te, CsmFontRenderer fr, boolean large,
      float width) {
    float[][] rows = large ? LARGE : SMALL;
    float margin = large ? 2f : 1f;
    int ink = VenueColours.rgb(te.getInk());
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int i = 0; i < 3; i++) {
      TileEntityScoreboardRenderer.line(buf, fr, te.getLine(i).toUpperCase(java.util.Locale.ROOT),
          width / 2f, rows[i][0], rows[i][1], width - 2 * margin, ink, 0);
    }
    tess.draw();
  }
}
