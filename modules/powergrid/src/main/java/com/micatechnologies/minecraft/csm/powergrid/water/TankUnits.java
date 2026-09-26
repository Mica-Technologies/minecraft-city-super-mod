package com.micatechnologies.minecraft.csm.powergrid.water;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * What the tanks, their parts and the pedestal column share: finding a part's root, the
 * walkway's collision, removing a unit, and refusing a placement that has no room.
 *
 * @since 2026.9
 */
final class TankUnits {

  /**
   * Set while a unit is being removed or placed by this code, so a cell's own removal (or the
   * placed block being written over) does not start a second demolition.
   */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  static final AxisAlignedBB FULL = Block.FULL_BLOCK_AABB;
  /** The balcony's grating: the walkway's floor is a sixteenth thick, at the cell's bottom. */
  static final AxisAlignedBB FLOOR = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);
  private static final double RAIL_H = 1.5;
  private static final double RAIL_T = 0.125;

  private TankUnits() {
  }

  /** The root of the unit covering {@code cell}, among the cells next to it, or null. */
  @Nullable
  static BlockPos findRoot(IBlockAccess world, BlockPos cell) {
    for (int dy = -1; dy <= 1; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          BlockPos at = cell.add(dx, dy, dz);
          IBlockState state = world.getBlockState(at);
          if (state.getBlock() instanceof ITankRoot
              && ((ITankRoot) state.getBlock()).coversCell(world, at, state, cell)) {
            return at;
          }
        }
      }
    }
    return null;
  }

  /** Whether {@code state} belongs to a tank or a column: a part, a tile or a section. */
  static boolean isTankBlock(IBlockState state) {
    return state.getBlock() instanceof BlockTankPart || state.getBlock() instanceof ITankRoot;
  }

  /** The box a part kind is selected and ray traced by: the whole cell, or the walkway floor. */
  static AxisAlignedBB selectionBox(int kind) {
    return kind == 0 ? FULL : FLOOR;
  }

  /**
   * Adds a part kind's collision: the whole cell, or the walkway's floor and its railing along
   * the sides the mask names -- except where a caged ladder comes up beside it, which is the gap
   * in the railing a real balcony has at its ladder.
   */
  static void addCollision(int kind, IBlockAccess world, BlockPos pos, AxisAlignedBB entityBox,
      List<AxisAlignedBB> boxes) {
    if (kind < 0) {
      return;
    }
    if (kind == 0) {
      add(FULL, pos, entityBox, boxes);
      return;
    }
    add(FLOOR, pos, entityBox, boxes);
    int mask = kind - 1;
    if ((mask & 1) != 0 && !ladderAt(world, pos, EnumFacing.NORTH)) {
      add(new AxisAlignedBB(0, 0, 0, 1, RAIL_H, RAIL_T), pos, entityBox, boxes);
    }
    if ((mask & 2) != 0 && !ladderAt(world, pos, EnumFacing.EAST)) {
      add(new AxisAlignedBB(1 - RAIL_T, 0, 0, 1, RAIL_H, 1), pos, entityBox, boxes);
    }
    if ((mask & 4) != 0 && !ladderAt(world, pos, EnumFacing.SOUTH)) {
      add(new AxisAlignedBB(0, 0, 1 - RAIL_T, 1, RAIL_H, 1), pos, entityBox, boxes);
    }
    if ((mask & 8) != 0 && !ladderAt(world, pos, EnumFacing.WEST)) {
      add(new AxisAlignedBB(0, 0, 0, RAIL_T, RAIL_H, 1), pos, entityBox, boxes);
    }
  }

  private static boolean ladderAt(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    return world.getBlockState(at).getBlock() instanceof BlockCagedLadder
        || world.getBlockState(at.down()).getBlock() instanceof BlockCagedLadder;
  }

  private static void add(AxisAlignedBB local, BlockPos pos, AxisAlignedBB entityBox,
      List<AxisAlignedBB> boxes) {
    AxisAlignedBB box = local.offset(pos);
    if (entityBox.intersects(box)) {
      boxes.add(box);
    }
  }

  /** Removes a cell of a unit, if it is still one of the unit's own blocks. */
  static void clear(World world, BlockPos cell) {
    if (isTankBlock(world.getBlockState(cell))) {
      world.setBlockToAir(cell);
    }
  }

  /** Drops a unit's one item where it stood, for a survival player. */
  static void drop(World world, BlockPos at, @Nullable EntityPlayer player, Block unit) {
    if (player != null && !player.capabilities.isCreativeMode && !world.isRemote) {
      Block.spawnAsEntity(world, at, new ItemStack(unit));
    }
  }

  /**
   * Takes a placed unit back when something is in its way: the block the item placed goes, the
   * item comes back to a survival player, and the player is told where it was blocked.
   */
  static void refuse(World world, BlockPos placed, EntityLivingBase placer, Block unit,
      BlockPos blockedBy) {
    DEMOLISHING.set(true);
    try {
      world.setBlockToAir(placed);
    } finally {
      DEMOLISHING.set(false);
    }
    if (placer instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) placer;
      if (!player.capabilities.isCreativeMode) {
        player.inventory.addItemStackToInventory(new ItemStack(unit));
      }
      player.sendStatusMessage(new TextComponentTranslation("csm.utilities.tank.blocked",
          blockedBy.getX(), blockedBy.getY(), blockedBy.getZ()), true);
    }
  }
}
