#!/usr/bin/env python3
"""
gen_venues_pool.py -- the swimming pool in the Sports & Venues Courts, Track & Pool tab.

    py -3 dev-env-utils/scripts/gen_venues_pool.py                    # write
    py -3 dev-env-utils/scripts/gen_venues_pool.py --check            # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_pool.py --fragments        # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_pool.py --layout pool_25m   # a pool's blocks (JSON)
    py -3 dev-env-utils/scripts/gen_venues_pool.py --layout pool_25m --commands X Y Z [--rot R]
    py -3 dev-env-utils/scripts/gen_venues_pool.py --preview DIR      # pictures of the floors

A competition pool built from blocks round real water, one block a metre: tile walls and floor,
a deck-level gutter, the deck. The floor's lane lines (25 cm, black, ending in a T 2 m from each
end wall) fall where they really are, 2.5 m apart, so they are drawn by the line art engine
(venues_lines.py, a third family, ``pool``): ``pool_floor_anchor`` and ``pool_floor_painted``
cells, ``pool_tile`` where no line falls. The end walls carry the lanes' targets
(``pool_wall_target``) or touch pads (``pool_touchpad``), each tile knowing which quarter of its
block the lane's middle is in.

Then the fittings: lane ropes (a run of floats at the water line, in the air block above the
water, the floats dipping into it), backstroke flags 5 m from each end on poles, numbered
starting blocks, a lifeguard chair, ladders and grab rails, 1 m and 3 m springboards and a 5 m
platform, a pace clock and the scoreboard (the venue text engine). No brand.

**Lanes.** An 8-lane pool is 21 m wide (lanes 1-8 and 0.5 m outside them), a 10-lane pool 25 m
(lanes 0-9): the same lanes fall in the same places from the side walls, a lane's middle a
quarter of the way across a block. Lanes are numbered from the right of a swimmer at the start
end; ``--layout`` lays the start end so its swimmers face north or east (``--rot`` 2 and 3 are
the same pools as 0 and 1 with their ends swapped), which is what lets a starting block place
itself from its lane number alone (BlockPoolStartingBlock).
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402
import venues_lines as vl  # noqa: E402
import venues_units as vu  # noqa: E402
import gen_venues_courts as gc  # noqa: E402

GEN = "gen_venues_pool.py"
C = vc.catalogue(GEN, "pool")
T = C.T
M = C.M
U = vu.Units(C, GEN, "pool")
box = vc.box
wbox = vc.lsc.pp.wbox
TAB = gc.TAB
FACINGS = vc.FACINGS
ys = vu.ys

WHITE = (240, 240, 236)
BLACK = (24, 24, 26)
STEEL = (196, 200, 206)
YELLOW = (240, 200, 30)
TILE_TINT = (0xC4, 0xC6, 0xC8)       # SurfaceColours 15, the walls' colour: tiles not tinted
#                                      by a handler are drawn in it
LANE_BLACK = 12                       # VenueColours.BLACK


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def mul(c, t):
    return tuple(int(c[k] * t[k] / 255) for k in range(3))


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def tile(size=32, base=(238, 240, 240), grout=(196, 200, 202), cell=8, seed=1):
    import random
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % cell == 0 or y % cell == 0:
                px[x, y] = rgba(grout)
            else:
                k = 1.0 + rng.uniform(-0.015, 0.015) + (0.02 if (x % cell == 1 or y % cell == 1)
                                                        else 0)
                px[x, y] = rgba(vc.shade(base, k))
    return img


@C.texture("pool_tile")
def _pool_tile():
    """Glazed 25 cm tile, light grey before its colour's tint."""
    return tile()


@C.texture("pool_tile_white")
def _pool_tile_white():
    """The same tile already in the walls' colour, for tiles with no tint (targets, pads)."""
    img = tile()
    px = img.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            px[x, y] = rgba(mul((r, g, b), TILE_TINT))
    return img


@C.texture("deck")
def _deck():
    """Non-slip deck tile, 33 cm squares with a raised dot grain."""
    import random
    rng = random.Random(4)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            c = (232, 232, 230)
            if x % 11 == 0 or y % 11 == 0:
                c = (200, 200, 198)
            elif (x * 7 + y * 3) % 5 == 0:
                c = vc.shade(c, 0.94)
            px[x, y] = rgba(vc.shade(c, 1 + rng.uniform(-0.02, 0.02)))
    return img


@C.texture("gutter_grate")
def _gutter():
    img = Image.new("RGBA", (16, 16), rgba(WHITE))
    for y in range(1, 16, 2):
        vc.rect(img, 0, y, 16, y + 1, rgba((90, 120, 140)))
    vc.rect(img, 0, 0, 16, 1, rgba((220, 222, 220)))
    return img


def target(stem_u, top):
    """A wall tile with the lane's target in black, as the swimmer sees it: the stem 25 cm wide
    centred at ``stem_u`` (texture px of 16, on a 32 px texture), the cross bar 50 cm across at
    the top."""
    img = _pool_tile_white()
    k = 2
    x0, x1 = int((stem_u - 2) * k), int((stem_u + 2) * k)
    vc.rect(img, x0, 0 if not top else 10, x1, 32, rgba(BLACK))
    if top:
        vc.rect(img, int((stem_u - 4) * k), 4, int((stem_u + 4) * k), 10, rgba(BLACK))
    return img


for _u in (4, 12):
    for _top in (0, 1):
        C.textures["target_%d_%d" % (_u, _top)] = (lambda u=_u, t=_top: target(u, t))


def pad(spans):
    """A touch pad tile: yellow-edged black panels over ``spans`` (u ranges of 16)."""
    img = _pool_tile_white()
    for a, b in spans:
        x0, x1 = int(round(a * 2)), int(round(b * 2))
        vc.rect(img, x0, 2, x1, 30, rgba(YELLOW))
        vc.rect(img, x0 + (2 if a > 0 else 0), 4, x1 - (2 if b < 16 else 0), 28, rgba(BLACK))
    return img


PAD_SPANS = {0: [(0, 16)], 1: [(0, 7.2), (8.8, 16)], 2: [(0.8, 16)], 3: [(0, 15.2)]}
for _p, _sp in PAD_SPANS.items():
    C.textures["pad_%d" % _p] = (lambda sp=_sp: pad(sp))

for _n, _c, _s in (("white", WHITE, 1), ("black", BLACK, 2), ("steel", STEEL, 3),
                   ("navy", (26, 40, 86), 4), ("red", (200, 36, 36), 5),
                   ("blue", (36, 84, 196), 6), ("green", (36, 150, 70), 7),
                   ("yellow", YELLOW, 8), ("orange", (232, 110, 30), 9),
                   ("grey", (128, 132, 136), 10), ("concrete", (178, 176, 170), 11),
                   ("board", (232, 232, 226), 12), ("tread", (60, 62, 66), 13),
                   ("canvas", (230, 60, 40), 14), ("cable", (230, 230, 230), 15)):
    C.textures[_n] = (lambda c=_c, s=_s: vc.swatch(c, 20261400 + s))


@C.texture("nonslip")
def _nonslip():
    img = vc.swatch((236, 236, 232), 20261420)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y * 3) % 4 == 0:
                px[x, y] = rgba((206, 206, 202))
    return img


for _num in range(10):
    def _numeral(n=_num):
        img = Image.new("RGBA", (16, 16), rgba((26, 40, 86)))
        s = str(n)
        vc.draw_text(img, s, 8 - vc.text_width(s, 2) // 2, 3, WHITE, 2)
        return img
    C.textures["num_%d" % _num] = _numeral


@C.texture("flags")
def _flags():
    """Backstroke pennants: triangles, red, white and blue in turn, on a cutout strip."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    cols = [(200, 36, 36), (240, 240, 236), (36, 84, 196)]
    for k in range(4):
        x0 = k * 8
        for y in range(12):
            half = 4 * (1 - y / 12.0)
            for x in range(x0, x0 + 8):
                if abs(x + 0.5 - (x0 + 4)) <= half:
                    px[x, y] = rgba(cols[k % 3])
    return img


@C.texture("dial")
def _dial():
    """A 60 second pace clock's face: white, a tick a second, a heavy tick every five, the
    fives numbered, on a black square case front."""
    size = 64
    img = Image.new("RGBA", (size, size), rgba((20, 20, 22)))
    c = size / 2.0
    r = size / 2.0 - 2
    vc.disc(img, c, c, r, rgba((246, 246, 240)))
    px = img.load()
    for s in range(60):
        a = math.radians(s * 6)
        heavy = s % 5 == 0
        for t in range(0, 30):
            rr = r - 0.5 - t * 0.1
            if rr < r - (5 if heavy else 2.5):
                break
            for w in ((-0.6, 0, 0.6) if heavy else (0,)):
                x = c + rr * math.sin(a) + w * math.cos(a)
                y = c - rr * math.cos(a) + w * math.sin(a)
                if 0 <= x < size and 0 <= y < size:
                    px[int(x), int(y)] = rgba((20, 20, 22))
    for s in range(0, 60, 5):
        a = math.radians(s * 6)
        txt = str(60 if s == 0 else s)
        x = c + (r - 11) * math.sin(a)
        y = c - (r - 11) * math.cos(a)
        vc.draw_text(img, txt, int(round(x - vc.text_width(txt, 1) / 2.0)), int(round(y - 2.5)),
                     (200, 30, 30) if s % 15 == 0 else (20, 20, 22), 1)
    return img


@C.texture("board_face")
def _board_face():
    return vc.swatch((14, 16, 14), 20261430)


@C.texture("floor_painted_icon")
def _floor_icon():
    img = tile()
    vc.rect(img, 12, 0, 20, 32, rgba((60, 60, 60)))
    return img


@C.texture("floor_anchor_icon")
def _floor_anchor_icon():
    img = _floor_icon()
    vc.rect(img, 11, 11, 21, 21, rgba((240, 180, 30)))
    return img


# ------------------------------------------------------------------------------------------
# Tile, deck, gutter and the floor's line art
# ------------------------------------------------------------------------------------------
def tinted_cube(tex):
    """A full block of one texture, every face tinted (a wall shows its sides)."""
    return vc.model({"all": tex, "particle": tex},
                    [{"from": [0, 0, 0], "to": [16, 16, 16],
                      "faces": {n: {"texture": "#all", "cullface": n, "tintindex": 0}
                                for n in ("north", "south", "east", "west", "up", "down")}}])


C.add("pool_tile",
      'new BlockSportSurface("pool_tile", BlockArenaFloorBase.Surface.TILE, true, 15)',
      ("Pool Tile", "Beckenfliese", "Azulejo de Piscina", "Bassängkakel"),
      {"pool_tile": tinted_cube(T("pool_tile"))},
      {"forge_marker": 1, "defaults": {"model": M("pool_tile")},
       "variants": {"inventory": [{}], "colour": {str(k): {} for k in range(16)}}}, tab=TAB)
C.blocks[-1]["models"]["pool_tile"]["elements"][0]["faces"] = {
    f: dict(v, tintindex=0) for f, v in
    C.blocks[-1]["models"]["pool_tile"]["elements"][0]["faces"].items()} \
    if "elements" in C.blocks[-1]["models"]["pool_tile"] else None
C.add("pool_deck",
      'new BlockSportSurface("pool_deck", BlockArenaFloorBase.Surface.TILE, true, 8)',
      ("Pool Deck Tile (Non-Slip)", "Beckenumgang-Fliese (rutschfest)",
       "Baldosa de Andén de Piscina (Antideslizante)", "Bassängdäck-Kakel (Halkfritt)"),
      {"pool_deck": tinted_cube(T("deck"))},
      {"forge_marker": 1, "defaults": {"model": M("pool_deck")},
       "variants": {"inventory": [{}], "colour": {str(k): {} for k in range(16)}}}, tab=TAB)
C.add("pool_gutter",
      'new BlockSportSurface("pool_gutter", BlockArenaFloorBase.Surface.TILE, false)',
      ("Pool Gutter (Deck Level)", "Überlaufrinne (Beckenrand)", "Rebosadero de Piscina",
       "Bräddavlopp"),
      {"pool_gutter": vc.model({"top": T("gutter_grate"), "side": T("pool_tile_white"),
                                "particle": T("pool_tile_white")},
                               [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": dict(
                                   {"up": {"texture": "#top", "cullface": "up"},
                                    "down": {"texture": "#side", "cullface": "down"}},
                                   **{n: {"texture": "#side", "cullface": n}
                                      for n in ("north", "south", "east", "west")})}])},
      {"forge_marker": 1, "defaults": {"model": M("pool_gutter")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)
C.add("pool_floor_anchor",
      'new BlockLineAnchor("pool_floor_anchor", BlockArenaFloorBase.Surface.TILE, LineArtFamily.POOL)',
      ("Pool Floor Marking Anchor", "Beckenboden-Markierungsanker",
       "Ancla de Marcado del Fondo de Piscina", "Bassängbottnens Markeringsankare"),
      {"pool_floor_anchor": gc.cube(T("floor_anchor_icon"), T("pool_tile_white"))},
      {"forge_marker": 1, "defaults": {"model": M("pool_floor_anchor")},
       "variants": {"inventory": [{}], "rot": {str(k): {} for k in range(4)}}}, tab=TAB)
C.add("pool_floor_painted",
      'new BlockLinePainted("pool_floor_painted", BlockArenaFloorBase.Surface.TILE, LineArtFamily.POOL)',
      ("Pool Floor Tile (Lane Lines)", "Beckenbodenfliese (Bahnmarkierung)",
       "Azulejo del Fondo de Piscina (Líneas de Calle)", "Bassängbottenkakel (Banlinjer)"),
      {"pool_floor_painted": gc.cube(T("floor_painted_icon"), T("pool_tile_white"))},
      {"forge_marker": 1, "defaults": {"model": M("pool_floor_painted")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)


# ------------------------------------------------------------------------------------------
# End wall tiles: targets and touch pads (BlockSportFixture, full blocks)
# ------------------------------------------------------------------------------------------
def wall_tile(front):
    t = {"front": T(front), "side": T("pool_tile_white"), "particle": T("pool_tile_white")}
    faces = {n: {"texture": "#side", "cullface": n} for n in ("south", "east", "west", "up",
                                                              "down")}
    faces["north"] = {"texture": "#front", "cullface": "north"}
    return vc.model(t, [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}])


TARGET_MODELS = {"wall_target_%d" % v: wall_tile("target_%d_%d" % (4 if v & 1 == 0 else 12,
                                                                   v >> 1)) for v in range(4)}
C.add("pool_wall_target",
      'new BlockSportFixture("pool_wall_target", BlockSportFixture.Kind.WALL_TARGET)',
      ("Pool Wall Tile (Lane Target)", "Beckenwandfliese (Zielmarkierung)",
       "Azulejo de Pared de Piscina (Marca de Llegada)", "Bassängväggkakel (Målmarkering)"),
      TARGET_MODELS,
      {"forge_marker": 1, "defaults": {"model": M("wall_target_2")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(v): {"model": M("wall_target_%d" % v)} for v in range(4)},
                    "inventory": [{"model": M("wall_target_2")}]}}, tab=TAB)
C.add("pool_touchpad",
      'new BlockSportFixture("pool_touchpad", BlockSportFixture.Kind.TOUCHPAD)',
      ("Pool Touch Pad", "Anschlagplatte", "Placa de Toque", "Tidtagningsplatta"),
      {"touchpad_%d" % v: wall_tile("pad_%d" % v) for v in range(4)},
      {"forge_marker": 1, "defaults": {"model": M("touchpad_0")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(v): {"model": M("touchpad_%d" % v)} for v in range(4)},
                    "inventory": [{"model": M("touchpad_0")}]}}, tab=TAB)


# ------------------------------------------------------------------------------------------
# Lane rope: floats at the water line, hanging from the air block into the water block
# ------------------------------------------------------------------------------------------
ROPE_Y = -2.0            # the water line, in the rope block's frame (the water's top is 14 px)


def rope(cx):
    els = [box([cx - 0.15, ROPE_Y - 0.15, 0], [cx + 0.15, ROPE_Y + 0.15, 16], "cable",
               faces=("east", "west", "up", "down"))]
    for k in range(8):
        z0 = k * 2 + 0.2
        els += vc.lsc._octagon("z", cx, ROPE_Y, 1.25, z0, z0 + 1.6, "float", True)
    return els


ROPE_COLOURS = ["blue", "green", "yellow", "red"]
ROPE_TEX = {"cable": T("cable"), "float": T("blue"), "particle": T("blue")}
C.add("pool_lane_rope", 'new BlockSportFixture("pool_lane_rope", BlockSportFixture.Kind.LANE_ROPE)',
      ("Lane Rope", "Schwimmleine", "Corchera", "Banlina"),
      {"lane_rope_mid": vc.model(ROPE_TEX, rope(8.0)), "lane_rope_edge": vc.model(ROPE_TEX,
                                                                                    rope(0.0)),
       "lane_rope_item": dict(vc.model(ROPE_TEX, rope(8.0)), display=vu.icon_display(1.0, 4))},
      {"forge_marker": 1, "defaults": {"model": M("lane_rope_mid")},
       "variants": {"facing": {"north": {}, "east": {"y": 90},
                               "south": {"model": M("lane_rope_edge")},
                               "west": {"model": M("lane_rope_edge"), "y": 90}},
                    "variant": {str(k): {"textures": {"float": T(c), "particle": T(c)}}
                                for k, c in enumerate(ROPE_COLOURS)},
                    "inventory": [{"model": M("lane_rope_item")}]}}, tab=TAB)


# ------------------------------------------------------------------------------------------
# Backstroke flags: a run (BlockArenaRun), the line 1.8 m over the water on the block's back edge
# ------------------------------------------------------------------------------------------
FLAG_Y = 10.8           # the line, 1.8 m over the water (14 px into the water block, a block
#                         under this one's floor)
FLAG_TEX = {"flags": T("flags"), "cord": T("cable"), "pole": T("white"), "cap": T("red"),
            "particle": T("white")}


def flag_parts():
    return [box([0, FLAG_Y - 0.15, 15.85], [16, FLAG_Y + 0.15, 16.15], "cord",
                faces=("north", "south", "up", "down")),
            {"from": [0, FLAG_Y - 6, 16], "to": [16, FLAG_Y, 16],
             "faces": {"north": {"texture": "#flags", "uv": [0, 0, 16, 6]},
                       "south": {"texture": "#flags", "uv": [16, 0, 0, 6]}}}]


def flag_end():
    return [wbox([0, -16, 15.3], [1.4, FLAG_Y + 2.6, 16.7], "pole"),
            wbox([-0.2, FLAG_Y + 2.6, 15.1], [1.6, FLAG_Y + 3.4, 16.9], "cap")]


C.add("pool_backstroke_flags",
      'new BlockArenaRun("pool_backstroke_flags", BlockArenaRun.Kind.BACKSTROKE_FLAGS)',
      ("Backstroke Flags", "Rückenschwimm-Wendeleine", "Banderines de Espalda",
       "Ryggsimsflaggor"),
      gc.run_models("backstroke_flags", flag_parts(), flag_end(), FLAG_TEX), None, tab=TAB)


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


C.blocks[-1]["blockstate"] = run_state("backstroke_flags")
C.blocks[-1]["item"] = {"parent": "csm:block/venues/pool/backstroke_flags_item",
                        "display": vu.icon_display(0.6, -3)}


# ------------------------------------------------------------------------------------------
# Starting blocks: numbered by metadata, their facing and quarter read from the world
# ------------------------------------------------------------------------------------------
SB_TEX = {"top": T("nonslip"), "steel": T("steel"), "black": T("black"), "plate": T("num_1"),
          "particle": T("steel")}


def starting_block(cx):
    els = [box([cx - 4, 10.4, 0.6], [cx + 4, 12, 10], "top"),           # the platform
           box([cx - 4, 12, 7.6], [cx + 4, 13.4, 9.6], "black"),         # the adjustable back plate
           box([cx - 3.6, 0, 1], [cx - 2.8, 10.4, 1.8], "steel"),
           box([cx + 2.8, 0, 1], [cx + 3.6, 10.4, 1.8], "steel"),
           box([cx - 3.6, 0, 8.6], [cx - 2.8, 10.4, 9.4], "steel"),
           box([cx + 2.8, 0, 8.6], [cx + 3.6, 10.4, 9.4], "steel"),
           box([cx - 3.6, 0, 1], [cx + 3.6, 0.8, 9.4], "steel"),
           box([cx - 3.4, 9.2, 0.2], [cx + 3.4, 9.8, 0.8], "steel"),     # backstroke grip
           box([cx - 2.6, 4.4, 0.7], [cx + 2.6, 8.8, 1], "plate", faces=("north",)),
           box([cx - 4.05, 4.4, 3], [cx - 4, 8.8, 7.4], "plate", faces=("west",)),
           box([cx + 4, 4.4, 3], [cx + 4.05, 8.8, 7.4], "plate", faces=("east",))]
    for e in els:
        for f in e["faces"].values():
            if f["texture"] == "#plate":
                f["uv"] = [0, 0, 16, 16]
    return els


SB_MODELS = {}
for _side, _cx in (("l", 4.0), ("r", 12.0)):
    SB_MODELS["starting_block_" + _side] = vc.model(SB_TEX, starting_block(_cx))
    for _num in range(10):
        SB_MODELS["starting_block_%s_%d" % (_side, _num)] = {
            "parent": "csm:block/venues/pool/starting_block_" + _side,
            "textures": {"plate": T("num_%d" % _num)}}


def sb_variants():
    out = {}
    for facing, y in FACINGS:
        for n in range(10):
            # a lane's middle is a quarter of a block from the low-coordinate edge for an odd
            # lane, three quarters for an even one; the model's left (x 4) is that low edge
            # facing north or east, and the high edge facing south or west
            low = facing in ("north", "east")
            side = "l" if (n % 2 == 1) == low else "r"
            out["facing=%s,number=%d" % (facing, n)] = dict(
                {"model": M("starting_block_%s_%d" % (side, n))}, **ys(y))
    return out


C.add("pool_starting_block", 'new BlockPoolStartingBlock("pool_starting_block")',
      ("Starting Block (Pool)", "Startblock (Schwimmbecken)", "Poyete de Salida",
       "Startpall"), SB_MODELS, {"variants": sb_variants()},
      item={"parent": "csm:block/venues/pool/starting_block_l_1",
            "display": vu.icon_display(0.8, 0)}, tab=TAB)


# ------------------------------------------------------------------------------------------
# Ladder and grab rails: OBJ, from the deck block above the gutter down into the water
# ------------------------------------------------------------------------------------------
LADDER_MATS = {"steel": T("steel"), "tread": T("tread")}


def ladder(treads):
    out = []
    for x in (3.0, 13.0):
        out += vu.tube((x, 0, 12), (x, 13, 12), 0.7, "steel", sides=8)
        out += vu.tube((x, 13, 12), (x, 16, 7), 0.7, "steel", caps=False, sides=8)
        out += vu.tube((x, 16, 7), (x, 13, 1), 0.7, "steel", caps=False, sides=8)
        out += vu.tube((x, 13, 1), (x, 6, -2.5), 0.7, "steel", caps=False, sides=8)
        bottom = -44 if treads else -6
        out += vu.tube((x, 6, -2.5), (x, bottom, -2.5), 0.7, "steel", sides=8)
        out += vu.box(x - 1.4, x + 1.4, 0, 0.4, 10.6, 13.4, "steel")
    if treads:
        for y in (-6, -18, -30, -42):
            out += vu.box(3.6, 12.4, y - 0.8, y, -5.5, -1.6, "tread")
    return out


U.write_obj("pool_ladder", "pool_ladder", ladder(True))
U.write_obj("pool_grab_rails", "pool_ladder", ladder(False))
U.mtl("pool_ladder", LADDER_MATS)
C.add("pool_ladder", 'new BlockSportFixture("pool_ladder", BlockSportFixture.Kind.POOL_LADDER)',
      ("Pool Ladder and Grab Rails", "Beckenleiter und Haltegriffe",
       "Escalera y Pasamanos de Piscina", "Bassängstege och Handtag"), {},
      U.obj_state("pool_ladder", 0.3, ty=0.25, extra={
          "variant": {"0": {}, "1": {"model": M("pool_grab_rails") + ".obj"}}}), tab=TAB)


# ------------------------------------------------------------------------------------------
# Lifeguard chair, springboards, platform (OBJ units)
# ------------------------------------------------------------------------------------------
LG_MATS = {"white": T("white"), "seat": T("navy"), "canvas": T("canvas"), "black": T("black")}


def lifeguard_chair():
    out = []
    for x, z0, z1 in ((2, 2, 12), (14, 2, 12)):
        out += vu.tube((x, 0, z0 - 2), (x, 30, z0 + 3), 0.8, "white", sides=8)
        out += vu.tube((x, 0, z1 + 2), (x, 30, z1 - 1), 0.8, "white", sides=8)
    for y in (6, 12, 18, 24):
        z = -2 + 5 * y / 30.0
        out += vu.tube((2, y, z), (14, y, z), 0.5, "white", sides=6)
    out += vu.box(1.5, 14.5, 30, 31.5, 2, 13, "seat")
    out += vu.box(1.5, 14.5, 31.5, 46, 12, 13.2, "seat")
    for x in (1.5, 14.5):
        out += vu.tube((x, 31.5, 3), (x, 38, 3), 0.5, "white", sides=6)
        out += vu.tube((x, 38, 3), (x, 38, 12), 0.5, "white", sides=6)
    out += vu.tube((8, 46, 12.6), (8, 62, 12.6), 0.5, "white", sides=6)
    tip = (8, 66, 8)
    ring = [(8 + 14 * math.cos(math.radians(45 * k + 22.5)), 60,
             8 + 14 * math.sin(math.radians(45 * k + 22.5))) for k in range(8)]
    for k in range(8):
        a, b = ring[k], ring[(k + 1) % 8]
        mid = ((a[0] + b[0]) / 2 - 8, 0.6, (a[2] + b[2]) / 2 - 8)
        out.append(vc.Poly([a, b, tip], "canvas", mid, [(0, 1), (1, 1), (0.5, 0)]))
        out.append(vc.Poly([tip, b, a], "canvas", (-mid[0], -1, -mid[2]),
                           [(0.5, 0), (1, 1), (0, 1)]))
    return out


U.write_obj("lifeguard_chair", "lifeguard_chair", lifeguard_chair())
U.mtl("lifeguard_chair", LG_MATS)
C.add("pool_lifeguard_chair",
      vu.unit_java("BlockVenueUnit", "pool_lifeguard_chair",
                   [(1, 0, 0, 15, 31, 14), (1, 31, 10, 15, 46, 14), (7, 46, 7, 9, 62, 9)]),
      ("Lifeguard Chair", "Rettungsschwimmer-Hochsitz", "Silla de Socorrista",
       "Livräddarstol"), {},
      U.obj_state("lifeguard_chair", 0.24, ty=-0.35), tab=TAB)

BOARD_MATS = {"board": T("board"), "tread": T("nonslip"), "steel": T("steel"),
              "concrete": T("concrete"), "blue": T("blue"), "white": T("white")}
BOARD_TIP = -24.0
BOARD_LEN = 78.0


def springboard(top):
    """A 4.88 m springboard over the water, its tip 1.5 m past the pool's edge (the master's
    front), its top ``top`` px over the master's floor; the stand beneath its back half."""
    z0, z1 = BOARD_TIP, BOARD_TIP + BOARD_LEN
    out = vu.box(4, 12, top - 1.4, top, z0, z1, "board", faces="nsewd")
    out += vu.box(4, 12, top - 0.01, top, z0, z1, "tread", faces="u")
    if top < 20:
        out += vu.box(3, 13, 0, top - 1.4, 10, 18, "blue")             # the fulcrum
        out += vu.box(3.5, 12.5, 0, top - 1.4, 46, 52, "blue")          # the hinge stand
        return out
    out += vu.box(2, 14, 0, top - 1.4, 18, 54, "concrete")
    for x in (1.2, 14.8):
        out += vu.tube((x, top, 22), (x, top + 16, 22), 0.5, "steel", sides=6)
        out += vu.tube((x, top, 54), (x, top + 16, 54), 0.5, "steel", sides=6)
        out += vu.tube((x, top + 16, 22), (x, top + 16, 54), 0.5, "steel", caps=False, sides=6)
    for k in range(0, int(top // 6) + 1):
        y = min(top, k * 6.0)
        out += vu.box(4, 12, y - 0.6, y, 54, 58, "steel")
    for x in (3.6, 12.4):
        out += vu.tube((x, 0, 58), (x, top + 14, 58), 0.5, "steel", sides=6)
    return out


PLATFORM_TOP = (5.0 - 0.125) * 16          # 5 m over the water, the water's top 2 px below


def platform():
    """The 5 m platform: a slab 2 m wide and 6 m long on a concrete column over the master, its
    tip 2.25 m in front of the master (1.25 m past the pool's edge with the master a block back
    from the gutter), a ladder at its back."""
    t = PLATFORM_TOP
    out = vu.box(-8, 24, t - 3, t, -36, 60, "concrete", faces="nsewd")
    out += vu.box(-8, 24, t - 0.01, t, -36, 60, "tread", faces="u")
    out += vu.box(-4, 20, 0, t - 3, -2, 36, "concrete")
    for x in (-7.4, 23.4):
        for z in (-12, 12, 36, 59):
            out += vu.tube((x, t, z), (x, t + 16, z), 0.5, "steel", sides=6)
        out += vu.tube((x, t + 16, -12), (x, t + 16, 59), 0.5, "steel", caps=False, sides=6)
    for k in range(0, int(t // 6) + 1):
        out += vu.box(4, 12, k * 6.0 - 0.6, k * 6.0, 60, 64, "steel")
    for x in (3.6, 12.4):
        out += vu.tube((x, 0, 64), (x, t + 14, 64), 0.5, "steel", sides=6)
    return out


U.write_obj("springboard_1m", "diving", springboard(14.0))
U.write_obj("springboard_3m", "diving", springboard((3.0 - 0.125) * 16))
U.write_obj("platform_5m", "diving", platform())
U.mtl("diving", BOARD_MATS)
for reg, obj, top, names in (
        ("pool_springboard_1m", "springboard_1m", 14.0,
         ("1 m Springboard", "1-m-Sprungbrett", "Trampolín de 1 m", "1 m Svikt")),
        ("pool_springboard_3m", "springboard_3m", (3.0 - 0.125) * 16,
         ("3 m Springboard", "3-m-Sprungbrett", "Trampolín de 3 m", "3 m Svikt"))):
    boxes = [(4, int(top) - 2, int(BOARD_TIP), 12, int(math.ceil(top)), int(BOARD_TIP + BOARD_LEN))]
    if top < 20:
        boxes += [(3, 0, 10, 13, int(top) - 2, 18), (4, 0, 46, 12, int(top) - 2, 52)]
    else:
        boxes += [(2, 0, 18, 14, int(top) - 2, 54), (3, 0, 54, 13, int(top) + 14, 58)]
    C.add(reg, vu.unit_java("BlockVenueUnit", reg, boxes), names, {},
          U.obj_state(obj, 0.12 if top < 20 else 0.1, ty=-0.1, tz=0.3), tab=TAB)
_t = int(PLATFORM_TOP)
C.add("pool_dive_platform",
      vu.unit_java("BlockVenueUnit", "pool_dive_platform",
                   [(-8, _t - 3, -36, 24, _t, 60), (-4, 0, -2, 20, _t - 3, 36),
                    (3, 0, 60, 13, _t + 14, 65)]),
      ("5 m Diving Platform", "5-m-Sprungturm", "Plataforma de Saltos de 5 m",
       "5 m Hopptorn"), {},
      U.obj_state("platform_5m", 0.06, ty=-0.3, tz=0.3), tab=TAB)


# ------------------------------------------------------------------------------------------
# Pace clock (BlockPaceClock, hands by renderer) and scoreboard (BlockVenueSign)
# ------------------------------------------------------------------------------------------
DIAL_WALL = (8.0, 8.0, 12.0, 6.5)           # TileEntityPaceClockRenderer.DIAL_WALL (z its front)
DIAL_STAND = (8.0, 9.5, 7.0, 6.5)
PC_TEX = {"dial": T("dial"), "case": T("black"), "steel": T("steel"), "particle": T("black")}


def pace_clock(stand):
    cx, cy, z, r = DIAL_STAND if stand else DIAL_WALL
    lo, hi = cy - r - 0.5, cy + r + 0.5
    els = [box([cx - r - 0.5, lo, z], [cx + r + 0.5, hi, z + 2.5], "case",
               faces=("south", "east", "west", "up", "down")),
           {"from": [cx - r - 0.5, lo, z], "to": [cx + r + 0.5, hi, z],
            "faces": {"north": {"texture": "#dial", "uv": [0, 0, 16, 16]}}}]
    if not stand:
        els.append(box([cx - r - 0.5, lo, z + 2.5], [cx + r + 0.5, hi, 16], "case",
                       faces=("east", "west", "up", "down")))
    else:
        for x in (3.0, 13.0):
            p = box([x - 0.5, 0, 6], [x + 0.5, lo + 0.5, 7], "steel")
            els.append(p)
            q = box([x - 0.5, 0, 8.5], [x + 0.5, lo, 9.5], "steel")
            q["rotation"] = {"origin": [x, lo, 9.5], "axis": "x", "angle": -22.5}
            els.append(q)
    return els


C.add("pool_pace_clock", 'new BlockPaceClock("pool_pace_clock")',
      ("Pace Clock", "Trainingsuhr (Schwimmen)", "Reloj de Ritmo", "Tempoklocka"),
      {"pace_clock_wall": vc.model(PC_TEX, pace_clock(False)),
       "pace_clock_stand": vc.model(PC_TEX, pace_clock(True))},
      vc.facing_state(M("pace_clock_wall"), {"stand": {"true": {"model": M("pace_clock_stand")},
                                                       "false": {}}}),
      item={"parent": "csm:block/venues/pool/pace_clock_stand",
            "display": vu.icon_display(0.8, 0)}, tab=TAB)

BOARD_FACE = (1, 63, 17, 47, 6)             # TileEntityVenueSignRenderer.BOARD_FACE
SBD_MATS = {"face": T("board_face"), "case": T("navy"), "steel": T("steel")}


def scoreboard():
    """The board four blocks wide on two posts: OBJ, since it is wider than a JSON element may
    reach. Its face is dark; the words are the renderer's."""
    x0, x1, y0, y1, z = BOARD_FACE
    out = vu.box(x0 - 1, x1 + 1, y0 - 1, y1 + 1, z, z + 4, "case", faces="sewud")
    out += vu.box(x0 - 1, x1 + 1, y0 - 1, y0, z - 0.6, z, "case", faces="nud")
    out += vu.box(x0 - 1, x1 + 1, y1, y1 + 1, z - 0.6, z, "case", faces="nud")
    out += vu.box(x0 - 1, x0, y0, y1, z - 0.6, z, "case", faces="new")
    out += vu.box(x1, x1 + 1, y0, y1, z - 0.6, z, "case", faces="new")
    out += vu.box(x0, x1, y0, y1, z, z + 0.01, "face", faces="n")
    for x in (8, 56):
        out += vu.box(x - 1, x + 1, 0, y0 - 1, z + 1, z + 3, "steel")
        out += vu.box(x - 3, x + 3, 0, 0.6, z - 1, z + 5, "steel")
    return out


U.write_obj("pool_scoreboard", "pool_scoreboard", scoreboard())
U.mtl("pool_scoreboard", SBD_MATS)
C.add("pool_scoreboard",
      vu.unit_java("BlockVenueSign", "pool_scoreboard",
                   [(7, 0, 7, 9, 16, 9), (55, 0, 7, 57, 16, 9), (0, 16, 5, 64, 48, 10)],
                   ", BlockVenueSign.Style.POOL_BOARD"),
      ("Pool Scoreboard", "Schwimm-Anzeigetafel", "Marcador de Natación", "Simresultattavla"),
      {}, U.obj_state("pool_scoreboard", 0.2, ty=-0.2, tx=-0.3), tab=TAB)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
for _i, _n in enumerate((("Blue", "Blau", "Azul", "Blå"), ("Green", "Grün", "Verde", "Grön"),
                         ("Yellow", "Gelb", "Amarillo", "Gul"), ("Red", "Rot", "Rojo", "Röd"))):
    L("csm.venues.pool.rope.%d" % _i, _n)
for _k, _n in {"pool_25m": ("25 m pool, 8 lanes", "25-m-Becken, 8 Bahnen",
                            "Piscina de 25 m, 8 calles", "25 m bassäng, 8 banor"),
               "pool_50m": ("50 m pool, 10 lanes", "50-m-Becken, 10 Bahnen",
                            "Piscina de 50 m, 10 calles", "50 m bassäng, 10 banor"),
               "diving_well": ("Diving well", "Sprungbecken", "Foso de saltos",
                               "Hoppbassäng")}.items():
    L("csm.venues.lines." + _k, _n)
for _i, _n in enumerate((("Event", "Wettbewerb", "Prueba", "Gren"),
                         ("Heat", "Lauf", "Serie", "Heat"),
                         ("Swimmers by lane (commas)", "Schwimmer nach Bahn (Kommas)",
                          "Nadadores por calle (comas)", "Simmare per bana (kommatecken)"),
                         ("Times by lane", "Zeiten nach Bahn", "Tiempos por calle",
                          "Tider per bana"),
                         ("Places by lane", "Platzierungen nach Bahn", "Puestos por calle",
                          "Placeringar per bana"),
                         ("Race clock", "Rennuhr", "Reloj de carrera", "Tävlingsklocka"))):
    L("gui.csm.venue_text.pool.%d" % _i, _n)
L("gui.csm.venue_text.choice.colour", ("Colour: %s", "Farbe: %s", "Color: %s", "Färg: %s"))


# ------------------------------------------------------------------------------------------
# The pools' floors as line art, and the layouts
# ------------------------------------------------------------------------------------------
LANE = 2.5
LINE_W = 0.25
S_TILE = 15          # SurfaceColours light grey: the tile


class Pool(object):
    """A pool: its length, its lanes (first and last number), and the space outside them."""

    def __init__(self, name, length, first, last, space, depth):
        self.name, self.length, self.first, self.last = name, length, first, last
        self.space, self.depth = space, depth
        self.lanes = last - first + 1
        self.width = int(round(self.lanes * LANE + 2 * space))

    def centre(self, n):
        """Lane n's middle, x in the frame (the start end south, at z = length; swimmers face
        north, so lanes count from the east side, x = width)."""
        return self.width - self.space - LANE * (n - self.first + 0.5)

    def boundaries(self):
        """The x of every lane rope: between lanes, and between the outside lanes and their
        spaces where there are spaces."""
        out = []
        for k in range(self.lanes + 1):
            x = self.width - self.space - LANE * k
            if 0 < x < self.width:
                out.append(x)
        return out


POOLS = [Pool("pool_25m", 25, 1, 8, 0.5, 3), Pool("pool_50m", 50, 0, 9, 0.0, 3),
         Pool("diving_well", 12, 0, -1, 0.0, 5)]
POOLS[2].width = 12


def floor_layout(p):
    ops = []
    for n in range(p.first, p.last + 1):
        xc = p.centre(n)
        ops.append(vl.Op("LINE", [vl.rect(xc - LINE_W / 2, 2.0, xc + LINE_W / 2, p.length - 2.0)]))
        for z in (2.0, p.length - 2.0 - LINE_W):
            ops.append(vl.Op("LINE", [vl.rect(xc - 0.5, z, xc + 0.5, z + LINE_W)]))
    if not ops:
        ops.append(vl.Op("SURFACE", [vl.rect(0, 0, p.width, p.length)]))
    return vl.Layout(p.name, "SURFACE", ops, (0, 0, p.width - 1, p.length - 1),
                     (S_TILE, S_TILE, LANE_BLACK, 0))


LAYOUTS = [floor_layout(p) for p in POOLS]

JAVA = {os.path.join(gc.JAVA_DIR, "PoolLayouts.java"): vl.layouts_java(
    GEN, "PoolLayouts",
    "The pools' floors: the 25 m pool's eight lanes and the 50 m pool's ten, each lane's line "
    "ending in a T 2 m from the end walls, and the diving well's plain floor "
    "({@code gen_venues_pool.py}).", LAYOUTS),
    os.path.join(gc.TEST_DIR, "PoolSamples.java"): gc.samples_file(
        "PoolSamples", "POOL", gc.samples_java("POOL", LAYOUTS, every=5), GEN)}


# ------------------------------------------------------------------------------------------
# --layout: the whole pool, as cells at levels relative to the deck (dy 0 the deck and gutter)
# ------------------------------------------------------------------------------------------
FACE_IDX = {"south": 0, "west": 1, "north": 2, "east": 3}
DECK = 3


def frac_u(f, facing):
    """The texture u (of 16) a lane's middle is at on a wall tile facing ``facing`` (into the
    pool), the middle a fraction ``f`` of a block from the low-coordinate edge."""
    low = facing in ("north", "east")
    x = 16 * f if low else 16 * (1 - f)       # the model's x
    return 16 - x                              # a north face's u runs from x = 16


def pool_cells(p, pads=True):
    """{(i, dy, j): (block, meta, nbt)} in the frame: the start end south, swimmers facing
    north. dy 0 is the deck."""
    W, Lg, D = p.width, p.length, p.depth
    lay = LAYOUTS[POOLS.index(p)]
    out = {}
    lid = POOLS.index(p)
    # the floor
    pics = set(lay.pictures())
    anchors = set(lay.anchors())
    for i in range(W):
        for j in range(Lg):
            if (i, j) in anchors:
                out[(i, -D, j)] = ("pool_floor_anchor", 0, "{lay:%d,ax:%d,az:%d}" % (lid, i, j))
            elif (i, j) in pics:
                out[(i, -D, j)] = ("pool_floor_painted", 0, None)
            else:
                out[(i, -D, j)] = ("pool_tile", S_TILE, None)
            for dy in range(-D + 1, 1):
                out[(i, dy, j)] = ("water", 0, None)
    # walls, the gutter ring and the deck
    for i in range(-1 - DECK, W + 1 + DECK):
        for j in range(-1 - DECK, Lg + 1 + DECK):
            if 0 <= i < W and 0 <= j < Lg:
                continue
            ring = -1 <= i <= W and -1 <= j <= Lg
            out[(i, -D, j)] = ("pool_tile", S_TILE, None)
            for dy in range(-D + 1, 0):
                out[(i, dy, j)] = ("pool_tile", S_TILE, None)
            out[(i, 0, j)] = ("pool_gutter", 0, None) if ring else ("pool_deck", 8, None)
    # the end walls' lane marks: targets at the turn end (north, j = -1, facing south into the
    # pool), touch pads over targets at the start end (south, j = Lg, facing north)
    for n in range(p.first, p.last + 1):
        xc = p.centre(n)
        i = int(math.floor(xc))
        f = xc - i
        for j, facing in ((-1, "south"), (Lg, "north")):
            u = frac_u(f, facing)
            side = 0 if abs(u - 4) < 1e-6 else 1
            for dy in range(-D + 1, 0):
                top = dy == -1
                out[(i, dy, j)] = ("pool_wall_target", FACE_IDX[facing] | side << 2
                                   | (1 << 3 if top else 0), None)
    if pads and p.lanes > 0:
        for j, facing in ((Lg, "north"),):
            for i in range(W):
                spans = []
                for n in range(p.first, p.last + 1):
                    a = p.centre(n) - 1.2 - i
                    b = p.centre(n) + 1.2 - i
                    lo, hi = max(0.0, a), min(1.0, b)
                    if hi - lo > 1e-6:
                        spans.append((lo, hi))
                if not spans:
                    continue
                # in u, as the swimmer sees the wall
                us = sorted((frac_u(hi, facing), frac_u(lo, facing)) if facing in ("north", "east")
                            else (frac_u(lo, facing), frac_u(hi, facing)) for lo, hi in spans)
                us = [(min(a, b), max(a, b)) for a, b in us]
                # pads are 2.4 m on 2.5 m lanes, so a pad's 10 cm gap falls in a block's middle
                # (piece 1) or at its edge (pieces 2 and 3); a pad ending in a 0.5 m side space
                # is drawn as piece 1, its far half over the side space
                if len(us) > 1 or us[0][0] >= 6 or us[0][1] <= 10:
                    piece = 1
                elif us[0][0] > 0.4:
                    piece = 2
                elif us[0][1] < 15.6:
                    piece = 3
                else:
                    piece = 0
                out[(i, -1, j)] = ("pool_touchpad", FACE_IDX[facing] | piece << 2, None)
    return out


def rope_meta(x, colour, along_z=True):
    """A lane rope at frame x (running along z): the cell and the metadata."""
    i = int(math.floor(x + 1e-9))
    edge = abs(x - i) < 1e-6
    facing = ("south" if edge else "north") if along_z else ("west" if edge else "east")
    return i, FACE_IDX[facing] | colour << 2


def pool_objects(p):
    """(i, dy, j, block, meta) above the deck and water."""
    out = []
    W, Lg = p.width, p.length
    if p.lanes > 0:
        bounds = p.boundaries()
        for k, x in enumerate(bounds):
            off = abs(k - (len(bounds) - 1) / 2.0)
            colour = 2 if off < 1.5 else 0 if off < 3.0 else 1
            for j in range(Lg):
                c = 3 if (j < 5 or j >= Lg - 5) else colour
                i, m = rope_meta(x, c)
                out.append((i, 1, j, "pool_lane_rope", m))
        # backstroke flags 5 m from each end wall: the line on the block's back edge
        for j, facing in ((4, "north"), (Lg - 5, "south")):
            for i in range(-1, W + 1):
                out.append((i, 2, j, "pool_backstroke_flags", FACE_IDX[facing]))
        for n in range(p.first, p.last + 1):
            i = int(math.floor(p.centre(n)))
            out.append((i, 1, Lg, "pool_starting_block", n))
            out.append((i, 1, -1, "pool_starting_block", n))
        out.append((-1, 1, 1, "pool_ladder", FACE_IDX["east"]))
        out.append((W, 1, 1, "pool_ladder", FACE_IDX["west"]))
        out.append((-1, 1, Lg - 2, "pool_ladder", FACE_IDX["east"]))
        out.append((W, 1, Lg - 2, "pool_ladder", FACE_IDX["west"]))
        out.append((W + 2, 1, Lg // 2, "pool_lifeguard_chair", FACE_IDX["west"]))
        out.append((W + 1, 1, -3, "pool_pace_clock", FACE_IDX["south"] | 4))
        # facing south the board runs from its master a block east to three west
        out.append((W // 2 + 1, 1, -4, "pool_scoreboard", FACE_IDX["south"]))
    else:
        out.append((3, 1, Lg, "pool_springboard_1m", FACE_IDX["north"]))
        out.append((8, 1, Lg, "pool_springboard_3m", FACE_IDX["north"]))
        out.append((W // 2 - 1, 1, -2, "pool_dive_platform", FACE_IDX["south"]))
        out.append((W, 1, 2, "pool_ladder", FACE_IDX["west"]))
    return out


def turn_meta(block, meta, rot):
    if block in ("pool_tile", "pool_deck", "pool_gutter", "pool_floor_painted", "water",
                 "pool_starting_block"):
        return meta
    if block == "pool_floor_anchor":
        return (meta + rot) & 3
    if block == "pool_lane_rope":
        f = meta & 3
        # turned a quarter clockwise, a rope along z (north, through the middle; south, on the
        # west edge) runs along x (east through the middle; west on the north edge) and back
        if rot & 1:
            f = {2: 3, 0: 1, 3: 2, 1: 0}[f]
        return (meta & ~3) | f
    return (meta & ~3) | (((meta & 3) + rot) % 4)


def commands(name, x0, y0, z0, rot=0):
    p = next(q for q in POOLS if q.name == name)
    rot &= 1          # the start end faces north or east (see the module comment)
    cells = pool_cells(p)
    out = []
    lines = {}
    for (i, dy, j), (b, m, nbt) in cells.items():
        wx, wz = gc.to_world(i, j, rot)
        lines[(wx, dy, wz)] = (b, turn_meta(b, m, rot), nbt)
    # clear the volume above the deck first
    xs = [k[0] for k in lines]
    zs = [k[2] for k in lines]
    out.append("fill %d %d %d %d %d %d air" % (x0 + min(xs), y0 + 1, z0 + min(zs), x0 + max(xs),
                                               y0 + 8, z0 + max(zs)))
    for dy in sorted(set(k[1] for k in lines)):
        rows = {}
        for (wx, d, wz), v in lines.items():
            if d == dy:
                rows.setdefault(wz, []).append((wx, v))
        for wz in sorted(rows):
            row = sorted(rows[wz])
            k = 0
            while k < len(row):
                wx, (b, m, nbt) = row[k]
                q = k
                while (nbt is None and q + 1 < len(row) and row[q + 1][0] == row[q][0] + 1
                       and row[q + 1][1] == (b, m, None)):
                    q += 1
                bid = "minecraft:water" if b == "water" else "csm:" + b
                if q > k:
                    out.append("fill %d %d %d %d %d %d %s %d" % (
                        x0 + wx, y0 + dy, z0 + wz, x0 + row[q][0], y0 + dy, z0 + wz, bid, m))
                else:
                    out.append("setblock %d %d %d %s %d%s" % (
                        x0 + wx, y0 + dy, z0 + wz, bid, m, " replace " + nbt if nbt else ""))
                k = q + 1
    for (i, dy, j, b, m) in pool_objects(p):
        wx, wz = gc.to_world(i, j, rot)
        out.append("setblock %d %d %d csm:%s %d" % (x0 + wx, y0 + dy, z0 + wz, b,
                                                    turn_meta(b, m, rot)))
    return out


def layout_json(name):
    p = next(q for q in POOLS if q.name == name)
    return {"layout": name, "id": POOLS.index(p),
            "frame": "x across the pool (east), z along it, the start end south (z = length); "
                     "dy 0 the deck and gutter, the floor at dy -depth; swimmers at the start "
                     "face north, lanes numbered from the east",
            "width": p.width, "length": p.length, "depth": p.depth,
            "cells": [[i, dy, j, b, m, nbt] for (i, dy, j), (b, m, nbt) in sorted(
                pool_cells(p).items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0]))],
            "objects": [list(o) for o in pool_objects(p)]}


def main():
    if "--layout" in sys.argv:
        name = sys.argv[sys.argv.index("--layout") + 1]
        rot = int(sys.argv[sys.argv.index("--rot") + 1]) if "--rot" in sys.argv else 0
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(name, x, y, z, rot)))
        else:
            print(json.dumps(layout_json(name)))
        return 0
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(out, exist_ok=True)
        cols = {"SURFACE": (200, 214, 222), "LINE": (20, 20, 22)}
        for lay in LAYOUTS:
            print(vl.preview(lay, os.path.join(out, lay.name + ".png"), ppm=16, colours=cols))
        return 0
    if "--doc" in sys.argv:
        print(json.dumps({lay.name: {"extent": lay.extent, "painted": len(lay.pictures()),
                                     "anchors": len(lay.anchors()), "ops": len(lay.ops)}
                          for lay in LAYOUTS}, indent=1))
        return 0
    if "--check" in sys.argv:
        stale = gc.write_java(JAVA, True)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        gc.write_java(JAVA, False)
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
