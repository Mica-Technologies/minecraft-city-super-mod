# CSM i18n tooling

CSM is built from several source trees: Core at `src/main` plus one per optional module under
`modules/<name>/src/main` (see `modules.gradle`, and `dev-env-utils/scripts/csm_layout.py` for the
Python side of this same layout). Every tree ships its own `assets/csm/lang/en_us.lang` and its own
`es_es.lang` / `de_de.lang` / `sv_se.lang`, because each module carries its lang files in its own
jar and Forge merges the `csm` lang domain across whichever jars are installed.

`translate-lang.js` walks every tree in turn (`src/main/resources/assets/csm/lang` plus one entry
per directory under `modules/`), reads that tree's own `en_us.lang` as the English source of truth,
and refreshes that SAME tree's own `es_es.lang`, `de_de.lang` and `sv_se.lang` beside it, using
[`google-translate-api-x`](https://www.npmjs.com/package/google-translate-api-x) against the free
public Google Translate endpoint. A key in the roads module's `en_us.lang` is only ever compared
against the roads module's own target files, never Core's.

This mirrors the launcher's `tools/i18n/translate-locales.js`. See **Differences from the
launcher** below for what had to change for Minecraft.

## Setup

```bash
cd dev-env-utils/i18n
npm install
```

## Usage

```bash
npm run translate          # incremental: every tree, only keys missing from each target file
npm run translate:dry      # print what would change, write nothing
npm run translate:force    # re-translate every key, overwriting existing values

node translate-lang.js --only=sv_se     # restrict to one locale, still every tree
```

Incremental is the normal mode and the reason this exists: adding one block costs one API call
per language, which is well within what the free endpoint tolerates. Every run processes every
tree that has an `en_us.lang` (there is currently no per-tree filter — the run is cheap because
only missing keys cost an API call).

**Always run the validator afterwards** — it now checks every tree, each against its own
`en_us.lang`, in one pass:

```bash
python ../scripts/validate_lang_translations.py
```

It checks key parity and order, duplicate keys, `%s` specifiers, protected terms surviving
intact, leading whitespace, encoding and line endings, and reports how many values came back
identical to English. It exits non-zero, so it can gate a batch.

## This is not how the current translations were made

The bulk pass — all 1,631 keys in three languages — was done by language models working against
a per-language glossary, not by this tool. That was a deliberate choice. A general-purpose
translator has no memory between calls, and this file is full of vocabulary where that matters:

- **`Sign` is the head noun of 483 entries.** Three inconsistent renderings of it would be
  invisible to anyone who does not read the language.
- **`Solid` means the round ball indication**, not a steady one. `Vertical Solid Flash Red
  Signal` proves it: "steady flashing red" is a contradiction.
- **`Border` is the signal backplate** — *Placa de Contraste*, *Rückplatte*, *kontrastskärm* —
  not an edge or a frame.
- **`Doghouse` is two unrelated things**: a signal head cluster in the traffic blocks, and an
  actual dog kennel in the furniture ones.
- **`Alto` is a city in this mod** and also the word on every Spanish stop sign. `EST` is a
  manufacturer and also French. `MT`, `BG`, `TE`, `GE` are all real words somewhere.

Use this tool to keep the files current as names are added. For another full sweep, or a new
language, go back to the glossary-driven approach.

## How values are protected

Manufacturer names, agency names, model designations and technical acronyms are listed in
`glossary.json` under `protected`. Along with `%s` specifiers they are swapped for `__CSM0__`
sentinels before each API call and restored afterwards — the same mechanism the launcher uses for
`{0}` placeholders, extended to cover proper nouns. Restoration is case-insensitive because some
target languages lowercase all-caps tokens.

`glossary.json` also pins `terms`: the settled translation for vocabulary that recurs across many
names. After each call the tool reports any value where the English contained a glossary term but
the translation did not come back with the pinned rendering, so drift is surfaced rather than
silently written.

## Units

Two rules, decided by what the player is looking at:

- **Verbatim** — units printed on the depicted sign artwork: `MPH`, clock times like
  `8:30a-5:30p`, distances like `500 Feet`. Localising these makes the label disagree with the
  texture.
- **Localised** — equipment specifications that appear on no sign: lens diameters (`16-Inch`),
  rotation angles (`90-Degree`). These read as *Pulgadas*, *Zoll*, *Tum*.

## Differences from the launcher's version

- Minecraft `.lang` files are read as **raw UTF-8**. The launcher escapes non-ASCII as `\uXXXX`
  because `java.util.Properties` needs it; doing that here would put a literal backslash-u in the
  player's inventory.
- **Line endings follow each tree's own `en_us.lang`** (CRLF in this repo), so a refresh doesn't
  rewrite the whole file and wreck `git blame`.
- The sentinel mechanism protects **proper nouns and acronyms**, not just format placeholders.
- **Glossary terms** keep incremental additions consistent with the bulk pass.
- Keys present in a target but no longer in English are **dropped**, so the files cannot
  accumulate orphans.

## Keep the locale list in sync

`TARGET_LOCALES` in `translate-lang.js` lists the shipped languages. When adding one, add a
`terms` block for it in `glossary.json` too, or the new language gets no vocabulary pinning.

## On quality

Auto-translation is a usability floor: every name shows in the target language instead of falling
back to English mid-inventory. It is **not** a substitute for a native speaker. These files are a
starting point for community corrections.
