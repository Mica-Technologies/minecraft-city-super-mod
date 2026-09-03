# Building from Source

## Requirements

- **Java 17** (Azul Zulu Community recommended). The project uses Jabel so modern Java syntax
  compiles to a JVM 8 target.
- Around 3 GB of heap for decompilation, already set in `gradle.properties`.

The project ships its own Gradle wrapper, so no separate Gradle install is needed.

## Commands

```bash
# First time, or after a clean
./gradlew setupDecompWorkspace

# Build the mod — the Core jar plus one jar per optional module
./gradlew build

# Run a development client (every module jar on the classpath)
./gradlew runClient

# Run a development server
./gradlew runServer

# Tests (JUnit 5) — one suite across Core and every module
./gradlew test
```

## Modules

The mod builds as a mandatory **CSM: Core** jar plus nine optional module jars, from this one
repository: `build/libs/minecraft-city-super-mod-core-<version>.jar` and
`minecraft-city-super-mod-<module>-<version>.jar` (`./gradlew printModuleJarNames` lists them).
Core is the `src/main` source set; each module is a source set of its own under `modules/<name>`,
compiled against Minecraft and Core only, so the compiler is what stops Core depending on a module.

Run tasks take a property choosing which module jars go on the game's classpath. Core is always
loaded:

```bash
./gradlew runClient -PcsmRunModules=core            # Core alone
./gradlew runClient -PcsmRunModules=lighting        # Core + Lighting
./gradlew runClient -PcsmRunModules=roads,hvac      # Core + two modules
./gradlew runClient -PcsmRunModules=all             # the default
```

Running a module on its own is the only check that proves its assets are self-contained. See
`assets/docs/MODULE_SYSTEM.md` for the whole design.

Point `JAVA_HOME` at a Java 17 install when running from a shell:

=== "Windows"

    ```bash
    JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.18" ./gradlew build
    ```

=== "macOS"

    ```bash
    export JAVA_HOME=~/Library/Java/JavaVirtualMachines/azul-17.0.19/Contents/Home
    ./gradlew build
    ```

## Apple Silicon

Minecraft 1.12.2 ships LWJGL 2 with x86_64-only natives, so a dev client on Apple Silicon needs
help. There are two routes and only one of them currently gives a working window:

| Task | Result |
|---|---|
| `./gradlew runClient -Prosetta` | **The one that works.** Vanilla LWJGL 2 under Rosetta 2. Needs Rosetta (`softwareupdate --install-rosetta --agree-to-license`) and an x86_64 Java 8 JDK that Gradle can find. |
| `./gradlew runClient17` | arm64-native through lwjgl3ify. Launches and loads mods, but the window is currently broken on macOS — tiny and non-resizable, because the relauncher never applies the `-XstartOnFirstThread` handling GLFW requires there. |

## After a build

Compiling green is not the same as working. Check `run/logs/latest.log` for the failures that never
throw:

```bash
grep -iE "missing model|exception loading model|texture.*not found" run/logs/latest.log
```

A missing model or texture loads fine and renders as the purple-and-black placeholder in game, so
the log is the only place it shows up before you see it.
