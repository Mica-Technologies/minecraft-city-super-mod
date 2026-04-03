package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
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
 * TESR that renders a fullbright white flash on fire alarm strobe blocks during alarm.
 * The flash follows NFPA 72 cadence: 75ms flash at 1Hz (75ms on, 925ms off).
 * The quad position is derived from the block's {@link IStrobeBlock#getStrobeLensFrom()} and
 * {@link IStrobeBlock#getStrobeLensTo()} which match Element 2 of the device's 3D model.
 */
@SideOnly(Side.CLIENT)
public class TileEntityFireAlarmStrobeRenderer
    extends TileEntitySpecialRenderer<AbstractTileEntity> {

  private static final long STROBE_CYCLE_MS = 1000L;
  private static final long STROBE_FLASH_MS = 75L;
  private static final long STROBE_FADE_MS = 75L;

  @Override
  public void render(AbstractTileEntity te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;
    if (!ActiveStrobeRegistry.isActive(te.getPos())) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof IStrobeBlock)) return;

    IStrobeBlock strobeBlock = (IStrobeBlock) block;
    boolean redToggle = strobeBlock.isRedSlowToggleStrobe();

    // Timing: compute intensity factor (1.0 = full flash, 0.0 = off)
    // Modern strobes: 75ms full brightness + 75ms fade-out (simulates xenon capacitor discharge)
    // Older red strobes: 500ms on / 500ms off with no fade
    float intensity;
    if (redToggle) {
      long t = System.currentTimeMillis() % STROBE_CYCLE_MS;
      if (t >= 500L) return;
      intensity = 1.0f;
    } else {
      long t = System.currentTimeMillis() % STROBE_CYCLE_MS;
      if (t < STROBE_FLASH_MS) {
        intensity = 1.0f;
      } else if (t < STROBE_FLASH_MS + STROBE_FADE_MS) {
        // Fade-out: linear ramp from 1.0 to 0.0 over STROBE_FADE_MS
        intensity = 1.0f - (float) (t - STROBE_FLASH_MS) / STROBE_FADE_MS;
      } else {
        return;
      }
    }
    if (!state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) return;

    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);
    float[] from = strobeBlock.getStrobeLensFrom();
    float[] to = strobeBlock.getStrobeLensTo();

    // Convert model coordinates (0-16) to block-centered coordinates (-0.5 to 0.5)
    float minX = from[0] / 16f - 0.5f;
    float minY = from[1] / 16f - 0.5f;
    float minZ = from[2] / 16f - 0.5f;
    float maxX = to[0] / 16f - 0.5f;
    float maxY = to[1] / 16f - 0.5f;

    // Quad sits on the front face of the lens element (minimum Z), offset slightly forward
    float quadZ = minZ - 0.01f;

    // Save lightmap and set fullbright
    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
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
    BufferBuilder buffer = tessellator.getBuffer();

    float maxZ = to[2] / 16f - 0.5f;
    float depth = maxZ - minZ;

    // Color: red for older incandescent-style strobes, white for modern xenon/LED
    float r = redToggle ? 1.0f : 1.0f;
    float g = redToggle ? 0.15f : 1.0f;
    float b = redToggle ? 0.1f : 1.0f;

    // Front face — main flash quad covering the strobe lens (fully opaque core)
    GL11.glColor4f(r, g, b, 1.0f * intensity);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(minX, minY, quadZ).endVertex();
    buffer.pos(maxX, minY, quadZ).endVertex();
    buffer.pos(maxX, maxY, quadZ).endVertex();
    buffer.pos(minX, maxY, quadZ).endVertex();
    tessellator.draw();

    // Side quads — make the strobe visible from angles (along the lens depth)
    GL11.glColor4f(r, g, b, 0.7f * intensity);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    // Left side
    buffer.pos(minX, minY, quadZ).endVertex();
    buffer.pos(minX, minY, quadZ + depth).endVertex();
    buffer.pos(minX, maxY, quadZ + depth).endVertex();
    buffer.pos(minX, maxY, quadZ).endVertex();
    // Right side
    buffer.pos(maxX, minY, quadZ + depth).endVertex();
    buffer.pos(maxX, minY, quadZ).endVertex();
    buffer.pos(maxX, maxY, quadZ).endVertex();
    buffer.pos(maxX, maxY, quadZ + depth).endVertex();
    // Top side
    buffer.pos(minX, maxY, quadZ).endVertex();
    buffer.pos(minX, maxY, quadZ + depth).endVertex();
    buffer.pos(maxX, maxY, quadZ + depth).endVertex();
    buffer.pos(maxX, maxY, quadZ).endVertex();
    // Bottom side
    buffer.pos(minX, minY, quadZ + depth).endVertex();
    buffer.pos(minX, minY, quadZ).endVertex();
    buffer.pos(maxX, minY, quadZ).endVertex();
    buffer.pos(maxX, minY, quadZ + depth).endVertex();
    tessellator.draw();

    // Inner glow halo — close bloom around the lens
    float pad1X = (maxX - minX) * 0.5f;
    float pad1Y = (maxY - minY) * 0.5f;
    GL11.glColor4f(r, g, b, 0.35f * intensity);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(minX - pad1X, minY - pad1Y, quadZ - 0.02f).endVertex();
    buffer.pos(maxX + pad1X, minY - pad1Y, quadZ - 0.02f).endVertex();
    buffer.pos(maxX + pad1X, maxY + pad1Y, quadZ - 0.02f).endVertex();
    buffer.pos(minX - pad1X, maxY + pad1Y, quadZ - 0.02f).endVertex();
    tessellator.draw();

    // Projected light cone — a 3D frustum (truncated pyramid) expanding outward from the
    // lens. Rendered as multiple nested frustum segments so the cone has graduated alpha
    // falloff. Each segment has 4 side walls + a front cap, visible from any viewing angle.
    float lensW = maxX - minX;
    float lensH = maxY - minY;
    float cenX = (minX + maxX) * 0.5f;
    float cenY = (minY + maxY) * 0.5f;

    int segments = 5;
    float maxProjectionDist = 0.45f;
    float startPad = 0.6f;
    float endPad = 2.5f;
    float startAlpha = 0.16f;
    float endAlpha = 0.02f;

    // Precompute ring positions: each ring is a rectangle at a given Z depth
    float[] ringZ = new float[segments + 1];
    float[] ringHalfW = new float[segments + 1];
    float[] ringHalfH = new float[segments + 1];
    float[] ringAlpha = new float[segments + 1];
    for (int i = 0; i <= segments; i++) {
      float t = (float) i / segments;
      ringZ[i] = quadZ - 0.03f - t * maxProjectionDist;
      float pad = startPad + t * (endPad - startPad);
      ringHalfW[i] = lensW * (0.5f + pad);
      ringHalfH[i] = lensH * (0.5f + pad);
      ringAlpha[i] = (startAlpha + t * (endAlpha - startAlpha)) * intensity;
    }

    for (int i = 0; i < segments; i++) {
      // Alpha for this segment is the average of the two ring endpoints
      float segAlpha = (ringAlpha[i] + ringAlpha[i + 1]) * 0.5f;

      float nearZ = ringZ[i];
      float farZ = ringZ[i + 1];
      float nw = ringHalfW[i];
      float nh = ringHalfH[i];
      float fw = ringHalfW[i + 1];
      float fh = ringHalfH[i + 1];

      GL11.glColor4f(r, g, b, segAlpha);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

      // Left wall
      buffer.pos(cenX - nw, cenY - nh, nearZ).endVertex();
      buffer.pos(cenX - fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX - fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX - nw, cenY + nh, nearZ).endVertex();

      // Right wall
      buffer.pos(cenX + nw, cenY - nh, nearZ).endVertex();
      buffer.pos(cenX + nw, cenY + nh, nearZ).endVertex();
      buffer.pos(cenX + fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY - fh, farZ).endVertex();

      // Top wall
      buffer.pos(cenX - nw, cenY + nh, nearZ).endVertex();
      buffer.pos(cenX - fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX + nw, cenY + nh, nearZ).endVertex();

      // Bottom wall
      buffer.pos(cenX - nw, cenY - nh, nearZ).endVertex();
      buffer.pos(cenX + nw, cenY - nh, nearZ).endVertex();
      buffer.pos(cenX + fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX - fw, cenY - fh, farZ).endVertex();

      tessellator.draw();

      // Front cap on each segment for head-on viewing
      GL11.glColor4f(r, g, b, ringAlpha[i + 1]);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
      buffer.pos(cenX - fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX - fw, cenY + fh, farZ).endVertex();
      tessellator.draw();
    }

    // Restore GL state
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        prevBrightnessX, prevBrightnessY);
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
