package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a tower crane head's slewing unit.
 *
 * <p>Phase 2 of the crane: a placeholder -- the slewing ring, a jib and a counter-jib as plain
 * bars -- that exists to prove the head's placement, its centring on a 2x2 mast, and that the
 * render box and distance keep a long jib on screen. Phase 3 replaces the bars with the three
 * lattice models, compiled per configuration into a display list.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityCraneHeadRenderer extends TileEntitySpecialRenderer<TileEntityCraneHead> {

  private static final String SPRITE = "csm:blocks/constructionsite/crane_chord_";

  @Override
  public void render(TileEntityCraneHead te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    int light = te.getWorld().getCombinedLight(te.getPos(), 0);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536,
        light / 65536F);
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(SPRITE + te.getLivery().getName());

    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.translate(x + 0.5 + te.getCentreX(), y, z + 0.5 + te.getCentreZ());
    GlStateManager.rotate(-te.getSlew(), 0F, 1F, 0F);

    int s = te.getScale();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder b = tessellator.getBuffer();
    b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    // Slewing ring and platform.
    CraneDraw.box(b, sprite, -0.5 * s, 0.0, -0.5 * s, 0.5 * s, 0.75, 0.5 * s);
    // Jib, out along +x.
    CraneDraw.box(b, sprite, 0.0, 1.0, -0.35 * s, te.getJibLength(), 1.0 + 0.7 * s, 0.35 * s);
    // Counter-jib, a third of the jib's length behind.
    CraneDraw.box(b, sprite, -te.getJibLength() / 3.0, 1.0, -0.35 * s, 0.0, 1.0 + 0.5 * s,
        0.35 * s);
    tessellator.draw();

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  @Override
  public boolean isGlobalRenderer(TileEntityCraneHead te) {
    return true;
  }
}
