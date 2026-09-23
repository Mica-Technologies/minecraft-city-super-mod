package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws a utility box's ID number: yellow digits on a black decal, stuck on the box's front
 * face where its {@link UtilityBoxSpec.Label} says.
 *
 * <p>The digits take the world's light, as a decal does; they are not lit. The light is read from
 * the cell in front of the box rather than the box's own cell, which the box's body shades.
 * {@link CsmFontRenderer#drawString} binds the font atlas and leaves it bound, so the backing is
 * drawn first and binds its own texture.</p>
 *
 * @version 1.0
 */
public class TileEntityUtilityBoxLabelRenderer
    extends TileEntitySpecialRenderer<TileEntityUtilityBoxLabel> {

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int DECAL_YELLOW = 0xF0C020;
  /** The decal's black margin around the digits, in pixels. */
  private static final float MARGIN = 0.35f;
  /** Line pitch as a multiple of the character height. */
  private static final float LINE_PITCH = 1.3f;
  /** The share of the font's line height its capitals and digits fill. */
  private static final float CAP_SHARE = 0.72f;

  @Override
  public void render(TileEntityUtilityBoxLabel te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockUtilityBoxLabelled)) {
      return;
    }
    UtilityBoxSpec.Label label = ((BlockUtilityBoxLabelled) block).getSpec().getLabel();
    if (label == null) {
      return;
    }
    EnumFacing facing = state.getValue(BlockUtilityBox.FACING);
    String[] lines = te.isSet() ? new String[]{te.getLine1(), te.getLine2()}
        : TileEntityUtilityBoxLabel.defaultLines(te.getPos());

    // A renderer draws in world space and knows nothing about the block's render offset, so
    // the settle onto the road below has to be applied here too. See RoadSurfaceHeight.
    double settle = RoadSurfaceHeight.offsetFor(te.getWorld(), te.getPos());
    int combined = te.getWorld().getCombinedLight(te.getPos().offset(facing), 0);
    float previousX = OpenGlHelper.lastBrightnessX;
    float previousY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combined & 0xFFFF,
        (combined >> 16) & 0xFFFF);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y + settle, z);
    GlStateManager.translate(0.5, 0.0, 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();

    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    float scale = label.getTextHeight() / (fr.FONT_HEIGHT * CAP_SHARE);
    float pitch = label.getTextHeight() * LINE_PITCH;
    int count = Math.min(label.getLines(), lines.length);
    float rowY = label.getCentreY();
    for (int i = 0; i < count; i++) {
      String text = lines[i];
      if (text == null || text.isEmpty()) {
        continue;
      }
      if (label.isVertical()) {
        for (char c : text.toCharArray()) {
          drawLine(fr, String.valueOf(c), label, rowY, scale);
          rowY -= pitch;
        }
      } else {
        drawLine(fr, text, label, rowY, scale);
        rowY -= pitch;
      }
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousX, previousY);
  }

  /** One line centred at (centreX, centreY) on the face: its black backing, then its digits. */
  private void drawLine(CsmFontRenderer fr, String text, UtilityBoxSpec.Label label,
      float centreY, float scale) {
    float width = fr.getStringWidth(text) * scale;
    float halfW = width / 2.0f + MARGIN;
    float halfH = label.getTextHeight() / 2.0f + MARGIN;

    GlStateManager.pushMatrix();
    GlStateManager.translate(label.getCentreX(), centreY, label.getFaceZ() - 0.03f);
    GlStateManager.rotate(180, 0, 1, 0);

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.color(0.06f, 0.06f, 0.05f, 1.0f);
    BufferBuilder buf = Tessellator.getInstance().getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(-halfW, -halfH, 0).tex(0, 0).endVertex();
    buf.pos(halfW, -halfH, 0).tex(1, 0).endVertex();
    buf.pos(halfW, halfH, 0).tex(1, 1).endVertex();
    buf.pos(-halfW, halfH, 0).tex(0, 1).endVertex();
    Tessellator.getInstance().draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    // Turned half about y, this frame's +z points out of the face toward the viewer.
    GlStateManager.translate(0, 0, 0.02f);
    GlStateManager.depthMask(false);
    GlStateManager.scale(scale, -scale, scale);
    fr.drawString(text, -fr.getStringWidth(text) / 2, -fr.FONT_HEIGHT / 2, DECAL_YELLOW);
    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  private static float rotationFor(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90;
      case SOUTH:
        return 180;
      case EAST:
        return 270;
      case NORTH:
      default:
        return 0;
    }
  }
}
