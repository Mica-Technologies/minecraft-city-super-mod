#!/usr/bin/env python3
"""Generate the signs that fill the gaps found by the 2026-09 catalogue review.

One catalogue, one command. Each entry names a sign, the shape of plate it goes on, where its
face comes from (the official FHWA drawing through shs_signs, or Highway Gothic text through
render_sign where the book has no sign at the mod's wording), the plain sign it sits beside in
the creative tab, and its display name in the four shipped languages.
From that the script writes:

    textures/blocks/trafficsigns/<registry>.png          128 x 128, drawn at the plate's aspect
    textures/blocks/trafficsigns/<registry>_back.png     silhouette signs only: the gray back
    blockstates/<registry>.json                          the shape sibling's, retextured

and with --apply also inserts the lang line into every language file and the tab line after
its sibling in CsmTabRoadSigns. Without --apply the lang and tab lines are printed.

Plates and aspects. A sign texture is square but is stretched onto its model's plate, so the
face is drawn at the plate's aspect and then squished to 128 x 128; in the world it comes back
out at the right proportions. The shapes and the sibling whose blockstate is cloned for each:

    diamond    23 x 23  signbump            warning diamonds
    portrait   16 x 21  signspeed65         speed limits and other tall regulatory signs
    square     16 x 16  signnouturn         square regulatory signs and 24" plaques
    wide       22 x 16  signwrongway        landscape regulatory signs
    plaque     16 x 8   signaheadplaque     supplemental plaques
    silhouette 23 x 23  yieldsign           anything whose outline is none of the above: the
                                            yield model draws the texture's own alpha on a bare
                                            plate, with a gray back texture on slot "2"

Run from the repo root:  python dev-env-utils/scripts/gen_gap_signs.py [--apply] [--check]
"""

import json
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import render_sign as rs  # noqa: E402
import shs_signs as shs  # noqa: E402

SS = rs.SS
SIZE = rs.SIZE  # supersampled square canvas
OWNER = layout.owner_of_folder('trafficsigns')
TEX_DIR = layout.asset_dir_for_write(OWNER, 'textures/blocks/trafficsigns')
BS_DIR = layout.asset_dir_for_write(OWNER, 'blockstates')
LANG_DIR = layout.asset_dir_for_write(OWNER, 'lang')
TAB = layout.resolve_source('tabs/CsmTabRoadSigns.java')

YELLOW = shs.MOD_COLOURS['yellow']
WHITE = shs.MOD_COLOURS['white']
BLACK = shs.MOD_COLOURS['black']
RED = shs.MOD_COLOURS['red']
FYG = shs.MOD_COLOURS['fyg']

SHAPES = {
    'diamond': ('signbump', 1.0),
    'portrait': ('signspeed65', 16 / 21),
    'square': ('signnouturn', 1.0),
    'wide': ('signwrongway', 22 / 16),
    'plaque': ('signaheadplaque', 2.0),
    'silhouette': ('yieldsign', 1.0),
    'circle': ('signfdcstandpipe', 1.0),
}
LANGS = ('en_us', 'es_es', 'de_de', 'sv_se')


# ----------------------------------------------------------------------------- canvases

def _canvas(aspect):
    """A transparent supersampled canvas at the plate's aspect (width = aspect * height)."""
    w = int(round(SIZE * aspect)) if aspect >= 1 else SIZE
    h = SIZE if aspect >= 1 else int(round(SIZE / aspect))
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def _finish(img):
    """Squish the aspect canvas to the square texture the plate stretches back out."""
    return img.resize((128, 128), Image.LANCZOS)


def _rounded_panel(img, bg, fg, radius=7):
    """Rounded panel filling the canvas with the thin inset border every rendered sign has."""
    d = ImageDraw.Draw(img)
    m, inset, stroke = 3 * SS, 2 * SS, 3 * SS
    box = (m, m, img.width - m, img.height - m)
    d.rounded_rectangle(box, radius=radius * SS, fill=bg)
    d.rounded_rectangle((box[0] + inset, box[1] + inset, box[2] - inset, box[3] - inset),
                        radius=max(2, radius * SS - inset), outline=fg, width=stroke)
    pad = inset + stroke + 5 * SS
    return d, (box[0] + pad, box[1] + pad, box[2] - pad, box[3] - pad)


def _diamond_panel(img, bg, fg):
    """The warning diamond on a square canvas, as render_sign draws it."""
    m, inset, stroke = 3 * SS, 2 * SS, 3 * SS
    side = int((SIZE - 2 * m) / math.sqrt(2))
    sq = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sq)
    r = int(8 * SS)
    sd.rounded_rectangle((0, 0, side - 1, side - 1), radius=r, fill=bg)
    sd.rounded_rectangle((inset, inset, side - 1 - inset, side - 1 - inset),
                         radius=max(2, r - inset), outline=fg, width=stroke)
    sq = sq.rotate(45, expand=True, resample=Image.BICUBIC)
    img.alpha_composite(sq, ((SIZE - sq.width) // 2, (SIZE - sq.height) // 2))
    return ImageDraw.Draw(img)


def text_sign(shape, lines, bg, fg):
    """A text-only sign on the given plate shape."""
    aspect = SHAPES[shape][1]
    if shape == 'diamond':
        img = _canvas(1.0)
        _diamond_panel(img, bg, fg)
        R = (SIZE / 2 - 3 * SS) - (2 * SS + 3 * SS) - 4 * SS
        rs._draw_text_diamond(img, lines, fg, SIZE / 2, SIZE / 2, R)
        return _finish(img)
    img = _canvas(aspect)
    _d, box = _rounded_panel(img, bg, fg)
    rs._draw_text(img, lines, fg, box)
    return _finish(img)


# ----------------------------------------------------------------------------- official faces
# Everything whose design FHWA publishes is rendered from the Standard Highway Signs drawings
# (shs_signs.py) rather than drawn here; only the legends the book does not carry at the
# mod's wording (1000 FT, NEXT 2 MILES, the lane closures with AHEAD, ...) are set in text.
# The palette mapping and the plate fit live in shs_signs.official_face.

def SHS(shape, chapter, page, pick=0, mirror=False, palette=None):
    """A face from a page of the 2004 SHS book (0-based page; ``pick`` for pages with more
    than one sign). ``mirror`` makes the left-hand version of a symbol sign the book draws
    right-handed only."""
    return lambda: shs.official_face(shs.book_sign(chapter, page, pick), SHAPES[shape][1],
                                     mirror, palette)


def SHSI(shape, code, variant=None):
    """A face from an interim SHS ZIP (a sign added or redrawn since the book)."""
    return lambda: shs.official_face(shs.interim_sign(code, variant), SHAPES[shape][1])


# ----------------------------------------------------------------------------- catalogue
# registry, display (en, es, de, sv), shape, drawer, tab sibling (insert after)

def T(shape, lines, bg=YELLOW, fg=BLACK):
    return lambda: text_sign(shape, lines, bg, fg)


def gray_back(face):
    """The back of a silhouette sign: the face's outline in unpainted gray."""
    return shs.back_texture(face)


CATALOGUE = [
    # --- regulatory
    ('signpostkeepleft', ('Keep Left Sign', 'Señal de Mantenerse a la Izquierda',
                          'Links Halten Schild', 'Håll Vänster-Vägmärke'),
     'portrait', SHS('portrait', 'Regulatory', 71), 'signpostkeepright'),
    ('signspeed10', ('Speed Limit 10 Sign', 'Señal de Límite de Velocidad 10',
                     'Geschwindigkeitsbegrenzung 10 Schild', 'Hastighetsbegränsning 10-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '10'], WHITE, BLACK), 'signspeed0'),
    ('signspeed60', ('Speed Limit 60 Sign', 'Señal de Límite de Velocidad 60',
                     'Geschwindigkeitsbegrenzung 60 Schild', 'Hastighetsbegränsning 60-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '60'], WHITE, BLACK), 'signpostspeed55'),
    ('signspeed70', ('Speed Limit 70 Sign', 'Señal de Límite de Velocidad 70',
                     'Geschwindigkeitsbegrenzung 70 Schild', 'Hastighetsbegränsning 70-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '70'], WHITE, BLACK), 'signspeed65'),
    ('signrightmustturnright', ('Right Lane Must Turn Right Sign',
                                'Señal de Carril Derecho Debe Girar a la Derecha',
                                'Rechte Spur Muss Rechts Abbiegen Schild',
                                'Höger Körfält Måste Svänga Höger-Vägmärke'),
     'square', SHS('square', 'Regulatory', 32), 'signrightahead'),
    ('signnoleftoruturn', ('No Left Turn or U-Turn Sign', 'Señal de Prohibido Girar a la Izquierda o en U',
                           'Kein Linksabbiegen oder Wenden Schild', 'Förbjuden Vänstersväng eller U-sväng-Vägmärke'),
     'square', SHS('square', 'Regulatory', 61), 'signnoleftturn'),
    ('signroundaboutdirectional', ('Roundabout Directional Arrow Sign', 'Señal de Flecha Direccional de Rotonda',
                                   'Kreisverkehr Richtungspfeil Schild', 'Rondell Riktningspil-Vägmärke'),
     'wide', SHSI('wide', 'r06_04'), 'signr105a'),
    ('signroundaboutplaque', ('Roundabout Sign (Plaque)', 'Señal de Rotonda (Placa)',
                              'Kreisverkehr Schild (Zusatzschild)', 'Rondell-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['ROUNDABOUT'], WHITE, BLACK), 'signroundaboutdirectional'),
    # --- warning
    ('signbepreparedtostop', ('Be Prepared To Stop Sign', 'Señal de Prepárese para Detenerse',
                              'Bremsbereit Sein Schild', 'Var Beredd att Stanna-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 25), 'basestationradiosign'),
    ('signfallingrocks', ('Fallen Rocks Sign', 'Señal de Rocas Caídas',
                          'Steinschlag Schild', 'Stenras-Vägmärke'),
     'diamond', SHSI('diamond', 'w08_14', '24x24'), 'signexit25'),
    ('signhorse', ('Horse Crossing Sign', 'Señal de Cruce de Caballos',
                   'Reiter Schild', 'Ridande-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 97), 'signhill'),
    ('signloosegravel', ('Loose Gravel Sign', 'Señal de Gravilla Suelta',
                         'Rollsplitt Schild', 'Löst Grus-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 64), 'signleftrightarrow'),
    ('signreversecurveleft', ('Reverse Curve Left Sign', 'Señal de Curva Inversa a la Izquierda',
                              'Doppelkurve Links Schild', 'Dubbelkurva Vänster-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 4, mirror=True), 'signrampsignalahead'),
    ('signreversecurveright', ('Reverse Curve Right Sign', 'Señal de Curva Inversa a la Derecha',
                               'Doppelkurve Rechts Schild', 'Dubbelkurva Höger-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 4), 'signreversecurveleft'),
    ('signreverseturnleft', ('Reverse Turn Left Sign', 'Señal de Giro Inverso a la Izquierda',
                             'Doppelkurve Scharf Links Schild', 'Skarp Dubbelkurva Vänster-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 3, mirror=True), 'signreversecurveright'),
    ('signreverseturnright', ('Reverse Turn Right Sign', 'Señal de Giro Inverso a la Derecha',
                              'Doppelkurve Scharf Rechts Schild', 'Skarp Dubbelkurva Höger-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 3), 'signreverseturnleft'),
    ('signroadnarrows', ('Road Narrows Sign', 'Señal de Estrechamiento de Calzada',
                         'Fahrbahnverengung Schild', 'Avsmalnande Väg-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 35), 'signhightideroadflood'),
    ('signroughroad', ('Rough Road Sign', 'Señal de Calzada Irregular',
                       'Unebene Fahrbahn Schild', 'Ojämn Väg-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 65), 'signroadsplit'),
    ('signrunawaytruckramp', ('Runaway Truck Ramp Sign', 'Señal de Rampa de Escape para Camiones',
                              'Notfallspur Schild', 'Nödficka för Lastbilar-Vägmärke'),
     'wide', SHS('wide', 'Warning', 53), 'signroundabout'),
    ('signnopassingzone', ('No Passing Zone Sign', 'Señal de Zona de Prohibido Adelantar',
                           'Überholverbot Schild', 'Omkörningsförbud-Vägmärke'),
     'silhouette', SHS('silhouette', 'Warning', 118), 'signnooutlet'),
    # --- plaques
    ('signcrosstrafficdoesnotstop', ('Cross Traffic Does Not Stop Sign (Plaque)',
                                     'Señal de Tráfico Transversal No Se Detiene (Placa)',
                                     'Querverkehr Hält Nicht Schild (Zusatzschild)',
                                     'Korsande Trafik Stannar Inte-Vägmärke (Tilläggsskylt)'),
     'plaque', SHS('plaque', 'Warning', 32), 'signcow'),
    ('sign500feet', ('500 Feet Sign (Plaque)', 'Señal de 500 Pies (Placa)',
                     '500 Fuß Schild (Zusatzschild)', '500 Fot-Vägmärke (Tilläggsskylt)'),
     'plaque', SHS('plaque', 'Warning', 122), 'sign4way'),
    ('sign1000feet', ('1000 Feet Sign (Plaque)', 'Señal de 1000 Pies (Placa)',
                      '1000 Fuß Schild (Zusatzschild)', '1000 Fot-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['1000 FT']), 'sign500feet'),
    ('signnext2miles', ('Next 2 Miles Sign (Plaque)', 'Señal de Próximas 2 Millas (Placa)',
                        'Nächste 2 Meilen Schild (Zusatzschild)', 'Nästa 2 Miles-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['NEXT 2 MILES']), 'signnewsignal'),
    # --- pedestrian and school (Phase 2)
    ('signyieldheretopeds', ('Yield Here To Pedestrians Sign', 'Señal de Ceda el Paso Aquí a Peatones',
                             'Hier Fußgängern Vorfahrt Gewähren Schild', 'Lämna Företräde Här för Fotgängare-Vägmärke'),
     'square', SHS('square', 'Regulatory', 6), 'signusecrosswalkright'),
    ('signendschoolzone', ('End School Zone Sign', 'Señal de Fin de Zona Escolar',
                           'Ende Schulzone Schild', 'Slut på Skolzon-Vägmärke'),
     'portrait', SHS('portrait', 'School', 11), 'signyieldheretopeds'),
    ('signschoolbusstopahead', ('School Bus Stop Ahead Sign', 'Señal de Parada de Autobús Escolar Adelante',
                                'Schulbushaltestelle Voraus Schild', 'Skolbusshållplats Framför-Vägmärke'),
     'silhouette', SHSI('silhouette', 's03_01'), 'signendschoolzone'),
    ('signschoolcrossing', ('School Crossing Sign', 'Señal de Cruce Escolar',
                            'Schulweg Schild', 'Skolövergång-Vägmärke'),
     'silhouette', SHS('silhouette', 'School', 0, palette={(255, 245, 0): FYG}), 'signschoolbusstopahead'),
    # --- rail crossing (Phase 4)
    ('signrailroadcrossbuck', ('Railroad Crossing Sign (Crossbuck)', 'Señal de Cruce Ferroviario (Cruz de San Andrés)',
                               'Bahnübergang Andreaskreuz Schild', 'Järnvägskorsning Kryssmärke-Vägmärke'),
     'silhouette', SHS('silhouette', 'Regulatory', 172), 'signphotoenforced'),
    ('signrailroadtracks2', ('2 Tracks Sign (Plaque)', 'Señal de 2 Vías (Placa)',
                             '2 Gleise Schild (Zusatzschild)', '2 Spår-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['2 TRACKS'], WHITE, BLACK), 'signrailroadcrossbuck'),
    ('signdonotstopontracks', ('Do Not Stop On Tracks Sign', 'Señal de No Detenerse Sobre las Vías',
                               'Nicht auf den Gleisen Anhalten Schild', 'Stanna Inte på Spåret-Vägmärke'),
     'portrait', SHS('portrait', 'Regulatory', 113), 'signdonotpass'),
    ('signrailroadadvance', ('Railroad Crossing Advance Warning Sign', 'Señal de Advertencia Anticipada de Cruce Ferroviario',
                             'Bahnübergang Vorwarnung Schild', 'Järnvägskorsning Förvarning-Vägmärke'),
     'circle', SHS('circle', 'Warning', 75), 'signradioradiation'),
    # --- work zone (Phase 3): the lane-closure family, all legends
    ('signleftlaneclosedahead', ('Left Lane Closed Ahead Sign', 'Señal de Carril Izquierdo Cerrado Adelante',
                                 'Linke Spur Gesperrt Voraus Schild', 'Vänster Körfält Avstängt Framför-Vägmärke'),
     'diamond', T('diamond', ['LEFT LANE', 'CLOSED', 'AHEAD'], rs.ORANGE), 'landworkaheadsign'),
    ('signrightlaneclosedahead', ('Right Lane Closed Ahead Sign', 'Señal de Carril Derecho Cerrado Adelante',
                                  'Rechte Spur Gesperrt Voraus Schild', 'Höger Körfält Avstängt Framför-Vägmärke'),
     'diamond', T('diamond', ['RIGHT LANE', 'CLOSED', 'AHEAD'], rs.ORANGE), 'rgrchickensign'),
    ('signonelaneroadahead', ('One Lane Road Ahead Sign', 'Señal de Carretera de Un Carril Adelante',
                              'Einspurige Straße Voraus Schild', 'Enfilig Väg Framför-Vägmärke'),
     'diamond', T('diamond', ['ONE LANE', 'ROAD', 'AHEAD'], rs.ORANGE), 'noguardrailssignrr'),
    ('signutilityworkahead', ('Utility Work Ahead Sign', 'Señal de Trabajos de Servicios Adelante',
                              'Versorgungsarbeiten Voraus Schild', 'Ledningsarbete Framför-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 158), 'signunmarkedpavement'),
    ('signshoulderwork', ('Shoulder Work Sign', 'Señal de Trabajos en el Arcén',
                          'Arbeiten am Seitenstreifen Schild', 'Vägrensarbete-Vägmärke'),
     'diamond', SHS('diamond', 'Warning', 154), 'signsignalworkahead'),
    ('signbridgeout', ('Bridge Out Sign', 'Señal de Puente Fuera de Servicio',
                       'Brücke Gesperrt Schild', 'Bro Avstängd-Vägmärke'),
     'wide', T('wide', ['BRIDGE', 'OUT'], WHITE, BLACK), 'signblastingzone'),
    ('signstatelawstopforpeds', ('State Law Stop For Pedestrians In Crosswalk Sign',
                                 'Señal de Ley Estatal Deténgase por Peatones en el Cruce',
                                 'Landesgesetz Für Fußgänger im Zebrastreifen Anhalten Schild',
                                 'Delstatslag Stanna för Fotgängare på Övergångsstället-Vägmärke'),
     'portrait', SHSI('portrait', 'r01_06c'), 'signslowschool'),
]
for _mph, _after in ((10, 'signaddright'), (15, 'signadvisoryspeed10'), (20, 'signadvisoryspeed15'),
                     (25, 'signadvisoryspeed20'), (30, 'signadvisoryspeed25'), (35, 'signadvisoryspeed30'),
                     (40, 'signadvisoryspeed35'), (45, 'signadvisoryspeed40')):
    CATALOGUE.append(('signadvisoryspeed%d' % _mph,
                      ('Advisory Speed %d MPH Sign (Plaque)' % _mph,
                       'Señal de Velocidad Recomendada %d MPH (Placa)' % _mph,
                       'Richtgeschwindigkeit %d MPH Schild (Zusatzschild)' % _mph,
                       'Rekommenderad Hastighet %d MPH-Vägmärke (Tilläggsskylt)' % _mph),
                      'square', T('square', ['%d' % _mph, 'MPH']), _after))


# ----------------------------------------------------------------------------- wiring

def write_blockstate(registry, shape, has_back):
    sibling = SHAPES[shape][0]
    src = os.path.join(BS_DIR, sibling + '.json')
    with open(src, 'rb') as fh:
        raw = fh.read()
    text = raw.decode('utf-8')
    d = json.loads(text)
    old_tex = d['defaults']['textures']['1']
    new_tex = 'csm:blocks/trafficsigns/' + registry
    assert old_tex in text, (sibling, old_tex)
    out = text.replace('"%s"' % old_tex, '"%s"' % new_tex)
    if has_back:
        # the yield model reads the back from slot "2"; give this sign its own
        eol = '\r\n' if '\r\n' in text else '\n'
        out = out.replace('"1": "%s"' % new_tex,
                          '"1": "%s",%s      "2": "%s_back"' % (new_tex, eol, new_tex), 1)
        assert '_back' in out, registry
    dst = os.path.join(BS_DIR, registry + '.json')
    with open(dst, 'wb') as fh:
        fh.write(out.encode('utf-8'))
    return dst


def insert_after(path, needle_line_pred, new_line, exists_pred):
    """Insert ``new_line`` after the first line matching the predicate, keeping the file's
    line endings; no-op if a line already satisfies exists_pred."""
    with open(path, 'rb') as fh:
        raw = fh.read()
    eol = b'\r\n' if b'\r\n' in raw else b'\n'
    lines = raw.decode('utf-8').split(eol.decode())
    if any(exists_pred(l) for l in lines):
        return False
    idx = next(i for i, l in enumerate(lines) if needle_line_pred(l))
    lines.insert(idx + 1, new_line)
    with open(path, 'wb') as fh:
        fh.write(eol.decode().join(lines).encode('utf-8'))
    return True


def main():
    apply = '--apply' in sys.argv
    check = '--check' in sys.argv
    drift = []
    lang_lines, tab_lines = [], []
    for registry, names, shape, draw, after in CATALOGUE:
        face = draw()
        png = os.path.join(TEX_DIR, registry + '.png')
        if check:
            if not os.path.exists(png):
                drift.append(png)
            else:
                cur = Image.open(png).convert('RGBA')
                if cur.size != face.size or cur.tobytes() != face.tobytes():
                    drift.append(png)
            continue
        face.save(png)
        has_back = shape == 'silhouette'
        if has_back:
            gray_back(face).save(os.path.join(TEX_DIR, registry + '_back.png'))
        write_blockstate(registry, shape, has_back)
        for code, name in zip(LANGS, names):
            lang_lines.append((code, registry, name))
        tab_lines.append((registry, after))
        print('wrote %s (%s%s)' % (registry, shape, ', +back' if has_back else ''))

    if check:
        if drift:
            print('DRIFT: %d texture(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d gap-sign textures match the generator' % len(CATALOGUE))
        return

    if not apply:
        print('\n# lang (en_us):')
        for code, registry, name in lang_lines:
            if code == 'en_us':
                print('tile.%s.name=%s' % (registry, name))
        print('\n# tab:')
        for registry, after in tab_lines:
            print('    initTabBlock(new BlockTrafficSign("%s"));  // after %s' % (registry, after))
        print('\n(--apply inserts these into the lang files and the tab)')
        return

    # Tab lines: in catalogue order, so a sign that names an earlier catalogue entry as its
    # sibling finds it already inserted.
    for registry, after in tab_lines:
        needle = 'new BlockTrafficSign("%s")' % after
        line = '    initTabBlock(new BlockTrafficSign("%s"));' % registry
        added = insert_after(TAB, lambda l: needle in l, line,
                             lambda l: 'new BlockTrafficSign("%s")' % registry in l)
        print('tab: %s %s' % (registry, 'inserted after ' + after if added else 'already present'))
    for code in LANGS:
        path = os.path.join(LANG_DIR, code + '.lang')
        n = 0
        for lcode, registry, name in lang_lines:
            if lcode != code:
                continue
            # after the sibling's lang line when it has one, else at the end of the sign block
            _r, _n, _s, _d, after = next(e for e in CATALOGUE if e[0] == registry)
            key = 'tile.%s.name=' % registry
            n += insert_after(path, lambda l, a=after: l.startswith('tile.%s.name=' % a),
                              key + name, lambda l, k=key: l.startswith(k))
        print('lang %s: %d added' % (code, n))


if __name__ == '__main__':
    main()
