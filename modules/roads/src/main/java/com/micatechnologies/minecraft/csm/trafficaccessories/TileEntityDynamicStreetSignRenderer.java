package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignAtlas;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.SignLightMode;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignEmblemKind;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignLegend;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignSlotPosition;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignVerticalPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
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
 * the block behind it exactly like a guide sign. A hanging blade is centered in the block's
 * depth, hangs below hardware that reaches half a block ABOVE its own block so it can grip a
 * top slab, and can carry its legend on both faces -- the back face is the same draw inside a
 * 180 degree Y rotation about the block center, which is orientation-preserving and so reads
 * correctly (not mirrored) from behind.
 *
 * <p>The two hanging styles differ only in that hardware. {@link StreetSignMount#HANGING} runs
 * two independent hangers the full way up. {@link StreetSignMount#HANGING_BRACKET} hangs the
 * blade off a horizontal support beam on two short links and carries the beam on a single
 * centre drop, so the assembly needs one attachment point instead of two.
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

  /**
   * Where the sign post's axis runs through the block, from the shared post model: the
   * five nested bars are centred on x 8 and span z 0.5 to 3.5. A post-top blade sits on
   * that axis, not on the block's centre, and its crossing partner turns about it.
   */
  private static final float POST_X = 8.0f;
  private static final float POST_Z = 2.0f;

  /**
   * How much of a mast-arm blade's size a post-top blade is drawn at.
   *
   * <p>The layout is measured for a blade hung over a road, which is most of a block tall
   * -- on a sign post, beside a one-block STOP sign, that is enormous. A real street blade
   * is six inches deep against a thirty-inch sign, so the assembly is scaled about the
   * post top instead of the panel being re-measured: the panel, its border, its frame, the
   * legend and the bracket all shrink together and keep the proportions the hanging blade
   * was drawn with, which re-tuning a dozen constants would not have.</p>
   */
  private static final float POST_TOP_SCALE = 0.38f;
  /**
   * How far in front of the post's axis a blade's panel is centred, measured where it
   * ends up -- after {@link #POST_TOP_SCALE}.
   *
   * <p>The post's bars run from z 0.5 to 3.5 about an axis at z 2, and the panel is
   * {@code SIGN_DEPTH * POST_TOP_SCALE} thick, so this is the least that keeps the post
   * from standing through the blade. It does not need to be more: the blade's two faces
   * straddle the post (see the mirror in {@code renderSign}), so the post between them is
   * what tells one from the other, exactly as it is on a back-to-back pair of signs.</p>
   */
  private static final float POST_BLADE_CLEARANCE = 1.95f;

  /**
   * Where a post-top blade's panel is centred, in front of the post rather than on its
   * axis. Centred on the axis the post's own bars stand in front of the legend and read
   * as a bar painted through the street's name; the crossing blade turns about the post
   * all the same, which puts its panel the same distance in front of the post along the
   * way <em>it</em> is read, so each ends up on its own side of the post -- one offset,
   * correct for both.
   *
   * <p>Divided by the scale, because the whole assembly is shrunk about the post top
   * afterwards and a clearance written in these units would be shrunk with it. Set in
   * plain model units first, it looked right in the layout and put both panels back
   * inside the post in the world, where the post cut through each blade in turn.</p>
   */
  private static final float POST_BLADE_Z = POST_Z - POST_BLADE_CLEARANCE / POST_TOP_SCALE;


  /** The height the post-top assembly is scaled about: the top of the block. */
  private static final float POST_TOP_Y = 16.0f;

  /** Gap between a post-top pair's two blades, and how far the upper one clears the top. */
  private static final float CROSS_GAP = 0.75f;
  private static final float POST_TOP_CLEAR = 0.5f;

  /** Bracket plate: how far it reaches each side of the post, and how thick and tall. */
  private static final float BRACKET_REACH = 2.6f;
  private static final float BRACKET_THICK = 0.55f;
  private static final float BRACKET_PAD = 0.9f;

  /**
   * Marks the display lists of a post-top pair's crossing blade, which is drawn from the
   * same position under a quarter turn. Without it the two passes would share a cache
   * entry and each would replay the other's geometry.
   */
  private static final long CROSS_BLADE_KEY = 1L << 34;
  /**
   * Clear space between the two blades of a stacked pair, outer edge to outer edge (border and
   * frame included). A real two-name assembly hangs its blades a few inches apart on one set of
   * hardware; about a fifth of a blade's height reads as one assembly rather than as either two
   * unrelated signs or one sign with a stripe through it, and leaves the links between them a
   * visible run of rod. Package-private for the layout test.
   */
  static final float BLADE_GAP = 3.0f;

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

  // ---- Bracket hanger (HANGING_BRACKET) ----------------------------------------------------
  /** Vertical clearance between the blade's top edge and the underside of the support beam. */
  private static final float BEAM_GAP = 2.6f;
  /** Square section of the horizontal support beam. */
  private static final float BEAM_THICKNESS = 1.4f;
  /**
   * How far the beam runs past each end of the blade. It has to overhang visibly: a beam
   * flush with the blade reads as part of the panel's frame rather than as the thing the
   * panel is hanging from.
   */
  private static final float BEAM_OVERHANG = 3.2f;
  /** Width of the cap closing each end of the beam, and how far it stands proud of it. */
  private static final float BEAM_CAP_WIDTH = 0.8f;
  private static final float BEAM_CAP_GROW = 0.25f;
  /** The collar clamping each blade link onto the beam. */
  private static final float BEAM_COLLAR_WIDTH = 2.2f;
  private static final float BEAM_COLLAR_GROW = 0.4f;
  /**
   * Fraction of the blade's width the links are inset from each end -- well outboard of the
   * two-hanger mount's HANGER_INSET_FRACTION. These links are short fittings rather than a
   * full-height run, so at that mount's inset they bunch in toward the centre drop instead of
   * reading as carrying the blade. Out near the ends is also where the real ones sit.
   */
  private static final float BRACKET_LINK_INSET_FRACTION = 0.12f;
  /** Square section of the single centre drop that carries the whole assembly. */
  private static final float DROP_POST_WIDTH = 1.3f;
  /** The saddle casting where the centre drop meets the beam. */
  private static final float DROP_SADDLE_WIDTH = 3.0f;
  private static final float DROP_SADDLE_HEIGHT = 1.5f;

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
  /**
   * The blade's structural aluminium: the core slab, and the frame, hangers and power cable. All
   * of it draws against the white pixel at ambient light and never animates, so it compiles once
   * and replays. Measured at the dense verification pose, the whole street sign renderer was 20.5%
   * of frame time while its legend detail was only 2.3% -- the structure is where the time went.
   *
   * <p>Two caches rather than one because the face draws between them and the order matters: the
   * core sleeves the painted plates from behind, and the frame sits in front of them.</p>
   */
  private static final CsmDisplayListCache CORE_LISTS =
      new CsmDisplayListCache("street_sign_core");

  private static final CsmDisplayListCache FRAME_LISTS =
      new CsmDisplayListCache("street_sign_frame");

  /**
   * The painted face and its border plate. Static like the structure, but keyed on the illumination
   * state as well: a lit blade draws its face at full brightness, and in
   * {@link SignLightMode#NIGHT} that follows the sky without the tile entity ever being marked
   * dirty. One list serves both sides of a double-sided blade -- the back is the same geometry
   * under a rotated matrix, and a display list does not capture the matrix.
   */
  private static final CsmDisplayListCache FACE_LISTS =
      new CsmDisplayListCache("street_sign_face");

  /**
   * Releases the compiled geometry for one blade.
   *
   * @param pos the block position
   */
  public static void cleanupDisplayList(BlockPos pos) {
    CORE_LISTS.invalidate(pos);
    FRAME_LISTS.invalidate(pos);
    FACE_LISTS.invalidate(pos);
  }

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

    IBlockState blockState = te.getWorld().getBlockState(te.getPos());

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.translate(0.5, 0.0, 0.5);

    // The panel is modelled on the block's +Z side reading toward -Z, so an unrotated draw faces
    // NORTH. Assigning 0 degrees to SOUTH therefore rendered the north/south pair backwards --
    // a sign set to face south showed its blank back to a viewer standing south of it -- while
    // east and west, being a quarter turn either side, came out right and hid the error.
    float rotY = facingRotation(blockState);
    GlStateManager.rotate(rotY, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    if (data.getMountType().isPostTop()) {
      // About the post top, so the blades stay bolted to it however far they shrink.
      GlStateManager.translate(POST_X, POST_TOP_Y, POST_Z);
      GlStateManager.scale(POST_TOP_SCALE, POST_TOP_SCALE, POST_TOP_SCALE);
      GlStateManager.translate(-POST_X, -POST_TOP_Y, -POST_Z);
    }
    // Un-mirror pixel space so +X below is the reader's right. See the class notes.
    GlStateManager.translate(16.0f, 0.0f, 0.0f);
    GlStateManager.scale(-1.0f, 1.0f, 1.0f);

    // x/y/z are camera-relative, so this is the squared camera distance.
    if (CsmRenderToggles.skipStreetSign) {
      GlStateManager.popMatrix();
      return;
    }
    boolean farLod = x * x + y * y + z * z > LOD_FULL_DETAIL_DIST_SQ
        || CsmRenderToggles.streetSignForceFarLod;
    renderSign(data, farLod, te.getPos(), te.isStateDirty(), combinedLight);
    te.clearStateDirty();

    GlStateManager.popMatrix();
  }

  /**
   * Draws the blade flat for the GUI preview, fullbright, through the exact world render
   * path. The caller owns the matrix: pixel space is +X reader-right and +Y up, and quads
   * land in the block's own 0..16 box.
   */
  /**
   * How far to turn the assembly for the block's facing.
   *
   * <p>Two kinds of block are drawn by this renderer and they do not carry the same facing
   * property: the dynamic street sign has the vanilla four-way {@code BlockHorizontal.FACING},
   * and the street name blades are road signs, which face eight ways. Reading one and
   * assuming the other throws out of {@code getValue} -- the blades crashed the first time
   * one was looked at. The two agree on the angles they share, so the eight-way enum's own
   * {@link DirectionEight#getRotationDegrees()} is the answer for both.</p>
   *
   * @param state the block's state
   *
   * @return the rotation about Y, in degrees
   */
  private static float facingRotation(IBlockState state) {
    if (state.getPropertyKeys().contains(AbstractBlockRotatableHZEight.FACING)) {
      return state.getValue(AbstractBlockRotatableHZEight.FACING).getRotationDegrees();
    }
    if (!state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
      return 0.0f;
    }
    switch (state.getValue(BlockHorizontal.FACING)) {
      case WEST:
        return 90.0f;
      case SOUTH:
        return 180.0f;
      case EAST:
        return 270.0f;
      default:
        // The panel is modelled on the block's +Z side reading toward -Z, so an unrotated
        // draw faces NORTH. Assigning 0 to SOUTH rendered the north/south pair backwards,
        // while east and west, a quarter turn either side, came out right and hid it.
        return 0.0f;
    }
  }
  public void renderForGui(StreetSignData data) {
    worldSkyLight = FULLBRIGHT;
    worldBlockLight = FULLBRIGHT;
    ambientSkyLight = FULLBRIGHT;
    ambientBlockLight = FULLBRIGHT;
    // The preview has no world to read redstone or the time of day from, so show the blade
    // energized whenever it is wired for light at all -- that is what the player is checking.
    lightOn = data.hasInternalLight() && data.getLightMode() != SignLightMode.OFF;
    renderSign(data, false, null, true, 0);
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
    /**
     * How far the mount's own hardware reaches past everything else horizontally -- the
     * bracket mount's support beam overhangs both ends of the blade. Zero for the mounts whose
     * hardware stays within the panel's width.
     */
    float mountOverhangX;
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

    /** Height of the name line plus the city line under it, gaps included. */
    float textBlockHeight;

    /** Topmost point of the whole assembly, hangers included -- for the preview's fit math. */
    float assemblyTop;
    /**
     * Bottommost point of the whole assembly -- the lower blade's bottom edge when there are
     * two. Only meaningful on the top blade's layout.
     */
    float assemblyBottom;

    /** What this blade is lettered with. */
    StreetSignLegend legend;
    /**
     * The second blade hung below this one, sharing its width and height, or null for a single
     * blade. Only the top blade's layout carries one; the lower blade's own is always null.
     */
    Layout lower;

    /**
     * Set on a post-top pair, where the second blade crosses this one at a right angle
     * instead of hanging below it. It is drawn by a second pass under a quarter turn, so
     * every pass that walks {@link #blades()} must see one blade, not two.
     */
    boolean crossed;

    /** The blades this pass draws: this one, and a stacked partner if it has one. */
    Layout[] blades() {
      return lower == null || crossed ? new Layout[]{this} : new Layout[]{this, lower};
    }
  }

  /**
   * Measures a sign -- one blade, or a stacked pair -- without drawing it. Safe to call off the
   * render thread's state. Returns the top blade's layout; a second blade is on {@link
   * Layout#lower}.
   *
   * <p>A stacked pair is sized as one: each blade is measured on its own content, then both
   * take the wider of the two widths and the taller of the two heights. Content-driven sizing
   * per blade is exactly what made two separately placed blades disagree, and one block owning
   * both is what lets the sizes be settled once.
   */
  public Layout computeLayout(StreetSignData data) {
    Layout top = measureBlade(data, data);
    Layout lower = data.hasLowerBlade() ? measureBlade(data, data.getLowerBlade()) : null;
    if (lower != null) {
      float width = Math.max(top.signWidth, lower.signWidth);
      float height = Math.max(top.signHeight, lower.signHeight);
      top.signWidth = width;
      lower.signWidth = width;
      top.signHeight = height;
      lower.signHeight = height;
    }
    // Border and frame are shared style, so both blades reach the same distance past their
    // painted panel at each edge.
    float edge = top.borderInset + top.frameOverhangY;

    StreetSignMount mount = data.getMountType();
    if (mount.isPostTop()) {
      // On the post's axis, readable from both sides, with the blade in the top of the
      // block so the post carries it the way a real bracket does. A pair does not stack:
      // the second blade crosses the first at a right angle and sits just above it, which
      // is why it is placed here and drawn by its own pass rather than walked with the
      // first. The one the player letters first is the lower of the two, as it is on the
      // street -- the crossing street's blade rides over it.
      top.faceZ = POST_BLADE_Z - SIGN_DEPTH / 2.0f;
      top.coreBack = 2.0f * POST_BLADE_Z - top.faceZ - Z_CORE_FRONT;
      top.crossed = lower != null;
      float upperTop = 16.0f - POST_TOP_CLEAR;
      if (lower == null) {
        top.signTop = upperTop;
      } else {
        lower.faceZ = top.faceZ;
        lower.coreBack = top.coreBack;
        lower.signTop = upperTop;
        lower.crossed = true;
        placeBlade(lower);
        top.signTop = lower.signBottom - 2 * edge - CROSS_GAP;
      }
      placeBlade(top);
      top.assemblyTop = upperTop + edge;
      top.assemblyBottom = top.signBottom - edge;
      top.lower = lower;
      return top;
    }
    if (mount.isHanging()) {
      // The blade drops from its hardware rather than centering on the block, so the hangers
      // have somewhere to go. Both hanging styles drop by the same amount, so switching
      // between them swaps the hardware without moving the panel. A second blade hangs below
      // the first and moves nothing above it: the hangers still grip the top blade only.
      top.signTop = 16.0f - HANG_DROP;
      top.faceZ = CZ - SIGN_DEPTH / 2.0f;
      top.coreBack = 16.0f - top.faceZ - Z_CORE_FRONT;
      top.assemblyTop = 16.0f + HANGER_REACH_ABOVE;
      if (mount == StreetSignMount.HANGING_BRACKET) {
        top.mountOverhangX = BEAM_OVERHANG;
        // A deeply bordered and framed blade can push the beam most of the way to the top of
        // the run on its own. Take the whole stack as the floor so the centre drop always has
        // somewhere to go: without it the post's box inverts and renders inside out.
        top.assemblyTop = Math.max(top.assemblyTop,
            top.signTop + edge + BEAM_GAP + BEAM_THICKNESS
                + DROP_SADDLE_HEIGHT + HANGER_CLAMP_HEIGHT + JOINT_OVERLAP);
      }
    } else {
      // Centered on the block -- a stacked pair centers as a whole, so the top blade rises by
      // half of what the lower one adds.
      float stackRise = lower == null ? 0 : (top.signHeight + 2 * edge + BLADE_GAP) / 2.0f;
      top.signTop = CY + top.signHeight / 2.0f + stackRise;
      top.faceZ = 16.0f - SIGN_DEPTH;
      top.coreBack = 16.0f + 0.05f;
      top.assemblyTop = top.signTop + edge;
    }
    placeBlade(top);

    Layout bottom = top;
    if (lower != null) {
      lower.faceZ = top.faceZ;
      lower.coreBack = top.coreBack;
      lower.mountOverhangX = top.mountOverhangX;
      lower.signTop = top.signBottom - 2 * edge - BLADE_GAP;
      placeBlade(lower);
      top.lower = lower;
      bottom = lower;
    }
    top.assemblyBottom = bottom.signBottom - edge;
    return top;
  }

  /**
   * Everything about one blade that depends only on its own content: the legend metrics, the
   * side slots, and its natural panel size. Placement is left to {@link #placeBlade}, after a
   * stacked pair has agreed a shared size.
   */
  private static Layout measureBlade(StreetSignData data, StreetSignLegend legend) {
    Layout l = new Layout();
    l.legend = legend;
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

    l.nameWidth = GuideSignFontRenderer.getStringWidth(legend.getStreetName(), l.nameCap);
    l.prefixWidth = legend.getPrefix().isEmpty() ? 0
        : GuideSignFontRenderer.getStringWidth(legend.getPrefix(), l.affixCap) + gapAffix;
    l.suffixWidth = legend.getSuffix().isEmpty() ? 0
        : GuideSignFontRenderer.getStringWidth(legend.getSuffix(), l.affixCap) + gapAffix;
    l.nameGroupWidth = l.prefixWidth + l.nameWidth + l.suffixWidth;
    l.cityWidth = legend.hasCityText()
        ? GuideSignFontRenderer.getStringWidth(legend.getCityText(), l.cityCap) : 0;
    l.textColumnWidth = Math.max(l.nameGroupWidth, l.cityWidth);

    l.blockWidth = legend.hasBlockNumber()
        ? GuideSignFontRenderer.getStringWidth(legend.getBlockNumber(), l.blockCap) : 0;
    l.emblemWidth = legend.hasEmblem() ? emblemWidth(legend, l.emblemSize) : 0;

    float contentWidth = l.textColumnWidth;
    if (legend.hasBlockNumber()) {
      contentWidth += l.blockWidth + gapSlot;
    }
    if (legend.hasEmblem()) {
      contentWidth += l.emblemWidth + gapSlot;
    }
    if (legend.hasArrow()) {
      contentWidth += l.arrowSize + gapSlot;
    }
    l.contentWidth = contentWidth;

    l.textBlockHeight = l.nameCap * TEXT_VISUAL_FACTOR
        + (legend.hasCityText() ? l.cityCap * TEXT_VISUAL_FACTOR + gapCity : 0);
    float contentHeight = l.textBlockHeight;
    if (legend.hasEmblem()) {
      contentHeight = Math.max(contentHeight, l.emblemSize);
    }
    if (legend.hasArrow()) {
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
    return l;
  }

  /**
   * Resolves a measured blade's position from its {@code signTop} and its (possibly shared)
   * size: the panel edges and where the legend sits inside it. Content stays centered in any
   * surplus a floor or a wider partner blade leaves.
   */
  private static void placeBlade(Layout l) {
    l.signLeft = CX - l.signWidth / 2.0f;
    l.signRight = l.signLeft + l.signWidth;
    l.signBottom = l.signTop - l.signHeight;

    l.contentCenterY = (l.signTop + l.signBottom) / 2.0f;
    l.contentLeft = CX - l.contentWidth / 2.0f;

    float gapCity = CITY_GAP * l.scale;
    float textBlockTop = l.contentCenterY + l.textBlockHeight / 2.0f;
    l.nameCenterY = textBlockTop - l.nameCap * TEXT_VISUAL_FACTOR / 2.0f;
    l.cityCenterY = textBlockTop - l.nameCap * TEXT_VISUAL_FACTOR - gapCity
        - l.cityCap * TEXT_VISUAL_FACTOR / 2.0f;
  }

  /** Rendered width of the emblem cell -- square, except a wide 3-digit shield variant. */
  private static float emblemWidth(StreetSignLegend legend, float emblemSize) {
    if (legend.getEmblemKind() == StreetSignEmblemKind.SHIELD) {
      GuideSignShieldType type = legend.getShieldType();
      if (type.usesWideVariant(legend.getShieldRoute())) {
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
    float width = l.signWidth + 2 * (l.borderInset + l.frameOverhangX + l.mountOverhangX);
    float height = l.assemblyTop - l.assemblyBottom;
    return new float[]{CX, (l.assemblyTop + l.assemblyBottom) / 2.0f, width, height};
  }

  // ==================================================================== drawing ========

  private void renderSign(StreetSignData data, boolean farLod, BlockPos pos,
      boolean stateDirty, int combinedLight) {
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

    // Clearing the cache is the whole position's, so it happens once here rather than in
    // renderAssembly, which a post-top pair runs twice.
    if (stateDirty && pos != null) {
      cleanupDisplayList(pos);
    }
    renderAssembly(l, data, signColor, legendR, legendG, legendB, legendTextColor,
        farLod, pos, combinedLight, 0L);
    if (data.getMountType().isPostTop() && l.lower != null) {
      // The crossing blade: the same draw, a quarter turn about the post's axis. It is a
      // separate pass rather than more geometry in the first because a display list is
      // compiled once and replayed, and these two differ by a matrix, not by vertices.
      GlStateManager.pushMatrix();
      GlStateManager.translate(POST_X, 0.0f, POST_Z);
      GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
      GlStateManager.translate(-POST_X, 0.0f, -POST_Z);
      renderAssembly(l.lower, data, signColor, legendR, legendG, legendB, legendTextColor,
          farLod, pos, combinedLight, CROSS_BLADE_KEY);
      GlStateManager.popMatrix();
    }
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GL11.glEnable(GL11.GL_LIGHTING);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
  }

  /**
   * One blade and its hardware, drawn where the model-view matrix currently is. Split out of
   * {@link #renderSign} so a post-top pair's crossing blade can be the same draw under a
   * quarter turn; {@code keyBias} keeps the two passes' display lists apart.
   *
   * @param l       the blade's resolved layout
   * @param data    the sign's configuration, which both blades of a pair share
   * @param keyBias distinguishes this pass's cached lists from the other blade's
   */
  private void renderAssembly(Layout l, StreetSignData data, GuideSignColor signColor,
      float legendR, float legendG, float legendB, int legendTextColor, boolean farLod,
      BlockPos pos, int combinedLight, long keyBias) {
    // The structural passes below compile into display lists keyed on the block light, which is
    // the only input to them that changes without the tile entity being marked dirty (they draw at
    // ambient light, so an illuminated blade does not affect them). The white pixel is bound
    // outside every list: a bind inside is dropped at compile time whenever TextureManager
    // believes that texture is current, and at replay time glCallList moves the real binding
    // without GlStateManager noticing, leaving its shadow state stale. See
    // TileEntityTrafficSignalHeadRenderer for the full account.
    boolean bakeable = pos != null && !CsmRenderToggles.streetSignStructurePerFrame;
    // The block's combined light leads the key because it is baked into every vertex (the BLOCK
    // format carries the lightmap per vertex, which is what OptiFine's shaders read -- they
    // ignore OpenGlHelper.setLightmapTextureCoords, so the light cannot be applied per frame from
    // outside the list instead). Keyed on it, a light change compiles a new list rather than
    // replaying yesterday's brightness; a blade that looks a level too dark is the client's own
    // light data for that block, which no cache key can correct.
    //
    // Adding or removing the second blade reshapes every list. The dirty flag already forces a
    // rebuild on any edit; the key carries the stack as well, so a list compiled for one shape
    // can never be replayed for the other whatever path the edit took.
    long structureKey = (combinedLight & 0xFFFFFFFFL) | (l.lower != null ? 1L << 33 : 0L) | keyBias;
    // The face additionally follows the illumination, which the structure does not.
    long faceKey = structureKey | (lightOn ? 1L << 32 : 0L);

    int coreList = bakeable ? CORE_LISTS.get(pos, structureKey) : CsmDisplayListCache.NO_LIST;
    if (coreList == CsmDisplayListCache.NO_LIST && bakeable) {
      coreList = CORE_LISTS.allocate(pos, structureKey);
      if (coreList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(coreList, GL11.GL_COMPILE);
        renderCore(l, data.getCornerStyle());
        GL11.glEndList();
      }
    }
    if (coreList == CsmDisplayListCache.NO_LIST) {
      renderCore(l, data.getCornerStyle());
    } else {
      GL11.glCallList(coreList);
    }

    renderFace(l, data, signColor, legendR, legendG, legendB, legendTextColor, farLod,
        pos, bakeable, faceKey, false);
    if (data.isDoubleSided()) {
      // The back face is the same draw rotated 180 degrees about the PANEL's own vertical
      // axis. That is orientation-preserving, so combined with the outer mirror the
      // legend reads correctly (not mirrored) to a viewer standing behind the blade. The
      // arrow is the one thing that must NOT come along unchanged -- see renderArrow.
      //
      // What the half turn is about decides where the reverse face lands, and the answer
      // is the thing the sign is mounted ON.
      //
      // A hanging blade is centred in the block's depth, so its panel, the block's centre
      // and the arm it hangs from are all the same plane and the distinction never came
      // up. A post-top blade stands to one side of its post. Turning it about its own
      // panel leaves both faces on that same side, with the post behind the pair -- which
      // is not how a sign sits on a pole here. Turning it about the POST puts one face on
      // each side of it, with the post between them: the geometry SignShift.BACKTOBACK
      // gives a pair of road signs sharing one post, which is what these are.
      float mirrorZ = data.getMountType().isPostTop() ? POST_Z : CZ;
      GlStateManager.pushMatrix();
      GlStateManager.translate(CX, 0.0f, mirrorZ);
      GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
      GlStateManager.translate(-CX, 0.0f, -mirrorZ);
      renderFace(l, data, signColor, legendR, legendG, legendB, legendTextColor, farLod,
          pos, bakeable, faceKey, true);
      GlStateManager.popMatrix();
    }
    // Frame, hangers and cable share one list: they are contiguous in the draw order and all draw
    // against the white pixel, so nothing separates them.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    int frameList = bakeable ? FRAME_LISTS.get(pos, structureKey) : CsmDisplayListCache.NO_LIST;
    if (frameList == CsmDisplayListCache.NO_LIST && bakeable) {
      frameList = FRAME_LISTS.allocate(pos, structureKey);
      if (frameList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(frameList, GL11.GL_COMPILE);
        renderStructure(l, data);
        GL11.glEndList();
      }
    }
    if (frameList == CsmDisplayListCache.NO_LIST) {
      renderStructure(l, data);
    } else {
      GL11.glCallList(frameList);
    }

  }

  /**
   * The frame, hangers and power cable, in the order they were drawn before they shared a display
   * list. Emits geometry only -- the white pixel is bound by the caller, outside the list.
   *
   * @param l    the resolved layout
   * @param data the blade's configuration
   */
  private void renderStructure(Layout l, StreetSignData data) {
    StreetSignMount mount = data.getMountType();
    if (data.hasExtrudedFrame()) {
      // Each blade of a stacked pair is its own extrusion, as the real ones are.
      for (Layout blade : l.blades()) {
        renderExtrudedFrame(blade, mount);
      }
    }
    if (mount.isPostTop()) {
      renderPostTopBracket(l, mount);
      return;
    }
    if (mount.isHanging()) {
      // The hardware above grips the TOP blade only; the lower blade hangs from the top one.
      if (mount == StreetSignMount.HANGING_BRACKET) {
        renderBracketHanger(l);
      } else {
        renderHangers(l);
      }
      float inset = mount == StreetSignMount.HANGING_BRACKET
          ? BRACKET_LINK_INSET_FRACTION : HANGER_INSET_FRACTION;
      if (l.lower != null) {
        renderBladeLinks(l, l.lower, inset);
      }
      if (data.hasExtrudedFrame()) {
        renderPowerCable(l, mount);
        if (l.lower != null) {
          // The lower blade is fed from the one above it, across the gap, at the same end.
          renderCableRun(cableX(l), bladeTop(l.lower) - JOINT_OVERLAP,
              bladeBottom(l) + JOINT_OVERLAP);
        }
      }
    }
  }

  /**
   * The hardware a post-top blade is carried on, drawn around the post for the one blade
   * this pass is drawing. A crossing pair gets one of these each, from its own pass, which
   * is why the two stack up the post rather than sharing a bracket.
   *
   * <p>Which bracket is the block's, not the player's: {@code POST_TOP_CLAMP} is the flat
   * plate the blade bolts into, with a saddle wrapping the post under it, and
   * {@code POST_TOP_CROSS} is the collar the blade passes through. They are the two the
   * hardware actually comes in, and they read differently from a distance, which is the
   * point of having both.</p>
   *
   * @param l     the blade's layout
   * @param mount which bracket to draw
   */
  private void renderPostTopBracket(Layout l, StreetSignMount mount) {
    final float top = bladeTop(l);
    final float bottom = bladeBottom(l);
    final float midY = (top + bottom) / 2.0f;
    // Everything the bracket is made of lives BEHIND the panel, between its back and the
    // post: in front it would be hardware painted across the street's name.
    final float backZ = POST_BLADE_Z + SIGN_DEPTH / 2.0f;
    final float postBack = POST_Z + 1.9f;
    List<RenderHelper.Box> parts = new ArrayList<>();
    if (mount == StreetSignMount.POST_TOP_CROSS) {
      // A collar round the post at the blade's height, reaching forward to take the panel.
      parts.add(new RenderHelper.Box(
          new float[]{POST_X - BRACKET_PAD, bottom - BRACKET_PAD, backZ},
          new float[]{POST_X + BRACKET_PAD, top + BRACKET_PAD, postBack}));
    } else {
      // A plate the width of the bracket across the panel's back, and the saddle that
      // carries it round the post.
      parts.add(new RenderHelper.Box(
          new float[]{POST_X - BRACKET_REACH, bottom, backZ},
          new float[]{POST_X + BRACKET_REACH, top, backZ + BRACKET_THICK}));
      parts.add(new RenderHelper.Box(
          new float[]{POST_X - BRACKET_PAD, midY - BRACKET_PAD, backZ + BRACKET_THICK},
          new float[]{POST_X + BRACKET_PAD, midY + BRACKET_PAD, postBack}));
      for (float bolt : new float[]{-BRACKET_REACH + BRACKET_PAD,
          BRACKET_REACH - BRACKET_PAD}) {
        parts.add(new RenderHelper.Box(
            new float[]{POST_X + bolt - 0.22f, midY - 0.22f, backZ + BRACKET_THICK},
            new float[]{POST_X + bolt + 0.22f, midY + 0.22f,
                backZ + BRACKET_THICK + 0.3f}));
      }
    }
    drawMetalwork(parts);
  }
  /**
   * The short links a lower blade hangs from: at each grip point, a shoe on the lower blade's
   * top edge, a clip on the upper blade's bottom edge, and a rod across the gap between them.
   * They sit directly below the hardware above -- the same x as the hangers or bracket links --
   * so the load path reads straight down through both blades.
   *
   * @param upper the top blade's layout
   * @param lower the lower blade's layout
   * @param inset the grip inset the mount above uses
   */
  private void renderBladeLinks(Layout upper, Layout lower, float inset) {
    float lowerShoe = bladeTop(lower) - JOINT_OVERLAP;
    float upperClip = bladeBottom(upper) + JOINT_OVERLAP;
    List<RenderHelper.Box> parts = new ArrayList<>();
    for (float hx : hangerCenters(upper, inset)) {
      addHangerShoe(parts, hx, lowerShoe);
      addHangerShoe(parts, hx, upperClip);
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_ROD_WIDTH / 2, lowerShoe + HANGER_SHOE_HEIGHT - JOINT_OVERLAP,
              CZ - HANGER_ROD_WIDTH / 2},
          new float[]{hx + HANGER_ROD_WIDTH / 2, upperClip - HANGER_SHOE_HEIGHT + JOINT_OVERLAP,
              CZ + HANGER_ROD_WIDTH / 2}));
    }
    drawMetalwork(parts);
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
    for (Layout b : l.blades()) {
      addRectBoxes(core, b.signLeft - b.borderInset - m, b.signBottom - b.borderInset - m,
          b.signRight + b.borderInset + m, b.signTop + b.borderInset + m,
          b.faceZ + Z_CORE_FRONT, b.coreBack, corners);
    }
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(core, buf, 0.55f, 0.56f, 0.58f, 1.0f, 0, 0, 0,
        ambientSkyLight, ambientBlockLight);
    tess.draw();
  }

  /**
   * Border plate, painted face, and (unless in far LOD) the whole legend. {@code backFace} is
   * set for the mirrored rear pass; only the arrow reacts to it.
   */
  private void renderFace(Layout l, StreetSignData data, GuideSignColor signColor,
      float legendR, float legendG, float legendB, int legendTextColor, boolean farLod,
      BlockPos pos, boolean bakeable, long faceKey, boolean backFace) {
    CornerStyle corners = data.getCornerStyle();

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    int faceList = bakeable ? FACE_LISTS.get(pos, faceKey) : CsmDisplayListCache.NO_LIST;
    if (faceList == CsmDisplayListCache.NO_LIST && bakeable) {
      faceList = FACE_LISTS.allocate(pos, faceKey);
      if (faceList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(faceList, GL11.GL_COMPILE);
        renderFaceBackground(l, signColor, legendR, legendG, legendB, corners);
        GL11.glEndList();
      }
    }
    if (faceList == CsmDisplayListCache.NO_LIST) {
      renderFaceBackground(l, signColor, legendR, legendG, legendB, corners);
    } else {
      GL11.glCallList(faceList);
    }

    if (farLod) {
      // Past the detail range the legend is unreadable and the font and atlas passes are the
      // expensive part; the painted body alone still reads as a street blade.
      return;
    }

    for (Layout blade : l.blades()) {
      renderLegend(blade, data, legendTextColor, backFace);
    }

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /**
   * One blade's lettering and side slots, laid out from that blade's own layout.
   *
   * @param l        the blade's layout, which carries its legend
   * @param data     the whole sign, for the style the blades share
   * @param backFace whether this is the mirrored rear pass
   */
  private void renderLegend(Layout l, StreetSignData data, int legendTextColor,
      boolean backFace) {
    StreetSignLegend legend = l.legend;
    float gapSlot = SLOT_GAP * l.scale;
    float x = l.contentLeft;

    // Side slots run outward from the text column in a fixed order -- arrow outermost, then
    // emblem, then block number -- which is how a real blade reads at both ends.
    if (legend.getArrowPosition() == StreetSignSlotPosition.LEFT) {
      renderArrow(legend, l, x, backFace);
      x += l.arrowSize + gapSlot;
    }
    if (legend.getEmblemPosition() == StreetSignSlotPosition.LEFT && legend.hasEmblem()) {
      renderEmblem(legend, l, x, legendTextColor);
      x += l.emblemWidth + gapSlot;
    }
    if (legend.getBlockPosition() == StreetSignSlotPosition.LEFT && legend.hasBlockNumber()) {
      renderBlockNumber(legend, l, x, legendTextColor);
      x += l.blockWidth + gapSlot;
    }

    renderTextColumn(data, legend, l, x, legendTextColor);
    x += l.textColumnWidth;

    if (legend.getBlockPosition() == StreetSignSlotPosition.RIGHT && legend.hasBlockNumber()) {
      x += gapSlot;
      renderBlockNumber(legend, l, x, legendTextColor);
      x += l.blockWidth;
    }
    if (legend.getEmblemPosition() == StreetSignSlotPosition.RIGHT && legend.hasEmblem()) {
      x += gapSlot;
      renderEmblem(legend, l, x, legendTextColor);
      x += l.emblemWidth;
    }
    if (legend.getArrowPosition() == StreetSignSlotPosition.RIGHT) {
      x += gapSlot;
      renderArrow(legend, l, x, backFace);
    }
  }

  /**
   * The border plate and the painted face behind the legend. Emits geometry only -- the white pixel
   * is bound by the caller, outside the list.
   *
   * @param l         the resolved layout
   * @param signColor the face colour
   * @param legendR   the legend red channel, which the border plate is painted in
   * @param legendG   the legend green channel
   * @param legendB   the legend blue channel
   * @param corners   the corner style
   */
  private void renderFaceBackground(Layout l, GuideSignColor signColor,
      float legendR, float legendG, float legendB, CornerStyle corners) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    if (l.borderInset > 0) {
      List<RenderHelper.Box> border = new ArrayList<>();
      for (Layout b : l.blades()) {
        addRectBoxes(border, b.signLeft - b.borderInset, b.signBottom - b.borderInset,
            b.signRight + b.borderInset, b.signTop + b.borderInset,
            b.faceZ + Z_BORDER_PLATE, b.faceZ + Z_BORDER_PLATE + LIT_FACE_DEPTH, corners);
      }
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(border, buf, legendR, legendG, legendB, 1.0f, 0, 0, 0,
          worldSkyLight, worldBlockLight);
      tess.draw();
    }

    // The painted face sits IN FRONT of the border plate (smaller Z), never behind it and
    // never at the same depth -- coplanar faces z-fight and a face behind the border renders
    // white from the front.
    List<RenderHelper.Box> face = new ArrayList<>();
    for (Layout b : l.blades()) {
      addRectBoxes(face, b.signLeft, b.signBottom, b.signRight, b.signTop,
          b.faceZ + Z_FACE_PLATE, b.faceZ + Z_FACE_PLATE + LIT_FACE_DEPTH, corners);
    }
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(face, buf,
        signColor.getRed(), signColor.getGreen(), signColor.getBlue(), 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();
  }

  /** Prefix, street name, suffix on one line, with the optional city line centered under it. */
  private void renderTextColumn(StreetSignData data, StreetSignLegend legend, Layout l,
      float columnLeft, int color) {
    float gapAffix = AFFIX_GAP * l.scale;
    float groupLeft = columnLeft + (l.textColumnWidth - l.nameGroupWidth) / 2.0f;
    float z = l.faceZ + Z_LEGEND;
    // Hanging the affixes from the name's cap line gives a blade the raised "W ... RD" look;
    // dropping them to its baseline gives the equally common flush style.
    float affixCenterY = alignToNameCap(l, data.getAffixVertical(), l.affixCap);

    GlStateManager.depthMask(false);
    float pen = groupLeft;
    if (!legend.getPrefix().isEmpty()) {
      GuideSignFontRenderer.drawString(legend.getPrefix(), pen, affixCenterY, z, l.affixCap,
          color, worldSkyLight, worldBlockLight);
      pen += l.prefixWidth;
    }
    GuideSignFontRenderer.drawString(legend.getStreetName(), pen, l.nameCenterY, z, l.nameCap,
        color, worldSkyLight, worldBlockLight);
    pen += l.nameWidth;
    if (!legend.getSuffix().isEmpty()) {
      GuideSignFontRenderer.drawString(legend.getSuffix(), pen + gapAffix, affixCenterY, z,
          l.affixCap, color, worldSkyLight, worldBlockLight);
    }
    if (legend.hasCityText()) {
      float cityLeft = columnLeft + (l.textColumnWidth - l.cityWidth) / 2.0f;
      GuideSignFontRenderer.drawString(legend.getCityText(), cityLeft, l.cityCenterY, z,
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

  private void renderBlockNumber(StreetSignLegend legend, Layout l, float x, int color) {
    float centerY = alignToNameCap(l, legend.getBlockVertical(), l.blockCap);
    GlStateManager.depthMask(false);
    GuideSignFontRenderer.drawString(legend.getBlockNumber(), x, centerY, l.faceZ + Z_LEGEND,
        l.blockCap, color, worldSkyLight, worldBlockLight);
    GlStateManager.depthMask(true);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /** The emblem slot: a route shield with its number drawn over it, or a civic logo cell. */
  private void renderEmblem(StreetSignLegend legend, Layout l, float x, int legendTextColor) {
    boolean isShield = legend.getEmblemKind() == StreetSignEmblemKind.SHIELD;
    GuideSignShieldType shieldType = legend.getShieldType();
    boolean wide = isShield && shieldType.usesWideVariant(legend.getShieldRoute());

    float[] uv;
    if (isShield) {
      uv = wide ? GuideSignAtlas.getShieldWideUV(shieldType)
          : GuideSignAtlas.getShieldUV(shieldType);
    } else {
      uv = GuideSignAtlas.getCellUV(legend.getLogoType().getAtlasCol(),
          legend.getLogoType().getAtlasRow());
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

    String route = legend.getShieldRoute();
    if (isShield && !route.isEmpty()) {
      // Shrink to fit so a long route number stays inside the shield's legend area instead
      // of spilling past its outline.
      float capPx = l.emblemSize * shieldType.getRouteTextCapFraction();
      float available = l.emblemWidth * shieldType.getRouteTextMaxFraction();
      float width = GuideSignFontRenderer.getStringWidth(route, capPx);
      if (width > available) {
        capPx *= available / width;
        width = available;
      }
      // Centred where the marker leaves room for it (above TEXAS, below Colorado's flag). The
      // offset is measured top-down in the atlas cell, and pixel space here runs upwards.
      float textCenterX = centerX + (shieldType.getRouteTextCenterX() - 0.5f) * l.emblemWidth;
      float textCenterY = centerY - (shieldType.getRouteTextCenterY() - 0.5f) * l.emblemSize;
      GlStateManager.depthMask(false);
      GuideSignFontRenderer.drawString(route, textCenterX - width / 2.0f, textCenterY,
          l.faceZ + Z_ROUTE_TEXT, capPx, shieldType.getRouteTextColor(),
          worldSkyLight, worldBlockLight);
      GlStateManager.depthMask(true);
    }
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  /**
   * The direction arrow in a side slot.
   *
   * <p>Unlike the legend, the arrow names a direction in the WORLD, not on the panel. The rear
   * pass draws the whole face rotated 180 degrees about the blade's vertical axis, which swaps
   * the reader's left and right, so an arrow carried through that rotation unchanged would point
   * a viewer behind the blade at the opposite street. Mirroring the quad's U coordinates undoes
   * exactly that swap and nothing else, so LEFT reads as LEFT from both sides and the symmetric
   * arrows (UP, DOWN, LEFT_RIGHT, UP_LEFT_RIGHT) are unaffected. Text and emblems are NOT
   * mirrored -- they are painted on the panel and rotate with it.
   *
   * @param backFace whether this is the mirrored rear pass
   */
  private void renderArrow(StreetSignLegend legend, Layout l, float x, boolean backFace) {
    float[] uv = GuideSignAtlas.getArrowUV(legend.getArrowType());
    float uLeft = backFace ? uv[2] : uv[0];
    float uRight = backFace ? uv[0] : uv[2];
    float half = l.arrowSize / 2.0f;
    float centerX = x + half;
    float centerY = l.contentCenterY;
    float z = l.faceZ + Z_EMBLEM;

    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    atlasVertex(buf, centerX + half, centerY + half, z, uRight, uv[1]);
    atlasVertex(buf, centerX - half, centerY + half, z, uLeft, uv[1]);
    atlasVertex(buf, centerX - half, centerY - half, z, uLeft, uv[3]);
    atlasVertex(buf, centerX + half, centerY - half, z, uRight, uv[3]);
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
    float back = mount.isHanging() ? 16.0f - front : l.coreBack + 0.05f;

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
    float shoeBottom = bladeTop(l) - JOINT_OVERLAP;
    float shoeTop = shoeBottom + HANGER_SHOE_HEIGHT;
    // The clamp grips at the TOP of the run, not at the block boundary, so whatever the run
    // reaches is what it appears to hang from.
    float hangerTop = l.assemblyTop;
    float clampBottom = hangerTop - HANGER_CLAMP_HEIGHT;

    List<RenderHelper.Box> parts = new ArrayList<>();
    for (float hx : hangerCenters(l, HANGER_INSET_FRACTION)) {
      addHangerShoe(parts, hx, shoeBottom);
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_ROD_WIDTH / 2, shoeTop - JOINT_OVERLAP,
              CZ - HANGER_ROD_WIDTH / 2},
          new float[]{hx + HANGER_ROD_WIDTH / 2, clampBottom + JOINT_OVERLAP,
              CZ + HANGER_ROD_WIDTH / 2}));
      addHangerClamp(parts, hx, clampBottom, hangerTop);
    }

    drawMetalwork(parts);
  }

  /**
   * The other common way a blade is hung: it swings off a horizontal support beam on two short
   * links, and the beam itself is carried on a <b>single</b> drop in the middle -- one
   * attachment point on the mast arm instead of two.
   *
   * <p>The links sit well outboard of where the two-hanger mount grips, for the reason given
   * on BRACKET_LINK_INSET_FRACTION. The beam overhangs both ends of the blade and is capped
   * there; flush with the blade it read as one more rail of the panel's own frame rather than
   * as the thing the panel hangs from.
   *
   * <p>Structural metal like the rest of it, so it draws at ambient light and stays dark around
   * a glowing blade.
   */
  private void renderBracketHanger(Layout l) {
    float shoeBottom = bladeTop(l) - JOINT_OVERLAP;
    float shoeTop = shoeBottom + HANGER_SHOE_HEIGHT;
    float beamBottom = beamBottom(l);
    float beamTop = beamBottom + BEAM_THICKNESS;
    float beamLeft = l.signLeft - l.borderInset - l.frameOverhangX - BEAM_OVERHANG;
    float beamRight = l.signRight + l.borderInset + l.frameOverhangX + BEAM_OVERHANG;
    float beamNear = CZ - BEAM_THICKNESS / 2;
    float beamFar = CZ + BEAM_THICKNESS / 2;

    List<RenderHelper.Box> parts = new ArrayList<>();
    parts.add(new RenderHelper.Box(
        new float[]{beamLeft, beamBottom, beamNear},
        new float[]{beamRight, beamTop, beamFar}));
    for (float capX : new float[]{beamLeft, beamRight - BEAM_CAP_WIDTH}) {
      parts.add(new RenderHelper.Box(
          new float[]{capX, beamBottom - BEAM_CAP_GROW, beamNear - BEAM_CAP_GROW},
          new float[]{capX + BEAM_CAP_WIDTH, beamTop + BEAM_CAP_GROW,
              beamFar + BEAM_CAP_GROW}));
    }

    for (float hx : hangerCenters(l, BRACKET_LINK_INSET_FRACTION)) {
      addHangerShoe(parts, hx, shoeBottom);
      parts.add(new RenderHelper.Box(
          new float[]{hx - HANGER_ROD_WIDTH / 2, shoeTop - JOINT_OVERLAP,
              CZ - HANGER_ROD_WIDTH / 2},
          new float[]{hx + HANGER_ROD_WIDTH / 2, beamBottom + JOINT_OVERLAP,
              CZ + HANGER_ROD_WIDTH / 2}));
      // The collar wraps the beam rather than butting into it, which is what makes the link
      // read as clamped onto a continuous beam instead of welded to a broken one.
      parts.add(new RenderHelper.Box(
          new float[]{hx - BEAM_COLLAR_WIDTH / 2, beamBottom - BEAM_COLLAR_GROW,
              beamNear - BEAM_COLLAR_GROW},
          new float[]{hx + BEAM_COLLAR_WIDTH / 2, beamTop + BEAM_COLLAR_GROW,
              beamFar + BEAM_COLLAR_GROW}));
    }

    // The single centre drop: a saddle over the beam, the post, and the clamp gripping
    // whatever is above -- at the same height the two-hanger mount's clamps reach, so either
    // mount appears to hang from the same thing.
    float saddleTop = beamTop + DROP_SADDLE_HEIGHT;
    float hangerTop = l.assemblyTop;
    float clampBottom = hangerTop - HANGER_CLAMP_HEIGHT;
    parts.add(new RenderHelper.Box(
        new float[]{CX - DROP_SADDLE_WIDTH / 2, beamBottom - BEAM_COLLAR_GROW,
            beamNear - BEAM_COLLAR_GROW},
        new float[]{CX + DROP_SADDLE_WIDTH / 2, saddleTop, beamFar + BEAM_COLLAR_GROW}));
    parts.add(new RenderHelper.Box(
        new float[]{CX - DROP_POST_WIDTH / 2, saddleTop - JOINT_OVERLAP,
            CZ - DROP_POST_WIDTH / 2},
        new float[]{CX + DROP_POST_WIDTH / 2, clampBottom + JOINT_OVERLAP,
            CZ + DROP_POST_WIDTH / 2}));
    addHangerClamp(parts, CX, clampBottom, hangerTop);

    drawMetalwork(parts);
  }

  /** Top edge of the blade, border and extruded frame included. */
  private static float bladeTop(Layout l) {
    return l.signTop + l.borderInset + l.frameOverhangY;
  }

  /** Bottom edge of the blade, border and extruded frame included. */
  private static float bladeBottom(Layout l) {
    return l.signBottom - l.borderInset - l.frameOverhangY;
  }

  /** Underside of the bracket mount's support beam. */
  private static float beamBottom(Layout l) {
    return bladeTop(l) + BEAM_GAP;
  }

  /**
   * Where along the blade the hangers -- or, on the bracket mount, the links up to the beam --
   * grip it. Shared, but not at a shared inset: see BRACKET_LINK_INSET_FRACTION.
   *
   * @param l             the resolved layout
   * @param insetFraction how far in from each end to grip, as a fraction of the blade's width
   */
  private static float[] hangerCenters(Layout l, float insetFraction) {
    float inset = Math.max(HANGER_CLAMP_WIDTH, l.signWidth * insetFraction);
    float[] centers = {l.signLeft + inset, l.signRight - inset};
    // On a very narrow blade the two hangers would collide; collapse to one down the middle.
    if (centers[1] - centers[0] < HANGER_CLAMP_WIDTH * 1.5f) {
      return new float[]{CX};
    }
    return centers;
  }

  private static void addHangerShoe(List<RenderHelper.Box> parts, float hx, float shoeBottom) {
    parts.add(new RenderHelper.Box(
        new float[]{hx - HANGER_SHOE_WIDTH / 2, shoeBottom - HANGER_SHOE_HEIGHT,
            CZ - HANGER_SHOE_DEPTH / 2},
        new float[]{hx + HANGER_SHOE_WIDTH / 2, shoeBottom + HANGER_SHOE_HEIGHT,
            CZ + HANGER_SHOE_DEPTH / 2}));
  }

  private static void addHangerClamp(List<RenderHelper.Box> parts, float hx, float clampBottom,
      float clampTop) {
    parts.add(new RenderHelper.Box(
        new float[]{hx - HANGER_CLAMP_WIDTH / 2, clampBottom, CZ - HANGER_CLAMP_DEPTH / 2},
        new float[]{hx + HANGER_CLAMP_WIDTH / 2, clampTop, CZ + HANGER_CLAMP_DEPTH / 2}));
  }

  /** One buffered pass of ambient-lit structural aluminum. */
  private void drawMetalwork(List<RenderHelper.Box> parts) {
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
   *
   * <p>Where it terminates follows the mount. On the two-hanger mount it runs the full height
   * the hangers reach, so it disappears into the same thing they grip. On the bracket mount
   * the support beam is already in the way at that x -- it overhangs the blade further than
   * the cable leaves -- so the cable ends inside the beam instead, which is where the real
   * ones are dressed anyway. Running it to full height there would have it pass visibly
   * through the beam.
   */
  private void renderPowerCable(Layout l, StreetSignMount mount) {
    float topY = mount == StreetSignMount.HANGING_BRACKET
        ? beamBottom(l) + BEAM_THICKNESS / 2.0f
        : l.assemblyTop;
    renderCableRun(cableX(l), bladeTop(l) - JOINT_OVERLAP, topY);
  }

  /** Where along the blade's end casting a feed cable leaves it. */
  private static float cableX(Layout l) {
    return l.signRight + l.borderInset + FRAME_END * CABLE_END_FRACTION;
  }

  /**
   * One bowed run of feed cable between two heights at the same x -- the blade's feed up to
   * its hardware, or a lower blade's feed across the gap from the blade above it.
   *
   * @param baseX where the run starts and ends horizontally
   * @param baseY the bottom of the run
   * @param topY  the top of the run
   */
  private void renderCableRun(float baseX, float baseY, float topY) {
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
