package com.micatechnologies.minecraft.csm.transit.airport;

/**
 * The stand sign on the apron: the stand's letter and number, twice the gate sign's size, on a
 * post that stands on the airfield mast. It is the gate sign in all but its model and its message:
 * a click steps the number 1 to 20, a sneaking click the letter A to D, kept in the same tile
 * entity and drawn from the same cell textures.
 *
 * @since 2026.9
 */
public class BlockStandSign extends BlockGateSign {

  /**
   * Constructs a stand sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockStandSign(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  protected String getMessageKey() {
    return "csm.transit.stand";
  }
}
