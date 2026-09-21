package com.micatechnologies.minecraft.csm.signage;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws an advertising board's ad: one textured quad across the whole board, inside its frame,
 * from the controller. The rest of the board -- backing and frame -- is baked block models, so a
 * board of any size costs one quad a frame; two while it fades or slides from one ad to the next,
 * one more for a letterbox, and one for a screen's LED grid.
 *
 * <p>Drawn in the frame the models are drawn in, the face to the south, and turned to the facing
 * as the blockstate turns them: {@code x} runs to the board's right seen from the front, from
 * {@code -controllerColumn} at its left edge, and the face stands {@link AdBoardKind#getFacePx()}
 * off the back of the block.</p>
 */
@SideOnly(Side.CLIENT)
public class TileEntityAdBoardRenderer extends TileEntitySpecialRenderer<TileEntityAdBoard> {

  /** The LED grid laid over a screen: one texel square per LED, repeated across the face. */
  private static final ResourceLocation LED_GRID =
      new ResourceLocation("csm", "textures/ads/led_grid.png");

  /** LEDs per block along each side of a screen. */
  private static final int LEDS_PER_BLOCK = 8;

  /** How bright a lit screen is at night: signs are dimmed after dark. */
  private static final float NIGHT_DIM = 0.78F;

  @Override
  public void render(TileEntityAdBoard te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    World world = te.getWorld();
    IBlockState state = world.getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockAdBoard)) {
      return;
    }
    AdBoardKind kind = ((BlockAdBoard) state.getBlock()).kind();
    EnumFacing facing = state.getValue(AbstractBlockAdBoard.FACING);

    double frame = kind.getFramePx() / 16.0;
    int column = te.getControllerColumn();
    Face face = new Face(-column + frame, kind.getServiceRows() + frame,
        te.getWidth() - column - frame, te.getHeight() - frame);
    if (face.top <= face.bottom || face.right <= face.left) {
      // A billboard's controller on its own, before its screen has built it: all service row.
      return;
    }

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.disableLighting();

    float sun = world.getSunBrightness(partialTicks);
    boolean lit = te.getLight().isLit(sun, te.isPowered());
    boolean screen = kind == AdBoardKind.DIGITAL_BILLBOARD;
    int light = lit ? 0xF000F0 : te.lightmap();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >>> 16);
    float bright = lit && screen && sun < 0.6F ? NIGHT_DIM : 1F;

    long time = world.getTotalWorldTime();
    double front = kind.getFacePx() / 16.0;
    double back = kind.getBackFacePx() / 16.0;
    boolean hasBack = kind.isCabinet() && te.getBack() != AdBack.NONE;
    if (screen && !lit) {
      // A screen that is off is black, not an ad lit by the day.
      blank(face, front, false);
      if (hasBack) {
        blank(face, back, true);
      }
    } else {
      side(te, face, front, false, time, partialTicks, bright);
      if (hasBack) {
        side(te, face, back, true, time, partialTicks, bright);
      }
      if (screen) {
        grid(face, front, false);
        if (hasBack) {
          grid(face, back, true);
        }
      }
    }

    GlStateManager.color(1F, 1F, 1F, 1F);
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /** The face of the board, inside its frame, in the model frame. */
  private static final class Face {

    final double left;
    final double bottom;
    final double right;
    final double top;

    Face(double left, double bottom, double right, double top) {
      this.left = left;
      this.bottom = bottom;
      this.right = right;
      this.top = top;
    }

    double width() {
      return right - left;
    }

    double height() {
      return top - bottom;
    }
  }

  /**
   * One side of the board: the ad up now, and while the board is changing ads, the one going.
   */
  private static void side(TileEntityAdBoard te, Face face, double z, boolean back, long time,
      float partialTicks, float bright) {
    AdEntry now = back ? te.showingBack(time) : te.showing(time);
    AdTransition transition = te.getTransition();
    double p = 1.0;
    AdEntry was = null;
    if (te.getRotation() != AdRotation.SINGLE && transition != AdTransition.CUT && time > 0) {
      long step = te.stepTicks();
      p = AdTransition.progress(Math.floorMod(time, step) + partialTicks);
      if (p < 1.0) {
        was = back ? te.showingBack(time - step) : te.showing(time - step);
        if (was == now) {
          was = null;
        }
      }
    }
    if (was == null) {
      ad(now, te.getFit(), face, z, back, bright, 1F, 0, AD);
    } else if (transition == AdTransition.FADE) {
      ad(was, te.getFit(), face, z, back, bright, 1F, 0, AD);
      ad(now, te.getFit(), face, z, back, bright, (float) p, 0, FADING);
    } else {
      // A scroller: the old ad rolls up and out of the top as the new one rolls up from below.
      // They do not overlap, so they share a layer.
      ad(was, te.getFit(), face, z, back, bright, 1F, p, AD);
      ad(now, te.getFit(), face, z, back, bright, 1F, p - 1, AD);
    }
  }

  /**
   * One ad on one side: fitted into the face, faded to {@code alpha}, and moved up by
   * {@code shift} face heights and clipped to the face, for a slide. The back is seen from the
   * north, so it is wound the other way and its image runs from high x to low, or it would read
   * mirrored.
   */
  private static void ad(AdEntry ad, AdFit fit, Face face, double z, boolean back, float bright,
      float alpha, double shift, int layer) {
    double faceW = face.width();
    double faceH = face.height();
    AdShape shape = ad.shapeFor(faceW, faceH);
    double[] place = fit.place(faceW / faceH, shape.getAspect());
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    boolean blend = alpha < 1F;
    if (blend) {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
          GlStateManager.DestFactor.ZERO);
    }

    // The part of the face this ad covers after the shift, as fractions of the face's height.
    double lo = Math.max(0, shift);
    double hi = Math.min(1, 1 + shift);
    if (hi > lo) {
      if (fit == AdFit.CONTAIN) {
        // The letterbox: the ad's own background colour behind it.
        int bg = ad.getBackground();
        GlStateManager.disableTexture2D();
        GlStateManager.color(bright * ((bg >> 16) & 255) / 255F, bright * ((bg >> 8) & 255) / 255F,
            bright * (bg & 255) / 255F, alpha);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        layer(LETTERBOX);
        quad(buf, face.left, face.bottom + lo * faceH, face.right, face.bottom + hi * faceH, z,
            back);
        tessellator.draw();
        GlStateManager.enableTexture2D();
      }

      // The image's own extent on the face, bottom to top, moved by the shift and then clipped.
      double y0 = place[1] + shift;
      double y1 = place[3] + shift;
      double c0 = Math.max(y0, 0);
      double c1 = Math.min(y1, 1);
      if (c1 > c0) {
        // v runs from place[7] at the image's bottom to place[5] at its top.
        double v0 = place[7] + (place[5] - place[7]) * (c0 - y0) / (y1 - y0);
        double v1 = place[7] + (place[5] - place[7]) * (c1 - y0) / (y1 - y0);
        double yb = face.bottom + c0 * faceH;
        double yt = face.bottom + c1 * faceH;
        // Image left and right as seen by whoever looks at this face.
        double imageLeft = back ? face.right - place[0] * faceW : face.left + place[0] * faceW;
        double imageRight = back ? face.right - place[2] * faceW : face.left + place[2] * faceW;
        GlStateManager.color(bright, bright, bright, alpha);
        layer(layer);
        AdTextures.bind(ad.texture(shape));
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        if (back) {
          // imageRight is the lower x here: counter-clockwise as seen from the north.
          buf.pos(imageRight, yb, z).tex(place[6], v0).endVertex();
          buf.pos(imageRight, yt, z).tex(place[6], v1).endVertex();
          buf.pos(imageLeft, yt, z).tex(place[4], v1).endVertex();
          buf.pos(imageLeft, yb, z).tex(place[4], v0).endVertex();
        } else {
          buf.pos(imageLeft, yb, z).tex(place[4], v0).endVertex();
          buf.pos(imageRight, yb, z).tex(place[6], v0).endVertex();
          buf.pos(imageRight, yt, z).tex(place[6], v1).endVertex();
          buf.pos(imageLeft, yt, z).tex(place[4], v1).endVertex();
        }
        tessellator.draw();
      }
    }
    layer(0);
    if (blend) {
      GlStateManager.disableBlend();
    }
  }

  /**
   * A screen's LED grid, multiplied over the ad: dark lines between the LEDs, repeated
   * {@link #LEDS_PER_BLOCK} times a block. The texture is mipmapped, so from far off the grid
   * averages into a slight dimming rather than shimmering.
   */
  private static void grid(Face face, double z, boolean back) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR,
        GlStateManager.DestFactor.ZERO);
    GlStateManager.color(1F, 1F, 1F, 1F);
    AdTextures.bind(LED_GRID);
    double u = face.width() * LEDS_PER_BLOCK;
    double v = face.height() * LEDS_PER_BLOCK;
    double at = z;
    layer(GRID);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    if (back) {
      buf.pos(face.left, face.bottom, at).tex(u, v).endVertex();
      buf.pos(face.left, face.top, at).tex(u, 0).endVertex();
      buf.pos(face.right, face.top, at).tex(0, 0).endVertex();
      buf.pos(face.right, face.bottom, at).tex(0, v).endVertex();
    } else {
      buf.pos(face.left, face.bottom, at).tex(0, v).endVertex();
      buf.pos(face.right, face.bottom, at).tex(u, v).endVertex();
      buf.pos(face.right, face.top, at).tex(u, 0).endVertex();
      buf.pos(face.left, face.top, at).tex(0, 0).endVertex();
    }
    tessellator.draw();
    layer(0);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
  }

  /**
   * The layers a face is drawn in, nearest the cabinet first. Each is pulled toward the eye by
   * polygon offset, which is in depth-buffer units and so holds at any distance. A z gap does
   * not: the face at a quarter pixel off the cabinet held at thirty blocks and banded the ad block
   * by block at sixty, and a fading ad or the LED grid a thousandth of a block over the ad banded
   * sooner still.
   */
  private static final int LETTERBOX = 1;
  private static final int AD = 2;
  private static final int FADING = 3;
  private static final int GRID = 4;

  /** Draws what follows in layer {@code n}; 0 turns the offset off. */
  private static void layer(int n) {
    if (n == 0) {
      GlStateManager.doPolygonOffset(0F, 0F);
      GlStateManager.disablePolygonOffset();
    } else {
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(-1F * n, -10F * n);
    }
  }

  /** A screen that is switched off: the face black. */
  private static void blank(Face face, double z, boolean back) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    GlStateManager.disableTexture2D();
    GlStateManager.color(0.04F, 0.04F, 0.05F, 1F);
    layer(AD);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
    quad(buf, face.left, face.bottom, face.right, face.top, z, back);
    tessellator.draw();
    layer(0);
    GlStateManager.enableTexture2D();
  }

  private static void quad(BufferBuilder buf, double x0, double y0, double x1, double y1,
      double z, boolean back) {
    if (back) {
      buf.pos(x0, y0, z).endVertex();
      buf.pos(x0, y1, z).endVertex();
      buf.pos(x1, y1, z).endVertex();
      buf.pos(x1, y0, z).endVertex();
    } else {
      buf.pos(x0, y0, z).endVertex();
      buf.pos(x1, y0, z).endVertex();
      buf.pos(x1, y1, z).endVertex();
      buf.pos(x0, y1, z).endVertex();
    }
  }

  /**
   * Drawn whenever its box is in view, not only when the controller's own chunk section is: a
   * board reaches far past the section its controller is in, and culling it with that section
   * would drop a whole billboard out of the sky as its bottom corner went behind a hill.
   */
  @Override
  public boolean isGlobalRenderer(TileEntityAdBoard te) {
    return true;
  }
}
