#!/usr/bin/env python3
"""
gen_euro_button_sounds.py -- the European push button's sounds, from a field recording.

The recording (one mechanical click from a real European push button, 0.28 s, with street noise
under it) is not committed; pass its path. What this does with it:

  1. high-pass at about 1.2 kHz: the street noise is almost all below 300 Hz, the click is
     between 1 and 10 kHz (its ring is at 7.6-7.9 kHz, with 3.1 kHz and 8.6-9 kHz beside it)
  2. spectral subtraction: the noise left above the cut is measured from the recording's quiet
     tail, after the click has rung out, and taken off the click frame by frame
  3. the click alone is cut out (onset about 8 ms in, rung out by about 66 ms), faded in over
     1 ms, its tail faded to silence, and normalised

and writes three OGGs into the Roads module's sounds:

  euro_button_click       the cleaned click: the locator (once a second) and the press
  euro_button_rapid_tick  the walk: that click every 100 ms, the rate of polara_rapid_tick1
                          (measured: 16 ticks 100 ms apart), but with no pause between loops as
                          a European button has none: ten clicks in a file exactly 1.0 s long,
                          which the scheme replays every 20 game ticks, so the tick runs on
                          unbroken at 10 Hz

No --check: Vorbis output is not byte-stable. Listen, then commit the OGGs.

Usage:
    python gen_euro_button_sounds.py <recording.ogg>
"""
import os
import subprocess
import sys
import tempfile

import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOUNDS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                      'sounds')

HIGH_PASS_HZ = 900.0         # start of the roll-off; full level from 1.5 kHz
HIGH_PASS_ROLL_HZ = 600.0
CLICK_FROM_S = 0.006         # just before the onset
CLICK_TO_S = 0.072           # the click has rung out by here
NOISE_FROM_S = 0.090         # the quiet tail the noise profile is measured on
NOISE_TO_S = 0.270
SUBTRACT = 1.6               # how much of the noise profile to take off
FLOOR = 0.04                 # never take a bin below this fraction of itself (no musical noise)
PEAK = 0.8

TICK_EVERY_S = 0.100         # polara_rapid_tick1's rate: 16 ticks, 100 ms apart
TICKS = 10                   # one second of them...
WALK_LENGTH_S = 1.0          # ...in a file exactly one walk cycle (20 game ticks) long


def load(path):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-ac', '1', '-ar', str(SR),
                          '-f', 'f32le', '-'], capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def high_pass(x):
    spectrum = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    spectrum *= np.clip((freqs - HIGH_PASS_HZ) / HIGH_PASS_ROLL_HZ, 0, 1)
    return np.fft.irfft(spectrum, len(x))


def stft(x, n=512, hop=128):
    win = np.hanning(n)
    frames = [x[i:i + n] * win for i in range(0, len(x) - n + 1, hop)]
    return np.fft.rfft(frames, axis=1), win, hop


def istft(spec, win, hop, length):
    n = len(win)
    out = np.zeros(length)
    norm = np.zeros(length)
    for k, frame in enumerate(np.fft.irfft(spec, n, axis=1)):
        i = k * hop
        out[i:i + n] += frame * win
        norm[i:i + n] += win ** 2
    return out / np.maximum(norm, 1e-8)


def clean_click(x):
    x = high_pass(x)
    spec, win, hop = stft(x)
    mag, phase = np.abs(spec), np.angle(spec)
    first, last = int(NOISE_FROM_S * SR / hop), int(NOISE_TO_S * SR / hop)
    noise = mag[first:last].mean(axis=0)
    cleaned = np.maximum(mag - SUBTRACT * noise, FLOOR * mag)
    x = istft(cleaned * np.exp(1j * phase), win, hop, len(x))
    click = x[int(CLICK_FROM_S * SR):int(CLICK_TO_S * SR)].copy()
    fade_in = int(0.001 * SR)
    click[:fade_in] *= np.linspace(0, 1, fade_in)
    fade_out = int(0.020 * SR)
    click[-fade_out:] *= np.linspace(1, 0, fade_out) ** 2
    return click / np.abs(click).max() * PEAK


def write_ogg(name, samples):
    path = os.path.join(SOUNDS, name + '.ogg')
    with tempfile.NamedTemporaryFile(suffix='.f32', delete=False) as tmp:
        tmp.write(samples.astype(np.float32).tobytes())
    try:
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(SR), '-ac', '1',
                        '-i', tmp.name, '-c:a', 'libvorbis', '-q:a', '6', path], check=True)
    finally:
        os.unlink(tmp.name)
    print('wrote', path, '%.3f s' % (len(samples) / SR))


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    click = clean_click(load(sys.argv[1]))
    write_ogg('euro_button_click', click)
    walk = np.zeros(int(WALK_LENGTH_S * SR))
    for k in range(TICKS):
        at = int(k * TICK_EVERY_S * SR)
        walk[at:at + len(click)] += click
    write_ogg('euro_button_rapid_tick', walk)


if __name__ == '__main__':
    main()
