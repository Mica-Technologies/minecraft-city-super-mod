#!/usr/bin/env python3
"""Measure the legend of a sign's ORIGINAL texture: where each line sits, how tall its capitals
are and which FHWA series it was set in.

A remade face should keep the layout of the sign it replaces -- the hand-made originals often set a
larger capital in a narrower series than a naive remake picks. This reads the first version of the
texture from git (the sign before any remake), finds the legend's ink, splits it into lines by row
projection and, for each line whose text is known, compares its width per cap height with that text
set in every series (shs_signs.legend_width). The nearest series and the measured cap height and
centre are printed as fractions of the plate, ready to go into a catalogue entry.

    python dev-env-utils/scripts/detect_legend_series.py <registry> "LINE ONE|LINE TWO|..."
    python dev-env-utils/scripts/detect_legend_series.py <registry> "..." --rev <commit>

Blurry 128 px originals measure small lines a little wide; treat the series of a line under about
8 px cap as a hint, not a verdict. ``B<`` marks a line set narrower than Series B -- many of the mod's
hand-made textures squashed their lettering -- which a remake sets in B at the measured cap height.
"""

import io
import os
import subprocess
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import gen_official_faces as gof  # noqa: E402
import shs_signs as shs  # noqa: E402


def original_texture(path, rev=None):
    """The texture at ``rev``, or the first version git has of it."""
    rel = os.path.relpath(path, layout.REPO_ROOT).replace('\\', '/')
    if rev is None:
        # --follow crosses the modularization move, so pair each commit with the path it had then
        log = subprocess.check_output(['git', 'log', '--follow', '--name-only', '--format=@%H', '--', rel],
                                      cwd=layout.REPO_ROOT, text=True)
        pairs, current = [], None
        for line in log.splitlines():
            if line.startswith('@'):
                current = line[1:]
            elif line.strip() and current:
                pairs.append((current, line.strip()))
                current = None
        rev, rel = pairs[-1]
    blob = subprocess.check_output(['git', 'show', '%s:%s' % (rev, rel)], cwd=layout.REPO_ROOT)
    return Image.open(io.BytesIO(blob)).convert('RGBA'), rev


def legend_lines(img, aspect, margin=0.08, min_rows=3, diamond=False):
    """Row bands of legend ink, as (top, bottom, left, right) in plate fractions. The panel colour
    is the commonest opaque colour inside the margin; ink is anything far from it. ``diamond`` looks
    only inside a warning diamond's border."""
    h = 512
    w = int(round(h * aspect))
    a = np.asarray(img.resize((w, h), Image.LANCZOS)).astype(np.int32)
    my, mx = int(h * margin), int(w * margin)
    inner = a[my:h - my, mx:w - mx]
    opaque = inner[inner[..., 3] > 200][:, :3]
    q = (opaque // 16) * 16
    vals, counts = np.unique(q, axis=0, return_counts=True)
    bg = vals[counts.argmax()] + 8
    ink = (np.abs(a[..., :3] - bg).sum(axis=2) > 150) & (a[..., 3] > 200)
    ink[:my] = ink[h - my:] = False
    ink[:, :mx] = ink[:, w - mx:] = False
    if diamond:
        # a warning diamond: only inside its border, which otherwise reads as ink on every row
        yy, xx = np.mgrid[0:h, 0:w]
        ink &= (np.abs(xx / w - 0.5) + np.abs(yy / h - 0.5)) < 0.36
    rows = ink.sum(axis=1) > max(2, w * 0.01)
    bands, start = [], None
    for y, on in enumerate(list(rows) + [False]):
        if on and start is None:
            start = y
        elif not on and start is not None:
            cols = np.nonzero(ink[start:y].sum(axis=0))[0]
            # a mounting hole or a speck is not a line: too short or too narrow
            if y - start >= max(min_rows, h * 0.025) and (cols[-1] + 1 - cols[0]) >= w * 0.05:
                bands.append((start / h, y / h, cols[0] / w, (cols[-1] + 1) / w))
            start = None
    return bands


def main():
    args = sys.argv[1:]
    rev = None
    if '--rev' in args:
        k = args.index('--rev')
        rev = args[k + 1]
        del args[k:k + 2]
    registry, texts = args[0], [t for t in args[1].split('|') if t]
    info = gof.sign_info(registry)
    img, rev = original_texture(info['texture'], rev)
    aspect = info['aspect']
    bands = legend_lines(img, aspect)
    print('%s  original %s  plate %.2f  %d ink band(s) for %d line(s)' % (registry, rev[:9], aspect,
                                                                        len(bands), len(texts)))
    for k, (t0, t1, x0, x1) in enumerate(bands):
        text = texts[k] if k < len(texts) else None
        cap = t1 - t0
        width = (x1 - x0) * aspect           # in plate heights, the unit cap is in
        row = '  line %d  centre y %.3f  cap %.3f  width %.3f' % (k + 1, (t0 + t1) / 2, cap, (x1 - x0))
        if text:
            per_cap = width / cap
            fits = {s: shs.legend_width(text, s, 1000) / 1000.0 for s in shs.SERIES}
            best = min(fits, key=lambda s: abs(fits[s] - per_cap))
            if per_cap < fits['B'] * 0.95:
                best = 'B<'                  # set narrower than any alphabet: the original was squashed
            row += '  %-24r series %-4s (measured %.2f; %s)' % (text, best, per_cap,
                                                                ' '.join('%s %.2f' % kv for kv in fits.items()))
        print(row)


if __name__ == '__main__':
    main()
