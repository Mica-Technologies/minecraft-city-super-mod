package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Pictogram;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on a face of a hospital wayfinding panel goes: the background in the scheme's
 * colour, the pictogram on its square, the arrow and the legend's glyphs, each a rectangle in the
 * reader's space (x along the panel to the reader's right from its left edge, y up from its
 * bottom, in blocks) with the sheet position it shows, as an affine map from that space. Kept free
 * of the client so it can be tested; {@link HospitalWayfindingBakedModel} clips the pieces to a
 * cell and bakes them.
 *
 * <p>The proportions are Transit's large hanging sign's, so the two read alike: the pictogram a
 * square the panel's height less its margins at the end away from the arrow, the arrow 0.8 of it
 * at the end it points to, the text left-aligned between them, its capitals 0.45 of the panel's
 * height on one line or 0.28 each on two, both lines scaled down together to fit; a long legend
 * first takes room from the pictogram and arrow, down to 0.55 of their size.</p>
 *
 * @since 2026.10
 */
public final class HospitalWayfindingLayout {

  /** The frame's width on the panel's outer edges (the generator's PANEL_FRAME), in blocks. */
  public static final float FRAME = 0.8f / 16f;
  private static final float CAPS_ONE_LINE = 0.45f;
  private static final float CAPS_TWO_LINES = 0.28f;
  /** Baseline to baseline, in cap heights (Transit's 0.85 line height over its 0.45 caps). */
  private static final float LINE_STEP = 0.85f / 0.45f;
  private static final float ARROW_SHARE = 0.8f;
  private static final float MIN_ART_SHARE = 0.55f;

  /** A piece is drawn on the background (0) or on top of it (1). */
  public static final int LAYER_BACKGROUND = 0;
  public static final int LAYER_ART = 1;

  private HospitalWayfindingLayout() {
  }

  /** One rectangle of a face, and the sheet it shows: u = ua x + ub y + uc, v likewise (px). */
  public static final class Piece {

    public final float x0;
    public final float y0;
    public final float x1;
    public final float y1;
    public final float ua;
    public final float ub;
    public final float uc;
    public final float va;
    public final float vb;
    public final float vc;
    public final int rgb;
    public final int layer;

    Piece(float x0, float y0, float x1, float y1, float ua, float ub, float uc, float va,
        float vb, float vc, int rgb, int layer) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.ua = ua;
      this.ub = ub;
      this.uc = uc;
      this.va = va;
      this.vb = vb;
      this.vc = vc;
      this.rgb = rgb;
      this.layer = layer;
    }

    public float u(float x, float y) {
      return ua * x + ub * y + uc;
    }

    public float v(float x, float y) {
      return va * x + vb * y + vc;
    }

    /** This piece cut to a rectangle, or null if none of it is inside. */
    public Piece clip(float cx0, float cy0, float cx1, float cy1) {
      float nx0 = Math.max(x0, cx0);
      float ny0 = Math.max(y0, cy0);
      float nx1 = Math.min(x1, cx1);
      float ny1 = Math.min(y1, cy1);
      if (nx1 - nx0 <= 1e-5f || ny1 - ny0 <= 1e-5f) {
        return null;
      }
      return new Piece(nx0, ny0, nx1, ny1, ua, ub, uc, va, vb, vc, rgb, layer);
    }
  }

  /**
   * A face's pieces.
   *
   * @param f     the cell's view of its panel
   * @param arrow the arrow this face shows (the back's is mirrored)
   * @return the pieces, background first
   */
  public static List<Piece> face(HospitalWayfindingFace f, Arrow arrow) {
    List<Piece> out = new ArrayList<>();
    int width = f.width;
    int height = f.height;
    int[] white = HospitalWayfindingGlyphs.WHITE;
    float wu = white[0] + white[2] / 2f;
    float wv = white[1] + white[2] / 2f;
    out.add(new Piece(FRAME, FRAME, width - FRAME, height - FRAME, 0, 0, wu, 0, 0, wv,
        f.scheme.getBackground(), LAYER_BACKGROUND));

    String[] lines;
    if (f.line1.isEmpty() && f.line2.isEmpty()) {
      lines = new String[0];
    } else if (f.line1.isEmpty() || f.line2.isEmpty()) {
      lines = new String[]{f.line1.isEmpty() ? f.line2 : f.line1};
    } else {
      lines = new String[]{f.line1, f.line2};
    }
    int n = lines.length;
    float widest = 0;
    for (String line : lines) {
      widest = Math.max(widest, lineWidth(line));
    }
    boolean pictogram = f.pictogram != Pictogram.NONE
        && HospitalWayfindingGlyphs.pictogram(f.pictogram.getId()) != null;
    boolean hasArrow = arrow != Arrow.NONE;
    float margin = FRAME + 0.08f * height;
    float fullSquare = height - 2 * margin;
    float gap = margin;
    float cap = n == 0 ? 0 : (n == 1 ? CAPS_ONE_LINE : CAPS_TWO_LINES) * height;

    float perSquare = (pictogram ? 1f : 0f) + (hasArrow ? ARROW_SHARE : 0f);
    float room = width - 2 * margin - (pictogram ? fullSquare + gap : 0)
        - (hasArrow ? fullSquare * ARROW_SHARE + gap : 0);
    float shortBy = widest * cap - room;
    float square = fullSquare;
    if (shortBy > 0 && perSquare > 0) {
      square = Math.max(fullSquare * MIN_ART_SHARE, fullSquare - shortBy / perSquare);
    }
    float arrowSize = square * ARROW_SHARE;
    float mid = height / 2f;

    boolean left = hasArrow && arrow.isLeftEnd();
    float xl = margin;
    float xr = width - margin;
    float pictX;
    float arrowX;
    if (left) {
      arrowX = xl;
      if (hasArrow) {
        xl += arrowSize + gap;
      }
      pictX = xr - square;
      if (pictogram) {
        xr -= square + gap;
      }
    } else {
      arrowX = xr - arrowSize;
      if (hasArrow) {
        xr -= arrowSize + gap;
      }
      pictX = xl;
      if (pictogram) {
        xl += square + gap;
      }
    }

    if (pictogram) {
      int[] cell = HospitalWayfindingGlyphs.pictogram(f.pictogram.getId());
      float y0 = mid - square / 2f;
      float k = (HospitalWayfindingGlyphs.PICTO - 1) / square;
      // u = cell.x + 0.5 + (x - pictX) k ; v = cell.y + 0.5 + (y0 + square - y) k
      out.add(new Piece(pictX, y0, pictX + square, y0 + square, k, 0,
          cell[0] + 0.5f - pictX * k, 0, -k, cell[1] + 0.5f + (y0 + square) * k,
          f.scheme.getPictogram(), LAYER_ART));
    }
    if (hasArrow) {
      out.add(arrowPiece(arrow, arrowX + arrowSize / 2f, mid, arrowSize,
          f.scheme.getLegend()));
    }

    float textWidth = xr - xl;
    if (n > 0 && widest > 0 && textWidth > 0) {
      if (widest * cap > textWidth) {
        cap = textWidth / widest;
      }
      float step = cap * LINE_STEP;
      float total = cap + (n - 1) * step;
      float top = mid + total / 2f;
      for (int i = 0; i < n; i++) {
        glyphs(out, lines[i], xl, top - i * step, cap, f.scheme.getLegend());
      }
    }
    return out;
  }

  /** A line's width in cap heights. */
  static float lineWidth(String line) {
    float px = 0;
    for (int i = 0; i < line.length(); i++) {
      int o = HospitalWayfindingGlyphs.offset(line.charAt(i));
      if (o >= 0) {
        px += HospitalWayfindingGlyphs.GLYPHS[o + 6] / 16f;
      }
    }
    return px / HospitalWayfindingGlyphs.CAP;
  }

  private static void glyphs(List<Piece> out, String line, float x, float capTop, float cap,
      int rgb) {
    int[] g = HospitalWayfindingGlyphs.GLYPHS;
    float k = cap / HospitalWayfindingGlyphs.CAP;       // blocks per sheet px
    float pen = x;
    for (int i = 0; i < line.length(); i++) {
      int o = HospitalWayfindingGlyphs.offset(line.charAt(i));
      if (o < 0) {
        continue;
      }
      int w = g[o + 2];
      int h = g[o + 3];
      if (w > 0 && h > 0) {
        float bx0 = pen + g[o + 4] * k;
        float top = capTop - g[o + 5] * k;
        float inv = 1f / k;
        out.add(new Piece(bx0, top - h * k, bx0 + w * k, top, inv, 0, g[o] - bx0 * inv, 0, -inv,
            g[o + 1] + top * inv, rgb, LAYER_ART));
      }
      pen += g[o + 6] / 16f * k;
    }
  }

  /**
   * The arrow's square, centred on (cx, cy), size s: the straight sprite (pointing right) for the
   * four square directions, the diagonal (up and right) for the others, turned by a multiple of
   * 90 degrees through its sheet mapping.
   */
  private static Piece arrowPiece(Arrow arrow, float cx, float cy, float s, int rgb) {
    int angle = arrow.getAngle();
    boolean diagonal = angle % 90 != 0;
    int[] cell = diagonal ? HospitalWayfindingGlyphs.ARROW_DIAGONAL
        : HospitalWayfindingGlyphs.ARROW_STRAIGHT;
    int turn = ((angle - (diagonal ? 45 : 0)) % 360 + 360) % 360;
    // sprite-local (sx, sy) in -0.5..0.5 = the point (dx, dy) turned back by `turn`
    float cos = (float) Math.round(Math.cos(Math.toRadians(turn)));
    float sin = (float) Math.round(Math.sin(Math.toRadians(turn)));
    float span = HospitalWayfindingGlyphs.PICTO - 1;
    // dx = (x - cx)/s, dy = (y - cy)/s; sx = dx cos + dy sin; sy = -dx sin + dy cos
    // u = cell.x + 0.5 + (sx + 0.5) span ; v = cell.y + 0.5 + (0.5 - sy) span
    float ua = cos / s * span;
    float ub = sin / s * span;
    float uc = cell[0] + 0.5f + 0.5f * span - (cx * cos + cy * sin) / s * span;
    float va = sin / s * span;
    float vb = -cos / s * span;
    float vc = cell[1] + 0.5f + 0.5f * span + (-cx * sin + cy * cos) / s * span;
    return new Piece(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f, ua, ub, uc, va, vb, vc,
        rgb, LAYER_ART);
  }
}
