"""Synthesise the CSM: Vehicles siren speaker's three tones and write them as OGG Vorbis.

An electronic siren drives a 100 W horn speaker with a square-ish wave whose pitch is swept by
the siren's controller; the horn rolls off the harsh top. Each tone is one sweep period, written
so it loops without a click: the period is a whole number of samples and the waveform's phase
is scaled to complete a whole number of cycles over it.

  wail   a slow rise and fall, 650 to 1500 Hz over 4.8 s
  yelp   the same range, fast: 0.32 s a sweep
  hi-lo  two tones, 960 and 770 Hz, 0.55 s each

Immersive Vehicles plays a pack sound named "<packID>:<name>" from assets/<packID>/sounds/
<name>.ogg, with no sounds.json, so the module's CSM sound enum and sounds.json have no part in
it. Writes modules/vehicles/src/main/resources/assets/csmvehicles/sounds/. Vorbis output is not
byte-stable, so there is no --check: listen, then commit. Run from the repo root:

    python dev-env-utils/scripts/gen_vehicle_sounds.py
"""

import os
import subprocess
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


def main():
    write('siren_wail', render(wail_profile(int(RATE * 4.8))))
    # several sweeps to a file, so the loop restarts less often than it sweeps
    write('siren_yelp', render(np.tile(yelp_profile(int(RATE * 0.32)), 8)))
    write('siren_hilo', render(np.tile(hilo_profile(int(RATE * 1.1)), 2)))


if __name__ == '__main__':
    main()
