# Shops (Market & Store)

The Market & Store tab's working shops: shelving, a wall shelf, a display case, a cooler and a
vending machine that sell the real item a player puts in them, and can buy it back. They use the
optional SUM economy's money when SUM is installed and allows it, and emeralds otherwise. CSM
never depends on SUM.

Code: `modules/furnishings/.../furniture/market/shop/`. Models: `gen_furniture_market.py`.
The economy bridge is Core's: `src/main/.../codeutils/economy/`.

## The blocks

| Registry | Class | Base it looks like |
|---|---|---|
| `shop_gondola_shelf` | `BlockShopGondola` | the gondola, shelves bare |
| `shop_end_cap` | `BlockShopGondola` (deeper box) | the end cap |
| `shop_wall_shelf` | `BlockShopWallShelf` | new: store shelving on a wall |
| `shop_display_case` | `BlockShopDisplayCase` | a glass counter case |
| `shop_cooler` | `BlockShopCooler` | the reach-in cooler (2 tall) |
| `shop_vending_machine` | `BlockShopVendingMachine` | new: vending machine (2 tall) |

They are new registry names beside the decorative pieces, never a mode of them. A shop is a tile
entity with 27 slots and state in every block; the decorative pieces have none. Giving every
gondola already placed in every world a tile entity would have cost memory for nothing, and would
have taken the empty-hand click that hangs sale tags.

A shop gondola joins and stacks with every gondola and end cap, so a shop is one more bay in a run
of ordinary shelving. `BlockGondola.continues` and `BlockShopGondola.continues` each accept the
other. The shop gondola extends `BlockBookcase` directly, not `BlockGondola`, because
`BlockGondola`'s constructor sets `TAGS` on the default state, and a subclass that leaves `TAGS`
out of its state container crashes there.

## What a shop is (`TileEntityShop`)

- **Template:** one real stack in its own slot. It is the item for sale, it is never sold, and it
  drops when the shop is broken. Stock counts only stacks matching it exactly (item, metadata and
  NBT).
- **Stock:** 27 slots (`TileEntityShop extends TileEntityDisplayCase`, so the coolers' redstone
  light memory and the storage drops and comparator come with it).
- **Sale:** an amount (1 to 64) and a price.
- **Buy-back:** an amount and a price. Zero means the shop doesn't buy.
- **Till:** what a player shop has taken and not yet collected.
- **Owner:** UUID and name.
- **Switches:**
  - **Server shop:** no owner. What it's paid is destroyed, and what it pays out is made. This
    matches SUM's own server shop.
  - **Unlimited stock.**
  - **Decorative:** it shows its item and price but trades nothing.

Prices and tills are kept **per currency**, money (a double, in cents) and emeralds (whole) side
by side. The one in force is used. A server that adds or drops SUM finds every price as the owner
left it, never reinterpreted.

## Trading (`ShopTrade`)

Each trade checks everything it can before anything moves, then moves the payment, then moves the
goods. If the goods can't move after all, the payment goes back.

**Buy** (the customer buys a sale's worth):
1. Is the shop decorative or not set up?
2. Is the buyer its owner?
3. Is there enough stock?
4. Take the payment.
5. Take the stock (refund if short).
6. Add to the till (player shop only).
7. Give the items, dropping what doesn't fit.

**Sell** (the customer sells a buy-back's worth):
1. Does the shop buy, and does the customer hold the items?
2. Does the stock have room? (Unlimited stock needs no room; the items are destroyed.)
3. Can the till pay? (A server shop makes the money.)
4. Take the items.
5. Pay the customer (give the items back if the payment is refused).
6. Draw from the till.
7. Stock the items.

Only the owner collects the till, in person (**Withdraw**): an admin can manage anyone's shop but
never takes its takings. Anyone who manages a shop can top the till up for buy-backs (**+**).

SUM only pays and charges players who are online. So nothing is ever paid to an absent owner: the
till waits for them. Money can't drop as an item, so:
- An owner breaking a shop that has money in its till collects it automatically.
- Anyone else (an admin) can't break it until the till is collected.
- A shop with anything in its till can't be made a server shop, so an op flipping the switch
  never ends up holding the owner's takings.

Creative players pay like anyone else.

## Who may do what (`ShopPermissions`, `ShopBlocks`)

| Node (op level 2) | Allows |
|---|---|
| `csm.shop.admin` | open the setup of, and break, anyone's shop |
| `csm.shop.server` | make a shop a server shop, and set up or break one |
| `csm.shop.infinite` | turn on unlimited stock |

A permissions mod can grant or refuse each node, since they go through vanilla's
`canUseCommand(level, node)`.

**Clicks:**
- An owner's click opens the setup screen. An owner's sneak-click shows the counter as a customer
  would see it.
- Everyone else's click opens the counter.
- A server shop opens the counter for everyone. Its managers sneak-click with an empty hand to set
  it up, as with SUM's server shop. An admin reaches anyone's setup the same way.
- A shop with no owner (one placed by a command) belongs to the first player who clicks it.

**Breaking:**
- Only those who may manage a shop can break it. Others get no break progress.
- Shops have a blast resistance of 2000.
- The two-block shops run their checks in `onBlockHarvested` too, because vanilla calls it before
  `removedByPlayer`, and the tall base class clears the other half there. Without that, a refused
  creative break of the upper half still dropped the shop's contents.

**Hoppers:** they can put matching items into stock, but never take anything out. A two-block
shop (the cooler, the vending machine) holds its shop in the lower half, so a hopper feeds it
from beside or below that half; one over the top feeds nothing. Stock is
reached only through the setup screen; the plain storage screen (GUI 35) refuses a shop's tile
entity.

## Screens and packets

- **Setup (GUI 47):** `ContainerShopSetup` + `GuiShopSetup`, a three-row chest of stock with a
  panel beside it. The panel holds the template slot, the sale and buy-back fields, the till with
  Withdraw and +, and the switches. Unlimited and Server are shown only to those allowed. The
  container re-checks that the player may manage the shop every tick.
- **Counter (GUI 48):** `ContainerShopCounter` + `GuiShopCounter`. It shows the item, what a sale
  is and what a buy-back pays, the stock and the customer's balance, with Buy and Sell buttons. A
  manager also gets a Set up button. The container has no slots. It exists because FML opens a
  screen from the server only when there's a server-side container; a bare `GuiScreen` opened that
  way would also take the player's inventory window id.
- **`ShopActionPacket`** (client to server): every button. The server checks reach and the
  player's rights, and the shop clamps every number.
- **`ShopStatusPacket`** (server to one client): the player's balance (money or emeralds held),
  the exact stock, the till, and which switches they may use. It's sent as a screen opens and
  after every trade. The shop's own sync never carries the stock or the till.

## The goods on the shelves (`TileEntityShopRenderer`)

CSM's first in-world item renderer. Each block's `ShopDisplay` holds rows of places on its
shelves (`{x0, x1, y, z, size, count}`, in sixteenths, model space facing north) and the price
card's point. The numbers are printed into the tab line by the generator, from the same variables
it draws the shelves with, so an item stands on a shelf rather than near one.

- **How many places are filled:** one for every quarter stack held. Every place is filled when
  the stock is unlimited or the shop is decorative.
- **How items are drawn:** with the `FIXED` transform, as an item frame draws them, scaled to the
  row.
- **Cost:** the item's model is looked up once per change of template and kept on the tile
  entity. A plain item's whole shelf is then compiled into one display list (`CsmDisplayListCache`
  "shop goods"), keyed by the template stack and how many places are filled, and replayed every
  frame. The atlas and item lighting are set outside the list, under the rules in
  `TRAFFIC_SIGNAL_SYSTEM.md` ("Display lists: one texture, no cached state").
- **What can't be cached:** an enchanted item (its glint binds another texture and cached state)
  and a built-in renderer (a chest, a shield) are drawn item by item. Their cost is the same as
  vanilla item frames holding them.
- **Measured** (dev client, 64 full shop gondolas, 512 items in view, render work per frame):

  | Setup | Render work |
  |---|---|
  | Items off | 0.97 ms |
  | Every item drawn each frame (before the cache) | 4.56 ms |
  | Cached shelves | 1.78 ms, about 13 µs a shop |

  - Each price card in range costs about 37 µs (glyphs are drawn every frame).
  - Each shop of chests or enchanted items costs about 60 µs.
- **Distances:** items draw within `shops.itemRenderDistance` (default 32 blocks) and the price
  card within `shops.priceLabelDistance` (8).
- **Syncing:** the shop syncs only when the stock changes how full the shelf looks, or whether a
  sale can be made. A trade moves stock quietly and syncs once. A sync never rebuilds the chunk
  (`getBakedModelKey` is constant), since no baked model reads the shop.

## SUM, optionally (`CsmSumEconomy`)

Core's `codeutils/economy/CsmSumEconomy` is the one place CSM reaches SUM, and it does so by
reflection only. Each module makes its own instance under its own integration id:
- parking meters: `csm_roads` (`ParkingPaymentSum`);
- shops: `csm_furnishings` (`ShopEconomy.SUM`).

Each module acquires its handle in `FMLServerStartedEvent` and releases it in
`FMLServerStoppingEvent`. It must be server *started*: SUM attaches its economy in its own
starting handler and loads after CSM.

A server operator allows each id in SUM's `economy_integration.allowedMods`, for example:

```
csm_furnishings=wallet_read,wallet_write
```

Reading a balance (the counter screen) needs `wallet_read`. Without it, or without SUM, shops
take emeralds (`CsmEmeralds`), and one log line says why.

While SUM refuses, the bridge asks again at most every 30 seconds, whenever a shop or meter checks
which currency is in force. So an operator who allows the integration and runs
`/sum econ api reload` sees it take effect within half a minute, with no restart.

## Config (`shops` category)

| Key | Default | Side | What it does |
|---|---|---|---|
| `decorativeOnly` | false | server | every shop shows its item and trades nothing; sent to clients in each shop's sync |
| `maxMoneyPrice` | 1,000,000 | server | the most money a trade may ask or pay |
| `maxEmeraldPrice` | 576 | server | the most emeralds; a buyer must carry them loose |
| `itemRenderDistance` | 32 | client | how far away the goods are drawn |
| `priceLabelDistance` | 8 | client | how far away the price card is drawn |

## Testing

- **Emerald mode** needs nothing extra.
- **Money mode:**
  1. Copy SUM's `build/libs/*-dev.jar` into `run/mods` (MixinBooter is already there).
  2. Launch once to write `run/config/sum.cfg`.
  3. Add `csm_furnishings=wallet_read,wallet_write` to `allowedMods`, then relaunch.
  4. Give money with `/sum econ add <amount> <player>`.
  5. Remove the jar and `run/config/sum*` afterwards.
- **Non-op:** set `allowCommands` to 0 in the world's `level.dat`.
