package com.micatechnologies.minecraft.csm.healthcare.queue;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the now-serving display's screen: its texture again, lit, and on it NOW SERVING across
 * the header band, the number in big figures, and the desk under it.
 *
 * <p>Built as the ED tracking board's renderer is: each piece of text is a display list shared by
 * every display ({@link CsmSharedDisplayLists}), compiled the first time it is shown and keyed by
 * a bounded value (the number 0-999, the desk 0-99), centred on the screen in the font's units.
 * Following "Display lists: one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md},
 * the lists hold geometry only: the font atlas, the colours, the depth mask and the fullbright
 * lightmap are set outside them, every frame. The tile entity's render range stops it past 48
 * blocks ({@link TileEntityNowServingDisplay#getMaxRenderDistanceSquared}).</p>
 *
 * <p>The screen's place and bands are {@code gen_healthcare_patient_areas.py}'s ({@code NS_W},
 * {@code NS_H}, {@code NS_SCREEN_Z}, {@code NS_HEADER}): change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityNowServingDisplayRenderer
    extends TileEntitySpecialRenderer<TileEntityNowServingDisplay> {

  private static final float SCREEN_LEFT_X = 15.0f;
  private static final float SCREEN_TOP_Y = 12.5f;
  private static final float SCREEN_Z = 13.7f;
  static final float WIDTH = 14.0f;
  private static final float HEIGHT = 9.0f;
  static final float HEADER = 2.0f;
  static final float HEAD_TEXT_H = 1.1f;
  static final float NUMBER_TEXT_H = 4.6f;
  static final float NUMBER_TOP = 2.6f;
  static final float DESK_TEXT_H = 1.0f;
  static final float DESK_TOP = 7.55f;
  private static final float SCREEN_LIFT = 0.02f;
  private static final float TEXT_LIFT = 0.02f;

  static final int HEAD_COLOUR = 0xFFFFFF;
  static final int NUMBER_COLOUR = 0xFFB428;
  static final int DESK_COLOUR = 0xBFE4F2;

  private static final CsmSharedDisplayLists LISTS = new CsmSharedDisplayLists(
      "now_serving_text");

  private static final int KIND_HEAD = 0;
  private static final int KIND_NUMBER = 1;
  private static final int KIND_DESK = 2;

  @Override
  public void render(TileEntityNowServingDisplay te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate((8 - SCREEN_LEFT_X) / 16.0, SCREEN_TOP_Y / 16.0,
        (8 - SCREEN_Z) / 16.0);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    screen();

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    colour(HEAD_COLOUR);
    at((HEADER - HEAD_TEXT_H) / 2, HEAD_TEXT_H, fr);
    draw(fr, KIND_HEAD, 0);
    GlStateManager.popMatrix();
    colour(NUMBER_COLOUR);
    at(NUMBER_TOP, NUMBER_TEXT_H, fr);
    draw(fr, KIND_NUMBER, te.getNumber());
    GlStateManager.popMatrix();
    if (te.getDesk() > 0) {
      colour(DESK_COLOUR);
      at(DESK_TOP, DESK_TEXT_H, fr);
      draw(fr, KIND_DESK, te.getDesk());
      GlStateManager.popMatrix();
    }

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** The screen's texture, one quad a hair in front of the baked one, fullbright. */
  private void screen() {
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
        "csm:blocks/healthcare/patient/now_serving_screen");
    float u0 = sprite.getMinU();
    float u1 = sprite.getMaxU();
    float v0 = sprite.getMinV();
    float v1 = sprite.getInterpolatedV(16.0 * HEIGHT / WIDTH);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(0, HEIGHT, SCREEN_LIFT).tex(u0, v1).endVertex();
    buf.pos(WIDTH, HEIGHT, SCREEN_LIFT).tex(u1, v1).endVertex();
    buf.pos(WIDTH, 0, SCREEN_LIFT).tex(u1, v0).endVertex();
    buf.pos(0, 0, SCREEN_LIFT).tex(u0, v0).endVertex();
    tess.draw();
  }

  /** Moves to a line's top and scales the font to its height; the caller pops. */
  private static void at(float top, float height, CsmFontRenderer fr) {
    float scale = height / fr.FONT_HEIGHT;
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, top, 0);
    GlStateManager.scale(scale, scale, 1);
  }

  private static void colour(int rgb) {
    GlStateManager.color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
        (rgb & 0xFF) / 255f, 1.0f);
  }

  /** Replays a text's list, compiling it the first time. */
  private static void draw(CsmFontRenderer fr, int kind, int value) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value);
      return;
    }
    long key = ((long) kind << 32) | (value & 0xFFFFFFFFL);
    int list = LISTS.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        add(fr, kind, value);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      add(fr, kind, value);
    }
  }

  /** The text a kind and value show. */
  static String text(int kind, int value) {
    switch (kind) {
      case KIND_HEAD:
        return "NOW SERVING";
      case KIND_NUMBER:
        return value <= 0 ? "---" : String.format("%03d", value);
      default:
        return "DESK " + value;
    }
  }

  private static float height(int kind) {
    return kind == KIND_HEAD ? HEAD_TEXT_H : kind == KIND_NUMBER ? NUMBER_TEXT_H : DESK_TEXT_H;
  }

  /** One line centred across the screen, in the font's units at its height. Geometry only. */
  private static void add(CsmFontRenderer fr, int kind, int value) {
    String s = text(kind, value);
    float scale = height(kind) / fr.FONT_HEIGHT;
    int width = fr.getStringWidth(s);
    int x = Math.round(WIDTH / 2 / scale - width / 2.0f);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, s, x, 0);
    tess.draw();
  }
}
