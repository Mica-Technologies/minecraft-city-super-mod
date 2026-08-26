package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignAtlas;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.SignLightMode;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignEmblemKind;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignSlotPosition;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignVerticalPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import org.lwjgl.opengl.GL11;

/**
 * Draws the dynamic street sign. Immediate-mode, like the dynamic guide sign's renderer, and
 * bound by the same conventions -- read that renderer's header and
 * {@code assets/docs/DYNAMIC_GUIDE_SIGN_SYSTEM.md} before changing anything here:
 *
 * <ul>
 *   <li><b>Pixel space.</b> Everything is in sign pixels; the renderer scales by 1/16 so 16
 *       units is one block.</li>
 *   <li><b>Mirrored pixel space.</b> After the scale it applies {@code translate(16,0,0);
 *       scale(-1,1,1)} so +X in all layout math is the READER's right. Overlays therefore
 *       draw unflipped: text advances toward +X and atlas quads map u0 to their left edge.
 *       Never re-add a per-overlay flip.</li>
 *   <li><b>Viewer at smaller Z.</b> Every depth comes from the Z layer table below, never
 *       from an offset written at the call site. The gaps in that table are a correctness
 *       constraint: too small and the layers z-fight into a flickering outline at range.</li>
 *   <li><b>Lit faces must be sleeved.</b> {@code addBoxesToBufferLit} lights all six faces of
 *       a box alike, so a fullbright plate spanning the assembly's depth would glow from
 *       behind and along its edges. The painted plates are only {@code LIT_FACE_DEPTH} deep
 *       and the ambient-lit core slab is drawn oversize around them.</li>
 * </ul>
 *
 * <p>What is different here is the mount. A {@link StreetSignMount#FLAT} blade sits against
 * the block behind it exactly like a guide sign. A {@link StreetSignMount#HANGING} blade is
 * centered in the block's depth, drops below two hangers that reach half a block ABOVE its own
 * block so they can grip a top slab, and can carry its legend on both faces -- the back face
 * is the same draw inside a 180 degree Y rotation about the block center, which is
 * orientation-preserving and so reads correctly (not mirrored) from behind.
 */
public class TileEntityDynamicStreetSignRenderer
    extends TileEntitySpecialRenderer<TileEntityDynamicStreetSign> {

  // ---- Assembly depth ----------------------------------------------------------------
  private static final float SIGN_DEPTH = 1.5f;
  // The painted plates are only this deep; the core slab sleeves them. See the class notes.
  private static final float LIT_FACE_DEPTH = 0.25f;
  private static final float BACK_SLEEVE_MARGIN = 0.06f;

  // ---- Z layers ---------------------------------------------------------------------
  // Offsets from faceZ, negative toward the viewer. EVERY depth in this renderer comes from
  // this table -- no magic offsets at the call sites -- because the spacing between the
  // layers is a correctness constraint, not a style choice: the guide sign's 0.05 gap between
  // the border plate and the core slab z-fights into a flickering outline once the sign is
  // more than a few blocks away, which is exactly what a blade is usually viewed from. Every
  // gap here is at least 0.15 sign px (~0.01 blocks), which the depth buffer resolves out to
  // the LOD range.
  //
  // The sleeve invariant still holds, and it is what bounds how far apart these can go: each
  // painted plate's rear and side faces must end up INSIDE the next layer back, or an
  // illuminated blade glows along its edges and from behind. Reading front to back, the
  // colored face is sleeved by the border plate and the border plate by the core slab.
  private static final float Z_FRAME = -1.00f;
  private static final float Z_ROUTE_TEXT = -0.85f;
  private static final float Z_EMBLEM = -0.70f;
  private static final float Z_LEGEND = -0.55f;
  private static final float Z_FACE_PLATE = -0.36f;
  private static final float Z_BORDER_PLATE = -0.18f;
  private static final float Z_CORE_FRONT = 0.02f;
  // Multiplier: actual border thickness = borderWidth * this. Deliberately larger than the
  // guide sign's 0.4 -- that sign is several blocks tall, where 0.4 px reads as a proper
  // border; a one-block-tall blade needs about 1 px before the border is visible at all.
  private static final float BORDER_INSET = 1.0f;

  // ---- Block-space anchors -------------------------------------------------------------
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 8.0f;
  /** How far below the block's top edge a hanging blade's top rail sits. */
  private static final float HANG_DROP = 5.5f;

  // ---- Padding and gaps ------------------------------------------------------------------
  private static final float PAD_SIDE = 3.0f;
  private static final float PAD_TOP = 2.0f;
  private static final float PAD_BOTTOM = 2.0f;
  /** Gap between the street name and a cardinal prefix or street-type suffix. */
  private static final float AFFIX_GAP = 1.4f;
  /** Gap between the text column and any side slot (block number, emblem, arrow). */
  private static final float SLOT_GAP = 2.5f;
  /** Gap between the name line and the city line under it. */
  private static final float CITY_GAP = 1.2f;

  // ---- Legend sizing ---------------------------------------------------------------------
  /** Street-name capital height in sign pixels at textScale 1.0. */
  private static final float NAME_CAP_HEIGHT = 6.5f;
  /** Prefix and suffix cap height as a fraction of the name's -- the raised-affix look. */
  private static final float AFFIX_CAP_FRACTION = 0.55f;
  private static final float CITY_CAP_FRACTION = 0.42f;
  private static final float BLOCK_CAP_FRACTION = 0.50f;
  /** Line height over cap height, leaving room for lowercase descenders. */
  private static final float TEXT_VISUAL_FACTOR = 1.32f;
  private static final float EMBLEM_SIZE = 11.0f;
  private static final float ARROW_SIZE = 9.0f;
  /** Route-number cap height over an emblem shield, as a fraction of the shield size. */
  private static final float ROUTE_CAP_FRACTION = 0.42f;

  private static final float CORNER_STEP = 0.6f;

  // ---- Extruded frame ----------------------------------------------------------------------
  /** Depth of the top and bottom rails of an extruded (internally-lit) blade. */
  private static final float FRAME_RAIL = 1.6f;
  /** Width of the cast end pieces that close the extrusion. */
  private static final float FRAME_END = 1.9f;
  /**
   * How far the frame bites into the panel's outer edge. Only enough to close the seam
   * against the core slab's oversize margin -- any more and it swallows the legend border,
   * which on a real extruded blade stays visible inside the frame.
   */
  private static final float FRAME_SEAM = 0.05f;

  // ---- Hangers -------------------------------------------------------------------------------
  private static final float HANGER_SHOE_WIDTH = 2.6f;
  private static final float HANGER_SHOE_HEIGHT = 1.1f;
  private static final float HANGER_SHOE_DEPTH = 2.6f;
  private static final float HANGER_ROD_WIDTH = 0.9f;
  private static final float HANGER_CLAMP_WIDTH = 2.6f;
  private static final float HANGER_CLAMP_HEIGHT = 1.8f;
  private static final float HANGER_CLAMP_DEPTH = 3.4f;
  /** Fraction of the blade's width the hangers are inset from each end. */
  private static final float HANGER_INSET_FRACTION = 0.22f;
  /**
   * How far above its own block the hanger run reaches -- half a block, so the clamp lands on
   * the underside of a TOP slab sitting in the space above.
   *
   * <p>Stopping at the block boundary (16) was only ever right for the things whose underside
   * IS that boundary: a full block, or a bottom slab. Anything mounted higher in the block
   * above -- a top slab, an upper step -- left the blade hanging from nothing across a visible
   * gap. Running to 24 covers those, and costs nothing in the cases that already worked: the
   * extra length is inside a full block or inside a bottom slab, so it is never seen.
   */
  private static final float HANGER_REACH_ABOVE = 8.0f;
  /** Every member overlaps its neighbor by this much so no two faces are ever coplanar. */
  private static final float JOINT_OVERLAP = 0.3f;

  // ---- Power feed cable ----------------------------------------------------------------
  /**
   * Square section of the feed cable. Matches the wire radius the sensor blocks' OBJ models use
   * (0.018-0.022 blocks, so ~0.6 sign px across) so every cable in the mod reads at one weight.
   */
  private static final float CABLE_THICKNESS = 0.65f;
  /** How far the cable bellies away from the blade at the middle of its run. */
  private static final float CABLE_BOW = 0.7f;
  /** Where along the frame's end casting the cable leaves, as a fraction of its width. */
  private static final float CABLE_END_FRACTION = 0.35f;
  /** Segments the bow is stepped in. Enough to read as a curve at the size it renders. */
  private static final int CABLE_SEGMENTS = 8;

  // ---- Lighting / LOD -----------------------------------------------------------------------
  private static final int LIGHT_NIGHT_SKY_THRESHOLD = 8;
  private static final int FULLBRIGHT = 240;
  private static final double LOD_FULL_DETAIL_DIST_SQ = 64.0 * 64.0;
  private static final int LEGEND_DARK = 0x101010;
  private static final int LEGEND_WHITE = 0xFFFFFF;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  // Per-frame light state. worldSky/worldBlock go fullbright for the face and legend of an
  // illuminated blade; ambientSky/ambientBlock keep the block's true light for the structural
  // metal (core slab, frame, hangers), so a night scene still reads as night around it.
  private int worldSkyLight;
  private int worldBlockLight;
  private int ambientSkyLight;
  private int ambientBlockLight;
  private boolean lightOn;

  @Override
  public void render(TileEntityDynamicStreetSign te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    StreetSignData data = te.getSignData();
    if (data == null) {
      return;
    }

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    worldSkyLight = (combinedLight >> 16) & 0xFFFF;
    worldBlockLight = combinedLight & 0xFFFF;
    ambientSkyLight = worldSkyLight;
    ambientBlockLight = worldBlockLight;
    lightOn = resolveLightOn(data, te);

    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(BlockHorizontal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.translate(0.5, 0.0, 0.5);

    // The panel is modelled on the block's +Z side reading toward -Z, so an unrotated draw faces
    // NORTH. Assigning 0 degrees to SOUTH therefore rendered the north/south pair backwards --
    // a sign set to face south showed its blank back to a viewer standing south of it -- while
    // east and west, being a quarter turn either side, came out right and hid the error.
    float rotY = 0;
    switch (facing) {
      case NORTH:
        rotY = 0;
        break;
      case WEST:
        rotY = 90;
        break;
      case SOUTH:
        rotY = 180;
        break;
      case EAST:
        rotY = 270;
        break;
      default:
        break;
    }
    GlStateManager.rotate(rotY, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    // Un-mirror pixel space so +X below is the reader's right. See the class notes.
    GlStateManager.translate(16.0f, 0.0f, 0.0f);
    GlStateManager.scale(-1.0f, 1.0f, 1.0f);

    // x/y/z are camera-relative, so this is the squared camera distance.
    boolean farLod = x * x + y * y + z * z > LOD_FULL_DETAIL_DIST_SQ;
    renderSign(data, farLod);

    GlStateManager.popMatrix();
  }

  /**
   * Draws the blade flat for the GUI preview, fullbright, through the exact world render
   * path. The caller owns the matrix: pixel space is +X reader-right and +Y up, and quads
   * land in the block's own 0..16 box.
   */
  public void renderForGui(StreetSignData data) {
    worldSkyLight = FULLBRIGHT;
    worldBlockLight = FULLBRIGHT;
    ambientSkyLight = FULLBRIGHT;
    ambientBlockLight = FULLBRIGHT;
    // The preview has no world to read redstone or the time of day from, so show the blade
    // energized whenever it is wired for light at all -- that is what the player is checking.
    lightOn = data.hasInternalLight() && data.getLightMode() != SignLightMode.OFF;
    renderSign(data, false);
  }

  /**
   * Whether the blade's internal illumination is energized right now.
   *
   * <p>NIGHT is a photocell reading the sky light actually reaching the sign minus the
   * world's current skylight subtraction, so it comes on at dusk, in a storm, and in a
   * tunnel. Use {@code calculateSkylightSubtracted}, never {@code getSkylightSubtracted}:
   * the cached field behind the getter is written once in the {@code WorldClient} constructor
   * and never updated, so client-side it forever reports the sky as it was when the player
   * joined.
   */
  private boolean resolveLightOn(StreetSignData data, TileEntityDynamicStreetSign te) {
    if (!data.hasInternalLight()) {
      return false;
    }
    SignLightMode mode = data.getLightMode();
    if (mode == SignLightMode.ON) {
      return true;
    }
    if (mode == SignLightMode.REDSTONE) {
      return te.isPowered();
    }
    if (mode == SignLightMode.NIGHT) {
      int skyLevel = te.getWorld().getLightFor(EnumSkyBlock.SKY, te.getPos())
          - te.getWorld().calculateSkylightSubtracted(1.0f);
      return skyLevel <= LIGHT_NIGHT_SKY_THRESHOLD;
    }
    return false;
  }

  // ==================================================================== layout =========

  /**
   * Every measurement the renderer and the GUI preview need, computed once so the draw pass
   * and the fit math can never disagree. All values are sign pixels in block space.
   */
  public static final class Layout {
    float signLeft;
    float signRight;
    float signTop;
    float signBottom;
    float signWidth;
    float signHeight;
    float borderInset;
    /** How far the extruded frame reaches past the panel, vertically and horizontally. */
    float frameOverhangY;
    float frameOverhangX;
    /** Front plane of the painted plates; the viewer is at smaller Z than this. */
    float faceZ;
    /** Rear plane of the ambient-lit core slab. */
    float coreBack;

    float contentCenterY;
    float contentLeft;
    float contentWidth;

    float nameCap;
    float affixCap;
    float cityCap;
    float blockCap;
    float emblemSize;
    float emblemWidth;
    float arrowSize;
    float scale;

    float textColumnWidth;
    float nameGroupWidth;
    float nameWidth;
    float prefixWidth;
    float suffixWidth;
    float cityWidth;
    float blockWidth;
    float nameCenterY;
    float cityCenterY;

    /** Topmost point of the whole assembly, hangers included -- for the preview's fit math. */
    float assemblyTop;
    /** Bottommost point of the whole assembly. */
    float assemblyBottom;
  }

  /** Measures a blade without drawing it. Safe to call off the render thread's state. */
  public Layout computeLayout(StreetSignData data) {
    Layout l = new Layout();
    l.scale = data.getTextScale();
    l.nameCap = NAME_CAP_HEIGHT * l.scale;
    l.affixCap = l.nameCap * AFFIX_CAP_FRACTION;
    l.cityCap = l.nameCap * CITY_CAP_FRACTION;
    l.blockCap = l.nameCap * BLOCK_CAP_FRACTION;
    l.emblemSize = EMBLEM_SIZE * l.scale;
    l.arrowSize = ARROW_SIZE * l.scale;

    float gapAffix = AFFIX_GAP * l.scale;
    float gapSlot = SLOT_GAP * l.scale;
    float gapCity = CITY_GAP * l.scale;

    l.nameWidth = GuideSignFontRenderer.getStringWidth(data.getStreetName(), l.nameCap);
    l.prefixWidth = data.getPrefix().isEmpty() ? 0
        : GuideSignFontRenderer.getStringWidth(data.getPrefix(), l.affixCap) + gapAffix;
    l.suffixWidth = data.getSuffix().isEmpty() ? 0
        : GuideSignFontRenderer.getStringWidth(data.getSuffix(), l.affixCap) + gapAffix;
    l.nameGroupWidth = l.prefixWidth + l.nameWidth + l.suffixWidth;
    l.cityWidth = data.hasCityText()
        ? GuideSignFontRenderer.getStringWidth(data.getCityText(), l.cityCap) : 0;
    l.textColumnWidth = Math.max(l.nameGroupWidth, l.cityWidth);

    l.blockWidth = data.hasBlockNumber()
        ? GuideSignFontRenderer.getStringWidth(data.getBlockNumber(), l.blockCap) : 0;
    l.emblemWidth = data.hasEmblem() ? emblemWidth(data, l.emblemSize) : 0;

    float contentWidth = l.textColumnWidth;
    if (data.hasBlockNumber()) {
      contentWidth += l.blockWidth + gapSlot;
    }
    if (data.hasEmblem()) {
      contentWidth += l.emblemWidth + gapSlot;
    }
    if (data.hasArrow()) {
      contentWidth += l.arrowSize + gapSlot;
    }
    l.contentWidth = contentWidth;

    float textBlockHeight = l.nameCap * TEXT_VISUAL_FACTOR
        + (data.hasCityText() ? l.cityCap * TEXT_VISUAL_FACTOR + gapCity : 0);
    float contentHeight = textBlockHeight;
    if (data.hasEmblem()) {
      contentHeight = Math.max(contentHeight, l.emblemSize);
    }
    if (data.hasArrow()) {
      contentHeight = Math.max(contentHeight, l.arrowSize);
    }

    l.borderInset = data.getBorderWidth() > 0 ? data.getBorderWidth() * BORDER_INSET : 0;
    // The extrusion wraps the painted panel from OUTSIDE, so it grows the assembly rather
    // than eating into the panel's border.
    l.frameOverhangY = data.hasExtrudedFrame() ? FRAME_RAIL : 0;
    l.frameOverhangX = data.hasExtrudedFrame() ? FRAME_END : 0;
    l.signWidth = Math.max(data.getMinWidth(),
        contentWidth + 2 * (PAD_SIDE + l.borderInset));
    l.signHeight = Math.max(data.getMinHeight(),
        contentHeight + PAD_TOP + PAD_BOTTOM + 2 * l.borderInset);

    l.signLeft = CX - l.signWidth / 2.0f;
    l.signRight = l.signLeft + l.signWidth;

    StreetSignMount mount = data.getMountType();
    if (mount == StreetSignMount.HANGING) {
      // The blade drops from its hangers rather than centering on the block, so the hangers
      // have somewhere to go.
      l.signTop = 16.0f - HANG_DROP;
      l.signBottom = l.signTop - l.signHeight;
      l.faceZ = CZ - SIGN_DEPTH / 2.0f;
      l.coreBack = 16.0f - l.faceZ - Z_CORE_FRONT;
      l.assemblyTop = 16.0f + HANGER_REACH_ABOVE;
      l.assemblyBottom = l.signBottom - l.borderInset - l.frameOverhangY;
    } else {
      l.signTop = CY + l.signHeight / 2.0f;
      l.signBottom = CY - l.signHeight / 2.0f;
      l.faceZ = 16.0f - SIGN_DEPTH;
      l.coreBack = 16.0f + 0.05f;
      l.assemblyTop = l.signTop + l.borderInset + l.frameOverhangY;
      l.assemblyBottom = l.signBottom - l.borderInset - l.frameOverhangY;
    }

    l.contentCenterY = (l.signTop + l.signBottom) / 2.0f;
    l.contentLeft = CX - contentWidth / 2.0f;

    float textBlockTop = l.contentCenterY + textBlockHeight / 2.0f;
    l.nameCenterY = textBlockTop - l.nameCap * TEXT_VISUAL_FACTOR / 2.0f;
    l.cityCenterY = textBlockTop - l.nameCap * TEXT_VISUAL_FACTOR - gapCity
        - l.cityCap * TEXT_VISUAL_FACTOR / 2.0f;
    return l;
  }

  /** Rendered width of the emblem cell -- square, except a wide 3-digit shield variant. */
  private static float emblemWidth(StreetSignData data, float emblemSize) {
    if (data.getEmblemKind() == StreetSignEmblemKind.SHIELD) {
      GuideSignShieldType type = data.getShieldType();
      if (type.usesWideVariant(data.getShieldRoute())) {
        return emblemSize * type.getWideAspect();
      }
    }
    return emblemSize;
  }

  /**
   * {@code {centerX, centerY, width, height}} of the whole assembly, hangers included, for
   * the GUI preview's fit math.
   */
  public float[] computePreviewBox(StreetSignData data) {
    Layout l = computeLayout(data);
    float width = l.signWidth + 2 * (l.borderInset + l.frameOverhangX);
    float height = l.assemblyTop - l.assemblyBottom;
    return new float[]{CX, (l.assemblyTop + l.assemblyBottom) / 2.0f, width, height};
  }

  // ==================================================================== drawing ========

  private void renderSign(StreetSignData data, boolean farLod) {
    // An illuminated blade reads at full brightness however dark the world is -- that is the
    // point of internal illumination. Only the face and legend go fullbright; the core slab,
    // the frame, and the hangers keep the ambient light stashed above.
    if (lightOn) {
      worldSkyLight = FULLBRIGHT;
      worldBlockLight = FULLBRIGHT;
    }

    Layout l = computeLayout(data);
    GuideSignColor signColor = data.getSignColor();
    boolean lightFace = signColor.isLight();
    float legendR = lightFace ? 0.06f : 0.94f;
    float legendG = lightFace ? 0.06f : 0.94f;
    float legendB = lightFace ? 0.06f : 0.92f;
    int legendTextColor = lightFace ? LEGEND_DARK : LEGEND_WHITE;

    GlStateManager.disableLighting();
    GL11.glDisable(GL11.GL_LIGHTING);
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    // Bind a 1x1 white texture rather than calling disableTexture2D -- shaders ignore the
    // global disable and would render untextured geometry with whatever was last bound.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    renderCore(l, data.getCornerStyle());
    renderFace(l, data, signColor, legendR, legendG, legendB, legendTextColor, farLod);
    if (data.isDoubleSided()) {
      // The back face is the same draw rotated 180 degrees about the block's vertical axis.
      // That is orientation-preserving, so combined with the outer mirror the legend reads
      // correctly (not mirrored) to a viewer standing behind the blade.
      GlStateManager.pushMatrix();
      GlStateManager.translate(CX, 0.0f, CZ);
      GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
      GlStateManager.translate(-CX, 0.0f, -CZ);
      renderFace(l, data, signColor, legendR, legendG, legendB, legendTextColor, farLod);
      GlStateManager.popMatrix();
    }
    if (data.hasExtrudedFrame()) {
      renderExtrudedFrame(l, data.getMountType());
    }
    if (data.getMountType() == StreetSignMount.HANGING) {
      renderHangers(l);
      if (data.hasExtrudedFrame()) {
        renderPowerCable(l);
      }
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GL11.glEnable(GL11.GL_LIGHTING);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
  }

  /**
   * The unpainted aluminum body. Drawn a hair oversize and starting just behind the painted
   * plates so it sleeves them: their side and rear faces end up inside it and only their
   * front faces are ever seen. This is what confines an illuminated blade's glow to its face
   * -- the slab always draws at ambient light, so from the side and along the top edge a lit
   * blade is as dark as the night around it.
   */
  private void renderCore(Layout l, CornerStyle corners) {
    float m = BACK_SLEEVE_MARGIN;
    List<RenderHelper.Box> core = new ArrayList<>();
    addRectBoxes(core, l.signLeft - l.borderInset - m, l.signBottom - l.borderInset - m,
        l.signRight + l.borderInset + m, l.signTop + l.borderInset + m,
        l.faceZ + Z_CORE_FRONT, l.coreBack, corners);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(core, buf, 0.55f, 0.56f, 0.58f, 1.0f, 0, 0, 0,
        ambientSkyLight, ambientBlockLight);
    tess.draw();
  }

  /** Border plate, painted face, and (unless in far LOD) the whole legend. */
  private void renderFace(Layout l, StreetSignData data, GuideSignColor signColor,
      float legendR, float legendG, float legendB, int legendTextColor, boolean farLod) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    CornerStyle corners = data.getCornerStyle();

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    if (l.borderInset > 0) {
      List<RenderHelper.Box> border = new ArrayList<>();
      addRectBoxes(border, l.signLeft - l.borderInset, l.signBottom - l.borderInset,
          l.signRight + l.borderInset, l.signTop + l.borderInset,
          l.faceZ + Z_BORDER_PLATE, l.faceZ + Z_BORDER_PLATE + LIT_FACE_DEPTH, corners);
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(border, buf, legendR, legendG, legendB, 1.0f, 0, 0, 0,
          worldSkyLight, worldBlockLight);
      tess.draw();
    }

    // The painted face sits IN FRONT of the border plate (smaller Z), never behind it and
    // never at the same depth -- coplanar faces z-fight and a face behind the border renders
    // white from the front.
    List<RenderHelper.Box> face = new ArrayList<>();
    addRectBoxes(face, l.signLeft, l.signBottom, l.signRight, l.signTop,
        l.faceZ + Z_FACE_PLATE, l.faceZ + Z_FACE_PLATE + LIT_FACE_DEPTH, corners);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(face, buf,
        signColor.getRed(), signColor.getGreen(), signColor.getBlue(), 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();

    if (farLod) {
      // Past the detail range the legend is unreadable and the font and atlas passes are the
      // expensive part; the painted body alone still reads as a street blade.
      return;
    }

    float gapSlot = SLOT_GAP * l.scale;
    float x = l.contentLeft;

    // Side slots run outward from the text column in a fixed order -- arrow outermost, then
    // emblem, then block number -- which is how a real blade reads at both ends.
    if (data.getArrowPosition() == StreetSignSlotPosition.LEFT) {
      renderArrow(data, l, x);
      x += l.arrowSize + gapSlot;
    }
    if (data.getEmblemPosition() == StreetSignSlotPosition.LEFT && data.hasEmblem()) {
      renderEmblem(data, l, x, legendTextColor);
      x += l.emblemWidth + gapSlot;
    }
    if (data.getBlockPosition() == StreetSignSlotPosition.LEFT && data.hasBlockNumber()) {
      renderBlockNumber(data, l, x, legendTextColor);
      x += l.blockWidth + gapSlot;
    }

    renderTextColumn(data, l, x, legendTextColor);
    x += l.textColumnWidth;

    if (data.getBlockPosition() == StreetSignSlotPosition.RIGHT && data.hasBlockNumber()) {
      x += gapSlot;
      renderBlockNumber(data, l, x, legendTextColor);
      x += l.blockWidth;
    }
    if (data.getEmblemPosition() == StreetSignSlotPosition.RIGHT && data.hasEmblem()) {
      x += gapSlot;
      renderEmblem(data, l, x, legendTextColor);
      x += l.emblemWidth;
    }
    if (data.getArrowPosition() == StreetSignSlotPosition.RIGHT) {
      x += gapSlot;
      renderArrow(data, l, x);
    }

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /** Prefix, street name, suffix on one line, with the optional city line centered under it. */
  private void renderTextColumn(StreetSignData data, Layout l, float columnLeft, int color) {
    float gapAffix = AFFIX_GAP * l.scale;
    float groupLeft = columnLeft + (l.textColumnWidth - l.nameGroupWidth) / 2.0f;
    float z = l.faceZ + Z_LEGEND;
    // Hanging the affixes from the name's cap line gives a blade the raised "W ... RD" look;
    // dropping them to its baseline gives the equally common flush style.
    float affixCenterY = alignToNameCap(l, data.getAffixVertical(), l.affixCap);

    GlStateManager.depthMask(false);
    float pen = groupLeft;
    if (!data.getPrefix().isEmpty()) {
      GuideSignFontRenderer.drawString(data.getPrefix(), pen, affixCenterY, z, l.affixCap,
          color, worldSkyLight, worldBlockLight);
      pen += l.prefixWidth;
    }
    GuideSignFontRenderer.drawString(data.getStreetName(), pen, l.nameCenterY, z, l.nameCap,
        color, worldSkyLight, worldBlockLight);
    pen += l.nameWidth;
    if (!data.getSuffix().isEmpty()) {
      GuideSignFontRenderer.drawString(data.getSuffix(), pen + gapAffix, affixCenterY, z,
          l.affixCap, color, worldSkyLight, worldBlockLight);
    }
    if (data.hasCityText()) {
      float cityLeft = columnLeft + (l.textColumnWidth - l.cityWidth) / 2.0f;
      GuideSignFontRenderer.drawString(data.getCityText(), cityLeft, l.cityCenterY, z,
          l.cityCap, color, worldSkyLight, worldBlockLight);
    }
    GlStateManager.depthMask(true);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /**
   * Center Y for a small legend element -- a prefix, a suffix, the block number -- aligned
   * against the street name's cap box. TOP puts its cap top on the name's cap line, BOTTOM
   * puts its baseline on the name's baseline, MIDDLE centers it on the name.
   *
   * <p>Everything small on a blade aligns to the NAME, never to the content band. The band is
   * sized by the tallest thing in the row, which is normally the emblem at half again the
   * name's cap height, so an element aligned to the band's edge sits visibly clear of the
   * name's own top or bottom instead of lining up with it.
   */
  private static float alignToNameCap(Layout l, StreetSignVerticalPos vertical,
      float capHeight) {
    if (vertical == StreetSignVerticalPos.BOTTOM) {
      return l.nameCenterY - l.nameCap / 2.0f + capHeight / 2.0f;
    }
    if (vertical == StreetSignVerticalPos.MIDDLE) {
      return l.nameCenterY;
    }
    return l.nameCenterY + l.nameCap / 2.0f - capHeight / 2.0f;
  }

  private void renderBlockNumber(StreetSignData data, Layout l, float x, int color) {
    float centerY = alignToNameCap(l, data.getBlockVertical(), l.blockCap);
    GlStateManager.depthMask(false);
    GuideSignFontRenderer.drawString(data.getBlockNumber(), x, centerY, l.faceZ + Z_LEGEND,
        l.blockCap, color, worldSkyLight, worldBlockLight);
    GlStateManager.depthMask(true);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /** The emblem slot: a route shield with its number drawn over it, or a civic logo cell. */
  private void renderEmblem(StreetSignData data, Layout l, float x, int legendTextColor) {
    boolean isShield = data.getEmblemKind() == StreetSignEmblemKind.SHIELD;
    GuideSignShieldType shieldType = data.getShieldType();
    boolean wide = isShield && shieldType.usesWideVariant(data.getShieldRoute());

    float[] uv;
    if (isShield) {
      uv = wide ? GuideSignAtlas.getShieldWideUV(shieldType)
          : GuideSignAtlas.getShieldUV(shieldType);
    } else {
      uv = GuideSignAtlas.getCellUV(data.getLogoType().getAtlasCol(),
          data.getLogoType().getAtlasRow());
    }

    float halfWidth = l.emblemWidth / 2.0f;
    float halfHeight = l.emblemSize / 2.0f;
    float centerX = x + halfWidth;
    float centerY = l.contentCenterY;
    float z = l.faceZ + Z_EMBLEM;

    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // u0 on the left edge: the enclosing transform already un-mirrors pixel space.
    atlasVertex(buf, centerX + halfWidth, centerY + halfHeight, z, uv[2], uv[1]);
    atlasVertex(buf, centerX - halfWidth, centerY + halfHeight, z, uv[0], uv[1]);
    atlasVertex(buf, centerX - halfWidth, centerY - halfHeight, z, uv[0], uv[3]);
    atlasVertex(buf, centerX + halfWidth, centerY - halfHeight, z, uv[2], uv[3]);
    tess.draw();

    String route = data.getShieldRoute();
    if (isShield && !route.isEmpty()) {
      // Shrink to fit so a long route number stays inside the shield's legend area instead
      // of spilling past its outline.
      float capPx = l.emblemSize * ROUTE_CAP_FRACTION;
      float available = l.emblemWidth * shieldType.getRouteTextMaxFraction();
      float width = GuideSignFontRenderer.getStringWidth(route, capPx);
      if (width > available) {
        capPx *= available / width;
        width = available;
      }
      GlStateManager.depthMask(false);
      GuideSignFontRenderer.drawString(route, centerX - width / 2.0f, centerY,
          l.faceZ + Z_ROUTE_TEXT, capPx, shieldType.getRouteTextColor(),
          worldSkyLight, worldBlockLight);
      GlStateManager.depthMask(true);
    }
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  private void renderArrow(StreetSignData data, Layout l, float x) {
    float[] uv = GuideSignAtlas.getArrowUV(data.getArrowType());
    float half = l.arrowSize / 2.0f;
    float centerX = x + half;
    float centerY = l.contentCenterY;
    float z = l.faceZ + Z_EMBLEM;

    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    atlasVertex(buf, centerX + half, centerY + half, z, uv[2], uv[1]);
    atlasVertex(buf, centerX - half, centerY + half, z, uv[0], uv[1]);
    atlasVertex(buf, centerX - half, centerY - half, z, uv[0], uv[3]);
    atlasVertex(buf, centerX + half, centerY - half, z, uv[2], uv[3]);
    tess.draw();
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /**
   * The dark aluminum extrusion an internally-lit blade is built in: top and bottom rails
   * welded to cast end pieces, standing proud of the painted face so the panel reads as
   * recessed inside it. Structural metal, so it draws at ambient light -- it must not glow
   * along with the face.
   */
  private void renderExtrudedFrame(Layout l, StreetSignMount mount) {
    float outerLeft = l.signLeft - l.borderInset;
    float outerRight = l.signRight + l.borderInset;
    float outerTop = l.signTop + l.borderInset;
    float outerBottom = l.signBottom - l.borderInset;
    float front = l.faceZ + Z_FRAME;
    // A double-sided blade carries painted plates on BOTH faces (the back pass mirrors them
    // about the block center), so the extrusion has to reach symmetrically past both or the
    // rear face renders unframed and the frame looks like it stops halfway.
    float back = mount == StreetSignMount.HANGING ? 16.0f - front : l.coreBack + 0.05f;

    List<RenderHelper.Box> frame = new ArrayList<>();
    // Rails and end castings sit OUTSIDE the painted panel, overlapping it only by
    // JOINT_OVERLAP to close the seam. Covering the panel instead would hide the white
    // legend border, which on a real extruded blade stays visible inside the frame.
    frame.add(new RenderHelper.Box(
        new float[]{outerLeft - FRAME_END, outerTop - FRAME_SEAM, front},
        new float[]{outerRight + FRAME_END, outerTop + FRAME_RAIL, back}));
    frame.add(new RenderHelper.Box(
        new float[]{outerLeft - FRAME_END, outerBottom - FRAME_RAIL, front},
        new float[]{outerRight + FRAME_END, outerBottom + FRAME_SEAM, back}));
    frame.add(new RenderHelper.Box(
        new float[]{outerLeft - FRAME_END, outerBottom - FRAME_RAIL, front},
        new float[]{outerLeft + FRAME_SEAM, outerTop + FRAME_RAIL, back}));
    frame.add(new RenderHelper.Box(
        new float[]{outerRight - FRAME_SEAM, outerBottom - FRAME_RAIL, front},
        new float[]{outerRight + FRAME_END, outerTop + FRAME_RAIL, back}));

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(frame, buf, 0.22f, 0.22f, 0.24f, 1.0f, 0, 0, 0,
        ambientSkyLight, ambientBlockLight);
    tess.draw();
  }

  /**
   * The two hangers a mast-arm blade swings from: a shoe on the blade's top edge, a rod, and
   * a clamp at the block's top where the mast arm would be. Structural metal, drawn at
   * ambient light so it stays dark around a glowing blade.
   */
  private void renderHangers(Layout l) {
    float inset = Math.max(HANGER_CLAMP_WIDTH, l.signWidth * HANGER_INSET_FRACTION);
    float[] centers = {l.signLeft + inset, l.signRight - inset};
    // On a very narrow blade the two hangers would collide; collapse to one down the middle.
    if (centers[1] - centers[0] < HANGER_CLAMP_WIDTH * 1.5f) {
      centers = new float[]{CX};
    }

    float shoeBottom = l.signTop + l.borderInset + l.frameOverhangY - JOINT_OVERLAP;
    float shoeTop = shoeBottom + HANGER_SHOE_HEIGHT;
    // The clamp grips at the TOP of the run, not at the block boundary, so whatever the run
    // reaches is what it appears to hang from.
    float hangerTop = 16.0f + HANGER_REACH_ABOVE;
    float clampBottom = hangerTop - HANGER_CLAMP_HEIGHT;

    List<RenderHelper.Box> parts = new ArrayList<>();
    for (float hx : centers) {
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_SHOE_WIDTH / 2, shoeBottom - HANGER_SHOE_HEIGHT,
              CZ - HANGER_SHOE_DEPTH / 2},
          new float[]{hx + HANGER_SHOE_WIDTH / 2, shoeTop, CZ + HANGER_SHOE_DEPTH / 2}));
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_ROD_WIDTH / 2, shoeTop - JOINT_OVERLAP,
              CZ - HANGER_ROD_WIDTH / 2},
          new float[]{hx + HANGER_ROD_WIDTH / 2, clampBottom + JOINT_OVERLAP,
              CZ + HANGER_ROD_WIDTH / 2}));
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_CLAMP_WIDTH / 2, clampBottom,
              CZ - HANGER_CLAMP_DEPTH / 2},
          new float[]{hx + HANGER_CLAMP_WIDTH / 2, hangerTop, CZ + HANGER_CLAMP_DEPTH / 2}));
    }

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(parts, buf, 0.18f, 0.18f, 0.20f, 1.0f, 0, 0, 0,
        ambientSkyLight, ambientBlockLight);
    tess.draw();
  }

  /**
   * The power feed: a cable leaving the top of the extruded frame's end casting and running up
   * to the same height the hangers reach, so it disappears into whatever the blade hangs from
   * rather than stopping in mid-air.
   *
   * <p>Drawn only for a framed hanging blade, which is the only configuration with anything to
   * feed: the frame is the housing of an internally-lit sign, and a flat blade's conduit runs
   * inside whatever it is bolted to, where nobody would see it. It leaves from beyond the
   * blade's end rather than from the top rail, both because that is where the real ones are
   * dressed and because there it is never hidden behind the panel.
   *
   * <p>The run bellies away from the blade in the middle and returns to the same x at both
   * ends, which is what a slack cable between two fixed points does; a dead-straight one reads
   * as a rod. Ambient-lit like the rest of the metalwork, so it stays dark against a glowing
   * blade at night.
   */
  private void renderPowerCable(Layout l) {
    float baseX = l.signRight + l.borderInset + FRAME_END * CABLE_END_FRACTION;
    float baseY = l.signTop + l.borderInset + l.frameOverhangY - JOINT_OVERLAP;
    float topY = 16.0f + HANGER_REACH_ABOVE;
    float half = CABLE_THICKNESS / 2.0f;

    List<RenderHelper.Box> cable = new ArrayList<>();
    float previousX = baseX;
    float previousY = baseY;
    for (int i = 1; i <= CABLE_SEGMENTS; i++) {
      float t = (float) i / CABLE_SEGMENTS;
      float y = baseY + (topY - baseY) * t;
      float x = baseX + CABLE_BOW * (float) Math.sin(Math.PI * t);
      // Each segment spans both endpoints' x, so consecutive boxes overlap along the bow and
      // the run reads as one cable rather than a ladder of disconnected pieces.
      cable.add(new RenderHelper.Box(
          new float[]{Math.min(previousX, x) - half, previousY, CZ - half},
          new float[]{Math.max(previousX, x) + half, y, CZ + half}));
      previousX = x;
      previousY = y;
    }

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(cable, buf, 0.11f, 0.11f, 0.12f, 1.0f, 0, 0, 0,
        ambientSkyLight, ambientBlockLight);
    tess.draw();
  }

  /**
   * Voxel approximation of a rounded corner: a horizontal and a vertical strip that overlap
   * in the middle and leave a {@code CORNER_STEP} square notch at each outer corner. Falls
   * back to one box when the style is SHARP or the rectangle is too small to notch.
   */
  private void addRectBoxes(List<RenderHelper.Box> boxes, float l, float b, float r, float t,
      float z1, float z2, CornerStyle style) {
    boolean canNotch = style == CornerStyle.ROUND
        && (r - l) > 2 * CORNER_STEP
        && (t - b) > 2 * CORNER_STEP;
    if (!canNotch) {
      boxes.add(new RenderHelper.Box(new float[]{l, b, z1}, new float[]{r, t, z2}));
      return;
    }
    float s = CORNER_STEP;
    boxes.add(new RenderHelper.Box(new float[]{l, b + s, z1}, new float[]{r, t - s, z2}));
    boxes.add(new RenderHelper.Box(new float[]{l + s, b, z1}, new float[]{r - s, t, z2}));
  }

  private void atlasVertex(BufferBuilder buf, float x, float y, float z, float u, float v) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
        .lightmap(worldSkyLight, worldBlockLight).endVertex();
  }
}
