"""Synthesise the railroad crossing bell and write it as OGG Vorbis.

A crossing bell is a struck gong on a mechanical striker, about 3 strikes a second, each a
short inharmonic ring: a few partials that are not integer multiples of one another (which is
what makes it a bell and not a note), the higher ones dying faster. One second of strikes is
written, seamless at the loop point, and the fixture plays it once a second while active.

Writes modules/roads/src/main/resources/assets/csm/sounds/railroad_crossing_bell.ogg via ffmpeg
(the only OGG encoder on the machine; Pillow does not do audio). Run from the repo root:

    python dev-env-utils/scripts/gen_rail_crossing_sounds.py
"""

import os
import struct
import subprocess
import tempfile
import wave

import numpy as np

OUT = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm', 'sounds',
                   'railroad_crossing_bell.ogg')
RATE = 44100
STRIKES_PER_SECOND = 3
# Partials of a small gong, as (frequency multiple, relative level, decay seconds). The 1.0
# partial is the hum; the inharmonic ratios above it are what read as metal.
PARTIALS = [(1.00, 1.00, 0.30), (2.71, 0.55, 0.16), (4.07, 0.35, 0.10), (5.43, 0.18, 0.06)]
FUNDAMENTAL = 1180.0  # Hz
TARGET_RMS = 5200


def strike(length):
    t = np.arange(length) / RATE
    out = np.zeros(length)
    for ratio, level, decay in PARTIALS:
        out += level * np.sin(2 * np.pi * FUNDAMENTAL * ratio * t) * np.exp(-t / decay)
    # the striker's clank: a few ms of noise at the front
    n = int(RATE * 0.004)
    out[:n] += np.random.RandomState(7).uniform(-0.6, 0.6, n) * np.linspace(1, 0, n)
    return out


def main():
    period = RATE // STRIKES_PER_SECOND
    one_strike = strike(period)
    loop = np.tile(one_strike, STRIKES_PER_SECOND)
    loop = loop / np.max(np.abs(loop))
    rms = np.sqrt(np.mean(loop ** 2)) * 32767
    loop = loop * (TARGET_RMS / rms)
    loop = np.clip(loop, -1, 1)
    pcm = (loop * 32767).astype('<i2')

    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'bell.wav')
        with wave.open(wav, 'wb') as fh:
            fh.setnchannels(1)
            fh.setsampwidth(2)
            fh.setframerate(RATE)
            fh.writeframes(pcm.tobytes())
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        subprocess.check_call(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav,
                               '-c:a', 'libvorbis', '-q:a', '4', OUT])
    print('wrote %s  (%d strikes/s, rms %d)' % (OUT, STRIKES_PER_SECOND, int(TARGET_RMS)))


if __name__ == '__main__':
    main()
