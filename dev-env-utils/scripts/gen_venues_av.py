#!/usr/bin/env python3
"""
gen_venues_av.py -- the Sports & Venues arena's scoreboards, LED ribbon boards, banners and AV.

    py -3 dev-env-utils/scripts/gen_venues_av.py               # write
    py -3 dev-env-utils/scripts/gen_venues_av.py --check       # fail on drift (the Java too)
    py -3 dev-env-utils/scripts/gen_venues_av.py --fragments   # the tab's lines

The blocks (the Sports & Venues - Arena tab, CsmTabVenuesArena, after the event floor):

  * **The centre-hung scoreboard** (BlockScoreboard), large (12 x 8, 9 tall: 6-tall screens with
    a 1-tall LED ring above and below and the rigging on top) and small (6 x 4, 5 tall). A
    hung multi-block unit (BlockVenueUnit) drawn whole by its master, the middle cell of its
    bottom layer: the casing, the rings (an animated texture) and the dark screens are one OBJ,
    so the board is there at any distance; what the screens show (the score, the clock, the
    idle video) is TileEntityScoreboardRenderer's, fullbright and within its render range. The
    screens' rectangles and the shell boxes are written into ScoreboardSize.java from the
    numbers the OBJ is drawn with.
  * **The LED ribbon board** (BlockLedRibbon): a 1-block fascia band, straights and 45 degree
    corners (BlockLedRibbonCorner) joining into one band, its pattern an animated texture
    (no tile entity, no renderer): one 64 px frame holds four patterns of four blocks each, a
    block showing the quarter its phase (its place along the band, actual state) picks, so a
    sweep runs four blocks before it repeats. The scrolling-text piece (BlockLedRibbonText)
    adds a tile entity whose renderer draws its text once across the straight run to its right.
  * **Banners** (BlockVenueBanner): championship and retired-number banners, large (2 x 3) and
    small (1 x 2), hung from the block above; the cloth and trim are tinted from the tile
    entity's colours, the three lines of text are the renderer's.
  * the line-array speaker cluster (hung, OBJ), the broadcast camera platform and a venue
    catwalk grating whose rails close its open sides (actual state).

Every model faces north. No brand appears: team names are what players type, the idle video is
an invented intro and noise meter, the ribbon crawl reads LET'S GO.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_av.py"
C = vc.catalogue(GEN, "av")
T = C.T
M = C.M
box = vc.box

ARENA = "Sports & Venues - Arena"
FACINGS = vc.FACINGS
JAVA_DIR = os.path.join(vc.REPO, "modules", "venues", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "av")
JAVA = {}

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
CASING = (46, 48, 54)
CASING_TOP = (92, 96, 102)
BEZEL = (14, 14, 16)
SCREEN = (10, 11, 14)
STEEL = (120, 124, 130)
ALU = (190, 194, 198)
BLACK = (22, 22, 24)
GRILLE = (30, 30, 32)
CAB = (40, 41, 44)
LENS = (18, 22, 30)
CAM_GREY = (70, 72, 78)
SAFETY = (226, 186, 32)
NAVY = (26, 40, 86)
ICE = (120, 190, 232)
CRIMSON = (148, 30, 44)
GOLD = (240, 180, 30)
GREEN = (24, 128, 56)
WHITE = (244, 244, 240)


def lerp(a, b, k):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


# ------------------------------------------------------------------------------------------
# Shared textures
# ------------------------------------------------------------------------------------------
@C.texture("casing")
def _casing():
    return vc.brushed(CASING, 301)


@C.texture("casing_top")
def _casing_top():
    img = vc.swatch(CASING_TOP, 302, grain=4)
    for k in range(0, 16, 4):
        vc.rect(img, 0, k, 16, k + 1, vc.shade(CASING_TOP, 0.8))
    return img


@C.texture("bezel")
def _bezel():
    return vc.swatch(BEZEL, 303, grain=2)


@C.texture("screen")
def _screen():
    img = Image.new("RGBA", (16, 16), SCREEN + (255,))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x % 2 == 0 and y % 2 == 0:
                px[x, y] = (22, 24, 30, 255)
    return img


@C.texture("steel")
def _steel():
    return vc.brushed(STEEL, 304)


@C.texture("alu")
def _alu():
    return vc.brushed(ALU, 305)


@C.texture("black")
def _black():
    return vc.swatch(BLACK, 306, grain=2)


def ring_frame(k, n, lit_only=False):
    """The scoreboard's LED ring: a band of amber and white chasers on black, tiling every block
    and moving one block in n frames."""
    def draw():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0) if lit_only else (12, 12, 14, 255))
        px = img.load()
        for y in range(16):
            for x in range(16):
                if y < 2 or y > 13:
                    if not lit_only:
                        px[x, y] = BEZEL + (255,)
                    continue
                phase = ((x - k * 16.0 / n) % 16) / 16.0
                wave = 0.5 + 0.5 * math.cos(2 * math.pi * phase)
                band = 0.5 + 0.5 * math.cos(2 * math.pi * ((y - 2) / 12.0 - phase))
                c = lerp((255, 150, 20), (255, 248, 230), band)
                k2 = 0.25 + 0.75 * wave ** 2
                if (x + y) % 2:
                    k2 *= 0.8
                px[x, y] = vc.clamp(tuple(v * k2 for v in c)) + (255,)
        return img
    return draw


RING_N = 16
vc.animation(C, "ring", [ring_frame(k, RING_N) for k in range(RING_N)], 2)
vc.animation(C, "ring_e", [ring_frame(k, RING_N, True) for k in range(RING_N)], 2)


# ------------------------------------------------------------------------------------------
# The idle video: a 512 px sheet of 32 frames, 128 x 64 each, four across. Not a block texture
# any model names: TileEntityScoreboardRenderer binds it on its own and picks the frame from
# the game time, so it plays whatever an animation optimiser does to the block atlas.
# ------------------------------------------------------------------------------------------
VID_W, VID_H, VID_COLS, VID_FRAMES = 128, 64, 4, 32


def video_frame(f):
    img = Image.new("RGBA", (VID_W, VID_H), (6, 10, 30, 255))
    px = img.load()
    if f < 16:
        # the intro: three searchlights sweeping over a dark blue haze, LET'S GO coming up
        t = f / 16.0
        for y in range(VID_H):
            for x in range(VID_W):
                base = lerp((4, 8, 26), (18, 30, 80), y / float(VID_H))
                light = 0.0
                for i, (ox, sp) in enumerate(((20, 1.0), (64, -1.3), (108, 0.8))):
                    ang = math.sin(2 * math.pi * (t * sp + i / 3.0)) * 0.6
                    dx = x - (ox + (VID_H - y) * math.tan(ang))
                    w = 3 + (VID_H - y) * 0.18
                    if abs(dx) < w:
                        light += (1 - abs(dx) / w) * 0.75
                c = lerp(base, (230, 236, 255), min(1.0, light))
                px[x, y] = c + (255,)
        k = min(1.0, max(0.0, (f - 4) / 6.0))
        if k > 0:
            text = "LET'S GO"
            scale = 4
            w = vc.text_width(text.replace("'", ""), scale) + 6
            x0 = (VID_W - w) // 2
            colour = lerp((40, 60, 120), (255, 255, 255), k)
            vc.draw_text(img, "LET", x0, 22, colour, scale)
            vc.rect(img, x0 + vc.text_width("LET", scale) + 2, 22,
                    x0 + vc.text_width("LET", scale) + 4, 28, colour)
            vc.draw_text(img, "S GO", x0 + vc.text_width("LET", scale) + 6, 22, colour, scale)
    else:
        # the noise meter: MAKE SOME NOISE over a bar that fills and falls back
        g = f - 16
        for y in range(VID_H):
            for x in range(VID_W):
                px[x, y] = lerp((40, 6, 14), (8, 6, 30), x / float(VID_W)) + (255,)
        vc.draw_text_centred(img, "MAKE SOME", VID_W / 2.0, 6, WHITE, 2)
        vc.draw_text_centred(img, "NOISE", VID_W / 2.0, 20, (255, 210, 40), 3)
        level = [0.2, 0.35, 0.5, 0.62, 0.74, 0.83, 0.9, 0.95, 1.0, 0.92, 0.97, 1.0, 0.86, 0.7,
                 0.5, 0.3][g]
        x0, x1, y0, y1 = 10, VID_W - 10, 44, 56
        vc.rect(img, x0 - 1, y0 - 1, x1 + 1, y1 + 1, (200, 200, 210))
        vc.rect(img, x0, y0, x1, y1, (10, 10, 14))
        segs = 20
        for s in range(int(round(level * segs))):
            sx0 = x0 + 1 + s * (x1 - x0 - 2) // segs
            sx1 = x0 + 1 + (s + 1) * (x1 - x0 - 2) // segs - 1
            c = (40, 210, 60) if s < 12 else (250, 210, 40) if s < 17 else (240, 40, 40)
            vc.rect(img, sx0, y0 + 1, sx1, y1 - 1, c)
    return img


@C.texture("scoreboard_video")
def _video():
    rows = VID_FRAMES // VID_COLS
    sheet = Image.new("RGBA", (512, 512), (0, 0, 0, 255))
    for f in range(VID_FRAMES):
        sheet.paste(video_frame(f), ((f % VID_COLS) * VID_W, (f // VID_COLS) * VID_H))
    assert rows * VID_H <= 512
    return sheet


# ------------------------------------------------------------------------------------------
# The centre-hung scoreboard
# ------------------------------------------------------------------------------------------
class Board(object):
    """One size. Blocks, from the master cell's origin, the board centred on the cell's centre
    (0.5, 0.5); the master is the middle cell of the bottom layer."""

    def __init__(self, name, w, d, ring, screen, post):
        self.name, self.w, self.d, self.ring, self.screen, self.post = (name, w, d, ring, screen,
                                                                        post)
        self.top = 2 * ring + screen            # the casing's top
        self.rig = 1.0                          # the rigging above it
        self.cx = self.cz = 0.5
        self.bezel = 1 / 16.0
        self.screen_y0 = ring + self.bezel
        self.screen_y1 = ring + screen - self.bezel

    def hw(self, long_face):
        return (self.w if long_face else self.d) / 2.0

    def boxes(self):
        """The shell (half a block thick) and the rigging's shackle, in pixels: what claims the
        unit's cells and what collides."""
        x0, x1 = (self.cx - self.w / 2) * 16, (self.cx + self.w / 2) * 16
        z0, z1 = (self.cz - self.d / 2) * 16, (self.cz + self.d / 2) * 16
        y1 = self.top * 16
        s = 8
        return [(x0, 0, z0, x1, s, z1), (x0, y1 - s, z0, x1, y1, z1),
                (x0, s, z0, x1, y1 - s, z0 + s), (x0, s, z1 - s, x1, y1 - s, z1),
                (x0, s, z0 + s, x0 + s, y1 - s, z1 - s), (x1 - s, s, z0 + s, x1, y1 - s, z1 - s),
                (4, y1, 4, 12, y1 + 16, 12)]


BOARDS = [Board("LARGE", 12, 8, 1.0, 6.0, 0.25), Board("SMALL", 6, 4, 0.5, 3.0, 0.1875)]


def p(v):
    return v * 16.0


def board_polys(b):
    P = []
    x0, x1 = b.cx - b.w / 2, b.cx + b.w / 2
    z0, z1 = b.cz - b.d / 2, b.cz + b.d / 2
    # each face: (plan p0, plan p1 as seen from outside, left to right; outward normal)
    faces = [((x1, z0), (x0, z0), (0, -1)), ((x0, z1), (x1, z1), (0, 1)),
             ((x0, z0), (x0, z1), (-1, 0)), ((x1, z1), (x1, z0), (1, 0))]

    def tiled(a, c, y0, y1, mat, n, v0=0.0, v1=1.0, step=1.0):
        """A face from plan point a to c, y0..y1 blocks, cut into whole blocks along it so a
        16 px texture keeps its size."""
        length = math.hypot(c[0] - a[0], c[1] - a[1])
        cells = max(1, int(round(length / step)))
        out = []
        for i in range(cells):
            f0, f1 = i / float(cells), (i + 1) / float(cells)
            pa = (a[0] + (c[0] - a[0]) * f0, a[1] + (c[1] - a[1]) * f0)
            pb = (a[0] + (c[0] - a[0]) * f1, a[1] + (c[1] - a[1]) * f1)
            pts = [(p(pa[0]), p(y0), p(pa[1])), (p(pb[0]), p(y0), p(pb[1])),
                   (p(pb[0]), p(y1), p(pb[1])), (p(pa[0]), p(y1), p(pa[1]))]
            out.append(vc.Poly(pts, mat, (n[0], 0, n[1]), [(0, v1), (1, v1), (1, v0), (0, v0)]))
        return out

    ring_v = (2.0 / 16, 14.0 / 16) if b.ring < 1 else (0.0, 1.0)
    for a, c, n in faces:
        # the rings, above and below
        P += tiled(a, c, 0, b.ring, "ring", n, ring_v[0], ring_v[1])
        P += tiled(a, c, b.top - b.ring, b.top, "ring", n, ring_v[0], ring_v[1])
        # the screen between its corner posts, a bezel round it
        inset = b.post
        dx, dz = c[0] - a[0], c[1] - a[1]
        length = math.hypot(dx, dz)
        ux, uz = dx / length, dz / length
        sa = (a[0] + ux * inset, a[1] + uz * inset)
        sc = (c[0] - ux * inset, c[1] - uz * inset)
        rows = int(round(b.screen))
        for r in range(rows):
            ya = b.ring + r * (b.screen / rows)
            yb = ya + b.screen / rows
            P += tiled(sa, sc, ya, yb, "screen", n)
        # bezel strips at the screen's top and foot, a hair proud
        o = 0.01
        sa2 = (sa[0] + n[0] * o, sa[1] + n[1] * o)
        sc2 = (sc[0] + n[0] * o, sc[1] + n[1] * o)
        P += tiled(sa2, sc2, b.ring, b.screen_y0, "bezel", n)
        P += tiled(sa2, sc2, b.screen_y1, b.top - b.ring, "bezel", n)
    # corner posts: square columns at the four vertical edges, a pixel proud of every face
    pr = b.post
    e = 1 / 16.0
    for cx, cz in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        sx = 1 if cx == x0 else -1
        sz = 1 if cz == z0 else -1
        px0, px1 = sorted((cx - sx * e, cx + sx * pr))
        pz0, pz1 = sorted((cz - sz * e, cz + sz * pr))
        P += vc.obox(p(px0), p(px1), p(b.ring), p(b.top - b.ring), p(pz0), p(pz1), "casing",
                     "nsew")
    # the underside and the top deck
    P.append(vc.Poly([(p(x0), 0, p(z0)), (p(x1), 0, p(z0)), (p(x1), 0, p(z1)), (p(x0), 0, p(z1))],
                     "casing", (0, -1, 0), [(0, 0), (1, 0), (1, 1), (0, 1)]))
    top = b.top
    P.append(vc.Poly([(p(x0), p(top), p(z1)), (p(x1), p(top), p(z1)), (p(x1), p(top), p(z0)),
                      (p(x0), p(top), p(z0))], "deck", (0, 1, 0), [(0, 1), (1, 1), (1, 0), (0, 0)]))
    # the rigging: a square hoist frame on the deck, four bridles up to one shackle
    fi = 0.6 if b.w > 8 else 0.4
    fx0, fx1, fz0, fz1 = x0 + fi, x1 - fi, z0 + fi, z1 - fi
    bw = 0.18 if b.w > 8 else 0.12
    for (ax, az, cx2, cz2) in ((fx0, fz0, fx1, fz0 + bw), (fx0, fz1 - bw, fx1, fz1),
                               (fx0, fz0, fx0 + bw, fz1), (fx1 - bw, fz0, fx1, fz1)):
        P += vc.obox(p(ax), p(cx2), p(top), p(top + bw), p(az), p(cz2), "steel", "nsewu")
    sh = (b.cx, top + b.rig - 0.06, b.cz)
    P += vc.obox(p(b.cx - 0.12), p(b.cx + 0.12), p(top + b.rig - 0.32), p(top + b.rig),
                 p(b.cz - 0.12), p(b.cz + 0.12), "steel")
    for ax, az in ((fx0 + bw / 2, fz0 + bw / 2), (fx1 - bw / 2, fz0 + bw / 2),
                   (fx0 + bw / 2, fz1 - bw / 2), (fx1 - bw / 2, fz1 - bw / 2)):
        P += cable((ax, top + bw, az), (sh[0], top + b.rig - 0.3, sh[2]), 0.035)
    return P


def cable(a, c, r):
    """A thin square-section cable from a to c (blocks), four long faces."""
    d = (c[0] - a[0], c[1] - a[1], c[2] - a[2])
    ln = math.sqrt(d[0] ** 2 + d[1] ** 2 + d[2] ** 2)
    u = (d[0] / ln, d[1] / ln, d[2] / ln)
    ref = (0, 1, 0) if abs(u[1]) < 0.9 else (1, 0, 0)
    s = vc._norm(vc._cross(u, ref))
    t = vc._norm(vc._cross(u, s))
    out = []
    corners = [(1, 1), (-1, 1), (-1, -1), (1, -1)]
    for i in range(4):
        (s0, t0), (s1, t1) = corners[i], corners[(i + 1) % 4]
        pa = [p(a[k] + r * (s[k] * s0 + t[k] * t0)) for k in range(3)]
        pb = [p(a[k] + r * (s[k] * s1 + t[k] * t1)) for k in range(3)]
        pc = [p(c[k] + r * (s[k] * s1 + t[k] * t1)) for k in range(3)]
        pd = [p(c[k] + r * (s[k] * s0 + t[k] * t0)) for k in range(3)]
        n = tuple(s[k] * (s0 + s1) + t[k] * (t0 + t1) for k in range(3))
        out.append(vc.Poly([pa, pb, pc, pd], "steel", n))
    return out


AV_MTL = "av"
C.extra["models/block/venues/av/%s.mtl" % AV_MTL] = vc.mtl_text(GEN, {
    "casing": T("casing"), "deck": T("casing_top"), "bezel": T("bezel"), "screen": T("screen"),
    "ring": T("ring"), "steel": T("steel"), "alu": T("alu"), "black": T("black"),
    "cab": T("speaker_cab"), "grille": T("speaker_grille"), "led": T("ribbon_led"),
    "frame": T("ribbon_frame")})


def write_obj(name, polys):
    o = vc.Obj(GEN, AV_MTL, name)
    for poly in polys:
        o.add(poly)
    C.extra["models/block/venues/av/%s.obj" % name] = o.text()
    return M(name) + ".obj"


def unit_state(obj_model, icon):
    return {"forge_marker": 1, "defaults": {"model": obj_model, "custom": {"ambient": False}},
            "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                         "inventory": [{"model": M(icon), "custom": {}}]}}


def scoreboard_icon(small):
    """A small board in the slot: four screens between two rings, a shackle on top."""
    tex = {"casing": T("casing"), "screen": T("scoreboard_icon"), "ring": T("ring"),
           "steel": T("steel"), "particle": T("casing")}
    w = 6 if small else 7
    x0, x1 = 8 - w, 8 + w
    z0, z1 = 8 - (w - 2), 8 + (w - 2)
    els = [box([x0, 2, z0], [x1, 4, z1], "ring"), box([x0, 4, z0], [x1, 11, z1], "screen"),
           box([x0, 11, z0], [x1, 13, z1], "ring"),
           box([7, 13, 7], [9, 16, 9], "steel")]
    for f in ("north", "south", "east", "west"):
        els[1]["faces"][f]["uv"] = [0, 0, 16, 16]
    return vc.model(tex, els)


@C.texture("scoreboard_icon")
def _scoreboard_icon():
    img = Image.new("RGBA", (16, 16), SCREEN + (255,))
    vc.rect(img, 1, 2, 6, 4, NAVY)
    vc.rect(img, 10, 2, 15, 4, CRIMSON)
    vc.rect(img, 2, 6, 5, 11, (255, 180, 40))
    vc.rect(img, 11, 6, 14, 11, (255, 180, 40))
    vc.rect(img, 6, 3, 10, 6, (255, 200, 60))
    return img


def size_java():
    lines = ["package com.micatechnologies.minecraft.csm.venues.av;", "",
             "/**",
             " * The centre-hung scoreboard's sizes: the casing, the screens and the shell boxes",
             " * that claim its cells, in blocks from the master cell's origin (the master is the",
             " * middle cell of the bottom layer, the board centred on that cell's centre, drawn",
             " * facing north).",
             " *",
             " * <p>Generated by {@code dev-env-utils/scripts/gen_venues_av.py} from the numbers the",
             " * OBJ is drawn with; do not edit by hand.</p>",
             " *",
             " * @since 2026.10",
             " */",
             "public enum ScoreboardSize {", ""]
    entries = []
    for b in BOARDS:
        flat = ", ".join(", ".join("%d" % int(round(v)) for v in bx) for bx in b.boxes())
        entries.append("  /** %d x %d, %s tall. */\n  %s(%sf, %sf, %sf, %sf, %sf, %sf, %sf, %sf,\n"
                       "      new int[]{%s})" % (
                           b.w, b.d, _n(b.top + b.rig), b.name, _n(b.w), _n(b.d), _n(b.ring),
                           _n(b.top), _n(b.post), _n(b.screen_y0), _n(b.screen_y1), _n(b.rig),
                           flat))
    lines.append(",\n\n".join(entries) + ";")
    lines += ["",
              "  /** Width (x) and depth (z) of the casing, blocks. */",
              "  public final float width;",
              "  public final float depth;",
              "  /** Height of each LED ring, blocks. */",
              "  public final float ring;",
              "  /** The casing's top, blocks above the master cell's floor. */",
              "  public final float top;",
              "  /** The corner posts' width: each screen stops this far short of its face's ends. */",
              "  public final float post;",
              "  /** The screens' foot and top, blocks above the master cell's floor. */",
              "  public final float screenBottom;",
              "  public final float screenTop;",
              "  /** The rigging above the casing, blocks. */",
              "  public final float rig;",
              "  /** The shell boxes, pixels: x0, y0, z0, x1, y1, z1 each. */",
              "  final int[] boxes;", "",
              "  ScoreboardSize(float width, float depth, float ring, float top, float post,",
              "      float screenBottom, float screenTop, float rig, int[] boxes) {",
              "    this.width = width;",
              "    this.depth = depth;",
              "    this.ring = ring;",
              "    this.top = top;",
              "    this.post = post;",
              "    this.screenBottom = screenBottom;",
              "    this.screenTop = screenTop;",
              "    this.rig = rig;",
              "    this.boxes = boxes;",
              "  }",
              "",
              "  /** A long face's screen width (the north and south faces), blocks. */",
              "  public float longScreen() {",
              "    return width - 2 * post;",
              "  }",
              "",
              "  /** A short face's screen width (east and west), blocks. */",
              "  public float shortScreen() {",
              "    return depth - 2 * post;",
              "  }",
              "",
              "  /** A screen's height, blocks. */",
              "  public float screenHeight() {",
              "    return screenTop - screenBottom;",
              "  }",
              "}", ""]
    return "\n".join(lines)


def _n(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    return s if "." in s else s + ".0" if False else s


for _b in BOARDS:
    _reg = "centre_hung_scoreboard" + ("" if _b.name == "LARGE" else "_small")
    _obj = write_obj(_reg, board_polys(_b))
    _icon = _reg + "_item"
    C.add(_reg, 'new BlockScoreboard("%s", ScoreboardSize.%s)' % (_reg, _b.name),
          (("Centre-Hung Scoreboard", "Hängender Videowürfel", "Marcador Central Colgante",
            "Hängande Mediakub") if _b.name == "LARGE" else
           ("Centre-Hung Scoreboard (Small)", "Hängender Videowürfel (klein)",
            "Marcador Central Colgante (Pequeño)", "Hängande Mediakub (Liten)")),
          {_icon: scoreboard_icon(_b.name == "SMALL")}, unit_state(_obj, _icon), tab=ARENA)

JAVA["ScoreboardSize.java"] = size_java()

# ------------------------------------------------------------------------------------------
# The LED ribbon board
# ------------------------------------------------------------------------------------------
RIB_N = 32          # frames; a pattern moves one 4-block period in RIB_N frames
RIB_Z = 13.5        # the LED face, pixels from the cell's front (the band backs onto +z)
RIB_LED = (2, 14)   # its LED rows


def ribbon_frame(k, lit_only=False):
    """One 64 x 64 frame: four patterns, 16 rows each, each 64 px (four blocks) long and tiling
    at that length. Pattern rows: the LED field fills rows 2-13 of its 16."""
    def draw():
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        px = img.load()
        t = k / float(RIB_N)
        crawl = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
        _crawl_text(crawl)
        cpx = crawl.load()
        for pat in range(4):
            for y in range(16):
                for x in range(64):
                    if y < RIB_LED[0] or y >= RIB_LED[1]:
                        if not lit_only:
                            px[x, pat * 16 + y] = (8, 8, 10, 255)
                        continue
                    u = x / 64.0
                    ly = (y - RIB_LED[0]) / float(RIB_LED[1] - RIB_LED[0])
                    if pat == 0:
                        # navy into ice blue, a white head running along it
                        ph = (u - t) % 1.0
                        c = lerp(NAVY, ICE, ph)
                        if ph > 0.92:
                            c = lerp(c, WHITE, (ph - 0.92) / 0.08)
                    elif pat == 1:
                        # crimson and gold chevrons marching
                        ph = (u * 4 - t * 4 + abs(ly - 0.5) * 0.8) % 1.0
                        c = CRIMSON if ph < 0.5 else GOLD
                    elif pat == 2:
                        # green with white pulses chasing through it
                        ph = (u * 2 - t * 2) % 1.0
                        k2 = max(0.0, 1 - abs(ph - 0.5) * 6)
                        c = lerp((10, 70, 34), WHITE, k2)
                        if k2 == 0 and ly < 0.18 or ly > 0.82:
                            c = GREEN if k2 == 0 else c
                    else:
                        # the white crawl: LET'S GO sliding left on black
                        sx = int(round(x + t * 64)) % 64
                        a = cpx[sx, y][3]
                        c = WHITE if a else (8, 8, 10)
                        if lit_only and not a:
                            continue
                    if (x + y) % 2:
                        c = vc.shade(c, 0.86)
                    px[x, pat * 16 + y] = vc.clamp(c) + (255,)
        return img
    return draw


def _crawl_text(img):
    """LET'S GO in the 3 x 5 font at twice its size, 64 px wide with its gap."""
    colour = WHITE
    vc.draw_text(img, "LET", 2, RIB_LED[0] + 1, colour, 2)
    vc.rect(img, 2 + vc.text_width("LET", 2) + 2, RIB_LED[0] + 1,
            2 + vc.text_width("LET", 2) + 4, RIB_LED[0] + 5, colour)
    vc.draw_text(img, "S GO", 2 + vc.text_width("LET", 2) + 6, RIB_LED[0] + 1, colour, 2)


vc.animation(C, "ribbon_led", [ribbon_frame(k) for k in range(RIB_N)], 2)
vc.animation(C, "ribbon_led_e", [ribbon_frame(k, True) for k in range(RIB_N)], 2)


@C.texture("ribbon_frame")
def _ribbon_frame():
    return vc.swatch(BLACK, 311, grain=2)


RIB_TEX = {"frame": T("ribbon_frame"), "back": T("steel"), "led": T("ribbon_led"),
           "particle": T("ribbon_frame")}
PATTERNS = ("team_navy", "team_crimson", "team_green", "white_crawl")


def ribbon_model(pat, phase):
    """The straight: a housing on the back of the cell, black lips above and below, the LED
    face between showing pattern ``pat`` at ``phase`` (which quarter of the four-block period)."""
    els = [box([0, 0, RIB_Z], [16, 16, 16], "frame", faces=("east", "west", "up", "down")),
           box([0, 0, 15.5], [16, 16, 16], "back", faces=("south",)),
           box([0, 0, 13], [16, 2, RIB_Z], "frame"),
           box([0, 14, 13], [16, 16, RIB_Z], "frame")]
    # the north face's uv: u0 is the viewer's left (+x side); phase k shows u 4k..4k+4
    v0 = pat * 4 + RIB_LED[0] / 4.0
    v1 = pat * 4 + RIB_LED[1] / 4.0
    els.append({"from": [0, RIB_LED[0], RIB_Z], "to": [16, RIB_LED[1], RIB_Z],
                "shade": False,
                "faces": {"north": {"texture": "#led", "uv": [phase * 4, v0, phase * 4 + 4, v1]}}})
    return vc.model(RIB_TEX, els, ao=False)


def ribbon_models():
    out = {}
    for pat in range(4):
        for ph in range(4):
            out["ribbon_%d_%d" % (pat, ph)] = ribbon_model(pat, ph)
    return out


def ribbon_state(text=False):
    props = [("facing", [f for f, _ in FACINGS]), ("phase", ["0", "1", "2", "3"])]
    if not text:
        props.insert(1, ("pattern", list(PATTERNS)))
    ys = dict(FACINGS)

    def choose(v):
        pat = PATTERNS.index(v["pattern"]) if not text else 3
        d = {"model": M("ribbon_%d_%s" % (pat, v["phase"]))}
        if ys[v["facing"]]:
            d["y"] = ys[v["facing"]]
        return d
    return vc.keyed_state(props, choose, inventory={"model": M("ribbon_%d_0" % (3 if text else 0))})


# --- the 45 degree corner: drawn as at a bowl's north-west corner, the band facing south-east ---
RIB_T = 16 - RIB_Z          # the LED face's distance in front of the back edge


def corner_polys(pat):
    t = RIB_T
    # the face line x + z = 16 + t, from the viewer's left (south-west) to right (north-east)
    left = (t / 2.0, 16 + t / 2.0)
    right = (16 + t / 2.0, t / 2.0)
    n = (1, 1)
    P = []
    lip = 0.5 / math.sqrt(2)
    lft2 = (left[0] + lip, left[1] + lip)
    rgt2 = (right[0] + lip, right[1] + lip)
    u0, u1 = 0.0, 0.25
    v0 = (pat * 16 + RIB_LED[0]) / 64.0
    v1 = (pat * 16 + RIB_LED[1]) / 64.0
    P.append(vc.Poly([(left[0], RIB_LED[0], left[1]), (right[0], RIB_LED[0], right[1]),
                      (right[0], RIB_LED[1], right[1]), (left[0], RIB_LED[1], left[1])],
                     "led", (n[0], 0, n[1]), [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]))
    for y0, y1 in ((0, RIB_LED[0]), (RIB_LED[1], 16)):
        P.append(vc.ovquad(lft2, rgt2, y0, y1, "frame", n))
    # the housing behind: its top, foot and back
    d = 2.5
    back = (-d / math.sqrt(2) / 2, -d / math.sqrt(2) / 2)
    bl = (left[0] + back[0] * 2, left[1] + back[1] * 2)
    br = (right[0] + back[0] * 2, right[1] + back[1] * 2)
    P.append(vc.ohpoly([lft2, rgt2, br, bl], 16, "frame", up=True))
    P.append(vc.ohpoly([lft2, rgt2, br, bl], 0, "frame", up=False))
    P.append(vc.ovquad(br, bl, 0, 16, "back_steel", (-1, -1)))
    return P


C.extra["models/block/venues/av/ribbon.mtl"] = vc.mtl_text(GEN, {
    "led": T("ribbon_led"), "frame": T("ribbon_frame"), "back_steel": T("steel")})


def write_ribbon_obj(name, polys):
    o = vc.Obj(GEN, "ribbon", name)
    for poly in polys:
        o.add(poly)
    C.extra["models/block/venues/av/%s.obj" % name] = o.text()
    return M(name) + ".obj"


CORNER_OBJ = [write_ribbon_obj("ribbon_corner_%d" % pat, corner_polys(pat)) for pat in range(4)]
# the corner's facing (dasher convention): the band faces between FACING and FACING turned
# clockwise; the model is drawn facing south-east, which is FACING east
CORNER_Y = {"east": 0, "south": 90, "west": 180, "north": 270}


def corner_state():
    props = [("facing", [f for f, _ in FACINGS]), ("pattern", list(PATTERNS))]

    def choose(v):
        d = {"model": CORNER_OBJ[PATTERNS.index(v["pattern"])], "custom": {"ambient": False}}
        if CORNER_Y[v["facing"]]:
            d["y"] = CORNER_Y[v["facing"]]
        return d
    return vc.keyed_state(props, choose, inventory={"model": M("ribbon_corner_item"),
                                                     "custom": {}})


def corner_icon():
    els = []
    for el in ribbon_model(0, 0)["elements"]:
        el = dict(el)
        el["from"] = [el["from"][0], el["from"][1], el["from"][2] - 6]
        el["to"] = [el["to"][0], el["to"][1], el["to"][2] - 6]
        el["rotation"] = {"origin": [8, 8, 8], "axis": "y", "angle": 45}
        els.append(el)
    return vc.model(RIB_TEX, els, ao=False)


_rib_models = ribbon_models()
C.add("led_ribbon_board", 'new BlockLedRibbon("led_ribbon_board")',
      ("LED Ribbon Board", "LED-Bandanzeige", "Cinta LED", "LED-Bandskärm"),
      _rib_models, ribbon_state(), tab=ARENA)
C.add("led_ribbon_board_corner", 'new BlockLedRibbonCorner("led_ribbon_board_corner")',
      ("LED Ribbon Board (45 Degree Corner)", "LED-Bandanzeige (45-Grad-Ecke)",
       "Cinta LED (Esquina de 45 Grados)", "LED-Bandskärm (45-Gradershörn)"),
      {"ribbon_corner_item": corner_icon()}, corner_state(), tab=ARENA)
C.add("led_ribbon_board_text", 'new BlockLedRibbonText("led_ribbon_board_text")',
      ("LED Ribbon Board (Scrolling Text)", "LED-Bandanzeige (Lauftext)",
       "Cinta LED (Texto Desplazable)", "LED-Bandskärm (Rullande Text)"),
      {}, ribbon_state(text=True), tab=ARENA)

# ------------------------------------------------------------------------------------------
# Banners
# ------------------------------------------------------------------------------------------
@C.texture("banner_cloth")
def _banner_cloth():
    rng = random.Random(321)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            k = 0.9 + (0.06 if (x + y) % 2 else 0) + rng.uniform(-0.03, 0.03)
            px[x, y] = vc.clamp((236 * k, 236 * k, 236 * k)) + (255,)
    return img


@C.texture("banner_trim")
def _banner_trim():
    img = vc.swatch((240, 240, 240), 322, grain=3)
    vc.rect(img, 0, 0, 16, 1, (210, 210, 210))
    vc.rect(img, 0, 15, 16, 16, (210, 210, 210))
    return img


BAN_TEX = {"cloth": T("banner_cloth"), "trim": T("banner_trim"), "alu": T("alu"),
           "particle": T("banner_cloth")}
BAN_Z = (7.75, 8.25)


def tint(el, idx):
    for f in el["faces"].values():
        f["tintindex"] = idx
    return el


def banner_model(large):
    """The cloth from its batten down: main cloth (tint 0) over a trim band (tint 1) at its
    foot, a thin trim stripe under the batten. Large: 2 x 3 with the master in the middle row
    of the left column; small: 1 x 2, the master the bottom cell."""
    x0, x1 = 1, (31 if large else 15)
    y_top = 30
    y_foot = -15 if large else 1
    trim = 5 if large else 3
    z0, z1 = BAN_Z
    els = [tint(box([x0, y_foot + trim, z0], [x1, y_top - 3, z1], "cloth"), 0),
           tint(box([x0, y_top - 3, z0], [x1, y_top - 2, z1], "trim"), 1),
           tint(box([x0, y_top - 2, z0], [x1, y_top, z1], "cloth"), 0),
           tint(box([x0, y_foot, z0], [x1, y_foot + trim, z1], "trim"), 1),
           box([0, y_top, 7], [x1 + 1, y_top + 1.5, 9], "alu")]
    for hx in ((3, x1 - 3) if large else (2.5, x1 - 2.5)):
        els.append(box([hx - 0.25, y_top + 1.5, 7.75], [hx + 0.25, 32, 8.25], "alu"))
    # the cloth's faces keep the whole 16 px weave across them
    for el in els[:4]:
        for f in ("north", "south"):
            el["faces"][f]["uv"] = [0, 0, 16, 16]
    return vc.model(BAN_TEX, els)


def banner_icon(large):
    els = [tint(box([3, 4, 7.5], [13, 13, 8.5], "cloth"), 0),
           tint(box([3, 1, 7.5], [13, 4, 8.5], "trim"), 1),
           box([2, 13, 7], [14, 14.5, 9], "alu")]
    m = vc.model(BAN_TEX, els)
    return m


def banner_boxes(large):
    if large:
        return [(0, -15, 7, 32, 32, 9)]
    return [(0, 1, 7, 16, 32, 9)]


def unit_cells(boxes_px):
    """The arena generator's rule: every cell a box reaches 6 px into, but the master."""
    def axis(lo, hi, tol=6.0):
        out = []
        for c in range(int(math.floor(lo / 16.0)), int(math.ceil(hi / 16.0))):
            ov = min(hi, 16.0 * (c + 1)) - max(lo, 16.0 * c)
            if ov >= min(tol, hi - lo - 1e-6):
                out.append(c)
        return out
    cells = set()
    for b in boxes_px:
        for cx in axis(b[0], b[3]):
            for cy in axis(b[1], b[4]):
                for cz in axis(b[2], b[5]):
                    cells.add((cx, cy, cz))
    cells.discard((0, 0, 0))
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def unit_java(cls, reg, boxes_px, extra_args=""):
    cells = unit_cells(boxes_px)
    flat_c = ", ".join("%d, %d, %d" % c for c in cells)
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s"%s, new int[]{%s}, new int[]{%s})' % (cls, reg, extra_args, flat_c,
                                                              flat_b)


BANNERS = [
    ("championship_banner", True, False, ("Championship Banner", "Meisterschaftsbanner",
                                          "Estandarte de Campeonato", "Mästerskapsbanderoll")),
    ("championship_banner_small", False, False,
     ("Championship Banner (Small)", "Meisterschaftsbanner (klein)",
      "Estandarte de Campeonato (Pequeño)", "Mästerskapsbanderoll (Liten)")),
    ("retired_number_banner", True, True,
     ("Retired Number Banner", "Banner für gesperrte Rückennummer",
      "Estandarte de Número Retirado", "Banderoll för Pensionerat Nummer")),
    ("retired_number_banner_small", False, True,
     ("Retired Number Banner (Small)", "Banner für gesperrte Rückennummer (klein)",
      "Estandarte de Número Retirado (Pequeño)", "Banderoll för Pensionerat Nummer (Liten)")),
]
for _reg, _large, _retired, _names in BANNERS:
    _m = "banner_large" if _large else "banner_small"
    _models = {_m: banner_model(_large), _m + "_item": banner_icon(_large)}
    _state = {"forge_marker": 1, "defaults": {"model": M(_m)},
              "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                           "inventory": [{"model": M(_m + "_item")}]}}
    C.add(_reg, unit_java("BlockVenueBanner", _reg, banner_boxes(_large),
                          ", %s, %s" % ("true" if _large else "false",
                                        "true" if _retired else "false")),
          _names, _models, _state, tab=ARENA)

# ------------------------------------------------------------------------------------------
# The line-array speaker cluster (OBJ: the cabinets curve in a J)
# ------------------------------------------------------------------------------------------
@C.texture("speaker_cab")
def _speaker_cab():
    return vc.swatch(CAB, 331, grain=3)


@C.texture("speaker_grille")
def _speaker_grille():
    img = Image.new("RGBA", (16, 16), GRILLE + (255,))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x % 2 == 0) and (y % 2 == 0):
                px[x, y] = (14, 14, 16, 255)
    vc.rect(img, 0, 0, 16, 1, CAB)
    vc.rect(img, 0, 15, 16, 16, CAB)
    return img


SPK_ANGLES = (0, 0.5, 1, 2, 3.5, 5.5, 8, 11, 14.5)


def speaker_polys():
    P = []
    # the fly bar at the top, a shackle up to the cell above's floor
    P += vc.obox(1, 15, 26.5, 28.5, 2, 13, "steel")
    P += vc.obox(7, 9, 28.5, 32, 6.5, 8.5, "steel")
    pivot = (0.0, 26.5, 3.0)      # (unused x), y, z of the first cabinet's front-top edge
    cab_h, cab_d = 4.2, 9.0
    y, z = pivot[1], pivot[2]
    for a in SPK_ANGLES:
        th = math.radians(a)
        cy, sy = math.cos(th), math.sin(th)

        def tr(ly, lz):
            # local (ly down from the top, lz back from the front) to model (y, z)
            return (y + ly * cy + lz * sy, z - ly * sy + lz * cy)
        corners = [(0, 0), (-cab_h, 0), (-cab_h, cab_d), (0, cab_d)]   # FT, FB, BB, BT
        pts = [tr(*c) for c in corners]
        x0, x1 = 1.5, 14.5
        ft, fb, bb, bt = pts
        nf = (0, -sy, -cy)
        P.append(vc.Poly([(x1, fb[0], fb[1]), (x0, fb[0], fb[1]), (x0, ft[0], ft[1]),
                          (x1, ft[0], ft[1])], "grille", nf))
        P.append(vc.Poly([(x0, bb[0], bb[1]), (x1, bb[0], bb[1]), (x1, bt[0], bt[1]),
                          (x0, bt[0], bt[1])], "cab", (0, sy, cy)))
        P.append(vc.Poly([(x0, bt[0], bt[1]), (x1, bt[0], bt[1]), (x1, ft[0], ft[1]),
                          (x0, ft[0], ft[1])], "cab", (0, cy, -sy)))
        P.append(vc.Poly([(x0, fb[0], fb[1]), (x1, fb[0], fb[1]), (x1, bb[0], bb[1]),
                          (x0, bb[0], bb[1])], "cab", (0, -cy, sy)))
        for x, s in ((x0, -1), (x1, 1)):
            P.append(vc.Poly([(x, ft[0], ft[1]), (x, fb[0], fb[1]), (x, bb[0], bb[1]),
                              (x, bt[0], bt[1])], "cab", (s, 0, 0)))
        y, z = fb[0] - 0.15 * cy, fb[1] + 0.15 * sy
    return P


C.extra["models/block/venues/av/speaker.mtl"] = vc.mtl_text(GEN, {
    "steel": T("steel"), "cab": T("speaker_cab"), "grille": T("speaker_grille")})


def speaker_icon():
    tex = {"cab": T("speaker_cab"), "grille": T("speaker_grille"), "steel": T("steel"),
           "particle": T("speaker_cab")}
    els = [box([5, 15, 6], [11, 16, 10], "steel")]
    for i in range(5):
        y1 = 15 - i * 3
        el = box([4, y1 - 3, 5], [12, y1, 11], "cab")
        el["faces"]["north"]["texture"] = "#grille"
        if i >= 3:
            el["rotation"] = {"origin": [8, y1, 5], "axis": "x", "angle": 22.5}
        els.append(el)
    return vc.model(tex, els)


_spk = vc.Obj(GEN, "speaker", "line_array_speaker")
for _poly in speaker_polys():
    _spk.add(_poly)
C.extra["models/block/venues/av/line_array_speaker.obj"] = _spk.text()
C.add("line_array_speaker", unit_java("BlockVenueUnit", "line_array_speaker",
                                      [(1, -15, 2, 15, 32, 14)], ", true"),
      ("Line Array Speaker Cluster", "Line-Array-Lautsprecher", "Altavoces Line Array",
       "Line Array-Högtalare"),
      {"line_array_speaker_item": speaker_icon()},
      unit_state(M("line_array_speaker") + ".obj", "line_array_speaker_item"), tab=ARENA)

# ------------------------------------------------------------------------------------------
# The broadcast camera platform (2 x 2, 2 high, the camera looking north)
# ------------------------------------------------------------------------------------------
@C.texture("deck_plate")
def _deck_plate():
    img = vc.swatch((132, 136, 140), 341, grain=3)
    for y in range(0, 16, 4):
        for x in range(0, 16, 4):
            o = 2 if (y // 4) % 2 else 0
            vc.rect(img, (x + o) % 16, y + 1, (x + o) % 16 + 2, y + 2, (170, 174, 178))
    return img


@C.texture("camera_body")
def _camera_body():
    return vc.swatch(CAM_GREY, 342, grain=3)


@C.texture("camera_lens")
def _camera_lens():
    img = vc.swatch(BLACK, 343, grain=2)
    vc.disc(img, 8, 8, 5, LENS)
    vc.disc(img, 6.5, 6.5, 1.5, (90, 110, 150))
    return img


@C.texture("skirt")
def _skirt():
    return vc.swatch((30, 30, 32), 344, grain=2)


CAM_TEX = {"deck": T("deck_plate"), "skirt": T("skirt"), "rail": T("black"),
           "body": T("camera_body"), "lens": T("camera_lens"), "steel": T("steel"),
           "particle": T("deck_plate")}


def camera_parts():
    els = [box([0, 0, 0], [32, 13, 32], "skirt", faces=("north", "south", "east", "west",
                                                          "down")),
           box([0, 13, 0], [32, 14, 32], "deck")]
    # rails along the back and both sides, the front left open for the lens
    for x0, z0, x1, z1 in ((0, 30.5, 32, 32), (0, 0, 1.5, 32), (30.5, 0, 32, 32)):
        els.append(box([x0, 28, z0], [x1, 29.5, z1], "rail"))
        els.append(box([x0, 21, z0 + 0.25 if z1 - z0 < 2 else z0],
                       [x1, 22, z1 - 0.25 if z1 - z0 < 2 else z1], "rail"))
    for x, z in ((0, 0), (30.5, 0), (0, 30.5), (30.5, 30.5), (0, 15.25), (30.5, 15.25),
                 (15.25, 30.5)):
        els.append(box([x, 14, z], [x + 1.5, 29.5, z + 1.5], "rail"))
    # the pedestal and the camera, its lens forward (north)
    els += vc.lsc.post(16, 18, 1.2, 14, 23, "steel")
    els.append(box([11, 14, 13], [21, 15, 23], "steel"))
    els.append(box([12, 23, 9], [20, 29.5, 24], "body"))
    els.append(box([13.5, 24.5, 2], [18.5, 29, 9], "body"))
    lens = box([14, 25, 1.75], [18, 28.5, 2], "lens", faces=("north",))
    lens["faces"]["north"]["uv"] = [0, 0, 16, 16]
    els.append(lens)
    els.append(box([19.5, 27, 19], [23, 30, 23], "body"))
    els.append(box([10, 25, 23], [11, 26, 30], "steel"))
    els.append(box([21, 25, 23], [22, 26, 30], "steel"))
    return els


def camera_icon():
    els = [box([2, 0, 2], [14, 5, 14], "skirt"), box([2, 5, 2], [14, 5.5, 14], "deck"),
           box([6, 8, 5], [10, 11, 12], "body"), box([6.75, 8.5, 2], [9.25, 10.5, 5], "body"),
           box([7.5, 5.5, 8], [8.5, 8, 9], "steel")]
    return vc.model(CAM_TEX, els)


C.add("broadcast_camera_platform",
      unit_java("BlockVenueUnit", "broadcast_camera_platform",
                [(0, 0, 0, 32, 14, 32), (8, 14, 6, 24, 30, 26)]),
      ("Broadcast Camera Platform", "TV-Kamerapodest", "Plataforma de Cámara de TV",
       "TV-Kameraplattform"),
      {"camera_platform": vc.model(CAM_TEX, camera_parts()),
       "camera_platform_item": camera_icon()},
      {"forge_marker": 1, "defaults": {"model": M("camera_platform")},
       "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                    "inventory": [{"model": M("camera_platform_item")}]}}, tab=ARENA)

# ------------------------------------------------------------------------------------------
# The catwalk grating: a floor of bar grating, a rail on every side with no catwalk beside it
# ------------------------------------------------------------------------------------------
@C.texture("grating")
def _grating():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(0, 16, 2):
        vc.rect(img, x, 0, x + 1, 16, (150, 154, 158))
    for y in (0, 8):
        vc.rect(img, 0, y, 16, y + 1, (128, 132, 136))
    return img


CAT_TEX = {"grating": T("grating"), "rail": T("steel"), "toe": T("steel"),
           "particle": T("steel")}


def catwalk_floor():
    els = [box([0, 0, 0], [16, 1.5, 16], "grating", faces=("up", "down")),
           box([0, 0, 0], [16, 1.5, 0.5], "rail", faces=("north", "up", "down")),
           box([0, 0, 15.5], [16, 1.5, 16], "rail", faces=("south", "up", "down"))]
    return vc.model(CAT_TEX, els, ao=False)


def catwalk_rail():
    """The rail on the north side; the blockstate turns it to the others."""
    els = [box([0, 1.5, 0], [16, 4, 0.5], "toe"),
           box([0, 7.5, 0.1], [16, 8.5, 0.6], "rail"),
           box([0, 14.5, 0], [16, 15.5, 1], "rail"),
           box([0, 1.5, 0], [1, 14.5, 1], "rail"),
           box([15, 1.5, 0], [16, 14.5, 1], "rail")]
    return vc.model(CAT_TEX, els)


def catwalk_state():
    parts = [{"apply": {"model": M("catwalk_floor")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        d = {"model": M("catwalk_rail")}
        if y:
            d["y"] = y
        parts.append({"when": {side: "true"}, "apply": d})
    return {"multipart": parts}


C.add("venue_catwalk", 'new BlockVenueCatwalk()',
      ("Catwalk Grating", "Laufsteg-Gitterrost", "Pasarela de Rejilla", "Gångbrygga med Galler"),
      {"catwalk_floor": catwalk_floor(), "catwalk_rail": catwalk_rail(),
       "catwalk_item": vc.model(CAT_TEX, catwalk_floor()["elements"]
                                + catwalk_rail()["elements"])},
      catwalk_state(), item={"parent": "csm:block/venues/av/catwalk_item"}, tab=ARENA)

# ------------------------------------------------------------------------------------------
# Messages: the screens, the colours, the clock
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("gui.csm.scoreboard.title", ("Scoreboard", "Anzeigetafel", "Marcador", "Resultattavla"))
L("gui.csm.scoreboard.home", ("Home", "Heim", "Local", "Hemma"))
L("gui.csm.scoreboard.away", ("Away", "Gast", "Visitante", "Borta"))
L("gui.csm.scoreboard.score", ("Score", "Tore", "Marcador", "Mål"))
L("gui.csm.scoreboard.shots", ("Shots", "Schüsse", "Tiros", "Skott"))
L("gui.csm.scoreboard.period", ("Period", "Drittel", "Periodo", "Period"))
L("gui.csm.scoreboard.clock", ("Clock (m:ss)", "Uhr (m:ss)", "Reloj (m:ss)", "Klocka (m:ss)"))
L("gui.csm.scoreboard.length", ("Period length (m:ss)", "Drittellänge (m:ss)",
                                "Duración del periodo (m:ss)", "Periodlängd (m:ss)"))
L("gui.csm.scoreboard.start", ("Start Clock", "Uhr starten", "Iniciar reloj", "Starta klockan"))
L("gui.csm.scoreboard.stop", ("Stop Clock", "Uhr anhalten", "Parar reloj", "Stoppa klockan"))
L("gui.csm.scoreboard.reset", ("Reset Clock", "Uhr zurücksetzen", "Reiniciar reloj",
                               "Nollställ klockan"))
L("gui.csm.scoreboard.colour", ("Colour: %s", "Farbe: %s", "Color: %s", "Färg: %s"))
L("gui.csm.scoreboard.mode", ("Screens: %s", "Bildschirme: %s", "Pantallas: %s",
                              "Skärmar: %s"))
L("gui.csm.scoreboard.mode.0", ("Score while the clock runs, else video",
                                "Spielstand bei laufender Uhr, sonst Video",
                                "Marcador con el reloj en marcha, si no vídeo",
                                "Resultat när klockan går, annars video"))
L("gui.csm.scoreboard.mode.1", ("Score", "Spielstand", "Marcador", "Resultat"))
L("gui.csm.scoreboard.mode.2", ("Video", "Video", "Vídeo", "Video"))
L("gui.csm.scoreboard.hint", ("Sneak-click the board, or a redstone pulse, starts and stops the "
                              "clock", "Schleichklick auf die Tafel oder ein Redstone-Impuls "
                              "startet und stoppt die Uhr", "Clic agachado en el marcador, o un "
                              "pulso de redstone, inicia y para el reloj",
                              "Smygklick på tavlan eller en redstonepuls startar och stoppar "
                              "klockan"))
L("csm.venues.scoreboard.running", ("Clock running", "Uhr läuft", "Reloj en marcha",
                                    "Klockan går"))
L("csm.venues.scoreboard.stopped", ("Clock stopped at %s", "Uhr angehalten bei %s",
                                    "Reloj parado en %s", "Klockan stoppad på %s"))
L("gui.csm.venue_text.line", ("Line %s", "Zeile %s", "Línea %s", "Rad %s"))
L("gui.csm.venue_text.text", ("Text", "Text", "Texto", "Text"))
L("gui.csm.venue_text.ribbon.0", ("Scrolling text", "Lauftext", "Texto desplazable",
                                  "Rullande text"))
L("gui.csm.venue_text.banner.0", ("Top line", "Obere Zeile", "Línea superior", "Övre rad"))
L("gui.csm.venue_text.banner.1", ("Big line (a year or a number)",
                                  "Große Zeile (Jahr oder Nummer)",
                                  "Línea grande (un año o un número)",
                                  "Stor rad (ett år eller ett nummer)"))
L("gui.csm.venue_text.banner.2", ("Bottom line", "Untere Zeile", "Línea inferior",
                                  "Nedre rad"))
L("gui.csm.venue_text.choice.text", ("Text: %s", "Text: %s", "Texto: %s", "Text: %s"))
L("gui.csm.venue_text.choice.back", ("Background: %s", "Hintergrund: %s", "Fondo: %s",
                                     "Bakgrund: %s"))
L("gui.csm.venue_text.choice.speed", ("Speed: %s", "Geschwindigkeit: %s", "Velocidad: %s",
                                      "Hastighet: %s"))
L("gui.csm.venue_text.choice.cloth", ("Cloth: %s", "Stoff: %s", "Tela: %s", "Tyg: %s"))
L("gui.csm.venue_text.choice.trim", ("Trim: %s", "Besatz: %s", "Ribete: %s", "Kant: %s"))
for _i, _names in enumerate((("Still", "Stehend", "Fijo", "Stilla"),
                             ("Slow", "Langsam", "Lento", "Långsam"),
                             ("Medium", "Mittel", "Media", "Medel"),
                             ("Fast", "Schnell", "Rápido", "Snabb"))):
    L("csm.venues.speed.%d" % _i, _names)
COLOURS = [
    ("Navy", "Marineblau", "Azul marino", "Marinblå"),
    ("Royal Blue", "Königsblau", "Azul real", "Kungsblå"),
    ("Ice Blue", "Eisblau", "Azul hielo", "Isblå"),
    ("Teal", "Petrol", "Verde azulado", "Blågrön"),
    ("Green", "Grün", "Verde", "Grön"),
    ("Forest Green", "Waldgrün", "Verde bosque", "Skogsgrön"),
    ("Gold", "Gold", "Oro", "Guld"),
    ("Orange", "Orange", "Naranja", "Orange"),
    ("Red", "Rot", "Rojo", "Röd"),
    ("Crimson", "Karmesinrot", "Carmesí", "Karmosinröd"),
    ("Maroon", "Kastanienbraun", "Granate", "Vinröd"),
    ("Purple", "Lila", "Morado", "Lila"),
    ("Black", "Schwarz", "Negro", "Svart"),
    ("Silver", "Silber", "Plata", "Silver"),
    ("White", "Weiß", "Blanco", "Vit"),
    ("Brown", "Braun", "Marrón", "Brun"),
]
for _i, _names in enumerate(COLOURS):
    L("csm.venues.colour.%d" % _i, _names)
L("csm.venues.ribbon.pattern", ("Pattern: %s", "Muster: %s", "Patrón: %s", "Mönster: %s"))
for _i, _names in enumerate((("Navy and ice blue sweep", "Marine- und Eisblau-Welle",
                              "Barrido azul marino y hielo", "Svep i marinblått och isblått"),
                             ("Crimson and gold chevrons", "Karmesin- und Gold-Winkel",
                              "Galones carmesí y oro", "Vinklar i karmosinrött och guld"),
                             ("Green and white chase", "Grün-weißes Lauflicht",
                              "Persecución verde y blanca", "Grönt och vitt jagande ljus"),
                             ("White crawl", "Weißer Lauftext", "Rótulo blanco",
                              "Vit rullande text"))):
    L("csm.venues.ribbon.pattern.%d" % _i, _names)


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def java_files():
    return {os.path.join(JAVA_DIR, name): text for name, text in JAVA.items()}


def main():
    if "--check" in sys.argv:
        stale = []
        for path, text in java_files().items():
            if not os.path.exists(path):
                stale.append(path)
                continue
            with open(path, encoding="utf-8") as fh:
                if fh.read() != text:
                    stale.append(path)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        for path, text in java_files().items():
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(text)
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, f) for dp, _, fs in os.walk(C.assets) for f in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
