package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignAtlas;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Compiles a dynamic sign's legend -- text, shields, arrows and the plates between them -- into
 * display lists per position, <b>in the order it was drawn</b>.
 *
 * <p>A legend samples three textures (the white pixel, the sign atlas and the font atlas) and
 * switches between them element by element. A display list may hold only one texture, bound
 * outside it (see "Display lists: one texture, no cached state" in
 * {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}), so the legend is cut into a run of lists at every
 * texture change and replayed as that same run: bind, call, bind, call. Grouping the draws by
 * texture instead would have been fewer lists, but it reorders translucent glyph and atlas edges
 * against the opaque plates drawn after them, and where the two overlap -- a descender over the
 * next row's yellow patch -- the pixels move. Keeping the order keeps every pixel.</p>
 *
 * <p>Segment {@code i} of every position lives in its own {@link CsmDisplayListCache}, created the
 * first time any legend needs that many segments, so eviction, chunk-unload release and the
 * disconnect clear all come from the cache unchanged. The caller keeps the run's texture sequence
 * (one byte per segment, returned by {@link #endRecording()}) with the sign's data: it depends
 * only on what the legend says, never on the light the lists are keyed on.</p>
 *
 * <p>Drawing code calls {@link #use(byte)} before each draw and {@link #draw(Tessellator)} in
 * place of {@code Tessellator.draw()}. Live, {@code use} binds the texture and sets the depth
 * mask exactly as the per-frame code did (text never writes depth); recording, it only starts the
 * next list, so no cached {@code GlStateManager} call ever lands inside one.</p>
 *
 * <p>Render thread only, like the caches it uses.</p>
 */
@SideOnly(Side.CLIENT)
final class DynamicSignLegendLists {

  /** The white pixel: plates, patches, dividers. Depth writes on. */
  static final byte WHITE = 0;
  /** The sign atlas: shields, arrows, logos. Depth writes on. */
  static final byte ATLAS = 1;
  /** The legend font atlas. Depth writes OFF, as every legend string has always drawn. */
  static final byte FONT = 2;
  private static final byte NONE = -1;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  private final String name;
  private final List<CsmDisplayListCache> segmentCaches = new ArrayList<>();

  private boolean recording;
  /** Set when a list could not be allocated mid-recording: the rest of the pass is dropped. */
  private boolean discarding;
  private BlockPos recordPos;
  private long recordKey;
  private byte current = NONE;
  private byte[] recorded = new byte[16];
  private int recordedCount;
  /** Reused by {@link #replay} so a hit allocates nothing. */
  private int[] scratch = new int[16];

  DynamicSignLegendLists(String name) {
    this.name = name;
  }

  private CsmDisplayListCache segment(int index) {
    while (segmentCaches.size() <= index) {
      segmentCaches.add(new CsmDisplayListCache(name + "_" + segmentCaches.size()));
    }
    return segmentCaches.get(index);
  }

  /**
   * Replays the run compiled for a position and key, if every one of its lists is still resident.
   * Leaves the white pixel bound and depth writes on, as the live pass does.
   *
   * @param kinds the texture sequence recorded with the run, or null if none was recorded yet
   *
   * @return false, having drawn nothing, if the run must be (re)compiled
   */
  boolean replay(BlockPos pos, long key, byte[] kinds) {
    if (kinds == null) {
      return false;
    }
    if (scratch.length < kinds.length) {
      scratch = new int[kinds.length];
    }
    for (int i = 0; i < kinds.length; i++) {
      int id = i < segmentCaches.size() ? segmentCaches.get(i).get(pos, key)
          : CsmDisplayListCache.NO_LIST;
      if (id == CsmDisplayListCache.NO_LIST) {
        return false;
      }
      scratch[i] = id;
    }
    for (int i = 0; i < kinds.length; i++) {
      apply(kinds[i]);
      GL11.glCallList(scratch[i]);
      // The lists carry vertex colour, and a replay leaves GL's current colour at the last
      // vertex's without GlStateManager knowing; a direct draw resets it in its post-draw step.
      GlStateManager.resetColor();
    }
    finish();
    return true;
  }

  /** Starts compiling a run for a position and key. Follow with the drawing code, then end. */
  void beginRecording(BlockPos pos, long key) {
    recording = true;
    discarding = false;
    recordPos = pos;
    recordKey = key;
    recordedCount = 0;
    current = NONE;
  }

  /**
   * Closes the run.
   *
   * @return the run's texture sequence, or null if a list could not be allocated -- in which case
   *     nothing was drawn and every list of the position has been released
   */
  byte[] endRecording() {
    if (!discarding && current != NONE) {
      GL11.glEndList();
    }
    recording = false;
    current = NONE;
    if (discarding) {
      discarding = false;
      invalidate(recordPos);
      return null;
    }
    return Arrays.copyOf(recorded, recordedCount);
  }

  /** Starts a live pass: drawing code draws straight to the screen. */
  void beginLive() {
    recording = false;
    discarding = false;
    current = NONE;
  }

  /** Ends a live pass, leaving the white pixel bound and depth writes on. */
  void endLive() {
    current = NONE;
    finish();
  }

  /**
   * Declares the texture the next draw samples. Live, binds it (and sets the depth mask); while
   * recording, starts a new list whenever the texture changes.
   */
  void use(byte kind) {
    if (kind == current) {
      return;
    }
    if (!recording) {
      apply(kind);
      current = kind;
      return;
    }
    if (discarding) {
      current = kind;
      return;
    }
    if (current != NONE) {
      GL11.glEndList();
    }
    current = kind;
    int id = segment(recordedCount).allocate(recordPos, recordKey);
    if (id == CsmDisplayListCache.NO_LIST) {
      discarding = true;
      return;
    }
    GL11.glNewList(id, GL11.GL_COMPILE);
    if (recordedCount == recorded.length) {
      recorded = Arrays.copyOf(recorded, recorded.length * 2);
    }
    recorded[recordedCount++] = kind;
  }

  /** In place of {@code tess.draw()}; drops the vertices instead once a recording has failed. */
  void draw(Tessellator tess) {
    if (discarding) {
      BufferBuilder buf = tess.getBuffer();
      buf.finishDrawing();
      buf.reset();
      return;
    }
    tess.draw();
  }

  /**
   * One legend string in the guide sign font, as {@code GuideSignFontRenderer.drawString} drew it:
   * the font atlas, no depth write, colour and light in the vertices.
   */
  void text(String text, float leftX, float centerY, float z, float capHeightPx, int color,
      int sky, int block) {
    if (text == null || text.isEmpty() || !GuideSignFontRenderer.isAvailable()) {
      return;
    }
    use(FONT);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    GuideSignFontRenderer.addString(buf, text, leftX, centerY, z, capHeightPx, color, sky, block);
    draw(tess);
  }

  /** Releases every run compiled for a position. */
  void invalidate(BlockPos pos) {
    for (int i = 0; i < segmentCaches.size(); i++) {
      segmentCaches.get(i).invalidate(pos);
    }
  }

  private static void apply(byte kind) {
    GlStateManager.depthMask(kind != FONT);
    ResourceLocation texture = kind == FONT ? GuideSignFontRenderer.FONT_TEXTURE
        : kind == ATLAS ? GuideSignAtlas.ATLAS_TEXTURE : WHITE_TEXTURE;
    Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
  }

  private static void finish() {
    GlStateManager.depthMask(true);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }
}
