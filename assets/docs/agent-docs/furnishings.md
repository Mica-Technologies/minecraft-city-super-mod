# Furniture & Novelties

The Furniture, Residential, Commercial & Office and Market & Store generators.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_produce_crates.py`, `gen_furnishings_gameroom.py`, `gen_novelties.py`,
  `gen_novelties_seasonal.py`, `gen_furnishings_showpieces.py` -- the Furniture & Novelties
  models rebuilt in 2026-09: produce crates; bar and game room pieces and the wooden barrel;
  props; seasonal figures; the OBJ showpieces (jukebox, piano, clock, swing chair, tree). Which block each draws
  is tabled in `assets/docs/NOVELTIES_SYSTEM.md`; none writes lang or tab lines; `--check`
- `gen_furniture_residential.py` -- the Residential tab (furniture round-out): tables, chairs, bar
  stools, bookcases, TV stands, sideboards in light oak, walnut and white; armchairs, sofas and
  sofa corners in four fabrics. Joining pieces use multipart blockstates driven by actual state
  (a model parent must be `csm:block/...`; a blockstate may drop the `block/`); writes its own
  lang lines by key; `--check`, `--fragments`
- `gen_furniture_kitchen.py` -- the Residential tab's kitchen, importing
  `gen_furniture_residential.py`'s helpers and finishes: base cabinets (doors, drawers, door and
  drawer, sink with a working tap), the corner base that turns a run,
  island, wall cabinets and open shelves, each finish with its countertop (the kitchen's own
  `WOODS` adds cherry under tan granite, which the cooktop cabinet, the dishwasher's counter and
  the bathroom vanity follow; `KitchenCountertop.TAN_GRANITE` in Java); range hoods and the under-cabinet light (lit
  lens by blockstate), the two-block refrigerator (drawn whole, cut at the block line into two
  models) and the chest freezer; `--check`, `--fragments`. `gen_furniture_sounds.py`
  synthesises the cabinet, drawer and refrigerator door sounds (numpy to ffmpeg to OGG)
- `gen_furniture_appliances.py` -- the kitchen's appliances and tableware, importing the
  residential and kitchen generators: range, wall oven, dishwasher (carrying the neighbouring
  cabinet's countertop), cooktop cabinet, the countertop appliances (microwave, toaster, air
  fryer, blender, coffee machine, kettle, stand mixer), cookie jar, chopping board, plates, mugs,
  a glass, cake stands, and the toast / smoothie / coffee item sprites. Each countertop piece is
  drawn once per surface it can stand on (`SurfaceRest`, dropped to sit on a counter or table),
  its UVs fitted before the drop; `--check`, `--fragments`. The appliances run on the
  `furniture.appliance` machine framework (NOVELTIES_SYSTEM.md, Residential Furniture)
- `gen_furniture_bedroom.py` -- the bedroom, study and nursery, importing the residential and
  kitchen generators: beds (single, double, king in four fabrics; day bed and bunk in the wood
  finishes; single, double and king also in forest green and in white with a forest-green
  throw, `BED_FABRICS`, the throw beds on their own base models) drawn whole in `BedLayout`'s
  frame and cut at the block lines into one model per cell, the two-block pieces (dresser with mirror, wardrobe, standing mirror, vanity, the
  joining white closet) cut into halves, the one-block storage and seats, and rugs whose border
  is drawn only on an open side (multipart `OR` corners); `--check`, `--fragments`. The mattress
  heights must match `BedLayout.java`
- `gen_furniture_bathroom.py` -- the bathroom, commercial restroom and laundry, importing the
  residential, kitchen, bedroom and appliance generators: toilet, pedestal sink, vanities that
  join under one top (own `KitchenLine`), mirror cabinets, the two-block bathtub (cut into cells
  like a bed, its water a part shown while full), the two-tall shower enclosure (translucent
  glass), shower head, towel rails, radiator, wastebaskets, bath mats (the bedroom's rug geometry
  in terry), urinal, dispensers, grab bar, the fold-down changing station, the washing machine
  and dryer (round door windows as cutouts, lit while running), iron, ironing board, baskets and
  laundry tub; and the commercial restroom: flushometer toilets (floor-mounted and wall-hung,
  manual and sensor) and urinals with the valve drawn once by `flush_valve()`, the waterless
  urinal and urinal screen, toilet partitions (`BlockToiletPartition`: door, pilaster and panel
  fronts at the outer edge of the block in front of the toilet, the stall panels on the block
  line reaching back into the toilet's block; pilaster and panel pieces are one-block cells
  that stack to any height, shoes on the bottom cell and headrail on the top, a cell on a
  door making a floor-to-ceiling stall with a transom; panel cells on the floor behind a front
  deepen a stall; pilasters beside a door widen it with no divider; the door stays two tall,
  its open pose written out swung about its hinge; see NOVELTIES_SYSTEM.md), the wall-hung lavatory and the joining trough sink with sensor faucets, two hand
  dryers, the jumbo roll and seat cover dispensers and the napkin bin; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the flush, flushometer flush, hand dryer, shower,
  washer, dryer and iron sounds
- `gen_furniture_office.py` -- the Commercial & Office tab, importing the residential, kitchen,
  bedroom and appliance generators: office desks, pedestals and the L-desk corner (their own
  `KitchenLine.DESK`, turned by `BlockKitchenCorner`), the reception desk (`RECEPTION`), filing
  cabinet, office shelving (the bookcase's parts with binders), conference table, cubicle panels
  (`BlockCubiclePanel`: arms and posts from the neighbours, stacking, a shelf per face), office
  seating and the waiting bench, the whiteboard, chalkboard and cork board (`BlockWallBoard`:
  join both ways into one N x M board, frame round the outside, tray on the bottom row, all from
  actual state) and projector screen, the school desk, teacher's desk and lockers, the school
  set (tablet-arm desk, stacking chair, lectern, podium, desk globe, pencil sharpener, pull-down
  map, wall flag, ceiling and overhead projectors on an AV cart, the cafeteria table that seats
  eight, the trophy case), the things on a desk (`BlockCounterLight` for the
  screens and the desk lamp), the copier (`OfficeAppliances`, a supply slot on the appliance
  framework) and a streamer's set; `--check`, `--fragments`. `gen_furniture_sounds.py`
  synthesises the copier's run, the locker door and the pencil sharpener
- `gen_furniture_living.py` -- the Residential tab's living extras: TVs (animated channel
  textures, one- and two-block, stand and wall, a tube TV), the disc-playing stereo, speakers,
  the playable upright piano and bench, the digital and wall clocks, photo frames, wall art,
  house plants, the fireplace, ceiling fan, lamps, candles, door mats, the linkable light switch
  and its hidden relay, the doorbell and the storage crate. Two-block pieces are cut into their
  blocks with spanned UVs so a picture stays whole; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the doorbell chime and the fireplace crackle
- `gen_furniture_outdoor.py` -- the Residential tab's outdoor and backyard section, importing the
  residential, bedroom, appliance and living generators: the patio set (a slatted table joining
  like the dining table, chairs, Adirondack chairs, two-block sun loungers, an outdoor sofa and
  corner, rugs), umbrellas (free-standing, two blocks, and one for a table's middle; a square
  canopy of strips narrowing to the top, open or furled), the gas and charcoal grills (the
  appliance framework), fire pits, a cooler, a stacking chimney, the picket fence and gate
  (vanilla fence and gate behaviour), stepping stones (layouts picked by position), string
  lights, the trampoline, the bounce castle (drawn whole by its root, -16..32; sixteen invisible
  parts carry its floor and walls), a diving board, a kiddie pool, float rings, pet furniture, a
  hose reel with a working tap and an outdoor wall light; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the grill sizzle and the trampoline boing
- `gen_furniture_market.py` -- the Market & Store tab, importing the residential, kitchen,
  bedroom, appliance and office generators and the Life Safety pixel font: refrigerated displays
  (two-block reach-in coolers and freezers and the open dairy case, cut at the block line with
  their explicit UVs kept; the island freezer and the curved-glass ice cream, deli and bakery
  cases, three facets at 0/22.5/45 degrees), all stocked from product sheets (four product
  columns a 32 px sheet, drawn bottom-aligned at their true height, and a sheet of their tops)
  and lit, the lit parts written twice (lit and unlit copies) for the multipart; gondola
  shelving stocked six ways and bare (bookcase-style stacking); produce stands (the produce
  crates' beds on a tilted board), the scale, bulk bins; the checkout lane (animated belt with
  its `.mcmeta`), registers, printer, card terminal, self-checkout, the customer service desk
  (the office's reception desk parts in store colours plus a sign), candy rack; carts, the cart
  corral, basket stacks, security gates, the numbered aisle sign, magazine rack, bottle return;
  the fresh departments: butcher and seafood cases on the deli case's section (so the three
  join as one counter, a scale on each), bread racks, the self-serve pastry case, the hot food
  case and the rotisserie oven, the flower bucket stand and floral cooler (bunches as crossed
  planes in octagon buckets), and the coffee station (coffee bar and cup counter that join, the
  brewer and fountain drink machine on the appliance framework, the fountain drink's sprite);
  the front of the store: pharmacy drop-off and pick-up counters (the reception desk's parts
  with a sign on a post), a stacking shelf wall and hanging PHARMACY and CONSULTATION signs (an
  Rx and a capsule, never a cross), the wall-hung tobacco case, the lottery dispenser and
  terminal (an invented lottery), the ice merchandiser, propane exchange cage and firewood rack,
  and the coin, photo and movie kiosks, every sign a 4:1 band of one of seven shared 64 px
  sheets (`SIGNS`); the merchandising: end caps (gondolas drawn deeper under a header, so they
  join and stack as gondolas), the sale tags and shelf talker a click hangs on a gondola's or an
  end cap's shelf edges (`tags`, from the shared `store_cards` sheet), pallet stacks, the wire
  bargain bin and cardboard dump bin, seasonal tables (one model, every season's sheet at the
  same column heights and its sign band 3 of its own sheet), the checkout's candy strip (a lane
  counter), the hanging department sign (click-cycled, a model per band and the blockstate
  picking band and sheet) and standing sale signs, and the apparel corner: clothes rails and a
  round rack (garments as cutout planes square to the rail), folded clothes tables, mannequins,
  dress forms and fitting rooms whose curtain a click draws; and the moved Verifone's blockstate
  with its model dropped onto each counter height; `--check`, `--fragments` (the whole tab body,
  moved crates included). `gen_furniture_sounds.py` synthesises the card terminal beep, the
  scanner beep, the cash drawer and the fitting room curtain
