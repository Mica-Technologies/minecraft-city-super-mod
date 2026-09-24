package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the world's time on the Residential clocks: the digital clock's red digits (HH:MM, lit
 * whatever the light) and the wall clock's two hands. Everything else about either clock is its
 * baked model, so a clock costs a short string or two quads a frame; the string is made again
 * only when the minute changes, and where a digital clock stands is read from the world at most
 * once a second ({@link TileEntityResidentialClock} keeps both).
 *
 * <p>The positions are the models' ({@code gen_furniture_living.py}): the model faces north, so
 * the drawing is turned by the block's facing about the block's middle, then laid on the face.
 * A day of 24,000 ticks starts at 6:00, as the vanilla clock's does.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityResidentialClockRenderer
    extends TileEntitySpecialRenderer<TileEntityResidentialClock> {

  /** The digital clock's display: its middle and front, in sixteenths, facing north. */
  private static final double DIGITAL_X = 8.0;
  private static final double DIGITAL_Y = 1.75;
  private static final double DIGITAL_FACE_Z = 6.4;
  /** Font units to blocks: "88:88" (26 units) about 5 px across. */
  private static final float DIGITAL_SCALE = 0.012F;
  private static final int DIGITAL_COLOUR = 0xFF3A2A;

  /** The wall clock's dial: its middle and face, in sixteenths, facing north. */
  private static final double DIAL_X = 8.0;
  private static final double DIAL_Y = 8.0;
  private static final double DIAL_FACE_Z = 14.4;
  /** The hands, in sixteenths: length past the middle, the tail behind it, half the width. */
  private static final double HOUR_LENGTH = 3.1;
  private static final double MINUTE_LENGTH = 4.6;
  private static final double HAND_TAIL = 0.6;
  private static final double HOUR_HALF_WIDTH = 0.36;
  private static final double MINUTE_HALF_WIDTH = 0.24;

  private static final long TICKS_PER_DAY = 24000L;
  private static final long DROP_REFRESH_TICKS = 20L;

  @Override
  public void render(TileEntityResidentialClock te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    World world = te.getWorld();
    if (world == null) {
      return;
    }
    Block block = te.getBlockType();
    boolean digital = block instanceof BlockDigitalClock;
    if (!digital && !(block instanceof BlockWallClock)) {
      return;
    }
    EnumFacing facing = EnumFacing.byHorizontalIndex(te.getBlockMetadata() & 3);
    long time = world.getWorldTime() % TICKS_PER_DAY;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so that the model's front (north) faces +z here, as a sign's front does.
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    if (digital) {
      drawDigital(te, world, time);
    } else {
      drawHands(time);
    }
    GlStateManager.popMatrix();
  }

  private void drawDigital(TileEntityResidentialClock te, World world, long time) {
    long now = world.getTotalWorldTime();
    if (te.dropReadAt < 0 || now - te.dropReadAt >= DROP_REFRESH_TICKS || now < te.dropReadAt) {
      te.dropReadAt = now;
      te.drop = SurfaceRest.under(world, te.getPos()).getDrop();
    }
    FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    long minute = time * 60 / 1000;
    if (minute != te.textMinute) {
      te.textMinute = minute;
      int hours = (int) ((time / 1000 + 6) % 24);
      int minutes = (int) ((time % 1000) * 60 / 1000);
      te.text = String.format("%02d:%02d", hours, minutes);
      te.textWidth = font.getStringWidth(te.text);
    }
    // The model's point (mx, my, mz) is (8 - mx, my, 8 - mz) / 16 here.
    GlStateManager.translate((8 - DIGITAL_X) / 16.0, (DIGITAL_Y - te.drop) / 16.0,
        (8 - DIGITAL_FACE_Z) / 16.0 + 0.002);
    GlStateManager.scale(DIGITAL_SCALE, -DIGITAL_SCALE, DIGITAL_SCALE);
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
    font.drawString(te.text, -te.textWidth / 2, -font.FONT_HEIGHT / 2 + 1, DIGITAL_COLOUR);
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
  }

  private static void drawHands(long time) {
    double hours = ((time / 1000.0) + 6.0) % 12.0;
    double minutes = (time % 1000) * 60.0 / 1000.0;
    double hourAngle = Math.toRadians(hours * 30.0);
    double minuteAngle = Math.toRadians(minutes * 6.0);

    GlStateManager.translate((8 - DIAL_X) / 16.0, DIAL_Y / 16.0, (8 - DIAL_FACE_Z) / 16.0);
    GlStateManager.scale(1 / 16.0, 1 / 16.0, 1 / 16.0);
    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    hand(buffer, hourAngle, HOUR_LENGTH, HOUR_HALF_WIDTH, 0.05);
    hand(buffer, minuteAngle, MINUTE_LENGTH, MINUTE_HALF_WIDTH, 0.1);
    tessellator.draw();
    GlStateManager.enableLighting();
    GlStateManager.enableTexture2D();
  }

  /**
   * One hand, as a quad a little in front of the dial, turned clockwise (seen from the front)
   * from twelve by {@code angle}.
   */
  private static void hand(BufferBuilder buffer, double angle, double length, double halfWidth,
      double lift) {
    double c = Math.cos(angle);
    double s = Math.sin(angle);
    double[][] corners = {{-halfWidth, -HAND_TAIL}, {halfWidth, -HAND_TAIL},
        {halfWidth * 0.55, length}, {-halfWidth * 0.55, length}};
    for (double[] p : corners) {
      // Clockwise seen from the front, where +x is to the viewer's right and +y is up.
      double hx = p[0] * c + p[1] * s;
      double hy = -p[0] * s + p[1] * c;
      buffer.pos(hx, hy, lift).color(24, 22, 20, 255).endVertex();
    }
  }
}
