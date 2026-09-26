#!/usr/bin/env python3
"""Every asset the Utilities tab's water system ships: the build-to-size water tower (legs,
riser, cross bracing and struts, the caged ladder, the pedestal column and the tank bowls with
their balcony and name band) and the ground storage tank.

    python dev-env-utils/scripts/gen_utilities_water.py
    python dev-env-utils/scripts/gen_utilities_water.py --check
    python dev-env-utils/scripts/gen_utilities_water.py --fragments   # tab lines to paste
    python dev-env-utils/scripts/gen_utilities_water.py --report      # the tanks' OBJ quads

What is here, and the class that places each (package powergrid.water unless named):

- **The water tower is stacked and arranged, not placed whole.** Legs and the riser
  (BlockTowerColumn) are one-block sections that pick a footing or a cap from their neighbours
  like the poles; the cross bracing (BlockTowerBrace) is a tie rod corner to corner of a block,
  so a square panel between two legs is braced by placing rods along its diagonals, and struts
  (BlockTowerStrut) join legs level; the caged ladder (BlockCagedLadder) climbs anything. The
  pedestal column (BlockPedestalSection) is a three-block-wide section with invisible parts
  round it, stacked the same way.
- **The tank bowl is a few fixed sizes placed whole**: a grid of three-block tiles, each tile's
  root drawing its own share of the bowl (an OBJ lathe cut at the tile's faces) and invisible
  parts (BlockTankPart) filling the solid for collision. A tile finds where it sits in the grid
  by counting the tiles beside and below it (actual state), so nothing is stored per tile and
  one registry name serves the whole bowl. The band tiles (BlockTankBand) carry the town name,
  cycled by a click. See Shape below and UTILITIES_SYSTEM.md, "The water system".
- **The ground storage tank** (BlockGroundTank) is the same tiles, one three-block layer a unit,
  stacked to any height; each layer draws its footing or roof by whether a layer is below or
  above it.

The generator also writes TankShapes.java: every bowl's cell map (which cells hold a part, and
the balcony's walkway parts with their railing sides), measured from the same profiles the OBJ
lathe draws, so the collision and the model cannot disagree.

Every text write keeps the line endings the file already has on disk, and a file whose content
has not changed is not touched (gen_utilities_meters.Catalogue).
"""
import math
import os
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_utilities_meters as um  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

REPO = um.REPO
ASSETS = um.ASSETS
JAVA_DIR = os.path.join(REPO, "modules", "powergrid", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "powergrid", "water")
TAB = "CsmTabUtilities"
HIDDEN = "CsmTabUtilitiesHidden"
FACINGS = um.FACINGS

C = um.Catalogue("gen_utilities_water.py", "utilities/water", "utilities/water", assets=ASSETS)
box, fmt, extent = um.box, um.fmt, um.extent
TAN8 = math.tan(math.pi / 8)
TAN16 = math.tan(math.pi / 16)


def names_of(*n):
    assert len(n) == 4
    return n


def M(name):
    return C.M(name)


def T(name):
    return C.T(name)


def model(tex, els, display=None, ao=True):
    m = {"parent": "block/block", "textures": dict(tex)}
    if not ao:
        m["ambientocclusion"] = False
    if els:
        m["elements"] = els
    if display:
        m["display"] = display
    return m


def rule(name, when=None, y=0):
    r = {"apply": {"model": M(name)}}
    if y:
        r["apply"]["y"] = y
    if when:
        r["when"] = {k: (("true" if v else "false") if isinstance(v, bool) else str(v))
                     for k, v in when.items()}
    return r


def multipart(rules, inventory=None):
    out = {"multipart": rules}
    if inventory:
        out = {"variants": {"inventory": {"model": M(inventory)}}, "multipart": rules}
    return out


def facing_rules(parts):
    """[(model name, conditions)] each turned to the four facings (the model faces north)."""
    out = []
    for name, cond in parts:
        for f, y in FACINGS:
            w = {"facing": f}
            w.update(cond)
            out.append(rule(name, w, y))
    return out


ICON_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
}


def big_display(scale):
    """Display transforms for an item model drawn at world size past its block (a two-cell
    unit): the block/block ones, scaled down."""
    s = scale
    return {"gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [s, s, s]},
            "fixed": {"translation": [0, -2, 0], "scale": [s * 1.05, s * 1.05, s * 1.05]},
            "ground": {"translation": [0, 2, 0], "scale": [s * 0.5, s * 0.5, s * 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 1.5],
                                      "scale": [s * 0.6, s * 0.6, s * 0.6]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0],
                                      "scale": [s * 0.65, s * 0.65, s * 0.65]}}


# ------------------------------------------------------------------------------------------
# Colours and textures
# ------------------------------------------------------------------------------------------
PAINT = (226, 230, 231)          # the tower's white
GRATING = (150, 156, 158)
DARK = (66, 70, 74)
CONCRETE = (170, 168, 162)
TANK_TAN = (205, 192, 162)
BAND_BLUE = (28, 62, 132)
STEEL = (170, 174, 176)

for _name, _colour, _seed, _grain in (
        ("paint", PAINT, 101, 2), ("dark", DARK, 102, 3), ("concrete", CONCRETE, 103, 7),
        ("steel", STEEL, 106, 3), ("tank_roof", lc.shade(TANK_TAN, 1.05), 110, 3)):
    C.textures[_name] = (lambda c=_colour, s=_seed, g=_grain: lc.fill(c, 16, g, s))


@C.texture("grating")
def _grating():
    """The balcony's bar grating seen from above: bearing bars two texels apart, a cross bar
    every eight."""
    img = lc.fill(GRATING, 16, 3, 121)
    for y in range(16):
        for x in range(16):
            if x % 2 == 1:
                img.putpixel((x, y), lc.shade(GRATING, 0.62) + (255,))
            if y % 8 == 0:
                img.putpixel((x, y), lc.shade(GRATING, 1.08) + (255,))
    return img


@C.texture("railing")
def _railing():
    """The handrail seen side on, cut out: top rail, knee rail and toe plate, and a post at each
    edge and in the middle (the texture spans one facet of the ring)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rail = PAINT
    lc.rect(img, 0, 0, 32, 2, lc.shade(rail, 1.0))
    lc.rect(img, 0, 2, 32, 3, lc.shade(rail, 0.8))
    lc.rect(img, 0, 14, 32, 16, lc.shade(rail, 0.94))
    lc.rect(img, 0, 27, 32, 32, lc.shade(rail, 0.9))
    for x0 in (0, 15, 30):
        lc.rect(img, x0, 0, x0 + 2, 32, lc.shade(rail, 0.97))
    return img


@C.texture("light")
def _light():
    """The obstruction light on the finial: a red lens that flashes, as an animated strip."""
    strip = Image.new("RGBA", (16, 32))
    for f, c in enumerate(((255, 44, 30), (96, 22, 20))):
        img = lc.fill(c, 16, 3, 131 + f)
        lc.disc(img, 8, 8, 4, lc.shade(c, 1.15) if f == 0 else c)
        strip.paste(img, (0, 16 * f))
    return strip


@C.texture("light_e")
def _light_e():
    """OptiFine's emissive companion: the lens only while it is lit."""
    strip = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    img = lc.fill((255, 60, 44), 16, 3, 133)
    strip.paste(img, (0, 0))
    return strip


C.extra["textures/blocks/utilities/water/light.png.mcmeta"] = um.mcmeta(20)
C.extra["textures/blocks/utilities/water/light_e.png.mcmeta"] = um.mcmeta(20)


@C.texture("tank_wall")
def _tank_wall():
    """A ground storage tank's shell plate: 32 texels over one facet and one three-block layer,
    the course seam along the top and half way down, the vertical seam at the facet's edge."""
    img = lc.fill(TANK_TAN, 32, 3, 141)
    for x in range(32):
        img.putpixel((x, 0), lc.shade(TANK_TAN, 0.8) + (255,))
        img.putpixel((x, 16), lc.shade(TANK_TAN, 0.86) + (255,))
    for y in range(32):
        img.putpixel((0, y), lc.shade(TANK_TAN, 0.86) + (255,))
    return img


# ------------------------------------------------------------------------------------------
# The name band: invented town names, one 16-texel row each
# ------------------------------------------------------------------------------------------
BAND_NAMES = ["OAKDALE", "BRIARTON", "ASHBROOK", "CEDARVALE", "GLENMARSH", "NORTHWICK",
              "WILLOWBY", ""]
BAND_ROWS = len(BAND_NAMES)
assert BAND_ROWS == 8


@C.texture("band")
def _band():
    """Eight 128 x 16 rows: the band's stripe with a name in white, three-times the pixel font,
    and the last row the stripe alone (the blank band)."""
    img = Image.new("RGBA", (128, 128))
    for i, name in enumerate(BAND_NAMES):
        y0 = 16 * i
        stripe = lc.fill(BAND_BLUE, 128, 3, 191 + i)
        img.paste(stripe.crop((0, 0, 128, 16)), (0, y0))
        lc.rect(img, 0, y0, 128, y0 + 1, lc.shade(BAND_BLUE, 1.35))
        lc.rect(img, 0, y0 + 15, 128, y0 + 16, lc.shade(BAND_BLUE, 1.35))
        if name:
            assert lc.text_width(name, 3) <= 124, name
            lc.draw_text_centred(img, name, 64, y0 + 1, (246, 246, 240), scale=3)
    return img


# ------------------------------------------------------------------------------------------
# OBJ: polygons in block space, clipped at tile faces
# ------------------------------------------------------------------------------------------
def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _norm(v):
    ln = math.sqrt(_dot(v, v))
    return (v[0] / ln, v[1] / ln, v[2] / ln) if ln > 1e-12 else (0.0, 1.0, 0.0)


def _lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(len(a)))


class Poly(object):
    """A convex polygon: points, their uvs (0..1, v down the texture) and normals, a material,
    and a tag (the name band's zone carries ("band", side))."""

    __slots__ = ("pts", "uvs", "ns", "mat", "tag")

    def __init__(self, pts, uvs, ns, mat, tag=None, outward=None):
        self.pts, self.uvs, self.ns, self.mat, self.tag = list(pts), list(uvs), list(ns), mat, tag
        if outward is not None:
            n = self.area_normal()
            if _dot(n, outward) < 0:
                self.pts.reverse()
                self.uvs.reverse()
                self.ns.reverse()

    def area_normal(self):
        n = (0.0, 0.0, 0.0)
        p0 = self.pts[0]
        for i in range(1, len(self.pts) - 1):
            c = _cross(_sub(self.pts[i], p0), _sub(self.pts[i + 1], p0))
            n = (n[0] + c[0], n[1] + c[1], n[2] + c[2])
        return n

    def area(self):
        return math.sqrt(_dot(self.area_normal(), self.area_normal())) / 2

    def moved(self, d):
        return Poly([(p[0] + d[0], p[1] + d[1], p[2] + d[2]) for p in self.pts], self.uvs,
                    self.ns, self.mat, self.tag)

    def clip(self, axis, bound, keep_above):
        """Sutherland-Hodgman against one plane, or None if nothing is left."""
        out_p, out_t, out_n = [], [], []
        n = len(self.pts)

        def inside(p):
            return p[axis] >= bound - 1e-9 if keep_above else p[axis] <= bound + 1e-9
        for i in range(n):
            a, b = self.pts[i], self.pts[(i + 1) % n]
            ta, tb = self.uvs[i], self.uvs[(i + 1) % n]
            na, nb = self.ns[i], self.ns[(i + 1) % n]
            ia, ib = inside(a), inside(b)
            if ia:
                out_p.append(a)
                out_t.append(ta)
                out_n.append(na)
            if ia != ib:
                t = (bound - a[axis]) / (b[axis] - a[axis])
                p = list(_lerp(a, b, t))
                p[axis] = bound
                out_p.append(tuple(p))
                out_t.append(_lerp(ta, tb, t))
                out_n.append(_norm(_lerp(na, nb, t)))
        if len(out_p) < 3:
            return None
        q = Poly(out_p, out_t, out_n, self.mat, self.tag)
        return q if q.area() > 1e-7 else None


def clip_box(polys, lo, hi):
    out = []
    for p in polys:
        q = p
        for axis in range(3):
            if q is not None:
                q = q.clip(axis, lo[axis], True)
            if q is not None:
                q = q.clip(axis, hi[axis], False)
        if q is not None:
            out.append(q)
    return out


class Obj(object):
    """An OBJ in 0..1 block space with its materials in a shared MTL; every face carries its
    normals (OptiFine needs them), v runs down the texture (no flip-v, since a multipart part
    cannot pass Forge's custom data)."""

    def __init__(self, mtl, name):
        self.mtl, self.name = mtl, name
        self.lines_v, self.lines_t, self.lines_n, self.faces = [], [], [], []
        self.count = 0

    def add(self, poly):
        pts = poly.pts
        # a polygon of more than four corners is fanned into quads and a triangle
        idx = list(range(len(pts)))
        pieces = []
        while len(idx) > 4:
            pieces.append([idx[0], idx[1], idx[2], idx[3]])
            idx = [idx[0]] + idx[3:]
        pieces.append(idx)
        for piece in pieces:
            refs = []
            for i in piece:
                p, t, n = pts[i], poly.uvs[i], poly.ns[i]
                assert -1e-6 <= t[0] <= 1 + 1e-6 and -1e-6 <= t[1] <= 1 + 1e-6, (self.name, t)
                self.lines_v.append("v %.5f %.5f %.5f" % (p[0] + 0.0, p[1] + 0.0, p[2] + 0.0))
                self.lines_t.append("vt %.5f %.5f" % (min(1, max(0, t[0])),
                                                      min(1, max(0, t[1]))))
                self.lines_n.append("vn %.5f %.5f %.5f" % n)
                refs.append(len(self.lines_v))
            self.faces.append((poly.mat, refs))
            self.count += 1

    def text(self):
        lines = ["# Generated by dev-env-utils/scripts/gen_utilities_water.py -- do not edit",
                 "mtllib %s.mtl" % self.mtl, "o %s" % self.name]
        lines += self.lines_v + self.lines_t + self.lines_n
        current = None
        for mat, refs in sorted(self.faces, key=lambda f: f[0]):
            if mat != current:
                lines.append("usemtl " + mat)
                current = mat
            lines.append("f " + " ".join("%d/%d/%d" % (r, r, r) for r in refs))
        return "\n".join(lines) + "\n"


# Without flip-v, Forge's OBJ loader takes vt's v as the row down the sprite, so v is written
# as it is kept here: a uv of (u, 0) is the top of the texture, as in a JSON model.

MTLS = {}


def mtl(name, materials):
    MTLS[name] = materials
    text = ["# Generated by dev-env-utils/scripts/gen_utilities_water.py -- do not edit"]
    for mat, tex in materials.items():
        text += ["newmtl " + mat, "map_Kd " + tex]
    C.extra["models/block/utilities/water/%s.mtl" % name] = "\n".join(text) + "\n"


def write_obj(name, mtl_name, polys, dx=(0, 0, 0)):
    o = Obj(mtl_name, name)
    for p in polys:
        o.add(p.moved(dx) if dx != (0, 0, 0) else p)
    C.extra["models/block/utilities/water/%s.obj" % name] = o.text()
    OBJ_QUADS[name] = o.count
    return "csm:utilities/water/%s.obj" % name


OBJ_QUADS = {}

# ------------------------------------------------------------------------------------------
# Lathes. Angle phi is measured from north toward east, so direction (sin phi, 0, -cos phi);
# facet k of a 16-gon faces phi = k * 22.5 degrees (facet 0 north), an 8-gon's facet k faces
# k * 45. r is the inradius, measured to the facets, as the JSON rounds measure theirs.
# ------------------------------------------------------------------------------------------
def dirv(phi):
    return (math.sin(phi), -math.cos(phi))


def lathe_band(p0, p1, n0, n1, mat, sides=16, tag_fn=None, v=(1.0, 0.0),
               facets=None, outward=True):
    """The surface swept by the profile segment p0 -> p1 ((r, y) in blocks, about the axis at
    x = z = 0), one polygon a facet, with profile normals n0, n1 ((nr, ny)) swept round smoothly.
    A point at r = 0 closes a cone to the axis. v gives the texture row at p0 and at p1: by
    default the bottom of the texture at the bottom of a rising segment."""
    half = math.pi / sides
    polys = []
    for k in (facets if facets is not None else range(sides)):
        pa, pb = k * 2 * half - half, k * 2 * half + half
        pts, uvs, ns = [], [], []
        for (r, y), (nr, ny), phi, u, vv in ((p0, n0, pa, 0.0, v[0]), (p0, n0, pb, 1.0, v[0]),
                                              (p1, n1, pb, 1.0, v[1]), (p1, n1, pa, 0.0, v[1])):
            R = r / math.cos(half)
            dx, dz = dirv(phi)
            pts.append((R * dx, y, R * dz))
            uvs.append((u, vv))
            ns.append(_norm((nr * dx, ny, nr * dz)))
        # drop a corner that repeats (the cone's point)
        dedup_p, dedup_t, dedup_n = [], [], []
        for i in range(4):
            if dedup_p and max(abs(pts[i][j] - dedup_p[-1][j]) for j in range(3)) < 1e-9:
                continue
            dedup_p.append(pts[i])
            dedup_t.append(uvs[i])
            dedup_n.append(ns[i])
        if len(dedup_p) > 3 and max(abs(dedup_p[0][j] - dedup_p[-1][j]) for j in range(3)) < 1e-9:
            dedup_p.pop()
            dedup_t.pop()
            dedup_n.pop()
        if len(dedup_p) < 3:
            continue
        nav = _norm(tuple(sum(n[j] for n in dedup_n) for j in range(3)))
        if not outward:
            nav = (-nav[0], -nav[1], -nav[2])
            dedup_n = [(-n[0], -n[1], -n[2]) for n in dedup_n]
        tag = tag_fn(k) if tag_fn else None
        polys.append(Poly(dedup_p, dedup_t, dedup_n, mat, tag, outward=nav))
    return polys


def profile_normal(p0, p1):
    """The outward normal (nr, ny) of a profile segment running up or out."""
    dr, dy = p1[0] - p0[0], p1[1] - p0[1]
    ln = math.hypot(dr, dy)
    return (dy / ln, -dr / ln)


def lathe_profile(points, mat, smooth=True, sides=16, tag_fn=None):
    """Sweeps a profile polyline [(r, y), ...] from bottom to top; with smooth, the normals are
    averaged at the joints so the shading runs on round the bend."""
    segs = list(zip(points[:-1], points[1:]))
    seg_n = [profile_normal(a, b) for a, b in segs]
    out = []
    for i, (a, b) in enumerate(segs):
        na, nb = seg_n[i], seg_n[i]
        if smooth:
            if i > 0:
                na = _norm2(seg_n[i - 1], seg_n[i])
            if i < len(segs) - 1:
                nb = _norm2(seg_n[i], seg_n[i + 1])
        out += lathe_band(a, b, na, nb, mat, sides=sides, tag_fn=tag_fn)
    return out


def _norm2(a, b):
    x, y = a[0] + b[0], a[1] + b[1]
    ln = math.hypot(x, y)
    return (x / ln, y / ln)


def disc(r, y, mat, up=True, sides=16, r_in=0.0):
    """A flat 16-gon (or annulus) facing up or down, uv in polar strips (u across the facet,
    v from inside to outside)."""
    n = (0.0, 1.0, 0.0) if up else (0.0, -1.0, 0.0)
    half = math.pi / sides
    polys = []
    for k in range(sides):
        pa, pb = k * 2 * half - half, k * 2 * half + half
        pts, uvs = [], []
        for rr, phi, u, v in ((r_in, pa, 0, 0), (r_in, pb, 1, 0), (r, pb, 1, 1), (r, pa, 0, 1)):
            R = rr / math.cos(half)
            dx, dz = dirv(phi)
            pts.append((R * dx, y, R * dz))
            uvs.append((u, v))
        if r_in <= 0:
            pts, uvs = pts[1:], uvs[1:]
            uvs[0] = (0.5, 0)
        polys.append(Poly(pts, uvs, [n] * len(pts), mat, outward=n))
    return polys


def prism(cx, cz, r, y0, y1, mat, sides=8, top=True, bottom=False):
    """A vertical n-gon prism about (cx, cz) (the JSON rounds' orientation: flats square to the
    axes), with its caps where asked."""
    polys = lathe_band((r, y0), (r, y1), (1, 0), (1, 0), mat, sides=sides)
    if top:
        polys += disc(r, y1, mat, up=True, sides=sides)
    if bottom:
        polys += disc(r, y0, mat, up=False, sides=sides)
    return [p.moved((cx, 0, cz)) for p in polys]


def ring_wall(r, y0, y1, mat, sides=16, v=(0.0, 1.0)):
    """A cut-out band (a railing) both ways round: the outside facing out, the inside in."""
    out = lathe_band((r, y0), (r, y1), (1, 0), (1, 0), mat, sides=sides, v=(v[1], v[0]))
    inner = lathe_band((r, y0), (r, y1), (1, 0), (1, 0), mat, sides=sides, v=(v[1], v[0]),
                       outward=False)
    return out + inner


# ------------------------------------------------------------------------------------------
# Tank shapes: a grid of three-block tiles
# ------------------------------------------------------------------------------------------
FACET_N = [(math.sin(k * math.pi / 8), -math.cos(k * math.pi / 8)) for k in range(16)]
BAND_SIDES = {15: 0, 0: 0, 1: 0, 3: 1, 4: 1, 5: 1, 7: 2, 8: 2, 9: 2, 11: 3, 12: 3, 13: 3}


def d16(x, z):
    """How far (x, z) is from the axis measured as the 16-gon measures its inradius: a point
    is inside a 16-gon of inradius r when this is at most r."""
    return max(x * n[0] + z * n[1] for n in FACET_N)


def radius_at(outline, y):
    """The outline's radius at height y, or None above or below it ([(r, y)] rising)."""
    for (r0, y0), (r1, y1) in zip(outline[:-1], outline[1:]):
        if y0 <= y <= y1 and y1 > y0:
            return r0 + (r1 - r0) * (y - y0) / (y1 - y0)
    return None


def rot90(gi, gj, n):
    """Where a tile goes when the grid turns as a blockstate's y: 90 turns a model (north to
    east): (u, v) -> (-v, u) about the centre tile."""
    c = (n - 1) / 2.0
    u, v = gi - c, gj - c
    return int(round(c - v)), int(round(c + u))


def canonical(gi, gj, n):
    """(canonical tile, quarter turns) with rot90^turns(canonical) = (gi, gj)."""
    orbit = [(gi, gj)]
    for _ in range(3):
        orbit.append(rot90(orbit[-1][0], orbit[-1][1], n))
    rep = min(orbit, key=lambda t: (t[1], t[0]))
    t, k = rep, 0
    while t != (gi, gj):
        t = rot90(t[0], t[1], n)
        k += 1
    return rep, k


class Shape(object):
    """A tank drawn by a grid of N x N x L three-block tiles about a vertical axis through the
    centre cell (so N is odd), the grid's bottom at y = 0. ``polys`` is the whole drawing in
    blocks about the axis; ``cells`` the collision map; a band shape also has the band."""

    def __init__(self, key, reg, n, layers, mtl_name, stackable=False):
        self.key, self.reg, self.n, self.layers = key, reg, n, layers
        self.w = 3 * n
        self.h = (self.w - 1) // 2
        self.height = 3 * layers
        self.mtl = mtl_name
        self.stackable = stackable
        self.polys = []
        self.outline = []
        self.cells = {}
        self.band = None      # (layer, slots: gi values on the north row, y0, y1)
        self.band_polys = []  # the decal on the north facets, u 0..1 across what shows
        self.band_arc = 0.0

    def tile_box(self, gi, gj, gk, y_lo=None, y_hi=None):
        x0 = 3 * gi - self.h - 0.5
        z0 = 3 * gj - self.h - 0.5
        y0 = 3 * gk
        return ((x0, y0 if y_lo is None else y_lo, z0),
                (x0 + 3, y0 + 3 if y_hi is None else y_hi, z0 + 3))

    def root(self, gi, gj, gk):
        """A tile's root cell, as the cell whose centre is at (x, z) about the axis."""
        return 3 * gi - self.h + 1, 3 * gk + 1, 3 * gj - self.h + 1

    def local(self, polys, gi, gj, gk):
        """Polygons about the axis moved into the root cell's own block space."""
        rx, ry, rz = self.root(gi, gj, gk)
        return [p.moved((-(rx - 0.5), -ry, -(rz - 0.5))) for p in polys]

    def classify(self, walkway=None, stubs=(), stub_top=0):
        """The collision map: '#' where a cell's centre is inside the tank (as the 16-gon
        measures it), the leg stubs' cells, and the balcony's walkway cells with a letter for
        the sides that take a railing ('a' + mask, north 1, east 2, south 4, west 8)."""
        solid = set()
        for y in range(self.height):
            r = radius_at(self.outline, y + 0.5)
            if r is None:
                continue
            for z in range(-self.h, self.h + 1):
                for x in range(-self.h, self.h + 1):
                    if d16(x, z) <= r:
                        solid.add((x, y, z))
        for (sx, sz) in stubs:
            for y in range(0, stub_top + 1):
                solid.add((sx, y, sz))
        cells = {c: "#" for c in solid}
        if walkway:
            y, r_out = walkway
            walk = set()
            for z in range(-self.h, self.h + 1):
                for x in range(-self.h, self.h + 1):
                    if (x, y, z) not in solid and d16(x, z) <= r_out:
                        walk.add((x, y, z))
            for (x, yy, z) in walk:
                mask = 0
                for bit, (dx, dz) in ((1, (0, -1)), (2, (1, 0)), (4, (0, 1)), (8, (-1, 0))):
                    nb = (x + dx, yy, z + dz)
                    if nb not in walk and nb not in solid:
                        mask |= bit
                assert mask != 15, (self.key, x, z)
                cells[(x, yy, z)] = chr(ord("a") + mask)
        self.cells = cells

    def cell_rows(self):
        out = []
        for y in range(self.height):
            for z in range(-self.h, self.h + 1):
                out.append("".join(self.cells.get((x, y, z), ".")
                                   for x in range(-self.h, self.h + 1)))
        return out


def band_tag(k):
    return ("band", BAND_SIDES[k]) if k in BAND_SIDES else None


def band_decal(shape, r, y0, y1):
    """The name band across the three north facets at radius r, cut to the band tiles' extent,
    u running 0..1 over what shows so a name sits centred on it."""
    facet_w = 2 * r * TAN16
    polys = []
    R = r / math.cos(math.pi / 16)
    for i, k in enumerate((15, 0, 1)):
        pa, pb = math.radians(k * 22.5 - 11.25), math.radians(k * 22.5 + 11.25)
        fn = (math.sin(k * math.pi / 8), 0.0, -math.cos(k * math.pi / 8))
        pts, uvs = [], []
        for phi, s, y in ((pa, i, y0), (pb, i + 1, y0), (pb, i + 1, y1), (pa, i, y1)):
            dx, dz = dirv(phi)
            pts.append((R * dx, y, R * dz))
            # seen from the north, east is on the left: the text starts at the east end
            uvs.append((1 - s / 3.0, 1.0 if y == y0 else 0.0))
        polys.append(Poly(pts, uvs, [fn] * 4, "band", outward=fn))
    slots = shape.band[1]
    x_lo = 3 * min(slots) - shape.h - 0.5
    x_hi = 3 * max(slots) - shape.h + 2.5
    cut = clip_box(polys, (x_lo, -99, -99), (x_hi, 99, 99))
    us = [t[0] for p in cut for t in p.uvs]
    u0, u1 = min(us), max(us)
    for p in cut:
        p.uvs = [((t[0] - u0) / (u1 - u0), t[1]) for t in p.uvs]
    return cut, (u1 - u0) * 3 * facet_w


def name_row(polys, row):
    """The band decal set to one name's row of the band sheet, kept half a texel clear of the
    rows either side."""
    lo, hi = (row + 0.6 / 16) / BAND_ROWS, (row + 1 - 0.6 / 16) / BAND_ROWS
    return [Poly(p.pts, [(t[0], lo + t[1] * (hi - lo)) for t in p.uvs], p.ns, p.mat)
            for p in polys]


TOWER_MTL = {"paint": T("paint"), "grating": T("grating"), "railing": T("railing"),
             "band": T("band"), "dark": T("dark"), "light": T("light")}
mtl("water_tower", TOWER_MTL)

LEG_R = 5.5 / 16       # the leg's inradius
RISER_R = 6.8 / 16
COLUMN_R = 23.2 / 16   # the pedestal column's inradius (16-gon, three blocks across)


def finial(y, r_fin, top, cap_r, sides=8):
    """The roof's centre vent: a stub, its mushroom cap and the obstruction light on top."""
    polys = prism(0, 0, r_fin, y, top, "paint", sides=sides, top=False)
    polys += prism(0, 0, cap_r, top, top + 0.12, "dark", sides=sides, top=True, bottom=True)
    polys += prism(0, 0, 0.12, top + 0.12, top + 0.3, "light", sides=8, top=True)
    return polys, top + 0.3


def multileg(key, reg, n, r, eq, head, cyl_top, slope, band, r_out, leg, layers):
    """A multi-leg elevated tank: ellipsoidal bottom, cylindrical shell, conical roof, the
    balcony round its equator and the legs' tops down to the grid's floor."""
    s = Shape(key, reg, n, layers, "water_tower")
    bottom = eq - head
    head_pts = [(r * math.sin(math.radians(t)), eq - head * math.cos(math.radians(t)))
                for t in range(0, 91, 15)]
    yb0, yb1 = band
    polys = lathe_profile(head_pts, "paint", smooth=True)
    polys += lathe_band((r, eq), (r, yb0), (1, 0), (1, 0), "paint")
    polys += lathe_band((r, yb0), (r, yb1), (1, 0), (1, 0), "paint", tag_fn=band_tag)
    polys += lathe_band((r, yb1), (r, cyl_top), (1, 0), (1, 0), "paint")
    r_fin = 0.3 if n == 3 else 0.45
    apex = cyl_top + (r - r_fin) * slope
    rn = profile_normal((r, cyl_top), (r_fin, apex))
    polys += lathe_band((r, cyl_top), (r_fin, apex), rn, rn, "paint")
    fin, top = finial(apex - 0.05, r_fin, apex + (0.35 if n == 3 else 0.45), r_fin + 0.2)
    polys += fin
    # the roof's handrail round the vent
    rr = 0.85 if n == 3 else 1.3
    ry = cyl_top + (r - rr) * slope
    polys += ring_wall(rr, ry, ry + 0.55, "railing", sides=8)
    # the balcony: grating on top, painted underneath, the toe plate and the handrail
    polys += disc(r_out, eq, "grating", up=True, r_in=r)
    polys += disc(r_out, eq - 0.1, "paint", up=False, r_in=r)
    polys += lathe_band((r_out, eq - 0.1), (r_out, eq + 0.12), (1, 0), (1, 0), "paint")
    polys += ring_wall(r_out - 0.04, eq, eq + 1.1, "railing")
    # the riser's top, into the bottom of the bowl, and the legs up to the balcony
    polys += prism(0, 0, RISER_R, 0, bottom + 0.15, "paint", top=False)
    for sx in (-leg, leg):
        for sz in (-leg, leg):
            polys += prism(sx, sz, LEG_R, 0, eq - 0.1, "paint", top=False)
    s.polys = polys
    s.outline = head_pts + [(r, yb0), (r, yb1), (r, cyl_top), (r_fin, apex), (r_fin, top)]
    s.band = (1, [(n - 1) // 2] if n == 3 else [1, 2, 3], yb0, yb1)
    s.band_polys, s.band_arc = band_decal(s, r, yb0, yb1)
    s.classify(walkway=(int(math.floor(eq)), r_out + 0.1),
               stubs=[(a, b) for a in (-leg, leg) for b in (-leg, leg)],
               stub_top=int(math.floor(eq - 0.1)))
    s.legs = leg
    assert top <= s.height, (key, top)
    return s


def radius_y(outline, r):
    """The height at which a falling outline (a dome) comes in to radius r."""
    for (r0, y0), (r1, y1) in zip(outline[:-1], outline[1:]):
        if r1 <= r <= r0 and r0 > r1:
            return y0 + (y1 - y0) * (r0 - r) / (r0 - r1)
    return outline[-1][1]


def spheroid(key, reg, n, lower, band, cyl_top, dome_h, layers):
    """A single-pedestal tank: a flared bottom rising from the column, a short cylindrical band
    at its widest, and a domed top."""
    s = Shape(key, reg, n, layers, "water_tower")
    r = lower[-1][0]
    yb0, yb1 = band
    polys = lathe_profile(lower + [(r, lower[-1][1] + 0.05)], "paint", smooth=True)
    if yb0 > lower[-1][1] + 0.05 + 1e-6:
        polys += lathe_band((r, lower[-1][1] + 0.05), (r, yb0), (1, 0), (1, 0), "paint")
    polys += lathe_band((r, yb0), (r, yb1), (1, 0), (1, 0), "paint", tag_fn=band_tag)
    polys += lathe_band((r, yb1), (r, cyl_top), (1, 0), (1, 0), "paint")
    r_fin = 0.3 if n == 3 else 0.45
    dome = [(r, cyl_top)]
    for t in range(10, 91, 10):
        a = math.radians(t)
        rr = r * math.cos(a)
        if rr <= r_fin:
            dome.append((r_fin, cyl_top + dome_h * math.sqrt(max(0, 1 - (r_fin / r) ** 2))))
            break
        dome.append((rr, cyl_top + dome_h * math.sin(a)))
    polys += lathe_profile(dome, "paint", smooth=True)
    apex = dome[-1][1]
    fin, top = finial(apex - 0.05, r_fin, apex + (0.25 if n == 3 else 0.35), r_fin + 0.18)
    polys += fin
    rr = 0.85 if n == 3 else 1.3
    ry = radius_y(dome, rr)
    polys += ring_wall(rr, ry, ry + 0.55, "railing", sides=8)
    s.polys = polys
    s.outline = lower + [(r, yb0), (r, yb1)] + dome + [(r_fin, top)]
    s.band = (1, [(n - 1) // 2] if n == 3 else [1, 2, 3], yb0, yb1)
    s.band_polys, s.band_arc = band_decal(s, r, yb0, yb1)
    s.classify()
    s.legs = None
    assert top <= s.height, (key, top)
    return s


BOWLS = [
    multileg("bowl_small", "water_tower_bowl_small", 3, r=3.4, eq=2.0625, head=1.7,
             cyl_top=4.0, slope=0.3, band=(3.08, 3.88), r_out=4.45, leg=3, layers=2),
    multileg("bowl_medium", "water_tower_bowl_medium", 5, r=6.2, eq=3.0625, head=2.9,
             cyl_top=6.3, slope=0.28, band=(4.2, 5.4), r_out=7.35, leg=5, layers=3),
    spheroid("spheroid_small", "water_tower_spheroid_small", 3,
             lower=[(COLUMN_R, 0.0), (2.1, 0.75), (2.85, 1.5), (3.55, 2.2), (4.0, 2.65),
                    (4.3, 3.0)], band=(3.05, 3.9), cyl_top=3.95, dome_h=1.4, layers=2),
    spheroid("spheroid_medium", "water_tower_spheroid_medium", 5,
             lower=[(COLUMN_R, 0.0), (2.6, 1.35), (3.9, 2.55), (5.2, 3.45), (6.3, 3.95),
                    (7.0, 4.15), (7.3, 4.35)], band=(4.4, 5.45), cyl_top=5.5, dome_h=2.75,
             layers=3),
]


# ------------------------------------------------------------------------------------------
# Ground storage tanks: one three-block layer a unit, stacked
# ------------------------------------------------------------------------------------------
mtl("ground_tank", {"wall": T("tank_wall"), "roof": T("tank_roof"), "concrete": T("concrete"),
                    "railing": T("railing"), "dark": T("dark")})


def ground_tank(key, reg, n, r):
    """A welded steel ground tank a layer at a time: the shell plate, the concrete ringwall
    drawn only under the lowest layer, and the roof (a shallow cone inside the top angle, the
    centre vent and the roof's handrail) only on the highest."""
    s = Shape(key, reg, n, 1, "ground_tank", stackable=True)
    s.wall = lathe_band((r, 0), (r, 3), (1, 0), (1, 0), "wall", v=(1.0, 0.0))
    footing = lathe_band((r + 0.2, 0), (r + 0.2, 0.35), (1, 0), (1, 0), "concrete")
    footing += disc(r + 0.2, 0.35, "concrete", up=True, r_in=r)
    s.footing = footing
    roof = lathe_band((r - 0.01, 2.85), (r - 0.01, 3.0), (1, 0), (1, 0), "wall",
                      v=(1.0, 0.95), outward=False)
    roof += disc(r, 3.0, "wall", up=True, r_in=r - 0.01)
    rn = profile_normal((r - 0.01, 2.85), (0.6, 3.05))
    roof += lathe_band((r - 0.01, 2.85), (0.6, 3.05), rn, rn, "roof")
    roof += prism(0, 0, 0.6, 3.0, 3.1, "roof", sides=16, top=True)
    roof += prism(0, 0, 0.3, 3.1, 3.45, "roof", sides=8, top=False)
    roof += prism(0, 0, 0.45, 3.45, 3.57, "dark", sides=8, top=True, bottom=True)
    rr = r - 0.35
    roof += ring_wall(rr, 2.87, 3.87, "railing")
    s.roof = roof
    s.polys = s.wall + footing + roof
    s.outline = [(r, 0), (r, 3)]
    s.classify()
    return s


TANKS = [ground_tank("tank_small", "ground_tank_small", 3, 4.3),
         ground_tank("tank_large", "ground_tank_large", 5, 7.3)]


# ------------------------------------------------------------------------------------------
# Writing the tiles
# ------------------------------------------------------------------------------------------
EMPTY = "tank_empty"
C_EMPTY = {"parent": "block/block", "textures": {"particle": T("paint")}}


def tile_obj(shape, polys, name, gi, gj, gk, y_lo=None, y_hi=None):
    """The polygons inside a tile, in its root's block space, as an OBJ; None if nothing is."""
    lo, hi = shape.tile_box(gi, gj, gk, y_lo, y_hi)
    cut = clip_box(polys, lo, hi)
    if not cut:
        return None
    return write_obj(name, shape.mtl, shape.local(cut, gi, gj, gk))


def bowl_models(s):
    """Every canonical tile's OBJ, the band tiles' shells and decals, and the multipart rules:
    (tile rules, band rules)."""
    n, rules, band_rules = s.n, [], []
    made = {}
    for gk in range(s.layers):
        for gj in range(n):
            for gi in range(n):
                (ci, cj), turns = canonical(gi, gj, n)
                key = (ci, cj, gk)
                if key not in made:
                    made[key] = tile_obj(s, s.polys, "%s_%d_%d_%d" % (s.key, gk, ci, cj),
                                         ci, cj, gk)
                idx = (gk * n + gj) * n + gi
                loc = made[key] or M(EMPTY)
                r = {"when": {"tile": str(idx)}, "apply": {"model": loc}}
                if turns:
                    r["apply"]["y"] = 90 * turns
                rules.append(r)
    # the band tiles: a shell without the north band zone, and the decal in every name
    layer, slots = s.band[0], s.band[1]
    plain = [p for p in s.polys if p.tag != ("band", 0)]
    shells, decals = {}, {}
    for si, gi in enumerate(slots):
        shells[si] = tile_obj(s, plain, "%s_band_shell_%d" % (s.key, si), gi, 0, layer)
        for row in range(BAND_ROWS):
            decals[(si, row)] = tile_obj(s, name_row(s.band_polys, row),
                                         "%s_band_%d_%d" % (s.key, si, row), gi, 0, layer)
    for side in range(4):
        for si in range(len(slots)):
            slot = side * len(slots) + si
            r = {"when": {"slot": str(slot)}, "apply": {"model": shells[si]}}
            if side:
                r["apply"]["y"] = 90 * side
            band_rules.append(r)
            for row in range(BAND_ROWS):
                r = {"when": {"slot": str(slot), "name": str(row)},
                     "apply": {"model": decals[(si, row)]}}
                if side:
                    r["apply"]["y"] = 90 * side
                band_rules.append(r)
    return rules, band_rules


def tank_models(s):
    n, rules = s.n, []
    made = {}
    for gj in range(n):
        for gi in range(n):
            (ci, cj), turns = canonical(gi, gj, n)
            if (ci, cj) not in made:
                made[(ci, cj)] = (
                    tile_obj(s, s.wall, "%s_wall_%d_%d" % (s.key, ci, cj), ci, cj, 0),
                    tile_obj(s, s.footing, "%s_footing_%d_%d" % (s.key, ci, cj), ci, cj, 0),
                    tile_obj(s, s.roof, "%s_roof_%d_%d" % (s.key, ci, cj), ci, cj, 0,
                             y_hi=4.5))
            idx = gj * n + gi
            for loc, cond in zip(made[(ci, cj)], ({}, {"below": "false"}, {"above": "false"})):
                if loc is None:
                    continue
                when = {"tile": str(idx)}
                when.update(cond)
                r = {"when": when, "apply": {"model": loc}}
                if turns:
                    r["apply"]["y"] = 90 * turns
                rules.append(r)
    return rules


# ------------------------------------------------------------------------------------------
# Item icons: small JSON models, since an OBJ has no display transforms in a multipart file
# ------------------------------------------------------------------------------------------
def oct_y(cx, cz, r, y0, y1, tex, caps=True):
    return um.octagon("y", cx, cz, r, y0, y1, tex, caps, caps)


def icon_multileg(small):
    els = []
    s = 0.8 if small else 1.0
    r = 5.0 * s
    els += oct_y(8, 8, r, 9, 12.5, "paint")
    els += oct_y(8, 8, r * 0.72, 8, 9, "paint")
    els += oct_y(8, 8, r * 0.4, 7.3, 8, "paint")
    els += oct_y(8, 8, r * 0.62, 12.5, 13.3, "paint")
    els += oct_y(8, 8, r * 0.25, 13.3, 14.3, "paint")
    els += oct_y(8, 8, r + 1.0, 8.8, 9.1, "grating")
    els.append(box([8 - r * 0.62, 10.2, 8 - r - 0.02], [8 + r * 0.62, 11.4, 8 - r], "band",
                   faces=("north",), uv={"north": [0, 14.2, 16, 15.8]}))
    lp = 3.9 * s
    for dx in (-lp, lp):
        for dz in (-lp, lp):
            els += oct_y(8 + dx, 8 + dz, 0.55, 0, 8.8, "paint", caps=False)
    els += oct_y(8, 8, 0.8, 0, 7.4, "paint", caps=False)
    return model({"paint": T("paint"), "grating": T("grating"), "band": T("band"),
                  "particle": T("paint")}, els, ICON_DISPLAY)


def icon_spheroid(small):
    els = []
    s = 0.85 if small else 1.0
    r = 5.2 * s
    els += oct_y(8, 8, 1.1, 0, 7, "paint", caps=False)
    els += oct_y(8, 8, r * 0.45, 7, 8.2, "paint")
    els += oct_y(8, 8, r * 0.78, 8.2, 9.4, "paint")
    els += oct_y(8, 8, r, 9.4, 11.6, "paint")
    els += oct_y(8, 8, r * 0.8, 11.6, 12.6, "paint")
    els += oct_y(8, 8, r * 0.45, 12.6, 13.4, "paint")
    els.append(box([8 - r * 0.6, 10.0, 8 - r - 0.02], [8 + r * 0.6, 11.2, 8 - r], "band",
                   faces=("north",), uv={"north": [0, 14.2, 16, 15.8]}))
    return model({"paint": T("paint"), "band": T("band"), "particle": T("paint")}, els,
                 ICON_DISPLAY)


def icon_tank(small):
    r = 5.5 if small else 7.2
    els = oct_y(8, 8, r, 1, 10 if small else 9, "wall")
    els += oct_y(8, 8, r + 0.6, 0, 1, "concrete")
    els += oct_y(8, 8, 1.0, 10 if small else 9, 11 if small else 10, "roof")
    return model({"wall": T("tank_wall"), "roof": T("tank_roof"), "concrete": T("concrete"),
                  "particle": T("tank_wall")}, els, ICON_DISPLAY)


# ------------------------------------------------------------------------------------------
# The generated Java: every shape's grid and cell map
# ------------------------------------------------------------------------------------------
JAVA_SHAPES = []


def java_shapes():
    lines = ["// Generated by dev-env-utils/scripts/gen_utilities_water.py -- do not edit.",
             "package com.micatechnologies.minecraft.csm.powergrid.water;",
             "",
             "/**",
             " * The tanks' grids and cell maps, written from the same profiles the generator lathes",
             " * into their models. A cell map holds one string per row of cells, bottom layer first,",
             " * north row first, west cell first: {@code '.'} for no part, {@code '#'} for a solid",
             " * part, and {@code 'a' + mask} for a balcony walkway part whose railing runs along the",
             " * sides in {@code mask} (north 1, east 2, south 4, west 8).",
             " *",
             " * @since 2026.9",
             " */",
             "public final class TankShapes {",
             "",
             "  /** The town names the band cycles through; the last, empty, is the blank band. */",
             "  public static final String[] BAND_NAMES = {%s};" % ", ".join(
                 '"%s"' % b for b in BAND_NAMES),
             ""]
    for s, const in JAVA_SHAPES:
        slots = s.band[1] if s.band else []
        lines.append("  /** %s. */" % s.doc)
        lines.append("  public static final TankShape %s = new TankShape(\"%s\", %d, %d, %s, %d,"
                     % (const, s.reg, s.n, s.layers, "true" if s.stackable else "false",
                        s.band[0] if s.band else -1))
        lines.append("      new int[]{%s}, new String[]{" % ", ".join(str(g) for g in slots))
        rows = s.cell_rows()
        for i, row in enumerate(rows):
            lines.append("          \"%s\"%s" % (row, "," if i < len(rows) - 1 else "});"))
        lines.append("")
    lines += ["  private TankShapes() {", "  }", "}"]
    return "\n".join(lines) + "\n"


# ------------------------------------------------------------------------------------------
# The tower's stacking pieces (JSON)
# ------------------------------------------------------------------------------------------
PAINT_TEX = {"paint": T("paint"), "concrete": T("concrete"), "dark": T("dark"),
             "steel": T("steel"), "particle": T("paint")}


def polygon16(cx, cz, r, y0, y1, tex, top=False, bottom=False):
    """A vertical 16-gon prism of inradius r (pixels) about (cx, cz): eight rectangles, five
    long in x turned 0, +-22.5 and +-45 degrees about y and three long in z turned 0 and
    +-22.5, each drawing only its two far faces, which are the 16 facets. Caps, where asked,
    are every rectangle's end, a hair apart so they do not fight."""
    a = r * TAN16
    els = []
    specs = [("x", 0), ("x", 22.5), ("x", -22.5), ("x", 45), ("x", -45),
             ("z", 0), ("z", 22.5), ("z", -22.5)]
    for k, (long_axis, angle) in enumerate(specs):
        eps = 0.003 * k
        if long_axis == "x":
            frm, to, sides = [cx - r, y0 - eps, cz - a], [cx + r, y1 + eps, cz + a], ["east",
                                                                                      "west"]
        else:
            frm, to, sides = [cx - a, y0 - eps, cz - r], [cx + a, y1 + eps, cz + r], ["north",
                                                                                      "south"]
        faces = list(sides) + (["up"] if top else []) + (["down"] if bottom else [])
        b = box(frm, to, tex, faces=faces)
        if angle:
            b["rotation"] = {"origin": [cx, 8, cz], "axis": "y", "angle": angle}
        els.append(b)
    return els


def column_block(reg, r, foot_r, foot_h, cap_r, names, flange=False):
    """A leg or the riser: an octagonal shaft a block long, its concrete pier and base plate
    where nothing of it is below, its cap where nothing is above (BlockTowerColumn)."""
    shaft = um.octagon("y", 8, 8, r, 0, 16, "paint", False, False)
    footing = um.octagon("y", 8, 8, foot_r, 0, foot_h, "concrete", False, True)
    if flange:
        footing += um.octagon("y", 8, 8, r + 0.9, foot_h, foot_h + 0.8, "paint", True, True)
    else:
        footing.append(box([8 - r - 2.6, foot_h, 8 - r - 2.6], [8 + r + 2.6, foot_h + 0.7,
                                                               8 + r + 2.6], "paint"))
        for dx in (-1, 1):
            for dz in (-1, 1):
                x, z = 8 + dx * (r + 1.5), 8 + dz * (r + 1.5)
                footing.append(box([x - 0.5, foot_h + 0.7, z - 0.5], [x + 0.5, foot_h + 1.5,
                                                                     z + 0.5], "dark",
                                   faces=("north", "south", "east", "west", "up")))
    cap = um.octagon("y", 8, 8, cap_r, 15.2, 16, "paint", True, True)
    base = reg + "_"
    models = {base + "shaft": model(PAINT_TEX, shaft), base + "footing": model(PAINT_TEX,
                                                                                 footing),
              base + "cap": model(PAINT_TEX, cap)}
    rules = [rule(base + "shaft"), rule(base + "footing", {"base": True}),
             rule(base + "cap", {"top": True})]
    C.add(reg, 'new BlockTowerColumn("%s", %s)' % (reg, fmt(r)), names, models,
          multipart(rules), item=model(PAINT_TEX, shaft + footing + cap), tab=TAB)


def rod(cx, cy, length, z, thick, angle, tex="paint"):
    """A tie rod in the x-y plane centred on (cx, cy), length along it, turned about z."""
    h = thick / 2
    b = box([cx - length / 2, cy - h, z - h], [cx + length / 2, cy + h, z + h], tex,
            faces=("north", "south", "up", "down", "east", "west"))
    b["rotation"] = {"origin": [cx, cy, z], "axis": "z", "angle": angle}
    return b


ROD_T = 1.6
FALL_Z = 8.9     # the falling rod passes behind the rising one where they cross


def braces():
    """The cross bracing (BlockTowerBrace): a tie rod corner to corner of the block in the
    plane through the legs' centres, rising or falling as its diagonal neighbours run, and
    reaching on into a leg beside it. Facing north its plane is x-y; y: 90 turns it to z-y."""
    diag = 16 * math.sqrt(2)
    t_ext = 2.7                        # how far past the corner a rod reaches into a leg
    ext = t_ext * math.sqrt(2)
    c = t_ext / 2
    parts = {
        "brace_rise": [rod(8, 8, diag, 8, ROD_T, 45)],
        "brace_fall": [rod(8, 8, diag, FALL_Z, ROD_T, -45)],
        "brace_rise_lo": [rod(-c, -c, ext, 8, ROD_T, 45)],
        "brace_rise_hi": [rod(16 + c, 16 + c, ext, 8, ROD_T, 45)],
        "brace_fall_hi": [rod(-c, 16 + c, ext, FALL_Z, ROD_T, -45)],
        "brace_fall_lo": [rod(16 + c, -c, ext, FALL_Z, ROD_T, -45)],
    }
    models = {k: model(PAINT_TEX, v) for k, v in parts.items()}
    conds = [("brace_rise", {"rise": True}), ("brace_fall", {"fall": True}),
             ("brace_rise_lo", {"rise": True, "legl": True}),
             ("brace_rise_hi", {"rise": True, "legr": True}),
             ("brace_fall_hi", {"fall": True, "legl": True}),
             ("brace_fall_lo", {"fall": True, "legr": True})]
    rules = []
    for name, cond in conds:
        for axis, y in (("x", 0), ("z", 90)):
            w = {"axis": axis}
            w.update(cond)
            rules.append(rule(name, w, y))
    C.add("water_tower_brace", 'new BlockTowerBrace("water_tower_brace")',
          names_of("Water Tower Cross Brace", "Wasserturm-Kreuzverband",
                   "Arriostramiento de Torre de Agua", "Vattentornsstag"),
          models, multipart(rules),
          item=model(PAINT_TEX, parts["brace_rise"] + parts["brace_fall"], ICON_DISPLAY),
          tab=TAB)

    run = um.octagon("x", 8, 8, 2.4, 0, 16, "paint", False, False)
    end_l = um.octagon("x", 8, 8, 2.4, -2.7, 0, "paint", False, False)
    end_l.append(box([-3.1, 4.4, 7.6], [-2.5, 11.6, 8.4], "paint"))
    end_r = um.octagon("x", 8, 8, 2.4, 16, 18.7, "paint", False, False)
    end_r.append(box([18.5, 4.4, 7.6], [19.1, 11.6, 8.4], "paint"))
    models = {"strut_run": model(PAINT_TEX, run), "strut_end_l": model(PAINT_TEX, end_l),
              "strut_end_r": model(PAINT_TEX, end_r)}
    rules = []
    for name, cond in (("strut_run", {}), ("strut_end_l", {"legl": True}),
                       ("strut_end_r", {"legr": True})):
        for axis, y in (("x", 0), ("z", 90)):
            w = {"axis": axis}
            w.update(cond)
            rules.append(rule(name, w, y))
    C.add("water_tower_strut", 'new BlockTowerStrut("water_tower_strut")',
          names_of("Water Tower Strut", "Wasserturm-Querstrebe", "Puntal de Torre de Agua",
                   "Vattentornsstrav"),
          models, multipart(rules), item=model(PAINT_TEX, run, ICON_DISPLAY), tab=TAB)


def ladder():
    """The caged ladder (BlockCagedLadder): rails and rungs on stand-offs from what it is hung
    on, the cage from the third block up, feet on the lowest block, and the rails carried up as
    grab rails past the top."""
    tex = {"steel": T("steel"), "dark": T("dark"), "particle": T("steel")}
    rails = [box([3.5, 0, 13.4], [4.5, 16, 14.4], "steel",
                 faces=("north", "south", "east", "west")),
             box([11.5, 0, 13.4], [12.5, 16, 14.4], "steel",
                 faces=("north", "south", "east", "west"))]
    for y in (2, 6, 10, 14):
        rails.append(box([4.5, y, 13.6], [11.5, y + 0.8, 14.2], "steel",
                         faces=("north", "south", "up", "down")))
    for x in (3.6, 11.6):
        rails.append(box([x, 7.6, 14.4], [x + 0.8, 8.4, 16], "dark",
                         faces=("north", "east", "west", "up", "down")))
    cage = [box([2.0, 7.4, 1.6], [14.0, 8.4, 2.4], "steel"),
            box([1.2, 7.4, 1.6], [2.0, 8.4, 13.4], "steel"),
            box([14.0, 7.4, 1.6], [14.8, 8.4, 13.4], "steel")]
    for x in (4.6, 7.6, 10.6):
        cage.append(box([x, 0, 1.7], [x + 0.8, 16, 2.3], "steel",
                        faces=("north", "south", "east", "west")))
    for x in (1.3, 14.1):
        for z in (6.6, 10.6):
            cage.append(box([x, 0, z], [x + 0.6, 16, z + 0.8], "steel",
                            faces=("north", "south", "east", "west")))
    feet = [box([3.0, 0, 13.0], [5.0, 0.4, 14.8], "dark"),
            box([11.0, 0, 13.0], [13.0, 0.4, 14.8], "dark")]
    grab = [box([3.5, 16, 13.4], [4.5, 27, 14.4], "steel", shift=(0, 16, 0),
                faces=("north", "south", "east", "west", "up")),
            box([11.5, 16, 13.4], [12.5, 27, 14.4], "steel", shift=(0, 16, 0),
                faces=("north", "south", "east", "west", "up"))]
    models = {"ladder_rails": model(tex, rails), "ladder_cage": model(tex, cage),
              "ladder_feet": model(tex, feet), "ladder_grab": model(tex, grab)}
    rules = facing_rules([("ladder_rails", {}), ("ladder_cage", {"cage": "true"}),
                          ("ladder_feet", {"bottom": "true"}),
                          ("ladder_grab", {"top": "true"})])
    C.add("caged_ladder", 'new BlockCagedLadder("caged_ladder", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(rails + cage)),
          names_of("Caged Ladder", "Steigleiter mit Rückenschutz", "Escalera con Jaula",
                   "Stege med Ryggskydd"),
          models, multipart(rules), item=model(tex, rails + cage), tab=TAB)


DOOR_UV = [0, 0, 8, 16]


@C.texture("door")
def _door():
    """The pedestal's access door, drawn on the left half (8 x 16 of a 16 texture, stretched
    over a door twice as tall as it is wide): a louvre, the handle and the kick plate."""
    img = lc.fill(lc.shade(PAINT, 0.9), 16, 2, 201)
    lc.frame(img, 0, 0, 8, 16, lc.shade(PAINT, 0.62))
    for y in range(3, 7):
        lc.rect(img, 2, y, 6, y + 1, lc.shade(PAINT, 0.55) if y % 2 else lc.shade(PAINT, 0.8))
    lc.rect(img, 6, 8, 7, 10, (60, 60, 64))
    lc.rect(img, 1, 14, 7, 15, lc.shade(PAINT, 0.7))
    return img


def pedestal():
    """The pedestal column (BlockPedestalSection): a 16-gon three blocks across, a block of it
    at a time, its root in the middle drawing it and eight invisible parts round it. The lowest
    section stands on its concrete footing with the access door; the highest takes a cap unless
    a tank sits on it."""
    r = COLUMN_R * 16
    shaft = polygon16(8, 8, r, 0, 16, "paint")
    footing = polygon16(8, 8, 24, 0, 4, "concrete", top=True)
    z_face = 8 - r
    door = [box([2.5, 4, z_face - 0.5], [13.5, 28.5, z_face + 0.5], "paint",
                faces=("north", "east", "west", "up"), shift=(0, 0, 0),
                uv={"north": [0, 0, 11, 15.5], "east": [0, 0, 1, 15.5], "west": [0, 0, 1, 15.5],
                    "up": [0, 0, 11, 1]}),
            box([3.3, 4.2, z_face - 0.6], [12.7, 27.7, z_face - 0.5], "door", faces=("north",),
                uv={"north": DOOR_UV})]
    cap = polygon16(8, 8, r + 0.4, 15, 16, "paint", top=True, bottom=True)
    tex = dict(PAINT_TEX, door=T("door"))
    models = {"pedestal_shaft": model(tex, shaft), "pedestal_footing": model(tex, footing),
              "pedestal_door": model(tex, door), "pedestal_cap": model(tex, cap)}
    rules = [rule("pedestal_shaft"), rule("pedestal_footing", {"base": True}),
             rule("pedestal_cap", {"top": True})]
    rules += facing_rules([("pedestal_door", {"base": "true"})])
    C.add("water_tower_pedestal", 'new BlockPedestalSection("water_tower_pedestal")',
          names_of("Water Tower Pedestal Section", "Wasserturm-Schaftsegment",
                   "Sección de Fuste de Torre de Agua", "Vattentornsskaftsektion"),
          models, multipart(rules, inventory="pedestal_icon"), tab=TAB)
    C.blocks[-1]["models"]["pedestal_icon"] = model(
        tex, um.octagon("y", 8, 8, 6.5, 0, 16, "paint", True, True)
        + um.octagon("y", 8, 8, 7.4, 0, 2, "concrete", True, True), ICON_DISPLAY)


# ------------------------------------------------------------------------------------------
# The tanks' blocks
# ------------------------------------------------------------------------------------------
BOWL_NAMES = {
    "bowl_small": names_of("Water Tower Tank (Small)", "Wasserturmbehälter (Klein)",
                           "Tanque de Torre de Agua (Pequeño)", "Vattentornstank (Liten)"),
    "bowl_medium": names_of("Water Tower Tank (Medium)", "Wasserturmbehälter (Mittel)",
                            "Tanque de Torre de Agua (Mediano)", "Vattentornstank (Mellan)"),
    "spheroid_small": names_of("Pedestal Water Tank (Small)", "Kugelwasserturmbehälter (Klein)",
                               "Tanque Esferoidal (Pequeño)", "Sfäroidtank (Liten)"),
    "spheroid_medium": names_of("Pedestal Water Tank (Medium)",
                                "Kugelwasserturmbehälter (Mittel)",
                                "Tanque Esferoidal (Mediano)", "Sfäroidtank (Mellan)"),
    "tank_small": names_of("Ground Storage Tank (Small)", "Bodenspeicherbehälter (Klein)",
                           "Tanque de Almacenamiento (Pequeño)", "Marktank (Liten)"),
    "tank_large": names_of("Ground Storage Tank (Large)", "Bodenspeicherbehälter (Groß)",
                           "Tanque de Almacenamiento (Grande)", "Marktank (Stor)"),
}
SHAPE_CONST = {"bowl_small": "BOWL_SMALL", "bowl_medium": "BOWL_MEDIUM",
               "spheroid_small": "SPHEROID_SMALL", "spheroid_medium": "SPHEROID_MEDIUM",
               "tank_small": "TANK_SMALL", "tank_large": "TANK_LARGE"}
SHAPE_DOC = {"bowl_small": "The small multi-leg tank: nine blocks across with its balcony",
             "bowl_medium": "The medium multi-leg tank: fifteen blocks across",
             "spheroid_small": "The small pedestal tank, on a three-block column",
             "spheroid_medium": "The medium pedestal tank",
             "tank_small": "The small ground storage tank, one layer a unit",
             "tank_large": "The large ground storage tank, one layer a unit"}


def tanks():
    for s in BOWLS:
        s.doc = SHAPE_DOC[s.key]
        JAVA_SHAPES.append((s, SHAPE_CONST[s.key]))
        rules, band_rules = bowl_models(s)
        icon = s.key + "_icon"
        icon_model = (icon_multileg(s.n == 3) if s.key.startswith("bowl")
                      else icon_spheroid(s.n == 3))
        const = "TankShapes." + SHAPE_CONST[s.key]
        C.add(s.reg, 'new BlockTankTile("%s", %s)' % (s.reg, const), BOWL_NAMES[s.key],
              {icon: icon_model}, multipart(rules, inventory=icon), tab=TAB)
        C.add(s.reg + "_band", 'new BlockTankBand("%s_band", %s)' % (s.reg, const),
              BOWL_NAMES[s.key], {}, multipart(band_rules, inventory=icon), tab=HIDDEN)
    for s in TANKS:
        s.doc = SHAPE_DOC[s.key]
        JAVA_SHAPES.append((s, SHAPE_CONST[s.key]))
        rules = tank_models(s)
        icon = s.key + "_icon"
        C.add(s.reg, 'new BlockGroundTank("%s", TankShapes.%s)' % (s.reg, SHAPE_CONST[s.key]),
              BOWL_NAMES[s.key], {icon: icon_tank(s.n == 3)}, multipart(rules, inventory=icon),
              tab=TAB)
    C.add("water_tank_part", "new BlockTankPart()",
          names_of("Water Tank", "Wasserbehälter", "Tanque de Agua", "Vattentank"),
          {EMPTY: C_EMPTY},
          {"forge_marker": 1, "defaults": {"model": M(EMPTY)},
           "variants": {"kind": {str(k): {} for k in range(16)}, "inventory": [{}]}},
          tab=HIDDEN)


# ------------------------------------------------------------------------------------------
# Everything, and the command line
# ------------------------------------------------------------------------------------------
column_block("water_tower_leg", 5.5, 7.2, 5, 6.3,
             names_of("Water Tower Leg", "Wasserturmstütze", "Pata de Torre de Agua",
                      "Vattentornsben"))
column_block("water_tower_riser", 6.8, 7.9, 3, 7.6,
             names_of("Water Tower Riser", "Wasserturm-Steigrohr", "Tubo Ascendente de Torre",
                      "Vattentornsstigrör"), flange=True)
braces()
ladder()
pedestal()
tanks()
C.add_lang("csm.utilities.water_tower.band", ("Name band: %s", "Namensband: %s",
                                              "Banda del nombre: %s", "Namnband: %s"))
C.add_lang("csm.utilities.water_tower.band_blank", ("Name band: blank", "Namensband: leer",
                                                    "Banda del nombre: en blanco",
                                                    "Namnband: tomt"))
C.add_lang("csm.utilities.tank.blocked", (
    "Not enough room for this tank -- blocked at %s, %s, %s",
    "Nicht genug Platz für diesen Behälter -- blockiert bei %s, %s, %s",
    "No hay espacio para este tanque -- bloqueado en %s, %s, %s",
    "Inte tillräckligt med plats för tanken -- blockerad vid %s, %s, %s"))

JAVA_REL = "TankShapes.java"


def write_java(java_dir):
    os.makedirs(java_dir, exist_ok=True)
    tmp = tempfile.mkdtemp(prefix="utilities_java_")
    try:
        src = os.path.join(tmp, JAVA_REL)
        with open(src, "w", newline="\n", encoding="utf-8") as fh:
            fh.write(java_shapes())
        um.sync(src, os.path.join(java_dir, JAVA_REL))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def java_up_to_date():
    path = os.path.join(JAVA_DIR, JAVA_REL)
    if not os.path.exists(path):
        return False
    with open(path, "rb") as fh:
        return fh.read().decode("utf-8").replace("\r\n", "\n") == java_shapes()


def report():
    """What the tanks cost: OBJ quads per shape, and the heaviest tile."""
    for s in BOWLS + TANKS:
        names = [k for k in OBJ_QUADS if k.startswith(s.key + "_")]
        print("%-16s %3d OBJ files, %5d quads, heaviest %s (%d)"
              % (s.key, len(names), sum(OBJ_QUADS[k] for k in names),
                 max(names, key=lambda k: OBJ_QUADS[k]),
                 max(OBJ_QUADS[k] for k in names)))


if __name__ == "__main__":
    if "--report" in sys.argv:
        report()
        sys.exit(0)
    if "--check" in sys.argv and not java_up_to_date():
        print("out of date (re-run without --check):\n  " + os.path.join(JAVA_DIR, JAVA_REL))
        rc = C.main()
        sys.exit(1)
    rc = C.main()
    if rc == 0 and "--check" not in sys.argv and "--fragments" not in sys.argv:
        write_java(JAVA_DIR)
    sys.exit(rc)
