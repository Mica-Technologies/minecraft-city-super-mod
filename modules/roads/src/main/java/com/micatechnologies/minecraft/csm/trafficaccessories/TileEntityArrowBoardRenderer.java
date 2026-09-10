package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
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
import net.minecraft.util.EnumFacing;
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
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityArrowBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityArrowBoard> {

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

    EnumFacing facing = state.getPropertyKeys().contains(AbstractBlockRotatableNSEW.FACING)
        ? state.getValue(AbstractBlockRotatableNSEW.FACING) : EnumFacing.NORTH;

    // The board settles onto the road under it exactly as its chassis model does; drawing here
    // in world space means adding that offset by hand or the mast leaves the trailer behind.
    double settle = ((ICsmRoadSurfaceAware) block).getRoadSurfaceOffset(te.getWorld(),
        te.getPos());

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

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

    renderStructure(sky, blockLight);
    renderLamps(te, partialTicks, sky, blockLight);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * The mast, its braces and the panel casing. None of this changes, so it is one buffer.
   *
   * @param sky        the sky light at the board
   * @param blockLight the block light at the board
   */
  private void renderStructure(int sky, int blockLight) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<RenderHelper.Box> frame = new ArrayList<>();
    float h = ArrowBoardGeometry.MAST_HALF;
    for (float mx : new float[]{ArrowBoardGeometry.MAST_X0, ArrowBoardGeometry.MAST_X1}) {
      frame.add(new RenderHelper.Box(
          new float[]{mx - h, ArrowBoardGeometry.MAST_Y0, 8f - h},
          new float[]{mx + h, ArrowBoardGeometry.MAST_Y1, 8f + h}));
    }
    for (float by : new float[]{ArrowBoardGeometry.BRACE_Y0, ArrowBoardGeometry.BRACE_Y1}) {
      frame.add(new RenderHelper.Box(
          new float[]{ArrowBoardGeometry.MAST_X0, by - ArrowBoardGeometry.BRACE_HALF_Y,
              8f - h * 0.7f},
          new float[]{ArrowBoardGeometry.MAST_X1, by + ArrowBoardGeometry.BRACE_HALF_Y,
              8f + h * 0.7f}));
    }
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(frame, buf, COL_FRAME[0], COL_FRAME[1], COL_FRAME[2],
        COL_FRAME[3], 0, 0, 0, sky, blockLight);
    tess.draw();

    List<RenderHelper.Box> panel = new ArrayList<>();
    panel.add(new RenderHelper.Box(
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
   * The lamp grid: every position drawn dark, then the lit ones drawn over them and given a glow.
   *
   * @param te           the board
   * @param partialTicks the partial tick
   * @param sky          the sky light at the board
   * @param blockLight   the block light at the board
   */
  private void renderLamps(TileEntityArrowBoard te, float partialTicks, int sky, int blockLight) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // The board's own clock. The per-board offset is what stops a row of them stepping in
    // unison, which a row of real boards does not do.
    long millis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + te.getSequenceOffset();
    int[][] lit = te.getPattern().getLitLamps(millis);

    boolean[][] isLit =
        new boolean[ArrowBoardGeometry.GRID_COLS][ArrowBoardGeometry.GRID_ROWS];
    for (int[] lamp : lit) {
      if (lamp[0] >= 0 && lamp[0] < ArrowBoardGeometry.GRID_COLS
          && lamp[1] >= 0 && lamp[1] < ArrowBoardGeometry.GRID_ROWS) {
        isLit[lamp[0]][lamp[1]] = true;
      }
    }

    List<RenderHelper.Box> dark = new ArrayList<>();
    List<RenderHelper.Box> bright = new ArrayList<>();
    for (int c = 0; c < ArrowBoardGeometry.GRID_COLS; c++) {
      for (int r = 0; r < ArrowBoardGeometry.GRID_ROWS; r++) {
        (isLit[c][r] ? bright : dark).add(lampBox(c, r));
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

    if (CsmConfig.isStrobeEffectEnabled()) {
      renderGlow(bright, tess, buf);
    }
  }

  /**
   * The halo around the lit lamps, drawn additively so it reads as light rather than as a bigger
   * lamp.
   *
   * @param bright the lit lamp boxes
   * @param tess   the tessellator
   * @param buf    its buffer
   */
  private void renderGlow(List<RenderHelper.Box> bright, Tessellator tess, BufferBuilder buf) {
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    for (float[] layer : new float[][]{{0.6f, 0.34f}, {1.3f, 0.13f}}) {
      List<RenderHelper.Box> halo = new ArrayList<>();
      for (RenderHelper.Box box : bright) {
        halo.add(grow(box, ArrowBoardGeometry.LAMP_RADIUS * layer[0]));
      }
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(halo, buf, COL_LAMP_ON[0], COL_LAMP_ON[1], COL_LAMP_ON[2],
          layer[1], 0, 0, 0, LIGHTMAP_FULLBRIGHT, LIGHTMAP_FULLBRIGHT);
      tess.draw();
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
}
