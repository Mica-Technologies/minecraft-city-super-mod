package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * TESR that renders a steady blue glow on the tattle-tale beacon when its linked signal
 * phase is active (COLOR == SIGNAL_RED, which maps to the blue lit texture).
 */
@SideOnly(Side.CLIENT)
public class TileEntityTattleTaleBeaconRenderer
    extends TileEntitySpecialRenderer<TileEntityTattleTaleBeacon> {

  // Beacon lens element (Element 2) from signal_tattle_tale_beacon.json
  private static final float[] LENS_FROM = {6.0f, 11.85f, 6.4f};
  private static final float[] LENS_TO = {9.0f, 15.1f, 9.4f};

  private static final float CORE_R = 0.2f;
  private static final float CORE_G = 0.4f;
  private static final float CORE_B = 1.0f;
  private static final float HALO_R = 0.2f;
  private static final float HALO_G = 0.4f;
  private static final float HALO_B = 1.0f;

  @Override
  public void render(TileEntityTattleTaleBeacon te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof AbstractBlockControllableSignal)) return;
    if (!state.getPropertyKeys().contains(AbstractBlockControllableSignal.COLOR)) return;

    int color = state.getValue(AbstractBlockControllableSignal.COLOR);
    if (color != AbstractBlockControllableSignal.SIGNAL_RED) return;

    if (!state.getPropertyKeys().contains(AbstractBlockRotatableNSEW.FACING)) return;
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEW.FACING);

    float minX = LENS_FROM[0] / 16f - 0.5f;
    float minY = LENS_FROM[1] / 16f - 0.5f;
    float minZ = LENS_FROM[2] / 16f - 0.5f;
    float maxX = LENS_TO[0] / 16f - 0.5f;
    float maxY = LENS_TO[1] / 16f - 0.5f;
    float maxZ = LENS_TO[2] / 16f - 0.5f;

    int prevBrX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
    applyFacingRotation(facing);

    GlStateManager.disableTexture2D();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();

    float off = 0.01f;
    GL11.glColor4f(CORE_R, CORE_G, CORE_B, 0.6f);
    drawBox(buf, tessellator, minX - off, minY - off, minZ - off,
        maxX + off, maxY + off, maxZ + off);

    // Inner halo: blue bloom close to the lens
    float padX = (maxX - minX) * 0.6f;
    float padY = (maxY - minY) * 0.6f;
    float padZ = (maxZ - minZ) * 0.6f;
    GL11.glColor4f(HALO_R, HALO_G, HALO_B, 0.35f);
    drawBox(buf, tessellator, minX - padX, minY - padY, minZ - padZ,
        maxX + padX, maxY + padY, maxZ + padZ);

    // Outer halo: wider subtle glow
    GL11.glColor4f(HALO_R, HALO_G, HALO_B, 0.12f);
    drawBox(buf, tessellator, minX - padX * 2f, minY - padY * 2f, minZ - padZ * 2f,
        maxX + padX * 2f, maxY + padY * 2f, maxZ + padZ * 2f);

    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrX, prevBrY);
  }

  private static void drawBox(BufferBuilder buf, Tessellator tess,
      float x1, float y1, float z1, float x2, float y2, float z2) {
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    // -Z face
    buf.pos(x1, y1, z1).endVertex();
    buf.pos(x2, y1, z1).endVertex();
    buf.pos(x2, y2, z1).endVertex();
    buf.pos(x1, y2, z1).endVertex();
    // +Z face
    buf.pos(x2, y1, z2).endVertex();
    buf.pos(x1, y1, z2).endVertex();
    buf.pos(x1, y2, z2).endVertex();
    buf.pos(x2, y2, z2).endVertex();
    // -X face
    buf.pos(x1, y1, z2).endVertex();
    buf.pos(x1, y1, z1).endVertex();
    buf.pos(x1, y2, z1).endVertex();
    buf.pos(x1, y2, z2).endVertex();
    // +X face
    buf.pos(x2, y1, z1).endVertex();
    buf.pos(x2, y1, z2).endVertex();
    buf.pos(x2, y2, z2).endVertex();
    buf.pos(x2, y2, z1).endVertex();
    // -Y face
    buf.pos(x1, y1, z2).endVertex();
    buf.pos(x2, y1, z2).endVertex();
    buf.pos(x2, y1, z1).endVertex();
    buf.pos(x1, y1, z1).endVertex();
    // +Y face
    buf.pos(x1, y2, z1).endVertex();
    buf.pos(x2, y2, z1).endVertex();
    buf.pos(x2, y2, z2).endVertex();
    buf.pos(x1, y2, z2).endVertex();
    tess.draw();
  }

  private static void applyFacingRotation(EnumFacing facing) {
    switch (facing) {
      case NORTH:
        break;
      case SOUTH:
        GlStateManager.rotate(180f, 0f, 1f, 0f);
        break;
      case EAST:
        GlStateManager.rotate(-90f, 0f, 1f, 0f);
        break;
      case WEST:
        GlStateManager.rotate(90f, 0f, 1f, 0f);
        break;
      default:
        break;
    }
  }
}
