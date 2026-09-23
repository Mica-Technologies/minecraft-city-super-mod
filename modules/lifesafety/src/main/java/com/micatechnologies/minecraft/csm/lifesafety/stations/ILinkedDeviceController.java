package com.micatechnologies.minecraft.csm.lifesafety.stations;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

/**
 * A controller's tile entity that keeps a list of devices the fire alarm linker adds to it: the
 * station alerting controller, the outdoor warning siren controller. The linker selects a
 * controller when it clicks a block that is an {@link Block} marked {@link ControllerBlock}, then
 * offers it each device clicked after that.
 *
 * @since 2026.9
 */
public interface ILinkedDeviceController {

  /** Marks a block whose tile entity is an {@link ILinkedDeviceController}. */
  interface ControllerBlock {

  }

  /** What offering a device did. */
  enum LinkResult {
    LINKED, ALREADY_LINKED, NOT_MINE
  }

  /**
   * Offers a device to this controller.
   *
   * @param block the device's block
   * @param pos   where it is
   *
   * @return whether it was added, was already there, or is not a kind this controller drives
   */
  LinkResult link(Block block, BlockPos pos);

  /** What this controller is called in the linker's replies. */
  String describe();
}
