#!/usr/bin/env python3
"""The Lighting tab's aviation obstruction beacons for tall buildings: a red flashing beacon
(FAA L-864 style) and a white strobe (L-865 style), each standing on a block or on a bracket off
the side of one (BlockObstructionBeacon).

    python dev-env-utils/scripts/gen_obstruction_beacons.py
    python dev-env-utils/scripts/gen_obstruction_beacons.py --check
    python dev-env-utils/scripts/gen_obstruction_beacons.py --fragments   # tab lines to paste

The models carry only the dark lens. The flash is TileEntityObstructionBeaconRenderer's: the
lens at full brightness and a glow, which a baked model cannot draw (a lit lens texture read as a
slightly lighter lens, and the strobe's one-tick frame was not seen at all). Its timings, from
FAA AC 150/5345-43J, Table 3-5, are documented there; the lens sizes and heights here must match
its Style constants. Every model faces north; a bracket's wall is at z = 16.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "lighting", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_obstruction_beacons.py", "lighting/obstruction", "lighting/obstruction",
                 assets=ASSETS)
TAB = "CsmTabLighting"
LIGHT = 9                 # block light while lit, as the airside obstruction lights give

# lens colours: dark, half, full
LENS = {
    "red": ((92, 24, 22), (206, 44, 36), (255, 76, 60)),
    "white": ((150, 158, 166), (226, 232, 240), (255, 255, 255)),
}
BODY = {"red": (74, 76, 80), "white": (148, 152, 156)}


def grain(colour, amount, seed, size=16):
    return lc.fill(colour, size, amount, seed)


def lens_tile(colour, level, seed, emissive=False):
    """One frame of a lens: moulded glass with horizontal Fresnel ribs, and, lit, a hot band
    across the LEDs behind it. Emissive, the dark frame is empty so nothing glows."""
    dark, half, full = LENS[colour]
    if emissive and level == 0:
        return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base = (dark, half, full)[level]
    img = grain(base, 3 if level else 5, seed)
    for y in range(1, 16, 3):
        lc.rect(img, 0, y, 16, y + 1, lc.shade(base, 1.18 if level == 0 else 0.9))
    if level:
        core = lc.clamp(tuple(v * 0.4 + 255 * 0.6 for v in base))
        lc.rect(img, 0, 6, 16, 10, core)
        if level == 2:
            lc.rect(img, 0, 7, 16, 9, (255, 255, 255))
    else:
        # the dark lens shows the LED boards behind it
        for x in (2, 7, 12):
            lc.rect(img, x, 6, x + 2, 10, lc.shade(base, 0.6))
    return img


def register_textures():
    for colour in LENS:
        C.texture("lens_%s_off" % colour)(lambda c=colour: lens_tile(c, 0, 900 + (
            10 if c == "white" else 0)))
        C.texture("body_%s" % colour)(lambda c=colour: grain(BODY[c], 4, 920 + len(c)))
    C.texture("bracket")(lambda: grain((96, 98, 100), 4, 930))


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
def unshaded(els):
    for e in els:
        e["shade"] = False
        for f in e["faces"].values():
            f["uv"] = [0, 0, 16, 16]
    return els


def red_beacon(y):
    """A squat LED L-864: a base flange, the driver housing, a short round lens of Fresnel ribs
    and a low domed cap."""
    return (lc.post(8, 8, 4.6, y, y + 1.2, "body")
            + lc.post(8, 8, 4.2, y + 1.2, y + 3.4, "body", bottom=False)
            + unshaded(lc.post(8, 8, 3.8, y + 3.4, y + 8.6, "lens", top=False, bottom=False))
            + lc.post(8, 8, 4.2, y + 8.6, y + 9.6, "body")
            + lc.post(8, 8, 2.6, y + 9.6, y + 10.2, "body", bottom=False))


def white_beacon(y):
    """A medium-intensity L-865 strobe: a wide base, a slimmer housing, a taller clear lens and
    a flat cap with a vent."""
    return (lc.post(8, 8, 4.6, y, y + 1.2, "body")
            + lc.post(8, 8, 3.6, y + 1.2, y + 4, "body", bottom=False)
            + unshaded(lc.post(8, 8, 3.2, y + 4, y + 10.6, "lens", top=False, bottom=False))
            + lc.post(8, 8, 3.6, y + 10.6, y + 11.4, "body")
            + lc.post(8, 8, 1.4, y + 11.4, y + 12.2, "body", bottom=False))


def bracket():
    """A steel bracket off a wall at z 16: a back plate on the wall, a shelf plate the beacon
    stands on and a fin under the shelf, all inside the block."""
    return [lc.box((3, 0, 15), (13, 6, 16), "bracket", faces=("north", "east", "west", "up",
                                                                "down")),
            lc.box((3, 2, 3), (13, 3, 15), "bracket", faces=("north", "east", "west", "up",
                                                               "down")),
            lc.box((7.5, 0.5, 8), (8.5, 2, 15), "bracket",
                   faces=("east", "west", "down", "north"))]


def display(scale):
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def box_java(b):
    return "new double[]{%s}" % ", ".join("%g" % v for v in b)


def beacon(colour, draw, height, names):
    reg = "obstruction_beacon_%s" % colour
    tex = {"body": C.T("body_%s" % colour), "lens": C.T("lens_%s_off" % colour),
           "bracket": C.T("bracket"), "particle": C.T("body_%s" % colour)}
    floor = lc.model(tex, draw(0), ao=False)
    floor["display"] = display(1.0)
    wall = lc.model(tex, bracket() + draw(3), ao=False)
    state = {
        "forge_marker": 1,
        "defaults": {"model": C.M(reg)},
        "variants": {
            "mount": {"floor": {}, "north": {"model": C.M(reg + "_wall")},
                      "east": {"model": C.M(reg + "_wall"), "y": 90},
                      "south": {"model": C.M(reg + "_wall"), "y": 180},
                      "west": {"model": C.M(reg + "_wall"), "y": 270}},
            "lit": {"true": {}, "false": {}},
            "inventory": [{}],
        },
    }
    floor_box = (3.4, 0, 3.4, 12.6, height, 12.6)
    wall_box = (3, 0, 3, 13, height + 3, 16)
    java = 'new BlockObstructionBeacon("%s", %s, %s, %d)' % (
        reg, box_java(floor_box), box_java(wall_box), LIGHT)
    C.add(reg, java, names, {reg: floor, reg + "_wall": wall}, state, tab=TAB)


register_textures()
beacon("red", red_beacon, 10.2, (
    "Tall Building Beacon (Red, Flashing)", "Hochhaus-Hindernisfeuer (Rot, blinkend)",
    "Baliza de Obstáculo para Edificios Altos (Roja, Intermitente)",
    "Hinderljus för Höga Byggnader (Rött, Blinkande)"))
beacon("white", white_beacon, 12.2, (
    "Tall Building Beacon (White, Strobe)", "Hochhaus-Hindernisfeuer (Weiß, Blitz)",
    "Baliza de Obstáculo para Edificios Altos (Blanca, Estroboscópica)",
    "Hinderljus för Höga Byggnader (Vitt, Blixtljus)"))

C.add_lang("csm.lighting.obstruction_beacon_red.tooltip", (
    "FAA L-864 style: red, 30 flashes a minute",
    "Nach FAA L-864: rot, 30 Blitze pro Minute",
    "Estilo FAA L-864: roja, 30 destellos por minuto",
    "Enligt FAA L-864: rött, 30 blixtar per minut"))
C.add_lang("csm.lighting.obstruction_beacon_white.tooltip", (
    "FAA L-865 style: white strobe, 40 flashes a minute",
    "Nach FAA L-865: weißer Blitz, 40 Blitze pro Minute",
    "Estilo FAA L-865: estroboscópica blanca, 40 destellos por minuto",
    "Enligt FAA L-865: vitt blixtljus, 40 blixtar per minut"))
C.add_lang("csm.lighting.obstruction_beacon.redstone", (
    "Redstone power switches it off (a daylight sensor keeps it dark by day)",
    "Redstone-Signal schaltet es aus (ein Tageslichtsensor hält es tagsüber dunkel)",
    "La señal de redstone la apaga (un sensor de luz diurna la mantiene apagada de día)",
    "Redstone-signal släcker det (en dagsljussensor håller det släckt på dagen)"))

if __name__ == "__main__":
    sys.exit(C.main())
