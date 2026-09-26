#!/usr/bin/env python3
"""Every asset the Utilities tab's sewer and stormwater pieces ship: the lift station (the wet
well and valve vault access hatches, the control panel on its rack, the standby generator), the
curb inlet, the stormwater outfall (headwall, pipe mouth, flap gate, wingwalls, riprap), the
detention pond outlet riser and emergency spillway, and the precast manhole riser sections and
cone, whole and cut away.

    python dev-env-utils/scripts/gen_utilities_sewer.py
    python dev-env-utils/scripts/gen_utilities_sewer.py --check
    python dev-env-utils/scripts/gen_utilities_sewer.py --fragments   # tab lines to paste

What is here, and the class that places each (package powergrid.sewer unless named):

- **Access hatches** (BlockAccessHatch): an aluminium double-leaf hatch set flush in the ground,
  one leaf a block, placed as a pair. A click opens both leaves; the wet well's safety grate
  stays shut under them, the valve vault's opening is open.
- **The control panel and the standby generator** (BlockSwitchedUnit, Roads' utility box
  multi-block): the panel's alarm beacon flashes and its flood light lights while it is powered
  by redstone; the generator runs, or stops, at a click.
- **Precast pieces**: the curb inlet and the emergency spillway (BlockPrecastRun) join side by
  side; the outfall headwall (BlockHeadwall) joins side by side and stacks; the wingwall
  (BlockWingwall) runs two blocks downstream from the headwall; the outlet riser and the manhole
  riser sections (BlockStackedSection) stack and pick their ends from their neighbours like the
  poles; the manhole cone (BlockManholeCone) tops a manhole. Riprap (BlockRiprap) is a layer of
  stones, a drawing chosen by the block's position.

Round things (the manhole rings and cone, the outfall pipe and its flap) are OBJ lathes, as the
water tanks are (gen_utilities_water's Poly and lathe); everything else is JSON. What exists in
Roads' Streetscape already -- manhole covers, catch basin and gutter grates, the storm drain
marker, valve boxes and cleanouts, bollards -- is not repeated here: the curb inlet carries
Streetscape's storm manhole lid on its top, and the rest is placed beside these pieces.

Every text write keeps the line endings the file already has on disk, and a file whose content
has not changed is not touched (gen_utilities_meters.Catalogue).
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

REPO = um.REPO
ASSETS = um.ASSETS
TAB = "CsmTabUtilities"
FACINGS = um.FACINGS
GEN = "gen_utilities_sewer.py"

C = um.Catalogue(GEN, "utilities/sewer", "utilities/sewer", assets=ASSETS)
box, fmt, extent = um.box, um.fmt, um.extent
Poly = gw.Poly


def names_of(*n):
    assert len(n) == 4
    return n


def M(name):
    return C.M(name)


def T(name):
    return C.T(name)


def W(name):
    """One of the water system's textures, shared rather than drawn again."""
    return "csm:blocks/utilities/water/" + name


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
    r = {"apply": {"model": M(name) if ":" not in name else name}}
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


def extent_unit(els):
    return gw.extent_unit(els)


def box_java(els, top=16):
    return "new double[]{%s}" % ", ".join(fmt(v) for v in extent(els, top))


ICON_DISPLAY = gw.ICON_DISPLAY
big_display = gw.big_display
clamp_uv = gw.clamp_uv

# ------------------------------------------------------------------------------------------
# Colours and textures
# ------------------------------------------------------------------------------------------
PRECAST = (184, 182, 174)
PRECAST_DARK = (92, 92, 88)
IRON = (70, 69, 68)
ORANGE = (222, 112, 30)
GALV = (160, 166, 168)
ENCLOSURE = (200, 188, 152)      # the generator's sand enclosure
STAINLESS = (196, 200, 202)

C.textures["precast"] = lambda: lc.fill(PRECAST, 16, 5, 301)
C.textures["precast_dark"] = lambda: lc.fill(PRECAST_DARK, 16, 4, 302)
C.textures["iron"] = lambda: lc.fill(IRON, 16, 3, 303)


@C.texture("riprap")
def _riprap():
    """Broken stone: grey angular stones with dark joints between them, a Voronoi partition on
    a torus so the texture tiles."""
    size = 32
    rng = random.Random(311)
    seeds = [(rng.uniform(0, size), rng.uniform(0, size), rng.uniform(0.78, 1.12))
             for _ in range(14)]
    img = Image.new("RGBA", (size, size))
    px = img.load()
    base = (128, 126, 120)
    for y in range(size):
        for x in range(size):
            best, second, shade = 1e9, 1e9, 1.0
            for sx, sy, k in seeds:
                dx = min(abs(x + 0.5 - sx), size - abs(x + 0.5 - sx))
                dy = min(abs(y + 0.5 - sy), size - abs(y + 0.5 - sy))
                d = math.hypot(dx, dy)
                if d < best:
                    best, second, shade = d, best, k
                elif d < second:
                    second = d
            if second - best < 1.1:
                px[x, y] = lc.shade(base, 0.45) + (255,)
            else:
                n = rng.uniform(-6, 6)
                c = lc.shade(base, shade)
                px[x, y] = lc.clamp((c[0] + n, c[1] + n, c[2] + n)) + (255,)
    return img


@C.texture("safety_grate")
def _safety_grate():
    """A wet well's fall-through prevention grate: orange FRP bars on a two-texel pitch both
    ways, cut out."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0 or x in (0, 15) or y in (0, 15):
                k = 0.86 if (x % 4 == 0 and y % 4 == 0) else 1.0
                img.putpixel((x, y), lc.shade(ORANGE, k) + (255,))
    return img


@C.texture("bar_rack")
def _bar_rack():
    """A trash rack's bars, galvanised, cut out: uprights every four texels, a tie bar at the
    top and the middle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x % 4 == 1 or y in (0, 1, 8):
                img.putpixel((x, y), lc.shade(GALV, 0.9 if y in (1, 8) else 1.0) + (255,))
    return img


@C.texture("enclosure")
def _enclosure():
    """The generator's sound-attenuated enclosure: sand paint with louvre slats."""
    img = lc.fill(ENCLOSURE, 16, 3, 321)
    for y in range(2, 16, 3):
        for x in range(16):
            img.putpixel((x, y), lc.shade(ENCLOSURE, 0.72) + (255,))
            img.putpixel((x, y - 1), lc.shade(ENCLOSURE, 1.05) + (255,))
    return img


C.textures["enclosure_plain"] = lambda: lc.fill(ENCLOSURE, 16, 3, 322)


def _gen_screen(img, lines, colour):
    lc.rect(img, 0, 0, 16, 16, (48, 50, 54))
    lc.rect(img, 1, 1, 15, 15, (22, 30, 26))
    for i, t in enumerate(lines):
        lc.draw_text(img, t, 2, 2 + 6 * i, colour)


@C.texture("gen_display")
def _gen_display():
    """The generator controller's display while it runs: RUN, and the frequency and the load
    changing a little, as an animated strip, so nothing ticks."""
    frames = 4
    strip = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        img = Image.new("RGBA", (16, 16))
        _gen_screen(img, ["RUN", "%02d" % (60 if f % 2 == 0 else 59)], (120, 230, 120))
        lc.rect(img, 12, 9 + f % 3, 14, 14, (90, 200, 90))
        strip.paste(img, (0, 16 * f))
    return strip


C.extra["textures/blocks/utilities/sewer/gen_display.png.mcmeta"] = um.mcmeta(12)


@C.texture("gen_display_off")
def _gen_display_off():
    img = Image.new("RGBA", (16, 16))
    _gen_screen(img, ["AUT", "OFF"], (220, 190, 80))
    return img


@C.texture("ls_panel_front")
def _ls_panel_front():
    """The lift station control panel's door, 20 x 28 texels at the top left of a 32 sheet:
    run lights and hand-off-auto switches for the two pumps, the high level alarm light and its
    silence button, two hour meters and the nameplate."""
    img = lc.fill(STAINLESS, 32, 3, 331)
    lc.frame(img, 0, 0, 20, 28, lc.shade(STAINLESS, 0.62))
    lc.frame(img, 1, 1, 19, 27, lc.shade(STAINLESS, 0.82))
    lc.rect(img, 4, 3, 16, 7, (236, 234, 226))           # nameplate
    lc.frame(img, 4, 3, 16, 7, (110, 112, 116))
    lc.rect(img, 6, 4, 14, 5, (40, 60, 120))
    lc.rect(img, 7, 5, 13, 6, (40, 60, 120))
    for i in range(2):                                     # pump 1 and 2
        x = 5 + 8 * i
        lc.disc(img, x + 1, 10.5, 1.6, (30, 140, 50))      # run light
        lc.disc(img, x + 1, 10.5, 1.0, (70, 210, 90))
        lc.disc(img, x + 1, 15, 1.8, (34, 34, 38))         # H-O-A selector
        lc.rect(img, x + 1, 13, x + 2, 17, (210, 210, 210))
        lc.rect(img, x - 1, 19, x + 4, 22, (30, 30, 34))   # hour meter
        lc.rect(img, x, 20, x + 3, 21, (210, 206, 190))
    lc.disc(img, 10, 24.5, 1.8, (180, 30, 26))             # alarm silence
    lc.rect(img, 17, 12, 18, 17, (80, 82, 86))             # handle
    return img


@C.texture("beacon_off")
def _beacon_off():
    img = lc.fill((104, 26, 22), 16, 3, 341)
    lc.disc(img, 8, 5, 3, (128, 40, 34))
    return img


@C.texture("lamp_on")
def _lamp_on():
    img = lc.fill((252, 246, 214), 16, 2, 351)
    for x in range(1, 16, 3):
        for y in range(1, 16, 3):
            img.putpixel((x, y), (255, 255, 244, 255))
    return img


@C.texture("lamp_on_e")
def _lamp_on_e():
    return lc.fill((255, 250, 226), 16, 2, 352)


@C.texture("lamp_off")
def _lamp_off():
    img = lc.fill((150, 150, 144), 16, 2, 353)
    for x in range(1, 16, 3):
        for y in range(1, 16, 3):
            img.putpixel((x, y), (176, 176, 170, 255))
    return img


# ------------------------------------------------------------------------------------------
# OBJ: the round precast pieces, built as the water tanks are (gw.Poly, gw lathes)
# ------------------------------------------------------------------------------------------
MTL = "sewer"
MATERIALS = {"precast": T("precast"), "inside": T("precast_dark"), "iron": T("iron"),
             "dark": W("dark")}
OBJ_QUADS = {}


class Obj(gw.Obj):
    def text(self):
        return gw.Obj.text(self).replace("gen_utilities_water.py", GEN, 1)


def write_obj(name, polys):
    o = Obj(MTL, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/utilities/sewer/%s.obj" % name] = o.text()
    OBJ_QUADS[name] = o.count
    return "csm:utilities/sewer/%s.obj" % name


def _mtl():
    text = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % GEN]
    for mat, tex in MATERIALS.items():
        text += ["newmtl " + mat, "map_Kd " + tex]
    C.extra["models/block/utilities/sewer/%s.mtl" % MTL] = "\n".join(text) + "\n"


_mtl()


def xform(polys, f):
    """Every point and normal through the rotation-and-move f(point, is_normal)."""
    return [Poly([f(p, False) for p in q.pts], q.uvs, [f(n, True) for n in q.ns], q.mat, q.tag)
            for q in polys]


def moved(polys, dx, dy, dz):
    return xform(polys, lambda p, n: p if n else (p[0] + dx, p[1] + dy, p[2] + dz))


def flat(points, normal, mat, uv_fn):
    """A flat convex polygon facing ``normal``, its uvs from uv_fn(point)."""
    return Poly(points, [uv_fn(p) for p in points], [normal] * len(points), mat, outward=normal)


def s16(*v):
    return tuple(x / 16.0 for x in v)


# ---- the manhole: rings, the base, the cone -----------------------------------------------
MH_OUT = 8 * math.cos(math.pi / 16)     # the ring's outer inradius: its corners on the cell
MH_IN = 6.0                             # the inner inradius: 0.75 block clear inside
CONE_TOP_OUT = 6.3
CONE_TOP_IN = 5.3                       # the cover's frame (Streetscape's manhole, r 6 px)
FRAME_OUT = 6.5
CONE_SHOULDER = 11.0
FRAME_Y = 13.5


def _lathe(points, mat, outward=True, smooth=False):
    """A profile [(r, y)] in sixteenths swept round the vertical axis at the cell's centre."""
    pts = [(r / 16.0, y / 16.0) for r, y in points]
    out = []
    for a, b in zip(pts[:-1], pts[1:]):
        n = gw.profile_normal(a, b) if b[1] > a[1] else gw.profile_normal(b, a)
        out += gw.lathe_band(a, b, n, n, mat, outward=outward, v=(1.0, 0.0))
    return moved(out, 0.5, 0, 0.5)


def _annulus(r_out, r_in, y, mat, up=True):
    return moved(gw.disc(r_out / 16.0, y / 16.0, mat, up=up, r_in=r_in / 16.0), 0.5, 0, 0.5)


def ring_polys():
    return _lathe([(MH_OUT, 0), (MH_OUT, 16)], "precast") + _lathe(
        [(MH_IN, 0), (MH_IN, 16)], "inside", outward=False)


def ring_top_polys():
    return _annulus(MH_OUT, MH_IN, 16, "precast")


BENCH_Y = 4.0
TROUGH_Y = 2.0
TROUGH_W = 2.0     # the channel's half width


def base_polys():
    """The base section's floor: the bench at BENCH_Y either side of the channel that runs
    through it east to west, the channel's floor at TROUGH_Y, its sides, and the pipe mouths on
    the ring's inner wall at its ends."""
    top = gw.disc(MH_IN / 16.0 + 0.002, BENCH_Y / 16.0, "inside", up=True)
    halves = gw.clip_box(top, (-1, -1, TROUGH_W / 16.0), (1, 1, 1)) + gw.clip_box(
        top, (-1, -1, -1), (1, 1, -TROUGH_W / 16.0))
    floor = gw.clip_box(gw.disc(MH_IN / 16.0 + 0.002, TROUGH_Y / 16.0, "dark", up=True),
                        (-1, -1, -TROUGH_W / 16.0), (1, 1, TROUGH_W / 16.0))
    polys = moved(halves + floor, 0.5, 0, 0.5)
    uv = lambda p: (p[0], 1 - p[1] * 4)
    x = MH_IN * math.cos(math.pi / 16) - 0.05
    for sz in (-1, 1):
        z = 8 + sz * TROUGH_W
        pts = [s16(8 - x, TROUGH_Y, z), s16(8 + x, TROUGH_Y, z), s16(8 + x, BENCH_Y, z),
               s16(8 - x, BENCH_Y, z)]
        polys.append(flat(pts, (0, 0, -sz), "inside", uv))
    for sx in (-1, 1):
        xx = 8 + sx * (MH_IN - 0.05)
        pts = [s16(xx, TROUGH_Y, 8 - TROUGH_W), s16(xx, TROUGH_Y, 8 + TROUGH_W),
               s16(xx, TROUGH_Y + 3.2, 8 + TROUGH_W), s16(xx, TROUGH_Y + 3.2, 8 - TROUGH_W)]
        polys.append(flat(pts, (-sx, 0, 0), "dark", lambda p: (p[2], 1 - p[1] * 2)))
    polys += _annulus(MH_OUT, 0.001, 0, "precast", up=False)
    return polys


def cone_polys():
    outer = _lathe([(MH_OUT, 0), (CONE_TOP_OUT, CONE_SHOULDER)], "precast")
    outer += _lathe([(CONE_TOP_OUT, CONE_SHOULDER), (CONE_TOP_OUT, FRAME_Y)], "precast")
    outer += _lathe([(FRAME_OUT, FRAME_Y), (FRAME_OUT, 16)], "iron")
    outer += _annulus(FRAME_OUT, CONE_TOP_OUT, FRAME_Y, "iron", up=False)
    inner = _lathe([(MH_IN, 0), (CONE_TOP_IN, CONE_SHOULDER)], "inside", outward=False)
    inner += _lathe([(CONE_TOP_IN, CONE_SHOULDER), (CONE_TOP_IN, 16)], "inside",
                    outward=False)
    top = _annulus(FRAME_OUT, CONE_TOP_IN, 16, "iron")
    return outer + inner + top


def back_half(polys):
    """What a cutaway keeps: the half behind the cell's centre (the model's south)."""
    return gw.clip_box(polys, (-1, -1, 0.5), (2, 2, 2))


def cut_faces(bands, mat_of=lambda i: "precast"):
    """The faces a cutaway shows on its cut through the axis: for each band
    [(r_in0, r_out0, y0), (r_in1, r_out1, y1)] the wall's section each side, facing north."""
    out = []
    for i, ((a0, b0, y0), (a1, b1, y1)) in enumerate(bands):
        for sx in (-1, 1):
            pts = [s16(8 + sx * a0, y0, 8), s16(8 + sx * b0, y0, 8), s16(8 + sx * b1, y1, 8),
                   s16(8 + sx * a1, y1, 8)]
            out.append(flat(pts, (0, 0, -1), mat_of(i),
                            lambda p: (1 - p[0], 1 - p[1])))
    return out


def ring_cut():
    return cut_faces([((MH_IN, MH_OUT, 0), (MH_IN, MH_OUT, 16))])


def base_cut():
    x = MH_IN * math.cos(math.pi / 16)
    pts = [s16(8 - x, 0, 8), s16(8 + x, 0, 8), s16(8 + x, TROUGH_Y, 8), s16(8 - x, TROUGH_Y, 8)]
    return [flat(pts, (0, 0, -1), "precast", lambda p: (1 - p[0], 1 - p[1]))]


def cone_cut():
    return cut_faces([((MH_IN, MH_OUT, 0), (CONE_TOP_IN, CONE_TOP_OUT, CONE_SHOULDER)),
                      ((CONE_TOP_IN, CONE_TOP_OUT, CONE_SHOULDER),
                       (CONE_TOP_IN, CONE_TOP_OUT, FRAME_Y)),
                      ((CONE_TOP_IN, FRAME_OUT, FRAME_Y), (CONE_TOP_IN, FRAME_OUT, 16))],
                     mat_of=lambda i: "iron" if i == 2 else "precast")


# ---- the outfall: the headwall's face round the pipe, the pipe, the flap, the wingwall -------
HW_Z = 10.0          # the headwall's front face; it is 6 px thick, to the back of the cell
PIPE_Y = 7.0
PIPE_OUT = 6.5
PIPE_IN = 5.5
PIPE_FRONT = 8.0     # the pipe stands out of the wall to here
PIPE_BACK = 14.0     # where its dark inside ends


def _z_lathe(points, mat, outward=True):
    """A profile [(r, d)] in sixteenths, d measured north (toward the viewer) from z = 16,
    swept round the pipe's axis (x = 8, y = PIPE_Y)."""
    polys = []
    pts = [(r / 16.0, d / 16.0) for r, d in points]
    for a, b in zip(pts[:-1], pts[1:]):
        n = gw.profile_normal(a, b) if b[1] > a[1] else gw.profile_normal(b, a)
        polys += gw.lathe_band(a, b, n, n, mat, outward=outward, v=(1.0, 0.0))
    return _to_pipe(polys)


def _to_pipe(polys):
    # (x, y, z) -> (x, z, -y): the lathe's axis turned to run north, then moved to the pipe
    cx, cy = 0.5, PIPE_Y / 16.0
    return xform(polys, lambda p, n: (p[0], p[2], -p[1]) if n else (
        cx + p[0], cy + p[2], 1.0 - p[1]))


def _z_disc(r_out, r_in, d, mat, north=True):
    return _to_pipe(gw.disc(r_out / 16.0, d / 16.0, mat, up=north, r_in=r_in / 16.0))


def wall_face_with_hole(r, z=HW_Z, x0=0.0, x1=16.0, y0=0.0, y1=16.0, cy=PIPE_Y, mat="precast"):
    """The headwall's front face with a 16-gon hole of inradius r at (8, cy): one convex
    polygon for each edge of the hole, reaching out along the rays through its ends to the
    face's edge (with the corner between, where there is one)."""
    R = r / math.cos(math.pi / 16)
    polys = []
    corners = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]

    def ang(px, py):
        return math.atan2(px - 8, -(py - cy)) % (2 * math.pi)

    def ray_hit(phi):
        dx, dy = math.sin(phi), -math.cos(phi)
        ts = []
        if dx > 1e-9:
            ts.append((x1 - 8) / dx)
        if dx < -1e-9:
            ts.append((x0 - 8) / dx)
        if dy > 1e-9:
            ts.append((y1 - cy) / dy)
        if dy < -1e-9:
            ts.append((y0 - cy) / dy)
        t = min(ts)
        return (8 + dx * t, cy + dy * t)

    for k in range(16):
        pa = k * math.pi / 8 - math.pi / 16
        pb = pa + math.pi / 8
        va = (8 + R * math.sin(pa), cy - R * math.cos(pa))
        vb = (8 + R * math.sin(pb), cy - R * math.cos(pb))
        ba, bb = ray_hit(pa), ray_hit(pb)
        mid = []
        for c in corners:
            a = ang(*c)
            lo, hi = pa % (2 * math.pi), pb % (2 * math.pi)
            inside = (lo < a < hi) if lo < hi else (a > lo or a < hi)
            if inside:
                mid.append(c)
        pts2 = [va, vb, bb] + mid + [ba]
        pts = [s16(px, py, z) for px, py in pts2]
        polys.append(flat(pts, (0, 0, -1), mat, lambda p: (1 - p[0], 1 - p[1])))
    return polys


def pipe_polys():
    """The pipe standing out of the headwall: its outside to the mouth, the mouth's ring, its
    dark inside back into the wall and the dark end where the view gives out."""
    d_wall, d_front, d_back = 16 - HW_Z, 16 - PIPE_FRONT, 16 - PIPE_BACK
    out = _z_lathe([(PIPE_OUT, d_wall), (PIPE_OUT, d_front)], "precast")
    out += _z_disc(PIPE_OUT, PIPE_IN, d_front, "precast", north=True)
    out += _z_lathe([(PIPE_IN, d_back), (PIPE_IN, d_front)], "inside", outward=False)
    out += _z_disc(PIPE_IN + 0.01, 0.001, d_back, "dark", north=True)
    return out


FLAP_R = 7.0


def flap_polys():
    """The flap gate's cast iron disc hanging on the pipe's mouth."""
    z0, z1 = PIPE_FRONT - 0.9, PIPE_FRONT - 0.1
    d0, d1 = 16 - z1, 16 - z0
    out = _z_lathe([(FLAP_R, d0), (FLAP_R, d1)], "iron")
    out += _z_disc(FLAP_R, 0.001, d1, "iron", north=True)
    out += _z_disc(FLAP_R - 1.4, FLAP_R - 2.0, d1 + 0.35, "iron", north=True)
    out += _z_lathe([(FLAP_R - 1.4, d1), (FLAP_R - 1.4, d1 + 0.35)], "iron")
    return out


WING_X = (5.0, 11.0)
WING_RUN = 42.0      # the wingwall's length, from the headwall's face to its toe
WING_TOE = 1.0       # its height at the toe
WING_UPPER_Z = 26.0  # the upper piece reaches this far back, to the headwall's face


def wing_top(z_total):
    """The wingwall's top at distance z_total from its toe."""
    return WING_TOE + (16 - WING_TOE) * z_total / WING_RUN


def wing_polys(z_lo, z_hi, offset):
    """A length of wingwall between z_lo and z_hi (sixteenths, in its own cell's frame, from
    the toe end), whose toe is ``offset`` sixteenths nearer the toe than this cell's z = 0; the
    parts past z = 16 are split off so each keeps its uv within the sprite."""
    x0, x1 = WING_X
    polys = []
    cuts = sorted(set([z_lo, z_hi] + [z for z in (16.0,) if z_lo < z < z_hi]))
    for za, zb in zip(cuts[:-1], cuts[1:]):
        ya, yb = wing_top(za + offset), wing_top(zb + offset)
        cell = 16.0 if za >= 16 else 0.0
        uv_side = lambda p, c=cell: ((p[2] * 16 - c) / 16, 1 - p[1])
        for x, nx in ((x0, -1), (x1, 1)):
            pts = [s16(x, 0, za), s16(x, 0, zb), s16(x, yb, zb), s16(x, ya, za)]
            polys.append(flat(pts, (nx, 0, 0), "precast", uv_side))
        pts = [s16(x0, ya, za), s16(x1, ya, za), s16(x1, yb, zb), s16(x0, yb, zb)]
        slope = (yb - ya) / (zb - za)
        n = (0.0, 1.0 / math.hypot(1, slope), -slope / math.hypot(1, slope))
        polys.append(flat(pts, n, "precast", lambda p, c=cell: (p[0], (p[2] * 16 - c) / 16)))
    y_toe = wing_top(z_lo + offset)
    pts = [s16(x0, 0, z_lo), s16(x1, 0, z_lo), s16(x1, y_toe, z_lo), s16(x0, y_toe, z_lo)]
    polys.append(flat(pts, (0, 0, -1), "precast", lambda p: (1 - p[0], 1 - p[1])))
    return polys


# ------------------------------------------------------------------------------------------
# The lift station: access hatches, the control panel on its rack, the standby generator
# ------------------------------------------------------------------------------------------
HATCH_TEX = {"concrete": T("precast"), "frame": W("aluminium"), "plate": W("diamond_plate"),
             "dark": W("dark"), "steel": W("steel"), "grate": T("safety_grate"),
             "particle": W("diamond_plate")}
COLLAR = 1.5         # the concrete collar's wall and the frame round the opening
LEAF_Y = 15.2        # the leaf's underside; its top is flush with the block's top


def hatches():
    """One leaf a block, hinged at the model's west edge; its pair is the same block facing the
    other way to the east, so the two leaves meet at the seam over the middle of the opening.
    The collar and frame are on the three sides that are not the seam."""
    c = COLLAR
    collar = [box([0, 0, 0], [c, LEAF_Y, 16], "concrete", faces=("north", "south", "east", "west",
                                                                   "down")),
              box([c, 0, 0], [16, LEAF_Y, c], "concrete", faces=("north", "south", "east",
                                                                  "down")),
              box([c, 0, 16 - c], [16, LEAF_Y, 16], "concrete", faces=("north", "south", "east",
                                                                        "down")),
              box([0, LEAF_Y, 0], [c, 16, 16], "frame", faces=("up", "east", "north", "south",
                                                               "west")),
              box([c, LEAF_Y, 0], [16, 16, c], "frame", faces=("up", "south", "north")),
              box([c, LEAF_Y, 16 - c], [16, 16, 16], "frame", faces=("up", "north", "south"))]
    leaf = [box([c, LEAF_Y, c], [16, 16, 16 - c], "plate", faces=("up", "down", "east", "north",
                                                                  "south")),
            box([10, 15.99, 7], [13, 16.02, 9], "dark", faces=("up",)),     # the lift handle
            box([c + 0.2, 16, 3], [c + 1.4, 16.5, 5], "steel", shift=(0, 16, 0)),              # hinge knuckles
            box([c + 0.2, 16, 11], [c + 1.4, 16.5, 13], "steel", shift=(0, 16, 0))]
    # open: the leaf stands on its hinge, its top turned to face west, the hold-open arm on it
    L = 16 - c
    opened = [box([0.3, 16, c], [1.1, 16 + L, 16 - c], "plate",
                  faces=("west", "east", "north", "south", "up"),
                  uv={"west": [c, 0, 16 - c, L], "east": [c, 0, 16 - c, L]}, shift=(0, 16, 0)),
              box([1.1, 20, 7.6], [1.6, 26, 8.4], "steel", shift=(0, 16, 0)),
              box([1.1, 20, 7.6], [4.5, 20.6, 8.4], "steel", shift=(0, 16, 0))]
    grate = [box([c, 14.4, c], [16, 14.9, 16 - c], "grate", faces=("up", "down"))]
    opened = [clamp_uv(e) for e in opened]
    models = {"hatch_collar": model(HATCH_TEX, collar), "hatch_leaf": model(HATCH_TEX, leaf),
              "hatch_leaf_open": model(HATCH_TEX, opened),
              "hatch_grate": model(HATCH_TEX, grate)}
    icon = model(HATCH_TEX, collar + leaf, ICON_DISPLAY)
    for reg, has_grate, names in (
            ("wet_well_hatch", True,
             names_of("Wet Well Access Hatch", "Pumpensumpf-Zugangsluke",
                      "Escotilla de Pozo Húmedo", "Pumpsumpslucka")),
            ("valve_vault_hatch", False,
             names_of("Valve Vault Access Hatch", "Schieberkammer-Zugangsluke",
                      "Escotilla de Cámara de Válvulas", "Ventilkammarlucka"))):
        parts = [("hatch_collar", {}), ("hatch_leaf", {"open": "false"}),
                 ("hatch_leaf_open", {"open": "true"})]
        if has_grate:
            parts.append(("hatch_grate", {"open": "true"}))
        C.add(reg, 'new BlockAccessHatch("%s", %s)' % (reg, "true" if has_grate else "false"),
              names, models if reg == "wet_well_hatch" else {}, multipart(facing_rules(parts)),
              item=icon, tab=TAB)


PANEL_TEX = {"steel": W("steel"), "stainless": W("aluminium"), "front": T("ls_panel_front"),
             "dark": W("dark"), "concrete": W("concrete"), "beacon": W("light"),
             "beacon_off": T("beacon_off"), "lamp": T("lamp_on"), "lamp_off": T("lamp_off"),
             "particle": W("aluminium")}


def control_panel():
    """The lift station's control panel on a unistrut rack: two posts set in a pad, the
    stainless enclosure between them, the alarm beacon on its roof and an LED flood light on an
    arm over its door. Powered by redstone the beacon flashes and the light is lit."""
    up = (0, 16, 0)
    rack = [box([0.5, 0, 9], [15.5, 1, 15], "concrete")]
    for x in (1.5, 13.3):
        rack.append(box([x, 1, 11.2], [x + 1.2, 16, 12.4], "steel",
                        faces=("north", "south", "east", "west")))
        rack.append(box([x, 16, 11.2], [x + 1.2, 28, 12.4], "steel", shift=up,
                        faces=("north", "south", "east", "west", "up")))
    for y in (7, 23.5):
        rack.append(box([1.5, y, 12.4], [14.5, y + 1.2, 13.6], "steel",
                        shift=up if y >= 16 else (0, 0, 0)))
    # the enclosure: body below and above the block line, its door a plane on the front
    rack += [box([3, 8, 8.2], [13, 16, 12.4], "stainless", faces=("north", "east", "west",
                                                                   "down")),
             box([3, 16, 8.2], [13, 22, 12.4], "stainless", shift=up,
                 faces=("north", "east", "west", "up")),
             box([12.2, 13, 7.8], [12.8, 17, 8.2], "dark")]
    # the door picture is 20 x 28 texels at the sheet's top left (10 x 14 in uv): split it at
    # the block line in proportion
    cut = (16 - 8.2) / (21.8 - 8.2)
    front_lo = box([3.2, 8.2, 8.18], [12.8, 16, 8.18], "front", faces=("north",),
                   uv={"north": [0, 14 * (1 - cut), 10, 14]})
    front_hi = box([3.2, 16, 8.18], [12.8, 21.8, 8.18], "front", faces=("north",),
                   uv={"north": [0, 0, 10, 14 * (1 - cut)]})
    rack += [front_lo, front_hi]
    # the flood light on its arm, reaching out over the door
    rack += [box([7.4, 27, 7], [8.6, 27.8, 11.2], "steel", shift=up),
             box([6, 26.2, 5.2], [10, 27.4, 7.6], "dark", shift=up)]
    rack = [clamp_uv(e) for e in rack]
    beacon = um.octagon("y", 5.5, 10.3, 1.4, 22, 22.8, "dark", False, True, shift=up)
    lens_on = um.octagon("y", 5.5, 10.3, 1.1, 22.8, 25.4, "beacon", False, True, shift=up)
    lens_off = um.octagon("y", 5.5, 10.3, 1.1, 22.8, 25.4, "beacon_off", False, True, shift=up)
    lamp_on = [box([6.3, 26.1, 5.4], [9.7, 26.2, 7.4], "lamp", faces=("down",),
                   uv={"down": [0, 0, 16, 16]}, shift=up)]
    lamp_off = [box([6.3, 26.1, 5.4], [9.7, 26.2, 7.4], "lamp_off", faces=("down",),
                    uv={"down": [0, 0, 16, 16]}, shift=up)]
    fix = lambda els: [clamp_uv(e) for e in els]
    unit = extent_unit(rack + beacon + lens_on)
    models = {"ls_panel": model(PANEL_TEX, rack), "ls_beacon": model(PANEL_TEX, fix(beacon)),
              "ls_beacon_on": model(PANEL_TEX, fix(lens_on), ao=False),
              "ls_beacon_off": model(PANEL_TEX, fix(lens_off)),
              "ls_lamp_on": model(PANEL_TEX, fix(lamp_on), ao=False),
              "ls_lamp_off": model(PANEL_TEX, fix(lamp_off))}
    rules = facing_rules([("ls_panel", {}), ("ls_beacon", {}), ("ls_beacon_on", {"on": "true"}),
                          ("ls_beacon_off", {"on": "false"}), ("ls_lamp_on", {"on": "true"}),
                          ("ls_lamp_off", {"on": "false"})])
    models["ls_panel_icon"] = model(PANEL_TEX, rack + fix(beacon) + fix(lens_off)
                                    + fix(lamp_off), big_display(0.5))
    C.add("lift_station_control_panel",
          'new BlockSwitchedUnit("lift_station_control_panel", new UtilityBoxSpec(1, 1, 2, '
          'new AxisAlignedBB(%s), null), false, 12)' % ", ".join(fmt(v) for v in unit),
          names_of("Lift Station Control Panel", "Pumpwerk-Steuerschrank",
                   "Panel de Control de Estación de Bombeo", "Pumpstationsstyrskåp"),
          models, multipart(rules, inventory="ls_panel_icon"), tab=TAB)


GEN_TEX = {"enclosure": T("enclosure"), "plain": T("enclosure_plain"), "black": W("black"),
           "dark": W("dark"), "steel": W("steel"), "display": T("gen_display"),
           "display_off": T("gen_display_off"), "concrete": W("concrete"),
           "particle": T("enclosure_plain")}


def generator():
    """The standby generator: a sand enclosure on its sub-base fuel tank, two blocks long
    (growing to the placer's right, the model's west) and a block and a half tall, its
    controller window and the exhaust's rain cap on top. A click starts or stops it: running,
    the display shows RUN and its readings and the rain cap stands open."""
    up = (0, 16, 0)
    body = [box([-15.6, 0, 1.2], [15.6, 3.6, 14.8], "black"),
            box([-15.2, 3.6, 1.6], [15.2, 16, 14.4], "plain",
                faces=("north", "south", "east", "west"),
                per={"east": "enclosure", "west": "enclosure"}),
            box([-15.2, 16, 1.6], [15.2, 22.4, 14.4], "plain", shift=up,
                faces=("north", "south", "east", "west"),
                per={"east": "enclosure", "west": "enclosure"}),
            # the louvred intake over the doors, front and back
            box([-14, 17.5, 1.5], [14, 21.2, 1.6], "enclosure", faces=("north",), shift=up),
            box([-14, 17.5, 14.4], [14, 21.2, 14.5], "enclosure", faces=("south",), shift=up),
            box([-15.6, 22.4, 1.2], [15.6, 23.2, 14.8], "plain", shift=up),
            # door seams, handles and the lifting eyes
            box([-0.2, 4.6, 1.4], [0.2, 17.2, 1.6], "dark"),
            box([-8.2, 11.4, 1.3], [-7.6, 13.6, 1.6], "steel", shift=(-16, 0, 0)),
            box([7.6, 11.4, 1.3], [8.2, 13.6, 1.6], "steel"),
            box([-14.4, 23.2, 7.4], [-13.2, 24.2, 8.6], "steel", shift=(-16, 16, 0)),
            box([13.2, 23.2, 7.4], [14.4, 24.2, 8.6], "steel", shift=up),
            # the fuel fill and vent on the tank
            box([11, 3.6, 2.2], [12.4, 4.6, 3.6], "steel"),
            # the controller's window frame
            box([9.4, 14.4, 1.3], [14, 18.6, 1.6], "dark", shift=(0, 0, 0))]
    body += um.octagon("y", -9, 8, 1.1, 23.2, 27.6, "steel", False, False, shift=(-16, 16, 0))
    body = [clamp_uv(e) for e in body]
    disp = [box([10, 15, 1.28], [13.4, 18, 1.28], "display", faces=("north",),
                uv={"north": [0, 0, 16, 16]})]
    disp_off = [box([10, 15, 1.28], [13.4, 18, 1.28], "display_off", faces=("north",),
                    uv={"north": [0, 0, 16, 16]})]
    cap_shut = [box([-10.5, 27.6, 6.5], [-7.5, 28.0, 9.5], "steel", shift=(-16, 16, 0))]
    cap_open = [box([-10.5, 27.6, 9.2], [-7.5, 31.2, 9.6], "steel", shift=(-16, 16, 0))]
    fix = lambda els: [clamp_uv(e) for e in els]
    unit = extent_unit(body + cap_open)
    models = {"gen_body": model(GEN_TEX, body), "gen_run": model(GEN_TEX, fix(disp), ao=False),
              "gen_stop": model(GEN_TEX, fix(disp_off)),
              "gen_cap_shut": model(GEN_TEX, fix(cap_shut)),
              "gen_cap_open": model(GEN_TEX, fix(cap_open))}
    rules = facing_rules([("gen_body", {}), ("gen_run", {"on": "true"}),
                          ("gen_stop", {"on": "false"}), ("gen_cap_open", {"on": "true"}),
                          ("gen_cap_shut", {"on": "false"})])
    models["gen_icon"] = model(GEN_TEX, body + fix(disp_off) + fix(cap_shut), big_display(0.4))
    C.add("lift_station_generator",
          'new BlockSwitchedUnit("lift_station_generator", new UtilityBoxSpec(2, 1, 2, '
          'new AxisAlignedBB(%s), null), true, 0)' % ", ".join(fmt(v) for v in unit),
          names_of("Standby Generator", "Notstromaggregat", "Generador de Emergencia",
                   "Reservgenerator"),
          models, multipart(rules, inventory="gen_icon"), tab=TAB)


# ------------------------------------------------------------------------------------------
# The curb inlet
# ------------------------------------------------------------------------------------------
THROAT = (12.4, 14.6)    # the curb opening, under the curb angle; the road is at y = 12
THROAT_D = 3.0           # how deep the opening goes back under the top slab
STORM_LID = "csm:blocks/streetscape/manhole_storm"   # Roads' Streetscape cover


def curb_inlet():
    """A curb-opening inlet set in the curb line, facing the road: the top slab level with the
    sidewalk at the block's top, the opening a curb's height above the road (the road in front at
    y = 12, the external road mod's curb) under a steel curb angle. Side by side they are one
    long inlet: the opening's end walls are drawn only where the run stops, and Streetscape's
    STORM manhole lid only in the top at its west end."""
    t0, t1 = THROAT
    d = THROAT_D
    tex = {"precast": T("precast"), "dark": T("precast_dark"), "steel": W("steel"),
           "lid": STORM_LID, "particle": T("precast")}
    body = [box([0, 0, 0], [16, t0, 16], "precast", faces=("north", "south", "down", "up"),
                per={"up": "dark"}),
            box([0, t0, d], [16, 16, 16], "precast", faces=("north", "south", "up"),
                per={"north": "dark"}),
            box([0, t1, 0], [16, 16, d], "steel", faces=("north", "down", "up"),
                per={"down": "dark"})]

    def ends(side):
        x = 0 if side == "west" else 16
        faces = (side,)
        els = [box([0, 0, 0], [16, t0, 16], "precast", faces=faces),
               box([0, t0, d], [16, 16, 16], "precast", faces=faces),
               box([0, t1, 0], [16, 16, d], "steel", faces=faces)]
        wall_x = (0, 1.4) if side == "west" else (14.6, 16)
        inner = "east" if side == "west" else "west"
        els.append(box([wall_x[0], t0, 0], [wall_x[1], t1, d], "precast",
                       faces=("north", inner)))
        return els

    lid = [box([0, 16, 0], [16, 16.02, 16], "lid", faces=("up",), uv={"up": [0, 0, 16, 16]})]
    models = {"curb_inlet_body": model(tex, body), "curb_inlet_end_w": model(tex, ends("west")),
              "curb_inlet_end_e": model(tex, ends("east")), "curb_inlet_lid": model(tex, lid)}
    rules = facing_rules([("curb_inlet_body", {}), ("curb_inlet_end_w", {"left": "false"}),
                          ("curb_inlet_end_e", {"right": "false"}),
                          ("curb_inlet_lid", {"left": "false"})])
    models["curb_inlet_icon"] = model(tex, body + ends("west") + ends("east") + lid,
                                      ICON_DISPLAY)
    C.add("curb_inlet", 'new BlockPrecastRun("curb_inlet", new double[]{0, 0, 0, 16, 16, 16})',
          names_of("Curb Inlet", "Bordsteinablauf", "Imbornal de Bordillo", "Kantstensbrunn"),
          models, multipart(rules, inventory="curb_inlet_icon"), tab=TAB)


# ------------------------------------------------------------------------------------------
# The stormwater outfall: headwall, pipe mouth, flap gate, wingwall, riprap
# ------------------------------------------------------------------------------------------
HW_TEX = {"precast": T("precast"), "inside": T("precast_dark"), "iron": T("iron"),
          "dark": W("dark"), "steel": W("steel"), "particle": T("precast")}
COPING_Y = 17.2


def headwall():
    """The outfall's headwall: a wall 6 px thick at the back of the cell, its front face facing
    downstream (the player who placed it), with a coping along its top where nothing of the wall
    is above; side by side and stacked the walls are one wall, and a wall's end faces are drawn
    only where it stops. One piece of it carries the pipe's mouth, and one the pipe with its
    flap gate."""
    z = HW_Z
    front = [box([0, 0, z], [16, 16, 16], "precast", faces=("north",))]
    back = [box([0, 0, z], [16, 16, 16], "precast", faces=("south",))]
    coping = [box([0, 16, z - 0.6], [16, COPING_Y, 16], "precast",
                  faces=("north", "south", "up", "down"), shift=(0, 16, 0))]
    end_w = [box([0, 0, z], [16, 16, 16], "precast", faces=("west",))]
    end_e = [box([0, 0, z], [16, 16, 16], "precast", faces=("east",))]
    cope_w = [box([0, 16, z - 0.6], [16, COPING_Y, 16], "precast", faces=("west",),
                  shift=(0, 16, 0))]
    cope_e = [box([0, 16, z - 0.6], [16, COPING_Y, 16], "precast", faces=("east",),
                  shift=(0, 16, 0))]
    hinge = [box([3.5, 14.2, 7.0], [12.5, 15.0, 7.9], "iron"),
             box([3.6, 14.2, 7.9], [5.0, 15.6, z], "iron"),
             box([11.0, 14.2, 7.9], [12.4, 15.6, z], "iron"),
             box([6.6, 8.6, 6.2], [7.4, 14.2, 7.0], "iron"),
             box([8.6, 8.6, 6.2], [9.4, 14.2, 7.0], "iron")]
    models = {"hw_front": model(HW_TEX, front), "hw_back": model(HW_TEX, back),
              "hw_coping": model(HW_TEX, coping), "hw_end_w": model(HW_TEX, end_w),
              "hw_end_e": model(HW_TEX, end_e), "hw_cope_w": model(HW_TEX, cope_w),
              "hw_cope_e": model(HW_TEX, cope_e), "hw_hinge": model(HW_TEX, hinge)}
    pipe = write_obj("hw_pipe", wall_face_with_hole(PIPE_OUT) + pipe_polys())
    flap = write_obj("hw_flap", flap_polys())
    common = [("hw_back", {}), ("hw_coping", {"up": "false"}), ("hw_end_w", {"left": "false"}),
              ("hw_end_e", {"right": "false"}),
              ("hw_cope_w", {"left": "false", "up": "false"}),
              ("hw_cope_e", {"right": "false", "up": "false"})]
    wall_els = back + coping + end_w + end_e + cope_w + cope_e
    pipe_icon = um.octagon("z", 8, PIPE_Y, PIPE_OUT, PIPE_FRONT, z, "precast", True, False) + [
        box([4.4, PIPE_Y - 3.6, PIPE_FRONT - 0.02], [11.6, PIPE_Y + 3.6, PIPE_FRONT - 0.02],
            "dark", faces=("north",))]
    flap_icon = um.octagon("z", 8, PIPE_Y, FLAP_R, PIPE_FRONT - 0.9, PIPE_FRONT - 0.1, "iron",
                           True, False) + hinge
    box_plain = box_java(front + coping, top=COPING_Y)
    for reg, extra, icon_extra, names in (
            ("outfall_headwall", [("hw_front", {})], [],
             names_of("Outfall Headwall", "Auslauf-Stirnwand", "Muro de Cabecera de Descarga",
                      "Utloppsmur")),
            ("outfall_headwall_pipe", [(pipe, {})], pipe_icon,
             names_of("Outfall Headwall (Pipe)", "Auslauf-Stirnwand (Rohr)",
                      "Muro de Cabecera de Descarga (Tubo)", "Utloppsmur (Rör)")),
            ("outfall_headwall_flap_gate", [(pipe, {}), (flap, {}), ("hw_hinge", {})],
             pipe_icon + flap_icon,
             names_of("Outfall Headwall (Flap Gate)", "Auslauf-Stirnwand (Rückstauklappe)",
                      "Muro de Cabecera de Descarga (Compuerta de Charnela)",
                      "Utloppsmur (Backklaff)"))):
        icon = reg + "_icon"
        # an OBJ is no item model's parent here, so the icon draws the pipe as JSON octagons
        m = dict(models) if reg == "outfall_headwall" else {}
        m[icon] = model(HW_TEX, front + wall_els + icon_extra, ICON_DISPLAY)
        if reg == "outfall_headwall":
            jbox = box_plain
        else:
            front_z = PIPE_FRONT - (0.9 if reg.endswith("gate") else 0)
            jbox = "new double[]{0, 0, %s, 16, %s, 16}" % (fmt(front_z), fmt(COPING_Y))
        C.add(reg, 'new BlockHeadwall("%s", %s)' % (reg, jbox), names, m,
              multipart(facing_rules(common + extra), inventory=icon), tab=TAB)


def wingwall():
    """The outfall's wingwall, a U-type wall running downstream from the headwall's end: the
    first block reaches back to the headwall's face, the second (where a wingwall is behind it)
    runs on down to the toe. One slope over both, so a two-block wingwall reads as one wall."""
    upper = write_obj("wing_upper", wing_polys(0.0, WING_UPPER_Z, 16.0))
    lower = write_obj("wing_lower", wing_polys(0.0, 16.0, 0.0))
    icon = [box([WING_X[0], 0, 0], [WING_X[1], 10, 16], "precast")]
    models = {"wing_icon": model(HW_TEX, icon, ICON_DISPLAY)}
    rules = facing_rules([(upper, {"lower": "false"}), (lower, {"lower": "true"})])
    C.add("outfall_wingwall", 'new BlockWingwall("outfall_wingwall", %s, %s)'
          % (fmt(wing_top(16.0 + 16.0)), fmt(wing_top(16.0))),
          names_of("Outfall Wingwall", "Auslauf-Flügelmauer", "Muro de Ala de Descarga",
                   "Utloppsvingmur"),
          models, multipart(rules, inventory="wing_icon"), tab=TAB)


def _stones(seed, n):
    """A layer of riprap: a bed and n angular stones, some turned about y, some tipped."""
    rng = random.Random(seed)
    els = [box([0, 0, 0], [16, 1.4, 16], "stone", faces=("up", "north", "south", "east", "west"))]
    placed = []
    tries = 0
    while len(placed) < n and tries < 400:
        tries += 1
        w, d = rng.uniform(3.2, 5.6), rng.uniform(3.2, 5.6)
        h = rng.uniform(2.2, 4.4)
        cx, cz = rng.uniform(w / 2 + 0.3, 16 - w / 2 - 0.3), rng.uniform(d / 2 + 0.3,
                                                                         16 - d / 2 - 0.3)
        if any(math.hypot(cx - px, cz - pz) < (w + pw) / 2.6 for px, pz, pw in placed):
            continue
        placed.append((cx, cz, w))
        y0 = rng.uniform(0.3, 0.9)
        e = box([cx - w / 2, y0, cz - d / 2], [cx + w / 2, y0 + h, cz + d / 2], "stone",
                faces=("up", "north", "south", "east", "west"))
        kind = rng.random()
        if kind < 0.55:
            e["rotation"] = {"origin": [round(cx, 3), round(y0, 3), round(cz, 3)], "axis": "y",
                             "angle": rng.choice([-45, -22.5, 22.5, 45])}
        elif kind < 0.8:
            e["rotation"] = {"origin": [round(cx, 3), round(y0, 3), round(cz, 3)],
                             "axis": rng.choice(["x", "z"]), "angle": rng.choice([-22.5, 22.5])}
        els.append(e)
    return els


RIPRAP_H = 5.0


def riprap():
    """Riprap: a layer of broken stone on the ground, for an outfall's apron and a channel's
    banks. Two drawings, each turned four ways, picked by the block's position (a vanilla
    blockstate's weighted list), so an apron shows no repeat and the block stores nothing."""
    tex = {"stone": T("riprap"), "particle": T("riprap")}
    models = {"riprap_a": model(tex, _stones(401, 11)), "riprap_b": model(tex, _stones(402, 12))}
    variants = []
    for name in ("riprap_a", "riprap_b"):
        for y in (0, 90, 180, 270):
            v = {"model": M(name)}
            if y:
                v["y"] = y
            variants.append(v)
    C.add("riprap", 'new BlockRiprap("riprap", %s)' % fmt(RIPRAP_H),
          names_of("Riprap", "Steinschüttung", "Escollera", "Stenskoning"),
          models, {"variants": {"normal": variants}},
          item={"parent": "csm:block/utilities/sewer/riprap_a", "display": ICON_DISPLAY},
          tab=TAB)


# ------------------------------------------------------------------------------------------
# The detention pond: the outlet riser and the emergency spillway
# ------------------------------------------------------------------------------------------
RISER_T = 2.0
ORIFICE = (6.0, 2.0, 10.0, 6.0)      # x0, y0, x1, y1 on the front wall
NOTCH = (5.0, 11.0, 11.0, 16.0)


def outlet_riser():
    """The detention pond's outlet riser: a precast box a block square, stacked a section at a
    time. The lowest section has the floor and the low-flow orifice in its front; the highest
    has the weir notch in its front and the trash rack over its top. Nothing is stored: both
    ends are actual state, like the manhole's."""
    t = RISER_T
    tex = {"precast": T("precast"), "inside": T("precast_dark"), "rack": T("bar_rack"),
           "steel": W("steel"), "dark": W("dark"), "particle": T("precast")}
    side = {"north": "inside"}
    walls = [box([0, 0, 16 - t], [16, 16, 16], "precast", faces=("north", "south", "east",
                                                                 "west", "up"), per=side),
             box([0, 0, t], [t, 16, 16 - t], "precast", faces=("east", "west", "up"),
                 per={"east": "inside"}),
             box([16 - t, 0, t], [16, 16, 16 - t], "precast", faces=("east", "west", "up"),
                 per={"west": "inside"})]

    def front(holes):
        """The front wall with its holes cut: [(x0, y0, x1, y1)]."""
        xs = sorted(set([0.0, 16.0] + [h[0] for h in holes] + [h[2] for h in holes]))
        els = []
        for xa, xb in zip(xs[:-1], xs[1:]):
            gaps = [(h[1], h[3]) for h in holes if h[0] <= xa and xb <= h[2]]
            spans = [(0.0, 16.0)]
            for g0, g1 in gaps:
                new = []
                for s0, s1 in spans:
                    if g1 <= s0 or g0 >= s1:
                        new.append((s0, s1))
                        continue
                    if g0 > s0:
                        new.append((s0, g0))
                    if g1 < s1:
                        new.append((g1, s1))
                spans = new
            for y0, y1 in spans:
                els.append(box([xa, y0, 0], [xb, y1, t], "precast", per={"south": "inside"}))
        return els

    fronts = {"plain": front([]), "orifice": front([ORIFICE]), "notch": front([NOTCH]),
              "both": front([ORIFICE, NOTCH])}
    floor = [box([t, 0, t], [16 - t, 1, 16 - t], "inside", faces=("up",))]
    ox0, oy0, ox1, oy1 = ORIFICE
    plate = [box([ox0 - 1, oy0 - 1, -0.3], [ox1 + 1, oy0, 0], "steel"),
             box([ox0 - 1, oy1, -0.3], [ox1 + 1, oy1 + 1, 0], "steel"),
             box([ox0 - 1, oy0, -0.3], [ox0, oy1, 0], "steel"),
             box([ox1, oy0, -0.3], [ox1 + 1, oy1, 0], "steel")]
    # the plate is a frame round the hole in the front wall
    rack = []
    lo, hi, top = -0.3, 16.3, 24.0
    rack.append(box([lo, 16, lo], [hi, top, lo], "rack", faces=("north", "south"),
                    uv={"north": [0, 0, 16, 8], "south": [0, 0, 16, 8]}, shift=(0, 16, 0)))
    rack.append(box([lo, 16, hi], [hi, top, hi], "rack", faces=("north", "south"),
                    uv={"north": [0, 0, 16, 8], "south": [0, 0, 16, 8]}, shift=(0, 16, 0)))
    rack.append(box([lo, 16, lo], [lo, top, hi], "rack", faces=("east", "west"),
                    uv={"east": [0, 0, 16, 8], "west": [0, 0, 16, 8]}, shift=(0, 16, 0)))
    rack.append(box([hi, 16, lo], [hi, top, hi], "rack", faces=("east", "west"),
                    uv={"east": [0, 0, 16, 8], "west": [0, 0, 16, 8]}, shift=(0, 16, 0)))
    rack.append(box([lo, top, lo], [hi, top, hi], "rack", faces=("up", "down"),
                    uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}, shift=(0, 16, 0)))
    rack += [box([0, top - 0.8, 0], [16, top, 0.6], "steel", shift=(0, 16, 0)),
             box([0, top - 0.8, 15.4], [16, top, 16], "steel", shift=(0, 16, 0))]
    rack = [clamp_uv(e) for e in rack]
    models = {"riser_walls": model(tex, walls), "riser_floor": model(tex, floor),
              "riser_orifice_plate": model(tex, plate), "riser_rack": model(tex, rack)}
    for k, els in fronts.items():
        models["riser_front_" + k] = model(tex, els)
    parts = [("riser_walls", {}), ("riser_front_plain", {"base": "false", "top": "false"}),
             ("riser_front_orifice", {"base": "true", "top": "false"}),
             ("riser_front_notch", {"base": "false", "top": "true"}),
             ("riser_front_both", {"base": "true", "top": "true"}),
             ("riser_floor", {"base": "true"}), ("riser_orifice_plate", {"base": "true"}),
             ("riser_rack", {"top": "true"})]
    models["riser_icon"] = model(tex, walls + fronts["both"] + plate, ICON_DISPLAY)
    C.add("outlet_riser", 'new BlockStackedSection("outlet_riser", "outlet", false, false)',
          names_of("Pond Outlet Riser", "Teich-Auslaufbauwerk", "Torre de Descarga de Estanque",
                   "Dammutloppstorn"),
          models, multipart(facing_rules(parts), inventory="riser_icon"), tab=TAB)


SPILL_Y = 3.0
TRAIN_Y = 12.0
TRAIN_T = 2.0


def spillway():
    """The emergency spillway's crest: a concrete slab level across the embankment's notch, the
    flow running toward the model's north, with its cutoff lip along the upstream edge and its
    end sill downstream; side by side the crest is one, with a training wall at each end."""
    tex = {"precast": T("precast"), "particle": T("precast")}
    slab = [box([0, 0, 0], [16, SPILL_Y, 16], "precast", faces=("up", "north", "south")),
            box([0, SPILL_Y, 14.6], [16, SPILL_Y + 0.8, 16], "precast",
                faces=("up", "north", "south")),
            box([0, SPILL_Y, 0], [16, SPILL_Y + 1.2, 1.2], "precast",
                faces=("up", "north", "south"))]

    def ends(side):
        f = (side,)
        els = [box([0, 0, 0], [16, SPILL_Y, 16], "precast", faces=f),
               box([0, SPILL_Y, 14.6], [16, SPILL_Y + 0.8, 16], "precast", faces=f),
               box([0, SPILL_Y, 0], [16, SPILL_Y + 1.2, 1.2], "precast", faces=f)]
        x0, x1 = (0, TRAIN_T) if side == "west" else (16 - TRAIN_T, 16)
        els.append(box([x0, SPILL_Y, 0], [x1, TRAIN_Y, 16], "precast"))
        return els

    models = {"spill_slab": model(tex, slab), "spill_end_w": model(tex, ends("west")),
              "spill_end_e": model(tex, ends("east"))}
    rules = facing_rules([("spill_slab", {}), ("spill_end_w", {"left": "false"}),
                          ("spill_end_e", {"right": "false"})])
    models["spill_icon"] = model(tex, slab + ends("west") + ends("east"), ICON_DISPLAY)
    C.add("emergency_spillway", 'new BlockPrecastRun("emergency_spillway", %s)'
          % box_java(slab),
          names_of("Emergency Spillway", "Notüberlauf", "Aliviadero de Emergencia",
                   "Nödutskov"),
          models, multipart(rules, inventory="spill_icon"), tab=TAB)


# ------------------------------------------------------------------------------------------
# Manhole riser sections and the cone
# ------------------------------------------------------------------------------------------
MH_TEX = {"step": W("yellow"), "particle": T("precast")}


def rung(y, r):
    """A manhole step on the back wall at height y, the wall's inner radius r there: a U of
    coated bar standing 1.6 px out from the wall."""
    z_wall = 8 + r
    return [box([6, y, z_wall - 1.6], [10, y + 0.8, z_wall - 0.8], "step"),
            box([6, y, z_wall - 0.8], [6.8, y + 0.8, z_wall + 0.4], "step",
                faces=("up", "down", "east", "west")),
            box([9.2, y, z_wall - 0.8], [10, y + 0.8, z_wall + 0.4], "step",
                faces=("up", "down", "east", "west"))]


STEP_Y = (3.0, 8.3, 13.6)


def cone_r(y):
    if y <= CONE_SHOULDER:
        return MH_IN + (CONE_TOP_IN - MH_IN) * y / CONE_SHOULDER
    return CONE_TOP_IN


def manholes():
    """Precast manhole sections, a block across (0.75 block clear inside, room to climb): the
    riser ring stacks and picks its ends from its neighbours, the base (bench and channel)
    where nothing of the manhole is below and the open rim where nothing is above; the cone
    tops it off at the size of Streetscape's manhole cover, which is placed on the block above.
    Steps on the back wall, climbed like a ladder. The cutaway sections are the back half,
    their cut faces showing the wall, for a manhole seen from the side."""
    ring = write_obj("mh_ring", ring_polys())
    ring_top = write_obj("mh_ring_top", ring_top_polys())
    base = write_obj("mh_base", base_polys())
    cone = write_obj("mh_cone", cone_polys())
    ring_c = write_obj("mh_ring_cut", back_half(ring_polys()) + ring_cut())
    top_c = write_obj("mh_ring_top_cut", back_half(ring_top_polys()))
    base_c = write_obj("mh_base_cut", back_half(base_polys()) + base_cut())
    cone_c = write_obj("mh_cone_cut", back_half(cone_polys()) + cone_cut())
    steps = [e for y in STEP_Y for e in rung(y, MH_IN)]
    cone_steps = [e for y in STEP_Y for e in rung(y, cone_r(y + 0.4))]
    models = {"mh_steps": model(MH_TEX, steps), "mh_cone_steps": model(MH_TEX, cone_steps)}
    icon_tex = {"precast": T("precast"), "inside": T("precast_dark"), "iron": T("iron"),
                "particle": T("precast")}
    ring_icon = um.octagon("y", 8, 8, 7.4, 0, 16, "precast", True, True)
    cut_icon = [box([0.6, 0, 13.6], [15.4, 16, 15.4], "precast", per={"north": "inside"}),
                box([0.6, 0, 8], [2.4, 16, 13.6], "precast", per={"east": "inside"}),
                box([13.6, 0, 8], [15.4, 16, 13.6], "precast", per={"west": "inside"})]
    cone_icon = (um.octagon("y", 8, 8, 7.4, 0, 6, "precast", False, True)
                 + um.octagon("y", 8, 8, 6.8, 6, 11, "precast", False, False)
                 + um.octagon("y", 8, 8, 6.2, 11, 13.5, "precast", False, False)
                 + um.octagon("y", 8, 8, 6.4, 13.5, 16, "iron", True, False))
    for reg, cut, names in (
            ("manhole_riser", False,
             names_of("Manhole Riser Section", "Schachtring", "Anillo de Pozo de Registro",
                      "Brunnsring")),
            ("manhole_riser_cutaway", True,
             names_of("Manhole Riser Section (Cutaway)", "Schachtring (Schnitt)",
                      "Anillo de Pozo de Registro (Corte)", "Brunnsring (Genomskärning)"))):
        parts = [(ring_c if cut else ring, {}), (ring_top if not cut else top_c, {"top": "true"}),
                 (base_c if cut else base, {"base": "true"}), ("mh_steps", {})]
        icon = reg + "_icon"
        m = dict(models) if not cut else {}
        m[icon] = model(icon_tex, ring_icon if not cut else cut_icon, ICON_DISPLAY)
        C.add(reg, 'new BlockStackedSection("%s", "manhole", true, %s)'
              % (reg, "true" if cut else "false"), names, m,
              multipart(facing_rules(parts), inventory=icon), tab=TAB)
    for reg, cut, names in (
            ("manhole_cone", False,
             names_of("Manhole Cone", "Schachtkonus", "Cono de Pozo de Registro", "Brunnskon")),
            ("manhole_cone_cutaway", True,
             names_of("Manhole Cone (Cutaway)", "Schachtkonus (Schnitt)",
                      "Cono de Pozo de Registro (Corte)", "Brunnskon (Genomskärning)"))):
        parts = [(cone_c if cut else cone, {}), ("mh_cone_steps", {})]
        icon = reg + "_icon"
        C.add(reg, 'new BlockManholeCone("%s", %s)' % (reg, "true" if cut else "false"), names,
              {icon: model(icon_tex, cone_icon, ICON_DISPLAY)},
              multipart(facing_rules(parts), inventory=icon), tab=TAB)


# ------------------------------------------------------------------------------------------
# Everything, and the command line
# ------------------------------------------------------------------------------------------
hatches()
control_panel()
generator()
curb_inlet()
headwall()
wingwall()
riprap()
outlet_riser()
spillway()
manholes()
C.add_lang("csm.utilities.hatch.blocked", (
    "Not enough room for this hatch -- blocked at %s, %s, %s",
    "Nicht genug Platz für diese Luke -- blockiert bei %s, %s, %s",
    "No hay espacio para esta escotilla -- bloqueada en %s, %s, %s",
    "Inte tillräckligt med plats för luckan -- blockerad vid %s, %s, %s"))
C.add_lang("csm.utilities.generator.running", ("Generator running", "Aggregat läuft",
                                               "Generador en marcha", "Generatorn går"))
C.add_lang("csm.utilities.generator.stopped", ("Generator stopped", "Aggregat gestoppt",
                                               "Generador parado", "Generatorn stoppad"))


def report():
    for k in sorted(OBJ_QUADS):
        print("%-18s %4d quads" % (k, OBJ_QUADS[k]))


if __name__ == "__main__":
    if "--report" in sys.argv:
        report()
        sys.exit(0)
    sys.exit(C.main())
