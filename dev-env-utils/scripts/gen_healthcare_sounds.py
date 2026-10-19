"""Synthesise the Healthcare module's sounds and write them as OGG Vorbis.

Every sound here is made from sine waves, noise and envelopes by this script, never recorded
from real equipment or taken from a sound library, so the module owns them outright. Each entry
in SOUNDS names a function returning mono samples in -1..1 at RATE, and the level (RMS, of 32767)
it is normalised to; the script writes
modules/healthcare/src/main/resources/assets/csm/sounds/<name>.ogg through ffmpeg (Pillow does
not do audio) and adds a "<name>" entry to that module's sounds.json (creating it if need be).
The event still needs its constant in HealthcareSounds; CsmSoundsTest fails the build until it
has one.

    py dev-env-utils/scripts/gen_healthcare_sounds.py            # write every sound
    py dev-env-utils/scripts/gen_healthcare_sounds.py code_blue_chime

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
ASSETS = os.path.join(REPO, 'modules', 'healthcare', 'src', 'main', 'resources', 'assets', 'csm')
RATE = 44100


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def silence(seconds):
    return np.zeros(int(RATE * seconds))


def chime_note(freq, seconds, decay=0.55):
    """One struck chime bar: a sine with its bright, quickly dying upper partials and a soft
    hammer at the front, ringing down exponentially."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for ratio, level, d in ((1.0, 1.0, decay), (2.0, 0.28, decay * 0.5),
                            (3.01, 0.12, decay * 0.3), (4.2, 0.05, decay * 0.18)):
        out += level * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t / d)
    a = int(RATE * 0.004)
    out[:a] *= np.linspace(0, 1, a)
    r = int(RATE * 0.03)
    out[-r:] *= np.linspace(1, 0, r)
    return out


def mix_at(total, part, at):
    i = int(RATE * at)
    seg = part[:max(0, len(total) - i)]
    total[i:i + len(seg)] += seg


def code_blue_chime():
    """The code blue annunciator's alert: a falling three-note chime played twice, quick and
    insistent, the way an overhead alert tone cuts through a ward. About 2.2 s; the annunciator
    plays it again every few seconds while the code is active."""
    total = np.zeros(int(RATE * 2.25))
    at = 0.0
    for _ in range(2):
        for f in (1046.5, 880.0, 698.5):
            mix_at(total, chime_note(f, 0.9, decay=0.32), at)
            at += 0.17
        at += 0.55
    return total


def piezo(freq, seconds):
    """A piezo sounder: a sine with its odd harmonics, bright and harsh."""
    t = t_of(seconds)
    out = (np.sin(2 * np.pi * freq * t) + 0.33 * np.sin(2 * np.pi * 3 * freq * t)
           + 0.2 * np.sin(2 * np.pi * 5 * freq * t))
    ramp = int(RATE * 0.003)
    out[:ramp] *= np.linspace(0, 1, ramp)
    out[-ramp:] *= np.linspace(1, 0, ramp)
    return out


def medical_gas_alarm():
    """The medical gas master alarm's buzzer: a steady pulsed piezo, four half-second pulses,
    which the panel repeats while it is in alarm and not silenced. 2 s."""
    parts = []
    for _ in range(4):
        parts += [piezo(2800, 0.3), silence(0.2)]
    return np.concatenate(parts)


def infant_security_alarm():
    """The infant security panel's tag alarm: a fast two-tone warble, a second and a half,
    played once as the panel goes into alarm."""
    parts = []
    for i in range(12):
        parts.append(piezo(2400 if i % 2 == 0 else 3100, 0.11))
    parts.append(silence(0.15))
    return np.concatenate(parts)


SOUNDS = {
    'code_blue_chime': (code_blue_chime, 5200),
    'medical_gas_alarm': (medical_gas_alarm, 4200),
    'infant_security_alarm': (infant_security_alarm, 4200),
}


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
