package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
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
 * world light above the cabinet, as the railroad crossing gate's arm is.
 *
 * <p>The arm's shape depends on its length alone (3 to 6 blocks), so it is compiled once per
 * length into a list every gate shares ({@link CsmSharedDisplayLists}), in a vertex format with no
 * lightmap: the gate's light is set as GL state before the call, and the facing and the swing are
 * the matrix. The light and the arm's direction are read by the tile entity once a client tick,
 * not here every frame. Tessellated live it was some 80 quads a frame, each a varargs array.
 * {@link CsmRenderToggles#sharedBakesPerFrame} tessellates it live, for comparison.</p>
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

  /** One arm a length, keyed on {@link #armKey}. */
  private static final CsmSharedDisplayLists ARMS = new CsmSharedDisplayLists("parking_gate_arm");

  /**
   * The shared list key of an arm.
   *
   * @param length the arm's length in blocks
   *
   * @return the key
   */
  static long armKey(int length) {
    return length;
  }

  @Override
  public void render(TileEntityParkingGate te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te.getWorld() == null) {
      return;
    }
    EnumFacing along = te.getArmDirection();
    if (along == null) {
      return;
    }
    int length = te.getArmLength();
    float angle = te.getRenderAngle(partialTicks);
    int light = te.getArmLight();
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
    if (CsmRenderToggles.sharedBakesPerFrame) {
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      arm(buf, length, true, sky, blockLight);
      tessellator.draw();
    } else {
      // The light the live draw put in every vertex, as GL state: the list has no lightmap.
      float lastX = OpenGlHelper.lastBrightnessX;
      float lastY = OpenGlHelper.lastBrightnessY;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, blockLight, sky);
      long key = armKey(length);
      int list = ARMS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = ARMS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawShared(tessellator, length);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawShared(tessellator, length);
      }
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    }
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /** The arm for a shared list, in a format without a lightmap. Geometry only. */
  private static void drawShared(Tessellator tessellator, int length) {
    BufferBuilder buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    arm(buf, length, false, 0, 0);
    tessellator.draw();
  }

  /**
   * The counterweight, the hub, the bands and the tip, from the pivot along +x.
   *
   * @param lit whether the vertices carry the lightmap (the BLOCK format) or not
   *            ({@code POSITION_TEX_COLOR})
   */
  private static void arm(BufferBuilder buf, float length, boolean lit, int sky, int blockLight) {
    // the counterweight behind the pivot, and the hub
    box(buf, -0.45F, -0.09F, -0.06F, 0F, 0.09F, 0.06F, DARK, lit, sky, blockLight);
    box(buf, -0.06F, -0.11F, -0.08F, 0.06F, 0.11F, 0.08F, DARK, lit, sky, blockLight);
    // the arm in bands from the pivot, red first
    int bands = (int) Math.ceil((length - 0.3F) / BAND);
    for (int i = 0; i < bands; i++) {
      float x0 = 0.06F + i * BAND;
      float x1 = Math.min(length - 0.3F, x0 + BAND);
      box(buf, x0, -HALF_H, -HALF_D, x1, HALF_H, HALF_D, i % 2 == 0 ? RED : WHITE, lit, sky,
          blockLight);
    }
    // the tip: a red reflector, a little taller
    box(buf, length - 0.3F, -HALF_H - 0.01F, -HALF_D - 0.005F, length - 0.12F, HALF_H + 0.01F,
        HALF_D + 0.005F, RED, lit, sky, blockLight);
  }

  private static void box(BufferBuilder b, float x1, float y1, float z1, float x2, float y2,
      float z2, float[] c, boolean lit, int sky, int bl) {
    // -z, +z, -x, +x, -y, +y, each wound as it always was
    v(b, c, lit, sky, bl, x1, y1, z1);
    v(b, c, lit, sky, bl, x2, y1, z1);
    v(b, c, lit, sky, bl, x2, y2, z1);
    v(b, c, lit, sky, bl, x1, y2, z1);

    v(b, c, lit, sky, bl, x2, y1, z2);
    v(b, c, lit, sky, bl, x1, y1, z2);
    v(b, c, lit, sky, bl, x1, y2, z2);
    v(b, c, lit, sky, bl, x2, y2, z2);

    v(b, c, lit, sky, bl, x1, y1, z2);
    v(b, c, lit, sky, bl, x1, y1, z1);
    v(b, c, lit, sky, bl, x1, y2, z1);
    v(b, c, lit, sky, bl, x1, y2, z2);

    v(b, c, lit, sky, bl, x2, y1, z1);
    v(b, c, lit, sky, bl, x2, y1, z2);
    v(b, c, lit, sky, bl, x2, y2, z2);
    v(b, c, lit, sky, bl, x2, y2, z1);

    v(b, c, lit, sky, bl, x1, y1, z2);
    v(b, c, lit, sky, bl, x2, y1, z2);
    v(b, c, lit, sky, bl, x2, y1, z1);
    v(b, c, lit, sky, bl, x1, y1, z1);

    v(b, c, lit, sky, bl, x1, y2, z1);
    v(b, c, lit, sky, bl, x2, y2, z1);
    v(b, c, lit, sky, bl, x2, y2, z2);
    v(b, c, lit, sky, bl, x1, y2, z2);
  }

  private static void v(BufferBuilder b, float[] c, boolean lit, int sky, int bl, float x,
      float y, float z) {
    if (lit) {
      b.pos(x, y, z).color(c[0], c[1], c[2], 1.0F).tex(0.5F, 0.5F).lightmap(sky, bl)
          .endVertex();
    } else {
      b.pos(x, y, z).tex(0.5F, 0.5F).color(c[0], c[1], c[2], 1.0F).endVertex();
    }
  }
}
