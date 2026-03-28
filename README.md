# City Super Mod

The City Super Mod is a Minecraft mod that adds a variety of new realistic blocks and items to your
world. Open-source and free to use, the City Super Mod is designed to be compatible with other mods
and provide a realistic city-building experience in Minecraft.

![GitHub Release](https://img.shields.io/github/v/release/Mica-Technologies/minecraft-city-super-mod?sort=semver&display_name=tag&style=for-the-badge&logo=GitHub)
![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/Mica-Technologies/minecraft-city-super-mod/latest/total?style=for-the-badge&label=Downloads)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/Mica-Technologies/minecraft-city-super-mod/total?style=for-the-badge&label=Downloads%20(All))

## Wiki/Player Information

Information about the City Super Mod can be found on
the City Super Mod
Wiki [https://github.com/Mica-Technologies/minecraft-city-super-mod/wiki](https://github.com/Mica-Technologies/minecraft-city-super-mod/wiki).

Wiki topics include:

- Supported Minecraft Versions
- Player and Developer Safety
- Downloading and Running the City Super Mod
- Help and Support
- Website and Discord

## Developer Information

### IDE

The preferred development environment/IDE for the City Super Mod
is [IntelliJ IDEA](https://www.jetbrains.com/idea/download). It is recommended to use the latest
version of IntelliJ IDEA for the best development experience.

### Making Changes

To make changes to the City Super Mod, you will need to clone the project from the GitHub repository and open it in your preferred IDE.

#### Getting Started (IntelliJ IDEA)

1. **Open the Project**:
   - Clone the repository and open the project in IntelliJ IDEA.
2. **Install the Project SDK**:
   - Open `File` > `Project Structure` > `Platform Settings` > `SDKs`.
   - Click the `+` button and select `Download JDK`.
   - Select `17` as the version and `Azul Zulu Community™` as the vendor.
   - Leave the location as default and click `Download` to download and install the JDK.
   - Once installed, go to `Project Settings` > `Project` and set the `Project SDK` to the newly installed JDK 17, if not already set.
3. **Import Gradle Project**:
   - If prompted, import the Gradle project. If not prompted, you can manually import it by going to `View` > `Tool Windows` > `Gradle`, then clicking the `Sync all Gradle Projects` button in the Gradle tool window.
   - Wait for the Gradle import to complete.
   - If you experience any issues during the Gradle import, try refreshing the Gradle project or invalidating caches and restarting IntelliJ IDEA (`File` > `Invalidate caches`).
4. **Setup Workspace**:
   - In IntelliJ, locate the `1. Setup Workspace` run configuration and run it. This will set up the Minecraft mod workspace using the GregTechCEu Buildscripts.
   - Wait for the setup to complete before making changes or running the mod.

To learn more about the version control system, Git, please see [https://git-scm.com/doc](https://git-scm.com/doc).

If you are using IntelliJ IDEA as your preferred IDE, you may use the built-in Git integration to clone the project from the GitHub repository. To learn more about using Git integration with IntelliJ IDEA, please see [https://www.jetbrains.com/help/idea/using-git-integration.html](https://www.jetbrains.com/help/idea/using-git-integration.html).

#### Adding a Block

Adding a block looks different depending on the blockstate format, model format, or other
features. In general, the following steps should be followed:

- Create a new block class file within `src/main/java/com/micatechnologies/minecraft/csm`.
    - Block classes shall extend an appropriate base class, listed below.
    - Block classes shall be named in `CamelCase`.
    - Block classes shall be placed in the appropriate package based on the inventory tab.
    - Block classes shall contain a unique registry name in the format `snake_case`.
- Create a blockstate file in `src/main/resources/assets/csm/blockstates`.
    - Blockstate files shall be named using the block's registry name.
    - Blockstate files shall reference the block's model and any additional properties.
- Create or copy any required base block models to `src/main/resources/assets/csm/models/custom`.
    - Block model base files shall use a descriptive name.
    - Block model base files shall be referenced by block models, not blockstate files.
    - Block model base files shall only be used when necessary to reduce duplication.
- Create or copy any required block models to `src/main/resources/assets/csm/models/block`.
    - Block models shall be named using the block's registry name.
    - Block models shall reference the block's texture(s) and any additional properties, or any
      block model base file(s), if applicable.
- Create or copy any required texture files to `src/main/resources/assets/csm/textures/block`.
    - Texture files shall be named using a descriptive name.
    - Texture files shall be referenced by block models.
    - Texture files shall be PNG format and have a power of two resolution (16x16, 32x32, 64x64,
      128x128, etc.).
- Create a block lang entry in `src/main/resources/assets/csm/lang/en_us.lang`.
    - Block lang entries shall be named using the block's registry name.
    - Block lang entries shall contain a human-readable name for the block.

If the blockstate file does not define an `inventory` variant, an item model must be created for the
block to properly be displayed in the inventory or player's hand. If required, create an item model
within `src/main/resources/assets/csm/models/item` which references the block model.

- Block item models shall be named using the block's registry name.
- Block item models should reference the block model, if possible.
    - If the block model is suitable for use as an item model, the item model should reference the
      block model. For example:
      ```json
      {
        "parent": "csm:block/[block_registry_name]"
      }
      ```
    - If the block model is not suitable for use as an item model, a separate item model may be
      created.

##### Example Block Class (Simple Non-Rotatable Block)
```java
public class BlockExampleBasic extends AbstractBlock {
    // Constructor, material, sound, tool, harvest level, hardness, resistance, light, light opacity
    public BlockExampleBasic() {
        super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    }

    @Override
    public String getBlockRegistryName() {
        return "example_basic";
    }
}
```

##### Example Block Class (Rotatable Block with Custom Bounding Box)
```java
public class BlockExampleRotatable extends AbstractBlockRotatableNSEWUD {
    public BlockExampleRotatable() {
        super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    }

    @Override
    public String getBlockRegistryName() {
        return "example_rotatable";
    }

    @Override
    public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0, 0.0, 0.8, 1.0, 1.0, 1.0);
    }

    @Override
    public boolean getBlockIsOpaqueCube(IBlockState state) { return false; }

    @Override
    public boolean getBlockIsFullCube(IBlockState state) { return false; }
}
```

##### Block Base Classes

- `AbstractBlock`: The base class for all blocks in the City Super Mod, including the base classes
  below.
- `AbstractBlockFence`: The base class for all blocks in the City Super Mod that are fences.
- `AbstractBlockStairs`: The base class for all blocks in the City Super Mod that are stairs.
- `AbstractBlockSlab`: The base class for all blocks in the City Super Mod that are slabs.
- `AbstractBlockSetBasic`: The base class for all blocks in the City Super Mod that generate as a
  set of basic blocks (fence, stairs, slab).
- `AbstractBlockRotatableNSEW`: The base class for all blocks in the City Super Mod that are
  horizontally rotatable (north, south, east, west).
- `AbstractBlockRotatableNSEWUD`: The base class for all blocks in the City Super Mod that are
  horizontally and vertically rotatable (north, south, east, west, up, down).
- `AbstractBlockRotatableHZEight`: The base class for all blocks in the City Super Mod that are
  horizontally rotatable in eight directions (north, northeast, east, southeast, south, southwest,
  west, northwest).
- `AbstractPoweredBlockRotatableNSEWUD`: The base class for all blocks in the City Super Mod that
  are powered and are horizontally and vertically rotatable (north, south, east, west, up, down).

#### Adding an Item

Adding an item looks different depending on the model format, item type, or other features. In
general, the following steps should be followed:

- Create a new item class file within
  `src/main/java/com/micatechnologies/minecraft/csm/item`.
    - Item classes shall extend an appropriate base class, listed below.
    - Item classes shall be named in `CamelCase`.
    - Item classes shall be placed in the appropriate package based on the item type.
    - Item classes shall contain a unique registry name in the format `snake_case`.
- Create or copy any required base items models to `src/main/resources/assets/csm/models/custom`.
    - Item model base files shall use a descriptive name.
    - Item model base files shall only be used when necessary to reduce duplication.
- Create an item model in `src/main/resources/assets/csm/models/item`.
    - Item models shall be named using the item's registry name.
    - Item models shall reference the item's texture and any additional properties, or any item
      model base file(s), if applicable.
- Create or copy any required texture files to `src/main/resources/assets/csm/textures/item`.
    - Texture files shall be named using a descriptive name.
    - Texture files shall be referenced by item models.
    - Texture files shall be PNG format and have a power of two resolution (16x16, 32x32, 64x64,
      128x128, etc.).
- Create an item lang entry in `src/main/resources/assets/csm/lang/en_us.lang`.
    - Item lang entries shall be named using the item's registry name.
    - Item lang entries shall contain a human-readable name for the item.

##### Example Item Class
```java
public class ItemExample extends AbstractItem {
    public ItemExample() {
        super(0, 64); // maxDamage, maxStackSize
        setRegistryName("example_item");
        setTranslationKey("example_item");
    }
}
```

##### Item Base Classes

- `AbstractItem`: The base class for all items in the City Super Mod.
- `AbstractItemSpade`: The base class for all items in the City Super Mod that are shovels/spades.

#### Registering a Block or Item

After creating a block or item class, the block or item must be registered with the game. To
register a block or item, an entry must be added to the appropriate tab class file within the
`src/main/java/com/micatechnologies/minecraft/csm/tabs` package folder.

- If the block or item is intended to be unlisted, it shall be registered with the `CsmTabNone` tab
  class file.
- If a new tab is required for the block or item, a new tab class file shall be created using the
  process outlined in the [Adding a Tab](#adding-a-tab) section below.

#### Adding a Sound

Sounds must be registered in three places:

1. **Add the `.ogg` file** to `src/main/resources/assets/csm/sounds/`
   - Sound files must be in OGG Vorbis format.
   - Use `snake_case` naming (e.g., `example_alarm_tone.ogg`).
   - For horn/alarm sounds, target ~9,900 burst RMS for consistent volume across devices.

2. **Register in `sounds.json`** (`src/main/resources/assets/csm/sounds.json`):
   ```json
   "example_alarm_tone": {
     "category": "block",
     "sounds": [
       {
         "name": "csm:example_alarm_tone",
         "stream": false
       }
     ]
   }
   ```
   - The key (`"example_alarm_tone"`) becomes the sound event ID, referenced in code as
     `"csm:example_alarm_tone"`.
   - Use `"stream": false` for short sounds (horns, chimes). Use `"stream": true` for long
     sounds if needed (to avoid loading the entire file into memory).

3. **Register in `CsmSounds.java`** (`src/main/java/.../CsmSounds.java`):
   ```java
   EXAMPLE_ALARM_TONE("example_alarm_tone"),
   ```
   - Add an enum entry matching the sound event ID from `sounds.json`.
   - The enum name is `UPPER_SNAKE_CASE`, the string parameter matches the `sounds.json` key.

Sounds are automatically registered during mod initialization via `CsmSounds.registerSounds()`.
No additional registration code is needed beyond these three steps.

#### Adding a Tile Entity

Tile entities store per-block data beyond what fits in the 4-bit block metadata. To add one:

1. **Create a tile entity class** extending `AbstractTileEntity` (non-ticking) or
   `AbstractTickableTileEntity` (ticking):
   ```java
   public class TileEntityExample extends AbstractTileEntity {
       private int myData = 0;

       @Override
       public void readNBT(NBTTagCompound compound) {
           myData = compound.getInteger("myData");
       }

       @Override
       public NBTTagCompound writeNBT(NBTTagCompound compound) {
           compound.setInteger("myData", myData);
           return compound;
       }
   }
   ```

2. **Implement `ICsmTileEntityProvider`** on the block class:
   ```java
   public class BlockExample extends AbstractBlock implements ICsmTileEntityProvider {
       @Override
       public Class<? extends TileEntity> getTileEntityClass() {
           return TileEntityExample.class;
       }

       @Override
       public String getTileEntityName() {
           return "tileentityexample";  // Must be unique across all tile entities
       }

       @Override
       public TileEntity createNewTileEntity(World worldIn, int meta) {
           return new TileEntityExample();
       }
   }
   ```

Tile entities are automatically registered during mod initialization when the block implements
`ICsmTileEntityProvider`. No manual registration step is needed.

**Note on block metadata limits:** Minecraft 1.12.2 block meta is 4 bits (0-15). With NSEWUD
rotation using 6 values, only `floor(15/6) + 1 = 2` additional property values fit in meta. For
blocks that need more states (e.g., more than 2 selectable sounds), use a tile entity to store
the extra data instead of block meta.

#### Adding a Tab

Adding a tab is a fairly simple process. To add a tab, follow these steps:

- Copy/duplicate an existing tab class file within
  the `src/main/java/com/micatechnologies/minecraft/csm/tabs`
  package folder.
    - It is recommended to copy the `CsmTabNovelties` tab class file, as it is the simplest tab
      class file.
- Ensure the copied/duplicated tab class file has a unique name in `CamelCase`.
- Update the value of the `CsmTab.Load` annotation to reflect the new tab's load order.
    - The load order value should be positive, unique and sequential, as it determines the order in
      which tabs are loaded and displayed in-game.
- Update the values of the following methods in the tab class file:
    - `getTabName()`: The human-readable name of the tab.
    - `getTabIcon()`: The block or item that represents the tab in the creative inventory.
    - `getTabSearchable()`: Whether the tab is searchable in the creative inventory.
    - `getTabHidden()`: Whether the tab is hidden from the creative inventory.
- Populate the `initTabElements()` method with the blocks and items that should be displayed in the
  tab.
    - Ensure cleanup/removal of any existing blocks or items from the copied/duplicated tab class
      file.
    - Blocks and items shall be added to the tab using the `initTabBlock()` and `initTabItem()`
      methods, respectively.
    - Blocks and items shall be added in the order they are to be displayed in the tab.

##### Example Tab Class
```java
@CsmTab.Load(order = 11)
public class CsmTabExample extends CsmTab {
    @Override
    public boolean getTabHidden() { return false; }
    @Override
    public String getTabId() { return "tabexample"; }
    @Override
    public Block getTabIcon() { return ModBlocks.EXAMPLE_BLOCK; }
    @Override
    public boolean getTabSearchable() { return true; }
    @Override
    public void initTabElements(FMLPreInitializationEvent event) {
        initTabBlock(BlockExample.class, event);
        initTabItem(ItemExample.class, event);
    }
}
```

#### Fire Alarm System Architecture

The fire alarm system is a major subsystem in the `lifesafety` package. It uses a custom
client-side `MovingSound` system for audio that follows the player with distance-based volume
attenuation.

##### Key Components

- **`BlockFireAlarmControlPanel`** / **`TileEntityFireAlarmControlPanel`** -- The central
  controller. Detects redstone input to activate alarms. Manages connected appliances and sends
  sound packets to players.
- **`AbstractBlockFireAlarmSounder`** -- Base class for all horn and horn strobe devices.
  Subclasses implement `getSoundResourceName()` to return their sound event ID.
- **`AbstractBlockFireAlarmSounderVoiceEvac`** -- Base class for voice evac speaker/speaker
  strobe devices. Returns `null` from `getSoundResourceName()` since voice evac audio is
  managed differently (single MovingSound per voice evac channel).
- **`FireAlarmVoiceEvacSound`** -- Client-side `MovingSound` that follows the player. Volume
  is calculated based on distance to the nearest speaker/horn position.
- **`FireAlarmSoundPacket`** / **`FireAlarmSoundPacketHandler`** -- Network packets for
  starting/stopping sounds on the client. Each sound type gets its own "channel" (voice evac,
  storm, or each unique horn sound resource name).

##### How Sound Playback Works

1. Server-side: `TileEntityFireAlarmControlPanel.onTick()` groups connected horn blocks by their
   `getSoundResourceName()` return value.
2. For each unique sound, it collects all horn positions and checks which players are in range.
3. It sends `FireAlarmSoundPacket.start(channel, soundName, hearingRange, positions)` to
   in-range players.
4. Client-side: `FireAlarmSoundPacketHandler` creates a `FireAlarmVoiceEvacSound` (MovingSound)
   per channel, which loops the sound and adjusts volume based on distance to nearest horn.
5. Multiple channels play simultaneously (e.g., different horn types + voice evac).

##### Adding a New Fire Alarm Horn Block

1. Create a class extending `AbstractBlockFireAlarmSounder`.
2. Implement `getSoundResourceName()` to return the sound event ID (e.g., `"csm:my_horn"`).
3. Follow the [Adding a Sound](#adding-a-sound) steps to register the sound file.
4. Follow the standard [Adding a Block](#adding-a-block) steps for blockstate, model, texture,
   lang, and tab registration.
5. The control panel will automatically detect and play the horn when connected.

For blocks with selectable sounds (more than 2 options), use a tile entity. See
`BlockFireAlarmGentexCommander3Red` for an example using `TileEntityFireAlarmSoundIndex`.

##### Sound File Standards for Code 3 Horns

Code 3 horn sounds should follow these targets for consistency:
- **Total length:** ~4.024 seconds
- **Burst alignment:** 3 bursts with onsets at ~0.040s, ~1.020s, ~2.000s
- **Volume:** ~9,900 burst RMS (measure during the first burst)
- **Format:** OGG Vorbis, `"stream": false` in sounds.json

### Developer Documentation

The `assets/docs/` directory contains in-depth technical documentation for the mod's major
subsystems:

- **[BLOCK_AND_ITEM_BASE_CLASSES.md](assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md)** --
  Comprehensive reference for every abstract block, item, and tile entity class. Covers
  constructors, rotation systems, meta encoding, registration, and choosing the right base
  class. Start here if you're adding any new content to the mod.
- **[FIRE_ALARM_SYSTEM.md](assets/docs/FIRE_ALARM_SYSTEM.md)** -- Complete guide to the fire
  alarm notification system: MovingSound architecture, channel-based audio, sound file
  standards, adding new horn blocks, and the full sound inventory.
- **[TRAFFIC_SIGNAL_SYSTEM.md](assets/docs/TRAFFIC_SIGNAL_SYSTEM.md)** -- Guide to the traffic
  signal controller system, signal phases, pedestrian signals, and sensors.
- **[LIGHTING_SYSTEM.md](assets/docs/LIGHTING_SYSTEM.md)** -- The 4-state on/off control
  system, light-up air projection, redstone vs manual control, and the AbstractBrightLight
  hierarchy.
- **[POWER_GRID_SYSTEM.md](assets/docs/POWER_GRID_SYSTEM.md)** -- Forge Energy integration,
  utility pole system, and decorative electrical infrastructure.
- **[TRAFFIC_SIGNS.md](assets/docs/TRAFFIC_SIGNS.md)** -- The 472-sign system: Forge
  blockstate format, dynamic DOWNWARD/SETBACK properties, shared base models, and how to add
  new signs.

The `assets/docs/agent_progress/` subdirectory contains work-in-progress tracking documents
used by AI coding agents (Claude Code). These track ongoing improvement efforts and contain
resume prompts for continuing work across sessions.

### Submitting Changes

After making changes, you can push your modifications to GitHub on a new branch. To protect the
working code, modification of the `main` branch is not permitted except through pull request.

## Credits

### Active Developers

<img src="https://minotar.net/armor/bust/Akselhok/100.png" width="50" alt="Akselhok Minecraft Avatar"/>

**Name:** Alex<br/>
**GitHub Username:** mica-alex<br/>
**Minecraft Username:** Akselhok


<img src="https://minotar.net/armor/bust/AngelWingsPanda/100.png" width="50" alt="AngelWingsPanda Minecraft Avatar"/>

**Name:** Brandon<br />
**GitHub Username:** AngelWingsPanda<br />
**Minecraft Username:** AngelWingsPanda

### Acknowledgments

The City Super Mod began as an [MCreator](https://mcreator.net) project. While the project has since been migrated to a standalone project, the development team would like to acknowledge the MCreator project and its developers for their contributions to the Minecraft modding community.

**Build System:**
This mod uses the [GregTechCEu Buildscripts](https://github.com/GregTechCEu/Buildscripts) for Minecraft 1.12. Special thanks to the GregTechCEu team for their build infrastructure and support.
