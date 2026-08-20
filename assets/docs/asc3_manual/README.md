# ASC-3 Operations Manual

`ASC3_MANUAL.tex` is the LaTeX source for the CSM ASC-3 operations manual /
signal-timing primer (`ASC3_MANUAL.pdf`). All diagrams are TikZ — no external
image assets.

## Building

The easiest toolchain is [Tectonic](https://tectonic-typesetting.github.io)
(single binary, auto-downloads packages on first run):

```bash
tectonic ASC3_MANUAL.tex
```

On Windows, grab `tectonic-*-x86_64-pc-windows-msvc.zip` from the GitHub
releases page (it is not on winget), or use MiKTeX (`winget install
MiKTeX.MiKTeX`):

```bash
latexmk -xelatex -interaction=nonstopmode ASC3_MANUAL.tex
latexmk -c   # remove the aux files; commit only the .tex and .pdf
```

**Use `xelatex`, not `pdflatex`.** The source has no `inputenc`/`fontspec` and
relies on the engine reading UTF-8 directly, which the XeTeX-family engines
(Tectonic, `xelatex`) do. `pdflatex` aborts on the Unicode minus signs in the
keypad diagrams with `Unicode character − (U+2212) not set up for use with
LaTeX`.

The `Fontconfig error: Cannot load default config file` warning from Tectonic
on Windows is harmless.

## Content sources

The manual documents behavior as implemented in
`com.micatechnologies.minecraft.csm.trafficsignals` (notably
`logic/RingBarrierState.java` for the ring/coordination engine and
`AdvancedSignalControllerGui.java` for the panel). If controller behavior
changes, update the corresponding section — the architecture doc is
`../TRAFFIC_SIGNAL_SYSTEM.md`.
