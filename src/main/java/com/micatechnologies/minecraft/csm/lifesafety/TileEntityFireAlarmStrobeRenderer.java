package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * TESR that renders a fullbright white flash on fire alarm strobe blocks during alarm.
 * The flash follows NFPA 72 cadence: 75ms flash at 1Hz (75ms on, 925ms off).
 * Checks {@link ActiveStrobeRegistry} to determine if the block is actively alarming,
 * and {@link CsmConfig#isStrobeEffectEnabled()} to allow disabling via config.
 */
@SideOnly(Side.CLIENT)
public class TileEntityFireAlarmStrobeRenderer
    extends TileEntitySpecialRenderer<TileEntityFireAlarmStrobe> {

  /** Strobe cadence: 1Hz cycle (1000ms), 75ms flash duration. */
  private static final long STROBE_CYCLE_MS = 1000L;
  private static final long STROBE_FLASH_MS = 75L;

  @Override
  public void render(TileEntityFireAlarmStrobe te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;
    if (!ActiveStrobeRegistry.isActive(te.getPos())) return;

    // Check strobe timing — only render during the 75ms flash window
    long t = System.currentTimeMillis() % STROBE_CYCLE_MS;
    if (t >= STROBE_FLASH_MS) return;

    // Get facing direction for quad orientation
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) return;
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);

    // Compute strobe quad position from the block's bounding box
    AxisAlignedBB bb = state.getBoundingBox(te.getWorld(), te.getPos());

    // Save lightmap state and set fullbright
    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);

    // Rotate quad to face the correct direction based on block FACING
    applyFacingRotation(facing);

    // Disable texture, enable additive blending for bright flash
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    // Compute quad dimensions from bounding box (strobe lens in lower portion of device)
    float halfW = (float) (bb.maxX - bb.minX) * 0.3f;  // 60% of block width
    float halfH = (float) (bb.maxY - bb.minY) * 0.1f;  // 20% of block height
    float centerY = (float) (bb.minY + (bb.maxY - bb.minY) * 0.35f) - 0.5f; // 35% up from bottom
    float faceZ = (float) (bb.minZ - 0.5f) - 0.005f; // slightly in front of block face

    // Draw bright white strobe quad
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Main flash quad (bright white)
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.95f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(-halfW, centerY - halfH, faceZ).endVertex();
    buffer.pos(halfW, centerY - halfH, faceZ).endVertex();
    buffer.pos(halfW, centerY + halfH, faceZ).endVertex();
    buffer.pos(-halfW, centerY + halfH, faceZ).endVertex();
    tessellator.draw();

    // Glow halo (larger, semi-transparent)
    float glowW = halfW * 1.8f;
    float glowH = halfH * 1.8f;
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.35f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(-glowW, centerY - glowH, faceZ - 0.005f).endVertex();
    buffer.pos(glowW, centerY - glowH, faceZ - 0.005f).endVertex();
    buffer.pos(glowW, centerY + glowH, faceZ - 0.005f).endVertex();
    buffer.pos(-glowW, centerY + glowH, faceZ - 0.005f).endVertex();
    tessellator.draw();

    // Restore GL state
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.disableBlend();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();

    // Restore lightmap
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        prevBrightnessX, prevBrightnessY);
  }

  /**
   * Applies GL rotation so that the strobe quad faces outward from the block's mounting face.
   * The quad is drawn on the -Z face by default; this rotates it to match the block's FACING.
   */
  private static void applyFacingRotation(EnumFacing facing) {
    switch (facing) {
      case NORTH:
        // Default: quad on -Z face, no rotation needed
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
        GlStateManager.rotate(-90f, 1f, 0f, 0f);
        break;
      case DOWN:
        GlStateManager.rotate(90f, 1f, 0f, 0f);
        break;
    }
  }
}
