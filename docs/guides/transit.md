# Transit

Buses, subways and airports: fare gates that take tickets and cards, bus stops, shelters and
departure boards, station fit-out, station entrances, and an airport's terminal and airside. It is
the **Transit** tab of the **CSM: Transit** module.

The module needs **CSM: Roads & Traffic**, because a bus stop is built from road signs on Roads'
sign posts.

Every agency, airline, route, station name and city is invented. There are four bus agencies,
each with its own colours: **CITYLINE** (teal and yellow), **RIVERWAY** (navy and orange),
**VERDANT** (green and white) and **EMBERLINE** (red and graphite).

## Fares

**The Fare Vending Machine** sells fares for emeralds. Right-click it to open its screen:

- a **Fare Ticket** (one ride) for one emerald;
- a new **Transit Card** with 1, 2, 5, 10 or 25 trips, one emerald a trip;
- a reload of 1, 5 or 25 trips onto the card **in your main hand**.

A card shows its trips left in its tooltip. Cards don't stack, since each one carries its own
balance. If your inventory is full, what you bought drops at your feet.

**Fare gates** come in a standard width and two wider accessible (ADA) widths. Place a gate from
the side riders come in from: that side is its **outside**.

- **Entering.** Right-click the gate from outside with a Fare Ticket (it is used up) or a Transit
  Card (one trip comes off). The gate opens with a green arrow. A card with no trips left is
  refused with a low note, and chat tells you to reload it.
- **Leaving is free.** Walk up to the gate from the inside and it opens by itself, showing a red X
  to the outside so nobody tries to come in against the flow.
- An open gate closes itself after 3 seconds.

**Operator modes.** Sneak and right-click a gate with an empty hand to choose:

| Mode | What the gate does |
|---|---|
| **Normal** | Takes fares, opens for leaving riders |
| **Always Open** | Stays open, no fare needed |
| **Always Closed (Maintenance)** | Stays shut, showing the red X |

The **Ticket Validator** is for a platform or a station without gates. Hold a ticket or card to it
and it takes the fare just as a gate does: a green tick if it's good, a red cross if not. It opens
nothing by itself.

## Bus stops

A bus stop is built on **Roads' sign posts** (in the Road Signs tab), exactly as a road sign is:

1. Place one or two **sign posts** from the ground, facing the way the stop should read.
2. Stack the **timetable case**, **route map case** and **Bus Arrival Display** on the post, in any
   order.
3. Put the agency's **Bus Stop Flag** on top.

Each piece takes its facing from the post below, so only the bottom post needs turning. Because
every piece is a road sign, a stop does everything a road sign does: it can stand on a signal
pole's arm, hang from a span wire, stand back to back with another sign on one post, and use the
extension post onto a slab.

**The flag** hangs off one side of the post, so it reads from both sides. Right-click the flag
itself to move it to the other side of the post.

**Route plates.** Up to three route plates hang under the flag. Right-click a plate to step its
number up (1 to 99, or none), and sneak and right-click to step it down. The action bar says which
plate now shows what.

**The Bus Arrival Display** lists the next two buses of each route on its stop's flag, turning its
page every 5 seconds; under a minute reads DUE. The countdown is a minute each real minute, and
every player at the stop sees the same one.

**The curb plaque** is a bronze plate reading BUS STOP that lies on the ground at the curb.

A bench or bin at the stop comes from [Parks & Greenery](parks-and-greenery.md).

## Shelters

Three styles in each agency's livery: **glass** (with a bench and a lit name fascia), **cantilever**
(a curved canopy on one column a block) and **flat roof** (with a lean rail). A shelter is two
blocks tall and places like a door. Its open front faces you.

- **Joining.** Place shelters of the same block facing the same way side by side for a longer
  shelter, or one behind the other for a deeper one. End walls appear only at the ends. Shelters
  of different agencies don't join.
- **The roof light.** Right-click with an empty hand, or give either half redstone, to switch the
  light. It switches the whole joined shelter, so one daylight sensor lights a long shelter.
- **The ad panel.** The glass shelter leaves an empty frame at one end of its back wall. With the
  **CSM: Signage & Advertising** module installed, set its **Bus Shelter Ad Panel** against that
  frame from outside the shelter, and it shows an ad to the street and one into the shelter. See
  [Advertising](advertising.md).

## Departure boards

The **Bus Departure Board** is a bus station's big concourse screen, and the **Bus Bay Display** a
small amber sign over one bay. Both hang on a wall, or on rods from the ceiling when there is no
wall behind.

**Nothing to link.** A board finds the bus stop flags around it by itself (roughly two chunks
around it and twelve blocks up or down). Each flag is a **bay**, numbered from 1 in the same order
on every board. The board lists every route plate's next two buses, soonest first: route,
destination, bay and minutes. It agrees to the minute with the stop's own arrival display.

!!! tip "Keep a station together"

    Keep a station's stops and boards within a couple of chunks of each other. Two boards far
    enough apart to see different sets of stops can number the same stop differently.

**Banks and pages.** Boards of the same kind facing the same way, side by side or stacked, make a
**bank**. It reads like text: each board shows the page after the one to its left, or above. If a
station has more departures than the bank has rows, the whole bank turns to the next set every ten
seconds, and the header shows which set it is on.

**Setting a board up.** Right-click to step which agency it lists: all of them, then each agency
in turn. Sneak and right-click to switch **spoken announcements** on or off. Either change applies
to the whole bank. There is no screen to open.

**Announcements** are only heard with the **CSM: Text to Speech** module installed (which itself
needs CSM: Technology). With them on, you hear "Route 12 to Downtown is now departing from bay 3"
as each bus becomes due. Without that module the board stays silent. Announcements are off on a
new board.

The **bay display** lists the nearest stop, within a few blocks of it. It has nothing to set.

## Station and platform fit-out

These pieces are made to fit out the stations of the **RCMC** train mod, and never to repeat it.
RCMC has the platform, the platform edge, the line map, the arrival board, the speaker and the
track; Transit adds what's missing.

!!! warning "Keep clear of the train"

    An RCMC metro car is about 3.8 blocks wide and stands about four blocks above the platform.
    Nothing here checks. Keep furniture at least a block back from the edge, and hang signs and
    clocks over the platform, not the track, or the train drives through them.

- **Tactile paving** (warning domes and guidance bars, yellow or grey) is a thin layer laid on any
  floor, RCMC's platform included. Lay it on top of the floor block, like a carpet.
- **Platform bench**: benches side by side join into one. Right-click to sit in the nearer seat.
  The **perch** is a lean rail that joins the same way.
- **Help point and emergency point.** Right-click the help point to hear its chime; click its red
  button (low on the front) for an emergency call. It is scenery: nothing is sent anywhere.
- **Platform clock**: two faces, showing the world's time.
- **CCTV cameras** and a clear-bag **litter bin**.
- **Platform number sign** and **number band column**: right-click to step the number 1 to 20,
  sneak and right-click to go back.
- **Hanging signs** (TO TRAINS, EXIT, a strip of line bullets) read correctly from both sides, the
  arrows pointing the same way in the world from either side.
- **Station name sign**: right-click to step through ten station names.
- A **gap warning sign** and a **network map board**.
- **Station wall tile**, plain white or with a coloured band in an agency's colour that lines up
  from block to block.
- **Columns** (tiled or steel) stack into one column, with a base only at the foot and a head only
  at the top.
- **Platform canopy**: set it on columns. Canopies join on all four sides, with a fascia only round
  the outside and a light strip under it.

## Station entrances and the fare line

**A subway kiosk** is built from pieces:

- **Station Entrance Glass** joins like a pane. Two blocks of glass stacked are one pane, with the
  head rail and kick plate only where the wall stops.
- **Station Entrance Roof** sits on the glass and joins on all four sides, the agency's fascia
  only round the outside. Its light is always on.
- **The name board**: right-click a roof block with an empty hand to step its board through none,
  SUBWAY, METRO and the ten station names; sneak and right-click to go back. The board shows on
  every outside side of that block, so set the middle of the front to the station's name and the
  corners to SUBWAY.
- Dig vanilla stairs down under the kiosk, with the glass standing on the rim of the stair well.

**An open stair entrance** has no roof, only the **Station Entrance Railing** round the well (it
behaves like a fence), the railing **with a name plate** (right-click to step the name), and a
**globe lamp** either side of the mouth. Right-click a globe to change its colour (green, red,
white); it is always lit.

**The fare line:**

- **Fare Line Railing** is stainless, at the fare gates' height, and **joins the fare gates**, so
  a gate line reads as one barrier. A version carries a PAID AREA plate.
- **The service gate** is a fence gate: a click or redstone opens it. It checks no fare.
- **Line bullets**: right-click to step through sixteen lines, sneak and right-click to go back.
- **The Station Agent Booth Counter** stands in line with the entrance glass: stack glass on it for
  the booth's window. It's decoration; the vending machine sells the fares.

## The airport terminal

**The schedule.** Every board, kiosk and scanner works from one timetable, which follows the
world's time of day, so the boards agree with the clocks and the sun. A flight leaves every 20
game minutes, about every 17 real seconds. Statuses follow the minutes left: BOARDING, FINAL CALL,
GATE CLOSED, DEPARTED; arrivals read ON TIME, LANDING, LANDED. Now and then a flight is delayed or
cancelled. With the daylight cycle off, the boards stop too.

- **Check-in desks** and **bag drop scales** facing the same way join into one counter. Right-click
  a desk with an empty hand to change its airline, sneak and right-click to go back. **Gate desks**
  join each other.
- **The Self Check-In Kiosk**: right-click it to print a **Boarding Pass** for one of the next
  departures, with a seat. The pass's tooltip shows its flight, city, time and gate, matching the
  boards. A pass taken from the creative tab has no flight.
- **Queue stanchions**: each belt reaches to every stanchion beside it, so a row is one barrier,
  and it can't be jumped. Belts run only to the four sides, not diagonally.
- **Security**: the X-ray scanner, roller conveyor and divesting table line up into one lane.
  **Security trays** set on a roller or the table drop onto its top.
- **The Boarding Pass Scanner**: hold a pass to it. A pass for a flight that hasn't departed or
  been cancelled, and hasn't been used, is marked boarded with a green tick. Otherwise a red cross,
  and the action bar says why. It doesn't check the gate.
- **Gate sign**: right-click to step the number 1 to 20, sneak and right-click to step the letter
  A to D.
- **Airport seating** joins like the platform bench, and you can sit in it.
- **Flight information boards** (Departures and Arrivals) glow at night. Boards of the same kind
  side by side and stacked are a bank: each lists seven flights, continuing from the board to its
  left or above, so a bank of six shows the next 42 flights.
- **The baggage carousel** joins on all four sides into a loop of any size (two blocks wide is
  usual). The plates run clockwise round the loop.
- **Luggage carts**, a **cart rack**, and hanging signs for GATES, ARRIVALS, CHECK-IN, BAGGAGE CLAIM
  and GROUND TRANSPORT.

The walk-through metal detector is in [Emergency Services](emergency-services.md), and shop and
cafe fit-out is in the Market & Store tab ([Furniture](furniture.md)). Both work in a terminal if
their modules are installed.

## Airside

**Airfield lights** (runway and taxiway edge, threshold, centreline, stop bar, approach light bar,
beacon, and an obstruction-lit wind sock and antenna mast) switch **a circuit at a time**:

- The circuits are runway, taxiway (which includes the signs), beacon and obstruction.
- Right-click any light with an empty hand, or give it redstone (on when power comes, off when it
  goes), and every light of that circuit within eight blocks of the next switches with it. The
  action bar says how many lights it reached.
- A light placed next to a lit circuit comes on with it.
- Keep two runways' lights more than eight blocks apart if they should switch separately.

The **beacon** flashes white and green round its lens on its own.

**Airfield signs**: taxiway location, taxiway direction, runway holding position and distance
remaining. Right-click to step the legend. On the direction sign, sneak and right-click to turn the
arrow round; on the others it steps back.

**The airfield mast** stacks to any height, and the approach light bar, wind sock and stand sign
stand on it. The **stand sign** is set like the gate sign.

**Ground equipment** (wheel chocks, ground power unit, baggage tug, baggage cart, air stairs) are
props, two blocks long, placed whole like Streetscape's utility boxes. The air stairs can't be
climbed.

**The jet bridge** is a walk-through corridor two blocks wide and two tall: **tunnel** sections
that join end to end, a **cab** at the aircraft end with a safety bar across the open end, and a
**rotunda** that a tunnel joins two blocks out. Build it up on **drive legs** and a **rotunda
column** to the terminal's upper floor. It's level; there are no sloping pieces. Face every piece of
one bridge the same way, towards where the aircraft would be.

## What isn't included

- **No aircraft** of any kind, and no trains. Nothing docks at the jet bridge or taxis.
- **Nothing that RCMC already has**: platforms, platform edges, station speakers, platform arrival
  boards, line maps or track pieces.
- No vehicles, no road markings, and no real transit or airline brands.

## What is in the tab

119 blocks, listed in the [Transit reference](../reference/transit.md).
