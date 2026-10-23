package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on one face of a wayfinding panel goes: the background, the border, the
 * pictogram, the arrow, each line's icons and the legend's glyphs, each a rectangle in the panel's
 * own space (x to the reader's right from the panel's left edge, y up from its bottom, in blocks)
 * with the sheet position it shows, as an affine map from that space. Kept free of the client so
 * it can be tested; {@link WayfindingBakedModel} places the face on the panel, clips the pieces to
 * a cell and bakes them.
 *
 * <p><b>The proportions</b> are the airport and hospital panels', so a sign that uses none of the
 * #307 options lays out exactly as it did: the pictogram a square the panel's height less its
 * margins at the end away from the arrow, the arrow 0.8 of it at the end it points to, the text
 * between them, its capitals 0.45 of the panel's height on one line or 0.28 each on two (0.17 on
 * three, 0.12 on four), every line scaled down together to fit; a long legend first takes room
 * from the pictogram and arrow, down to 0.55 of their size. A line's text scale makes it smaller
 * about the middle it has at full size.</p>
 *
 * <p><b>Added for #307.</b> A line's icons stand either side of its text, 1.4 of its capitals
 * square, and count in its width when the lines are fitted; the line, icons and all, sits left,
 * centred or right between the pictogram and the arrow. A border is a frame in the legend's
 * colour inside the edge, and moves the margins in; the capitals shrink with the room it takes.
 * An icon is drawn on a tile in the pictogram colour with its art in the background colour, or
 * plain in the legend's.</p>
 *
 * @since 2026.10
 */
public final class WayfindingLayout {

  /** The frame's width round the panel's face, in blocks. */
  public static final float FRAME = 0.8f / 16f;
  /** The capitals' height for one to four lines, of the panel's height. */
  private static final float[] CAPS = {0.45f, 0.28f, 0.17f, 0.12f};
  /** Baseline to baseline, in cap heights (0.85 line heights over 0.45 caps). */
  static final float LINE_STEP = 0.85f / 0.45f;
  static final float ARROW_SHARE = 0.8f;
  static final float MIN_ART_SHARE = 0.55f;
  /** A line's icon, and the gap between it and the text, in the line's cap heights. */
  static final float LINE_ICON = 1.4f;
  static final float LINE_ICON_GAP = 0.4f;
  /** A border's thickness for {@code bd} 1 and 2, and its inset from the frame, of the height. */
  private static final float[] BORDER = {0f, 0.025f, 0.045f};
  private static final float BORDER_INSET = 0.035f;
  private static final float MIN_BORDER = 0.35f / 16f;

  /** The layers a piece is drawn on, back to front. */
  public static final int LAYER_BACKGROUND = 0;
  public static final int LAYER_TILE = 1;
  public static final int LAYER_ART = 2;

  /** The sheets a piece shows. */
  public static final int SHEET_GLYPHS = 0;
  public static final int SHEET_ICONS = 1;

  private WayfindingLayout() {
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
    public final int sheet;

    Piece(float x0, float y0, float x1, float y1, float ua, float ub, float uc, float va,
        float vb, float vc, int rgb, int layer, int sheet) {
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
      this.sheet = sheet;
    }

    public float u(float x, float y) {
      return ua * x + ub * y + uc;
    }

    public float v(float x, float y) {
      return va * x + vb * y + vc;
    }

    /** This piece moved by (dx, dy), its sheet mapping with it. */
    public Piece moved(float dx, float dy) {
      return new Piece(x0 + dx, y0 + dy, x1 + dx, y1 + dy, ua, ub, uc - ua * dx - ub * dy, va,
          vb, vc - va * dx - vb * dy, rgb, layer, sheet);
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
      return new Piece(nx0, ny0, nx1, ny1, ua, ub, uc, va, vb, vc, rgb, layer, sheet);
    }
  }

  /**
   * A face's pieces.
   *
   * @param sign   the sign
   * @param width  the panel's width as drawn, blocks
   * @param height the panel's height as drawn, blocks
   * @param back   whether this is the back face: its arrows are mirrored, so they point the same
   *               way in the world as the front's
   * @return the pieces, background first, in the panel's space
   */
  public static List<Piece> face(WayfindingSign sign, float width, float height, boolean back) {
    List<Piece> out = new ArrayList<>();
    int bg = sign.background();
    int fg = sign.legend();
    out.add(white(FRAME, FRAME, width - FRAME, height - FRAME, bg, LAYER_BACKGROUND));

    float legacyMargin = FRAME + 0.08f * height;
    float margin = legacyMargin;
    if (sign.getBorder() > 0) {
      float t = Math.max(MIN_BORDER, BORDER[sign.getBorder()] * height);
      float i0 = FRAME + BORDER_INSET * height;
      out.add(white(i0, i0, width - i0, i0 + t, fg, LAYER_TILE));
      out.add(white(i0, height - i0 - t, width - i0, height - i0, fg, LAYER_TILE));
      out.add(white(i0, i0 + t, i0 + t, height - i0 - t, fg, LAYER_TILE));
      out.add(white(width - i0 - t, i0 + t, width - i0, height - i0 - t, fg, LAYER_TILE));
      margin = Math.max(legacyMargin, i0 + t + 0.05f * height);
    }
    float gap = legacyMargin;
    float fullSquare = height - 2 * margin;
    if (fullSquare <= 0 || width - 2 * margin <= 0) {
      return out;
    }

    int[] shown = sign.shownLines();
    int n = shown.length;
    // the capitals at full size, shrunk by the room a border takes
    float cap = n == 0 ? 0
        : CAPS[n - 1] * height * fullSquare / (height - 2 * legacyMargin);
    float[] scales = new float[n];
    float widest = 0;
    // the widest line at the size it wants, as a share of full size: widest when unscaled
    float widestScaled = 0;
    for (int k = 0; k < n; k++) {
      int i = shown[k];
      scales[k] = sign.getLineTextScale(i);
      float w = lineUnits(sign, i);
      widest = Math.max(widest, w);
      widestScaled = Math.max(widestScaled, w * scales[k]);
    }

    WayfindingArrow arrow = back ? sign.getArrow().mirrored() : sign.getArrow();
    WayfindingIcon pictogram = sign.getPictogram();
    boolean hasPicto = drawable(pictogram);
    boolean hasArrow = arrow != WayfindingArrow.NONE;

    float perSquare = (hasPicto ? 1f : 0f) + (hasArrow ? ARROW_SHARE : 0f);
    float room = width - 2 * margin - (hasPicto ? fullSquare + gap : 0)
        - (hasArrow ? fullSquare * ARROW_SHARE + gap : 0);
    float shortBy = widestScaled * cap - room;
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
      if (hasPicto) {
        xr -= square + gap;
      }
    } else {
      arrowX = xr - arrowSize;
      if (hasArrow) {
        xr -= arrowSize + gap;
      }
      pictX = xl;
      if (hasPicto) {
        xl += square + gap;
      }
    }

    if (hasPicto) {
      icon(out, sign, pictogram, pictX, mid - square / 2f, square, back);
    }
    if (hasArrow) {
      out.add(arrowPiece(arrow, arrowX + arrowSize / 2f, mid, arrowSize, fg));
    }

    float textWidth = xr - xl;
    if (n == 0 || widest <= 0 || textWidth <= 0) {
      return out;
    }
    float want = cap;
    if (widest * cap > textWidth) {
      cap = textWidth / widest;
    }
    float step = cap * LINE_STEP;
    float total = cap + (n - 1) * step;
    float top = mid + total / 2f;
    // the lines at the sizes they want, fitted together; unscaled, exactly the cap above
    float fitted = want;
    if (widestScaled * fitted > textWidth) {
      fitted = textWidth / widestScaled;
    }
    for (int k = 0; k < n; k++) {
      int i = shown[k];
      float lineCap = fitted * scales[k];
      // smaller capitals about the same middle
      float capTop = top - k * step - (cap - lineCap) / 2f;
      float lineWidth = lineUnits(sign, i) * lineCap;
      float x;
      switch (sign.getAlign()) {
        case CENTRE:
          x = xl + (textWidth - lineWidth) / 2f;
          break;
        case RIGHT:
          x = xr - lineWidth;
          break;
        default:
          x = xl;
          break;
      }
      line(out, sign, i, x, capTop, lineCap, fg, back);
    }
    return out;
  }

  /** Whether an icon draws anything. */
  static boolean drawable(WayfindingIcon icon) {
    return icon.isArrow() || (icon != WayfindingIcon.NONE
        && WayfindingSheet.icon(icon.getId()) != null);
  }

  /** A line's width in its cap heights: its text and its icons, with the gaps between them. */
  static float lineUnits(WayfindingSign sign, int i) {
    float text = textUnits(sign.getLine(i));
    boolean l = drawable(sign.getIconLeft(i));
    boolean r = drawable(sign.getIconRight(i));
    int parts = (text > 0 ? 1 : 0) + (l ? 1 : 0) + (r ? 1 : 0);
    return text + (l ? LINE_ICON : 0) + (r ? LINE_ICON : 0)
        + Math.max(0, parts - 1) * LINE_ICON_GAP;
  }

  /** A text's width in cap heights. */
  static float textUnits(String line) {
    float px = 0;
    for (int i = 0; i < line.length(); i++) {
      int o = WayfindingSheet.offset(line.charAt(i));
      if (o >= 0) {
        px += WayfindingSheet.GLYPHS[o + 6] / 16f;
      }
    }
    return px / WayfindingSheet.CAP;
  }

  /** One line from its left edge: its left icon, its text and its right icon. */
  private static void line(List<Piece> out, WayfindingSign sign, int i, float x, float capTop,
      float cap, int rgb, boolean back) {
    float middle = capTop - cap / 2f;
    float size = cap * LINE_ICON;
    float pen = x;
    boolean any = false;
    WayfindingIcon left = sign.getIconLeft(i);
    if (drawable(left)) {
      icon(out, sign, left, pen, middle - size / 2f, size, back);
      pen += size;
      any = true;
    }
    String text = sign.getLine(i);
    float textWidth = textUnits(text) * cap;
    if (textWidth > 0) {
      if (any) {
        pen += cap * LINE_ICON_GAP;
      }
      glyphs(out, text, pen, capTop, cap, rgb);
      pen += textWidth;
      any = true;
    }
    WayfindingIcon right = sign.getIconRight(i);
    if (drawable(right)) {
      if (any) {
        pen += cap * LINE_ICON_GAP;
      }
      icon(out, sign, right, pen, middle - size / 2f, size, back);
    }
  }

  /** An icon in a square at (x, y), size s: an arrow, or the sheet's art on its tile or plain. */
  private static void icon(List<Piece> out, WayfindingSign sign, WayfindingIcon icon, float x,
      float y, float s, boolean back) {
    WayfindingIcon drawn = back ? icon.mirrored() : icon;
    if (drawn.isArrow()) {
      float a = s * ARROW_SHARE;
      out.add(arrowPiece(drawn.getArrow(), x + s / 2f, y + s / 2f, a, sign.legend()));
      return;
    }
    int[] cell = WayfindingSheet.icon(drawn.getId());
    if (cell == null) {
      return;
    }
    int art = sign.legend();
    if (sign.getIconStyle() == WayfindingSign.IconStyle.TILE) {
      out.add(white(x, y, x + s, y + s, sign.pictogramTile(), LAYER_TILE));
      art = sign.background();
    }
    float k = (WayfindingSheet.ICON - 1) / s;
    // u = cell.x + 0.5 + (x' - x) k ; v = cell.y + 0.5 + (y + s - y') k
    out.add(new Piece(x, y, x + s, y + s, k, 0, cell[0] + 0.5f - x * k, 0, -k,
        cell[1] + 0.5f + (y + s) * k, art, LAYER_ART, SHEET_ICONS));
  }

  private static void glyphs(List<Piece> out, String line, float x, float capTop, float cap,
      int rgb) {
    int[] g = WayfindingSheet.GLYPHS;
    float k = cap / WayfindingSheet.CAP;       // blocks per sheet px
    float pen = x;
    for (int i = 0; i < line.length(); i++) {
      int o = WayfindingSheet.offset(line.charAt(i));
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
            g[o + 1] + top * inv, rgb, LAYER_ART, SHEET_GLYPHS));
      }
      pen += g[o + 6] / 16f * k;
    }
  }

  /** A flat rectangle in one colour, from the glyph sheet's white swatch. */
  static Piece white(float x0, float y0, float x1, float y1, int rgb, int layer) {
    int[] w = WayfindingSheet.WHITE;
    float wu = w[0] + w[2] / 2f;
    float wv = w[1] + w[2] / 2f;
    return new Piece(x0, y0, x1, y1, 0, 0, wu, 0, 0, wv, rgb, layer, SHEET_GLYPHS);
  }

  /**
   * The arrow's square, centred on (cx, cy), size s: the straight sprite (pointing right) for the
   * four square directions, the diagonal (up and right) for the others, turned by a multiple of
   * 90 degrees through its sheet mapping.
   */
  private static Piece arrowPiece(WayfindingArrow arrow, float cx, float cy, float s, int rgb) {
    int angle = arrow.getAngle();
    boolean diagonal = angle % 90 != 0;
    int[] cell = diagonal ? WayfindingSheet.ARROW_DIAGONAL : WayfindingSheet.ARROW_STRAIGHT;
    int turn = ((angle - (diagonal ? 45 : 0)) % 360 + 360) % 360;
    // sprite-local (sx, sy) in -0.5..0.5 = the point (dx, dy) turned back by `turn`
    float cos = (float) Math.round(Math.cos(Math.toRadians(turn)));
    float sin = (float) Math.round(Math.sin(Math.toRadians(turn)));
    float span = WayfindingSheet.ICON - 1;
    // dx = (x - cx)/s, dy = (y - cy)/s; sx = dx cos + dy sin; sy = -dx sin + dy cos
    // u = cell.x + 0.5 + (sx + 0.5) span ; v = cell.y + 0.5 + (0.5 - sy) span
    float ua = cos / s * span;
    float ub = sin / s * span;
    float uc = cell[0] + 0.5f + 0.5f * span - (cx * cos + cy * sin) / s * span;
    float va = sin / s * span;
    float vb = -cos / s * span;
    float vc = cell[1] + 0.5f + 0.5f * span + (-cx * sin + cy * cos) / s * span;
    return new Piece(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f, ua, ub, uc, va, vb, vc,
        rgb, LAYER_ART, SHEET_ICONS);
  }
}
