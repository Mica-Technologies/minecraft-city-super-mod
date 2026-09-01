# HVAC

Heating and cooling that actually simulates room temperature, rather than looking the part.

## How temperature works

Every position has a temperature in degrees Fahrenheit, calculated from the **biome** and then
modified by whatever HVAC equipment is running near it.

The biome baseline is deliberately extreme at the top end, so that coolers matter somewhere hot:

| Biome | Baseline |
|---|---|
| Ice Plains | −4 °F |
| Taiga | 18.5 °F |
| Forest | 41 °F |
| Plains | 68 °F |
| Jungle | 86 °F |
| Mesa | 131 °F |
| Desert | 176 °F |

Heaters, coolers and vent relays then contribute to that, weighted by distance — so a room warms
from the equipment in it, and the effect falls off as you walk away.

## Reading the temperature

Two places:

- **The HUD overlay**, which shows the temperature where you are standing.
- **Thermostat displays**, which are drawn on the block.

## Thermostats

A thermostat holds a setpoint and switches its equipment to reach it.

### It will not chatter

Two things stop a thermostat oscillating against its own equipment:

- **Thermal blending.** The reading eases toward the real temperature rather than snapping to it —
  about 76 seconds to cover 90% of a change. So the thermostat responds to the room, not to the
  draught from the vent beside it.
- **Hysteresis on the restart side.** It will not immediately re-fire the moment it drifts past the
  setpoint.

### Ramp-up

Equipment comes up in two phases rather than going to full output instantly.

## Zones

A building can have more than one zone. **Zone thermostats** carry their own setpoint and their own
vent network, so an office and a server room in the same building can hold different temperatures.

## Vents

Vents are the delivery end. Link them to a thermostat — primary or zone — and they extend that
thermostat's reach.

!!! tip "More vents is genuinely better"

    Each vent beyond the first adds a stacking **+2 °F** of capability to its thermostat. Ducting a
    large room properly is rewarded rather than merely decorative.

## Power

HVAC equipment uses **Forge Energy**, so it interoperates with any other mod that provides it, as
well as with CSM's own [power grid](power-grid.md).

## What is in the tab

Air handlers, condensers, heaters, coolers, thermostats, vents and ducting — 45 blocks, listed in
the [HVAC reference](../reference/hvac.md). The old wiki was missing 15 of them.
