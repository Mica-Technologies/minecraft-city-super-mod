#!/usr/bin/env python3
"""Every asset the Transit tab's station pieces ship: the subway entrance (a glass kiosk built
to size, or an open stair with railings), its lit globe lamps, the paid-area railing with its
service gate, the line bullets and the station agent's booth counter.

    python dev-env-utils/scripts/gen_transit_stations.py
    python dev-env-utils/scripts/gen_transit_stations.py --check
    python dev-env-utils/scripts/gen_transit_stations.py --fragments   # tab lines to paste

Like the platform fit-out (gen_transit_platforms.py, whose element helpers these are), the set is
made to complement RCMC's stations, never to repeat them: RCMC has the platforms, the line map
sign, arrival boards, speakers and the line desk, so there is no line diagram here and no
platform piece. What is here, and the class that places each (package transit.station):

- **The entrance kiosk**, built block by block rather than placed whole: framed glass walls
  (BlockStationGlass) that join like panes, with a post at every block and a head rail and kick
  plate only where the wall stops, and a roof (BlockStationEntranceRoof, one per agency) that
  joins on all four sides with its fascia only round the outside. A roof block can carry a name
  board on its fascia: SUBWAY, METRO or one of the station name sign's ten names, stepped by
  clicking (`legend`). The stair under it is vanilla stairs.
- **The open stair entrance**: a painted railing that joins as a fence does, with a ball finial
  on each post (BlockStationRailing), the same railing carrying a name plate on a straight run
  (BlockStationRailingSign), and the lit globe lamp on its post (BlockStationGlobe: green, red
  or white, clicked).
- **The fare line**: a stainless railing at the fare gates' cabinet height that joins the gates
  themselves (BlockStationRailing), the same railing with a PAID AREA plate, and a service gate
  (BlockStationGate, a vanilla fence gate: click or redstone).
- **Line bullets** (BlockStationLineBullet): a round enamel plate on the wall, sixteen invented
  lines, stepped by clicking.
- **The booth counter** (BlockStationBoothCounter): the station agent's counter with its deal
  tray, glazed above so entrance glass stacks on it into a booth.

Every model faces north, as the platform fit-out's: a sign's front and the counter's customer
side look north, a wall piece has its wall at z = 16. Joining parts are drawn once for the north
side and turned by the blockstate. Names, lines and wording are invented; the four agencies are
gen_transit_stops.py's.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_transit_platforms as gp  # noqa: E402
import gen_transit_stops as gs  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_stations.py", "transit/stations", "transit/stations",
                 assets=ASSETS)
TAB = "CsmTabTransit"

B, win = gp.B, gp.win
ALL, SIDES = gp.ALL, gp.SIDES
names_of, box_java, gui_display = gp.names_of, gp.box_java, gp.gui_display
FACINGS = gp.FACINGS

WHITE = gp.WHITE
BLACK = gp.BLACK
NAVY = gp.NAVY
YELLOW = gp.YELLOW
STAINLESS = gp.STAINLESS
FRAME = (46, 50, 54)             # the kiosk's steel
PAINT = (34, 54, 42)             # the stair railing's cast iron, painted
LAMP = (250, 246, 226)

# the four agencies: id, primary, accent (gen_transit_stops.py)
AGENCIES = [(a[0], a[2], a[3]) for a in gs.AGENCIES]
AGENCY_NAMES = {
    "cityline": ("CITYLINE", "CITYLINE", "CITYLINE", "CITYLINE"),
    "riverway": ("RIVERWAY", "RIVERWAY", "RIVERWAY", "RIVERWAY"),
    "verdant": ("VERDANT", "VERDANT", "VERDANT", "VERDANT"),
    "emberline": ("EMBERLINE", "EMBERLINE", "EMBERLINE", "EMBERLINE"),
}

# What a name board can say, in order (BlockStationEntranceRoof and BlockStationRailingSign):
# the two generic words, then the station name sign's ten invented names.
LEGENDS = ["SUBWAY", "METRO"] + gp.STATION_NAMES

# BlockStationLineBullet.LINES: the line's letter or number and its colour, in this order. The
# first four are the platform line strip's (1, 4, 7, E in the agencies' colours).
LINES = [
    ("1", gp.CITYLINE, WHITE), ("2", gp.CITYLINE, WHITE),
    ("4", gp.RIVERWAY_ORANGE, WHITE), ("5", gp.RIVERWAY_ORANGE, WHITE),
    ("7", gp.VERDANT, WHITE), ("8", gp.VERDANT, WHITE),
    ("E", gp.EMBERLINE, WHITE), ("F", gp.EMBERLINE, WHITE),
    ("A", (36, 110, 196), WHITE), ("C", (36, 110, 196), WHITE),
    ("K", (112, 60, 156), WHITE), ("M", (186, 44, 120), WHITE),
    ("S", (112, 116, 120), WHITE), ("T", (132, 88, 52), WHITE),
    ("X", (240, 196, 30), BLACK), ("Z", (28, 48, 108), WHITE),
]

# BlockStationGlobe.Colour, in this order
GLOBES = [("green", (70, 230, 110)), ("red", (250, 70, 56)), ("white", (255, 246, 222))]


def grain(size, colour, amount, seed, alpha=255):
    return lc.fill(colour, size, amount, seed, alpha)


def noshade(els):
    for e in els:
        e["shade"] = False
    return els


def flat_uv(els, uv):
    """Every face of these elements reads the same small window, so the overlapping caps and
    sides of an octagon show one colour and cannot be seen to fight."""
    for e in els:
        for f in e["faces"].values():
            f["uv"] = list(uv)
    return els


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
LEG_W, LEG_H = 128, 43     # a name board, 14 x 4.7 units
PAID_W, PAID_H = 128, 55   # the paid-area plate, 14 x 6
GATE_W, GATE_H = 64, 28    # the service gate's plate, 7.2 x 3.2
AGENT_W, AGENT_H = 128, 29  # the booth's plate, 11 x 2.5


def legend_board(text):
    """A porcelain-enamel name board: white capitals on navy inside a white rule, a two-word
    name on two lines."""
    img = grain(128, NAVY, 2, 900 + len(text))
    lc.frame(img, 0, 0, LEG_W, LEG_H, lc.shade(NAVY, 0.7), 2)
    lc.frame(img, 3, 3, LEG_W - 3, LEG_H - 3, WHITE, 1)
    words = text.split(" ")
    if len(words) == 1:
        scale = 4 if lc.text_width(text, 4) <= LEG_W - 16 else 3
        lc.draw_text_centred(img, text, LEG_W / 2.0, (LEG_H - 5 * scale) // 2, WHITE, scale)
    else:
        lc.draw_text_centred(img, words[0], LEG_W / 2.0, 5, WHITE, 3)
        lc.draw_text_centred(img, " ".join(words[1:]), LEG_W / 2.0, 23, WHITE, 3)
    return img


def paid_plate():
    img = grain(128, YELLOW, 2, 920)
    lc.frame(img, 0, 0, PAID_W, PAID_H, BLACK, 3)
    lc.draw_text_centred(img, "PAID AREA", PAID_W / 2.0, 11, BLACK, 3)
    lc.rect(img, 12, 31, PAID_W - 12, 33, BLACK)
    lc.draw_text_centred(img, "FARE REQUIRED", PAID_W / 2.0, 38, BLACK, 2)
    return img


def gate_plate():
    img = grain(64, (200, 36, 30), 2, 921)
    lc.frame(img, 0, 0, GATE_W, GATE_H, WHITE, 1)
    lc.draw_text_centred(img, "SERVICE", GATE_W / 2.0, 3, WHITE, 2)
    lc.draw_text_centred(img, "GATE", GATE_W / 2.0, 15, WHITE, 2)
    return img


def agent_plate():
    img = grain(128, NAVY, 2, 922)
    lc.frame(img, 0, 0, AGENT_W, AGENT_H, lc.shade(NAVY, 0.7), 1)
    lc.draw_text_centred(img, "STATION AGENT", AGENT_W / 2.0, 9, WHITE, 2)
    return img


def fascia(primary, accent, seed):
    """An entrance roof's fascia, read through the window [0, 0, 16, 6] (rows 0 to 12 of 32):
    the agency's colour with a lit top edge, an accent line near the foot and a dark drip."""
    img = grain(32, primary, 3, seed)
    lc.rect(img, 0, 0, 32, 1, lc.shade(primary, 1.3))
    lc.rect(img, 0, 8, 32, 10, accent)
    lc.rect(img, 0, 11, 32, 12, lc.shade(primary, 0.6))
    return img


def glass():
    """The kiosk's glass: pale, see-through, with two faint reflections."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(930)
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-3, 3)
            px[x, y] = lc.clamp((196 + d, 218 + d, 226 + d)) + (70,)
    for k in (3, 9):
        for y in range(16):
            x = (k + y // 2) % 16
            px[x, y] = (236, 244, 248, 120)
    return img


def globe(colour, emissive=False):
    """A globe lamp's opal glass, lit: a hot centre fading to the colour."""
    img = grain(16, colour, 4, 940 + sum(colour))
    lc.disc(img, 8, 8, 5, lc.clamp(tuple(v * 0.7 + 255 * 0.3 for v in colour)))
    lc.disc(img, 8, 8, 2.5, lc.clamp(tuple(v * 0.35 + 255 * 0.65 for v in colour)))
    return img


def bullet(glyph, colour, ink):
    """A line bullet on a transparent square: the disc, a thin white rim, the glyph."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    lc.disc(img, 32, 32, 31.6, lc.shade(colour, 0.8))
    lc.disc(img, 32, 32, 30.2, (236, 238, 236))
    lc.disc(img, 32, 32, 28.4, colour)
    lc.draw_text_centred(img, glyph, 32, 17, ink, 6)
    return img


def deal_tray():
    img = grain(16, (58, 62, 66), 2, 950)
    lc.rect(img, 1, 1, 15, 15, (30, 32, 34))
    lc.rect(img, 1, 1, 15, 3, (90, 94, 98))
    return img


def speak_grille():
    img = grain(16, STAINLESS, 2, 951)
    for y in range(2, 15, 3):
        for x in range(2, 15, 3):
            if (x - 8) ** 2 + (y - 8) ** 2 <= 42:
                lc.rect(img, x, y, x + 2, y + 2, (40, 42, 44))
    return img


def register_textures():
    tex = {
        "frame": lambda: grain(16, FRAME, 3, 960),
        "glass": glass,
        "stainless": lambda: grain(16, STAINLESS, 3, 961),
        "kick": lambda: grain(16, lc.shade(STAINLESS, 0.92), 5, 962),
        "paint": lambda: grain(16, PAINT, 3, 963),
        "roof": lambda: grain(16, (120, 124, 126), 5, 964),
        "soffit": lambda: grain(16, (222, 224, 220), 2, 965),
        "lamp_on": lambda: gp.lens(LAMP, 1.05),
        "board_edge": lambda: grain(16, (36, 40, 46), 2, 966),
        "cast_iron": lambda: grain(16, (30, 32, 30), 3, 967),
        "paid_plate": paid_plate,
        "gate_plate": gate_plate,
        "agent_plate": agent_plate,
        "counter": lambda: grain(16, (70, 66, 60), 3, 968),
        "deal_tray": deal_tray,
        "grille": speak_grille,
    }
    for aid, primary, accent in AGENCIES:
        tex["fascia_" + aid] = (lambda p=primary, a=accent, s=len(aid): fascia(p, a, 970 + s))
    for i, text in enumerate(LEGENDS, 1):
        tex["legend_%d" % i] = (lambda t=text: legend_board(t))
    for name, colour in GLOBES:
        tex["globe_" + name] = (lambda c=colour: globe(c))
        tex["globe_%s_e" % name] = (lambda c=colour: globe(c, True))
    for i, (glyph, colour, ink) in enumerate(LINES, 1):
        tex["bullet_%d" % i] = (lambda g=glyph, c=colour, k=ink: bullet(g, c, k))
    for name, draw in tex.items():
        C.texture(name)(draw)


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def when(**cond):
    return {k: ("true" if v else "false") if isinstance(v, bool) else str(v)
            for k, v in cond.items()}


def rule(model, cond=None, y=0, any_of=None):
    """A multipart rule: the model, turned y, when every condition holds (or any of any_of)."""
    apply = {"model": C.M(model)}
    if y:
        apply["y"] = y
    if any_of:
        return {"when": {"OR": [when(**c) for c in any_of]}, "apply": apply}
    if cond:
        return {"when": when(**cond), "apply": apply}
    return {"apply": apply}


SIDE_Y = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def child(parent, textures):
    return {"parent": "csm:block/" + C.M(parent).split(":", 1)[1], "textures": textures}


# ------------------------------------------------------------------------------------------
# The kiosk: glass walls
# ------------------------------------------------------------------------------------------
PANE_Z0, PANE_Z1 = 7.9, 8.1      # the glass, either side of the wall's line
POST = 1.0                       # half a post's width
RAIL = 0.7                       # half a head rail's or kick plate's thickness
HEAD_Y = 14.8                    # the head rail's foot, where nothing stacks above
KICK_Y = 2.2                     # the kick plate's top, where nothing stacks below


def glass_wall():
    """Framed glass that joins as panes do: toward each side where more glass (or the booth
    counter, or a solid face) continues it runs out as an arm; a post stands at every block.
    The head rail and kick plate are drawn only where the wall stops (`up`, `down`), so a wall
    two blocks tall is one tall pane with a rail at its head and a plate at its foot. A lone
    block with nothing beside it is drawn as a panel along x."""
    reg = "station_entrance_glass"
    tex = {"glass": C.T("glass"), "frame": C.T("frame"), "kick": C.T("kick"),
           "particle": C.T("frame")}
    models = {
        reg + "_post": lc.model(tex, [B((8 - POST, 0, 8 - POST), (8 + POST, 16, 8 + POST),
                                        "frame", SIDES)]),
        reg + "_post_head": lc.model(tex, [B((8 - POST - 0.2, HEAD_Y, 8 - POST - 0.2),
                                             (8 + POST + 0.2, 16, 8 + POST + 0.2), "frame",
                                             SIDES + ("up",))]),
        reg + "_post_foot": lc.model(tex, [B((8 - POST - 0.2, 0, 8 - POST - 0.2),
                                             (8 + POST + 0.2, KICK_Y, 8 + POST + 0.2), "kick",
                                             SIDES + ("down",))]),
        # the arms, drawn toward the north
        reg + "_arm": lc.model(tex, [B((PANE_Z0, 0, 0), (PANE_Z1, 16, 8 - POST), "glass",
                                       ("east", "west"))]),
        reg + "_arm_head": lc.model(tex, [B((8 - RAIL, HEAD_Y, 0), (8 + RAIL, 16, 8 - POST),
                                            "frame", ("east", "west", "up", "down"))]),
        reg + "_arm_foot": lc.model(tex, [B((8 - RAIL, 0, 0), (8 + RAIL, KICK_Y, 8 - POST),
                                            "kick", ("east", "west", "up", "down"))]),
    }
    lone = dict(north=False, east=False, south=False, west=False)
    rules = [rule(reg + "_post"),
             rule(reg + "_post_head", dict(up=False)),
             rule(reg + "_post_foot", dict(down=False))]
    for side, y in SIDE_Y:
        loner = side in ("east", "west")
        for part, extra in (("_arm", {}), ("_arm_head", dict(up=False)),
                            ("_arm_foot", dict(down=False))):
            cond = dict(extra)
            cond[side] = True
            if loner:
                alone = dict(lone)
                alone.update(extra)
                rules.append(rule(reg + part, y=y, any_of=[cond, alone]))
            else:
                rules.append(rule(reg + part, cond, y=y))
    item_els = (models[reg + "_post"]["elements"] + models[reg + "_post_head"]["elements"]
                + models[reg + "_post_foot"]["elements"])
    for y in (90, 270):
        for part in ("_arm", "_arm_head", "_arm_foot"):
            item_els += turned(models[reg + part]["elements"], y)
    item = lc.model(tex, item_els)
    item["display"] = gui_display(0.6, 0.0)
    C.add(reg, 'new BlockStationGlass("%s")' % reg,
          names_of("Station Entrance Glass", "Stationseingang (Glaswand)",
                   "Acristalamiento de Acceso a Estación", "Stationsentré (Glasvägg)"),
          models, {"multipart": rules}, item=item, tab=TAB)


def turned(els, y):
    """Elements turned y degrees about the block's middle, as a multipart "y" turns them: for
    the item models, which have no blockstate to turn them."""
    out = []
    fm = {90: {"north": "east", "east": "south", "south": "west", "west": "north"},
          180: {"north": "south", "south": "north", "east": "west", "west": "east"},
          270: {"north": "west", "west": "south", "south": "east", "east": "north"}}[y]

    def pt(x, yy, z):
        if y == 90:
            return (16 - z, yy, x)
        if y == 180:
            return (16 - x, yy, 16 - z)
        return (z, yy, 16 - x)
    for e in els:
        a, b = pt(*e["from"]), pt(*e["to"])
        n = {"from": [round(min(a[i], b[i]), 4) for i in range(3)],
             "to": [round(max(a[i], b[i]), 4) for i in range(3)],
             "faces": {fm.get(f, f): dict(v) for f, v in e["faces"].items()}}
        if "shade" in e:
            n["shade"] = e["shade"]
        assert "rotation" not in e
        out.append(n)
    return out


# ------------------------------------------------------------------------------------------
# The kiosk: the roof
# ------------------------------------------------------------------------------------------
FASCIA_Y0, FASCIA_Y1, FASCIA_T = -2.0, 4.0, 0.8
BOARD = (1.0, -1.35, 15.0, 3.35)          # x0, y0, x1, y1 of a name board on the fascia
BOARD_Z = -0.4                            # its face, proud of the fascia's
FLAT = [1, 1, 1.5, 1.5]


def roofs():
    """The kiosk's roof one block at a time, set on the glass walls: a deck at the foot of its
    block (so it rests on the wall below), a white soffit with a square light, and on each side
    where no roof continues the agency's fascia, with a name board on it when the block's
    `legend` is past 1 (1 is no board). A block wholly inside the roof draws deck and light
    only."""
    base_tex = {"soffit": C.T("soffit"), "roof": C.T("roof"), "lamp": C.T("lamp_on"),
                "particle": C.T("soffit")}
    shared = {
        "station_entrance_roof_deck": lc.model(base_tex, [
            B((0, 0, 0), (16, 2, 16), "soffit", ("up", "down"), per={"up": "roof"}),
            B((5, -0.4, 5), (11, 0, 11), "lamp", ("down", "north", "south", "east", "west"),
              shade=False)]),
    }
    # the fascia, drawn on the north side; its top and underside read one flat texel, so where
    # two meet at a corner they overlap in one colour
    fascia_el = B((0, FASCIA_Y0, 0), (16, FASCIA_Y1, FASCIA_T), "fascia",
                  ("north", "south", "up", "down"),
                  per={"south": "frame", "up": "roof", "down": "soffit"},
                  uv={"north": [0, 0, 16, 6], "up": FLAT, "down": FLAT})
    ftex = {"fascia": C.T("fascia_cityline"), "frame": C.T("frame"), "roof": C.T("roof"),
            "soffit": C.T("soffit"), "particle": C.T("fascia_cityline")}
    shared["station_entrance_roof_fascia_cityline"] = lc.model(ftex, [fascia_el])
    for aid, _, _ in AGENCIES[1:]:
        shared["station_entrance_roof_fascia_" + aid] = child(
            "station_entrance_roof_fascia_cityline",
            {"fascia": C.T("fascia_" + aid), "particle": C.T("fascia_" + aid)})
    x0, y0, x1, y1 = BOARD
    board = B((x0, y0, BOARD_Z), (x1, y1, 0), "edge", ("north", "east", "west", "up", "down"),
              per={"north": "legend"}, uv={"north": win(LEG_W, LEG_H, 128)})
    btex = {"legend": C.T("legend_1"), "edge": C.T("board_edge"), "particle": C.T("legend_1")}
    shared["station_entrance_board_1"] = lc.model(btex, [board])
    for k in range(2, len(LEGENDS) + 1):
        shared["station_entrance_board_%d" % k] = child(
            "station_entrance_board_1", {"legend": C.T("legend_%d" % k),
                                         "particle": C.T("legend_%d" % k)})
    first = True
    for aid, _, _ in AGENCIES:
        reg = "station_entrance_roof_" + aid
        rules = [rule("station_entrance_roof_deck")]
        for side, y in SIDE_Y:
            rules.append(rule("station_entrance_roof_fascia_" + aid, {side: False}, y=y))
        for side, y in SIDE_Y:
            for k in range(1, len(LEGENDS) + 1):
                rules.append(rule("station_entrance_board_%d" % k, {side: False, "legend": k + 1},
                                  y=y))
        # the item: the deck, the fascia round all four sides and SUBWAY on the front
        fm = shared["station_entrance_roof_fascia_cityline"]["elements"]
        els = list(shared["station_entrance_roof_deck"]["elements"]) + list(fm)
        for y in (90, 180, 270):
            els += turned(fm, y)
        els += shared["station_entrance_board_1"]["elements"]
        itex = dict(base_tex)
        itex.update({"fascia": C.T("fascia_" + aid), "frame": C.T("frame"),
                     "legend": C.T("legend_1"), "edge": C.T("board_edge")})
        item = lc.model(itex, els)
        item["display"] = gui_display(0.55, 2.0)
        models = dict(shared) if first else {}
        first = False
        C.add(reg, 'new BlockStationEntranceRoof("%s")' % reg,
              names_of("Station Entrance Roof (%s)" % AGENCY_NAMES[aid][0],
                       "Stationseingang (Dach, %s)" % AGENCY_NAMES[aid][1],
                       "Cubierta de Acceso a Estación (%s)" % AGENCY_NAMES[aid][2],
                       "Stationsentré (Tak, %s)" % AGENCY_NAMES[aid][3]),
              models, {"multipart": rules}, item=item, tab=TAB)


# ------------------------------------------------------------------------------------------
# Railings: the stair entrance's and the fare line's
# ------------------------------------------------------------------------------------------
BALUSTERS = (1.0, 3.0, 5.0)      # along an arm, from the block's edge; the post is at 8


def railing_parts(style):
    """A railing's post and its arm toward the north. The stair railing is painted cast iron,
    its post topped with a ball; the fare railing is stainless at the fare gates' cabinet
    height (16), with round-ish rails and a plain cap."""
    if style == "entrance":
        t = "paint"
        post = [B((6.9, 0, 6.9), (9.1, 15.6, 9.1), t, SIDES),
                B((6.4, 0, 6.4), (9.6, 1.2, 9.6), t, SIDES + ("up",)),
                B((6.6, 15.6, 6.6), (9.4, 16.4, 9.4), t, SIDES + ("down",))]
        post += flat_uv(gp.octagon_y(8, 8, 0.6, 16.4, 16.9, t, top=False, bottom=False), FLAT)
        post += flat_uv(gp.octagon_y(8, 8, 1.15, 16.9, 18.5, t), FLAT)
        post += flat_uv(gp.octagon_y(8, 8, 0.7, 18.5, 18.9, t, bottom=False), FLAT)
        arm = [B((7.3, 14.2, 0), (8.7, 15.4, 6.9), t, ("east", "west", "up", "down")),
               B((7.5, 12.6, 0), (8.5, 13.2, 6.9), t, ("east", "west", "up", "down")),
               B((7.5, 1.8, 0), (8.5, 2.6, 6.9), t, ("east", "west", "up", "down"))]
        for z in BALUSTERS:
            arm.append(B((7.7, 2.6, z - 0.3), (8.3, 14.2, z + 0.3), t, SIDES))
            # a small ring between the upper rails
            arm.append(B((7.6, 13.2, z - 0.4), (8.4, 13.4, z + 0.4), t, SIDES + ("up",)))
    else:
        t = "stainless"
        post = [B((7, 0, 7), (9, 15.8, 9), t, SIDES),
                B((6.2, 0, 6.2), (9.8, 0.4, 9.8), t, SIDES + ("up",)),
                B((6.8, 15.8, 6.8), (9.2, 16.4, 9.2), t, SIDES + ("up", "down"))]
        arm = [B((7.35, 14.6, 0), (8.65, 15.9, 7), t, ("east", "west", "up", "down")),
               B((7.5, 2.0, 0), (8.5, 2.8, 7), t, ("east", "west", "up", "down"))]
        for z in BALUSTERS:
            arm.append(B((7.7, 2.8, z - 0.3), (8.3, 14.6, z + 0.3), t, SIDES))
    return post, arm


# plates hung on a straight run of railing, along x
ENTRANCE_PLATE = (1.0, 6.4, 15.0, 11.1)
PAID_PLATE = (1.0, 5.0, 15.0, 11.0)
PLATE_Z0, PLATE_Z1 = 6.7, 9.3


def plate_el(box, face, window, edge):
    x0, y0, x1, y1 = box
    return B((x0, y0, PLATE_Z0), (x1, y1, PLATE_Z1), edge, ALL,
             per={"north": face, "south": face}, uv={"north": window, "south": window})


def railings():
    straight_x = dict(north=False, east=True, south=False, west=True)
    straight_z = dict(north=True, east=False, south=True, west=False)
    specs = [
        ("station_entrance_railing", "entrance", None,
         names_of("Station Entrance Railing", "Stationseingang (Geländer)",
                  "Barandilla de Acceso a Estación", "Stationsentré (Räcke)")),
        ("station_entrance_railing_sign", "entrance", "legend",
         names_of("Station Entrance Railing (Name Plate)",
                  "Stationseingang (Geländer mit Namensschild)",
                  "Barandilla de Acceso a Estación (Placa de Nombre)",
                  "Stationsentré (Räcke med Namnskylt)")),
        ("station_fare_railing", "fare", None,
         names_of("Fare Line Railing", "Sperrengeländer", "Barandilla de Línea de Pago",
                  "Spärrlinjeräcke")),
        ("station_fare_railing_sign", "fare", "paid",
         names_of("Fare Line Railing (Paid Area Plate)", "Sperrengeländer (Schild Bezahlbereich)",
                  "Barandilla de Línea de Pago (Placa de Zona de Pago)",
                  "Spärrlinjeräcke (Skylt Betalt Område)")),
    ]
    tex_of = {"entrance": {"paint": C.T("paint"), "particle": C.T("paint")},
              "fare": {"stainless": C.T("stainless"), "particle": C.T("stainless")}}
    written = set()
    for reg, style, plate, names in specs:
        tex = tex_of[style]
        post, arm = railing_parts(style)
        base = "station_%s_railing" % style
        models = {}
        if base not in written:
            models[base + "_post"] = lc.model(tex, post)
            models[base + "_arm"] = lc.model(tex, arm)
            written.add(base)
        rules = [rule(base + "_post")]
        for side, y in SIDE_Y:
            rules.append(rule(base + "_arm", {side: True}, y=y))
        item_els = post + turned(arm, 90) + turned(arm, 270)
        itex = dict(tex)
        java = 'new BlockStationRailing("%s", %s)' % (reg, "true" if style == "fare" else "false")
        if plate == "legend":
            pt = {"face": C.T("legend_1"), "edge": C.T("paint"), "particle": C.T("legend_1")}
            models["station_entrance_plate_1"] = lc.model(pt, [plate_el(
                ENTRANCE_PLATE, "face", win(LEG_W, LEG_H, 128), "edge")])
            for k in range(2, len(LEGENDS) + 1):
                models["station_entrance_plate_%d" % k] = child(
                    "station_entrance_plate_1", {"face": C.T("legend_%d" % k),
                                                 "particle": C.T("legend_%d" % k)})
            for k in range(1, len(LEGENDS) + 1):
                c = dict(straight_x)
                c["legend"] = k
                rules.append(rule("station_entrance_plate_%d" % k, c))
                c = dict(straight_z)
                c["legend"] = k
                rules.append(rule("station_entrance_plate_%d" % k, c, y=90))
            item_els += models["station_entrance_plate_1"]["elements"]
            itex.update({"face": C.T("legend_1"), "edge": C.T("paint")})
            java = 'new BlockStationRailingSign("%s")' % reg
        elif plate == "paid":
            pt = {"face": C.T("paid_plate"), "edge": C.T("stainless"),
                  "particle": C.T("paid_plate")}
            models["station_fare_plate"] = lc.model(pt, [plate_el(
                PAID_PLATE, "face", win(PAID_W, PAID_H, 128), "edge")])
            rules.append(rule("station_fare_plate", straight_x))
            rules.append(rule("station_fare_plate", straight_z, y=90))
            item_els += models["station_fare_plate"]["elements"]
            itex.update({"face": C.T("paid_plate"), "edge": C.T("stainless")})
        item = lc.model(itex, item_els)
        item["display"] = gui_display(0.6, 0.0)
        C.add(reg, java, names, models, {"multipart": rules}, item=item, tab=TAB)


# ------------------------------------------------------------------------------------------
# The service gate
# ------------------------------------------------------------------------------------------
GATE_HINGE_X, GATE_HINGE_Z = 1.7, 8.0


def swing(els, hx=GATE_HINGE_X, hz=GATE_HINGE_Z):
    """Turned a quarter about the upright line (hx, hz), so what ran along +x from the hinge
    runs along -z: the leaf swung open toward the front (gen_furniture_outdoor.py's swing).
    A plate keeps its picture; the rest take their uv from their new place."""
    fm = {"north": "west", "west": "south", "south": "east", "east": "north"}
    out = []
    for e in els:
        def pt(x, y, z):
            return (hx + (z - hz), y, hz - (x - hx))
        a, b = pt(*e["from"]), pt(*e["to"])
        frm = [min(a[i], b[i]) for i in range(3)]
        to = [max(a[i], b[i]) for i in range(3)]
        faces = {}
        for f, v in e["faces"].items():
            nf = fm.get(f, f)
            nv = dict(v)
            if v["texture"] != "#face":
                # a picture keeps its window: north turns to west, whose u runs the same way
                # over the swung leaf
                nv["uv"] = gp._uv(nf, frm, to)
            faces[nf] = nv
        assert "rotation" not in e
        out.append({"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to],
                    "faces": faces})
    return out


def service_gate():
    """A stainless service gate for the fare line, a vanilla fence gate in behaviour: the leaf
    between a hinge post and a latch post, the railing's rails and balusters, a SERVICE GATE
    plate on both faces. Open, the leaf swings a quarter about its hinge toward the way the
    player faced."""
    reg = "station_service_gate"
    t = "stainless"
    posts = [B((0, 0, 7), (1.6, 16.4, 9), t, ALL), B((14.4, 0, 7), (16, 16.4, 9), t, ALL)]
    leaf = [B((1.8, 1.2, 7.4), (2.8, 15.6, 8.6), t, ALL),
            B((13.2, 1.2, 7.4), (14.2, 15.6, 8.6), t, ALL),
            B((2.8, 14.6, 7.45), (13.2, 15.6, 8.55), t, ("north", "south", "up", "down")),
            B((2.8, 1.2, 7.45), (13.2, 2.0, 8.55), t, ("north", "south", "up", "down")),
            B((2.8, 6.4, 7.5), (13.2, 7.0, 8.5), t, ("north", "south", "up", "down"))]
    for x in (4.3, 6.3, 9.7, 11.7):
        leaf.append(B((x - 0.3, 2.0, 7.7), (x + 0.3, 14.6, 8.3), t, SIDES))
    leaf.append(B((4.4, 7.4, 7.3), (11.6, 10.6, 8.7), "edge", ALL,
                  per={"north": "face", "south": "face"},
                  uv={"north": win(GATE_W, GATE_H, 64), "south": win(GATE_W, GATE_H, 64)}))
    # the hinges and the latch
    leaf += [B((1.6, 3.0, 7.6), (1.8, 4.4, 8.4), "hinge", ("north", "south", "up", "down")),
             B((1.6, 12.4, 7.6), (1.8, 13.8, 8.4), "hinge", ("north", "south", "up", "down")),
             B((14.2, 8.4, 7.5), (14.4, 9.6, 8.5), "hinge", ("north", "south", "up", "down"))]
    tex = {"stainless": C.T("stainless"), "edge": C.T("stainless"), "face": C.T("gate_plate"),
           "hinge": C.T("cast_iron"), "particle": C.T("stainless")}
    closed = lc.model(tex, posts + leaf)
    opened = lc.model(tex, posts + swing(leaf))
    closed["display"] = gui_display(0.6, 0.0)
    state = {"forge_marker": 1, "defaults": {"model": C.M(reg + "_closed")},
             "variants": {
                 "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                            "west": {"y": 270}},
                 "open": {"false": {"model": C.M(reg + "_closed")},
                          "true": {"model": C.M(reg + "_open")}},
                 "in_wall": {"true": {}, "false": {}},
                 "powered": {"true": {}, "false": {}},
                 "inventory": [{}]}}
    C.add(reg, 'new BlockStationGate("%s")' % reg,
          names_of("Fare Line Service Gate", "Sperren-Servicetür", "Puerta de Servicio de Línea de Pago",
                   "Servicegrind vid Spärrlinjen"),
          {reg + "_closed": closed, reg + "_open": opened}, state, tab=TAB)


# ------------------------------------------------------------------------------------------
# The globe lamp
# ------------------------------------------------------------------------------------------
def globe_post():
    """A cast-iron lamp post with a lit opal globe on top, two blocks tall from one block: a
    flared base, a slim shaft, a collar and fitter, the globe in three tiers and a finial. The
    globe glows from its texture (an `_e` companion for OptiFine) and the block's light."""
    reg = "station_entrance_globe"
    iron = "iron"
    els = []
    els += flat_uv(gp.octagon_y(8, 8, 2.6, 0, 1.2, iron, bottom=False), FLAT)
    els += flat_uv(gp.octagon_y(8, 8, 2.0, 1.2, 2.2, iron, bottom=False), FLAT)
    els += flat_uv(gp.octagon_y(8, 8, 1.5, 2.2, 3.4, iron, bottom=False), FLAT)
    els += gp.octagon_y(8, 8, 1.05, 3.4, 21.4, iron, top=False, bottom=False)
    els += flat_uv(gp.octagon_y(8, 8, 1.7, 21.4, 22.6, iron), FLAT)
    els += flat_uv(gp.octagon_y(8, 8, 1.3, 22.6, 24.0, iron, bottom=False), FLAT)
    g = noshade(gp.octagon_y(8, 8, 1.6, 24.0, 24.9, "globe", top=False, bottom=False)
                + gp.octagon_y(8, 8, 2.35, 24.9, 28.9, "globe")
                + gp.octagon_y(8, 8, 1.6, 28.9, 29.8, "globe", bottom=False))
    els += g
    els += flat_uv(gp.octagon_y(8, 8, 0.45, 29.8, 31.0, iron, bottom=False), FLAT)
    tex = {"iron": C.T("cast_iron"), "globe": C.T("globe_green"), "particle": C.T("cast_iron")}
    m = lc.model(tex, els, ao=False)
    m["display"] = gui_display(0.42, -3.5)
    colours = {name: {"textures": {"globe": C.T("globe_" + name)}} for name, _ in GLOBES}
    state = {"forge_marker": 1, "defaults": {"model": C.M(reg)},
             "variants": {"colour": colours, "inventory": [{}]}}
    C.add(reg, 'new BlockStationGlobe("%s")' % reg,
          names_of("Station Entrance Globe Lamp", "Stationseingang (Kugelleuchte)",
                   "Farola de Globo de Acceso a Estación", "Stationsentré (Globlykta)"),
          {reg: m}, state, tab=TAB)


# ------------------------------------------------------------------------------------------
# Line bullets
# ------------------------------------------------------------------------------------------
BULLET_R = 6.0
BULLET_Z0, BULLET_Z1 = 15.1, 16.0
SIDE_WINDOW = [3.9, 7.6, 4.3, 8.4]    # a patch of the disc's colour, clear of the glyph


def line_bullet():
    """A round enamel plate on the wall with a line's letter or number: an octagonal disc whose
    face is the bullet art (its corners clear), its edge read off the disc's own colour and
    set in a little so the octagon's corners stay behind the round face."""
    reg = "station_line_bullet"
    rim = flat_uv(gp.octagon_z(8, 8, BULLET_R * 0.93, BULLET_Z0, BULLET_Z1, "face", front=False,
                               back=False), SIDE_WINDOW)
    face = B((8 - BULLET_R, 8 - BULLET_R, BULLET_Z0), (8 + BULLET_R, 8 + BULLET_R, BULLET_Z0),
             "face", ("north",), uv={"north": [0, 0, 16, 16]})
    tex = {"face": C.T("bullet_1"), "particle": C.T("bullet_1")}
    lines = {str(i): {"textures": {"face": C.T("bullet_%d" % i)}}
             for i in range(1, len(LINES) + 1)}
    gp_box = (2, 2, 15, 14, 14, 16)
    m = lc.model(tex, rim + [face])
    m["display"] = gui_display(0.8, 0.0, (0, 180, 0))
    C.add(reg, 'new BlockStationLineBullet("%s", %s)' % (reg, box_java(gp_box)),
          names_of("Line Bullet", "Liniensymbol", "Símbolo de Línea", "Linjesymbol"),
          {reg: m}, lc.facing_state(C.M(reg), {"line": lines}), tab=TAB)


# ------------------------------------------------------------------------------------------
# The booth counter
# ------------------------------------------------------------------------------------------
COUNTER_Y = 13.6


def booth_counter():
    """The station agent's counter, the customer's side north: a stainless front with the
    STATION AGENT plate, a ledge with a deal tray under the glass, a speaking grille and glass
    from the ledge up, a work shelf inside. Entrance glass stacked on it makes the booth's
    window; entrance glass beside it runs on into the booth's walls."""
    reg = "station_booth_counter"
    els = [B((0, 0, 7.2), (16, COUNTER_Y, 8.8), "stainless", ("north", "south", "up")),
           # the ledge, reaching out to the customer, and the deal tray sunk into it
           B((0.4, COUNTER_Y, 3.6), (15.6, COUNTER_Y + 0.8, 9.4), "counter", ALL),
           B((5.2, COUNTER_Y + 0.8, 4.2), (10.8, COUNTER_Y + 0.9, 8.6), "tray", ("up",),
             uv={"up": [0, 0, 16, 16]}),
           B((0.4, COUNTER_Y - 1.4, 7.0), (15.6, COUNTER_Y, 7.2), "stainless",
             ("north", "down", "east", "west")),
           # the plate on the front
           B((2.5, 9.0, 6.8), (13.5, 11.5, 7.2), "stainless", ("north", "east", "west", "up",
                                                                 "down"),
             per={"north": "plate"}, uv={"north": win(AGENT_W, AGENT_H, 128)}),
           # the glass above the ledge, with the speaking grille in it
           B((0, COUNTER_Y + 0.8, gp_z(0)), (16, 16, gp_z(1)), "glass", ("north", "south")),
           B((6.5, COUNTER_Y + 0.8, 7.75), (9.5, 16, 8.25), "grille", ("north", "south", "east",
                                                                        "west"),
             uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}),
           # the agent's work shelf
           B((0.4, 11.4, 8.8), (15.6, 12.2, 13.8), "counter", ("up", "down", "south", "east",
                                                                "west"))]
    tex = {"stainless": C.T("stainless"), "counter": C.T("counter"), "tray": C.T("deal_tray"),
           "plate": C.T("agent_plate"), "glass": C.T("glass"), "grille": C.T("grille"),
           "particle": C.T("stainless")}
    m = lc.model(tex, els)
    m["display"] = gui_display(0.6, 0.0)
    C.add(reg, 'new BlockStationBoothCounter("%s", %s)' % (reg, box_java((0, 0, 3.6, 16, 16,
                                                                          13.8))),
          names_of("Station Agent Booth Counter", "Fahrkartenschalter (Tresen)",
                   "Mostrador de Taquilla de Estación", "Stationsexpedition (Disk)"),
          {reg: m}, lc.facing_state(C.M(reg)), tab=TAB)


def gp_z(side):
    return PANE_Z0 if side == 0 else PANE_Z1


# ------------------------------------------------------------------------------------------
C.add_lang("csm.transit.entrance.legend", (
    "Name board: %s", "Namensschild: %s", "Letrero: %s", "Namnskylt: %s"))
C.add_lang("csm.transit.entrance.none", ("none", "keines", "ninguno", "ingen"))
C.add_lang("csm.transit.globe", (
    "Globe lamp: %s", "Kugelleuchte: %s", "Farola de globo: %s", "Globlykta: %s"))
C.add_lang("csm.transit.globe.green", ("green", "grün", "verde", "grön"))
C.add_lang("csm.transit.globe.red", ("red", "rot", "roja", "röd"))
C.add_lang("csm.transit.globe.white", ("white", "weiß", "blanca", "vit"))
C.add_lang("csm.transit.line", ("Line %s", "Linie %s", "Línea %s", "Linje %s"))

register_textures()
glass_wall()
roofs()
globe_post()
railings()
service_gate()
line_bullet()
booth_counter()

if __name__ == "__main__":
    sys.exit(C.main())
