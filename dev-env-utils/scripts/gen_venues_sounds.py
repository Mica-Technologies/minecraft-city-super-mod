#!/usr/bin/env python3
"""Synthesise the Sports & Venues module's sounds and write them as OGG Vorbis.

Every sound here is made from sine waves, noise and envelopes by this script, never recorded
from real equipment or taken from a sound library, so the module owns them outright. Each entry
in SOUNDS names a function returning mono samples in -1..1 at RATE, and the level (RMS, of 32767)
it is normalised to; the script writes
modules/venues/src/main/resources/assets/csm/sounds/<name>.ogg through ffmpeg and adds a
"<name>" entry to that module's sounds.json (creating it if need be). The event still needs its
constant in VenuesSounds; CsmSoundsTest fails the build until it has one.

    py dev-env-utils/scripts/gen_venues_sounds.py                    # write every sound
    py dev-env-utils/scripts/gen_venues_sounds.py venue_turnstile

There is no --check: Vorbis output is not byte-stable across encoder builds. Re-run a sound
after changing its function, listen to it, and commit the OGG with the change.
"""
import json
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'modules', 'venues', 'src', 'main', 'resources', 'assets', 'csm')
RATE = 44100


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def silence(seconds):
    return np.zeros(int(RATE * seconds))




def tone(freq, seconds, harmonics=((1.0, 1.0),), attack=0.004, release=0.02):
    """A clean electronic tone: a sine and the partials given, with short ramps so it does not
    click."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for ratio, level in harmonics:
        out += level * np.sin(2 * np.pi * freq * ratio * t)
    a = max(1, int(RATE * attack))
    r = max(1, int(RATE * release))
    out[:a] *= np.linspace(0, 1, a)
    out[-r:] *= np.linspace(1, 0, r)
    return out


def venue_scanner_accept():
    """A ticket scanner's accept: two short rising beeps, high and bright, the way a handheld or
    pedestal barcode scanner chirps a good read. 0.32 s."""
    beep = ((1.0, 1.0), (2.0, 0.18), (3.0, 0.06))
    return np.concatenate([tone(2093.0, 0.09, beep), silence(0.04), tone(2637.0, 0.13, beep),
                           silence(0.06)])


def venue_scanner_deny():
    """A ticket scanner's refusal: one low, rough buzz, a square-ish wave at 220 Hz with a
    slow wobble. 0.5 s."""
    t = t_of(0.45)
    f = 220.0
    sq = np.zeros_like(t)
    for k in (1, 3, 5, 7, 9):
        sq += np.sin(2 * np.pi * f * k * t) / k
    sq *= 0.85 + 0.15 * np.sin(2 * np.pi * 18 * t)
    r = int(RATE * 0.03)
    sq[:r] *= np.linspace(0, 1, r)
    sq[-r:] *= np.linspace(1, 0, r)
    return np.concatenate([sq, silence(0.05)])


def click(seconds, freq, decay, noise=0.6, seed=0):
    """A metallic click: a burst of filtered noise and a damped ring."""
    rng = np.random.default_rng(seed)
    t = t_of(seconds)
    n = rng.standard_normal(len(t))
    # a crude high-pass: the difference of neighbours brightens the noise
    n = np.concatenate([[0.0], np.diff(n)])
    ring = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * freq * 2.76 * t)
    out = (noise * n + ring) * np.exp(-t / decay)
    a = int(RATE * 0.0015)
    out[:a] *= np.linspace(0, 1, a)
    return out


def venue_turnstile():
    """A tripod turnstile indexing a third of a turn: the pawl clicking over three teeth of the
    ratchet as the arms are pushed, then the heavier clunk of the hub dropping into its detent.
    About 0.75 s."""
    total = np.zeros(int(RATE * 0.78))
    for i, at in enumerate((0.0, 0.11, 0.21)):
        mix_at(total, 0.55 * click(0.08, 3100.0 - 150 * i, 0.012, seed=i + 1), at)
    mix_at(total, click(0.4, 640.0, 0.06, noise=0.9, seed=9), 0.34)
    mix_at(total, 0.5 * click(0.3, 1280.0, 0.03, noise=0.3, seed=10), 0.345)
    return total


SOUNDS = {
    'venue_scanner_accept': (venue_scanner_accept, 4600),
    'venue_scanner_deny': (venue_scanner_deny, 4200),
    'venue_turnstile': (venue_turnstile, 5000),
}


def mix_at(total, part, at):
    i = int(RATE * at)
    seg = part[:max(0, len(total) - i)]
    total[i:i + len(seg)] += seg


def write_ogg(name, samples, target_rms):
    samples = samples / max(1e-9, np.max(np.abs(samples)))
    rms = np.sqrt(np.mean(samples ** 2)) * 32767
    samples = np.clip(samples * (target_rms / max(1e-9, rms)), -1, 1)
    pcm = (samples * 32767).astype('<i2')
    out = os.path.join(ASSETS, 'sounds', name + '.ogg')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + '.wav')
        with wave.open(wav, 'wb') as fh:
            fh.setnchannels(1)
            fh.setsampwidth(2)
            fh.setframerate(RATE)
            fh.writeframes(pcm.tobytes())
        subprocess.check_call(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav,
                               '-c:a', 'libvorbis', '-q:a', '4', out])
    print('wrote %s  (%.2f s, rms %d)' % (os.path.relpath(out, REPO), len(pcm) / RATE,
                                        target_rms))


def ensure_sounds_json(names):
    path = os.path.join(ASSETS, 'sounds.json')
    data = {}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as fh:
            data = json.load(fh)
    added = [n for n in names if n not in data]
    for n in added:
        data[n] = {'category': 'block', 'sounds': [{'name': 'csm:' + n, 'stream': False}]}
    if added:
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
        write_ogg(n, fn(), rms)
    ensure_sounds_json(names)


if __name__ == '__main__':
    main(sys.argv[1:])
