"""Synthesise the Life Safety module's own sounds and write them as OGG Vorbis.

Every sound here is made from sine waves, noise and envelopes by this script, never recorded
from real equipment or taken from a sound library, so the module owns them outright. Each entry
in SOUNDS names a function returning mono samples in -1..1 at RATE, and the level (RMS, of 32767)
it is normalised to; the script writes
modules/lifesafety/src/main/resources/assets/csm/sounds/<name>.ogg through ffmpeg (Pillow does
not do audio) and adds a "<name>" entry to that module's sounds.json if it has none. The event
still needs its constant in LifeSafetySounds; CsmSoundsTest fails the build until it has one.

    python dev-env-utils/scripts/gen_life_safety_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_life_safety_sounds.py aed_cabinet_alarm

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
ASSETS = os.path.join(REPO, 'modules', 'lifesafety', 'src', 'main', 'resources', 'assets', 'csm')
RATE = 44100


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def tone(freq, seconds, attack=0.004, release=0.01):
    """A sine at freq with short linear ramps at each end, so it starts and stops without a
    click."""
    t = t_of(seconds)
    out = np.sin(2 * np.pi * freq * t)
    env = np.ones_like(t)
    a = max(1, int(RATE * attack))
    r = max(1, int(RATE * release))
    env[:a] = np.linspace(0, 1, a)
    env[-r:] = np.linspace(1, 0, r)
    return out * env


def silence(seconds):
    return np.zeros(int(RATE * seconds))


def piezo(freq, seconds):
    """A piezo sounder: a square-ish wave (a sine with its odd harmonics), bright and harsh."""
    t = t_of(seconds)
    out = (np.sin(2 * np.pi * freq * t) + 0.33 * np.sin(2 * np.pi * 3 * freq * t)
           + 0.2 * np.sin(2 * np.pi * 5 * freq * t))
    ramp = int(RATE * 0.003)
    out[:ramp] *= np.linspace(0, 1, ramp)
    out[-ramp:] *= np.linspace(1, 0, ramp)
    return out


# --------------------------------------------------------------------------------------------
# The sounds
# --------------------------------------------------------------------------------------------
def aed_cabinet_alarm():
    """An AED cabinet's door alarm: three fast piezo chirps, the pattern a front desk learns to
    recognise. About 0.7 s; played once when the door opens."""
    parts = []
    for _ in range(3):
        parts += [piezo(3150, 0.09), silence(0.07)]
    parts.append(silence(0.2))
    return np.concatenate(parts)


SOUNDS = {
    'aed_cabinet_alarm': (aed_cabinet_alarm, 7000),
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
