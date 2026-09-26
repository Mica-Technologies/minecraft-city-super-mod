#!/usr/bin/env python3
"""How full CSM leaves the block atlas, packed the way the game packs it.

Minecraft 1.12 puts every block and item sprite into one texture, the block atlas, whose size is
the next power of two on each side that the stitcher can pack everything into. CSM's sprites are
nearly all of it. Crossing a size is silent and doubles the cost: at 8192 x 8192 instead of
8192 x 4096 the atlas takes twice the GPU memory (~340 MiB with its mip levels instead of ~170)
and the stitch at launch, which reads every pixel, takes twice as long. That happened once
already, on the road sign faces (see "Texture resolution" in TRAFFIC_SIGNS.md and "Memory" in
PERFORMANCE_AND_SECURITY.md); this script is how it is seen coming.

What it counts: every texture a shipped blockstate or model can reach -- blockstate texture
overrides, JSON model texture maps and their parents, OBJ materials -- plus every
``models/item`` model, at its frame size (an animation strip is one frame), padded as the
stitcher pads it for four mip levels. Vanilla, Forge and the dev client's other mods add a fixed
820-odd small sprites (measured in a dev client). The pack is a line-for-line port of Forge's
``Stitcher``, so "fits" is the game's answer, not an estimate of slack.

    python dev-env-utils/scripts/atlas_budget.py            # report; exit 1 if it has doubled
    python dev-env-utils/scripts/atlas_budget.py --list N   # also the N largest sprites

The budget is ``BUDGET`` (95%) of 8192 x 4096 in padded pixels. Above it the script warns;
once the atlas no longer fits 8192 x 4096 it exits 1. With the road sign faces at their plate
size (2026-09) CSM sits right at the budget, about 1.6 Mpx -- some hundred more 128 px
textures -- short of doubling. The fix is to cut sprite pixels (a texture larger than its plate or face needs, identical
copies that could be one sprite), not to raise the budget. OptiFine's ``_e`` emissive companions
join the atlas only under OptiFine and are reported separately.
"""

import glob
import json
import os
import re
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

sys.setrecursionlimit(100000)

MIP_LEVELS = 4
MAX_SIZE = 16384
TARGET = (8192, 4096)
BUDGET = 0.95
# The dev client's own sprites besides CSM's (vanilla, Forge, The One Probe), from a stage 3a
# heap dump of the block atlas: (width, height, count).
OTHER_SPRITES = ((16, 16, 821), (32, 32, 2), (128, 128, 1))


# ----------------------------------------------------------------------------- the stitcher port

def mipdim(v, lvl=MIP_LEVELS):
    return ((v >> lvl) + (0 if (v & ((1 << lvl) - 1)) == 0 else 1)) << lvl


def pow2(v):
    # MathHelper.smallestEncompassingPowerOfTwo
    i = v - 1
    i |= i >> 1
    i |= i >> 2
    i |= i >> 4
    i |= i >> 8
    i |= i >> 16
    return i + 1


class _Holder(object):
    __slots__ = ('name', 'w0', 'h0', 'rot')

    def __init__(self, name, w, h):
        self.name, self.w0, self.h0 = name, w, h
        self.rot = mipdim(h) > mipdim(w)

    @property
    def w(self):
        return mipdim(self.h0 if self.rot else self.w0)

    @property
    def h(self):
        return mipdim(self.w0 if self.rot else self.h0)

    def key(self):
        # Stitcher.Holder.compareTo: taller first, then wider first, then by name
        return (-self.h, -self.w, self.name)


class _Slot(object):
    __slots__ = ('x', 'y', 'w', 'h', 'sub', 'holder')

    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h
        self.sub = None
        self.holder = None

    def add(self, hd):
        if self.holder is not None:
            return False
        i, j = hd.w, hd.h
        if i > self.w or j > self.h:
            return False
        if i == self.w and j == self.h:
            self.holder = hd
            return True
        if self.sub is None:
            self.sub = [_Slot(self.x, self.y, i, j)]
            k, l = self.w - i, self.h - j
            if l > 0 and k > 0:
                if max(self.h, k) >= max(self.w, l):
                    self.sub.append(_Slot(self.x, self.y + j, i, l))
                    self.sub.append(_Slot(self.x + i, self.y, k, self.h))
                else:
                    self.sub.append(_Slot(self.x + i, self.y, k, j))
                    self.sub.append(_Slot(self.x, self.y + j, self.w, l))
            elif k == 0:
                self.sub.append(_Slot(self.x, self.y + j, i, l))
            elif l == 0:
                self.sub.append(_Slot(self.x + i, self.y, k, j))
        return any(s.add(hd) for s in self.sub)


def stitch(sprites):
    """(width, height) of the atlas the game would make for [(name, w, h)], as Forge's
    Stitcher.doStitch packs them; raises OverflowError past MAX_SIZE."""
    holders = sorted((_Holder(n, w, h) for n, w, h in sprites), key=_Holder.key)
    slots, cw, ch = [], 0, 0
    for hd in holders:
        placed = False
        square = hd.w0 == hd.h0
        for s in slots:
            if s.add(hd):
                placed = True
                break
            if not square:
                hd.rot = not hd.rot
                if s.add(hd):
                    placed = True
                    break
                hd.rot = not hd.rot
        if placed:
            continue
        i = min(hd.w, hd.h)
        k, l = pow2(cw), pow2(ch)
        i1, j1 = pow2(cw + i), pow2(ch + i)
        f1, f2 = i1 <= MAX_SIZE, j1 <= MAX_SIZE
        if not f1 and not f2:
            raise OverflowError('unable to fit ' + hd.name)
        f3, f4 = f1 and k != i1, f2 and l != j1
        flag = ((not f3) and f1) if f3 ^ f4 else (f1 and k <= l)
        if flag:
            if hd.w > hd.h:
                hd.rot = not hd.rot
            if ch == 0:
                ch = hd.h
            s = _Slot(cw, 0, hd.w, ch)
            cw += hd.w
        else:
            s = _Slot(0, ch, cw, hd.h)
            ch += hd.h
        s.add(hd)
        slots.append(s)
    return pow2(cw), pow2(ch)


def padded(sprites):
    return sum(mipdim(w) * mipdim(h) for _n, w, h in sprites)


# ----------------------------------------------------------------------------- CSM's sprites

_json = {}


def _load(path):
    if path not in _json:
        with open(path, encoding='utf-8') as fh:
            _json[path] = json.load(fh)
    return _json[path]


def _model_file(ref):
    ns, _, p = ref.partition(':') if ':' in ref else ('minecraft', None, ref)
    if ns != 'csm':
        return None
    cands = ['models/' + p] if p.endswith('.obj') else ['models/' + p + '.json']
    if not p.startswith('block/') and not p.startswith('item/'):
        cands.insert(0, 'models/block/' + p + ('' if p.endswith('.obj') else '.json'))
    for c in cands:
        f = layout.resolve_asset(c)
        if f:
            return f
    return None


def _strings(node):
    if isinstance(node, dict):
        for v in node.values():
            yield from _strings(v)
    elif isinstance(node, list):
        for v in node:
            yield from _strings(v)
    elif isinstance(node, str):
        yield node


def _textures_of_model(path, out, seen):
    if path in seen:
        return
    seen.add(path)
    if path.endswith('.obj'):
        with open(path, encoding='utf-8') as fh:
            for line in fh:
                if line.startswith('mtllib'):
                    mtl = os.path.join(os.path.dirname(path), line.split()[1])
                    if os.path.isfile(mtl):
                        with open(mtl, encoding='utf-8') as mf:
                            for ml in mf:
                                if ml.startswith('map_Kd'):
                                    out.add(ml.split()[1])
        return
    m = _load(path)
    for v in (m.get('textures') or {}).values():
        if isinstance(v, str) and not v.startswith('#'):
            out.add(v)
    parent = m.get('parent')
    if parent and ':' in parent:
        pf = _model_file(parent)
        if pf:
            _textures_of_model(pf, out, seen)


def _blockstate_refs(bs):
    """Model refs and texture values anywhere in a blockstate."""
    models, textures = set(), set()

    def walk(node, forge):
        if isinstance(node, dict):
            for k, v in node.items():
                if k == 'model' and isinstance(v, str):
                    if forge:
                        models.add(v if ':' in v else 'minecraft:' + v)
                    else:
                        models.add('csm:block/' + v.split(':', 1)[-1] if ':' in v else 'minecraft:' + v)
                elif k == 'textures' and isinstance(v, dict):
                    textures.update(t for t in v.values() if isinstance(t, str) and not t.startswith('#'))
                else:
                    walk(v, forge)
        elif isinstance(node, list):
            for v in node:
                walk(v, forge)
    walk(bs, bs.get('forge_marker') == 1)
    return models, textures


def csm_sprites(include_emissive=False):
    """[(name, w, h)] for every csm: sprite the block atlas holds."""
    refs, seen = set(), set()
    for _module, root in layout.asset_roots():
        for bf in glob.glob(os.path.join(root, 'blockstates', '*.json')):
            models, textures = _blockstate_refs(_load(bf))
            refs |= textures
            for m in models:
                mf = _model_file(m)
                if mf:
                    _textures_of_model(mf, refs, seen)
        for mf in glob.glob(os.path.join(root, 'models', 'item', '*.json')):
            _textures_of_model(mf, refs, seen)
    out = {}
    for ref in refs:
        if ':' not in ref:
            continue          # minecraft: textures are vanilla's, already counted
        ns, path = ref.split(':', 1)
        if ns != 'csm':
            continue
        f = layout.resolve_asset('textures/' + path + '.png')
        if not f:
            continue
        with Image.open(f) as im:
            w, h = im.size
        if h != w and os.path.exists(f + '.mcmeta'):
            h = w             # an animation strip: the sprite is one frame
        out[ref] = (w, h)
        if include_emissive:
            e = f[:-4] + '_e.png'
            if os.path.exists(e):
                with Image.open(e) as im:
                    ew, eh = im.size
                out[ref + '_e'] = (ew, ew if eh != ew and os.path.exists(e + '.mcmeta') else eh)
    return sorted((n, w, h) for n, (w, h) in out.items())


def other_sprites():
    return [('other:%d_%d_%d' % (w, h, i), w, h) for w, h, n in OTHER_SPRITES for i in range(n)]


def main(argv):
    top = int(argv[argv.index('--list') + 1]) if '--list' in argv else 0
    csm = csm_sprites()
    allsp = csm + other_sprites()
    total = padded(allsp)
    cap = TARGET[0] * TARGET[1]
    try:
        atlas = stitch(allsp)
    except OverflowError as e:
        atlas = ('overflow: %s' % e,)
    folders = {}
    for n, w, h in csm:
        key = re.sub(r'^csm:(blocks|items)/([^/]+)/.*$', r'\1/\2', n)
        folders[key] = folders.get(key, 0) + mipdim(w) * mipdim(h)
    print('CSM sprites        %d, %.2f Mpx padded' % (len(csm), padded(csm) / 1e6))
    print('with the rest      %d, %.2f Mpx' % (len(allsp), total / 1e6))
    print('block atlas        %s' % ('x'.join(str(v) for v in atlas)))
    print('fill of %dx%d  %.1f%%  (budget %d%%, %.2f Mpx left before it)' % (
        TARGET[0], TARGET[1], total * 100.0 / cap, BUDGET * 100, (BUDGET * cap - total) / 1e6))
    emissive = csm_sprites(include_emissive=True)
    with_e = padded(emissive + other_sprites())
    try:
        atlas_e = 'x'.join(str(v) for v in stitch(emissive + other_sprites()))
    except OverflowError:
        atlas_e = 'overflow'
    print('under OptiFine     %.2f Mpx with the _e companions, atlas %s' % (with_e / 1e6, atlas_e))
    print('largest folders:')
    for k, v in sorted(folders.items(), key=lambda kv: -kv[1])[:8]:
        print('  %-40s %6.2f Mpx' % (k, v / 1e6))
    if top:
        print('largest sprites:')
        for n, w, h in sorted(csm, key=lambda s: -mipdim(s[1]) * mipdim(s[2]))[:top]:
            print('  %4d x %-4d %s' % (w, h, n))
    grown = len(atlas) != 2 or atlas[0] * atlas[1] > cap
    if grown:
        print('WARNING: the block atlas is larger than %dx%d' % TARGET)
    elif total > BUDGET * cap:
        print('WARNING: the block atlas is over %d%% of %dx%d; a few more textures will double it'
              % (BUDGET * 100, TARGET[0], TARGET[1]))
    return 1 if grown else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
