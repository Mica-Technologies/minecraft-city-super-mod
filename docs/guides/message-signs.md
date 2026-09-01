# Message & Speed Signs

Signs whose legend you set in game, and which change while the world is running.

## Portable changeable message sign (PCMS)

The trailer-mounted amber board you see at roadworks. Right-click to edit.

| | |
|---|---|
| **Pages** | Up to 8 |
| **Each page** | 3 lines, up to 16 characters each |
| **Cycle speed** | In seconds, between pages |
| **Trailer colour** | Orange, yellow, black, silver, white |
| **Flashers** | Two 8-inch beacons — off, on, or none |
| **Angle** | The board rotates on its trailer |

## Overhead gantry DMS

The three-line amber board over freeway lanes. Same page model as the PCMS — up to 8 pages of three
16-character lines with a cycle speed — but on a large fixed housing rather than a trailer.

Its collision is a single block; the housing it draws is far bigger, roughly nine blocks wide, so
place it in the middle of the gantry span and let the sign overhang.

## Variable speed limit signs

Two of them, sharing a display.

=== "Portable trailer"

    A standard MUTCD **SPEED LIMIT** panel with the number on an LED screen below the static
    legend, on a mast above a tow trailer with outriggers, wheels and two beacon flashers.

    | Setting | Range |
    |---|---|
    | Speed | **20–95** (default 35) |
    | Flashers | None, off, or on |
    | Trailer colour | Orange, yellow, black, silver, white |

    Collision is the low trailer footprint, so you can walk right up to it; the mast and sign are
    drawn upward from there.

=== "Overhead gantry"

    The same **SPEED LIMIT** legend over a large LED number, in a fixed housing with no trailer or
    flashers. Full-block collision.

    It adds one setting of its own: **full screen**, which drops the static legend and gives the
    whole housing over to the number.

## Placing them

All four are in the [Traffic Accessories](../reference/traffic-accessories.md) reference. Because
the housings are much larger than the blocks that own them, leave room:

| Block | Roughly |
|---|---|
| Overhead DMS | 9 wide × 5 tall × 5 deep |
| Portable speed limit | 9 wide × 7 tall × 9 deep |
| Overhead speed limit | 5 wide × 7 tall × 5 deep |
