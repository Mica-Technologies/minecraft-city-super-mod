"""Synthesise the Parks & Greenery module's sounds and write them as OGG Vorbis: the chainsaw's
cord pull, its start, one second of idle (played back to back while it runs) and a cut at full
throttle.

Made here from a two-stroke engine model (a sharp pressure pulse each firing, its rate following
the throttle, under band-passed exhaust noise), never recorded or taken from a sound library, with
the helpers of gen_furniture_sounds.py and the writer of gen_life_safety_sounds.py. Each entry in
SOUNDS names a function returning mono samples in -1..1 at RATE and the level (RMS, of 32767) it
is normalised to; the script writes modules/parks/src/main/resources/assets/csm/sounds/<name>.ogg
through ffmpeg and adds a "<name>" entry, in the player category, to that module's sounds.json if
it has none. The event still needs its constant in ParksSounds; CsmSoundsTest fails the build
until it has one.

    python dev-env-utils/scripts/gen_parks_tool_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_parks_tool_sounds.py chainsaw_idle

There is no --check: Vorbis output is not byte-stable across encoder builds. Re-run a sound after
changing its function, listen to it, and commit the OGG with the change.
"""

import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_furniture_sounds as fs  # noqa: E402
import gen_life_safety_sounds as ls  # noqa: E402

ls.ASSETS = os.path.join(ls.REPO, 'modules', 'parks', 'src', 'main', 'resources', 'assets', 'csm')
RATE = ls.RATE


def engine(freq, seed, rasp=0.35):
    """A small two-stroke engine firing at freq (Hz, one value per sample): a sharp decaying
    pressure pulse each cycle, a buzz of exhaust noise gated by it, and its second harmonic for
    the muffler's ring."""
    n = len(freq)
    phase = np.cumsum(freq) / RATE
    frac = phase - np.floor(phase)
    pulse = np.exp(-frac * 9.0) - 0.12
    ring = 0.35 * np.sin(2 * np.pi * 2 * phase) + 0.18 * np.sin(2 * np.pi * 3 * phase)
    hiss = fs.band(np.random.RandomState(seed).uniform(-1, 1, n), 400, 5000)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    gate = np.exp(-frac * 5.0)
    out = pulse + ring + rasp * hiss * (0.4 + gate)
    return out / max(1e-9, np.max(np.abs(out)))


def cord(seconds, seed):
    """The starter cord running out: a rope's rasp rising in pitch as it speeds up."""
    t = fs.t_of(seconds)
    n = fs.band(fs.noise(seconds, seed), 1500, 6000)
    n /= max(1e-9, np.max(np.abs(n)))
    flutter = 0.6 + 0.4 * np.sin(2 * np.pi * (30 + 60 * t / seconds) * t) ** 2
    return n * flutter * np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 0.6


def turn_over(seconds, seed):
    """The engine turned by the cord without firing: a few compression thumps, slowing."""
    t = fs.t_of(seconds)
    f = 14.0 * np.exp(-t * 2.2) + 3.0
    phase = np.cumsum(f) / RATE
    frac = phase - np.floor(phase)
    thump = np.exp(-frac * 14.0) * np.sin(2 * np.pi * 70 * t)
    clack = fs.band(fs.noise(seconds, seed), 600, 2500) * np.exp(-frac * 30.0) * 0.4
    return (thump + clack) * np.exp(-t * 2.5)


def chainsaw_pull():
    """One pull of the cord: the rasp and the engine turning over, not catching."""
    return fs.place(0.75, [(0.0, cord(0.32, 501), 0.6), (0.02, turn_over(0.7, 502), 1.0)])


def chainsaw_start():
    """The last pull catching: the rasp, a sputter, a rev and the drop to idle."""
    total = 1.6
    n = int(RATE * 1.3)
    t = np.arange(n) / RATE
    f = 46 + 70 * np.exp(-((t - 0.35) / 0.18) ** 2) + 8 * np.exp(-t * 6.0)
    f *= 1 + 0.03 * np.sin(2 * np.pi * 7 * t)
    run = engine(f, 503, rasp=0.45)
    run *= np.clip(t / 0.05, 0, 1) * (0.8 + 0.2 * np.exp(-((t - 0.35) / 0.2) ** 2))
    run *= np.clip((1.3 - t) / 0.05, 0, 1)
    return fs.place(total, [(0.0, cord(0.25, 504), 0.5), (0.2, run, 1.0)])


def chainsaw_idle():
    """One second of idle, a whole number of firings so it plays back to back without a beat:
    48 firings with a slight unevenness, fading in and out over 8 ms at the seam."""
    n = RATE
    t = np.arange(n) / RATE
    f = 48 + 1.5 * np.sin(2 * np.pi * 3 * t)
    out = engine(f, 505, rasp=0.3)
    return out * fs.env_ad(n, 0.008, 0.008)


def chainsaw_cut():
    """Full throttle through wood: the engine screaming up to speed and labouring under the load,
    the chain's rasp through the cut, and the drop back to idle at the end."""
    total = 1.5
    n = int(RATE * total)
    t = np.arange(n) / RATE
    up = np.clip(t / 0.18, 0, 1)
    down = np.clip((total - t) / 0.3, 0, 1)
    f = 48 + (178 - 48) * up * down + 6 * np.sin(2 * np.pi * 4.5 * t) * up * down
    out = engine(f, 506, rasp=0.5)
    chip = fs.band(fs.noise(total, 507), 2000, 9000)
    chip /= max(1e-9, np.max(np.abs(chip)))
    chatter = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 23 * t))
    out = out * 0.75 + chip * chatter * up * down * 0.45
    return out * fs.env_ad(n, 0.01, 0.05)


SOUNDS = {
    'chainsaw_pull': (chainsaw_pull, 2400),
    'chainsaw_start': (chainsaw_start, 3000),
    'chainsaw_idle': (chainsaw_idle, 2400),
    'chainsaw_cut': (chainsaw_cut, 3600),
}


def ensure_sounds_json(names):
    path = os.path.join(ls.ASSETS, 'sounds.json')
    data = {}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as fh:
            data = json.load(fh)
    added = [n for n in names if n not in data]
    for n in added:
        data[n] = {'category': 'player', 'sounds': [{'name': 'csm:' + n, 'stream': False}]}
    if added or not os.path.exists(path):
        with open(path, 'w', encoding='utf-8', newline='\n') as fh:
            json.dump(data, fh, indent=2)
            fh.write('\n')
        print('added to sounds.json: ' + ', '.join(added))


def main(argv):
    names = argv or list(SOUNDS)
    for n in names:
        if n not in SOUNDS:
            sys.exit('no such sound: %s (have: %s)' % (n, ', '.join(SOUNDS)))
    for n in names:
        fn, rms = SOUNDS[n]
        ls.write_ogg(n, fn(), rms)
    ensure_sounds_json(names)


if __name__ == '__main__':
    main(sys.argv[1:])
