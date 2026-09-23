#!/usr/bin/env python3
"""
gen_emergency_lighting.py -- the emergency lights in the Exits & Emergency Lighting tab.

The twin-head "bug-eye" units in three housings, an LED bar, remote heads (single and twin), an
outdoor wall pack and a recessed ceiling downlight. Every one is a BlockEmergencyLightFactory:
redstone is mains power, and without it the lamps light. The lamps' lenses swap to a lit texture
on the blockstate's powered=false variant, and the lamp boxes here are the ones the tab line
hands the block, so the glow the renderer draws in front of each lamp sits on the lamp the model
draws.

Writes, under the Life Safety module's assets/csm:

  * textures/blocks/lifesafety/emergencylighting/*.png
  * models/block/lifesafety/emergencylighting/*.json
  * blockstates/<registry>.json
  * the tile names in all four lang files

Usage:
    python gen_emergency_lighting.py              # write everything
    python gen_emergency_lighting.py --check      # fail if the tree has drifted
    python gen_emergency_lighting.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from life_safety_gen_common import (  # noqa: E402
    Catalogue, box, disc, fill, model, nsewud_state, pipe_z, post, rect, shade)

C = Catalogue("gen_emergency_lighting.py", "lifesafety/emergencylighting",
              "lifesafety/emergencylighting")
T = C.T
M = C.M

HOUSINGS = {"white": (232, 232, 226), "black": (40, 40, 42), "gray": (150, 152, 156)}
BRONZE = (74, 60, 44)
LENS_OFF = (214, 212, 196)
LENS_ON = (255, 252, 226)


def flat(name, colour, grain=3, seed=1):
    C.textures[name] = lambda: fill(colour, grain=grain, seed=seed)


for i, (name, colour) in enumerate(HOUSINGS.items()):
    flat("housing_" + name, colour, seed=10 + i)
flat("bronze", BRONZE, seed=20)


def housing_front(colour, seed):
    """The unit's face: vents, the green charge lamp and the test button."""
    def draw():
        img = fill(colour, grain=3, seed=seed)
        for x in range(4, 12, 2):
            rect(img, x, 6, x + 1, 9, shade(colour, 0.7))
        img.load()[4, 10] = (60, 230, 90, 255)
        rect(img, 10, 10, 12, 11, (200, 40, 40))
        return img
    return draw


for i, (name, colour) in enumerate(HOUSINGS.items()):
    C.textures["front_" + name] = housing_front(colour, 30 + i)


def lens(lit):
    """A lamp's lens seen head on: a reflector ring and the stippled lens, lit or dark."""
    def draw():
        base = LENS_ON if lit else LENS_OFF
        img = fill(shade(base, 0.8), grain=2, seed=40 + lit)
        disc(img, 8, 8, 7, base)
        px = img.load()
        for y in range(16):
            for x in range(16):
                if (x + y) % 3 == 0 and math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 6:
                    r, g, b, a = px[x, y]
                    px[x, y] = shade((r, g, b), 0.93 if not lit else 1.0) + (a,)
        if lit:
            disc(img, 8, 8, 3, (255, 255, 250))
        return img
    return draw


C.textures["lens_off"] = lens(False)
C.textures["lens_on"] = lens(True)


def strip(lit):
    def draw():
        img = fill((60, 62, 66), grain=2, seed=50)
        for x in range(0, 16, 2):
            rect(img, x, 6, x + 1, 10, (255, 250, 230) if lit else (150, 150, 140))
        return img
    return draw


C.textures["strip_off"] = strip(False)
C.textures["strip_on"] = strip(True)


def prism(lit):
    """A wall pack's prismatic lens: ribbed glass."""
    def draw():
        base = LENS_ON if lit else (170, 170, 160)
        img = fill(base, grain=2, seed=60 + lit)
        for y in range(0, 16, 3):
            rect(img, 0, y, 16, y + 1, shade(base, 0.8))
        return img
    return draw


C.textures["prism_off"] = prism(False)
C.textures["prism_on"] = prism(True)


def B6(*v):
    return "new float[]{%s}" % ", ".join("%gF" % x for x in v)


def light(reg, bbox, names, textures, elements, bulbs, lens_key="lens",
          on="lens_on", off="lens_off"):
    textures = dict(textures)
    textures[lens_key] = T(off)
    state = nsewud_state(M(reg))
    state["variants"]["powered"] = {"true": {}, "false": {"textures": {lens_key: T(on)}}}
    java = ('new BlockEmergencyLightFactory("%s", new AxisAlignedBB(%s), new float[][]{%s})'
            % (reg, ", ".join("%.6f" % (v / 16.0) for v in bbox),
               ", ".join(B6(*b) for b in bulbs)))
    C.add(reg, java, names, {reg: model(textures, elements)}, state, tab="Exits & Emergency Lighting")


# --- twin-head units --------------------------------------------------------------------------
NAMES = {
    "white": ("Twin-Head Emergency Light (White)", "Notleuchte mit Doppelstrahler (weiß)",
              "Luz de emergencia de doble foco (blanca)", "Nödbelysning med dubbla strålkastare (vit)"),
    "black": ("Twin-Head Emergency Light (Black)", "Notleuchte mit Doppelstrahler (schwarz)",
              "Luz de emergencia de doble foco (negra)",
              "Nödbelysning med dubbla strålkastare (svart)"),
    "gray": ("Twin-Head Emergency Light (Gray)", "Notleuchte mit Doppelstrahler (grau)",
             "Luz de emergencia de doble foco (gris)", "Nödbelysning med dubbla strålkastare (grå)"),
}


def head(cx, cy, r, z0, z1, tex, lens_tex="lens"):
    """A lamp head facing north: the shell, and its lens as the front cap."""
    els = pipe_z(cx, cy, r, z0 + 0.3, z1, tex, front=False)
    els += pipe_z(cx, cy, r * 0.85, z0, z0 + 0.3, lens_tex)
    return els


for name in HOUSINGS:
    reg = "emergency_light_twin_head_" + name
    els = [box([3, 4, 12], [13, 10, 16], "h", per={"north": "front"})]
    bulbs = []
    for cx in (5.5, 10.5):
        els += post(cx, 13, 0.5, 10, 11.5, "h", bottom=False)
        els += head(cx, 12.5, 2, 10, 14, "h")
        bulbs.append((cx - 1.7, 10.8, 10, cx + 1.7, 14.2, 11))
    light(reg, (2.5, 4, 10, 13.5, 14.5, 16), NAMES[name],
          {"h": T("housing_" + name), "front": T("front_" + name),
           "particle": T("housing_" + name)}, els, bulbs)

# --- LED bar ------------------------------------------------------------------------------------
light("emergency_light_led_bar", (1, 6, 13, 15, 10, 16),
      ("LED Emergency Light Bar", "LED-Notleuchte (Leiste)", "Barra de luz de emergencia LED",
       "LED-nödljuslist"),
      {"h": T("housing_white"), "particle": T("housing_white")},
      [box([1, 6, 13.6], [15, 10, 16], "h"),
       box([2, 6.8, 13.5], [14, 8.8, 13.6], "strip", faces=("north",))],
      [(2, 6.8, 13.5, 14, 8.8, 14)], lens_key="strip", on="strip_on", off="strip_off")

# --- remote heads -------------------------------------------------------------------------------
light("emergency_remote_head_single", (5, 5, 10, 11, 11, 16),
      ("Emergency Remote Lamp Head", "Notleuchten-Einzelstrahler", "Foco remoto de emergencia",
       "Fjärrstrålkastare för nödbelysning"),
      {"h": T("housing_white"), "particle": T("housing_white")},
      [box([6, 6, 15], [10, 10, 16], "h")] + pipe_z(8, 8, 0.6, 13.5, 15, "h", front=False)
      + head(8, 8, 2.2, 10.5, 13.5, "h"),
      [(6.1, 6.1, 10.5, 9.9, 9.9, 11.5)])
light("emergency_remote_head_twin", (2, 5, 10, 14, 11, 16),
      ("Emergency Remote Lamp Heads (Twin)", "Notleuchten-Doppelstrahler",
       "Focos remotos de emergencia (dobles)", "Fjärrstrålkastare för nödbelysning (dubbla)"),
      {"h": T("housing_white"), "particle": T("housing_white")},
      [box([3, 6.5, 15], [13, 9.5, 16], "h")]
      + pipe_z(5, 8, 0.6, 13.5, 15, "h", front=False) + head(5, 8, 2.2, 10.5, 13.5, "h")
      + pipe_z(11, 8, 0.6, 13.5, 15, "h", front=False) + head(11, 8, 2.2, 10.5, 13.5, "h"),
      [(3.1, 6.1, 10.5, 6.9, 9.9, 11.5), (9.1, 6.1, 10.5, 12.9, 9.9, 11.5)])

# --- outdoor wall pack --------------------------------------------------------------------------
light("emergency_wall_pack", (3, 2, 9, 13, 12, 16),
      ("Outdoor Emergency Wall Pack", "Außen-Notleuchte (Wandleuchte)",
       "Aplique exterior de emergencia", "Utomhus nödbelysningsarmatur"),
      {"b": T("bronze"), "particle": T("bronze")},
      [box([3, 7, 10], [13, 12, 16], "b"),
       box([3, 2, 12], [13, 7, 16], "b"),
       box([3.5, 2.5, 10], [12.5, 7, 12], "b", faces=("east", "west", "down")),
       box([3.6, 2.6, 9.9], [12.4, 7, 10], "prism", faces=("north",))],
      [(3.6, 2.6, 9.9, 12.4, 7, 10.5)], lens_key="prism", on="prism_on", off="prism_off")

# --- recessed downlight (place on a ceiling) ---------------------------------------------------
light("emergency_downlight_recessed", (3.5, 3.5, 14.5, 12.5, 12.5, 16),
      ("Recessed Emergency Downlight", "Einbau-Notleuchte (Downlight)",
       "Foco empotrado de emergencia", "Infälld nödbelysningsspot"),
      {"h": T("housing_white"), "particle": T("housing_white")},
      pipe_z(8, 8, 4.5, 15.3, 16, "h", front=False) + pipe_z(8, 8, 3.2, 15, 15.3, "lens"),
      [(5.2, 5.2, 14.9, 10.8, 10.8, 15.5)])


if __name__ == "__main__":
    sys.exit(C.main())
