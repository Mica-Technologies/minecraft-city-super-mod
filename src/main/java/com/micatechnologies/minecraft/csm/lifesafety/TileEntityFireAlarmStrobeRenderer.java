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
  // Unit outlines of the two lens shapes, as offsets from the lens centre in units of its half
  // width and half height. Every part of the effect -- the core, the sides, the halo and each cone
  // segment -- is one of these outlines scaled and placed at a depth, so a round lens costs nothing
  // beyond swapping which array is read and a rectangular one emits exactly the quads it always
  // did, in the same order.
  private static final float[] RECT_UNIT_X = {-1.0f, 1.0f, 1.0f, -1.0f};
  private static final float[] RECT_UNIT_Y = {-1.0f, -1.0f, 1.0f, 1.0f};
  private static final int ROUND_SEGMENTS = 20;
  private static final float[] ROUND_UNIT_X = new float[ROUND_SEGMENTS];
  private static final float[] ROUND_UNIT_Y = new float[ROUND_SEGMENTS];

  static {
    for (int i = 0; i < ROUND_SEGMENTS; i++) {
      double angle = 2.0 * Math.PI * i / ROUND_SEGMENTS;
      ROUND_UNIT_X[i] = (float) Math.cos(angle);
      ROUND_UNIT_Y[i] = (float) Math.sin(angle);
    }
  }

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

    // The lens as a centre plus half extents, which is how every piece below is placed.
    float cenX = (minX + maxX) * 0.5f;
    float cenY = (minY + maxY) * 0.5f;
    float halfW = (maxX - minX) * 0.5f;
    float halfH = (maxY - minY) * 0.5f;
    boolean round = strobeBlock.getStrobeLensShape() == IStrobeBlock.StrobeLensShape.ROUND;
    float[] unitX = round ? ROUND_UNIT_X : RECT_UNIT_X;
    float[] unitY = round ? ROUND_UNIT_Y : RECT_UNIT_Y;

    // Color comes from the block: red for older incandescent-style strobes, white for
    // modern xenon/LED, and the lens colour for coloured-lens devices such as beacons.
    float[] strobeColor = strobeBlock.getStrobeColor();
    float r = strobeColor[0];
    float g = strobeColor[1];
    float b = strobeColor[2];

    // Front face — main flash covering the strobe lens (fully opaque core)
    float coreA = 1.0f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emitCap(buffer, unitX, unitY, cenX, cenY, halfW, halfH, quadZ, r, g, b, coreA);
    tessellator.draw();

    // Side wall — makes the strobe visible from angles (along the lens depth)
    float sideA = 0.7f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emitBand(buffer, unitX, unitY, cenX, cenY, halfW, halfH, quadZ, halfW, halfH,
        quadZ + depth, r, g, b, sideA);
    tessellator.draw();

    // Inner glow halo — close bloom around the lens, at twice its extents
    float haloA = 0.35f * intensity;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emitCap(buffer, unitX, unitY, cenX, cenY, halfW * 2f, halfH * 2f, quadZ - 0.02f,
        r, g, b, haloA);
    tessellator.draw();

    // Projected light cone — a 3D frustum (truncated pyramid) expanding outward from the
    // lens. Rendered as multiple nested frustum segments so the cone has graduated alpha
    // falloff. Each segment has 4 side walls + a front cap, visible from any viewing angle.
    float lensW = maxX - minX;
    float lensH = maxY - minY;

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

      // Segment side walls
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emitBand(buffer, unitX, unitY, cenX, cenY, nw, nh, nearZ, fw, fh, farZ,
          r, g, b, segAlpha);
      tessellator.draw();

      // Front cap on each segment for head-on viewing
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emitCap(buffer, unitX, unitY, cenX, cenY, fw, fh, farZ, r, g, b, farAlpha);
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

  /**
   * Emits a filled cap: one outline at a single depth, as quads covering its interior.
   *
   * <p>A four-point outline is the quad itself. A round one fans out from the centre, emitting a
   * quad per segment whose first two corners coincide so it degenerates to a triangle -- which
   * keeps the whole effect in {@link GL11#GL_QUADS} rather than switching primitive mode midway
   * through a draw.</p>
   */
  private static void emitCap(BufferBuilder buf, float[] unitX, float[] unitY,
      float cenX, float cenY, float halfW, float halfH, float z,
      float r, float g, float b, float a) {
    int count = unitX.length;
    if (count == 4) {
      for (int i = 0; i < 4; i++) {
        emit(buf, cenX + unitX[i] * halfW, cenY + unitY[i] * halfH, z, r, g, b, a);
      }
      return;
    }
    for (int i = 0; i < count; i++) {
      int next = (i + 1) % count;
      emit(buf, cenX, cenY, z, r, g, b, a);
      emit(buf, cenX, cenY, z, r, g, b, a);
      emit(buf, cenX + unitX[i] * halfW, cenY + unitY[i] * halfH, z, r, g, b, a);
      emit(buf, cenX + unitX[next] * halfW, cenY + unitY[next] * halfH, z, r, g, b, a);
    }
  }

  /** Emits the wall joining two copies of an outline at different depths and extents. */
  private static void emitBand(BufferBuilder buf, float[] unitX, float[] unitY,
      float cenX, float cenY, float nearW, float nearH, float nearZ,
      float farW, float farH, float farZ, float r, float g, float b, float a) {
    int count = unitX.length;
    for (int i = 0; i < count; i++) {
      int next = (i + 1) % count;
      emit(buf, cenX + unitX[i] * nearW, cenY + unitY[i] * nearH, nearZ, r, g, b, a);
      emit(buf, cenX + unitX[next] * nearW, cenY + unitY[next] * nearH, nearZ, r, g, b, a);
      emit(buf, cenX + unitX[next] * farW, cenY + unitY[next] * farH, farZ, r, g, b, a);
      emit(buf, cenX + unitX[i] * farW, cenY + unitY[i] * farH, farZ, r, g, b, a);
    }
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
