package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;

/**
 * The station agent's booth counter, the customer's side toward the player who placed it: a
 * stainless front with the STATION AGENT plate, a ledge with a deal tray, glass from the ledge up
 * with a speaking grille, and the agent's work shelf behind. It is one straight run across its
 * block, so entrance glass stacks on it into the booth's window and joins it from the sides into
 * the booth's walls ({@link BlockStationGlass}); a roof or a ceiling and a door finish the booth.
 * It is RCMC's operator panel and line desk that run the trains; this is where a station sells
 * and checks fares, and does nothing but stand there.
 *
 * @since 2026.9
 */
public class BlockStationBoothCounter extends BlockPlatformFixture {

  /**
   * Constructs a booth counter, drawn in the translucent layer for its glass.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockStationBoothCounter(String registryName, double[] box) {
    super(registryName, box, true);
  }
}
