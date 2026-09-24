package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * What a piece two blocks wide shares, whatever it extends: the upright piano, the fireplace, a
 * large TV, a wide painting. It is placed as one piece by its own item (which refuses unless both
 * blocks are free) and broken as one from either block, as the bathtub is. The block it is placed
 * in is {@link #PART} 0; the other, {@code facing.rotateY()} of it (to the right of someone
 * facing the piece's front), is part 1. The model is drawn whole, two blocks wide, and cut into
 * the two cells, so each is lit in its own block; only part 0 drops the item.
 *
 * @since 2026.9
 */
public final class WidePieces {

  /** Which of the two blocks this is: 0 where it was placed, 1 beside it. */
  public static final PropertyInteger PART = PropertyInteger.create("part", 0, 1);

  private WidePieces() {
  }

  /**
   * The other block of the piece whose block {@code state} is at {@code pos}.
   *
   * @param pos   the block
   * @param state its state
   *
   * @return the other block's position
   */
  public static BlockPos otherCell(BlockPos pos, IBlockState state) {
    EnumFacing along = state.getValue(AbstractBlockRotatableNSEW.FACING).rotateY();
    return state.getValue(PART) == 0 ? pos.offset(along) : pos.offset(along.getOpposite());
  }

  /**
   * Whether {@code pos} holds the matching other block of the piece at {@code state}.
   *
   * @param world the world
   * @param pos   the position looked at
   * @param state this block's state
   *
   * @return whether it is the other half
   */
  public static boolean isOtherCell(IBlockAccess world, BlockPos pos, IBlockState state) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == state.getBlock()
        && other.getValue(AbstractBlockRotatableNSEW.FACING)
        == state.getValue(AbstractBlockRotatableNSEW.FACING)
        && !other.getValue(PART).equals(state.getValue(PART));
  }

  /**
   * Places part 1 beside the part 0 just placed at {@code pos}.
   *
   * @param world the world
   * @param pos   part 0
   * @param state part 0's state
   */
  public static void placeOther(World world, BlockPos pos, IBlockState state) {
    if (state.getValue(PART) == 0) {
      world.setBlockState(otherCell(pos, state), state.withProperty(PART, 1), 3);
    }
  }

  /**
   * A creative player breaking part 1 takes part 0 with it, dropping nothing.
   *
   * @param world  the world
   * @param pos    the block broken
   * @param state  its state
   * @param player the player
   */
  public static void harvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(PART) == 1 && player.capabilities.isCreativeMode) {
      BlockPos other = otherCell(pos, state);
      if (isOtherCell(world, other, state)) {
        world.setBlockToAir(other);
      }
    }
  }

  /**
   * The other block goes with this one: part 1 broken on its own breaks part 0, which drops the
   * piece; part 0 clears part 1.
   *
   * @param world the world
   * @param pos   the block broken
   * @param state its state
   */
  public static void broken(World world, BlockPos pos, IBlockState state) {
    BlockPos other = otherCell(pos, state);
    if (isOtherCell(world, other, state)) {
      if (state.getValue(PART) == 1) {
        world.destroyBlock(other, true);
      } else {
        world.setBlockToAir(other);
      }
    }
  }

  /**
   * A part 1 whose part 0 has gone (a command, an explosion) goes too.
   *
   * @param world the world
   * @param pos   the block
   * @param state its state
   */
  public static void checkOther(World world, BlockPos pos, IBlockState state) {
    if (state.getValue(PART) == 1 && !isOtherCell(world, otherCell(pos, state), state)) {
      world.setBlockToAir(pos);
    }
  }

  /**
   * One block's share of the piece's box. The box is given facing north across both blocks, in
   * sixteenths, x from 0 to 32 (part 0 is x 0 to 16, part 1 x 16 to 32).
   *
   * @param whole {x0, y0, z0, x1, y1, z1} across both blocks
   * @param part  0 or 1
   *
   * @return that block's box, facing north, in blocks
   */
  public static AxisAlignedBB cellBox(int[] whole, int part) {
    double lo = part * 16;
    double x0 = Math.max(whole[0], lo) - lo;
    double x1 = Math.min(whole[3], lo + 16) - lo;
    if (x1 <= x0) {
      x0 = 0;
      x1 = 16;
    }
    return new AxisAlignedBB(x0 / 16.0, whole[1] / 16.0, whole[2] / 16.0, x1 / 16.0,
        whole[4] / 16.0, whole[5] / 16.0);
  }

  /**
   * A block that may be two blocks wide. A block that makes {@link ItemWidePiece} its item
   * implements this, so the item knows whether it has a second block to find room for.
   */
  public interface IWidePiece {

    /**
     * Whether this block is two blocks wide.
     *
     * @return true for a two-block piece
     */
    default boolean isWide() {
      return true;
    }
  }

  /**
   * The item of a two-block piece: places the whole piece, and only where both of its blocks
   * are free.
   */
  public static class ItemWidePiece extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the piece
     */
    public ItemWidePiece(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
        float hitZ, @Nonnull IBlockState newState) {
      if (!(block instanceof IWidePiece) || ((IWidePiece) block).isWide()) {
        BlockPos other = pos.offset(
            newState.getValue(AbstractBlockRotatableNSEW.FACING).rotateY());
        if (!world.getBlockState(other).getBlock().isReplaceable(world, other)
            || !player.canPlayerEdit(other, side, stack)) {
          return false;
        }
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
  }
}
