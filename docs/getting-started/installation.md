# Installation

## What you need

| | |
|---|---|
| **Minecraft** | 1.12.2 |
| **Mod loader** | [Forge for 1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html) |
| **Java** | Whatever your Forge 1.12.2 profile already uses (Java 8) |

## The mod comes in pieces

City Super Mod ships as **CSM: Core plus optional modules**, one jar each. Core is mandatory —
every module requires it. Beyond that, install only the subsystems you actually want.

| Jar | What it adds |
|---|---|
| **CSM: Core** | Required. The crafting parts, the CSM Fabricator, and everything the modules build on. On its own it adds almost no blocks |
| **CSM: Roads & Traffic** | Traffic signals and controllers, span wire, mast arms, crosswalks, road signs and highway guide signs |
| **CSM: Life Safety** | Fire alarm appliances, control panels, emergency lighting, exit signs |
| **CSM: HVAC** | Thermostats, air handlers, ducting and vents |
| **CSM: Lighting** | Street and area luminaires, pendants and wall sconces |
| **CSM: Power Grid** | Utility poles, cross arms, insulators, transformers and other electrical infrastructure |
| **CSM: Technology** | Computers, servers, televisions, speakers, transit fare equipment |
| **CSM: Furniture & Novelties** | Indoor and outdoor furniture, arcade cabinets, decorative novelties |
| **CSM: Building Materials** | Block, stair, slab and fence sets |
| **CSM: Text to Speech** | The speech engine and the Redstone TTS block. **Also requires CSM: Technology** |

!!! warning "All from the same release, all the same version"

    Every module pins Core to its own exact version, so mixing versions is refused at startup
    rather than half-working. Take every jar from the same release — and when you update, update
    all of them together.

!!! tip "Want the whole mod?"

    Install all ten jars. That is the same content, in the same creative tabs, in the same order,
    as the mod had when it was a single jar. Modpacks that shipped the old single jar should list
    every jar they want instead.

## Steps

1. Install Forge for 1.12.2 and run it once, so it creates the `mods` folder.
2. Open the
   [Releases page](https://github.com/Mica-Technologies/minecraft-city-super-mod/releases/latest)
   and download **CSM: Core** plus each module you want. The release lists a `SHA256SUMS.txt` you
   can check the downloads against.

    [![Latest release](https://img.shields.io/github/v/release/Mica-Technologies/minecraft-city-super-mod?sort=semver&display_name=tag&style=for-the-badge&label=Latest%20Version)](https://github.com/Mica-Technologies/minecraft-city-super-mod/releases/latest)

3. Put **all** of the jars you downloaded in `mods`:

    === "Windows"

        ```
        %APPDATA%\.minecraft\mods
        ```

    === "macOS"

        ```
        ~/Library/Application Support/minecraft/mods
        ```

    === "Linux"

        ```
        ~/.minecraft/mods
        ```

4. Launch Minecraft with the Forge 1.12.2 profile.

!!! warning "Only from official locations"

    Download the mod only from the places listed on [Player & Developer Safety](safety.md).
    A jar from anywhere else can contain anything.

## Checking it worked

Open the mod list on the title screen: **CSM: Core** and each module you installed should be listed
by name.

Then open the creative inventory. With every module installed the mod adds **fourteen tabs** —
Building Materials, Furniture, Gaming, HVAC, Life Safety, Lighting, Materials, Novelties, Power
Grid, Road Signs, Technology, Traffic Accessories and Traffic Signals. With a subset, you get the
tabs belonging to the modules you installed, plus Materials from Core.

If a tab is missing, check the Forge log for a load error: `logs/latest.log` in your Minecraft
folder, searched for `csm`.

## Changing which modules you have

Adding a module later is just another jar in `mods`.

Removing one is the ordinary missing-mod situation: a world that already contains its blocks will
have ids Forge no longer knows, and it will say so on load. **Back the world up first.**

## Servers

The mod works on a dedicated server. Install the **same set of jars, at the same version**, into
the server's `mods` folder as well as the client's — signal logic runs on the server and the
rendering runs on the client, so a client with a module the server lacks (or the reverse) is not a
supported configuration.
