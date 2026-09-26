package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import java.util.function.ToIntFunction;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
 * Draws what a parking meter's heads show, from the time left on each space.
 *
 * <ul>
 *   <li>A mechanical head shows a dial whose needle sweeps down as time runs out, and the red
 *   EXPIRED flag in its place once it has.</li>
 *   <li>A digital head shows the time left on its LCD, or EXPIRED.</li>
 *   <li>A pay station's screen shows how many of its spaces are paid.</li>
 * </ul>
 *
 * <p>Everything is computed from the expiry each frame, so a display counts down without a
 * packet. The model draws each head's housing; this draws only the window's contents, just
 * proud of the window, with the settle onto the road applied by hand. The pay station's screen
 * is lit; a meter's window takes the world's light.</p>
 *
 * <p><b>How it is drawn.</b> In two passes over the heads, each under one texture bound from
 * outside the lists (TRAFFIC_SIGNAL_SYSTEM.md, "Display lists: one texture, no cached state").
 * The first draws the windows: each head's rectangles (window, EXPIRED flag) are one
 * vertex-coloured list shared by every head of that look and size ({@link CsmSharedDisplayLists}),
 * and a mechanical needle is one list replayed under its sweep. The second draws the text: every
 * fixed text (TIME, EXPIRED, PAY HERE and a pay station's count) is a list keyed on the string it
 * shows and the head's size, its colour and the depth mask set outside; a digital reading, which
 * changes every second, is drawn live from a string its tile entity keeps per second. The windows
 * come first so no text is drawn before the rectangle behind it, as before.
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws everything per frame, head by head, as it
 * was drawn before the bake, for comparison.</p>
 *
 * @version 1.0
 */
public class TileEntityParkingMeterRenderer
    extends TileEntitySpecialRenderer<TileEntityParkingMeter> {

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final float CAP_SHARE = 0.72f;
  private static final int LIGHT_FULL = 240;

  /** A head's look, which decides its rectangles and its fixed texts. */
  private static final int LOOK_DIAL = 0;
  private static final int LOOK_FLAG = 1;
  private static final int LOOK_LCD = 2;
  private static final int LOOK_STATION = 3;

  /** Fixed texts, for their keys. The pay station's count keys on its numbers. */
  private static final int TEXT_TIME = 0;
  private static final int TEXT_EXPIRED_FLAG = 1;
  private static final int TEXT_EXPIRED_LCD = 2;
  private static final int TEXT_PAY_HERE = 3;
  private static final int TEXT_COUNT = 4;

  private static final CsmSharedDisplayLists WINDOW_LISTS =
      new CsmSharedDisplayLists("parking_meter_windows");
  private static final CsmSharedDisplayLists NEEDLE_LISTS =
      new CsmSharedDisplayLists("parking_meter_needles");
  private static final CsmSharedDisplayLists TEXT_LISTS =
      new CsmSharedDisplayLists("parking_meter_texts");

  /** Measures a reading for the tile entity's cache; a constant, so a frame allocates nothing. */
  private static final ToIntFunction<String> MEASURE =
      s -> CsmFontRenderer.highwayGothic().getStringWidth(s);

  /** Each head's look this frame: scratch, render thread only. */
  private static final int[] LOOKS = new int[2];

  @Override
  public void render(TileEntityParkingMeter te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockParkingMeter)) {
      return;
    }
    BlockParkingMeter meterBlock = (BlockParkingMeter) block;
    EnumFacing facing = state.getValue(BlockUtilityBox.FACING);
    long now = System.currentTimeMillis();
    boolean lit = meterBlock.getKind() == BlockParkingMeter.Kind.STATION;

    double settle = RoadSurfaceHeight.offsetFor(te.getWorld(), te.getPos());
    int combined = te.getWorld().getCombinedLight(te.getPos().offset(facing), 0);
    float previousX = OpenGlHelper.lastBrightnessX;
    float previousY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        lit ? LIGHT_FULL : combined & 0xFFFF, lit ? LIGHT_FULL : (combined >> 16) & 0xFFFF);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y + settle, z);
    GlStateManager.translate(0.5, 0.0, 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();

    float[][] heads = meterBlock.getHeads();
    if (CsmRenderToggles.sharedBakesPerFrame || heads.length > LOOKS.length) {
      renderPerFrame(te, meterBlock, heads, now);
    } else {
      renderBaked(te, meterBlock, heads, now);
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousX, previousY);
  }

  /** Moves to a head's window centre, just proud of it, turned so +x reads left to right. */
  private static void enterHead(float[] h) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(h[0], h[1], h[2] - 0.04f);
    GlStateManager.rotate(180, 0, 1, 0);
  }

  // ----------------------------------------------------------------------------------------
  // Baked: windows, then text
  // ----------------------------------------------------------------------------------------

  private void renderBaked(TileEntityParkingMeter te, BlockParkingMeter meterBlock,
      float[][] heads, long now) {
    BlockParkingMeter.Kind kind = meterBlock.getKind();
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    for (int i = 0; i < heads.length; i++) {
      float[] h = heads[i];
      float w = h[3];
      float ht = h[4];
      int look = lookOf(kind, te, i, now);
      LOOKS[i] = look;
      enterHead(h);
      int list = callOrCompile(WINDOW_LISTS, key(look, w, ht), 0, look, w, ht, null);
      if (list == CsmDisplayListCache.NO_LIST) {
        addWindow(look, w, ht);
      }
      if (look == LOOK_DIAL) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -ht * 0.32f, 0.01f);
        GlStateManager.rotate(needleAngle(te, i, now), 0, 0, 1);
        if (callOrCompile(NEEDLE_LISTS, key(0, w, ht), 1, 0, w, ht, null)
            == CsmDisplayListCache.NO_LIST) {
          addNeedle(w, ht);
        }
        GlStateManager.popMatrix();
      }
      GlStateManager.popMatrix();
    }
    // A direct draw resets the colour cache after its colour array; a replay does not.
    GlStateManager.resetColor();

    fr.bindAtlas();
    GlStateManager.enableTexture2D();
    GlStateManager.depthMask(false);
    for (int i = 0; i < heads.length; i++) {
      float[] h = heads[i];
      float w = h[3];
      float ht = h[4];
      enterHead(h);
      switch (LOOKS[i]) {
        case LOOK_DIAL:
          fixedText(fr, TEXT_TIME, "TIME", 0, -ht * 0.12f, w * 0.5f, ht * 0.14f, 0x505050, w,
              ht);
          break;
        case LOOK_FLAG:
          fixedText(fr, TEXT_EXPIRED_FLAG, "EXPIRED", 0, ht * 0.12f, w * 0.8f, ht * 0.26f,
              0xF4F4F0, w, ht);
          break;
        case LOOK_LCD:
          if (te.isExpired(i, now)) {
            fixedText(fr, TEXT_EXPIRED_LCD, "EXPIRED", 0, 0, w * 0.88f, ht * 0.55f, 0x15180F, w,
                ht);
          } else {
            String shown = te.reading(i, now, MEASURE);
            colour(0x15180F);
            addText(fr, shown, te.readingWidth(i), 0, 0, w * 0.88f, ht * 0.55f);
          }
          break;
        default:
          fixedText(fr, TEXT_PAY_HERE, "PAY HERE", 0, ht * 0.2f, w * 0.85f, ht * 0.26f,
              0xF2F2F2, w, ht);
          int spaces = te.getSpaces();
          int paid = paidOf(te, now);
          colour(0x8FD0FF);
          int id = TEXT_COUNT | (paid << 8) | (spaces << 16);
          if (callOrCompile(TEXT_LISTS, key(id, w, ht), 2, id, w, ht, fr)
              == CsmDisplayListCache.NO_LIST) {
            String text = paid + "/" + spaces + " PAID";
            addText(fr, text, fr.getStringWidth(text), 0, -ht * 0.2f, w * 0.85f, ht * 0.22f);
          }
          break;
      }
      GlStateManager.popMatrix();
    }
    GlStateManager.depthMask(true);
  }

  private static int lookOf(BlockParkingMeter.Kind kind, TileEntityParkingMeter te, int head,
      long now) {
    switch (kind) {
      case MECHANICAL:
        return te.isExpired(head, now) ? LOOK_FLAG : LOOK_DIAL;
      case DIGITAL:
        return LOOK_LCD;
      default:
        return LOOK_STATION;
    }
  }

  /**
   * Packs a list's key: what it is (a look, a needle, or a text id) and the head size it was laid
   * out for, in hundredths of a sixteenth (every head size is a tenth).
   */
  private static long key(int id, float w, float h) {
    return ((long) id << 32) | ((long) (Math.round(w * 100) & 0xFFFF) << 16)
        | (Math.round(h * 100) & 0xFFFF);
  }

  /**
   * Replays a list, compiling it the first time. kind 0 is a window, 1 a needle, 2 a text (its
   * id and the font given). Returns {@link CsmDisplayListCache#NO_LIST} if the driver refused a
   * list, and the caller draws directly.
   */
  private static int callOrCompile(CsmSharedDisplayLists lists, long key, int kind, int id,
      float w, float h, CsmFontRenderer fr) {
    int list = lists.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = lists.allocate(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        return list;
      }
      GL11.glNewList(list, GL11.GL_COMPILE);
      if (kind == 0) {
        addWindow(id, w, h);
      } else if (kind == 1) {
        addNeedle(w, h);
      } else {
        compileText(fr, id, w, h);
      }
      GL11.glEndList();
    }
    GL11.glCallList(list);
    return list;
  }

  /** A fixed text: its colour set here, its geometry replayed from its list. */
  private static void fixedText(CsmFontRenderer fr, int id, String text, float cx, float cy,
      float maxW, float capHeight, int colour, float w, float h) {
    colour(colour);
    if (callOrCompile(TEXT_LISTS, key(id, w, h), 2, id, w, h, fr)
        == CsmDisplayListCache.NO_LIST) {
      addText(fr, text, fr.getStringWidth(text), cx, cy, maxW, capHeight);
    }
  }

  /** The geometry of a text list, laid out exactly as {@link #fixedText} and the count ask. */
  private static void compileText(CsmFontRenderer fr, int id, float w, float h) {
    switch (id & 0xFF) {
      case TEXT_TIME:
        addText(fr, "TIME", fr.getStringWidth("TIME"), 0, -h * 0.12f, w * 0.5f, h * 0.14f);
        break;
      case TEXT_EXPIRED_FLAG:
        addText(fr, "EXPIRED", fr.getStringWidth("EXPIRED"), 0, h * 0.12f, w * 0.8f,
            h * 0.26f);
        break;
      case TEXT_EXPIRED_LCD:
        addText(fr, "EXPIRED", fr.getStringWidth("EXPIRED"), 0, 0, w * 0.88f, h * 0.55f);
        break;
      case TEXT_PAY_HERE:
        addText(fr, "PAY HERE", fr.getStringWidth("PAY HERE"), 0, h * 0.2f, w * 0.85f,
            h * 0.26f);
        break;
      default:
        String text = ((id >> 8) & 0xFF) + "/" + ((id >> 16) & 0xFF) + " PAID";
        addText(fr, text, fr.getStringWidth(text), 0, -h * 0.2f, w * 0.85f, h * 0.22f);
        break;
    }
  }

  /**
   * A head's rectangles, vertex-coloured, in one draw: the window, and the EXPIRED flag over a
   * dark window. Geometry only.
   */
  private static void addWindow(int look, float w, float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    switch (look) {
      case LOOK_DIAL:
        addQuad(buf, 0, 0, w, h, 0xE8E6DC, 0);
        break;
      case LOOK_FLAG:
        addQuad(buf, 0, 0, w, h, 0x1A1A1A, 0);
        addQuad(buf, 0, h * 0.12f, w * 0.86f, h * 0.56f, 0xC81E1E, 0.01f);
        break;
      case LOOK_LCD:
        addQuad(buf, 0, 0, w, h, 0x9DAF8E, 0);
        break;
      default:
        addQuad(buf, 0, 0, w, h, 0x13305C, 0);
        break;
    }
    tess.draw();
  }

  /** The dial's needle, pointing up from its pivot. Geometry only. */
  private static void addNeedle(float w, float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    addQuad(buf, 0, h * 0.3f, w * 0.06f, h * 0.6f, 0x202020, 0);
    tess.draw();
  }

  /** The vertices {@link #quad} draws, with its colour on each. */
  private static void addQuad(BufferBuilder buf, float cx, float cy, float w, float h,
      int colour, float dz) {
    int r = (colour >> 16) & 0xFF;
    int g = (colour >> 8) & 0xFF;
    int b = colour & 0xFF;
    buf.pos(cx - w / 2, cy - h / 2, dz).tex(0, 0).color(r, g, b, 255).endVertex();
    buf.pos(cx + w / 2, cy - h / 2, dz).tex(1, 0).color(r, g, b, 255).endVertex();
    buf.pos(cx + w / 2, cy + h / 2, dz).tex(1, 1).color(r, g, b, 255).endVertex();
    buf.pos(cx - w / 2, cy + h / 2, dz).tex(0, 1).color(r, g, b, 255).endVertex();
  }

  /**
   * One line laid out as {@link #text} lays it out, geometry and matrix only (the matrix calls
   * are not cached by {@code GlStateManager}, so they compile into a list): the caller binds the
   * atlas, sets the colour and holds the depth mask off.
   */
  private static void addText(CsmFontRenderer fr, String s, int width, float cx, float cy,
      float maxW, float capHeight) {
    float scale = capHeight / (fr.FONT_HEIGHT * CAP_SHARE);
    if (width * scale > maxW && width > 0) {
      scale = maxW / width;
    }
    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, 0.02f);
    GlStateManager.scale(scale, -scale, scale);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, s, -width / 2, -fr.FONT_HEIGHT / 2);
    tess.draw();
    GlStateManager.popMatrix();
  }

  private static void colour(int colour) {
    GlStateManager.color(((colour >> 16) & 0xFF) / 255.0f, ((colour >> 8) & 0xFF) / 255.0f,
        (colour & 0xFF) / 255.0f, 1.0f);
  }

  private static float needleAngle(TileEntityParkingMeter te, int space, long now) {
    // The needle sweeps from the right (full) to the left (empty) over the top of the dial.
    float full = te.getMaxMinutes() * 60_000f;
    float fraction = Math.max(0f, Math.min(1f, te.remaining(space, now) / full));
    return -70f + 140f * (1f - fraction);
  }

  private static int paidOf(TileEntityParkingMeter te, long now) {
    int paid = 0;
    for (int i = 0; i < te.getSpaces(); i++) {
      if (!te.isExpired(i, now)) {
        paid++;
      }
    }
    return paid;
  }

  // ----------------------------------------------------------------------------------------
  // Per frame: head by head, as drawn before the bake
  // ----------------------------------------------------------------------------------------

  private void renderPerFrame(TileEntityParkingMeter te, BlockParkingMeter meterBlock,
      float[][] heads, long now) {
    for (int i = 0; i < heads.length; i++) {
      float[] h = heads[i];
      enterHead(h);
      switch (meterBlock.getKind()) {
        case MECHANICAL:
          drawMechanical(te, i, now, h[3], h[4]);
          break;
        case DIGITAL:
          drawDigital(te, i, now, h[3], h[4]);
          break;
        default:
          drawStation(te, now, h[3], h[4]);
          break;
      }
      GlStateManager.popMatrix();
    }
  }

  private void drawMechanical(TileEntityParkingMeter te, int space, long now, float w, float h) {
    if (te.isExpired(space, now)) {
      quad(0, 0, w, h, 0x1A1A1A, 0);
      quad(0, h * 0.12f, w * 0.86f, h * 0.56f, 0xC81E1E, 0.01f);
      text("EXPIRED", 0, h * 0.12f, w * 0.8f, h * 0.26f, 0xF4F4F0, 0.02f);
      return;
    }
    quad(0, 0, w, h, 0xE8E6DC, 0);
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, -h * 0.32f, 0.01f);
    GlStateManager.rotate(needleAngle(te, space, now), 0, 0, 1);
    quad(0, h * 0.3f, w * 0.06f, h * 0.6f, 0x202020, 0);
    GlStateManager.popMatrix();
    text("TIME", 0, -h * 0.12f, w * 0.5f, h * 0.14f, 0x505050, 0.02f);
  }

  private void drawDigital(TileEntityParkingMeter te, int space, long now, float w, float h) {
    quad(0, 0, w, h, 0x9DAF8E, 0);
    String shown = te.isExpired(space, now) ? "EXPIRED"
        : ParkingPayments.remaining(te, space, now);
    text(shown, 0, 0, w * 0.88f, h * 0.55f, 0x15180F, 0.02f);
  }

  private void drawStation(TileEntityParkingMeter te, long now, float w, float h) {
    quad(0, 0, w, h, 0x13305C, 0);
    int paid = paidOf(te, now);
    text("PAY HERE", 0, h * 0.2f, w * 0.85f, h * 0.26f, 0xF2F2F2, 0.02f);
    text(paid + "/" + te.getSpaces() + " PAID", 0, -h * 0.2f, w * 0.85f, h * 0.22f, 0x8FD0FF,
        0.02f);
  }

  /** A flat rectangle centred at (cx, cy), in a colour, pushed dz toward the viewer. */
  private static void quad(float cx, float cy, float w, float h, int colour, float dz) {
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.color(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
        (colour & 0xFF) / 255f, 1.0f);
    BufferBuilder buf = Tessellator.getInstance().getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(cx - w / 2, cy - h / 2, dz).tex(0, 0).endVertex();
    buf.pos(cx + w / 2, cy - h / 2, dz).tex(1, 0).endVertex();
    buf.pos(cx + w / 2, cy + h / 2, dz).tex(1, 1).endVertex();
    buf.pos(cx - w / 2, cy + h / 2, dz).tex(0, 1).endVertex();
    Tessellator.getInstance().draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /** One line centred at (cx, cy), as tall as {@code capHeight}, shrunk to {@code maxW}. */
  private static void text(String s, float cx, float cy, float maxW, float capHeight,
      int colour, float dz) {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    int width = fr.getStringWidth(s);
    float scale = capHeight / (fr.FONT_HEIGHT * CAP_SHARE);
    if (width * scale > maxW && width > 0) {
      scale = maxW / width;
    }
    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, dz);
    GlStateManager.depthMask(false);
    GlStateManager.scale(scale, -scale, scale);
    fr.drawString(s, -width / 2, -fr.FONT_HEIGHT / 2, colour);
    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  private static float rotationFor(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90;
      case SOUTH:
        return 180;
      case EAST:
        return 270;
      case NORTH:
      default:
        return 0;
    }
  }
}
