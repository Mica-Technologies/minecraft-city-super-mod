# Crosswalks & APS

Pedestrian signals, push buttons, and the audible units that make a crossing usable without sight.

## Pedestrian signals

CSM has symbolic (hand/person) and worded (DON'T WALK / WALK) crosswalk heads, in 12-inch and
16-inch, on base or pole mounts.

They link to a controller exactly like a vehicle head — hold a signal linker, right-click the
controller, then right-click the crosswalk head. The controller then drives walk, flashing don't
walk and don't walk from the pedestrian timing in its configuration.

| Controller timing | Effect on the crossing |
|---|---|
| Pedestrian walk | How long WALK shows |
| Flashing don't walk | The clearance interval |
| Leading pedestrian interval | Starts WALK before the parallel vehicle green — see [Advanced Mode](advanced-mode-asc3.md#leading-pedestrian-interval-dly-grn) |

## Push buttons

Two families, modelled on the real units:

- **Campbell / PedSafety Guardian** — in yellow and black
- **Polara iN2** — in yellow and black

In **requestable** mode the button is what calls the crossing. In normal mode it registers a
pedestrian call for the next cycle.

## Audible pedestrian signals

The buttons are APS units: they make sound, and what they say depends on what the crossing is
showing.

| Crossing shows | The unit plays |
|---|---|
| Don't walk | A locate tone, once a second |
| Clearance (flashing don't walk) | The locate tone |
| Walk | The walk message |
| Off | Nothing |

**Tweeters** are separate speaker units that chirp only during walk.

### Sound schemes

Each unit has a selectable scheme, so a build can mix the voices you would actually hear in
different cities.

=== "Campbell / PedSafety"

    | Scheme | Walk announcement |
    |---|---|
    | 1 | Standard voice — "Walk sign is on" |
    | 2 | Standard voice — "Warning lights are flashing" |
    | 3 | Standard voice — "Yellow lights are flashing" |
    | 4 | Standard voice — walk sign on, all crossings |
    | 5 | Standard percussive — east–west |
    | 6 | Standard percussive — north–south |
    | 7 | Philadelphia voice — "Walk sign is on" |
    | 8 | Philadelphia voice — "Warning lights activated" |
    | 9 | Philadelphia voice — "Crossing lights activated" |
    | 10 | Philadelphia voice — walk sign on, all crossings |
    | 11 | Audio disabled |

=== "Polara iN2"

    The Polara units carry their own scheme list in the same style, selectable the same way.

### How the sound works

Sound is driven by the server and rendered on the client, so everyone standing at the crossing
hears it correctly from where they are:

- **Hearing range is 10 blocks**, with volume falling off linearly with distance.
- Each unit gets its **own channel**, keyed to its position, so two crossings at one intersection
  do not fight over a single sound slot.
- Sounds play **single-shot** and are re-sent on the next tick, which is what gives the natural gap
  between repeats rather than a seamless loop.

## Crosswalk signal rendering

The crosswalk heads are drawn by a renderer rather than a block model, which is what lets a large
family of appearances collapse into a small number of blocks. Body colour, visor, and the symbol
style are properties on the head rather than separate blocks — so changing how a crossing looks does
not mean replacing it.
