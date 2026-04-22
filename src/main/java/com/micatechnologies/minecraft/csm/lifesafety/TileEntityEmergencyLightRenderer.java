package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
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
 * TESR that renders a steady warm-white glow and forward-projected light cone on emergency
 * light bulbs when the light is active (POWERED = false, i.e., no mains power).
 *
 * <p>Unlike the strobe renderer which flashes, this produces a constant glow. Each emergency
 * light has two bulbs (left and right) that each get their own glow core, halo, and directional
 * light cone projecting forward.
 */
@SideOnly(Side.CLIENT)
public class TileEntityEmergencyLightRenderer
    extends TileEntitySpecialRenderer<AbstractTileEntity> {

  // Warm white color (slight warm tint like LED emergency lights)
  private static final float COLOR_R = 1.0f;
  private static final float COLOR_G = 0.95f;
  private static final float COLOR_B = 0.85f;

  // Precomputed cone segment constants. Same optimization as in the strobe renderer — the
  // per-segment interpolation factors are class-constant, so we hoist their computation out
  // of the per-frame render loop. At render time the z-offset array is added to the bulb z,
  // and the half-dim factor is multiplied by the bulb width/height.
  private static final int CONE_SEGMENTS = 10;
  private static final float CONE_MAX_PROJECTION_DIST = 1.6f;
  private static final float CONE_START_PAD = 0.5f;
  private static final float CONE_END_PAD = 3.5f;
  private static final float CONE_START_ALPHA = 0.15f;
  private static final float CONE_END_ALPHA = 0.005f;
  private static final float[] CONE_Z_OFFSET;
  private static final float[] CONE_HALF_DIM_FACTOR;
  private static final float[] CONE_ALPHA;

  static {
    CONE_Z_OFFSET = new float[CONE_SEGMENTS + 1];
    CONE_HALF_DIM_FACTOR = new float[CONE_SEGMENTS + 1];
    CONE_ALPHA = new float[CONE_SEGMENTS + 1];
    for (int i = 0; i <= CONE_SEGMENTS; i++) {
      float t = (float) i / CONE_SEGMENTS;
      CONE_Z_OFFSET[i] = -0.03f - t * CONE_MAX_PROJECTION_DIST;
      float pad = CONE_START_PAD + t * (CONE_END_PAD - CONE_START_PAD);
      CONE_HALF_DIM_FACTOR[i] = 0.5f + pad;
      CONE_ALPHA[i] = CONE_START_ALPHA + t * (CONE_END_ALPHA - CONE_START_ALPHA);
    }
  }

  @Override
  public void render(AbstractTileEntity te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof IEmergencyLightBlock)) return;

    // Emergency lights are active when NOT powered (backup mode)
    if (!state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.POWERED)) return;
    if (state.getValue(AbstractPoweredBlockRotatableNSEWUD.POWERED)) return;

    if (!state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) return;
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);

    IEmergencyLightBlock lightBlock = (IEmergencyLightBlock) block;

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

    // Render both bulbs
    renderBulbGlow(lightBlock.getLeftBulbFrom(), lightBlock.getLeftBulbTo());
    renderBulbGlow(lightBlock.getRightBulbFrom(), lightBlock.getRightBulbTo());

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

  /**
   * Renders the glow effect for a single bulb: front face core, side glow, halo, and
   * a forward-projected directional light cone.
   */
  private void renderBulbGlow(float[] from, float[] to) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // Convert model coordinates (0-16) to block-centered (-0.5 to 0.5)
    float minX = from[0] / 16f - 0.5f;
    float minY = from[1] / 16f - 0.5f;
    float minZ = from[2] / 16f - 0.5f;
    float maxX = to[0] / 16f - 0.5f;
    float maxY = to[1] / 16f - 0.5f;
    float maxZ = to[2] / 16f - 0.5f;

    float quadZ = minZ - 0.01f;
    float depth = maxZ - minZ;

    // --- Front face: bright core covering the bulb face ---
    GL11.glColor4f(COLOR_R, COLOR_G, COLOR_B, 0.9f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    buffer.pos(minX, minY, quadZ).endVertex();
    buffer.pos(maxX, minY, quadZ).endVertex();
    buffer.pos(maxX, maxY, quadZ).endVertex();
    buffer.pos(minX, maxY, quadZ).endVertex();
    tessellator.draw();

    // --- Side quads along the bulb depth (front edge to back edge) ---
    GL11.glColor4f(COLOR_R, COLOR_G, COLOR_B, 0.5f);
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

    // --- Outer side faces of the bulb (left, right, top, bottom — not back) ---
    // These make the bulb visibly lit from the side. They extend forward to quadZ so
    // they connect seamlessly with the front face, forming a continuous lit shell.
    float sideOffset = 0.01f;
    GL11.glColor4f(COLOR_R, COLOR_G, COLOR_B, 0.7f);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    // Left face (facing -X)
    buffer.pos(minX - sideOffset, minY, quadZ).endVertex();
    buffer.pos(minX - sideOffset, minY, maxZ).endVertex();
    buffer.pos(minX - sideOffset, maxY, maxZ).endVertex();
    buffer.pos(minX - sideOffset, maxY, quadZ).endVertex();
    // Right face (facing +X)
    buffer.pos(maxX + sideOffset, minY, maxZ).endVertex();
    buffer.pos(maxX + sideOffset, minY, quadZ).endVertex();
    buffer.pos(maxX + sideOffset, maxY, quadZ).endVertex();
    buffer.pos(maxX + sideOffset, maxY, maxZ).endVertex();
    // Top face (facing +Y)
    buffer.pos(minX, maxY + sideOffset, quadZ).endVertex();
    buffer.pos(minX, maxY + sideOffset, maxZ).endVertex();
    buffer.pos(maxX, maxY + sideOffset, maxZ).endVertex();
    buffer.pos(maxX, maxY + sideOffset, quadZ).endVertex();
    // Bottom face (facing -Y)
    buffer.pos(minX, minY - sideOffset, maxZ).endVertex();
    buffer.pos(minX, minY - sideOffset, quadZ).endVertex();
    buffer.pos(maxX, minY - sideOffset, quadZ).endVertex();
    buffer.pos(maxX, minY - sideOffset, maxZ).endVertex();
    tessellator.draw();

    // --- Directional light cone projecting forward from the bulb ---
    // Emergency lights project a focused beam forward, softer and longer than a strobe
    float lensW = maxX - minX;
    float lensH = maxY - minY;
    float cenX = (minX + maxX) * 0.5f;
    float cenY = (minY + maxY) * 0.5f;

    for (int i = 0; i < CONE_SEGMENTS; i++) {
      float nearZ = quadZ + CONE_Z_OFFSET[i];
      float farZ = quadZ + CONE_Z_OFFSET[i + 1];
      float nw = lensW * CONE_HALF_DIM_FACTOR[i];
      float nh = lensH * CONE_HALF_DIM_FACTOR[i];
      float fw = lensW * CONE_HALF_DIM_FACTOR[i + 1];
      float fh = lensH * CONE_HALF_DIM_FACTOR[i + 1];
      float segAlpha = (CONE_ALPHA[i] + CONE_ALPHA[i + 1]) * 0.5f;

      GL11.glColor4f(COLOR_R, COLOR_G, COLOR_B, segAlpha);
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

      // Front cap
      GL11.glColor4f(COLOR_R, COLOR_G, COLOR_B, CONE_ALPHA[i + 1]);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
      buffer.pos(cenX - fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY - fh, farZ).endVertex();
      buffer.pos(cenX + fw, cenY + fh, farZ).endVertex();
      buffer.pos(cenX - fw, cenY + fh, farZ).endVertex();
      tessellator.draw();
    }
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
