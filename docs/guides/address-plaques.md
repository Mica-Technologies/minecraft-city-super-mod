# Address Plaques

House numbers for the front of a house or a small building: a plate beside the door, numerals on
the wall, gold leaf on the glass over the door, or a lit number box. They are in the **Building
Materials** tab of the **CSM: Building Materials** module, after the doors.

## The plaques

| Plaque | What it looks like |
|---|---|
| **Address Plaque (Brass)** | Polished brass, screwed at the corners, black numerals |
| **Address Plaque (Cast Iron)** | Black iron with a gold rim and raised gold numerals |
| **Address Plaque (White Enamel)** | White enamel with a blue border and blue numerals, as in New York and Paris |
| **Address Tablet (Carved Stone)** | A limestone tablet with the numbers cut into it |
| **Address Plaque (Painted Wood)** | A green painted board with cream numerals |
| **Address Plaque (Brushed Steel)** | Brushed steel with black numerals standing off it |
| **House Numerals (Stand-Off)** | Separate brass numerals held a little off the wall |
| **Transom Numbers (Gold Leaf)** | Gold-leaf numbers with a dark outline, for the glass over a door |
| **Illuminated Address Box** | A lit box with black numbers on a glowing lens; it lights up the step at night |

## Putting one up

Click the side of a block with a plaque to hang it on that block, at any height. A plaque is thin
and has nothing to bump into, so it fits beside a door, under a stoop's hood, on a door frame or
on a narrow pier without needing a block of depth. Clicking the top of a block hangs it on the
wall you are facing.

For the **transom numbers**, stand outside and click the glass over the door. If the glass sits
in the middle of its block (a glass pane), set **Depth into glass** to 7 so the numbers sit on the
glass; on a full glass block leave it at 0.

## Changing the number

Click a plaque with an empty hand to open its editor. The plaque changes as you type.

- **House number** (up to 8 characters) and **Street line** (optional, up to 32), for example
  `245` and `West 86th St`. Letters, digits, spaces and `. , - ' / & #` can be used.
- **Font**: Classic serif, Didone, Modern sans or Narrow. Narrow fits five digits on a small plate.
- **Text size**: 50% to 100% of the largest that fits.
- **Plate**: Small, Medium (about 1 x 1/2 block) or Large.
- **Layout**: Flat, or **Vertical stack**, the digits one above another for a narrow pier. A stack
  shows the number only.
- **Numerals**: the colour of the numbers, or the plaque's own.
- **Move across** and **Move up**: shift the plaque inside its block, for example to sit it closer
  to the door or at eye level.
- **Lit** (the illuminated box): switch the light on or off.

Click a button to step forward and **Shift-click** to step back.

## Setting plaques with commands

A plaque's number and settings are in its block data, so commands and building scripts can set
them. The block's data value is the side its wall is on: 0 south, 1 west, 2 north, 3 east.

```
/setblock ~ ~1 ~ csm:address_plaque_enamel 2 replace {num:"245",st:"W 86th St"}
/blockdata ~ ~1 ~ {num:"12",lay:"v",plate:"s"}
```

| Key | Values |
|---|---|
| `num` | the house number |
| `st` | the street line |
| `font` | `classic`, `didone`, `modern`, `narrow` |
| `size` | 0.5 to 1 |
| `plate` | `s`, `m`, `l` |
| `lay` | `h` (flat) or `v` (vertical stack) |
| `ink` | `default`, `black`, `white`, `gold`, `brass`, `bronze`, `blue`, `steel`, `red`, `green`, `slate` |
| `dx`, `dy` | -8 to 8: move across and up, in sixteenths of a block |
| `dz` | the transom numbers only: depth into the glass, -24 to 16 |
| `lit` | the illuminated box only: 1 on, 0 off |

## Performance

The numbers are part of the block's model, like the blocks around it, so a street full of
plaques costs nothing extra each frame. Only the illuminated box gives light.
