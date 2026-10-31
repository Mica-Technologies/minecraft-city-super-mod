"""
gen_venues_common.py -- what the Sports & Venues generators share.

Not run on its own. gen_venues_seating.py (and the later event floor, scoreboard and concourse
generators) import it for:

  * a Catalogue of blocks writing into the Sports & Venues module's tree: the Life Safety
    catalogue (registry, the Java that constructs it, four-language names, models, blockstate,
    optional item model, extra files) pointed at modules/venues, with its writing, --check and
    --fragments, and model_depth's separating and pruning run over every JSON model it writes;
  * the arena palette as small swatch textures every model shares;
  * a minimal OBJ writer for the parts no JSON element can draw (a rail or a guard on a 1 in 2
    slope): convex polygons in pixel space, written in block space with their normals, v down
    the texture (no flip-v, since a multipart part cannot pass Forge's custom data);
  * Forge blockstates for a horizontal facing and a keyed multipart helper.

Every model faces north (towards the event floor) with what it backs onto at +Z, as every
rotatable block in the mod is drawn.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lsc  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "venues", "src", "main", "resources", "assets", "csm")
LOCALES = lsc.LOCALES

# The Life Safety helpers, re-exported so a venues generator imports one module.
box = lsc.box
face = lsc.face
model = lsc.model
fill = lsc.fill
rect = lsc.rect
frame = lsc.frame
bevel = lsc.bevel
disc = lsc.disc
shade = lsc.shade
clamp = lsc.clamp
draw_text = lsc.draw_text
draw_text_centred = lsc.draw_text_centred
text_width = lsc.text_width
facing_state = lsc.facing_state
dump = lsc.dump

SEATING = "Sports & Venues - Seating & Venue"
HIDDEN = "hidden"

FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
SHELL_GREY = (232, 232, 230)    # a seat shell's plastic before its colour's tint
CAST = (70, 72, 76)             # powder-coated cast aluminium standards
ARMREST = (34, 34, 36)          # black moulded armrests and cupholders
ALUMINIUM = (192, 196, 200)
STAINLESS = (198, 202, 206)
STEEL_DARK = (88, 92, 98)
ACCESS_BLUE = (0, 84, 160)
LED_WARM = (255, 236, 196)


def catalogue(name, sub):
    """A catalogue writing textures under textures/blocks/venues/<sub> and models under
    models/block/venues/<sub>, into the Sports & Venues module's tree."""
    return lsc.Catalogue(name, "venues/" + sub, "venues/" + sub, assets=ASSETS)


def swatch(colour, seed, grain=3, size=16):
    """A flat painted or plastic surface: the swatch every model of that colour shares."""
    return fill(colour, size=size, grain=grain, seed=seed)


def brushed(colour, seed, size=16):
    """Brushed metal: fine horizontal streaks."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    rows = [rng.uniform(-0.06, 0.06) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(colour, 1.0 + rows[y] + rng.uniform(-0.015, 0.015)) + (255,)
    return img


def tinted(el, faces=None):
    """Marks an element's faces (all, or ``faces``) with tint index 0: the seat's colour."""
    for name, f in el["faces"].items():
        if faces is None or name in faces:
            f["tintindex"] = 0
    return el


def mirror_x(el):
    """The element mirrored across x = 8 (a left part made right): from/to swapped, east and
    west faces swapped with their uv windows flipped so a picture still reads."""
    out = json.loads(json.dumps(el))
    x0, x1 = el["from"][0], el["to"][0]
    out["from"][0] = round(16 - x1, 3)
    out["to"][0] = round(16 - x0, 3)
    faces = out["faces"]
    east, west = faces.pop("east", None), faces.pop("west", None)
    if east is not None:
        faces["west"] = east
    if west is not None:
        faces["east"] = west
    for name in ("north", "south", "up", "down"):
        if name in faces and "uv" in faces[name]:
            u = faces[name]["uv"]
            if name in ("north", "south", "up", "down"):
                faces[name]["uv"] = [round(16 - u[2], 3), u[1], round(16 - u[0], 3), u[3]]
    if "rotation" in out:
        r = out["rotation"]
        r["origin"][0] = round(16 - r["origin"][0], 3)
        if r["axis"] in ("y", "z"):
            r["angle"] = -r["angle"]
    return out


# ------------------------------------------------------------------------------------------
# OBJ
# ------------------------------------------------------------------------------------------
def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _norm(v):
    m = math.sqrt(_dot(v, v)) or 1.0
    return (v[0] / m, v[1] / m, v[2] / m)


class Poly(object):
    """A convex polygon in pixel space: its corners, their uvs (0..1, v down the texture), its
    normal and its material. Wound to face ``outward``."""

    def __init__(self, pts, mat, outward, uvs=None):
        self.pts = [tuple(float(c) for c in p) for p in pts]
        self.mat = mat
        if uvs is None:
            uvs = ([(0, 1), (1, 1), (1, 0), (0, 0)] if len(pts) == 4
                   else [(0.5, 0.5)] * len(pts))
        self.uvs = list(uvs)
        n = (0.0, 0.0, 0.0)
        p0 = self.pts[0]
        for i in range(1, len(self.pts) - 1):
            c = _cross(_sub(self.pts[i], p0), _sub(self.pts[i + 1], p0))
            n = (n[0] + c[0], n[1] + c[1], n[2] + c[2])
        if _dot(n, outward) < 0:
            self.pts.reverse()
            self.uvs.reverse()
        self.normal = _norm(outward)


class Obj(object):
    """An OBJ in 0..1 block space, its materials in an MTL beside it."""

    def __init__(self, gen, mtl, name):
        self.gen, self.mtl, self.name = gen, mtl, name
        self.v, self.t, self.n, self.faces = [], [], [], []

    def add(self, poly):
        refs = []
        for p, uv in zip(poly.pts, poly.uvs):
            self.v.append("v %.5f %.5f %.5f" % (p[0] / 16.0 + 0.0, p[1] / 16.0 + 0.0,
                                                p[2] / 16.0 + 0.0))
            self.t.append("vt %.5f %.5f" % (min(1, max(0, uv[0])), min(1, max(0, uv[1]))))
            self.n.append("vn %.5f %.5f %.5f" % poly.normal)
            refs.append(len(self.v))
        self.faces.append((poly.mat, refs))

    def text(self):
        lines = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % self.gen,
                 "mtllib %s.mtl" % self.mtl, "o %s" % self.name]
        lines += self.v + self.t + self.n
        current = None
        for mat, refs in sorted(self.faces, key=lambda f: f[0]):
            if mat != current:
                lines.append("usemtl " + mat)
                current = mat
            lines.append("f " + " ".join("%d/%d/%d" % (r, r, r) for r in refs))
        return "\n".join(lines) + "\n"


def mtl_text(gen, materials):
    text = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % gen]
    for mat, tex in sorted(materials.items()):
        text += ["newmtl " + mat, "map_Kd " + tex]
    return "\n".join(text) + "\n"


def obox(x0, x1, y0, y1, z0, z1, mat, faces="nsewud"):
    """An upright box, in pixels, as polygons."""
    out = []
    if "n" in faces:
        out.append(Poly([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)], mat, (0, 0, -1)))
    if "s" in faces:
        out.append(Poly([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], mat, (0, 0, 1)))
    if "w" in faces:
        out.append(Poly([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], mat, (-1, 0, 0)))
    if "e" in faces:
        out.append(Poly([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)], mat, (1, 0, 0)))
    if "u" in faces:
        out.append(Poly([(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)], mat, (0, 1, 0)))
    if "d" in faces:
        out.append(Poly([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], mat, (0, -1, 0)))
    return out


def opost(xc, zc, r, y0, y1, mat, cap=True):
    """An upright octagon (a round post), sides and top."""
    ring = [(xc + r * math.cos(math.radians(22.5 + 45 * i)),
             zc + r * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
    out = []
    for i in range(8):
        (xa, za), (xb, zb) = ring[i], ring[(i + 1) % 8]
        n = ((xa + xb) / 2 - xc, 0, (za + zb) / 2 - zc)
        out.append(Poly([(xa, y0, za), (xb, y0, zb), (xb, y1, zb), (xa, y1, za)], mat, n))
    if cap:
        out.append(Poly([(x, y1, z) for x, z in ring], mat, (0, 1, 0)))
    return out


def orail_z(xc, line, r, z0, z1, mat, caps=True):
    """A round rail running along z from z0 to z1, its centre at (xc, line(z)): an octagon
    sheared up the slope, so its ends are vertical (square to the run) and meet the next cell's
    rail exactly; its vertical radius is scaled by the slope's secant so it looks as thick as a
    level rail."""
    slope = (line(z1) - line(z0)) / float(z1 - z0)
    k = math.sqrt(1 + slope * slope)
    ring = [(r * math.cos(math.radians(22.5 + 45 * i)), r * k * math.sin(math.radians(22.5 + 45 * i)))
            for i in range(8)]
    out = []
    ya, yb = line(z0), line(z1)
    for i in range(8):
        (xa, da), (xb, db) = ring[i], ring[(i + 1) % 8]
        n = ((xa + xb) / 2, (da + db) / 2, -slope * (da + db) / 2)
        out.append(Poly([(xc + xa, ya + da, z0), (xc + xb, ya + db, z0), (xc + xb, yb + db, z1),
                         (xc + xa, yb + da, z1)], mat, n))
    if caps:
        out.append(Poly([(xc + x, ya + d, z0) for x, d in ring], mat, (0, 0, -1)))
        out.append(Poly([(xc + x, yb + d, z1) for x, d in ring], mat, (0, 0, 1)))
    return out


def obar_z(x0, x1, lo, hi, z0, z1, mat, ends=True):
    """A rectangular bar along z whose bottom follows lo(z) and top hi(z) (both straight across
    the span): a sheared box with vertical ends."""
    out = []
    l0, l1, h0, h1 = lo(z0), lo(z1), hi(z0), hi(z1)
    out.append(Poly([(x0, l0, z0), (x0, l1, z1), (x0, h1, z1), (x0, h0, z0)], mat, (-1, 0, 0)))
    out.append(Poly([(x1, l1, z1), (x1, l0, z0), (x1, h0, z0), (x1, h1, z1)], mat, (1, 0, 0)))
    s_top = (h1 - h0) / float(z1 - z0)
    s_bot = (l1 - l0) / float(z1 - z0)
    out.append(Poly([(x0, h1, z1), (x1, h1, z1), (x1, h0, z0), (x0, h0, z0)], mat, (0, 1, -s_top)))
    out.append(Poly([(x0, l0, z0), (x1, l0, z0), (x1, l1, z1), (x0, l1, z1)], mat, (0, -1, s_bot)))
    if ends:
        out.append(Poly([(x1, l0, z0), (x0, l0, z0), (x0, h0, z0), (x1, h0, z0)], mat, (0, 0, -1)))
        out.append(Poly([(x0, l1, z1), (x1, l1, z1), (x1, h1, z1), (x0, h1, z1)], mat, (0, 0, 1)))
    return out


def oplane_x(x, lo, hi, z0, z1, mat, both=True):
    """A plane at x from z0 to z1, bottom lo(z) and top hi(z): glass. Both faces unless told,
    v running from its top to its foot."""
    pts = [(x, lo(z0), z0), (x, lo(z1), z1), (x, hi(z1), z1), (x, hi(z0), z0)]
    ymin = min(lo(z0), lo(z1))
    ymax = max(hi(z0), hi(z1))
    span = (ymax - ymin) or 1.0

    def uv(p):
        return (p[2] / 16.0, (ymax - p[1]) / span)
    out = [Poly(pts, mat, (1, 0, 0), [uv(p) for p in pts])]
    if both:
        out.append(Poly(list(reversed(pts)), mat, (-1, 0, 0), [uv(p) for p in reversed(pts)]))
    return out


def check_json(paths):
    """Parses every JSON file the generator wrote, so a malformed one fails the run."""
    for p in paths:
        if p.endswith(".json") or p.endswith(".mcmeta"):
            with open(p, encoding="utf-8") as fh:
                json.load(fh)
