package com.micatechnologies.minecraft.csm.signage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Builds, resizes and takes down advertising boards: everything that changes which blocks a
 * board is made of. Server side only, except {@link #findController}, which the client's screen
 * uses too.
 *
 * <p>A board is a rectangle in one vertical plane: {@code width} columns by {@code height} rows,
 * the controller in the bottom row at the column its {@link AdBoardAlign} gives. Cell
 * {@code (column, row)} is the controller moved {@code column - controllerColumn} to the right
 * (seen from the front) and {@code row} up.</p>
 *
 * <p>In survival every block of a board costs one of the board's item, as if each had been placed
 * by hand: growing a board takes them from the player's inventory, shrinking it gives them back,
 * and breaking it drops them all.</p>
 */
public final class AdBoards {

  /** The most blocks a board of any kind can have; a search for a controller stops here. */
  static final int MAX_CELLS = 40 * 40;

  /** Set while this class is changing a board, so the blocks it removes do not tear it down. */
  private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

  private AdBoards() {
  }

  /** The block at {@code (column, row)} of a board whose controller is at {@code controller}. */
  static BlockPos cell(BlockPos controller, EnumFacing facing, int controllerColumn, int column,
      int row) {
    return controller.offset(AbstractBlockAdBoard.right(facing), column - controllerColumn)
        .up(row);
  }

  /**
   * The controller of the board {@code start} is a block of, found by walking the board's blocks
   * outward from {@code start}. Returns {@code null} if {@code start} is not a board block or the
   * walk finds no controller.
   */
  @Nullable
  public static BlockPos findController(IBlockAccess world, BlockPos start) {
    IBlockState state = world.getBlockState(start);
    if (!(state.getBlock() instanceof AbstractBlockAdBoard)) {
      return null;
    }
    return findController(world, start, state);
  }

  @Nullable
  private static BlockPos findController(IBlockAccess world, BlockPos start, IBlockState like) {
    AbstractBlockAdBoard block = (AbstractBlockAdBoard) like.getBlock();
    AdBoardKind kind = block.kind();
    EnumFacing facing = like.getValue(AbstractBlockAdBoard.FACING);
    int tag = like.getValue(AbstractBlockAdBoard.TAG);
    EnumFacing right = AbstractBlockAdBoard.right(facing);
    EnumFacing[] ways = {right, right.getOpposite(), EnumFacing.UP, EnumFacing.DOWN};
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    seen.add(start.toImmutable());
    todo.add(start.toImmutable());
    while (!todo.isEmpty() && seen.size() <= MAX_CELLS) {
      BlockPos p = todo.poll();
      IBlockState s = world.getBlockState(p);
      if (s.getBlock() instanceof BlockAdBoard && AbstractBlockAdBoard.sameBoard(s, kind, facing,
          tag)) {
        return p;
      }
      for (EnumFacing way : ways) {
        BlockPos q = p.offset(way);
        if (!seen.contains(q) && loaded(world, q)
            && AbstractBlockAdBoard.sameBoard(world.getBlockState(q), kind, facing, tag)) {
          seen.add(q);
          todo.add(q);
        }
      }
    }
    return null;
  }

  private static boolean loaded(IBlockAccess world, BlockPos pos) {
    return !(world instanceof World) || ((World) world).isBlockLoaded(pos);
  }

  /**
   * A tag for a board of {@code width} by {@code height} blocks round {@code controller} that no
   * board of the same kind and facing touching it already has, preferring {@code own}.
   *
   * @param ignore blocks that are not to count as a neighbour: the board's own, while it is being
   *               resized
   * @return the tag, or -1 if all four are taken
   */
  static int freeTag(IBlockAccess world, AdBoardKind kind, EnumFacing facing, BlockPos controller,
      int controllerColumn, int width, int height, int own, Predicate<BlockPos> ignore) {
    boolean[] taken = new boolean[4];
    for (int column = -1; column <= width; column++) {
      for (int row = -1; row <= height; row++) {
        boolean outside = column == -1 || column == width || row == -1 || row == height;
        boolean corner = (column == -1 || column == width) && (row == -1 || row == height);
        if (!outside || corner) {
          continue;
        }
        BlockPos p = cell(controller, facing, controllerColumn, column, row);
        if (ignore.test(p) || !loaded(world, p)) {
          continue;
        }
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() instanceof AbstractBlockAdBoard
            && ((AbstractBlockAdBoard) s.getBlock()).kind() == kind
            && s.getValue(AbstractBlockAdBoard.FACING) == facing) {
          taken[s.getValue(AbstractBlockAdBoard.TAG)] = true;
        }
      }
    }
    if (own >= 0 && own < 4 && !taken[own]) {
      return own;
    }
    for (int tag = 0; tag < 4; tag++) {
      if (!taken[tag]) {
        return tag;
      }
    }
    return -1;
  }

  /** The tag for a controller placed on its own at {@code pos}; 0 if every tag is taken. */
  static int freeTag(IBlockAccess world, AdBoardKind kind, EnumFacing facing, BlockPos pos) {
    return Math.max(0, freeTag(world, kind, facing, pos, 0, 1, 1, -1, p -> false));
  }

  /**
   * Makes the board whose controller is at {@code controller} {@code width} by {@code height},
   * aligned {@code align}: parts are added where the board grows, removed where it shrinks, and
   * every block re-tagged if a neighbour now holds its tag. Nothing changes if anything is in the
   * way, a block it needs is not loaded, the player cannot pay for the blocks, or no tag is free.
   *
   * @return {@code null} on success, otherwise what to tell the player
   */
  @Nullable
  public static ITextComponent resize(World world, BlockPos controller, EntityPlayer player,
      int width, int height, AdBoardAlign align) {
    IBlockState state = world.getBlockState(controller);
    TileEntity te = world.getTileEntity(controller);
    if (!(state.getBlock() instanceof BlockAdBoard) || !(te instanceof TileEntityAdBoard)) {
      return null;
    }
    TileEntityAdBoard board = (TileEntityAdBoard) te;
    AdBoardKind kind = ((BlockAdBoard) state.getBlock()).kind();
    EnumFacing facing = state.getValue(AbstractBlockAdBoard.FACING);
    int tag = state.getValue(AbstractBlockAdBoard.TAG);
    width = Math.max(1, Math.min(kind.getMaxWidth(), width));
    height = Math.max(1, Math.min(kind.getMaxHeight(), height));
    int column = align.controllerColumn(width);

    Set<BlockPos> old = cells(controller, facing, board.getControllerColumn(), board.getWidth(),
        board.getHeight());
    Set<BlockPos> wanted = cells(controller, facing, column, width, height);

    // 1. Everything the new board needs must be free, or this board already.
    for (BlockPos p : wanted) {
      if (p.equals(controller)) {
        continue;
      }
      if (p.getY() < 0 || p.getY() >= world.getHeight()) {
        return new TextComponentTranslation("chat.csm.adboard.outside");
      }
      if (!world.isBlockLoaded(p)) {
        return new TextComponentTranslation("chat.csm.adboard.unloaded");
      }
      IBlockState s = world.getBlockState(p);
      boolean ours = old.contains(p) && AbstractBlockAdBoard.sameBoard(s, kind, facing, tag);
      if (!ours && !s.getBlock().isReplaceable(world, p)) {
        return new TextComponentTranslation("chat.csm.adboard.blocked", p.getX(), p.getY(),
            p.getZ(), s.getBlock().getLocalizedName());
      }
    }

    // 2. A tag no neighbour has.
    int newTag = freeTag(world, kind, facing, controller, column, width, height, tag,
        old::contains);
    if (newTag < 0) {
      return new TextComponentTranslation("chat.csm.adboard.tags");
    }

    // 3. The blocks it costs, or gives back.
    int change = wanted.size() - old.size();
    Item item = Item.getItemFromBlock(AbstractBlockAdBoard.controller(kind));
    boolean pays = player != null && !player.capabilities.isCreativeMode;
    if (pays && change > 0 && count(player, item) < change) {
      return new TextComponentTranslation("chat.csm.adboard.items", change,
          new ItemStack(item).getDisplayName());
    }

    BUSY.set(true);
    try {
      for (BlockPos p : old) {
        if (!wanted.contains(p) && AbstractBlockAdBoard.sameBoard(world.getBlockState(p), kind,
            facing, tag)) {
          world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
        }
      }
      IBlockState part = AbstractBlockAdBoard.part(kind).getDefaultState()
          .withProperty(AbstractBlockAdBoard.FACING, facing)
          .withProperty(AbstractBlockAdBoard.TAG, newTag);
      for (BlockPos p : wanted) {
        if (p.equals(controller)) {
          continue;
        }
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() != part.getBlock() || s.getValue(AbstractBlockAdBoard.TAG) != newTag
            || s.getValue(AbstractBlockAdBoard.FACING) != facing) {
          world.setBlockState(p, part, 3);
        }
      }
      if (newTag != tag) {
        // Same block, so the controller keeps its tile entity (shouldRefresh compares blocks).
        world.setBlockState(controller, state.withProperty(AbstractBlockAdBoard.TAG, newTag), 3);
      }
    } finally {
      BUSY.set(false);
    }

    if (pays) {
      if (change > 0) {
        take(player, item, change);
      } else if (change < 0) {
        give(player, item, -change);
      }
    }
    board.setSize(width, height, align);
    board.markDirtySync(world, controller, true);
    return null;
  }

  /** Every block of a board, as positions. */
  static Set<BlockPos> cells(BlockPos controller, EnumFacing facing, int controllerColumn,
      int width, int height) {
    Set<BlockPos> out = new HashSet<>();
    for (int column = 0; column < width; column++) {
      for (int row = 0; row < height; row++) {
        out.add(cell(controller, facing, controllerColumn, column, row));
      }
    }
    return out;
  }

  /**
   * A block of a board has gone -- broken, blown up, replaced -- so the rest of the board goes
   * with it. A survival player who broke it gets every block of the board back as items.
   *
   * @param state   the block that was at {@code pos}
   * @param breaker the player who broke it, or {@code null}
   */
  static void onBlockGone(World world, BlockPos pos, IBlockState state,
      @Nullable EntityPlayer breaker) {
    if (world.isRemote || BUSY.get()) {
      return;
    }
    BlockPos controller = null;
    TileEntityAdBoard board = null;
    if (state.getBlock() instanceof BlockAdBoard) {
      controller = pos;
      TileEntity te = world.getTileEntity(pos);
      board = te instanceof TileEntityAdBoard ? (TileEntityAdBoard) te : null;
    } else {
      // A part: the block is already gone, so the walk starts from its neighbours.
      EnumFacing right = AbstractBlockAdBoard.right(state.getValue(AbstractBlockAdBoard.FACING));
      for (EnumFacing way : new EnumFacing[]{right, right.getOpposite(), EnumFacing.UP,
          EnumFacing.DOWN}) {
        BlockPos q = pos.offset(way);
        if (world.isBlockLoaded(q) && ((AbstractBlockAdBoard) state.getBlock())
            .sameBoard(world, q, state)) {
          controller = findController(world, q, state);
          if (controller != null) {
            break;
          }
        }
      }
      if (controller != null) {
        TileEntity te = world.getTileEntity(controller);
        board = te instanceof TileEntityAdBoard ? (TileEntityAdBoard) te : null;
      }
    }
    if (controller == null || board == null) {
      return;
    }
    AdBoardKind kind = ((AbstractBlockAdBoard) state.getBlock()).kind();
    EnumFacing facing = state.getValue(AbstractBlockAdBoard.FACING);
    int tag = state.getValue(AbstractBlockAdBoard.TAG);
    int blocks = board.getWidth() * board.getHeight();
    BUSY.set(true);
    try {
      for (BlockPos p : cells(controller, facing, board.getControllerColumn(), board.getWidth(),
          board.getHeight())) {
        if (!p.equals(pos) && world.isBlockLoaded(p)
            && AbstractBlockAdBoard.sameBoard(world.getBlockState(p), kind, facing, tag)) {
          world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
        }
      }
    } finally {
      BUSY.set(false);
    }
    if (breaker != null && !breaker.capabilities.isCreativeMode) {
      Item item = Item.getItemFromBlock(AbstractBlockAdBoard.controller(kind));
      drop(world, pos, item, blocks);
    }
  }

  // --- items --------------------------------------------------------------------------------

  private static int count(EntityPlayer player, Item item) {
    int n = 0;
    for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
      ItemStack stack = player.inventory.getStackInSlot(i);
      if (stack.getItem() == item) {
        n += stack.getCount();
      }
    }
    return n;
  }

  private static void take(EntityPlayer player, Item item, int n) {
    for (int i = 0; i < player.inventory.getSizeInventory() && n > 0; i++) {
      ItemStack stack = player.inventory.getStackInSlot(i);
      if (stack.getItem() == item) {
        int used = Math.min(n, stack.getCount());
        stack.shrink(used);
        n -= used;
      }
    }
    player.inventory.markDirty();
  }

  private static void give(EntityPlayer player, Item item, int n) {
    while (n > 0) {
      int size = Math.min(n, item.getItemStackLimit());
      ItemStack stack = new ItemStack(item, size);
      if (!player.inventory.addItemStackToInventory(stack) || !stack.isEmpty()) {
        player.dropItem(stack, false);
      }
      n -= size;
    }
  }

  private static void drop(World world, BlockPos pos, Item item, int n) {
    while (n > 0) {
      int size = Math.min(n, item.getItemStackLimit());
      Block.spawnAsEntity(world, pos, new ItemStack(item, size));
      n -= size;
    }
  }
}
