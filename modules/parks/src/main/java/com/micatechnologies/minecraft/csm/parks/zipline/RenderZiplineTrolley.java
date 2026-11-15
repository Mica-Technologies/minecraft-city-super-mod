package com.micatechnologies.minecraft.csm.parks.zipline;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a zipline trolley: a steel housing riding the cable, two hangers, and the yellow grip bar
 * the rider holds, just above their hands.
 *
 * @version 1.0
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class RenderZiplineTrolley extends Render<EntityZiplineTrolley> {

  /** How far below the cable the grip bar hangs: the rider's raised hands. */
  private static final double BAR_DROP = 0.55;

  public RenderZiplineTrolley(RenderManager manager) {
    super(manager);
    shadowSize = 0.0F;
  }

  @Override
  public void doRender(EntityZiplineTrolley entity, double x, double y, double z, float yaw,
      float partialTicks) {
    double cy = y + entity.height / 2;
    float rad = (float) Math.toRadians(-entity.rotationYaw);
    // The line runs along "along"; "across" is level and square to it.
    Vec3d along = new Vec3d(Math.sin(rad), 0, Math.cos(rad));
    Vec3d across = new Vec3d(along.z, 0, -along.x);
    Vec3d up = new Vec3d(0, 1, 0);
    Vec3d c = new Vec3d(x, cy, z);

    GlStateManager.pushMatrix();
    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    // Housing, round the cable.
    ZiplineDraw.box(buf, c, across, up, along, new Vec3d(0.12, 0.1, 0.24), 0.55F, 0.57F, 0.6F);
    // Hangers, splayed out to the ends of the bar.
    Vec3d barCentre = c.add(0, -BAR_DROP, 0);
    for (int side = -1; side <= 1; side += 2) {
      Vec3d top = c.add(across.scale(0.06 * side)).add(0, -0.08, 0);
      Vec3d bottom = barCentre.add(across.scale(0.26 * side));
      ZiplineDraw.tube(buf, top, bottom, 0.022, 0.25F, 0.25F, 0.27F);
    }
    // Grip bar, square to the line.
    ZiplineDraw.box(buf, barCentre, across, up, along, new Vec3d(0.3, 0.03, 0.03), 0.95F, 0.76F,
        0.15F);
    tess.draw();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();
    super.doRender(entity, x, y, z, yaw, partialTicks);
  }

  @Nullable
  @Override
  protected ResourceLocation getEntityTexture(EntityZiplineTrolley entity) {
    return TextureMap.LOCATION_BLOCKS_TEXTURE;
  }
}
