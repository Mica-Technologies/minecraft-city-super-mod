#!/usr/bin/env python3
"""Every asset the Streetscape tab's parking meters ship: mechanical and digital meters (one or
two heads), the multi-space pay station and the pay-by-phone sign.

    python dev-env-utils/scripts/gen_streetscape_meters.py
    python dev-env-utils/scripts/gen_streetscape_meters.py --check
    python dev-env-utils/scripts/gen_streetscape_meters.py --fragments   # tab lines to paste

The meters are BlockParkingMeter, the sign BlockUtilityBoxLabelled; both are BlockUtilityBox
units (a root block drawing the whole of it, invisible parts filling the cell above), so this
borrows gen_streetscape_utility.py's measuring and inventory-model helpers and its spec writer.

The model draws each head's housing and a dark window; TileEntityParkingMeterRenderer draws
what the window shows (a dial or the EXPIRED flag, an LCD, a screen). Each head's window is
written into the tab line as {centre x, centre y, face z, width, height} from the same numbers
the window element is built from, so the renderer draws exactly on it. The sign's zone number
is a label the player edits, like a transformer's ID number, drawn white on the sign's green.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

C = lc.Catalogue("gen_streetscape_meters.py", "streetscape/meters", "streetscape/meters",
                 assets=gu.ASSETS)
TAB = "CsmTabStreetscape"
slab = gu.slab
decal = gu.decal
post = lc.post

STEEL = (122, 126, 130)
CHARCOAL = (58, 60, 64)
GALVANISED = (150, 152, 148)
GLASS = (26, 30, 34)
DARK = (34, 34, 36)
SOLAR = (28, 42, 78)
SIGN_GREEN = (22, 104, 62)
WHITE = (240, 240, 236)
BLUE = (22, 58, 128)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def solar(seed):
    img = lc.fill(SOLAR, 32, 3, seed)
    for i in range(0, 32, 8):
        lc.rect(img, i, 0, i + 1, 32, (120, 130, 150))
        lc.rect(img, 0, i, 32, i + 1, (120, 130, 150))
    return img


def keypad():
    img = Image.new("RGBA", (32, 32), CHARCOAL + (255,))
    for row in range(4):
        for col in range(3):
            lc.bevel(img, 4 + col * 9, 3 + row * 7, 11 + col * 9, 8 + row * 7, (170, 170, 168))
    return img


def pay_here():
    img = Image.new("RGBA", (32, 32), BLUE + (255,))
    lc.draw_text_centred(img, "PAY", 16, 6, WHITE, 1)
    lc.draw_text_centred(img, "HERE", 16, 16, WHITE, 1)
    return img


def pay_by_phone():
    """The sign's face: PAY BY PHONE over a phone, and ZONE above the band where the renderer
    draws the zone number. Drawn on 64 px for a plate 9 x 11 px, so a texel is not square."""
    img = Image.new("RGBA", (64, 64), SIGN_GREEN + (255,))
    lc.frame(img, 1, 1, 63, 63, WHITE, 2)
    lc.draw_text_centred(img, "PAY BY", 32, 6, WHITE, 2)
    lc.draw_text_centred(img, "PHONE", 32, 18, WHITE, 2)
    lc.rect(img, 27, 30, 37, 42, WHITE)          # the phone, a texel clear of PHONE above
    lc.rect(img, 29, 32, 35, 38, SIGN_GREEN)
    lc.rect(img, 31, 39, 33, 41, SIGN_GREEN)
    lc.draw_text_centred(img, "ZONE", 32, 44, WHITE, 1)  # clear of the number's backing below
    return img


def register_textures():
    tex = {
        "steel": gu.paint(STEEL, 31),
        "charcoal": gu.paint(CHARCOAL, 32, grain=3),
        "galvanised": gu.paint(GALVANISED, 33),
        "glass": gu.paint(GLASS, 34, grain=2),
        "dark": gu.paint(DARK, 35, grain=2),
        "solar": solar(36),
        "keypad": keypad(),
        "pay_here": pay_here(),
        "sign_face": pay_by_phone(),
        "concrete": gu.concrete(37),
    }
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------
def meter_post(top):
    return (post(8, 8, 0.9, 0, top, "post", top=False)
            + post(8, 8, 1.6, 0, 0.5, "post"))


# How far a window stands proud of its housing. model_depth.py separates faces closer than 0.2,
# and a window drawn closer was moved out past the display the renderer draws in front of it,
# hiding every meter's display; at 0.2 it stays where the display is told it is.
WINDOW_PROUD = 0.2


def window(cx, cy, z, w, h):
    """A head's dark window, and the display the renderer draws over it."""
    el = slab([cx - w / 2 - 0.2, cy - h / 2 - 0.2, z - WINDOW_PROUD],
              [cx + w / 2 + 0.2, cy + h / 2 + 0.2, z], "glass", ("north",))
    return el, [round(cx, 3), round(cy, 3), round(z - WINDOW_PROUD, 3), round(w, 3), round(h, 3)]


def mechanical_head(cx, y0, width=4.8):
    """A classic coin meter's head: a housing with a rounded crown, a window over the dial, a
    coin slot and the winding knob below it."""
    x0, x1, z0, z1 = cx - width / 2, cx + width / 2, 6.2, 9.8
    els = [
        slab([x0, y0, z0], [x1, y0 + 5.5, z1], "body"),
        slab([x0 + 0.3, y0 + 5.5, z0 + 0.3], [x1 - 0.3, y0 + 6.4, z1 - 0.3], "body"),
        slab([x0 + 0.9, y0 + 6.4, z0 + 0.8], [x1 - 0.9, y0 + 6.9, z1 - 0.8], "body"),
        slab([cx - 0.7, y0 + 1.0, z0 - 0.05], [cx + 0.7, y0 + 1.3, z0], "dark", ("north",)),
        slab([cx - 0.4, y0 + 0.2, z0 - 0.5], [cx + 0.4, y0 + 0.8, z0], "dark"),
    ]
    win, display = window(cx, y0 + 3.6, z0, width - 1.6, 2.6)
    return els + [win], display


def digital_head(cx, y0, width=5.0):
    """A modern meter's head: a taller charcoal housing with a solar panel on top, an LCD and a
    keypad."""
    x0, x1, z0, z1 = cx - width / 2, cx + width / 2, 6.0, 10.0
    els = [
        slab([x0, y0, z0], [x1, y0 + 8.2, z1], "body"),
        decal([x0 + 0.2, y0 + 8.2, z0 + 0.2], [x1 - 0.2, y0 + 8.6, z1 - 0.2], "solar", "up"),
        slab([x0 + 0.2, y0 + 8.2, z0 + 0.2], [x1 - 0.2, y0 + 8.6, z1 - 0.2], "body",
             ("north", "south", "east", "west")),
        decal([cx - 1.5, y0 + 1.8, z0 - 0.05], [cx + 1.5, y0 + 4.4, z0], "keypad", "north"),
        slab([cx - 1.0, y0 + 0.8, z0 - 0.05], [cx + 1.0, y0 + 1.1, z0], "dark", ("north",)),
    ]
    win, display = window(cx, y0 + 6.2, z0, width - 1.0, 1.8)
    return els + [win], display


def single(head_fn):
    els, display = head_fn(8, 14)
    return meter_post(14) + els, [display]


def double(head_fn):
    """Two heads on a yoke, side by side, one per space."""
    els = meter_post(14) + [slab([2.6, 13.6, 7.3], [13.4, 14.4, 8.7], "post")]
    displays = []
    for cx in (11.0, 5.0):
        head, display = head_fn(cx, 14.4, 4.6)
        els += head
        displays.append(display)
    return els, displays


def pay_station():
    """The multi-space kiosk: a plinth, a tall cabinet with a hood and a solar panel, the lit
    screen, a keypad and a PAY HERE panel."""
    els = [
        slab([3.5, 0, 4.5], [12.5, 0.8, 11.5], "base"),
        slab([4, 0.8, 5], [12, 24, 11], "body"),
        slab([3.7, 24, 4.6], [12.3, 25, 11.4], "body"),
        decal([4, 25, 5], [12, 25.4, 11], "solar", "up"),
        slab([4, 25, 5], [12, 25.4, 11], "body", ("north", "south", "east", "west")),
        decal([5.5, 12, 4.95], [10.5, 16.5, 5], "keypad", "north"),
        slab([6.8, 10.6, 4.95], [9.2, 11.0, 5], "dark", ("north",)),
        decal([4.6, 21.8, 4.95], [11.4, 23.6, 5], "pay_here", "north"),
    ]
    win, display = window(8, 18.8, 5, 5.8, 3.6)
    return els + [win], [display]


def pay_by_phone_sign():
    """A plate on a square galvanised post: the face drawn in its texture, the zone number drawn
    over its bottom band by the label renderer."""
    return [
        slab([7.4, 0, 7.8], [8.6, 26, 9.0], "post"),
        decal([3.5, 15, 7.4], [12.5, 26, 7.8], "face", "north"),
        slab([3.5, 15, 7.4], [12.5, 26, 7.8], "back", ("south", "east", "west", "up", "down")),
    ]


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def add_meter(registry, names, els, displays, textures, kind):
    models = {registry: gu.model_for_catalogue(C, textures, els),
              registry + "_inventory": gu.model_for_catalogue(
                  C, textures, gu.inventory_elements(els))}
    state = lc.facing_state(C.M(registry))
    state["variants"]["inventory"] = [{"model": C.M(registry + "_inventory")}]
    heads = "new float[][]{%s}" % ", ".join(
        "{%s}" % ", ".join("%sf" % v for v in d) for d in displays)
    java = 'new BlockParkingMeter("%s", %s,\n        BlockParkingMeter.Kind.%s, %s)' % (
        registry, gu.spec_java(els, (1, 1, 2), None), kind, heads)
    C.add(registry, java, names, models, state, tab=TAB)


def meters():
    mech = {"body": "steel", "post": "galvanised", "dark": "dark", "glass": "glass"}
    digi = {"body": "charcoal", "post": "galvanised", "dark": "dark", "glass": "glass",
            "solar": "solar", "keypad": "keypad"}
    els, d = single(mechanical_head)
    add_meter("parking_meter_mechanical", ("Parking Meter (Mechanical)",
              "Parkuhr (Mechanisch)", "Parquímetro (Mecánico)", "Parkeringsautomat (Mekanisk)"),
              els, d, mech, "MECHANICAL")
    els, d = double(mechanical_head)
    add_meter("parking_meter_mechanical_double", ("Parking Meter (Mechanical, Double)",
              "Parkuhr (Mechanisch, Doppelt)", "Parquímetro (Mecánico, Doble)",
              "Parkeringsautomat (Mekanisk, Dubbel)"), els, d, mech, "MECHANICAL")
    els, d = single(digital_head)
    add_meter("parking_meter_digital", ("Parking Meter (Digital)", "Parkuhr (Digital)",
              "Parquímetro (Digital)", "Parkeringsautomat (Digital)"), els, d, digi, "DIGITAL")
    els, d = double(digital_head)
    add_meter("parking_meter_digital_double", ("Parking Meter (Digital, Double)",
              "Parkuhr (Digital, Doppelt)", "Parquímetro (Digital, Doble)",
              "Parkeringsautomat (Digital, Dubbel)"), els, d, digi, "DIGITAL")
    els, d = pay_station()
    add_meter("parking_pay_station", ("Parking Pay Station", "Parkscheinautomat",
              "Máquina Expendedora de Estacionamiento", "Parkeringsbiljettautomat"),
              els, d, {"body": "charcoal", "base": "concrete", "dark": "dark", "glass": "glass",
                       "solar": "solar", "keypad": "keypad", "pay_here": "pay_here"},
              "STATION")


def sign():
    els = pay_by_phone_sign()
    reg = "parking_pay_by_phone_sign"
    tex = {"face": "sign_face", "back": "galvanised", "post": "galvanised"}
    models = {reg: gu.model_for_catalogue(C, tex, els),
              reg + "_inventory": gu.model_for_catalogue(C, tex, gu.inventory_elements(els))}
    state = lc.facing_state(C.M(reg))
    state["variants"]["inventory"] = [{"model": C.M(reg + "_inventory")}]
    label = ("new UtilityBoxSpec.Label(8.0f, 16.3f, 7.4f, 1, 1.3f, false,\n"
             "            0xF0F0EC, 0x16683E)")
    java = 'new BlockUtilityBoxLabelled("%s", %s)' % (
        reg, gu.spec_java(els, (1, 1, 2), None).replace("null)", label + ")"))
    C.add(reg, java, ("Pay-by-Phone Parking Sign", "Handyparken-Schild",
                      "Señal de Pago por Móvil", "Skylt för Mobilparkering"),
          models, state, tab=TAB)


meters()
sign()

if __name__ == "__main__":
    sys.exit(C.main())
