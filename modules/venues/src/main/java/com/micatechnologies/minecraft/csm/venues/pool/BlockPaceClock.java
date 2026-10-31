package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A pool's pace clock ({@code pool_pace_clock}): a 60 second dial whose red sweep hand goes round
 * once a minute and black hand once an hour, from the world's running time, so every pace clock
 * in a world reads the same. The dial and case are baked; the hands are
 * {@link TileEntityPaceClockRenderer}'s, a few quads a frame. Nothing ticks.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (towards the swimmers), bit 2 on a stand (on the deck)
 * rather than on a wall (its back against the block behind it).</p>
 *
 * @since 2026.10
 */
public class BlockPaceClock extends BlockVenue implements ICsmTileEntityProvider {

  /** On a stand rather than on a wall. */
  public static final PropertyBool STAND = PropertyBool.create("stand");

  private static final AxisAlignedBB WALL = new AxisAlignedBB(1 / 16.0, 1 / 16.0, 12 / 16.0,
      15 / 16.0, 15 / 16.0, 1);
  private static final AxisAlignedBB ON_STAND = new AxisAlignedBB(1 / 16.0, 0, 4 / 16.0,
      15 / 16.0, 1, 12 / 16.0);

  public BlockPaceClock(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STAND, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, STAND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(STAND, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(STAND) ? 4 : 0);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(STAND) ? ON_STAND : WALL;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPaceClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypaceclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPaceClock();
  }
}
