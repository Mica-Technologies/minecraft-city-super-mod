"""Synthesise the Furniture & Novelties module's furniture sounds and write them as OGG Vorbis.

The kitchen's cabinet doors, drawers and refrigerator door, and its appliances' beeps, timer ding,
toaster pop, blender whirr, coffee gurgle, dishwasher hum, kettle whistle and jar lid, and the
bathroom's toilet flush and shower spray, the laundry's washing machine, dryer and steam iron, the
office's copier and school locker door, the living room's doorbell chime and fireplace crackle,
the backyard's grill sizzle and trampoline boing, and the store's card terminal beep, checkout
scanner and cash drawer and the fitting room's curtain (the locker sounds and the card terminal's beep replace recordings of
unknown origin the mod used to ship), made here from filtered noise, decaying sines and envelopes,
never recorded or taken from a sound library. Each entry in SOUNDS names a function returning mono samples in -1..1 at RATE and the level
(RMS, of 32767) it is normalised to; the script writes modules/furnishings/src/main/resources/assets/csm/sounds/<name>.ogg through
ffmpeg and adds a "<name>" entry to that module's sounds.json if it has none. The event still
needs its constant in FurnishingsSounds; CsmSoundsTest fails the build until it has one.

    python dev-env-utils/scripts/gen_furniture_sounds.py            # write every sound
    python dev-env-utils/scripts/gen_furniture_sounds.py drawer_open

There is no --check: Vorbis output is not byte-stable across encoder builds. Re-run a sound after
changing its function, listen to it, and commit the OGG with the change. Borrows the writer from
gen_life_safety_sounds.py.
"""

import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_life_safety_sounds as ls  # noqa: E402

ls.ASSETS = os.path.join(ls.REPO, 'modules', 'furnishings', 'src', 'main', 'resources', 'assets',
                         'csm')
RATE = ls.RATE


def t_of(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def noise(seconds, seed):
    return np.random.RandomState(seed).uniform(-1, 1, int(RATE * seconds))


def band(x, lo, hi):
    """x band-passed between lo and hi Hz, by zeroing the spectrum outside the band with soft
    shoulders."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / RATE)
    gain = np.clip((f - lo * 0.7) / (lo * 0.3 + 1e-9), 0, 1) * np.clip((hi * 1.3 - f) / (hi * 0.3),
                                                                          0, 1)
    return np.fft.irfft(spec * gain, len(x))


def knock(seconds, partials, decay, seed, click=0.4):
    """A struck wooden or steel panel: a few decaying partials and a short click of noise."""
    t = t_of(seconds)
    out = np.zeros_like(t)
    for freq, amp in partials:
        out += amp * np.sin(2 * np.pi * freq * t) * np.exp(-t * decay * (1 + freq / 2000.0))
    n = band(noise(seconds, seed), 800, 5000) * np.exp(-t * 180)
    out += click * n / max(1e-9, np.max(np.abs(n)))
    return out


def place(total, parts):
    """Mixes (start seconds, samples, gain) into a buffer total seconds long."""
    out = np.zeros(int(RATE * total))
    for start, x, g in parts:
        i = int(RATE * start)
        n = min(len(x), len(out) - i)
        out[i:i + n] += g * x[:n]
    return out


def slide(seconds, seed, rumble=34.0, lo=250, hi=2600):
    """A drawer running on its runners: band-passed noise, a roller rumble through it, swelling
    and fading."""
    t = t_of(seconds)
    n = band(noise(seconds, seed), lo, hi)
    n /= max(1e-9, np.max(np.abs(n)))
    roll = 0.6 + 0.4 * np.sin(2 * np.pi * rumble * t) ** 2
    env = np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 0.8
    return n * roll * env


# --------------------------------------------------------------------------------------------
# The sounds
# --------------------------------------------------------------------------------------------
def cabinet_open():
    """A cabinet door opening: the soft tock of the catch letting go, then the door's swish."""
    catch = knock(0.18, [(420, 1.0), (880, 0.4), (1650, 0.2)], 38, 11, click=0.6)
    swish = band(noise(0.3, 12), 300, 1500) * np.sin(np.pi * t_of(0.3) / 0.3) ** 2
    swish /= max(1e-9, np.max(np.abs(swish)))
    return place(0.45, [(0.0, catch, 1.0), (0.05, swish, 0.18)])


def cabinet_close():
    """A cabinet door closing: a short swish and a firm wooden knock on the frame."""
    swish = band(noise(0.15, 13), 300, 1500) * np.linspace(0, 1, int(RATE * 0.15)) ** 2
    swish /= max(1e-9, np.max(np.abs(swish)))
    thud = knock(0.3, [(170, 1.0), (340, 0.55), (610, 0.3), (1150, 0.12)], 26, 14, click=0.5)
    return place(0.45, [(0.0, swish, 0.2), (0.14, thud, 1.0)])


def drawer_open():
    """A drawer pulled out: its run on the runners, and the tap of it reaching its stop."""
    stop = knock(0.12, [(520, 1.0), (1040, 0.35)], 50, 15, click=0.5)
    return place(0.55, [(0.0, slide(0.42, 16), 0.55), (0.4, stop, 0.5)])


def drawer_close():
    """A drawer pushed shut: a quicker run and a wooden knock as the front meets the frame."""
    thud = knock(0.28, [(190, 1.0), (380, 0.5), (700, 0.25)], 28, 17, click=0.5)
    return place(0.5, [(0.0, slide(0.28, 18, rumble=46), 0.5), (0.25, thud, 1.0)])


def fridge_open():
    """A refrigerator door opening: the gasket's seal breaking with a soft low pop and a breath
    of air, and the faint rattle of what is on the door shelves."""
    t = t_of(0.12)
    pop = np.sin(2 * np.pi * 95 * t) * np.exp(-t * 40) * np.sin(np.pi * np.clip(t / 0.02, 0, 0.5))
    air = band(noise(0.5, 21), 200, 2200) * np.exp(-t_of(0.5) * 7)
    air /= max(1e-9, np.max(np.abs(air)))
    rattle = knock(0.08, [(2400, 1.0), (3700, 0.4)], 70, 22, click=0.1)
    return place(0.6, [(0.0, pop, 1.0), (0.02, air, 0.32), (0.16, rattle, 0.12),
                       (0.23, rattle, 0.08)])


def fridge_close():
    """A refrigerator door closing: a heavy, damped thump as the magnetic gasket pulls it home,
    with the bottles on the door shelves clinking."""
    thud = knock(0.4, [(70, 1.0), (140, 0.6), (260, 0.25), (520, 0.08)], 16, 23, click=0.25)
    puff = band(noise(0.12, 24), 150, 900) * np.exp(-t_of(0.12) * 35)
    puff /= max(1e-9, np.max(np.abs(puff)))
    clink = knock(0.1, [(2900, 1.0), (4300, 0.5)], 60, 25, click=0.05)
    return place(0.55, [(0.0, puff, 0.3), (0.03, thud, 1.0), (0.07, clink, 0.1),
                        (0.12, clink, 0.06)])


def tone(seconds, freq, harmonics=((1, 1.0),)):
    t = t_of(seconds)
    return sum(a * np.sin(2 * np.pi * freq * k * t) for k, a in harmonics)


def env_ad(n, attack, release):
    """An envelope n samples long rising over attack seconds and falling over release."""
    e = np.ones(n)
    a = max(1, int(RATE * attack))
    r = max(1, int(RATE * release))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r)
    return e


def appliance_beep():
    """A microwave's end-of-cycle beep: three short, soft-edged 2 kHz tones."""
    b = tone(0.13, 2050, ((1, 1.0), (3, 0.12)))
    b *= env_ad(len(b), 0.005, 0.02)
    return place(0.75, [(0.0, b, 1.0), (0.25, b, 1.0), (0.5, b, 1.0)])


def oven_timer():
    """An oven timer's ding: a small struck bell, two inharmonic partials ringing down."""
    t = t_of(1.2)
    x = (np.sin(2 * np.pi * 1760 * t) + 0.45 * np.sin(2 * np.pi * 4230 * t) * np.exp(-t * 3)
         + 0.2 * np.sin(2 * np.pi * 2650 * t)) * np.exp(-t * 3.2)
    return x * env_ad(len(t), 0.002, 0.05)


def toaster_pop():
    """The toaster letting go: the spring's twang and the carriage knocking up to its stop."""
    t = t_of(0.35)
    twang = np.sin(2 * np.pi * (900 - 500 * t / 0.35) * t) * np.exp(-t * 14)
    clack = knock(0.2, [(640, 1.0), (1500, 0.5), (2900, 0.2)], 45, 31, click=0.7)
    return place(0.45, [(0.0, twang, 0.4), (0.03, clack, 1.0)])


def blender_whirr():
    """A blender's motor: a buzzy fundamental and its harmonics under a rush of noise, spinning
    up, holding and running down again, so it loops without a click."""
    n = int(RATE * 1.5)
    t = t_of(1.5)
    f = 190 + 25 * np.clip(t / 0.2, 0, 1)
    ph = 2 * np.pi * np.cumsum(f) / RATE
    motor = sum(a * np.sin(k * ph) for k, a in ((1, 1.0), (2, 0.6), (3, 0.45), (5, 0.25),
                                                 (7, 0.15)))
    rush = band(noise(1.5, 32), 900, 5000)
    rush /= max(1e-9, np.max(np.abs(rush)))
    return (motor * 0.6 + rush * 0.35) * env_ad(n, 0.12, 0.2)


def coffee_gurgle():
    """A drip coffee machine: bubbles popping at random in the boiler over a steady hiss."""
    rng = np.random.RandomState(33)
    parts = []
    for _ in range(26):
        start = rng.uniform(0, 2.2)
        freq = rng.uniform(280, 900)
        t = t_of(0.08)
        pop = np.sin(2 * np.pi * freq * (1 + 2.5 * t) * t) * np.exp(-t * 60)
        parts.append((start, pop, rng.uniform(0.4, 1.0)))
    hiss = band(noise(2.4, 34), 2000, 7000)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    parts.append((0.0, hiss * env_ad(len(hiss), 0.3, 0.4), 0.18))
    return place(2.4, parts)


def dishwasher_hum():
    """A dishwasher washing: a low pump hum and water sloshing, swelling and easing so that it
    repeats every three seconds without a seam."""
    n = int(RATE * 3.0)
    t = t_of(3.0)
    hum = np.sin(2 * np.pi * 100 * t) + 0.4 * np.sin(2 * np.pi * 200 * t)
    slosh = band(noise(3.0, 35), 250, 1400)
    slosh /= max(1e-9, np.max(np.abs(slosh)))
    slosh *= 0.55 + 0.45 * np.sin(2 * np.pi * 0.66 * t) ** 2
    return (0.35 * hum + 0.65 * slosh) * env_ad(n, 0.4, 0.4)


def kettle_whistle():
    """A kettle coming to the boil: breathy noise growing into a whistle near 2.3 kHz with a
    slight warble, then cut off as it is lifted."""
    n = int(RATE * 1.8)
    t = t_of(1.8)
    f = 2250 + 60 * np.clip(t / 1.0, 0, 1) + 15 * np.sin(2 * np.pi * 5.5 * t)
    whistle = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.clip((t - 0.25) / 0.6, 0, 1)
    breath = band(noise(1.8, 36), 1500, 4500)
    breath /= max(1e-9, np.max(np.abs(breath)))
    return (0.7 * whistle + 0.3 * breath) * env_ad(n, 0.2, 0.15)


def jar_lid():
    """A ceramic lid lifted from its jar: a bright clink and a small ring."""
    return place(0.3, [(0.0, knock(0.3, [(1900, 1.0), (3300, 0.5), (5200, 0.25)], 18, 37,
                                   click=0.3), 1.0)])


def toilet_flush():
    """A toilet flushing: the lever's clunk, the cistern emptying into the bowl in a rush that
    swirls and gurgles away, and the quiet hiss of the cistern refilling."""
    total = 3.6
    n = int(RATE * total)
    t = t_of(total)
    lever = knock(0.25, [(240, 1.0), (520, 0.5), (1100, 0.2)], 30, 41, click=0.5)
    rush = band(noise(total, 42), 180, 3200)
    rush /= max(1e-9, np.max(np.abs(rush)))
    swell = np.clip((t - 0.08) / 0.3, 0, 1) * np.clip((2.4 - t) / 0.9, 0, 1) ** 1.5
    swirl = 0.7 + 0.3 * np.sin(2 * np.pi * (5.0 + 2.0 * t) * t)
    gurgle = np.zeros(n)
    rng = np.random.RandomState(43)
    for _ in range(18):
        start = rng.uniform(1.3, 2.5)
        f = rng.uniform(160, 420)
        tt = t_of(0.09)
        g = np.sin(2 * np.pi * f * (1 + 3 * tt) * tt) * np.exp(-tt * 40)
        i = int(RATE * start)
        m = min(len(g), n - i)
        gurgle[i:i + m] += g[:m] * rng.uniform(0.4, 1.0)
    refill = band(noise(total, 44), 1800, 6500)
    refill /= max(1e-9, np.max(np.abs(refill)))
    refill *= np.clip((t - 1.9) / 0.5, 0, 1) * np.clip((total - t) / 0.4, 0, 1)
    body = 0.85 * rush * swell * swirl + 0.35 * gurgle + 0.12 * refill
    return place(total, [(0.0, lever, 0.6), (0.0, body, 1.0)])


def shower_spray():
    """A shower running: a broad hiss of water on a tray with the crackle of drops through it,
    level from end to end, two seconds and a little over so that it is played again every
    forty ticks without a gap (the ends fade over a twentieth of a second)."""
    total = 2.1
    n = int(RATE * total)
    hiss = band(noise(total, 45), 700, 9000)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    rng = np.random.RandomState(46)
    drops = np.zeros(n)
    for _ in range(260):
        i = rng.randint(0, n - 200)
        tt = t_of(0.004)
        drops[i:i + len(tt)] += np.sin(2 * np.pi * rng.uniform(1800, 4200) * tt)             * np.exp(-tt * 900) * rng.uniform(0.3, 1.0)
    return (0.8 * hiss + 0.5 * drops) * env_ad(n, 0.05, 0.05)


def washing_machine_run():
    """A front loader washing: the motor's hum, and the water and clothes sloshing each time the
    drum turns over, swelling and easing so that it repeats every three seconds."""
    total = 3.0
    n = int(RATE * total)
    t = t_of(total)
    hum = np.sin(2 * np.pi * 50 * t) + 0.5 * np.sin(2 * np.pi * 100 * t)         + 0.2 * np.sin(2 * np.pi * 150 * t)
    slosh = band(noise(total, 47), 150, 1100)
    slosh /= max(1e-9, np.max(np.abs(slosh)))
    slosh *= np.sin(np.pi * ((t * 1.0) % 1.0)) ** 2
    return (0.3 * hum + 0.7 * slosh) * env_ad(n, 0.35, 0.35)


def dryer_tumble():
    """A tumble dryer: a steady motor and fan, and the soft thump of the clothes dropping as the
    drum turns, with the odd zip or button ticking against the drum."""
    total = 3.0
    n = int(RATE * total)
    t = t_of(total)
    motor = np.sin(2 * np.pi * 60 * t) + 0.4 * np.sin(2 * np.pi * 120 * t)
    fan = band(noise(total, 48), 300, 2500)
    fan /= max(1e-9, np.max(np.abs(fan)))
    parts = [(0.0, 0.25 * motor + 0.25 * fan, 1.0)]
    for k, start in enumerate(np.arange(0.25, total - 0.3, 0.62)):
        thud = knock(0.3, [(70, 1.0), (140, 0.5), (300, 0.15)], 22, 49 + k, click=0.15)
        parts.append((start, thud, 0.9))
    rng = np.random.RandomState(60)
    for k in range(5):
        tick = knock(0.05, [(2600, 1.0), (4100, 0.4)], 80, 61 + k, click=0.2)
        parts.append((rng.uniform(0.2, total - 0.3), tick, 0.25))
    return place(total, parts) * env_ad(n, 0.3, 0.3)


def iron_steam():
    """A steam iron's shot of steam: a sharp hiss that rises in a breath and dies away."""
    total = 0.9
    t = t_of(total)
    hiss = band(noise(total, 66), 2200, 9500)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    return hiss * np.clip(t / 0.04, 0, 1) * np.exp(-np.clip(t - 0.12, 0, None) * 5.5)


def locker_door_open():
    """A steel locker door opening: the latch lifting with a hard click, then the thin door
    swinging free with a tinny rattle of its louvres."""
    latch = knock(0.12, [(1300, 1.0), (2600, 0.5), (3900, 0.25)], 60, 71, click=0.7)
    rattle = knock(0.35, [(620, 1.0), (1480, 0.6), (2310, 0.35), (3350, 0.2)], 14, 72, click=0.2)
    squeak = np.sin(2 * np.pi * (1800 + 500 * t_of(0.25)) * t_of(0.25))         * np.sin(np.pi * t_of(0.25) / 0.25) ** 2
    return place(0.6, [(0.0, latch, 1.0), (0.07, rattle, 0.45), (0.12, squeak, 0.05)])


def locker_door_close():
    """A steel locker door slammed shut: a bright clang of thin sheet steel ringing, and the
    latch snapping home."""
    clang = knock(0.6, [(410, 1.0), (980, 0.8), (1720, 0.55), (2650, 0.35), (3900, 0.2)], 9, 73,
                  click=0.6)
    snap = knock(0.08, [(2200, 1.0), (3300, 0.4)], 80, 74, click=0.6)
    return place(0.7, [(0.0, clang, 1.0), (0.05, snap, 0.5)])


def printer_run():
    """A copier making a copy: the drive motor's whine, the paper rushing through the rollers,
    and the scanner carriage's clack at each end of its travel, over two seconds so that it
    repeats as the copier works."""
    total = 2.0
    n = int(RATE * total)
    t = t_of(total)
    motor = (np.sin(2 * np.pi * 220 * t) + 0.5 * np.sin(2 * np.pi * 440 * t)
             + 0.25 * np.sin(2 * np.pi * 660 * t)) * (0.85 + 0.15 * np.sin(2 * np.pi * 6 * t))
    feed = band(noise(total, 75), 700, 5200)
    feed /= max(1e-9, np.max(np.abs(feed)))
    feed *= np.clip(np.sin(np.pi * np.clip((t - 0.45) / 1.1, 0, 1)), 0, 1) ** 1.5
    parts = [(0.0, 0.18 * motor + 0.5 * feed, 1.0)]
    for k, start in enumerate((0.1, 0.9, 1.7)):
        parts.append((start, knock(0.1, [(900, 1.0), (1900, 0.5)], 55, 76 + k, click=0.6), 0.5))
    return place(total, parts) * env_ad(n, 0.08, 0.12)


def chime_bar(seconds, freq, seed):
    """One tone bar of a door chime struck by its plunger: a warm fundamental, its octave and a
    faint inharmonic partial ringing down, and the felt tip's soft thud."""
    t = t_of(seconds)
    x = (np.sin(2 * np.pi * freq * t) + 0.32 * np.sin(2 * np.pi * freq * 2.0 * t)
         * np.exp(-t * 2.5) + 0.12 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t * 6))
    x *= np.exp(-t * 1.9)
    thud = band(noise(seconds, seed), 150, 900) * np.exp(-t * 90)
    x += 0.25 * thud / max(1e-9, np.max(np.abs(thud)))
    return x * env_ad(len(t), 0.002, 0.08)


def doorbell_chime():
    """A two-note door chime: the plunger strikes the high bar as the button goes in, and the
    low bar as it springs back, a major third below -- ding, dong."""
    return place(2.3, [(0.0, chime_bar(1.7, 659.3, 81), 1.0),
                       (0.55, chime_bar(1.75, 523.3, 82), 1.0)])


def fireplace_crackle():
    """A log fire, two and a half seconds of it: a low soft roar of burning, and wood cracking
    and popping in it at random -- tiny ticks, a few sharper snaps, and a hiss of sap."""
    total = 2.5
    n = int(RATE * total)
    t = t_of(total)
    rng = np.random.RandomState(83)
    roar = band(noise(total, 84), 70, 520)
    roar /= max(1e-9, np.max(np.abs(roar)))
    roar *= 0.8 + 0.2 * np.sin(2 * np.pi * 0.7 * t + 1.3)
    parts = [(0.0, 0.22 * roar, 1.0)]
    for k in range(46):
        start = rng.uniform(0.0, total - 0.05)
        big = rng.rand() < 0.18
        length = 0.05 if big else 0.012
        tt = t_of(length)
        snap = band(noise(length, 100 + k), 900 if big else 1800, 6000)
        snap = snap / max(1e-9, np.max(np.abs(snap))) * np.exp(-tt * (70 if big else 380))
        parts.append((start, snap, rng.uniform(0.5, 1.0) if big else rng.uniform(0.15, 0.45)))
    hiss = band(noise(0.5, 99), 3000, 8000)
    hiss = hiss / max(1e-9, np.max(np.abs(hiss))) * np.sin(np.pi * np.clip(t_of(0.5) / 0.5, 0, 1))
    parts.append((1.3, hiss, 0.12))
    return place(total, parts) * env_ad(n, 0.25, 0.3)


def grill_sizzle():
    """Food on a hot grill, two seconds of it: a bright, crackly hiss of fat spitting, loudest
    in the top octaves, with tiny pops scattered through it and a soft low roar of the burners
    under it; it loops, so it swells in and out without a start or an end."""
    total = 2.0
    n = int(RATE * total)
    t = t_of(total)
    rng = np.random.RandomState(91)
    hiss = band(noise(total, 92), 2500, 9000)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    # The hiss flutters as the fat spits.
    flutter = 0.65 + 0.35 * np.abs(np.sin(2 * np.pi * 3.1 * t + 0.4)
                                   * np.sin(2 * np.pi * 7.3 * t + 1.1))
    roar = band(noise(total, 93), 90, 400)
    roar /= max(1e-9, np.max(np.abs(roar)))
    parts = [(0.0, 0.55 * hiss * flutter, 1.0), (0.0, 0.12 * roar, 1.0)]
    for k in range(70):
        start = rng.uniform(0.0, total - 0.02)
        length = 0.008
        tt = t_of(length)
        pop = band(noise(length, 200 + k), 2000, 8000)
        pop = pop / max(1e-9, np.max(np.abs(pop))) * np.exp(-tt * 500)
        parts.append((start, pop, rng.uniform(0.2, 0.6)))
    return place(total, parts) * env_ad(n, 0.15, 0.15)


def trampoline_boing():
    """A trampoline's springs as someone lands: a soft low thump on the mat and the twang of
    the springs, a tone that bends up and rings down, quietly."""
    total = 0.7
    t = t_of(total)
    # The springs: a slightly inharmonic tone that swoops up from 150 to 330 Hz and rings down.
    f = 150 + 180 * (1 - np.exp(-t * 18))
    phase = 2 * np.pi * np.cumsum(f) / RATE
    spring = (np.sin(phase) + 0.35 * np.sin(2.01 * phase) + 0.15 * np.sin(3.03 * phase))
    spring *= np.exp(-t * 6.5) * (1 + 0.25 * np.sin(2 * np.pi * 11 * t))
    tt = t_of(0.12)
    thump = np.sin(2 * np.pi * 70 * tt) * np.exp(-tt * 35)
    thump += 0.5 * band(noise(0.12, 94), 100, 900) / 3.0 * np.exp(-tt * 60)
    return place(total, [(0.0, 0.7 * spring, 1.0), (0.0, thump, 0.9)])


def piezo(seconds, freq):
    """A piezo buzzer's tone: a square-ish wave (odd harmonics) with soft edges, as a card
    terminal or a scanner sounds."""
    x = tone(seconds, freq, ((1, 1.0), (3, 0.28), (5, 0.1)))
    return x * env_ad(len(x), 0.004, 0.018)


def verifone_mx915():
    """A card terminal approving a payment: two short piezo beeps, the second a fourth higher
    and a little longer."""
    return place(0.42, [(0.0, piezo(0.09, 2350), 1.0), (0.14, piezo(0.16, 3130), 1.0)])


def scanner_beep():
    """A checkout scanner reading a barcode: one clean, short beep near 1.9 kHz, softer-edged than
    a card terminal's."""
    b = tone(0.12, 1880, ((1, 1.0), (2, 0.08), (3, 0.05)))
    return place(0.2, [(0.0, b * env_ad(len(b), 0.006, 0.04), 1.0)])


def register_drawer():
    """A cash register opening: the key's clack, the bell struck as the drawer releases -- the
    ka-ching -- ringing down, and the drawer running out on its rollers to its stop."""
    t = t_of(1.1)
    bell = (np.sin(2 * np.pi * 2780 * t) + 0.5 * np.sin(2 * np.pi * 5620 * t) * np.exp(-t * 4)
            + 0.3 * np.sin(2 * np.pi * 4130 * t) * np.exp(-t * 2)) * np.exp(-t * 3.4)
    bell *= env_ad(len(t), 0.002, 0.08)
    key = knock(0.08, [(1500, 1.0), (3100, 0.4)], 70, 131, click=0.7)
    run = slide(0.26, 132, rumble=58, lo=400, hi=3200)
    stop = knock(0.2, [(210, 1.0), (430, 0.5), (900, 0.2)], 30, 133, click=0.5)
    return place(1.2, [(0.0, key, 0.6), (0.05, bell, 0.8), (0.1, run, 0.45), (0.36, stop, 0.9)])


def curtain_slide():
    """A fitting room's curtain drawn along its rod: the rings rattling and scraping on the steel
    rod in a quick run of small metallic ticks, over the soft swish of the cloth, and the last
    ring knocking against its neighbours at the end."""
    total = 0.75
    rng = np.random.RandomState(151)
    parts = []
    t = 0.02
    while t < 0.5:
        tick = knock(0.05, [(3100 + rng.uniform(-400, 400), 1.0), (5200, 0.4)], 90, 152,
                     click=0.6)
        parts.append((t, tick, 0.25 + 0.2 * rng.uniform()))
        t += rng.uniform(0.018, 0.04)
    swish = band(noise(0.55, 153), 500, 4200)
    swish /= max(1e-9, np.max(np.abs(swish)))
    swish *= np.sin(np.pi * np.clip(t_of(0.55) / 0.55, 0, 1)) ** 1.2
    end = knock(0.12, [(2600, 1.0), (4100, 0.5)], 45, 154, click=0.5)
    return place(total, parts + [(0.0, swish, 0.55), (0.52, end, 0.6)])


SOUNDS = {
    'cabinet_open': (cabinet_open, 2600),
    'cabinet_close': (cabinet_close, 3600),
    'drawer_open': (drawer_open, 3000),
    'drawer_close': (drawer_close, 3600),
    'fridge_open': (fridge_open, 3000),
    'fridge_close': (fridge_close, 4200),
    'appliance_beep': (appliance_beep, 2600),
    'oven_timer': (oven_timer, 2600),
    'toaster_pop': (toaster_pop, 3400),
    'blender_whirr': (blender_whirr, 2800),
    'coffee_gurgle': (coffee_gurgle, 2000),
    'dishwasher_hum': (dishwasher_hum, 2000),
    'kettle_whistle': (kettle_whistle, 2400),
    'jar_lid': (jar_lid, 2400),
    'toilet_flush': (toilet_flush, 3000),
    'shower_spray': (shower_spray, 2200),
    'washing_machine_run': (washing_machine_run, 2200),
    'dryer_tumble': (dryer_tumble, 2200),
    'iron_steam': (iron_steam, 2600),
    'locker_door_open': (locker_door_open, 3000),
    'locker_door_close': (locker_door_close, 3600),
    'printer_run': (printer_run, 2200),
    'doorbell_chime': (doorbell_chime, 3200),
    'fireplace_crackle': (fireplace_crackle, 1800),
    'grill_sizzle': (grill_sizzle, 1600),
    'trampoline_boing': (trampoline_boing, 2600),
    'verifone_mx915': (verifone_mx915, 2600),
    'scanner_beep': (scanner_beep, 2400),
    'register_drawer': (register_drawer, 3000),
    'curtain_slide': (curtain_slide, 2600),
}


def main(argv):
    names = argv or list(SOUNDS)
    for n in names:
        if n not in SOUNDS:
            sys.exit('no such sound: %s (have: %s)' % (n, ', '.join(SOUNDS)))
    for n in names:
        fn, rms = SOUNDS[n]
        ls.write_ogg(n, fn(), rms)
    ls.ensure_sounds_json(names)


if __name__ == '__main__':
    main(sys.argv[1:])
