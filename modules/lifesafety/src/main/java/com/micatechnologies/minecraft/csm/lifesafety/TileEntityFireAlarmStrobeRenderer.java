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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
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

  /**
   * Light level at or above which the airborne and surface effects are gone. Below it they ramp
   * in, reaching full strength in the pitch dark. A strobe outdoors at noon should read as a bright
   * lens flashing, not as a beam -- there is nothing for its light to stand out against.
   */
  private static final float DARK_FULL_LIGHT = 11.0f;

  /**
   * How much of the airborne effect survives in full daylight. The halo and cone keep a floor
   * because they are partly the lens's own bloom, which you do see in daylight. The surface pools
   * get no floor at all: light landing on an already-lit wall is genuinely invisible.
   */
  private static final float ATMOSPHERE_DAYLIGHT_FLOOR = 0.30f;

  /** Peak alpha of a surface pool, before distance, incidence and darkness are applied. */
  private static final float POOL_ALPHA = 0.55f;

  /** Peak alpha of the wash thrown back onto the surface the device is mounted on. */
  private static final float WALL_WASH_ALPHA = 0.45f;

  /**
   * Radius of that wash, in blocks: a fixed spill plus a little for the size of the lens.
   *
   * <p>It cannot be a pure multiple of the lens half-extents. An L-Series lens is a tenth of a
   * block across, so any sane multiple of it lands smaller than the appliance and the whole wash
   * hides behind the device that casts it.</p>
   */
  private static final float WALL_WASH_RADIUS = 0.55f;
  private static final float WALL_WASH_LENS_SPREAD = 2.5f;

  /**
   * How much further the cone reaches in the pitch dark, as a multiple of its lit length. A beam
   * you can see hanging in the air is the whole reason a dark room feels different, and 0.45 of a
   * block does not read as one.
   */
  private static final float DARK_CONE_STRETCH = 2.5f;

  /** Halo extents in the light and in the dark, as multiples of the lens half-extents. */
  private static final float HALO_SPREAD_LIT = 2.0f;
  private static final float HALO_SPREAD_DARK = 3.5f;

  /** How far the wash and the pools sit off their surface, so they win its depth test. */
  private static final float SURFACE_LIFT = 0.012f;

  private static final int CONE_SEGMENTS = 5;
  private static final float CONE_MAX_PROJECTION_DIST = 0.45f;
  private static final float CONE_START_PAD = 0.6f;
  private static final float CONE_END_PAD = 2.5f;
  private static final float CONE_START_ALPHA = 0.16f;
  private static final float CONE_END_ALPHA = 0.02f;
  /** How far along the cone each ring sits, 0 at the lens to 1 at the tip. */
  private static final float[] CONE_T;
  private static final float[] CONE_HALF_DIM_FACTOR;
  private static final float[] CONE_ALPHA_BASE;

  static {
    CONE_T = new float[CONE_SEGMENTS + 1];
    CONE_HALF_DIM_FACTOR = new float[CONE_SEGMENTS + 1];
    CONE_ALPHA_BASE = new float[CONE_SEGMENTS + 1];
    for (int i = 0; i <= CONE_SEGMENTS; i++) {
      float t = (float) i / CONE_SEGMENTS;
      CONE_T[i] = t;
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

    // The device's own frame, for everything shaped by the lens: the flash, its halo and the cone.
    // The pools of light this throws onto surfaces are drawn after it is popped, in world axes.
    GlStateManager.pushMatrix();
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
    // The world render pass leaves the alpha test at GREATER 0.1, which throws away every fragment
    // fainter than that. It is the right rule for cutout block textures and exactly the wrong one
    // for additive glow: a pool of light that fades from 0.2 at its centre to nothing at its rim is
    // almost entirely below the threshold, so it is discarded and nothing reaches the screen. The
    // cone only ever survived it by sitting just above the line at 0.16.
    GlStateManager.disableAlpha();
    // Smooth shading, because the pools of light are the one part of this effect whose alpha
    // varies across a quad. The world render pass leaves the shade model FLAT, and under FLAT a
    // GL_QUADS face takes its colour from its LAST vertex -- which for a pool is a rim vertex at
    // alpha zero. Every disc was being flat-shaded to fully transparent, which is why correct
    // vertices, correct positions and a sane alpha still put nothing on the screen.
    GlStateManager.shadeModel(GL11.GL_SMOOTH);

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

    // How much the room lets this light show. The lens core and its glass edge are the source
    // itself and stay at full strength; everything that represents light travelling through air or
    // landing on something is scaled, which is what makes a dark room feel lit and stops a strobe
    // in daylight from trailing a beam through thin air.
    World world = te.getWorld();
    float darkness = darkness(world, te.getPos());
    float atmosphere =
        ATMOSPHERE_DAYLIGHT_FLOOR + (1.0f - ATMOSPHERE_DAYLIGHT_FLOOR) * darkness;

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
    float haloA = 0.35f * intensity * atmosphere;
    float haloSpread = HALO_SPREAD_LIT + (HALO_SPREAD_DARK - HALO_SPREAD_LIT) * darkness;
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    emitCap(buffer, unitX, unitY, cenX, cenY, halfW * haloSpread, halfH * haloSpread,
        quadZ - 0.02f, r, g, b, haloA);
    tessellator.draw();

    // Projected light cone — a 3D frustum (truncated pyramid) expanding outward from the
    // lens. Rendered as multiple nested frustum segments so the cone has graduated alpha
    // falloff. Each segment has 4 side walls + a front cap, visible from any viewing angle.
    float lensW = maxX - minX;
    float lensH = maxY - minY;
    float coneReach = CONE_MAX_PROJECTION_DIST * (1.0f + DARK_CONE_STRETCH * darkness);

    for (int i = 0; i < CONE_SEGMENTS; i++) {
      // Apply per-frame, per-block values to the precomputed segment factors
      float nearZ = quadZ - 0.03f - CONE_T[i] * coneReach;
      float farZ = quadZ - 0.03f - CONE_T[i + 1] * coneReach;
      float nw = lensW * CONE_HALF_DIM_FACTOR[i];
      float nh = lensH * CONE_HALF_DIM_FACTOR[i];
      float fw = lensW * CONE_HALF_DIM_FACTOR[i + 1];
      float fh = lensH * CONE_HALF_DIM_FACTOR[i + 1];
      float nearAlpha = CONE_ALPHA_BASE[i] * intensity * atmosphere;
      float farAlpha = CONE_ALPHA_BASE[i + 1] * intensity * atmosphere;

      // Segment side walls, fading along their length rather than each taking one flat average.
      // Under the smooth shade model this turns five stacked bands into one continuous beam; flat
      // averages left a visible step at every ring, which the stretched dark-room cone made
      // obvious.
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emitBand(buffer, unitX, unitY, cenX, cenY, nw, nh, nearZ, fw, fh, farZ,
          r, g, b, nearAlpha, farAlpha);
      tessellator.draw();

      // Front cap on each segment for head-on viewing. Brightest at the axis and fading to nothing
      // at the rim, so the stack reads as a beam rather than as a pile of discs.
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emitFadedCap(buffer, unitX, unitY, cenX, cenY, fw, fh, farZ, r, g, b, farAlpha);
      tessellator.draw();
    }

    // Wash thrown back onto the surface the device is mounted on. Real appliances spill a lot of
    // light onto their own backing, and it is the piece you cannot miss because it sits right where
    // you are already looking. Drawn just in front of the block's back face, so the device's own
    // model occludes the middle of it -- the appliance casts a shadow on its own wall.
    if (darkness > 0.01f) {
      BlockPos behind = te.getPos().offset(facing.getOpposite());
      if (world.getBlockState(behind).isSideSolid(world, behind, facing)) {
        float washA = WALL_WASH_ALPHA * intensity * darkness;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        emitSoftPool(buffer, cenX, cenY, 0.5f - SURFACE_LIFT,
            1.0, 0.0, 0.0, 0.0, 1.0, 0.0,
            WALL_WASH_RADIUS + Math.max(halfW, halfH) * WALL_WASH_LENS_SPREAD,
            r, g, b, washA);
        tessellator.draw();
      }
    }

    GlStateManager.popMatrix();

    // Pools of light where the beam actually lands. Placed in world axes rather than the device's,
    // because the surface a pool sits on has its own orientation and nothing to do with how the
    // appliance happens to be turned.
    if (darkness > 0.01f) {
      renderSurfacePools(world, te.getPos(), facing, cenX, cenY, quadZ, intensity, r, g, b,
          tessellator, buffer);
    }

    GlStateManager.depthMask(true);
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.enableAlpha();
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
    emitBand(buf, unitX, unitY, cenX, cenY, nearW, nearH, nearZ, farW, farH, farZ, r, g, b, a, a);
  }

  /** As above, fading from {@code nearA} at the near ring to {@code farA} at the far one. */
  private static void emitBand(BufferBuilder buf, float[] unitX, float[] unitY,
      float cenX, float cenY, float nearW, float nearH, float nearZ,
      float farW, float farH, float farZ, float r, float g, float b, float nearA, float farA) {
    int count = unitX.length;
    for (int i = 0; i < count; i++) {
      int next = (i + 1) % count;
      emit(buf, cenX + unitX[i] * nearW, cenY + unitY[i] * nearH, nearZ, r, g, b, nearA);
      emit(buf, cenX + unitX[next] * nearW, cenY + unitY[next] * nearH, nearZ, r, g, b, nearA);
      emit(buf, cenX + unitX[next] * farW, cenY + unitY[next] * farH, farZ, r, g, b, farA);
      emit(buf, cenX + unitX[i] * farW, cenY + unitY[i] * farH, farZ, r, g, b, farA);
    }
  }

  /** A cap at full alpha on the axis, fading to nothing at the outline. */
  private static void emitFadedCap(BufferBuilder buf, float[] unitX, float[] unitY,
      float cenX, float cenY, float halfW, float halfH, float z,
      float r, float g, float b, float a) {
    int count = unitX.length;
    for (int i = 0; i < count; i++) {
      int next = (i + 1) % count;
      emit(buf, cenX, cenY, z, r, g, b, a);
      emit(buf, cenX, cenY, z, r, g, b, a);
      emit(buf, cenX + unitX[i] * halfW, cenY + unitY[i] * halfH, z, r, g, b, 0.0f);
      emit(buf, cenX + unitX[next] * halfW, cenY + unitY[next] * halfH, z, r, g, b, 0.0f);
    }
  }

  /**
   * Draws a soft pool on every surface this strobe's beam reaches.
   *
   * <p>Each pool is faded by how far the light travelled, how squarely it struck, and how dark that
   * spot already is -- sampled where the light lands rather than at the device, so a beam crossing
   * from a dark corridor into a lit doorway fades out as it arrives.</p>
   */
  private static void renderSurfacePools(World world, BlockPos pos, EnumFacing facing,
      float cenX, float cenY, float lensZ, float intensity, float r, float g, float b,
      Tessellator tessellator, BufferBuilder buffer) {
    StrobeSurfaceProjection.Splash[] splashes = StrobeSurfaceProjection.get(
        world, pos, facing, new float[]{cenX, cenY, lensZ});
    for (StrobeSurfaceProjection.Splash splash : splashes) {
      float localDarkness = darkness(world, splash.lightPos);
      float alpha = POOL_ALPHA * intensity * splash.weight * localDarkness;
      if (alpha <= 0.004f) {
        continue;
      }
      double[] plane = planeAxes(splash.face);
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      emitSoftPool(buffer, splash.offsetX, splash.offsetY, splash.offsetZ,
          plane[0], plane[1], plane[2], plane[3], plane[4], plane[5],
          splash.radius, r, g, b, alpha);
      tessellator.draw();
    }
  }

  /** Two perpendicular in-plane axes for a face, as {ax, ay, az, bx, by, bz}. */
  private static double[] planeAxes(EnumFacing face) {
    switch (face.getAxis()) {
      case Y:
        return new double[]{1, 0, 0, 0, 0, 1};
      case X:
        return new double[]{0, 0, 1, 0, 1, 0};
      default:
        return new double[]{1, 0, 0, 0, 1, 0};
    }
  }

  /**
   * A disc that is brightest at its centre and fades to nothing at its rim, emitted as a fan of
   * degenerate quads the way {@link #emitCap} does so the whole effect stays in one primitive mode.
   *
   * <p>The gradient is the point. A flat disc of constant alpha reads as a decal stuck on the wall;
   * fading the rim to zero reads as light.</p>
   */
  private static void emitSoftPool(BufferBuilder buf, double cx, double cy, double cz,
      double ax, double ay, double az, double bx, double by, double bz,
      float radius, float r, float g, float b, float a) {
    for (int i = 0; i < ROUND_SEGMENTS; i++) {
      int next = (i + 1) % ROUND_SEGMENTS;
      double u0 = ROUND_UNIT_X[i] * radius;
      double v0 = ROUND_UNIT_Y[i] * radius;
      double u1 = ROUND_UNIT_X[next] * radius;
      double v1 = ROUND_UNIT_Y[next] * radius;
      emit(buf, cx, cy, cz, r, g, b, a);
      emit(buf, cx, cy, cz, r, g, b, a);
      emit(buf, cx + ax * u0 + bx * v0, cy + ay * u0 + by * v0, cz + az * u0 + bz * v0,
          r, g, b, 0.0f);
      emit(buf, cx + ax * u1 + bx * v1, cy + ay * u1 + by * v1, cz + az * u1 + bz * v1,
          r, g, b, 0.0f);
    }
  }

  /**
   * How dark it is at a position, 0 (bright) to 1 (pitch black).
   *
   * <p>Skylight is discounted by the world's current subtraction, so a room open to the sky goes
   * dark at night the way it should rather than reading as permanently lit.</p>
   */
  private static float darkness(World world, BlockPos pos) {
    int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
    int skyLight = world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted();
    float light = Math.max(blockLight, Math.max(skyLight, 0));
    if (light >= DARK_FULL_LIGHT) {
      return 0.0f;
    }
    return (DARK_FULL_LIGHT - light) / DARK_FULL_LIGHT;
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
