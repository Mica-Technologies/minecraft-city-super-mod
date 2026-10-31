package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
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
 * Draws an outfield distance marker's number on the wall's pad: about 1.4 blocks tall, centred
 * across the marker block's top edge, in the venue font, lit as the wall is (it is paint). On a
 * straight wall it is on the pad's face at the block's front; on a 45 degree wall along the
 * diagonal face. One display list a marker holds the glyphs, compiled when the number or its
 * colour changes.
 *
 * <p>The pad's face is {@code gen_venues_ballpark.py}'s: z 0 on a straight block, the line from
 * the north-west corner to the south-east one on a diagonal one (facing north).</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityOutfieldMarkerRenderer
    extends TileEntitySpecialRenderer<TileEntityOutfieldMarker> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_outfield_marker");

  /** The number's height and widest, in pixels. */
  private static final float HEIGHT = 22f;
  private static final float MAX_W = 44f;
  /** How far in front of the pad the number is drawn, in pixels. */
  private static final float LIFT = 0.06f;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityOutfieldMarker te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueBallpark
        || te.getText().isEmpty()) {
      return;
    }
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.facing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    if (te.diagonal()) {
      // the face runs from the north-west corner to the south-east one, looking north-east
      float d = LIFT / (float) Math.sqrt(2.0);
      GlStateManager.translate((8f + d) / 16.0, 1.0, (8f - d) / 16.0);
      GlStateManager.rotate(135F, 0F, 1F, 0F);
    } else {
      GlStateManager.translate(0.5, 1.0, -LIFT / 16.0);
      GlStateManager.rotate(180F, 0F, 1F, 0F);
    }
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
    int list = LISTS.get(te.getPos(), 0L);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(te.getPos(), 0L);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(te, fr);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr);
    }
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  private static void glyphs(TileEntityOutfieldMarker te, CsmFontRenderer fr) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    TileEntityScoreboardRenderer.line(buf, fr, te.getText().toUpperCase(java.util.Locale.ROOT),
        0f, -HEIGHT / 2f, HEIGHT, MAX_W, VenueColours.rgb(te.getInk()), 0);
    tess.draw();
  }
}
