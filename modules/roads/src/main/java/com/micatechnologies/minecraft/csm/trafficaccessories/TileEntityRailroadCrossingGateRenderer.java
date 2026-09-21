package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.block.Block;
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
 * Draws the gate arm: red and white bands from the pivot out across the road, the
 * counterweight behind, and the three lamps -- the tip lamp steady and the inner pair
 * alternating once a second while the arm is anywhere but up, as a real gate's do. The arm
 * swings about the pivot on the cabinet's side, the angle coming from the tile entity per
 * frame.
 *
 * <p>The bands are plain coloured quads on the one-pixel white texture, lit by the world light
 * at the block, so the arm is shaded like everything around it; only the lamps are drawn
 * full-bright.</p>
 *
 * <p>Nothing in the arm depends on where the gate stands except its light, and the lamps have
 * only three looks (dark, or lit in either half of the flash), so the whole arm is compiled
 * into a list shared by every gate that looks the same ({@link CsmSharedDisplayLists}) and
 * replayed under each gate's own facing and swing rotation. The light stays baked into the
 * vertices and is part of the key, so the vertex data is exactly what was drawn per frame.
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws it per frame, for comparison.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityRailroadCrossingGateRenderer
    extends TileEntitySpecialRenderer<TileEntityRailroadCrossingGate> {

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  // Pivot on the cabinet's right shoulder, block-local, relative to the block centre
  private static final float PIVOT_X = 0.3125F;
  private static final float PIVOT_Y = 0.125F;
  private static final float BAND = 0.5F;        // 16 in bands
  private static final float ARM_HALF_H = 0.09F;
  private static final float ARM_HALF_D = 0.045F;
  private static final float COUNTERWEIGHT_LEN = 0.75F;

  private static final float[] RED = {0.82F, 0.08F, 0.08F};
  private static final float[] WHITE = {0.95F, 0.95F, 0.93F};
  private static final float[] BLACK = {0.10F, 0.10F, 0.10F};
  private static final float[] LAMP_LIT = {1.0F, 0.15F, 0.10F};
  private static final float[] LAMP_DARK = {0.30F, 0.05F, 0.04F};

  /** The lamps' three looks: all dark, or lit with the inner pair in either flash half. */
  private static final int LAMPS_DARK = 0;
  private static final int LAMPS_FIRST_HALF = 1;
  private static final int LAMPS_SECOND_HALF = 2;

  /**
   * The whole arm, one list per look. Key layout (a {@code long}):
   * <ul>
   *   <li>bits 0-31: the combined light at the block above, as {@code getCombinedLight} returns
   *   it (sky light in the high half, block light in the low half; baked into the vertices)</li>
   *   <li>bits 32-33: the lamp look ({@link #LAMPS_DARK}, {@link #LAMPS_FIRST_HALF},
   *   {@link #LAMPS_SECOND_HALF})</li>
   *   <li>bits 34 and up: the block id, which fixes the arm length</li>
   * </ul>
   */
  private static final CsmSharedDisplayLists ARM_LISTS =
      new CsmSharedDisplayLists("crossing_gate_arm");

  @Override
  public void render(TileEntityRailroadCrossingGate te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockRailroadCrossingGate)) {
      return;
    }
    float length = ((BlockRailroadCrossingGate) block).getArmLength();
    EnumFacing facing = state.getValue(AbstractBlockRailroadCrossing.FACING);
    float angle = te.getRenderAngle(partialTicks);

    int light = te.getWorld().getCombinedLight(te.getLightPos(), 0);

    // The state read above is the gate's own, so its POWERED is what isActive would read again
    boolean lampsOn = te.isArmActive(state.getValue(AbstractBlockRailroadCrossing.POWERED));
    long millis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
    boolean firstHalf = Math.floorMod(millis, 1000L) < 500L;
    int lamps = !lampsOn ? LAMPS_DARK : firstHalf ? LAMPS_FIRST_HALF : LAMPS_SECOND_HALF;

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
    applyFacingRotation(facing);
    GlStateManager.translate(PIVOT_X, PIVOT_Y, 0F);
    GlStateManager.rotate(angle, 0F, 0F, 1F);

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.disableLighting();

    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawArm(length, light, lamps);
    } else {
      long key = ((long) Block.getIdFromBlock(block) << 34) | ((long) lamps << 32)
          | (light & 0xFFFFFFFFL);
      int list = ARM_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = ARM_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawArm(length, light, lamps);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawArm(length, light, lamps);
      }
    }

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * Draws the counterweight, the banded arm and the three lamps, in the arm's pivot space.
   * Geometry only: the caller owns the texture and every GL state.
   *
   * @param length the arm length, in blocks
   * @param light  the combined light the world-lit parts are drawn with
   * @param lamps  the lamp look
   */
  private static void drawArm(float length, int light, int lamps) {
    int sky = (light >> 16) & 0xFFFF;
    int blockLight = light & 0xFFFF;
    boolean lampsOn = lamps != LAMPS_DARK;
    boolean firstHalf = lamps == LAMPS_FIRST_HALF;

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    // Counterweight: a short black stub with the weight box on its end
    emitBox(buf, -COUNTERWEIGHT_LEN, -ARM_HALF_H, -ARM_HALF_D, 0F, ARM_HALF_H, ARM_HALF_D,
        BLACK, sky, blockLight);
    emitBox(buf, -COUNTERWEIGHT_LEN - 0.35F, -0.18F, -0.14F, -COUNTERWEIGHT_LEN, 0.18F, 0.14F,
        BLACK, sky, blockLight);
    // The arm, in alternating bands starting red at the pivot
    int bands = (int) Math.ceil(length / BAND);
    for (int i = 0; i < bands; i++) {
      float x0 = i * BAND;
      float x1 = Math.min(length, x0 + BAND);
      emitBox(buf, x0, -ARM_HALF_H, -ARM_HALF_D, x1, ARM_HALF_H, ARM_HALF_D,
          (i % 2 == 0) ? RED : WHITE, sky, blockLight);
    }
    tessellator.draw();

    // Lamps: full-bright when lit, on the road-facing side of the arm
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    float[] lampX = {length - 0.35F, length * 0.62F, length * 0.32F};
    for (int i = 0; i < lampX.length; i++) {
      boolean lit = lampsOn && (i == 0 || (i == 1) == firstHalf);
      float[] colour = lit ? LAMP_LIT : LAMP_DARK;
      int ls = lit ? 240 : sky;
      int lb = lit ? 240 : blockLight;
      emitBox(buf, lampX[i] - 0.08F, -0.07F, ARM_HALF_D, lampX[i] + 0.08F, 0.07F,
          ARM_HALF_D + 0.05F, colour, ls, lb);
      emitBox(buf, lampX[i] - 0.08F, -0.07F, -ARM_HALF_D - 0.05F, lampX[i] + 0.08F, 0.07F,
          -ARM_HALF_D, colour, ls, lb);
    }
    tessellator.draw();
  }

  private static void emitBox(BufferBuilder buf, float x1, float y1, float z1, float x2,
      float y2, float z2, float[] c, int sky, int block) {
    // -Z
    emit(buf, x1, y1, z1, c, sky, block);
    emit(buf, x2, y1, z1, c, sky, block);
    emit(buf, x2, y2, z1, c, sky, block);
    emit(buf, x1, y2, z1, c, sky, block);
    // +Z
    emit(buf, x2, y1, z2, c, sky, block);
    emit(buf, x1, y1, z2, c, sky, block);
    emit(buf, x1, y2, z2, c, sky, block);
    emit(buf, x2, y2, z2, c, sky, block);
    // -X
    emit(buf, x1, y1, z2, c, sky, block);
    emit(buf, x1, y1, z1, c, sky, block);
    emit(buf, x1, y2, z1, c, sky, block);
    emit(buf, x1, y2, z2, c, sky, block);
    // +X
    emit(buf, x2, y1, z1, c, sky, block);
    emit(buf, x2, y1, z2, c, sky, block);
    emit(buf, x2, y2, z2, c, sky, block);
    emit(buf, x2, y2, z1, c, sky, block);
    // -Y
    emit(buf, x1, y1, z2, c, sky, block);
    emit(buf, x2, y1, z2, c, sky, block);
    emit(buf, x2, y1, z1, c, sky, block);
    emit(buf, x1, y1, z1, c, sky, block);
    // +Y
    emit(buf, x1, y2, z1, c, sky, block);
    emit(buf, x2, y2, z1, c, sky, block);
    emit(buf, x2, y2, z2, c, sky, block);
    emit(buf, x1, y2, z2, c, sky, block);
  }

  private static void emit(BufferBuilder buf, float x, float y, float z, float[] c, int sky,
      int block) {
    buf.pos(x, y, z).color(c[0], c[1], c[2], 1.0F).tex(0.5F, 0.5F).lightmap(sky, block)
        .endVertex();
  }

  private static void applyFacingRotation(EnumFacing facing) {
    switch (facing) {
      case SOUTH:
        GlStateManager.rotate(180F, 0F, 1F, 0F);
        break;
      case EAST:
        GlStateManager.rotate(-90F, 0F, 1F, 0F);
        break;
      case WEST:
        GlStateManager.rotate(90F, 0F, 1F, 0F);
        break;
      default:
        break;
    }
  }
}
