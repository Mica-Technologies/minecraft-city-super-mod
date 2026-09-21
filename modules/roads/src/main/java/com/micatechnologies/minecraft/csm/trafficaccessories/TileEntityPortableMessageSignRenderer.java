package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renders the portable changeable message sign: trailer, wheels, outriggers, mast, sign panel,
 * solar panel, the optional flasher sections and the page text.
 *
 * <p>Everything but the text and the flasher lenses is the same for every sign that shares a
 * trailer colour, a flasher setting and a world light level, so it is compiled into a list shared
 * by all of them ({@link CsmSharedDisplayLists}, keyed by {@link #structureKey}) and replayed under
 * each sign's own facing and angle transform. The vertices keep the world lightmap they always had,
 * which is why the light is part of the key. The flasher lenses (their phase follows the clock) and
 * the text (its page follows the tile entity) are drawn live every frame.
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws the structure per frame, for comparison.</p>
 */
public class TileEntityPortableMessageSignRenderer
    extends TileEntitySpecialRenderer<TileEntityPortableMessageSign> {

  // =====================================================================
  // SCALE CONSTANTS — adjust these to resize the structure.
  // All values are in model units (16 = 1 Minecraft block).
  // =====================================================================

  // Sign panel
  private static final float SIGN_WIDTH = 66.0f;
  private static final float SIGN_HEIGHT = 33.0f;
  private static final float SIGN_DEPTH = 3.0f;
  private static final float SIGN_FRAME = 1.5f;

  // Mast (vertical pole from trailer top to sign bottom)
  private static final float MAST_HEIGHT = 30.0f;
  private static final float MAST_SIZE = 4.5f;

  // Trailer body
  private static final float TRAILER_LENGTH = 36.0f;
  private static final float TRAILER_WIDTH = 19.5f;
  private static final float TRAILER_HEIGHT = 6.0f;
  private static final float TRAILER_GROUND_CLEARANCE = 4.5f;

  // Wheels
  private static final float WHEEL_DIAMETER = 7.5f;
  private static final float WHEEL_WIDTH = 2.25f;

  // Outrigger legs (stabilizer jacks)
  private static final float OUTRIGGER_SPREAD = 27.0f;
  private static final float OUTRIGGER_LEG_SIZE = 1.5f;
  private static final float OUTRIGGER_FOOT_SIZE = 3.75f;

  // Solar panel (on top of sign)
  private static final float SOLAR_WIDTH = SIGN_WIDTH * 0.55f;
  private static final float SOLAR_DEPTH = 12.0f;
  private static final float SOLAR_THICKNESS = 0.75f;

  // Text rendering
  private static final float TEXT_SCALE = 0.975f;
  private static final int TEXT_COLOR_AMBER = 0xFFAA00;

  // =====================================================================
  // DERIVED POSITIONS (computed from the constants above)
  // =====================================================================
  private static final float CX = 8.0f;
  private static final float CZ = 8.0f;

  private static final float TRAILER_BOTTOM = TRAILER_GROUND_CLEARANCE;
  private static final float TRAILER_TOP = TRAILER_BOTTOM + TRAILER_HEIGHT;
  private static final float MAST_BOTTOM = TRAILER_TOP;
  private static final float MAST_TOP = MAST_BOTTOM + MAST_HEIGHT;
  private static final float SIGN_BOTTOM = MAST_TOP;
  private static final float SIGN_TOP = SIGN_BOTTOM + SIGN_HEIGHT;
  private static final float SIGN_CENTER_Y = (SIGN_BOTTOM + SIGN_TOP) / 2.0f;

  // =====================================================================
  // COLORS (RGBA floats)
  // =====================================================================
  private static final float[] COL_DARK_GRAY = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_FACE = {0.06f, 0.06f, 0.06f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};
  private static final float[] COL_RUBBER = {0.10f, 0.10f, 0.10f, 1.0f};
  private static final float[] COL_SILVER = {0.50f, 0.50f, 0.52f, 1.0f};
  private static final float[] COL_SOLAR = {0.08f, 0.10f, 0.20f, 1.0f};

  // Trailer color palette (indexed by tile entity trailerColor value)
  private static final float[][] TRAILER_COLORS = {
      {0.93f, 0.45f, 0.08f, 1.0f},  // 0: Orange
      {0.95f, 0.85f, 0.10f, 1.0f},  // 1: Yellow
      {0.12f, 0.12f, 0.12f, 1.0f},  // 2: Black
      {0.70f, 0.70f, 0.72f, 1.0f},  // 3: Silver
      {0.92f, 0.92f, 0.92f, 1.0f},  // 4: White
  };

  // Angle rotation offsets (degrees, indexed by tile entity signAngle value)
  private static final float[] ANGLE_ROTATIONS = {
      0f,    // 0: Normal
      -15f,  // 1: Left Tilt
      15f,   // 2: Right Tilt
      -45f,  // 3: Left Angle
      45f,   // 4: Right Angle
  };

  // =====================================================================
  // Flasher signal section constants (reuses traffic signal 8-inch geometry)
  // =====================================================================
  private static final ResourceLocation SIGNAL_ATLAS =
      new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");

  private static final float FLASHER_BODY_R = 0.094f;
  private static final float FLASHER_BODY_G = 0.094f;
  private static final float FLASHER_BODY_B = 0.094f;

  private static final float VISOR_TINT_SCALE = 1.04f;
  private static final float VISOR_TINT_BASE = 0.01f;
  private static final float VISOR_OUTER_R = Math.min(1.0f, FLASHER_BODY_R * VISOR_TINT_SCALE + VISOR_TINT_BASE);
  private static final float VISOR_OUTER_G = Math.min(1.0f, FLASHER_BODY_G * VISOR_TINT_SCALE + VISOR_TINT_BASE);
  private static final float VISOR_OUTER_B = Math.min(1.0f, FLASHER_BODY_B * VISOR_TINT_SCALE + VISOR_TINT_BASE);

  private static final float VISOR_TILT_DEGREES = 9.0f;
  private static final float VISOR_PIVOT_Z = 11.0f;
  private static final float VISOR_CENTER_X = 8.0f;
  private static final float VISOR_CENTER_Y = 6.0f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  // =====================================================================
  // Flasher placement. The signal sections sit at the top corners of the sign frame; the 8-inch
  // signal geometry is centered at (8, 6) in a 16-unit space, so these offsets move it there.
  // =====================================================================
  private static final float FLASHER_SIGN_LEFT_EDGE = CX - SIGN_WIDTH / 2 - SIGN_FRAME;
  private static final float FLASHER_SIGN_RIGHT_EDGE = CX + SIGN_WIDTH / 2 + SIGN_FRAME;
  private static final float FLASHER_Y = SIGN_TOP + SIGN_FRAME - 6.0f;

  // Signal body face is at Z=11 in vertex data; align it with the sign face
  private static final float FLASHER_Z_OFF = (CZ - SIGN_DEPTH / 2) - 11.0f;

  // Left flasher: center the 8-inch housing just outside the left sign frame
  private static final float FLASHER_LEFT_X_OFF = (FLASHER_SIGN_LEFT_EDGE - 4.0f) - VISOR_CENTER_X;
  private static final float FLASHER_LEFT_Y_OFF = FLASHER_Y - VISOR_CENTER_Y;

  // Right flasher: center the 8-inch housing just outside the right sign frame
  private static final float FLASHER_RIGHT_X_OFF =
      (FLASHER_SIGN_RIGHT_EDGE + 4.0f) - VISOR_CENTER_X;
  private static final float FLASHER_RIGHT_Y_OFF = FLASHER_Y - VISOR_CENTER_Y;

  // =====================================================================
  // CONSTANT GEOMETRY, built once rather than per frame
  // =====================================================================
  private static final List<RenderHelper.Box> TRAILER_BOXES = trailerBoxes();
  private static final List<RenderHelper.Box> WHEEL_BOXES = wheelBoxes();
  private static final List<RenderHelper.Box> AXLE_BOXES = axleBoxes();
  private static final List<RenderHelper.Box> OUTRIGGER_LEG_BOXES = outriggerBoxes(false);
  private static final List<RenderHelper.Box> OUTRIGGER_FOOT_BOXES = outriggerBoxes(true);
  private static final List<RenderHelper.Box> MAST_BOXES = mastBoxes();
  private static final List<RenderHelper.Box> SIGN_FRAME_BOXES = signFrameBoxes();
  private static final List<RenderHelper.Box> SIGN_FACE_BOXES = signFaceBoxes();
  private static final List<RenderHelper.Box> SOLAR_BRACKET_BOXES = solarBracketBoxes();
  private static final List<RenderHelper.Box> SOLAR_PANEL_BOXES = solarPanelBoxes();
  private static final List<RenderHelper.Box> FLASHER_ARM_BOXES = flasherArmBoxes();

  /** The constant structure, one list per look; see {@link #structureKey}. */
  private static final CsmSharedDisplayLists STRUCTURE_LISTS =
      new CsmSharedDisplayLists("portable_message_sign");

  @Override
  public void render(TileEntityPortableMessageSign te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(BlockHorizontal.FACING);

    int colorIdx = te.getTrailerColor();
    if (colorIdx < 0 || colorIdx >= TRAILER_COLORS.length) colorIdx = 0;

    int angleIdx = te.getSignAngle();
    if (angleIdx < 0 || angleIdx >= ANGLE_ROTATIONS.length) angleIdx = 0;
    float angleOffset = ANGLE_ROTATIONS[angleIdx];

    // A renderer draws in world space and knows nothing about the block's render offset, so
    // the settle onto the road below has to be applied here too. See RoadSurfaceHeight.
    double settle = RoadSurfaceHeight.offsetFor(te.getWorld(), te.getPos());

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y + settle, z);
    GlStateManager.translate(0.5, 0.0, 0.5);

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
    GlStateManager.rotate(rotY + angleOffset, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);

    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int block = combinedLight & 0xFFFF;

    int mode = te.getFlasherMode();
    boolean flashers = mode != TileEntityPortableMessageSign.FLASHER_NONE;

    // Bound outside the list, every frame: a bind inside a list may never be recorded.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    renderStructure(colorIdx, flashers, sky, block);

    if (flashers) {
      renderFlasherBulbs(te, mode);
    }

    renderText(te);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  /**
   * Packs everything the structure's vertices depend on. Bits 0-15: the sky lightmap coordinate;
   * bits 16-31: the block lightmap coordinate; bits 32-34: the trailer colour index (0-4); bit 35:
   * the flasher housings and arms are drawn (flasher mode is not NONE). Never a position.
   */
  private static long structureKey(int colorIdx, boolean flashers, int sky, int block) {
    return (sky & 0xFFFFL)
        | ((long) (block & 0xFFFF) << 16)
        | ((long) (colorIdx & 0x7) << 32)
        | (flashers ? 1L << 35 : 0L);
  }

  /** Draws the constant structure from its shared list, compiling it on first use. */
  private static void renderStructure(int colorIdx, boolean flashers, int sky, int block) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawStructure(colorIdx, flashers, sky, block);
      return;
    }
    long key = structureKey(colorIdx, flashers, sky, block);
    int list = STRUCTURE_LISTS.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = STRUCTURE_LISTS.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        drawStructure(colorIdx, flashers, sky, block);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
      // The draw's own post-draw colour reset ran at compile time, not now: redo it so the
      // colour cache does not skip the next GlStateManager.color call.
      GlStateManager.resetColor();
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      drawStructure(colorIdx, flashers, sky, block);
    }
  }

  /**
   * Draws the trailer, wheels, outriggers, mast, sign panel, solar panel and, when fitted, the
   * flasher housings and their arms, in one draw and in the order they were always drawn.
   * Geometry only: the caller binds the white texture and owns every GL state.
   */
  private static void drawStructure(int colorIdx, boolean flashers, int sky, int block) {
    float[] col = TRAILER_COLORS[colorIdx];
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    // Trailer body and tongue
    RenderHelper.addBoxesToBufferLit(TRAILER_BOXES, buf, col[0], col[1], col[2], col[3], 0, 0, 0,
        sky, block);

    // Wheels and axle
    RenderHelper.addBoxesToBufferLit(WHEEL_BOXES, buf,
        COL_RUBBER[0], COL_RUBBER[1], COL_RUBBER[2], COL_RUBBER[3], 0, 0, 0, sky, block);
    RenderHelper.addBoxesToBufferLit(AXLE_BOXES, buf,
        COL_DARK_GRAY[0], COL_DARK_GRAY[1], COL_DARK_GRAY[2], COL_DARK_GRAY[3], 0, 0, 0,
        sky, block);

    // Outriggers: legs and arms in the trailer colour, silver feet
    RenderHelper.addBoxesToBufferLit(OUTRIGGER_LEG_BOXES, buf, col[0], col[1], col[2], col[3],
        0, 0, 0, sky, block);
    RenderHelper.addBoxesToBufferLit(OUTRIGGER_FOOT_BOXES, buf,
        COL_SILVER[0], COL_SILVER[1], COL_SILVER[2], COL_SILVER[3], 0, 0, 0, sky, block);

    // Mast
    RenderHelper.addBoxesToBufferLit(MAST_BOXES, buf,
        COL_DARK_GRAY[0], COL_DARK_GRAY[1], COL_DARK_GRAY[2], COL_DARK_GRAY[3], 0, 0, 0,
        sky, block);

    // Sign frame and face
    RenderHelper.addBoxesToBufferLit(SIGN_FRAME_BOXES, buf,
        COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0, sky, block);
    RenderHelper.addBoxesToBufferLit(SIGN_FACE_BOXES, buf,
        COL_SIGN_FACE[0], COL_SIGN_FACE[1], COL_SIGN_FACE[2], COL_SIGN_FACE[3], 0, 0, 0,
        sky, block);

    // Solar panel bracket and panel
    RenderHelper.addBoxesToBufferLit(SOLAR_BRACKET_BOXES, buf,
        COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0, sky, block);
    RenderHelper.addBoxesToBufferLit(SOLAR_PANEL_BOXES, buf,
        COL_SOLAR[0], COL_SOLAR[1], COL_SOLAR[2], COL_SOLAR[3], 0, 0, 0, sky, block);

    if (flashers) {
      // Signal housings, then the mounting arms connecting them to the sign frame
      addFlasherHousing(buf, FLASHER_LEFT_X_OFF, FLASHER_LEFT_Y_OFF, FLASHER_Z_OFF, sky, block);
      addFlasherHousing(buf, FLASHER_RIGHT_X_OFF, FLASHER_RIGHT_Y_OFF, FLASHER_Z_OFF, sky,
          block);
      RenderHelper.addBoxesToBufferLit(FLASHER_ARM_BOXES, buf,
          COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0, sky, block);
    }

    tess.draw();
  }

  private static List<RenderHelper.Box> trailerBoxes() {
    List<RenderHelper.Box> boxes = new ArrayList<>();

    // Main body
    boxes.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2, TRAILER_BOTTOM, CZ - TRAILER_LENGTH / 2 + 4},
        new float[]{CX + TRAILER_WIDTH / 2, TRAILER_TOP, CZ + TRAILER_LENGTH / 2 + 4}));

    // Tongue (hitch arm extending back)
    boxes.add(new RenderHelper.Box(
        new float[]{CX - 1.0f, TRAILER_BOTTOM + 1, CZ + TRAILER_LENGTH / 2 + 4},
        new float[]{CX + 1.0f, TRAILER_BOTTOM + 2.5f, CZ + TRAILER_LENGTH / 2 + 10}));
    return boxes;
  }

  private static List<RenderHelper.Box> wheelBoxes() {
    List<RenderHelper.Box> boxes = new ArrayList<>();

    float wheelY = TRAILER_GROUND_CLEARANCE - WHEEL_DIAMETER / 2 + 1;
    float wheelZ = CZ + 4;

    // Left wheel
    boxes.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2 - WHEEL_WIDTH, wheelY, wheelZ - WHEEL_DIAMETER / 2},
        new float[]{CX - TRAILER_WIDTH / 2, wheelY + WHEEL_DIAMETER,
            wheelZ + WHEEL_DIAMETER / 2}));

    // Right wheel
    boxes.add(new RenderHelper.Box(
        new float[]{CX + TRAILER_WIDTH / 2, wheelY, wheelZ - WHEEL_DIAMETER / 2},
        new float[]{CX + TRAILER_WIDTH / 2 + WHEEL_WIDTH, wheelY + WHEEL_DIAMETER,
            wheelZ + WHEEL_DIAMETER / 2}));
    return boxes;
  }

  private static List<RenderHelper.Box> axleBoxes() {
    float wheelY = TRAILER_GROUND_CLEARANCE - WHEEL_DIAMETER / 2 + 1;
    float wheelZ = CZ + 4;

    List<RenderHelper.Box> axle = new ArrayList<>();
    axle.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2 - WHEEL_WIDTH, wheelY + WHEEL_DIAMETER / 2 - 0.5f,
            wheelZ - 0.5f},
        new float[]{CX + TRAILER_WIDTH / 2 + WHEEL_WIDTH, wheelY + WHEEL_DIAMETER / 2 + 0.5f,
            wheelZ + 0.5f}));
    return axle;
  }

  /** The outrigger legs with their arms (interleaved, as drawn), or the foot pads. */
  private static List<RenderHelper.Box> outriggerBoxes(boolean feetOnly) {
    List<RenderHelper.Box> legs = new ArrayList<>();
    List<RenderHelper.Box> feet = new ArrayList<>();

    float legZ1 = CZ - TRAILER_LENGTH / 2 + 6;
    float legZ2 = CZ + TRAILER_LENGTH / 2 + 2;

    float[][] legPositions = {
        {CX - OUTRIGGER_SPREAD / 2, legZ1},
        {CX + OUTRIGGER_SPREAD / 2, legZ1},
        {CX - OUTRIGGER_SPREAD / 2, legZ2},
        {CX + OUTRIGGER_SPREAD / 2, legZ2}
    };

    for (float[] p : legPositions) {
      // Vertical leg
      legs.add(new RenderHelper.Box(
          new float[]{p[0] - OUTRIGGER_LEG_SIZE / 2, 0, p[1] - OUTRIGGER_LEG_SIZE / 2},
          new float[]{p[0] + OUTRIGGER_LEG_SIZE / 2, TRAILER_BOTTOM,
              p[1] + OUTRIGGER_LEG_SIZE / 2}));

      // Foot pad
      feet.add(new RenderHelper.Box(
          new float[]{p[0] - OUTRIGGER_FOOT_SIZE / 2, 0, p[1] - OUTRIGGER_FOOT_SIZE / 2},
          new float[]{p[0] + OUTRIGGER_FOOT_SIZE / 2, 0.5f, p[1] + OUTRIGGER_FOOT_SIZE / 2}));

      // Horizontal arm connecting leg to trailer
      float armEndX = p[0] > CX ? CX + TRAILER_WIDTH / 2 : CX - TRAILER_WIDTH / 2;
      legs.add(new RenderHelper.Box(
          new float[]{Math.min(p[0], armEndX) - 0.25f, TRAILER_BOTTOM - 0.5f,
              p[1] - OUTRIGGER_LEG_SIZE / 2},
          new float[]{Math.max(p[0], armEndX) + 0.25f, TRAILER_BOTTOM + 0.5f,
              p[1] + OUTRIGGER_LEG_SIZE / 2}));
    }

    return feetOnly ? feet : legs;
  }

  private static List<RenderHelper.Box> mastBoxes() {
    List<RenderHelper.Box> boxes = new ArrayList<>();

    boxes.add(new RenderHelper.Box(
        new float[]{CX - MAST_SIZE / 2, MAST_BOTTOM, CZ - MAST_SIZE / 2},
        new float[]{CX + MAST_SIZE / 2, MAST_TOP, CZ + MAST_SIZE / 2}));
    return boxes;
  }

  private static List<RenderHelper.Box> signFrameBoxes() {
    // Frame (slightly larger than the face)
    List<RenderHelper.Box> frame = new ArrayList<>();
    frame.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            CZ - SIGN_DEPTH / 2},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            CZ + SIGN_DEPTH / 2}));
    return frame;
  }

  private static List<RenderHelper.Box> signFaceBoxes() {
    // Sign face (black, inset slightly from frame)
    List<RenderHelper.Box> face = new ArrayList<>();
    face.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, CZ - SIGN_DEPTH / 2 - 0.05f},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, CZ - SIGN_DEPTH / 2 + 0.3f}));
    return face;
  }

  private static List<RenderHelper.Box> solarBracketBoxes() {
    List<RenderHelper.Box> boxes = new ArrayList<>();

    // Solar panel bracket (small post above sign)
    boxes.add(new RenderHelper.Box(
        new float[]{CX - 1.0f, SIGN_TOP + SIGN_FRAME, CZ - 1.0f},
        new float[]{CX + 1.0f, SIGN_TOP + SIGN_FRAME + 3.0f, CZ + 1.0f}));
    return boxes;
  }

  private static List<RenderHelper.Box> solarPanelBoxes() {
    // Solar panel (flat, tilted slightly)
    List<RenderHelper.Box> panel = new ArrayList<>();
    float panelY = SIGN_TOP + SIGN_FRAME + 3.0f;
    panel.add(new RenderHelper.Box(
        new float[]{CX - SOLAR_WIDTH / 2, panelY, CZ - SOLAR_DEPTH / 2},
        new float[]{CX + SOLAR_WIDTH / 2, panelY + SOLAR_THICKNESS, CZ + SOLAR_DEPTH / 2}));
    return panel;
  }

  private static List<RenderHelper.Box> flasherArmBoxes() {
    // Mounting arms connecting signal sections to the sign frame
    float armZ1 = CZ - SIGN_DEPTH / 2;
    float armZ2 = armZ1 + 2.0f;
    List<RenderHelper.Box> arms = new ArrayList<>();
    arms.add(new RenderHelper.Box(
        new float[]{FLASHER_SIGN_LEFT_EDGE - 4.0f, FLASHER_Y - 0.5f, armZ1},
        new float[]{FLASHER_SIGN_LEFT_EDGE, FLASHER_Y + 0.5f, armZ2}));
    arms.add(new RenderHelper.Box(
        new float[]{FLASHER_SIGN_RIGHT_EDGE, FLASHER_Y - 0.5f, armZ1},
        new float[]{FLASHER_SIGN_RIGHT_EDGE + 4.0f, FLASHER_Y + 0.5f, armZ2}));
    return arms;
  }

  /**
   * Draws the two flasher lenses, live every frame: their phase follows the clock. The housings
   * and arms behind them are part of the structure list.
   */
  private void renderFlasherBulbs(TileEntityPortableMessageSign te, int mode) {
    boolean bulbLit = false;
    if (mode == TileEntityPortableMessageSign.FLASHER_ON) {
      // Read the once-per-frame cached wall clock rather than calling currentTimeMillis()
      // directly -- it is a JNI call, and this runs per visible sign per frame.
      bulbLit = (CsmRenderUtils.gameMillis(te.getWorld()) / 500) % 2 == 0;
    }

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Bulb face — atlas texture, fullbright per-vertex
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    Minecraft.getMinecraft().getTextureManager().bindTexture(SIGNAL_ATLAS);

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    addFlasherBulb(buf, FLASHER_LEFT_X_OFF, FLASHER_LEFT_Y_OFF, FLASHER_Z_OFF, bulbLit);
    addFlasherBulb(buf, FLASHER_RIGHT_X_OFF, FLASHER_RIGHT_Y_OFF, FLASHER_Z_OFF, bulbLit);

    tess.draw();
  }

  private static void addFlasherHousing(BufferBuilder buf, float xOff, float yOff, float zOff,
      int sky, int block) {
    // Body
    RenderHelper.addBoxesToBufferLit(
        TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA, buf,
        FLASHER_BODY_R, FLASHER_BODY_G, FLASHER_BODY_B, 1.0f, xOff, yOff, zOff,
        sky, block);

    // Door
    RenderHelper.addBoxesToBufferLit(
        TrafficSignalVertexData.SIGNAL_DOOR_8INCH_VERTEX_DATA, buf,
        FLASHER_BODY_R, FLASHER_BODY_G, FLASHER_BODY_B, 1.0f, xOff, yOff, zOff,
        sky, block);

    // Circle visor with tilt
    RenderHelper.addTiltedBoxesToBufferDualColorLit(
        TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA, buf,
        VISOR_OUTER_R, VISOR_OUTER_G, VISOR_OUTER_B,
        0f, 0f, 0f, 1.0f,
        xOff, yOff, zOff, VISOR_PIVOT_Z + zOff, VISOR_TILT_DEGREES,
        VISOR_CENTER_X, VISOR_CENTER_Y, 0f, sky, block);
  }

  private void addFlasherBulb(BufferBuilder buf, float xOff, float yOff, float zOff,
      boolean lit) {
    TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.YELLOW, lit);

    float fullSize = 8.0f;
    float sizeScale = fullSize / 12f;
    float inset = fullSize * 0.02f;
    float size = fullSize - inset * 2f;
    float sectionOffset = (12f - fullSize) / 2f;
    float baseX = 2f + inset + xOff + sectionOffset;
    float baseY = yOff + inset + sectionOffset;
    float z = VISOR_PIVOT_Z + (10.4f - VISOR_PIVOT_Z) * sizeScale + zOff;

    float u1 = texInfo.getU1();
    float v1 = texInfo.getV1();
    float u2 = texInfo.getU2();
    float v2 = texInfo.getV2();

    bulbVertex(buf, baseX, baseY, z, u2, v2);
    bulbVertex(buf, baseX + size, baseY, z, u1, v2);
    bulbVertex(buf, baseX + size, baseY + size, z, u1, v1);
    bulbVertex(buf, baseX, baseY + size, z, u2, v1);
  }

  private static void bulbVertex(BufferBuilder buf, float x, float y, float z,
      float u, float v) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
  }

  private void renderText(TileEntityPortableMessageSign te) {
    int pageIdx = te.getCurrentPageIndex();
    String[] page = te.getPage(pageIdx);
    String line1 = page[0];
    String line2 = page[1];
    String line3 = page[2];

    if (line1.isEmpty() && line2.isEmpty() && line3.isEmpty()) {
      return;
    }

    CsmFontRenderer fr = CsmFontRenderer.electronicSign();

    // Force the lightmap unit to fullbright for the text pass. CsmFontRenderer.drawString
    // uses POSITION_TEX (no per-vertex lightmap), so it inherits the GL lightmap state set
    // by the prior BLOCK-format draws — which is the world's combined-light. Without this
    // override the LED text dims with the surrounding world instead of glowing.
    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK);

    GlStateManager.pushMatrix();

    // Position at sign face
    float faceZ = CZ - SIGN_DEPTH / 2 - 0.1f;
    GlStateManager.translate(CX, SIGN_CENTER_Y, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

    GlStateManager.depthMask(false);

    float lineSpacing = fr.FONT_HEIGHT + 2;
    float totalHeight = 3 * lineSpacing;
    float startY = -totalHeight / 2.0f + 1;

    String[] lines = {line1, line2, line3};
    for (int i = 0; i < 3; i++) {
      if (!lines[i].isEmpty()) {
        int textWidth = fr.getStringWidth(lines[i]);
        float textX = -textWidth / 2.0f;
        float textY = startY + i * lineSpacing;
        fr.drawString(lines[i], (int) textX, (int) textY, TEXT_COLOR_AMBER);
      }
    }

    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }
}
