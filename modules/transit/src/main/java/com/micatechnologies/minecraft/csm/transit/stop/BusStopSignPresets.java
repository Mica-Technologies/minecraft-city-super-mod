package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingArrow;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingIcon;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingMount;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingScheme;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;

/**
 * The full-size stop's signs' defaults and presets: the pole's flag ({@link BlockBusStopPoleFlag})
 * and the stop-name blade ({@link BlockBusStopBlade}). Both are Core's wayfinding panel, so every
 * option and the whole NBT are the panel's; the presets put each agency's livery on them, and the
 * editor's custom colours are a custom agency.
 *
 * <p>The flag's third line is its routes: the ids on it ("B 30", "12 / 40") are the stop's routes
 * for the boards and displays ({@link TileEntityBusStopPoleFlag}).</p>
 *
 * @since 2026.11
 */
public final class BusStopSignPresets {

  /** The route line a flag preset starts with. */
  public static final String SAMPLE_ROUTES = "12  40";

  /** The flag's presets: BUS STOP and the agency's name in its livery, and METRO. */
  public static final WayfindingPreset[] FLAG = {
      flag(BusAgency.CITYLINE), flag(BusAgency.RIVERWAY), flag(BusAgency.VERDANT),
      flag(BusAgency.EMBERLINE),
      new WayfindingPreset("BUS STOP", "METRO", WayfindingIcon.BUS, WayfindingArrow.NONE,
          WayfindingScheme.METRO).with(SAMPLE_ROUTES, "", WayfindingSign.NO_COLOUR,
          WayfindingSign.NO_COLOUR),
  };

  /** The blade's presets: a stop name in each agency's livery. */
  public static final WayfindingPreset[] BLADE = {
      blade(BusAgency.CITYLINE), blade(BusAgency.RIVERWAY), blade(BusAgency.VERDANT),
      blade(BusAgency.EMBERLINE),
      new WayfindingPreset("STOP NAME", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.METRO),
  };

  /** The flag's default: CITYLINE's, centred, both sides, lit, flush with nothing holding it. */
  static final WayfindingSign FLAG_DEFAULT = FLAG[0].toSign(WayfindingArrow.NONE, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).lineScale(1, 0.7f).lineScale(2, 0.85f)
      .mount(WayfindingMount.CEILING).doubleSided(true).build();

  /** The blade's default: CITYLINE's stop name, centred, both sides, lit. */
  static final WayfindingSign BLADE_DEFAULT = BLADE[0].toSign(WayfindingArrow.NONE, true)
      .toBuilder().align(WayfindingSign.Align.CENTRE).mount(WayfindingMount.CEILING)
      .doubleSided(true).build();

  private BusStopSignPresets() {
  }

  private static WayfindingPreset flag(BusAgency agency) {
    return new WayfindingPreset("BUS STOP", agency.getTitle(), WayfindingIcon.BUS,
        WayfindingArrow.NONE, WayfindingScheme.TRANSIT).with(SAMPLE_ROUTES, "",
        agency.getSignGround(), agency.getSignLegend());
  }

  private static WayfindingPreset blade(BusAgency agency) {
    return new WayfindingPreset("STOP NAME", agency.getTitle(), WayfindingIcon.NONE,
        WayfindingArrow.NONE, WayfindingScheme.TRANSIT).with("", "", agency.getSignGround(),
        agency.getSignLegend());
  }
}
