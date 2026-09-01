# Installation

## What you need

| | |
|---|---|
| **Minecraft** | 1.12.2 |
| **Mod loader** | [Forge for 1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html) |
| **Java** | Whatever your Forge 1.12.2 profile already uses (Java 8) |

## Steps

1. Install Forge for 1.12.2 and run it once, so it creates the `mods` folder.
2. Download the latest `.jar` from the
   [Releases page](https://github.com/Mica-Technologies/minecraft-city-super-mod/releases/latest).

    [![Latest release](https://img.shields.io/github/v/release/Mica-Technologies/minecraft-city-super-mod?sort=semver&display_name=tag&style=for-the-badge&label=Latest%20Version)](https://github.com/Mica-Technologies/minecraft-city-super-mod/releases/latest)

3. Put the jar in `mods`:

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

Open the creative inventory. The City Super Mod adds **fourteen tabs** — Building Materials,
Furniture, Gaming, HVAC, Life Safety, Lighting, Materials, Novelties, Power Grid, Road Signs,
Technology, Traffic Accessories and Traffic Signals.

If the tabs are missing, check the Forge log for a load error: `logs/latest.log` in your Minecraft
folder, searched for `csm`.

## Servers

The mod works on a dedicated server. Install it into the server's `mods` folder as well as the
client's — the versions must match, since signal logic runs on the server and the rendering runs on
the client.
