package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An airfield light: an edge, threshold or centreline light, the approach light bar, the airport
 * beacon, or the red obstruction light on the wind sock and the antenna mast. It is lit or not
 * ({@link #LIT}), and that is all it does: the blockstate swaps its lens for the lit one, and a
 * lit light gives the block light it was made with. There is no renderer.
 *
 * <p>Lights are switched a circuit at a time, as an airfield's are. Every light of the same
 * circuit (runway, taxiway, beacon, obstruction) within {@link #REACH} blocks of another is on
 * that circuit, so a row of edge lights set a few blocks apart is one circuit however long it is.
 * A click with an empty hand on any of them, or a change of redstone power at any of them (on when
 * power comes, off when it goes, as the shelters' lights are), switches the whole circuit. A light
 * placed beside a lit circuit comes on with it.</p>
 *
 * <p>The metadata holds the facing in its low two bits, {@link #LIT} above it and
 * {@link #POWERED} (the redstone the light last saw) above that.</p>
 *
 * @since 2026.9
 */
public class BlockAirfieldLight extends BlockPlatformFixture {

  /** Whether the light is on. */
  public static final PropertyBool LIT = PropertyBool.create("lit");
  /** Whether redstone powered the light when it last looked. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** How far apart two lights of a circuit may be, across. */
  static final int REACH = 8;
  /** How far apart two lights of a circuit may be, up or down. */
  static final int REACH_Y = 2;
  /** The most lights one switch reaches. */
  static final int MAX_LIGHTS = 512;

  private final String circuit;
  private final int light;

  /**
   * Constructs an airfield light.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   * @param circuit      the circuit it switches with
   * @param light        the light it gives when lit, 0 to 15
   */
  public BlockAirfieldLight(String registryName, double[] box, String circuit, int light) {
    super(registryName, box);
    this.circuit = circuit;
    this.light = light;
    setDefaultState(getDefaultState().withProperty(LIT, false).withProperty(POWERED, false));
  }

  /** The circuit this light switches with. */
  public String getCircuit() {
    return circuit;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIT, POWERED);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LIT, (meta & 4) != 0).withProperty(POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0)
        | (state.getValue(POWERED) ? 8 : 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? light : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** A click with an empty hand switches the light's circuit. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      int count = switchCircuit(world, pos, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.6F : 0.5F);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.airfield_lights",
          new TextComponentTranslation(lit ? "csm.transit.airfield_lights.on"
              : "csm.transit.airfield_lights.off"), String.valueOf(count)), true);
    }
    return true;
  }

  /** A change of redstone power switches the circuit: on when power comes, off when it goes. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      switchCircuit(world, pos, powered);
    }
  }

  /** A new light takes the power it stands in, or else the state of the circuit it joins. */
  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered) {
      world.setBlockState(pos, state.withProperty(POWERED, true), 2);
      switchCircuit(world, pos, true);
      return;
    }
    BlockPos other = nearestOnCircuit(world, pos);
    if (other != null && world.getBlockState(other).getValue(LIT) != state.getValue(LIT)) {
      world.setBlockState(pos, state.withProperty(LIT, world.getBlockState(other).getValue(LIT)),
          2);
    }
  }

  /**
   * Sets every light on the circuit {@code start} belongs to on or off.
   *
   * @return how many lights the circuit has
   */
  int switchCircuit(World world, BlockPos start, boolean lit) {
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    seen.add(start);
    open.add(start);
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    int count = 0;
    while (!open.isEmpty() && count < MAX_LIGHTS) {
      BlockPos pos = open.poll();
      IBlockState here = world.getBlockState(pos);
      if (!onCircuit(here)) {
        continue;
      }
      count++;
      if (here.getValue(LIT) != lit) {
        world.setBlockState(pos, here.withProperty(LIT, lit), 2);
      }
      for (int dy = -REACH_Y; dy <= REACH_Y; dy++) {
        for (int dx = -REACH; dx <= REACH; dx++) {
          for (int dz = -REACH; dz <= REACH; dz++) {
            at.setPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
            if (seen.contains(at) || !world.isBlockLoaded(at)) {
              continue;
            }
            if (onCircuit(world.getBlockState(at))) {
              BlockPos found = at.toImmutable();
              seen.add(found);
              open.add(found);
            }
          }
        }
      }
    }
    return count;
  }

  /** The nearest other light on this circuit, or null. */
  private BlockPos nearestOnCircuit(World world, BlockPos pos) {
    BlockPos best = null;
    double bestDistance = Double.MAX_VALUE;
    for (BlockPos at : BlockPos.getAllInBoxMutable(pos.add(-REACH, -REACH_Y, -REACH),
        pos.add(REACH, REACH_Y, REACH))) {
      if (!at.equals(pos) && world.isBlockLoaded(at) && onCircuit(world.getBlockState(at))) {
        double d = at.distanceSq(pos);
        if (d < bestDistance) {
          bestDistance = d;
          best = at.toImmutable();
        }
      }
    }
    return best;
  }

  private boolean onCircuit(IBlockState state) {
    return state.getBlock() instanceof BlockAirfieldLight
        && ((BlockAirfieldLight) state.getBlock()).circuit.equals(circuit);
  }
}
