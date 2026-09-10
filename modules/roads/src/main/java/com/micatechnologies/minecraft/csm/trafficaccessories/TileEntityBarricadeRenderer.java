package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRoadSurfaceAware;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws what a barricade is carrying: warning lights on its ends, and any mounted sign.
 *
 * <p>The sign is drawn from the sign block's OWN baked model rather than from a table of allowed
 * signs. The largest quad facing the model's front is its panel, and that quad gives both the
 * shape to draw and the sprite to draw it with, so every sign in the mod can be mounted and one
 * added later works without anything here changing.</p>
 *
 * <p>Rendering the sign's baked model outright would have been simpler still, and is wrong: sign
 * models carry their own mounting post, which would arrive bolted through the barricade.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityBarricadeRenderer
    extends TileEntitySpecialRenderer<TileEntityBarricade> {

  /** The amber lens of a warning light. */
  private static final float[] COL_LENS = {0.965f, 0.651f, 0.094f, 1.0f};

  /** The yellow body a warning light sits in. */
  private static final float[] COL_LAMP_BODY = {0.925f, 0.788f, 0.129f, 1.0f};

  /** The white of a sign's edge and its mounting straps. */
  private static final float[] COL_HARDWARE = {0.878f, 0.878f, 0.886f, 1.0f};

  /** One full flash cycle, matching the drums. */
  private static final long FLASH_CYCLE_MILLIS = 1000L;

  /** How long a warning light is lit within each cycle. */
  private static final long FLASH_PULSE_MILLIS = 90L;

  /** How long it fades afterwards. */
  private static final long FLASH_FADE_MILLIS = 70L;

  /** Half the width of a warning light's body. */
  private static final float LAMP_HALF = 1.15f;

  /** How tall a warning light's body is. */
  private static final float LAMP_HEIGHT = 1.5f;

  /** The radius of the lens above it. */
  private static final float LENS_RADIUS = 1.35f;

  /** The widest a mounted sign is drawn, in 1/16 units. */
  private static final float SIGN_MAX_WIDTH = 13.0f;

  /** The tallest a mounted sign is drawn. */
  private static final float SIGN_MAX_HEIGHT = 11.0f;

  /** How far a mounted sign stands proud of the rails it is bolted to. */
  private static final float SIGN_STANDOFF = 0.45f;

  /** How far apart a sign's two faces sit. */
  private static final float SIGN_THICKNESS = 0.25f;

  /**
   * Where a mounted sign's centre sits, as a fraction of the barricade's height.
   *
   * <p>A sign is bolted ACROSS the barricade's face, covering its middle rails, not perched
   * above it: that is how every one of these is actually rigged, and a sign floating over the
   * top reads as a separate object that happens to be nearby.</p>
   */
  private static final float SIGN_CENTRE_FRACTION = 0.70f;

  /** A 1x1 white pixel, bound so shaders cannot sample whatever was last used. */
  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** Fullbright, for a lit lens. */
  private static final int LIGHTMAP_FULLBRIGHT = 240;

  /**
   * The panel each sign block draws, worked out once and kept.
   *
   * <p>Reading it means walking a baked model's quads, which is far too much to do per frame per
   * barricade. Sign models do not change after the resource pack is loaded, so one lookup each
   * is enough.</p>
   */
  private static final Map<Block, SignPanel> SIGN_PANELS = new HashMap<>();

  @Override
  public void render(TileEntityBarricade te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockWorkZoneBarricade)) {
      return;
    }
    BarricadeFlashers flashers = te.getFlashers();
    Block sign = te.getSignBlock();
    if (flashers == BarricadeFlashers.NONE && sign == null) {
      return;
    }

    EnumFacing facing = state.getPropertyKeys().contains(AbstractBlockRotatableNSEW.FACING)
        ? state.getValue(AbstractBlockRotatableNSEW.FACING) : EnumFacing.NORTH;
    double settle = ((ICsmRoadSurfaceAware) block).getRoadSurfaceOffset(te.getWorld(),
        te.getPos());
    float topY = ((BlockWorkZoneBarricade) block).getTopY();

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + settle, z + 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    if (sign != null) {
      renderSign(sign, topY, sky, blockLight);
    }
    if (flashers != BarricadeFlashers.NONE) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      renderLamps(te, flashers, topY, partialTicks, sky, blockLight);
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * Draws a mounted sign, scaled to fit the barricade but keeping its own proportions.
   *
   * @param sign       the sign block
   * @param topY       the height of the barricade's uprights
   * @param sky        the sky light
   * @param blockLight the block light
   */
  private void renderSign(Block sign, float topY, int sky, int blockLight) {
    SignPanel panel = panelFor(sign);
    if (panel == null) {
      return;
    }

    // Fit inside the mount box without distorting the sign: whichever axis runs out first sets
    // the scale. Stretching to fill would misdraw every sign that is not the box's shape, which
    // is most of them.
    float scale = Math.min(SIGN_MAX_WIDTH / panel.width, SIGN_MAX_HEIGHT / panel.height);
    float halfW = panel.width * scale * 0.5f;
    float halfH = panel.height * scale * 0.5f;
    float cx = 8.0f;
    float cy = topY * SIGN_CENTRE_FRACTION;
    float faceZ = 8.0f - BarricadeGeometry.RAIL_HALF_Z - SIGN_STANDOFF;

    // The straps bolting it to the rails, spanning the sign's own height. They start BEHIND the
    // sign's rear face rather than at its front one: a strap that reaches the front runs straight
    // down the middle of the legend.
    List<RenderHelper.Box> hardware = new ArrayList<>();
    for (float sx : new float[]{cx - halfW * 0.6f, cx + halfW * 0.6f}) {
      hardware.add(new RenderHelper.Box(
          new float[]{sx - 0.3f, cy - halfH, faceZ + SIGN_THICKNESS},
          new float[]{sx + 0.3f, cy + halfH, 8.0f + BarricadeGeometry.RAIL_HALF_Z}));
    }
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(hardware, buf, COL_HARDWARE[0], COL_HARDWARE[1],
        COL_HARDWARE[2], COL_HARDWARE[3], 0, 0, 0, sky, blockLight);
    tess.draw();

    // The face itself, both sides, off the block atlas.
    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = panel.sprite;
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    quad(buf, cx - halfW, cx + halfW, cy - halfH, cy + halfH, faceZ, sprite, false, sky,
        blockLight);
    quad(buf, cx - halfW, cx + halfW, cy - halfH, cy + halfH, faceZ + SIGN_THICKNESS, sprite, true,
        sky, blockLight);
    tess.draw();
  }

  /**
   * Emits one face of a sign panel.
   *
   * @param buf        the buffer
   * @param x0         the left edge
   * @param x1         the right edge
   * @param y0         the bottom edge
   * @param y1         the top edge
   * @param z          the depth
   * @param sprite     the sprite to draw
   * @param back       whether this is the rear face, which is wound and mirrored the other way
   * @param sky        the sky light
   * @param blockLight the block light
   */
  private static void quad(BufferBuilder buf, float x0, float x1, float y0, float y1, float z,
      TextureAtlasSprite sprite, boolean back, int sky, int blockLight) {
    float u0 = sprite.getMinU();
    float u1 = sprite.getMaxU();
    float v0 = sprite.getMinV();
    float v1 = sprite.getMaxV();
    // A north-facing quad runs its u the opposite way to +x: standing in front of the sign, the
    // model's +x is on your LEFT. Taking u straight along +x reads the legend backwards.
    // Seen from behind, that reverses again, so the rear face gets the other assignment and the
    // legend reads correctly from both sides.
    float uLeft = back ? u0 : u1;
    float uRight = back ? u1 : u0;
    if (back) {
      vertex(buf, x0, y0, z, uLeft, v1, sky, blockLight);
      vertex(buf, x0, y1, z, uLeft, v0, sky, blockLight);
      vertex(buf, x1, y1, z, uRight, v0, sky, blockLight);
      vertex(buf, x1, y0, z, uRight, v1, sky, blockLight);
    } else {
      vertex(buf, x0, y0, z, uLeft, v1, sky, blockLight);
      vertex(buf, x1, y0, z, uRight, v1, sky, blockLight);
      vertex(buf, x1, y1, z, uRight, v0, sky, blockLight);
      vertex(buf, x0, y1, z, uLeft, v0, sky, blockLight);
    }
  }

  /**
   * Emits one textured vertex.
   *
   * @param buf        the buffer
   * @param x          the x position
   * @param y          the y position
   * @param z          the z position
   * @param u          the texture u
   * @param v          the texture v
   * @param sky        the sky light
   * @param blockLight the block light
   */
  private static void vertex(BufferBuilder buf, float x, float y, float z, float u, float v,
      int sky, int blockLight) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v).lightmap(sky, blockLight)
        .endVertex();
  }

  /**
   * Draws the warning lights, and their glow when lit.
   *
   * @param te           the barricade
   * @param flashers     which ends carry a light
   * @param topY         the height of the barricade's uprights
   * @param partialTicks the partial tick
   * @param sky          the sky light
   * @param blockLight   the block light
   */
  private void renderLamps(TileEntityBarricade te, BarricadeFlashers flashers, float topY,
      float partialTicks, int sky, int blockLight) {
    List<Float> centres = new ArrayList<>();
    if (flashers.hasLeft()) {
      centres.add(BarricadeGeometry.LEFT_UPRIGHT_X);
    }
    if (flashers.hasRight()) {
      centres.add(BarricadeGeometry.RIGHT_UPRIGHT_X);
    }

    List<RenderHelper.Box> bodies = new ArrayList<>();
    List<RenderHelper.Box> lenses = new ArrayList<>();
    for (float cx : centres) {
      bodies.add(new RenderHelper.Box(
          new float[]{cx - LAMP_HALF, topY, 8.0f - LAMP_HALF},
          new float[]{cx + LAMP_HALF, topY + LAMP_HEIGHT, 8.0f + LAMP_HALF}));
      lenses.add(new RenderHelper.Box(
          new float[]{cx - LENS_RADIUS, topY + LAMP_HEIGHT, 8.0f - LENS_RADIUS * 0.55f},
          new float[]{cx + LENS_RADIUS, topY + LAMP_HEIGHT + LENS_RADIUS * 1.6f,
              8.0f + LENS_RADIUS * 0.55f}));
    }
    if (bodies.isEmpty()) {
      return;
    }

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bodies, buf, COL_LAMP_BODY[0], COL_LAMP_BODY[1],
        COL_LAMP_BODY[2], COL_LAMP_BODY[3], 0, 0, 0, sky, blockLight);
    tess.draw();

    long millis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + te.getStrobeOffset();
    float intensity = pulse(millis);
    boolean lit = intensity > 0f && CsmConfig.isStrobeEffectEnabled();

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(lenses, buf, COL_LENS[0], COL_LENS[1], COL_LENS[2],
        COL_LENS[3], 0, 0, 0, lit ? LIGHTMAP_FULLBRIGHT : sky,
        lit ? LIGHTMAP_FULLBRIGHT : blockLight);
    tess.draw();

    if (!lit) {
      return;
    }
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    for (float[] layer : new float[][]{{0.7f, 0.34f}, {1.5f, 0.13f}}) {
      List<RenderHelper.Box> halo = new ArrayList<>();
      for (RenderHelper.Box box : lenses) {
        float pad = LENS_RADIUS * layer[0];
        halo.add(new RenderHelper.Box(
            new float[]{box.from[0] - pad, box.from[1] - pad, box.from[2] - pad},
            new float[]{box.to[0] + pad, box.to[1] + pad, box.to[2] + pad}));
      }
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(halo, buf, COL_LENS[0], COL_LENS[1], COL_LENS[2],
          layer[1] * intensity, 0, 0, 0, LIGHTMAP_FULLBRIGHT, LIGHTMAP_FULLBRIGHT);
      tess.draw();
    }
    GlStateManager.depthMask(true);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
  }

  /**
   * One short pulse a second, the same cadence the drums flash at.
   *
   * @param millis the barricade's own clock
   *
   * @return the brightness, 0 to 1
   */
  private static float pulse(long millis) {
    long t = Math.floorMod(millis, FLASH_CYCLE_MILLIS);
    if (t < FLASH_PULSE_MILLIS) {
      return 1.0f;
    }
    if (t < FLASH_PULSE_MILLIS + FLASH_FADE_MILLIS) {
      return 1.0f - (float) (t - FLASH_PULSE_MILLIS) / FLASH_FADE_MILLIS;
    }
    return 0f;
  }

  /**
   * Gets the panel a sign block draws, reading it from the sign's baked model the first time and
   * keeping it thereafter.
   *
   * @param sign the sign block
   *
   * @return the panel, or null if the sign has nothing that looks like one
   */
  private static SignPanel panelFor(Block sign) {
    if (SIGN_PANELS.containsKey(sign)) {
      return SIGN_PANELS.get(sign);
    }
    SignPanel panel = readPanel(sign);
    SIGN_PANELS.put(sign, panel);
    return panel;
  }

  /**
   * Finds a sign's panel: the largest quad facing the model's front.
   *
   * <p>Largest, because a sign model's other front-facing quads belong to its mounting post, and
   * the post is precisely what must not come along.</p>
   *
   * @param sign the sign block
   *
   * @return the panel, or null if none was found
   */
  private static SignPanel readPanel(Block sign) {
    IBlockState state = sign.getDefaultState();
    IBakedModel model;
    try {
      model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
    } catch (Exception e) {
      return null;
    }
    if (model == null) {
      return null;
    }

    SignPanel best = null;
    double bestArea = 0.0;
    float bestDepth = Float.MAX_VALUE;
    // Both lists have to be read, not the first one that answers. A face lying exactly on the
    // block's north boundary is filed under NORTH so it can be culled; a face a hair in front of
    // it is not. Sign models put the blank board on the boundary and the legend just ahead of it,
    // so stopping at the first non-empty list picks the blank one every time.
    for (EnumFacing side : new EnumFacing[]{EnumFacing.NORTH, null}) {
      List<BakedQuad> quads;
      try {
        quads = model.getQuads(state, side, 0L);
      } catch (Exception e) {
        continue;
      }
      for (BakedQuad quad : quads) {
        if (quad.getFace() != EnumFacing.NORTH) {
          continue;
        }
        float[] bounds = quadBounds(quad);
        float width = bounds[1] - bounds[0];
        float height = bounds[3] - bounds[2];
        float depth = bounds[4];
        double area = width * height;
        if (width <= 0.01f || height <= 0.01f) {
          continue;
        }
        // Bigger wins; among equals the FRONTMOST wins. Sign models carry a blank backing panel
        // exactly behind the legend, identical in size, and picking by area alone is a coin flip
        // between the artwork and a blank white board.
        boolean better = area > bestArea + 1.0e-4
            || (Math.abs(area - bestArea) <= 1.0e-4 && depth < bestDepth);
        if (better) {
          bestArea = area;
          bestDepth = depth;
          best = new SignPanel(width, height, quad.getSprite());
        }
      }
    }
    return best;
  }

  /**
   * Gets a quad's extent, as {minX, maxX, minY, maxY, minZ}.
   *
   * <p>Vertex data is packed as seven ints each in the block format, of which the first three are
   * the position as float bits.</p>
   *
   * <p>The z is carried along because it is what separates a sign's legend from the blank board
   * sitting immediately behind it, which is otherwise exactly the same size.</p>
   *
   * @param quad the quad
   *
   * @return the bounds
   */
  private static float[] quadBounds(BakedQuad quad) {
    int[] data = quad.getVertexData();
    int stride = data.length / 4;
    float minX = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float maxY = -Float.MAX_VALUE;
    float minZ = Float.MAX_VALUE;
    for (int i = 0; i < 4; i++) {
      float vx = Float.intBitsToFloat(data[i * stride]);
      float vy = Float.intBitsToFloat(data[i * stride + 1]);
      float vz = Float.intBitsToFloat(data[i * stride + 2]);
      minX = Math.min(minX, vx);
      maxX = Math.max(maxX, vx);
      minY = Math.min(minY, vy);
      maxY = Math.max(maxY, vy);
      minZ = Math.min(minZ, vz);
    }
    return new float[]{minX, maxX, minY, maxY, minZ};
  }

  /**
   * Gets the rotation, in degrees, that carries the model's north face to the given facing.
   *
   * @param facing the facing
   *
   * @return the rotation in degrees
   */
  private static float rotationFor(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90f;
      case SOUTH:
        return 180f;
      case EAST:
        return 270f;
      default:
        return 0f;
    }
  }

  /**
   * A sign's panel: how big it is in block units, and what it is painted with.
   */
  private static final class SignPanel {

    /** The panel's width, in block units. */
    private final float width;

    /** The panel's height, in block units. */
    private final float height;

    /** The sprite the panel is painted with. */
    private final TextureAtlasSprite sprite;

    private SignPanel(float width, float height, TextureAtlasSprite sprite) {
      this.width = width;
      this.height = height;
      this.sprite = sprite;
    }
  }
}
