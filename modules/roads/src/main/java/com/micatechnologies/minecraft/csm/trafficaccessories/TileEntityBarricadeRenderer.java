package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
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
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
 * <p>Nothing here changes from frame to frame except whether a light is lit. The sign's straps
 * and its faces, and the light bodies with their lenses, are compiled once per look into lists
 * shared by every barricade that looks the same ({@link CsmSharedDisplayLists}): the straps and
 * the faces are two lists because they are two textures, keyed on the barricade block, the sign
 * block and the combined light; the lights are one, keyed on the barricade block, which ends carry
 * a light, whether they are lit and the combined light. The light stays in the vertices exactly as
 * before (at most 256 lights per look). The glow round a lit lens stays live: it fades over 70 ms,
 * so its brightness is not one of a few states. {@link CsmRenderToggles#sharedBakesPerFrame} draws
 * everything per frame, for comparison.</p>
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

  /** The widest a mounted sign is drawn, as a fraction of the distance between the uprights. */
  private static final float SIGN_MAX_WIDTH_FRACTION = 0.8125f;

  /** The tallest a mounted sign is drawn, as a fraction of the barricade's height. */
  private static final float SIGN_MAX_HEIGHT_FRACTION = 0.62f;

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
   * barricade. Sign models do not change while a resource pack is loaded, so one lookup each is
   * enough; but a panel holds its sprite, which a reload replaces, so {@link #clearSignPanels()}
   * empties this on every texture stitch and on disconnect.</p>
   */
  private static final Map<Block, SignPanel> SIGN_PANELS = new HashMap<>();

  /** The two glow layers round a lit lens: how far each reaches, in lens radii, and its alpha. */
  private static final float[][] HALO_LAYERS = {{0.7f, 0.34f}, {1.5f, 0.13f}};

  /** A mounted sign's straps, in the white swatch. Key, see {@link #signKey}. */
  private static final CsmSharedDisplayLists SIGN_HARDWARE_LISTS =
      new CsmSharedDisplayLists("barricade_sign_straps");

  /**
   * A mounted sign's two faces, on the block atlas. Key, see {@link #signKey}. The sprite's UVs
   * are compiled in, so these (and the straps, sized from the same panel) are released with
   * {@link #SIGN_PANELS} on every texture stitch.
   */
  private static final CsmSharedDisplayLists SIGN_FACE_LISTS =
      new CsmSharedDisplayLists("barricade_sign_face");

  /** The warning light bodies and lenses, in the white swatch. Key, see {@link #lampKey}. */
  private static final CsmSharedDisplayLists LAMP_LISTS =
      new CsmSharedDisplayLists("barricade_lamps");

  @Override
  public void render(TileEntityBarricade te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    // A bare barricade, which is most of them, draws nothing here: find that out from the tile
    // entity's own fields before reading the world at all.
    BarricadeFlashers flashers = te.getFlashers();
    Block sign = te.getSignBlock();
    if (flashers == BarricadeFlashers.NONE && sign == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof AbstractBlockWorkZoneBarricade)) {
      return;
    }

    DirectionEight facing = state.getPropertyKeys().contains(
        AbstractBlockRotatableHZEight.FACING)
        ? state.getValue(AbstractBlockRotatableHZEight.FACING) : DirectionEight.N;
    double settle = ((ICsmRoadSurfaceAware) block).getRoadSurfaceOffset(te.getWorld(),
        te.getPos());
    AbstractBlockWorkZoneBarricade barricade = (AbstractBlockWorkZoneBarricade) block;
    float topY = barricade.getTopY();

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);

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
      renderSign(barricade, block, sign, topY, combinedLight);
    }
    if (flashers != BarricadeFlashers.NONE) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      renderLamps(te, barricade, block, flashers, topY, partialTicks, combinedLight);
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * Draws a mounted sign, scaled to fit the barricade but keeping its own proportions: the straps
   * from one shared list in the white swatch, then both faces from another on the block atlas.
   *
   * @param barricade     the barricade block, which knows its own proportions
   * @param block         the same block, for the key
   * @param sign          the sign block
   * @param topY          the height of the barricade's uprights
   * @param combinedLight the combined light
   */
  private void renderSign(AbstractBlockWorkZoneBarricade barricade, Block block, Block sign,
      float topY, int combinedLight) {
    SignPanel panel = panelFor(sign);
    if (panel == null) {
      return;
    }
    long key = signKey(block, sign, combinedLight);

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawSignHardware(barricade, panel, topY, combinedLight);
    } else {
      int list = SIGN_HARDWARE_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = SIGN_HARDWARE_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawSignHardware(barricade, panel, topY, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawSignHardware(barricade, panel, topY, combinedLight);
      }
    }

    // The face itself, both sides, off the block atlas.
    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawSignFace(barricade, panel, topY, combinedLight);
    } else {
      int list = SIGN_FACE_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = SIGN_FACE_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawSignFace(barricade, panel, topY, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        GlStateManager.resetColor();
      } else {
        drawSignFace(barricade, panel, topY, combinedLight);
      }
    }
  }

  /**
   * Packs everything a mounted sign's straps and faces depend on. Facing and the road settle are
   * not in it: both are the matrix the lists are replayed under. The panel is not either: it is
   * a function of the sign block until the next texture stitch, which releases both lists.
   *
   * <pre>
   *  bits  0-31  combined light, as getCombinedLight returns it (sky in the high half)
   *  bits 32-47  barricade block id (uprights, rail and height)
   *  bits 48-63  sign block id
   * </pre>
   */
  private static long signKey(Block barricade, Block sign, int combinedLight) {
    return (combinedLight & 0xFFFFFFFFL)
        | ((long) (Block.getIdFromBlock(barricade) & 0xFFFF) << 32)
        | ((long) (Block.getIdFromBlock(sign) & 0xFFFF) << 48);
  }

  /**
   * Where a mounted sign sits and how big it is drawn, worked out the same way for the straps and
   * for the faces.
   */
  private static final class SignLayout {

    private final float halfW;
    private final float halfH;
    private final float cx;
    private final float cy;
    private final float railCz;
    private final float faceZ;

    private SignLayout(AbstractBlockWorkZoneBarricade barricade, SignPanel panel, float topY) {
      // Fit inside the mount box without distorting the sign: whichever axis runs out first sets
      // the scale. Stretching to fill would misdraw every sign that is not the box's shape, which
      // is most of them.
      float maxWidth = (barricade.getRightUprightX() - barricade.getLeftUprightX())
          * SIGN_MAX_WIDTH_FRACTION;
      float maxHeight = topY * SIGN_MAX_HEIGHT_FRACTION;
      float scale = Math.min(maxWidth / panel.width, maxHeight / panel.height);
      halfW = panel.width * scale * 0.5f;
      halfH = panel.height * scale * 0.5f;
      cx = 0.5f * (barricade.getLeftUprightX() + barricade.getRightUprightX());
      cy = topY * SIGN_CENTRE_FRACTION;
      railCz = barricade.getRailCentreZ();
      faceZ = railCz - barricade.getRailHalfZ() - SIGN_STANDOFF;
    }
  }

  /**
   * Draws the straps bolting a sign to the rails. Geometry only: the caller binds the white
   * swatch and owns every GL state.
   */
  private static void drawSignHardware(AbstractBlockWorkZoneBarricade barricade, SignPanel panel,
      float topY, int combinedLight) {
    SignLayout at = new SignLayout(barricade, panel, topY);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

    // The straps bolting it to the rails, spanning the sign's own height. They start BEHIND the
    // sign's rear face rather than at its front one: a strap that reaches the front runs straight
    // down the middle of the legend.
    List<RenderHelper.Box> hardware = new ArrayList<>();
    for (float sx : new float[]{at.cx - at.halfW * 0.6f, at.cx + at.halfW * 0.6f}) {
      hardware.add(new RenderHelper.Box(
          new float[]{sx - 0.3f, at.cy - at.halfH, at.faceZ + SIGN_THICKNESS},
          new float[]{sx + 0.3f, at.cy + at.halfH, at.railCz + barricade.getRailHalfZ()}));
    }
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(hardware, buf, COL_HARDWARE[0], COL_HARDWARE[1],
        COL_HARDWARE[2], COL_HARDWARE[3], 0, 0, 0, sky, blockLight);
    tess.draw();
  }

  /**
   * Draws a sign's front and rear faces. Geometry only: the caller binds the block atlas and owns
   * every GL state.
   */
  private static void drawSignFace(AbstractBlockWorkZoneBarricade barricade, SignPanel panel,
      float topY, int combinedLight) {
    SignLayout at = new SignLayout(barricade, panel, topY);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    TextureAtlasSprite sprite = panel.sprite;
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    quad(buf, at.cx - at.halfW, at.cx + at.halfW, at.cy - at.halfH, at.cy + at.halfH, at.faceZ,
        sprite, false, sky, blockLight);
    quad(buf, at.cx - at.halfW, at.cx + at.halfW, at.cy - at.halfH, at.cy + at.halfH,
        at.faceZ + SIGN_THICKNESS, sprite, true, sky, blockLight);
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
   * Draws the warning lights, and their glow when lit. The bodies and lenses come from one shared
   * list per look; the glow is drawn live.
   *
   * @param te            the barricade's tile entity
   * @param barricade     the barricade block, which knows where its uprights are
   * @param block         the same block, for the key
   * @param flashers      which ends carry a light
   * @param topY          the height of the barricade's uprights
   * @param partialTicks  the partial tick
   * @param combinedLight the combined light
   */
  private void renderLamps(TileEntityBarricade te, AbstractBlockWorkZoneBarricade barricade,
      Block block, BarricadeFlashers flashers, float topY, float partialTicks,
      int combinedLight) {
    if (!flashers.hasLeft() && !flashers.hasRight()) {
      return;
    }

    long millis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + te.getStrobeOffset();
    float intensity = pulse(millis);
    boolean lit = intensity > 0f && CsmConfig.isStrobeEffectEnabled();

    // The white swatch is bound by the caller.
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawLamps(barricade, flashers, topY, lit, combinedLight);
    } else {
      long key = lampKey(block, flashers, lit, combinedLight);
      int list = LAMP_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = LAMP_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawLamps(barricade, flashers, topY, lit, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawLamps(barricade, flashers, topY, lit, combinedLight);
      }
    }

    if (!lit) {
      return;
    }
    List<RenderHelper.Box> lenses = new ArrayList<>(2);
    addLenses(lenses, barricade, flashers, topY);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    for (float[] layer : HALO_LAYERS) {
      List<RenderHelper.Box> halo = new ArrayList<>(lenses.size());
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
   * Packs everything the light bodies and lenses depend on. Facing and the road settle are the
   * matrix the list is replayed under.
   *
   * <pre>
   *  bits  0-31  combined light, as getCombinedLight returns it (sky in the high half)
   *  bits 32-47  barricade block id (uprights, rail and height)
   *  bits 48-49  flashers ordinal
   *  bit  50     lit (the lenses fullbright rather than world lit)
   * </pre>
   */
  private static long lampKey(Block barricade, BarricadeFlashers flashers, boolean lit,
      int combinedLight) {
    return (combinedLight & 0xFFFFFFFFL)
        | ((long) (Block.getIdFromBlock(barricade) & 0xFFFF) << 32)
        | ((long) (flashers.ordinal() & 0x3) << 48)
        | ((lit ? 1L : 0L) << 50);
  }

  /**
   * Draws the light bodies, then their lenses, in one draw: both opaque and in one texture, in
   * the order they were once drawn in two. Geometry only: the caller binds the white swatch and
   * owns every GL state.
   */
  private static void drawLamps(AbstractBlockWorkZoneBarricade barricade,
      BarricadeFlashers flashers, float topY, boolean lit, int combinedLight) {
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;
    float railCz = barricade.getRailCentreZ();
    List<RenderHelper.Box> bodies = new ArrayList<>(2);
    if (flashers.hasLeft()) {
      addBody(bodies, barricade.getLeftUprightX(), topY, railCz);
    }
    if (flashers.hasRight()) {
      addBody(bodies, barricade.getRightUprightX(), topY, railCz);
    }
    List<RenderHelper.Box> lenses = new ArrayList<>(2);
    addLenses(lenses, barricade, flashers, topY);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bodies, buf, COL_LAMP_BODY[0], COL_LAMP_BODY[1],
        COL_LAMP_BODY[2], COL_LAMP_BODY[3], 0, 0, 0, sky, blockLight);
    RenderHelper.addBoxesToBufferLit(lenses, buf, COL_LENS[0], COL_LENS[1], COL_LENS[2],
        COL_LENS[3], 0, 0, 0, lit ? LIGHTMAP_FULLBRIGHT : sky,
        lit ? LIGHTMAP_FULLBRIGHT : blockLight);
    tess.draw();
  }

  /** Adds the body of the light on the upright at {@code cx}. */
  private static void addBody(List<RenderHelper.Box> out, float cx, float topY, float railCz) {
    out.add(new RenderHelper.Box(
        new float[]{cx - LAMP_HALF, topY, railCz - LAMP_HALF},
        new float[]{cx + LAMP_HALF, topY + LAMP_HEIGHT, railCz + LAMP_HALF}));
  }

  /** Adds the lens of each light the barricade carries, left first. */
  private static void addLenses(List<RenderHelper.Box> out,
      AbstractBlockWorkZoneBarricade barricade, BarricadeFlashers flashers, float topY) {
    float railCz = barricade.getRailCentreZ();
    if (flashers.hasLeft()) {
      addLens(out, barricade.getLeftUprightX(), topY, railCz);
    }
    if (flashers.hasRight()) {
      addLens(out, barricade.getRightUprightX(), topY, railCz);
    }
  }

  /** Adds the lens above the light on the upright at {@code cx}. */
  private static void addLens(List<RenderHelper.Box> out, float cx, float topY, float railCz) {
    out.add(new RenderHelper.Box(
        new float[]{cx - LENS_RADIUS, topY + LAMP_HEIGHT, railCz - LENS_RADIUS * 0.55f},
        new float[]{cx + LENS_RADIUS, topY + LAMP_HEIGHT + LENS_RADIUS * 1.6f,
            railCz + LENS_RADIUS * 0.55f}));
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
   * Forgets every sign panel read so far, and the sign lists compiled from them. Run on every
   * texture stitch, where a resource reload replaces the sprites a panel holds (and may replace
   * the model its size was read from), and on disconnect; on the client thread, which owns the GL
   * context, in both cases.
   */
  public static void clearSignPanels() {
    SIGN_PANELS.clear();
    SIGN_HARDWARE_LISTS.clear();
    SIGN_FACE_LISTS.clear();
  }

  /**
   * Clears the sign panels whenever a texture atlas is stitched: F3+T, a resource pack change, or
   * anything else that reloads resources. Registered from the Roads client proxy.
   */
  public static final class Events {

    /**
     * Clears the sign panels once an atlas has been stitched.
     *
     * @param event the stitch event
     */
    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
      clearSignPanels();
    }
  }

  /**
   * Gets the rotation, in degrees, that carries the model's north face to the given facing.
   *
   * @param facing the facing
   *
   * @return the rotation in degrees
   */
  private static float rotationFor(DirectionEight facing) {
    return facing.getRotationDegrees();
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
