#!/usr/bin/env python3
"""Replace existing road signs' faces with the official FHWA Standard Highway Signs drawings.

The catalogue below maps a registry name to the SHS page (or interim ZIP) its design is
drawn on. For each, the script reads the sign's own blockstate to find the plate model and
the texture it paints (slot ``1``; the file is often not named after the registry), measures
the plate's aspect off the model's elements, renders the drawing through
``shs_signs.official_face`` and writes the texture -- nothing else. A silhouette sign whose
blockstate names a ``_back`` texture on slot ``2`` gets that regenerated too. Registration,
lang and blockstates are never touched: this is a texture swap for signs that already exist,
which is what makes ``--check`` a byte comparison and a batch reversible with
``git checkout <commit> -- <texture>``.

    python dev-env-utils/scripts/gen_official_faces.py [--only a,b] [--sheet out.png] [--check]
    python dev-env-utils/scripts/gen_official_faces.py --verify-sheet <prefix>

``--sheet`` writes a before/after contact sheet at plate aspect (current texture left, new
face right) without touching the tree -- the batch review the user sees before anything is
written. ``--verify-sheet`` renders every ``?`` row of
``assets/docs/agent_progress/SHS_MATCH_TABLE.md`` as the current texture beside a thumbnail
of the cited book page, eight per sheet, so the token-matched guesses can be confirmed or
rejected by eye before they are catalogued.

Numbered signs (Speed Limit, CURVE xx MPH, ...) use ``replace=('50', '35')``: the book draws
one numeral, which is dropped and the mod's set in its place at the same cap height.
"""

import json
import os
import re
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import shs_signs as shs  # noqa: E402

MATCH_TABLE = os.path.join(layout.REPO_ROOT, 'assets', 'docs', 'agent_progress', 'SHS_MATCH_TABLE.md')


# ----------------------------------------------------------------------------- sources

def SHS(chapter, page, pick=0, mirror=False, palette=None, replace=None, inner=None):
    """A face from a 2004 SHS book page (0-based). ``pick`` for pages with more than one
    sign, ``mirror`` for the left-hand version of a symbol the book draws right-handed only,
    ``replace=(old, new)`` to re-set the one numeral the book draws."""
    return lambda: (shs.book_sign(chapter, page, pick, inner=inner, replace=replace), mirror, palette)


def SHSI(code, variant=None, palette=None):
    """A face from an interim SHS ZIP (a sign added or redrawn since the book)."""
    return lambda: (shs.interim_sign(code, variant), False, palette)


# ----------------------------------------------------------------------------- catalogue
# registry, source, SHS code (for the sheet caption)

CATALOGUE = [
]


# ----------------------------------------------------------------------------- the sign's own plate

def _asset_json(relative_path):
    path = layout.resolve_asset(relative_path)
    if path is None:
        raise SystemExit('missing asset %s' % relative_path)
    with open(path, encoding='utf-8') as fh:
        return json.load(fh)


def plate_aspect(model_ref):
    """Width / height of the plate face the sign texture (slot ``1``) is painted on, read off
    the model's elements: the largest vertical face textured ``#1``."""
    name = model_ref.split(':', 1)[1]
    model = _asset_json('models/block/%s.json' % name)
    best = None
    for el in model.get('elements', []):
        f, t = el['from'], el['to']
        dims = [abs(t[i] - f[i]) for i in range(3)]
        for side, face in el.get('faces', {}).items():
            if face.get('texture') != '#1' or side in ('up', 'down'):
                continue
            w = dims[0] if side in ('north', 'south') else dims[2]
            h = dims[1]
            if best is None or w * h > best[0] * best[1]:
                best = (w, h)
    if best is None:
        raise SystemExit('%s has no face textured #1' % model_ref)
    return best[0] / float(best[1])


def sign_info(registry):
    """Where the sign's face texture lives and the plate it is stretched onto."""
    bs_path = layout.blockstate_file(registry)
    if bs_path is None:
        raise SystemExit('no blockstate for %s' % registry)
    with open(bs_path, encoding='utf-8') as fh:
        bs = json.load(fh)
    defaults = bs['defaults']
    textures = defaults['textures']
    face_ref = textures['1']
    back_ref = textures.get('2')

    def tex_path(ref):
        rel = 'textures/' + ref.split(':', 1)[1] + '.png'
        path = layout.resolve_asset(rel)
        return path or layout.asset_for_write(layout.owner_of(registry), rel)

    return {
        'model': defaults['model'],
        'aspect': plate_aspect(defaults['model']),
        'texture': tex_path(face_ref),
        'back': tex_path(back_ref) if back_ref and back_ref.endswith('_back') else None,
    }


def render(source, aspect):
    face, mirror, palette = source()
    return shs.official_face(face, aspect, mirror, palette)


# ----------------------------------------------------------------------------- sheets

def _at_aspect(img, aspect, h=150, max_w=200):
    """The square texture stretched back to its plate, fitted in ``max_w`` x ``h``."""
    w = int(round(h * aspect))
    if w > max_w:
        w, h = max_w, int(round(max_w / aspect))
    return img.convert('RGBA').resize((w, h), Image.LANCZOS)


def contact_sheet(entries, out, cols=3):
    """Before / after per entry: the current texture and the new face, both stretched to
    the plate, captioned with the registry and the SHS code."""
    cell_w, cell_h = 420, 200
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell_w, rows * cell_h), (60, 60, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, (registry, source, code) in enumerate(entries):
        info = sign_info(registry)
        x0, y0 = (i % cols) * cell_w, (i // cols) * cell_h
        before = _at_aspect(Image.open(info['texture']), info['aspect']) \
            if os.path.exists(info['texture']) else None
        after = _at_aspect(render(source, info['aspect']), info['aspect'])
        if before is not None:
            sheet.alpha_composite(before, (x0 + 10 + (200 - before.width) // 2, y0 + 10))
        sheet.alpha_composite(after, (x0 + 210 + (200 - after.width) // 2, y0 + 10))
        d.text((x0 + 10, y0 + 170), '%s  <-  %s' % (registry, code), fill=(235, 235, 235, 255))
        d.text((x0 + 10, y0 + 184), os.path.basename(info['texture']), fill=(170, 170, 170, 255))
    sheet.save(out)
    return sheet.size


_ROW_RE = re.compile(r'^\| (\S+) \| (.*?) \| (EXACT|NEAR|NONE)\? \| (\w+) p(\d+)\s*([^|]*)\| (.*?) \|$')


def verify_sheets(prefix, per_sheet=8):
    """Every ``?`` row of the match table that cites a page: current texture beside the page
    thumbnail, so a human can confirm or reject the guess."""
    rows = []
    with open(MATCH_TABLE, encoding='utf-8') as fh:
        for line in fh:
            m = _ROW_RE.match(line.rstrip('\n'))
            if m:
                rows.append(m.groups())
    print('%d ? rows with a cited page' % len(rows))
    cell_w, cell_h = 640, 330
    for s in range(0, len(rows), per_sheet):
        chunk = rows[s:s + per_sheet]
        sheet = Image.new('RGBA', (2 * cell_w, ((len(chunk) + 1) // 2) * cell_h), (60, 60, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, (registry, display, cat, chapter, page, code, note) in enumerate(chunk):
            x0, y0 = (i % 2) * cell_w, (i // 2) * cell_h
            try:
                info = sign_info(registry)
                cur = _at_aspect(Image.open(info['texture']), info['aspect'], h=200)
                sheet.alpha_composite(cur, (x0 + 10 + (230 - cur.width) // 2, y0 + 10))
            except SystemExit as e:
                d.text((x0 + 10, y0 + 100), str(e), fill=(255, 120, 120, 255))
            pix = shs.book_page(chapter, int(page)).get_pixmap(dpi=28)
            thumb = Image.frombytes('RGB', (pix.width, pix.height), pix.samples).convert('RGBA')
            sheet.alpha_composite(thumb, (x0 + 250, y0 + 10))
            d.text((x0 + 10, y0 + 230), '%s (%s?)' % (registry, cat), fill=(235, 235, 235, 255))
            d.text((x0 + 10, y0 + 246), display[:60], fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 262), '%s p%s %s' % (chapter, page, code.strip()), fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 278), note[:90], fill=(160, 160, 160, 255))
        out = '%s_%02d.png' % (prefix, s // per_sheet)
        sheet.save(out)
        print('wrote', out)


# ----------------------------------------------------------------------------- main

def main():
    args = sys.argv[1:]

    def opt(name):
        if name in args:
            i = args.index(name)
            return args[i + 1]
        return None

    if '--verify-sheet' in args:
        verify_sheets(opt('--verify-sheet'))
        return

    only = opt('--only')
    entries = CATALOGUE if not only else [e for e in CATALOGUE if e[0] in only.split(',')]
    if not entries:
        raise SystemExit('nothing selected')

    sheet = opt('--sheet')
    if sheet:
        print('sheet %s %s' % (sheet, contact_sheet(entries, sheet)))
        return

    check = '--check' in args
    drift = []
    for registry, source, code in entries:
        info = sign_info(registry)
        face = render(source, info['aspect'])
        targets = [(info['texture'], face)]
        if info['back']:
            targets.append((info['back'], shs.back_texture(face)))
        for path, img in targets:
            if check:
                cur = Image.open(path).convert('RGBA') if os.path.exists(path) else None
                if cur is None or cur.size != img.size or cur.tobytes() != img.tobytes():
                    drift.append(path)
            else:
                img.save(path)
        if not check:
            print('wrote %s <- %s (%s, plate %.2f)' % (
                os.path.basename(info['texture']), code, registry, info['aspect']))
    if check:
        if drift:
            print('DRIFT: %d texture(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d official faces match the generator' % len(entries))


if __name__ == '__main__':
    main()
