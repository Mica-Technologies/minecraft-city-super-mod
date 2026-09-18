#!/usr/bin/env python3
"""Every asset the floor finishes ship.

    python dev-env-utils/scripts/gen_flooring.py
    python dev-env-utils/scripts/gen_flooring.py --check
    python dev-env-utils/scripts/gen_flooring.py --fragments   # lang and tab lines to paste

Two kinds of flooring, both in the Interior Finishes tab:

- **Overlays** (BlockFloorFinish, one class constructed by registry name): a one-pixel finish laid
  on top of any floor, as vanilla carpet is -- carpet tile, vinyl composition tile, ceramic tile,
  hardwood, polished concrete and studded rubber, eleven in all.
- **Full-block sets** (a block, stairs, slab and fence each, on gen_cmu.py's blockstates) for the
  two that are also a structure: polished concrete and hardwood in two species.

Nothing repeats as a visible pattern across a big floor. Each overlay's blockstate picks, per block
position, one of several turns of the model (a carpet tile laid quarter-turned) and, where a texture
would otherwise tile visibly, one of two drawings of it (hardwood board joints fall in different
places, tiles are shaded differently). A tile grid is never turned: its joints are drawn on two
edges of the texture, and a turned block would double them at one seam and lose them at another. Hardwood keeps its boards running the way it was laid (the
block's `axis`), and only turns end for end.
"""

import argparse
import os
import random
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "flooring")
MODEL_DIR = os.path.join(MODULE, "models", "block", "flooring")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/flooring/%s"
MODEL_REF = "csm:flooring/%s"

# Overlays: registry name -> (drawing, colour, turns, name in each language). Order is creative
# order. "turns" is "any" (four turns), "axis" (boards: end for end only) or "none" (a tile
# grid, whose joints are drawn on two edges of the texture and would double up or go missing at a
# seam if a block were turned).
OVERLAYS = {
    "floor_carpet_grey": ("carpet", (120, 122, 126), "any", (
        "Carpet Tile (Grey)", "Loseta de Moqueta (Gris)", "Teppichfliese (Grau)",
        "Textilplatta (Grå)")),
    "floor_carpet_blue": ("carpet", (58, 72, 104), "any", (
        "Carpet Tile (Blue)", "Loseta de Moqueta (Azul)", "Teppichfliese (Blau)",
        "Textilplatta (Blå)")),
    "floor_carpet_charcoal": ("carpet", (58, 58, 62), "any", (
        "Carpet Tile (Charcoal)", "Loseta de Moqueta (Carbón)", "Teppichfliese (Anthrazit)",
        "Textilplatta (Koksgrå)")),
    "floor_vct_white": ("vct", (226, 224, 216), "none", (
        "Vinyl Composition Tile (White)", "Loseta Vinílica (Blanca)", "Vinylfliese (Weiß)",
        "Vinylplatta (Vit)")),
    "floor_vct_beige": ("vct", (206, 192, 166), "none", (
        "Vinyl Composition Tile (Beige)", "Loseta Vinílica (Beige)", "Vinylfliese (Beige)",
        "Vinylplatta (Beige)")),
    "floor_ceramic_white": ("ceramic", (232, 230, 226), "none", (
        "Ceramic Floor Tile (White)", "Baldosa Cerámica (Blanca)", "Keramikfliese (Weiß)",
        "Klinkerplatta (Vit)")),
    "floor_ceramic_grey": ("ceramic", (150, 150, 148), "none", (
        "Ceramic Floor Tile (Grey)", "Baldosa Cerámica (Gris)", "Keramikfliese (Grau)",
        "Klinkerplatta (Grå)")),
    "floor_hardwood_oak": ("hardwood", (176, 132, 84), "axis", (
        "Hardwood Floor (Oak)", "Suelo de Madera (Roble)", "Parkett (Eiche)",
        "Trägolv (Ek)")),
    "floor_hardwood_walnut": ("hardwood", (104, 70, 46), "axis", (
        "Hardwood Floor (Walnut)", "Suelo de Madera (Nogal)", "Parkett (Nussbaum)",
        "Trägolv (Valnöt)")),
    "floor_polished_concrete": ("concrete", (166, 166, 162), "any", (
        "Polished Concrete Floor", "Suelo de Hormigón Pulido", "Geschliffener Betonboden",
        "Slipat Betonggolv")),
    "floor_rubber_studded": ("rubber", (44, 44, 46), "any", (
        "Rubber Floor (Studded)", "Suelo de Caucho (Con Tacos)", "Noppenbelag (Gummi)",
        "Gummigolv (Noppor)")),
}

# Full-block sets: name -> (drawing, colour, name in each language). The class for each is
# gen_cmu.class_name(name).
SETS = {
    "polished_concrete": ("concrete", (166, 166, 162), (
        "Polished Concrete", "Hormigón Pulido", "Geschliffener Beton", "Slipad Betong")),
    "hardwood_oak": ("hardwood", (176, 132, 84), (
        "Oak Hardwood", "Madera de Roble", "Eichenparkett", "Ekparkett")),
    "hardwood_walnut": ("hardwood", (104, 70, 46), (
        "Walnut Hardwood", "Madera de Nogal", "Nussbaumparkett", "Valnötsparkett")),
}

# --------------------------------------------------------------------------------------------
# Textures -- each drawing takes a colour and a variant number (0 or 1)
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift


def _seed(name, variant):
    return sum(ord(c) * (i + 1) for i, c in enumerate(name)) * 7 + variant


def carpet(rgb, rng, variant):
    """Loop pile carpet tile: fine noise, rows alternately a shade lighter and darker so a tile has
    a direction -- which is what makes quarter-turned tiles read as tiles. No seam is drawn: a seam
    on two edges would double up or vanish where turned tiles meet."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-7, 7) + (3 if y % 2 == 0 else -3)
            px[x, y] = _shift(rgb, d)
    return img


def vct(rgb, rng, variant):
    """Vinyl composition tile: four 12-inch tiles to a block, each a slightly different shade (as a
    real floor's are), speckled with chips, with a hairline joint."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    shades = [rng.uniform(-8, 8) for _ in range(4)]
    for y in range(16):
        for x in range(16):
            t = (y // 8) * 2 + x // 8
            d = shades[t] + rng.uniform(-3, 3)
            r = rng.random()
            if r < 0.06:
                d += 22
            elif r < 0.12:
                d -= 22
            if x % 8 == 7 or y % 8 == 7:
                d -= 12
            px[x, y] = _shift(rgb, d)
    return img


def ceramic(rgb, rng, variant):
    """Ceramic floor tile: four to a block, a glazed face with a lit corner, and grout lines."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    grout = (128, 126, 120) if sum(rgb) > 450 else (96, 96, 94)
    shades = [rng.uniform(-5, 5) for _ in range(4)]
    for y in range(16):
        for x in range(16):
            if x % 8 == 7 or y % 8 == 7:
                px[x, y] = _shift(grout, rng.uniform(-3, 3))
                continue
            t = (y // 8) * 2 + x // 8
            d = shades[t] + rng.uniform(-2, 2)
            if x % 8 == 0 or y % 8 == 0:
                d += 8
            if x % 8 == 6 or y % 8 == 6:
                d -= 8
            px[x, y] = _shift(rgb, d)
    return img


def hardwood(rgb, rng, variant):
    """Strip hardwood: four boards across a block, running along u, each its own shade, with grain
    along it and an end joint at a different place in each board (variant 1 moves them)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for b in range(4):
        tone = rng.uniform(-14, 14)
        joint = (3 + b * 5 + variant * 7) % 16
        for y in range(b * 4, b * 4 + 4):
            grain = rng.uniform(-6, 6)
            for x in range(16):
                d = tone + grain + rng.uniform(-3, 3)
                if y == b * 4 + 3:
                    d -= 26  # the gap between boards
                if x == joint:
                    d -= 24
                px[x, y] = _shift(rgb, d)
    return img


def concrete(rgb, rng, variant):
    """Polished concrete: a soft mottle of the paste with the odd exposed fleck of aggregate, and a
    sheen."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    cells = [[rng.uniform(-8, 8) for _ in range(5)] for _ in range(5)]
    for y in range(16):
        for x in range(16):
            fx, fy = x / 4.0, y / 4.0
            i, j = int(fx), int(fy)
            u, v = fx - i, fy - j
            a = cells[j % 5][i % 5] * (1 - u) + cells[j % 5][(i + 1) % 5] * u
            c = cells[(j + 1) % 5][i % 5] * (1 - u) + cells[(j + 1) % 5][(i + 1) % 5] * u
            d = a * (1 - v) + c * v + rng.uniform(-2, 2)
            r = rng.random()
            if r < 0.05:
                d += 18
            elif r < 0.09:
                d -= 16
            px[x, y] = _shift(rgb, d)
    return img


def rubber(rgb, rng, variant):
    """Studded rubber: round raised studs on a four-pixel grid, lit from the top left."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rng.uniform(-2, 2))
    for cy in range(1, 16, 4):
        for cx in range(1, 16, 4):
            px[cx, cy] = _shift(rgb, 26)
            px[cx + 1, cy] = _shift(rgb, 14)
            px[cx, cy + 1] = _shift(rgb, 14)
            px[cx + 1, cy + 1] = _shift(rgb, -8)
    return img


def side_grain(rgb, rng, variant):
    """A hardwood block's side: the board edges, stacked like the flooring they are cut into."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    tone = 0.0
    for y in range(16):
        if y % 4 == 0:
            tone = rng.uniform(-10, 10)
        for x in range(16):
            d = tone + rng.uniform(-4, 4)
            if y % 4 == 3:
                d -= 24
            px[x, y] = _shift(rgb, d)
    return img


DRAW = {"carpet": carpet, "vct": vct, "ceramic": ceramic, "hardwood": hardwood,
        "concrete": concrete, "rubber": rubber}
# Drawings that get a second variant, so a big floor shows no repeat.
TWO = {"hardwood", "concrete", "vct", "ceramic"}


def textures():
    out = {}
    for name, (draw, rgb, _, _) in OVERLAYS.items():
        for v in range(2 if draw in TWO else 1):
            key = name if v == 0 else name + "_b"
            out[key] = DRAW[draw](rgb, random.Random(_seed(name, v)), v)
    for name, (draw, rgb, _) in SETS.items():
        rng = random.Random(_seed(name, 0))
        if draw == "hardwood":
            out[name + "_top"] = hardwood(rgb, rng, 0)
            out[name] = side_grain(rgb, random.Random(_seed(name, 1)), 0)
        else:
            out[name] = DRAW[draw](rgb, rng, 0)
    return out

# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------


def overlay_model(tex):
    """A one-pixel finish over the whole cell. Its underside is culled against the floor under
    it."""
    el = sc._box(0, 0, 0, 16, 1, 16, "#floor")
    el["faces"]["down"]["cullface"] = "down"
    return {"parent": "block/thin_block",
            "textures": {"floor": TEX_REF % tex, "particle": TEX_REF % tex},
            "elements": [el]}


def models():
    out = {}
    for name, (draw, _, _, _) in OVERLAYS.items():
        out[name] = overlay_model(name)
        if draw in TWO:
            out[name + "_b"] = overlay_model(name + "_b")
    return out


def overlay_state(name):
    draw, _, turns, _ = OVERLAYS[name]
    models_ = [name] + ([name + "_b"] if draw in TWO else [])

    def pick(rotations):
        out = []
        for m in models_:
            for r in rotations:
                v = {"model": MODEL_REF % m}
                if r:
                    v["y"] = r
                out.append(v)
        return out

    if turns == "axis":
        # Boards run along the block's axis: along x drawn as they are, along z a quarter turn.
        x, z = pick((0, 180)), pick((90, 270))
    elif turns == "none":
        x = z = pick((0,))
    else:
        x = z = pick((0, 90, 180, 270))
    return {"variants": {"axis=x": x, "axis=z": z,
                         "inventory": {"model": MODEL_REF % name}}}


def blockstates():
    return {name: overlay_state(name) for name in OVERLAYS}

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
    # The sets: gen_cmu's five blockstates and six models each.
    set_model_dir = os.path.join(os.path.dirname(model_dir), "buildingmaterials")
    for name, (draw, _, _) in SETS.items():
        suffix = "_top" if draw == "hardwood" else ""
        for state, body in sorted(gen_cmu.blockstates(name, TEX_REF, top_suffix=suffix).items()):
            gen_cmu._write_json(os.path.join(state_dir, state + ".json"), body)
            written.append(("state", state + ".json"))
        for model, body in sorted(gen_cmu.models(name, TEX_REF, top_suffix=suffix).items()):
            gen_cmu._write_json(os.path.join(set_model_dir, model + ".json"), body)
            written.append(("setmodel", model + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    out = [("tile.%s.name" % name, dict(zip(LANGS, names)))
           for name, (_, _, _, names) in OVERLAYS.items()]
    for name, (_, _, names) in SETS.items():
        base = dict(zip(LANGS, names))
        for suffix, _ in gen_cmu.VARIANTS:
            out.append(("tile.%s%s.name" % (name, suffix),
                        {lang: (base[lang] if not suffix
                                else "%s %s" % (base[lang], gen_cmu.VARIANT_WORDS[lang][suffix]))
                         for lang in LANGS}))
    return out


def tab_lines():
    lines = []
    for name, (_, _, _, names) in OVERLAYS.items():
        lines.append('    initTabBlock(new BlockFloorFinish("%s")); // %s' % (name, names[0]))
    for name, (_, _, names) in SETS.items():
        lines.append("    initTabBlock(%s.class,\n        fmlPreInitializationEvent); // %s Set "
                     "(Block, Fence, Slab, Stairs)" % (gen_cmu.class_name(name), names[0]))
    return lines


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabInteriorFinishes")
    lines += tab_lines()
    return "\n".join(lines)


def _check():
    import filecmp
    import shutil
    import tempfile
    tmp = tempfile.mkdtemp(prefix="csm_flooring_")
    try:
        roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR,
                 "setmodel": os.path.join(os.path.dirname(MODEL_DIR), "buildingmaterials")}
        t = {k: os.path.join(tmp, k) for k in ("tex", "model", "state")}
        t["setmodel"] = os.path.join(tmp, "buildingmaterials")
        for d in t.values():
            os.makedirs(d, exist_ok=True)
        written = write_all(t["tex"], t["model"], t["state"])
        drifted = [os.path.join(roots[k], f) for k, f in written
                   if not os.path.exists(os.path.join(roots[k], f))
                   or not filecmp.cmp(os.path.join(roots[k], f), os.path.join(t[k], f),
                                      shallow=False)]
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + os.path.relpath(path, REPO))
            return 1
        print("%d generated flooring files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang and tab-registration lines and exit")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if args.check:
        return _check()
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d flooring files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
