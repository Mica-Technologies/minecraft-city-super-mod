package com.micatechnologies.minecraft.csm.streetscape;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a parking gate's arm (#302): from a pivot on the cabinet's shoulder out to the arm's
 * length in red and white bands, a short counterweight behind and a red reflector at the tip,
 * swung up by the tile entity's angle. Coloured quads on the one-pixel white texture with the
 * block's world light, as the railroad crossing gate's arm is; a few dozen quads a gate.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityParkingGateRenderer
    extends TileEntitySpecialRenderer<TileEntityParkingGate> {

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** The pivot, from the block's centre: toward the arm, up, and across. */
  private static final float PIVOT_ALONG = 0.22F;
  private static final float PIVOT_UP = 0.38F;
  private static final float BAND = 0.5F;
  private static final float HALF_H = 0.07F;
  private static final float HALF_D = 0.035F;

  private static final float[] RED = {0.80F, 0.10F, 0.09F};
  private static final float[] WHITE = {0.95F, 0.95F, 0.93F};
  private static final float[] DARK = {0.16F, 0.16F, 0.17F};

  @Override
  public void render(TileEntityParkingGate te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockParkingGate)) {
      return;
    }
    EnumFacing along = BlockParkingGate.armDirection(state);
    float length = te.getArmLength();
    float angle = te.getRenderAngle(partialTicks);
    int light = te.getWorld().getCombinedLight(te.getPos().up(), 0);
    int sky = (light >> 16) & 0xFFFF;
    int blockLight = light & 0xFFFF;

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
    // turn local +x onto the arm's direction
    GlStateManager.rotate(along == EnumFacing.NORTH ? 90F : along == EnumFacing.WEST ? 180F
        : along == EnumFacing.SOUTH ? -90F : 0F, 0F, 1F, 0F);
    GlStateManager.translate(PIVOT_ALONG, PIVOT_UP, 0F);
    GlStateManager.rotate(angle, 0F, 0F, 1F);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.disableLighting();

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // the counterweight behind the pivot, and the hub
    box(buf, -0.45F, -0.09F, -0.06F, 0F, 0.09F, 0.06F, DARK, sky, blockLight);
    box(buf, -0.06F, -0.11F, -0.08F, 0.06F, 0.11F, 0.08F, DARK, sky, blockLight);
    // the arm in bands from the pivot, red first
    int bands = (int) Math.ceil((length - 0.3F) / BAND);
    for (int i = 0; i < bands; i++) {
      float x0 = 0.06F + i * BAND;
      float x1 = Math.min(length - 0.3F, x0 + BAND);
      box(buf, x0, -HALF_H, -HALF_D, x1, HALF_H, HALF_D, i % 2 == 0 ? RED : WHITE, sky,
          blockLight);
    }
    // the tip: a red reflector, a little taller
    box(buf, length - 0.3F, -HALF_H - 0.01F, -HALF_D - 0.005F, length - 0.12F, HALF_H + 0.01F,
        HALF_D + 0.005F, RED, sky, blockLight);
    tessellator.draw();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  private static void box(BufferBuilder b, float x1, float y1, float z1, float x2, float y2,
      float z2, float[] c, int sky, int bl) {
    quad(b, c, sky, bl, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1);
    quad(b, c, sky, bl, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2);
    quad(b, c, sky, bl, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2);
    quad(b, c, sky, bl, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
    quad(b, c, sky, bl, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1);
    quad(b, c, sky, bl, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
  }

  private static void quad(BufferBuilder b, float[] c, int sky, int bl, float... p) {
    for (int i = 0; i < 12; i += 3) {
      b.pos(p[i], p[i + 1], p[i + 2]).color(c[0], c[1], c[2], 1.0F).tex(0.5F, 0.5F)
          .lightmap(sky, bl).endVertex();
    }
  }
}
