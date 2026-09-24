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
 * Draws what a parking meter's heads show, from the time left on each space.
 *
 * <ul>
 *   <li>A mechanical head shows a dial whose needle sweeps down as time runs out, and the red
 *   EXPIRED flag in its place once it has.</li>
 *   <li>A digital head shows the time left on its LCD, or EXPIRED.</li>
 *   <li>A pay station's screen shows how many of its spaces are paid.</li>
 * </ul>
 *
 * <p>Everything is computed from the expiry each frame, so a display counts down without a
 * packet. The model draws each head's housing; this draws only the window's contents, just
 * proud of the window, with the settle onto the road applied by hand. The pay station's screen
 * is lit; a meter's window takes the world's light.</p>
 *
 * @version 1.0
 */
public class TileEntityParkingMeterRenderer
    extends TileEntitySpecialRenderer<TileEntityParkingMeter> {

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final float CAP_SHARE = 0.72f;
  private static final int LIGHT_FULL = 240;

  @Override
  public void render(TileEntityParkingMeter te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockParkingMeter)) {
      return;
    }
    BlockParkingMeter meterBlock = (BlockParkingMeter) block;
    EnumFacing facing = state.getValue(BlockUtilityBox.FACING);
    long now = System.currentTimeMillis();
    boolean lit = meterBlock.getKind() == BlockParkingMeter.Kind.STATION;

    double settle = RoadSurfaceHeight.offsetFor(te.getWorld(), te.getPos());
    int combined = te.getWorld().getCombinedLight(te.getPos().offset(facing), 0);
    float previousX = OpenGlHelper.lastBrightnessX;
    float previousY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        lit ? LIGHT_FULL : combined & 0xFFFF, lit ? LIGHT_FULL : (combined >> 16) & 0xFFFF);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y + settle, z);
    GlStateManager.translate(0.5, 0.0, 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();

    float[][] heads = meterBlock.getHeads();
    for (int i = 0; i < heads.length; i++) {
      float[] h = heads[i];
      GlStateManager.pushMatrix();
      // The window's centre, just proud of it, turned so +x reads left to right from the front.
      GlStateManager.translate(h[0], h[1], h[2] - 0.04f);
      GlStateManager.rotate(180, 0, 1, 0);
      switch (meterBlock.getKind()) {
        case MECHANICAL:
          drawMechanical(te, i, now, h[3], h[4]);
          break;
        case DIGITAL:
          drawDigital(te, i, now, h[3], h[4]);
          break;
        default:
          drawStation(te, now, h[3], h[4]);
          break;
      }
      GlStateManager.popMatrix();
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousX, previousY);
  }

  private void drawMechanical(TileEntityParkingMeter te, int space, long now, float w, float h) {
    if (te.isExpired(space, now)) {
      quad(0, 0, w, h, 0x1A1A1A, 0);
      quad(0, h * 0.12f, w * 0.86f, h * 0.56f, 0xC81E1E, 0.01f);
      text("EXPIRED", 0, h * 0.12f, w * 0.8f, h * 0.26f, 0xF4F4F0, 0.02f);
      return;
    }
    quad(0, 0, w, h, 0xE8E6DC, 0);
    // The needle sweeps from the right (full) to the left (empty) over the top of the dial.
    float full = te.getMaxMinutes() * 60_000f;
    float fraction = Math.max(0f, Math.min(1f, te.remaining(space, now) / full));
    float angle = -70f + 140f * (1f - fraction);
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, -h * 0.32f, 0.01f);
    GlStateManager.rotate(angle, 0, 0, 1);
    quad(0, h * 0.3f, w * 0.06f, h * 0.6f, 0x202020, 0);
    GlStateManager.popMatrix();
    text("TIME", 0, -h * 0.12f, w * 0.5f, h * 0.14f, 0x505050, 0.02f);
  }

  private void drawDigital(TileEntityParkingMeter te, int space, long now, float w, float h) {
    quad(0, 0, w, h, 0x9DAF8E, 0);
    String shown = te.isExpired(space, now) ? "EXPIRED"
        : ParkingPayments.remaining(te, space, now);
    text(shown, 0, 0, w * 0.88f, h * 0.55f, 0x15180F, 0.02f);
  }

  private void drawStation(TileEntityParkingMeter te, long now, float w, float h) {
    quad(0, 0, w, h, 0x13305C, 0);
    int paid = 0;
    for (int i = 0; i < te.getSpaces(); i++) {
      if (!te.isExpired(i, now)) {
        paid++;
      }
    }
    text("PAY HERE", 0, h * 0.2f, w * 0.85f, h * 0.26f, 0xF2F2F2, 0.02f);
    text(paid + "/" + te.getSpaces() + " PAID", 0, -h * 0.2f, w * 0.85f, h * 0.22f, 0x8FD0FF,
        0.02f);
  }

  /** A flat rectangle centred at (cx, cy), in a colour, pushed dz toward the viewer. */
  private static void quad(float cx, float cy, float w, float h, int colour, float dz) {
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.color(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
        (colour & 0xFF) / 255f, 1.0f);
    BufferBuilder buf = Tessellator.getInstance().getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(cx - w / 2, cy - h / 2, dz).tex(0, 0).endVertex();
    buf.pos(cx + w / 2, cy - h / 2, dz).tex(1, 0).endVertex();
    buf.pos(cx + w / 2, cy + h / 2, dz).tex(1, 1).endVertex();
    buf.pos(cx - w / 2, cy + h / 2, dz).tex(0, 1).endVertex();
    Tessellator.getInstance().draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /** One line centred at (cx, cy), as tall as {@code capHeight}, shrunk to {@code maxW}. */
  private static void text(String s, float cx, float cy, float maxW, float capHeight,
      int colour, float dz) {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    int width = fr.getStringWidth(s);
    float scale = capHeight / (fr.FONT_HEIGHT * CAP_SHARE);
    if (width * scale > maxW && width > 0) {
      scale = maxW / width;
    }
    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, dz);
    GlStateManager.depthMask(false);
    GlStateManager.scale(scale, -scale, scale);
    fr.drawString(s, -width / 2, -fr.FONT_HEIGHT / 2, colour);
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
