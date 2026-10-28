package com.micatechnologies.minecraft.csm.healthcare.diagnostics;

/**
 * The refrigerated morgue cabinet ({@code morgue_cabinet}): two tiers, one door and one tray to a
 * cell, two blocks tall as a {@link BlockDoorCabinetPair door cabinet pair}. A click on a cell
 * opens or shuts that cell's door; an open door stands square to the cabinet with the tray slid
 * half out in front.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), bit 2 the upper cell, bit 3 this
 * cell's door open.</p>
 *
 * @since 2026.10
 */
public class BlockMorgueCabinet extends BlockDoorCabinetPair {

  /** Constructs the cabinet. */
  public BlockMorgueCabinet() {
    super("morgue_cabinet", new int[]{0, 0, 0, 16, 16, 16});
  }
}
