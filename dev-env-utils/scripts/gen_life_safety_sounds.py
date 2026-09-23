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


def gong_strike(seconds, fundamental=620.0, seed=3):
    """One strike of a heavy brass gong: inharmonic partials, the high ones dying first, and the
    clank of the hammer at the front."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for ratio, level, decay in ((1.0, 1.0, 1.6), (2.32, 0.6, 0.9), (4.25, 0.35, 0.45),
                                (6.63, 0.18, 0.25), (0.5, 0.25, 2.2)):
        out += level * np.sin(2 * np.pi * fundamental * ratio * t) * np.exp(-t / decay)
    n = int(RATE * 0.005)
    out[:n] += np.random.RandomState(seed).uniform(-0.7, 0.7, n) * np.linspace(1, 0, n)
    return out


def station_bell():
    """The firehouse gong's signal: three rounds of three strikes, the rounds a second apart, the
    last strike left to ring out. About 6 s."""
    total = np.zeros(int(RATE * 6.2))
    strike = gong_strike(2.4)
    at = 0.0
    for _ in range(3):
        for _ in range(3):
            i = int(RATE * at)
            seg = strike[:len(total) - i]
            total[i:i + len(seg)] += seg
            at += 0.42
        at += 0.75
    return total


def water_motor_gong():
    """A sprinkler's water motor gong: the water flow spins a turbine whose hammer strikes a
    ten-inch dome over and over, about three times a second, for as long as the water runs.
    It loops as a fire alarm sounder does, so each strike's ring-out is wrapped round past the
    loop's end onto its start and the loop has no seam. The hammer's strength wanders a little,
    as a turbine's does. 4.2 s, fourteen strikes."""
    period, count = 0.3, 14
    total = np.zeros(int(RATE * period * count))
    strike = gong_strike(3.0, fundamental=540.0, seed=11)
    levels = 1.0 - 0.12 * np.random.RandomState(12).rand(count)
    for k in range(count):
        i = int(RATE * period * k)
        idx = (np.arange(len(strike)) + i) % len(total)
        np.add.at(total, idx, levels[k] * strike)
    return total


def station_prealert():
    """The alerting system's pre-alert: a two-pitch warble for two seconds, so the tones that
    follow are listened for."""
    parts = []
    for i in range(16):
        parts.append(tone(880 if i % 2 == 0 else 1180, 0.125, attack=0.002, release=0.002))
    return np.concatenate(parts)


def two_tone(a, b):
    """Sequential two-tone paging: one tone for a second, the next for two, as a station's
    receiver is set to answer. The pairs here are this mod's own, one per zone."""
    def make():
        return np.concatenate([tone(a, 1.0), silence(0.05), tone(b, 2.0, release=0.05),
                               silence(0.3)])
    return make


def all_call():
    """The all-call: a long single tone, then the zone pairs' first tones stepped through."""
    return np.concatenate([tone(1500, 1.6), silence(0.1)]
                          + [tone(f, 0.35, attack=0.003, release=0.01)
                             for f in (660, 780, 900, 1020, 1140, 1260)] + [silence(0.3)])


def metal_detector_alarm():
    """A walk-through metal detector's alarm: a rising two-tone beep, three times, about a
    second."""
    parts = []
    for _ in range(3):
        parts += [tone(1400, 0.12, attack=0.003, release=0.01),
                  tone(1850, 0.12, attack=0.003, release=0.01), silence(0.08)]
    return np.concatenate(parts)


def siren(freqs, amp=None):
    """A mechanical rotating siren from the pitch it is making at each sample (Hz, the lower of
    its two rotor tones): two chopped tones a minor third apart, each a buzz of odd harmonics, the
    air rushing under them. The phase is integrated from the pitch, so a sweep glides."""
    freqs = np.asarray(freqs, dtype=float)
    out = np.zeros_like(freqs)
    for ratio, level in ((1.0, 1.0), (1.2, 0.8)):
        phase = 2 * np.pi * np.cumsum(freqs * ratio) / RATE
        for h, hl in ((1, 1.0), (3, 0.33), (5, 0.16), (7, 0.08)):
            out += level * hl * np.sin(h * phase)
    out += np.random.RandomState(11).uniform(-0.12, 0.12, len(out))
    if amp is not None:
        out *= amp
    return out


def seamless(freqs):
    """Scales a looping pitch curve so it ends on a whole number of cycles of its lower tone and
    the loop has no click. The upper tone is 6/5 of it, so five whole cycles of the lower make six
    of the upper."""
    freqs = np.asarray(freqs, dtype=float)
    cycles = np.sum(freqs) / RATE
    target = max(5.0, round(cycles / 5.0) * 5.0)
    return freqs * (target / cycles)


def siren_steady():
    """Alert: the siren at full speed, a steady chord. Two seconds, looped."""
    return siren(seamless(np.full(int(RATE * 2.0), 540.0)))


def siren_wail():
    """Attack: winding up from low to full, holding, and running down, over and over. Twelve
    seconds, looped at the bottom of the run-down, where the siren is quietest."""
    up = np.linspace(180, 560, int(RATE * 4.0))
    hold = np.full(int(RATE * 2.0), 560.0)
    down = np.linspace(560, 180, int(RATE * 6.0))
    f = seamless(np.concatenate([up, hold, down]))
    amp = 0.25 + 0.75 * (f - f.min()) / (f.max() - f.min())
    return siren(f, amp)


def siren_hilo():
    """Fire: alternating high and low, the European-style call for volunteers. Two seconds,
    looped."""
    half = int(RATE * 0.5)
    f = np.concatenate([np.full(half, 560.0), np.full(half, 420.0)] * 2)
    # soften each step over 20 ms so it slides rather than clicks
    k = int(RATE * 0.02)
    kernel = np.ones(k) / k
    f = np.convolve(np.concatenate([f[-k:], f, f[:k]]), kernel, mode='same')[k:-k]
    return siren(seamless(f))


def siren_growl():
    """The test: one short wind-up to a third of full speed and back down, about twelve
    seconds."""
    up = np.linspace(60, 330, int(RATE * 5.0))
    down = np.linspace(330, 40, int(RATE * 7.0))
    f = np.concatenate([up, down])
    amp = (f - f.min()) / (f.max() - f.min())
    return siren(f, amp)


def call_box_ring():
    """A call box connecting: the button's click, two ring-back tones (440 and 480 Hz together,
    as a line rings), and the click of the line picking up."""
    click = np.random.RandomState(5).uniform(-1, 1, int(RATE * 0.01)) * np.linspace(1, 0,
                                                                                int(RATE * 0.01))
    ring = tone(440, 1.2) + tone(480, 1.2)
    return np.concatenate([click, silence(0.3), ring, silence(1.0), ring, silence(0.6),
                           click * 0.8, silence(0.2)])


# Levels are RMS of 32767. The station alerting sounds are at the fire alarm horns' level (about
# 10,000; FIRE_ALARM_SYSTEM.md § Sound File Standards): at 6,000 they were too soft to carry
# through a station.
SOUNDS = {
    'siren_steady': (siren_steady, 7000),
    'siren_wail': (siren_wail, 7000),
    'siren_hilo': (siren_hilo, 7000),
    'siren_growl': (siren_growl, 6000),
    'call_box_ring': (call_box_ring, 5000),
    'metal_detector_alarm': (metal_detector_alarm, 6500),
    'station_prealert': (station_prealert, 10000),
    'station_tone_engine': (two_tone(630, 1010), 10000),
    'station_tone_ladder': (two_tone(720, 1180), 10000),
    'station_tone_medic': (two_tone(840, 1320), 10000),
    'station_tone_battalion': (two_tone(930, 570), 10000),
    'station_tone_all_call': (all_call, 10000),
    'aed_cabinet_alarm': (aed_cabinet_alarm, 7000),
    'station_bell': (station_bell, 9000),
    'water_motor_gong': (water_motor_gong, 9900),
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
