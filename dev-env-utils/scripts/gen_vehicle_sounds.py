"""Synthesise the CSM: Vehicles siren speaker's three tones and write them as OGG Vorbis.

An electronic siren drives a 100 W horn speaker with a square-ish wave whose pitch is swept by
the siren's controller; the horn rolls off the harsh top. Each tone is one sweep period, written
so it loops without a click: the period is a whole number of samples and the waveform's phase
is scaled to complete a whole number of cycles over it.

  wail   a slow rise and fall, 650 to 1500 Hz over 4.8 s
  yelp   the same range, fast: 0.32 s a sweep
  hi-lo  two tones, 960 and 770 Hz, 0.55 s each

and the tree crew chip bed's chipper: chipper_run (a diesel and the drum idling, looped) and
chipper_chip (one log through the knives).

Immersive Vehicles plays a pack sound named "<packID>:<name>" from assets/<packID>/sounds/
<name>.ogg, with no sounds.json, so the module's CSM sound enum and sounds.json have no part in
it. Writes modules/vehicles/src/main/resources/assets/csmvehicles/sounds/. Vorbis output is not
byte-stable, so there is no --check: listen, then commit. Run from the repo root:

    python dev-env-utils/scripts/gen_vehicle_sounds.py [name ...]
"""

import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

OUT_DIR = os.path.join('modules', 'vehicles', 'src', 'main', 'resources', 'assets',
                       'csmvehicles', 'sounds')
RATE = 44100
TARGET_RMS = 6000
LOW, HIGH = 650.0, 1500.0


def wail_profile(n):
    """Rise for two fifths of the period, fall for the rest, eased at the turns."""
    t = np.arange(n) / n
    rise = 0.4
    up = np.clip(t / rise, 0, 1)
    down = np.clip((t - rise) / (1 - rise), 0, 1)
    shape = np.where(t < rise, np.sin(up * np.pi / 2), np.cos(down * np.pi / 2))
    return LOW + (HIGH - LOW) * shape


def yelp_profile(n):
    """The wail's shape, fast: a sawtooth-ish rise and a quick drop."""
    t = np.arange(n) / n
    shape = np.where(t < 0.8, np.sin(t / 0.8 * np.pi / 2), np.cos((t - 0.8) / 0.2 * np.pi / 2))
    return LOW + (HIGH - LOW) * shape


def hilo_profile(n):
    half = n // 2
    return np.concatenate([np.full(half, 960.0), np.full(n - half, 770.0)])


def render(freq):
    """A siren voice over one period of frequencies, phase-closed so it loops."""
    phase = np.cumsum(freq) / RATE  # in cycles
    cycles = round(phase[-1])
    phase *= cycles / phase[-1]  # a whole number of cycles over the period
    x = 2 * np.pi * phase
    # odd harmonics, rolled off: a square wave through a horn
    wave_ = (np.sin(x) + np.sin(3 * x) / 3 * 0.6 + np.sin(5 * x) / 5 * 0.35
             + np.sin(7 * x) / 7 * 0.15)
    return wave_


def write(name, signal):
    signal = signal / np.max(np.abs(signal))
    rms = np.sqrt(np.mean(signal ** 2)) * 32767
    signal = np.clip(signal * (TARGET_RMS / rms), -1, 1)
    pcm = (signal * 32767).astype('<i2')
    out = os.path.join(OUT_DIR, name + '.ogg')
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + '.wav')
        with wave.open(wav, 'wb') as fh:
            fh.setnchannels(1)
            fh.setsampwidth(2)
            fh.setframerate(RATE)
            fh.writeframes(pcm.tobytes())
        os.makedirs(OUT_DIR, exist_ok=True)
        subprocess.check_call(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav,
                               '-c:a', 'libvorbis', '-q:a', '4', out])
    print('wrote %s  (%.2f s)' % (out, len(pcm) / RATE))


def chipper_run(seconds=3.0):
    """The chip bed's chipper idling: a diesel's firing pulses under the drum's whine and the
    blower's rush. Every component completes whole cycles over the loop, so it loops clean."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    firing = 28.0 * 3  # a three-cylinder at 1,700 rpm fires about 85 times a second
    pulses = np.zeros(n)
    for k, level in ((1, 1.0), (2, 0.55), (3, 0.35), (5, 0.15)):
        f = round(firing * k * seconds) / seconds  # whole cycles over the loop
        pulses += level * np.sin(2 * np.pi * f * t)
    drum = 0.35 * np.sin(2 * np.pi * (round(190 * seconds) / seconds) * t)
    rng = np.random.RandomState(11)
    rush = rng.normal(0, 1, n)
    # a gentle low-pass on the rush (a running mean) so it hisses rather than crackles
    kernel = np.ones(12) / 12
    rush = np.convolve(np.concatenate([rush[-12:], rush]), kernel, mode='valid')[1:n + 1] * 0.25
    return pulses + drum + rush


def chipper_chip(seconds=0.55):
    """One log through the drum: a thump as the knives bite, then a fading crunch."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    rng = np.random.RandomState(5)
    crunch = rng.normal(0, 1, n) * np.exp(-t / 0.18)
    # grain: the knives hit about 40 times a second as the log feeds
    crunch *= 0.6 + 0.4 * (np.sin(2 * np.pi * 40 * t) > 0)
    thump = np.sin(2 * np.pi * 70 * t) * np.exp(-t / 0.06) * 1.5
    out = crunch + thump
    out[-200:] *= np.linspace(1, 0, 200)
    return out


SOUNDS = {
    'siren_wail': lambda: render(wail_profile(int(RATE * 4.8))),
    # several sweeps to a file, so the loop restarts less often than it sweeps
    'siren_yelp': lambda: render(np.tile(yelp_profile(int(RATE * 0.32)), 8)),
    'siren_hilo': lambda: render(np.tile(hilo_profile(int(RATE * 1.1)), 2)),
    'chipper_run': chipper_run,
    'chipper_chip': chipper_chip,
}


def main():
    # Name the sounds to write, or none for all. Vorbis output differs run to run, so writing
    # only the ones that changed keeps the others out of the diff.
    for name in sys.argv[1:] or SOUNDS:
        write(name, SOUNDS[name]())


if __name__ == '__main__':
    main()
