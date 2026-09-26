#!/usr/bin/env python3
"""How large a road sign's face texture should be, and the one filter that makes it that size.

The rule. A sign texture is square (or a strip of square animation frames) and is stretched
onto its plate, so what decides how sharp a face looks is its density: texels per block of
plate. Every road sign face is the smallest power of two that gives its plate at least
``DENSITY`` = 85.3 texels per block -- 128 px on a plate up to 1.5 blocks, 256 up to 3, 512 up
to 6 -- never larger than the size it is drawn at, and never below ``FLOOR`` = 128. A texture
drawn by several plates takes the largest. Frames of an animation strip count one frame at a
time, and an OptiFine ``_e`` companion takes its base texture's size.

Why that density: a 1080p screen shows a block at ~771/d px at d blocks, and the game samples
a mip level, so extra texels only show while the plate covers more screen pixels per block than
the texture has texels per block. At 85.3 the face matches a 256 px one-block face from ~9
blocks out, and the block atlas stays at 8192 x 4096 instead of 8192 x 8192 (see "Memory" in
PERFORMANCE_AND_SECURITY.md and "Texture resolution" in TRAFFIC_SIGNS.md).

The plate is measured, not declared: every model any blockstate draws is walked, JSON element
faces through their UV window (and element rescale), OBJ polygons through their UV Jacobian,
and for each face that paints the texture the number of blocks one full texture width or height
covers (the *span*) is recorded. ``size = pow2ceil(DENSITY * span)``, with 2% tolerance so a
24-unit plate counts as 1.5 blocks.

The filter (``reduce``): area average in linear light (gamma 2.2, premultiplied alpha -- on an
opaque area exactly the game's own mip level of the larger texture), then a light unsharp mask
(radius 0.6 px, 60%) on the fully opaque pixels only; at a cutout edge the colour under a soft
alpha is not picture, and sharpening it pulls a dark rim into the edge. Deterministic, so every
generator's ``--check`` stays a byte comparison.

Generators call :func:`fit` on every sign texture before they save or compare it::

    import sign_texture_size as sts
    img = sts.fit(img, path_or_name)

    python sign_texture_size.py <texture name or path> ...   # print span and size
    python sign_texture_size.py --all                         # every sign texture over its size
"""

import collections
import glob
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

DENSITY = 256.0 / 3.0       # texels per block: 128 px to 1.5 blocks, 256 to 3, 512 to 6
FLOOR = 128                 # never below this, whatever the plate
TOLERANCE = 1.02            # a plate within 2% of a threshold counts as on it
PREFIX = 'csm:blocks/trafficsigns/'
FOLDER = 'textures/blocks/trafficsigns'
# Not sign faces: the bare-metal back every plate wears, sampled in UV slivers, and a texture
# only ever named as a particle / "all" slot.
EXEMPT = frozenset({PREFIX + 'absolutely_nothing_sign', PREFIX + 'pole_dead_end_large'})

UNSHARP_RADIUS = 0.6
UNSHARP_PERCENT = 60
GAMMA = 2.2


# ----------------------------------------------------------------------------- names

def name_of(path_or_name):
    """``csm:blocks/trafficsigns/foo`` for a texture path, a bare name or a full name."""
    s = str(path_or_name).replace('\\', '/')
    if s.startswith('csm:'):
        return s
    if s.endswith('.png'):
        s = s[:-4]
    if '/textures/' in s:
        return 'csm:' + s.split('/textures/', 1)[1]
    if '/' not in s:
        return PREFIX + s
    return 'csm:' + s


def is_sign_texture(name):
    return name.startswith(PREFIX) and name not in EXEMPT


# ----------------------------------------------------------------------------- plate measurement

def _find(rel):
    return layout.resolve_asset(rel)


_json_cache = {}


def _load_json(path):
    if path not in _json_cache:
        with open(path, encoding='utf-8') as fh:
            _json_cache[path] = json.load(fh)
    return _json_cache[path]


def _model_path(ref):
    ns, _, p = ref.partition(':') if ':' in ref else ('minecraft', None, ref)
    if ns != 'csm':
        return None
    if p.endswith('.obj'):
        for cand in ('models/block/' + p, 'models/' + p):
            f = _find(cand)
            if f:
                return f
        return None
    for cand in ('models/block/' + p + '.json', 'models/' + p + '.json'):
        f = _find(cand)
        if f:
            return f
    return None


def _resolve_json_model(path, depth=0):
    """(elements, textures) with the parent chain applied."""
    m = _load_json(path)
    tex, elements = {}, None
    par = m.get('parent')
    if par and depth < 20 and ':' in par:
        pp = _model_path(par)
        if pp and pp.endswith('.json'):
            elements, pt = _resolve_json_model(pp, depth + 1)
            tex.update(pt)
    tex.update(m.get('textures', {}))
    if 'elements' in m:
        elements = m['elements']
    return elements or [], tex


def _resolve_tex(var, maps):
    """Follow ``#refs`` through the texture maps (blockstate first, then the model's)."""
    v = var
    for _ in range(10):
        if not v.startswith('#'):
            break
        k = v[1:]
        nv = next((mp[k] for mp in maps if k in mp), None)
        if nv is None:
            return None
        v = nv
    if v.startswith('#'):
        return None
    return v if ':' in v else 'minecraft:' + v


_AXES = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


def _json_faces(path, bs_tex):
    elements, mtex = _resolve_json_model(path)
    maps = [bs_tex, mtex]
    for el in elements:
        f, t = el['from'], el['to']
        d = [abs(t[i] - f[i]) for i in range(3)]
        rot = el.get('rotation')
        if rot and rot.get('rescale') and rot.get('angle'):
            s = 1.0 / math.cos(math.radians(abs(rot['angle'])))
            ax = 'xyz'.index(rot['axis'])
            d = [d[i] * (s if i != ax else 1.0) for i in range(3)]
        for side, face in el.get('faces', {}).items():
            var = face.get('texture')
            tx = _resolve_tex(var, maps) if var else None
            if not tx:
                continue
            a, b = _AXES[side]
            w, h = d[a], d[b]
            uv = face.get('uv')
            if uv:
                du, dv = abs(uv[2] - uv[0]), abs(uv[3] - uv[1])
            else:
                du, dv = min(w, 16), min(h, 16)    # the automatic UV: the face's own window
            if face.get('rotation', 0) in (90, 270):
                du, dv = dv, du
            if du <= 0 or dv <= 0 or w <= 0 or h <= 0:
                continue
            yield tx, (w / du, h / dv)             # blocks per texture width, per height


def _obj_faces(path, bs_tex):
    mtl_tex, verts, uvs, faces, cur = {}, [], [], [], None
    base = os.path.dirname(path)
    with open(path, encoding='utf-8') as fh:
        lines = fh.read().splitlines()
    for ln in lines:
        s = ln.split()
        if not s:
            continue
        if s[0] == 'mtllib':
            mp = os.path.join(base, s[1])
            if os.path.isfile(mp):
                name = None
                with open(mp, encoding='utf-8') as mf:
                    for ml in mf.read().splitlines():
                        ms = ml.split()
                        if ms and ms[0] == 'newmtl':
                            name = ms[1]
                        elif ms and ms[0] == 'map_Kd' and name:
                            mtl_tex[name] = ms[1]
        elif s[0] == 'v':
            verts.append(tuple(float(x) for x in s[1:4]))
        elif s[0] == 'vt':
            uvs.append(tuple(float(x) for x in s[1:3]))
        elif s[0] == 'usemtl':
            cur = s[1]
        elif s[0] == 'f':
            idx = []
            for tok in s[1:]:
                parts = tok.split('/')
                ti = int(parts[1]) - 1 if len(parts) > 1 and parts[1] else None
                idx.append((int(parts[0]) - 1, ti))
            faces.append((cur, idx))
    for mat, idx in faces:
        if mat is None or len(idx) < 3 or any(t is None for _, t in idx):
            continue
        tx = bs_tex.get('#' + mat) or bs_tex.get(mat) or mtl_tex.get(mat)
        if tx and tx.startswith('#'):
            tx = _resolve_tex(tx, [bs_tex])
        if not tx:
            continue
        p = [verts[v] for v, _ in idx[:3]]
        t = [uvs[i] for _, i in idx[:3]]
        e1 = [p[1][i] - p[0][i] for i in range(3)]
        e2 = [p[2][i] - p[0][i] for i in range(3)]
        du1, dv1 = t[1][0] - t[0][0], t[1][1] - t[0][1]
        du2, dv2 = t[2][0] - t[0][0], t[2][1] - t[0][1]
        det = du1 * dv2 - du2 * dv1
        if abs(det) < 1e-9:
            continue
        dpdu = [(e1[i] * dv2 - e2[i] * dv1) / det for i in range(3)]
        dpdv = [(e2[i] * du1 - e1[i] * du2) / det for i in range(3)]
        yield (tx if ':' in tx else 'minecraft:' + tx), (
            math.sqrt(sum(x * x for x in dpdu)), math.sqrt(sum(x * x for x in dpdv)))


def _variants(bs):
    """(model ref, blockstate textures) for every model a blockstate draws in the world."""
    if bs.get('forge_marker') == 1:
        d = bs.get('defaults', {})
        dmodel, dtex, dsub = d.get('model'), dict(d.get('textures', {})), d.get('submodel')

        def emit(v):
            model = v.get('model', dmodel)
            tex = dict(dtex)
            tex.update(v.get('textures', {}))
            if model:
                yield model, tex
            for sub in (dsub, v.get('submodel')):
                if isinstance(sub, str):
                    yield sub, tex
                elif isinstance(sub, dict):
                    for sv in sub.values():
                        if isinstance(sv, dict) and sv.get('model'):
                            st = dict(tex)
                            st.update(sv.get('textures', {}))
                            yield sv['model'], st
        yield from emit({})
        for key, val in bs.get('variants', {}).items():
            if key == 'inventory':
                continue
            if isinstance(val, list):
                for v in val:
                    yield from emit(v)
            elif isinstance(val, dict):
                if 'model' in val or 'textures' in val:
                    yield from emit(val)
                else:
                    for v in val.values():
                        if isinstance(v, dict):
                            yield from emit(v)
    else:
        for key, val in bs.get('variants', {}).items():
            if key == 'inventory':
                continue
            for v in (val if isinstance(val, list) else [val]):
                if v.get('model'):
                    m = v['model']
                    yield ('csm:' + m.split(':', 1)[-1] if ':' in m else m), {}
        for part in bs.get('multipart', []):
            ap = part.get('apply', [])
            for v in (ap if isinstance(ap, list) else [ap]):
                if v.get('model'):
                    m = v['model']
                    yield (m if ':' in m else 'minecraft:' + m), {}


_SPANS = None


def spans(refresh=False):
    """{texture name: largest span in blocks} over every plate in every module that draws a
    road sign texture. Only blockstates that mention ``trafficsigns`` are walked (the rest
    cannot reach a sign texture: sign models, sign textures and sign blockstates all carry
    the folder name); ``--verify-index`` checks that against a walk of everything."""
    global _SPANS
    if _SPANS is None or refresh:
        _SPANS = _build_spans(only_signs=True)
    return _SPANS


def _build_spans(only_signs):
    out = collections.defaultdict(float)
    seen = set()
    for _module, root in layout.asset_roots():
        for bf in sorted(glob.glob(os.path.join(root, 'blockstates', '*.json'))):
            if only_signs:
                with open(bf, encoding='utf-8') as fh:
                    if 'trafficsigns' not in fh.read():
                        continue
            bs = _load_json(bf)
            for model, tex in _variants(bs):
                key = (model, json.dumps(tex, sort_keys=True))
                if key in seen:
                    continue
                seen.add(key)
                mp = _model_path(model)
                if not mp:
                    continue
                faces = _obj_faces(mp, tex) if mp.endswith('.obj') else _json_faces(mp, tex)
                for tx, (bu, bv) in faces:
                    if tx.startswith(PREFIX):
                        out[tx] = max(out[tx], bu, bv)
    return dict(out)


def span(name):
    """Blocks one full texture width or height covers on the largest plate that draws it;
    an ``_e`` companion answers for its base. None if no model in the tree draws it."""
    name = name_of(name)
    s = spans()
    if name in s:
        return s[name]
    if name.endswith('_e') and name[:-2] in s:
        return s[name[:-2]]
    return None


# ----------------------------------------------------------------------------- the size

def rule_size(plate_span):
    """The smallest power of two that gives ``plate_span`` blocks ``DENSITY`` texels a block."""
    need = DENSITY * plate_span / TOLERANCE
    return 1 << max(0, int(math.ceil(math.log2(need))))


def target_size(name, current):
    """The side a sign texture of ``current`` px (one frame) should be stored at."""
    name = name_of(name)
    if not is_sign_texture(name) or current <= FLOOR:
        return current
    sp = span(name)
    if sp is None:
        return current
    return max(FLOOR, min(current, rule_size(sp)))


# ----------------------------------------------------------------------------- the filter

def frames(img):
    """Square frames of an animation strip (a still texture is one frame)."""
    w, h = img.size
    n = max(1, h // w)
    return [img.crop((0, i * w, w, (i + 1) * w)) for i in range(n)]


def _join(frs):
    w = frs[0].width
    out = Image.new('RGBA', (w, w * len(frs)))
    for i, f in enumerate(frs):
        out.paste(f, (0, i * w))
    return out


def _gamma_box(img, size):
    a = np.asarray(img.convert('RGBA')).astype(np.float64)
    f = a.shape[0] // size
    if f * size != a.shape[0] or a.shape[0] != a.shape[1]:
        raise ValueError('%s does not divide into %d' % (img.size, size))
    lin = (a[..., :3] / 255.0) ** GAMMA
    al = a[..., 3:4] / 255.0
    pm = np.concatenate([lin * al, al], axis=2)
    q = pm.reshape(size, f, size, f, 4).mean(axis=(1, 3))
    alpha = q[..., 3:4]
    rgb = np.where(alpha > 0, q[..., :3] / np.maximum(alpha, 1e-9), 0.0)
    out = np.concatenate([np.power(rgb, 1 / GAMMA) * 255.0, alpha * 255.0], axis=2)
    return Image.fromarray(np.round(out).clip(0, 255).astype(np.uint8), 'RGBA')


def _reduce_frame(img, size):
    b = _gamma_box(img, size)
    rgb = b.convert('RGB').filter(ImageFilter.UnsharpMask(
        radius=UNSHARP_RADIUS, percent=UNSHARP_PERCENT, threshold=0))
    a = np.asarray(b)
    out = np.asarray(rgb).copy()
    edge = a[..., 3] < 255
    out[edge] = a[..., :3][edge]
    return Image.fromarray(np.dstack([out, a[..., 3]]), 'RGBA')


def reduce(img, size):
    """``img`` (a square or a strip of square frames) at ``size`` px a frame; unchanged if it
    already is."""
    img = img.convert('RGBA')
    if img.width == size:
        return img
    if img.width < size:
        raise ValueError('reduce() never enlarges (%d -> %d)' % (img.width, size))
    return _join([_reduce_frame(f, size) for f in frames(img)])


def fit(img, path_or_name):
    """``img`` at the size the rule gives the texture it will be saved as."""
    img = img.convert('RGBA')
    return reduce(img, target_size(path_or_name, img.width))


# ----------------------------------------------------------------------------- the tree

def sign_texture_files():
    """{texture name: path} for every shipped road sign texture (any module)."""
    out = {}
    for d in layout.asset_dirs(FOLDER):
        for p in sorted(glob.glob(os.path.join(d, '*.png'))):
            out[name_of(p)] = p
    return out


def oversized():
    """[(name, path, current, target)] for every sign texture stored above its size."""
    bad = []
    for name, path in sorted(sign_texture_files().items()):
        with Image.open(path) as im:
            w = im.size[0]
        t = target_size(name, w)
        if t < w:
            bad.append((name, path, w, t))
    return bad


def _verify_index():
    fast, full = spans(), _build_spans(only_signs=False)
    diff = sorted(set(fast) ^ set(full)) + sorted(
        k for k in set(fast) & set(full) if abs(fast[k] - full[k]) > 1e-9)
    for k in diff:
        print('index differs:', k, fast.get(k), full.get(k))
    print('%d sign textures indexed; %d differ from the full walk' % (len(fast), len(diff)))
    return 1 if diff else 0


def main(argv):
    if not argv:
        print(__doc__)
        return 0
    if argv[0] == '--verify-index':
        return _verify_index()
    if argv[0] == '--all':
        bad = oversized()
        for name, _p, w, t in bad:
            print('%-70s %4d -> %4d  (span %.2f)' % (name, w, t, span(name)))
        print('%d sign texture(s) above their plate size' % len(bad))
        return 0
    for a in argv:
        n = name_of(a)
        sp = span(n)
        print('%s  span %s  rule %s' % (n, 'none' if sp is None else '%.3f' % sp,
                                        '-' if sp is None else rule_size(sp)))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
