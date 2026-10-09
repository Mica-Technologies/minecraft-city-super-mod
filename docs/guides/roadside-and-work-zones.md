# Roadside & Work Zones

The hardware along and over a road that is not a signal or a sign face: work zone devices and the
arrow board, guardrails, overhead sign trusses, concrete and pedestal poles, enforcement cameras,
and mile markers, object markers and delineators. It all ships in the **CSM: Roads & Traffic**
module, mostly in the **Traffic Accessories** tab.

Some roadside hardware has a page of its own: [Railroad Crossings](railroad-crossings.md),
[Mast Arms](mast-arms.md), [Span Wire](span-wire.md), [Road Signs](road-signs.md),
[Message & Speed Signs](message-signs.md) (the portable message sign, speed limit trailer and radar
sign) and [Streetscape](streetscape.md).

## Work zone devices

| Device | Notes |
|---|---|
| **Traffic Cone**, **Traffic Cone (Lime)**, **Traffic Cone (Knocked Over)** | |
| **Traffic Drum** | Its warning light flashes on its own, no redstone needed. **Traffic Drum (No Light)** has no light |
| **Channelizer**, **Channelizer (Lime)** | The tall slim tube |
| **Channelizer-Cade**, **Vertical Panel** (Keep Left / Keep Right) | A tube with a small striped panel, and a narrow panel for spaces too tight for a barricade |
| **Type I**, **Type II Folding** and **Type III Barricade** (Keep Left / Keep Right) | See below |
| **Water-Filled Barrier** (orange, white), **Temporary Concrete Barrier** | Walls that join into one long run |
| **Steel Road Plate** | Laid over a trench and driven over. Plates join on all four sides into one patch |
| **Safety Fence** | Orange mesh, see-through |
| **Temporary Pavement Marker** (six colours) | Small tabs taped along a lane line. Their reflective strip glows at night under OptiFine |
| **Sand Barrel**, **Flagger STOP/SLOW Paddle** | |
| **Portable Signal Trailer** (plain, Mast Arm, Pedestrian) | Carries no heads of its own; place signal heads on it. A head hung under the mast arm's boom uses the **Overhead Mount** type, set with the Signal Head Configuration Tool (see [Configuring a head](traffic-signals.md#configuring-a-head)) |
| **Arrow Board** | See below |

"Keep Left" and "Keep Right" mean the stripes slope toward the side traffic should pass.

### They settle onto the road

A road that climbs is built from blocks whose top sits partway up the cell. A device placed on one
goes in the cell above, and would normally float. Work zone devices **settle** instead: they drop
onto whatever is actually below them, whether that is a sloped road block, a slab, a snow layer, or
a road marking over a road. It works with any mod's road blocks.

- A device drops at most two blocks. If it would have to fall further, it stays where you put it.
- Devices stacked on each other do not settle; they sit as placed.
- Each block settles on its own, so a run of barricades across a height change **steps** rather
  than ramping. One step per barricade looks right; steeper does not.

Nearly everything that faces a way takes **eight facings**, the four diagonals included, so a
taper or a closure can follow a road laid at 45 degrees. Runs join diagonally too, with a short
filler bridging each joint. The one exception is the **Portable Signal Trailer (Mast Arm)**, which
takes four.

### Barricades

**Type I** and **Type III** barricades join the barricades beside them into one continuous run,
with one upright at each joint. A barricade joins only along its length, only to the **same
block**, and only to one **facing the same way**: a keep-left meeting a keep-right would contradict
itself. Break one out of the middle and the two halves close up on their own.

The **Type II Folding Barricade** is an A-frame carried in and set down. It never joins; a row of
them is a row of separate barricades.

Right-click any barricade:

| With | Does |
|---|---|
| A road sign in hand | Mounts that sign on the barricade's face. Any sign in the Road Signs tab works |
| An empty hand | Cycles the warning lights: **None**, **Left only**, **Right only**, **Both ends** |
| Sneak | Takes the mounted sign off and gives it back |

Warning lights on a run flash out of step with each other, the way real ones drift apart.

### The arrow board

Right-click the **Arrow Board** to step to the next mode; sneak-right-click to step back. A new
board starts on **Sequential Chevron (Right)**.

| Mode | Stage length |
|---|---|
| **Sequential Chevron** (Right / Left) | 420 ms |
| **Sequential Arrow** (Right / Left) | 380 ms |
| **Flashing Arrow** (Right / Left) | 700 ms |
| **Flashing Caution** | 700 ms |

The board is a 25-lamp, 7 by 5 grid on its trailer, drawn at the same scale as the portable message
sign beside it.

To change how fast every board runs, set `arrowBoardSpeedPercent` in `config/csm.cfg` (in
`general`). It runs from 10 to 400 and defaults to 100; a bigger number is faster, and it scales
every stage of every mode. See
[Configuration](../getting-started/configuration.md#arrowboardspeedpercent).

## Guardrails

Four rail families, each in several versions:

| Family | Versions |
|---|---|
| **W-Beam Guardrail** | Steel post, **Wood Post**, **Double Sided**, **Wood Post, Double Sided** |
| **Thrie-Beam Guardrail** | The same four |
| **Box Beam Guardrail** | One-sided, **Double Sided**, **Stacked**, **Stacked, Double Sided** (two tubes) |
| **Cable Barrier** | One-sided, **Double Sided** |

The pieces that finish a run:

| Piece | For |
|---|---|
| **W-Beam Guardrail End** (Flared, Impact Head, Terminal, Turndown) | W-beam |
| **Thrie-Beam Guardrail End** (Flared, Turndown) | Thrie beam |
| **Box Beam Guardrail End (Bullnose)** | Box beam |
| **Cable Barrier Anchor** | Cable barrier |
| **W-Beam to Thrie-Beam Transition** | W-beam at one end, thrie beam at the other |
| **Crash Cushion (Nose)**, **Crash Cushion (Bay)** | Any rail |

### How a run joins

Rails join on the **rail**, not the block. A run can change from steel to wood posts, or from one
side to double sided, partway along and still read as one run. W-beam and thrie beam do not join
each other, because they are different rails; put a **W-Beam to Thrie-Beam Transition** between
them.

Guardrails settle onto the ground like the work zone devices, and a run **climbs** rather than
staircasing: where the next cell is a whole block higher, the rail ramps up across the lower cell.
A grade of less than a block draws level and steps at each cell. Runs also join on the diagonals.

An end piece mirrors itself to suit the side its run is on, so the same block finishes either end.
A lone end, or one with rail on both sides, is not mirrored.

The **crash cushion** is laid the way a run is: a **Nose**, then as many **Bays** as you want, then
the rail. It joins in one direction only, nose to bay to bay to rail.

Every rail, end and cushion collides 1.5 blocks tall, like a fence, so you cannot step or jump over
it. Stack a guardrail on a guardrail for a taller barrier: the posts carry on through. A box beam
on a box beam drops its base plate.

!!! tip "Signs and poles behind the rail"

    Place a road sign, or a painted thick or thin traffic pole, on top of a guardrail and its post
    reaches down through the rail to the ground. Concrete and pedestal poles do not.

### The Guardrail Tool

| Click a rail with the **Guardrail Tool** | Does |
|---|---|
| Right-click | Adds or removes the post in that cell |
| Sneak-right-click | Steps to the next version in the same family |

The versions cycle within a family, in the order in the table above (the W-beam: steel, wood,
double sided, wood double sided, and round again). The tool never changes one family into another,
since that would break the run on both sides. It works on the rails and on the transition's post;
it does nothing to ends, anchors or the crash cushion.

## Overhead sign trusses

The galvanized truss a guide sign hangs from over a road.

| Block | What it is |
|---|---|
| **Overhead Sign Truss** | A 1x1 box truss |
| **Overhead Sign Truss (2x2)** | A 2x2 box truss. Each block is one quarter of the section; it works out which from its neighbours |
| **Low-Profile Sign Truss** | The shallow pipe truss on twin-post frames that most newer sign bridges use |
| **Sign Truss Catwalk** | The maintenance walkway in front of a truss, below its signs |

**Box trusses** take their direction from the face you place them against, like a log. Place one
against the side of a block for a horizontal span or arm, or on the ground for an upright leg. The
same block is a sign bridge's span, its legs, or a cantilever's upright and arm. End frames appear
wherever the truss stops, and a standing leg gets a base plate with anchor bolts.

**The low-profile truss** is a span when placed against the side of a block, running that way. On
the ground (or under a block) it is a **frame**, set to carry a span across the direction you are
looking. A span runs its chords into a frame beside it, or closes with an end frame.

**The catwalk** faces you when placed: its railed side is the outside, with the truss behind it.
Place it in front of the truss, a block below the signs. Catwalks side by side that face the same
way join into one walkway, with a railing across each open end.

**Hanging signs.** A [highway guide sign](dynamic-signs.md#highway-guide-signs) placed against a
truss starts with its post type set to **Truss**, and draws hanger brackets up to the truss's top
chord. An ordinary road sign facing away from a truss behind it sets back onto the truss and uses
it as its post; see [Posts and setback are automatic](road-signs.md#posts-and-setback-are-automatic).

## Concrete and pedestal poles

### Concrete poles

**Concrete Thick Traffic Pole**, **Octagon Concrete Thick Traffic Pole**, **Concrete Thin Traffic
Pole** and **Octagon Concrete Thin Traffic Pole**: a spun round pole and a precast octagonal one,
at the thick and thin poles' widths. There is one block of each, stacked to any height, and each
end decides for itself what it shows:

| Beyond that end is | It shows |
|---|---|
| Another concrete pole (either profile) | A seamless joint |
| A post light | A collar and the tenon the light slips over |
| Air, plants, snow or water | A cap band and a chamfered top |
| The ground, under an upright pole | A stepped plinth |
| Any other solid block | A plain end |

So there is no separate base or top to place. Put a post light on the pole and its cap becomes a
tenon; break the light and the cap comes back. A post light on a concrete pole is silver.

The sides work like any traffic pole: mountable hardware beside the shaft grows a mount toward it,
and the light mounts, tapered masts and [mast arm](mast-arms.md) curves fit the shaft. Most pole
accessories (connectors, quad and signal mounts, guy mounts, double poles, sign mounts, curve
connectors, mast arm curves) also come in a concrete finish, and the straight vertical pieces in
octagon as well.

### Pedestal poles

The **Pedestal Traffic Pole** (silver, black, tan, white, unpainted) is the slim aluminium post that
carries a pedestrian signal, push button or small sign at the curb. It stacks the same way: a
pedestal base on the ground, a domed cap where it ends in air, a plain end where it runs into a
block such as a signal head, and a seamless tube where poles meet. Laid sideways, one block is a
short capped bar. Devices beside it get a band clamp.

A pedestal pole can wear a decorative **Pole Finial**; see
[Pole finials](traffic-signals.md#pole-finials).

## Enforcement cameras

**Red Light Camera**, **Speed Camera** and **Enforcement Camera Flash Unit**, in white or black.
They mount on CSM's traffic poles and are decorative: no redstone, no detection.

- **Pole Top** versions go in the block above a pole, sleeved over its top on a collar that fits
  every pole width.
- **Side Arm** versions hang beside a pole. Click one onto the side of a pole and it turns to look
  back toward you, hanging beside the pole with its arm running into it. The arm and clamp fit the
  pole's width, on whichever side the pole is.

## Mile markers, object markers and delineators

### Mile markers

| Block | Plate |
|---|---|
| **Mile Marker Sign** (1, 2 or 3 Digits) | MILE over stacked numerals |
| **Intermediate Mile Marker Sign** (1, 2 or 3 Digits) | The same, with a tenth panel (".2") |
| **Enhanced Mile Marker Sign** | Direction, route shield and route number, MILE, the number |
| **Enhanced Intermediate Mile Marker Sign** | The same, with a tenth panel |

Right-click a mile marker to set it. The screen has the **Mile** field, a **Tenth** field on the
intermediate plates, and on the enhanced plates a direction button (North, South, East, West;
shift-click to go back), the route shield with `<<` and `>>` buttons, and a **Route number** of up
to three digits. Changes apply as you type.

Each plate takes the numbers that fit it: 0 to 9, 10 to 99 or 100 to 999 on the stacked plates, and
0 to 999 on the enhanced ones. Otherwise a mile marker is an ordinary road sign, with eight facings,
posts, setback and back to back; see [Road Signs](road-signs.md). They are in the **Road Signs**
tab.

### Object markers

**Type 1 Object Marker** (OM1-1 to OM1-3), **Type 2 Object Marker** (vertical and horizontal,
OM2-1 and OM2-2), **Type 3 Object Marker** (Left, Center, Right) and **End of Roadway Marker**
(OM4-1 to OM4-3). Each stands on its own green U-channel post, takes eight facings and settles onto
the road like a work zone device. They are at the end of the warning signs in the **Road Signs**
tab.

### Delineators

In the **Streetscape** tab, alongside the Delineator Post and Zebra Delineator covered in
[Streetscape](streetscape.md#bollards):

- **Delineator Post (U-Channel)** in white, yellow and red, and **Double White** and **Double
  Yellow** with two reflectors.
- **Flexible Marker Post** in white, yellow and red.

Both kinds settle and take eight facings, like the object markers.

## See also

- [Traffic Accessories](../reference/traffic-accessories.md): work zone devices, guardrails,
  trusses, poles and cameras in the reference.
- [Road Signs](../reference/road-signs.md): mile markers and object markers.
- [Streetscape](../reference/streetscape.md): the delineators.
- [Compatibility](../getting-started/compatibility.md): road mods whose sloped blocks these devices
  settle onto.
