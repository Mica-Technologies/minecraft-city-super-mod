#!/usr/bin/env python3
"""Every asset the glazing windows ship: the framed windows that extend the Glazing set.

    python dev-env-utils/scripts/gen_glazing_windows.py
    python dev-env-utils/scripts/gen_glazing_windows.py --check
    python dev-env-utils/scripts/gen_glazing_windows.py --fragments   # lang and tab lines

Seven frame finishes -- dark bronze, painted white, painted black, oak, walnut, dark oak,
aluminium -- each as a straight window (``glass_window_<finish>``) and a 45-degree canted bay
corner (``glass_window_<finish>_canted``): fourteen blocks, one class (BlockGlazingWindow).

What this writes is only what the game cannot do without: the frame and stone textures, each
block's base model (no elements, only the particle texture: every face of a window is drawn by
GlazingWindowBakedModel from WindowLayout, because what a window draws depends on its run, its
pattern, its depth and whether it is open, far past what a blockstate can enumerate), the
twelve-variant blockstates (facing x depth, all one model), the inventory icons and their item
models. The glass is Glazing's own clear glass (gen_glazing.py), so a window's glass is the
same glass as a pane's.

The texture names matter: a face is drawn in the translucent pass if its texture is named as
glass (CsmGlassLayer.isGlass), so the frame and stone textures are named ``window_frame_*`` and
``window_stone``, never ``glass_*``.

SHARED with WindowLayout.java: the icon's frame and meeting rail are drawn to the same
proportions as the double-hung window, so the slot reads as a window.
"""

import argparse
import os
import random
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc
import model_depth  # noqa: E402

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "glazing")
ITEM_TEX_DIR = os.path.join(MODULE, "textures", "items", "glazing")
MODEL_DIR = os.path.join(MODULE, "models", "block", "glazing")
ITEM_MODEL_DIR = os.path.join(MODULE, "models", "item")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/glazing/%s"
ITEM_TEX_REF = "csm:items/glazing/%s"
MODEL_REF = "csm:glazing/%s"

# finish -> (kind, base colour, seed, name in en_us, es_es, de_de, sv_se). Order is creative order.
FINISHES = {
    "bronze": ("bronze", (58, 48, 38), 20261661,
               ("Dark Bronze", "Bronce Oscuro", "Dunkelbronze", "Mörk Brons")),
    "white": ("paint", (232, 232, 226), 20261662,
              ("Painted White", "Pintada de Blanco", "Weiß lackiert", "Vitmålat")),
    "black": ("paint", (36, 37, 40), 20261663,
              ("Painted Black", "Pintada de Negro", "Schwarz lackiert", "Svartmålat")),
    "oak": ("wood", (178, 140, 88), 20261664, ("Oak", "Roble", "Eiche", "Ek")),
    "walnut": ("wood", (98, 66, 44), 20261665, ("Walnut", "Nogal", "Walnuss", "Valnöt")),
    "darkoak": ("wood", (68, 46, 28), 20261666,
                ("Dark Oak", "Roble Oscuro", "Dunkle Eiche", "Mörk Ek")),
    "aluminium": ("metal", (176, 180, 184), 20261667,
                  ("Aluminium", "Aluminio", "Aluminium", "Aluminium")),
}
STONE = (206, 198, 182)
GLASS_ICON = (196, 222, 232, 150)

WINDOW_NAMES = ("Window (%s)", "Ventana (%s)", "Fenster (%s)", "Fönster (%s)")
CANTED_NAMES = ("Canted Bay Window (%s)", "Ventana de Mirador Achaflanada (%s)",
                "Abgeschrägtes Erkerfenster (%s)", "Avfasat Burspråksfönster (%s)")

BLOCKS = {}
for _f, (_k, _rgb, _seed, _names) in FINISHES.items():
    BLOCKS["glass_window_" + _f] = tuple(t % n for t, n in zip(WINDOW_NAMES, _names))
    BLOCKS["glass_window_%s_canted" % _f] = tuple(t % n for t, n in zip(CANTED_NAMES, _names))

DEPTHS = ("centred", "flush", "inset")
FACINGS = (("south", 180), ("west", 270), ("north", 0), ("east", 90))

# The status lines and tooltip, keyed under csm.building.window.
MESSAGES = {
    "tooltip.click": ("Right-click: pattern; on the bottom rail: open or close; on the top: arch",
                      "Clic derecho: patrón; en el travesaño inferior: abrir o cerrar; arriba: arco",
                      "Rechtsklick: Teilung; unten am Rahmen: öffnen oder schließen; oben: Bogen",
                      "Högerklick: spröjsning; på nederkanten: öppna eller stäng; upptill: båge"),
    "tooltip.sneak": ("Sneak + right-click: depth; on the bottom rail: sill; on the top: head",
                      "Agacharse + clic derecho: profundidad; abajo: alféizar; arriba: dintel",
                      "Schleichen + Rechtsklick: Tiefe; unten: Fensterbank; oben: Sturz",
                      "Smyg + högerklick: djup; nedtill: fönsterbleck; upptill: överstycke"),
    "pattern.0": ("Plain", "Fijo", "Festverglasung", "Fast"),
    "pattern.1": ("Double-hung, one over one", "Guillotina, uno sobre uno",
                  "Schiebefenster, eins über eins", "Skjutfönster, en över en"),
    "pattern.2": ("Double-hung, six over six", "Guillotina, seis sobre seis",
                  "Schiebefenster, sechs über sechs", "Skjutfönster, sex över sex"),
    "pattern.3": ("Casement pair", "Batientes dobles", "Zweiflügelig", "Tvåluftsfönster"),
    "depth.0": ("Depth: centred", "Profundidad: centrada", "Tiefe: mittig", "Djup: mitten"),
    "depth.1": ("Depth: flush with the outside", "Profundidad: a ras del exterior",
                "Tiefe: bündig außen", "Djup: i liv med utsidan"),
    "depth.2": ("Depth: inset, deep reveal", "Profundidad: retranqueada",
                "Tiefe: zurückgesetzt, tiefe Laibung", "Djup: indraget, djup smyg"),
    "open": ("Opened", "Abierta", "Geöffnet", "Öppnat"),
    "closed": ("Closed", "Cerrada", "Geschlossen", "Stängt"),
    "fixed": ("A plain window does not open", "Una ventana fija no se abre",
              "Festverglasung lässt sich nicht öffnen", "Ett fast fönster går inte att öppna"),
    "arch.on": ("Arched top", "Remate en arco", "Rundbogen", "Rundbåge"),
    "arch.off": ("Square top", "Remate recto", "Gerader Abschluss", "Rak överkant"),
    "sill.0": ("Sill: none", "Alféizar: ninguno", "Fensterbank: keine", "Fönsterbleck: inget"),
    "sill.1": ("Sill: stone", "Alféizar: piedra", "Fensterbank: Stein", "Fönsterbleck: sten"),
    "sill.2": ("Sill: frame", "Alféizar: marco", "Fensterbank: Rahmen", "Fönsterbleck: karm"),
    "head.0": ("Head: none", "Dintel: ninguno", "Sturz: keiner", "Överstycke: inget"),
    "head.1": ("Head: flat lintel", "Dintel: plano", "Sturz: gerade", "Överstycke: rakt"),
    "head.2": ("Head: keystone lintel", "Dintel: con clave", "Sturz: mit Schlussstein",
               "Överstycke: med slutsten"),
}


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _clamp(v):
    return max(0, min(255, int(round(v))))


def _shift(rgb, d):
    return tuple(_clamp(c + d) for c in rgb) + (255,)


def wood(rgb, seed):
    """Frame timber: grain along x, each row its own shade, a few darker streaks. Fine grain
    mipmaps to the base colour a few blocks off, which is what a painted-looking frame should
    not do, so the rows differ by more than the pixels do."""
    rng = random.Random(seed)
    rows = [rng.uniform(-9, 9) for _ in range(16)]
    for _ in range(3):
        rows[rng.randrange(16)] -= 14
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rows[y] + rng.uniform(-3, 3))
    return img


def paint(rgb, seed):
    """Painted timber: nearly flat, so it reads as paint and not as plastic."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rng.uniform(-3, 3))
    return img


def metal(rgb, seed):
    """Anodised aluminium: a brushed streak along x."""
    rng = random.Random(seed)
    rows = [rng.uniform(-6, 6) for _ in range(16)]
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rows[y] * 0.6 + rng.uniform(-2, 2))
    return img


def bronze(rgb, seed):
    """Dark bronze, drawn as the panes' frame is (gen_glazing.frame_texture), so a bronze
    window and a pane's frame are one colour."""
    return gen_logistics._canvas(seed, rgb, 4)[0]


def stone(seed):
    """Limestone for sills and lintels: pale, with a little speckle."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-6, 6)
            if rng.random() < 0.06:
                d -= 18
            px[x, y] = _shift(STONE, d)
    return img


MAKERS = {"wood": wood, "paint": paint, "metal": metal, "bronze": bronze}


def textures():
    out = {"window_stone": stone(20261660)}
    for f, (kind, rgb, seed, _) in FINISHES.items():
        out["window_frame_" + f] = MAKERS[kind](rgb, seed)
    return out


def icon(frame, canted):
    """A window in its frame with a meeting rail; the canted corner's glass is shaded on one
    side, as a pane turned 45 degrees away is."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    fr = frame.load()
    for y in range(1, 15):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (1, 14) or y in (7, 8)
            if edge:
                px[x, y] = fr[x, y]
            else:
                g = GLASS_ICON
                if canted and x < 8:
                    g = (g[0] - 40, g[1] - 40, g[2] - 40, g[3])
                px[x, y] = g
    return img


def icons(tex):
    out = {}
    for f in FINISHES:
        frame = tex["window_frame_" + f]
        out["glass_window_" + f] = icon(frame, False)
        out["glass_window_%s_canted" % f] = icon(frame, True)
    return out


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------

def models():
    """One base model per finish: no elements, the frame for the breaking particles."""
    return {"window_base_" + f: {"textures": {"particle": TEX_REF % ("window_frame_" + f)},
                                 "elements": []}
            for f in FINISHES}


def item_models():
    return {name: {"parent": "item/generated", "textures": {"layer0": ITEM_TEX_REF % name}}
            for name in BLOCKS}


def finish_of(name):
    return name[len("glass_window_"):].replace("_canted", "")


def blockstates():
    """Twelve variants, facing by depth, all the one base model: the baked model reads the
    facing from the state and turns its quads itself."""
    out = {}
    for name in BLOCKS:
        variants = {}
        for depth in DEPTHS:
            for facing, _ in FACINGS:
                variants["depth=%s,facing=%s" % (depth, facing)] = {
                    "model": MODEL_REF % ("window_base_" + finish_of(name))}
        out[name] = {"variants": variants}
    return out


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir, item_tex_dir, item_model_dir):
    written = []
    for d in (tex_dir, item_tex_dir):
        os.makedirs(d, exist_ok=True)
    tex = textures()
    for name, img in sorted(tex.items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, img in sorted(icons(tex).items()):
        img.save(os.path.join(item_tex_dir, name + ".png"))
        written.append(("itemtex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(item_models().items()):
        gen_cmu._write_json(os.path.join(item_model_dir, name + ".json"), body)
        written.append(("itemmodel", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    model_depth.separate_dirs([(tex_dir, TEX_DIR), (model_dir, MODEL_DIR),
                               (state_dir, STATE_DIR), (item_tex_dir, ITEM_TEX_DIR),
                               (item_model_dir, ITEM_MODEL_DIR)], written, gen_cmu._write_json)
    return written


LANGS = gen_cmu.LANGS   # en_us, es_es, de_de, sv_se: the order every name tuple above uses


def lang_entries():
    out = [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, names in BLOCKS.items()]
    out += [("csm.building.window." + key, dict(zip(LANGS, names)))
            for key, names in MESSAGES.items()]
    return out


def tab_lines():
    return ['    initTabBlock(new BlockGlazingWindow("%s")); // %s' % (name, names[0])
            for name, names in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("## CsmTabBuildingMaterials, after the glazing lines")
    lines += tab_lines()
    return "\n".join(lines)


def main():
    import filecmp
    import shutil
    import tempfile
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang and tab lines and exit")
    args = ap.parse_args()
    if args.fragments:
        sys.stdout.reconfigure(encoding="utf-8")
        print(fragments())
        return 0
    roots = {"tex": TEX_DIR, "itemtex": ITEM_TEX_DIR, "model": MODEL_DIR,
             "itemmodel": ITEM_MODEL_DIR, "state": STATE_DIR}
    order = ("tex", "model", "state", "itemtex", "itemmodel")
    if not args.check:
        written = write_all(*(roots[k] for k in order))
        print("Wrote %d glazing window files" % len(written))
        return 0
    tmp = tempfile.mkdtemp(prefix="csm_glazing_windows_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(*(tmp_roots[k] for k in order))
        drifted = [os.path.relpath(os.path.join(roots[kind], f), REPO)
                   for kind, f in written
                   if not os.path.exists(os.path.join(roots[kind], f))
                   or not filecmp.cmp(os.path.join(roots[kind], f),
                                      os.path.join(tmp_roots[kind], f), shallow=False)]
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated glazing window files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
