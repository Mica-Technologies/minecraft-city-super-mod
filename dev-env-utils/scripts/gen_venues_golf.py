#!/usr/bin/env python3
"""
gen_venues_golf.py -- the Sports & Venues Golf tab: the course, the driving range, mini-golf.

    py -3 dev-env-utils/scripts/gen_venues_golf.py                    # write
    py -3 dev-env-utils/scripts/gen_venues_golf.py --check            # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_golf.py --fragments        # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_golf.py --layout NAME --commands X Y Z [--rot R]
                                                       # NAME: green, range, minigolf

Decor first, no ball physics. **The course**: mown turf in five cuts (putting green, collar,
fairway, rough, tee) whose light and dark stripes come from each block's position and the way
the mower ran (``golf_turf``), the cup cut into a green, the flagstick with its flag in four
colours, a raked bunker, tee markers in four colours, yardage markers, a tee sign (hole, par and
the yards from each tee, typed in: the venue text engine), a ball washer, a bench, the cart path
and its curb. **The range**: a hitting mat, the divider between bays, the ball dispenser, a
bucket of balls and the distance flags out on the range. **Mini-golf**: carpet in sixteen
colours, the cup, a timber border along any side of a block, a ramp, a windmill, a loop and a
scorecard stand. No brand.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402
import venues_units as vu  # noqa: E402
import gen_venues_courts as gc  # noqa: E402

GEN = "gen_venues_golf.py"
TAB = "Sports & Venues - Golf"
C = vc.catalogue(GEN, "golf")
T = C.T
M = C.M
U = vu.Units(C, GEN, "golf")
box = vc.box
wbox = vc.lsc.pp.wbox
FACINGS = vc.FACINGS
ys = vu.ys

WHITE = (240, 240, 236)
BLACK = (24, 24, 26)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


# ------------------------------------------------------------------------------------------
# Turf: five cuts, light and dark, and the cup
# ------------------------------------------------------------------------------------------
CUTS = [  # name, base colour, blade length (texture streak), stripe contrast
    ("green", (92, 156, 64), 1, 0.10),
    ("collar", (86, 148, 58), 2, 0.08),
    ("fairway", (84, 146, 56), 2, 0.12),
    ("rough", (62, 112, 40), 4, 0.0),
    ("tee", (88, 150, 60), 2, 0.12),
]


def turf(base, blade, dark, seed):
    """Mown grass, mower lines along the texture's v (north-south)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    k0 = 1.0 - (dark if dark else 0)
    cols = [rng.uniform(-0.05, 0.05) for _ in range(32)]
    for y in range(32):
        for x in range(32):
            k = k0 + cols[x] * 0.4 + rng.uniform(-0.07, 0.07) * (blade / 2.0 + 0.5)
            if blade >= 3 and rng.random() < 0.12:
                k -= 0.12
            px[x, y] = rgba(vc.shade(base, k))
    return img


for _c, (_name, _base, _blade, _contrast) in enumerate(CUTS):
    for _dark in (0, 1):
        C.textures["turf_%s_%d" % (_name, _dark)] = (
            lambda b=_base, bl=_blade, d=(_contrast if _dark else 0), s=20261600 + _c * 2 + _dark:
            turf(b, bl, d, s))
    C.textures["turf_%s_side" % _name] = (lambda b=_base, s=20261650 + _c: turf(
        vc.shade(b, 0.82), 2, 0, s))


def cup(dark):
    img = turf(CUTS[0][1], 1, CUTS[0][3] if dark else 0, 20261600 + dark)
    c = 16.0
    vc.disc(img, c, c, 3.6, rgba((236, 236, 232)))
    vc.disc(img, c, c, 2.8, rgba((20, 20, 18)))
    return img


C.textures["turf_cup_0"] = lambda: cup(0)
C.textures["turf_cup_1"] = lambda: cup(1)
TURF_CUT_TEX = ["green", "collar", "fairway", "rough", "tee", "cup"]

TURF_MODELS = {}
for _k, _n in enumerate(TURF_CUT_TEX):
    side = "turf_%s_side" % ("green" if _n == "cup" else _n)
    for _dark in (0, 1):
        top = "turf_%s_%d" % (_n, 0 if _n == "rough" else _dark)
        TURF_MODELS["turf_%s_%d" % (_n, _dark)] = gc.cube(T(top), T(side), bottom=T(
            "turf_rough_side"))


def turf_variants():
    out = {}
    for across in ("false", "true"):
        for k, n in enumerate(TURF_CUT_TEX):
            for dark in ("false", "true"):
                v = {"model": M("turf_%s_%d" % (n, 1 if dark == "true" else 0))}
                if across == "true":
                    v["y"] = 90
                out["across=%s,cut=%d,dark=%s" % (across, k, dark)] = v
    return out


C.add("golf_turf", 'new BlockGolfTurf("golf_turf")',
      ("Golf Turf", "Golfrasen", "Césped de Golf", "Golfgräs"), TURF_MODELS,
      {"variants": turf_variants()},
      item={"parent": "csm:block/venues/golf/turf_fairway_0"}, tab=TAB)


@C.texture("bunker")
def _bunker():
    """Raked bunker sand: furrows along the texture's v."""
    rng = random.Random(20261660)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            k = 1.0 + 0.06 * math.sin(x * math.pi / 2.0) + rng.uniform(-0.03, 0.03)
            px[x, y] = rgba(vc.shade((226, 212, 172), k))
    return img


C.add("golf_bunker_sand",
      'new BlockSportSurface("golf_bunker_sand", BlockArenaFloorBase.Surface.SAND, false)',
      ("Bunker Sand (Raked)", "Bunkersand (geharkt)", "Arena de Búnker (Rastrillada)",
       "Bunkersand (Krattad)"),
      {"bunker": gc.cube(T("bunker"), T("bunker"))},
      {"forge_marker": 1, "defaults": {"model": M("bunker")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)


@C.texture("cart_path")
def _cart_path():
    """Broom-finished concrete with a control joint across."""
    rng = random.Random(20261661)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            k = 1.0 + rng.uniform(-0.04, 0.04) + (0.03 if y % 3 == 0 else 0)
            if y == 0:
                k = 0.78
            px[x, y] = rgba(vc.shade((176, 174, 168), k))
    return img


C.add("golf_cart_path",
      'new BlockSportSurface("golf_cart_path", BlockArenaFloorBase.Surface.CONCRETE, false)',
      ("Cart Path", "Cartweg", "Camino de Carritos", "Golfbilsväg"),
      {"cart_path": gc.cube(T("cart_path"), T("cart_path"))},
      {"forge_marker": 1, "defaults": {"model": M("cart_path")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)

# ------------------------------------------------------------------------------------------
# Swatches and small textures
# ------------------------------------------------------------------------------------------
for _n, _c, _s in (("white", WHITE, 1), ("black", BLACK, 2), ("steel", (196, 200, 206), 3),
                   ("red", (200, 36, 36), 4), ("blue", (36, 84, 196), 5),
                   ("yellow", (240, 200, 30), 6), ("gold", (212, 170, 60), 7),
                   ("green_paint", (30, 96, 52), 8), ("wood", (150, 106, 64), 9),
                   ("rubber", (40, 40, 42), 10), ("grey", (128, 132, 136), 11),
                   ("concrete", (176, 174, 168), 12), ("orange", (232, 110, 30), 13),
                   ("cream", (236, 226, 196), 14), ("brick", (150, 70, 52), 15),
                   ("roof", (90, 40, 34), 16), ("sail", (236, 230, 214), 17),
                   ("navy", (26, 40, 86), 18), ("purple", (110, 60, 150), 19)):
    C.textures[_n] = (lambda c=_c, s=_s: vc.swatch(c, 20261700 + s))


@C.texture("mat")
def _mat():
    """A hitting mat's dense nylon turf."""
    return turf((70, 150, 70), 1, 0, 20261720)


@C.texture("balls")
def _balls():
    """A bucket's top: range balls, white with a stripe."""
    img = Image.new("RGBA", (16, 16), rgba((210, 210, 206)))
    rng = random.Random(20261721)
    for _k in range(18):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        vc.disc(img, x + 0.5, y + 0.5, 1.6, rgba((246, 246, 242)))
        img.load()[x, y] = rgba((200, 40, 40)) if rng.random() < 0.4 else rgba((246, 246, 242))
    return img


def number_plate(text, bg, fg):
    img = Image.new("RGBA", (16, 16), rgba(bg))
    vc.draw_text(img, text, 8 - vc.text_width(text, 1) // 2, 6, fg, 1)
    return img


YARDS = [("100", (200, 36, 36), WHITE), ("150", WHITE, BLACK), ("200", (36, 84, 196), WHITE),
         ("250", (240, 200, 30), BLACK)]
for _k, (_t, _bg, _fg) in enumerate(YARDS):
    C.textures["yards_%d" % _k] = (lambda t=_t, bg=_bg, fg=_fg: number_plate(t, bg, fg))
RANGE = [("50", (200, 36, 36)), ("100", (240, 200, 30)), ("150", (36, 84, 196)),
         ("200", WHITE)]
for _k, (_t, _bg) in enumerate(RANGE):
    C.textures["range_%d" % _k] = (lambda t=_t, bg=_bg: number_plate(
        t, bg, BLACK if bg in (WHITE, (240, 200, 30)) else WHITE))


@C.texture("scorecards")
def _scorecards():
    img = Image.new("RGBA", (16, 16), rgba((236, 226, 196)))
    for y in range(2, 14, 2):
        vc.rect(img, 2, y, 14, y + 1, rgba((120, 150, 110)))
    return img


@C.texture("windmill_door")
def _wm_door():
    img = Image.new("RGBA", (16, 16), rgba((150, 70, 52)))
    vc.rect(img, 4, 6, 12, 16, rgba((20, 20, 18)))
    return img


# ------------------------------------------------------------------------------------------
# Flagstick, tee markers, yardage markers (BlockSportFixture)
# ------------------------------------------------------------------------------------------
PIN_COLOURS = ["red", "white", "blue", "yellow"]
FLAG_TEX = {"pole": T("white"), "flag": T("red"), "black": T("black"), "particle": T("white")}


def flagstick():
    els = vc.lsc.post(8, 8, 0.32, -1.5, 31.9, "pole")
    for k in range(0, 32, 4):
        els.append(box([7.66, k, 7.66], [8.34, k + 2, 8.34], "black",
                       faces=("north", "south", "east", "west")))
    flag = {"from": [8.3, 26.2, 8], "to": [16, 31.6, 8],
            "faces": {"north": {"texture": "#flag", "uv": [0, 0, 16, 11]},
                      "south": {"texture": "#flag", "uv": [16, 0, 0, 11]}}}
    els.append(flag)
    return els


C.add("golf_flagstick", 'new BlockSportFixture("golf_flagstick", BlockSportFixture.Kind.FLAGSTICK)',
      ("Flagstick", "Flaggenstock", "Bandera de Hoyo", "Flaggstång (Golf)"),
      {"flagstick": vc.model(FLAG_TEX, flagstick())},
      {"forge_marker": 1, "defaults": {"model": M("flagstick")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(k): {"textures": {"flag": T(c)}}
                                for k, c in enumerate(PIN_COLOURS)},
                    "inventory": [{}]}},
      item=None, tab=TAB)
C.blocks[-1]["blockstate"]["variants"]["inventory"] = [{"transform": vu.obj_inventory(0.5)}]

TEE_COLOURS = ["red", "white", "blue", "gold"]
TEE_TEX = {"ball": T("red"), "black": T("black"), "particle": T("red")}


def tee_marker():
    els = vc.lsc.post(8, 8, 2.2, 0, 3.6, "ball")
    els += vc.lsc.post(8, 8, 1.2, 3.6, 4.6, "ball")
    els.append(box([7.6, -1, 7.6], [8.4, 0, 8.4], "black"))
    return els


C.add("golf_tee_marker", 'new BlockSportFixture("golf_tee_marker", BlockSportFixture.Kind.TEE_MARKER)',
      ("Tee Marker", "Abschlagmarkierung", "Marca de Salida", "Utslagsmarkering"),
      {"tee_marker": dict(vc.model(TEE_TEX, tee_marker()), display=vu.icon_display(1.4, 2))},
      {"forge_marker": 1, "defaults": {"model": M("tee_marker")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(k): {"textures": {"ball": T(c), "particle": T(c)}}
                                for k, c in enumerate(TEE_COLOURS)},
                    "inventory": [{}]}}, tab=TAB)

YD_TEX = {"plate": T("yards_0"), "post": T("white"), "particle": T("white")}


def yardage_marker():
    els = [box([7.2, 0, 7.2], [8.8, 7, 8.8], "post")]
    p = box([4.5, 6, 7.4], [11.5, 11.6, 8.6], "plate")
    for f in ("north", "south"):
        p["faces"][f]["uv"] = [0, 3, 16, 13] if f == "north" else [16, 3, 0, 13]
    els.append(p)
    return els


C.add("golf_yardage_marker",
      'new BlockSportFixture("golf_yardage_marker", BlockSportFixture.Kind.YARDAGE_MARKER)',
      ("Yardage Marker", "Entfernungsmarkierung", "Marcador de Yardas", "Avståndsmarkering"),
      {"yardage_marker": dict(vc.model(YD_TEX, yardage_marker()), display=vu.icon_display(1.1))},
      {"forge_marker": 1, "defaults": {"model": M("yardage_marker")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(k): {"textures": {"plate": T("yards_%d" % k)}}
                                for k in range(4)},
                    "inventory": [{}]}}, tab=TAB)

# ------------------------------------------------------------------------------------------
# Tee sign (BlockVenueSign, GOLF_HOLE), ball washer, bench, cart path curb
# ------------------------------------------------------------------------------------------
HOLE_FACE = (2.5, 13.5, 14.5, 29.5, 7)     # TileEntityVenueSignRenderer.HOLE_FACE
HS_TEX = {"wood": T("wood"), "green": T("green_paint"), "particle": T("green_paint")}


def hole_sign():
    x0, x1, y0, y1, z = HOLE_FACE
    return [box([7, 0, 8], [9, y0 - 0.5, 10], "wood"),
            box([x0 - 0.6, y0 - 0.6, z], [x1 + 0.6, y1 + 0.6, z + 1.6], "green"),
            box([x0 - 0.6, y1 + 0.6, z - 0.4], [x1 + 0.6, y1 + 1.4, z + 2.0], "wood")]


C.add("golf_hole_sign",
      vu.unit_java("BlockVenueSign", "golf_hole_sign", [(7, 0, 8, 9, 14, 10),
                                                         (2, 14, 6, 14, 31, 9)],
                   ", BlockVenueSign.Style.GOLF_HOLE"),
      ("Tee Sign (Hole, Par, Yards)", "Abschlagtafel (Loch, Par, Yards)",
       "Cartel de Salida (Hoyo, Par, Yardas)", "Utslagsskylt (Hål, Par, Yards)"),
      {"hole_sign": vc.model(HS_TEX, hole_sign())}, vc.facing_state(M("hole_sign")),
      item={"parent": "csm:block/venues/golf/hole_sign", "display": vu.icon_display(0.5, -2)},
      tab=TAB)

BW_TEX = {"green": T("green_paint"), "steel": T("steel"), "towel": T("white"),
          "black": T("black"), "particle": T("green_paint")}


def ball_washer():
    els = vc.lsc.post(8, 9, 0.9, 0, 10, "green")
    els += [box([5, 0, 6], [11, 0.6, 12], "black"),
            box([5.4, 10, 6.6], [10.6, 14.6, 11.4], "green"),
            box([7.2, 14.6, 7.2], [8.8, 15.4, 10.6], "steel"),
            box([5.8, 14.6, 8.4], [10.2, 15, 9.6], "black"),
            box([10.6, 11, 8.4], [11.2, 14.6, 9.6], "steel"),
            box([11.2, 13.8, 8.6], [12.6, 14.4, 9.4], "steel"),
            box([4.6, 6, 8.2], [5.4, 12, 9.8], "towel")]
    return els


C.add("golf_ball_washer", 'new BlockSportFixture("golf_ball_washer", BlockSportFixture.Kind.BALL_WASHER)',
      ("Ball Washer", "Ballwäscher", "Lavabolas", "Bolltvätt"),
      {"ball_washer": vc.model(BW_TEX, ball_washer())}, vc.facing_state(M("ball_washer")),
      item={"parent": "csm:block/venues/golf/ball_washer", "display": vu.icon_display(0.9)},
      tab=TAB)

BENCH_TEX = {"slat": T("wood"), "frame": T("green_paint"), "particle": T("wood")}


def bench_parts():
    els = [box([0, 7, z], [16, 8, z + 2.4], "slat") for z in (3.5, 6.3, 9.1)]
    els += [box([0, 9.5, 12.2], [16, 11.5, 13.2], "slat"), box([0, 12.4, 12.4], [16, 14.4, 13.4],
                                                                "slat")]
    return els


def bench_end():
    return [box([0.4, 0, 3.6], [1.6, 7, 4.8], "frame"), box([0.4, 0, 11.6], [1.6, 15, 12.8],
                                                            "frame"),
            box([0.4, 6, 3.6], [1.6, 7, 12.8], "frame"), box([0.4, 9.5, 3.6], [1.6, 10.4, 12],
                                                             "frame")]


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


C.add("golf_bench", 'new BlockArenaRun("golf_bench", BlockArenaRun.Kind.GOLF_BENCH)',
      ("Golf Course Bench", "Golfplatzbank", "Banco de Campo de Golf", "Golfbanebänk"),
      gc.run_models("golf_bench", bench_parts(), bench_end(), BENCH_TEX),
      run_state("golf_bench"),
      item={"parent": "csm:block/venues/golf/golf_bench_item",
            "display": vu.icon_display(0.625)}, tab=TAB)


def edge_rail_state(prefix, tex, t, h):
    """A rail along each side its property names, a corner post where two meet."""
    models = {}
    rules = []
    for k, side in enumerate(("north", "east", "south", "west")):
        models["%s_%s" % (prefix, side)] = vc.model(tex, [box([0, 0, 0], [16, h, t], "rail")])
        rules.append({"when": {side: "true"},
                      "apply": dict({"model": M("%s_%s" % (prefix, "north"))}, **ys(90 * k))})
    del models["%s_east" % prefix], models["%s_south" % prefix], models["%s_west" % prefix]
    models[prefix + "_item"] = dict(vc.model(tex, [box([0, 0, 6], [16, h, 6 + t], "rail")]),
                                    display=vu.icon_display(0.9, 3))
    return models, {"multipart": rules}


MG_RAIL_TEX = {"rail": T("wood"), "particle": T("wood")}
_m, _s = edge_rail_state("minigolf_border", MG_RAIL_TEX, 2.0, 3.0)
C.add("minigolf_border",
      'new BlockEdgeRail("minigolf_border", Material.WOOD, SoundType.WOOD, 2, 3)',
      ("Mini-Golf Border", "Minigolf-Bande", "Borde de Minigolf", "Minigolfsarg"), _m, _s,
      item={"parent": "csm:block/venues/golf/minigolf_border_item"}, tab=TAB)
CURB_TEX = {"rail": T("concrete"), "particle": T("concrete")}
_m, _s = edge_rail_state("cart_path_curb", CURB_TEX, 3.0, 2.5)
C.add("golf_cart_path_curb",
      'new BlockEdgeRail("golf_cart_path_curb", Material.ROCK, SoundType.STONE, 3, 2.5)',
      ("Cart Path Curb", "Cartweg-Bordstein", "Bordillo de Camino de Carritos",
       "Golfbilsvägens Kantsten"), _m, _s,
      item={"parent": "csm:block/venues/golf/cart_path_curb_item"}, tab=TAB)

# ------------------------------------------------------------------------------------------
# Driving range
# ------------------------------------------------------------------------------------------
MAT_TEX = {"mat": T("mat"), "rubber": T("rubber"), "particle": T("mat")}


def range_mat():
    return [box([0, 0, 0], [16, 1, 16], "rubber"),
            box([1, 1, 1], [15, 1.4, 15], "mat", faces=("up", "north", "south", "east", "west")),
            box([7.5, 1.4, 4.5], [8.5, 2.6, 5.5], "rubber")]


C.add("driving_range_mat", 'new BlockSportFixture("driving_range_mat", BlockSportFixture.Kind.RANGE_MAT)',
      ("Driving Range Mat", "Driving-Range-Matte", "Alfombra de Campo de Prácticas",
       "Drivingrangematta"),
      {"range_mat": vc.model(MAT_TEX, range_mat())}, vc.facing_state(M("range_mat")),
      item={"parent": "csm:block/venues/golf/range_mat", "display": vu.icon_display(0.625)},
      tab=TAB)
DIV_TEX = {"panel": T("green_paint"), "steel": T("steel"), "particle": T("green_paint")}


def divider():
    return [box([7.2, 0.6, 1], [8.8, 15, 15], "panel"),
            box([6.8, 0, 0.6], [9.2, 15.4, 1.4], "steel"),
            box([6.8, 0, 14.6], [9.2, 15.4, 15.4], "steel"),
            box([6.8, 15, 0.6], [9.2, 15.6, 15.4], "steel"),
            box([5, 0, 0.6], [11, 0.6, 2], "steel"), box([5, 0, 14], [11, 0.6, 15.4], "steel")]


C.add("range_tee_divider",
      'new BlockSportFixture("range_tee_divider", BlockSportFixture.Kind.RANGE_DIVIDER)',
      ("Driving Range Bay Divider", "Driving-Range-Trennwand", "Divisor de Puesto de Prácticas",
       "Skiljevägg (Drivingrange)"),
      {"range_divider": vc.model(DIV_TEX, divider())}, vc.facing_state(M("range_divider")),
      item={"parent": "csm:block/venues/golf/range_divider", "display": vu.icon_display(0.625)},
      tab=TAB)
BUCKET_TEX = {"bucket": T("green_paint"), "balls": T("balls"), "steel": T("steel"),
              "particle": T("green_paint")}


def bucket():
    els = vc.lsc.post(8, 8, 2.6, 0, 5, "bucket", top=False)
    els.append(box([5.6, 4.6, 5.6], [10.4, 5.4, 10.4], "balls", faces=("up",)))
    for e in els[-1:]:
        e["faces"]["up"]["uv"] = [0, 0, 16, 16]
    els.append(box([5.3, 5, 7.8], [10.7, 7.6, 8.2], "steel", faces=("north", "south", "up")))
    return els


C.add("range_ball_bucket",
      'new BlockSportFixture("range_ball_bucket", BlockSportFixture.Kind.RANGE_BUCKET)',
      ("Bucket of Range Balls", "Eimer Rangebälle", "Cubo de Bolas de Prácticas",
       "Hink med Rangebollar"),
      {"ball_bucket": dict(vc.model(BUCKET_TEX, bucket()), display=vu.icon_display(1.3, 1))},
      vc.facing_state(M("ball_bucket")), tab=TAB)
RF_TEX = {"pole": T("white"), "flag": T("red"), "plate": T("range_0"), "black": T("black"),
          "particle": T("white")}


def range_flag():
    els = vc.lsc.post(8, 8, 0.5, 0, 31.9, "pole")
    els.append({"from": [8.4, 24, 8], "to": [16, 31, 8],
                "faces": {"north": {"texture": "#flag", "uv": [0, 0, 16, 14]},
                          "south": {"texture": "#flag", "uv": [16, 0, 0, 14]}}})
    p = box([3, 2, 7.2], [13, 8, 8.8], "plate")
    for f in ("north", "south"):
        p["faces"][f]["uv"] = [0, 2, 16, 14] if f == "north" else [16, 2, 0, 14]
    els.append(p)
    els += [box([3.4, 0, 7.6], [4.2, 2, 8.4], "black"), box([11.8, 0, 7.6], [12.6, 2, 8.4], "black")]
    return els


RANGE_FLAG_COLOURS = ["red", "yellow", "blue", "white"]
C.add("range_target_flag",
      'new BlockSportFixture("range_target_flag", BlockSportFixture.Kind.RANGE_FLAG)',
      ("Driving Range Distance Flag", "Entfernungsflagge (Driving Range)",
       "Bandera de Distancia de Campo de Prácticas", "Avståndsflagga (Drivingrange)"),
      {"range_flag": vc.model(RF_TEX, range_flag())},
      {"forge_marker": 1, "defaults": {"model": M("range_flag")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(k): {"textures": {"plate": T("range_%d" % k),
                                                      "flag": T(RANGE_FLAG_COLOURS[k])}}
                                for k in range(4)},
                    "inventory": [{"transform": vu.obj_inventory(0.5)}]}}, tab=TAB)

DISP_TEX = {"cab": T("green_paint"), "steel": T("steel"), "balls": T("balls"),
            "black": T("black"), "glass": T("white"), "particle": T("green_paint")}


def dispenser():
    els = [wbox([2, 0, 3], [14, 18, 14], "cab"),
           wbox([3, 18, 4], [13, 28, 13], "balls"),
           wbox([2.6, 28, 3.6], [13.4, 29.4, 13.4], "steel"),
           wbox([6, 6, 1.6], [10, 9, 3], "steel"),
           wbox([5, 0.6, 0.4], [11, 4.6, 6], "black"),
           wbox([5.4, 4.6, 0.8], [10.6, 5.0, 5.6], "balls", faces=("up",)),
           wbox([9.4, 12, 2.6], [12.6, 16, 3], "black"),
           wbox([4, 12.6, 2.6], [8, 14.6, 3], "steel")]
    return els


C.add("range_ball_dispenser",
      vu.unit_java("BlockVenueUnit", "range_ball_dispenser", [(2, 0, 3, 14, 29, 14)]),
      ("Range Ball Dispenser", "Ballautomat (Driving Range)",
       "Dispensador de Bolas de Prácticas", "Bollautomat (Drivingrange)"),
      {"ball_dispenser": vc.model(DISP_TEX, dispenser())}, vc.facing_state(M("ball_dispenser")),
      item={"parent": "csm:block/venues/golf/ball_dispenser", "display": vu.icon_display(0.5, -2)},
      tab=TAB)

# ------------------------------------------------------------------------------------------
# Mini-golf
# ------------------------------------------------------------------------------------------
@C.texture("carpet")
def _carpet():
    rng = random.Random(20261740)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgba(vc.shade((236, 236, 236), 1 + rng.uniform(-0.05, 0.05)))
    return img


@C.texture("carpet_cup")
def _carpet_cup():
    img = _carpet()
    vc.disc(img, 8, 8, 1.6, rgba((24, 24, 22)))
    return img


def tinted_cube(tex):
    return vc.model({"all": tex, "particle": tex},
                    [{"from": [0, 0, 0], "to": [16, 16, 16],
                      "faces": {n: {"texture": "#all", "cullface": n, "tintindex": 0}
                                for n in ("north", "south", "east", "west", "up", "down")}}])


C.add("minigolf_carpet",
      'new BlockSportSurface("minigolf_carpet", BlockArenaFloorBase.Surface.CARPET, true, 0)',
      ("Mini-Golf Carpet", "Minigolf-Teppich", "Alfombra de Minigolf", "Minigolfmatta"),
      {"minigolf_carpet": tinted_cube(T("carpet"))},
      {"forge_marker": 1, "defaults": {"model": M("minigolf_carpet")},
       "variants": {"inventory": [{}], "colour": {str(k): {} for k in range(16)}}}, tab=TAB)
_cup = tinted_cube(T("carpet"))
_cup["textures"]["cup"] = T("carpet_cup")
_cup["elements"][0]["faces"]["up"]["texture"] = "#cup"
C.add("minigolf_cup",
      'new BlockSportSurface("minigolf_cup", BlockArenaFloorBase.Surface.CARPET, true, 0)',
      ("Mini-Golf Cup", "Minigolf-Loch", "Hoyo de Minigolf", "Minigolfhål"),
      {"minigolf_cup": _cup},
      {"forge_marker": 1, "defaults": {"model": M("minigolf_cup")},
       "variants": {"inventory": [{}], "colour": {str(k): {} for k in range(16)}}}, tab=TAB)

HUMP_TEX = {"carpet": T("mat"), "wood": T("wood"), "particle": T("mat")}


def hump():
    els = []
    for k in range(8):
        z0 = k * 2
        h = 3.0 * math.sin(math.pi * (z0 + 1) / 16.0)
        els.append(box([0, 0, z0], [16, max(0.2, h), z0 + 2], "carpet",
                       faces=("up", "north", "south", "east", "west")))
    return els


C.add("minigolf_ramp", 'new BlockSportFixture("minigolf_ramp", BlockSportFixture.Kind.MINIGOLF_HUMP)',
      ("Mini-Golf Ramp", "Minigolf-Rampe", "Rampa de Minigolf", "Minigolframp"),
      {"minigolf_ramp": vc.model(HUMP_TEX, hump())}, vc.facing_state(M("minigolf_ramp")),
      item={"parent": "csm:block/venues/golf/minigolf_ramp", "display": vu.icon_display(0.625)},
      tab=TAB)

SC_TEX = {"wood": T("wood"), "cards": T("scorecards"), "steel": T("steel"),
          "particle": T("wood")}


def scorecard_stand():
    return [box([7, 0, 7], [9, 10, 9], "wood"), box([4, 0, 6], [12, 0.6, 10], "wood"),
            box([3.5, 10, 5.5], [12.5, 14, 10.5], "wood", faces=("north", "south", "east",
                                                                  "west", "down")),
            box([4, 13.2, 6], [8, 14.2, 10], "cards", faces=("up",)),
            box([8.4, 12.8, 6], [12, 13.8, 10], "steel", faces=("up",)),
            box([9, 13.8, 7], [11.6, 14.3, 7.4], "wood")]


C.add("minigolf_scorecard_stand",
      'new BlockSportFixture("minigolf_scorecard_stand", BlockSportFixture.Kind.SCORECARD_STAND)',
      ("Scorecard and Pencil Stand", "Scorekarten- und Bleistiftständer",
       "Soporte de Tarjetas y Lápices", "Ställ för Scorekort och Pennor"),
      {"scorecard_stand": vc.model(SC_TEX, scorecard_stand())},
      vc.facing_state(M("scorecard_stand")),
      item={"parent": "csm:block/venues/golf/scorecard_stand", "display": vu.icon_display(0.9)},
      tab=TAB)

# windmill and loop: OBJ units
WM_MATS = {"brick": T("brick"), "roof": T("roof"), "sail": T("sail"), "wood": T("wood"),
           "door": T("windmill_door"), "cream": T("cream")}


def windmill():
    out = []
    # the tower: a tapering octagon, an arch the ball runs through at its foot
    for k in range(8):
        a0 = math.radians(22.5 + 45 * k)
        a1 = math.radians(22.5 + 45 * (k + 1))
        r0, r1 = 7.4, 5.6
        p = [(8 + r0 * math.cos(a0), 0, 8 + r0 * math.sin(a0)),
             (8 + r0 * math.cos(a1), 0, 8 + r0 * math.sin(a1)),
             (8 + r1 * math.cos(a1), 36, 8 + r1 * math.sin(a1)),
             (8 + r1 * math.cos(a0), 36, 8 + r1 * math.sin(a0))]
        mid = (math.cos((a0 + a1) / 2), 0.05, math.sin((a0 + a1) / 2))
        mat = "door" if abs(math.degrees((a0 + a1) / 2) - 270) < 1 else "brick"
        uv = [(0, 1), (1, 1), (1, 0), (0, 0)] if mat == "door" else [(0, 1), (1, 1), (1, 0),
                                                                     (0, 0)]
        out.append(vc.Poly(p, mat, mid, uv))
    # the cap
    tip = (8, 46, 8)
    for k in range(8):
        a0 = math.radians(22.5 + 45 * k)
        a1 = math.radians(22.5 + 45 * (k + 1))
        p = [(8 + 6.6 * math.cos(a0), 36, 8 + 6.6 * math.sin(a0)),
             (8 + 6.6 * math.cos(a1), 36, 8 + 6.6 * math.sin(a1)), tip]
        mid = (math.cos((a0 + a1) / 2), 0.6, math.sin((a0 + a1) / 2))
        out.append(vc.Poly(p, "roof", mid, [(0, 1), (1, 1), (0.5, 0)]))
    # the sails, on the north face, turned 45 degrees
    hub = (8, 32, 0.6)
    out += vu.tube((8, 32, 2.6), (8, 32, 0.0), 0.8, "wood", sides=8)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        d = (math.cos(a), math.sin(a))
        n = (-d[1], d[0])
        p0 = (hub[0] + d[0] * 2, hub[1] + d[1] * 2)
        p1 = (hub[0] + d[0] * 18, hub[1] + d[1] * 18)
        w = 3.2
        pts = [(p0[0], p0[1], 0.4), (p1[0], p1[1], 0.4), (p1[0] + n[0] * w, p1[1] + n[1] * w, 0.4),
               (p0[0] + n[0] * w, p0[1] + n[1] * w, 0.4)]
        out.append(vc.Poly(pts, "sail", (0, 0, -1)))
        out.append(vc.Poly(list(reversed(pts)), "sail", (0, 0, 1)))
        out += vu.tube((p0[0], p0[1], 0.3), (p1[0], p1[1], 0.3), 0.35, "wood", sides=6)
    return out


U.write_obj("windmill", "minigolf", windmill())
LOOP_MATS = {"carpet": T("mat"), "steel": T("steel"), "wood": T("wood")}


def loop():
    """A loop-the-loop: a carpeted track 10 px wide round a vertical circle of radius 11 px,
    entering at the front and leaving at the back, its sides edged in steel."""
    out = []
    r, cy, cz = 11.0, 12.0, 8.0
    n = 20
    for k in range(n):
        a0 = 2 * math.pi * k / n
        a1 = 2 * math.pi * (k + 1) / n
        if k == 0 or k == n - 1:
            pass
        p0 = (cz - r * math.sin(a0), cy - r * math.cos(a0))
        p1 = (cz - r * math.sin(a1), cy - r * math.cos(a1))
        x0, x1 = 3.0, 13.0
        quad = [(x0, p0[1], p0[0]), (x1, p0[1], p0[0]), (x1, p1[1], p1[0]), (x0, p1[1], p1[0])]
        mid = ((a0 + a1) / 2)
        inward = (0, math.cos(mid), math.sin(mid))
        out.append(vc.Poly(quad, "carpet", inward))
        out.append(vc.Poly(list(reversed(quad)), "steel", tuple(-c for c in inward)))
        for x in (x0, x1):
            out += vu.tube((x, p0[1], p0[0]), (x, p1[1], p1[0]), 0.4, "steel", caps=False, sides=6)
    for z in (cz - 4, cz + 4):
        out += vu.box(1, 15, 0, 1.2, z - 1, z + 1, "wood")
    return out


U.write_obj("loop", "minigolf", loop())
U.mtl("minigolf", dict(WM_MATS, **LOOP_MATS))
C.add("minigolf_windmill",
      vu.unit_java("BlockVenueUnit", "minigolf_windmill", [(1, 0, 1, 15, 36, 15)]),
      ("Mini-Golf Windmill", "Minigolf-Windmühle", "Molino de Minigolf", "Minigolfväderkvarn"),
      {}, U.obj_state("windmill", 0.3, ty=-0.2), tab=TAB)
C.add("minigolf_loop",
      vu.unit_java("BlockVenueUnit", "minigolf_loop", [(1, 0, 1, 15, 2, 15), (3, 20, 2, 13, 24, 14)]),
      ("Mini-Golf Loop", "Minigolf-Looping", "Rizo de Minigolf", "Minigolfloop"),
      {}, U.obj_state("loop", 0.45, ty=-0.1), tab=TAB)

# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesgolf", ("CSM: Sports & Venues - Golf", "CSM: Sport & Veranstaltungsstätten - Golf",
                              "CSM: Deportes y Recintos - Golf", "CSM: Sport & Arenor - Golf"))
for _i, _n in enumerate((("Red flag (front)", "Rote Flagge (vorn)", "Bandera roja (delante)",
                          "Röd flagga (fram)"),
                         ("White flag (middle)", "Weiße Flagge (Mitte)",
                          "Bandera blanca (centro)", "Vit flagga (mitten)"),
                         ("Blue flag (back)", "Blaue Flagge (hinten)", "Bandera azul (detrás)",
                          "Blå flagga (bak)"),
                         ("Yellow flag", "Gelbe Flagge", "Bandera amarilla", "Gul flagga"))):
    L("csm.venues.golf.pin.%d" % _i, _n)
for _i, _n in enumerate((("Red tee", "Roter Abschlag", "Salida roja", "Röd tee"),
                         ("White tee", "Weißer Abschlag", "Salida blanca", "Vit tee"),
                         ("Blue tee", "Blauer Abschlag", "Salida azul", "Blå tee"),
                         ("Gold tee", "Goldener Abschlag", "Salida dorada", "Guld tee"))):
    L("csm.venues.golf.tee.%d" % _i, _n)
for _i, (_t, _bg, _fg) in enumerate(YARDS):
    L("csm.venues.golf.yards.%d" % _i, ("%s yards" % _t, "%s Yards" % _t, "%s yardas" % _t,
                                        "%s yards" % _t))
for _i, (_t, _bg) in enumerate(RANGE):
    L("csm.venues.golf.range.%d" % _i, ("%s yards" % _t, "%s Yards" % _t, "%s yardas" % _t,
                                        "%s yards" % _t))
for _i, _n in enumerate((("Hole", "Loch", "Hoyo", "Hål"), ("Par", "Par", "Par", "Par"),
                         ("Yards from each tee (back first)", "Yards je Abschlag (hinten zuerst)",
                          "Yardas desde cada salida (atrás primero)",
                          "Yards från varje tee (bakre först)"),
                         ("Line under them", "Zeile darunter", "Línea debajo",
                          "Rad under dem"))):
    L("gui.csm.venue_text.golf.%d" % _i, _n)


# ------------------------------------------------------------------------------------------
# --layout: a green, a range, a mini-golf hole
# ------------------------------------------------------------------------------------------
FACE_IDX = {"south": 0, "west": 1, "north": 2, "east": 3}
CUT = {"green": 0, "collar": 1, "fairway": 2, "rough": 3, "tee": 4, "cup": 5}


def green():
    """(i, dy, j, block, meta): an oval green 13 x 10 with its collar, the cup and flag, a
    bunker beside it, fairway leading in from the south, rough round them, a tee box further
    south with markers and its sign, a yardage marker, a bench, a ball washer and a cart path
    with its curb."""
    out = []
    for i in range(-10, 11):
        for j in range(-8, 26):
            e = (i / 6.5) ** 2 + (j / 5.0) ** 2
            if e <= 1.0:
                cut = "green"
            elif e <= 1.35:
                cut = "collar"
            elif abs(i) <= 5 and 0 < j <= 20:
                cut = "fairway"
            elif abs(i) <= 2 and 21 <= j <= 24:
                cut = "tee"
            else:
                cut = "rough"
            out.append((i, 0, j, "golf_turf", CUT[cut]))
    out.append((1, 0, -1, "golf_turf", CUT["cup"]))
    out.append((1, 1, -1, "golf_flagstick", FACE_IDX["south"] | 0 << 2))
    for i in range(-10, -6):
        for j in range(-1, 3):
            if (i + 8.5) ** 2 + (j - 1) ** 2 <= 4.5:
                out.append((i, 0, j, "golf_bunker_sand", 0))
    for i in (-1, 1):
        out.append((i, 1, 22, "golf_tee_marker", FACE_IDX["north"] | 1 << 2))
    out.append((3, 1, 24, "golf_hole_sign", FACE_IDX["south"]))
    out.append((-3, 1, 24, "golf_ball_washer", FACE_IDX["east"]))
    out.append((6, 1, 10, "golf_yardage_marker", FACE_IDX["east"] | 0 << 2))
    for j in range(-6, 26):
        out.append((9, 0, j, "golf_cart_path", 0))
        out.append((10, 0, j, "golf_cart_path", 0))
        out.append((8, 1, j, "golf_cart_path_curb", 2))     # its east side, the path's edge
    for j in (14, 15):
        out.append((11, 1, j, "golf_bench", FACE_IDX["west"]))
    return out


def driving_range():
    """Four bays along x facing north, dividers between, a dispenser, buckets; the range north
    with flags at 50, 100, 150 and 200 yards (46, 91, 137, 183 m: shown here at a quarter of
    that, the flags being decor)."""
    out = []
    for i in range(-2, 14):
        for j in range(-60, 4):
            out.append((i, 0, j, "golf_turf", CUT["fairway"] | 8))
    for b in range(4):
        i = b * 3
        out.append((i + 1, 1, 0, "driving_range_mat", FACE_IDX["north"]))
        out.append((i + 1, 1, 1, "range_ball_bucket", FACE_IDX["north"]))
        out.append((i, 1, 0, "range_tee_divider", FACE_IDX["north"]))
    out.append((12, 1, 0, "range_tee_divider", FACE_IDX["north"]))
    out.append((13, 1, 2, "range_ball_dispenser", FACE_IDX["west"]))
    for k, d in enumerate((12, 23, 34, 46)):
        out.append((5, 1, -d, "range_target_flag", FACE_IDX["south"] | k << 2))
    return out


def minigolf():
    """Two holes on carpet, 3 wide: an L with a ramp, and a straight one past a windmill and
    through a loop,
    their borders, cups, a scorecard stand and a hole sign."""
    out = []
    cells = set()
    for j in range(0, 10):
        for i in range(0, 3):
            cells.add((i, j))
    for i in range(3, 8):
        for j in range(7, 10):
            cells.add((i, j))
    for i in range(10, 13):
        for j in range(0, 10):
            cells.add((i, j))
    for (i, j) in cells:
        out.append((i, 0, j, "minigolf_carpet", 0))
    out.append((6, 0, 8, "minigolf_cup", 0))
    out.append((11, 0, 1, "minigolf_cup", 0))
    for (i, j) in cells:
        mask = 0
        for bit, (di, dj) in enumerate(((0, -1), (1, 0), (0, 1), (-1, 0))):
            if (i + di, j + dj) not in cells:
                mask |= 1 << bit
        if mask:
            out.append((i, 1, j, "minigolf_border", mask))
    for i in range(0, 3):
        out.append((i, 1, 4, "minigolf_ramp", FACE_IDX["north"]))
    out.append((11, 1, 5, "minigolf_windmill", FACE_IDX["south"]))
    out.append((11, 1, 8, "minigolf_loop", FACE_IDX["south"]))
    out.append((-2, 1, 0, "minigolf_scorecard_stand", FACE_IDX["east"]))
    out.append((-2, 1, 3, "golf_hole_sign", FACE_IDX["east"]))
    return out


LAYOUTS = {"green": green, "range": driving_range, "minigolf": minigolf}


def turn_meta(block, meta, rot):
    if block in ("golf_turf",):
        return meta ^ (8 if rot & 1 else 0)
    if block in ("golf_bunker_sand", "golf_cart_path", "minigolf_carpet", "minigolf_cup"):
        return meta
    if block in ("minigolf_border", "golf_cart_path_curb"):
        return ((meta << rot) | (meta >> (4 - rot))) & 15 if rot else meta
    return (meta & ~3) | (((meta & 3) + rot) % 4)


def commands(name, x0, y0, z0, rot=0):
    out = []
    for (i, dy, j, b, m) in LAYOUTS[name]():
        wx, wz = gc.to_world(i, j, rot)
        out.append("setblock %d %d %d csm:%s %d" % (x0 + wx, y0 + dy, z0 + wz, b,
                                                    turn_meta(b, m, rot)))
    return out


def main():
    if "--layout" in sys.argv:
        name = sys.argv[sys.argv.index("--layout") + 1]
        rot = int(sys.argv[sys.argv.index("--rot") + 1]) if "--rot" in sys.argv else 0
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(name, x, y, z, rot)))
        else:
            print(json.dumps({"layout": name, "objects": [list(o) for o in LAYOUTS[name]()]}))
        return 0
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
