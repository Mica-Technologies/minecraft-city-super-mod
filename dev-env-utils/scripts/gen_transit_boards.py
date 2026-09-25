#!/usr/bin/env python3
"""Every asset the Transit tab's bus departure displays ship: the bus station departure board and
the bay display.

    python dev-env-utils/scripts/gen_transit_boards.py
    python dev-env-utils/scripts/gen_transit_boards.py --check
    python dev-env-utils/scripts/gen_transit_boards.py --fragments   # tab lines to paste

Bus only: rail departure boards are RCMC's (the author's train mod), and the airport's flight
information boards are gen_transit_airport.py's. What is here, both placed by
transit.board.BlockBusBoard:

- **The departure board**: a slim landscape monitor for a bus station's concourse, on a wall or
  hung on two rods, whose screen lists every bus leaving the stops around it -- route (in its
  agency's colour), destination, bay and minutes -- worked out by
  TileEntityBusDepartureBoardRenderer from the stops' own route plates and the arrival display's
  timetable. Boards side by side and stacked are a bank that pages like the flight boards. The
  screen's texture here is its header (with a bus pictogram), the column heads' band and the
  rows' bands; the renderer draws it again lit, and every word on it.
- **The bay display**: a small amber LED sign hung over one bay, listing that stop's next buses
  in the arrival display's own panel (TileEntityBusBayDisplayRenderer), on a screen the same size
  as the arrival display's so the two share their text.

Every model faces north, as the platform fit-out's do (gen_transit_platforms.py, whose element
helpers this borrows): the screen looks north and the wall is at z = 16.

The screens stand 0.4 proud of their bezels (SCREEN_Z 14.6 against a bezel face at 15), as the
flight board's does: a face a renderer draws on must be more than 0.2 from every other face of
its model, or model_depth.separate moves it forward past where the renderer draws, and the lit
screen and its text vanish behind it with no error.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_transit_platforms as gp  # noqa: E402
import gen_transit_airport as ga  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_boards.py", "transit/boards", "transit/boards", assets=ASSETS)
TAB = "CsmTabTransit"

B, win = gp.B, gp.win
ALL, SIDES = gp.ALL, gp.SIDES
names_of, gui_display = gp.names_of, gp.gui_display

WHITE = gp.WHITE
AMBER = (255, 176, 40)

# --- the departure board --------------------------------------------------------------------
# TileEntityBusDepartureBoardRenderer: the screen's place on the model and its bands, in
# sixteenths. The screen's window is SCREEN_W x SCREEN_H texels of a 256 texture.
SCREEN_X0, SCREEN_X1 = 0.4, 15.6
SCREEN_Y0, SCREEN_Y1 = 2.4, 13.6
SCREEN_Z = 14.6   # 0.4 proud of the bezel, as the flight board's
BEZEL = (0, 2, 15, 16, 14, 16)
HEADER_H, COLHEAD_H, ROW_PITCH, ROWS = 1.8, 0.8, 1.0, 8
TITLE_X = 2.3     # where the renderer starts the title, clear of the pictogram
SCREEN_W = 256
SCREEN_H = int(round(SCREEN_W * (SCREEN_Y1 - SCREEN_Y0) / (SCREEN_X1 - SCREEN_X0)))   # 189
SCREEN_BG = (8, 10, 14)

# --- the bay display ------------------------------------------------------------------------
# TileEntityBusBayDisplayRenderer: the screen is the arrival display's size (12.8 x 4.4), so
# the two share the panel's display lists.
BAY_SCREEN_X0, BAY_SCREEN_X1 = 1.6, 14.4
BAY_SCREEN_Y0, BAY_SCREEN_Y1 = 5.4, 9.8
BAY_SCREEN_Z = 14.6
BAY_HOUSING = (0.5, 4.0, 15.0, 15.5, 12.2, 16.0)
BAY_K = 4          # texels a unit on the housing's front
BAY_LED_K = 5      # texels a unit on the dark LED screen


def board_screen():
    """The departure board's screen: the header band with a bus pictogram, the column heads'
    band and the rows' bands. Every word is the renderer's."""
    img = Image.new("RGBA", (SCREEN_W, SCREEN_W), (0, 0, 0, 255))
    k = SCREEN_W / (SCREEN_X1 - SCREEN_X0)
    lc.rect(img, 0, 0, SCREEN_W, SCREEN_H, SCREEN_BG)
    hdr = int(round(HEADER_H * k))
    lc.rect(img, 0, 0, SCREEN_W, hdr, (24, 28, 36))
    lc.rect(img, 0, hdr - 2, SCREEN_W, hdr, AMBER)
    # the pictogram: a bus on an amber square, left of the title
    sq = hdr - 8
    lc.rect(img, 5, 3, 5 + sq, 3 + sq, AMBER)
    ga.bus(img, 7, 3 + sq * 0.2, sq - 4, sq * 0.62, (24, 28, 36), AMBER)
    assert 5 + sq < TITLE_X * k, "the pictogram runs into the title"
    col = int(round((HEADER_H + COLHEAD_H) * k))
    lc.rect(img, 0, hdr, SCREEN_W, col, (18, 22, 30))
    for i in range(ROWS):
        y0 = int(round((HEADER_H + COLHEAD_H + i * ROW_PITCH) * k))
        y1 = int(round((HEADER_H + COLHEAD_H + (i + 1) * ROW_PITCH) * k))
        if i % 2:
            lc.rect(img, 0, y0, SCREEN_W, min(y1, SCREEN_H), (15, 18, 24))
    return img


def bay_front():
    """The bay display's housing front: black, with NEXT BUSES printed over the screen."""
    x0, y0, _, x1, y1, _ = BAY_HOUSING
    w = int(round((x1 - x0) * BAY_K))
    img = ga.grain(64, (20, 20, 22), 1, 740)
    # the strip above the hood: the label's five texel rows sit a texel under the top
    hood_top = int(round((y1 - (BAY_SCREEN_Y1 + 0.6)) * BAY_K))
    assert hood_top >= 7, "no room for the label above the hood"
    lc.draw_text_centred(img, "NEXT BUSES", w / 2.0, 1, (230, 230, 226))
    return img


def bay_screen():
    """The bay display's LED screen, dark: a faint grid of unlit amber dots."""
    w = int(round((BAY_SCREEN_X1 - BAY_SCREEN_X0) * BAY_LED_K))
    h = int(round((BAY_SCREEN_Y1 - BAY_SCREEN_Y0) * BAY_LED_K))
    img = Image.new("RGBA", (64, 64), (6, 6, 6, 255))
    px = img.load()
    for y in range(1, h, 2):
        for x in range(1, w, 2):
            px[x, y] = (34, 22, 8, 255)
    return img


def register_textures():
    tex = {
        "bus_board_screen": board_screen,
        "bus_board_bezel": lambda: ga.grain(16, (26, 27, 30), 1, 741),
        "bay_front": bay_front,
        "bay_screen": bay_screen,
        "steel": lambda: ga.grain(16, gp.STEEL, 3, 742),
    }
    for name, draw in tex.items():
        C.texture(name)(draw)


def rods(top):
    """Two rods from the display's top to a plate on the ceiling of the block above."""
    out = []
    for x in (3.0, 13.0):
        out += [gp.rod(x, 15.5, top, 15.6), B((x - 0.8, 15.6, 14.7), (x + 0.8, 16, 16.0),
                                              "steel", SIDES + ("down",))]
    return out


def display(reg, java, names, body, tex, top, gui_scale):
    parts = [("monitor", {}, body), ("rods", {"hung": True}, rods(top))]
    models, state = multipart(parts, reg, tex)
    C.add(reg, java, names, models, state,
          item=ga.item_of(parts, tex, display=gui_display(gui_scale, 0.0, (0, 180, 0))), tab=TAB)


def multipart(parts, prefix, textures):
    """gen_transit_airport.multipart's rules (each part turned to the four facings, conditions
    on actual-state booleans), for this catalogue's model paths."""
    models = {}
    rules = []
    for name, cond, els in parts:
        mname = "%s_%s" % (prefix, name)
        models[mname] = lc.model(textures, els)
        for f, y in gp.FACINGS:
            when = {"facing": f}
            for k, v in cond.items():
                when[k] = "true" if v else "false"
            apply = {"model": C.M(mname)}
            if y:
                apply["y"] = y
            rules.append({"when": when, "apply": apply})
    return models, {"multipart": rules}


def departure_board():
    x0, y0, z0, x1, y1, z1 = BEZEL
    body = [B((x0, y0, z0), (x1, y1, z1), "bezel", ALL),
            B((SCREEN_X0, SCREEN_Y0, SCREEN_Z), (SCREEN_X1, SCREEN_Y1, z0), "bezel",
              ("north", "east", "west", "up", "down"), per={"north": "screen"},
              uv={"north": win(SCREEN_W, SCREEN_H, SCREEN_W)})]
    tex = {"bezel": C.T("bus_board_bezel"), "screen": C.T("bus_board_screen"),
           "steel": C.T("steel"), "particle": C.T("bus_board_bezel")}
    reg = "bus_departure_board"
    display(reg, 'new BlockBusBoard("%s", new double[]{0, 2, 14.6, 16, 14, 16}, false)' % reg,
            names_of("Bus Departure Board", "Bus-Abfahrtsanzeige",
                     "Panel de Salidas de Autobuses", "Avgångstavla för Bussar"),
            body, tex, y1, 0.6)


def bay_display():
    x0, y0, z0, x1, y1, z1 = BAY_HOUSING
    body = [B((x0, y0, z0), (x1, y1, z1), "housing", ALL, per={"north": "front"},
              uv={"north": [0, 0, (x1 - x0) * BAY_K / 4.0, (y1 - y0) * BAY_K / 4.0]}),
            B((BAY_SCREEN_X0, BAY_SCREEN_Y0, BAY_SCREEN_Z), (BAY_SCREEN_X1, BAY_SCREEN_Y1, z0),
              "housing", ("north", "east", "west", "up", "down"), per={"north": "screen"},
              uv={"north": win((BAY_SCREEN_X1 - BAY_SCREEN_X0) * BAY_LED_K,
                               (BAY_SCREEN_Y1 - BAY_SCREEN_Y0) * BAY_LED_K, 64)}),
            # a hood over the screen, clear of its top face so neither is moved
            B((BAY_SCREEN_X0 - 0.4, BAY_SCREEN_Y1 + 0.2, 13.8),
              (BAY_SCREEN_X1 + 0.4, BAY_SCREEN_Y1 + 0.6, z0), "housing",
              ("north", "east", "west", "up", "down"))]
    tex = {"housing": C.T("bus_board_bezel"), "front": C.T("bay_front"),
           "screen": C.T("bay_screen"), "steel": C.T("steel"),
           "particle": C.T("bus_board_bezel")}
    reg = "bus_bay_display"
    display(reg, 'new BlockBusBoard("%s", new double[]{0.5, 4, 13.8, 15.5, 12.2, 16}, true)'
            % reg,
            names_of("Bus Bay Display", "Bussteig-Anzeige", "Pantalla de Andén de Autobús",
                     "Avgångsskärm för Hållplatsläge"),
            body, tex, y1, 0.6)


C.add_lang("csm.transit.board.filter_all", (
    "Departure board: every agency", "Abfahrtsanzeige: alle Verkehrsbetriebe",
    "Panel de salidas: todas las empresas", "Avgångstavla: alla trafikbolag"))
C.add_lang("csm.transit.board.filter", (
    "Departure board: %s only", "Abfahrtsanzeige: nur %s", "Panel de salidas: solo %s",
    "Avgångstavla: endast %s"))
C.add_lang("csm.transit.board.announce_on", (
    "Departure announcements on", "Abfahrtsansagen an", "Anuncios de salida activados",
    "Avgångsutrop på"))
C.add_lang("csm.transit.board.announce_off", (
    "Departure announcements off", "Abfahrtsansagen aus", "Anuncios de salida desactivados",
    "Avgångsutrop av"))
C.add_lang("csm.transit.board.announce_no_tts", (
    "Departure announcements on (heard only with the Text to Speech module)",
    "Abfahrtsansagen an (nur mit dem Text-to-Speech-Modul zu hören)",
    "Anuncios de salida activados (solo se oyen con el módulo Text to Speech)",
    "Avgångsutrop på (hörs bara med modulen Text to Speech)"))

register_textures()
departure_board()
bay_display()

if __name__ == "__main__":
    sys.exit(C.main())
