package com.micatechnologies.minecraft.csm.api.firealarm;

import com.micatechnologies.minecraft.csm.api.CsmEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Events fired by the CSM fire alarm system. External mods can subscribe to these on the Forge
 * event bus to react to fire alarm state changes.
 * <p>
 * All events include the world and the control panel position. Use {@link CsmFireAlarmQuery} for
 * on-demand state checks (e.g., when an entity spawns mid-alarm).
 */
public abstract class FireAlarmEvent extends CsmEvent {

  protected FireAlarmEvent(World world, BlockPos panelPos) {
    super(world, panelPos);
  }

  /**
   * Returns the position of the fire alarm control panel that fired this event.
   */
  public BlockPos getPanelPos() {
    return getPos();
  }

  /**
   * Fired when a fire alarm control panel transitions from normal to alarm-active state. This
   * indicates a fire alarm activation (pull station pulled, detector triggered, etc.).
   */
  public static class Activated extends FireAlarmEvent {
    public Activated(World world, BlockPos panelPos) {
      super(world, panelPos);
    }
  }

  /**
   * Fired when a fire alarm control panel transitions from alarm-active to normal state (reset).
   */
  public static class Deactivated extends FireAlarmEvent {
    public Deactivated(World world, BlockPos panelPos) {
      super(world, panelPos);
    }
  }

  /**
   * Fired when a fire alarm control panel activates its storm/tornado alarm (redstone-triggered
   * from an external weather sensor). During a storm alarm, occupants should shelter in place on
   * the lowest floor, NOT evacuate outside.
   */
  public static class StormActivated extends FireAlarmEvent {
    public StormActivated(World world, BlockPos panelPos) {
      super(world, panelPos);
    }
  }

  /**
   * Fired when a fire alarm control panel deactivates its storm/tornado alarm.
   */
  public static class StormDeactivated extends FireAlarmEvent {
    public StormDeactivated(World world, BlockPos panelPos) {
      super(world, panelPos);
    }
  }

  /**
   * Fired when audible silence is engaged on a fire alarm panel. The alarm is still active
   * (strobes flash) but horns and voice evacuation are silenced.
   */
  public static class AudibleSilenced extends FireAlarmEvent {
    public AudibleSilenced(World world, BlockPos panelPos) {
      super(world, panelPos);
    }
  }
}
