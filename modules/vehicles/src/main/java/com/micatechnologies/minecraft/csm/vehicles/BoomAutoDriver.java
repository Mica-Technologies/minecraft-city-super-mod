package com.micatechnologies.minecraft.csm.vehicles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mcinterface1122.WrapperWorld;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The one-touch BOOM switch of CSM's boom trucks, on the server.
 *
 * <p>The boom is drawn from its manual variables alone ({@link BoomAxes}), so BOOM does not run
 * an animation of its own: when the switch is flipped (the panel's button, or B), this drives
 * those same variables to the truck's working pose, or back to stowed, an axis at a time at the
 * held keys' rates. Deploying raises the boom, then opens the elbow or runs out the reach, then
 * swings; stowing runs that backwards. A flip from a stowed boom deploys it and leaves BOOM on;
 * a flip from any other pose stows it and leaves BOOM off, so B always does what the HUD says.</p>
 *
 * <p>Whoever steers by hand while it drives takes over: a variable that is no longer where this
 * last put it was moved by a key or a panel button, so the drive stops where the boom is and BOOM
 * goes off, and the steering carries on from there with no jump.</p>
 *
 * <p>A truck seen for the first time (a world loading, a chunk coming in) only has its BOOM
 * recorded, so loading never starts a drive. Being on the server, it drives the boom once
 * however many riders the truck has, and every client sees it through Immersive Vehicles' own
 * variable packets.</p>
 *
 * @since 2026.11
 */
public class BoomAutoDriver {

  /** How close counts as there, and how far a variable must move to count as steered. */
  private static final double EPS = 1e-3;

  private static final class Drive {

    double boom;
    /** +1 deploying, -1 stowing, 0 idle. */
    int target;
    /** What this last set each axis to, while driving: lift, second, swing. */
    final double[] set = new double[3];
  }

  private final Map<UUID, Drive> drives = new HashMap<>();

  @SubscribeEvent
  public void onWorldTick(TickEvent.WorldTickEvent event) {
    World world = event.world;
    if (event.phase != TickEvent.Phase.END || world.isRemote) {
      return;
    }
    WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
    if (wrapper == null) {
      return;
    }
    Set<UUID> seen = new HashSet<>();
    for (EntityVehicleF_Physics vehicle : wrapper.getEntitiesOfType(EntityVehicleF_Physics.class)) {
      Map<String, Double> constants = BoomAxes.constants(vehicle);
      if (constants == null) {
        continue;
      }
      seen.add(vehicle.uniqueUUID);
      ComputedVariable boom = vehicle.getOrCreateVariable(BoomAxes.BOOM);
      ComputedVariable[] axes = {vehicle.getOrCreateVariable(BoomAxes.LIFT),
          vehicle.getOrCreateVariable(BoomAxes.second(constants)),
          vehicle.getOrCreateVariable(BoomAxes.SWING)};
      Drive drive = drives.get(vehicle.uniqueUUID);
      if (drive == null) {
        drive = new Drive();
        drive.boom = boom.currentValue;
        drives.put(vehicle.uniqueUUID, drive);
        continue;
      }
      if (Math.abs(boom.currentValue - drive.boom) > EPS) {
        start(drive, boom, axes);
      } else if (drive.target != 0 && steered(drive, axes)) {
        drive.target = 0;
        setBoom(drive, boom, 0);
      }
      if (drive.target != 0) {
        step(drive, axes, constants);
      }
    }
    drives.keySet().retainAll(seen);
  }

  /** BOOM was flipped: deploy a stowed boom, stow any other. */
  private static void start(Drive drive, ComputedVariable boom, ComputedVariable[] axes) {
    boolean stowed = true;
    for (int i = 0; i < 3; i++) {
      drive.set[i] = axes[i].currentValue;
      stowed &= Math.abs(axes[i].currentValue) < EPS;
    }
    drive.target = stowed ? 1 : -1;
    setBoom(drive, boom, stowed ? 1 : 0);
  }

  private static void setBoom(Drive drive, ComputedVariable boom, double value) {
    if (Math.abs(boom.currentValue - value) > EPS) {
      boom.setTo(value, true);
    }
    drive.boom = value;
  }

  private static boolean steered(Drive drive, ComputedVariable[] axes) {
    for (int i = 0; i < 3; i++) {
      if (Math.abs(axes[i].currentValue - drive.set[i]) > EPS) {
        return true;
      }
    }
    return false;
  }

  /** One tick of the drive: the first axis, in order, that is not yet where it is going. */
  private static void step(Drive drive, ComputedVariable[] axes, Map<String, Double> constants) {
    double[] goal = drive.target > 0 ? BoomAxes.deployPose(constants) : new double[3];
    double[] rate = {BoomAxes.LIFT_RATE, BoomAxes.secondRate(constants), BoomAxes.SWING_RATE};
    int[] order = drive.target > 0 ? new int[]{0, 1, 2} : new int[]{2, 1, 0};
    for (int i : order) {
      double now = axes[i].currentValue;
      double gap = goal[i] - now;
      if (Math.abs(gap) <= EPS) {
        continue;
      }
      double next = Math.abs(gap) <= rate[i] ? goal[i] : now + Math.signum(gap) * rate[i];
      next = Math.round(next * 1000.0) / 1000.0;
      axes[i].setTo(next, true);
      drive.set[i] = next;
      return;
    }
    drive.target = 0;
  }
}
