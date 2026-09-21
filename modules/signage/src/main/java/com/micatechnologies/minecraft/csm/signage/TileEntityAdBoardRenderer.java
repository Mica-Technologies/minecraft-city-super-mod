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
    AdEntry ad = te.showing(world.getTotalWorldTime());

    double frame = kind.getFramePx() / 16.0;
    int column = te.getControllerColumn();
    double left = -column + frame;
    double right = te.getWidth() - column - frame;
    double bottom = frame;
    double top = te.getHeight() - frame;
    double faceW = right - left;
    double faceH = top - bottom;
    double depth = kind.getFacePx() / 16.0;

    AdShape shape = ad.shapeFor(faceW, faceH);
    AdFit fit = te.getFit();
    double[] place = fit.place(faceW / faceH, shape.getAspect());

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

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();

    if (fit == AdFit.CONTAIN) {
      // The letterbox: the ad's own background colour behind it, over the whole face.
      int bg = ad.getBackground();
      GlStateManager.disableTexture2D();
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      quadColour(buf, left, bottom, right, top, depth - 0.001, (bg >> 16) & 255, (bg >> 8) & 255,
          bg & 255);
      tessellator.draw();
      GlStateManager.enableTexture2D();
    }

    AdTextures.bind(ad.texture(shape));
    double x0 = left + place[0] * faceW;
    double x1 = left + place[2] * faceW;
    double y0 = bottom + place[1] * faceH;
    double y1 = bottom + place[3] * faceH;
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(x0, y0, depth).tex(place[4], place[7]).endVertex();
    buf.pos(x1, y0, depth).tex(place[6], place[7]).endVertex();
    buf.pos(x1, y1, depth).tex(place[6], place[5]).endVertex();
    buf.pos(x0, y1, depth).tex(place[4], place[5]).endVertex();
    tessellator.draw();

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  private static void quadColour(BufferBuilder buf, double x0, double y0, double x1, double y1,
      double z, int r, int g, int b) {
    buf.pos(x0, y0, z).color(r, g, b, 255).endVertex();
    buf.pos(x1, y0, z).color(r, g, b, 255).endVertex();
    buf.pos(x1, y1, z).color(r, g, b, 255).endVertex();
    buf.pos(x0, y1, z).color(r, g, b, 255).endVertex();
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
