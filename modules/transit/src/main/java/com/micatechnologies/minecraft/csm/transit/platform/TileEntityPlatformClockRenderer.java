package com.micatechnologies.minecraft.csm.transit.platform;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the platform clock's hands, on both of its dials: the hour and minute hands, two quads a
 * dial, in one untextured batch. Everything else about the clock is its baked model.
 *
 * <p>The dial's middle and faces are the model's ({@code gen_transit_platforms.py}: CLOCK_CX,
 * CLOCK_CY, DIAL_FRONT, DIAL_BACK), which faces north with its two dials the same distance
 * either side of the block's middle, so the back dial is the front one turned half round. A day
 * of 24,000 ticks starts at 6:00, as the vanilla clock's does.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityPlatformClockRenderer
    extends TileEntitySpecialRenderer<TileEntityPlatformClock> {

  /** The dial's middle and its front face, in sixteenths, facing north. */
  private static final double DIAL_X = 8.0;
  private static final double DIAL_Y = 8.0;
  private static final double DIAL_FACE_Z = 6.3;
  /** The hands, in sixteenths: length past the middle, the tail behind it, half the width. */
  private static final double HOUR_LENGTH = 2.2;
  private static final double MINUTE_LENGTH = 3.2;
  private static final double HAND_TAIL = 0.5;
  private static final double HOUR_HALF_WIDTH = 0.28;
  private static final double MINUTE_HALF_WIDTH = 0.18;

  private static final long TICKS_PER_DAY = 24000L;

  @Override
  public void render(TileEntityPlatformClock te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    World world = te.getWorld();
    if (world == null || !(te.getBlockType() instanceof BlockPlatformClock)) {
      return;
    }
    EnumFacing facing = EnumFacing.byHorizontalIndex(te.getBlockMetadata() & 3);
    long time = world.getWorldTime() % TICKS_PER_DAY;
    double hours = ((time / 1000.0) + 6.0) % 12.0;
    double minutes = (time % 1000) * 60.0 / 1000.0;
    double hourAngle = Math.toRadians(hours * 30.0);
    double minuteAngle = Math.toRadians(minutes * 6.0);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so that the model's front (north) faces +z here, as a sign's front does.
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    for (int dial = 0; dial < 2; dial++) {
      GlStateManager.pushMatrix();
      if (dial == 1) {
        GlStateManager.rotate(180F, 0F, 1F, 0F);
      }
      // The model's point (mx, my, mz) is (8 - mx, my, 8 - mz) / 16 here.
      GlStateManager.translate((8 - DIAL_X) / 16.0, DIAL_Y / 16.0, (8 - DIAL_FACE_Z) / 16.0);
      GlStateManager.scale(1 / 16.0, 1 / 16.0, 1 / 16.0);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      hand(buffer, hourAngle, HOUR_LENGTH, HOUR_HALF_WIDTH, 0.04);
      hand(buffer, minuteAngle, MINUTE_LENGTH, MINUTE_HALF_WIDTH, 0.08);
      tessellator.draw();
      GlStateManager.popMatrix();
    }
    GlStateManager.enableLighting();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();
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
        {halfWidth * 0.6, length}, {-halfWidth * 0.6, length}};
    for (double[] p : corners) {
      double hx = p[0] * c + p[1] * s;
      double hy = -p[0] * s + p[1] * c;
      buffer.pos(hx, hy, lift).color(20, 20, 22, 255).endVertex();
    }
  }
}
