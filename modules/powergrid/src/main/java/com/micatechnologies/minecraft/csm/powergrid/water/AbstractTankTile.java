package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tile of a tank: the block in the middle of a three-block cube of the tank that draws that
 * cube's share of it, the tank's parts ({@link BlockTankPart}) filling the rest of the cube.
 *
 * <p>A tile stores nothing about where it sits. It counts the tiles of the same tank three,
 * six, nine blocks to its west, north and below it, and that is its place in the grid
 * ({@link #gridPos}); the blockstate then picks the tile's model from that place, turning one
 * of the tank's few distinct tiles to it. So one registry name draws a whole tank, a tank's
 * tiles cost only the states of that place, and nothing is written per tile. The count reaches
 * at most twelve blocks, which a chunk being drawn always has around it.</p>
 *
 * <p>The item places the whole tank or nothing, a click on any cell of it goes to its tiles
 * (the name band), and breaking any cell removes the whole tank (a ground storage tank, the
 * layer) and drops its one item.</p>
 *
 * @since 2026.9
 */
public abstract class AbstractTankTile extends AbstractBlock
    implements ITankRoot, ICsmNoSnowAccumulation {

  /**
   * Carries the registry name and the shape to the superclass constructor, which builds the
   * state container (whose property ranges come from the shape) before this class's fields are
   * assigned.
   */
  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  protected final TankShape shape;

  protected AbstractTankTile(String registryName, TankShape shape) {
    super(stash(registryName, shape), SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
    this.registryName = registryName;
    this.shape = shape;
    PENDING.remove();
  }

  private static Material stash(String registryName, TankShape shape) {
    PENDING.set(new Object[]{registryName, shape});
    return Material.IRON;
  }

  /** The shape while the superclass constructor runs, when {@link #shape} is not yet set. */
  protected TankShape pendingShape() {
    return shape != null ? shape : (TankShape) PENDING.get()[1];
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : (String) PENDING.get()[0];
  }

  public TankShape getShape() {
    return shape;
  }

  @Override
  public Block getUnitBlock() {
    return CsmRegistry.getBlock(shape.getRegistryName());
  }

  // ---------------------------------------------------------------------------------------
  // Where a tile sits
  // ---------------------------------------------------------------------------------------

  /** Whether {@code state} is a tile of a tank of this shape. */
  protected boolean sameShape(IBlockState state) {
    return state.getBlock() instanceof AbstractTankTile
        && ((AbstractTankTile) state.getBlock()).shape == shape;
  }

  private int count(IBlockAccess world, BlockPos pos, EnumFacing towards, int max) {
    int n = 0;
    for (int k = 1; k <= max; k++) {
      if (!sameShape(world.getBlockState(pos.offset(towards, 3 * k)))) {
        break;
      }
      n++;
    }
    return n;
  }

  /** The tile's column, row and layer in its tank's grid: {gi (west to east), gj, gk}. */
  protected int[] gridPos(IBlockAccess world, BlockPos pos) {
    int max = shape.getGrid() - 1;
    int gi = count(world, pos, EnumFacing.WEST, max);
    int gj = count(world, pos, EnumFacing.NORTH, max);
    int gk = shape.isStackable() ? 0 : count(world, pos, EnumFacing.DOWN, shape.getLayers() - 1);
    return new int[]{gi, gj, gk};
  }

  /** The minimum corner of the grid (or of the layer, for a stackable tank) a tile is in. */
  protected BlockPos origin(IBlockAccess world, BlockPos pos) {
    int[] g = gridPos(world, pos);
    return pos.add(-(3 * g[0] + 1), -(3 * g[2] + 1), -(3 * g[1] + 1));
  }

  /** The map character of this tile's own cell. */
  private char ownCell(IBlockAccess world, BlockPos pos) {
    int[] g = gridPos(world, pos);
    return shape.cell(3 * g[0] + 1, 3 * g[2] + 1, 3 * g[1] + 1);
  }

  @Override
  public boolean coversCell(IBlockAccess world, BlockPos root, IBlockState rootState,
      BlockPos cell) {
    return Math.abs(cell.getX() - root.getX()) <= 1 && Math.abs(cell.getY() - root.getY()) <= 1
        && Math.abs(cell.getZ() - root.getZ()) <= 1;
  }

  // ---------------------------------------------------------------------------------------
  // Placing and removing
  // ---------------------------------------------------------------------------------------

  /**
   * The grid's bottom centre cell for a tank placed at {@code pos}: the cell itself, or one
   * snapped onto what the tank was set on (overridden).
   */
  protected BlockPos axisFor(World world, BlockPos pos) {
    return pos;
  }

  /** The state a tile root at grid place {@code gi, gj, gk} is placed with. */
  protected IBlockState rootStateFor(int gi, int gj, int gk, EnumFacing placerFacing) {
    return getDefaultState();
  }

  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    BlockPos axis = axisFor(world, pos);
    int half = shape.getHalf();
    BlockPos origin = axis.add(-half, 0, -half);
    int w = shape.getWidth();
    int h = shape.getHeight();
    for (int y = 0; y < h; y++) {
      for (int z = 0; z < w; z++) {
        for (int x = 0; x < w; x++) {
          if (shape.cell(x, y, z) == TankShape.NONE && !shape.isRootCell(x, y, z)) {
            continue;
          }
          BlockPos cell = origin.add(x, y, z);
          if (!cell.equals(pos) && !world.getBlockState(cell).getBlock()
              .isReplaceable(world, cell)) {
            TankUnits.refuse(world, pos, placer, getUnitBlock(), cell);
            return;
          }
        }
      }
    }
    IBlockState part = CsmRegistry.getBlock(BlockTankPart.REGISTRY_NAME).getDefaultState();
    EnumFacing facing = placer.getHorizontalFacing();
    TankUnits.DEMOLISHING.set(true);
    try {
      // the block the item placed makes way; whatever belongs in its cell is written below
      world.setBlockToAir(pos);
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < w; z++) {
          for (int x = 0; x < w; x++) {
            BlockPos cell = origin.add(x, y, z);
            if (shape.isRootCell(x, y, z)) {
              world.setBlockState(cell, rootStateFor(x / 3, z / 3, y / 3, facing), 3);
              continue;
            }
            int kind = TankShape.partKind(shape.cell(x, y, z));
            if (kind >= 0) {
              world.setBlockState(cell, part.withProperty(BlockTankPart.KIND, kind), 3);
            }
          }
        }
      }
    } finally {
      TankUnits.DEMOLISHING.set(false);
    }
  }

  /** The grid layers a demolition takes: the whole tank, or one layer of a stackable one. */
  @Override
  public void demolishUnit(World world, BlockPos root, @Nullable EntityPlayer player) {
    if (world.isRemote || TankUnits.DEMOLISHING.get()) {
      return;
    }
    BlockPos origin = origin(world, root);
    int n = shape.getGrid();
    TankUnits.DEMOLISHING.set(true);
    try {
      for (int gk = 0; gk < shape.getLayers(); gk++) {
        for (int gj = 0; gj < n; gj++) {
          for (int gi = 0; gi < n; gi++) {
            BlockPos tile = origin.add(3 * gi + 1, 3 * gk + 1, 3 * gj + 1);
            if (!sameShape(world.getBlockState(tile))) {
              continue;
            }
            for (BlockPos cell : BlockPos.getAllInBox(tile.add(-1, -1, -1), tile.add(1, 1, 1))) {
              if (cell.equals(tile)) {
                continue;
              }
              if (world.getBlockState(cell).getBlock() instanceof BlockTankPart) {
                world.setBlockToAir(cell);
              }
            }
            world.setBlockToAir(tile);
          }
        }
      }
    } finally {
      TankUnits.DEMOLISHING.set(false);
    }
    TankUnits.drop(world, root, player, getUnitBlock());
  }

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    if (!world.isRemote) {
      demolishUnit(world, pos, player);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !TankUnits.DEMOLISHING.get()) {
      demolishUnit(world, pos, null);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return new ItemStack(getUnitBlock());
  }

  // ---------------------------------------------------------------------------------------
  // Collision: the tile's own cell, as the cell map has it
  // ---------------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (source.getBlockState(pos).getBlock() != this) {
      return TankUnits.FULL;
    }
    int kind = TankShape.partKind(ownCell(source, pos));
    return kind < 0 ? NULL_AABB : TankUnits.selectionBox(kind);
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return getBlockBoundingBox(state, source, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    TankUnits.addCollision(TankShape.partKind(ownCell(world, pos)), world, pos, entityBox,
        boxes);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
    return false;
  }

  /** Cut-out, mipped: the handrails are a cut-out texture. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
