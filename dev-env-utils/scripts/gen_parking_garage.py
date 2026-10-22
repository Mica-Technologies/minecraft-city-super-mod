#!/usr/bin/env python3
"""Every asset the parking garage pieces ship (Roads, Streetscape tab; #302).

    python dev-env-utils/scripts/gen_parking_garage.py
    python dev-env-utils/scripts/gen_parking_garage.py --check
    python dev-env-utils/scripts/gen_parking_garage.py --fragments   # lang and tab lines

- **Gate arm cabinet** (BlockParkingGate): a yellow cabinet with a black-and-yellow band and a
  grey cap. Its arm is TileEntityParkingGateRenderer's, pivoting on the cabinet's side at the
  numbers that renderer keeps (PIVOT_ALONG, PIVOT_UP): 4.5 px in from the arm's side, 14 px up.
- **Ticket dispenser** and **exit pay kiosk** (BlockParkingMachine): a head at a car window's
  height (up to 26 px) on a pedestal, in the pay station's blue, the front lettered TICKET /
  PUSH or PAY HERE / INSERT TICKET, with a lit button or card slot.
- **Parking ticket** (ItemParkingTicket): its sprite.

Every model faces north (the front toward -z); Forge blockstates turn it to the facing.
"""

import argparse
import io
import json
import os
import random
import sys

from PIL import Image

import life_safety_gen_common as lc

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROADS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(ROADS, "textures", "blocks", "streetscape", "parking_garage")
ITEM_TEX = os.path.join(ROADS, "textures", "items", "parking_ticket.png")
MODEL_DIR = os.path.join(ROADS, "models", "block", "streetscape", "parking_garage")
ITEM_DIR = os.path.join(ROADS, "models", "item")
STATE_DIR = os.path.join(ROADS, "blockstates")
TEX_REF = "csm:blocks/streetscape/parking_garage/%s"
MODEL_REF = "csm:streetscape/parking_garage/%s"

YELLOW = (226, 178, 32)
BLACK = (26, 26, 28)
GREY = (120, 124, 128)
BLUE = (34, 72, 128)
WHITE = (240, 240, 236)


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def grain(colour, seed, size=16, amp=3):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = clamp([v + rng.uniform(-amp, amp) for v in colour])
    return img


def hazard():
    """The cabinet's band: black and yellow diagonal stripes."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = clamp(YELLOW if ((x + y) // 4) % 2 == 0 else BLACK)
    return img


def machine_face(top, bottom, lamp):
    """A machine's front, 32 px: a screen, two lines of white lettering, and a lit button (green)
    or card slot (amber)."""
    img = grain(BLUE, 4701 if lamp == "green" else 4702, 32, 2)
    px = img.load()
    for y in range(3, 11):
        for x in range(4, 28):
            px[x, y] = clamp((20, 32, 26) if y in (3, 10) or x in (4, 27) else (40, 120, 70))
    lc.draw_text_centred(img, top, 16, 13, WHITE)
    lc.draw_text_centred(img, bottom, 16, 19, WHITE)
    c = (60, 200, 90) if lamp == "green" else (240, 170, 40)
    for y in range(25, 30):
        for x in range(12, 20):
            px[x, y] = clamp(c if lamp == "green" else ((20, 20, 22) if y == 27 else c))
    return img


def ticket_sprite():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 12):
        for x in range(2, 14):
            px[x, y] = clamp(WHITE)
    for x in range(2, 14):
        px[x, 4] = clamp((200, 60, 50))
    for x in (4, 5, 7, 9, 10, 12):
        for y in (8, 9, 10):
            px[x, y] = clamp(BLACK)
    for x in range(4, 11):
        px[x, 6] = clamp((90, 90, 96))
    return img


TEXTURES = {
    "cabinet": lambda: grain(YELLOW, 4703),
    "cabinet_band": hazard,
    "cap": lambda: grain(GREY, 4704),
    "machine": lambda: grain(BLUE, 4705),
    "pedestal": lambda: grain((70, 74, 80), 4706),
    "dispenser_face": lambda: machine_face("TICKET", "PUSH", "green"),
    "kiosk_face": lambda: machine_face("PAY HERE", "TICKET", "amber"),
}


# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

ALL = ("north", "south", "east", "west", "up", "down")


def box(frm, to, tex, faces=ALL, per=None, uv=None):
    (x0, y0, z0), (x1, y1, z1) = frm, to

    def wrap(v):
        return v - 16 if v > 16 else v
    uvs = {"north": [16 - x1, 16 - wrap(y1), 16 - x0, 16 - wrap(y0)],
           "south": [x0, 16 - wrap(y1), x1, 16 - wrap(y0)],
           "east": [16 - z1, 16 - wrap(y1), 16 - z0, 16 - wrap(y0)],
           "west": [z0, 16 - wrap(y1), z1, 16 - wrap(y0)],
           "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    faces_out = {}
    for f in faces:
        u = (uv or {}).get(f) or [round(min(16, max(0, v)), 3) for v in uvs[f]]
        faces_out[f] = {"texture": "#" + (per or {}).get(f, tex), "uv": u}
    return {"from": list(frm), "to": list(to), "faces": faces_out}


def cabinet():
    return [box((3, 0, 4), (13, 12, 12), "cabinet"),
            box((3, 12, 4), (13, 14.5, 12), "cabinet_band"),
            box((2.5, 14.5, 3.5), (13.5, 15.5, 12.5), "cap")]


def machine(face):
    """A pedestal and a head at a car window's height; the head's front (north) is the face.
    The head spans y 16..26, so it is drawn past the block's top with the face's uv set
    whole."""
    return [box((6, 0, 6), (10, 16, 10), "pedestal", ("north", "south", "east", "west")),
            box((5, 0, 5), (11, 0.6, 11), "pedestal"),
            box((4, 16, 4), (12, 26, 12), "machine", per={"north": face},
                uv={"north": [0, 0, 16, 16], "south": [0, 3, 16, 13], "east": [0, 3, 16, 13],
                    "west": [0, 3, 16, 13]}),
            box((3.5, 26, 3.5), (12.5, 27, 12.5), "cap")]


def model(textures, elements, display=None):
    m = {"parent": "block/block", "textures": textures, "elements": elements}
    if display:
        m["display"] = display
    return m


DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0],
                   "scale": [0.55] * 3},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
           "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                     "scale": [0.375] * 3},
           "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                     "scale": [0.4] * 3},
           "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                    "scale": [0.4] * 3}}


def facing_state(model_name, extra=None):
    variants = {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                           "west": {"y": 270}},
                "inventory": [{}]}
    variants.update(extra or {})
    return {"forge_marker": 1, "defaults": {"model": MODEL_REF % model_name},
            "variants": variants}


BLOCKS = {
    # registry: (model, elements, textures, java, names en/es/de/sv, extra variants)
    "parking_gate_arm": (
        "gate_cabinet", cabinet,
        {"cabinet": "cabinet", "cabinet_band": "cabinet_band", "cap": "cap"},
        "new BlockParkingGate()",
        ("Parking Gate Arm", "Barrera de Aparcamiento", "Parkhausschranke",
         "Parkeringsbom"),
        {"open": {"true": {}, "false": {}}}),
    "parking_ticket_dispenser": (
        "ticket_dispenser", lambda: machine("face"),
        {"pedestal": "pedestal", "machine": "machine", "face": "dispenser_face", "cap": "cap"},
        'new BlockParkingMachine("parking_ticket_dispenser", false)',
        ("Parking Ticket Dispenser", "Expendedor de Tickets de Aparcamiento",
         "Parkticketautomat", "Parkeringsbiljettautomat"), None),
    "parking_exit_kiosk": (
        "exit_kiosk", lambda: machine("face"),
        {"pedestal": "pedestal", "machine": "machine", "face": "kiosk_face", "cap": "cap"},
        'new BlockParkingMachine("parking_exit_kiosk", true)',
        ("Parking Exit Pay Kiosk", "Cajero de Salida de Aparcamiento",
         "Parkhaus-Ausfahrtskasse", "Betalautomat vid Parkeringsutfart"), None),
}
TICKET_NAMES = ("Parking Ticket", "Ticket de Aparcamiento", "Parkticket", "Parkeringsbiljett")


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2) + "\n").encode()


def outputs():
    out = {os.path.join(TEX_DIR, n + ".png"): png(f()) for n, f in TEXTURES.items()}
    out[ITEM_TEX] = png(ticket_sprite())
    out[os.path.join(ITEM_DIR, "parking_ticket.json")] = js(
        {"parent": "item/generated", "textures": {"layer0": "csm:items/parking_ticket"}})
    for reg, (mname, els, tex, _, _, extra) in BLOCKS.items():
        t = {k: TEX_REF % v for k, v in tex.items()}
        t["particle"] = t[next(iter(tex))]
        out[os.path.join(MODEL_DIR, mname + ".json")] = js(model(t, els(), DISPLAY))
        out[os.path.join(STATE_DIR, reg + ".json")] = js(facing_state(mname, extra))
    return out


def fragments():
    for i, lang in enumerate(("en_us", "es_es", "de_de", "sv_se")):
        print("# %s.lang" % lang)
        for reg, b in BLOCKS.items():
            print("tile.%s.name=%s" % (reg, b[4][i]))
        print("item.parking_ticket.name=%s" % TICKET_NAMES[i])
    print("# CsmTabStreetscape")
    for reg, b in BLOCKS.items():
        print("    initTabBlock(%s); // %s" % (b[3], b[4][0]))
    print("    initTabItem(new ItemParkingTicket()); // Parking Ticket")


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    a = ap.parse_args()
    if a.fragments:
        fragments()
        return 0
    out = outputs()
    if a.check:
        bad = []
        for p, data in out.items():
            # git may have checked a text file out with CRLF endings; compare it as LF
            have = open(p, "rb").read() if os.path.exists(p) else None
            if have is not None and p.endswith(".json"):
                have = have.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_parking_garage.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_parking_garage.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
