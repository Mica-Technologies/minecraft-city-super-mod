package com.micatechnologies.minecraft.csm.vehicles;

import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptEmitter;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPreemptSource;
import java.util.List;
import java.util.Locale;
import mcinterface1122.WrapperWorld;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import net.minecraft.world.World;

/**
 * The Immersive Vehicles vehicles that are running their emergency lights, as preemption emitters
 * for Roads' preempt detectors.
 *
 * <p>Emergency lights are not something Immersive Vehicles knows about: each pack declares a
 * <em>custom variable</em> that its lights and siren animate on, and the panel shows a switch for
 * it. Most packs, Immersive Vehicles' own and UNU's included, call it {@code EMERLTS}; others name
 * theirs in words ("Emergency Lights", "City Siren"). A vehicle is an emitter while any custom
 * variable it or one of its parts declares is on and is one of those, so a lightbar or siren
 * fitted from another pack counts as much as one the vehicle was built with.</p>
 *
 * <p>Only variables a definition declares are read. Asking a vehicle for a variable it does not
 * have creates one, which this must never do to every vehicle in the world four times a
 * second.</p>
 *
 * <p>The vehicles come from Immersive Vehicles' own entity list for the world, the same one its
 * signal controller reads; nothing in the world's blocks is searched.</p>
 *
 * @since 2026.10
 */
public class IvPreemptSource implements ICsmPreemptSource {

  /** The forward axis of an Immersive Vehicles vehicle, before its orientation turns it. */
  private static final Point3D FORWARD = new Point3D(0, 0, 1);

  @Override
  public void collectEmitters(World world, List<CsmPreemptEmitter> out) {
    WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
    if (wrapper == null) {
      return;
    }
    for (EntityVehicleF_Physics vehicle : wrapper.getEntitiesOfType(EntityVehicleF_Physics.class)) {
      if (vehicle.isValid && emergencyLightsOn(vehicle)) {
        Point3D heading = FORWARD.copy().rotate(vehicle.orientation);
        out.add(new CsmPreemptEmitter(vehicle.position.x, vehicle.position.y,
            vehicle.position.z, heading.x, heading.z, CsmPreemptEmitter.Kind.EMERGENCY));
      }
    }
  }

  /** Whether the vehicle, or any part fitted to it, has an emergency custom variable on. */
  private static boolean emergencyLightsOn(EntityVehicleF_Physics vehicle) {
    if (declaresEmergencyOn(vehicle)) {
      return true;
    }
    for (APart part : vehicle.allParts) {
      if (declaresEmergencyOn(part)) {
        return true;
      }
    }
    return false;
  }

  /** Whether one of the custom variables this entity's definition declares is on and is an
   * emergency one. */
  private static boolean declaresEmergencyOn(AEntityD_Definable<?> entity) {
    JSONRendering rendering = entity.definition == null ? null : entity.definition.rendering;
    if (rendering == null || rendering.customVariables == null) {
      return false;
    }
    for (String name : rendering.customVariables) {
      if (isEmergencyVariable(name)) {
        ComputedVariable variable = entity.getOrCreateVariable(name);
        if (variable != null && variable.isActive) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Whether a custom variable's name is an emergency light or siren switch: {@code EMERLTS}, or a
   * name that says emergency or siren. Package-private for the test.
   *
   * @param name the variable's name as the pack declares it
   *
   * @return whether it is an emergency switch
   */
  static boolean isEmergencyVariable(String name) {
    if (name == null) {
      return false;
    }
    String n = name.toLowerCase(Locale.ROOT);
    return n.equals("emerlts") || n.contains("emergency") || n.contains("siren");
  }
}
