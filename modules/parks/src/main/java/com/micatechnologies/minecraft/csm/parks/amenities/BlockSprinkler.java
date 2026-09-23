package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A pop-up lawn sprinkler: flush with the ground until it is powered, then the riser pops up
 * and a rotor stream sweeps round it ({@link TileEntitySprinkler}, client side only). Wire it to
 * an {@link BlockIrrigationController} to water on the morning schedule, or to any redstone.
 *
 * @since 2026.9
 */
public class BlockSprinkler extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyBool POWERED = PropertyBool.create("powered");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.375, 0, 0.375, 0.625, 0.125,
      0.625);

  public BlockSprinkler() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1.0F, 2.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(POWERED, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "irrigation_sprinkler";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, POWERED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(POWERED) ? 1 : 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(POWERED, (meta & 1) != 0);
  }

  // --- redstone ---

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    update(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    update(world, pos, state);
  }

  private void update(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos) || liveWireBeside(world, pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
    }
  }

  /**
   * Whether a powered redstone wire runs beside it. Dust powers only the blocks it points into,
   * and a line that connects to sprinklers on both sides becomes a cross that points nowhere, so
   * a row of heads along one line would stay dry; a head on a live line runs, as it would on a
   * real zone valve.
   */
  private static boolean liveWireBeside(World world, BlockPos pos) {
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      IBlockState s = world.getBlockState(pos.offset(side));
      if (s.getBlock() == Blocks.REDSTONE_WIRE && s.getValue(BlockRedstoneWire.POWER) > 0) {
        return true;
      }
    }
    return false;
  }

  // --- tile entity: the spray ---

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySprinkler.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityirrigationsprinkler";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntitySprinkler();
  }

  // --- shape ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
