package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;

/**
 * The street name blade carried on the flat clamp plate. See {@link TileEntityStreetNameBlade}
 * for why the bracket needs a class of its own.
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class TileEntityStreetNameBladeClamp extends TileEntityStreetNameBlade {

  @Override
  protected StreetSignMount mount() {
    return StreetSignMount.POST_TOP_CLAMP;
  }
}
