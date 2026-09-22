# Fire Alarm Systems

A working building fire alarm: initiating devices report to a control panel, the panel decides
what to do, and notification appliances sound together across the building.

## The pieces

| | |
|---|---|
| **Control panel** | The brain. Right-click for its front panel screen |
| **Initiating devices** | Pull stations, smoke and heat detectors, sprinkler flow switches |
| **Notification appliances** | Horns, horn/strobes, speakers, speaker/strobes, and beacons |

## Wiring it up

1. Place a **control panel**.
2. Place your **horns and strobes** through the building.
3. Place **pull stations and detectors**.
4. **Link the initiating devices to the panel** with the linker. Devices can be re-linked freely —
   the linker tells you whether it linked, re-linked, or was already linked.

Notification appliances are picked up by the panel and grouped by the sound they play, so one
channel drives every horn making the same noise rather than each one shouting independently.

!!! tip "Panels heal their own index"

    A panel keeps a reverse index of what feeds it, and writes to it both when a linker is used and
    whenever a device activates the panel. So a world built before that existed repairs itself the
    first time a device goes off — nothing to migrate.

## The panel — "CSM 4100"

An amber-on-black front panel with the lamps and keys a real one has:

| Lamps | Keys |
|---|---|
| FIRE ALARM | **ACK** — acknowledge |
| SUPERVISORY | **SILENCE** — becomes RESOUND while silenced |
| TROUBLE | **RESET** |
| SIGNALS SILENCED | **DRILL** |
| AC POWER | **LAMP TEST** |

### First-alarm annunciation

When a device initiates, the panel records **where it was and what it was** and shows it:

```
FROM -212, 64, 89   Pull Station
```

Only the **first** device to report is kept while the alarm is active, which is what a real panel
does. A reset clears it. Alarms with no reporting device — a drill, a redstone trigger — simply have
no origin line.

The panel also stays in sync with alarms raised elsewhere, so an open screen tracks a pull station
somebody else just hit.

## Detectors

A detector watches the column beneath each position within **15 blocks**, running downward until it
reaches a floor — anything that blocks movement — so a detector on the ceiling of a room covers that
room rather than everything below it in the building.

## Sound

Sound is spatially aware and follows the player:

- Appliances are grouped **by channel**, one per distinct sound, rather than one per block.
- Volume attenuates with distance from the appliances on that channel.
- Sound is server-driven and client-rendered, so what you hear matches where you are.

### Choosing a sound

Most appliances carry their sound selection in block state, which leaves room for **two** options
alongside the facing. Appliances with more than two — the Gentex Commander 3 family — carry a small
tile entity instead so the list can be as long as the real unit's.

## Redstone

The control panel reads redstone input, so an alarm can be triggered by anything in the world that
can produce a signal — a tripwire, a pressure plate, or a circuit of your own.

## Exit signage

The Life Safety tab also carries exit signs and emergency lighting. They are not part of the
panel system, and are listed in the [Life Safety reference](../reference/life-safety.md).

### Traditional and specialty exit signs

Seven exit signs are set up after you place them, instead of coming in a block per combination:

| Sign | What it is |
|---|---|
| Traditional Exit Sign | The everyday square-cornered plastic sign, white or black |
| Traditional Exit Sign (Rounded) | The same with rounded corners |
| Exit Sign / Emergency Light Combo | A sign with two emergency lamp heads above its top corners |
| Die-Cast Exit Sign | A heavier cast metal sign: brushed aluminum, black or white |
| Vandal-Resistant Exit Sign | A sign inside a clear protective shield, for garages, stairwells and outdoors |
| Photoluminescent Exit Sign | A thin self-glowing panel that needs no power |
| Explosion-Proof Exit Sign | An industrial sign in a heavy cast frame with a conduit hub on top |

**Right-click a placed sign** to open its screen. Each row is one option; left-click a button for
the next choice and right-click for the previous one. The sign changes as you click:

- **Legend:** EXIT or SALIDA
- **Letters:** red or green
- **Housing:** the sign's color, which its mount matches
- **Arrow:** none, left, right or both. Arrows you have not picked still show faintly, molded
  into the housing, as on a real sign
- **Mount:** on the wall; hung from the ceiling; or sticking out from a wall at one end. A hung
  sign shows its legend on both sides
- **Emergency Heads:** none, square LED heads or round lamps, on the ends of a traditional sign

Placing a sign against a wall mounts it on the wall, facing out; placing it under a ceiling hangs
it from the ceiling. The creative tab has a few common setups ready to place, and a sign you pick
up or break keeps its setup.

!!! tip "Emergency heads and redstone"

    As with the emergency lights, redstone is the building's mains power. A sign with heads that
    loses its redstone signal is running on battery: its heads light up and throw light forward.
    The legend is lit either way. Signs without heads ignore redstone.
