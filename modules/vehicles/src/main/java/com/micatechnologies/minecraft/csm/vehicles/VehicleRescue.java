package com.micatechnologies.minecraft.csm.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import mcinterface1122.WrapperPlayer;
import mcinterface1122.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Lifts an Immersive Vehicles vehicle out of the blocks it has sunk into, on the server.
 *
 * <p>IV's collision stops a vehicle going into blocks, but with lag or a wonky turn one can end
 * up partly inside the ground, a wall or a road deck, and from there its collision holds it in
 * place in every direction. Each tick this looks at every vehicle's block collision boxes (its
 * body and its wheels, the same boxes IV collides with); one that has had a box inside a solid
 * block for {@link #STUCK_TICKS} ticks in a row is lifted straight up to the first height where
 * every box is clear, at most {@link #MAX_LIFT} blocks. Being the lowest clear height, that is
 * where it rests on what it was stuck in: a truck that fell through a road comes back up onto
 * it. Its heading, parts, fuel, inventory and riders all stay. A tunnel or a garage is open
 * air, so a vehicle driven into one is never touched.</p>
 *
 * <p>The test shrinks each box a little ({@link #AUTO_MARGIN}), so a wheel resting a hair into
 * the road, which IV corrects itself, never counts. A rider can also press the unstick key
 * ({@link VehicleRescueKey}): that flips the vehicle's {@link #UNSTICK} variable through IV's
 * own toggle packet, and a flip rescues it at once with a tighter test ({@link #KEY_MARGIN}), for
 * one only just wedged into a block. Pressed in a vehicle that is in no block at all but has
 * blocks right above it (fallen through a road into the pocket under it, or down a hole), it
 * climbs: up through those blocks to the first clear height on top. With nothing above, it says
 * the vehicle is not stuck. Leaves and liquids never count, as IV ignores them too.</p>
 *
 * <p>The lift is IV's own server movement: the position moves, the server's sync total takes
 * the move, and every client is sent the same {@link PacketVehicleServerMovement} IV sends each
 * tick, so clients slide the vehicle up with their usual correction. A load mounted on a flatbed
 * is never tested or moved itself; it rides its hitch and follows the truck. A vehicle with no
 * room above is left where it is and tried again after {@link #RETRY_TICKS} ticks.</p>
 *
 * @since 2026.11
 */
public class VehicleRescue {

  /** The vehicle variable the unstick key flips. */
  static final String UNSTICK = "csm_unstick";

  /** How long a box must stay inside a block, in ticks, before the vehicle is lifted. */
  private static final int STUCK_TICKS = 60;

  /** How long to wait before trying again when there was no room above, in ticks. */
  private static final int RETRY_TICKS = 600;

  /** The most it will lift a vehicle, in blocks. */
  private static final double MAX_LIFT = 32.0;

  /** The height steps it tries, in blocks. */
  private static final double STEP = 1.0 / 16.0;

  /** How far each box is shrunk for the automatic test, in blocks. */
  private static final double AUTO_MARGIN = 0.125;

  /** How far each box's sides and top are shrunk for the key's test; the bottom keeps the auto margin. */
  private static final double KEY_MARGIN = 0.02;

  private static final class Watch {

    double unstick;
    /** Ticks stuck in a row; negative while waiting to retry. */
    int stuck;
  }

  /** Per world: every loaded world ticks on its own, and each prunes only its own vehicles. */
  private final Map<World, Map<UUID, Watch>> worlds = new WeakHashMap<>();

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
    Map<UUID, Watch> watches = worlds.computeIfAbsent(world, w -> new HashMap<>());
    Set<UUID> seen = new HashSet<>();
    for (EntityVehicleF_Physics vehicle : wrapper.getEntitiesOfType(EntityVehicleF_Physics.class)) {
      if (!vehicle.isValid || vehicle.towedByConnection != null
          && vehicle.towedByConnection.hitchConnection.mounted) {
        continue;
      }
      seen.add(vehicle.uniqueUUID);
      double unstick = vehicle.getOrCreateVariable(UNSTICK).currentValue;
      Watch watch = watches.get(vehicle.uniqueUUID);
      if (watch == null) {
        watch = new Watch();
        watch.unstick = unstick;
        watches.put(vehicle.uniqueUUID, watch);
      }
      if (unstick != watch.unstick) {
        watch.unstick = unstick;
        rescue(world, vehicle, watch, !inBlocks(world, vehicle, 0, KEY_MARGIN));
        continue;
      }
      if (watch.stuck < 0) {
        watch.stuck++;
      } else if (!inBlocks(world, vehicle, 0, AUTO_MARGIN)) {
        watch.stuck = 0;
      } else if (++watch.stuck >= STUCK_TICKS) {
        rescue(world, vehicle, watch, false);
      }
    }
    watches.keySet().retainAll(seen);
  }

  /**
   * Lifts the vehicle to the first clear height above, or says there is none. To {@code climb} is
   * for a vehicle that is clear where it is (the key, pressed in a pocket under a road it fell
   * through): it goes up through the blocks above it to the first clear height on top of them,
   * and with no blocks above it is not stuck at all.
   */
  private static void rescue(World world, EntityVehicleF_Physics vehicle, Watch watch,
      boolean climb) {
    boolean blocked = !climb;
    for (double lift = STEP; lift <= MAX_LIFT; lift += STEP) {
      if (inBlocks(world, vehicle, lift, 0)) {
        blocked = true;
      } else if (blocked) {
        lift(vehicle, lift);
        watch.stuck = 0;
        tell(world, vehicle, "csm_vehicles.rescue.lifted",
            String.format(Locale.ROOT, "%.1f", lift));
        CsmVehicles.getLogger().info("Lifted stuck vehicle {} at {} up {} blocks",
            vehicle.definition.systemName, vehicle.position, lift);
        return;
      }
    }
    if (!blocked) {
      tell(world, vehicle, "csm_vehicles.rescue.not_stuck");
      return;
    }
    watch.stuck = -RETRY_TICKS;
    tell(world, vehicle, "csm_vehicles.rescue.no_room");
  }

  /** Moves the vehicle up as IV's own server movement does, clients included. */
  private static void lift(EntityVehicleF_Physics vehicle, double lift) {
    Point3D motion = new Point3D(0, lift, 0);
    vehicle.position.add(motion);
    vehicle.motion.set(0, 0, 0);
    vehicle.addToServerDeltas(motion, new Point3D(), 0);
    InterfaceManager.packetInterface.sendToAllClients(
        new PacketVehicleServerMovement(vehicle, motion, new Point3D(), 0));
  }

  /**
   * Whether any of the vehicle's block collision boxes, raised {@code lift} blocks and shrunk
   * {@code margin} on every side (the bottom by at least {@link #AUTO_MARGIN} when the margin is
   * not zero), is inside a block IV would collide with.
   */
  private static boolean inBlocks(World world, EntityVehicleF_Physics vehicle, double lift,
      double margin) {
    double bottom = margin == 0 ? 0 : Math.max(margin, AUTO_MARGIN);
    List<AxisAlignedBB> hits = new ArrayList<>();
    for (BoundingBox box : vehicle.allBlockCollisionBoxes) {
      Point3D c = box.globalCenter;
      double w = box.widthRadius - margin;
      double h = box.heightRadius;
      double d = box.depthRadius - margin;
      if (w <= 0 || d <= 0 || h * 2 <= margin + bottom) {
        continue;
      }
      AxisAlignedBB aabb = new AxisAlignedBB(c.x - w, c.y + lift - h + bottom, c.z - d,
          c.x + w, c.y + lift + h - margin, c.z + d);
      if (collides(world, aabb, hits)) {
        return true;
      }
    }
    return false;
  }

  /** IV's block test ({@code WrapperWorld.checkForCollisions}) without its liquids. */
  private static boolean collides(World world, AxisAlignedBB aabb, List<AxisAlignedBB> hits) {
    for (int x = MathHelper.floor(aabb.minX); x < Math.ceil(aabb.maxX); x++) {
      for (int y = MathHelper.floor(aabb.minY); y < Math.ceil(aabb.maxY); y++) {
        for (int z = MathHelper.floor(aabb.minZ); z < Math.ceil(aabb.maxZ); z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (!world.isBlockLoaded(pos)) {
            continue;
          }
          IBlockState state = world.getBlockState(pos);
          if (state.getMaterial() == Material.LEAVES || state.getMaterial().isLiquid()
              || !state.getBlock().canCollideCheck(state, false)
              || state.getCollisionBoundingBox(world, pos) == null) {
            continue;
          }
          hits.clear();
          state.addCollisionBoxToList(world, pos, aabb, hits, null, false);
          if (!hits.isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** Sends a chat line to every player riding the vehicle. */
  private static void tell(World world, EntityVehicleF_Physics vehicle, String key,
      Object... args) {
    List<Object> riders = new ArrayList<>();
    for (APart part : vehicle.allParts) {
      if (part instanceof PartSeat && part.rider != null) {
        riders.add(part.rider);
      }
    }
    if (riders.isEmpty()) {
      return;
    }
    for (EntityPlayer player : world.playerEntities) {
      if (riders.contains(WrapperPlayer.getWrapperFor(player))) {
        player.sendMessage(new TextComponentTranslation(key, args));
      }
    }
  }
}
