# Technology & Text to Speech

Computers, speakers, network gear and consumer electronics, a school's clocks, PA speakers and
bells, and a block that reads text aloud. It's the **CSM: Technology** tab, which holds the
**CSM: Technology** module and, when it is installed, the optional **CSM: Text to Speech**
module's two entries.

Most of the tab is decoration. The table below says what actually does something; everything
else just places, faces the way you put it, and looks the part. The full list is in the
[Technology block catalogue](../reference/technology.md).

## What works and what is for looks

| Block or item | What it does |
|---|---|
| **Apple iMac**, **iMac Pro**, **MacBook Pro** | Switch on and use a small desktop (below) |
| **Speakers**: Atlas, Bose, Altec Lansing, Bosch, Boston Acoustics, Polk Audio, FourJay, JBL Control, Valcom, the two School PA Speakers | Link to a Redstone TTS Module or a bell controller; play music on redstone |
| **Micaplex clocks** and **Clock/Speaker Panels** | Tell the world's time with moving hands |
| **Micaplex Bell Schedule Controller**, **School Hallway Bell**, **Bell System Linker** | Ring class periods by the clock |
| **Redstone TTS Module**, **TTS Speaker Linker** | Speak text on redstone (Text to Speech module) |
| **Apple Pencil** | Works as a weak shovel |
| Apple MacBook Air (Closed), Mac Studio, Mac Keyboard, Apple TV, Cable STB, Sat TV Dish, wireless access points, the Spectrum gateway, Micarolla ONT, NEMA Enclosure | Decorative |
| Phones, the iPad, the Apple Watch, the Rabbit R1 and the TV remotes | Decorative items. Their tooltip says so |

The desk devices (the closed MacBook Air, Mac Studio and keyboard) always sit flat and face you,
even when you place them from above. The **NEMA Enclosure** fits against the side of a pole.

## Computers

The iMac, iMac Pro and MacBook Pro have a screen you can turn on.

- **Right-click** a computer that is off to switch it on. Right-click again to open its desktop.
- **Sneak and right-click** to switch it on or off without opening anything.

A lit screen gives off a little light. The desktop, "CSM-OS", has a tab for each app: a
**Notepad** that keeps what you type in the computer, a **Calculator**, a **Weather** panel
showing your biome, the sky and the room temperature, **Minesweeper**, **Snake**, a joke
**Terminal**, and **About This PC**. Only the notepad is saved; the games start fresh each time.

## Speakers

Every speaker in the tab is a real speaker to the rest of the mod: a Redstone TTS Module can talk
through it, and a bell controller can ring through it. Unlinked, a speaker costs nothing.

Each speaker can also play music by itself. **Right-click** it with an empty hand to step through
its ambient sounds (there is one, the *Mii Channel Remix*, and *off*). It plays while the speaker
has **redstone power**, to anyone within 24 blocks.

## School clocks

| Clock | Where it goes |
|---|---|
| **Micaplex Classroom Clock** | Flat on a wall: black rim, white face, red sweep hand |
| **Micaplex Double-Dial Clock (Wall)** | On a bracket off a wall, a dial at each end looking along the corridor |
| **Micaplex Double-Dial Clock (Ceiling)** | On a stem from the ceiling, a dial each way across the corridor |
| **Micaplex Clock/Speaker Panel** | Two blocks wide: clock on the left, speaker grille on the right |
| **Micaplex Clock/Speaker Panel (Vertical)** | Two blocks tall: clock on top, grille below |

The hour and minute hands tell the **world's time**, the same time F3 shows: tick 0 is 6:00, and
a game hour is 1,000 ticks. If you turn the daylight cycle off, the clocks stop with the sun.

The red sweep hand goes round once a **real** minute. A second hand true to game time would spin
round faster than once a second, so it is there to look right, not to be read.

### The clock/speaker panels

A panel is two blocks, placed and broken as one, like a door. The second half goes to your right
(wide panel) or above (vertical panel), and if that space is taken the panel will not place.
Breaking either half breaks both and drops one panel.

The speaker half is an ordinary speaker: link it to a Redstone TTS Module or a bell controller
like any other. With the Bell System Linker you can click either half.

## The bell schedule

The **Micaplex Bell Schedule Controller** rings class changes at set times of day, through the
speakers and hallway bells you link to it.

### Setting the schedule

Right-click the controller to open its screen. It holds up to **12 bells**, six to a page.

| Column | What it sets |
|---|---|
| **Time** | 24-hour game time. `8:50`, `08:50`, `8.50`, `850` and `0850` all work. A time it can't read turns red, and Save waits until you fix it |
| **Tone** | Click to cycle: **Bell**, **Tone**, **Chime** or **Announce** |
| **Ring** | **On** or **Off**, to keep a bell in the list without ringing it |
| **Announcement (spoken)** | The words an Announce bell speaks, up to 120 characters |

**+ Add bell** adds one fifty minutes after the latest, and **x** removes one. **Ring now** plays
the tone beside it straight away, without saving, so you can hear it. The header shows the time
now and the next bell. Nothing changes in the world until you press **Save**.

Every game day is the same: Minecraft has no weekdays, so there is no weekend schedule.

### Linking speakers and bells

Hold the **Bell System Linker**:

1. **Right-click the controller** to select it. The linker remembers it.
2. **Right-click each speaker, clock/speaker panel or hallway bell** to link it. Click a linked one
   again to unlink it.
3. **Sneak and right-click the controller** to clear all its links.

A controller takes up to **64** links, each within **128 blocks** of it. A speaker can belong to a
bell controller and a Redstone TTS Module at the same time.

### What happens when a bell rings

| Tone | Speakers play | Hallway bells |
|---|---|---|
| **Bell** | An electric school bell | Ring |
| **Tone** | Three warm beeps | Ring |
| **Chime** | A three-note chime | Stay silent |
| **Announce** | The chime, then the announcement spoken | Stay silent |

You hear each bell **once**, from the nearest linked speaker within 24 blocks (the controller
counts as one), and the nearest hallway bell within 40 blocks, not forty copies a few ticks apart.

The controller also gives a **one-second redstone pulse** each time a bell rings, to the blocks
around it and strongly into the block it hangs on, like a button. Use it to drive anything else
you want on the bell: a lamp, a door, a note block.

!!! info "When bells don't ring"

    A bell rings when the clock passes its minute. If you load a world after a bell's time, or
    jump the time with `/time set`, the bells you skipped stay silent. And like anything else, a
    controller in a chunk nobody has loaded does not run, so an empty school is a quiet one.

**Announcements** use the Text to Speech module's voice when it is installed, and your system's
narrator when it is not.

## The hallway bell

The **School Hallway Bell** is a red dome gong. Link it to a controller, or wire it straight to
redstone: it rings when powered, and rings again every three seconds while the power stays on.

## Redstone TTS Module

The **Redstone TTS Module** reads a line of text aloud when it gets a redstone signal. It is
dressed as a 1980s speech synthesiser.

It comes in the **CSM: Text to Speech** module, which **requires CSM: Technology**, but appears in
the Technology tab beside the speakers. Without the Text to Speech module it isn't there at all.

**Setting it up.** Right-click the module to open its screen. Type the text, pick a **Voice** (two
US and two British voices, female and male), and press **Close** to keep it or **Cancel** to throw
it away.

**Making it speak.** Give it redstone power. Each player within **32 blocks** hears it, at most once
every two seconds. The voice is not placed in the world: everyone in range hears it at the same
volume, set by the **Master** and **Voice/Speech** sliders in Minecraft's sound options.

**Speaking through speakers.** To reach further than 32 blocks, link speakers to it. Each linked
speaker carries the message to everyone within 32 blocks of *it*, and nobody hears it twice. Hold
the **TTS Speaker Linker**:

- **Right-click the TTS module** to select it.
- **Right-click each speaker** to link it. A speaker belongs to one module; linking it to another
  moves it.
- **Sneak and right-click a speaker** to unlink it, or **the module** to unlink all of its
  speakers.

The speech engine is large, so it loads the first time something needs it rather than when the
game starts. The first message after launching may take a moment.

### Other things that speak

The Text to Speech module's voice is also used by:

- the bell controller's **Announce** bells (above);
- the bus **departure boards** in Transit, which call out departures only when this module is
  installed. See [Transit: Departure boards](transit.md#departure-boards).

## Survival

Every Technology block is made in the CSM Fabricator at the tab's cost. The **Bell System Linker**
has a crafting recipe of its own. See [Survival & Crafting](survival-and-crafting.md).
