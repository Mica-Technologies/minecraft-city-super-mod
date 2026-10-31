#!/usr/bin/env python3
"""
gen_venues_arena.py -- every asset the Sports & Venues arena event floor ships.

    py -3 dev-env-utils/scripts/gen_venues_arena.py               # write
    py -3 dev-env-utils/scripts/gen_venues_arena.py --check       # fail on drift (the Java too)
    py -3 dev-env-utils/scripts/gen_venues_arena.py --fragments   # the tab's lines

The blocks (the Sports & Venues - Arena tab, CsmTabVenuesArena):

  * **Dasher boards and glass.** A board is one block high, white with a yellow kick plate and
    a grey cap rail, 4 px thick on the ice side of its cell; the glass is a block of its own
    stacked on it (two high along the sides, three at the ends). Straights (BlockDasherStraight)
    join edge to edge and draw an end post where the run stops (actual state); the 45 degree
    corner (BlockDasherCorner) cuts across a cell and mitres into the straights diagonally beside
    it; the curve (BlockDasherCurve) is a true quarter circle of radius 8, the rink's corner,
    split into the fifteen cells its ice face passes through, one block per rink corner with the
    cell as its metadata; the player bench gate and the penalty box door (BlockDasherDoor) open
    by a click. Corners and curves are OBJ, cut at the cell faces; `DasherShapes.java` carries
    their collision boxes, written from the same geometry.
  * **The ice and the court.** Full blocks (BlockArenaFloor): plain ice and three paints, rubber
    matting, ice lines (red centre, blue, goal), maple in four finishes and the court's boundary
    and centre lines, all set by metadata. The circles, the crease and the keys are pictures
    laid out around an anchor block (BlockArenaFloorAnchor: a face-off spot, the centre spot,
    the goal line's centre, a basket end or the court's centre): a painted block
    (BlockArenaFloorPainted) finds the nearest anchor and shows its piece of that anchor's
    picture, so a builder fills an area round an anchor and the marking appears. Every distinct
    cell picture is one 32 px texture; `ArenaFloorLayouts.java` holds which cell sits where.
  * the hockey goal, the portable basketball stanchion (OBJ, rim at 10 ft), the ice resurfacer
    and the ice plant: multi-block units drawn by their master (BlockVenueUnit) with invisible
    venue_unit_part cells; the player and penalty benches and the scorer's table, which join
    into runs (BlockArenaRun); the scorer's table LED front and the shot clock are animated
    textures, so nothing ticks.

Every model faces north: a board's ice side, a bench's sitter, a goal's mouth, a stanchion's
court. Brands are invented or absent; no league, team or sponsor appears.
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_arena.py"
C = vc.catalogue(GEN, "arena")
T = C.T
M = C.M
box = vc.box
ARENA = "Sports & Venues - Arena"
HIDDEN = vc.HIDDEN
FACINGS = vc.FACINGS
ROT_Y = vc.ROT_Y
JAVA_DIR = os.path.join(vc.REPO, "modules", "venues", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "arena")
JAVA = {}           # file name -> text, written beside the assets and held by --check
DOC = {}            # tables for VENUES_SYSTEM.md, printed by --doc

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
ICE = (234, 241, 247)
ICE_RED = (196, 32, 44)
ICE_BLUE = (26, 66, 168)
CREASE = (118, 176, 226)
BOARD_WHITE = (240, 240, 236)
KICK_YELLOW = (236, 194, 32)
CAP_GREY = (196, 200, 206)
RUBBER = (36, 36, 38)
MAPLE = (218, 174, 116)
NAVY = (26, 40, 86)
CRIMSON = (148, 30, 44)
APRON_GREY = (108, 110, 114)
LINE_WHITE = (246, 246, 240)
GOAL_RED = (204, 30, 34)
PAD_NAVY = (30, 44, 92)
PAD_BLACK = (30, 30, 32)
STEEL = (120, 124, 130)
DARK = (44, 46, 50)
RIM_ORANGE = (226, 96, 28)
LED_RED = (255, 52, 40)


# ------------------------------------------------------------------------------------------
# Plain textures
# ------------------------------------------------------------------------------------------
@C.texture("board")
def _board():
    img = vc.swatch(BOARD_WHITE, 31, grain=2)
    vc.rect(img, 0, 13, 16, 16, KICK_YELLOW)
    vc.rect(img, 0, 12, 16, 13, vc.shade(BOARD_WHITE, 0.86))
    return img


C.textures["board_back"] = lambda: vc.swatch(vc.shade(BOARD_WHITE, 0.92), 32, grain=2)
C.textures["cap"] = lambda: vc.swatch(CAP_GREY, 33, grain=2)
C.textures["post"] = lambda: vc.brushed(vc.ALUMINIUM, 34)
C.textures["latch"] = lambda: vc.swatch(DARK, 35, grain=2)
C.textures["rubber"] = lambda: vc.swatch(RUBBER, 36, grain=5)
C.textures["steel"] = lambda: vc.brushed(STEEL, 37)
C.textures["dark"] = lambda: vc.swatch(DARK, 38, grain=3)
C.textures["goal_red"] = lambda: vc.swatch(GOAL_RED, 39, grain=3)
C.textures["goal_white"] = lambda: vc.swatch((236, 236, 232), 40, grain=2)
C.textures["pad_navy"] = lambda: vc.swatch(PAD_NAVY, 41, grain=3)
C.textures["pad_black"] = lambda: vc.swatch(PAD_BLACK, 42, grain=3)
C.textures["rim"] = lambda: vc.swatch(RIM_ORANGE, 43, grain=3)
C.textures["tire"] = lambda: vc.swatch((24, 24, 26), 44, grain=3)


def _glass(alpha, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (206, 226, 230, alpha + rng.randint(-3, 3))
    for i in range(16):
        x, y = (i + 4) % 16, 15 - i
        px[x, y] = (236, 246, 248, alpha + 34)
    return img


C.textures["glass"] = lambda: _glass(30, 45)
C.textures["glass_item"] = lambda: _glass(150, 45)


# ------------------------------------------------------------------------------------------
# The ice and the court: base drawings
# ------------------------------------------------------------------------------------------
RES = 32


def ice_base(seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (RES, RES))
    px = img.load()
    for y in range(RES):
        for x in range(RES):
            d = rng.uniform(-2.5, 2.5)
            px[x, y] = vc.clamp((ICE[0] + d, ICE[1] + d, ICE[2] + d * 0.6)) + (255,)
    return img


def maple_base(seed, paint=None):
    """Maple strip flooring: 2 px boards along the texture's height (the court's length), each
    a slightly different tone, butt joints staggered, a little grain. ``paint`` covers it with
    a colour through which the board seams still show."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (RES, RES))
    px = img.load()
    for col in range(RES // 2):
        tone = rng.uniform(0.9, 1.08)
        joint = rng.randint(0, RES - 1)
        for y in range(RES):
            for dx in range(2):
                x = col * 2 + dx
                k = tone * (1 + rng.uniform(-0.025, 0.025))
                if dx == 1:
                    k *= 0.93
                if y == joint:
                    k *= 0.8
                px[x, y] = vc.shade(MAPLE, k) + (255,)
    if paint is not None:
        img = paint_over_img(img, paint)
    return img


def paint_over_arr(base, colour, keep=0.35):
    lum = base[..., :3].mean(axis=2, keepdims=True)
    mean = np.array(MAPLE, dtype=float).mean()
    k = 1 + (lum - mean) / mean * keep
    return np.clip(np.array(colour, dtype=float)[None, None, :] * k, 0, 255)


def paint_over_img(img, colour):
    arr = np.asarray(img, dtype=float)
    out = arr.copy()
    out[..., :3] = paint_over_arr(arr, colour)
    return Image.fromarray(out.astype(np.uint8), "RGBA")


for _i in range(3):
    C.textures["ice_%d" % _i] = (lambda s: (lambda: ice_base(s)))(50 + _i)
    C.textures["maple_%d" % _i] = (lambda s: (lambda: maple_base(s)))(60 + _i)
C.textures["maple_navy"] = lambda: maple_base(60, NAVY)
C.textures["maple_crimson"] = lambda: maple_base(60, CRIMSON)
C.textures["maple_grey"] = lambda: maple_base(60, APRON_GREY)


def ice_paint(colour, seed):
    img = ice_base(seed)
    arr = np.asarray(img, dtype=float)
    out = arr.copy()
    out[..., :3] = np.clip(arr[..., :3] / np.array(ICE)[None, None, :]
                           * np.array(colour)[None, None, :] * 0.96 + 6, 0, 255)
    return Image.fromarray(out.astype(np.uint8), "RGBA")


C.textures["ice_red"] = lambda: ice_paint(ICE_RED, 50)
C.textures["ice_blue"] = lambda: ice_paint(ICE_BLUE, 50)
C.textures["ice_crease"] = lambda: ice_paint(CREASE, 50)

BASES = {"ice": lambda: np.asarray(ice_base(50), dtype=float),
         "court": lambda: np.asarray(maple_base(60), dtype=float)}
BASE_TEX = {"ice": "ice_0", "court": "maple_0"}

# ------------------------------------------------------------------------------------------
# Painted pictures: shapes on a (u, v) plane in blocks, u to the right and v forward
# ------------------------------------------------------------------------------------------
SS = 4      # samples a texel, each way


class Shape(object):
    def __init__(self, bbox, fn):
        self.bbox, self.fn = bbox, fn

    def hits(self, u0, u1, v0, v1):
        b = self.bbox
        return not (b[1] < u0 or b[0] > u1 or b[3] < v0 or b[2] > v1)

    def __and__(self, other_fn):
        return Shape(self.bbox, (lambda U, V, f=self.fn: f(U, V) & other_fn(U, V)))


def seg(u0, v0, u1, v1, w):
    du, dv = u1 - u0, v1 - v0
    ll = du * du + dv * dv

    def fn(U, V):
        t = np.clip(((U - u0) * du + (V - v0) * dv) / ll, 0, 1)
        return (U - u0 - t * du) ** 2 + (V - v0 - t * dv) ** 2 <= (w / 2.0) ** 2
    h = w / 2.0
    return Shape((min(u0, u1) - h, max(u0, u1) + h, min(v0, v1) - h, max(v0, v1) + h), fn)


def rect(u0, v0, u1, v1):
    return Shape((u0, u1, v0, v1), lambda U, V: (U >= u0) & (U <= u1) & (V >= v0) & (V <= v1))


def ring(cu, cv, r, w, keep=None):
    def fn(U, V):
        m = np.abs(np.hypot(U - cu, V - cv) - r) <= w / 2.0
        return m & keep(U, V) if keep is not None else m
    e = r + w
    return Shape((cu - e, cu + e, cv - e, cv + e), fn)


def disc(cu, cv, r):
    return Shape((cu - r, cu + r, cv - r, cv + r),
                 lambda U, V: (U - cu) ** 2 + (V - cv) ** 2 <= r * r)


def region(bbox, fn):
    return Shape(bbox, fn)


class Layout(object):
    """One anchor kind's picture: cells i (to the right) and j (forward) from the anchor, cell i
    spanning u = uo + i .. uo + i + 1 and cell j v = vo + j .. vo + j + 1."""

    def __init__(self, family, kind, name, uo, vo, irange, jrange, layers):
        self.family, self.kind, self.name = family, kind, name
        self.uo, self.vo = uo, vo
        self.irange, self.jrange = irange, jrange
        self.layers = layers        # (colour or callable(base) -> rgb array, Shape)

    def cell(self, base, i, j):
        u0, v0 = self.uo + i, self.vo + j
        n = RES * SS
        t = (np.arange(n) + 0.5) / (SS * RES)
        U = np.broadcast_to(u0 + t[None, :], (n, n))
        V = np.broadcast_to(v0 + 1 - t[:, None], (n, n))
        out = base[..., :3].copy()
        touched = False
        for colour, shape in self.layers:
            if not shape.hits(u0, u0 + 1, v0, v0 + 1):
                continue
            mask = shape.fn(U, V)
            cov = mask.reshape(RES, SS, RES, SS).mean(axis=(1, 3))
            if not cov.any():
                continue
            touched = True
            col = colour(base) if callable(colour) else np.array(colour, dtype=float)[None, None, :]
            out = out * (1 - cov[..., None]) + col * cov[..., None]
        if not touched:
            return None
        rgba = np.concatenate([np.clip(np.rint(out), 0, 255), base[..., 3:4]], axis=2)
        return rgba.astype(np.uint8)


W2 = 0.06       # a 2 in line, drawn a little wider so it reads at 32 px a block
W_COURT = 0.06


def ice_layouts():
    out = []
    # 0: end zone face-off: the 15 ft circle, its hash marks, the L marks round the spot
    layers = [(ICE_RED, ring(0, 0, 4.57, W2))]
    uh = math.sqrt(4.57 ** 2 - 0.85 ** 2)
    for su in (-1, 1):
        for sv in (-1, 1):
            layers.append((ICE_RED, seg(su * uh, sv * 0.85, su * (uh + 0.61), sv * 0.85, W2)))
            layers.append((ICE_RED, seg(su * 0.61, sv * 0.46, su * 1.83, sv * 0.46, W2)))
            layers.append((ICE_RED, seg(su * 0.61, sv * 0.46, su * 0.61, sv * 1.37, W2)))
    layers.append((ICE_RED, disc(0, 0, 0.305)))
    out.append(Layout("ice", 0, "end zone face-off", -0.5, -0.5, (-6, 6), (-6, 6), layers))
    # 1: neutral zone face-off spot
    out.append(Layout("ice", 1, "neutral zone spot", -0.5, -0.5, (0, 0), (0, 0),
                      [(ICE_RED, disc(0, 0, 0.305))]))
    # 2: centre ice: the red line through a blue 15 ft circle and the blue spot, centred on the
    # anchor's left edge (a 26 wide rink's middle is a block line)
    out.append(Layout("ice", 2, "centre ice", 0.0, -0.5, (-5, 4), (-5, 5),
                      [(ICE_RED, rect(-5.0, -0.15, 5.0, 0.15)),
                       (ICE_BLUE, ring(0, 0, 4.57, W2)),
                       (ICE_BLUE, disc(0, 0, 0.15))]))
    # 3: the goal line's centre: goal line, crease and the trapezoid behind the goal, centred
    # on the anchor's left edge
    yc = math.sqrt(1.83 ** 2 - 1.22 ** 2)

    def in_crease(U, V):
        return (np.abs(U) <= 1.22) & (V >= 0.03) & (U * U + V * V <= 1.83 ** 2)
    layers = [(ICE_RED, rect(-5.0, -0.03, 5.0, 0.03)),
              (CREASE, region((-1.3, 1.3, 0, 1.9), in_crease))]
    for su in (-1, 1):
        layers.append((ICE_RED, seg(su * 1.22, 0, su * 1.22, yc, W2)))
        layers.append((ICE_RED, seg(su * 3.35, 0, su * 4.27, -3.5, W2)))
    layers.append((ICE_RED, ring(0, 0, 1.83, W2, keep=lambda U, V: (np.abs(U) <= 1.22) & (V > 0))))
    out.append(Layout("ice", 3, "goal crease", 0.0, -0.5, (-5, 4), (-3, 2), layers))
    return out


def court_layouts():
    out = []
    rim_v = 1.575
    three = 6.71
    three_v = rim_v + math.sqrt(7.24 ** 2 - three ** 2)
    w = W_COURT

    def lines(paint):
        def in_key(U, V):
            return (np.abs(U) <= 2.44) & (V >= 0) & (V <= 5.79)
        shapes = [rect(-7.5, 0, 7.5, w), rect(-7.5, 0, -7.5 + w, 9.0), rect(7.5 - w, 0, 7.5, 9.0),
                  rect(-2.44, 0, -2.44 + w, 5.79), rect(2.44 - w, 0, 2.44, 5.79),
                  rect(-2.44, 5.79 - w, 2.44, 5.79),
                  ring(0, 5.79, 1.83 - w / 2, w, keep=lambda U, V: V >= 5.79),
                  ring(0, 5.79, 1.83 - w / 2, w,
                       keep=lambda U, V: (V < 5.79) & (np.floor(np.degrees(np.arctan2(V - 5.79, U)) / 15.0) % 2 == 0)),
                  ring(0, rim_v, 1.22, w, keep=lambda U, V: V >= rim_v),
                  rect(-1.22 - w / 2, 1.22, -1.22 + w / 2, rim_v),
                  rect(1.22 - w / 2, 1.22, 1.22 + w / 2, rim_v),
                  rect(-three - w / 2, 0, -three + w / 2, three_v),
                  rect(three - w / 2, 0, three + w / 2, three_v),
                  ring(0, rim_v, 7.24, w, keep=lambda U, V: (np.abs(U) <= three) & (V >= rim_v))]
        for vm in (2.13, 3.05, 3.96, 4.88):
            shapes.append(rect(2.44, vm, 2.64, vm + w))
            shapes.append(rect(-2.64, vm, -2.44, vm + w))
        shapes.append(rect(2.44, 2.13, 2.64, 2.43))
        shapes.append(rect(-2.64, 2.13, -2.44, 2.43))
        layers = []
        if paint is not None:
            layers.append(((lambda base, p=paint: paint_over_arr(base, p)),
                           region((-2.44, 2.44, 0, 5.79), in_key)))
            for s in shapes:
                layers.append((NAVY, s & (lambda U, V: ~in_key(U, V))))
                layers.append((LINE_WHITE, s & in_key))
        else:
            layers = [(NAVY, s) for s in shapes]
        return layers
    for kind, paint, name in ((0, None, "basket end, natural key"),
                              (1, NAVY, "basket end, navy key"),
                              (2, CRIMSON, "basket end, crimson key")):
        out.append(Layout("court", kind, name, -0.5, 0.0, (-7, 7), (0, 8), lines(paint)))
    out.append(Layout("court", 3, "centre court", -0.5, -0.5, (-7, 7), (-2, 2),
                      [(NAVY, rect(-7.5, -w / 2, 7.5, w / 2)),
                       (NAVY, ring(0, 0, 1.83 - w / 2, w)),
                       (NAVY, ring(0, 0, 0.61 - w / 2, w)),
                       (NAVY, rect(-7.5, -2.5, -7.5 + w, 2.5)),
                       (NAVY, rect(7.5 - w, -2.5, 7.5, 2.5))]))
    return out


LAYOUTS = {"ice": ice_layouts(), "court": court_layouts()}
CELLS = {}          # family -> [None (plain)] + [rgba arrays]
GRIDS = {}          # family -> {kind: {(i, j): cell}}


def build_cells():
    for family, layouts in LAYOUTS.items():
        base = BASES[family]()
        cells = [None]
        seen = {}
        grids = {}
        for lay in layouts:
            g = {}
            for j in range(lay.jrange[0], lay.jrange[1] + 1):
                for i in range(lay.irange[0], lay.irange[1] + 1):
                    pic = lay.cell(base, i, j)
                    if pic is None:
                        if i == 0 and j == 0:
                            g[(0, 0)] = 0
                        continue
                    key = pic.tobytes()
                    if key not in seen:
                        seen[key] = len(cells)
                        cells.append(pic)
                    g[(i, j)] = seen[key]
            grids[lay.kind] = g
        CELLS[family] = cells
        GRIDS[family] = grids


build_cells()
CELL_TEX = {"ice": "ice_cell_%03d", "court": "court_cell_%03d"}
for _fam, _cells in CELLS.items():
    for _k, _pic in enumerate(_cells):
        if _pic is None:
            continue
        C.textures[CELL_TEX[_fam] % _k] = (lambda p: (lambda: Image.fromarray(p, "RGBA")))(_pic)


def cell_texture(family, k):
    return T(BASE_TEX[family]) if k == 0 else T(CELL_TEX[family] % k)


def floor_model(top, side):
    return {"parent": "block/cube",
            "textures": {"particle": side, "down": side, "up": top, "north": side,
                         "south": side, "east": side, "west": side}}


# ------------------------------------------------------------------------------------------
# Line textures (a line along the texture's height, i.e. along z unturned)
# ------------------------------------------------------------------------------------------
def ice_line(colour, x0, x1):
    img = ice_base(50)
    arr = np.asarray(img, dtype=float).copy()
    arr[:, x0:x1, :3] = arr[:, x0:x1, :3] * 0.05 + np.array(colour)[None, None, :] * 0.95
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


C.textures["ice_line_red"] = lambda: ice_line(ICE_RED, 11, 21)
C.textures["ice_line_blue"] = lambda: ice_line(ICE_BLUE, 11, 21)
C.textures["ice_line_goal"] = lambda: ice_line(ICE_RED, 15, 17)


def court_line(edges):
    """Navy 2 in lines on the natural maple: 'n' along the top (north) edge, 'w' down the left
    edge, 'c' across the middle."""
    base = np.asarray(maple_base(60), dtype=float).copy()
    cov = np.zeros((RES, RES))
    lw = W_COURT * RES      # 1.6 px
    for e in edges:
        for k in range(RES):
            a = min(1.0, max(0.0, lw - k))
            if e == "n":
                cov[k, :] = np.maximum(cov[k, :], a)
            elif e == "w":
                cov[:, k] = np.maximum(cov[:, k], a)
        if e == "c":
            for k in range(RES):
                d = abs(k + 0.5 - RES / 2.0)
                a = min(1.0, max(0.0, lw / 2.0 + 0.5 - d))
                cov[k, :] = np.maximum(cov[k, :], a)
    base[..., :3] = base[..., :3] * (1 - cov[..., None]) + np.array(NAVY)[None, None, :] * cov[..., None]
    return Image.fromarray(np.clip(np.rint(base), 0, 255).astype(np.uint8), "RGBA")


C.textures["court_line_edge"] = lambda: court_line("n")
C.textures["court_line_corner"] = lambda: court_line("nw")
C.textures["court_line_centre"] = lambda: court_line("c")

# ------------------------------------------------------------------------------------------
# Floor blocks
# ------------------------------------------------------------------------------------------
SIDE_ICE = T("ice_0")
SIDE_MAPLE = T("maple_0")
FLOOR_MODELS = {}


def fmodel(name, top, side):
    FLOOR_MODELS[name] = floor_model(top, side)
    return M(name)


def turns(model_name, ys):
    return [{"model": model_name, "y": y} if y else {"model": model_name} for y in ys]


def facing_y(meta):
    return {0: 180, 1: 270, 2: 0, 3: 90}[meta & 3]


def floor_state(variants, inventory):
    if len(variants) == 1:
        v = {"normal": variants[0]}
    else:
        v = {"variant=%d" % k: body for k, body in variants.items()}
    v["inventory"] = inventory
    return {"variants": v}


# rink_ice: white (three drawings, any turn), red, blue and crease blue paint
ice_models = [fmodel("ice_%d" % i, T("ice_%d" % i), SIDE_ICE) for i in range(3)]
rink_ice = {0: [b for m in ice_models for b in turns(m, (0, 90, 180, 270))],
            1: turns(fmodel("ice_red", T("ice_red"), SIDE_ICE), (0, 90, 180, 270)),
            2: turns(fmodel("ice_blue", T("ice_blue"), SIDE_ICE), (0, 90, 180, 270)),
            3: turns(fmodel("ice_crease", T("ice_crease"), SIDE_ICE), (0, 90, 180, 270))}
C.add("rink_ice", 'new BlockArenaFloor("rink_ice", 4, BlockArenaFloor.Orient.NONE, BlockArenaFloor.Surface.ICE)',
      ("Rink Ice", "Eisfläche", "Hielo de Pista", "Rinkis"),
      dict(FLOOR_MODELS), floor_state(rink_ice, {"model": ice_models[0]}), tab=ARENA)
FLOOR_MODELS.clear()

rubber = fmodel("rubber", T("rubber"), T("rubber"))
C.add("rink_rubber_floor",
      'new BlockArenaFloor("rink_rubber_floor", 1, BlockArenaFloor.Orient.NONE, BlockArenaFloor.Surface.RUBBER)',
      ("Rubber Rink Matting", "Gummi-Bodenbelag", "Suelo de Goma", "Gummimatta"),
      dict(FLOOR_MODELS), floor_state({0: turns(rubber, (0, 90, 180, 270))}, {"model": rubber}),
      tab=ARENA)
FLOOR_MODELS.clear()

# rink_ice_line: metadata kind * 2 + axis; axis 0 runs along x (turned), 1 along z
line_models = [fmodel("ice_line_red", T("ice_line_red"), SIDE_ICE),
               fmodel("ice_line_blue", T("ice_line_blue"), SIDE_ICE),
               fmodel("ice_line_goal", T("ice_line_goal"), SIDE_ICE)]
rink_lines = {}
for kind, m in enumerate(line_models):
    rink_lines[kind * 2] = turns(m, (90, 270))
    rink_lines[kind * 2 + 1] = turns(m, (0, 180))
C.add("rink_ice_line", 'new BlockArenaFloor("rink_ice_line", 3, BlockArenaFloor.Orient.AXIS, BlockArenaFloor.Surface.ICE)',
      ("Rink Ice Line", "Eisflächenlinie", "Línea de Pista de Hielo", "Rinklinje"),
      dict(FLOOR_MODELS), floor_state(rink_lines, {"model": line_models[0], "y": 90}), tab=ARENA)
FLOOR_MODELS.clear()

# court_maple: metadata finish * 2 + axis; finishes natural, navy, crimson, grey
maple_models = [fmodel("maple_%d" % i, T("maple_%d" % i), SIDE_MAPLE) for i in range(3)]
paints = [fmodel("maple_" + n, T("maple_" + n), SIDE_MAPLE) for n in ("navy", "crimson", "grey")]
court = {}
for finish in range(4):
    ms = maple_models if finish == 0 else [paints[finish - 1]]
    court[finish * 2] = [b for m in ms for b in turns(m, (90, 270))]
    court[finish * 2 + 1] = [b for m in ms for b in turns(m, (0, 180))]
C.add("court_maple", 'new BlockArenaFloor("court_maple", 4, BlockArenaFloor.Orient.AXIS, BlockArenaFloor.Surface.WOOD)',
      ("Maple Court Floor", "Ahorn-Hallenboden", "Suelo de Pista de Arce", "Lönngolv för Plan"),
      dict(FLOOR_MODELS), floor_state(court, {"model": maple_models[0]}), tab=ARENA)
FLOOR_MODELS.clear()

# court_line: metadata facing | kind << 2; kinds edge, corner, centre
cl_models = [fmodel("court_line_edge", T("court_line_edge"), SIDE_MAPLE),
             fmodel("court_line_corner", T("court_line_corner"), SIDE_MAPLE),
             fmodel("court_line_centre", T("court_line_centre"), SIDE_MAPLE)]
cl = {}
for kind in range(3):
    for f in range(4):
        y = facing_y(f)
        cl[kind * 4 + f] = [{"model": cl_models[kind], "y": y} if y else {"model": cl_models[kind]}]
C.add("court_line", 'new BlockArenaFloor("court_line", 3, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.WOOD)',
      ("Court Line", "Spielfeldlinie", "Línea de Pista", "Planlinje"),
      dict(FLOOR_MODELS), floor_state(cl, {"model": cl_models[0]}), tab=ARENA)
FLOOR_MODELS.clear()


def cell_models(family):
    models = {}
    for k in range(len(CELLS[family])):
        models["%s_cell_%03d" % (family, k)] = floor_model(
            cell_texture(family, k), SIDE_ICE if family == "ice" else SIDE_MAPLE)
    return models


ANCHOR = {"ice": "rink_ice_spot", "court": "court_floor_anchor"}
PAINTED = {"ice": "rink_ice_painted", "court": "court_painted"}


def anchor_state(family):
    variants = {}
    grids = GRIDS[family]
    for kind in range(4):
        cell = grids[kind].get((0, 0), 0)
        for name, y in FACINGS:
            body = {"model": M("%s_cell_%03d" % (family, cell))}
            if y:
                body["y"] = y
            variants["facing=%s,kind=%d" % (name, kind)] = [body]
    variants["inventory"] = [{"model": M("%s_cell_%03d" % (family, grids[0].get((0, 0), 0)))}]
    return {"forge_marker": 1, "variants": variants}


def painted_state(family, icon_cell):
    n = len(CELLS[family])
    return {"forge_marker": 1,
            "defaults": {"model": M("%s_cell_000" % family)},
            "variants": {"cell": {str(k): {"model": M("%s_cell_%03d" % (family, k))}
                                  for k in range(n)},
                         "rot": {"0": {}, "1": {"y": 90}, "2": {"y": 180}, "3": {"y": 270}},
                         "inventory": [{"model": M("%s_cell_%03d" % (family, icon_cell))}]}}


def icon_cell(family, kind, ij):
    return GRIDS[family][kind].get(ij, 0)


C.add("rink_ice_spot", 'new BlockArenaFloorAnchor("rink_ice_spot", ArenaFloorLayouts.ICE)',
      ("Rink Ice Marking Anchor", "Eisflächen-Markierungsanker", "Ancla de Marca de Pista de Hielo",
       "Ankare för Rinkmarkering"),
      cell_models("ice"), anchor_state("ice"), tab=ARENA)
C.add("rink_ice_painted", 'new BlockArenaFloorPainted("rink_ice_painted", ArenaFloorLayouts.ICE)',
      ("Rink Ice, Painted Marking", "Eisfläche, gemalte Markierung",
       "Hielo de Pista, Marca Pintada", "Rinkis, Målad Markering"),
      {}, painted_state("ice", icon_cell("ice", 0, (4, 1))), tab=ARENA)
C.add("court_floor_anchor",
      'new BlockArenaFloorAnchor("court_floor_anchor", ArenaFloorLayouts.COURT)',
      ("Court Marking Anchor", "Spielfeld-Markierungsanker", "Ancla de Marca de Pista",
       "Ankare för Planmarkering"),
      cell_models("court"), anchor_state("court"), tab=ARENA)
C.add("court_painted", 'new BlockArenaFloorPainted("court_painted", ArenaFloorLayouts.COURT)',
      ("Maple Court, Painted Marking", "Ahorn-Hallenboden, gemalte Markierung",
       "Pista de Arce, Marca Pintada", "Lönngolv, Målad Markering"),
      {}, painted_state("court", icon_cell("court", 1, (1, 4))), tab=ARENA)


def layouts_java():
    out = []
    out.append("package com.micatechnologies.minecraft.csm.venues.arena;\n")
    out.append("/**")
    out.append(" * Where every painted floor cell sits round its anchor: generated by")
    out.append(" * {@code dev-env-utils/scripts/gen_venues_arena.py} from the pictures it draws, so the")
    out.append(" * models and this table cannot disagree. Do not edit.")
    out.append(" *")
    out.append(" * <p>For each anchor kind (an anchor's metadata bits 2-3), triplets of {@code i} (cells")
    out.append(" * to the anchor's right), {@code j} (cells forward, the way the anchor faces) and the")
    out.append(" * cell picture there, which is the painted block's {@code cell} property. Cell 0 is plain")
    out.append(" * and is not listed.</p>")
    out.append(" *")
    out.append(" * @since 2026.10")
    out.append(" */")
    out.append("public final class ArenaFloorLayouts {\n")
    for family, const in (("ice", "ICE"), ("court", "COURT")):
        grids = GRIDS[family]
        radius = 0
        for kind in range(4):
            for (i, j) in grids[kind]:
                radius = max(radius, abs(i), abs(j))
        out.append("  /** The %s family: %s. */" % (family, ", ".join(
            "%d %s" % (lay.kind, lay.name) for lay in LAYOUTS[family])))
        out.append("  public static final ArenaFloorFamily %s = new ArenaFloorFamily(\"%s\", %d, %d,"
                   % (const, family, len(CELLS[family]), radius))
        rows = []
        for kind in range(4):
            trip = []
            for (i, j), cell in sorted(grids[kind].items(), key=lambda kv: (kv[0][1], kv[0][0])):
                if cell:
                    trip += [i, j, cell]
            body = ", ".join(str(x) for x in trip)
            rows.append("      new int[]{%s}" % body)
        out.append(",\n".join(rows) + ");\n")
    out.append("  private ArenaFloorLayouts() {")
    out.append("  }")
    out.append("}")
    text = "\n".join(out) + "\n"
    # wrap long lines at 100 columns on the commas
    wrapped = []
    for line in text.split("\n"):
        while len(line) > 100:
            cut = line.rfind(", ", 0, 99)
            wrapped.append(line[:cut + 1])
            line = "          " + line[cut + 2:]
        wrapped.append(line)
    return "\n".join(wrapped)


JAVA["ArenaFloorLayouts.java"] = layouts_java()
DOC["cells"] = {f: len(c) for f, c in CELLS.items()}
DOC["grids"] = {f: {k: sorted(g.keys()) for k, g in GRIDS[f].items()} for f in GRIDS}

# ------------------------------------------------------------------------------------------
# Dasher boards and glass
# ------------------------------------------------------------------------------------------
BOARD_T = 4.0           # the board's thickness, from its ice face
GLASS_IN, GLASS_OUT = 1.75, 2.5
CAP_OVER = 0.25
DTEX = {"board": T("board"), "back": T("board_back"), "cap": T("cap"), "post": T("post"),
        "glass": T("glass"), "latch": T("latch"), "particle": T("board")}


def straight_board():
    return [box([0, 0, 0], [16, 15, 4], "board", faces=("north", "south", "east", "west", "down"),
                per={"south": "back", "east": "back", "west": "back"}),
            box([0, 15, -CAP_OVER], [16, 16, 4 + CAP_OVER], "cap",
                faces=("north", "south", "up", "down", "east", "west"))]


def board_end(left):
    el = box([0, 0, -0.5], [1.5, 16.5, 4.5], "post")
    return [el] if left else [vc.mirror_x(el)]


def straight_glass():
    return [box([0, 0, GLASS_IN], [16, 16, GLASS_OUT], "glass", faces=("north", "south", "up"))]


def glass_end(left):
    el = box([0, 0, 1.25], [1.0, 16, 3.0], "post")
    return [el] if left else [vc.mirror_x(el)]


def dm(els, glass_item=False):
    tex = dict(DTEX)
    if glass_item:
        tex["glass"] = T("glass_item")
    # no ambient occlusion: the floor darkens a board's face from its foot otherwise, and the
    # boards read grey instead of white
    return vc.model(tex, els, ao=False)


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            a = {"model": M(name)}
            if y:
                a["y"] = y
            return a
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


C.add("dasher_board", 'new BlockDasherStraight("dasher_board", false)',
      ("Dasher Board", "Bande", "Valla de Pista", "Sarg"),
      {"board_straight": dm(straight_board()), "board_straight_end_l": dm(board_end(True)),
       "board_straight_end_r": dm(board_end(False)),
       "board_item": dm(straight_board() + board_end(True) + board_end(False))},
      run_state("board_straight"), item={"parent": "csm:block/venues/arena/board_item"}, tab=ARENA)
C.add("dasher_glass", 'new BlockDasherStraight("dasher_glass", true)',
      ("Rink Glass", "Bandenverglasung", "Vidrio de Pista", "Sargglas"),
      {"glass_straight": dm(straight_glass()), "glass_straight_end_l": dm(glass_end(True)),
       "glass_straight_end_r": dm(glass_end(False)),
       "glass_item": dm(straight_glass() + glass_end(True) + glass_end(False), glass_item=True)},
      run_state("glass_straight"), item={"parent": "csm:block/venues/arena/glass_item"}, tab=ARENA)

# --- the 45 degree corner, drawn as at a rink's north-west corner (ice to the south-east) ---
K = math.sqrt(2) - 1


def chamfer_outline(o1, o2):
    """The plan outline of a band o1..o2 px out from the ice face of a chamfer whose ice face
    runs (0, 16) to (16, 0), mitred into the straights diagonally beside it (the west run along
    x = 0 below z = 16, the north run along z = 0 east of x = 16)."""
    pts = [(-o2, 16.0), (-o1, 16.0), (-o1, 16.0 - o1 * K), (16.0 - o1 * K, -o1), (16.0, -o1),
           (16.0, -o2), (16.0 - o2 * K, -o2), (-o2, 16.0 - o2 * K)]
    dedup = []
    for p in pts:
        if not dedup or (abs(dedup[-1][0] - p[0]) > 1e-6 or abs(dedup[-1][1] - p[1]) > 1e-6):
            dedup.append(p)
    return dedup


def prism_band(inner, outer, y0, y1, mat_in, mat_out, mat_top, top=True, ends=None):
    """A band between two plan polylines (same length, inner on the ice side), from y0 to y1:
    its inner and outer faces and its top, one quad a step. ``ends`` adds end faces."""
    polys = []
    for k in range(len(inner) - 1):
        a, b = inner[k], inner[k + 1]
        c, d = outer[k], outer[k + 1]
        n_in = (a[1] - b[1], b[0] - a[0])       # left of a->b
        mid_io = ((a[0] + b[0] - c[0] - d[0]) / 2.0, (a[1] + b[1] - c[1] - d[1]) / 2.0)
        if n_in[0] * mid_io[0] + n_in[1] * mid_io[1] < 0:
            n_in = (-n_in[0], -n_in[1])
        if mat_in:
            polys.append(vc.ovquad(a, b, y0, y1, mat_in, n_in))
        if mat_out:
            polys.append(vc.ovquad(c, d, y0, y1, mat_out, (-n_in[0], -n_in[1])))
        if top:
            polys.append(vc.ohpoly([a, b, d, c], y1, mat_top))
    for e in (ends or ()):
        k = 0 if e == "start" else len(inner) - 1
        a, c = inner[k], outer[k]
        nxt = inner[1] if k == 0 else inner[-2]
        out_dir = (a[0] - nxt[0], a[1] - nxt[1])
        polys.append(vc.ovquad(a, c, y0, y1, mat_out, out_dir))
    return polys


def poly_prism(outline, y0, y1, mat_side, mat_top, sides):
    """A prism over a convex plan outline; ``sides`` maps an edge index to its material (None
    leaves the edge open, where the next piece carries on)."""
    polys = [vc.ohpoly(outline, y1, mat_top)]
    n = len(outline)
    cx = sum(p[0] for p in outline) / n
    cz = sum(p[1] for p in outline) / n
    for k in range(n):
        a, b = outline[k], outline[(k + 1) % n]
        mat = sides.get(k, mat_side)
        if mat is None:
            continue
        mid = ((a[0] + b[0]) / 2.0 - cx, (a[1] + b[1]) / 2.0 - cz)
        polys.append(vc.ovquad(a, b, y0, y1, mat, mid))
    return polys


def edge_materials(outline, o1, o2, mat_in, mat_out):
    """Which edges of a chamfer outline face the ice (mat_in), face out (mat_out) or are the
    joints with the straights (None)."""
    sides = {}
    n = len(outline)
    for k in range(n):
        a, b = outline[k], outline[(k + 1) % n]
        if abs(a[1] - 16) < 1e-6 and abs(b[1] - 16) < 1e-6:
            sides[k] = None                      # the joint with the west run
        elif abs(a[0] - 16) < 1e-6 and abs(b[0] - 16) < 1e-6:
            sides[k] = None                      # the joint with the north run
        else:
            # inner edges lie on x = -o1, the ice line or z = -o1; the rest face out
            s = (a[0] + a[1] + b[0] + b[1]) / 2.0
            on_in = (abs(a[0] + o1) < 1e-6 and abs(b[0] + o1) < 1e-6) or \
                    (abs(a[1] + o1) < 1e-6 and abs(b[1] + o1) < 1e-6) or \
                    abs(s - (16 - o1 * math.sqrt(2))) < 1e-4
            sides[k] = mat_in if on_in else mat_out
    return sides


def corner_board_polys():
    out = chamfer_outline(0.0, BOARD_T)
    polys = poly_prism(out, 0, 15, "back", "cap", edge_materials(out, 0.0, BOARD_T, "board", "back"))
    cap = chamfer_outline(-CAP_OVER, BOARD_T + CAP_OVER)
    polys += poly_prism(cap, 15, 16, "cap", "cap", {k: (None if v is None else "cap") for k, v in
                                                    edge_materials(cap, -CAP_OVER,
                                                                   BOARD_T + CAP_OVER, "cap",
                                                                   "cap").items()})
    return polys


def corner_glass_polys():
    out = chamfer_outline(GLASS_IN, GLASS_OUT)
    return poly_prism(out, 0, 16, "glass", "glass",
                      edge_materials(out, GLASS_IN, GLASS_OUT, "glass", "glass"))


MTL = "dasher"
C.extra["models/block/venues/arena/%s.mtl" % MTL] = vc.mtl_text(GEN, {
    "board": T("board"), "back": T("board_back"), "cap": T("cap"), "glass": T("glass"),
    "post": T("post")})


def write_obj(name, polys, mtl=MTL):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/venues/arena/%s.obj" % name] = o.text()
    return M(name) + ".obj"


def turned_icon(glass):
    """The inventory icon of a corner or curve: a straight length turned 45 degrees."""
    els = straight_glass() if glass else straight_board()
    out = []
    for el in els:
        el = dict(el)
        el["from"] = [el["from"][0], el["from"][1], el["from"][2] + 6]
        el["to"] = [el["to"][0], el["to"][1], el["to"][2] + 6]
        el["rotation"] = {"origin": [8, 8, 8], "axis": "y", "angle": 45}
        out.append(el)
    return dm(out, glass_item=glass)


ICONS = {"corner_item_board": turned_icon(False), "corner_item_glass": turned_icon(True)}
# The corner's facing: the board faces between FACING and FACING turned clockwise, so the
# north-west chamfer as drawn (facing south-east) is facing east.
CORNER_Y = {"east": 0, "south": 90, "west": 180, "north": 270}


def corner_state(obj, icon):
    return {"forge_marker": 1, "defaults": {"model": obj, "custom": {"ambient": False}},
            "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in CORNER_Y.items()},
                         "inventory": [{"model": M(icon)}]}}


cb = write_obj("corner_board", corner_board_polys())
cg = write_obj("corner_glass", corner_glass_polys())
C.add("dasher_board_corner", 'new BlockDasherCorner("dasher_board_corner", false)',
      ("Dasher Board, 45° Corner", "Bande, 45°-Ecke", "Valla de Pista, Esquina de 45°",
       "Sarg, 45°-hörn"),
      dict(ICONS), corner_state(cb, "corner_item_board"), tab=ARENA)
C.add("dasher_glass_corner", 'new BlockDasherCorner("dasher_glass_corner", true)',
      ("Rink Glass, 45° Corner", "Bandenverglasung, 45°-Ecke", "Vidrio de Pista, Esquina de 45°",
       "Sargglas, 45°-hörn"),
      {}, corner_state(cg, "corner_item_glass"), tab=ARENA)

# --- the true curve: a quarter circle of radius 8, at a rink's north-west corner ------------
R_CURVE = 8.0           # blocks, from the centre (8, 8) blocks from the ice's corner


def curve_cells():
    """The cells the ice face passes through, in order from the west straight to the north one,
    with the angle range each owns (0 at the west tangent, 90 at the north)."""
    cells = []
    n = 36000
    for k in range(n + 1):
        th = math.pi / 2 * k / n
        x = R_CURVE - R_CURVE * math.cos(th)
        z = R_CURVE - R_CURVE * math.sin(th)
        c = (int(math.floor(x - 1e-9)) if k == n else int(math.floor(x)),
             int(math.floor(z - 1e-9)) if k == 0 else int(math.floor(z)))
        if c[0] < 0 or c[1] < 0:
            c = (max(c[0], 0), max(c[1], 0))
        if cells and cells[-1][0] == c:
            cells[-1][2] = th
        else:
            cells.append([c, th, th])
    cells = [c for c in cells if c[2] - c[1] > 1e-6]
    # snap each boundary to the exact crossing of a block line
    for k in range(len(cells) - 1):
        (x0, z0), (x1, z1) = cells[k][0], cells[k + 1][0]
        if x1 != x0:
            line = max(x0, x1)
            th = math.acos((R_CURVE - line) / R_CURVE)
        else:
            line = max(z0, z1)
            th = math.asin((R_CURVE - line) / R_CURVE)
        cells[k][2] = th
        cells[k + 1][1] = th
    cells[0][1] = 0.0
    cells[-1][2] = math.pi / 2
    return [(c[0], c[1], c[2]) for c in cells]


CURVE = curve_cells()
assert len(CURVE) == 15, CURVE


def arc(cell, th0, th1, r, step=math.radians(2.0)):
    n = max(1, int(math.ceil((th1 - th0) / step)))
    pts = []
    for k in range(n + 1):
        th = th0 + (th1 - th0) * k / n
        x = (R_CURVE - r * math.cos(th) - cell[0]) * 16.0
        z = (R_CURVE - r * math.sin(th) - cell[1]) * 16.0
        pts.append((x, z))
    return pts


def curve_board_polys(cell, th0, th1):
    r_in, r_out = R_CURVE, R_CURVE + BOARD_T / 16.0
    polys = prism_band(arc(cell, th0, th1, r_in), arc(cell, th0, th1, r_out), 0, 15,
                       "board", "back", "cap", top=False)
    c_in, c_out = r_in - CAP_OVER / 16.0, r_out + CAP_OVER / 16.0
    polys += prism_band(arc(cell, th0, th1, c_in), arc(cell, th0, th1, c_out), 15, 16,
                        "cap", "cap", "cap")
    return polys


def curve_glass_polys(cell, th0, th1):
    r_in, r_out = R_CURVE + GLASS_IN / 16.0, R_CURVE + GLASS_OUT / 16.0
    return prism_band(arc(cell, th0, th1, r_in), arc(cell, th0, th1, r_out), 0, 16,
                      "glass", "glass", "glass")


def boxes_along(points_in, points_out, parts=4):
    """Collision boxes along a band, ``parts`` of them, each the plan bounds of its stretch,
    clipped to the cell; in pixels, (x0, z0, x1, z1)."""
    n = len(points_in)
    out = []
    for p in range(parts):
        a = int(round(p * (n - 1) / float(parts)))
        b = int(round((p + 1) * (n - 1) / float(parts)))
        pts = points_in[a:b + 1] + points_out[a:b + 1]
        x0 = max(0.0, min(q[0] for q in pts))
        x1 = min(16.0, max(q[0] for q in pts))
        z0 = max(0.0, min(q[1] for q in pts))
        z1 = min(16.0, max(q[1] for q in pts))
        if x1 - x0 > 0.01 and z1 - z0 > 0.01:
            out.append((round(x0, 2), round(z0, 2), round(x1, 2), round(z1, 2)))
    return out


CURVE_STATE = {"board": {}, "glass": {}}
CURVE_BOXES = {"board": [], "glass": []}
for idx, (cell, th0, th1) in enumerate(CURVE):
    fine = math.radians(0.25)
    for kind, polys, (ri, ro) in (
            ("board", curve_board_polys(cell, th0, th1), (R_CURVE, R_CURVE + BOARD_T / 16.0)),
            ("glass", curve_glass_polys(cell, th0, th1),
             (R_CURVE + GLASS_IN / 16.0, R_CURVE + GLASS_OUT / 16.0))):
        CURVE_STATE[kind][idx] = write_obj("curve_%s_%02d" % (kind, idx), polys)
        CURVE_BOXES[kind].append(boxes_along(arc(cell, th0, th1, ri, fine),
                                             arc(cell, th0, th1, ro, fine)))
CORNER_BOXES = {}
for kind, (o1, o2) in (("board", (0.0, BOARD_T)), ("glass", (GLASS_IN, GLASS_OUT))):
    d = math.sqrt(2)
    inner = [(16.0 * t - o1 / d, 16.0 * (1 - t) - o1 / d) for t in
             [k / 64.0 for k in range(65)]]
    outer = [(16.0 * t - o2 / d, 16.0 * (1 - t) - o2 / d) for t in [k / 64.0 for k in range(65)]]
    CORNER_BOXES[kind] = boxes_along(inner, outer)

CORNERS = (("nw", 0), ("ne", 90), ("se", 180), ("sw", 270))
CORNER_NAMES = {"nw": ("North-West", "Nordwest", "Noroeste", "Nordväst"),
                "ne": ("North-East", "Nordost", "Noreste", "Nordost"),
                "se": ("South-East", "Südost", "Sureste", "Sydost"),
                "sw": ("South-West", "Südwest", "Suroeste", "Sydväst")}


def curve_state(kind, y):
    v = {"forge_marker": 1, "defaults": {"model": CURVE_STATE[kind][0], "custom": {"ambient": False}},
         "variants": {"cell": {str(i): {"model": CURVE_STATE[kind][i]} for i in range(15)},
                      "inventory": [{"model": M("corner_item_" + kind)}]}}
    if y:
        v["defaults"]["y"] = y
    return v


for kind, names in (("board", ("Dasher Board, Curve", "Bande, Kurve", "Valla de Pista, Curva",
                               "Sarg, Kurva")),
                    ("glass", ("Rink Glass, Curve", "Bandenverglasung, Kurve",
                               "Vidrio de Pista, Curva", "Sargglas, Kurva"))):
    for corner, y in CORNERS:
        reg = "dasher_%s_curve_%s" % (kind, corner)
        java = 'new BlockDasherCurve("%s", %s, EnumFacing.%s)' % (
            reg, "true" if kind == "glass" else "false",
            {"nw": "NORTH", "ne": "EAST", "se": "SOUTH", "sw": "WEST"}[corner])
        C.add(reg, java, tuple("%s (%s)" % (names[i], CORNER_NAMES[corner][i]) for i in range(4)),
              {}, curve_state(kind, y), tab=ARENA)


def shapes_java():
    def boxes(bs):
        return "{" + ", ".join("%s, %s, %s, %s" % tuple(_n(v) for v in b) for b in bs) + "}"
    lines = ["package com.micatechnologies.minecraft.csm.venues.arena;", "",
             "/**",
             " * The collision boxes of the dasher board corners and curves, generated by",
             " * {@code dev-env-utils/scripts/gen_venues_arena.py} from the geometry it draws. Do not",
             " * edit.",
             " *",
             " * <p>Each box is {@code x0, z0, x1, z1} in pixels within the cell, as drawn: a curve at",
             " * a rink's north-west corner (turned for the others), a corner as the north-west",
             " * chamfer (facing east). The height is the block's own.</p>",
             " *",
             " * @since 2026.10",
             " */",
             "final class DasherShapes {", "",
             "  /** The fifteen curve cells' offsets {dx, dz} from the ice's corner cell, as drawn. */",
             "  static final int[][] CURVE_CELLS = {%s};" % ", ".join(
                 "{%d, %d}" % c[0] for c in CURVE), "",
             "  /** The curve's board boxes, by cell. */",
             "  static final double[][] CURVE_BOARD = {"]
    lines += ["      %s," % boxes(b) for b in CURVE_BOXES["board"]]
    lines += ["  };", "", "  /** The curve's glass boxes, by cell. */",
              "  static final double[][] CURVE_GLASS = {"]
    lines += ["      %s," % boxes(b) for b in CURVE_BOXES["glass"]]
    lines += ["  };", "",
              "  /** The 45 degree corner's board boxes. */",
              "  static final double[] CORNER_BOARD = %s;" % boxes(CORNER_BOXES["board"]), "",
              "  /** The 45 degree corner's glass boxes. */",
              "  static final double[] CORNER_GLASS = %s;" % boxes(CORNER_BOXES["glass"]), "",
              "  private DasherShapes() {", "  }", "}", ""]
    wrapped = []
    for line in lines:
        while len(line) > 100:
            cut = line.rfind(", ", 0, 99)
            wrapped.append(line[:cut + 1])
            line = "          " + line[cut + 2:]
        wrapped.append(line)
    return "\n".join(wrapped)


def _n(v):
    return ("%.2f" % v).rstrip("0").rstrip(".") if v != int(v) else "%d" % v


JAVA["DasherShapes.java"] = shapes_java()
DOC["curve"] = [(c[0], round(math.degrees(c[1]), 1), round(math.degrees(c[2]), 1)) for c in CURVE]


def display(scale, centre, gui_rot=(30, 225, 0)):
    """Item display transforms for a model bigger than its cell: scaled down and its centre
    (x, y, z in pixels) brought to the slot's."""
    t = [round((8 - centre[0]) * scale, 2), round((8 - centre[1]) * scale, 2),
         round((8 - centre[2]) * scale, 2)]
    s = [scale] * 3
    # the slot's translation is applied after the turn, so the centre's offset is turned too
    ax, ay = math.radians(gui_rot[0]), math.radians(gui_rot[1])
    d = [8 - centre[0], 8 - centre[1], 8 - centre[2]]
    d = [d[0] * math.cos(ay) + d[2] * math.sin(ay), d[1], -d[0] * math.sin(ay) + d[2] * math.cos(ay)]
    d = [d[0], d[1] * math.cos(ax) - d[2] * math.sin(ax), d[1] * math.sin(ax) + d[2] * math.cos(ax)]
    g = [round(v * scale, 2) for v in d]
    return {"gui": {"rotation": list(gui_rot), "translation": g, "scale": s},
            "ground": {"translation": [0, 2, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"rotation": [0, 180, 0], "translation": t, "scale": s},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [scale * 0.6] * 3}}


# --- doors ----------------------------------------------------------------------------------
def door_leaf(penalty):
    els = [box([0.25, 0, 0], [15.75, 15, 4], "board", faces=("north", "south", "east", "west",
                                                            "down"),
               per={"south": "back", "east": "back", "west": "back"}),
           box([0.25, 15, -CAP_OVER], [15.75, 16, 4 + CAP_OVER], "cap"),
           box([11.5, 7.5, 4], [14.5, 9, 5], "latch"),
           box([0, 2, 4], [1.0, 4.5, 4.75], "latch"),
           box([0, 10.5, 4], [1.0, 13, 4.75], "latch")]
    if penalty:
        els += [box([0.75, 16, GLASS_IN], [15.25, 31, GLASS_OUT], "glass",
                    faces=("north", "south")),
                box([0.25, 16, 1.25], [0.75, 32, 3.0], "post"),
                box([15.25, 16, 1.25], [15.75, 32, 3.0], "post"),
                box([0.75, 31, 1.25], [15.25, 32, 3.0], "post")]
    return els


def swing(el, pivot=(0.0, 4.0)):
    """A closed leaf's element swung a quarter turn about its hinge at the back of the x = 0
    end, away from the ice: (x, z) -> (px + (pz - z), pz + (x - px))."""
    px, pz = pivot
    x0, y0, z0 = el["from"]
    x1, y1, z1 = el["to"]
    nx0, nx1 = sorted((px + (pz - z0), px + (pz - z1)))
    nz0, nz1 = sorted((pz + (x0 - px), pz + (x1 - px)))
    names = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up",
             "down": "down"}
    faces = tuple(names[f] for f in el["faces"])
    per = {names[f]: v["texture"][1:] for f, v in el["faces"].items()}
    first = next(iter(per.values()))
    return box([round(nx0, 3), y0, round(nz0, 3)], [round(nx1, 3), y1, round(nz1, 3)], first,
               faces=faces, per=per)


def door_models(name, penalty):
    leaf = door_leaf(penalty)
    tex = dict(DTEX)
    closed = vc.model(tex, leaf, ao=False)
    if penalty:
        # two blocks tall: shown smaller in a slot, so its glass leaf stays inside it
        closed["display"] = display(0.42, (8, 16, 2))
    return {name + "_closed_left": closed,
            name + "_closed_right": vc.model(tex, [vc.mirror_x(e) for e in leaf], ao=False),
            name + "_open_left": vc.model(tex, [swing(e) for e in leaf], ao=False),
            name + "_open_right": vc.model(tex, [vc.mirror_x(swing(e)) for e in leaf], ao=False)}


def door_state(name):
    def choose(v):
        body = {"model": M("%s_%s_%s" % (name, "open" if v["open"] == "true" else "closed",
                                          "right" if v["right"] == "true" else "left"))}
        y = ROT_Y[v["facing"]]
        if y:
            body["y"] = y
        return body
    return vc.keyed_state([("facing", ["north", "east", "south", "west"]),
                           ("open", ["false", "true"]), ("right", ["false", "true"])], choose,
                          inventory={"model": M(name + "_closed_left")})


C.add("dasher_bench_gate", 'new BlockDasherDoor("dasher_bench_gate", false)',
      ("Player Bench Gate", "Spielerbank-Tür", "Puerta del Banquillo", "Spelarbåsgrind"),
      door_models("bench_gate", False), door_state("bench_gate"), tab=ARENA)
C.add("dasher_penalty_door", 'new BlockDasherDoor("dasher_penalty_door", true)',
      ("Penalty Box Door", "Strafbank-Tür", "Puerta del Banco de Castigo", "Utvisningsbåsdörr"),
      door_models("penalty_door", True), door_state("penalty_door"), tab=ARENA)


# ------------------------------------------------------------------------------------------
# Units (BlockVenueUnit): footprints, boxes and the tab line
# ------------------------------------------------------------------------------------------
def unit_cells(boxes_px, extra=()):
    """Every cell the boxes (x0, y0, z0, x1, y1, z1 in pixels from the master) reach, but the
    master, model frame; checked connected through the footprint."""
    def axis(lo, hi, tol=6.0):
        # the cells a box claims along one axis: those it reaches 6 px into, or wholly holds;
        # a sliver past a cell line is left to the cell beside, whose collision covers it
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
    cells |= set(extra)
    cells.discard((0, 0, 0))
    reach = {(0, 0, 0)}
    frontier = [(0, 0, 0)]
    while frontier:
        c = frontier.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (c[0] + d[0], c[1] + d[1], c[2] + d[2])
            if n in cells and n not in reach:
                reach.add(n)
                frontier.append(n)
    missing = cells - reach
    assert not missing, "unit cells not connected: %s" % sorted(missing)
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def unit_java(reg, boxes_px, extra=()):
    cells = unit_cells(boxes_px, extra)
    flat_c = ", ".join("%d, %d, %d" % c for c in cells)
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    DOC.setdefault("units", {})[reg] = cells
    return 'new BlockVenueUnit("%s", new int[]{%s}, new int[]{%s})' % (reg, flat_c, flat_b)


def unit_state(model, obj=False, translucent=False):
    d = {"model": model}
    if obj:
        d["custom"] = {"ambient": False}
    return {"forge_marker": 1, "defaults": d,
            "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                         "inventory": [{}]}}


# --- hockey goal ----------------------------------------------------------------------------
@C.texture("net")
def _net():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for k in range(32):
        for x in range(32):
            if (x + k) % 6 == 0 or (x - k) % 6 == 0:
                img.putpixel((x, k), (246, 246, 244, 255))
    return img


@C.texture("net_side")
def _net_side():
    """The side net: the mesh cut to the side's trapezoid (the top 6 px deep, the foot 14),
    mapped 1:1 onto a face 14 px deep and 19.5 px tall."""
    base = _net()
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            # texture x 0 is the front post, 32 the foot of the back; y 0 is the top
            depth = 6 + (14 - 6) * (y / 31.0)
            if x / 32.0 * 14 <= depth:
                img.putpixel((x, y), base.getpixel((x, y)))
    for y in range(32):
        img.putpixel((0, y), (246, 246, 244, 255))
    return img


GOAL_POST_R = 0.85
GOAL_TOP = 19.5
GOAL_X = (0.85, 31.15)


def goal_model():
    tex = {"red": T("goal_red"), "white": T("goal_white"), "net": T("net"),
           "side": T("net_side"), "particle": T("goal_red")}
    els = []
    for x in GOAL_X:
        els += vc.lsc.post(x, 8.0, GOAL_POST_R, 0, GOAL_TOP, "red")
        els += vc.lsc.pipe_z(x, GOAL_TOP - 0.8, 0.7, 8.0, 14.0, "red", front=False)
        els += vc.lsc.pipe_z(x, 0.7, 0.7, 8.0, 22.0, "white", front=False)
        back = box([x - 0.6, 0, 21.4], [x + 0.6, GOAL_TOP - 0.2, 22.6], "red")
        back["rotation"] = {"origin": [x, 0, 22.0], "axis": "x", "angle": -22.5}
        els.append(back)
    els += vc.lsc.pipe_x(GOAL_TOP - 0.8, 8.0, 0.8, GOAL_X[0], GOAL_X[1], "red")
    els += vc.lsc.pipe_x(0.7, 22.0, 0.7, GOAL_X[0], GOAL_X[1], "white")
    top = {"from": [GOAL_X[0], GOAL_TOP - 1.0, 8.5], "to": [GOAL_X[1], GOAL_TOP - 1.0, 14.0],
           "faces": {"up": {"texture": "#net", "uv": [0, 0, 16, 5]},
                     "down": {"texture": "#net", "uv": [0, 0, 16, 5]}}}
    back = {"from": [GOAL_X[0], 0.6, 22.0], "to": [GOAL_X[1], GOAL_TOP - 0.2, 22.0],
            "rotation": {"origin": [16, 0, 22.0], "axis": "x", "angle": -22.5},
            "faces": {"north": {"texture": "#net", "uv": [0, 0, 16, 10]},
                      "south": {"texture": "#net", "uv": [0, 0, 16, 10]}}}
    els += [top, back]
    for x in GOAL_X:
        els.append({"from": [x, 0.3, 8.0], "to": [x, GOAL_TOP - 0.9, 22.0],
                    "faces": {"east": {"texture": "#side", "uv": [16, 0, 0, 16]},
                              "west": {"texture": "#side", "uv": [0, 0, 16, 16]}}})
    m = vc.model(tex, els)
    m["display"] = display(0.4, (16, 10, 15))
    return m


GOAL_BOXES = [(0, 0, 7, 32, 20, 23)]
C.add("hockey_goal", unit_java("hockey_goal", GOAL_BOXES),
      ("Hockey Goal", "Eishockeytor", "Portería de Hockey", "Hockeymål"),
      {"hockey_goal": goal_model()}, unit_state(M("hockey_goal")), tab=ARENA)


# --- basketball stanchion (OBJ: it stands four and a half blocks tall) ------------------------
def shot_clock_frame(n, lit_only=False):
    def draw():
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0) if lit_only else (12, 12, 14, 255))
        if not lit_only:
            vc.frame(img, 0, 0, 32, 32, (40, 40, 44))
        text = "%d" % n
        w = vc.text_width(text, 3)
        vc.draw_text(img, text, (32 - w) // 2, 9, LED_RED, 3)
        return img
    return draw


vc.animation(C, "shot_clock", [shot_clock_frame(n) for n in range(24, -1, -1)], 20)
vc.animation(C, "shot_clock_e", [shot_clock_frame(n, True) for n in range(24, -1, -1)], 20)


@C.texture("backboard")
def _backboard():
    img = Image.new("RGBA", (64, 64), (210, 228, 234, 70))
    w = (250, 250, 250, 255)
    vc.frame(img, 0, 0, 64, 64, w, width=2)
    # the shooter's square: 24 x 18 in on a 72 x 42 in board, its foot at the rim's height
    sx0, sx1 = 32 - 11, 32 + 11
    sy1 = 64 - 9
    sy0 = sy1 - 26
    vc.frame(img, sx0, sy0, sx1, sy1, w, width=2)
    return img


@C.texture("pad_logo")
def _pad_logo():
    img = vc.swatch(PAD_NAVY, 46, grain=3)
    vc.rect(img, 0, 6, 16, 8, (236, 236, 232))
    return img


SB = {}     # stanchion geometry, blocks from the master's cell origin (x 0..1)
CX = 0.5
BACKBOARD_Z = -2.22     # its face; the baseline is the master's front edge less one block
RIM_Y = 3.05


def p16(v):
    return v * 16.0


def obox_b(x0, x1, y0, y1, z0, z1, mat, faces="nsewud"):
    return vc.obox(p16(x0), p16(x1), p16(y0), p16(y1), p16(z0), p16(z1), mat, faces)


def beam(a, b, w, h, mat):
    """A box beam from a to b (block points), w wide (x) and h deep, its ends square to it; in
    the y-z plane (x is across)."""
    ay, az = p16(a[1]), p16(a[2])
    by_, bz = p16(b[1]), p16(b[2])
    dy, dz = by_ - ay, bz - az
    ln = math.hypot(dy, dz)
    ny, nz = -dz / ln * p16(h) / 2, dy / ln * p16(h) / 2
    x0, x1 = p16(a[0] - w / 2), p16(a[0] + w / 2)
    q = [(ay + ny, az + nz), (by_ + ny, bz + nz), (by_ - ny, bz - nz), (ay - ny, az - nz)]
    polys = []
    for x, out in ((x0, -1), (x1, 1)):
        polys.append(vc.Poly([(x, y, z) for y, z in q], mat, (out, 0, 0)))
    for k in range(4):
        (y0, z0), (y1, z1) = q[k], q[(k + 1) % 4]
        cy = sum(p[0] for p in q) / 4
        cz = sum(p[1] for p in q) / 4
        n = ((y0 + y1) / 2 - cy, (z0 + z1) / 2 - cz)
        polys.append(vc.Poly([(x0, y0, z0), (x1, y0, z0), (x1, y1, z1), (x0, y1, z1)], mat,
                             (0, n[0], n[1])))
    return polys


def stanchion_polys():
    P = []
    # base: steel cabinet with a padded front and top, on four casters
    P += obox_b(0.02, 0.98, 0.12, 0.92, 0.0, 2.4, "steel", "sewud")
    P += obox_b(0.0, 1.0, 0.1, 1.0, -0.12, 0.0, "pad", "nsewud")
    P += obox_b(0.0, 1.0, 0.92, 1.02, 0.0, 1.2, "pad", "sewu")
    for x in (0.08, 0.92):
        for z in (0.2, 2.2):
            P += obox_b(x - 0.05, x + 0.05, 0.0, 0.12, z - 0.06, z + 0.06, "tire")
    # the boom: up and forward over the baseline, padded at its foot
    foot = (CX, 0.95, 0.55)
    head = (CX, 3.18, -1.55)
    P += beam(foot, head, 0.2, 0.24, "steel")
    P += beam((CX, 0.95, 0.55), (CX, 2.0, -0.33), 0.26, 0.32, "pad")
    # the extension arm to the back of the board, and its brace
    P += obox_b(CX - 0.08, CX + 0.08, 3.12, 3.3, BACKBOARD_Z + 0.05, -1.45, "steel")
    P += beam((CX, 3.0, -1.6), (CX, 3.24, BACKBOARD_Z + 0.06), 0.1, 0.08, "steel")
    # backboard 72 x 42 in, its foot padded
    bx0, bx1 = CX - 0.915, CX + 0.915
    P += obox_b(bx0, bx1, 2.9, 3.97, BACKBOARD_Z, BACKBOARD_Z + 0.04, "board", "nsewud")
    P += obox_b(bx0 - 0.02, bx1 + 0.02, 2.84, 2.92, BACKBOARD_Z - 0.03, BACKBOARD_Z + 0.07,
                "pad", "nsewud")
    # rim: 18 in inside, on a bracket, the net under it
    rz = BACKBOARD_Z - 0.15 - 0.229
    P += obox_b(CX - 0.1, CX + 0.1, RIM_Y - 0.08, RIM_Y, BACKBOARD_Z - 0.16, BACKBOARD_Z, "rim")
    seg_n = 16
    for k in range(seg_n):
        a0 = 2 * math.pi * k / seg_n
        a1 = 2 * math.pi * (k + 1) / seg_n
        for r_in, r_out in ((0.229, 0.255),):
            pts_top = [(CX + r_in * math.cos(a0), rz + r_in * math.sin(a0)),
                       (CX + r_in * math.cos(a1), rz + r_in * math.sin(a1)),
                       (CX + r_out * math.cos(a1), rz + r_out * math.sin(a1)),
                       (CX + r_out * math.cos(a0), rz + r_out * math.sin(a0))]
            P.append(vc.Poly([(p16(x), p16(RIM_Y), p16(z)) for x, z in pts_top], "rim", (0, 1, 0)))
            P.append(vc.Poly([(p16(x), p16(RIM_Y - 0.03), p16(z)) for x, z in pts_top], "rim",
                             (0, -1, 0)))
            for (r, s) in ((r_out, 1), (r_in, -1)):
                xa, za = CX + r * math.cos(a0), rz + r * math.sin(a0)
                xb, zb = CX + r * math.cos(a1), rz + r * math.sin(a1)
                n = (s * math.cos((a0 + a1) / 2), 0, s * math.sin((a0 + a1) / 2))
                P.append(vc.Poly([(p16(xa), p16(RIM_Y - 0.03), p16(za)),
                                  (p16(xb), p16(RIM_Y - 0.03), p16(zb)),
                                  (p16(xb), p16(RIM_Y), p16(zb)), (p16(xa), p16(RIM_Y), p16(za))],
                                 "rim", n))
        # the net: a truncated cone, both faces
        rt, rb, drop = 0.235, 0.13, 0.42
        xa, za = CX + rt * math.cos(a0), rz + rt * math.sin(a0)
        xb, zb = CX + rt * math.cos(a1), rz + rt * math.sin(a1)
        xc, zc = CX + rb * math.cos(a1), rz + rb * math.sin(a1)
        xd, zd = CX + rb * math.cos(a0), rz + rb * math.sin(a0)
        quad = [(p16(xd), p16(RIM_Y - drop), p16(zd)), (p16(xc), p16(RIM_Y - drop), p16(zc)),
                (p16(xb), p16(RIM_Y - 0.03), p16(zb)), (p16(xa), p16(RIM_Y - 0.03), p16(za))]
        n = (math.cos((a0 + a1) / 2), 0.3, math.sin((a0 + a1) / 2))
        uvs = [(0, 1), (1, 1), (1, 0), (0, 0)]
        P.append(vc.Poly(quad, "net", n, uvs))
        P.append(vc.Poly(list(reversed(quad)), "net", (-n[0], -n[1], -n[2]), list(reversed(uvs))))
    # shot clock on top of the board: a display each way and a frame
    sx0, sx1 = CX - 0.32, CX + 0.32
    P += obox_b(sx0, sx1, 4.0, 4.36, BACKBOARD_Z - 0.06, BACKBOARD_Z + 0.1, "dark", "sewud")
    P.append(vc.Poly([(p16(sx1), p16(4.0), p16(BACKBOARD_Z - 0.061)),
                      (p16(sx0), p16(4.0), p16(BACKBOARD_Z - 0.061)),
                      (p16(sx0), p16(4.36), p16(BACKBOARD_Z - 0.061)),
                      (p16(sx1), p16(4.36), p16(BACKBOARD_Z - 0.061))], "clock", (0, 0, -1),
                     [(0.04, 0.82), (0.96, 0.82), (0.96, 0.18), (0.04, 0.18)]))
    P.append(vc.Poly([(p16(sx0), p16(4.0), p16(BACKBOARD_Z + 0.101)),
                      (p16(sx1), p16(4.0), p16(BACKBOARD_Z + 0.101)),
                      (p16(sx1), p16(4.36), p16(BACKBOARD_Z + 0.101)),
                      (p16(sx0), p16(4.36), p16(BACKBOARD_Z + 0.101))], "clock", (0, 0, 1),
                     [(0.04, 0.82), (0.96, 0.82), (0.96, 0.18), (0.04, 0.18)]))
    for x in (CX - 0.25, CX + 0.25):
        P += obox_b(x - 0.03, x + 0.03, 3.97, 4.0, BACKBOARD_Z, BACKBOARD_Z + 0.04, "steel")
    return P


C.extra["models/block/venues/arena/stanchion.mtl"] = vc.mtl_text(GEN, {
    "steel": T("steel"), "pad": T("pad_navy"), "tire": T("tire"), "board": T("backboard"),
    "rim": T("rim"), "net": T("net"), "dark": T("dark"), "clock": T("shot_clock")})
stanchion_obj = write_obj("basketball_stanchion", stanchion_polys(), mtl="stanchion")


def px_box(x0, y0, z0, x1, y1, z1):
    return tuple(int(round(v * 16)) for v in (x0, y0, z0, x1, y1, z1))


STANCHION_BOXES = [px_box(0, 0, -0.12, 1, 1.02, 2.4),                 # base
                   px_box(0.35, 1.0, -0.5, 0.65, 2.0, 0.5),          # boom, lower
                   px_box(0.4, 2.0, -1.5, 0.6, 3.2, -0.5),           # boom, upper
                   px_box(0.42, 3.0, -2.2, 0.58, 3.3, -1.5),         # arm
                   px_box(-0.415, 2.84, -2.25, 1.415, 3.97, -2.15),  # backboard
                   px_box(0.24, 2.98, -2.72, 0.76, 3.06, -2.22),     # rim
                   px_box(0.18, 3.97, -2.28, 0.82, 4.36, -2.12)]     # shot clock


def stanchion_icon():
    tex = {"steel": T("steel"), "pad": T("pad_navy"), "board": T("backboard"), "rim": T("rim"),
           "particle": T("pad_navy")}
    els = [box([4, 0, 6], [12, 4, 16], "steel"), box([4, 0, 5], [12, 4.5, 6], "pad"),
           box([7, 4, 4], [9, 11, 6], "steel"), box([7, 10, 2], [9, 11.5, 4], "steel"),
           box([2, 9, 1], [14, 15, 1.5], "board", faces=("north", "south", "east", "west", "up",
                                                         "down")),
           box([6, 9.5, -1.5], [10, 10, 1], "rim")]
    els[4]["faces"]["north"]["uv"] = [0, 0, 16, 16]
    els[4]["faces"]["south"]["uv"] = [0, 0, 16, 16]
    m = vc.model(tex, els)
    return m


C.add("basketball_stanchion", unit_java("basketball_stanchion", STANCHION_BOXES),
      ("Basketball Stanchion", "Basketballanlage", "Canasta de Baloncesto Portátil",
       "Basketkorgställning"),
      {"stanchion_item": stanchion_icon()},
      {"forge_marker": 1, "defaults": {"model": stanchion_obj, "custom": {"ambient": False}},
       "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                    "inventory": [{"model": M("stanchion_item"), "custom": {}}]}}, tab=ARENA)


# --- the ice resurfacer: three blocks long, two wide, two tall, drawn by its master ---------
@C.texture("resurf_body")
def _resurf_body():
    img = vc.swatch((236, 238, 240), 47, grain=2)
    vc.rect(img, 0, 9, 16, 12, (24, 72, 150))
    vc.rect(img, 0, 12, 16, 13, (120, 176, 220))
    return img


C.textures["resurf_tank"] = lambda: vc.swatch((228, 232, 236), 48, grain=2)
C.textures["resurf_trim"] = lambda: vc.swatch((24, 72, 150), 49, grain=2)
C.textures["resurf_seat"] = lambda: vc.swatch((28, 28, 30), 50, grain=3)
C.textures["resurf_grille"] = lambda: _grille((70, 74, 80), 51)


def _grille(colour, seed):
    img = vc.swatch(colour, seed, grain=2)
    for y in range(1, 16, 3):
        vc.rect(img, 1, y, 15, y + 1, vc.shade(colour, 0.55))
    return img


def resurfacer_model():
    tex = {"body": T("resurf_body"), "tank": T("resurf_tank"), "trim": T("resurf_trim"),
           "seat": T("resurf_seat"), "grille": T("resurf_grille"), "tire": T("tire"),
           "steel": T("steel"), "dark": T("dark"), "particle": T("resurf_body")}
    els = [
        box([1, 3, -15], [31, 15, 29], "body"),                      # chassis and body
        box([2, 15, -14], [30, 28, 9], "tank"),                      # snow tank
        box([1.5, 27, -14.5], [30.5, 28.5, 9.5], "trim"),            # tank rim
        box([4, 15, 10], [28, 18, 27], "dark"),                      # deck
        box([10, 18, 15], [22, 21, 24], "seat"),                     # seat
        box([10, 21, 22], [22, 28, 24], "seat"),                     # seat back
        box([14.5, 18, 10.5], [17.5, 23, 12.5], "dark"),             # steering column
        box([12, 23, 10], [20, 23.75, 13], "dark"),                  # wheel
        box([2, 1, 28], [30, 7, 32], "steel"),                       # conditioner
        box([2, 7, 28], [30, 9, 31], "trim"),
        box([3, 4, -16], [29, 12, -15], "grille"),                   # front grille
        box([24, 15, 12], [29, 26, 14], "trim"),                     # mast / beacon post
        box([25.5, 26, 12.5], [27.5, 28, 13.5], "dark"),
    ]
    for z in (-9.0, 21.0):
        els += vc.lsc.pipe_x(4.0, z, 4.0, 0.1, 3.5, "tire")
        els += vc.lsc.pipe_x(4.0, z, 4.0, 28.5, 31.9, "tire")
    m = vc.model(tex, els)
    m["display"] = display(0.3, (16, 14, 8))
    return m


RESURF_BOXES = [(0, 0, -16, 32, 29, 32)]
C.add("ice_resurfacer", unit_java("ice_resurfacer", RESURF_BOXES),
      ("Ice Resurfacer", "Eisbearbeitungsmaschine", "Pulidora de Hielo", "Ismaskin"),
      {"ice_resurfacer": resurfacer_model()}, unit_state(M("ice_resurfacer")), tab=ARENA)


# --- the ice plant: a packaged chiller, two wide and two tall --------------------------------
C.textures["plant_cabinet"] = lambda: vc.swatch((176, 182, 186), 52, grain=2)
C.textures["plant_louvre"] = lambda: _grille((150, 156, 162), 53)
C.textures["pipe_insul"] = lambda: vc.swatch((30, 30, 32), 54, grain=3)


@C.texture("plant_hmi")
def _plant_hmi():
    img = vc.swatch((60, 64, 70), 55, grain=2)
    vc.rect(img, 2, 2, 14, 9, (14, 30, 40))
    vc.draw_text(img, "-8", 3, 3, (90, 220, 255))
    vc.rect(img, 3, 11, 5, 13, (60, 210, 90))
    vc.rect(img, 7, 11, 9, 13, (240, 190, 40))
    vc.rect(img, 11, 11, 13, 13, (210, 50, 40))
    return img


@C.texture("plant_gauge")
def _plant_gauge():
    img = vc.swatch((60, 64, 70), 56, grain=2)
    vc.disc(img, 8, 8, 7, (220, 220, 214))
    vc.disc(img, 8, 8, 6, (246, 246, 240))
    for k in range(9):
        a = math.radians(225 - k * 33.75)
        img.putpixel((int(8 + 5 * math.cos(a)), int(8 - 5 * math.sin(a))), (40, 40, 40, 255))
    for t in range(5):
        img.putpixel((8 + t, 8 - t // 2), (200, 30, 30, 255))
    return img


def plant_model():
    tex = {"cab": T("plant_cabinet"), "louvre": T("plant_louvre"), "hmi": T("plant_hmi"),
           "gauge": T("plant_gauge"), "insul": T("pipe_insul"), "steel": T("steel"),
           "dark": T("dark"), "particle": T("plant_cabinet")}
    els = [box([0, 0, 3], [32, 2, 16], "dark"),                      # base rail
           box([0.5, 2, 3.5], [31.5, 26, 15.5], "cab"),
           box([2, 4, 3], [14, 22, 3.5], "louvre", faces=("north",)),
           box([18, 14, 3], [26, 22, 3.5], "hmi", faces=("north",)),
           box([18, 6, 3], [21, 9, 3.5], "gauge", faces=("north",)),
           box([23, 6, 3], [26, 9, 3.5], "gauge", faces=("north",)),
           box([28, 4, 3], [30, 22, 3.25], "dark", faces=("north",))]
    for x in (8.0, 22.0):
        els += vc.lsc.post(x, 9.5, 2.2, 26, 31.9, "insul")
        els += vc.lsc.post(x, 9.5, 2.6, 30.5, 31.5, "steel")
    els += vc.lsc.pipe_x(29.0, 9.5, 1.8, 8.0, 22.0, "insul")
    m = vc.model(tex, els)
    m["display"] = display(0.4, (16, 16, 9.5))
    return m


PLANT_BOXES = [(0, 0, 3, 32, 32, 16)]
C.add("ice_plant", unit_java("ice_plant", PLANT_BOXES),
      ("Ice Plant Chiller", "Eisanlagen-Kältemaschine", "Enfriadora de Pista de Hielo",
       "Kylaggregat för Isbana"),
      {"ice_plant": plant_model()}, unit_state(M("ice_plant")), tab=ARENA)

C.add("venue_unit_part", "new BlockVenueUnitPart()",
      ("Venue Unit Part", "Teil einer Veranstaltungseinheit", "Parte de Unidad del Recinto",
       "Del av Arenaenhet"),
      {"unit_part": {"textures": {"particle": T("steel")}, "elements": []}},
      {"forge_marker": 1, "defaults": {"model": M("unit_part")},
       "variants": {"toward": {d: {} for d in ("down", "up", "north", "south", "west", "east")},
                    "inventory": [{}]}}, tab=HIDDEN)


# ------------------------------------------------------------------------------------------
# Benches and the scorer's table (BlockArenaRun): runs closed by end pieces (actual state)
# ------------------------------------------------------------------------------------------
C.textures["bench_frame"] = lambda: vc.brushed((150, 154, 160), 57)
C.textures["table_top"] = lambda: vc.swatch((66, 68, 72), 58, grain=2)
C.textures["table_panel"] = lambda: vc.swatch((40, 42, 46), 59, grain=2)


def led_frame(k, n, lit_only=False):
    def draw():
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0) if lit_only else (10, 10, 12, 255))
        px = img.load()
        for y in range(5, 28):
            for x in range(32):
                # chevrons scrolling left with a slow colour sweep: tiles every 32 px
                ph = ((x + y * 0.5 + k * 2) % 16) / 16.0
                on = ph < 0.42
                hue = (x / 32.0 + k / float(n)) % 1.0
                if on:
                    r = int(140 + 115 * math.cos(2 * math.pi * hue))
                    g = int(140 + 115 * math.cos(2 * math.pi * (hue - 1 / 3.0)))
                    b = int(140 + 115 * math.cos(2 * math.pi * (hue - 2 / 3.0)))
                    if (x + y) % 2 == 0:
                        px[x, y] = vc.clamp((r, g, b)) + (255,)
                    else:
                        px[x, y] = vc.clamp((r * 0.7, g * 0.7, b * 0.7)) + (255,)
                elif not lit_only:
                    px[x, y] = (22, 22, 26, 255) if (x + y) % 2 == 0 else (14, 14, 16, 255)
        return img
    return draw


N_LED = 16
vc.animation(C, "scorer_led", [led_frame(k, N_LED) for k in range(N_LED)], 2)
vc.animation(C, "scorer_led_e", [led_frame(k, N_LED, True) for k in range(N_LED)], 2)

RUN_TEX = {"frame": T("bench_frame"), "pad": T("pad_black"), "pad2": T("pad_navy"),
           "top": T("table_top"), "panel": T("table_panel"), "led": T("scorer_led"),
           "particle": T("bench_frame")}


def bench_parts(back):
    els = [box([0, 7, 3], [16, 9, 13], "pad"),
           box([0, 5.5, 4], [16, 7, 12], "frame", faces=("north", "south", "down")),
           box([7.25, 0, 7], [8.75, 5.5, 9], "frame"),
           box([5, 0, 6], [11, 0.75, 10], "frame")]
    if back:
        els += [box([0, 10, 13.5], [16, 17, 15.5], "pad2"),
                box([7.25, 5.5, 13], [8.75, 17, 14], "frame", faces=("east", "west", "south")),
                box([0, 7, 13], [16, 8, 14.5], "frame")]
    return els


def bench_end(back):
    els = [box([0, 0, 3.5], [1.25, 9.5, 12.5], "frame")]
    if back:
        els.append(box([0, 9.5, 12.5], [1.25, 17.5, 15.5], "frame"))
    return els


def table_parts():
    els = [box([0, 13, 1], [16, 14, 15], "top"),
           box([0, 0, 0.5], [16, 1.5, 1.5], "panel"),
           box([0, 1.5, 0.5], [16, 13, 1.5], "panel", faces=("south", "up")),
           box([0, 12, 1.5], [16, 13, 15], "panel", faces=("down", "south")),
           {"from": [0, 1.5, 0.45], "to": [16, 13, 0.45],
            "faces": {"north": {"texture": "#led", "uv": [0, 2.5, 16, 14]}}}]
    return els


def table_end():
    return [box([0, 0, 0.5], [1, 13, 15], "panel")]


def run_models(prefix, parts, end, item_extra=None):
    left = end
    return {prefix: vc.model(RUN_TEX, parts),
            prefix + "_end_l": vc.model(RUN_TEX, left),
            prefix + "_end_r": vc.model(RUN_TEX, [vc.mirror_x(e) for e in left]),
            prefix + "_item": vc.model(RUN_TEX, parts + left + [vc.mirror_x(e) for e in left])}


def run_item(prefix):
    return {"parent": "csm:block/venues/arena/%s_item" % prefix}


C.add("player_bench", 'new BlockArenaRun("player_bench", BlockArenaRun.Kind.PLAYER_BENCH)',
      ("Player Bench", "Spielerbank", "Banquillo de Jugadores", "Spelarbänk"),
      run_models("player_bench", bench_parts(False), bench_end(False)),
      run_state("player_bench"), item=run_item("player_bench"), tab=ARENA)
C.add("penalty_bench", 'new BlockArenaRun("penalty_bench", BlockArenaRun.Kind.PENALTY_BENCH)',
      ("Penalty Box Bench", "Strafbank", "Banco de Castigo", "Utvisningsbänk"),
      run_models("penalty_bench", bench_parts(True), bench_end(True)),
      run_state("penalty_bench"), item=run_item("penalty_bench"), tab=ARENA)
C.add("scorers_table", 'new BlockArenaRun("scorers_table", BlockArenaRun.Kind.SCORERS_TABLE)',
      ("Scorer's Table", "Kampfrichtertisch", "Mesa de Anotadores", "Sekretariatsbord"),
      run_models("scorers_table", table_parts(), table_end()),
      run_state("scorers_table"), item=run_item("scorers_table"), tab=ARENA)

# ------------------------------------------------------------------------------------------
# Messages and the tab
# ------------------------------------------------------------------------------------------
C.add_lang("itemGroup.tabvenuesarena", ("CSM: Sports & Venues - Arena",
                                        "CSM: Sport & Veranstaltungsstätten - Arena",
                                        "CSM: Deportes y Recintos - Arena",
                                        "CSM: Sport & Arenor - Arena"))
C.add_lang("csm.venues.floor.kind", ("Marking: %s", "Markierung: %s", "Marca: %s",
                                     "Markering: %s"))
C.add_lang("csm.venues.unit.blocked", ("Not enough room: blocked at %s, %s, %s",
                                       "Nicht genug Platz: blockiert bei %s, %s, %s",
                                       "No hay espacio: bloqueado en %s, %s, %s",
                                       "Inte tillräckligt med plats: blockerat vid %s, %s, %s"))
_KIND_NAMES = {
    "ice.0": ("End zone face-off circle", "Bully-Kreis im Angriffsdrittel",
              "Círculo de saque de zona final", "Tekningscirkel i anfallszon"),
    "ice.1": ("Neutral zone face-off spot", "Bullypunkt in der neutralen Zone",
              "Punto de saque de zona neutral", "Tekningspunkt i neutral zon"),
    "ice.2": ("Centre ice", "Mittelkreis", "Centro de la pista", "Mittcirkel"),
    "ice.3": ("Goal crease", "Torraum", "Área de portería", "Målgård"),
    "court.0": ("Basket end, natural key", "Korbseite, natürliche Zone",
                "Fondo de canasta, zona natural", "Korgände, naturlig zon"),
    "court.1": ("Basket end, navy key", "Korbseite, marineblaue Zone",
                "Fondo de canasta, zona azul marino", "Korgände, marinblå zon"),
    "court.2": ("Basket end, crimson key", "Korbseite, karmesinrote Zone",
                "Fondo de canasta, zona carmesí", "Korgände, karmosinröd zon"),
    "court.3": ("Centre court", "Spielfeldmitte", "Centro de la pista", "Mittplan"),
}
for _k, _names in _KIND_NAMES.items():
    C.add_lang("csm.venues.floor." + _k, _names)


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def java_files():
    return {os.path.join(JAVA_DIR, name): text for name, text in JAVA.items()}


def main():
    if "--doc" in sys.argv:
        import json
        print(json.dumps(DOC, indent=1, default=str))
        return 0
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
