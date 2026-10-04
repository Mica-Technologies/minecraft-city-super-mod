package com.micatechnologies.minecraft.csm.vehicles;

import com.micatechnologies.minecraft.csm.codeutils.CsmStumpGrinders;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mcinterface1122.WrapperWorld;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Grinds stumps under the cutter wheel of the pack's stump grinder ({@code csm_stump_grinder})
 * while its GRIND switch is on.
 *
 * <p>Immersive Vehicles knows nothing about stumps (its drill effector breaks whatever is in its
 * box), so the grinding is done here, by the rules of the hand-guided grinder, through Core's
 * {@link CsmStumpGrinders}: a stump standing in the ground goes with its roots and leaves Mulch; a
 * standing tree, or a log on a floor or a foundation, is left alone. Without CSM: Parks &amp;
 * Greenery nothing is registered there, and the machine grinds nothing (it is also in the pack's
 * tree crew folder, which loads only with Parks).</p>
 *
 * <p>The wheel has to be down (the boom takes {@link #LOWER_TICKS} to lower after GRIND goes on)
 * and held on one stump for {@link #GRIND_TICKS}, about as long as the hand-guided grinder takes.
 * It grinds as the player at its controls (or who last drove it), for build permissions; one
 * nobody is driving or has driven grinds nothing.</p>
 *
 * @since 2026.10
 */
public class StumpGrinderMachines {

  /** The grinder's system name in the pack. */
  static final String SYSTEM_NAME = "csm_stump_grinder";
  /** The switch that lowers the boom and runs the wheel. */
  private static final String GRIND = "GRIND";
  /**
   * Where the cutter wheel's centre is with the boom down, in the vehicle's frame (x left, y up
   * from the axles, z forward). From the generator's boom: pivot (0, 0.55, 0.45), wheel centre
   * (0, 0.35, 1.6), lowered 20 degrees.
   */
  private static final double CUTTER_X = 0.0;
  private static final double CUTTER_Y = -0.03;
  private static final double CUTTER_Z = 1.46;
  /** How often the machines are looked at, in ticks. */
  private static final int EVERY = 5;
  /** How long the boom takes to come down, in ticks (the generator's animation duration). */
  private static final int LOWER_TICKS = 40;
  /** How long the wheel works one stump before it goes, in ticks. */
  private static final int GRIND_TICKS = 40;

  /** Each running grinder: how long GRIND has been on, the stump it is on and for how long. */
  private static final class Working {

    int onTicks;
    BlockPos stump;
    int stumpTicks;
  }

  private final Map<UUID, Working> working = new HashMap<>();

  @SubscribeEvent
  public void onWorldTick(TickEvent.WorldTickEvent event) {
    World world = event.world;
    if (event.phase != TickEvent.Phase.END || world.isRemote
        || world.getTotalWorldTime() % EVERY != 0 || !CsmStumpGrinders.available()) {
      return;
    }
    WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
    if (wrapper == null) {
      return;
    }
    Set<UUID> seen = new HashSet<>();
    for (EntityVehicleF_Physics vehicle : wrapper.getEntitiesOfType(EntityVehicleF_Physics.class)) {
      if (!vehicle.isValid || vehicle.definition == null
          || !CsmVehicles.PACK_ID.equals(vehicle.definition.packID)
          || !SYSTEM_NAME.equals(vehicle.definition.systemName)) {
        continue;
      }
      // GRIND is declared by the grinder's definition, so asking for it creates nothing new
      ComputedVariable grind = vehicle.getOrCreateVariable(GRIND);
      if (grind == null || !grind.isActive) {
        continue;
      }
      seen.add(vehicle.uniqueUUID);
      Working w = working.computeIfAbsent(vehicle.uniqueUUID, k -> new Working());
      w.onTicks += EVERY;
      if (w.onTicks < LOWER_TICKS) {
        continue;
      }
      BlockPos stump = stumpUnderCutter(world, vehicle);
      if (stump == null || !stump.equals(w.stump)) {
        w.stump = stump;
        w.stumpTicks = 0;
        continue;
      }
      w.stumpTicks += EVERY;
      if (w.stumpTicks < GRIND_TICKS) {
        continue;
      }
      w.stump = null;
      w.stumpTicks = 0;
      EntityPlayer driver = driver(world, vehicle);
      if (driver != null) {
        CsmStumpGrinders.grind(world, stump, driver);
      }
    }
    // A grinder switched off, or gone, starts again from the top.
    working.keySet().retainAll(seen);
  }

  /**
   * The log nearest the cutter wheel, within a block of it across and from a block under it to
   * the wheel's own height, or null.
   */
  private static BlockPos stumpUnderCutter(World world, EntityVehicleF_Physics vehicle) {
    Point3D cutter = new Point3D(CUTTER_X, CUTTER_Y, CUTTER_Z).rotate(vehicle.orientation)
        .add(vehicle.position);
    int cx = (int) Math.floor(cutter.x);
    int cy = (int) Math.floor(cutter.y);
    int cz = (int) Math.floor(cutter.z);
    BlockPos best = null;
    double bestDistance = Double.MAX_VALUE;
    for (int dy = -1; dy <= 0; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          BlockPos p = new BlockPos(cx + dx, cy + dy, cz + dz);
          if (!world.isBlockLoaded(p) || !CsmStumpGrinders.isLog(world, p)) {
            continue;
          }
          double d = p.distanceSqToCenter(cutter.x, cutter.y, cutter.z);
          if (d < bestDistance) {
            bestDistance = d;
            best = p;
          }
        }
      }
    }
    return best;
  }

  /**
   * Who is grinding: the player standing at the controls, or else the one who last drove it, if
   * they are in this world. Immersive Vehicles sets {@code lastController} only once a control is
   * used, so a player who has only got on is found by the seat.
   */
  private static EntityPlayer driver(World world, EntityVehicleF_Physics vehicle) {
    for (APart part : vehicle.allParts) {
      if (part instanceof PartSeat && part.placementDefinition.isController
          && part.rider != null) {
        EntityPlayer player = world.getPlayerEntityByUUID(part.rider.getID());
        if (player != null) {
          return player;
        }
      }
    }
    IWrapperPlayer controller = vehicle.lastController;
    return controller == null ? null : world.getPlayerEntityByUUID(controller.getID());
  }
}
