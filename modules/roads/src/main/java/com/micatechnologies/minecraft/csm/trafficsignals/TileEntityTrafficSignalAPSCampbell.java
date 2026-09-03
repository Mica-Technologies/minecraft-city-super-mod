package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundSchemes;

/**
 * Tile entity class for a Campbell-styled traffic signal APS button.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2025.1.0
 */
public class TileEntityTrafficSignalAPSCampbell extends TileEntityTrafficSignalAPS {

  /**
   * Constructor for a {@link TileEntityTrafficSignalAPSCampbell} instance.
   *
   * @since 2.0
   */
  public TileEntityTrafficSignalAPSCampbell() {
    super(TrafficSignalAPSSoundSchemes.CAMPBELL);
  }
}
