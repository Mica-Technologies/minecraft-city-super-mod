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

To make changes to the City Super Mod, you will need to clone the project from the GitHub repository
and open it in your preferred IDE.

To learn more about the version control system, Git, please
see [https://git-scm.com/doc](https://git-scm.com/doc).

If you are using IntelliJ IDEA as your preferred IDE, you may use the built-in Git integration to
clone the project from the GitHub repository. To learn more about using Git integration with
IntelliJ IDEA, please see
[https://www.jetbrains.com/help/idea/using-git-integration.html](https://www.jetbrains.com/help/idea/using-git-integration.html).

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

### Submitting Changes

After making changes, you can push your modifications to GitHub on a new branch. To protect the
working code, modification of the `main` branch is not permitted except through pull request.

## Credits

### Active Developers

<img src="https://minotar.net/armor/bust/Akselhok/100.png" width="50"/>

**Name:** Alex<br/>
**GitHub Username:** mica-alex<br/>
**Minecraft Username:** Akselhok


<img src="https://minotar.net/armor/bust/AngelWingsPanda/100.png" width="50"/>

**Name:** Brandon<br />
**GitHub Username:** AngelWingsPanda<br />
**Minecraft Username:** AngelWingsPanda

### Acknowledgments

The City Super Mod began as an [MCreator](https://mcreator.net) project. While the project has
since been migrated to a standalone project, the development team would like to acknowledge the
MCreator project and its developers for their contributions to the Minecraft modding community.



