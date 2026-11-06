package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmGlassLayer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An automatic sliding glass door: a sliding panel ({@code auto_door_slider_*}) or the fixed
 * sidelite it slides past ({@code auto_door_sidelite_*}). One class, constructed by registry
 * name, built on {@link BlockBuildingDoor}: two halves, the same metadata, keypad locks, pairs.
 *
 * <p>A panel's "hinge" is the side it slides to. Two panels side by side hinged on their far
 * sides are a pair, latch to latch, and part in the middle: a bi-parting door. One panel beside a
 * sidelite on its hinge side is a single-slide door. A panel slides {@link #SLIDE} along the wall,
 * through the sidelite's cell (in a plane in front of the sidelite's glass) or into the wall.</p>
 *
 * <ul>
 *   <li><b>Opening.</b> The approach sensor ({@link AutoDoorSensors}) opens a panel (and its
 *   pair) as a player comes within a couple of blocks of either face, as a real door's header
 *   sensor does. Redstone opens it while it is powered; a click opens it by hand; a linked keypad,
 *   button or station as ever; and an old {@link BlockAutoDoorMat}, no longer in the creative
 *   tab, still opens it when someone steps on the mat.</li>
 *   <li><b>Closing.</b> One scheduled tick, {@link #HOLD_TICKS} after the last thing that opened
 *   it (the sensor, while someone is in its zone, and each step on a mat, push the time back). If
 *   someone is still in the doorway or just either side of it when the tick comes, it looks again
 *   a second later. Nothing ticks on the door itself while it is idle, and no tile entity exists
 *   at rest: the sensor is one look every few ticks at the doors near each player, and a slider's
 *   random tick only puts it on the sensor's list.</li>
 *   <li><b>Locked</b> by a keypad, someone outside does not open it, by the sensor or a mat;
 *   someone inside still does (free egress).</li>
 *   <li><b>The slide</b> is drawn by the swing renderer while the door moves (the swing's
 *   short-lived client tile entity, {@link TileEntityDoorSwing}), moving the panel along the wall
 *   instead of turning it; at rest it is baked models.</li>
 * </ul>
 *
 * <p>No closer, no reversing: the closer item and the Door Swing Tool refuse it.</p>
 *
 * @version 1.1
 * @since 2026.10
 */
public class BlockAutoSlidingDoor extends BlockBuildingDoor implements ICsmGlassFronted {

  /** How far a panel slides, in blocks. SHARED with gen_doors.SLIDE_PX. */
  public static final double SLIDE = 15.5 / 16;
  /** How long a slide takes, in ticks. */
  private static final int SLIDE_TICKS = 14;
  /** How long a door stays open after the last thing that opened it, in ticks. */
  static final int HOLD_TICKS = 60;
  /** How soon an open door looks again when someone is still in its way, in ticks. */
  private static final int RECHECK_TICKS = 20;

  /** SHARED with gen_doors: the panel's and the sidelite's planes, in blocks, inside north. */
  private static final double SLIDER_Z0 = 4.0 / 16;
  private static final double SLIDER_Z1 = 5.75 / 16;
  private static final double SIDELITE_Z0 = 6.5 / 16;
  private static final double SIDELITE_Z1 = 8.25 / 16;
  private static final double STRIP = 0.5 / 16;

  /**
   * How far from a door's middle along the wall the approach sensor still sees someone, in
   * blocks: a little past the door's own half width, so an angled approach counts.
   */
  private static final double SENSOR_ALONG = 0.9;

  /**
   * Constructs a {@link BlockAutoSlidingDoor}.
   *
   * @param registryName {@code auto_door_slider_<finish>} or {@code auto_door_sidelite_<finish>}
   *
   * @since 1.0
   */
  public BlockAutoSlidingDoor(String registryName) {
    super(registryName);
    if (!fixed()) {
      // Only to put itself on the approach sensor's list (randomTick); see AutoDoorSensors.
      setTickRandomly(true);
    }
  }

  /** Whether this is the fixed sidelite, which never moves. */
  public boolean fixed() {
    return getBlockRegistryName().contains("sidelite");
  }

  @Override
  protected boolean reversible() {
    return false;
  }

  @Override
  protected boolean glazed() {
    return true;
  }

  @Override
  public int motionTicks() {
    return SLIDE_TICKS;
  }

  @Override
  protected SoundEvent sound(boolean open) {
    // A soft pneumatic hiss rather than a door's clunk.
    return open ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT;
  }

  @Override
  boolean fitCloser(World world, BlockPos pos) {
    return false;
  }

  // --- opening and closing --------------------------------------------------------------------

  @Override
  protected void swing(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    if (fixed()) {
      return;
    }
    super.swing(world, lowerPos, open, byHand);
    if (open) {
      // Whatever opened it, it shuts itself a few seconds later, unless redstone holds it.
      armClose(world, lowerPos, HOLD_TICKS);
    }
  }

  @Override
  protected void onPower(World world, BlockPos lowerPos, boolean powered) {
    if (fixed()) {
      return;
    }
    if (powered) {
      setOpen(world, lowerPos, true, false);
    } else {
      // The signal has gone: shut once the hold time is up and the doorway is clear.
      armClose(world, lowerPos, HOLD_TICKS);
    }
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (fixed() && !playerIn.isSneaking()) {
      return false;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }

  /**
   * Someone stepped on an activation mat beside the door at {@code pos}: open it and its pair,
   * and push back the time it shuts. A locked door does not open for a mat on its outside.
   *
   * @param world  the server's world
   * @param pos    either half of the door
   * @param matPos the mat
   *
   * @since 1.0
   */
  void sensorOpen(World world, BlockPos pos, BlockPos matPos) {
    if (fixed()) {
      return;
    }
    BlockPos lowerPos = lower(world.getBlockState(pos), pos);
    IBlockState door = whole(world, lowerPos);
    openFor(world, lowerPos, door, insidePos(matPos, lowerPos, door.getValue(FACING)));
  }

  /**
   * The approach sensor ({@link AutoDoorSensors}) asks whether a player is in this door's zone:
   * within {@code range} blocks in front of or behind it, roughly in line with it (within
   * {@link #SENSOR_ALONG} of its middle along the wall, so an angled approach counts and a
   * bi-parting pair's other half sees the same person), and at its height. If so, the door opens,
   * or stays open a while longer.
   *
   * @param world    the server's world
   * @param lowerPos the door's lower half
   * @param who      the player
   * @param range    how far either side of the door its zone reaches, in blocks
   *
   * @since 1.1
   */
  void sensed(World world, BlockPos lowerPos, Entity who, double range) {
    IBlockState door = whole(world, lowerPos);
    EnumFacing f = door.getValue(FACING);
    EnumFacing along = f.rotateY();
    double dx = who.posX - (lowerPos.getX() + 0.5);
    double dz = who.posZ - (lowerPos.getZ() + 0.5);
    double depth = dx * f.getXOffset() + dz * f.getZOffset();
    double side = dx * along.getXOffset() + dz * along.getZOffset();
    double dy = who.posY - lowerPos.getY();
    if (Math.abs(depth) > range || Math.abs(side) > SENSOR_ALONG || dy < -1.0 || dy > 1.5) {
      return;
    }
    openFor(world, lowerPos, door, depth > 0);
  }

  /**
   * Opens the door and its pair for a mat or the sensor, or pushes back the time it shuts if it
   * is open already. A locked door does not open for someone outside it (free egress inside).
   */
  private void openFor(World world, BlockPos lowerPos, IBlockState door, boolean inside) {
    if (!door.getValue(OPEN) && !inside && DoorLocks.holds(world, lowerPos)) {
      return;
    }
    setOpen(world, lowerPos, true, false);
  }

  private static boolean insidePos(BlockPos p, BlockPos door, EnumFacing f) {
    return (p.getX() - door.getX()) * f.getXOffset() + (p.getZ() - door.getZ()) * f.getZOffset()
        > 0;
  }

  // --- the approach sensor's list -----------------------------------------------------------------

  @Override
  public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
    super.onBlockAdded(worldIn, pos, state);
    if (!worldIn.isRemote && !fixed() && state.getValue(HALF) == Half.LOWER) {
      AutoDoorSensors.get(worldIn).add(pos);
    }
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    if (!worldIn.isRemote && !fixed() && state.getValue(HALF) == Half.LOWER) {
      AutoDoorSensors.get(worldIn).remove(pos);
    }
    super.breakBlock(worldIn, pos, state);
  }

  /**
   * A slider's random tick only puts it on the approach sensor's list, so a door placed before the
   * sensor existed (or ever missed) is found within a few minutes of its chunk loading. Not
   * {@code super}: vanilla's random tick runs the scheduled tick, which here shuts the door.
   *
   * @since 1.1
   */
  @Override
  public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
    if (!worldIn.isRemote && !fixed() && state.getValue(HALF) == Half.LOWER) {
      AutoDoorSensors.get(worldIn).add(pos);
    }
  }

  /**
   * The lower half's tick: shut, once the hold time is up, unless redstone holds it open or
   * someone is in the doorway.
   *
   * @since 1.0
   */
  @Override
  public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    if (state.getValue(HALF) == Half.UPPER) {
      super.updateTick(worldIn, pos, state, rand);
      return;
    }
    if (!closeDue(worldIn, pos)) {
      return;
    }
    IBlockState door = whole(worldIn, pos);
    if (!door.getValue(OPEN) || worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up())) {
      return;
    }
    if (occupied(worldIn, pos, door.getValue(FACING))) {
      armClose(worldIn, pos, RECHECK_TICKS);
      return;
    }
    setOpen(worldIn, pos, false, false);
  }

  /**
   * Whether a player or a non-hostile creature is in the doorway or within a block and a half of
   * it either side, along the wall a block either way (a bi-parting pair's other half).
   */
  private static boolean occupied(World world, BlockPos lowerPos, EnumFacing f) {
    EnumFacing along = f.rotateY();
    AxisAlignedBB box = new AxisAlignedBB(lowerPos).expand(0, 1, 0)
        .grow(Math.abs(along.getXOffset()) + Math.abs(f.getXOffset()) * 1.5, 0,
            Math.abs(along.getZOffset()) + Math.abs(f.getZOffset()) * 1.5);
    List<EntityLivingBase> found = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
        e -> e != null && e.isEntityAlive() && !(e instanceof IMob)
            && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()));
    return !found.isEmpty();
  }

  /**
   * Whether a mat should answer to this entity: a player, or any creature that is not hostile.
   *
   * @param entity the entity on the mat
   * @return whether it opens the door
   *
   * @since 1.0
   */
  static boolean opensFor(Entity entity) {
    return entity instanceof EntityLivingBase && entity.isEntityAlive() && !(entity instanceof IMob)
        && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator());
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState door = getActualState(state, source, pos);
    AxisAlignedBB north;
    if (fixed()) {
      north = new AxisAlignedBB(0, 0, SIDELITE_Z0, 1, 1, SIDELITE_Z1);
    } else if (!door.getValue(OPEN)) {
      north = new AxisAlignedBB(0, 0, SLIDER_Z0, 1, 1, SLIDER_Z1);
    } else {
      // Open, only the panel's edge is left in the cell, at the side it slid to: enough to click.
      boolean left = door.getValue(HINGE) == Hinge.LEFT;
      north = new AxisAlignedBB(left ? 0 : 1 - STRIP, 0, SLIDER_Z0, left ? STRIP : 1, 1,
          SLIDER_Z1);
    }
    return BlockGarageDoor.turn(north, door.getValue(FACING));
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }

  /** Glass in the translucent pass, frames and header in the cutout pass. */
  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return CsmGlassLayer.drawsIn(layer);
  }
}
