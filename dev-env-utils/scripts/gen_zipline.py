#!/usr/bin/env python3
"""
gen_zipline.py -- the Parks & Greenery zipline's assets (#351).

The launch and landing anchors (a post-top bracket with a crossed cable eye, launch accented
green and landing red), the Zipline Linker's item texture and model, and every lang line the
zipline uses, in the four locales, under modules/parks/.../assets/csm:

  * models/block/parks/zipline/anchor_<launch|landing>.json
  * blockstates/zipline_anchor_<launch|landing>.json
  * textures/items/parks/zipline_linker.png, models/item/zipline_linker.json
  * lang/<locale>.lang: the block and item names and the chat messages

The anchor has no facing: the cable eye is a cross, so it reads the same whichever way a line
leaves it. The cable leaves at ZiplineLine.CABLE_HEIGHT (14/16), the middle of the eye. The
textures are the park amenities' own (gen_park_amenities.py).

Usage:
    python gen_zipline.py              # write everything
    python gen_zipline.py --check      # fail if the tree has drifted
    python gen_zipline.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import os
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_park_plantings as pp  # noqa: E402
import gen_trees  # noqa: E402
import model_depth  # noqa: E402

ASSETS = pp.ASSETS
LOCALES = pp.LOCALES  # en_us, de_de, es_es, sv_se
AMENITY = "csm:blocks/parks/amenities/"
box = pp.box

ROLES = {
    # role: (accent texture, names in LOCALES order)
    "launch": ("green_steel", ["Zipline Anchor (Launch)", "Seilrutschen-Anker (Start)",
                               "Anclaje de tirolesa (salida)", "Linbaneankare (start)"]),
    "landing": ("red_steel", ["Zipline Anchor (Landing)", "Seilrutschen-Anker (Landung)",
                              "Anclaje de tirolesa (llegada)", "Linbaneankare (landning)"]),
}

LINKER_NAMES = ["Zipline Linker", "Seilrutschen-Verbinder", "Enlazador de tirolesa",
                "Linbanelänkare"]

# Chat and tooltip lines: key -> names in LOCALES order. %s are filled by the game.
MESSAGES = {
    "csm.zipline.not_launch": [
        "%s is not a zipline launch anchor.",
        "%s ist kein Seilrutschen-Startanker.",
        "%s no es un anclaje de salida de tirolesa.",
        "%s är inget startankare för linbana."],
    "csm.zipline.not_landing": [
        "%s is not a zipline landing anchor.",
        "%s ist kein Seilrutschen-Landeanker.",
        "%s no es un anclaje de llegada de tirolesa.",
        "%s är inget landningsankare för linbana."],
    "csm.zipline.too_long": [
        "That line would be %s blocks long; the longest allowed is %s.",
        "Die Strecke wäre %s Blöcke lang; erlaubt sind höchstens %s.",
        "La línea mediría %s bloques; el máximo permitido es %s.",
        "Linan skulle bli %s block lång; längst tillåtet är %s."],
    "csm.zipline.too_short": [
        "The anchors are too close together for a zipline.",
        "Die Anker liegen für eine Seilrutsche zu nah beieinander.",
        "Los anclajes están demasiado cerca para una tirolesa.",
        "Ankarna sitter för nära varandra för en linbana."],
    "csm.zipline.blocked": [
        "The line is blocked at %s: a rider would hit it.",
        "Die Strecke ist bei %s blockiert: ein Fahrer würde dagegenstoßen.",
        "La línea está bloqueada en %s: un pasajero chocaría.",
        "Linan är blockerad vid %s: en åkare skulle slå i."],
    "csm.zipline.no_landing": [
        "There is no landing platform under the end of the line, within 4 blocks of a rider's"
        " feet.",
        "Unter dem Ende der Strecke ist keine Landeplattform, höchstens 4 Blöcke unter den Füßen"
        " eines Fahrers.",
        "No hay plataforma de llegada bajo el final de la línea, a 4 bloques de los pies del"
        " pasajero.",
        "Det finns ingen landningsplattform under linans slut, inom 4 block under åkarens"
        " fötter."],
    "csm.zipline.linked": [
        "Zipline strung: %s blocks long, dropping %s.",
        "Seilrutsche gespannt: %s Blöcke lang, %s Gefälle.",
        "Tirolesa tendida: %s bloques de largo, %s de desnivel.",
        "Linbana spänd: %s block lång, %s i fall."],
    "csm.zipline.unlinked": [
        "Zipline taken down.",
        "Seilrutsche abgebaut.",
        "Tirolesa desmontada.",
        "Linbanan nedmonterad."],
    "csm.zipline.first": [
        "Launch anchor at %s selected. Now click a landing anchor.",
        "Startanker bei %s gewählt. Jetzt einen Landeanker anklicken.",
        "Anclaje de salida en %s seleccionado. Ahora haz clic en un anclaje de llegada.",
        "Startankare vid %s valt. Klicka nu på ett landningsankare."],
    "csm.zipline.pick_launch": [
        "Click a launch anchor first.",
        "Zuerst einen Startanker anklicken.",
        "Primero haz clic en un anclaje de salida.",
        "Klicka först på ett startankare."],
    "csm.zipline.no_line": [
        "This launch anchor has no line. String one with the Zipline Linker.",
        "Dieser Startanker hat keine Strecke. Spanne eine mit dem Seilrutschen-Verbinder.",
        "Este anclaje de salida no tiene línea. Tiende una con el Enlazador de tirolesa.",
        "Det här startankaret har ingen lina. Spänn en med linbanelänkaren."],
    "csm.zipline.wait": [
        "Wait for the rider ahead to get clear.",
        "Warte, bis der Fahrer vor dir weit genug weg ist.",
        "Espera a que el pasajero de delante se aleje.",
        "Vänta tills åkaren framför har kommit undan."],
    "csm.zipline.hold_on": [
        "Hold on! You can let go over the landing.",
        "Festhalten! Loslassen geht erst über der Landung.",
        "¡Agárrate! Puedes soltarte sobre la llegada.",
        "Håll i dig! Du kan släppa över landningen."],
    "csm.zipline.info": [
        "%s: %s, line to %s, %s blocks long, dropping %s.",
        "%s: %s, Strecke nach %s, %s Blöcke lang, %s Gefälle.",
        "%s: %s, línea hasta %s, %s bloques de largo, %s de desnivel.",
        "%s: %s, lina till %s, %s block lång, %s i fall."],
    "csm.zipline.info_none": [
        "%s: %s, no line.",
        "%s: %s, keine Strecke.",
        "%s: %s, sin línea.",
        "%s: %s, ingen lina."],
    "csm.zipline.not_anchor": [
        "%s is not a zipline anchor.",
        "%s ist kein Seilrutschen-Anker.",
        "%s no es un anclaje de tirolesa.",
        "%s är inget linbaneankare."],
    "csm.zipline.role.launch": [
        "launch anchor", "Startanker", "anclaje de salida", "startankare"],
    "csm.zipline.role.landing": [
        "landing anchor", "Landeanker", "anclaje de llegada", "landningsankare"],
    "csm.zipline.linker.tooltip": [
        "Click a launch anchor, then a landing anchor. Sneak-click an anchor to take its line"
        " down.",
        "Startanker, dann Landeanker anklicken. Mit Schleichen-Klick die Strecke eines Ankers"
        " abbauen.",
        "Haz clic en un anclaje de salida y luego en uno de llegada. Agáchate y haz clic para"
        " desmontar la línea.",
        "Klicka på ett startankare och sedan ett landningsankare. Smyg-klicka på ett ankare för"
        " att ta ner linan."],
    "csm.zipline.anchor.tooltip": [
        "Right-click with an empty hand to ride its line; sneak-click to turn its lights on or"
        " off.",
        "Mit leerer Hand rechtsklicken, um mitzufahren; mit Schleichen-Klick die Beleuchtung"
        " schalten.",
        "Haz clic derecho con la mano vacía para montar; agáchate y haz clic para encender o"
        " apagar sus luces.",
        "Högerklicka med tom hand för att åka; smyg-klicka för att tända eller släcka linans"
        " lampor."],
    "csm.zipline.lights_on": [
        "Line lights on.", "Streckenbeleuchtung an.", "Luces de la línea encendidas.",
        "Linans lampor tända."],
    "csm.zipline.lights_off": [
        "Line lights off.", "Streckenbeleuchtung aus.", "Luces de la línea apagadas.",
        "Linans lampor släckta."],
}


def anchor_model(accent):
    """The anchor; ``accent`` is the collar's texture, an amenity texture's name."""
    galv = "galvanized"
    iron = "iron"
    elements = [
        box([3, 0, 3], [13, 1, 13], galv),            # base plate, bolted to the post top
        box([6.5, 1, 6.5], [9.5, 11, 9.5], iron),     # mast
        box([5.5, 11, 5.5], [10.5, 12.5, 10.5], "accent"),  # collar: green launch, red landing
        box([7.25, 12.5, 3.5], [8.75, 16, 12.5], iron),   # cable eye, one plate
        box([3.5, 12.5, 7.25], [7.25, 15.75, 8.75], iron),   # and the crossing plate, either side
        box([8.75, 12.5, 7.25], [12.5, 15.75, 8.75], iron),
    ]
    return pp.model({"galvanized": AMENITY + "locker_galvanized", "iron": AMENITY + "iron",
                     "accent": AMENITY + accent, "particle": AMENITY + "iron"},
                    elements)


def blockstate(role):
    return {
        "forge_marker": 1,
        "defaults": {"model": "csm:parks/zipline/anchor_%s" % role},
        "variants": {"normal": [{}], "inventory": [{}]},
    }


def linker_texture():
    """A trolley on a length of cable: a grey sheave and hanger over a yellow grip bar."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cable = (70, 72, 78, 255)
    for x in range(16):  # the cable, falling left to right
        y = 3 + x // 4
        px[x, y] = cable
    sheave = [(92, 96, 104, 255), (150, 154, 162, 255), (200, 204, 210, 255)]
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx * dx + dy * dy <= 5:
                px[7 + dx, 5 + dy] = sheave[0] if dx * dx + dy * dy > 2 else sheave[1]
    px[7, 5] = sheave[2]
    for y in range(8, 12):  # hanger straps
        px[5, y] = (60, 60, 64, 255)
        px[9, y] = (60, 60, 64, 255)
    for x in range(3, 12):  # grip bar
        px[x, 12] = (232, 190, 40, 255)
        px[x, 13] = (186, 146, 24, 255)
    return img


def outputs():
    """{relative path: JSON data or a PIL image}."""
    out = {}
    for role, (accent, _names) in ROLES.items():
        out["models/block/parks/zipline/anchor_%s.json" % role] = anchor_model(accent)
        out["blockstates/zipline_anchor_%s.json" % role] = blockstate(role)
    out["models/item/zipline_linker.json"] = {
        "parent": "item/generated",
        "textures": {"layer0": "csm:items/parks/zipline_linker"},
    }
    out["textures/items/parks/zipline_linker.png"] = linker_texture()
    return out


def lang_entries():
    entries = {loc: {} for loc in LOCALES}
    for role, (_accent, names) in ROLES.items():
        for i, loc in enumerate(LOCALES):
            entries[loc]["tile.zipline_anchor_%s.name" % role] = names[i]
    for i, loc in enumerate(LOCALES):
        entries[loc]["item.zipline_linker.name"] = LINKER_NAMES[i]
        for key, texts in MESSAGES.items():
            entries[loc][key] = texts[i]
    return entries


def generate(assets):
    written = []
    for rel, data in outputs().items():
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if isinstance(data, Image.Image):
            data.save(path)
        else:
            pp.dump(path, data)
        written.append(rel)
    gen_trees.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in LOCALES]
    model_depth.separate(assets, written, pp.dump)
    return written


def fragments():
    return "\n".join([
        '    initTabBlock(new BlockZiplineAnchor("zipline_anchor_launch"));',
        '    initTabBlock(new BlockZiplineAnchor("zipline_anchor_landing"));',
        "    initTabItem(ItemZiplineLinker.class, fmlPreInitializationEvent);",
    ])


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="zipline_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("zipline assets are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
