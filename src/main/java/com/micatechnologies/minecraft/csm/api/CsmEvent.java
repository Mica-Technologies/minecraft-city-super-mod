package com.micatechnologies.minecraft.csm.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Base class for all City Super Mod events posted to the Forge event bus. External mods can
 * subscribe to subclasses of this event to react to CSM systems (fire alarms, traffic signals,
 * etc.) without a hard dependency by using reflection-guarded event handlers.
 */
public abstract class CsmEvent extends Event {

  private final World world;
  private final BlockPos pos;

  protected CsmEvent(World world, BlockPos pos) {
    this.world = world;
    this.pos = pos;
  }

  /**
   * Returns the world in which this event occurred.
   */
  public World getWorld() {
    return world;
  }

  /**
   * Returns the block position associated with this event (typically the source block).
   */
  public BlockPos getPos() {
    return pos;
  }
}
