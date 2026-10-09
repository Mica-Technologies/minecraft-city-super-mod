# Configuration

Every setting CSM reads from a file: where the files are, which ones a player edits and which ones
a server owner edits, and what each entry does.

## The files

| File | Written by | Holds |
|---|---|---|
| `config/csm.cfg` | Core | The general options, plus the parking meter and Parks tool settings, whichever modules you installed |
| `config/csm_signage.cfg` | CSM: Signage & Advertising | Whether a server offers its own ads, and whether a client takes them |
| `config/csm/ads/` | You | A folder of PNG images a server shows on the advertising boards (see [Advertising](../guides/advertising.md)) |

`config/` is the folder next to `mods/` in your game or server directory. Each file is written the
first time the game starts with the jar that owns it, with every entry at its default and a comment
above it. You only need to edit a file to change something. Delete it to go back to the defaults;
the next launch writes it again.

!!! note "Modules don't add their own files"
    The parking settings are used only by CSM: Roads & Traffic, and the chainsaw setting only by
    CSM: Parks & Greenery. Both still live in Core's `csm.cfg`, and Core writes them whether or not
    those modules are installed. An entry for a module you don't have does nothing.

### Reading the format

These are standard Forge configuration files. Each entry is a type letter, a name and a value:

```
general {
    # Set to false to disable the visual strobe flash effect on fire alarm strobe devices. [default: true]
    B:enableStrobeEffect=true
}
```

| Prefix | Type | Example |
|---|---|---|
| `B:` | true or false | `B:animateDoors=false` |
| `I:` | whole number, inside the range in its comment | `I:arrowBoardSpeedPercent=150` |
| `D:` | decimal number | `D:defaultMoneyPerBlock=2.5` |
| `S:` | text | `S:chainsawBrushPiles=MANY` |
| `S:name <` … `>` | a list, one entry a line | see [`trafficPoleIgnoreBlocks`](#trafficpoleignoreblocks) |

Change only the value after `=`. An `I:` value outside the range in its comment is treated as the
nearest end of that range. If a `B:` value is not `true` or `false`, the entry's default is used.

### Client, server, or both

In single player your game is the client and the server at once, so it reads one copy of each
file and every setting applies. On a multiplayer server there are two copies, the server's and
each player's, and **each setting is read on one side only**:

- A **client** setting changes what *you* see or hear. Set it in your own `config/`; the server's
  value is ignored, and other players keep their own choice.
- A **server** setting changes the game for everyone. Only the server's file counts, and a player
  editing their own copy changes nothing on that server.

The *Side* column in each table below tells you which copy to edit.

### Applying a change

| File | How to apply an edit |
|---|---|
| `csm.cfg` | Restart the game, or run [`/csm reloadconfig`](commands.md#csm-reloadconfig) (operator) |
| `csm_signage.cfg` | Restart. It is read once at startup. |
| `config/csm/ads/` | Restart the server |

`/csm reloadconfig` re-reads `csm.cfg` in the game that runs it. In single player that is your
whole game. On a dedicated server it is the server's copy only, so a player who changes a client
setting in their own file restarts their game to apply it.

## csm.cfg

### general

| Entry | Side | Default | What it does |
|---|---|---|---|
| [`enableStrobeEffect`](#enablestrobeeffect) | Client | `true` | The flash of strobes and flashing beacons |
| [`animateDoors`](#animatedoors) | Client | `true` | Doors swing, or snap |
| [`enableThermostatDisplay`](#enablethermostatdisplay) | Client | `true` | The live screen on HVAC thermostats |
| [`arrowBoardSpeedPercent`](#arrowboardspeedpercent) | Client | `100` | How fast arrow boards run their sequences |
| [`enableUpdateCheck`](#enableupdatecheck) | Client | `true` | A chat message when a newer release exists |

The performance mode and the memory settings have a category of their own: [performance](#performance). On `HIGH`, `MEDIUM` and `LOW` these switches can only turn their effect off; on `CUSTOM` they decide on their own.

#### `enableStrobeEffect`

`B:` · default `true` · **client**

When `false`, CSM stops drawing the bright flash and glow these devices put out:

- the flash of fire alarm strobes and horn-strobes in alarm
- the light cast by emergency lights
- the flash of traffic beacons and signal tattle-tale beacons
- the flashing lamps on barricades, which stay dark
- the halo around an arrow board's lit lamps (the arrows themselves still run)

Nothing else changes: an alarm still sounds and a panel still reports it, and a beacon or an arrow
board still runs.

Turn this off if flashing light bothers you, or to save a little frame time in a building full of
strobes in alarm.

#### `animateDoors`

`B:` · default `true` · **client**

When `false`, CSM's two-block doors and custom doors snap straight to open or shut instead of
swinging. Garage doors still roll. This
changes only how a door is drawn. It opens and shuts on the same click, and the server decides
when, so your setting never has to match anyone else's. See [Doors](../guides/doors.md).

#### `enableThermostatDisplay`

`B:` · default `true` · **client**

The live screen on HVAC thermostats, which shows the time, the room temperature and the outside
temperature. When `false`, a thermostat shows a blank screen in the world. Its control screen and
the HVAC system itself work exactly as before. See [HVAC](../guides/hvac.md).

#### `arrowBoardSpeedPercent`

`I:` · default `100` · range `10`–`400` · **client**

How fast work zone arrow boards run their sequences, as a percentage of the standard rate. A
bigger number is a faster board: `50` runs every sequence at half speed and `200` at double. Every
stage of every pattern is scaled by the same amount, so the shape of a sequence stays the same.

This sets the speed you see, so players on one server can each pick their own.

#### `enableUpdateCheck`

`B:` · default `true` · **client**

When you join a world, CSM asks GitHub for the latest release. If yours is older, you get a chat
message with a **[Download]** link to the releases page. Set this to `false` to stop the request,
for example in a modpack that manages its own versions or on a machine with no internet access.

### performance

**Client.** How much CSM draws, and how much memory it lets Minecraft keep. Each player's own
setting applies, on any server. What each level does, and why, is in
[Performance & Graphics](performance-and-graphics.md#performance-mode).

#### `performanceMode`

`S:` · default `HIGH` · values `HIGH`, `MEDIUM`, `LOW`, `CUSTOM` · **client**

`HIGH` is how CSM has always looked. `MEDIUM` and `LOW` draw less and keep less memory. `CUSTOM`
takes each value from the entries marked *CUSTOM only* below, which the other three levels ignore.
Change it in game with [`/csmclient performance`](commands.md#csmclient); the command saves it here.

The switches `enableStrobeEffect`, `animateDoors`, `enableThermostatDisplay` and `trimChunkBuilders`
still count. On `HIGH`, `MEDIUM` and `LOW` they can only turn a thing off; on `CUSTOM` they decide on
their own.

| Entry | Type | Default | Range | What it does |
|---|---|---|---|---|
| `trimChunkBuilders` | `B:` | `true` | | Gives back the memory the chunk builders grew, largest first, whenever they hold more than their share of the direct memory limit. Below that it does nothing. A switch. |
| `maxRenderDistance` | `I:` | `0` | 0–512 | *CUSTOM only.* The farthest, in blocks, that CSM's animated blocks are drawn. `0` keeps each block's own distance, mostly 128. |
| `signDetailDistance` | `I:` | `64` | 8–256 | *CUSTOM only.* Within this many blocks guide and street signs draw their legends; farther away, only the blank sign |
| `arrowBoardHaloDistance` | `I:` | `48` | 0–128 | *CUSTOM only.* Within this many blocks an arrow board's lit lamps glow; `0` turns the glow off |
| `strobeDetail` | `S:` | `FULL` | `FULL`, `CONE`, `LENS` | *CUSTOM only.* How much of a fire alarm strobe's flash is drawn: the lens, its beam and the light it throws on walls and floors; no light on surfaces; or the lens alone |
| `emergencyLightGlow` | `B:` | `true` | | *CUSTOM only.* Whether lit emergency lights cast their glow |
| `thermostatDisplayDistance` | `I:` | `0` | 0–256 | *CUSTOM only.* Within this many blocks thermostats show their live screen; `0` for any distance |
| `incandescentFade` | `B:` | `true` | | *CUSTOM only.* Whether incandescent signal lamps fade on and off like a filament, rather than switching like an LED |
| `adBoardTransitions` | `B:` | `true` | | *CUSTOM only.* Whether advertising boards fade or scroll between ads, rather than cutting |
| `chunkBuilderLimit` | `I:` | `0` | 0–1024 | *CUSTOM only.* The most chunk builders to keep. `0` keeps Minecraft's own number. Fewer use less memory but load chunks more slowly while you travel. Never fewer than two per chunk build thread. Lowering it takes effect within seconds; raising it again needs a restart. |
| `chunkBuilderBudgetPercent` | `I:` | `40` | 10–90 | *CUSTOM only.* How much of the direct memory limit the chunk builders may hold before some is given back |

The *CUSTOM only* defaults are the `HIGH` values, so switching to `CUSTOM` changes nothing until you
edit them.

### trafficpoles

#### `trafficPoleIgnoreBlocks`

`S:` list · default empty · **both sides** (see below)

Blocks that traffic poles should **not** connect to. A pole draws a mounting arm towards most
solid blocks beside it. List a block here if it shouldn't, such as another mod's railing, lamp or
decoration that ends up with an unwanted arm. CSM already ignores its own fixtures, so this list is
for what it can't know about.

```
trafficpoles {
    S:trafficPoleIgnoreBlocks <
        examplemod:iron_railing
        glass_pane
     >
}
```

Each line is a block's registry name, `modid:name`. A bare name such as `glass_pane` means
`minecraft:glass_pane`. Press **F3+H** in game to show registry names in item tooltips. A line
that is not a valid name is skipped with a warning in the log. A name that is valid but not
installed matches nothing, which does no harm.

**Changing it in game.** An operator can run
[`/csm poleignore add|remove|list`](commands.md#csm-poleignore). The command saves the file at
once, and poles update the next time a block next to them changes.

**Which copy counts.** A pole's arms are drawn by the client, so on a multiplayer server each
player's own list decides what *they* see. `/csm poleignore` edits the list of the game it runs in:
in single player that is the only list, but on a dedicated server it edits the server's copy. To
remove an unwanted arm for everyone on a server, the players add the same entry to their own
`csm.cfg`. A modpack can ship the file with the list already filled in.

### parking

**Server.** Parking meters and pay stations from Roads' Streetscape tab, explained in
[Streetscape](../guides/streetscape.md). A meter's owner sets its rates on the meter. The
`default…` entries are what a newly placed meter starts with, and the `cap…` entries are the most
an owner may set.

A meter charges money when the optional SUM economy is installed and allows the `csm_roads`
integration (see [Compatibility](compatibility.md#known-interactions)). Otherwise it takes
emeralds.

| Entry | Type | Default | Range | What it does |
|---|---|---|---|---|
| `defaultEmeraldsPerBlock` | `I:` | `1` | 1–4096 | Emeralds a new meter charges per block of time |
| `defaultMinutesPerBlock` | `I:` | `15` | 1–1440 | Minutes a new meter sells per payment ("block of time") |
| `defaultMaxMinutes` | `I:` | `240` | 1–525600 | The most time a new meter sells ahead, in minutes |
| `defaultMoneyPerBlock` | `D:` | `1.0` | 0.01–1000000 | Money a new meter charges per block of time |
| `capEmeraldsPerBlock` | `I:` | `64` | 1–4096 | The most emeralds an owner may charge per block |
| `capMinutes` | `I:` | `1440` | 1–525600 | The most time an owner may let a meter sell ahead, in minutes |
| `capMoneyPerBlock` | `D:` | `100.0` | 0.01–1000000 | The most money an owner may charge per block |

Minutes here are real minutes, not game time. A meter's paid time runs on the wall clock, so it
keeps expiring while the server is empty.

If a default is above its cap, a new meter starts at the cap instead. Changing these values does
not reprice meters that already exist. Each one keeps its rates until its owner next saves its
settings, and only then do the caps apply to it.

### parks

#### `chainsawBrushPiles`

`S:` · default `FEW` · values `NONE`, `FEW`, `MANY` · **server**

How many brush piles a tree felled with the Parks chainsaw leaves around its stump:

| Value | Piles |
|---|---|
| `NONE` | None |
| `FEW` | 1, plus one more per 12 logs felled, at most 3 |
| `MANY` | 3, plus one more per 6 logs felled, at most 8 |

Piles go only on open ground under the sky, never replacing a block. Fewer may appear than the
table says if there isn't room. Any other value is read as `FEW`. See
[Parks & Greenery](../guides/parks-and-greenery.md).

### wiki

`generateWikiFiles` (default `false`) and `wikiFilesFolder` (default `csmWiki`) belong to a wiki
export that the mod no longer has. The current version reads neither, so leave them as they are.

!!! tip "Entries that aren't listed here"
    A `csm.cfg` written by an older version can hold entries that have since been retired, such as
    `shaderCompatibilityMode` under `general`, with no comment above them. Nothing reads them any
    more and they do no harm. Delete them if you want a tidy file.

## csm_signage.cfg

Written by CSM: Signage & Advertising. Restart to apply a change.

| Entry | Category | Side | Default | What it does |
|---|---|---|---|---|
| `allowServerAds` | `server` | Server | `true` | Offer the PNGs in `config/csm/ads/` to every player, for the advertising boards |
| `acceptServerAds` | `client` | Client | `true` | Download the ads a server offers when a board shows one |

**`allowServerAds`.** The server reads its `config/csm/ads/` folder at startup. It takes at most
64 files, each at most 1024 KB and 1024 pixels a side, and skips anything else with a message in
the log. Set this to `false` to keep the folder private: boards then show only the ads built into
the mod.

**`acceptServerAds`.** When `false`, your game downloads nothing. A board that should show a
server ad shows the house ad in its place, for you only.

Making ads for a server is covered in [Advertising](../guides/advertising.md).

## Settings that aren't in a file

Some things you might look for here are set somewhere else:

- **Each block's own setup** (signal timing, sign text, a meter's rate, a thermostat's set point)
  is saved with that block in the world, through its screen. It is not in any config file.
- **How far away CSM's animated blocks are drawn, and Fancy or Fast leaves on Parks trees**, follow
  your Minecraft video settings. [Performance & Graphics](performance-and-graphics.md) covers which
  ones matter.
- **Diagnostic switches** such as `/csm renderpass` and `/csm incandescent` last only until you
  restart, and are not saved. See [Commands](commands.md#diagnostics).
