package com.micatechnologies.minecraft.csm.signage;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws an advertising board's ad: one textured quad across the whole board, inside its frame,
 * from the controller. The rest of the board -- backing and frame -- is baked block models, so a
 * board of any size costs one quad a frame (two when letterboxed).
 *
 * <p>Drawn in the frame the models are drawn in, the face to the south, and turned to the facing
 * as the blockstate turns them: {@code x} runs to the board's right seen from the front, from
 * {@code -controllerColumn} at its left edge, and the face stands {@link AdBoardKind#getFacePx()}
 * off the back of the block.</p>
 */
@SideOnly(Side.CLIENT)
public class TileEntityAdBoardRenderer extends TileEntitySpecialRenderer<TileEntityAdBoard> {

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
    double left = -column + frame;
    double right = te.getWidth() - column - frame;
    // Above any service row: a printed billboard's catwalk is under its face, not in front of it.
    double bottom = kind.getServiceRows() + frame;
    double top = te.getHeight() - frame;
    double depth = kind.getFacePx() / 16.0;
    if (top <= bottom || right <= left) {
      // A billboard's controller on its own, before its screen has built it: all service row.
      return;
    }

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.disableLighting();
    GlStateManager.color(1F, 1F, 1F, 1F);

    boolean lit = te.getLight().isLit(world.getSunBrightness(partialTicks), te.isPowered());
    int light = lit ? 0xF000F0 : te.lightmap();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >>> 16);

    long time = world.getTotalWorldTime();
    face(te.showing(time), te.getFit(), left, bottom, right, top, depth, false);
    if (kind.isCabinet() && te.getBack() != AdBack.NONE) {
      face(te.showingBack(time), te.getFit(), left, bottom, right, top,
          kind.getBackFacePx() / 16.0, true);
    }

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * One face of the board: the ad fitted into {@code left..right} by {@code bottom..top} at depth
   * {@code z}. The back is seen from the north, so it is wound the other way and its image runs
   * from high x to low, or it would read mirrored.
   */
  private static void face(AdEntry ad, AdFit fit, double left, double bottom, double right,
      double top, double z, boolean back) {
    double faceW = right - left;
    double faceH = top - bottom;
    AdShape shape = ad.shapeFor(faceW, faceH);
    double[] place = fit.place(faceW / faceH, shape.getAspect());
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    double lift = back ? 0.001 : -0.001;

    if (fit == AdFit.CONTAIN) {
      // The letterbox: the ad's own background colour behind it, over the whole face.
      int bg = ad.getBackground();
      GlStateManager.disableTexture2D();
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      quadColour(buf, left, bottom, right, top, z + lift, back, (bg >> 16) & 255,
          (bg >> 8) & 255, bg & 255);
      tessellator.draw();
      GlStateManager.enableTexture2D();
    }

    AdTextures.bind(ad.texture(shape));
    // Image left and right as seen by whoever looks at this face.
    double imageLeft = back ? right - place[0] * faceW : left + place[0] * faceW;
    double imageRight = back ? right - place[2] * faceW : left + place[2] * faceW;
    double y0 = bottom + place[1] * faceH;
    double y1 = bottom + place[3] * faceH;
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    if (back) {
      // imageRight is the lower x here: counter-clockwise as seen from the north.
      buf.pos(imageRight, y0, z).tex(place[6], place[7]).endVertex();
      buf.pos(imageRight, y1, z).tex(place[6], place[5]).endVertex();
      buf.pos(imageLeft, y1, z).tex(place[4], place[5]).endVertex();
      buf.pos(imageLeft, y0, z).tex(place[4], place[7]).endVertex();
    } else {
      buf.pos(imageLeft, y0, z).tex(place[4], place[7]).endVertex();
      buf.pos(imageRight, y0, z).tex(place[6], place[7]).endVertex();
      buf.pos(imageRight, y1, z).tex(place[6], place[5]).endVertex();
      buf.pos(imageLeft, y1, z).tex(place[4], place[5]).endVertex();
    }
    tessellator.draw();
  }

  private static void quadColour(BufferBuilder buf, double x0, double y0, double x1, double y1,
      double z, boolean back, int r, int g, int b) {
    if (back) {
      buf.pos(x0, y0, z).color(r, g, b, 255).endVertex();
      buf.pos(x0, y1, z).color(r, g, b, 255).endVertex();
      buf.pos(x1, y1, z).color(r, g, b, 255).endVertex();
      buf.pos(x1, y0, z).color(r, g, b, 255).endVertex();
    } else {
      buf.pos(x0, y0, z).color(r, g, b, 255).endVertex();
      buf.pos(x1, y0, z).color(r, g, b, 255).endVertex();
      buf.pos(x1, y1, z).color(r, g, b, 255).endVertex();
      buf.pos(x0, y1, z).color(r, g, b, 255).endVertex();
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
