#!/usr/bin/env python3
"""Every asset the doors ship.

    python dev-env-utils/scripts/gen_doors.py
    python dev-env-utils/scripts/gen_doors.py --check
    python dev-env-utils/scripts/gen_doors.py --fragments   # lang and tab lines

Twelve doors, one class (BlockBuildingDoor), constructed by registry name: interior wood doors
(oak, white; solid and with a vision lite), hollow metal (grey, a fire door with a wired lite, an
exit door with a push bar), a storefront door (dark bronze and glass), and residential doors (a
six-panel front door in three colours, a half-glass back door). And the Door Closer, an add-on
item that fits a real-looking closer to any of them and makes it close itself.

Each door is two blocks, a lower and an upper half, drawn here with the INSIDE to the north and
the outside to the south. Most doors hang as a vanilla door does: the leaf lies along the outside
face of its cell (z 14.25..16) and swings inward within the cell. The exit, storefront and fire
doors swing OUT, as real ones do -- toward the way out, away from the push bar -- and hang in the
depth mirror of that: along the inside face (z 0..1.75), turning outward within the cell, so an open
door of either kind lies along the jamb inside its own cell (OUTSWING). Its hinge is on the left
(x = 0, seen from outside) or the right, and the right is the mirror of the left.

The open door is the closed one turned a quarter about a pivot on the hinge edge -- (0.875,
15.125) for a left-hinged inswing door, (0.875, 0.875) for an outswing one -- which carries the leaf
to lie along the jamb (x 0..1.75), its latch edge to the far side of the cell. It is written out
as its own model rather than a blockstate rotation (which turns about the block's centre and would
put the hinge in the wrong corner), and BlockBuildingDoor's renderer swings the closed model about
the same pivot, so the swing ends exactly on the open model. SHARED: PIVOT.

Any placed door may be REVERSED from its kind's default -- an interior door made to swing out, an
exit door made to swing in -- by sneaking as it is placed or with the Door Swing Tool. A reversed
door is the depth mirror of its kind's own models, shut and open, hardware and all, so a push bar
stays on the face that is pushed; the blockstate picks those (_out for a kind that swings in, _in
for one that swings out) when the door's `reversed` state is true, and the closer that goes with
the way it now swings.

A fitted door closer sits on the PUSH side of the leaf -- the side the door swings away from -- as a
parallel-arm closer: its body on the leaf, a shoe on the wall above the opening, and a two-link arm
between them whose elbow is solved from the links' lengths at every angle (closer_joints). Only the
body swings with the leaf; the renderer draws the arm from the same solution, so the swing starts on
the shut model and ends on the open one. SHARED: CLOSER_*.
"""

import argparse
import json
import math
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
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "doors")
MODEL_DIR = os.path.join(MODULE, "models", "block", "doors")
ITEM_TEX_DIR = os.path.join(MODULE, "textures", "items")
ITEM_MODEL_DIR = os.path.join(MODULE, "models", "item")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/doors/%s"
MODEL_REF = "csm:doors/%s"

# SHARED with BlockBuildingDoor: the leaf's thickness and the pivot a left-hinged leaf turns
# about, in px, with the inside to the north. An outswing door's pivot is the depth mirror.
LEAF = 1.75
PIVOT = (0.875, 15.125)

# SHARED with BlockBuildingDoor.OUTSWING: the doors that swing out, toward the outside. A real exit
# door opens in the direction of escape, which is what lets a push bar work at all -- a push bar on
# a door that swings toward you is a pull handle nobody can pull -- and a storefront's or a fire
# door's does the same. The rest swing in, as a vanilla door does.
OUTSWING = ("door_metal_fire", "door_metal_exit", "door_storefront_bronze")

# SHARED with DoorCloserArm: the closer's arm, in px, for a left-hinged inswing door with the inside
# to the north (every other door is a mirror of it). The spindle rides on the closer's body on the
# leaf's push face; the shoe is fixed on the wall above the opening; the main arm (spindle to
# elbow) and the forearm (elbow to shoe) are rigid, so the elbow is wherever the two lengths meet,
# on the side CLOSER_ELBOW_SIDE picks -- toward the latch while the door is shut, as a parallel arm
# folds. The lengths and the shoe were chosen so that both baked poses (shut, and a quarter turn
# open) put each link on a multiple of 22.5 degrees, the only angles a model element can be turned
# to: shut, the main arm runs along the door toward the latch and the forearm folds back to the
# shoe at 157.5; open, they are at 45 and 90, the forearm straight out through the top of the
# opening. No other choice kept the arm clear of the leaf and the jambs through the whole swing.
CLOSER_SPINDLE = (6.0, 17.0)
CLOSER_SHOE = (6.5317, 18.995)
CLOSER_MAIN = 5.3482
CLOSER_FORE = 5.2133
CLOSER_ELBOW_SIDE = -1

# Tint indices mark which part of a door model is which for the swing renderer; no colour handler is
# registered for the doors, so they tint nothing. SHARED with TileEntityDoorSwingRenderer.
TINT_FIXED = 1   # fixed to the frame, so drawn where it is and not swung: the closer's shoe
TINT_ARM = 2     # the closer's arm, which the renderer solves and draws itself at every angle
TINT_PUSH = 3    # a push bar's touch bar, which dips toward the leaf as the door is pushed open

# registry name -> (style, colour, glass layer, name in each language). Order is creative order.
DOORS = {
    "door_wood_oak": ("wood", (184, 142, 96), False, (
        "Interior Door (Oak)", "Puerta de Interior (Roble)", "Innentür (Eiche)",
        "Innerdörr (Ek)")),
    "door_wood_oak_lite": ("wood_lite", (184, 142, 96), True, (
        "Interior Door (Oak, Vision Lite)", "Puerta de Interior (Roble, Mirilla)",
        "Innentür (Eiche, Lichtausschnitt)", "Innerdörr (Ek, Glasruta)")),
    "door_wood_white": ("wood", (236, 236, 230), False, (
        "Interior Door (White)", "Puerta de Interior (Blanca)", "Innentür (Weiß)",
        "Innerdörr (Vit)")),
    "door_wood_white_lite": ("wood_lite", (236, 236, 230), True, (
        "Interior Door (White, Vision Lite)", "Puerta de Interior (Blanca, Mirilla)",
        "Innentür (Weiß, Lichtausschnitt)", "Innerdörr (Vit, Glasruta)")),
    "door_metal_grey": ("metal", (146, 152, 158), False, (
        "Hollow Metal Door (Grey)", "Puerta Metálica (Gris)", "Stahltür (Grau)",
        "Ståldörr (Grå)")),
    "door_metal_fire": ("fire", (146, 152, 158), True, (
        "Fire Door (Wired Lite)", "Puerta Cortafuegos (Mirilla Armada)",
        "Brandschutztür (Drahtglas)", "Branddörr (Trådglas)")),
    "door_metal_exit": ("exit", (146, 152, 158), False, (
        "Exit Door (Push Bar)", "Puerta de Salida (Barra Antipánico)",
        "Ausgangstür (Panikstange)", "Utgångsdörr (Tryckregel)")),
    "door_storefront_bronze": ("storefront", (58, 48, 38), True, (
        "Storefront Door (Dark Bronze)", "Puerta de Escaparate (Bronce Oscuro)",
        "Ladentür (Dunkelbronze)", "Butiksdörr (Mörk Brons)")),
    "door_front_white": ("front", (236, 236, 230), False, (
        "Front Door (White)", "Puerta Principal (Blanca)", "Haustür (Weiß)",
        "Ytterdörr (Vit)")),
    "door_front_red": ("front", (140, 32, 30), False, (
        "Front Door (Red)", "Puerta Principal (Roja)", "Haustür (Rot)", "Ytterdörr (Röd)")),
    "door_front_black": ("front", (38, 38, 40), False, (
        "Front Door (Black)", "Puerta Principal (Negra)", "Haustür (Schwarz)",
        "Ytterdörr (Svart)")),
    "door_back_halfglass": ("halfglass", (236, 236, 230), True, (
        "Back Door (Half Glass)", "Puerta Trasera (Media Vidriera)", "Hintertür (Halbverglast)",
        "Bakdörr (Halvglasad)")),
    # The job trailer's entry door, registered in the Construction Site tab beside the trailer's
    # walls (issue #228: it was a wall block with a door drawn on it).
    "job_trailer_door": ("trailer", (214, 214, 208), True, (
        "Job Trailer Door", "Oficina de Obra (Puerta)", "Baucontainer (Tür)", "Bodvagn (Dörr)")),
}

# The hospital doors: a narrow vision lite or a half-glass leaf, in a wood veneer (maple) or a
# painted hollow metal finish (blue, sage), each with a stainless kick plate on both faces. Every
# leaf also comes NARROW (`_narrow`): an 8 px leaf hinged in the middle of its cell beside a fixed
# 8 px sidelite, the inactive leaf of a 1.5-leaf pair, which pairs with the full leaf of its own
# finish. finish -> (colour, veneer, name in each language).
HOSPITAL_FINISHES = {
    "maple": ((212, 172, 120), True, ("Maple", "Arce", "Ahorn", "Lönn")),
    "metal_blue": ((126, 154, 182), False, ("Blue Metal", "Metal Azul", "Metall Blau", "Blå Metall")),
    "metal_sage": ((146, 168, 136), False, ("Sage Metal", "Metal Salvia", "Metall Salbeigrün",
                                            "Salviagrön Metall")),
}
HOSPITAL_LEAVES = {
    "lite": ("Vision Lite", "Mirilla", "Lichtausschnitt", "Glasruta"),
    "halfglass": ("Half Glass", "Media Vidriera", "Halbverglast", "Halvglasad"),
}
NARROW_NAMES = ("Narrow Leaf", "Hoja Estrecha", "Schmaler Flügel", "Smalt Dörrblad")
HOSPITAL_NAMES = ("Hospital Door", "Puerta de Hospital", "Krankenhaustür", "Sjukhusdörr")

for _f, (_rgb, _veneer_on, _fn) in HOSPITAL_FINISHES.items():
    for _l, _ln in HOSPITAL_LEAVES.items():
        for _narrow in (False, True):
            _names = tuple("%s (%s, %s%s)" % (h, f, l, (", " + n) if _narrow else "")
                           for h, f, l, n in zip(HOSPITAL_NAMES, _fn, _ln, NARROW_NAMES))
            DOORS["door_hosp_%s_%s%s" % (_f, _l, "_narrow" if _narrow else "")] = (
                "hosp_%s%s" % (_l, "_narrow" if _narrow else ""), _rgb, True, _names)

# Cross-corridor smoke-compartment doors: hollow metal in neutral, red-free colours, a tall vision
# panel, a push plate and a pull, kick plates, and a closer that is always fitted. Placed as a
# pair across a corridor, held open by Life Safety's magnetic door holders through redstone, and
# shut on the closer as soon as the signal goes.
DOORS["door_smoke_metal_grey"] = ("smoke", (176, 178, 176), True, (
    "Smoke Barrier Door (Grey Metal)", "Puerta de Barrera de Humo (Metal Gris)",
    "Rauchschutztür (Metall Grau)", "Brandcellsdörr (Grå Metall)"))
DOORS["door_smoke_metal_beige"] = ("smoke", (200, 190, 168), True, (
    "Smoke Barrier Door (Beige Metal)", "Puerta de Barrera de Humo (Metal Beige)",
    "Rauchschutztür (Metall Beige)", "Brandcellsdörr (Beige Metall)"))
# A lead-lined (radiation shielding) door: a solid veneer leaf with a small lead-glass lite and a
# radiation trefoil plate on its outside face.
DOORS["door_lead_lined"] = ("lead", (150, 108, 72), True, (
    "Lead-Lined Door (Radiation)", "Puerta Plomada (Radiación)", "Bleitür (Strahlenschutz)",
    "Blyskyddsdörr (Strålning)"))

# Doors whose closer is always fitted (BlockBuildingDoor.builtInCloser). SHARED.
BUILT_IN_CLOSER = ("door_smoke_metal_grey", "door_smoke_metal_beige")

# A narrow leaf's hinge pivot, in px: the left-hinged leaf spans x 8..16, beside a fixed sidelite
# at x 0..8, and turns about (8.875, 15.125). SHARED with BlockBuildingDoor.pivotX.
NARROW_PIVOT_X = 8.875


def narrow(name):
    return name.endswith("_narrow")


def hosp_finish(name):
    """A hospital door's finish key, from door_hosp_<finish>_<leaf>[_narrow]."""
    rest = name[len("door_hosp_"):]
    return next(f for f in HOSPITAL_FINISHES if rest.startswith(f + "_"))


def full_leaf(name):
    """The full leaf a narrow leaf pairs with, whose edge texture it shares."""
    return name[:-len("_narrow")] if narrow(name) else name

CLOSER_NAMES = ("Door Closer", "Cierrapuertas", "Türschließer", "Dörrstängare")
SWING_TOOL_NAMES = ("Door Swing Tool", "Herramienta de Sentido de Puerta", "Türanschlagwerkzeug",
                    "Dörrvändningsverktyg")

# What the Door Swing Tool says: its tooltip, and the status line after a click.
SWING_LANG = {
    "gui.csm.door.swing_tool.tip": ("Right-click a shut door to make it swing the other way",
                                    "Clic derecho en una puerta cerrada para invertir su apertura",
                                    "Rechtsklick auf eine geschlossene Tür kehrt ihren Anschlag um",
                                    "Högerklicka på en stängd dörr för att vända dess öppning"),
    "gui.csm.door.swing_out": ("This door now swings out", "Esta puerta ahora abre hacia fuera",
                               "Diese Tür öffnet jetzt nach außen",
                               "Dörren öppnas nu utåt"),
    "gui.csm.door.swing_in": ("This door now swings in", "Esta puerta ahora abre hacia dentro",
                              "Diese Tür öffnet jetzt nach innen", "Dörren öppnas nu inåt"),
    "gui.csm.door.swing_busy": ("Shut the door and let it stop first",
                                "Cierra la puerta y espera a que se detenga",
                                "Erst die Tür schließen und stehen lassen",
                                "Stäng dörren och låt den stanna först"),
    "gui.csm.door.swing_fixed": ("This door's movement is set in the Door Workshop",
                                 "El movimiento de esta puerta se ajusta en el Taller de Puertas",
                                 "Die Bewegung dieser Tür wird in der Türwerkstatt eingestellt",
                                 "Dörrens rörelse ställs in i Dörrverkstaden"),
}

# The custom door (BlockCustomDoor, made in the Door Workshop): its name and the words its tooltip
# and the workshop's screen use.
CUSTOM_LANG = {
    "tile.custom_door.name": ("Custom Door", "Puerta Personalizada", "Individuelle Tür",
                              "Anpassad Dörr"),
    "gui.csm.door.tip.frame": ("Frame: %s", "Marco: %s", "Rahmen: %s", "Karm: %s"),
    "gui.csm.door.tip.upper": ("Upper: %s", "Superior: %s", "Oben: %s", "Övre: %s"),
    "gui.csm.door.tip.lower": ("Lower: %s", "Inferior: %s", "Unten: %s", "Nedre: %s"),
    "gui.csm.door.tip.movement": ("Opens: %s", "Apertura: %s", "Öffnet: %s", "Öppnas: %s"),
    "gui.csm.door.tip.sound": ("Sound: %s", "Sonido: %s", "Klang: %s", "Ljud: %s"),
    "gui.csm.door.tip.speed": ("Takes %s s", "Tarda %s s", "Dauert %s s", "Tar %s s"),
    "gui.csm.door.tip.autoclose": ("Closes itself after %s s", "Se cierra sola tras %s s",
                                   "Schließt sich nach %s s", "Stänger sig efter %s s"),
    "gui.csm.door.tip.redstone": ("Redstone: %s", "Redstone: %s", "Redstone: %s",
                                  "Rödsten: %s"),
    "gui.csm.door.tip.proximity": ("Opens when someone comes near",
                                   "Se abre al acercarse alguien",
                                   "Öffnet, wenn sich jemand nähert",
                                   "Öppnas när någon kommer nära"),
    "gui.csm.door.movement.swing": ("Swing", "Batiente", "Drehflügel", "Slag"),
    "gui.csm.door.movement.slide": ("Slide", "Corredera", "Schiebe", "Skjut"),
    "gui.csm.door.movement.slide_together": ("Slide Together", "Corredera Conjunta",
                                             "Gemeinsam Schieben", "Skjut Tillsammans"),
    "gui.csm.door.movement.slide_up": ("Slide Up", "Elevable", "Hochschieben", "Skjut Upp"),
    "gui.csm.door.movement.split": ("Split", "Partida", "Geteilt", "Delad"),
    "gui.csm.door.sound.wood": ("Wood", "Madera", "Holz", "Trä"),
    "gui.csm.door.sound.iron": ("Iron", "Hierro", "Eisen", "Järn"),
    "gui.csm.door.sound.heavy": ("Heavy", "Pesado", "Schwer", "Tung"),
    "gui.csm.door.sound.trapdoor": ("Trapdoor", "Trampilla", "Falltür", "Fallucka"),
    "gui.csm.door.sound.gate": ("Gate", "Portón", "Tor", "Grind"),
    "gui.csm.door.sound.pneumatic": ("Pneumatic", "Neumático", "Pneumatisch", "Pneumatisk"),
    "gui.csm.door.sound.sliding": ("Sliding", "Deslizante", "Gleitend", "Glidande"),
    "gui.csm.door.sound.silent": ("Silent", "Silencioso", "Lautlos", "Tyst"),
    "gui.csm.door.redstone.normal": ("Normal", "Normal", "Normal", "Normal"),
    "gui.csm.door.redstone.redstone_only": ("Redstone Only", "Solo Redstone", "Nur Redstone",
                                            "Endast Rödsten"),
    "gui.csm.door.redstone.hand_only": ("Hand Only", "Solo a Mano", "Nur von Hand",
                                        "Endast för Hand"),
    "gui.csm.door.redstone.redstone_lock": ("Redstone Locks", "Redstone Bloquea",
                                            "Redstone Verriegelt", "Rödsten Låser"),
    "gui.csm.door.redstone_only": ("This door opens by redstone only",
                                   "Esta puerta solo se abre con redstone",
                                   "Diese Tür öffnet nur mit Redstone",
                                   "Dörren öppnas bara med rödsten"),
    "gui.csm.door.locked_redstone": ("Locked by redstone", "Bloqueada por redstone",
                                     "Durch Redstone verriegelt", "Låst av rödsten"),
    "tile.door_workshop.name": ("Door Workshop", "Taller de Puertas", "Türwerkstatt",
                                "Dörrverkstad"),
    "gui.csm.workshop.make": ("Make", "Hacer", "Bauen", "Gör"),
    "gui.csm.workshop.apply": ("Set", "Fijar", "Setzen", "Ange"),
    "gui.csm.workshop.copy": ("Copy", "Copiar", "Kopie", "Kopia"),
    "gui.csm.workshop.save": ("Save", "Guardar", "Sichern", "Spara"),
    "gui.csm.workshop.load": ("Load", "Cargar", "Laden", "Ladda"),
    "gui.csm.workshop.delete": ("Del", "Borr", "Lösch", "Ta bort"),
    "gui.csm.workshop.design_has": ("Design: %s", "Diseño: %s", "Entwurf: %s", "Design: %s"),
    "gui.csm.workshop.sensor_on": ("Sensor: On", "Sensor: Sí", "Sensor: An", "Sensor: På"),
    "gui.csm.workshop.sensor_off": ("Sensor: Off", "Sensor: No", "Sensor: Aus", "Sensor: Av"),
    "gui.csm.workshop.speed": ("Opens in %s s", "Abre en %s s", "Öffnet in %s s",
                               "Öppnas på %s s"),
    "gui.csm.workshop.stays_open": ("Stays open", "Queda abierta", "Bleibt offen",
                                    "Förblir öppen"),
    "gui.csm.workshop.closes": ("Closes after %s s", "Cierra tras %s s", "Schließt nach %s s",
                                "Stänger efter %s s"),
    "gui.csm.workshop.slot.frame": ("Frame material", "Material del marco", "Rahmenmaterial",
                                    "Karmens material"),
    "gui.csm.workshop.slot.upper": ("Upper panel material", "Material del panel superior",
                                    "Material der oberen Füllung", "Övre panelens material"),
    "gui.csm.workshop.slot.lower": ("Lower panel material", "Material del panel inferior",
                                    "Material der unteren Füllung", "Nedre panelens material"),
    "gui.csm.workshop.slot.edit": ("Custom doors to re-program", "Puertas a reprogramar",
                                   "Türen zum Umprogrammieren", "Dörrar att programmera om"),
    "gui.csm.workshop.slot.output": ("Finished door", "Puerta terminada", "Fertige Tür",
                                     "Färdig dörr"),
}

# --------------------------------------------------------------------------------------------
# Textures -- a lower and an upper face for each door, outside and inside
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift
GLASS = (176, 204, 214, 110)
BRASS = (196, 160, 76)
SILVER = (192, 196, 200)


def _seed(name):
    return sum(ord(c) * (i + 5) for i, c in enumerate(name))


def _canvas(rgb, rng, amount):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rng.uniform(-amount, amount))
    return img, px


def _veneer(rgb, rng, px):
    """Wood veneer: grain running up the door."""
    for x in range(16):
        tone = rng.uniform(-9, 9)
        for y in range(16):
            px[x, y] = _shift(rgb, tone + rng.uniform(-3, 3))


def _glass(px, x0, y0, x1, y1, frame_rgb, wired=False, glass=GLASS):
    """A lite from (x0, y0) to (x1, y1) inclusive, in a one-pixel frame."""
    for y in range(y0 - 1, y1 + 2):
        for x in range(x0 - 1, x1 + 2):
            px[x, y] = _shift(frame_rgb, -16)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            g = glass
            if wired and ((x - x0) % 3 == 1 or (y - y0) % 3 == 1):
                g = (120, 124, 128, 200)
            px[x, y] = g


def _panel(px, rgb, x0, y0, x1, y1):
    """A raised panel's outline: lit top and left, shaded bottom and right."""
    for x in range(x0, x1 + 1):
        px[x, y0] = _shift(rgb, 14)
        px[x, y1] = _shift(rgb, -28)
    for y in range(y0, y1 + 1):
        px[x0, y] = _shift(rgb, 10)
        px[x1, y] = _shift(rgb, -22)


def door_faces(name):
    """(lower, upper) face textures of a door; the inside and outside are the same drawing."""
    style, rgb, _, _ = DOORS[name]
    rng = random.Random(_seed(name))
    lower, lp = _canvas(rgb, rng, 2)
    upper, up = _canvas(rgb, rng, 2)
    if style in ("wood", "wood_lite"):
        if sum(rgb) < 600:
            _veneer(rgb, rng, lp)
            _veneer(rgb, rng, up)
        if style == "wood_lite":
            _glass(up, 10, 2, 12, 12, rgb)
    elif style == "fire":
        _glass(up, 5, 3, 10, 12, rgb, wired=True)
    elif style == "storefront":
        for img_px, top in ((lp, False), (up, True)):
            for y in range(16):
                for x in range(16):
                    stile = x < 3 or x > 12
                    rail = (top and y < 3) or (not top and y > 11)
                    img_px[x, y] = _shift(rgb, rng.uniform(-3, 3)) if stile or rail else GLASS
    elif style == "front":
        # Six panels, as a real six-panel door has them: two short at the top, two tall in the
        # middle and two tall at the bottom, with the lock rail between the middle and bottom
        # pairs. The halves are separate textures, so every panel stays whole inside one of them
        # and the lock rail falls on the seam (the upper half's last row and the lower half's
        # first). A panel drawn across the seam reads as two, its outline closed at the edge of
        # each half (issue #221).
        _panel(up, rgb, 2, 1, 7, 5)
        _panel(up, rgb, 9, 1, 14, 5)
        _panel(up, rgb, 2, 7, 7, 14)
        _panel(up, rgb, 9, 7, 14, 14)
        _panel(lp, rgb, 2, 1, 7, 12)
        _panel(lp, rgb, 9, 1, 14, 12)
    elif style == "trailer":
        # A steel entry door: a stile down each side and a small wired-glass light up top.
        for img_px in (lp, up):
            for y in range(16):
                for x in (1, 14):
                    img_px[x, y] = _shift(rgb, -64 + rng.uniform(-3, 3))
        for x in range(1, 15):
            up[x, 1] = _shift(rgb, -64 + rng.uniform(-3, 3))
        _glass(up, 6, 4, 9, 9, rgb, wired=True)
    elif style == "halfglass":
        for gx in range(3):
            for gy in range(3):
                x0, y0 = 3 + gx * 4, 3 + gy * 4
                _glass(up, x0, y0, x0 + 2, y0 + 2, rgb)
        _panel(lp, rgb, 2, 2, 7, 13)
        _panel(lp, rgb, 9, 2, 14, 13)
    elif style.startswith("hosp_"):
        if HOSPITAL_FINISHES[hosp_finish(name)][1]:
            _veneer(rgb, rng, lp)
            _veneer(rgb, rng, up)
        # A narrow leaf is 8 px wide: its drawing is made twice, side by side, so that whichever
        # half of the texture a face's UVs fall on (they follow the face's position, and so its
        # hinge and swing), it shows the whole leaf.
        copies = (0, 8) if style.endswith("_narrow") else (None,)
        for ox in copies:
            if style.startswith("hosp_lite"):
                # A narrow vision lite, 4 in by 25 in, toward the latch.
                if ox is None:
                    _glass(up, 10, 6, 11, 14, rgb)
                else:
                    _glass(up, ox + 4, 6, ox + 5, 14, rgb)
            else:
                if ox is None:
                    _glass(up, 3, 2, 12, 14, rgb)
                else:
                    _glass(up, ox + 2, 2, ox + 5, 14, rgb)
    elif style == "smoke":
        # A tall vision panel of clear fire-rated glass.
        _glass(up, 4, 2, 11, 14, rgb)
        _glass(lp, 4, 0, 11, 1, rgb)
        for x in range(3, 13):
            lp[x, 0] = _shift(rgb, -16)
    elif style == "lead":
        _veneer(rgb, rng, lp)
        _veneer(rgb, rng, up)
        # A small lite of lead glass, greyer and darker than plain glass.
        _glass(up, 6, 3, 9, 7, rgb, glass=(120, 134, 126, 170))
    return lower, upper


# The fixed sidelite beside a narrow leaf: a light grey hollow metal frame round clear glass, with
# a solid panel below kick height. Drawn twice side by side, as the narrow leaf is.
SIDELITE_FRAME = (204, 204, 200)


def sidelite_faces():
    rng = random.Random(20261941)
    lower, lp = _canvas(SIDELITE_FRAME, rng, 2)
    upper, up = _canvas(SIDELITE_FRAME, rng, 2)
    for ox in (0, 8):
        for y in range(16):
            for x in range(ox + 1, ox + 7):
                up[x, y] = GLASS if y >= 1 else _shift(SIDELITE_FRAME, -10)
                if y < 9:
                    lp[x, y] = GLASS
                elif y == 9:
                    lp[x, y] = _shift(SIDELITE_FRAME, -16)
        for y in range(16):
            for img_px in (lp, up):
                img_px[ox, y] = _shift(SIDELITE_FRAME, 8)
                img_px[ox + 7, y] = _shift(SIDELITE_FRAME, -14)
    return lower, upper


def trefoil():
    """The radiation warning plate: a black trefoil on yellow, in a thin black border. The
    trefoil is the public-domain symbol: a disc, and three blades of 60 degrees each, 120 apart."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    yellow, black = (242, 200, 40), (24, 22, 20)
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, c - y
            r = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 360
            blade = any(abs((a - centre + 180) % 360 - 180) <= 30 for centre in (90, 210, 330))
            on = r <= 1.6 or (2.6 <= r <= 6.2 and blade)
            edge = x in (0, 15) or y in (0, 15)
            px[x, y] = black + (255,) if on or edge else yellow + (255,)
    return img


def edge(rgb, rng):
    return _canvas(_shift(rgb, -10)[:3], rng, 2)[0]


def hardware(rgb, rng):
    return _canvas(rgb, rng, 4)[0]


def door_icon(lower, upper):
    """The door as an item: both halves squeezed into a door-shaped 8 x 16 in the middle of the
    slot, as vanilla's door items are drawn."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    up = upper.resize((8, 8), Image.NEAREST)
    lo = lower.resize((8, 8), Image.NEAREST)
    img.paste(up, (4, 0))
    img.paste(lo, (4, 8))
    return img


def closer_icon():
    """The Door Closer item: the silver body and its arm."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(2, 11):
        for y in range(8, 12):
            px[x, y] = _shift(SILVER, 16 if y == 8 else (-24 if y == 11 else 0))
    for i in range(6):
        px[9 + i // 2, 7 - i] = _shift(SILVER, -10)
    for x in range(8, 15):
        px[x, 2] = _shift(SILVER, 0)
    return img


def swing_tool_icon():
    """The Door Swing Tool item: a door as a plan shows one -- the wall either side, the leaf
    standing open from its hinge and the arc it swings through -- with an arrowhead at both ends
    of the arc, since the tool turns the swing round."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    wall = (128, 132, 138)
    leaf_rgb = (184, 142, 96)
    for x in list(range(0, 3)) + list(range(13, 16)):
        for y in (13, 14):
            px[x, y] = _shift(wall, 10 if y == 13 else -14)
    for y in range(4, 15):
        px[3, y] = _shift(leaf_rgb, 12)
        px[4, y] = _shift(leaf_rgb, -18)
    for step in range(200):
        a = math.radians(78 * step / 199)
        x, y = round(4 + 8 * math.cos(a)), round(13 - 8 * math.sin(a))
        px[x, y] = _shift(SILVER, 20)
    # The arrowheads: pointing down the wall at one end of the arc, back at the leaf at the other.
    for x, y in ((11, 12), (13, 12), (10, 11), (14, 11), (7, 4), (8, 4), (7, 7)):
        px[x, y] = _shift(SILVER, 0)
    return img


OAK = (162, 130, 78)
STEEL = (120, 124, 130)


def _planks(rng, rgb=OAK):
    """Oak boards running across, a seam every four rows."""
    img, px = _canvas(rgb, rng, 6)
    for y in (3, 7, 11, 15):
        for x in range(16):
            px[x, y] = _shift(rgb, -34)
    return img, px


def workshop_top():
    """The bench top: boards, with a steel vice at the front edge."""
    img, px = _planks(random.Random(20261911))
    for x in range(5, 11):
        for y in range(12, 16):
            px[x, y] = _shift(STEEL, 14 if y == 12 else (-18 if y == 15 else 0))
    for x in range(7, 9):
        px[x, 11] = _shift(STEEL, -30)
    return img


def workshop_side():
    """Boards over a drawer, the bench's cabinet."""
    img, px = _planks(random.Random(20261912))
    for x in range(16):
        px[x, 0] = _shift(OAK, -44)
    for x in range(2, 14):
        for y in (8, 13):
            px[x, y] = _shift(OAK, -40)
    for y in range(8, 14):
        px[2, y] = _shift(OAK, -40)
        px[13, y] = _shift(OAK, -40)
    px[7, 10] = px[8, 10] = _shift(BRASS, 0)
    return img


def workshop_front():
    """A pegboard with a little door hung on it, a saw and a hammer: what the block is for."""
    rng = random.Random(20261913)
    board = (150, 118, 76)
    img, px = _canvas(board, rng, 3)
    for y in range(1, 16, 3):
        for x in range(1, 16, 3):
            px[x, y] = _shift(board, -46)
    for x in range(16):
        px[x, 0] = px[x, 15] = _shift(OAK, -44)
    for y in range(16):
        px[0, y] = px[15, y] = _shift(OAK, -44)
    # The door: a frame and two panels, its knob.
    for y in range(3, 14):
        for x in range(2, 8):
            edge_px = x in (2, 7) or y in (3, 13)
            px[x, y] = _shift((116, 78, 46), -20 if edge_px else (8 if y < 8 else 0))
    for x in range(3, 7):
        px[x, 8] = _shift((116, 78, 46), -30)
    px[6, 9] = _shift(BRASS, 10)
    # The saw: a blade and its handle.
    for i in range(6):
        for j in range(2):
            px[9 + i, 4 + j] = _shift(SILVER, 10 - j * 30)
    px[9, 6] = px[10, 6] = px[11, 6] = _shift(SILVER, -40)
    for x in range(13, 15):
        for y in range(3, 7):
            px[x, y] = _shift((120, 40, 30), 0)
    # The hammer: a head across a handle.
    for y in range(9, 14):
        px[12, y] = _shift((110, 76, 44), 0)
    for x in range(10, 15):
        px[x, 9] = _shift(STEEL, 0)
    return img


def workshop_textures():
    return {"door_workshop_top": workshop_top(), "door_workshop_side": workshop_side(),
            "door_workshop_front": workshop_front()}


def workshop_model():
    return {"parent": "block/orientable",
            "textures": {"top": TEX_REF % "door_workshop_top",
                         "side": TEX_REF % "door_workshop_side",
                         "front": TEX_REF % "door_workshop_front"}}


def workshop_state():
    variants = {"facing=%s" % side: {"model": MODEL_REF % "door_workshop",
                                     **({"y": turn} if turn else {})}
                for side, turn in SIDES}
    variants["inventory"] = {"model": MODEL_REF % "door_workshop"}
    return {"variants": variants}


def textures():
    out = {}
    side_lower, side_upper = sidelite_faces()
    for name, (style, rgb, _, _) in DOORS.items():
        lower, upper = door_faces(name)
        out[name + "_lower"] = lower
        out[name + "_upper"] = upper
        if narrow(name):
            # The icon: the sidelite and the leaf beside it, as the pair's narrow half stands.
            il, iu = lower.copy(), upper.copy()
            il.paste(side_lower.crop((0, 0, 8, 16)), (0, 0))
            iu.paste(side_upper.crop((0, 0, 8, 16)), (0, 0))
            out[name + "_icon"] = door_icon(il, iu)
            continue
        out[name + "_icon"] = door_icon(lower, upper)
        out[name + "_edge"] = edge(rgb, random.Random(_seed(name) + 1))
    out["door_brass"] = hardware(BRASS, random.Random(20261901))
    out["door_silver"] = hardware(SILVER, random.Random(20261902))
    out["door_black"] = hardware((34, 34, 36), random.Random(20261903))
    out["door_hosp_sidelite_lower"] = side_lower
    out["door_hosp_sidelite_upper"] = side_upper
    out["door_hm_frame"] = hardware(SIDELITE_FRAME, random.Random(20261942))
    out["door_trefoil"] = trefoil()
    out.update(workshop_textures())
    return out

# --------------------------------------------------------------------------------------------
# Models -- left hinge, closed, inside to the north
# --------------------------------------------------------------------------------------------

box = sc._box
Z0, Z1 = 16 - LEAF, 16.0


def planes(name):
    """(inside face, outside face) of a door's leaf: on the outside face of the cell for a door that
    swings in, on the inside face for one that swings out."""
    return (0.0, LEAF) if name in OUTSWING else (Z0, Z1)


def _tint(elements, index):
    for e in elements:
        for spec in e["faces"].values():
            spec["tintindex"] = index
    return elements


def leaf(upper, z0=Z0, z1=Z1):
    """The door leaf's half: outside face south, inside face north."""
    el = box(0, 0, z0, 16, 16, z1, "#edge")
    el["faces"]["south"]["texture"] = "#face"
    el["faces"]["north"]["texture"] = "#face"
    return [el]


def storefront_leaf(upper, z0=Z0, z1=Z1):
    """A storefront leaf: the frame solid, the glass a thin pane in the middle of it."""
    rails = [box(0, 0, z0, 3, 16, z1, "#face"), box(13, 0, z0, 16, 16, z1, "#face")]
    if upper:
        rails.append(box(3, 13, z0, 13, 16, z1, "#face"))
    else:
        rails.append(box(3, 0, z0, 13, 4, z1, "#face"))
    y0, y1 = (0, 13) if upper else (4, 16)
    pane = box(3, y0, z0 + 0.5, 13, y1, z0 + 1.25, "#face", faces=("north", "south"))
    return rails + [pane]


def lever(z_in, z_out):
    """A lever handle on both faces, on the latch side (x high, for a left hinge)."""
    return [box(13, 13.5, z_in - 1.5, 14, 14.5, z_in, "#hw"),
            box(10.5, 13.5, z_in - 2, 14, 14.5, z_in - 1.25, "#hw"),
            box(13, 13.5, z_out, 14, 14.5, z_out + 1.5, "#hw"),
            box(10.5, 13.5, z_out + 1.25, 14, 14.5, z_out + 2, "#hw")]


def knob(z_in, z_out):
    return [box(12.5, 12.75, z_in - 1.75, 14.25, 14.5, z_in, "#hw"),
            box(12.5, 12.75, z_out, 14.25, 14.5, z_out + 1.75, "#hw")]


def push_bar(z_in):
    """An exit device across the inside face at waist height, with its end cases. The touch bar
    is marked TINT_PUSH: the renderer dips it toward the leaf as the door is pushed open."""
    return _tint([box(2, 13, z_in - 1.25, 14, 14.25, z_in, "#hw")], TINT_PUSH) + [
        box(1, 12.5, z_in - 1.75, 3, 14.75, z_in, "#hw"),
        box(12.5, 12.25, z_in - 2, 15, 15, z_in, "#hw")]


def pull_handle(z_out):
    """A storefront door's tall pull handle outside, across both halves (this is the lower's
    part; the upper draws the rest)."""
    return [box(12, 8, z_out + 1.5, 13, 16, z_out + 2.25, "#hw"),
            box(12, 8, z_out, 13, 8.75, z_out + 1.5, "#hw")]


def pull_handle_top(z_out):
    return [box(12, 0, z_out + 1.5, 13, 6, z_out + 2.25, "#hw"),
            box(12, 5.25, z_out, 13, 6, z_out + 1.5, "#hw")]


KICK_H = 4.0


def kick_plates(x0, z_in, z_out):
    """A stainless kick plate across the foot of each face, 10 in high, standing a quarter pixel
    proud of the leaf."""
    faces = _but_faces("north")
    out = [box(x0, 0, z_out, 16 - 0.5, KICK_H, z_out + 0.25, "#hw", faces=faces)]
    back = _but_faces("south")
    out.append(box(x0, 0, z_in - 0.25, 16 - 0.5, KICK_H, z_in, "#hw", faces=back))
    return out


def _but_faces(*leave):
    return tuple(f for f in ("north", "south", "east", "west", "up", "down") if f not in leave)


def push_plate(z_out):
    """A smoke door's stainless push plate on the face it is pushed from, latch side."""
    return [box(10.5, 9, z_out, 14.5, 15, z_out + 0.25, "#hw", faces=_but_faces("north"))]


def pull_bar(z_in):
    """And its pull on the other face: a vertical bar on two standoffs."""
    return [box(12.5, 7, z_in - 1.75, 13.5, 15, z_in - 1, "#hw"),
            box(12.5, 7.5, z_in - 1, 13.5, 8.5, z_in, "#hw"),
            box(12.5, 13.5, z_in - 1, 13.5, 14.5, z_in, "#hw")]


def trefoil_plate(z_face, outward=1):
    """The radiation warning plate on the leaf's outside face (z_face, standing off it toward
    `outward`), below the lead-glass lite."""
    z0, z1 = (z_face, z_face + 0.2) if outward > 0 else (z_face - 0.2, z_face)
    back = "north" if outward > 0 else "south"
    return [box(5.5, 1.5, z0, 10.5, 6.5, z1, "#plate", faces=_but_faces(back))]


def hardware_for(style, upper, z_in=Z0, z_out=Z1):
    """The hardware on a leaf whose inside face is at z_in and outside face at z_out: the push bar
    goes on the inside, the side a person leaving pushes from."""
    if style.startswith("hosp_") or style in ("smoke", "lead"):
        if upper:
            return []
        x0 = 10.0 if style.endswith("_narrow") else 0.5
        kick = kick_plates(x0, z_in, z_out)
        if style.endswith("_narrow"):
            return kick
        if style == "smoke":
            return kick + push_plate(z_out) + pull_bar(z_in)
        return kick + lever(z_in, z_out)
    if upper:
        return pull_handle_top(z_out) if style == "storefront" else []
    if style == "exit":
        return push_bar(z_in)
    if style == "storefront":
        return push_bar(z_in) + pull_handle(z_out)
    if style in ("front", "halfglass"):
        return knob(z_in, z_out)
    return lever(z_in, z_out)


# --- the door closer: a body on the leaf, a shoe on the wall, and the arm between them ---
#
# Drawn for a left-hinged inswing door; the rest are its mirrors. It sits on the PUSH side of the
# leaf, the side the door swings away from -- here the outside face, z = 16 -- because the other
# face turns to the jamb as the door opens: the leaf opens within the wall's thickness, so an arm
# from a closer on the pull face would have to pass through the open leaf to reach the frame. On
# the push face the body turns into the opening with the leaf and the arm reaches it there.


def closer_body():
    """The closer's body, on the push face near the top of the leaf by the hinge, and the spindle
    post the arm turns on. These ride on the leaf."""
    sx, sz = CLOSER_SPINDLE
    return [box(1, 11.5, Z1, 7, 14, Z1 + 2.25, "#hw"),
            box(7, 12, Z1 + 0.25, 7.5, 13.5, Z1 + 2, "#hw"),
            box(sx - 0.6, 14, sz - 0.6, sx + 0.6, 15.25, sz + 0.6, "#hw")]


def closer_shoe():
    """The shoe the forearm turns in, on a plate on the wall face above the opening: fixed to the
    frame, so it is marked TINT_FIXED and the renderer never swings it."""
    px, pz = CLOSER_SHOE
    return _tint([box(px - 1.5, 16, Z1, px + 1.5, 17.5, Z1 + 0.5, "#hw"),
                  box(px - 1, 15.25, Z1 + 0.25, px + 1, 16.25, pz + 1, "#hw")], TINT_FIXED)


def closer_joints(turn):
    """(spindle, elbow) with the door turned `turn` degrees open (0 shut, 90 open), in px.

    The spindle turns with the leaf about PIVOT; the elbow is where a circle of CLOSER_MAIN about
    it meets one of CLOSER_FORE about the shoe, on the CLOSER_ELBOW_SIDE of the line between them.
    SHARED with DoorCloserArm.solve, which the renderer draws every frame of a swing from."""
    a = math.radians(turn)
    hx, hz = PIVOT
    x, z = CLOSER_SPINDLE[0] - hx, CLOSER_SPINDLE[1] - hz
    sx, sz = hx + x * math.cos(a) + z * math.sin(a), hz - x * math.sin(a) + z * math.cos(a)
    px, pz = CLOSER_SHOE
    dx, dz = px - sx, pz - sz
    d = math.hypot(dx, dz)
    along = (CLOSER_MAIN ** 2 - CLOSER_FORE ** 2 + d * d) / (2 * d)
    h = math.sqrt(CLOSER_MAIN ** 2 - along ** 2)
    mx, mz = sx + along * dx / d, sz + along * dz / d
    side = CLOSER_ELBOW_SIDE
    return (sx, sz), (mx - side * h * dz / d, mz + side * h * dx / d)


def _link(a, b, y0, y1, half):
    """A straight link from a to b (x, z) at y0..y1, `half` either side of the line: an element
    along one axis, turned about a to the link's angle. Only multiples of 22.5 degrees can be
    drawn that way, which is what the arm's lengths were chosen for."""
    ax, az = a
    phi = math.degrees(math.atan2(b[1] - az, b[0] - ax))
    snap = round(phi / 22.5) * 22.5
    assert abs(phi - snap) < 0.05, "a closer link at %.3f degrees cannot be drawn" % phi
    phi = (snap + 180) % 360 - 180
    length = math.hypot(b[0] - ax, b[1] - az)
    r = lambda v: round(v, 4)
    # An element turned by `angle` about +y carries +x to (cos, -sin): pick the axis the link
    # runs nearest to, and the turn left over is within the 45 degrees an element allows.
    if -45 <= phi <= 45:
        el = box(r(ax), y0, r(az - half), r(ax + length), y1, r(az + half), "#hw")
        angle = -phi
    elif 45 < phi < 135:
        el = box(r(ax - half), y0, r(az), r(ax + half), y1, r(az + length), "#hw")
        angle = 90 - phi
    elif -135 < phi < -45:
        el = box(r(ax - half), y0, r(az - length), r(ax + half), y1, r(az), "#hw")
        angle = -90 - phi
    else:
        el = box(r(ax - length), y0, r(az - half), r(ax), y1, r(az + half), "#hw")
        angle = (180 - phi + 180) % 360 - 180
    if angle:
        el["rotation"] = {"origin": [r(ax), y0, r(az)], "axis": "y", "angle": angle}
    return el


# SHARED with DoorCloserArm: the links' heights and half widths, and the elbow pin's, in px.
MAIN_Y, FORE_Y, PIN_Y = (14.5, 15.0), (15.0, 15.5), (14.375, 15.625)
LINK_HALF, PIN_HALF = 0.5, 0.625


def closer_arm(turn):
    """The arm at `turn` degrees open: the main arm from the spindle to the elbow, the forearm from
    the elbow to the shoe, and the elbow's pin. The renderer draws its own at every angle between
    (TINT_ARM), so these are only ever seen shut and open."""
    s, e = closer_joints(turn)
    pin = box(round(e[0] - PIN_HALF, 4), PIN_Y[0], round(e[1] - PIN_HALF, 4),
              round(e[0] + PIN_HALF, 4), PIN_Y[1], round(e[1] + PIN_HALF, 4), "#hw")
    return _tint([_link(s, e, MAIN_Y[0], MAIN_Y[1], LINK_HALF),
                  _link(e, CLOSER_SHOE, FORE_Y[0], FORE_Y[1], LINK_HALF), pin], TINT_ARM)


def _rounded(elements):
    """The arm's odd lengths leave float noise (7.968299999999999) after a mirror: round it off."""
    for e in elements:
        for key in ("from", "to"):
            e[key] = [round(v, 4) for v in e[key]]
        if "rotation" in e:
            e["rotation"]["origin"] = [round(v, 4) for v in e["rotation"]["origin"]]
    return elements


def closer_models():
    """(left shut, left open) of the closer for a left-hinged inswing door."""
    return (closer_body() + closer_shoe() + closer_arm(0),
            _open(closer_body()) + closer_shoe() + closer_arm(90))

# --- the open pose: the closed model turned a quarter about the hinge pivot ---


FACE_TURN = {"south": "east", "north": "west", "east": "north", "west": "south",
             "up": "up", "down": "down"}


def _open(elements):
    """Turn a left-hinged closed model to its open pose: (x, z) -> (z - 14.25, 16 - x), which is a
    quarter turn anticlockwise (seen from above) about PIVOT."""
    out = []
    for e in elements:
        m = json.loads(json.dumps(e))
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        m["from"] = [round(z0 - Z0, 4), y0, round(16 - x1, 4)]
        m["to"] = [round(z1 - Z0, 4), y1, round(16 - x0, 4)]
        faces = {}
        for face, spec in e["faces"].items():
            faces[FACE_TURN[face]] = dict(spec)
        m["faces"] = faces
        out.append(sc._reuv(m))
    return out


def _open_about(elements, px):
    """_open about a pivot at (px, PIVOT[1]): a narrow leaf's, hinged in the middle of its cell.
    (x, z) -> (z - pz + px, pz + px - x)."""
    pz = PIVOT[1]
    out = []
    for e in elements:
        m = json.loads(json.dumps(e))
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        m["from"] = [round(z0 - pz + px, 4), y0, round(pz + px - x1, 4)]
        m["to"] = [round(z1 - pz + px, 4), y1, round(pz + px - x0, 4)]
        m["faces"] = {FACE_TURN[face]: dict(spec) for face, spec in e["faces"].items()}
        out.append(sc._reuv(m))
    return out


def narrow_leaf(z0=Z0, z1=Z1):
    """A narrow leaf (x 8..16, hinged at x 8) and the fixed sidelite beside it (x 0..8), which is
    marked TINT_FIXED: the swing renderer draws it where it is."""
    el = box(8, 0, z0, 16, 16, z1, "#edge")
    el["faces"]["south"]["texture"] = "#face"
    el["faces"]["north"]["texture"] = "#face"
    side = box(0, 0, z0, 8, 16, z1, "#frame")
    side["faces"]["south"]["texture"] = "#side"
    side["faces"]["north"]["texture"] = "#side"
    return [el], _tint([side], TINT_FIXED)


def _open_out(elements):
    """Turn a left-hinged outswing door's closed model to its open pose: the depth mirror of
    _open, a quarter turn clockwise (seen from above) about (0.875, 0.875), which carries the leaf
    from the inside face to lie along the jamb with its latch edge to the south: (x, z) ->
    (1.75 - z, x). Every door texture draws the same on its two broad faces and its two edges, so
    mirroring the faces' positions without swapping their names is safe here."""
    return sc._mirror_z(_open(sc._mirror_z(elements)))


def reversed_suffix(name):
    """What a reversed door's models are named for: the way it then swings."""
    return "_in" if name in OUTSWING else "_out"


def _model(elements, textures):
    t = dict(textures)
    t["particle"] = next(iter(textures.values()))
    return {"textures": t, "elements": elements}


def _hw_tex(style):
    if style in ("front", "halfglass"):
        return TEX_REF % "door_brass"
    if style == "storefront":
        return TEX_REF % "door_silver"
    return TEX_REF % ("door_black" if style in ("metal",) else "door_silver")


def models():
    out = {}
    for name, (style, _, _, _) in DOORS.items():
        z_in, z_out = planes(name)
        turn = _open_out if name in OUTSWING else _open
        for half in ("lower", "upper"):
            upper = half == "upper"
            fixed = []
            if narrow(name):
                els, fixed = narrow_leaf(z_in, z_out)
                turn = lambda e: _open_about(e, NARROW_PIVOT_X)  # noqa: E731
            elif style == "storefront":
                els = storefront_leaf(upper, z_in, z_out)
            else:
                els = leaf(upper, z_in, z_out)
            els = els + hardware_for(style, upper, z_in, z_out)
            tex = {"face": TEX_REF % (name + "_" + half),
                   "edge": TEX_REF % (full_leaf(name) + "_edge"), "hw": _hw_tex(style)}
            if narrow(name):
                tex["side"] = TEX_REF % ("door_hosp_sidelite_" + half)
                tex["frame"] = TEX_REF % "door_hm_frame"
            plate = upper and style == "lead"
            if plate:
                tex["plate"] = TEX_REF % "door_trefoil"
            # The leaf, and what is fixed to the frame beside it (a narrow leaf's sidelite), which
            # the open models keep where it is.
            left = els + (trefoil_plate(z_out) if plate else []) + fixed
            left_open = turn(els + (trefoil_plate(z_out) if plate else [])) + fixed
            right = sc._mirror_x(left)
            out["%s_%s_left" % (name, half)] = _model(left, tex)
            out["%s_%s_right" % (name, half)] = _model(right, tex)
            out["%s_%s_left_open" % (name, half)] = _model(left_open, tex)
            out["%s_%s_right_open" % (name, half)] = _model(sc._mirror_x(left_open), tex)
            # Reversed: the depth mirror of every one of those, so the leaf hangs on the other face
            # of the cell and opens the other way, and the hardware goes with it (a push bar stays
            # on the face that is pushed).
            rev = reversed_suffix(name)
            if plate:
                # A sign plate stays on the outside, whichever way the door swings: the leaf is
                # mirrored to the inside face and the plate goes on its outside face there.
                shut = _rounded(sc._mirror_z(els) + trefoil_plate(LEAF))
                for hinge, flip in (("left", lambda e: e), ("right", sc._mirror_x)):
                    out["%s_%s_%s%s" % (name, half, hinge, rev)] = _model(
                        _rounded(flip(shut)), tex)
                    out["%s_%s_%s%s_open" % (name, half, hinge, rev)] = _model(
                        _rounded(flip(_open_out(shut))), tex)
                continue
            for hinge, shut in (("left", left), ("right", right)):
                opened = left_open if hinge == "left" else sc._mirror_x(left_open)
                out["%s_%s_%s%s" % (name, half, hinge, rev)] = _model(
                    _rounded(sc._mirror_z(shut)), tex)
                out["%s_%s_%s%s_open" % (name, half, hinge, rev)] = _model(
                    _rounded(sc._mirror_z(opened)), tex)
        out[name + "_inventory"] = {
            "parent": "item/generated",
            "textures": {"layer0": TEX_REF % (name + "_icon")}}
    hw = {"hw": TEX_REF % "door_silver"}
    # An outswing door's closer is the depth mirror of an inswing door's, as its leaf is: still on
    # the push face, which is now the inside, with the shoe on the room side of the wall above.
    shut, open_ = closer_models()
    for prefix, flip in (("closer", lambda els: els), ("closer_out", sc._mirror_z)):
        out[prefix + "_left"] = _model(_rounded(flip(shut)), hw)
        out[prefix + "_right"] = _model(_rounded(sc._mirror_x(flip(shut))), hw)
        out[prefix + "_left_open"] = _model(_rounded(flip(open_)), hw)
        out[prefix + "_right_open"] = _model(_rounded(sc._mirror_x(flip(open_))), hw)
    out["door_workshop"] = workshop_model()
    return out

# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def state_for(name):
    """The door's multipart blockstate: a leaf for each facing, half, hinge, open and reversed, and
    the closer on the push side of the way the door then swings. Nothing draws while it swings."""
    parts = []
    for side, rot in SIDES:
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for open_ in (False, True):
                    for reversed_ in (False, True):
                        rev = reversed_suffix(name) if reversed_ else ""
                        model = "%s_%s_%s%s%s" % (name, half, hinge, rev,
                                                  "_open" if open_ else "")
                        when = {"facing": side, "half": half, "hinge": hinge,
                                "open": str(open_).lower(), "reversed": str(reversed_).lower(),
                                "swing": "false"}
                        apply = {"model": MODEL_REF % model}
                        if rot:
                            apply["y"] = rot
                        parts.append({"when": when, "apply": apply})
                        # A narrow leaf takes no closer: its inactive leaf is held by bolts.
                        if half == "upper" and not narrow(name):
                            outward = (name in OUTSWING) != reversed_
                            closer = "closer_out_%s%s" if outward else "closer_%s%s"
                            c = dict(when)
                            c["closer"] = "true"
                            ca = {"model": MODEL_REF % (closer % (hinge, "_open" if open_
                                                                  else ""))}
                            if rot:
                                ca["y"] = rot
                            parts.append({"when": c, "apply": ca})
    return {"variants": {"inventory": {"model": MODEL_REF % (name + "_inventory")}},
            "multipart": parts}


def blockstates():
    out = {name: state_for(name) for name in DOORS}
    out["door_workshop"] = workshop_state()
    return out

# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------


def write_all(tex_dir, model_dir, state_dir, item_tex_dir, item_model_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(item_tex_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    closer_icon().save(os.path.join(item_tex_dir, "door_closer.png"))
    written.append(("itemtex", "door_closer.png"))
    swing_tool_icon().save(os.path.join(item_tex_dir, "door_swing_tool.png"))
    written.append(("itemtex", "door_swing_tool.png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    gen_cmu._write_json(os.path.join(item_model_dir, "door_closer.json"),
                        {"parent": "item/generated",
                         "textures": {"layer0": "csm:items/door_closer"}})
    written.append(("itemmodel", "door_closer.json"))
    gen_cmu._write_json(os.path.join(item_model_dir, "door_swing_tool.json"),
                        {"parent": "item/generated",
                         "textures": {"layer0": "csm:items/door_swing_tool"}})
    written.append(("itemmodel", "door_swing_tool.json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    model_depth.separate_dirs([(tex_dir, TEX_DIR), (model_dir, MODEL_DIR), (state_dir, STATE_DIR), (item_tex_dir, ITEM_TEX_DIR), (item_model_dir, ITEM_MODEL_DIR)], written, gen_cmu._write_json)
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    out = [("tile.%s.name" % name, dict(zip(LANGS, names)))
           for name, (_, _, _, names) in DOORS.items()]
    out.append(("item.door_closer.name", dict(zip(LANGS, CLOSER_NAMES))))
    out.append(("item.door_swing_tool.name", dict(zip(LANGS, SWING_TOOL_NAMES))))
    out += [(k, dict(zip(LANGS, v))) for k, v in SWING_LANG.items()]
    out += [(k, dict(zip(LANGS, v))) for k, v in CUSTOM_LANG.items()]
    return out


def tab_lines():
    lines = ['    initTabBlock(new BlockBuildingDoor("%s")); // %s' % (n, v[3][0])
             for n, v in DOORS.items() if n.startswith("door_")]
    lines.append("    initTabItem(ItemDoorCloser.class, fmlPreInitializationEvent); // Door Closer")
    lines.append("    initTabItem(ItemDoorSwingTool.class, fmlPreInitializationEvent);"
                 " // Door Swing Tool")
    return lines


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines += ["# tab registration"] + tab_lines()
    return "\n".join(lines)


def _check():
    import filecmp
    import shutil
    import tempfile
    tmp = tempfile.mkdtemp(prefix="csm_doors_")
    try:
        keys = ("tex", "model", "state", "itemtex", "itemmodel")
        real = dict(zip(keys, (TEX_DIR, MODEL_DIR, STATE_DIR, ITEM_TEX_DIR, ITEM_MODEL_DIR)))
        t = {k: os.path.join(tmp, k) for k in keys}
        for d in t.values():
            os.makedirs(d, exist_ok=True)
        written = write_all(t["tex"], t["model"], t["state"], t["itemtex"], t["itemmodel"])
        drifted = [os.path.join(real[k], f) for k, f in written
                   if not os.path.exists(os.path.join(real[k], f))
                   or not filecmp.cmp(os.path.join(real[k], f), os.path.join(t[k], f),
                                      shallow=False)]
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + os.path.relpath(path, REPO))
            return 1
        print("%d generated door files are up to date" % len(written))
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
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR, ITEM_TEX_DIR, ITEM_MODEL_DIR)
    print("Wrote %d door files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
