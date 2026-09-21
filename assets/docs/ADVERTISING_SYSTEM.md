# Advertising System

The advertising boards of **CSM: Signage & Advertising** (`csm_signage`): wall poster boards,
printed and digital billboards built to size up to 40 x 40, and street ad kiosks, showing made-up
ads, public-domain vintage ads, or images the server supplies -- one ad, or a rotation that cuts,
fades or scrolls between them. Road signs are not part of this module; they stay in Roads &
Traffic.

| Block | Registry | Size | Built by | Default light |
|---|---|---|---|---|
| Poster Board | `ad_poster_board` | 1 x 1 to 24 x 12 | its screen | unlit (paper) |
| Billboard | `ad_billboard` | 1 x 2 to 40 x 40 | its screen | lit at night (floodlights) |
| Digital Billboard | `ad_digital_billboard` | 1 x 1 to 40 x 40 | its screen | always lit (screen) |
| Ad Kiosk | `ad_kiosk` | 2 x 4 | itself, on placing | always lit (backlit) |
| Large Ad Kiosk | `ad_kiosk_large` | 4 x 8 | itself, on placing | always lit (backlit) |

Every block of a board but the one placed is a hidden part (`<name>_part`, and `<name>_service`
for a billboard's catwalk row and a kiosk's post), registered in `CsmTabSignageHidden`.

## A board is one object

The block a player places is the board's **controller**, in its bottom row: the board grows up
from it and to its left, right or both (`AdBoardAlign`), and it holds the board's one tile entity
(`TileEntityAdBoard`: size, alignment, ads, rotation, fit, light, back, transition). Its screen's
Done sends `AdBoardConfigPacket`; the server finds the controller from the block clicked, clamps
everything, and `AdBoards.resize` builds or trims the board, naming whatever is in the way.
Breaking any block of a board takes the whole board down (`AdBoards.onBlockGone`, from
`breakBlock`, so explosions and `/fill` do it too).

**No tile entity on the parts.** A part finds its board through its neighbours: its metadata is
facing (2 bits) and a 2-bit **tag** that every block of one board shares and that no touching
board of the same kind and facing does -- `AdBoards.freeTag` picks it when the board is built,
and re-tags the board if a resize brings it against a neighbour holding its tag. That is how two
boards built side by side stay two boards with two frames, and how `findController` walks from
any block to the controller. All four tags taken is refused with a message.

**Survival.** A board built to size costs one of its own item per block, taken as it grows,
given back as it shrinks, dropped in full when a survival player breaks it; nothing is dropped
otherwise. A kiosk (`AdBoardKind.isFixedSize`) is one item, bought whole at the Fabricator (sheet
metal x2, LED module, sign blank) and given back whole.

## Drawing: one quad per board

At rest a board is baked models -- backing, cabinet, frame drawn only where a block has no board
neighbour (actual state `left/right/up/down`), catwalk, post -- from `gen_ad_boards.py`. The ad
itself is **one textured quad across the whole board**, drawn by the controller's renderer
(`TileEntityAdBoardRenderer`), plus a second while it fades or scrolls, one for a letterbox and
one for a digital board's LED grid. A baked face was impossible: each block's slice of an
arbitrary image would need its position in the board as block state.

- **Cabinet boards are solid cubes** (billboards, kiosks): the faces between the 1,600 blocks
  of a 40 x 40 board are culled like a wall's. The frame is a 1 px lip standing proud of the box
  front and back, and the ad sits a quarter pixel outside the box. A billboard's **service row**
  (its bottom row: hangers, catwalk under the face, floodlights, end railings) and a kiosk's post
  rows are not opaque, so they hide nothing behind them.
- **The face is drawn in layers pulled forward by polygon offset** -- letterbox, ad, the fading
  ad, the LED grid -- never by a z gap. A gap small enough to hide banded the ad from sixty blocks
  out; polygon offset is in depth-buffer units and holds at any distance.
- **Ads are their own textures**, not atlas sprites, bound outside any display list, and
  `AdTextures` gives each trilinear filtering and mipmaps on its first bind. The texture manager
  allocates a plain texture with its top mip level and greatest LOD at 0, so both are raised
  first, or the mipmaps are never sampled -- which made the LED grid alias into bands and ad text
  crawl at a distance. A resource reload re-uploads every texture, so the prepared set is reset.
- **Render range is the long range plus the board's own size.** The game measures a tile
  entity's distance to its position alone, the board's bottom corner; without the size a 40-block
  billboard's face vanished while most of it was in range. `isGlobalRenderer` is true for the same
  reason: a board reaches far past its controller's chunk section.
- **Light:** unlit takes the world's light in front of the face's middle, looked up at most once
  a second; lit draws full bright. A digital board dims a little at night and is black, not a
  daylit poster, when switched off (unlit, or redstone mode unpowered).

**Measured** (2026-09-21, `1b2744d99`..`6eda64fb4`): ten 40 x 40 digital billboards (16,000 blocks,
fading, LED grid) and 50 kiosks, all in view, cost **0.32 ms of render work a frame** -- 1.00 ms
with the scene, 0.68 ms with it removed, in one session, three 10 s samples each side agreeing
within 0.02 ms. About 5 microseconds a board.

## What a board shows

`AdLibrary` reads `assets/csm/ads/<source>.json` off the classpath, so the server knows the same
ads the client does, and adds the server's own (below). An unknown id shows the **house ad**,
`your_ad_here`, which "all" and "random" leave out.

- **Shapes.** Every bundled ad is drawn in up to four shapes -- portrait 2:3 (kiosks), square,
  poster 2:1 (wall boards), bulletin 7:2 (billboards); a board takes the one nearest its face
  (`AdShape.nearest`, on the log of the aspect) and fits it by `AdFit`: cover (crop, default),
  contain (letterbox in the ad's background colour) or stretch.
- **Rotation** (`AdRotation`): one ad, or every ad or one category in order or shuffled, at an
  interval of 5 s to 10 min. What is up is worked out from the world time and the board's
  position (a SplitMix64 hash, so neighbouring boards are not in step) -- nothing ticks and
  nothing is sent. A double-sided board's back shows the same ad or the next one.
- **Transitions** (`AdTransition`): cut, fade, or scroll (the old ad rolls up out of the top as
  the new one rolls in), 16 ticks, eased.

### Where the ads come from

| Source | Script | Rights |
|---|---|---|
| `parody` -- 34 invented brands and the house ad | `gen_ads.py` + `ad_art.py`: SVG through resvg, text measured with the same OFL fonts | Original. No real company, product, person or near-miss parody |
| `vintage` -- 18 US print ads, 1900-1910 | `fetch_vintage_ads.py`: Wikimedia Commons, into a gitignored cache | Public domain (pre-1930 US); each file's own licence checked, refused otherwise; `ads/vintage-sources.md` |
| `server` -- a server's own PNGs | none: `config/csm/ads/` | The server admin's |

No ad is fetched from the web while the game runs. Left out of the vintage set on review: patent
medicine, alcohol, tobacco and firearms ads, brands still trading, and the racist caricatures a
good deal of period advertising carries.

## Server-supplied ads

A server offers the PNGs in `config/csm/ads/` (`ServerAds`), switched by `allowServerAds` in
`config/csm_signage.cfg`; a client can refuse them with `acceptServerAds`. On joining, a client is
sent the catalogue (`ServerAdPackets.Catalogue`: id, name, SHA-256, size); it asks for an ad only
when a board on its screen shows it (`ServerAdImages.shown`), one download at a time, in 30 KB
chunks, and caches it as `csm_ad_cache/<sha256>.png`. Until then the board shows the house ad.

Nothing on either side is trusted:

- 64 files, 1 MB, 1024 px a side. The PNG header is read **before** anything is decoded, on the
  server as it loads the folder and again on the client for a download, so a tiny file claiming a
  vast image is refused; the client also checks the hash.
- Every packet read is bounded before it allocates (`CsmPacketUtils`); chunks are taken only in
  order and only up to the catalogue's length.
- A request is answered only for an index the server has, at least 250 ms after the player's
  last, within a budget of twice the catalogue per session; otherwise an empty chunk says no.
- Ids are made from file names (`server_<letters_digits>`) and never used as paths; names lose
  formatting codes and control characters.

## Adding

- **An ad:** add it to `ad_art.py`'s `CATALOGUE` (and an illustration), run `gen_ads.py`, review
  `gen_ads.py --sheet`. Never rename a shipped id: boards store it.
- **A vintage ad:** add the Commons file to `fetch_vintage_ads.py`'s `CATALOGUE`; the script
  refuses anything not recorded as public domain.
- **A board kind:** an `AdBoardKind` entry, a `KINDS` entry in `gen_ad_boards.py`, tab and hidden
  tab lines, lang in all four files. Fixed-size, service rows and cabinet or wall are all flags on
  the kind.

## Traps

- **A z gap is not enough for anything drawn over the ad.** See polygon offset above.
- **`glGenerateMipmap` does nothing useful on a texture the manager allocated** until
  `GL_TEXTURE_MAX_LEVEL` and `GL_TEXTURE_MAX_LOD` are raised.
- **`/fill` places a pedestal pole lying down** (metadata 0): for a test pole under a billboard
  use `csm:trafficpolevertical` at metadata 1.
- **A dedicated server stops ticking entities 15 s after its last player leaves**, so TNT summoned
  with no one on does not go off; test explosions in single player.
- **A UV span longer than a block** (the catwalk's rails) must be squeezed into 0..16, or it reads
  the neighbouring sprite in the atlas; `gen_ad_boards.py` does, and every element outside its cell
  gets explicit UVs.
