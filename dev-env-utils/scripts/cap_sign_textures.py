#!/usr/bin/env python3
"""Hold every road sign face texture to the size its plate is given (sign_texture_size).

    python dev-env-utils/scripts/cap_sign_textures.py --check    # exit 1 on any texture over
    python dev-env-utils/scripts/cap_sign_textures.py --apply    # reduce the unclaimed ones
    python dev-env-utils/scripts/cap_sign_textures.py --claims   # which script owns each texture

``--check`` writes nothing and fails on ANY ``textures/blocks/trafficsigns`` texture stored
above its size (85.3 texels per block of the largest plate that draws it, never below 128 px),
whatever produced it: a generator, a legacy one-off script or a hand-made file. The build runs
the same rule as ``SignTextureSizeTest``; this is the script to run while working on signs,
since it names the fix.

``--apply`` reduces, with the same filter the generators use, only the textures no checked
generator claims -- the hand-made faces and the outputs of the retired one-off scripts
(``upscale_signs.py``, ``clean_signs.py``, ``render_ladot.py``, ``render_steep_edge.py``,
``render_tolled_bike.py``), whose sources are partly outside the repository. A texture a
generator writes is left for that generator (``gen_official_faces.py``, ``gen_gap_signs.py``,
``gen_route_markers.py``, ``gen_large_custom_signs.py``, ``gen_led_signs.py``): its ``--check``
is a byte comparison, so fixing its output here would only make that check fail. Re-run the
generator instead; each one sizes its textures through ``sign_texture_size.fit``.
"""

import os
import re
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sign_texture_size as sts  # noqa: E402

SCRIPTS = os.path.dirname(os.path.abspath(__file__))


def claims():
    """{texture name: generator} for every sign texture a --check generator writes."""
    own = {}

    def claim(name, who):
        own.setdefault(sts.name_of(name), who)

    import gen_official_faces as gof
    for registry, _source, _code in gof.CATALOGUE:
        info = gof.sign_info(registry)
        claim(info['texture'], 'gen_official_faces.py')
        if info['back']:
            claim(info['back'], 'gen_official_faces.py')
    with open(os.path.join(SCRIPTS, 'gen_official_faces.py'), encoding='utf-8') as fh:
        for extra in re.findall(r"extras=lambda a: \{'([a-z0-9_]+)\.png'", fh.read()):
            claim(extra, 'gen_official_faces.py')

    import gen_gap_signs as ggs
    for registry, _names, _shape, _draw, _after in ggs.CATALOGUE:
        claim(registry, 'gen_gap_signs.py')
        claim(registry + '_back', 'gen_gap_signs.py')

    import gen_led_signs as gls
    for entry in gls.CATALOGUE:
        claim(entry[0], 'gen_led_signs.py')
        claim(entry[0] + '_e', 'gen_led_signs.py')

    import gen_large_custom_signs as glc
    for entry in glc.CATALOGUE:
        claim(entry[0], 'gen_large_custom_signs.py')

    for name in sts.sign_texture_files():
        if name.startswith(sts.PREFIX + 'route_marker_'):
            claim(name, 'gen_route_markers.py')
    return own


def main(argv):
    if not argv or argv[0] not in ('--check', '--apply', '--claims'):
        print(__doc__)
        return 2
    if argv[0] == '--claims':
        own = claims()
        for name in sorted(sts.sign_texture_files()):
            print('%-70s %s' % (name, own.get(name, '(no checked generator)')))
        return 0

    bad = sts.oversized()
    if argv[0] == '--check':
        if not bad:
            print('every road sign texture is within its plate size')
            return 0
        own = claims()
        print('%d road sign texture(s) above their plate size:' % len(bad))
        for name, path, cur, target in bad:
            fix = own.get(name)
            print('  %s  %d -> %d  (%s)' % (os.path.relpath(path, sts.layout.REPO_ROOT), cur, target,
                                          're-run ' + fix if fix else 'cap_sign_textures.py --apply'))
        return 1

    own = claims()
    done = skipped = 0
    for name, path, cur, target in bad:
        if name in own:
            print('skip %s: written by %s, re-run it' % (name, own[name]))
            skipped += 1
            continue
        with Image.open(path) as im:
            img = im.convert('RGBA')
        sts.reduce(img, target).save(path)
        print('reduced %s %d -> %d' % (os.path.relpath(path, sts.layout.REPO_ROOT), cur, target))
        done += 1
    print('%d reduced, %d left for their generator' % (done, skipped))
    return 1 if skipped else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
