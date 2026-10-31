#!/usr/bin/env python3
"""
gen_venues_courts.py -- the courts of the Sports & Venues Courts, Track & Pool tab.

    py -3 dev-env-utils/scripts/gen_venues_courts.py                      # write
    py -3 dev-env-utils/scripts/gen_venues_courts.py --check              # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_courts.py --fragments          # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_courts.py --layout tennis_pickleball
                                                                            # a court's blocks
    py -3 dev-env-utils/scripts/gen_venues_courts.py --layout tennis --commands X Y Z [--rot R]
                                                                            # ... as commands
    py -3 dev-env-utils/scripts/gen_venues_courts.py --preview DIR        # pictures of them

Outdoor hard courts in acrylic, every line where it really falls: tennis, pickleball (on its
own, or over a tennis court in a second line colour), basketball (full and half court),
volleyball indoors and on sand. The courts are court_layouts.py's geometry, drawn by
venues_lines.py's engine: a court is an anchor (``court_acrylic_anchor``, whose tile entity says
which court, where in it the anchor sits and the colours) and painted cells
(``court_acrylic_painted``) that each find it and show their piece, as baked quads computed from the
geometry, so a line is exactly where it is in any cell and no cell needs a texture of its own.
Plain cells are ``court_acrylic`` in any of sixteen colours, or ``sports_sand``.

This generator also writes the engine's Java tables (``LineArtMaterials.java``,
``CourtLayouts.java``) and a sample of painted cells for ``LinePainterTest``, so the Java painter
is held to this one.

Then the fixtures: the tennis net (a run with posts and the centre strap, sagging to the middle),
the pickleball net, the volleyball net (posts, antennas, two heights), the tennis umpire chair,
the players' bench, the chain-link fence with its windscreen and the divider curtain, a tennis
ball machine, the courtside scoreboard (sets, games and points) and an outdoor pole-mounted
basketball hoop. No brand.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402
import venues_lines as vl  # noqa: E402
import venues_units as vu  # noqa: E402
import court_layouts as cl  # noqa: E402

GEN = "gen_venues_courts.py"
C = vc.catalogue(GEN, "courts")
T = C.T
M = C.M
U = vu.Units(C, GEN, "courts")
box = vc.box
wbox = vc.lsc.pp.wbox
TAB = "Sports & Venues - Courts, Track & Pool"
FACINGS = vc.FACINGS
ys = vu.ys
REPO = vc.REPO
JAVA_DIR = os.path.join(REPO, "modules", "venues", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "lines")
TEST_DIR = os.path.join(REPO, "modules", "venues", "src", "test", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "lines")

# palette
ASPHALT = (58, 58, 60)
WHITE = (240, 240, 236)
BLACK = (28, 28, 30)
NET_BLACK = (30, 32, 32)
POST_GREEN = (36, 72, 48)
ALU = (192, 196, 200)
STEEL = (120, 124, 130)
YELLOW_BALL = (214, 232, 60)
ORANGE = (232, 104, 30)
RED = (196, 36, 36)
NAVY = (30, 44, 84)
SAND = (214, 192, 146)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def grain(base, seed, spread, size=32, flecks=()):
    """A light isotropic grain round ``base`` (grey when tinted), with optional flecks:
    (colour, density) pairs."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = vc.shade(base, 1.0 + rng.uniform(-spread, spread))
            for col, dens in flecks:
                if rng.random() < dens:
                    c = col
            px[x, y] = rgba(c)
    return img


# ------------------------------------------------------------------------------------------
# Surface textures (the line art's sprites live here for both families)
# ------------------------------------------------------------------------------------------
@C.texture("acrylic")
def _acrylic():
    """An acrylic court coating, light grey (the colour is a tint or the vertex colour): the
    fine sand in the paint gives it a soft stipple."""
    return grain((236, 236, 236), 20261101, 0.035)


@C.texture("paint")
def _paint():
    return grain((246, 246, 244), 20261102, 0.02)


@C.texture("tape")
def _tape():
    """A beach court's line tape: a woven strap, light so the line colour tints it."""
    img = grain((236, 236, 236), 20261103, 0.03, size=16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                px[x, y] = rgba(vc.shade(px[x, y][:3], 0.9))
    return img


@C.texture("sand")
def _sand():
    return grain(SAND, 20261104, 0.07, flecks=(((178, 156, 118), 0.06), ((236, 222, 190), 0.05)))


@C.texture("kerb")
def _kerb():
    return vc.brushed((214, 218, 222), 20261105)


@C.texture("concrete")
def _concrete():
    return grain((176, 174, 166), 20261106, 0.05, flecks=(((150, 148, 140), 0.05),))


@C.texture("steel")
def _steel():
    return vc.brushed((150, 154, 160), 20261107)


@C.texture("asphalt")
def _asphalt():
    return grain(ASPHALT, 20261108, 0.12, size=16, flecks=(((96, 96, 96), 0.08),))


def _lines_icon(base, line, seed):
    img = grain(base, seed, 0.03)
    vc.rect(img, 0, 20, 32, 22, rgba(line))
    vc.rect(img, 9, 0, 11, 20, rgba(line))
    return img


@C.texture("painted_icon")
def _painted_icon():
    return _lines_icon((236, 236, 236), (250, 250, 250), 20261109)


@C.texture("anchor_icon")
def _anchor_icon():
    img = _lines_icon((236, 236, 236), (250, 250, 250), 20261110)
    vc.rect(img, 22, 4, 28, 10, rgba((240, 180, 30)))
    return img


# ------------------------------------------------------------------------------------------
# Fixture textures
# ------------------------------------------------------------------------------------------
@C.texture("net")
def _net():
    """A tennis or pickleball net: black cord in 4 cm squares, the rest cut out."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 4 == 0 or y % 4 == 0:
                px[x, y] = rgba(vc.shade(NET_BLACK, 1.0 + 0.1 * ((x * 7 + y * 3) % 3)))
    return img


@C.texture("net_white")
def _net_white():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 5 == 0 or y % 5 == 0:
                px[x, y] = rgba(vc.shade(WHITE, 1.0 - 0.05 * ((x + y) % 3)))
    return img


for _n, _c, _s in (("white", WHITE, 1), ("black", BLACK, 2), ("post_green", POST_GREEN, 3),
                   ("alu", ALU, 4), ("steel_dark", STEEL, 5), ("ball", YELLOW_BALL, 6),
                   ("orange", ORANGE, 7), ("red", RED, 8), ("navy", NAVY, 9),
                   ("grey", (128, 132, 136), 10), ("blue_pad", (36, 70, 150), 11)):
    C.textures[_n] = (lambda c=_c, s=_s: vc.swatch(c, 20261120 + s))


@C.texture("ball_hopper")
def _hopper():
    """A hopper full of tennis balls, seen from above."""
    rng = random.Random(20261131)
    img = Image.new("RGBA", (16, 16), rgba((60, 60, 60)))
    for _k in range(26):
        cx, cy = rng.uniform(1, 15), rng.uniform(1, 15)
        vc.disc(img, cx, cy, 1.6, rgba(vc.shade(YELLOW_BALL, rng.uniform(0.85, 1.05))))
    return img


@C.texture("antenna")
def _antenna():
    """A volleyball antenna's 10 cm red and white bands."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        c = RED if (y // 2) % 2 == 0 else WHITE
        vc.rect(img, 0, y, 16, y + 1, rgba(c))
    return img


@C.texture("backboard")
def _backboard():
    """An outdoor backboard: white, the shooter's square and the border in orange."""
    img = vc.swatch(WHITE, 20261132, size=32)
    vc.frame(img, 0, 0, 32, 32, rgba(ORANGE), 1)
    vc.frame(img, 11, 15, 21, 25, rgba(ORANGE), 1)
    return img


@C.texture("chainlink_mesh")
def _chainlink_mesh():
    """The chain-link: the Building module's mesh, drawn the same way here (two sets of
    diagonal wires, galvanized), so the courts' fence reads as that fence."""
    n, pitch = 32, 8
    colour = (178, 184, 186)
    rng = random.Random(20261210)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            a = (x + y) % pitch == 0
            b = (x - y) % pitch == 0
            if a or b:
                d = (14 if a and b else 0) + rng.uniform(-8, 8)
                px[x, y] = tuple(max(0, min(255, int(round(c + d)))) for c in colour) + (255,)
    return img


@C.texture("chainlink_post")
def _chainlink_post():
    rng = random.Random(20261220)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 7.5
            v = 18 - 40 * d * d + rng.uniform(-3, 3)
            px[x, y] = tuple(max(0, min(255, int(c + v))) for c in (168, 174, 176)) + (255,)
    return img


SCREENS = {1: (40, 84, 52), 2: (36, 64, 120), 3: (32, 34, 36)}
CURTAINS = {0: (40, 84, 52), 1: (36, 64, 120), 2: (32, 34, 36), 3: (110, 112, 114)}


def _screen(colour, seed):
    """Windscreen: knitted shade cloth with a fine open weave (a few texels cut through)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x % 8 == 3 and y % 8 == 5) and rng.random() < 0.5:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = rgba(vc.shade(colour, 1.0 + (0.08 if y % 2 else -0.04)
                                         + rng.uniform(-0.03, 0.03)))
    return img


for _k, _c in SCREENS.items():
    C.textures["screen_%d" % _k] = (lambda c=_c, k=_k: _screen(c, 20261140 + k))


def _curtain(colour, seed):
    """A divider curtain: mesh in its colour with a solid hem band at the foot of its texture."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if x % 2 == 0 or y % 2 == 0:
                px[x, y] = rgba(vc.shade(colour, 1.0 + rng.uniform(-0.05, 0.05)))
    return img


for _k, _c in CURTAINS.items():
    C.textures["curtain_%d" % _k] = (lambda c=_c, k=_k: _curtain(c, 20261150 + k))


@C.texture("board_face")
def _board_face():
    return vc.swatch((22, 24, 24), 20261160)


@C.texture("board_cab")
def _board_cab():
    return vc.swatch((40, 44, 48), 20261161)


# ------------------------------------------------------------------------------------------
# Plain surfaces and the line art blocks
# ------------------------------------------------------------------------------------------
def cube(top, side, tint=False, bottom=None):
    f = {"texture": "#top", "cullface": "up"}
    if tint:
        f["tintindex"] = 0
    faces = {"up": f, "down": {"texture": "#bottom", "cullface": "down"}}
    for n in ("north", "south", "east", "west"):
        faces[n] = {"texture": "#side", "cullface": n}
    return {"parent": "block/block",
            "textures": {"top": top, "side": side, "bottom": bottom or side, "particle": top},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def plain_state(model_name, prop=None, n=0):
    variants = {"inventory": [{}]}
    if prop:
        variants[prop] = {str(k): {} for k in range(n)}
    else:
        variants["normal"] = [{}]
    return {"forge_marker": 1, "defaults": {"model": M(model_name)}, "variants": variants}


C.add("court_acrylic", 'new BlockSportSurface("court_acrylic", BlockArenaFloorBase.Surface.ACRYLIC, true)',
      ("Court Surface (Acrylic)", "Platzbelag (Acryl)", "Superficie de Pista (Acrílico)",
       "Banbeläggning (Akryl)"),
      {"court_acrylic": cube(T("acrylic"), T("asphalt"), tint=True)},
      plain_state("court_acrylic", "colour", 16), tab=TAB)
C.add("court_acrylic_anchor",
      'new BlockLineAnchor("court_acrylic_anchor", BlockArenaFloorBase.Surface.ACRYLIC, LineArtFamily.COURTS)',
      ("Court Marking Anchor", "Platzmarkierungs-Anker", "Ancla de Marcado de Pista",
       "Banmarkeringsankare"),
      {"court_anchor": cube(T("anchor_icon"), T("asphalt"))},
      plain_state("court_anchor", "rot", 4), tab=TAB)
C.add("court_acrylic_painted",
      'new BlockLinePainted("court_acrylic_painted", BlockArenaFloorBase.Surface.ACRYLIC, LineArtFamily.COURTS)',
      ("Court Surface (Painted)", "Platzbelag (Markiert)", "Superficie de Pista (Pintada)",
       "Banbeläggning (Målad)"),
      {"court_acrylic_painted": cube(T("painted_icon"), T("asphalt"))},
      plain_state("court_acrylic_painted"), tab=TAB)
C.add("sports_sand", 'new BlockSportSurface("sports_sand", BlockArenaFloorBase.Surface.SAND, false)',
      ("Sports Sand", "Sportsand", "Arena Deportiva", "Sportsand"),
      {"sports_sand": cube(T("sand"), T("sand"))}, plain_state("sports_sand"), tab=TAB)


# ------------------------------------------------------------------------------------------
# Nets: runs across the court (BlockCourtNet)
# ------------------------------------------------------------------------------------------
NET_TEX = {"net": T("net"), "white": T("white"), "post": T("post_green"), "black": T("black"),
           "steel": T("steel_dark"), "particle": T("post_green")}


def net_span(x0, x1, top, tape_tex="white", net_tex="net"):
    """A net from x0 to x1 in the middle of the cell, its tape's top at ``top``."""
    return [box([x0, 0.6, 8], [x1, top - 1.0, 8], net_tex, faces=("north", "south")),
            box([x0, top - 1.1, 7.55], [x1, top, 8.45], tape_tex),
            box([x0, 0.3, 7.8], [x1, 0.7, 8.2], "black", faces=("north", "south", "up"))]


def net_post(xc, top, foot=False):
    els = vc.lsc.post(xc, 8, 0.9, 0, top + 0.5, "post")
    if foot:
        els.append(box([xc - 0.7, 0, 3.5], [xc + 0.7, 0.8, 12.5], "steel"))
    return els


def net_models(prefix, tops, post_off, centre_top, foot=False, winder=True):
    """The run's parts: mid_<sag> (the net across), mid_4 (the middle, with its strap), end_l /
    end_r (a post, the net from it to the far edge), solo (posts both sides)."""
    out = {}
    for s, t in enumerate(tops):
        out["%s_mid_%d" % (prefix, s)] = vc.model(NET_TEX, net_span(0, 16, t))
    strap = [box([7.6, 0, 7.7], [8.4, centre_top - 0.2, 8.3], "white"),
             box([7.4, 0, 7.4], [8.6, 0.4, 8.6], "steel")]
    out["%s_mid_4" % prefix] = vc.model(NET_TEX, net_span(0, 16, centre_top) + strap)
    pl, pr = 8 - post_off, 8 + post_off
    top0 = tops[0]
    end_l = net_post(pl, top0, foot) + net_span(pl, 16, top0)
    if winder:
        end_l += [box([pl - 0.4, 9, 8.9], [pl + 0.4, 11, 10.2], "steel"),
                  box([pl - 0.25, 9.6, 10.2], [pl + 0.25, 10.4, 11.4], "black")]
    out["%s_end_l" % prefix] = vc.model(NET_TEX, end_l)
    out["%s_end_r" % prefix] = vc.model(NET_TEX, net_post(pr, top0, foot) + net_span(0, pr, top0))
    out["%s_solo" % prefix] = vc.model(NET_TEX, net_post(pl, top0, foot) + net_post(pr, top0, foot)
                                       + net_span(pl, pr, top0))
    out["%s_item" % prefix] = vc.model(NET_TEX, net_post(pl, top0, foot)
                                       + net_span(pl, 16, top0))
    return out


def net_state(prefix, sags):
    def choose(v):
        el, er, s = v["end_l"] == "true", v["end_r"] == "true", int(v["sag"])
        s = s if s == 4 else min(s, sags - 1)
        name = ("solo" if el and er else "end_l" if el else "end_r" if er else "mid_%d" % s)
        return dict({"model": M("%s_%s" % (prefix, name))}, **ys(dict(FACINGS)[v["facing"]]))
    return vc.keyed_state([("facing", [f for f, _ in FACINGS]), ("end_l", ["false", "true"]),
                           ("end_r", ["false", "true"]), ("sag", [str(k) for k in range(5)])],
                          choose, {"model": M(prefix + "_item")})


TENNIS_TOPS = [17.12, 16.29, 15.45, 14.84]
PICKLE_TOPS = [14.63, 14.22]
C.add("tennis_net", 'new BlockCourtNet("tennis_net", BlockCourtNet.Kind.TENNIS)',
      ("Tennis Net", "Tennisnetz", "Red de Tenis", "Tennisnät"),
      net_models("tennis_net", TENNIS_TOPS, 6.4, 14.62), net_state("tennis_net", 4),
      item={"parent": "csm:block/venues/courts/tennis_net_item",
            "display": vu.icon_display(0.6, -1)}, tab=TAB)
C.add("pickleball_net", 'new BlockCourtNet("pickleball_net", BlockCourtNet.Kind.PICKLEBALL)',
      ("Pickleball Net (Portable)", "Pickleball-Netz (Mobil)", "Red de Pickleball (Portátil)",
       "Pickleballnät (Portabelt)"),
      net_models("pickleball_net", PICKLE_TOPS, 5.6, 13.82, foot=True, winder=False),
      net_state("pickleball_net", 2),
      item={"parent": "csm:block/venues/courts/pickleball_net_item",
            "display": vu.icon_display(0.6, -1)}, tab=TAB)


# The volleyball net: OBJ (it stands 2.43 m), a 1 m band under its top
VB_TOP = {"m": 2.43 * 16, "w": 2.24 * 16}
VB_MATS = {"post": T("white"), "pad": T("navy"), "net": T("net"), "white": T("white"),
           "ant": T("antenna"), "steel": T("steel_dark")}


def vb_band(x0, x1, top):
    out = vu.panel((x0, top - 15.0, 8), (x1, top - 15.0, 8), (x1, top - 1.1, 8),
                   (x0, top - 1.1, 8), (0, 0, -1), "net", tile=8.0)
    out += vu.box(x0, x1, top - 1.1, top, 7.6, 8.4, "white")
    out += vu.box(x0, x1, top - 15.8, top - 15.0, 7.7, 8.3, "white")
    out += vu.box(x0, x1, top - 8.2, top - 8.0, 7.9, 8.1, "steel", faces="nsud")
    return out


def vb_post(xc, top):
    out = vu.tube((xc, 0, 8), (xc, top + 2.0, 8), 1.3, "post")
    out += vu.tube((xc, 0, 8), (xc, 28.0, 8), 2.4, "pad")
    out += vu.box(xc - 3, xc + 3, 0, 0.6, 5, 11, "steel")
    return out


def vb_antenna(x, top):
    return vu.tube((x, top - 15.8, 8), (x, top + 12.8, 8), 0.3, "ant", sides=6)


for _h, _top in VB_TOP.items():
    U.write_obj("vb_%s_mid" % _h, "volleyball_net", vb_band(0, 16, _top))
    U.write_obj("vb_%s_end_l" % _h, "volleyball_net", vb_post(8, _top) + vb_band(8, 16, _top))
    U.write_obj("vb_%s_end_r" % _h, "volleyball_net", vb_post(8, _top) + vb_band(0, 8, _top))
    U.write_obj("vb_%s_ant_l" % _h, "volleyball_net", vb_antenna(0.4, _top))
    U.write_obj("vb_%s_ant_r" % _h, "volleyball_net", vb_antenna(15.6, _top))
U.mtl("volleyball_net", VB_MATS)


def vb_state():
    rules = []
    for f, y in FACINGS:
        for w, h in (("false", "m"), ("true", "w")):
            def ap(part):
                return dict({"model": M("vb_%s_%s.obj" % (h, part))}, **ys(y))
            base = {"facing": f, "women": w}
            rules.append({"when": dict(base, end_l="false", end_r="false"), "apply": ap("mid")})
            rules.append({"when": dict(base, end_l="true"), "apply": ap("end_l")})
            rules.append({"when": dict(base, end_r="true"), "apply": ap("end_r")})
            rules.append({"when": dict(base, ant_l="true"), "apply": ap("ant_l")})
            rules.append({"when": dict(base, ant_r="true"), "apply": ap("ant_r")})
    return {"multipart": rules}


C.add("volleyball_net", 'new BlockCourtNet("volleyball_net", BlockCourtNet.Kind.VOLLEYBALL)',
      ("Volleyball Net", "Volleyballnetz", "Red de Voleibol", "Volleybollnät"), {}, vb_state(),
      item={"parent": "csm:block/venues/courts/volleyball_net_icon",
            "display": vu.icon_display(0.45, -2)}, tab=TAB)
C.blocks[-1]["models"]["volleyball_net_icon"] = vc.model(
    {"post": T("white"), "net": T("net"), "white": T("white"), "pad": T("navy"),
     "particle": T("white")},
    vc.lsc.post(4, 8, 1.3, 0, 31.9, "post") + vc.lsc.post(4, 8, 2.4, 0, 16, "pad")
    + [box([4, 17, 8], [16, 31, 8], "net", faces=("north", "south")),
       box([4, 30.8, 7.6], [16, 31.9, 8.4], "white")])


# ------------------------------------------------------------------------------------------
# The umpire chair, the bench, the ball machine
# ------------------------------------------------------------------------------------------
UMP_TEX = {"frame": T("post_green"), "seat": T("post_green"), "steel": T("steel_dark"),
           "white": T("white"), "black": T("black"), "particle": T("post_green")}


def umpire_chair():
    els = []
    for x in (2, 14):
        for z in (3, 14):
            els.append(box([x - 0.8, 0, z - 0.8], [x + 0.8, 20, z + 0.8], "frame"))
    els += [box([1.2, 19.5, 2.2], [14.8, 20.5, 14.8], "frame"),           # the platform
            box([3.5, 20.5, 6], [12.5, 23, 13], "seat"),                   # the seat box
            box([3.5, 23, 12], [12.5, 31, 13.4], "seat"),                  # the back
            box([2, 23, 4], [3.5, 26, 13], "frame"), box([12.5, 23, 4], [14, 26, 13], "frame"),
            box([3, 17, 1.8], [13, 18, 2.6], "frame"),                     # foot bar
            box([4.5, 26.5, 1.5], [11.5, 27.2, 5.5], "white")]             # the score table
    for y in (4, 8, 12, 16):
        els.append(box([1.2, y, 14.2], [14.8, y + 0.8, 15.2], "steel"))    # the ladder's rungs
    for x in (2, 14):
        els.append(box([x - 0.5, 0.5, 3.5], [x + 0.5, 1.3, 13.5], "frame"))
    return els


C.add("tennis_umpire_chair",
      vu.unit_java("BlockVenueUnit", "tennis_umpire_chair", [(1, 0, 2, 15, 31, 15)]),
      ("Tennis Umpire Chair", "Tennis-Schiedsrichterstuhl", "Silla de Juez de Tenis",
       "Tennisdomarstol"),
      {"umpire_chair": vc.model(UMP_TEX, umpire_chair())},
      vc.facing_state(M("umpire_chair")),
      item={"parent": "csm:block/venues/courts/umpire_chair", "display": vu.icon_display(0.4, -2)},
      tab=TAB)

BENCH_TEX = {"slat": T("post_green"), "frame": T("black"), "particle": T("post_green")}


def bench_parts():
    els = [box([0, 7.5, z], [16, 8.5, z + 2.2], "slat") for z in (4.5, 7.2, 10)]
    els += [box([0, 10.5, 12.6], [16, 12.5, 13.6], "slat"), box([0, 13.5, 12.6], [16, 15.5, 13.6],
                                                                 "slat")]
    return els


def bench_end():
    return [box([0.5, 0, 4.5], [1.5, 8, 5.5], "frame"), box([0.5, 0, 11.5], [1.5, 16, 12.5],
                                                             "frame"),
            box([0.5, 7, 4.5], [1.5, 7.5, 12.5], "frame"),
            box([0.5, 8, 4.5], [1.5, 10, 6], "frame")]


def run_models(prefix, parts, end, tex):
    return {prefix: vc.model(tex, parts),
            prefix + "_end_l": vc.model(tex, end),
            prefix + "_end_r": vc.model(tex, [vc.mirror_x(e) for e in end]),
            prefix + "_item": vc.model(tex, parts + end + [vc.mirror_x(e) for e in end])}


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


C.add("court_players_bench", 'new BlockArenaRun("court_players_bench", BlockArenaRun.Kind.COURT_BENCH)',
      ("Players' Bench", "Spielerbank", "Banca de Jugadores", "Spelarbänk"),
      run_models("court_bench", bench_parts(), bench_end(), BENCH_TEX), run_state("court_bench"),
      item={"parent": "csm:block/venues/courts/court_bench_item",
            "display": vu.icon_display(0.625)}, tab=TAB)

MACH_TEX = {"body": T("navy"), "hopper": T("ball_hopper"), "steel": T("steel_dark"),
            "black": T("black"), "ball": T("ball"), "particle": T("navy")}


def ball_machine():
    els = [box([4, 2, 3], [12, 9, 13], "body"),
           box([3.5, 9, 2.5], [12.5, 14, 13.5], "body", faces=("north", "south", "east", "west",
                                                               "down")),
           box([4, 13.5, 3], [12, 13.6, 13], "hopper", faces=("up",)),
           box([3.5, 14, 2.5], [12.5, 14.6, 3.2], "steel"), box([3.5, 14, 12.8], [12.5, 14.6, 13.5],
                                                                "steel"),
           box([6.8, 5, 0.5], [9.2, 7.4, 3], "black"),                       # the throat
           box([3, 0, 3.5], [4, 2.6, 6.5], "black"), box([12, 0, 3.5], [13, 2.6, 6.5], "black"),
           box([7, 0, 11], [9, 2, 13], "black"),
           box([7.4, 9, 13], [8.6, 16, 14], "steel"), box([5, 15.5, 13.2], [11, 16.3, 14.2],
                                                           "steel")]
    return els


C.add("tennis_ball_machine",
      'new BlockSportFixture("tennis_ball_machine", BlockSportFixture.Kind.BALL_MACHINE)',
      ("Tennis Ball Machine", "Tennis-Ballwurfmaschine", "Máquina Lanzapelotas",
       "Tennisbollkanon"),
      {"ball_machine": vc.model(MACH_TEX, ball_machine())}, vc.facing_state(M("ball_machine")),
      item={"parent": "csm:block/venues/courts/ball_machine", "display": vu.icon_display(0.8)},
      tab=TAB)


# ------------------------------------------------------------------------------------------
# Fences: chain-link with windscreen, divider curtain (BlockCourtFence)
# ------------------------------------------------------------------------------------------
FENCE_TEX = {"post": T("chainlink_post"), "mesh": T("chainlink_mesh"),
             "particle": T("chainlink_post")}
for _k in SCREENS:
    FENCE_TEX["screen%d" % _k] = T("screen_%d" % _k)


def fence_models():
    out = {"fence_post": vc.model(FENCE_TEX, [box([7.1, 0, 7.1], [8.9, 16, 8.9], "post")]),
           "fence_post_top": vc.model(FENCE_TEX, [box([7.1, 0, 7.1], [8.9, 15.5, 8.9], "post"),
                                                  box([6.8, 15.5, 6.8], [9.2, 16.6, 9.2], "post")])}
    for top in (False, True):
        t = "_top" if top else ""
        mtop = 15.0 if top else 16.0
        els = [box([8, 0, 8], [16, mtop, 8], "mesh", faces=("north", "south"))]
        if top:
            els.append(box([8, 14.8, 7.4], [16, 16, 8.6], "post"))
        out["fence_arm" + t] = vc.model(FENCE_TEX, els)
        for k in SCREENS:
            out["fence_screen%d%s" % (k, t)] = vc.model(FENCE_TEX, [
                box([8, 0, 7.6], [16, mtop - 0.6 if top else 16, 8.4], "screen%d" % k,
                    faces=("north", "south"))])
    out["fence_item"] = vc.model(FENCE_TEX, [box([7.1, 0, 7.1], [8.9, 16, 8.9], "post"),
                                             box([0, 0, 8], [16, 15, 8], "mesh",
                                                 faces=("north", "south")),
                                             box([0, 14.8, 7.4], [16, 16, 8.6], "post"),
                                             box([0, 0.8, 7.6], [16, 14.4, 8.4], "screen1",
                                                 faces=("north", "south"))])
    return out


ARM_TURN = {"east": 0, "south": 90, "west": 180, "north": 270}


def fence_state():
    rules = [{"when": {"top": "false"}, "apply": {"model": M("fence_post")}},
             {"when": {"top": "true"}, "apply": {"model": M("fence_post_top")}}]
    for side, y in ARM_TURN.items():
        for top in ("false", "true"):
            t = "_top" if top == "true" else ""
            rules.append({"when": {side: "true", "top": top},
                          "apply": dict({"model": M("fence_arm" + t)}, **ys(y))})
            for k in SCREENS:
                rules.append({"when": {side: "true", "top": top, "colour": str(k)},
                              "apply": dict({"model": M("fence_screen%d%s" % (k, t))}, **ys(y))})
    return {"multipart": rules}


C.add("court_fence", 'new BlockCourtFence("court_fence", false)',
      ("Court Fence (Chain-Link)", "Platzzaun (Maschendraht)", "Valla de Pista (Malla Ciclónica)",
       "Banstängsel (Nätstängsel)"),
      fence_models(), fence_state(),
      item={"parent": "csm:block/venues/courts/fence_item", "display": vu.icon_display(0.625)},
      tab=TAB)

CURT_TEX = {"pipe": T("steel_dark"), "particle": T("curtain_0")}
for _k in CURTAINS:
    CURT_TEX["c%d" % _k] = T("curtain_%d" % _k)


def curtain_models():
    out = {}
    for k in CURTAINS:
        for top in (False, True):
            t = "_top" if top else ""
            els = [box([8, 0, 8], [16, 15.2 if top else 16, 8], "c%d" % k,
                       faces=("north", "south"))]
            if top:
                els.append(box([8, 15.2, 7.5], [16, 16, 8.5], "pipe"))
            out["curtain_arm%d%s" % (k, t)] = vc.model(CURT_TEX, els)
            els = [box([0, 0, 8], [16, 15.2 if top else 16, 8], "c%d" % k,
                       faces=("north", "south"))]
            if top:
                els.append(box([0, 15.2, 7.5], [16, 16, 8.5], "pipe"))
            out["curtain_lone%d%s" % (k, t)] = vc.model(CURT_TEX, els)
    out["curtain_item"] = out["curtain_lone0_top"]
    return out


def curtain_state():
    rules = []
    for k in CURTAINS:
        for top in ("false", "true"):
            t = "_top" if top == "true" else ""
            rules.append({"when": {"colour": str(k), "top": top, "north": "false",
                                   "south": "false", "east": "false", "west": "false"},
                          "apply": {"model": M("curtain_lone%d%s" % (k, t))}})
            for side, y in ARM_TURN.items():
                rules.append({"when": {side: "true", "top": top, "colour": str(k)},
                              "apply": dict({"model": M("curtain_arm%d%s" % (k, t))}, **ys(y))})
    return {"multipart": rules}


C.add("court_divider_curtain", 'new BlockCourtFence("court_divider_curtain", true)',
      ("Court Divider Curtain", "Hallentrennvorhang", "Cortina Divisoria de Pista",
       "Avdelningsridå"),
      curtain_models(), curtain_state(),
      item={"parent": "csm:block/venues/courts/curtain_item", "display": vu.icon_display(0.625)},
      tab=TAB)


# ------------------------------------------------------------------------------------------
# The courtside scoreboard (OBJ casing; the face is TileEntityCourtBoardRenderer's)
# ------------------------------------------------------------------------------------------
SB_FACE = (-32, 16, 17, 39, 6)          # x0, x1, y0, y1, z front: the renderer's SB_FACE
SB_MATS = {"cab": T("board_cab"), "face": T("board_face"), "post": T("steel_dark")}


def scoreboard_polys():
    x0, x1, y0, y1, zf = SB_FACE
    out = vu.box(x0 - 1, x1 + 1, y0 - 1, y1 + 1, zf + 0.2, zf + 4, "cab", faces="sewud")
    out += vu.panel((x1 + 1, y0 - 1, zf + 0.2), (x0 - 1, y0 - 1, zf + 0.2),
                    (x0 - 1, y1 + 1, zf + 0.2), (x1 + 1, y1 + 1, zf + 0.2), (0, 0, -1), "cab",
                    both=False)
    out += vu.panel((x1, y0, zf), (x0, y0, zf), (x0, y1, zf), (x1, y1, zf), (0, 0, -1), "face",
                    both=False)
    for xc in (10.5, -26.5):
        out += vu.box(xc - 1.5, xc + 1.5, 0, y0 - 1, 7, 10, "post")
    return out


U.write_obj("scoreboard", "scoreboard", scoreboard_polys())
U.mtl("scoreboard", SB_MATS)
C.add("court_scoreboard",
      vu.unit_java("BlockCourtBoard", "court_scoreboard",
                   [(9, 0, 7, 12, 16, 10), (-28, 0, 7, -25, 16, 10), (-33, 16, 6, 17, 40, 10)],
                   ", false"),
      ("Court Scoreboard", "Platz-Anzeigetafel", "Marcador de Pista", "Bantavla"), {},
      U.obj_state("scoreboard", 0.32, ty=-0.1, tx=0.35), tab=TAB)


# ------------------------------------------------------------------------------------------
# The outdoor basketball hoop: a square pole behind the baseline, an extension arm, the board
# ------------------------------------------------------------------------------------------
# Master frame (pixels): the court to the north; the pole in the master cell; on --layout the
# master is the cell whose middle is 1 m behind the baseline's middle, so the board's face is
# 1.2 m inside the baseline and the rim's centre 1.575 m (FIBA).
HP_BOARD_Z = 8 - 2.2 * 16                      # the board's face (z): 2.2 m before the pole
HP_RIM_Y = 3.05 * 16
HP_MATS = {"pole": T("navy"), "board": T("backboard"), "frame": T("steel_dark"),
           "rim": T("orange"), "net": T("net_white"), "pad": T("navy")}


def hoop_polys():
    out = vu.box(6, 10, 0, 57, 6, 10, "pole")
    out += vu.box(4, 12, 0, 1, 4, 12, "frame")
    out += vu.box(6.5, 9.5, 40, 42.5, HP_BOARD_Z + 2, 8, "pole")
    out += vu.tube((8, 56, 8), (8, 50, HP_BOARD_Z + 2), 1.0, "pole")
    out += vu.box(6.5, 9.5, 46, 57, HP_BOARD_Z + 1, HP_BOARD_Z + 2.5, "frame")
    bw, by = 1.8 * 16 / 2, 2.9 * 16
    out += vu.box(8 - bw, 8 + bw, by, by + 1.05 * 16, HP_BOARD_Z - 0.0, HP_BOARD_Z + 1.0, "board")
    out += vu.box(8 - 2.5, 8 + 2.5, HP_RIM_Y - 1, HP_RIM_Y + 1.5, HP_BOARD_Z - 2.4, HP_BOARD_Z,
                  "rim")
    rz = HP_BOARD_Z - 0.15 * 16 - 0.225 * 16
    r = 0.225 * 16
    pts = [(8 + r * math.cos(math.radians(a)), rz + r * math.sin(math.radians(a)))
           for a in range(0, 360, 30)]
    for (ax, az), (bx, bz) in zip(pts, pts[1:] + pts[:1]):
        out += vu.tube((ax, HP_RIM_Y, az), (bx, HP_RIM_Y, bz), 0.32, "rim", caps=False, sides=6)
    low = [(8 + 0.6 * r * math.cos(math.radians(a)), rz + 0.6 * r * math.sin(math.radians(a)))
           for a in range(0, 360, 30)]
    for (ax, az), (bx, bz), (cx, cz), (dx, dz) in zip(pts, pts[1:] + pts[:1], low[1:] + low[:1],
                                                      low):
        out.append(vc.Poly([(ax, HP_RIM_Y, az), (bx, HP_RIM_Y, bz), (cx, HP_RIM_Y - 7, cz),
                            (dx, HP_RIM_Y - 7, dz)], "net", ((ax + bx) / 2 - 8, -0.3,
                                                             (az + bz) / 2 - rz),
                           [(0, 0), (1, 0), (1, 1), (0, 1)]))
        out.append(vc.Poly([(dx, HP_RIM_Y - 7, dz), (cx, HP_RIM_Y - 7, cz), (bx, HP_RIM_Y, bz),
                            (ax, HP_RIM_Y, az)], "net", (8 - (ax + bx) / 2, 0.3,
                                                         rz - (az + bz) / 2),
                           [(0, 1), (1, 1), (1, 0), (0, 0)]))
    out += vu.box(4, 12, 0, 28, 5, 11, "pad", faces="nsew")
    return out


U.write_obj("hoop_pole", "hoop_pole", hoop_polys())
U.mtl("hoop_pole", HP_MATS)
C.add("basketball_hoop_pole",
      vu.unit_java("BlockVenueUnit", "basketball_hoop_pole",
                   [(5, 0, 5, 11, 57, 11), (6, 40, int(HP_BOARD_Z), 10, 57, 8),
                    (-6, 46, int(HP_BOARD_Z) - 1, 22, 64, int(HP_BOARD_Z) + 2)]),
      ("Basketball Hoop (Pole-Mounted)", "Basketballkorb (Mastmontiert)",
       "Canasta de Baloncesto (en Poste)", "Basketkorg (Stolpmonterad)"), {},
      U.obj_state("hoop_pole", 0.18, ty=-0.3, tz=-0.4), tab=TAB)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuescourts", ("CSM: Sports & Venues - Courts, Track & Pool",
                                "CSM: Sport & Veranstaltungsstätten - Plätze, Bahn & Becken",
                                "CSM: Deportes y Recintos - Pistas, Atletismo y Piscina",
                                "CSM: Sport & Arenor - Banor, Friidrott & Bassäng"))
SURFACE_NAMES = (("green", "Grün", "verde", "grön"), ("dark green", "Dunkelgrün", "verde oscuro",
                                                       "mörkgrön"),
                 ("blue", "Blau", "azul", "blå"), ("light blue", "Hellblau", "azul claro",
                                                   "ljusblå"),
                 ("red", "Rot", "rojo", "röd"), ("terracotta", "Terrakotta", "terracota",
                                                 "terrakotta"),
                 ("purple", "Violett", "morado", "lila"), ("grey", "Grau", "gris", "grå"),
                 ("tan", "Sandbraun", "beige", "sandbrun"), ("navy", "Marineblau", "azul marino",
                                                             "marinblå"),
                 ("orange", "Orange", "naranja", "orange"), ("black", "Schwarz", "negro", "svart"),
                 ("teal", "Petrol", "verde azulado", "petrol"), ("burgundy", "Bordeaux", "burdeos",
                                                                 "vinröd"),
                 ("yellow", "Gelb", "amarillo", "gul"), ("light grey", "Hellgrau", "gris claro",
                                                         "ljusgrå"))
for _i, _n in enumerate(SURFACE_NAMES):
    L("csm.venues.surface.%d" % _i, _n)
LAYOUT_NAMES = {"tennis": ("Tennis", "Tennis", "Tenis", "Tennis"),
                "pickleball": ("Pickleball", "Pickleball", "Pickleball", "Pickleball"),
                "tennis_pickleball": ("Tennis with pickleball", "Tennis mit Pickleball",
                                      "Tenis con pickleball", "Tennis med pickleball"),
                "basketball": ("Basketball", "Basketball", "Baloncesto", "Basket"),
                "basketball_half": ("Basketball, half court", "Basketball, halbes Feld",
                                    "Baloncesto, media pista", "Basket, halvplan"),
                "volleyball": ("Volleyball", "Volleyball", "Voleibol", "Volleyboll"),
                "sand_volleyball": ("Beach volleyball", "Beachvolleyball", "Vóley playa",
                                    "Beachvolleyboll")}
for _k, _n in LAYOUT_NAMES.items():
    L("csm.venues.lines." + _k, _n)
for _i, _n in enumerate((("men's height", "Herrenhöhe", "altura masculina", "herrhöjd"),
                         ("women's height", "Damenhöhe", "altura femenina", "damhöjd"))):
    L("csm.venues.courts.net_height.%d" % _i, _n)
for _i, _n in enumerate((("no windscreen", "kein Windschutz", "sin cortavientos",
                          "utan vindskydd"),
                         ("green windscreen", "grüner Windschutz", "cortavientos verde",
                          "grönt vindskydd"),
                         ("blue windscreen", "blauer Windschutz", "cortavientos azul",
                          "blått vindskydd"),
                         ("black windscreen", "schwarzer Windschutz", "cortavientos negro",
                          "svart vindskydd"))):
    L("csm.venues.courts.screen.%d" % _i, _n)
for _i, _n in enumerate((("green", "grün", "verde", "grön"), ("blue", "blau", "azul", "blå"),
                         ("black", "schwarz", "negro", "svart"), ("grey", "grau", "gris", "grå"))):
    L("csm.venues.courts.curtain.%d" % _i, _n)
for _i, _n in enumerate((("nobody", "niemand", "nadie", "ingen"),
                         ("player 1", "Spieler 1", "jugador 1", "spelare 1"),
                         ("player 2", "Spieler 2", "jugador 2", "spelare 2"))):
    L("csm.venues.courts.serve.%d" % _i, _n)
for _i, _n in enumerate((("Player 1", "Spieler 1", "Jugador 1", "Spelare 1"),
                         ("Player 2", "Spieler 2", "Jugador 2", "Spelare 2"),
                         ("Player 1's games by set (6 4 2)", "Spiele von Spieler 1 je Satz (6 4 2)",
                          "Juegos del jugador 1 por set (6 4 2)",
                          "Spelare 1:s gem per set (6 4 2)"),
                         ("Player 2's games by set", "Spiele von Spieler 2 je Satz",
                          "Juegos del jugador 2 por set", "Spelare 2:s gem per set"),
                         ("Player 1's points", "Punkte von Spieler 1", "Puntos del jugador 1",
                          "Spelare 1:s poäng"),
                         ("Player 2's points", "Punkte von Spieler 2", "Puntos del jugador 2",
                          "Spelare 2:s poäng"))):
    L("gui.csm.venue_text.tennis.%d" % _i, _n)
L("gui.csm.venue_text.choice.serve", ("Serving: %s", "Aufschlag: %s", "Saque: %s", "Serve: %s"))
L("gui.csm.venue_text.choice.player1", ("Player 1: %s", "Spieler 1: %s", "Jugador 1: %s",
                                        "Spelare 1: %s"))
L("gui.csm.venue_text.choice.player2", ("Player 2: %s", "Spieler 2: %s", "Jugador 2: %s",
                                        "Spelare 2: %s"))


# ------------------------------------------------------------------------------------------
# Java: the engine's tables and the painter's test samples
# ------------------------------------------------------------------------------------------
LAYOUTS = cl.layouts()
JAVA = {
    os.path.join(JAVA_DIR, "LineArtMaterials.java"): vl.materials_java(GEN),
    os.path.join(JAVA_DIR, "CourtLayouts.java"): vl.layouts_java(
        GEN, "CourtLayouts",
        "The courts: tennis, pickleball, tennis with pickleball over it, basketball (full and "
        "half court), volleyball and sand volleyball ({@code court_layouts.py}).", LAYOUTS),
}


def samples_java(family_const, layouts, every=7):
    """Sample painted cells: (layout, i, j) and each material's area there, as this painter
    finds them, for LinePainterTest to hold the Java painter to."""
    rows = []
    for k, lay in enumerate(layouts):
        pics = lay.pictures()
        for c in pics[::every]:
            areas = {}
            for poly, m in vl.paint(lay, c[0], c[1]):
                areas[m] = areas.get(m, 0.0) + abs(vl.area(poly))
            flat = ", ".join("%d, %s" % (m, vl._jnum(round(a, 6))) for m, a in sorted(areas.items()))
            rows.append("      {%d, %d, %d, %s}" % (k, c[0], c[1], flat))
    return rows


def samples_file(name, family_const, rows, gen):
    return """package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * Painted cells sampled by {@code dev-env-utils/scripts/%s}: for each, the layout, the cell
 * and the area of each material there as {@code venues_lines.py} paints it. Do not edit.
 */
final class %s {

  static final LineArtFamily FAMILY = LineArtFamily.%s;

  /** {layout, i, j, material, area, material, area, ...}. */
  static final double[][] CELLS = {
%s};

  private %s() {
  }
}
""" % (gen, name, family_const, ",\n".join(rows), name)


TEST_JAVA = {os.path.join(TEST_DIR, "CourtSamples.java"):
             samples_file("CourtSamples", "COURTS", samples_java("COURTS", LAYOUTS), GEN)}


# ------------------------------------------------------------------------------------------
# A court's blocks, for a builder: --layout NAME [--commands X Y Z [--rot R]]
# ------------------------------------------------------------------------------------------
PLAIN = {"SURFACE": ("court_acrylic", "cc"), "SURROUND": ("court_acrylic", "sc"),
         "SAND": ("sports_sand", None)}


def ground(lay, colours=None):
    """{(i, j): (block, meta, nbt)} for every cell of a court and its run-off, in its frame."""
    cols = list(colours or lay.colours)
    cen = lay.census()
    out = {}
    end, side = cl.RUNOFF[lay.name]
    i0, j0, i1, j1 = lay.extent
    # the run-off round the extent, as the base material
    ri0 = i0 - int(math.ceil(max(0, side - 1)))
    ri1 = i1 + int(math.ceil(max(0, side - 1)))
    rj0 = j0 - int(math.ceil(max(0, end - 1)))
    rj1 = j1 + int(math.ceil(max(0, end - 1)))
    for j in range(rj0, rj1 + 1):
        for i in range(ri0, ri1 + 1):
            v = cen.get((i, j), ("plain", lay.base))
            if v[0] == "picture":
                out[(i, j)] = ("court_acrylic_painted", 0, None)
            else:
                name = vl.MATERIALS[v[1]][0]
                blk, ckey = PLAIN[name]
                meta = cols["cc sc".split().index(ckey)] if ckey else 0
                out[(i, j)] = (blk, meta, None)
    lid = [x.name for x in LAYOUTS].index(lay.name)
    extra = ""
    if colours is not None:
        extra = "".join(",%s:%d" % (k, cols[n]) for n, k in enumerate(("cc", "sc", "lc", "oc")))
    for (ai, aj) in lay.anchors():
        nbt = "{lay:%d,ax:%d,az:%d%s}" % (lid, ai, aj, extra)
        out[(ai, aj)] = ("court_acrylic_anchor", 0, nbt)
    return out


def court_objects(name):
    """A court's fixtures: (i, dy, j, block, meta) in its frame (the court's length north-south,
    meta facing as drawn)."""
    out = []
    if name in ("tennis", "tennis_pickleball"):
        out += [(i, 1, 0, "tennis_net", 2) for i in range(-6, 7)]
        out.append((8, 1, 0, "tennis_umpire_chair", 1))
        out += [(-8, 1, j, "court_players_bench", 3) for j in (-3, -2, 2, 3)]
    elif name == "pickleball":
        for i in range(-3, 4):
            out.append((i, 1, 0, "pickleball_net", 2))
    elif name == "volleyball":
        for i in range(-5, 6):
            out.append((i, 1, 0, "volleyball_net", 2))
    elif name == "sand_volleyball":
        for i in range(-5, 5):
            out.append((i, 1, 0, "volleyball_net", 2))
    elif name in ("basketball", "basketball_half"):
        # each pole's cell is 1 m behind its baseline's middle, facing the court
        out.append((0, 1, -15, "basketball_hoop_pole", 0))
        if name == "basketball":
            out.append((0, 1, 15, "basketball_hoop_pole", 2))
    return out


def turn_meta(block, meta, rot):
    """A fixture's facing turned with the court: S 0 -> W 1 -> N 2 -> E 3 a quarter clockwise."""
    if block in ("court_acrylic", "sports_sand", "court_acrylic_painted", "court_fence",
                 "court_divider_curtain"):
        return meta
    if block == "court_acrylic_anchor":
        return (meta + rot) & 3
    return (meta & ~3) | (((meta & 3) + rot) % 4)


def commands(name, x0, y0, z0, rot=0, colours=None):
    lay = next(l for l in LAYOUTS if l.name == name)
    return lines_commands(ground(lay, colours), court_objects(name), x0, y0, z0, rot, turn_meta)


def lines_commands(g, objects, x0, y0, z0, rot, turn):
    """/fill and /setblock lines for a layout's ground and objects, turned ``rot`` quarters."""
    cells = {}
    for (i, j), (b, m, nbt) in g.items():
        wx, wz = to_world(i, j, rot)
        cells[(wx, wz)] = (b, turn(b, m, rot), nbt)
    out = []
    for wz in sorted(set(z for _x, z in cells)):
        row = sorted(x for x, z in cells if z == wz)
        k = 0
        while k < len(row):
            b, m, nbt = cells[(row[k], wz)]
            q = k
            while (nbt is None and q + 1 < len(row) and row[q + 1] == row[q] + 1
                   and cells[(row[q + 1], wz)] == (b, m, None)):
                q += 1
            if q > k:
                out.append("fill %d %d %d %d %d %d csm:%s %d" % (
                    x0 + row[k], y0, z0 + wz, x0 + row[q], y0, z0 + wz, b, m))
            else:
                out.append("setblock %d %d %d csm:%s %d%s" % (
                    x0 + row[k], y0, z0 + wz, b, m, " replace " + nbt if nbt else ""))
            k = q + 1
    for (i, dy, j, b, m) in objects:
        wx, wz = to_world(i, j, rot)
        out.append("setblock %d %d %d csm:%s %d" % (x0 + wx, y0 + dy, z0 + wz, b, turn(b, m, rot)))
    return out


def to_world(i, j, rot):
    """A layout cell's world offset for a layout turned ``rot`` quarters clockwise
    (LineArtKey.toWorld)."""
    return {0: (i, j), 1: (-j, i), 2: (-i, -j), 3: (j, -i)}[rot & 3]


def layout_json(name):
    lay = next(l for l in LAYOUTS if l.name == name)
    g = ground(lay)
    return {"layout": name, "id": [x.name for x in LAYOUTS].index(name),
            "frame": "the court's centre cell at (0, 0) as court_layouts.py says, its length "
                     "north-south; ground blocks at the floor level, objects dy above it",
            "ground": [[i, j, b, m, nbt] for (i, j), (b, m, nbt) in sorted(
                g.items(), key=lambda kv: (kv[0][1], kv[0][0]))],
            "objects": [list(o) for o in court_objects(name)]}


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def write_java(files, check):
    stale = []
    for path, text in files.items():
        if check:
            if not os.path.exists(path):
                stale.append(path)
                continue
            with open(path, encoding="utf-8") as fh:
                if fh.read() != text:
                    stale.append(path)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(text)
    return stale


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
        for lay in LAYOUTS:
            print(vl.preview(lay, os.path.join(out, lay.name + ".png"), ppm=24,
                             colours=cl.PREVIEW))
        return 0
    if "--doc" in sys.argv:
        print(json.dumps({lay.name: {"extent": lay.extent, "painted": len(lay.pictures()),
                                     "anchors": lay.anchors(), "ops": len(lay.ops)}
                          for lay in LAYOUTS}, indent=1))
        return 0
    files = dict(JAVA)
    files.update(TEST_JAVA)
    if "--check" in sys.argv:
        stale = write_java(files, True)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        write_java(files, False)
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
