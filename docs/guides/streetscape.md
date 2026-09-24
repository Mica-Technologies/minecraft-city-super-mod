# Streetscape

Everything on the sidewalk between the curb and the buildings: covers, utility boxes, bollards,
hydrants, news racks, mailboxes and parking meters. It's the **Streetscape** tab of the
**CSM: Roads & Traffic** module.

Everything in the tab settles onto sloped and partial-height road and sidewalk blocks, the way the
work zone cones and drums do, so a hydrant on a sloped sidewalk stands on the slope rather than
floating above it.

## Covers

Manhole covers marked for each utility (plain, sewer, storm, water, gas, electric, telecom), pull
boxes, a fiber optic handhole, water meter and valve boxes, a gas valve box, a sewer cleanout,
catch basin and gutter inlet grates, a trench drain, and the storm drain marker plaque. Each lies
flat on the surface below it and turns to face you when placed. Nearly all of them also come as a
separate **(Rusted)** block.

## Utility boxes

Pad-mount transformers in three sizes plus a three-phase unit, utility pedestals (square and
round, tall and short), telecom pedestals and enclosures, and buried utility markers. The green
metal boxes also come **rusted**.

A box up to two blocks a side is **one unit**. Placing it places the whole unit, or nothing if
something is in the way (it tells you where). Breaking any block of it breaks the whole unit and
drops one item.

Transformers, the ribbed telecom pedestal and the pay-by-phone sign carry an **ID number**. Right-click
to edit it.

## Bollards

A pipe bollard with a yellow sleeve, a stainless steel bollard, a crash-rated bollard, cast-iron
bollards in black and dark green, and the red **concrete sphere** found at store entrances. The
**flexible delineators** (white and yellow) bend out of the way, so they can be walked and driven
through; every other bollard is solid. The Delineator Post, its yellow version and the Zebra
Delineator live here too.

## Hydrants and standpipe connections

The original fire hydrant, and dry-barrel hydrants in six colour schemes. Yellow hydrants come
with blue, green, orange or red caps, and red hydrants with white or silver caps, following the
NFPA flow colour code. There is also a **wall hydrant** and a **sidewalk standpipe connection**.
For standpipe runs up a building or along a bridge, see
[Emergency Services & Fire Protection](emergency-services.md#connectable-standpipe).

## News racks

Coin-operated racks in four colours and a free-paper rack, all on a half-block stand. Racks placed
side by side facing the same way join into one bank. The bank's plinth and top rail run straight
through, and only the two ends get end panels.

## Mailboxes

Mailboxes hold items, and they are locked.

| Mailbox | Compartments |
|---|---|
| **Collection Mailbox** (blue, UIA MAIL) | One large compartment (27 stacks) |
| **Cluster Mailbox** (gray or bronze), two blocks | Boxes 1–12 (9 stacks each) and two parcel lockers (27 each) |
| **Curbside Mailbox** (black, green or white), on a post | One compartment (9 stacks). The red flag goes up while it holds mail |
| **Apartment Mailbox Bank** (aluminum or brass), on a wall | Boxes 1–6 (9 stacks each) |

**Who owns what.** The player who places a mailbox owns every compartment in it. On a
compartment's screen, the owner (or an operator) can type a player's name and press **Assign**
to hand the compartment over, or press **Free** so the next player to open it can **Claim** it.
The player must have been on the server before.

**Posting.** Right-click a compartment's door. On a cluster box or wall bank, the door you're
looking at is the one that opens.

- **Your own compartment** opens to show what's inside, and you can take things out.
- **Someone else's compartment** shows one slot to post through. Anything you put in goes
  straight into their compartment. You can't see what's already inside. Clicking the door with an
  item in hand posts that item straight away.
- A **free** compartment has to be claimed before it takes any mail.

If the owner is online they get a chat message when mail arrives.

**Hoppers.** A hopper or pipe can post into a mailbox that has only one compartment (the
collection box and curbside boxes), but can't take anything out of any mailbox.

**Protection.** Only the placer or an operator can break a mailbox, whichever block of it is hit,
and explosions don't harm it. Breaking it drops everything it held.

## Parking meters and pay stations

Mechanical and digital meters, single or double head, a multi-space **pay station**, and a
**pay-by-phone** sign.

**Paying.** Right-click a meter with an empty hand to open its screen. It shows the time left and
lets you pay one block of time, an hour, or up to the maximum. On a meter that takes emeralds,
clicking the head with emeralds in hand pays for one block of time on that head. Paid time is
**real time**: it keeps running while the chunk is unloaded or the server is down, like real
parking.

**Money.** When the server runs SUM (the UIA server utility mod) with its economy on, and SUM's
`economy_integration.allowedMods` lets in `csm_roads`, meters charge dollars from the player's
wallet. Otherwise they take emeralds. SUM is never required.

**The owner's settings.** The player who placed a meter (or an operator) sets the rate, the length
of one block of time, the maximum time, and where the takings go. Takings either vanish or are
kept for the owner to collect from the screen. Server defaults and caps are in the `parking`
section of the CSM config. A pay station also sets how many numbered spaces it sells.

**Redstone.** A meter gives a redstone signal while any of its heads has run out, including a head
that was never paid. Wire it to a lamp or a note block to flag expired spaces. Pay stations give
no signal, since their spaces aren't physical.
