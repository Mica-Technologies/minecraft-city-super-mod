package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundSchemes;

/**
 * Tile entity class for a Polara-styled traffic signal APS button.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2025.1.0
 */
public class TileEntityTrafficSignalAPSPolara extends TileEntityTrafficSignalAPS {

  /**
   * Constructor for a {@link TileEntityTrafficSignalAPSPolara} instance.
   *
   * @since 2.0
   */
  public TileEntityTrafficSignalAPSPolara() {
    super(TrafficSignalAPSSoundSchemes.POLARA);
  }
}
