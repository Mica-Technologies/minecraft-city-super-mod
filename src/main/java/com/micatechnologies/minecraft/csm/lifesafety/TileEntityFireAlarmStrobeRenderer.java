package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
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

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  // Precomputed cone segment constants. These are unitless factors — at render time the
  // z-offset array is added to the lens z-coordinate, and the half-dim array is multiplied
  // by the lens width/height to get each ring's size. This avoids allocating four
  // segments+1 float arrays (and populating them via the i/SEGMENTS linear interpolation)
  // on every frame per strobe.
  private static final int CONE_SEGMENTS = 5;
  private static final float CONE_MAX_PROJECTION_DIST = 0.45f;
  private static final float CONE_START_PAD = 0.6f;
  private static final float CONE_END_PAD = 2.5f;
  private static final float CONE_START_ALPHA = 0.16f;
  private static final float CONE_END_ALPHA = 0.02f;
  private static final float[] CONE_Z_OFFSET;
  private static final float[] CONE_HALF_DIM_FACTOR;
  private static final float[] CONE_ALPHA_BASE;

  static {
    CONE_Z_OFFSET = new float[CONE_SEGMENTS + 1];
    CONE_HALF_DIM_FACTOR = new float[CONE_SEGMENTS + 1];
    CONE_ALPHA_BASE = new float[CONE_SEGMENTS + 1];
    for (int i = 0; i <= CONE_SEGMENTS; i++) {
      float t = (float) i / CONE_SEGMENTS;
      CONE_Z_OFFSET[i] = -0.03f - t * CONE_MAX_PROJECTION_DIST;
      float pad = CONE_START_PAD + t * (CONE_END_PAD - CONE_START_PAD);
      CONE_HALF_DIM_FACTOR[i] = 0.5f + pad;
      CONE_ALPHA_BASE[i] = CONE_START_ALPHA + t * (CONE_END_ALPHA - CONE_START_ALPHA);
    }
  }

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
    long gameMillis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
    if (redToggle) {
      long t = gameMillis % STROBE_CYCLE_MS;
      if (t >= 500L) return;
      intensity = 1.0f;
    } else {
      long t = gameMillis % STROBE_CYCLE_MS;
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

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    float maxZ = to[2] / 16f - 0.5f;
    float depth = maxZ - minZ;

    // Color: red for older incandescent-style strobes, white for modern xenon/LED
    float r = redToggle ? 1.0f : 1.0f;
    float g = redToggle ? 0.15f : 1.0f;
    float b = redToggle ? 0.1f : 1.0f;

    // Front face — main flash quad covering the strobe lens (fully opaque core)
    float coreA = 1.0f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emit(buffer, minX, minY, quadZ, r, g, b, coreA);
    emit(buffer, maxX, minY, quadZ, r, g, b, coreA);
    emit(buffer, maxX, maxY, quadZ, r, g, b, coreA);
    emit(buffer, minX, maxY, quadZ, r, g, b, coreA);
    tessellator.draw();

    // Side quads — make the strobe visible from angles (along the lens depth)
    float sideA = 0.7f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // Left side
    emit(buffer, minX, minY, quadZ, r, g, b, sideA);
    emit(buffer, minX, minY, quadZ + depth, r, g, b, sideA);
    emit(buffer, minX, maxY, quadZ + depth, r, g, b, sideA);
    emit(buffer, minX, maxY, quadZ, r, g, b, sideA);
    // Right side
    emit(buffer, maxX, minY, quadZ + depth, r, g, b, sideA);
    emit(buffer, maxX, minY, quadZ, r, g, b, sideA);
    emit(buffer, maxX, maxY, quadZ, r, g, b, sideA);
    emit(buffer, maxX, maxY, quadZ + depth, r, g, b, sideA);
    // Top side
    emit(buffer, minX, maxY, quadZ, r, g, b, sideA);
    emit(buffer, minX, maxY, quadZ + depth, r, g, b, sideA);
    emit(buffer, maxX, maxY, quadZ + depth, r, g, b, sideA);
    emit(buffer, maxX, maxY, quadZ, r, g, b, sideA);
    // Bottom side
    emit(buffer, minX, minY, quadZ + depth, r, g, b, sideA);
    emit(buffer, minX, minY, quadZ, r, g, b, sideA);
    emit(buffer, maxX, minY, quadZ, r, g, b, sideA);
    emit(buffer, maxX, minY, quadZ + depth, r, g, b, sideA);
    tessellator.draw();

    // Inner glow halo — close bloom around the lens
    float pad1X = (maxX - minX) * 0.5f;
    float pad1Y = (maxY - minY) * 0.5f;
    float haloA = 0.35f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emit(buffer, minX - pad1X, minY - pad1Y, quadZ - 0.02f, r, g, b, haloA);
    emit(buffer, maxX + pad1X, minY - pad1Y, quadZ - 0.02f, r, g, b, haloA);
    emit(buffer, maxX + pad1X, maxY + pad1Y, quadZ - 0.02f, r, g, b, haloA);
    emit(buffer, minX - pad1X, maxY + pad1Y, quadZ - 0.02f, r, g, b, haloA);
    tessellator.draw();

    // Projected light cone — a 3D frustum (truncated pyramid) expanding outward from the
    // lens. Rendered as multiple nested frustum segments so the cone has graduated alpha
    // falloff. Each segment has 4 side walls + a front cap, visible from any viewing angle.
    float lensW = maxX - minX;
    float lensH = maxY - minY;
    float cenX = (minX + maxX) * 0.5f;
    float cenY = (minY + maxY) * 0.5f;

    for (int i = 0; i < CONE_SEGMENTS; i++) {
      // Apply per-frame, per-block values to the precomputed segment factors
      float nearZ = quadZ + CONE_Z_OFFSET[i];
      float farZ = quadZ + CONE_Z_OFFSET[i + 1];
      float nw = lensW * CONE_HALF_DIM_FACTOR[i];
      float nh = lensH * CONE_HALF_DIM_FACTOR[i];
      float fw = lensW * CONE_HALF_DIM_FACTOR[i + 1];
      float fh = lensH * CONE_HALF_DIM_FACTOR[i + 1];
      float nearAlpha = CONE_ALPHA_BASE[i] * intensity;
      float farAlpha = CONE_ALPHA_BASE[i + 1] * intensity;
      // Alpha for this segment is the average of the two ring endpoints
      float segAlpha = (nearAlpha + farAlpha) * 0.5f;

      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

      // Left wall
      emit(buffer, cenX - nw, cenY - nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX - fw, cenY - fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX - nw, cenY + nh, nearZ, r, g, b, segAlpha);

      // Right wall
      emit(buffer, cenX + nw, cenY - nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX + nw, cenY + nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, r, g, b, segAlpha);

      // Top wall
      emit(buffer, cenX - nw, cenY + nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX + nw, cenY + nh, nearZ, r, g, b, segAlpha);

      // Bottom wall
      emit(buffer, cenX - nw, cenY - nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX + nw, cenY - nh, nearZ, r, g, b, segAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, r, g, b, segAlpha);
      emit(buffer, cenX - fw, cenY - fh, farZ, r, g, b, segAlpha);

      tessellator.draw();

      // Front cap on each segment for head-on viewing
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emit(buffer, cenX - fw, cenY - fh, farZ, r, g, b, farAlpha);
      emit(buffer, cenX + fw, cenY - fh, farZ, r, g, b, farAlpha);
      emit(buffer, cenX + fw, cenY + fh, farZ, r, g, b, farAlpha);
      emit(buffer, cenX - fw, cenY + fh, farZ, r, g, b, farAlpha);
      tessellator.draw();
    }

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    // Restore the standard alpha blend func before disabling blend — otherwise the
    // (SRC_ALPHA, ONE) additive func we set above remains as the global GL state, and
    // the next TESR that enables blend without setting its own func inherits additive
    // blending and visibly "strobes" semi-transparent geometry in step with this one.
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /** Emits one BLOCK-format vertex with explicit color and fullbright lightmap. */
  private static void emit(BufferBuilder buf, double px, double py, double pz,
      float r, float g, float b, float a) {
    buf.pos(px, py, pz).color(r, g, b, a).tex(0.5f, 0.5f)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
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
