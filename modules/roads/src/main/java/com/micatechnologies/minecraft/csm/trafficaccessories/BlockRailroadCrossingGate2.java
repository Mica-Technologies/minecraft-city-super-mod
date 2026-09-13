package com.micatechnologies.minecraft.csm.trafficaccessories;

/** The two-lane crossing gate. */
public class BlockRailroadCrossingGate2 extends BlockRailroadCrossingGate {

  @Override
  public int getLanes() {
    return 2;
  }

  @Override
  public String getBlockRegistryName() {
    return "railroad_crossing_gate_2";
  }
}
