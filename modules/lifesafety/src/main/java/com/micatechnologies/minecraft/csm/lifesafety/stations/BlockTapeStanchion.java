package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;

/**
 * A weighted post that scene tape runs to ({@link BlockSceneTape} joins any side of it). A plain
 * prop; only its type matters to the tape.
 *
 * @since 2026.9
 */
public class BlockTapeStanchion extends BlockFireProtectionProp {

  public BlockTapeStanchion(String registryName, int[] box) {
    super(registryName, box, true);
  }
}
