package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a pace clock's hands over its baked dial: the red sweep hand once round a minute and the
 * black hand once round an hour, from the world's total time (20 ticks a second) and the partial
 * tick, so the sweep is smooth and every clock in the world agrees. Four quads a frame.
 *
 * <p>The dial's centre and radius are {@code gen_venues_pool.py}'s ({@code DIAL_WALL},
 * {@code DIAL_STAND}): change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityPaceClockRenderer extends TileEntitySpecialRenderer<TileEntityPaceClock> {

  /** The dial on a wall and on a stand, model frame pixels: centre x, y, front z, radius. */
  static final float[] DIAL_WALL = {8f, 8f, 11.9f, 6.5f};
  static final float[] DIAL_STAND = {8f, 9.5f, 6.9f, 6.5f};

  @Override
  public void render(TileEntityPaceClock te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueCourts) {
      return;
    }
    IBlockState s = te.getWorld().getBlockState(te.getPos());
    if (!(s.getBlock() instanceof BlockPaceClock)) {
      return;
    }
    float[] d = s.getValue(BlockPaceClock.STAND) ? DIAL_STAND : DIAL_WALL;
    double t = (te.getWorld().getTotalWorldTime() % 72000L) + partialTicks;
    float sweep = (float) ((t % 1200.0) / 1200.0 * 360.0);
    float hour = (float) (t / 72000.0 * 360.0);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(s.getValue(BlockVenue.FACING).getHorizontalAngle() + 180.0F), 0F, 1F,
        0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.translate(d[0] / 16.0, d[1] / 16.0, d[2] / 16.0);
    GlStateManager.scale(1 / 16.0, 1 / 16.0, 1 / 16.0);
    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    hand(buf, hour, d[3] * 0.62f, 0.45f, -0.06f, 0x1A1A1C);
    hand(buf, sweep, d[3] * 0.92f, 0.28f, -0.1f, 0xD02020);
    hand(buf, sweep + 180f, d[3] * 0.22f, 0.4f, -0.1f, 0xD02020);
    hand(buf, 0f, 0.5f, 0.5f, -0.14f, 0x1A1A1C);
    tess.draw();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();
  }

  /**
   * A hand from the centre, {@code len} long and {@code half} half wide, at {@code deg} clockwise
   * from 12 as the swimmers see it, {@code dz} in front of the dial (towards -z).
   */
  private static void hand(BufferBuilder buf, float deg, float len, float half, float dz,
      int rgb) {
    double a = Math.toRadians(deg);
    // the dial faces -z; seen from there +x is to the viewer's left, so clockwise is towards -x
    float ux = (float) -Math.sin(a);
    float uy = (float) Math.cos(a);
    float px = -uy;
    float py = ux;
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    float bx = -ux * half;
    float by = -uy * half;
    buf.pos(bx + px * half, by + py * half, dz).color(r, g, b, 255).endVertex();
    buf.pos(bx - px * half, by - py * half, dz).color(r, g, b, 255).endVertex();
    buf.pos(ux * len - px * half, uy * len - py * half, dz).color(r, g, b, 255).endVertex();
    buf.pos(ux * len + px * half, uy * len + py * half, dz).color(r, g, b, 255).endVertex();
  }
}
