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
MiKTeX.MiKTeX`) and run `pdflatex`-compatible builds via `latexmk`.

The `Fontconfig error: Cannot load default config file` warning from Tectonic
on Windows is harmless.

## Content sources

The manual documents behavior as implemented in
`com.micatechnologies.minecraft.csm.trafficsignals` (notably
`logic/RingBarrierState.java` for the ring/coordination engine and
`AdvancedSignalControllerGui.java` for the panel). If controller behavior
changes, update the corresponding section — the architecture doc is
`../TRAFFIC_SIGNAL_SYSTEM.md`.
