package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.CsmServerThread;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Entry point to the thermal simulation: one {@link HvacThermalWorld} per loaded server world,
 * created on first use, stepped from the world tick and dropped when the world unloads.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class HvacThermal {

  private static final Map<World, HvacThermalWorld> WORLDS = new WeakHashMap<>();

  private HvacThermal() {
  }

  /** Registers the world tick and load/unload listeners. Called from the module's pre-init. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new HvacThermal());
  }

  /**
   * The simulation of a server world, created on demand, or null for a client world (the client
   * only ever displays what the server sends it).
   */
  @Nullable
  static HvacThermalWorld get(@Nullable World world) {
    if (world == null || world.isRemote) {
      return null;
    }
    synchronized (WORLDS) {
      HvacThermalWorld w = WORLDS.get(world);
      if (w == null) {
        w = new HvacThermalWorld(world);
        WORLDS.put(world, w);
        world.addEventListener(w);
      }
      return w;
    }
  }

  /**
   * The simulation of a server world if it already exists. Used when a tile entity leaves the
   * world, which can happen while the world itself is unloading; creating one then would attach a
   * listener to a dying world.
   */
  @Nullable
  static HvacThermalWorld peek(@Nullable World world) {
    if (world == null || world.isRemote) {
      return null;
    }
    synchronized (WORLDS) {
      return WORLDS.get(world);
    }
  }

  /**
   * Whether a tile entity is the live one at its position in a loaded server world: not a copy a
   * tool made and read data into, and not an instance already replaced or unloaded. Only the live
   * instance takes up an anchor outside {@code onLoad} (#353).
   */
  static boolean isLive(TileEntity te) {
    World world = te.getWorld();
    if (world == null || world.isRemote || te.isInvalid()) {
      return false;
    }
    BlockPos pos = te.getPos();
    return world.isBlockLoaded(pos, false) && world.getTileEntity(pos) == te;
  }

  /**
   * The temperature at a position, for Core's {@code CsmEnvironment}: the simulated room
   * temperature on the server, the HUD's reading on the client when the position is by the
   * player, else the biome's.
   */
  public static float temperatureAt(World world, BlockPos pos) {
    HvacThermalWorld w = get(world);
    if (w != null) {
      return w.temperatureAt(pos);
    }
    if (world != null && world.isRemote) {
      Float t = HvacHudPacket.clientTemperatureNear(world, pos);
      if (t != null) {
        return t;
      }
    }
    return com.micatechnologies.minecraft.csm.codeutils.CsmEnvironment
        .getBaselineTemperatureAt(world, pos);
  }

  @SubscribeEvent
  public void onWorldTick(TickEvent.WorldTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
      return;
    }
    HvacThermalWorld w;
    synchronized (WORLDS) {
      w = WORLDS.get(event.world);
    }
    if (w != null) {
      w.tick();
    }
  }

  /**
   * A chunk has loaded on the server: its HVAC devices are audited shortly (#353). An async world
   * editor fires this off the server thread, so the queueing is handed to it (#375).
   */
  @SubscribeEvent
  public void onChunkLoad(ChunkEvent.Load event) {
    World world = event.getWorld();
    if (world == null || world.isRemote) {
      return;
    }
    int cx = event.getChunk().x;
    int cz = event.getChunk().z;
    CsmServerThread.run(world, () -> {
      HvacThermalWorld w = peek(world);
      if (w != null) {
        w.queueChunkAudit(cx, cz);
      }
    });
  }

  @SubscribeEvent
  public void onChunkUnload(ChunkEvent.Unload event) {
    World world = event.getWorld();
    if (world == null || world.isRemote) {
      return;
    }
    int cx = event.getChunk().x;
    int cz = event.getChunk().z;
    CsmServerThread.run(world, () -> {
      HvacThermalWorld w = peek(world);
      if (w != null) {
        w.onChunkUnload(cx, cz);
      }
    });
  }

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    World world = event.getWorld();
    synchronized (WORLDS) {
      HvacThermalWorld w = WORLDS.remove(world);
      if (w != null) {
        world.removeEventListener(w);
      }
    }
  }
}
