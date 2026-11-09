package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A large packaged rooftop unit: the cooling section, the gas heat section and the supply fan in
 * one cabinet on a roof curb, several blocks across, so it both heats and cools
 * ({@link TileEntityHvacPackagedRtu}, a combination unit), through vents only.
 *
 * <p>This block is the <em>master</em>, at the middle of the unit's front edge: it carries the
 * tile entity and draws its own cell of the unit. Every other cell of the footprint is a
 * {@link BlockHvacPackagedRtuPart} that draws its own slice of the same model (cut by
 * {@code dev-env-utils/scripts/gen_hvac_units.py}), so nothing is drawn outside the cell that
 * draws it: one model drawn from the master would vanish whenever the master's chunk section is
 * culled, and nothing could collide with or be mounted on the air under it.</p>
 *
 * <p><b>Footprint</b> ({@link Size}), in the frame the model is drawn in facing north (+x east,
 * +y up, +z towards the back): the master at (0, 0, 0), the unit reaching back from the placer.
 * <b>Placing:</b> metadata is the facing (S 0, W 1, N 2, E 3); a {@code /setblock} of the master
 * alone fills every free cell of its footprint with parts when it is added. By hand, the item
 * places the unit only where every cell is free. Breaking any cell takes the whole unit; only the
 * master drops an item.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class BlockHvacPackagedRtu extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** A unit's size: its footprint, its solid boxes, its capacity and the block its cells are. */
  public static final class Size {

    final String partName;
    final int x0;
    final int x1;
    final int y1;
    final int z1;
    final float capacity;
    /** The unit's solid boxes, model frame, in blocks from the master cell's corner. */
    final AxisAlignedBB[] boxes;
    final AxisAlignedBB whole;
    /** The cells after the master, y then z then x: the order of the parts' {@link #cell}. */
    final List<int[]> cells;
    /** A part's cell, its index in {@link #cells}; {@code cells.size()} for none. */
    final PropertyInteger cell;

    Size(String partName, int x0, int x1, int y1, int z1, float capacity,
        AxisAlignedBB... boxes) {
      this.partName = partName;
      this.x0 = x0;
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
      this.capacity = capacity;
      this.boxes = boxes;
      AxisAlignedBB u = boxes[0];
      for (AxisAlignedBB b : boxes) {
        u = u.union(b);
      }
      this.whole = u;
      List<int[]> c = new ArrayList<>();
      for (int y = 0; y <= y1; y++) {
        for (int z = 0; z <= z1; z++) {
          for (int x = x0; x <= x1; x++) {
            if (x != 0 || y != 0 || z != 0) {
              c.add(new int[]{x, y, z});
            }
          }
        }
      }
      this.cells = Collections.unmodifiableList(c);
      this.cell = PropertyInteger.create("cell", 0, c.size());
    }

    /** The value of {@link #cell} for a part that belongs to no unit: it draws nothing. */
    int none() {
      return cells.size();
    }
  }

  private static AxisAlignedBB px(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  /**
   * The large unit: 3 x 2 x 5 blocks, about a 30-ton unit, two and a half rooftop units' worth.
   * Its boxes follow {@code rtu(-16, 32, 0, 80, ...)} in the generator: the cabinet on its base
   * rail and curb, and the outside air hood on the front.
   */
  public static final Size LARGE = new Size("hvac_rtu_large_part", -1, 1, 1, 4, 3000.0f,
      px(-15, 0, 2.5, 31, 27, 80), px(-12.5, 11, 0.25, 28.5, 24, 2.5));

  /** The size a registry name builds. */
  static Size sizeOf(String registryName) {
    return LARGE;
  }

  /** The finish a registry name builds: 0 sandstone, 1 silver, 2 black. */
  static int finishOf(String registryName) {
    if (registryName.endsWith("_black")) {
      return 2;
    }
    return registryName.endsWith("_silver") ? 1 : 0;
  }

  /** Set while a unit removes its own cells, so the removals do not recurse. */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final Size size;

  public BlockHvacPackagedRtu(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    this.size = sizeOf(registryName);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public Size getSize() {
    return size;
  }

  public int getFinish() {
    return finishOf(getBlockRegistryName());
  }

  // --- geometry ------------------------------------------------------------------------------

  /** A model-frame offset turned to the world for a facing. */
  static BlockPos turn(int dx, int dy, int dz, EnumFacing facing) {
    AxisAlignedBB b = RotationUtils.rotateBoundingBoxByFacing(
        new AxisAlignedBB(dx, dy, dz, dx + 1, dy + 1, dz + 1), facing);
    return new BlockPos((int) Math.round(b.minX), (int) Math.round(b.minY),
        (int) Math.round(b.minZ));
  }

  /** The world positions of a unit's cells after the master, in {@link Size#cells} order. */
  static List<BlockPos> partCells(Size size, BlockPos master, EnumFacing facing) {
    List<BlockPos> out = new ArrayList<>(size.cells.size());
    for (int[] c : size.cells) {
      out.add(master.add(turn(c[0], c[1], c[2], facing)));
    }
    return out;
  }

  /** The share of the unit's boxes in the cell at {@code cell} (world), relative to it. */
  @Nullable
  static AxisAlignedBB share(Size size, BlockPos master, EnumFacing facing, BlockPos cell) {
    AxisAlignedBB bounds = new AxisAlignedBB(cell);
    AxisAlignedBB out = null;
    for (AxisAlignedBB b : size.boxes) {
      AxisAlignedBB w = RotationUtils.rotateBoundingBoxByFacing(b, facing).offset(master);
      if (w.intersects(bounds)) {
        AxisAlignedBB c = w.intersect(bounds);
        out = out == null ? c : out.union(c);
      }
    }
    return out == null ? null : out.offset(-cell.getX(), -cell.getY(), -cell.getZ());
  }

  /** The whole unit's outline in the world. */
  static AxisAlignedBB worldOutline(Size size, BlockPos master, EnumFacing facing) {
    return RotationUtils.rotateBoundingBoxByFacing(size.whole, facing).offset(master);
  }

  /**
   * Which cell of which unit a part is: the index into {@link Size#cells} of the cell whose
   * master, at the part's position less the cell's offset turned by the part's facing, is a
   * master of this size with the same facing and finish; -1 for an orphan.
   */
  static int findCell(IBlockAccess world, BlockPos part, Size size, EnumFacing facing,
      int finish) {
    for (int i = 0; i < size.cells.size(); i++) {
      int[] c = size.cells.get(i);
      BlockPos m = part.subtract(turn(c[0], c[1], c[2], facing));
      IBlockState s = world.getBlockState(m);
      if (s.getBlock() instanceof BlockHvacPackagedRtu) {
        BlockHvacPackagedRtu b = (BlockHvacPackagedRtu) s.getBlock();
        if (b.size == size && s.getValue(FACING) == facing && b.getFinish() == finish) {
          return i;
        }
      }
    }
    return -1;
  }

  /** The master of the unit a part belongs to, or null for an orphan or anything else. */
  @Nullable
  static BlockPos findMaster(IBlockAccess world, BlockPos part) {
    IBlockState s = world.getBlockState(part);
    if (!(s.getBlock() instanceof BlockHvacPackagedRtuPart)) {
      return null;
    }
    BlockHvacPackagedRtuPart b = (BlockHvacPackagedRtuPart) s.getBlock();
    EnumFacing facing = s.getValue(BlockHvacPackagedRtuPart.FACING);
    int i = findCell(world, part, b.getSize(), facing,
        s.getValue(BlockHvacPackagedRtuPart.FINISH).ordinal());
    if (i < 0) {
      return null;
    }
    int[] c = b.getSize().cells.get(i);
    return part.subtract(turn(c[0], c[1], c[2], facing));
  }

  // --- shape ---------------------------------------------------------------------------------

  /** The master cell's share of the boxes, model frame (the base class turns it). */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB s = share(size, BlockPos.ORIGIN, EnumFacing.NORTH, BlockPos.ORIGIN);
    return s != null ? s : FULL_BLOCK_AABB;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    return worldOutline(size, pos, state.getValue(FACING));
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  // --- placing and breaking ------------------------------------------------------------------

  /** Fills every free cell of the unit with a part; returns how many were placed. */
  public int fillParts(World world, BlockPos master, IBlockState state) {
    Block part = CsmRegistry.getBlock(size.partName);
    if (!(part instanceof BlockHvacPackagedRtuPart)) {
      return 0;
    }
    EnumFacing facing = state.getValue(FACING);
    IBlockState partState = part.getDefaultState()
        .withProperty(BlockHvacPackagedRtuPart.FACING, facing)
        .withProperty(BlockHvacPackagedRtuPart.FINISH,
            BlockHvacPackagedRtuPart.Finish.values()[getFinish()]);
    int placed = 0;
    for (BlockPos at : partCells(size, master, facing)) {
      if (at.getY() < 0 || at.getY() >= world.getHeight()) {
        continue;
      }
      IBlockState here = world.getBlockState(at);
      if (here.getBlock() instanceof BlockHvacPackagedRtuPart
          || !here.getBlock().isReplaceable(world, at)) {
        continue;
      }
      world.setBlockState(at, partState, 3);
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
        for (BlockPos at : partCells(size, pos, facing)) {
          IBlockState s = world.getBlockState(at);
          if (s.getBlock() instanceof BlockHvacPackagedRtuPart
              && s.getValue(BlockHvacPackagedRtuPart.FACING) == facing) {
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
      Size size = ((BlockHvacPackagedRtu) block).size;
      for (BlockPos cell : partCells(size, pos, newState.getValue(FACING))) {
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

  // --- the unit ------------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacPackagedRtu.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacpackagedrtu";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacPackagedRtu();
  }
}
