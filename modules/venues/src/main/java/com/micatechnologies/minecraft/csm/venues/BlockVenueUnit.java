package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A Sports &amp; Venues piece bigger than one block, drawn whole by one block (the
 * <em>master</em>): the hockey goal, the basketball stanchion, the ice resurfacer and the ice
 * plant. The master carries the whole unit's baked model and its facing; invisible
 * {@link BlockVenueUnitPart} cells fill the rest of its footprint, so it is solid and can be
 * clicked anywhere. (The pattern of Roads' utility box and Healthcare's scanners, which this
 * module may not name.)
 *
 * <p><b>Footprint.</b> Every unit lists its cells as offsets from the master in the frame its
 * model is drawn in, facing north: +x east, +z south (away from the front), +y up. The master is
 * on the unit's bottom layer. {@code gen_venues_arena.py} writes the cells and the boxes into the
 * tab line from the geometry it draws.</p>
 *
 * <p><b>Placing.</b> Metadata is the facing (S 0, W 1, N 2, E 3), the way the unit's front
 * faces. A {@code /setblock} of the master alone is enough: when it is added to the world it
 * fills every cell of its footprint that is free (air or replaceable) with a part, and leaves the
 * rest. By hand, the item places the unit only where every cell is free.</p>
 *
 * <p>A part's metadata is the {@link EnumFacing} index of its step towards the master, so a part
 * finds its master in a few lookups. Breaking the master or any part takes the whole unit; only
 * the master drops an item.</p>
 *
 * @since 2026.10
 */
public class BlockVenueUnit extends BlockVenue {

  /** Set while a unit removes its own cells, so the removals do not recurse. */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  /** How high above its cell's floor a cell's share of the unit's boxes may reach, in blocks. */
  private static final double SHARE_TOP = 1.5;

  private final int[][] cells;
  private final EnumFacing[] stepToMaster;
  private final AxisAlignedBB[] boxes;
  private final AxisAlignedBB whole;
  private final AxisAlignedBB masterShare;

  /**
   * Constructs a unit.
   *
   * @param registryName its registry name
   * @param cells        the footprint after the master: {dx, dy, dz} triples, model frame, each
   *                     connected to the master through others
   * @param boxes        its solid boxes, model frame, in sixteenths relative to the master cell:
   *                     {x0, y0, z0, x1, y1, z1} sextuples, which may reach past the cell
   */
  public BlockVenueUnit(String registryName, int[] cells, int[] boxes) {
    super(registryName, Material.IRON, SoundType.METAL, 2.0F);
    int n = cells.length / 3;
    this.cells = new int[n][];
    for (int i = 0; i < n; i++) {
      this.cells[i] = new int[]{cells[i * 3], cells[i * 3 + 1], cells[i * 3 + 2]};
    }
    this.stepToMaster = paths(this.cells);
    this.boxes = new AxisAlignedBB[boxes.length / 6];
    AxisAlignedBB u = null;
    for (int i = 0; i < this.boxes.length; i++) {
      int o = i * 6;
      this.boxes[i] = new AxisAlignedBB(boxes[o] / 16.0, boxes[o + 1] / 16.0,
          boxes[o + 2] / 16.0, boxes[o + 3] / 16.0, boxes[o + 4] / 16.0, boxes[o + 5] / 16.0);
      u = u == null ? this.boxes[i] : u.union(this.boxes[i]);
    }
    this.whole = u;
    this.masterShare = share(this.boxes, new AxisAlignedBB(0, 0, 0, 1, SHARE_TOP, 1));
  }

  /** The union of the boxes' parts inside {@code cell}, or a thin slab if none. */
  static AxisAlignedBB share(AxisAlignedBB[] boxes, AxisAlignedBB cell) {
    AxisAlignedBB out = null;
    for (AxisAlignedBB b : boxes) {
      if (b.intersects(cell)) {
        AxisAlignedBB c = b.intersect(cell);
        out = out == null ? c : out.union(c);
      }
    }
    return out != null ? out : new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);
  }

  private static EnumFacing[] paths(int[][] cells) {
    Map<Long, Integer> index = new HashMap<>();
    for (int i = 0; i < cells.length; i++) {
      index.put(key(cells[i][0], cells[i][1], cells[i][2]), i);
    }
    EnumFacing[] step = new EnumFacing[cells.length];
    Deque<int[]> queue = new ArrayDeque<>();
    queue.add(new int[]{0, 0, 0});
    while (!queue.isEmpty()) {
      int[] at = queue.poll();
      for (EnumFacing f : EnumFacing.values()) {
        int[] next = {at[0] + f.getXOffset(), at[1] + f.getYOffset(), at[2] + f.getZOffset()};
        Integer i = index.get(key(next[0], next[1], next[2]));
        if (i != null && step[i] == null) {
          step[i] = f.getOpposite();
          queue.add(next);
        }
      }
    }
    for (int i = 0; i < step.length; i++) {
      if (step[i] == null) {
        throw new IllegalArgumentException("unit cell " + i + " is not connected to the master");
      }
    }
    return step;
  }

  private static long key(int x, int y, int z) {
    return ((long) (x + 512) << 20) | ((long) (y + 512) << 10) | (z + 512);
  }

  // --- geometry ------------------------------------------------------------------------------

  private static BlockPos turn(int dx, int dy, int dz, EnumFacing facing) {
    AxisAlignedBB b = RotationUtils.rotateBoundingBoxByFacing(
        new AxisAlignedBB(dx, dy, dz, dx + 1, dy + 1, dz + 1), facing);
    return new BlockPos((int) Math.round(b.minX), (int) Math.round(b.minY),
        (int) Math.round(b.minZ));
  }

  private static EnumFacing turn(EnumFacing step, EnumFacing facing) {
    if (step.getAxis() == EnumFacing.Axis.Y) {
      return step;
    }
    BlockPos p = turn(step.getXOffset(), 0, step.getZOffset(), facing);
    return EnumFacing.getFacingFromVector(p.getX(), 0, p.getZ());
  }

  /**
   * The world positions of the unit's cells after the master.
   *
   * @param master the master's position
   * @param facing its facing
   *
   * @return the cells, in footprint order
   */
  public List<BlockPos> partCells(BlockPos master, EnumFacing facing) {
    List<BlockPos> out = new ArrayList<>(cells.length);
    for (int[] c : cells) {
      out.add(master.add(turn(c[0], c[1], c[2], facing)));
    }
    return out;
  }

  /** Whether the unit with its master at {@code master} covers {@code cell}. */
  boolean covers(BlockPos master, EnumFacing facing, BlockPos cell) {
    for (int[] c : cells) {
      if (master.add(turn(c[0], c[1], c[2], facing)).equals(cell)) {
        return true;
      }
    }
    return false;
  }

  /** The unit's boxes in world coordinates. */
  AxisAlignedBB[] worldBoxes(BlockPos master, EnumFacing facing) {
    AxisAlignedBB[] out = new AxisAlignedBB[boxes.length];
    for (int i = 0; i < boxes.length; i++) {
      out[i] = RotationUtils.rotateBoundingBoxByFacing(boxes[i], facing).offset(master);
    }
    return out;
  }

  /** The whole unit's outline in the world. */
  AxisAlignedBB worldOutline(BlockPos master, EnumFacing facing) {
    return RotationUtils.rotateBoundingBoxByFacing(whole, facing).offset(master);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return masterShare;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    return worldOutline(pos, state.getValue(FACING));
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  // --- placing and breaking ------------------------------------------------------------------

  /**
   * Fills every free cell of the unit with a part.
   *
   * @param world  the world
   * @param master the master's position
   * @param state  its state
   *
   * @return the parts placed
   */
  public int fillParts(World world, BlockPos master, IBlockState state) {
    Block part = CsmRegistry.getBlock(BlockVenueUnitPart.REGISTRY_NAME);
    if (part == null) {
      return 0;
    }
    EnumFacing facing = state.getValue(FACING);
    int placed = 0;
    for (int i = 0; i < cells.length; i++) {
      int[] c = cells[i];
      BlockPos at = master.add(turn(c[0], c[1], c[2], facing));
      IBlockState here = world.getBlockState(at);
      if (here.getBlock() instanceof BlockVenueUnitPart
          || !here.getBlock().isReplaceable(world, at)) {
        continue;
      }
      world.setBlockState(at, part.getDefaultState().withProperty(
          BlockVenueUnitPart.TOWARD, turn(stepToMaster[i], facing)), 3);
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
        for (int[] c : cells) {
          BlockPos at = pos.add(turn(c[0], c[1], c[2], facing));
          if (world.getBlockState(at).getBlock() instanceof BlockVenueUnitPart) {
            world.setBlockToAir(at);
          }
        }
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /**
   * The master of the unit covering a part, following the parts' steps.
   *
   * @param world the world
   * @param part  a part's position
   *
   * @return the master's position, or null for an orphan
   */
  @Nullable
  public static BlockPos findMaster(IBlockAccess world, BlockPos part) {
    BlockPos at = part;
    for (int i = 0; i < 16; i++) {
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof BlockVenueUnitPart) {
        at = at.offset(s.getValue(BlockVenueUnitPart.TOWARD));
        continue;
      }
      if (s.getBlock() instanceof BlockVenueUnit) {
        BlockVenueUnit unit = (BlockVenueUnit) s.getBlock();
        return unit.covers(at, s.getValue(FACING), part) ? at : null;
      }
      return null;
    }
    return null;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemUnit(this);
  }

  /** The item: places the unit only where every one of its cells is free. */
  public static class ItemUnit extends ItemBlock {

    public ItemUnit(BlockVenueUnit block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockVenueUnit unit = (BlockVenueUnit) block;
      for (BlockPos cell : unit.partCells(pos, newState.getValue(FACING))) {
        if (cell.getY() < 0 || cell.getY() >= world.getHeight()
            || !world.getBlockState(cell).getBlock().isReplaceable(world, cell)
            || !player.canPlayerEdit(cell, side, stack)) {
          if (!world.isRemote) {
            player.sendStatusMessage(new TextComponentTranslation("csm.venues.unit.blocked",
                cell.getX(), cell.getY(), cell.getZ()), true);
          }
          return false;
        }
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
  }
}
