package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import net.minecraft.block.Block;
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
 * TESR that renders a double-flash strobe on traffic beacon blocks. The pattern is two rapid
 * blinks per one-second cycle, matching common MUTCD Type B flashing beacon cadence.
 */
@SideOnly(Side.CLIENT)
public class TileEntityTrafficBeaconRenderer
    extends TileEntitySpecialRenderer<AbstractTileEntity> {

  private static final long CYCLE_MS = 1000L;
  private static final long FLASH1_START = 0L;
  private static final long FLASH1_END = 75L;
  private static final long FADE1_END = 125L;
  private static final long FLASH2_START = 250L;
  private static final long FLASH2_END = 325L;
  private static final long FADE2_END = 375L;

  @Override
  public void render(AbstractTileEntity te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof ITrafficBeaconBlock)) return;

    ITrafficBeaconBlock beacon = (ITrafficBeaconBlock) block;

    if (!state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.POWERED)) return;
    if (!state.getValue(AbstractPoweredBlockRotatableNSEWUD.POWERED)) return;

    long offset = (te instanceof TileEntityTrafficBeacon)
        ? ((TileEntityTrafficBeacon) te).getStrobeOffset() : 0L;
    long gameMillis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + offset;
    float intensity = computeIntensity(gameMillis);
    if (intensity <= 0f) return;

    if (!state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.FACING)) return;
    EnumFacing facing = state.getValue(AbstractPoweredBlockRotatableNSEWUD.FACING);

    float[] from = beacon.getBeaconLensFrom();
    float[] to = beacon.getBeaconLensTo();
    float r = beacon.getBeaconColorR();
    float g = beacon.getBeaconColorG();
    float b = beacon.getBeaconColorB();

    float minX = from[0] / 16f - 0.5f;
    float minY = from[1] / 16f - 0.5f;
    float minZ = from[2] / 16f - 0.5f;
    float maxX = to[0] / 16f - 0.5f;
    float maxY = to[1] / 16f - 0.5f;
    float maxZ = to[2] / 16f - 0.5f;

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

    // Core glow: all 6 faces of the beacon lens, slightly offset outward
    float off = 0.005f;
    GL11.glColor4f(r, g, b, 1.0f * intensity);
    drawBox(buf, tessellator, minX - off, minY - off, minZ - off,
        maxX + off, maxY + off, maxZ + off);

    // Inner halo: 50% padding around the lens
    float padX = (maxX - minX) * 0.5f;
    float padY = (maxY - minY) * 0.5f;
    float padZ = (maxZ - minZ) * 0.5f;
    GL11.glColor4f(r, g, b, 0.35f * intensity);
    drawBox(buf, tessellator, minX - padX, minY - padY, minZ - padZ,
        maxX + padX, maxY + padY, maxZ + padZ);

    // Outer halo: larger, more transparent
    GL11.glColor4f(r, g, b, 0.12f * intensity);
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

  private static float computeIntensity(long gameMillis) {
    long t = gameMillis % CYCLE_MS;
    // Flash 1
    if (t >= FLASH1_START && t < FLASH1_END) return 1.0f;
    if (t >= FLASH1_END && t < FADE1_END)
      return 1.0f - (float) (t - FLASH1_END) / (FADE1_END - FLASH1_END);
    // Flash 2
    if (t >= FLASH2_START && t < FLASH2_END) return 1.0f;
    if (t >= FLASH2_END && t < FADE2_END)
      return 1.0f - (float) (t - FLASH2_END) / (FADE2_END - FLASH2_END);
    return 0f;
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
      case UP:
        GlStateManager.rotate(90f, 1f, 0f, 0f);
        break;
      case DOWN:
        GlStateManager.rotate(-90f, 1f, 0f, 0f);
        break;
    }
  }
}
