# Pawn Shop (Market & Store)

The Market & Store tab's pawn shop: server-run blocks that buy any item a player hands them, at
a value worked out from the item's recipes, paying money through the optional SUM economy or
emeralds without it. It is the shops' other half: a shop sells what a player stocks it with, a
pawn shop takes anything at the server's price. CSM never depends on SUM.

Code: `modules/furnishings/.../furniture/market/pawn/`. Models: `gen_furniture_market.py` (the
Shops group). The economy bridge is Core's `codeutils/economy/` (see `SHOP_SYSTEM.md`).

## The blocks

| Registry | Class | What it does |
|---|---|---|
| `pawn_counter` | `BlockPawnCounter` (a `BlockDisplayCase`) | The pawn screen: nine offer slots, a quote per slot and a total, Pawn. Can resell what it buys. Joins other pawn counters. |
| `pawn_drop_bin` | `BlockPawnDropBin` | A click with an item pawns the stack held, no screen. A player who claims it is paid for what hoppers feed it. |
| `pawn_price_kiosk` | `BlockPawnPriceKiosk` (a `BlockMarketTall`) | Says what the stack held fetches, and one of it. Buys nothing. |
| `pawn_neon_sign` | `BlockPawnNeonSign` | Decorative PAWN sign for a window or counter, light 9. |
| `pawn_jewelry_case` | `BlockCounterPiece` | Decorative counter-top case of rings and watches. |
| `pawn_guitar_wall` | `BlockBathroomFixture` | Decorative: three guitars on pegboard against a wall. |
| `we_buy_gold_sign` | `BlockBathroomFixture` | Decorative hanging sign. |

**Server-run only.** The counter and the bin pay from nothing, so only
`csm.pawn.admin` (op level 2, or a permissions mod's grant) places or breaks one: a pawn block a
player could put down anywhere would be their own money tap. A refused placement takes the
block back up and returns the item (`PawnBlocks.placed`); a refused break gets no dig progress.
The kiosk pays nothing, so anyone may place it. Pawn blocks resist explosions as the shops do
(2000).

## What an item is worth (`PawnValuation`, `PawnValueSolver`)

An item's value, in money, is the first of:

1. its entry in `config/csm/pawn_values.json`'s `overrides`;
2. what its cheapest recipe costs, starting from the file's `raw` values;
3. the config's `defaultValue` (an item with no recipe and no entry: other mods' raw materials,
   creative-only items). `0` refuses such items instead.

The file is written with defaults on the first start (a hundred-odd raw vanilla items: logs,
stone, ores and their drops, mob drops, crops, found items) and from then on is the server's own;
a later CSM version never rewrites it. Keys are `modid:item` (every subtype) or
`modid:item@meta`. Its `blocked` list is never taken (bedrock, command blocks, spawn eggs,
structure blocks...).

**The recipe walk** runs once when the server has started (every mod's recipes are registered
by then) and on `/csmpawn reload`. With the pawn shop disabled (`pawn.enabled=false`) it is
skipped at start, and the file read and the walk wait for the first quote or `/csmpawn` command
that needs a value. Its edges are every `IRecipe`, every furnace recipe (plus
`smeltFuelCost`), every CSM block's Fabricator cost (`CsmFabricatorCosts.getCost`), and two
in-world conversions with no recipe of their own (concrete from powder, dyed shulker boxes).

- An item takes its **cheapest** way to be made, and each recipe slot its cheapest choice (ore
  dictionary slots included). Nothing is then worth more than another way to make it, so
  crafting the expensive way and pawning makes no profit.
- Values only fall as passes run, so loops (a block and its nine ingots, wool and string) settle.
  A loop that loses value each way round drifts towards nothing (the safe direction) and stops at
  the pass limit (100).
- A container a recipe hands back (a bucket) is subtracted.
- An input nothing makes and nothing fixes gets the default, and the walk runs again.

`PawnValueSolver` knows nothing of Minecraft (keys and recipes), so `PawnValueSolverTest` holds
it offline. On the dev client with every module it values about 4,700 items from 4,900 recipes
in under 100 ms; 96% of CSM's items price from their parts.

**A quote** is value x `payoutFraction` (0.5) x count, rounded down to a cent, with:

- worn tools scaled by durability left;
- `enchantValuePerLevel` added per enchantment level (enchanted books too);
- refused: blocked items, a container still holding items (a filled shulker box, a backpack),
  anything worth nothing.

Keys ignore NBT, so every potion is one value (the file can set it).

## Paying (`PawnEconomy`, `PawnTrade`, `PawnLedger`)

Money goes through SUM when it is installed and allows `csm_furnishings` (the shops' own link,
`ShopEconomy.SUM`; `wallet_write` is enough). Without it, emeralds at `moneyPerEmerald`: what a
payout does not make up to a whole emerald is kept per player and paid once it adds up, so
selling cheap things still earns something.

`PawnTrade.pawn` checks everything before anything moves:

1. Are pawn shops buying (`enabled`), and is the player not in creative (`allowCreative`)?
2. Quote every stack; refused stacks stay.
3. Take stacks in slot order while the total stays inside the player's allowance for the day
   (`maxPayoutPerDay`, whole stacks only, the server's local date).
4. Take them, pay, and put them all back if the payment is refused.
5. Record the day's total; hand each stack to a reselling counter's stock, or destroy it.

The **ledger** is the save's global data (`csm_pawn`): each player's emerald remainder, the day
and what they were paid on it, and what they are **owed**: a payout for an offline player (a
claimed bin fed by a hopper) is owed and paid at their next login (`PawnEvents`). It is all in
money, so adding or dropping SUM never reinterprets it.

## The counter (`ContainerPawnCounter`, `GuiPawnCounter`)

GUI id 60. The offer slots belong to the screen alone: what is left in them goes back to the
player when it closes. Quotes are the server's (only it has the values): whenever the offer
changes it sends `PawnQuotePacket` (a line per slot and a header, as JSON text components, so the
client shows them in its own language). A refused slot is tinted red; hovering any offer slot
shows its quote or refusal.

**Resale.** Those with `csm.pawn.admin` get a resale switch in the title row. With it on, the
counter keeps what it buys in its 27 slots (`TileEntityPawnCounter`, `pr`) and a Buy back button
opens GUI 61: a click buys one of a slot's items, a shift-click the stack, at value x
`resaleMarkup` (1.5), money or whole emeralds rounded up. The price is always above the payout,
so buying back and pawning again never makes money. The price goes nowhere: the counter is the
server's and keeps no till. Its stock is reached only through that screen; it offers hoppers no
item handler. An admin breaking the counter drops its stock.

## The drop-off bin (`TileEntityPawnDropBin`)

- A click with an item pawns the held stack, paid to whoever clicks (`PawnTrade.pawn` on one
  inventory slot).
- A sneaking click with an empty hand claims an unclaimed bin (not in creative unless
  `allowCreative`), or releases your own; pawn admins release anyone's.
- A hopper (or anything that inserts) feeds one virtual slot that never holds anything: each
  stack is quoted, and if it is taken and the claimer's daily allowance has room it is destroyed
  and paid to the claimer through `payOrOwe`. A hopper brings one item at a time, so the bin
  saves each item's fraction of a cent (`pp`) rather than rounding it away; dirt still counts and
  does not clog the hopper. Refused items stay in the hopper. A release forfeits the saved
  fraction. `binAcceptsHoppers` turns hopper feeding off.

## Commands (`/csmpawn`, op level 2)

| Command | Does |
|---|---|
| `value [hand\|item]` | What an item is worth, how it was found, what one and a stack pay |
| `explain [hand\|item]` | The cheapest recipe chain behind a value, a line a step |
| `set <hand\|item> <value\|clear>` | Fixes or clears an override in the file |
| `block <hand\|item>` | Stops or lets pawn shops take an item |
| `reload` | Reads the file again and works every value out again |
| `dump` | Writes every item's value to `csm_pawn_values.csv` in the server directory |
| `sell` | Pawns the held stack as a pawn shop would (for trying the payout) |
| `ledger [player]` | A player's emerald remainder, today's payouts, what they are owed |

`set` and `block` rewrite the file without any comments added by hand.

## Config (`pawn` category)

| Key | Default | What it does |
|---|---|---|
| `enabled` | true | Off, pawn shops quote but buy nothing |
| `payoutFraction` | 0.5 | Share of the value paid |
| `resaleMarkup` | 1.5 | Resale price over the value |
| `defaultValue` | 1.0 | Value of an item with no entry and no recipe; 0 refuses them |
| `moneyPerEmerald` | 10.0 | The emerald rate without SUM |
| `smeltFuelCost` | 0.05 | Added per smelt in the recipe walk |
| `enchantValuePerLevel` | 2.0 | Added per enchantment level |
| `maxPayoutPerDay` | 0 | Most one player is paid a day; 0 for no limit |
| `allowCreative` | false | Whether creative players may pawn (or claim a bin) |
| `binAcceptsHoppers` | true | Whether a claimed bin takes hopper input |

## Traps

- **A refused placement and the client's count.** The item block takes its one after
  `onBlockPlacedBy`, so handing the item back leaves the server's count where it began and
  vanilla sends nothing; the client, which already showed one gone, is left one short. `PawnEvents`
  resends the inventory at the end of the tick.
- **A scripted click on the resale screen.** `getSlotUnderMouse` is the slot drawn last frame,
  stale for a click no mouse moved to; the screen buys in `handleMouseClick`, which gets the slot
  found at the click.
- **The market generator writes LF** into a CRLF checkout, so every market file shows as
  modified after a run; `git diff --ignore-cr-at-eol` shows which really changed.

## Testing

- Emerald mode needs nothing. Money mode: as `SHOP_SYSTEM.md` (SUM's dev jar, then
  `csm_furnishings=wallet_read,wallet_write` in `allowedMods`).
- Non-admin: a singleplayer owner is op while cheats are on; a copy of the world with
  `allowCommands` 0 in `level.dat` (and the pawn blocks already in the inventory) makes the
  owner a non-op.
- Hopper payout to an offline claimer, and its payment at login, is covered by the ledger code
  but has not been run with a second account.

## What was left out

- Player-run pawn shops: players already have the shops' buy-back price.
- Prices that fall as a server sinks lots of one item.
- Other mods' machine recipes (grinders, IV's part benches, the external road mod's
  workbench): their items take the default; a server sets the ones it cares about.
- Values by NBT (potion types, IV vehicles by definition).
