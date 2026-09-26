# Fire Alarm Systems

A working building fire alarm: initiating devices report to a control panel, the panel decides
what to do, and notification appliances sound together across the building.

## The pieces

| | |
|---|---|
| **Control panel** | The brain. Right-click for its front panel screen. Two makes: the Simplex 4100 style **Fire Alarm Control Panel**, and the **Edwards iO** panel in red or white |
| **Initiating devices** | Pull stations, smoke and heat detectors, sprinkler flow switches |
| **Notification appliances** | Horns, horn/strobes, bells, speakers, speaker/strobes, and beacons |

## Wiring it up

1. Place a **control panel**.
2. Place your **horns, bells and strobes** through the building.
3. Place **pull stations and detectors**.
4. With the **linker**, click the panel to select it, then click each appliance and each
   initiating device to link it to that panel. Devices can be re-linked freely — the linker tells
   you whether it linked, re-linked, or was already linked.

The linker remembers its selected panel, even after you put it away. Sneak-right-click the air to
clear the selection before you start on another building. Its tooltip shows which panel it holds.

The panel groups its appliances by the sound they play, so one channel drives every horn making
the same noise rather than each one shouting independently.

### Unlinking

**Sneak-click a device** with the linker to unlink it from the selected panel. Sneak-clicking a
device the panel does not have tells you it was not linked.

**Sneak-click the panel** itself to unlink every device of that panel's that has gone missing,
since a device that has been broken can no longer be clicked. The linker reports how many it
removed.

!!! tip "Panels heal their own index"

    A panel keeps a reverse index of what feeds it, and writes to it both when a linker is used and
    whenever a device activates the panel. So a world built before that existed repairs itself the
    first time a device goes off — nothing to migrate.

## The panel — "CSM 4100"

An amber-on-black front panel with the lamps and keys a real one has. The Edwards iO panel opens
the same screen under the name "CSM iO64" and works exactly the same way; its cabinet is taller,
rising into the block above.

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

### The panel's buzzer

Like a real panel, the panel sounds a buzzer of its own, heard close to the panel (about 12
blocks):

| When | Buzzer |
|---|---|
| In alarm, not yet acknowledged or silenced | The alarm tone, repeating |
| Otherwise, in trouble and not acknowledged | The trouble tone, repeating |
| Reset out of an alarm | One reset tone |

The Simplex-style panel uses Simplex tones and the Edwards iO panel uses Edwards tones.
**ACK** quiets the buzzer, for an alarm and for trouble alike.

### Trouble: missing devices

If a linked appliance or initiating device is broken or replaced by something else, the panel does
not forget it. It goes into **trouble**: the TROUBLE lamp lights, the trouble buzzer sounds, and
the display steps through each missing device every two seconds, with its kind and coordinates:

```
MISSING 2/3 INITIATING 142,7,-2297
```

ACK silences the buzzer, but the TROUBLE lamp stays lit while anything is still missing. The
trouble clears when the device is put back (the same kind of block at the same spot) or when you
unlink it with the linker (see [Unlinking](#unlinking)). A panel with nothing linked yet is not
in trouble, so a new panel does not beep before you have set it up, though its display says
NO APPLIANCES LINKED.

### More than one panel

Each panel keeps its own sounds and strobes. Two panels in alarm near each other both flash their
own strobes, and silencing or resetting one leaves the other sounding. Breaking a panel during an
alarm puts out its strobes and stops its sounds.

## Detectors

A detector watches the column beneath each position within **15 blocks**, running downward until it
reaches a floor — anything that blocks movement — so a detector on the ceiling of a room covers that
room rather than everything below it in the building.

### Detector with a strobe

The **Gentex 710CS-C** is a smoke detector with a strobe built in. You link it to a panel like any
other detector, and it reports to that panel like one. When the panel goes into alarm, its strobe
flashes with the panel's other strobes, whatever set the alarm off.

## Sound

Sound is spatially aware and follows the player:

- Appliances are grouped **by channel**, one per distinct sound, rather than one per block.
- Volume attenuates with distance from the appliances on that channel.
- Sound is server-driven and client-rendered, so what you hear matches where you are.

### Choosing a sound

Most appliances have **two** sound options at most. A few have a longer list, as long as the
real unit's: the Gentex Commander 3 family and the bells. On those, **sneak-click with an empty
hand** to step to the next sound; chat tells you which one it is now set to.

### Bells

Three working fire alarm bells ring with the panel's horns: the **Simplex 4090 Fire Alarm Bell**,
and the **System Sensor Fire Alarm Bell** in grey or red. Link them to a panel like any other
appliance. Sneak-click a bell with an empty hand to cycle the four patterns a real bell circuit
rings:

| Pattern | Rings |
|---|---|
| **Code 3** | Three rings and a pause, over and over |
| **Code 4-4** | A coded pattern of fours |
| **Continuous** | Without stopping |
| **March Time** | On and off, 120 times a minute |

## Redstone

The control panel reads redstone input, so an alarm can be triggered by anything in the world that
can produce a signal — a tripwire, a pressure plate, or a circuit of your own.

## Exit signage

Exit signs and emergency lighting have a tab of their own, Exits & Emergency Lighting. They are
not part of the panel system, and are listed in the
[Exits & Emergency Lighting reference](../reference/exits-and-emergency-lighting.md).

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
