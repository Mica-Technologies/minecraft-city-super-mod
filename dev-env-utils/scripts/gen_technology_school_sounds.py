"""Synthesise the Technology module's school bell sounds and write them as OGG Vorbis.

Every sound here is made from sine waves, noise and envelopes by this script, never recorded
from real equipment or taken from a sound library, so the module owns them outright. Each entry
in SOUNDS names a function returning mono samples in -1..1 at RATE, and the level (RMS, of 32767)
it is normalised to; the script writes
modules/technology/src/main/resources/assets/csm/sounds/<name>.ogg through ffmpeg and adds a
"<name>" entry to that module's sounds.json if it has none. The event still needs its constant in
TechnologySounds; CsmSoundsTest fails the build until it has one.

    python dev-env-utils/scripts/gen_technology_school_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_technology_school_sounds.py school_chime

The sounds (see assets/docs/SCHOOL_TIME_AND_PA.md):

- school_bell_ring: the hallway bell, a vibrating electric bell. Its hammer strikes the dome
  about eighteen times a second while the current flows, each strike exciting the dome's
  inharmonic partials, then the dome rings out. 3.0 s; the bell block plays it again every
  three seconds while it stays powered.
- school_tone: an electronic class-change tone over the PA, three warm pulses.
- school_chime: the three-tone chime before an announcement, falling G, E, C on a soft mallet
  bar sound.

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
ASSETS = os.path.join(REPO, 'modules', 'technology', 'src', 'main', 'resources', 'assets', 'csm')
RATE = 44100


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def silence(seconds):
    return np.zeros(int(RATE * seconds))


def place(total, sound, at):
    """Adds sound into total starting at `at` seconds, cut at total's end."""
    i = int(RATE * at)
    seg = sound[:max(0, len(total) - i)]
    total[i:i + len(seg)] += seg


# --------------------------------------------------------------------------------------------
# The sounds
# --------------------------------------------------------------------------------------------
def dome_strike(seconds, fundamental, seed, strength=1.0):
    """One hammer strike on a small steel dome: inharmonic partials (a dome's, not a string's),
    the high ones dying first, and the hammer's click at the front."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for ratio, level, decay in ((1.0, 1.0, 0.55), (2.41, 0.55, 0.30), (3.93, 0.32, 0.18),
                                (5.62, 0.18, 0.10), (7.48, 0.10, 0.06)):
        out += level * np.sin(2 * np.pi * fundamental * ratio * t + seed) * np.exp(-t / decay)
    n = int(RATE * 0.003)
    out[:n] += np.random.RandomState(seed).uniform(-0.6, 0.6, n) * np.linspace(1, 0, n)
    return out * strength


def school_bell_ring():
    """The hallway bell: strikes at about 18 a second for 2.4 s, the rate and the hammer's
    strength wandering a little as a real bell's do, then 0.6 s of ring-out."""
    total = silence(3.0)
    rng = np.random.RandomState(7)
    at = 0.0
    k = 0
    while at < 2.4:
        strength = 0.85 + 0.15 * rng.rand()
        place(total, dome_strike(0.9, 1180.0, k % 13, strength), at)
        at += 1.0 / (17.5 + 1.2 * rng.rand())
        k += 1
    # the mechanism's buzz under the strikes
    t = t_of(2.4)
    buzz = 0.04 * np.sign(np.sin(2 * np.pi * 120 * t)) * np.random.RandomState(8).uniform(
        0.6, 1.0, len(t))
    place(total, buzz, 0.0)
    fade = int(RATE * 0.05)
    total[:fade] *= np.linspace(0, 1, fade)
    total[-fade:] *= np.linspace(1, 0, fade)
    return total


def warm_tone(freq, seconds):
    """A soft electronic tone: a sine with a little of its second and third harmonics, eased in
    and out so it does not click."""
    t = t_of(seconds)
    out = (np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * 2 * freq * t)
           + 0.12 * np.sin(2 * np.pi * 3 * freq * t))
    ramp = int(RATE * 0.02)
    out[:ramp] *= np.linspace(0, 1, ramp)
    out[-ramp:] *= np.linspace(1, 0, ramp)
    return out


def school_tone():
    """The class-change tone: three pulses of 0.5 s at 784 Hz, 0.15 s apart."""
    total = silence(2.1)
    for i in range(3):
        place(total, warm_tone(784.0, 0.5), 0.05 + i * 0.65)
    return total


def mallet(freq, seconds):
    """A soft mallet on a tuned bar: the fundamental and its fourth partial, a quick attack and a
    long decay, the overtone dying first."""
    t = t_of(seconds)
    out = (np.sin(2 * np.pi * freq * t) * np.exp(-t / 1.1)
           + 0.22 * np.sin(2 * np.pi * freq * 4.0 * t) * np.exp(-t / 0.25)
           + 0.08 * np.sin(2 * np.pi * freq * 10.0 * t) * np.exp(-t / 0.06))
    attack = int(RATE * 0.004)
    out[:attack] *= np.linspace(0, 1, attack)
    return out


def school_chime():
    """The pre-announcement chime: G5, E5, C5, 0.55 s apart, the last left to ring."""
    total = silence(3.0)
    for i, f in enumerate((783.99, 659.25, 523.25)):
        place(total, mallet(f, 2.2), 0.02 + i * 0.55)
    fade = int(RATE * 0.2)
    total[-fade:] *= np.linspace(1, 0, fade)
    return total


# Levels are RMS of 32767. The bell is a sounder, at about the fire alarm horns' level (FIRE_ALARM
# _SYSTEM.md, Sound File Standards); the PA tones are quieter, as a speaker's are.
SOUNDS = {
    'school_bell_ring': (school_bell_ring, 9000),
    'school_tone': (school_tone, 6500),
    'school_chime': (school_chime, 6000),
}


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------
def write_ogg(name, samples, target_rms):
    samples = samples / max(1e-9, np.max(np.abs(samples)))
    rms = np.sqrt(np.mean(samples ** 2)) * 32767
    samples = np.clip(samples * (target_rms / max(1e-9, rms)), -1, 1)
    pcm = (samples * 32767).astype('<i2')
    out = os.path.join(ASSETS, 'sounds', name + '.ogg')
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + '.wav')
        with wave.open(wav, 'wb') as fh:
            fh.setnchannels(1)
            fh.setsampwidth(2)
            fh.setframerate(RATE)
            fh.writeframes(pcm.tobytes())
        os.makedirs(os.path.dirname(out), exist_ok=True)
        subprocess.check_call(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav,
                               '-c:a', 'libvorbis', '-q:a', '4', out])
    print('wrote %s  (%.2f s, rms %d)' % (os.path.relpath(out, REPO), len(pcm) / RATE,
                                        target_rms))


def ensure_sounds_json(names):
    path = os.path.join(ASSETS, 'sounds.json')
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
