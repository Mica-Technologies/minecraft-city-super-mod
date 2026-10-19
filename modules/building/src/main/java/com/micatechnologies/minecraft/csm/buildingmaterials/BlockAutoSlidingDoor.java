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
 *   <li><b>Opening.</b> Redstone opens a panel (and its pair) while it is powered; a
 *   {@link BlockAutoDoorMat} beside it opens it when someone steps on the mat; a click opens it
 *   by hand; a linked keypad, button or station as ever.</li>
 *   <li><b>Closing.</b> One scheduled tick, {@link #HOLD_TICKS} after the last thing that opened
 *   it (each step on a mat pushes the time back). If someone is still in the doorway or just
 *   either side of it when the tick comes, it looks again a second later; that look is the only
 *   entity search, and only an open door makes it. Nothing ticks while the door is idle, and no
 *   tile entity exists at rest.</li>
 *   <li><b>Locked</b> by a keypad, a mat on the outside does not open it; a mat on the inside
 *   still does (free egress).</li>
 *   <li><b>The slide</b> is drawn by the swing renderer while the door moves (the swing's
 *   short-lived client tile entity, {@link TileEntityDoorSwing}), moving the panel along the wall
 *   instead of turning it; at rest it is baked models.</li>
 * </ul>
 *
 * <p>No closer, no reversing: the closer item and the Door Swing Tool refuse it.</p>
 *
 * @version 1.0
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
   * Constructs a {@link BlockAutoSlidingDoor}.
   *
   * @param registryName {@code auto_door_slider_<finish>} or {@code auto_door_sidelite_<finish>}
   *
   * @since 1.0
   */
  public BlockAutoSlidingDoor(String registryName) {
    super(registryName);
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
    if (!door.getValue(OPEN) && !insidePos(matPos, lowerPos, door.getValue(FACING))
        && DoorLocks.holds(world, lowerPos)) {
      return;
    }
    setOpen(world, lowerPos, true, false);
  }

  private static boolean insidePos(BlockPos p, BlockPos door, EnumFacing f) {
    return (p.getX() - door.getX()) * f.getXOffset() + (p.getZ() - door.getZ()) * f.getZOffset()
        > 0;
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
