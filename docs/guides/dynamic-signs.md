# Guide & Street Signs

Two sign families you compose rather than pick off a shelf: **highway guide signs** and **street
name blades**. Both are edited in game through a screen on the block, and both draw their legend in
the FHWA typeface.

## Highway guide signs

The big green overhead signs. Right-click one to open its editor.

### How a sign is built up

```
Sign
└── Panel  (up to 4, stacked)
    ├── Exit tab      (optional — position, text, colour, toll flag)
    └── Row  (up to 6)
        └── Element  (up to 5)
```

An **element** is one of:

| Element | What it is |
|---|---|
| **Text** | A line of legend |
| **Shield** | A route marker — 8 generic plus 10 state-specific, with an optional banner word above or beside it, and an optional white backing plate |
| **Arrow** | One of 10 arrow types |
| **Divider** | A rule between elements |
| **Spacing** | Deliberate gap |

### Sign-level settings

| Setting | Options |
|---|---|
| Colour | Green, blue, brown, yellow, white, black, purple — the seven FHWA sign colours |
| Post | Overhead, or ground-mounted |
| Border | Width |
| Corners | Round or square |
| Minimum width / height | Floors, in sign pixels — 16 px is one block. Surplus height centres the content |
| Auto-fit | Scales *everything* — text, shields, arrows, spacing, dividers and the exit tab — uniformly to fill the minimum box |

### Rows

Each row has its own alignment and vertical spacing. A row can be given a **yellow patch**, which
renders it on the MUTCD "EXIT ONLY" yellow band spanning the sign, with dark text and dark arrows.

### Arrow-per-lane

A panel can carry an **arrow-per-lane band** below its rows — up to 8 lanes, with the rightmost
lanes markable as EXIT ONLY.

## Street name blades

The blades on street corners. Same editor idea, much simpler document, because a street blade always
has the same anatomy.

### Anatomy

| Slot | What goes in it |
|---|---|
| Prefix / street name / suffix | The legend itself |
| City text | A secondary line |
| Block number | Optional, left or right, and top/middle/bottom |
| Emblem | Optional — a route shield with its number drawn over it, or one of 16 civic logos |
| Arrow | Optional, left or right |

### Mounting

| Mount | How it hangs |
|---|---|
| **Hanging** | Suspended below two hangers, panel centred in the block's depth |
| **Hanging bracket** | The same panel, off a horizontal support beam on a single centre drop |
| **Flat** | Back against the block behind, like a guide sign |

Either hanging style can be **double-sided**.

### Lighting

Blades can be internally lit, with a light mode controlling when.

### Presets

Six templates to start from rather than building a blade from nothing: Standard Green Blade, Blue
Blade, Illuminated Blade, Numbered Cross Street, Historic District, and Flat Wall Blade.

## One visual language

Street blades reuse the guide sign's colours, corner styles, arrows, the 78 route markers, the
shared atlas and the FHWA legend font. One atlas, one font, one look — so a blade and a guide sign
on the same corner belong to each other.
