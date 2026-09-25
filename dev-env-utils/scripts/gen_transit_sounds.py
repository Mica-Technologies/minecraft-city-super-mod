"""Synthesise the Transit module's sounds and write them as OGG Vorbis: the help point's connect
chime and the ticket validator's accept and refuse tones.

Made here from decaying sines and envelopes, never recorded or taken from a sound library, with
the helpers of gen_furniture_sounds.py (the door chime's struck bar, the card terminal's piezo)
and the writer of gen_life_safety_sounds.py. Each entry in SOUNDS names a function returning mono
samples in -1..1 at RATE and the level (RMS, of 32767) it is normalised to; the script writes
modules/transit/src/main/resources/assets/csm/sounds/<name>.ogg through ffmpeg and adds a
"<name>" entry to that module's sounds.json if it has none. The event still needs its constant in
TransitSounds; CsmSoundsTest fails the build until it has one.

    python dev-env-utils/scripts/gen_transit_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_transit_sounds.py help_point_chime

There is no --check: Vorbis output is not byte-stable across encoder builds. Re-run a sound after
changing its function, listen to it, and commit the OGG with the change.
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_furniture_sounds as fs  # noqa: E402
import gen_life_safety_sounds as ls  # noqa: E402

ls.ASSETS = os.path.join(ls.REPO, 'modules', 'transit', 'src', 'main', 'resources', 'assets',
                         'csm')
RATE = ls.RATE


def help_point_chime():
    """A help point connecting a call: three struck bars rising a major triad, the last ringing
    on -- the sound of a line opening rather than an alarm."""
    return fs.place(2.2, [(0.0, fs.chime_bar(1.2, 587.3, 91), 0.9),
                          (0.18, fs.chime_bar(1.2, 740.0, 92), 0.9),
                          (0.36, fs.chime_bar(1.8, 880.0, 93), 1.0)])


def validator_accept():
    """A validator taking a fare: one bright, short piezo beep."""
    return fs.place(0.25, [(0.0, fs.piezo(0.16, 2640), 1.0)])


def validator_deny():
    """A validator refusing: two low, buzzy beeps."""
    b = fs.tone(0.14, 520, ((1, 1.0), (3, 0.45), (5, 0.25), (7, 0.12)))
    b *= fs.env_ad(len(b), 0.004, 0.02)
    return fs.place(0.42, [(0.0, b, 1.0), (0.22, b, 1.0)])


SOUNDS = {
    'help_point_chime': (help_point_chime, 3200),
    'validator_accept': (validator_accept, 2600),
    'validator_deny': (validator_deny, 3000),
}


def main(argv):
    names = argv or list(SOUNDS)
    for n in names:
        if n not in SOUNDS:
            sys.exit('no such sound: %s (have: %s)' % (n, ', '.join(SOUNDS)))
    path = os.path.join(ls.ASSETS, 'sounds.json')
    if not os.path.exists(path):
        with open(path, 'w', encoding='utf-8', newline='\n') as fh:
            json.dump({}, fh)
    for n in names:
        fn, rms = SOUNDS[n]
        ls.write_ogg(n, fn(), rms)
    ls.ensure_sounds_json(names)


if __name__ == '__main__':
    main(sys.argv[1:])
