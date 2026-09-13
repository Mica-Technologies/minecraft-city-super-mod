package com.micatechnologies.minecraft.csm.trafficaccessories;

/** The three-lane crossing gate. */
public class BlockRailroadCrossingGate3 extends BlockRailroadCrossingGate {

  @Override
  public int getLanes() {
    return 3;
  }

  @Override
  public String getBlockRegistryName() {
    return "railroad_crossing_gate_3";
  }
}
