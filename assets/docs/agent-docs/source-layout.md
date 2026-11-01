# Source layout

The package and asset tree of Core and the module trees, moved out of the repository `CLAUDE.md`,
which keeps a summary and the rules about shared assets and directory names.

```
src/main/java/com/micatechnologies/minecraft/csm/   # Core only
├── codeutils/        # Base classes, service registries, utilities (see below); light/ is the
│                    # light-up air every light places (Lighting's fixtures, Venues' light heads)
├── api/              # CsmEvent
├── materials/        # Survival crafting parts + the CSM Fabricator
└── tabs/             # Core's own tab (Materials)

modules/<name>/src/main/java/com/micatechnologies/minecraft/csm/
├── tabs/             # That module's creative tabs, incl. its hidden tab (same package name)
├── buildingmaterials/  (modules/building)
├── furniture/, novelties/  (modules/furnishings)
├── hvac/             (modules/hvac)
├── lifesafety/       # Largest: fire alarms, emergency lighting, exit signs
├── lighting/
├── powergrid/       (modules/powergrid, CSM: Utilities) utility poles and line hardware, fe/
│                    (Forge Energy blocks), services/ (building service meters, panels,
│                    labels and signs), water/ (the water tower, tanks, pump station),
│                    sewer/ (lift station, curb inlet, outfall, pond outlet, manholes),
│                    gas/ (the regulator yard), telecom/ (cabinets, ice bridge, monopole,
│                    small cell)
├── signage/         (modules/signage) ad kiosks, poster boards, billboards
├── parks/           (modules/parks) trees/ (log and leaves kit), planting/ (the tool and its
│                    generators), landscape/ (plantings), planters/ (planting in planters),
│                    amenities/ (the Parks tab)
├── technology/       # AV, phones, the iMac family; desk devices (BlockDeskDeviceFactory,
│                    BlockDeskScreenFactory) for the computers tab, network/ (cable
│                    trays, Micafi access points and outdoor radios),
│                    rack/ (racks, units, the baked rack model), school/ (clocks, PA, bells)
├── transit/         (modules/transit) fare/ (fare gates, vending machine, tickets, cards),
│                    stop/, shelter/, platform/ (station and platform fit-out),
│                    airport/ (terminal and airside pieces, the flight schedule),
│                    board/ (bus departure board and bay display),
│                    station/ (entrances, fare line railing and gate, line bullets,
│                    booth counter)
├── tts/              (modules/tts)
├── healthcare/      (modules/healthcare) wards/ (beds, headwalls, monitors, curtains, room
│                    furniture), nursecall/ (devices, dome lights, master panel, network),
│                    clinical/ (ICU, ED tracking board, nurses' station, ceiling-hung
│                    pieces), diagnostics/ (lab bench, morgue cabinet), patient/ (lecture
│                    seating, exercise stairs, mannequin), queue/ (kiosk, now-serving
│                    display, network), codeblue/, systems/ (gas alarm, infant security),
│                    wayfinding/ (the baked wayfinding panel); the CT/MRI unit, pair, run,
│                    screen and light base classes at the package root
├── venues/          (modules/venues) seating/ (stadium seats, aisle-end seats, step lights,
│                    handrail, glass guards, section and portal signs), arena/ (dasher boards,
│                    glass, curves, doors, the floors and painted markings, benches, table),
│                    av/ (centre-hung scoreboard, ribbon boards, banners, speakers, camera,
│                    catwalk; their renderers, editors and packets), concourse/ (counters,
│                    concession sign, menu board, machines, beer kiosk, turnstile, scanner,
│                    ticket, suite bar and rail, team store), ballpark/ (field anchors,
│                    painted cells and their layout table, mound, fixtures, nets, outfield
│                    wall and markers, foul pole, scoreboards), field/ (football and soccer:
│                    turf, anchors, painted cells, their tint and layout table, fixtures,
│                    end zone lettering, scoreboards), bowling/, lines/ (the courts' and
│                    track's line art engine: layouts, painter, anchors, painted cells, the
│                    baked model), courts/ (nets, fences, fixtures, the court scoreboard and
│                    lap counter), pool/ (starting block, pace clock, the venue sign text
│                    engine), golf/ (turf, edge rail), skate/ (skate element); the
│                    multi-block unit (hung or
│                    standing) and its part, and the module's sounds, at the package root
├── vehicles/        (modules/vehicles) IvPreemptSource; the IV pack is assets/csmvehicles/
├── streetscape/      (modules/roads) street fixtures that settle onto road surfaces
├── trafficaccessories/
├── trafficsignals/   # Crosswalk/pedestrian signals with redstone support
└── trafficsigns/     # Largest: 472 road sign blocks

src/main/resources/assets/csm/     # Core's share; each module has the same tree under
                                  # modules/<name>/src/main/resources/assets/csm, and every
                                  # file keeps its csm: path whichever jar ships it
├── blockstates/      # One JSON per block; prefer Forge format (forge_marker: 1)
├── models/block/     # Block model JSONs (base models referencing shared parents)
│   └── shared_models/  # Shared 3D geometry (Blockbench), organized by subsystem:
│       ├── hvac/            # 5 models
│       ├── lifesafety/      # 84 models
│       ├── lighting/        # 102 models
│       ├── novelties/       # 9 models
│       ├── powergrid/       # 42 models
│       ├── technology/      # 33 models
│       ├── trafficaccessories/ # 61 models
│       └── trafficsignals/  # 50 models
├── models/item/      # Item model JSONs (only for actual items, not block inventory)
├── recipes/          # Core only — tier-1 JSON recipes (parts + the Fabricator), parsed at
│                     # game start; a recipe for a module's item needs a forge:mod_loaded condition
├── textures/blocks/  # Organized by subsystem subfolder
├── textures/items/
├── sounds/           # Module trees only; each module also ships its own sounds.json
└── lang/en_us.lang   # Split by owner: Core keeps the parts, each module its own lines
```
