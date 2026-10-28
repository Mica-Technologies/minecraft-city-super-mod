#!/usr/bin/env python3
"""Every asset the Streetscape tab's EV charging pieces ship (Roads; #315): the Level 2 pedestal
charger (one port or two), the wall-mount charger, the DC fast charger, and the stall markings.

    python dev-env-utils/scripts/gen_ev_charging.py
    python dev-env-utils/scripts/gen_ev_charging.py --check
    python dev-env-utils/scripts/gen_ev_charging.py --fragments   # tab lines to paste

- The **pedestal** and **DC fast** chargers are BlockEvCharger, a BlockUtilityBox two cells
  tall (the root draws the whole charger, an invisible utility box part fills the cell above),
  so they borrow gen_streetscape_utility.py's spec writer and inventory models, as the parking
  meters do. The cable hangs in a loop from a holstered connector on the side.
- The **wall-mount** charger is BlockEvChargerWall, one block standing in the cell in front of
  the wall it hangs on: its back at the cell's far face (z = 16 facing north), as the Life
  Safety wall devices are drawn.
- The **status light** is a lens whose texture the blockstate's ``status`` variant swaps:
  available (green), charging (blue), fault (red). Each lens texture has an OptiFine ``_e``
  companion (Core's emissive.properties), the whole lens, so it glows; so does the DC fast
  charger's screen.
- The **stall markings** are BlockStreetCover, a single upward face like the covers
  (gen_streetscape_covers.py): green stall paint to fill a stall a block at a time, the EV
  pump symbol on green and EV ONLY lettering on green. Their textures are drawn as the placer
  sees them and stored turned 180 degrees, as the covers' are.

No brand: the chargers say EV and show a plug. Every model faces north (the front toward -z).
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

C = lc.Catalogue("gen_ev_charging.py", "streetscape/ev_charging", "streetscape/ev_charging",
                 assets=gu.ASSETS)
TAB = "CsmTabStreetscape"
slab = lc.pp.wbox

SHELL = (220, 224, 226)
DARK = (50, 53, 58)
GREEN = (0, 150, 96)
CABLE = (24, 24, 26)
CONNECTOR = (70, 72, 76)
NAVY = (14, 26, 52)
WHITE = (240, 240, 236)
PAINT = (40, 132, 84)     # the green stall paint
STATUS = {"available": (40, 230, 90), "charging": (40, 140, 255), "fault": (240, 40, 36)}
STATUS_KEYS = ("available", "charging", "fault")


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def plug(img, cx, y, colour):
    """A plug icon: a body with two prongs and a cord, 7 x 9 texels, its top at y."""
    lc.rect(img, cx - 2, y, cx - 1, y + 2, colour)
    lc.rect(img, cx + 1, y, cx + 2, y + 2, colour)
    lc.rect(img, cx - 3, y + 2, cx + 4, y + 6, colour)
    lc.rect(img, cx - 1, y + 6, cx + 2, y + 7, colour)
    lc.rect(img, cx, y + 7, cx + 1, y + 9, colour)


def charger_face(seed):
    """The front of a pedestal's head and of the wall unit: a green band, EV large, a plug."""
    img = lc.fill(SHELL, 32, 2, seed)
    lc.rect(img, 0, 0, 32, 4, GREEN)
    lc.draw_text_centred(img, "EV", 16, 7, GREEN, 2)
    plug(img, 16, 19, DARK)
    lc.rect(img, 0, 30, 32, 32, lc.shade(SHELL, 0.85))
    return img


def dc_screen():
    img = Image.new("RGBA", (32, 32), NAVY + (255,))
    lc.frame(img, 0, 0, 32, 32, (8, 12, 20), 1)
    lc.draw_text_centred(img, "PLUG IN", 16, 5, WHITE)
    lc.draw_text_centred(img, "TO", 16, 12, WHITE)
    lc.draw_text_centred(img, "START", 16, 18, WHITE)
    lc.frame(img, 10, 25, 21, 30, (90, 220, 120))     # a battery
    lc.rect(img, 21, 26, 22, 29, (90, 220, 120))
    lc.rect(img, 11, 26, 16, 29, (90, 220, 120))
    return img


def dc_label(seed):
    img = lc.fill(DARK, 32, 2, seed)
    lc.draw_text_centred(img, "DC FAST", 16, 6, WHITE)
    lc.draw_text_centred(img, "EV", 16, 15, GREEN, 2)
    return img


def card_reader():
    img = lc.fill((36, 38, 42), 16, 2, 4511)
    lc.frame(img, 1, 1, 15, 15, (90, 92, 96))
    lc.draw_text_centred(img, "TAP", 8, 5, WHITE)
    return img


def vent(seed):
    img = lc.fill(SHELL, 32, 2, seed)
    for y in range(4, 28, 3):
        lc.rect(img, 6, y, 26, y + 1, lc.shade(SHELL, 0.6))
    return img


def lens(colour):
    """A status lens: the colour, a lighter core and a darker rim."""
    img = Image.new("RGBA", (16, 16), lc.clamp(colour) + (255,))
    lc.frame(img, 0, 0, 16, 16, lc.shade(colour, 0.7), 2)
    lc.rect(img, 4, 5, 12, 11, lc.shade(colour, 1.25))
    return img


def register_textures():
    tex = {
        "shell": gu.paint(SHELL, 4501, grain=2),
        "dark": gu.paint(DARK, 4502, grain=3),
        "cable": gu.paint(CABLE, 4503, grain=2),
        "connector": gu.paint(CONNECTOR, 4504, grain=3),
        "concrete": gu.concrete(4505),
        "face": charger_face(4506),
        "screen": dc_screen(),
        "screen_e": dc_screen(),
        "dc_label": dc_label(4507),
        "reader": card_reader(),
        "vent": vent(4508),
    }
    for key, colour in STATUS.items():
        tex["status_" + key] = lens(colour)
        tex["status_" + key + "_e"] = lens(colour)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# Geometry (facing north: the front is the -z face; the viewer's right is -x)
# ------------------------------------------------------------------------------------------
PROUD = 0.2     # a decal or lens stands this far proud of its face (model_depth.py's gap)


def decal(frm, to, tex, face):
    el = slab(frm, to, tex, (face,))
    el["faces"][face]["uv"] = [0, 0, 16, 16]
    return el


def holster(side, y0, z0, z1, body_x, thick=0.8, low=2.6):
    """A holstered connector on one side of a body (side -1 the viewer's right, -x; +1 the
    left) and its cable hanging in a loop down to the foot of the body and back in."""
    s = side
    hx0, hx1 = (body_x - 1.6, body_x) if s < 0 else (body_x, body_x + 1.6)
    cx0, cx1 = (hx0 - 0.9, hx0) if s < 0 else (hx1, hx1 + 0.9)
    zc = (z0 + z1) / 2.0
    sx0, sx1 = (cx0 + 0.05, cx0 + 0.05 + thick) if s < 0 else (cx1 - 0.05 - thick, cx1 - 0.05)
    return [
        slab([hx0, y0, z0], [hx1, y0 + 3.5, z1], "dark"),                         # holster
        slab([cx0, y0 + 0.5, zc - 0.7], [cx1, y0 + 4.2, zc + 0.7], "connector"),  # handle
        slab([sx0, low, zc - thick / 2], [sx1, y0 + 0.5, zc + thick / 2], "cable"),
        slab([min(sx0, body_x), low - thick, zc - thick / 2],
             [max(sx1, body_x), low, zc + thick / 2], "cable"),
    ]


def pedestal(dual):
    """A Level 2 pedestal about 1.6 m tall: a slim column on a base flange, a head with the
    face and the status light, one holstered connector (two for the dual), each cable looped
    down to the foot of the column."""
    x0, x1, z0, z1 = 5.5, 10.5, 6.5, 9.5
    els = [
        slab([5, 0, 6], [11, 0.8, 10], "dark"),
        slab([x0, 0.8, z0], [x1, 26, z1], "shell"),
        slab([x0 - 0.3, 26, z0 - 0.3], [x1 + 0.3, 27, z1 + 0.3], "dark"),
        decal([x0, 15, z0 - PROUD], [x1, 24, z0], "face", "north"),
        slab([6.5, 24.4, z0 - PROUD], [9.5, 25.4, z0], "status", ("north",)),
    ]
    els += holster(-1, 13, 7.2, 8.8, x0)
    if dual:
        els += [decal([x0, 15, z1], [x1, 24, z1 + PROUD], "face", "south"),
                slab([6.5, 24.4, z1], [9.5, 25.4, z1 + PROUD], "status", ("south",))]
        els += holster(1, 13, 7.2, 8.8, x1)
    return els


def dc_fast():
    """A DC fast charger about 1.9 m tall: a cabinet on a plinth, a cap, the screen and a
    status bar across the top of the front, a card reader, the DC FAST band, vents on the
    sides and a heavy connector holstered on each side."""
    x0, x1, z0, z1 = 3, 13, 4, 12
    els = [
        slab([2, 0, 3.5], [14, 1, 12.5], "concrete"),
        slab([x0, 1, z0], [x1, 29, z1], "shell", ("north", "south", "up", "down")),
        slab([x0, 1, z0], [x1, 29, z1], "vent", ("east", "west")),
        slab([x0 - 0.5, 29, z0 - 0.5], [x1 + 0.5, 30.5, z1 + 0.5], "dark"),
        decal([4, 16.5, z0 - PROUD], [12, 24.5, z0], "screen", "north"),
        slab([3.5, 26, z0 - PROUD], [12.5, 27.2, z0], "status", ("north",)),
        decal([6.5, 12.5, z0 - PROUD], [9.5, 15.5, z0], "reader", "north"),
        decal([3.5, 3, z0 - PROUD], [12.5, 10, z0], "dc_label", "north"),
    ]
    els += holster(-1, 14, 6.8, 9.2, x0, thick=1.2, low=3.2)
    els += holster(1, 14, 6.8, 9.2, x1, thick=1.2, low=3.2)
    return els


def wall_unit():
    """The wall-mount charger: a mounting plate on the wall face (z = 16), the unit on it with
    the face and the status light, a holstered connector on its right and the cable coiled on
    a hook on its left."""
    els = [
        slab([4.5, 2.5, 15.5], [11.5, 13.5, 16], "dark"),
        slab([5, 3, 12.5], [11, 13, 15.5], "shell"),
        decal([5, 4, 12.5 - PROUD], [11, 10.5, 12.5], "face", "north"),
        slab([6.5, 11, 12.5 - PROUD], [9.5, 12, 12.5], "status", ("north",)),
        slab([3.4, 6, 13.2], [5, 9.5, 15.5], "dark"),                         # holster
        slab([2.6, 6.5, 13.7], [3.4, 10.2, 15], "connector"),
        slab([11, 8.5, 14.2], [13.2, 9.3, 15.5], "dark"),                     # the hook
    ]
    # the coil: a loop of cable on the hook, and its run up into the unit's foot
    for (a, b) in ((11.6, 12.4), (13.2, 14.0)):
        els.append(slab([a, 1.4, 14.4], [b, 8.5, 15.2], "cable"))
    els.append(slab([11.6, 0.6, 14.4], [14.0, 1.4, 15.2], "cable"))
    els.append(slab([12.4, 8.5, 14.4], [13.2, 9.3, 15.2], "cable"))
    els.append(slab([7.6, 1.8, 13.6], [8.4, 3, 14.4], "cable"))
    els.append(slab([7.6, 1.0, 13.6], [11.6, 1.8, 14.4], "cable"))
    return els


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
TEXTURES = {"shell": "shell", "dark": "dark", "cable": "cable", "connector": "connector",
            "face": "face", "status": "status_available"}
DC_TEXTURES = {"shell": "shell", "dark": "dark", "cable": "cable", "connector": "connector",
               "vent": "vent", "concrete": "concrete", "screen": "screen", "reader": "reader",
               "dc_label": "dc_label", "status": "status_available"}


def status_variants():
    return {"status": {k: {"textures": {"status": C.T("status_" + k)}} for k in STATUS_KEYS}}


def add_unit(registry, names, els, textures, kind):
    models = {registry: gu.model_for_catalogue(C, textures, els),
              registry + "_inventory": gu.model_for_catalogue(
                  C, textures, gu.inventory_elements(els))}
    state = lc.facing_state(C.M(registry), status_variants())
    state["variants"]["inventory"] = [{"model": C.M(registry + "_inventory")}]
    java = 'new BlockEvCharger("%s", %s,\n        BlockEvCharger.Kind.%s)' % (
        registry, gu.spec_java(els, (1, 1, 2), None), kind)
    C.add(registry, java, names, models, state, tab=TAB)


def add_wall(registry, names, els, textures):
    models = {registry: gu.model_for_catalogue(C, textures, els)}
    state = lc.facing_state(C.M(registry), status_variants())
    x0, y0, z0, x1, y1, z1 = gu.bounds(els)
    k = 1 / 16.0
    java = 'new BlockEvChargerWall("%s",\n        new AxisAlignedBB(%s))' % (
        registry, ", ".join("%.6f" % round(v * k, 6) for v in (x0, y0, z0, x1, y1, z1)))
    C.add(registry, java, names, models, state, tab=TAB)


def chargers():
    add_unit("ev_charger_pedestal", ("EV Charger (Pedestal)", "E-Ladesäule",
             "Cargador de Vehículos Eléctricos (Pedestal)", "Laddstolpe för Elbil"),
             pedestal(False), TEXTURES, "LEVEL_2")
    add_unit("ev_charger_pedestal_dual", ("EV Charger (Pedestal, Dual Port)",
             "E-Ladesäule (Zwei Ladepunkte)",
             "Cargador de Vehículos Eléctricos (Pedestal, Doble Puerto)",
             "Laddstolpe för Elbil (Två Uttag)"),
             pedestal(True), TEXTURES, "LEVEL_2")
    add_wall("ev_charger_wall", ("EV Charger (Wall Mount)", "Wallbox",
             "Cargador de Vehículos Eléctricos (de Pared)", "Laddbox för Elbil (Väggmonterad)"),
             wall_unit(), TEXTURES)
    add_unit("ev_charger_dc_fast", ("EV Charger (DC Fast)", "Schnellladesäule (DC)",
             "Cargador Rápido de Vehículos Eléctricos (CC)", "Snabbladdare för Elbil (DC)"),
             dc_fast(), DC_TEXTURES, "DC_FAST")


# ------------------------------------------------------------------------------------------
# Stall markings (BlockStreetCover)
# ------------------------------------------------------------------------------------------
MARK = 64
FACE_Y = 0.2      # as the covers: the face a fraction of a pixel above the surface
BOX_TOP = 1.0


def paint_tex(seed):
    return lc.fill(PAINT, MARK, 3, seed)


def pump_marking(seed):
    """The EV pump of the D9-11b symbol in white on the stall paint, as it is stencilled."""
    img = paint_tex(seed)
    d = ImageDraw.Draw(img)
    w = WHITE + (255,)
    # the pump: a body with a window, EV in it, a hose and nozzle, and a base bar
    d.rectangle((20, 12, 37, 50), fill=w)
    d.rectangle((23, 15, 34, 23), fill=PAINT + (255,))
    lc.draw_text_centred(img, "E", 28.5, 27, PAINT, 2)
    lc.draw_text_centred(img, "V", 28.5, 39, PAINT, 2)
    d.rectangle((38, 18, 43, 20), fill=w)
    d.rectangle((42, 18, 44, 46), fill=w)
    d.rectangle((38, 45, 44, 47), fill=w)
    d.rectangle((14, 50, 50, 53), fill=w)
    return img


def text_marking(seed):
    img = paint_tex(seed)
    lc.draw_text_centred(img, "EV", 32, 12, WHITE, 4)
    lc.draw_text_centred(img, "ONLY", 32, 38, WHITE, 3)
    return img


def cover_model(tex):
    return lc.model({"cover": C.T(tex), "particle": C.T(tex)}, [
        {"from": [0, FACE_Y, 0], "to": [16, FACE_Y, 16],
         "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#cover"}}}], ao=False)


def cover_state(tex):
    return {"forge_marker": 1, "defaults": {"model": C.M(tex)},
            "variants": {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                    "west": {"y": 270}}}}


def cover_item(tex):
    return {"parent": "item/generated", "textures": {"layer0": C.T(tex)},
            "display": {"gui": {"rotation": [0, 0, 180]},
                        "fixed": {"rotation": [0, 180, 180]},
                        "ground": {"rotation": [0, 0, 180], "translation": [0, 2, 0],
                                   "scale": [0.5, 0.5, 0.5]}}}


def add_marking(registry, draw, names):
    seed = sum(ord(c) * (i + 1) for i, c in enumerate(registry))
    img = draw(seed).rotate(180)
    C.texture(registry)(lambda img=img: img)
    java = ('new BlockStreetCover("%s",\n        new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, %.6f, 1.0))'
            % (registry, BOX_TOP / 16.0))
    C.add(registry, java, names, {registry: cover_model(registry)}, cover_state(registry),
          item=cover_item(registry), tab=TAB)


def markings():
    add_marking("ev_stall_paint", paint_tex, ("EV Stall Paint (Green)",
                "Ladeplatz-Markierung (Grün)", "Pintura de Plaza de Carga (Verde)",
                "Laddplatsmålning (Grön)"))
    add_marking("ev_stall_marking_symbol", pump_marking, ("EV Stall Marking (Symbol)",
                "Ladeplatz-Markierung (Symbol)", "Marca de Plaza de Carga (Símbolo)",
                "Laddplatsmarkering (Symbol)"))
    add_marking("ev_stall_marking_ev_only", text_marking, ("EV Stall Marking (EV Only)",
                "Ladeplatz-Markierung (Nur E-Fahrzeuge)", "Marca de Plaza de Carga (Solo VE)",
                "Laddplatsmarkering (Endast Elbil)"))


chargers()
markings()

if __name__ == "__main__":
    sys.exit(C.main())
