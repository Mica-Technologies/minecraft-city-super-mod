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

  /** Strobe quad dimensions in block units, centered on the device face. */
  private static final float STROBE_HALF_W = 0.15f;
  private static final float STROBE_HALF_H = 0.10f;

  /**
   * Z offset from block center to place the quad just in front of the device face.
   * Most fire alarm devices are mounted against the back of the block cell (Z near 1.0
   * in un-rotated coords), with the strobe lens on the front face (Z near 0.6-0.7).
   * This places the quad at 0.5 - 0.19 = 0.31 from block origin, which is roughly
   * where the device front face sits for typical fire alarm models.
   */
  private static final float STROBE_Z_OFFSET = -0.19f;

  /** Y offset from block center to place the strobe lens in the lower portion of the device. */
  private static final float STROBE_Y_OFFSET = -0.10f;

  @Override
  public void render(TileEntityFireAlarmStrobe te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;

    // Check alarm state from the client-side strobe registry
    boolean alarmActive = ActiveStrobeRegistry.isActive(te.getPos());
    if (!alarmActive) return;

    // Check strobe timing — only render during the 75ms flash window
    long t = System.currentTimeMillis() % STROBE_CYCLE_MS;
    if (t >= STROBE_FLASH_MS) return;

    // Get facing direction for quad orientation
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) return;
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);

    // Save lightmap state and set fullbright
    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);

    // Rotate so the quad faces outward from the device's mounting direction
    applyFacingRotation(facing);

    // Disable texture and face culling, enable additive blending
    GlStateManager.disableTexture2D();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Main flash quad (bright white)
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.95f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(-STROBE_HALF_W, STROBE_Y_OFFSET - STROBE_HALF_H, STROBE_Z_OFFSET).endVertex();
    buffer.pos(STROBE_HALF_W, STROBE_Y_OFFSET - STROBE_HALF_H, STROBE_Z_OFFSET).endVertex();
    buffer.pos(STROBE_HALF_W, STROBE_Y_OFFSET + STROBE_HALF_H, STROBE_Z_OFFSET).endVertex();
    buffer.pos(-STROBE_HALF_W, STROBE_Y_OFFSET + STROBE_HALF_H, STROBE_Z_OFFSET).endVertex();
    tessellator.draw();

    // Glow halo (larger, semi-transparent)
    float glowW = STROBE_HALF_W * 2.5f;
    float glowH = STROBE_HALF_H * 2.5f;
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(-glowW, STROBE_Y_OFFSET - glowH, STROBE_Z_OFFSET - 0.01f).endVertex();
    buffer.pos(glowW, STROBE_Y_OFFSET - glowH, STROBE_Z_OFFSET - 0.01f).endVertex();
    buffer.pos(glowW, STROBE_Y_OFFSET + glowH, STROBE_Z_OFFSET - 0.01f).endVertex();
    buffer.pos(-glowW, STROBE_Y_OFFSET + glowH, STROBE_Z_OFFSET - 0.01f).endVertex();
    tessellator.draw();

    // Restore GL state
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();

    // Restore lightmap
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        prevBrightnessX, prevBrightnessY);
  }

  /**
   * Applies GL rotation so that the strobe quad faces outward from the block's mounting face.
   * The quad is drawn on the -Z face by default (facing north); this rotates it to match
   * the block's FACING direction.
   */
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
        GlStateManager.rotate(-90f, 1f, 0f, 0f);
        break;
      case DOWN:
        GlStateManager.rotate(90f, 1f, 0f, 0f);
        break;
    }
  }
}
