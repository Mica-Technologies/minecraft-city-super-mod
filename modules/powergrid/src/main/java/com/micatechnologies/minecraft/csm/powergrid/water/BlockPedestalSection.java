package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A section of a pedestal water tower's column: a 16-sided shaft three blocks across and a
 * block tall, drawn by this block in the middle of its three by three, with eight invisible
 * {@link BlockTankPart}s round it. Sections stack: one placed on a column, whichever of its
 * cells was clicked, goes square on top of it.
 *
 * <p>The lowest section ({@link #BASE}) stands on its concrete footing with the access door
 * facing the player who placed it ({@link #FACING}, the only thing stored); the highest
 * ({@link #TOP}) takes a cap unless a tank sits on it. Both are actual state, so a column
 * re-forms as sections are added or taken away. Breaking any cell takes its section.</p>
 *
 * @since 2026.9
 */
public class BlockPedestalSection extends AbstractBlock implements ITankRoot,
    ICsmNoSnowAccumulation {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  /** Nothing of the column is below this section. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** Nothing of the column, and no tank, is above this section. */
  public static final PropertyBool TOP = PropertyBool.create("top");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockPedestalSection(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
    this.registryName = registryName;
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

  @Override
  public Block getUnitBlock() {
    return this;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BASE, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState above = world.getBlockState(pos.up());
    return state.withProperty(BASE, world.getBlockState(pos.down()).getBlock() != this)
        .withProperty(TOP, above.getBlock() != this && !TankUnits.isTankBlock(above));
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public boolean coversCell(IBlockAccess world, BlockPos root, IBlockState rootState,
      BlockPos cell) {
    return cell.getY() == root.getY() && Math.abs(cell.getX() - root.getX()) <= 1
        && Math.abs(cell.getZ() - root.getZ()) <= 1;
  }

  private static List<BlockPos> ring(BlockPos root) {
    List<BlockPos> cells = new ArrayList<>(8);
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx != 0 || dz != 0) {
          cells.add(root.add(dx, 0, dz));
        }
      }
    }
    return cells;
  }

  /** Placed on a column: the section goes square on top of it, then its parts go round it. */
  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    BlockPos root = pos;
    BlockPos below = pos.down();
    IBlockState under = world.getBlockState(below);
    if (under.getBlock() instanceof BlockTankPart) {
      BlockPos other = TankUnits.findRoot(world, below);
      if (other != null && world.getBlockState(other).getBlock() == this) {
        root = other.up();
      }
    }
    List<BlockPos> cells = ring(root);
    cells.add(root);
    for (BlockPos cell : cells) {
      if (!cell.equals(pos) && !world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
        TankUnits.refuse(world, pos, placer, this, cell);
        return;
      }
    }
    IBlockState part = CsmRegistry.getBlock(BlockTankPart.REGISTRY_NAME).getDefaultState();
    TankUnits.DEMOLISHING.set(true);
    try {
      if (!root.equals(pos)) {
        world.setBlockToAir(pos);
        world.setBlockState(root, state, 3);
      }
      for (BlockPos cell : ring(root)) {
        world.setBlockState(cell, part, 3);
      }
    } finally {
      TankUnits.DEMOLISHING.set(false);
    }
  }

  @Override
  public void demolishUnit(World world, BlockPos root, @Nullable EntityPlayer player) {
    if (world.isRemote || TankUnits.DEMOLISHING.get()) {
      return;
    }
    TankUnits.DEMOLISHING.set(true);
    try {
      for (BlockPos cell : ring(root)) {
        if (world.getBlockState(cell).getBlock() instanceof BlockTankPart) {
          world.setBlockToAir(cell);
        }
      }
      if (world.getBlockState(root).getBlock() == this) {
        world.setBlockToAir(root);
      }
    } finally {
      TankUnits.DEMOLISHING.set(false);
    }
    TankUnits.drop(world, root, player, this);
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
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
