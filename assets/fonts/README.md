# Mica Open Highway

Mica Open Highway is a font family of the lettering used on US road signs: the FHWA Standard
Alphabets, often called "Highway Gothic". The mod uses these fonts for its sign textures and its
in-game sign text. This folder is where they live; the build copies the fonts the game needs from
here.

| File | Style | Typical use |
|---|---|---|
| `MicaOpenHighway-SeriesB.ttf` | Series B | narrowest; small plaques and crowded legends |
| `MicaOpenHighway-SeriesC.ttf` | Series C | narrow regulatory and warning legends |
| `MicaOpenHighway-SeriesD.ttf` | Series D | most regulatory and warning signs |
| `MicaOpenHighway-SeriesE.ttf` | Series E | wide; street names, route markers |
| `MicaOpenHighway-SeriesEM.ttf` | Series E Modified | heavier E; guide sign destinations |
| `MicaOpenHighway-SeriesF.ttf` | Series F | widest |

Each font has the capitals, the lowercase, the digits and the punctuation the alphabets define:
`& ! " # $ ¢ / * . , : ( ) - @ = + ?`. An apostrophe (the comma raised to the cap line) and a
space are added. All six are TrueType, with the family name "Mica Open Highway" and the series as
the style.

## How they were made

The source is the FHWA publication *Standard Alphabets for Traffic Control Devices* (2000 edition),
a public-domain US government work. It has two parts for each series:

- **Letter pages**, with every character drawn large on a grid.
- **A spacing table**, with each character's left bearing, width and right bearing in inches, for
  letters 4 inches tall.

The fonts come from those two parts:

1. **Outlines.** Each letter page is rendered at high resolution with the grid removed. Each
   character is found as a shape of ink, matched to its place on the page, and traced into curves.
2. **Placement.** Every glyph is scaled so the capital H is 700 units tall on a 1000-unit em. It
   sits on its row's baseline and starts at the left bearing given in the table.
3. **Spacing.** Each character's advance is its left bearing, width and right bearing from the
   table. The alphabets set spacing by these bearings, not by kerning pairs, so the fonts have no
   kerning. The word space is 0.49 of the cap height for Series B, C and D, and 0.69 for E, E
   Modified and F. These are the values measured off the signs in the FHWA *Standard Highway Signs*
   book.
4. **Corrections.** A few table rows disagree with their own drawings or break the pattern of every
   other series. In those cases the drawing's width is kept, and the two misprinted bearings (Series
   E `k`, Series D `3`) are corrected. The generator lists each one.

The outlines are traced from the drawings, not copied from any existing font file.

## Rebuilding

```bash
pip install fonttools pymupdf potracer numpy
python dev-env-utils/scripts/gen_mica_open_highway.py           # writes the six fonts here
python dev-env-utils/scripts/gen_mica_open_highway.py --check   # exits 1 if they have drifted
```

The generator downloads the alphabets book on first use. Its output is byte-for-byte repeatable, so
`--check` compares the fonts exactly.

## License

The fonts are licensed under the SIL Open Font License 1.1 (see `OFL.txt`). You may use, bundle,
modify and redistribute them, but you may not sell the fonts on their own. The letterform designs
come from the FHWA alphabets, which are in the public domain.
