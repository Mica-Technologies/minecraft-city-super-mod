#!/usr/bin/env python3
"""The large bus arrival boards (bus_arrival_board_3x1, bus_arrival_board_5x2), the info totem
(bus_info_totem) and the invisible part their cells are filled with (bus_arrival_board_part).

The boards and the totem are drawn whole by their renderer (TileEntityBusArrivalBoardRenderer):
their block model is empty (a particle texture only) and the part's is too, so all this writes is
their blockstates (facing x mount for the masters, toward for the part, every variant the empty
model), an item model for each (a small model of the board, which also puts the two textures on
the block atlas), the two textures, and the lang: the four names, the editor's captions and the
"no room" message, in all four locales.

    python dev-env-utils/scripts/gen_transit_arrival_boards.py
    python dev-env-utils/scripts/gen_transit_arrival_boards.py --check
    python dev-env-utils/scripts/gen_transit_arrival_boards.py --fragments
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_arrival_boards.py", "transit/boards", "transit/boards",
                 assets=ASSETS)
TAB = "CsmTabTransit"
HIDDEN = "CsmTabTransitHidden"

HOUSING = (43, 46, 51)
SCREEN = (8, 9, 10)
AMBER = (255, 163, 26)
TEAL = (60, 200, 216)
CONCRETE = (142, 140, 134)
GROUND = (14, 94, 111)
LEGEND = (244, 196, 40)


@C.texture("housing")
def housing():
    """The housing's powder-coated charcoal, the boards' particle."""
    img = lc.fill(HOUSING, 16, 3, 411)
    lc.frame(img, 0, 0, 16, 16, lc.shade(HOUSING, 0.8))
    return img


@C.texture("face")
def face():
    """An icon of a lit board: a teal header and two amber lines on a dark screen, in rows 4 to
    12 (the item's front face shows that band); the totem's name panel and map above and below."""
    img = lc.fill(HOUSING, 16, 2, 412)
    lc.rect(img, 1, 4, 15, 12, SCREEN)
    lc.rect(img, 2, 5, 10, 6, TEAL)
    lc.rect(img, 2, 7, 8, 8, AMBER)
    lc.rect(img, 11, 7, 14, 8, AMBER)
    lc.rect(img, 2, 9, 8, 10, AMBER)
    lc.rect(img, 11, 9, 14, 10, AMBER)
    # rows 0-3: the totem's name panel; rows 12-16: its map
    lc.rect(img, 0, 0, 16, 4, GROUND)
    lc.rect(img, 3, 1, 13, 3, LEGEND)
    lc.rect(img, 1, 12, 15, 16, (226, 228, 222))
    lc.rect(img, 2, 13, 14, 14, (40, 150, 70))
    lc.rect(img, 7, 12, 8, 16, (210, 60, 50))
    return img


def item_display(scale=0.62):
    return {
        "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.35] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def board_item(width, height):
    """The board as an item: its proportions in a 16-wide box, the screen on its front."""
    h = 16.0 * height / width
    y0 = 8 - h / 2
    tex = {"housing": C.T("housing"), "face": C.T("face"), "particle": C.T("housing")}
    el = lc.box((0, y0, 7), (16, y0 + h, 9), "housing")
    el["faces"]["north"] = {"texture": "#face", "uv": [0, 4, 16, 12]}
    el["faces"]["south"] = {"texture": "#face", "uv": [16, 4, 0, 12]}
    rods = [lc.box((3, y0 + h, 7.5), (4, 16, 8.5), "housing"),
            lc.box((12, y0 + h, 7.5), (13, 16, 8.5), "housing")]
    m = lc.model(tex, [el] + rods)
    m["display"] = item_display()
    return m


def totem_item():
    """The totem as an item: plinth, body, name panel, screen and map."""
    tex = {"housing": C.T("housing"), "face": C.T("face"), "particle": C.T("housing")}
    plinth = lc.box((4.5, 0, 6), (11.5, 1, 10), "housing")
    body = lc.box((5, 1, 6.75), (11, 16, 9.25), "housing")
    body["faces"]["north"] = {"texture": "#face", "uv": [5, 0, 11, 16]}
    body["faces"]["south"] = {"texture": "#face", "uv": [11, 0, 5, 16]}
    m = lc.model(tex, [plinth, body])
    m["display"] = item_display(0.7)
    return m


def empty_model():
    """The masters' and the part's block model: nothing drawn, the housing's particles."""
    return {"textures": {"particle": C.T("housing")}}


def master_blockstate(item_model):
    return {
        "forge_marker": 1,
        "defaults": {"model": C.M("arrival_board_empty")},
        "variants": {
            "facing": {"north": {}, "east": {}, "south": {}, "west": {}},
            "mount": {"hung": {}, "post": {}, "wall": {}, "floor": {}},
            "inventory": [{"model": C.M(item_model)}],
        },
    }


def part_blockstate():
    return {
        "forge_marker": 1,
        "defaults": {"model": C.M("arrival_board_empty")},
        "variants": {
            "toward": {d: {} for d in ("down", "up", "north", "south", "west", "east")},
            "inventory": [{}],
        },
    }


GUI = {
    "agency": ("Agency: %s", "Verkehrsbetrieb: %s", "Operador: %s", "Trafikföretag: %s"),
    "custom": ("Custom", "Eigener", "Personalizado", "Eget"),
    "custom_name": ("Agency name", "Name", "Nombre", "Namn"),
    "custom_colours": ("Colours (hex)", "Farben (hex)", "Colores (hex)", "Färger (hex)"),
    "stop_name": ("Stop name", "Haltestellenname", "Nombre de la parada", "Hållplatsnamn"),
    "direction": ("Direction (blank: one page)", "Richtung (leer: eine Seite)",
                  "Dirección (vacía: una página)", "Riktning (tom: en sida)"),
    "routes": ("Routes, e.g. B 30 (blank: the nearest stop's)",
               "Linien, z. B. B 30 (leer: die der nächsten Haltestelle)",
               "Líneas, p. ej. B 30 (vacío: las de la parada más cercana)",
               "Linjer, t.ex. B 30 (tomt: närmaste hållplatsens)"),
    "destinations": ("Destination of each route (blank: none)",
                     "Fahrtziel je Linie (leer: keins)",
                     "Destino de cada línea (vacío: ninguno)",
                     "Destination per linje (tom: ingen)"),
    "closed": ("Closed: %s", "Geschlossen: %s", "Cerrada: %s", "Stängd: %s"),
    "two_sided": ("Two-sided: %s", "Beidseitig: %s", "Dos caras: %s", "Dubbelsidig: %s"),
    "save": ("Save", "Speichern", "Guardar", "Spara"),
    "blocked": ("No room for the board: %s %s %s is in the way",
                "Kein Platz für die Anzeige: %s %s %s ist im Weg",
                "No hay sitio para el panel: %s %s %s estorba",
                "Ingen plats för tavlan: %s %s %s är i vägen"),
}


def blocks():
    C.add("bus_arrival_board_3x1", 'new BlockBusArrivalBoard("bus_arrival_board_3x1", 3, 1, false)',
          ("Bus Arrival Board (3x1)", "Bus-Abfahrtsanzeige (3x1)",
           "Panel de Llegadas de Autobús (3x1)", "Bussankomsttavla (3x1)"),
          {"arrival_board_empty": empty_model(), "arrival_board_3x1_item": board_item(3, 1)},
          master_blockstate("arrival_board_3x1_item"), tab=TAB)
    C.add("bus_arrival_board_5x2", 'new BlockBusArrivalBoard("bus_arrival_board_5x2", 5, 2, false)',
          ("Bus Arrival Board (5x2)", "Bus-Abfahrtsanzeige (5x2)",
           "Panel de Llegadas de Autobús (5x2)", "Bussankomsttavla (5x2)"),
          {"arrival_board_5x2_item": board_item(5, 2)},
          master_blockstate("arrival_board_5x2_item"), tab=TAB)
    C.add("bus_info_totem", 'new BlockBusArrivalBoard("bus_info_totem", 1, 3, true)',
          ("Bus Stop Info Totem", "Haltestellen-Infosäule", "Tótem de Información de Parada",
           "Hållplatsens informationspelare"),
          {"info_totem_item": totem_item()}, master_blockstate("info_totem_item"), tab=TAB)
    C.add("bus_arrival_board_part", "new BlockBusArrivalBoardPart()",
          ("Bus Arrival Board Part", "Bus-Abfahrtsanzeige (Teil)",
           "Panel de Llegadas de Autobús (Parte)", "Bussankomsttavla (del)"),
          {}, part_blockstate(), tab=HIDDEN)
    for key, names in GUI.items():
        C.add_lang("csm.transit.arrival_board." + key, names)


blocks()

if __name__ == "__main__":
    sys.exit(C.main())
