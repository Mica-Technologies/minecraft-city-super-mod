#!/usr/bin/env python3
"""Every asset the site and chain-link fences ship.

    python dev-env-utils/scripts/gen_fencing.py
    python dev-env-utils/scripts/gen_fencing.py --check
    python dev-env-utils/scripts/gen_fencing.py --fragments   # lang lines to paste

Six blocks, all one Java class (BlockSiteFence) that joins its neighbours the way a vanilla fence
does -- a post at the centre and a panel out to each side it connects on:

- the temporary site fence, mesh panels in tube frames standing in precast concrete feet, and the
  same with green privacy screen on the mesh;
- the silt fence, black geotextile on wooden stakes;
- permanent chain-link in galvanized and black vinyl-coated, which stacks: a tall fence is several
  one-block courses sharing one post and one mesh, with the top rail and post cap only on the top
  course and the tension wire only along the bottom;
- the barbed-wire top that sits on a chain-link fence: V arms and three strands a side.

The temporary fence and the chain-link share one mesh texture, drawn as a zero-thickness cutout
plane through the post; a mesh is a pattern of holes, and a plane is the only way to draw it that
does not double every wire. A panel is drawn once, running east from the post, and the blockstate
turns it for the other three sides.

The temporary fence stands 1.75 blocks, a real panel's six feet, so its upper part is drawn in the
cell above with gen_scaffold's fitted UVs (a span there is shifted a whole block, never clamped).
"""

import argparse
import filecmp
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

import gen_cmu
import gen_scaffold as sc

REPO = sc.REPO
TEX_DIR = sc.TEX_DIR
MODEL_DIR = sc.MODEL_DIR
STATE_DIR = sc.STATE_DIR
TEX_REF = sc.TEX_REF
MODEL_REF = sc.MODEL_REF

# Registry name -> (tab, name in each language). Order is creative order within each tab.
BLOCKS = {
    "temp_fence": ("site", ("Temporary Fence", "Valla Temporal", "Bauzaun", "Byggstängsel")),
    "temp_fence_screened": ("site", ("Temporary Fence (Privacy Screen)",
                                     "Valla Temporal (con Malla de Ocultación)",
                                     "Bauzaun (mit Sichtschutz)",
                                     "Byggstängsel (med Insynsskydd)")),
    "silt_fence": ("site", ("Silt Fence", "Barrera de Sedimentos", "Schlammschutzzaun",
                            "Sedimentstängsel")),
    "chainlink_fence": ("materials", ("Chain-Link Fence", "Cerca de Malla Ciclónica",
                                      "Maschendrahtzaun", "Nätstängsel")),
    "chainlink_fence_black": ("materials", ("Chain-Link Fence (Black)",
                                            "Cerca de Malla Ciclónica (Negra)",
                                            "Maschendrahtzaun (Schwarz)", "Nätstängsel (Svart)")),
    "chainlink_barbed_top": ("materials", ("Chain-Link Barbed Wire Top",
                                           "Remate de Alambre de Púas",
                                           "Stacheldrahtaufsatz", "Taggtrådskrön")),
}

# The chain-link finishes: (post and rail colour, mesh wire colour).
FINISHES = {"galv": ((168, 174, 176), (178, 184, 186)),
            "black": ((50, 54, 50), (44, 48, 44))}
FINISH_OF = {"chainlink_fence": "galv", "chainlink_fence_black": "black"}

MESH_SIZE = 32      # mesh texture resolution: four diamonds across a block
MESH_PITCH = 8      # texels between parallel wires


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def mesh_texture(colour, seed):
    """Chain-link: two sets of diagonal wires crossing into diamonds, the rest open. The pitch
    divides the texture, so mesh on neighbouring blocks, and on the course above, is one mesh."""
    rng = random.Random(seed)
    n = MESH_SIZE
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            a = (x + y) % MESH_PITCH == 0
            b = (x - y) % MESH_PITCH == 0
            if a or b:
                # The knuckles, where the wires cross, catch a touch more light.
                px[x, y] = _shift(colour, (14 if a and b else 0) + rng.uniform(-8, 8))
    return img


def pipe_texture(colour, seed):
    """A round steel tube: lit down its middle and falling off to either side."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 7.5
            px[x, y] = _shift(colour, 18 - 40 * d * d + rng.uniform(-3, 3))
    return img


def foot_texture():
    """A precast temporary-fence foot: grey concrete, a little rough."""
    rng = random.Random(20261201)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((150, 149, 142), rng.uniform(-9, 9))
    return img


def screen_texture():
    """Privacy screen: dark green knitted shade cloth, a fine weave of lighter and darker rows."""
    rng = random.Random(20261202)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            weave = 7 if (x + (y // 2)) % 2 == 0 else -5
            px[x, y] = _shift((38, 92, 56), weave + rng.uniform(-4, 4))
    return img


def silt_texture():
    """Silt fence geotextile: black woven fabric."""
    rng = random.Random(20261203)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((30, 30, 32), (6 if (x + y) % 2 == 0 else -2) + rng.uniform(-3, 3))
    return img


BARB_ROWS = (6, 10)   # the texture rows a strand samples: the wire runs along row 8


def barb_texture():
    """Barbed wire, drawn along row 8: two wires twisted round each other, and a four-point barb
    every four texels. Everything else is open."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    wire = (150, 156, 158)
    for x in range(16):
        px[x, 8] = _shift(wire, 10 if x % 2 == 0 else -10)
        px[x, 7 + (x // 2) % 2] = _shift(wire, -18)
    for x in (1, 5, 9, 13):
        for dx, dy in ((-1, -2), (0, -1), (1, 1), (2, 2), (1, -2), (0, 1), (-1, 2)):
            xx, yy = x + dx, 8 + dy
            if 0 <= xx < 16:
                px[xx, yy] = _shift(wire, 20)
    return img


def textures():
    out = {"fence_foot": foot_texture(), "fence_screen": screen_texture(),
           "silt_fabric": silt_texture(), "barbed_wire": barb_texture()}
    for i, (finish, (post, wire)) in enumerate(sorted(FINISHES.items())):
        out["chainlink_mesh_" + finish] = mesh_texture(wire, 20261210 + i)
        out["chainlink_post_" + finish] = pipe_texture(post, 20261220 + i)
    return out


# --------------------------------------------------------------------------------------------
# Geometry -- a panel is drawn running east from the post; the blockstate turns it
# --------------------------------------------------------------------------------------------

box = sc._box
PLANE = ("north", "south")          # a zero-thickness plane in x-y shows these two faces


def plane(x0, y0, x1, y1, z, tex, uv=None):
    return box(x0, y0, z, x1, y1, z, tex, uv=uv, faces=PLANE)


# Chain-link.
LINE_POST = (7.25, 8.75)
TERMINAL_POST = (7.0, 9.0)
RAIL = (14.8, 16.0)       # the top rail, only on the top course
MESH_TOP = 15.0           # the mesh stops under the top rail


def cl_post(terminal, top):
    a, b = TERMINAL_POST if terminal else LINE_POST
    if not top:
        return [box(a, 0, a, b, 16, b, "#post")]
    if terminal:
        # A terminal post wears a dome cap.
        return [box(a, 0, a, b, 15.5, b, "#post"),
                box(a - 0.25, 15.5, a - 0.25, b + 0.25, 16.75, b + 0.25, "#post")]
    # A line post wears a loop cap, which the top rail passes through.
    return [box(a, 0, a, b, 15.5, b, "#post"),
            box(a - 0.25, 15.5, a - 0.25, b + 0.25, 16.5, b + 0.25, "#post")]


def cl_mesh(top):
    if not top:
        return [plane(8, 0, 16, 16, 8, "#mesh")]
    return [plane(8, 0, 16, MESH_TOP, 8, "#mesh"),
            box(8, RAIL[0], 7.4, 16, RAIL[1], 8.6, "#post")]


def cl_wire():
    """The tension wire along the bottom of the mesh."""
    return [box(8, 0.35, 7.85, 16, 0.65, 8.15, "#post")]


# Temporary fence: panels 1.75 blocks tall in 1.2 px tube frames.
TUBE = 1.2
FRAME_END = 8.6           # the panel's end tube, just east of the post line
PANEL_BOTTOM = 3.0        # the feet are 3 px tall; the frame stands in them
PANEL_TOP = 28.0


def temp_foot():
    """The precast foot, long along the fence line, with the two panels' tubes standing in it."""
    return [box(1.5, 0, 5, 14.5, PANEL_BOTTOM, 11, "#foot")]


def temp_half(screen):
    """The half panel east of the post: its end tube, top and bottom tubes, the mesh between, and
    half of each clamp that couples it to the next panel's end tube."""
    e0, e1 = FRAME_END, FRAME_END + TUBE
    lo, hi = 8 - TUBE / 2, 8 + TUBE / 2
    els = [
        box(e0, PANEL_BOTTOM, lo, e1, PANEL_TOP, hi, "#frame"),
        box(e1, PANEL_TOP - TUBE, lo, 16, PANEL_TOP, hi, "#frame"),
        box(e1, PANEL_BOTTOM, lo, 16, PANEL_BOTTOM + TUBE, hi, "#frame"),
        plane(e1, PANEL_BOTTOM + TUBE, 16, PANEL_TOP - TUBE, 8, "#mesh"),
    ]
    for y in (9.0, 21.0):
        els.append(box(8, y, 7.1, e1 + 0.2, y + 1.5, 8.9, "#frame"))
    if screen:
        els.append(plane(e1, PANEL_BOTTOM + TUBE, 16, PANEL_TOP - TUBE, 8.45, "#screen"))
    return els


# Silt fence.
def silt_stake():
    return [box(7.25, 0, 7.25, 8.75, 14, 8.75, "#stake")]


def silt_half():
    return [plane(8.75, 0, 16, 11, 8, "#fabric")]


# Barbed-wire top: V arms leaning 45 degrees either side of the fence line, three strands on each.
ARM_BASE = 2.5
ARM_LENGTH = 12.0
STRANDS = (4.0, 8.0, 12.0)    # distance along each arm


def barbed_sleeve():
    """The sleeve that fits over the post top, covering its cap."""
    return [box(6.6, 0, 6.6, 9.4, ARM_BASE + 0.5, 9.4, "#post")]


def barbed_arms():
    """The two arms of the V, for a run along x: they lean toward north and south."""
    els = []
    for angle in (45, -45):
        els.append(box(7.5, ARM_BASE, 7.5, 8.5, ARM_BASE + ARM_LENGTH, 8.5, "#post",
                       rotation={"origin": [8, ARM_BASE, 8], "axis": "x", "angle": angle}))
    return els


def barbed_strands():
    """Three strands each side of the fence line, east of the post, where the arms reach."""
    els = []
    k = math.sqrt(0.5)
    for d in STRANDS:
        y = round(ARM_BASE + d * k, 3)
        for side in (-1, 1):
            z = round(8 + side * d * k, 3)
            els.append(plane(8, y - 1.0, 16, y + 1.0, z, "#barb",
                             uv=[8, BARB_ROWS[0], 16, BARB_ROWS[1]]))
    return els


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------

def _model(elements, textures, particle, parent=None):
    t = dict(textures)
    t["particle"] = textures[particle]
    m = {"textures": t, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def _cl_textures(finish):
    return {"post": TEX_REF % ("chainlink_post_" + finish),
            "mesh": TEX_REF % ("chainlink_mesh_" + finish)}


TEMP_TEXTURES = {"frame": TEX_REF % "chainlink_post_galv", "mesh": TEX_REF % "chainlink_mesh_galv",
                 "foot": TEX_REF % "fence_foot", "screen": TEX_REF % "fence_screen"}
SILT_TEXTURES = {"stake": TEX_REF % "form_lumber", "fabric": TEX_REF % "silt_fabric"}
BARB_TEXTURES = {"post": TEX_REF % "chainlink_post_galv", "barb": TEX_REF % "barbed_wire"}


def _mirror_x(elements):
    """The half panel west of the post, for an inventory model: every element reflected in x."""
    out = []
    for el in elements:
        f, t = el["from"], el["to"]
        nf = [16 - t[0], f[1], f[2]]
        nt = [16 - f[0], t[1], t[2]]
        faces = tuple(el["faces"].keys())
        uv = None
        first = next(iter(el["faces"].values()))
        if el["faces"] and all(v.get("uv") == first.get("uv") for v in el["faces"].values()) \
                and "#barb" == first["texture"]:
            uv = first["uv"]
        out.append(box(nf[0], nf[1], nf[2], nt[0], nt[1], nt[2], first["texture"], uv=uv,
                       faces=faces))
    return out


# The temporary fence is 1.75 blocks tall, which block/block's inventory view pushes out of the
# top of the slot: shrink it to fit, and bring its middle (y 14, not 8) back to the slot's.
TEMP_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0],
                        "scale": [0.42, 0.42, 0.42]}}


def models():
    out = {}
    for finish in FINISHES:
        tex = _cl_textures(finish)
        p = "chainlink_%s_" % finish
        out[p + "post_line"] = _model(cl_post(False, False), tex, "post")
        out[p + "post_line_top"] = _model(cl_post(False, True), tex, "post")
        out[p + "post_terminal"] = _model(cl_post(True, False), tex, "post")
        out[p + "post_terminal_top"] = _model(cl_post(True, True), tex, "post")
        out[p + "mesh"] = _model(cl_mesh(False), tex, "post")
        out[p + "mesh_top"] = _model(cl_mesh(True), tex, "post")
        out[p + "wire"] = _model(cl_wire(), tex, "post")
        half = cl_mesh(True) + cl_wire()
        out[p + "inventory"] = _model(cl_post(True, True) + half + _mirror_x(half), tex, "post",
                                      parent="block/block")
    out["temp_fence_foot"] = _model(temp_foot(), TEMP_TEXTURES, "foot")
    out["temp_fence_half"] = _model(temp_half(False), TEMP_TEXTURES, "frame")
    # The screen hangs on one side of the mesh. Turning the east half's screen 180 degrees for the
    # west half would move it to the other side, so each panel would have it in front of the mesh
    # on one half and behind it on the other: the west half gets its own, mirrored only in x.
    screen = [e for e in temp_half(True) if e["faces"]["north"]["texture"] == "#screen"]
    out["temp_fence_screen_east"] = _model(screen, TEMP_TEXTURES, "frame")
    out["temp_fence_screen_west"] = _model(_mirror_x(screen), TEMP_TEXTURES, "frame")
    for screen, name in ((False, "temp_fence_inventory"), (True, "temp_fence_screened_inventory")):
        half = temp_half(screen)
        out[name] = _model(temp_foot() + half + _mirror_x(half), TEMP_TEXTURES, "frame",
                           parent="block/block")
        out[name]["display"] = TEMP_DISPLAY
    out["silt_fence_stake"] = _model(silt_stake(), SILT_TEXTURES, "fabric")
    out["silt_fence_half"] = _model(silt_half(), SILT_TEXTURES, "fabric")
    out["silt_fence_inventory"] = _model(silt_stake() + silt_half() + _mirror_x(silt_half()),
                                         SILT_TEXTURES, "fabric", parent="block/block")
    out["barbed_top_sleeve"] = _model(barbed_sleeve(), BARB_TEXTURES, "post")
    out["barbed_top_arms"] = _model(barbed_arms(), BARB_TEXTURES, "post")
    out["barbed_top_strands"] = _model(barbed_strands(), BARB_TEXTURES, "post")
    out["barbed_top_inventory"] = _model(
        barbed_sleeve() + barbed_arms() + barbed_strands() + _mirror_x(barbed_strands()),
        BARB_TEXTURES, "post", parent="block/block")
    return out


def _ref(model):
    return MODEL_REF % model


# Each side a panel can run to, and the blockstate y rotation that turns an east panel onto it.
SIDES = (("east", 0), ("south", 90), ("west", 180), ("north", 270))
NONE = {"north": "false", "east": "false", "south": "false", "west": "false"}


def _along_x():
    """A part oriented along x: a run with an east or west panel, or a lone post."""
    return {"OR": [{"east": "true"}, {"west": "true"}, dict(NONE)]}


def _along_z_only():
    return {"OR": [{"north": "true", "east": "false", "west": "false"},
                   {"south": "true", "east": "false", "west": "false"}]}


def _sides(model, extra=None):
    parts = []
    for side, rot in SIDES:
        when = {side: "true"}
        if extra:
            when.update(extra)
        apply = {"model": _ref(model)}
        if rot:
            apply["y"] = rot
        parts.append({"when": when, "apply": apply})
    return parts


def blockstates():
    out = {}
    for name, finish in FINISH_OF.items():
        p = "chainlink_%s_" % finish
        parts = [
            {"when": {"terminal": "false", "up": "true"}, "apply": {"model": _ref(p + "post_line")}},
            {"when": {"terminal": "false", "up": "false"},
             "apply": {"model": _ref(p + "post_line_top")}},
            {"when": {"terminal": "true", "up": "true"},
             "apply": {"model": _ref(p + "post_terminal")}},
            {"when": {"terminal": "true", "up": "false"},
             "apply": {"model": _ref(p + "post_terminal_top")}},
        ]
        parts += _sides(p + "mesh", {"up": "true"})
        parts += _sides(p + "mesh_top", {"up": "false"})
        parts += _sides(p + "wire", {"down": "false"})
        out[name] = {"variants": {"inventory": {"model": _ref(p + "inventory")}},
                     "multipart": parts}
    for name, screen in (("temp_fence", False), ("temp_fence_screened", True)):
        parts = [{"when": _along_x(), "apply": {"model": _ref("temp_fence_foot")}},
                 {"when": _along_z_only(), "apply": {"model": _ref("temp_fence_foot"), "y": 90}}]
        parts += _sides("temp_fence_half")
        if screen:
            # East and west keep the screen on the south side; turned a quarter for a run along z,
            # both halves put it on the west.
            for side, model, rot in (("east", "temp_fence_screen_east", 0),
                                     ("west", "temp_fence_screen_west", 0),
                                     ("south", "temp_fence_screen_east", 90),
                                     ("north", "temp_fence_screen_west", 90)):
                apply = {"model": _ref(model)}
                if rot:
                    apply["y"] = rot
                parts.append({"when": {side: "true"}, "apply": apply})
        out[name] = {"variants": {"inventory": {"model": _ref(name + "_inventory")}},
                     "multipart": parts}
    out["silt_fence"] = {"variants": {"inventory": {"model": _ref("silt_fence_inventory")}},
                         "multipart": [{"apply": {"model": _ref("silt_fence_stake")}}]
                         + _sides("silt_fence_half")}
    out["chainlink_barbed_top"] = {
        "variants": {"inventory": {"model": _ref("barbed_top_inventory")}},
        "multipart": [{"apply": {"model": _ref("barbed_top_sleeve")}},
                      {"when": _along_x(), "apply": {"model": _ref("barbed_top_arms")}},
                      {"when": {"OR": [{"north": "true"}, {"south": "true"}]},
                       "apply": {"model": _ref("barbed_top_arms"), "y": 90}}]
        + _sides("barbed_top_strands")}
    return out


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    return [("tile.%s.name" % name, dict(zip(LANGS, names)))
            for name, (_, names) in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang lines and exit")
    args = ap.parse_args()

    if args.fragments:
        print(fragments())
        return 0

    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}

    if not args.check:
        written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d fencing files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_fencing_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(tmp_roots["tex"], tmp_roots["model"], tmp_roots["state"])
        drifted = []
        for kind, filename in written:
            here = os.path.join(roots[kind], filename)
            there = os.path.join(tmp_roots[kind], filename)
            if not os.path.exists(here) or not filecmp.cmp(here, there, shallow=False):
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated fencing files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
