package com.micatechnologies.minecraft.csm.trafficaccessories;

/** The one-lane crossing gate. */
public class BlockRailroadCrossingGate1 extends BlockRailroadCrossingGate {

  @Override
  public int getLanes() {
    return 1;
  }

  @Override
  public String getBlockRegistryName() {
    return "railroad_crossing_gate_1";
  }
}
