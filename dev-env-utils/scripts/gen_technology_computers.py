#!/usr/bin/env python3
"""Every asset the Technology - Computers & Peripherals tab ships.

    python dev-env-utils/scripts/gen_technology_computers.py
    python dev-env-utils/scripts/gen_technology_computers.py --check
    python dev-env-utils/scripts/gen_technology_computers.py --fragments   # tab lines to paste

What is here, and the class that places each:

- **Mechanical Keyboard (Full-Size, Black)** (technology.BlockDeskDeviceFactory): a black
  aluminium case and a full 104-key layout (function row, main block, navigation cluster and
  number pad) drawn on the key bed's top.

Keyboard layouts are drawn on a shared 128 px sheet, one 37-texel band a layout, at the
layout's true aspect (22.5 key units by 6.5 rows); a keyboard's key bed takes its band by uv.
More layouts and keycap colourways are more bands on the same sheet.

Writes models under models/block/technology/computers, textures under
textures/blocks/technology/computers, and the shared swatches under
textures/blocks/technology/common (see gen_technology_common.py).
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_computers.py", "computers")
TAB = tc.COMPUTERS

C.add_lang("itemGroup.tabtechcomputers",
           ("CSM: Technology - Computers & Peripherals", "CSM: Technik - Computer & Peripherie",
            "CSM: Tecnología - Ordenadores y periféricos",
            "CSM: Teknik - Datorer & kringutrustning"))

SIDES = ("north", "south", "east", "west", "down")

# ------------------------------------------------------------------------------------------
# Keyboard layouts
# ------------------------------------------------------------------------------------------
KEY_SHEET = 128
BAND = 37                       # texels a layout band: 6.5 rows at 128 / 22.5 texels a unit
UNIT = KEY_SHEET / 22.5         # texels a key unit across the sheet

# A full-size (104-key) ANSI layout, as rows of (start unit, width) in key units, the function
# row a half unit above the rest. The main block is 15 units wide, the navigation cluster
# starts at 15.25 and the number pad at 18.5.
_MAIN = [
    # number row
    [(0, 1)] + [(1 + i, 1) for i in range(12)] + [(13, 2)],
    # Q row
    [(0, 1.5)] + [(1.5 + i, 1) for i in range(12)] + [(13.5, 1.5)],
    # home row
    [(0, 1.75)] + [(1.75 + i, 1) for i in range(11)] + [(12.75, 2.25)],
    # shift row
    [(0, 2.25)] + [(2.25 + i, 1) for i in range(10)] + [(12.25, 2.75)],
    # bottom row: ctrl, win, alt, space, alt, win, menu, ctrl
    [(0, 1.25), (1.25, 1.25), (2.5, 1.25), (3.75, 6.25), (10, 1.25), (11.25, 1.25),
     (12.5, 1.25), (13.75, 1.25)],
]
_FUNCTION = [(0, 1)] + [(2 + i, 1) for i in range(4)] + [(6.5 + i, 1) for i in range(4)] + \
    [(11 + i, 1) for i in range(4)] + [(15.25 + i, 1) for i in range(3)]
_NAV = {0: [(15.25, 1), (16.25, 1), (17.25, 1)], 1: [(15.25, 1), (16.25, 1), (17.25, 1)],
        3: [(16.25, 1)], 4: [(15.25, 1), (16.25, 1), (17.25, 1)]}
_PAD = {0: [(18.5, 1), (19.5, 1), (20.5, 1), (21.5, 1)],
        1: [(18.5, 1), (19.5, 1), (20.5, 1)], 2: [(18.5, 1), (19.5, 1), (20.5, 1)],
        3: [(18.5, 1), (19.5, 1), (20.5, 1)], 4: [(18.5, 2), (20.5, 1)]}
# the pad's tall keys: plus (rows 1-2) and enter (rows 3-4), in the right-hand column
_PAD_TALL = [(21.5, 1, 1), (21.5, 3, 1)]


def full_size_keys():
    """Every key of the full-size layout as (x, y, w, h) in key units, y down from the far
    edge, the function row at y 0 and the number row at 1.5."""
    keys = [(x, 0, w, 1) for x, w in _FUNCTION]
    for r, row in enumerate(_MAIN):
        keys += [(x, 1.5 + r, w, 1) for x, w in row]
        keys += [(x, 1.5 + r, w, 1) for x, w in _NAV.get(r, [])]
        keys += [(x, 1.5 + r, w, 1) for x, w in _PAD.get(r, [])]
    keys += [(x, 1.5 + r, w, 2) for x, r, w in _PAD_TALL]
    return keys


def key_band(img, y0, keys, cap, plate, accent=None, accents=()):
    """Draws a layout into the band whose top is texel row y0: the plate, then each key as a
    cap with a lighter dished top and a darker front edge."""
    tc.rect(img, 0, y0, KEY_SHEET, y0 + BAND, plate)
    for i, (x, y, w, h) in enumerate(keys):
        a = int(round(x * UNIT)) + 1
        b = int(round(y * UNIT)) + y0 + 1
        c = int(round((x + w) * UNIT))
        d = int(round((y + h) * UNIT)) + y0
        colour = accent if (accent and i in accents) else cap
        tc.rect(img, a, b, c, d, tc.shade(colour, 0.78))         # the cap's skirt
        tc.rect(img, a, b, c, d - 1, colour)                      # its top
        tc.rect(img, a + 1, b, c - 1, b + 1, tc.shade(colour, 1.25))  # the lit back edge


@C.texture("keyboard_layouts")
def tex_keyboard_layouts():
    img = tc.fill(tc.BLACK_ANODISED, KEY_SHEET, 2, 501)
    keys = full_size_keys()
    # band 0: full-size, black caps on a black plate, a grey Esc
    key_band(img, 0, keys, (40, 41, 45), (14, 14, 16), accent=(112, 114, 120), accents=(0,))
    return img


# ------------------------------------------------------------------------------------------
# Keyboards
# ------------------------------------------------------------------------------------------
# The full-size keyboard: a 14.8 x 4.8 case, its key bed inset 0.3 all round and 0.55 proud.
# The bed's top takes its layout band whole: 128 x 37 texels for 14.2 x 4.2 units, near
# enough the layout's own aspect (3.46 against 3.38).
KB_FULL_BOX = (0.6, 0, 5.6, 15.4, 1.35, 10.4)


def band_uv(band):
    """The uv window of layout band ``band`` on the 128 px sheet."""
    v0 = band * BAND * 16.0 / KEY_SHEET
    return [0, round(v0, 4), 16, round(v0 + BAND * 16.0 / KEY_SHEET, 4)]


def keyboard(reg, names, box_, band, case_tex):
    x0, y0, z0, x1, y1, z1 = box_
    bed_y = y1 - 0.55
    els = [tc.B((x0, y0, z0), (x1, bed_y, z1), "case"),
           tc.B((x0 + 0.3, bed_y, z0 + 0.3), (x1 - 0.3, y1, z1 - 0.3), "keys", ("up",),
                uv={"up": band_uv(band)}, rot={"up": 180}),
           tc.B((x0 + 0.3, bed_y, z0 + 0.3), (x1 - 0.3, y1, z1 - 0.3), "bed",
                ("north", "south", "east", "west"))]
    m = tc.model({"case": case_tex, "keys": C.T("keyboard_layouts"),
                  "bed": tc.T("port_black"), "particle": case_tex}, els)
    C.add(reg, tc.desk_device_java(reg, box_), names,
          {reg: tc.with_display(m, tc.desk_display(0.85, 2))},
          tc.facing_state(C.M(reg)), tab=TAB)


keyboard("keyboard_mech_full_black",
         tc.names("Mechanical Keyboard (Full-Size, Black)",
                  "Mechanische Tastatur (Vollformat, Schwarz)",
                  "Teclado Mecánico (Tamaño Completo, Negro)",
                  "Mekaniskt tangentbord (fullstorlek, svart)"),
         KB_FULL_BOX, 0, tc.T("black_anodised"))

if __name__ == "__main__":
    sys.exit(C.main())
