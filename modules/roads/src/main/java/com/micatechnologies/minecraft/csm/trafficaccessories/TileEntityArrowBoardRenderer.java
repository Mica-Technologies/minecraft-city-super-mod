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
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws everything on an arrow board above its trailer: the mast, the panel and the lamp grid.
 *
 * <p>The lamps are drawn here rather than baked into the panel's texture because they ANIMATE. A
 * sequential chevron is three chevrons lighting in turn; a texture can only ever show one moment
 * of that. It is also why the board's chassis is still a baked model and only the parts above it
 * moved here — the trailer never changes, and leaving it baked keeps the block's inventory icon
 * and its in-world presence when the renderer is not drawing.</p>
 *
 * <p>The tall part being tile entity geometry has a second benefit: it is culled against the tile
 * entity's own render bounding box rather than against the chunk section its block sits in, so a
 * board four blocks tall no longer vanishes when the section holding its base leaves view.</p>
 *
 * <p>Animated as it is, a board only ever shows one of a few dozen pictures: each pattern is a
 * short fixed sequence of stages, and the timer only picks which stage is showing. So each
 * stage is compiled once into a list shared by every board showing it ({@link
 * CsmSharedDisplayLists}), and the timer, the per-board offset and the configured speed still
 * choose the stage every frame exactly as before. {@link CsmRenderToggles#sharedBakesPerFrame}
 * draws it per frame, for comparison.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityArrowBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityArrowBoard> {

  /**
   * The mast, the panel and the lamp grid for one stage of one pattern at one light level. The
   * light is baked into the unlit parts' vertices, so it is part of the key; the lit lamps are
   * fullbright. Key layout (a {@code long}):
   * <ul>
   *   <li>bits 0-31: the combined light at the board, as {@code getCombinedLight} returns it</li>
   *   <li>bits 32-47: the stage index</li>
   *   <li>bits 48-55: the pattern ordinal</li>
   * </ul>
   */
  private static final CsmSharedDisplayLists BODY_LISTS =
      new CsmSharedDisplayLists("arrow_board_body");

  /**
   * The halo around one stage's lit lamps. Fullbright, so keyed only on the stage: bits 0-15 the
   * stage index, bits 16-23 the pattern ordinal. The additive blend and the depth mask it needs
   * are set around the call, never inside the list.
   */
  private static final CsmSharedDisplayLists GLOW_LISTS =
      new CsmSharedDisplayLists("arrow_board_glow");

  /** Beyond this distance the small halos contribute little but cost two blended passes. */
  private static final double GLOW_DISTANCE_SQUARED = 48.0 * 48.0;

  /** The orange every trailer and mast on one of these is built in. */
  private static final float[] COL_FRAME = {0.910f, 0.416f, 0.094f, 1.0f};

  /** The panel's black face and casing. */
  private static final float[] COL_PANEL = {0.102f, 0.102f, 0.110f, 1.0f};

  /** An unlit lamp: a dark housing, visible against the panel but plainly off. */
  private static final float[] COL_LAMP_OFF = {0.227f, 0.227f, 0.243f, 1.0f};

  /** A lit lamp. */
  private static final float[] COL_LAMP_ON = {0.965f, 0.651f, 0.094f, 1.0f};

  /** How far the lamps stand proud of the panel face. */
  private static final float LAMP_DEPTH = 0.45f;

  /** A 1x1 white pixel, bound so shaders cannot sample whatever was last used. */
  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** Fullbright sky light, for the lit lamps. */
  private static final int LIGHTMAP_FULLBRIGHT = 240;

  /** The halo layers, each {how far it grows as a fraction of the lamp radius, its alpha}. */
  private static final float[][] HALO_LAYERS = {{0.6f, 0.34f}, {1.3f, 0.13f}};

  /** Every lamp position's box, by [column][row]. Constant, so built once. */
  private static final RenderHelper.Box[][] LAMP_BOXES = buildLampBoxes();

  /** Every lamp position's halo box, by [layer][column][row]. Constant, so built once. */
  private static final RenderHelper.Box[][][] HALO_BOXES = buildHaloBoxes();

  @Override
  public void render(TileEntityArrowBoard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockWorkZoneArrowBoard)) {
      return;
    }

    DirectionEight facing = state.getPropertyKeys().contains(
        AbstractBlockRotatableHZEight.FACING)
        ? state.getValue(AbstractBlockRotatableHZEight.FACING) : DirectionEight.N;

    // The board settles onto the road under it exactly as its chassis model does; drawing here
    // in world space means adding that offset by hand or the mast leaves the trailer behind.
    double settle = ((ICsmRoadSurfaceAware) block).getRoadSurfaceOffset(te.getWorld(),
        te.getPos());

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);

    // The board's own clock. The per-board offset is what stops a row of them stepping in
    // unison, which a row of real boards does not do.
    ArrowBoardPattern pattern = te.getPattern();
    long millis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + te.getSequenceOffset();
    int stage = pattern.getStageIndex(millis);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + settle, z + 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    // Everything below is written in 1/16 block units, as the rest of this tab's renderers are.
    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    renderBody(pattern, stage, combinedLight);
    // x, y and z arrive already measured from the camera, as the dynamic signs' LOD tests read
    // them: subtracting the viewer's world position again would measure from the world origin
    // and switch the halos off everywhere but there.
    double dx = x + 0.5;
    double dy = y + 2.0;
    double dz = z + 0.5;
    if (dx * dx + dy * dy + dz * dz <= GLOW_DISTANCE_SQUARED
        && CsmConfig.isStrobeEffectEnabled() && hasLitLamp(pattern.getStageLamps(stage))) {
      renderGlow(pattern, stage);
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * Draws the mast, panel and lamp grid for one stage, from its shared list.
   *
   * @param pattern       the pattern
   * @param stage         the stage showing
   * @param combinedLight the combined light at the board
   */
  private static void renderBody(ArrowBoardPattern pattern, int stage, int combinedLight) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawBody(pattern, stage, combinedLight);
      return;
    }
    long key = ((long) (pattern.ordinal() & 0xFF) << 48) | ((long) (stage & 0xFFFF) << 32)
        | (combinedLight & 0xFFFFFFFFL);
    int list = BODY_LISTS.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = BODY_LISTS.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        drawBody(pattern, stage, combinedLight);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      drawBody(pattern, stage, combinedLight);
    }
  }

  /**
   * Draws the halo around one stage's lit lamps, additively so it reads as light rather than as
   * a bigger lamp. The blend, depth mask, cull and lighting it needs are set here, around the
   * list, never inside it.
   *
   * @param pattern the pattern
   * @param stage   the stage showing
   */
  private static void renderGlow(ArrowBoardPattern pattern, int stage) {
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawGlow(pattern, stage);
    } else {
      long key = ((long) (pattern.ordinal() & 0xFF) << 16) | (stage & 0xFFFF);
      int list = GLOW_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = GLOW_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawGlow(pattern, stage);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawGlow(pattern, stage);
      }
    }

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    // Put the ordinary alpha blend back before disabling blend, so the next renderer that turns
    // blending on without setting its own function does not inherit this additive one.
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
  }

  /**
   * The mast, its braces, the panel casing and the lamp grid for one stage. Geometry only: the
   * caller owns the texture and every GL state.
   *
   * @param pattern       the pattern
   * @param stage         the stage showing
   * @param combinedLight the combined light at the board
   */
  private static void drawBody(ArrowBoardPattern pattern, int stage, int combinedLight) {
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;
    buildStructure(sky, blockLight);
    drawLamps(litGrid(pattern.getStageLamps(stage)), sky, blockLight);
  }

  /** Draws the unchanging mast, braces and panel for one light level. */
  private static void buildStructure(int sky, int blockLight) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<RenderHelper.Box> frame = java.util.Arrays.asList(
        new RenderHelper.Box(new float[]{ArrowBoardGeometry.MAST_X0 - ArrowBoardGeometry.MAST_HALF,
            ArrowBoardGeometry.MAST_Y0, 8f - ArrowBoardGeometry.MAST_HALF},
            new float[]{ArrowBoardGeometry.MAST_X0 + ArrowBoardGeometry.MAST_HALF,
                ArrowBoardGeometry.MAST_Y1, 8f + ArrowBoardGeometry.MAST_HALF}),
        new RenderHelper.Box(new float[]{ArrowBoardGeometry.MAST_X1 - ArrowBoardGeometry.MAST_HALF,
            ArrowBoardGeometry.MAST_Y0, 8f - ArrowBoardGeometry.MAST_HALF},
            new float[]{ArrowBoardGeometry.MAST_X1 + ArrowBoardGeometry.MAST_HALF,
                ArrowBoardGeometry.MAST_Y1, 8f + ArrowBoardGeometry.MAST_HALF}),
        new RenderHelper.Box(new float[]{ArrowBoardGeometry.MAST_X0,
            ArrowBoardGeometry.BRACE_Y0 - ArrowBoardGeometry.BRACE_HALF_Y,
            8f - ArrowBoardGeometry.MAST_HALF * 0.7f},
            new float[]{ArrowBoardGeometry.MAST_X1,
                ArrowBoardGeometry.BRACE_Y0 + ArrowBoardGeometry.BRACE_HALF_Y,
                8f + ArrowBoardGeometry.MAST_HALF * 0.7f}),
        new RenderHelper.Box(new float[]{ArrowBoardGeometry.MAST_X0,
            ArrowBoardGeometry.BRACE_Y1 - ArrowBoardGeometry.BRACE_HALF_Y,
            8f - ArrowBoardGeometry.MAST_HALF * 0.7f},
            new float[]{ArrowBoardGeometry.MAST_X1,
                ArrowBoardGeometry.BRACE_Y1 + ArrowBoardGeometry.BRACE_HALF_Y,
                8f + ArrowBoardGeometry.MAST_HALF * 0.7f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(frame, buf, COL_FRAME[0], COL_FRAME[1], COL_FRAME[2],
        COL_FRAME[3], 0, 0, 0, sky, blockLight);
    tess.draw();

    List<RenderHelper.Box> panel = java.util.Collections.singletonList(new RenderHelper.Box(
        new float[]{ArrowBoardGeometry.PANEL_X0, ArrowBoardGeometry.PANEL_Y0,
            ArrowBoardGeometry.PANEL_Z0},
        new float[]{ArrowBoardGeometry.PANEL_X1, ArrowBoardGeometry.PANEL_Y1,
            ArrowBoardGeometry.PANEL_Z1}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(panel, buf, COL_PANEL[0], COL_PANEL[1], COL_PANEL[2],
        COL_PANEL[3], 0, 0, 0, sky, blockLight);
    tess.draw();
  }

  /**
   * Marks which grid positions a stage lights, ignoring any outside the grid.
   *
   * @param lit the stage's lit lamps, each {column, row}
   *
   * @return lit flags by [column][row]
   */
  private static boolean[][] litGrid(int[][] lit) {
    boolean[][] isLit =
        new boolean[ArrowBoardGeometry.GRID_COLS][ArrowBoardGeometry.GRID_ROWS];
    for (int[] lamp : lit) {
      if (inGrid(lamp)) {
        isLit[lamp[0]][lamp[1]] = true;
      }
    }
    return isLit;
  }

  /**
   * Whether a stage lights any lamp on the grid, which is what decides whether it has a halo.
   *
   * @param lit the stage's lit lamps, each {column, row}
   *
   * @return true if at least one is on the grid
   */
  private static boolean hasLitLamp(int[][] lit) {
    for (int[] lamp : lit) {
      if (inGrid(lamp)) {
        return true;
      }
    }
    return false;
  }

  private static boolean inGrid(int[] lamp) {
    return lamp[0] >= 0 && lamp[0] < ArrowBoardGeometry.GRID_COLS
        && lamp[1] >= 0 && lamp[1] < ArrowBoardGeometry.GRID_ROWS;
  }

  /**
   * The lamp grid: every position drawn dark, then the lit ones drawn over them.
   *
   * @param isLit      lit flags by [column][row]
   * @param sky        the sky light at the board
   * @param blockLight the block light at the board
   */
  private static void drawLamps(boolean[][] isLit, int sky, int blockLight) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<RenderHelper.Box> dark = new ArrayList<>();
    List<RenderHelper.Box> bright = new ArrayList<>();
    for (int c = 0; c < ArrowBoardGeometry.GRID_COLS; c++) {
      for (int r = 0; r < ArrowBoardGeometry.GRID_ROWS; r++) {
        (isLit[c][r] ? bright : dark).add(LAMP_BOXES[c][r]);
      }
    }

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(dark, buf, COL_LAMP_OFF[0], COL_LAMP_OFF[1],
        COL_LAMP_OFF[2], COL_LAMP_OFF[3], 0, 0, 0, sky, blockLight);
    tess.draw();

    if (bright.isEmpty()) {
      return;
    }
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bright, buf, COL_LAMP_ON[0], COL_LAMP_ON[1], COL_LAMP_ON[2],
        COL_LAMP_ON[3], 0, 0, 0, LIGHTMAP_FULLBRIGHT, LIGHTMAP_FULLBRIGHT);
    tess.draw();
  }

  /**
   * The halo quads around one stage's lit lamps, one draw per layer. Geometry only: the caller
   * owns the blend and every other GL state.
   *
   * @param pattern the pattern
   * @param stage   the stage showing
   */
  private static void drawGlow(ArrowBoardPattern pattern, int stage) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    boolean[][] isLit = litGrid(pattern.getStageLamps(stage));

    for (int layer = 0; layer < HALO_LAYERS.length; layer++) {
      List<RenderHelper.Box> halo = new ArrayList<>();
      for (int c = 0; c < ArrowBoardGeometry.GRID_COLS; c++) {
        for (int r = 0; r < ArrowBoardGeometry.GRID_ROWS; r++) {
          if (isLit[c][r]) {
            halo.add(HALO_BOXES[layer][c][r]);
          }
        }
      }
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(halo, buf, COL_LAMP_ON[0], COL_LAMP_ON[1], COL_LAMP_ON[2],
          HALO_LAYERS[layer][1], 0, 0, 0, LIGHTMAP_FULLBRIGHT, LIGHTMAP_FULLBRIGHT);
      tess.draw();
    }
  }

  /** Builds {@link #LAMP_BOXES}. */
  private static RenderHelper.Box[][] buildLampBoxes() {
    RenderHelper.Box[][] boxes =
        new RenderHelper.Box[ArrowBoardGeometry.GRID_COLS][ArrowBoardGeometry.GRID_ROWS];
    for (int c = 0; c < ArrowBoardGeometry.GRID_COLS; c++) {
      for (int r = 0; r < ArrowBoardGeometry.GRID_ROWS; r++) {
        boxes[c][r] = lampBox(c, r);
      }
    }
    return boxes;
  }

  /** Builds {@link #HALO_BOXES} from {@link #LAMP_BOXES}, which must already be built. */
  private static RenderHelper.Box[][][] buildHaloBoxes() {
    RenderHelper.Box[][][] boxes = new RenderHelper.Box[HALO_LAYERS.length]
        [ArrowBoardGeometry.GRID_COLS][ArrowBoardGeometry.GRID_ROWS];
    for (int layer = 0; layer < HALO_LAYERS.length; layer++) {
      for (int c = 0; c < ArrowBoardGeometry.GRID_COLS; c++) {
        for (int r = 0; r < ArrowBoardGeometry.GRID_ROWS; r++) {
          boxes[layer][c][r] = grow(LAMP_BOXES[c][r],
              ArrowBoardGeometry.LAMP_RADIUS * HALO_LAYERS[layer][0]);
        }
      }
    }
    return boxes;
  }

  /**
   * Gets the box of one lamp position on the panel.
   *
   * @param column the column, 0 at the left
   * @param row    the row, 0 at the top
   *
   * @return the lamp's box
   */
  private static RenderHelper.Box lampBox(int column, int row) {
    // Column 0 sits at MAX x. Looking at the board's face from outside puts world +X on the
    // viewer's LEFT, so laying the columns out in increasing x would mirror every pattern and
    // point each arrow the wrong way -- which reads as perfectly correct until someone follows
    // one.
    float cx = ArrowBoardGeometry.LIT_X0
        + (ArrowBoardGeometry.GRID_COLS - 1 - column + 0.5f) * ArrowBoardGeometry.CELL_W;
    float cy = ArrowBoardGeometry.LIT_Y0
        + (ArrowBoardGeometry.GRID_ROWS - 1 - row + 0.5f) * ArrowBoardGeometry.CELL_H;
    float rad = ArrowBoardGeometry.LAMP_RADIUS;
    return new RenderHelper.Box(
        new float[]{cx - rad, cy - rad, ArrowBoardGeometry.PANEL_Z0 - LAMP_DEPTH},
        new float[]{cx + rad, cy + rad, ArrowBoardGeometry.PANEL_Z0 + 0.05f});
  }

  /**
   * Grows a box outward by the given amount in x and y, for a halo layer.
   *
   * @param box the box
   * @param pad how far to grow it
   *
   * @return the grown box
   */
  private static RenderHelper.Box grow(RenderHelper.Box box, float pad) {
    return new RenderHelper.Box(
        new float[]{box.from[0] - pad, box.from[1] - pad, box.from[2]},
        new float[]{box.to[0] + pad, box.to[1] + pad, box.to[2]});
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
}
