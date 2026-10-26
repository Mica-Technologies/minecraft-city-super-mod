#!/usr/bin/env python3
"""Every asset the Building module's Stairs & Railings tab ships: stair railings, guards and wall
handrails in eight styles, and the stairs they run on.

    python dev-env-utils/scripts/gen_stairs_railings.py
    python dev-env-utils/scripts/gen_stairs_railings.py --check
    python dev-env-utils/scripts/gen_stairs_railings.py --fragments   # tab lines
    python dev-env-utils/scripts/gen_stairs_railings.py --report      # quads a cell

**Railings** (BlockStairRailing). A cell is the air cell over a stair or a floor, with a railing
along one or more of its edges. Every part here is drawn for the north edge, running along x, and
the blockstate turns it to the edges the cell has. A cell whose edges run one way also has a
``slope``, read from the stairs around it: ``none`` (level), ``mid`` (over a stair, the flight
going on), ``top`` (over the top stair, the rail bending level halfway across) and ``foot`` (in
front of the bottom stair, bending up halfway across), each ``pos`` (climbing east or south in the
world) or ``neg``. The models are drawn climbing east; on the south and west edges, whose turn
carries the model's east to the west or north, the blockstate draws the mirror model.

Everything follows one line, the **nosing line** N(x): the line through the stair nosings, which a
vanilla stair's two half steps put at 45 degrees. Per shape it is three heights at x = 0, 8 and
16 px -- level (0, 0, 0), mid (-8, 0, 8), top (-8, 0, 0), foot (0, 0, 8) -- so the handrail at
N(x) + 15 meets the next cell's at every cell boundary and the bends fall halfway across, where a
real rail's do (it levels off a tread's width past the top nosing). The treads under the cell,
where posts and balusters stand, are F(x): a mid or top cell has its two half steps at -8 and 0.

Posts stand on the back tread of a sloped cell (x = 11.2, or 4.8 the other way) and in the middle
of a level one, so a run needs no end state: every cell carries its own post. Rails and pickets
are sheared prisms, so their ends are vertical and meet the next cell's exactly; on a slope a
rail's vertical size is scaled by root 2 so it looks as thick as a level one.

Every part is OBJ: a 45 degree JSON element can only be turned about one axis, a glass or cable
infill on a slope is a parallelogram, and a single leaf of uv per face is enough for paint, steel
and grain. Glass styles draw in the translucent layer; cables and turned balusters are cutout
planes. Each finish has its own OBJ set and MTL (a vanilla multipart cannot retexture an OBJ).

**Stairs** (BlockStairFlight) keep vanilla's stair geometry and rotations, without uvlock, so a
nosing strip drawn at a tread's front stays at its front whichever way the stair faces.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_utilities_meters as um  # noqa: E402
import gen_utilities_water as gw  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

GEN = "gen_stairs_railings.py"
TAB = "CsmTabStairsRailings"
ASSETS = os.path.join(um.REPO, "modules", "building", "src", "main", "resources", "assets",
                      "csm")
C = um.Catalogue(GEN, "stairsrailings", "stairsrailings", assets=ASSETS)
Poly = gw.Poly
FACINGS = um.FACINGS


def T(name):
    return C.T(name)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
METALS = {
    "black": (30, 30, 32), "white": (232, 232, 228), "grey": (150, 153, 156),
    "bronze": (72, 58, 46), "beige": (198, 188, 164), "yellow": (228, 186, 30),
}
WOODS = {"oak": (178, 128, 76), "walnut": (96, 64, 42), "cherry": (150, 78, 50)}
GALV = (160, 166, 168)
STAINLESS = (192, 198, 202)
PAINT_WHITE = (238, 238, 234)

for _k, _c in METALS.items():
    C.textures["paint_" + _k] = (lambda c, s: (lambda: lc.fill(c, 16, 3, s)))(_c, 4100 + len(_k))


@C.texture("galvanized")
def _galvanized():
    img = lc.fill(GALV, 16, 4, 4120)
    rnd = random.Random(4121)
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        k = rnd.choice((0.9, 1.1))
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            img.putpixel(((x + dx) % 16, (y + dy) % 16), lc.shade(GALV, k) + (255,))
    return img


@C.texture("stainless")
def _stainless():
    """Brushed stainless: fine streaks along u, the way a rail is drawn."""
    rnd = random.Random(4122)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        k = rnd.uniform(0.94, 1.06)
        for x in range(16):
            n = rnd.uniform(-3, 3)
            c = lc.shade(STAINLESS, k)
            img.putpixel((x, y), lc.clamp((c[0] + n, c[1] + n, c[2] + n)) + (255,))
    return img


def wood(base, seed):
    """Timber, the grain running along u (along a rail, up a post)."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        k = rnd.uniform(0.88, 1.08)
        for x in range(16):
            ring = 0.06 * math.sin((x * 0.4 + y * 1.7 + rnd.uniform(0, 0.4)))
            c = lc.shade(base, k + ring)
            img.putpixel((x, y), c + (255,))
    return img


for _i, (_k, _c) in enumerate(WOODS.items()):
    C.textures["wood_" + _k] = (lambda c, s: (lambda: wood(c, s)))(_c, 4130 + _i)
C.textures["paint_trim"] = lambda: lc.fill(PAINT_WHITE, 16, 2, 4140)


@C.texture("glass")
def _glass():
    """Clear tempered glass: a faint green-blue tint, its edge a touch stronger."""
    img = Image.new("RGBA", (16, 16), (196, 226, 222, 54))
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, (170, 210, 206, 110))
    return img


@C.texture("cable")
def _cable():
    """Stainless cables, eight across the infill, on a clear ground (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 16, 2):
        for x in range(16):
            img.putpixel((x, y), lc.shade(STAINLESS, 0.92 if x % 4 else 1.05) + (255,))
    return img


@C.texture("turned")
def _turned():
    """A turned baluster's silhouette, painted white, on a clear ground (cutout). Drawn on a
    plane two pixels wide and as tall as the baluster: square at the foot and the head, a vase
    and a bead between."""
    size = 32
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    profile = []
    for y in range(size):
        t = y / (size - 1)          # 0 at the top
        if t < 0.18 or t > 0.72:
            w = 1.0                  # the square head and foot
        elif t < 0.22 or 0.68 < t:
            w = 0.62                 # collars
        elif t < 0.30:
            w = 0.45                 # neck
        else:
            u = (t - 0.30) / 0.38    # the vase, widest low
            w = 0.42 + 0.48 * math.sin(math.pi * min(1.0, u * 1.25)) ** 1.5
        profile.append(w)
    for y, w in enumerate(profile):
        half = w * size / 2
        for x in range(size):
            d = abs(x + 0.5 - size / 2)
            if d <= half:
                k = 1.0 - 0.18 * (d / max(half, 1))
                img.putpixel((x, y), lc.shade(PAINT_WHITE, k) + (255,))
    return img


CONCRETE = (176, 174, 168)
C.textures["concrete"] = lambda: lc.fill(CONCRETE, 16, 6, 4150)
C.textures["nosing_dark"] = lambda: lc.fill((52, 52, 50), 16, 6, 4151)
C.textures["carpet_grey"] = lambda: lc.fill((92, 94, 98), 16, 9, 4152)


# ------------------------------------------------------------------------------------------
# Geometry, in px with the railing on the north edge, then written in blocks
# ------------------------------------------------------------------------------------------
SHAPES = {          # the nosing line N at x = 0, 8, 16; and the treads F of the two halves
    "level": ((0, 0, 0), (0, 0)),
    "mid_pos": ((-8, 0, 8), (-8, 0)),
    "top_pos": ((-8, 0, 0), (-8, 0)),
    "foot_pos": ((0, 0, 8), (0, 0)),
}
for _k in ("mid", "top", "foot"):
    n, f = SHAPES[_k + "_pos"]
    SHAPES[_k + "_neg"] = ((n[2], n[1], n[0]), (f[1], f[0]))
SLOPED = ("mid_pos", "mid_neg", "top_pos", "top_neg", "foot_pos", "foot_neg")
H = 15.0            # the handrail's top above the nosing line
ROOT2 = math.sqrt(2)


def nose(shape, x):
    n = SHAPES[shape][0]
    if x <= 8:
        return n[0] + (n[1] - n[0]) * x / 8.0
    return n[1] + (n[2] - n[1]) * (x - 8) / 8.0


def tread(shape, x):
    return SHAPES[shape][1][0 if x < 8 else 1]


def segments(shape):
    """The spans of x over which N is straight, joined where both halves slope alike."""
    n = SHAPES[shape][0]
    if n[1] - n[0] == n[2] - n[1]:
        return [(0.0, 16.0)]
    return [(0.0, 8.0), (8.0, 16.0)]


def post_x(shape):
    """Where a cell's post stands: on the back (upper) tread of a slope, mid-cell on the level."""
    if shape.endswith("_pos"):
        return 11.2
    if shape.endswith("_neg"):
        return 4.8
    return 8.0


def px(p):
    return (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0)


def _norm(v):
    m = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / m for c in v)


def quad(pts, mat, outward, uvs=None):
    """A polygon in px, facing ``outward``; uvs default to the whole sprite, corner by corner
    (bottom left, bottom right, top right, top left)."""
    if uvs is None:
        uvs = [(0, 1), (1, 1), (1, 0), (0, 0)] if len(pts) == 4 else [(0.5, 0.5)] * len(pts)
    p = Poly([px(q) for q in pts], uvs, [_norm(outward)] * len(pts), mat, outward=outward)
    return p


class Part(object):
    """A shape's polygons, tagged with their materials (style texture slots)."""

    def __init__(self):
        self.polys = []

    def add(self, polys):
        self.polys += polys


def rail(shape, z0, z1, y0, y1, mat, round_=False, xs=None):
    """A rail along the run, from y0 to y1 above the nosing line and z0 to z1 across, over the
    whole cell or ``xs`` (x from, x to): a sheared box or octagon per straight span."""
    out = []
    lo, hi = xs if xs else (0.0, 16.0)
    for a, b in segments(shape):
        a, b = max(a, lo), min(b, hi)
        if b <= a:
            continue
        slope = (nose(shape, b) - nose(shape, a)) / (b - a)
        k = ROOT2 if abs(slope) > 1e-6 else 1.0
        yc, hy = (y0 + y1) / 2.0, (y1 - y0) / 2.0 * k
        zc, hz = (z0 + z1) / 2.0, (z1 - z0) / 2.0
        if round_:
            ring = []
            for i in range(8):
                ang = math.radians(22.5 + 45 * i)
                ring.append((zc + hz * math.cos(ang), yc + hy * math.sin(ang)))
        else:
            ring = [(zc - hz, yc - hy), (zc + hz, yc - hy), (zc + hz, yc + hy),
                    (zc - hz, yc + hy)]
        na, nb = nose(shape, a), nose(shape, b)
        for i in range(len(ring)):
            (za, ya), (zb, yb) = ring[i], ring[(i + 1) % len(ring)]
            mz, my = (za + zb) / 2 - zc, (yb + ya) / 2 - yc
            pts = [(a, na + ya, za), (b, nb + ya, za), (b, nb + yb, zb), (a, na + yb, zb)]
            out.append(quad(pts, mat, (0, my, mz)))
        for x, n, d in ((a, na, -1), (b, nb, 1)):
            pts = [(x, n + y, z) for z, y in ring]
            out.append(quad(pts, mat, (d, 0, 0)))
    return out


def plane(shape, z, y0, y1, mat, xs=(0.0, 16.0)):
    """An infill plane at depth z from y0 to y1 above the nosing line, both faces: glass or
    cables. v runs from the plane's top to its foot, so a texture's rows follow the slope."""
    out = []
    for a, b in segments(shape):
        a, b = max(a, xs[0]), min(b, xs[1])
        if b <= a:
            continue
        na, nb = nose(shape, a), nose(shape, b)
        ua, ub = a / 16.0, b / 16.0
        pts = [(a, na + y0, z), (b, nb + y0, z), (b, nb + y1, z), (a, na + y1, z)]
        uvs = [(ua, 1), (ub, 1), (ub, 0), (ua, 0)]
        out.append(quad(pts, mat, (0, 0, 1), uvs))
        out.append(quad(list(reversed(pts)), mat, (0, 0, -1), list(reversed(uvs))))
    return out


def vbox(x0, x1, z0, z1, y0, y1, mat, faces=("n", "s", "e", "w", "u", "d")):
    """An upright box, in absolute px."""
    out = []
    if "n" in faces:
        out.append(quad([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)], mat, (0, 0, -1)))
    if "s" in faces:
        out.append(quad([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], mat, (0, 0, 1)))
    if "w" in faces:
        out.append(quad([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], mat, (-1, 0, 0)))
    if "e" in faces:
        out.append(quad([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)], mat, (1, 0, 0)))
    if "u" in faces:
        out.append(quad([(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)], mat, (0, 1, 0)))
    if "d" in faces:
        out.append(quad([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], mat, (0, -1, 0)))
    return out


def voct(xc, zc, r, y0, y1, mat, cap=True):
    """An upright octagon (a round post), sides and top."""
    ring = [(xc + r * math.cos(math.radians(22.5 + 45 * i)),
             zc + r * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
    out = []
    for i in range(8):
        (xa, za), (xb, zb) = ring[i], ring[(i + 1) % 8]
        n = ((xa + xb) / 2 - xc, 0, (za + zb) / 2 - zc)
        out.append(quad([(xa, y0, za), (xb, y0, zb), (xb, y1, zb), (xa, y1, za)], mat, n))
    if cap:
        out.append(quad([(x, y1, z) for x, z in ring], mat, (0, 1, 0)))
    return out


def crossed(xc, zc, w, y0, y1, mat):
    """Two crossed planes, both faces, for a turned baluster's cutout silhouette."""
    out = []
    h = w / 2.0
    for dx, dz in ((h, 0), (0, h)):
        pts = [(xc - dx, y0, zc - dz), (xc + dx, y0, zc + dz), (xc + dx, y1, zc + dz),
               (xc - dx, y1, zc - dz)]
        n = (-dz, 0, dx)
        out.append(quad(pts, mat, n))
        out.append(quad(list(reversed(pts)), mat, (-n[0], 0, -n[2]),
                        [(0, 0), (1, 0), (1, 1), (0, 1)]))
    return out


PICKETS = (1.6, 4.8, 8.0, 11.2, 14.4)   # one of them is the post's place
BALUSTERS = (2.0, 6.0, 10.0, 14.0)      # two to a tread


# ------------------------------------------------------------------------------------------
# Styles: what a cell of each is made of, for a shape
# ------------------------------------------------------------------------------------------
def picket(shape, round_rail):
    zc = 2.0
    out = []
    if round_rail:
        out += rail(shape, zc - 1.0, zc + 1.0, H - 2.0, H, "rail", round_=True)
        out += rail(shape, zc - 0.7, zc + 0.7, H - 4.6, H - 3.2, "frame")
    else:
        out += rail(shape, zc - 0.9, zc + 0.9, H - 1.6, H, "rail")
    out += rail(shape, zc - 0.7, zc + 0.7, 2.0, 3.4, "frame")
    p = post_x(shape)
    top_rail_bottom = H - (4.6 if round_rail else 1.6)
    for x in PICKETS:
        if abs(x - p) < 0.1:
            continue
        n = nose(shape, x)
        out += vbox(x - 0.4, x + 0.4, zc - 0.4, zc + 0.4, n + 3.4, n + top_rail_bottom, "frame",
                    faces=("n", "s", "e", "w"))
    n = nose(shape, p)
    out += vbox(p - 0.8, p + 0.8, zc - 0.8, zc + 0.8, tread(shape, p), n + H - 1.6, "frame",
                faces=("n", "s", "e", "w", "u"))
    return out


def pipe(shape):
    zc = 2.0
    out = rail(shape, zc - 0.9, zc + 0.9, H - 1.8, H, "rail", round_=True)
    out += rail(shape, zc - 0.9, zc + 0.9, 6.6, 8.4, "rail", round_=True)
    p = post_x(shape)
    out += voct(p, zc, 0.9, tread(shape, p), nose(shape, p) + H - 0.9, "rail", cap=False)
    return out


def cable(shape, flat_cap):
    zc = 2.0
    out = []
    if flat_cap:
        out += rail(shape, zc - 1.6, zc + 1.6, H - 1.2, H, "rail")
    else:
        out += rail(shape, zc - 0.9, zc + 0.9, H - 1.8, H, "rail", round_=True)
    out += plane(shape, zc, 1.5, H - 2.2, "cable")
    p = post_x(shape)
    n = nose(shape, p)
    out += vbox(p - 0.75, p + 0.75, zc - 0.75, zc + 0.75, tread(shape, p), n + H - 1.2, "frame",
                faces=("n", "s", "e", "w", "u"))
    return out


def glass(shape):
    zc = 1.8
    out = rail(shape, zc - 1.0, zc + 1.0, 0.0, 2.6, "frame")             # the base shoe
    out += plane(shape, zc - 0.25, 2.6, H - 3.4, "glass")
    out += plane(shape, zc + 0.25, 2.6, H - 3.4, "glass")
    out += rail(shape, zc + 1.6, zc + 3.4, H - 1.8, H, "rail", round_=True)   # on standoffs
    p = post_x(shape)
    n = nose(shape, p)
    out += vbox(p - 0.4, p + 0.4, zc + 0.25, zc + 2.0, n + H - 1.5, n + H - 0.7, "rail",
                faces=("n", "s", "e", "w", "u", "d"))
    return out


def wall_rail(shape, round_):
    zc = 3.0          # the rail's centre off the wall
    out = rail(shape, zc - 1.0, zc + 1.0, H - 2.0, H, "rail", round_=round_)
    p = post_x(shape)
    n = nose(shape, p)
    out += vbox(p - 1.0, p + 1.0, 0.0, 0.4, n + H - 4.4, n + H - 2.6, "frame")       # rose
    out += vbox(p - 0.3, p + 0.3, 0.4, zc - 0.4, n + H - 3.8, n + H - 3.2, "frame",
                faces=("e", "w", "u", "d"))                                     # arm
    out += vbox(p - 0.3, p + 0.3, zc - 0.4, zc + 0.4, n + H - 3.8, n + H - 2.0, "frame",
                faces=("n", "s", "e", "w"))                                     # saddle
    return out


def balusters(shape, turned):
    zc = 2.0
    out = []
    foot = shape.startswith("foot")
    newel_x = 3.0 if shape == "foot_pos" else 13.0
    xs = (0.0, 16.0)
    if foot:
        xs = (newel_x, 16.0) if shape == "foot_pos" else (0.0, newel_x)
    out += rail(shape, zc - 1.25, zc + 1.25, H - 2.0, H, "rail", xs=xs)
    for x in BALUSTERS:
        if foot and abs(x - newel_x) < 2.5:
            continue
        if foot and ((shape == "foot_pos" and x < newel_x) or (shape == "foot_neg" and x > newel_x)):
            continue
        y0, y1 = tread(shape, x), nose(shape, x) + H - 1.8
        if turned:
            out += crossed(x, zc, 1.8, y0, y1, "baluster")
        else:
            out += vbox(x - 0.6, x + 0.6, zc - 0.6, zc + 0.6, y0, y1, "baluster",
                        faces=("n", "s", "e", "w"))
    if foot:
        top = H + 3.0
        if turned:
            out += vbox(newel_x - 1.8, newel_x + 1.8, zc - 1.8, zc + 1.8, 0.0, 5.0, "newel",
                        faces=("n", "s", "e", "w", "u"))
            out += voct(newel_x, zc, 1.3, 5.0, top - 2.5, "newel", cap=False)
            out += vbox(newel_x - 1.6, newel_x + 1.6, zc - 1.6, zc + 1.6, top - 2.5, top - 1.0,
                        "newel")
            out += voct(newel_x, zc, 1.0, top - 1.0, top, "newel")
        else:
            out += vbox(newel_x - 1.8, newel_x + 1.8, zc - 1.8, zc + 1.8, 0.0, top, "newel",
                        faces=("n", "s", "e", "w"))
            out += vbox(newel_x - 2.3, newel_x + 2.3, zc - 2.3, zc + 2.3, top, top + 1.2,
                        "newel")
            out += vbox(newel_x - 2.1, newel_x + 2.1, zc - 2.1, zc + 2.1, 0.0, 1.5, "newel",
                        faces=("n", "s", "e", "w", "u"))
    return out


# registry, style builder, kind, wood, materials, names (en, de, es, sv)
FIN = {
    "black": ("Black", "Schwarz", "Negro", "Svart"),
    "white": ("White", "Weiß", "Blanco", "Vit"),
    "grey": ("Grey", "Grau", "Gris", "Grå"),
    "bronze": ("Dark Bronze", "Dunkelbronze", "Bronce Oscuro", "Mörk Brons"),
    "galvanized": ("Galvanized", "Feuerverzinkt", "Galvanizado", "Galvaniserad"),
    "beige": ("Beige", "Beige", "Beige", "Beige"),
    "yellow": ("Safety Yellow", "Sicherheitsgelb", "Amarillo de Seguridad", "Varningsgul"),
    "stainless": ("Stainless", "Edelstahl", "Inoxidable", "Rostfri"),
    "oak": ("Oak", "Eiche", "Roble", "Ek"),
    "walnut": ("Walnut", "Nussbaum", "Nogal", "Valnöt"),
    "cherry": ("Cherry", "Kirsche", "Cerezo", "Körsbär"),
}
RAILING = ("Stair Railing", "Treppengeländer", "Barandilla de Escalera", "Trappräcke")


def metal_tex(key):
    if key == "galvanized":
        return T("galvanized")
    if key == "stainless":
        return T("stainless")
    return T("paint_" + key)


def names(prefix, kind_words, finish):
    return tuple("%s (%s, %s)" % (prefix[i], kind_words[i], finish[i]) for i in range(4))


RAILINGS = []


def railing(reg, build, kind, is_wood, mats, display):
    RAILINGS.append((reg, build, kind, is_wood, mats, display))


for key in ("black", "white", "grey", "bronze", "galvanized"):
    t = metal_tex(key)
    railing("railing_picket_" + key, lambda s: picket(s, False), "GUARD", False,
            {"rail": t, "frame": t}, names(RAILING, ("Picket", "Stäbe", "Barrotes", "Spjälor"),
                                           FIN[key]))
for key in ("black", "grey", "beige"):
    t = metal_tex(key)
    railing("railing_picket_round_" + key, lambda s: picket(s, True), "GUARD", False,
            {"rail": t, "frame": t},
            names(RAILING, ("Picket, Round Rail", "Stäbe, Rundhandlauf",
                            "Barrotes, Pasamanos Redondo", "Spjälor, Rund Ledstång"), FIN[key]))
for key in ("black", "galvanized", "yellow"):
    t = metal_tex(key)
    railing("railing_pipe_" + key, pipe, "GUARD", False, {"rail": t},
            names(RAILING, ("Two-Line Pipe", "Zwei Rohrholme", "Tubo de Dos Líneas",
                            "Tvåledat Rör"), FIN[key]))
CABLE = ("Cable", "Seil", "Cable", "Vajer")
railing("railing_cable_stainless", lambda s: cable(s, False), "GUARD", False,
        {"rail": T("stainless"), "frame": T("stainless"), "cable": T("cable")},
        names(RAILING, CABLE, FIN["stainless"]))
railing("railing_cable_black_oak", lambda s: cable(s, True), "GUARD", False,
        {"rail": T("wood_oak"), "frame": T("paint_black"), "cable": T("cable")},
        names(RAILING, CABLE, ("Black, Oak Rail", "Schwarz, Eichenhandlauf",
                               "Negro, Pasamanos de Roble", "Svart, Ekledstång")))
railing("railing_cable_black", lambda s: cable(s, True), "GUARD", False,
        {"rail": T("paint_black"), "frame": T("paint_black"), "cable": T("cable")},
        names(RAILING, CABLE, FIN["black"]))
GLASS = ("Glass", "Glas", "Vidrio", "Glas")
railing("railing_glass_stainless", glass, "GLASS", False,
        {"rail": T("stainless"), "frame": T("stainless"), "glass": T("glass")},
        names(RAILING, GLASS, FIN["stainless"]))
railing("railing_glass_black", glass, "GLASS", False,
        {"rail": T("paint_black"), "frame": T("paint_black"), "glass": T("glass")},
        names(RAILING, GLASS, FIN["black"]))
WALL = ("Wall Handrail", "Wandhandlauf", "Pasamanos de Pared", "Väggledstång")
for key, round_, words in (
        ("stainless_round", True, ("Stainless, Round", "Edelstahl, Rund", "Inoxidable, Redondo",
                                   "Rostfri, Rund")),
        ("black_square", False, ("Black, Square", "Schwarz, Eckig", "Negro, Cuadrado",
                                 "Svart, Fyrkantig")),
        ("oak_round", True, ("Oak, Round", "Eiche, Rund", "Roble, Redondo", "Ek, Rund")),
        ("white_round", True, ("White, Round", "Weiß, Rund", "Blanco, Redondo", "Vit, Rund"))):
    rail_t = {"stainless_round": T("stainless"), "black_square": T("paint_black"),
              "oak_round": T("wood_oak"), "white_round": T("paint_white")}[key]
    frame_t = T("stainless") if key == "stainless_round" else (
        T("paint_black") if key == "black_square" else T("stainless"))
    railing("handrail_wall_" + key, (lambda r: lambda s: wall_rail(s, r))(round_), "WALL",
            key == "oak_round", {"rail": rail_t, "frame": frame_t},
            tuple("%s (%s)" % (WALL[i], words[i]) for i in range(4)))
SQUARE = ("Square Balusters", "Vierkantstäbe", "Balaustres Cuadrados", "Fyrkantiga Balustrar")
for key in ("oak", "walnut", "black", "white"):
    rail_t = T("wood_" + key) if key in WOODS else T("paint_" + key)
    bal_t = T("paint_black") if key == "black" else T("paint_trim")
    railing("railing_baluster_" + key, lambda s: balusters(s, False), "GUARD", key in WOODS,
            {"rail": rail_t, "baluster": bal_t, "newel": rail_t},
            names(RAILING, SQUARE, FIN[key]))
TURNED = ("Turned Balusters", "Gedrechselte Stäbe", "Balaustres Torneados", "Svarvade Balustrar")
for key in ("oak", "walnut", "cherry"):
    railing("railing_turned_" + key, lambda s: balusters(s, True), "GUARD", True,
            {"rail": T("wood_" + key), "baluster": T("turned"), "newel": T("wood_" + key)},
            names(RAILING, TURNED, FIN[key]))

# ------------------------------------------------------------------------------------------
# Writing the railings
# ------------------------------------------------------------------------------------------
OBJ_QUADS = {}
EDGES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def write_obj(reg, shape, polys, mats):
    mtl = reg
    o = gw.Obj(mtl, "%s_%s" % (reg, shape))
    for p in polys:
        assert p.mat in mats, (reg, p.mat)
        o.add(p)
    rel = "models/block/stairsrailings/%s/%s.obj" % (reg, shape)
    C.extra[rel] = o.text().replace("gen_utilities_water.py", GEN, 1)
    OBJ_QUADS["%s/%s" % (reg, shape)] = o.count
    return "csm:stairsrailings/%s/%s.obj" % (reg, shape)


def write_mtl(reg, mats):
    text = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % GEN]
    for mat, tex in sorted(mats.items()):
        text += ["newmtl " + mat, "map_Kd " + tex]
    C.extra["models/block/stairsrailings/%s/%s.mtl" % (reg, reg)] = "\n".join(text) + "\n"


def icon_model(reg, polys, mats):
    """The item: the level cell's polygons as JSON boxes, where they are boxes, and the planes
    as thin boxes; good enough for a 16 px slot."""
    els = []
    for p in polys:
        xs = [q[0] * 16 for q in p.pts]
        ys = [q[1] * 16 for q in p.pts]
        zs = [q[2] * 16 for q in p.pts]
        lo = [min(xs), min(ys), min(zs)]
        hi = [max(xs), max(ys), max(zs)]
        for i in range(3):
            if hi[i] - lo[i] < 0.05:
                hi[i] = lo[i] + 0.05
        n = p.ns[0]
        axis = max(range(3), key=lambda i: abs(n[i]))
        face = (("west", "east"), ("down", "up"), ("north", "south"))[axis][1 if n[axis] > 0 else 0]
        els.append({"from": [round(v, 3) for v in lo], "to": [round(v, 3) for v in hi],
                    "faces": {face: {"texture": "#" + p.mat, "uv": [0, 0, 16, 16]}}})
    tex = dict(mats)
    tex["particle"] = mats["rail"]
    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0],
                "scale": [0.7, 0.7, 0.7]},
        "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
    }
    return {"parent": "block/block", "textures": tex, "elements": els, "display": display}


def when(**cond):
    return {k: ("true" if v is True else "false" if v is False else v) for k, v in cond.items()}


def railings():
    for reg, build, kind, is_wood, mats, display in RAILINGS:
        write_mtl(reg, mats)
        models = {}
        for shape in SHAPES:
            polys = build(shape)
            models[shape] = write_obj(reg, shape, polys, mats)
            if shape == "level":
                icon = icon_model(reg, polys, mats)
        rules = []
        for edge, y in EDGES:
            rules.append({"when": when(**{edge: True, "slope": "none"}),
                          "apply": dict({"model": models["level"]}, **({"y": y} if y else {}))})
            for shape in SLOPED:
                # slope is world-relative (pos climbs east or south); turned to the south or
                # west edge, the model's east points the other way, so its mirror is drawn
                drawn = shape
                if edge in ("south", "west"):
                    drawn = shape.replace("_pos", "_x").replace("_neg", "_pos").replace("_x",
                                                                                         "_neg")
                rules.append({"when": when(**{edge: True, "slope": shape}),
                              "apply": dict({"model": models[drawn]}, **({"y": y} if y else {}))})
        C.add(reg, 'new BlockStairRailing("%s", BlockStairRailing.Kind.%s, %s)'
              % (reg, kind, "true" if is_wood else "false"), display, {},
              {"multipart": rules}, item=icon, tab=TAB)


# ------------------------------------------------------------------------------------------
# Stairs
# ------------------------------------------------------------------------------------------
# Vanilla's stair volumes, the model climbing east (+x): (lower step, upper steps)
STAIR_SHAPES = {
    "straight": [((8, 8, 0), (16, 16, 16))],
    "inner": [((8, 8, 0), (16, 16, 16)), ((0, 8, 8), (8, 16, 16))],
    "outer": [((8, 8, 8), (16, 16, 16))],
}
# Where each tread's front edge is, for its nosing: (x0, z0, x1, z1) along the edge at y 8 / 16
NOSINGS = {
    "straight": [((0, 0), (0, 16), 8), ((8, 0), (8, 16), 16)],
    "inner": [((0, 0), (0, 8), 8), ((0, 0), (8, 0), 8), ((8, 0), (8, 8), 16),
              ((0, 8), (8, 8), 16)],
    "outer": [((0, 0), (0, 16), 8), ((0, 0), (16, 0), 8), ((8, 8), (8, 16), 16),
              ((8, 8), (16, 8), 16)],
}
# The part of the lower step's top that no upper step covers, (x0, z0), (x1, z1)
LOWER = {
    "straight": [((0, 0), (8, 16))],
    "inner": [((0, 0), (8, 8))],
    "outer": [((0, 0), (16, 8)), ((0, 8), (8, 16))],
}


def stair_box(frm, to, tex, faces):
    return um.box(list(frm), list(to), tex, faces=faces)


def stair_models(kind):
    """Elements of the straight, inner and outer models for a stair kind:
    'solid' (body + nosing strips), 'treads' (overhanging timber treads on a painted body),
    'open' (treads only, on a steel stringer)."""
    out = {}
    for shape, uppers in STAIR_SHAPES.items():
        els = []
        if kind in ("solid", "treads"):
            top = 8 if kind == "solid" else 7
            els.append(stair_box((0, 0, 0), (16, top, 16), "riser",
                                 ("north", "south", "east", "west", "down", "up")))
            for frm, to in uppers:
                els.append(stair_box((frm[0], top, frm[2]), (to[0], 16 if kind == "solid" else 15,
                                                              to[2]), "riser",
                                     ("north", "south", "east", "west", "up")))
            if kind == "solid":
                for (x0, z0), (x1, z1), y in NOSINGS[shape]:
                    if x0 == x1:
                        els.append(stair_box((x0, y - 0.3, z0), (x0 + 1.5, y + 0.15, z1), "nosing",
                                             ("up", "west", "north", "south")))
                    else:
                        els.append(stair_box((x0, y - 0.3, z0), (x1, y + 0.15, z0 + 1.5),
                                             "nosing", ("up", "north", "east", "west")))
        if kind in ("treads", "open"):
            # Treads over the exposed part of each step; a straight flight's overhang its front
            lower_y, upper_y = (7, 15) if kind == "treads" else (6, 14)
            over = 1 if kind == "treads" and shape == "straight" else 0
            for (x0, z0), (x1, z1) in LOWER[shape]:
                els.append(stair_box((x0 - over, lower_y, z0), (x1, 8, z1), "tread",
                                     ("north", "south", "east", "west", "up", "down")))
            for frm, to in uppers:
                els.append(stair_box((frm[0] - over, upper_y, frm[2]), (to[0], 16, to[2]),
                                     "tread", ("north", "south", "east", "west", "up", "down")))
        if kind == "open" and shape == "straight":
            # The stringer: a steel channel under the treads' middle, at 45 degrees
            e = stair_box((-3.3, 6.8, 7.2), (19.3, 9.2, 8.8), "stringer",
                          ("north", "south", "up", "down", "east", "west"))
            e["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": 45}
            for f in e["faces"].values():
                f["uv"] = [0, 0, 16, 2]
            els.append(e)
        out[shape] = els
    return out


VANILLA_STAIRS = []
for _half, _x in (("bottom", 0), ("top", 180)):
    for _shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
        for _facing in ("east", "west", "south", "north"):
            base = {"east": 0, "south": 90, "west": 180, "north": 270}[_facing]
            if _shape.endswith("_left"):
                base = (base + (270 if _half == "bottom" else 0)) % 360
            elif _shape.endswith("_right") and _half == "top":
                base = (base + 90) % 360
            VANILLA_STAIRS.append((_facing, _half, _shape, _x, base))

STAIRS = (
    ("stairs_steel_pan_grey", "solid", False,
     {"riser": T("paint_grey"), "nosing": T("nosing_dark"), "tread": T("concrete")},
     ("Steel Pan Stairs (Grey)", "Stahlwannentreppe (Grau)", "Escalera de Bandeja de Acero (Gris)",
      "Ståltrappa med Betongsteg (Grå)")),
    ("stairs_concrete_nosing", "solid", False,
     {"riser": T("concrete"), "nosing": T("nosing_dark"), "tread": T("concrete")},
     ("Concrete Stairs (Safety Nosing)", "Betontreppe (Trittkantenprofil)",
      "Escalera de Hormigón (Mamperlán Antideslizante)", "Betongtrappa (Halkskydd)")),
    ("stairs_open_tread_oak", "open", True,
     {"tread": T("wood_oak"), "stringer": T("paint_black")},
     ("Open Tread Stairs (Oak)", "Offene Treppe (Eiche)", "Escalera de Peldaños Abiertos (Roble)",
      "Öppen Trappa (Ek)")),
    ("stairs_residential_oak", "treads", True,
     {"tread": T("wood_oak"), "riser": T("paint_trim")},
     ("Residential Stairs (Oak)", "Wohnhaustreppe (Eiche)", "Escalera Residencial (Roble)",
      "Bostadstrappa (Ek)")),
    ("stairs_residential_walnut", "treads", True,
     {"tread": T("wood_walnut"), "riser": T("paint_trim")},
     ("Residential Stairs (Walnut)", "Wohnhaustreppe (Nussbaum)", "Escalera Residencial (Nogal)",
      "Bostadstrappa (Valnöt)")),
    ("stairs_carpet_grey", "treads", True,
     {"tread": T("carpet_grey"), "riser": T("carpet_grey")},
     ("Carpeted Stairs (Grey)", "Teppichtreppe (Grau)", "Escalera Enmoquetada (Gris)",
      "Heltäckningsmattad Trappa (Grå)")),
)


def stairs():
    for reg, kind, is_wood, tex, display in STAIRS:
        t = dict(tex)
        t["particle"] = tex.get("tread", tex.get("riser"))
        els = stair_models(kind)
        models = {}
        for shape, e in els.items():
            models["%s_%s" % (reg, shape)] = {"parent": "block/block", "textures": t,
                                              "elements": e}
        variants = {}
        for facing, half, shape, x, y in VANILLA_STAIRS:
            model = "%s_%s" % (reg, shape.split("_")[0])
            v = {"model": C.M(model)}
            if x:
                v["x"] = x
            if y:
                v["y"] = y
            variants["facing=%s,half=%s,shape=%s" % (facing, half, shape)] = v
        item = {"parent": C.M("%s_straight" % reg).replace("csm:", "csm:block/"),
                "display": {"gui": {"rotation": [30, 135, 0], "scale": [0.625, 0.625, 0.625]},
                            "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                            "fixed": {"scale": [0.5, 0.5, 0.5]},
                            "thirdperson_righthand": {"rotation": [75, 135, 0],
                                                      "translation": [0, 2.5, 0],
                                                      "scale": [0.375, 0.375, 0.375]},
                            "firstperson_righthand": {"rotation": [0, 45, 0],
                                                      "scale": [0.4, 0.4, 0.4]},
                            "firstperson_lefthand": {"rotation": [0, 225, 0],
                                                     "scale": [0.4, 0.4, 0.4]}}}
        C.add(reg, 'new BlockStairFlight("%s", %s)' % (reg, "true" if is_wood else "false"),
              display, models, {"variants": variants}, item=item, tab=TAB)


stairs()
railings()
C.add_lang("itemGroup.tabstairsrailings", ("CSM: Stairs & Railings", "CSM: Treppen & Geländer",
                                           "CSM: Escaleras y Barandillas",
                                           "CSM: Trappor & Räcken"))


if __name__ == "__main__":
    if "--report" in sys.argv:
        for k in sorted(OBJ_QUADS):
            print("%-48s %4d quads" % (k, OBJ_QUADS[k]))
        sys.exit(0)
    sys.exit(C.main())
