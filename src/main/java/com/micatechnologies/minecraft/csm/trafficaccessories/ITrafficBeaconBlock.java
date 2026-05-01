package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * Interface for traffic beacon blocks that should render a periodic strobe flash via
 * {@link TileEntityTrafficBeaconRenderer}. Provides the beacon lens geometry and color.
 */
public interface ITrafficBeaconBlock {

  float[] getBeaconLensFrom();

  float[] getBeaconLensTo();

  float getBeaconColorR();

  float getBeaconColorG();

  float getBeaconColorB();
}
