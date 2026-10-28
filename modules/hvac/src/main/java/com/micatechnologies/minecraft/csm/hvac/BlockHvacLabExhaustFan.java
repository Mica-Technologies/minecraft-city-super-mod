package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The high-plume lab exhaust fan (#314): the fan a lab building's exhaust risers end in, on its
 * roof curb, 2 x 2 blocks and five tall -- a bypass air plenum, the fan housing with its motor,
 * and a white discharge nozzle three blocks tall in a windband, throwing the exhaust well clear of
 * the roof's air intakes.
 *
 * <p>This block is the <em>master</em>: it draws the curb, plenum and housing (the bottom two
 * blocks, reaching one block past its cell, the JSON limit) and carries the fan's tile entity.
 * Invisible {@link BlockHvacExhaustFanPart} cells fill the rest of the footprint, so the unit is
 * solid and can be clicked anywhere, as the Healthcare module's CT scanner is; the parts over the
 * master draw the nozzle a block at a time, so nothing is drawn more than a block from the cell
 * that draws it.</p>
 *
 * <p><b>Footprint</b>, in the frame the model is drawn in facing north (+x east, +z towards the
 * back, +y up): x 0..1, z 0..1, y 0..4, the master at (0, 0, 0). <b>Placing:</b> metadata is the
 * facing (S 0, W 1, N 2, E 3); a {@code /setblock} of the master alone is enough, since it fills
 * every free cell of its footprint with a part when it is added. By hand, the item places the
 * unit only where all twenty cells are free. Breaking any cell takes the whole unit; only the
 * master drops an item.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacLabExhaustFan extends BlockHvacExhaustFan {

  /** Levels of the unit: curb and plenum, fan, two of nozzle, the windband. */
  static final int LEVELS = 5;

  /** Set while a unit removes its own cells, so the removals do not recurse. */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  /** The unit's cells after the master, model frame, as {dx, dy, dz}. */
  static final int[][] CELLS;

  static {
    List<int[]> c = new ArrayList<>();
    for (int y = 0; y < LEVELS; y++) {
      for (int x = 0; x < 2; x++) {
        for (int z = 0; z < 2; z++) {
          if (x != 0 || y != 0 || z != 0) {
            c.add(new int[]{x, y, z});
          }
        }
      }
    }
    CELLS = c.toArray(new int[0][]);
  }

  /** The unit's solid boxes, model frame, in blocks relative to the master cell. */
  private static final AxisAlignedBB[] BOXES = {
      box(1, 0, 1, 31, 16, 31),   // curb and plenum
      box(5, 16, 5, 27, 32, 27),  // fan housing
      box(24, 17, 11, 32, 30, 19), // motor and belt guard
      box(9, 32, 9, 23, 70, 23),  // nozzle
      box(6, 68, 6, 26, 78, 26),  // windband
  };

  private static final AxisAlignedBB WHOLE;

  static {
    AxisAlignedBB u = BOXES[0];
    for (AxisAlignedBB b : BOXES) {
      u = u.union(b);
    }
    WHOLE = u;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  public BlockHvacLabExhaustFan(String registryName) {
    super(registryName);
  }

  // --- geometry ------------------------------------------------------------------------------

  /** A model-frame offset turned to the world for a facing. */
  static BlockPos turn(int dx, int dy, int dz, EnumFacing facing) {
    AxisAlignedBB b = RotationUtils.rotateBoundingBoxByFacing(
        new AxisAlignedBB(dx, dy, dz, dx + 1, dy + 1, dz + 1), facing);
    return new BlockPos((int) Math.round(b.minX), (int) Math.round(b.minY),
        (int) Math.round(b.minZ));
  }

  /** What a part at a model-frame cell draws. */
  static BlockHvacExhaustFanPart.Piece pieceAt(int dx, int dy, int dz) {
    if (dx != 0 || dz != 0 || dy < 2) {
      return BlockHvacExhaustFanPart.Piece.NONE;
    }
    return dy == LEVELS - 1 ? BlockHvacExhaustFanPart.Piece.TOP
        : BlockHvacExhaustFanPart.Piece.STACK;
  }

  /** The world positions of the unit's cells after the master, in {@link #CELLS} order. */
  public static List<BlockPos> partCells(BlockPos master, EnumFacing facing) {
    List<BlockPos> out = new ArrayList<>(CELLS.length);
    for (int[] c : CELLS) {
      out.add(master.add(turn(c[0], c[1], c[2], facing)));
    }
    return out;
  }

  /** Whether the unit with its master at {@code master} covers {@code cell}. */
  static boolean covers(BlockPos master, EnumFacing facing, BlockPos cell) {
    for (int[] c : CELLS) {
      if (master.add(turn(c[0], c[1], c[2], facing)).equals(cell)) {
        return true;
      }
    }
    return false;
  }

  /** The unit's boxes in world coordinates. */
  static List<AxisAlignedBB> worldBoxes(BlockPos master, EnumFacing facing) {
    List<AxisAlignedBB> out = new ArrayList<>(BOXES.length);
    for (AxisAlignedBB b : BOXES) {
      out.add(RotationUtils.rotateBoundingBoxByFacing(b, facing).offset(master));
    }
    return out;
  }

  /** The whole unit's outline in the world. */
  static AxisAlignedBB worldOutline(BlockPos master, EnumFacing facing) {
    return RotationUtils.rotateBoundingBoxByFacing(WHOLE, facing).offset(master);
  }

  /**
   * The master of the unit a part belongs to: down the part's column to the unit's bottom, then
   * the master is under it (the master's own column) or beside it, a footprint step away.
   *
   * @param world the world
   * @param part  a part's position
   *
   * @return the master's position, or null for an orphan
   */
  @Nullable
  static BlockPos findMaster(IBlockAccess world, BlockPos part) {
    IBlockState here = world.getBlockState(part);
    if (!(here.getBlock() instanceof BlockHvacExhaustFanPart)) {
      return null;
    }
    EnumFacing facing = here.getValue(FACING);
    BlockPos bottom = part;
    for (int i = 0; i < LEVELS; i++) {
      IBlockState below = world.getBlockState(bottom.down());
      if (!(below.getBlock() instanceof BlockHvacExhaustFanPart)) {
        break;
      }
      bottom = bottom.down();
    }
    BlockPos under = bottom.down();
    if (isMasterOf(world, under, facing, part)) {
      return under;
    }
    for (int[] col : new int[][]{{1, 0}, {0, 1}, {1, 1}}) {
      BlockPos m = bottom.subtract(turn(col[0], 0, col[1], facing));
      if (isMasterOf(world, m, facing, part)) {
        return m;
      }
    }
    return null;
  }

  private static boolean isMasterOf(IBlockAccess world, BlockPos m, EnumFacing facing,
      BlockPos part) {
    IBlockState s = world.getBlockState(m);
    return s.getBlock() instanceof BlockHvacLabExhaustFan && s.getValue(FACING) == facing
        && covers(m, facing, part);
  }

  // --- shape ---------------------------------------------------------------------------------

  /** The master cell's share of the boxes, model frame (the base class turns it). */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box(1, 0, 1, 16, 16, 16);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    return worldOutline(pos, state.getValue(FACING));
  }

  // --- placing and breaking ------------------------------------------------------------------

  /** Fills every free cell of the unit with a part; returns how many were placed. */
  public int fillParts(World world, BlockPos master, IBlockState state) {
    Block part = CsmRegistry.getBlock(BlockHvacExhaustFanPart.REGISTRY_NAME);
    if (part == null) {
      return 0;
    }
    EnumFacing facing = state.getValue(FACING);
    int placed = 0;
    for (int[] c : CELLS) {
      BlockPos at = master.add(turn(c[0], c[1], c[2], facing));
      if (at.getY() < 0 || at.getY() >= world.getHeight()) {
        continue;
      }
      IBlockState here = world.getBlockState(at);
      if (here.getBlock() instanceof BlockHvacExhaustFanPart
          || !here.getBlock().isReplaceable(world, at)) {
        continue;
      }
      world.setBlockState(at, part.getDefaultState()
          .withProperty(BlockHvacExhaustFanPart.FACING, facing)
          .withProperty(BlockHvacExhaustFanPart.PIECE, pieceAt(c[0], c[1], c[2])), 3);
      placed++;
    }
    return placed;
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      fillParts(world, pos, state);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get()) {
      DEMOLISHING.set(true);
      try {
        EnumFacing facing = state.getValue(FACING);
        for (BlockPos at : partCells(pos, facing)) {
          IBlockState s = world.getBlockState(at);
          if (s.getBlock() instanceof BlockHvacExhaustFanPart
              && s.getValue(BlockHvacExhaustFanPart.FACING) == facing) {
            world.setBlockToAir(at);
          }
        }
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemUnit(this);
  }

  /** The item: places the unit only where every one of its cells is free. */
  public static class ItemUnit extends ItemBlock {

    public ItemUnit(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      for (BlockPos cell : partCells(pos, newState.getValue(FACING))) {
        if (cell.getY() < 0 || cell.getY() >= world.getHeight()
            || !world.getBlockState(cell).getBlock().isReplaceable(world, cell)
            || !player.canPlayerEdit(cell, side, stack)) {
          if (!world.isRemote) {
            player.sendStatusMessage(new TextComponentString(String.format(
                "Not enough room -- blocked at %d, %d, %d", cell.getX(), cell.getY(),
                cell.getZ())), true);
          }
          return false;
        }
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
  }
}
