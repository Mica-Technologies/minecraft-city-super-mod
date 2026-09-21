# Advertising

Poster boards, billboards and street kiosks for your city, from the **CSM: Signage & Advertising**
module, all in the **Signage & Advertising** creative tab. Every ad is made up, or a real ad old
enough to be in the public domain, or one your server supplies.

## The boards

| Block | Size | What it is |
|---|---|---|
| Poster Board | up to 24 x 12 | A framed poster on a wall. Printed, so it is dark at night unless you light it |
| Billboard | up to 40 x 40 | A printed billboard: its bottom row is a catwalk with floodlights, and the ad starts above it |
| Digital Billboard | up to 40 x 40 | An LED screen that fades between ads and goes black when switched off |
| Ad Kiosk | 2 x 4 | A backlit pavement kiosk at true size, with an ad on each side |
| Large Ad Kiosk | 4 x 8 | The same kiosk, well over life size |

## Building one

Place the block and its screen opens. For a poster board or billboard, pick a size preset or set
the width and height, and choose which way it grows from the block you placed -- to the left, to
the right, or both ways. A yellow outline shows where the board will go. Press **Done** and the
board is built; if something is in the way, you are told what and where. A kiosk has one size
and builds itself as soon as it is placed.

Right-click any block of a board to open its screen again, change its ads or resize it. Break any
block and the whole board comes down.

Billboards don't come with a support: stand them on a pole (a **Thick Traffic Pole** works well)
or build your own structure.

!!! tip "Survival"

    Every block of a poster board or billboard costs one of its item: growing a board takes them
    from your inventory, shrinking it gives them back, and breaking it drops them all. A kiosk is
    one item. All of them are made at the CSM Fabricator.

## What it shows

- **One ad**, or **all ads** or **one category** in turn or shuffled, changing every 5 seconds to
  10 minutes. Every player sees the same ad at the same moment.
- **Change:** cut, fade, or scroll (the next ad rolls up from below, like a real scroller kiosk).
- **Fit:** fill the board (cropping the edges), show the whole ad, or stretch it.
- **Light:** unlit, always lit, lit at night, or lit by a redstone signal at the placed block.
- **Back** (billboards and kiosks): plain, the same ad, or a different one.

The ads come in categories -- food, drinks, cars, civic notices and so on -- plus **Vintage**,
eighteen real print ads from 1900 to 1910, and **Server** for your server's own.

## Your server's own ads

Put PNG images in the server's `config/csm/ads/` folder and restart it. They appear under the
**Server** category for everyone, and players download each one only when a board shows it,
keeping a copy so they don't download it again. Up to 64 images, each at most 1 MB and 1024
pixels on a side; anything else is skipped with a note in the server log.

`config/csm_signage.cfg` can turn this off on the server (`allowServerAds`), and a player can
refuse server images on their own client (`acceptServerAds`), in which case those boards show a
"Your Ad Here" placeholder instead.
