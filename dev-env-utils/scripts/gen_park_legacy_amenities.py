#!/usr/bin/env python3
"""
gen_park_legacy_amenities.py -- the older Parks-tab amenities, redrawn as our own work.

Five blocks that predate the Parks & Greenery generators kept their registry names, classes and
tab places, and get their models from here: two swing sets (parkswinga, a two-seat belt swing on
a red A-frame; parkswingb, a tyre swing on a blue one), a seesaw (teetertotter), a slatted wooden
litter receptacle (parktrashcan) and a cast-stone pedestal bird bath (birdbath). Everything is
drawn from real-world sizes (1 block = 1 m, 16 px = 1 m) and written under
modules/parks/.../assets/csm:

  * textures/blocks/parks/amenities/<new textures>.png  (the rest are gen_park_amenities.py's,
    referenced and never rewritten here)
  * models/block/parks/amenities/seesaw.json and litter_receptacle_wood.json
  * models/block/parks/amenities/swing_belt_set.obj, swing_tyre_set.obj (+ swing_sets.mtl):
    the swing sets stand 2.77 m to the top of the beam, past the y = 32 a JSON element may reach,
    so they are drawn as the same boxes (cbox) written out as OBJ faces (obj_from_elements),
    one material per texture key, retextured by the blockstate
  * models/block/parks/amenities/birdbath_cast.obj (+ .mtl): an eight-sided lathe
  * blockstates/<registry>.json, keeping the six-way facing the blocks were placed with

This writes no lang: the names are the blocks' existing ones.

The swing sets and the seesaw are three blocks wide, centred on the placed block. A JSON element
may run from -16 to 32, but a face on one that reaches past the cell samples a neighbouring atlas
sprite unless its UVs stay inside 0..16, so every long member is cut at the block boundaries
(cbox) and each piece gets UVs fitted to its own cell; the OBJ swing sets keep the same cuts, for
the same texel density. A-frame legs and the seesaw board are turned 22.5 degrees, the one
element angle that is also the real splay of an A-frame and the tilt of a 3 m seesaw resting on
one end over a 0.6 m pivot.

audit_obj_models.py reads only the first three corners of each face, and the swing sets are
written as quads, so audit a triangulated copy of them to have every face checked.

Every model faces north (model -Z): a swing's seats swing north-south under a beam running east
to west; the seesaw's board runs east to west.

Usage:
    python gen_park_legacy_amenities.py              # write everything
    python gen_park_legacy_amenities.py --check      # fail if the tree has drifted

Requires Pillow.
"""
import argparse
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_park_plantings as pp  # noqa: E402
import model_depth  # noqa: E402
import gen_pedestal_pole as pole  # noqa: E402
import gen_trees  # noqa: E402

ASSETS = pp.ASSETS
TEX = "csm:blocks/parks/amenities/"
MODEL = "csm:parks/amenities/"
face = pp.face
noise_tex = pp.noise_tex
clamp = pp.clamp


def T(name):
    return TEX + name


# ------------------------------------------------------------------------------------------
# Textures (new ones; red_steel, platform_blue, iron, teak, yellow_plastic and water are
# gen_park_amenities.py's)
# ------------------------------------------------------------------------------------------
def chain():
    """Galvanised chain seen edge on: a pale link, a dark gap, every three pixels."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 3 == 2:
                c = (92, 96, 100)
            else:
                c = (176, 180, 184) if (x + y) % 2 else (156, 160, 166)
            px[x, y] = c + (255,)
    return img


def belt_rubber():
    return noise_tex([(50, 50, 54), (42, 42, 46), (34, 34, 38), (28, 28, 30)], 201, grain=0.5)


def tyre():
    """Black tyre rubber with a zig-zag tread."""
    img = noise_tex([(46, 46, 48), (40, 40, 42), (34, 34, 36)], 202, grain=0.6)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + (y // 2) * 2) % 5 == 0:
                px[x, y] = (20, 20, 22, 255)
    return img


def litter_slat():
    """Ipe-like hardwood slats, the grain running up the texture (the slats stand upright)."""
    rng = random.Random(203)
    base = [(150, 98, 60), (138, 88, 52), (126, 80, 46), (112, 70, 40)]
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    tones = [rng.randrange(len(base)) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            i = tones[x]
            if rng.random() < 0.18:
                i = min(len(base) - 1, i + 1)
            c = tuple(v + rng.randint(-4, 4) for v in base[i])
            px[x, y] = clamp(c) + (255,)
    return img


def litter_liner():
    """Looking into the receptacle: a black bag gathered over the liner's rim."""
    rng = random.Random(204)
    img = noise_tex([(38, 40, 38), (30, 32, 30), (24, 26, 24), (18, 20, 18)], 204, grain=0.7)
    px = img.load()
    for _ in range(10):  # folds catching the light
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        for k in range(rng.randint(2, 4)):
            if 0 <= x + k < 16:
                px[x + k, y] = (58, 60, 58, 255)
    return img


def cast_stone():
    """Cast stone: a pale, warm grey with sand and small dark voids."""
    return noise_tex([(196, 192, 182), (186, 182, 172), (176, 172, 162), (164, 160, 150)], 205,
                     grain=0.85, speckle=0.12,
                     speck_colour=[(140, 136, 128), (214, 210, 200), (120, 116, 110)])


TEXTURES = {
    "swing_chain": chain,
    "swing_belt": belt_rubber,
    "swing_tyre": tyre,
    "litter_slat": litter_slat,
    "litter_liner": litter_liner,
    "birdbath_stone": cast_stone,
}


# ------------------------------------------------------------------------------------------
# JSON element helpers
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")
CUT_FACE = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}


def _uvs(lo, hi):
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    return {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }


def cbox(frm, to, tex, faces=ALL, rot=None, per=None, bound=True):
    """A box cut at every block boundary it crosses; each piece's UVs are fitted to its place
    in its own cell, so no face samples past its sprite and nothing stretches. The pieces share
    one rotation (same origin), so a turned member stays one straight member. Faces on the cuts
    are left out. Returns a list of elements. bound=False lifts the JSON -16..32 limit, for
    elements that only ever become OBJ faces (obj_from_elements)."""
    pieces = [(list(frm), list(to), set(faces))]
    for axis in range(3):
        out = []
        for lo, hi, fs in pieces:
            cuts = [16 * k for k in range(math.floor(lo[axis] / 16) + 1,
                                          math.ceil(hi[axis] / 16))]
            edges = [lo[axis]] + cuts + [hi[axis]]
            for i in range(len(edges) - 1):
                a, b = list(lo), list(hi)
                a[axis], b[axis] = edges[i], edges[i + 1]
                f = set(fs)
                if i > 0:
                    f.discard(CUT_FACE[axis][0])
                if i < len(edges) - 2:
                    f.discard(CUT_FACE[axis][1])
                out.append((a, b, f))
        pieces = out
    els = []
    for lo, hi, fs in pieces:
        for v in lo + hi:
            assert not bound or -16 <= v <= 32, (frm, to)
        # The cell each piece lies in, so its UVs come out inside 0..16.
        shift = [math.floor(((lo[i] + hi[i]) / 2) / 16) * 16 for i in range(3)]
        uv = _uvs([lo[i] - shift[i] for i in range(3)], [hi[i] - shift[i] for i in range(3)])
        fdict = {}
        for fname in ALL:
            if fname not in fs:
                continue
            u = [round(min(16, max(0, v)), 3) for v in uv[fname]]
            if u[0] == u[2]:
                u[2] = u[0] + 0.01
            if u[1] == u[3]:
                u[3] = u[1] + 0.01
            fdict[fname] = face((per or {}).get(fname, tex), u)
        if not fdict:
            continue
        e = {"from": [round(v, 3) for v in lo], "to": [round(v, 3) for v in hi], "faces": fdict}
        if rot:
            e["rotation"] = dict(rot)
        els.append(e)
    return els


def turned(axis, origin, angle):
    return {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}


def sbox(*args, **kwargs):
    """cbox for the OBJ-built swing sets, which stand taller than a JSON element may reach."""
    return cbox(*args, bound=False, **kwargs)


# Corners of each face (min 0 / max 1 of the element on each axis) in the order Minecraft pairs
# with the face's uv rect's (u0,v0), (u1,v0), (u1,v1), (u0,v1), and the outward normal.
FACE_DEF = {
    "north": ([(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)], (0, 0, -1)),
    "south": ([(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)], (0, 0, 1)),
    "west": ([(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)], (-1, 0, 0)),
    "east": ([(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)], (1, 0, 0)),
    "up": ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], (0, 1, 0)),
    "down": ([(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)], (0, -1, 0)),
}


def _rotate(p, axis, angle, origin):
    c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    x, y, z = (p[i] - origin[i] for i in range(3))
    if axis == "x":
        x, y, z = x, c * y - s * z, s * y + c * z
    elif axis == "y":
        x, y, z = c * x + s * z, y, -s * x + c * z
    else:
        x, y, z = c * x - s * y, s * x + c * y, z
    return (x + origin[0], y + origin[1], z + origin[2])


def obj_from_elements(elements, name, mtl):
    """JSON-style elements written as an OBJ, one material per texture key, each face keeping
    the uv rect cbox fitted it (inset a hair from the sprite's edge; flip-v is on)."""
    v, vt, vn, vi, ti, ni = [], [], [], {}, {}, {}
    groups = {}

    def idx(store, index, key):
        key = tuple(round(c, 6) + 0.0 for c in key)
        if key not in index:
            store.append(key)
            index[key] = len(store)
        return index[key]

    faces = []
    for e in elements:
        if "quad" in e:            # a free quad (the tyre's octagonal ring)
            faces.append((e["texture"], e["quad"], e["uvs"], e["normal"]))
            continue
        lo, hi = e["from"], e["to"]
        rot = e.get("rotation")
        for fname, f in e["faces"].items():
            picks, normal = FACE_DEF[fname]
            pts = [tuple(hi[i] if pk[i] else lo[i] for i in range(3)) for pk in picks]
            if rot:
                pts = [_rotate(p, rot["axis"], rot["angle"], rot["origin"]) for p in pts]
                normal = _rotate(normal, rot["axis"], rot["angle"], (0, 0, 0))
            u0, v0, u1, v1 = f["uv"]
            faces.append((f["texture"], pts, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], normal))
    for texture, pts, uvs, normal in faces:
        a, b, c = pts[0], pts[1], pts[2]
        e1 = [b[i] - a[i] for i in range(3)]
        e2 = [c[i] - a[i] for i in range(3)]
        cr = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2],
              e1[0] * e2[1] - e1[1] * e2[0])
        if sum(cr[i] * normal[i] for i in range(3)) < 0:
            pts, uvs = pts[::-1], uvs[::-1]
        n = idx(vn, ni, normal)
        face_idx = []
        for p, (u, w) in zip(pts, uvs):
            t = (0.01 + 0.98 * u / 16.0, 1.0 - (0.01 + 0.98 * w / 16.0))
            face_idx.append((idx(v, vi, tuple(q / 16.0 for q in p)), idx(vt, ti, t), n))
        groups.setdefault(texture.lstrip("#"), []).append(face_idx)
    lines = ["# Procedurally generated by dev-env-utils/scripts/gen_park_legacy_amenities.py"
             " -- do not hand edit", "mtllib " + mtl, "o " + name]
    lines += ["v %.6f %.6f %.6f" % p for p in v]
    lines += ["vt %.6f %.6f" % t for t in vt]
    lines += ["vn %.6f %.6f %.6f" % n for n in vn]
    for mat in sorted(groups):
        lines.append("usemtl " + mat)
        lines += ["f " + " ".join("%d/%d/%d" % i for i in f) for f in groups[mat]]
    return "\n".join(lines) + "\n"


def obj_state(obj, textures, scale, lift):
    """An OBJ block's forge blockstate: the six-way facing, the textures keyed by material, and
    item transforms (Forge's, in block units) that shrink it into the slot."""
    s = [scale] * 3
    down = [0, -lift, 0]
    transform = {
        "gui": {"rotation": [30, 225, 0], "translation": down, "scale": s},
        "ground": {"rotation": [0, 0, 0], "translation": down, "scale": s},
        "fixed": {"rotation": [0, 0, 0], "translation": down, "scale": s},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0.15, 0],
                                  "scale": s},
        "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 0.15, 0],
                                 "scale": s},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": s},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": s},
    }
    tex = {"#" + k: t for k, t in textures.items() if k != "particle"}
    tex["particle"] = textures["particle"]
    return {"forge_marker": 1,
            "defaults": {"model": MODEL + obj, "custom": {"flip-v": True}, "textures": tex},
            "variants": {"facing": FACING, "inventory": [{"transform": transform}],
                         "normal": [{}]}}


def display(scale, lift=0.0):
    """Item transforms for a model bigger than its cell: the vanilla block ones, shrunk to fit,
    and lowered by `lift` px where the model's middle is above the cell's."""
    s = [scale] * 3
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -lift, 0], "scale": s},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3 - lift, 0], "scale": s},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -lift, 0], "scale": s},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": s},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": s},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": s},
    }


def model(textures, elements, disp=None):
    m = {"parent": "block/block", "textures": dict(textures)}
    if disp:
        m["display"] = disp
    m["elements"] = elements
    return m


FACING = {"down": {"x": 90}, "east": {"y": 90}, "north": {}, "south": {"y": 180},
          "up": {"x": 270}, "west": {"y": 270}}


def json_state(model_name):
    """The six-way facing the blocks were placed with, pointed at the new model."""
    return {"forge_marker": 1, "defaults": {"model": MODEL + model_name},
            "variants": {"facing": FACING, "inventory": [{}], "normal": [{}]}}


# ------------------------------------------------------------------------------------------
# Swing sets
# ------------------------------------------------------------------------------------------
SPLAY = 22.5                          # an A-frame leg's lean from vertical
BEAM_Y0, BEAM_Y1 = 42.3, 44.3         # top beam: 2.77 m to its top
APEX = 42.8                           # where each A-frame's legs meet, under the beam's middle
FRAME_X = ((-14.0, -12.5), (28.5, 30.0))   # the two A-frames, 2.75 m apart outside
LEG = 1.5
SWING_MTL = "swing_sets.mtl"


def a_frames(frame):
    """Beam, and at each end two legs splayed north and south with a spreader across them."""
    # The beam's ends are inside the brackets, so they are not drawn.
    els = sbox([-14, BEAM_Y0, 7], [30, BEAM_Y1, 9], frame,
               faces=("north", "south", "up", "down"))
    length = APEX / math.cos(math.radians(SPLAY))
    reach = (APEX - 17) * math.tan(math.radians(SPLAY))
    # The legs stop inside the bracket, before they would cross each other (two faces in one
    # plane, which z-fight).
    stop = 2.5
    cap_z = (stop + 1) * math.tan(math.radians(SPLAY)) + LEG / 2 / math.cos(math.radians(SPLAY))
    for x0, x1 in FRAME_X:
        for angle in (SPLAY, -SPLAY):
            els += sbox([x0, APEX - length, 8 - LEG / 2], [x1, APEX - stop, 8 + LEG / 2], frame,
                        faces=("north", "south", "east", "west", "down"),
                        rot=turned("x", [(x0 + x1) / 2, APEX, 8], angle))
        # Spreader bar a metre up, between the legs.
        els += sbox([x0 + 0.25, 16.5, 8 - reach + 0.6], [x1 - 0.25, 17.5, 8 + reach - 0.6], frame)
        # The cast bracket that takes the beam's end and both legs.
        els += sbox([x0 - 0.25, APEX - stop - 1, 8 - cap_z - 0.2],
                    [x1 + 0.25, BEAM_Y1 + 0.25, 8 + cap_z + 0.2], "cap")
    return els


def belt_seat(cx):
    """A two-chain belt seat: a sagging rubber strap, its end clamps, the chains to the beam."""
    y = 7.0
    els = sbox([cx - 2.5, y, 6.5], [cx + 2.5, y + 0.75, 9.5], "belt")
    for side in (-1, 1):
        # The strap's ends rising to the clamps.
        x_in = cx + side * 2.5
        lo, hi = (x_in, x_in + 2.4) if side > 0 else (x_in - 2.4, x_in)
        els += sbox([lo, y, 6.6], [hi, y + 0.75, 9.4], "belt",
                    rot=turned("z", [x_in, y, 8], 22.5 * side))
        xc = cx + side * 4.5
        els += sbox([xc - 0.5, y + 0.6, 7.25], [xc + 0.5, y + 2.2, 8.75], "clamp")
        els += sbox([xc - 0.2, y + 2.2, 7.8], [xc + 0.2, BEAM_Y0 - 0.8, 8.2], "chain",
                    faces=("north", "south", "east", "west"))
        # The hanger clamped to the beam.
        els += sbox([xc - 0.6, BEAM_Y0 - 0.8, 7.2], [xc + 0.6, BEAM_Y0, 8.8], "clamp")
    return els


swing_a_tex = {"frame": T("red_steel"), "cap": T("yellow_plastic"), "belt": T("swing_belt"),
               "clamp": T("iron"), "chain": T("swing_chain"), "particle": T("red_steel")}
SWING_A = a_frames("frame") + belt_seat(-1.5) + belt_seat(17.5)


def tyre_swing():
    """A tyre hung flat from a swivel under the beam's middle by four chains."""
    els = []
    ty0, ty1 = 6.0, 9.5               # tyre: 0.22 m wide, its top 0.6 m up
    r_out, r_in = 6.5, 3.5            # 0.8 m across
    r_mid = (r_out + r_in) / 2
    # An octagonal ring (flats facing the compass points): tread outside, bead inside, the
    # sidewalls top and bottom, one quad each per side.
    def corner(r, k):
        a = math.radians(22.5 + 45 * k)
        rr = r / math.cos(math.radians(22.5))
        return (8 + rr * math.cos(a), 8 + rr * math.sin(a))
    for k in range(8):
        (ox0, oz0), (ox1, oz1) = corner(r_out, k), corner(r_out, k + 1)
        (ix0, iz0), (ix1, iz1) = corner(r_in, k), corner(r_in, k + 1)
        mid = math.radians(45 * k + 45)
        out_n = (math.cos(mid), 0.0, math.sin(mid))
        w_out = math.hypot(ox1 - ox0, oz1 - oz0)
        w_in = math.hypot(ix1 - ix0, iz1 - iz0)
        for pts, uvs, n in (
                ([(ox0, ty0, oz0), (ox1, ty0, oz1), (ox1, ty1, oz1), (ox0, ty1, oz0)],
                 [(0, 16 - ty0), (w_out, 16 - ty0), (w_out, 16 - ty1), (0, 16 - ty1)], out_n),
                ([(ix0, ty0, iz0), (ix1, ty0, iz1), (ix1, ty1, iz1), (ix0, ty1, iz0)],
                 [(0, 16 - ty0), (w_in, 16 - ty0), (w_in, 16 - ty1), (0, 16 - ty1)],
                 tuple(-c for c in out_n)),
                ([(ox0, ty1, oz0), (ox1, ty1, oz1), (ix1, ty1, iz1), (ix0, ty1, iz0)],
                 [(ox0, oz0), (ox1, oz1), (ix1, iz1), (ix0, iz0)], (0.0, 1.0, 0.0)),
                ([(ox0, ty0, oz0), (ox1, ty0, oz1), (ix1, ty0, iz1), (ix0, ty0, iz0)],
                 [(ox0, oz0), (ox1, oz1), (ix1, iz1), (ix0, iz0)], (0.0, -1.0, 0.0))):
            els.append({"quad": [tuple(round(c, 4) for c in p) for p in pts], "texture": "#tyre",
                        "uvs": [(round(u, 4), round(w, 4)) for u, w in uvs], "normal": n})
    # Four chains, each leaning 14 degrees from the hub out to the tyre's top.
    lean = 14.0
    drop = r_mid / math.tan(math.radians(lean))
    hub = ty1 + drop
    length = drop / math.cos(math.radians(lean))
    for axis, angle in (("x", lean), ("x", -lean), ("z", lean), ("z", -lean)):
        els += sbox([7.8, hub - length, 7.8], [8.2, hub - 1.8, 8.2], "chain",
                    faces=("north", "south", "east", "west"),
                    rot=turned(axis, [8, hub, 8], angle))
    # The hub ring, the swivel's chain, and the swivel bearing bolted under the beam.
    els += sbox([6.9, hub - 2, 6.9], [9.1, hub + 0.75, 9.1], "clamp")
    els += sbox([7.8, hub + 0.75, 7.8], [8.2, BEAM_Y0 - 2, 8.2], "chain",
                faces=("north", "south", "east", "west"))
    els += sbox([7.1, BEAM_Y0 - 2, 7.1], [8.9, BEAM_Y0, 8.9], "clamp")
    return els


swing_b_tex = {"frame": T("platform_blue"), "cap": T("yellow_plastic"), "tyre": T("swing_tyre"),
               "clamp": T("iron"), "chain": T("swing_chain"), "particle": T("platform_blue")}
SWING_B = a_frames("frame") + tyre_swing()


# ------------------------------------------------------------------------------------------
# Seesaw: a 2.9 m board on a pivot 0.6 m up, resting with its west end down
# ------------------------------------------------------------------------------------------
def seesaw():
    els = []
    px_, py_ = 8.0, 10.0               # the pivot: the board's underside at the axle
    tilt = turned("z", [px_, py_, 8], -22.5)
    # Board, seat pads and the handles in front of them, all turned with the board.
    els += cbox([-15, py_, 5.5], [31, py_ + 1.5, 10.5], "board", rot=tilt)
    for x0, x1, hx in ((-14.5, -8.5, -6.0), (24.5, 30.5, 22.0)):
        els += cbox([x0, py_ + 1.5, 6], [x1, py_ + 2.25, 10], "pad", rot=tilt)
        els += cbox([hx - 0.5, py_ + 1.5, 7.5], [hx + 0.5, py_ + 6, 8.5], "handle", rot=tilt)
        els += cbox([hx - 0.5, py_ + 5, 5.5], [hx + 0.5, py_ + 6, 10.5], "handle", rot=tilt)
    # The bearing block under the board, round the axle.
    els += cbox([6.5, py_ - 1.25, 5.5], [9.5, py_, 10.5], "frame", rot=tilt)
    # Axle through both stands.
    els += cbox([7.25, py_ - 1.5, 3], [8.75, py_, 13], "axle")
    # Two A-stands, one each side of the board, legs splayed east and west, on a ground bar.
    apex = py_ - 0.75
    length = apex / math.cos(math.radians(22.5)) + 0.4
    reach = apex * math.tan(math.radians(22.5))
    for z0, z1 in ((3.5, 5.0), (11.0, 12.5)):
        for angle in (22.5, -22.5):
            els += cbox([7.25, apex - length, z0], [8.75, apex + 0.5, z1], "frame",
                        rot=turned("z", [8, apex, (z0 + z1) / 2], angle))
        els += cbox([8 - reach - 1, 0, z0], [8 + reach + 1, 0.75, z1], "frame")
    # Rubber bumpers set in the ground under each end.
    for x0 in (-13.5, 25.5):
        els += cbox([x0, 0, 5.5], [x0 + 4, 1.0, 10.5], "pad")
    return els


seesaw_tex = {"board": T("teak"), "pad": T("swing_belt"), "handle": T("yellow_plastic"),
              "frame": T("red_steel"), "axle": T("iron"), "particle": T("teak")}
SEESAW = model(seesaw_tex, seesaw(), display(0.2))


# ------------------------------------------------------------------------------------------
# Litter receptacle: upright hardwood slats in a black steel frame, open top with a steel rim
# ------------------------------------------------------------------------------------------
def litter():
    lo, hi, top = 2.75, 13.25, 14.0            # 0.66 m square, 0.9 m to the rim
    els = cbox([lo + 0.75, 0.75, lo + 0.75], [hi - 0.75, 13.25, hi - 0.75], "liner",
               per={"up": "liner_top"})
    # Corner posts, standing a little proud as feet.
    for x in (lo - 0.25, hi - 1.25):
        for z in (lo - 0.25, hi - 1.25):
            els += cbox([x, 0, z], [x + 1.5, top, z + 1.5], "steel")
    # Four slats a side between the posts.
    span0, span1 = lo + 1.25, hi - 1.25
    width = (span1 - span0 - 3 * 0.35) / 4
    for i in range(4):
        a = span0 + i * (width + 0.35)
        b = a + width
        els += cbox([a, 0.75, lo], [b, top - 0.5, lo + 0.75], "slat")      # north
        els += cbox([a, 0.75, hi - 0.75], [b, top - 0.5, hi], "slat")      # south
        els += cbox([lo, 0.75, a], [lo + 0.75, top - 0.5, b], "slat")      # west
        els += cbox([hi - 0.75, 0.75, a], [hi, top - 0.5, b], "slat")      # east
    # Two steel straps round the slats.
    for y0 in (2.5, 10.5):
        y1 = y0 + 1
        els += cbox([lo - 0.25, y0, lo - 0.25], [hi + 0.25, y1, lo], "steel")
        els += cbox([lo - 0.25, y0, hi], [hi + 0.25, y1, hi + 0.25], "steel")
        els += cbox([lo - 0.25, y0, lo], [lo, y1, hi], "steel")
        els += cbox([hi, y0, lo], [hi + 0.25, y1, hi], "steel")
    # The rim: a steel frame round the opening.
    r = 1.25
    els += cbox([lo - 0.25, top - 0.5, lo - 0.25], [hi + 0.25, top + 0.5, lo - 0.25 + r], "steel")
    els += cbox([lo - 0.25, top - 0.5, hi + 0.25 - r], [hi + 0.25, top + 0.5, hi + 0.25], "steel")
    els += cbox([lo - 0.25, top - 0.5, lo - 0.25 + r], [lo - 0.25 + r, top + 0.5, hi + 0.25 - r],
                "steel")
    els += cbox([hi + 0.25 - r, top - 0.5, lo - 0.25 + r], [hi + 0.25, top + 0.5, hi + 0.25 - r],
                "steel")
    return els


litter_tex = {"slat": T("litter_slat"), "steel": T("iron"), "liner": T("litter_liner"),
              "liner_top": T("litter_liner"), "particle": T("litter_slat")}
LITTER = model(litter_tex, litter())


# ------------------------------------------------------------------------------------------
# Bird bath: a lathed cast-stone bowl on a baluster pedestal, water in the bowl (OBJ)
# ------------------------------------------------------------------------------------------
BIRDBATH_OBJ = "birdbath_cast.obj"
BIRDBATH_MTL = "birdbath_cast.mtl"
BATH_SIDES = 8
WATER_Y = 11.3


def birdbath_obj():
    """0.73 m tall with a 0.7 m bowl, octagonal like a cast-stone one. One profile: the plinth,
    the pedestal with a single swell, out under the bowl, over the rim and a little way down
    inside; the water is a disc just under the rim, hiding the bowl's open middle."""
    stone = pole.Mesh()
    water = pole.Mesh()
    profile = [(3.6, 0.0), (3.6, 1.2), (1.7, 1.2),                   # plinth
               (2.1, 5.0), (1.4, 8.6),                                 # pedestal, one swell
               (5.6, 10.6), (5.6, 11.7),                               # under the bowl, rim
               (4.7, 11.7), (4.7, 11.0)]                               # inside, below the water
    pole.lathe(stone, profile, sides=BATH_SIDES, bottom_disc=True)
    pole.disc(water, 4.75, WATER_Y, (0.0, 1.0, 0.0), sides=BATH_SIDES)
    lines = ["# Procedurally generated by dev-env-utils/scripts/gen_park_legacy_amenities.py"
             " -- do not hand edit", "mtllib " + BIRDBATH_MTL, "o birdbath"]
    faces = []
    nv = nt = nn = 0
    for material, mesh in (("stone", stone), ("water", water)):
        lines += ["v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0) for p in mesh.v]
        lines += ["vt %.6f %.6f" % t for t in mesh.vt]
        lines += ["vn %.6f %.6f %.6f" % n for n in mesh.vn]
        faces.append("usemtl " + material)
        for tri in mesh.f:
            faces.append("f " + " ".join("%d/%d/%d" % (i[0] + nv, i[1] + nt, i[2] + nn)
                                         for i in tri))
        nv, nt, nn = nv + len(mesh.v), nt + len(mesh.vt), nn + len(mesh.vn)
    return "\n".join(lines + faces) + "\n"


BIRDBATH_STATE = {
    "forge_marker": 1,
    "defaults": {
        "model": MODEL + BIRDBATH_OBJ,
        "custom": {"flip-v": True},
        "textures": {"#stone": T("birdbath_stone"), "#water": T("water"),
                     "particle": T("birdbath_stone")},
    },
    "variants": {"facing": FACING, "inventory": [{"transform": "forge:default-block"}],
                 "normal": [{}]},
}


# ------------------------------------------------------------------------------------------
# Catalogue and output
# ------------------------------------------------------------------------------------------
MODELS = {
    "seesaw": SEESAW,
    "litter_receptacle_wood": LITTER,
}
STATES = {
    "parkswinga": obj_state("swing_belt_set.obj", swing_a_tex, 0.2, 0.15),
    "parkswingb": obj_state("swing_tyre_set.obj", swing_b_tex, 0.2, 0.15),
    "teetertotter": json_state("seesaw"),
    "parktrashcan": json_state("litter_receptacle_wood"),
    "birdbath": BIRDBATH_STATE,
}
TEXT_FILES = {
    "models/block/parks/amenities/swing_belt_set.obj":
        lambda: obj_from_elements(SWING_A, "swing_belt_set", SWING_MTL),
    "models/block/parks/amenities/swing_tyre_set.obj":
        lambda: obj_from_elements(SWING_B, "swing_tyre_set", SWING_MTL),
    "models/block/parks/amenities/" + SWING_MTL: lambda: (
        "# Generated by gen_park_legacy_amenities.py -- do not hand edit; the blockstates "
        "retexture these\n" + "".join("newmtl %s\nmap_Kd %s\n" % (m, t) for m, t in (
            ("belt", T("swing_belt")), ("cap", T("yellow_plastic")), ("chain", T("swing_chain")),
            ("clamp", T("iron")), ("frame", T("red_steel")), ("tyre", T("swing_tyre"))))),
    "models/block/parks/amenities/" + BIRDBATH_OBJ: birdbath_obj,
    "models/block/parks/amenities/" + BIRDBATH_MTL: lambda: (
        "# Generated by gen_park_legacy_amenities.py -- do not hand edit; the blockstate "
        "retextures these\nnewmtl stone\nmap_Kd %s\nnewmtl water\nmap_Kd %s\n"
        % (T("birdbath_stone"), T("water"))),
}


def generate(assets):
    written = []
    for name, draw in TEXTURES.items():
        rel = "textures/blocks/parks/amenities/%s.png" % name
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for name, data in MODELS.items():
        rel = "models/block/parks/amenities/%s.json" % name
        pp.dump(os.path.join(assets, rel), data)
        written.append(rel)
    for reg, data in STATES.items():
        rel = "blockstates/%s.json" % reg
        pp.dump(os.path.join(assets, rel), data)
        written.append(rel)
    for rel, text in TEXT_FILES.items():
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", newline="\n", encoding="utf-8") as fh:
            fh.write(text())
        written.append(rel)
    model_depth.separate(assets, written, pp.dump)
    return written


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="legacy_amenities_")
    try:
        written = generate(tmp)
        stale = [rel for rel in written
                 if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("legacy park amenities are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
