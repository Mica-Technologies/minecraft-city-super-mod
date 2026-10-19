package com.micatechnologies.minecraft.csm.healthcare.clinical;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Pieces hung from the ceiling on a drop column: the ICU pendant, the surgical equipment boom and
 * the trauma bay exam light. A piece is a <em>head</em> (its own block, with its own metadata) and
 * above it a stack of {@link BlockCeilingColumn} cells up to the ceiling; the top column cell
 * carries the ceiling flange. Nothing is stored about the stack: a column is a column, and a head
 * is found by walking down the columns.
 *
 * <p><b>Placing.</b> A generator places the head with {@code /setblock} and the column cells above
 * it with {@code /setblock csm:ceiling_boom_column}, or places the head and runs
 * {@code /csmhealthcare boom fill <head x y z>}, which fills the air above the head up to the
 * ceiling. By hand, the item clicked on the underside of a ceiling puts the head at its working
 * height above the floor below ({@link ICeilingHung#getLift()}) and fills the column; placed any
 * other way the head goes where it is put and the column is filled above it. A column is filled
 * only if a ceiling is found within {@link #MAX_COLUMN} cells.</p>
 *
 * <p><b>Breaking.</b> Breaking the head or any column cell takes down the whole piece; only the
 * head drops an item.</p>
 *
 * @since 2026.10
 */
public final class CeilingHung {

  /** The most column cells above a head (the hospital's storeys have 8 clear blocks). */
  public static final int MAX_COLUMN = 8;

  /** Set while this class takes a piece down, so the cells it removes do not recurse. */
  private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

  private CeilingHung() {
  }

  /**
   * Whether a block is a ceiling-hung head.
   *
   * @param block a block
   *
   * @return true for the pendant, the boom and the exam light
   */
  public static boolean isHead(Block block) {
    return block instanceof ICeilingHung;
  }

  /**
   * Fills the air above a head with column cells, up to the first block above it.
   *
   * @param world the world
   * @param head  the head
   *
   * @return the column cells placed, or -1 if there is no ceiling within {@link #MAX_COLUMN}
   */
  public static int fillColumn(World world, BlockPos head) {
    List<BlockPos> air = new ArrayList<>();
    BlockPos at = head.up();
    for (int i = 0; i <= MAX_COLUMN; i++, at = at.up()) {
      if (at.getY() >= world.getHeight()) {
        return -1;
      }
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof BlockCeilingColumn) {
        continue;
      }
      if (s.getBlock().isAir(s, world, at)) {
        if (i == MAX_COLUMN) {
          return -1;
        }
        air.add(at);
        continue;
      }
      break;
    }
    IBlockState column = BlockCeilingColumn.get().getDefaultState();
    for (BlockPos p : air) {
      world.setBlockState(p, column, 3);
    }
    return air.size();
  }

  /**
   * The head under a column cell.
   *
   * @param world the world
   * @param pos   a column cell
   *
   * @return the head's position, or null if the column has none
   */
  public static BlockPos headBelow(World world, BlockPos pos) {
    BlockPos at = pos;
    for (int i = 0; i <= MAX_COLUMN; i++) {
      at = at.down();
      Block b = world.getBlockState(at).getBlock();
      if (isHead(b)) {
        return at;
      }
      if (!(b instanceof BlockCeilingColumn)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Takes down a piece from any of its cells: the column above the head and, from a column cell,
   * the head and the column above it.
   *
   * @param world the world
   * @param pos   the cell being broken
   * @param head  whether that cell is the head (which the caller is breaking itself)
   */
  public static void takeDown(World world, BlockPos pos, boolean head) {
    if (world.isRemote || REMOVING.get()) {
      return;
    }
    REMOVING.set(true);
    try {
      // (the cell being broken is already air when its breakBlock runs)
      BlockPos at = pos.up();
      for (int i = 0; i <= MAX_COLUMN; i++, at = at.up()) {
        if (!(world.getBlockState(at).getBlock() instanceof BlockCeilingColumn)) {
          break;
        }
        world.setBlockToAir(at);
      }
      if (!head) {
        at = pos.down();
        for (int i = 0; i <= MAX_COLUMN; i++, at = at.down()) {
          if (!(world.getBlockState(at).getBlock() instanceof BlockCeilingColumn)) {
            break;
          }
          world.setBlockToAir(at);
        }
        if (isHead(world.getBlockState(at).getBlock())) {
          world.destroyBlock(at, true);
        }
      }
    } finally {
      REMOVING.set(false);
    }
  }

  /**
   * Removes the column above a head, leaving the head.
   *
   * @param world the world
   * @param head  the head
   *
   * @return the cells removed
   */
  public static int clearColumn(World world, BlockPos head) {
    REMOVING.set(true);
    try {
      int n = 0;
      for (BlockPos p = head.up(); world.getBlockState(p).getBlock()
          instanceof BlockCeilingColumn; p = p.up()) {
        world.setBlockToAir(p);
        n++;
      }
      return n;
    } finally {
      REMOVING.set(false);
    }
  }

  /**
   * Where a hand-placed head goes when its item is used on the underside of a ceiling: the cell
   * {@code lift} above the floor below, or {@code pos} if there is no floor in reach.
   *
   * @param world the world
   * @param pos   the cell under the ceiling the item was used on
   * @param lift  the head's height in cells above the floor cell
   *
   * @return the head's position
   */
  public static BlockPos headFromCeiling(World world, BlockPos pos, int lift) {
    BlockPos at = pos;
    for (int i = 0; i <= MAX_COLUMN + 1; i++) {
      BlockPos below = at.down();
      IBlockState s = world.getBlockState(below);
      if (below.getY() < 0 || !s.getBlock().isAir(s, world, below)) {
        BlockPos head = at.up(lift);
        return head.getY() > pos.getY() ? pos : head;
      }
      at = below;
    }
    return pos;
  }

  /**
   * The item of a ceiling-hung head: places the head at its working height when used on a
   * ceiling, and fills the column above it.
   */
  public static class ItemCeilingHung extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the head
     */
    public ItemCeilingHung(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockPos at = pos;
      if (side == EnumFacing.DOWN) {
        at = headFromCeiling(world, pos, ((ICeilingHung) block).getLift());
        if (!world.getBlockState(at).getBlock().isReplaceable(world, at)) {
          at = pos;
        }
      }
      if (!super.placeBlockAt(stack, player, world, at, side, hitX, hitY, hitZ, newState)) {
        return false;
      }
      if (!world.isRemote) {
        fillColumn(world, at);
      }
      return true;
    }
  }
}
