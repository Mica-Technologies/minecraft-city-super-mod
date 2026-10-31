package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Paints an end zone's words flat on its turf: centred on the lettering block, the letters'
 * bottoms towards the block's facing (the goal line), lit as the air over the field is (so the
 * stadium lights light them too), drawn with a polygon offset just above the turf. An outline,
 * if set, is the words drawn eight times a little out from the fill, under it.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityEndZoneLetteringRenderer
    extends TileEntitySpecialRenderer<TileEntityEndZoneLettering> {

  @Override
  public void render(TileEntityEndZoneLettering te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueField
        || te.getText().isEmpty()) {
      return;
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    String text = te.getText().toUpperCase(java.util.Locale.ROOT);
    float h = te.getHeight();
    float maxW = te.getWidth();
    float scale = h / fr.FONT_HEIGHT;
    float width = fr.getStringWidth(text) * scale;
    if (width > maxW) {
      h = h * maxW / width;
    }
    float angle = (360.0F - te.facing().getHorizontalAngle()) % 360.0F;
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + 0.012, z + 0.5);
    GlStateManager.rotate(angle, 0F, 1F, 0F);
    GlStateManager.rotate(90F, 1F, 0F, 0F);
    GlStateManager.disableCull();
    GlStateManager.disableLighting();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.enablePolygonOffset();
    GlStateManager.doPolygonOffset(-1.0F, -10.0F);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    float top = -h / 2f;
    if (te.getOutline() < VenueColours.COUNT) {
      int oc = VenueColours.rgb(te.getOutline());
      float o = h * 0.045f;
      for (int i = 0; i < 8; i++) {
        double a = Math.PI / 4 * i;
        TileEntityScoreboardRenderer.line(buf, fr, text, (float) (Math.cos(a) * o),
            top + (float) (Math.sin(a) * o), h, maxW, oc, 0);
      }
    }
    TileEntityScoreboardRenderer.line(buf, fr, text, 0f, top, h, maxW,
        VenueColours.rgb(te.getColour()), 0);
    tess.draw();
    GlStateManager.depthMask(true);
    GlStateManager.doPolygonOffset(0.0F, 0.0F);
    GlStateManager.disablePolygonOffset();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.popMatrix();
  }

  @Override
  public boolean isGlobalRenderer(TileEntityEndZoneLettering te) {
    return true;
  }
}
