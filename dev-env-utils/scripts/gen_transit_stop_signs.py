#!/usr/bin/env python3
"""The full-size bus stop's sign blocks: the pole's flag (bus_stop_pole_flag) and the stop-name
blade (bus_stop_name_blade). Both are Core's wayfinding panel, drawn by its baked model from their
NBT, so all this writes is their blockstates (Core's empty panel model, turned by facing), their
item models, the blade's item texture and their lang, including the pole item's messages.

    python dev-env-utils/scripts/gen_transit_stop_signs.py
    python dev-env-utils/scripts/gen_transit_stop_signs.py --check
    python dev-env-utils/scripts/gen_transit_stop_signs.py --fragments
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_stop_signs.py", "transit/stops", "transit/stops", assets=ASSETS)
TAB = "CsmTabTransit"

TEAL = (14, 94, 111)
YELLOW = (244, 196, 40)


@C.texture("blade_item")
def blade_item():
    """The blade's icon: a teal plate with a line of yellow 'lettering'."""
    img = lc.fill(TEAL, 16, 2, 401)
    lc.rect(img, 2, 6, 14, 7, YELLOW)
    lc.rect(img, 3, 9, 13, 10, YELLOW)
    lc.frame(img, 0, 0, 16, 16, lc.shade(TEAL, 0.7))
    return img


def facing_blockstate(item_model):
    return {
        "forge_marker": 1,
        "defaults": {"model": "csm:wayfinding/wayfinding_panel_base"},
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "inventory": [{"model": C.M(item_model)}],
        },
    }


def item_display():
    return {
        "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.62] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.35] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def flag_item():
    """The pole: a post with the CITYLINE flag beside it, as the item."""
    tex = {"flag": "csm:blocks/transit/stops/flag_cityline", "post": "csm:blocks/transit/stops/clamp",
           "particle": "csm:blocks/transit/stops/flag_cityline"}
    els = [lc.box((2, -8, 7.5), (3.5, 24, 9), "post"),
           lc.box((3.5, 4, 7.75), (14, 22, 8.75), "flag",
                   faces=("north", "south", "east", "west", "up", "down"))]
    els[1]["faces"]["north"]["uv"] = [0, 0, 16, 16]
    els[1]["faces"]["south"]["uv"] = [16, 0, 0, 16]
    m = lc.model(tex, els)
    m["display"] = item_display()
    return m


def blade_model():
    tex = {"b": C.T("blade_item"), "particle": C.T("blade_item")}
    m = lc.model(tex, [lc.box((-4, 5, 7.25), (20, 11, 8.75), "b")])
    m["display"] = item_display()
    return m


MESSAGES = {
    "short": ("A full stop needs %s flags; placed one flag",
              "Eine ganze Haltestelle braucht %s Fahnen; eine Fahne gesetzt",
              "Una parada completa necesita %s banderas; se colocó una bandera",
              "En hel hållplats kräver %s flaggor; en flagga placerad"),
    "blocked": ("No room for the stop: %s %s %s is in the way; placed one flag",
                "Kein Platz für die Haltestelle: %s %s %s ist im Weg; eine Fahne gesetzt",
                "No hay sitio para la parada: %s %s %s estorba; se colocó una bandera",
                "Ingen plats för hållplatsen: %s %s %s är i vägen; en flagga placerad"),
}


def blocks():
    C.add("bus_stop_pole_flag", 'new BlockBusStopPoleFlag("bus_stop_pole_flag")',
          ("Bus Stop Pole (Full Size)", "Bushaltestellenmast (volle Größe)",
           "Poste de Parada de Autobús (Tamaño Completo)", "Busshållplatsstolpe (Fullstor)"),
          {"pole_flag_item": flag_item()}, facing_blockstate("pole_flag_item"), tab=TAB)
    C.add("bus_stop_name_blade", 'new BlockBusStopBlade("bus_stop_name_blade")',
          ("Bus Stop Name Blade", "Haltestellen-Namensschild", "Rótulo de Nombre de Parada",
           "Hållplatsnamnskylt"),
          {"name_blade_item": blade_model()}, facing_blockstate("name_blade_item"), tab=TAB)
    for key, names in MESSAGES.items():
        C.add_lang("csm.transit.stop_pole." + key, names)


blocks()

if __name__ == "__main__":
    sys.exit(C.main())
