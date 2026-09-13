package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.world.World;

/**
 * What a requestable-mode controller actually has to do before and after serving a crossing,
 * read off the devices that are linked rather than assumed.
 *
 * <p>The requestable sequence was written for a signalised crossing: the main street's
 * pedestrian signals clear, a HAWK runs its flashing-yellow approach, the main street goes
 * yellow then all-red, the crossing is served, and the whole thing runs backwards. Every one of
 * those steps exists to clear a specific kind of device, and a crossing that has none of that
 * kind should not wait on it. An RRFB crossing is the plain case: nothing on the main street but
 * beacons that flash while called, so the beacons and the WALK should start together, the moment
 * the button is pressed.</p>
 *
 * <p>The four questions, each answered from the circuits:</p>
 * <ul>
 *   <li>{@link #needsFlashDontWalk()} -- the main street (circuit 1) has pedestrian signals or
 *   accessories showing WALK that must run their clearance first.</li>
 *   <li>{@link #needsHawkPreflash()} -- circuit 1 has a HAWK, which announces itself with
 *   flashing yellow before going steady.</li>
 *   <li>{@link #needsDefaultClearance()} -- circuit 1 has something that must be brought to red
 *   before the crossing is served: vehicle heads, or a HAWK.</li>
 *   <li>{@link #needsServiceClearance()} -- a served circuit has vehicle heads (or a HAWK) that
 *   must go yellow then red before the main street returns to green. A HAWK on circuit 1 is not
 *   one of these: it ends its own service with the wig-wag and goes dark.</li>
 * </ul>
 *
 * <p>Beacons that flash on call ({@link AbstractBlockControllableSignal#isFlashOnCallBeacon()})
 * never create a need: they have no approach and no clearance of their own.</p>
 *
 * @author Mica Technologies
 * @see TrafficSignalControllerTicker#requestableModeTick
 * @since 2026.9
 */
public final class RequestableCircuitNeeds {

  private final boolean flashDontWalk;
  private final boolean hawkPreflash;
  private final boolean defaultClearance;
  private final boolean serviceClearance;

  /**
   * Creates the needs directly. The ticker's tests use this; the controller uses
   * {@link #of(World, TrafficSignalControllerCircuits)}.
   *
   * @param flashDontWalk    circuit 1 must run a flashing don't walk before the crossing
   * @param hawkPreflash     circuit 1 has a HAWK that must pre-flash yellow
   * @param defaultClearance circuit 1 must go yellow then all-red before the crossing
   * @param serviceClearance a served circuit must go yellow then all-red after the crossing
   */
  public RequestableCircuitNeeds(boolean flashDontWalk, boolean hawkPreflash,
      boolean defaultClearance, boolean serviceClearance) {
    this.flashDontWalk = flashDontWalk;
    this.hawkPreflash = hawkPreflash;
    this.defaultClearance = defaultClearance;
    this.serviceClearance = serviceClearance;
  }

  /**
   * Reads the needs off the controller's circuits.
   *
   * <p>With no circuits at all every need is reported, which is the full sequence the mode has
   * always run -- there is nothing to read, so nothing is skipped.</p>
   *
   * @param world    the world the devices are in, used to tell a flash-on-call beacon from a HAWK
   * @param circuits the controller's circuits, circuit 1 being the main street
   *
   * @return the needs
   */
  public static RequestableCircuitNeeds of(World world, TrafficSignalControllerCircuits circuits) {
    if (circuits.getCircuitCount() == 0) {
      return new RequestableCircuitNeeds(true, true, true, true);
    }
    TrafficSignalControllerCircuit main = circuits.getCircuit(0);
    boolean mainHawks = !main.getHawkBeaconSignals(world).isEmpty();
    boolean flashDontWalk = !main.getPedestrianSignals().isEmpty()
        || !main.getPedestrianAccessorySignals().isEmpty();
    boolean serviceClearance = false;
    for (int i = 1; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit served = circuits.getCircuit(i);
      if (served.hasVehicleSignals() || !served.getHawkBeaconSignals(world).isEmpty()) {
        serviceClearance = true;
        break;
      }
    }
    return new RequestableCircuitNeeds(flashDontWalk, mainHawks,
        main.hasVehicleSignals() || mainHawks, serviceClearance);
  }

  /** @return whether circuit 1 must run a flashing don't walk before the crossing is served */
  public boolean needsFlashDontWalk() {
    return flashDontWalk;
  }

  /** @return whether circuit 1 has a HAWK that must pre-flash yellow */
  public boolean needsHawkPreflash() {
    return hawkPreflash;
  }

  /** @return whether circuit 1 must go yellow then all-red before the crossing is served */
  public boolean needsDefaultClearance() {
    return defaultClearance;
  }

  /** @return whether a served circuit must go yellow then all-red after the crossing */
  public boolean needsServiceClearance() {
    return serviceClearance;
  }

  @Override
  public String toString() {
    return "RequestableCircuitNeeds{flashDontWalk=" + flashDontWalk + ", hawkPreflash="
        + hawkPreflash + ", defaultClearance=" + defaultClearance + ", serviceClearance="
        + serviceClearance + '}';
  }
}
