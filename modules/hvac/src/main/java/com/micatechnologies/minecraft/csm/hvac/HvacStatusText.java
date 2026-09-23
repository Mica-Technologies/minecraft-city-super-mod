package com.micatechnologies.minecraft.csm.hvac;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The status block of the thermostat screens: what the system is doing and, when it cannot do
 * what is asked, why -- in the words a player needs to fix it.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
final class HvacStatusText {

  static final int COLOR_HEAT = 0xFFFF5555;
  static final int COLOR_COOL = 0xFF3399FF;
  static final int COLOR_OK = 0xFF33CC33;
  static final int COLOR_WARN = 0xFFFFAA00;
  static final int COLOR_BAD = 0xFFFF4444;
  static final int COLOR_INFO = 0xFF888888;

  /** One line of text and its colour. */
  static final class Line {
    final String text;
    final int color;

    Line(String text, int color) {
      this.text = text;
      this.color = color;
    }
  }

  private HvacStatusText() {
  }

  /**
   * The status lines for a thermostat, most important first; the screen shows as many as fit.
   *
   * @param units units in the system
   */
  static List<Line> lines(TileEntityHvacThermostatBase t, boolean primary, int units,
      int zones) {
    List<Line> out = new ArrayList<>();
    int flags = t.getStatusFlags();
    int mode = t.getCallingMode();
    int output = t.getOutputPercent();

    // What it is doing.
    if (!primary && (flags & HvacStatus.FLAG_NO_PRIMARY) != 0) {
      out.add(new Line("⚠ Not linked to a primary thermostat", COLOR_BAD));
    } else if (units == 0) {
      out.add(new Line("⚠ No heaters or coolers linked", COLOR_WARN));
    } else if (mode == HvacStatus.MODE_HEATING) {
      out.add(new Line("● Heating, output " + output + "%", COLOR_HEAT));
    } else if (mode == HvacStatus.MODE_COOLING) {
      out.add(new Line("● Cooling, output " + output + "%", COLOR_COOL));
    } else if (t.getBlockedMode() == HvacStatus.MODE_HEATING) {
      out.add(new Line("⚠ Too cold, and no heater linked", COLOR_WARN));
    } else if (t.getBlockedMode() == HvacStatus.MODE_COOLING) {
      out.add(new Line("⚠ Too warm, and no cooler linked", COLOR_WARN));
    } else if ((flags & HvacStatus.FLAG_WAITING) != 0) {
      out.add(new Line("● Waiting: system busy the other way", COLOR_WARN));
    } else {
      out.add(new Line("● Satisfied", COLOR_OK));
    }

    // Why it may not be working.
    if ((flags & HvacStatus.FLAG_NOT_ENCLOSED) != 0) {
      out.add(new Line("⚠ Room is open to the outdoors", COLOR_BAD));
    }
    if ((flags & HvacStatus.FLAG_TOO_LARGE) != 0) {
      out.add(new Line("⚠ Space too large to condition", COLOR_BAD));
    }
    if ((flags & HvacStatus.FLAG_WAITING_FOR_CHUNKS) != 0) {
      out.add(new Line("Room still loading", COLOR_INFO));
    }
    if ((flags & HvacStatus.FLAG_NO_POWER) != 0) {
      out.add(new Line("⚠ No power to the units", COLOR_BAD));
    }
    if ((flags & HvacStatus.FLAG_CAPACITY_LIMITED) != 0) {
      out.add(new Line("⚠ At full capacity: add units or vents", COLOR_WARN));
    }
    if ((flags & HvacStatus.FLAG_NO_VENTS) != 0) {
      out.add(new Line("⚠ No vents linked to this thermostat", COLOR_WARN));
    }
    if ((flags & HvacStatus.FLAG_ROOM_NOT_SERVED) != 0) {
      out.add(new Line("⚠ No vent in this room", COLOR_WARN));
    }
    if ((flags & HvacStatus.FLAG_UNITS_UNCONNECTED) != 0) {
      out.add(new Line("⚠ Rooftop units need vents", COLOR_WARN));
    }

    // Sizing and inventory.
    if (primary && t.getCapacityPercent() >= 0) {
      int cap = t.getCapacityPercent();
      out.add(new Line("Capacity " + (cap >= 1000 ? ">999" : String.valueOf(cap))
          + "% of load", cap < 100 ? COLOR_BAD : cap < 120 ? COLOR_WARN : COLOR_INFO));
    }
    StringBuilder info = new StringBuilder();
    info.append(t.getPoweredUnitCount()).append('/').append(units).append(" units powered | ")
        .append(t.getLinkedVentCount()).append(" vents");
    if (primary && zones > 0) {
      info.append(" | ").append(zones).append(zones == 1 ? " zone" : " zones");
    }
    out.add(new Line(info.toString(), COLOR_INFO));
    return out;
  }
}
