package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a glazing window ({@link BlockGlazingWindow}) draws, as boxes, from everything that decides
 * it: its run and corner joins, its place in a vertical stack, its depth and its stored settings,
 * all packed into one {@link Shape}. Pure: no game state, so it is unit tested, and the baked
 * model ({@code GlazingWindowBakedModel}) caches its quads by the packed shape.
 *
 * <p><b>Model space.</b> Sixteenths of a block, the window facing north: its outside is toward
 * {@code -z}. The baked model turns the boxes to the block's facing as a blockstate's {@code y}
 * would (north 0, east 90, south 180, west 270).</p>
 *
 * <p><b>Spans and planes.</b> Glass runs in spans. A straight window is one span across the cell
 * along x, its glass plane at {@code z = depth} ({@link #planeOf}). A 90-degree turn adds a cross
 * span along z, its plane {@code depth} in from the side its outside is on, and a corner post where
 * the two meet; the run span then runs from the post to the side it continues on. A canted corner
 * is one span on the cell's diagonal from {@code (16, depth)} to {@code (depth, 16)}, so it meets a
 * straight window's glass at the east edge and a side window's (or the facade wall) at the south
 * edge, whatever the depth; it is drawn turned 45 degrees about y with a post at each end.</p>
 *
 * <p><b>Stacks.</b> Windows stacked one on another are one tall window. Each span is laid out in
 * stack coordinates ({@code v} from 0 at the stack's foot to {@code 16 * height} at its head) and
 * clipped to this block's sixteen, so the sill frame lands only in the bottom block, the head only
 * in the top one, and a double-hung window's meeting rail halfway up the opening: at the middle
 * of a single block, at the joint of a 2-tall stack, at the middle of a 3-tall stack's middle
 * block. The top block keeps what reaches above the stack (a keystone) and the bottom block what
 * hangs below it.</p>
 *
 * <p><b>Patterns.</b> {@link #PLAIN} is the Glazing pane's look: glass to the cell edges, a 1 px
 * frame only where the window ends, no mullion between joined cells. The others give every cell
 * its own sash unit inside a 1.5 px frame, and a joined side half a mullion, so two cells share one
 * mullion: {@link #HUNG_1_1} and {@link #HUNG_6_6} (the upper sash on the outer track, the lower on
 * the inner one; open, the lower slides up behind the upper), and {@link #CASEMENT} (a pair of
 * side-hung sashes meeting in the middle; open, both swing out square to the wall).</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public final class WindowLayout {

  /** Patterns. */
  public static final int PLAIN = 0;
  public static final int HUNG_1_1 = 1;
  public static final int HUNG_6_6 = 2;
  public static final int CASEMENT = 3;
  public static final int PATTERNS = 4;

  /** Depths, as stored in metadata. */
  public static final int CENTRED = 0;
  public static final int FLUSH = 1;
  public static final int INSET = 2;
  public static final int DEPTHS = 3;

  /** Sill styles, drawn at the foot of the stack. */
  public static final int SILL_NONE = 0;
  public static final int SILL_STONE = 1;
  public static final int SILL_FRAME = 2;
  public static final int SILLS = 3;

  /** Head styles, drawn at the top of the stack. */
  public static final int HEAD_NONE = 0;
  public static final int HEAD_LINTEL = 1;
  public static final int HEAD_KEYSTONE = 2;
  public static final int HEADS = 3;

  /** A run end (west or east in model space). */
  public static final int END_FREE = 0;
  /** Against a solid face: framed, and lined to the inner face. */
  public static final int END_SOLID = 1;
  /** Against other glass or another window: framed. */
  public static final int END_OTHER = 2;
  /** The same window carries on: no frame (plain) or half a mullion. */
  public static final int END_JOIN = 3;
  /** A corner post: drawn apart, nothing at the span's end. Not packed. */
  static final int END_POST = 4;

  /** A cross arm (north or south in model space). */
  public static final int CROSS_NONE = 0;
  /** The window turns to meet a solid face (a bay's side, back to the facade). */
  public static final int CROSS_SOLID = 1;
  /** The window turns into a side window of the same finish. */
  public static final int CROSS_JOIN = 2;

  /** What a box is made of. */
  public static final int FRAME = 0;
  public static final int GLASS = 1;
  public static final int STONE = 2;

  /** Face bits, by {@code EnumFacing} index: down, up, north, south, west, east. */
  public static final int DOWN = 1;
  public static final int UP = 1 << 1;
  public static final int NORTH = 1 << 2;
  public static final int SOUTH = 1 << 3;
  public static final int WEST = 1 << 4;
  public static final int EAST = 1 << 5;
  public static final int ALL = 63;

  /**
   * The tallest stack laid out as one window. Each block looks at most this far up and down, so
   * in a taller column the blocks past the reach each find their own foot and top: the column
   * reads as overlapping windows. Build tall glazing as stacks of eight or fewer.
   */
  public static final int MAX_STACK = 8;

  // plane face bits, before a span is mapped to the model
  private static final int U_LO = 1;
  private static final int U_HI = 2;
  private static final int V_LO = 4;
  private static final int V_HI = 8;
  private static final int W_LO = 16;
  private static final int W_HI = 32;
  private static final int P_ALL = 63;

  /** Sash members: stiles and top rail, bottom rail. */
  private static final float S = 1.25f;
  private static final float BOTTOM_RAIL = 2f;

  private WindowLayout() {
  }

  /** The glass plane's distance from the outer face for a depth: the frame is 3 px deep. */
  public static float planeOf(int depth) {
    switch (depth) {
      case FLUSH:
        return 1.5f;
      case INSET:
        return 14.5f;
      default:
        return 8f;
    }
  }

  /**
   * A canted corner's facing for a look: the quarter the placer looks into, named by the compass
   * point clockwise of the other (looking north-west, yaw 90 to 180, gives north; its outsides are
   * north and west).
   *
   * @param yaw the placer's yaw
   * @return the facing's horizontal index, S0 W1 N2 E3
   */
  public static int cantedFacing(float yaw) {
    double y = yaw % 360.0;
    if (y < 0) {
      y += 360.0;
    }
    int quarter = (int) Math.floor(y / 90.0) & 3;
    return (quarter + 1) & 3;
  }

  // --- shape --------------------------------------------------------------------------------

  /**
   * Everything a window's model depends on, packed into a {@code long} ({@link #pack}) that is the
   * baked model's cache key.
   */
  public static final class Shape {

    /** Horizontal index, S0 W1 N2 E3. */
    public int facing = 2;
    public int depth = CENTRED;
    public boolean canted;
    /** Run ends in model space (the window faces north): west and east. */
    public int endW = END_FREE;
    public int endE = END_FREE;
    /** Cross arms in model space: north (in front) and south (behind). */
    public int crossN = CROSS_NONE;
    public int crossS = CROSS_NONE;
    /** The stack's height, 1 to {@link #MAX_STACK}, and this block's place in it from 0. */
    public int height = 1;
    public int index;
    public int pattern = PLAIN;
    public boolean open;
    public boolean arch;
    public int sill = SILL_NONE;
    public int head = HEAD_NONE;
    /** A solid face above the stack's top, below its foot: the frame is lined there. */
    public boolean solidAbove;
    public boolean solidBelow;

    /** The packed shape. */
    public long pack() {
      long k = facing & 3;
      k |= (long) (depth & 3) << 2;
      k |= (canted ? 1L : 0L) << 4;
      k |= (long) (endW & 3) << 5;
      k |= (long) (endE & 3) << 7;
      k |= (long) (crossN & 3) << 9;
      k |= (long) (crossS & 3) << 11;
      k |= (long) ((height - 1) & 7) << 13;
      k |= (long) (index & 7) << 16;
      k |= (long) (pattern & 3) << 19;
      k |= (open ? 1L : 0L) << 21;
      k |= (arch ? 1L : 0L) << 22;
      k |= (long) (sill & 3) << 23;
      k |= (long) (head & 3) << 25;
      k |= (solidAbove ? 1L : 0L) << 27;
      k |= (solidBelow ? 1L : 0L) << 28;
      return k;
    }

    /** The shape a packed key stands for. */
    public static Shape unpack(long k) {
      Shape s = new Shape();
      s.facing = (int) (k & 3);
      s.depth = (int) (k >> 2 & 3);
      s.canted = (k >> 4 & 1) != 0;
      s.endW = (int) (k >> 5 & 3);
      s.endE = (int) (k >> 7 & 3);
      s.crossN = (int) (k >> 9 & 3);
      s.crossS = (int) (k >> 11 & 3);
      s.height = (int) (k >> 13 & 7) + 1;
      s.index = (int) (k >> 16 & 7);
      s.pattern = (int) (k >> 19 & 3);
      s.open = (k >> 21 & 1) != 0;
      s.arch = (k >> 22 & 1) != 0;
      s.sill = (int) (k >> 23 & 3);
      s.head = (int) (k >> 25 & 3);
      s.solidAbove = (k >> 27 & 1) != 0;
      s.solidBelow = (k >> 28 & 1) != 0;
      return s;
    }
  }

  /**
   * A box in model space, sixteenths, facing north. A canted box is turned 45 degrees about y
   * round {@code (ox, oz)} (positive: its {@code -z} face turns to the north-west).
   */
  public static final class Box {

    public final float x0;
    public final float y0;
    public final float z0;
    public final float x1;
    public final float y1;
    public final float z1;
    public final int part;
    public final int faces;
    public final boolean rotated;
    public final float ox;
    public final float oz;

    Box(float x0, float y0, float z0, float x1, float y1, float z1, int part, int faces,
        boolean rotated, float ox, float oz) {
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
      this.part = part;
      this.faces = faces;
      this.rotated = rotated;
      this.ox = ox;
      this.oz = oz;
    }

    @Override
    public String toString() {
      return String.format("Box[%s %.2f,%.2f,%.2f -> %.2f,%.2f,%.2f faces=%d%s]",
          part == GLASS ? "glass" : part == STONE ? "stone" : "frame", x0, y0, z0, x1, y1, z1,
          faces, rotated ? " rot45" : "");
    }
  }

  /** A box in a span's plane: u along the glass, v up the stack, w through it (outside < 0). */
  private static final class PBox {

    final float u0;
    final float u1;
    final float v0;
    final float v1;
    final float w0;
    final float w1;
    final int part;
    final int faces;

    PBox(float u0, float u1, float v0, float v1, float w0, float w1, int part, int faces) {
      this.u0 = u0;
      this.u1 = u1;
      this.v0 = v0;
      this.v1 = v1;
      this.w0 = w0;
      this.w1 = w1;
      this.part = part;
      this.faces = faces;
    }
  }

  /** How a span's plane sits in the model. */
  private enum Plane { RUN, CROSS_W, CROSS_E, CANTED }

  /** One run of glass across (part of) a cell. */
  private static final class Span {

    Plane plane = Plane.RUN;
    float u0;
    float u1;
    int lo;
    int hi;
    /** Run: the glass plane's z. Cross: its x. Canted: unused. */
    float at;
    /** The outer face's offset from the glass (negative) and the inner face's (positive). */
    float wFace;
    float wInner;
    boolean trim = true;
    float trimLo;
    float trimHi;
  }

  // --- layout -------------------------------------------------------------------------------

  /**
   * The boxes this block draws.
   *
   * @param s the shape
   * @return the boxes, in model space facing north
   */
  public static List<Box> boxes(Shape s) {
    List<Box> out = new ArrayList<>();
    boolean patterned = s.pattern != PLAIN;
    float post = patterned ? 1.25f : 1f;
    float zd = planeOf(s.depth);
    int height = Math.max(1, Math.min(MAX_STACK, s.height));
    int index = Math.max(0, Math.min(height - 1, s.index));
    float top = 16f * height;
    List<Span> spans = new ArrayList<>();
    List<PBox> posts = new ArrayList<>();

    if (s.canted) {
      Span c = new Span();
      c.plane = Plane.CANTED;
      c.u0 = 0;
      c.u1 = (16 - zd) * (float) Math.sqrt(2);
      c.lo = END_POST;
      c.hi = END_POST;
      c.wFace = -2f;
      c.wInner = 0;
      c.trimLo = 0.6f;
      c.trimHi = 0.6f;
      spans.add(c);
      // the posts, in the run plane (z = zd + w)
      posts.add(new PBox(16 - 2 * post, 16, 0, top, -post, post, FRAME, P_ALL));
      posts.add(new PBox(zd - post, zd + post, 0, top, 16 - 2 * post - zd, 16 - zd, FRAME,
          P_ALL));
    } else {
      boolean runW = s.endW != END_FREE;
      boolean runE = s.endE != END_FREE;
      int crosses = (s.crossN != CROSS_NONE ? 1 : 0) + (s.crossS != CROSS_NONE ? 1 : 0);
      if (crosses == 0) {
        spans.add(run(0, 16, s.endW, s.endE, zd));
      } else if (crosses == 1 && runW != runE) {
        // an L: the run from the post to the side it carries on, the cross from the post to
        // the side the arm is on
        boolean behind = s.crossS != CROSS_NONE;
        int crossKind = behind ? s.crossS : s.crossN;
        boolean convex = behind;
        boolean runEast = runE;
        // the cross glass's outside: the free end's side on an outside corner, the run's side on
        // an inside one
        boolean outsideWest = convex ? runEast : !runEast;
        float xc = outsideWest ? zd : 16 - zd;
        Span r = runEast ? run(xc, 16, END_POST, s.endE, zd) : run(0, xc, s.endW, END_POST, zd);
        if (convex) {
          // the run's sill and head reach round the corner to the cross's
          float reach = zd + 1.5f;
          if (runEast) {
            r.trimLo = reach;
          } else {
            r.trimHi = reach;
          }
        }
        spans.add(r);
        Span c = cross(xc, outsideWest, behind ? zd : 0, behind ? 16 : zd,
            behind ? END_POST : endOfCross(crossKind), behind ? endOfCross(crossKind) : END_POST,
            zd);
        if (convex) {
          c.trimLo = 1.5f;
        } else {
          c.trim = false;
        }
        spans.add(c);
        posts.add(new PBox(xc - post, xc + post, 0, top, -post, post, FRAME, P_ALL));
      } else {
        // a T, a cross, or a turn from a lone window: the run whole, each arm from it
        spans.add(run(0, 16, s.endW, s.endE, zd));
        if (s.crossS != CROSS_NONE) {
          Span c = cross(zd, true, zd, 16, END_POST, endOfCross(s.crossS), zd);
          c.trim = false;
          spans.add(c);
        }
        if (s.crossN != CROSS_NONE) {
          Span c = cross(zd, true, 0, zd, endOfCross(s.crossN), END_POST, zd);
          c.trim = false;
          spans.add(c);
        }
        posts.add(new PBox(zd - post, zd + post, 0, top, -post, post, FRAME, P_ALL));
      }
    }

    for (Span span : spans) {
      List<PBox> boxes = new ArrayList<>();
      span(boxes, span, s, top);
      for (PBox b : boxes) {
        emit(out, b, span, s, zd, index, height);
      }
    }
    Span postPlane = run(0, 16, END_FREE, END_FREE, zd);
    for (PBox b : posts) {
      emit(out, b, postPlane, s, zd, index, height);
    }
    return out;
  }

  private static int endOfCross(int kind) {
    return kind == CROSS_JOIN ? END_JOIN : END_SOLID;
  }

  private static Span run(float u0, float u1, int lo, int hi, float zd) {
    Span s = new Span();
    s.plane = Plane.RUN;
    s.u0 = u0;
    s.u1 = u1;
    s.lo = lo;
    s.hi = hi;
    s.at = zd;
    s.wFace = -zd;
    s.wInner = 16 - zd;
    return s;
  }

  private static Span cross(float xc, boolean outsideWest, float u0, float u1, int lo, int hi,
      float zd) {
    Span s = new Span();
    s.plane = outsideWest ? Plane.CROSS_W : Plane.CROSS_E;
    s.u0 = u0;
    s.u1 = u1;
    s.lo = lo;
    s.hi = hi;
    s.at = xc;
    s.wFace = -zd;
    // no lining: the cross span's inner side is the bay itself, and a lining at its end against
    // the facade would close the bay off from the room (the run span lines the sill and head)
    s.wInner = 0;
    return s;
  }

  /** How far the sash opening starts in from a span end of this kind. */
  private static float inset(int kind, boolean patterned) {
    if (!patterned) {
      return kind == END_JOIN || kind == END_POST ? 0 : 1f;
    }
    switch (kind) {
      case END_JOIN:
        return 0.75f;
      case END_POST:
        return 1.25f;
      default:
        return 1.5f;
    }
  }

  /** Lays out one span in stack coordinates. */
  private static void span(List<PBox> out, Span sp, Shape s, float top) {
    boolean patterned = s.pattern != PLAIN;
    float f = patterned ? 1.5f : 1f;
    float fd = patterned ? 1.5f : 1f;
    float u0 = sp.u0;
    float u1 = sp.u1;

    // the frame: sill, head, and the jambs or half mullions at the span's ends
    out.add(new PBox(u0, u1, 0, f, -fd, fd, FRAME, P_ALL));
    out.add(new PBox(u0, u1, top - f, top, -fd, fd, FRAME, P_ALL));
    end(out, sp.lo, u0, true, f, fd, top, patterned);
    end(out, sp.hi, u1, false, f, fd, top, patterned);

    float a = u0 + inset(sp.lo, patterned);
    float b = u1 - inset(sp.hi, patterned);
    float vb = f;
    float vt = top - f;

    if (patterned && sp.wInner > fd + 0.01f) {
      // the frame lines the opening to the inner face where it meets a wall (#357: no daylight
      // past a window in a bay)
      if (sp.lo == END_SOLID) {
        out.add(new PBox(u0, u0 + f, 0, top, fd, sp.wInner, FRAME, P_ALL));
      }
      if (sp.hi == END_SOLID) {
        out.add(new PBox(u1 - f, u1, 0, top, fd, sp.wInner, FRAME, P_ALL));
      }
      if (s.solidBelow) {
        out.add(new PBox(u0, u1, 0, f, fd, sp.wInner, FRAME, P_ALL));
      }
      if (s.solidAbove) {
        out.add(new PBox(u0, u1, top - f, top, fd, sp.wInner, FRAME, P_ALL));
      }
    }

    if (b - a > 0.5f) {
      switch (s.pattern) {
        case HUNG_1_1:
        case HUNG_6_6:
          doubleHung(out, a, b, vb, vt, s.open, s.pattern == HUNG_6_6);
          break;
        case CASEMENT:
          casements(out, a, b, vb, vt, s.open);
          break;
        default:
          out.add(new PBox(u0, u1, 0, top, -0.5f, 0.5f, GLASS, W_LO | W_HI));
          break;
      }
      if (s.arch) {
        arch(out, a, b, vt, fd, top);
      }
    }

    if (sp.trim) {
      trim(out, sp, s, fd, top);
    }
  }

  private static void end(List<PBox> out, int kind, float u, boolean lo, float f, float fd,
      float top, boolean patterned) {
    float w;
    if (kind == END_POST || kind == END_JOIN && !patterned) {
      return;
    }
    w = kind == END_JOIN ? 0.75f : f;
    out.add(lo ? new PBox(u, u + w, f, top - f, -fd, fd, FRAME, P_ALL)
        : new PBox(u - w, u, f, top - f, -fd, fd, FRAME, P_ALL));
  }

  /**
   * The height the meeting rail's middle is at, in stack coordinates: halfway up the opening.
   *
   * @param height the stack's height in blocks
   * @return the meeting rail's middle, in sixteenths from the stack's foot
   */
  public static float meetingRail(int height) {
    float vb = 1.5f;
    float vt = 16f * height - 1.5f;
    return (vb + vt) / 2f;
  }

  private static void doubleHung(List<PBox> out, float a, float b, float vb, float vt,
      boolean open, boolean sixOverSix) {
    float m = (vb + vt) / 2f;
    // the upper sash, on the outer track
    float uw0 = -1.25f;
    float uw1 = -0.25f;
    sash(out, a, b, m - 0.5f, vt, uw0, uw1, 1f, S, sixOverSix);
    // the lower sash, on the inner track; open, it slides up behind the upper
    float shift = open ? vt - (m + 0.5f) : 0f;
    sash(out, a, b, vb + shift, m + 0.5f + shift, 0.25f, 1.25f, BOTTOM_RAIL, 1f, sixOverSix);
  }

  /** A sash from v0 to v1: stiles, rails (bottom and top heights given), glass, muntins. */
  private static void sash(List<PBox> out, float a, float b, float v0, float v1, float w0,
      float w1, float bottomRail, float topRail, boolean sixLights) {
    out.add(new PBox(a, a + S, v0, v1, w0, w1, FRAME, P_ALL));
    out.add(new PBox(b - S, b, v0, v1, w0, w1, FRAME, P_ALL));
    out.add(new PBox(a + S, b - S, v0, v0 + bottomRail, w0, w1, FRAME, P_ALL));
    out.add(new PBox(a + S, b - S, v1 - topRail, v1, w0, w1, FRAME, P_ALL));
    float gu0 = a + S;
    float gu1 = b - S;
    float gv0 = v0 + bottomRail;
    float gv1 = v1 - topRail;
    float gw = (w0 + w1) / 2f;
    out.add(new PBox(gu0, gu1, gv0, gv1, gw - 0.1f, gw + 0.1f, GLASS, W_LO | W_HI));
    if (sixLights && gu1 - gu0 > 3 && gv1 - gv0 > 2) {
      float mw0 = w0 + 0.15f;
      float mw1 = w1 - 0.15f;
      for (int k = 1; k <= 2; k++) {
        float u = gu0 + (gu1 - gu0) * k / 3f;
        out.add(new PBox(u - 0.25f, u + 0.25f, gv0, gv1, mw0, mw1, FRAME, P_ALL & ~(V_LO | V_HI)));
      }
      float v = (gv0 + gv1) / 2f;
      out.add(new PBox(gu0, gu1, v - 0.25f, v + 0.25f, mw0, mw1, FRAME, P_ALL & ~(U_LO | U_HI)));
    }
  }

  private static void casements(List<PBox> out, float a, float b, float vb, float vt,
      boolean open) {
    float c = (a + b) / 2f;
    if (!open) {
      sash(out, a, c, vb, vt, -0.6f, 0.6f, S, S, false);
      sash(out, c, b, vb, vt, -0.6f, 0.6f, S, S, false);
      return;
    }
    // each sash swung out square to the wall on its hinge at the jamb
    swung(out, a, a + 1.2f, c - a, vb, vt);
    swung(out, b - 1.2f, b, c - a, vb, vt);
  }

  /** An open casement: the sash lying from the glass plane outward, between u0 and u1. */
  private static void swung(List<PBox> out, float u0, float u1, float width, float vb,
      float vt) {
    float w1 = -0.6f;
    float w0 = w1 - width;
    out.add(new PBox(u0, u1, vb, vt, w1 - S, w1, FRAME, P_ALL));
    out.add(new PBox(u0, u1, vb, vt, w0, w0 + S, FRAME, P_ALL));
    out.add(new PBox(u0, u1, vb, vb + S, w0 + S, w1 - S, FRAME, P_ALL));
    out.add(new PBox(u0, u1, vt - S, vt, w0 + S, w1 - S, FRAME, P_ALL));
    float um = (u0 + u1) / 2f;
    out.add(new PBox(um - 0.1f, um + 0.1f, vb + S, vt - S, w0 + S, w1 - S, GLASS, U_LO | U_HI));
  }

  /**
   * A round arch over the opening's top: the spandrels either side filled with frame, stepped a
   * sixteenth at a time. The rise is half the opening's width, at most twelve sixteenths, so the
   * arch stays in the top block.
   */
  private static void arch(List<PBox> out, float a, float b, float vt, float fd, float top) {
    float r = (b - a) / 2f;
    float rise = Math.min(r, 12f);
    if (rise < 1f) {
      return;
    }
    float vs = vt - rise;
    float cx = (a + b) / 2f;
    for (float y0 = vs; y0 < vt - 0.001f; y0 += 1f) {
      float y1 = Math.min(y0 + 1f, vt);
      float t = ((y0 + y1) / 2f - vs) / rise;
      float hw = r * (float) Math.sqrt(Math.max(0, 1 - t * t));
      if (cx - hw - a > 0.05f) {
        out.add(new PBox(a, cx - hw, y0, y1, -fd, fd, FRAME, P_ALL));
      }
      if (b - (cx + hw) > 0.05f) {
        out.add(new PBox(cx + hw, b, y0, y1, -fd, fd, FRAME, P_ALL));
      }
    }
  }

  /** The sill at the stack's foot and the head at its top, on the outside of the glass. */
  private static void trim(List<PBox> out, Span sp, Shape s, float fd, float top) {
    float lo = sp.u0 - sp.trimLo - (sp.lo == END_SOLID ? 1.5f : 0f);
    float hi = sp.u1 + sp.trimHi + (sp.hi == END_SOLID ? 1.5f : 0f);
    float wf = sp.wFace;
    // a canted span's trim sits a hair off the straight windows' so their faces never share
    // a plane where the two overlap at the joint
    float lift = sp.plane == Plane.CANTED ? 0.03f : 0f;
    if (s.sill == SILL_STONE) {
      out.add(new PBox(lo, hi, 0, 2f + lift, wf - 1.5f, -fd, STONE, P_ALL));
    } else if (s.sill == SILL_FRAME) {
      out.add(new PBox(lo, hi, 0, 1.5f + lift, wf - 1f, -fd, FRAME, P_ALL));
    }
    if (s.head == HEAD_LINTEL || s.head == HEAD_KEYSTONE) {
      out.add(new PBox(lo, hi, top - 2.5f - lift, top, wf - 0.75f, -fd, STONE, P_ALL));
    }
    if (s.head == HEAD_KEYSTONE) {
      float cu = (sp.u0 + sp.u1) / 2f;
      out.add(new PBox(cu - 1.5f, cu + 1.5f, top - 3f, top + 2f, wf - 1.25f, -fd, STONE,
          P_ALL));
    }
  }

  /** Clips a plane box to this block's part of the stack and maps it into the model. */
  private static void emit(List<Box> out, PBox b, Span sp, Shape s, float zd, int index,
      int height) {
    float lo = index == 0 ? Float.NEGATIVE_INFINITY : 16f * index;
    float hi = index == height - 1 ? Float.POSITIVE_INFINITY : 16f * index + 16f;
    float v0 = Math.max(b.v0, lo);
    float v1 = Math.min(b.v1, hi);
    if (v1 - v0 < 0.001f || b.u1 - b.u0 < 0.001f || b.w1 - b.w0 < 0.001f) {
      return;
    }
    int faces = b.faces;
    if (v0 > b.v0) {
      faces &= ~V_LO;
    }
    if (v1 < b.v1) {
      faces &= ~V_HI;
    }
    float y0 = v0 - 16f * index;
    float y1 = v1 - 16f * index;
    int mf = 0;
    switch (sp.plane) {
      case CROSS_W:
        mf = map(faces, NORTH, SOUTH, WEST, EAST);
        out.add(new Box(sp.at + b.w0, y0, b.u0, sp.at + b.w1, y1, b.u1, b.part, mf, false, 0,
            0));
        return;
      case CROSS_E:
        mf = map(faces, NORTH, SOUTH, EAST, WEST);
        out.add(new Box(sp.at - b.w1, y0, b.u0, sp.at - b.w0, y1, b.u1, b.part, mf, false, 0,
            0));
        return;
      case CANTED:
        float c = (16 + zd) / 2f;
        float half = (sp.u1 - sp.u0) / 2f;
        mf = map(faces, WEST, EAST, NORTH, SOUTH);
        out.add(new Box(c - half + b.u0, y0, c + b.w0, c - half + b.u1, y1, c + b.w1, b.part, mf,
            true, c, c));
        return;
      default:
        mf = map(faces, WEST, EAST, NORTH, SOUTH);
        out.add(new Box(b.u0, y0, sp.at + b.w0, b.u1, y1, sp.at + b.w1, b.part, mf, false, 0,
            0));
    }
  }

  private static int map(int faces, int uLo, int uHi, int wLo, int wHi) {
    int m = 0;
    if ((faces & U_LO) != 0) {
      m |= uLo;
    }
    if ((faces & U_HI) != 0) {
      m |= uHi;
    }
    if ((faces & V_LO) != 0) {
      m |= DOWN;
    }
    if ((faces & V_HI) != 0) {
      m |= UP;
    }
    if ((faces & W_LO) != 0) {
      m |= wLo;
    }
    if ((faces & W_HI) != 0) {
      m |= wHi;
    }
    return m;
  }

  // --- collision ----------------------------------------------------------------------------

  /**
   * The boxes nothing passes, in model space facing north, a block tall: the glass plane of each
   * span, closed whether the window is open or not, so an open window lets nothing through.
   *
   * @param s the shape
   * @return {x0, z0, x1, z1} per box, sixteenths
   */
  public static List<float[]> collision(Shape s) {
    if (s.canted) {
      float zd = planeOf(s.depth);
      List<float[]> out = new ArrayList<>();
      int steps = 8;
      for (int k = 0; k < steps; k++) {
        float t0 = k / (float) steps;
        float t1 = (k + 1) / (float) steps;
        // from (zd, 16) to (16, zd)
        float xa = zd + (16 - zd) * t0;
        float xb = zd + (16 - zd) * t1;
        float za = 16 - (16 - zd) * t0;
        float zb = 16 - (16 - zd) * t1;
        out.add(clamp(Math.min(xa, xb) - 1, Math.min(za, zb) - 1, Math.max(xa, xb) + 1,
            Math.max(za, zb) + 1));
      }
      return out;
    }
    List<float[]> out = new ArrayList<>();
    for (Box b : boxes(withoutDecoration(s))) {
      if (b.part == GLASS) {
        boolean alongX = b.x1 - b.x0 >= b.z1 - b.z0;
        out.add(alongX ? clamp(b.x0, (b.z0 + b.z1) / 2 - 1, b.x1, (b.z0 + b.z1) / 2 + 1)
            : clamp((b.x0 + b.x1) / 2 - 1, b.z0, (b.x0 + b.x1) / 2 + 1, b.z1));
      }
    }
    return out.isEmpty() ? Collections.singletonList(clamp(0, 7, 16, 9)) : out;
  }

  /** The same joins, closed and plain: just the glass, one box a span. */
  private static Shape withoutDecoration(Shape s) {
    Shape p = Shape.unpack(s.pack());
    p.pattern = PLAIN;
    p.open = false;
    p.arch = false;
    p.sill = SILL_NONE;
    p.head = HEAD_NONE;
    p.height = 1;
    p.index = 0;
    return p;
  }

  private static float[] clamp(float x0, float z0, float x1, float z1) {
    return new float[]{Math.max(0, x0), Math.max(0, z0), Math.min(16, x1), Math.min(16, z1)};
  }
}
