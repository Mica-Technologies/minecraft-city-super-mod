package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerCircuit;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerCircuits;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerNBTKeys;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhase;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhaseApplicability;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The Alto Springs fairgrounds gate crossing, run through the real controller tile entity in a
 * stand-in world: a REQUESTABLE controller with four RRFBs and APS buttons, a button pressed, and
 * the controller ticked a world tick at a time through the whole call and back to rest. At every
 * tick each RRFB's block colour must agree with where the controller's current phase puts it --
 * lit (not 3) exactly when the phase has it lit -- and it must be written only when that
 * changes, never once per flash.
 *
 * <p>Covers the beacons on the button's circuit and on another one, linked through the link API
 * and written in as NBT (the way a /blockdata or a city team's tooling links them), and the
 * phases the fairgrounds controller was seen in: {@code ap} 15 (main street pedestrian
 * clearance), 21 (the service clearance's last interval) and 14 (rest).</p>
 */
class RrfbRequestableEngineTest {

  private static BlockControllableRrfb rrfb;
  private static BlockControllableCrosswalkButtonAutomated aps;

  private static final BlockPos CONTROLLER = new BlockPos(0, 64, 0);
  private static final List<BlockPos> RRFBS = Arrays.asList(new BlockPos(5, 66, 0),
      new BlockPos(5, 66, 8), new BlockPos(-5, 66, 0), new BlockPos(-5, 66, 8));
  private static final List<BlockPos> BUTTONS = Arrays.asList(new BlockPos(4, 65, -1),
      new BlockPos(4, 65, 9));

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
    rrfb = new BlockControllableRrfb();
    aps = new BlockControllableCrosswalkButtonAutomated();
  }

  /** A world of exactly the blocks put in it, every chunk loaded, counting block writes. */
  abstract static class StandInWorld extends World {

    Map<BlockPos, IBlockState> states;
    Map<BlockPos, TileEntity> tiles;
    Map<BlockPos, Integer> writes;
    Set<BlockPos> unloaded;
    long time;

    protected StandInWorld() {
      super(null, null, null, null, false);
    }

    void setUpStandIn() {
      states = new HashMap<>();
      tiles = new HashMap<>();
      writes = new HashMap<>();
      unloaded = new java.util.HashSet<>();
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
      return states.getOrDefault(pos.toImmutable(), Blocks.AIR.getDefaultState());
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
      states.put(pos.toImmutable(), state);
      writes.merge(pos.toImmutable(), 1, Integer::sum);
      return true;
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState state) {
      return setBlockState(pos, state, 3);
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
      return tiles.get(pos.toImmutable());
    }

    @Override
    public boolean isBlockLoaded(BlockPos pos) {
      return !unloaded.contains(pos.toImmutable());
    }

    @Override
    public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty) {
      return isBlockLoaded(pos);
    }

    @Override
    public long getTotalWorldTime() {
      return time;
    }

    @Override
    public long getWorldTime() {
      return 6000;
    }

    @Override
    public boolean isDaytime() {
      return true;
    }

    @Override
    public boolean isBlockPowered(BlockPos pos) {
      return true;
    }

    @Override
    public int getRedstonePowerFromNeighbors(BlockPos pos) {
      return 15;
    }

    @Override
    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState,
        int flags) {
    }

    @Override
    public void markChunkDirty(BlockPos pos, TileEntity unusedTileEntity) {
    }

    @Override
    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
    }

    @Override
    public boolean checkLight(BlockPos pos) {
      return true;
    }

    @Override
    public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority) {
    }
  }

  /** A stand-in world allocated without running World's constructor, which needs a server. */
  private static StandInWorld newWorld() throws Exception {
    Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
    Class<? extends StandInWorld> concrete = Concrete.class;
    StandInWorld world = (StandInWorld) unsafe.allocateInstance(concrete);
    world.setUpStandIn();
    return world;
  }

  static class Concrete extends StandInWorld {

    @Override
    protected IChunkProvider createChunkProvider() {
      return null;
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
      return true;
    }
  }

  /** How the devices are wired: which circuit (1-based) the RRFBs and the buttons are on. */
  private static final class Wiring {

    final int[] rrfbCircuit;
    final int buttonCircuit;
    final boolean viaNbt;

    Wiring(int[] rrfbCircuit, int buttonCircuit, boolean viaNbt) {
      this.rrfbCircuit = rrfbCircuit;
      this.buttonCircuit = buttonCircuit;
      this.viaNbt = viaNbt;
    }
  }

  private static TileEntityTrafficSignalController build(StandInWorld world, Wiring wiring) {
    for (BlockPos pos : RRFBS) {
      world.states.put(pos, rrfb.getDefaultState()
          .withProperty(AbstractBlockControllableSignal.FACING, EnumFacing.NORTH)
          .withProperty(AbstractBlockControllableSignal.COLOR,
              AbstractBlockControllableSignal.SIGNAL_OFF));
      TileEntityRrfb te = new TileEntityRrfb();
      te.setWorld(world);
      te.setPos(pos);
      world.tiles.put(pos, te);
    }
    for (BlockPos pos : BUTTONS) {
      world.states.put(pos, aps.getDefaultState()
          .withProperty(AbstractBlockControllableSignal.COLOR,
              AbstractBlockControllableSignal.SIGNAL_RED));
      TileEntityTrafficSignalAPSPolara te = new TileEntityTrafficSignalAPSPolara();
      te.setWorld(world);
      te.setPos(pos);
      world.tiles.put(pos, te);
    }
    world.writes.clear();

    TileEntityTrafficSignalController controller = new TileEntityTrafficSignalController();
    controller.setWorld(world);
    controller.setPos(CONTROLLER);
    world.tiles.put(CONTROLLER, controller);
    if (wiring.viaNbt) {
      // Linked the way /blockdata links them: circuits and mode written into the tile entity,
      // no cached phases and no phase format, as a hand-written compound has.
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      for (int i = 0; i < RRFBS.size(); i++) {
        circuits.getCircuit(wiring.rrfbCircuit[i] - 1).linkDevice(RRFBS.get(i),
            SIGNAL_SIDE.PEDESTRIAN_BEACON);
      }
      for (BlockPos b : BUTTONS) {
        circuits.getCircuit(wiring.buttonCircuit - 1).linkDevice(b, SIGNAL_SIDE.PEDESTRIAN_ACCESSORY);
      }
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setInteger(TrafficSignalControllerNBTKeys.MODE,
          TrafficSignalControllerMode.REQUESTABLE.toNBT());
      nbt.setInteger(TrafficSignalControllerNBTKeys.OPERATING_MODE,
          TrafficSignalControllerMode.REQUESTABLE.toNBT());
      nbt.setTag(TrafficSignalControllerNBTKeys.CIRCUITS, circuits.toNBT());
      controller.readNBT(nbt);
    } else {
      controller.setModeByOrdinal(TrafficSignalControllerMode.REQUESTABLE.toNBT());
      for (int i = 0; i < RRFBS.size(); i++) {
        assertTrue(controller.linkDevice(RRFBS.get(i), SIGNAL_SIDE.PEDESTRIAN_BEACON,
            wiring.rrfbCircuit[i]));
      }
      for (BlockPos b : BUTTONS) {
        assertTrue(controller.linkDevice(b, SIGNAL_SIDE.PEDESTRIAN_ACCESSORY,
            wiring.buttonCircuit));
      }
    }
    return controller;
  }

  private static TrafficSignalPhase currentPhase(TileEntityTrafficSignalController controller) {
    NBTTagCompound nbt = controller.writeNBT(new NBTTagCompound());
    if (!nbt.hasKey(TrafficSignalControllerNBTKeys.CURRENT_PHASE)) {
      return null;
    }
    return TrafficSignalPhase.fromNBT(nbt.getCompoundTag(TrafficSignalControllerNBTKeys.CURRENT_PHASE));
  }

  /**
   * Runs the crossing: rest, a press, the whole call and back to rest, checking every tick.
   *
   * @return the applicabilities the controller passed through, in order
   */
  private static List<TrafficSignalPhaseApplicability> run(Wiring wiring) throws Exception {
    return run(wiring, false);
  }

  /**
   * As {@link #run(Wiring)}; with {@code unloadForTheWalk}, the beacons' chunk is unloaded from
   * the press until three seconds into the WALK, so the controller cannot write them when the
   * call starts, and must as soon as their chunk is back.
   */
  private static List<TrafficSignalPhaseApplicability> run(Wiring wiring,
      boolean unloadForTheWalk) throws Exception {
    StandInWorld world = newWorld();
    TileEntityTrafficSignalController controller = build(world, wiring);
    List<TrafficSignalPhaseApplicability> seen = new ArrayList<>();
    Map<BlockPos, Integer> lastColour = new HashMap<>();
    Map<BlockPos, Integer> colourChanges = new HashMap<>();
    boolean pressed = false;
    long walkSince = -1;
    long loadedAt = -1;
    for (world.time = 1000; world.time < 1000 + 20 * 400; world.time++) {
      controller.update();
      TrafficSignalPhase phase = currentPhase(controller);
      if (phase == null) {
        continue;
      }
      TrafficSignalPhaseApplicability ap = phase.getApplicability();
      if (seen.isEmpty() || seen.get(seen.size() - 1) != ap) {
        seen.add(ap);
      }
      if (!pressed && ap == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN
          && world.time > 1000 + 20 * 20) {
        ((TileEntityTrafficSignalTickableRequester) world.tiles.get(BUTTONS.get(0)))
            .incrementRequestCount();
        pressed = true;
        if (unloadForTheWalk) {
          world.unloaded.addAll(RRFBS);
        }
      }
      if (ap == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN && walkSince < 0) {
        walkSince = world.time;
      }
      if (!world.unloaded.isEmpty() && walkSince >= 0 && world.time - walkSince >= 60) {
        world.unloaded.clear();
        loadedAt = world.time;
      }
      // Unloaded, a beacon cannot be seen, and is written by the controller's next tick once its
      // chunk is back (the controller ticks every ten world ticks in this mode).
      boolean settling = !world.unloaded.isEmpty()
          || (loadedAt >= 0 && world.time - loadedAt <= 10);
      for (BlockPos pos : RRFBS) {
        if (settling) {
          continue;
        }
        int colour = world.getBlockState(pos).getValue(AbstractBlockControllableSignal.COLOR);
        boolean lit = phase.getYellowSignals().contains(pos) || phase.getRedSignals().contains(pos)
            || phase.getGreenSignals().contains(pos);
        assertEquals(lit, colour != AbstractBlockControllableSignal.SIGNAL_OFF,
            "RRFB " + pos + " in " + ap + " at tick " + world.time + ": colour " + colour
                + " but the phase has it " + (lit ? "lit" : "dark"));
        Integer before = lastColour.put(pos, colour);
        if (before != null && before != colour) {
          colourChanges.merge(pos, 1, Integer::sum);
        }
      }
      if (pressed && ap == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN
          && seen.size() > 2) {
        break;
      }
    }
    assertTrue(pressed, "the crossing never rested to take the press: " + seen);
    for (BlockPos pos : RRFBS) {
      // on with the WALK, off at the end of the clearance: two changes, two writes
      assertEquals(Integer.valueOf(2), colourChanges.get(pos),
          "RRFB " + pos + " changed colour " + colourChanges.get(pos) + " times through " + seen);
      assertEquals(Integer.valueOf(2), world.writes.get(pos),
          "RRFB " + pos + " written " + world.writes.get(pos) + " times through " + seen);
    }
    return seen;
  }

  private static void assertWentThrough(List<TrafficSignalPhaseApplicability> seen,
      TrafficSignalPhaseApplicability... expected) {
    Set<TrafficSignalPhaseApplicability> s = new LinkedHashSet<>(seen);
    for (TrafficSignalPhaseApplicability ap : expected) {
      assertTrue(s.contains(ap), ap + " not reached: " + seen);
    }
  }

  @Test
  void beaconsInAnUnloadedChunkAreWrittenWhenItLoads() throws Exception {
    run(new Wiring(new int[]{2, 2, 2, 2}, 1, true), true);
  }

  @Test
  void beaconsOnTheButtonsCircuitLinkedByTool() throws Exception {
    List<TrafficSignalPhaseApplicability> seen = run(new Wiring(new int[]{1, 1, 1, 1}, 1, false));
    assertWentThrough(seen, TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW,
        TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK);
  }

  @Test
  void beaconsOnAnotherCircuitLinkedByTool() throws Exception {
    List<TrafficSignalPhaseApplicability> seen = run(new Wiring(new int[]{2, 2, 2, 2}, 1, false));
    assertWentThrough(seen, TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW,
        TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK);
  }

  @Test
  void beaconsSplitAcrossCircuitsButtonsOnTheServedOne() throws Exception {
    run(new Wiring(new int[]{1, 1, 2, 2}, 2, false));
  }

  @Test
  void beaconsOnTheButtonsCircuitLinkedByNbt() throws Exception {
    List<TrafficSignalPhaseApplicability> seen = run(new Wiring(new int[]{1, 1, 1, 1}, 1, true));
    assertWentThrough(seen, TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW,
        TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK);
  }

  @Test
  void beaconsOnAnotherCircuitLinkedByNbt() throws Exception {
    run(new Wiring(new int[]{2, 2, 2, 2}, 1, true));
  }
}
