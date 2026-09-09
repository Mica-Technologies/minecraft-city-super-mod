package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;

/**
 * Shared answers to "what counts as traffic", so every block in the road system agrees.
 *
 * <p>This used to be a private constant on {@code TileEntityTrafficSignalSensor}. It moved here
 * the moment a second block needed it (the radar speed feedback sign): two independent
 * definitions of what a vehicle is would drift, and a signal that gives a green to something the
 * radar sign refuses to measure is a bug nobody would think to look for.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class TrafficEntitySelectors {

  /**
   * Entities the road system treats as a vehicle: players and villagers.
   *
   * <p>The {@code NOT_SPECTATING} term is load-bearing, not decorative. The two-argument
   * {@code World.getEntitiesWithinAABB(Class, AxisAlignedBB)} applies
   * {@link EntitySelectors#NOT_SPECTATING} internally, but the three-argument overload
   * <em>replaces</em> that default with the supplied predicate rather than adding to it.
   * Dropping it would let a player in spectator mode place a call at an intersection, or set off
   * a speed display while invisible.</p>
   *
   * @since 2026.9
   */
  public static final Predicate<Entity> VEHICLE =
      entity -> EntitySelectors.NOT_SPECTATING.apply(entity)
          && (entity instanceof EntityPlayer || entity instanceof EntityVillager);

  private TrafficEntitySelectors() {
  }
}
