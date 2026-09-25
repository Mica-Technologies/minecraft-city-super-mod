"""Generates the fare vending machine's model, textures and blockstate (Transit module).

The machine is a free-standing ticket vending machine, two blocks tall, drawn by one block (so the
metadata and every machine already placed stay as they were): a plinth, a cabinet in the invented
CITYLINE livery (teal, a yellow accent), a lit TICKETS header, and on its front, in relief, a hooded
touchscreen, a keypad and a tap-to-pay pad, card and cash slots, and a ticket and change tray.
It stands against the south edge of its block, facing north, as the old flat panel did.

Textures are drawn at 4 texels a model unit and mapped 1:1 (each face's UV covers exactly the
texels drawn for it), so nothing is stretched. The model turns ambient occlusion off: the upper
half sits in the air above its block and was shaded by the lower block's neighbours.

    python gen_transit_fare_vending.py            # write
    python gen_transit_fare_vending.py --check    # fail if the tree differs
"""
import argparse
import json
import os
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lsc  # noqa: E402
import model_depth  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")
SUB = "transit/fare_vending"
TEX = "csm:blocks/%s/%%s" % SUB
PX = 4  # texels per model unit

TEAL = (14, 94, 111)
TEAL_DARK = (9, 62, 74)
YELLOW = (244, 196, 40)
WHITE = (236, 240, 240)
GRAPHITE = (46, 50, 54)
STEEL = (150, 156, 160)
SCREEN = (22, 34, 44)
SCREEN_LIT = (206, 232, 240)
GREEN = (60, 200, 110)

TEXTURES = {}


def texture(name):
    def reg(fn):
        TEXTURES[name] = fn
        return fn
    return reg


def canvas(colour=TEAL, seed=1):
    return lsc.fill(colour, size=64, grain=3, seed=seed)


# Face art is drawn in a (w * PX) x (h * PX) window at the top-left of a 64 x 64 sheet and the
# face's uv is that window, so a texel is a quarter of a model unit everywhere.
def win(w, h):
    return [0, 0, round(w * PX / 4.0, 4), round(h * PX / 4.0, 4)]


@texture("front_upper")
def front_upper():
    """The upper front, 12 x 13 units: the screen under its hood, the keypad and tap pad below
    it (drawn in relief as boxes; their labels here)."""
    img = canvas(TEAL, 11)
    w, h = 12 * PX, 13 * PX
    # screen: bezel, then the lit display with a fare menu
    lsc.rect(img, 5, 6, w - 5, 34, GRAPHITE)
    lsc.rect(img, 7, 8, w - 7, 32, SCREEN)
    lsc.rect(img, 8, 9, w - 8, 14, TEAL)
    lsc.rect(img, 9, 10, 12, 13, YELLOW)
    lsc.draw_text_centred(img, "FARES", w / 2.0 + 1, 15, SCREEN_LIT)
    for i, c in enumerate(((66, 160, 220), (90, 190, 120), (230, 170, 60))):
        y = 21 + i * 4
        lsc.rect(img, 9, y, w - 9, y + 3, c)
        lsc.rect(img, 10, y + 1, 18, y + 2, lsc.shade(c, 1.4))
    # a yellow accent line under the screen, the labels over the keypad and tap pad
    lsc.rect(img, 2, 36, w - 2, 37, YELLOW)
    lsc.draw_text(img, "PIN", 9, 39, WHITE)
    lsc.draw_text(img, "TAP", 30, 39, WHITE)
    lsc.rect(img, 0, h - 1, w, h, TEAL_DARK)
    return img


@texture("front_lower")
def front_lower():
    """The lower front, 12 x 13 units: cash and card slot labels, the coin slot, the tray's
    label and the kick plate."""
    img = canvas(TEAL, 12)
    w, h = 12 * PX, 13 * PX
    lsc.rect(img, 0, 0, w, 1, TEAL_DARK)
    lsc.draw_text(img, "CASH", 6, 4, WHITE)
    lsc.draw_text(img, "CARD", 27, 4, WHITE)
    # coin slot under the cash slot's bezel
    lsc.rect(img, 11, 18, 17, 22, GRAPHITE)
    lsc.rect(img, 13, 19, 15, 21, (10, 10, 12))
    lsc.draw_text(img, "COINS", 6, 23, lsc.shade(WHITE, 0.85))
    # card symbols: generic, no network marks
    for i, c in enumerate(((200, 60, 60), (60, 110, 200), (230, 180, 40))):
        lsc.rect(img, 27 + i * 5, 18, 31 + i * 5, 21, c)
    lsc.draw_text_centred(img, "COLLECT", w / 2.0, 29, YELLOW)
    lsc.rect(img, 0, h - 6, w, h, GRAPHITE)
    lsc.rect(img, 0, h - 7, w, h - 6, YELLOW)
    return img


@texture("side")
def side():
    """The cabinet's side, 8.5 x 13 units (upper and lower halves share it): pale panel, a teal
    band at the front edge and a vent grille."""
    img = canvas((206, 212, 214), 13)
    w, h = int(8.5 * PX), 13 * PX
    lsc.rect(img, w - 5, 0, w, h, TEAL)
    lsc.rect(img, w - 6, 0, w - 5, h, YELLOW)
    for y in range(10, 30, 3):
        lsc.rect(img, 6, y, w - 12, y + 1, lsc.shade((206, 212, 214), 0.62))
    return img


@texture("header")
def header():
    """The lit header, 14 x 3 units: TICKETS in teal on white."""
    img = canvas(WHITE, 14)
    w, h = 14 * PX, 3 * PX
    lsc.draw_text_centred(img, "TICKETS", w / 2.0, 1, TEAL, scale=2)
    lsc.rect(img, 0, h - 1, w, h, lsc.shade(WHITE, 0.8))
    return img


@texture("trim")
def trim():
    return lsc.fill(GRAPHITE, size=64, grain=3, seed=15)


@texture("steel")
def steel():
    return lsc.fill(STEEL, size=64, grain=5, seed=16)


@texture("keypad")
def keypad():
    """3.5 x 2.5 units: a 3 x 4 grid of steel keys on graphite."""
    img = lsc.fill(GRAPHITE, size=64, grain=2, seed=17)
    for r in range(3):
        for c in range(4):
            lsc.rect(img, 1 + c * 3, 1 + r * 3, 3 + c * 3, 3 + r * 3, STEEL)
    return img


@texture("tap")
def tap():
    """3.5 x 2.5 units: a dark pad with the contactless waves and a green light bar."""
    img = lsc.fill((24, 28, 32), size=64, grain=2, seed=18)
    for i, r in enumerate((2, 3, 4)):
        for y in range(10):
            for x in range(14):
                d = ((x + 0.5 - 4) ** 2 + (y + 0.5 - 5) ** 2) ** 0.5
                if r - 0.5 <= d < r and x >= 4:
                    img.putpixel((x, y), WHITE + (255,))
    lsc.rect(img, 1, 8, 13, 9, GREEN)
    return img


@texture("slot")
def slot():
    """A slot bezel's face: steel with a black slot across it."""
    img = lsc.fill(STEEL, size=64, grain=3, seed=19)
    lsc.rect(img, 1, 1, 13, 3, (8, 8, 10))
    return img


@texture("tray")
def tray():
    """The tray's inside, seen from above: dark, a lit edge at the back."""
    img = lsc.fill((20, 22, 24), size=64, grain=2, seed=20)
    lsc.rect(img, 0, 0, 24, 1, lsc.shade(STEEL, 0.9))
    return img


def face(tex, uv):
    return {"uv": [round(v, 4) for v in uv], "texture": "#" + tex}


def box(frm, to, faces):
    return {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}


def auto(frm, to, tex, which):
    """Faces whose uv is the box's own coordinates (fine for plain panels within 0..16)."""
    out = {}
    for f in which:
        if f in ("north", "south"):
            u = [frm[0], 16 - min(to[1], 16), to[0], 16 - min(frm[1], 16)]
        elif f in ("east", "west"):
            u = [frm[2], 16 - min(to[1], 16), to[2], 16 - min(frm[1], 16)]
        else:
            u = [frm[0], frm[2], to[0], to[2]]
        out[f] = face(tex, u)
    return out


def elements():
    els = []
    # plinth
    els.append(box([1.5, 0, 7], [14.5, 1, 15.8],
                   auto([1.5, 0, 7], [14.5, 1, 15.8], "trim", ("north", "east", "west", "up"))))
    # cabinet, lower and upper halves (each front a 12 x 13 window)
    for lo, hi, front in ((1, 14, "front_lower"), (14, 27, "front_upper")):
        f = {"north": face(front, win(12, 13)),
             "east": face("side", win(8.5, 13)),
             "west": face("side", [8.5 * PX / 4.0, 0, 0, 13 * PX / 4.0]),
             "south": face("trim", [2, 0, 14, 13])}
        els.append(box([2, lo, 7.5], [14, hi, 16], f))
    # header, lit sign on its front
    els.append(box([1, 27, 7], [15, 30, 16],
                   {"north": face("header", win(14, 3)),
                    "east": face("trim", [7, 0, 16, 3]), "west": face("trim", [7, 0, 16, 3]),
                    "up": face("trim", [1, 7, 15, 16]), "down": face("trim", [1, 7, 15, 7.5]),
                    "south": face("trim", [1, 0, 15, 3])}))
    # the screen's hood
    els.append(box([3.5, 24.2, 6.4], [12.5, 24.8, 7.5],
                   auto([3.5, 8.2, 6.4], [12.5, 8.8, 7.5], "trim",
                        ("north", "east", "west", "up", "down"))))
    # keypad and tap pad, standing out under the screen
    els.append(box([8.8, 15.2, 6.9], [12.3, 17.7, 7.5],
                   {"north": face("keypad", win(3.5, 2.5)),
                    "east": face("trim", [0, 0, 0.6, 2.5]), "west": face("trim", [0, 0, 0.6, 2.5]),
                    "up": face("trim", [0, 0, 3.5, 0.6]), "down": face("trim", [0, 0, 3.5, 0.6])}))
    els.append(box([3.7, 15.2, 6.7], [7.2, 17.7, 7.5],
                   {"north": face("tap", win(3.5, 2.5)),
                    "east": face("trim", [0, 0, 0.8, 2.5]), "west": face("trim", [0, 0, 0.8, 2.5]),
                    "up": face("trim", [0, 0, 3.5, 0.8]), "down": face("trim", [0, 0, 3.5, 0.8])}))
    # cash slot and card slot bezels
    for x0, x1 in ((9.2, 12.8), (3.2, 6.8)):
        els.append(box([x0, 10.8, 7.0], [x1, 11.8, 7.5],
                       {"north": face("slot", win(x1 - x0, 1)),
                        "east": face("steel", [0, 0, 0.5, 1]), "west": face("steel", [0, 0, 0.5, 1]),
                        "up": face("steel", [0, 0, x1 - x0, 0.5]),
                        "down": face("steel", [0, 0, x1 - x0, 0.5])}))
    # ticket and change tray: a lip standing out, its dark inside seen from above
    els.append(box([4.5, 3.2, 6.2], [11.5, 5.6, 7.5],
                   {"north": face("steel", [0, 0, 7, 2.4]),
                    "east": face("steel", [0, 0, 1.3, 2.4]), "west": face("steel", [0, 0, 1.3, 2.4]),
                    "up": face("tray", win(7, 1.3)), "down": face("steel", [0, 0, 7, 1.3])}))
    return els


MODEL_NAME = "fare_vending"


def model():
    tex = {k: TEX % k for k in TEXTURES}
    tex["particle"] = TEX % "front_upper"
    return {
        "ambientocclusion": False,
        "textures": tex,
        "elements": elements(),
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.36, 0.36, 0.36]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [0.25, 0.25, 0.25]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                      "scale": [0.28, 0.28, 0.28]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                     "scale": [0.28, 0.28, 0.28]},
        },
    }


def blockstate():
    return {
        "forge_marker": 1,
        "defaults": {"model": "csm:%s/%s" % (SUB, MODEL_NAME)},
        "variants": {
            "facing": {"down": {"x": 90}, "east": {"y": 90}, "north": {}, "south": {"y": 180},
                       "up": {"x": 270}, "west": {"y": 270}},
            "inventory": [{}],
            "normal": [{}],
        },
    }


def generate(assets):
    written = []
    for name, draw in sorted(TEXTURES.items()):
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    rel = "models/block/%s/%s.json" % (SUB, MODEL_NAME)
    lsc.dump(os.path.join(assets, rel), model())
    written.append(rel)
    rel = "blockstates/farevend.json"
    lsc.dump(os.path.join(assets, rel), blockstate())
    written.append(rel)
    model_depth.separate(assets, written, lsc.dump)
    return written


def same(a, b):
    if not os.path.exists(b):
        return False
    if a.endswith(".png"):
        ia, ib = Image.open(a).convert("RGBA"), Image.open(b).convert("RGBA")
        return ia.size == ib.size and ia.tobytes() == ib.tobytes()
    return open(a, "rb").read().replace(b"\r\n", b"\n") == open(b, "rb").read().replace(b"\r\n", b"\n")


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="farevend_")
    try:
        written = generate(tmp)
        stale = [r for r in written if not same(os.path.join(tmp, r), os.path.join(ASSETS, r))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("fare vending machine is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
