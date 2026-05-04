package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
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

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

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

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
    applyFacingRotation(facing);

    // Bind a 1x1 white pixel texture instead of disableTexture2D — shaders ignore
    // disableTexture2D and sample whatever was last bound. Fullbright lightmap is baked
    // per-vertex via the BLOCK vertex format.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    // Render both bulbs
    renderBulbGlow(lightBlock.getLeftBulbFrom(), lightBlock.getLeftBulbTo());
    renderBulbGlow(lightBlock.getRightBulbFrom(), lightBlock.getRightBulbTo());

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /** Emits one BLOCK-format vertex with explicit color and fullbright lightmap. */
  private static void emit(BufferBuilder buf, double px, double py, double pz,
      float r, float g, float b, float a) {
    buf.pos(px, py, pz).color(r, g, b, a).tex(0.5f, 0.5f)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
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
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emit(buffer, minX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.9f);
    emit(buffer, maxX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.9f);
    emit(buffer, maxX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.9f);
    emit(buffer, minX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.9f);
    tessellator.draw();

    // --- Side quads along the bulb depth (front edge to back edge) ---
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // Left side
    emit(buffer, minX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, minX, minY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, minX, maxY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, minX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    // Right side
    emit(buffer, maxX, minY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, maxY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    // Top side
    emit(buffer, minX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, minX, maxY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, maxY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    // Bottom side
    emit(buffer, minX, minY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, minX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    emit(buffer, maxX, minY, quadZ + depth, COLOR_R, COLOR_G, COLOR_B, 0.5f);
    tessellator.draw();

    // --- Outer side faces of the bulb (left, right, top, bottom — not back) ---
    // These make the bulb visibly lit from the side. They extend forward to quadZ so
    // they connect seamlessly with the front face, forming a continuous lit shell.
    float sideOffset = 0.01f;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // Left face (facing -X)
    emit(buffer, minX - sideOffset, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, minX - sideOffset, minY, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, minX - sideOffset, maxY, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, minX - sideOffset, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    // Right face (facing +X)
    emit(buffer, maxX + sideOffset, minY, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX + sideOffset, minY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX + sideOffset, maxY, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX + sideOffset, maxY, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    // Top face (facing +Y)
    emit(buffer, minX, maxY + sideOffset, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, minX, maxY + sideOffset, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX, maxY + sideOffset, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX, maxY + sideOffset, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    // Bottom face (facing -Y)
    emit(buffer, minX, minY - sideOffset, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, minX, minY - sideOffset, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX, minY - sideOffset, quadZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
    emit(buffer, maxX, minY - sideOffset, maxZ, COLOR_R, COLOR_G, COLOR_B, 0.7f);
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

      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

      // Left wall
      emit(buffer, cenX - nw, cenY - nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX - fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX - nw, cenY + nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);

      // Right wall
      emit(buffer, cenX + nw, cenY - nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + nw, cenY + nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);

      // Top wall
      emit(buffer, cenX - nw, cenY + nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + nw, cenY + nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);

      // Bottom wall
      emit(buffer, cenX - nw, cenY - nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + nw, cenY - nh, nearZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);
      emit(buffer, cenX - fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, segAlpha);

      tessellator.draw();

      // Front cap
      float capAlpha = CONE_ALPHA[i + 1];
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emit(buffer, cenX - fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, capAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, COLOR_R, COLOR_G, COLOR_B, capAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, capAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, COLOR_R, COLOR_G, COLOR_B, capAlpha);
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
