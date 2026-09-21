package com.micatechnologies.minecraft.csm.signage;

/**
 * A block of an advertising board other than its controller: backing and frame, and nothing else.
 * Placed only by {@link AdBoards} when a board is built, never by a player, so it is in no
 * creative tab. It has no tile entity: it finds its controller through its neighbours.
 */
public class BlockAdBoardPart extends AbstractBlockAdBoard {

  public BlockAdBoardPart(String registryName) {
    super(registryName);
  }
}
