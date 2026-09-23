#!/usr/bin/env python3
"""
gen_park_amenities.py -- the Parks & Greenery park amenities (the Parks tab).

Benches and picnic tables, bins and the dog-waste station, a pergola, playground pieces,
fountains, the irrigation controller and sprinklers, and a rubber safety surface. Like
gen_park_plantings.py (whose model and texture helpers this borrows), one catalogue writes every
texture, JSON model, blockstate and lang line, under modules/parks/.../assets/csm:

  * textures/blocks/parks/amenities/*.png  (+ .mcmeta for the animated water)
  * models/block/parks/amenities/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json for multipart blocks
  * models/block/parks/amenities/gazebo_roof_<n>.obj (+ one .mtl): the gazebo roofs, the one
    shape here a JSON element cannot draw (sloped faces, and a footprint past -16..32)

Every model faces north with its back (the wall, the ladder, a bench's backrest) at +Z, which
is how the rotatable blocks in the mod are drawn. Benches and picnic tables are one block of seat
each, placed side by side into a run; the multipart blockstate draws their end frames only where
the run ends (BlockParkBench's LEFT and RIGHT, the sitter's).

Usage:
    python gen_park_amenities.py              # write everything
    python gen_park_amenities.py --check      # fail if the tree has drifted
    python gen_park_amenities.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_park_plantings as pp  # noqa: E402
import gen_trees  # noqa: E402

ASSETS = pp.ASSETS
LOCALES = pp.LOCALES
TEX = "csm:blocks/parks/amenities/"
MODEL = "csm:parks/amenities/"
box = pp.box
model = pp.model
face = pp.face
noise_tex = pp.noise_tex
clamp = pp.clamp


def T(name):
    return TEX + name


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
TEAK = [(176, 120, 72), (160, 106, 62), (142, 92, 52), (122, 78, 44)]
IRON = [(52, 54, 56), (42, 44, 46), (34, 36, 38)]
GREEN_STEEL = [(46, 96, 64), (38, 84, 56), (30, 72, 48)]
BLUE_PLASTIC = [(46, 96, 172), (40, 86, 156), (34, 76, 140)]
GREEN_PLASTIC = [(58, 110, 60), (50, 98, 52), (42, 86, 44)]
RED_STEEL = [(188, 44, 40), (170, 36, 34), (150, 30, 28)]
YELLOW_PLASTIC = [(240, 196, 40), (226, 178, 30), (206, 160, 24)]
PLATFORM_BLUE = [(48, 110, 190), (40, 98, 172)]
LIMESTONE = [(214, 206, 188), (202, 194, 176), (188, 180, 162), (172, 164, 148)]
CEDAR = [(186, 138, 96), (170, 124, 84), (152, 110, 72), (132, 94, 60)]
RUBBER = [(150, 58, 46), (132, 50, 40), (112, 42, 34), (70, 64, 62)]


def perforated(palette, seed):
    """Perforated steel: a grid of round holes, darker than the sheet."""
    img = noise_tex(palette, seed, grain=0.4)
    px = img.load()
    for y in range(1, 16, 3):
        for x in range(1 + (y // 3) % 2, 16, 3):
            px[x, y] = clamp(tuple(c * 0.45 for c in palette[-1])) + (255,)
    return img


def slats(palette, seed):
    """Teak slats running across the texture, a dark joint every four pixels."""
    img = noise_tex(palette, seed, grain=0.35)
    px = img.load()
    for y in range(3, 16, 4):
        for x in range(16):
            px[x, y] = clamp(palette[-1]) + (255,)
    return img


def lid(body, seed, mark):
    """A bin lid seen from above: a slot, and a light mark (recycling arrows or a bag)."""
    img = noise_tex(body, seed, grain=0.3)
    px = img.load()
    for x in range(4, 12):
        for y in range(5, 7):
            px[x, y] = (20, 22, 24, 255)
    light = (236, 238, 236, 255)
    if mark == "recycle":
        for (x, y) in ((7, 9), (8, 9), (6, 10), (9, 10), (5, 11), (10, 11), (5, 12), (6, 12),
                       (7, 12), (8, 12), (9, 12), (10, 12)):
            px[x, y] = light
    return img


def dog_sign(seed):
    img = Image.new("RGBA", (16, 16), (236, 236, 230, 255))
    px = img.load()
    green = (38, 84, 56, 255)
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = green
    # A dog in profile, and a bag.
    dog = ["....XX....", "...XXXX...", "XXXXXXX...", "XXXXXX....", "X.X..X.X..", "X.X..X.X.."]
    for r, row in enumerate(dog):
        for c, ch in enumerate(row):
            if ch == "X":
                px[3 + c, 4 + r] = (30, 30, 30, 255)
    for x in range(4, 12):
        px[x, 12] = green
    return img


def water(seed, frames=4):
    """Animated water, frames stacked vertically; its .mcmeta sets the pace."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16 * frames))
    base = [(70, 132, 196), (62, 120, 184), (54, 108, 172), (88, 150, 210)]
    for f in range(frames):
        tile = noise_tex(base, seed + f * 7, grain=0.7)
        tp = tile.load()
        for _ in range(10):
            x, y = rng.randrange(16), rng.randrange(16)
            tp[x, y] = (196, 224, 244, 255)
        img.paste(tile, (0, 16 * f))
    return img


def falling_water(seed, frames=4):
    """Animated falling water: translucent-looking streaks on a cutout, moving down a frame."""
    rng = random.Random(seed)
    streaks = [(rng.randrange(16), rng.randrange(16), rng.randint(4, 9)) for _ in range(12)]
    img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    px = img.load()
    for f in range(frames):
        for x, y0, length in streaks:
            for k in range(length):
                y = (y0 + k + f * 4) % 16
                px[x, 16 * f + y] = (178, 214, 240, 255) if k % 3 else (226, 240, 250, 255)
    return img


def spring(seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y * 2) % 6 < 2:
                px[x, y] = (150, 152, 156, 255)
    return img


def cage(seed):
    """Expanded-metal cage mesh over a backflow preventer: diamonds, cut out between."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    g = (44, 92, 60, 255)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0 or x in (0, 15) or y in (0, 15):
                px[x, y] = g
    return img


def lamp(on):
    img = Image.new("RGBA", (16, 16), (40, 110, 50, 255) if on else (26, 42, 30, 255))
    if on:
        px = img.load()
        for x in range(5, 11):
            for y in range(5, 11):
                px[x, y] = (140, 250, 150, 255)
    return img


def controller_face(seed):
    img = noise_tex([(200, 196, 180), (190, 186, 170)], seed, grain=0.3)
    px = img.load()
    for x in range(3, 13):
        for y in range(4, 8):
            px[x, y] = (40, 60, 52, 255)  # the display
    for x in range(4, 9):
        px[x, 5] = (120, 220, 150, 255)
    for (x, y) in ((4, 10), (7, 10), (10, 10), (4, 12), (7, 12), (10, 12)):
        px[x, y] = (90, 90, 96, 255)  # buttons
    return img


def shingles(seed):
    """A roof face's shingles: staggered courses on a 64 px tile, one tile per face, so courses
    come out a few pixels apart on the roof whatever its size."""
    rng = random.Random(seed)
    base = [(70, 92, 76), (62, 84, 68), (56, 76, 62), (48, 66, 54)]
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    for y in range(64):
        course = y // 6
        for x in range(64):
            tab = (x + (course % 2) * 5) // 10
            c = base[(course * 7 + tab * 3) % len(base)]
            c = tuple(v + rng.randint(-5, 5) for v in c)
            if y % 6 == 5:
                c = (32, 44, 36)          # the shadow line under a course
            elif (x + (course % 2) * 5) % 10 == 0:
                c = (40, 54, 44)          # the slot between two tabs
            px[x, y] = clamp(c) + (255,)
    return img


def beadboard(seed):
    """The ceiling under a gazebo roof: white-washed boards with a bead every four pixels."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            v = 232 + rng.randint(-6, 4)
            if x % 4 == 0:
                v -= 34
            px[x, y] = (v, v - 2, v - 8, 255)
    return img


def white_paint(seed):
    return noise_tex([(240, 240, 234), (230, 230, 224), (220, 220, 214)], seed, grain=0.5)


def gazebo_icon():
    """A little gazebo for the roof blocks' inventory icon."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    green, dark, white = (70, 92, 76, 255), (48, 66, 54, 255), (238, 238, 232, 255)
    for y in range(1, 8):
        half = y + 1
        for x in range(8 - half, 8 + half):
            px[x, y] = green if (x + y) % 3 else dark
    for x in range(0, 16):
        px[x, 8] = white
    px[7, 0] = px[8, 0] = white
    for y in range(9, 15):
        for x in (1, 2, 13, 14, 7, 8):
            px[x, y] = white
    for x in range(1, 15):
        px[x, 15] = (150, 120, 84, 255)
        if x % 2:
            px[x, 12] = white
    return img


TEXTURES = {
    "gazebo_shingle": lambda: shingles(131),
    "gazebo_ceiling": lambda: beadboard(132),
    "white_paint": lambda: white_paint(133),
    "gazebo_deck": lambda: pp.boards([(214, 204, 186), (200, 190, 172), (186, 176, 158),
                                      (170, 160, 142)], 134),
    "gazebo_icon": gazebo_icon,
    "teak": lambda: slats(TEAK, 101),
    "iron": lambda: noise_tex(IRON, 102, grain=0.8),
    "green_steel": lambda: noise_tex(GREEN_STEEL, 103, grain=0.6),
    "perforated_green": lambda: perforated(GREEN_STEEL, 104),
    "bin_blue": lambda: noise_tex(BLUE_PLASTIC, 105, grain=0.3),
    "bin_green": lambda: noise_tex(GREEN_PLASTIC, 106, grain=0.3),
    "lid_blue": lambda: lid(BLUE_PLASTIC, 107, "recycle"),
    "lid_green": lambda: lid(GREEN_PLASTIC, 108, None),
    "dog_sign": lambda: dog_sign(109),
    "red_steel": lambda: noise_tex(RED_STEEL, 110, grain=0.5),
    "yellow_plastic": lambda: noise_tex(YELLOW_PLASTIC, 111, grain=0.3),
    "platform_blue": lambda: noise_tex(PLATFORM_BLUE, 112, grain=0.3),
    "spring": lambda: spring(113),
    "limestone": lambda: noise_tex(LIMESTONE, 114, grain=0.8),
    "water": lambda: water(115),
    "falling_water": lambda: falling_water(116),
    "cedar": lambda: pp.boards(CEDAR, 117),
    "rubber": lambda: noise_tex(RUBBER, 118, cells=8, grain=0.95),
    "controller": lambda: controller_face(119),
    "controller_side": lambda: noise_tex([(200, 196, 180), (188, 184, 168)], 120, grain=0.4),
    "lamp_off": lambda: lamp(False),
    "lamp_on": lambda: lamp(True),
    "cage": lambda: cage(121),
    "brass": lambda: noise_tex([(196, 160, 80), (178, 142, 66), (158, 124, 54)], 122),
    "sprinkler_cap": lambda: noise_tex([(44, 46, 48), (36, 38, 40)], 123),
}
ANIMATED = {"water": 6, "falling_water": 3}


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
BLOCKS = []
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def add(registry, java, names, models, blockstate, item=None):
    BLOCKS.append({"registry": registry, "java": java, "names": names, "models": models,
                   "blockstate": blockstate, "item": item})


def facing_state(model_name, extra=None, textures=None):
    variants = {"facing": {f: ({"y": r} if r else {}) for f, r in ROT.items()},
                "inventory": [{}]}
    variants.update(extra or {})
    defaults = {"model": MODEL + model_name}
    if textures:
        defaults["textures"] = textures
    return {"forge_marker": 1, "defaults": defaults, "variants": variants}


def facing_block(registry, java_box, names, elements, textures, collides=True):
    add(registry, 'new BlockParkFacing("%s", new int[]{%s}, %s)'
        % (registry, ", ".join(str(v) for v in java_box), "true" if collides else "false"),
        names, {registry: model(dict(textures, particle=textures[next(iter(textures))]),
                                elements)},
        facing_state(registry))


def run_block(registry, java_box, names, textures, body, left, right):
    """A bench-like run: body always; the end frames where the run stops (sitter's left/right)."""
    tex = dict(textures, particle=textures[next(iter(textures))])
    models = {registry: model(tex, body), registry + "_left": model(tex, left),
              registry + "_right": model(tex, right),
              registry + "_item": model(tex, body + left + right)}
    parts = []
    for facing, rot in ROT.items():
        for part, side in (("", None), ("_left", "left"), ("_right", "right")):
            when = {"facing": facing}
            if side:
                when[side] = "false"
            apply = {"model": MODEL + registry + part}
            if rot:
                apply["y"] = rot
            parts.append({"when": when, "apply": apply})
    add(registry, 'new BlockParkBench("%s", new int[]{%s})'
        % (registry, ", ".join(str(v) for v in java_box)), names, models,
        {"multipart": parts}, {"parent": "csm:block/parks/amenities/%s_item" % registry})


def mirror_x(elements):
    """The same elements reflected across x = 8 (the right-hand end frame from the left)."""
    out = []
    for e in elements:
        m = dict(e)
        m["from"] = [16 - e["to"][0], e["from"][1], e["from"][2]]
        m["to"] = [16 - e["from"][0], e["to"][1], e["to"][2]]
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [16 - r["origin"][0], r["origin"][1], r["origin"][2]]
            m["rotation"] = r
        faces = dict(e["faces"])
        if "east" in faces or "west" in faces:
            faces["east"], faces["west"] = e["faces"].get("west"), e["faces"].get("east")
            faces = {k: v for k, v in faces.items() if v is not None}
        m["faces"] = faces
        out.append(m)
    return out


# --- benches ---
def bench_frame(frame):
    return [box([1, 0, 2.5], [2, 7, 3.5], frame), box([1, 0, 9.5], [2, 14.5, 10.5], frame),
            box([1, 6, 2], [2, 7, 10.5], frame), box([1, 10, 2], [2, 11, 10.5], frame),
            box([1, 7, 2.5], [2, 10, 3.5], frame)]


def seat_slats(zs, tex, y=7):
    return [box([0, y, z0], [16, y + 1, z1], tex) for z0, z1 in zs]


frame = bench_frame("frame")
run_block("park_bench_wood", [0, 0, 1, 16, 15, 12],
          ("Park Bench", "Parkbank", "Banco de parque", "Parkbänk"),
          {"slat": T("teak"), "frame": T("iron")},
          seat_slats([(2, 4), (4.5, 6.5), (7, 9)], "slat")
          + [box([0, y0, 10], [16, y0 + 1.5, 11], "slat") for y0 in (9, 11, 13)],
          frame, mirror_x(frame))
backless = [box([1, 0, 3.5], [2, 7, 4.5], "frame"), box([1, 0, 11], [2, 7, 12], "frame"),
            box([1, 6, 3], [2, 7, 13], "frame")]
run_block("park_bench_backless", [0, 0, 2, 16, 8, 14],
          ("Backless Park Bench", "Parkbank ohne Lehne", "Banco de parque sin respaldo",
           "Parkbänk utan ryggstöd"),
          {"slat": T("teak"), "frame": T("iron")},
          seat_slats([(3, 5), (5.5, 7.5), (8, 10), (10.5, 12.5)], "slat"),
          backless, mirror_x(backless))
steel_frame = bench_frame("frame")
run_block("park_bench_steel", [0, 0, 1, 16, 15, 12],
          ("Perforated Steel Bench", "Lochblech-Parkbank", "Banco de acero perforado",
           "Bänk av perforerat stål"),
          {"sheet": T("perforated_green"), "frame": T("green_steel")},
          [box([0, 7, 2], [16, 8, 9.5], "sheet"), box([0, 9, 10], [16, 14.5, 11], "sheet")],
          steel_frame, mirror_x(steel_frame))


# --- picnic tables ---
def picnic_frame(frame):
    leg = {"from": [1, 0, 7.5], "to": [2, 11.5, 8.5]}
    legs = []
    for angle in (22.5, -22.5):
        e = box(leg["from"], leg["to"], frame)
        e["rotation"] = {"origin": [1.5, 5.75, 8], "axis": "x", "angle": angle}
        legs.append(e)
    return legs + [box([1, 5.5, 0.5], [2, 6.5, 15.5], frame),
                   box([1, 10.5, 4], [2, 11, 12], frame)]


for tid, tex, names in (
        ("wood", {"top": T("teak"), "frame": T("teak")},
         ("Picnic Table", "Picknicktisch", "Mesa de pícnic", "Picknickbord")),
        ("steel", {"top": T("perforated_green"), "frame": T("green_steel")},
         ("Perforated Steel Picnic Table", "Lochblech-Picknicktisch",
          "Mesa de pícnic de acero perforado", "Picknickbord av perforerat stål"))):
    pf = picnic_frame("frame")
    run_block("picnic_table_" + tid, [0, 0, 0, 16, 12, 16], names, tex,
              [box([0, 11, 4], [16, 12, 12], "top"), box([0, 6.5, 0.5], [16, 7.5, 3.5], "top"),
               box([0, 6.5, 12.5], [16, 7.5, 15.5], "top")],
              pf, mirror_x(pf))

# --- bins ---
facing_block("recycling_bin", [3, 0, 3, 13, 15, 13],
             ("Recycling Bin", "Wertstofftonne", "Contenedor de reciclaje", "Återvinningskärl"),
             [box([3, 0, 3], [13, 13, 13], "bin"),
              box([2.5, 13, 2.5], [13.5, 15, 13.5], "bin", per={"up": "lid"})],
             {"bin": T("bin_blue"), "lid": T("lid_blue")})
facing_block("trash_recycling_station", [1, 0, 3, 15, 15, 13],
             ("Trash and Recycling Station", "Abfall- und Wertstoffstation",
              "Estación de basura y reciclaje", "Station för sopor och återvinning"),
             [box([1.5, 0, 3.5], [7.5, 13, 12.5], "green"),
              box([1.25, 13, 3.25], [7.75, 14.5, 12.75], "green", per={"up": "lid_green"}),
              box([8.5, 0, 3.5], [14.5, 13, 12.5], "blue"),
              box([8.25, 13, 3.25], [14.75, 14.5, 12.75], "blue", per={"up": "lid_blue"}),
              box([1, 0, 12.75], [15, 15, 13.25], "steel")],
             {"green": T("bin_green"), "blue": T("bin_blue"), "lid_green": T("lid_green"),
              "lid_blue": T("lid_blue"), "steel": T("green_steel")})
facing_block("dog_waste_station", [4, 0, 4, 12, 16, 13],
             ("Dog Waste Station", "Hundekotbeutelstation", "Estación para desechos caninos",
              "Hundbajsstation"),
             [box([7, 0, 11], [9, 24, 13], "steel"),
              box([4, 17, 10.5], [12, 24, 11], "steel", per={"north": "sign"}),
              box([5.5, 12, 8.5], [10.5, 16, 11], "steel"),
              box([4.5, 0, 4], [11.5, 10, 10.5], "steel"),
              box([4.25, 10, 3.75], [11.75, 11, 10.75], "steel", per={"up": "lid"})],
             {"steel": T("green_steel"), "sign": T("dog_sign"), "lid": T("lid_green")})

# --- playground ---
chute_len = 26.5
chute = box([3, 9.75, 0.5 - chute_len / 2], [13, 10.75, 0.5 + chute_len / 2], "chute")
chute["rotation"] = {"origin": [8, 10.25, 0.5], "axis": "x", "angle": -45}
rails = []
for x0 in (2.5, 12.5):
    r = box([x0, 10.25, 0.5 - chute_len / 2], [x0 + 1, 12.25, 0.5 + chute_len / 2], "frame")
    r["rotation"] = {"origin": [8, 10.25, 0.5], "axis": "x", "angle": -45}
    rails.append(r)
facing_block("playground_slide", [2, 0, 0, 14, 16, 16],
             ("Playground Slide", "Spielplatzrutsche", "Tobogán", "Lekplatsrutschkana"),
             [box([3, 0, 14], [4, 22, 15], "frame"), box([12, 0, 14], [13, 22, 15], "frame")]
             + [box([4, y, 14.25], [12, y + 0.75, 14.75], "frame") for y in (3, 7, 11, 15)]
             + [box([2.5, 19, 10.5], [13.5, 20, 15.5], "platform"),
                box([2.5, 20, 10.5], [3.5, 24, 15.5], "frame"),
                box([12.5, 20, 10.5], [13.5, 24, 15.5], "frame"),
                chute] + rails,
             {"frame": T("red_steel"), "chute": T("yellow_plastic"),
              "platform": T("platform_blue")})
facing_block("spring_rider", [4, 0, 2, 12, 14, 14],
             ("Spring Rider", "Federwippe", "Balancín de muelle", "Fjädergunga"),
             [box([5, 0, 5], [11, 0.5, 11], "steel"),
              box([6.5, 0.5, 6.5], [9.5, 6, 9.5], "spring"),
              box([5.5, 6, 3], [10.5, 10, 13], "body"),
              box([6, 9, 1], [10, 14, 5], "body"),
              box([6.5, 10, 12.5], [9.5, 12, 15], "body"),
              box([5.5, 10, 6], [10.5, 10.5, 10], "steel"),
              box([4, 11.5, 4.5], [12, 12.5, 5.5], "handle")],
             {"body": T("red_steel"), "steel": T("iron"), "spring": T("spring"),
              "handle": T("yellow_plastic")})

# --- pergola ---
add("pergola_post", 'new BlockParkProp("pergola_post", BlockParkProp.Kind.POST, 16, 5)',
    ("Pergola Post", "Pergolapfosten", "Poste de pérgola", "Pergolastolpe"),
    {"pergola_post": model({"wood": T("cedar"), "particle": T("cedar")},
                           [box([5, 0, 5], [11, 16, 11], "wood")])},
    {"forge_marker": 1, "defaults": {"model": MODEL + "pergola_post"},
     "variants": {"normal": [{}], "inventory": [{}]}})


def joining_block(registry, kind, height, width, names, post, side, item_extra, side_when,
                  uvlock=False):
    """uvlock keeps a turned side's texture where the unturned one's is, for sides that overlap
    at a corner (the pergola's beams cross there): identical pixels on the shared faces, so
    they cannot z-fight."""
    models = {registry + "_post": post, registry + "_side": side}
    parts = [{"apply": {"model": MODEL + registry + "_post"}}]
    for direction, rot in ROT.items():
        apply = {"model": MODEL + registry + "_side"}
        if rot:
            apply["y"] = rot
            if uvlock:
                apply["uvlock"] = True
        parts.append({"when": {direction: side_when}, "apply": apply})
    models[registry + "_item"] = {
        "parent": "block/block", "textures": post["textures"],
        "elements": post.get("elements", []) + list(item_extra),
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                            "scale": [0.625, 0.625, 0.625]}},
    }
    add(registry, 'new BlockParkJoining("%s", BlockParkJoining.Kind.%s, %d, %d)'
        % (registry, kind, height, width), names, models, {"multipart": parts},
        {"parent": "csm:block/parks/amenities/%s_item" % registry})


# The roof sits straight on the posts: a beam along each outer row, centred over the post
# line (the posts are 6 across at the middle of their cells), and rafters across on top.
wood = {"wood": T("cedar"), "particle": T("cedar")}
beam_n = box([0, 0, 6], [16, 4, 10], "wood")
joining_block("pergola_top", "PERGOLA", 6, 16,
              ("Pergola Beams", "Pergolabalken", "Vigas de pérgola", "Pergolabjälkar"),
              model(wood, [box([0, 4, z], [16, 6, z + 2], "wood") for z in (2, 12)]),
              model(wood, [beam_n]),
              [beam_n, box([6, 0, 0], [10, 4, 16], "wood")], "false", uvlock=True)

# --- fountains ---
stone_water = {"stone": T("limestone"), "water": T("water"), "particle": T("limestone")}
joining_block("fountain_basin", "BED", 11, 16,
              ("Fountain Basin", "Brunnenbecken", "Pila de fuente", "Fontänbassäng"),
              model(stone_water, [box([0, 0, 0], [16, 8, 16], "stone", per={"up": "water"})]),
              model(stone_water, [box([0, 0, 0], [16, 11, 2.5], "stone")]),
              [box([0, 0, 0], [16, 11, 2.5], "stone"), box([0, 0, 13.5], [16, 11, 16], "stone"),
               box([0, 0, 2.5], [2.5, 11, 13.5], "stone"),
               box([13.5, 0, 2.5], [16, 11, 13.5], "stone")], "false")


def curtain(x0, x1, z, y0, y1, facing_z=True):
    """A zero-thickness sheet of falling water, both faces drawn."""
    if facing_z:
        return {"from": [x0, y0, z], "to": [x1, y1, z], "shade": False,
                "faces": {"north": face("fall", [0, 0, 16, 16]),
                          "south": face("fall", [0, 0, 16, 16])}}
    return {"from": [z, y0, x0], "to": [z, y1, x1], "shade": False,
            "faces": {"east": face("fall", [0, 0, 16, 16]), "west": face("fall", [0, 0, 16, 16])}}


fountain_tex = {"stone": T("limestone"), "water": T("water"), "fall": T("falling_water"),
                "particle": T("limestone")}
add("fountain_tiered", 'new BlockParkProp("fountain_tiered", BlockParkProp.Kind.PLANTER, 16, 2)',
    ("Tiered Fountain", "Etagenbrunnen", "Fuente de pisos", "Våningsfontän"),
    {"fountain_tiered": model(fountain_tex, [
        box([5, 0, 5], [11, 8, 11], "stone"),
        box([1, 8, 1], [15, 10, 15], "stone", per={"up": "water"}),
        box([7, 10, 7], [9, 18, 9], "stone"),
        box([4, 18, 4], [12, 19.5, 12], "stone", per={"up": "water"}),
        box([7.25, 19.5, 7.25], [8.75, 23, 8.75], "stone"),
        curtain(4, 12, 3.9, 10, 18), curtain(4, 12, 12.1, 10, 18),
        curtain(4, 12, 3.9, 10, 18, facing_z=False), curtain(4, 12, 12.1, 10, 18, facing_z=False),
    ])},
    {"forge_marker": 1, "defaults": {"model": MODEL + "fountain_tiered"},
     "variants": {"normal": [{}], "inventory": [{}]}})

# --- irrigation ---
spr_tex = {"cap": T("sprinkler_cap"), "riser": T("iron"), "particle": T("sprinkler_cap")}
add("irrigation_sprinkler", None,
    ("Pop-Up Sprinkler", "Versenkregner", "Aspersor emergente", "Pop-up-spridare"),
    {"sprinkler_down": model(spr_tex, [box([6.5, 0, 6.5], [9.5, 1, 9.5], "cap")]),
     "sprinkler_up": model(spr_tex, [box([7, 0, 7], [9, 4, 9], "riser"),
                                     box([6.5, 4, 6.5], [9.5, 5.5, 9.5], "cap"),
                                     box([7.5, 4.5, 5.5], [8.5, 5, 6.5], "cap")])},
    {"forge_marker": 1, "defaults": {"model": MODEL + "sprinkler_down"},
     "variants": {"powered": {"false": {}, "true": {"model": MODEL + "sprinkler_up"}},
                  "inventory": [{"model": MODEL + "sprinkler_up"}]}})
ctl_tex = {"face": T("controller"), "side": T("controller_side"), "lamp": T("lamp_off"),
           "particle": T("controller_side")}
add("irrigation_controller", None,
    ("Irrigation Controller", "Bewässerungssteuerung", "Programador de riego",
     "Bevattningsstyrning"),
    {"irrigation_controller": model(ctl_tex, [
        box([4, 3, 13], [12, 13, 16], "side", per={"north": "face"}),
        box([10, 11, 12.75], [11, 12, 13], "lamp", faces=("north", "east", "west", "up", "down")),
    ])},
    facing_state("irrigation_controller", extra={
        "powered": {"false": {}, "true": {"textures": {"lamp": T("lamp_on")}}},
        "manual": {"false": {}, "true": {}}}))
facing_block("backflow_preventer", [2, 0, 3, 14, 14, 13],
             ("Backflow Preventer", "Rückflussverhinderer", "Válvula antirretorno",
              "Återströmningsskydd"),
             [box([3, 0, 7], [5, 9, 9], "brass"), box([11, 0, 7], [13, 9, 9], "brass"),
              box([3, 9, 7], [13, 11, 9], "brass"), box([6, 8, 6.5], [8, 12, 9.5], "brass"),
              box([2, 0, 3], [14, 14, 13], "cage", faces=("north", "south", "east", "west",
                                                           "up"))],
             {"cage": T("cage"), "brass": T("brass")})

# --- surface ---
add("ground_rubber_safety",
    'new BlockParkProp("ground_rubber_safety", BlockParkProp.Kind.COVER, 1, 0)',
    ("Rubber Safety Surface", "Fallschutzbelag", "Suelo de caucho de seguridad",
     "Fallskyddsgummi"),
    {"ground_rubber_safety": model({"all": T("rubber"), "particle": T("rubber")},
                                   [box([0, 0, 0], [16, 1, 16], "all")])},
    {"forge_marker": 1, "defaults": {"model": MODEL + "ground_rubber_safety"},
     "variants": {"normal": [{}], "inventory": [{}]}})

# --- gazebo: a kit of post, railing and deck, and a roof block drawn as an OBJ ---
white = {"paint": T("white_paint"), "particle": T("white_paint")}
# A post stacked from blocks is one turned column: the shaft in every block, the base only at the
# bottom and the capital only at the top (BlockParkColumn's actual state).
post_base = box([5, 0, 5], [11, 2, 11], "paint")
post_cap = box([5, 14, 5], [11, 16, 11], "paint")
add("gazebo_post", 'new BlockParkColumn("gazebo_post", BlockParkProp.Kind.POST, 16, 5)',
    ("Gazebo Post", "Pavillonpfosten", "Poste de cenador", "Paviljongstolpe"),
    {"gazebo_post_shaft": model(white, [box([6, 0, 6], [10, 16, 10], "paint",
                                            faces=("north", "south", "east", "west"))]),
     "gazebo_post_base": model(white, [post_base]),
     "gazebo_post_cap": model(white, [post_cap]),
     "gazebo_post_item": model(white, [post_base, box([6, 2, 6], [10, 14, 10], "paint"),
                                       post_cap])},
    {"multipart": [
        {"apply": {"model": MODEL + "gazebo_post_shaft"}},
        {"when": {"base": "true"}, "apply": {"model": MODEL + "gazebo_post_base"}},
        {"when": {"cap": "true"}, "apply": {"model": MODEL + "gazebo_post_cap"}}]},
    {"parent": "csm:block/parks/amenities/gazebo_post_item"})
rail_side = [box([7, 9, 0], [9, 10, 8], "paint", faces=("east", "west", "up", "down")),
             box([7, 1, 0], [9, 2, 8], "paint", faces=("east", "west", "up", "down"))]
rail_side += [box([7.5, 2, z], [8.5, 9, z + 1], "paint", faces=("north", "south", "east", "west"))
              for z in (1.5, 4.5)]
joining_block("gazebo_railing", "RAIL", 10, 2,
              ("Gazebo Railing", "Pavillongeländer", "Barandilla de cenador", "Paviljongräcke"),
              model(white, [box([7, 0, 7], [9, 10, 9], "paint")]),
              model(white, rail_side), rail_side, "true")
add("gazebo_deck", 'new BlockParkProp("gazebo_deck", BlockParkProp.Kind.COVER, 2, 0)',
    ("Gazebo Deck", "Pavillonboden", "Suelo de cenador", "Paviljonggolv"),
    {"gazebo_deck": model({"all": T("gazebo_deck"), "particle": T("gazebo_deck")},
                          [box([0, 0, 0], [16, 2, 16], "all")])},
    {"forge_marker": 1, "defaults": {"model": MODEL + "gazebo_deck"},
     "variants": {"normal": [{}], "inventory": [{}]}})


class Mesh:
    """An OBJ in pixels, grouped by material. Faces are wound counter-clockwise seen from
    outside, which is what the normal given says outside is."""

    def __init__(self):
        self.v, self.vt, self.vn = [], [], []
        self.groups = {}

    def face(self, mat, pts, uvs, normal):
        a, b, c = pts[0], pts[1], pts[2]
        e1 = [b[i] - a[i] for i in range(3)]
        e2 = [c[i] - a[i] for i in range(3)]
        cr = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2],
              e1[0] * e2[1] - e1[1] * e2[0]]
        if sum(cr[i] * normal[i] for i in range(3)) < 0:
            pts, uvs = list(reversed(pts)), list(reversed(uvs))
        ln = sum(n * n for n in normal) ** 0.5
        self.vn.append(tuple(n / ln for n in normal))
        ni = len(self.vn)
        idx = []
        for p_, t in zip(pts, uvs):
            self.v.append(p_)
            # Inside the sprite's border, where mip levels do not bleed; flip-v is on.
            self.vt.append((0.02 + 0.96 * t[0], 1.0 - (0.02 + 0.96 * t[1])))
            idx.append((len(self.v), len(self.vt), ni))
        self.groups.setdefault(mat, []).append(idx)

    def box(self, mat, lo, hi, skip=()):
        x0, y0, z0 = lo
        x1, y1, z1 = hi
        faces = {
            "down": ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], (0, -1, 0)),
            "up": ([(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)], (0, 1, 0)),
            "north": ([(x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (x1, y0, z0)], (0, 0, -1)),
            "south": ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], (0, 0, 1)),
            "west": ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], (-1, 0, 0)),
            "east": ([(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], (1, 0, 0)),
        }
        for name, (pts, n) in faces.items():
            if name not in skip:
                self.face(mat, pts, [(0, 0), (1, 0), (1, 1), (0, 1)], n)

    def text(self, mtl):
        lines = ["# Generated by dev-env-utils/scripts/gen_park_amenities.py -- do not hand edit",
                 "mtllib %s" % mtl, "o gazebo_roof"]
        lines += ["v %.6f %.6f %.6f" % (x / 16.0, y / 16.0, z / 16.0) for x, y, z in self.v]
        lines += ["vt %.6f %.6f" % t for t in self.vt]
        lines += ["vn %.6f %.6f %.6f" % n for n in self.vn]
        for mat in sorted(self.groups):
            lines.append("usemtl %s" % mat)
            for f in self.groups[mat]:
                lines.append("f " + " ".join("%d/%d/%d" % i for i in f))
        return "\n".join(lines) + "\n"


def gazebo_roof(n):
    """A hip roof over an n x n footprint, from the block on top of its middle: the eave on the
    posts (the cell's floor), a white fascia, a boarded ceiling to look up at, four shingled
    faces up to a cupola, and a finial on the cupola's cap."""
    m = Mesh()
    c = 8.0
    half = n * 8 + 3                   # the footprint, and a 3 px overhang
    eave = 2.0                         # the fascia's height
    rise = half * 0.62                 # a pitch of about 32 degrees
    apex = eave + rise
    # Ceiling, seen from below, tiled a texture to 16 px so the boards keep their width (one
    # stretched quad made each bead a finger wide), and the fascia round the eave.
    edges = [c - half]
    while edges[-1] + 16 < c + half:
        edges.append(edges[-1] + 16)
    edges.append(c + half)
    for i in range(len(edges) - 1):
        for j in range(len(edges) - 1):
            x0, x1, z0, z1 = edges[i], edges[i + 1], edges[j], edges[j + 1]
            u1, v1 = (x1 - x0) / 16.0, (z1 - z0) / 16.0
            m.face("ceiling", [(x0, 0.02, z0), (x1, 0.02, z0), (x1, 0.02, z1), (x0, 0.02, z1)],
                   [(0, 0), (u1, 0), (u1, v1), (0, v1)], (0, -1, 0))
    t = 1.2
    m.box("trim", (c - half, 0, c - half), (c + half, eave, c - half + t), skip=("up",))
    m.box("trim", (c - half, 0, c + half - t), (c + half, eave, c + half), skip=("up",))
    m.box("trim", (c - half, 0, c - half + t), (c - half + t, eave, c + half - t),
          skip=("up", "north", "south"))
    m.box("trim", (c + half - t, 0, c - half + t), (c + half, eave, c + half - t),
          skip=("up", "north", "south"))
    # The four faces, eave corners to the apex.
    corners = [(c - half, c - half), (c + half, c - half), (c + half, c + half),
               (c - half, c + half)]
    for i in range(4):
        (ax, az), (bx, bz) = corners[i], corners[(i + 1) % 4]
        mx, mz = (ax + bx) / 2 - c, (az + bz) / 2 - c
        # Outward normal of a face: horizontal part toward the edge, vertical part up.
        normal = (mx / half * rise, half, mz / half * rise)
        m.face("shingle", [(ax, eave, az), (bx, eave, bz), (c, apex, c)],
               [(0, 0), (1, 0), (0.5, 1)], normal)
    # Cupola: a white box where the faces are 4.5 px from the ridge line, capped with a small
    # shingled pyramid and a finial.
    r = 4.5
    y_c = eave + rise * (1 - r / half) - 1
    m.box("trim", (c - r, y_c, c - r), (c + r, y_c + 5, c + r), skip=("down",))
    cap = r + 1.5
    for i in range(4):
        ax, az = [(c - cap, c - cap), (c + cap, c - cap), (c + cap, c + cap), (c - cap, c + cap)][i]
        bx, bz = [(c + cap, c - cap), (c + cap, c + cap), (c - cap, c + cap), (c - cap, c - cap)][i]
        mx, mz = (ax + bx) / 2 - c, (az + bz) / 2 - c
        m.face("shingle", [(ax, y_c + 5, az), (bx, y_c + 5, bz), (c, y_c + 9, c)],
               [(0, 0), (1, 0), (0.5, 1)], (mx / cap * 4, cap, mz / cap * 4))
    m.face("ceiling", [(c - cap, y_c + 5, c - cap), (c + cap, y_c + 5, c - cap),
                       (c + cap, y_c + 5, c + cap), (c - cap, y_c + 5, c + cap)],
           [(0, 0), (1, 0), (1, 1), (0, 1)], (0, -1, 0))
    m.box("trim", (c - 0.75, y_c + 8, c - 0.75), (c + 0.75, y_c + 12, c + 0.75), skip=("down",))
    return m


OBJ_FILES = {}
GAZEBO_MTL = "gazebo_roof.mtl"
for n in (3, 5):
    reg = "gazebo_roof_%dx%d" % (n, n)
    OBJ_FILES["models/block/parks/amenities/%s.obj" % reg] = gazebo_roof(n).text(GAZEBO_MTL)
    add(reg, 'new BlockParkProp("%s", BlockParkProp.Kind.ROOF, 16, %d)' % (reg, -8 * (n - 1)),
        ("Gazebo Roof (%dx%d)" % (n, n), "Pavillondach (%dx%d)" % (n, n),
         "Tejado de cenador (%dx%d)" % (n, n), "Paviljongtak (%dx%d)" % (n, n)),
        {reg + "_item": {"parent": "item/generated", "textures": {"layer0": T("gazebo_icon")}}},
        {"forge_marker": 1,
         "variants": {
             "normal": [{"model": MODEL + reg + ".obj", "custom": {"flip-v": True},
                         "textures": {"#shingle": T("gazebo_shingle"),
                                      "#trim": T("white_paint"),
                                      "#ceiling": T("gazebo_ceiling"),
                                      "particle": T("gazebo_shingle")}}],
             "inventory": [{"model": MODEL + reg + "_item"}]}})
OBJ_FILES["models/block/parks/amenities/" + GAZEBO_MTL] = (
    "# Generated by gen_park_amenities.py -- do not hand edit; the blockstate retextures these\n"
    + "".join("newmtl %s\nmap_Kd %s\n" % (mat, T(tex)) for mat, tex in
              (("ceiling", "gazebo_ceiling"), ("shingle", "gazebo_shingle"),
               ("trim", "white_paint"))))

# Blocks built from their own class, with a fixed registry name, register by class.
CLASS_BLOCKS = {"irrigation_sprinkler": "BlockSprinkler",
                "irrigation_controller": "BlockIrrigationController"}

EXTRA_LANG = {
    "csm.parks.seat.taken": (
        "Someone is already sitting there", "Da sitzt schon jemand",
        "Ya hay alguien sentado ahí", "Någon sitter redan där"),
    "csm.parks.irrigation.manual_on": (
        "Manual watering on", "Manuelle Bewässerung an", "Riego manual activado",
        "Manuell bevattning på"),
    "csm.parks.irrigation.manual_off": (
        "Manual watering off: back on the morning schedule",
        "Manuelle Bewässerung aus: wieder nach Morgenplan",
        "Riego manual desactivado: vuelve al horario de la mañana",
        "Manuell bevattning av: tillbaka till morgonschemat"),
}


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def lang_entries():
    out = {loc: {} for loc in LOCALES}
    for b in BLOCKS:
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % b["registry"]] = b["names"][i]
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(LOCALES):
            out[loc][key] = names[i]
    return out


def generate(assets):
    written = []
    for name, draw in TEXTURES.items():
        rel = "textures/blocks/parks/amenities/%s.png" % name
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
        if name in ANIMATED:
            rel += ".mcmeta"
            pp.dump(os.path.join(assets, rel), {"animation": {"frametime": ANIMATED[name]}})
            written.append(rel)
    for b in BLOCKS:
        for name, data in b["models"].items():
            rel = "models/block/parks/amenities/%s.json" % name
            pp.dump(os.path.join(assets, rel), data)
            written.append(rel)
        rel = "blockstates/%s.json" % b["registry"]
        pp.dump(os.path.join(assets, rel), b["blockstate"])
        written.append(rel)
        if b["item"]:
            rel = "models/item/%s.json" % b["registry"]
            pp.dump(os.path.join(assets, rel), b["item"])
            written.append(rel)
    for rel, text in OBJ_FILES.items():
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", newline="\n", encoding="utf-8") as fh:
            fh.write(text)
        written.append(rel)
    gen_trees.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in LOCALES]
    return written


def fragments():
    lines = []
    for b in BLOCKS:
        if b["registry"] in CLASS_BLOCKS:
            lines.append("    initTabBlock(%s.class, fmlPreInitializationEvent);"
                         % CLASS_BLOCKS[b["registry"]])
        else:
            lines.append("    initTabBlock(%s);" % b["java"])
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="amenities_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("park amenities are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
