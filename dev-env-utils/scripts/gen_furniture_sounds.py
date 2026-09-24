"""Synthesise the Furniture & Novelties module's furniture sounds and write them as OGG Vorbis.

The kitchen's cabinet doors, drawers and refrigerator door, made here from filtered noise, decaying
sines and envelopes, never recorded or taken from a sound library. Each entry in SOUNDS names a
function returning mono samples in -1..1 at RATE and the level (RMS, of 32767) it is normalised to;
the script writes modules/furnishings/src/main/resources/assets/csm/sounds/<name>.ogg through
ffmpeg and adds a "<name>" entry to that module's sounds.json if it has none. The event still
needs its constant in FurnishingsSounds; CsmSoundsTest fails the build until it has one.

    python dev-env-utils/scripts/gen_furniture_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_furniture_sounds.py drawer_open

There is no --check: Vorbis output is not byte-stable across encoder builds. Re-run a sound after
changing its function, listen to it, and commit the OGG with the change. Borrows the writer from
gen_life_safety_sounds.py.
"""

import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_life_safety_sounds as ls  # noqa: E402

ls.ASSETS = os.path.join(ls.REPO, 'modules', 'furnishings', 'src', 'main', 'resources', 'assets',
                         'csm')
RATE = ls.RATE


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def noise(seconds, seed):
    return np.random.RandomState(seed).uniform(-1, 1, int(RATE * seconds))


def band(x, lo, hi):
    """x band-passed between lo and hi Hz, by zeroing the spectrum outside the band with soft
    shoulders."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / RATE)
    gain = np.clip((f - lo * 0.7) / (lo * 0.3 + 1e-9), 0, 1) * np.clip((hi * 1.3 - f) / (hi * 0.3),
                                                                          0, 1)
    return np.fft.irfft(spec * gain, len(x))


def knock(seconds, partials, decay, seed, click=0.4):
    """A struck wooden or steel panel: a few decaying partials and a short click of noise."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for freq, amp in partials:
        out += amp * np.sin(2 * np.pi * freq * t) * np.exp(-t * decay * (1 + freq / 2000.0))
    n = band(noise(seconds, seed), 800, 5000) * np.exp(-t * 180)
    out += click * n / max(1e-9, np.max(np.abs(n)))
    return out


def place(total, parts):
    """Mixes (start seconds, samples, gain) into a buffer total seconds long."""
    out = np.zeros(int(RATE * total))
    for start, x, g in parts:
        i = int(RATE * start)
        n = min(len(x), len(out) - i)
        out[i:i + n] += g * x[:n]
    return out


def slide(seconds, seed, rumble=34.0, lo=250, hi=2600):
    """A drawer running on its runners: band-passed noise, a roller rumble through it, swelling
    and fading."""
    t = t_of(seconds)
    n = band(noise(seconds, seed), lo, hi)
    n /= max(1e-9, np.max(np.abs(n)))
    roll = 0.6 + 0.4 * np.sin(2 * np.pi * rumble * t) ** 2
    env = np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 0.8
    return n * roll * env


# --------------------------------------------------------------------------------------------
# The sounds
# --------------------------------------------------------------------------------------------
def cabinet_open():
    """A cabinet door opening: the soft tock of the catch letting go, then the door's swish."""
    catch = knock(0.18, [(420, 1.0), (880, 0.4), (1650, 0.2)], 38, 11, click=0.6)
    swish = band(noise(0.3, 12), 300, 1500) * np.sin(np.pi * t_of(0.3) / 0.3) ** 2
    swish /= max(1e-9, np.max(np.abs(swish)))
    return place(0.45, [(0.0, catch, 1.0), (0.05, swish, 0.18)])


def cabinet_close():
    """A cabinet door closing: a short swish and a firm wooden knock on the frame."""
    swish = band(noise(0.15, 13), 300, 1500) * np.linspace(0, 1, int(RATE * 0.15)) ** 2
    swish /= max(1e-9, np.max(np.abs(swish)))
    thud = knock(0.3, [(170, 1.0), (340, 0.55), (610, 0.3), (1150, 0.12)], 26, 14, click=0.5)
    return place(0.45, [(0.0, swish, 0.2), (0.14, thud, 1.0)])


def drawer_open():
    """A drawer pulled out: its run on the runners, and the tap of it reaching its stop."""
    stop = knock(0.12, [(520, 1.0), (1040, 0.35)], 50, 15, click=0.5)
    return place(0.55, [(0.0, slide(0.42, 16), 0.55), (0.4, stop, 0.5)])


def drawer_close():
    """A drawer pushed shut: a quicker run and a wooden knock as the front meets the frame."""
    thud = knock(0.28, [(190, 1.0), (380, 0.5), (700, 0.25)], 28, 17, click=0.5)
    return place(0.5, [(0.0, slide(0.28, 18, rumble=46), 0.5), (0.25, thud, 1.0)])


def fridge_open():
    """A refrigerator door opening: the gasket's seal breaking with a soft low pop and a breath
    of air, and the faint rattle of what is on the door shelves."""
    t = t_of(0.12)
    pop = np.sin(2 * np.pi * 95 * t) * np.exp(-t * 40) * np.sin(np.pi * np.clip(t / 0.02, 0, 0.5))
    air = band(noise(0.5, 21), 200, 2200) * np.exp(-t_of(0.5) * 7)
    air /= max(1e-9, np.max(np.abs(air)))
    rattle = knock(0.08, [(2400, 1.0), (3700, 0.4)], 70, 22, click=0.1)
    return place(0.6, [(0.0, pop, 1.0), (0.02, air, 0.32), (0.16, rattle, 0.12),
                       (0.23, rattle, 0.08)])


def fridge_close():
    """A refrigerator door closing: a heavy, damped thump as the magnetic gasket pulls it home,
    with the bottles on the door shelves clinking."""
    thud = knock(0.4, [(70, 1.0), (140, 0.6), (260, 0.25), (520, 0.08)], 16, 23, click=0.25)
    puff = band(noise(0.12, 24), 150, 900) * np.exp(-t_of(0.12) * 35)
    puff /= max(1e-9, np.max(np.abs(puff)))
    clink = knock(0.1, [(2900, 1.0), (4300, 0.5)], 60, 25, click=0.05)
    return place(0.55, [(0.0, puff, 0.3), (0.03, thud, 1.0), (0.07, clink, 0.1),
                        (0.12, clink, 0.06)])


SOUNDS = {
    'cabinet_open': (cabinet_open, 2600),
    'cabinet_close': (cabinet_close, 3600),
    'drawer_open': (drawer_open, 3000),
    'drawer_close': (drawer_close, 3600),
    'fridge_open': (fridge_open, 3000),
    'fridge_close': (fridge_close, 4200),
}


def main(argv):
    names = argv or list(SOUNDS)
    for n in names:
        if n not in SOUNDS:
            sys.exit('no such sound: %s (have: %s)' % (n, ', '.join(SOUNDS)))
    for n in names:
        fn, rms = SOUNDS[n]
        ls.write_ogg(n, fn(), rms)
    ls.ensure_sounds_json(names)


if __name__ == '__main__':
    main(sys.argv[1:])
