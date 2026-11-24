"""Synthesise the CSM: Vehicles siren speaker's three tones and write them as OGG Vorbis.

An electronic siren drives a 100 W horn speaker with a square-ish wave whose pitch is swept by
the siren's controller; the horn rolls off the harsh top. Each tone is one sweep period, written
so it loops without a click: the period is a whole number of samples and the waveform's phase
is scaled to complete a whole number of cycles over it.

  wail   a slow rise and fall, 650 to 1500 Hz over 4.8 s
  yelp   the same range, fast: 0.32 s a sweep
  hi-lo  two tones, 960 and 770 Hz, 0.55 s each

and the tree crew chip bed's chipper: chipper_run (a diesel and the drum idling, looped) and
chipper_chip (one log through the knives); and the fleet's engines and horns: a diesel's and a
V8's idle (looped; Immersive Vehicles raises the pitch with the revs), cranking (looped) and
start, an air horn and a car horn (looped while the horn is held); and the doors: door_open
(the latch letting go), door_close (the thump and the latch catching) and rollup (a fire
compartment's roll-up door), played once at the ends of a door's swing; and the reversing
alarms, backup_whitenoise (broadband bursts) and backup_beeper (the classic beep), both
recreated from measurements of real recordings (pattern, pitch, spectrum), not cut from them.

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


def engine_idle(firing_hz, seconds, harmonics, rough, seed):
    """An engine idling: its firing pulses and their harmonics, a little roughness. IV raises the
    pitch with the revs, so one loop serves the whole range. Whole cycles over the loop."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for k, level in harmonics:
        f = round(firing_hz * k * seconds) / seconds
        out += level * np.sin(2 * np.pi * f * t)
    rng = np.random.RandomState(seed)
    noise = np.convolve(rng.normal(0, 1, n + 16), np.ones(16) / 16, mode='same')[:n]
    # roughness that loops: modulate the noise by a whole number of firing cycles
    beat = 0.5 + 0.5 * np.sin(2 * np.pi * round(firing_hz * seconds) / seconds * t)
    return out + rough * noise * beat


def cranking(rate_hz, seconds, pitch, seed):
    """A starter motor turning an engine over: a whine, chugging at the compression strokes."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    whine = 0.4 * np.sin(2 * np.pi * (round(pitch * seconds) / seconds) * t)
    chug = 0.6 + 0.4 * np.sin(2 * np.pi * (round(rate_hz * seconds) / seconds) * t)
    rng = np.random.RandomState(seed)
    noise = np.convolve(rng.normal(0, 1, n + 8), np.ones(8) / 8, mode='same')[:n] * 0.5
    return (whine + noise) * chug


def starting(crank, idle, crank_seconds):
    """Cranking that catches and settles into the idle."""
    c = crank[:int(RATE * crank_seconds)]
    fade = np.linspace(0, 1, int(RATE * 0.35))
    i = idle[:int(RATE * 0.9)].copy()
    i[:len(fade)] *= fade
    c[-len(fade):] *= fade[::-1]
    roar = np.concatenate([c, i])
    roar[-2000:] *= np.linspace(1, 0, 2000)
    return roar


def horn(freqs, seconds):
    """A horn: a chord of buzzy tones (odd and even harmonics), looped."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for f in freqs:
        f = round(f * seconds) / seconds
        for k in range(1, 7):
            out += np.sin(2 * np.pi * f * k * t) / k ** 0.9
    return out


def latch(t, at, rng, level=1.0):
    """A latch's click at `at` seconds: a short, bright tick of noise and a ring."""
    s = t - at
    on = s >= 0
    s = np.where(on, s, 0)
    tick = rng.normal(0, 1, len(t)) * np.exp(-s / 0.004)
    ring = np.sin(2 * np.pi * 2300 * s) * np.exp(-s / 0.012) * 0.5
    return np.where(on, (tick + ring) * level, 0)


def door_open(seconds=0.45):
    """A car door opening: the handle's latch letting go, then the seal's soft peel."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    rng = np.random.RandomState(41)
    peel = rng.normal(0, 1, n) * np.exp(-((t - 0.09) / 0.06) ** 2) * 0.18
    kernel = np.ones(20) / 20
    peel = np.convolve(peel, kernel, mode='same')
    out = latch(t, 0.01, rng) + peel * 6
    out[-400:] *= np.linspace(1, 0, 400)
    return out


def door_close(seconds=0.5):
    """A car door shutting: a low, padded thump as it meets the seals, the latch catching."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    rng = np.random.RandomState(42)
    thump = (np.sin(2 * np.pi * 62 * t) + 0.5 * np.sin(2 * np.pi * 118 * t)) * np.exp(-t / 0.07)
    body = rng.normal(0, 1, n) * np.exp(-t / 0.025) * 0.4
    body = np.convolve(body, np.ones(8) / 8, mode='same')
    out = thump * 1.6 + body + latch(t, 0.012, rng, 0.5)
    out[-400:] *= np.linspace(1, 0, 400)
    return out


def rollup(seconds=0.9):
    """A roll-up compartment door: the slats rattling over the drum, about 30 a second, and
    the bar's clunk at the end of its travel."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    rng = np.random.RandomState(43)
    out = np.zeros(n)
    k = 0
    for at in np.arange(0.02, seconds - 0.15, 1 / 30.0):
        s = t - at
        on = s >= 0
        out += np.where(on, rng.normal(0, 1, n) * np.exp(-np.where(on, s, 0) / 0.006), 0) * (
            0.55 + 0.25 * ((k % 3) == 0))
        k += 1
    stop = seconds - 0.14
    s = np.clip(t - stop, 0, None)
    out += np.where(t >= stop, np.sin(2 * np.pi * 90 * s) * np.exp(-s / 0.04) * 2.0, 0)
    out[-400:] *= np.linspace(1, 0, 400)
    return out


def gate(n, period, on, attack=0.02, release=0.03):
    """An on/off envelope over n samples: on for `on` of every `period` seconds, ramped in and
    out, starting on and ending off, so it loops clean."""
    t = (np.arange(n) / RATE) % period
    up = np.clip(t / attack, 0, 1)
    down = np.clip((on - t) / release, 0, 1)
    return np.where(t < on, np.minimum(up, down), 0.0)


# The white noise ("broadband") reversing alarm, after a recording of a real one: bursts 0.38 s
# long every 0.745 s, noise loudest from 1 to 1.6 kHz with a second rise about 3 to 4 kHz,
# falling away under 800 Hz and over 6 kHz (third-octave levels, dB, measured off the bursts).
WHITENOISE_BANDS = ((250, 6), (315, 5), (400, 6), (500, 11), (630, 10), (800, 15),
                    (1000, 25), (1250, 23), (1600, 22), (2000, 18), (2500, 15), (3150, 20),
                    (4000, 22), (5000, 14), (6300, 15), (8000, 9), (10000, 3), (12500, -4))


def backup_whitenoise(periods=4, period=0.745, on=0.38):
    """The white noise reversing alarm: band-shaped noise, gated to its bursts. Recreated from
    the measured pattern and spectrum, not cut from the recording."""
    n = int(round(period * RATE)) * periods
    rng = np.random.RandomState(51)
    spec = np.fft.rfft(rng.normal(0, 1, n))
    f = np.fft.rfftfreq(n, 1 / RATE)
    cs = np.log([c for c, _ in WHITENOISE_BANDS])
    gains = np.array([g for _, g in WHITENOISE_BANDS], dtype=float)
    shape = np.interp(np.log(np.maximum(f, 1.0)), cs, gains, left=gains[0] - 20,
                      right=gains[-1] - 20)
    noise = np.fft.irfft(spec * 10 ** (shape / 20), n)
    return noise * gate(n, period, on)


def backup_beeper(periods=2, period=1.03, on=0.54, pitch=1186.0):
    """The classic reversing beeper: a 1,186 Hz tone with its weak 2nd and 3rd harmonics
    (-33 and -40 dB), 0.54 s on every 1.03 s. Recreated from a recording's measurements; the
    recording's background noise has no part in it."""
    n = int(round(period * RATE)) * periods
    t = np.arange(n) / RATE
    f = round(pitch * n / RATE) * RATE / n          # whole cycles over the loop
    tone = (np.sin(2 * np.pi * f * t) + 10 ** (-33 / 20) * np.sin(2 * np.pi * 2 * f * t)
            + 10 ** (-40 / 20) * np.sin(2 * np.pi * 3 * f * t))
    return tone * gate(n, period, on, attack=0.012, release=0.02)


def diesel_idle():
    return engine_idle(26.0, 2.0, ((1, 1.0), (2, 0.6), (3, 0.45), (4, 0.2), (6, 0.12)), 0.35, 21)


def petrol_idle():
    return engine_idle(42.0, 2.0, ((1, 1.0), (2, 0.5), (4, 0.3), (8, 0.1)), 0.2, 22)


SOUNDS = {
    'siren_wail': lambda: render(wail_profile(int(RATE * 4.8))),
    # several sweeps to a file, so the loop restarts less often than it sweeps
    'siren_yelp': lambda: render(np.tile(yelp_profile(int(RATE * 0.32)), 8)),
    'siren_hilo': lambda: render(np.tile(hilo_profile(int(RATE * 1.1)), 2)),
    'chipper_run': chipper_run,
    'chipper_chip': chipper_chip,
    'diesel_idle': diesel_idle,
    'diesel_crank': lambda: cranking(4.0, 1.0, 180.0, 31),
    'diesel_start': lambda: starting(cranking(4.0, 1.5, 180.0, 31), diesel_idle(), 1.2),
    'petrol_idle': petrol_idle,
    'petrol_crank': lambda: cranking(6.0, 1.0, 240.0, 32),
    'petrol_start': lambda: starting(cranking(6.0, 1.0, 240.0, 32), petrol_idle(), 0.8),
    'horn_air': lambda: horn((233.0, 294.0, 349.0), 1.0),
    'horn_car': lambda: horn((415.0, 494.0), 1.0),
    'door_open': door_open,
    'door_close': door_close,
    'rollup': rollup,
    'backup_whitenoise': backup_whitenoise,
    'backup_beeper': backup_beeper,
}


def main():
    # Name the sounds to write, or none for all. Vorbis output differs run to run, so writing
    # only the ones that changed keeps the others out of the diff.
    for name in sys.argv[1:] or SOUNDS:
        write(name, SOUNDS[name]())


if __name__ == '__main__':
    main()
