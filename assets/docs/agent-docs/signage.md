# Signage & Advertising

The advertisements and the board blocks.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_ads.py` -- the parody advertisements the Signage & Advertising boards show: invented
  brands only, each drawn as SVG (illustrations in `ad_art.py`, text measured with the same OFL
  fonts resvg renders it with, fetched into the gitignored `_font_cache/`) in four shapes -- portrait
  2:3, square, poster 2:1, bulletin 7:2 -- laid out per shape rather than cropped, written as
  256-colour PNGs plus the `ads/parody.json` index `AdLibrary` reads; `--sheet` makes the review
  contact sheet, `--check` fails on drift
- `fetch_vintage_ads.py` -- the public-domain vintage ads: fetches each from Wikimedia Commons
  into the gitignored `_vintage_cache/`, refuses any file Commons does not record as public
  domain, sets it whole on a flat backdrop in the ad shapes, and writes `ads/vintage.json` and
  `ads/vintage-sources.md`. Paces its requests and backs off on HTTP 429; `--check`, `--sheet`
- `gen_ad_boards.py` -- the advertising boards' blocks: the backing and aluminium frame models,
  the multipart blockstate shared by a board's controller and its parts (frame only on the edge
  blocks, picked from actual state; left and right strips run the full height and top and bottom
  caps finish a strip only where it continues, so no two pieces overlap), and the item icons.
  The ad itself is the controller's renderer's; `--check` fails on drift
